package com.google.android.exoplayer2.extractor.mp4;

import android.support.v4.media.session.PlaybackStateCompat;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;

final class Sniffer {
    private static final int[] COMPATIBLE_BRANDS = {Util.getIntegerCodeForString("isom"), Util.getIntegerCodeForString("iso2"), Util.getIntegerCodeForString("iso3"), Util.getIntegerCodeForString("iso4"), Util.getIntegerCodeForString("iso5"), Util.getIntegerCodeForString("iso6"), Util.getIntegerCodeForString("avc1"), Util.getIntegerCodeForString("hvc1"), Util.getIntegerCodeForString("hev1"), Util.getIntegerCodeForString("mp41"), Util.getIntegerCodeForString("mp42"), Util.getIntegerCodeForString("3g2a"), Util.getIntegerCodeForString("3g2b"), Util.getIntegerCodeForString("3gr6"), Util.getIntegerCodeForString("3gs6"), Util.getIntegerCodeForString("3ge6"), Util.getIntegerCodeForString("3gg6"), Util.getIntegerCodeForString("M4V "), Util.getIntegerCodeForString("M4A "), Util.getIntegerCodeForString("f4v "), Util.getIntegerCodeForString("kddi"), Util.getIntegerCodeForString("M4VP"), Util.getIntegerCodeForString("qt  "), Util.getIntegerCodeForString("MSNV")};
    private static final int SEARCH_LENGTH = 4096;

    public static boolean sniffFragmented(ExtractorInput input) throws IOException, InterruptedException {
        return sniffInternal(input, true);
    }

    public static boolean sniffUnfragmented(ExtractorInput input) throws IOException, InterruptedException {
        return sniffInternal(input, false);
    }

    private static boolean sniffInternal(ExtractorInput input, boolean fragmented) throws IOException, InterruptedException {
        boolean z;
        ExtractorInput extractorInput = input;
        long inputLength = input.getLength();
        long j = PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM;
        long j2 = -1;
        if (inputLength != -1 && inputLength <= PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM) {
            j = inputLength;
        }
        int bytesToSearch = (int) j;
        ParsableByteArray buffer = new ParsableByteArray(64);
        int bytesSearched = 0;
        boolean foundGoodFileType = false;
        boolean isFragmented = false;
        while (true) {
            if (bytesSearched >= bytesToSearch) {
                break;
            }
            int headerSize = 8;
            buffer.reset(8);
            extractorInput.peekFully(buffer.data, 0, 8);
            long atomSize = buffer.readUnsignedInt();
            int atomType = buffer.readInt();
            if (atomSize == 1) {
                headerSize = 16;
                extractorInput.peekFully(buffer.data, 8, 8);
                buffer.setLimit(16);
                atomSize = buffer.readUnsignedLongToLong();
            } else if (atomSize == 0) {
                long endPosition = input.getLength();
                if (endPosition != j2) {
                    atomSize = (endPosition - input.getPosition()) + ((long) 8);
                }
            }
            if (atomSize >= ((long) headerSize)) {
                bytesSearched += headerSize;
                if (atomType != Atom.TYPE_moov) {
                    if (atomType == Atom.TYPE_moof) {
                        break;
                    } else if (atomType == Atom.TYPE_mvex) {
                        break;
                    } else if ((((long) bytesSearched) + atomSize) - ((long) headerSize) >= ((long) bytesToSearch)) {
                        break;
                    } else {
                        int atomDataSize = (int) (atomSize - ((long) headerSize));
                        bytesSearched += atomDataSize;
                        if (atomType == Atom.TYPE_ftyp) {
                            if (atomDataSize < 8) {
                                return false;
                            }
                            buffer.reset(atomDataSize);
                            extractorInput.peekFully(buffer.data, 0, atomDataSize);
                            int brandsCount = atomDataSize / 4;
                            int i = 0;
                            while (true) {
                                if (i >= brandsCount) {
                                    break;
                                }
                                if (i == 1) {
                                    buffer.skipBytes(4);
                                } else if (isCompatibleBrand(buffer.readInt())) {
                                    foundGoodFileType = true;
                                    break;
                                }
                                i++;
                            }
                            if (!foundGoodFileType) {
                                return false;
                            }
                        } else if (atomDataSize != 0) {
                            extractorInput.advancePeekPosition(atomDataSize);
                        }
                        j2 = -1;
                    }
                } else {
                    j2 = -1;
                }
            } else {
                return false;
            }
        }
        isFragmented = true;
        if (!foundGoodFileType) {
            boolean z2 = fragmented;
        } else if (fragmented == isFragmented) {
            z = true;
            return z;
        }
        z = false;
        return z;
    }

    private static boolean isCompatibleBrand(int brand) {
        if ((brand >>> 8) == Util.getIntegerCodeForString("3gp")) {
            return true;
        }
        for (int compatibleBrand : COMPATIBLE_BRANDS) {
            if (compatibleBrand == brand) {
                return true;
            }
        }
        return false;
    }

    private Sniffer() {
    }
}
