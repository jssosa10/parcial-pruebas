package com.google.android.exoplayer2;

import com.google.android.exoplayer2.source.MediaSource.MediaPeriodId;

final class MediaPeriodInfo {
    public final long contentPositionUs;
    public final long durationUs;
    public final MediaPeriodId id;
    public final boolean isFinal;
    public final boolean isLastInTimelinePeriod;
    public final long startPositionUs;

    MediaPeriodInfo(MediaPeriodId id2, long startPositionUs2, long contentPositionUs2, long durationUs2, boolean isLastInTimelinePeriod2, boolean isFinal2) {
        this.id = id2;
        this.startPositionUs = startPositionUs2;
        this.contentPositionUs = contentPositionUs2;
        this.durationUs = durationUs2;
        this.isLastInTimelinePeriod = isLastInTimelinePeriod2;
        this.isFinal = isFinal2;
    }

    public MediaPeriodInfo copyWithStartPositionUs(long startPositionUs2) {
        MediaPeriodInfo mediaPeriodInfo = new MediaPeriodInfo(this.id, startPositionUs2, this.contentPositionUs, this.durationUs, this.isLastInTimelinePeriod, this.isFinal);
        return mediaPeriodInfo;
    }
}
