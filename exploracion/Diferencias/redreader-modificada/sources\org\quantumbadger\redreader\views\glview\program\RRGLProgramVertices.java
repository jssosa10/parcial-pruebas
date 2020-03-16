package org.quantumbadger.redreader.views.glview.program;

import android.opengl.GLES20;
import java.nio.FloatBuffer;

public abstract class RRGLProgramVertices extends RRGLProgram {
    private int mMatrixUniformHandle;
    private int mPixelMatrixUniformHandle;
    private int mVertexBufferHandle;

    public RRGLProgramVertices(String vertexShaderSource, String fragmentShaderSource) {
        super(vertexShaderSource, fragmentShaderSource);
    }

    public final void activateVertexBuffer(FloatBuffer vertexBuffer) {
        GLES20.glVertexAttribPointer(this.mVertexBufferHandle, 3, 5126, false, 0, vertexBuffer);
    }

    public final void drawTriangleStrip(int vertices) {
        GLES20.glDrawArrays(5, 0, vertices);
    }

    /* access modifiers changed from: protected */
    public final void setVertexBufferHandle(int handle) {
        this.mVertexBufferHandle = handle;
    }

    /* access modifiers changed from: protected */
    public final void setMatrixUniformHandle(int handle) {
        this.mMatrixUniformHandle = handle;
    }

    /* access modifiers changed from: protected */
    public final void setPixelMatrixHandle(int handle) {
        this.mPixelMatrixUniformHandle = handle;
    }

    public final void activateMatrix(float[] buf, int offset) {
        GLES20.glUniformMatrix4fv(this.mMatrixUniformHandle, 1, false, buf, offset);
    }

    public final void activatePixelMatrix(float[] buf, int offset) {
        GLES20.glUniformMatrix4fv(this.mPixelMatrixUniformHandle, 1, false, buf, offset);
    }

    public void onActivated() {
        GLES20.glEnableVertexAttribArray(this.mVertexBufferHandle);
    }

    public void onDeactivated() {
        GLES20.glDisableVertexAttribArray(this.mVertexBufferHandle);
    }
}
