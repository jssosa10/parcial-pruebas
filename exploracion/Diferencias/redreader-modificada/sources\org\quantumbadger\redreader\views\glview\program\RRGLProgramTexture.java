package org.quantumbadger.redreader.views.glview.program;

import android.opengl.GLES20;
import java.nio.FloatBuffer;

public class RRGLProgramTexture extends RRGLProgramVertices {
    private static final String fragmentShaderSource = "precision mediump float; \nuniform sampler2D u_Texture; \nvarying vec2 v_TexCoordinate; \nvoid main() { \n  gl_FragColor = texture2D(u_Texture, v_TexCoordinate); \n} \n";
    private static final String vertexShaderSource = "uniform mat4 u_Matrix; \nuniform mat4 u_PixelMatrix; \nattribute vec4 a_Position; \nattribute vec2 a_TexCoordinate; \nvarying vec2 v_TexCoordinate; \nvoid main() {\n  v_TexCoordinate = a_TexCoordinate; \n  gl_Position = u_PixelMatrix * (u_Matrix * a_Position);\n} \n";
    private final int mTextureUniformHandle = getUniformHandle("u_Texture");
    private final int mUVDataHandle = getAttributeHandle("a_TexCoordinate");

    public RRGLProgramTexture() {
        super(vertexShaderSource, fragmentShaderSource);
        setVertexBufferHandle(getAttributeHandle("a_Position"));
        setMatrixUniformHandle(getUniformHandle("u_Matrix"));
        setPixelMatrixHandle(getUniformHandle("u_PixelMatrix"));
    }

    public void activateTextureByHandle(int textureHandle) {
        GLES20.glBindTexture(3553, textureHandle);
        GLES20.glUniform1i(this.mTextureUniformHandle, 0);
    }

    public void activateUVBuffer(FloatBuffer uvBuffer) {
        GLES20.glVertexAttribPointer(this.mUVDataHandle, 2, 5126, false, 0, uvBuffer);
    }

    public void onActivated() {
        super.onActivated();
        GLES20.glEnableVertexAttribArray(this.mUVDataHandle);
        GLES20.glActiveTexture(33984);
    }

    public void onDeactivated() {
        super.onDeactivated();
        GLES20.glDisableVertexAttribArray(this.mUVDataHandle);
        GLES20.glBindTexture(3553, 0);
    }
}
