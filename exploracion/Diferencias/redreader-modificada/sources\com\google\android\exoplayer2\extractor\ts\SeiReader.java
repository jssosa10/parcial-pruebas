package com.google.android.exoplayer2.extractor.ts;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.extractor.ts.TsPayloadReader.TrackIdGenerator;
import com.google.android.exoplayer2.text.cea.CeaUtil;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.ParsableByteArray;
import java.util.List;

final class SeiReader {
    private final List<Format> closedCaptionFormats;
    private final TrackOutput[] outputs;

    public SeiReader(List<Format> closedCaptionFormats2) {
        this.closedCaptionFormats = closedCaptionFormats2;
        this.outputs = new TrackOutput[closedCaptionFormats2.size()];
    }

    public void createTracks(ExtractorOutput extractorOutput, TrackIdGenerator idGenerator) {
        for (int i = 0; i < this.outputs.length; i++) {
            idGenerator.generateNewId();
            TrackOutput output = extractorOutput.track(idGenerator.getTrackId(), 3);
            Format channelFormat = (Format) this.closedCaptionFormats.get(i);
            String channelMimeType = channelFormat.sampleMimeType;
            boolean z = MimeTypes.APPLICATION_CEA608.equals(channelMimeType) || MimeTypes.APPLICATION_CEA708.equals(channelMimeType);
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid closed caption mime type provided: ");
            sb.append(channelMimeType);
            Assertions.checkArgument(z, sb.toString());
            String str = channelMimeType;
            output.format(Format.createTextSampleFormat(channelFormat.id != null ? channelFormat.id : idGenerator.getFormatId(), channelMimeType, null, -1, channelFormat.selectionFlags, channelFormat.language, channelFormat.accessibilityChannel, null, Long.MAX_VALUE, channelFormat.initializationData));
            this.outputs[i] = output;
        }
        ExtractorOutput extractorOutput2 = extractorOutput;
    }

    public void consume(long pesTimeUs, ParsableByteArray seiBuffer) {
        CeaUtil.consume(pesTimeUs, seiBuffer, this.outputs);
    }
}
