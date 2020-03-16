package com.google.android.exoplayer2.source.chunk;

import java.util.NoSuchElementException;

public abstract class BaseMediaChunkIterator implements MediaChunkIterator {
    private long currentIndex;
    private final long fromIndex;
    private final long toIndex;

    public BaseMediaChunkIterator(long fromIndex2, long toIndex2) {
        this.fromIndex = fromIndex2;
        this.toIndex = toIndex2;
        this.currentIndex = fromIndex2 - 1;
    }

    public boolean isEnded() {
        return this.currentIndex > this.toIndex;
    }

    public boolean next() {
        this.currentIndex++;
        return !isEnded();
    }

    /* access modifiers changed from: protected */
    public void checkInBounds() {
        long j = this.currentIndex;
        if (j < this.fromIndex || j > this.toIndex) {
            throw new NoSuchElementException();
        }
    }

    /* access modifiers changed from: protected */
    public long getCurrentIndex() {
        return this.currentIndex;
    }
}
