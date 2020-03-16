package org.apache.commons.lang3.time;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public class DurationFormatUtils {
    static final Object H = "H";
    public static final String ISO_EXTENDED_FORMAT_PATTERN = "'P'yyyy'Y'M'M'd'DT'H'H'm'M's.SSS'S'";
    static final Object M = "M";
    static final Object S = "S";
    static final Object d = "d";
    static final Object m = "m";
    static final Object s = "s";
    static final Object y = "y";

    static class Token {
        private int count;
        private final Object value;

        static boolean containsTokenWithValue(Token[] tokens, Object value2) {
            for (Token token : tokens) {
                if (token.getValue() == value2) {
                    return true;
                }
            }
            return false;
        }

        Token(Object value2) {
            this.value = value2;
            this.count = 1;
        }

        Token(Object value2, int count2) {
            this.value = value2;
            this.count = count2;
        }

        /* access modifiers changed from: 0000 */
        public void increment() {
            this.count++;
        }

        /* access modifiers changed from: 0000 */
        public int getCount() {
            return this.count;
        }

        /* access modifiers changed from: 0000 */
        public Object getValue() {
            return this.value;
        }

        public boolean equals(Object obj2) {
            boolean z = false;
            if (!(obj2 instanceof Token)) {
                return false;
            }
            Token tok2 = (Token) obj2;
            if (this.value.getClass() != tok2.value.getClass() || this.count != tok2.count) {
                return false;
            }
            Object obj = this.value;
            if (obj instanceof StringBuilder) {
                return obj.toString().equals(tok2.value.toString());
            }
            if (obj instanceof Number) {
                return obj.equals(tok2.value);
            }
            if (obj == tok2.value) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return this.value.hashCode();
        }

        public String toString() {
            return StringUtils.repeat(this.value.toString(), this.count);
        }
    }

    public static String formatDurationHMS(long durationMillis) {
        return formatDuration(durationMillis, "HH:mm:ss.SSS");
    }

    public static String formatDurationISO(long durationMillis) {
        return formatDuration(durationMillis, ISO_EXTENDED_FORMAT_PATTERN, false);
    }

    public static String formatDuration(long durationMillis, String format) {
        return formatDuration(durationMillis, format, true);
    }

    public static String formatDuration(long durationMillis, String format, boolean padWithZeros) {
        long milliseconds;
        long seconds;
        Validate.inclusiveBetween(0, Long.MAX_VALUE, durationMillis, "durationMillis must not be negative");
        Token[] tokens = lexx(format);
        long days = 0;
        long hours = 0;
        long minutes = 0;
        long milliseconds2 = durationMillis;
        if (Token.containsTokenWithValue(tokens, d)) {
            days = milliseconds2 / DateUtils.MILLIS_PER_DAY;
            milliseconds2 -= DateUtils.MILLIS_PER_DAY * days;
        }
        if (Token.containsTokenWithValue(tokens, H)) {
            hours = milliseconds2 / DateUtils.MILLIS_PER_HOUR;
            milliseconds2 -= DateUtils.MILLIS_PER_HOUR * hours;
        }
        if (Token.containsTokenWithValue(tokens, m)) {
            minutes = milliseconds2 / 60000;
            milliseconds2 -= 60000 * minutes;
        }
        if (Token.containsTokenWithValue(tokens, s)) {
            long seconds2 = milliseconds2 / 1000;
            seconds = seconds2;
            milliseconds = milliseconds2 - (1000 * seconds2);
        } else {
            seconds = 0;
            milliseconds = milliseconds2;
        }
        return format(tokens, 0, 0, days, hours, minutes, seconds, milliseconds, padWithZeros);
    }

    public static String formatDurationWords(long durationMillis, boolean suppressLeadingZeroElements, boolean suppressTrailingZeroElements) {
        String duration = formatDuration(durationMillis, "d' days 'H' hours 'm' minutes 's' seconds'");
        if (suppressLeadingZeroElements) {
            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.SPACE);
            sb.append(duration);
            duration = sb.toString();
            String tmp = StringUtils.replaceOnce(duration, " 0 days", "");
            if (tmp.length() != duration.length()) {
                duration = tmp;
                String tmp2 = StringUtils.replaceOnce(duration, " 0 hours", "");
                if (tmp2.length() != duration.length()) {
                    String tmp3 = StringUtils.replaceOnce(tmp2, " 0 minutes", "");
                    duration = tmp3;
                    if (tmp3.length() != duration.length()) {
                        duration = StringUtils.replaceOnce(tmp3, " 0 seconds", "");
                    }
                }
            }
            if (duration.length() != 0) {
                duration = duration.substring(1);
            }
        }
        if (suppressTrailingZeroElements) {
            String tmp4 = StringUtils.replaceOnce(duration, " 0 seconds", "");
            if (tmp4.length() != duration.length()) {
                duration = tmp4;
                String tmp5 = StringUtils.replaceOnce(duration, " 0 minutes", "");
                if (tmp5.length() != duration.length()) {
                    duration = tmp5;
                    String tmp6 = StringUtils.replaceOnce(duration, " 0 hours", "");
                    if (tmp6.length() != duration.length()) {
                        duration = StringUtils.replaceOnce(tmp6, " 0 days", "");
                    }
                }
            }
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append(StringUtils.SPACE);
        sb2.append(duration);
        return StringUtils.replaceOnce(StringUtils.replaceOnce(StringUtils.replaceOnce(StringUtils.replaceOnce(sb2.toString(), " 1 seconds", " 1 second"), " 1 minutes", " 1 minute"), " 1 hours", " 1 hour"), " 1 days", " 1 day").trim();
    }

    public static String formatPeriodISO(long startMillis, long endMillis) {
        return formatPeriod(startMillis, endMillis, ISO_EXTENDED_FORMAT_PATTERN, false, TimeZone.getDefault());
    }

    public static String formatPeriod(long startMillis, long endMillis, String format) {
        return formatPeriod(startMillis, endMillis, format, true, TimeZone.getDefault());
    }

    public static String formatPeriod(long startMillis, long endMillis, String format, boolean padWithZeros, TimeZone timezone) {
        int months;
        int months2;
        int hours;
        int seconds;
        int days;
        int days2;
        long j = startMillis;
        long j2 = endMillis;
        Validate.isTrue(j <= j2, "startMillis must not be greater than endMillis", new Object[0]);
        Token[] tokens = lexx(format);
        Calendar start = Calendar.getInstance(timezone);
        start.setTime(new Date(j));
        Calendar end = Calendar.getInstance(timezone);
        end.setTime(new Date(j2));
        int milliseconds = end.get(14) - start.get(14);
        int seconds2 = end.get(13) - start.get(13);
        int minutes = end.get(12) - start.get(12);
        int hours2 = end.get(11) - start.get(11);
        int days3 = end.get(5) - start.get(5);
        int i = 2;
        int months3 = end.get(2) - start.get(2);
        int years = end.get(1) - start.get(1);
        while (milliseconds < 0) {
            milliseconds += 1000;
            seconds2--;
        }
        while (seconds2 < 0) {
            seconds2 += 60;
            minutes--;
        }
        while (minutes < 0) {
            minutes += 60;
            hours2--;
        }
        while (hours2 < 0) {
            hours2 += 24;
            days3--;
        }
        if (Token.containsTokenWithValue(tokens, M)) {
            while (days3 < 0) {
                days3 += start.getActualMaximum(5);
                months3--;
                start.add(2, 1);
            }
            while (months3 < 0) {
                months3 += 12;
                years--;
            }
            if (Token.containsTokenWithValue(tokens, y) || years == 0) {
                months2 = months3;
                months = years;
            } else {
                while (years != 0) {
                    months3 += years * 12;
                    years = 0;
                }
                months2 = months3;
                months = years;
            }
        } else {
            if (!Token.containsTokenWithValue(tokens, y)) {
                int i2 = 1;
                int target = end.get(1);
                if (months3 < 0) {
                    days = days3;
                    days2 = target - 1;
                } else {
                    days = days3;
                    days2 = target;
                }
                while (start.get(i2) != days2) {
                    int days4 = days + (start.getActualMaximum(6) - start.get(6));
                    if ((start instanceof GregorianCalendar) && start.get(i) == 1 && start.get(5) == 29) {
                        days4++;
                    }
                    start.add(1, 1);
                    days = days4 + start.get(6);
                    i2 = 1;
                    i = 2;
                }
                years = 0;
                days3 = days;
            }
            while (start.get(2) != end.get(2)) {
                days3 += start.getActualMaximum(5);
                start.add(2, 1);
            }
            int months4 = 0;
            while (days3 < 0) {
                days3 += start.getActualMaximum(5);
                months4--;
                start.add(2, 1);
            }
            months2 = months4;
            months = years;
        }
        if (!Token.containsTokenWithValue(tokens, d)) {
            hours2 += days3 * 24;
            days3 = 0;
        }
        if (!Token.containsTokenWithValue(tokens, H)) {
            minutes += hours2 * 60;
            hours = 0;
        } else {
            hours = hours2;
        }
        if (!Token.containsTokenWithValue(tokens, m)) {
            seconds2 += minutes * 60;
            minutes = 0;
        }
        if (!Token.containsTokenWithValue(tokens, s)) {
            milliseconds += seconds2 * 1000;
            seconds = 0;
        } else {
            seconds = seconds2;
        }
        long j3 = (long) months2;
        int i3 = months2;
        Calendar calendar = start;
        long j4 = (long) minutes;
        int i4 = milliseconds;
        int i5 = minutes;
        int i6 = seconds;
        long j5 = j3;
        int i7 = days3;
        int i8 = hours;
        int i9 = months;
        return format(tokens, (long) months, j5, (long) days3, (long) hours, j4, (long) seconds, (long) milliseconds, padWithZeros);
    }

    static String format(Token[] tokens, long years, long months, long days, long hours, long minutes, long seconds, long milliseconds, boolean padWithZeros) {
        int len$;
        Token[] arr$;
        boolean lastOutputSeconds;
        long j = milliseconds;
        boolean z = padWithZeros;
        StringBuilder buffer = new StringBuilder();
        boolean lastOutputSeconds2 = false;
        Token[] arr$2 = tokens;
        int len$2 = arr$2.length;
        int i$ = 0;
        while (i$ < len$2) {
            Token token = arr$2[i$];
            Object value = token.getValue();
            int count = token.getCount();
            if (value instanceof StringBuilder) {
                buffer.append(value.toString());
                long j2 = years;
                long j3 = months;
                lastOutputSeconds = lastOutputSeconds2;
                arr$ = arr$2;
                len$ = len$2;
                long j4 = seconds;
            } else {
                if (value.equals(y)) {
                    buffer.append(paddedValue(years, z, count));
                    lastOutputSeconds2 = false;
                    long j5 = months;
                    arr$ = arr$2;
                    len$ = len$2;
                    long j6 = seconds;
                } else {
                    long j7 = years;
                    if (value.equals(M)) {
                        buffer.append(paddedValue(months, z, count));
                        lastOutputSeconds2 = false;
                        arr$ = arr$2;
                        len$ = len$2;
                        long j8 = seconds;
                    } else {
                        long j9 = months;
                        if (value.equals(d)) {
                            arr$ = arr$2;
                            len$ = len$2;
                            Token token2 = token;
                            buffer.append(paddedValue(days, z, count));
                            lastOutputSeconds2 = false;
                            long j10 = seconds;
                        } else {
                            arr$ = arr$2;
                            len$ = len$2;
                            Token token3 = token;
                            long j11 = days;
                            if (value.equals(H)) {
                                buffer.append(paddedValue(hours, z, count));
                                lastOutputSeconds2 = false;
                                long j12 = seconds;
                            } else {
                                long j13 = hours;
                                if (value.equals(m)) {
                                    buffer.append(paddedValue(minutes, z, count));
                                    lastOutputSeconds2 = false;
                                    long j14 = seconds;
                                } else {
                                    long j15 = minutes;
                                    if (value.equals(s)) {
                                        buffer.append(paddedValue(seconds, z, count));
                                        lastOutputSeconds2 = true;
                                    } else {
                                        long j16 = seconds;
                                        if (value.equals(S)) {
                                            if (lastOutputSeconds2) {
                                                int width = 3;
                                                if (z) {
                                                    width = Math.max(3, count);
                                                }
                                                boolean z2 = lastOutputSeconds2;
                                                buffer.append(paddedValue(j, true, width));
                                            } else {
                                                buffer.append(paddedValue(j, z, count));
                                            }
                                            lastOutputSeconds2 = false;
                                        } else {
                                            lastOutputSeconds = lastOutputSeconds2;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                i$++;
                arr$2 = arr$;
                len$2 = len$;
            }
            lastOutputSeconds2 = lastOutputSeconds;
            i$++;
            arr$2 = arr$;
            len$2 = len$;
        }
        long j17 = years;
        long j18 = months;
        boolean z3 = lastOutputSeconds2;
        Token[] tokenArr = arr$2;
        int i = len$2;
        long j19 = seconds;
        return buffer.toString();
    }

    private static String paddedValue(long value, boolean padWithZeros, int count) {
        String longString = Long.toString(value);
        return padWithZeros ? StringUtils.leftPad(longString, count, '0') : longString;
    }

    static Token[] lexx(String format) {
        ArrayList<Token> list = new ArrayList<>(format.length());
        boolean inLiteral = false;
        StringBuilder buffer = null;
        Token previous = null;
        for (int i = 0; i < format.length(); i++) {
            char ch = format.charAt(i);
            if (!inLiteral || ch == '\'') {
                Object value = null;
                if (ch != '\'') {
                    if (ch == 'H') {
                        value = H;
                    } else if (ch == 'M') {
                        value = M;
                    } else if (ch == 'S') {
                        value = S;
                    } else if (ch == 'd') {
                        value = d;
                    } else if (ch == 'm') {
                        value = m;
                    } else if (ch == 's') {
                        value = s;
                    } else if (ch != 'y') {
                        if (buffer == null) {
                            buffer = new StringBuilder();
                            list.add(new Token(buffer));
                        }
                        buffer.append(ch);
                    } else {
                        value = y;
                    }
                } else if (inLiteral) {
                    buffer = null;
                    inLiteral = false;
                } else {
                    buffer = new StringBuilder();
                    list.add(new Token(buffer));
                    inLiteral = true;
                }
                if (value != null) {
                    if (previous == null || !previous.getValue().equals(value)) {
                        Token token = new Token(value);
                        list.add(token);
                        previous = token;
                    } else {
                        previous.increment();
                    }
                    buffer = null;
                }
            } else {
                buffer.append(ch);
            }
        }
        if (!inLiteral) {
            return (Token[]) list.toArray(new Token[list.size()]);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Unmatched quote in format: ");
        sb.append(format);
        throw new IllegalArgumentException(sb.toString());
    }
}
