package com.google.android.exoplayer2.mediacodec;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodec.CodecException;
import android.media.MediaCodec.CryptoException;
import android.media.MediaCodec.CryptoInfo;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.BaseRenderer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.drm.DrmSession;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.NalUnitUtil;
import com.google.android.exoplayer2.util.TimedValueQueue;
import com.google.android.exoplayer2.util.TraceUtil;
import com.google.android.exoplayer2.util.Util;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

@TargetApi(16)
public abstract class MediaCodecRenderer extends BaseRenderer {
    private static final byte[] ADAPTATION_WORKAROUND_BUFFER = Util.getBytesFromHexString("0000016742C00BDA259000000168CE0F13200000016588840DCE7118A0002FBF1C31C3275D78");
    private static final int ADAPTATION_WORKAROUND_MODE_ALWAYS = 2;
    private static final int ADAPTATION_WORKAROUND_MODE_NEVER = 0;
    private static final int ADAPTATION_WORKAROUND_MODE_SAME_RESOLUTION = 1;
    private static final int ADAPTATION_WORKAROUND_SLICE_WIDTH_HEIGHT = 32;
    protected static final float CODEC_OPERATING_RATE_UNSET = -1.0f;
    protected static final int KEEP_CODEC_RESULT_NO = 0;
    protected static final int KEEP_CODEC_RESULT_YES_WITHOUT_RECONFIGURATION = 1;
    protected static final int KEEP_CODEC_RESULT_YES_WITH_RECONFIGURATION = 3;
    private static final long MAX_CODEC_HOTSWAP_TIME_MS = 1000;
    private static final int RECONFIGURATION_STATE_NONE = 0;
    private static final int RECONFIGURATION_STATE_QUEUE_PENDING = 2;
    private static final int RECONFIGURATION_STATE_WRITE_PENDING = 1;
    private static final int REINITIALIZATION_STATE_NONE = 0;
    private static final int REINITIALIZATION_STATE_SIGNAL_END_OF_STREAM = 1;
    private static final int REINITIALIZATION_STATE_WAIT_END_OF_STREAM = 2;
    private static final String TAG = "MediaCodecRenderer";
    private final float assumedMinimumCodecOperatingRate;
    @Nullable
    private ArrayDeque<MediaCodecInfo> availableCodecInfos;
    private final DecoderInputBuffer buffer;
    private MediaCodec codec;
    private int codecAdaptationWorkaroundMode;
    private boolean codecConfiguredWithOperatingRate;
    private long codecHotswapDeadlineMs;
    @Nullable
    private MediaCodecInfo codecInfo;
    private boolean codecNeedsAdaptationWorkaroundBuffer;
    private boolean codecNeedsDiscardToSpsWorkaround;
    private boolean codecNeedsEosFlushWorkaround;
    private boolean codecNeedsEosOutputExceptionWorkaround;
    private boolean codecNeedsEosPropagation;
    private boolean codecNeedsFlushWorkaround;
    private boolean codecNeedsMonoChannelCountWorkaround;
    private boolean codecNeedsReconfigureWorkaround;
    private float codecOperatingRate;
    private boolean codecReceivedBuffers;
    private boolean codecReceivedEos;
    private int codecReconfigurationState;
    private boolean codecReconfigured;
    private int codecReinitializationState;
    private final List<Long> decodeOnlyPresentationTimestamps;
    protected DecoderCounters decoderCounters;
    private DrmSession<FrameworkMediaCrypto> drmSession;
    @Nullable
    private final DrmSessionManager<FrameworkMediaCrypto> drmSessionManager;
    private final DecoderInputBuffer flagsOnlyBuffer;
    private Format format;
    private final FormatHolder formatHolder;
    private final TimedValueQueue<Format> formatQueue;
    private ByteBuffer[] inputBuffers;
    private int inputIndex;
    private boolean inputStreamEnded;
    private final MediaCodecSelector mediaCodecSelector;
    private ByteBuffer outputBuffer;
    private final BufferInfo outputBufferInfo;
    private ByteBuffer[] outputBuffers;
    private Format outputFormat;
    private int outputIndex;
    private boolean outputStreamEnded;
    private DrmSession<FrameworkMediaCrypto> pendingDrmSession;
    private Format pendingFormat;
    private final boolean playClearSamplesWithoutKeys;
    @Nullable
    private DecoderInitializationException preferredDecoderInitializationException;
    private float rendererOperatingRate;
    private boolean shouldSkipAdaptationWorkaroundOutputBuffer;
    private boolean shouldSkipOutputBuffer;
    private boolean waitingForFirstSyncFrame;
    private boolean waitingForKeys;

    public static class DecoderInitializationException extends Exception {
        private static final int CUSTOM_ERROR_CODE_BASE = -50000;
        private static final int DECODER_QUERY_ERROR = -49998;
        private static final int NO_SUITABLE_DECODER_ERROR = -49999;
        public final String decoderName;
        public final String diagnosticInfo;
        @Nullable
        public final DecoderInitializationException fallbackDecoderInitializationException;
        public final String mimeType;
        public final boolean secureDecoderRequired;

        public DecoderInitializationException(Format format, Throwable cause, boolean secureDecoderRequired2, int errorCode) {
            StringBuilder sb = new StringBuilder();
            sb.append("Decoder init failed: [");
            sb.append(errorCode);
            sb.append("], ");
            sb.append(format);
            this(sb.toString(), cause, format.sampleMimeType, secureDecoderRequired2, null, buildCustomDiagnosticInfo(errorCode), null);
        }

        public DecoderInitializationException(Format format, Throwable cause, boolean secureDecoderRequired2, String decoderName2) {
            String str;
            StringBuilder sb = new StringBuilder();
            sb.append("Decoder init failed: ");
            sb.append(decoderName2);
            sb.append(", ");
            sb.append(format);
            String sb2 = sb.toString();
            String str2 = format.sampleMimeType;
            if (Util.SDK_INT >= 21) {
                str = getDiagnosticInfoV21(cause);
            } else {
                str = null;
            }
            this(sb2, cause, str2, secureDecoderRequired2, decoderName2, str, null);
        }

        private DecoderInitializationException(String message, Throwable cause, String mimeType2, boolean secureDecoderRequired2, @Nullable String decoderName2, @Nullable String diagnosticInfo2, @Nullable DecoderInitializationException fallbackDecoderInitializationException2) {
            super(message, cause);
            this.mimeType = mimeType2;
            this.secureDecoderRequired = secureDecoderRequired2;
            this.decoderName = decoderName2;
            this.diagnosticInfo = diagnosticInfo2;
            this.fallbackDecoderInitializationException = fallbackDecoderInitializationException2;
        }

        /* access modifiers changed from: private */
        @CheckResult
        public DecoderInitializationException copyWithFallbackException(DecoderInitializationException fallbackException) {
            DecoderInitializationException decoderInitializationException = new DecoderInitializationException(getMessage(), getCause(), this.mimeType, this.secureDecoderRequired, this.decoderName, this.diagnosticInfo, fallbackException);
            return decoderInitializationException;
        }

        @TargetApi(21)
        private static String getDiagnosticInfoV21(Throwable cause) {
            if (cause instanceof CodecException) {
                return ((CodecException) cause).getDiagnosticInfo();
            }
            return null;
        }

        private static String buildCustomDiagnosticInfo(int errorCode) {
            String sign = errorCode < 0 ? "neg_" : "";
            StringBuilder sb = new StringBuilder();
            sb.append("com.google.android.exoplayer.MediaCodecTrackRenderer_");
            sb.append(sign);
            sb.append(Math.abs(errorCode));
            return sb.toString();
        }
    }

    /* access modifiers changed from: protected */
    public abstract void configureCodec(MediaCodecInfo mediaCodecInfo, MediaCodec mediaCodec, Format format2, MediaCrypto mediaCrypto, float f) throws DecoderQueryException;

    /* access modifiers changed from: protected */
    public abstract boolean processOutputBuffer(long j, long j2, MediaCodec mediaCodec, ByteBuffer byteBuffer, int i, int i2, long j3, boolean z, Format format2) throws ExoPlaybackException;

    /* access modifiers changed from: protected */
    public abstract int supportsFormat(MediaCodecSelector mediaCodecSelector2, DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2, Format format2) throws DecoderQueryException;

    public MediaCodecRenderer(int trackType, MediaCodecSelector mediaCodecSelector2, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2, boolean playClearSamplesWithoutKeys2, float assumedMinimumCodecOperatingRate2) {
        super(trackType);
        Assertions.checkState(Util.SDK_INT >= 16);
        this.mediaCodecSelector = (MediaCodecSelector) Assertions.checkNotNull(mediaCodecSelector2);
        this.drmSessionManager = drmSessionManager2;
        this.playClearSamplesWithoutKeys = playClearSamplesWithoutKeys2;
        this.assumedMinimumCodecOperatingRate = assumedMinimumCodecOperatingRate2;
        this.buffer = new DecoderInputBuffer(0);
        this.flagsOnlyBuffer = DecoderInputBuffer.newFlagsOnlyInstance();
        this.formatHolder = new FormatHolder();
        this.formatQueue = new TimedValueQueue<>();
        this.decodeOnlyPresentationTimestamps = new ArrayList();
        this.outputBufferInfo = new BufferInfo();
        this.codecReconfigurationState = 0;
        this.codecReinitializationState = 0;
        this.codecOperatingRate = CODEC_OPERATING_RATE_UNSET;
        this.rendererOperatingRate = 1.0f;
    }

    public final int supportsMixedMimeTypeAdaptation() {
        return 8;
    }

    public final int supportsFormat(Format format2) throws ExoPlaybackException {
        try {
            return supportsFormat(this.mediaCodecSelector, this.drmSessionManager, format2);
        } catch (DecoderQueryException e) {
            throw ExoPlaybackException.createForRenderer(e, getIndex());
        }
    }

    /* access modifiers changed from: protected */
    public List<MediaCodecInfo> getDecoderInfos(MediaCodecSelector mediaCodecSelector2, Format format2, boolean requiresSecureDecoder) throws DecoderQueryException {
        return mediaCodecSelector2.getDecoderInfos(format2.sampleMimeType, requiresSecureDecoder);
    }

    /* access modifiers changed from: protected */
    public final void maybeInitCodec() throws ExoPlaybackException {
        if (this.codec == null) {
            Format format2 = this.format;
            if (format2 != null) {
                this.drmSession = this.pendingDrmSession;
                String mimeType = format2.sampleMimeType;
                MediaCrypto wrappedMediaCrypto = null;
                boolean drmSessionRequiresSecureDecoder = false;
                DrmSession<FrameworkMediaCrypto> drmSession2 = this.drmSession;
                if (drmSession2 != null) {
                    FrameworkMediaCrypto mediaCrypto = (FrameworkMediaCrypto) drmSession2.getMediaCrypto();
                    if (mediaCrypto != null) {
                        wrappedMediaCrypto = mediaCrypto.getWrappedMediaCrypto();
                        drmSessionRequiresSecureDecoder = mediaCrypto.requiresSecureDecoderComponent(mimeType);
                    } else if (this.drmSession.getError() == null) {
                        return;
                    }
                    if (deviceNeedsDrmKeysToConfigureCodecWorkaround()) {
                        int drmSessionState = this.drmSession.getState();
                        if (drmSessionState == 1) {
                            throw ExoPlaybackException.createForRenderer(this.drmSession.getError(), getIndex());
                        } else if (drmSessionState != 4) {
                            return;
                        }
                    }
                }
                try {
                    if (initCodecWithFallback(wrappedMediaCrypto, drmSessionRequiresSecureDecoder)) {
                        String codecName = this.codecInfo.name;
                        this.codecAdaptationWorkaroundMode = codecAdaptationWorkaroundMode(codecName);
                        this.codecNeedsReconfigureWorkaround = codecNeedsReconfigureWorkaround(codecName);
                        this.codecNeedsDiscardToSpsWorkaround = codecNeedsDiscardToSpsWorkaround(codecName, this.format);
                        this.codecNeedsFlushWorkaround = codecNeedsFlushWorkaround(codecName);
                        this.codecNeedsEosFlushWorkaround = codecNeedsEosFlushWorkaround(codecName);
                        this.codecNeedsEosOutputExceptionWorkaround = codecNeedsEosOutputExceptionWorkaround(codecName);
                        this.codecNeedsMonoChannelCountWorkaround = codecNeedsMonoChannelCountWorkaround(codecName, this.format);
                        this.codecNeedsEosPropagation = codecNeedsEosPropagationWorkaround(this.codecInfo) || getCodecNeedsEosPropagation();
                        this.codecHotswapDeadlineMs = getState() == 2 ? SystemClock.elapsedRealtime() + 1000 : C.TIME_UNSET;
                        resetInputBuffer();
                        resetOutputBuffer();
                        this.waitingForFirstSyncFrame = true;
                        this.decoderCounters.decoderInitCount++;
                    }
                } catch (DecoderInitializationException e) {
                    throw ExoPlaybackException.createForRenderer(e, getIndex());
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public boolean shouldInitCodec(MediaCodecInfo codecInfo2) {
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean getCodecNeedsEosPropagation() {
        return false;
    }

    /* access modifiers changed from: protected */
    @Nullable
    public final Format updateOutputFormatForTime(long presentationTimeUs) {
        Format format2 = (Format) this.formatQueue.pollFloor(presentationTimeUs);
        if (format2 != null) {
            this.outputFormat = format2;
        }
        return format2;
    }

    /* access modifiers changed from: protected */
    public final MediaCodec getCodec() {
        return this.codec;
    }

    /* access modifiers changed from: protected */
    @Nullable
    public final MediaCodecInfo getCodecInfo() {
        return this.codecInfo;
    }

    /* access modifiers changed from: protected */
    public void onEnabled(boolean joining) throws ExoPlaybackException {
        this.decoderCounters = new DecoderCounters();
    }

    /* access modifiers changed from: protected */
    public void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
        this.inputStreamEnded = false;
        this.outputStreamEnded = false;
        if (this.codec != null) {
            flushCodec();
        }
        this.formatQueue.clear();
    }

    public final void setOperatingRate(float operatingRate) throws ExoPlaybackException {
        this.rendererOperatingRate = operatingRate;
        updateCodecOperatingRate();
    }

    /* access modifiers changed from: protected */
    public void onDisabled() {
        this.format = null;
        this.availableCodecInfos = null;
        try {
            releaseCodec();
            try {
                if (this.drmSession != null) {
                    this.drmSessionManager.releaseSession(this.drmSession);
                }
                try {
                    if (!(this.pendingDrmSession == null || this.pendingDrmSession == this.drmSession)) {
                        this.drmSessionManager.releaseSession(this.pendingDrmSession);
                    }
                } finally {
                    this.drmSession = null;
                    this.pendingDrmSession = null;
                }
            } catch (Throwable th) {
                if (!(this.pendingDrmSession == null || this.pendingDrmSession == this.drmSession)) {
                    this.drmSessionManager.releaseSession(this.pendingDrmSession);
                }
                throw th;
            } finally {
                this.drmSession = null;
                this.pendingDrmSession = null;
            }
        } catch (Throwable th2) {
            try {
                if (!(this.pendingDrmSession == null || this.pendingDrmSession == this.drmSession)) {
                    this.drmSessionManager.releaseSession(this.pendingDrmSession);
                }
                throw th2;
            } finally {
                this.drmSession = null;
                this.pendingDrmSession = null;
            }
        } finally {
            this.drmSession = null;
            this.pendingDrmSession = null;
        }
    }

    /* access modifiers changed from: protected */
    public void releaseCodec() {
        this.codecHotswapDeadlineMs = C.TIME_UNSET;
        resetInputBuffer();
        resetOutputBuffer();
        this.waitingForKeys = false;
        this.shouldSkipOutputBuffer = false;
        this.decodeOnlyPresentationTimestamps.clear();
        resetCodecBuffers();
        this.codecInfo = null;
        this.codecReconfigured = false;
        this.codecReceivedBuffers = false;
        this.codecNeedsDiscardToSpsWorkaround = false;
        this.codecNeedsFlushWorkaround = false;
        this.codecAdaptationWorkaroundMode = 0;
        this.codecNeedsReconfigureWorkaround = false;
        this.codecNeedsEosFlushWorkaround = false;
        this.codecNeedsMonoChannelCountWorkaround = false;
        this.codecNeedsAdaptationWorkaroundBuffer = false;
        this.shouldSkipAdaptationWorkaroundOutputBuffer = false;
        this.codecNeedsEosPropagation = false;
        this.codecReceivedEos = false;
        this.codecReconfigurationState = 0;
        this.codecReinitializationState = 0;
        this.codecConfiguredWithOperatingRate = false;
        if (this.codec != null) {
            this.decoderCounters.decoderReleaseCount++;
            try {
                this.codec.stop();
                try {
                    this.codec.release();
                    this.codec = null;
                    DrmSession<FrameworkMediaCrypto> drmSession2 = this.drmSession;
                    if (drmSession2 != null && this.pendingDrmSession != drmSession2) {
                        try {
                            this.drmSessionManager.releaseSession(drmSession2);
                        } finally {
                            this.drmSession = null;
                        }
                    }
                } catch (Throwable th) {
                    this.codec = null;
                    DrmSession<FrameworkMediaCrypto> drmSession3 = this.drmSession;
                    if (!(drmSession3 == null || this.pendingDrmSession == drmSession3)) {
                        this.drmSessionManager.releaseSession(drmSession3);
                    }
                    throw th;
                } finally {
                    this.drmSession = null;
                }
            } catch (Throwable th2) {
                this.codec = null;
                DrmSession<FrameworkMediaCrypto> drmSession4 = this.drmSession;
                if (!(drmSession4 == null || this.pendingDrmSession == drmSession4)) {
                    try {
                        this.drmSessionManager.releaseSession(drmSession4);
                    } finally {
                        this.drmSession = null;
                    }
                }
                throw th2;
            } finally {
                this.drmSession = null;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onStarted() {
    }

    /* access modifiers changed from: protected */
    public void onStopped() {
    }

    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        if (this.outputStreamEnded) {
            renderToEndOfStream();
            return;
        }
        if (this.format == null) {
            this.flagsOnlyBuffer.clear();
            int result = readSource(this.formatHolder, this.flagsOnlyBuffer, true);
            if (result == -5) {
                onInputFormatChanged(this.formatHolder.format);
            } else if (result == -4) {
                Assertions.checkState(this.flagsOnlyBuffer.isEndOfStream());
                this.inputStreamEnded = true;
                processEndOfStream();
                return;
            } else {
                return;
            }
        }
        maybeInitCodec();
        if (this.codec != null) {
            TraceUtil.beginSection("drainAndFeed");
            do {
            } while (drainOutputBuffer(positionUs, elapsedRealtimeUs));
            do {
            } while (feedInputBuffer());
            TraceUtil.endSection();
        } else {
            this.decoderCounters.skippedInputBufferCount += skipSource(positionUs);
            this.flagsOnlyBuffer.clear();
            int result2 = readSource(this.formatHolder, this.flagsOnlyBuffer, false);
            if (result2 == -5) {
                onInputFormatChanged(this.formatHolder.format);
            } else if (result2 == -4) {
                Assertions.checkState(this.flagsOnlyBuffer.isEndOfStream());
                this.inputStreamEnded = true;
                processEndOfStream();
            }
        }
        this.decoderCounters.ensureUpdated();
    }

    /* access modifiers changed from: protected */
    public void flushCodec() throws ExoPlaybackException {
        this.codecHotswapDeadlineMs = C.TIME_UNSET;
        resetInputBuffer();
        resetOutputBuffer();
        this.waitingForFirstSyncFrame = true;
        this.waitingForKeys = false;
        this.shouldSkipOutputBuffer = false;
        this.decodeOnlyPresentationTimestamps.clear();
        this.codecNeedsAdaptationWorkaroundBuffer = false;
        this.shouldSkipAdaptationWorkaroundOutputBuffer = false;
        if (this.codecNeedsFlushWorkaround || (this.codecNeedsEosFlushWorkaround && this.codecReceivedEos)) {
            releaseCodec();
            maybeInitCodec();
        } else if (this.codecReinitializationState != 0) {
            releaseCodec();
            maybeInitCodec();
        } else {
            this.codec.flush();
            this.codecReceivedBuffers = false;
        }
        if (this.codecReconfigured && this.format != null) {
            this.codecReconfigurationState = 1;
        }
    }

    private boolean initCodecWithFallback(MediaCrypto crypto, boolean drmSessionRequiresSecureDecoder) throws DecoderInitializationException {
        if (this.availableCodecInfos == null) {
            try {
                this.availableCodecInfos = new ArrayDeque<>(getAvailableCodecInfos(drmSessionRequiresSecureDecoder));
                this.preferredDecoderInitializationException = null;
            } catch (DecoderQueryException e) {
                throw new DecoderInitializationException(this.format, (Throwable) e, drmSessionRequiresSecureDecoder, -49998);
            }
        }
        if (!this.availableCodecInfos.isEmpty()) {
            do {
                MediaCodecInfo codecInfo2 = (MediaCodecInfo) this.availableCodecInfos.peekFirst();
                if (!shouldInitCodec(codecInfo2)) {
                    return false;
                }
                try {
                    initCodec(codecInfo2, crypto);
                    return true;
                } catch (Exception e2) {
                    String str = TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Failed to initialize decoder: ");
                    sb.append(codecInfo2);
                    Log.w(str, sb.toString(), e2);
                    this.availableCodecInfos.removeFirst();
                    DecoderInitializationException exception = new DecoderInitializationException(this.format, (Throwable) e2, drmSessionRequiresSecureDecoder, codecInfo2.name);
                    DecoderInitializationException decoderInitializationException = this.preferredDecoderInitializationException;
                    if (decoderInitializationException == null) {
                        this.preferredDecoderInitializationException = exception;
                    } else {
                        this.preferredDecoderInitializationException = decoderInitializationException.copyWithFallbackException(exception);
                    }
                    if (this.availableCodecInfos.isEmpty()) {
                        throw this.preferredDecoderInitializationException;
                    }
                }
            } while (this.availableCodecInfos.isEmpty());
            throw this.preferredDecoderInitializationException;
        }
        throw new DecoderInitializationException(this.format, (Throwable) null, drmSessionRequiresSecureDecoder, -49999);
    }

    private List<MediaCodecInfo> getAvailableCodecInfos(boolean drmSessionRequiresSecureDecoder) throws DecoderQueryException {
        List<MediaCodecInfo> codecInfos = getDecoderInfos(this.mediaCodecSelector, this.format, drmSessionRequiresSecureDecoder);
        if (codecInfos.isEmpty() && drmSessionRequiresSecureDecoder) {
            codecInfos = getDecoderInfos(this.mediaCodecSelector, this.format, false);
            if (!codecInfos.isEmpty()) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Drm session requires secure decoder for ");
                sb.append(this.format.sampleMimeType);
                sb.append(", but no secure decoder available. Trying to proceed with ");
                sb.append(codecInfos);
                sb.append(".");
                Log.w(str, sb.toString());
            }
        }
        return codecInfos;
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x0081  */
    private void initCodec(MediaCodecInfo codecInfo2, MediaCrypto crypto) throws Exception {
        MediaCodec codec2;
        MediaCodecInfo mediaCodecInfo = codecInfo2;
        String name = mediaCodecInfo.name;
        updateCodecOperatingRate();
        boolean configureWithOperatingRate = this.codecOperatingRate > this.assumedMinimumCodecOperatingRate;
        try {
            long codecInitializingTimestamp = SystemClock.elapsedRealtime();
            StringBuilder sb = new StringBuilder();
            sb.append("createCodec:");
            sb.append(name);
            TraceUtil.beginSection(sb.toString());
            codec2 = MediaCodec.createByCodecName(name);
            try {
                TraceUtil.endSection();
                TraceUtil.beginSection("configureCodec");
                configureCodec(codecInfo2, codec2, this.format, crypto, configureWithOperatingRate ? this.codecOperatingRate : CODEC_OPERATING_RATE_UNSET);
                this.codecConfiguredWithOperatingRate = configureWithOperatingRate;
                TraceUtil.endSection();
                TraceUtil.beginSection("startCodec");
                codec2.start();
                TraceUtil.endSection();
                long codecInitializedTimestamp = SystemClock.elapsedRealtime();
                getCodecBuffers(codec2);
                this.codec = codec2;
                this.codecInfo = mediaCodecInfo;
                onCodecInitialized(name, codecInitializedTimestamp, codecInitializedTimestamp - codecInitializingTimestamp);
            } catch (Exception e) {
                e = e;
                if (codec2 != null) {
                }
                throw e;
            }
        } catch (Exception e2) {
            e = e2;
            codec2 = null;
            if (codec2 != null) {
                resetCodecBuffers();
                codec2.release();
            }
            throw e;
        }
    }

    private void getCodecBuffers(MediaCodec codec2) {
        if (Util.SDK_INT < 21) {
            this.inputBuffers = codec2.getInputBuffers();
            this.outputBuffers = codec2.getOutputBuffers();
        }
    }

    private void resetCodecBuffers() {
        if (Util.SDK_INT < 21) {
            this.inputBuffers = null;
            this.outputBuffers = null;
        }
    }

    private ByteBuffer getInputBuffer(int inputIndex2) {
        if (Util.SDK_INT >= 21) {
            return this.codec.getInputBuffer(inputIndex2);
        }
        return this.inputBuffers[inputIndex2];
    }

    private ByteBuffer getOutputBuffer(int outputIndex2) {
        if (Util.SDK_INT >= 21) {
            return this.codec.getOutputBuffer(outputIndex2);
        }
        return this.outputBuffers[outputIndex2];
    }

    private boolean hasOutputBuffer() {
        return this.outputIndex >= 0;
    }

    private void resetInputBuffer() {
        this.inputIndex = -1;
        this.buffer.data = null;
    }

    private void resetOutputBuffer() {
        this.outputIndex = -1;
        this.outputBuffer = null;
    }

    private boolean feedInputBuffer() throws ExoPlaybackException {
        int result;
        MediaCodec mediaCodec = this.codec;
        if (mediaCodec == null || this.codecReinitializationState == 2 || this.inputStreamEnded) {
            return false;
        }
        if (this.inputIndex < 0) {
            this.inputIndex = mediaCodec.dequeueInputBuffer(0);
            int i = this.inputIndex;
            if (i < 0) {
                return false;
            }
            this.buffer.data = getInputBuffer(i);
            this.buffer.clear();
        }
        if (this.codecReinitializationState == 1) {
            if (!this.codecNeedsEosPropagation) {
                this.codecReceivedEos = true;
                this.codec.queueInputBuffer(this.inputIndex, 0, 0, 0, 4);
                resetInputBuffer();
            }
            this.codecReinitializationState = 2;
            return false;
        } else if (this.codecNeedsAdaptationWorkaroundBuffer) {
            this.codecNeedsAdaptationWorkaroundBuffer = false;
            this.buffer.data.put(ADAPTATION_WORKAROUND_BUFFER);
            this.codec.queueInputBuffer(this.inputIndex, 0, ADAPTATION_WORKAROUND_BUFFER.length, 0, 0);
            resetInputBuffer();
            this.codecReceivedBuffers = true;
            return true;
        } else {
            int adaptiveReconfigurationBytes = 0;
            if (this.waitingForKeys) {
                result = -4;
            } else {
                if (this.codecReconfigurationState == 1) {
                    for (int i2 = 0; i2 < this.format.initializationData.size(); i2++) {
                        this.buffer.data.put((byte[]) this.format.initializationData.get(i2));
                    }
                    this.codecReconfigurationState = 2;
                }
                adaptiveReconfigurationBytes = this.buffer.data.position();
                result = readSource(this.formatHolder, this.buffer, false);
            }
            if (result == -3) {
                return false;
            }
            if (result == -5) {
                if (this.codecReconfigurationState == 2) {
                    this.buffer.clear();
                    this.codecReconfigurationState = 1;
                }
                onInputFormatChanged(this.formatHolder.format);
                return true;
            } else if (this.buffer.isEndOfStream()) {
                if (this.codecReconfigurationState == 2) {
                    this.buffer.clear();
                    this.codecReconfigurationState = 1;
                }
                this.inputStreamEnded = true;
                if (!this.codecReceivedBuffers) {
                    processEndOfStream();
                    return false;
                }
                try {
                    if (!this.codecNeedsEosPropagation) {
                        this.codecReceivedEos = true;
                        this.codec.queueInputBuffer(this.inputIndex, 0, 0, 0, 4);
                        resetInputBuffer();
                    }
                    return false;
                } catch (CryptoException e) {
                    throw ExoPlaybackException.createForRenderer(e, getIndex());
                }
            } else if (!this.waitingForFirstSyncFrame || this.buffer.isKeyFrame()) {
                this.waitingForFirstSyncFrame = false;
                boolean bufferEncrypted = this.buffer.isEncrypted();
                this.waitingForKeys = shouldWaitForKeys(bufferEncrypted);
                if (this.waitingForKeys) {
                    return false;
                }
                if (this.codecNeedsDiscardToSpsWorkaround && !bufferEncrypted) {
                    NalUnitUtil.discardToSps(this.buffer.data);
                    if (this.buffer.data.position() == 0) {
                        return true;
                    }
                    this.codecNeedsDiscardToSpsWorkaround = false;
                }
                try {
                    long presentationTimeUs = this.buffer.timeUs;
                    if (this.buffer.isDecodeOnly()) {
                        this.decodeOnlyPresentationTimestamps.add(Long.valueOf(presentationTimeUs));
                    }
                    if (this.pendingFormat != null) {
                        this.formatQueue.add(presentationTimeUs, this.pendingFormat);
                        this.pendingFormat = null;
                    }
                    this.buffer.flip();
                    onQueueInputBuffer(this.buffer);
                    if (bufferEncrypted) {
                        this.codec.queueSecureInputBuffer(this.inputIndex, 0, getFrameworkCryptoInfo(this.buffer, adaptiveReconfigurationBytes), presentationTimeUs, 0);
                    } else {
                        this.codec.queueInputBuffer(this.inputIndex, 0, this.buffer.data.limit(), presentationTimeUs, 0);
                    }
                    resetInputBuffer();
                    this.codecReceivedBuffers = true;
                    this.codecReconfigurationState = 0;
                    this.decoderCounters.inputBufferCount++;
                    return true;
                } catch (CryptoException e2) {
                    throw ExoPlaybackException.createForRenderer(e2, getIndex());
                }
            } else {
                this.buffer.clear();
                if (this.codecReconfigurationState == 2) {
                    this.codecReconfigurationState = 1;
                }
                return true;
            }
        }
    }

    private boolean shouldWaitForKeys(boolean bufferEncrypted) throws ExoPlaybackException {
        boolean z = false;
        if (this.drmSession == null || (!bufferEncrypted && this.playClearSamplesWithoutKeys)) {
            return false;
        }
        int drmSessionState = this.drmSession.getState();
        if (drmSessionState != 1) {
            if (drmSessionState != 4) {
                z = true;
            }
            return z;
        }
        throw ExoPlaybackException.createForRenderer(this.drmSession.getError(), getIndex());
    }

    /* access modifiers changed from: protected */
    public void onCodecInitialized(String name, long initializedTimestampMs, long initializationDurationMs) {
    }

    /* access modifiers changed from: protected */
    public void onInputFormatChanged(Format newFormat) throws ExoPlaybackException {
        Format oldFormat = this.format;
        this.format = newFormat;
        this.pendingFormat = newFormat;
        boolean z = true;
        if (!Util.areEqual(this.format.drmInitData, oldFormat == null ? null : oldFormat.drmInitData)) {
            if (this.format.drmInitData != null) {
                DrmSessionManager<FrameworkMediaCrypto> drmSessionManager2 = this.drmSessionManager;
                if (drmSessionManager2 != null) {
                    this.pendingDrmSession = drmSessionManager2.acquireSession(Looper.myLooper(), this.format.drmInitData);
                    DrmSession<FrameworkMediaCrypto> drmSession2 = this.pendingDrmSession;
                    if (drmSession2 == this.drmSession) {
                        this.drmSessionManager.releaseSession(drmSession2);
                    }
                } else {
                    throw ExoPlaybackException.createForRenderer(new IllegalStateException("Media requires a DrmSessionManager"), getIndex());
                }
            } else {
                this.pendingDrmSession = null;
            }
        }
        boolean keepingCodec = false;
        if (this.pendingDrmSession == this.drmSession) {
            MediaCodec mediaCodec = this.codec;
            if (mediaCodec != null) {
                int canKeepCodec = canKeepCodec(mediaCodec, this.codecInfo, oldFormat, this.format);
                if (canKeepCodec != 3) {
                    switch (canKeepCodec) {
                        case 0:
                            break;
                        case 1:
                            keepingCodec = true;
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                } else if (!this.codecNeedsReconfigureWorkaround) {
                    keepingCodec = true;
                    this.codecReconfigured = true;
                    this.codecReconfigurationState = 1;
                    int i = this.codecAdaptationWorkaroundMode;
                    if (!(i == 2 || (i == 1 && this.format.width == oldFormat.width && this.format.height == oldFormat.height))) {
                        z = false;
                    }
                    this.codecNeedsAdaptationWorkaroundBuffer = z;
                }
            }
        }
        if (!keepingCodec) {
            reinitializeCodec();
        } else {
            updateCodecOperatingRate();
        }
    }

    /* access modifiers changed from: protected */
    public void onOutputFormatChanged(MediaCodec codec2, MediaFormat outputFormat2) throws ExoPlaybackException {
    }

    /* access modifiers changed from: protected */
    public void onQueueInputBuffer(DecoderInputBuffer buffer2) {
    }

    /* access modifiers changed from: protected */
    public void onProcessedOutputBuffer(long presentationTimeUs) {
    }

    /* access modifiers changed from: protected */
    public int canKeepCodec(MediaCodec codec2, MediaCodecInfo codecInfo2, Format oldFormat, Format newFormat) {
        return 0;
    }

    public boolean isEnded() {
        return this.outputStreamEnded;
    }

    public boolean isReady() {
        return this.format != null && !this.waitingForKeys && (isSourceReady() || hasOutputBuffer() || (this.codecHotswapDeadlineMs != C.TIME_UNSET && SystemClock.elapsedRealtime() < this.codecHotswapDeadlineMs));
    }

    /* access modifiers changed from: protected */
    public long getDequeueOutputBufferTimeoutUs() {
        return 0;
    }

    /* access modifiers changed from: protected */
    public float getCodecOperatingRate(float operatingRate, Format format2, Format[] streamFormats) {
        return CODEC_OPERATING_RATE_UNSET;
    }

    private void updateCodecOperatingRate() throws ExoPlaybackException {
        if (this.format != null && Util.SDK_INT >= 23) {
            float codecOperatingRate2 = getCodecOperatingRate(this.rendererOperatingRate, this.format, getStreamFormats());
            if (this.codecOperatingRate != codecOperatingRate2) {
                this.codecOperatingRate = codecOperatingRate2;
                if (this.codec != null && this.codecReinitializationState == 0) {
                    if (codecOperatingRate2 == CODEC_OPERATING_RATE_UNSET && this.codecConfiguredWithOperatingRate) {
                        reinitializeCodec();
                    } else if (codecOperatingRate2 != CODEC_OPERATING_RATE_UNSET && (this.codecConfiguredWithOperatingRate || codecOperatingRate2 > this.assumedMinimumCodecOperatingRate)) {
                        Bundle codecParameters = new Bundle();
                        codecParameters.putFloat("operating-rate", codecOperatingRate2);
                        this.codec.setParameters(codecParameters);
                        this.codecConfiguredWithOperatingRate = true;
                    }
                }
            }
        }
    }

    private void reinitializeCodec() throws ExoPlaybackException {
        this.availableCodecInfos = null;
        if (this.codecReceivedBuffers) {
            this.codecReinitializationState = 1;
            return;
        }
        releaseCodec();
        maybeInitCodec();
    }

    /* JADX WARNING: Removed duplicated region for block: B:59:0x00dc  */
    private boolean drainOutputBuffer(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        boolean z;
        boolean processedOutputBuffer;
        int outputIndex2;
        if (!hasOutputBuffer()) {
            if (!this.codecNeedsEosOutputExceptionWorkaround || !this.codecReceivedEos) {
                outputIndex2 = this.codec.dequeueOutputBuffer(this.outputBufferInfo, getDequeueOutputBufferTimeoutUs());
            } else {
                try {
                    outputIndex2 = this.codec.dequeueOutputBuffer(this.outputBufferInfo, getDequeueOutputBufferTimeoutUs());
                } catch (IllegalStateException e) {
                    processEndOfStream();
                    if (this.outputStreamEnded) {
                        releaseCodec();
                    }
                    return false;
                }
            }
            if (outputIndex2 < 0) {
                if (outputIndex2 == -2) {
                    processOutputFormat();
                    return true;
                } else if (outputIndex2 == -3) {
                    processOutputBuffersChanged();
                    return true;
                } else {
                    if (this.codecNeedsEosPropagation && (this.inputStreamEnded || this.codecReinitializationState == 2)) {
                        processEndOfStream();
                    }
                    return false;
                }
            } else if (this.shouldSkipAdaptationWorkaroundOutputBuffer) {
                this.shouldSkipAdaptationWorkaroundOutputBuffer = false;
                this.codec.releaseOutputBuffer(outputIndex2, false);
                return true;
            } else if (this.outputBufferInfo.size != 0 || (this.outputBufferInfo.flags & 4) == 0) {
                this.outputIndex = outputIndex2;
                this.outputBuffer = getOutputBuffer(outputIndex2);
                ByteBuffer byteBuffer = this.outputBuffer;
                if (byteBuffer != null) {
                    byteBuffer.position(this.outputBufferInfo.offset);
                    this.outputBuffer.limit(this.outputBufferInfo.offset + this.outputBufferInfo.size);
                }
                this.shouldSkipOutputBuffer = shouldSkipOutputBuffer(this.outputBufferInfo.presentationTimeUs);
                updateOutputFormatForTime(this.outputBufferInfo.presentationTimeUs);
            } else {
                processEndOfStream();
                return false;
            }
        }
        if (this.codecNeedsEosOutputExceptionWorkaround == 0 || !this.codecReceivedEos) {
            z = false;
            processedOutputBuffer = processOutputBuffer(positionUs, elapsedRealtimeUs, this.codec, this.outputBuffer, this.outputIndex, this.outputBufferInfo.flags, this.outputBufferInfo.presentationTimeUs, this.shouldSkipOutputBuffer, this.outputFormat);
        } else {
            try {
                z = false;
                try {
                    processedOutputBuffer = processOutputBuffer(positionUs, elapsedRealtimeUs, this.codec, this.outputBuffer, this.outputIndex, this.outputBufferInfo.flags, this.outputBufferInfo.presentationTimeUs, this.shouldSkipOutputBuffer, this.outputFormat);
                } catch (IllegalStateException e2) {
                    processEndOfStream();
                    if (this.outputStreamEnded) {
                        releaseCodec();
                    }
                    return z;
                }
            } catch (IllegalStateException e3) {
                z = false;
                processEndOfStream();
                if (this.outputStreamEnded) {
                }
                return z;
            }
        }
        if (processedOutputBuffer) {
            onProcessedOutputBuffer(this.outputBufferInfo.presentationTimeUs);
            boolean isEndOfStream = (this.outputBufferInfo.flags & 4) != 0;
            resetOutputBuffer();
            if (!isEndOfStream) {
                return true;
            }
            processEndOfStream();
        }
        return z;
    }

    private void processOutputFormat() throws ExoPlaybackException {
        MediaFormat format2 = this.codec.getOutputFormat();
        if (this.codecAdaptationWorkaroundMode != 0 && format2.getInteger("width") == 32 && format2.getInteger("height") == 32) {
            this.shouldSkipAdaptationWorkaroundOutputBuffer = true;
            return;
        }
        if (this.codecNeedsMonoChannelCountWorkaround) {
            format2.setInteger("channel-count", 1);
        }
        onOutputFormatChanged(this.codec, format2);
    }

    private void processOutputBuffersChanged() {
        if (Util.SDK_INT < 21) {
            this.outputBuffers = this.codec.getOutputBuffers();
        }
    }

    /* access modifiers changed from: protected */
    public void renderToEndOfStream() throws ExoPlaybackException {
    }

    private void processEndOfStream() throws ExoPlaybackException {
        if (this.codecReinitializationState == 2) {
            releaseCodec();
            maybeInitCodec();
            return;
        }
        this.outputStreamEnded = true;
        renderToEndOfStream();
    }

    private boolean shouldSkipOutputBuffer(long presentationTimeUs) {
        int size = this.decodeOnlyPresentationTimestamps.size();
        for (int i = 0; i < size; i++) {
            if (((Long) this.decodeOnlyPresentationTimestamps.get(i)).longValue() == presentationTimeUs) {
                this.decodeOnlyPresentationTimestamps.remove(i);
                return true;
            }
        }
        return false;
    }

    private static CryptoInfo getFrameworkCryptoInfo(DecoderInputBuffer buffer2, int adaptiveReconfigurationBytes) {
        CryptoInfo cryptoInfo = buffer2.cryptoInfo.getFrameworkCryptoInfoV16();
        if (adaptiveReconfigurationBytes == 0) {
            return cryptoInfo;
        }
        if (cryptoInfo.numBytesOfClearData == null) {
            cryptoInfo.numBytesOfClearData = new int[1];
        }
        int[] iArr = cryptoInfo.numBytesOfClearData;
        iArr[0] = iArr[0] + adaptiveReconfigurationBytes;
        return cryptoInfo;
    }

    private boolean deviceNeedsDrmKeysToConfigureCodecWorkaround() {
        return "Amazon".equals(Util.MANUFACTURER) && ("AFTM".equals(Util.MODEL) || "AFTB".equals(Util.MODEL));
    }

    private static boolean codecNeedsFlushWorkaround(String name) {
        return Util.SDK_INT < 18 || (Util.SDK_INT == 18 && ("OMX.SEC.avc.dec".equals(name) || "OMX.SEC.avc.dec.secure".equals(name))) || (Util.SDK_INT == 19 && Util.MODEL.startsWith("SM-G800") && ("OMX.Exynos.avc.dec".equals(name) || "OMX.Exynos.avc.dec.secure".equals(name)));
    }

    private int codecAdaptationWorkaroundMode(String name) {
        if (Util.SDK_INT <= 25 && "OMX.Exynos.avc.dec.secure".equals(name) && (Util.MODEL.startsWith("SM-T585") || Util.MODEL.startsWith("SM-A510") || Util.MODEL.startsWith("SM-A520") || Util.MODEL.startsWith("SM-J700"))) {
            return 2;
        }
        if (Util.SDK_INT >= 24 || ((!"OMX.Nvidia.h264.decode".equals(name) && !"OMX.Nvidia.h264.decode.secure".equals(name)) || (!"flounder".equals(Util.DEVICE) && !"flounder_lte".equals(Util.DEVICE) && !"grouper".equals(Util.DEVICE) && !"tilapia".equals(Util.DEVICE)))) {
            return 0;
        }
        return 1;
    }

    private static boolean codecNeedsReconfigureWorkaround(String name) {
        return Util.MODEL.startsWith("SM-T230") && "OMX.MARVELL.VIDEO.HW.CODA7542DECODER".equals(name);
    }

    private static boolean codecNeedsDiscardToSpsWorkaround(String name, Format format2) {
        return Util.SDK_INT < 21 && format2.initializationData.isEmpty() && "OMX.MTK.VIDEO.DECODER.AVC".equals(name);
    }

    private static boolean codecNeedsEosPropagationWorkaround(MediaCodecInfo codecInfo2) {
        String name = codecInfo2.name;
        return (Util.SDK_INT <= 17 && ("OMX.rk.video_decoder.avc".equals(name) || "OMX.allwinner.video.decoder.avc".equals(name))) || ("Amazon".equals(Util.MANUFACTURER) && "AFTS".equals(Util.MODEL) && codecInfo2.secure);
    }

    private static boolean codecNeedsEosFlushWorkaround(String name) {
        return (Util.SDK_INT <= 23 && "OMX.google.vorbis.decoder".equals(name)) || (Util.SDK_INT <= 19 && "hb2000".equals(Util.DEVICE) && ("OMX.amlogic.avc.decoder.awesome".equals(name) || "OMX.amlogic.avc.decoder.awesome.secure".equals(name)));
    }

    private static boolean codecNeedsEosOutputExceptionWorkaround(String name) {
        return Util.SDK_INT == 21 && "OMX.google.aac.decoder".equals(name);
    }

    private static boolean codecNeedsMonoChannelCountWorkaround(String name, Format format2) {
        if (Util.SDK_INT > 18 || format2.channelCount != 1 || !"OMX.MTK.AUDIO.DECODER.MP3".equals(name)) {
            return false;
        }
        return true;
    }
}
