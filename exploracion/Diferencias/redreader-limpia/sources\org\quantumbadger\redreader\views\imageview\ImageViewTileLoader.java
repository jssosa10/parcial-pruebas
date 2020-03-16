package org.quantumbadger.redreader.views.imageview;

import android.graphics.Bitmap;
import android.support.annotation.UiThread;

public class ImageViewTileLoader {
    /* access modifiers changed from: private */
    public final Listener mListener;
    private final Object mLock;
    private final Runnable mNotifyRunnable = new Runnable() {
        public void run() {
            ImageViewTileLoader.this.mListener.onTileLoaded(ImageViewTileLoader.this.mX, ImageViewTileLoader.this.mY, ImageViewTileLoader.this.mSampleSize);
        }
    };
    private Bitmap mResult;
    /* access modifiers changed from: private */
    public final int mSampleSize;
    private final ImageTileSource mSource;
    private final ImageViewTileLoaderThread mThread;
    private boolean mWanted;
    /* access modifiers changed from: private */
    public final int mX;
    /* access modifiers changed from: private */
    public final int mY;

    @UiThread
    public interface Listener {
        void onTileLoaded(int i, int i2, int i3);

        void onTileLoaderException(Throwable th);

        void onTileLoaderOutOfMemory();
    }

    private class NotifyErrorRunnable implements Runnable {
        private final Throwable mError;

        private NotifyErrorRunnable(Throwable mError2) {
            this.mError = mError2;
        }

        public void run() {
            ImageViewTileLoader.this.mListener.onTileLoaderException(this.mError);
        }
    }

    private class NotifyOOMRunnable implements Runnable {
        private NotifyOOMRunnable() {
        }

        public void run() {
            ImageViewTileLoader.this.mListener.onTileLoaderOutOfMemory();
        }
    }

    public ImageViewTileLoader(ImageTileSource source, ImageViewTileLoaderThread thread, int x, int y, int sampleSize, Listener listener, Object lock) {
        this.mSource = source;
        this.mThread = thread;
        this.mX = x;
        this.mY = y;
        this.mSampleSize = sampleSize;
        this.mListener = listener;
        this.mLock = lock;
    }

    public void markAsWanted() {
        if (!this.mWanted) {
            if (this.mResult == null) {
                this.mThread.enqueue(this);
                this.mWanted = true;
                return;
            }
            throw new RuntimeException("Not wanted, but the image is loaded anyway!");
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:15:0x001d, code lost:
        r1 = r5.mSource.getTile(r5.mSampleSize, r5.mX, r5.mY);
        r2 = r5.mLock;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0021, code lost:
        monitor-enter(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0024, code lost:
        if (r5.mWanted == false) goto L_0x0029;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0026, code lost:
        r5.mResult = r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0029, code lost:
        if (r1 == null) goto L_0x002e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x002b, code lost:
        r1.recycle();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x002e, code lost:
        monitor-exit(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x002f, code lost:
        org.quantumbadger.redreader.common.AndroidCommon.UI_THREAD_HANDLER.post(r5.mNotifyRunnable);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0036, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x003a, code lost:
        r1 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x003b, code lost:
        android.util.Log.e("ImageViewTileLoader", "Exception in getTile()", r1);
        org.quantumbadger.redreader.common.AndroidCommon.UI_THREAD_HANDLER.post(new org.quantumbadger.redreader.views.imageview.ImageViewTileLoader.NotifyErrorRunnable(r5, r1, null));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x004c, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x004e, code lost:
        org.quantumbadger.redreader.common.AndroidCommon.UI_THREAD_HANDLER.post(new org.quantumbadger.redreader.views.imageview.ImageViewTileLoader.NotifyOOMRunnable(r5, null));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x0058, code lost:
        return;
     */
    public void doPrepare() {
        synchronized (this.mLock) {
            if (this.mWanted) {
                if (this.mResult != null) {
                }
            }
        }
    }

    public Bitmap get() {
        Bitmap bitmap;
        synchronized (this.mLock) {
            if (this.mWanted) {
                bitmap = this.mResult;
            } else {
                throw new RuntimeException("Attempted to get unwanted image!");
            }
        }
        return bitmap;
    }

    public void markAsUnwanted() {
        this.mWanted = false;
        Bitmap bitmap = this.mResult;
        if (bitmap != null) {
            bitmap.recycle();
            this.mResult = null;
        }
    }
}
