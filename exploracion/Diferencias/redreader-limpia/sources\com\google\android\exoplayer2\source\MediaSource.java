package com.google.android.exoplayer2.source;

import android.os.Handler;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.TransferListener;
import java.io.IOException;

public interface MediaSource {

    public static final class MediaPeriodId {
        public final int adGroupIndex;
        public final int adIndexInAdGroup;
        public final long endPositionUs;
        public final Object periodUid;
        public final long windowSequenceNumber;

        public MediaPeriodId(Object periodUid2) {
            this(periodUid2, -1);
        }

        public MediaPeriodId(Object periodUid2, long windowSequenceNumber2) {
            this(periodUid2, -1, -1, windowSequenceNumber2, Long.MIN_VALUE);
        }

        public MediaPeriodId(Object periodUid2, long windowSequenceNumber2, long endPositionUs2) {
            this(periodUid2, -1, -1, windowSequenceNumber2, endPositionUs2);
        }

        public MediaPeriodId(Object periodUid2, int adGroupIndex2, int adIndexInAdGroup2, long windowSequenceNumber2) {
            this(periodUid2, adGroupIndex2, adIndexInAdGroup2, windowSequenceNumber2, Long.MIN_VALUE);
        }

        private MediaPeriodId(Object periodUid2, int adGroupIndex2, int adIndexInAdGroup2, long windowSequenceNumber2, long endPositionUs2) {
            this.periodUid = periodUid2;
            this.adGroupIndex = adGroupIndex2;
            this.adIndexInAdGroup = adIndexInAdGroup2;
            this.windowSequenceNumber = windowSequenceNumber2;
            this.endPositionUs = endPositionUs2;
        }

        public MediaPeriodId copyWithPeriodUid(Object newPeriodUid) {
            if (this.periodUid.equals(newPeriodUid)) {
                return this;
            }
            MediaPeriodId mediaPeriodId = new MediaPeriodId(newPeriodUid, this.adGroupIndex, this.adIndexInAdGroup, this.windowSequenceNumber, this.endPositionUs);
            return mediaPeriodId;
        }

        public boolean isAd() {
            return this.adGroupIndex != -1;
        }

        public boolean equals(@Nullable Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            MediaPeriodId periodId = (MediaPeriodId) obj;
            if (!(this.periodUid.equals(periodId.periodUid) && this.adGroupIndex == periodId.adGroupIndex && this.adIndexInAdGroup == periodId.adIndexInAdGroup && this.windowSequenceNumber == periodId.windowSequenceNumber && this.endPositionUs == periodId.endPositionUs)) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            return (((((((((17 * 31) + this.periodUid.hashCode()) * 31) + this.adGroupIndex) * 31) + this.adIndexInAdGroup) * 31) + ((int) this.windowSequenceNumber)) * 31) + ((int) this.endPositionUs);
        }
    }

    public interface SourceInfoRefreshListener {
        void onSourceInfoRefreshed(MediaSource mediaSource, Timeline timeline, @Nullable Object obj);
    }

    void addEventListener(Handler handler, MediaSourceEventListener mediaSourceEventListener);

    MediaPeriod createPeriod(MediaPeriodId mediaPeriodId, Allocator allocator);

    void maybeThrowSourceInfoRefreshError() throws IOException;

    @Deprecated
    void prepareSource(ExoPlayer exoPlayer, boolean z, SourceInfoRefreshListener sourceInfoRefreshListener);

    void prepareSource(ExoPlayer exoPlayer, boolean z, SourceInfoRefreshListener sourceInfoRefreshListener, @Nullable TransferListener transferListener);

    void releasePeriod(MediaPeriod mediaPeriod);

    void releaseSource(SourceInfoRefreshListener sourceInfoRefreshListener);

    void removeEventListener(MediaSourceEventListener mediaSourceEventListener);
}
