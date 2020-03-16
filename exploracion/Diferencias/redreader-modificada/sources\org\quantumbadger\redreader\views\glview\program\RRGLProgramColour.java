package org.quantumbadger.redreader.views.glview.program;

import android.opengl.GLES20;

public class RRGLProgramColour extends RRGLProgramVertices {
    private static final String fragmentShaderSource = "precision mediump float; \nuniform vec4 u_Color; \nvoid main() { \n  gl_FragColor = u_Color; \n} \n";
    private static final String vertexShaderSource = "uniform mat4 u_Matrix; \nuniform mat4 u_PixelMatrix; \nattribute vec4 a_Position; \nattribute vec2 a_TexCoordinate; \nvarying vec2 v_TexCoordinate; \nvoid main() {\n  v_TexCoordinate = a_TexCoordinate; \n  gl_Position = u_PixelMatrix * (u_Matrix * a_Position);\n} \n";
    private final int mColorHandle = getUniformHandle("u_Color");

    public RRGLProgramColour() {
        super(vertexShaderSource, fragmentShaderSource);
        setVertexBufferHandle(getAttributeHandle("a_Position"));
        setMatrixUniformHandle(getUniformHandle("u_Matrix"));
        setPixelMatrixHandle(getUniformHandle("u_PixelMatrix"));
    }

    public void activateColour(float r, float g, float b, float a) {
        GLES20.glUniform4f(this.mColorHandle, r, g, b, a);
    }

    public void onActivated() {
        super.onActivated();
        GLES20.glEnableVertexAttribArray(this.mColorHandle);
    }

    public void onDeactivated() {
        super.onDeactivated();
        GLES20.glDisableVertexAttribArray(this.mColorHandle);
    }
}
