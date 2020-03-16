package com.google.android.exoplayer2.metadata.id3;

import android.support.annotation.Nullable;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataDecoder;
import com.google.android.exoplayer2.metadata.MetadataInputBuffer;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.ParsableBitArray;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.CharEncoding;

public final class Id3Decoder implements MetadataDecoder {
    private static final int FRAME_FLAG_V3_HAS_GROUP_IDENTIFIER = 32;
    private static final int FRAME_FLAG_V3_IS_COMPRESSED = 128;
    private static final int FRAME_FLAG_V3_IS_ENCRYPTED = 64;
    private static final int FRAME_FLAG_V4_HAS_DATA_LENGTH = 1;
    private static final int FRAME_FLAG_V4_HAS_GROUP_IDENTIFIER = 64;
    private static final int FRAME_FLAG_V4_IS_COMPRESSED = 8;
    private static final int FRAME_FLAG_V4_IS_ENCRYPTED = 4;
    private static final int FRAME_FLAG_V4_IS_UNSYNCHRONIZED = 2;
    public static final int ID3_HEADER_LENGTH = 10;
    public static final int ID3_TAG = Util.getIntegerCodeForString("ID3");
    private static final int ID3_TEXT_ENCODING_ISO_8859_1 = 0;
    private static final int ID3_TEXT_ENCODING_UTF_16 = 1;
    private static final int ID3_TEXT_ENCODING_UTF_16BE = 2;
    private static final int ID3_TEXT_ENCODING_UTF_8 = 3;
    public static final FramePredicate NO_FRAMES_PREDICATE = $$Lambda$Id3Decoder$7M0gBIGKaTbyTVXWCb62bIHyc.INSTANCE;
    private static final String TAG = "Id3Decoder";
    @Nullable
    private final FramePredicate framePredicate;

    public interface FramePredicate {
        boolean evaluate(int i, int i2, int i3, int i4, int i5);
    }

    private static final class Id3Header {
        /* access modifiers changed from: private */
        public final int framesSize;
        /* access modifiers changed from: private */
        public final boolean isUnsynchronized;
        /* access modifiers changed from: private */
        public final int majorVersion;

        public Id3Header(int majorVersion2, boolean isUnsynchronized2, int framesSize2) {
            this.majorVersion = majorVersion2;
            this.isUnsynchronized = isUnsynchronized2;
            this.framesSize = framesSize2;
        }
    }

    static /* synthetic */ boolean lambda$static$0(int majorVersion, int id0, int id1, int id2, int id3) {
        return false;
    }

    public Id3Decoder() {
        this(null);
    }

    public Id3Decoder(@Nullable FramePredicate framePredicate2) {
        this.framePredicate = framePredicate2;
    }

    @Nullable
    public Metadata decode(MetadataInputBuffer inputBuffer) {
        ByteBuffer buffer = inputBuffer.data;
        return decode(buffer.array(), buffer.limit());
    }

    @Nullable
    public Metadata decode(byte[] data, int size) {
        List<Id3Frame> id3Frames = new ArrayList<>();
        ParsableByteArray id3Data = new ParsableByteArray(data, size);
        Id3Header id3Header = decodeHeader(id3Data);
        if (id3Header == null) {
            return null;
        }
        int startPosition = id3Data.getPosition();
        int frameHeaderSize = id3Header.majorVersion == 2 ? 6 : 10;
        int framesSize = id3Header.framesSize;
        if (id3Header.isUnsynchronized) {
            framesSize = removeUnsynchronization(id3Data, id3Header.framesSize);
        }
        id3Data.setLimit(startPosition + framesSize);
        boolean unsignedIntFrameSizeHack = false;
        if (!validateFrames(id3Data, id3Header.majorVersion, frameHeaderSize, false)) {
            if (id3Header.majorVersion != 4 || !validateFrames(id3Data, 4, frameHeaderSize, true)) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Failed to validate ID3 tag with majorVersion=");
                sb.append(id3Header.majorVersion);
                Log.w(str, sb.toString());
                return null;
            }
            unsignedIntFrameSizeHack = true;
        }
        while (id3Data.bytesLeft() >= frameHeaderSize) {
            Id3Frame frame = decodeFrame(id3Header.majorVersion, id3Data, unsignedIntFrameSizeHack, frameHeaderSize, this.framePredicate);
            if (frame != null) {
                id3Frames.add(frame);
            }
        }
        return new Metadata(id3Frames);
    }

    @Nullable
    private static Id3Header decodeHeader(ParsableByteArray data) {
        if (data.bytesLeft() < 10) {
            Log.w(TAG, "Data too short to be an ID3 tag");
            return null;
        }
        int id = data.readUnsignedInt24();
        if (id != ID3_TAG) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Unexpected first three bytes of ID3 tag header: ");
            sb.append(id);
            Log.w(str, sb.toString());
            return null;
        }
        int majorVersion = data.readUnsignedByte();
        boolean z = true;
        data.skipBytes(1);
        int flags = data.readUnsignedByte();
        int framesSize = data.readSynchSafeInt();
        if (majorVersion == 2) {
            if ((flags & 64) != 0) {
                Log.w(TAG, "Skipped ID3 tag with majorVersion=2 and undefined compression scheme");
                return null;
            }
        } else if (majorVersion == 3) {
            if ((flags & 64) != 0) {
                int extendedHeaderSize = data.readInt();
                data.skipBytes(extendedHeaderSize);
                framesSize -= extendedHeaderSize + 4;
            }
        } else if (majorVersion == 4) {
            if ((flags & 64) != 0) {
                int extendedHeaderSize2 = data.readSynchSafeInt();
                data.skipBytes(extendedHeaderSize2 - 4);
                framesSize -= extendedHeaderSize2;
            }
            if ((flags & 16) != 0) {
                framesSize -= 10;
            }
        } else {
            String str2 = TAG;
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Skipped ID3 tag with unsupported majorVersion=");
            sb2.append(majorVersion);
            Log.w(str2, sb2.toString());
            return null;
        }
        if (majorVersion >= 4 || (flags & 128) == 0) {
            z = false;
        }
        return new Id3Header(majorVersion, z, framesSize);
    }

    private static boolean validateFrames(ParsableByteArray id3Data, int majorVersion, int frameHeaderSize, boolean unsignedIntFrameSizeHack) {
        int flags;
        long frameSize;
        int id;
        ParsableByteArray parsableByteArray = id3Data;
        int i = majorVersion;
        int startPosition = id3Data.getPosition();
        while (true) {
            try {
                boolean z = true;
                if (id3Data.bytesLeft() >= frameHeaderSize) {
                    if (i >= 3) {
                        try {
                            id = id3Data.readInt();
                            frameSize = id3Data.readUnsignedInt();
                            flags = id3Data.readUnsignedShort();
                        } catch (Throwable th) {
                            th = th;
                        }
                    } else {
                        id = id3Data.readUnsignedInt24();
                        frameSize = (long) id3Data.readUnsignedInt24();
                        flags = 0;
                    }
                    if (id == 0 && frameSize == 0 && flags == 0) {
                        parsableByteArray.setPosition(startPosition);
                        return true;
                    }
                    if (i == 4 && !unsignedIntFrameSizeHack) {
                        if ((8421504 & frameSize) != 0) {
                            parsableByteArray.setPosition(startPosition);
                            return false;
                        }
                        frameSize = (frameSize & 255) | (((frameSize >> 8) & 255) << 7) | (((frameSize >> 16) & 255) << 14) | (((frameSize >> 24) & 255) << 21);
                    }
                    boolean hasGroupIdentifier = false;
                    boolean hasDataLength = false;
                    if (i == 4) {
                        hasGroupIdentifier = (flags & 64) != 0;
                        if ((flags & 1) == 0) {
                            z = false;
                        }
                        hasDataLength = z;
                    } else if (i == 3) {
                        hasGroupIdentifier = (flags & 32) != 0;
                        if ((flags & 128) == 0) {
                            z = false;
                        }
                        hasDataLength = z;
                    }
                    int minimumFrameSize = 0;
                    if (hasGroupIdentifier) {
                        minimumFrameSize = 0 + 1;
                    }
                    if (hasDataLength) {
                        minimumFrameSize += 4;
                    }
                    if (frameSize < ((long) minimumFrameSize)) {
                        parsableByteArray.setPosition(startPosition);
                        return false;
                    } else if (((long) id3Data.bytesLeft()) < frameSize) {
                        parsableByteArray.setPosition(startPosition);
                        return false;
                    } else {
                        parsableByteArray.skipBytes((int) frameSize);
                    }
                } else {
                    parsableByteArray.setPosition(startPosition);
                    return true;
                }
            } catch (Throwable th2) {
                th = th2;
                int i2 = frameHeaderSize;
                parsableByteArray.setPosition(startPosition);
                throw th;
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:133:0x01b6, code lost:
        if (r13 == 67) goto L_0x01b8;
     */
    @Nullable
    private static Id3Frame decodeFrame(int majorVersion, ParsableByteArray id3Data, boolean unsignedIntFrameSizeHack, int frameHeaderSize, @Nullable FramePredicate framePredicate2) {
        int frameSize;
        int flags;
        int nextFramePosition;
        boolean hasDataLength;
        boolean isUnsynchronized;
        boolean hasGroupIdentifier;
        boolean isEncrypted;
        boolean isCompressed;
        Id3Frame frame;
        int i = majorVersion;
        ParsableByteArray parsableByteArray = id3Data;
        int frameId0 = id3Data.readUnsignedByte();
        int frameId1 = id3Data.readUnsignedByte();
        int frameId2 = id3Data.readUnsignedByte();
        int frameId3 = i >= 3 ? id3Data.readUnsignedByte() : 0;
        if (i == 4) {
            int frameSize2 = id3Data.readUnsignedIntToInt();
            if (!unsignedIntFrameSizeHack) {
                frameSize = (frameSize2 & 255) | (((frameSize2 >> 8) & 255) << 7) | (((frameSize2 >> 16) & 255) << 14) | (((frameSize2 >> 24) & 255) << 21);
            } else {
                frameSize = frameSize2;
            }
        } else if (i == 3) {
            frameSize = id3Data.readUnsignedIntToInt();
        } else {
            frameSize = id3Data.readUnsignedInt24();
        }
        int flags2 = i >= 3 ? id3Data.readUnsignedShort() : 0;
        if (frameId0 == 0 && frameId1 == 0 && frameId2 == 0 && frameId3 == 0 && frameSize == 0 && flags2 == 0) {
            parsableByteArray.setPosition(id3Data.limit());
            return null;
        }
        int nextFramePosition2 = id3Data.getPosition() + frameSize;
        if (nextFramePosition2 > id3Data.limit()) {
            Log.w(TAG, "Frame size exceeds remaining tag data");
            parsableByteArray.setPosition(id3Data.limit());
            return null;
        }
        if (framePredicate2 != null) {
            nextFramePosition = nextFramePosition2;
            flags = flags2;
            if (!framePredicate2.evaluate(majorVersion, frameId0, frameId1, frameId2, frameId3)) {
                parsableByteArray.setPosition(nextFramePosition);
                return null;
            }
        } else {
            nextFramePosition = nextFramePosition2;
            flags = flags2;
        }
        if (i == 3) {
            boolean isCompressed2 = (flags & 128) != 0;
            boolean hasGroupIdentifier2 = (flags & 32) != 0;
            isCompressed = isCompressed2;
            isEncrypted = (flags & 64) != 0;
            isUnsynchronized = false;
            hasDataLength = isCompressed2;
            hasGroupIdentifier = hasGroupIdentifier2;
        } else if (i == 4) {
            boolean hasGroupIdentifier3 = (flags & 64) != 0;
            boolean isCompressed3 = (flags & 8) != 0;
            boolean isEncrypted2 = (flags & 4) != 0;
            boolean hasDataLength2 = (flags & 1) != 0;
            isCompressed = isCompressed3;
            isEncrypted = isEncrypted2;
            isUnsynchronized = (flags & 2) != 0;
            hasDataLength = hasDataLength2;
            hasGroupIdentifier = hasGroupIdentifier3;
        } else {
            isCompressed = false;
            isEncrypted = false;
            isUnsynchronized = false;
            hasDataLength = false;
            hasGroupIdentifier = false;
        }
        if (isCompressed || isEncrypted) {
            Log.w(TAG, "Skipping unsupported compressed or encrypted frame");
            parsableByteArray.setPosition(nextFramePosition);
            return null;
        }
        if (hasGroupIdentifier) {
            frameSize--;
            parsableByteArray.skipBytes(1);
        }
        if (hasDataLength) {
            frameSize -= 4;
            parsableByteArray.skipBytes(4);
        }
        if (isUnsynchronized) {
            frameSize = removeUnsynchronization(parsableByteArray, frameSize);
        }
        if (frameId0 == 84 && frameId1 == 88 && frameId2 == 88 && (i == 2 || frameId3 == 88)) {
            try {
                frame = decodeTxxxFrame(parsableByteArray, frameSize);
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, "Unsupported character encoding");
                parsableByteArray.setPosition(nextFramePosition);
                return null;
            } catch (Throwable th) {
                parsableByteArray.setPosition(nextFramePosition);
                throw th;
            }
        } else if (frameId0 == 84) {
            frame = decodeTextInformationFrame(parsableByteArray, frameSize, getFrameId(i, frameId0, frameId1, frameId2, frameId3));
        } else if (frameId0 == 87 && frameId1 == 88 && frameId2 == 88 && (i == 2 || frameId3 == 88)) {
            frame = decodeWxxxFrame(parsableByteArray, frameSize);
        } else if (frameId0 == 87) {
            frame = decodeUrlLinkFrame(parsableByteArray, frameSize, getFrameId(i, frameId0, frameId1, frameId2, frameId3));
        } else if (frameId0 == 80 && frameId1 == 82 && frameId2 == 73 && frameId3 == 86) {
            frame = decodePrivFrame(parsableByteArray, frameSize);
        } else if (frameId0 == 71 && frameId1 == 69 && frameId2 == 79 && (frameId3 == 66 || i == 2)) {
            frame = decodeGeobFrame(parsableByteArray, frameSize);
        } else {
            if (i == 2) {
                if (frameId0 == 80 && frameId1 == 73 && frameId2 == 67) {
                }
                if (frameId0 != 67 && frameId1 == 79 && frameId2 == 77 && (frameId3 == 77 || i == 2)) {
                    frame = decodeCommentFrame(parsableByteArray, frameSize);
                } else if (frameId0 != 67 && frameId1 == 72 && frameId2 == 65 && frameId3 == 80) {
                    frame = decodeChapterFrame(id3Data, frameSize, majorVersion, unsignedIntFrameSizeHack, frameHeaderSize, framePredicate2);
                } else if (frameId0 != 67 && frameId1 == 84 && frameId2 == 79 && frameId3 == 67) {
                    frame = decodeChapterTOCFrame(id3Data, frameSize, majorVersion, unsignedIntFrameSizeHack, frameHeaderSize, framePredicate2);
                } else if (frameId0 != 77 && frameId1 == 76 && frameId2 == 76 && frameId3 == 84) {
                    frame = decodeMlltFrame(parsableByteArray, frameSize);
                } else {
                    frame = decodeBinaryFrame(parsableByteArray, frameSize, getFrameId(i, frameId0, frameId1, frameId2, frameId3));
                }
            } else {
                if (frameId0 == 65) {
                    if (frameId1 == 80) {
                        if (frameId2 == 73) {
                        }
                    }
                }
                if (frameId0 != 67) {
                }
                if (frameId0 != 67) {
                }
                if (frameId0 != 67) {
                }
                if (frameId0 != 77) {
                }
                frame = decodeBinaryFrame(parsableByteArray, frameSize, getFrameId(i, frameId0, frameId1, frameId2, frameId3));
            }
            frame = decodeApicFrame(parsableByteArray, frameSize, i);
        }
        if (frame == null) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Failed to decode frame: id=");
            sb.append(getFrameId(i, frameId0, frameId1, frameId2, frameId3));
            sb.append(", frameSize=");
            sb.append(frameSize);
            Log.w(str, sb.toString());
        }
        parsableByteArray.setPosition(nextFramePosition);
        return frame;
    }

    @Nullable
    private static TextInformationFrame decodeTxxxFrame(ParsableByteArray id3Data, int frameSize) throws UnsupportedEncodingException {
        if (frameSize < 1) {
            return null;
        }
        int encoding = id3Data.readUnsignedByte();
        String charset = getCharsetName(encoding);
        byte[] data = new byte[(frameSize - 1)];
        id3Data.readBytes(data, 0, frameSize - 1);
        int descriptionEndIndex = indexOfEos(data, 0, encoding);
        String description = new String(data, 0, descriptionEndIndex, charset);
        int valueStartIndex = delimiterLength(encoding) + descriptionEndIndex;
        return new TextInformationFrame("TXXX", description, decodeStringIfValid(data, valueStartIndex, indexOfEos(data, valueStartIndex, encoding), charset));
    }

    @Nullable
    private static TextInformationFrame decodeTextInformationFrame(ParsableByteArray id3Data, int frameSize, String id) throws UnsupportedEncodingException {
        if (frameSize < 1) {
            return null;
        }
        int encoding = id3Data.readUnsignedByte();
        String charset = getCharsetName(encoding);
        byte[] data = new byte[(frameSize - 1)];
        id3Data.readBytes(data, 0, frameSize - 1);
        return new TextInformationFrame(id, null, new String(data, 0, indexOfEos(data, 0, encoding), charset));
    }

    @Nullable
    private static UrlLinkFrame decodeWxxxFrame(ParsableByteArray id3Data, int frameSize) throws UnsupportedEncodingException {
        if (frameSize < 1) {
            return null;
        }
        int encoding = id3Data.readUnsignedByte();
        String charset = getCharsetName(encoding);
        byte[] data = new byte[(frameSize - 1)];
        id3Data.readBytes(data, 0, frameSize - 1);
        int descriptionEndIndex = indexOfEos(data, 0, encoding);
        String description = new String(data, 0, descriptionEndIndex, charset);
        int urlStartIndex = delimiterLength(encoding) + descriptionEndIndex;
        return new UrlLinkFrame("WXXX", description, decodeStringIfValid(data, urlStartIndex, indexOfZeroByte(data, urlStartIndex), CharEncoding.ISO_8859_1));
    }

    private static UrlLinkFrame decodeUrlLinkFrame(ParsableByteArray id3Data, int frameSize, String id) throws UnsupportedEncodingException {
        byte[] data = new byte[frameSize];
        id3Data.readBytes(data, 0, frameSize);
        return new UrlLinkFrame(id, null, new String(data, 0, indexOfZeroByte(data, 0), CharEncoding.ISO_8859_1));
    }

    private static PrivFrame decodePrivFrame(ParsableByteArray id3Data, int frameSize) throws UnsupportedEncodingException {
        byte[] data = new byte[frameSize];
        id3Data.readBytes(data, 0, frameSize);
        int ownerEndIndex = indexOfZeroByte(data, 0);
        return new PrivFrame(new String(data, 0, ownerEndIndex, CharEncoding.ISO_8859_1), copyOfRangeIfValid(data, ownerEndIndex + 1, data.length));
    }

    private static GeobFrame decodeGeobFrame(ParsableByteArray id3Data, int frameSize) throws UnsupportedEncodingException {
        int encoding = id3Data.readUnsignedByte();
        String charset = getCharsetName(encoding);
        byte[] data = new byte[(frameSize - 1)];
        id3Data.readBytes(data, 0, frameSize - 1);
        int mimeTypeEndIndex = indexOfZeroByte(data, 0);
        String mimeType = new String(data, 0, mimeTypeEndIndex, CharEncoding.ISO_8859_1);
        int filenameStartIndex = mimeTypeEndIndex + 1;
        int filenameEndIndex = indexOfEos(data, filenameStartIndex, encoding);
        String filename = decodeStringIfValid(data, filenameStartIndex, filenameEndIndex, charset);
        int descriptionStartIndex = delimiterLength(encoding) + filenameEndIndex;
        int descriptionEndIndex = indexOfEos(data, descriptionStartIndex, encoding);
        return new GeobFrame(mimeType, filename, decodeStringIfValid(data, descriptionStartIndex, descriptionEndIndex, charset), copyOfRangeIfValid(data, delimiterLength(encoding) + descriptionEndIndex, data.length));
    }

    private static ApicFrame decodeApicFrame(ParsableByteArray id3Data, int frameSize, int majorVersion) throws UnsupportedEncodingException {
        String mimeType;
        int mimeTypeEndIndex;
        int encoding = id3Data.readUnsignedByte();
        String charset = getCharsetName(encoding);
        byte[] data = new byte[(frameSize - 1)];
        id3Data.readBytes(data, 0, frameSize - 1);
        if (majorVersion == 2) {
            mimeTypeEndIndex = 2;
            StringBuilder sb = new StringBuilder();
            sb.append("image/");
            sb.append(Util.toLowerInvariant(new String(data, 0, 3, CharEncoding.ISO_8859_1)));
            mimeType = sb.toString();
            if ("image/jpg".equals(mimeType)) {
                mimeType = "image/jpeg";
            }
        } else {
            mimeTypeEndIndex = indexOfZeroByte(data, 0);
            mimeType = Util.toLowerInvariant(new String(data, 0, mimeTypeEndIndex, CharEncoding.ISO_8859_1));
            if (mimeType.indexOf(47) == -1) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("image/");
                sb2.append(mimeType);
                mimeType = sb2.toString();
            }
        }
        int pictureType = data[mimeTypeEndIndex + 1] & 255;
        int descriptionStartIndex = mimeTypeEndIndex + 2;
        int descriptionEndIndex = indexOfEos(data, descriptionStartIndex, encoding);
        return new ApicFrame(mimeType, new String(data, descriptionStartIndex, descriptionEndIndex - descriptionStartIndex, charset), pictureType, copyOfRangeIfValid(data, delimiterLength(encoding) + descriptionEndIndex, data.length));
    }

    @Nullable
    private static CommentFrame decodeCommentFrame(ParsableByteArray id3Data, int frameSize) throws UnsupportedEncodingException {
        if (frameSize < 4) {
            return null;
        }
        int encoding = id3Data.readUnsignedByte();
        String charset = getCharsetName(encoding);
        byte[] data = new byte[3];
        id3Data.readBytes(data, 0, 3);
        String language = new String(data, 0, 3);
        byte[] data2 = new byte[(frameSize - 4)];
        id3Data.readBytes(data2, 0, frameSize - 4);
        int descriptionEndIndex = indexOfEos(data2, 0, encoding);
        String description = new String(data2, 0, descriptionEndIndex, charset);
        int textStartIndex = delimiterLength(encoding) + descriptionEndIndex;
        return new CommentFrame(language, description, decodeStringIfValid(data2, textStartIndex, indexOfEos(data2, textStartIndex, encoding), charset));
    }

    private static ChapterFrame decodeChapterFrame(ParsableByteArray id3Data, int frameSize, int majorVersion, boolean unsignedIntFrameSizeHack, int frameHeaderSize, @Nullable FramePredicate framePredicate2) throws UnsupportedEncodingException {
        long startOffset;
        long endOffset;
        ParsableByteArray parsableByteArray = id3Data;
        int framePosition = id3Data.getPosition();
        int chapterIdEndIndex = indexOfZeroByte(parsableByteArray.data, framePosition);
        String chapterId = new String(parsableByteArray.data, framePosition, chapterIdEndIndex - framePosition, CharEncoding.ISO_8859_1);
        parsableByteArray.setPosition(chapterIdEndIndex + 1);
        int startTime = id3Data.readInt();
        int endTime = id3Data.readInt();
        long startOffset2 = id3Data.readUnsignedInt();
        if (startOffset2 == 4294967295L) {
            startOffset = -1;
        } else {
            startOffset = startOffset2;
        }
        long endOffset2 = id3Data.readUnsignedInt();
        if (endOffset2 == 4294967295L) {
            endOffset = -1;
        } else {
            endOffset = endOffset2;
        }
        ArrayList arrayList = new ArrayList();
        int limit = framePosition + frameSize;
        while (id3Data.getPosition() < limit) {
            Id3Frame frame = decodeFrame(majorVersion, parsableByteArray, unsignedIntFrameSizeHack, frameHeaderSize, framePredicate2);
            if (frame != null) {
                arrayList.add(frame);
            }
        }
        int i = majorVersion;
        boolean z = unsignedIntFrameSizeHack;
        int i2 = frameHeaderSize;
        FramePredicate framePredicate3 = framePredicate2;
        Id3Frame[] subFrameArray = new Id3Frame[arrayList.size()];
        arrayList.toArray(subFrameArray);
        int i3 = limit;
        ArrayList arrayList2 = arrayList;
        ChapterFrame chapterFrame = new ChapterFrame(chapterId, startTime, endTime, startOffset, endOffset, subFrameArray);
        return chapterFrame;
    }

    private static ChapterTocFrame decodeChapterTOCFrame(ParsableByteArray id3Data, int frameSize, int majorVersion, boolean unsignedIntFrameSizeHack, int frameHeaderSize, @Nullable FramePredicate framePredicate2) throws UnsupportedEncodingException {
        ParsableByteArray parsableByteArray = id3Data;
        int framePosition = id3Data.getPosition();
        int elementIdEndIndex = indexOfZeroByte(parsableByteArray.data, framePosition);
        String elementId = new String(parsableByteArray.data, framePosition, elementIdEndIndex - framePosition, CharEncoding.ISO_8859_1);
        parsableByteArray.setPosition(elementIdEndIndex + 1);
        int ctocFlags = id3Data.readUnsignedByte();
        boolean isOrdered = false;
        boolean isRoot = (ctocFlags & 2) != 0;
        if ((ctocFlags & 1) != 0) {
            isOrdered = true;
        }
        int childCount = id3Data.readUnsignedByte();
        String[] children = new String[childCount];
        for (int i = 0; i < childCount; i++) {
            int startIndex = id3Data.getPosition();
            int endIndex = indexOfZeroByte(parsableByteArray.data, startIndex);
            children[i] = new String(parsableByteArray.data, startIndex, endIndex - startIndex, CharEncoding.ISO_8859_1);
            parsableByteArray.setPosition(endIndex + 1);
        }
        ArrayList arrayList = new ArrayList();
        int limit = framePosition + frameSize;
        while (id3Data.getPosition() < limit) {
            Id3Frame frame = decodeFrame(majorVersion, parsableByteArray, unsignedIntFrameSizeHack, frameHeaderSize, framePredicate2);
            if (frame != null) {
                arrayList.add(frame);
            }
        }
        int i2 = majorVersion;
        boolean z = unsignedIntFrameSizeHack;
        int i3 = frameHeaderSize;
        FramePredicate framePredicate3 = framePredicate2;
        Id3Frame[] subFrameArray = new Id3Frame[arrayList.size()];
        arrayList.toArray(subFrameArray);
        ChapterTocFrame chapterTocFrame = new ChapterTocFrame(elementId, isRoot, isOrdered, children, subFrameArray);
        return chapterTocFrame;
    }

    private static MlltFrame decodeMlltFrame(ParsableByteArray id3Data, int frameSize) {
        int mpegFramesBetweenReference = id3Data.readUnsignedShort();
        int bytesBetweenReference = id3Data.readUnsignedInt24();
        int millisecondsBetweenReference = id3Data.readUnsignedInt24();
        int bitsForBytesDeviation = id3Data.readUnsignedByte();
        int bitsForMillisecondsDeviation = id3Data.readUnsignedByte();
        ParsableBitArray references = new ParsableBitArray();
        references.reset(id3Data);
        int referencesCount = ((frameSize - 10) * 8) / (bitsForBytesDeviation + bitsForMillisecondsDeviation);
        int[] bytesDeviations = new int[referencesCount];
        int[] millisecondsDeviations = new int[referencesCount];
        for (int i = 0; i < referencesCount; i++) {
            int bytesDeviation = references.readBits(bitsForBytesDeviation);
            int millisecondsDeviation = references.readBits(bitsForMillisecondsDeviation);
            bytesDeviations[i] = bytesDeviation;
            millisecondsDeviations[i] = millisecondsDeviation;
        }
        int[] iArr = bytesDeviations;
        MlltFrame mlltFrame = new MlltFrame(mpegFramesBetweenReference, bytesBetweenReference, millisecondsBetweenReference, bytesDeviations, millisecondsDeviations);
        return mlltFrame;
    }

    private static BinaryFrame decodeBinaryFrame(ParsableByteArray id3Data, int frameSize, String id) {
        byte[] frame = new byte[frameSize];
        id3Data.readBytes(frame, 0, frameSize);
        return new BinaryFrame(id, frame);
    }

    private static int removeUnsynchronization(ParsableByteArray data, int length) {
        byte[] bytes = data.data;
        for (int i = data.getPosition(); i + 1 < length; i++) {
            if ((bytes[i] & 255) == 255 && bytes[i + 1] == 0) {
                System.arraycopy(bytes, i + 2, bytes, i + 1, (length - i) - 2);
                length--;
            }
        }
        return length;
    }

    private static String getCharsetName(int encodingByte) {
        switch (encodingByte) {
            case 1:
                return "UTF-16";
            case 2:
                return CharEncoding.UTF_16BE;
            case 3:
                return "UTF-8";
            default:
                return CharEncoding.ISO_8859_1;
        }
    }

    private static String getFrameId(int majorVersion, int frameId0, int frameId1, int frameId2, int frameId3) {
        if (majorVersion == 2) {
            return String.format(Locale.US, "%c%c%c", new Object[]{Integer.valueOf(frameId0), Integer.valueOf(frameId1), Integer.valueOf(frameId2)});
        }
        return String.format(Locale.US, "%c%c%c%c", new Object[]{Integer.valueOf(frameId0), Integer.valueOf(frameId1), Integer.valueOf(frameId2), Integer.valueOf(frameId3)});
    }

    private static int indexOfEos(byte[] data, int fromIndex, int encoding) {
        int terminationPos = indexOfZeroByte(data, fromIndex);
        if (encoding == 0 || encoding == 3) {
            return terminationPos;
        }
        while (terminationPos < data.length - 1) {
            if (terminationPos % 2 == 0 && data[terminationPos + 1] == 0) {
                return terminationPos;
            }
            terminationPos = indexOfZeroByte(data, terminationPos + 1);
        }
        return data.length;
    }

    private static int indexOfZeroByte(byte[] data, int fromIndex) {
        for (int i = fromIndex; i < data.length; i++) {
            if (data[i] == 0) {
                return i;
            }
        }
        return data.length;
    }

    private static int delimiterLength(int encodingByte) {
        return (encodingByte == 0 || encodingByte == 3) ? 1 : 2;
    }

    private static byte[] copyOfRangeIfValid(byte[] data, int from, int to) {
        if (to <= from) {
            return Util.EMPTY_BYTE_ARRAY;
        }
        return Arrays.copyOfRange(data, from, to);
    }

    private static String decodeStringIfValid(byte[] data, int from, int to, String charsetName) throws UnsupportedEncodingException {
        if (to <= from || to > data.length) {
            return "";
        }
        return new String(data, from, to - from, charsetName);
    }
}
