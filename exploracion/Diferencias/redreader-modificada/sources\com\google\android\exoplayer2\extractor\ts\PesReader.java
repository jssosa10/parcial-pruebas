package com.google.android.exoplayer2.extractor.ts;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.ts.TsPayloadReader.TrackIdGenerator;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.ParsableBitArray;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.TimestampAdjuster;

public final class PesReader implements TsPayloadReader {
    private static final int HEADER_SIZE = 9;
    private static final int MAX_HEADER_EXTENSION_SIZE = 10;
    private static final int PES_SCRATCH_SIZE = 10;
    private static final int STATE_FINDING_HEADER = 0;
    private static final int STATE_READING_BODY = 3;
    private static final int STATE_READING_HEADER = 1;
    private static final int STATE_READING_HEADER_EXTENSION = 2;
    private static final String TAG = "PesReader";
    private int bytesRead;
    private boolean dataAlignmentIndicator;
    private boolean dtsFlag;
    private int extendedHeaderLength;
    private int payloadSize;
    private final ParsableBitArray pesScratch = new ParsableBitArray(new byte[10]);
    private boolean ptsFlag;
    private final ElementaryStreamReader reader;
    private boolean seenFirstDts;
    private int state = 0;
    private long timeUs;
    private TimestampAdjuster timestampAdjuster;

    public PesReader(ElementaryStreamReader reader2) {
        this.reader = reader2;
    }

    public void init(TimestampAdjuster timestampAdjuster2, ExtractorOutput extractorOutput, TrackIdGenerator idGenerator) {
        this.timestampAdjuster = timestampAdjuster2;
        this.reader.createTracks(extractorOutput, idGenerator);
    }

    public final void seek() {
        this.state = 0;
        this.bytesRead = 0;
        this.seenFirstDts = false;
        this.reader.seek();
    }

    public final void consume(ParsableByteArray data, boolean payloadUnitStartIndicator) throws ParserException {
        if (payloadUnitStartIndicator) {
            switch (this.state) {
                case 0:
                case 1:
                    break;
                case 2:
                    Log.w(TAG, "Unexpected start indicator reading extended header");
                    break;
                case 3:
                    if (this.payloadSize != -1) {
                        String str = TAG;
                        StringBuilder sb = new StringBuilder();
                        sb.append("Unexpected start indicator: expected ");
                        sb.append(this.payloadSize);
                        sb.append(" more bytes");
                        Log.w(str, sb.toString());
                    }
                    this.reader.packetFinished();
                    break;
                default:
                    throw new IllegalStateException();
            }
            setState(1);
        }
        while (data.bytesLeft() > 0) {
            int padding = 0;
            switch (this.state) {
                case 0:
                    data.skipBytes(data.bytesLeft());
                    break;
                case 1:
                    if (continueRead(data, this.pesScratch.data, 9)) {
                        if (parseHeader()) {
                            padding = 2;
                        }
                        setState(padding);
                        break;
                    } else {
                        break;
                    }
                case 2:
                    if (continueRead(data, this.pesScratch.data, Math.min(10, this.extendedHeaderLength)) && continueRead(data, null, this.extendedHeaderLength)) {
                        parseHeaderExtension();
                        this.reader.packetStarted(this.timeUs, this.dataAlignmentIndicator);
                        setState(3);
                        break;
                    }
                case 3:
                    int readLength = data.bytesLeft();
                    int i = this.payloadSize;
                    if (i != -1) {
                        padding = readLength - i;
                    }
                    if (padding > 0) {
                        readLength -= padding;
                        data.setLimit(data.getPosition() + readLength);
                    }
                    this.reader.consume(data);
                    int i2 = this.payloadSize;
                    if (i2 == -1) {
                        break;
                    } else {
                        this.payloadSize = i2 - readLength;
                        if (this.payloadSize != 0) {
                            break;
                        } else {
                            this.reader.packetFinished();
                            setState(1);
                            break;
                        }
                    }
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private void setState(int state2) {
        this.state = state2;
        this.bytesRead = 0;
    }

    private boolean continueRead(ParsableByteArray source, byte[] target, int targetLength) {
        int bytesToRead = Math.min(source.bytesLeft(), targetLength - this.bytesRead);
        boolean z = true;
        if (bytesToRead <= 0) {
            return true;
        }
        if (target == null) {
            source.skipBytes(bytesToRead);
        } else {
            source.readBytes(target, this.bytesRead, bytesToRead);
        }
        this.bytesRead += bytesToRead;
        if (this.bytesRead != targetLength) {
            z = false;
        }
        return z;
    }

    private boolean parseHeader() {
        this.pesScratch.setPosition(0);
        int startCodePrefix = this.pesScratch.readBits(24);
        if (startCodePrefix != 1) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Unexpected start code prefix: ");
            sb.append(startCodePrefix);
            Log.w(str, sb.toString());
            this.payloadSize = -1;
            return false;
        }
        this.pesScratch.skipBits(8);
        int packetLength = this.pesScratch.readBits(16);
        this.pesScratch.skipBits(5);
        this.dataAlignmentIndicator = this.pesScratch.readBit();
        this.pesScratch.skipBits(2);
        this.ptsFlag = this.pesScratch.readBit();
        this.dtsFlag = this.pesScratch.readBit();
        this.pesScratch.skipBits(6);
        this.extendedHeaderLength = this.pesScratch.readBits(8);
        if (packetLength == 0) {
            this.payloadSize = -1;
        } else {
            this.payloadSize = ((packetLength + 6) - 9) - this.extendedHeaderLength;
        }
        return true;
    }

    private void parseHeaderExtension() {
        this.pesScratch.setPosition(0);
        this.timeUs = C.TIME_UNSET;
        if (this.ptsFlag) {
            this.pesScratch.skipBits(4);
            long pts = ((long) this.pesScratch.readBits(3)) << 30;
            this.pesScratch.skipBits(1);
            long pts2 = pts | ((long) (this.pesScratch.readBits(15) << 15));
            this.pesScratch.skipBits(1);
            long pts3 = pts2 | ((long) this.pesScratch.readBits(15));
            this.pesScratch.skipBits(1);
            if (!this.seenFirstDts && this.dtsFlag) {
                this.pesScratch.skipBits(4);
                long dts = ((long) this.pesScratch.readBits(3)) << 30;
                this.pesScratch.skipBits(1);
                long dts2 = dts | ((long) (this.pesScratch.readBits(15) << 15));
                this.pesScratch.skipBits(1);
                long dts3 = dts2 | ((long) this.pesScratch.readBits(15));
                this.pesScratch.skipBits(1);
                this.timestampAdjuster.adjustTsTimestamp(dts3);
                this.seenFirstDts = true;
            }
            this.timeUs = this.timestampAdjuster.adjustTsTimestamp(pts3);
        }
    }
}
