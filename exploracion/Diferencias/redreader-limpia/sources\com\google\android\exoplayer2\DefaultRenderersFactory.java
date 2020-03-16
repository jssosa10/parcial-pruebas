package com.google.android.exoplayer2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.audio.AudioProcessor;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.metadata.MetadataRenderer;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.text.TextRenderer;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.google.android.exoplayer2.video.spherical.CameraMotionRenderer;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public class DefaultRenderersFactory implements RenderersFactory {
    public static final long DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS = 5000;
    public static final int EXTENSION_RENDERER_MODE_OFF = 0;
    public static final int EXTENSION_RENDERER_MODE_ON = 1;
    public static final int EXTENSION_RENDERER_MODE_PREFER = 2;
    protected static final int MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY = 50;
    private static final String TAG = "DefaultRenderersFactory";
    private final long allowedVideoJoiningTimeMs;
    private final Context context;
    @Nullable
    private final DrmSessionManager<FrameworkMediaCrypto> drmSessionManager;
    private final int extensionRendererMode;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface ExtensionRendererMode {
    }

    public DefaultRenderersFactory(Context context2) {
        this(context2, 0);
    }

    @Deprecated
    public DefaultRenderersFactory(Context context2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2) {
        this(context2, drmSessionManager2, 0);
    }

    public DefaultRenderersFactory(Context context2, int extensionRendererMode2) {
        this(context2, extensionRendererMode2, (long) DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS);
    }

    @Deprecated
    public DefaultRenderersFactory(Context context2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2, int extensionRendererMode2) {
        this(context2, drmSessionManager2, extensionRendererMode2, DEFAULT_ALLOWED_VIDEO_JOINING_TIME_MS);
    }

    public DefaultRenderersFactory(Context context2, int extensionRendererMode2, long allowedVideoJoiningTimeMs2) {
        this.context = context2;
        this.extensionRendererMode = extensionRendererMode2;
        this.allowedVideoJoiningTimeMs = allowedVideoJoiningTimeMs2;
        this.drmSessionManager = null;
    }

    @Deprecated
    public DefaultRenderersFactory(Context context2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2, int extensionRendererMode2, long allowedVideoJoiningTimeMs2) {
        this.context = context2;
        this.extensionRendererMode = extensionRendererMode2;
        this.allowedVideoJoiningTimeMs = allowedVideoJoiningTimeMs2;
        this.drmSessionManager = drmSessionManager2;
    }

    public Renderer[] createRenderers(Handler eventHandler, VideoRendererEventListener videoRendererEventListener, AudioRendererEventListener audioRendererEventListener, TextOutput textRendererOutput, MetadataOutput metadataRendererOutput, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2) {
        DrmSessionManager<FrameworkMediaCrypto> drmSessionManager3;
        if (drmSessionManager2 == null) {
            drmSessionManager3 = this.drmSessionManager;
        } else {
            drmSessionManager3 = drmSessionManager2;
        }
        ArrayList arrayList = new ArrayList();
        DrmSessionManager<FrameworkMediaCrypto> drmSessionManager4 = drmSessionManager3;
        buildVideoRenderers(this.context, drmSessionManager4, this.allowedVideoJoiningTimeMs, eventHandler, videoRendererEventListener, this.extensionRendererMode, arrayList);
        buildAudioRenderers(this.context, drmSessionManager4, buildAudioProcessors(), eventHandler, audioRendererEventListener, this.extensionRendererMode, arrayList);
        ArrayList arrayList2 = arrayList;
        buildTextRenderers(this.context, textRendererOutput, eventHandler.getLooper(), this.extensionRendererMode, arrayList2);
        buildMetadataRenderers(this.context, metadataRendererOutput, eventHandler.getLooper(), this.extensionRendererMode, arrayList2);
        buildCameraMotionRenderers(this.context, this.extensionRendererMode, arrayList);
        Handler handler = eventHandler;
        buildMiscellaneousRenderers(this.context, eventHandler, this.extensionRendererMode, arrayList);
        return (Renderer[]) arrayList.toArray(new Renderer[arrayList.size()]);
    }

    /* access modifiers changed from: protected */
    public void buildVideoRenderers(Context context2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2, long allowedVideoJoiningTimeMs2, Handler eventHandler, VideoRendererEventListener eventListener, int extensionRendererMode2, ArrayList<Renderer> out) {
        int extensionRendererIndex;
        int i = extensionRendererMode2;
        ArrayList<Renderer> arrayList = out;
        MediaCodecVideoRenderer mediaCodecVideoRenderer = new MediaCodecVideoRenderer(context2, MediaCodecSelector.DEFAULT, allowedVideoJoiningTimeMs2, drmSessionManager2, false, eventHandler, eventListener, 50);
        arrayList.add(mediaCodecVideoRenderer);
        if (i != 0) {
            int extensionRendererIndex2 = out.size();
            if (i == 2) {
                extensionRendererIndex = extensionRendererIndex2 - 1;
            } else {
                extensionRendererIndex = extensionRendererIndex2;
            }
            try {
                int extensionRendererIndex3 = extensionRendererIndex + 1;
                try {
                    arrayList.add(extensionRendererIndex, (Renderer) Class.forName("com.google.android.exoplayer2.ext.vp9.LibvpxVideoRenderer").getConstructor(new Class[]{Boolean.TYPE, Long.TYPE, Handler.class, VideoRendererEventListener.class, Integer.TYPE}).newInstance(new Object[]{Boolean.valueOf(true), Long.valueOf(allowedVideoJoiningTimeMs2), eventHandler, eventListener, Integer.valueOf(50)}));
                    Log.i(TAG, "Loaded LibvpxVideoRenderer.");
                } catch (ClassNotFoundException e) {
                } catch (Exception e2) {
                    e = e2;
                    int i2 = extensionRendererIndex3;
                    throw new RuntimeException("Error instantiating VP9 extension", e);
                }
            } catch (ClassNotFoundException e3) {
                int i3 = extensionRendererIndex;
            } catch (Exception e4) {
                e = e4;
                throw new RuntimeException("Error instantiating VP9 extension", e);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void buildAudioRenderers(Context context2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2, AudioProcessor[] audioProcessors, Handler eventHandler, AudioRendererEventListener eventListener, int extensionRendererMode2, ArrayList<Renderer> out) {
        int extensionRendererIndex;
        int extensionRendererIndex2;
        int extensionRendererIndex3;
        int i = extensionRendererMode2;
        ArrayList<Renderer> arrayList = out;
        MediaCodecAudioRenderer mediaCodecAudioRenderer = new MediaCodecAudioRenderer(context2, MediaCodecSelector.DEFAULT, drmSessionManager2, false, eventHandler, eventListener, AudioCapabilities.getCapabilities(context2), audioProcessors);
        arrayList.add(mediaCodecAudioRenderer);
        if (i != 0) {
            int extensionRendererIndex4 = out.size();
            if (i == 2) {
                extensionRendererIndex = extensionRendererIndex4 - 1;
            } else {
                extensionRendererIndex = extensionRendererIndex4;
            }
            try {
                extensionRendererIndex2 = extensionRendererIndex + 1;
                try {
                    arrayList.add(extensionRendererIndex, (Renderer) Class.forName("com.google.android.exoplayer2.ext.opus.LibopusAudioRenderer").getConstructor(new Class[]{Handler.class, AudioRendererEventListener.class, AudioProcessor[].class}).newInstance(new Object[]{eventHandler, eventListener, audioProcessors}));
                    Log.i(TAG, "Loaded LibopusAudioRenderer.");
                } catch (ClassNotFoundException e) {
                } catch (Exception e2) {
                    e = e2;
                    int i2 = extensionRendererIndex2;
                    throw new RuntimeException("Error instantiating Opus extension", e);
                }
            } catch (ClassNotFoundException e3) {
                extensionRendererIndex2 = extensionRendererIndex;
            } catch (Exception e4) {
                e = e4;
                throw new RuntimeException("Error instantiating Opus extension", e);
            }
            try {
                extensionRendererIndex3 = extensionRendererIndex2 + 1;
                try {
                    arrayList.add(extensionRendererIndex2, (Renderer) Class.forName("com.google.android.exoplayer2.ext.flac.LibflacAudioRenderer").getConstructor(new Class[]{Handler.class, AudioRendererEventListener.class, AudioProcessor[].class}).newInstance(new Object[]{eventHandler, eventListener, audioProcessors}));
                    Log.i(TAG, "Loaded LibflacAudioRenderer.");
                } catch (ClassNotFoundException e5) {
                } catch (Exception e6) {
                    e = e6;
                    int i3 = extensionRendererIndex3;
                    throw new RuntimeException("Error instantiating FLAC extension", e);
                }
            } catch (ClassNotFoundException e7) {
                extensionRendererIndex3 = extensionRendererIndex2;
            } catch (Exception e8) {
                e = e8;
                throw new RuntimeException("Error instantiating FLAC extension", e);
            }
            try {
                int extensionRendererIndex5 = extensionRendererIndex3 + 1;
                try {
                    arrayList.add(extensionRendererIndex3, (Renderer) Class.forName("com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer").getConstructor(new Class[]{Handler.class, AudioRendererEventListener.class, AudioProcessor[].class}).newInstance(new Object[]{eventHandler, eventListener, audioProcessors}));
                    Log.i(TAG, "Loaded FfmpegAudioRenderer.");
                } catch (ClassNotFoundException e9) {
                } catch (Exception e10) {
                    e = e10;
                    int i4 = extensionRendererIndex5;
                    throw new RuntimeException("Error instantiating FFmpeg extension", e);
                }
            } catch (ClassNotFoundException e11) {
                int i5 = extensionRendererIndex3;
            } catch (Exception e12) {
                e = e12;
                throw new RuntimeException("Error instantiating FFmpeg extension", e);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void buildTextRenderers(Context context2, TextOutput output, Looper outputLooper, int extensionRendererMode2, ArrayList<Renderer> out) {
        out.add(new TextRenderer(output, outputLooper));
    }

    /* access modifiers changed from: protected */
    public void buildMetadataRenderers(Context context2, MetadataOutput output, Looper outputLooper, int extensionRendererMode2, ArrayList<Renderer> out) {
        out.add(new MetadataRenderer(output, outputLooper));
    }

    /* access modifiers changed from: protected */
    public void buildCameraMotionRenderers(Context context2, int extensionRendererMode2, ArrayList<Renderer> out) {
        out.add(new CameraMotionRenderer());
    }

    /* access modifiers changed from: protected */
    public void buildMiscellaneousRenderers(Context context2, Handler eventHandler, int extensionRendererMode2, ArrayList<Renderer> arrayList) {
    }

    /* access modifiers changed from: protected */
    public AudioProcessor[] buildAudioProcessors() {
        return new AudioProcessor[0];
    }
}
