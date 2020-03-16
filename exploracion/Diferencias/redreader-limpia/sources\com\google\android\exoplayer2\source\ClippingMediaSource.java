package com.google.android.exoplayer2.source;

import android.support.annotation.Nullable;
import android.support.v4.os.EnvironmentCompat;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Timeline.Period;
import com.google.android.exoplayer2.Timeline.Window;
import com.google.android.exoplayer2.source.MediaSource.MediaPeriodId;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

public final class ClippingMediaSource extends CompositeMediaSource<Void> {
    private final boolean allowDynamicClippingUpdates;
    private IllegalClippingException clippingError;
    private ClippingTimeline clippingTimeline;
    private final boolean enableInitialDiscontinuity;
    private final long endUs;
    @Nullable
    private Object manifest;
    private final ArrayList<ClippingMediaPeriod> mediaPeriods;
    private final MediaSource mediaSource;
    private long periodEndUs;
    private long periodStartUs;
    private final boolean relativeToDefaultPosition;
    private final long startUs;
    private final Window window;

    private static final class ClippingTimeline extends ForwardingTimeline {
        private final long durationUs;
        private final long endUs;
        private final boolean isDynamic;
        private final long startUs;

        public ClippingTimeline(Timeline timeline, long startUs2, long endUs2) throws IllegalClippingException {
            long j = endUs2;
            super(timeline);
            boolean z = false;
            if (timeline.getPeriodCount() == 1) {
                Window window = timeline.getWindow(0, new Window());
                long startUs3 = Math.max(0, startUs2);
                long resolvedEndUs = j == Long.MIN_VALUE ? window.durationUs : Math.max(0, j);
                if (window.durationUs != C.TIME_UNSET) {
                    if (resolvedEndUs > window.durationUs) {
                        resolvedEndUs = window.durationUs;
                    }
                    if (startUs3 != 0 && !window.isSeekable) {
                        throw new IllegalClippingException(1);
                    } else if (startUs3 > resolvedEndUs) {
                        throw new IllegalClippingException(2);
                    }
                }
                this.startUs = startUs3;
                this.endUs = resolvedEndUs;
                this.durationUs = resolvedEndUs == C.TIME_UNSET ? -9223372036854775807L : resolvedEndUs - startUs3;
                if (window.isDynamic && (resolvedEndUs == C.TIME_UNSET || (window.durationUs != C.TIME_UNSET && resolvedEndUs == window.durationUs))) {
                    z = true;
                }
                this.isDynamic = z;
                return;
            }
            Timeline timeline2 = timeline;
            long j2 = startUs2;
            throw new IllegalClippingException(0);
        }

        public Window getWindow(int windowIndex, Window window, boolean setTag, long defaultPositionProjectionUs) {
            long j;
            this.timeline.getWindow(0, window, setTag, 0);
            window.positionInFirstPeriodUs += this.startUs;
            window.durationUs = this.durationUs;
            window.isDynamic = this.isDynamic;
            if (window.defaultPositionUs != C.TIME_UNSET) {
                window.defaultPositionUs = Math.max(window.defaultPositionUs, this.startUs);
                if (this.endUs == C.TIME_UNSET) {
                    j = window.defaultPositionUs;
                } else {
                    j = Math.min(window.defaultPositionUs, this.endUs);
                }
                window.defaultPositionUs = j;
                window.defaultPositionUs -= this.startUs;
            }
            long startMs = C.usToMs(this.startUs);
            if (window.presentationStartTimeMs != C.TIME_UNSET) {
                window.presentationStartTimeMs += startMs;
            }
            if (window.windowStartTimeMs != C.TIME_UNSET) {
                window.windowStartTimeMs += startMs;
            }
            return window;
        }

        public Period getPeriod(int periodIndex, Period period, boolean setIds) {
            this.timeline.getPeriod(0, period, setIds);
            long positionInClippedWindowUs = period.getPositionInWindowUs() - this.startUs;
            long j = this.durationUs;
            return period.set(period.id, period.uid, 0, j == C.TIME_UNSET ? -9223372036854775807L : j - positionInClippedWindowUs, positionInClippedWindowUs);
        }
    }

    public static final class IllegalClippingException extends IOException {
        public static final int REASON_INVALID_PERIOD_COUNT = 0;
        public static final int REASON_NOT_SEEKABLE_TO_START = 1;
        public static final int REASON_START_EXCEEDS_END = 2;
        public final int reason;

        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface Reason {
        }

        public IllegalClippingException(int reason2) {
            StringBuilder sb = new StringBuilder();
            sb.append("Illegal clipping: ");
            sb.append(getReasonDescription(reason2));
            super(sb.toString());
            this.reason = reason2;
        }

        private static String getReasonDescription(int reason2) {
            switch (reason2) {
                case 0:
                    return "invalid period count";
                case 1:
                    return "not seekable to start";
                case 2:
                    return "start exceeds end";
                default:
                    return EnvironmentCompat.MEDIA_UNKNOWN;
            }
        }
    }

    public ClippingMediaSource(MediaSource mediaSource2, long startPositionUs, long endPositionUs) {
        this(mediaSource2, startPositionUs, endPositionUs, true, false, false);
    }

    @Deprecated
    public ClippingMediaSource(MediaSource mediaSource2, long startPositionUs, long endPositionUs, boolean enableInitialDiscontinuity2) {
        this(mediaSource2, startPositionUs, endPositionUs, enableInitialDiscontinuity2, false, false);
    }

    public ClippingMediaSource(MediaSource mediaSource2, long durationUs) {
        this(mediaSource2, 0, durationUs, true, false, true);
    }

    public ClippingMediaSource(MediaSource mediaSource2, long startPositionUs, long endPositionUs, boolean enableInitialDiscontinuity2, boolean allowDynamicClippingUpdates2, boolean relativeToDefaultPosition2) {
        Assertions.checkArgument(startPositionUs >= 0);
        this.mediaSource = (MediaSource) Assertions.checkNotNull(mediaSource2);
        this.startUs = startPositionUs;
        this.endUs = endPositionUs;
        this.enableInitialDiscontinuity = enableInitialDiscontinuity2;
        this.allowDynamicClippingUpdates = allowDynamicClippingUpdates2;
        this.relativeToDefaultPosition = relativeToDefaultPosition2;
        this.mediaPeriods = new ArrayList<>();
        this.window = new Window();
    }

    public void prepareSourceInternal(ExoPlayer player, boolean isTopLevelSource, @Nullable TransferListener mediaTransferListener) {
        super.prepareSourceInternal(player, isTopLevelSource, mediaTransferListener);
        prepareChildSource(null, this.mediaSource);
    }

    public void maybeThrowSourceInfoRefreshError() throws IOException {
        IllegalClippingException illegalClippingException = this.clippingError;
        if (illegalClippingException == null) {
            super.maybeThrowSourceInfoRefreshError();
            return;
        }
        throw illegalClippingException;
    }

    public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator) {
        ClippingMediaPeriod mediaPeriod = new ClippingMediaPeriod(this.mediaSource.createPeriod(id, allocator), this.enableInitialDiscontinuity, this.periodStartUs, this.periodEndUs);
        this.mediaPeriods.add(mediaPeriod);
        return mediaPeriod;
    }

    public void releasePeriod(MediaPeriod mediaPeriod) {
        Assertions.checkState(this.mediaPeriods.remove(mediaPeriod));
        this.mediaSource.releasePeriod(((ClippingMediaPeriod) mediaPeriod).mediaPeriod);
        if (this.mediaPeriods.isEmpty() && !this.allowDynamicClippingUpdates) {
            refreshClippedTimeline(this.clippingTimeline.timeline);
        }
    }

    public void releaseSourceInternal() {
        super.releaseSourceInternal();
        this.clippingError = null;
        this.clippingTimeline = null;
    }

    /* access modifiers changed from: protected */
    public void onChildSourceInfoRefreshed(Void id, MediaSource mediaSource2, Timeline timeline, @Nullable Object manifest2) {
        if (this.clippingError == null) {
            this.manifest = manifest2;
            refreshClippedTimeline(timeline);
        }
    }

    private void refreshClippedTimeline(Timeline timeline) {
        long windowEndUs;
        long windowStartUs;
        timeline.getWindow(0, this.window);
        long windowPositionInPeriodUs = this.window.getPositionInFirstPeriodUs();
        long j = Long.MIN_VALUE;
        if (this.clippingTimeline == null || this.mediaPeriods.isEmpty() || this.allowDynamicClippingUpdates) {
            long windowStartUs2 = this.startUs;
            long windowEndUs2 = this.endUs;
            if (this.relativeToDefaultPosition) {
                long windowDefaultPositionUs = this.window.getDefaultPositionUs();
                windowStartUs2 += windowDefaultPositionUs;
                windowEndUs2 += windowDefaultPositionUs;
            }
            this.periodStartUs = windowPositionInPeriodUs + windowStartUs2;
            if (this.endUs != Long.MIN_VALUE) {
                j = windowPositionInPeriodUs + windowEndUs2;
            }
            this.periodEndUs = j;
            int count = this.mediaPeriods.size();
            for (int i = 0; i < count; i++) {
                ((ClippingMediaPeriod) this.mediaPeriods.get(i)).updateClipping(this.periodStartUs, this.periodEndUs);
            }
            windowStartUs = windowStartUs2;
            windowEndUs = windowEndUs2;
        } else {
            long windowStartUs3 = this.periodStartUs - windowPositionInPeriodUs;
            if (this.endUs != Long.MIN_VALUE) {
                j = this.periodEndUs - windowPositionInPeriodUs;
            }
            windowEndUs = j;
            windowStartUs = windowStartUs3;
        }
        try {
            ClippingTimeline clippingTimeline2 = new ClippingTimeline(timeline, windowStartUs, windowEndUs);
            this.clippingTimeline = clippingTimeline2;
            refreshSourceInfo(this.clippingTimeline, this.manifest);
        } catch (IllegalClippingException e) {
            this.clippingError = e;
        }
    }

    /* access modifiers changed from: protected */
    public long getMediaTimeForChildMediaTime(Void id, long mediaTimeMs) {
        if (mediaTimeMs == C.TIME_UNSET) {
            return C.TIME_UNSET;
        }
        long startMs = C.usToMs(this.startUs);
        long clippedTimeMs = Math.max(0, mediaTimeMs - startMs);
        long j = this.endUs;
        if (j != Long.MIN_VALUE) {
            clippedTimeMs = Math.min(C.usToMs(j) - startMs, clippedTimeMs);
        }
        return clippedTimeMs;
    }
}
