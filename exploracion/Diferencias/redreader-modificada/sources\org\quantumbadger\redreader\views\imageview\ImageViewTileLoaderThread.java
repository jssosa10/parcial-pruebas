package org.quantumbadger.redreader.views.imageview;

import org.quantumbadger.redreader.common.TriggerableThread;
import org.quantumbadger.redreader.common.collections.Stack;

public class ImageViewTileLoaderThread {
    /* access modifiers changed from: private */
    public final Stack<ImageViewTileLoader> mStack = new Stack<>(128);
    private final InternalThread mThread = new InternalThread(new InternalRunnable(), 0);

    private class InternalRunnable implements Runnable {
        private InternalRunnable() {
        }

        public void run() {
            ImageViewTileLoader tile;
            while (true) {
                synchronized (ImageViewTileLoaderThread.this.mStack) {
                    if (!ImageViewTileLoaderThread.this.mStack.isEmpty()) {
                        tile = (ImageViewTileLoader) ImageViewTileLoaderThread.this.mStack.pop();
                    } else {
                        return;
                    }
                }
                tile.doPrepare();
            }
            while (true) {
            }
        }
    }

    private class InternalThread extends TriggerableThread {
        public InternalThread(Runnable task, long initialDelay) {
            super(task, initialDelay);
        }
    }

    public void enqueue(ImageViewTileLoader tile) {
        synchronized (this.mStack) {
            this.mStack.push(tile);
            this.mThread.trigger();
        }
    }
}
