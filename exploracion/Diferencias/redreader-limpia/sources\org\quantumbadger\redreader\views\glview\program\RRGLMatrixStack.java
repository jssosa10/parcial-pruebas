package org.quantumbadger.redreader.views.glview.program;

import android.opengl.Matrix;

public class RRGLMatrixStack {
    private final RRGLContext mGLContext;
    private final float[] mMatrices = new float[2048];
    private int mTopMatrixPos = 0;

    public RRGLMatrixStack(RRGLContext glContext) {
        this.mGLContext = glContext;
        setIdentity();
    }

    public int pushAndTranslate(float offsetX, float offsetY) {
        this.mTopMatrixPos += 16;
        float[] fArr = this.mMatrices;
        int i = this.mTopMatrixPos;
        Matrix.translateM(fArr, i, fArr, i - 16, offsetX, offsetY, 0.0f);
        return this.mTopMatrixPos - 16;
    }

    public int pushAndScale(float factorX, float factorY) {
        this.mTopMatrixPos += 16;
        float[] fArr = this.mMatrices;
        int i = this.mTopMatrixPos;
        Matrix.scaleM(fArr, i, fArr, i - 16, factorX, factorY, 0.0f);
        return this.mTopMatrixPos - 16;
    }

    public int pop() {
        this.mTopMatrixPos -= 16;
        return this.mTopMatrixPos;
    }

    public void setIdentity() {
        Matrix.setIdentityM(this.mMatrices, this.mTopMatrixPos);
    }

    public void scale(float factorX, float factorY, float factorZ) {
        Matrix.scaleM(this.mMatrices, this.mTopMatrixPos, factorX, factorY, factorZ);
    }

    public void flush() {
        this.mGLContext.activateMatrix(this.mMatrices, this.mTopMatrixPos);
    }

    public void assertAtRoot() {
        if (this.mTopMatrixPos == 0) {
            for (int i = 0; i < 16; i++) {
                if (i == 0 || i == 5 || i == 10 || i == 15) {
                    if (this.mMatrices[i] != 1.0f) {
                        throw new RuntimeException("Root matrix is not identity!");
                    }
                } else if (this.mMatrices[i] != 0.0f) {
                    throw new RuntimeException("Root matrix is not identity!");
                }
            }
            return;
        }
        throw new RuntimeException("assertAtRoot() failed!");
    }
}
