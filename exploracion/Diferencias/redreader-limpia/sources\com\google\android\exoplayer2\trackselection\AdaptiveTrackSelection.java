package com.google.android.exoplayer2.trackselection;

import android.support.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.chunk.MediaChunk;
import com.google.android.exoplayer2.source.chunk.MediaChunkIterator;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.Util;
import java.util.List;

public class AdaptiveTrackSelection extends BaseTrackSelection {
    public static final float DEFAULT_BANDWIDTH_FRACTION = 0.75f;
    public static final float DEFAULT_BUFFERED_FRACTION_TO_LIVE_EDGE_FOR_QUALITY_INCREASE = 0.75f;
    public static final int DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS = 25000;
    public static final int DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS = 10000;
    public static final int DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS = 25000;
    public static final long DEFAULT_MIN_TIME_BETWEEN_BUFFER_REEVALUTATION_MS = 2000;
    private final float bandwidthFraction;
    private final BandwidthMeter bandwidthMeter;
    private final float bufferedFractionToLiveEdgeForQualityIncrease;
    private final Clock clock;
    private long lastBufferEvaluationMs;
    private final long maxDurationForQualityDecreaseUs;
    private final long minDurationForQualityIncreaseUs;
    private final long minDurationToRetainAfterDiscardUs;
    private final long minTimeBetweenBufferReevaluationMs;
    private float playbackSpeed;
    private int reason;
    private int selectedIndex;

    public static final class Factory implements com.google.android.exoplayer2.trackselection.TrackSelection.Factory {
        private final float bandwidthFraction;
        @Nullable
        private final BandwidthMeter bandwidthMeter;
        private final float bufferedFractionToLiveEdgeForQualityIncrease;
        private final Clock clock;
        private final int maxDurationForQualityDecreaseMs;
        private final int minDurationForQualityIncreaseMs;
        private final int minDurationToRetainAfterDiscardMs;
        private final long minTimeBetweenBufferReevaluationMs;

        public Factory() {
            this(10000, 25000, 25000, 0.75f, 0.75f, AdaptiveTrackSelection.DEFAULT_MIN_TIME_BETWEEN_BUFFER_REEVALUTATION_MS, Clock.DEFAULT);
        }

        @Deprecated
        public Factory(BandwidthMeter bandwidthMeter2) {
            this(bandwidthMeter2, 10000, 25000, 25000, 0.75f, 0.75f, AdaptiveTrackSelection.DEFAULT_MIN_TIME_BETWEEN_BUFFER_REEVALUTATION_MS, Clock.DEFAULT);
        }

        public Factory(int minDurationForQualityIncreaseMs2, int maxDurationForQualityDecreaseMs2, int minDurationToRetainAfterDiscardMs2, float bandwidthFraction2) {
            this(minDurationForQualityIncreaseMs2, maxDurationForQualityDecreaseMs2, minDurationToRetainAfterDiscardMs2, bandwidthFraction2, 0.75f, AdaptiveTrackSelection.DEFAULT_MIN_TIME_BETWEEN_BUFFER_REEVALUTATION_MS, Clock.DEFAULT);
        }

        @Deprecated
        public Factory(BandwidthMeter bandwidthMeter2, int minDurationForQualityIncreaseMs2, int maxDurationForQualityDecreaseMs2, int minDurationToRetainAfterDiscardMs2, float bandwidthFraction2) {
            this(bandwidthMeter2, minDurationForQualityIncreaseMs2, maxDurationForQualityDecreaseMs2, minDurationToRetainAfterDiscardMs2, bandwidthFraction2, 0.75f, AdaptiveTrackSelection.DEFAULT_MIN_TIME_BETWEEN_BUFFER_REEVALUTATION_MS, Clock.DEFAULT);
        }

        public Factory(int minDurationForQualityIncreaseMs2, int maxDurationForQualityDecreaseMs2, int minDurationToRetainAfterDiscardMs2, float bandwidthFraction2, float bufferedFractionToLiveEdgeForQualityIncrease2, long minTimeBetweenBufferReevaluationMs2, Clock clock2) {
            this(null, minDurationForQualityIncreaseMs2, maxDurationForQualityDecreaseMs2, minDurationToRetainAfterDiscardMs2, bandwidthFraction2, bufferedFractionToLiveEdgeForQualityIncrease2, minTimeBetweenBufferReevaluationMs2, clock2);
        }

        @Deprecated
        public Factory(@Nullable BandwidthMeter bandwidthMeter2, int minDurationForQualityIncreaseMs2, int maxDurationForQualityDecreaseMs2, int minDurationToRetainAfterDiscardMs2, float bandwidthFraction2, float bufferedFractionToLiveEdgeForQualityIncrease2, long minTimeBetweenBufferReevaluationMs2, Clock clock2) {
            this.bandwidthMeter = bandwidthMeter2;
            this.minDurationForQualityIncreaseMs = minDurationForQualityIncreaseMs2;
            this.maxDurationForQualityDecreaseMs = maxDurationForQualityDecreaseMs2;
            this.minDurationToRetainAfterDiscardMs = minDurationToRetainAfterDiscardMs2;
            this.bandwidthFraction = bandwidthFraction2;
            this.bufferedFractionToLiveEdgeForQualityIncrease = bufferedFractionToLiveEdgeForQualityIncrease2;
            this.minTimeBetweenBufferReevaluationMs = minTimeBetweenBufferReevaluationMs2;
            this.clock = clock2;
        }

        public AdaptiveTrackSelection createTrackSelection(TrackGroup group, BandwidthMeter bandwidthMeter2, int... tracks) {
            BandwidthMeter bandwidthMeter3;
            if (this.bandwidthMeter != null) {
                bandwidthMeter3 = this.bandwidthMeter;
            } else {
                bandwidthMeter3 = bandwidthMeter2;
            }
            AdaptiveTrackSelection adaptiveTrackSelection = new AdaptiveTrackSelection(group, tracks, bandwidthMeter3, (long) this.minDurationForQualityIncreaseMs, (long) this.maxDurationForQualityDecreaseMs, (long) this.minDurationToRetainAfterDiscardMs, this.bandwidthFraction, this.bufferedFractionToLiveEdgeForQualityIncrease, this.minTimeBetweenBufferReevaluationMs, this.clock);
            return adaptiveTrackSelection;
        }
    }

    public AdaptiveTrackSelection(TrackGroup group, int[] tracks, BandwidthMeter bandwidthMeter2) {
        this(group, tracks, bandwidthMeter2, 10000, 25000, 25000, 0.75f, 0.75f, DEFAULT_MIN_TIME_BETWEEN_BUFFER_REEVALUTATION_MS, Clock.DEFAULT);
    }

    public AdaptiveTrackSelection(TrackGroup group, int[] tracks, BandwidthMeter bandwidthMeter2, long minDurationForQualityIncreaseMs, long maxDurationForQualityDecreaseMs, long minDurationToRetainAfterDiscardMs, float bandwidthFraction2, float bufferedFractionToLiveEdgeForQualityIncrease2, long minTimeBetweenBufferReevaluationMs2, Clock clock2) {
        super(group, tracks);
        this.bandwidthMeter = bandwidthMeter2;
        this.minDurationForQualityIncreaseUs = minDurationForQualityIncreaseMs * 1000;
        this.maxDurationForQualityDecreaseUs = maxDurationForQualityDecreaseMs * 1000;
        this.minDurationToRetainAfterDiscardUs = 1000 * minDurationToRetainAfterDiscardMs;
        this.bandwidthFraction = bandwidthFraction2;
        this.bufferedFractionToLiveEdgeForQualityIncrease = bufferedFractionToLiveEdgeForQualityIncrease2;
        this.minTimeBetweenBufferReevaluationMs = minTimeBetweenBufferReevaluationMs2;
        this.clock = clock2;
        this.playbackSpeed = 1.0f;
        this.reason = 1;
        this.lastBufferEvaluationMs = C.TIME_UNSET;
        this.selectedIndex = determineIdealSelectedIndex(Long.MIN_VALUE);
    }

    public void enable() {
        this.lastBufferEvaluationMs = C.TIME_UNSET;
    }

    public void onPlaybackSpeed(float playbackSpeed2) {
        this.playbackSpeed = playbackSpeed2;
    }

    public void updateSelectedTrack(long playbackPositionUs, long bufferedDurationUs, long availableDurationUs, List<? extends MediaChunk> list, MediaChunkIterator[] mediaChunkIterators) {
        long nowMs = this.clock.elapsedRealtime();
        int currentSelectedIndex = this.selectedIndex;
        this.selectedIndex = determineIdealSelectedIndex(nowMs);
        if (this.selectedIndex != currentSelectedIndex) {
            if (!isBlacklisted(currentSelectedIndex, nowMs)) {
                Format currentFormat = getFormat(currentSelectedIndex);
                Format selectedFormat = getFormat(this.selectedIndex);
                if (selectedFormat.bitrate <= currentFormat.bitrate) {
                    long j = availableDurationUs;
                } else if (bufferedDurationUs < minDurationForQualityIncreaseUs(availableDurationUs)) {
                    this.selectedIndex = currentSelectedIndex;
                }
                if (selectedFormat.bitrate < currentFormat.bitrate && bufferedDurationUs >= this.maxDurationForQualityDecreaseUs) {
                    this.selectedIndex = currentSelectedIndex;
                }
            } else {
                long j2 = availableDurationUs;
            }
            if (this.selectedIndex != currentSelectedIndex) {
                this.reason = 3;
            }
        }
    }

    public int getSelectedIndex() {
        return this.selectedIndex;
    }

    public int getSelectionReason() {
        return this.reason;
    }

    @Nullable
    public Object getSelectionData() {
        return null;
    }

    public int evaluateQueueSize(long playbackPositionUs, List<? extends MediaChunk> queue) {
        List<? extends MediaChunk> list = queue;
        long nowMs = this.clock.elapsedRealtime();
        long j = this.lastBufferEvaluationMs;
        if (j != C.TIME_UNSET && nowMs - j < this.minTimeBetweenBufferReevaluationMs) {
            return queue.size();
        }
        this.lastBufferEvaluationMs = nowMs;
        if (queue.isEmpty()) {
            return 0;
        }
        int queueSize = queue.size();
        if (Util.getPlayoutDurationForMediaDuration(((MediaChunk) list.get(queueSize - 1)).startTimeUs - playbackPositionUs, this.playbackSpeed) < this.minDurationToRetainAfterDiscardUs) {
            return queueSize;
        }
        Format idealFormat = getFormat(determineIdealSelectedIndex(nowMs));
        int i = 0;
        while (i < queueSize) {
            MediaChunk chunk = (MediaChunk) list.get(i);
            Format format = chunk.trackFormat;
            long nowMs2 = nowMs;
            if (Util.getPlayoutDurationForMediaDuration(chunk.startTimeUs - playbackPositionUs, this.playbackSpeed) >= this.minDurationToRetainAfterDiscardUs && format.bitrate < idealFormat.bitrate && format.height != -1 && format.height < 720 && format.width != -1 && format.width < 1280 && format.height < idealFormat.height) {
                return i;
            }
            i++;
            nowMs = nowMs2;
            list = queue;
        }
        return queueSize;
    }

    private int determineIdealSelectedIndex(long nowMs) {
        long effectiveBitrate = (long) (((float) this.bandwidthMeter.getBitrateEstimate()) * this.bandwidthFraction);
        int lowestBitrateNonBlacklistedIndex = 0;
        for (int i = 0; i < this.length; i++) {
            if (nowMs == Long.MIN_VALUE || !isBlacklisted(i, nowMs)) {
                if (((long) Math.round(((float) getFormat(i).bitrate) * this.playbackSpeed)) <= effectiveBitrate) {
                    return i;
                }
                lowestBitrateNonBlacklistedIndex = i;
            }
        }
        return lowestBitrateNonBlacklistedIndex;
    }

    private long minDurationForQualityIncreaseUs(long availableDurationUs) {
        return (availableDurationUs > C.TIME_UNSET ? 1 : (availableDurationUs == C.TIME_UNSET ? 0 : -1)) != 0 && (availableDurationUs > this.minDurationForQualityIncreaseUs ? 1 : (availableDurationUs == this.minDurationForQualityIncreaseUs ? 0 : -1)) <= 0 ? (long) (((float) availableDurationUs) * this.bufferedFractionToLiveEdgeForQualityIncrease) : this.minDurationForQualityIncreaseUs;
    }
}
