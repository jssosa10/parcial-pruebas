package com.google.android.exoplayer2.trackselection;

import android.os.SystemClock;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.chunk.MediaChunk;
import com.google.android.exoplayer2.source.chunk.MediaChunkIterator;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import java.util.List;
import java.util.Random;

public final class RandomTrackSelection extends BaseTrackSelection {
    private final Random random;
    private int selectedIndex;

    public static final class Factory implements com.google.android.exoplayer2.trackselection.TrackSelection.Factory {
        private final Random random;

        public Factory() {
            this.random = new Random();
        }

        public Factory(int seed) {
            this.random = new Random((long) seed);
        }

        public RandomTrackSelection createTrackSelection(TrackGroup group, BandwidthMeter bandwidthMeter, int... tracks) {
            return new RandomTrackSelection(group, tracks, this.random);
        }
    }

    public RandomTrackSelection(TrackGroup group, int... tracks) {
        super(group, tracks);
        this.random = new Random();
        this.selectedIndex = this.random.nextInt(this.length);
    }

    public RandomTrackSelection(TrackGroup group, int[] tracks, long seed) {
        this(group, tracks, new Random(seed));
    }

    public RandomTrackSelection(TrackGroup group, int[] tracks, Random random2) {
        super(group, tracks);
        this.random = random2;
        this.selectedIndex = random2.nextInt(this.length);
    }

    public void updateSelectedTrack(long playbackPositionUs, long bufferedDurationUs, long availableDurationUs, List<? extends MediaChunk> list, MediaChunkIterator[] mediaChunkIterators) {
        long nowMs = SystemClock.elapsedRealtime();
        int nonBlacklistedFormatCount = 0;
        for (int i = 0; i < this.length; i++) {
            if (!isBlacklisted(i, nowMs)) {
                nonBlacklistedFormatCount++;
            }
        }
        this.selectedIndex = this.random.nextInt(nonBlacklistedFormatCount);
        if (nonBlacklistedFormatCount != this.length) {
            int nonBlacklistedFormatCount2 = 0;
            for (int i2 = 0; i2 < this.length; i2++) {
                if (!isBlacklisted(i2, nowMs)) {
                    int nonBlacklistedFormatCount3 = nonBlacklistedFormatCount2 + 1;
                    if (this.selectedIndex == nonBlacklistedFormatCount2) {
                        this.selectedIndex = i2;
                        return;
                    }
                    nonBlacklistedFormatCount2 = nonBlacklistedFormatCount3;
                }
            }
        }
    }

    public int getSelectedIndex() {
        return this.selectedIndex;
    }

    public int getSelectionReason() {
        return 3;
    }

    @Nullable
    public Object getSelectionData() {
        return null;
    }
}
