package com.google.android.exoplayer2.mediacodec;

import android.annotation.TargetApi;
import android.graphics.Point;
import android.media.MediaCodecInfo.AudioCapabilities;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaCodecInfo.VideoCapabilities;
import android.support.annotation.Nullable;
import android.util.Pair;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

@TargetApi(16)
public final class MediaCodecInfo {
    public static final int MAX_SUPPORTED_INSTANCES_UNKNOWN = -1;
    public static final String TAG = "MediaCodecInfo";
    public final boolean adaptive;
    @Nullable
    public final CodecCapabilities capabilities;
    private final boolean isVideo;
    @Nullable
    public final String mimeType;
    public final String name;
    public final boolean passthrough;
    public final boolean secure;
    public final boolean tunneling;

    public static MediaCodecInfo newPassthroughInstance(String name2) {
        MediaCodecInfo mediaCodecInfo = new MediaCodecInfo(name2, null, null, true, false, false);
        return mediaCodecInfo;
    }

    public static MediaCodecInfo newInstance(String name2, String mimeType2, CodecCapabilities capabilities2) {
        MediaCodecInfo mediaCodecInfo = new MediaCodecInfo(name2, mimeType2, capabilities2, false, false, false);
        return mediaCodecInfo;
    }

    public static MediaCodecInfo newInstance(String name2, String mimeType2, CodecCapabilities capabilities2, boolean forceDisableAdaptive, boolean forceSecure) {
        MediaCodecInfo mediaCodecInfo = new MediaCodecInfo(name2, mimeType2, capabilities2, false, forceDisableAdaptive, forceSecure);
        return mediaCodecInfo;
    }

    private MediaCodecInfo(String name2, @Nullable String mimeType2, @Nullable CodecCapabilities capabilities2, boolean passthrough2, boolean forceDisableAdaptive, boolean forceSecure) {
        this.name = (String) Assertions.checkNotNull(name2);
        this.mimeType = mimeType2;
        this.capabilities = capabilities2;
        this.passthrough = passthrough2;
        boolean z = true;
        this.adaptive = !forceDisableAdaptive && capabilities2 != null && isAdaptive(capabilities2);
        this.tunneling = capabilities2 != null && isTunneling(capabilities2);
        if (!forceSecure && (capabilities2 == null || !isSecure(capabilities2))) {
            z = false;
        }
        this.secure = z;
        this.isVideo = MimeTypes.isVideo(mimeType2);
    }

    public String toString() {
        return this.name;
    }

    public CodecProfileLevel[] getProfileLevels() {
        CodecCapabilities codecCapabilities = this.capabilities;
        return (codecCapabilities == null || codecCapabilities.profileLevels == null) ? new CodecProfileLevel[0] : this.capabilities.profileLevels;
    }

    public int getMaxSupportedInstances() {
        if (Util.SDK_INT >= 23) {
            CodecCapabilities codecCapabilities = this.capabilities;
            if (codecCapabilities != null) {
                return getMaxSupportedInstancesV23(codecCapabilities);
            }
        }
        return -1;
    }

    public boolean isFormatSupported(Format format) throws DecoderQueryException {
        boolean z = false;
        if (!isCodecSupported(format.codecs)) {
            return false;
        }
        if (!this.isVideo) {
            if (Util.SDK_INT < 21 || ((format.sampleRate == -1 || isAudioSampleRateSupportedV21(format.sampleRate)) && (format.channelCount == -1 || isAudioChannelCountSupportedV21(format.channelCount)))) {
                z = true;
            }
            return z;
        } else if (format.width <= 0 || format.height <= 0) {
            return true;
        } else {
            if (Util.SDK_INT >= 21) {
                return isVideoSizeAndRateSupportedV21(format.width, format.height, (double) format.frameRate);
            }
            if (format.width * format.height <= MediaCodecUtil.maxH264DecodableFrameSize()) {
                z = true;
            }
            boolean isFormatSupported = z;
            if (!isFormatSupported) {
                StringBuilder sb = new StringBuilder();
                sb.append("legacyFrameSize, ");
                sb.append(format.width);
                sb.append("x");
                sb.append(format.height);
                logNoSupport(sb.toString());
            }
            return isFormatSupported;
        }
    }

    public boolean isCodecSupported(String codec) {
        CodecProfileLevel[] profileLevels;
        if (codec == null || this.mimeType == null) {
            return true;
        }
        String codecMimeType = MimeTypes.getMediaMimeType(codec);
        if (codecMimeType == null) {
            return true;
        }
        if (!this.mimeType.equals(codecMimeType)) {
            StringBuilder sb = new StringBuilder();
            sb.append("codec.mime ");
            sb.append(codec);
            sb.append(", ");
            sb.append(codecMimeType);
            logNoSupport(sb.toString());
            return false;
        }
        Pair<Integer, Integer> codecProfileAndLevel = MediaCodecUtil.getCodecProfileAndLevel(codec);
        if (codecProfileAndLevel == null) {
            return true;
        }
        for (CodecProfileLevel capabilities2 : getProfileLevels()) {
            if (capabilities2.profile == ((Integer) codecProfileAndLevel.first).intValue() && capabilities2.level >= ((Integer) codecProfileAndLevel.second).intValue()) {
                return true;
            }
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("codec.profileLevel, ");
        sb2.append(codec);
        sb2.append(", ");
        sb2.append(codecMimeType);
        logNoSupport(sb2.toString());
        return false;
    }

    public boolean isSeamlessAdaptationSupported(Format format) {
        if (this.isVideo) {
            return this.adaptive;
        }
        Pair<Integer, Integer> codecProfileLevel = MediaCodecUtil.getCodecProfileAndLevel(format.codecs);
        return codecProfileLevel != null && ((Integer) codecProfileLevel.first).intValue() == 42;
    }

    public boolean isSeamlessAdaptationSupported(Format oldFormat, Format newFormat, boolean isNewFormatComplete) {
        boolean z = true;
        if (this.isVideo) {
            if (!oldFormat.sampleMimeType.equals(newFormat.sampleMimeType) || oldFormat.rotationDegrees != newFormat.rotationDegrees || ((!this.adaptive && !(oldFormat.width == newFormat.width && oldFormat.height == newFormat.height)) || ((isNewFormatComplete || newFormat.colorInfo != null) && !Util.areEqual(oldFormat.colorInfo, newFormat.colorInfo)))) {
                z = false;
            }
            return z;
        } else if (!MimeTypes.AUDIO_AAC.equals(this.mimeType) || !oldFormat.sampleMimeType.equals(newFormat.sampleMimeType) || oldFormat.channelCount != newFormat.channelCount || oldFormat.sampleRate != newFormat.sampleRate) {
            return false;
        } else {
            Pair<Integer, Integer> oldCodecProfileLevel = MediaCodecUtil.getCodecProfileAndLevel(oldFormat.codecs);
            Pair<Integer, Integer> newCodecProfileLevel = MediaCodecUtil.getCodecProfileAndLevel(newFormat.codecs);
            if (oldCodecProfileLevel == null || newCodecProfileLevel == null) {
                return false;
            }
            int oldProfile = ((Integer) oldCodecProfileLevel.first).intValue();
            int newProfile = ((Integer) newCodecProfileLevel.first).intValue();
            if (!(oldProfile == 42 && newProfile == 42)) {
                z = false;
            }
            return z;
        }
    }

    @TargetApi(21)
    public boolean isVideoSizeAndRateSupportedV21(int width, int height, double frameRate) {
        CodecCapabilities codecCapabilities = this.capabilities;
        if (codecCapabilities == null) {
            logNoSupport("sizeAndRate.caps");
            return false;
        }
        VideoCapabilities videoCapabilities = codecCapabilities.getVideoCapabilities();
        if (videoCapabilities == null) {
            logNoSupport("sizeAndRate.vCaps");
            return false;
        }
        if (!areSizeAndRateSupportedV21(videoCapabilities, width, height, frameRate)) {
            if (width >= height || !areSizeAndRateSupportedV21(videoCapabilities, height, width, frameRate)) {
                StringBuilder sb = new StringBuilder();
                sb.append("sizeAndRate.support, ");
                sb.append(width);
                sb.append("x");
                sb.append(height);
                sb.append("x");
                sb.append(frameRate);
                logNoSupport(sb.toString());
                return false;
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("sizeAndRate.rotated, ");
            sb2.append(width);
            sb2.append("x");
            sb2.append(height);
            sb2.append("x");
            sb2.append(frameRate);
            logAssumedSupport(sb2.toString());
        }
        return true;
    }

    @TargetApi(21)
    public Point alignVideoSizeV21(int width, int height) {
        CodecCapabilities codecCapabilities = this.capabilities;
        if (codecCapabilities == null) {
            logNoSupport("align.caps");
            return null;
        }
        VideoCapabilities videoCapabilities = codecCapabilities.getVideoCapabilities();
        if (videoCapabilities == null) {
            logNoSupport("align.vCaps");
            return null;
        }
        int widthAlignment = videoCapabilities.getWidthAlignment();
        int heightAlignment = videoCapabilities.getHeightAlignment();
        return new Point(Util.ceilDivide(width, widthAlignment) * widthAlignment, Util.ceilDivide(height, heightAlignment) * heightAlignment);
    }

    @TargetApi(21)
    public boolean isAudioSampleRateSupportedV21(int sampleRate) {
        CodecCapabilities codecCapabilities = this.capabilities;
        if (codecCapabilities == null) {
            logNoSupport("sampleRate.caps");
            return false;
        }
        AudioCapabilities audioCapabilities = codecCapabilities.getAudioCapabilities();
        if (audioCapabilities == null) {
            logNoSupport("sampleRate.aCaps");
            return false;
        } else if (audioCapabilities.isSampleRateSupported(sampleRate)) {
            return true;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("sampleRate.support, ");
            sb.append(sampleRate);
            logNoSupport(sb.toString());
            return false;
        }
    }

    @TargetApi(21)
    public boolean isAudioChannelCountSupportedV21(int channelCount) {
        CodecCapabilities codecCapabilities = this.capabilities;
        if (codecCapabilities == null) {
            logNoSupport("channelCount.caps");
            return false;
        }
        AudioCapabilities audioCapabilities = codecCapabilities.getAudioCapabilities();
        if (audioCapabilities == null) {
            logNoSupport("channelCount.aCaps");
            return false;
        } else if (adjustMaxInputChannelCount(this.name, this.mimeType, audioCapabilities.getMaxInputChannelCount()) >= channelCount) {
            return true;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("channelCount.support, ");
            sb.append(channelCount);
            logNoSupport(sb.toString());
            return false;
        }
    }

    private void logNoSupport(String message) {
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("NoSupport [");
        sb.append(message);
        sb.append("] [");
        sb.append(this.name);
        sb.append(", ");
        sb.append(this.mimeType);
        sb.append("] [");
        sb.append(Util.DEVICE_DEBUG_INFO);
        sb.append("]");
        Log.d(str, sb.toString());
    }

    private void logAssumedSupport(String message) {
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("AssumedSupport [");
        sb.append(message);
        sb.append("] [");
        sb.append(this.name);
        sb.append(", ");
        sb.append(this.mimeType);
        sb.append("] [");
        sb.append(Util.DEVICE_DEBUG_INFO);
        sb.append("]");
        Log.d(str, sb.toString());
    }

    private static int adjustMaxInputChannelCount(String name2, String mimeType2, int maxChannelCount) {
        int assumedMaxChannelCount;
        if (maxChannelCount > 1 || ((Util.SDK_INT >= 26 && maxChannelCount > 0) || MimeTypes.AUDIO_MPEG.equals(mimeType2) || MimeTypes.AUDIO_AMR_NB.equals(mimeType2) || MimeTypes.AUDIO_AMR_WB.equals(mimeType2) || MimeTypes.AUDIO_AAC.equals(mimeType2) || MimeTypes.AUDIO_VORBIS.equals(mimeType2) || MimeTypes.AUDIO_OPUS.equals(mimeType2) || MimeTypes.AUDIO_RAW.equals(mimeType2) || MimeTypes.AUDIO_FLAC.equals(mimeType2) || MimeTypes.AUDIO_ALAW.equals(mimeType2) || MimeTypes.AUDIO_MLAW.equals(mimeType2) || MimeTypes.AUDIO_MSGSM.equals(mimeType2))) {
            return maxChannelCount;
        }
        if (MimeTypes.AUDIO_AC3.equals(mimeType2)) {
            assumedMaxChannelCount = 6;
        } else if (MimeTypes.AUDIO_E_AC3.equals(mimeType2)) {
            assumedMaxChannelCount = 16;
        } else {
            assumedMaxChannelCount = 30;
        }
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("AssumedMaxChannelAdjustment: ");
        sb.append(name2);
        sb.append(", [");
        sb.append(maxChannelCount);
        sb.append(" to ");
        sb.append(assumedMaxChannelCount);
        sb.append("]");
        Log.w(str, sb.toString());
        return assumedMaxChannelCount;
    }

    private static boolean isAdaptive(CodecCapabilities capabilities2) {
        return Util.SDK_INT >= 19 && isAdaptiveV19(capabilities2);
    }

    @TargetApi(19)
    private static boolean isAdaptiveV19(CodecCapabilities capabilities2) {
        return capabilities2.isFeatureSupported("adaptive-playback");
    }

    private static boolean isTunneling(CodecCapabilities capabilities2) {
        return Util.SDK_INT >= 21 && isTunnelingV21(capabilities2);
    }

    @TargetApi(21)
    private static boolean isTunnelingV21(CodecCapabilities capabilities2) {
        return capabilities2.isFeatureSupported("tunneled-playback");
    }

    private static boolean isSecure(CodecCapabilities capabilities2) {
        return Util.SDK_INT >= 21 && isSecureV21(capabilities2);
    }

    @TargetApi(21)
    private static boolean isSecureV21(CodecCapabilities capabilities2) {
        return capabilities2.isFeatureSupported("secure-playback");
    }

    @TargetApi(21)
    private static boolean areSizeAndRateSupportedV21(VideoCapabilities capabilities2, int width, int height, double frameRate) {
        if (frameRate == -1.0d || frameRate <= 0.0d) {
            return capabilities2.isSizeSupported(width, height);
        }
        return capabilities2.areSizeAndRateSupported(width, height, frameRate);
    }

    @TargetApi(23)
    private static int getMaxSupportedInstancesV23(CodecCapabilities capabilities2) {
        return capabilities2.getMaxSupportedInstances();
    }
}
