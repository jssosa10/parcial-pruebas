package com.google.android.exoplayer2;

import com.google.android.exoplayer2.PlayerMessage.Target;
import com.google.android.exoplayer2.source.SampleStream;
import com.google.android.exoplayer2.util.MediaClock;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface Renderer extends Target {
    public static final int STATE_DISABLED = 0;
    public static final int STATE_ENABLED = 1;
    public static final int STATE_STARTED = 2;

    /* renamed from: com.google.android.exoplayer2.Renderer$-CC reason: invalid class name */
    public final /* synthetic */ class CC {
        public static void $default$setOperatingRate(Renderer renderer, float operatingRate) throws ExoPlaybackException {
        }
    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
    }

    void disable();

    void enable(RendererConfiguration rendererConfiguration, Format[] formatArr, SampleStream sampleStream, long j, boolean z, long j2) throws ExoPlaybackException;

    RendererCapabilities getCapabilities();

    MediaClock getMediaClock();

    int getState();

    SampleStream getStream();

    int getTrackType();

    boolean hasReadStreamToEnd();

    boolean isCurrentStreamFinal();

    boolean isEnded();

    boolean isReady();

    void maybeThrowStreamError() throws IOException;

    void render(long j, long j2) throws ExoPlaybackException;

    void replaceStream(Format[] formatArr, SampleStream sampleStream, long j) throws ExoPlaybackException;

    void resetPosition(long j) throws ExoPlaybackException;

    void setCurrentStreamFinal();

    void setIndex(int i);

    void setOperatingRate(float f) throws ExoPlaybackException;

    void start() throws ExoPlaybackException;

    void stop() throws ExoPlaybackException;
}
