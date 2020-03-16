package com.google.android.exoplayer2.extractor;

import com.google.android.exoplayer2.extractor.SeekMap.SeekPoints;
import com.google.android.exoplayer2.util.Util;
import java.util.Arrays;

public final class ChunkIndex implements SeekMap {
    private final long durationUs;
    public final long[] durationsUs;
    public final int length;
    public final long[] offsets;
    public final int[] sizes;
    public final long[] timesUs;

    public ChunkIndex(int[] sizes2, long[] offsets2, long[] durationsUs2, long[] timesUs2) {
        this.sizes = sizes2;
        this.offsets = offsets2;
        this.durationsUs = durationsUs2;
        this.timesUs = timesUs2;
        this.length = sizes2.length;
        int i = this.length;
        if (i > 0) {
            this.durationUs = durationsUs2[i - 1] + timesUs2[i - 1];
        } else {
            this.durationUs = 0;
        }
    }

    public int getChunkIndex(long timeUs) {
        return Util.binarySearchFloor(this.timesUs, timeUs, true, true);
    }

    public boolean isSeekable() {
        return true;
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public SeekPoints getSeekPoints(long timeUs) {
        int chunkIndex = getChunkIndex(timeUs);
        SeekPoint seekPoint = new SeekPoint(this.timesUs[chunkIndex], this.offsets[chunkIndex]);
        if (seekPoint.timeUs >= timeUs || chunkIndex == this.length - 1) {
            return new SeekPoints(seekPoint);
        }
        return new SeekPoints(seekPoint, new SeekPoint(this.timesUs[chunkIndex + 1], this.offsets[chunkIndex + 1]));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ChunkIndex(length=");
        sb.append(this.length);
        sb.append(", sizes=");
        sb.append(Arrays.toString(this.sizes));
        sb.append(", offsets=");
        sb.append(Arrays.toString(this.offsets));
        sb.append(", timeUs=");
        sb.append(Arrays.toString(this.timesUs));
        sb.append(", durationsUs=");
        sb.append(Arrays.toString(this.durationsUs));
        sb.append(")");
        return sb.toString();
    }
}
