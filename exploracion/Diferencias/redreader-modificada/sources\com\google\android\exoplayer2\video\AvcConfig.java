package com.google.android.exoplayer2.video;

import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.util.CodecSpecificDataUtil;
import com.google.android.exoplayer2.util.NalUnitUtil;
import com.google.android.exoplayer2.util.NalUnitUtil.SpsData;
import com.google.android.exoplayer2.util.ParsableByteArray;
import java.util.ArrayList;
import java.util.List;

public final class AvcConfig {
    public final int height;
    public final List<byte[]> initializationData;
    public final int nalUnitLengthFieldLength;
    public final float pixelWidthAspectRatio;
    public final int width;

    public static AvcConfig parse(ParsableByteArray data) throws ParserException {
        float pixelWidthAspectRatio2;
        int height2;
        int width2;
        try {
            data.skipBytes(4);
            int nalUnitLengthFieldLength2 = (data.readUnsignedByte() & 3) + 1;
            if (nalUnitLengthFieldLength2 != 3) {
                ArrayList arrayList = new ArrayList();
                int numSequenceParameterSets = data.readUnsignedByte() & 31;
                for (int j = 0; j < numSequenceParameterSets; j++) {
                    arrayList.add(buildNalUnitForChild(data));
                }
                int numPictureParameterSets = data.readUnsignedByte();
                for (int j2 = 0; j2 < numPictureParameterSets; j2++) {
                    arrayList.add(buildNalUnitForChild(data));
                }
                if (numSequenceParameterSets > 0) {
                    SpsData spsData = NalUnitUtil.parseSpsNalUnit((byte[]) arrayList.get(0), nalUnitLengthFieldLength2, ((byte[]) arrayList.get(0)).length);
                    int width3 = spsData.width;
                    width2 = width3;
                    height2 = spsData.height;
                    pixelWidthAspectRatio2 = spsData.pixelWidthAspectRatio;
                } else {
                    width2 = -1;
                    height2 = -1;
                    pixelWidthAspectRatio2 = 1.0f;
                }
                AvcConfig avcConfig = new AvcConfig(arrayList, nalUnitLengthFieldLength2, width2, height2, pixelWidthAspectRatio2);
                return avcConfig;
            }
            throw new IllegalStateException();
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ParserException("Error parsing AVC config", e);
        }
    }

    private AvcConfig(List<byte[]> initializationData2, int nalUnitLengthFieldLength2, int width2, int height2, float pixelWidthAspectRatio2) {
        this.initializationData = initializationData2;
        this.nalUnitLengthFieldLength = nalUnitLengthFieldLength2;
        this.width = width2;
        this.height = height2;
        this.pixelWidthAspectRatio = pixelWidthAspectRatio2;
    }

    private static byte[] buildNalUnitForChild(ParsableByteArray data) {
        int length = data.readUnsignedShort();
        int offset = data.getPosition();
        data.skipBytes(length);
        return CodecSpecificDataUtil.buildNalUnit(data.data, offset, length);
    }
}
