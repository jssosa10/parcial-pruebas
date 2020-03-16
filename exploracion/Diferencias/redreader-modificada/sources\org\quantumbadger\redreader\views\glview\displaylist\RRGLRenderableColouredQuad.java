package org.quantumbadger.redreader.views.glview.displaylist;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import org.quantumbadger.redreader.views.glview.program.RRGLContext;
import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack;

public class RRGLRenderableColouredQuad extends RRGLRenderable {
    private static final FloatBuffer mVertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static final float[] vertexData = {0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f};
    private float mAlpha;
    private float mBlue;
    private final RRGLContext mGLContext;
    private float mGreen;
    private float mOverallAlpha = 1.0f;
    private float mRed;

    static {
        mVertexBuffer.put(vertexData).position(0);
    }

    public RRGLRenderableColouredQuad(RRGLContext glContext) {
        this.mGLContext = glContext;
    }

    public void setColour(float r, float g, float b, float a) {
        this.mRed = r;
        this.mGreen = g;
        this.mBlue = b;
        this.mAlpha = a;
    }

    public void setOverallAlpha(float alpha) {
        this.mOverallAlpha = alpha;
    }

    /* access modifiers changed from: protected */
    public void renderInternal(RRGLMatrixStack matrixStack, long time) {
        this.mGLContext.activateProgramColour();
        matrixStack.flush();
        this.mGLContext.activateVertexBuffer(mVertexBuffer);
        this.mGLContext.activateColour(this.mRed, this.mGreen, this.mBlue, this.mAlpha * this.mOverallAlpha);
        this.mGLContext.drawTriangleStrip(4);
    }
}
