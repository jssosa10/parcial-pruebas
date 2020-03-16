package com.google.android.exoplayer2.extractor.mp4;

import android.util.Pair;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.audio.Ac3Util;
import com.google.android.exoplayer2.drm.DrmInitData;
import com.google.android.exoplayer2.extractor.GaplessInfoHolder;
import com.google.android.exoplayer2.extractor.mp4.FixedSampleSizeRechunker.Results;
import com.google.android.exoplayer2.extractor.ts.PsExtractor;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.Metadata.Entry;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.CodecSpecificDataUtil;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.AvcConfig;
import com.google.android.exoplayer2.video.HevcConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class AtomParsers {
    private static final int MAX_GAPLESS_TRIM_SIZE_SAMPLES = 3;
    private static final String TAG = "AtomParsers";
    private static final int TYPE_clcp = Util.getIntegerCodeForString("clcp");
    private static final int TYPE_meta = Util.getIntegerCodeForString("meta");
    private static final int TYPE_sbtl = Util.getIntegerCodeForString("sbtl");
    private static final int TYPE_soun = Util.getIntegerCodeForString("soun");
    private static final int TYPE_subt = Util.getIntegerCodeForString("subt");
    private static final int TYPE_text = Util.getIntegerCodeForString(MimeTypes.BASE_TYPE_TEXT);
    private static final int TYPE_vide = Util.getIntegerCodeForString("vide");

    private static final class ChunkIterator {
        private final ParsableByteArray chunkOffsets;
        private final boolean chunkOffsetsAreLongs;
        public int index;
        public final int length;
        private int nextSamplesPerChunkChangeIndex;
        public int numSamples;
        public long offset;
        private int remainingSamplesPerChunkChanges;
        private final ParsableByteArray stsc;

        public ChunkIterator(ParsableByteArray stsc2, ParsableByteArray chunkOffsets2, boolean chunkOffsetsAreLongs2) {
            this.stsc = stsc2;
            this.chunkOffsets = chunkOffsets2;
            this.chunkOffsetsAreLongs = chunkOffsetsAreLongs2;
            chunkOffsets2.setPosition(12);
            this.length = chunkOffsets2.readUnsignedIntToInt();
            stsc2.setPosition(12);
            this.remainingSamplesPerChunkChanges = stsc2.readUnsignedIntToInt();
            boolean z = true;
            if (stsc2.readInt() != 1) {
                z = false;
            }
            Assertions.checkState(z, "first_chunk must be 1");
            this.index = -1;
        }

        public boolean moveNext() {
            long j;
            int i = this.index + 1;
            this.index = i;
            if (i == this.length) {
                return false;
            }
            if (this.chunkOffsetsAreLongs) {
                j = this.chunkOffsets.readUnsignedLongToLong();
            } else {
                j = this.chunkOffsets.readUnsignedInt();
            }
            this.offset = j;
            if (this.index == this.nextSamplesPerChunkChangeIndex) {
                this.numSamples = this.stsc.readUnsignedIntToInt();
                this.stsc.skipBytes(4);
                int i2 = this.remainingSamplesPerChunkChanges - 1;
                this.remainingSamplesPerChunkChanges = i2;
                this.nextSamplesPerChunkChangeIndex = i2 > 0 ? this.stsc.readUnsignedIntToInt() - 1 : -1;
            }
            return true;
        }
    }

    private interface SampleSizeBox {
        int getSampleCount();

        boolean isFixedSampleSize();

        int readNextSampleSize();
    }

    private static final class StsdData {
        public static final int STSD_HEADER_SIZE = 8;
        public Format format;
        public int nalUnitLengthFieldLength;
        public int requiredSampleTransformation = 0;
        public final TrackEncryptionBox[] trackEncryptionBoxes;

        public StsdData(int numberOfEntries) {
            this.trackEncryptionBoxes = new TrackEncryptionBox[numberOfEntries];
        }
    }

    static final class StszSampleSizeBox implements SampleSizeBox {
        private final ParsableByteArray data;
        private final int fixedSampleSize = this.data.readUnsignedIntToInt();
        private final int sampleCount = this.data.readUnsignedIntToInt();

        public StszSampleSizeBox(LeafAtom stszAtom) {
            this.data = stszAtom.data;
            this.data.setPosition(12);
        }

        public int getSampleCount() {
            return this.sampleCount;
        }

        public int readNextSampleSize() {
            int i = this.fixedSampleSize;
            return i == 0 ? this.data.readUnsignedIntToInt() : i;
        }

        public boolean isFixedSampleSize() {
            return this.fixedSampleSize != 0;
        }
    }

    static final class Stz2SampleSizeBox implements SampleSizeBox {
        private int currentByte;
        private final ParsableByteArray data;
        private final int fieldSize = (this.data.readUnsignedIntToInt() & 255);
        private final int sampleCount = this.data.readUnsignedIntToInt();
        private int sampleIndex;

        public Stz2SampleSizeBox(LeafAtom stz2Atom) {
            this.data = stz2Atom.data;
            this.data.setPosition(12);
        }

        public int getSampleCount() {
            return this.sampleCount;
        }

        public int readNextSampleSize() {
            int i = this.fieldSize;
            if (i == 8) {
                return this.data.readUnsignedByte();
            }
            if (i == 16) {
                return this.data.readUnsignedShort();
            }
            int i2 = this.sampleIndex;
            this.sampleIndex = i2 + 1;
            if (i2 % 2 != 0) {
                return this.currentByte & 15;
            }
            this.currentByte = this.data.readUnsignedByte();
            return (this.currentByte & PsExtractor.VIDEO_STREAM_MASK) >> 4;
        }

        public boolean isFixedSampleSize() {
            return false;
        }
    }

    private static final class TkhdData {
        /* access modifiers changed from: private */
        public final long duration;
        /* access modifiers changed from: private */
        public final int id;
        /* access modifiers changed from: private */
        public final int rotationDegrees;

        public TkhdData(int id2, long duration2, int rotationDegrees2) {
            this.id = id2;
            this.duration = duration2;
            this.rotationDegrees = rotationDegrees2;
        }
    }

    public static Track parseTrak(ContainerAtom trak, LeafAtom mvhd, long duration, DrmInitData drmInitData, boolean ignoreEditLists, boolean isQuickTime) throws ParserException {
        long duration2;
        long durationUs;
        long[] editListMediaTimes;
        long[] editListDurations;
        Track track;
        ContainerAtom containerAtom = trak;
        ContainerAtom mdia = containerAtom.getContainerAtomOfType(Atom.TYPE_mdia);
        int trackType = parseHdlr(mdia.getLeafAtomOfType(Atom.TYPE_hdlr).data);
        if (trackType == -1) {
            return null;
        }
        TkhdData tkhdData = parseTkhd(containerAtom.getLeafAtomOfType(Atom.TYPE_tkhd).data);
        if (duration == C.TIME_UNSET) {
            duration2 = tkhdData.duration;
        } else {
            duration2 = duration;
        }
        long movieTimescale = parseMvhd(mvhd.data);
        if (duration2 == C.TIME_UNSET) {
            durationUs = -9223372036854775807L;
        } else {
            durationUs = Util.scaleLargeTimestamp(duration2, 1000000, movieTimescale);
        }
        ContainerAtom stbl = mdia.getContainerAtomOfType(Atom.TYPE_minf).getContainerAtomOfType(Atom.TYPE_stbl);
        Pair<Long, String> mdhdData = parseMdhd(mdia.getLeafAtomOfType(Atom.TYPE_mdhd).data);
        StsdData stsdData = parseStsd(stbl.getLeafAtomOfType(Atom.TYPE_stsd).data, tkhdData.id, tkhdData.rotationDegrees, (String) mdhdData.second, drmInitData, isQuickTime);
        if (!ignoreEditLists) {
            Pair<long[], long[]> edtsData = parseEdts(containerAtom.getContainerAtomOfType(Atom.TYPE_edts));
            editListDurations = (long[]) edtsData.first;
            editListMediaTimes = (long[]) edtsData.second;
        } else {
            editListDurations = null;
            editListMediaTimes = null;
        }
        if (stsdData.format == null) {
            track = null;
            StsdData stsdData2 = stsdData;
            Pair pair = mdhdData;
            ContainerAtom containerAtom2 = stbl;
        } else {
            int access$100 = tkhdData.id;
            long longValue = ((Long) mdhdData.first).longValue();
            Format format = stsdData.format;
            int i = stsdData.requiredSampleTransformation;
            StsdData stsdData3 = stsdData;
            Format format2 = format;
            Pair pair2 = mdhdData;
            int i2 = i;
            ContainerAtom containerAtom3 = stbl;
            track = new Track(access$100, trackType, longValue, movieTimescale, durationUs, format2, i2, stsdData.trackEncryptionBoxes, stsdData.nalUnitLengthFieldLength, editListDurations, editListMediaTimes);
        }
        return track;
    }

    public static TrackSampleTable parseStbl(Track track, ContainerAtom stblAtom, GaplessInfoHolder gaplessInfoHolder) throws ParserException {
        SampleSizeBox sampleSizeBox;
        LeafAtom chunkOffsetsAtom;
        boolean chunkOffsetsAreLongs;
        int nextSynchronizationSampleIndex;
        ParsableByteArray stss;
        ParsableByteArray chunkOffsets;
        int nextSynchronizationSampleIndex2;
        ParsableByteArray ctts;
        int remainingSynchronizationSamples;
        long duration;
        int[] sizes;
        long[] offsets;
        long[] timestamps;
        int[] flags;
        long[] offsets2;
        int[] sizes2;
        int[] flags2;
        long[] timestamps2;
        int[] sizes3;
        int[] flags3;
        long[] offsets3;
        ParsableByteArray chunkOffsets2;
        long[] timestamps3;
        ParsableByteArray ctts2;
        int[] endIndices;
        int[] sizes4;
        long[] editedTimestamps;
        ParsableByteArray chunkOffsets3;
        int sampleCount;
        int[] flags4;
        ChunkIterator chunkIterator;
        int remainingSamplesAtTimestampDelta;
        int remainingSamplesInChunk;
        long duration2;
        int timestampOffset;
        int timestampOffset2;
        Track track2 = track;
        ContainerAtom containerAtom = stblAtom;
        GaplessInfoHolder gaplessInfoHolder2 = gaplessInfoHolder;
        LeafAtom stszAtom = containerAtom.getLeafAtomOfType(Atom.TYPE_stsz);
        if (stszAtom != null) {
            sampleSizeBox = new StszSampleSizeBox(stszAtom);
        } else {
            LeafAtom stz2Atom = containerAtom.getLeafAtomOfType(Atom.TYPE_stz2);
            if (stz2Atom != null) {
                sampleSizeBox = new Stz2SampleSizeBox(stz2Atom);
            } else {
                throw new ParserException("Track has no sample table size information");
            }
        }
        int sampleCount2 = sampleSizeBox.getSampleCount();
        if (sampleCount2 == 0) {
            TrackSampleTable trackSampleTable = new TrackSampleTable(track, new long[0], new int[0], 0, new long[0], new int[0], C.TIME_UNSET);
            return trackSampleTable;
        }
        LeafAtom chunkOffsetsAtom2 = containerAtom.getLeafAtomOfType(Atom.TYPE_stco);
        if (chunkOffsetsAtom2 == null) {
            chunkOffsetsAreLongs = true;
            chunkOffsetsAtom = containerAtom.getLeafAtomOfType(Atom.TYPE_co64);
        } else {
            chunkOffsetsAreLongs = false;
            chunkOffsetsAtom = chunkOffsetsAtom2;
        }
        ParsableByteArray chunkOffsets4 = chunkOffsetsAtom.data;
        ParsableByteArray stsc = containerAtom.getLeafAtomOfType(Atom.TYPE_stsc).data;
        ParsableByteArray stts = containerAtom.getLeafAtomOfType(Atom.TYPE_stts).data;
        LeafAtom stssAtom = containerAtom.getLeafAtomOfType(Atom.TYPE_stss);
        ParsableByteArray ctts3 = null;
        ParsableByteArray stss2 = stssAtom != null ? stssAtom.data : null;
        LeafAtom cttsAtom = containerAtom.getLeafAtomOfType(Atom.TYPE_ctts);
        if (cttsAtom != null) {
            ctts3 = cttsAtom.data;
        }
        ChunkIterator chunkIterator2 = new ChunkIterator(stsc, chunkOffsets4, chunkOffsetsAreLongs);
        LeafAtom leafAtom = cttsAtom;
        stts.setPosition(12);
        int remainingTimestampDeltaChanges = stts.readUnsignedIntToInt() - 1;
        int remainingSamplesAtTimestampDelta2 = stts.readUnsignedIntToInt();
        int timestampDeltaInTimeUnits = stts.readUnsignedIntToInt();
        int remainingSamplesAtTimestampOffset = 0;
        int remainingTimestampOffsetChanges = 0;
        if (ctts3 != null) {
            LeafAtom leafAtom2 = stssAtom;
            ctts3.setPosition(12);
            remainingTimestampOffsetChanges = ctts3.readUnsignedIntToInt();
        }
        int remainingSynchronizationSamples2 = 0;
        if (stss2 != null) {
            nextSynchronizationSampleIndex = -1;
            stss2.setPosition(12);
            remainingSynchronizationSamples2 = stss2.readUnsignedIntToInt();
            if (remainingSynchronizationSamples2 > 0) {
                stss = stss2;
                nextSynchronizationSampleIndex = stss2.readUnsignedIntToInt() - 1;
            } else {
                stss = null;
            }
        } else {
            nextSynchronizationSampleIndex = -1;
            stss = stss2;
        }
        long timestampTimeUnits = 0;
        if (!(sampleSizeBox.isFixedSampleSize() && MimeTypes.AUDIO_RAW.equals(track2.format.sampleMimeType) && remainingTimestampDeltaChanges == 0 && remainingTimestampOffsetChanges == 0 && remainingSynchronizationSamples2 == 0)) {
            long[] offsets4 = new long[sampleCount2];
            int[] sizes5 = new int[sampleCount2];
            ParsableByteArray parsableByteArray = stsc;
            timestamps = new long[sampleCount2];
            boolean z = chunkOffsetsAreLongs;
            int[] flags5 = new int[sampleCount2];
            int remainingSamplesInChunk2 = 0;
            int remainingTimestampDeltaChanges2 = remainingTimestampDeltaChanges;
            int timestampOffset3 = 0;
            int nextSynchronizationSampleIndex3 = nextSynchronizationSampleIndex;
            int maximumSize = 0;
            ParsableByteArray parsableByteArray2 = chunkOffsets4;
            int timestampDeltaInTimeUnits2 = timestampDeltaInTimeUnits;
            int i = 0;
            LeafAtom leafAtom3 = chunkOffsetsAtom;
            int remainingSamplesAtTimestampDelta3 = remainingSamplesAtTimestampDelta2;
            chunkOffsets = parsableByteArray2;
            LeafAtom leafAtom4 = stszAtom;
            int remainingSynchronizationSamples3 = remainingSynchronizationSamples2;
            long offset = 0;
            while (true) {
                if (i >= sampleCount2) {
                    chunkIterator = chunkIterator2;
                    int i2 = sampleCount2;
                    remainingSamplesAtTimestampDelta = remainingSamplesAtTimestampDelta3;
                    ctts = ctts3;
                    remainingSamplesInChunk = remainingSamplesInChunk2;
                    break;
                }
                boolean chunkDataComplete = true;
                while (remainingSamplesInChunk2 == 0) {
                    boolean moveNext = chunkIterator2.moveNext();
                    chunkDataComplete = moveNext;
                    if (!moveNext) {
                        break;
                    }
                    int sampleCount3 = sampleCount2;
                    int remainingSamplesAtTimestampDelta4 = remainingSamplesAtTimestampDelta3;
                    offset = chunkIterator2.offset;
                    remainingSamplesInChunk2 = chunkIterator2.numSamples;
                    remainingSamplesAtTimestampDelta3 = remainingSamplesAtTimestampDelta4;
                    sampleCount2 = sampleCount3;
                }
                int sampleCount4 = sampleCount2;
                remainingSamplesAtTimestampDelta = remainingSamplesAtTimestampDelta3;
                if (!chunkDataComplete) {
                    Log.w(TAG, "Unexpected end of chunk data");
                    sampleCount2 = i;
                    offsets4 = Arrays.copyOf(offsets4, sampleCount2);
                    sizes5 = Arrays.copyOf(sizes5, sampleCount2);
                    timestamps = Arrays.copyOf(timestamps, sampleCount2);
                    flags5 = Arrays.copyOf(flags5, sampleCount2);
                    remainingTimestampOffsetChanges = 0;
                    chunkIterator = chunkIterator2;
                    remainingSamplesAtTimestampOffset = 0;
                    remainingSamplesInChunk = remainingSamplesInChunk2;
                    ctts = ctts3;
                    break;
                }
                if (ctts3 != null) {
                    while (remainingSamplesAtTimestampOffset == 0 && remainingTimestampOffsetChanges > 0) {
                        remainingSamplesAtTimestampOffset = ctts3.readUnsignedIntToInt();
                        timestampOffset3 = ctts3.readInt();
                        remainingTimestampOffsetChanges--;
                    }
                    remainingSamplesAtTimestampOffset--;
                    timestampOffset2 = timestampOffset3;
                } else {
                    timestampOffset2 = timestampOffset3;
                }
                offsets4[i] = offset;
                sizes5[i] = sampleSizeBox.readNextSampleSize();
                if (sizes5[i] > maximumSize) {
                    maximumSize = sizes5[i];
                }
                ChunkIterator chunkIterator3 = chunkIterator2;
                ParsableByteArray ctts4 = ctts3;
                timestamps[i] = timestampTimeUnits + ((long) timestampOffset2);
                flags5[i] = stss == null ? 1 : 0;
                if (i == nextSynchronizationSampleIndex3) {
                    flags5[i] = 1;
                    remainingSynchronizationSamples3--;
                    if (remainingSynchronizationSamples3 > 0) {
                        nextSynchronizationSampleIndex3 = stss.readUnsignedIntToInt() - 1;
                    }
                }
                timestampTimeUnits += (long) timestampDeltaInTimeUnits2;
                int remainingSamplesAtTimestampDelta5 = remainingSamplesAtTimestampDelta - 1;
                if (remainingSamplesAtTimestampDelta5 == 0 && remainingTimestampDeltaChanges2 > 0) {
                    remainingSamplesAtTimestampDelta5 = stts.readUnsignedIntToInt();
                    timestampDeltaInTimeUnits2 = stts.readInt();
                    remainingTimestampDeltaChanges2--;
                }
                offset += (long) sizes5[i];
                remainingSamplesInChunk2--;
                i++;
                ctts3 = ctts4;
                remainingSamplesAtTimestampDelta3 = remainingSamplesAtTimestampDelta5;
                chunkIterator2 = chunkIterator3;
                timestampOffset3 = timestampOffset2;
                sampleCount2 = sampleCount4;
            }
            int timestampOffset4 = timestampOffset3;
            int[] sizes6 = sizes5;
            long duration3 = timestampTimeUnits + ((long) timestampOffset4);
            Assertions.checkArgument(remainingSamplesAtTimestampOffset == 0);
            while (remainingTimestampOffsetChanges > 0) {
                Assertions.checkArgument(ctts.readUnsignedIntToInt() == 0);
                ctts.readInt();
                remainingTimestampOffsetChanges--;
            }
            if (remainingSynchronizationSamples3 == 0 && remainingSamplesAtTimestampDelta == 0 && remainingSamplesInChunk == 0 && remainingTimestampDeltaChanges2 == 0) {
                timestampOffset = timestampOffset4;
                duration2 = duration3;
                int i3 = nextSynchronizationSampleIndex3;
                track2 = track;
            } else {
                timestampOffset = timestampOffset4;
                String str = TAG;
                duration2 = duration3;
                StringBuilder sb = new StringBuilder();
                sb.append("Inconsistent stbl box for track ");
                int i4 = nextSynchronizationSampleIndex3;
                track2 = track;
                sb.append(track2.id);
                sb.append(": remainingSynchronizationSamples ");
                sb.append(remainingSynchronizationSamples3);
                sb.append(", remainingSamplesAtTimestampDelta ");
                sb.append(remainingSamplesAtTimestampDelta);
                sb.append(", remainingSamplesInChunk ");
                sb.append(remainingSamplesInChunk);
                sb.append(", remainingTimestampDeltaChanges ");
                sb.append(remainingTimestampDeltaChanges2);
                Log.w(str, sb.toString());
            }
            nextSynchronizationSampleIndex2 = maximumSize;
            int i5 = remainingSynchronizationSamples3;
            chunkIterator2 = chunkIterator;
            remainingSynchronizationSamples = timestampDeltaInTimeUnits2;
            sizes = sizes6;
            int i6 = remainingTimestampDeltaChanges2;
            duration = duration2;
            int[] iArr = flags5;
            offsets = offsets4;
            flags = iArr;
            int i7 = remainingSamplesAtTimestampDelta;
            int remainingSamplesAtTimestampDelta6 = timestampOffset;
            int timestampOffset5 = i7;
        } else {
            ParsableByteArray parsableByteArray3 = stsc;
            boolean z2 = chunkOffsetsAreLongs;
            chunkOffsets = chunkOffsets4;
            LeafAtom leafAtom5 = stszAtom;
            int sampleCount5 = sampleCount2;
            LeafAtom leafAtom6 = chunkOffsetsAtom;
            ctts = ctts3;
            long[] chunkOffsetsBytes = new long[chunkIterator2.length];
            int[] chunkSampleCounts = new int[chunkIterator2.length];
            while (chunkIterator2.moveNext()) {
                chunkOffsetsBytes[chunkIterator2.index] = chunkIterator2.offset;
                chunkSampleCounts[chunkIterator2.index] = chunkIterator2.numSamples;
            }
            Results rechunkedResults = FixedSampleSizeRechunker.rechunk(Util.getPcmFrameSize(track2.format.pcmEncoding, track2.format.channelCount), chunkOffsetsBytes, chunkSampleCounts, (long) timestampDeltaInTimeUnits);
            offsets = rechunkedResults.offsets;
            sizes = rechunkedResults.sizes;
            int maximumSize2 = rechunkedResults.maximumSize;
            long[] timestamps4 = rechunkedResults.timestamps;
            int[] flags6 = rechunkedResults.flags;
            long j = rechunkedResults.duration;
            timestamps = timestamps4;
            flags = flags6;
            int i8 = nextSynchronizationSampleIndex;
            sampleCount2 = sampleCount5;
            remainingSynchronizationSamples = timestampDeltaInTimeUnits;
            nextSynchronizationSampleIndex2 = maximumSize2;
            duration = j;
        }
        long durationUs = Util.scaleLargeTimestamp(duration, 1000000, track2.timescale);
        if (track2.editListDurations == null) {
            ParsableByteArray parsableByteArray4 = stts;
            long j2 = duration;
            int i9 = remainingSynchronizationSamples;
            SampleSizeBox sampleSizeBox2 = sampleSizeBox;
            int i10 = sampleCount2;
            ParsableByteArray parsableByteArray5 = ctts;
            ParsableByteArray parsableByteArray6 = chunkOffsets;
            flags2 = flags;
            timestamps2 = timestamps;
            offsets2 = offsets;
            sizes2 = sizes;
        } else if (gaplessInfoHolder.hasGaplessInfo()) {
            ChunkIterator chunkIterator4 = chunkIterator2;
            ParsableByteArray parsableByteArray7 = stts;
            long j3 = duration;
            int i11 = remainingSynchronizationSamples;
            SampleSizeBox sampleSizeBox3 = sampleSizeBox;
            int i12 = sampleCount2;
            ParsableByteArray parsableByteArray8 = ctts;
            ParsableByteArray parsableByteArray9 = chunkOffsets;
            flags2 = flags;
            timestamps2 = timestamps;
            offsets2 = offsets;
            sizes2 = sizes;
        } else {
            if (track2.editListDurations.length == 1 && track2.type == 1 && timestamps.length >= 2) {
                long editStartTime = track2.editListMediaTimes[0];
                flags3 = flags;
                long editEndTime = editStartTime + Util.scaleLargeTimestamp(track2.editListDurations[0], track2.timescale, track2.movieTimescale);
                if (canApplyEditWithGaplessInfo(timestamps, duration, editStartTime, editEndTime)) {
                    long paddingTimeUnits = duration - editEndTime;
                    long encoderDelay = Util.scaleLargeTimestamp(editStartTime - timestamps[0], (long) track2.format.sampleRate, track2.timescale);
                    int i13 = remainingSynchronizationSamples;
                    SampleSizeBox sampleSizeBox4 = sampleSizeBox;
                    long encoderPadding = Util.scaleLargeTimestamp(paddingTimeUnits, (long) track2.format.sampleRate, track2.timescale);
                    if (encoderDelay == 0 && encoderPadding == 0) {
                        ChunkIterator chunkIterator5 = chunkIterator2;
                        ParsableByteArray parsableByteArray10 = stts;
                        timestamps3 = timestamps;
                        sizes3 = sizes;
                        chunkOffsets2 = chunkOffsets;
                        offsets3 = offsets;
                    } else if (encoderDelay > 2147483647L || encoderPadding > 2147483647L) {
                        long j4 = encoderDelay;
                        ParsableByteArray parsableByteArray11 = stts;
                        sizes3 = sizes;
                        long j5 = encoderPadding;
                        chunkOffsets2 = chunkOffsets;
                        timestamps3 = timestamps;
                        offsets3 = offsets;
                    } else {
                        GaplessInfoHolder gaplessInfoHolder3 = gaplessInfoHolder;
                        gaplessInfoHolder3.encoderDelay = (int) encoderDelay;
                        gaplessInfoHolder3.encoderPadding = (int) encoderPadding;
                        ChunkIterator chunkIterator6 = chunkIterator2;
                        long encoderDelay2 = encoderDelay;
                        Util.scaleLargeTimestampsInPlace(timestamps, 1000000, track2.timescale);
                        ChunkIterator chunkIterator7 = chunkIterator6;
                        long j6 = encoderDelay2;
                        ParsableByteArray parsableByteArray12 = stts;
                        long j7 = encoderPadding;
                        long[] jArr = timestamps;
                        int[] iArr2 = sizes;
                        ParsableByteArray parsableByteArray13 = chunkOffsets;
                        long[] jArr2 = offsets;
                        TrackSampleTable trackSampleTable2 = new TrackSampleTable(track, offsets, sizes, nextSynchronizationSampleIndex2, timestamps, flags3, Util.scaleLargeTimestamp(track2.editListDurations[0], 1000000, track2.movieTimescale));
                        return trackSampleTable2;
                    }
                } else {
                    ParsableByteArray parsableByteArray14 = stts;
                    sizes3 = sizes;
                    int i14 = remainingSynchronizationSamples;
                    SampleSizeBox sampleSizeBox5 = sampleSizeBox;
                    chunkOffsets2 = chunkOffsets;
                    timestamps3 = timestamps;
                    offsets3 = offsets;
                }
            } else {
                flags3 = flags;
                ParsableByteArray parsableByteArray15 = stts;
                sizes3 = sizes;
                int i15 = remainingSynchronizationSamples;
                SampleSizeBox sampleSizeBox6 = sampleSizeBox;
                chunkOffsets2 = chunkOffsets;
                timestamps3 = timestamps;
                offsets3 = offsets;
            }
            if (track2.editListDurations.length == 1 && track2.editListDurations[0] == 0) {
                long editStartTime2 = track2.editListMediaTimes[0];
                for (int i16 = 0; i16 < timestamps3.length; i16++) {
                    timestamps3[i16] = Util.scaleLargeTimestamp(timestamps3[i16] - editStartTime2, 1000000, track2.timescale);
                }
                TrackSampleTable trackSampleTable3 = new TrackSampleTable(track, offsets3, sizes3, nextSynchronizationSampleIndex2, timestamps3, flags3, Util.scaleLargeTimestamp(duration - editStartTime2, 1000000, track2.timescale));
                return trackSampleTable3;
            }
            boolean omitClippedSample = track2.type == 1;
            boolean copyMetadata = 0;
            int[] startIndices = new int[track2.editListDurations.length];
            int[] endIndices2 = new int[track2.editListDurations.length];
            int i17 = 0;
            int editedSampleCount = 0;
            int nextSampleIndex = 0;
            while (i17 < track2.editListDurations.length) {
                long duration4 = duration;
                long editMediaTime = track2.editListMediaTimes[i17];
                if (editMediaTime != -1) {
                    chunkOffsets3 = chunkOffsets2;
                    sampleCount = sampleCount2;
                    long editDuration = Util.scaleLargeTimestamp(track2.editListDurations[i17], track2.timescale, track2.movieTimescale);
                    startIndices[i17] = Util.binarySearchCeil(timestamps3, editMediaTime, true, true);
                    long j8 = editDuration;
                    endIndices2[i17] = Util.binarySearchCeil(timestamps3, editMediaTime + editDuration, omitClippedSample, false);
                    while (true) {
                        if (startIndices[i17] >= endIndices2[i17]) {
                            flags4 = flags3;
                            break;
                        }
                        flags4 = flags3;
                        if ((flags4[startIndices[i17]] & 1) != 0) {
                            break;
                        }
                        startIndices[i17] = startIndices[i17] + 1;
                        flags3 = flags4;
                    }
                    editedSampleCount += endIndices2[i17] - startIndices[i17];
                    boolean copyMetadata2 = (nextSampleIndex != startIndices[i17]) | copyMetadata;
                    nextSampleIndex = endIndices2[i17];
                    copyMetadata = copyMetadata2;
                } else {
                    chunkOffsets3 = chunkOffsets2;
                    sampleCount = sampleCount2;
                    flags4 = flags3;
                }
                i17++;
                flags3 = flags4;
                duration = duration4;
                sampleCount2 = sampleCount;
                chunkOffsets2 = chunkOffsets3;
            }
            ParsableByteArray parsableByteArray16 = chunkOffsets2;
            int sampleCount6 = sampleCount2;
            int[] flags7 = flags3;
            int editedMaximumSize = 0;
            boolean sampleCount7 = true;
            int sampleCount8 = sampleCount6;
            if (editedSampleCount == sampleCount8) {
                sampleCount7 = false;
            }
            boolean copyMetadata3 = copyMetadata | sampleCount7;
            long[] editedOffsets = copyMetadata3 ? new long[editedSampleCount] : offsets3;
            int[] editedSizes = copyMetadata3 ? new int[editedSampleCount] : sizes3;
            if (!copyMetadata3) {
                editedMaximumSize = nextSynchronizationSampleIndex2;
            }
            int[] editedFlags = copyMetadata3 ? new int[editedSampleCount] : flags7;
            long[] editedTimestamps2 = new long[editedSampleCount];
            long pts = 0;
            int i18 = sampleCount8;
            int editedMaximumSize2 = 0;
            int sampleIndex = editedMaximumSize;
            int i19 = 0;
            while (true) {
                int nextSampleIndex2 = nextSampleIndex;
                if (i19 < track2.editListDurations.length) {
                    long editMediaTime2 = track2.editListMediaTimes[i19];
                    int startIndex = startIndices[i19];
                    int editedSampleCount2 = editedSampleCount;
                    int endIndex = endIndices2[i19];
                    if (copyMetadata3) {
                        endIndices = endIndices2;
                        int count = endIndex - startIndex;
                        ctts2 = ctts;
                        System.arraycopy(offsets3, startIndex, editedOffsets, editedMaximumSize2, count);
                        sizes4 = sizes3;
                        System.arraycopy(sizes4, startIndex, editedSizes, editedMaximumSize2, count);
                        System.arraycopy(flags7, startIndex, editedFlags, editedMaximumSize2, count);
                    } else {
                        endIndices = endIndices2;
                        ctts2 = ctts;
                        sizes4 = sizes3;
                    }
                    int j9 = startIndex;
                    int i20 = sampleIndex;
                    int sampleIndex2 = editedMaximumSize2;
                    int editedMaximumSize3 = i20;
                    while (j9 < endIndex) {
                        int startIndex2 = startIndex;
                        int endIndex2 = endIndex;
                        boolean omitClippedSample2 = omitClippedSample;
                        int[] startIndices2 = startIndices;
                        editedTimestamps2[sampleIndex2] = Util.scaleLargeTimestamp(pts, 1000000, track2.movieTimescale) + Util.scaleLargeTimestamp(timestamps3[j9] - editMediaTime2, 1000000, track2.timescale);
                        if (copyMetadata3) {
                            editedTimestamps = editedTimestamps2;
                            if (editedSizes[sampleIndex2] > editedMaximumSize3) {
                                editedMaximumSize3 = sizes4[j9];
                            }
                        } else {
                            editedTimestamps = editedTimestamps2;
                        }
                        sampleIndex2++;
                        j9++;
                        startIndex = startIndex2;
                        endIndex = endIndex2;
                        editedTimestamps2 = editedTimestamps;
                        omitClippedSample = omitClippedSample2;
                        startIndices = startIndices2;
                    }
                    int i21 = startIndex;
                    int i22 = endIndex;
                    boolean z3 = omitClippedSample;
                    int[] iArr3 = startIndices;
                    pts += track2.editListDurations[i19];
                    i19++;
                    sizes3 = sizes4;
                    nextSampleIndex = nextSampleIndex2;
                    editedSampleCount = editedSampleCount2;
                    endIndices2 = endIndices;
                    ctts = ctts2;
                    editedTimestamps2 = editedTimestamps2;
                    int i23 = sampleIndex2;
                    sampleIndex = editedMaximumSize3;
                    editedMaximumSize2 = i23;
                } else {
                    int i24 = editedSampleCount;
                    ParsableByteArray parsableByteArray17 = ctts;
                    int[] iArr4 = sizes3;
                    int[] iArr5 = editedSizes;
                    int[] iArr6 = endIndices2;
                    boolean z4 = omitClippedSample;
                    int[] iArr7 = startIndices;
                    TrackSampleTable trackSampleTable4 = new TrackSampleTable(track, editedOffsets, editedSizes, sampleIndex, editedTimestamps2, editedFlags, Util.scaleLargeTimestamp(pts, 1000000, track2.movieTimescale));
                    return trackSampleTable4;
                }
            }
        }
        Util.scaleLargeTimestampsInPlace(timestamps2, 1000000, track2.timescale);
        TrackSampleTable trackSampleTable5 = new TrackSampleTable(track, offsets2, sizes2, nextSynchronizationSampleIndex2, timestamps2, flags2, durationUs);
        return trackSampleTable5;
    }

    public static Metadata parseUdta(LeafAtom udtaAtom, boolean isQuickTime) {
        if (isQuickTime) {
            return null;
        }
        ParsableByteArray udtaData = udtaAtom.data;
        udtaData.setPosition(8);
        while (udtaData.bytesLeft() >= 8) {
            int atomPosition = udtaData.getPosition();
            int atomSize = udtaData.readInt();
            if (udtaData.readInt() == Atom.TYPE_meta) {
                udtaData.setPosition(atomPosition);
                return parseMetaAtom(udtaData, atomPosition + atomSize);
            }
            udtaData.skipBytes(atomSize - 8);
        }
        return null;
    }

    private static Metadata parseMetaAtom(ParsableByteArray meta, int limit) {
        meta.skipBytes(12);
        while (meta.getPosition() < limit) {
            int atomPosition = meta.getPosition();
            int atomSize = meta.readInt();
            if (meta.readInt() == Atom.TYPE_ilst) {
                meta.setPosition(atomPosition);
                return parseIlst(meta, atomPosition + atomSize);
            }
            meta.skipBytes(atomSize - 8);
        }
        return null;
    }

    private static Metadata parseIlst(ParsableByteArray ilst, int limit) {
        ilst.skipBytes(8);
        ArrayList<Entry> entries = new ArrayList<>();
        while (ilst.getPosition() < limit) {
            Entry entry = MetadataUtil.parseIlstElement(ilst);
            if (entry != null) {
                entries.add(entry);
            }
        }
        if (entries.isEmpty()) {
            return null;
        }
        return new Metadata((List<? extends Entry>) entries);
    }

    private static long parseMvhd(ParsableByteArray mvhd) {
        int i = 8;
        mvhd.setPosition(8);
        if (Atom.parseFullAtomVersion(mvhd.readInt()) != 0) {
            i = 16;
        }
        mvhd.skipBytes(i);
        return mvhd.readUnsignedInt();
    }

    private static TkhdData parseTkhd(ParsableByteArray tkhd) {
        long duration;
        int rotationDegrees;
        int durationByteCount = 8;
        tkhd.setPosition(8);
        int version = Atom.parseFullAtomVersion(tkhd.readInt());
        tkhd.skipBytes(version == 0 ? 8 : 16);
        int trackId = tkhd.readInt();
        tkhd.skipBytes(4);
        boolean durationUnknown = true;
        int durationPosition = tkhd.getPosition();
        if (version == 0) {
            durationByteCount = 4;
        }
        int i = 0;
        while (true) {
            if (i >= durationByteCount) {
                break;
            } else if (tkhd.data[durationPosition + i] != -1) {
                durationUnknown = false;
                break;
            } else {
                i++;
            }
        }
        if (durationUnknown) {
            tkhd.skipBytes(durationByteCount);
            duration = C.TIME_UNSET;
        } else {
            duration = version == 0 ? tkhd.readUnsignedInt() : tkhd.readUnsignedLongToLong();
            if (duration == 0) {
                duration = C.TIME_UNSET;
            }
        }
        tkhd.skipBytes(16);
        int a00 = tkhd.readInt();
        int a01 = tkhd.readInt();
        tkhd.skipBytes(4);
        int a10 = tkhd.readInt();
        int a11 = tkhd.readInt();
        if (a00 == 0 && a01 == 65536 && a10 == (-65536) && a11 == 0) {
            rotationDegrees = 90;
        } else if (a00 == 0 && a01 == (-65536) && a10 == 65536 && a11 == 0) {
            rotationDegrees = 270;
        } else if (a00 == (-65536) && a01 == 0 && a10 == 0 && a11 == (-65536)) {
            rotationDegrees = 180;
        } else {
            rotationDegrees = 0;
        }
        return new TkhdData(trackId, duration, rotationDegrees);
    }

    private static int parseHdlr(ParsableByteArray hdlr) {
        hdlr.setPosition(16);
        int trackType = hdlr.readInt();
        if (trackType == TYPE_soun) {
            return 1;
        }
        if (trackType == TYPE_vide) {
            return 2;
        }
        if (trackType == TYPE_text || trackType == TYPE_sbtl || trackType == TYPE_subt || trackType == TYPE_clcp) {
            return 3;
        }
        if (trackType == TYPE_meta) {
            return 4;
        }
        return -1;
    }

    private static Pair<Long, String> parseMdhd(ParsableByteArray mdhd) {
        int i = 8;
        mdhd.setPosition(8);
        int version = Atom.parseFullAtomVersion(mdhd.readInt());
        mdhd.skipBytes(version == 0 ? 8 : 16);
        long timescale = mdhd.readUnsignedInt();
        if (version == 0) {
            i = 4;
        }
        mdhd.skipBytes(i);
        int languageCode = mdhd.readUnsignedShort();
        StringBuilder sb = new StringBuilder();
        sb.append("");
        sb.append((char) (((languageCode >> 10) & 31) + 96));
        sb.append((char) (((languageCode >> 5) & 31) + 96));
        sb.append((char) ((languageCode & 31) + 96));
        return Pair.create(Long.valueOf(timescale), sb.toString());
    }

    private static StsdData parseStsd(ParsableByteArray stsd, int trackId, int rotationDegrees, String language, DrmInitData drmInitData, boolean isQuickTime) throws ParserException {
        int childAtomType;
        ParsableByteArray parsableByteArray = stsd;
        parsableByteArray.setPosition(12);
        int numberOfEntries = stsd.readInt();
        StsdData out = new StsdData(numberOfEntries);
        for (int i = 0; i < numberOfEntries; i++) {
            int childStartPosition = stsd.getPosition();
            int childAtomSize = stsd.readInt();
            Assertions.checkArgument(childAtomSize > 0, "childAtomSize should be positive");
            int childAtomType2 = stsd.readInt();
            if (childAtomType2 == Atom.TYPE_avc1 || childAtomType2 == Atom.TYPE_avc3 || childAtomType2 == Atom.TYPE_encv || childAtomType2 == Atom.TYPE_mp4v || childAtomType2 == Atom.TYPE_hvc1 || childAtomType2 == Atom.TYPE_hev1 || childAtomType2 == Atom.TYPE_s263 || childAtomType2 == Atom.TYPE_vp08) {
                childAtomType = childAtomType2;
            } else if (childAtomType2 == Atom.TYPE_vp09) {
                childAtomType = childAtomType2;
            } else if (childAtomType2 == Atom.TYPE_mp4a || childAtomType2 == Atom.TYPE_enca || childAtomType2 == Atom.TYPE_ac_3 || childAtomType2 == Atom.TYPE_ec_3 || childAtomType2 == Atom.TYPE_dtsc || childAtomType2 == Atom.TYPE_dtse || childAtomType2 == Atom.TYPE_dtsh || childAtomType2 == Atom.TYPE_dtsl || childAtomType2 == Atom.TYPE_samr || childAtomType2 == Atom.TYPE_sawb || childAtomType2 == Atom.TYPE_lpcm || childAtomType2 == Atom.TYPE_sowt || childAtomType2 == Atom.TYPE__mp3 || childAtomType2 == Atom.TYPE_alac || childAtomType2 == Atom.TYPE_alaw || childAtomType2 == Atom.TYPE_ulaw) {
                int i2 = childAtomType2;
                parseAudioSampleEntry(stsd, childAtomType2, childStartPosition, childAtomSize, trackId, language, isQuickTime, drmInitData, out, i);
                parsableByteArray.setPosition(childStartPosition + childAtomSize);
            } else if (childAtomType2 == Atom.TYPE_TTML || childAtomType2 == Atom.TYPE_tx3g || childAtomType2 == Atom.TYPE_wvtt || childAtomType2 == Atom.TYPE_stpp || childAtomType2 == Atom.TYPE_c608) {
                parseTextSampleEntry(stsd, childAtomType2, childStartPosition, childAtomSize, trackId, language, out);
                int i3 = childAtomType2;
                parsableByteArray.setPosition(childStartPosition + childAtomSize);
            } else {
                if (childAtomType2 == Atom.TYPE_camm) {
                    out.format = Format.createSampleFormat(Integer.toString(trackId), MimeTypes.APPLICATION_CAMERA_MOTION, null, -1, null);
                    int i4 = childAtomType2;
                } else {
                    int i5 = childAtomType2;
                }
                parsableByteArray.setPosition(childStartPosition + childAtomSize);
            }
            parseVideoSampleEntry(stsd, childAtomType, childStartPosition, childAtomSize, trackId, rotationDegrees, drmInitData, out, i);
            parsableByteArray.setPosition(childStartPosition + childAtomSize);
        }
        return out;
    }

    private static void parseTextSampleEntry(ParsableByteArray parent, int atomType, int position, int atomSize, int trackId, String language, StsdData out) throws ParserException {
        String mimeType;
        ParsableByteArray parsableByteArray = parent;
        int i = atomType;
        StsdData stsdData = out;
        parsableByteArray.setPosition(position + 8 + 8);
        List<byte[]> initializationData = null;
        long subsampleOffsetUs = Long.MAX_VALUE;
        if (i == Atom.TYPE_TTML) {
            mimeType = MimeTypes.APPLICATION_TTML;
        } else if (i == Atom.TYPE_tx3g) {
            mimeType = MimeTypes.APPLICATION_TX3G;
            int sampleDescriptionLength = (atomSize - 8) - 8;
            byte[] sampleDescriptionData = new byte[sampleDescriptionLength];
            parsableByteArray.readBytes(sampleDescriptionData, 0, sampleDescriptionLength);
            initializationData = Collections.singletonList(sampleDescriptionData);
        } else if (i == Atom.TYPE_wvtt) {
            mimeType = MimeTypes.APPLICATION_MP4VTT;
        } else if (i == Atom.TYPE_stpp) {
            mimeType = MimeTypes.APPLICATION_TTML;
            subsampleOffsetUs = 0;
        } else if (i == Atom.TYPE_c608) {
            mimeType = MimeTypes.APPLICATION_MP4CEA608;
            stsdData.requiredSampleTransformation = 1;
        } else {
            throw new IllegalStateException();
        }
        stsdData.format = Format.createTextSampleFormat(Integer.toString(trackId), mimeType, null, -1, 0, language, -1, null, subsampleOffsetUs, initializationData);
    }

    /* JADX WARNING: Removed duplicated region for block: B:72:0x015a A[RETURN] */
    /* JADX WARNING: Removed duplicated region for block: B:73:0x015b  */
    private static void parseVideoSampleEntry(ParsableByteArray parent, int atomType, int position, int size, int trackId, int rotationDegrees, DrmInitData drmInitData, StsdData out, int entryIndex) throws ParserException {
        int atomType2;
        DrmInitData drmInitData2;
        ParsableByteArray parsableByteArray = parent;
        int i = position;
        int i2 = size;
        DrmInitData drmInitData3 = drmInitData;
        StsdData stsdData = out;
        parsableByteArray.setPosition(i + 8 + 8);
        parsableByteArray.skipBytes(16);
        int width = parent.readUnsignedShort();
        int height = parent.readUnsignedShort();
        parsableByteArray.skipBytes(50);
        int childPosition = parent.getPosition();
        int atomType3 = atomType;
        if (atomType3 == Atom.TYPE_encv) {
            Pair<Integer, TrackEncryptionBox> sampleEntryEncryptionData = parseSampleEntryEncryptionData(parsableByteArray, i, i2);
            if (sampleEntryEncryptionData != null) {
                atomType3 = ((Integer) sampleEntryEncryptionData.first).intValue();
                if (drmInitData3 == null) {
                    drmInitData2 = null;
                } else {
                    drmInitData2 = drmInitData3.copyWithSchemeType(((TrackEncryptionBox) sampleEntryEncryptionData.second).schemeType);
                }
                drmInitData3 = drmInitData2;
                stsdData.trackEncryptionBoxes[entryIndex] = (TrackEncryptionBox) sampleEntryEncryptionData.second;
            }
            parsableByteArray.setPosition(childPosition);
            atomType2 = atomType3;
        } else {
            atomType2 = atomType3;
        }
        boolean pixelWidthHeightRatioFromPasp = false;
        float pixelWidthHeightRatio = 1.0f;
        int childPosition2 = childPosition;
        List list = null;
        String mimeType = null;
        byte[] projectionData = null;
        int stereoMode = -1;
        while (childPosition2 - i < i2) {
            parsableByteArray.setPosition(childPosition2);
            int childStartPosition = parent.getPosition();
            int childAtomSize = parent.readInt();
            if (childAtomSize != 0 || parent.getPosition() - i != i2) {
                boolean z = true;
                Assertions.checkArgument(childAtomSize > 0, "childAtomSize should be positive");
                int childAtomType = parent.readInt();
                if (childAtomType == Atom.TYPE_avcC) {
                    if (mimeType != null) {
                        z = false;
                    }
                    Assertions.checkState(z);
                    mimeType = MimeTypes.VIDEO_H264;
                    parsableByteArray.setPosition(childStartPosition + 8);
                    AvcConfig avcConfig = AvcConfig.parse(parent);
                    List<byte[]> initializationData = avcConfig.initializationData;
                    stsdData.nalUnitLengthFieldLength = avcConfig.nalUnitLengthFieldLength;
                    if (!pixelWidthHeightRatioFromPasp) {
                        pixelWidthHeightRatio = avcConfig.pixelWidthAspectRatio;
                    }
                    list = initializationData;
                } else if (childAtomType == Atom.TYPE_hvcC) {
                    if (mimeType != null) {
                        z = false;
                    }
                    Assertions.checkState(z);
                    mimeType = MimeTypes.VIDEO_H265;
                    parsableByteArray.setPosition(childStartPosition + 8);
                    HevcConfig hevcConfig = HevcConfig.parse(parent);
                    List<byte[]> initializationData2 = hevcConfig.initializationData;
                    stsdData.nalUnitLengthFieldLength = hevcConfig.nalUnitLengthFieldLength;
                    list = initializationData2;
                } else if (childAtomType == Atom.TYPE_vpcC) {
                    if (mimeType != null) {
                        z = false;
                    }
                    Assertions.checkState(z);
                    mimeType = atomType2 == Atom.TYPE_vp08 ? MimeTypes.VIDEO_VP8 : MimeTypes.VIDEO_VP9;
                } else if (childAtomType == Atom.TYPE_d263) {
                    if (mimeType != null) {
                        z = false;
                    }
                    Assertions.checkState(z);
                    mimeType = MimeTypes.VIDEO_H263;
                } else if (childAtomType == Atom.TYPE_esds) {
                    if (mimeType != null) {
                        z = false;
                    }
                    Assertions.checkState(z);
                    Pair<String, byte[]> mimeTypeAndInitializationData = parseEsdsFromParent(parsableByteArray, childStartPosition);
                    mimeType = (String) mimeTypeAndInitializationData.first;
                    list = Collections.singletonList(mimeTypeAndInitializationData.second);
                } else if (childAtomType == Atom.TYPE_pasp) {
                    pixelWidthHeightRatio = parsePaspFromParent(parsableByteArray, childStartPosition);
                    pixelWidthHeightRatioFromPasp = true;
                } else if (childAtomType == Atom.TYPE_sv3d) {
                    projectionData = parseProjFromParent(parsableByteArray, childStartPosition, childAtomSize);
                } else if (childAtomType == Atom.TYPE_st3d) {
                    int version = parent.readUnsignedByte();
                    parsableByteArray.skipBytes(3);
                    if (version == 0) {
                        switch (parent.readUnsignedByte()) {
                            case 0:
                                stereoMode = 0;
                                break;
                            case 1:
                                stereoMode = 1;
                                break;
                            case 2:
                                stereoMode = 2;
                                break;
                            case 3:
                                stereoMode = 3;
                                break;
                        }
                    }
                }
                childPosition2 += childAtomSize;
            } else if (mimeType == null) {
                int i3 = childPosition2;
                int i4 = atomType2;
                stsdData.format = Format.createVideoSampleFormat(Integer.toString(trackId), mimeType, null, -1, -1, width, height, -1.0f, list, rotationDegrees, pixelWidthHeightRatio, projectionData, stereoMode, null, drmInitData3);
                return;
            } else {
                return;
            }
        }
        if (mimeType == null) {
        }
    }

    private static Pair<long[], long[]> parseEdts(ContainerAtom edtsAtom) {
        if (edtsAtom != null) {
            LeafAtom leafAtomOfType = edtsAtom.getLeafAtomOfType(Atom.TYPE_elst);
            LeafAtom elst = leafAtomOfType;
            if (leafAtomOfType != null) {
                ParsableByteArray elstData = elst.data;
                elstData.setPosition(8);
                int version = Atom.parseFullAtomVersion(elstData.readInt());
                int entryCount = elstData.readUnsignedIntToInt();
                long[] editListDurations = new long[entryCount];
                long[] editListMediaTimes = new long[entryCount];
                int i = 0;
                while (i < entryCount) {
                    editListDurations[i] = version == 1 ? elstData.readUnsignedLongToLong() : elstData.readUnsignedInt();
                    editListMediaTimes[i] = version == 1 ? elstData.readLong() : (long) elstData.readInt();
                    if (elstData.readShort() == 1) {
                        elstData.skipBytes(2);
                        i++;
                    } else {
                        throw new IllegalArgumentException("Unsupported media rate.");
                    }
                }
                return Pair.create(editListDurations, editListMediaTimes);
            }
        }
        return Pair.create(null, null);
    }

    private static float parsePaspFromParent(ParsableByteArray parent, int position) {
        parent.setPosition(position + 8);
        return ((float) parent.readUnsignedIntToInt()) / ((float) parent.readUnsignedIntToInt());
    }

    private static void parseAudioSampleEntry(ParsableByteArray parent, int atomType, int position, int size, int trackId, String language, boolean isQuickTime, DrmInitData drmInitData, StsdData out, int entryIndex) throws ParserException {
        int quickTimeSoundDescriptionVersion;
        int sampleRate;
        int channelCount;
        int atomType2;
        DrmInitData drmInitData2;
        List list;
        int quickTimeSoundDescriptionVersion2;
        int atomType3;
        DrmInitData drmInitData3;
        int quickTimeSoundDescriptionVersion3;
        String mimeType;
        int childAtomType;
        int i;
        int quickTimeSoundDescriptionVersion4;
        String mimeType2;
        DrmInitData drmInitData4;
        ParsableByteArray parsableByteArray = parent;
        int i2 = position;
        int i3 = size;
        String str = language;
        DrmInitData drmInitData5 = drmInitData;
        StsdData stsdData = out;
        parsableByteArray.setPosition(i2 + 8 + 8);
        if (isQuickTime) {
            int quickTimeSoundDescriptionVersion5 = parent.readUnsignedShort();
            parsableByteArray.skipBytes(6);
            quickTimeSoundDescriptionVersion = quickTimeSoundDescriptionVersion5;
        } else {
            parsableByteArray.skipBytes(8);
            quickTimeSoundDescriptionVersion = 0;
        }
        if (quickTimeSoundDescriptionVersion == 0 || quickTimeSoundDescriptionVersion == 1) {
            int channelCount2 = parent.readUnsignedShort();
            parsableByteArray.skipBytes(6);
            sampleRate = parent.readUnsignedFixedPoint1616();
            if (quickTimeSoundDescriptionVersion == 1) {
                parsableByteArray.skipBytes(16);
            }
            channelCount = channelCount2;
        } else if (quickTimeSoundDescriptionVersion == 2) {
            parsableByteArray.skipBytes(16);
            int sampleRate2 = (int) Math.round(parent.readDouble());
            channelCount = parent.readUnsignedIntToInt();
            parsableByteArray.skipBytes(20);
            sampleRate = sampleRate2;
        } else {
            return;
        }
        int childPosition = parent.getPosition();
        int atomType4 = atomType;
        if (atomType4 == Atom.TYPE_enca) {
            Pair<Integer, TrackEncryptionBox> sampleEntryEncryptionData = parseSampleEntryEncryptionData(parsableByteArray, i2, i3);
            if (sampleEntryEncryptionData != null) {
                atomType4 = ((Integer) sampleEntryEncryptionData.first).intValue();
                if (drmInitData5 == null) {
                    drmInitData4 = null;
                } else {
                    drmInitData4 = drmInitData5.copyWithSchemeType(((TrackEncryptionBox) sampleEntryEncryptionData.second).schemeType);
                }
                drmInitData5 = drmInitData4;
                stsdData.trackEncryptionBoxes[entryIndex] = (TrackEncryptionBox) sampleEntryEncryptionData.second;
            }
            parsableByteArray.setPosition(childPosition);
            drmInitData2 = drmInitData5;
            atomType2 = atomType4;
        } else {
            drmInitData2 = drmInitData5;
            atomType2 = atomType4;
        }
        String mimeType3 = null;
        if (atomType2 == Atom.TYPE_ac_3) {
            mimeType3 = MimeTypes.AUDIO_AC3;
        } else if (atomType2 == Atom.TYPE_ec_3) {
            mimeType3 = MimeTypes.AUDIO_E_AC3;
        } else if (atomType2 == Atom.TYPE_dtsc) {
            mimeType3 = MimeTypes.AUDIO_DTS;
        } else if (atomType2 == Atom.TYPE_dtsh || atomType2 == Atom.TYPE_dtsl) {
            mimeType3 = MimeTypes.AUDIO_DTS_HD;
        } else if (atomType2 == Atom.TYPE_dtse) {
            mimeType3 = MimeTypes.AUDIO_DTS_EXPRESS;
        } else if (atomType2 == Atom.TYPE_samr) {
            mimeType3 = MimeTypes.AUDIO_AMR_NB;
        } else if (atomType2 == Atom.TYPE_sawb) {
            mimeType3 = MimeTypes.AUDIO_AMR_WB;
        } else if (atomType2 == Atom.TYPE_lpcm || atomType2 == Atom.TYPE_sowt) {
            mimeType3 = MimeTypes.AUDIO_RAW;
        } else if (atomType2 == Atom.TYPE__mp3) {
            mimeType3 = MimeTypes.AUDIO_MPEG;
        } else if (atomType2 == Atom.TYPE_alac) {
            mimeType3 = MimeTypes.AUDIO_ALAC;
        } else if (atomType2 == Atom.TYPE_alaw) {
            mimeType3 = MimeTypes.AUDIO_ALAW;
        } else if (atomType2 == Atom.TYPE_ulaw) {
            mimeType3 = MimeTypes.AUDIO_MLAW;
        }
        int channelCount3 = channelCount;
        int childPosition2 = childPosition;
        int sampleRate3 = sampleRate;
        byte[] initializationData = null;
        String mimeType4 = mimeType3;
        while (childPosition2 - i2 < i3) {
            parsableByteArray.setPosition(childPosition2);
            int childAtomSize = parent.readInt();
            Assertions.checkArgument(childAtomSize > 0, "childAtomSize should be positive");
            int childAtomType2 = parent.readInt();
            if (childAtomType2 == Atom.TYPE_esds) {
                mimeType = mimeType4;
                drmInitData3 = drmInitData2;
                atomType3 = atomType2;
                childAtomType = childAtomType2;
                quickTimeSoundDescriptionVersion2 = quickTimeSoundDescriptionVersion;
                quickTimeSoundDescriptionVersion4 = childPosition2;
            } else if (!isQuickTime || childAtomType2 != Atom.TYPE_wave) {
                if (childAtomType2 == Atom.TYPE_dac3) {
                    parsableByteArray.setPosition(childPosition2 + 8);
                    stsdData.format = Ac3Util.parseAc3AnnexFFormat(parsableByteArray, Integer.toString(trackId), str, drmInitData2);
                    mimeType2 = mimeType4;
                    drmInitData3 = drmInitData2;
                    atomType3 = atomType2;
                    int i4 = childAtomType2;
                    quickTimeSoundDescriptionVersion2 = quickTimeSoundDescriptionVersion;
                    quickTimeSoundDescriptionVersion3 = childPosition2;
                } else if (childAtomType2 == Atom.TYPE_dec3) {
                    parsableByteArray.setPosition(childPosition2 + 8);
                    stsdData.format = Ac3Util.parseEAc3AnnexFFormat(parsableByteArray, Integer.toString(trackId), str, drmInitData2);
                    mimeType2 = mimeType4;
                    drmInitData3 = drmInitData2;
                    atomType3 = atomType2;
                    int i5 = childAtomType2;
                    quickTimeSoundDescriptionVersion2 = quickTimeSoundDescriptionVersion;
                    quickTimeSoundDescriptionVersion3 = childPosition2;
                } else if (childAtomType2 == Atom.TYPE_ddts) {
                    int childAtomSize2 = childAtomSize;
                    mimeType2 = mimeType4;
                    int childPosition3 = childPosition2;
                    drmInitData3 = drmInitData2;
                    atomType3 = atomType2;
                    int childAtomType3 = childAtomType2;
                    quickTimeSoundDescriptionVersion2 = quickTimeSoundDescriptionVersion;
                    stsdData.format = Format.createAudioSampleFormat(Integer.toString(trackId), mimeType4, null, -1, -1, channelCount3, sampleRate3, null, drmInitData3, 0, language);
                    childAtomSize = childAtomSize2;
                    quickTimeSoundDescriptionVersion3 = childPosition3;
                    int i6 = childAtomType3;
                } else {
                    int childAtomSize3 = childAtomSize;
                    mimeType2 = mimeType4;
                    int childPosition4 = childPosition2;
                    drmInitData3 = drmInitData2;
                    atomType3 = atomType2;
                    quickTimeSoundDescriptionVersion2 = quickTimeSoundDescriptionVersion;
                    if (childAtomType2 == Atom.TYPE_alac) {
                        childAtomSize = childAtomSize3;
                        byte[] initializationData2 = new byte[childAtomSize];
                        quickTimeSoundDescriptionVersion3 = childPosition4;
                        parsableByteArray.setPosition(quickTimeSoundDescriptionVersion3);
                        parsableByteArray.readBytes(initializationData2, 0, childAtomSize);
                        initializationData = initializationData2;
                        mimeType4 = mimeType2;
                        childPosition2 = quickTimeSoundDescriptionVersion3 + childAtomSize;
                        drmInitData2 = drmInitData3;
                        atomType2 = atomType3;
                        quickTimeSoundDescriptionVersion = quickTimeSoundDescriptionVersion2;
                    } else {
                        childAtomSize = childAtomSize3;
                        quickTimeSoundDescriptionVersion3 = childPosition4;
                    }
                }
                mimeType4 = mimeType2;
                childPosition2 = quickTimeSoundDescriptionVersion3 + childAtomSize;
                drmInitData2 = drmInitData3;
                atomType2 = atomType3;
                quickTimeSoundDescriptionVersion = quickTimeSoundDescriptionVersion2;
            } else {
                mimeType = mimeType4;
                drmInitData3 = drmInitData2;
                atomType3 = atomType2;
                childAtomType = childAtomType2;
                quickTimeSoundDescriptionVersion2 = quickTimeSoundDescriptionVersion;
                quickTimeSoundDescriptionVersion4 = childPosition2;
            }
            if (childAtomType == Atom.TYPE_esds) {
                i = quickTimeSoundDescriptionVersion3;
            } else {
                i = findEsdsPosition(parsableByteArray, quickTimeSoundDescriptionVersion3, childAtomSize);
            }
            int esdsAtomPosition = i;
            if (esdsAtomPosition != -1) {
                Pair<String, byte[]> mimeTypeAndInitializationData = parseEsdsFromParent(parsableByteArray, esdsAtomPosition);
                mimeType4 = (String) mimeTypeAndInitializationData.first;
                initializationData = (byte[]) mimeTypeAndInitializationData.second;
                if (MimeTypes.AUDIO_AAC.equals(mimeType4)) {
                    Pair<Integer, Integer> audioSpecificConfig = CodecSpecificDataUtil.parseAacAudioSpecificConfig(initializationData);
                    sampleRate3 = ((Integer) audioSpecificConfig.first).intValue();
                    channelCount3 = ((Integer) audioSpecificConfig.second).intValue();
                }
            } else {
                mimeType4 = mimeType;
            }
            childPosition2 = quickTimeSoundDescriptionVersion3 + childAtomSize;
            drmInitData2 = drmInitData3;
            atomType2 = atomType3;
            quickTimeSoundDescriptionVersion = quickTimeSoundDescriptionVersion2;
        }
        String mimeType5 = mimeType4;
        DrmInitData drmInitData6 = drmInitData2;
        int i7 = atomType2;
        int i8 = quickTimeSoundDescriptionVersion;
        int childPosition5 = childPosition2;
        if (stsdData.format == null) {
            String mimeType6 = mimeType5;
            if (mimeType6 != null) {
                int pcmEncoding = MimeTypes.AUDIO_RAW.equals(mimeType6) ? 2 : -1;
                String num = Integer.toString(trackId);
                if (initializationData == null) {
                    list = null;
                } else {
                    list = Collections.singletonList(initializationData);
                }
                String str2 = mimeType6;
                int i9 = childPosition5;
                stsdData.format = Format.createAudioSampleFormat(num, mimeType6, null, -1, -1, channelCount3, sampleRate3, pcmEncoding, list, drmInitData6, 0, language);
            } else {
                int i10 = childPosition5;
                StsdData stsdData2 = stsdData;
            }
        } else {
            StsdData stsdData3 = stsdData;
            String str3 = mimeType5;
        }
    }

    private static int findEsdsPosition(ParsableByteArray parent, int position, int size) {
        int childAtomPosition = parent.getPosition();
        while (childAtomPosition - position < size) {
            parent.setPosition(childAtomPosition);
            int childAtomSize = parent.readInt();
            Assertions.checkArgument(childAtomSize > 0, "childAtomSize should be positive");
            if (parent.readInt() == Atom.TYPE_esds) {
                return childAtomPosition;
            }
            childAtomPosition += childAtomSize;
        }
        return -1;
    }

    private static Pair<String, byte[]> parseEsdsFromParent(ParsableByteArray parent, int position) {
        parent.setPosition(position + 8 + 4);
        parent.skipBytes(1);
        parseExpandableClassSize(parent);
        parent.skipBytes(2);
        int flags = parent.readUnsignedByte();
        if ((flags & 128) != 0) {
            parent.skipBytes(2);
        }
        if ((flags & 64) != 0) {
            parent.skipBytes(parent.readUnsignedShort());
        }
        if ((flags & 32) != 0) {
            parent.skipBytes(2);
        }
        parent.skipBytes(1);
        parseExpandableClassSize(parent);
        String mimeType = MimeTypes.getMimeTypeFromMp4ObjectType(parent.readUnsignedByte());
        if (MimeTypes.AUDIO_MPEG.equals(mimeType) || MimeTypes.AUDIO_DTS.equals(mimeType) || MimeTypes.AUDIO_DTS_HD.equals(mimeType)) {
            return Pair.create(mimeType, null);
        }
        parent.skipBytes(12);
        parent.skipBytes(1);
        int initializationDataSize = parseExpandableClassSize(parent);
        byte[] initializationData = new byte[initializationDataSize];
        parent.readBytes(initializationData, 0, initializationDataSize);
        return Pair.create(mimeType, initializationData);
    }

    private static Pair<Integer, TrackEncryptionBox> parseSampleEntryEncryptionData(ParsableByteArray parent, int position, int size) {
        int childPosition = parent.getPosition();
        while (childPosition - position < size) {
            parent.setPosition(childPosition);
            int childAtomSize = parent.readInt();
            Assertions.checkArgument(childAtomSize > 0, "childAtomSize should be positive");
            if (parent.readInt() == Atom.TYPE_sinf) {
                Pair<Integer, TrackEncryptionBox> result = parseCommonEncryptionSinfFromParent(parent, childPosition, childAtomSize);
                if (result != null) {
                    return result;
                }
            }
            childPosition += childAtomSize;
        }
        return null;
    }

    static Pair<Integer, TrackEncryptionBox> parseCommonEncryptionSinfFromParent(ParsableByteArray parent, int position, int size) {
        int childPosition = position + 8;
        int schemeInformationBoxPosition = -1;
        int schemeInformationBoxSize = 0;
        String schemeType = null;
        Integer dataFormat = null;
        while (childPosition - position < size) {
            parent.setPosition(childPosition);
            int childAtomSize = parent.readInt();
            int childAtomType = parent.readInt();
            if (childAtomType == Atom.TYPE_frma) {
                dataFormat = Integer.valueOf(parent.readInt());
            } else if (childAtomType == Atom.TYPE_schm) {
                parent.skipBytes(4);
                schemeType = parent.readString(4);
            } else if (childAtomType == Atom.TYPE_schi) {
                schemeInformationBoxPosition = childPosition;
                schemeInformationBoxSize = childAtomSize;
            }
            childPosition += childAtomSize;
        }
        if (!C.CENC_TYPE_cenc.equals(schemeType) && !C.CENC_TYPE_cbc1.equals(schemeType) && !C.CENC_TYPE_cens.equals(schemeType) && !C.CENC_TYPE_cbcs.equals(schemeType)) {
            return null;
        }
        boolean z = true;
        Assertions.checkArgument(dataFormat != null, "frma atom is mandatory");
        Assertions.checkArgument(schemeInformationBoxPosition != -1, "schi atom is mandatory");
        TrackEncryptionBox encryptionBox = parseSchiFromParent(parent, schemeInformationBoxPosition, schemeInformationBoxSize, schemeType);
        if (encryptionBox == null) {
            z = false;
        }
        Assertions.checkArgument(z, "tenc atom is mandatory");
        return Pair.create(dataFormat, encryptionBox);
    }

    private static TrackEncryptionBox parseSchiFromParent(ParsableByteArray parent, int position, int size, String schemeType) {
        byte[] constantIv;
        ParsableByteArray parsableByteArray = parent;
        int childPosition = position + 8;
        while (childPosition - position < size) {
            parsableByteArray.setPosition(childPosition);
            int childAtomSize = parent.readInt();
            if (parent.readInt() == Atom.TYPE_tenc) {
                int version = Atom.parseFullAtomVersion(parent.readInt());
                boolean defaultIsProtected = true;
                parsableByteArray.skipBytes(1);
                int defaultCryptByteBlock = 0;
                int defaultSkipByteBlock = 0;
                if (version == 0) {
                    parsableByteArray.skipBytes(1);
                } else {
                    int patternByte = parent.readUnsignedByte();
                    defaultCryptByteBlock = (patternByte & PsExtractor.VIDEO_STREAM_MASK) >> 4;
                    defaultSkipByteBlock = patternByte & 15;
                }
                if (parent.readUnsignedByte() != 1) {
                    defaultIsProtected = false;
                }
                int defaultPerSampleIvSize = parent.readUnsignedByte();
                byte[] defaultKeyId = new byte[16];
                parsableByteArray.readBytes(defaultKeyId, 0, defaultKeyId.length);
                if (!defaultIsProtected || defaultPerSampleIvSize != 0) {
                    constantIv = null;
                } else {
                    int constantIvSize = parent.readUnsignedByte();
                    byte[] constantIv2 = new byte[constantIvSize];
                    parsableByteArray.readBytes(constantIv2, 0, constantIvSize);
                    constantIv = constantIv2;
                }
                byte[] bArr = defaultKeyId;
                TrackEncryptionBox trackEncryptionBox = new TrackEncryptionBox(defaultIsProtected, schemeType, defaultPerSampleIvSize, defaultKeyId, defaultCryptByteBlock, defaultSkipByteBlock, constantIv);
                return trackEncryptionBox;
            }
            childPosition += childAtomSize;
        }
        return null;
    }

    private static byte[] parseProjFromParent(ParsableByteArray parent, int position, int size) {
        int childPosition = position + 8;
        while (childPosition - position < size) {
            parent.setPosition(childPosition);
            int childAtomSize = parent.readInt();
            if (parent.readInt() == Atom.TYPE_proj) {
                return Arrays.copyOfRange(parent.data, childPosition, childPosition + childAtomSize);
            }
            childPosition += childAtomSize;
        }
        return null;
    }

    private static int parseExpandableClassSize(ParsableByteArray data) {
        int currentByte = data.readUnsignedByte();
        int size = currentByte & 127;
        while ((currentByte & 128) == 128) {
            currentByte = data.readUnsignedByte();
            size = (size << 7) | (currentByte & 127);
        }
        return size;
    }

    private static boolean canApplyEditWithGaplessInfo(long[] timestamps, long duration, long editStartTime, long editEndTime) {
        int lastIndex = timestamps.length - 1;
        int latestDelayIndex = Util.constrainValue(3, 0, lastIndex);
        int earliestPaddingIndex = Util.constrainValue(timestamps.length - 3, 0, lastIndex);
        if (timestamps[0] > editStartTime || editStartTime >= timestamps[latestDelayIndex] || timestamps[earliestPaddingIndex] >= editEndTime || editEndTime > duration) {
            return false;
        }
        return true;
    }

    private AtomParsers() {
    }
}
