package com.fasterxml.jackson.core.io;

import android.support.v4.internal.view.SupportMenu;
import com.fasterxml.jackson.core.base.GeneratorBase;
import java.io.CharConversionException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public class UTF32Reader extends Reader {
    protected static final int LAST_VALID_UNICODE_CHAR = 1114111;
    protected static final char NC = 0;
    protected final boolean _bigEndian;
    protected byte[] _buffer;
    protected int _byteCount;
    protected int _charCount;
    protected final IOContext _context;
    protected InputStream _in;
    protected int _length;
    protected final boolean _managedBuffers;
    protected int _ptr;
    protected char _surrogate = 0;
    protected char[] _tmpBuf;

    public UTF32Reader(IOContext ctxt, InputStream in, byte[] buf, int ptr, int len, boolean isBigEndian) {
        boolean z = false;
        this._context = ctxt;
        this._in = in;
        this._buffer = buf;
        this._ptr = ptr;
        this._length = len;
        this._bigEndian = isBigEndian;
        if (in != null) {
            z = true;
        }
        this._managedBuffers = z;
    }

    public void close() throws IOException {
        InputStream in = this._in;
        if (in != null) {
            this._in = null;
            freeBuffers();
            in.close();
        }
    }

    public int read() throws IOException {
        if (this._tmpBuf == null) {
            this._tmpBuf = new char[1];
        }
        if (read(this._tmpBuf, 0, 1) < 1) {
            return -1;
        }
        return this._tmpBuf[0];
    }

    public int read(char[] cbuf, int start, int len) throws IOException {
        int outPtr;
        int hi;
        int hi2;
        int outPtr2;
        char[] cArr = cbuf;
        int i = len;
        if (this._buffer == null) {
            return -1;
        }
        if (i < 1) {
            return i;
        }
        if (start < 0 || start + i > cArr.length) {
            reportBounds(cbuf, start, len);
        }
        int outPtr3 = start;
        int outEnd = i + start;
        char c = this._surrogate;
        if (c != 0) {
            outPtr = outPtr3 + 1;
            cArr[outPtr3] = c;
            this._surrogate = 0;
        } else {
            int left = this._length - this._ptr;
            if (left < 4 && !loadMore(left)) {
                if (left == 0) {
                    return -1;
                }
                reportUnexpectedEOF(this._length - this._ptr, 4);
            }
            outPtr = outPtr3;
        }
        int lastValidInputStart = this._length - 4;
        while (true) {
            if (outPtr >= outEnd) {
                break;
            }
            int ptr = this._ptr;
            if (this._bigEndian) {
                byte[] bArr = this._buffer;
                int i2 = (bArr[ptr] << 8) | (bArr[ptr + 1] & 255);
                hi = (bArr[ptr + 3] & 255) | ((bArr[ptr + 2] & 255) << 8);
                hi2 = i2;
            } else {
                byte[] bArr2 = this._buffer;
                hi = (bArr2[ptr] & 255) | ((bArr2[ptr + 1] & 255) << 8);
                hi2 = (bArr2[ptr + 3] << 8) | (bArr2[ptr + 2] & 255);
            }
            this._ptr += 4;
            if (hi2 != 0) {
                int hi3 = hi2 & SupportMenu.USER_MASK;
                int ch = ((hi3 - 1) << 16) | hi;
                if (hi3 > 16) {
                    reportInvalid(ch, outPtr - start, String.format(" (above 0x%08x)", new Object[]{Integer.valueOf(LAST_VALID_UNICODE_CHAR)}));
                }
                outPtr2 = outPtr + 1;
                cArr[outPtr] = (char) ((ch >> 10) + GeneratorBase.SURR1_FIRST);
                hi = (ch & 1023) | GeneratorBase.SURR2_FIRST;
                if (outPtr2 >= outEnd) {
                    this._surrogate = (char) ch;
                    outPtr = outPtr2;
                    break;
                }
            } else {
                outPtr2 = outPtr;
            }
            outPtr = outPtr2 + 1;
            cArr[outPtr2] = (char) hi;
            if (this._ptr > lastValidInputStart) {
                break;
            }
        }
        int actualLen = outPtr - start;
        this._charCount += actualLen;
        return actualLen;
    }

    private void reportUnexpectedEOF(int gotBytes, int needed) throws IOException {
        int bytePos = this._byteCount + gotBytes;
        int charPos = this._charCount;
        StringBuilder sb = new StringBuilder();
        sb.append("Unexpected EOF in the middle of a 4-byte UTF-32 char: got ");
        sb.append(gotBytes);
        sb.append(", needed ");
        sb.append(needed);
        sb.append(", at char #");
        sb.append(charPos);
        sb.append(", byte #");
        sb.append(bytePos);
        sb.append(")");
        throw new CharConversionException(sb.toString());
    }

    private void reportInvalid(int value, int offset, String msg) throws IOException {
        int bytePos = (this._byteCount + this._ptr) - 1;
        int charPos = this._charCount + offset;
        StringBuilder sb = new StringBuilder();
        sb.append("Invalid UTF-32 character 0x");
        sb.append(Integer.toHexString(value));
        sb.append(msg);
        sb.append(" at char #");
        sb.append(charPos);
        sb.append(", byte #");
        sb.append(bytePos);
        sb.append(")");
        throw new CharConversionException(sb.toString());
    }

    private boolean loadMore(int available) throws IOException {
        int count;
        this._byteCount += this._length - available;
        if (available > 0) {
            int i = this._ptr;
            if (i > 0) {
                byte[] bArr = this._buffer;
                System.arraycopy(bArr, i, bArr, 0, available);
                this._ptr = 0;
            }
            this._length = available;
        } else {
            this._ptr = 0;
            InputStream inputStream = this._in;
            int count2 = inputStream == null ? -1 : inputStream.read(this._buffer);
            if (count2 < 1) {
                this._length = 0;
                if (count2 < 0) {
                    if (this._managedBuffers) {
                        freeBuffers();
                    }
                    return false;
                }
                reportStrangeStream();
            }
            this._length = count2;
        }
        while (true) {
            int i2 = this._length;
            if (i2 >= 4) {
                return true;
            }
            InputStream inputStream2 = this._in;
            if (inputStream2 == null) {
                count = -1;
            } else {
                byte[] bArr2 = this._buffer;
                count = inputStream2.read(bArr2, i2, bArr2.length - i2);
            }
            if (count < 1) {
                if (count < 0) {
                    if (this._managedBuffers) {
                        freeBuffers();
                    }
                    reportUnexpectedEOF(this._length, 4);
                }
                reportStrangeStream();
            }
            this._length += count;
        }
    }

    private void freeBuffers() {
        byte[] buf = this._buffer;
        if (buf != null) {
            this._buffer = null;
            this._context.releaseReadIOBuffer(buf);
        }
    }

    private void reportBounds(char[] cbuf, int start, int len) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("read(buf,");
        sb.append(start);
        sb.append(",");
        sb.append(len);
        sb.append("), cbuf[");
        sb.append(cbuf.length);
        sb.append("]");
        throw new ArrayIndexOutOfBoundsException(sb.toString());
    }

    private void reportStrangeStream() throws IOException {
        throw new IOException("Strange I/O stream, returned 0 bytes on read");
    }
}
