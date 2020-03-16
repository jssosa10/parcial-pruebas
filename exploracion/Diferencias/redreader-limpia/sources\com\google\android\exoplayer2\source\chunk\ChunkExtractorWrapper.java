package com.google.android.exoplayer2.source.chunk;

import android.support.annotation.Nullable;
import android.util.SparseArray;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.extractor.DummyTrackOutput;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.extractor.TrackOutput.CryptoData;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.ParsableByteArray;
import java.io.IOException;

public final class ChunkExtractorWrapper implements ExtractorOutput {
    private final SparseArray<BindingTrackOutput> bindingTrackOutputs = new SparseArray<>();
    private long endTimeUs;
    public final Extractor extractor;
    private boolean extractorInitialized;
    private final Format primaryTrackManifestFormat;
    private final int primaryTrackType;
    private Format[] sampleFormats;
    private SeekMap seekMap;
    private TrackOutputProvider trackOutputProvider;

    private static final class BindingTrackOutput implements TrackOutput {
        private final DummyTrackOutput dummyTrackOutput = new DummyTrackOutput();
        private long endTimeUs;
        private final int id;
        private final Format manifestFormat;
        public Format sampleFormat;
        private TrackOutput trackOutput;
        private final int type;

        public BindingTrackOutput(int id2, int type2, Format manifestFormat2) {
            this.id = id2;
            this.type = type2;
            this.manifestFormat = manifestFormat2;
        }

        public void bind(TrackOutputProvider trackOutputProvider, long endTimeUs2) {
            if (trackOutputProvider == null) {
                this.trackOutput = this.dummyTrackOutput;
                return;
            }
            this.endTimeUs = endTimeUs2;
            this.trackOutput = trackOutputProvider.track(this.id, this.type);
            Format format = this.sampleFormat;
            if (format != null) {
                this.trackOutput.format(format);
            }
        }

        public void format(Format format) {
            Format format2 = this.manifestFormat;
            this.sampleFormat = format2 != null ? format.copyWithManifestFormatInfo(format2) : format;
            this.trackOutput.format(this.sampleFormat);
        }

        public int sampleData(ExtractorInput input, int length, boolean allowEndOfInput) throws IOException, InterruptedException {
            return this.trackOutput.sampleData(input, length, allowEndOfInput);
        }

        public void sampleData(ParsableByteArray data, int length) {
            this.trackOutput.sampleData(data, length);
        }

        public void sampleMetadata(long timeUs, int flags, int size, int offset, CryptoData cryptoData) {
            long j = this.endTimeUs;
            if (j != C.TIME_UNSET && timeUs >= j) {
                this.trackOutput = this.dummyTrackOutput;
            }
            this.trackOutput.sampleMetadata(timeUs, flags, size, offset, cryptoData);
        }
    }

    public interface TrackOutputProvider {
        TrackOutput track(int i, int i2);
    }

    public ChunkExtractorWrapper(Extractor extractor2, int primaryTrackType2, Format primaryTrackManifestFormat2) {
        this.extractor = extractor2;
        this.primaryTrackType = primaryTrackType2;
        this.primaryTrackManifestFormat = primaryTrackManifestFormat2;
    }

    public SeekMap getSeekMap() {
        return this.seekMap;
    }

    public Format[] getSampleFormats() {
        return this.sampleFormats;
    }

    public void init(@Nullable TrackOutputProvider trackOutputProvider2, long startTimeUs, long endTimeUs2) {
        this.trackOutputProvider = trackOutputProvider2;
        this.endTimeUs = endTimeUs2;
        if (!this.extractorInitialized) {
            this.extractor.init(this);
            if (startTimeUs != C.TIME_UNSET) {
                this.extractor.seek(0, startTimeUs);
            }
            this.extractorInitialized = true;
            return;
        }
        this.extractor.seek(0, startTimeUs == C.TIME_UNSET ? 0 : startTimeUs);
        for (int i = 0; i < this.bindingTrackOutputs.size(); i++) {
            ((BindingTrackOutput) this.bindingTrackOutputs.valueAt(i)).bind(trackOutputProvider2, endTimeUs2);
        }
    }

    public TrackOutput track(int id, int type) {
        BindingTrackOutput bindingTrackOutput = (BindingTrackOutput) this.bindingTrackOutputs.get(id);
        if (bindingTrackOutput != null) {
            return bindingTrackOutput;
        }
        Assertions.checkState(this.sampleFormats == null);
        BindingTrackOutput bindingTrackOutput2 = new BindingTrackOutput(id, type, type == this.primaryTrackType ? this.primaryTrackManifestFormat : null);
        bindingTrackOutput2.bind(this.trackOutputProvider, this.endTimeUs);
        this.bindingTrackOutputs.put(id, bindingTrackOutput2);
        return bindingTrackOutput2;
    }

    public void endTracks() {
        Format[] sampleFormats2 = new Format[this.bindingTrackOutputs.size()];
        for (int i = 0; i < this.bindingTrackOutputs.size(); i++) {
            sampleFormats2[i] = ((BindingTrackOutput) this.bindingTrackOutputs.valueAt(i)).sampleFormat;
        }
        this.sampleFormats = sampleFormats2;
    }

    public void seekMap(SeekMap seekMap2) {
        this.seekMap = seekMap2;
    }
}
