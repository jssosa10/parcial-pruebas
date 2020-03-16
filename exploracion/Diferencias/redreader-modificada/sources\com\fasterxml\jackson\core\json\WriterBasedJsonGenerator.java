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
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

public class WriterBasedJsonGenerator extends JsonGeneratorImpl {
    protected static final char[] HEX_CHARS = CharTypes.copyHexChars();
    protected static final int SHORT_WRITE = 32;
    protected char[] _charBuffer;
    protected SerializableString _currentEscape;
    protected char[] _entityBuffer;
    protected char[] _outputBuffer;
    protected int _outputEnd;
    protected int _outputHead;
    protected int _outputTail;
    protected char _quoteChar = '\"';
    protected final Writer _writer;

    public WriterBasedJsonGenerator(IOContext ctxt, int features, ObjectCodec codec, Writer w) {
        super(ctxt, features, codec);
        this._writer = w;
        this._outputBuffer = ctxt.allocConcatBuffer();
        this._outputEnd = this._outputBuffer.length;
    }

    public Object getOutputTarget() {
        return this._writer;
    }

    public int getOutputBuffered() {
        return Math.max(0, this._outputTail - this._outputHead);
    }

    public boolean canWriteFormattedNumbers() {
        return true;
    }

    public void writeFieldName(String name) throws IOException {
        int status = this._writeContext.writeFieldName(name);
        if (status == 4) {
            _reportError("Can not write a field name, expecting a value");
        }
        boolean z = true;
        if (status != 1) {
            z = false;
        }
        _writeFieldName(name, z);
    }

    public void writeFieldName(SerializableString name) throws IOException {
        int status = this._writeContext.writeFieldName(name.getValue());
        if (status == 4) {
            _reportError("Can not write a field name, expecting a value");
        }
        boolean z = true;
        if (status != 1) {
            z = false;
        }
        _writeFieldName(name, z);
    }

    /* access modifiers changed from: protected */
    public final void _writeFieldName(String name, boolean commaBefore) throws IOException {
        if (this._cfgPrettyPrinter != null) {
            _writePPFieldName(name, commaBefore);
            return;
        }
        if (this._outputTail + 1 >= this._outputEnd) {
            _flushBuffer();
        }
        if (commaBefore) {
            char[] cArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            cArr[i] = ',';
        }
        if (this._cfgUnqNames) {
            _writeString(name);
            return;
        }
        char[] cArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr2[i2] = this._quoteChar;
        _writeString(name);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr3 = this._outputBuffer;
        int i3 = this._outputTail;
        this._outputTail = i3 + 1;
        cArr3[i3] = this._quoteChar;
    }

    /* access modifiers changed from: protected */
    public final void _writeFieldName(SerializableString name, boolean commaBefore) throws IOException {
        if (this._cfgPrettyPrinter != null) {
            _writePPFieldName(name, commaBefore);
            return;
        }
        if (this._outputTail + 1 >= this._outputEnd) {
            _flushBuffer();
        }
        if (commaBefore) {
            char[] cArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            cArr[i] = ',';
        }
        char[] quoted = name.asQuotedChars();
        if (this._cfgUnqNames) {
            writeRaw(quoted, 0, quoted.length);
            return;
        }
        char[] cArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr2[i2] = this._quoteChar;
        int qlen = quoted.length;
        int i3 = this._outputTail;
        if (i3 + qlen + 1 >= this._outputEnd) {
            writeRaw(quoted, 0, qlen);
            if (this._outputTail >= this._outputEnd) {
                _flushBuffer();
            }
            char[] cArr3 = this._outputBuffer;
            int i4 = this._outputTail;
            this._outputTail = i4 + 1;
            cArr3[i4] = this._quoteChar;
        } else {
            System.arraycopy(quoted, 0, cArr2, i3, qlen);
            this._outputTail += qlen;
            char[] cArr4 = this._outputBuffer;
            int i5 = this._outputTail;
            this._outputTail = i5 + 1;
            cArr4[i5] = this._quoteChar;
        }
    }

    public void writeStartArray() throws IOException {
        _verifyValueWrite("start an array");
        this._writeContext = this._writeContext.createChildArrayContext();
        if (this._cfgPrettyPrinter != null) {
            this._cfgPrettyPrinter.writeStartArray(this);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = '[';
    }

    public void writeEndArray() throws IOException {
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
            char[] cArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            cArr[i] = ']';
        }
        this._writeContext = this._writeContext.clearAndGetParent();
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
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = '{';
    }

    public void writeStartObject() throws IOException {
        _verifyValueWrite("start an object");
        this._writeContext = this._writeContext.createChildObjectContext();
        if (this._cfgPrettyPrinter != null) {
            this._cfgPrettyPrinter.writeStartObject(this);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = '{';
    }

    public void writeEndObject() throws IOException {
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
            char[] cArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            cArr[i] = '}';
        }
        this._writeContext = this._writeContext.clearAndGetParent();
    }

    /* access modifiers changed from: protected */
    public final void _writePPFieldName(String name, boolean commaBefore) throws IOException {
        if (commaBefore) {
            this._cfgPrettyPrinter.writeObjectEntrySeparator(this);
        } else {
            this._cfgPrettyPrinter.beforeObjectEntries(this);
        }
        if (this._cfgUnqNames) {
            _writeString(name);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        _writeString(name);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr2[i2] = this._quoteChar;
    }

    /* access modifiers changed from: protected */
    public final void _writePPFieldName(SerializableString name, boolean commaBefore) throws IOException {
        if (commaBefore) {
            this._cfgPrettyPrinter.writeObjectEntrySeparator(this);
        } else {
            this._cfgPrettyPrinter.beforeObjectEntries(this);
        }
        char[] quoted = name.asQuotedChars();
        if (this._cfgUnqNames) {
            writeRaw(quoted, 0, quoted.length);
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        writeRaw(quoted, 0, quoted.length);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr2[i2] = this._quoteChar;
    }

    public void writeString(String text) throws IOException {
        _verifyValueWrite("write a string");
        if (text == null) {
            _writeNull();
            return;
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        _writeString(text);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr2[i2] = this._quoteChar;
    }

    public void writeString(Reader reader, int len) throws IOException {
        _verifyValueWrite("write a string");
        if (reader == null) {
            _reportError("null reader");
        }
        int toRead = len >= 0 ? len : Integer.MAX_VALUE;
        char[] buf = _allocateCopyBuffer();
        if (this._outputTail + len >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        while (toRead > 0) {
            int numRead = reader.read(buf, 0, Math.min(toRead, buf.length));
            if (numRead <= 0) {
                break;
            }
            if (this._outputTail + len >= this._outputEnd) {
                _flushBuffer();
            }
            _writeString(buf, 0, numRead);
            toRead -= numRead;
        }
        if (this._outputTail + len >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr2[i2] = this._quoteChar;
        if (toRead > 0 && len >= 0) {
            _reportError("Didn't read enough from reader");
        }
    }

    public void writeString(char[] text, int offset, int len) throws IOException {
        _verifyValueWrite("write a string");
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        _writeString(text, offset, len);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr2[i2] = this._quoteChar;
    }

    public void writeString(SerializableString sstr) throws IOException {
        _verifyValueWrite("write a string");
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        char[] text = sstr.asQuotedChars();
        int len = text.length;
        if (len < 32) {
            if (len > this._outputEnd - this._outputTail) {
                _flushBuffer();
            }
            System.arraycopy(text, 0, this._outputBuffer, this._outputTail, len);
            this._outputTail += len;
        } else {
            _flushBuffer();
            this._writer.write(text, 0, len);
        }
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr2[i2] = this._quoteChar;
    }

    public void writeRawUTF8String(byte[] text, int offset, int length) throws IOException {
        _reportUnsupportedOperation();
    }

    public void writeUTF8String(byte[] text, int offset, int length) throws IOException {
        _reportUnsupportedOperation();
    }

    public void writeRaw(String text) throws IOException {
        int len = text.length();
        int room = this._outputEnd - this._outputTail;
        if (room == 0) {
            _flushBuffer();
            room = this._outputEnd - this._outputTail;
        }
        if (room >= len) {
            text.getChars(0, len, this._outputBuffer, this._outputTail);
            this._outputTail += len;
            return;
        }
        writeRawLong(text);
    }

    public void writeRaw(String text, int start, int len) throws IOException {
        int room = this._outputEnd - this._outputTail;
        if (room < len) {
            _flushBuffer();
            room = this._outputEnd - this._outputTail;
        }
        if (room >= len) {
            text.getChars(start, start + len, this._outputBuffer, this._outputTail);
            this._outputTail += len;
            return;
        }
        writeRawLong(text.substring(start, start + len));
    }

    public void writeRaw(SerializableString text) throws IOException {
        writeRaw(text.getValue());
    }

    public void writeRaw(char[] text, int offset, int len) throws IOException {
        if (len < 32) {
            if (len > this._outputEnd - this._outputTail) {
                _flushBuffer();
            }
            System.arraycopy(text, offset, this._outputBuffer, this._outputTail, len);
            this._outputTail += len;
            return;
        }
        _flushBuffer();
        this._writer.write(text, offset, len);
    }

    public void writeRaw(char c) throws IOException {
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = c;
    }

    private void writeRawLong(String text) throws IOException {
        int i = this._outputEnd;
        int i2 = this._outputTail;
        int room = i - i2;
        text.getChars(0, room, this._outputBuffer, i2);
        this._outputTail += room;
        _flushBuffer();
        int offset = room;
        int len = text.length() - room;
        while (len > this._outputEnd) {
            int amount = this._outputEnd;
            text.getChars(offset, offset + amount, this._outputBuffer, 0);
            this._outputHead = 0;
            this._outputTail = amount;
            _flushBuffer();
            offset += amount;
            len -= amount;
        }
        text.getChars(offset, offset + len, this._outputBuffer, 0);
        this._outputHead = 0;
        this._outputTail = len;
    }

    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException, JsonGenerationException {
        _verifyValueWrite("write a binary value");
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        _writeBinary(b64variant, data, offset, offset + len);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr2[i2] = this._quoteChar;
    }

    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws IOException, JsonGenerationException {
        int missing;
        _verifyValueWrite("write a binary value");
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
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
        char[] cArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr2[i2] = this._quoteChar;
        return missing;
    }

    public void writeNumber(short s) throws IOException {
        _verifyValueWrite("write a number");
        if (this._cfgNumbersAsStrings) {
            _writeQuotedShort(s);
            return;
        }
        if (this._outputTail + 6 >= this._outputEnd) {
            _flushBuffer();
        }
        this._outputTail = NumberOutput.outputInt((int) s, this._outputBuffer, this._outputTail);
    }

    private void _writeQuotedShort(short s) throws IOException {
        if (this._outputTail + 8 >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        this._outputTail = NumberOutput.outputInt((int) s, cArr, this._outputTail);
        char[] cArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr2[i2] = this._quoteChar;
    }

    public void writeNumber(int i) throws IOException {
        _verifyValueWrite("write a number");
        if (this._cfgNumbersAsStrings) {
            _writeQuotedInt(i);
            return;
        }
        if (this._outputTail + 11 >= this._outputEnd) {
            _flushBuffer();
        }
        this._outputTail = NumberOutput.outputInt(i, this._outputBuffer, this._outputTail);
    }

    private void _writeQuotedInt(int i) throws IOException {
        if (this._outputTail + 13 >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr[i2] = this._quoteChar;
        this._outputTail = NumberOutput.outputInt(i, cArr, this._outputTail);
        char[] cArr2 = this._outputBuffer;
        int i3 = this._outputTail;
        this._outputTail = i3 + 1;
        cArr2[i3] = this._quoteChar;
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

    private void _writeQuotedLong(long l) throws IOException {
        if (this._outputTail + 23 >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        this._outputTail = NumberOutput.outputLong(l, cArr, this._outputTail);
        char[] cArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr2[i2] = this._quoteChar;
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
        if (this._cfgNumbersAsStrings || (isEnabled(Feature.QUOTE_NON_NUMERIC_NUMBERS) && (Double.isNaN(d) || Double.isInfinite(d)))) {
            writeString(String.valueOf(d));
            return;
        }
        _verifyValueWrite("write a number");
        writeRaw(String.valueOf(d));
    }

    public void writeNumber(float f) throws IOException {
        if (this._cfgNumbersAsStrings || (isEnabled(Feature.QUOTE_NON_NUMERIC_NUMBERS) && (Float.isNaN(f) || Float.isInfinite(f)))) {
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

    private void _writeQuotedRaw(String value) throws IOException {
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr = this._outputBuffer;
        int i = this._outputTail;
        this._outputTail = i + 1;
        cArr[i] = this._quoteChar;
        writeRaw(value);
        if (this._outputTail >= this._outputEnd) {
            _flushBuffer();
        }
        char[] cArr2 = this._outputBuffer;
        int i2 = this._outputTail;
        this._outputTail = i2 + 1;
        cArr2[i2] = this._quoteChar;
    }

    public void writeBoolean(boolean state) throws IOException {
        int ptr;
        _verifyValueWrite("write a boolean value");
        if (this._outputTail + 5 >= this._outputEnd) {
            _flushBuffer();
        }
        int ptr2 = this._outputTail;
        char[] buf = this._outputBuffer;
        if (state) {
            buf[ptr2] = 't';
            int ptr3 = ptr2 + 1;
            buf[ptr3] = 'r';
            int ptr4 = ptr3 + 1;
            buf[ptr4] = 'u';
            ptr = ptr4 + 1;
            buf[ptr] = 'e';
        } else {
            buf[ptr2] = 'f';
            int ptr5 = ptr2 + 1;
            buf[ptr5] = 'a';
            int ptr6 = ptr5 + 1;
            buf[ptr6] = 'l';
            int ptr7 = ptr6 + 1;
            buf[ptr7] = 's';
            ptr = ptr7 + 1;
            buf[ptr] = 'e';
        }
        this._outputTail = ptr + 1;
    }

    public void writeNull() throws IOException {
        _verifyValueWrite("write a null");
        _writeNull();
    }

    /* access modifiers changed from: protected */
    public final void _verifyValueWrite(String typeMsg) throws IOException {
        char c;
        int status = this._writeContext.writeValue();
        if (this._cfgPrettyPrinter != null) {
            _verifyPrettyValueWrite(typeMsg, status);
        } else if (status != 5) {
            switch (status) {
                case 1:
                    c = ',';
                    break;
                case 2:
                    c = ':';
                    break;
                case 3:
                    if (this._rootValueSeparator != null) {
                        writeRaw(this._rootValueSeparator.getValue());
                    }
                    return;
                default:
                    return;
            }
            if (this._outputTail >= this._outputEnd) {
                _flushBuffer();
            }
            char[] cArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            cArr[i] = c;
        } else {
            _reportCantWriteValueExpectName(typeMsg);
        }
    }

    public void flush() throws IOException {
        _flushBuffer();
        if (this._writer != null && isEnabled(Feature.FLUSH_PASSED_TO_STREAM)) {
            this._writer.flush();
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
        this._outputHead = 0;
        this._outputTail = 0;
        if (this._writer != null) {
            if (this._ioContext.isResourceManaged() || isEnabled(Feature.AUTO_CLOSE_TARGET)) {
                this._writer.close();
            } else if (isEnabled(Feature.FLUSH_PASSED_TO_STREAM)) {
                this._writer.flush();
            }
        }
        _releaseBuffers();
    }

    /* access modifiers changed from: protected */
    public void _releaseBuffers() {
        char[] buf = this._outputBuffer;
        if (buf != null) {
            this._outputBuffer = null;
            this._ioContext.releaseConcatBuffer(buf);
        }
        char[] buf2 = this._charBuffer;
        if (buf2 != null) {
            this._charBuffer = null;
            this._ioContext.releaseNameCopyBuffer(buf2);
        }
    }

    private void _writeString(String text) throws IOException {
        int len = text.length();
        int i = this._outputEnd;
        if (len > i) {
            _writeLongString(text);
            return;
        }
        if (this._outputTail + len > i) {
            _flushBuffer();
        }
        text.getChars(0, len, this._outputBuffer, this._outputTail);
        if (this._characterEscapes != null) {
            _writeStringCustom(len);
        } else if (this._maximumNonEscapedChar != 0) {
            _writeStringASCII(len, this._maximumNonEscapedChar);
        } else {
            _writeString2(len);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0021, code lost:
        r3 = r7._outputBuffer;
        r5 = r7._outputTail;
        r7._outputTail = r5 + 1;
        r3 = r3[r5];
        _prependOrWriteCharacterEscape(r3, r1[r3]);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:7:0x0016, code lost:
        r5 = r7._outputHead;
        r4 = r4 - r5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x001a, code lost:
        if (r4 <= 0) goto L_0x0021;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x001c, code lost:
        r7._writer.write(r3, r5, r4);
     */
    private void _writeString2(int len) throws IOException {
        int end = this._outputTail + len;
        int[] escCodes = this._outputEscapes;
        int escLen = escCodes.length;
        while (this._outputTail < end) {
            while (true) {
                char[] cArr = this._outputBuffer;
                int i = this._outputTail;
                char c = cArr[i];
                if (c < escLen && escCodes[c] != 0) {
                    break;
                }
                int i2 = this._outputTail + 1;
                this._outputTail = i2;
                if (i2 >= end) {
                    return;
                }
            }
        }
    }

    private void _writeLongString(String text) throws IOException {
        _flushBuffer();
        int textLen = text.length();
        int offset = 0;
        do {
            int max = this._outputEnd;
            int segmentLen = offset + max > textLen ? textLen - offset : max;
            text.getChars(offset, offset + segmentLen, this._outputBuffer, 0);
            if (this._characterEscapes != null) {
                _writeSegmentCustom(segmentLen);
            } else if (this._maximumNonEscapedChar != 0) {
                _writeSegmentASCII(segmentLen, this._maximumNonEscapedChar);
            } else {
                _writeSegment(segmentLen);
            }
            offset += segmentLen;
        } while (offset < textLen);
    }

    private void _writeSegment(int end) throws IOException {
        char c;
        int[] escCodes = this._outputEscapes;
        int escLen = escCodes.length;
        int ptr = 0;
        int start = 0;
        while (ptr < end) {
            do {
                c = this._outputBuffer[ptr];
                if (c < escLen && escCodes[c] != 0) {
                    break;
                }
                ptr++;
            } while (ptr < end);
            int flushLen = ptr - start;
            if (flushLen > 0) {
                this._writer.write(this._outputBuffer, start, flushLen);
                if (ptr >= end) {
                    return;
                }
            }
            ptr++;
            start = _prependOrWriteCharacterEscape(this._outputBuffer, ptr, end, c, escCodes[c]);
        }
    }

    private void _writeString(char[] text, int offset, int len) throws IOException {
        if (this._characterEscapes != null) {
            _writeStringCustom(text, offset, len);
        } else if (this._maximumNonEscapedChar != 0) {
            _writeStringASCII(text, offset, len, this._maximumNonEscapedChar);
        } else {
            int len2 = len + offset;
            int[] escCodes = this._outputEscapes;
            int escLen = escCodes.length;
            while (offset < len2) {
                int start = offset;
                do {
                    char c = text[offset];
                    if (c < escLen && escCodes[c] != 0) {
                        break;
                    }
                    offset++;
                } while (offset < len2);
                int newAmount = offset - start;
                if (newAmount < 32) {
                    if (this._outputTail + newAmount > this._outputEnd) {
                        _flushBuffer();
                    }
                    if (newAmount > 0) {
                        System.arraycopy(text, start, this._outputBuffer, this._outputTail, newAmount);
                        this._outputTail += newAmount;
                    }
                } else {
                    _flushBuffer();
                    this._writer.write(text, start, newAmount);
                }
                if (offset >= len2) {
                    break;
                }
                int offset2 = offset + 1;
                char c2 = text[offset];
                _appendCharacterEscape(c2, escCodes[c2]);
                offset = offset2;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:15:0x0042 A[SYNTHETIC] */
    private void _writeStringASCII(int len, int maxNonEscaped) throws IOException, JsonGenerationException {
        char c;
        int i;
        int escCode;
        int end = this._outputTail + len;
        int[] escCodes = this._outputEscapes;
        int escLimit = Math.min(escCodes.length, maxNonEscaped + 1);
        while (this._outputTail < end) {
            while (true) {
                c = this._outputBuffer[this._outputTail];
                if (c < escLimit) {
                    escCode = escCodes[c];
                    if (escCode != 0) {
                        break;
                    }
                    i = this._outputTail + 1;
                    this._outputTail = i;
                    if (i >= end) {
                        return;
                    }
                } else {
                    if (c > maxNonEscaped) {
                        escCode = -1;
                        break;
                    }
                    i = this._outputTail + 1;
                    this._outputTail = i;
                    if (i >= end) {
                    }
                }
            }
            int i2 = this._outputTail;
            int i3 = this._outputHead;
            int flushLen = i2 - i3;
            if (flushLen > 0) {
                this._writer.write(this._outputBuffer, i3, flushLen);
            }
            this._outputTail++;
            _prependOrWriteCharacterEscape(c, escCode);
        }
    }

    private void _writeSegmentASCII(int end, int maxNonEscaped) throws IOException, JsonGenerationException {
        char c;
        int[] escCodes = this._outputEscapes;
        int escLimit = Math.min(escCodes.length, maxNonEscaped + 1);
        int ptr = 0;
        int escCode = 0;
        int start = 0;
        while (ptr < end) {
            while (true) {
                c = this._outputBuffer[ptr];
                if (c >= escLimit) {
                    if (c > maxNonEscaped) {
                        escCode = -1;
                        break;
                    }
                } else {
                    escCode = escCodes[c];
                    if (escCode != 0) {
                        break;
                    }
                }
                ptr++;
                if (ptr >= end) {
                    break;
                }
            }
            int flushLen = ptr - start;
            if (flushLen > 0) {
                this._writer.write(this._outputBuffer, start, flushLen);
                if (ptr >= end) {
                    return;
                }
            }
            ptr++;
            start = _prependOrWriteCharacterEscape(this._outputBuffer, ptr, end, c, escCode);
        }
    }

    private void _writeStringASCII(char[] text, int offset, int len, int maxNonEscaped) throws IOException, JsonGenerationException {
        char c;
        int len2 = len + offset;
        int[] escCodes = this._outputEscapes;
        int escLimit = Math.min(escCodes.length, maxNonEscaped + 1);
        int escCode = 0;
        while (offset < len2) {
            int start = offset;
            while (true) {
                c = text[offset];
                if (c >= escLimit) {
                    if (c > maxNonEscaped) {
                        escCode = -1;
                        break;
                    }
                } else {
                    escCode = escCodes[c];
                    if (escCode != 0) {
                        break;
                    }
                }
                offset++;
                if (offset >= len2) {
                    break;
                }
            }
            int newAmount = offset - start;
            if (newAmount < 32) {
                if (this._outputTail + newAmount > this._outputEnd) {
                    _flushBuffer();
                }
                if (newAmount > 0) {
                    System.arraycopy(text, start, this._outputBuffer, this._outputTail, newAmount);
                    this._outputTail += newAmount;
                }
            } else {
                _flushBuffer();
                this._writer.write(text, start, newAmount);
            }
            if (offset < len2) {
                offset++;
                _appendCharacterEscape(c, escCode);
            } else {
                return;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0057 A[SYNTHETIC] */
    private void _writeStringCustom(int len) throws IOException, JsonGenerationException {
        char c;
        int i;
        int escCode;
        int end = this._outputTail + len;
        int[] escCodes = this._outputEscapes;
        int maxNonEscaped = this._maximumNonEscapedChar < 1 ? SupportMenu.USER_MASK : this._maximumNonEscapedChar;
        int escLimit = Math.min(escCodes.length, maxNonEscaped + 1);
        CharacterEscapes customEscapes = this._characterEscapes;
        while (this._outputTail < end) {
            while (true) {
                c = this._outputBuffer[this._outputTail];
                if (c < escLimit) {
                    escCode = escCodes[c];
                    if (escCode != 0) {
                        break;
                    }
                    i = this._outputTail + 1;
                    this._outputTail = i;
                    if (i >= end) {
                        return;
                    }
                } else if (c > maxNonEscaped) {
                    escCode = -1;
                    break;
                } else {
                    SerializableString escapeSequence = customEscapes.getEscapeSequence(c);
                    this._currentEscape = escapeSequence;
                    if (escapeSequence != null) {
                        escCode = -2;
                        break;
                    }
                    i = this._outputTail + 1;
                    this._outputTail = i;
                    if (i >= end) {
                    }
                }
            }
            int i2 = this._outputTail;
            int i3 = this._outputHead;
            int flushLen = i2 - i3;
            if (flushLen > 0) {
                this._writer.write(this._outputBuffer, i3, flushLen);
            }
            this._outputTail++;
            _prependOrWriteCharacterEscape(c, escCode);
        }
    }

    private void _writeSegmentCustom(int end) throws IOException, JsonGenerationException {
        char c;
        int escCode;
        int i = end;
        int[] escCodes = this._outputEscapes;
        int maxNonEscaped = this._maximumNonEscapedChar < 1 ? SupportMenu.USER_MASK : this._maximumNonEscapedChar;
        int escLimit = Math.min(escCodes.length, maxNonEscaped + 1);
        CharacterEscapes customEscapes = this._characterEscapes;
        int ptr = 0;
        int escCode2 = 0;
        int start = 0;
        while (ptr < i) {
            while (true) {
                c = this._outputBuffer[ptr];
                if (c >= escLimit) {
                    if (c <= maxNonEscaped) {
                        SerializableString escapeSequence = customEscapes.getEscapeSequence(c);
                        this._currentEscape = escapeSequence;
                        if (escapeSequence != null) {
                            escCode = -2;
                            break;
                        }
                    } else {
                        escCode = -1;
                        break;
                    }
                } else {
                    escCode2 = escCodes[c];
                    if (escCode2 != 0) {
                        escCode = escCode2;
                        break;
                    }
                }
                ptr++;
                if (ptr >= i) {
                    escCode = escCode2;
                    break;
                }
            }
            int flushLen = ptr - start;
            if (flushLen > 0) {
                this._writer.write(this._outputBuffer, start, flushLen);
                if (ptr >= i) {
                    return;
                }
            }
            int ptr2 = ptr + 1;
            start = _prependOrWriteCharacterEscape(this._outputBuffer, ptr2, end, c, escCode);
            escCode2 = escCode;
            ptr = ptr2;
        }
    }

    private void _writeStringCustom(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        char c;
        int len2 = len + offset;
        int[] escCodes = this._outputEscapes;
        int maxNonEscaped = this._maximumNonEscapedChar < 1 ? SupportMenu.USER_MASK : this._maximumNonEscapedChar;
        int escLimit = Math.min(escCodes.length, maxNonEscaped + 1);
        CharacterEscapes customEscapes = this._characterEscapes;
        int escCode = 0;
        while (offset < len2) {
            int start = offset;
            while (true) {
                c = text[offset];
                if (c >= escLimit) {
                    if (c <= maxNonEscaped) {
                        SerializableString escapeSequence = customEscapes.getEscapeSequence(c);
                        this._currentEscape = escapeSequence;
                        if (escapeSequence != null) {
                            escCode = -2;
                            break;
                        }
                    } else {
                        escCode = -1;
                        break;
                    }
                } else {
                    escCode = escCodes[c];
                    if (escCode != 0) {
                        break;
                    }
                }
                offset++;
                if (offset >= len2) {
                    break;
                }
            }
            int newAmount = offset - start;
            if (newAmount < 32) {
                if (this._outputTail + newAmount > this._outputEnd) {
                    _flushBuffer();
                }
                if (newAmount > 0) {
                    System.arraycopy(text, start, this._outputBuffer, this._outputTail, newAmount);
                    this._outputTail += newAmount;
                }
            } else {
                _flushBuffer();
                this._writer.write(text, start, newAmount);
            }
            if (offset < len2) {
                offset++;
                _appendCharacterEscape(c, escCode);
            } else {
                return;
            }
        }
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
                char[] cArr = this._outputBuffer;
                int i = this._outputTail;
                this._outputTail = i + 1;
                cArr[i] = '\\';
                int i2 = this._outputTail;
                this._outputTail = i2 + 1;
                cArr[i2] = 'n';
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
                char[] cArr = this._outputBuffer;
                int i = this._outputTail;
                this._outputTail = i + 1;
                cArr[i] = '\\';
                int i2 = this._outputTail;
                this._outputTail = i2 + 1;
                cArr[i2] = 'n';
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
                char[] cArr = this._outputBuffer;
                int i = this._outputTail;
                this._outputTail = i + 1;
                cArr[i] = '\\';
                int i2 = this._outputTail;
                this._outputTail = i2 + 1;
                cArr[i2] = 'n';
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

    private int _readMore(InputStream in, byte[] readBuffer, int inputPtr, int inputEnd, int maxRead) throws IOException {
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

    private final void _writeNull() throws IOException {
        if (this._outputTail + 4 >= this._outputEnd) {
            _flushBuffer();
        }
        int ptr = this._outputTail;
        char[] buf = this._outputBuffer;
        buf[ptr] = 'n';
        int ptr2 = ptr + 1;
        buf[ptr2] = 'u';
        int ptr3 = ptr2 + 1;
        buf[ptr3] = 'l';
        int ptr4 = ptr3 + 1;
        buf[ptr4] = 'l';
        this._outputTail = ptr4 + 1;
    }

    private void _prependOrWriteCharacterEscape(char ch, int escCode) throws IOException, JsonGenerationException {
        String escape;
        int ptr;
        if (escCode >= 0) {
            int i = this._outputTail;
            if (i >= 2) {
                int ptr2 = i - 2;
                this._outputHead = ptr2;
                char[] cArr = this._outputBuffer;
                int ptr3 = ptr2 + 1;
                cArr[ptr2] = '\\';
                cArr[ptr3] = (char) escCode;
                return;
            }
            char[] buf = this._entityBuffer;
            if (buf == null) {
                buf = _allocateEntityBuffer();
            }
            this._outputHead = this._outputTail;
            buf[1] = (char) escCode;
            this._writer.write(buf, 0, 2);
        } else if (escCode != -2) {
            int i2 = this._outputTail;
            if (i2 >= 6) {
                char[] buf2 = this._outputBuffer;
                int ptr4 = i2 - 6;
                this._outputHead = ptr4;
                buf2[ptr4] = '\\';
                int ptr5 = ptr4 + 1;
                buf2[ptr5] = 'u';
                if (ch > 255) {
                    int hi = (ch >> 8) & 255;
                    int ptr6 = ptr5 + 1;
                    char[] cArr2 = HEX_CHARS;
                    buf2[ptr6] = cArr2[hi >> 4];
                    ptr = ptr6 + 1;
                    buf2[ptr] = cArr2[hi & 15];
                    ch = (char) (ch & 255);
                } else {
                    int ptr7 = ptr5 + 1;
                    buf2[ptr7] = '0';
                    ptr = ptr7 + 1;
                    buf2[ptr] = '0';
                }
                int ptr8 = ptr + 1;
                char[] cArr3 = HEX_CHARS;
                buf2[ptr8] = cArr3[ch >> 4];
                buf2[ptr8 + 1] = cArr3[ch & 15];
                return;
            }
            char[] buf3 = this._entityBuffer;
            if (buf3 == null) {
                buf3 = _allocateEntityBuffer();
            }
            this._outputHead = this._outputTail;
            if (ch > 255) {
                int hi2 = (ch >> 8) & 255;
                int lo = ch & 255;
                char[] cArr4 = HEX_CHARS;
                buf3[10] = cArr4[hi2 >> 4];
                buf3[11] = cArr4[hi2 & 15];
                buf3[12] = cArr4[lo >> 4];
                buf3[13] = cArr4[lo & 15];
                this._writer.write(buf3, 8, 6);
            } else {
                char[] cArr5 = HEX_CHARS;
                buf3[6] = cArr5[ch >> 4];
                buf3[7] = cArr5[ch & 15];
                this._writer.write(buf3, 2, 6);
            }
        } else {
            SerializableString serializableString = this._currentEscape;
            if (serializableString == null) {
                escape = this._characterEscapes.getEscapeSequence(ch).getValue();
            } else {
                escape = serializableString.getValue();
                this._currentEscape = null;
            }
            int len = escape.length();
            int ptr9 = this._outputTail;
            if (ptr9 >= len) {
                int ptr10 = ptr9 - len;
                this._outputHead = ptr10;
                escape.getChars(0, len, this._outputBuffer, ptr10);
                return;
            }
            this._outputHead = ptr9;
            this._writer.write(escape);
        }
    }

    private int _prependOrWriteCharacterEscape(char[] buffer, int ptr, int end, char ch, int escCode) throws IOException, JsonGenerationException {
        String escape;
        int ptr2;
        if (escCode >= 0) {
            if (ptr <= 1 || ptr >= end) {
                char[] ent = this._entityBuffer;
                if (ent == null) {
                    ent = _allocateEntityBuffer();
                }
                ent[1] = (char) escCode;
                this._writer.write(ent, 0, 2);
            } else {
                ptr -= 2;
                buffer[ptr] = '\\';
                buffer[ptr + 1] = (char) escCode;
            }
            return ptr;
        } else if (escCode != -2) {
            if (ptr <= 5 || ptr >= end) {
                char[] ent2 = this._entityBuffer;
                if (ent2 == null) {
                    ent2 = _allocateEntityBuffer();
                }
                this._outputHead = this._outputTail;
                if (ch > 255) {
                    int hi = (ch >> 8) & 255;
                    int lo = ch & 255;
                    char[] cArr = HEX_CHARS;
                    ent2[10] = cArr[hi >> 4];
                    ent2[11] = cArr[hi & 15];
                    ent2[12] = cArr[lo >> 4];
                    ent2[13] = cArr[lo & 15];
                    this._writer.write(ent2, 8, 6);
                } else {
                    char[] cArr2 = HEX_CHARS;
                    ent2[6] = cArr2[ch >> 4];
                    ent2[7] = cArr2[ch & 15];
                    this._writer.write(ent2, 2, 6);
                }
            } else {
                int ptr3 = ptr - 6;
                int ptr4 = ptr3 + 1;
                buffer[ptr3] = '\\';
                int ptr5 = ptr4 + 1;
                buffer[ptr4] = 'u';
                if (ch > 255) {
                    int hi2 = (ch >> 8) & 255;
                    int ptr6 = ptr5 + 1;
                    char[] cArr3 = HEX_CHARS;
                    buffer[ptr5] = cArr3[hi2 >> 4];
                    ptr2 = ptr6 + 1;
                    buffer[ptr6] = cArr3[hi2 & 15];
                    ch = (char) (ch & 255);
                } else {
                    int ptr7 = ptr5 + 1;
                    buffer[ptr5] = '0';
                    ptr2 = ptr7 + 1;
                    buffer[ptr7] = '0';
                }
                int ptr8 = ptr2 + 1;
                char[] cArr4 = HEX_CHARS;
                buffer[ptr2] = cArr4[ch >> 4];
                buffer[ptr8] = cArr4[ch & 15];
                ptr = ptr8 - 5;
            }
            return ptr;
        } else {
            SerializableString serializableString = this._currentEscape;
            if (serializableString == null) {
                escape = this._characterEscapes.getEscapeSequence(ch).getValue();
            } else {
                escape = serializableString.getValue();
                this._currentEscape = null;
            }
            int len = escape.length();
            if (ptr < len || ptr >= end) {
                this._writer.write(escape);
            } else {
                ptr -= len;
                escape.getChars(0, len, buffer, ptr);
            }
            return ptr;
        }
    }

    private void _appendCharacterEscape(char ch, int escCode) throws IOException, JsonGenerationException {
        String escape;
        int ptr;
        if (escCode >= 0) {
            if (this._outputTail + 2 > this._outputEnd) {
                _flushBuffer();
            }
            char[] cArr = this._outputBuffer;
            int i = this._outputTail;
            this._outputTail = i + 1;
            cArr[i] = '\\';
            int i2 = this._outputTail;
            this._outputTail = i2 + 1;
            cArr[i2] = (char) escCode;
        } else if (escCode != -2) {
            if (this._outputTail + 5 >= this._outputEnd) {
                _flushBuffer();
            }
            int ptr2 = this._outputTail;
            char[] buf = this._outputBuffer;
            int ptr3 = ptr2 + 1;
            buf[ptr2] = '\\';
            int ptr4 = ptr3 + 1;
            buf[ptr3] = 'u';
            if (ch > 255) {
                int hi = 255 & (ch >> 8);
                int ptr5 = ptr4 + 1;
                char[] cArr2 = HEX_CHARS;
                buf[ptr4] = cArr2[hi >> 4];
                ptr = ptr5 + 1;
                buf[ptr5] = cArr2[hi & 15];
                ch = (char) (ch & 255);
            } else {
                int ptr6 = ptr4 + 1;
                buf[ptr4] = '0';
                ptr = ptr6 + 1;
                buf[ptr6] = '0';
            }
            int ptr7 = ptr + 1;
            char[] cArr3 = HEX_CHARS;
            buf[ptr] = cArr3[ch >> 4];
            int ptr8 = ptr7 + 1;
            buf[ptr7] = cArr3[ch & 15];
            this._outputTail = ptr8;
        } else {
            SerializableString serializableString = this._currentEscape;
            if (serializableString == null) {
                escape = this._characterEscapes.getEscapeSequence(ch).getValue();
            } else {
                escape = serializableString.getValue();
                this._currentEscape = null;
            }
            int len = escape.length();
            if (this._outputTail + len > this._outputEnd) {
                _flushBuffer();
                if (len > this._outputEnd) {
                    this._writer.write(escape);
                    return;
                }
            }
            escape.getChars(0, len, this._outputBuffer, this._outputTail);
            this._outputTail += len;
        }
    }

    private char[] _allocateEntityBuffer() {
        char[] buf = new char[14];
        buf[0] = '\\';
        buf[2] = '\\';
        buf[3] = 'u';
        buf[4] = '0';
        buf[5] = '0';
        buf[8] = '\\';
        buf[9] = 'u';
        this._entityBuffer = buf;
        return buf;
    }

    private char[] _allocateCopyBuffer() {
        if (this._charBuffer == null) {
            this._charBuffer = this._ioContext.allocNameCopyBuffer(2000);
        }
        return this._charBuffer;
    }

    /* access modifiers changed from: protected */
    public void _flushBuffer() throws IOException {
        int len = this._outputTail - this._outputHead;
        if (len > 0) {
            int offset = this._outputHead;
            this._outputHead = 0;
            this._outputTail = 0;
            this._writer.write(this._outputBuffer, offset, len);
        }
    }
}
