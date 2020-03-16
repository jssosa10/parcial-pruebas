package com.google.android.exoplayer2.extractor.mp3;

import android.util.Pair;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.SeekMap.SeekPoints;
import com.google.android.exoplayer2.extractor.SeekPoint;
import com.google.android.exoplayer2.metadata.id3.MlltFrame;
import com.google.android.exoplayer2.util.Util;

final class MlltSeeker implements Seeker {
    private final long durationUs;
    private final long[] referencePositions;
    private final long[] referenceTimesMs;

    public static MlltSeeker create(long firstFramePosition, MlltFrame mlltFrame) {
        int referenceCount = mlltFrame.bytesDeviations.length;
        long[] referencePositions2 = new long[(referenceCount + 1)];
        long[] referenceTimesMs2 = new long[(referenceCount + 1)];
        referencePositions2[0] = firstFramePosition;
        referenceTimesMs2[0] = 0;
        long position = firstFramePosition;
        long timeMs = 0;
        for (int i = 1; i <= referenceCount; i++) {
            position += (long) (mlltFrame.bytesBetweenReference + mlltFrame.bytesDeviations[i - 1]);
            timeMs += (long) (mlltFrame.millisecondsBetweenReference + mlltFrame.millisecondsDeviations[i - 1]);
            referencePositions2[i] = position;
            referenceTimesMs2[i] = timeMs;
        }
        return new MlltSeeker(referencePositions2, referenceTimesMs2);
    }

    private MlltSeeker(long[] referencePositions2, long[] referenceTimesMs2) {
        this.referencePositions = referencePositions2;
        this.referenceTimesMs = referenceTimesMs2;
        this.durationUs = C.msToUs(referenceTimesMs2[referenceTimesMs2.length - 1]);
    }

    public boolean isSeekable() {
        return true;
    }

    public SeekPoints getSeekPoints(long timeUs) {
        Pair<Long, Long> timeMsAndPosition = linearlyInterpolate(C.usToMs(Util.constrainValue(timeUs, 0, this.durationUs)), this.referenceTimesMs, this.referencePositions);
        return new SeekPoints(new SeekPoint(C.msToUs(((Long) timeMsAndPosition.first).longValue()), ((Long) timeMsAndPosition.second).longValue()));
    }

    public long getTimeUs(long position) {
        return C.msToUs(((Long) linearlyInterpolate(position, this.referencePositions, this.referenceTimesMs).second).longValue());
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    private static Pair<Long, Long> linearlyInterpolate(long x, long[] xReferences, long[] yReferences) {
        double d;
        long j = x;
        long[] jArr = xReferences;
        int previousReferenceIndex = Util.binarySearchFloor(jArr, j, true, true);
        long xPreviousReference = jArr[previousReferenceIndex];
        long yPreviousReference = yReferences[previousReferenceIndex];
        int nextReferenceIndex = previousReferenceIndex + 1;
        if (nextReferenceIndex == jArr.length) {
            return Pair.create(Long.valueOf(xPreviousReference), Long.valueOf(yPreviousReference));
        }
        long xNextReference = jArr[nextReferenceIndex];
        long yNextReference = yReferences[nextReferenceIndex];
        if (xNextReference == xPreviousReference) {
            d = 0.0d;
        } else {
            double d2 = (double) j;
            double d3 = (double) xPreviousReference;
            Double.isNaN(d2);
            Double.isNaN(d3);
            double d4 = d2 - d3;
            double d5 = (double) (xNextReference - xPreviousReference);
            Double.isNaN(d5);
            d = d4 / d5;
        }
        double proportion = d;
        double d6 = (double) (yNextReference - yPreviousReference);
        Double.isNaN(d6);
        double d7 = proportion;
        return Pair.create(Long.valueOf(x), Long.valueOf(((long) (d6 * proportion)) + yPreviousReference));
    }

    public long getDataEndPosition() {
        return -1;
    }
}
