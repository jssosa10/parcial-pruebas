package org.quantumbadger.redreader.views.imageview;

import org.quantumbadger.redreader.common.MutableFloatPoint2D;

public class ImageViewScaleAnimation {
    private final CoordinateHelper mCoordinateHelper;
    private final MutableFloatPoint2D mScreenCoord = new MutableFloatPoint2D();
    private final float mStepSize;
    private final float mTargetScale;

    public ImageViewScaleAnimation(float targetScale, CoordinateHelper coordinateHelper, int stepCount, MutableFloatPoint2D screenCoord) {
        this.mTargetScale = targetScale;
        this.mCoordinateHelper = coordinateHelper;
        double scale = (double) (targetScale / coordinateHelper.getScale());
        double d = (double) stepCount;
        Double.isNaN(d);
        this.mStepSize = (float) Math.pow(scale, 1.0d / d);
        this.mScreenCoord.set(screenCoord);
    }

    public boolean onStep() {
        this.mCoordinateHelper.scaleAboutScreenPoint(this.mScreenCoord, this.mStepSize);
        if (this.mStepSize > 1.0f) {
            if (this.mTargetScale <= this.mCoordinateHelper.getScale()) {
                this.mCoordinateHelper.setScaleAboutScreenPoint(this.mScreenCoord, this.mTargetScale);
                return false;
            }
        } else if (this.mTargetScale >= this.mCoordinateHelper.getScale()) {
            this.mCoordinateHelper.setScaleAboutScreenPoint(this.mScreenCoord, this.mTargetScale);
            return false;
        }
        return true;
    }
}
