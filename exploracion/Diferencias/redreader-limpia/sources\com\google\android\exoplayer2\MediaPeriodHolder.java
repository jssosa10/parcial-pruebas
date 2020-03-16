package com.google.android.exoplayer2;

import com.google.android.exoplayer2.source.ClippingMediaPeriod;
import com.google.android.exoplayer2.source.EmptySampleStream;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.SampleStream;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectorResult;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;

final class MediaPeriodHolder {
    private static final String TAG = "MediaPeriodHolder";
    public boolean hasEnabledTracks;

    /* renamed from: info reason: collision with root package name */
    public MediaPeriodInfo f18info;
    public final boolean[] mayRetainStreamFlags;
    public final MediaPeriod mediaPeriod;
    private final MediaSource mediaSource;
    public MediaPeriodHolder next;
    private TrackSelectorResult periodTrackSelectorResult;
    public boolean prepared;
    private final RendererCapabilities[] rendererCapabilities;
    private long rendererPositionOffsetUs;
    public final SampleStream[] sampleStreams;
    public TrackGroupArray trackGroups;
    private final TrackSelector trackSelector;
    public TrackSelectorResult trackSelectorResult;
    public final Object uid;

    public MediaPeriodHolder(RendererCapabilities[] rendererCapabilities2, long rendererPositionOffsetUs2, TrackSelector trackSelector2, Allocator allocator, MediaSource mediaSource2, MediaPeriodInfo info2) {
        RendererCapabilities[] rendererCapabilitiesArr = rendererCapabilities2;
        MediaSource mediaSource3 = mediaSource2;
        MediaPeriodInfo mediaPeriodInfo = info2;
        this.rendererCapabilities = rendererCapabilitiesArr;
        this.rendererPositionOffsetUs = rendererPositionOffsetUs2 - mediaPeriodInfo.startPositionUs;
        this.trackSelector = trackSelector2;
        this.mediaSource = mediaSource3;
        this.uid = Assertions.checkNotNull(mediaPeriodInfo.id.periodUid);
        this.f18info = mediaPeriodInfo;
        this.sampleStreams = new SampleStream[rendererCapabilitiesArr.length];
        this.mayRetainStreamFlags = new boolean[rendererCapabilitiesArr.length];
        MediaPeriod mediaPeriod2 = mediaSource3.createPeriod(mediaPeriodInfo.id, allocator);
        if (mediaPeriodInfo.id.endPositionUs != Long.MIN_VALUE) {
            ClippingMediaPeriod clippingMediaPeriod = new ClippingMediaPeriod(mediaPeriod2, true, 0, mediaPeriodInfo.id.endPositionUs);
            mediaPeriod2 = clippingMediaPeriod;
        }
        this.mediaPeriod = mediaPeriod2;
    }

    public long toRendererTime(long periodTimeUs) {
        return getRendererOffset() + periodTimeUs;
    }

    public long toPeriodTime(long rendererTimeUs) {
        return rendererTimeUs - getRendererOffset();
    }

    public long getRendererOffset() {
        return this.rendererPositionOffsetUs;
    }

    public long getStartPositionRendererTime() {
        return this.f18info.startPositionUs + this.rendererPositionOffsetUs;
    }

    public boolean isFullyBuffered() {
        return this.prepared && (!this.hasEnabledTracks || this.mediaPeriod.getBufferedPositionUs() == Long.MIN_VALUE);
    }

    public long getDurationUs() {
        return this.f18info.durationUs;
    }

    public long getBufferedPositionUs() {
        if (!this.prepared) {
            return this.f18info.startPositionUs;
        }
        long bufferedPositionUs = this.hasEnabledTracks ? this.mediaPeriod.getBufferedPositionUs() : Long.MIN_VALUE;
        return bufferedPositionUs == Long.MIN_VALUE ? this.f18info.durationUs : bufferedPositionUs;
    }

    public long getNextLoadPositionUs() {
        if (!this.prepared) {
            return 0;
        }
        return this.mediaPeriod.getNextLoadPositionUs();
    }

    public void handlePrepared(float playbackSpeed) throws ExoPlaybackException {
        this.prepared = true;
        this.trackGroups = this.mediaPeriod.getTrackGroups();
        selectTracks(playbackSpeed);
        long newStartPositionUs = applyTrackSelection(this.f18info.startPositionUs, false);
        this.rendererPositionOffsetUs += this.f18info.startPositionUs - newStartPositionUs;
        this.f18info = this.f18info.copyWithStartPositionUs(newStartPositionUs);
    }

    public void reevaluateBuffer(long rendererPositionUs) {
        if (this.prepared) {
            this.mediaPeriod.reevaluateBuffer(toPeriodTime(rendererPositionUs));
        }
    }

    public void continueLoading(long rendererPositionUs) {
        this.mediaPeriod.continueLoading(toPeriodTime(rendererPositionUs));
    }

    public boolean selectTracks(float playbackSpeed) throws ExoPlaybackException {
        TrackSelection[] all;
        TrackSelectorResult selectorResult = this.trackSelector.selectTracks(this.rendererCapabilities, this.trackGroups);
        if (selectorResult.isEquivalent(this.periodTrackSelectorResult)) {
            return false;
        }
        this.trackSelectorResult = selectorResult;
        for (TrackSelection trackSelection : this.trackSelectorResult.selections.getAll()) {
            if (trackSelection != null) {
                trackSelection.onPlaybackSpeed(playbackSpeed);
            }
        }
        return true;
    }

    public long applyTrackSelection(long positionUs, boolean forceRecreateStreams) {
        return applyTrackSelection(positionUs, forceRecreateStreams, new boolean[this.rendererCapabilities.length]);
    }

    public long applyTrackSelection(long positionUs, boolean forceRecreateStreams, boolean[] streamResetFlags) {
        int i = 0;
        while (true) {
            boolean z = false;
            if (i >= this.trackSelectorResult.length) {
                break;
            }
            boolean[] zArr = this.mayRetainStreamFlags;
            if (!forceRecreateStreams && this.trackSelectorResult.isEquivalent(this.periodTrackSelectorResult, i)) {
                z = true;
            }
            zArr[i] = z;
            i++;
        }
        disassociateNoSampleRenderersWithEmptySampleStream(this.sampleStreams);
        updatePeriodTrackSelectorResult(this.trackSelectorResult);
        TrackSelectionArray trackSelections = this.trackSelectorResult.selections;
        long positionUs2 = this.mediaPeriod.selectTracks(trackSelections.getAll(), this.mayRetainStreamFlags, this.sampleStreams, streamResetFlags, positionUs);
        associateNoSampleRenderersWithEmptySampleStream(this.sampleStreams);
        this.hasEnabledTracks = false;
        int i2 = 0;
        while (true) {
            SampleStream[] sampleStreamArr = this.sampleStreams;
            if (i2 >= sampleStreamArr.length) {
                return positionUs2;
            }
            if (sampleStreamArr[i2] != null) {
                Assertions.checkState(this.trackSelectorResult.isRendererEnabled(i2));
                if (this.rendererCapabilities[i2].getTrackType() != 6) {
                    this.hasEnabledTracks = true;
                }
            } else {
                Assertions.checkState(trackSelections.get(i2) == null);
            }
            i2++;
        }
    }

    public void release() {
        updatePeriodTrackSelectorResult(null);
        try {
            if (this.f18info.id.endPositionUs != Long.MIN_VALUE) {
                this.mediaSource.releasePeriod(((ClippingMediaPeriod) this.mediaPeriod).mediaPeriod);
            } else {
                this.mediaSource.releasePeriod(this.mediaPeriod);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Period release failed.", e);
        }
    }

    private void updatePeriodTrackSelectorResult(TrackSelectorResult trackSelectorResult2) {
        TrackSelectorResult trackSelectorResult3 = this.periodTrackSelectorResult;
        if (trackSelectorResult3 != null) {
            disableTrackSelectionsInResult(trackSelectorResult3);
        }
        this.periodTrackSelectorResult = trackSelectorResult2;
        TrackSelectorResult trackSelectorResult4 = this.periodTrackSelectorResult;
        if (trackSelectorResult4 != null) {
            enableTrackSelectionsInResult(trackSelectorResult4);
        }
    }

    private void enableTrackSelectionsInResult(TrackSelectorResult trackSelectorResult2) {
        for (int i = 0; i < trackSelectorResult2.length; i++) {
            boolean rendererEnabled = trackSelectorResult2.isRendererEnabled(i);
            TrackSelection trackSelection = trackSelectorResult2.selections.get(i);
            if (rendererEnabled && trackSelection != null) {
                trackSelection.enable();
            }
        }
    }

    private void disableTrackSelectionsInResult(TrackSelectorResult trackSelectorResult2) {
        for (int i = 0; i < trackSelectorResult2.length; i++) {
            boolean rendererEnabled = trackSelectorResult2.isRendererEnabled(i);
            TrackSelection trackSelection = trackSelectorResult2.selections.get(i);
            if (rendererEnabled && trackSelection != null) {
                trackSelection.disable();
            }
        }
    }

    private void disassociateNoSampleRenderersWithEmptySampleStream(SampleStream[] sampleStreams2) {
        int i = 0;
        while (true) {
            RendererCapabilities[] rendererCapabilitiesArr = this.rendererCapabilities;
            if (i < rendererCapabilitiesArr.length) {
                if (rendererCapabilitiesArr[i].getTrackType() == 6) {
                    sampleStreams2[i] = null;
                }
                i++;
            } else {
                return;
            }
        }
    }

    private void associateNoSampleRenderersWithEmptySampleStream(SampleStream[] sampleStreams2) {
        int i = 0;
        while (true) {
            RendererCapabilities[] rendererCapabilitiesArr = this.rendererCapabilities;
            if (i < rendererCapabilitiesArr.length) {
                if (rendererCapabilitiesArr[i].getTrackType() == 6 && this.trackSelectorResult.isRendererEnabled(i)) {
                    sampleStreams2[i] = new EmptySampleStream();
                }
                i++;
            } else {
                return;
            }
        }
    }
}
