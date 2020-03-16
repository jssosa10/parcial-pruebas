package com.fasterxml.jackson.core.io;

import com.google.android.exoplayer2.extractor.ts.PsExtractor;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public final class UTF8Writer extends Writer {
    static final int SURR1_FIRST = 55296;
    static final int SURR1_LAST = 56319;
    static final int SURR2_FIRST = 56320;
    static final int SURR2_LAST = 57343;
    private final IOContext _context;
    private OutputStream _out;
    private byte[] _outBuffer;
    private final int _outBufferEnd = (this._outBuffer.length - 4);
    private int _outPtr = 0;
    private int _surrogate;

    public UTF8Writer(IOContext ctxt, OutputStream out) {
        this._context = ctxt;
        this._out = out;
        this._outBuffer = ctxt.allocWriteEncodingBuffer();
    }

    public Writer append(char c) throws IOException {
        write((int) c);
        return this;
    }

    public void close() throws IOException {
        OutputStream outputStream = this._out;
        if (outputStream != null) {
            int i = this._outPtr;
            if (i > 0) {
                outputStream.write(this._outBuffer, 0, i);
                this._outPtr = 0;
            }
            OutputStream out = this._out;
            this._out = null;
            byte[] buf = this._outBuffer;
            if (buf != null) {
                this._outBuffer = null;
                this._context.releaseWriteEncodingBuffer(buf);
            }
            out.close();
            int code = this._surrogate;
            this._surrogate = 0;
            if (code > 0) {
                illegalSurrogate(code);
            }
        }
    }

    public void flush() throws IOException {
        OutputStream outputStream = this._out;
        if (outputStream != null) {
            int i = this._outPtr;
            if (i > 0) {
                outputStream.write(this._outBuffer, 0, i);
                this._outPtr = 0;
            }
            this._out.flush();
        }
    }

    public void write(char[] cbuf) throws IOException {
        write(cbuf, 0, cbuf.length);
    }

    /* JADX WARNING: Incorrect type for immutable var: ssa=char, code=int, for r11v3, types: [char, int] */
    public void write(char[] cbuf, int outPtr, int len) throws IOException {
        if (len < 2) {
            if (len == 1) {
                write((int) cbuf[outPtr]);
            }
            return;
        }
        if (this._surrogate > 0) {
            int off = outPtr + 1;
            len--;
            write(convertSurrogate(cbuf[outPtr]));
            outPtr = off;
        }
        int outPtr2 = this._outPtr;
        byte[] outBuf = this._outBuffer;
        int outBufLast = this._outBufferEnd;
        int len2 = len + outPtr;
        while (true) {
            if (outPtr >= len2) {
                break;
            }
            if (outPtr2 >= outBufLast) {
                this._out.write(outBuf, 0, outPtr2);
                outPtr2 = 0;
            }
            int off2 = outPtr + 1;
            int outPtr3 = cbuf[outPtr];
            if (outPtr3 < 128) {
                int outPtr4 = outPtr2 + 1;
                outBuf[outPtr2] = (byte) outPtr3;
                int maxInCount = len2 - off2;
                int maxOutCount = outBufLast - outPtr4;
                if (maxInCount > maxOutCount) {
                    maxInCount = maxOutCount;
                }
                int maxInCount2 = maxInCount + off2;
                int maxInCount3 = outPtr3;
                outPtr = off2;
                while (outPtr < maxInCount2) {
                    off2 = outPtr + 1;
                    char c = cbuf[outPtr];
                    if (c >= 128) {
                        outPtr3 = c;
                        outPtr2 = outPtr4;
                    } else {
                        int outPtr5 = outPtr4 + 1;
                        outBuf[outPtr4] = (byte) c;
                        outPtr4 = outPtr5;
                        outPtr = off2;
                    }
                }
                outPtr2 = outPtr4;
            }
            if (outPtr3 < 2048) {
                int outPtr6 = outPtr2 + 1;
                outBuf[outPtr2] = (byte) ((outPtr3 >> 6) | PsExtractor.AUDIO_STREAM);
                outPtr2 = outPtr6 + 1;
                outBuf[outPtr6] = (byte) (128 | (outPtr3 & 63));
                outPtr = off2;
            } else if (outPtr3 < 55296 || outPtr3 > 57343) {
                int outPtr7 = outPtr2 + 1;
                outBuf[outPtr2] = (byte) ((outPtr3 >> 12) | 224);
                int outPtr8 = outPtr7 + 1;
                outBuf[outPtr7] = (byte) (((outPtr3 >> 6) & 63) | 128);
                int outPtr9 = outPtr8 + 1;
                outBuf[outPtr8] = (byte) (128 | (outPtr3 & 63));
                outPtr = off2;
                outPtr2 = outPtr9;
            } else {
                if (outPtr3 > 56319) {
                    this._outPtr = outPtr2;
                    illegalSurrogate(outPtr3);
                }
                this._surrogate = outPtr3;
                if (off2 >= len2) {
                    int c2 = off2;
                    break;
                }
                int off3 = off2 + 1;
                int c3 = convertSurrogate(cbuf[off2]);
                if (c3 > 1114111) {
                    this._outPtr = outPtr2;
                    illegalSurrogate(c3);
                }
                int outPtr10 = outPtr2 + 1;
                outBuf[outPtr2] = (byte) ((c3 >> 18) | PsExtractor.VIDEO_STREAM_MASK);
                int outPtr11 = outPtr10 + 1;
                outBuf[outPtr10] = (byte) (((c3 >> 12) & 63) | 128);
                int outPtr12 = outPtr11 + 1;
                outBuf[outPtr11] = (byte) (((c3 >> 6) & 63) | 128);
                outPtr2 = outPtr12 + 1;
                outBuf[outPtr12] = (byte) (128 | (c3 & 63));
                outPtr = off3;
            }
        }
        this._outPtr = outPtr2;
    }

    public void write(int c) throws IOException {
        int ptr;
        if (this._surrogate > 0) {
            c = convertSurrogate(c);
        } else if (c >= 55296 && c <= 57343) {
            if (c > 56319) {
                illegalSurrogate(c);
            }
            this._surrogate = c;
            return;
        }
        int i = this._outPtr;
        if (i >= this._outBufferEnd) {
            this._out.write(this._outBuffer, 0, i);
            this._outPtr = 0;
        }
        if (c < 128) {
            byte[] bArr = this._outBuffer;
            int i2 = this._outPtr;
            this._outPtr = i2 + 1;
            bArr[i2] = (byte) c;
        } else {
            int ptr2 = this._outPtr;
            if (c < 2048) {
                byte[] bArr2 = this._outBuffer;
                int ptr3 = ptr2 + 1;
                bArr2[ptr2] = (byte) ((c >> 6) | PsExtractor.AUDIO_STREAM);
                ptr = ptr3 + 1;
                bArr2[ptr3] = (byte) (128 | (c & 63));
            } else if (c <= 65535) {
                byte[] bArr3 = this._outBuffer;
                int ptr4 = ptr2 + 1;
                bArr3[ptr2] = (byte) ((c >> 12) | 224);
                int ptr5 = ptr4 + 1;
                bArr3[ptr4] = (byte) (((c >> 6) & 63) | 128);
                int ptr6 = ptr5 + 1;
                bArr3[ptr5] = (byte) (128 | (c & 63));
                ptr = ptr6;
            } else {
                if (c > 1114111) {
                    illegalSurrogate(c);
                }
                byte[] bArr4 = this._outBuffer;
                int ptr7 = ptr2 + 1;
                bArr4[ptr2] = (byte) ((c >> 18) | PsExtractor.VIDEO_STREAM_MASK);
                int ptr8 = ptr7 + 1;
                bArr4[ptr7] = (byte) (((c >> 12) & 63) | 128);
                int ptr9 = ptr8 + 1;
                bArr4[ptr8] = (byte) (((c >> 6) & 63) | 128);
                ptr = ptr9 + 1;
                bArr4[ptr9] = (byte) (128 | (c & 63));
            }
            this._outPtr = ptr;
        }
    }

    public void write(String str) throws IOException {
        write(str, 0, str.length());
    }

    public void write(String str, int outPtr, int len) throws IOException {
        if (len < 2) {
            if (len == 1) {
                write((int) str.charAt(outPtr));
            }
            return;
        }
        if (this._surrogate > 0) {
            int off = outPtr + 1;
            len--;
            write(convertSurrogate(str.charAt(outPtr)));
            outPtr = off;
        }
        int outPtr2 = this._outPtr;
        byte[] outBuf = this._outBuffer;
        int outBufLast = this._outBufferEnd;
        int len2 = len + outPtr;
        while (true) {
            if (outPtr >= len2) {
                break;
            }
            if (outPtr2 >= outBufLast) {
                this._out.write(outBuf, 0, outPtr2);
                outPtr2 = 0;
            }
            int off2 = outPtr + 1;
            int outPtr3 = str.charAt(outPtr);
            if (outPtr3 < 128) {
                int outPtr4 = outPtr2 + 1;
                outBuf[outPtr2] = (byte) outPtr3;
                int maxInCount = len2 - off2;
                int maxOutCount = outBufLast - outPtr4;
                if (maxInCount > maxOutCount) {
                    maxInCount = maxOutCount;
                }
                int maxInCount2 = maxInCount + off2;
                int maxInCount3 = outPtr3;
                outPtr = off2;
                while (outPtr < maxInCount2) {
                    off2 = outPtr + 1;
                    int charAt = str.charAt(outPtr);
                    if (charAt >= 128) {
                        outPtr3 = charAt;
                        outPtr2 = outPtr4;
                    } else {
                        int outPtr5 = outPtr4 + 1;
                        outBuf[outPtr4] = (byte) charAt;
                        outPtr4 = outPtr5;
                        outPtr = off2;
                    }
                }
                outPtr2 = outPtr4;
            }
            if (outPtr3 < 2048) {
                int outPtr6 = outPtr2 + 1;
                outBuf[outPtr2] = (byte) ((outPtr3 >> 6) | PsExtractor.AUDIO_STREAM);
                outPtr2 = outPtr6 + 1;
                outBuf[outPtr6] = (byte) (128 | (outPtr3 & 63));
                outPtr = off2;
            } else if (outPtr3 < 55296 || outPtr3 > 57343) {
                int outPtr7 = outPtr2 + 1;
                outBuf[outPtr2] = (byte) ((outPtr3 >> 12) | 224);
                int outPtr8 = outPtr7 + 1;
                outBuf[outPtr7] = (byte) (((outPtr3 >> 6) & 63) | 128);
                int outPtr9 = outPtr8 + 1;
                outBuf[outPtr8] = (byte) (128 | (outPtr3 & 63));
                outPtr = off2;
                outPtr2 = outPtr9;
            } else {
                if (outPtr3 > 56319) {
                    this._outPtr = outPtr2;
                    illegalSurrogate(outPtr3);
                }
                this._surrogate = outPtr3;
                if (off2 >= len2) {
                    int c = off2;
                    break;
                }
                int off3 = off2 + 1;
                int c2 = convertSurrogate(str.charAt(off2));
                if (c2 > 1114111) {
                    this._outPtr = outPtr2;
                    illegalSurrogate(c2);
                }
                int outPtr10 = outPtr2 + 1;
                outBuf[outPtr2] = (byte) ((c2 >> 18) | PsExtractor.VIDEO_STREAM_MASK);
                int outPtr11 = outPtr10 + 1;
                outBuf[outPtr10] = (byte) (((c2 >> 12) & 63) | 128);
                int outPtr12 = outPtr11 + 1;
                outBuf[outPtr11] = (byte) (((c2 >> 6) & 63) | 128);
                outPtr2 = outPtr12 + 1;
                outBuf[outPtr12] = (byte) (128 | (c2 & 63));
                outPtr = off3;
            }
        }
        this._outPtr = outPtr2;
    }

    /* access modifiers changed from: protected */
    public int convertSurrogate(int secondPart) throws IOException {
        int firstPart = this._surrogate;
        this._surrogate = 0;
        if (secondPart >= 56320 && secondPart <= 57343) {
            return ((firstPart - 55296) << 10) + 65536 + (secondPart - 56320);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Broken surrogate pair: first char 0x");
        sb.append(Integer.toHexString(firstPart));
        sb.append(", second 0x");
        sb.append(Integer.toHexString(secondPart));
        sb.append("; illegal combination");
        throw new IOException(sb.toString());
    }

    protected static void illegalSurrogate(int code) throws IOException {
        throw new IOException(illegalSurrogateDesc(code));
    }

    protected static String illegalSurrogateDesc(int code) {
        if (code > 1114111) {
            StringBuilder sb = new StringBuilder();
            sb.append("Illegal character point (0x");
            sb.append(Integer.toHexString(code));
            sb.append(") to output; max is 0x10FFFF as per RFC 4627");
            return sb.toString();
        } else if (code < 55296) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Illegal character point (0x");
            sb2.append(Integer.toHexString(code));
            sb2.append(") to output");
            return sb2.toString();
        } else if (code <= 56319) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("Unmatched first part of surrogate pair (0x");
            sb3.append(Integer.toHexString(code));
            sb3.append(")");
            return sb3.toString();
        } else {
            StringBuilder sb4 = new StringBuilder();
            sb4.append("Unmatched second part of surrogate pair (0x");
            sb4.append(Integer.toHexString(code));
            sb4.append(")");
            return sb4.toString();
        }
    }
}
