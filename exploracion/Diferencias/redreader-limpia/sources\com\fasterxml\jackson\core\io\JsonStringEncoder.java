package com.fasterxml.jackson.core.io;

import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.BufferRecyclers;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.core.util.TextBuffer;
import com.google.android.exoplayer2.extractor.ts.PsExtractor;

public final class JsonStringEncoder {
    private static final byte[] HB = CharTypes.copyHexBytes();
    private static final char[] HC = CharTypes.copyHexChars();
    private static final int SURR1_FIRST = 55296;
    private static final int SURR1_LAST = 56319;
    private static final int SURR2_FIRST = 56320;
    private static final int SURR2_LAST = 57343;
    protected ByteArrayBuilder _bytes;
    protected final char[] _qbuf = new char[6];
    protected TextBuffer _text;

    public JsonStringEncoder() {
        char[] cArr = this._qbuf;
        cArr[0] = '\\';
        cArr[2] = '0';
        cArr[3] = '0';
    }

    @Deprecated
    public static JsonStringEncoder getInstance() {
        return BufferRecyclers.getJsonStringEncoder();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0031, code lost:
        if (r8 >= 0) goto L_0x003a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0033, code lost:
        r9 = _appendNumeric(r4, r14._qbuf);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x003a, code lost:
        r9 = _appendNamed(r8, r14._qbuf);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0045, code lost:
        if ((r6 + r9) <= r1.length) goto L_0x005d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0047, code lost:
        r10 = r1.length - r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0049, code lost:
        if (r10 <= 0) goto L_0x0050;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x004b, code lost:
        java.lang.System.arraycopy(r14._qbuf, 0, r1, r6, r10);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0050, code lost:
        r1 = r0.finishCurrentSegment();
        r11 = r9 - r10;
        java.lang.System.arraycopy(r14._qbuf, r10, r1, 0, r11);
        r6 = r11;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x005d, code lost:
        java.lang.System.arraycopy(r14._qbuf, 0, r1, r6, r9);
        r6 = r6 + r9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0028, code lost:
        r7 = r4 + 1;
        r4 = r15.charAt(r4);
        r8 = r2[r4];
     */
    public char[] quoteAsString(String input) {
        int inPtr;
        TextBuffer textBuffer = this._text;
        if (textBuffer == null) {
            TextBuffer textBuffer2 = new TextBuffer(null);
            textBuffer = textBuffer2;
            this._text = textBuffer2;
        }
        char[] outputBuffer = textBuffer.emptyAndGetCurrentSegment();
        int[] escCodes = CharTypes.get7BitOutputEscapes();
        int escCodeCount = escCodes.length;
        int inPtr2 = 0;
        int inputLen = input.length();
        int outPtr = 0;
        loop0:
        while (true) {
            if (inPtr2 >= inputLen) {
                break;
            }
            while (true) {
                char c = input.charAt(inPtr2);
                if (c < escCodeCount && escCodes[c] != 0) {
                    break;
                }
                if (outPtr >= outputBuffer.length) {
                    outputBuffer = textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                int outPtr2 = outPtr + 1;
                outputBuffer[outPtr] = c;
                inPtr2++;
                if (inPtr2 >= inputLen) {
                    outPtr = outPtr2;
                    break loop0;
                }
                outPtr = outPtr2;
            }
            inPtr2 = inPtr;
        }
        textBuffer.setCurrentLength(outPtr);
        return textBuffer.contentsAsArray();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x002e, code lost:
        r11.append(r9._qbuf, 0, r6);
        r2 = r4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:6:0x0016, code lost:
        r4 = r2 + 1;
        r2 = r10.charAt(r2);
        r5 = r0[r2];
     */
    /* JADX WARNING: Code restructure failed: missing block: B:7:0x001f, code lost:
        if (r5 >= 0) goto L_0x0028;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x0021, code lost:
        r6 = _appendNumeric(r2, r9._qbuf);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0028, code lost:
        r6 = _appendNamed(r5, r9._qbuf);
     */
    public void quoteAsString(CharSequence input, StringBuilder output) {
        int[] escCodes = CharTypes.get7BitOutputEscapes();
        int escCodeCount = escCodes.length;
        int inPtr = 0;
        int inputLen = input.length();
        while (inPtr < inputLen) {
            while (true) {
                char c = input.charAt(inPtr);
                if (c < escCodeCount && escCodes[c] != 0) {
                    break;
                }
                output.append(c);
                inPtr++;
                if (inPtr >= inputLen) {
                    return;
                }
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0041, code lost:
        if (r3 < r4.length) goto L_0x0048;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0043, code lost:
        r4 = r0.finishCurrentSegment();
        r3 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0048, code lost:
        r6 = r1 + 1;
        r1 = r10.charAt(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x004e, code lost:
        if (r1 > 127) goto L_0x005c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0050, code lost:
        r3 = _appendByte(r1, r5[r1], r0, r3);
        r4 = r0.getCurrentSegment();
        r1 = r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x005e, code lost:
        if (r1 > 2047) goto L_0x0070;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0060, code lost:
        r7 = r3 + 1;
        r4[r3] = (byte) ((r1 >> 6) | com.google.android.exoplayer2.extractor.ts.PsExtractor.AUDIO_STREAM);
        r1 = (r1 & 63) | 128;
        r3 = r7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0073, code lost:
        if (r1 < 55296) goto L_0x00d0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0078, code lost:
        if (r1 <= 57343) goto L_0x007b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x007e, code lost:
        if (r1 <= 56319) goto L_0x0083;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0080, code lost:
        _illegal(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x0083, code lost:
        if (r6 < r2) goto L_0x0088;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x0085, code lost:
        _illegal(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x0088, code lost:
        r7 = r6 + 1;
        r1 = _convert(r1, r10.charAt(r6));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x0095, code lost:
        if (r1 <= 1114111) goto L_0x009a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x0097, code lost:
        _illegal(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x009a, code lost:
        r6 = r3 + 1;
        r4[r3] = (byte) ((r1 >> 18) | com.google.android.exoplayer2.extractor.ts.PsExtractor.VIDEO_STREAM_MASK);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x00a4, code lost:
        if (r6 < r4.length) goto L_0x00ab;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x00a6, code lost:
        r4 = r0.finishCurrentSegment();
        r6 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x00ab, code lost:
        r3 = r6 + 1;
        r4[r6] = (byte) (((r1 >> 12) & 63) | 128);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x00b7, code lost:
        if (r3 < r4.length) goto L_0x00be;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x00b9, code lost:
        r4 = r0.finishCurrentSegment();
        r3 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x00be, code lost:
        r6 = r3 + 1;
        r4[r3] = (byte) (((r1 >> 6) & 63) | 128);
        r1 = (r1 & 63) | 128;
        r3 = r6;
        r6 = r7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x00d0, code lost:
        r7 = r3 + 1;
        r4[r3] = (byte) ((r1 >> 12) | 224);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x00da, code lost:
        if (r7 < r4.length) goto L_0x00e1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:0x00dc, code lost:
        r4 = r0.finishCurrentSegment();
        r7 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x00e1, code lost:
        r3 = r7 + 1;
        r4[r7] = (byte) (((r1 >> 6) & 63) | 128);
        r1 = (r1 & 63) | 128;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:0x00f1, code lost:
        if (r3 < r4.length) goto L_0x00f8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x00f3, code lost:
        r4 = r0.finishCurrentSegment();
        r3 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:0x00f8, code lost:
        r7 = r3 + 1;
        r4[r3] = (byte) r1;
        r1 = r6;
        r3 = r7;
     */
    public byte[] quoteAsUTF8(String text) {
        ByteArrayBuilder bb = this._bytes;
        if (bb == null) {
            ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder((BufferRecycler) null);
            bb = byteArrayBuilder;
            this._bytes = byteArrayBuilder;
        }
        int inputPtr = 0;
        int inputEnd = text.length();
        int outputPtr = 0;
        byte[] outputBuffer = bb.resetAndGetFirstSegment();
        loop0:
        while (true) {
            if (inputPtr >= inputEnd) {
                break;
            }
            int[] escCodes = CharTypes.get7BitOutputEscapes();
            while (true) {
                int ch = text.charAt(inputPtr);
                if (ch <= 127 && escCodes[ch] == 0) {
                    if (outputPtr >= outputBuffer.length) {
                        outputBuffer = bb.finishCurrentSegment();
                        outputPtr = 0;
                    }
                    int outputPtr2 = outputPtr + 1;
                    outputBuffer[outputPtr] = (byte) ch;
                    inputPtr++;
                    if (inputPtr >= inputEnd) {
                        outputPtr = outputPtr2;
                        break loop0;
                    }
                    outputPtr = outputPtr2;
                }
            }
        }
        return this._bytes.completeAndCoalesce(outputPtr);
    }

    public byte[] encodeAsUTF8(String text) {
        int outputPtr;
        ByteArrayBuilder byteBuilder = this._bytes;
        if (byteBuilder == null) {
            ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder((BufferRecycler) null);
            byteBuilder = byteArrayBuilder;
            this._bytes = byteArrayBuilder;
        }
        int c = 0;
        int inputEnd = text.length();
        int outputPtr2 = 0;
        byte[] outputBuffer = byteBuilder.resetAndGetFirstSegment();
        int outputEnd = outputBuffer.length;
        loop0:
        while (true) {
            if (c >= inputEnd) {
                break;
            }
            int inputPtr = c + 1;
            int c2 = text.charAt(c);
            while (c2 <= 127) {
                if (outputPtr2 >= outputEnd) {
                    outputBuffer = byteBuilder.finishCurrentSegment();
                    outputEnd = outputBuffer.length;
                    outputPtr2 = 0;
                }
                int outputPtr3 = outputPtr2 + 1;
                outputBuffer[outputPtr2] = (byte) c2;
                if (inputPtr >= inputEnd) {
                    int c3 = inputPtr;
                    outputPtr2 = outputPtr3;
                    break loop0;
                }
                int inputPtr2 = inputPtr + 1;
                c2 = text.charAt(inputPtr);
                inputPtr = inputPtr2;
                outputPtr2 = outputPtr3;
            }
            if (outputPtr2 >= outputEnd) {
                outputBuffer = byteBuilder.finishCurrentSegment();
                outputEnd = outputBuffer.length;
                outputPtr2 = 0;
            }
            if (c2 < 2048) {
                int outputPtr4 = outputPtr2 + 1;
                outputBuffer[outputPtr2] = (byte) ((c2 >> 6) | PsExtractor.AUDIO_STREAM);
                outputPtr = outputPtr4;
            } else if (c2 < 55296 || c2 > 57343) {
                int outputPtr5 = outputPtr2 + 1;
                outputBuffer[outputPtr2] = (byte) ((c2 >> 12) | 224);
                if (outputPtr5 >= outputEnd) {
                    outputBuffer = byteBuilder.finishCurrentSegment();
                    outputEnd = outputBuffer.length;
                    outputPtr5 = 0;
                }
                outputPtr = outputPtr5 + 1;
                outputBuffer[outputPtr5] = (byte) (((c2 >> 6) & 63) | 128);
            } else {
                if (c2 > 56319) {
                    _illegal(c2);
                }
                if (inputPtr >= inputEnd) {
                    _illegal(c2);
                }
                int inputPtr3 = inputPtr + 1;
                c2 = _convert(c2, text.charAt(inputPtr));
                if (c2 > 1114111) {
                    _illegal(c2);
                }
                int outputPtr6 = outputPtr2 + 1;
                outputBuffer[outputPtr2] = (byte) ((c2 >> 18) | PsExtractor.VIDEO_STREAM_MASK);
                if (outputPtr6 >= outputEnd) {
                    outputBuffer = byteBuilder.finishCurrentSegment();
                    outputEnd = outputBuffer.length;
                    outputPtr6 = 0;
                }
                int outputPtr7 = outputPtr6 + 1;
                outputBuffer[outputPtr6] = (byte) (((c2 >> 12) & 63) | 128);
                if (outputPtr7 >= outputEnd) {
                    outputBuffer = byteBuilder.finishCurrentSegment();
                    outputEnd = outputBuffer.length;
                    outputPtr7 = 0;
                }
                int outputPtr8 = outputPtr7 + 1;
                outputBuffer[outputPtr7] = (byte) (((c2 >> 6) & 63) | 128);
                outputPtr = outputPtr8;
                inputPtr = inputPtr3;
            }
            if (outputPtr >= outputEnd) {
                outputBuffer = byteBuilder.finishCurrentSegment();
                outputEnd = outputBuffer.length;
                outputPtr = 0;
            }
            int outputPtr9 = outputPtr + 1;
            outputBuffer[outputPtr] = (byte) ((c2 & 63) | 128);
            c = inputPtr;
            outputPtr2 = outputPtr9;
        }
        return this._bytes.completeAndCoalesce(outputPtr2);
    }

    private int _appendNumeric(int value, char[] qbuf) {
        qbuf[1] = 'u';
        char[] cArr = HC;
        qbuf[4] = cArr[value >> 4];
        qbuf[5] = cArr[value & 15];
        return 6;
    }

    private int _appendNamed(int esc, char[] qbuf) {
        qbuf[1] = (char) esc;
        return 2;
    }

    private int _appendByte(int ch, int esc, ByteArrayBuilder bb, int ptr) {
        bb.setCurrentSegmentLength(ptr);
        bb.append(92);
        if (esc < 0) {
            bb.append(117);
            if (ch > 255) {
                int hi = ch >> 8;
                bb.append(HB[hi >> 4]);
                bb.append(HB[hi & 15]);
                ch &= 255;
            } else {
                bb.append(48);
                bb.append(48);
            }
            bb.append(HB[ch >> 4]);
            bb.append(HB[ch & 15]);
        } else {
            bb.append((byte) esc);
        }
        return bb.getCurrentSegmentLength();
    }

    private static int _convert(int p1, int p2) {
        if (p2 >= 56320 && p2 <= 57343) {
            return ((p1 - 55296) << 10) + 65536 + (p2 - 56320);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Broken surrogate pair: first char 0x");
        sb.append(Integer.toHexString(p1));
        sb.append(", second 0x");
        sb.append(Integer.toHexString(p2));
        sb.append("; illegal combination");
        throw new IllegalArgumentException(sb.toString());
    }

    private static void _illegal(int c) {
        throw new IllegalArgumentException(UTF8Writer.illegalSurrogateDesc(c));
    }
}
