package org.apache.commons.lang3;

import android.support.v4.internal.view.SupportMenu;
import java.util.UUID;

public class Conversion {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final boolean[] FFFF = {false, false, false, false};
    private static final boolean[] FFFT = {false, false, false, true};
    private static final boolean[] FFTF = {false, false, true, false};
    private static final boolean[] FFTT = {false, false, true, true};
    private static final boolean[] FTFF = {false, true, false, false};
    private static final boolean[] FTFT = {false, true, false, true};
    private static final boolean[] FTTF = {false, true, true, false};
    private static final boolean[] FTTT = {false, true, true, true};
    private static final boolean[] TFFF = {true, false, false, false};
    private static final boolean[] TFFT = {true, false, false, true};
    private static final boolean[] TFTF = {true, false, true, false};
    private static final boolean[] TFTT = {true, false, true, true};
    private static final boolean[] TTFF = {true, true, false, false};
    private static final boolean[] TTFT = {true, true, false, true};
    private static final boolean[] TTTF = {true, true, true, false};
    private static final boolean[] TTTT = {true, true, true, true};

    public static int hexDigitToInt(char hexDigit) {
        int digit = Character.digit(hexDigit, 16);
        if (digit >= 0) {
            return digit;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Cannot interpret '");
        sb.append(hexDigit);
        sb.append("' as a hexadecimal digit");
        throw new IllegalArgumentException(sb.toString());
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x002c, code lost:
        return 11;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x002e, code lost:
        return 3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0031, code lost:
        return 13;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0033, code lost:
        return 5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:6:0x0027, code lost:
        return 15;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x0029, code lost:
        return 7;
     */
    public static int hexDigitMsb0ToInt(char hexDigit) {
        switch (hexDigit) {
            case '0':
                return 0;
            case '1':
                return 8;
            case '2':
                return 4;
            case '3':
                return 12;
            case '4':
                return 2;
            case '5':
                return 10;
            case '6':
                return 6;
            case '7':
                return 14;
            case '8':
                return 1;
            case '9':
                return 9;
            default:
                switch (hexDigit) {
                    case 'A':
                        break;
                    case 'B':
                        break;
                    case 'C':
                        break;
                    case 'D':
                        break;
                    case 'E':
                        break;
                    case 'F':
                        break;
                    default:
                        switch (hexDigit) {
                            case 'a':
                                break;
                            case 'b':
                                break;
                            case 'c':
                                break;
                            case 'd':
                                break;
                            case 'e':
                                break;
                            case 'f':
                                break;
                            default:
                                StringBuilder sb = new StringBuilder();
                                sb.append("Cannot interpret '");
                                sb.append(hexDigit);
                                sb.append("' as a hexadecimal digit");
                                throw new IllegalArgumentException(sb.toString());
                        }
                }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x003f, code lost:
        return (boolean[]) TFTT.clone();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0048, code lost:
        return (boolean[]) FFTT.clone();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0051, code lost:
        return (boolean[]) TTFT.clone();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x005a, code lost:
        return (boolean[]) FTFT.clone();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:6:0x002d, code lost:
        return (boolean[]) TTTT.clone();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x0036, code lost:
        return (boolean[]) FTTT.clone();
     */
    public static boolean[] hexDigitToBinary(char hexDigit) {
        switch (hexDigit) {
            case '0':
                return (boolean[]) FFFF.clone();
            case '1':
                return (boolean[]) TFFF.clone();
            case '2':
                return (boolean[]) FTFF.clone();
            case '3':
                return (boolean[]) TTFF.clone();
            case '4':
                return (boolean[]) FFTF.clone();
            case '5':
                return (boolean[]) TFTF.clone();
            case '6':
                return (boolean[]) FTTF.clone();
            case '7':
                return (boolean[]) TTTF.clone();
            case '8':
                return (boolean[]) FFFT.clone();
            case '9':
                return (boolean[]) TFFT.clone();
            default:
                switch (hexDigit) {
                    case 'A':
                        break;
                    case 'B':
                        break;
                    case 'C':
                        break;
                    case 'D':
                        break;
                    case 'E':
                        break;
                    case 'F':
                        break;
                    default:
                        switch (hexDigit) {
                            case 'a':
                                break;
                            case 'b':
                                break;
                            case 'c':
                                break;
                            case 'd':
                                break;
                            case 'e':
                                break;
                            case 'f':
                                break;
                            default:
                                StringBuilder sb = new StringBuilder();
                                sb.append("Cannot interpret '");
                                sb.append(hexDigit);
                                sb.append("' as a hexadecimal digit");
                                throw new IllegalArgumentException(sb.toString());
                        }
                }
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:10:0x003f, code lost:
        return (boolean[]) TTFT.clone();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0048, code lost:
        return (boolean[]) TTFF.clone();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0051, code lost:
        return (boolean[]) TFTT.clone();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x005a, code lost:
        return (boolean[]) TFTF.clone();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:6:0x002d, code lost:
        return (boolean[]) TTTT.clone();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x0036, code lost:
        return (boolean[]) TTTF.clone();
     */
    public static boolean[] hexDigitMsb0ToBinary(char hexDigit) {
        switch (hexDigit) {
            case '0':
                return (boolean[]) FFFF.clone();
            case '1':
                return (boolean[]) FFFT.clone();
            case '2':
                return (boolean[]) FFTF.clone();
            case '3':
                return (boolean[]) FFTT.clone();
            case '4':
                return (boolean[]) FTFF.clone();
            case '5':
                return (boolean[]) FTFT.clone();
            case '6':
                return (boolean[]) FTTF.clone();
            case '7':
                return (boolean[]) FTTT.clone();
            case '8':
                return (boolean[]) TFFF.clone();
            case '9':
                return (boolean[]) TFFT.clone();
            default:
                switch (hexDigit) {
                    case 'A':
                        break;
                    case 'B':
                        break;
                    case 'C':
                        break;
                    case 'D':
                        break;
                    case 'E':
                        break;
                    case 'F':
                        break;
                    default:
                        switch (hexDigit) {
                            case 'a':
                                break;
                            case 'b':
                                break;
                            case 'c':
                                break;
                            case 'd':
                                break;
                            case 'e':
                                break;
                            case 'f':
                                break;
                            default:
                                StringBuilder sb = new StringBuilder();
                                sb.append("Cannot interpret '");
                                sb.append(hexDigit);
                                sb.append("' as a hexadecimal digit");
                                throw new IllegalArgumentException(sb.toString());
                        }
                }
        }
    }

    public static char binaryToHexDigit(boolean[] src) {
        return binaryToHexDigit(src, 0);
    }

    public static char binaryToHexDigit(boolean[] src, int srcPos) {
        if (src.length == 0) {
            throw new IllegalArgumentException("Cannot convert an empty array.");
        } else if (src.length <= srcPos + 3 || !src[srcPos + 3]) {
            if (src.length <= srcPos + 2 || !src[srcPos + 2]) {
                if (src.length <= srcPos + 1 || !src[srcPos + 1]) {
                    return src[srcPos] ? '1' : '0';
                }
                return src[srcPos] ? '3' : '2';
            } else if (src.length <= srcPos + 1 || !src[srcPos + 1]) {
                return src[srcPos] ? '5' : '4';
            } else {
                return src[srcPos] ? '7' : '6';
            }
        } else if (src.length <= srcPos + 2 || !src[srcPos + 2]) {
            if (src.length <= srcPos + 1 || !src[srcPos + 1]) {
                return src[srcPos] ? '9' : '8';
            }
            return src[srcPos] ? 'b' : 'a';
        } else if (src.length <= srcPos + 1 || !src[srcPos + 1]) {
            return src[srcPos] ? 'd' : 'c';
        } else {
            return src[srcPos] ? 'f' : 'e';
        }
    }

    public static char binaryToHexDigitMsb0_4bits(boolean[] src) {
        return binaryToHexDigitMsb0_4bits(src, 0);
    }

    public static char binaryToHexDigitMsb0_4bits(boolean[] src, int srcPos) {
        if (src.length > 8) {
            StringBuilder sb = new StringBuilder();
            sb.append("src.length>8: src.length=");
            sb.append(src.length);
            throw new IllegalArgumentException(sb.toString());
        } else if (src.length - srcPos < 4) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("src.length-srcPos<4: src.length=");
            sb2.append(src.length);
            sb2.append(", srcPos=");
            sb2.append(srcPos);
            throw new IllegalArgumentException(sb2.toString());
        } else if (src[srcPos + 3]) {
            if (src[srcPos + 2]) {
                if (src[srcPos + 1]) {
                    return src[srcPos] ? 'f' : '7';
                }
                return src[srcPos] ? 'b' : '3';
            } else if (src[srcPos + 1]) {
                return src[srcPos] ? 'd' : '5';
            } else {
                return src[srcPos] ? '9' : '1';
            }
        } else if (src[srcPos + 2]) {
            if (src[srcPos + 1]) {
                return src[srcPos] ? 'e' : '6';
            }
            return src[srcPos] ? 'a' : '2';
        } else if (src[srcPos + 1]) {
            return src[srcPos] ? 'c' : '4';
        } else {
            return src[srcPos] ? '8' : '0';
        }
    }

    public static char binaryBeMsb0ToHexDigit(boolean[] src) {
        return binaryBeMsb0ToHexDigit(src, 0);
    }

    public static char binaryBeMsb0ToHexDigit(boolean[] src, int srcPos) {
        if (src.length != 0) {
            int beSrcPos = (src.length - 1) - srcPos;
            int srcLen = Math.min(4, beSrcPos + 1);
            boolean[] paddedSrc = new boolean[4];
            System.arraycopy(src, (beSrcPos + 1) - srcLen, paddedSrc, 4 - srcLen, srcLen);
            boolean[] src2 = paddedSrc;
            if (src2[0]) {
                if (src2.length <= 0 + 1 || !src2[0 + 1]) {
                    if (src2.length <= 0 + 2 || !src2[0 + 2]) {
                        return (src2.length <= 0 + 3 || !src2[0 + 3]) ? '8' : '9';
                    }
                    return (src2.length <= 0 + 3 || !src2[0 + 3]) ? 'a' : 'b';
                } else if (src2.length <= 0 + 2 || !src2[0 + 2]) {
                    return (src2.length <= 0 + 3 || !src2[0 + 3]) ? 'c' : 'd';
                } else {
                    return (src2.length <= 0 + 3 || !src2[0 + 3]) ? 'e' : 'f';
                }
            } else if (src2.length <= 0 + 1 || !src2[0 + 1]) {
                if (src2.length <= 0 + 2 || !src2[0 + 2]) {
                    return (src2.length <= 0 + 3 || !src2[0 + 3]) ? '0' : '1';
                }
                return (src2.length <= 0 + 3 || !src2[0 + 3]) ? '2' : '3';
            } else if (src2.length <= 0 + 2 || !src2[0 + 2]) {
                return (src2.length <= 0 + 3 || !src2[0 + 3]) ? '4' : '5';
            } else {
                return (src2.length <= 0 + 3 || !src2[0 + 3]) ? '6' : '7';
            }
        } else {
            throw new IllegalArgumentException("Cannot convert an empty array.");
        }
    }

    public static char intToHexDigit(int nibble) {
        char c = Character.forDigit(nibble, 16);
        if (c != 0) {
            return c;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("nibble value not between 0 and 15: ");
        sb.append(nibble);
        throw new IllegalArgumentException(sb.toString());
    }

    public static char intToHexDigitMsb0(int nibble) {
        switch (nibble) {
            case 0:
                return '0';
            case 1:
                return '8';
            case 2:
                return '4';
            case 3:
                return 'c';
            case 4:
                return '2';
            case 5:
                return 'a';
            case 6:
                return '6';
            case 7:
                return 'e';
            case 8:
                return '1';
            case 9:
                return '9';
            case 10:
                return '5';
            case 11:
                return 'd';
            case 12:
                return '3';
            case 13:
                return 'b';
            case 14:
                return '7';
            case 15:
                return 'f';
            default:
                StringBuilder sb = new StringBuilder();
                sb.append("nibble value not between 0 and 15: ");
                sb.append(nibble);
                throw new IllegalArgumentException(sb.toString());
        }
    }

    public static long intArrayToLong(int[] src, int srcPos, long dstInit, int dstPos, int nInts) {
        if ((src.length == 0 && srcPos == 0) || nInts == 0) {
            return dstInit;
        }
        if (((nInts - 1) * 32) + dstPos < 64) {
            long out = dstInit;
            for (int i = 0; i < nInts; i++) {
                int shift = (i * 32) + dstPos;
                out = ((-1 ^ (4294967295 << shift)) & out) | ((((long) src[i + srcPos]) & 4294967295L) << shift);
            }
            return out;
        }
        throw new IllegalArgumentException("(nInts-1)*32+dstPos is greater or equal to than 64");
    }

    public static long shortArrayToLong(short[] src, int srcPos, long dstInit, int dstPos, int nShorts) {
        if ((src.length == 0 && srcPos == 0) || nShorts == 0) {
            return dstInit;
        }
        if (((nShorts - 1) * 16) + dstPos < 64) {
            long out = dstInit;
            for (int i = 0; i < nShorts; i++) {
                int shift = (i * 16) + dstPos;
                out = ((-1 ^ (65535 << shift)) & out) | ((((long) src[i + srcPos]) & 65535) << shift);
            }
            return out;
        }
        throw new IllegalArgumentException("(nShorts-1)*16+dstPos is greater or equal to than 64");
    }

    public static int shortArrayToInt(short[] src, int srcPos, int dstInit, int dstPos, int nShorts) {
        if ((src.length == 0 && srcPos == 0) || nShorts == 0) {
            return dstInit;
        }
        if (((nShorts - 1) * 16) + dstPos < 32) {
            int out = dstInit;
            for (int i = 0; i < nShorts; i++) {
                int shift = (i * 16) + dstPos;
                out = (((SupportMenu.USER_MASK << shift) ^ -1) & out) | ((src[i + srcPos] & 65535) << shift);
            }
            return out;
        }
        throw new IllegalArgumentException("(nShorts-1)*16+dstPos is greater or equal to than 32");
    }

    public static long byteArrayToLong(byte[] src, int srcPos, long dstInit, int dstPos, int nBytes) {
        if ((src.length == 0 && srcPos == 0) || nBytes == 0) {
            return dstInit;
        }
        if (((nBytes - 1) * 8) + dstPos < 64) {
            long out = dstInit;
            for (int i = 0; i < nBytes; i++) {
                int shift = (i * 8) + dstPos;
                out = ((-1 ^ (255 << shift)) & out) | ((((long) src[i + srcPos]) & 255) << shift);
            }
            return out;
        }
        throw new IllegalArgumentException("(nBytes-1)*8+dstPos is greater or equal to than 64");
    }

    public static int byteArrayToInt(byte[] src, int srcPos, int dstInit, int dstPos, int nBytes) {
        if ((src.length == 0 && srcPos == 0) || nBytes == 0) {
            return dstInit;
        }
        if (((nBytes - 1) * 8) + dstPos < 32) {
            int out = dstInit;
            for (int i = 0; i < nBytes; i++) {
                int shift = (i * 8) + dstPos;
                out = (((255 << shift) ^ -1) & out) | ((src[i + srcPos] & 255) << shift);
            }
            return out;
        }
        throw new IllegalArgumentException("(nBytes-1)*8+dstPos is greater or equal to than 32");
    }

    public static short byteArrayToShort(byte[] src, int srcPos, short dstInit, int dstPos, int nBytes) {
        if ((src.length == 0 && srcPos == 0) || nBytes == 0) {
            return dstInit;
        }
        if (((nBytes - 1) * 8) + dstPos < 16) {
            short out = dstInit;
            for (int i = 0; i < nBytes; i++) {
                int shift = (i * 8) + dstPos;
                out = (short) ((((255 << shift) ^ -1) & out) | ((src[i + srcPos] & 255) << shift));
            }
            return out;
        }
        throw new IllegalArgumentException("(nBytes-1)*8+dstPos is greater or equal to than 16");
    }

    public static long hexToLong(String src, int srcPos, long dstInit, int dstPos, int nHex) {
        if (nHex == 0) {
            return dstInit;
        }
        if (((nHex - 1) * 4) + dstPos < 64) {
            long out = dstInit;
            for (int i = 0; i < nHex; i++) {
                int shift = (i * 4) + dstPos;
                out = ((-1 ^ (15 << shift)) & out) | ((((long) hexDigitToInt(src.charAt(i + srcPos))) & 15) << shift);
            }
            return out;
        }
        throw new IllegalArgumentException("(nHexs-1)*4+dstPos is greater or equal to than 64");
    }

    public static int hexToInt(String src, int srcPos, int dstInit, int dstPos, int nHex) {
        if (nHex == 0) {
            return dstInit;
        }
        if (((nHex - 1) * 4) + dstPos < 32) {
            int out = dstInit;
            for (int i = 0; i < nHex; i++) {
                int shift = (i * 4) + dstPos;
                out = (((15 << shift) ^ -1) & out) | ((hexDigitToInt(src.charAt(i + srcPos)) & 15) << shift);
            }
            return out;
        }
        throw new IllegalArgumentException("(nHexs-1)*4+dstPos is greater or equal to than 32");
    }

    public static short hexToShort(String src, int srcPos, short dstInit, int dstPos, int nHex) {
        if (nHex == 0) {
            return dstInit;
        }
        if (((nHex - 1) * 4) + dstPos < 16) {
            short out = dstInit;
            for (int i = 0; i < nHex; i++) {
                int shift = (i * 4) + dstPos;
                out = (short) ((((15 << shift) ^ -1) & out) | ((hexDigitToInt(src.charAt(i + srcPos)) & 15) << shift));
            }
            return out;
        }
        throw new IllegalArgumentException("(nHexs-1)*4+dstPos is greater or equal to than 16");
    }

    public static byte hexToByte(String src, int srcPos, byte dstInit, int dstPos, int nHex) {
        if (nHex == 0) {
            return dstInit;
        }
        if (((nHex - 1) * 4) + dstPos < 8) {
            byte out = dstInit;
            for (int i = 0; i < nHex; i++) {
                int shift = (i * 4) + dstPos;
                out = (byte) ((((15 << shift) ^ -1) & out) | ((hexDigitToInt(src.charAt(i + srcPos)) & 15) << shift));
            }
            return out;
        }
        throw new IllegalArgumentException("(nHexs-1)*4+dstPos is greater or equal to than 8");
    }

    public static long binaryToLong(boolean[] src, int srcPos, long dstInit, int dstPos, int nBools) {
        boolean[] zArr = src;
        int i = nBools;
        if ((zArr.length == 0 && srcPos == 0) || i == 0) {
            return dstInit;
        }
        if ((i - 1) + dstPos < 64) {
            long out = dstInit;
            for (int i2 = 0; i2 < i; i2++) {
                int shift = i2 + dstPos;
                out = ((-1 ^ (1 << shift)) & out) | ((zArr[i2 + srcPos] ? 1 : 0) << shift);
            }
            return out;
        }
        throw new IllegalArgumentException("nBools-1+dstPos is greater or equal to than 64");
    }

    public static int binaryToInt(boolean[] src, int srcPos, int dstInit, int dstPos, int nBools) {
        if ((src.length == 0 && srcPos == 0) || nBools == 0) {
            return dstInit;
        }
        if ((nBools - 1) + dstPos < 32) {
            int out = dstInit;
            for (int i = 0; i < nBools; i++) {
                int shift = i + dstPos;
                out = (((1 << shift) ^ -1) & out) | ((src[i + srcPos] ? 1 : 0) << shift);
            }
            return out;
        }
        throw new IllegalArgumentException("nBools-1+dstPos is greater or equal to than 32");
    }

    public static short binaryToShort(boolean[] src, int srcPos, short dstInit, int dstPos, int nBools) {
        if ((src.length == 0 && srcPos == 0) || nBools == 0) {
            return dstInit;
        }
        if ((nBools - 1) + dstPos < 16) {
            short out = dstInit;
            for (int i = 0; i < nBools; i++) {
                int shift = i + dstPos;
                out = (short) ((((1 << shift) ^ -1) & out) | ((src[i + srcPos] ? 1 : 0) << shift));
            }
            return out;
        }
        throw new IllegalArgumentException("nBools-1+dstPos is greater or equal to than 16");
    }

    public static byte binaryToByte(boolean[] src, int srcPos, byte dstInit, int dstPos, int nBools) {
        if ((src.length == 0 && srcPos == 0) || nBools == 0) {
            return dstInit;
        }
        if ((nBools - 1) + dstPos < 8) {
            byte out = dstInit;
            for (int i = 0; i < nBools; i++) {
                int shift = i + dstPos;
                out = (byte) ((((1 << shift) ^ -1) & out) | ((src[i + srcPos] ? 1 : 0) << shift));
            }
            return out;
        }
        throw new IllegalArgumentException("nBools-1+dstPos is greater or equal to than 8");
    }

    public static int[] longToIntArray(long src, int srcPos, int[] dst, int dstPos, int nInts) {
        if (nInts == 0) {
            return dst;
        }
        if (((nInts - 1) * 32) + srcPos < 64) {
            for (int i = 0; i < nInts; i++) {
                dst[dstPos + i] = (int) (-1 & (src >> ((i * 32) + srcPos)));
            }
            return dst;
        }
        throw new IllegalArgumentException("(nInts-1)*32+srcPos is greater or equal to than 64");
    }

    public static short[] longToShortArray(long src, int srcPos, short[] dst, int dstPos, int nShorts) {
        if (nShorts == 0) {
            return dst;
        }
        if (((nShorts - 1) * 16) + srcPos < 64) {
            for (int i = 0; i < nShorts; i++) {
                dst[dstPos + i] = (short) ((int) (65535 & (src >> ((i * 16) + srcPos))));
            }
            return dst;
        }
        throw new IllegalArgumentException("(nShorts-1)*16+srcPos is greater or equal to than 64");
    }

    public static short[] intToShortArray(int src, int srcPos, short[] dst, int dstPos, int nShorts) {
        if (nShorts == 0) {
            return dst;
        }
        if (((nShorts - 1) * 16) + srcPos < 32) {
            for (int i = 0; i < nShorts; i++) {
                dst[dstPos + i] = (short) (65535 & (src >> ((i * 16) + srcPos)));
            }
            return dst;
        }
        throw new IllegalArgumentException("(nShorts-1)*16+srcPos is greater or equal to than 32");
    }

    public static byte[] longToByteArray(long src, int srcPos, byte[] dst, int dstPos, int nBytes) {
        if (nBytes == 0) {
            return dst;
        }
        if (((nBytes - 1) * 8) + srcPos < 64) {
            for (int i = 0; i < nBytes; i++) {
                dst[dstPos + i] = (byte) ((int) (255 & (src >> ((i * 8) + srcPos))));
            }
            return dst;
        }
        throw new IllegalArgumentException("(nBytes-1)*8+srcPos is greater or equal to than 64");
    }

    public static byte[] intToByteArray(int src, int srcPos, byte[] dst, int dstPos, int nBytes) {
        if (nBytes == 0) {
            return dst;
        }
        if (((nBytes - 1) * 8) + srcPos < 32) {
            for (int i = 0; i < nBytes; i++) {
                dst[dstPos + i] = (byte) ((src >> ((i * 8) + srcPos)) & 255);
            }
            return dst;
        }
        throw new IllegalArgumentException("(nBytes-1)*8+srcPos is greater or equal to than 32");
    }

    public static byte[] shortToByteArray(short src, int srcPos, byte[] dst, int dstPos, int nBytes) {
        if (nBytes == 0) {
            return dst;
        }
        if (((nBytes - 1) * 8) + srcPos < 16) {
            for (int i = 0; i < nBytes; i++) {
                dst[dstPos + i] = (byte) ((src >> ((i * 8) + srcPos)) & 255);
            }
            return dst;
        }
        throw new IllegalArgumentException("(nBytes-1)*8+srcPos is greater or equal to than 16");
    }

    public static String longToHex(long src, int srcPos, String dstInit, int dstPos, int nHexs) {
        if (nHexs == 0) {
            return dstInit;
        }
        if (((nHexs - 1) * 4) + srcPos < 64) {
            StringBuilder sb = new StringBuilder(dstInit);
            int append = sb.length();
            for (int i = 0; i < nHexs; i++) {
                int bits = (int) (15 & (src >> ((i * 4) + srcPos)));
                if (dstPos + i == append) {
                    append++;
                    sb.append(intToHexDigit(bits));
                } else {
                    sb.setCharAt(dstPos + i, intToHexDigit(bits));
                }
            }
            return sb.toString();
        }
        throw new IllegalArgumentException("(nHexs-1)*4+srcPos is greater or equal to than 64");
    }

    public static String intToHex(int src, int srcPos, String dstInit, int dstPos, int nHexs) {
        if (nHexs == 0) {
            return dstInit;
        }
        if (((nHexs - 1) * 4) + srcPos < 32) {
            StringBuilder sb = new StringBuilder(dstInit);
            int append = sb.length();
            for (int i = 0; i < nHexs; i++) {
                int bits = (src >> ((i * 4) + srcPos)) & 15;
                if (dstPos + i == append) {
                    append++;
                    sb.append(intToHexDigit(bits));
                } else {
                    sb.setCharAt(dstPos + i, intToHexDigit(bits));
                }
            }
            return sb.toString();
        }
        throw new IllegalArgumentException("(nHexs-1)*4+srcPos is greater or equal to than 32");
    }

    public static String shortToHex(short src, int srcPos, String dstInit, int dstPos, int nHexs) {
        if (nHexs == 0) {
            return dstInit;
        }
        if (((nHexs - 1) * 4) + srcPos < 16) {
            StringBuilder sb = new StringBuilder(dstInit);
            int append = sb.length();
            for (int i = 0; i < nHexs; i++) {
                int bits = (src >> ((i * 4) + srcPos)) & 15;
                if (dstPos + i == append) {
                    append++;
                    sb.append(intToHexDigit(bits));
                } else {
                    sb.setCharAt(dstPos + i, intToHexDigit(bits));
                }
            }
            return sb.toString();
        }
        throw new IllegalArgumentException("(nHexs-1)*4+srcPos is greater or equal to than 16");
    }

    public static String byteToHex(byte src, int srcPos, String dstInit, int dstPos, int nHexs) {
        if (nHexs == 0) {
            return dstInit;
        }
        if (((nHexs - 1) * 4) + srcPos < 8) {
            StringBuilder sb = new StringBuilder(dstInit);
            int append = sb.length();
            for (int i = 0; i < nHexs; i++) {
                int bits = (src >> ((i * 4) + srcPos)) & 15;
                if (dstPos + i == append) {
                    append++;
                    sb.append(intToHexDigit(bits));
                } else {
                    sb.setCharAt(dstPos + i, intToHexDigit(bits));
                }
            }
            return sb.toString();
        }
        throw new IllegalArgumentException("(nHexs-1)*4+srcPos is greater or equal to than 8");
    }

    public static boolean[] longToBinary(long src, int srcPos, boolean[] dst, int dstPos, int nBools) {
        if (nBools == 0) {
            return dst;
        }
        if ((nBools - 1) + srcPos < 64) {
            for (int i = 0; i < nBools; i++) {
                dst[dstPos + i] = (1 & (src >> (i + srcPos))) != 0;
            }
            return dst;
        }
        throw new IllegalArgumentException("nBools-1+srcPos is greater or equal to than 64");
    }

    public static boolean[] intToBinary(int src, int srcPos, boolean[] dst, int dstPos, int nBools) {
        if (nBools == 0) {
            return dst;
        }
        if ((nBools - 1) + srcPos < 32) {
            for (int i = 0; i < nBools; i++) {
                int i2 = dstPos + i;
                boolean z = true;
                if (((src >> (i + srcPos)) & 1) == 0) {
                    z = false;
                }
                dst[i2] = z;
            }
            return dst;
        }
        throw new IllegalArgumentException("nBools-1+srcPos is greater or equal to than 32");
    }

    public static boolean[] shortToBinary(short src, int srcPos, boolean[] dst, int dstPos, int nBools) {
        if (nBools == 0) {
            return dst;
        }
        if ((nBools - 1) + srcPos < 16) {
            for (int i = 0; i < nBools; i++) {
                int i2 = dstPos + i;
                boolean z = true;
                if (((src >> (i + srcPos)) & 1) == 0) {
                    z = false;
                }
                dst[i2] = z;
            }
            return dst;
        }
        throw new IllegalArgumentException("nBools-1+srcPos is greater or equal to than 16");
    }

    public static boolean[] byteToBinary(byte src, int srcPos, boolean[] dst, int dstPos, int nBools) {
        if (nBools == 0) {
            return dst;
        }
        if ((nBools - 1) + srcPos < 8) {
            for (int i = 0; i < nBools; i++) {
                int i2 = dstPos + i;
                boolean z = true;
                if (((src >> (i + srcPos)) & 1) == 0) {
                    z = false;
                }
                dst[i2] = z;
            }
            return dst;
        }
        throw new IllegalArgumentException("nBools-1+srcPos is greater or equal to than 8");
    }

    public static byte[] uuidToByteArray(UUID src, byte[] dst, int dstPos, int nBytes) {
        if (nBytes == 0) {
            return dst;
        }
        if (nBytes <= 16) {
            longToByteArray(src.getMostSignificantBits(), 0, dst, dstPos, nBytes > 8 ? 8 : nBytes);
            if (nBytes >= 8) {
                longToByteArray(src.getLeastSignificantBits(), 0, dst, dstPos + 8, nBytes - 8);
            }
            return dst;
        }
        throw new IllegalArgumentException("nBytes is greater than 16");
    }

    public static UUID byteArrayToUuid(byte[] src, int srcPos) {
        if (src.length - srcPos >= 16) {
            return new UUID(byteArrayToLong(src, srcPos, 0, 0, 8), byteArrayToLong(src, srcPos + 8, 0, 0, 8));
        }
        throw new IllegalArgumentException("Need at least 16 bytes for UUID");
    }
}
