package com.google.android.exoplayer2.extractor;

import android.support.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.SeekMap.SeekPoints;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class BinarySearchSeeker {
    private static final long MAX_SKIP_BYTES = 262144;
    private final int minimumSearchRange;
    protected final BinarySearchSeekMap seekMap;
    @Nullable
    protected SeekOperationParams seekOperationParams;
    protected final TimestampSeeker timestampSeeker;

    public static class BinarySearchSeekMap implements SeekMap {
        /* access modifiers changed from: private */
        public final long approxBytesPerFrame;
        /* access modifiers changed from: private */
        public final long ceilingBytePosition;
        /* access modifiers changed from: private */
        public final long ceilingTimePosition;
        private final long durationUs;
        /* access modifiers changed from: private */
        public final long floorBytePosition;
        /* access modifiers changed from: private */
        public final long floorTimePosition;
        private final SeekTimestampConverter seekTimestampConverter;

        public BinarySearchSeekMap(SeekTimestampConverter seekTimestampConverter2, long durationUs2, long floorTimePosition2, long ceilingTimePosition2, long floorBytePosition2, long ceilingBytePosition2, long approxBytesPerFrame2) {
            this.seekTimestampConverter = seekTimestampConverter2;
            this.durationUs = durationUs2;
            this.floorTimePosition = floorTimePosition2;
            this.ceilingTimePosition = ceilingTimePosition2;
            this.floorBytePosition = floorBytePosition2;
            this.ceilingBytePosition = ceilingBytePosition2;
            this.approxBytesPerFrame = approxBytesPerFrame2;
        }

        public boolean isSeekable() {
            return true;
        }

        public SeekPoints getSeekPoints(long timeUs) {
            return new SeekPoints(new SeekPoint(timeUs, SeekOperationParams.calculateNextSearchBytePosition(this.seekTimestampConverter.timeUsToTargetTime(timeUs), this.floorTimePosition, this.ceilingTimePosition, this.floorBytePosition, this.ceilingBytePosition, this.approxBytesPerFrame)));
        }

        public long getDurationUs() {
            return this.durationUs;
        }

        public long timeUsToTargetTime(long timeUs) {
            return this.seekTimestampConverter.timeUsToTargetTime(timeUs);
        }
    }

    public static final class DefaultSeekTimestampConverter implements SeekTimestampConverter {
        public long timeUsToTargetTime(long timeUs) {
            return timeUs;
        }
    }

    public static final class OutputFrameHolder {
        public ByteBuffer byteBuffer;
        public long timeUs = 0;

        public OutputFrameHolder(ByteBuffer outputByteBuffer) {
            this.byteBuffer = outputByteBuffer;
        }
    }

    protected static class SeekOperationParams {
        private final long approxBytesPerFrame;
        private long ceilingBytePosition;
        private long ceilingTimePosition;
        private long floorBytePosition;
        private long floorTimePosition;
        private long nextSearchBytePosition;
        private final long seekTimeUs;
        private final long targetTimePosition;

        protected static long calculateNextSearchBytePosition(long targetTimePosition2, long floorTimePosition2, long ceilingTimePosition2, long floorBytePosition2, long ceilingBytePosition2, long approxBytesPerFrame2) {
            if (floorBytePosition2 + 1 >= ceilingBytePosition2 || floorTimePosition2 + 1 >= ceilingTimePosition2) {
                return floorBytePosition2;
            }
            long bytesToSkip = (long) (((float) (targetTimePosition2 - floorTimePosition2)) * (((float) (ceilingBytePosition2 - floorBytePosition2)) / ((float) (ceilingTimePosition2 - floorTimePosition2))));
            return Util.constrainValue(((floorBytePosition2 + bytesToSkip) - approxBytesPerFrame2) - (bytesToSkip / 20), floorBytePosition2, ceilingBytePosition2 - 1);
        }

        protected SeekOperationParams(long seekTimeUs2, long targetTimePosition2, long floorTimePosition2, long ceilingTimePosition2, long floorBytePosition2, long ceilingBytePosition2, long approxBytesPerFrame2) {
            this.seekTimeUs = seekTimeUs2;
            this.targetTimePosition = targetTimePosition2;
            this.floorTimePosition = floorTimePosition2;
            this.ceilingTimePosition = ceilingTimePosition2;
            this.floorBytePosition = floorBytePosition2;
            this.ceilingBytePosition = ceilingBytePosition2;
            this.approxBytesPerFrame = approxBytesPerFrame2;
            this.nextSearchBytePosition = calculateNextSearchBytePosition(targetTimePosition2, floorTimePosition2, ceilingTimePosition2, floorBytePosition2, ceilingBytePosition2, approxBytesPerFrame2);
        }

        /* access modifiers changed from: private */
        public long getFloorBytePosition() {
            return this.floorBytePosition;
        }

        /* access modifiers changed from: private */
        public long getCeilingBytePosition() {
            return this.ceilingBytePosition;
        }

        /* access modifiers changed from: private */
        public long getTargetTimePosition() {
            return this.targetTimePosition;
        }

        /* access modifiers changed from: private */
        public long getSeekTimeUs() {
            return this.seekTimeUs;
        }

        /* access modifiers changed from: private */
        public void updateSeekFloor(long floorTimePosition2, long floorBytePosition2) {
            this.floorTimePosition = floorTimePosition2;
            this.floorBytePosition = floorBytePosition2;
            updateNextSearchBytePosition();
        }

        /* access modifiers changed from: private */
        public void updateSeekCeiling(long ceilingTimePosition2, long ceilingBytePosition2) {
            this.ceilingTimePosition = ceilingTimePosition2;
            this.ceilingBytePosition = ceilingBytePosition2;
            updateNextSearchBytePosition();
        }

        /* access modifiers changed from: private */
        public long getNextSearchBytePosition() {
            return this.nextSearchBytePosition;
        }

        private void updateNextSearchBytePosition() {
            this.nextSearchBytePosition = calculateNextSearchBytePosition(this.targetTimePosition, this.floorTimePosition, this.ceilingTimePosition, this.floorBytePosition, this.ceilingBytePosition, this.approxBytesPerFrame);
        }
    }

    protected interface SeekTimestampConverter {
        long timeUsToTargetTime(long j);
    }

    public static final class TimestampSearchResult {
        public static final TimestampSearchResult NO_TIMESTAMP_IN_RANGE_RESULT;
        public static final int RESULT_NO_TIMESTAMP = -3;
        public static final int RESULT_POSITION_OVERESTIMATED = -1;
        public static final int RESULT_POSITION_UNDERESTIMATED = -2;
        public static final int RESULT_TARGET_TIMESTAMP_FOUND = 0;
        /* access modifiers changed from: private */
        public final long bytePositionToUpdate;
        /* access modifiers changed from: private */
        public final int result;
        /* access modifiers changed from: private */
        public final long timestampToUpdate;

        static {
            TimestampSearchResult timestampSearchResult = new TimestampSearchResult(-3, C.TIME_UNSET, -1);
            NO_TIMESTAMP_IN_RANGE_RESULT = timestampSearchResult;
        }

        private TimestampSearchResult(int result2, long timestampToUpdate2, long bytePositionToUpdate2) {
            this.result = result2;
            this.timestampToUpdate = timestampToUpdate2;
            this.bytePositionToUpdate = bytePositionToUpdate2;
        }

        public static TimestampSearchResult overestimatedResult(long newCeilingTimestamp, long newCeilingBytePosition) {
            TimestampSearchResult timestampSearchResult = new TimestampSearchResult(-1, newCeilingTimestamp, newCeilingBytePosition);
            return timestampSearchResult;
        }

        public static TimestampSearchResult underestimatedResult(long newFloorTimestamp, long newCeilingBytePosition) {
            TimestampSearchResult timestampSearchResult = new TimestampSearchResult(-2, newFloorTimestamp, newCeilingBytePosition);
            return timestampSearchResult;
        }

        public static TimestampSearchResult targetFoundResult(long resultBytePosition) {
            TimestampSearchResult timestampSearchResult = new TimestampSearchResult(0, C.TIME_UNSET, resultBytePosition);
            return timestampSearchResult;
        }
    }

    protected interface TimestampSeeker {
        TimestampSearchResult searchForTimestamp(ExtractorInput extractorInput, long j, OutputFrameHolder outputFrameHolder) throws IOException, InterruptedException;
    }

    protected BinarySearchSeeker(SeekTimestampConverter seekTimestampConverter, TimestampSeeker timestampSeeker2, long durationUs, long floorTimePosition, long ceilingTimePosition, long floorBytePosition, long ceilingBytePosition, long approxBytesPerFrame, int minimumSearchRange2) {
        this.timestampSeeker = timestampSeeker2;
        this.minimumSearchRange = minimumSearchRange2;
        BinarySearchSeekMap binarySearchSeekMap = r3;
        BinarySearchSeekMap binarySearchSeekMap2 = new BinarySearchSeekMap(seekTimestampConverter, durationUs, floorTimePosition, ceilingTimePosition, floorBytePosition, ceilingBytePosition, approxBytesPerFrame);
        this.seekMap = binarySearchSeekMap;
    }

    public final SeekMap getSeekMap() {
        return this.seekMap;
    }

    public final void setSeekTargetUs(long timeUs) {
        SeekOperationParams seekOperationParams2 = this.seekOperationParams;
        if (seekOperationParams2 == null || seekOperationParams2.getSeekTimeUs() != timeUs) {
            this.seekOperationParams = createSeekParamsForTargetTimeUs(timeUs);
        }
    }

    public final boolean isSeeking() {
        return this.seekOperationParams != null;
    }

    public int handlePendingSeek(ExtractorInput input, PositionHolder seekPositionHolder, OutputFrameHolder outputFrameHolder) throws InterruptedException, IOException {
        ExtractorInput extractorInput = input;
        PositionHolder positionHolder = seekPositionHolder;
        TimestampSeeker timestampSeeker2 = (TimestampSeeker) Assertions.checkNotNull(this.timestampSeeker);
        while (true) {
            SeekOperationParams seekOperationParams2 = (SeekOperationParams) Assertions.checkNotNull(this.seekOperationParams);
            long floorPosition = seekOperationParams2.getFloorBytePosition();
            long ceilingPosition = seekOperationParams2.getCeilingBytePosition();
            long searchPosition = seekOperationParams2.getNextSearchBytePosition();
            if (ceilingPosition - floorPosition <= ((long) this.minimumSearchRange)) {
                markSeekOperationFinished(false, floorPosition);
                return seekToPosition(extractorInput, floorPosition, positionHolder);
            } else if (!skipInputUntilPosition(extractorInput, searchPosition)) {
                return seekToPosition(extractorInput, searchPosition, positionHolder);
            } else {
                input.resetPeekPosition();
                TimestampSearchResult timestampSearchResult = timestampSeeker2.searchForTimestamp(extractorInput, seekOperationParams2.getTargetTimePosition(), outputFrameHolder);
                switch (timestampSearchResult.result) {
                    case -3:
                        long j = floorPosition;
                        markSeekOperationFinished(false, searchPosition);
                        return seekToPosition(extractorInput, searchPosition, positionHolder);
                    case -2:
                        seekOperationParams2.updateSeekFloor(timestampSearchResult.timestampToUpdate, timestampSearchResult.bytePositionToUpdate);
                        break;
                    case -1:
                        long j2 = floorPosition;
                        seekOperationParams2.updateSeekCeiling(timestampSearchResult.timestampToUpdate, timestampSearchResult.bytePositionToUpdate);
                        break;
                    case 0:
                        markSeekOperationFinished(true, timestampSearchResult.bytePositionToUpdate);
                        skipInputUntilPosition(extractorInput, timestampSearchResult.bytePositionToUpdate);
                        return seekToPosition(extractorInput, timestampSearchResult.bytePositionToUpdate, positionHolder);
                    default:
                        long j3 = floorPosition;
                        throw new IllegalStateException("Invalid case");
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public SeekOperationParams createSeekParamsForTargetTimeUs(long timeUs) {
        SeekOperationParams seekOperationParams2 = new SeekOperationParams(timeUs, this.seekMap.timeUsToTargetTime(timeUs), this.seekMap.floorTimePosition, this.seekMap.ceilingTimePosition, this.seekMap.floorBytePosition, this.seekMap.ceilingBytePosition, this.seekMap.approxBytesPerFrame);
        return seekOperationParams2;
    }

    /* access modifiers changed from: protected */
    public final void markSeekOperationFinished(boolean foundTargetFrame, long resultPosition) {
        this.seekOperationParams = null;
        onSeekOperationFinished(foundTargetFrame, resultPosition);
    }

    /* access modifiers changed from: protected */
    public void onSeekOperationFinished(boolean foundTargetFrame, long resultPosition) {
    }

    /* access modifiers changed from: protected */
    public final boolean skipInputUntilPosition(ExtractorInput input, long position) throws IOException, InterruptedException {
        long bytesToSkip = position - input.getPosition();
        if (bytesToSkip < 0 || bytesToSkip > 262144) {
            return false;
        }
        input.skipFully((int) bytesToSkip);
        return true;
    }

    /* access modifiers changed from: protected */
    public final int seekToPosition(ExtractorInput input, long position, PositionHolder seekPositionHolder) {
        if (position == input.getPosition()) {
            return 0;
        }
        seekPositionHolder.position = position;
        return 1;
    }
}
