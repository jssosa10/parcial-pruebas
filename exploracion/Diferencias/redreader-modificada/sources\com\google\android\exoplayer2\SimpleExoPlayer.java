package com.google.android.exoplayer2;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.PlaybackParams;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import com.google.android.exoplayer2.ExoPlayer.ExoPlayerMessage;
import com.google.android.exoplayer2.Player.AudioComponent;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.Player.TextComponent;
import com.google.android.exoplayer2.Player.VideoComponent;
import com.google.android.exoplayer2.PlayerMessage.Target;
import com.google.android.exoplayer2.analytics.AnalyticsCollector;
import com.google.android.exoplayer2.analytics.AnalyticsCollector.Factory;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.audio.AudioAttributes.Builder;
import com.google.android.exoplayer2.audio.AudioFocusManager;
import com.google.android.exoplayer2.audio.AudioFocusManager.PlayerControl;
import com.google.android.exoplayer2.audio.AudioListener;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.AuxEffectInfo;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataOutput;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoFrameMetadataListener;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.google.android.exoplayer2.video.spherical.CameraMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

@TargetApi(16)
public class SimpleExoPlayer extends BasePlayer implements ExoPlayer, AudioComponent, VideoComponent, TextComponent {
    private static final String TAG = "SimpleExoPlayer";
    private final AnalyticsCollector analyticsCollector;
    private AudioAttributes audioAttributes;
    /* access modifiers changed from: private */
    public final CopyOnWriteArraySet<AudioRendererEventListener> audioDebugListeners;
    /* access modifiers changed from: private */
    public DecoderCounters audioDecoderCounters;
    private final AudioFocusManager audioFocusManager;
    /* access modifiers changed from: private */
    public Format audioFormat;
    /* access modifiers changed from: private */
    public final CopyOnWriteArraySet<AudioListener> audioListeners;
    /* access modifiers changed from: private */
    public int audioSessionId;
    private float audioVolume;
    private final BandwidthMeter bandwidthMeter;
    private CameraMotionListener cameraMotionListener;
    private final ComponentListener componentListener;
    /* access modifiers changed from: private */
    public List<Cue> currentCues;
    private final Handler eventHandler;
    private boolean hasNotifiedFullWrongThreadWarning;
    private MediaSource mediaSource;
    /* access modifiers changed from: private */
    public final CopyOnWriteArraySet<MetadataOutput> metadataOutputs;
    private boolean ownsSurface;
    private final ExoPlayerImpl player;
    protected final Renderer[] renderers;
    /* access modifiers changed from: private */
    public Surface surface;
    private int surfaceHeight;
    private SurfaceHolder surfaceHolder;
    private int surfaceWidth;
    /* access modifiers changed from: private */
    public final CopyOnWriteArraySet<TextOutput> textOutputs;
    private TextureView textureView;
    /* access modifiers changed from: private */
    public final CopyOnWriteArraySet<VideoRendererEventListener> videoDebugListeners;
    /* access modifiers changed from: private */
    public DecoderCounters videoDecoderCounters;
    /* access modifiers changed from: private */
    public Format videoFormat;
    private VideoFrameMetadataListener videoFrameMetadataListener;
    /* access modifiers changed from: private */
    public final CopyOnWriteArraySet<com.google.android.exoplayer2.video.VideoListener> videoListeners;
    private int videoScalingMode;

    private final class ComponentListener implements VideoRendererEventListener, AudioRendererEventListener, TextOutput, MetadataOutput, Callback, SurfaceTextureListener, PlayerControl {
        private ComponentListener() {
        }

        public void onVideoEnabled(DecoderCounters counters) {
            SimpleExoPlayer.this.videoDecoderCounters = counters;
            Iterator it = SimpleExoPlayer.this.videoDebugListeners.iterator();
            while (it.hasNext()) {
                ((VideoRendererEventListener) it.next()).onVideoEnabled(counters);
            }
        }

        public void onVideoDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            Iterator it = SimpleExoPlayer.this.videoDebugListeners.iterator();
            while (it.hasNext()) {
                ((VideoRendererEventListener) it.next()).onVideoDecoderInitialized(decoderName, initializedTimestampMs, initializationDurationMs);
            }
        }

        public void onVideoInputFormatChanged(Format format) {
            SimpleExoPlayer.this.videoFormat = format;
            Iterator it = SimpleExoPlayer.this.videoDebugListeners.iterator();
            while (it.hasNext()) {
                ((VideoRendererEventListener) it.next()).onVideoInputFormatChanged(format);
            }
        }

        public void onDroppedFrames(int count, long elapsed) {
            Iterator it = SimpleExoPlayer.this.videoDebugListeners.iterator();
            while (it.hasNext()) {
                ((VideoRendererEventListener) it.next()).onDroppedFrames(count, elapsed);
            }
        }

        public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
            Iterator it = SimpleExoPlayer.this.videoListeners.iterator();
            while (it.hasNext()) {
                com.google.android.exoplayer2.video.VideoListener videoListener = (com.google.android.exoplayer2.video.VideoListener) it.next();
                if (!SimpleExoPlayer.this.videoDebugListeners.contains(videoListener)) {
                    videoListener.onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
                }
            }
            Iterator it2 = SimpleExoPlayer.this.videoDebugListeners.iterator();
            while (it2.hasNext()) {
                ((VideoRendererEventListener) it2.next()).onVideoSizeChanged(width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
            }
        }

        public void onRenderedFirstFrame(Surface surface) {
            if (SimpleExoPlayer.this.surface == surface) {
                Iterator it = SimpleExoPlayer.this.videoListeners.iterator();
                while (it.hasNext()) {
                    ((com.google.android.exoplayer2.video.VideoListener) it.next()).onRenderedFirstFrame();
                }
            }
            Iterator it2 = SimpleExoPlayer.this.videoDebugListeners.iterator();
            while (it2.hasNext()) {
                ((VideoRendererEventListener) it2.next()).onRenderedFirstFrame(surface);
            }
        }

        public void onVideoDisabled(DecoderCounters counters) {
            Iterator it = SimpleExoPlayer.this.videoDebugListeners.iterator();
            while (it.hasNext()) {
                ((VideoRendererEventListener) it.next()).onVideoDisabled(counters);
            }
            SimpleExoPlayer.this.videoFormat = null;
            SimpleExoPlayer.this.videoDecoderCounters = null;
        }

        public void onAudioEnabled(DecoderCounters counters) {
            SimpleExoPlayer.this.audioDecoderCounters = counters;
            Iterator it = SimpleExoPlayer.this.audioDebugListeners.iterator();
            while (it.hasNext()) {
                ((AudioRendererEventListener) it.next()).onAudioEnabled(counters);
            }
        }

        public void onAudioSessionId(int sessionId) {
            if (SimpleExoPlayer.this.audioSessionId != sessionId) {
                SimpleExoPlayer.this.audioSessionId = sessionId;
                Iterator it = SimpleExoPlayer.this.audioListeners.iterator();
                while (it.hasNext()) {
                    AudioListener audioListener = (AudioListener) it.next();
                    if (!SimpleExoPlayer.this.audioDebugListeners.contains(audioListener)) {
                        audioListener.onAudioSessionId(sessionId);
                    }
                }
                Iterator it2 = SimpleExoPlayer.this.audioDebugListeners.iterator();
                while (it2.hasNext()) {
                    ((AudioRendererEventListener) it2.next()).onAudioSessionId(sessionId);
                }
            }
        }

        public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
            Iterator it = SimpleExoPlayer.this.audioDebugListeners.iterator();
            while (it.hasNext()) {
                ((AudioRendererEventListener) it.next()).onAudioDecoderInitialized(decoderName, initializedTimestampMs, initializationDurationMs);
            }
        }

        public void onAudioInputFormatChanged(Format format) {
            SimpleExoPlayer.this.audioFormat = format;
            Iterator it = SimpleExoPlayer.this.audioDebugListeners.iterator();
            while (it.hasNext()) {
                ((AudioRendererEventListener) it.next()).onAudioInputFormatChanged(format);
            }
        }

        public void onAudioSinkUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
            Iterator it = SimpleExoPlayer.this.audioDebugListeners.iterator();
            while (it.hasNext()) {
                ((AudioRendererEventListener) it.next()).onAudioSinkUnderrun(bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
            }
        }

        public void onAudioDisabled(DecoderCounters counters) {
            Iterator it = SimpleExoPlayer.this.audioDebugListeners.iterator();
            while (it.hasNext()) {
                ((AudioRendererEventListener) it.next()).onAudioDisabled(counters);
            }
            SimpleExoPlayer.this.audioFormat = null;
            SimpleExoPlayer.this.audioDecoderCounters = null;
            SimpleExoPlayer.this.audioSessionId = 0;
        }

        public void onCues(List<Cue> cues) {
            SimpleExoPlayer.this.currentCues = cues;
            Iterator it = SimpleExoPlayer.this.textOutputs.iterator();
            while (it.hasNext()) {
                ((TextOutput) it.next()).onCues(cues);
            }
        }

        public void onMetadata(Metadata metadata) {
            Iterator it = SimpleExoPlayer.this.metadataOutputs.iterator();
            while (it.hasNext()) {
                ((MetadataOutput) it.next()).onMetadata(metadata);
            }
        }

        public void surfaceCreated(SurfaceHolder holder) {
            SimpleExoPlayer.this.setVideoSurfaceInternal(holder.getSurface(), false);
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            SimpleExoPlayer.this.maybeNotifySurfaceSizeChanged(width, height);
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            SimpleExoPlayer.this.setVideoSurfaceInternal(null, false);
            SimpleExoPlayer.this.maybeNotifySurfaceSizeChanged(0, 0);
        }

        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
            SimpleExoPlayer.this.setVideoSurfaceInternal(new Surface(surfaceTexture), true);
            SimpleExoPlayer.this.maybeNotifySurfaceSizeChanged(width, height);
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
            SimpleExoPlayer.this.maybeNotifySurfaceSizeChanged(width, height);
        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            SimpleExoPlayer.this.setVideoSurfaceInternal(null, true);
            SimpleExoPlayer.this.maybeNotifySurfaceSizeChanged(0, 0);
            return true;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

        public void setVolumeMultiplier(float volumeMultiplier) {
            SimpleExoPlayer.this.sendVolumeToRenderers();
        }

        public void executePlayerCommand(int playerCommand) {
            SimpleExoPlayer simpleExoPlayer = SimpleExoPlayer.this;
            simpleExoPlayer.updatePlayWhenReady(simpleExoPlayer.getPlayWhenReady(), playerCommand);
        }
    }

    @Deprecated
    public interface VideoListener extends com.google.android.exoplayer2.video.VideoListener {
    }

    protected SimpleExoPlayer(Context context, RenderersFactory renderersFactory, TrackSelector trackSelector, LoadControl loadControl, BandwidthMeter bandwidthMeter2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, Looper looper) {
        this(context, renderersFactory, trackSelector, loadControl, drmSessionManager, bandwidthMeter2, new Factory(), looper);
    }

    protected SimpleExoPlayer(Context context, RenderersFactory renderersFactory, TrackSelector trackSelector, LoadControl loadControl, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, BandwidthMeter bandwidthMeter2, Factory analyticsCollectorFactory, Looper looper) {
        this(context, renderersFactory, trackSelector, loadControl, drmSessionManager, bandwidthMeter2, analyticsCollectorFactory, Clock.DEFAULT, looper);
    }

    protected SimpleExoPlayer(Context context, RenderersFactory renderersFactory, TrackSelector trackSelector, LoadControl loadControl, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, BandwidthMeter bandwidthMeter2, Factory analyticsCollectorFactory, Clock clock, Looper looper) {
        DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2 = drmSessionManager;
        BandwidthMeter bandwidthMeter3 = bandwidthMeter2;
        this.bandwidthMeter = bandwidthMeter3;
        this.componentListener = new ComponentListener();
        this.videoListeners = new CopyOnWriteArraySet<>();
        this.audioListeners = new CopyOnWriteArraySet<>();
        this.textOutputs = new CopyOnWriteArraySet<>();
        this.metadataOutputs = new CopyOnWriteArraySet<>();
        this.videoDebugListeners = new CopyOnWriteArraySet<>();
        this.audioDebugListeners = new CopyOnWriteArraySet<>();
        this.eventHandler = new Handler(looper);
        Handler handler = this.eventHandler;
        ComponentListener componentListener2 = this.componentListener;
        this.renderers = renderersFactory.createRenderers(handler, componentListener2, componentListener2, componentListener2, componentListener2, drmSessionManager);
        this.audioVolume = 1.0f;
        this.audioSessionId = 0;
        this.audioAttributes = AudioAttributes.DEFAULT;
        this.videoScalingMode = 1;
        this.currentCues = Collections.emptyList();
        ExoPlayerImpl exoPlayerImpl = new ExoPlayerImpl(this.renderers, trackSelector, loadControl, bandwidthMeter2, clock, looper);
        this.player = exoPlayerImpl;
        this.analyticsCollector = analyticsCollectorFactory.createAnalyticsCollector(this.player, clock);
        addListener(this.analyticsCollector);
        this.videoDebugListeners.add(this.analyticsCollector);
        this.videoListeners.add(this.analyticsCollector);
        this.audioDebugListeners.add(this.analyticsCollector);
        this.audioListeners.add(this.analyticsCollector);
        addMetadataOutput(this.analyticsCollector);
        bandwidthMeter3.addEventListener(this.eventHandler, this.analyticsCollector);
        if (drmSessionManager2 instanceof DefaultDrmSessionManager) {
            ((DefaultDrmSessionManager) drmSessionManager2).addListener(this.eventHandler, this.analyticsCollector);
        }
        Context context2 = context;
        this.audioFocusManager = new AudioFocusManager(context, this.componentListener);
    }

    public AudioComponent getAudioComponent() {
        return this;
    }

    public VideoComponent getVideoComponent() {
        return this;
    }

    public TextComponent getTextComponent() {
        return this;
    }

    public void setVideoScalingMode(int videoScalingMode2) {
        Renderer[] rendererArr;
        verifyApplicationThread();
        this.videoScalingMode = videoScalingMode2;
        for (Renderer renderer : this.renderers) {
            if (renderer.getTrackType() == 2) {
                this.player.createMessage(renderer).setType(4).setPayload(Integer.valueOf(videoScalingMode2)).send();
            }
        }
    }

    public int getVideoScalingMode() {
        return this.videoScalingMode;
    }

    public void clearVideoSurface() {
        verifyApplicationThread();
        setVideoSurface(null);
    }

    public void clearVideoSurface(Surface surface2) {
        verifyApplicationThread();
        if (surface2 != null && surface2 == this.surface) {
            setVideoSurface(null);
        }
    }

    public void setVideoSurface(@Nullable Surface surface2) {
        verifyApplicationThread();
        removeSurfaceCallbacks();
        int newSurfaceSize = 0;
        setVideoSurfaceInternal(surface2, false);
        if (surface2 != null) {
            newSurfaceSize = -1;
        }
        maybeNotifySurfaceSizeChanged(newSurfaceSize, newSurfaceSize);
    }

    public void setVideoSurfaceHolder(SurfaceHolder surfaceHolder2) {
        verifyApplicationThread();
        removeSurfaceCallbacks();
        this.surfaceHolder = surfaceHolder2;
        if (surfaceHolder2 == null) {
            setVideoSurfaceInternal(null, false);
            maybeNotifySurfaceSizeChanged(0, 0);
            return;
        }
        surfaceHolder2.addCallback(this.componentListener);
        Surface surface2 = surfaceHolder2.getSurface();
        if (surface2 == null || !surface2.isValid()) {
            setVideoSurfaceInternal(null, false);
            maybeNotifySurfaceSizeChanged(0, 0);
            return;
        }
        setVideoSurfaceInternal(surface2, false);
        Rect surfaceSize = surfaceHolder2.getSurfaceFrame();
        maybeNotifySurfaceSizeChanged(surfaceSize.width(), surfaceSize.height());
    }

    public void clearVideoSurfaceHolder(SurfaceHolder surfaceHolder2) {
        verifyApplicationThread();
        if (surfaceHolder2 != null && surfaceHolder2 == this.surfaceHolder) {
            setVideoSurfaceHolder(null);
        }
    }

    public void setVideoSurfaceView(SurfaceView surfaceView) {
        setVideoSurfaceHolder(surfaceView == null ? null : surfaceView.getHolder());
    }

    public void clearVideoSurfaceView(SurfaceView surfaceView) {
        clearVideoSurfaceHolder(surfaceView == null ? null : surfaceView.getHolder());
    }

    public void setVideoTextureView(TextureView textureView2) {
        verifyApplicationThread();
        removeSurfaceCallbacks();
        this.textureView = textureView2;
        if (textureView2 == null) {
            setVideoSurfaceInternal(null, true);
            maybeNotifySurfaceSizeChanged(0, 0);
            return;
        }
        if (textureView2.getSurfaceTextureListener() != null) {
            Log.w(TAG, "Replacing existing SurfaceTextureListener.");
        }
        textureView2.setSurfaceTextureListener(this.componentListener);
        SurfaceTexture surfaceTexture = textureView2.isAvailable() ? textureView2.getSurfaceTexture() : null;
        if (surfaceTexture == null) {
            setVideoSurfaceInternal(null, true);
            maybeNotifySurfaceSizeChanged(0, 0);
            return;
        }
        setVideoSurfaceInternal(new Surface(surfaceTexture), true);
        maybeNotifySurfaceSizeChanged(textureView2.getWidth(), textureView2.getHeight());
    }

    public void clearVideoTextureView(TextureView textureView2) {
        verifyApplicationThread();
        if (textureView2 != null && textureView2 == this.textureView) {
            setVideoTextureView(null);
        }
    }

    public void addAudioListener(AudioListener listener) {
        this.audioListeners.add(listener);
    }

    public void removeAudioListener(AudioListener listener) {
        this.audioListeners.remove(listener);
    }

    public void setAudioAttributes(AudioAttributes audioAttributes2) {
        setAudioAttributes(audioAttributes2, false);
    }

    public void setAudioAttributes(AudioAttributes audioAttributes2, boolean handleAudioFocus) {
        Renderer[] rendererArr;
        verifyApplicationThread();
        if (!Util.areEqual(this.audioAttributes, audioAttributes2)) {
            this.audioAttributes = audioAttributes2;
            for (Renderer renderer : this.renderers) {
                if (renderer.getTrackType() == 1) {
                    this.player.createMessage(renderer).setType(3).setPayload(audioAttributes2).send();
                }
            }
            Iterator it = this.audioListeners.iterator();
            while (it.hasNext()) {
                ((AudioListener) it.next()).onAudioAttributesChanged(audioAttributes2);
            }
        }
        updatePlayWhenReady(getPlayWhenReady(), this.audioFocusManager.setAudioAttributes(handleAudioFocus ? audioAttributes2 : null, getPlayWhenReady(), getPlaybackState()));
    }

    public AudioAttributes getAudioAttributes() {
        return this.audioAttributes;
    }

    public int getAudioSessionId() {
        return this.audioSessionId;
    }

    public void setAuxEffectInfo(AuxEffectInfo auxEffectInfo) {
        Renderer[] rendererArr;
        verifyApplicationThread();
        for (Renderer renderer : this.renderers) {
            if (renderer.getTrackType() == 1) {
                this.player.createMessage(renderer).setType(5).setPayload(auxEffectInfo).send();
            }
        }
    }

    public void clearAuxEffectInfo() {
        setAuxEffectInfo(new AuxEffectInfo(0, 0.0f));
    }

    public void setVolume(float audioVolume2) {
        verifyApplicationThread();
        float audioVolume3 = Util.constrainValue(audioVolume2, 0.0f, 1.0f);
        if (this.audioVolume != audioVolume3) {
            this.audioVolume = audioVolume3;
            sendVolumeToRenderers();
            Iterator it = this.audioListeners.iterator();
            while (it.hasNext()) {
                ((AudioListener) it.next()).onVolumeChanged(audioVolume3);
            }
        }
    }

    public float getVolume() {
        return this.audioVolume;
    }

    @Deprecated
    public void setAudioStreamType(int streamType) {
        int usage = Util.getAudioUsageForStreamType(streamType);
        setAudioAttributes(new Builder().setUsage(usage).setContentType(Util.getAudioContentTypeForStreamType(streamType)).build());
    }

    @Deprecated
    public int getAudioStreamType() {
        return Util.getStreamTypeForAudioUsage(this.audioAttributes.usage);
    }

    public AnalyticsCollector getAnalyticsCollector() {
        return this.analyticsCollector;
    }

    public void addAnalyticsListener(AnalyticsListener listener) {
        verifyApplicationThread();
        this.analyticsCollector.addListener(listener);
    }

    public void removeAnalyticsListener(AnalyticsListener listener) {
        verifyApplicationThread();
        this.analyticsCollector.removeListener(listener);
    }

    @TargetApi(23)
    @Deprecated
    public void setPlaybackParams(@Nullable PlaybackParams params) {
        PlaybackParameters playbackParameters;
        if (params != null) {
            params.allowDefaults();
            playbackParameters = new PlaybackParameters(params.getSpeed(), params.getPitch());
        } else {
            playbackParameters = null;
        }
        setPlaybackParameters(playbackParameters);
    }

    public Format getVideoFormat() {
        return this.videoFormat;
    }

    public Format getAudioFormat() {
        return this.audioFormat;
    }

    public DecoderCounters getVideoDecoderCounters() {
        return this.videoDecoderCounters;
    }

    public DecoderCounters getAudioDecoderCounters() {
        return this.audioDecoderCounters;
    }

    public void addVideoListener(com.google.android.exoplayer2.video.VideoListener listener) {
        this.videoListeners.add(listener);
    }

    public void removeVideoListener(com.google.android.exoplayer2.video.VideoListener listener) {
        this.videoListeners.remove(listener);
    }

    public void setVideoFrameMetadataListener(VideoFrameMetadataListener listener) {
        Renderer[] rendererArr;
        verifyApplicationThread();
        this.videoFrameMetadataListener = listener;
        for (Renderer renderer : this.renderers) {
            if (renderer.getTrackType() == 2) {
                this.player.createMessage(renderer).setType(6).setPayload(listener).send();
            }
        }
    }

    public void clearVideoFrameMetadataListener(VideoFrameMetadataListener listener) {
        Renderer[] rendererArr;
        verifyApplicationThread();
        if (this.videoFrameMetadataListener == listener) {
            for (Renderer renderer : this.renderers) {
                if (renderer.getTrackType() == 2) {
                    this.player.createMessage(renderer).setType(6).setPayload(null).send();
                }
            }
        }
    }

    public void setCameraMotionListener(CameraMotionListener listener) {
        Renderer[] rendererArr;
        verifyApplicationThread();
        this.cameraMotionListener = listener;
        for (Renderer renderer : this.renderers) {
            if (renderer.getTrackType() == 5) {
                this.player.createMessage(renderer).setType(7).setPayload(listener).send();
            }
        }
    }

    public void clearCameraMotionListener(CameraMotionListener listener) {
        Renderer[] rendererArr;
        verifyApplicationThread();
        if (this.cameraMotionListener == listener) {
            for (Renderer renderer : this.renderers) {
                if (renderer.getTrackType() == 5) {
                    this.player.createMessage(renderer).setType(7).setPayload(null).send();
                }
            }
        }
    }

    @Deprecated
    public void setVideoListener(VideoListener listener) {
        this.videoListeners.clear();
        if (listener != null) {
            addVideoListener(listener);
        }
    }

    @Deprecated
    public void clearVideoListener(VideoListener listener) {
        removeVideoListener(listener);
    }

    public void addTextOutput(TextOutput listener) {
        if (!this.currentCues.isEmpty()) {
            listener.onCues(this.currentCues);
        }
        this.textOutputs.add(listener);
    }

    public void removeTextOutput(TextOutput listener) {
        this.textOutputs.remove(listener);
    }

    @Deprecated
    public void setTextOutput(TextOutput output) {
        this.textOutputs.clear();
        if (output != null) {
            addTextOutput(output);
        }
    }

    @Deprecated
    public void clearTextOutput(TextOutput output) {
        removeTextOutput(output);
    }

    public void addMetadataOutput(MetadataOutput listener) {
        this.metadataOutputs.add(listener);
    }

    public void removeMetadataOutput(MetadataOutput listener) {
        this.metadataOutputs.remove(listener);
    }

    @Deprecated
    public void setMetadataOutput(MetadataOutput output) {
        this.metadataOutputs.retainAll(Collections.singleton(this.analyticsCollector));
        if (output != null) {
            addMetadataOutput(output);
        }
    }

    @Deprecated
    public void clearMetadataOutput(MetadataOutput output) {
        removeMetadataOutput(output);
    }

    @Deprecated
    public void setVideoDebugListener(VideoRendererEventListener listener) {
        this.videoDebugListeners.retainAll(Collections.singleton(this.analyticsCollector));
        if (listener != null) {
            addVideoDebugListener(listener);
        }
    }

    @Deprecated
    public void addVideoDebugListener(VideoRendererEventListener listener) {
        this.videoDebugListeners.add(listener);
    }

    @Deprecated
    public void removeVideoDebugListener(VideoRendererEventListener listener) {
        this.videoDebugListeners.remove(listener);
    }

    @Deprecated
    public void setAudioDebugListener(AudioRendererEventListener listener) {
        this.audioDebugListeners.retainAll(Collections.singleton(this.analyticsCollector));
        if (listener != null) {
            addAudioDebugListener(listener);
        }
    }

    @Deprecated
    public void addAudioDebugListener(AudioRendererEventListener listener) {
        this.audioDebugListeners.add(listener);
    }

    @Deprecated
    public void removeAudioDebugListener(AudioRendererEventListener listener) {
        this.audioDebugListeners.remove(listener);
    }

    public Looper getPlaybackLooper() {
        return this.player.getPlaybackLooper();
    }

    public Looper getApplicationLooper() {
        return this.player.getApplicationLooper();
    }

    public void addListener(EventListener listener) {
        verifyApplicationThread();
        this.player.addListener(listener);
    }

    public void removeListener(EventListener listener) {
        verifyApplicationThread();
        this.player.removeListener(listener);
    }

    public int getPlaybackState() {
        verifyApplicationThread();
        return this.player.getPlaybackState();
    }

    @Nullable
    public ExoPlaybackException getPlaybackError() {
        verifyApplicationThread();
        return this.player.getPlaybackError();
    }

    public void retry() {
        verifyApplicationThread();
        if (this.mediaSource == null) {
            return;
        }
        if (getPlaybackError() != null || getPlaybackState() == 1) {
            prepare(this.mediaSource, false, false);
        }
    }

    public void prepare(MediaSource mediaSource2) {
        prepare(mediaSource2, true, true);
    }

    public void prepare(MediaSource mediaSource2, boolean resetPosition, boolean resetState) {
        verifyApplicationThread();
        MediaSource mediaSource3 = this.mediaSource;
        if (mediaSource3 != null) {
            mediaSource3.removeEventListener(this.analyticsCollector);
            this.analyticsCollector.resetForNewMediaSource();
        }
        this.mediaSource = mediaSource2;
        mediaSource2.addEventListener(this.eventHandler, this.analyticsCollector);
        updatePlayWhenReady(getPlayWhenReady(), this.audioFocusManager.handlePrepare(getPlayWhenReady()));
        this.player.prepare(mediaSource2, resetPosition, resetState);
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        verifyApplicationThread();
        updatePlayWhenReady(playWhenReady, this.audioFocusManager.handleSetPlayWhenReady(playWhenReady, getPlaybackState()));
    }

    public boolean getPlayWhenReady() {
        verifyApplicationThread();
        return this.player.getPlayWhenReady();
    }

    public int getRepeatMode() {
        verifyApplicationThread();
        return this.player.getRepeatMode();
    }

    public void setRepeatMode(int repeatMode) {
        verifyApplicationThread();
        this.player.setRepeatMode(repeatMode);
    }

    public void setShuffleModeEnabled(boolean shuffleModeEnabled) {
        verifyApplicationThread();
        this.player.setShuffleModeEnabled(shuffleModeEnabled);
    }

    public boolean getShuffleModeEnabled() {
        verifyApplicationThread();
        return this.player.getShuffleModeEnabled();
    }

    public boolean isLoading() {
        verifyApplicationThread();
        return this.player.isLoading();
    }

    public void seekTo(int windowIndex, long positionMs) {
        verifyApplicationThread();
        this.analyticsCollector.notifySeekStarted();
        this.player.seekTo(windowIndex, positionMs);
    }

    public void setPlaybackParameters(@Nullable PlaybackParameters playbackParameters) {
        verifyApplicationThread();
        this.player.setPlaybackParameters(playbackParameters);
    }

    public PlaybackParameters getPlaybackParameters() {
        verifyApplicationThread();
        return this.player.getPlaybackParameters();
    }

    public void setSeekParameters(@Nullable SeekParameters seekParameters) {
        verifyApplicationThread();
        this.player.setSeekParameters(seekParameters);
    }

    public SeekParameters getSeekParameters() {
        verifyApplicationThread();
        return this.player.getSeekParameters();
    }

    public void stop(boolean reset) {
        verifyApplicationThread();
        this.player.stop(reset);
        MediaSource mediaSource2 = this.mediaSource;
        if (mediaSource2 != null) {
            mediaSource2.removeEventListener(this.analyticsCollector);
            this.analyticsCollector.resetForNewMediaSource();
            if (reset) {
                this.mediaSource = null;
            }
        }
        this.audioFocusManager.handleStop();
        this.currentCues = Collections.emptyList();
    }

    public void release() {
        this.audioFocusManager.handleStop();
        this.player.release();
        removeSurfaceCallbacks();
        Surface surface2 = this.surface;
        if (surface2 != null) {
            if (this.ownsSurface) {
                surface2.release();
            }
            this.surface = null;
        }
        MediaSource mediaSource2 = this.mediaSource;
        if (mediaSource2 != null) {
            mediaSource2.removeEventListener(this.analyticsCollector);
            this.mediaSource = null;
        }
        this.bandwidthMeter.removeEventListener(this.analyticsCollector);
        this.currentCues = Collections.emptyList();
    }

    @Deprecated
    public void sendMessages(ExoPlayerMessage... messages) {
        this.player.sendMessages(messages);
    }

    public PlayerMessage createMessage(Target target) {
        verifyApplicationThread();
        return this.player.createMessage(target);
    }

    @Deprecated
    public void blockingSendMessages(ExoPlayerMessage... messages) {
        this.player.blockingSendMessages(messages);
    }

    public int getRendererCount() {
        verifyApplicationThread();
        return this.player.getRendererCount();
    }

    public int getRendererType(int index) {
        verifyApplicationThread();
        return this.player.getRendererType(index);
    }

    public TrackGroupArray getCurrentTrackGroups() {
        verifyApplicationThread();
        return this.player.getCurrentTrackGroups();
    }

    public TrackSelectionArray getCurrentTrackSelections() {
        verifyApplicationThread();
        return this.player.getCurrentTrackSelections();
    }

    public Timeline getCurrentTimeline() {
        verifyApplicationThread();
        return this.player.getCurrentTimeline();
    }

    @Nullable
    public Object getCurrentManifest() {
        verifyApplicationThread();
        return this.player.getCurrentManifest();
    }

    public int getCurrentPeriodIndex() {
        verifyApplicationThread();
        return this.player.getCurrentPeriodIndex();
    }

    public int getCurrentWindowIndex() {
        verifyApplicationThread();
        return this.player.getCurrentWindowIndex();
    }

    public long getDuration() {
        verifyApplicationThread();
        return this.player.getDuration();
    }

    public long getCurrentPosition() {
        verifyApplicationThread();
        return this.player.getCurrentPosition();
    }

    public long getBufferedPosition() {
        verifyApplicationThread();
        return this.player.getBufferedPosition();
    }

    public long getTotalBufferedDuration() {
        verifyApplicationThread();
        return this.player.getTotalBufferedDuration();
    }

    public boolean isPlayingAd() {
        verifyApplicationThread();
        return this.player.isPlayingAd();
    }

    public int getCurrentAdGroupIndex() {
        verifyApplicationThread();
        return this.player.getCurrentAdGroupIndex();
    }

    public int getCurrentAdIndexInAdGroup() {
        verifyApplicationThread();
        return this.player.getCurrentAdIndexInAdGroup();
    }

    public long getContentPosition() {
        verifyApplicationThread();
        return this.player.getContentPosition();
    }

    public long getContentBufferedPosition() {
        verifyApplicationThread();
        return this.player.getContentBufferedPosition();
    }

    private void removeSurfaceCallbacks() {
        TextureView textureView2 = this.textureView;
        if (textureView2 != null) {
            if (textureView2.getSurfaceTextureListener() != this.componentListener) {
                Log.w(TAG, "SurfaceTextureListener already unset or replaced.");
            } else {
                this.textureView.setSurfaceTextureListener(null);
            }
            this.textureView = null;
        }
        SurfaceHolder surfaceHolder2 = this.surfaceHolder;
        if (surfaceHolder2 != null) {
            surfaceHolder2.removeCallback(this.componentListener);
            this.surfaceHolder = null;
        }
    }

    /* access modifiers changed from: private */
    public void setVideoSurfaceInternal(@Nullable Surface surface2, boolean ownsSurface2) {
        Renderer[] rendererArr;
        List<PlayerMessage> messages = new ArrayList<>();
        for (Renderer renderer : this.renderers) {
            if (renderer.getTrackType() == 2) {
                messages.add(this.player.createMessage(renderer).setType(1).setPayload(surface2).send());
            }
        }
        Surface surface3 = this.surface;
        if (!(surface3 == null || surface3 == surface2)) {
            try {
                for (PlayerMessage message : messages) {
                    message.blockUntilDelivered();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (this.ownsSurface) {
                this.surface.release();
            }
        }
        this.surface = surface2;
        this.ownsSurface = ownsSurface2;
    }

    /* access modifiers changed from: private */
    public void maybeNotifySurfaceSizeChanged(int width, int height) {
        if (width != this.surfaceWidth || height != this.surfaceHeight) {
            this.surfaceWidth = width;
            this.surfaceHeight = height;
            Iterator it = this.videoListeners.iterator();
            while (it.hasNext()) {
                ((com.google.android.exoplayer2.video.VideoListener) it.next()).onSurfaceSizeChanged(width, height);
            }
        }
    }

    /* access modifiers changed from: private */
    public void sendVolumeToRenderers() {
        Renderer[] rendererArr;
        float scaledVolume = this.audioVolume * this.audioFocusManager.getVolumeMultiplier();
        for (Renderer renderer : this.renderers) {
            if (renderer.getTrackType() == 1) {
                this.player.createMessage(renderer).setType(2).setPayload(Float.valueOf(scaledVolume)).send();
            }
        }
    }

    /* access modifiers changed from: private */
    public void updatePlayWhenReady(boolean playWhenReady, int playerCommand) {
        ExoPlayerImpl exoPlayerImpl = this.player;
        boolean z = false;
        boolean z2 = playWhenReady && playerCommand != -1;
        if (playerCommand != 1) {
            z = true;
        }
        exoPlayerImpl.setPlayWhenReady(z2, z);
    }

    private void verifyApplicationThread() {
        if (Looper.myLooper() != getApplicationLooper()) {
            Log.w(TAG, "Player is accessed on the wrong thread. See https://google.github.io/ExoPlayer/faqs.html#what-do-player-is-accessed-on-the-wrong-thread-warnings-mean", this.hasNotifiedFullWrongThreadWarning ? null : new IllegalStateException());
            this.hasNotifiedFullWrongThreadWarning = true;
        }
    }
}
