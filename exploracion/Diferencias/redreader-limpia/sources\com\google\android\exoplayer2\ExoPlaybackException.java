package com.google.android.exoplayer2;

import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class ExoPlaybackException extends Exception {
    public static final int TYPE_RENDERER = 1;
    public static final int TYPE_SOURCE = 0;
    public static final int TYPE_UNEXPECTED = 2;
    private final Throwable cause;
    public final int rendererIndex;
    public final int type;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }

    public static ExoPlaybackException createForSource(IOException cause2) {
        return new ExoPlaybackException(0, cause2, -1);
    }

    public static ExoPlaybackException createForRenderer(Exception cause2, int rendererIndex2) {
        return new ExoPlaybackException(1, cause2, rendererIndex2);
    }

    static ExoPlaybackException createForUnexpected(RuntimeException cause2) {
        return new ExoPlaybackException(2, cause2, -1);
    }

    private ExoPlaybackException(int type2, Throwable cause2, int rendererIndex2) {
        super(cause2);
        this.type = type2;
        this.cause = cause2;
        this.rendererIndex = rendererIndex2;
    }

    public IOException getSourceException() {
        Assertions.checkState(this.type == 0);
        return (IOException) this.cause;
    }

    public Exception getRendererException() {
        boolean z = true;
        if (this.type != 1) {
            z = false;
        }
        Assertions.checkState(z);
        return (Exception) this.cause;
    }

    public RuntimeException getUnexpectedException() {
        Assertions.checkState(this.type == 2);
        return (RuntimeException) this.cause;
    }
}
