package org.quantumbadger.redreader.views.glview.displaylist;

import android.opengl.GLES20;
import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack;

public class RRGLRenderableBlend extends RRGLRenderableRenderHooks {
    public RRGLRenderableBlend(RRGLRenderable entity) {
        super(entity);
    }

    /* access modifiers changed from: protected */
    public void preRender(RRGLMatrixStack stack, long time) {
        GLES20.glEnable(3042);
        GLES20.glBlendFunc(770, 771);
    }

    /* access modifiers changed from: protected */
    public void postRender(RRGLMatrixStack stack, long time) {
        GLES20.glDisable(3042);
    }
}
