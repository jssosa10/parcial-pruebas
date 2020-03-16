package org.quantumbadger.redreader.views.glview.program;

import android.content.Context;
import android.opengl.GLES20;
import java.nio.FloatBuffer;
import org.quantumbadger.redreader.common.General;

public final class RRGLContext {
    private final Context mContext;
    private float[] mPixelMatrix;
    private int mPixelMatrixOffset;
    private final RRGLProgramColour mProgramColour = new RRGLProgramColour();
    private RRGLProgramVertices mProgramCurrent;
    private final RRGLProgramTexture mProgramTexture = new RRGLProgramTexture();

    public RRGLContext(Context context) {
        this.mContext = context;
    }

    public int dpToPixels(float dp) {
        return General.dpToPixels(this.mContext, dp);
    }

    public float getScreenDensity() {
        return this.mContext.getResources().getDisplayMetrics().density;
    }

    public void activateProgramColour() {
        RRGLProgramVertices rRGLProgramVertices = this.mProgramCurrent;
        RRGLProgramColour rRGLProgramColour = this.mProgramColour;
        if (rRGLProgramVertices != rRGLProgramColour) {
            activateProgram(rRGLProgramColour);
        }
    }

    public void activateProgramTexture() {
        RRGLProgramVertices rRGLProgramVertices = this.mProgramCurrent;
        RRGLProgramTexture rRGLProgramTexture = this.mProgramTexture;
        if (rRGLProgramVertices != rRGLProgramTexture) {
            activateProgram(rRGLProgramTexture);
        }
    }

    private void activateProgram(RRGLProgramVertices program) {
        RRGLProgramVertices rRGLProgramVertices = this.mProgramCurrent;
        if (rRGLProgramVertices != null) {
            rRGLProgramVertices.onDeactivated();
        }
        GLES20.glUseProgram(program.getHandle());
        this.mProgramCurrent = program;
        program.onActivated();
        float[] fArr = this.mPixelMatrix;
        if (fArr != null) {
            program.activatePixelMatrix(fArr, this.mPixelMatrixOffset);
        }
    }

    /* access modifiers changed from: 0000 */
    public void activateTextureByHandle(int textureHandle) {
        this.mProgramTexture.activateTextureByHandle(textureHandle);
    }

    public void activateVertexBuffer(FloatBuffer vertexBuffer) {
        this.mProgramCurrent.activateVertexBuffer(vertexBuffer);
    }

    public void activateColour(float r, float g, float b, float a) {
        this.mProgramColour.activateColour(r, g, b, a);
    }

    public void activateUVBuffer(FloatBuffer uvBuffer) {
        this.mProgramTexture.activateUVBuffer(uvBuffer);
    }

    public void drawTriangleStrip(int vertices) {
        this.mProgramCurrent.drawTriangleStrip(vertices);
    }

    public void activateMatrix(float[] buf, int offset) {
        this.mProgramCurrent.activateMatrix(buf, offset);
    }

    public void activatePixelMatrix(float[] buf, int offset) {
        this.mPixelMatrix = buf;
        this.mPixelMatrixOffset = offset;
        RRGLProgramVertices rRGLProgramVertices = this.mProgramCurrent;
        if (rRGLProgramVertices != null) {
            rRGLProgramVertices.activatePixelMatrix(buf, offset);
        }
    }

    public void setClearColor(float r, float g, float b, float a) {
        GLES20.glClearColor(r, g, b, a);
    }

    public void clear() {
        GLES20.glClear(16640);
    }

    public void setViewport(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }
}
