package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory.Feature;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.MergedStream;
import com.fasterxml.jackson.core.io.UTF32Reader;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;
import java.io.ByteArrayInputStream;
import java.io.CharConversionException;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public final class ByteSourceJsonBootstrapper {
    public static final byte UTF8_BOM_1 = -17;
    public static final byte UTF8_BOM_2 = -69;
    public static final byte UTF8_BOM_3 = -65;
    private boolean _bigEndian = true;
    private final boolean _bufferRecyclable;
    private int _bytesPerChar;
    private final IOContext _context;
    private final InputStream _in;
    private final byte[] _inputBuffer;
    private int _inputEnd;
    private int _inputPtr;

    public ByteSourceJsonBootstrapper(IOContext ctxt, InputStream in) {
        this._context = ctxt;
        this._in = in;
        this._inputBuffer = ctxt.allocReadIOBuffer();
        this._inputPtr = 0;
        this._inputEnd = 0;
        this._bufferRecyclable = true;
    }

    public ByteSourceJsonBootstrapper(IOContext ctxt, byte[] inputBuffer, int inputStart, int inputLen) {
        this._context = ctxt;
        this._in = null;
        this._inputBuffer = inputBuffer;
        this._inputPtr = inputStart;
        this._inputEnd = inputStart + inputLen;
        this._bufferRecyclable = false;
    }

    public JsonEncoding detectEncoding() throws IOException {
        JsonEncoding enc;
        boolean foundEncoding = false;
        if (ensureLoaded(4)) {
            byte[] bArr = this._inputBuffer;
            int i = this._inputPtr;
            int quad = (bArr[i + 3] & 255) | (bArr[i] << 24) | ((bArr[i + 1] & 255) << 16) | ((bArr[i + 2] & 255) << 8);
            if (handleBOM(quad)) {
                foundEncoding = true;
            } else if (checkUTF32(quad)) {
                foundEncoding = true;
            } else if (checkUTF16(quad >>> 16)) {
                foundEncoding = true;
            }
        } else if (ensureLoaded(2)) {
            byte[] bArr2 = this._inputBuffer;
            int i2 = this._inputPtr;
            if (checkUTF16((bArr2[i2 + 1] & 255) | ((bArr2[i2] & 255) << 8))) {
                foundEncoding = true;
            }
        }
        if (!foundEncoding) {
            enc = JsonEncoding.UTF8;
        } else {
            int i3 = this._bytesPerChar;
            if (i3 != 4) {
                switch (i3) {
                    case 1:
                        enc = JsonEncoding.UTF8;
                        break;
                    case 2:
                        if (!this._bigEndian) {
                            enc = JsonEncoding.UTF16_LE;
                            break;
                        } else {
                            enc = JsonEncoding.UTF16_BE;
                            break;
                        }
                    default:
                        throw new RuntimeException("Internal error");
                }
            } else {
                enc = this._bigEndian ? JsonEncoding.UTF32_BE : JsonEncoding.UTF32_LE;
            }
        }
        this._context.setEncoding(enc);
        return enc;
    }

    public static int skipUTF8BOM(DataInput input) throws IOException {
        int b = input.readUnsignedByte();
        if (b != 239) {
            return b;
        }
        int b2 = input.readUnsignedByte();
        if (b2 == 187) {
            int b3 = input.readUnsignedByte();
            if (b3 == 191) {
                return input.readUnsignedByte();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Unexpected byte 0x");
            sb.append(Integer.toHexString(b3));
            sb.append(" following 0xEF 0xBB; should get 0xBF as part of UTF-8 BOM");
            throw new IOException(sb.toString());
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Unexpected byte 0x");
        sb2.append(Integer.toHexString(b2));
        sb2.append(" following 0xEF; should get 0xBB as part of UTF-8 BOM");
        throw new IOException(sb2.toString());
    }

    public Reader constructReader() throws IOException {
        JsonEncoding enc = this._context.getEncoding();
        int bits = enc.bits();
        if (bits == 8 || bits == 16) {
            InputStream in = this._in;
            if (in == 0) {
                in = new ByteArrayInputStream(this._inputBuffer, this._inputPtr, this._inputEnd);
            } else {
                int i = this._inputPtr;
                int i2 = this._inputEnd;
                if (i < i2) {
                    MergedStream mergedStream = new MergedStream(this._context, in, this._inputBuffer, i, i2);
                    in = mergedStream;
                }
            }
            return new InputStreamReader(in, enc.getJavaName());
        } else if (bits == 32) {
            IOContext iOContext = this._context;
            UTF32Reader uTF32Reader = new UTF32Reader(iOContext, this._in, this._inputBuffer, this._inputPtr, this._inputEnd, iOContext.getEncoding().isBigEndian());
            return uTF32Reader;
        } else {
            throw new RuntimeException("Internal error");
        }
    }

    public JsonParser constructParser(int parserFeatures, ObjectCodec codec, ByteQuadsCanonicalizer rootByteSymbols, CharsToNameCanonicalizer rootCharSymbols, int factoryFeatures) throws IOException {
        int i = factoryFeatures;
        if (detectEncoding() != JsonEncoding.UTF8) {
            ByteQuadsCanonicalizer byteQuadsCanonicalizer = rootByteSymbols;
        } else if (Feature.CANONICALIZE_FIELD_NAMES.enabledIn(i)) {
            UTF8StreamJsonParser uTF8StreamJsonParser = new UTF8StreamJsonParser(this._context, parserFeatures, this._in, codec, rootByteSymbols.makeChild(i), this._inputBuffer, this._inputPtr, this._inputEnd, this._bufferRecyclable);
            return uTF8StreamJsonParser;
        } else {
            ByteQuadsCanonicalizer byteQuadsCanonicalizer2 = rootByteSymbols;
        }
        ReaderBasedJsonParser readerBasedJsonParser = new ReaderBasedJsonParser(this._context, parserFeatures, constructReader(), codec, rootCharSymbols.makeChild(factoryFeatures));
        return readerBasedJsonParser;
    }

    public static MatchStrength hasJSONFormat(InputAccessor acc) throws IOException {
        if (!acc.hasMoreBytes()) {
            return MatchStrength.INCONCLUSIVE;
        }
        byte b = acc.nextByte();
        if (b == -17) {
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            if (acc.nextByte() != -69) {
                return MatchStrength.NO_MATCH;
            }
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            if (acc.nextByte() != -65) {
                return MatchStrength.NO_MATCH;
            }
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            b = acc.nextByte();
        }
        int ch = skipSpace(acc, b);
        if (ch < 0) {
            return MatchStrength.INCONCLUSIVE;
        }
        if (ch == 123) {
            int ch2 = skipSpace(acc);
            if (ch2 < 0) {
                return MatchStrength.INCONCLUSIVE;
            }
            if (ch2 == 34 || ch2 == 125) {
                return MatchStrength.SOLID_MATCH;
            }
            return MatchStrength.NO_MATCH;
        } else if (ch == 91) {
            int ch3 = skipSpace(acc);
            if (ch3 < 0) {
                return MatchStrength.INCONCLUSIVE;
            }
            if (ch3 == 93 || ch3 == 91) {
                return MatchStrength.SOLID_MATCH;
            }
            return MatchStrength.SOLID_MATCH;
        } else {
            MatchStrength strength = MatchStrength.WEAK_MATCH;
            if (ch == 34) {
                return strength;
            }
            if (ch <= 57 && ch >= 48) {
                return strength;
            }
            if (ch == 45) {
                int ch4 = skipSpace(acc);
                if (ch4 < 0) {
                    return MatchStrength.INCONCLUSIVE;
                }
                return (ch4 > 57 || ch4 < 48) ? MatchStrength.NO_MATCH : strength;
            } else if (ch == 110) {
                return tryMatch(acc, "ull", strength);
            } else {
                if (ch == 116) {
                    return tryMatch(acc, "rue", strength);
                }
                if (ch == 102) {
                    return tryMatch(acc, "alse", strength);
                }
                return MatchStrength.NO_MATCH;
            }
        }
    }

    private static MatchStrength tryMatch(InputAccessor acc, String matchStr, MatchStrength fullMatchStrength) throws IOException {
        int len = matchStr.length();
        for (int i = 0; i < len; i++) {
            if (!acc.hasMoreBytes()) {
                return MatchStrength.INCONCLUSIVE;
            }
            if (acc.nextByte() != matchStr.charAt(i)) {
                return MatchStrength.NO_MATCH;
            }
        }
        return fullMatchStrength;
    }

    private static int skipSpace(InputAccessor acc) throws IOException {
        if (!acc.hasMoreBytes()) {
            return -1;
        }
        return skipSpace(acc, acc.nextByte());
    }

    private static int skipSpace(InputAccessor acc, byte b) throws IOException {
        while (true) {
            byte ch = b & 255;
            if (ch != 32 && ch != 13 && ch != 10 && ch != 9) {
                return ch;
            }
            if (!acc.hasMoreBytes()) {
                return -1;
            }
            b = acc.nextByte();
        }
    }

    private boolean handleBOM(int quad) throws IOException {
        if (quad == -16842752) {
            reportWeirdUCS4("3412");
        } else if (quad == -131072) {
            this._inputPtr += 4;
            this._bytesPerChar = 4;
            this._bigEndian = false;
            return true;
        } else if (quad == 65279) {
            this._bigEndian = true;
            this._inputPtr += 4;
            this._bytesPerChar = 4;
            return true;
        } else if (quad == 65534) {
            reportWeirdUCS4("2143");
        }
        int msw = quad >>> 16;
        if (msw == 65279) {
            this._inputPtr += 2;
            this._bytesPerChar = 2;
            this._bigEndian = true;
            return true;
        } else if (msw == 65534) {
            this._inputPtr += 2;
            this._bytesPerChar = 2;
            this._bigEndian = false;
            return true;
        } else if ((quad >>> 8) != 15711167) {
            return false;
        } else {
            this._inputPtr += 3;
            this._bytesPerChar = 1;
            this._bigEndian = true;
            return true;
        }
    }

    private boolean checkUTF32(int quad) throws IOException {
        if ((quad >> 8) == 0) {
            this._bigEndian = true;
        } else if ((16777215 & quad) == 0) {
            this._bigEndian = false;
        } else if ((-16711681 & quad) == 0) {
            reportWeirdUCS4("3412");
        } else if ((-65281 & quad) != 0) {
            return false;
        } else {
            reportWeirdUCS4("2143");
        }
        this._bytesPerChar = 4;
        return true;
    }

    private boolean checkUTF16(int i16) {
        if ((65280 & i16) == 0) {
            this._bigEndian = true;
        } else if ((i16 & 255) != 0) {
            return false;
        } else {
            this._bigEndian = false;
        }
        this._bytesPerChar = 2;
        return true;
    }

    private void reportWeirdUCS4(String type) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Unsupported UCS-4 endianness (");
        sb.append(type);
        sb.append(") detected");
        throw new CharConversionException(sb.toString());
    }

    /* access modifiers changed from: protected */
    public boolean ensureLoaded(int minimum) throws IOException {
        int count;
        int gotten = this._inputEnd - this._inputPtr;
        while (gotten < minimum) {
            InputStream inputStream = this._in;
            if (inputStream == null) {
                count = -1;
            } else {
                byte[] bArr = this._inputBuffer;
                int i = this._inputEnd;
                count = inputStream.read(bArr, i, bArr.length - i);
            }
            if (count < 1) {
                return false;
            }
            this._inputEnd += count;
            gotten += count;
        }
        return true;
    }
}
