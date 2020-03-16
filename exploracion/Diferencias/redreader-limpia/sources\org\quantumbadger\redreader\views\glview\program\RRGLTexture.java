package org.quantumbadger.redreader.views.glview.program;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class RRGLTexture {
    private final RRGLContext mGLContext;
    private int mRefCount = 1;
    private final int mTextureHandle;

    public RRGLTexture(RRGLContext glContext, Bitmap bitmap) {
        this.mTextureHandle = loadTexture(bitmap);
        this.mGLContext = glContext;
    }

    public void addReference() {
        this.mRefCount++;
    }

    public void releaseReference() {
        this.mRefCount--;
        if (this.mRefCount == 0) {
            deleteTexture(this.mTextureHandle);
        }
    }

    public void activate() {
        this.mGLContext.activateTextureByHandle(this.mTextureHandle);
    }

    private static int loadTexture(Bitmap bitmap) {
        int[] textureHandle = new int[1];
        GLES20.glGenTextures(1, textureHandle, 0);
        if (textureHandle[0] != 0) {
            GLES20.glBindTexture(3553, textureHandle[0]);
            GLES20.glTexParameteri(3553, 10241, 9729);
            GLES20.glTexParameteri(3553, 10240, 9729);
            GLES20.glTexParameteri(3553, 10242, 33071);
            GLES20.glTexParameteri(3553, 10243, 33071);
            GLUtils.texImage2D(3553, 0, bitmap, 0);
            return textureHandle[0];
        }
        throw new RuntimeException("OpenGL error: glGenTextures failed.");
    }

    private static void deleteTexture(int handle) {
        GLES20.glDeleteTextures(1, new int[]{handle}, 0);
    }
}
