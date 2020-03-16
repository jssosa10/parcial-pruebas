package com.google.android.exoplayer2.upstream;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.TraceUtil;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

public final class Loader implements LoaderErrorThrower {
    private static final int ACTION_TYPE_DONT_RETRY = 2;
    private static final int ACTION_TYPE_DONT_RETRY_FATAL = 3;
    private static final int ACTION_TYPE_RETRY = 0;
    private static final int ACTION_TYPE_RETRY_AND_RESET_ERROR_COUNT = 1;
    public static final LoadErrorAction DONT_RETRY = new LoadErrorAction(2, C.TIME_UNSET);
    public static final LoadErrorAction DONT_RETRY_FATAL = new LoadErrorAction(3, C.TIME_UNSET);
    public static final LoadErrorAction RETRY = createRetryAction(false, C.TIME_UNSET);
    public static final LoadErrorAction RETRY_RESET_ERROR_COUNT = createRetryAction(true, C.TIME_UNSET);
    /* access modifiers changed from: private */
    public LoadTask<? extends Loadable> currentTask;
    /* access modifiers changed from: private */
    public final ExecutorService downloadExecutorService;
    /* access modifiers changed from: private */
    public IOException fatalError;

    public interface Callback<T extends Loadable> {
        void onLoadCanceled(T t, long j, long j2, boolean z);

        void onLoadCompleted(T t, long j, long j2);

        LoadErrorAction onLoadError(T t, long j, long j2, IOException iOException, int i);
    }

    public static final class LoadErrorAction {
        /* access modifiers changed from: private */
        public final long retryDelayMillis;
        /* access modifiers changed from: private */
        public final int type;

        private LoadErrorAction(int type2, long retryDelayMillis2) {
            this.type = type2;
            this.retryDelayMillis = retryDelayMillis2;
        }

        public boolean isRetry() {
            int i = this.type;
            return i == 0 || i == 1;
        }
    }

    @SuppressLint({"HandlerLeak"})
    private final class LoadTask<T extends Loadable> extends Handler implements Runnable {
        private static final int MSG_CANCEL = 1;
        private static final int MSG_END_OF_SOURCE = 2;
        private static final int MSG_FATAL_ERROR = 4;
        private static final int MSG_IO_EXCEPTION = 3;
        private static final int MSG_START = 0;
        private static final String TAG = "LoadTask";
        @Nullable
        private Callback<T> callback;
        private volatile boolean canceled;
        private IOException currentError;
        public final int defaultMinRetryCount;
        private int errorCount;
        private volatile Thread executorThread;
        private final T loadable;
        private volatile boolean released;
        private final long startTimeMs;

        public LoadTask(Looper looper, T loadable2, Callback<T> callback2, int defaultMinRetryCount2, long startTimeMs2) {
            super(looper);
            this.loadable = loadable2;
            this.callback = callback2;
            this.defaultMinRetryCount = defaultMinRetryCount2;
            this.startTimeMs = startTimeMs2;
        }

        public void maybeThrowError(int minRetryCount) throws IOException {
            IOException iOException = this.currentError;
            if (iOException != null && this.errorCount > minRetryCount) {
                throw iOException;
            }
        }

        public void start(long delayMillis) {
            Assertions.checkState(Loader.this.currentTask == null);
            Loader.this.currentTask = this;
            if (delayMillis > 0) {
                sendEmptyMessageDelayed(0, delayMillis);
            } else {
                execute();
            }
        }

        public void cancel(boolean released2) {
            this.released = released2;
            this.currentError = null;
            if (hasMessages(0)) {
                removeMessages(0);
                if (!released2) {
                    sendEmptyMessage(1);
                }
            } else {
                this.canceled = true;
                this.loadable.cancelLoad();
                if (this.executorThread != null) {
                    this.executorThread.interrupt();
                }
            }
            if (released2) {
                finish();
                long nowMs = SystemClock.elapsedRealtime();
                this.callback.onLoadCanceled(this.loadable, nowMs, nowMs - this.startTimeMs, true);
                this.callback = null;
            }
        }

        public void run() {
            try {
                this.executorThread = Thread.currentThread();
                if (!this.canceled) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("load:");
                    sb.append(this.loadable.getClass().getSimpleName());
                    TraceUtil.beginSection(sb.toString());
                    this.loadable.load();
                    TraceUtil.endSection();
                }
                if (!this.released) {
                    sendEmptyMessage(2);
                }
            } catch (IOException e) {
                if (!this.released) {
                    obtainMessage(3, e).sendToTarget();
                }
            } catch (InterruptedException e2) {
                Assertions.checkState(this.canceled);
                if (!this.released) {
                    sendEmptyMessage(2);
                }
            } catch (Exception e3) {
                Log.e(TAG, "Unexpected exception loading stream", e3);
                if (!this.released) {
                    obtainMessage(3, new UnexpectedLoaderException(e3)).sendToTarget();
                }
            } catch (OutOfMemoryError e4) {
                Log.e(TAG, "OutOfMemory error loading stream", e4);
                if (!this.released) {
                    obtainMessage(3, new UnexpectedLoaderException(e4)).sendToTarget();
                }
            } catch (Error e5) {
                Log.e(TAG, "Unexpected error loading stream", e5);
                if (!this.released) {
                    obtainMessage(4, e5).sendToTarget();
                }
                throw e5;
            } catch (Throwable th) {
                TraceUtil.endSection();
                throw th;
            }
        }

        public void handleMessage(Message msg) {
            long j;
            if (!this.released) {
                if (msg.what == 0) {
                    execute();
                } else if (msg.what != 4) {
                    finish();
                    long nowMs = SystemClock.elapsedRealtime();
                    long durationMs = nowMs - this.startTimeMs;
                    if (this.canceled) {
                        this.callback.onLoadCanceled(this.loadable, nowMs, durationMs, false);
                        return;
                    }
                    switch (msg.what) {
                        case 1:
                            this.callback.onLoadCanceled(this.loadable, nowMs, durationMs, false);
                            break;
                        case 2:
                            try {
                                this.callback.onLoadCompleted(this.loadable, nowMs, durationMs);
                                break;
                            } catch (RuntimeException e) {
                                Log.e(TAG, "Unexpected exception handling load completed", e);
                                Loader.this.fatalError = new UnexpectedLoaderException(e);
                                break;
                            }
                        case 3:
                            this.currentError = (IOException) msg.obj;
                            this.errorCount++;
                            LoadErrorAction action = this.callback.onLoadError(this.loadable, nowMs, durationMs, this.currentError, this.errorCount);
                            if (action.type != 3) {
                                if (action.type != 2) {
                                    if (action.type == 1) {
                                        this.errorCount = 1;
                                    }
                                    if (action.retryDelayMillis != C.TIME_UNSET) {
                                        j = action.retryDelayMillis;
                                    } else {
                                        j = getRetryDelayMillis();
                                    }
                                    start(j);
                                    break;
                                }
                            } else {
                                Loader.this.fatalError = this.currentError;
                                break;
                            }
                            break;
                    }
                } else {
                    throw ((Error) msg.obj);
                }
            }
        }

        private void execute() {
            this.currentError = null;
            Loader.this.downloadExecutorService.execute(Loader.this.currentTask);
        }

        private void finish() {
            Loader.this.currentTask = null;
        }

        private long getRetryDelayMillis() {
            return (long) Math.min((this.errorCount - 1) * 1000, 5000);
        }
    }

    public interface Loadable {
        void cancelLoad();

        void load() throws IOException, InterruptedException;
    }

    public interface ReleaseCallback {
        void onLoaderReleased();
    }

    private static final class ReleaseTask implements Runnable {
        private final ReleaseCallback callback;

        public ReleaseTask(ReleaseCallback callback2) {
            this.callback = callback2;
        }

        public void run() {
            this.callback.onLoaderReleased();
        }
    }

    public static final class UnexpectedLoaderException extends IOException {
        public UnexpectedLoaderException(Throwable cause) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unexpected ");
            sb.append(cause.getClass().getSimpleName());
            sb.append(": ");
            sb.append(cause.getMessage());
            super(sb.toString(), cause);
        }
    }

    public Loader(String threadName) {
        this.downloadExecutorService = Util.newSingleThreadExecutor(threadName);
    }

    public static LoadErrorAction createRetryAction(boolean resetErrorCount, long retryDelayMillis) {
        return new LoadErrorAction(resetErrorCount, retryDelayMillis);
    }

    public <T extends Loadable> long startLoading(T loadable, Callback<T> callback, int defaultMinRetryCount) {
        Looper looper = Looper.myLooper();
        Assertions.checkState(looper != null);
        this.fatalError = null;
        long startTimeMs = SystemClock.elapsedRealtime();
        LoadTask loadTask = new LoadTask(looper, loadable, callback, defaultMinRetryCount, startTimeMs);
        loadTask.start(0);
        return startTimeMs;
    }

    public boolean isLoading() {
        return this.currentTask != null;
    }

    public void cancelLoading() {
        this.currentTask.cancel(false);
    }

    public void release() {
        release(null);
    }

    public void release(@Nullable ReleaseCallback callback) {
        LoadTask<? extends Loadable> loadTask = this.currentTask;
        if (loadTask != null) {
            loadTask.cancel(true);
        }
        if (callback != null) {
            this.downloadExecutorService.execute(new ReleaseTask(callback));
        }
        this.downloadExecutorService.shutdown();
    }

    public void maybeThrowError() throws IOException {
        maybeThrowError(Integer.MIN_VALUE);
    }

    public void maybeThrowError(int minRetryCount) throws IOException {
        IOException iOException = this.fatalError;
        if (iOException == null) {
            LoadTask<? extends Loadable> loadTask = this.currentTask;
            if (loadTask != null) {
                loadTask.maybeThrowError(minRetryCount == Integer.MIN_VALUE ? loadTask.defaultMinRetryCount : minRetryCount);
                return;
            }
            return;
        }
        throw iOException;
    }
}
