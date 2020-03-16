package com.google.android.exoplayer2.extractor;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.SeekMap.SeekPoints;
import com.google.android.exoplayer2.util.Util;

public class ConstantBitrateSeekMap implements SeekMap {
    private final int bitrate;
    private final long dataSize;
    private final long durationUs;
    private final long firstFrameBytePosition;
    private final int frameSize;
    private final long inputLength;

    public ConstantBitrateSeekMap(long inputLength2, long firstFrameBytePosition2, int bitrate2, int frameSize2) {
        this.inputLength = inputLength2;
        this.firstFrameBytePosition = firstFrameBytePosition2;
        this.frameSize = frameSize2 == -1 ? 1 : frameSize2;
        this.bitrate = bitrate2;
        if (inputLength2 == -1) {
            this.dataSize = -1;
            this.durationUs = C.TIME_UNSET;
            return;
        }
        this.dataSize = inputLength2 - firstFrameBytePosition2;
        this.durationUs = getTimeUsAtPosition(inputLength2, firstFrameBytePosition2, bitrate2);
    }

    public boolean isSeekable() {
        return this.dataSize != -1;
    }

    public SeekPoints getSeekPoints(long timeUs) {
        if (this.dataSize == -1) {
            return new SeekPoints(new SeekPoint(0, this.firstFrameBytePosition));
        }
        long seekFramePosition = getFramePositionForTimeUs(timeUs);
        long seekTimeUs = getTimeUsAtPosition(seekFramePosition);
        SeekPoint seekPoint = new SeekPoint(seekTimeUs, seekFramePosition);
        if (seekTimeUs < timeUs) {
            int i = this.frameSize;
            if (((long) i) + seekFramePosition < this.inputLength) {
                long secondSeekPosition = ((long) i) + seekFramePosition;
                return new SeekPoints(seekPoint, new SeekPoint(getTimeUsAtPosition(secondSeekPosition), secondSeekPosition));
            }
        }
        return new SeekPoints(seekPoint);
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public long getTimeUsAtPosition(long position) {
        return getTimeUsAtPosition(position, this.firstFrameBytePosition, this.bitrate);
    }

    private static long getTimeUsAtPosition(long position, long firstFrameBytePosition2, int bitrate2) {
        return ((Math.max(0, position - firstFrameBytePosition2) * 8) * 1000000) / ((long) bitrate2);
    }

    private long getFramePositionForTimeUs(long timeUs) {
        long positionOffset = (((long) this.bitrate) * timeUs) / 8000000;
        int i = this.frameSize;
        return this.firstFrameBytePosition + Util.constrainValue((positionOffset / ((long) i)) * ((long) i), 0, this.dataSize - ((long) i));
    }
}
