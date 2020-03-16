package com.google.android.exoplayer2.offline;

public final class TrackKey {
    public final int groupIndex;
    public final int periodIndex;
    public final int trackIndex;

    public TrackKey(int periodIndex2, int groupIndex2, int trackIndex2) {
        this.periodIndex = periodIndex2;
        this.groupIndex = groupIndex2;
        this.trackIndex = trackIndex2;
    }
}
