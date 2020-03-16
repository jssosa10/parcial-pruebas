package com.google.android.exoplayer2.extractor.ts;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.extractor.ts.TsPayloadReader.DvbSubtitleInfo;
import com.google.android.exoplayer2.extractor.ts.TsPayloadReader.TrackIdGenerator;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.ParsableByteArray;
import java.util.Collections;
import java.util.List;

public final class DvbSubtitleReader implements ElementaryStreamReader {
    private int bytesToCheck;
    private final TrackOutput[] outputs;
    private int sampleBytesWritten;
    private long sampleTimeUs;
    private final List<DvbSubtitleInfo> subtitleInfos;
    private boolean writingSample;

    public DvbSubtitleReader(List<DvbSubtitleInfo> subtitleInfos2) {
        this.subtitleInfos = subtitleInfos2;
        this.outputs = new TrackOutput[subtitleInfos2.size()];
    }

    public void seek() {
        this.writingSample = false;
    }

    public void createTracks(ExtractorOutput extractorOutput, TrackIdGenerator idGenerator) {
        for (int i = 0; i < this.outputs.length; i++) {
            DvbSubtitleInfo subtitleInfo = (DvbSubtitleInfo) this.subtitleInfos.get(i);
            idGenerator.generateNewId();
            TrackOutput output = extractorOutput.track(idGenerator.getTrackId(), 3);
            output.format(Format.createImageSampleFormat(idGenerator.getFormatId(), MimeTypes.APPLICATION_DVBSUBS, null, -1, 0, Collections.singletonList(subtitleInfo.initializationData), subtitleInfo.language, null));
            this.outputs[i] = output;
        }
    }

    public void packetStarted(long pesTimeUs, boolean dataAlignmentIndicator) {
        if (dataAlignmentIndicator) {
            this.writingSample = true;
            this.sampleTimeUs = pesTimeUs;
            this.sampleBytesWritten = 0;
            this.bytesToCheck = 2;
        }
    }

    public void packetFinished() {
        if (this.writingSample) {
            TrackOutput[] trackOutputArr = this.outputs;
            int length = trackOutputArr.length;
            for (int i = 0; i < length; i++) {
                trackOutputArr[i].sampleMetadata(this.sampleTimeUs, 1, this.sampleBytesWritten, 0, null);
            }
            this.writingSample = false;
        }
    }

    public void consume(ParsableByteArray data) {
        TrackOutput[] trackOutputArr;
        if (this.writingSample && (this.bytesToCheck != 2 || checkNextByte(data, 32))) {
            if (this.bytesToCheck != 1 || checkNextByte(data, 0)) {
                int dataPosition = data.getPosition();
                int bytesAvailable = data.bytesLeft();
                for (TrackOutput output : this.outputs) {
                    data.setPosition(dataPosition);
                    output.sampleData(data, bytesAvailable);
                }
                this.sampleBytesWritten += bytesAvailable;
            }
        }
    }

    private boolean checkNextByte(ParsableByteArray data, int expectedValue) {
        if (data.bytesLeft() == 0) {
            return false;
        }
        if (data.readUnsignedByte() != expectedValue) {
            this.writingSample = false;
        }
        this.bytesToCheck--;
        return this.writingSample;
    }
}
