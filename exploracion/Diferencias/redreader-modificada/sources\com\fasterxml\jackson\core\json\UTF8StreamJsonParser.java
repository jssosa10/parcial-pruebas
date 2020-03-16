package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.google.android.exoplayer2.extractor.ts.PsExtractor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import org.apache.commons.lang3.CharUtils;

public class UTF8StreamJsonParser extends ParserBase {
    static final byte BYTE_LF = 10;
    protected static final int FEAT_MASK_TRAILING_COMMA = Feature.ALLOW_TRAILING_COMMA.getMask();
    protected static final int[] _icLatin1 = CharTypes.getInputCodeLatin1();
    private static final int[] _icUTF8 = CharTypes.getInputCodeUtf8();
    protected boolean _bufferRecyclable;
    protected byte[] _inputBuffer;
    protected InputStream _inputStream;
    protected int _nameStartCol;
    protected int _nameStartOffset;
    protected int _nameStartRow;
    protected ObjectCodec _objectCodec;
    private int _quad1;
    protected int[] _quadBuffer = new int[16];
    protected final ByteQuadsCanonicalizer _symbols;
    protected boolean _tokenIncomplete;

    public UTF8StreamJsonParser(IOContext ctxt, int features, InputStream in, ObjectCodec codec, ByteQuadsCanonicalizer sym, byte[] inputBuffer, int start, int end, boolean bufferRecyclable) {
        super(ctxt, features);
        this._inputStream = in;
        this._objectCodec = codec;
        this._symbols = sym;
        this._inputBuffer = inputBuffer;
        this._inputPtr = start;
        this._inputEnd = end;
        this._currInputRowStart = start;
        this._currInputProcessed = (long) (-start);
        this._bufferRecyclable = bufferRecyclable;
    }

    public ObjectCodec getCodec() {
        return this._objectCodec;
    }

    public void setCodec(ObjectCodec c) {
        this._objectCodec = c;
    }

    public int releaseBuffered(OutputStream out) throws IOException {
        int count = this._inputEnd - this._inputPtr;
        if (count < 1) {
            return 0;
        }
        out.write(this._inputBuffer, this._inputPtr, count);
        return count;
    }

    public Object getInputSource() {
        return this._inputStream;
    }

    /* access modifiers changed from: protected */
    public final boolean _loadMore() throws IOException {
        int bufSize = this._inputEnd;
        this._currInputProcessed += (long) this._inputEnd;
        this._currInputRowStart -= this._inputEnd;
        this._nameStartOffset -= bufSize;
        InputStream inputStream = this._inputStream;
        if (inputStream != null) {
            byte[] bArr = this._inputBuffer;
            int space = bArr.length;
            if (space == 0) {
                return false;
            }
            int count = inputStream.read(bArr, 0, space);
            if (count > 0) {
                this._inputPtr = 0;
                this._inputEnd = count;
                return true;
            }
            _closeInput();
            if (count == 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("InputStream.read() returned 0 characters when trying to read ");
                sb.append(this._inputBuffer.length);
                sb.append(" bytes");
                throw new IOException(sb.toString());
            }
        }
        return false;
    }

    /* access modifiers changed from: protected */
    public void _closeInput() throws IOException {
        if (this._inputStream != null) {
            if (this._ioContext.isResourceManaged() || isEnabled(Feature.AUTO_CLOSE_SOURCE)) {
                this._inputStream.close();
            }
            this._inputStream = null;
        }
    }

    /* access modifiers changed from: protected */
    public void _releaseBuffers() throws IOException {
        super._releaseBuffers();
        this._symbols.release();
        if (this._bufferRecyclable) {
            byte[] buf = this._inputBuffer;
            if (buf != null) {
                this._inputBuffer = NO_BYTES;
                this._ioContext.releaseReadIOBuffer(buf);
            }
        }
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
        if (this._currToken == null) {
            return 0;
        }
        switch (this._currToken.id()) {
            case 5:
                return this._parsingContext.getCurrentName().length();
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
                return this._currToken.asCharArray().length;
        }
        return this._textBuffer.size();
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
    /* JADX WARNING: Code restructure failed: missing block: B:10:0x0037, code lost:
        if (r9 < 0) goto L_0x015b;
     */
    public int _readBinary(Base64Variant b64variant, OutputStream out, byte[] buffer) throws IOException {
        Base64Variant base64Variant = b64variant;
        OutputStream outputStream = out;
        byte[] bArr = buffer;
        int outputPtr = 0;
        int ch = 3;
        int outputEnd = bArr.length - 3;
        int outputCount = 0;
        while (true) {
            if (this._inputPtr >= this._inputEnd) {
                _loadMoreGuaranteed();
            }
            byte[] bArr2 = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int ch2 = bArr2[i] & 255;
            if (ch2 > 32) {
                int bits = base64Variant.decodeBase64Char(ch2);
                if (bits < 0) {
                    if (ch2 == 34) {
                        break;
                    }
                    bits = _decodeBase64Escape(base64Variant, ch2, 0);
                }
                if (outputPtr > outputEnd) {
                    outputCount += outputPtr;
                    outputStream.write(bArr, 0, outputPtr);
                    outputPtr = 0;
                }
                int decodedData = bits;
                if (this._inputPtr >= this._inputEnd) {
                    _loadMoreGuaranteed();
                }
                byte[] bArr3 = this._inputBuffer;
                int i2 = this._inputPtr;
                this._inputPtr = i2 + 1;
                int ch3 = bArr3[i2] & 255;
                int bits2 = base64Variant.decodeBase64Char(ch3);
                if (bits2 < 0) {
                    bits2 = _decodeBase64Escape(base64Variant, ch3, 1);
                }
                int decodedData2 = (decodedData << 6) | bits2;
                if (this._inputPtr >= this._inputEnd) {
                    _loadMoreGuaranteed();
                }
                byte[] bArr4 = this._inputBuffer;
                int i3 = this._inputPtr;
                this._inputPtr = i3 + 1;
                int ch4 = bArr4[i3] & 255;
                int bits3 = base64Variant.decodeBase64Char(ch4);
                if (bits3 < 0) {
                    if (bits3 != -2) {
                        if (ch4 == 34 && !b64variant.usesPadding()) {
                            int outputPtr2 = outputPtr + 1;
                            bArr[outputPtr] = (byte) (decodedData2 >> 4);
                            outputPtr = outputPtr2;
                            break;
                        }
                        bits3 = _decodeBase64Escape(base64Variant, ch4, 2);
                    }
                    if (bits3 == -2) {
                        if (this._inputPtr >= this._inputEnd) {
                            _loadMoreGuaranteed();
                        }
                        byte[] bArr5 = this._inputBuffer;
                        int i4 = this._inputPtr;
                        this._inputPtr = i4 + 1;
                        int ch5 = bArr5[i4] & 255;
                        if (base64Variant.usesPaddingChar(ch5)) {
                            int outputPtr3 = outputPtr + 1;
                            bArr[outputPtr] = (byte) (decodedData2 >> 4);
                            outputPtr = outputPtr3;
                        } else {
                            StringBuilder sb = new StringBuilder();
                            sb.append("expected padding character '");
                            sb.append(b64variant.getPaddingChar());
                            sb.append("'");
                            throw reportInvalidBase64Char(base64Variant, ch5, ch, sb.toString());
                        }
                    }
                }
                int decodedData3 = (decodedData2 << 6) | bits3;
                if (this._inputPtr >= this._inputEnd) {
                    _loadMoreGuaranteed();
                }
                byte[] bArr6 = this._inputBuffer;
                int i5 = this._inputPtr;
                this._inputPtr = i5 + 1;
                int ch6 = bArr6[i5] & 255;
                int bits4 = base64Variant.decodeBase64Char(ch6);
                if (bits4 < 0) {
                    if (bits4 != -2) {
                        if (ch6 == 34 && !b64variant.usesPadding()) {
                            int decodedData4 = decodedData3 >> 2;
                            int outputPtr4 = outputPtr + 1;
                            bArr[outputPtr] = (byte) (decodedData4 >> 8);
                            outputPtr = outputPtr4 + 1;
                            bArr[outputPtr4] = (byte) decodedData4;
                            break;
                        }
                        bits4 = _decodeBase64Escape(base64Variant, ch6, 3);
                    }
                    if (bits4 == -2) {
                        int decodedData5 = decodedData3 >> 2;
                        int outputPtr5 = outputPtr + 1;
                        bArr[outputPtr] = (byte) (decodedData5 >> 8);
                        outputPtr = outputPtr5 + 1;
                        bArr[outputPtr5] = (byte) decodedData5;
                        ch = 3;
                    }
                }
                int decodedData6 = (decodedData3 << 6) | bits4;
                int outputPtr6 = outputPtr + 1;
                bArr[outputPtr] = (byte) (decodedData6 >> 16);
                int outputPtr7 = outputPtr6 + 1;
                bArr[outputPtr6] = (byte) (decodedData6 >> 8);
                int outputPtr8 = outputPtr7 + 1;
                bArr[outputPtr7] = (byte) decodedData6;
                outputPtr = outputPtr8;
                ch = 3;
            }
            ch = 3;
        }
        this._tokenIncomplete = false;
        if (outputPtr <= 0) {
            return outputCount;
        }
        int outputCount2 = outputCount + outputPtr;
        outputStream.write(bArr, 0, outputPtr);
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
        if (i == 93) {
            _closeArrayScope();
            JsonToken jsonToken = JsonToken.END_ARRAY;
            this._currToken = jsonToken;
            return jsonToken;
        } else if (i == 125) {
            _closeObjectScope();
            JsonToken jsonToken2 = JsonToken.END_OBJECT;
            this._currToken = jsonToken2;
            return jsonToken2;
        } else {
            if (this._parsingContext.expectComma()) {
                if (i != 44) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("was expecting comma to separate ");
                    sb.append(this._parsingContext.typeDesc());
                    sb.append(" entries");
                    _reportUnexpectedChar(i, sb.toString());
                }
                i = _skipWS();
                if ((this._features & FEAT_MASK_TRAILING_COMMA) != 0 && (i == 93 || i == 125)) {
                    return _closeScope(i);
                }
            }
            if (!this._parsingContext.inObject()) {
                _updateLocation();
                return _nextTokenNotInObject(i);
            }
            _updateNameLocation();
            this._parsingContext.setCurrentName(_parseName(i));
            this._currToken = JsonToken.FIELD_NAME;
            int i2 = _skipColon();
            _updateLocation();
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
                _matchFalse();
                t = JsonToken.VALUE_FALSE;
            } else if (i2 == 110) {
                _matchNull();
                t = JsonToken.VALUE_NULL;
            } else if (i2 == 116) {
                _matchTrue();
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
            _matchFalse();
            JsonToken jsonToken3 = JsonToken.VALUE_FALSE;
            this._currToken = jsonToken3;
            return jsonToken3;
        } else if (i == 110) {
            _matchNull();
            JsonToken jsonToken4 = JsonToken.VALUE_NULL;
            this._currToken = jsonToken4;
            return jsonToken4;
        } else if (i == 116) {
            _matchTrue();
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

    public boolean nextFieldName(SerializableString str) throws IOException {
        this._numTypesValid = 0;
        if (this._currToken == JsonToken.FIELD_NAME) {
            _nextAfterName();
            return false;
        }
        if (this._tokenIncomplete) {
            _skipString();
        }
        int i = _skipWSOrEnd();
        if (i < 0) {
            close();
            this._currToken = null;
            return false;
        }
        this._binaryValue = null;
        if (i == 93) {
            _closeArrayScope();
            this._currToken = JsonToken.END_ARRAY;
            return false;
        } else if (i == 125) {
            _closeObjectScope();
            this._currToken = JsonToken.END_OBJECT;
            return false;
        } else {
            if (this._parsingContext.expectComma()) {
                if (i != 44) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("was expecting comma to separate ");
                    sb.append(this._parsingContext.typeDesc());
                    sb.append(" entries");
                    _reportUnexpectedChar(i, sb.toString());
                }
                i = _skipWS();
                if ((this._features & FEAT_MASK_TRAILING_COMMA) != 0 && (i == 93 || i == 125)) {
                    _closeScope(i);
                    return false;
                }
            }
            if (!this._parsingContext.inObject()) {
                _updateLocation();
                _nextTokenNotInObject(i);
                return false;
            }
            _updateNameLocation();
            if (i == 34) {
                byte[] nameBytes = str.asQuotedUTF8();
                int len = nameBytes.length;
                if (this._inputPtr + len + 4 < this._inputEnd) {
                    int end = this._inputPtr + len;
                    if (this._inputBuffer[end] == 34) {
                        int offset = 0;
                        int ptr = this._inputPtr;
                        while (ptr != end) {
                            if (nameBytes[offset] == this._inputBuffer[ptr]) {
                                offset++;
                                ptr++;
                            }
                        }
                        this._parsingContext.setCurrentName(str.getValue());
                        _isNextTokenNameYes(_skipColonFast(ptr + 1));
                        return true;
                    }
                }
            }
            return _isNextTokenNameMaybe(i, str);
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
        int i = _skipWSOrEnd();
        if (i < 0) {
            close();
            this._currToken = null;
            return null;
        }
        this._binaryValue = null;
        if (i == 93) {
            _closeArrayScope();
            this._currToken = JsonToken.END_ARRAY;
            return null;
        } else if (i == 125) {
            _closeObjectScope();
            this._currToken = JsonToken.END_OBJECT;
            return null;
        } else {
            if (this._parsingContext.expectComma()) {
                if (i != 44) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("was expecting comma to separate ");
                    sb.append(this._parsingContext.typeDesc());
                    sb.append(" entries");
                    _reportUnexpectedChar(i, sb.toString());
                }
                i = _skipWS();
                if ((this._features & FEAT_MASK_TRAILING_COMMA) != 0 && (i == 93 || i == 125)) {
                    _closeScope(i);
                    return null;
                }
            }
            if (!this._parsingContext.inObject()) {
                _updateLocation();
                _nextTokenNotInObject(i);
                return null;
            }
            _updateNameLocation();
            String nameStr = _parseName(i);
            this._parsingContext.setCurrentName(nameStr);
            this._currToken = JsonToken.FIELD_NAME;
            int i2 = _skipColon();
            _updateLocation();
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
                _matchFalse();
                t = JsonToken.VALUE_FALSE;
            } else if (i2 == 110) {
                _matchNull();
                t = JsonToken.VALUE_NULL;
            } else if (i2 == 116) {
                _matchTrue();
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
    }

    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r10v1, types: [byte] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r10v5, types: [byte] */
    private final int _skipColonFast(int ptr) throws IOException {
        int ptr2;
        int ptr3;
        byte[] bArr = this._inputBuffer;
        int ptr4 = ptr + 1;
        int i = bArr[ptr];
        if (i == 58) {
            int ptr5 = ptr4 + 1;
            byte i2 = bArr[ptr4];
            if (i2 > 32) {
                if (!(i2 == 47 || i2 == 35)) {
                    this._inputPtr = ptr5;
                    return i2;
                }
            } else if (i2 == 32 || i2 == 9) {
                ptr3 = ptr5 + 1;
                byte i3 = this._inputBuffer[ptr5];
                if (!(i3 <= 32 || i3 == 47 || i3 == 35)) {
                    this._inputPtr = ptr3;
                    return i3;
                }
                this._inputPtr = ptr3 - 1;
                return _skipColon2(true);
            }
            ptr3 = ptr5;
            this._inputPtr = ptr3 - 1;
            return _skipColon2(true);
        }
        if (i == 32 || i == 9) {
            int ptr6 = ptr4 + 1;
            i = this._inputBuffer[ptr4];
            ptr4 = ptr6;
        }
        if (i == 58) {
            int ptr7 = ptr4 + 1;
            byte i4 = this._inputBuffer[ptr4];
            if (i4 > 32) {
                if (!(i4 == 47 || i4 == 35)) {
                    this._inputPtr = ptr7;
                    return i4;
                }
            } else if (i4 == 32 || i4 == 9) {
                ptr2 = ptr7 + 1;
                byte i5 = this._inputBuffer[ptr7];
                if (!(i5 <= 32 || i5 == 47 || i5 == 35)) {
                    this._inputPtr = ptr2;
                    return i5;
                }
                this._inputPtr = ptr2 - 1;
                return _skipColon2(true);
            }
            ptr2 = ptr7;
            this._inputPtr = ptr2 - 1;
            return _skipColon2(true);
        }
        this._inputPtr = ptr4 - 1;
        return _skipColon2(false);
    }

    private final void _isNextTokenNameYes(int i) throws IOException {
        this._currToken = JsonToken.FIELD_NAME;
        _updateLocation();
        if (i == 34) {
            this._tokenIncomplete = true;
            this._nextToken = JsonToken.VALUE_STRING;
        } else if (i == 45) {
            this._nextToken = _parseNegNumber();
        } else if (i == 91) {
            this._nextToken = JsonToken.START_ARRAY;
        } else if (i == 102) {
            _matchFalse();
            this._nextToken = JsonToken.VALUE_FALSE;
        } else if (i == 110) {
            _matchNull();
            this._nextToken = JsonToken.VALUE_NULL;
        } else if (i == 116) {
            _matchTrue();
            this._nextToken = JsonToken.VALUE_TRUE;
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
                    this._nextToken = _parsePosNumber(i);
                    return;
                default:
                    this._nextToken = _handleUnexpectedValue(i);
                    return;
            }
        } else {
            this._nextToken = JsonToken.START_OBJECT;
        }
    }

    private final boolean _isNextTokenNameMaybe(int i, SerializableString str) throws IOException {
        JsonToken t;
        String n = _parseName(i);
        this._parsingContext.setCurrentName(n);
        boolean match = n.equals(str.getValue());
        this._currToken = JsonToken.FIELD_NAME;
        int i2 = _skipColon();
        _updateLocation();
        if (i2 == 34) {
            this._tokenIncomplete = true;
            this._nextToken = JsonToken.VALUE_STRING;
            return match;
        }
        if (i2 == 45) {
            t = _parseNegNumber();
        } else if (i2 == 91) {
            t = JsonToken.START_ARRAY;
        } else if (i2 == 102) {
            _matchFalse();
            t = JsonToken.VALUE_FALSE;
        } else if (i2 == 110) {
            _matchNull();
            t = JsonToken.VALUE_NULL;
        } else if (i2 == 116) {
            _matchTrue();
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
        return match;
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
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        if (c == 48) {
            c = _verifyNoLeadingZeroes();
        }
        outBuf[0] = (char) c;
        int end = Math.min(this._inputEnd, (this._inputPtr + outBuf.length) - 1);
        int intLen = 1;
        int outPtr = 1;
        while (this._inputPtr < end) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            byte b = bArr[i] & 255;
            if (b >= 48 && b <= 57) {
                intLen++;
                int outPtr2 = outPtr + 1;
                outBuf[outPtr] = (char) b;
                outPtr = outPtr2;
            } else if (b == 46 || b == 101 || b == 69) {
                return _parseFloat(outBuf, outPtr, b, false, intLen);
            } else {
                this._inputPtr--;
                this._textBuffer.setCurrentLength(outPtr);
                if (this._parsingContext.inRoot()) {
                    _verifyRootSpace(b);
                }
                return resetInt(false, intLen);
            }
        }
        return _parseNumber2(outBuf, outPtr, false, intLen);
    }

    /* access modifiers changed from: protected */
    public JsonToken _parseNegNumber() throws IOException {
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        int outPtr = 0 + 1;
        outBuf[0] = '-';
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int c = bArr[i] & 255;
        if (c <= 48) {
            if (c != 48) {
                return _handleInvalidNumberStart(c, true);
            }
            c = _verifyNoLeadingZeroes();
        } else if (c > 57) {
            return _handleInvalidNumberStart(c, true);
        }
        int outPtr2 = outPtr + 1;
        outBuf[outPtr] = (char) c;
        int end = Math.min(this._inputEnd, (this._inputPtr + outBuf.length) - outPtr2);
        int intLen = 1;
        int outPtr3 = outPtr2;
        while (this._inputPtr < end) {
            byte[] bArr2 = this._inputBuffer;
            int i2 = this._inputPtr;
            this._inputPtr = i2 + 1;
            int i3 = bArr2[i2] & 255;
            if (i3 >= 48 && i3 <= 57) {
                intLen++;
                int outPtr4 = outPtr3 + 1;
                outBuf[outPtr3] = (char) i3;
                outPtr3 = outPtr4;
                int outPtr5 = i3;
            } else if (i3 == 46 || i3 == 101 || i3 == 69) {
                return _parseFloat(outBuf, outPtr3, i3, true, intLen);
            } else {
                this._inputPtr--;
                this._textBuffer.setCurrentLength(outPtr3);
                if (this._parsingContext.inRoot()) {
                    _verifyRootSpace(i3);
                }
                return resetInt(true, intLen);
            }
        }
        return _parseNumber2(outBuf, outPtr3, true, intLen);
    }

    private final JsonToken _parseNumber2(char[] outBuf, int outPtr, boolean negative, int intPartLength) throws IOException {
        byte b;
        while (true) {
            if (this._inputPtr < this._inputEnd || _loadMore()) {
                byte[] bArr = this._inputBuffer;
                int i = this._inputPtr;
                this._inputPtr = i + 1;
                b = bArr[i] & 255;
                if (b <= 57 && b >= 48) {
                    if (outPtr >= outBuf.length) {
                        outBuf = this._textBuffer.finishCurrentSegment();
                        outPtr = 0;
                    }
                    int outPtr2 = outPtr + 1;
                    outBuf[outPtr] = (char) b;
                    intPartLength++;
                    outPtr = outPtr2;
                }
            } else {
                this._textBuffer.setCurrentLength(outPtr);
                return resetInt(negative, intPartLength);
            }
        }
        if (b == 46 || b == 101 || b == 69) {
            return _parseFloat(outBuf, outPtr, b, negative, intPartLength);
        }
        this._inputPtr--;
        this._textBuffer.setCurrentLength(outPtr);
        if (this._parsingContext.inRoot()) {
            byte[] bArr2 = this._inputBuffer;
            int i2 = this._inputPtr;
            this._inputPtr = i2 + 1;
            _verifyRootSpace(bArr2[i2] & 255);
        }
        return resetInt(negative, intPartLength);
    }

    private final int _verifyNoLeadingZeroes() throws IOException {
        if (this._inputPtr >= this._inputEnd && !_loadMore()) {
            return 48;
        }
        int ch = this._inputBuffer[this._inputPtr] & 255;
        if (ch < 48 || ch > 57) {
            return 48;
        }
        if (!isEnabled(Feature.ALLOW_NUMERIC_LEADING_ZEROS)) {
            reportInvalidNumber("Leading zeroes not allowed");
        }
        this._inputPtr++;
        if (ch == 48) {
            do {
                if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                    break;
                }
                ch = this._inputBuffer[this._inputPtr] & 255;
                if (ch < 48 || ch > 57) {
                    return 48;
                }
                this._inputPtr++;
            } while (ch == 48);
        }
        return ch;
    }

    private final JsonToken _parseFloat(char[] outBuf, int outPtr, int outPtr2, boolean negative, int integerPartLength) throws IOException {
        int fractLen = 0;
        boolean eof = false;
        if (outPtr2 == 46) {
            if (outPtr >= outBuf.length) {
                outBuf = this._textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            int outPtr3 = outPtr + 1;
            outBuf[outPtr] = (char) outPtr2;
            while (true) {
                outPtr = outPtr3;
                if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                    eof = true;
                    break;
                }
                byte[] bArr = this._inputBuffer;
                int i = this._inputPtr;
                this._inputPtr = i + 1;
                outPtr2 = bArr[i] & 255;
                if (outPtr2 < 48 || outPtr2 > 57) {
                    break;
                }
                fractLen++;
                if (outPtr >= outBuf.length) {
                    outBuf = this._textBuffer.finishCurrentSegment();
                    outPtr = 0;
                }
                outPtr3 = outPtr + 1;
                outBuf[outPtr] = (char) outPtr2;
            }
            if (fractLen == 0) {
                reportUnexpectedNumberChar(outPtr2, "Decimal point not followed by a digit");
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
            if (this._inputPtr >= this._inputEnd) {
                _loadMoreGuaranteed();
            }
            byte[] bArr2 = this._inputBuffer;
            int i2 = this._inputPtr;
            this._inputPtr = i2 + 1;
            int c = bArr2[i2] & 255;
            if (c == 45 || c == 43) {
                if (outPtr4 >= outBuf.length) {
                    outBuf = this._textBuffer.finishCurrentSegment();
                    outPtr4 = 0;
                }
                int outPtr5 = outPtr4 + 1;
                outBuf[outPtr4] = (char) c;
                if (this._inputPtr >= this._inputEnd) {
                    _loadMoreGuaranteed();
                }
                byte[] bArr3 = this._inputBuffer;
                int i3 = this._inputPtr;
                this._inputPtr = i3 + 1;
                int c2 = bArr3[i3] & 255;
                outPtr4 = outPtr5;
                outPtr = c2;
            } else {
                outPtr = c;
            }
            while (true) {
                if (outPtr >= 48 && outPtr <= 57) {
                    expLen++;
                    if (outPtr4 >= outBuf.length) {
                        outBuf = this._textBuffer.finishCurrentSegment();
                        outPtr4 = 0;
                    }
                    outPtr = outPtr4 + 1;
                    outBuf[outPtr4] = (char) outPtr;
                    if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                        eof = true;
                        break;
                    }
                    byte[] bArr4 = this._inputBuffer;
                    int i4 = this._inputPtr;
                    this._inputPtr = i4 + 1;
                    outPtr = bArr4[i4] & 255;
                    outPtr4 = outPtr;
                } else {
                    outPtr = outPtr4;
                }
            }
            outPtr = outPtr4;
            if (expLen == 0) {
                reportUnexpectedNumberChar(outPtr, "Exponent indicator not followed by a digit");
            }
        }
        if (!eof) {
            this._inputPtr--;
            if (this._parsingContext.inRoot()) {
                _verifyRootSpace(outPtr);
            }
        }
        this._textBuffer.setCurrentLength(outPtr);
        return resetFloat(negative, integerPartLength, fractLen, expLen);
    }

    private final void _verifyRootSpace(int ch) throws IOException {
        this._inputPtr++;
        if (ch != 13) {
            if (ch != 32) {
                switch (ch) {
                    case 9:
                        break;
                    case 10:
                        this._currInputRow++;
                        this._currInputRowStart = this._inputPtr;
                        return;
                    default:
                        _reportMissingRootWS(ch);
                        return;
                }
            }
            return;
        }
        _skipCR();
    }

    /* access modifiers changed from: protected */
    public final String _parseName(int i) throws IOException {
        if (i != 34) {
            return _handleOddName(i);
        }
        if (this._inputPtr + 13 > this._inputEnd) {
            return slowParseName();
        }
        byte[] input = this._inputBuffer;
        int[] codes = _icLatin1;
        int i2 = this._inputPtr;
        this._inputPtr = i2 + 1;
        int q = input[i2] & 255;
        if (codes[q] == 0) {
            int i3 = this._inputPtr;
            this._inputPtr = i3 + 1;
            int i4 = input[i3] & 255;
            if (codes[i4] == 0) {
                int q2 = (q << 8) | i4;
                int i5 = this._inputPtr;
                this._inputPtr = i5 + 1;
                int i6 = input[i5] & 255;
                if (codes[i6] == 0) {
                    int q3 = (q2 << 8) | i6;
                    int i7 = this._inputPtr;
                    this._inputPtr = i7 + 1;
                    int i8 = input[i7] & 255;
                    if (codes[i8] == 0) {
                        int q4 = (q3 << 8) | i8;
                        int i9 = this._inputPtr;
                        this._inputPtr = i9 + 1;
                        int i10 = input[i9] & 255;
                        if (codes[i10] == 0) {
                            this._quad1 = q4;
                            return parseMediumName(i10);
                        } else if (i10 == 34) {
                            return findName(q4, 4);
                        } else {
                            return parseName(q4, i10, 4);
                        }
                    } else if (i8 == 34) {
                        return findName(q3, 3);
                    } else {
                        return parseName(q3, i8, 3);
                    }
                } else if (i6 == 34) {
                    return findName(q2, 2);
                } else {
                    return parseName(q2, i6, 2);
                }
            } else if (i4 == 34) {
                return findName(q, 1);
            } else {
                return parseName(q, i4, 1);
            }
        } else if (q == 34) {
            return "";
        } else {
            return parseName(0, q, 0);
        }
    }

    /* access modifiers changed from: protected */
    public final String parseMediumName(int q2) throws IOException {
        byte[] input = this._inputBuffer;
        int[] codes = _icLatin1;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int i2 = input[i] & 255;
        if (codes[i2] == 0) {
            int q22 = (q2 << 8) | i2;
            int i3 = this._inputPtr;
            this._inputPtr = i3 + 1;
            int i4 = input[i3] & 255;
            if (codes[i4] == 0) {
                int q23 = (q22 << 8) | i4;
                int i5 = this._inputPtr;
                this._inputPtr = i5 + 1;
                int i6 = input[i5] & 255;
                if (codes[i6] == 0) {
                    int q24 = (q23 << 8) | i6;
                    int i7 = this._inputPtr;
                    this._inputPtr = i7 + 1;
                    int i8 = input[i7] & 255;
                    if (codes[i8] == 0) {
                        return parseMediumName2(i8, q24);
                    }
                    if (i8 == 34) {
                        return findName(this._quad1, q24, 4);
                    }
                    return parseName(this._quad1, q24, i8, 4);
                } else if (i6 == 34) {
                    return findName(this._quad1, q23, 3);
                } else {
                    return parseName(this._quad1, q23, i6, 3);
                }
            } else if (i4 == 34) {
                return findName(this._quad1, q22, 2);
            } else {
                return parseName(this._quad1, q22, i4, 2);
            }
        } else if (i2 == 34) {
            return findName(this._quad1, q2, 1);
        } else {
            return parseName(this._quad1, q2, i2, 1);
        }
    }

    /* access modifiers changed from: protected */
    public final String parseMediumName2(int q3, int q2) throws IOException {
        byte[] input = this._inputBuffer;
        int[] codes = _icLatin1;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int i2 = input[i] & 255;
        if (codes[i2] == 0) {
            int q32 = (q3 << 8) | i2;
            int i3 = this._inputPtr;
            this._inputPtr = i3 + 1;
            int i4 = input[i3] & 255;
            if (codes[i4] == 0) {
                int q33 = (q32 << 8) | i4;
                int i5 = this._inputPtr;
                this._inputPtr = i5 + 1;
                int i6 = input[i5] & 255;
                if (codes[i6] == 0) {
                    int q34 = (q33 << 8) | i6;
                    int i7 = this._inputPtr;
                    this._inputPtr = i7 + 1;
                    byte b = input[i7] & 255;
                    if (codes[b] == 0) {
                        return parseLongName(b, q2, q34);
                    }
                    if (b == 34) {
                        return findName(this._quad1, q2, q34, 4);
                    }
                    return parseName(this._quad1, q2, q34, b, 4);
                } else if (i6 == 34) {
                    return findName(this._quad1, q2, q33, 3);
                } else {
                    return parseName(this._quad1, q2, q33, i6, 3);
                }
            } else if (i4 == 34) {
                return findName(this._quad1, q2, q32, 2);
            } else {
                return parseName(this._quad1, q2, q32, i4, 2);
            }
        } else if (i2 == 34) {
            return findName(this._quad1, q2, q3, 1);
        } else {
            return parseName(this._quad1, q2, q3, i2, 1);
        }
    }

    /* access modifiers changed from: protected */
    public final String parseLongName(int q, int q2, int q3) throws IOException {
        int[] iArr = this._quadBuffer;
        iArr[0] = this._quad1;
        iArr[1] = q2;
        iArr[2] = q3;
        byte[] input = this._inputBuffer;
        int[] codes = _icLatin1;
        int qlen = 3;
        while (this._inputPtr + 4 <= this._inputEnd) {
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int i2 = input[i] & 255;
            if (codes[i2] == 0) {
                int q4 = (q << 8) | i2;
                int i3 = this._inputPtr;
                this._inputPtr = i3 + 1;
                int i4 = input[i3] & 255;
                if (codes[i4] == 0) {
                    int q5 = (q4 << 8) | i4;
                    int i5 = this._inputPtr;
                    this._inputPtr = i5 + 1;
                    int i6 = input[i5] & 255;
                    if (codes[i6] == 0) {
                        int q6 = (q5 << 8) | i6;
                        int i7 = this._inputPtr;
                        this._inputPtr = i7 + 1;
                        int i8 = input[i7] & 255;
                        if (codes[i8] == 0) {
                            int[] iArr2 = this._quadBuffer;
                            if (qlen >= iArr2.length) {
                                this._quadBuffer = growArrayBy(iArr2, qlen);
                            }
                            int qlen2 = qlen + 1;
                            this._quadBuffer[qlen] = q6;
                            q = i8;
                            qlen = qlen2;
                        } else if (i8 == 34) {
                            return findName(this._quadBuffer, qlen, q6, 4);
                        } else {
                            return parseEscapedName(this._quadBuffer, qlen, q6, i8, 4);
                        }
                    } else if (i6 == 34) {
                        return findName(this._quadBuffer, qlen, q5, 3);
                    } else {
                        return parseEscapedName(this._quadBuffer, qlen, q5, i6, 3);
                    }
                } else if (i4 == 34) {
                    return findName(this._quadBuffer, qlen, q4, 2);
                } else {
                    return parseEscapedName(this._quadBuffer, qlen, q4, i4, 2);
                }
            } else if (i2 == 34) {
                return findName(this._quadBuffer, qlen, q, 1);
            } else {
                return parseEscapedName(this._quadBuffer, qlen, q, i2, 1);
            }
        }
        return parseEscapedName(this._quadBuffer, qlen, 0, q, 0);
    }

    /* access modifiers changed from: protected */
    public String slowParseName() throws IOException {
        if (this._inputPtr >= this._inputEnd && !_loadMore()) {
            _reportInvalidEOF(": was expecting closing '\"' for name", JsonToken.FIELD_NAME);
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        byte b = bArr[i] & 255;
        if (b == 34) {
            return "";
        }
        return parseEscapedName(this._quadBuffer, 0, 0, b, 0);
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
                            int[] growArrayBy = growArrayBy(quads, quads.length);
                            quads = growArrayBy;
                            this._quadBuffer = growArrayBy;
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
                                int[] growArrayBy2 = growArrayBy(quads, quads.length);
                                quads = growArrayBy2;
                                this._quadBuffer = growArrayBy2;
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
                    int[] growArrayBy3 = growArrayBy(quads, quads.length);
                    quads = growArrayBy3;
                    this._quadBuffer = growArrayBy3;
                }
                int qlen4 = qlen + 1;
                quads[qlen] = currQuad;
                currQuadBytes = 1;
                currQuad = ch;
                qlen = qlen4;
            }
            if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                _reportInvalidEOF(" in field name", JsonToken.FIELD_NAME);
            }
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            ch = bArr[i] & 255;
        }
        if (currQuadBytes > 0) {
            if (qlen >= quads.length) {
                int[] growArrayBy4 = growArrayBy(quads, quads.length);
                quads = growArrayBy4;
                this._quadBuffer = growArrayBy4;
            }
            int qlen5 = qlen + 1;
            quads[qlen] = _padLastQuad(currQuad, currQuadBytes);
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
        while (true) {
            if (currQuadBytes < 4) {
                currQuadBytes++;
                currQuad2 = (currQuad2 << 8) | ch;
            } else {
                if (currQuad >= quads.length) {
                    int[] growArrayBy = growArrayBy(quads, quads.length);
                    quads = growArrayBy;
                    this._quadBuffer = growArrayBy;
                }
                int qlen = currQuad + 1;
                quads[currQuad] = currQuad2;
                currQuadBytes = 1;
                currQuad2 = ch;
                currQuad = qlen;
            }
            if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                _reportInvalidEOF(" in field name", JsonToken.FIELD_NAME);
            }
            ch = this._inputBuffer[this._inputPtr] & 255;
            if (codes[ch] != 0) {
                break;
            }
            this._inputPtr++;
        }
        if (currQuadBytes > 0) {
            if (currQuad >= quads.length) {
                int[] growArrayBy2 = growArrayBy(quads, quads.length);
                quads = growArrayBy2;
                this._quadBuffer = growArrayBy2;
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
        if (this._inputPtr >= this._inputEnd && !_loadMore()) {
            _reportInvalidEOF(": was expecting closing ''' for field name", JsonToken.FIELD_NAME);
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int ch = bArr[i] & 255;
        if (ch == 39) {
            return "";
        }
        int[] quads = this._quadBuffer;
        int qlen2 = 0;
        int currQuad2 = 0;
        int currQuadBytes2 = 0;
        int[] codes = _icLatin1;
        while (ch != 39) {
            if (!(codes[ch] == 0 || ch == '\"')) {
                if (ch != '\\') {
                    _throwUnquotedSpace(ch, "name");
                } else {
                    ch = _decodeEscaped();
                }
                if (ch > 127) {
                    if (currQuadBytes >= 4) {
                        if (qlen2 >= quads.length) {
                            int[] growArrayBy = growArrayBy(quads, quads.length);
                            quads = growArrayBy;
                            this._quadBuffer = growArrayBy;
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
                                int[] growArrayBy2 = growArrayBy(quads, quads.length);
                                quads = growArrayBy2;
                                this._quadBuffer = growArrayBy2;
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
                    int[] growArrayBy3 = growArrayBy(quads, quads.length);
                    quads = growArrayBy3;
                    this._quadBuffer = growArrayBy3;
                }
                int qlen5 = qlen2 + 1;
                quads[qlen2] = currQuad;
                currQuadBytes2 = 1;
                currQuad2 = ch;
                qlen2 = qlen5;
            }
            if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                _reportInvalidEOF(" in field name", JsonToken.FIELD_NAME);
            }
            byte[] bArr2 = this._inputBuffer;
            int i2 = this._inputPtr;
            this._inputPtr = i2 + 1;
            ch = bArr2[i2] & 255;
        }
        if (currQuadBytes > 0) {
            if (qlen2 >= quads.length) {
                int[] growArrayBy4 = growArrayBy(quads, quads.length);
                quads = growArrayBy4;
                this._quadBuffer = growArrayBy4;
            }
            qlen = qlen2 + 1;
            quads[qlen2] = _padLastQuad(currQuad, currQuadBytes);
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
        int q12 = _padLastQuad(q1, lastQuadBytes);
        String name = this._symbols.findName(q12);
        if (name != null) {
            return name;
        }
        int[] iArr = this._quadBuffer;
        iArr[0] = q12;
        return addName(iArr, 1, lastQuadBytes);
    }

    private final String findName(int q1, int q2, int lastQuadBytes) throws JsonParseException {
        int q22 = _padLastQuad(q2, lastQuadBytes);
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
        int q32 = _padLastQuad(q3, lastQuadBytes);
        String name = this._symbols.findName(q1, q2, q32);
        if (name != null) {
            return name;
        }
        int[] quads = this._quadBuffer;
        quads[0] = q1;
        quads[1] = q2;
        quads[2] = _padLastQuad(q32, lastQuadBytes);
        return addName(quads, 3, lastQuadBytes);
    }

    private final String findName(int[] quads, int qlen, int lastQuad, int lastQuadBytes) throws JsonParseException {
        if (qlen >= quads.length) {
            int[] growArrayBy = growArrayBy(quads, quads.length);
            quads = growArrayBy;
            this._quadBuffer = growArrayBy;
        }
        int qlen2 = qlen + 1;
        quads[qlen] = _padLastQuad(lastQuad, lastQuadBytes);
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

    private static final int _padLastQuad(int q, int bytes) {
        return bytes == 4 ? q : (-1 << (bytes << 3)) | q;
    }

    /* access modifiers changed from: protected */
    public void _loadMoreGuaranteed() throws IOException {
        if (!_loadMore()) {
            _reportInvalidEOF();
        }
    }

    /* access modifiers changed from: protected */
    public void _finishString() throws IOException {
        int ptr = this._inputPtr;
        if (ptr >= this._inputEnd) {
            _loadMoreGuaranteed();
            ptr = this._inputPtr;
        }
        int outPtr = 0;
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        int[] codes = _icUTF8;
        int max = Math.min(this._inputEnd, outBuf.length + ptr);
        byte[] inputBuffer = this._inputBuffer;
        while (true) {
            if (ptr >= max) {
                break;
            }
            int c = inputBuffer[ptr] & 255;
            if (codes[c] == 0) {
                ptr++;
                int outPtr2 = outPtr + 1;
                outBuf[outPtr] = (char) c;
                outPtr = outPtr2;
            } else if (c == 34) {
                this._inputPtr = ptr + 1;
                this._textBuffer.setCurrentLength(outPtr);
                return;
            }
        }
        this._inputPtr = ptr;
        _finishString2(outBuf, outPtr);
    }

    /* access modifiers changed from: protected */
    public String _finishAndReturnString() throws IOException {
        int ptr = this._inputPtr;
        if (ptr >= this._inputEnd) {
            _loadMoreGuaranteed();
            ptr = this._inputPtr;
        }
        int outPtr = 0;
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        int[] codes = _icUTF8;
        int max = Math.min(this._inputEnd, outBuf.length + ptr);
        byte[] inputBuffer = this._inputBuffer;
        while (true) {
            if (ptr >= max) {
                break;
            }
            int c = inputBuffer[ptr] & 255;
            if (codes[c] == 0) {
                ptr++;
                int outPtr2 = outPtr + 1;
                outBuf[outPtr] = (char) c;
                outPtr = outPtr2;
            } else if (c == 34) {
                this._inputPtr = ptr + 1;
                return this._textBuffer.setCurrentAndReturn(outPtr);
            }
        }
        this._inputPtr = ptr;
        _finishString2(outBuf, outPtr);
        return this._textBuffer.contentsAsString();
    }

    private final void _finishString2(char[] outBuf, int outPtr) throws IOException {
        int[] codes = _icUTF8;
        byte[] inputBuffer = this._inputBuffer;
        while (true) {
            int ptr = this._inputPtr;
            if (ptr >= this._inputEnd) {
                _loadMoreGuaranteed();
                ptr = this._inputPtr;
            }
            if (outPtr >= outBuf.length) {
                outBuf = this._textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            int max = Math.min(this._inputEnd, (outBuf.length - outPtr) + ptr);
            while (true) {
                if (ptr < max) {
                    int ptr2 = ptr + 1;
                    int c = inputBuffer[ptr] & 255;
                    if (codes[c] != 0) {
                        this._inputPtr = ptr2;
                        if (c == 34) {
                            this._textBuffer.setCurrentLength(outPtr);
                            return;
                        }
                        switch (codes[c]) {
                            case 1:
                                c = _decodeEscaped();
                                break;
                            case 2:
                                c = _decodeUtf8_2(c);
                                break;
                            case 3:
                                if (this._inputEnd - this._inputPtr < 2) {
                                    c = _decodeUtf8_3(c);
                                    break;
                                } else {
                                    c = _decodeUtf8_3fast(c);
                                    break;
                                }
                            case 4:
                                int c2 = _decodeUtf8_4(c);
                                int outPtr2 = outPtr + 1;
                                outBuf[outPtr] = (char) (55296 | (c2 >> 10));
                                if (outPtr2 >= outBuf.length) {
                                    outBuf = this._textBuffer.finishCurrentSegment();
                                    outPtr = 0;
                                } else {
                                    outPtr = outPtr2;
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
                        }
                        int outPtr3 = outPtr + 1;
                        outBuf[outPtr] = (char) c;
                        outPtr = outPtr3;
                    } else {
                        int outPtr4 = outPtr + 1;
                        outBuf[outPtr] = (char) c;
                        ptr = ptr2;
                        outPtr = outPtr4;
                    }
                } else {
                    this._inputPtr = ptr;
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void _skipString() throws IOException {
        this._tokenIncomplete = false;
        int[] codes = _icUTF8;
        byte[] inputBuffer = this._inputBuffer;
        while (true) {
            int ptr = this._inputPtr;
            int max = this._inputEnd;
            if (ptr >= max) {
                _loadMoreGuaranteed();
                ptr = this._inputPtr;
                max = this._inputEnd;
            }
            while (true) {
                if (ptr < max) {
                    int ptr2 = ptr + 1;
                    int c = inputBuffer[ptr] & 255;
                    if (codes[c] != 0) {
                        this._inputPtr = ptr2;
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
                                    _skipUtf8_4(c);
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
                    } else {
                        ptr = ptr2;
                    }
                } else {
                    this._inputPtr = ptr;
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0043, code lost:
        if (r3._parsingContext.inArray() == false) goto L_0x00a1;
     */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x00a7  */
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
                                if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                                    _reportInvalidEOFInValue(JsonToken.VALUE_NUMBER_INT);
                                }
                                byte[] bArr = this._inputBuffer;
                                int i = this._inputPtr;
                                this._inputPtr = i + 1;
                                return _handleInvalidNumberStart(bArr[i] & 255, false);
                            case 44:
                                break;
                        }
                    }
                    _reportUnexpectedChar(c, "expected a value");
                }
                if (isEnabled(Feature.ALLOW_MISSING_VALUES)) {
                    this._inputPtr--;
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
                _reportInvalidToken(sb.toString(), "('true', 'false' or 'null')");
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
    /* JADX WARNING: Removed duplicated region for block: B:20:0x0052  */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x0049 A[SYNTHETIC] */
    public JsonToken _handleApos() throws IOException {
        int c;
        int outPtr = 0;
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        int[] codes = _icUTF8;
        byte[] inputBuffer = this._inputBuffer;
        while (true) {
            if (this._inputPtr >= this._inputEnd) {
                _loadMoreGuaranteed();
            }
            if (outPtr >= outBuf.length) {
                outBuf = this._textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            int max = this._inputEnd;
            int max2 = this._inputPtr + (outBuf.length - outPtr);
            if (max2 < max) {
                max = max2;
            }
            while (true) {
                if (this._inputPtr < max) {
                    int i = this._inputPtr;
                    this._inputPtr = i + 1;
                    c = inputBuffer[i] & 255;
                    if (c != 39 && codes[c] == 0) {
                        int outPtr2 = outPtr + 1;
                        outBuf[outPtr] = (char) c;
                        outPtr = outPtr2;
                    } else if (c != 39) {
                        this._textBuffer.setCurrentLength(outPtr);
                        return JsonToken.VALUE_STRING;
                    } else {
                        switch (codes[c]) {
                            case 1:
                                c = _decodeEscaped();
                                break;
                            case 2:
                                c = _decodeUtf8_2(c);
                                break;
                            case 3:
                                if (this._inputEnd - this._inputPtr < 2) {
                                    c = _decodeUtf8_3(c);
                                    break;
                                } else {
                                    c = _decodeUtf8_3fast(c);
                                    break;
                                }
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
                        outPtr = outPtr4;
                    }
                }
            }
            if (c != 39) {
            }
        }
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Incorrect condition in loop: B:1:0x0002 */
    public JsonToken _handleInvalidNumberStart(int ch, boolean neg) throws IOException {
        String match;
        ch = ch;
        while (ch == 73) {
            if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                _reportInvalidEOFInValue(JsonToken.VALUE_NUMBER_FLOAT);
            }
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            ch = bArr[i];
            if (ch != 78) {
                if (ch != 110) {
                    ch = ch;
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
            _reportError("Non-standard token '%s': enable JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS to allow", match);
            ch = ch;
        }
        ch = ch;
        reportUnexpectedNumberChar(ch, "expected digit (0-9) to follow minus sign, for valid numeric value");
        return null;
    }

    /* access modifiers changed from: protected */
    public final void _matchTrue() throws IOException {
        int ptr = this._inputPtr;
        if (ptr + 3 < this._inputEnd) {
            byte[] buf = this._inputBuffer;
            int ptr2 = ptr + 1;
            if (buf[ptr] == 114) {
                int ptr3 = ptr2 + 1;
                if (buf[ptr2] == 117) {
                    int ptr4 = ptr3 + 1;
                    if (buf[ptr3] == 101) {
                        int ch = buf[ptr4] & 255;
                        if (ch < 48 || ch == 93 || ch == 125) {
                            this._inputPtr = ptr4;
                            return;
                        }
                    }
                    int ch2 = ptr4;
                }
            } else {
                int i = ptr2;
            }
        }
        _matchToken2("true", 1);
    }

    /* access modifiers changed from: protected */
    public final void _matchFalse() throws IOException {
        int ptr = this._inputPtr;
        if (ptr + 4 < this._inputEnd) {
            byte[] buf = this._inputBuffer;
            int ptr2 = ptr + 1;
            if (buf[ptr] == 97) {
                int ptr3 = ptr2 + 1;
                if (buf[ptr2] == 108) {
                    ptr2 = ptr3 + 1;
                    if (buf[ptr3] == 115) {
                        int ptr4 = ptr2 + 1;
                        if (buf[ptr2] == 101) {
                            int ch = buf[ptr4] & 255;
                            if (ch < 48 || ch == 93 || ch == 125) {
                                this._inputPtr = ptr4;
                                return;
                            }
                        }
                    }
                }
            }
        }
        _matchToken2("false", 1);
    }

    /* access modifiers changed from: protected */
    public final void _matchNull() throws IOException {
        int ptr = this._inputPtr;
        if (ptr + 3 < this._inputEnd) {
            byte[] buf = this._inputBuffer;
            int ptr2 = ptr + 1;
            if (buf[ptr] == 117) {
                int ptr3 = ptr2 + 1;
                if (buf[ptr2] == 108) {
                    int ptr4 = ptr3 + 1;
                    if (buf[ptr3] == 108) {
                        int ch = buf[ptr4] & 255;
                        if (ch < 48 || ch == 93 || ch == 125) {
                            this._inputPtr = ptr4;
                            return;
                        }
                    }
                    int ch2 = ptr4;
                }
            } else {
                int i = ptr2;
            }
        }
        _matchToken2("null", 1);
    }

    /* access modifiers changed from: protected */
    public final void _matchToken(String matchStr, int i) throws IOException {
        int len = matchStr.length();
        if (this._inputPtr + len >= this._inputEnd) {
            _matchToken2(matchStr, i);
            return;
        }
        do {
            if (this._inputBuffer[this._inputPtr] != matchStr.charAt(i)) {
                _reportInvalidToken(matchStr.substring(0, i));
            }
            this._inputPtr++;
            i++;
        } while (i < len);
        int ch = this._inputBuffer[this._inputPtr] & 255;
        if (!(ch < 48 || ch == 93 || ch == 125)) {
            _checkMatchEnd(matchStr, i, ch);
        }
    }

    private final void _matchToken2(String matchStr, int i) throws IOException {
        int len = matchStr.length();
        do {
            if ((this._inputPtr >= this._inputEnd && !_loadMore()) || this._inputBuffer[this._inputPtr] != matchStr.charAt(i)) {
                _reportInvalidToken(matchStr.substring(0, i));
            }
            this._inputPtr++;
            i++;
        } while (i < len);
        if (this._inputPtr < this._inputEnd || _loadMore()) {
            int ch = this._inputBuffer[this._inputPtr] & 255;
            if (!(ch < 48 || ch == 93 || ch == 125)) {
                _checkMatchEnd(matchStr, i, ch);
            }
        }
    }

    private final void _checkMatchEnd(String matchStr, int i, int ch) throws IOException {
        if (Character.isJavaIdentifierPart((char) _decodeCharForError(ch))) {
            _reportInvalidToken(matchStr.substring(0, i));
        }
    }

    private final int _skipWS() throws IOException {
        while (this._inputPtr < this._inputEnd) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int i2 = bArr[i] & 255;
            if (i2 > 32) {
                if (i2 != 47 && i2 != 35) {
                    return i2;
                }
                this._inputPtr--;
                return _skipWS2();
            } else if (i2 != 32) {
                if (i2 == 10) {
                    this._currInputRow++;
                    this._currInputRowStart = this._inputPtr;
                } else if (i2 == 13) {
                    _skipCR();
                } else if (i2 != 9) {
                    _throwInvalidSpace(i2);
                }
            }
        }
        return _skipWS2();
    }

    private final int _skipWS2() throws IOException {
        int i;
        while (true) {
            if (this._inputPtr < this._inputEnd || _loadMore()) {
                byte[] bArr = this._inputBuffer;
                int i2 = this._inputPtr;
                this._inputPtr = i2 + 1;
                i = bArr[i2] & 255;
                if (i > 32) {
                    if (i == 47) {
                        _skipComment();
                    } else if (i != 35 || !_skipYAMLComment()) {
                        return i;
                    }
                } else if (i != 32) {
                    if (i == 10) {
                        this._currInputRow++;
                        this._currInputRowStart = this._inputPtr;
                    } else if (i == 13) {
                        _skipCR();
                    } else if (i != 9) {
                        _throwInvalidSpace(i);
                    }
                }
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Unexpected end-of-input within/between ");
                sb.append(this._parsingContext.typeDesc());
                sb.append(" entries");
                throw _constructError(sb.toString());
            }
        }
        return i;
    }

    private final int _skipWSOrEnd() throws IOException {
        if (this._inputPtr >= this._inputEnd && !_loadMore()) {
            return _eofAsNextChar();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int i2 = bArr[i] & 255;
        if (i2 <= 32) {
            if (i2 != 32) {
                if (i2 == 10) {
                    this._currInputRow++;
                    this._currInputRowStart = this._inputPtr;
                } else if (i2 == 13) {
                    _skipCR();
                } else if (i2 != 9) {
                    _throwInvalidSpace(i2);
                }
            }
            while (this._inputPtr < this._inputEnd) {
                byte[] bArr2 = this._inputBuffer;
                int i3 = this._inputPtr;
                this._inputPtr = i3 + 1;
                int i4 = bArr2[i3] & 255;
                if (i4 > 32) {
                    if (i4 != 47 && i4 != 35) {
                        return i4;
                    }
                    this._inputPtr--;
                    return _skipWSOrEnd2();
                } else if (i4 != 32) {
                    if (i4 == 10) {
                        this._currInputRow++;
                        this._currInputRowStart = this._inputPtr;
                    } else if (i4 == 13) {
                        _skipCR();
                    } else if (i4 != 9) {
                        _throwInvalidSpace(i4);
                    }
                }
            }
            return _skipWSOrEnd2();
        } else if (i2 != 47 && i2 != 35) {
            return i2;
        } else {
            this._inputPtr--;
            return _skipWSOrEnd2();
        }
    }

    private final int _skipWSOrEnd2() throws IOException {
        int i;
        while (true) {
            if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                return _eofAsNextChar();
            }
            byte[] bArr = this._inputBuffer;
            int i2 = this._inputPtr;
            this._inputPtr = i2 + 1;
            i = bArr[i2] & 255;
            if (i > 32) {
                if (i == 47) {
                    _skipComment();
                } else if (i != 35 || !_skipYAMLComment()) {
                    return i;
                }
            } else if (i != 32) {
                if (i == 10) {
                    this._currInputRow++;
                    this._currInputRowStart = this._inputPtr;
                } else if (i == 13) {
                    _skipCR();
                } else if (i != 9) {
                    _throwInvalidSpace(i);
                }
            }
        }
        return i;
    }

    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r0v3, types: [byte] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r0v7, types: [byte] */
    private final int _skipColon() throws IOException {
        if (this._inputPtr + 4 >= this._inputEnd) {
            return _skipColon2(false);
        }
        int i = this._inputBuffer[this._inputPtr];
        if (i == 58) {
            byte[] bArr = this._inputBuffer;
            int i2 = this._inputPtr + 1;
            this._inputPtr = i2;
            byte i3 = bArr[i2];
            if (i3 <= 32) {
                if (i3 == 32 || i3 == 9) {
                    byte[] bArr2 = this._inputBuffer;
                    int i4 = this._inputPtr + 1;
                    this._inputPtr = i4;
                    byte i5 = bArr2[i4];
                    if (i5 > 32) {
                        if (i5 == 47 || i5 == 35) {
                            return _skipColon2(true);
                        }
                        this._inputPtr++;
                        return i5;
                    }
                }
                return _skipColon2(true);
            } else if (i3 == 47 || i3 == 35) {
                return _skipColon2(true);
            } else {
                this._inputPtr++;
                return i3;
            }
        } else {
            if (i == 32 || i == 9) {
                byte[] bArr3 = this._inputBuffer;
                int i6 = this._inputPtr + 1;
                this._inputPtr = i6;
                i = bArr3[i6];
            }
            if (i != 58) {
                return _skipColon2(false);
            }
            byte[] bArr4 = this._inputBuffer;
            int i7 = this._inputPtr + 1;
            this._inputPtr = i7;
            byte i8 = bArr4[i7];
            if (i8 <= 32) {
                if (i8 == 32 || i8 == 9) {
                    byte[] bArr5 = this._inputBuffer;
                    int i9 = this._inputPtr + 1;
                    this._inputPtr = i9;
                    byte i10 = bArr5[i9];
                    if (i10 > 32) {
                        if (i10 == 47 || i10 == 35) {
                            return _skipColon2(true);
                        }
                        this._inputPtr++;
                        return i10;
                    }
                }
                return _skipColon2(true);
            } else if (i8 == 47 || i8 == 35) {
                return _skipColon2(true);
            } else {
                this._inputPtr++;
                return i8;
            }
        }
    }

    private final int _skipColon2(boolean gotColon) throws IOException {
        while (true) {
            if (this._inputPtr < this._inputEnd || _loadMore()) {
                byte[] bArr = this._inputBuffer;
                int i = this._inputPtr;
                this._inputPtr = i + 1;
                int i2 = bArr[i] & 255;
                if (i2 > 32) {
                    if (i2 == 47) {
                        _skipComment();
                    } else if (i2 != 35 || !_skipYAMLComment()) {
                        if (gotColon) {
                            return i2;
                        }
                        if (i2 != 58) {
                            _reportUnexpectedChar(i2, "was expecting a colon to separate field name and value");
                        }
                        gotColon = true;
                    }
                } else if (i2 != 32) {
                    if (i2 == 10) {
                        this._currInputRow++;
                        this._currInputRowStart = this._inputPtr;
                    } else if (i2 == 13) {
                        _skipCR();
                    } else if (i2 != 9) {
                        _throwInvalidSpace(i2);
                    }
                }
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(" within/between ");
                sb.append(this._parsingContext.typeDesc());
                sb.append(" entries");
                _reportInvalidEOF(sb.toString(), null);
                return -1;
            }
        }
    }

    private final void _skipComment() throws IOException {
        if (!isEnabled(Feature.ALLOW_COMMENTS)) {
            _reportUnexpectedChar(47, "maybe a (non-standard) comment? (not recognized as one since Feature 'ALLOW_COMMENTS' not enabled for parser)");
        }
        if (this._inputPtr >= this._inputEnd && !_loadMore()) {
            _reportInvalidEOF(" in a comment", null);
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int c = bArr[i] & 255;
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
        while (true) {
            if (this._inputPtr < this._inputEnd || _loadMore()) {
                byte[] bArr = this._inputBuffer;
                int i = this._inputPtr;
                this._inputPtr = i + 1;
                int i2 = bArr[i] & 255;
                int code = codes[i2];
                if (code != 0) {
                    if (code == 10) {
                        this._currInputRow++;
                        this._currInputRowStart = this._inputPtr;
                    } else if (code == 13) {
                        _skipCR();
                    } else if (code != 42) {
                        switch (code) {
                            case 2:
                                _skipUtf8_2();
                                break;
                            case 3:
                                _skipUtf8_3();
                                break;
                            case 4:
                                _skipUtf8_4(i2);
                                break;
                            default:
                                _reportInvalidChar(i2);
                                break;
                        }
                    } else if (this._inputPtr < this._inputEnd || _loadMore()) {
                        if (this._inputBuffer[this._inputPtr] == 47) {
                            this._inputPtr++;
                            return;
                        }
                    }
                }
            }
        }
        _reportInvalidEOF(" in a comment", null);
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
            if (this._inputPtr < this._inputEnd || _loadMore()) {
                byte[] bArr = this._inputBuffer;
                int i = this._inputPtr;
                this._inputPtr = i + 1;
                int i2 = bArr[i] & 255;
                int code = codes[i2];
                if (code != 0) {
                    if (code == 10) {
                        this._currInputRow++;
                        this._currInputRowStart = this._inputPtr;
                        return;
                    } else if (code == 13) {
                        _skipCR();
                        return;
                    } else if (code != 42) {
                        switch (code) {
                            case 2:
                                _skipUtf8_2();
                                break;
                            case 3:
                                _skipUtf8_3();
                                break;
                            case 4:
                                _skipUtf8_4(i2);
                                break;
                            default:
                                if (code >= 0) {
                                    break;
                                } else {
                                    _reportInvalidChar(i2);
                                    break;
                                }
                        }
                    }
                }
            } else {
                return;
            }
        }
    }

    /* access modifiers changed from: protected */
    public char _decodeEscaped() throws IOException {
        if (this._inputPtr >= this._inputEnd && !_loadMore()) {
            _reportInvalidEOF(" in character escape sequence", JsonToken.VALUE_STRING);
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        byte c = bArr[i];
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
                for (int i2 = 0; i2 < 4; i2++) {
                    if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                        _reportInvalidEOF(" in character escape sequence", JsonToken.VALUE_STRING);
                    }
                    byte[] bArr2 = this._inputBuffer;
                    int i3 = this._inputPtr;
                    this._inputPtr = i3 + 1;
                    byte ch = bArr2[i3];
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
        int d = nextByte();
        if ((d & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d & 255);
        }
        int c2 = (c << 6) | (d & 63);
        if (needed <= 1) {
            return c2;
        }
        int d2 = nextByte();
        if ((d2 & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d2 & 255);
        }
        int c3 = (c2 << 6) | (d2 & 63);
        if (needed <= 2) {
            return c3;
        }
        int d3 = nextByte();
        if ((d3 & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d3 & 255);
        }
        return (c3 << 6) | (d3 & 63);
    }

    private final int _decodeUtf8_2(int c) throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        byte d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        return ((c & 31) << 6) | (d & 63);
    }

    private final int _decodeUtf8_3(int c1) throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        int c12 = c1 & 15;
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        byte d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        int c = (c12 << 6) | (d & 63);
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr2 = this._inputBuffer;
        int i2 = this._inputPtr;
        this._inputPtr = i2 + 1;
        byte d2 = bArr2[i2];
        if ((d2 & 192) != 128) {
            _reportInvalidOther(d2 & 255, this._inputPtr);
        }
        return (c << 6) | (d2 & 63);
    }

    private final int _decodeUtf8_3fast(int c1) throws IOException {
        int c12 = c1 & 15;
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        byte d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        int c = (c12 << 6) | (d & 63);
        byte[] bArr2 = this._inputBuffer;
        int i2 = this._inputPtr;
        this._inputPtr = i2 + 1;
        byte d2 = bArr2[i2];
        if ((d2 & 192) != 128) {
            _reportInvalidOther(d2 & 255, this._inputPtr);
        }
        return (c << 6) | (d2 & 63);
    }

    private final int _decodeUtf8_4(int c) throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        byte d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        int c2 = ((c & 7) << 6) | (d & 63);
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr2 = this._inputBuffer;
        int i2 = this._inputPtr;
        this._inputPtr = i2 + 1;
        byte d2 = bArr2[i2];
        if ((d2 & 192) != 128) {
            _reportInvalidOther(d2 & 255, this._inputPtr);
        }
        int c3 = (c2 << 6) | (d2 & 63);
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr3 = this._inputBuffer;
        int i3 = this._inputPtr;
        this._inputPtr = i3 + 1;
        byte d3 = bArr3[i3];
        if ((d3 & 192) != 128) {
            _reportInvalidOther(d3 & 255, this._inputPtr);
        }
        return ((c3 << 6) | (d3 & 63)) - 65536;
    }

    private final void _skipUtf8_2() throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        byte c = bArr[i];
        if ((c & 192) != 128) {
            _reportInvalidOther(c & 255, this._inputPtr);
        }
    }

    private final void _skipUtf8_3() throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        byte c = bArr[i];
        if ((c & 192) != 128) {
            _reportInvalidOther(c & 255, this._inputPtr);
        }
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr2 = this._inputBuffer;
        int i2 = this._inputPtr;
        this._inputPtr = i2 + 1;
        byte c2 = bArr2[i2];
        if ((c2 & 192) != 128) {
            _reportInvalidOther(c2 & 255, this._inputPtr);
        }
    }

    private final void _skipUtf8_4(int c) throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        byte d = bArr[i];
        if ((d & 192) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr2 = this._inputBuffer;
        int i2 = this._inputPtr;
        this._inputPtr = i2 + 1;
        byte d2 = bArr2[i2];
        if ((d2 & 192) != 128) {
            _reportInvalidOther(d2 & 255, this._inputPtr);
        }
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr3 = this._inputBuffer;
        int i3 = this._inputPtr;
        this._inputPtr = i3 + 1;
        byte d3 = bArr3[i3];
        if ((d3 & 192) != 128) {
            _reportInvalidOther(d3 & 255, this._inputPtr);
        }
    }

    /* access modifiers changed from: protected */
    public final void _skipCR() throws IOException {
        if ((this._inputPtr < this._inputEnd || _loadMore()) && this._inputBuffer[this._inputPtr] == 10) {
            this._inputPtr++;
        }
        this._currInputRow++;
        this._currInputRowStart = this._inputPtr;
    }

    private int nextByte() throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            _loadMoreGuaranteed();
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        return bArr[i] & 255;
    }

    /* access modifiers changed from: protected */
    public void _reportInvalidToken(String matchedPart, int ptr) throws IOException {
        this._inputPtr = ptr;
        _reportInvalidToken(matchedPart, "'null', 'true', 'false' or NaN");
    }

    /* access modifiers changed from: protected */
    public void _reportInvalidToken(String matchedPart) throws IOException {
        _reportInvalidToken(matchedPart, "'null', 'true', 'false' or NaN");
    }

    /* access modifiers changed from: protected */
    public void _reportInvalidToken(String matchedPart, String msg) throws IOException {
        StringBuilder sb = new StringBuilder(matchedPart);
        while (true) {
            if (this._inputPtr >= this._inputEnd && !_loadMore()) {
                break;
            }
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            char c = (char) _decodeCharForError(bArr[i]);
            if (Character.isJavaIdentifierPart(c)) {
                sb.append(c);
                if (sb.length() >= 256) {
                    sb.append("...");
                    break;
                }
            } else {
                break;
            }
        }
        _reportError("Unrecognized token '%s': was expecting %s", sb, msg);
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
    public void _reportInvalidOther(int mask) throws JsonParseException {
        StringBuilder sb = new StringBuilder();
        sb.append("Invalid UTF-8 middle byte 0x");
        sb.append(Integer.toHexString(mask));
        _reportError(sb.toString());
    }

    /* access modifiers changed from: protected */
    public void _reportInvalidOther(int mask, int ptr) throws JsonParseException {
        this._inputPtr = ptr;
        _reportInvalidOther(mask);
    }

    /* access modifiers changed from: protected */
    public final byte[] _decodeBase64(Base64Variant b64variant) throws IOException {
        ByteArrayBuilder builder = _getByteArrayBuilder();
        while (true) {
            if (this._inputPtr >= this._inputEnd) {
                _loadMoreGuaranteed();
            }
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int ch = bArr[i] & 255;
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
                if (this._inputPtr >= this._inputEnd) {
                    _loadMoreGuaranteed();
                }
                byte[] bArr2 = this._inputBuffer;
                int i2 = this._inputPtr;
                this._inputPtr = i2 + 1;
                int ch2 = bArr2[i2] & 255;
                int bits2 = b64variant.decodeBase64Char(ch2);
                if (bits2 < 0) {
                    bits2 = _decodeBase64Escape(b64variant, ch2, 1);
                }
                int decodedData2 = (decodedData << 6) | bits2;
                if (this._inputPtr >= this._inputEnd) {
                    _loadMoreGuaranteed();
                }
                byte[] bArr3 = this._inputBuffer;
                int i3 = this._inputPtr;
                this._inputPtr = i3 + 1;
                int ch3 = bArr3[i3] & 255;
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
                        if (this._inputPtr >= this._inputEnd) {
                            _loadMoreGuaranteed();
                        }
                        byte[] bArr4 = this._inputBuffer;
                        int i4 = this._inputPtr;
                        this._inputPtr = i4 + 1;
                        int ch4 = bArr4[i4] & 255;
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
                if (this._inputPtr >= this._inputEnd) {
                    _loadMoreGuaranteed();
                }
                byte[] bArr5 = this._inputBuffer;
                int i5 = this._inputPtr;
                this._inputPtr = i5 + 1;
                int ch5 = bArr5[i5] & 255;
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
        if (this._currToken == JsonToken.FIELD_NAME) {
            JsonLocation jsonLocation = new JsonLocation(_getSourceReference(), this._currInputProcessed + ((long) (this._nameStartOffset - 1)), -1, this._nameStartRow, this._nameStartCol);
            return jsonLocation;
        }
        JsonLocation jsonLocation2 = new JsonLocation(_getSourceReference(), this._tokenInputTotal - 1, -1, this._tokenInputRow, this._tokenInputCol);
        return jsonLocation2;
    }

    public JsonLocation getCurrentLocation() {
        JsonLocation jsonLocation = new JsonLocation(_getSourceReference(), this._currInputProcessed + ((long) this._inputPtr), -1, this._currInputRow, (this._inputPtr - this._currInputRowStart) + 1);
        return jsonLocation;
    }

    private final void _updateLocation() {
        this._tokenInputRow = this._currInputRow;
        int ptr = this._inputPtr;
        this._tokenInputTotal = this._currInputProcessed + ((long) ptr);
        this._tokenInputCol = ptr - this._currInputRowStart;
    }

    private final void _updateNameLocation() {
        this._nameStartRow = this._currInputRow;
        int ptr = this._inputPtr;
        this._nameStartOffset = ptr;
        this._nameStartCol = ptr - this._currInputRowStart;
    }

    private final JsonToken _closeScope(int i) throws JsonParseException {
        if (i == 125) {
            _closeObjectScope();
            JsonToken jsonToken = JsonToken.END_OBJECT;
            this._currToken = jsonToken;
            return jsonToken;
        }
        _closeArrayScope();
        JsonToken jsonToken2 = JsonToken.END_ARRAY;
        this._currToken = jsonToken2;
        return jsonToken2;
    }

    private final void _closeArrayScope() throws JsonParseException {
        _updateLocation();
        if (!this._parsingContext.inArray()) {
            _reportMismatchedEndMarker(93, '}');
        }
        this._parsingContext = this._parsingContext.clearAndGetParent();
    }

    private final void _closeObjectScope() throws JsonParseException {
        _updateLocation();
        if (!this._parsingContext.inObject()) {
            _reportMismatchedEndMarker(125, ']');
        }
        this._parsingContext = this._parsingContext.clearAndGetParent();
    }
}
