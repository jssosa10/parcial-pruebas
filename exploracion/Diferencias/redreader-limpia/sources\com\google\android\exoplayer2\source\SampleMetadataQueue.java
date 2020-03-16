package com.google.android.exoplayer2.source;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.extractor.TrackOutput.CryptoData;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;

final class SampleMetadataQueue {
    private static final int SAMPLE_CAPACITY_INCREMENT = 1000;
    private int absoluteFirstIndex;
    private int capacity = 1000;
    private CryptoData[] cryptoDatas;
    private int[] flags;
    private Format[] formats;
    private long largestDiscardedTimestampUs;
    private long largestQueuedTimestampUs;
    private int length;
    private long[] offsets;
    private int readPosition;
    private int relativeFirstIndex;
    private int[] sizes;
    private int[] sourceIds;
    private long[] timesUs;
    private Format upstreamFormat;
    private boolean upstreamFormatRequired;
    private boolean upstreamKeyframeRequired;
    private int upstreamSourceId;

    public static final class SampleExtrasHolder {
        public CryptoData cryptoData;
        public long offset;
        public int size;
    }

    public SampleMetadataQueue() {
        int i = this.capacity;
        this.sourceIds = new int[i];
        this.offsets = new long[i];
        this.timesUs = new long[i];
        this.flags = new int[i];
        this.sizes = new int[i];
        this.cryptoDatas = new CryptoData[i];
        this.formats = new Format[i];
        this.largestDiscardedTimestampUs = Long.MIN_VALUE;
        this.largestQueuedTimestampUs = Long.MIN_VALUE;
        this.upstreamFormatRequired = true;
        this.upstreamKeyframeRequired = true;
    }

    public void reset(boolean resetUpstreamFormat) {
        this.length = 0;
        this.absoluteFirstIndex = 0;
        this.relativeFirstIndex = 0;
        this.readPosition = 0;
        this.upstreamKeyframeRequired = true;
        this.largestDiscardedTimestampUs = Long.MIN_VALUE;
        this.largestQueuedTimestampUs = Long.MIN_VALUE;
        if (resetUpstreamFormat) {
            this.upstreamFormat = null;
            this.upstreamFormatRequired = true;
        }
    }

    public int getWriteIndex() {
        return this.absoluteFirstIndex + this.length;
    }

    public long discardUpstreamSamples(int discardFromIndex) {
        int discardCount = getWriteIndex() - discardFromIndex;
        Assertions.checkArgument(discardCount >= 0 && discardCount <= this.length - this.readPosition);
        this.length -= discardCount;
        this.largestQueuedTimestampUs = Math.max(this.largestDiscardedTimestampUs, getLargestTimestamp(this.length));
        int i = this.length;
        if (i == 0) {
            return 0;
        }
        int relativeLastWriteIndex = getRelativeIndex(i - 1);
        return this.offsets[relativeLastWriteIndex] + ((long) this.sizes[relativeLastWriteIndex]);
    }

    public void sourceId(int sourceId) {
        this.upstreamSourceId = sourceId;
    }

    public int getFirstIndex() {
        return this.absoluteFirstIndex;
    }

    public int getReadIndex() {
        return this.absoluteFirstIndex + this.readPosition;
    }

    public int peekSourceId() {
        return hasNextSample() ? this.sourceIds[getRelativeIndex(this.readPosition)] : this.upstreamSourceId;
    }

    public synchronized boolean hasNextSample() {
        return this.readPosition != this.length;
    }

    public synchronized Format getUpstreamFormat() {
        return this.upstreamFormatRequired ? null : this.upstreamFormat;
    }

    public synchronized long getLargestQueuedTimestampUs() {
        return this.largestQueuedTimestampUs;
    }

    public synchronized long getFirstTimestampUs() {
        return this.length == 0 ? Long.MIN_VALUE : this.timesUs[this.relativeFirstIndex];
    }

    public synchronized void rewind() {
        this.readPosition = 0;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0023, code lost:
        return -3;
     */
    public synchronized int read(FormatHolder formatHolder, DecoderInputBuffer buffer, boolean formatRequired, boolean loadingFinished, Format downstreamFormat, SampleExtrasHolder extrasHolder) {
        if (hasNextSample()) {
            int relativeReadIndex = getRelativeIndex(this.readPosition);
            if (!formatRequired) {
                if (this.formats[relativeReadIndex] == downstreamFormat) {
                    if (buffer.isFlagsOnly()) {
                        return -3;
                    }
                    buffer.timeUs = this.timesUs[relativeReadIndex];
                    buffer.setFlags(this.flags[relativeReadIndex]);
                    extrasHolder.size = this.sizes[relativeReadIndex];
                    extrasHolder.offset = this.offsets[relativeReadIndex];
                    extrasHolder.cryptoData = this.cryptoDatas[relativeReadIndex];
                    this.readPosition++;
                    return -4;
                }
            }
            formatHolder.format = this.formats[relativeReadIndex];
            return -5;
        } else if (loadingFinished) {
            buffer.setFlags(4);
            return -4;
        } else if (this.upstreamFormat != null && (formatRequired || this.upstreamFormat != downstreamFormat)) {
            formatHolder.format = this.upstreamFormat;
            return -5;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0039, code lost:
        return -1;
     */
    public synchronized int advanceTo(long timeUs, boolean toKeyframe, boolean allowTimeBeyondBuffer) {
        int relativeReadIndex = getRelativeIndex(this.readPosition);
        if (hasNextSample() && timeUs >= this.timesUs[relativeReadIndex]) {
            if (timeUs <= this.largestQueuedTimestampUs || allowTimeBeyondBuffer) {
                int offset = findSampleBefore(relativeReadIndex, this.length - this.readPosition, timeUs, toKeyframe);
                if (offset == -1) {
                    return -1;
                }
                this.readPosition += offset;
                return offset;
            }
        }
    }

    public synchronized int advanceToEnd() {
        int skipCount;
        skipCount = this.length - this.readPosition;
        this.readPosition = this.length;
        return skipCount;
    }

    public synchronized boolean setReadPosition(int sampleIndex) {
        if (this.absoluteFirstIndex > sampleIndex || sampleIndex > this.absoluteFirstIndex + this.length) {
            return false;
        }
        this.readPosition = sampleIndex - this.absoluteFirstIndex;
        return true;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0038, code lost:
        return -1;
     */
    public synchronized long discardTo(long timeUs, boolean toKeyframe, boolean stopAtReadPosition) {
        if (this.length != 0) {
            if (timeUs >= this.timesUs[this.relativeFirstIndex]) {
                int discardCount = findSampleBefore(this.relativeFirstIndex, (!stopAtReadPosition || this.readPosition == this.length) ? this.length : this.readPosition + 1, timeUs, toKeyframe);
                if (discardCount == -1) {
                    return -1;
                }
                return discardSamples(discardCount);
            }
        }
    }

    public synchronized long discardToRead() {
        if (this.readPosition == 0) {
            return -1;
        }
        return discardSamples(this.readPosition);
    }

    public synchronized long discardToEnd() {
        if (this.length == 0) {
            return -1;
        }
        return discardSamples(this.length);
    }

    public synchronized boolean format(Format format) {
        if (format == null) {
            this.upstreamFormatRequired = true;
            return false;
        }
        this.upstreamFormatRequired = false;
        if (Util.areEqual(format, this.upstreamFormat)) {
            return false;
        }
        this.upstreamFormat = format;
        return true;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:20:0x00d2, code lost:
        return;
     */
    public synchronized void commitSample(long timeUs, int sampleFlags, long offset, int size, CryptoData cryptoData) {
        synchronized (this) {
            if (this.upstreamKeyframeRequired) {
                if ((sampleFlags & 1) != 0) {
                    this.upstreamKeyframeRequired = false;
                } else {
                    return;
                }
            }
            Assertions.checkState(!this.upstreamFormatRequired);
            commitSampleTimestamp(timeUs);
            int relativeEndIndex = getRelativeIndex(this.length);
            this.timesUs[relativeEndIndex] = timeUs;
            this.offsets[relativeEndIndex] = offset;
            this.sizes[relativeEndIndex] = size;
            this.flags[relativeEndIndex] = sampleFlags;
            this.cryptoDatas[relativeEndIndex] = cryptoData;
            this.formats[relativeEndIndex] = this.upstreamFormat;
            this.sourceIds[relativeEndIndex] = this.upstreamSourceId;
            this.length++;
            if (this.length == this.capacity) {
                int newCapacity = this.capacity + 1000;
                int[] newSourceIds = new int[newCapacity];
                long[] newOffsets = new long[newCapacity];
                long[] newTimesUs = new long[newCapacity];
                int[] newFlags = new int[newCapacity];
                int[] newSizes = new int[newCapacity];
                CryptoData[] newCryptoDatas = new CryptoData[newCapacity];
                Format[] newFormats = new Format[newCapacity];
                int beforeWrap = this.capacity - this.relativeFirstIndex;
                System.arraycopy(this.offsets, this.relativeFirstIndex, newOffsets, 0, beforeWrap);
                System.arraycopy(this.timesUs, this.relativeFirstIndex, newTimesUs, 0, beforeWrap);
                System.arraycopy(this.flags, this.relativeFirstIndex, newFlags, 0, beforeWrap);
                System.arraycopy(this.sizes, this.relativeFirstIndex, newSizes, 0, beforeWrap);
                System.arraycopy(this.cryptoDatas, this.relativeFirstIndex, newCryptoDatas, 0, beforeWrap);
                System.arraycopy(this.formats, this.relativeFirstIndex, newFormats, 0, beforeWrap);
                System.arraycopy(this.sourceIds, this.relativeFirstIndex, newSourceIds, 0, beforeWrap);
                int afterWrap = this.relativeFirstIndex;
                System.arraycopy(this.offsets, 0, newOffsets, beforeWrap, afterWrap);
                System.arraycopy(this.timesUs, 0, newTimesUs, beforeWrap, afterWrap);
                System.arraycopy(this.flags, 0, newFlags, beforeWrap, afterWrap);
                System.arraycopy(this.sizes, 0, newSizes, beforeWrap, afterWrap);
                System.arraycopy(this.cryptoDatas, 0, newCryptoDatas, beforeWrap, afterWrap);
                System.arraycopy(this.formats, 0, newFormats, beforeWrap, afterWrap);
                System.arraycopy(this.sourceIds, 0, newSourceIds, beforeWrap, afterWrap);
                this.offsets = newOffsets;
                this.timesUs = newTimesUs;
                this.flags = newFlags;
                this.sizes = newSizes;
                this.cryptoDatas = newCryptoDatas;
                this.formats = newFormats;
                this.sourceIds = newSourceIds;
                this.relativeFirstIndex = 0;
                this.length = this.capacity;
                this.capacity = newCapacity;
            }
        }
    }

    public synchronized void commitSampleTimestamp(long timeUs) {
        this.largestQueuedTimestampUs = Math.max(this.largestQueuedTimestampUs, timeUs);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:9:0x000f, code lost:
        return r1;
     */
    public synchronized boolean attemptSplice(long timeUs) {
        boolean z = false;
        if (this.length == 0) {
            if (timeUs > this.largestDiscardedTimestampUs) {
                z = true;
            }
        } else if (Math.max(this.largestDiscardedTimestampUs, getLargestTimestamp(this.readPosition)) >= timeUs) {
            return false;
        } else {
            int retainCount = this.length;
            int relativeSampleIndex = getRelativeIndex(this.length - 1);
            while (retainCount > this.readPosition && this.timesUs[relativeSampleIndex] >= timeUs) {
                retainCount--;
                relativeSampleIndex--;
                if (relativeSampleIndex == -1) {
                    relativeSampleIndex = this.capacity - 1;
                }
            }
            discardUpstreamSamples(this.absoluteFirstIndex + retainCount);
            return true;
        }
    }

    private int findSampleBefore(int relativeStartIndex, int length2, long timeUs, boolean keyframe) {
        int sampleCountToTarget = -1;
        int searchIndex = relativeStartIndex;
        for (int i = 0; i < length2 && this.timesUs[searchIndex] <= timeUs; i++) {
            if (!keyframe || (this.flags[searchIndex] & 1) != 0) {
                sampleCountToTarget = i;
            }
            searchIndex++;
            if (searchIndex == this.capacity) {
                searchIndex = 0;
            }
        }
        return sampleCountToTarget;
    }

    private long discardSamples(int discardCount) {
        this.largestDiscardedTimestampUs = Math.max(this.largestDiscardedTimestampUs, getLargestTimestamp(discardCount));
        this.length -= discardCount;
        this.absoluteFirstIndex += discardCount;
        this.relativeFirstIndex += discardCount;
        int i = this.relativeFirstIndex;
        int i2 = this.capacity;
        if (i >= i2) {
            this.relativeFirstIndex = i - i2;
        }
        this.readPosition -= discardCount;
        if (this.readPosition < 0) {
            this.readPosition = 0;
        }
        if (this.length != 0) {
            return this.offsets[this.relativeFirstIndex];
        }
        int i3 = this.relativeFirstIndex;
        if (i3 == 0) {
            i3 = this.capacity;
        }
        int relativeLastDiscardIndex = i3 - 1;
        return this.offsets[relativeLastDiscardIndex] + ((long) this.sizes[relativeLastDiscardIndex]);
    }

    private long getLargestTimestamp(int length2) {
        if (length2 == 0) {
            return Long.MIN_VALUE;
        }
        long largestTimestampUs = Long.MIN_VALUE;
        int relativeSampleIndex = getRelativeIndex(length2 - 1);
        for (int i = 0; i < length2; i++) {
            largestTimestampUs = Math.max(largestTimestampUs, this.timesUs[relativeSampleIndex]);
            if ((this.flags[relativeSampleIndex] & 1) != 0) {
                break;
            }
            relativeSampleIndex--;
            if (relativeSampleIndex == -1) {
                relativeSampleIndex = this.capacity - 1;
            }
        }
        return largestTimestampUs;
    }

    private int getRelativeIndex(int offset) {
        int relativeIndex = this.relativeFirstIndex + offset;
        int i = this.capacity;
        return relativeIndex < i ? relativeIndex : relativeIndex - i;
    }
}
