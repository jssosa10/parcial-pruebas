package com.google.android.exoplayer2;

import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.Timeline.Window;
import com.google.android.exoplayer2.source.MediaSource.MediaPeriodId;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectorResult;

final class PlaybackInfo {
    private static final MediaPeriodId DUMMY_MEDIA_PERIOD_ID = new MediaPeriodId(new Object());
    public volatile long bufferedPositionUs;
    public final long contentPositionUs;
    public final boolean isLoading;
    public final MediaPeriodId loadingMediaPeriodId;
    @Nullable
    public final Object manifest;
    public final MediaPeriodId periodId;
    public final int playbackState;
    public volatile long positionUs;
    public final long startPositionUs;
    public final Timeline timeline;
    public volatile long totalBufferedDurationUs;
    public final TrackGroupArray trackGroups;
    public final TrackSelectorResult trackSelectorResult;

    public static PlaybackInfo createDummy(long startPositionUs2, TrackSelectorResult emptyTrackSelectorResult) {
        TrackSelectorResult trackSelectorResult2 = emptyTrackSelectorResult;
        PlaybackInfo playbackInfo = new PlaybackInfo(Timeline.EMPTY, null, DUMMY_MEDIA_PERIOD_ID, startPositionUs2, C.TIME_UNSET, 1, false, TrackGroupArray.EMPTY, trackSelectorResult2, DUMMY_MEDIA_PERIOD_ID, startPositionUs2, 0, startPositionUs2);
        return playbackInfo;
    }

    public PlaybackInfo(Timeline timeline2, @Nullable Object manifest2, MediaPeriodId periodId2, long startPositionUs2, long contentPositionUs2, int playbackState2, boolean isLoading2, TrackGroupArray trackGroups2, TrackSelectorResult trackSelectorResult2, MediaPeriodId loadingMediaPeriodId2, long bufferedPositionUs2, long totalBufferedDurationUs2, long positionUs2) {
        this.timeline = timeline2;
        this.manifest = manifest2;
        this.periodId = periodId2;
        this.startPositionUs = startPositionUs2;
        this.contentPositionUs = contentPositionUs2;
        this.playbackState = playbackState2;
        this.isLoading = isLoading2;
        this.trackGroups = trackGroups2;
        this.trackSelectorResult = trackSelectorResult2;
        this.loadingMediaPeriodId = loadingMediaPeriodId2;
        this.bufferedPositionUs = bufferedPositionUs2;
        this.totalBufferedDurationUs = totalBufferedDurationUs2;
        this.positionUs = positionUs2;
    }

    public MediaPeriodId getDummyFirstMediaPeriodId(boolean shuffleModeEnabled, Window window) {
        if (this.timeline.isEmpty()) {
            return DUMMY_MEDIA_PERIOD_ID;
        }
        Timeline timeline2 = this.timeline;
        return new MediaPeriodId(this.timeline.getUidOfPeriod(timeline2.getWindow(timeline2.getFirstWindowIndex(shuffleModeEnabled), window).firstPeriodIndex));
    }

    @CheckResult
    public PlaybackInfo resetToNewPosition(MediaPeriodId periodId2, long startPositionUs2, long contentPositionUs2) {
        PlaybackInfo playbackInfo = new PlaybackInfo(this.timeline, this.manifest, periodId2, startPositionUs2, periodId2.isAd() ? contentPositionUs2 : -9223372036854775807L, this.playbackState, this.isLoading, this.trackGroups, this.trackSelectorResult, periodId2, startPositionUs2, 0, startPositionUs2);
        return playbackInfo;
    }

    @CheckResult
    public PlaybackInfo copyWithNewPosition(MediaPeriodId periodId2, long positionUs2, long contentPositionUs2, long totalBufferedDurationUs2) {
        PlaybackInfo playbackInfo = new PlaybackInfo(this.timeline, this.manifest, periodId2, positionUs2, periodId2.isAd() ? contentPositionUs2 : -9223372036854775807L, this.playbackState, this.isLoading, this.trackGroups, this.trackSelectorResult, this.loadingMediaPeriodId, this.bufferedPositionUs, totalBufferedDurationUs2, positionUs2);
        return playbackInfo;
    }

    @CheckResult
    public PlaybackInfo copyWithTimeline(Timeline timeline2, Object manifest2) {
        PlaybackInfo playbackInfo = new PlaybackInfo(timeline2, manifest2, this.periodId, this.startPositionUs, this.contentPositionUs, this.playbackState, this.isLoading, this.trackGroups, this.trackSelectorResult, this.loadingMediaPeriodId, this.bufferedPositionUs, this.totalBufferedDurationUs, this.positionUs);
        return playbackInfo;
    }

    @CheckResult
    public PlaybackInfo copyWithPlaybackState(int playbackState2) {
        int i = playbackState2;
        PlaybackInfo playbackInfo = new PlaybackInfo(this.timeline, this.manifest, this.periodId, this.startPositionUs, this.contentPositionUs, i, this.isLoading, this.trackGroups, this.trackSelectorResult, this.loadingMediaPeriodId, this.bufferedPositionUs, this.totalBufferedDurationUs, this.positionUs);
        return playbackInfo;
    }

    @CheckResult
    public PlaybackInfo copyWithIsLoading(boolean isLoading2) {
        boolean z = isLoading2;
        PlaybackInfo playbackInfo = new PlaybackInfo(this.timeline, this.manifest, this.periodId, this.startPositionUs, this.contentPositionUs, this.playbackState, z, this.trackGroups, this.trackSelectorResult, this.loadingMediaPeriodId, this.bufferedPositionUs, this.totalBufferedDurationUs, this.positionUs);
        return playbackInfo;
    }

    @CheckResult
    public PlaybackInfo copyWithTrackInfo(TrackGroupArray trackGroups2, TrackSelectorResult trackSelectorResult2) {
        TrackGroupArray trackGroupArray = trackGroups2;
        TrackSelectorResult trackSelectorResult3 = trackSelectorResult2;
        PlaybackInfo playbackInfo = new PlaybackInfo(this.timeline, this.manifest, this.periodId, this.startPositionUs, this.contentPositionUs, this.playbackState, this.isLoading, trackGroupArray, trackSelectorResult3, this.loadingMediaPeriodId, this.bufferedPositionUs, this.totalBufferedDurationUs, this.positionUs);
        return playbackInfo;
    }

    @CheckResult
    public PlaybackInfo copyWithLoadingMediaPeriodId(MediaPeriodId loadingMediaPeriodId2) {
        MediaPeriodId mediaPeriodId = loadingMediaPeriodId2;
        PlaybackInfo playbackInfo = new PlaybackInfo(this.timeline, this.manifest, this.periodId, this.startPositionUs, this.contentPositionUs, this.playbackState, this.isLoading, this.trackGroups, this.trackSelectorResult, mediaPeriodId, this.bufferedPositionUs, this.totalBufferedDurationUs, this.positionUs);
        return playbackInfo;
    }
}
