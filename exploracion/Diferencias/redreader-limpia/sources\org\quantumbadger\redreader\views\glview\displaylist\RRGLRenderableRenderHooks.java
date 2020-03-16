package org.quantumbadger.redreader.views.glview.displaylist;

import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack;

public abstract class RRGLRenderableRenderHooks extends RRGLRenderable {
    private final RRGLRenderable mEntity;

    /* access modifiers changed from: protected */
    public abstract void postRender(RRGLMatrixStack rRGLMatrixStack, long j);

    /* access modifiers changed from: protected */
    public abstract void preRender(RRGLMatrixStack rRGLMatrixStack, long j);

    public RRGLRenderableRenderHooks(RRGLRenderable entity) {
        this.mEntity = entity;
    }

    /* access modifiers changed from: protected */
    public void renderInternal(RRGLMatrixStack stack, long time) {
        preRender(stack, time);
        this.mEntity.startRender(stack, time);
        postRender(stack, time);
    }

    public void onAdded() {
        this.mEntity.onAdded();
        super.onAdded();
    }

    public void onRemoved() {
        super.onRemoved();
        this.mEntity.onRemoved();
    }

    public boolean isAnimating() {
        return this.mEntity.isAnimating();
    }

    public void setOverallAlpha(float alpha) {
        this.mEntity.setOverallAlpha(alpha);
    }
}
