package okhttp3.internal.tls;

import com.google.android.exoplayer2.extractor.ts.PsExtractor;
import javax.security.auth.x500.X500Principal;

final class DistinguishedNameParser {
    private int beg;
    private char[] chars;
    private int cur;
    private final String dn;
    private int end;
    private final int length = this.dn.length();
    private int pos;

    DistinguishedNameParser(X500Principal principal) {
        this.dn = principal.getName("RFC2253");
    }

    private String nextAT() {
        while (true) {
            int i = this.pos;
            if (i >= this.length || this.chars[i] != ' ') {
                int i2 = this.pos;
            } else {
                this.pos = i + 1;
            }
        }
        int i22 = this.pos;
        if (i22 == this.length) {
            return null;
        }
        this.beg = i22;
        this.pos = i22 + 1;
        while (true) {
            int i3 = this.pos;
            if (i3 >= this.length) {
                break;
            }
            char[] cArr = this.chars;
            if (cArr[i3] == '=' || cArr[i3] == ' ') {
                break;
            }
            this.pos = i3 + 1;
        }
        int i4 = this.pos;
        if (i4 < this.length) {
            this.end = i4;
            if (this.chars[i4] == ' ') {
                while (true) {
                    int i5 = this.pos;
                    if (i5 >= this.length) {
                        break;
                    }
                    char[] cArr2 = this.chars;
                    if (cArr2[i5] == '=' || cArr2[i5] != ' ') {
                        break;
                    }
                    this.pos = i5 + 1;
                }
                char[] cArr3 = this.chars;
                int i6 = this.pos;
                if (cArr3[i6] != '=' || i6 == this.length) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unexpected end of DN: ");
                    sb.append(this.dn);
                    throw new IllegalStateException(sb.toString());
                }
            }
            this.pos++;
            while (true) {
                int i7 = this.pos;
                if (i7 >= this.length || this.chars[i7] != ' ') {
                    int i8 = this.end;
                    int i9 = this.beg;
                } else {
                    this.pos = i7 + 1;
                }
            }
            int i82 = this.end;
            int i92 = this.beg;
            if (i82 - i92 > 4) {
                char[] cArr4 = this.chars;
                if (cArr4[i92 + 3] == '.' && (cArr4[i92] == 'O' || cArr4[i92] == 'o')) {
                    char[] cArr5 = this.chars;
                    int i10 = this.beg;
                    if (cArr5[i10 + 1] == 'I' || cArr5[i10 + 1] == 'i') {
                        char[] cArr6 = this.chars;
                        int i11 = this.beg;
                        if (cArr6[i11 + 2] == 'D' || cArr6[i11 + 2] == 'd') {
                            this.beg += 4;
                        }
                    }
                }
            }
            char[] cArr7 = this.chars;
            int i12 = this.beg;
            return new String(cArr7, i12, this.end - i12);
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Unexpected end of DN: ");
        sb2.append(this.dn);
        throw new IllegalStateException(sb2.toString());
    }

    private String quotedAV() {
        this.pos++;
        this.beg = this.pos;
        this.end = this.beg;
        while (true) {
            int i = this.pos;
            if (i != this.length) {
                char[] cArr = this.chars;
                if (cArr[i] == '\"') {
                    this.pos = i + 1;
                    while (true) {
                        int i2 = this.pos;
                        if (i2 >= this.length || this.chars[i2] != ' ') {
                            char[] cArr2 = this.chars;
                            int i3 = this.beg;
                        } else {
                            this.pos = i2 + 1;
                        }
                    }
                    char[] cArr22 = this.chars;
                    int i32 = this.beg;
                    return new String(cArr22, i32, this.end - i32);
                }
                if (cArr[i] == '\\') {
                    cArr[this.end] = getEscaped();
                } else {
                    cArr[this.end] = cArr[i];
                }
                this.pos++;
                this.end++;
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Unexpected end of DN: ");
                sb.append(this.dn);
                throw new IllegalStateException(sb.toString());
            }
        }
    }

    private String hexAV() {
        int i = this.pos;
        if (i + 4 < this.length) {
            this.beg = i;
            this.pos = i + 1;
            while (true) {
                int i2 = this.pos;
                if (i2 == this.length) {
                    break;
                }
                char[] cArr = this.chars;
                if (cArr[i2] == '+' || cArr[i2] == ',' || cArr[i2] == ';') {
                    break;
                } else if (cArr[i2] == ' ') {
                    this.end = i2;
                    this.pos = i2 + 1;
                    while (true) {
                        int i3 = this.pos;
                        if (i3 >= this.length || this.chars[i3] != ' ') {
                            break;
                        }
                        this.pos = i3 + 1;
                    }
                } else {
                    if (cArr[i2] >= 'A' && cArr[i2] <= 'F') {
                        cArr[i2] = (char) (cArr[i2] + ' ');
                    }
                    this.pos++;
                }
            }
            this.end = this.pos;
            int i4 = this.end;
            int i5 = this.beg;
            int hexLen = i4 - i5;
            if (hexLen < 5 || (hexLen & 1) == 0) {
                StringBuilder sb = new StringBuilder();
                sb.append("Unexpected end of DN: ");
                sb.append(this.dn);
                throw new IllegalStateException(sb.toString());
            }
            byte[] encoded = new byte[(hexLen / 2)];
            int p = i5 + 1;
            for (int i6 = 0; i6 < encoded.length; i6++) {
                encoded[i6] = (byte) getByte(p);
                p += 2;
            }
            return new String(this.chars, this.beg, hexLen);
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Unexpected end of DN: ");
        sb2.append(this.dn);
        throw new IllegalStateException(sb2.toString());
    }

    /* JADX WARNING: Code restructure failed: missing block: B:29:0x009a, code lost:
        r1 = r6.chars;
        r2 = r6.beg;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x00a6, code lost:
        return new java.lang.String(r1, r2, r6.cur - r2);
     */
    private String escapedAV() {
        int i = this.pos;
        this.beg = i;
        this.end = i;
        while (true) {
            int i2 = this.pos;
            if (i2 >= this.length) {
                char[] cArr = this.chars;
                int i3 = this.beg;
                return new String(cArr, i3, this.end - i3);
            }
            char[] cArr2 = this.chars;
            char c = cArr2[i2];
            if (c == ' ') {
                int i4 = this.end;
                this.cur = i4;
                this.pos = i2 + 1;
                this.end = i4 + 1;
                cArr2[i4] = ' ';
                while (true) {
                    int i5 = this.pos;
                    if (i5 < this.length) {
                        char[] cArr3 = this.chars;
                        if (cArr3[i5] == ' ') {
                            int i6 = this.end;
                            this.end = i6 + 1;
                            cArr3[i6] = ' ';
                            this.pos = i5 + 1;
                        }
                    }
                }
                int i7 = this.pos;
                if (i7 != this.length) {
                    char[] cArr4 = this.chars;
                    if (!(cArr4[i7] == ',' || cArr4[i7] == '+' || cArr4[i7] == ';')) {
                    }
                }
            } else if (c != ';') {
                if (c != '\\') {
                    switch (c) {
                        case '+':
                        case ',':
                            break;
                        default:
                            int i8 = this.end;
                            this.end = i8 + 1;
                            cArr2[i8] = cArr2[i2];
                            this.pos = i2 + 1;
                            continue;
                    }
                } else {
                    int i9 = this.end;
                    this.end = i9 + 1;
                    cArr2[i9] = getEscaped();
                    this.pos++;
                }
            }
        }
        char[] cArr5 = this.chars;
        int i10 = this.beg;
        return new String(cArr5, i10, this.end - i10);
    }

    private char getEscaped() {
        this.pos++;
        int i = this.pos;
        if (i != this.length) {
            char c = this.chars[i];
            if (!(c == ' ' || c == '%' || c == '\\' || c == '_')) {
                switch (c) {
                    case '\"':
                    case '#':
                        break;
                    default:
                        switch (c) {
                            case '*':
                            case '+':
                            case ',':
                                break;
                            default:
                                switch (c) {
                                    case ';':
                                    case '<':
                                    case '=':
                                    case '>':
                                        break;
                                    default:
                                        return getUTF8();
                                }
                        }
                }
            }
            return this.chars[this.pos];
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Unexpected end of DN: ");
        sb.append(this.dn);
        throw new IllegalStateException(sb.toString());
    }

    private char getUTF8() {
        int count;
        int res;
        int res2 = getByte(this.pos);
        this.pos++;
        if (res2 < 128) {
            return (char) res2;
        }
        if (res2 < 192 || res2 > 247) {
            return '?';
        }
        if (res2 <= 223) {
            count = 1;
            res = res2 & 31;
        } else if (res2 <= 239) {
            count = 2;
            res = res2 & 15;
        } else {
            count = 3;
            res = res2 & 7;
        }
        for (int i = 0; i < count; i++) {
            this.pos++;
            int i2 = this.pos;
            if (i2 == this.length || this.chars[i2] != '\\') {
                return '?';
            }
            this.pos = i2 + 1;
            int b = getByte(this.pos);
            this.pos++;
            if ((b & PsExtractor.AUDIO_STREAM) != 128) {
                return '?';
            }
            res = (res << 6) + (b & 63);
        }
        return (char) res;
    }

    private int getByte(int position) {
        int b1;
        int b2;
        if (position + 1 < this.length) {
            char b12 = this.chars[position];
            if (b12 >= '0' && b12 <= '9') {
                b1 = b12 - '0';
            } else if (b12 >= 'a' && b12 <= 'f') {
                b1 = b12 - 'W';
            } else if (b12 < 'A' || b12 > 'F') {
                StringBuilder sb = new StringBuilder();
                sb.append("Malformed DN: ");
                sb.append(this.dn);
                throw new IllegalStateException(sb.toString());
            } else {
                b1 = b12 - '7';
            }
            char b22 = this.chars[position + 1];
            if (b22 >= '0' && b22 <= '9') {
                b2 = b22 - '0';
            } else if (b22 >= 'a' && b22 <= 'f') {
                b2 = b22 - 'W';
            } else if (b22 < 'A' || b22 > 'F') {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Malformed DN: ");
                sb2.append(this.dn);
                throw new IllegalStateException(sb2.toString());
            } else {
                b2 = b22 - '7';
            }
            return (b1 << 4) + b2;
        }
        StringBuilder sb3 = new StringBuilder();
        sb3.append("Malformed DN: ");
        sb3.append(this.dn);
        throw new IllegalStateException(sb3.toString());
    }

    public String findMostSpecific(String attributeType) {
        this.pos = 0;
        this.beg = 0;
        this.end = 0;
        this.cur = 0;
        this.chars = this.dn.toCharArray();
        String attType = nextAT();
        if (attType == null) {
            return null;
        }
        do {
            String attValue = "";
            int i = this.pos;
            if (i == this.length) {
                return null;
            }
            switch (this.chars[i]) {
                case '\"':
                    attValue = quotedAV();
                    break;
                case '#':
                    attValue = hexAV();
                    break;
                case '+':
                case ',':
                case ';':
                    break;
                default:
                    attValue = escapedAV();
                    break;
            }
            if (attributeType.equalsIgnoreCase(attType)) {
                return attValue;
            }
            int i2 = this.pos;
            if (i2 >= this.length) {
                return null;
            }
            char[] cArr = this.chars;
            if (cArr[i2] == ',' || cArr[i2] == ';' || cArr[i2] == '+') {
                this.pos++;
                attType = nextAT();
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Malformed DN: ");
                sb.append(this.dn);
                throw new IllegalStateException(sb.toString());
            }
        } while (attType != null);
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Malformed DN: ");
        sb2.append(this.dn);
        throw new IllegalStateException(sb2.toString());
    }
}
