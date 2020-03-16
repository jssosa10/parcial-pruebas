package com.google.android.exoplayer2.ui.spherical;

import android.annotation.TargetApi;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.text.TextUtils;
import com.google.android.exoplayer2.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.apache.commons.lang3.StringUtils;

final class GlUtil {
    private static final String TAG = "Spherical.Utils";

    private GlUtil() {
    }

    public static void checkGlError() {
        int error = GLES20.glGetError();
        if (error != 0) {
            do {
                int lastError = error;
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("glError ");
                sb.append(GLU.gluErrorString(lastError));
                Log.e(str, sb.toString());
                error = GLES20.glGetError();
            } while (error != 0);
        }
    }

    public static int compileProgram(String[] vertexCode, String[] fragmentCode) {
        checkGlError();
        int vertexShader = GLES20.glCreateShader(35633);
        GLES20.glShaderSource(vertexShader, TextUtils.join(StringUtils.LF, vertexCode));
        GLES20.glCompileShader(vertexShader);
        checkGlError();
        int fragmentShader = GLES20.glCreateShader(35632);
        GLES20.glShaderSource(fragmentShader, TextUtils.join(StringUtils.LF, fragmentCode));
        GLES20.glCompileShader(fragmentShader);
        checkGlError();
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, 35714, linkStatus, 0);
        if (linkStatus[0] != 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unable to link shader program: \n");
            sb.append(GLES20.glGetProgramInfoLog(program));
            Log.e(TAG, sb.toString());
        }
        checkGlError();
        return program;
    }

    public static FloatBuffer createBuffer(float[] data) {
        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer buffer = bb.asFloatBuffer();
        buffer.put(data);
        buffer.position(0);
        return buffer;
    }

    @TargetApi(15)
    public static int createExternalTexture() {
        int[] texId = new int[1];
        GLES20.glGenTextures(1, IntBuffer.wrap(texId));
        GLES20.glBindTexture(36197, texId[0]);
        GLES20.glTexParameteri(36197, 10241, 9729);
        GLES20.glTexParameteri(36197, 10240, 9729);
        GLES20.glTexParameteri(36197, 10242, 33071);
        GLES20.glTexParameteri(36197, 10243, 33071);
        checkGlError();
        return texId[0];
    }
}
