package org.quantumbadger.redreader.views.glview.displaylist;

import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack;

public abstract class RRGLRenderable {
    private int mAttachmentCount = 0;
    private boolean mVisible = true;

    /* access modifiers changed from: protected */
    public abstract void renderInternal(RRGLMatrixStack rRGLMatrixStack, long j);

    public final void hide() {
        this.mVisible = false;
    }

    public final void show() {
        this.mVisible = true;
    }

    public final boolean isVisible() {
        return this.mVisible;
    }

    public final void startRender(RRGLMatrixStack stack, long time) {
        if (this.mVisible) {
            renderInternal(stack, time);
        }
    }

    public void onAdded() {
        this.mAttachmentCount++;
    }

    public boolean isAdded() {
        return this.mAttachmentCount > 0;
    }

    public void onRemoved() {
        this.mAttachmentCount--;
    }

    public boolean isAnimating() {
        return false;
    }

    public void setOverallAlpha(float alpha) {
        throw new UnsupportedOperationException();
    }
}
