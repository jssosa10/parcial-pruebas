package com.google.android.exoplayer2;

import android.support.annotation.Nullable;
import android.util.Pair;
import com.google.android.exoplayer2.Timeline.Period;
import com.google.android.exoplayer2.Timeline.Window;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSource.MediaPeriodId;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.util.Assertions;

final class MediaPeriodQueue {
    private static final int MAXIMUM_BUFFER_AHEAD_PERIODS = 100;
    private int length;
    @Nullable
    private MediaPeriodHolder loading;
    private long nextWindowSequenceNumber;
    @Nullable
    private Object oldFrontPeriodUid;
    private long oldFrontPeriodWindowSequenceNumber;
    private final Period period = new Period();
    @Nullable
    private MediaPeriodHolder playing;
    @Nullable
    private MediaPeriodHolder reading;
    private int repeatMode;
    private boolean shuffleModeEnabled;
    private Timeline timeline = Timeline.EMPTY;
    private final Window window = new Window();

    public void setTimeline(Timeline timeline2) {
        this.timeline = timeline2;
    }

    public boolean updateRepeatMode(int repeatMode2) {
        this.repeatMode = repeatMode2;
        return updateForPlaybackModeChange();
    }

    public boolean updateShuffleModeEnabled(boolean shuffleModeEnabled2) {
        this.shuffleModeEnabled = shuffleModeEnabled2;
        return updateForPlaybackModeChange();
    }

    public boolean isLoading(MediaPeriod mediaPeriod) {
        MediaPeriodHolder mediaPeriodHolder = this.loading;
        return mediaPeriodHolder != null && mediaPeriodHolder.mediaPeriod == mediaPeriod;
    }

    public void reevaluateBuffer(long rendererPositionUs) {
        MediaPeriodHolder mediaPeriodHolder = this.loading;
        if (mediaPeriodHolder != null) {
            mediaPeriodHolder.reevaluateBuffer(rendererPositionUs);
        }
    }

    public boolean shouldLoadNextMediaPeriod() {
        MediaPeriodHolder mediaPeriodHolder = this.loading;
        return mediaPeriodHolder == null || (!mediaPeriodHolder.f18info.isFinal && this.loading.isFullyBuffered() && this.loading.f18info.durationUs != C.TIME_UNSET && this.length < 100);
    }

    @Nullable
    public MediaPeriodInfo getNextMediaPeriodInfo(long rendererPositionUs, PlaybackInfo playbackInfo) {
        MediaPeriodHolder mediaPeriodHolder = this.loading;
        if (mediaPeriodHolder == null) {
            return getFirstMediaPeriodInfo(playbackInfo);
        }
        return getFollowingMediaPeriodInfo(mediaPeriodHolder, rendererPositionUs);
    }

    public MediaPeriod enqueueNextMediaPeriod(RendererCapabilities[] rendererCapabilities, TrackSelector trackSelector, Allocator allocator, MediaSource mediaSource, MediaPeriodInfo info2) {
        long rendererPositionOffsetUs;
        MediaPeriodHolder mediaPeriodHolder = this.loading;
        if (mediaPeriodHolder == null) {
            rendererPositionOffsetUs = info2.startPositionUs;
        } else {
            rendererPositionOffsetUs = mediaPeriodHolder.getRendererOffset() + this.loading.f18info.durationUs;
        }
        MediaPeriodHolder mediaPeriodHolder2 = new MediaPeriodHolder(rendererCapabilities, rendererPositionOffsetUs, trackSelector, allocator, mediaSource, info2);
        if (this.loading != null) {
            Assertions.checkState(hasPlayingPeriod());
            this.loading.next = mediaPeriodHolder2;
        }
        this.oldFrontPeriodUid = null;
        this.loading = mediaPeriodHolder2;
        this.length++;
        return mediaPeriodHolder2.mediaPeriod;
    }

    public MediaPeriodHolder getLoadingPeriod() {
        return this.loading;
    }

    public MediaPeriodHolder getPlayingPeriod() {
        return this.playing;
    }

    public MediaPeriodHolder getReadingPeriod() {
        return this.reading;
    }

    public MediaPeriodHolder getFrontPeriod() {
        return hasPlayingPeriod() ? this.playing : this.loading;
    }

    public boolean hasPlayingPeriod() {
        return this.playing != null;
    }

    public MediaPeriodHolder advanceReadingPeriod() {
        MediaPeriodHolder mediaPeriodHolder = this.reading;
        Assertions.checkState((mediaPeriodHolder == null || mediaPeriodHolder.next == null) ? false : true);
        this.reading = this.reading.next;
        return this.reading;
    }

    public MediaPeriodHolder advancePlayingPeriod() {
        MediaPeriodHolder mediaPeriodHolder = this.playing;
        if (mediaPeriodHolder != null) {
            if (mediaPeriodHolder == this.reading) {
                this.reading = mediaPeriodHolder.next;
            }
            this.playing.release();
            this.length--;
            if (this.length == 0) {
                this.loading = null;
                this.oldFrontPeriodUid = this.playing.uid;
                this.oldFrontPeriodWindowSequenceNumber = this.playing.f18info.id.windowSequenceNumber;
            }
            this.playing = this.playing.next;
        } else {
            MediaPeriodHolder mediaPeriodHolder2 = this.loading;
            this.playing = mediaPeriodHolder2;
            this.reading = mediaPeriodHolder2;
        }
        return this.playing;
    }

    public boolean removeAfter(MediaPeriodHolder mediaPeriodHolder) {
        Assertions.checkState(mediaPeriodHolder != null);
        boolean removedReading = false;
        this.loading = mediaPeriodHolder;
        while (mediaPeriodHolder.next != null) {
            mediaPeriodHolder = mediaPeriodHolder.next;
            if (mediaPeriodHolder == this.reading) {
                this.reading = this.playing;
                removedReading = true;
            }
            mediaPeriodHolder.release();
            this.length--;
        }
        this.loading.next = null;
        return removedReading;
    }

    public void clear(boolean keepFrontPeriodUid) {
        MediaPeriodHolder front = getFrontPeriod();
        if (front != null) {
            this.oldFrontPeriodUid = keepFrontPeriodUid ? front.uid : null;
            this.oldFrontPeriodWindowSequenceNumber = front.f18info.id.windowSequenceNumber;
            front.release();
            removeAfter(front);
        } else if (!keepFrontPeriodUid) {
            this.oldFrontPeriodUid = null;
        }
        this.playing = null;
        this.loading = null;
        this.reading = null;
        this.length = 0;
    }

    public boolean updateQueuedPeriods(MediaPeriodId playingPeriodId, long rendererPositionUs) {
        int periodIndex = this.timeline.getIndexOfPeriod(playingPeriodId.periodUid);
        MediaPeriodHolder previousPeriodHolder = null;
        for (MediaPeriodHolder periodHolder = getFrontPeriod(); periodHolder != null; periodHolder = periodHolder.next) {
            if (previousPeriodHolder == null) {
                periodHolder.f18info = getUpdatedMediaPeriodInfo(periodHolder.f18info);
            } else if (periodIndex == -1 || !periodHolder.uid.equals(this.timeline.getUidOfPeriod(periodIndex))) {
                return true ^ removeAfter(previousPeriodHolder);
            } else {
                MediaPeriodInfo periodInfo = getFollowingMediaPeriodInfo(previousPeriodHolder, rendererPositionUs);
                if (periodInfo == null) {
                    return true ^ removeAfter(previousPeriodHolder);
                }
                periodHolder.f18info = getUpdatedMediaPeriodInfo(periodHolder.f18info);
                if (!canKeepMediaPeriodHolder(periodHolder, periodInfo)) {
                    return true ^ removeAfter(previousPeriodHolder);
                }
            }
            if (periodHolder.f18info.isLastInTimelinePeriod) {
                periodIndex = this.timeline.getNextPeriodIndex(periodIndex, this.period, this.window, this.repeatMode, this.shuffleModeEnabled);
            }
            previousPeriodHolder = periodHolder;
        }
        return true;
    }

    public MediaPeriodInfo getUpdatedMediaPeriodInfo(MediaPeriodInfo info2) {
        long durationUs;
        boolean isLastInPeriod = isLastInPeriod(info2.id);
        boolean isLastInTimeline = isLastInTimeline(info2.id, isLastInPeriod);
        this.timeline.getPeriodByUid(info2.id.periodUid, this.period);
        if (info2.id.isAd()) {
            durationUs = this.period.getAdDurationUs(info2.id.adGroupIndex, info2.id.adIndexInAdGroup);
        } else {
            durationUs = info2.id.endPositionUs == Long.MIN_VALUE ? this.period.getDurationUs() : info2.id.endPositionUs;
        }
        MediaPeriodInfo mediaPeriodInfo = new MediaPeriodInfo(info2.id, info2.startPositionUs, info2.contentPositionUs, durationUs, isLastInPeriod, isLastInTimeline);
        return mediaPeriodInfo;
    }

    public MediaPeriodId resolveMediaPeriodIdForAds(Object periodUid, long positionUs) {
        return resolveMediaPeriodIdForAds(periodUid, positionUs, resolvePeriodIndexToWindowSequenceNumber(periodUid));
    }

    private MediaPeriodId resolveMediaPeriodIdForAds(Object periodUid, long positionUs, long windowSequenceNumber) {
        long endPositionUs;
        long j = positionUs;
        Object obj = periodUid;
        this.timeline.getPeriodByUid(periodUid, this.period);
        int adGroupIndex = this.period.getAdGroupIndexForPositionUs(j);
        if (adGroupIndex == -1) {
            int nextAdGroupIndex = this.period.getAdGroupIndexAfterPositionUs(j);
            if (nextAdGroupIndex == -1) {
                endPositionUs = Long.MIN_VALUE;
            } else {
                endPositionUs = this.period.getAdGroupTimeUs(nextAdGroupIndex);
            }
            MediaPeriodId mediaPeriodId = new MediaPeriodId(periodUid, windowSequenceNumber, endPositionUs);
            return mediaPeriodId;
        }
        MediaPeriodId mediaPeriodId2 = new MediaPeriodId(periodUid, adGroupIndex, this.period.getFirstAdIndexToPlay(adGroupIndex), windowSequenceNumber);
        return mediaPeriodId2;
    }

    private long resolvePeriodIndexToWindowSequenceNumber(Object periodUid) {
        int windowIndex = this.timeline.getPeriodByUid(periodUid, this.period).windowIndex;
        Object obj = this.oldFrontPeriodUid;
        if (obj != null) {
            int oldFrontPeriodIndex = this.timeline.getIndexOfPeriod(obj);
            if (oldFrontPeriodIndex != -1 && this.timeline.getPeriod(oldFrontPeriodIndex, this.period).windowIndex == windowIndex) {
                return this.oldFrontPeriodWindowSequenceNumber;
            }
        }
        for (MediaPeriodHolder mediaPeriodHolder = getFrontPeriod(); mediaPeriodHolder != null; mediaPeriodHolder = mediaPeriodHolder.next) {
            if (mediaPeriodHolder.uid.equals(periodUid)) {
                return mediaPeriodHolder.f18info.id.windowSequenceNumber;
            }
        }
        for (MediaPeriodHolder mediaPeriodHolder2 = getFrontPeriod(); mediaPeriodHolder2 != null; mediaPeriodHolder2 = mediaPeriodHolder2.next) {
            int indexOfHolderInTimeline = this.timeline.getIndexOfPeriod(mediaPeriodHolder2.uid);
            if (indexOfHolderInTimeline != -1 && this.timeline.getPeriod(indexOfHolderInTimeline, this.period).windowIndex == windowIndex) {
                return mediaPeriodHolder2.f18info.id.windowSequenceNumber;
            }
        }
        long j = this.nextWindowSequenceNumber;
        this.nextWindowSequenceNumber = 1 + j;
        return j;
    }

    private boolean canKeepMediaPeriodHolder(MediaPeriodHolder periodHolder, MediaPeriodInfo info2) {
        MediaPeriodInfo periodHolderInfo = periodHolder.f18info;
        return periodHolderInfo.startPositionUs == info2.startPositionUs && periodHolderInfo.id.equals(info2.id);
    }

    private boolean updateForPlaybackModeChange() {
        MediaPeriodHolder lastValidPeriodHolder = getFrontPeriod();
        boolean z = true;
        if (lastValidPeriodHolder == null) {
            return true;
        }
        int currentPeriodIndex = this.timeline.getIndexOfPeriod(lastValidPeriodHolder.uid);
        while (true) {
            int nextPeriodIndex = this.timeline.getNextPeriodIndex(currentPeriodIndex, this.period, this.window, this.repeatMode, this.shuffleModeEnabled);
            while (lastValidPeriodHolder.next != null && !lastValidPeriodHolder.f18info.isLastInTimelinePeriod) {
                lastValidPeriodHolder = lastValidPeriodHolder.next;
            }
            if (nextPeriodIndex == -1 || lastValidPeriodHolder.next == null || this.timeline.getIndexOfPeriod(lastValidPeriodHolder.next.uid) != nextPeriodIndex) {
                boolean readingPeriodRemoved = removeAfter(lastValidPeriodHolder);
                lastValidPeriodHolder.f18info = getUpdatedMediaPeriodInfo(lastValidPeriodHolder.f18info);
            } else {
                lastValidPeriodHolder = lastValidPeriodHolder.next;
                currentPeriodIndex = nextPeriodIndex;
            }
        }
        boolean readingPeriodRemoved2 = removeAfter(lastValidPeriodHolder);
        lastValidPeriodHolder.f18info = getUpdatedMediaPeriodInfo(lastValidPeriodHolder.f18info);
        if (readingPeriodRemoved2 && hasPlayingPeriod()) {
            z = false;
        }
        return z;
    }

    private MediaPeriodInfo getFirstMediaPeriodInfo(PlaybackInfo playbackInfo) {
        return getMediaPeriodInfo(playbackInfo.periodId, playbackInfo.contentPositionUs, playbackInfo.startPositionUs);
    }

    @Nullable
    private MediaPeriodInfo getFollowingMediaPeriodInfo(MediaPeriodHolder mediaPeriodHolder, long rendererPositionUs) {
        MediaPeriodInfo mediaPeriodInfo;
        long startPositionUs;
        MediaPeriodInfo mediaPeriodInfo2;
        long windowSequenceNumber;
        long startPositionUs2;
        Object nextPeriodUid;
        Object nextPeriodUid2;
        long windowSequenceNumber2;
        MediaPeriodHolder mediaPeriodHolder2 = mediaPeriodHolder;
        MediaPeriodInfo mediaPeriodInfo3 = mediaPeriodHolder2.f18info;
        long bufferedDurationUs = (mediaPeriodHolder.getRendererOffset() + mediaPeriodInfo3.durationUs) - rendererPositionUs;
        if (mediaPeriodInfo3.isLastInTimelinePeriod) {
            int currentPeriodIndex = this.timeline.getIndexOfPeriod(mediaPeriodInfo3.id.periodUid);
            int nextPeriodIndex = this.timeline.getNextPeriodIndex(currentPeriodIndex, this.period, this.window, this.repeatMode, this.shuffleModeEnabled);
            if (nextPeriodIndex == -1) {
                return null;
            }
            int nextWindowIndex = this.timeline.getPeriod(nextPeriodIndex, this.period, true).windowIndex;
            Object nextPeriodUid3 = this.period.uid;
            long windowSequenceNumber3 = mediaPeriodInfo3.id.windowSequenceNumber;
            if (this.timeline.getWindow(nextWindowIndex, this.window).firstPeriodIndex == nextPeriodIndex) {
                Timeline timeline2 = this.timeline;
                Window window2 = this.window;
                long windowSequenceNumber4 = windowSequenceNumber3;
                Pair<Object, Long> defaultPosition = timeline2.getPeriodPosition(window2, this.period, nextWindowIndex, C.TIME_UNSET, Math.max(0, bufferedDurationUs));
                if (defaultPosition == null) {
                    return null;
                }
                Object nextPeriodUid4 = defaultPosition.first;
                long startPositionUs3 = ((Long) defaultPosition.second).longValue();
                if (mediaPeriodHolder2.next == null || !mediaPeriodHolder2.next.uid.equals(nextPeriodUid4)) {
                    nextPeriodUid2 = nextPeriodUid4;
                    long j = windowSequenceNumber4;
                    long j2 = this.nextWindowSequenceNumber;
                    Pair pair = defaultPosition;
                    this.nextWindowSequenceNumber = j2 + 1;
                    windowSequenceNumber2 = j2;
                } else {
                    windowSequenceNumber2 = mediaPeriodHolder2.next.f18info.id.windowSequenceNumber;
                    nextPeriodUid2 = nextPeriodUid4;
                }
                windowSequenceNumber = windowSequenceNumber2;
                startPositionUs2 = startPositionUs3;
                nextPeriodUid = nextPeriodUid2;
            } else {
                nextPeriodUid = nextPeriodUid3;
                windowSequenceNumber = windowSequenceNumber3;
                startPositionUs2 = 0;
            }
            long j3 = startPositionUs2;
            int i = nextWindowIndex;
            return getMediaPeriodInfo(resolveMediaPeriodIdForAds(nextPeriodUid, j3, windowSequenceNumber), j3, startPositionUs2);
        }
        MediaPeriodId currentPeriodId = mediaPeriodInfo3.id;
        this.timeline.getPeriodByUid(currentPeriodId.periodUid, this.period);
        if (currentPeriodId.isAd()) {
            int adGroupIndex = currentPeriodId.adGroupIndex;
            int adCountInCurrentAdGroup = this.period.getAdCountInAdGroup(adGroupIndex);
            if (adCountInCurrentAdGroup == -1) {
                return null;
            }
            int nextAdIndexInAdGroup = this.period.getNextAdIndexToPlay(adGroupIndex, currentPeriodId.adIndexInAdGroup);
            if (nextAdIndexInAdGroup < adCountInCurrentAdGroup) {
                if (!this.period.isAdAvailable(adGroupIndex, nextAdIndexInAdGroup)) {
                    int i2 = nextAdIndexInAdGroup;
                    mediaPeriodInfo2 = null;
                } else {
                    int i3 = nextAdIndexInAdGroup;
                    mediaPeriodInfo2 = getMediaPeriodInfoForAd(currentPeriodId.periodUid, adGroupIndex, nextAdIndexInAdGroup, mediaPeriodInfo3.contentPositionUs, currentPeriodId.windowSequenceNumber);
                }
                return mediaPeriodInfo2;
            }
            long startPositionUs4 = mediaPeriodInfo3.contentPositionUs;
            if (this.period.getAdGroupCount() == 1 && this.period.getAdGroupTimeUs(0) == 0) {
                Timeline timeline3 = this.timeline;
                Window window3 = this.window;
                Period period2 = this.period;
                Pair<Object, Long> defaultPosition2 = timeline3.getPeriodPosition(window3, period2, period2.windowIndex, C.TIME_UNSET, Math.max(0, bufferedDurationUs));
                if (defaultPosition2 == null) {
                    return null;
                }
                startPositionUs = ((Long) defaultPosition2.second).longValue();
            } else {
                startPositionUs = startPositionUs4;
            }
            return getMediaPeriodInfoForContent(currentPeriodId.periodUid, startPositionUs, currentPeriodId.windowSequenceNumber);
        } else if (mediaPeriodInfo3.id.endPositionUs != Long.MIN_VALUE) {
            int nextAdGroupIndex = this.period.getAdGroupIndexForPositionUs(mediaPeriodInfo3.id.endPositionUs);
            if (nextAdGroupIndex == -1) {
                return getMediaPeriodInfoForContent(currentPeriodId.periodUid, mediaPeriodInfo3.id.endPositionUs, currentPeriodId.windowSequenceNumber);
            }
            int adIndexInAdGroup = this.period.getFirstAdIndexToPlay(nextAdGroupIndex);
            if (!this.period.isAdAvailable(nextAdGroupIndex, adIndexInAdGroup)) {
                mediaPeriodInfo = null;
            } else {
                mediaPeriodInfo = getMediaPeriodInfoForAd(currentPeriodId.periodUid, nextAdGroupIndex, adIndexInAdGroup, mediaPeriodInfo3.id.endPositionUs, currentPeriodId.windowSequenceNumber);
            }
            return mediaPeriodInfo;
        } else {
            int adGroupCount = this.period.getAdGroupCount();
            if (adGroupCount == 0) {
                return null;
            }
            int adGroupIndex2 = adGroupCount - 1;
            if (this.period.getAdGroupTimeUs(adGroupIndex2) != Long.MIN_VALUE || this.period.hasPlayedAdGroup(adGroupIndex2)) {
                return null;
            }
            int adIndexInAdGroup2 = this.period.getFirstAdIndexToPlay(adGroupIndex2);
            if (!this.period.isAdAvailable(adGroupIndex2, adIndexInAdGroup2)) {
                return null;
            }
            long contentDurationUs = this.period.getDurationUs();
            int i4 = adIndexInAdGroup2;
            return getMediaPeriodInfoForAd(currentPeriodId.periodUid, adGroupIndex2, adIndexInAdGroup2, contentDurationUs, currentPeriodId.windowSequenceNumber);
        }
    }

    private MediaPeriodInfo getMediaPeriodInfo(MediaPeriodId id, long contentPositionUs, long startPositionUs) {
        this.timeline.getPeriodByUid(id.periodUid, this.period);
        if (!id.isAd()) {
            return getMediaPeriodInfoForContent(id.periodUid, startPositionUs, id.windowSequenceNumber);
        } else if (!this.period.isAdAvailable(id.adGroupIndex, id.adIndexInAdGroup)) {
            return null;
        } else {
            return getMediaPeriodInfoForAd(id.periodUid, id.adGroupIndex, id.adIndexInAdGroup, contentPositionUs, id.windowSequenceNumber);
        }
    }

    private MediaPeriodInfo getMediaPeriodInfoForAd(Object periodUid, int adGroupIndex, int adIndexInAdGroup, long contentPositionUs, long windowSequenceNumber) {
        MediaPeriodId id = new MediaPeriodId(periodUid, adGroupIndex, adIndexInAdGroup, windowSequenceNumber);
        boolean isLastInPeriod = isLastInPeriod(id);
        boolean isLastInTimeline = isLastInTimeline(id, isLastInPeriod);
        MediaPeriodInfo mediaPeriodInfo = new MediaPeriodInfo(id, adIndexInAdGroup == this.period.getFirstAdIndexToPlay(adGroupIndex) ? this.period.getAdResumePositionUs() : 0, contentPositionUs, this.timeline.getPeriodByUid(id.periodUid, this.period).getAdDurationUs(id.adGroupIndex, id.adIndexInAdGroup), isLastInPeriod, isLastInTimeline);
        return mediaPeriodInfo;
    }

    private MediaPeriodInfo getMediaPeriodInfoForContent(Object periodUid, long startPositionUs, long windowSequenceNumber) {
        long j;
        int nextAdGroupIndex = this.period.getAdGroupIndexAfterPositionUs(startPositionUs);
        if (nextAdGroupIndex == -1) {
            j = Long.MIN_VALUE;
        } else {
            j = this.period.getAdGroupTimeUs(nextAdGroupIndex);
        }
        long endPositionUs = j;
        MediaPeriodId mediaPeriodId = new MediaPeriodId(periodUid, windowSequenceNumber, endPositionUs);
        MediaPeriodId id = mediaPeriodId;
        this.timeline.getPeriodByUid(id.periodUid, this.period);
        boolean isLastInPeriod = isLastInPeriod(id);
        boolean z = isLastInPeriod;
        MediaPeriodId mediaPeriodId2 = id;
        MediaPeriodInfo mediaPeriodInfo = new MediaPeriodInfo(id, startPositionUs, C.TIME_UNSET, endPositionUs == Long.MIN_VALUE ? this.period.getDurationUs() : endPositionUs, isLastInPeriod, isLastInTimeline(id, isLastInPeriod));
        return mediaPeriodInfo;
    }

    private boolean isLastInPeriod(MediaPeriodId id) {
        int adGroupCount = this.timeline.getPeriodByUid(id.periodUid, this.period).getAdGroupCount();
        boolean z = true;
        if (adGroupCount == 0) {
            return true;
        }
        int lastAdGroupIndex = adGroupCount - 1;
        boolean isAd = id.isAd();
        if (this.period.getAdGroupTimeUs(lastAdGroupIndex) != Long.MIN_VALUE) {
            if (isAd || id.endPositionUs != Long.MIN_VALUE) {
                z = false;
            }
            return z;
        }
        int postrollAdCount = this.period.getAdCountInAdGroup(lastAdGroupIndex);
        if (postrollAdCount == -1) {
            return false;
        }
        if (!(isAd && id.adGroupIndex == lastAdGroupIndex && id.adIndexInAdGroup == postrollAdCount + -1) && (isAd || this.period.getFirstAdIndexToPlay(lastAdGroupIndex) != postrollAdCount)) {
            z = false;
        }
        return z;
    }

    private boolean isLastInTimeline(MediaPeriodId id, boolean isLastMediaPeriodInPeriod) {
        int periodIndex = this.timeline.getIndexOfPeriod(id.periodUid);
        if (!this.timeline.getWindow(this.timeline.getPeriod(periodIndex, this.period).windowIndex, this.window).isDynamic) {
            if (this.timeline.isLastPeriod(periodIndex, this.period, this.window, this.repeatMode, this.shuffleModeEnabled) && isLastMediaPeriodInPeriod) {
                return true;
            }
        }
        return false;
    }
}
