package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.google.android.exoplayer2.extractor.ts.PsExtractor;
import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import org.apache.commons.lang3.CharUtils;

public class UTF8DataInputJsonParser extends ParserBase {
    static final byte BYTE_LF = 10;
    protected static final int[] _icLatin1 = CharTypes.getInputCodeLatin1();
    private static final int[] _icUTF8 = CharTypes.getInputCodeUtf8();
    protected DataInput _inputData;
    protected int _nextByte = -1;
    protected ObjectCodec _objectCodec;
    private int _quad1;
    protected int[] _quadBuffer = new int[16];
    protected final ByteQuadsCanonicalizer _symbols;
    protected boolean _tokenIncomplete;

    public UTF8DataInputJsonParser(IOContext ctxt, int features, DataInput inputData, ObjectCodec codec, ByteQuadsCanonicalizer sym, int firstByte) {
        super(ctxt, features);
        this._objectCodec = codec;
        this._symbols = sym;
        this._inputData = inputData;
        this._nextByte = firstByte;
    }

    public ObjectCodec getCodec() {
        return this._objectCodec;
    }

    public void setCodec(ObjectCodec c) {
        this._objectCodec = c;
    }

    public int releaseBuffered(OutputStream out) throws IOException {
        return 0;
    }

    public Object getInputSource() {
        return this._inputData;
    }

    /* access modifiers changed from: protected */
    public void _closeInput() throws IOException {
    }

    /* access modifiers changed from: protected */
    public void _releaseBuffers() throws IOException {
        super._releaseBuffers();
        this._symbols.release();
    }

    public String getText() throws IOException {
        if (this._currToken != JsonToken.VALUE_STRING) {
            return _getText2(this._currToken);
        }
        if (!this._tokenIncomplete) {
            return this._textBuffer.contentsAsString();
        }
        this._tokenIncomplete = false;
        return _finishAndReturnString();
    }

    public int getText(Writer writer) throws IOException {
        JsonToken t = this._currToken;
        if (t == JsonToken.VALUE_STRING) {
            if (this._tokenIncomplete) {
                this._tokenIncomplete = false;
                _finishString();
            }
            return this._textBuffer.contentsToWriter(writer);
        } else if (t == JsonToken.FIELD_NAME) {
            String n = this._parsingContext.getCurrentName();
            writer.write(n);
            return n.length();
        } else if (t == null) {
            return 0;
        } else {
            if (t.isNumeric()) {
                return this._textBuffer.contentsToWriter(writer);
            }
            char[] ch = t.asCharArray();
            writer.write(ch);
            return ch.length;
        }
    }

    public String getValueAsString() throws IOException {
        if (this._currToken == JsonToken.VALUE_STRING) {
            if (!this._tokenIncomplete) {
                return this._textBuffer.contentsAsString();
            }
            this._tokenIncomplete = false;
            return _finishAndReturnString();
        } else if (this._currToken == JsonToken.FIELD_NAME) {
            return getCurrentName();
        } else {
            return super.getValueAsString(null);
        }
    }

    public String getValueAsString(String defValue) throws IOException {
        if (this._currToken == JsonToken.VALUE_STRING) {
            if (!this._tokenIncomplete) {
                return this._textBuffer.contentsAsString();
            }
            this._tokenIncomplete = false;
            return _finishAndReturnString();
        } else if (this._currToken == JsonToken.FIELD_NAME) {
            return getCurrentName();
        } else {
            return super.getValueAsString(defValue);
        }
    }

    public int getValueAsInt() throws IOException {
        JsonToken t = this._currToken;
        if (t != JsonToken.VALUE_NUMBER_INT && t != JsonToken.VALUE_NUMBER_FLOAT) {
            return super.getValueAsInt(0);
        }
        if ((this._numTypesValid & 1) == 0) {
            if (this._numTypesValid == 0) {
                return _parseIntValue();
            }
            if ((this._numTypesValid & 1) == 0) {
                convertNumberToInt();
            }
        }
        return this._numberInt;
    }

    public int getValueAsInt(int defValue) throws IOException {
        JsonToken t = this._currToken;
        if (t != JsonToken.VALUE_NUMBER_INT && t != JsonToken.VALUE_NUMBER_FLOAT) {
            return super.getValueAsInt(defValue);
        }
        if ((this._numTypesValid & 1) == 0) {
            if (this._numTypesValid == 0) {
                return _parseIntValue();
            }
            if ((this._numTypesValid & 1) == 0) {
                convertNumberToInt();
            }
        }
        return this._numberInt;
    }

    /* access modifiers changed from: protected */
    public final String _getText2(JsonToken t) {
        if (t == null) {
            return null;
        }
        switch (t.id()) {
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
                if (this._tokenIncomplete) {
                    this._tokenIncomplete = false;
                    _finishString();
                    break;
                }
                break;
            case 7:
            case 8:
                break;
            default:
                return this._currToken.asCharArray();
        }
        return this._textBuffer.getTextBuffer();
    }

    public int getTextLength() throws IOException {
        if (this._currToken == JsonToken.VALUE_STRING) {
            if (this._tokenIncomplete) {
                this._tokenIncomplete = false;
                _finishString();
            }
            return this._textBuffer.size();
        } else if (this._currToken == JsonToken.FIELD_NAME) {
            return this._parsingContext.getCurrentName().length();
        } else {
            if (this._currToken == null) {
                return 0;
            }
            if (this._currToken.isNumeric()) {
                return this._textBuffer.size();
            }
            return this._currToken.asCharArray().length;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:8:0x001e, code lost:
        return r2._textBuffer.getTextOffset();
     */
    public int getTextOffset() throws IOException {
        if (this._currToken != null) {
            switch (this._currToken.id()) {
                case 5:
                    return 0;
                case 6:
                    if (this._tokenIncomplete) {
                        this._tokenIncomplete = false;
                        _finishString();
                        break;
                    }
                    break;
                case 7:
                case 8:
                    break;
            }
        }
        return 0;
    }

    public byte[] getBinaryValue(Base64Variant b64variant) throws IOException {
        if (this._currToken != JsonToken.VALUE_STRING && (this._currToken != JsonToken.VALUE_EMBEDDED_OBJECT || this._binaryValue == null)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Current token (");
            sb.append(this._currToken);
            sb.append(") not VALUE_STRING or VALUE_EMBEDDED_OBJECT, can not access as binary");
            _reportError(sb.toString());
        }
        if (this._tokenIncomplete) {
            try {
                this._binaryValue = _decodeBase64(b64variant);
                this._tokenIncomplete = false;
            } catch (IllegalArgumentException iae) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Failed to decode VALUE_STRING as base64 (");
                sb2.append(b64variant);
                sb2.append("): ");
                sb2.append(iae.getMessage());
                throw _constructError(sb2.toString());
            }
        } else if (this._binaryValue == null) {
            ByteArrayBuilder builder = _getByteArrayBuilder();
            _decodeBase64(getText(), builder, b64variant);
            this._binaryValue = builder.toByteArray();
        }
        return this._binaryValue;
    }

    public int readBinaryValue(Base64Variant b64variant, OutputStream out) throws IOException {
        if (!this._tokenIncomplete || this._currToken != JsonToken.VALUE_STRING) {
            byte[] b = getBinaryValue(b64variant);
            out.write(b);
            return b.length;
        }
        byte[] buf = this._ioContext.allocBase64Buffer();
        try {
            return _readBinary(b64variant, out, buf);
        } finally {
            this._ioContext.releaseBase64Buffer(buf);
        }
    }

    /* access modifiers changed from: protected */
    public int _readBinary(Base64Variant b64variant, OutputStream out, byte[] buffer) throws IOException {
        int outputPtr = 0;
        int outputEnd = buffer.length - 3;
        int outputCount = 0;
        while (true) {
            int ch = this._inputData.readUnsignedByte();
            if (ch > 32) {
                int bits = b64variant.decodeBase64Char(ch);
                if (bits < 0) {
                    if (ch == 34) {
                        break;
                    }
                    bits = _decodeBase64Escape(b64variant, ch, 0);
                    if (bits < 0) {
                        continue;
                    }
                }
                if (outputPtr > outputEnd) {
                    outputCount += outputPtr;
                    out.write(buffer, 0, outputPtr);
                    outputPtr = 0;
                }
                int decodedData = bits;
                int ch2 = this._inputData.readUnsignedByte();
                int bits2 = b64variant.decodeBase64Char(ch2);
                if (bits2 < 0) {
                    bits2 = _decodeBase64Escape(b64variant, ch2, 1);
                }
                int decodedData2 = (decodedData << 6) | bits2;
                int ch3 = this._inputData.readUnsignedByte();
                int bits3 = b64variant.decodeBase64Char(ch3);
                if (bits3 < 0) {
                    if (bits3 != -2) {
                        if (ch3 == 34 && !b64variant.usesPadding()) {
                            int outputPtr2 = outputPtr + 1;
                            buffer[outputPtr] = (byte) (decodedData2 >> 4);
                            outputPtr = outputPtr2;
                            break;
                        }
                        bits3 = _decodeBase64Escape(b64variant, ch3, 2);
                    }
                    if (bits3 == -2) {
                        int ch4 = this._inputData.readUnsignedByte();
                        if (b64variant.usesPaddingChar(ch4)) {
                            int outputPtr3 = outputPtr + 1;
                            buffer[outputPtr] = (byte) (decodedData2 >> 4);
                            outputPtr = outputPtr3;
                        } else {
                            StringBuilder sb = new StringBuilder();
                            sb.append("expected padding character '");
                            sb.append(b64variant.getPaddingChar());
                            sb.append("'");
                            throw reportInvalidBase64Char(b64variant, ch4, 3, sb.toString());
                        }
                    }
                }
                int decodedData3 = (decodedData2 << 6) | bits3;
                int ch5 = this._inputData.readUnsignedByte();
                int bits4 = b64variant.decodeBase64Char(ch5);
                if (bits4 < 0) {
                    if (bits4 != -2) {
                        if (ch5 == 34 && !b64variant.usesPadding()) {
                            int decodedData4 = decodedData3 >> 2;
                            int outputPtr4 = outputPtr + 1;
                            buffer[outputPtr] = (byte) (decodedData4 >> 8);
                            outputPtr = outputPtr4 + 1;
                            buffer[outputPtr4] = (byte) decodedData4;
                            break;
                        }
                        bits4 = _decodeBase64Escape(b64variant, ch5, 3);
                    }
                    if (bits4 == -2) {
                        int decodedData5 = decodedData3 >> 2;
                        int outputPtr5 = outputPtr + 1;
                        buffer[outputPtr] = (byte) (decodedData5 >> 8);
                        outputPtr = outputPtr5 + 1;
                        buffer[outputPtr5] = (byte) decodedData5;
                    }
                }
                int decodedData6 = (decodedData3 << 6) | bits4;
                int outputPtr6 = outputPtr + 1;
                buffer[outputPtr] = (byte) (decodedData6 >> 16);
                int outputPtr7 = outputPtr6 + 1;
                buffer[outputPtr6] = (byte) (decodedData6 >> 8);
                int outputPtr8 = outputPtr7 + 1;
                buffer[outputPtr7] = (byte) decodedData6;
                outputPtr = outputPtr8;
            }
        }
        this._tokenIncomplete = false;
        if (outputPtr <= 0) {
            return outputCount;
        }
        int outputCount2 = outputCount + outputPtr;
        out.write(buffer, 0, outputPtr);
        return outputCount2;
    }

    public JsonToken nextToken() throws IOException {
        JsonToken t;
        if (this._currToken == JsonToken.FIELD_NAME) {
            return _nextAfterName();
        }
        this._numTypesValid = 0;
        if (this._tokenIncomplete) {
            _skipString();
        }
        int i = _skipWSOrEnd();
        if (i < 0) {
            close();
            this._currToken = null;
            return null;
        }
        this._binaryValue = null;
        this._tokenInputRow = this._currInputRow;
        if (i == 93 || i == 125) {
            _closeScope(i);
            return this._currToken;
        }
        if (this._parsingContext.expectComma()) {
            if (i != 44) {
                StringBuilder sb = new StringBuilder();
                sb.append("was expecting comma to separate ");
                sb.append(this._parsingContext.typeDesc());
                sb.append(" entries");
                _reportUnexpectedChar(i, sb.toString());
            }
            i = _skipWS();
            if (Feature.ALLOW_TRAILING_COMMA.enabledIn(this._features) && (i == 93 || i == 125)) {
                _closeScope(i);
                return this._currToken;
            }
        }
        if (!this._parsingContext.inObject()) {
            return _nextTokenNotInObject(i);
        }
        this._parsingContext.setCurrentName(_parseName(i));
        this._currToken = JsonToken.FIELD_NAME;
        int i2 = _skipColon();
        if (i2 == 34) {
            this._tokenIncomplete = true;
            this._nextToken = JsonToken.VALUE_STRING;
            return this._currToken;
        }
        if (i2 == 45) {
            t = _parseNegNumber();
        } else if (i2 == 91) {
            t = JsonToken.START_ARRAY;
        } else if (i2 == 102) {
            _matchToken("false", 1);
            t = JsonToken.VALUE_FALSE;
        } else if (i2 == 110) {
            _matchToken("null", 1);
            t = JsonToken.VALUE_NULL;
        } else if (i2 == 116) {
            _matchToken("true", 1);
            t = JsonToken.VALUE_TRUE;
        } else if (i2 != 123) {
            switch (i2) {
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                    t = _parsePosNumber(i2);
                    break;
                default:
                    t = _handleUnexpectedValue(i2);
                    break;
            }
        } else {
            t = JsonToken.START_OBJECT;
        }
        this._nextToken = t;
        return this._currToken;
    }

    private final JsonToken _nextTokenNotInObject(int i) throws IOException {
        if (i == 34) {
            this._tokenIncomplete = true;
            JsonToken jsonToken = JsonToken.VALUE_STRING;
            this._currToken = jsonToken;
            return jsonToken;
        } else if (i == 45) {
            JsonToken _parseNegNumber = _parseNegNumber();
            this._currToken = _parseNegNumber;
            return _parseNegNumber;
        } else if (i == 91) {
            this._parsingContext = this._parsingContext.createChildArrayContext(this._tokenInputRow, this._tokenInputCol);
            JsonToken jsonToken2 = JsonToken.START_ARRAY;
            this._currToken = jsonToken2;
            return jsonToken2;
        } else if (i == 102) {
            _matchToken("false", 1);
            JsonToken jsonToken3 = JsonToken.VALUE_FALSE;
            this._currToken = jsonToken3;
            return jsonToken3;
        } else if (i == 110) {
            _matchToken("null", 1);
            JsonToken jsonToken4 = JsonToken.VALUE_NULL;
            this._currToken = jsonToken4;
            return jsonToken4;
        } else if (i == 116) {
            _matchToken("true", 1);
            JsonToken jsonToken5 = JsonToken.VALUE_TRUE;
            this._currToken = jsonToken5;
            return jsonToken5;
        } else if (i != 123) {
            switch (i) {
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                    JsonToken _parsePosNumber = _parsePosNumber(i);
                    this._currToken = _parsePosNumber;
                    return _parsePosNumber;
                default:
                    JsonToken _handleUnexpectedValue = _handleUnexpectedValue(i);
                    this._currToken = _handleUnexpectedValue;
                    return _handleUnexpectedValue;
            }
        } else {
            this._parsingContext = this._parsingContext.createChildObjectContext(this._tokenInputRow, this._tokenInputCol);
            JsonToken jsonToken6 = JsonToken.START_OBJECT;
            this._currToken = jsonToken6;
            return jsonToken6;
        }
    }

    private final JsonToken _nextAfterName() {
        this._nameCopied = false;
        JsonToken t = this._nextToken;
        this._nextToken = null;
        if (t == JsonToken.START_ARRAY) {
            this._parsingContext = this._parsingContext.createChildArrayContext(this._tokenInputRow, this._tokenInputCol);
        } else if (t == JsonToken.START_OBJECT) {
            this._parsingContext = this._parsingContext.createChildObjectContext(this._tokenInputRow, this._tokenInputCol);
        }
        this._currToken = t;
        return t;
    }

    public void finishToken() throws IOException {
        if (this._tokenIncomplete) {
            this._tokenIncomplete = false;
            _finishString();
        }
    }

    public String nextFieldName() throws IOException {
        JsonToken t;
        this._numTypesValid = 0;
        if (this._currToken == JsonToken.FIELD_NAME) {
            _nextAfterName();
            return null;
        }
        if (this._tokenIncomplete) {
            _skipString();
        }
        int i = _skipWS();
        this._binaryValue = null;
        this._tokenInputRow = this._currInputRow;
        if (i == 93 || i == 125) {
            _closeScope(i);
            return null;
        }
        if (this._parsingContext.expectComma()) {
            if (i != 44) {
                StringBuilder sb = new StringBuilder();
                sb.append("was expecting comma to separate ");
                sb.append(this._parsingContext.typeDesc());
                sb.append(" entries");
                _reportUnexpectedChar(i, sb.toString());
            }
            i = _skipWS();
            if (Feature.ALLOW_TRAILING_COMMA.enabledIn(this._features) && (i == 93 || i == 125)) {
                _closeScope(i);
                return null;
            }
        }
        if (!this._parsingContext.inObject()) {
            _nextTokenNotInObject(i);
            return null;
        }
        String nameStr = _parseName(i);
        this._parsingContext.setCurrentName(nameStr);
        this._currToken = JsonToken.FIELD_NAME;
        int i2 = _skipColon();
        if (i2 == 34) {
            this._tokenIncomplete = true;
            this._nextToken = JsonToken.VALUE_STRING;
            return nameStr;
        }
        if (i2 == 45) {
            t = _parseNegNumber();
        } else if (i2 == 91) {
            t = JsonToken.START_ARRAY;
        } else if (i2 == 102) {
            _matchToken("false", 1);
            t = JsonToken.VALUE_FALSE;
        } else if (i2 == 110) {
            _matchToken("null", 1);
            t = JsonToken.VALUE_NULL;
        } else if (i2 == 116) {
            _matchToken("true", 1);
            t = JsonToken.VALUE_TRUE;
        } else if (i2 != 123) {
            switch (i2) {
                case 48:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                    t = _parsePosNumber(i2);
                    break;
                default:
                    t = _handleUnexpectedValue(i2);
                    break;
            }
        } else {
            t = JsonToken.START_OBJECT;
        }
        this._nextToken = t;
        return nameStr;
    }

    public String nextTextValue() throws IOException {
        String str = null;
        if (this._currToken == JsonToken.FIELD_NAME) {
            this._nameCopied = false;
            JsonToken t = this._nextToken;
            this._nextToken = null;
            this._currToken = t;
            if (t != JsonToken.VALUE_STRING) {
                if (t == JsonToken.START_ARRAY) {
                    this._parsingContext = this._parsingContext.createChildArrayContext(this._tokenInputRow, this._tokenInputCol);
                } else if (t == JsonToken.START_OBJECT) {
                    this._parsingContext = this._parsingContext.createChildObjectContext(this._tokenInputRow, this._tokenInputCol);
                }
                return null;
            } else if (!this._tokenIncomplete) {
                return this._textBuffer.contentsAsString();
            } else {
                this._tokenIncomplete = false;
                return _finishAndReturnString();
            }
        } else {
            if (nextToken() == JsonToken.VALUE_STRING) {
                str = getText();
            }
            return str;
        }
    }

    public int nextIntValue(int defaultValue) throws IOException {
        if (this._currToken == JsonToken.FIELD_NAME) {
            this._nameCopied = false;
            JsonToken t = this._nextToken;
            this._nextToken = null;
            this._currToken = t;
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return getIntValue();
            }
            if (t == JsonToken.START_ARRAY) {
                this._parsingContext = this._parsingContext.createChildArrayContext(this._tokenInputRow, this._tokenInputCol);
            } else if (t == JsonToken.START_OBJECT) {
                this._parsingContext = this._parsingContext.createChildObjectContext(this._tokenInputRow, this._tokenInputCol);
            }
            return defaultValue;
        }
        return nextToken() == JsonToken.VALUE_NUMBER_INT ? getIntValue() : defaultValue;
    }

    public long nextLongValue(long defaultValue) throws IOException {
        if (this._currToken == JsonToken.FIELD_NAME) {
            this._nameCopied = false;
            JsonToken t = this._nextToken;
            this._nextToken = null;
            this._currToken = t;
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return getLongValue();
            }
            if (t == JsonToken.START_ARRAY) {
                this._parsingContext = this._parsingContext.createChildArrayContext(this._tokenInputRow, this._tokenInputCol);
            } else if (t == JsonToken.START_OBJECT) {
                this._parsingContext = this._parsingContext.createChildObjectContext(this._tokenInputRow, this._tokenInputCol);
            }
            return defaultValue;
        }
        return nextToken() == JsonToken.VALUE_NUMBER_INT ? getLongValue() : defaultValue;
    }

    public Boolean nextBooleanValue() throws IOException {
        if (this._currToken == JsonToken.FIELD_NAME) {
            this._nameCopied = false;
            JsonToken t = this._nextToken;
            this._nextToken = null;
            this._currToken = t;
            if (t == JsonToken.VALUE_TRUE) {
                return Boolean.TRUE;
            }
            if (t == JsonToken.VALUE_FALSE) {
                return Boolean.FALSE;
            }
            if (t == JsonToken.START_ARRAY) {
                this._parsingContext = this._parsingContext.createChildArrayContext(this._tokenInputRow, this._tokenInputCol);
            } else if (t == JsonToken.START_OBJECT) {
                this._parsingContext = this._parsingContext.createChildObjectContext(this._tokenInputRow, this._tokenInputCol);
            }
            return null;
        }
        JsonToken t2 = nextToken();
        if (t2 == JsonToken.VALUE_TRUE) {
            return Boolean.TRUE;
        }
        if (t2 == JsonToken.VALUE_FALSE) {
            return Boolean.FALSE;
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public JsonToken _parsePosNumber(int c) throws IOException {
        int c2;
        int outPtr;
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        if (c == 48) {
            c2 = _handleLeadingZeroes();
            if (c2 > 57 || c2 < 48) {
                outBuf[0] = '0';
                outPtr = 1;
            } else {
                outPtr = 0;
            }
        } else {
            outBuf[0] = (char) c;
            c2 = this._inputData.readUnsignedByte();
            outPtr = 1;
        }
        int outPtr2 = outPtr;
        int intLen = outPtr;
        while (c2 <= 57 && c2 >= 48) {
            intLen++;
            int outPtr3 = outPtr2 + 1;
            outBuf[outPtr2] = (char) c2;
            c2 = this._inputData.readUnsignedByte();
            outPtr2 = outPtr3;
        }
        if (c2 == 46 || c2 == 101 || c2 == 69) {
            return _parseFloat(outBuf, outPtr2, c2, false, intLen);
        }
        this._textBuffer.setCurrentLength(outPtr2);
        if (this._parsingContext.inRoot()) {
            _verifyRootSpace();
        } else {
            this._nextByte = c2;
        }
        return resetInt(false, intLen);
    }

    /* access modifiers changed from: protected */
    public JsonToken _parseNegNumber() throws IOException {
        int c;
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        int outPtr = 0 + 1;
        outBuf[0] = '-';
        int c2 = this._inputData.readUnsignedByte();
        int outPtr2 = outPtr + 1;
        outBuf[outPtr] = (char) c2;
        if (c2 <= 48) {
            if (c2 != 48) {
                return _handleInvalidNumberStart(c2, true);
            }
            c = _handleLeadingZeroes();
        } else if (c2 > 57) {
            return _handleInvalidNumberStart(c2, true);
        } else {
            c = this._inputData.readUnsignedByte();
        }
        int c3 = c;
        int outPtr3 = outPtr2;
        int intLen = 1;
        while (c3 <= 57 && c3 >= 48) {
            intLen++;
            int outPtr4 = outPtr3 + 1;
            outBuf[outPtr3] = (char) c3;
            c3 = this._inputData.readUnsignedByte();
            outPtr3 = outPtr4;
        }
        if (c3 == 46 || c3 == 101 || c3 == 69) {
            return _parseFloat(outBuf, outPtr3, c3, true, intLen);
        }
        this._textBuffer.setCurrentLength(outPtr3);
        this._nextByte = c3;
        if (this._parsingContext.inRoot()) {
            _verifyRootSpace();
        }
        return resetInt(true, intLen);
    }

    private final int _handleLeadingZeroes() throws IOException {
        int ch = this._inputData.readUnsignedByte();
        if (ch < 48 || ch > 57) {
            return ch;
        }
        if (!isEnabled(Feature.ALLOW_NUMERIC_LEADING_ZEROS)) {
            reportInvalidNumber("Leading zeroes not allowed");
        }
        while (ch == 48) {
            ch = this._inputData.readUnsignedByte();
        }
        return ch;
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x002f  */
    private final JsonToken _parseFloat(char[] outBuf, int outPtr, int outPtr2, boolean negative, int integerPartLength) throws IOException {
        int fractLen = 0;
        if (outPtr2 == 46) {
            int outPtr3 = outPtr + 1;
            outBuf[outPtr] = (char) outPtr2;
            while (true) {
                outPtr = outPtr3;
                outPtr2 = this._inputData.readUnsignedByte();
                if (outPtr2 >= 48 && outPtr2 <= 57) {
                    fractLen++;
                    if (outPtr >= outBuf.length) {
                        outBuf = this._textBuffer.finishCurrentSegment();
                        outPtr = 0;
                    }
                    outPtr3 = outPtr + 1;
                    outBuf[outPtr] = (char) outPtr2;
                } else if (fractLen == 0) {
                    reportUnexpectedNumberChar(outPtr2, "Decimal point not followed by a digit");
                }
            }
            if (fractLen == 0) {
            }
        }
        int expLen = 0;
        if (outPtr == 101 || outPtr == 69) {
            if (outPtr >= outBuf.length) {
                outBuf = this._textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            int outPtr4 = outPtr + 1;
            outBuf[outPtr] = (char) outPtr;
            int c = this._inputData.readUnsignedByte();
            if (c == 45 || c == 43) {
                if (outPtr4 >= outBuf.length) {
                    outBuf = this._textBuffer.finishCurrentSegment();
                    outPtr4 = 0;
                }
                int outPtr5 = outPtr4 + 1;
                outBuf[outPtr4] = (char) c;
                int i = outPtr5;
                outPtr = this._inputData.readUnsignedByte();
                outPtr = i;
            } else {
                outPtr = c;
                outPtr = outPtr4;
            }
            while (outPtr <= 57 && outPtr >= 48) {
                expLen++;
                if (outPtr >= outBuf.length) {
                    outBuf = this._textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                int outPtr6 = outPtr + 1;
                outBuf[outPtr] = (char) outPtr;
                outPtr = this._inputData.readUnsignedByte();
                outPtr = outPtr6;
            }
            if (expLen == 0) {
                reportUnexpectedNumberChar(outPtr, "Exponent indicator not followed by a digit");
            }
        }
        this._nextByte = outPtr;
        if (this._parsingContext.inRoot()) {
            _verifyRootSpace();
        }
        this._textBuffer.setCurrentLength(outPtr);
        return resetFloat(negative, integerPartLength, fractLen, expLen);
    }

    private final void _verifyRootSpace() throws IOException {
        int ch = this._nextByte;
        if (ch <= 32) {
            this._nextByte = -1;
            if (ch == 13 || ch == 10) {
                this._currInputRow++;
            }
            return;
        }
        _reportMissingRootWS(ch);
    }

    /* access modifiers changed from: protected */
    public final String _parseName(int i) throws IOException {
        if (i != 34) {
            return _handleOddName(i);
        }
        int[] codes = _icLatin1;
        int q = this._inputData.readUnsignedByte();
        if (codes[q] == 0) {
            int i2 = this._inputData.readUnsignedByte();
            if (codes[i2] == 0) {
                int q2 = (q << 8) | i2;
                int i3 = this._inputData.readUnsignedByte();
                if (codes[i3] == 0) {
                    int q3 = (q2 << 8) | i3;
                    int i4 = this._inputData.readUnsignedByte();
                    if (codes[i4] == 0) {
                        int q4 = (q3 << 8) | i4;
                        int i5 = this._inputData.readUnsignedByte();
                        if (codes[i5] == 0) {
                            this._quad1 = q4;
                            return _parseMediumName(i5);
                        } else if (i5 == 34) {
                            return findName(q4, 4);
                        } else {
                            return parseName(q4, i5, 4);
                        }
                    } else if (i4 == 34) {
                        return findName(q3, 3);
                    } else {
                        return parseName(q3, i4, 3);
                    }
                } else if (i3 == 34) {
                    return findName(q2, 2);
                } else {
                    return parseName(q2, i3, 2);
                }
            } else if (i2 == 34) {
                return findName(q, 1);
            } else {
                return parseName(q, i2, 1);
            }
        } else if (q == 34) {
            return "";
        } else {
            return parseName(0, q, 0);
        }
    }

    private final String _parseMediumName(int q2) throws IOException {
        int[] codes = _icLatin1;
        int i = this._inputData.readUnsignedByte();
        if (codes[i] == 0) {
            int q22 = (q2 << 8) | i;
            int i2 = this._inputData.readUnsignedByte();
            if (codes[i2] == 0) {
                int q23 = (q22 << 8) | i2;
                int i3 = this._inputData.readUnsignedByte();
                if (codes[i3] == 0) {
                    int q24 = (q23 << 8) | i3;
                    int i4 = this._inputData.readUnsignedByte();
                    if (codes[i4] == 0) {
                        return _parseMediumName2(i4, q24);
                    }
                    if (i4 == 34) {
                        return findName(this._quad1, q24, 4);
                    }
                    return parseName(this._quad1, q24, i4, 4);
                } else if (i3 == 34) {
                    return findName(this._quad1, q23, 3);
                } else {
                    return parseName(this._quad1, q23, i3, 3);
                }
            } else if (i2 == 34) {
                return findName(this._quad1, q22, 2);
            } else {
                return parseName(this._quad1, q22, i2, 2);
            }
        } else if (i == 34) {
            return findName(this._quad1, q2, 1);
        } else {
            return parseName(this._quad1, q2, i, 1);
        }
    }

    private final String _parseMediumName2(int q3, int q2) throws IOException {
        int[] codes = _icLatin1;
        int i = this._inputData.readUnsignedByte();
        if (codes[i] == 0) {
            int q32 = (q3 << 8) | i;
            int i2 = this._inputData.readUnsignedByte();
            if (codes[i2] == 0) {
                int q33 = (q32 << 8) | i2;
                int i3 = this._inputData.readUnsignedByte();
                if (codes[i3] == 0) {
                    int q34 = (q33 << 8) | i3;
                    int i4 = this._inputData.readUnsignedByte();
                    if (codes[i4] == 0) {
                        return _parseLongName(i4, q2, q34);
                    }
                    if (i4 == 34) {
                        return findName(this._quad1, q2, q34, 4);
                    }
                    return parseName(this._quad1, q2, q34, i4, 4);
                } else if (i3 == 34) {
                    return findName(this._quad1, q2, q33, 3);
                } else {
                    return parseName(this._quad1, q2, q33, i3, 3);
                }
            } else if (i2 == 34) {
                return findName(this._quad1, q2, q32, 2);
            } else {
                return parseName(this._quad1, q2, q32, i2, 2);
            }
        } else if (i == 34) {
            return findName(this._quad1, q2, q3, 1);
        } else {
            return parseName(this._quad1, q2, q3, i, 1);
        }
    }

    private final String _parseLongName(int q, int q2, int q3) throws IOException {
        int[] iArr = this._quadBuffer;
        iArr[0] = this._quad1;
        iArr[1] = q2;
        iArr[2] = q3;
        int[] codes = _icLatin1;
        int qlen = 3;
        while (true) {
            int i = this._inputData.readUnsignedByte();
            if (codes[i] == 0) {
                int q4 = (q << 8) | i;
                int i2 = this._inputData.readUnsignedByte();
                if (codes[i2] == 0) {
                    int q5 = (q4 << 8) | i2;
                    int i3 = this._inputData.readUnsignedByte();
                    if (codes[i3] == 0) {
                        int q6 = (q5 << 8) | i3;
                        int i4 = this._inputData.readUnsignedByte();
                        if (codes[i4] == 0) {
                            int[] iArr2 = this._quadBuffer;
                            if (qlen >= iArr2.length) {
                                this._quadBuffer = _growArrayBy(iArr2, qlen);
                            }
                            int qlen2 = qlen + 1;
                            this._quadBuffer[qlen] = q6;
                            q = i4;
                            qlen = qlen2;
                        } else if (i4 == 34) {
                            return findName(this._quadBuffer, qlen, q6, 4);
                        } else {
                            return parseEscapedName(this._quadBuffer, qlen, q6, i4, 4);
                        }
                    } else if (i3 == 34) {
                        return findName(this._quadBuffer, qlen, q5, 3);
                    } else {
                        return parseEscapedName(this._quadBuffer, qlen, q5, i3, 3);
                    }
                } else if (i2 == 34) {
                    return findName(this._quadBuffer, qlen, q4, 2);
                } else {
                    return parseEscapedName(this._quadBuffer, qlen, q4, i2, 2);
                }
            } else if (i == 34) {
                return findName(this._quadBuffer, qlen, q, 1);
            } else {
                return parseEscapedName(this._quadBuffer, qlen, q, i, 1);
            }
        }
    }

    private final String parseName(int q1, int ch, int lastQuadBytes) throws IOException {
        return parseEscapedName(this._quadBuffer, 0, q1, ch, lastQuadBytes);
    }

    private final String parseName(int q1, int q2, int ch, int lastQuadBytes) throws IOException {
        int[] iArr = this._quadBuffer;
        iArr[0] = q1;
        return parseEscapedName(iArr, 1, q2, ch, lastQuadBytes);
    }

    private final String parseName(int q1, int q2, int q3, int ch, int lastQuadBytes) throws IOException {
        int[] iArr = this._quadBuffer;
        iArr[0] = q1;
        iArr[1] = q2;
        return parseEscapedName(iArr, 2, q3, ch, lastQuadBytes);
    }

    /* access modifiers changed from: protected */
    public final String parseEscapedName(int[] quads, int qlen, int currQuad, int ch, int currQuadBytes) throws IOException {
        int[] codes = _icLatin1;
        while (true) {
            if (codes[ch] != 0) {
                if (ch == 34) {
                    break;
                }
                if (ch != 92) {
                    _throwUnquotedSpace(ch, "name");
                } else {
                    ch = _decodeEscaped();
                }
                if (ch > 127) {
                    if (currQuadBytes >= 4) {
                        if (qlen >= quads.length) {
                            int[] _growArrayBy = _growArrayBy(quads, quads.length);
                            quads = _growArrayBy;
                            this._quadBuffer = _growArrayBy;
                        }
                        int qlen2 = qlen + 1;
                        quads[qlen] = currQuad;
                        currQuad = 0;
                        currQuadBytes = 0;
                        qlen = qlen2;
                    }
                    if (ch < 2048) {
                        currQuad = (currQuad << 8) | (ch >> 6) | PsExtractor.AUDIO_STREAM;
                        currQuadBytes++;
                    } else {
                        int currQuad2 = (currQuad << 8) | (ch >> 12) | 224;
                        int currQuadBytes2 = currQuadBytes + 1;
                        if (currQuadBytes2 >= 4) {
                            if (qlen >= quads.length) {
                                int[] _growArrayBy2 = _growArrayBy(quads, quads.length);
                                quads = _growArrayBy2;
                                this._quadBuffer = _growArrayBy2;
                            }
                            int qlen3 = qlen + 1;
                            quads[qlen] = currQuad2;
                            currQuad2 = 0;
                            currQuadBytes2 = 0;
                            qlen = qlen3;
                        }
                        currQuad = (currQuad2 << 8) | ((ch >> 6) & 63) | 128;
                        currQuadBytes = currQuadBytes2 + 1;
                    }
                    ch = (ch & 63) | 128;
                }
            }
            if (currQuadBytes < 4) {
                currQuadBytes = currQuadBytes + 1;
                currQuad = (currQuad << 8) | ch;
            } else {
                if (qlen >= quads.length) {
                    int[] _growArrayBy3 = _growArrayBy(quads, quads.length);
                    quads = _growArrayBy3;
                    this._quadBuffer = _growArrayBy3;
                }
                int qlen4 = qlen + 1;
                quads[qlen] = currQuad;
                currQuadBytes = 1;
                currQuad = ch;
                qlen = qlen4;
            }
            ch = this._inputData.readUnsignedByte();
        }
        if (currQuadBytes > 0) {
            if (qlen >= quads.length) {
                int[] _growArrayBy4 = _growArrayBy(quads, quads.length);
                quads = _growArrayBy4;
                this._quadBuffer = _growArrayBy4;
            }
            int qlen5 = qlen + 1;
            quads[qlen] = pad(currQuad, currQuadBytes);
            qlen = qlen5;
        }
        String name = this._symbols.findName(quads, qlen);
        if (name == null) {
            return addName(quads, qlen, currQuadBytes);
        }
        return name;
    }

    /* access modifiers changed from: protected */
    public String _handleOddName(int ch) throws IOException {
        if (ch == 39 && isEnabled(Feature.ALLOW_SINGLE_QUOTES)) {
            return _parseAposName();
        }
        if (!isEnabled(Feature.ALLOW_UNQUOTED_FIELD_NAMES)) {
            _reportUnexpectedChar((char) _decodeCharForError(ch), "was expecting double-quote to start field name");
        }
        int[] codes = CharTypes.getInputCodeUtf8JsNames();
        if (codes[ch] != 0) {
            _reportUnexpectedChar(ch, "was expecting either valid name character (for unquoted name) or double-quote (for quoted) to start field name");
        }
        int[] quads = this._quadBuffer;
        int currQuad = 0;
        int currQuad2 = 0;
        int currQuadBytes = 0;
        do {
            if (currQuadBytes < 4) {
                currQuadBytes++;
                currQuad2 = (currQuad2 << 8) | ch;
            } else {
                if (currQuad >= quads.length) {
                    int[] _growArrayBy = _growArrayBy(quads, quads.length);
                    quads = _growArrayBy;
                    this._quadBuffer = _growArrayBy;
                }
                int qlen = currQuad + 1;
                quads[currQuad] = currQuad2;
                currQuadBytes = 1;
                currQuad2 = ch;
                currQuad = qlen;
            }
            ch = this._inputData.readUnsignedByte();
        } while (codes[ch] == 0);
        this._nextByte = ch;
        if (currQuadBytes > 0) {
            if (currQuad >= quads.length) {
                int[] _growArrayBy2 = _growArrayBy(quads, quads.length);
                quads = _growArrayBy2;
                this._quadBuffer = _growArrayBy2;
            }
            int qlen2 = currQuad + 1;
            quads[currQuad] = currQuad2;
            currQuad = qlen2;
        }
        String name = this._symbols.findName(quads, currQuad);
        if (name == null) {
            name = addName(quads, currQuad, currQuadBytes);
        }
        return name;
    }

    /* access modifiers changed from: protected */
    public String _parseAposName() throws IOException {
        int currQuadBytes;
        int currQuad;
        int qlen;
        int ch = this._inputData.readUnsignedByte();
        if (ch == 39) {
            return "";
        }
        int[] quads = this._quadBuffer;
        int qlen2 = 0;
        int currQuad2 = 0;
        int currQuadBytes2 = 0;
        int[] codes = _icLatin1;
        while (ch != 39) {
            if (!(ch == 34 || codes[ch] == 0)) {
                if (ch != 92) {
                    _throwUnquotedSpace(ch, "name");
                } else {
                    ch = _decodeEscaped();
                }
                if (ch > 127) {
                    if (currQuadBytes >= 4) {
                        if (qlen2 >= quads.length) {
                            int[] _growArrayBy = _growArrayBy(quads, quads.length);
                            quads = _growArrayBy;
                            this._quadBuffer = _growArrayBy;
                        }
                        int qlen3 = qlen2 + 1;
                        quads[qlen2] = currQuad;
                        currQuad = 0;
                        currQuadBytes = 0;
                        qlen2 = qlen3;
                    }
                    if (ch < 2048) {
                        currQuad = (currQuad << 8) | (ch >> 6) | PsExtractor.AUDIO_STREAM;
                        currQuadBytes++;
                    } else {
                        int currQuad3 = (currQuad << 8) | (ch >> 12) | 224;
                        int currQuadBytes3 = currQuadBytes + 1;
                        if (currQuadBytes3 >= 4) {
                            if (qlen2 >= quads.length) {
                                int[] _growArrayBy2 = _growArrayBy(quads, quads.length);
                                quads = _growArrayBy2;
                                this._quadBuffer = _growArrayBy2;
                            }
                            int qlen4 = qlen2 + 1;
                            quads[qlen2] = currQuad3;
                            currQuad3 = 0;
                            currQuadBytes3 = 0;
                            qlen2 = qlen4;
                        }
                        currQuad = (currQuad3 << 8) | ((ch >> 6) & 63) | 128;
                        currQuadBytes = currQuadBytes3 + 1;
                    }
                    ch = (ch & 63) | 128;
                }
            }
            if (currQuadBytes < 4) {
                currQuadBytes2 = currQuadBytes + 1;
                currQuad2 = (currQuad << 8) | ch;
            } else {
                if (qlen2 >= quads.length) {
                    int[] _growArrayBy3 = _growArrayBy(quads, quads.length);
                    quads = _growArrayBy3;
                    this._quadBuffer = _growArrayBy3;
                }
                int qlen5 = qlen2 + 1;
                quads[qlen2] = currQuad;
                currQuadBytes2 = 1;
                currQuad2 = ch;
                qlen2 = qlen5;
            }
            ch = this._inputData.readUnsignedByte();
        }
        if (currQuadBytes > 0) {
            if (qlen2 >= quads.length) {
                int[] _growArrayBy4 = _growArrayBy(quads, quads.length);
                quads = _growArrayBy4;
                this._quadBuffer = _growArrayBy4;
            }
            qlen = qlen2 + 1;
            quads[qlen2] = pad(currQuad, currQuadBytes);
        } else {
            qlen = qlen2;
        }
        String name = this._symbols.findName(quads, qlen);
        if (name == null) {
            name = addName(quads, qlen, currQuadBytes);
        }
        return name;
    }

    private final String findName(int q1, int lastQuadBytes) throws JsonParseException {
        int q12 = pad(q1, lastQuadBytes);
        String name = this._symbols.findName(q12);
        if (name != null) {
            return name;
        }
        int[] iArr = this._quadBuffer;
        iArr[0] = q12;
        return addName(iArr, 1, lastQuadBytes);
    }

    private final String findName(int q1, int q2, int lastQuadBytes) throws JsonParseException {
        int q22 = pad(q2, lastQuadBytes);
        String name = this._symbols.findName(q1, q22);
        if (name != null) {
            return name;
        }
        int[] iArr = this._quadBuffer;
        iArr[0] = q1;
        iArr[1] = q22;
        return addName(iArr, 2, lastQuadBytes);
    }

    private final String findName(int q1, int q2, int q3, int lastQuadBytes) throws JsonParseException {
        int q32 = pad(q3, lastQuadBytes);
        String name = this._symbols.findName(q1, q2, q32);
        if (name != null) {
            return name;
        }
        int[] quads = this._quadBuffer;
        quads[0] = q1;
        quads[1] = q2;
        quads[2] = pad(q32, lastQuadBytes);
        return addName(quads, 3, lastQuadBytes);
    }

    private final String findName(int[] quads, int qlen, int lastQuad, int lastQuadBytes) throws JsonParseException {
        if (qlen >= quads.length) {
            int[] _growArrayBy = _growArrayBy(quads, quads.length);
            quads = _growArrayBy;
            this._quadBuffer = _growArrayBy;
        }
        int qlen2 = qlen + 1;
        quads[qlen] = pad(lastQuad, lastQuadBytes);
        String name = this._symbols.findName(quads, qlen2);
        if (name == null) {
            return addName(quads, qlen2, lastQuadBytes);
        }
        return name;
    }

    private final String addName(int[] quads, int qlen, int lastQuadBytes) throws JsonParseException {
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

    /* access modifiers changed from: protected */
    public void _finishString() throws IOException {
        int outPtr = 0;
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        int[] codes = _icUTF8;
        int outEnd = outBuf.length;
        while (true) {
            int c = this._inputData.readUnsignedByte();
            if (codes[c] == 0) {
                int outPtr2 = outPtr + 1;
                outBuf[outPtr] = (char) c;
                if (outPtr2 >= outEnd) {
                    _finishString2(outBuf, outPtr2, this._inputData.readUnsignedByte());
                    return;
                }
                outPtr = outPtr2;
            } else if (c == 34) {
                this._textBuffer.setCurrentLength(outPtr);
                return;
            } else {
                _finishString2(outBuf, outPtr, c);
                return;
            }
        }
    }

    private String _finishAndReturnString() throws IOException {
        int outPtr = 0;
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        int[] codes = _icUTF8;
        int outEnd = outBuf.length;
        while (true) {
            int c = this._inputData.readUnsignedByte();
            if (codes[c] == 0) {
                int outPtr2 = outPtr + 1;
                outBuf[outPtr] = (char) c;
                if (outPtr2 >= outEnd) {
                    _finishString2(outBuf, outPtr2, this._inputData.readUnsignedByte());
                    return this._textBuffer.contentsAsString();
                }
                outPtr = outPtr2;
            } else if (c == 34) {
                return this._textBuffer.setCurrentAndReturn(outPtr);
            } else {
                _finishString2(outBuf, outPtr, c);
                return this._textBuffer.contentsAsString();
            }
        }
    }

    private final void _finishString2(char[] outBuf, int outPtr, int c) throws IOException {
        int[] codes = _icUTF8;
        int outEnd = outBuf.length;
        while (true) {
            if (codes[c] == 0) {
                if (outPtr >= outEnd) {
                    outBuf = this._textBuffer.finishCurrentSegment();
                    outPtr = 0;
                    outEnd = outBuf.length;
                }
                int outPtr2 = outPtr + 1;
                outBuf[outPtr] = (char) c;
                c = this._inputData.readUnsignedByte();
                outPtr = outPtr2;
            } else if (c == 34) {
                this._textBuffer.setCurrentLength(outPtr);
                return;
            } else {
                switch (codes[c]) {
                    case 1:
                        c = _decodeEscaped();
                        break;
                    case 2:
                        c = _decodeUtf8_2(c);
                        break;
                    case 3:
                        c = _decodeUtf8_3(c);
                        break;
                    case 4:
                        int c2 = _decodeUtf8_4(c);
                        int outPtr3 = outPtr + 1;
                        outBuf[outPtr] = (char) (55296 | (c2 >> 10));
                        if (outPtr3 >= outBuf.length) {
                            outBuf = this._textBuffer.finishCurrentSegment();
                            outPtr = 0;
                            outEnd = outBuf.length;
                        } else {
                            outPtr = outPtr3;
                        }
                        c = (c2 & 1023) | GeneratorBase.SURR2_FIRST;
                        break;
                    default:
                        if (c >= 32) {
                            _reportInvalidChar(c);
                            break;
                        } else {
                            _throwUnquotedSpace(c, "string value");
                            break;
                        }
                }
                if (outPtr >= outBuf.length) {
                    outBuf = this._textBuffer.finishCurrentSegment();
                    outPtr = 0;
                    outEnd = outBuf.length;
                }
                int outPtr4 = outPtr + 1;
                outBuf[outPtr] = (char) c;
                c = this._inputData.readUnsignedByte();
                outPtr = outPtr4;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void _skipString() throws IOException {
        this._tokenIncomplete = false;
        int[] codes = _icUTF8;
        while (true) {
            int c = this._inputData.readUnsignedByte();
            if (codes[c] != 0) {
                if (c != 34) {
                    switch (codes[c]) {
                        case 1:
                            _decodeEscaped();
                            break;
                        case 2:
                            _skipUtf8_2();
                            break;
                        case 3:
                            _skipUtf8_3();
                            break;
                        case 4:
                            _skipUtf8_4();
                            break;
                        default:
                            if (c >= 32) {
                                _reportInvalidChar(c);
                                break;
                            } else {
                                _throwUnquotedSpace(c, "string value");
                                break;
                            }
                    }
                } else {
                    return;
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x002b, code lost:
        if (r3._parsingContext.inArray() == false) goto L_0x0086;
     */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x008c  */
    public JsonToken _handleUnexpectedValue(int c) throws IOException {
        if (c != 39) {
            if (c == 73) {
                _matchToken("Infinity", 1);
                if (isEnabled(Feature.ALLOW_NON_NUMERIC_NUMBERS)) {
                    return resetAsNaN("Infinity", Double.POSITIVE_INFINITY);
                }
                _reportError("Non-standard token 'Infinity': enable JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS to allow");
            } else if (c != 78) {
                if (c != 93) {
                    if (c != 125) {
                        switch (c) {
                            case 43:
                                return _handleInvalidNumberStart(this._inputData.readUnsignedByte(), false);
                            case 44:
                                break;
                        }
                    }
                    _reportUnexpectedChar(c, "expected a value");
                }
                if (isEnabled(Feature.ALLOW_MISSING_VALUES)) {
                    this._nextByte = c;
                    return JsonToken.VALUE_NULL;
                }
                _reportUnexpectedChar(c, "expected a value");
            } else {
                _matchToken("NaN", 1);
                if (isEnabled(Feature.ALLOW_NON_NUMERIC_NUMBERS)) {
                    return resetAsNaN("NaN", Double.NaN);
                }
                _reportError("Non-standard token 'NaN': enable JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS to allow");
            }
            if (Character.isJavaIdentifierStart(c)) {
                StringBuilder sb = new StringBuilder();
                sb.append("");
                sb.append((char) c);
                _reportInvalidToken(c, sb.toString(), "('true', 'false' or 'null')");
            }
            _reportUnexpectedChar(c, "expected a valid value (number, String, array, object, 'true', 'false' or 'null')");
            return null;
        }
        if (isEnabled(Feature.ALLOW_SINGLE_QUOTES)) {
            return _handleApos();
        }
        if (Character.isJavaIdentifierStart(c)) {
        }
        _reportUnexpectedChar(c, "expected a valid value (number, String, array, object, 'true', 'false' or 'null')");
        return null;
    }

    /* access modifiers changed from: protected */
    public JsonToken _handleApos() throws IOException {
        int outPtr;
        int outPtr2 = 0;
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        int[] codes = _icUTF8;
        while (true) {
            int outEnd = outBuf.length;
            if (outPtr2 >= outBuf.length) {
                outBuf = this._textBuffer.finishCurrentSegment();
                outPtr2 = 0;
                outEnd = outBuf.length;
            }
            while (true) {
                int c = this._inputData.readUnsignedByte();
                if (c == 39) {
                    this._textBuffer.setCurrentLength(outPtr);
                    return JsonToken.VALUE_STRING;
                } else if (codes[c] != 0) {
                    switch (codes[c]) {
                        case 1:
                            c = _decodeEscaped();
                            break;
                        case 2:
                            c = _decodeUtf8_2(c);
                            break;
                        case 3:
                            c = _decodeUtf8_3(c);
                            break;
                        case 4:
                            int c2 = _decodeUtf8_4(c);
                            int outPtr3 = outPtr + 1;
                            outBuf[outPtr] = (char) (55296 | (c2 >> 10));
                            if (outPtr3 >= outBuf.length) {
                                outBuf = this._textBuffer.finishCurrentSegment();
                                outPtr = 0;
                            } else {
                                outPtr = outPtr3;
                            }
                            c = (c2 & 1023) | GeneratorBase.SURR2_FIRST;
                            break;
                        default:
                            if (c < 32) {
                                _throwUnquotedSpace(c, "string value");
                            }
                            _reportInvalidChar(c);
                            break;
                    }
                    if (outPtr >= outBuf.length) {
                        outBuf = this._textBuffer.finishCurrentSegment();
                        outPtr = 0;
                    }
                    int outPtr4 = outPtr + 1;
                    outBuf[outPtr] = (char) c;
                    outPtr2 = outPtr4;
                } else {
                    int outPtr5 = outPtr + 1;
                    outBuf[outPtr] = (char) c;
                    if (outPtr5 >= outEnd) {
                        outPtr2 = outPtr5;
                    } else {
                        outPtr2 = outPtr5;
                    }
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public JsonToken _handleInvalidNumberStart(int ch, boolean neg) throws IOException {
        String match;
        while (ch == 73) {
            ch = this._inputData.readUnsignedByte();
            if (ch != 78) {
                if (ch != 110) {
                    break;
                }
                match = neg ? "-Infinity" : "+Infinity";
            } else {
                match = neg ? "-INF" : "+INF";
            }
            _matchToken(match, 3);
            if (isEnabled(Feature.ALLOW_NON_NUMERIC_NUMBERS)) {
                return resetAsNaN(match, neg ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Non-standard token '");
            sb.append(match);
            sb.append("': enable JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS to allow");
            _reportError(sb.toString());
        }
        reportUnexpectedNumberChar(ch, "expected digit (0-9) to follow minus sign, for valid numeric value");
        return null;
    }

    /* access modifiers changed from: protected */
    public final void _matchToken(String matchStr, int i) throws IOException {
        int len = matchStr.length();
        do {
            int ch = this._inputData.readUnsignedByte();
            if (ch != matchStr.charAt(i)) {
                _reportInvalidToken(ch, matchStr.substring(0, i));
            }
            i++;
        } while (i < len);
        int ch2 = this._inputData.readUnsignedByte();
        if (!(ch2 < 48 || ch2 == 93 || ch2 == 125)) {
            _checkMatchEnd(matchStr, i, ch2);
        }
        this._nextByte = ch2;
    }

    private final void _checkMatchEnd(String matchStr, int i, int ch) throws IOException {
        char c = (char) _decodeCharForError(ch);
        if (Character.isJavaIdentifierPart(c)) {
            _reportInvalidToken(c, matchStr.substring(0, i));
        }
    }

    private final int _skipWS() throws IOException {
        int i = this._nextByte;
        if (i < 0) {
            i = this._inputData.readUnsignedByte();
        } else {
            this._nextByte = -1;
        }
        while (i <= 32) {
            if (i == 13 || i == 10) {
                this._currInputRow++;
            }
            i = this._inputData.readUnsignedByte();
        }
        if (i == 47 || i == 35) {
            return _skipWSComment(i);
        }
        return i;
    }

    private final int _skipWSOrEnd() throws IOException {
        int i = this._nextByte;
        if (i < 0) {
            try {
                i = this._inputData.readUnsignedByte();
            } catch (EOFException e) {
                return _eofAsNextChar();
            }
        } else {
            this._nextByte = -1;
        }
        while (i <= 32) {
            if (i == 13 || i == 10) {
                this._currInputRow++;
            }
            try {
                i = this._inputData.readUnsignedByte();
            } catch (EOFException e2) {
                return _eofAsNextChar();
            }
        }
        if (i == 47 || i == 35) {
            return _skipWSComment(i);
        }
        return i;
    }

    private final int _skipWSComment(int i) throws IOException {
        while (true) {
            if (i > 32) {
                if (i == 47) {
                    _skipComment();
                } else if (i != 35 || !_skipYAMLComment()) {
                    return i;
                }
            } else if (i == 13 || i == 10) {
                this._currInputRow++;
            }
            i = this._inputData.readUnsignedByte();
        }
    }

    private final int _skipColon() throws IOException {
        int i = this._nextByte;
        if (i < 0) {
            i = this._inputData.readUnsignedByte();
        } else {
            this._nextByte = -1;
        }
        if (i == 58) {
            int i2 = this._inputData.readUnsignedByte();
            if (i2 <= 32) {
                if (i2 == 32 || i2 == 9) {
                    i2 = this._inputData.readUnsignedByte();
                    if (i2 > 32) {
                        if (i2 == 47 || i2 == 35) {
                            return _skipColon2(i2, true);
                        }
                        return i2;
                    }
                }
                return _skipColon2(i2, true);
            } else if (i2 == 47 || i2 == 35) {
                return _skipColon2(i2, true);
            } else {
                return i2;
            }
        } else {
            if (i == 32 || i == 9) {
                i = this._inputData.readUnsignedByte();
            }
            if (i != 58) {
                return _skipColon2(i, false);
            }
            int i3 = this._inputData.readUnsignedByte();
            if (i3 <= 32) {
                if (i3 == 32 || i3 == 9) {
                    i3 = this._inputData.readUnsignedByte();
                    if (i3 > 32) {
                        if (i3 == 47 || i3 == 35) {
                            return _skipColon2(i3, true);
                        }
                        return i3;
                    }
                }
                return _skipColon2(i3, true);
            } else if (i3 == 47 || i3 == 35) {
                return _skipColon2(i3, true);
            } else {
                return i3;
            }
        }
    }

    private final int _skipColon2(int i, boolean gotColon) throws IOException {
        while (true) {
            if (i > 32) {
                if (i == 47) {
                    _skipComment();
                } else if (i != 35 || !_skipYAMLComment()) {
                    if (gotColon) {
                        return i;
                    }
                    if (i != 58) {
                        _reportUnexpectedChar(i, "was expecting a colon to separate field name and value");
                    }
                    gotColon = true;
                }
            } else if (i == 13 || i == 10) {
                this._currInputRow++;
            }
            i = this._inputData.readUnsignedByte();
        }
    }

    private final void _skipComment() throws IOException {
        if (!isEnabled(Feature.ALLOW_COMMENTS)) {
            _reportUnexpectedChar(47, "maybe a (non-standard) comment? (not recognized as one since Feature 'ALLOW_COMMENTS' not enabled for parser)");
        }
        int c = this._inputData.readUnsignedByte();
        if (c == 47) {
            _skipLine();
        } else if (c == 42) {
            _skipCComment();
        } else {
            _reportUnexpectedChar(c, "was expecting either '*' or '/' for a comment");
        }
    }

    private final void _skipCComment() throws IOException {
        int[] codes = CharTypes.getInputCodeComment();
        int i = this._inputData.readUnsignedByte();
        while (true) {
            int code = codes[i];
            if (code != 0) {
                if (code == 10 || code == 13) {
                    this._currInputRow++;
                } else if (code != 42) {
                    switch (code) {
                        case 2:
                            _skipUtf8_2();
                            break;
                        case 3:
                            _skipUtf8_3();
                            break;
                        case 4:
                            _skipUtf8_4();
                            break;
                        default:
                            _reportInvalidChar(i);
                            break;
                    }
                } else {
                    i = this._inputData.readUnsignedByte();
                    if (i == 47) {
                        return;
                    }
                }
            }
            i = this._inputData.readUnsignedByte();
        }
    }

    private final boolean _skipYAMLComment() throws IOException {
        if (!isEnabled(Feature.ALLOW_YAML_COMMENTS)) {
            return false;
        }
        _skipLine();
        return true;
    }

    private final void _skipLine() throws IOException {
        int[] codes = CharTypes.getInputCodeComment();
        while (true) {
            int i = this._inputData.readUnsignedByte();
            int code = codes[i];
            if (code != 0) {
                if (code == 10 || code == 13) {
                    this._currInputRow++;
                } else if (code != 42) {
                    switch (code) {
                        case 2:
                            _skipUtf8_2();
                            break;
                        case 3:
                            _skipUtf8_3();
                            break;
                        case 4:
                            _skipUtf8_4();
                            break;
                        default:
                            if (code >= 0) {
                                break;
                            } else {
                                _reportInvalidChar(i);
                                break;
                            }
                    }
                }
            }
        }
        this._currInputRow++;
    }

    /* access modifiers changed from: protected */
    public char _decodeEscaped() throws IOException {
        int c = this._inputData.readUnsignedByte();
        if (c == 34 || c == 47 || c == 92) {
            return (char) c;
        }
        if (c == 98) {
            return 8;
        }
        if (c == 102) {
            return 12;
        }
        if (c == 110) {
            return 10;
        }
        if (c == 114) {
            return CharUtils.CR;
        }
        switch (c) {
            case 116:
                return 9;
            case 117:
                int value = 0;
                for (int i = 0; i < 4; i++) {
                    int ch = this._inputData.readUnsignedByte();
                    int digit = CharTypes.charToHex(ch);
                    if (digit < 0) {
                        _reportUnexpectedChar(ch, "expected a hex-digit for character escape sequence");
                    }
                    value = (value << 4) | digit;
                }
                return (char) value;
            default:
                return _handleUnrecognizedCharacterEscape((char) _decodeCharForError(c));
        }
    }

    /* access modifiers changed from: protected */
    public int _decodeCharForError(int firstByte) throws IOException {
        int needed;
        int c = firstByte & 255;
        if (c <= 127) {
            return c;
        }
        if ((c & 224) == 192) {
            c &= 31;
            needed = 1;
        } else if ((c & PsExtractor.VIDEO_STREAM_MASK) == 224) {
            c &= 15;
            needed = 2;
        } else if ((c & 248) == 240) {
            c &= 7;
            needed = 3;
        } else {
            _reportInvalidInitial(c & 255);
            needed = 1;
        }
        int d = this._inputData.readUnsignedByte();
        if ((d & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d & 255);
        }
        int c2 = (c << 6) | (d & 63);
        if (needed <= 1) {
            return c2;
        }
        int d2 = this._inputData.readUnsignedByte();
        if ((d2 & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d2 & 255);
        }
        int c3 = (c2 << 6) | (d2 & 63);
        if (needed <= 2) {
            return c3;
        }
        int d3 = this._inputData.readUnsignedByte();
        if ((d3 & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d3 & 255);
        }
        return (c3 << 6) | (d3 & 63);
    }

    private final int _decodeUtf8_2(int c) throws IOException {
        int d = this._inputData.readUnsignedByte();
        if ((d & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d & 255);
        }
        return ((c & 31) << 6) | (d & 63);
    }

    private final int _decodeUtf8_3(int c1) throws IOException {
        int c12 = c1 & 15;
        int d = this._inputData.readUnsignedByte();
        if ((d & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d & 255);
        }
        int c = (c12 << 6) | (d & 63);
        int d2 = this._inputData.readUnsignedByte();
        if ((d2 & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d2 & 255);
        }
        return (c << 6) | (d2 & 63);
    }

    private final int _decodeUtf8_4(int c) throws IOException {
        int d = this._inputData.readUnsignedByte();
        if ((d & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d & 255);
        }
        int c2 = ((c & 7) << 6) | (d & 63);
        int d2 = this._inputData.readUnsignedByte();
        if ((d2 & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d2 & 255);
        }
        int c3 = (c2 << 6) | (d2 & 63);
        int d3 = this._inputData.readUnsignedByte();
        if ((d3 & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d3 & 255);
        }
        return ((c3 << 6) | (d3 & 63)) - 65536;
    }

    private final void _skipUtf8_2() throws IOException {
        int c = this._inputData.readUnsignedByte();
        if ((c & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(c & 255);
        }
    }

    private final void _skipUtf8_3() throws IOException {
        int c = this._inputData.readUnsignedByte();
        if ((c & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(c & 255);
        }
        int c2 = this._inputData.readUnsignedByte();
        if ((c2 & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(c2 & 255);
        }
    }

    private final void _skipUtf8_4() throws IOException {
        int d = this._inputData.readUnsignedByte();
        if ((d & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d & 255);
        }
        int d2 = this._inputData.readUnsignedByte();
        if ((d2 & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d2 & 255);
        }
        int d3 = this._inputData.readUnsignedByte();
        if ((d3 & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d3 & 255);
        }
    }

    /* access modifiers changed from: protected */
    public void _reportInvalidToken(int ch, String matchedPart) throws IOException {
        _reportInvalidToken(ch, matchedPart, "'null', 'true', 'false' or NaN");
    }

    /* access modifiers changed from: protected */
    public void _reportInvalidToken(int ch, String matchedPart, String msg) throws IOException {
        StringBuilder sb = new StringBuilder(matchedPart);
        while (true) {
            char c = (char) _decodeCharForError(ch);
            if (!Character.isJavaIdentifierPart(c)) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Unrecognized token '");
                sb2.append(sb.toString());
                sb2.append("': was expecting ");
                sb2.append(msg);
                _reportError(sb2.toString());
                return;
            }
            sb.append(c);
            ch = this._inputData.readUnsignedByte();
        }
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

    private void _reportInvalidOther(int mask) throws JsonParseException {
        StringBuilder sb = new StringBuilder();
        sb.append("Invalid UTF-8 middle byte 0x");
        sb.append(Integer.toHexString(mask));
        _reportError(sb.toString());
    }

    private static int[] _growArrayBy(int[] arr, int more) {
        if (arr == null) {
            return new int[more];
        }
        return Arrays.copyOf(arr, arr.length + more);
    }

    /* access modifiers changed from: protected */
    public final byte[] _decodeBase64(Base64Variant b64variant) throws IOException {
        ByteArrayBuilder builder = _getByteArrayBuilder();
        while (true) {
            int ch = this._inputData.readUnsignedByte();
            if (ch > 32) {
                int bits = b64variant.decodeBase64Char(ch);
                if (bits < 0) {
                    if (ch == 34) {
                        return builder.toByteArray();
                    }
                    bits = _decodeBase64Escape(b64variant, ch, 0);
                    if (bits < 0) {
                        continue;
                    }
                }
                int decodedData = bits;
                int ch2 = this._inputData.readUnsignedByte();
                int bits2 = b64variant.decodeBase64Char(ch2);
                if (bits2 < 0) {
                    bits2 = _decodeBase64Escape(b64variant, ch2, 1);
                }
                int decodedData2 = (decodedData << 6) | bits2;
                int ch3 = this._inputData.readUnsignedByte();
                int bits3 = b64variant.decodeBase64Char(ch3);
                if (bits3 < 0) {
                    if (bits3 != -2) {
                        if (ch3 != 34 || b64variant.usesPadding()) {
                            bits3 = _decodeBase64Escape(b64variant, ch3, 2);
                        } else {
                            builder.append(decodedData2 >> 4);
                            return builder.toByteArray();
                        }
                    }
                    if (bits3 == -2) {
                        int ch4 = this._inputData.readUnsignedByte();
                        if (b64variant.usesPaddingChar(ch4)) {
                            builder.append(decodedData2 >> 4);
                        } else {
                            StringBuilder sb = new StringBuilder();
                            sb.append("expected padding character '");
                            sb.append(b64variant.getPaddingChar());
                            sb.append("'");
                            throw reportInvalidBase64Char(b64variant, ch4, 3, sb.toString());
                        }
                    }
                }
                int decodedData3 = (decodedData2 << 6) | bits3;
                int ch5 = this._inputData.readUnsignedByte();
                int bits4 = b64variant.decodeBase64Char(ch5);
                if (bits4 < 0) {
                    if (bits4 != -2) {
                        if (ch5 != 34 || b64variant.usesPadding()) {
                            bits4 = _decodeBase64Escape(b64variant, ch5, 3);
                        } else {
                            builder.appendTwoBytes(decodedData3 >> 2);
                            return builder.toByteArray();
                        }
                    }
                    if (bits4 == -2) {
                        builder.appendTwoBytes(decodedData3 >> 2);
                    }
                }
                builder.appendThreeBytes((decodedData3 << 6) | bits4);
            }
        }
    }

    public JsonLocation getTokenLocation() {
        JsonLocation jsonLocation = new JsonLocation(_getSourceReference(), -1, -1, this._tokenInputRow, -1);
        return jsonLocation;
    }

    public JsonLocation getCurrentLocation() {
        JsonLocation jsonLocation = new JsonLocation(_getSourceReference(), -1, -1, this._currInputRow, -1);
        return jsonLocation;
    }

    private void _closeScope(int i) throws JsonParseException {
        if (i == 93) {
            if (!this._parsingContext.inArray()) {
                _reportMismatchedEndMarker(i, '}');
            }
            this._parsingContext = this._parsingContext.clearAndGetParent();
            this._currToken = JsonToken.END_ARRAY;
        }
        if (i == 125) {
            if (!this._parsingContext.inObject()) {
                _reportMismatchedEndMarker(i, ']');
            }
            this._parsingContext = this._parsingContext.clearAndGetParent();
            this._currToken = JsonToken.END_OBJECT;
        }
    }

    private static final int pad(int q, int bytes) {
        return bytes == 4 ? q : (-1 << (bytes << 3)) | q;
    }
}
