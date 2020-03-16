package com.google.android.exoplayer2.extractor.mp3;

import android.support.annotation.Nullable;
import com.google.android.exoplayer2.extractor.MpegAudioHeader;
import com.google.android.exoplayer2.extractor.SeekMap.SeekPoints;
import com.google.android.exoplayer2.extractor.SeekPoint;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;

final class XingSeeker implements Seeker {
    private static final String TAG = "XingSeeker";
    private final long dataEndPosition;
    private final long dataSize;
    private final long dataStartPosition;
    private final long durationUs;
    @Nullable
    private final long[] tableOfContents;
    private final int xingFrameSize;

    @Nullable
    public static XingSeeker create(long inputLength, long position, MpegAudioHeader mpegAudioHeader, ParsableByteArray frame) {
        long j = inputLength;
        MpegAudioHeader mpegAudioHeader2 = mpegAudioHeader;
        int samplesPerFrame = mpegAudioHeader2.samplesPerFrame;
        int sampleRate = mpegAudioHeader2.sampleRate;
        int flags = frame.readInt();
        if ((flags & 1) == 1) {
            int readUnsignedIntToInt = frame.readUnsignedIntToInt();
            int frameCount = readUnsignedIntToInt;
            if (readUnsignedIntToInt != 0) {
                long durationUs2 = Util.scaleLargeTimestamp((long) frameCount, ((long) samplesPerFrame) * 1000000, (long) sampleRate);
                if ((flags & 6) != 6) {
                    XingSeeker xingSeeker = new XingSeeker(position, mpegAudioHeader2.frameSize, durationUs2);
                    return xingSeeker;
                }
                long dataSize2 = (long) frame.readUnsignedIntToInt();
                long[] tableOfContents2 = new long[100];
                for (int i = 0; i < 100; i++) {
                    tableOfContents2[i] = (long) frame.readUnsignedByte();
                }
                if (!(j == -1 || j == position + dataSize2)) {
                    String str = TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("XING data size mismatch: ");
                    sb.append(j);
                    sb.append(", ");
                    sb.append(position + dataSize2);
                    Log.w(str, sb.toString());
                }
                long j2 = dataSize2;
                long[] jArr = tableOfContents2;
                XingSeeker xingSeeker2 = new XingSeeker(position, mpegAudioHeader2.frameSize, durationUs2, dataSize2, tableOfContents2);
                return xingSeeker2;
            }
        }
        return null;
    }

    private XingSeeker(long dataStartPosition2, int xingFrameSize2, long durationUs2) {
        this(dataStartPosition2, xingFrameSize2, durationUs2, -1, null);
    }

    private XingSeeker(long dataStartPosition2, int xingFrameSize2, long durationUs2, long dataSize2, @Nullable long[] tableOfContents2) {
        this.dataStartPosition = dataStartPosition2;
        this.xingFrameSize = xingFrameSize2;
        this.durationUs = durationUs2;
        this.tableOfContents = tableOfContents2;
        this.dataSize = dataSize2;
        long j = -1;
        if (dataSize2 != -1) {
            j = dataStartPosition2 + dataSize2;
        }
        this.dataEndPosition = j;
    }

    public boolean isSeekable() {
        return this.tableOfContents != null;
    }

    public SeekPoints getSeekPoints(long timeUs) {
        double scaledPosition;
        if (!isSeekable()) {
            return new SeekPoints(new SeekPoint(0, this.dataStartPosition + ((long) this.xingFrameSize)));
        }
        long timeUs2 = Util.constrainValue(timeUs, 0, this.durationUs);
        double d = (double) timeUs2;
        Double.isNaN(d);
        double d2 = d * 100.0d;
        double d3 = (double) this.durationUs;
        Double.isNaN(d3);
        double percent = d2 / d3;
        if (percent <= 0.0d) {
            scaledPosition = 0.0d;
        } else if (percent >= 100.0d) {
            scaledPosition = 256.0d;
        } else {
            int prevTableIndex = (int) percent;
            long[] tableOfContents2 = (long[]) Assertions.checkNotNull(this.tableOfContents);
            double prevScaledPosition = (double) tableOfContents2[prevTableIndex];
            double nextScaledPosition = prevTableIndex == 99 ? 256.0d : (double) tableOfContents2[prevTableIndex + 1];
            double d4 = (double) prevTableIndex;
            Double.isNaN(d4);
            double interpolateFraction = percent - d4;
            Double.isNaN(prevScaledPosition);
            double d5 = (nextScaledPosition - prevScaledPosition) * interpolateFraction;
            Double.isNaN(prevScaledPosition);
            scaledPosition = prevScaledPosition + d5;
        }
        double prevScaledPosition2 = scaledPosition / 256.0d;
        double d6 = (double) this.dataSize;
        Double.isNaN(d6);
        return new SeekPoints(new SeekPoint(timeUs2, this.dataStartPosition + Util.constrainValue(Math.round(prevScaledPosition2 * d6), (long) this.xingFrameSize, this.dataSize - 1)));
    }

    public long getTimeUs(long position) {
        double interpolateFraction;
        long positionOffset = position - this.dataStartPosition;
        if (!isSeekable()) {
        } else if (positionOffset <= ((long) this.xingFrameSize)) {
            long j = positionOffset;
        } else {
            long[] tableOfContents2 = (long[]) Assertions.checkNotNull(this.tableOfContents);
            double d = (double) positionOffset;
            Double.isNaN(d);
            double d2 = d * 256.0d;
            double d3 = (double) this.dataSize;
            Double.isNaN(d3);
            double scaledPosition = d2 / d3;
            int prevTableIndex = Util.binarySearchFloor(tableOfContents2, (long) scaledPosition, true, true);
            long prevTimeUs = getTimeUsForTableIndex(prevTableIndex);
            long prevScaledPosition = tableOfContents2[prevTableIndex];
            long nextTimeUs = getTimeUsForTableIndex(prevTableIndex + 1);
            long nextScaledPosition = prevTableIndex == 99 ? 256 : tableOfContents2[prevTableIndex + 1];
            if (prevScaledPosition == nextScaledPosition) {
                long[] jArr = tableOfContents2;
                interpolateFraction = 0.0d;
                long j2 = positionOffset;
            } else {
                long j3 = positionOffset;
                double d4 = (double) prevScaledPosition;
                Double.isNaN(d4);
                double d5 = scaledPosition - d4;
                long[] jArr2 = tableOfContents2;
                double d6 = (double) (nextScaledPosition - prevScaledPosition);
                Double.isNaN(d6);
                interpolateFraction = d5 / d6;
            }
            double d7 = (double) (nextTimeUs - prevTimeUs);
            Double.isNaN(d7);
            return Math.round(d7 * interpolateFraction) + prevTimeUs;
        }
        return 0;
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public long getDataEndPosition() {
        return this.dataEndPosition;
    }

    private long getTimeUsForTableIndex(int tableIndex) {
        return (this.durationUs * ((long) tableIndex)) / 100;
    }
}
