package com.fasterxml.jackson.core.json.async;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.JsonReadContext;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.google.android.exoplayer2.extractor.ts.PsExtractor;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public abstract class NonBlockingJsonParserBase extends ParserBase {
    protected static final int MAJOR_ARRAY_ELEMENT_FIRST = 5;
    protected static final int MAJOR_ARRAY_ELEMENT_NEXT = 6;
    protected static final int MAJOR_CLOSED = 7;
    protected static final int MAJOR_INITIAL = 0;
    protected static final int MAJOR_OBJECT_FIELD_FIRST = 2;
    protected static final int MAJOR_OBJECT_FIELD_NEXT = 3;
    protected static final int MAJOR_OBJECT_VALUE = 4;
    protected static final int MAJOR_ROOT = 1;
    protected static final int MINOR_COMMENT_C = 53;
    protected static final int MINOR_COMMENT_CLOSING_ASTERISK = 52;
    protected static final int MINOR_COMMENT_CPP = 54;
    protected static final int MINOR_COMMENT_LEADING_SLASH = 51;
    protected static final int MINOR_COMMENT_YAML = 55;
    protected static final int MINOR_FIELD_APOS_NAME = 9;
    protected static final int MINOR_FIELD_LEADING_COMMA = 5;
    protected static final int MINOR_FIELD_LEADING_WS = 4;
    protected static final int MINOR_FIELD_NAME = 7;
    protected static final int MINOR_FIELD_NAME_ESCAPE = 8;
    protected static final int MINOR_FIELD_UNQUOTED_NAME = 10;
    protected static final int MINOR_NUMBER_EXPONENT_DIGITS = 32;
    protected static final int MINOR_NUMBER_EXPONENT_MARKER = 31;
    protected static final int MINOR_NUMBER_FRACTION_DIGITS = 30;
    protected static final int MINOR_NUMBER_INTEGER_DIGITS = 26;
    protected static final int MINOR_NUMBER_MINUS = 23;
    protected static final int MINOR_NUMBER_MINUSZERO = 25;
    protected static final int MINOR_NUMBER_ZERO = 24;
    protected static final int MINOR_ROOT_BOM = 1;
    protected static final int MINOR_ROOT_GOT_SEPARATOR = 3;
    protected static final int MINOR_ROOT_NEED_SEPARATOR = 2;
    protected static final int MINOR_VALUE_APOS_STRING = 45;
    protected static final int MINOR_VALUE_EXPECTING_COLON = 14;
    protected static final int MINOR_VALUE_EXPECTING_COMMA = 13;
    protected static final int MINOR_VALUE_LEADING_WS = 12;
    protected static final int MINOR_VALUE_STRING = 40;
    protected static final int MINOR_VALUE_STRING_ESCAPE = 41;
    protected static final int MINOR_VALUE_STRING_UTF8_2 = 42;
    protected static final int MINOR_VALUE_STRING_UTF8_3 = 43;
    protected static final int MINOR_VALUE_STRING_UTF8_4 = 44;
    protected static final int MINOR_VALUE_TOKEN_ERROR = 50;
    protected static final int MINOR_VALUE_TOKEN_FALSE = 18;
    protected static final int MINOR_VALUE_TOKEN_NON_STD = 19;
    protected static final int MINOR_VALUE_TOKEN_NULL = 16;
    protected static final int MINOR_VALUE_TOKEN_TRUE = 17;
    protected static final int MINOR_VALUE_WS_AFTER_COMMA = 15;
    protected static final String[] NON_STD_TOKENS = {"NaN", "Infinity", "+Infinity", "-Infinity"};
    protected static final int NON_STD_TOKEN_INFINITY = 1;
    protected static final int NON_STD_TOKEN_MINUS_INFINITY = 3;
    protected static final int NON_STD_TOKEN_NAN = 0;
    protected static final int NON_STD_TOKEN_PLUS_INFINITY = 2;
    protected static final double[] NON_STD_TOKEN_VALUES = {Double.NaN, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
    protected int _currBufferStart = 0;
    protected int _currInputRowAlt = 1;
    protected boolean _endOfInput = false;
    protected int _majorState;
    protected int _majorStateAfterValue;
    protected int _minorState;
    protected int _minorStateAfterSplit;
    protected int _nonStdTokenType;
    protected int _pending32;
    protected int _pendingBytes;
    protected int _quad1;
    protected int[] _quadBuffer = new int[8];
    protected int _quadLength;
    protected int _quoted32;
    protected int _quotedDigits;
    protected final ByteQuadsCanonicalizer _symbols;

    public abstract int releaseBuffered(OutputStream outputStream) throws IOException;

    public NonBlockingJsonParserBase(IOContext ctxt, int parserFeatures, ByteQuadsCanonicalizer sym) {
        super(ctxt, parserFeatures);
        this._symbols = sym;
        this._currToken = null;
        this._majorState = 0;
        this._majorStateAfterValue = 1;
    }

    public ObjectCodec getCodec() {
        return null;
    }

    public void setCodec(ObjectCodec c) {
        throw new UnsupportedOperationException("Can not use ObjectMapper with non-blocking parser");
    }

    public boolean canParseAsync() {
        return true;
    }

    /* access modifiers changed from: protected */
    public ByteQuadsCanonicalizer symbolTableForTests() {
        return this._symbols;
    }

    /* access modifiers changed from: protected */
    public void _releaseBuffers() throws IOException {
        super._releaseBuffers();
        this._symbols.release();
    }

    public Object getInputSource() {
        return null;
    }

    /* access modifiers changed from: protected */
    public void _closeInput() throws IOException {
        this._currBufferStart = 0;
        this._inputEnd = 0;
    }

    public boolean hasTextCharacters() {
        if (this._currToken == JsonToken.VALUE_STRING) {
            return this._textBuffer.hasTextAsCharacters();
        }
        if (this._currToken == JsonToken.FIELD_NAME) {
            return this._nameCopied;
        }
        return false;
    }

    public JsonLocation getCurrentLocation() {
        int col = (this._inputPtr - this._currInputRowStart) + 1;
        JsonLocation jsonLocation = new JsonLocation(_getSourceReference(), this._currInputProcessed + ((long) (this._inputPtr - this._currBufferStart)), -1, Math.max(this._currInputRow, this._currInputRowAlt), col);
        return jsonLocation;
    }

    public JsonLocation getTokenLocation() {
        JsonLocation jsonLocation = new JsonLocation(_getSourceReference(), this._tokenInputTotal, -1, this._tokenInputRow, this._tokenInputCol);
        return jsonLocation;
    }

    public String getText() throws IOException {
        if (this._currToken == JsonToken.VALUE_STRING) {
            return this._textBuffer.contentsAsString();
        }
        return _getText2(this._currToken);
    }

    /* access modifiers changed from: protected */
    public final String _getText2(JsonToken t) {
        if (t == null) {
            return null;
        }
        int id = t.id();
        if (id == -1) {
            return null;
        }
        switch (id) {
            case 5:
                return this._parsingContext.getCurrentName();
            case 6:
            case 7:
            case 8:
                return this._textBuffer.contentsAsString();
            default:
                return t.asString();
        }
    }

    public int getText(Writer writer) throws IOException {
        JsonToken t = this._currToken;
        if (t == JsonToken.VALUE_STRING) {
            return this._textBuffer.contentsToWriter(writer);
        }
        if (t == JsonToken.FIELD_NAME) {
            String n = this._parsingContext.getCurrentName();
            writer.write(n);
            return n.length();
        } else if (t == null) {
            return 0;
        } else {
            if (t.isNumeric()) {
                return this._textBuffer.contentsToWriter(writer);
            }
            if (t == JsonToken.NOT_AVAILABLE) {
                _reportError("Current token not available: can not call this method");
            }
            char[] ch = t.asCharArray();
            writer.write(ch);
            return ch.length;
        }
    }

    public String getValueAsString() throws IOException {
        if (this._currToken == JsonToken.VALUE_STRING) {
            return this._textBuffer.contentsAsString();
        }
        if (this._currToken == JsonToken.FIELD_NAME) {
            return getCurrentName();
        }
        return super.getValueAsString(null);
    }

    public String getValueAsString(String defValue) throws IOException {
        if (this._currToken == JsonToken.VALUE_STRING) {
            return this._textBuffer.contentsAsString();
        }
        if (this._currToken == JsonToken.FIELD_NAME) {
            return getCurrentName();
        }
        return super.getValueAsString(defValue);
    }

    public char[] getTextCharacters() throws IOException {
        if (this._currToken == null) {
            return null;
        }
        switch (this._currToken.id()) {
            case 5:
                if (!this._nameCopied) {
                    String name = this._parsingContext.getCurrentName();
                    int nameLen = name.length();
                    if (this._nameCopyBuffer == null) {
                        this._nameCopyBuffer = this._ioContext.allocNameCopyBuffer(nameLen);
                    } else if (this._nameCopyBuffer.length < nameLen) {
                        this._nameCopyBuffer = new char[nameLen];
                    }
                    name.getChars(0, nameLen, this._nameCopyBuffer, 0);
                    this._nameCopied = true;
                }
                return this._nameCopyBuffer;
            case 6:
            case 7:
            case 8:
                return this._textBuffer.getTextBuffer();
            default:
                return this._currToken.asCharArray();
        }
    }

    public int getTextLength() throws IOException {
        if (this._currToken == null) {
            return 0;
        }
        switch (this._currToken.id()) {
            case 5:
                return this._parsingContext.getCurrentName().length();
            case 6:
            case 7:
            case 8:
                return this._textBuffer.size();
            default:
                return this._currToken.asCharArray().length;
        }
    }

    public int getTextOffset() throws IOException {
        if (this._currToken != null) {
            switch (this._currToken.id()) {
                case 5:
                    return 0;
                case 6:
                case 7:
                case 8:
                    return this._textBuffer.getTextOffset();
            }
        }
        return 0;
    }

    public byte[] getBinaryValue(Base64Variant b64variant) throws IOException {
        if (this._currToken != JsonToken.VALUE_STRING) {
            _reportError("Current token (%s) not VALUE_STRING or VALUE_EMBEDDED_OBJECT, can not access as binary", this._currToken);
        }
        if (this._binaryValue == null) {
            ByteArrayBuilder builder = _getByteArrayBuilder();
            _decodeBase64(getText(), builder, b64variant);
            this._binaryValue = builder.toByteArray();
        }
        return this._binaryValue;
    }

    public int readBinaryValue(Base64Variant b64variant, OutputStream out) throws IOException {
        byte[] b = getBinaryValue(b64variant);
        out.write(b);
        return b.length;
    }

    public Object getEmbeddedObject() throws IOException {
        if (this._currToken == JsonToken.VALUE_EMBEDDED_OBJECT) {
            return this._binaryValue;
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public final JsonToken _startArrayScope() throws IOException {
        this._parsingContext = this._parsingContext.createChildArrayContext(-1, -1);
        this._majorState = 5;
        this._majorStateAfterValue = 6;
        JsonToken jsonToken = JsonToken.START_ARRAY;
        this._currToken = jsonToken;
        return jsonToken;
    }

    /* access modifiers changed from: protected */
    public final JsonToken _startObjectScope() throws IOException {
        this._parsingContext = this._parsingContext.createChildObjectContext(-1, -1);
        this._majorState = 2;
        this._majorStateAfterValue = 3;
        JsonToken jsonToken = JsonToken.START_OBJECT;
        this._currToken = jsonToken;
        return jsonToken;
    }

    /* access modifiers changed from: protected */
    public final JsonToken _closeArrayScope() throws IOException {
        int st;
        if (!this._parsingContext.inArray()) {
            _reportMismatchedEndMarker(93, '}');
        }
        JsonReadContext ctxt = this._parsingContext.getParent();
        this._parsingContext = ctxt;
        if (ctxt.inObject()) {
            st = 3;
        } else if (ctxt.inArray()) {
            st = 6;
        } else {
            st = 1;
        }
        this._majorState = st;
        this._majorStateAfterValue = st;
        JsonToken jsonToken = JsonToken.END_ARRAY;
        this._currToken = jsonToken;
        return jsonToken;
    }

    /* access modifiers changed from: protected */
    public final JsonToken _closeObjectScope() throws IOException {
        int st;
        if (!this._parsingContext.inObject()) {
            _reportMismatchedEndMarker(125, ']');
        }
        JsonReadContext ctxt = this._parsingContext.getParent();
        this._parsingContext = ctxt;
        if (ctxt.inObject()) {
            st = 3;
        } else if (ctxt.inArray()) {
            st = 6;
        } else {
            st = 1;
        }
        this._majorState = st;
        this._majorStateAfterValue = st;
        JsonToken jsonToken = JsonToken.END_OBJECT;
        this._currToken = jsonToken;
        return jsonToken;
    }

    /* access modifiers changed from: protected */
    public final String _findName(int q1, int lastQuadBytes) throws JsonParseException {
        int q12 = _padLastQuad(q1, lastQuadBytes);
        String name = this._symbols.findName(q12);
        if (name != null) {
            return name;
        }
        int[] iArr = this._quadBuffer;
        iArr[0] = q12;
        return _addName(iArr, 1, lastQuadBytes);
    }

    /* access modifiers changed from: protected */
    public final String _findName(int q1, int q2, int lastQuadBytes) throws JsonParseException {
        int q22 = _padLastQuad(q2, lastQuadBytes);
        String name = this._symbols.findName(q1, q22);
        if (name != null) {
            return name;
        }
        int[] iArr = this._quadBuffer;
        iArr[0] = q1;
        iArr[1] = q22;
        return _addName(iArr, 2, lastQuadBytes);
    }

    /* access modifiers changed from: protected */
    public final String _findName(int q1, int q2, int q3, int lastQuadBytes) throws JsonParseException {
        int q32 = _padLastQuad(q3, lastQuadBytes);
        String name = this._symbols.findName(q1, q2, q32);
        if (name != null) {
            return name;
        }
        int[] quads = this._quadBuffer;
        quads[0] = q1;
        quads[1] = q2;
        quads[2] = _padLastQuad(q32, lastQuadBytes);
        return _addName(quads, 3, lastQuadBytes);
    }

    /* access modifiers changed from: protected */
    public final String _addName(int[] quads, int qlen, int lastQuadBytes) throws JsonParseException {
        int lastQuad;
        int needed;
        int ch;
        int[] iArr = quads;
        int i = qlen;
        int i2 = lastQuadBytes;
        int byteLen = ((i << 2) - 4) + i2;
        if (i2 < 4) {
            lastQuad = iArr[i - 1];
            iArr[i - 1] = lastQuad << ((4 - i2) << 3);
        } else {
            lastQuad = 0;
        }
        char[] cbuf = this._textBuffer.emptyAndGetCurrentSegment();
        int cix = 0;
        int ix = 0;
        while (ix < byteLen) {
            int ch2 = (iArr[ix >> 2] >> ((3 - (ix & 3)) << 3)) & 255;
            ix++;
            if (ch2 > 127) {
                if ((ch2 & 224) == 192) {
                    ch = ch2 & 31;
                    needed = 1;
                } else if ((ch2 & PsExtractor.VIDEO_STREAM_MASK) == 224) {
                    ch = ch2 & 15;
                    needed = 2;
                } else if ((ch2 & 248) == 240) {
                    ch = ch2 & 7;
                    needed = 3;
                } else {
                    _reportInvalidInitial(ch2);
                    ch = 1;
                    needed = 1;
                }
                if (ix + needed > byteLen) {
                    _reportInvalidEOF(" in field name", JsonToken.FIELD_NAME);
                }
                int ch22 = iArr[ix >> 2] >> ((3 - (ix & 3)) << 3);
                ix++;
                if ((ch22 & PsExtractor.AUDIO_STREAM) != 128) {
                    _reportInvalidOther(ch22);
                }
                ch2 = (ch << 6) | (ch22 & 63);
                if (needed > 1) {
                    int ch23 = iArr[ix >> 2] >> ((3 - (ix & 3)) << 3);
                    ix++;
                    if ((ch23 & PsExtractor.AUDIO_STREAM) != 128) {
                        _reportInvalidOther(ch23);
                    }
                    ch2 = (ch2 << 6) | (ch23 & 63);
                    if (needed > 2) {
                        int ch24 = iArr[ix >> 2] >> ((3 - (ix & 3)) << 3);
                        ix++;
                        if ((ch24 & PsExtractor.AUDIO_STREAM) != 128) {
                            _reportInvalidOther(ch24 & 255);
                        }
                        ch2 = (ch2 << 6) | (ch24 & 63);
                    }
                }
                if (needed > 2) {
                    int ch3 = ch2 - 65536;
                    if (cix >= cbuf.length) {
                        cbuf = this._textBuffer.expandCurrentSegment();
                    }
                    int cix2 = cix + 1;
                    cbuf[cix] = (char) ((ch3 >> 10) + GeneratorBase.SURR1_FIRST);
                    ch2 = (ch3 & 1023) | GeneratorBase.SURR2_FIRST;
                    cix = cix2;
                }
            }
            if (cix >= cbuf.length) {
                cbuf = this._textBuffer.expandCurrentSegment();
            }
            int cix3 = cix + 1;
            cbuf[cix] = (char) ch2;
            cix = cix3;
        }
        String baseName = new String(cbuf, 0, cix);
        if (i2 < 4) {
            iArr[i - 1] = lastQuad;
        }
        return this._symbols.addName(baseName, iArr, i);
    }

    protected static final int _padLastQuad(int q, int bytes) {
        return bytes == 4 ? q : (-1 << (bytes << 3)) | q;
    }

    /* access modifiers changed from: protected */
    public final JsonToken _eofAsNextToken() throws IOException {
        this._majorState = 7;
        if (!this._parsingContext.inRoot()) {
            _handleEOF();
        }
        close();
        this._currToken = null;
        return null;
    }

    /* access modifiers changed from: protected */
    public final JsonToken _fieldComplete(String name) throws IOException {
        this._majorState = 4;
        this._parsingContext.setCurrentName(name);
        JsonToken jsonToken = JsonToken.FIELD_NAME;
        this._currToken = jsonToken;
        return jsonToken;
    }

    /* access modifiers changed from: protected */
    public final JsonToken _valueComplete(JsonToken t) throws IOException {
        this._majorState = this._majorStateAfterValue;
        this._currToken = t;
        return t;
    }

    /* access modifiers changed from: protected */
    public final JsonToken _valueCompleteInt(int value, String asText) throws IOException {
        this._textBuffer.resetWithString(asText);
        this._intLength = asText.length();
        this._numTypesValid = 1;
        this._numberInt = value;
        this._majorState = this._majorStateAfterValue;
        JsonToken t = JsonToken.VALUE_NUMBER_INT;
        this._currToken = t;
        return t;
    }

    /* access modifiers changed from: protected */
    public final JsonToken _valueNonStdNumberComplete(int type) throws IOException {
        String tokenStr = NON_STD_TOKENS[type];
        this._textBuffer.resetWithString(tokenStr);
        if (!isEnabled(Feature.ALLOW_NON_NUMERIC_NUMBERS)) {
            _reportError("Non-standard token '%s': enable JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS to allow", tokenStr);
        }
        this._intLength = 0;
        this._numTypesValid = 8;
        this._numberDouble = NON_STD_TOKEN_VALUES[type];
        this._majorState = this._majorStateAfterValue;
        JsonToken jsonToken = JsonToken.VALUE_NUMBER_FLOAT;
        this._currToken = jsonToken;
        return jsonToken;
    }

    /* access modifiers changed from: protected */
    public final String _nonStdToken(int type) {
        return NON_STD_TOKENS[type];
    }

    /* access modifiers changed from: protected */
    public final void _updateTokenLocation() {
        this._tokenInputRow = Math.max(this._currInputRow, this._currInputRowAlt);
        int ptr = this._inputPtr;
        this._tokenInputCol = ptr - this._currInputRowStart;
        this._tokenInputTotal = this._currInputProcessed + ((long) (ptr - this._currBufferStart));
    }

    /* access modifiers changed from: protected */
    public void _reportInvalidChar(int c) throws JsonParseException {
        if (c < 32) {
            _throwInvalidSpace(c);
        }
        _reportInvalidInitial(c);
    }

    /* access modifiers changed from: protected */
    public void _reportInvalidInitial(int mask) throws JsonParseException {
        StringBuilder sb = new StringBuilder();
        sb.append("Invalid UTF-8 start byte 0x");
        sb.append(Integer.toHexString(mask));
        _reportError(sb.toString());
    }

    /* access modifiers changed from: protected */
    public void _reportInvalidOther(int mask, int ptr) throws JsonParseException {
        this._inputPtr = ptr;
        _reportInvalidOther(mask);
    }

    /* access modifiers changed from: protected */
    public void _reportInvalidOther(int mask) throws JsonParseException {
        StringBuilder sb = new StringBuilder();
        sb.append("Invalid UTF-8 middle byte 0x");
        sb.append(Integer.toHexString(mask));
        _reportError(sb.toString());
    }
}
