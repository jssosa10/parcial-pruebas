package org.quantumbadger.redreader.views.imageview;

import org.quantumbadger.redreader.common.MutableFloatPoint2D;

public class BoundsHelper {
    private final CoordinateHelper mCoordinateHelper;
    private final int mImageResolutionX;
    private final int mImageResolutionY;
    private final float mMinScale = Math.min(((float) this.mResolutionX) / ((float) this.mImageResolutionX), ((float) this.mResolutionY) / ((float) this.mImageResolutionY));
    private final int mResolutionX;
    private final int mResolutionY;

    public BoundsHelper(int resolutionX, int resolutionY, int imageResolutionX, int imageResolutionY, CoordinateHelper coordinateHelper) {
        this.mResolutionX = resolutionX;
        this.mResolutionY = resolutionY;
        this.mImageResolutionX = imageResolutionX;
        this.mImageResolutionY = imageResolutionY;
        this.mCoordinateHelper = coordinateHelper;
    }

    public void applyMinScale() {
        this.mCoordinateHelper.setScale(this.mMinScale);
    }

    public boolean isMinScale() {
        return this.mCoordinateHelper.getScale() - 1.0E-6f <= this.mMinScale;
    }

    public void applyBounds() {
        if (this.mCoordinateHelper.getScale() < this.mMinScale) {
            applyMinScale();
        }
        float scale = this.mCoordinateHelper.getScale();
        MutableFloatPoint2D posOffset = this.mCoordinateHelper.getPositionOffset();
        float scaledImageWidth = ((float) this.mImageResolutionX) * scale;
        float scaledImageHeight = ((float) this.mImageResolutionY) * scale;
        int i = this.mResolutionX;
        if (scaledImageWidth <= ((float) i)) {
            posOffset.x = (((float) i) - scaledImageWidth) / 2.0f;
        } else if (posOffset.x > 0.0f) {
            posOffset.x = 0.0f;
        } else {
            float f = posOffset.x;
            int i2 = this.mResolutionX;
            if (f < ((float) i2) - scaledImageWidth) {
                posOffset.x = ((float) i2) - scaledImageWidth;
            }
        }
        int i3 = this.mResolutionY;
        if (scaledImageHeight <= ((float) i3)) {
            posOffset.y = (((float) i3) - scaledImageHeight) / 2.0f;
        } else if (posOffset.y > 0.0f) {
            posOffset.y = 0.0f;
        } else {
            float f2 = posOffset.y;
            int i4 = this.mResolutionY;
            if (f2 < ((float) i4) - scaledImageHeight) {
                posOffset.y = ((float) i4) - scaledImageHeight;
            }
        }
    }

    public float getMinScale() {
        return this.mMinScale;
    }
}
