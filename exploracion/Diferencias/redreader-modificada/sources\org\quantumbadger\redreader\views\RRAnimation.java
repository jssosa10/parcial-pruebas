package org.quantumbadger.redreader.views;

import org.quantumbadger.redreader.views.RRChoreographer.Callback;

public abstract class RRAnimation implements Callback {
    private static final RRChoreographer CHOREOGRAPHER = RRChoreographer.getInstance();
    private long mFirstFrameNanos = -1;
    private boolean mStarted = false;
    private boolean mStopped = false;

    /* access modifiers changed from: protected */
    public abstract boolean handleFrame(long j);

    public final void start() {
        if (!this.mStarted) {
            this.mStarted = true;
            CHOREOGRAPHER.postFrameCallback(this);
            return;
        }
        throw new RuntimeException("Attempted to start animation twice!");
    }

    public final void stop() {
        if (!this.mStarted) {
            throw new RuntimeException("Attempted to stop animation before it's started!");
        } else if (!this.mStopped) {
            this.mStopped = true;
        } else {
            throw new RuntimeException("Attempted to stop animation twice!");
        }
    }

    public final void doFrame(long frameTimeNanos) {
        if (!this.mStopped) {
            if (this.mFirstFrameNanos == -1) {
                this.mFirstFrameNanos = frameTimeNanos;
            }
            if (handleFrame(frameTimeNanos - this.mFirstFrameNanos)) {
                CHOREOGRAPHER.postFrameCallback(this);
            }
        }
    }
}
