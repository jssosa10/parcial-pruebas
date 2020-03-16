package com.fasterxml.jackson.core.json.async;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.io.CharTypes;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.util.TextBuffer;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.google.android.exoplayer2.extractor.ts.PsExtractor;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.lang3.ClassUtils;

public class NonBlockingJsonParser extends NonBlockingJsonParserBase implements ByteArrayFeeder {
    protected static final int[] _icLatin1 = CharTypes.getInputCodeLatin1();
    private static final int[] _icUTF8 = CharTypes.getInputCodeUtf8();
    protected byte[] _inputBuffer = NO_BYTES;
    protected int _origBufferLen;

    public NonBlockingJsonParser(IOContext ctxt, int parserFeatures, ByteQuadsCanonicalizer sym) {
        super(ctxt, parserFeatures, sym);
    }

    public ByteArrayFeeder getNonBlockingInputFeeder() {
        return this;
    }

    public final boolean needMoreInput() {
        return this._inputPtr >= this._inputEnd && !this._endOfInput;
    }

    public void feedInput(byte[] buf, int start, int end) throws IOException {
        if (this._inputPtr < this._inputEnd) {
            _reportError("Still have %d undecoded bytes, should not call 'feedInput'", Integer.valueOf(this._inputEnd - this._inputPtr));
        }
        if (end < start) {
            _reportError("Input end (%d) may not be before start (%d)", Integer.valueOf(end), Integer.valueOf(start));
        }
        if (this._endOfInput) {
            _reportError("Already closed, can not feed more input");
        }
        this._currInputProcessed += (long) this._origBufferLen;
        this._currInputRowStart = start - (this._inputEnd - this._currInputRowStart);
        this._inputBuffer = buf;
        this._inputPtr = start;
        this._inputEnd = end;
        this._origBufferLen = end - start;
    }

    public void endOfInput() {
        this._endOfInput = true;
    }

    public int releaseBuffered(OutputStream out) throws IOException {
        int avail = this._inputEnd - this._inputPtr;
        if (avail > 0) {
            out.write(this._inputBuffer, this._inputPtr, avail);
        }
        return avail;
    }

    /* access modifiers changed from: protected */
    public char _decodeEscaped() throws IOException {
        VersionUtil.throwInternal();
        return ' ';
    }

    public JsonToken nextToken() throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            if (this._closed) {
                return null;
            }
            if (!this._endOfInput) {
                return JsonToken.NOT_AVAILABLE;
            }
            if (this._currToken == JsonToken.NOT_AVAILABLE) {
                return _finishTokenWithEOF();
            }
            return _eofAsNextToken();
        } else if (this._currToken == JsonToken.NOT_AVAILABLE) {
            return _finishToken();
        } else {
            this._numTypesValid = 0;
            this._tokenInputTotal = this._currInputProcessed + ((long) this._inputPtr);
            this._binaryValue = null;
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int ch = bArr[i] & 255;
            switch (this._majorState) {
                case 0:
                    return _startDocument(ch);
                case 1:
                    return _startValue(ch);
                case 2:
                    return _startFieldName(ch);
                case 3:
                    return _startFieldNameAfterComma(ch);
                case 4:
                    return _startValueExpectColon(ch);
                case 5:
                    return _startValue(ch);
                case 6:
                    return _startValueExpectComma(ch);
                default:
                    VersionUtil.throwInternal();
                    return null;
            }
        }
    }

    /* access modifiers changed from: protected */
    public final JsonToken _finishToken() throws IOException {
        switch (this._minorState) {
            case 1:
                return _finishBOM(this._pending32);
            case 4:
                byte[] bArr = this._inputBuffer;
                int i = this._inputPtr;
                this._inputPtr = i + 1;
                return _startFieldName(bArr[i] & 255);
            case 5:
                byte[] bArr2 = this._inputBuffer;
                int i2 = this._inputPtr;
                this._inputPtr = i2 + 1;
                return _startFieldNameAfterComma(bArr2[i2] & 255);
            case 7:
                return _parseEscapedName(this._quadLength, this._pending32, this._pendingBytes);
            case 8:
                return _finishFieldWithEscape();
            case 9:
                return _finishAposName(this._quadLength, this._pending32, this._pendingBytes);
            case 10:
                return _finishUnquotedName(this._quadLength, this._pending32, this._pendingBytes);
            case 12:
                byte[] bArr3 = this._inputBuffer;
                int i3 = this._inputPtr;
                this._inputPtr = i3 + 1;
                return _startValue(bArr3[i3] & 255);
            case 13:
                byte[] bArr4 = this._inputBuffer;
                int i4 = this._inputPtr;
                this._inputPtr = i4 + 1;
                return _startValueExpectComma(bArr4[i4] & 255);
            case 14:
                byte[] bArr5 = this._inputBuffer;
                int i5 = this._inputPtr;
                this._inputPtr = i5 + 1;
                return _startValueExpectColon(bArr5[i5] & 255);
            case 15:
                byte[] bArr6 = this._inputBuffer;
                int i6 = this._inputPtr;
                this._inputPtr = i6 + 1;
                return _startValueAfterComma(bArr6[i6] & 255);
            case 16:
                return _finishKeywordToken("null", this._pending32, JsonToken.VALUE_NULL);
            case 17:
                return _finishKeywordToken("true", this._pending32, JsonToken.VALUE_TRUE);
            case 18:
                return _finishKeywordToken("false", this._pending32, JsonToken.VALUE_FALSE);
            case 19:
                return _finishNonStdToken(this._nonStdTokenType, this._pending32);
            case 23:
                byte[] bArr7 = this._inputBuffer;
                int i7 = this._inputPtr;
                this._inputPtr = i7 + 1;
                return _finishNumberMinus(bArr7[i7] & 255);
            case 24:
                return _finishNumberLeadingZeroes();
            case 25:
                return _finishNumberLeadingNegZeroes();
            case 26:
                return _finishNumberIntegralPart(this._textBuffer.getBufferWithoutReset(), this._textBuffer.getCurrentSegmentSize());
            case 30:
                return _finishFloatFraction();
            case 31:
                byte[] bArr8 = this._inputBuffer;
                int i8 = this._inputPtr;
                this._inputPtr = i8 + 1;
                return _finishFloatExponent(true, bArr8[i8] & 255);
            case 32:
                byte[] bArr9 = this._inputBuffer;
                int i9 = this._inputPtr;
                this._inputPtr = i9 + 1;
                return _finishFloatExponent(false, bArr9[i9] & 255);
            case 40:
                return _finishRegularString();
            case 41:
                int c = _decodeSplitEscaped(this._quoted32, this._quotedDigits);
                if (c < 0) {
                    return JsonToken.NOT_AVAILABLE;
                }
                this._textBuffer.append((char) c);
                if (this._minorStateAfterSplit == 45) {
                    return _finishAposString();
                }
                return _finishRegularString();
            case 42:
                TextBuffer textBuffer = this._textBuffer;
                int i10 = this._pending32;
                byte[] bArr10 = this._inputBuffer;
                int i11 = this._inputPtr;
                this._inputPtr = i11 + 1;
                textBuffer.append((char) _decodeUTF8_2(i10, bArr10[i11]));
                if (this._minorStateAfterSplit == 45) {
                    return _finishAposString();
                }
                return _finishRegularString();
            case 43:
                int i12 = this._pending32;
                int i13 = this._pendingBytes;
                byte[] bArr11 = this._inputBuffer;
                int i14 = this._inputPtr;
                this._inputPtr = i14 + 1;
                if (!_decodeSplitUTF8_3(i12, i13, bArr11[i14])) {
                    return JsonToken.NOT_AVAILABLE;
                }
                if (this._minorStateAfterSplit == 45) {
                    return _finishAposString();
                }
                return _finishRegularString();
            case 44:
                int i15 = this._pending32;
                int i16 = this._pendingBytes;
                byte[] bArr12 = this._inputBuffer;
                int i17 = this._inputPtr;
                this._inputPtr = i17 + 1;
                if (!_decodeSplitUTF8_4(i15, i16, bArr12[i17])) {
                    return JsonToken.NOT_AVAILABLE;
                }
                if (this._minorStateAfterSplit == 45) {
                    return _finishAposString();
                }
                return _finishRegularString();
            case 45:
                return _finishAposString();
            case 50:
                return _finishErrorToken();
            case 51:
                return _startSlashComment(this._pending32);
            case 52:
                return _finishCComment(this._pending32, true);
            case 53:
                return _finishCComment(this._pending32, false);
            case 54:
                return _finishCppComment(this._pending32);
            case 55:
                return _finishHashComment(this._pending32);
            default:
                VersionUtil.throwInternal();
                return null;
        }
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0041, code lost:
        _reportInvalidEOF(": was expecting closing '*/' for comment", com.fasterxml.jackson.core.JsonToken.NOT_AVAILABLE);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x004c, code lost:
        return _eofAsNextToken();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0055, code lost:
        return _valueComplete(com.fasterxml.jackson.core.JsonToken.VALUE_NUMBER_FLOAT);
     */
    public final JsonToken _finishTokenWithEOF() throws IOException {
        JsonToken t = this._currToken;
        int i = this._minorState;
        if (i == 3) {
            return _eofAsNextToken();
        }
        if (i == 12) {
            return _eofAsNextToken();
        }
        if (i == 50) {
            return _finishErrorTokenWithEOF();
        }
        switch (i) {
            case 16:
                return _finishKeywordTokenWithEOF("null", this._pending32, JsonToken.VALUE_NULL);
            case 17:
                return _finishKeywordTokenWithEOF("true", this._pending32, JsonToken.VALUE_TRUE);
            case 18:
                return _finishKeywordTokenWithEOF("false", this._pending32, JsonToken.VALUE_FALSE);
            case 19:
                return _finishNonStdTokenWithEOF(this._nonStdTokenType, this._pending32);
            default:
                switch (i) {
                    case 24:
                    case 25:
                        return _valueCompleteInt(0, "0");
                    case 26:
                        int len = this._textBuffer.getCurrentSegmentSize();
                        if (this._numberNegative) {
                            len--;
                        }
                        this._intLength = len;
                        return _valueComplete(JsonToken.VALUE_NUMBER_INT);
                    default:
                        switch (i) {
                            case 30:
                                this._expLength = 0;
                                break;
                            case 31:
                                _reportInvalidEOF(": was expecting fraction after exponent marker", JsonToken.VALUE_NUMBER_FLOAT);
                                break;
                            case 32:
                                break;
                            default:
                                switch (i) {
                                    case 52:
                                    case 53:
                                        break;
                                    case 54:
                                    case 55:
                                        break;
                                    default:
                                        StringBuilder sb = new StringBuilder();
                                        sb.append(": was expecting rest of token (internal state: ");
                                        sb.append(this._minorState);
                                        sb.append(")");
                                        _reportInvalidEOF(sb.toString(), this._currToken);
                                        return t;
                                }
                        }
                }
        }
    }

    private final JsonToken _startDocument(int ch) throws IOException {
        int ch2 = ch & 255;
        if (ch2 == 239 && this._minorState != 1) {
            return _finishBOM(1);
        }
        while (ch2 <= 32) {
            if (ch2 != 32) {
                if (ch2 == 10) {
                    this._currInputRow++;
                    this._currInputRowStart = this._inputPtr;
                } else if (ch2 == 13) {
                    this._currInputRowAlt++;
                    this._currInputRowStart = this._inputPtr;
                } else if (ch2 != 9) {
                    _throwInvalidSpace(ch2);
                }
            }
            if (this._inputPtr >= this._inputEnd) {
                this._minorState = 3;
                if (this._closed) {
                    return null;
                }
                if (this._endOfInput) {
                    return _eofAsNextToken();
                }
                return JsonToken.NOT_AVAILABLE;
            }
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            ch2 = bArr[i] & 255;
        }
        return _startValue(ch2);
    }

    private final JsonToken _finishBOM(int bytesHandled) throws IOException {
        while (this._inputPtr < this._inputEnd) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int ch = bArr[i] & 255;
            switch (bytesHandled) {
                case 1:
                    if (ch == 187) {
                        break;
                    } else {
                        _reportError("Unexpected byte 0x%02x following 0xEF; should get 0xBB as second byte UTF-8 BOM", Integer.valueOf(ch));
                        break;
                    }
                case 2:
                    if (ch == 191) {
                        break;
                    } else {
                        _reportError("Unexpected byte 0x%02x following 0xEF 0xBB; should get 0xBF as third byte of UTF-8 BOM", Integer.valueOf(ch));
                        break;
                    }
                case 3:
                    this._currInputProcessed -= 3;
                    return _startDocument(ch);
            }
            bytesHandled++;
        }
        this._pending32 = bytesHandled;
        this._minorState = 1;
        JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken;
        return jsonToken;
    }

    private final JsonToken _startFieldName(int ch) throws IOException {
        if (ch <= 32) {
            ch = _skipWS(ch);
            if (ch <= 0) {
                this._minorState = 4;
                return this._currToken;
            }
        }
        _updateTokenLocation();
        if (ch == 34) {
            if (this._inputPtr + 13 <= this._inputEnd) {
                String n = _fastParseName();
                if (n != null) {
                    return _fieldComplete(n);
                }
            }
            return _parseEscapedName(0, 0, 0);
        } else if (ch == 125) {
            return _closeObjectScope();
        } else {
            return _handleOddName(ch);
        }
    }

    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r6v2, types: [byte, int] */
    /* JADX WARNING: Multi-variable type inference failed */
    private final JsonToken _startFieldNameAfterComma(int ch) throws IOException {
        if (ch <= 32) {
            ch = _skipWS(ch);
            if (ch <= 0) {
                this._minorState = 5;
                return this._currToken;
            }
        }
        if (ch != 44) {
            if (ch == 125) {
                return _closeObjectScope();
            }
            if (ch == 35) {
                return _finishHashComment(5);
            }
            if (ch == 47) {
                return _startSlashComment(5);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("was expecting comma to separate ");
            sb.append(this._parsingContext.typeDesc());
            sb.append(" entries");
            _reportUnexpectedChar(ch, sb.toString());
        }
        int ptr = this._inputPtr;
        if (ptr >= this._inputEnd) {
            this._minorState = 4;
            JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
            this._currToken = jsonToken;
            return jsonToken;
        }
        int ch2 = this._inputBuffer[ptr];
        this._inputPtr = ptr + 1;
        if (ch2 <= 32) {
            ch2 = _skipWS(ch2);
            if (ch2 <= 0) {
                this._minorState = 4;
                return this._currToken;
            }
        }
        _updateTokenLocation();
        if (ch2 == 34) {
            if (this._inputPtr + 13 <= this._inputEnd) {
                String n = _fastParseName();
                if (n != null) {
                    return _fieldComplete(n);
                }
            }
            return _parseEscapedName(0, 0, 0);
        } else if (ch2 != 125 || !Feature.ALLOW_TRAILING_COMMA.enabledIn(this._features)) {
            return _handleOddName(ch2);
        } else {
            return _closeObjectScope();
        }
    }

    private final JsonToken _startValue(int ch) throws IOException {
        if (ch <= 32) {
            ch = _skipWS(ch);
            if (ch <= 0) {
                this._minorState = 12;
                return this._currToken;
            }
        }
        _updateTokenLocation();
        if (ch == 34) {
            return _startString();
        }
        if (ch == 35) {
            return _finishHashComment(12);
        }
        if (ch == 45) {
            return _startNegativeNumber();
        }
        if (ch == 91) {
            return _startArrayScope();
        }
        if (ch == 93) {
            return _closeArrayScope();
        }
        if (ch == 102) {
            return _startFalseToken();
        }
        if (ch == 110) {
            return _startNullToken();
        }
        if (ch == 116) {
            return _startTrueToken();
        }
        if (ch == 123) {
            return _startObjectScope();
        }
        if (ch == 125) {
            return _closeObjectScope();
        }
        switch (ch) {
            case 47:
                return _startSlashComment(12);
            case 48:
                return _startNumberLeadingZero();
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
                return _startPositiveNumber(ch);
            default:
                return _startUnexpectedValue(false, ch);
        }
    }

    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r8v2, types: [byte, int] */
    /* JADX WARNING: Multi-variable type inference failed */
    private final JsonToken _startValueExpectComma(int ch) throws IOException {
        if (ch <= 32) {
            ch = _skipWS(ch);
            if (ch <= 0) {
                this._minorState = 13;
                return this._currToken;
            }
        }
        if (ch != 44) {
            if (ch == 93) {
                return _closeArrayScope();
            }
            if (ch == 125) {
                return _closeObjectScope();
            }
            if (ch == 47) {
                return _startSlashComment(13);
            }
            if (ch == 35) {
                return _finishHashComment(13);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("was expecting comma to separate ");
            sb.append(this._parsingContext.typeDesc());
            sb.append(" entries");
            _reportUnexpectedChar(ch, sb.toString());
        }
        int ptr = this._inputPtr;
        if (ptr >= this._inputEnd) {
            this._minorState = 15;
            JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
            this._currToken = jsonToken;
            return jsonToken;
        }
        int ch2 = this._inputBuffer[ptr];
        this._inputPtr = ptr + 1;
        if (ch2 <= 32) {
            ch2 = _skipWS(ch2);
            if (ch2 <= 0) {
                this._minorState = 15;
                return this._currToken;
            }
        }
        _updateTokenLocation();
        if (ch2 == 34) {
            return _startString();
        }
        if (ch2 == 35) {
            return _finishHashComment(15);
        }
        if (ch2 == 45) {
            return _startNegativeNumber();
        }
        if (ch2 == 91) {
            return _startArrayScope();
        }
        if (ch2 != 93) {
            if (ch2 == 102) {
                return _startFalseToken();
            }
            if (ch2 == 110) {
                return _startNullToken();
            }
            if (ch2 == 116) {
                return _startTrueToken();
            }
            if (ch2 == 123) {
                return _startObjectScope();
            }
            if (ch2 != 125) {
                switch (ch2) {
                    case 47:
                        return _startSlashComment(15);
                    case 48:
                        return _startNumberLeadingZero();
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                        return _startPositiveNumber(ch2);
                }
            } else if (isEnabled(Feature.ALLOW_TRAILING_COMMA)) {
                return _closeObjectScope();
            }
        } else if (isEnabled(Feature.ALLOW_TRAILING_COMMA)) {
            return _closeArrayScope();
        }
        return _startUnexpectedValue(true, ch2);
    }

    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r6v2, types: [byte, int] */
    /* JADX WARNING: Multi-variable type inference failed */
    private final JsonToken _startValueExpectColon(int ch) throws IOException {
        if (ch <= 32) {
            ch = _skipWS(ch);
            if (ch <= 0) {
                this._minorState = 14;
                return this._currToken;
            }
        }
        if (ch != 58) {
            if (ch == 47) {
                return _startSlashComment(14);
            }
            if (ch == 35) {
                return _finishHashComment(14);
            }
            _reportUnexpectedChar(ch, "was expecting a colon to separate field name and value");
        }
        int ptr = this._inputPtr;
        if (ptr >= this._inputEnd) {
            this._minorState = 12;
            JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
            this._currToken = jsonToken;
            return jsonToken;
        }
        int ch2 = this._inputBuffer[ptr];
        this._inputPtr = ptr + 1;
        if (ch2 <= 32) {
            ch2 = _skipWS(ch2);
            if (ch2 <= 0) {
                this._minorState = 12;
                return this._currToken;
            }
        }
        _updateTokenLocation();
        if (ch2 == 34) {
            return _startString();
        }
        if (ch2 == 35) {
            return _finishHashComment(12);
        }
        if (ch2 == 45) {
            return _startNegativeNumber();
        }
        if (ch2 == 91) {
            return _startArrayScope();
        }
        if (ch2 == 102) {
            return _startFalseToken();
        }
        if (ch2 == 110) {
            return _startNullToken();
        }
        if (ch2 == 116) {
            return _startTrueToken();
        }
        if (ch2 == 123) {
            return _startObjectScope();
        }
        switch (ch2) {
            case 47:
                return _startSlashComment(12);
            case 48:
                return _startNumberLeadingZero();
            case 49:
            case 50:
            case 51:
            case 52:
            case 53:
            case 54:
            case 55:
            case 56:
            case 57:
                return _startPositiveNumber(ch2);
            default:
                return _startUnexpectedValue(false, ch2);
        }
    }

    private final JsonToken _startValueAfterComma(int ch) throws IOException {
        if (ch <= 32) {
            ch = _skipWS(ch);
            if (ch <= 0) {
                this._minorState = 15;
                return this._currToken;
            }
        }
        _updateTokenLocation();
        if (ch == 34) {
            return _startString();
        }
        if (ch == 35) {
            return _finishHashComment(15);
        }
        if (ch == 45) {
            return _startNegativeNumber();
        }
        if (ch == 91) {
            return _startArrayScope();
        }
        if (ch != 93) {
            if (ch == 102) {
                return _startFalseToken();
            }
            if (ch == 110) {
                return _startNullToken();
            }
            if (ch == 116) {
                return _startTrueToken();
            }
            if (ch == 123) {
                return _startObjectScope();
            }
            if (ch != 125) {
                switch (ch) {
                    case 47:
                        return _startSlashComment(15);
                    case 48:
                        return _startNumberLeadingZero();
                    case 49:
                    case 50:
                    case 51:
                    case 52:
                    case 53:
                    case 54:
                    case 55:
                    case 56:
                    case 57:
                        return _startPositiveNumber(ch);
                }
            } else if (isEnabled(Feature.ALLOW_TRAILING_COMMA)) {
                return _closeObjectScope();
            }
        } else if (isEnabled(Feature.ALLOW_TRAILING_COMMA)) {
            return _closeArrayScope();
        }
        return _startUnexpectedValue(true, ch);
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0025, code lost:
        if (r2._parsingContext.inArray() == false) goto L_0x0055;
     */
    public JsonToken _startUnexpectedValue(boolean leadingComma, int ch) throws IOException {
        if (ch != 39) {
            if (ch == 73) {
                return _finishNonStdToken(1, 1);
            }
            if (ch == 78) {
                return _finishNonStdToken(0, 1);
            }
            if (ch != 93) {
                if (ch != 125) {
                    switch (ch) {
                        case 43:
                            return _finishNonStdToken(2, 1);
                        case 44:
                            break;
                    }
                }
            }
            if (isEnabled(Feature.ALLOW_MISSING_VALUES)) {
                this._inputPtr--;
                return _valueComplete(JsonToken.VALUE_NULL);
            }
        } else if (isEnabled(Feature.ALLOW_SINGLE_QUOTES)) {
            return _startAposString();
        }
        _reportUnexpectedChar(ch, "expected a valid value (number, String, array, object, 'true', 'false' or 'null')");
        return null;
    }

    private final int _skipWS(int ch) throws IOException {
        do {
            if (ch != 32) {
                if (ch == 10) {
                    this._currInputRow++;
                    this._currInputRowStart = this._inputPtr;
                } else if (ch == 13) {
                    this._currInputRowAlt++;
                    this._currInputRowStart = this._inputPtr;
                } else if (ch != 9) {
                    _throwInvalidSpace(ch);
                }
            }
            if (this._inputPtr >= this._inputEnd) {
                this._currToken = JsonToken.NOT_AVAILABLE;
                return 0;
            }
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            ch = bArr[i] & 255;
        } while (ch <= 32);
        return ch;
    }

    private final JsonToken _startSlashComment(int fromMinorState) throws IOException {
        if (!Feature.ALLOW_COMMENTS.enabledIn(this._features)) {
            _reportUnexpectedChar(47, "maybe a (non-standard) comment? (not recognized as one since Feature 'ALLOW_COMMENTS' not enabled for parser)");
        }
        if (this._inputPtr >= this._inputEnd) {
            this._pending32 = fromMinorState;
            this._minorState = 51;
            JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
            this._currToken = jsonToken;
            return jsonToken;
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        byte ch = bArr[i];
        if (ch == 42) {
            return _finishCComment(fromMinorState, false);
        }
        if (ch == 47) {
            return _finishCppComment(fromMinorState);
        }
        _reportUnexpectedChar(ch & 255, "was expecting either '*' or '/' for a comment");
        return null;
    }

    private final JsonToken _finishHashComment(int fromMinorState) throws IOException {
        if (!Feature.ALLOW_YAML_COMMENTS.enabledIn(this._features)) {
            _reportUnexpectedChar(35, "maybe a (non-standard) comment? (not recognized as one since Feature 'ALLOW_YAML_COMMENTS' not enabled for parser)");
        }
        while (this._inputPtr < this._inputEnd) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int ch = bArr[i] & 255;
            if (ch < 32) {
                if (ch == 10) {
                    this._currInputRow++;
                    this._currInputRowStart = this._inputPtr;
                } else if (ch == 13) {
                    this._currInputRowAlt++;
                    this._currInputRowStart = this._inputPtr;
                } else if (ch != 9) {
                    _throwInvalidSpace(ch);
                }
                return _startAfterComment(fromMinorState);
            }
        }
        this._minorState = 55;
        this._pending32 = fromMinorState;
        JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken;
        return jsonToken;
    }

    private final JsonToken _finishCppComment(int fromMinorState) throws IOException {
        while (this._inputPtr < this._inputEnd) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int ch = bArr[i] & 255;
            if (ch < 32) {
                if (ch == 10) {
                    this._currInputRow++;
                    this._currInputRowStart = this._inputPtr;
                } else if (ch == 13) {
                    this._currInputRowAlt++;
                    this._currInputRowStart = this._inputPtr;
                } else if (ch != 9) {
                    _throwInvalidSpace(ch);
                }
                return _startAfterComment(fromMinorState);
            }
        }
        this._minorState = 54;
        this._pending32 = fromMinorState;
        JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken;
        return jsonToken;
    }

    private final JsonToken _finishCComment(int fromMinorState, boolean gotStar) throws IOException {
        while (this._inputPtr < this._inputEnd) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int ch = bArr[i] & 255;
            if (ch < 32) {
                if (ch == 10) {
                    this._currInputRow++;
                    this._currInputRowStart = this._inputPtr;
                } else if (ch == 13) {
                    this._currInputRowAlt++;
                    this._currInputRowStart = this._inputPtr;
                } else if (ch != 9) {
                    _throwInvalidSpace(ch);
                }
            } else if (ch == 42) {
                gotStar = true;
            } else if (ch == 47 && gotStar) {
                return _startAfterComment(fromMinorState);
            }
            gotStar = false;
        }
        this._minorState = gotStar ? 52 : 53;
        this._pending32 = fromMinorState;
        JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken;
        return jsonToken;
    }

    private final JsonToken _startAfterComment(int fromMinorState) throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            this._minorState = fromMinorState;
            JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
            this._currToken = jsonToken;
            return jsonToken;
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int ch = bArr[i] & 255;
        switch (fromMinorState) {
            case 4:
                return _startFieldName(ch);
            case 5:
                return _startFieldNameAfterComma(ch);
            default:
                switch (fromMinorState) {
                    case 12:
                        return _startValue(ch);
                    case 13:
                        return _startValueExpectComma(ch);
                    case 14:
                        return _startValueExpectColon(ch);
                    case 15:
                        return _startValueAfterComma(ch);
                    default:
                        VersionUtil.throwInternal();
                        return null;
                }
        }
    }

    /* access modifiers changed from: protected */
    public JsonToken _startFalseToken() throws IOException {
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
                                return _valueComplete(JsonToken.VALUE_FALSE);
                            }
                        }
                    }
                }
            }
        }
        this._minorState = 18;
        return _finishKeywordToken("false", 1, JsonToken.VALUE_FALSE);
    }

    /* access modifiers changed from: protected */
    public JsonToken _startTrueToken() throws IOException {
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
                            return _valueComplete(JsonToken.VALUE_TRUE);
                        }
                    }
                    int ch2 = ptr4;
                }
            } else {
                int i = ptr2;
            }
        }
        this._minorState = 17;
        return _finishKeywordToken("true", 1, JsonToken.VALUE_TRUE);
    }

    /* access modifiers changed from: protected */
    public JsonToken _startNullToken() throws IOException {
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
                            return _valueComplete(JsonToken.VALUE_NULL);
                        }
                    }
                    int ch2 = ptr4;
                }
            } else {
                int i = ptr2;
            }
        }
        this._minorState = 16;
        return _finishKeywordToken("null", 1, JsonToken.VALUE_NULL);
    }

    /* access modifiers changed from: protected */
    public JsonToken _finishKeywordToken(String expToken, int matched, JsonToken result) throws IOException {
        int end = expToken.length();
        while (this._inputPtr < this._inputEnd) {
            byte ch = this._inputBuffer[this._inputPtr];
            if (matched == end) {
                if (ch < 48 || ch == 93 || ch == 125) {
                    return _valueComplete(result);
                }
            } else if (ch == expToken.charAt(matched)) {
                matched++;
                this._inputPtr++;
            }
            this._minorState = 50;
            this._textBuffer.resetWithCopy(expToken, 0, matched);
            return _finishErrorToken();
        }
        this._pending32 = matched;
        JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken;
        return jsonToken;
    }

    /* access modifiers changed from: protected */
    public JsonToken _finishKeywordTokenWithEOF(String expToken, int matched, JsonToken result) throws IOException {
        if (matched == expToken.length()) {
            this._currToken = result;
            return result;
        }
        this._textBuffer.resetWithCopy(expToken, 0, matched);
        return _finishErrorTokenWithEOF();
    }

    /* access modifiers changed from: protected */
    public JsonToken _finishNonStdToken(int type, int matched) throws IOException {
        String expToken = _nonStdToken(type);
        int end = expToken.length();
        while (this._inputPtr < this._inputEnd) {
            byte ch = this._inputBuffer[this._inputPtr];
            if (matched == end) {
                if (ch < 48 || ch == 93 || ch == 125) {
                    return _valueNonStdNumberComplete(type);
                }
            } else if (ch == expToken.charAt(matched)) {
                matched++;
                this._inputPtr++;
            }
            this._minorState = 50;
            this._textBuffer.resetWithCopy(expToken, 0, matched);
            return _finishErrorToken();
        }
        this._nonStdTokenType = type;
        this._pending32 = matched;
        this._minorState = 19;
        JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken;
        return jsonToken;
    }

    /* access modifiers changed from: protected */
    public JsonToken _finishNonStdTokenWithEOF(int type, int matched) throws IOException {
        String expToken = _nonStdToken(type);
        if (matched == expToken.length()) {
            return _valueNonStdNumberComplete(type);
        }
        this._textBuffer.resetWithCopy(expToken, 0, matched);
        return _finishErrorTokenWithEOF();
    }

    /* access modifiers changed from: protected */
    public JsonToken _finishErrorToken() throws IOException {
        while (this._inputPtr < this._inputEnd) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            char ch = (char) bArr[i];
            if (Character.isJavaIdentifierPart(ch)) {
                this._textBuffer.append(ch);
                if (this._textBuffer.size() >= 256) {
                }
            }
            return _reportErrorToken(this._textBuffer.contentsAsString());
        }
        JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken;
        return jsonToken;
    }

    /* access modifiers changed from: protected */
    public JsonToken _finishErrorTokenWithEOF() throws IOException {
        return _reportErrorToken(this._textBuffer.contentsAsString());
    }

    /* access modifiers changed from: protected */
    public JsonToken _reportErrorToken(String actualToken) throws IOException {
        _reportError("Unrecognized token '%s': was expecting %s", this._textBuffer.contentsAsString(), "'null', 'true' or 'false'");
        return JsonToken.NOT_AVAILABLE;
    }

    /* access modifiers changed from: protected */
    public JsonToken _startPositiveNumber(int ch) throws IOException {
        this._numberNegative = false;
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        outBuf[0] = (char) ch;
        if (this._inputPtr >= this._inputEnd) {
            this._minorState = 26;
            this._textBuffer.setCurrentLength(1);
            JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
            this._currToken = jsonToken;
            return jsonToken;
        }
        int outPtr = 1;
        int ch2 = this._inputBuffer[this._inputPtr] & 255;
        while (true) {
            if (ch2 < 48) {
                if (ch2 == 46) {
                    this._intLength = outPtr;
                    this._inputPtr++;
                    return _startFloat(outBuf, outPtr, ch2);
                }
            } else if (ch2 <= 57) {
                if (outPtr >= outBuf.length) {
                    outBuf = this._textBuffer.expandCurrentSegment();
                }
                int outPtr2 = outPtr + 1;
                outBuf[outPtr] = (char) ch2;
                int i = this._inputPtr + 1;
                this._inputPtr = i;
                if (i >= this._inputEnd) {
                    this._minorState = 26;
                    this._textBuffer.setCurrentLength(outPtr2);
                    JsonToken jsonToken2 = JsonToken.NOT_AVAILABLE;
                    this._currToken = jsonToken2;
                    return jsonToken2;
                }
                ch2 = this._inputBuffer[this._inputPtr] & 255;
                outPtr = outPtr2;
            } else if (ch2 == 101 || ch2 == 69) {
                this._intLength = outPtr;
                this._inputPtr++;
                return _startFloat(outBuf, outPtr, ch2);
            }
        }
        this._intLength = outPtr;
        this._textBuffer.setCurrentLength(outPtr);
        return _valueComplete(JsonToken.VALUE_NUMBER_INT);
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r1v4, types: [byte] */
    public JsonToken _startNegativeNumber() throws IOException {
        this._numberNegative = true;
        if (this._inputPtr >= this._inputEnd) {
            this._minorState = 23;
            JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
            this._currToken = jsonToken;
            return jsonToken;
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int ch = bArr[i] & 255;
        if (ch <= 48) {
            if (ch == 48) {
                return _finishNumberLeadingNegZeroes();
            }
            reportUnexpectedNumberChar(ch, "expected digit (0-9) to follow minus sign, for valid numeric value");
        } else if (ch > 57) {
            if (ch == 73) {
                return _finishNonStdToken(3, 2);
            }
            reportUnexpectedNumberChar(ch, "expected digit (0-9) to follow minus sign, for valid numeric value");
        }
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        outBuf[0] = '-';
        outBuf[1] = (char) ch;
        if (this._inputPtr >= this._inputEnd) {
            this._minorState = 26;
            this._textBuffer.setCurrentLength(2);
            this._intLength = 1;
            JsonToken jsonToken2 = JsonToken.NOT_AVAILABLE;
            this._currToken = jsonToken2;
            return jsonToken2;
        }
        int ch2 = this._inputBuffer[this._inputPtr];
        int outPtr = 2;
        while (true) {
            if (ch2 < 48) {
                if (ch2 == 46) {
                    this._intLength = outPtr - 1;
                    this._inputPtr++;
                    return _startFloat(outBuf, outPtr, ch2);
                }
            } else if (ch2 <= 57) {
                if (outPtr >= outBuf.length) {
                    outBuf = this._textBuffer.expandCurrentSegment();
                }
                int outPtr2 = outPtr + 1;
                outBuf[outPtr] = (char) ch2;
                int i2 = this._inputPtr + 1;
                this._inputPtr = i2;
                if (i2 >= this._inputEnd) {
                    this._minorState = 26;
                    this._textBuffer.setCurrentLength(outPtr2);
                    JsonToken jsonToken3 = JsonToken.NOT_AVAILABLE;
                    this._currToken = jsonToken3;
                    return jsonToken3;
                }
                ch2 = this._inputBuffer[this._inputPtr] & 255;
                outPtr = outPtr2;
            } else if (ch2 == 101 || ch2 == 69) {
                this._intLength = outPtr - 1;
                this._inputPtr++;
                return _startFloat(outBuf, outPtr, ch2);
            }
        }
        this._intLength = outPtr - 1;
        this._textBuffer.setCurrentLength(outPtr);
        return _valueComplete(JsonToken.VALUE_NUMBER_INT);
    }

    /* access modifiers changed from: protected */
    public JsonToken _startNumberLeadingZero() throws IOException {
        int ptr = this._inputPtr;
        if (ptr >= this._inputEnd) {
            this._minorState = 24;
            JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
            this._currToken = jsonToken;
            return jsonToken;
        }
        int ptr2 = ptr + 1;
        int ch = this._inputBuffer[ptr] & 255;
        if (ch < 48) {
            if (ch == 46) {
                this._inputPtr = ptr2;
                this._intLength = 1;
                char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
                outBuf[0] = '0';
                return _startFloat(outBuf, 1, ch);
            }
        } else if (ch <= 57) {
            return _finishNumberLeadingZeroes();
        } else {
            if (ch == 101 || ch == 69) {
                this._inputPtr = ptr2;
                this._intLength = 1;
                char[] outBuf2 = this._textBuffer.emptyAndGetCurrentSegment();
                outBuf2[0] = '0';
                return _startFloat(outBuf2, 1, ch);
            } else if (!(ch == 93 || ch == 125)) {
                reportUnexpectedNumberChar(ch, "expected digit (0-9), decimal point (.) or exponent indicator (e/E) to follow '0'");
            }
        }
        return _valueCompleteInt(0, "0");
    }

    /* access modifiers changed from: protected */
    public JsonToken _finishNumberMinus(int ch) throws IOException {
        if (ch <= 48) {
            if (ch == 48) {
                return _finishNumberLeadingNegZeroes();
            }
            reportUnexpectedNumberChar(ch, "expected digit (0-9) to follow minus sign, for valid numeric value");
        } else if (ch > 57) {
            if (ch == 73) {
                return _finishNonStdToken(3, 2);
            }
            reportUnexpectedNumberChar(ch, "expected digit (0-9) to follow minus sign, for valid numeric value");
        }
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        outBuf[0] = '-';
        outBuf[1] = (char) ch;
        this._intLength = 1;
        return _finishNumberIntegralPart(outBuf, 2);
    }

    /* access modifiers changed from: protected */
    public JsonToken _finishNumberLeadingZeroes() throws IOException {
        while (this._inputPtr < this._inputEnd) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int ch = bArr[i] & 255;
            if (ch < 48) {
                if (ch == 46) {
                    char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
                    outBuf[0] = '0';
                    this._intLength = 1;
                    return _startFloat(outBuf, 1, ch);
                }
            } else if (ch <= 57) {
                if (!isEnabled(Feature.ALLOW_NUMERIC_LEADING_ZEROS)) {
                    reportInvalidNumber("Leading zeroes not allowed");
                    continue;
                }
                if (ch != 48) {
                    char[] outBuf2 = this._textBuffer.emptyAndGetCurrentSegment();
                    outBuf2[0] = (char) ch;
                    this._intLength = 1;
                    return _finishNumberIntegralPart(outBuf2, 1);
                }
            } else if (ch == 101 || ch == 69) {
                char[] outBuf3 = this._textBuffer.emptyAndGetCurrentSegment();
                outBuf3[0] = '0';
                this._intLength = 1;
                return _startFloat(outBuf3, 1, ch);
            } else if (!(ch == 93 || ch == 125)) {
                reportUnexpectedNumberChar(ch, "expected digit (0-9), decimal point (.) or exponent indicator (e/E) to follow '0'");
            }
            this._inputPtr--;
            return _valueCompleteInt(0, "0");
        }
        this._minorState = 24;
        JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken;
        return jsonToken;
    }

    /* access modifiers changed from: protected */
    public JsonToken _finishNumberLeadingNegZeroes() throws IOException {
        while (this._inputPtr < this._inputEnd) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int ch = bArr[i] & 255;
            if (ch < 48) {
                if (ch == 46) {
                    char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
                    outBuf[0] = '-';
                    outBuf[1] = '0';
                    this._intLength = 1;
                    return _startFloat(outBuf, 2, ch);
                }
            } else if (ch <= 57) {
                if (!isEnabled(Feature.ALLOW_NUMERIC_LEADING_ZEROS)) {
                    reportInvalidNumber("Leading zeroes not allowed");
                    continue;
                }
                if (ch != 48) {
                    char[] outBuf2 = this._textBuffer.emptyAndGetCurrentSegment();
                    outBuf2[0] = '-';
                    outBuf2[1] = (char) ch;
                    this._intLength = 1;
                    return _finishNumberIntegralPart(outBuf2, 2);
                }
            } else if (ch == 101 || ch == 69) {
                char[] outBuf3 = this._textBuffer.emptyAndGetCurrentSegment();
                outBuf3[0] = '-';
                outBuf3[1] = '0';
                this._intLength = 1;
                return _startFloat(outBuf3, 2, ch);
            } else if (!(ch == 93 || ch == 125)) {
                reportUnexpectedNumberChar(ch, "expected digit (0-9), decimal point (.) or exponent indicator (e/E) to follow '0'");
            }
            this._inputPtr--;
            return _valueCompleteInt(0, "0");
        }
        this._minorState = 25;
        JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken;
        return jsonToken;
    }

    /* access modifiers changed from: protected */
    public JsonToken _finishNumberIntegralPart(char[] outBuf, int outPtr) throws IOException {
        int negMod = this._numberNegative ? -1 : 0;
        while (this._inputPtr < this._inputEnd) {
            int ch = this._inputBuffer[this._inputPtr] & 255;
            if (ch < 48) {
                if (ch == 46) {
                    this._intLength = outPtr + negMod;
                    this._inputPtr++;
                    return _startFloat(outBuf, outPtr, ch);
                }
            } else if (ch <= 57) {
                this._inputPtr++;
                if (outPtr >= outBuf.length) {
                    outBuf = this._textBuffer.expandCurrentSegment();
                }
                int outPtr2 = outPtr + 1;
                outBuf[outPtr] = (char) ch;
                outPtr = outPtr2;
            } else if (ch == 101 || ch == 69) {
                this._intLength = outPtr + negMod;
                this._inputPtr++;
                return _startFloat(outBuf, outPtr, ch);
            }
            this._intLength = outPtr + negMod;
            this._textBuffer.setCurrentLength(outPtr);
            return _valueComplete(JsonToken.VALUE_NUMBER_INT);
        }
        this._minorState = 26;
        this._textBuffer.setCurrentLength(outPtr);
        JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken;
        return jsonToken;
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r11v10, types: [byte] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r11v11, types: [byte] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r11v5, types: [byte, int] */
    public JsonToken _startFloat(char[] outBuf, int outPtr, int ch) throws IOException {
        int outPtr2;
        int fractLen = 0;
        if (ch == 46) {
            if (outPtr >= outBuf.length) {
                outBuf = this._textBuffer.expandCurrentSegment();
            }
            int outPtr3 = outPtr + 1;
            outBuf[outPtr] = ClassUtils.PACKAGE_SEPARATOR_CHAR;
            outPtr = outPtr3;
            while (this._inputPtr < this._inputEnd) {
                byte[] bArr = this._inputBuffer;
                int i = this._inputPtr;
                this._inputPtr = i + 1;
                byte ch2 = bArr[i];
                if (ch2 < 48 || ch2 > 57) {
                    ch = ch2 & 255;
                    if (fractLen == 0) {
                        reportUnexpectedNumberChar(ch, "Decimal point not followed by a digit");
                    }
                } else {
                    if (outPtr >= outBuf.length) {
                        outBuf = this._textBuffer.expandCurrentSegment();
                    }
                    int outPtr4 = outPtr + 1;
                    outBuf[outPtr] = (char) ch2;
                    fractLen++;
                    outPtr = outPtr4;
                }
            }
            this._textBuffer.setCurrentLength(outPtr);
            this._minorState = 30;
            this._fractLength = fractLen;
            JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
            this._currToken = jsonToken;
            return jsonToken;
        }
        this._fractLength = fractLen;
        int expLen = 0;
        if (ch == 101 || ch == 69) {
            if (outPtr >= outBuf.length) {
                outBuf = this._textBuffer.expandCurrentSegment();
            }
            int outPtr5 = outPtr + 1;
            outBuf[outPtr] = (char) ch;
            if (this._inputPtr >= this._inputEnd) {
                this._textBuffer.setCurrentLength(outPtr5);
                this._minorState = 31;
                this._expLength = 0;
                JsonToken jsonToken2 = JsonToken.NOT_AVAILABLE;
                this._currToken = jsonToken2;
                return jsonToken2;
            }
            byte[] bArr2 = this._inputBuffer;
            int i2 = this._inputPtr;
            this._inputPtr = i2 + 1;
            int ch3 = bArr2[i2];
            if (ch3 == 45 || ch3 == 43) {
                if (outPtr5 >= outBuf.length) {
                    outBuf = this._textBuffer.expandCurrentSegment();
                }
                outPtr2 = outPtr5 + 1;
                outBuf[outPtr5] = (char) ch3;
                if (this._inputPtr >= this._inputEnd) {
                    this._textBuffer.setCurrentLength(outPtr2);
                    this._minorState = 32;
                    this._expLength = 0;
                    JsonToken jsonToken3 = JsonToken.NOT_AVAILABLE;
                    this._currToken = jsonToken3;
                    return jsonToken3;
                }
                byte[] bArr3 = this._inputBuffer;
                int i3 = this._inputPtr;
                this._inputPtr = i3 + 1;
                ch3 = bArr3[i3];
            } else {
                outPtr2 = outPtr5;
            }
            while (ch3 >= 48 && ch3 <= 57) {
                expLen++;
                if (outPtr2 >= outBuf.length) {
                    outBuf = this._textBuffer.expandCurrentSegment();
                }
                int outPtr6 = outPtr2 + 1;
                outBuf[outPtr2] = (char) ch3;
                if (this._inputPtr >= this._inputEnd) {
                    this._textBuffer.setCurrentLength(outPtr6);
                    this._minorState = 32;
                    this._expLength = expLen;
                    JsonToken jsonToken4 = JsonToken.NOT_AVAILABLE;
                    this._currToken = jsonToken4;
                    return jsonToken4;
                }
                byte[] bArr4 = this._inputBuffer;
                int i4 = this._inputPtr;
                this._inputPtr = i4 + 1;
                ch3 = bArr4[i4];
                outPtr2 = outPtr6;
            }
            int ch4 = ch3 & 255;
            if (expLen == 0) {
                reportUnexpectedNumberChar(ch4, "Exponent indicator not followed by a digit");
            }
            int i5 = outPtr2;
            int outPtr7 = ch4;
            outPtr = i5;
        }
        this._inputPtr--;
        this._textBuffer.setCurrentLength(outPtr);
        this._expLength = expLen;
        return _valueComplete(JsonToken.VALUE_NUMBER_FLOAT);
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x0045  */
    public JsonToken _finishFloatFraction() throws IOException {
        int ch;
        int fractLen = this._fractLength;
        char[] outBuf = this._textBuffer.getBufferWithoutReset();
        int outPtr = this._textBuffer.getCurrentSegmentSize();
        while (true) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            byte b = bArr[i];
            ch = b;
            if (b >= 48 && ch <= 57) {
                fractLen++;
                if (outPtr >= outBuf.length) {
                    outBuf = this._textBuffer.expandCurrentSegment();
                }
                int outPtr2 = outPtr + 1;
                outBuf[outPtr] = (char) ch;
                if (this._inputPtr >= this._inputEnd) {
                    this._textBuffer.setCurrentLength(outPtr2);
                    this._fractLength = fractLen;
                    return JsonToken.NOT_AVAILABLE;
                }
                outPtr = outPtr2;
            } else if (fractLen == 0) {
                reportUnexpectedNumberChar(ch, "Decimal point not followed by a digit");
            }
        }
        if (fractLen == 0) {
        }
        this._fractLength = fractLen;
        this._textBuffer.setCurrentLength(outPtr);
        if (ch == 101 || ch == 69) {
            this._textBuffer.append((char) ch);
            this._expLength = 0;
            if (this._inputPtr >= this._inputEnd) {
                this._minorState = 31;
                return JsonToken.NOT_AVAILABLE;
            }
            this._minorState = 32;
            byte[] bArr2 = this._inputBuffer;
            int i2 = this._inputPtr;
            this._inputPtr = i2 + 1;
            return _finishFloatExponent(true, bArr2[i2] & 255);
        }
        this._inputPtr--;
        this._textBuffer.setCurrentLength(outPtr);
        this._expLength = 0;
        return _valueComplete(JsonToken.VALUE_NUMBER_FLOAT);
    }

    /* JADX WARNING: type inference failed for: r1v6, types: [byte[]] */
    /* JADX WARNING: type inference failed for: r8v4, types: [byte] */
    /* JADX WARNING: type inference failed for: r0v6, types: [byte[]] */
    /* JADX WARNING: type inference failed for: r8v5, types: [byte] */
    /* access modifiers changed from: protected */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r8v4, types: [byte] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r8v5, types: [byte] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte[], code=null, for r0v6, types: [byte[]] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte[], code=null, for r1v6, types: [byte[]] */
    /* JADX WARNING: Unknown variable types count: 2 */
    public JsonToken _finishFloatExponent(boolean checkSign, int ch) throws IOException {
        if (checkSign) {
            this._minorState = 32;
            if (ch == 45 || ch == 43) {
                this._textBuffer.append((char) ch);
                if (this._inputPtr >= this._inputEnd) {
                    this._minorState = 32;
                    this._expLength = 0;
                    return JsonToken.NOT_AVAILABLE;
                }
                ? r0 = this._inputBuffer;
                int i = this._inputPtr;
                this._inputPtr = i + 1;
                ch = r0[i];
            }
        }
        char[] outBuf = this._textBuffer.getBufferWithoutReset();
        int outPtr = this._textBuffer.getCurrentSegmentSize();
        int expLen = this._expLength;
        while (ch >= 48 && ch <= 57) {
            expLen++;
            if (outPtr >= outBuf.length) {
                outBuf = this._textBuffer.expandCurrentSegment();
            }
            int outPtr2 = outPtr + 1;
            outBuf[outPtr] = (char) ch;
            if (this._inputPtr >= this._inputEnd) {
                this._textBuffer.setCurrentLength(outPtr2);
                this._expLength = expLen;
                return JsonToken.NOT_AVAILABLE;
            }
            ? r1 = this._inputBuffer;
            int i2 = this._inputPtr;
            this._inputPtr = i2 + 1;
            ch = r1[i2];
            outPtr = outPtr2;
        }
        int ch2 = ch & 255;
        if (expLen == 0) {
            reportUnexpectedNumberChar(ch2, "Exponent indicator not followed by a digit");
        }
        this._inputPtr--;
        this._textBuffer.setCurrentLength(outPtr);
        this._expLength = expLen;
        return _valueComplete(JsonToken.VALUE_NUMBER_FLOAT);
    }

    private final String _fastParseName() throws IOException {
        byte[] input = this._inputBuffer;
        int[] codes = _icLatin1;
        int ptr = this._inputPtr;
        int ptr2 = ptr + 1;
        int q0 = input[ptr] & 255;
        if (codes[q0] == 0) {
            int ptr3 = ptr2 + 1;
            int i = input[ptr2] & 255;
            if (codes[i] == 0) {
                int q = (q0 << 8) | i;
                int ptr4 = ptr3 + 1;
                int i2 = input[ptr3] & 255;
                if (codes[i2] == 0) {
                    int q2 = (q << 8) | i2;
                    int ptr5 = ptr4 + 1;
                    int i3 = input[ptr4] & 255;
                    if (codes[i3] == 0) {
                        int q3 = (q2 << 8) | i3;
                        int ptr6 = ptr5 + 1;
                        int i4 = input[ptr5] & 255;
                        if (codes[i4] == 0) {
                            this._quad1 = q3;
                            return _parseMediumName(ptr6, i4);
                        } else if (i4 != 34) {
                            return null;
                        } else {
                            this._inputPtr = ptr6;
                            return _findName(q3, 4);
                        }
                    } else if (i3 != 34) {
                        return null;
                    } else {
                        this._inputPtr = ptr5;
                        return _findName(q2, 3);
                    }
                } else if (i2 != 34) {
                    return null;
                } else {
                    this._inputPtr = ptr4;
                    return _findName(q, 2);
                }
            } else if (i != 34) {
                return null;
            } else {
                this._inputPtr = ptr3;
                return _findName(q0, 1);
            }
        } else if (q0 != 34) {
            return null;
        } else {
            this._inputPtr = ptr2;
            return "";
        }
    }

    private final String _parseMediumName(int ptr, int q2) throws IOException {
        byte[] input = this._inputBuffer;
        int[] codes = _icLatin1;
        int ptr2 = ptr + 1;
        int i = input[ptr] & 255;
        if (codes[i] == 0) {
            int q22 = (q2 << 8) | i;
            int ptr3 = ptr2 + 1;
            int i2 = input[ptr2] & 255;
            if (codes[i2] == 0) {
                int q23 = (q22 << 8) | i2;
                int ptr4 = ptr3 + 1;
                int i3 = input[ptr3] & 255;
                if (codes[i3] == 0) {
                    int q24 = (q23 << 8) | i3;
                    int ptr5 = ptr4 + 1;
                    int i4 = input[ptr4] & 255;
                    if (codes[i4] == 0) {
                        return _parseMediumName2(ptr5, i4, q24);
                    }
                    if (i4 != 34) {
                        return null;
                    }
                    this._inputPtr = ptr5;
                    return _findName(this._quad1, q24, 4);
                } else if (i3 != 34) {
                    return null;
                } else {
                    this._inputPtr = ptr4;
                    return _findName(this._quad1, q23, 3);
                }
            } else if (i2 != 34) {
                return null;
            } else {
                this._inputPtr = ptr3;
                return _findName(this._quad1, q22, 2);
            }
        } else if (i != 34) {
            return null;
        } else {
            this._inputPtr = ptr2;
            return _findName(this._quad1, q2, 1);
        }
    }

    private final String _parseMediumName2(int ptr, int q3, int q2) throws IOException {
        byte[] input = this._inputBuffer;
        int[] codes = _icLatin1;
        int ptr2 = ptr + 1;
        int i = input[ptr] & 255;
        if (codes[i] == 0) {
            int q32 = (q3 << 8) | i;
            int ptr3 = ptr2 + 1;
            int i2 = input[ptr2] & 255;
            if (codes[i2] == 0) {
                int q33 = (q32 << 8) | i2;
                int ptr4 = ptr3 + 1;
                int i3 = input[ptr3] & 255;
                if (codes[i3] == 0) {
                    int q34 = (q33 << 8) | i3;
                    int ptr5 = ptr4 + 1;
                    if ((input[ptr4] & 255) != 34) {
                        return null;
                    }
                    this._inputPtr = ptr5;
                    return _findName(this._quad1, q2, q34, 4);
                } else if (i3 != 34) {
                    return null;
                } else {
                    this._inputPtr = ptr4;
                    return _findName(this._quad1, q2, q33, 3);
                }
            } else if (i2 != 34) {
                return null;
            } else {
                this._inputPtr = ptr3;
                return _findName(this._quad1, q2, q32, 2);
            }
        } else if (i != 34) {
            return null;
        } else {
            this._inputPtr = ptr2;
            return _findName(this._quad1, q2, q3, 1);
        }
    }

    private final JsonToken _parseEscapedName(int qlen, int currQuad, int currQuadBytes) throws IOException {
        int[] quads = this._quadBuffer;
        int[] codes = _icLatin1;
        while (this._inputPtr < this._inputEnd) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int ch = bArr[i] & 255;
            if (codes[ch] == 0) {
                if (currQuadBytes < 4) {
                    currQuadBytes = currQuadBytes + 1;
                    currQuad = (currQuad << 8) | ch;
                } else {
                    if (qlen >= quads.length) {
                        int[] growArrayBy = growArrayBy(quads, quads.length);
                        quads = growArrayBy;
                        this._quadBuffer = growArrayBy;
                    }
                    int qlen2 = qlen + 1;
                    quads[qlen] = currQuad;
                    currQuad = ch;
                    currQuadBytes = 1;
                    qlen = qlen2;
                }
            } else if (ch == 34) {
                if (currQuadBytes > 0) {
                    if (qlen >= quads.length) {
                        int[] growArrayBy2 = growArrayBy(quads, quads.length);
                        quads = growArrayBy2;
                        this._quadBuffer = growArrayBy2;
                    }
                    int qlen3 = qlen + 1;
                    quads[qlen] = _padLastQuad(currQuad, currQuadBytes);
                    qlen = qlen3;
                } else if (qlen == 0) {
                    return _fieldComplete("");
                }
                String name = this._symbols.findName(quads, qlen);
                if (name == null) {
                    name = _addName(quads, qlen, currQuadBytes);
                }
                return _fieldComplete(name);
            } else {
                if (ch != 92) {
                    _throwUnquotedSpace(ch, "name");
                } else {
                    ch = _decodeCharEscape();
                    if (ch < 0) {
                        this._minorState = 8;
                        this._minorStateAfterSplit = 7;
                        this._quadLength = qlen;
                        this._pending32 = currQuad;
                        this._pendingBytes = currQuadBytes;
                        JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
                        this._currToken = jsonToken;
                        return jsonToken;
                    }
                }
                if (qlen >= quads.length) {
                    int[] growArrayBy3 = growArrayBy(quads, quads.length);
                    quads = growArrayBy3;
                    this._quadBuffer = growArrayBy3;
                }
                if (ch > 127) {
                    if (currQuadBytes >= 4) {
                        int qlen4 = qlen + 1;
                        quads[qlen] = currQuad;
                        currQuad = 0;
                        currQuadBytes = 0;
                        qlen = qlen4;
                    }
                    if (ch < 2048) {
                        currQuad = (currQuad << 8) | (ch >> 6) | PsExtractor.AUDIO_STREAM;
                        currQuadBytes++;
                    } else {
                        int currQuad2 = (currQuad << 8) | (ch >> 12) | 224;
                        int currQuadBytes2 = currQuadBytes + 1;
                        if (currQuadBytes2 >= 4) {
                            int qlen5 = qlen + 1;
                            quads[qlen] = currQuad2;
                            currQuad2 = 0;
                            currQuadBytes2 = 0;
                            qlen = qlen5;
                        }
                        currQuad = (currQuad2 << 8) | ((ch >> 6) & 63) | 128;
                        currQuadBytes = currQuadBytes2 + 1;
                    }
                    ch = (ch & 63) | 128;
                }
                if (currQuadBytes < 4) {
                    currQuadBytes = currQuadBytes + 1;
                    currQuad = (currQuad << 8) | ch;
                } else {
                    int qlen6 = qlen + 1;
                    quads[qlen] = currQuad;
                    currQuad = ch;
                    currQuadBytes = 1;
                    qlen = qlen6;
                }
            }
        }
        this._quadLength = qlen;
        this._pending32 = currQuad;
        this._pendingBytes = currQuadBytes;
        this._minorState = 7;
        JsonToken jsonToken2 = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken2;
        return jsonToken2;
    }

    private JsonToken _handleOddName(int ch) throws IOException {
        if (ch != 35) {
            if (ch != 39) {
                if (ch == 47) {
                    return _startSlashComment(4);
                }
                if (ch == 93) {
                    return _closeArrayScope();
                }
            } else if (isEnabled(Feature.ALLOW_SINGLE_QUOTES)) {
                return _finishAposName(0, 0, 0);
            }
        } else if (Feature.ALLOW_YAML_COMMENTS.enabledIn(this._features)) {
            return _finishHashComment(4);
        }
        if (!isEnabled(Feature.ALLOW_UNQUOTED_FIELD_NAMES)) {
            _reportUnexpectedChar((char) ch, "was expecting double-quote to start field name");
        }
        if (CharTypes.getInputCodeUtf8JsNames()[ch] != 0) {
            _reportUnexpectedChar(ch, "was expecting either valid name character (for unquoted name) or double-quote (for quoted) to start field name");
        }
        return _finishUnquotedName(0, ch, 1);
    }

    private JsonToken _finishUnquotedName(int currQuad, int currQuad2, int currQuadBytes) throws IOException {
        int[] quads = this._quadBuffer;
        int[] codes = CharTypes.getInputCodeUtf8JsNames();
        while (this._inputPtr < this._inputEnd) {
            int ch = this._inputBuffer[this._inputPtr] & 255;
            if (codes[ch] != 0) {
                if (currQuadBytes > 0) {
                    if (currQuad >= quads.length) {
                        int[] growArrayBy = growArrayBy(quads, quads.length);
                        quads = growArrayBy;
                        this._quadBuffer = growArrayBy;
                    }
                    int qlen = currQuad + 1;
                    quads[currQuad] = currQuad2;
                    currQuad = qlen;
                }
                String name = this._symbols.findName(quads, currQuad);
                if (name == null) {
                    name = _addName(quads, currQuad, currQuadBytes);
                }
                return _fieldComplete(name);
            }
            this._inputPtr++;
            if (currQuadBytes < 4) {
                currQuadBytes++;
                currQuad2 = (currQuad2 << 8) | ch;
            } else {
                if (currQuad >= quads.length) {
                    int[] growArrayBy2 = growArrayBy(quads, quads.length);
                    quads = growArrayBy2;
                    this._quadBuffer = growArrayBy2;
                }
                int qlen2 = currQuad + 1;
                quads[currQuad] = currQuad2;
                currQuadBytes = 1;
                currQuad2 = ch;
                currQuad = qlen2;
            }
        }
        this._quadLength = currQuad;
        this._pending32 = currQuad2;
        this._pendingBytes = currQuadBytes;
        this._minorState = 10;
        JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken;
        return jsonToken;
    }

    private JsonToken _finishAposName(int qlen, int currQuad, int currQuadBytes) throws IOException {
        int[] quads = this._quadBuffer;
        int[] codes = _icLatin1;
        while (this._inputPtr < this._inputEnd) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            int ch = bArr[i] & 255;
            if (ch == 39) {
                if (currQuadBytes > 0) {
                    if (qlen >= quads.length) {
                        int[] growArrayBy = growArrayBy(quads, quads.length);
                        quads = growArrayBy;
                        this._quadBuffer = growArrayBy;
                    }
                    int qlen2 = qlen + 1;
                    quads[qlen] = _padLastQuad(currQuad, currQuadBytes);
                    qlen = qlen2;
                } else if (qlen == 0) {
                    return _fieldComplete("");
                }
                String name = this._symbols.findName(quads, qlen);
                if (name == null) {
                    name = _addName(quads, qlen, currQuadBytes);
                }
                return _fieldComplete(name);
            }
            if (!(ch == 34 || codes[ch] == 0)) {
                if (ch != 92) {
                    _throwUnquotedSpace(ch, "name");
                } else {
                    ch = _decodeCharEscape();
                    if (ch < 0) {
                        this._minorState = 8;
                        this._minorStateAfterSplit = 9;
                        this._quadLength = qlen;
                        this._pending32 = currQuad;
                        this._pendingBytes = currQuadBytes;
                        JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
                        this._currToken = jsonToken;
                        return jsonToken;
                    }
                }
                if (ch > 127) {
                    if (currQuadBytes >= 4) {
                        if (qlen >= quads.length) {
                            int[] growArrayBy2 = growArrayBy(quads, quads.length);
                            quads = growArrayBy2;
                            this._quadBuffer = growArrayBy2;
                        }
                        int qlen3 = qlen + 1;
                        quads[qlen] = currQuad;
                        currQuad = 0;
                        currQuadBytes = 0;
                        qlen = qlen3;
                    }
                    if (ch < 2048) {
                        currQuad = (currQuad << 8) | (ch >> 6) | PsExtractor.AUDIO_STREAM;
                        currQuadBytes++;
                    } else {
                        int currQuad2 = (currQuad << 8) | (ch >> 12) | 224;
                        int currQuadBytes2 = currQuadBytes + 1;
                        if (currQuadBytes2 >= 4) {
                            if (qlen >= quads.length) {
                                int[] growArrayBy3 = growArrayBy(quads, quads.length);
                                quads = growArrayBy3;
                                this._quadBuffer = growArrayBy3;
                            }
                            int qlen4 = qlen + 1;
                            quads[qlen] = currQuad2;
                            currQuad2 = 0;
                            currQuadBytes2 = 0;
                            qlen = qlen4;
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
                    int[] growArrayBy4 = growArrayBy(quads, quads.length);
                    quads = growArrayBy4;
                    this._quadBuffer = growArrayBy4;
                }
                int qlen5 = qlen + 1;
                quads[qlen] = currQuad;
                currQuadBytes = 1;
                currQuad = ch;
                qlen = qlen5;
            }
        }
        this._quadLength = qlen;
        this._pending32 = currQuad;
        this._pendingBytes = currQuadBytes;
        this._minorState = 9;
        JsonToken jsonToken2 = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken2;
        return jsonToken2;
    }

    /* access modifiers changed from: protected */
    public final JsonToken _finishFieldWithEscape() throws IOException {
        int currQuadBytes;
        int currQuad;
        int ch = _decodeSplitEscaped(this._quoted32, this._quotedDigits);
        if (ch < 0) {
            this._minorState = 8;
            return JsonToken.NOT_AVAILABLE;
        }
        if (this._quadLength >= this._quadBuffer.length) {
            this._quadBuffer = growArrayBy(this._quadBuffer, 32);
        }
        int currQuad2 = this._pending32;
        int currQuadBytes2 = this._pendingBytes;
        if (ch > 127) {
            if (currQuadBytes2 >= 4) {
                int[] iArr = this._quadBuffer;
                int i = this._quadLength;
                this._quadLength = i + 1;
                iArr[i] = currQuad2;
                currQuad2 = 0;
                currQuadBytes2 = 0;
            }
            if (ch < 2048) {
                currQuad2 = (currQuad2 << 8) | (ch >> 6) | PsExtractor.AUDIO_STREAM;
                currQuadBytes2++;
            } else {
                int currQuad3 = (currQuad2 << 8) | (ch >> 12) | 224;
                int currQuadBytes3 = currQuadBytes2 + 1;
                if (currQuadBytes3 >= 4) {
                    int[] iArr2 = this._quadBuffer;
                    int i2 = this._quadLength;
                    this._quadLength = i2 + 1;
                    iArr2[i2] = currQuad3;
                    currQuad3 = 0;
                    currQuadBytes3 = 0;
                }
                currQuad2 = (currQuad3 << 8) | ((ch >> 6) & 63) | 128;
                currQuadBytes2 = currQuadBytes3 + 1;
            }
            ch = (ch & 63) | 128;
        }
        if (currQuadBytes2 < 4) {
            currQuadBytes = currQuadBytes2 + 1;
            currQuad = (currQuad2 << 8) | ch;
        } else {
            int[] iArr3 = this._quadBuffer;
            int i3 = this._quadLength;
            this._quadLength = i3 + 1;
            iArr3[i3] = currQuad2;
            currQuad = ch;
            currQuadBytes = 1;
        }
        if (this._minorStateAfterSplit == 9) {
            return _finishAposName(this._quadLength, currQuad, currQuadBytes);
        }
        return _parseEscapedName(this._quadLength, currQuad, currQuadBytes);
    }

    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r0v2, types: [byte, int] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r0v7, types: [byte] */
    private int _decodeSplitEscaped(int value, int bytesRead) throws IOException {
        if (this._inputPtr >= this._inputEnd) {
            this._quoted32 = value;
            this._quotedDigits = bytesRead;
            return -1;
        }
        byte[] bArr = this._inputBuffer;
        int i = this._inputPtr;
        this._inputPtr = i + 1;
        int c = bArr[i];
        if (bytesRead == -1) {
            if (c == 34 || c == 47 || c == 92) {
                return c;
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
                return 13;
            }
            switch (c) {
                case 116:
                    return 9;
                case 117:
                    if (this._inputPtr < this._inputEnd) {
                        byte[] bArr2 = this._inputBuffer;
                        int i2 = this._inputPtr;
                        this._inputPtr = i2 + 1;
                        c = bArr2[i2];
                        bytesRead = 0;
                        break;
                    } else {
                        this._quotedDigits = 0;
                        this._quoted32 = 0;
                        return -1;
                    }
                default:
                    return _handleUnrecognizedCharacterEscape((char) c);
            }
        }
        int c2 = c & 255;
        while (true) {
            int digit = CharTypes.charToHex(c2);
            if (digit < 0) {
                _reportUnexpectedChar(c2, "expected a hex-digit for character escape sequence");
            }
            value = (value << 4) | digit;
            bytesRead++;
            if (bytesRead == 4) {
                return value;
            }
            if (this._inputPtr >= this._inputEnd) {
                this._quotedDigits = bytesRead;
                this._quoted32 = value;
                return -1;
            }
            byte[] bArr3 = this._inputBuffer;
            int i3 = this._inputPtr;
            this._inputPtr = i3 + 1;
            c2 = bArr3[i3] & 255;
        }
    }

    /* access modifiers changed from: protected */
    public JsonToken _startString() throws IOException {
        int ptr = this._inputPtr;
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
                return _valueComplete(JsonToken.VALUE_STRING);
            }
        }
        this._textBuffer.setCurrentLength(outPtr);
        this._inputPtr = ptr;
        return _finishRegularString();
    }

    private final JsonToken _finishRegularString() throws IOException {
        int ptr;
        int[] codes = _icUTF8;
        byte[] inputBuffer = this._inputBuffer;
        char[] outBuf = this._textBuffer.getBufferWithoutReset();
        int outPtr = this._textBuffer.getCurrentSegmentSize();
        int c = this._inputPtr;
        int safeEnd = this._inputEnd - 5;
        while (c < this._inputEnd) {
            if (outPtr >= outBuf.length) {
                outBuf = this._textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            int max = Math.min(this._inputEnd, (outBuf.length - outPtr) + c);
            while (true) {
                if (c < max) {
                    int ptr2 = c + 1;
                    int c2 = inputBuffer[c] & 255;
                    if (codes[c2] == 0) {
                        int outPtr2 = outPtr + 1;
                        outBuf[outPtr] = (char) c2;
                        c = ptr2;
                        outPtr = outPtr2;
                    } else if (c2 == 34) {
                        this._inputPtr = ptr2;
                        this._textBuffer.setCurrentLength(outPtr);
                        return _valueComplete(JsonToken.VALUE_STRING);
                    } else if (ptr2 >= safeEnd) {
                        this._inputPtr = ptr2;
                        this._textBuffer.setCurrentLength(outPtr);
                        if (!_decodeSplitMultiByte(c2, codes[c2], ptr2 < this._inputEnd)) {
                            this._minorStateAfterSplit = 40;
                            JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
                            this._currToken = jsonToken;
                            return jsonToken;
                        }
                        outBuf = this._textBuffer.getBufferWithoutReset();
                        outPtr = this._textBuffer.getCurrentSegmentSize();
                        c = this._inputPtr;
                    } else {
                        switch (codes[c2]) {
                            case 1:
                                this._inputPtr = ptr2;
                                c2 = _decodeFastCharEscape();
                                ptr = this._inputPtr;
                                break;
                            case 2:
                                int ptr3 = ptr2 + 1;
                                c2 = _decodeUTF8_2(c2, this._inputBuffer[ptr2]);
                                ptr = ptr3;
                                break;
                            case 3:
                                byte[] bArr = this._inputBuffer;
                                int ptr4 = ptr2 + 1;
                                int ptr5 = ptr4 + 1;
                                c2 = _decodeUTF8_3(c2, bArr[ptr2], bArr[ptr4]);
                                ptr = ptr5;
                                break;
                            case 4:
                                byte[] bArr2 = this._inputBuffer;
                                int ptr6 = ptr2 + 1;
                                int ptr7 = ptr6 + 1;
                                int ptr8 = ptr7 + 1;
                                int c3 = _decodeUTF8_4(c2, bArr2[ptr2], bArr2[ptr6], bArr2[ptr7]);
                                int outPtr3 = outPtr + 1;
                                outBuf[outPtr] = (char) (55296 | (c3 >> 10));
                                if (outPtr3 >= outBuf.length) {
                                    outBuf = this._textBuffer.finishCurrentSegment();
                                    outPtr = 0;
                                } else {
                                    outPtr = outPtr3;
                                }
                                c2 = (c3 & 1023) | GeneratorBase.SURR2_FIRST;
                                ptr = ptr8;
                                break;
                            default:
                                if (c2 < 32) {
                                    _throwUnquotedSpace(c2, "string value");
                                } else {
                                    _reportInvalidChar(c2);
                                }
                                ptr = ptr2;
                                break;
                        }
                        if (outPtr >= outBuf.length) {
                            outBuf = this._textBuffer.finishCurrentSegment();
                            outPtr = 0;
                        }
                        int outPtr4 = outPtr + 1;
                        outBuf[outPtr] = (char) c2;
                        c = ptr;
                        outPtr = outPtr4;
                    }
                }
            }
        }
        this._inputPtr = c;
        this._minorState = 40;
        this._textBuffer.setCurrentLength(outPtr);
        JsonToken jsonToken2 = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken2;
        return jsonToken2;
    }

    /* access modifiers changed from: protected */
    public JsonToken _startAposString() throws IOException {
        int ptr = this._inputPtr;
        int outPtr = 0;
        char[] outBuf = this._textBuffer.emptyAndGetCurrentSegment();
        int[] codes = _icUTF8;
        int max = Math.min(this._inputEnd, outBuf.length + ptr);
        byte[] inputBuffer = this._inputBuffer;
        while (ptr < max) {
            int c = inputBuffer[ptr] & 255;
            if (c == 39) {
                this._inputPtr = ptr + 1;
                this._textBuffer.setCurrentLength(outPtr);
                return _valueComplete(JsonToken.VALUE_STRING);
            } else if (codes[c] != 0) {
                break;
            } else {
                ptr++;
                int outPtr2 = outPtr + 1;
                outBuf[outPtr] = (char) c;
                outPtr = outPtr2;
            }
        }
        this._textBuffer.setCurrentLength(outPtr);
        this._inputPtr = ptr;
        return _finishAposString();
    }

    private final JsonToken _finishAposString() throws IOException {
        int ptr;
        int[] codes = _icUTF8;
        byte[] inputBuffer = this._inputBuffer;
        char[] outBuf = this._textBuffer.getBufferWithoutReset();
        int outPtr = this._textBuffer.getCurrentSegmentSize();
        int c = this._inputPtr;
        int safeEnd = this._inputEnd - 5;
        while (c < this._inputEnd) {
            if (outPtr >= outBuf.length) {
                outBuf = this._textBuffer.finishCurrentSegment();
                outPtr = 0;
            }
            int max = Math.min(this._inputEnd, (outBuf.length - outPtr) + c);
            while (true) {
                if (c < max) {
                    int ptr2 = c + 1;
                    int c2 = inputBuffer[c] & 255;
                    if (codes[c2] == 0 || c2 == 34) {
                        if (c2 == 39) {
                            this._inputPtr = ptr2;
                            this._textBuffer.setCurrentLength(outPtr);
                            return _valueComplete(JsonToken.VALUE_STRING);
                        }
                        int outPtr2 = outPtr + 1;
                        outBuf[outPtr] = (char) c2;
                        c = ptr2;
                        outPtr = outPtr2;
                    } else if (ptr2 >= safeEnd) {
                        this._inputPtr = ptr2;
                        this._textBuffer.setCurrentLength(outPtr);
                        if (!_decodeSplitMultiByte(c2, codes[c2], ptr2 < this._inputEnd)) {
                            this._minorStateAfterSplit = 45;
                            JsonToken jsonToken = JsonToken.NOT_AVAILABLE;
                            this._currToken = jsonToken;
                            return jsonToken;
                        }
                        outBuf = this._textBuffer.getBufferWithoutReset();
                        outPtr = this._textBuffer.getCurrentSegmentSize();
                        c = this._inputPtr;
                    } else {
                        switch (codes[c2]) {
                            case 1:
                                this._inputPtr = ptr2;
                                c2 = _decodeFastCharEscape();
                                ptr = this._inputPtr;
                                break;
                            case 2:
                                int ptr3 = ptr2 + 1;
                                c2 = _decodeUTF8_2(c2, this._inputBuffer[ptr2]);
                                ptr = ptr3;
                                break;
                            case 3:
                                byte[] bArr = this._inputBuffer;
                                int ptr4 = ptr2 + 1;
                                int ptr5 = ptr4 + 1;
                                c2 = _decodeUTF8_3(c2, bArr[ptr2], bArr[ptr4]);
                                ptr = ptr5;
                                break;
                            case 4:
                                byte[] bArr2 = this._inputBuffer;
                                int ptr6 = ptr2 + 1;
                                int ptr7 = ptr6 + 1;
                                int ptr8 = ptr7 + 1;
                                int c3 = _decodeUTF8_4(c2, bArr2[ptr2], bArr2[ptr6], bArr2[ptr7]);
                                int outPtr3 = outPtr + 1;
                                outBuf[outPtr] = (char) (55296 | (c3 >> 10));
                                if (outPtr3 >= outBuf.length) {
                                    outBuf = this._textBuffer.finishCurrentSegment();
                                    outPtr = 0;
                                } else {
                                    outPtr = outPtr3;
                                }
                                c2 = (c3 & 1023) | GeneratorBase.SURR2_FIRST;
                                ptr = ptr8;
                                break;
                            default:
                                if (c2 < 32) {
                                    _throwUnquotedSpace(c2, "string value");
                                } else {
                                    _reportInvalidChar(c2);
                                }
                                ptr = ptr2;
                                break;
                        }
                        if (outPtr >= outBuf.length) {
                            outBuf = this._textBuffer.finishCurrentSegment();
                            outPtr = 0;
                        }
                        int outPtr4 = outPtr + 1;
                        outBuf[outPtr] = (char) c2;
                        c = ptr;
                        outPtr = outPtr4;
                    }
                }
            }
        }
        this._inputPtr = c;
        this._minorState = 45;
        this._textBuffer.setCurrentLength(outPtr);
        JsonToken jsonToken2 = JsonToken.NOT_AVAILABLE;
        this._currToken = jsonToken2;
        return jsonToken2;
    }

    private final boolean _decodeSplitMultiByte(int c, int type, boolean gotNext) throws IOException {
        switch (type) {
            case 1:
                int c2 = _decodeSplitEscaped(0, -1);
                if (c2 < 0) {
                    this._minorState = 41;
                    return false;
                }
                this._textBuffer.append((char) c2);
                return true;
            case 2:
                if (gotNext) {
                    byte[] bArr = this._inputBuffer;
                    int i = this._inputPtr;
                    this._inputPtr = i + 1;
                    this._textBuffer.append((char) _decodeUTF8_2(c, bArr[i]));
                    return true;
                }
                this._minorState = 42;
                this._pending32 = c;
                return false;
            case 3:
                int c3 = c & 15;
                if (gotNext) {
                    byte[] bArr2 = this._inputBuffer;
                    int i2 = this._inputPtr;
                    this._inputPtr = i2 + 1;
                    return _decodeSplitUTF8_3(c3, 1, bArr2[i2]);
                }
                this._minorState = 43;
                this._pending32 = c3;
                this._pendingBytes = 1;
                return false;
            case 4:
                int c4 = c & 7;
                if (gotNext) {
                    byte[] bArr3 = this._inputBuffer;
                    int i3 = this._inputPtr;
                    this._inputPtr = i3 + 1;
                    return _decodeSplitUTF8_4(c4, 1, bArr3[i3]);
                }
                this._pending32 = c4;
                this._pendingBytes = 1;
                this._minorState = 44;
                return false;
            default:
                if (c < 32) {
                    _throwUnquotedSpace(c, "string value");
                } else {
                    _reportInvalidChar(c);
                }
                this._textBuffer.append((char) c);
                return true;
        }
    }

    /* JADX WARNING: type inference failed for: r2v8, types: [byte[]] */
    /* JADX WARNING: type inference failed for: r8v2, types: [byte] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r8v2, types: [byte] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte[], code=null, for r2v8, types: [byte[]] */
    /* JADX WARNING: Unknown variable types count: 1 */
    private final boolean _decodeSplitUTF8_3(int prev, int prevCount, int next) throws IOException {
        if (prevCount == 1) {
            if ((next & PsExtractor.AUDIO_STREAM) != 128) {
                _reportInvalidOther(next & 255, this._inputPtr);
            }
            prev = (prev << 6) | (next & 63);
            if (this._inputPtr >= this._inputEnd) {
                this._minorState = 43;
                this._pending32 = prev;
                this._pendingBytes = 2;
                return false;
            }
            ? r2 = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            next = r2[i];
        }
        if ((next & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(next & 255, this._inputPtr);
        }
        this._textBuffer.append((char) ((prev << 6) | (next & 63)));
        return true;
    }

    /* JADX WARNING: type inference failed for: r0v7, types: [byte[]] */
    /* JADX WARNING: type inference failed for: r11v3, types: [byte] */
    /* JADX WARNING: type inference failed for: r5v6, types: [byte[]] */
    /* JADX WARNING: type inference failed for: r11v4, types: [byte] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r11v3, types: [byte] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r11v4, types: [byte] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte[], code=null, for r0v7, types: [byte[]] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte[], code=null, for r5v6, types: [byte[]] */
    /* JADX WARNING: Unknown variable types count: 2 */
    private final boolean _decodeSplitUTF8_4(int prev, int prevCount, int next) throws IOException {
        if (prevCount == 1) {
            if ((next & PsExtractor.AUDIO_STREAM) != 128) {
                _reportInvalidOther(next & 255, this._inputPtr);
            }
            prev = (prev << 6) | (next & 63);
            if (this._inputPtr >= this._inputEnd) {
                this._minorState = 44;
                this._pending32 = prev;
                this._pendingBytes = 2;
                return false;
            }
            prevCount = 2;
            ? r5 = this._inputBuffer;
            int i = this._inputPtr;
            this._inputPtr = i + 1;
            next = r5[i];
        }
        if (prevCount == 2) {
            if ((next & PsExtractor.AUDIO_STREAM) != 128) {
                _reportInvalidOther(next & 255, this._inputPtr);
            }
            prev = (prev << 6) | (next & 63);
            if (this._inputPtr >= this._inputEnd) {
                this._minorState = 44;
                this._pending32 = prev;
                this._pendingBytes = 3;
                return false;
            }
            ? r0 = this._inputBuffer;
            int i2 = this._inputPtr;
            this._inputPtr = i2 + 1;
            next = r0[i2];
        }
        if ((next & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(next & 255, this._inputPtr);
        }
        int c = ((prev << 6) | (next & 63)) - 65536;
        this._textBuffer.append((char) (55296 | (c >> 10)));
        this._textBuffer.append((char) ((c & 1023) | GeneratorBase.SURR2_FIRST));
        return true;
    }

    private final int _decodeCharEscape() throws IOException {
        if (this._inputEnd - this._inputPtr < 5) {
            return _decodeSplitEscaped(0, -1);
        }
        return _decodeFastCharEscape();
    }

    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r1v15, types: [byte, int] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r1v17, types: [byte, int] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r1v18, types: [byte, int] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=byte, code=int, for r1v19, types: [byte, int] */
    private final int _decodeFastCharEscape() throws IOException {
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
            return 13;
        }
        switch (c) {
            case 116:
                return 9;
            case 117:
                byte[] bArr2 = this._inputBuffer;
                int i2 = this._inputPtr;
                this._inputPtr = i2 + 1;
                int ch = bArr2[i2];
                int digit = CharTypes.charToHex(ch);
                int result = digit;
                if (digit >= 0) {
                    byte[] bArr3 = this._inputBuffer;
                    int i3 = this._inputPtr;
                    this._inputPtr = i3 + 1;
                    ch = bArr3[i3];
                    int digit2 = CharTypes.charToHex(ch);
                    if (digit2 >= 0) {
                        int result2 = (result << 4) | digit2;
                        byte[] bArr4 = this._inputBuffer;
                        int i4 = this._inputPtr;
                        this._inputPtr = i4 + 1;
                        ch = bArr4[i4];
                        int digit3 = CharTypes.charToHex(ch);
                        if (digit3 >= 0) {
                            int result3 = (result2 << 4) | digit3;
                            byte[] bArr5 = this._inputBuffer;
                            int i5 = this._inputPtr;
                            this._inputPtr = i5 + 1;
                            ch = bArr5[i5];
                            int digit4 = CharTypes.charToHex(ch);
                            if (digit4 >= 0) {
                                return (result3 << 4) | digit4;
                            }
                        }
                    }
                }
                _reportUnexpectedChar(ch & 255, "expected a hex-digit for character escape sequence");
                return -1;
            default:
                return _handleUnrecognizedCharacterEscape((char) c);
        }
    }

    private final int _decodeUTF8_2(int c, int d) throws IOException {
        if ((d & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        return ((c & 31) << 6) | (d & 63);
    }

    private final int _decodeUTF8_3(int c, int d, int e) throws IOException {
        int c2 = c & 15;
        if ((d & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        int c3 = (c2 << 6) | (d & 63);
        if ((e & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(e & 255, this._inputPtr);
        }
        return (c3 << 6) | (e & 63);
    }

    private final int _decodeUTF8_4(int c, int d, int e, int f) throws IOException {
        if ((d & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(d & 255, this._inputPtr);
        }
        int c2 = ((c & 7) << 6) | (d & 63);
        if ((e & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(e & 255, this._inputPtr);
        }
        int c3 = (c2 << 6) | (e & 63);
        if ((f & PsExtractor.AUDIO_STREAM) != 128) {
            _reportInvalidOther(f & 255, this._inputPtr);
        }
        return ((c3 << 6) | (f & 63)) - 65536;
    }
}
