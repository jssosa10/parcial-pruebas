package org.quantumbadger.redreader.views.glview.displaylist;

import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack;

public class RRGLRenderableScale extends RRGLRenderableRenderHooks {
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;

    public RRGLRenderableScale(RRGLRenderable entity) {
        super(entity);
    }

    public void setScale(float x, float y) {
        this.mScaleX = x;
        this.mScaleY = y;
    }

    /* access modifiers changed from: protected */
    public void preRender(RRGLMatrixStack stack, long time) {
        stack.pushAndScale(this.mScaleX, this.mScaleY);
    }

    /* access modifiers changed from: protected */
    public void postRender(RRGLMatrixStack stack, long time) {
        stack.pop();
    }
}
