package org.quantumbadger.redreader.views;

import org.quantumbadger.redreader.views.LiveDHM.Params;

public abstract class RRDHMAnimation extends RRAnimation {
    private final LiveDHM mDHM;

    /* access modifiers changed from: protected */
    public abstract void onEndPosition(float f);

    /* access modifiers changed from: protected */
    public abstract void onUpdatedPosition(float f);

    public RRDHMAnimation(Params params) {
        this.mDHM = new LiveDHM(params);
    }

    /* access modifiers changed from: protected */
    public boolean handleFrame(long nanosSinceAnimationStart) {
        long microsSinceAnimationStart = nanosSinceAnimationStart / 1000;
        double d = (double) this.mDHM.getParams().stepLengthSeconds;
        Double.isNaN(d);
        long stepLengthMicros = (long) (d * 1000.0d * 1000.0d);
        int desiredStepNumber = (int) (((stepLengthMicros / 2) + microsSinceAnimationStart) / stepLengthMicros);
        while (this.mDHM.getCurrentStep() < desiredStepNumber) {
            this.mDHM.calculateStep();
            if (this.mDHM.isEndThresholdReached()) {
                onEndPosition(this.mDHM.getParams().endPosition);
                return false;
            }
        }
        onUpdatedPosition(this.mDHM.getCurrentPosition());
        return true;
    }

    public final float getCurrentVelocity() {
        return this.mDHM.getCurrentVelocity();
    }
}
