package org.quantumbadger.redreader.views.glview.displaylist;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import org.quantumbadger.redreader.views.glview.program.RRGLContext;
import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack;
import org.quantumbadger.redreader.views.glview.program.RRGLTexture;

public class RRGLRenderableTexturedQuad extends RRGLRenderable {
    private static final FloatBuffer mUVBuffer = ByteBuffer.allocateDirect(uvData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static final FloatBuffer mVertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    private static final float[] uvData = {0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f};
    private static final float[] vertexData = {0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f};
    private final RRGLContext mGLContext;
    private RRGLTexture mTexture;

    static {
        mVertexBuffer.put(vertexData).position(0);
        mUVBuffer.put(uvData).position(0);
    }

    public RRGLRenderableTexturedQuad(RRGLContext glContext, RRGLTexture texture) {
        this.mGLContext = glContext;
        this.mTexture = texture;
    }

    public void setTexture(RRGLTexture newTexture) {
        if (isAdded()) {
            this.mTexture.releaseReference();
        }
        this.mTexture = newTexture;
        if (isAdded()) {
            this.mTexture.addReference();
        }
    }

    public void onAdded() {
        super.onAdded();
        this.mTexture.addReference();
    }

    public void onRemoved() {
        this.mTexture.releaseReference();
        super.onRemoved();
    }

    /* access modifiers changed from: protected */
    public void renderInternal(RRGLMatrixStack matrixStack, long time) {
        this.mGLContext.activateProgramTexture();
        this.mTexture.activate();
        matrixStack.flush();
        this.mGLContext.activateVertexBuffer(mVertexBuffer);
        this.mGLContext.activateUVBuffer(mUVBuffer);
        this.mGLContext.drawTriangleStrip(4);
    }
}
