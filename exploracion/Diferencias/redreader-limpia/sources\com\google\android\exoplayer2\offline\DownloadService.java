package com.google.android.exoplayer2.offline;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import com.google.android.exoplayer2.offline.DownloadManager.Listener;
import com.google.android.exoplayer2.offline.DownloadManager.TaskState;
import com.google.android.exoplayer2.scheduler.Requirements;
import com.google.android.exoplayer2.scheduler.RequirementsWatcher;
import com.google.android.exoplayer2.scheduler.Scheduler;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.NotificationUtil;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.util.HashMap;

public abstract class DownloadService extends Service {
    public static final String ACTION_ADD = "com.google.android.exoplayer.downloadService.action.ADD";
    public static final String ACTION_INIT = "com.google.android.exoplayer.downloadService.action.INIT";
    public static final String ACTION_RELOAD_REQUIREMENTS = "com.google.android.exoplayer.downloadService.action.RELOAD_REQUIREMENTS";
    private static final String ACTION_RESTART = "com.google.android.exoplayer.downloadService.action.RESTART";
    private static final boolean DEBUG = false;
    public static final long DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL = 1000;
    private static final Requirements DEFAULT_REQUIREMENTS = new Requirements(1, false, false);
    public static final int FOREGROUND_NOTIFICATION_ID_NONE = 0;
    public static final String KEY_DOWNLOAD_ACTION = "download_action";
    public static final String KEY_FOREGROUND = "foreground";
    private static final String TAG = "DownloadService";
    private static final HashMap<Class<? extends DownloadService>, RequirementsHelper> requirementsHelpers = new HashMap<>();
    @Nullable
    private final String channelId;
    @StringRes
    private final int channelName;
    /* access modifiers changed from: private */
    public DownloadManager downloadManager;
    private DownloadManagerListener downloadManagerListener;
    /* access modifiers changed from: private */
    @Nullable
    public final ForegroundNotificationUpdater foregroundNotificationUpdater;
    private int lastStartId;
    private boolean startedInForeground;
    private boolean taskRemoved;

    private final class DownloadManagerListener implements Listener {
        private DownloadManagerListener() {
        }

        public void onInitialized(DownloadManager downloadManager) {
            DownloadService downloadService = DownloadService.this;
            downloadService.maybeStartWatchingRequirements(downloadService.getRequirements());
        }

        public void onTaskStateChanged(DownloadManager downloadManager, TaskState taskState) {
            DownloadService.this.onTaskStateChanged(taskState);
            if (DownloadService.this.foregroundNotificationUpdater == null) {
                return;
            }
            if (taskState.state == 1) {
                DownloadService.this.foregroundNotificationUpdater.startPeriodicUpdates();
            } else {
                DownloadService.this.foregroundNotificationUpdater.update();
            }
        }

        public final void onIdle(DownloadManager downloadManager) {
            DownloadService.this.stop();
        }
    }

    private final class ForegroundNotificationUpdater implements Runnable {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private boolean notificationDisplayed;
        private final int notificationId;
        private boolean periodicUpdatesStarted;
        private final long updateInterval;

        public ForegroundNotificationUpdater(int notificationId2, long updateInterval2) {
            this.notificationId = notificationId2;
            this.updateInterval = updateInterval2;
        }

        public void startPeriodicUpdates() {
            this.periodicUpdatesStarted = true;
            update();
        }

        public void stopPeriodicUpdates() {
            this.periodicUpdatesStarted = false;
            this.handler.removeCallbacks(this);
        }

        public void update() {
            TaskState[] taskStates = DownloadService.this.downloadManager.getAllTaskStates();
            DownloadService downloadService = DownloadService.this;
            downloadService.startForeground(this.notificationId, downloadService.getForegroundNotification(taskStates));
            this.notificationDisplayed = true;
            if (this.periodicUpdatesStarted) {
                this.handler.removeCallbacks(this);
                this.handler.postDelayed(this, this.updateInterval);
            }
        }

        public void showNotificationIfNotAlready() {
            if (!this.notificationDisplayed) {
                update();
            }
        }

        public void run() {
            update();
        }
    }

    private static final class RequirementsHelper implements RequirementsWatcher.Listener {
        private final Context context;
        private final Requirements requirements;
        private final RequirementsWatcher requirementsWatcher;
        @Nullable
        private final Scheduler scheduler;
        private final Class<? extends DownloadService> serviceClass;

        private RequirementsHelper(Context context2, Requirements requirements2, @Nullable Scheduler scheduler2, Class<? extends DownloadService> serviceClass2) {
            this.context = context2;
            this.requirements = requirements2;
            this.scheduler = scheduler2;
            this.serviceClass = serviceClass2;
            this.requirementsWatcher = new RequirementsWatcher(context2, this, requirements2);
        }

        public void start() {
            this.requirementsWatcher.start();
        }

        public void stop() {
            this.requirementsWatcher.stop();
            Scheduler scheduler2 = this.scheduler;
            if (scheduler2 != null) {
                scheduler2.cancel();
            }
        }

        public void requirementsMet(RequirementsWatcher requirementsWatcher2) {
            try {
                notifyService();
                Scheduler scheduler2 = this.scheduler;
                if (scheduler2 != null) {
                    scheduler2.cancel();
                }
            } catch (Exception e) {
            }
        }

        public void requirementsNotMet(RequirementsWatcher requirementsWatcher2) {
            try {
                notifyService();
            } catch (Exception e) {
            }
            if (this.scheduler != null) {
                if (!this.scheduler.schedule(this.requirements, this.context.getPackageName(), DownloadService.ACTION_RESTART)) {
                    Log.e(DownloadService.TAG, "Scheduling downloads failed.");
                }
            }
        }

        private void notifyService() throws Exception {
            try {
                this.context.startService(DownloadService.getIntent(this.context, this.serviceClass, DownloadService.ACTION_INIT));
            } catch (IllegalStateException e) {
                throw new Exception(e);
            }
        }
    }

    /* access modifiers changed from: protected */
    public abstract DownloadManager getDownloadManager();

    /* access modifiers changed from: protected */
    @Nullable
    public abstract Scheduler getScheduler();

    protected DownloadService(int foregroundNotificationId) {
        this(foregroundNotificationId, 1000);
    }

    protected DownloadService(int foregroundNotificationId, long foregroundNotificationUpdateInterval) {
        this(foregroundNotificationId, foregroundNotificationUpdateInterval, null, 0);
    }

    protected DownloadService(int foregroundNotificationId, long foregroundNotificationUpdateInterval, @Nullable String channelId2, @StringRes int channelName2) {
        this.foregroundNotificationUpdater = foregroundNotificationId == 0 ? null : new ForegroundNotificationUpdater(foregroundNotificationId, foregroundNotificationUpdateInterval);
        this.channelId = channelId2;
        this.channelName = channelName2;
    }

    public static Intent buildAddActionIntent(Context context, Class<? extends DownloadService> clazz, DownloadAction downloadAction, boolean foreground) {
        return getIntent(context, clazz, ACTION_ADD).putExtra(KEY_DOWNLOAD_ACTION, downloadAction.toByteArray()).putExtra(KEY_FOREGROUND, foreground);
    }

    public static void startWithAction(Context context, Class<? extends DownloadService> clazz, DownloadAction downloadAction, boolean foreground) {
        Intent intent = buildAddActionIntent(context, clazz, downloadAction, foreground);
        if (foreground) {
            Util.startForegroundService(context, intent);
        } else {
            context.startService(intent);
        }
    }

    public static void start(Context context, Class<? extends DownloadService> clazz) {
        context.startService(getIntent(context, clazz, ACTION_INIT));
    }

    public static void startForeground(Context context, Class<? extends DownloadService> clazz) {
        Util.startForegroundService(context, getIntent(context, clazz, ACTION_INIT).putExtra(KEY_FOREGROUND, true));
    }

    public void onCreate() {
        logd("onCreate");
        String str = this.channelId;
        if (str != null) {
            NotificationUtil.createNotificationChannel(this, str, this.channelName, 2);
        }
        this.downloadManager = getDownloadManager();
        this.downloadManagerListener = new DownloadManagerListener();
        this.downloadManager.addListener(this.downloadManagerListener);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0066, code lost:
        if (r1.equals(ACTION_INIT) == false) goto L_0x0087;
     */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x008b  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x00a2  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x00a6  */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x00d0  */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x00d6  */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x00e6  */
    public int onStartCommand(Intent intent, int flags, int startId) {
        Requirements requirements;
        this.lastStartId = startId;
        char c = 0;
        this.taskRemoved = false;
        String intentAction = null;
        if (intent != null) {
            intentAction = intent.getAction();
            this.startedInForeground |= intent.getBooleanExtra(KEY_FOREGROUND, false) || ACTION_RESTART.equals(intentAction);
        }
        if (intentAction == null) {
            intentAction = ACTION_INIT;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("onStartCommand action: ");
        sb.append(intentAction);
        sb.append(" startId: ");
        sb.append(startId);
        logd(sb.toString());
        int hashCode = intentAction.hashCode();
        if (hashCode != -871181424) {
            if (hashCode != -608867945) {
                if (hashCode != -382886238) {
                    if (hashCode == 1015676687) {
                    }
                } else if (intentAction.equals(ACTION_ADD)) {
                    c = 2;
                    switch (c) {
                        case 0:
                        case 1:
                            break;
                        case 2:
                            byte[] actionData = intent.getByteArrayExtra(KEY_DOWNLOAD_ACTION);
                            if (actionData != null) {
                                try {
                                    this.downloadManager.handleAction(actionData);
                                    break;
                                } catch (IOException e) {
                                    Log.e(TAG, "Failed to handle ADD action", e);
                                    break;
                                }
                            } else {
                                Log.e(TAG, "Ignoring ADD action with no action data");
                                break;
                            }
                        case 3:
                            stopWatchingRequirements();
                            break;
                        default:
                            String str = TAG;
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("Ignoring unrecognized action: ");
                            sb2.append(intentAction);
                            Log.e(str, sb2.toString());
                            break;
                    }
                    requirements = getRequirements();
                    if (!requirements.checkRequirements(this)) {
                        this.downloadManager.startDownloads();
                    } else {
                        this.downloadManager.stopDownloads();
                    }
                    maybeStartWatchingRequirements(requirements);
                    if (this.downloadManager.isIdle()) {
                        stop();
                    }
                    return 1;
                }
            } else if (intentAction.equals(ACTION_RELOAD_REQUIREMENTS)) {
                c = 3;
                switch (c) {
                    case 0:
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                }
                requirements = getRequirements();
                if (!requirements.checkRequirements(this)) {
                }
                maybeStartWatchingRequirements(requirements);
                if (this.downloadManager.isIdle()) {
                }
                return 1;
            }
        } else if (intentAction.equals(ACTION_RESTART)) {
            c = 1;
            switch (c) {
                case 0:
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
            }
            requirements = getRequirements();
            if (!requirements.checkRequirements(this)) {
            }
            maybeStartWatchingRequirements(requirements);
            if (this.downloadManager.isIdle()) {
            }
            return 1;
        }
        c = 65535;
        switch (c) {
            case 0:
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
        }
        requirements = getRequirements();
        if (!requirements.checkRequirements(this)) {
        }
        maybeStartWatchingRequirements(requirements);
        if (this.downloadManager.isIdle()) {
        }
        return 1;
    }

    public void onTaskRemoved(Intent rootIntent) {
        StringBuilder sb = new StringBuilder();
        sb.append("onTaskRemoved rootIntent: ");
        sb.append(rootIntent);
        logd(sb.toString());
        this.taskRemoved = true;
    }

    public void onDestroy() {
        logd("onDestroy");
        ForegroundNotificationUpdater foregroundNotificationUpdater2 = this.foregroundNotificationUpdater;
        if (foregroundNotificationUpdater2 != null) {
            foregroundNotificationUpdater2.stopPeriodicUpdates();
        }
        this.downloadManager.removeListener(this.downloadManagerListener);
        maybeStopWatchingRequirements();
    }

    @Nullable
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* access modifiers changed from: protected */
    public Requirements getRequirements() {
        return DEFAULT_REQUIREMENTS;
    }

    /* access modifiers changed from: protected */
    public Notification getForegroundNotification(TaskState[] taskStates) {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        sb.append(" is started in the foreground but getForegroundNotification() is not implemented.");
        throw new IllegalStateException(sb.toString());
    }

    /* access modifiers changed from: protected */
    public void onTaskStateChanged(TaskState taskState) {
    }

    /* access modifiers changed from: private */
    public void maybeStartWatchingRequirements(Requirements requirements) {
        if (this.downloadManager.getDownloadCount() != 0) {
            Class<? extends DownloadService> clazz = getClass();
            if (((RequirementsHelper) requirementsHelpers.get(clazz)) == null) {
                RequirementsHelper requirementsHelper = new RequirementsHelper(this, requirements, getScheduler(), clazz);
                RequirementsHelper requirementsHelper2 = requirementsHelper;
                requirementsHelpers.put(clazz, requirementsHelper2);
                requirementsHelper2.start();
                logd("started watching requirements");
            }
        }
    }

    private void maybeStopWatchingRequirements() {
        if (this.downloadManager.getDownloadCount() <= 0) {
            stopWatchingRequirements();
        }
    }

    private void stopWatchingRequirements() {
        RequirementsHelper requirementsHelper = (RequirementsHelper) requirementsHelpers.remove(getClass());
        if (requirementsHelper != null) {
            requirementsHelper.stop();
            logd("stopped watching requirements");
        }
    }

    /* access modifiers changed from: private */
    public void stop() {
        ForegroundNotificationUpdater foregroundNotificationUpdater2 = this.foregroundNotificationUpdater;
        if (foregroundNotificationUpdater2 != null) {
            foregroundNotificationUpdater2.stopPeriodicUpdates();
            if (this.startedInForeground && Util.SDK_INT >= 26) {
                this.foregroundNotificationUpdater.showNotificationIfNotAlready();
            }
        }
        if (Util.SDK_INT >= 28 || !this.taskRemoved) {
            boolean stopSelfResult = stopSelfResult(this.lastStartId);
            StringBuilder sb = new StringBuilder();
            sb.append("stopSelf(");
            sb.append(this.lastStartId);
            sb.append(") result: ");
            sb.append(stopSelfResult);
            logd(sb.toString());
            return;
        }
        stopSelf();
        logd("stopSelf()");
    }

    private void logd(String message) {
    }

    /* access modifiers changed from: private */
    public static Intent getIntent(Context context, Class<? extends DownloadService> clazz, String action) {
        return new Intent(context, clazz).setAction(action);
    }
}
