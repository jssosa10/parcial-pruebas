package com.google.android.exoplayer2.extractor.wav;

import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.extractor.SeekMap.SeekPoints;
import com.google.android.exoplayer2.extractor.SeekPoint;
import com.google.android.exoplayer2.util.Util;

final class WavHeader implements SeekMap {
    private final int averageBytesPerSecond;
    private final int bitsPerSample;
    private final int blockAlignment;
    private long dataSize;
    private long dataStartPosition;
    private final int encoding;
    private final int numChannels;
    private final int sampleRateHz;

    public WavHeader(int numChannels2, int sampleRateHz2, int averageBytesPerSecond2, int blockAlignment2, int bitsPerSample2, int encoding2) {
        this.numChannels = numChannels2;
        this.sampleRateHz = sampleRateHz2;
        this.averageBytesPerSecond = averageBytesPerSecond2;
        this.blockAlignment = blockAlignment2;
        this.bitsPerSample = bitsPerSample2;
        this.encoding = encoding2;
    }

    public void setDataBounds(long dataStartPosition2, long dataSize2) {
        this.dataStartPosition = dataStartPosition2;
        this.dataSize = dataSize2;
    }

    public long getDataLimit() {
        if (hasDataBounds()) {
            return this.dataStartPosition + this.dataSize;
        }
        return -1;
    }

    public boolean hasDataBounds() {
        return (this.dataStartPosition == 0 || this.dataSize == 0) ? false : true;
    }

    public boolean isSeekable() {
        return true;
    }

    public long getDurationUs() {
        return (1000000 * (this.dataSize / ((long) this.blockAlignment))) / ((long) this.sampleRateHz);
    }

    public SeekPoints getSeekPoints(long timeUs) {
        long positionOffset = (((long) this.averageBytesPerSecond) * timeUs) / 1000000;
        int i = this.blockAlignment;
        long positionOffset2 = Util.constrainValue((positionOffset / ((long) i)) * ((long) i), 0, this.dataSize - ((long) i));
        long seekPosition = this.dataStartPosition + positionOffset2;
        long seekTimeUs = getTimeUs(seekPosition);
        SeekPoint seekPoint = new SeekPoint(seekTimeUs, seekPosition);
        if (seekTimeUs < timeUs) {
            long j = this.dataSize;
            int i2 = this.blockAlignment;
            if (positionOffset2 != j - ((long) i2)) {
                long secondSeekPosition = ((long) i2) + seekPosition;
                return new SeekPoints(seekPoint, new SeekPoint(getTimeUs(secondSeekPosition), secondSeekPosition));
            }
        }
        return new SeekPoints(seekPoint);
    }

    public long getTimeUs(long position) {
        return (1000000 * Math.max(0, position - this.dataStartPosition)) / ((long) this.averageBytesPerSecond);
    }

    public int getBytesPerFrame() {
        return this.blockAlignment;
    }

    public int getBitrate() {
        return this.sampleRateHz * this.bitsPerSample * this.numChannels;
    }

    public int getSampleRateHz() {
        return this.sampleRateHz;
    }

    public int getNumChannels() {
        return this.numChannels;
    }

    public int getEncoding() {
        return this.encoding;
    }
}
