package org.quantumbadger.redreader.io;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.quantumbadger.redreader.common.TriggerableThread;
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager;

public class RedditChangeDataIO {
    private static final String DB_FILENAME = "rr_change_data.dat";
    private static final int DB_VERSION = 1;
    private static final String DB_WRITETMP_FILENAME = "rr_change_data_tmp.dat";
    private static RedditChangeDataIO INSTANCE = null;
    private static boolean STATIC_UPDATE_PENDING = false;
    private static final String TAG = "RedditChangeDataIO";
    private final Context mContext;
    private boolean mIsInitialReadComplete = false;
    private final AtomicBoolean mIsInitialReadStarted = new AtomicBoolean(false);
    private final Object mLock = new Object();
    private boolean mUpdatePending = false;
    private final TriggerableThread mWriteThread = new TriggerableThread(new WriteRunnable(), DefaultRenderersFactory.DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS);

    private final class WriteRunnable implements Runnable {
        private WriteRunnable() {
        }

        public void run() {
            long startTime = System.currentTimeMillis();
            try {
                File dataFileTmpLocation = RedditChangeDataIO.this.getDataFileWriteTmpLocation();
                Log.i(RedditChangeDataIO.TAG, String.format(Locale.US, "Writing tmp data file at '%s'", new Object[]{dataFileTmpLocation.getAbsolutePath()}));
                ExtendedDataOutputStream dos = new ExtendedDataOutputStream(new BufferedOutputStream(new FileOutputStream(dataFileTmpLocation), 65536));
                dos.writeInt(1);
                RedditChangeDataManager.writeAllUsers(dos);
                dos.flush();
                dos.close();
                Log.i(RedditChangeDataIO.TAG, "Write successful. Atomically replacing data file...");
                File dataFileLocation = RedditChangeDataIO.this.getDataFileLocation();
                if (!dataFileTmpLocation.renameTo(dataFileLocation)) {
                    Log.e(RedditChangeDataIO.TAG, "Atomic replace failed!");
                    return;
                }
                Log.i(RedditChangeDataIO.TAG, "Write complete.");
                long bytes = dataFileLocation.length();
                long duration = System.currentTimeMillis() - startTime;
                Log.i(RedditChangeDataIO.TAG, String.format(Locale.US, "%d bytes written in %d ms", new Object[]{Long.valueOf(bytes), Long.valueOf(duration)}));
            } catch (IOException e) {
                Log.e(RedditChangeDataIO.TAG, "Write failed!", e);
            }
        }
    }

    @NonNull
    public static synchronized RedditChangeDataIO getInstance(Context context) {
        RedditChangeDataIO redditChangeDataIO;
        synchronized (RedditChangeDataIO.class) {
            if (INSTANCE == null) {
                INSTANCE = new RedditChangeDataIO(context);
                if (STATIC_UPDATE_PENDING) {
                    INSTANCE.notifyUpdate();
                }
            }
            redditChangeDataIO = INSTANCE;
        }
        return redditChangeDataIO;
    }

    public static synchronized void notifyUpdateStatic() {
        synchronized (RedditChangeDataIO.class) {
            if (INSTANCE != null) {
                INSTANCE.notifyUpdate();
            } else {
                STATIC_UPDATE_PENDING = true;
            }
        }
    }

    private RedditChangeDataIO(Context context) {
        this.mContext = context;
    }

    private void notifyUpdate() {
        synchronized (this.mLock) {
            if (this.mIsInitialReadComplete) {
                triggerUpdate();
            } else {
                this.mUpdatePending = true;
            }
        }
    }

    /* access modifiers changed from: private */
    public File getDataFileLocation() {
        return new File(this.mContext.getFilesDir(), DB_FILENAME);
    }

    /* access modifiers changed from: private */
    public File getDataFileWriteTmpLocation() {
        return new File(this.mContext.getFilesDir(), DB_WRITETMP_FILENAME);
    }

    public void runInitialReadInThisThread() {
        ExtendedDataInputStream dis;
        if (!this.mIsInitialReadStarted.getAndSet(true)) {
            Log.i(TAG, "Running initial read...");
            try {
                File dataFileLocation = getDataFileLocation();
                Log.i(TAG, String.format(Locale.US, "Data file at '%s'", new Object[]{dataFileLocation.getAbsolutePath()}));
                if (!dataFileLocation.exists()) {
                    Log.i(TAG, "Data file does not exist. Aborting read.");
                    notifyInitialReadComplete();
                    return;
                }
                dis = new ExtendedDataInputStream(new BufferedInputStream(new FileInputStream(dataFileLocation), 65536));
                int version = dis.readInt();
                if (1 != version) {
                    Log.i(TAG, String.format(Locale.US, "Wanted version %d, got %d. Aborting read.", new Object[]{Integer.valueOf(1), Integer.valueOf(version)}));
                    try {
                        dis.close();
                    } catch (IOException e) {
                        Log.e(TAG, "IO error while trying to close input file", e);
                    }
                    notifyInitialReadComplete();
                    return;
                }
                RedditChangeDataManager.readAllUsers(dis, this.mContext);
                Log.i(TAG, "Initial read successful.");
                try {
                    dis.close();
                } catch (IOException e2) {
                    Log.e(TAG, "IO error while trying to close input file", e2);
                }
                notifyInitialReadComplete();
            } catch (Exception e3) {
                try {
                    Log.e(TAG, "Initial read failed", e3);
                } catch (Throwable th) {
                    notifyInitialReadComplete();
                    throw th;
                }
            } catch (Throwable th2) {
                try {
                    dis.close();
                } catch (IOException e4) {
                    Log.e(TAG, "IO error while trying to close input file", e4);
                }
                throw th2;
            }
        } else {
            throw new RuntimeException("Attempted to run initial read twice!");
        }
    }

    private void notifyInitialReadComplete() {
        synchronized (this.mLock) {
            this.mIsInitialReadComplete = true;
            if (this.mUpdatePending) {
                triggerUpdate();
                this.mUpdatePending = false;
            }
        }
    }

    private void triggerUpdate() {
        this.mWriteThread.trigger();
    }
}
