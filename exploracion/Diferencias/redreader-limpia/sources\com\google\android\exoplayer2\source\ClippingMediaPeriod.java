package com.google.android.exoplayer2.source;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.source.MediaPeriod.Callback;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;

public final class ClippingMediaPeriod implements MediaPeriod, Callback {
    private Callback callback;
    long endUs;
    public final MediaPeriod mediaPeriod;
    private long pendingInitialDiscontinuityPositionUs;
    private ClippingSampleStream[] sampleStreams = new ClippingSampleStream[0];
    long startUs;

    private final class ClippingSampleStream implements SampleStream {
        public final SampleStream childStream;
        private boolean sentEos;

        public ClippingSampleStream(SampleStream childStream2) {
            this.childStream = childStream2;
        }

        public void clearSentEos() {
            this.sentEos = false;
        }

        public boolean isReady() {
            return !ClippingMediaPeriod.this.isPendingInitialDiscontinuity() && this.childStream.isReady();
        }

        public void maybeThrowError() throws IOException {
            this.childStream.maybeThrowError();
        }

        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, boolean requireFormat) {
            if (ClippingMediaPeriod.this.isPendingInitialDiscontinuity()) {
                return -3;
            }
            if (this.sentEos) {
                buffer.setFlags(4);
                return -4;
            }
            int result = this.childStream.readData(formatHolder, buffer, requireFormat);
            if (result == -5) {
                Format format = formatHolder.format;
                if (!(format.encoderDelay == 0 && format.encoderPadding == 0)) {
                    int i = 0;
                    int encoderDelay = ClippingMediaPeriod.this.startUs != 0 ? 0 : format.encoderDelay;
                    if (ClippingMediaPeriod.this.endUs == Long.MIN_VALUE) {
                        i = format.encoderPadding;
                    }
                    formatHolder.format = format.copyWithGaplessInfo(encoderDelay, i);
                }
                return -5;
            } else if (ClippingMediaPeriod.this.endUs == Long.MIN_VALUE || ((result != -4 || buffer.timeUs < ClippingMediaPeriod.this.endUs) && (result != -3 || ClippingMediaPeriod.this.getBufferedPositionUs() != Long.MIN_VALUE))) {
                return result;
            } else {
                buffer.clear();
                buffer.setFlags(4);
                this.sentEos = true;
                return -4;
            }
        }

        public int skipData(long positionUs) {
            if (ClippingMediaPeriod.this.isPendingInitialDiscontinuity()) {
                return -3;
            }
            return this.childStream.skipData(positionUs);
        }
    }

    public ClippingMediaPeriod(MediaPeriod mediaPeriod2, boolean enableInitialDiscontinuity, long startUs2, long endUs2) {
        this.mediaPeriod = mediaPeriod2;
        this.pendingInitialDiscontinuityPositionUs = enableInitialDiscontinuity ? startUs2 : C.TIME_UNSET;
        this.startUs = startUs2;
        this.endUs = endUs2;
    }

    public void updateClipping(long startUs2, long endUs2) {
        this.startUs = startUs2;
        this.endUs = endUs2;
    }

    public void prepare(Callback callback2, long positionUs) {
        this.callback = callback2;
        this.mediaPeriod.prepare(this, positionUs);
    }

    public void maybeThrowPrepareError() throws IOException {
        this.mediaPeriod.maybeThrowPrepareError();
    }

    public TrackGroupArray getTrackGroups() {
        return this.mediaPeriod.getTrackGroups();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:22:0x005d, code lost:
        if (r1 > r3) goto L_0x0060;
     */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x004d  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x006a  */
    public long selectTracks(TrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        long j;
        boolean z;
        int i;
        this.sampleStreams = new ClippingSampleStream[streams.length];
        SampleStream[] childStreams = new SampleStream[streams.length];
        int i2 = 0;
        while (true) {
            SampleStream sampleStream = null;
            if (i2 >= streams.length) {
                break;
            }
            ClippingSampleStream[] clippingSampleStreamArr = this.sampleStreams;
            clippingSampleStreamArr[i2] = streams[i2];
            if (clippingSampleStreamArr[i2] != null) {
                sampleStream = clippingSampleStreamArr[i2].childStream;
            }
            childStreams[i2] = sampleStream;
            i2++;
        }
        long enablePositionUs = this.mediaPeriod.selectTracks(selections, mayRetainStreamFlags, childStreams, streamResetFlags, positionUs);
        if (isPendingInitialDiscontinuity()) {
            long j2 = this.startUs;
            if (positionUs == j2 && shouldKeepInitialDiscontinuity(j2, selections)) {
                j = enablePositionUs;
                this.pendingInitialDiscontinuityPositionUs = j;
                if (enablePositionUs != positionUs) {
                    if (enablePositionUs >= this.startUs) {
                        long j3 = this.endUs;
                        if (j3 != Long.MIN_VALUE) {
                        }
                    }
                    z = false;
                    Assertions.checkState(z);
                    for (i = 0; i < streams.length; i++) {
                        if (childStreams[i] == null) {
                            this.sampleStreams[i] = null;
                        } else if (streams[i] == null || this.sampleStreams[i].childStream != childStreams[i]) {
                            this.sampleStreams[i] = new ClippingSampleStream(childStreams[i]);
                        }
                        streams[i] = this.sampleStreams[i];
                    }
                    return enablePositionUs;
                }
                z = true;
                Assertions.checkState(z);
                while (i < streams.length) {
                }
                return enablePositionUs;
            }
        }
        j = C.TIME_UNSET;
        this.pendingInitialDiscontinuityPositionUs = j;
        if (enablePositionUs != positionUs) {
        }
        z = true;
        Assertions.checkState(z);
        while (i < streams.length) {
        }
        return enablePositionUs;
    }

    public void discardBuffer(long positionUs, boolean toKeyframe) {
        this.mediaPeriod.discardBuffer(positionUs, toKeyframe);
    }

    public void reevaluateBuffer(long positionUs) {
        this.mediaPeriod.reevaluateBuffer(positionUs);
    }

    public long readDiscontinuity() {
        if (isPendingInitialDiscontinuity()) {
            long initialDiscontinuityUs = this.pendingInitialDiscontinuityPositionUs;
            this.pendingInitialDiscontinuityPositionUs = C.TIME_UNSET;
            long childDiscontinuityUs = readDiscontinuity();
            return childDiscontinuityUs != C.TIME_UNSET ? childDiscontinuityUs : initialDiscontinuityUs;
        }
        long discontinuityUs = this.mediaPeriod.readDiscontinuity();
        if (discontinuityUs == C.TIME_UNSET) {
            return C.TIME_UNSET;
        }
        boolean z = true;
        Assertions.checkState(discontinuityUs >= this.startUs);
        long j = this.endUs;
        if (j != Long.MIN_VALUE && discontinuityUs > j) {
            z = false;
        }
        Assertions.checkState(z);
        return discontinuityUs;
    }

    public long getBufferedPositionUs() {
        long bufferedPositionUs = this.mediaPeriod.getBufferedPositionUs();
        if (bufferedPositionUs != Long.MIN_VALUE) {
            long j = this.endUs;
            if (j == Long.MIN_VALUE || bufferedPositionUs < j) {
                return bufferedPositionUs;
            }
        }
        return Long.MIN_VALUE;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0032, code lost:
        if (r0 > r3) goto L_0x0035;
     */
    public long seekToUs(long positionUs) {
        ClippingSampleStream[] clippingSampleStreamArr;
        this.pendingInitialDiscontinuityPositionUs = C.TIME_UNSET;
        boolean z = false;
        for (ClippingSampleStream sampleStream : this.sampleStreams) {
            if (sampleStream != null) {
                sampleStream.clearSentEos();
            }
        }
        long seekUs = this.mediaPeriod.seekToUs(positionUs);
        if (seekUs != positionUs) {
            if (seekUs >= this.startUs) {
                long j = this.endUs;
                if (j != Long.MIN_VALUE) {
                }
            }
            Assertions.checkState(z);
            return seekUs;
        }
        z = true;
        Assertions.checkState(z);
        return seekUs;
    }

    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        long j = this.startUs;
        if (positionUs == j) {
            return j;
        }
        return this.mediaPeriod.getAdjustedSeekPositionUs(positionUs, clipSeekParameters(positionUs, seekParameters));
    }

    public long getNextLoadPositionUs() {
        long nextLoadPositionUs = this.mediaPeriod.getNextLoadPositionUs();
        if (nextLoadPositionUs != Long.MIN_VALUE) {
            long j = this.endUs;
            if (j == Long.MIN_VALUE || nextLoadPositionUs < j) {
                return nextLoadPositionUs;
            }
        }
        return Long.MIN_VALUE;
    }

    public boolean continueLoading(long positionUs) {
        return this.mediaPeriod.continueLoading(positionUs);
    }

    public void onPrepared(MediaPeriod mediaPeriod2) {
        this.callback.onPrepared(this);
    }

    public void onContinueLoadingRequested(MediaPeriod source) {
        this.callback.onContinueLoadingRequested(this);
    }

    /* access modifiers changed from: 0000 */
    public boolean isPendingInitialDiscontinuity() {
        return this.pendingInitialDiscontinuityPositionUs != C.TIME_UNSET;
    }

    private SeekParameters clipSeekParameters(long positionUs, SeekParameters seekParameters) {
        long toleranceBeforeUs = Util.constrainValue(seekParameters.toleranceBeforeUs, 0, positionUs - this.startUs);
        long j = seekParameters.toleranceAfterUs;
        long j2 = this.endUs;
        long toleranceAfterUs = Util.constrainValue(j, 0, j2 == Long.MIN_VALUE ? Long.MAX_VALUE : j2 - positionUs);
        if (toleranceBeforeUs == seekParameters.toleranceBeforeUs && toleranceAfterUs == seekParameters.toleranceAfterUs) {
            return seekParameters;
        }
        return new SeekParameters(toleranceBeforeUs, toleranceAfterUs);
    }

    private static boolean shouldKeepInitialDiscontinuity(long startUs2, TrackSelection[] selections) {
        if (startUs2 != 0) {
            for (TrackSelection trackSelection : selections) {
                if (trackSelection != null && !MimeTypes.isAudio(trackSelection.getSelectedFormat().sampleMimeType)) {
                    return true;
                }
            }
        }
        return false;
    }
}
