package com.google.android.exoplayer2;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.ColorInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Format implements Parcelable {
    public static final Creator<Format> CREATOR = new Creator<Format>() {
        public Format createFromParcel(Parcel in) {
            return new Format(in);
        }

        public Format[] newArray(int size) {
            return new Format[size];
        }
    };
    public static final int NO_VALUE = -1;
    public static final long OFFSET_SAMPLE_RELATIVE = Long.MAX_VALUE;
    public final int accessibilityChannel;
    public final int bitrate;
    public final int channelCount;
    @Nullable
    public final String codecs;
    @Nullable
    public final ColorInfo colorInfo;
    @Nullable
    public final String containerMimeType;
    @Nullable
    public final DrmInitData drmInitData;
    public final int encoderDelay;
    public final int encoderPadding;
    public final float frameRate;
    private int hashCode;
    public final int height;
    @Nullable
    public final String id;
    public final List<byte[]> initializationData;
    @Nullable
    public final String label;
    @Nullable
    public final String language;
    public final int maxInputSize;
    @Nullable
    public final Metadata metadata;
    public final int pcmEncoding;
    public final float pixelWidthHeightRatio;
    @Nullable
    public final byte[] projectionData;
    public final int rotationDegrees;
    @Nullable
    public final String sampleMimeType;
    public final int sampleRate;
    public final int selectionFlags;
    public final int stereoMode;
    public final long subsampleOffsetUs;
    public final int width;

    @Deprecated
    public static Format createVideoContainerFormat(@Nullable String id2, @Nullable String containerMimeType2, String sampleMimeType2, String codecs2, int bitrate2, int width2, int height2, float frameRate2, @Nullable List<byte[]> initializationData2, int selectionFlags2) {
        return createVideoContainerFormat(id2, null, containerMimeType2, sampleMimeType2, codecs2, bitrate2, width2, height2, frameRate2, initializationData2, selectionFlags2);
    }

    public static Format createVideoContainerFormat(@Nullable String id2, @Nullable String label2, @Nullable String containerMimeType2, String sampleMimeType2, String codecs2, int bitrate2, int width2, int height2, float frameRate2, @Nullable List<byte[]> initializationData2, int selectionFlags2) {
        Format format = new Format(id2, label2, containerMimeType2, sampleMimeType2, codecs2, bitrate2, -1, width2, height2, frameRate2, -1, -1.0f, null, -1, null, -1, -1, -1, -1, -1, selectionFlags2, null, -1, Long.MAX_VALUE, initializationData2, null, null);
        return format;
    }

    public static Format createVideoSampleFormat(@Nullable String id2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int maxInputSize2, int width2, int height2, float frameRate2, @Nullable List<byte[]> initializationData2, @Nullable DrmInitData drmInitData2) {
        return createVideoSampleFormat(id2, sampleMimeType2, codecs2, bitrate2, maxInputSize2, width2, height2, frameRate2, initializationData2, -1, -1.0f, drmInitData2);
    }

    public static Format createVideoSampleFormat(@Nullable String id2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int maxInputSize2, int width2, int height2, float frameRate2, @Nullable List<byte[]> initializationData2, int rotationDegrees2, float pixelWidthHeightRatio2, @Nullable DrmInitData drmInitData2) {
        return createVideoSampleFormat(id2, sampleMimeType2, codecs2, bitrate2, maxInputSize2, width2, height2, frameRate2, initializationData2, rotationDegrees2, pixelWidthHeightRatio2, null, -1, null, drmInitData2);
    }

    public static Format createVideoSampleFormat(@Nullable String id2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int maxInputSize2, int width2, int height2, float frameRate2, @Nullable List<byte[]> initializationData2, int rotationDegrees2, float pixelWidthHeightRatio2, byte[] projectionData2, int stereoMode2, @Nullable ColorInfo colorInfo2, @Nullable DrmInitData drmInitData2) {
        Format format = new Format(id2, null, null, sampleMimeType2, codecs2, bitrate2, maxInputSize2, width2, height2, frameRate2, rotationDegrees2, pixelWidthHeightRatio2, projectionData2, stereoMode2, colorInfo2, -1, -1, -1, -1, -1, 0, null, -1, Long.MAX_VALUE, initializationData2, drmInitData2, null);
        return format;
    }

    @Deprecated
    public static Format createAudioContainerFormat(@Nullable String id2, @Nullable String containerMimeType2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int channelCount2, int sampleRate2, @Nullable List<byte[]> initializationData2, int selectionFlags2, @Nullable String language2) {
        return createAudioContainerFormat(id2, null, containerMimeType2, sampleMimeType2, codecs2, bitrate2, channelCount2, sampleRate2, initializationData2, selectionFlags2, language2);
    }

    public static Format createAudioContainerFormat(@Nullable String id2, @Nullable String label2, @Nullable String containerMimeType2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int channelCount2, int sampleRate2, @Nullable List<byte[]> initializationData2, int selectionFlags2, @Nullable String language2) {
        Format format = new Format(id2, label2, containerMimeType2, sampleMimeType2, codecs2, bitrate2, -1, -1, -1, -1.0f, -1, -1.0f, null, -1, null, channelCount2, sampleRate2, -1, -1, -1, selectionFlags2, language2, -1, Long.MAX_VALUE, initializationData2, null, null);
        return format;
    }

    public static Format createAudioSampleFormat(@Nullable String id2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int maxInputSize2, int channelCount2, int sampleRate2, @Nullable List<byte[]> initializationData2, @Nullable DrmInitData drmInitData2, int selectionFlags2, @Nullable String language2) {
        return createAudioSampleFormat(id2, sampleMimeType2, codecs2, bitrate2, maxInputSize2, channelCount2, sampleRate2, -1, initializationData2, drmInitData2, selectionFlags2, language2);
    }

    public static Format createAudioSampleFormat(@Nullable String id2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int maxInputSize2, int channelCount2, int sampleRate2, int pcmEncoding2, @Nullable List<byte[]> initializationData2, @Nullable DrmInitData drmInitData2, int selectionFlags2, @Nullable String language2) {
        return createAudioSampleFormat(id2, sampleMimeType2, codecs2, bitrate2, maxInputSize2, channelCount2, sampleRate2, pcmEncoding2, -1, -1, initializationData2, drmInitData2, selectionFlags2, language2, null);
    }

    public static Format createAudioSampleFormat(@Nullable String id2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int maxInputSize2, int channelCount2, int sampleRate2, int pcmEncoding2, int encoderDelay2, int encoderPadding2, @Nullable List<byte[]> initializationData2, @Nullable DrmInitData drmInitData2, int selectionFlags2, @Nullable String language2, @Nullable Metadata metadata2) {
        Format format = new Format(id2, null, null, sampleMimeType2, codecs2, bitrate2, maxInputSize2, -1, -1, -1.0f, -1, -1.0f, null, -1, null, channelCount2, sampleRate2, pcmEncoding2, encoderDelay2, encoderPadding2, selectionFlags2, language2, -1, Long.MAX_VALUE, initializationData2, drmInitData2, metadata2);
        return format;
    }

    @Deprecated
    public static Format createTextContainerFormat(@Nullable String id2, @Nullable String containerMimeType2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int selectionFlags2, @Nullable String language2) {
        return createTextContainerFormat(id2, null, containerMimeType2, sampleMimeType2, codecs2, bitrate2, selectionFlags2, language2);
    }

    public static Format createTextContainerFormat(@Nullable String id2, @Nullable String label2, @Nullable String containerMimeType2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int selectionFlags2, @Nullable String language2) {
        return createTextContainerFormat(id2, label2, containerMimeType2, sampleMimeType2, codecs2, bitrate2, selectionFlags2, language2, -1);
    }

    public static Format createTextContainerFormat(@Nullable String id2, @Nullable String label2, @Nullable String containerMimeType2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int selectionFlags2, @Nullable String language2, int accessibilityChannel2) {
        Format format = new Format(id2, label2, containerMimeType2, sampleMimeType2, codecs2, bitrate2, -1, -1, -1, -1.0f, -1, -1.0f, null, -1, null, -1, -1, -1, -1, -1, selectionFlags2, language2, accessibilityChannel2, Long.MAX_VALUE, null, null, null);
        return format;
    }

    public static Format createTextSampleFormat(@Nullable String id2, String sampleMimeType2, int selectionFlags2, @Nullable String language2) {
        return createTextSampleFormat(id2, sampleMimeType2, selectionFlags2, language2, null);
    }

    public static Format createTextSampleFormat(@Nullable String id2, String sampleMimeType2, int selectionFlags2, @Nullable String language2, @Nullable DrmInitData drmInitData2) {
        return createTextSampleFormat(id2, sampleMimeType2, null, -1, selectionFlags2, language2, -1, drmInitData2, Long.MAX_VALUE, Collections.emptyList());
    }

    public static Format createTextSampleFormat(@Nullable String id2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int selectionFlags2, @Nullable String language2, int accessibilityChannel2, @Nullable DrmInitData drmInitData2) {
        return createTextSampleFormat(id2, sampleMimeType2, codecs2, bitrate2, selectionFlags2, language2, accessibilityChannel2, drmInitData2, Long.MAX_VALUE, Collections.emptyList());
    }

    public static Format createTextSampleFormat(@Nullable String id2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int selectionFlags2, @Nullable String language2, @Nullable DrmInitData drmInitData2, long subsampleOffsetUs2) {
        return createTextSampleFormat(id2, sampleMimeType2, codecs2, bitrate2, selectionFlags2, language2, -1, drmInitData2, subsampleOffsetUs2, Collections.emptyList());
    }

    public static Format createTextSampleFormat(@Nullable String id2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int selectionFlags2, @Nullable String language2, int accessibilityChannel2, @Nullable DrmInitData drmInitData2, long subsampleOffsetUs2, List<byte[]> initializationData2) {
        Format format = new Format(id2, null, null, sampleMimeType2, codecs2, bitrate2, -1, -1, -1, -1.0f, -1, -1.0f, null, -1, null, -1, -1, -1, -1, -1, selectionFlags2, language2, accessibilityChannel2, subsampleOffsetUs2, initializationData2, drmInitData2, null);
        return format;
    }

    public static Format createImageSampleFormat(@Nullable String id2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int selectionFlags2, @Nullable List<byte[]> initializationData2, @Nullable String language2, @Nullable DrmInitData drmInitData2) {
        Format format = new Format(id2, null, null, sampleMimeType2, codecs2, bitrate2, -1, -1, -1, -1.0f, -1, -1.0f, null, -1, null, -1, -1, -1, -1, -1, selectionFlags2, language2, -1, Long.MAX_VALUE, initializationData2, drmInitData2, null);
        return format;
    }

    @Deprecated
    public static Format createContainerFormat(@Nullable String id2, @Nullable String containerMimeType2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int selectionFlags2, @Nullable String language2) {
        return createContainerFormat(id2, null, containerMimeType2, sampleMimeType2, codecs2, bitrate2, selectionFlags2, language2);
    }

    public static Format createContainerFormat(@Nullable String id2, @Nullable String label2, @Nullable String containerMimeType2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int selectionFlags2, @Nullable String language2) {
        Format format = new Format(id2, label2, containerMimeType2, sampleMimeType2, codecs2, bitrate2, -1, -1, -1, -1.0f, -1, -1.0f, null, -1, null, -1, -1, -1, -1, -1, selectionFlags2, language2, -1, Long.MAX_VALUE, null, null, null);
        return format;
    }

    public static Format createSampleFormat(@Nullable String id2, @Nullable String sampleMimeType2, long subsampleOffsetUs2) {
        Format format = new Format(id2, null, null, sampleMimeType2, null, -1, -1, -1, -1, -1.0f, -1, -1.0f, null, -1, null, -1, -1, -1, -1, -1, 0, null, -1, subsampleOffsetUs2, null, null, null);
        return format;
    }

    public static Format createSampleFormat(@Nullable String id2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, @Nullable DrmInitData drmInitData2) {
        Format format = new Format(id2, null, null, sampleMimeType2, codecs2, bitrate2, -1, -1, -1, -1.0f, -1, -1.0f, null, -1, null, -1, -1, -1, -1, -1, 0, null, -1, Long.MAX_VALUE, null, drmInitData2, null);
        return format;
    }

    Format(@Nullable String id2, @Nullable String label2, @Nullable String containerMimeType2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int maxInputSize2, int width2, int height2, float frameRate2, int rotationDegrees2, float pixelWidthHeightRatio2, @Nullable byte[] projectionData2, int stereoMode2, @Nullable ColorInfo colorInfo2, int channelCount2, int sampleRate2, int pcmEncoding2, int encoderDelay2, int encoderPadding2, int selectionFlags2, @Nullable String language2, int accessibilityChannel2, long subsampleOffsetUs2, @Nullable List<byte[]> initializationData2, @Nullable DrmInitData drmInitData2, @Nullable Metadata metadata2) {
        this.id = id2;
        this.label = label2;
        this.containerMimeType = containerMimeType2;
        this.sampleMimeType = sampleMimeType2;
        this.codecs = codecs2;
        this.bitrate = bitrate2;
        this.maxInputSize = maxInputSize2;
        this.width = width2;
        this.height = height2;
        this.frameRate = frameRate2;
        int i = rotationDegrees2;
        this.rotationDegrees = i == -1 ? 0 : i;
        this.pixelWidthHeightRatio = pixelWidthHeightRatio2 == -1.0f ? 1.0f : pixelWidthHeightRatio2;
        this.projectionData = projectionData2;
        this.stereoMode = stereoMode2;
        this.colorInfo = colorInfo2;
        this.channelCount = channelCount2;
        this.sampleRate = sampleRate2;
        this.pcmEncoding = pcmEncoding2;
        int i2 = encoderDelay2;
        this.encoderDelay = i2 == -1 ? 0 : i2;
        int i3 = encoderPadding2;
        this.encoderPadding = i3 == -1 ? 0 : i3;
        this.selectionFlags = selectionFlags2;
        this.language = language2;
        this.accessibilityChannel = accessibilityChannel2;
        this.subsampleOffsetUs = subsampleOffsetUs2;
        this.initializationData = initializationData2 == null ? Collections.emptyList() : initializationData2;
        this.drmInitData = drmInitData2;
        this.metadata = metadata2;
    }

    Format(Parcel in) {
        this.id = in.readString();
        this.label = in.readString();
        this.containerMimeType = in.readString();
        this.sampleMimeType = in.readString();
        this.codecs = in.readString();
        this.bitrate = in.readInt();
        this.maxInputSize = in.readInt();
        this.width = in.readInt();
        this.height = in.readInt();
        this.frameRate = in.readFloat();
        this.rotationDegrees = in.readInt();
        this.pixelWidthHeightRatio = in.readFloat();
        this.projectionData = Util.readBoolean(in) ? in.createByteArray() : null;
        this.stereoMode = in.readInt();
        this.colorInfo = (ColorInfo) in.readParcelable(ColorInfo.class.getClassLoader());
        this.channelCount = in.readInt();
        this.sampleRate = in.readInt();
        this.pcmEncoding = in.readInt();
        this.encoderDelay = in.readInt();
        this.encoderPadding = in.readInt();
        this.selectionFlags = in.readInt();
        this.language = in.readString();
        this.accessibilityChannel = in.readInt();
        this.subsampleOffsetUs = in.readLong();
        int initializationDataSize = in.readInt();
        this.initializationData = new ArrayList(initializationDataSize);
        for (int i = 0; i < initializationDataSize; i++) {
            this.initializationData.add(in.createByteArray());
        }
        this.drmInitData = (DrmInitData) in.readParcelable(DrmInitData.class.getClassLoader());
        this.metadata = (Metadata) in.readParcelable(Metadata.class.getClassLoader());
    }

    public Format copyWithMaxInputSize(int maxInputSize2) {
        int i = maxInputSize2;
        Format format = new Format(this.id, this.label, this.containerMimeType, this.sampleMimeType, this.codecs, this.bitrate, i, this.width, this.height, this.frameRate, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, this.selectionFlags, this.language, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, this.drmInitData, this.metadata);
        return format;
    }

    public Format copyWithSubsampleOffsetUs(long subsampleOffsetUs2) {
        Format format = new Format(this.id, this.label, this.containerMimeType, this.sampleMimeType, this.codecs, this.bitrate, this.maxInputSize, this.width, this.height, this.frameRate, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, this.selectionFlags, this.language, this.accessibilityChannel, subsampleOffsetUs2, this.initializationData, this.drmInitData, this.metadata);
        return format;
    }

    public Format copyWithContainerInfo(@Nullable String id2, @Nullable String label2, @Nullable String sampleMimeType2, @Nullable String codecs2, int bitrate2, int width2, int height2, int selectionFlags2, @Nullable String language2) {
        String str = id2;
        Format format = new Format(str, label2, this.containerMimeType, sampleMimeType2, codecs2, bitrate2, this.maxInputSize, width2, height2, this.frameRate, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, selectionFlags2, language2, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, this.drmInitData, this.metadata);
        return format;
    }

    public Format copyWithManifestFormatInfo(Format manifestFormat) {
        String language2;
        String codecs2;
        float frameRate2;
        float frameRate3;
        Format format = manifestFormat;
        if (this == format) {
            return this;
        }
        int trackType = MimeTypes.getTrackType(this.sampleMimeType);
        String id2 = format.id;
        String str = format.label;
        if (str == null) {
            str = this.label;
        }
        String label2 = str;
        String language3 = this.language;
        if ((trackType == 3 || trackType == 1) && format.language != null) {
            language2 = format.language;
        } else {
            language2 = language3;
        }
        int i = this.bitrate;
        if (i == -1) {
            i = format.bitrate;
        }
        int bitrate2 = i;
        String codecs3 = this.codecs;
        if (codecs3 == null) {
            String codecsOfType = Util.getCodecsOfType(format.codecs, trackType);
            if (Util.splitCodecs(codecsOfType).length == 1) {
                codecs2 = codecsOfType;
                frameRate2 = this.frameRate;
                if (frameRate2 == -1.0f || trackType != 2) {
                    frameRate3 = frameRate2;
                } else {
                    frameRate3 = format.frameRate;
                }
                int i2 = this.selectionFlags | format.selectionFlags;
                DrmInitData createSessionCreationData = DrmInitData.createSessionCreationData(format.drmInitData, this.drmInitData);
                int i3 = trackType;
                String str2 = id2;
                Format format2 = new Format(id2, label2, this.containerMimeType, this.sampleMimeType, codecs2, bitrate2, this.maxInputSize, this.width, this.height, frameRate3, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, i2, language2, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, createSessionCreationData, this.metadata);
                return format2;
            }
        }
        codecs2 = codecs3;
        frameRate2 = this.frameRate;
        if (frameRate2 == -1.0f) {
        }
        frameRate3 = frameRate2;
        int i22 = this.selectionFlags | format.selectionFlags;
        DrmInitData createSessionCreationData2 = DrmInitData.createSessionCreationData(format.drmInitData, this.drmInitData);
        int i32 = trackType;
        String str22 = id2;
        Format format22 = new Format(id2, label2, this.containerMimeType, this.sampleMimeType, codecs2, bitrate2, this.maxInputSize, this.width, this.height, frameRate3, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, i22, language2, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, createSessionCreationData2, this.metadata);
        return format22;
    }

    public Format copyWithGaplessInfo(int encoderDelay2, int encoderPadding2) {
        int i = encoderDelay2;
        int i2 = encoderPadding2;
        Format format = new Format(this.id, this.label, this.containerMimeType, this.sampleMimeType, this.codecs, this.bitrate, this.maxInputSize, this.width, this.height, this.frameRate, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, i, i2, this.selectionFlags, this.language, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, this.drmInitData, this.metadata);
        return format;
    }

    public Format copyWithDrmInitData(@Nullable DrmInitData drmInitData2) {
        DrmInitData drmInitData3 = drmInitData2;
        Format format = new Format(this.id, this.label, this.containerMimeType, this.sampleMimeType, this.codecs, this.bitrate, this.maxInputSize, this.width, this.height, this.frameRate, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, this.selectionFlags, this.language, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, drmInitData3, this.metadata);
        return format;
    }

    public Format copyWithMetadata(@Nullable Metadata metadata2) {
        Metadata metadata3 = metadata2;
        Format format = new Format(this.id, this.label, this.containerMimeType, this.sampleMimeType, this.codecs, this.bitrate, this.maxInputSize, this.width, this.height, this.frameRate, this.rotationDegrees, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, this.selectionFlags, this.language, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, this.drmInitData, metadata3);
        return format;
    }

    public Format copyWithRotationDegrees(int rotationDegrees2) {
        int i = rotationDegrees2;
        Format format = new Format(this.id, this.label, this.containerMimeType, this.sampleMimeType, this.codecs, this.bitrate, this.maxInputSize, this.width, this.height, this.frameRate, i, this.pixelWidthHeightRatio, this.projectionData, this.stereoMode, this.colorInfo, this.channelCount, this.sampleRate, this.pcmEncoding, this.encoderDelay, this.encoderPadding, this.selectionFlags, this.language, this.accessibilityChannel, this.subsampleOffsetUs, this.initializationData, this.drmInitData, this.metadata);
        return format;
    }

    public int getPixelCount() {
        int i = this.width;
        if (i == -1) {
            return -1;
        }
        int i2 = this.height;
        if (i2 == -1) {
            return -1;
        }
        return i * i2;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Format(");
        sb.append(this.id);
        sb.append(", ");
        sb.append(this.label);
        sb.append(", ");
        sb.append(this.containerMimeType);
        sb.append(", ");
        sb.append(this.sampleMimeType);
        sb.append(", ");
        sb.append(this.codecs);
        sb.append(", ");
        sb.append(this.bitrate);
        sb.append(", ");
        sb.append(this.language);
        sb.append(", [");
        sb.append(this.width);
        sb.append(", ");
        sb.append(this.height);
        sb.append(", ");
        sb.append(this.frameRate);
        sb.append("], [");
        sb.append(this.channelCount);
        sb.append(", ");
        sb.append(this.sampleRate);
        sb.append("])");
        return sb.toString();
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            int i = 17 * 31;
            String str = this.id;
            int i2 = 0;
            int result = (i + (str == null ? 0 : str.hashCode())) * 31;
            String str2 = this.containerMimeType;
            int result2 = (result + (str2 == null ? 0 : str2.hashCode())) * 31;
            String str3 = this.sampleMimeType;
            int result3 = (result2 + (str3 == null ? 0 : str3.hashCode())) * 31;
            String str4 = this.codecs;
            int result4 = (((((((((((result3 + (str4 == null ? 0 : str4.hashCode())) * 31) + this.bitrate) * 31) + this.width) * 31) + this.height) * 31) + this.channelCount) * 31) + this.sampleRate) * 31;
            String str5 = this.language;
            int result5 = (((result4 + (str5 == null ? 0 : str5.hashCode())) * 31) + this.accessibilityChannel) * 31;
            DrmInitData drmInitData2 = this.drmInitData;
            int result6 = (result5 + (drmInitData2 == null ? 0 : drmInitData2.hashCode())) * 31;
            Metadata metadata2 = this.metadata;
            int result7 = (result6 + (metadata2 == null ? 0 : metadata2.hashCode())) * 31;
            String str6 = this.label;
            if (str6 != null) {
                i2 = str6.hashCode();
            }
            this.hashCode = ((((((((((((((((((((result7 + i2) * 31) + this.maxInputSize) * 31) + ((int) this.subsampleOffsetUs)) * 31) + Float.floatToIntBits(this.frameRate)) * 31) + Float.floatToIntBits(this.pixelWidthHeightRatio)) * 31) + this.rotationDegrees) * 31) + this.stereoMode) * 31) + this.pcmEncoding) * 31) + this.encoderDelay) * 31) + this.encoderPadding) * 31) + this.selectionFlags;
        }
        return this.hashCode;
    }

    public boolean equals(@Nullable Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Format other = (Format) obj;
        int i = this.hashCode;
        if (i != 0) {
            int i2 = other.hashCode;
            if (!(i2 == 0 || i == i2)) {
                return false;
            }
        }
        if (!(this.bitrate == other.bitrate && this.maxInputSize == other.maxInputSize && this.width == other.width && this.height == other.height && Float.compare(this.frameRate, other.frameRate) == 0 && this.rotationDegrees == other.rotationDegrees && Float.compare(this.pixelWidthHeightRatio, other.pixelWidthHeightRatio) == 0 && this.stereoMode == other.stereoMode && this.channelCount == other.channelCount && this.sampleRate == other.sampleRate && this.pcmEncoding == other.pcmEncoding && this.encoderDelay == other.encoderDelay && this.encoderPadding == other.encoderPadding && this.subsampleOffsetUs == other.subsampleOffsetUs && this.selectionFlags == other.selectionFlags && Util.areEqual(this.id, other.id) && Util.areEqual(this.label, other.label) && Util.areEqual(this.language, other.language) && this.accessibilityChannel == other.accessibilityChannel && Util.areEqual(this.containerMimeType, other.containerMimeType) && Util.areEqual(this.sampleMimeType, other.sampleMimeType) && Util.areEqual(this.codecs, other.codecs) && Util.areEqual(this.drmInitData, other.drmInitData) && Util.areEqual(this.metadata, other.metadata) && Util.areEqual(this.colorInfo, other.colorInfo) && Arrays.equals(this.projectionData, other.projectionData) && initializationDataEquals(other))) {
            z = false;
        }
        return z;
    }

    public boolean initializationDataEquals(Format other) {
        if (this.initializationData.size() != other.initializationData.size()) {
            return false;
        }
        for (int i = 0; i < this.initializationData.size(); i++) {
            if (!Arrays.equals((byte[]) this.initializationData.get(i), (byte[]) other.initializationData.get(i))) {
                return false;
            }
        }
        return true;
    }

    public static String toLogString(@Nullable Format format) {
        if (format == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("id=");
        builder.append(format.id);
        builder.append(", mimeType=");
        builder.append(format.sampleMimeType);
        if (format.bitrate != -1) {
            builder.append(", bitrate=");
            builder.append(format.bitrate);
        }
        if (format.codecs != null) {
            builder.append(", codecs=");
            builder.append(format.codecs);
        }
        if (!(format.width == -1 || format.height == -1)) {
            builder.append(", res=");
            builder.append(format.width);
            builder.append("x");
            builder.append(format.height);
        }
        if (format.frameRate != -1.0f) {
            builder.append(", fps=");
            builder.append(format.frameRate);
        }
        if (format.channelCount != -1) {
            builder.append(", channels=");
            builder.append(format.channelCount);
        }
        if (format.sampleRate != -1) {
            builder.append(", sample_rate=");
            builder.append(format.sampleRate);
        }
        if (format.language != null) {
            builder.append(", language=");
            builder.append(format.language);
        }
        if (format.label != null) {
            builder.append(", label=");
            builder.append(format.label);
        }
        return builder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.label);
        dest.writeString(this.containerMimeType);
        dest.writeString(this.sampleMimeType);
        dest.writeString(this.codecs);
        dest.writeInt(this.bitrate);
        dest.writeInt(this.maxInputSize);
        dest.writeInt(this.width);
        dest.writeInt(this.height);
        dest.writeFloat(this.frameRate);
        dest.writeInt(this.rotationDegrees);
        dest.writeFloat(this.pixelWidthHeightRatio);
        Util.writeBoolean(dest, this.projectionData != null);
        byte[] bArr = this.projectionData;
        if (bArr != null) {
            dest.writeByteArray(bArr);
        }
        dest.writeInt(this.stereoMode);
        dest.writeParcelable(this.colorInfo, flags);
        dest.writeInt(this.channelCount);
        dest.writeInt(this.sampleRate);
        dest.writeInt(this.pcmEncoding);
        dest.writeInt(this.encoderDelay);
        dest.writeInt(this.encoderPadding);
        dest.writeInt(this.selectionFlags);
        dest.writeString(this.language);
        dest.writeInt(this.accessibilityChannel);
        dest.writeLong(this.subsampleOffsetUs);
        int initializationDataSize = this.initializationData.size();
        dest.writeInt(initializationDataSize);
        for (int i = 0; i < initializationDataSize; i++) {
            dest.writeByteArray((byte[]) this.initializationData.get(i));
        }
        dest.writeParcelable(this.drmInitData, 0);
        dest.writeParcelable(this.metadata, 0);
    }
}
