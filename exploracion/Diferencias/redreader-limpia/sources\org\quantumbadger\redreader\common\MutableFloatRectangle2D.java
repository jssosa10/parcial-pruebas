package org.quantumbadger.redreader.common;

public class MutableFloatRectangle2D {
    public final MutableFloatPoint2D mBottomRight = new MutableFloatPoint2D();
    public final MutableFloatPoint2D mTopLeft = new MutableFloatPoint2D();

    public boolean intersect(MutableFloatRectangle2D other) {
        return this.mTopLeft.x <= other.mBottomRight.x && this.mTopLeft.y <= other.mBottomRight.y && other.mTopLeft.x <= this.mBottomRight.x && other.mTopLeft.y <= this.mBottomRight.y;
    }
}
