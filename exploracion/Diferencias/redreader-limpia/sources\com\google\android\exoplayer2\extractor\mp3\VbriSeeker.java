package com.google.android.exoplayer2.extractor.mp3;

import android.support.annotation.Nullable;
import com.google.android.exoplayer2.extractor.MpegAudioHeader;
import com.google.android.exoplayer2.extractor.SeekMap.SeekPoints;
import com.google.android.exoplayer2.extractor.SeekPoint;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;

final class VbriSeeker implements Seeker {
    private static final String TAG = "VbriSeeker";
    private final long dataEndPosition;
    private final long durationUs;
    private final long[] positions;
    private final long[] timesUs;

    @Nullable
    public static VbriSeeker create(long inputLength, long position, MpegAudioHeader mpegAudioHeader, ParsableByteArray frame) {
        int segmentSize;
        long j = inputLength;
        MpegAudioHeader mpegAudioHeader2 = mpegAudioHeader;
        ParsableByteArray parsableByteArray = frame;
        parsableByteArray.skipBytes(10);
        int numFrames = frame.readInt();
        if (numFrames <= 0) {
            return null;
        }
        int sampleRate = mpegAudioHeader2.sampleRate;
        long durationUs2 = Util.scaleLargeTimestamp((long) numFrames, 1000000 * ((long) (sampleRate >= 32000 ? 1152 : 576)), (long) sampleRate);
        int entryCount = frame.readUnsignedShort();
        int scale = frame.readUnsignedShort();
        int entrySize = frame.readUnsignedShort();
        parsableByteArray.skipBytes(2);
        long minPosition = position + ((long) mpegAudioHeader2.frameSize);
        long[] timesUs2 = new long[entryCount];
        long[] positions2 = new long[entryCount];
        long position2 = position;
        int index = 0;
        while (index < entryCount) {
            int sampleRate2 = sampleRate;
            long durationUs3 = durationUs2;
            timesUs2[index] = (((long) index) * durationUs2) / ((long) entryCount);
            positions2[index] = Math.max(position2, minPosition);
            switch (entrySize) {
                case 1:
                    segmentSize = frame.readUnsignedByte();
                    break;
                case 2:
                    segmentSize = frame.readUnsignedShort();
                    break;
                case 3:
                    segmentSize = frame.readUnsignedInt24();
                    break;
                case 4:
                    segmentSize = frame.readUnsignedIntToInt();
                    break;
                default:
                    return null;
            }
            position2 += (long) (segmentSize * scale);
            index++;
            sampleRate = sampleRate2;
            durationUs2 = durationUs3;
            long j2 = inputLength;
        }
        long durationUs4 = durationUs2;
        long j3 = inputLength;
        if (!(j3 == -1 || j3 == position2)) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("VBRI data size mismatch: ");
            sb.append(j3);
            sb.append(", ");
            sb.append(position2);
            Log.w(str, sb.toString());
        }
        long j4 = minPosition;
        VbriSeeker vbriSeeker = new VbriSeeker(timesUs2, positions2, durationUs4, position2);
        return vbriSeeker;
    }

    private VbriSeeker(long[] timesUs2, long[] positions2, long durationUs2, long dataEndPosition2) {
        this.timesUs = timesUs2;
        this.positions = positions2;
        this.durationUs = durationUs2;
        this.dataEndPosition = dataEndPosition2;
    }

    public boolean isSeekable() {
        return true;
    }

    public SeekPoints getSeekPoints(long timeUs) {
        int tableIndex = Util.binarySearchFloor(this.timesUs, timeUs, true, true);
        SeekPoint seekPoint = new SeekPoint(this.timesUs[tableIndex], this.positions[tableIndex]);
        if (seekPoint.timeUs < timeUs) {
            long[] jArr = this.timesUs;
            if (tableIndex != jArr.length - 1) {
                return new SeekPoints(seekPoint, new SeekPoint(jArr[tableIndex + 1], this.positions[tableIndex + 1]));
            }
        }
        return new SeekPoints(seekPoint);
    }

    public long getTimeUs(long position) {
        return this.timesUs[Util.binarySearchFloor(this.positions, position, true, true)];
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public long getDataEndPosition() {
        return this.dataEndPosition;
    }
}
