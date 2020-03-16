package org.quantumbadger.redreader.reddit.prepared.markdown;

import java.util.HashSet;
import org.apache.commons.lang3.StringEscapeUtils;

public final class MarkdownTokenizer {
    public static final int TOKEN_ASTERISK = -3;
    public static final int TOKEN_ASTERISK_DOUBLE = -4;
    public static final int TOKEN_BRACKET_SQUARE_CLOSE = -9;
    public static final int TOKEN_BRACKET_SQUARE_OPEN = -8;
    public static final int TOKEN_CARET = -6;
    public static final int TOKEN_GRAVE = -7;
    public static final int TOKEN_PAREN_CLOSE = -11;
    public static final int TOKEN_PAREN_OPEN = -10;
    public static final int TOKEN_TILDE_DOUBLE = -5;
    public static final int TOKEN_UNDERSCORE = -1;
    public static final int TOKEN_UNDERSCORE_DOUBLE = -2;
    public static final int TOKEN_UNICODE_CLOSE = -13;
    public static final int TOKEN_UNICODE_OPEN = -12;
    private static final char[][] linkPrefixes = {"http://".toCharArray(), "https://".toCharArray(), "www.".toCharArray()};
    private static final char[][] linkPrefixes_reddit = {"/r/".toCharArray(), "r/".toCharArray(), "/u/".toCharArray(), "u/".toCharArray(), "/user/".toCharArray()};
    private static final char[][] reverseLookup = new char[20][];
    private static final HashSet<Integer> unicodeWhitespace = new HashSet<>();

    static {
        char[][] cArr = reverseLookup;
        cArr[19] = new char[]{'_'};
        cArr[18] = new char[]{'_', '_'};
        cArr[17] = new char[]{'*'};
        cArr[16] = new char[]{'*', '*'};
        cArr[15] = new char[]{'~', '~'};
        cArr[14] = new char[]{'^'};
        cArr[13] = new char[]{'`'};
        cArr[12] = new char[]{'['};
        cArr[11] = new char[]{']'};
        cArr[10] = new char[]{'('};
        cArr[9] = new char[]{')'};
        cArr[8] = new char[]{'&'};
        cArr[7] = new char[]{';'};
        unicodeWhitespace.add(Integer.valueOf(9));
        unicodeWhitespace.add(Integer.valueOf(11));
        unicodeWhitespace.add(Integer.valueOf(160));
        unicodeWhitespace.add(Integer.valueOf(5760));
        unicodeWhitespace.add(Integer.valueOf(8192));
        unicodeWhitespace.add(Integer.valueOf(8193));
        unicodeWhitespace.add(Integer.valueOf(8194));
        unicodeWhitespace.add(Integer.valueOf(8195));
        unicodeWhitespace.add(Integer.valueOf(8196));
        unicodeWhitespace.add(Integer.valueOf(8197));
        unicodeWhitespace.add(Integer.valueOf(8198));
        unicodeWhitespace.add(Integer.valueOf(8199));
        unicodeWhitespace.add(Integer.valueOf(8200));
        unicodeWhitespace.add(Integer.valueOf(8201));
        unicodeWhitespace.add(Integer.valueOf(8202));
        unicodeWhitespace.add(Integer.valueOf(8239));
        unicodeWhitespace.add(Integer.valueOf(8287));
        unicodeWhitespace.add(Integer.valueOf(12288));
    }

    public static boolean isUnicodeWhitespace(int codepoint) {
        return unicodeWhitespace.contains(Integer.valueOf(codepoint));
    }

    public static IntArrayLengthPair tokenize(CharArrSubstring input) {
        IntArrayLengthPair tmp1 = new IntArrayLengthPair(input.length * 3);
        IntArrayLengthPair tmp2 = new IntArrayLengthPair(input.length * 3);
        tmp1.pos = input.length;
        for (int i = 0; i < input.length; i++) {
            tmp1.data[i] = input.charAt(i);
        }
        naiveTokenize(tmp1, tmp2);
        clean(tmp2, tmp1);
        linkify(tmp1, tmp2);
        clean(tmp2, tmp1);
        return tmp1;
    }

    /* JADX WARNING: Removed duplicated region for block: B:111:0x01c3  */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x01fe  */
    private static void linkify(IntArrayLengthPair input, IntArrayLengthPair output) {
        IntArrayLengthPair intArrayLengthPair = input;
        IntArrayLengthPair intArrayLengthPair2 = output;
        if (intArrayLengthPair.data.length <= intArrayLengthPair2.data.length * 3) {
            output.clear();
            int inBrackets = 0;
            boolean lastCharOk = true;
            int i = 0;
            while (i < intArrayLengthPair.pos) {
                int token = intArrayLengthPair.data[i];
                int i2 = 32;
                if (token != 32) {
                    if (token != 47) {
                        if (token != 104) {
                            if (!(token == 114 || token == 117)) {
                                if (token != 119) {
                                    switch (token) {
                                        case TOKEN_PAREN_CLOSE /*-11*/:
                                        case TOKEN_BRACKET_SQUARE_CLOSE /*-9*/:
                                            int[] iArr = intArrayLengthPair2.data;
                                            int i3 = intArrayLengthPair2.pos;
                                            intArrayLengthPair2.pos = i3 + 1;
                                            iArr[i3] = token;
                                            inBrackets--;
                                            lastCharOk = true;
                                            break;
                                        case TOKEN_PAREN_OPEN /*-10*/:
                                        case TOKEN_BRACKET_SQUARE_OPEN /*-8*/:
                                            int[] iArr2 = intArrayLengthPair2.data;
                                            int i4 = intArrayLengthPair2.pos;
                                            intArrayLengthPair2.pos = i4 + 1;
                                            iArr2[i4] = token;
                                            inBrackets++;
                                            lastCharOk = true;
                                            break;
                                        default:
                                            lastCharOk = token < 0 || !Character.isLetterOrDigit(token);
                                            int[] iArr3 = intArrayLengthPair2.data;
                                            int i5 = intArrayLengthPair2.pos;
                                            intArrayLengthPair2.pos = i5 + 1;
                                            iArr3[i5] = token;
                                            break;
                                    }
                                }
                            }
                        }
                        if (inBrackets != 0 || !lastCharOk) {
                            int[] iArr4 = intArrayLengthPair2.data;
                            int i6 = intArrayLengthPair2.pos;
                            intArrayLengthPair2.pos = i6 + 1;
                            iArr4[i6] = token;
                        } else {
                            int linkStartType = getLinkStartType(intArrayLengthPair.data, i, intArrayLengthPair.pos);
                            if (linkStartType >= 0) {
                                int linkStartPos = i;
                                int linkPrefixEndPos = linkPrefixes[linkStartType].length + linkStartPos;
                                boolean hasOpeningParen = false;
                                int linkEndPos = linkPrefixEndPos;
                                while (linkEndPos < intArrayLengthPair.pos) {
                                    int lToken = intArrayLengthPair.data[linkEndPos];
                                    boolean isValidChar = (lToken == i2 || lToken == 60 || lToken == 62 || lToken == -7 || lToken == -8 || lToken == -9) ? false : true;
                                    if (lToken == 40) {
                                        hasOpeningParen = true;
                                    }
                                    if (isValidChar) {
                                        linkEndPos++;
                                        i2 = 32;
                                    } else {
                                        while (true) {
                                            if (intArrayLengthPair.data[linkEndPos - 1] != 46 || intArrayLengthPair.data[linkEndPos - 1] == 44 || intArrayLengthPair.data[linkEndPos - 1] == 63 || intArrayLengthPair.data[linkEndPos - 1] == 59) {
                                                linkEndPos--;
                                            } else {
                                                if (intArrayLengthPair.data[linkEndPos - 1] == 34) {
                                                    linkEndPos--;
                                                }
                                                if (intArrayLengthPair.data[linkEndPos - 1] == 39) {
                                                    linkEndPos--;
                                                }
                                                if (!hasOpeningParen && intArrayLengthPair.data[linkEndPos - 1] == 41) {
                                                    linkEndPos--;
                                                }
                                                if (linkEndPos - linkPrefixEndPos >= 2) {
                                                    int[] reverted = revert(intArrayLengthPair.data, linkStartPos, linkEndPos);
                                                    int[] iArr5 = intArrayLengthPair2.data;
                                                    int i7 = intArrayLengthPair2.pos;
                                                    intArrayLengthPair2.pos = i7 + 1;
                                                    iArr5[i7] = -8;
                                                    intArrayLengthPair2.append(reverted);
                                                    int[] iArr6 = intArrayLengthPair2.data;
                                                    int i8 = intArrayLengthPair2.pos;
                                                    intArrayLengthPair2.pos = i8 + 1;
                                                    iArr6[i8] = -9;
                                                    int[] iArr7 = intArrayLengthPair2.data;
                                                    int i9 = intArrayLengthPair2.pos;
                                                    intArrayLengthPair2.pos = i9 + 1;
                                                    iArr7[i9] = -10;
                                                    intArrayLengthPair2.append(reverted);
                                                    int[] iArr8 = intArrayLengthPair2.data;
                                                    int i10 = intArrayLengthPair2.pos;
                                                    intArrayLengthPair2.pos = i10 + 1;
                                                    iArr8[i10] = -11;
                                                    i = linkEndPos - 1;
                                                } else {
                                                    int[] iArr9 = intArrayLengthPair2.data;
                                                    int i11 = intArrayLengthPair2.pos;
                                                    intArrayLengthPair2.pos = i11 + 1;
                                                    iArr9[i11] = token;
                                                }
                                            }
                                        }
                                    }
                                }
                                while (true) {
                                    if (intArrayLengthPair.data[linkEndPos - 1] != 46) {
                                    }
                                    linkEndPos--;
                                }
                            } else {
                                int[] iArr10 = intArrayLengthPair2.data;
                                int i12 = intArrayLengthPair2.pos;
                                intArrayLengthPair2.pos = i12 + 1;
                                iArr10[i12] = token;
                            }
                        }
                        lastCharOk = false;
                    }
                    if (inBrackets != 0 || !lastCharOk) {
                        int[] iArr11 = intArrayLengthPair2.data;
                        int i13 = intArrayLengthPair2.pos;
                        intArrayLengthPair2.pos = i13 + 1;
                        iArr11[i13] = token;
                    } else {
                        int linkStartType2 = getRedditLinkStartType(intArrayLengthPair.data, i, intArrayLengthPair.pos);
                        if (linkStartType2 >= 0) {
                            int linkStartPos2 = i;
                            int linkPrefixEndPos2 = linkPrefixes_reddit[linkStartType2].length + linkStartPos2;
                            int linkEndPos2 = linkPrefixEndPos2;
                            while (linkEndPos2 < intArrayLengthPair.pos) {
                                int lToken2 = intArrayLengthPair.data[linkEndPos2];
                                if ((lToken2 >= 97 && lToken2 <= 122) || (lToken2 >= 65 && lToken2 <= 90) || ((lToken2 >= 48 && lToken2 <= 57) || lToken2 == 95 || lToken2 == -1 || lToken2 == -2 || lToken2 == 43 || lToken2 == 45)) {
                                    linkEndPos2++;
                                } else if (linkEndPos2 - linkPrefixEndPos2 <= 2) {
                                    int[] reverted2 = revert(intArrayLengthPair.data, linkStartPos2, linkEndPos2);
                                    int[] iArr12 = intArrayLengthPair2.data;
                                    int i14 = intArrayLengthPair2.pos;
                                    intArrayLengthPair2.pos = i14 + 1;
                                    iArr12[i14] = -8;
                                    intArrayLengthPair2.append(reverted2);
                                    int[] iArr13 = intArrayLengthPair2.data;
                                    int i15 = intArrayLengthPair2.pos;
                                    intArrayLengthPair2.pos = i15 + 1;
                                    iArr13[i15] = -9;
                                    int[] iArr14 = intArrayLengthPair2.data;
                                    int i16 = intArrayLengthPair2.pos;
                                    intArrayLengthPair2.pos = i16 + 1;
                                    iArr14[i16] = -10;
                                    intArrayLengthPair2.append(reverted2);
                                    int[] iArr15 = intArrayLengthPair2.data;
                                    int i17 = intArrayLengthPair2.pos;
                                    intArrayLengthPair2.pos = i17 + 1;
                                    iArr15[i17] = -11;
                                    i = linkEndPos2 - 1;
                                } else {
                                    int[] iArr16 = intArrayLengthPair2.data;
                                    int i18 = intArrayLengthPair2.pos;
                                    intArrayLengthPair2.pos = i18 + 1;
                                    iArr16[i18] = token;
                                }
                            }
                            if (linkEndPos2 - linkPrefixEndPos2 <= 2) {
                            }
                        } else {
                            int[] iArr17 = intArrayLengthPair2.data;
                            int i19 = intArrayLengthPair2.pos;
                            intArrayLengthPair2.pos = i19 + 1;
                            iArr17[i19] = token;
                        }
                    }
                    lastCharOk = false;
                } else {
                    int[] iArr18 = intArrayLengthPair2.data;
                    int i20 = intArrayLengthPair2.pos;
                    intArrayLengthPair2.pos = i20 + 1;
                    iArr18[i20] = 32;
                    lastCharOk = true;
                }
                i++;
            }
            return;
        }
        throw new RuntimeException();
    }

    /* JADX WARNING: Removed duplicated region for block: B:153:0x0259  */
    /* JADX WARNING: Removed duplicated region for block: B:204:0x0374  */
    /* JADX WARNING: Removed duplicated region for block: B:212:0x0399  */
    public static void clean(IntArrayLengthPair input, IntArrayLengthPair output) {
        int openingUnicode;
        Integer codePoint;
        Integer codePoint2;
        boolean linkParseSuccess;
        IntArrayLengthPair intArrayLengthPair = input;
        IntArrayLengthPair intArrayLengthPair2 = output;
        boolean[] toRevert = new boolean[intArrayLengthPair.pos];
        boolean[] toDelete = new boolean[intArrayLengthPair.pos];
        int i = 0;
        int lastBracketSquareOpen = -1;
        int openingTildeDouble = -1;
        int openingAsteriskDouble = -1;
        int openingAsterisk = -1;
        int openingUnderscoreDouble = -1;
        int openingUnderscore = -1;
        while (i < intArrayLengthPair.pos) {
            int c = intArrayLengthPair.data[i];
            boolean beforeASpace = i + 1 < intArrayLengthPair.pos && intArrayLengthPair.data[i + 1] == 32;
            boolean afterASpace = i > 0 && intArrayLengthPair.data[i + -1] == 32;
            if (c != 32) {
                switch (c) {
                    case TOKEN_UNICODE_CLOSE /*-13*/:
                    case TOKEN_PAREN_CLOSE /*-11*/:
                    case TOKEN_PAREN_OPEN /*-10*/:
                        boolean z = beforeASpace;
                        toRevert[i] = true;
                        openingUnicode = 1;
                        break;
                    case TOKEN_UNICODE_OPEN /*-12*/:
                        boolean z2 = beforeASpace;
                        int openingUnicode2 = i;
                        int closingUnicode = indexOf(intArrayLengthPair.data, -13, i + 1, Math.min(intArrayLengthPair.pos, i + 20));
                        if (closingUnicode < 0) {
                            toRevert[i] = true;
                            openingUnicode = 1;
                        } else if (intArrayLengthPair.data[i + 1] != 35) {
                            try {
                                codePoint2 = null;
                                try {
                                    String name = new String(intArrayLengthPair.data, openingUnicode2 + 1, (closingUnicode - openingUnicode2) - 1);
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("&");
                                    sb.append(name);
                                    sb.append(";");
                                    String result = StringEscapeUtils.unescapeHtml4(sb.toString());
                                    if (result.length() == 1) {
                                        codePoint = Integer.valueOf(result.charAt(0));
                                    } else if (name.equalsIgnoreCase("apos")) {
                                        codePoint = Integer.valueOf(39);
                                    } else if (name.equalsIgnoreCase("nsub")) {
                                        codePoint = Integer.valueOf(8836);
                                    } else {
                                        codePoint = null;
                                    }
                                } catch (Throwable th) {
                                    codePoint = codePoint2;
                                    if (codePoint != null) {
                                    }
                                    openingUnicode = 1;
                                    i += openingUnicode;
                                    IntArrayLengthPair intArrayLengthPair3 = output;
                                }
                            } catch (Throwable th2) {
                                codePoint2 = null;
                                codePoint = codePoint2;
                                if (codePoint != null) {
                                }
                                openingUnicode = 1;
                                i += openingUnicode;
                                IntArrayLengthPair intArrayLengthPair32 = output;
                            }
                            if (codePoint != null) {
                                if (unicodeWhitespace.contains(codePoint)) {
                                    intArrayLengthPair.data[openingUnicode2] = 32;
                                } else {
                                    intArrayLengthPair.data[openingUnicode2] = codePoint.intValue();
                                }
                                for (int j = openingUnicode2 + 1; j <= closingUnicode; j++) {
                                    toDelete[j] = true;
                                }
                                i = closingUnicode;
                            } else {
                                toRevert[i] = true;
                            }
                            openingUnicode = 1;
                        } else if (intArrayLengthPair.data[i + 2] == 120 && isHexDigits(intArrayLengthPair.data, openingUnicode2 + 3, closingUnicode)) {
                            int codePoint3 = getHex(intArrayLengthPair.data, openingUnicode2 + 3, closingUnicode);
                            if (unicodeWhitespace.contains(Integer.valueOf(codePoint3))) {
                                intArrayLengthPair.data[openingUnicode2] = 32;
                            } else {
                                intArrayLengthPair.data[openingUnicode2] = codePoint3;
                            }
                            for (int j2 = openingUnicode2 + 1; j2 <= closingUnicode; j2++) {
                                toDelete[j2] = true;
                            }
                            i = closingUnicode;
                            openingUnicode = 1;
                        } else if (isDigits(intArrayLengthPair.data, openingUnicode2 + 2, closingUnicode)) {
                            int codePoint4 = getDecimal(intArrayLengthPair.data, openingUnicode2 + 2, closingUnicode);
                            if (unicodeWhitespace.contains(Integer.valueOf(codePoint4))) {
                                intArrayLengthPair.data[openingUnicode2] = 32;
                            } else {
                                intArrayLengthPair.data[openingUnicode2] = codePoint4;
                            }
                            for (int j3 = openingUnicode2 + 1; j3 <= closingUnicode; j3++) {
                                toDelete[j3] = true;
                            }
                            i = closingUnicode;
                            openingUnicode = 1;
                        } else {
                            toRevert[i] = true;
                            openingUnicode = 1;
                        }
                        break;
                    case TOKEN_BRACKET_SQUARE_CLOSE /*-9*/:
                        boolean z3 = beforeASpace;
                        if (lastBracketSquareOpen < 0) {
                            toRevert[i] = true;
                        } else {
                            int lastBracketSquareClose = i;
                            int parenOpenPos = indexOf(intArrayLengthPair.data, -10, lastBracketSquareClose + 1, intArrayLengthPair.pos);
                            if (parenOpenPos >= 0 && isSpaces(intArrayLengthPair.data, lastBracketSquareClose + 1, parenOpenPos)) {
                                int parenClosePos = findParenClosePos(intArrayLengthPair, parenOpenPos + 1);
                                if (parenClosePos >= 0) {
                                    boolean linkParseSuccess2 = true;
                                    int j4 = lastBracketSquareOpen + 1;
                                    while (j4 < lastBracketSquareClose) {
                                        boolean linkParseSuccess3 = linkParseSuccess2;
                                        if (intArrayLengthPair.data[j4] != -8) {
                                            if (intArrayLengthPair.data[j4] != -9) {
                                                j4++;
                                                linkParseSuccess2 = linkParseSuccess3;
                                                IntArrayLengthPair intArrayLengthPair4 = output;
                                            }
                                        }
                                        toRevert[j4] = true;
                                        j4++;
                                        linkParseSuccess2 = linkParseSuccess3;
                                        IntArrayLengthPair intArrayLengthPair42 = output;
                                    }
                                    linkParseSuccess = linkParseSuccess2;
                                    for (int j5 = lastBracketSquareClose + 1; j5 < parenOpenPos; j5++) {
                                        toDelete[j5] = true;
                                    }
                                    for (int j6 = parenOpenPos + 1; j6 < parenClosePos; j6++) {
                                        if (intArrayLengthPair.data[j6] < 0) {
                                            toRevert[j6] = true;
                                        } else if (intArrayLengthPair.data[j6] == 32 && intArrayLengthPair.data[j6 - 1] == 32) {
                                            toDelete[j6] = true;
                                        }
                                    }
                                    for (int j7 = parenOpenPos + 1; intArrayLengthPair.data[j7] == 32; j7++) {
                                        toDelete[j7] = true;
                                    }
                                    for (int j8 = parenClosePos - 1; intArrayLengthPair.data[j8] == 32; j8--) {
                                        toDelete[j8] = true;
                                    }
                                    i = parenClosePos;
                                    if (!linkParseSuccess) {
                                        toRevert[lastBracketSquareOpen] = true;
                                        toRevert[lastBracketSquareClose] = true;
                                    }
                                }
                            }
                            linkParseSuccess = false;
                            if (!linkParseSuccess) {
                            }
                        }
                        lastBracketSquareOpen = -1;
                        openingUnicode = 1;
                        break;
                    case TOKEN_BRACKET_SQUARE_OPEN /*-8*/:
                        boolean z4 = beforeASpace;
                        if (lastBracketSquareOpen >= 0) {
                            toRevert[lastBracketSquareOpen] = true;
                            lastBracketSquareOpen = i;
                            openingUnicode = 1;
                            break;
                        } else {
                            int closingSquareBracket = findCloseWellBracketed(intArrayLengthPair.data, -8, -9, i, intArrayLengthPair.pos);
                            if (closingSquareBracket > i) {
                                int parenOpenPos2 = indexOf(intArrayLengthPair.data, -10, closingSquareBracket + 1, intArrayLengthPair.pos);
                                if (parenOpenPos2 <= closingSquareBracket || !isSpaces(intArrayLengthPair.data, closingSquareBracket + 1, parenOpenPos2)) {
                                    toRevert[i] = true;
                                } else {
                                    lastBracketSquareOpen = i;
                                    for (int j9 = i + 1; j9 < closingSquareBracket; j9++) {
                                        if (intArrayLengthPair.data[j9] == -8) {
                                            intArrayLengthPair.data[j9] = 91;
                                        } else if (intArrayLengthPair.data[j9] == -9) {
                                            intArrayLengthPair.data[j9] = 93;
                                        }
                                    }
                                }
                            } else {
                                toRevert[i] = true;
                            }
                            openingUnicode = 1;
                            break;
                        }
                        break;
                    case TOKEN_GRAVE /*-7*/:
                        int openingGrave = i;
                        int i2 = c;
                        boolean z5 = beforeASpace;
                        int closingGrave = indexOf(intArrayLengthPair.data, -7, i + 1, intArrayLengthPair.pos);
                        if (closingGrave >= 0) {
                            for (int j10 = openingGrave + 1; j10 < closingGrave; j10++) {
                                if (intArrayLengthPair.data[j10] < 0) {
                                    toRevert[j10] = true;
                                }
                            }
                            i = closingGrave;
                            openingUnicode = 1;
                            break;
                        } else {
                            toRevert[i] = true;
                            openingUnicode = 1;
                            break;
                        }
                    case TOKEN_CARET /*-6*/:
                        if (intArrayLengthPair.pos > i + 1 && intArrayLengthPair.data[i + 1] != 32) {
                            openingUnicode = 1;
                            break;
                        } else {
                            toRevert[i] = true;
                            openingUnicode = 1;
                            break;
                        }
                        break;
                    case -5:
                        if (i == 0 || openingTildeDouble != i - 1) {
                            if (openingTildeDouble >= 0) {
                                if (!afterASpace) {
                                    openingTildeDouble = -1;
                                    openingUnicode = 1;
                                    break;
                                } else {
                                    toRevert[i] = true;
                                    openingUnicode = 1;
                                    break;
                                }
                            } else if (!beforeASpace) {
                                openingTildeDouble = i;
                                openingUnicode = 1;
                                break;
                            } else {
                                toRevert[i] = true;
                                openingUnicode = 1;
                                break;
                            }
                        } else {
                            toRevert[openingTildeDouble] = true;
                            toRevert[i] = true;
                            openingTildeDouble = -1;
                            openingUnicode = 1;
                            break;
                        }
                        break;
                    case -4:
                        if (i == 0 || openingAsteriskDouble != i - 1) {
                            if (openingAsteriskDouble >= 0) {
                                if (!afterASpace) {
                                    openingAsteriskDouble = -1;
                                    openingUnicode = 1;
                                    break;
                                } else {
                                    toRevert[i] = true;
                                    openingUnicode = 1;
                                    break;
                                }
                            } else if (!beforeASpace) {
                                openingAsteriskDouble = i;
                                openingUnicode = 1;
                                break;
                            } else {
                                toRevert[i] = true;
                                openingUnicode = 1;
                                break;
                            }
                        } else {
                            toRevert[openingAsteriskDouble] = true;
                            toRevert[i] = true;
                            openingAsteriskDouble = -1;
                            openingUnicode = 1;
                            break;
                        }
                        break;
                    case -3:
                        if (openingAsterisk >= 0) {
                            if (!afterASpace) {
                                openingAsterisk = -1;
                                openingUnicode = 1;
                                break;
                            } else {
                                toRevert[i] = true;
                                openingUnicode = 1;
                                break;
                            }
                        } else if (!beforeASpace) {
                            openingAsterisk = i;
                            openingUnicode = 1;
                            break;
                        } else {
                            toRevert[i] = true;
                            openingUnicode = 1;
                            break;
                        }
                    case -2:
                        if (i == 0 || openingUnderscoreDouble != i - 1) {
                            if (openingUnderscoreDouble >= 0) {
                                if (!afterASpace) {
                                    openingUnderscoreDouble = -1;
                                    openingUnicode = 1;
                                    break;
                                } else {
                                    toRevert[i] = true;
                                    openingUnicode = 1;
                                    break;
                                }
                            } else if (!beforeASpace) {
                                openingUnderscoreDouble = i;
                                openingUnicode = 1;
                                break;
                            } else {
                                toRevert[i] = true;
                                openingUnicode = 1;
                                break;
                            }
                        } else {
                            toRevert[openingUnderscoreDouble] = true;
                            toRevert[i] = true;
                            openingUnderscoreDouble = -1;
                            openingUnicode = 1;
                            break;
                        }
                        break;
                    case -1:
                        if (openingUnderscore >= 0) {
                            if (!afterASpace) {
                                openingUnderscore = -1;
                                openingUnicode = 1;
                                break;
                            } else {
                                toRevert[i] = true;
                                openingUnicode = 1;
                                break;
                            }
                        } else if (!beforeASpace) {
                            openingUnderscore = i;
                            openingUnicode = 1;
                            break;
                        } else {
                            toRevert[i] = true;
                            openingUnicode = 1;
                            break;
                        }
                    default:
                        openingUnicode = 1;
                        break;
                }
            } else {
                boolean z6 = beforeASpace;
                if (i < 1 || intArrayLengthPair.data[i - 1] == 32) {
                    openingUnicode = 1;
                    toDelete[i] = true;
                } else {
                    openingUnicode = 1;
                }
            }
            i += openingUnicode;
            IntArrayLengthPair intArrayLengthPair322 = output;
        }
        if (openingUnderscore >= 0) {
            toRevert[openingUnderscore] = true;
        }
        if (openingUnderscoreDouble >= 0) {
            toRevert[openingUnderscoreDouble] = true;
        }
        if (openingAsterisk >= 0) {
            toRevert[openingAsterisk] = true;
        }
        if (openingAsteriskDouble >= 0) {
            toRevert[openingAsteriskDouble] = true;
        }
        if (openingTildeDouble >= 0) {
            toRevert[openingTildeDouble] = true;
        }
        if (lastBracketSquareOpen >= 0) {
            toRevert[lastBracketSquareOpen] = true;
        }
        int j11 = intArrayLengthPair.pos - 1;
        while (j11 >= 0 && intArrayLengthPair.data[j11] == 32) {
            toDelete[j11] = true;
            j11--;
        }
        output.clear();
        for (int i3 = 0; i3 < intArrayLengthPair.pos; i3++) {
            if (toDelete[i3]) {
                IntArrayLengthPair intArrayLengthPair5 = output;
            } else if (toRevert[i3]) {
                output.append(reverseLookup[intArrayLengthPair.data[i3] + 20]);
            } else {
                IntArrayLengthPair intArrayLengthPair6 = output;
                int[] iArr = intArrayLengthPair6.data;
                int i4 = intArrayLengthPair6.pos;
                intArrayLengthPair6.pos = i4 + 1;
                iArr[i4] = intArrayLengthPair.data[i3];
            }
        }
        IntArrayLengthPair intArrayLengthPair7 = output;
    }

    private static int findParenClosePos(IntArrayLengthPair tokens, int startPos) {
        int i = startPos;
        while (i < tokens.pos) {
            int i2 = tokens.data[i];
            if (i2 == -11) {
                return i;
            }
            if (i2 == 34) {
                i = indexOfIgnoreEscaped(tokens, 34, i + 1);
                if (i < 0) {
                    return -1;
                }
            }
            i++;
        }
        return -1;
    }

    private static int indexOfIgnoreEscaped(IntArrayLengthPair haystack, int needle, int startPos) {
        int i = startPos;
        while (i < haystack.pos) {
            if (haystack.data[i] == 92) {
                i++;
            } else if (haystack.data[i] == needle) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public static void naiveTokenize(IntArrayLengthPair input, IntArrayLengthPair output) {
        output.clear();
        int i = 0;
        while (i < input.pos) {
            int c = input.data[i];
            if (c == 38) {
                int[] iArr = output.data;
                int i2 = output.pos;
                output.pos = i2 + 1;
                iArr[i2] = -12;
            } else if (c == 59) {
                int[] iArr2 = output.data;
                int i3 = output.pos;
                output.pos = i3 + 1;
                iArr2[i3] = -13;
            } else if (c != 126) {
                switch (c) {
                    case 9:
                    case 10:
                        int[] iArr3 = output.data;
                        int i4 = output.pos;
                        output.pos = i4 + 1;
                        iArr3[i4] = 32;
                        break;
                    default:
                        switch (c) {
                            case 12:
                            case 13:
                                break;
                            default:
                                switch (c) {
                                    case 40:
                                        int[] iArr4 = output.data;
                                        int i5 = output.pos;
                                        output.pos = i5 + 1;
                                        iArr4[i5] = -10;
                                        break;
                                    case 41:
                                        int[] iArr5 = output.data;
                                        int i6 = output.pos;
                                        output.pos = i6 + 1;
                                        iArr5[i6] = -11;
                                        break;
                                    case 42:
                                        if (i < input.pos - 1 && input.data[i + 1] == 42) {
                                            i++;
                                            int[] iArr6 = output.data;
                                            int i7 = output.pos;
                                            output.pos = i7 + 1;
                                            iArr6[i7] = -4;
                                            break;
                                        } else {
                                            int[] iArr7 = output.data;
                                            int i8 = output.pos;
                                            output.pos = i8 + 1;
                                            iArr7[i8] = -3;
                                            break;
                                        }
                                    default:
                                        switch (c) {
                                            case 91:
                                                int[] iArr8 = output.data;
                                                int i9 = output.pos;
                                                output.pos = i9 + 1;
                                                iArr8[i9] = -8;
                                                break;
                                            case 92:
                                                if (i >= input.pos - 1) {
                                                    int[] iArr9 = output.data;
                                                    int i10 = output.pos;
                                                    output.pos = i10 + 1;
                                                    iArr9[i10] = 92;
                                                    break;
                                                } else {
                                                    int[] iArr10 = output.data;
                                                    int i11 = output.pos;
                                                    output.pos = i11 + 1;
                                                    i++;
                                                    iArr10[i11] = input.data[i];
                                                    break;
                                                }
                                            case 93:
                                                int[] iArr11 = output.data;
                                                int i12 = output.pos;
                                                output.pos = i12 + 1;
                                                iArr11[i12] = -9;
                                                break;
                                            case 94:
                                                int[] iArr12 = output.data;
                                                int i13 = output.pos;
                                                output.pos = i13 + 1;
                                                iArr12[i13] = -6;
                                                break;
                                            case 95:
                                                if (i >= input.pos - 1 || input.data[i + 1] != 95) {
                                                    if ((i < input.pos - 1 && input.data[i + 1] == 32) || ((i > 0 && input.data[i - 1] == 32) || i == 0 || i == input.pos - 1)) {
                                                        int[] iArr13 = output.data;
                                                        int i14 = output.pos;
                                                        output.pos = i14 + 1;
                                                        iArr13[i14] = -1;
                                                        break;
                                                    } else {
                                                        int[] iArr14 = output.data;
                                                        int i15 = output.pos;
                                                        output.pos = i15 + 1;
                                                        iArr14[i15] = c;
                                                        break;
                                                    }
                                                } else {
                                                    i++;
                                                    int[] iArr15 = output.data;
                                                    int i16 = output.pos;
                                                    output.pos = i16 + 1;
                                                    iArr15[i16] = -2;
                                                    break;
                                                }
                                                break;
                                            case 96:
                                                int[] iArr16 = output.data;
                                                int i17 = output.pos;
                                                output.pos = i17 + 1;
                                                iArr16[i17] = -7;
                                                break;
                                            default:
                                                int[] iArr17 = output.data;
                                                int i18 = output.pos;
                                                output.pos = i18 + 1;
                                                iArr17[i18] = c;
                                                continue;
                                        }
                                }
                        }
                        int[] iArr32 = output.data;
                        int i42 = output.pos;
                        output.pos = i42 + 1;
                        iArr32[i42] = 32;
                        break;
                }
            } else if (i >= input.pos - 1 || input.data[i + 1] != 126) {
                int[] iArr18 = output.data;
                int i19 = output.pos;
                output.pos = i19 + 1;
                iArr18[i19] = 126;
            } else {
                i++;
                int[] iArr19 = output.data;
                int i20 = output.pos;
                output.pos = i20 + 1;
                iArr19[i20] = -5;
            }
            i++;
        }
    }

    private static int indexOf(int[] haystack, int needle, int startInclusive, int endExclusive) {
        for (int i = startInclusive; i < endExclusive; i++) {
            if (haystack[i] == needle) {
                return i;
            }
        }
        return -1;
    }

    private static int reverseIndexOf(int[] haystack, int needle, int startInclusive) {
        for (int i = startInclusive; i >= 0; i--) {
            if (haystack[i] == needle) {
                return i;
            }
        }
        return -1;
    }

    public static int findCloseWellBracketed(int[] haystack, int openBracket, int closeBracket, int startInclusive, int endExclusive) {
        if (haystack[startInclusive] == openBracket) {
            int b = 1;
            for (int i = startInclusive + 1; i < endExclusive; i++) {
                if (haystack[i] == openBracket) {
                    b++;
                } else if (haystack[i] == closeBracket) {
                    b--;
                }
                if (b == 0) {
                    return i;
                }
            }
            return -1;
        }
        throw new RuntimeException("Internal markdown parser error");
    }

    private static boolean isSpaces(int[] haystack, int startInclusive, int endExclusive) {
        for (int i = startInclusive; i < endExclusive; i++) {
            if (haystack[i] != 32) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDigits(int[] haystack, int startInclusive, int endExclusive) {
        for (int i = startInclusive; i < endExclusive; i++) {
            if (haystack[i] < 48 || haystack[i] > 57) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexDigits(int[] haystack, int startInclusive, int endExclusive) {
        for (int i = startInclusive; i < endExclusive; i++) {
            int c = haystack[i];
            if ((c < 48 || c > 57) && ((c < 97 || c > 102) && (c < 65 || c > 70))) {
                return false;
            }
        }
        return true;
    }

    private static int getDecimal(int[] chars, int startInclusive, int endExclusive) {
        int result = 0;
        for (int i = startInclusive; i < endExclusive; i++) {
            result = (result * 10) + (chars[i] - 48);
        }
        return result;
    }

    private static int fromHex(int ch) {
        if (ch >= 48 && ch <= 57) {
            return ch - 48;
        }
        if (ch < 97 || ch > 102) {
            return (ch + 10) - 65;
        }
        return (ch + 10) - 97;
    }

    private static int getHex(int[] chars, int startInclusive, int endExclusive) {
        int result = 0;
        for (int i = startInclusive; i < endExclusive; i++) {
            result = (result * 16) + fromHex(chars[i]);
        }
        return result;
    }

    private static boolean equals(int[] haystack, char[] needle, int startInclusive) {
        for (int i = 0; i < needle.length; i++) {
            if (haystack[startInclusive + i] != needle[i]) {
                return false;
            }
        }
        return true;
    }

    private static int getLinkStartType(int[] haystack, int startInclusive, int endExclusive) {
        int maxLen = endExclusive - startInclusive;
        int type = 0;
        while (true) {
            char[][] cArr = linkPrefixes;
            if (type >= cArr.length) {
                return -1;
            }
            if (cArr[type].length <= maxLen && equals(haystack, cArr[type], startInclusive)) {
                return type;
            }
            type++;
        }
    }

    private static int getRedditLinkStartType(int[] haystack, int startInclusive, int endExclusive) {
        int maxLen = endExclusive - startInclusive;
        int type = 0;
        while (true) {
            char[][] cArr = linkPrefixes_reddit;
            if (type >= cArr.length) {
                return -1;
            }
            if (cArr[type].length <= maxLen && equals(haystack, cArr[type], startInclusive)) {
                return type;
            }
            type++;
        }
    }

    /* JADX WARNING: type inference failed for: r5v2, types: [char[]] */
    /* JADX WARNING: Multi-variable type inference failed */
    private static int[] revert(int[] tokens, int startInclusive, int endExclusive) {
        int outputLen = 0;
        for (int i = startInclusive; i < endExclusive; i++) {
            int token = tokens[i];
            if (token < 0) {
                outputLen += reverseLookup[token + 20].length;
            } else {
                outputLen++;
            }
        }
        int[] result = new int[outputLen];
        int resultPos = 0;
        for (int i2 = startInclusive; i2 < endExclusive; i2++) {
            int token2 = tokens[i2];
            if (token2 < 0) {
                ?[OBJECT, ARRAY] AOBJECT__ARRAY = reverseLookup[token2 + 20];
                int length = AOBJECT__ARRAY.length;
                int i3 = 0;
                while (i3 < length) {
                    int resultPos2 = resultPos + 1;
                    result[resultPos] = AOBJECT__ARRAY[i3];
                    i3++;
                    resultPos = resultPos2;
                }
            } else {
                int resultPos3 = resultPos + 1;
                result[resultPos] = token2;
                resultPos = resultPos3;
            }
        }
        return result;
    }
}
