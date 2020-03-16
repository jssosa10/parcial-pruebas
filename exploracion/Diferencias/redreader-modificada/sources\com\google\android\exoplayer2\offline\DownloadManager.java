package com.google.android.exoplayer2.offline;

import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.offline.DownloadAction.Deserializer;
import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

public final class DownloadManager {
    private static final boolean DEBUG = false;
    public static final int DEFAULT_MAX_SIMULTANEOUS_DOWNLOADS = 1;
    public static final int DEFAULT_MIN_RETRY_COUNT = 5;
    private static final String TAG = "DownloadManager";
    private final ActionFile actionFile;
    private final ArrayList<Task> activeDownloadTasks;
    private final Deserializer[] deserializers;
    /* access modifiers changed from: private */
    public final DownloaderConstructorHelper downloaderConstructorHelper;
    private boolean downloadsStopped;
    private final Handler fileIOHandler;
    private final HandlerThread fileIOThread;
    /* access modifiers changed from: private */
    public final Handler handler;
    private boolean initialized;
    private final CopyOnWriteArraySet<Listener> listeners;
    private final int maxActiveDownloadTasks;
    private final int minRetryCount;
    private int nextTaskId;
    private boolean released;
    private final ArrayList<Task> tasks;

    public interface Listener {
        void onIdle(DownloadManager downloadManager);

        void onInitialized(DownloadManager downloadManager);

        void onTaskStateChanged(DownloadManager downloadManager, TaskState taskState);
    }

    private static final class Task implements Runnable {
        public static final int STATE_QUEUED_CANCELING = 5;
        public static final int STATE_STARTED_CANCELING = 6;
        public static final int STATE_STARTED_STOPPING = 7;
        /* access modifiers changed from: private */
        public final DownloadAction action;
        /* access modifiers changed from: private */
        public volatile int currentState;
        private final DownloadManager downloadManager;
        private volatile Downloader downloader;
        private Throwable error;
        /* access modifiers changed from: private */
        public final int id;
        private final int minRetryCount;
        private Thread thread;

        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface InternalState {
        }

        private Task(int id2, DownloadManager downloadManager2, DownloadAction action2, int minRetryCount2) {
            this.id = id2;
            this.downloadManager = downloadManager2;
            this.action = action2;
            this.currentState = 0;
            this.minRetryCount = minRetryCount2;
        }

        public TaskState getDownloadState() {
            TaskState taskState = new TaskState(this.id, this.action, getExternalState(), getDownloadPercentage(), getDownloadedBytes(), this.error);
            return taskState;
        }

        public boolean isFinished() {
            return this.currentState == 4 || this.currentState == 2 || this.currentState == 3;
        }

        public boolean isActive() {
            return this.currentState == 5 || this.currentState == 1 || this.currentState == 7 || this.currentState == 6;
        }

        public float getDownloadPercentage() {
            if (this.downloader != null) {
                return this.downloader.getDownloadPercentage();
            }
            return -1.0f;
        }

        public long getDownloadedBytes() {
            if (this.downloader != null) {
                return this.downloader.getDownloadedBytes();
            }
            return 0;
        }

        public String toString() {
            return super.toString();
        }

        private static String toString(byte[] data) {
            if (data.length > 100) {
                return "<data is too long>";
            }
            StringBuilder sb = new StringBuilder();
            sb.append('\'');
            sb.append(Util.fromUtf8Bytes(data));
            sb.append('\'');
            return sb.toString();
        }

        private String getStateString() {
            switch (this.currentState) {
                case 5:
                case 6:
                    return "CANCELING";
                case 7:
                    return OrbotHelper.STATUS_STOPPING;
                default:
                    return TaskState.getStateString(this.currentState);
            }
        }

        private int getExternalState() {
            switch (this.currentState) {
                case 5:
                    return 0;
                case 6:
                case 7:
                    return 1;
                default:
                    return this.currentState;
            }
        }

        /* access modifiers changed from: private */
        public void start() {
            if (changeStateAndNotify(0, 1)) {
                this.thread = new Thread(this);
                this.thread.start();
            }
        }

        /* access modifiers changed from: private */
        public boolean canStart() {
            return this.currentState == 0;
        }

        /* access modifiers changed from: private */
        public void cancel() {
            if (changeStateAndNotify(0, 5)) {
                this.downloadManager.handler.post(new Runnable() {
                    public final void run() {
                        Task.this.changeStateAndNotify(5, 3);
                    }
                });
            } else if (changeStateAndNotify(1, 6)) {
                cancelDownload();
            }
        }

        /* access modifiers changed from: private */
        public void stop() {
            if (changeStateAndNotify(1, 7)) {
                DownloadManager.logd("Stopping", this);
                cancelDownload();
            }
        }

        /* access modifiers changed from: private */
        public boolean changeStateAndNotify(int oldState, int newState) {
            return changeStateAndNotify(oldState, newState, null);
        }

        private boolean changeStateAndNotify(int oldState, int newState, Throwable error2) {
            boolean z = false;
            if (this.currentState != oldState) {
                return false;
            }
            this.currentState = newState;
            this.error = error2;
            if (this.currentState != getExternalState()) {
                z = true;
            }
            if (!z) {
                this.downloadManager.onTaskStateChange(this);
            }
            return true;
        }

        private void cancelDownload() {
            if (this.downloader != null) {
                this.downloader.cancel();
            }
            this.thread.interrupt();
        }

        public void run() {
            int errorCount;
            long errorPosition;
            DownloadManager.logd("Task is started", this);
            Throwable error2 = null;
            try {
                this.downloader = this.action.createDownloader(this.downloadManager.downloaderConstructorHelper);
                if (this.action.isRemoveAction) {
                    this.downloader.remove();
                } else {
                    errorCount = 0;
                    errorPosition = -1;
                    while (!Thread.interrupted()) {
                        this.downloader.download();
                    }
                }
            } catch (IOException e) {
                long downloadedBytes = this.downloader.getDownloadedBytes();
                if (downloadedBytes != errorPosition) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Reset error count. downloadedBytes = ");
                    sb.append(downloadedBytes);
                    DownloadManager.logd(sb.toString(), this);
                    errorPosition = downloadedBytes;
                    errorCount = 0;
                }
                if (this.currentState == 1) {
                    errorCount++;
                    if (errorCount <= this.minRetryCount) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("Download error. Retry ");
                        sb2.append(errorCount);
                        DownloadManager.logd(sb2.toString(), this);
                        Thread.sleep((long) getRetryDelayMillis(errorCount));
                    }
                }
                throw e;
            } catch (Throwable e2) {
                error2 = e2;
            }
            this.downloadManager.handler.post(new Runnable(error2) {
                private final /* synthetic */ Throwable f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    Task.lambda$run$1(Task.this, this.f$1);
                }
            });
        }

        public static /* synthetic */ void lambda$run$1(Task task, Throwable finalError) {
            if (!task.changeStateAndNotify(1, finalError != null ? 4 : 2, finalError) && !task.changeStateAndNotify(6, 3) && !task.changeStateAndNotify(7, 0)) {
                throw new IllegalStateException();
            }
        }

        private int getRetryDelayMillis(int errorCount) {
            return Math.min((errorCount - 1) * 1000, 5000);
        }
    }

    public static final class TaskState {
        public static final int STATE_CANCELED = 3;
        public static final int STATE_COMPLETED = 2;
        public static final int STATE_FAILED = 4;
        public static final int STATE_QUEUED = 0;
        public static final int STATE_STARTED = 1;
        public final DownloadAction action;
        public final float downloadPercentage;
        public final long downloadedBytes;
        public final Throwable error;
        public final int state;
        public final int taskId;

        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface State {
        }

        public static String getStateString(int state2) {
            switch (state2) {
                case 0:
                    return "QUEUED";
                case 1:
                    return "STARTED";
                case 2:
                    return "COMPLETED";
                case 3:
                    return "CANCELED";
                case 4:
                    return "FAILED";
                default:
                    throw new IllegalStateException();
            }
        }

        private TaskState(int taskId2, DownloadAction action2, int state2, float downloadPercentage2, long downloadedBytes2, Throwable error2) {
            this.taskId = taskId2;
            this.action = action2;
            this.state = state2;
            this.downloadPercentage = downloadPercentage2;
            this.downloadedBytes = downloadedBytes2;
            this.error = error2;
        }
    }

    public DownloadManager(Cache cache, Factory upstreamDataSourceFactory, File actionSaveFile, Deserializer... deserializers2) {
        this(new DownloaderConstructorHelper(cache, upstreamDataSourceFactory), actionSaveFile, deserializers2);
    }

    public DownloadManager(DownloaderConstructorHelper constructorHelper, File actionFile2, Deserializer... deserializers2) {
        this(constructorHelper, 1, 5, actionFile2, deserializers2);
    }

    public DownloadManager(DownloaderConstructorHelper constructorHelper, int maxSimultaneousDownloads, int minRetryCount2, File actionFile2, Deserializer... deserializers2) {
        Deserializer[] deserializerArr;
        this.downloaderConstructorHelper = constructorHelper;
        this.maxActiveDownloadTasks = maxSimultaneousDownloads;
        this.minRetryCount = minRetryCount2;
        this.actionFile = new ActionFile(actionFile2);
        if (deserializers2.length > 0) {
            deserializerArr = deserializers2;
        } else {
            deserializerArr = DownloadAction.getDefaultDeserializers();
        }
        this.deserializers = deserializerArr;
        this.downloadsStopped = true;
        this.tasks = new ArrayList<>();
        this.activeDownloadTasks = new ArrayList<>();
        Looper looper = Looper.myLooper();
        if (looper == null) {
            looper = Looper.getMainLooper();
        }
        this.handler = new Handler(looper);
        this.fileIOThread = new HandlerThread("DownloadManager file i/o");
        this.fileIOThread.start();
        this.fileIOHandler = new Handler(this.fileIOThread.getLooper());
        this.listeners = new CopyOnWriteArraySet<>();
        loadActions();
        logd("Created");
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    public void startDownloads() {
        Assertions.checkState(!this.released);
        if (this.downloadsStopped) {
            this.downloadsStopped = false;
            maybeStartTasks();
            logd("Downloads are started");
        }
    }

    public void stopDownloads() {
        Assertions.checkState(!this.released);
        if (!this.downloadsStopped) {
            this.downloadsStopped = true;
            for (int i = 0; i < this.activeDownloadTasks.size(); i++) {
                ((Task) this.activeDownloadTasks.get(i)).stop();
            }
            logd("Downloads are stopping");
        }
    }

    public int handleAction(byte[] actionData) throws IOException {
        Assertions.checkState(!this.released);
        return handleAction(DownloadAction.deserializeFromStream(this.deserializers, new ByteArrayInputStream(actionData)));
    }

    public int handleAction(DownloadAction action) {
        Assertions.checkState(!this.released);
        Task task = addTaskForAction(action);
        if (this.initialized) {
            saveActions();
            maybeStartTasks();
            if (task.currentState == 0) {
                notifyListenersTaskStateChange(task);
            }
        }
        return task.id;
    }

    public int getTaskCount() {
        Assertions.checkState(!this.released);
        return this.tasks.size();
    }

    public int getDownloadCount() {
        int count = 0;
        for (int i = 0; i < this.tasks.size(); i++) {
            if (!((Task) this.tasks.get(i)).action.isRemoveAction) {
                count++;
            }
        }
        return count;
    }

    @Nullable
    public TaskState getTaskState(int taskId) {
        Assertions.checkState(!this.released);
        for (int i = 0; i < this.tasks.size(); i++) {
            Task task = (Task) this.tasks.get(i);
            if (task.id == taskId) {
                return task.getDownloadState();
            }
        }
        return null;
    }

    public TaskState[] getAllTaskStates() {
        Assertions.checkState(!this.released);
        TaskState[] states = new TaskState[this.tasks.size()];
        for (int i = 0; i < states.length; i++) {
            states[i] = ((Task) this.tasks.get(i)).getDownloadState();
        }
        return states;
    }

    public boolean isInitialized() {
        Assertions.checkState(!this.released);
        return this.initialized;
    }

    public boolean isIdle() {
        Assertions.checkState(!this.released);
        if (!this.initialized) {
            return false;
        }
        for (int i = 0; i < this.tasks.size(); i++) {
            if (((Task) this.tasks.get(i)).isActive()) {
                return false;
            }
        }
        return true;
    }

    public void release() {
        if (!this.released) {
            this.released = true;
            for (int i = 0; i < this.tasks.size(); i++) {
                ((Task) this.tasks.get(i)).stop();
            }
            ConditionVariable fileIOFinishedCondition = new ConditionVariable();
            Handler handler2 = this.fileIOHandler;
            fileIOFinishedCondition.getClass();
            handler2.post(new Runnable(fileIOFinishedCondition) {
                private final /* synthetic */ ConditionVariable f$0;

                {
                    this.f$0 = r1;
                }

                public final void run() {
                    this.f$0.open();
                }
            });
            fileIOFinishedCondition.block();
            this.fileIOThread.quit();
            logd("Released");
        }
    }

    private Task addTaskForAction(DownloadAction action) {
        int i = this.nextTaskId;
        this.nextTaskId = i + 1;
        Task task = new Task(i, this, action, this.minRetryCount);
        this.tasks.add(task);
        logd("Task is added", task);
        return task;
    }

    private void maybeStartTasks() {
        if (this.initialized && !this.released) {
            boolean skipDownloadActions = this.downloadsStopped || this.activeDownloadTasks.size() == this.maxActiveDownloadTasks;
            for (int i = 0; i < this.tasks.size(); i++) {
                Task task = (Task) this.tasks.get(i);
                if (task.canStart()) {
                    DownloadAction action = task.action;
                    boolean isRemoveAction = action.isRemoveAction;
                    if (isRemoveAction || !skipDownloadActions) {
                        boolean canStartTask = true;
                        int j = 0;
                        while (true) {
                            if (j >= i) {
                                break;
                            }
                            Task otherTask = (Task) this.tasks.get(j);
                            if (otherTask.action.isSameMedia(action)) {
                                if (isRemoveAction) {
                                    canStartTask = false;
                                    StringBuilder sb = new StringBuilder();
                                    sb.append(task);
                                    sb.append(" clashes with ");
                                    sb.append(otherTask);
                                    logd(sb.toString());
                                    otherTask.cancel();
                                } else if (otherTask.action.isRemoveAction) {
                                    canStartTask = false;
                                    skipDownloadActions = true;
                                    break;
                                }
                            }
                            j++;
                        }
                        if (canStartTask) {
                            task.start();
                            if (!isRemoveAction) {
                                this.activeDownloadTasks.add(task);
                                skipDownloadActions = this.activeDownloadTasks.size() == this.maxActiveDownloadTasks;
                            }
                        }
                    }
                }
            }
        }
    }

    private void maybeNotifyListenersIdle() {
        if (isIdle()) {
            logd("Notify idle state");
            Iterator it = this.listeners.iterator();
            while (it.hasNext()) {
                ((Listener) it.next()).onIdle(this);
            }
        }
    }

    /* access modifiers changed from: private */
    public void onTaskStateChange(Task task) {
        if (!this.released) {
            boolean stopped = !task.isActive();
            if (stopped) {
                this.activeDownloadTasks.remove(task);
            }
            notifyListenersTaskStateChange(task);
            if (task.isFinished()) {
                this.tasks.remove(task);
                saveActions();
            }
            if (stopped) {
                maybeStartTasks();
                maybeNotifyListenersIdle();
            }
        }
    }

    private void notifyListenersTaskStateChange(Task task) {
        logd("Task state is changed", task);
        TaskState taskState = task.getDownloadState();
        Iterator it = this.listeners.iterator();
        while (it.hasNext()) {
            ((Listener) it.next()).onTaskStateChanged(this, taskState);
        }
    }

    private void loadActions() {
        this.fileIOHandler.post(new Runnable() {
            public final void run() {
                DownloadManager.lambda$loadActions$1(DownloadManager.this);
            }
        });
    }

    public static /* synthetic */ void lambda$loadActions$1(DownloadManager downloadManager) {
        DownloadAction[] loadedActions;
        try {
            loadedActions = downloadManager.actionFile.load(downloadManager.deserializers);
            logd("Action file is loaded.");
        } catch (Throwable e) {
            Log.e(TAG, "Action file loading failed.", e);
            loadedActions = new DownloadAction[0];
        }
        downloadManager.handler.post(new Runnable(loadedActions) {
            private final /* synthetic */ DownloadAction[] f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                DownloadManager.lambda$null$0(DownloadManager.this, this.f$1);
            }
        });
    }

    public static /* synthetic */ void lambda$null$0(DownloadManager downloadManager, DownloadAction[] actions) {
        if (!downloadManager.released) {
            List<Task> pendingTasks = new ArrayList<>(downloadManager.tasks);
            downloadManager.tasks.clear();
            for (DownloadAction action : actions) {
                downloadManager.addTaskForAction(action);
            }
            logd("Tasks are created.");
            downloadManager.initialized = true;
            Iterator it = downloadManager.listeners.iterator();
            while (it.hasNext()) {
                ((Listener) it.next()).onInitialized(downloadManager);
            }
            if (!pendingTasks.isEmpty()) {
                downloadManager.tasks.addAll(pendingTasks);
                downloadManager.saveActions();
            }
            downloadManager.maybeStartTasks();
            for (int i = 0; i < downloadManager.tasks.size(); i++) {
                Task task = (Task) downloadManager.tasks.get(i);
                if (task.currentState == 0) {
                    downloadManager.notifyListenersTaskStateChange(task);
                }
            }
        }
    }

    private void saveActions() {
        if (!this.released) {
            DownloadAction[] actions = new DownloadAction[this.tasks.size()];
            for (int i = 0; i < this.tasks.size(); i++) {
                actions[i] = ((Task) this.tasks.get(i)).action;
            }
            this.fileIOHandler.post(new Runnable(actions) {
                private final /* synthetic */ DownloadAction[] f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    DownloadManager.lambda$saveActions$2(DownloadManager.this, this.f$1);
                }
            });
        }
    }

    public static /* synthetic */ void lambda$saveActions$2(DownloadManager downloadManager, DownloadAction[] actions) {
        try {
            downloadManager.actionFile.store(actions);
            logd("Actions persisted.");
        } catch (IOException e) {
            Log.e(TAG, "Persisting actions failed.", e);
        }
    }

    private static void logd(String message) {
    }

    /* access modifiers changed from: private */
    public static void logd(String message, Task task) {
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        sb.append(": ");
        sb.append(task);
        logd(sb.toString());
    }
}
