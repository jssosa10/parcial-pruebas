package com.google.android.exoplayer2.mediacodec;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaCodecList;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseIntArray;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressLint({"InlinedApi"})
@TargetApi(16)
public final class MediaCodecUtil {
    private static final SparseIntArray AVC_LEVEL_NUMBER_TO_CONST = new SparseIntArray();
    private static final SparseIntArray AVC_PROFILE_NUMBER_TO_CONST = new SparseIntArray();
    private static final String CODEC_ID_AVC1 = "avc1";
    private static final String CODEC_ID_AVC2 = "avc2";
    private static final String CODEC_ID_HEV1 = "hev1";
    private static final String CODEC_ID_HVC1 = "hvc1";
    private static final String CODEC_ID_MP4A = "mp4a";
    private static final Map<String, Integer> HEVC_CODEC_STRING_TO_PROFILE_LEVEL = new HashMap();
    private static final SparseIntArray MP4A_AUDIO_OBJECT_TYPE_TO_PROFILE = new SparseIntArray();
    private static final Pattern PROFILE_PATTERN = Pattern.compile("^\\D?(\\d+)$");
    private static final RawAudioCodecComparator RAW_AUDIO_CODEC_COMPARATOR = new RawAudioCodecComparator();
    private static final String TAG = "MediaCodecUtil";
    private static final HashMap<CodecKey, List<MediaCodecInfo>> decoderInfosCache = new HashMap<>();
    private static int maxH264DecodableFrameSize = -1;

    private static final class CodecKey {
        public final String mimeType;
        public final boolean secure;

        public CodecKey(String mimeType2, boolean secure2) {
            this.mimeType = mimeType2;
            this.secure = secure2;
        }

        public int hashCode() {
            int i = 1 * 31;
            String str = this.mimeType;
            return ((i + (str == null ? 0 : str.hashCode())) * 31) + (this.secure ? 1231 : 1237);
        }

        public boolean equals(@Nullable Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if (obj == null || obj.getClass() != CodecKey.class) {
                return false;
            }
            CodecKey other = (CodecKey) obj;
            if (!TextUtils.equals(this.mimeType, other.mimeType) || this.secure != other.secure) {
                z = false;
            }
            return z;
        }
    }

    public static class DecoderQueryException extends Exception {
        private DecoderQueryException(Throwable cause) {
            super("Failed to query underlying media codecs", cause);
        }
    }

    private interface MediaCodecListCompat {
        int getCodecCount();

        MediaCodecInfo getCodecInfoAt(int i);

        boolean isSecurePlaybackSupported(String str, CodecCapabilities codecCapabilities);

        boolean secureDecodersExplicit();
    }

    private static final class MediaCodecListCompatV16 implements MediaCodecListCompat {
        private MediaCodecListCompatV16() {
        }

        public int getCodecCount() {
            return MediaCodecList.getCodecCount();
        }

        public MediaCodecInfo getCodecInfoAt(int index) {
            return MediaCodecList.getCodecInfoAt(index);
        }

        public boolean secureDecodersExplicit() {
            return false;
        }

        public boolean isSecurePlaybackSupported(String mimeType, CodecCapabilities capabilities) {
            return MimeTypes.VIDEO_H264.equals(mimeType);
        }
    }

    @TargetApi(21)
    private static final class MediaCodecListCompatV21 implements MediaCodecListCompat {
        private final int codecKind;
        private MediaCodecInfo[] mediaCodecInfos;

        public MediaCodecListCompatV21(boolean includeSecure) {
            this.codecKind = includeSecure;
        }

        public int getCodecCount() {
            ensureMediaCodecInfosInitialized();
            return this.mediaCodecInfos.length;
        }

        public MediaCodecInfo getCodecInfoAt(int index) {
            ensureMediaCodecInfosInitialized();
            return this.mediaCodecInfos[index];
        }

        public boolean secureDecodersExplicit() {
            return true;
        }

        public boolean isSecurePlaybackSupported(String mimeType, CodecCapabilities capabilities) {
            return capabilities.isFeatureSupported("secure-playback");
        }

        private void ensureMediaCodecInfosInitialized() {
            if (this.mediaCodecInfos == null) {
                this.mediaCodecInfos = new MediaCodecList(this.codecKind).getCodecInfos();
            }
        }
    }

    private static final class RawAudioCodecComparator implements Comparator<MediaCodecInfo> {
        private RawAudioCodecComparator() {
        }

        public int compare(MediaCodecInfo a, MediaCodecInfo b) {
            return scoreMediaCodecInfo(a) - scoreMediaCodecInfo(b);
        }

        private static int scoreMediaCodecInfo(MediaCodecInfo mediaCodecInfo) {
            String name = mediaCodecInfo.name;
            if (name.startsWith("OMX.google") || name.startsWith("c2.android")) {
                return -1;
            }
            if (Util.SDK_INT >= 26 || !name.equals("OMX.MTK.AUDIO.DECODER.RAW")) {
                return 0;
            }
            return 1;
        }
    }

    static {
        AVC_PROFILE_NUMBER_TO_CONST.put(66, 1);
        AVC_PROFILE_NUMBER_TO_CONST.put(77, 2);
        AVC_PROFILE_NUMBER_TO_CONST.put(88, 4);
        AVC_PROFILE_NUMBER_TO_CONST.put(100, 8);
        AVC_PROFILE_NUMBER_TO_CONST.put(110, 16);
        AVC_PROFILE_NUMBER_TO_CONST.put(122, 32);
        AVC_PROFILE_NUMBER_TO_CONST.put(244, 64);
        AVC_LEVEL_NUMBER_TO_CONST.put(10, 1);
        AVC_LEVEL_NUMBER_TO_CONST.put(11, 4);
        AVC_LEVEL_NUMBER_TO_CONST.put(12, 8);
        AVC_LEVEL_NUMBER_TO_CONST.put(13, 16);
        AVC_LEVEL_NUMBER_TO_CONST.put(20, 32);
        AVC_LEVEL_NUMBER_TO_CONST.put(21, 64);
        AVC_LEVEL_NUMBER_TO_CONST.put(22, 128);
        AVC_LEVEL_NUMBER_TO_CONST.put(30, 256);
        AVC_LEVEL_NUMBER_TO_CONST.put(31, 512);
        AVC_LEVEL_NUMBER_TO_CONST.put(32, 1024);
        AVC_LEVEL_NUMBER_TO_CONST.put(40, 2048);
        AVC_LEVEL_NUMBER_TO_CONST.put(41, 4096);
        AVC_LEVEL_NUMBER_TO_CONST.put(42, 8192);
        AVC_LEVEL_NUMBER_TO_CONST.put(50, 16384);
        AVC_LEVEL_NUMBER_TO_CONST.put(51, 32768);
        AVC_LEVEL_NUMBER_TO_CONST.put(52, 65536);
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("L30", Integer.valueOf(1));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("L60", Integer.valueOf(4));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("L63", Integer.valueOf(16));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("L90", Integer.valueOf(64));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("L93", Integer.valueOf(256));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("L120", Integer.valueOf(1024));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("L123", Integer.valueOf(4096));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("L150", Integer.valueOf(16384));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("L153", Integer.valueOf(65536));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("L156", Integer.valueOf(262144));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("L180", Integer.valueOf(1048576));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("L183", Integer.valueOf(4194304));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("L186", Integer.valueOf(16777216));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("H30", Integer.valueOf(2));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("H60", Integer.valueOf(8));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("H63", Integer.valueOf(32));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("H90", Integer.valueOf(128));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("H93", Integer.valueOf(512));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("H120", Integer.valueOf(2048));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("H123", Integer.valueOf(8192));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("H150", Integer.valueOf(32768));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("H153", Integer.valueOf(131072));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("H156", Integer.valueOf(524288));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("H180", Integer.valueOf(2097152));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("H183", Integer.valueOf(8388608));
        HEVC_CODEC_STRING_TO_PROFILE_LEVEL.put("H186", Integer.valueOf(33554432));
        MP4A_AUDIO_OBJECT_TYPE_TO_PROFILE.put(1, 1);
        MP4A_AUDIO_OBJECT_TYPE_TO_PROFILE.put(2, 2);
        MP4A_AUDIO_OBJECT_TYPE_TO_PROFILE.put(3, 3);
        MP4A_AUDIO_OBJECT_TYPE_TO_PROFILE.put(4, 4);
        MP4A_AUDIO_OBJECT_TYPE_TO_PROFILE.put(5, 5);
        MP4A_AUDIO_OBJECT_TYPE_TO_PROFILE.put(6, 6);
        MP4A_AUDIO_OBJECT_TYPE_TO_PROFILE.put(17, 17);
        MP4A_AUDIO_OBJECT_TYPE_TO_PROFILE.put(20, 20);
        MP4A_AUDIO_OBJECT_TYPE_TO_PROFILE.put(23, 23);
        MP4A_AUDIO_OBJECT_TYPE_TO_PROFILE.put(29, 29);
        MP4A_AUDIO_OBJECT_TYPE_TO_PROFILE.put(39, 39);
        MP4A_AUDIO_OBJECT_TYPE_TO_PROFILE.put(42, 42);
    }

    private MediaCodecUtil() {
    }

    public static void warmDecoderInfoCache(String mimeType, boolean secure) {
        try {
            getDecoderInfos(mimeType, secure);
        } catch (DecoderQueryException e) {
            Log.e(TAG, "Codec warming failed", e);
        }
    }

    @Nullable
    public static MediaCodecInfo getPassthroughDecoderInfo() throws DecoderQueryException {
        MediaCodecInfo decoderInfo = getDecoderInfo(MimeTypes.AUDIO_RAW, false);
        if (decoderInfo == null) {
            return null;
        }
        return MediaCodecInfo.newPassthroughInstance(decoderInfo.name);
    }

    @Nullable
    public static MediaCodecInfo getDecoderInfo(String mimeType, boolean secure) throws DecoderQueryException {
        List<MediaCodecInfo> decoderInfos = getDecoderInfos(mimeType, secure);
        if (decoderInfos.isEmpty()) {
            return null;
        }
        return (MediaCodecInfo) decoderInfos.get(0);
    }

    public static synchronized List<MediaCodecInfo> getDecoderInfos(String mimeType, boolean secure) throws DecoderQueryException {
        synchronized (MediaCodecUtil.class) {
            CodecKey key = new CodecKey(mimeType, secure);
            List<MediaCodecInfo> cachedDecoderInfos = (List) decoderInfosCache.get(key);
            if (cachedDecoderInfos != null) {
                return cachedDecoderInfos;
            }
            MediaCodecListCompat mediaCodecList = Util.SDK_INT >= 21 ? new MediaCodecListCompatV21(secure) : new MediaCodecListCompatV16();
            ArrayList decoderInfosInternal = getDecoderInfosInternal(key, mediaCodecList, mimeType);
            if (secure && decoderInfosInternal.isEmpty() && 21 <= Util.SDK_INT && Util.SDK_INT <= 23) {
                mediaCodecList = new MediaCodecListCompatV16();
                decoderInfosInternal = getDecoderInfosInternal(key, mediaCodecList, mimeType);
                if (!decoderInfosInternal.isEmpty()) {
                    String str = TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("MediaCodecList API didn't list secure decoder for: ");
                    sb.append(mimeType);
                    sb.append(". Assuming: ");
                    sb.append(((MediaCodecInfo) decoderInfosInternal.get(0)).name);
                    Log.w(str, sb.toString());
                }
            }
            if (MimeTypes.AUDIO_E_AC3_JOC.equals(mimeType)) {
                decoderInfosInternal.addAll(getDecoderInfosInternal(new CodecKey(MimeTypes.AUDIO_E_AC3, key.secure), mediaCodecList, mimeType));
            }
            applyWorkarounds(mimeType, decoderInfosInternal);
            List<MediaCodecInfo> unmodifiableDecoderInfos = Collections.unmodifiableList(decoderInfosInternal);
            decoderInfosCache.put(key, unmodifiableDecoderInfos);
            return unmodifiableDecoderInfos;
        }
    }

    public static int maxH264DecodableFrameSize() throws DecoderQueryException {
        if (maxH264DecodableFrameSize == -1) {
            int result = 0;
            MediaCodecInfo decoderInfo = getDecoderInfo(MimeTypes.VIDEO_H264, false);
            if (decoderInfo != null) {
                for (CodecProfileLevel profileLevel : decoderInfo.getProfileLevels()) {
                    result = Math.max(avcLevelToMaxFrameSize(profileLevel.level), result);
                }
                result = Math.max(result, Util.SDK_INT >= 21 ? 345600 : 172800);
            }
            maxH264DecodableFrameSize = result;
        }
        return maxH264DecodableFrameSize;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0030, code lost:
        if (r3.equals(CODEC_ID_HEV1) != false) goto L_0x0048;
     */
    @Nullable
    public static Pair<Integer, Integer> getCodecProfileAndLevel(String codec) {
        if (codec == null) {
            return null;
        }
        String[] parts = codec.split("\\.");
        char c = 0;
        String str = parts[0];
        switch (str.hashCode()) {
            case 3006243:
                if (str.equals(CODEC_ID_AVC1)) {
                    c = 2;
                    break;
                }
            case 3006244:
                if (str.equals(CODEC_ID_AVC2)) {
                    c = 3;
                    break;
                }
            case 3199032:
                break;
            case 3214780:
                if (str.equals(CODEC_ID_HVC1)) {
                    c = 1;
                    break;
                }
            case 3356560:
                if (str.equals(CODEC_ID_MP4A)) {
                    c = 4;
                    break;
                }
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
            case 1:
                return getHevcProfileAndLevel(codec, parts);
            case 2:
            case 3:
                return getAvcProfileAndLevel(codec, parts);
            case 4:
                return getAacCodecProfileAndLevel(codec, parts);
            default:
                return null;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0054, code lost:
        if (r1.secure != r2) goto L_0x005d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0061, code lost:
        if (r1.secure == false) goto L_0x0063;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0063, code lost:
        r16 = r5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:?, code lost:
        r3.add(com.google.android.exoplayer2.mediacodec.MediaCodecInfo.newInstance(r9, r4, r0, r18, false));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0070, code lost:
        r0 = e;
     */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x00a4 A[Catch:{ Exception -> 0x0106 }] */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00c6 A[ADDED_TO_REGION, SYNTHETIC] */
    private static ArrayList<MediaCodecInfo> getDecoderInfosInternal(CodecKey key, MediaCodecListCompat mediaCodecList, String requestedMimeType) throws DecoderQueryException {
        int numberOfCodecs;
        boolean secure;
        CodecKey codecKey = key;
        MediaCodecListCompat mediaCodecListCompat = mediaCodecList;
        try {
            ArrayList arrayList = new ArrayList();
            String mimeType = codecKey.mimeType;
            int numberOfCodecs2 = mediaCodecList.getCodecCount();
            boolean secureDecodersExplicit = mediaCodecList.secureDecodersExplicit();
            int i = 0;
            while (i < numberOfCodecs2) {
                MediaCodecInfo codecInfo = mediaCodecListCompat.getCodecInfoAt(i);
                String codecName = codecInfo.getName();
                try {
                    if (isCodecUsableDecoder(codecInfo, codecName, secureDecodersExplicit, requestedMimeType)) {
                        String[] supportedTypes = codecInfo.getSupportedTypes();
                        int length = supportedTypes.length;
                        int i2 = 0;
                        while (i2 < length) {
                            String supportedType = supportedTypes[i2];
                            if (supportedType.equalsIgnoreCase(mimeType)) {
                                try {
                                    CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(supportedType);
                                    boolean secure2 = mediaCodecListCompat.isSecurePlaybackSupported(mimeType, capabilities);
                                    numberOfCodecs = codecNeedsDisableAdaptationWorkaround(codecName);
                                    int i3 = numberOfCodecs;
                                    if (secureDecodersExplicit) {
                                        try {
                                            secure = secure2;
                                        } catch (Exception e) {
                                            e = e;
                                            numberOfCodecs = numberOfCodecs2;
                                            if (Util.SDK_INT <= 23) {
                                            }
                                            String str = TAG;
                                            StringBuilder sb = new StringBuilder();
                                            sb.append("Failed to query codec ");
                                            sb.append(codecName);
                                            sb.append(" (");
                                            sb.append(supportedType);
                                            sb.append(")");
                                            Log.e(str, sb.toString());
                                            throw e;
                                        }
                                    } else {
                                        secure = secure2;
                                    }
                                    if (!secureDecodersExplicit) {
                                        try {
                                        } catch (Exception e2) {
                                            e = e2;
                                            numberOfCodecs = numberOfCodecs2;
                                            if (Util.SDK_INT <= 23 || arrayList.isEmpty()) {
                                                String str2 = TAG;
                                                StringBuilder sb2 = new StringBuilder();
                                                sb2.append("Failed to query codec ");
                                                sb2.append(codecName);
                                                sb2.append(" (");
                                                sb2.append(supportedType);
                                                sb2.append(")");
                                                Log.e(str2, sb2.toString());
                                                throw e;
                                            }
                                            String str3 = TAG;
                                            StringBuilder sb3 = new StringBuilder();
                                            sb3.append("Skipping codec ");
                                            sb3.append(codecName);
                                            sb3.append(" (failed to query capabilities)");
                                            Log.e(str3, sb3.toString());
                                            i2++;
                                            numberOfCodecs2 = numberOfCodecs;
                                            codecKey = key;
                                            mediaCodecListCompat = mediaCodecList;
                                        }
                                    }
                                    numberOfCodecs = numberOfCodecs2;
                                    boolean forceDisableAdaptive = i3;
                                    if (!secureDecodersExplicit && secure) {
                                        StringBuilder sb4 = new StringBuilder();
                                        sb4.append(codecName);
                                        sb4.append(".secure");
                                        arrayList.add(MediaCodecInfo.newInstance(sb4.toString(), mimeType, capabilities, forceDisableAdaptive, true));
                                        return arrayList;
                                    }
                                } catch (Exception e3) {
                                    e = e3;
                                    numberOfCodecs = numberOfCodecs2;
                                    if (Util.SDK_INT <= 23) {
                                    }
                                    String str22 = TAG;
                                    StringBuilder sb22 = new StringBuilder();
                                    sb22.append("Failed to query codec ");
                                    sb22.append(codecName);
                                    sb22.append(" (");
                                    sb22.append(supportedType);
                                    sb22.append(")");
                                    Log.e(str22, sb22.toString());
                                    throw e;
                                }
                            } else {
                                numberOfCodecs = numberOfCodecs2;
                            }
                            i2++;
                            numberOfCodecs2 = numberOfCodecs;
                            codecKey = key;
                            mediaCodecListCompat = mediaCodecList;
                        }
                        continue;
                    }
                    i++;
                    numberOfCodecs2 = numberOfCodecs2;
                    codecKey = key;
                    mediaCodecListCompat = mediaCodecList;
                } catch (Exception e4) {
                    e = e4;
                    throw new DecoderQueryException(e);
                }
            }
            String str4 = requestedMimeType;
            int i4 = numberOfCodecs2;
            return arrayList;
        } catch (Exception e5) {
            e = e5;
            String str5 = requestedMimeType;
            throw new DecoderQueryException(e);
        }
    }

    private static boolean isCodecUsableDecoder(MediaCodecInfo info2, String name, boolean secureDecodersExplicit, String requestedMimeType) {
        if (info2.isEncoder() || (!secureDecodersExplicit && name.endsWith(".secure"))) {
            return false;
        }
        if (Util.SDK_INT < 21 && ("CIPAACDecoder".equals(name) || "CIPMP3Decoder".equals(name) || "CIPVorbisDecoder".equals(name) || "CIPAMRNBDecoder".equals(name) || "AACDecoder".equals(name) || "MP3Decoder".equals(name))) {
            return false;
        }
        if (Util.SDK_INT < 18 && "OMX.SEC.MP3.Decoder".equals(name)) {
            return false;
        }
        if ("OMX.SEC.mp3.dec".equals(name) && "SM-T530".equals(Util.MODEL)) {
            return false;
        }
        if (Util.SDK_INT < 18 && "OMX.MTK.AUDIO.DECODER.AAC".equals(name) && ("a70".equals(Util.DEVICE) || ("Xiaomi".equals(Util.MANUFACTURER) && Util.DEVICE.startsWith("HM")))) {
            return false;
        }
        if (Util.SDK_INT == 16 && "OMX.qcom.audio.decoder.mp3".equals(name) && ("dlxu".equals(Util.DEVICE) || "protou".equals(Util.DEVICE) || "ville".equals(Util.DEVICE) || "villeplus".equals(Util.DEVICE) || "villec2".equals(Util.DEVICE) || Util.DEVICE.startsWith("gee") || "C6602".equals(Util.DEVICE) || "C6603".equals(Util.DEVICE) || "C6606".equals(Util.DEVICE) || "C6616".equals(Util.DEVICE) || "L36h".equals(Util.DEVICE) || "SO-02E".equals(Util.DEVICE))) {
            return false;
        }
        if (Util.SDK_INT == 16 && "OMX.qcom.audio.decoder.aac".equals(name) && ("C1504".equals(Util.DEVICE) || "C1505".equals(Util.DEVICE) || "C1604".equals(Util.DEVICE) || "C1605".equals(Util.DEVICE))) {
            return false;
        }
        if (Util.SDK_INT < 24 && (("OMX.SEC.aac.dec".equals(name) || "OMX.Exynos.AAC.Decoder".equals(name)) && "samsung".equals(Util.MANUFACTURER) && (Util.DEVICE.startsWith("zeroflte") || Util.DEVICE.startsWith("zerolte") || Util.DEVICE.startsWith("zenlte") || "SC-05G".equals(Util.DEVICE) || "marinelteatt".equals(Util.DEVICE) || "404SC".equals(Util.DEVICE) || "SC-04G".equals(Util.DEVICE) || "SCV31".equals(Util.DEVICE)))) {
            return false;
        }
        if (Util.SDK_INT <= 19 && "OMX.SEC.vp8.dec".equals(name) && "samsung".equals(Util.MANUFACTURER) && (Util.DEVICE.startsWith("d2") || Util.DEVICE.startsWith("serrano") || Util.DEVICE.startsWith("jflte") || Util.DEVICE.startsWith("santos") || Util.DEVICE.startsWith("t0"))) {
            return false;
        }
        if (Util.SDK_INT <= 19 && Util.DEVICE.startsWith("jflte") && "OMX.qcom.video.decoder.vp8".equals(name)) {
            return false;
        }
        if (!MimeTypes.AUDIO_E_AC3_JOC.equals(requestedMimeType) || !"OMX.MTK.AUDIO.DECODER.DSPAC3".equals(name)) {
            return true;
        }
        return false;
    }

    private static void applyWorkarounds(String mimeType, List<MediaCodecInfo> decoderInfos) {
        if (MimeTypes.AUDIO_RAW.equals(mimeType)) {
            Collections.sort(decoderInfos, RAW_AUDIO_CODEC_COMPARATOR);
        }
    }

    private static boolean codecNeedsDisableAdaptationWorkaround(String name) {
        return Util.SDK_INT <= 22 && ("ODROID-XU3".equals(Util.MODEL) || "Nexus 10".equals(Util.MODEL)) && ("OMX.Exynos.AVC.Decoder".equals(name) || "OMX.Exynos.AVC.Decoder.secure".equals(name));
    }

    private static Pair<Integer, Integer> getHevcProfileAndLevel(String codec, String[] parts) {
        int profile;
        if (parts.length < 4) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Ignoring malformed HEVC codec string: ");
            sb.append(codec);
            Log.w(str, sb.toString());
            return null;
        }
        Matcher matcher = PROFILE_PATTERN.matcher(parts[1]);
        if (!matcher.matches()) {
            String str2 = TAG;
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Ignoring malformed HEVC codec string: ");
            sb2.append(codec);
            Log.w(str2, sb2.toString());
            return null;
        }
        String profileString = matcher.group(1);
        if ("1".equals(profileString)) {
            profile = 1;
        } else if ("2".equals(profileString)) {
            profile = 2;
        } else {
            String str3 = TAG;
            StringBuilder sb3 = new StringBuilder();
            sb3.append("Unknown HEVC profile string: ");
            sb3.append(profileString);
            Log.w(str3, sb3.toString());
            return null;
        }
        Integer level = (Integer) HEVC_CODEC_STRING_TO_PROFILE_LEVEL.get(parts[3]);
        if (level != null) {
            return new Pair<>(Integer.valueOf(profile), level);
        }
        String str4 = TAG;
        StringBuilder sb4 = new StringBuilder();
        sb4.append("Unknown HEVC level string: ");
        sb4.append(matcher.group(1));
        Log.w(str4, sb4.toString());
        return null;
    }

    private static Pair<Integer, Integer> getAvcProfileAndLevel(String codec, String[] parts) {
        Integer profileInteger;
        Integer profileInteger2;
        if (parts.length < 2) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Ignoring malformed AVC codec string: ");
            sb.append(codec);
            Log.w(str, sb.toString());
            return null;
        }
        try {
            if (parts[1].length() == 6) {
                Integer valueOf = Integer.valueOf(Integer.parseInt(parts[1].substring(0, 2), 16));
                profileInteger = Integer.valueOf(Integer.parseInt(parts[1].substring(4), 16));
                profileInteger2 = valueOf;
            } else if (parts.length >= 3) {
                profileInteger2 = Integer.valueOf(Integer.parseInt(parts[1]));
                profileInteger = Integer.valueOf(Integer.parseInt(parts[2]));
            } else {
                String str2 = TAG;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Ignoring malformed AVC codec string: ");
                sb2.append(codec);
                Log.w(str2, sb2.toString());
                return null;
            }
            int profile = AVC_PROFILE_NUMBER_TO_CONST.get(profileInteger2.intValue(), -1);
            if (profile == -1) {
                String str3 = TAG;
                StringBuilder sb3 = new StringBuilder();
                sb3.append("Unknown AVC profile: ");
                sb3.append(profileInteger2);
                Log.w(str3, sb3.toString());
                return null;
            }
            int level = AVC_LEVEL_NUMBER_TO_CONST.get(profileInteger.intValue(), -1);
            if (level != -1) {
                return new Pair<>(Integer.valueOf(profile), Integer.valueOf(level));
            }
            String str4 = TAG;
            StringBuilder sb4 = new StringBuilder();
            sb4.append("Unknown AVC level: ");
            sb4.append(profileInteger);
            Log.w(str4, sb4.toString());
            return null;
        } catch (NumberFormatException e) {
            String str5 = TAG;
            StringBuilder sb5 = new StringBuilder();
            sb5.append("Ignoring malformed AVC codec string: ");
            sb5.append(codec);
            Log.w(str5, sb5.toString());
            return null;
        }
    }

    private static int avcLevelToMaxFrameSize(int avcLevel) {
        switch (avcLevel) {
            case 1:
            case 2:
                return 25344;
            case 8:
            case 16:
            case 32:
                return 101376;
            case 64:
                return 202752;
            case 128:
            case 256:
                return 414720;
            case 512:
                return 921600;
            case 1024:
                return 1310720;
            case 2048:
            case 4096:
                return 2097152;
            case 8192:
                return 2228224;
            case 16384:
                return 5652480;
            case 32768:
            case 65536:
                return 9437184;
            default:
                return -1;
        }
    }

    @Nullable
    private static Pair<Integer, Integer> getAacCodecProfileAndLevel(String codec, String[] parts) {
        if (parts.length != 3) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Ignoring malformed MP4A codec string: ");
            sb.append(codec);
            Log.w(str, sb.toString());
            return null;
        }
        try {
            if (MimeTypes.AUDIO_AAC.equals(MimeTypes.getMimeTypeFromMp4ObjectType(Integer.parseInt(parts[1], 16)))) {
                int profile = MP4A_AUDIO_OBJECT_TYPE_TO_PROFILE.get(Integer.parseInt(parts[2]), -1);
                if (profile != -1) {
                    return new Pair<>(Integer.valueOf(profile), Integer.valueOf(0));
                }
            }
        } catch (NumberFormatException e) {
            String str2 = TAG;
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Ignoring malformed MP4A codec string: ");
            sb2.append(codec);
            Log.w(str2, sb2.toString());
        }
        return null;
    }
}
