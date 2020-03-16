package org.quantumbadger.redreader.views.imageview;

import org.quantumbadger.redreader.common.MutableFloatPoint2D;

public class CoordinateHelper {
    private final MutableFloatPoint2D mPositionOffset = new MutableFloatPoint2D();
    private float mScale = 1.0f;

    public void setScale(float scale) {
        this.mScale = scale;
    }

    public float getScale() {
        return this.mScale;
    }

    public MutableFloatPoint2D getPositionOffset() {
        return this.mPositionOffset;
    }

    public void getPositionOffset(MutableFloatPoint2D result) {
        result.set(this.mPositionOffset);
    }

    public void convertScreenToScene(MutableFloatPoint2D screenPos, MutableFloatPoint2D output) {
        output.x = (screenPos.x - this.mPositionOffset.x) / this.mScale;
        output.y = (screenPos.y - this.mPositionOffset.y) / this.mScale;
    }

    public void convertSceneToScreen(MutableFloatPoint2D scenePos, MutableFloatPoint2D output) {
        output.x = (scenePos.x * this.mScale) + this.mPositionOffset.x;
        output.y = (scenePos.y * this.mScale) + this.mPositionOffset.y;
    }

    public void scaleAboutScreenPoint(MutableFloatPoint2D screenPos, float scaleFactor) {
        setScaleAboutScreenPoint(screenPos, this.mScale * scaleFactor);
    }

    public void setScaleAboutScreenPoint(MutableFloatPoint2D screenPos, float scale) {
        MutableFloatPoint2D oldScenePos = new MutableFloatPoint2D();
        convertScreenToScene(screenPos, oldScenePos);
        this.mScale = scale;
        MutableFloatPoint2D newScreenPos = new MutableFloatPoint2D();
        convertSceneToScreen(oldScenePos, newScreenPos);
        translateScreen(newScreenPos, screenPos);
    }

    public void translateScreen(MutableFloatPoint2D oldScreenPos, MutableFloatPoint2D newScreenPos) {
        this.mPositionOffset.add(newScreenPos);
        this.mPositionOffset.sub(oldScreenPos);
    }
}
