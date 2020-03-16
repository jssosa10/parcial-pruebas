package com.google.android.exoplayer2.video;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.media.MediaCodec;
import android.media.MediaCodec.OnFrameRenderedListener;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;
import com.fasterxml.jackson.core.JsonPointer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.mediacodec.MediaFormatUtil;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.TraceUtil;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.VideoRendererEventListener.EventDispatcher;
import java.nio.ByteBuffer;
import java.util.List;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.ClassUtils;
import org.joda.time.DateTimeConstants;

@TargetApi(16)
public class MediaCodecVideoRenderer extends MediaCodecRenderer {
    private static final float INITIAL_FORMAT_MAX_INPUT_SIZE_SCALE_FACTOR = 1.5f;
    private static final String KEY_CROP_BOTTOM = "crop-bottom";
    private static final String KEY_CROP_LEFT = "crop-left";
    private static final String KEY_CROP_RIGHT = "crop-right";
    private static final String KEY_CROP_TOP = "crop-top";
    private static final int MAX_PENDING_OUTPUT_STREAM_OFFSET_COUNT = 10;
    private static final int[] STANDARD_LONG_EDGE_VIDEO_PX = {1920, 1600, DateTimeConstants.MINUTES_PER_DAY, 1280, 960, 854, 640, 540, 480};
    private static final String TAG = "MediaCodecVideoRenderer";
    private static boolean deviceNeedsSetOutputSurfaceWorkaround;
    private static boolean evaluatedDeviceNeedsSetOutputSurfaceWorkaround;
    private final long allowedJoiningTimeMs;
    private int buffersInCodecCount;
    private CodecMaxValues codecMaxValues;
    private boolean codecNeedsSetOutputSurfaceWorkaround;
    private int consecutiveDroppedFrameCount;
    private final Context context;
    private int currentHeight;
    private float currentPixelWidthHeightRatio;
    private int currentUnappliedRotationDegrees;
    private int currentWidth;
    private final boolean deviceNeedsAutoFrcWorkaround;
    private long droppedFrameAccumulationStartTimeMs;
    private int droppedFrames;
    private Surface dummySurface;
    private final EventDispatcher eventDispatcher;
    @Nullable
    private VideoFrameMetadataListener frameMetadataListener;
    private final VideoFrameReleaseTimeHelper frameReleaseTimeHelper;
    private long initialPositionUs;
    private long joiningDeadlineMs;
    private long lastInputTimeUs;
    private long lastRenderTimeUs;
    private final int maxDroppedFramesToNotify;
    private long outputStreamOffsetUs;
    private int pendingOutputStreamOffsetCount;
    private final long[] pendingOutputStreamOffsetsUs;
    private final long[] pendingOutputStreamSwitchTimesUs;
    private float pendingPixelWidthHeightRatio;
    private int pendingRotationDegrees;
    private boolean renderedFirstFrame;
    private int reportedHeight;
    private float reportedPixelWidthHeightRatio;
    private int reportedUnappliedRotationDegrees;
    private int reportedWidth;
    private int scalingMode;
    private Surface surface;
    private boolean tunneling;
    private int tunnelingAudioSessionId;
    OnFrameRenderedListenerV23 tunnelingOnFrameRenderedListener;

    protected static final class CodecMaxValues {
        public final int height;
        public final int inputSize;
        public final int width;

        public CodecMaxValues(int width2, int height2, int inputSize2) {
            this.width = width2;
            this.height = height2;
            this.inputSize = inputSize2;
        }
    }

    @TargetApi(23)
    private final class OnFrameRenderedListenerV23 implements OnFrameRenderedListener {
        private OnFrameRenderedListenerV23(MediaCodec codec) {
            codec.setOnFrameRenderedListener(this, new Handler());
        }

        public void onFrameRendered(@NonNull MediaCodec codec, long presentationTimeUs, long nanoTime) {
            if (this == MediaCodecVideoRenderer.this.tunnelingOnFrameRenderedListener) {
                MediaCodecVideoRenderer.this.onProcessedTunneledBuffer(presentationTimeUs);
            }
        }
    }

    public MediaCodecVideoRenderer(Context context2, MediaCodecSelector mediaCodecSelector) {
        this(context2, mediaCodecSelector, 0);
    }

    public MediaCodecVideoRenderer(Context context2, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs2) {
        this(context2, mediaCodecSelector, allowedJoiningTimeMs2, null, null, -1);
    }

    public MediaCodecVideoRenderer(Context context2, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs2, @Nullable Handler eventHandler, @Nullable VideoRendererEventListener eventListener, int maxDroppedFramesToNotify2) {
        this(context2, mediaCodecSelector, allowedJoiningTimeMs2, null, false, eventHandler, eventListener, maxDroppedFramesToNotify2);
    }

    public MediaCodecVideoRenderer(Context context2, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, @Nullable Handler eventHandler, @Nullable VideoRendererEventListener eventListener, int maxDroppedFramesToNotify2) {
        super(2, mediaCodecSelector, drmSessionManager, playClearSamplesWithoutKeys, 30.0f);
        this.allowedJoiningTimeMs = allowedJoiningTimeMs2;
        this.maxDroppedFramesToNotify = maxDroppedFramesToNotify2;
        this.context = context2.getApplicationContext();
        this.frameReleaseTimeHelper = new VideoFrameReleaseTimeHelper(this.context);
        this.eventDispatcher = new EventDispatcher(eventHandler, eventListener);
        this.deviceNeedsAutoFrcWorkaround = deviceNeedsAutoFrcWorkaround();
        this.pendingOutputStreamOffsetsUs = new long[10];
        this.pendingOutputStreamSwitchTimesUs = new long[10];
        this.outputStreamOffsetUs = C.TIME_UNSET;
        this.lastInputTimeUs = C.TIME_UNSET;
        this.joiningDeadlineMs = C.TIME_UNSET;
        this.currentWidth = -1;
        this.currentHeight = -1;
        this.currentPixelWidthHeightRatio = -1.0f;
        this.pendingPixelWidthHeightRatio = -1.0f;
        this.scalingMode = 1;
        clearReportedVideoSize();
    }

    /* access modifiers changed from: protected */
    public int supportsFormat(MediaCodecSelector mediaCodecSelector, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, Format format) throws DecoderQueryException {
        int tunnelingSupport = 0;
        if (!MimeTypes.isVideo(format.sampleMimeType)) {
            return 0;
        }
        boolean requiresSecureDecryption = false;
        DrmInitData drmInitData = format.drmInitData;
        if (drmInitData != null) {
            for (int i = 0; i < drmInitData.schemeDataCount; i++) {
                requiresSecureDecryption |= drmInitData.get(i).requiresSecureDecryption;
            }
        }
        List<MediaCodecInfo> decoderInfos = mediaCodecSelector.getDecoderInfos(format.sampleMimeType, requiresSecureDecryption);
        int i2 = 2;
        if (decoderInfos.isEmpty()) {
            if (!requiresSecureDecryption || mediaCodecSelector.getDecoderInfos(format.sampleMimeType, false).isEmpty()) {
                i2 = 1;
            }
            return i2;
        } else if (!supportsFormatDrm(drmSessionManager, drmInitData)) {
            return 2;
        } else {
            MediaCodecInfo decoderInfo = (MediaCodecInfo) decoderInfos.get(0);
            boolean isFormatSupported = decoderInfo.isFormatSupported(format);
            int adaptiveSupport = decoderInfo.isSeamlessAdaptationSupported(format) ? 16 : 8;
            if (decoderInfo.tunneling) {
                tunnelingSupport = 32;
            }
            return adaptiveSupport | tunnelingSupport | (isFormatSupported ? 4 : 3);
        }
    }

    /* access modifiers changed from: protected */
    public void onEnabled(boolean joining) throws ExoPlaybackException {
        super.onEnabled(joining);
        this.tunnelingAudioSessionId = getConfiguration().tunnelingAudioSessionId;
        this.tunneling = this.tunnelingAudioSessionId != 0;
        this.eventDispatcher.enabled(this.decoderCounters);
        this.frameReleaseTimeHelper.enable();
    }

    /* access modifiers changed from: protected */
    public void onStreamChanged(Format[] formats, long offsetUs) throws ExoPlaybackException {
        if (this.outputStreamOffsetUs == C.TIME_UNSET) {
            this.outputStreamOffsetUs = offsetUs;
        } else {
            int i = this.pendingOutputStreamOffsetCount;
            if (i == this.pendingOutputStreamOffsetsUs.length) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Too many stream changes, so dropping offset: ");
                sb.append(this.pendingOutputStreamOffsetsUs[this.pendingOutputStreamOffsetCount - 1]);
                Log.w(str, sb.toString());
            } else {
                this.pendingOutputStreamOffsetCount = i + 1;
            }
            long[] jArr = this.pendingOutputStreamOffsetsUs;
            int i2 = this.pendingOutputStreamOffsetCount;
            jArr[i2 - 1] = offsetUs;
            this.pendingOutputStreamSwitchTimesUs[i2 - 1] = this.lastInputTimeUs;
        }
        super.onStreamChanged(formats, offsetUs);
    }

    /* access modifiers changed from: protected */
    public void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
        super.onPositionReset(positionUs, joining);
        clearRenderedFirstFrame();
        this.initialPositionUs = C.TIME_UNSET;
        this.consecutiveDroppedFrameCount = 0;
        this.lastInputTimeUs = C.TIME_UNSET;
        int i = this.pendingOutputStreamOffsetCount;
        if (i != 0) {
            this.outputStreamOffsetUs = this.pendingOutputStreamOffsetsUs[i - 1];
            this.pendingOutputStreamOffsetCount = 0;
        }
        if (joining) {
            setJoiningDeadlineMs();
        } else {
            this.joiningDeadlineMs = C.TIME_UNSET;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0020, code lost:
        if (r9.tunneling != false) goto L_0x0022;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:7:0x0016, code lost:
        if (r9.surface == r0) goto L_0x0022;
     */
    public boolean isReady() {
        if (super.isReady()) {
            if (!this.renderedFirstFrame) {
                Surface surface2 = this.dummySurface;
                if (surface2 != null) {
                }
                if (getCodec() != null) {
                }
            }
            this.joiningDeadlineMs = C.TIME_UNSET;
            return true;
        }
        if (this.joiningDeadlineMs == C.TIME_UNSET) {
            return false;
        }
        if (SystemClock.elapsedRealtime() < this.joiningDeadlineMs) {
            return true;
        }
        this.joiningDeadlineMs = C.TIME_UNSET;
        return false;
    }

    /* access modifiers changed from: protected */
    public void onStarted() {
        super.onStarted();
        this.droppedFrames = 0;
        this.droppedFrameAccumulationStartTimeMs = SystemClock.elapsedRealtime();
        this.lastRenderTimeUs = SystemClock.elapsedRealtime() * 1000;
    }

    /* access modifiers changed from: protected */
    public void onStopped() {
        this.joiningDeadlineMs = C.TIME_UNSET;
        maybeNotifyDroppedFrames();
        super.onStopped();
    }

    /* access modifiers changed from: protected */
    public void onDisabled() {
        this.currentWidth = -1;
        this.currentHeight = -1;
        this.currentPixelWidthHeightRatio = -1.0f;
        this.pendingPixelWidthHeightRatio = -1.0f;
        this.outputStreamOffsetUs = C.TIME_UNSET;
        this.lastInputTimeUs = C.TIME_UNSET;
        this.pendingOutputStreamOffsetCount = 0;
        clearReportedVideoSize();
        clearRenderedFirstFrame();
        this.frameReleaseTimeHelper.disable();
        this.tunnelingOnFrameRenderedListener = null;
        this.tunneling = false;
        try {
            super.onDisabled();
        } finally {
            this.decoderCounters.ensureUpdated();
            this.eventDispatcher.disabled(this.decoderCounters);
        }
    }

    public void handleMessage(int messageType, @Nullable Object message) throws ExoPlaybackException {
        if (messageType == 1) {
            setSurface((Surface) message);
        } else if (messageType == 4) {
            this.scalingMode = ((Integer) message).intValue();
            MediaCodec codec = getCodec();
            if (codec != null) {
                codec.setVideoScalingMode(this.scalingMode);
            }
        } else if (messageType == 6) {
            this.frameMetadataListener = (VideoFrameMetadataListener) message;
        } else {
            super.handleMessage(messageType, message);
        }
    }

    private void setSurface(Surface surface2) throws ExoPlaybackException {
        if (surface2 == null) {
            if (this.dummySurface != null) {
                surface2 = this.dummySurface;
            } else {
                MediaCodecInfo codecInfo = getCodecInfo();
                if (codecInfo != null && shouldUseDummySurface(codecInfo)) {
                    this.dummySurface = DummySurface.newInstanceV17(this.context, codecInfo.secure);
                    surface2 = this.dummySurface;
                }
            }
        }
        if (this.surface != surface2) {
            this.surface = surface2;
            int state = getState();
            if (state == 1 || state == 2) {
                MediaCodec codec = getCodec();
                if (Util.SDK_INT < 23 || codec == null || surface2 == null || this.codecNeedsSetOutputSurfaceWorkaround) {
                    releaseCodec();
                    maybeInitCodec();
                } else {
                    setOutputSurfaceV23(codec, surface2);
                }
            }
            if (surface2 == null || surface2 == this.dummySurface) {
                clearReportedVideoSize();
                clearRenderedFirstFrame();
                return;
            }
            maybeRenotifyVideoSizeChanged();
            clearRenderedFirstFrame();
            if (state == 2) {
                setJoiningDeadlineMs();
            }
        } else if (surface2 != null && surface2 != this.dummySurface) {
            maybeRenotifyVideoSizeChanged();
            maybeRenotifyRenderedFirstFrame();
        }
    }

    /* access modifiers changed from: protected */
    public boolean shouldInitCodec(MediaCodecInfo codecInfo) {
        return this.surface != null || shouldUseDummySurface(codecInfo);
    }

    /* access modifiers changed from: protected */
    public boolean getCodecNeedsEosPropagation() {
        return this.tunneling;
    }

    /* access modifiers changed from: protected */
    public void configureCodec(MediaCodecInfo codecInfo, MediaCodec codec, Format format, MediaCrypto crypto, float codecOperatingRate) throws DecoderQueryException {
        this.codecMaxValues = getCodecMaxValues(codecInfo, format, getStreamFormats());
        MediaFormat mediaFormat = getMediaFormat(format, this.codecMaxValues, codecOperatingRate, this.deviceNeedsAutoFrcWorkaround, this.tunnelingAudioSessionId);
        if (this.surface == null) {
            Assertions.checkState(shouldUseDummySurface(codecInfo));
            if (this.dummySurface == null) {
                this.dummySurface = DummySurface.newInstanceV17(this.context, codecInfo.secure);
            }
            this.surface = this.dummySurface;
        }
        codec.configure(mediaFormat, this.surface, crypto, 0);
        if (Util.SDK_INT >= 23 && this.tunneling) {
            this.tunnelingOnFrameRenderedListener = new OnFrameRenderedListenerV23(codec);
        }
    }

    /* access modifiers changed from: protected */
    public int canKeepCodec(MediaCodec codec, MediaCodecInfo codecInfo, Format oldFormat, Format newFormat) {
        int i = 1;
        if (!codecInfo.isSeamlessAdaptationSupported(oldFormat, newFormat, true) || newFormat.width > this.codecMaxValues.width || newFormat.height > this.codecMaxValues.height || getMaxInputSize(codecInfo, newFormat) > this.codecMaxValues.inputSize) {
            return 0;
        }
        if (!oldFormat.initializationDataEquals(newFormat)) {
            i = 3;
        }
        return i;
    }

    /* access modifiers changed from: protected */
    @CallSuper
    public void releaseCodec() {
        try {
            super.releaseCodec();
        } finally {
            this.buffersInCodecCount = 0;
            Surface surface2 = this.dummySurface;
            if (surface2 != null) {
                if (this.surface == surface2) {
                    this.surface = null;
                }
                this.dummySurface.release();
                this.dummySurface = null;
            }
        }
    }

    /* access modifiers changed from: protected */
    @CallSuper
    public void flushCodec() throws ExoPlaybackException {
        super.flushCodec();
        this.buffersInCodecCount = 0;
    }

    /* access modifiers changed from: protected */
    public float getCodecOperatingRate(float operatingRate, Format format, Format[] streamFormats) {
        float maxFrameRate = -1.0f;
        for (Format streamFormat : streamFormats) {
            float streamFrameRate = streamFormat.frameRate;
            if (streamFrameRate != -1.0f) {
                maxFrameRate = Math.max(maxFrameRate, streamFrameRate);
            }
        }
        if (maxFrameRate == -1.0f) {
            return -1.0f;
        }
        return maxFrameRate * operatingRate;
    }

    /* access modifiers changed from: protected */
    public void onCodecInitialized(String name, long initializedTimestampMs, long initializationDurationMs) {
        this.eventDispatcher.decoderInitialized(name, initializedTimestampMs, initializationDurationMs);
        this.codecNeedsSetOutputSurfaceWorkaround = codecNeedsSetOutputSurfaceWorkaround(name);
    }

    /* access modifiers changed from: protected */
    public void onInputFormatChanged(Format newFormat) throws ExoPlaybackException {
        super.onInputFormatChanged(newFormat);
        this.eventDispatcher.inputFormatChanged(newFormat);
        this.pendingPixelWidthHeightRatio = newFormat.pixelWidthHeightRatio;
        this.pendingRotationDegrees = newFormat.rotationDegrees;
    }

    /* access modifiers changed from: protected */
    @CallSuper
    public void onQueueInputBuffer(DecoderInputBuffer buffer) {
        this.buffersInCodecCount++;
        this.lastInputTimeUs = Math.max(buffer.timeUs, this.lastInputTimeUs);
        if (Util.SDK_INT < 23 && this.tunneling) {
            onProcessedTunneledBuffer(buffer.timeUs);
        }
    }

    /* access modifiers changed from: protected */
    public void onOutputFormatChanged(MediaCodec codec, MediaFormat outputFormat) {
        int width;
        int i;
        boolean hasCrop = outputFormat.containsKey(KEY_CROP_RIGHT) && outputFormat.containsKey(KEY_CROP_LEFT) && outputFormat.containsKey(KEY_CROP_BOTTOM) && outputFormat.containsKey(KEY_CROP_TOP);
        if (hasCrop) {
            width = (outputFormat.getInteger(KEY_CROP_RIGHT) - outputFormat.getInteger(KEY_CROP_LEFT)) + 1;
        } else {
            width = outputFormat.getInteger("width");
        }
        if (hasCrop) {
            i = (outputFormat.getInteger(KEY_CROP_BOTTOM) - outputFormat.getInteger(KEY_CROP_TOP)) + 1;
        } else {
            i = outputFormat.getInteger("height");
        }
        processOutputFormat(codec, width, i);
    }

    /* access modifiers changed from: protected */
    public boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, MediaCodec codec, ByteBuffer buffer, int bufferIndex, int bufferFlags, long bufferPresentationTimeUs, boolean shouldSkip, Format format) throws ExoPlaybackException {
        long presentationTimeUs;
        long presentationTimeUs2;
        long unadjustedFrameReleaseTimeNs;
        long j = positionUs;
        long j2 = elapsedRealtimeUs;
        MediaCodec mediaCodec = codec;
        int i = bufferIndex;
        long j3 = bufferPresentationTimeUs;
        if (this.initialPositionUs == C.TIME_UNSET) {
            this.initialPositionUs = j;
        }
        long presentationTimeUs3 = j3 - this.outputStreamOffsetUs;
        if (shouldSkip) {
            skipOutputBuffer(mediaCodec, i, presentationTimeUs3);
            return true;
        }
        long earlyUs = j3 - j;
        if (this.surface != this.dummySurface) {
            long elapsedRealtimeNowUs = SystemClock.elapsedRealtime() * 1000;
            boolean isStarted = getState() == 2;
            if (!this.renderedFirstFrame) {
                presentationTimeUs = presentationTimeUs3;
            } else if (!isStarted || !shouldForceRenderOutputBuffer(earlyUs, elapsedRealtimeNowUs - this.lastRenderTimeUs)) {
                if (!isStarted) {
                } else if (j == this.initialPositionUs) {
                    long j4 = presentationTimeUs3;
                } else {
                    long earlyUs2 = earlyUs - (elapsedRealtimeNowUs - j2);
                    long systemTimeNs = System.nanoTime();
                    long unadjustedFrameReleaseTimeNs2 = systemTimeNs + (earlyUs2 * 1000);
                    long adjustedReleaseTimeNs = this.frameReleaseTimeHelper.adjustReleaseTime(j3, unadjustedFrameReleaseTimeNs2);
                    long earlyUs3 = (adjustedReleaseTimeNs - systemTimeNs) / 1000;
                    if (shouldDropBuffersToKeyframe(earlyUs3, j2)) {
                        long j5 = unadjustedFrameReleaseTimeNs2;
                        unadjustedFrameReleaseTimeNs = earlyUs3;
                        presentationTimeUs2 = presentationTimeUs3;
                        if (maybeDropBuffersToKeyframe(codec, bufferIndex, presentationTimeUs3, positionUs)) {
                            return false;
                        }
                    } else {
                        presentationTimeUs2 = presentationTimeUs3;
                        long j6 = unadjustedFrameReleaseTimeNs2;
                        unadjustedFrameReleaseTimeNs = earlyUs3;
                    }
                    if (shouldDropOutputBuffer(unadjustedFrameReleaseTimeNs, j2)) {
                        dropOutputBuffer(mediaCodec, i, presentationTimeUs2);
                        return true;
                    }
                    long presentationTimeUs4 = presentationTimeUs2;
                    if (Util.SDK_INT < 21) {
                        long presentationTimeUs5 = presentationTimeUs4;
                        if (unadjustedFrameReleaseTimeNs < 30000) {
                            if (unadjustedFrameReleaseTimeNs > 11000) {
                                try {
                                    Thread.sleep((unadjustedFrameReleaseTimeNs - 10000) / 1000);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    return false;
                                }
                            }
                            notifyFrameMetadataListener(presentationTimeUs5, adjustedReleaseTimeNs, format);
                            renderOutputBuffer(mediaCodec, i, presentationTimeUs5);
                            return true;
                        }
                    } else if (unadjustedFrameReleaseTimeNs < 50000) {
                        long presentationTimeUs6 = presentationTimeUs4;
                        notifyFrameMetadataListener(presentationTimeUs4, adjustedReleaseTimeNs, format);
                        renderOutputBufferV21(codec, bufferIndex, presentationTimeUs6, adjustedReleaseTimeNs);
                        return true;
                    }
                    return false;
                }
                return false;
            } else {
                presentationTimeUs = presentationTimeUs3;
            }
            long releaseTimeNs = System.nanoTime();
            long j7 = earlyUs;
            long presentationTimeUs7 = presentationTimeUs;
            notifyFrameMetadataListener(presentationTimeUs, releaseTimeNs, format);
            if (Util.SDK_INT >= 21) {
                renderOutputBufferV21(codec, bufferIndex, presentationTimeUs7, releaseTimeNs);
                long j8 = presentationTimeUs7;
            } else {
                renderOutputBuffer(mediaCodec, i, presentationTimeUs7);
            }
            return true;
        } else if (!isBufferLate(earlyUs)) {
            return false;
        } else {
            skipOutputBuffer(mediaCodec, i, presentationTimeUs3);
            return true;
        }
    }

    private void processOutputFormat(MediaCodec codec, int width, int height) {
        this.currentWidth = width;
        this.currentHeight = height;
        this.currentPixelWidthHeightRatio = this.pendingPixelWidthHeightRatio;
        if (Util.SDK_INT >= 21) {
            int i = this.pendingRotationDegrees;
            if (i == 90 || i == 270) {
                int rotatedHeight = this.currentWidth;
                this.currentWidth = this.currentHeight;
                this.currentHeight = rotatedHeight;
                this.currentPixelWidthHeightRatio = 1.0f / this.currentPixelWidthHeightRatio;
            }
        } else {
            this.currentUnappliedRotationDegrees = this.pendingRotationDegrees;
        }
        codec.setVideoScalingMode(this.scalingMode);
    }

    private void notifyFrameMetadataListener(long presentationTimeUs, long releaseTimeNs, Format format) {
        VideoFrameMetadataListener videoFrameMetadataListener = this.frameMetadataListener;
        if (videoFrameMetadataListener != null) {
            videoFrameMetadataListener.onVideoFrameAboutToBeRendered(presentationTimeUs, releaseTimeNs, format);
        }
    }

    /* access modifiers changed from: protected */
    public long getOutputStreamOffsetUs() {
        return this.outputStreamOffsetUs;
    }

    /* access modifiers changed from: protected */
    public void onProcessedTunneledBuffer(long presentationTimeUs) {
        Format format = updateOutputFormatForTime(presentationTimeUs);
        if (format != null) {
            processOutputFormat(getCodec(), format.width, format.height);
        }
        maybeNotifyVideoSizeChanged();
        maybeNotifyRenderedFirstFrame();
        onProcessedOutputBuffer(presentationTimeUs);
    }

    /* access modifiers changed from: protected */
    @CallSuper
    public void onProcessedOutputBuffer(long presentationTimeUs) {
        this.buffersInCodecCount--;
        while (true) {
            int i = this.pendingOutputStreamOffsetCount;
            if (i != 0 && presentationTimeUs >= this.pendingOutputStreamSwitchTimesUs[0]) {
                long[] jArr = this.pendingOutputStreamOffsetsUs;
                this.outputStreamOffsetUs = jArr[0];
                this.pendingOutputStreamOffsetCount = i - 1;
                System.arraycopy(jArr, 1, jArr, 0, this.pendingOutputStreamOffsetCount);
                long[] jArr2 = this.pendingOutputStreamSwitchTimesUs;
                System.arraycopy(jArr2, 1, jArr2, 0, this.pendingOutputStreamOffsetCount);
            } else {
                return;
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean shouldDropOutputBuffer(long earlyUs, long elapsedRealtimeUs) {
        return isBufferLate(earlyUs);
    }

    /* access modifiers changed from: protected */
    public boolean shouldDropBuffersToKeyframe(long earlyUs, long elapsedRealtimeUs) {
        return isBufferVeryLate(earlyUs);
    }

    /* access modifiers changed from: protected */
    public boolean shouldForceRenderOutputBuffer(long earlyUs, long elapsedSinceLastRenderUs) {
        return isBufferLate(earlyUs) && elapsedSinceLastRenderUs > 100000;
    }

    /* access modifiers changed from: protected */
    public void skipOutputBuffer(MediaCodec codec, int index, long presentationTimeUs) {
        TraceUtil.beginSection("skipVideoBuffer");
        codec.releaseOutputBuffer(index, false);
        TraceUtil.endSection();
        this.decoderCounters.skippedOutputBufferCount++;
    }

    /* access modifiers changed from: protected */
    public void dropOutputBuffer(MediaCodec codec, int index, long presentationTimeUs) {
        TraceUtil.beginSection("dropVideoBuffer");
        codec.releaseOutputBuffer(index, false);
        TraceUtil.endSection();
        updateDroppedBufferCounters(1);
    }

    /* access modifiers changed from: protected */
    public boolean maybeDropBuffersToKeyframe(MediaCodec codec, int index, long presentationTimeUs, long positionUs) throws ExoPlaybackException {
        int droppedSourceBufferCount = skipSource(positionUs);
        if (droppedSourceBufferCount == 0) {
            return false;
        }
        this.decoderCounters.droppedToKeyframeCount++;
        updateDroppedBufferCounters(this.buffersInCodecCount + droppedSourceBufferCount);
        flushCodec();
        return true;
    }

    /* access modifiers changed from: protected */
    public void updateDroppedBufferCounters(int droppedBufferCount) {
        this.decoderCounters.droppedBufferCount += droppedBufferCount;
        this.droppedFrames += droppedBufferCount;
        this.consecutiveDroppedFrameCount += droppedBufferCount;
        this.decoderCounters.maxConsecutiveDroppedBufferCount = Math.max(this.consecutiveDroppedFrameCount, this.decoderCounters.maxConsecutiveDroppedBufferCount);
        int i = this.maxDroppedFramesToNotify;
        if (i > 0 && this.droppedFrames >= i) {
            maybeNotifyDroppedFrames();
        }
    }

    /* access modifiers changed from: protected */
    public void renderOutputBuffer(MediaCodec codec, int index, long presentationTimeUs) {
        maybeNotifyVideoSizeChanged();
        TraceUtil.beginSection("releaseOutputBuffer");
        codec.releaseOutputBuffer(index, true);
        TraceUtil.endSection();
        this.lastRenderTimeUs = SystemClock.elapsedRealtime() * 1000;
        this.decoderCounters.renderedOutputBufferCount++;
        this.consecutiveDroppedFrameCount = 0;
        maybeNotifyRenderedFirstFrame();
    }

    /* access modifiers changed from: protected */
    @TargetApi(21)
    public void renderOutputBufferV21(MediaCodec codec, int index, long presentationTimeUs, long releaseTimeNs) {
        maybeNotifyVideoSizeChanged();
        TraceUtil.beginSection("releaseOutputBuffer");
        codec.releaseOutputBuffer(index, releaseTimeNs);
        TraceUtil.endSection();
        this.lastRenderTimeUs = SystemClock.elapsedRealtime() * 1000;
        this.decoderCounters.renderedOutputBufferCount++;
        this.consecutiveDroppedFrameCount = 0;
        maybeNotifyRenderedFirstFrame();
    }

    private boolean shouldUseDummySurface(MediaCodecInfo codecInfo) {
        return Util.SDK_INT >= 23 && !this.tunneling && !codecNeedsSetOutputSurfaceWorkaround(codecInfo.name) && (!codecInfo.secure || DummySurface.isSecureSupported(this.context));
    }

    private void setJoiningDeadlineMs() {
        this.joiningDeadlineMs = this.allowedJoiningTimeMs > 0 ? SystemClock.elapsedRealtime() + this.allowedJoiningTimeMs : C.TIME_UNSET;
    }

    private void clearRenderedFirstFrame() {
        this.renderedFirstFrame = false;
        if (Util.SDK_INT >= 23 && this.tunneling) {
            MediaCodec codec = getCodec();
            if (codec != null) {
                this.tunnelingOnFrameRenderedListener = new OnFrameRenderedListenerV23(codec);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void maybeNotifyRenderedFirstFrame() {
        if (!this.renderedFirstFrame) {
            this.renderedFirstFrame = true;
            this.eventDispatcher.renderedFirstFrame(this.surface);
        }
    }

    private void maybeRenotifyRenderedFirstFrame() {
        if (this.renderedFirstFrame) {
            this.eventDispatcher.renderedFirstFrame(this.surface);
        }
    }

    private void clearReportedVideoSize() {
        this.reportedWidth = -1;
        this.reportedHeight = -1;
        this.reportedPixelWidthHeightRatio = -1.0f;
        this.reportedUnappliedRotationDegrees = -1;
    }

    private void maybeNotifyVideoSizeChanged() {
        if (this.currentWidth != -1 || this.currentHeight != -1) {
            if (this.reportedWidth != this.currentWidth || this.reportedHeight != this.currentHeight || this.reportedUnappliedRotationDegrees != this.currentUnappliedRotationDegrees || this.reportedPixelWidthHeightRatio != this.currentPixelWidthHeightRatio) {
                this.eventDispatcher.videoSizeChanged(this.currentWidth, this.currentHeight, this.currentUnappliedRotationDegrees, this.currentPixelWidthHeightRatio);
                this.reportedWidth = this.currentWidth;
                this.reportedHeight = this.currentHeight;
                this.reportedUnappliedRotationDegrees = this.currentUnappliedRotationDegrees;
                this.reportedPixelWidthHeightRatio = this.currentPixelWidthHeightRatio;
            }
        }
    }

    private void maybeRenotifyVideoSizeChanged() {
        if (this.reportedWidth != -1 || this.reportedHeight != -1) {
            this.eventDispatcher.videoSizeChanged(this.reportedWidth, this.reportedHeight, this.reportedUnappliedRotationDegrees, this.reportedPixelWidthHeightRatio);
        }
    }

    private void maybeNotifyDroppedFrames() {
        if (this.droppedFrames > 0) {
            long now = SystemClock.elapsedRealtime();
            this.eventDispatcher.droppedFrames(this.droppedFrames, now - this.droppedFrameAccumulationStartTimeMs);
            this.droppedFrames = 0;
            this.droppedFrameAccumulationStartTimeMs = now;
        }
    }

    private static boolean isBufferLate(long earlyUs) {
        return earlyUs < -30000;
    }

    private static boolean isBufferVeryLate(long earlyUs) {
        return earlyUs < -500000;
    }

    @TargetApi(23)
    private static void setOutputSurfaceV23(MediaCodec codec, Surface surface2) {
        codec.setOutputSurface(surface2);
    }

    @TargetApi(21)
    private static void configureTunnelingV21(MediaFormat mediaFormat, int tunnelingAudioSessionId2) {
        mediaFormat.setFeatureEnabled("tunneled-playback", true);
        mediaFormat.setInteger("audio-session-id", tunnelingAudioSessionId2);
    }

    /* access modifiers changed from: protected */
    @SuppressLint({"InlinedApi"})
    public MediaFormat getMediaFormat(Format format, CodecMaxValues codecMaxValues2, float codecOperatingRate, boolean deviceNeedsAutoFrcWorkaround2, int tunnelingAudioSessionId2) {
        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setString("mime", format.sampleMimeType);
        mediaFormat.setInteger("width", format.width);
        mediaFormat.setInteger("height", format.height);
        MediaFormatUtil.setCsdBuffers(mediaFormat, format.initializationData);
        MediaFormatUtil.maybeSetFloat(mediaFormat, "frame-rate", format.frameRate);
        MediaFormatUtil.maybeSetInteger(mediaFormat, "rotation-degrees", format.rotationDegrees);
        MediaFormatUtil.maybeSetColorInfo(mediaFormat, format.colorInfo);
        mediaFormat.setInteger("max-width", codecMaxValues2.width);
        mediaFormat.setInteger("max-height", codecMaxValues2.height);
        MediaFormatUtil.maybeSetInteger(mediaFormat, "max-input-size", codecMaxValues2.inputSize);
        if (Util.SDK_INT >= 23) {
            mediaFormat.setInteger("priority", 0);
            if (codecOperatingRate != -1.0f) {
                mediaFormat.setFloat("operating-rate", codecOperatingRate);
            }
        }
        if (deviceNeedsAutoFrcWorkaround2) {
            mediaFormat.setInteger("auto-frc", 0);
        }
        if (tunnelingAudioSessionId2 != 0) {
            configureTunnelingV21(mediaFormat, tunnelingAudioSessionId2);
        }
        return mediaFormat;
    }

    /* access modifiers changed from: protected */
    public CodecMaxValues getCodecMaxValues(MediaCodecInfo codecInfo, Format format, Format[] streamFormats) throws DecoderQueryException {
        int maxWidth = format.width;
        int maxHeight = format.height;
        int maxInputSize = getMaxInputSize(codecInfo, format);
        if (streamFormats.length == 1) {
            if (maxInputSize != -1) {
                int codecMaxInputSize = getCodecMaxInputSize(codecInfo, format.sampleMimeType, format.width, format.height);
                if (codecMaxInputSize != -1) {
                    maxInputSize = Math.min((int) (((float) maxInputSize) * INITIAL_FORMAT_MAX_INPUT_SIZE_SCALE_FACTOR), codecMaxInputSize);
                }
            }
            return new CodecMaxValues(maxWidth, maxHeight, maxInputSize);
        }
        boolean haveUnknownDimensions = false;
        int maxInputSize2 = maxInputSize;
        int maxHeight2 = maxHeight;
        int maxWidth2 = maxWidth;
        for (Format streamFormat : streamFormats) {
            if (codecInfo.isSeamlessAdaptationSupported(format, streamFormat, false)) {
                haveUnknownDimensions |= streamFormat.width == -1 || streamFormat.height == -1;
                maxWidth2 = Math.max(maxWidth2, streamFormat.width);
                maxHeight2 = Math.max(maxHeight2, streamFormat.height);
                maxInputSize2 = Math.max(maxInputSize2, getMaxInputSize(codecInfo, streamFormat));
            }
        }
        if (haveUnknownDimensions) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Resolutions unknown. Codec max resolution: ");
            sb.append(maxWidth2);
            sb.append("x");
            sb.append(maxHeight2);
            Log.w(str, sb.toString());
            Point codecMaxSize = getCodecMaxSize(codecInfo, format);
            if (codecMaxSize != null) {
                maxWidth2 = Math.max(maxWidth2, codecMaxSize.x);
                maxHeight2 = Math.max(maxHeight2, codecMaxSize.y);
                maxInputSize2 = Math.max(maxInputSize2, getCodecMaxInputSize(codecInfo, format.sampleMimeType, maxWidth2, maxHeight2));
                String str2 = TAG;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Codec max resolution adjusted to: ");
                sb2.append(maxWidth2);
                sb2.append("x");
                sb2.append(maxHeight2);
                Log.w(str2, sb2.toString());
            }
        }
        return new CodecMaxValues(maxWidth2, maxHeight2, maxInputSize2);
    }

    private static Point getCodecMaxSize(MediaCodecInfo codecInfo, Format format) throws DecoderQueryException {
        float aspectRatio;
        int formatShortEdgePx;
        MediaCodecInfo mediaCodecInfo = codecInfo;
        Format format2 = format;
        int i = 0;
        boolean isVerticalVideo = format2.height > format2.width;
        int formatLongEdgePx = isVerticalVideo ? format2.height : format2.width;
        int formatShortEdgePx2 = isVerticalVideo ? format2.width : format2.height;
        float aspectRatio2 = ((float) formatShortEdgePx2) / ((float) formatLongEdgePx);
        int[] iArr = STANDARD_LONG_EDGE_VIDEO_PX;
        int length = iArr.length;
        while (i < length) {
            int longEdgePx = iArr[i];
            int shortEdgePx = (int) (((float) longEdgePx) * aspectRatio2);
            if (longEdgePx <= formatLongEdgePx) {
                float f = aspectRatio2;
            } else if (shortEdgePx <= formatShortEdgePx2) {
                int i2 = formatShortEdgePx2;
                float f2 = aspectRatio2;
            } else {
                if (Util.SDK_INT >= 21) {
                    Point alignedSize = mediaCodecInfo.alignVideoSizeV21(isVerticalVideo ? shortEdgePx : longEdgePx, isVerticalVideo ? longEdgePx : shortEdgePx);
                    formatShortEdgePx = formatShortEdgePx2;
                    aspectRatio = aspectRatio2;
                    if (mediaCodecInfo.isVideoSizeAndRateSupportedV21(alignedSize.x, alignedSize.y, (double) format2.frameRate)) {
                        return alignedSize;
                    }
                } else {
                    formatShortEdgePx = formatShortEdgePx2;
                    aspectRatio = aspectRatio2;
                    int longEdgePx2 = Util.ceilDivide(longEdgePx, 16) * 16;
                    int shortEdgePx2 = Util.ceilDivide(shortEdgePx, 16) * 16;
                    if (longEdgePx2 * shortEdgePx2 <= MediaCodecUtil.maxH264DecodableFrameSize()) {
                        return new Point(isVerticalVideo ? shortEdgePx2 : longEdgePx2, isVerticalVideo ? longEdgePx2 : shortEdgePx2);
                    }
                }
                i++;
                formatShortEdgePx2 = formatShortEdgePx;
                aspectRatio2 = aspectRatio;
            }
            return null;
        }
        float f3 = aspectRatio2;
        return null;
    }

    private static int getMaxInputSize(MediaCodecInfo codecInfo, Format format) {
        if (format.maxInputSize == -1) {
            return getCodecMaxInputSize(codecInfo, format.sampleMimeType, format.width, format.height);
        }
        int totalInitializationDataSize = 0;
        for (int i = 0; i < format.initializationData.size(); i++) {
            totalInitializationDataSize += ((byte[]) format.initializationData.get(i)).length;
        }
        return format.maxInputSize + totalInitializationDataSize;
    }

    private static int getCodecMaxInputSize(MediaCodecInfo codecInfo, String sampleMimeType, int width, int height) {
        char c;
        int minCompressionRatio;
        int maxPixels;
        if (width == -1 || height == -1) {
            return -1;
        }
        switch (sampleMimeType.hashCode()) {
            case -1664118616:
                if (sampleMimeType.equals(MimeTypes.VIDEO_H263)) {
                    c = 0;
                    break;
                }
            case -1662541442:
                if (sampleMimeType.equals(MimeTypes.VIDEO_H265)) {
                    c = 4;
                    break;
                }
            case 1187890754:
                if (sampleMimeType.equals(MimeTypes.VIDEO_MP4V)) {
                    c = 1;
                    break;
                }
            case 1331836730:
                if (sampleMimeType.equals(MimeTypes.VIDEO_H264)) {
                    c = 2;
                    break;
                }
            case 1599127256:
                if (sampleMimeType.equals(MimeTypes.VIDEO_VP8)) {
                    c = 3;
                    break;
                }
            case 1599127257:
                if (sampleMimeType.equals(MimeTypes.VIDEO_VP9)) {
                    c = 5;
                    break;
                }
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
            case 1:
                maxPixels = width * height;
                minCompressionRatio = 2;
                break;
            case 2:
                if (!"BRAVIA 4K 2015".equals(Util.MODEL) && (!"Amazon".equals(Util.MANUFACTURER) || (!"KFSOWI".equals(Util.MODEL) && (!"AFTS".equals(Util.MODEL) || !codecInfo.secure)))) {
                    maxPixels = Util.ceilDivide(width, 16) * Util.ceilDivide(height, 16) * 16 * 16;
                    minCompressionRatio = 2;
                    break;
                } else {
                    return -1;
                }
                break;
            case 3:
                maxPixels = width * height;
                minCompressionRatio = 2;
                break;
            case 4:
            case 5:
                maxPixels = width * height;
                minCompressionRatio = 4;
                break;
            default:
                return -1;
        }
        return (maxPixels * 3) / (minCompressionRatio * 2);
    }

    private static boolean deviceNeedsAutoFrcWorkaround() {
        return Util.SDK_INT <= 22 && "foster".equals(Util.DEVICE) && "NVIDIA".equals(Util.MANUFACTURER);
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Removed duplicated region for block: B:383:0x05ca  */
    /* JADX WARNING: Removed duplicated region for block: B:384:0x05cb  */
    public boolean codecNeedsSetOutputSurfaceWorkaround(String name) {
        char c = 27;
        char c2 = 0;
        if (Util.SDK_INT >= 27 || name.startsWith("OMX.google")) {
            return false;
        }
        synchronized (MediaCodecVideoRenderer.class) {
            if (!evaluatedDeviceNeedsSetOutputSurfaceWorkaround) {
                String str = Util.DEVICE;
                switch (str.hashCode()) {
                    case -2144781245:
                        if (str.equals("GIONEE_SWW1609")) {
                            c = '\'';
                            break;
                        }
                    case -2144781185:
                        if (str.equals("GIONEE_SWW1627")) {
                            c = '(';
                            break;
                        }
                    case -2144781160:
                        if (str.equals("GIONEE_SWW1631")) {
                            c = ')';
                            break;
                        }
                    case -2097309513:
                        if (str.equals("K50a40")) {
                            c = '9';
                            break;
                        }
                    case -2022874474:
                        if (str.equals("CP8676_I02")) {
                            c = 16;
                            break;
                        }
                    case -1978993182:
                        if (str.equals("NX541J")) {
                            c = 'E';
                            break;
                        }
                    case -1978990237:
                        if (str.equals("NX573J")) {
                            c = 'F';
                            break;
                        }
                    case -1936688988:
                        if (str.equals("PGN528")) {
                            c = 'P';
                            break;
                        }
                    case -1936688066:
                        if (str.equals("PGN610")) {
                            c = 'Q';
                            break;
                        }
                    case -1936688065:
                        if (str.equals("PGN611")) {
                            c = 'R';
                            break;
                        }
                    case -1931988508:
                        if (str.equals("AquaPowerM")) {
                            c = 10;
                            break;
                        }
                    case -1696512866:
                        if (str.equals("XT1663")) {
                            c = 's';
                            break;
                        }
                    case -1680025915:
                        if (str.equals("ComioS1")) {
                            c = 15;
                            break;
                        }
                    case -1615810839:
                        if (str.equals("Phantom6")) {
                            c = 'S';
                            break;
                        }
                    case -1554255044:
                        if (str.equals("vernee_M5")) {
                            c = 'l';
                            break;
                        }
                    case -1481772737:
                        if (str.equals("panell_dl")) {
                            c = 'L';
                            break;
                        }
                    case -1481772730:
                        if (str.equals("panell_ds")) {
                            c = 'M';
                            break;
                        }
                    case -1481772729:
                        if (str.equals("panell_dt")) {
                            c = 'N';
                            break;
                        }
                    case -1320080169:
                        if (str.equals("GiONEE_GBL7319")) {
                            c = '%';
                            break;
                        }
                    case -1217592143:
                        if (str.equals("BRAVIA_ATV2")) {
                            c = CharUtils.CR;
                            break;
                        }
                    case -1180384755:
                        if (str.equals("iris60")) {
                            c = '5';
                            break;
                        }
                    case -1139198265:
                        if (str.equals("Slate_Pro")) {
                            c = '`';
                            break;
                        }
                    case -1052835013:
                        if (str.equals("namath")) {
                            c = 'C';
                            break;
                        }
                    case -993250464:
                        if (str.equals("A10-70F")) {
                            c = 3;
                            break;
                        }
                    case -965403638:
                        if (str.equals("s905x018")) {
                            c = 'b';
                            break;
                        }
                    case -958336948:
                        if (str.equals("ELUGA_Ray_X")) {
                            c = 26;
                            break;
                        }
                    case -879245230:
                        if (str.equals("tcl_eu")) {
                            c = 'h';
                            break;
                        }
                    case -842500323:
                        if (str.equals("nicklaus_f")) {
                            c = 'D';
                            break;
                        }
                    case -821392978:
                        if (str.equals("A7000-a")) {
                            c = 6;
                            break;
                        }
                    case -797483286:
                        if (str.equals("SVP-DTV15")) {
                            c = 'a';
                            break;
                        }
                    case -794946968:
                        if (str.equals("watson")) {
                            c = 'm';
                            break;
                        }
                    case -788334647:
                        if (str.equals("whyred")) {
                            c = 'n';
                            break;
                        }
                    case -782144577:
                        if (str.equals("OnePlus5T")) {
                            c = 'G';
                            break;
                        }
                    case -575125681:
                        if (str.equals("GiONEE_CBL7513")) {
                            c = '$';
                            break;
                        }
                    case -521118391:
                        if (str.equals("GIONEE_GBL7360")) {
                            c = '&';
                            break;
                        }
                    case -430914369:
                        if (str.equals("Pixi4-7_3G")) {
                            c = 'T';
                            break;
                        }
                    case -290434366:
                        if (str.equals("taido_row")) {
                            c = 'c';
                            break;
                        }
                    case -282781963:
                        if (str.equals("BLACK-1X")) {
                            c = 12;
                            break;
                        }
                    case -277133239:
                        if (str.equals("Z12_PRO")) {
                            c = 't';
                            break;
                        }
                    case -173639913:
                        if (str.equals("ELUGA_A3_Pro")) {
                            c = 23;
                            break;
                        }
                    case -56598463:
                        if (str.equals("woods_fn")) {
                            c = 'p';
                            break;
                        }
                    case 2126:
                        if (str.equals("C1")) {
                            c = 14;
                            break;
                        }
                    case 2564:
                        if (str.equals("Q5")) {
                            c = '\\';
                            break;
                        }
                    case 2715:
                        if (str.equals("V1")) {
                            c = 'i';
                            break;
                        }
                    case 2719:
                        if (str.equals("V5")) {
                            c = 'k';
                            break;
                        }
                    case 3483:
                        if (str.equals("mh")) {
                            c = '@';
                            break;
                        }
                    case 73405:
                        if (str.equals("JGZ")) {
                            c = '8';
                            break;
                        }
                    case 75739:
                        if (str.equals("M5c")) {
                            c = '<';
                            break;
                        }
                    case 76779:
                        if (str.equals("MX6")) {
                            c = 'B';
                            break;
                        }
                    case 78669:
                        if (str.equals("P85")) {
                            c = 'J';
                            break;
                        }
                    case 79305:
                        if (str.equals("PLE")) {
                            c = 'V';
                            break;
                        }
                    case 80618:
                        if (str.equals("QX1")) {
                            c = '^';
                            break;
                        }
                    case 88274:
                        if (str.equals("Z80")) {
                            c = 'u';
                            break;
                        }
                    case 98846:
                        if (str.equals("cv1")) {
                            c = 19;
                            break;
                        }
                    case 98848:
                        if (str.equals("cv3")) {
                            c = 20;
                            break;
                        }
                    case 99329:
                        if (str.equals("deb")) {
                            c = 21;
                            break;
                        }
                    case 101481:
                        if (str.equals("flo")) {
                            c = '#';
                            break;
                        }
                    case 1513190:
                        if (str.equals("1601")) {
                            c = 0;
                            break;
                        }
                    case 1514184:
                        if (str.equals("1713")) {
                            c = 1;
                            break;
                        }
                    case 1514185:
                        if (str.equals("1714")) {
                            c = 2;
                            break;
                        }
                    case 2436959:
                        if (str.equals("P681")) {
                            c = 'I';
                            break;
                        }
                    case 2463773:
                        if (str.equals("Q350")) {
                            c = 'X';
                            break;
                        }
                    case 2464648:
                        if (str.equals("Q427")) {
                            c = 'Z';
                            break;
                        }
                    case 2689555:
                        if (str.equals("XE2X")) {
                            c = 'r';
                            break;
                        }
                    case 3351335:
                        if (str.equals("mido")) {
                            c = 'A';
                            break;
                        }
                    case 3386211:
                        if (str.equals("p212")) {
                            c = 'H';
                            break;
                        }
                    case 41325051:
                        if (str.equals("MEIZU_M5")) {
                            c = '?';
                            break;
                        }
                    case 55178625:
                        if (str.equals("Aura_Note_2")) {
                            c = 11;
                            break;
                        }
                    case 61542055:
                        if (str.equals("A1601")) {
                            c = 4;
                            break;
                        }
                    case 65355429:
                        if (str.equals("E5643")) {
                            c = 22;
                            break;
                        }
                    case 66214468:
                        if (str.equals("F3111")) {
                            c = 28;
                            break;
                        }
                    case 66214470:
                        if (str.equals("F3113")) {
                            c = 29;
                            break;
                        }
                    case 66214473:
                        if (str.equals("F3116")) {
                            c = 30;
                            break;
                        }
                    case 66215429:
                        if (str.equals("F3211")) {
                            c = 31;
                            break;
                        }
                    case 66215431:
                        if (str.equals("F3213")) {
                            c = ' ';
                            break;
                        }
                    case 66215433:
                        if (str.equals("F3215")) {
                            c = '!';
                            break;
                        }
                    case 66216390:
                        if (str.equals("F3311")) {
                            c = '\"';
                            break;
                        }
                    case 76402249:
                        if (str.equals("PRO7S")) {
                            c = 'W';
                            break;
                        }
                    case 76404105:
                        if (str.equals("Q4260")) {
                            c = 'Y';
                            break;
                        }
                    case 76404911:
                        if (str.equals("Q4310")) {
                            c = '[';
                            break;
                        }
                    case 80963634:
                        if (str.equals("V23GB")) {
                            c = 'j';
                            break;
                        }
                    case 82882791:
                        if (str.equals("X3_HK")) {
                            c = 'q';
                            break;
                        }
                    case 102844228:
                        if (str.equals("le_x6")) {
                            c = ':';
                            break;
                        }
                    case 165221241:
                        if (str.equals("A2016a40")) {
                            c = 5;
                            break;
                        }
                    case 182191441:
                        if (str.equals("CPY83_I00")) {
                            c = 18;
                            break;
                        }
                    case 245388979:
                        if (str.equals("marino_f")) {
                            c = '>';
                            break;
                        }
                    case 287431619:
                        if (str.equals("griffin")) {
                            c = '-';
                            break;
                        }
                    case 307593612:
                        if (str.equals("A7010a48")) {
                            c = 8;
                            break;
                        }
                    case 308517133:
                        if (str.equals("A7020a48")) {
                            c = 9;
                            break;
                        }
                    case 316215098:
                        if (str.equals("TB3-730F")) {
                            c = 'd';
                            break;
                        }
                    case 316215116:
                        if (str.equals("TB3-730X")) {
                            c = 'e';
                            break;
                        }
                    case 316246811:
                        if (str.equals("TB3-850F")) {
                            c = 'f';
                            break;
                        }
                    case 316246818:
                        if (str.equals("TB3-850M")) {
                            c = 'g';
                            break;
                        }
                    case 407160593:
                        if (str.equals("Pixi5-10_4G")) {
                            c = 'U';
                            break;
                        }
                    case 507412548:
                        if (str.equals("QM16XE_U")) {
                            c = ']';
                            break;
                        }
                    case 793982701:
                        if (str.equals("GIONEE_WBL5708")) {
                            c = '*';
                            break;
                        }
                    case 794038622:
                        if (str.equals("GIONEE_WBL7365")) {
                            c = '+';
                            break;
                        }
                    case 794040393:
                        if (str.equals("GIONEE_WBL7519")) {
                            c = ',';
                            break;
                        }
                    case 835649806:
                        if (str.equals("manning")) {
                            c = '=';
                            break;
                        }
                    case 917340916:
                        if (str.equals("A7000plus")) {
                            c = 7;
                            break;
                        }
                    case 958008161:
                        if (str.equals("j2xlteins")) {
                            c = '7';
                            break;
                        }
                    case 1060579533:
                        if (str.equals("panell_d")) {
                            c = 'K';
                            break;
                        }
                    case 1150207623:
                        if (str.equals("LS-5017")) {
                            c = ';';
                            break;
                        }
                    case 1176899427:
                        if (str.equals("itel_S41")) {
                            c = '6';
                            break;
                        }
                    case 1280332038:
                        if (str.equals("hwALE-H")) {
                            c = JsonPointer.SEPARATOR;
                            break;
                        }
                    case 1306947716:
                        if (str.equals("EverStar_S")) {
                            break;
                        }
                    case 1349174697:
                        if (str.equals("htc_e56ml_dtul")) {
                            c = ClassUtils.PACKAGE_SEPARATOR_CHAR;
                            break;
                        }
                    case 1522194893:
                        if (str.equals("woods_f")) {
                            c = 'o';
                            break;
                        }
                    case 1691543273:
                        if (str.equals("CPH1609")) {
                            c = 17;
                            break;
                        }
                    case 1709443163:
                        if (str.equals("iball8735_9806")) {
                            c = '3';
                            break;
                        }
                    case 1865889110:
                        if (str.equals("santoni")) {
                            c = '_';
                            break;
                        }
                    case 1906253259:
                        if (str.equals("PB2-670M")) {
                            c = 'O';
                            break;
                        }
                    case 1977196784:
                        if (str.equals("Infinix-X572")) {
                            c = '4';
                            break;
                        }
                    case 2029784656:
                        if (str.equals("HWBLN-H")) {
                            c = '0';
                            break;
                        }
                    case 2030379515:
                        if (str.equals("HWCAM-H")) {
                            c = '1';
                            break;
                        }
                    case 2047190025:
                        if (str.equals("ELUGA_Note")) {
                            c = 24;
                            break;
                        }
                    case 2047252157:
                        if (str.equals("ELUGA_Prim")) {
                            c = 25;
                            break;
                        }
                    case 2048319463:
                        if (str.equals("HWVNS-H")) {
                            c = '2';
                            break;
                        }
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                    case 11:
                    case 12:
                    case 13:
                    case 14:
                    case 15:
                    case 16:
                    case 17:
                    case 18:
                    case 19:
                    case 20:
                    case 21:
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                    case ' ':
                    case '!':
                    case '\"':
                    case '#':
                    case '$':
                    case '%':
                    case '&':
                    case '\'':
                    case '(':
                    case ')':
                    case '*':
                    case '+':
                    case ',':
                    case '-':
                    case '.':
                    case '/':
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                    case ':':
                    case ';':
                    case '<':
                    case '=':
                    case '>':
                    case '?':
                    case '@':
                    case 'A':
                    case 'B':
                    case 'C':
                    case 'D':
                    case 'E':
                    case 'F':
                    case 'G':
                    case 'H':
                    case 'I':
                    case 'J':
                    case 'K':
                    case 'L':
                    case 'M':
                    case 'N':
                    case 'O':
                    case 'P':
                    case 'Q':
                    case 'R':
                    case 'S':
                    case 'T':
                    case 'U':
                    case 'V':
                    case 'W':
                    case 'X':
                    case 'Y':
                    case 'Z':
                    case '[':
                    case '\\':
                    case ']':
                    case '^':
                    case '_':
                    case '`':
                    case 'a':
                    case 'b':
                    case 'c':
                    case 'd':
                    case 'e':
                    case 'f':
                    case 'g':
                    case 'h':
                    case 'i':
                    case 'j':
                    case 'k':
                    case 'l':
                    case 'm':
                    case 'n':
                    case 'o':
                    case 'p':
                    case 'q':
                    case 'r':
                    case 's':
                    case 't':
                    case 'u':
                        deviceNeedsSetOutputSurfaceWorkaround = true;
                        break;
                }
                String str2 = Util.MODEL;
                int hashCode = str2.hashCode();
                if (hashCode != 2006354) {
                    if (hashCode == 2006367 && str2.equals("AFTN")) {
                        c2 = 1;
                        switch (c2) {
                            case 0:
                            case 1:
                                deviceNeedsSetOutputSurfaceWorkaround = true;
                                break;
                        }
                        evaluatedDeviceNeedsSetOutputSurfaceWorkaround = true;
                    }
                } else if (str2.equals("AFTA")) {
                    switch (c2) {
                        case 0:
                        case 1:
                            break;
                    }
                    evaluatedDeviceNeedsSetOutputSurfaceWorkaround = true;
                }
                c2 = 65535;
                switch (c2) {
                    case 0:
                    case 1:
                        break;
                }
                evaluatedDeviceNeedsSetOutputSurfaceWorkaround = true;
            }
        }
        return deviceNeedsSetOutputSurfaceWorkaround;
    }
}
