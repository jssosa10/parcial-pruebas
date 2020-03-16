package org.quantumbadger.redreader.views.glview.displaylist;

import org.quantumbadger.redreader.common.MutableFloatPoint2D;
import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack;

public class RRGLRenderableTranslation extends RRGLRenderableRenderHooks {
    private float mPositionX;
    private float mPositionY;

    public RRGLRenderableTranslation(RRGLRenderable entity) {
        super(entity);
    }

    public void setPosition(float x, float y) {
        this.mPositionX = x;
        this.mPositionY = y;
    }

    /* access modifiers changed from: protected */
    public void preRender(RRGLMatrixStack stack, long time) {
        stack.pushAndTranslate(this.mPositionX, this.mPositionY);
    }

    /* access modifiers changed from: protected */
    public void postRender(RRGLMatrixStack stack, long time) {
        stack.pop();
    }

    public void setPosition(MutableFloatPoint2D mPositionOffset) {
        this.mPositionX = mPositionOffset.x;
        this.mPositionY = mPositionOffset.y;
    }
}
