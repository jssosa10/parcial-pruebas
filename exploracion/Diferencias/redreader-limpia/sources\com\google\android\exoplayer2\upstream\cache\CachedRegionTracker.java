package com.google.android.exoplayer2.upstream.cache;

import android.support.annotation.NonNull;
import com.google.android.exoplayer2.extractor.ChunkIndex;
import com.google.android.exoplayer2.upstream.cache.Cache.Listener;
import com.google.android.exoplayer2.util.Log;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

public final class CachedRegionTracker implements Listener {
    public static final int CACHED_TO_END = -2;
    public static final int NOT_CACHED = -1;
    private static final String TAG = "CachedRegionTracker";
    private final Cache cache;
    private final String cacheKey;
    private final ChunkIndex chunkIndex;
    private final Region lookupRegion = new Region(0, 0);
    private final TreeSet<Region> regions = new TreeSet<>();

    private static class Region implements Comparable<Region> {
        public long endOffset;
        public int endOffsetIndex;
        public long startOffset;

        public Region(long position, long endOffset2) {
            this.startOffset = position;
            this.endOffset = endOffset2;
        }

        public int compareTo(@NonNull Region another) {
            long j = this.startOffset;
            long j2 = another.startOffset;
            if (j < j2) {
                return -1;
            }
            return j == j2 ? 0 : 1;
        }
    }

    public CachedRegionTracker(Cache cache2, String cacheKey2, ChunkIndex chunkIndex2) {
        this.cache = cache2;
        this.cacheKey = cacheKey2;
        this.chunkIndex = chunkIndex2;
        synchronized (this) {
            Iterator<CacheSpan> spanIterator = cache2.addListener(cacheKey2, this).descendingIterator();
            while (spanIterator.hasNext()) {
                mergeSpan((CacheSpan) spanIterator.next());
            }
        }
    }

    public void release() {
        this.cache.removeListener(this.cacheKey, this);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0066, code lost:
        return -1;
     */
    public synchronized int getRegionEndTimeMs(long byteOffset) {
        this.lookupRegion.startOffset = byteOffset;
        Region floorRegion = (Region) this.regions.floor(this.lookupRegion);
        if (floorRegion != null && byteOffset <= floorRegion.endOffset) {
            if (floorRegion.endOffsetIndex != -1) {
                int index = floorRegion.endOffsetIndex;
                if (index == this.chunkIndex.length - 1 && floorRegion.endOffset == this.chunkIndex.offsets[index] + ((long) this.chunkIndex.sizes[index])) {
                    return -2;
                }
                return (int) ((this.chunkIndex.timesUs[index] + ((this.chunkIndex.durationsUs[index] * (floorRegion.endOffset - this.chunkIndex.offsets[index])) / ((long) this.chunkIndex.sizes[index]))) / 1000);
            }
        }
    }

    public synchronized void onSpanAdded(Cache cache2, CacheSpan span) {
        mergeSpan(span);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:19:0x006c, code lost:
        return;
     */
    public synchronized void onSpanRemoved(Cache cache2, CacheSpan span) {
        Region removedRegion = new Region(span.position, span.position + span.length);
        Region floorRegion = (Region) this.regions.floor(removedRegion);
        if (floorRegion == null) {
            Log.e(TAG, "Removed a span we were not aware of");
            return;
        }
        this.regions.remove(floorRegion);
        if (floorRegion.startOffset < removedRegion.startOffset) {
            Region newFloorRegion = new Region(floorRegion.startOffset, removedRegion.startOffset);
            int index = Arrays.binarySearch(this.chunkIndex.offsets, newFloorRegion.endOffset);
            newFloorRegion.endOffsetIndex = index < 0 ? (-index) - 2 : index;
            this.regions.add(newFloorRegion);
        }
        if (floorRegion.endOffset > removedRegion.endOffset) {
            Region newCeilingRegion = new Region(removedRegion.endOffset + 1, floorRegion.endOffset);
            newCeilingRegion.endOffsetIndex = floorRegion.endOffsetIndex;
            this.regions.add(newCeilingRegion);
        }
    }

    public void onSpanTouched(Cache cache2, CacheSpan oldSpan, CacheSpan newSpan) {
    }

    private void mergeSpan(CacheSpan span) {
        Region newRegion = new Region(span.position, span.position + span.length);
        Region floorRegion = (Region) this.regions.floor(newRegion);
        Region ceilingRegion = (Region) this.regions.ceiling(newRegion);
        boolean floorConnects = regionsConnect(floorRegion, newRegion);
        if (regionsConnect(newRegion, ceilingRegion)) {
            if (floorConnects) {
                floorRegion.endOffset = ceilingRegion.endOffset;
                floorRegion.endOffsetIndex = ceilingRegion.endOffsetIndex;
            } else {
                newRegion.endOffset = ceilingRegion.endOffset;
                newRegion.endOffsetIndex = ceilingRegion.endOffsetIndex;
                this.regions.add(newRegion);
            }
            this.regions.remove(ceilingRegion);
        } else if (floorConnects) {
            floorRegion.endOffset = newRegion.endOffset;
            int index = floorRegion.endOffsetIndex;
            while (index < this.chunkIndex.length - 1 && this.chunkIndex.offsets[index + 1] <= floorRegion.endOffset) {
                index++;
            }
            floorRegion.endOffsetIndex = index;
        } else {
            int index2 = Arrays.binarySearch(this.chunkIndex.offsets, newRegion.endOffset);
            newRegion.endOffsetIndex = index2 < 0 ? (-index2) - 2 : index2;
            this.regions.add(newRegion);
        }
    }

    private boolean regionsConnect(Region lower, Region upper) {
        return (lower == null || upper == null || lower.endOffset != upper.startOffset) ? false : true;
    }
}
