package com.google.android.exoplayer2.extractor.mp4;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.GaplessInfoHolder;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.extractor.SeekMap.SeekPoints;
import com.google.android.exoplayer2.extractor.SeekPoint;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.NalUnitUtil;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class Mp4Extractor implements Extractor, SeekMap {
    private static final int BRAND_QUICKTIME = Util.getIntegerCodeForString("qt  ");
    public static final ExtractorsFactory FACTORY = $$Lambda$Mp4Extractor$quy71uYOGsneho91FZy1d2UGE1Q.INSTANCE;
    public static final int FLAG_WORKAROUND_IGNORE_EDIT_LISTS = 1;
    private static final long MAXIMUM_READ_AHEAD_BYTES_STREAM = 10485760;
    private static final long RELOAD_MINIMUM_SEEK_DISTANCE = 262144;
    private static final int STATE_READING_ATOM_HEADER = 0;
    private static final int STATE_READING_ATOM_PAYLOAD = 1;
    private static final int STATE_READING_SAMPLE = 2;
    private long[][] accumulatedSampleSizes;
    private ParsableByteArray atomData;
    private final ParsableByteArray atomHeader;
    private int atomHeaderBytesRead;
    private long atomSize;
    private int atomType;
    private final ArrayDeque<ContainerAtom> containerAtoms;
    private long durationUs;
    private ExtractorOutput extractorOutput;
    private int firstVideoTrackIndex;
    private final int flags;
    private boolean isQuickTime;
    private final ParsableByteArray nalLength;
    private final ParsableByteArray nalStartCode;
    private int parserState;
    private int sampleBytesWritten;
    private int sampleCurrentNalBytesRemaining;
    private int sampleTrackIndex;
    private Mp4Track[] tracks;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Flags {
    }

    private static final class Mp4Track {
        public int sampleIndex;
        public final TrackSampleTable sampleTable;
        public final Track track;
        public final TrackOutput trackOutput;

        public Mp4Track(Track track2, TrackSampleTable sampleTable2, TrackOutput trackOutput2) {
            this.track = track2;
            this.sampleTable = sampleTable2;
            this.trackOutput = trackOutput2;
        }
    }

    static /* synthetic */ Extractor[] lambda$static$0() {
        return new Extractor[]{new Mp4Extractor()};
    }

    public Mp4Extractor() {
        this(0);
    }

    public Mp4Extractor(int flags2) {
        this.flags = flags2;
        this.atomHeader = new ParsableByteArray(16);
        this.containerAtoms = new ArrayDeque<>();
        this.nalStartCode = new ParsableByteArray(NalUnitUtil.NAL_START_CODE);
        this.nalLength = new ParsableByteArray(4);
        this.sampleTrackIndex = -1;
    }

    public boolean sniff(ExtractorInput input) throws IOException, InterruptedException {
        return Sniffer.sniffUnfragmented(input);
    }

    public void init(ExtractorOutput output) {
        this.extractorOutput = output;
    }

    public void seek(long position, long timeUs) {
        this.containerAtoms.clear();
        this.atomHeaderBytesRead = 0;
        this.sampleTrackIndex = -1;
        this.sampleBytesWritten = 0;
        this.sampleCurrentNalBytesRemaining = 0;
        if (position == 0) {
            enterReadingAtomHeaderState();
        } else if (this.tracks != null) {
            updateSampleIndices(timeUs);
        }
    }

    public void release() {
    }

    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException, InterruptedException {
        while (true) {
            switch (this.parserState) {
                case 0:
                    if (readAtomHeader(input)) {
                        break;
                    } else {
                        return -1;
                    }
                case 1:
                    if (!readAtomPayload(input, seekPosition)) {
                        break;
                    } else {
                        return 1;
                    }
                case 2:
                    return readSample(input, seekPosition);
                default:
                    throw new IllegalStateException();
            }
        }
    }

    public boolean isSeekable() {
        return true;
    }

    public long getDurationUs() {
        return this.durationUs;
    }

    public SeekPoints getSeekPoints(long timeUs) {
        long firstOffset;
        long firstTimeUs;
        long j = timeUs;
        Mp4Track[] mp4TrackArr = this.tracks;
        if (mp4TrackArr.length == 0) {
            return new SeekPoints(SeekPoint.START);
        }
        long secondTimeUs = C.TIME_UNSET;
        long secondOffset = -1;
        int i = this.firstVideoTrackIndex;
        if (i != -1) {
            TrackSampleTable sampleTable = mp4TrackArr[i].sampleTable;
            int sampleIndex = getSynchronizationSampleIndex(sampleTable, j);
            if (sampleIndex == -1) {
                return new SeekPoints(SeekPoint.START);
            }
            long sampleTimeUs = sampleTable.timestampsUs[sampleIndex];
            firstTimeUs = sampleTimeUs;
            firstOffset = sampleTable.offsets[sampleIndex];
            if (sampleTimeUs < j && sampleIndex < sampleTable.sampleCount - 1) {
                int secondSampleIndex = sampleTable.getIndexOfLaterOrEqualSynchronizationSample(j);
                if (!(secondSampleIndex == -1 || secondSampleIndex == sampleIndex)) {
                    secondTimeUs = sampleTable.timestampsUs[secondSampleIndex];
                    secondOffset = sampleTable.offsets[secondSampleIndex];
                }
            }
        } else {
            firstTimeUs = timeUs;
            firstOffset = Long.MAX_VALUE;
        }
        int i2 = 0;
        long secondOffset2 = secondOffset;
        long firstOffset2 = firstOffset;
        while (true) {
            Mp4Track[] mp4TrackArr2 = this.tracks;
            if (i2 >= mp4TrackArr2.length) {
                break;
            }
            if (i2 != this.firstVideoTrackIndex) {
                TrackSampleTable sampleTable2 = mp4TrackArr2[i2].sampleTable;
                firstOffset2 = maybeAdjustSeekOffset(sampleTable2, firstTimeUs, firstOffset2);
                if (secondTimeUs != C.TIME_UNSET) {
                    secondOffset2 = maybeAdjustSeekOffset(sampleTable2, secondTimeUs, secondOffset2);
                }
            }
            i2++;
        }
        SeekPoint firstSeekPoint = new SeekPoint(firstTimeUs, firstOffset2);
        if (secondTimeUs == C.TIME_UNSET) {
            return new SeekPoints(firstSeekPoint);
        }
        return new SeekPoints(firstSeekPoint, new SeekPoint(secondTimeUs, secondOffset2));
    }

    private void enterReadingAtomHeaderState() {
        this.parserState = 0;
        this.atomHeaderBytesRead = 0;
    }

    private boolean readAtomHeader(ExtractorInput input) throws IOException, InterruptedException {
        if (this.atomHeaderBytesRead == 0) {
            if (!input.readFully(this.atomHeader.data, 0, 8, true)) {
                return false;
            }
            this.atomHeaderBytesRead = 8;
            this.atomHeader.setPosition(0);
            this.atomSize = this.atomHeader.readUnsignedInt();
            this.atomType = this.atomHeader.readInt();
        }
        long j = this.atomSize;
        if (j == 1) {
            input.readFully(this.atomHeader.data, 8, 8);
            this.atomHeaderBytesRead += 8;
            this.atomSize = this.atomHeader.readUnsignedLongToLong();
        } else if (j == 0) {
            long endPosition = input.getLength();
            if (endPosition == -1 && !this.containerAtoms.isEmpty()) {
                endPosition = ((ContainerAtom) this.containerAtoms.peek()).endPosition;
            }
            if (endPosition != -1) {
                this.atomSize = (endPosition - input.getPosition()) + ((long) this.atomHeaderBytesRead);
            }
        }
        if (this.atomSize >= ((long) this.atomHeaderBytesRead)) {
            if (shouldParseContainerAtom(this.atomType)) {
                long endPosition2 = (input.getPosition() + this.atomSize) - ((long) this.atomHeaderBytesRead);
                this.containerAtoms.push(new ContainerAtom(this.atomType, endPosition2));
                if (this.atomSize == ((long) this.atomHeaderBytesRead)) {
                    processAtomEnded(endPosition2);
                } else {
                    enterReadingAtomHeaderState();
                }
            } else if (shouldParseLeafAtom(this.atomType)) {
                Assertions.checkState(this.atomHeaderBytesRead == 8);
                Assertions.checkState(this.atomSize <= 2147483647L);
                this.atomData = new ParsableByteArray((int) this.atomSize);
                System.arraycopy(this.atomHeader.data, 0, this.atomData.data, 0, 8);
                this.parserState = 1;
            } else {
                this.atomData = null;
                this.parserState = 1;
            }
            return true;
        }
        throw new ParserException("Atom size less than header length (unsupported).");
    }

    private boolean readAtomPayload(ExtractorInput input, PositionHolder positionHolder) throws IOException, InterruptedException {
        long atomPayloadSize = this.atomSize - ((long) this.atomHeaderBytesRead);
        long atomEndPosition = input.getPosition() + atomPayloadSize;
        boolean seekRequired = false;
        ParsableByteArray parsableByteArray = this.atomData;
        if (parsableByteArray != null) {
            input.readFully(parsableByteArray.data, this.atomHeaderBytesRead, (int) atomPayloadSize);
            if (this.atomType == Atom.TYPE_ftyp) {
                this.isQuickTime = processFtypAtom(this.atomData);
            } else if (!this.containerAtoms.isEmpty()) {
                ((ContainerAtom) this.containerAtoms.peek()).add(new LeafAtom(this.atomType, this.atomData));
            }
        } else if (atomPayloadSize < 262144) {
            input.skipFully((int) atomPayloadSize);
        } else {
            positionHolder.position = input.getPosition() + atomPayloadSize;
            seekRequired = true;
        }
        processAtomEnded(atomEndPosition);
        return seekRequired && this.parserState != 2;
    }

    private void processAtomEnded(long atomEndPosition) throws ParserException {
        while (!this.containerAtoms.isEmpty() && ((ContainerAtom) this.containerAtoms.peek()).endPosition == atomEndPosition) {
            ContainerAtom containerAtom = (ContainerAtom) this.containerAtoms.pop();
            if (containerAtom.type == Atom.TYPE_moov) {
                processMoovAtom(containerAtom);
                this.containerAtoms.clear();
                this.parserState = 2;
            } else if (!this.containerAtoms.isEmpty()) {
                ((ContainerAtom) this.containerAtoms.peek()).add(containerAtom);
            }
        }
        if (this.parserState != 2) {
            enterReadingAtomHeaderState();
        }
    }

    private void processMoovAtom(ContainerAtom moov) throws ParserException {
        ContainerAtom containerAtom = moov;
        int firstVideoTrackIndex2 = -1;
        long durationUs2 = C.TIME_UNSET;
        List<Mp4Track> tracks2 = new ArrayList<>();
        Metadata metadata = null;
        GaplessInfoHolder gaplessInfoHolder = new GaplessInfoHolder();
        LeafAtom udta = containerAtom.getLeafAtomOfType(Atom.TYPE_udta);
        if (udta != null) {
            metadata = AtomParsers.parseUdta(udta, this.isQuickTime);
            if (metadata != null) {
                gaplessInfoHolder.setFromMetadata(metadata);
            }
        }
        boolean ignoreEditLists = (this.flags & 1) != 0;
        ArrayList<TrackSampleTable> trackSampleTables = getTrackSampleTables(containerAtom, gaplessInfoHolder, ignoreEditLists);
        int trackCount = trackSampleTables.size();
        int i = 0;
        while (i < trackCount) {
            TrackSampleTable trackSampleTable = (TrackSampleTable) trackSampleTables.get(i);
            Track track = trackSampleTable.track;
            LeafAtom udta2 = udta;
            Mp4Track mp4Track = new Mp4Track(track, trackSampleTable, this.extractorOutput.track(i, track.type));
            int maxInputSize = trackSampleTable.maximumSize + 30;
            Format format = track.format.copyWithMaxInputSize(maxInputSize);
            int i2 = maxInputSize;
            boolean ignoreEditLists2 = ignoreEditLists;
            if (track.type == 1) {
                if (gaplessInfoHolder.hasGaplessInfo()) {
                    format = format.copyWithGaplessInfo(gaplessInfoHolder.encoderDelay, gaplessInfoHolder.encoderPadding);
                }
                if (metadata != null) {
                    format = format.copyWithMetadata(metadata);
                }
            }
            mp4Track.trackOutput.format(format);
            durationUs2 = Math.max(durationUs2, track.durationUs != C.TIME_UNSET ? track.durationUs : trackSampleTable.durationUs);
            if (track.type == 2 && firstVideoTrackIndex2 == -1) {
                firstVideoTrackIndex2 = tracks2.size();
            }
            tracks2.add(mp4Track);
            i++;
            udta = udta2;
            ignoreEditLists = ignoreEditLists2;
            ContainerAtom containerAtom2 = moov;
        }
        boolean z = ignoreEditLists;
        this.firstVideoTrackIndex = firstVideoTrackIndex2;
        this.durationUs = durationUs2;
        this.tracks = (Mp4Track[]) tracks2.toArray(new Mp4Track[tracks2.size()]);
        this.accumulatedSampleSizes = calculateAccumulatedSampleSizes(this.tracks);
        this.extractorOutput.endTracks();
        this.extractorOutput.seekMap(this);
    }

    private ArrayList<TrackSampleTable> getTrackSampleTables(ContainerAtom moov, GaplessInfoHolder gaplessInfoHolder, boolean ignoreEditLists) throws ParserException {
        ArrayList<TrackSampleTable> trackSampleTables = new ArrayList<>();
        for (int i = 0; i < moov.containerChildren.size(); i++) {
            ContainerAtom atom = (ContainerAtom) moov.containerChildren.get(i);
            if (atom.type == Atom.TYPE_trak) {
                Track track = AtomParsers.parseTrak(atom, moov.getLeafAtomOfType(Atom.TYPE_mvhd), C.TIME_UNSET, null, ignoreEditLists, this.isQuickTime);
                if (track != null) {
                    TrackSampleTable trackSampleTable = AtomParsers.parseStbl(track, atom.getContainerAtomOfType(Atom.TYPE_mdia).getContainerAtomOfType(Atom.TYPE_minf).getContainerAtomOfType(Atom.TYPE_stbl), gaplessInfoHolder);
                    if (trackSampleTable.sampleCount != 0) {
                        trackSampleTables.add(trackSampleTable);
                    }
                }
            }
        }
        return trackSampleTables;
    }

    private int readSample(ExtractorInput input, PositionHolder positionHolder) throws IOException, InterruptedException {
        long position;
        long skipAmount;
        int i;
        int sampleSize;
        ExtractorInput extractorInput = input;
        long inputPosition = input.getPosition();
        if (this.sampleTrackIndex == -1) {
            this.sampleTrackIndex = getTrackIndexOfNextReadSample(inputPosition);
            if (this.sampleTrackIndex == -1) {
                return -1;
            }
        }
        Mp4Track track = this.tracks[this.sampleTrackIndex];
        TrackOutput trackOutput = track.trackOutput;
        int sampleIndex = track.sampleIndex;
        long position2 = track.sampleTable.offsets[sampleIndex];
        int sampleSize2 = track.sampleTable.sizes[sampleIndex];
        long skipAmount2 = (position2 - inputPosition) + ((long) this.sampleBytesWritten);
        if (skipAmount2 < 0) {
            position = position2;
        } else if (skipAmount2 >= 262144) {
            long j = inputPosition;
            position = position2;
        } else {
            if (track.track.sampleTransformation == 1) {
                sampleSize2 -= 8;
                skipAmount = skipAmount2 + 8;
            } else {
                skipAmount = skipAmount2;
            }
            extractorInput.skipFully((int) skipAmount);
            if (track.track.nalUnitLengthFieldLength != 0) {
                byte[] nalLengthData = this.nalLength.data;
                nalLengthData[0] = 0;
                nalLengthData[1] = 0;
                nalLengthData[2] = 0;
                int nalUnitLengthFieldLength = track.track.nalUnitLengthFieldLength;
                int nalUnitLengthFieldLengthDiff = 4 - track.track.nalUnitLengthFieldLength;
                while (this.sampleBytesWritten < sampleSize2) {
                    int i2 = this.sampleCurrentNalBytesRemaining;
                    if (i2 == 0) {
                        extractorInput.readFully(this.nalLength.data, nalUnitLengthFieldLengthDiff, nalUnitLengthFieldLength);
                        long inputPosition2 = inputPosition;
                        this.nalLength.setPosition(0);
                        this.sampleCurrentNalBytesRemaining = this.nalLength.readUnsignedIntToInt();
                        this.nalStartCode.setPosition(0);
                        trackOutput.sampleData(this.nalStartCode, 4);
                        this.sampleBytesWritten += 4;
                        sampleSize2 += nalUnitLengthFieldLengthDiff;
                        inputPosition = inputPosition2;
                    } else {
                        long inputPosition3 = inputPosition;
                        int writtenBytes = trackOutput.sampleData(extractorInput, i2, false);
                        this.sampleBytesWritten += writtenBytes;
                        this.sampleCurrentNalBytesRemaining -= writtenBytes;
                        inputPosition = inputPosition3;
                    }
                }
                sampleSize = sampleSize2;
                i = 0;
            } else {
                while (true) {
                    int i3 = this.sampleBytesWritten;
                    if (i3 >= sampleSize2) {
                        break;
                    }
                    int writtenBytes2 = trackOutput.sampleData(extractorInput, sampleSize2 - i3, false);
                    this.sampleBytesWritten += writtenBytes2;
                    this.sampleCurrentNalBytesRemaining -= writtenBytes2;
                }
                i = 0;
                sampleSize = sampleSize2;
            }
            long j2 = skipAmount;
            long j3 = position2;
            trackOutput.sampleMetadata(track.sampleTable.timestampsUs[sampleIndex], track.sampleTable.flags[sampleIndex], sampleSize, 0, null);
            track.sampleIndex++;
            this.sampleTrackIndex = -1;
            this.sampleBytesWritten = i;
            this.sampleCurrentNalBytesRemaining = i;
            return i;
        }
        positionHolder.position = position;
        return 1;
    }

    private int getTrackIndexOfNextReadSample(long inputPosition) {
        long preferredSkipAmount = Long.MAX_VALUE;
        boolean preferredRequiresReload = true;
        int preferredTrackIndex = -1;
        long preferredAccumulatedBytes = Long.MAX_VALUE;
        long minAccumulatedBytes = Long.MAX_VALUE;
        boolean minAccumulatedBytesRequiresReload = true;
        int minAccumulatedBytesTrackIndex = -1;
        int trackIndex = 0;
        while (true) {
            Mp4Track[] mp4TrackArr = this.tracks;
            if (trackIndex >= mp4TrackArr.length) {
                break;
            }
            Mp4Track track = mp4TrackArr[trackIndex];
            int sampleIndex = track.sampleIndex;
            if (sampleIndex != track.sampleTable.sampleCount) {
                long sampleOffset = track.sampleTable.offsets[sampleIndex];
                long sampleAccumulatedBytes = this.accumulatedSampleSizes[trackIndex][sampleIndex];
                long skipAmount = sampleOffset - inputPosition;
                boolean requiresReload = skipAmount < 0 || skipAmount >= 262144;
                if ((!requiresReload && preferredRequiresReload) || (requiresReload == preferredRequiresReload && skipAmount < preferredSkipAmount)) {
                    preferredRequiresReload = requiresReload;
                    preferredSkipAmount = skipAmount;
                    preferredTrackIndex = trackIndex;
                    preferredAccumulatedBytes = sampleAccumulatedBytes;
                }
                if (sampleAccumulatedBytes < minAccumulatedBytes) {
                    minAccumulatedBytes = sampleAccumulatedBytes;
                    minAccumulatedBytesRequiresReload = requiresReload;
                    minAccumulatedBytesTrackIndex = trackIndex;
                }
            }
            trackIndex++;
        }
        return (minAccumulatedBytes == Long.MAX_VALUE || !minAccumulatedBytesRequiresReload || preferredAccumulatedBytes < MAXIMUM_READ_AHEAD_BYTES_STREAM + minAccumulatedBytes) ? preferredTrackIndex : minAccumulatedBytesTrackIndex;
    }

    private void updateSampleIndices(long timeUs) {
        Mp4Track[] mp4TrackArr;
        for (Mp4Track track : this.tracks) {
            TrackSampleTable sampleTable = track.sampleTable;
            int sampleIndex = sampleTable.getIndexOfEarlierOrEqualSynchronizationSample(timeUs);
            if (sampleIndex == -1) {
                sampleIndex = sampleTable.getIndexOfLaterOrEqualSynchronizationSample(timeUs);
            }
            track.sampleIndex = sampleIndex;
        }
    }

    private static long[][] calculateAccumulatedSampleSizes(Mp4Track[] tracks2) {
        long[][] accumulatedSampleSizes2 = new long[tracks2.length][];
        int[] nextSampleIndex = new int[tracks2.length];
        long[] nextSampleTimesUs = new long[tracks2.length];
        boolean[] tracksFinished = new boolean[tracks2.length];
        for (int i = 0; i < tracks2.length; i++) {
            accumulatedSampleSizes2[i] = new long[tracks2[i].sampleTable.sampleCount];
            nextSampleTimesUs[i] = tracks2[i].sampleTable.timestampsUs[0];
        }
        long accumulatedSampleSize = 0;
        int finishedTracks = 0;
        while (finishedTracks < tracks2.length) {
            long minTimeUs = Long.MAX_VALUE;
            int minTimeTrackIndex = -1;
            for (int i2 = 0; i2 < tracks2.length; i2++) {
                if (!tracksFinished[i2] && nextSampleTimesUs[i2] <= minTimeUs) {
                    minTimeTrackIndex = i2;
                    minTimeUs = nextSampleTimesUs[i2];
                }
            }
            int trackSampleIndex = nextSampleIndex[minTimeTrackIndex];
            accumulatedSampleSizes2[minTimeTrackIndex][trackSampleIndex] = accumulatedSampleSize;
            accumulatedSampleSize += (long) tracks2[minTimeTrackIndex].sampleTable.sizes[trackSampleIndex];
            int trackSampleIndex2 = trackSampleIndex + 1;
            nextSampleIndex[minTimeTrackIndex] = trackSampleIndex2;
            if (trackSampleIndex2 < accumulatedSampleSizes2[minTimeTrackIndex].length) {
                nextSampleTimesUs[minTimeTrackIndex] = tracks2[minTimeTrackIndex].sampleTable.timestampsUs[trackSampleIndex2];
            } else {
                tracksFinished[minTimeTrackIndex] = true;
                finishedTracks++;
            }
        }
        return accumulatedSampleSizes2;
    }

    private static long maybeAdjustSeekOffset(TrackSampleTable sampleTable, long seekTimeUs, long offset) {
        int sampleIndex = getSynchronizationSampleIndex(sampleTable, seekTimeUs);
        if (sampleIndex == -1) {
            return offset;
        }
        return Math.min(sampleTable.offsets[sampleIndex], offset);
    }

    private static int getSynchronizationSampleIndex(TrackSampleTable sampleTable, long timeUs) {
        int sampleIndex = sampleTable.getIndexOfEarlierOrEqualSynchronizationSample(timeUs);
        if (sampleIndex == -1) {
            return sampleTable.getIndexOfLaterOrEqualSynchronizationSample(timeUs);
        }
        return sampleIndex;
    }

    private static boolean processFtypAtom(ParsableByteArray atomData2) {
        atomData2.setPosition(8);
        if (atomData2.readInt() == BRAND_QUICKTIME) {
            return true;
        }
        atomData2.skipBytes(4);
        while (atomData2.bytesLeft() > 0) {
            if (atomData2.readInt() == BRAND_QUICKTIME) {
                return true;
            }
        }
        return false;
    }

    private static boolean shouldParseLeafAtom(int atom) {
        return atom == Atom.TYPE_mdhd || atom == Atom.TYPE_mvhd || atom == Atom.TYPE_hdlr || atom == Atom.TYPE_stsd || atom == Atom.TYPE_stts || atom == Atom.TYPE_stss || atom == Atom.TYPE_ctts || atom == Atom.TYPE_elst || atom == Atom.TYPE_stsc || atom == Atom.TYPE_stsz || atom == Atom.TYPE_stz2 || atom == Atom.TYPE_stco || atom == Atom.TYPE_co64 || atom == Atom.TYPE_tkhd || atom == Atom.TYPE_ftyp || atom == Atom.TYPE_udta;
    }

    private static boolean shouldParseContainerAtom(int atom) {
        return atom == Atom.TYPE_moov || atom == Atom.TYPE_trak || atom == Atom.TYPE_mdia || atom == Atom.TYPE_minf || atom == Atom.TYPE_stbl || atom == Atom.TYPE_edts;
    }
}
