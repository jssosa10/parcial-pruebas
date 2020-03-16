package org.apache.commons.lang3.math;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public class NumberUtils {
    public static final Byte BYTE_MINUS_ONE = Byte.valueOf(-1);
    public static final Byte BYTE_ONE = Byte.valueOf(1);
    public static final Byte BYTE_ZERO = Byte.valueOf(0);
    public static final Double DOUBLE_MINUS_ONE = Double.valueOf(-1.0d);
    public static final Double DOUBLE_ONE = Double.valueOf(1.0d);
    public static final Double DOUBLE_ZERO = Double.valueOf(0.0d);
    public static final Float FLOAT_MINUS_ONE = Float.valueOf(-1.0f);
    public static final Float FLOAT_ONE = Float.valueOf(1.0f);
    public static final Float FLOAT_ZERO = Float.valueOf(0.0f);
    public static final Integer INTEGER_MINUS_ONE = Integer.valueOf(-1);
    public static final Integer INTEGER_ONE = Integer.valueOf(1);
    public static final Integer INTEGER_ZERO = Integer.valueOf(0);
    public static final Long LONG_MINUS_ONE = Long.valueOf(-1);
    public static final Long LONG_ONE = Long.valueOf(1);
    public static final Long LONG_ZERO = Long.valueOf(0);
    public static final Short SHORT_MINUS_ONE = Short.valueOf(-1);
    public static final Short SHORT_ONE = Short.valueOf(1);
    public static final Short SHORT_ZERO = Short.valueOf(0);

    public static int toInt(String str) {
        return toInt(str, 0);
    }

    public static int toInt(String str, int defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long toLong(String str) {
        return toLong(str, 0);
    }

    public static long toLong(String str, long defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static float toFloat(String str) {
        return toFloat(str, 0.0f);
    }

    public static float toFloat(String str, float defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static double toDouble(String str) {
        return toDouble(str, 0.0d);
    }

    public static double toDouble(String str, double defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static byte toByte(String str) {
        return toByte(str, 0);
    }

    public static byte toByte(String str, byte defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Byte.parseByte(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static short toShort(String str) {
        return toShort(str, 0);
    }

    public static short toShort(String str, short defaultValue) {
        if (str == null) {
            return defaultValue;
        }
        try {
            return Short.parseShort(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:79:0x0157, code lost:
        if (r6 == 'l') goto L_0x0159;
     */
    public static Number createNumber(String str) throws NumberFormatException {
        int pfxLen;
        String mant;
        String dec;
        String exp;
        String exp2;
        String str2 = str;
        if (str2 == null) {
            return null;
        }
        if (!StringUtils.isBlank(str)) {
            boolean allZeros = false;
            String[] arr$ = {"0x", "0X", "-0x", "-0X", "#", "-#"};
            int len$ = arr$.length;
            int i$ = 0;
            while (true) {
                if (i$ >= len$) {
                    pfxLen = 0;
                    break;
                }
                String pfx = arr$[i$];
                if (str2.startsWith(pfx)) {
                    pfxLen = 0 + pfx.length();
                    break;
                }
                i$++;
            }
            if (pfxLen > 0) {
                char firstSigDigit = 0;
                for (int i = pfxLen; i < str.length(); i++) {
                    firstSigDigit = str2.charAt(i);
                    if (firstSigDigit != '0') {
                        break;
                    }
                    pfxLen++;
                }
                int hexDigits = str.length() - pfxLen;
                if (hexDigits > 16 || (hexDigits == 16 && firstSigDigit > '7')) {
                    return createBigInteger(str);
                }
                if (hexDigits > 8 || (hexDigits == 8 && firstSigDigit > '7')) {
                    return createLong(str);
                }
                return createInteger(str);
            }
            char lastChar = str2.charAt(str.length() - 1);
            int decPos = str2.indexOf(46);
            int expPos = str2.indexOf(101) + str2.indexOf(69) + 1;
            if (decPos > -1) {
                if (expPos <= -1) {
                    dec = str2.substring(decPos + 1);
                } else if (expPos < decPos || expPos > str.length()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(str2);
                    sb.append(" is not a valid number.");
                    throw new NumberFormatException(sb.toString());
                } else {
                    dec = str2.substring(decPos + 1, expPos);
                }
                mant = getMantissa(str2, decPos);
            } else {
                if (expPos <= -1) {
                    mant = getMantissa(str);
                } else if (expPos <= str.length()) {
                    mant = getMantissa(str2, expPos);
                } else {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(str2);
                    sb2.append(" is not a valid number.");
                    throw new NumberFormatException(sb2.toString());
                }
                dec = null;
            }
            if (Character.isDigit(lastChar) || lastChar == '.') {
                if (expPos <= -1 || expPos >= str.length() - 1) {
                    exp = null;
                } else {
                    exp = str2.substring(expPos + 1, str.length());
                }
                if (dec == null && exp == null) {
                    try {
                        return createInteger(str);
                    } catch (NumberFormatException e) {
                        try {
                            return createLong(str);
                        } catch (NumberFormatException e2) {
                            return createBigInteger(str);
                        }
                    }
                } else {
                    if (isAllZeros(mant) && isAllZeros(exp)) {
                        allZeros = true;
                    }
                    try {
                        Float f = createFloat(str);
                        Double d = createDouble(str);
                        if (!f.isInfinite() && ((f.floatValue() != 0.0f || allZeros) && f.toString().equals(d.toString()))) {
                            return f;
                        }
                        if (!d.isInfinite() && (d.doubleValue() != 0.0d || allZeros)) {
                            BigDecimal b = createBigDecimal(str);
                            if (b.compareTo(BigDecimal.valueOf(d.doubleValue())) == 0) {
                                return d;
                            }
                            return b;
                        }
                        return createBigDecimal(str);
                    } catch (NumberFormatException e3) {
                    }
                }
            } else {
                if (expPos <= -1 || expPos >= str.length() - 1) {
                    exp2 = null;
                } else {
                    exp2 = str2.substring(expPos + 1, str.length() - 1);
                }
                String numeric = str2.substring(0, str.length() - 1);
                boolean allZeros2 = isAllZeros(mant) && isAllZeros(exp2);
                if (lastChar != 'D') {
                    if (lastChar != 'F') {
                        if (lastChar != 'L') {
                            if (lastChar != 'd') {
                                if (lastChar != 'f') {
                                }
                            }
                        }
                        if (dec == null && exp2 == null && ((numeric.charAt(0) == '-' && isDigits(numeric.substring(1))) || isDigits(numeric))) {
                            try {
                                return createLong(numeric);
                            } catch (NumberFormatException e4) {
                                return createBigInteger(numeric);
                            }
                        } else {
                            StringBuilder sb3 = new StringBuilder();
                            sb3.append(str2);
                            sb3.append(" is not a valid number.");
                            throw new NumberFormatException(sb3.toString());
                        }
                    }
                    try {
                        Float f2 = createFloat(str);
                        if (!f2.isInfinite() && (f2.floatValue() != 0.0f || allZeros2)) {
                            return f2;
                        }
                    } catch (NumberFormatException e5) {
                    }
                }
                try {
                    Double d2 = createDouble(str);
                    if (!d2.isInfinite() && (((double) d2.floatValue()) != 0.0d || allZeros2)) {
                        return d2;
                    }
                } catch (NumberFormatException e6) {
                }
                try {
                    return createBigDecimal(numeric);
                } catch (NumberFormatException e7) {
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append(str2);
                    sb4.append(" is not a valid number.");
                    throw new NumberFormatException(sb4.toString());
                }
            }
        } else {
            throw new NumberFormatException("A blank string is not a valid number");
        }
    }

    private static String getMantissa(String str) {
        return getMantissa(str, str.length());
    }

    private static String getMantissa(String str, int stopPos) {
        char firstChar = str.charAt(0);
        return firstChar == '-' || firstChar == '+' ? str.substring(1, stopPos) : str.substring(0, stopPos);
    }

    private static boolean isAllZeros(String str) {
        boolean z = true;
        if (str == null) {
            return true;
        }
        for (int i = str.length() - 1; i >= 0; i--) {
            if (str.charAt(i) != '0') {
                return false;
            }
        }
        if (str.length() <= 0) {
            z = false;
        }
        return z;
    }

    public static Float createFloat(String str) {
        if (str == null) {
            return null;
        }
        return Float.valueOf(str);
    }

    public static Double createDouble(String str) {
        if (str == null) {
            return null;
        }
        return Double.valueOf(str);
    }

    public static Integer createInteger(String str) {
        if (str == null) {
            return null;
        }
        return Integer.decode(str);
    }

    public static Long createLong(String str) {
        if (str == null) {
            return null;
        }
        return Long.decode(str);
    }

    public static BigInteger createBigInteger(String str) {
        if (str == null) {
            return null;
        }
        int pos = 0;
        int radix = 10;
        boolean negate = false;
        if (str.startsWith("-")) {
            negate = true;
            pos = 1;
        }
        if (str.startsWith("0x", pos) || str.startsWith("0X", pos)) {
            radix = 16;
            pos += 2;
        } else if (str.startsWith("#", pos)) {
            radix = 16;
            pos++;
        } else if (str.startsWith("0", pos) && str.length() > pos + 1) {
            radix = 8;
            pos++;
        }
        BigInteger value = new BigInteger(str.substring(pos), radix);
        return negate ? value.negate() : value;
    }

    public static BigDecimal createBigDecimal(String str) {
        if (str == null) {
            return null;
        }
        if (StringUtils.isBlank(str)) {
            throw new NumberFormatException("A blank string is not a valid number");
        } else if (!str.trim().startsWith("--")) {
            return new BigDecimal(str);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append(" is not a valid number.");
            throw new NumberFormatException(sb.toString());
        }
    }

    public static long min(long... array) {
        validateArray(array);
        long min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
        return min;
    }

    public static int min(int... array) {
        validateArray(array);
        int min = array[0];
        for (int j = 1; j < array.length; j++) {
            if (array[j] < min) {
                min = array[j];
            }
        }
        return min;
    }

    public static short min(short... array) {
        validateArray(array);
        short min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
        return min;
    }

    public static byte min(byte... array) {
        validateArray(array);
        byte min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
        return min;
    }

    public static double min(double... array) {
        validateArray(array);
        double min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (Double.isNaN(array[i])) {
                return Double.NaN;
            }
            if (array[i] < min) {
                min = array[i];
            }
        }
        return min;
    }

    public static float min(float... array) {
        validateArray(array);
        float min = array[0];
        for (int i = 1; i < array.length; i++) {
            if (Float.isNaN(array[i])) {
                return Float.NaN;
            }
            if (array[i] < min) {
                min = array[i];
            }
        }
        return min;
    }

    public static long max(long... array) {
        validateArray(array);
        long max = array[0];
        for (int j = 1; j < array.length; j++) {
            if (array[j] > max) {
                max = array[j];
            }
        }
        return max;
    }

    public static int max(int... array) {
        validateArray(array);
        int max = array[0];
        for (int j = 1; j < array.length; j++) {
            if (array[j] > max) {
                max = array[j];
            }
        }
        return max;
    }

    public static short max(short... array) {
        validateArray(array);
        short max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    public static byte max(byte... array) {
        validateArray(array);
        byte max = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    public static double max(double... array) {
        validateArray(array);
        double max = array[0];
        for (int j = 1; j < array.length; j++) {
            if (Double.isNaN(array[j])) {
                return Double.NaN;
            }
            if (array[j] > max) {
                max = array[j];
            }
        }
        return max;
    }

    public static float max(float... array) {
        validateArray(array);
        float max = array[0];
        for (int j = 1; j < array.length; j++) {
            if (Float.isNaN(array[j])) {
                return Float.NaN;
            }
            if (array[j] > max) {
                max = array[j];
            }
        }
        return max;
    }

    private static void validateArray(Object array) {
        boolean z = true;
        Validate.isTrue(array != null, "The Array must not be null", new Object[0]);
        if (Array.getLength(array) == 0) {
            z = false;
        }
        Validate.isTrue(z, "Array cannot be empty.", new Object[0]);
    }

    public static long min(long a, long b, long c) {
        if (b < a) {
            a = b;
        }
        if (c < a) {
            return c;
        }
        return a;
    }

    public static int min(int a, int b, int c) {
        if (b < a) {
            a = b;
        }
        if (c < a) {
            return c;
        }
        return a;
    }

    public static short min(short a, short b, short c) {
        if (b < a) {
            a = b;
        }
        if (c < a) {
            return c;
        }
        return a;
    }

    public static byte min(byte a, byte b, byte c) {
        if (b < a) {
            a = b;
        }
        if (c < a) {
            return c;
        }
        return a;
    }

    public static double min(double a, double b, double c) {
        return Math.min(Math.min(a, b), c);
    }

    public static float min(float a, float b, float c) {
        return Math.min(Math.min(a, b), c);
    }

    public static long max(long a, long b, long c) {
        if (b > a) {
            a = b;
        }
        if (c > a) {
            return c;
        }
        return a;
    }

    public static int max(int a, int b, int c) {
        if (b > a) {
            a = b;
        }
        if (c > a) {
            return c;
        }
        return a;
    }

    public static short max(short a, short b, short c) {
        if (b > a) {
            a = b;
        }
        if (c > a) {
            return c;
        }
        return a;
    }

    public static byte max(byte a, byte b, byte c) {
        if (b > a) {
            a = b;
        }
        if (c > a) {
            return c;
        }
        return a;
    }

    public static double max(double a, double b, double c) {
        return Math.max(Math.max(a, b), c);
    }

    public static float max(float a, float b, float c) {
        return Math.max(Math.max(a, b), c);
    }

    public static boolean isDigits(String str) {
        return StringUtils.isNumeric(str);
    }

    @Deprecated
    public static boolean isNumber(String str) {
        return isCreatable(str);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:100:0x0106, code lost:
        if (r3 != false) goto L_0x010b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:101:0x0108, code lost:
        if (r4 != false) goto L_0x010b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:102:0x010a, code lost:
        r1 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:103:0x010b, code lost:
        return r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:104:0x010c, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:105:0x010d, code lost:
        if (r5 != false) goto L_0x0112;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:106:0x010f, code lost:
        if (r6 == false) goto L_0x0112;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:107:0x0111, code lost:
        r1 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:108:0x0112, code lost:
        return r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:63:0x00b1, code lost:
        if (r9 >= r0.length) goto L_0x010d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:0x00b5, code lost:
        if (r0[r9] < r8) goto L_0x00c7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:67:0x00bb, code lost:
        if (r0[r9] > '9') goto L_0x00c7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:69:0x00bf, code lost:
        if (org.apache.commons.lang3.SystemUtils.IS_JAVA_1_6 == false) goto L_0x00c6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:0x00c1, code lost:
        if (r11 == false) goto L_0x00c6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:71:0x00c3, code lost:
        if (r4 != false) goto L_0x00c6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x00c5, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x00c6, code lost:
        return r10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:75:0x00c9, code lost:
        if (r0[r9] == 'e') goto L_0x010c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:0x00cd, code lost:
        if (r0[r9] != 'E') goto L_0x00d0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x00d2, code lost:
        if (r0[r9] != '.') goto L_0x00db;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x00d4, code lost:
        if (r4 != false) goto L_0x00da;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:81:0x00d6, code lost:
        if (r3 == false) goto L_0x00d9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:0x00d9, code lost:
        return r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:83:0x00da, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:84:0x00db, code lost:
        if (r5 != false) goto L_0x00f6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:86:0x00e1, code lost:
        if (r0[r9] == 'd') goto L_0x00f5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:88:0x00e7, code lost:
        if (r0[r9] == 'D') goto L_0x00f5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:90:0x00ed, code lost:
        if (r0[r9] == 'f') goto L_0x00f5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:92:0x00f3, code lost:
        if (r0[r9] != 'F') goto L_0x00f6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:93:0x00f5, code lost:
        return r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:95:0x00fa, code lost:
        if (r0[r9] == 'l') goto L_0x0104;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:97:0x0100, code lost:
        if (r0[r9] != 'L') goto L_0x0103;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:98:0x0103, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:99:0x0104, code lost:
        if (r6 == false) goto L_0x010b;
     */
    public static boolean isCreatable(String str) {
        boolean z = false;
        if (StringUtils.isEmpty(str)) {
            return false;
        }
        char[] chars = str.toCharArray();
        int sz = chars.length;
        boolean hasExp = false;
        boolean hasDecPoint = false;
        boolean allowSigns = false;
        boolean foundDigit = false;
        boolean z2 = true;
        int start = (chars[0] == '-' || chars[0] == '+') ? 1 : 0;
        boolean hasLeadingPlusSign = start == 1 && chars[0] == '+';
        char c = '9';
        char c2 = '0';
        if (sz > start + 1 && chars[start] == '0') {
            if (chars[start + 1] == 'x' || chars[start + 1] == 'X') {
                int i = start + 2;
                if (i == sz) {
                    return false;
                }
                while (i < chars.length) {
                    if ((chars[i] < '0' || chars[i] > c) && ((chars[i] < 'a' || chars[i] > 'f') && (chars[i] < 'A' || chars[i] > 'F'))) {
                        return false;
                    }
                    i++;
                    c = '9';
                }
                return true;
            } else if (Character.isDigit(chars[start + 1])) {
                for (int i2 = start + 1; i2 < chars.length; i2++) {
                    if (chars[i2] < '0' || chars[i2] > '7') {
                        return false;
                    }
                }
                return true;
            }
        }
        int sz2 = sz - 1;
        int i3 = start;
        while (true) {
            if (i3 >= sz2) {
                if (i3 >= sz2 + 1 || !allowSigns || foundDigit) {
                }
            }
            if (chars[i3] >= c2) {
                if (chars[i3] <= '9') {
                    foundDigit = true;
                    allowSigns = false;
                    i3++;
                    c2 = '0';
                    z2 = true;
                }
            }
            if (chars[i3] != '.') {
                if (chars[i3] != 'e') {
                    if (chars[i3] != 'E') {
                        if (chars[i3] != '+') {
                            if (chars[i3] != '-') {
                                return false;
                            }
                        }
                        if (!allowSigns) {
                            return false;
                        }
                        allowSigns = false;
                        foundDigit = false;
                    }
                }
                if (hasExp || !foundDigit) {
                    return false;
                }
                hasExp = true;
                allowSigns = true;
            } else if (hasDecPoint || hasExp) {
                return false;
            } else {
                hasDecPoint = true;
            }
            i3++;
            c2 = '0';
            z2 = true;
        }
        return false;
    }

    public static boolean isParsable(String str) {
        if (StringUtils.isEmpty(str) || str.charAt(str.length() - 1) == '.') {
            return false;
        }
        if (str.charAt(0) != '-') {
            return withDecimalsParsing(str, 0);
        }
        if (str.length() == 1) {
            return false;
        }
        return withDecimalsParsing(str, 1);
    }

    private static boolean withDecimalsParsing(String str, int beginIdx) {
        int decimalPoints = 0;
        for (int i = beginIdx; i < str.length(); i++) {
            boolean isDecimalPoint = str.charAt(i) == '.';
            if (isDecimalPoint) {
                decimalPoints++;
            }
            if (decimalPoints > 1) {
                return false;
            }
            if (!isDecimalPoint && !Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static int compare(int x, int y) {
        if (x == y) {
            return 0;
        }
        return x < y ? -1 : 1;
    }

    public static int compare(long x, long y) {
        if (x == y) {
            return 0;
        }
        return x < y ? -1 : 1;
    }

    public static int compare(short x, short y) {
        if (x == y) {
            return 0;
        }
        return x < y ? -1 : 1;
    }

    public static int compare(byte x, byte y) {
        return x - y;
    }
}
