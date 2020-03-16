package org.quantumbadger.redreader.views.glview.program;

import android.opengl.GLES20;
import java.util.Locale;

public abstract class RRGLProgram {
    private Integer mFragmentShaderHandle = null;
    private final int mHandle = GLES20.glCreateProgram();
    private Integer mVertexShaderHandle = null;

    public abstract void onActivated();

    public abstract void onDeactivated();

    public RRGLProgram(String vertexShaderSource, String fragmentShaderSource) {
        if (this.mHandle != 0) {
            compileAndAttachShader(35633, vertexShaderSource);
            compileAndAttachShader(35632, fragmentShaderSource);
            link();
            return;
        }
        throw new RuntimeException("Error creating program.");
    }

    private void compileAndAttachShader(int type, String source) {
        switch (type) {
            case 35632:
                if (this.mFragmentShaderHandle != null) {
                    throw new RuntimeException();
                }
                break;
            case 35633:
                if (this.mVertexShaderHandle != null) {
                    throw new RuntimeException();
                }
                break;
            default:
                throw new RuntimeException("Unknown shader type.");
        }
        int shaderHandle = GLES20.glCreateShader(type);
        if (shaderHandle != 0) {
            GLES20.glShaderSource(shaderHandle, source);
            GLES20.glCompileShader(shaderHandle);
            int[] compileStatus = new int[1];
            GLES20.glGetShaderiv(shaderHandle, 35713, compileStatus, 0);
            if (compileStatus[0] != 0) {
                GLES20.glAttachShader(this.mHandle, shaderHandle);
                switch (type) {
                    case 35632:
                        this.mFragmentShaderHandle = Integer.valueOf(shaderHandle);
                        return;
                    case 35633:
                        this.mVertexShaderHandle = Integer.valueOf(shaderHandle);
                        return;
                    default:
                        throw new RuntimeException("Unknown shader type.");
                }
            } else {
                String log = GLES20.glGetShaderInfoLog(this.mHandle);
                GLES20.glDeleteShader(shaderHandle);
                throw new RuntimeException(String.format(Locale.US, "Shader compile error: \"%s\".", new Object[]{log}));
            }
        } else {
            throw new RuntimeException("Error creating shader.");
        }
    }

    private void link() {
        if (this.mFragmentShaderHandle == null || this.mVertexShaderHandle == null) {
            throw new RuntimeException();
        }
        GLES20.glLinkProgram(this.mHandle);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(this.mHandle, 35714, linkStatus, 0);
        if (linkStatus[0] != 0) {
            GLES20.glDetachShader(this.mHandle, this.mFragmentShaderHandle.intValue());
            GLES20.glDetachShader(this.mHandle, this.mVertexShaderHandle.intValue());
            GLES20.glDeleteShader(this.mFragmentShaderHandle.intValue());
            GLES20.glDeleteShader(this.mVertexShaderHandle.intValue());
            this.mFragmentShaderHandle = null;
            this.mVertexShaderHandle = null;
            return;
        }
        String log = GLES20.glGetProgramInfoLog(this.mHandle);
        GLES20.glDeleteProgram(this.mHandle);
        throw new RuntimeException(String.format(Locale.US, "Linker error: \"%s\".", new Object[]{log}));
    }

    public int getAttributeHandle(String name) {
        return GLES20.glGetAttribLocation(this.mHandle, name);
    }

    public int getUniformHandle(String name) {
        return GLES20.glGetUniformLocation(this.mHandle, name);
    }

    public int getHandle() {
        return this.mHandle;
    }
}
