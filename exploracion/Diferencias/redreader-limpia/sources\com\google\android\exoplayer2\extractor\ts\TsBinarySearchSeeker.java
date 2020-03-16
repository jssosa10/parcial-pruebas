package com.google.android.exoplayer2.extractor.ts;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.BinarySearchSeeker;
import com.google.android.exoplayer2.extractor.BinarySearchSeeker.DefaultSeekTimestampConverter;
import com.google.android.exoplayer2.extractor.BinarySearchSeeker.OutputFrameHolder;
import com.google.android.exoplayer2.extractor.BinarySearchSeeker.TimestampSearchResult;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.TimestampAdjuster;
import java.io.IOException;

final class TsBinarySearchSeeker extends BinarySearchSeeker {
    private static final int MINIMUM_SEARCH_RANGE_BYTES = 940;
    private static final long SEEK_TOLERANCE_US = 100000;
    private static final int TIMESTAMP_SEARCH_BYTES = 37600;
    private static final int TIMESTAMP_SEARCH_PACKETS = 200;

    private static final class TsPcrSeeker implements TimestampSeeker {
        private final ParsableByteArray packetBuffer = new ParsableByteArray((int) TsBinarySearchSeeker.TIMESTAMP_SEARCH_BYTES);
        private final int pcrPid;
        private final TimestampAdjuster pcrTimestampAdjuster;

        public TsPcrSeeker(int pcrPid2, TimestampAdjuster pcrTimestampAdjuster2) {
            this.pcrPid = pcrPid2;
            this.pcrTimestampAdjuster = pcrTimestampAdjuster2;
        }

        public TimestampSearchResult searchForTimestamp(ExtractorInput input, long targetTimestamp, OutputFrameHolder outputFrameHolder) throws IOException, InterruptedException {
            long inputPosition = input.getPosition();
            int bytesToRead = (int) Math.min(37600, input.getLength() - input.getPosition());
            this.packetBuffer.reset(bytesToRead);
            input.peekFully(this.packetBuffer.data, 0, bytesToRead);
            return searchForPcrValueInBuffer(this.packetBuffer, targetTimestamp, inputPosition);
        }

        private TimestampSearchResult searchForPcrValueInBuffer(ParsableByteArray packetBuffer2, long targetPcrTimeUs, long bufferStartOffset) {
            long endOfLastPacketPosition;
            int limit;
            ParsableByteArray parsableByteArray = packetBuffer2;
            long j = bufferStartOffset;
            int limit2 = packetBuffer2.limit();
            long startOfLastPacketPosition = -1;
            long pcrValue = -1;
            long lastPcrTimeUsInRange = C.TIME_UNSET;
            while (true) {
                if (packetBuffer2.bytesLeft() < 188) {
                    long j2 = startOfLastPacketPosition;
                    endOfLastPacketPosition = pcrValue;
                    break;
                }
                int startOfPacket = TsUtil.findSyncBytePosition(parsableByteArray.data, packetBuffer2.getPosition(), limit2);
                int endOfPacket = startOfPacket + TsExtractor.TS_PACKET_SIZE;
                if (endOfPacket > limit2) {
                    int i = limit2;
                    long j3 = startOfLastPacketPosition;
                    endOfLastPacketPosition = pcrValue;
                    break;
                }
                long j4 = pcrValue;
                long pcrValue2 = TsUtil.readPcrFromPacket(parsableByteArray, startOfPacket, this.pcrPid);
                if (pcrValue2 != C.TIME_UNSET) {
                    long pcrTimeUs = this.pcrTimestampAdjuster.adjustTsTimestamp(pcrValue2);
                    if (pcrTimeUs > targetPcrTimeUs) {
                        if (lastPcrTimeUsInRange == C.TIME_UNSET) {
                            return TimestampSearchResult.overestimatedResult(pcrTimeUs, j);
                        }
                        return TimestampSearchResult.targetFoundResult(j + startOfLastPacketPosition);
                    } else if (pcrTimeUs + TsBinarySearchSeeker.SEEK_TOLERANCE_US > targetPcrTimeUs) {
                        int i2 = limit2;
                        long j5 = startOfLastPacketPosition;
                        return TimestampSearchResult.targetFoundResult(((long) startOfPacket) + j);
                    } else {
                        limit = limit2;
                        long j6 = startOfLastPacketPosition;
                        startOfLastPacketPosition = (long) startOfPacket;
                        lastPcrTimeUsInRange = pcrTimeUs;
                    }
                } else {
                    limit = limit2;
                    long j7 = startOfLastPacketPosition;
                }
                parsableByteArray.setPosition(endOfPacket);
                pcrValue = (long) endOfPacket;
                limit2 = limit;
            }
            if (lastPcrTimeUsInRange != C.TIME_UNSET) {
                return TimestampSearchResult.underestimatedResult(lastPcrTimeUsInRange, j + endOfLastPacketPosition);
            }
            return TimestampSearchResult.NO_TIMESTAMP_IN_RANGE_RESULT;
        }
    }

    public TsBinarySearchSeeker(TimestampAdjuster pcrTimestampAdjuster, long streamDurationUs, long inputLength, int pcrPid) {
        long j = streamDurationUs;
        super(new DefaultSeekTimestampConverter(), new TsPcrSeeker(pcrPid, pcrTimestampAdjuster), j, 0, streamDurationUs + 1, 0, inputLength, 188, MINIMUM_SEARCH_RANGE_BYTES);
    }
}
