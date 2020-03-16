package com.fasterxml.jackson.core.json;

import android.support.v4.internal.view.SupportMenu;
import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.NumberOutput;
import com.google.android.exoplayer2.extractor.ts.PsExtractor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;

public class UTF8JsonGenerator extends JsonGeneratorImpl {
    private static final byte BYTE_0 = 48;
    private static final byte BYTE_BACKSLASH = 92;
    private static final byte BYTE_COLON = 58;
    private static final byte BYTE_COMMA = 44;
    private static final byte BYTE_LBRACKET = 91;
    private static final byte BYTE_LCURLY = 123;
    private static final byte BYTE_RBRACKET = 93;
    private static final byte BYTE_RCURLY = 125;
    private static final byte BYTE_u = 117;
    private static final byte[] FALSE_BYTES = {102, 97, 108, 115, 101};
    private static final byte[] HEX_CHARS = CharTypes.copyHexBytes();
    private static final int MAX_BYTES_TO_BUFFER = 512;
    private static final byte[] NULL_BYTES = {110, BYTE_u, 108, 108};
    private static final byte[] TRUE_BYTES = {116, 114, BYTE_u, 101};
    protected boolean _bufferRecyclable;
    protected char[] _charBuffer;
    protected final int _charBufferLength;
    protected byte[] _entityBuffer;
    protected byte[] _outputBuffer;
    protected final int _outputEnd;
    protected final int _outputMaxContiguous;
    protected final OutputStream _outputStream;
    protected int _outputTail;
    protected byte _quoteChar = 34;

    public UTF8JsonGenerator(IOContext ctxt, int features, ObjectCodec codec, OutputStream out) {
        super(ctxt, features, codec);
        this._outputStream = out;
        this._bufferRecyclable = true;
        this._outputBuffer = ctxt.allocWriteEncodingBuffer();
        this._outputEnd = this._outputBuffer.length;
        this._outputMaxContiguous = this._outputEnd >> 3;
        this._charBuffer = ctxt.allocConcatBuffer();
        this._charBufferLength = this._charBuffer.length;
        if (isEnabled(Feature.ESCAPE_NON_ASCII)) {
            setHighestNonEscapedChar(127);
        }
    }

    public UTF8JsonGenerator(IOContext ctxt, int features, ObjectCodec codec, OutputStream out, byte[] outputBuffer, int outputOffset, boolean bufferRecyclable) {
        super(ctxt, features, codec);
        this._outputStream = out;
        this._bufferRecyclable = bufferRecyclable;
        this._outputTail = outputOffset;
        this._outputBuffer = outputBuffer;
        this._outputEnd = this._outputBuffer.length;
        this._outputMaxContiguous = this._outputEnd >> 3;
        this._charBuffer = ctxt.allocConcatBuffer();
        this._charBufferLength = this._charBuffer.length;
    }

    public Object getOutputTarget() {
        return this._outputStream;
    }

    public int getOutputBuffered() {
        return this._outputTail;
    }

    public void writeFieldName(String name) throws IOException {
        if (this._cfgPrettyPrinter != null) {
            _writePPFieldName(name);
            return;
        }
        int status = this._writeContext.writeFieldName(name);
        if (status == 4) {
            _reportError("Can not write a field name, expecting a value");
        }
        if (status == 1) {
            if (this._outputTail >= this._outputEnd) {
                _flushBuffer();
            }
            byte[] bArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            bArr[i] = BYTE_COMMA;
        }
        if (this._cfgUnqNames) {
            _writeStringSegments(name, false);
            return;
        }
        int len = name.length();
        if (len > this._charBufferLength) {
            _writeStringSegments(name, true);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr2[i2] = this._quoteChar;
        if (len <= this._outputMaxContiguous) {
            if (this._outputTail + len > this._outputEnd) {
                _flushBuffer();
            }
            _writeStringSegment(name, 0, len);
        } else {
            _writeStringSegments(name, 0, len);
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr3 = this._outputBuffer;
        int i3 = this._outputTail;
        this._outputTail = i3 + 1;
        bArr3[i3] = this._quoteChar;
    }

    public void writeFieldName(SerializableString name) throws IOException {
        if (this._cfgPrettyPrinter != null) {
            _writePPFieldName(name);
            return;
        }
        int status = this._writeContext.writeFieldName(name.getValue());
        if (status == 4) {
            _reportError("Can not write a field name, expecting a value");
        }
        if (status == 1) {
            if (this._outputTail >= this._outputEnd) {
                _flushBuffer();
            }
            byte[] bArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            bArr[i] = BYTE_COMMA;
        }
        if (this._cfgUnqNames) {
            _writeUnq(name);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr2[i2] = this._quoteChar;
        int len = name.appendQuotedUTF8(bArr2, this._outputTail);
        if (len < 0) {
            _writeBytes(name.asQuotedUTF8());
        } else {
            this._outputTail += len;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr3 = this._outputBuffer;
        int i3 = this._outputTail;
        this._outputTail = i3 + 1;
        bArr3[i3] = this._quoteChar;
    }

    private final void _writeUnq(SerializableString name) throws IOException {
        int len = name.appendQuotedUTF8(this._outputBuffer, this._outputTail);
        if (len < 0) {
            _writeBytes(name.asQuotedUTF8());
        } else {
            this._outputTail += len;
        }
    }

    public final void writeStartArray() throws IOException {
        _verifyValueWrite("start an array");
        this._writeContext = this._writeContext.createChildArrayContext();
        if (this._cfgPrettyPrinter != null) {
            this._cfgPrettyPrinter.writeStartArray(this);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = BYTE_LBRACKET;
    }

    public final void writeEndArray() throws IOException {
        if (!this._writeContext.inArray()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Current context not Array but ");
            sb.append(this._writeContext.typeDesc());
            _reportError(sb.toString());
        }
        if (this._cfgPrettyPrinter != null) {
            this._cfgPrettyPrinter.writeEndArray(this, this._writeContext.getEntryCount());
        } else {
            if (this._outputTail >= this._outputEnd) {
                _flushBuffer();
            }
            byte[] bArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            bArr[i] = BYTE_RBRACKET;
        }
        this._writeContext = this._writeContext.clearAndGetParent();
    }

    public final void writeStartObject() throws IOException {
        _verifyValueWrite("start an object");
        this._writeContext = this._writeContext.createChildObjectContext();
        if (this._cfgPrettyPrinter != null) {
            this._cfgPrettyPrinter.writeStartObject(this);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = BYTE_LCURLY;
    }

    public void writeStartObject(Object forValue) throws IOException {
        _verifyValueWrite("start an object");
        JsonWriteContext ctxt = this._writeContext.createChildObjectContext();
        this._writeContext = ctxt;
        if (forValue != null) {
            ctxt.setCurrentValue(forValue);
        }
        if (this._cfgPrettyPrinter != null) {
            this._cfgPrettyPrinter.writeStartObject(this);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = BYTE_LCURLY;
    }

    public final void writeEndObject() throws IOException {
        if (!this._writeContext.inObject()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Current context not Object but ");
            sb.append(this._writeContext.typeDesc());
            _reportError(sb.toString());
        }
        if (this._cfgPrettyPrinter != null) {
            this._cfgPrettyPrinter.writeEndObject(this, this._writeContext.getEntryCount());
        } else {
            if (this._outputTail >= this._outputEnd) {
                _flushBuffer();
            }
            byte[] bArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            bArr[i] = BYTE_RCURLY;
        }
        this._writeContext = this._writeContext.clearAndGetParent();
    }

    /* access modifiers changed from: protected */
    public final void _writePPFieldName(String name) throws IOException {
        int status = this._writeContext.writeFieldName(name);
        if (status == 4) {
            _reportError("Can not write a field name, expecting a value");
        }
        if (status == 1) {
            this._cfgPrettyPrinter.writeObjectEntrySeparator(this);
        } else {
            this._cfgPrettyPrinter.beforeObjectEntries(this);
        }
        if (this._cfgUnqNames) {
            _writeStringSegments(name, false);
            return;
        }
        int len = name.length();
        if (len > this._charBufferLength) {
            _writeStringSegments(name, true);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = this._quoteChar;
        name.getChars(0, len, this._charBuffer, 0);
        if (len <= this._outputMaxContiguous) {
            if (this._outputTail + len > this._outputEnd) {
                _flushBuffer();
            }
            _writeStringSegment(this._charBuffer, 0, len);
        } else {
            _writeStringSegments(this._charBuffer, 0, len);
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr2[i2] = this._quoteChar;
    }

    /* access modifiers changed from: protected */
    public final void _writePPFieldName(SerializableString name) throws IOException {
        int status = this._writeContext.writeFieldName(name.getValue());
        if (status == 4) {
            _reportError("Can not write a field name, expecting a value");
        }
        if (status == 1) {
            this._cfgPrettyPrinter.writeObjectEntrySeparator(this);
        } else {
            this._cfgPrettyPrinter.beforeObjectEntries(this);
        }
        boolean addQuotes = true ^ this._cfgUnqNames;
        if (addQuotes) {
            if (this._outputTail >= this._outputEnd) {
                _flushBuffer();
            }
            byte[] bArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            bArr[i] = this._quoteChar;
        }
        _writeBytes(name.asQuotedUTF8());
        if (addQuotes) {
            if (this._outputTail >= this._outputEnd) {
                _flushBuffer();
            }
            byte[] bArr2 = this._outputBuffer;
            int i2 = this._outputTail;
            this._outputTail = i2 + 1;
            bArr2[i2] = this._quoteChar;
        }
    }

    public void writeString(String text) throws IOException {
        _verifyValueWrite("write a string");
        if (text == null) {
            _writeNull();
            return;
        }
        int len = text.length();
        if (len > this._outputMaxContiguous) {
            _writeStringSegments(text, true);
            return;
        }
        if (this._outputTail + len >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = this._quoteChar;
        _writeStringSegment(text, 0, len);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr2[i2] = this._quoteChar;
    }

    public void writeString(Reader reader, int len) throws IOException {
        _verifyValueWrite("write a string");
        if (reader == null) {
            _reportError("null reader");
        }
        int toRead = len >= 0 ? len : Integer.MAX_VALUE;
        char[] buf = this._charBuffer;
        if (this._outputTail + len >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = this._quoteChar;
        while (toRead > 0) {
            int numRead = reader.read(buf, 0, Math.min(toRead, buf.length));
            if (numRead <= 0) {
                break;
            }
            if (this._outputTail + len >= this._outputEnd) {
                _flushBuffer();
            }
            _writeStringSegments(buf, 0, numRead);
            toRead -= numRead;
        }
        if (this._outputTail + len >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr2[i2] = this._quoteChar;
        if (toRead > 0 && len >= 0) {
            _reportError("Didn't read enough from reader");
        }
    }

    public void writeString(char[] text, int offset, int len) throws IOException {
        _verifyValueWrite("write a string");
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = this._quoteChar;
        if (len <= this._outputMaxContiguous) {
            if (this._outputTail + len > this._outputEnd) {
                _flushBuffer();
            }
            _writeStringSegment(text, offset, len);
        } else {
            _writeStringSegments(text, offset, len);
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr2[i2] = this._quoteChar;
    }

    public final void writeString(SerializableString text) throws IOException {
        _verifyValueWrite("write a string");
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = this._quoteChar;
        int len = text.appendQuotedUTF8(bArr, this._outputTail);
        if (len < 0) {
            _writeBytes(text.asQuotedUTF8());
        } else {
            this._outputTail += len;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr2[i2] = this._quoteChar;
    }

    public void writeRawUTF8String(byte[] text, int offset, int length) throws IOException {
        _verifyValueWrite("write a string");
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = this._quoteChar;
        _writeBytes(text, offset, length);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr2[i2] = this._quoteChar;
    }

    public void writeUTF8String(byte[] text, int offset, int len) throws IOException {
        _verifyValueWrite("write a string");
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = this._quoteChar;
        if (len <= this._outputMaxContiguous) {
            _writeUTF8Segment(text, offset, len);
        } else {
            _writeUTF8Segments(text, offset, len);
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr2[i2] = this._quoteChar;
    }

    public void writeRaw(String text) throws IOException {
        int len = text.length();
        char[] buf = this._charBuffer;
        if (len <= buf.length) {
            text.getChars(0, len, buf, 0);
            writeRaw(buf, 0, len);
            return;
        }
        writeRaw(text, 0, len);
    }

    public void writeRaw(String text, int offset, int len) throws IOException {
        char[] buf = this._charBuffer;
        int cbufLen = buf.length;
        if (len <= cbufLen) {
            text.getChars(offset, offset + len, buf, 0);
            writeRaw(buf, 0, len);
            return;
        }
        int i = this._outputEnd;
        int maxChunk = Math.min(cbufLen, (i >> 2) + (i >> 4));
        int maxBytes = maxChunk * 3;
        while (len > 0) {
            int len2 = Math.min(maxChunk, len);
            text.getChars(offset, offset + len2, buf, 0);
            if (this._outputTail + maxBytes > this._outputEnd) {
                _flushBuffer();
            }
            if (len2 > 1) {
                char ch = buf[len2 - 1];
                if (ch >= 55296 && ch <= 56319) {
                    len2--;
                }
            }
            _writeRawSegment(buf, 0, len2);
            offset += len2;
            len -= len2;
        }
    }

    public void writeRaw(SerializableString text) throws IOException {
        byte[] raw = text.asUnquotedUTF8();
        if (raw.length > 0) {
            _writeBytes(raw);
        }
    }

    public void writeRawValue(SerializableString text) throws IOException {
        _verifyValueWrite("write a raw (unencoded) value");
        byte[] raw = text.asUnquotedUTF8();
        if (raw.length > 0) {
            _writeBytes(raw);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x001c, code lost:
        r0 = r6 + 1;
        r6 = r5[r6];
     */
    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0023, code lost:
        if (r6 >= 2048) goto L_0x0043;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0025, code lost:
        r1 = r4._outputBuffer;
        r2 = r4._outputTail;
        r4._outputTail = r2 + 1;
        r1[r2] = (byte) ((r6 >> 6) | com.google.android.exoplayer2.extractor.ts.PsExtractor.AUDIO_STREAM);
        r2 = r4._outputTail;
        r4._outputTail = r2 + 1;
        r1[r2] = (byte) ((r6 & '?') | 128);
        r6 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0043, code lost:
        r6 = _outputRawMultiByteChar(r6, r5, r0, r7);
     */
    public final void writeRaw(char[] cbuf, int offset, int len) throws IOException {
        int len3 = len + len + len;
        int i = this._outputTail + len3;
        int i2 = this._outputEnd;
        if (i > i2) {
            if (i2 < len3) {
                _writeSegmentedRaw(cbuf, offset, len);
                return;
            }
            _flushBuffer();
        }
        int len2 = len + offset;
        loop0:
        while (offset < len2) {
            while (true) {
                char ch = cbuf[offset];
                if (ch <= 127) {
                    byte[] bArr = this._outputBuffer;
                    int i3 = this._outputTail;
                    this._outputTail = i3 + 1;
                    bArr[i3] = (byte) ch;
                    offset++;
                    if (offset >= len2) {
                        break loop0;
                    }
                } else {
                    break;
                }
            }
        }
    }

    public void writeRaw(char ch) throws IOException {
        if (this._outputTail + 3 >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bbuf = this._outputBuffer;
        if (ch <= 127) {
            int i = this._outputTail;
            this._outputTail = i + 1;
            bbuf[i] = (byte) ch;
        } else if (ch < 2048) {
            int i2 = this._outputTail;
            this._outputTail = i2 + 1;
            bbuf[i2] = (byte) ((ch >> 6) | PsExtractor.AUDIO_STREAM);
            int i3 = this._outputTail;
            this._outputTail = i3 + 1;
            bbuf[i3] = (byte) ((ch & '?') | 128);
        } else {
            _outputRawMultiByteChar(ch, null, 0, 0);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x003d, code lost:
        r9 = _outputRawMultiByteChar(r9, r8, r3, r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:5:0x0015, code lost:
        if ((r7._outputTail + 3) < r7._outputEnd) goto L_0x001a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:6:0x0017, code lost:
        _flushBuffer();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:7:0x001a, code lost:
        r3 = r9 + 1;
        r9 = r8[r9];
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x0020, code lost:
        if (r9 >= 2048) goto L_0x003d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0022, code lost:
        r5 = r7._outputTail;
        r7._outputTail = r5 + 1;
        r1[r5] = (byte) ((r9 >> 6) | com.google.android.exoplayer2.extractor.ts.PsExtractor.AUDIO_STREAM);
        r5 = r7._outputTail;
        r7._outputTail = r5 + 1;
        r1[r5] = (byte) (128 | (r9 & '?'));
        r9 = r3;
     */
    private final void _writeSegmentedRaw(char[] cbuf, int offset, int len) throws IOException {
        int end = this._outputEnd;
        byte[] bbuf = this._outputBuffer;
        int inputEnd = offset + len;
        while (offset < inputEnd) {
            while (true) {
                char ch = cbuf[offset];
                if (ch >= 128) {
                    break;
                }
                if (this._outputTail >= end) {
                    _flushBuffer();
                }
                int i = this._outputTail;
                this._outputTail = i + 1;
                bbuf[i] = (byte) ch;
                offset++;
                if (offset >= inputEnd) {
                    return;
                }
            }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:3:0x0008, code lost:
        r0 = r6 + 1;
        r6 = r5[r6];
     */
    /* JADX WARNING: Code restructure failed: missing block: B:4:0x000f, code lost:
        if (r6 >= 2048) goto L_0x002f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:5:0x0011, code lost:
        r1 = r4._outputBuffer;
        r2 = r4._outputTail;
        r4._outputTail = r2 + 1;
        r1[r2] = (byte) ((r6 >> 6) | com.google.android.exoplayer2.extractor.ts.PsExtractor.AUDIO_STREAM);
        r2 = r4._outputTail;
        r4._outputTail = r2 + 1;
        r1[r2] = (byte) ((r6 & '?') | 128);
        r6 = r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:6:0x002f, code lost:
        r6 = _outputRawMultiByteChar(r6, r5, r0, r7);
     */
    private void _writeRawSegment(char[] cbuf, int offset, int end) throws IOException {
        while (offset < end) {
            while (true) {
                char ch = cbuf[offset];
                if (ch > 127) {
                    break;
                }
                byte[] bArr = this._outputBuffer;
                int i = this._outputTail;
                this._outputTail = i + 1;
                bArr[i] = (byte) ch;
                offset++;
                if (offset >= end) {
                    return;
                }
            }
        }
    }

    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException, JsonGenerationException {
        _verifyValueWrite("write a binary value");
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = this._quoteChar;
        _writeBinary(b64variant, data, offset, offset + len);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr2[i2] = this._quoteChar;
    }

    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws IOException, JsonGenerationException {
        int missing;
        _verifyValueWrite("write a binary value");
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = this._quoteChar;
        byte[] encodingBuffer = this._ioContext.allocBase64Buffer();
        if (dataLength < 0) {
            try {
                missing = _writeBinary(b64variant, data, encodingBuffer);
            } catch (Throwable th) {
                this._ioContext.releaseBase64Buffer(encodingBuffer);
                throw th;
            }
        } else {
            int missing2 = _writeBinary(b64variant, data, encodingBuffer, dataLength);
            if (missing2 > 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("Too few bytes available: missing ");
                sb.append(missing2);
                sb.append(" bytes (out of ");
                sb.append(dataLength);
                sb.append(")");
                _reportError(sb.toString());
            }
            missing = dataLength;
        }
        this._ioContext.releaseBase64Buffer(encodingBuffer);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr2[i2] = this._quoteChar;
        return missing;
    }

    public void writeNumber(short s) throws IOException {
        _verifyValueWrite("write a number");
        if (this._outputTail + 6 >= this._outputEnd) {
            _flushBuffer();
        }
        if (this._cfgNumbersAsStrings) {
            _writeQuotedShort(s);
        } else {
            this._outputTail = NumberOutput.outputInt((int) s, this._outputBuffer, this._outputTail);
        }
    }

    private final void _writeQuotedShort(short s) throws IOException {
        if (this._outputTail + 8 >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = this._quoteChar;
        this._outputTail = NumberOutput.outputInt((int) s, bArr, this._outputTail);
        byte[] bArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr2[i2] = this._quoteChar;
    }

    public void writeNumber(int i) throws IOException {
        _verifyValueWrite("write a number");
        if (this._outputTail + 11 >= this._outputEnd) {
            _flushBuffer();
        }
        if (this._cfgNumbersAsStrings) {
            _writeQuotedInt(i);
        } else {
            this._outputTail = NumberOutput.outputInt(i, this._outputBuffer, this._outputTail);
        }
    }

    private final void _writeQuotedInt(int i) throws IOException {
        if (this._outputTail + 13 >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr[i2] = this._quoteChar;
        this._outputTail = NumberOutput.outputInt(i, bArr, this._outputTail);
        byte[] bArr2 = this._outputBuffer;
        int i3 = this._outputTail;
        this._outputTail = i3 + 1;
        bArr2[i3] = this._quoteChar;
    }

    public void writeNumber(long l) throws IOException {
        _verifyValueWrite("write a number");
        if (this._cfgNumbersAsStrings) {
            _writeQuotedLong(l);
            return;
        }
        if (this._outputTail + 21 >= this._outputEnd) {
            _flushBuffer();
        }
        this._outputTail = NumberOutput.outputLong(l, this._outputBuffer, this._outputTail);
    }

    private final void _writeQuotedLong(long l) throws IOException {
        if (this._outputTail + 23 >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = this._quoteChar;
        this._outputTail = NumberOutput.outputLong(l, bArr, this._outputTail);
        byte[] bArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr2[i2] = this._quoteChar;
    }

    public void writeNumber(BigInteger value) throws IOException {
        _verifyValueWrite("write a number");
        if (value == null) {
            _writeNull();
        } else if (this._cfgNumbersAsStrings) {
            _writeQuotedRaw(value.toString());
        } else {
            writeRaw(value.toString());
        }
    }

    public void writeNumber(double d) throws IOException {
        if (this._cfgNumbersAsStrings || ((Double.isNaN(d) || Double.isInfinite(d)) && Feature.QUOTE_NON_NUMERIC_NUMBERS.enabledIn(this._features))) {
            writeString(String.valueOf(d));
            return;
        }
        _verifyValueWrite("write a number");
        writeRaw(String.valueOf(d));
    }

    public void writeNumber(float f) throws IOException {
        if (this._cfgNumbersAsStrings || ((Float.isNaN(f) || Float.isInfinite(f)) && Feature.QUOTE_NON_NUMERIC_NUMBERS.enabledIn(this._features))) {
            writeString(String.valueOf(f));
            return;
        }
        _verifyValueWrite("write a number");
        writeRaw(String.valueOf(f));
    }

    public void writeNumber(BigDecimal value) throws IOException {
        _verifyValueWrite("write a number");
        if (value == null) {
            _writeNull();
        } else if (this._cfgNumbersAsStrings) {
            _writeQuotedRaw(_asString(value));
        } else {
            writeRaw(_asString(value));
        }
    }

    public void writeNumber(String encodedValue) throws IOException {
        _verifyValueWrite("write a number");
        if (this._cfgNumbersAsStrings) {
            _writeQuotedRaw(encodedValue);
        } else {
            writeRaw(encodedValue);
        }
    }

    private final void _writeQuotedRaw(String value) throws IOException {
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bArr[i] = this._quoteChar;
        writeRaw(value);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] bArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bArr2[i2] = this._quoteChar;
    }

    public void writeBoolean(boolean state) throws IOException {
        _verifyValueWrite("write a boolean value");
        if (this._outputTail + 5 >= this._outputEnd) {
            _flushBuffer();
        }
        byte[] keyword = state ? TRUE_BYTES : FALSE_BYTES;
        int len = keyword.length;
        System.arraycopy(keyword, 0, this._outputBuffer, this._outputTail, len);
        this._outputTail += len;
    }

    public void writeNull() throws IOException {
        _verifyValueWrite("write a null");
        _writeNull();
    }

    /* access modifiers changed from: protected */
    public final void _verifyValueWrite(String typeMsg) throws IOException {
        byte b;
        int status = this._writeContext.writeValue();
        if (this._cfgPrettyPrinter != null) {
            _verifyPrettyValueWrite(typeMsg, status);
        } else if (status != 5) {
            switch (status) {
                case 1:
                    b = BYTE_COMMA;
                    break;
                case 2:
                    b = BYTE_COLON;
                    break;
                case 3:
                    if (this._rootValueSeparator != null) {
                        byte[] raw = this._rootValueSeparator.asUnquotedUTF8();
                        if (raw.length > 0) {
                            _writeBytes(raw);
                        }
                    }
                    return;
                default:
                    return;
            }
            if (this._outputTail >= this._outputEnd) {
                _flushBuffer();
            }
            byte[] bArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            bArr[i] = b;
        } else {
            _reportCantWriteValueExpectName(typeMsg);
        }
    }

    public void flush() throws IOException {
        _flushBuffer();
        if (this._outputStream != null && isEnabled(Feature.FLUSH_PASSED_TO_STREAM)) {
            this._outputStream.flush();
        }
    }

    public void close() throws IOException {
        super.close();
        if (this._outputBuffer != null && isEnabled(Feature.AUTO_CLOSE_JSON_CONTENT)) {
            while (true) {
                JsonStreamContext ctxt = getOutputContext();
                if (!ctxt.inArray()) {
                    if (!ctxt.inObject()) {
                        break;
                    }
                    writeEndObject();
                } else {
                    writeEndArray();
                }
            }
        }
        _flushBuffer();
        this._outputTail = 0;
        if (this._outputStream != null) {
            if (this._ioContext.isResourceManaged() || isEnabled(Feature.AUTO_CLOSE_TARGET)) {
                this._outputStream.close();
            } else if (isEnabled(Feature.FLUSH_PASSED_TO_STREAM)) {
                this._outputStream.flush();
            }
        }
        _releaseBuffers();
    }

    /* access modifiers changed from: protected */
    public void _releaseBuffers() {
        byte[] buf = this._outputBuffer;
        if (buf != null && this._bufferRecyclable) {
            this._outputBuffer = null;
            this._ioContext.releaseWriteEncodingBuffer(buf);
        }
        char[] cbuf = this._charBuffer;
        if (cbuf != null) {
            this._charBuffer = null;
            this._ioContext.releaseConcatBuffer(cbuf);
        }
    }

    private final void _writeBytes(byte[] bytes) throws IOException {
        int len = bytes.length;
        if (this._outputTail + len > this._outputEnd) {
            _flushBuffer();
            if (len > 512) {
                this._outputStream.write(bytes, 0, len);
                return;
            }
        }
        System.arraycopy(bytes, 0, this._outputBuffer, this._outputTail, len);
        this._outputTail += len;
    }

    private final void _writeBytes(byte[] bytes, int offset, int len) throws IOException {
        if (this._outputTail + len > this._outputEnd) {
            _flushBuffer();
            if (len > 512) {
                this._outputStream.write(bytes, offset, len);
                return;
            }
        }
        System.arraycopy(bytes, offset, this._outputBuffer, this._outputTail, len);
        this._outputTail += len;
    }

    private final void _writeStringSegments(String text, boolean addQuotes) throws IOException {
        if (addQuotes) {
            if (this._outputTail >= this._outputEnd) {
                _flushBuffer();
            }
            byte[] bArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            bArr[i] = this._quoteChar;
        }
        int left = text.length();
        int offset = 0;
        while (left > 0) {
            int len = Math.min(this._outputMaxContiguous, left);
            if (this._outputTail + len > this._outputEnd) {
                _flushBuffer();
            }
            _writeStringSegment(text, offset, len);
            offset += len;
            left -= len;
        }
        if (addQuotes) {
            if (this._outputTail >= this._outputEnd) {
                _flushBuffer();
            }
            byte[] bArr2 = this._outputBuffer;
            int i2 = this._outputTail;
            this._outputTail = i2 + 1;
            bArr2[i2] = this._quoteChar;
        }
    }

    private final void _writeStringSegments(char[] cbuf, int offset, int totalLen) throws IOException {
        do {
            int len = Math.min(this._outputMaxContiguous, totalLen);
            if (this._outputTail + len > this._outputEnd) {
                _flushBuffer();
            }
            _writeStringSegment(cbuf, offset, len);
            offset += len;
            totalLen -= len;
        } while (totalLen > 0);
    }

    private final void _writeStringSegments(String text, int offset, int totalLen) throws IOException {
        do {
            int len = Math.min(this._outputMaxContiguous, totalLen);
            if (this._outputTail + len > this._outputEnd) {
                _flushBuffer();
            }
            _writeStringSegment(text, offset, len);
            offset += len;
            totalLen -= len;
        } while (totalLen > 0);
    }

    private final void _writeStringSegment(char[] cbuf, int offset, int len) throws IOException {
        int len2 = len + offset;
        int outputPtr = this._outputTail;
        byte[] outputBuffer = this._outputBuffer;
        int[] escCodes = this._outputEscapes;
        while (offset < len2) {
            char ch = cbuf[offset];
            if (ch > 127 || escCodes[ch] != 0) {
                break;
            }
            int outputPtr2 = outputPtr + 1;
            outputBuffer[outputPtr] = (byte) ch;
            offset++;
            outputPtr = outputPtr2;
        }
        this._outputTail = outputPtr;
        if (offset >= len2) {
            return;
        }
        if (this._characterEscapes != null) {
            _writeCustomStringSegment2(cbuf, offset, len2);
        } else if (this._maximumNonEscapedChar == 0) {
            _writeStringSegment2(cbuf, offset, len2);
        } else {
            _writeStringSegmentASCII2(cbuf, offset, len2);
        }
    }

    private final void _writeStringSegment(String text, int offset, int len) throws IOException {
        int len2 = len + offset;
        int outputPtr = this._outputTail;
        byte[] outputBuffer = this._outputBuffer;
        int[] escCodes = this._outputEscapes;
        while (offset < len2) {
            int ch = text.charAt(offset);
            if (ch > 127 || escCodes[ch] != 0) {
                break;
            }
            int outputPtr2 = outputPtr + 1;
            outputBuffer[outputPtr] = (byte) ch;
            offset++;
            outputPtr = outputPtr2;
        }
        this._outputTail = outputPtr;
        if (offset >= len2) {
            return;
        }
        if (this._characterEscapes != null) {
            _writeCustomStringSegment2(text, offset, len2);
        } else if (this._maximumNonEscapedChar == 0) {
            _writeStringSegment2(text, offset, len2);
        } else {
            _writeStringSegmentASCII2(text, offset, len2);
        }
    }

    private final void _writeStringSegment2(char[] cbuf, int ch, int end) throws IOException {
        if (this._outputTail + ((end - ch) * 6) > this._outputEnd) {
            _flushBuffer();
        }
        int outputPtr = this._outputTail;
        byte[] outputBuffer = this._outputBuffer;
        int[] escCodes = this._outputEscapes;
        while (ch < end) {
            int offset = ch + 1;
            char ch2 = cbuf[ch];
            if (ch2 > 127) {
                if (ch2 <= 2047) {
                    int outputPtr2 = outputPtr + 1;
                    outputBuffer[outputPtr] = (byte) ((ch2 >> 6) | PsExtractor.AUDIO_STREAM);
                    outputPtr = outputPtr2 + 1;
                    outputBuffer[outputPtr2] = (byte) ((ch2 & '?') | 128);
                } else {
                    outputPtr = _outputMultiByteChar(ch2, outputPtr);
                }
                ch = offset;
            } else if (escCodes[ch2] == 0) {
                int outputPtr3 = outputPtr + 1;
                outputBuffer[outputPtr] = (byte) ch2;
                ch = offset;
                outputPtr = outputPtr3;
            } else {
                int outputPtr4 = escCodes[ch2];
                if (outputPtr4 > 0) {
                    int outputPtr5 = outputPtr + 1;
                    outputBuffer[outputPtr] = BYTE_BACKSLASH;
                    outputPtr = outputPtr5 + 1;
                    outputBuffer[outputPtr5] = (byte) outputPtr4;
                    ch = offset;
                } else {
                    outputPtr = _writeGenericEscape(ch2, outputPtr);
                    ch = offset;
                }
            }
        }
        this._outputTail = outputPtr;
    }

    private final void _writeStringSegment2(String text, int ch, int end) throws IOException {
        if (this._outputTail + ((end - ch) * 6) > this._outputEnd) {
            _flushBuffer();
        }
        int outputPtr = this._outputTail;
        byte[] outputBuffer = this._outputBuffer;
        int[] escCodes = this._outputEscapes;
        while (ch < end) {
            int offset = ch + 1;
            int ch2 = text.charAt(ch);
            if (ch2 > 127) {
                if (ch2 <= 2047) {
                    int outputPtr2 = outputPtr + 1;
                    outputBuffer[outputPtr] = (byte) ((ch2 >> 6) | PsExtractor.AUDIO_STREAM);
                    outputPtr = outputPtr2 + 1;
                    outputBuffer[outputPtr2] = (byte) ((ch2 & 63) | 128);
                } else {
                    outputPtr = _outputMultiByteChar(ch2, outputPtr);
                }
                ch = offset;
            } else if (escCodes[ch2] == 0) {
                int outputPtr3 = outputPtr + 1;
                outputBuffer[outputPtr] = (byte) ch2;
                ch = offset;
                outputPtr = outputPtr3;
            } else {
                int outputPtr4 = escCodes[ch2];
                if (outputPtr4 > 0) {
                    int outputPtr5 = outputPtr + 1;
                    outputBuffer[outputPtr] = BYTE_BACKSLASH;
                    outputPtr = outputPtr5 + 1;
                    outputBuffer[outputPtr5] = (byte) outputPtr4;
                    ch = offset;
                } else {
                    outputPtr = _writeGenericEscape(ch2, outputPtr);
                    ch = offset;
                }
            }
        }
        this._outputTail = outputPtr;
    }

    private final void _writeStringSegmentASCII2(char[] cbuf, int ch, int end) throws IOException {
        if (this._outputTail + ((end - ch) * 6) > this._outputEnd) {
            _flushBuffer();
        }
        int outputPtr = this._outputTail;
        byte[] outputBuffer = this._outputBuffer;
        int[] escCodes = this._outputEscapes;
        int maxUnescaped = this._maximumNonEscapedChar;
        while (ch < end) {
            int offset = ch + 1;
            char ch2 = cbuf[ch];
            if (ch2 <= 127) {
                if (escCodes[ch2] == 0) {
                    int outputPtr2 = outputPtr + 1;
                    outputBuffer[outputPtr] = (byte) ch2;
                    ch = offset;
                    outputPtr = outputPtr2;
                } else {
                    int outputPtr3 = escCodes[ch2];
                    if (outputPtr3 > 0) {
                        int outputPtr4 = outputPtr + 1;
                        outputBuffer[outputPtr] = BYTE_BACKSLASH;
                        outputPtr = outputPtr4 + 1;
                        outputBuffer[outputPtr4] = (byte) outputPtr3;
                        ch = offset;
                    } else {
                        outputPtr = _writeGenericEscape(ch2, outputPtr);
                        ch = offset;
                    }
                }
            } else if (ch2 > maxUnescaped) {
                outputPtr = _writeGenericEscape(ch2, outputPtr);
                ch = offset;
            } else {
                if (ch2 <= 2047) {
                    int outputPtr5 = outputPtr + 1;
                    outputBuffer[outputPtr] = (byte) ((ch2 >> 6) | PsExtractor.AUDIO_STREAM);
                    outputPtr = outputPtr5 + 1;
                    outputBuffer[outputPtr5] = (byte) ((ch2 & '?') | 128);
                } else {
                    outputPtr = _outputMultiByteChar(ch2, outputPtr);
                }
                ch = offset;
            }
        }
        this._outputTail = outputPtr;
    }

    private final void _writeStringSegmentASCII2(String text, int ch, int end) throws IOException {
        if (this._outputTail + ((end - ch) * 6) > this._outputEnd) {
            _flushBuffer();
        }
        int outputPtr = this._outputTail;
        byte[] outputBuffer = this._outputBuffer;
        int[] escCodes = this._outputEscapes;
        int maxUnescaped = this._maximumNonEscapedChar;
        while (ch < end) {
            int offset = ch + 1;
            int ch2 = text.charAt(ch);
            if (ch2 <= 127) {
                if (escCodes[ch2] == 0) {
                    int outputPtr2 = outputPtr + 1;
                    outputBuffer[outputPtr] = (byte) ch2;
                    ch = offset;
                    outputPtr = outputPtr2;
                } else {
                    int outputPtr3 = escCodes[ch2];
                    if (outputPtr3 > 0) {
                        int outputPtr4 = outputPtr + 1;
                        outputBuffer[outputPtr] = BYTE_BACKSLASH;
                        outputPtr = outputPtr4 + 1;
                        outputBuffer[outputPtr4] = (byte) outputPtr3;
                        ch = offset;
                    } else {
                        outputPtr = _writeGenericEscape(ch2, outputPtr);
                        ch = offset;
                    }
                }
            } else if (ch2 > maxUnescaped) {
                outputPtr = _writeGenericEscape(ch2, outputPtr);
                ch = offset;
            } else {
                if (ch2 <= 2047) {
                    int outputPtr5 = outputPtr + 1;
                    outputBuffer[outputPtr] = (byte) ((ch2 >> 6) | PsExtractor.AUDIO_STREAM);
                    outputPtr = outputPtr5 + 1;
                    outputBuffer[outputPtr5] = (byte) ((ch2 & 63) | 128);
                } else {
                    outputPtr = _outputMultiByteChar(ch2, outputPtr);
                }
                ch = offset;
            }
        }
        this._outputTail = outputPtr;
    }

    private final void _writeCustomStringSegment2(char[] cbuf, int ch, int end) throws IOException {
        if (this._outputTail + ((end - ch) * 6) > this._outputEnd) {
            _flushBuffer();
        }
        int outputPtr = this._outputTail;
        byte[] outputBuffer = this._outputBuffer;
        int[] escCodes = this._outputEscapes;
        int maxUnescaped = this._maximumNonEscapedChar <= 0 ? SupportMenu.USER_MASK : this._maximumNonEscapedChar;
        CharacterEscapes customEscapes = this._characterEscapes;
        while (ch < end) {
            int offset = ch + 1;
            char ch2 = cbuf[ch];
            if (ch2 <= 127) {
                if (escCodes[ch2] == 0) {
                    int outputPtr2 = outputPtr + 1;
                    outputBuffer[outputPtr] = (byte) ch2;
                    ch = offset;
                    outputPtr = outputPtr2;
                } else {
                    int outputPtr3 = escCodes[ch2];
                    if (outputPtr3 > 0) {
                        int outputPtr4 = outputPtr + 1;
                        outputBuffer[outputPtr] = BYTE_BACKSLASH;
                        outputPtr = outputPtr4 + 1;
                        outputBuffer[outputPtr4] = (byte) outputPtr3;
                        ch = offset;
                    } else if (outputPtr3 == -2) {
                        SerializableString esc = customEscapes.getEscapeSequence(ch2);
                        if (esc == null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Invalid custom escape definitions; custom escape not found for character code 0x");
                            sb.append(Integer.toHexString(ch2));
                            sb.append(", although was supposed to have one");
                            _reportError(sb.toString());
                        }
                        outputPtr = _writeCustomEscape(outputBuffer, outputPtr, esc, end - offset);
                        ch = offset;
                    } else {
                        outputPtr = _writeGenericEscape(ch2, outputPtr);
                        ch = offset;
                    }
                }
            } else if (ch2 > maxUnescaped) {
                outputPtr = _writeGenericEscape(ch2, outputPtr);
                ch = offset;
            } else {
                SerializableString esc2 = customEscapes.getEscapeSequence(ch2);
                if (esc2 != null) {
                    outputPtr = _writeCustomEscape(outputBuffer, outputPtr, esc2, end - offset);
                    ch = offset;
                } else {
                    if (ch2 <= 2047) {
                        int outputPtr5 = outputPtr + 1;
                        outputBuffer[outputPtr] = (byte) ((ch2 >> 6) | PsExtractor.AUDIO_STREAM);
                        outputPtr = outputPtr5 + 1;
                        outputBuffer[outputPtr5] = (byte) ((ch2 & '?') | 128);
                    } else {
                        outputPtr = _outputMultiByteChar(ch2, outputPtr);
                    }
                    ch = offset;
                }
            }
        }
        this._outputTail = outputPtr;
    }

    private final void _writeCustomStringSegment2(String text, int ch, int end) throws IOException {
        if (this._outputTail + ((end - ch) * 6) > this._outputEnd) {
            _flushBuffer();
        }
        int outputPtr = this._outputTail;
        byte[] outputBuffer = this._outputBuffer;
        int[] escCodes = this._outputEscapes;
        int maxUnescaped = this._maximumNonEscapedChar <= 0 ? SupportMenu.USER_MASK : this._maximumNonEscapedChar;
        CharacterEscapes customEscapes = this._characterEscapes;
        while (ch < end) {
            int offset = ch + 1;
            int ch2 = text.charAt(ch);
            if (ch2 <= 127) {
                if (escCodes[ch2] == 0) {
                    int outputPtr2 = outputPtr + 1;
                    outputBuffer[outputPtr] = (byte) ch2;
                    ch = offset;
                    outputPtr = outputPtr2;
                } else {
                    int outputPtr3 = escCodes[ch2];
                    if (outputPtr3 > 0) {
                        int outputPtr4 = outputPtr + 1;
                        outputBuffer[outputPtr] = BYTE_BACKSLASH;
                        outputPtr = outputPtr4 + 1;
                        outputBuffer[outputPtr4] = (byte) outputPtr3;
                        ch = offset;
                    } else if (outputPtr3 == -2) {
                        SerializableString esc = customEscapes.getEscapeSequence(ch2);
                        if (esc == null) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Invalid custom escape definitions; custom escape not found for character code 0x");
                            sb.append(Integer.toHexString(ch2));
                            sb.append(", although was supposed to have one");
                            _reportError(sb.toString());
                        }
                        outputPtr = _writeCustomEscape(outputBuffer, outputPtr, esc, end - offset);
                        ch = offset;
                    } else {
                        outputPtr = _writeGenericEscape(ch2, outputPtr);
                        ch = offset;
                    }
                }
            } else if (ch2 > maxUnescaped) {
                outputPtr = _writeGenericEscape(ch2, outputPtr);
                ch = offset;
            } else {
                SerializableString esc2 = customEscapes.getEscapeSequence(ch2);
                if (esc2 != null) {
                    outputPtr = _writeCustomEscape(outputBuffer, outputPtr, esc2, end - offset);
                    ch = offset;
                } else {
                    if (ch2 <= 2047) {
                        int outputPtr5 = outputPtr + 1;
                        outputBuffer[outputPtr] = (byte) ((ch2 >> 6) | PsExtractor.AUDIO_STREAM);
                        outputPtr = outputPtr5 + 1;
                        outputBuffer[outputPtr5] = (byte) ((ch2 & 63) | 128);
                    } else {
                        outputPtr = _outputMultiByteChar(ch2, outputPtr);
                    }
                    ch = offset;
                }
            }
        }
        this._outputTail = outputPtr;
    }

    private final int _writeCustomEscape(byte[] outputBuffer, int outputPtr, SerializableString esc, int remainingChars) throws IOException, JsonGenerationException {
        byte[] raw = esc.asUnquotedUTF8();
        int len = raw.length;
        if (len > 6) {
            return _handleLongCustomEscape(outputBuffer, outputPtr, this._outputEnd, raw, remainingChars);
        }
        System.arraycopy(raw, 0, outputBuffer, outputPtr, len);
        return outputPtr + len;
    }

    private final int _handleLongCustomEscape(byte[] outputBuffer, int outputPtr, int outputEnd, byte[] raw, int remainingChars) throws IOException, JsonGenerationException {
        int len = raw.length;
        if (outputPtr + len > outputEnd) {
            this._outputTail = outputPtr;
            _flushBuffer();
            int outputPtr2 = this._outputTail;
            if (len > outputBuffer.length) {
                this._outputStream.write(raw, 0, len);
                return outputPtr2;
            }
            System.arraycopy(raw, 0, outputBuffer, outputPtr2, len);
            outputPtr = outputPtr2 + len;
        }
        if ((remainingChars * 6) + outputPtr <= outputEnd) {
            return outputPtr;
        }
        _flushBuffer();
        return this._outputTail;
    }

    private final void _writeUTF8Segments(byte[] utf8, int offset, int totalLen) throws IOException, JsonGenerationException {
        do {
            int len = Math.min(this._outputMaxContiguous, totalLen);
            _writeUTF8Segment(utf8, offset, len);
            offset += len;
            totalLen -= len;
        } while (totalLen > 0);
    }

    private final void _writeUTF8Segment(byte[] utf8, int offset, int len) throws IOException, JsonGenerationException {
        int[] escCodes = this._outputEscapes;
        int ch = offset;
        int end = offset + len;
        while (ch < end) {
            int ptr = ch + 1;
            byte ptr2 = utf8[ch];
            if (ptr2 < 0 || escCodes[ptr2] == 0) {
                ch = ptr;
            } else {
                _writeUTF8Segment2(utf8, offset, len);
                return;
            }
        }
        if (this._outputTail + len > this._outputEnd) {
            _flushBuffer();
        }
        System.arraycopy(utf8, offset, this._outputBuffer, this._outputTail, len);
        this._outputTail += len;
    }

    private final void _writeUTF8Segment2(byte[] utf8, int offset, int len) throws IOException, JsonGenerationException {
        int outputPtr;
        int outputPtr2 = this._outputTail;
        if ((len * 6) + outputPtr2 > this._outputEnd) {
            _flushBuffer();
            outputPtr2 = this._outputTail;
        }
        byte[] outputBuffer = this._outputBuffer;
        int[] escCodes = this._outputEscapes;
        int len2 = len + offset;
        while (offset < len2) {
            int offset2 = offset + 1;
            byte b = utf8[offset];
            byte b2 = b;
            if (b2 < 0 || escCodes[b2] == 0) {
                int outputPtr3 = outputPtr + 1;
                outputBuffer[outputPtr] = b;
                offset = offset2;
                outputPtr = outputPtr3;
            } else {
                int escape = escCodes[b2];
                if (escape > 0) {
                    int outputPtr4 = outputPtr + 1;
                    outputBuffer[outputPtr] = BYTE_BACKSLASH;
                    outputPtr = outputPtr4 + 1;
                    outputBuffer[outputPtr4] = (byte) escape;
                } else {
                    outputPtr = _writeGenericEscape(b2, outputPtr);
                }
                offset = offset2;
            }
        }
        this._outputTail = outputPtr;
    }

    /* access modifiers changed from: protected */
    public final void _writeBinary(Base64Variant b64variant, byte[] input, int b24, int inputEnd) throws IOException, JsonGenerationException {
        int inputPtr;
        int safeInputEnd = inputEnd - 3;
        int safeOutputEnd = this._outputEnd - 6;
        int chunksBeforeLF = b64variant.getMaxLineLength() >> 2;
        while (b24 <= safeInputEnd) {
            if (this._outputTail > safeOutputEnd) {
                _flushBuffer();
            }
            int inputPtr2 = b24 + 1;
            int inputPtr3 = inputPtr2 + 1;
            int inputPtr4 = inputPtr3 + 1;
            this._outputTail = b64variant.encodeBase64Chunk((((input[b24] << 8) | (input[inputPtr2] & 255)) << 8) | (input[inputPtr3] & 255), this._outputBuffer, this._outputTail);
            chunksBeforeLF--;
            if (chunksBeforeLF <= 0) {
                byte[] bArr = this._outputBuffer;
                int i = this._outputTail;
                this._outputTail = i + 1;
                bArr[i] = BYTE_BACKSLASH;
                int i2 = this._outputTail;
                this._outputTail = i2 + 1;
                bArr[i2] = 110;
                chunksBeforeLF = b64variant.getMaxLineLength() >> 2;
            }
            b24 = inputPtr4;
        }
        int inputLeft = inputEnd - b24;
        if (inputLeft > 0) {
            if (this._outputTail > safeOutputEnd) {
                _flushBuffer();
            }
            int inputPtr5 = b24 + 1;
            int b242 = input[b24] << 16;
            if (inputLeft == 2) {
                inputPtr = inputPtr5 + 1;
                b242 |= (input[inputPtr5] & 255) << 8;
            } else {
                inputPtr = inputPtr5;
            }
            this._outputTail = b64variant.encodeBase64Partial(b242, inputLeft, this._outputBuffer, this._outputTail);
            int b243 = inputPtr;
        }
    }

    /* access modifiers changed from: protected */
    public final int _writeBinary(Base64Variant b64variant, InputStream data, byte[] readBuffer, int bytesLeft) throws IOException, JsonGenerationException {
        int amount;
        Base64Variant base64Variant = b64variant;
        int safeOutputEnd = this._outputEnd - 6;
        int bytesLeft2 = bytesLeft;
        int inputPtr = 0;
        int inputEnd = 0;
        int lastFullOffset = -3;
        int chunksBeforeLF = b64variant.getMaxLineLength() >> 2;
        while (bytesLeft2 > 2) {
            if (inputPtr > lastFullOffset) {
                inputEnd = _readMore(data, readBuffer, inputPtr, inputEnd, bytesLeft2);
                inputPtr = 0;
                if (inputEnd < 3) {
                    break;
                }
                lastFullOffset = inputEnd - 3;
            }
            if (this._outputTail > safeOutputEnd) {
                _flushBuffer();
            }
            int inputPtr2 = inputPtr + 1;
            int b24 = readBuffer[inputPtr] << 8;
            int inputPtr3 = inputPtr2 + 1;
            inputPtr = inputPtr3 + 1;
            bytesLeft2 -= 3;
            this._outputTail = base64Variant.encodeBase64Chunk((((readBuffer[inputPtr2] & 255) | b24) << 8) | (readBuffer[inputPtr3] & 255), this._outputBuffer, this._outputTail);
            chunksBeforeLF--;
            if (chunksBeforeLF <= 0) {
                byte[] bArr = this._outputBuffer;
                int i = this._outputTail;
                this._outputTail = i + 1;
                bArr[i] = BYTE_BACKSLASH;
                int i2 = this._outputTail;
                this._outputTail = i2 + 1;
                bArr[i2] = 110;
                chunksBeforeLF = b64variant.getMaxLineLength() >> 2;
            }
        }
        if (bytesLeft2 <= 0) {
            return bytesLeft2;
        }
        int inputEnd2 = _readMore(data, readBuffer, inputPtr, inputEnd, bytesLeft2);
        if (inputEnd2 <= 0) {
            return bytesLeft2;
        }
        if (this._outputTail > safeOutputEnd) {
            _flushBuffer();
        }
        int inputPtr4 = 0 + 1;
        int b242 = readBuffer[0] << 16;
        if (inputPtr4 < inputEnd2) {
            b242 |= (readBuffer[inputPtr4] & 255) << 8;
            amount = 2;
        } else {
            amount = 1;
        }
        this._outputTail = base64Variant.encodeBase64Partial(b242, amount, this._outputBuffer, this._outputTail);
        int i3 = inputPtr4;
        return bytesLeft2 - amount;
    }

    /* access modifiers changed from: protected */
    public final int _writeBinary(Base64Variant b64variant, InputStream data, byte[] readBuffer) throws IOException, JsonGenerationException {
        int inputPtr = 0;
        int inputEnd = 0;
        int lastFullOffset = -3;
        int bytesDone = 0;
        int safeOutputEnd = this._outputEnd - 6;
        int chunksBeforeLF = b64variant.getMaxLineLength() >> 2;
        while (true) {
            if (inputPtr > lastFullOffset) {
                inputEnd = _readMore(data, readBuffer, inputPtr, inputEnd, readBuffer.length);
                inputPtr = 0;
                if (inputEnd < 3) {
                    break;
                }
                lastFullOffset = inputEnd - 3;
            }
            if (this._outputTail > safeOutputEnd) {
                _flushBuffer();
            }
            int inputPtr2 = inputPtr + 1;
            int inputPtr3 = inputPtr2 + 1;
            int inputPtr4 = inputPtr3 + 1;
            bytesDone += 3;
            this._outputTail = b64variant.encodeBase64Chunk((((readBuffer[inputPtr] << 8) | (readBuffer[inputPtr2] & 255)) << 8) | (readBuffer[inputPtr3] & 255), this._outputBuffer, this._outputTail);
            chunksBeforeLF--;
            if (chunksBeforeLF <= 0) {
                byte[] bArr = this._outputBuffer;
                int i = this._outputTail;
                this._outputTail = i + 1;
                bArr[i] = BYTE_BACKSLASH;
                int i2 = this._outputTail;
                this._outputTail = i2 + 1;
                bArr[i2] = 110;
                chunksBeforeLF = b64variant.getMaxLineLength() >> 2;
            }
            inputPtr = inputPtr4;
        }
        if (0 >= inputEnd) {
            return bytesDone;
        }
        if (this._outputTail > safeOutputEnd) {
            _flushBuffer();
        }
        int inputPtr5 = 0 + 1;
        int b24 = readBuffer[0] << 16;
        int amount = 1;
        if (inputPtr5 < inputEnd) {
            b24 |= (readBuffer[inputPtr5] & 255) << 8;
            amount = 2;
        }
        int bytesDone2 = bytesDone + amount;
        this._outputTail = b64variant.encodeBase64Partial(b24, amount, this._outputBuffer, this._outputTail);
        int b242 = inputPtr5;
        return bytesDone2;
    }

    private final int _readMore(InputStream in, byte[] readBuffer, int inputPtr, int inputEnd, int maxRead) throws IOException {
        int i = 0;
        while (inputPtr < inputEnd) {
            int i2 = i + 1;
            int inputPtr2 = inputPtr + 1;
            readBuffer[i] = readBuffer[inputPtr];
            i = i2;
            inputPtr = inputPtr2;
        }
        int inputEnd2 = i;
        int maxRead2 = Math.min(maxRead, readBuffer.length);
        do {
            int length = maxRead2 - inputEnd2;
            if (length == 0) {
                break;
            }
            int count = in.read(readBuffer, inputEnd2, length);
            if (count < 0) {
                return inputEnd2;
            }
            inputEnd2 += count;
        } while (inputEnd2 < 3);
        return inputEnd2;
    }

    private final int _outputRawMultiByteChar(int ch, char[] cbuf, int inputOffset, int inputEnd) throws IOException {
        if (ch < 55296 || ch > 57343) {
            byte[] bbuf = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            bbuf[i] = (byte) ((ch >> 12) | 224);
            int i2 = this._outputTail;
            this._outputTail = i2 + 1;
            bbuf[i2] = (byte) (((ch >> 6) & 63) | 128);
            int i3 = this._outputTail;
            this._outputTail = i3 + 1;
            bbuf[i3] = (byte) ((ch & 63) | 128);
            return inputOffset;
        }
        if (inputOffset >= inputEnd || cbuf == null) {
            _reportError(String.format("Split surrogate on writeRaw() input (last character): first character 0x%4x", new Object[]{Integer.valueOf(ch)}));
        }
        _outputSurrogates(ch, cbuf[inputOffset]);
        return inputOffset + 1;
    }

    /* access modifiers changed from: protected */
    public final void _outputSurrogates(int surr1, int surr2) throws IOException {
        int c = _decodeSurrogate(surr1, surr2);
        if (this._outputTail + 4 > this._outputEnd) {
            _flushBuffer();
        }
        byte[] bbuf = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        bbuf[i] = (byte) ((c >> 18) | PsExtractor.VIDEO_STREAM_MASK);
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        bbuf[i2] = (byte) (((c >> 12) & 63) | 128);
        int i3 = this._outputTail;
        this._outputTail = i3 + 1;
        bbuf[i3] = (byte) (((c >> 6) & 63) | 128);
        int i4 = this._outputTail;
        this._outputTail = i4 + 1;
        bbuf[i4] = (byte) ((c & 63) | 128);
    }

    private final int _outputMultiByteChar(int ch, int outputPtr) throws IOException {
        byte[] bbuf = this._outputBuffer;
        if (ch < 55296 || ch > 57343) {
            int outputPtr2 = outputPtr + 1;
            bbuf[outputPtr] = (byte) ((ch >> 12) | 224);
            int outputPtr3 = outputPtr2 + 1;
            bbuf[outputPtr2] = (byte) (((ch >> 6) & 63) | 128);
            int outputPtr4 = outputPtr3 + 1;
            bbuf[outputPtr3] = (byte) ((ch & 63) | 128);
            return outputPtr4;
        }
        int outputPtr5 = outputPtr + 1;
        bbuf[outputPtr] = BYTE_BACKSLASH;
        int outputPtr6 = outputPtr5 + 1;
        bbuf[outputPtr5] = BYTE_u;
        int outputPtr7 = outputPtr6 + 1;
        byte[] bArr = HEX_CHARS;
        bbuf[outputPtr6] = bArr[(ch >> 12) & 15];
        int outputPtr8 = outputPtr7 + 1;
        bbuf[outputPtr7] = bArr[(ch >> 8) & 15];
        int outputPtr9 = outputPtr8 + 1;
        bbuf[outputPtr8] = bArr[(ch >> 4) & 15];
        int outputPtr10 = outputPtr9 + 1;
        bbuf[outputPtr9] = bArr[ch & 15];
        return outputPtr10;
    }

    private final void _writeNull() throws IOException {
        if (this._outputTail + 4 >= this._outputEnd) {
            _flushBuffer();
        }
        System.arraycopy(NULL_BYTES, 0, this._outputBuffer, this._outputTail, 4);
        this._outputTail += 4;
    }

    private int _writeGenericEscape(int charToEscape, int outputPtr) throws IOException {
        int outputPtr2;
        byte[] bbuf = this._outputBuffer;
        int outputPtr3 = outputPtr + 1;
        bbuf[outputPtr] = BYTE_BACKSLASH;
        int outputPtr4 = outputPtr3 + 1;
        bbuf[outputPtr3] = BYTE_u;
        if (charToEscape > 255) {
            int hi = 255 & (charToEscape >> 8);
            int outputPtr5 = outputPtr4 + 1;
            byte[] bArr = HEX_CHARS;
            bbuf[outputPtr4] = bArr[hi >> 4];
            outputPtr2 = outputPtr5 + 1;
            bbuf[outputPtr5] = bArr[hi & 15];
            charToEscape &= 255;
        } else {
            int outputPtr6 = outputPtr4 + 1;
            bbuf[outputPtr4] = BYTE_0;
            outputPtr2 = outputPtr6 + 1;
            bbuf[outputPtr6] = BYTE_0;
        }
        int outputPtr7 = outputPtr2 + 1;
        byte[] bArr2 = HEX_CHARS;
        bbuf[outputPtr2] = bArr2[charToEscape >> 4];
        int outputPtr8 = outputPtr7 + 1;
        bbuf[outputPtr7] = bArr2[charToEscape & 15];
        return outputPtr8;
    }

    /* access modifiers changed from: protected */
    public final void _flushBuffer() throws IOException {
        int len = this._outputTail;
        if (len > 0) {
            this._outputTail = 0;
            this._outputStream.write(this._outputBuffer, 0, len);
        }
    }
}
