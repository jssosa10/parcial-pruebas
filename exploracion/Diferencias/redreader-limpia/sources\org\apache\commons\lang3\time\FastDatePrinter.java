package org.apache.commons.lang3.time;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTimeConstants;

public class FastDatePrinter implements DatePrinter, Serializable {
    public static final int FULL = 0;
    public static final int LONG = 1;
    private static final int MAX_DIGITS = 10;
    public static final int MEDIUM = 2;
    public static final int SHORT = 3;
    private static final ConcurrentMap<TimeZoneDisplayKey, String> cTimeZoneDisplayCache = new ConcurrentHashMap(7);
    private static final long serialVersionUID = 1;
    private final Locale mLocale;
    private transient int mMaxLengthEstimate;
    private final String mPattern;
    private transient Rule[] mRules;
    private final TimeZone mTimeZone;

    private static class CharacterLiteral implements Rule {
        private final char mValue;

        CharacterLiteral(char value) {
            this.mValue = value;
        }

        public int estimateLength() {
            return 1;
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            buffer.append(this.mValue);
        }
    }

    private static class DayInWeekField implements NumberRule {
        private final NumberRule mRule;

        DayInWeekField(NumberRule rule) {
            this.mRule = rule;
        }

        public int estimateLength() {
            return this.mRule.estimateLength();
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            int i = 7;
            int value = calendar.get(7);
            NumberRule numberRule = this.mRule;
            if (value != 1) {
                i = value - 1;
            }
            numberRule.appendTo(buffer, i);
        }

        public void appendTo(Appendable buffer, int value) throws IOException {
            this.mRule.appendTo(buffer, value);
        }
    }

    private static class Iso8601_Rule implements Rule {
        static final Iso8601_Rule ISO8601_HOURS = new Iso8601_Rule(3);
        static final Iso8601_Rule ISO8601_HOURS_COLON_MINUTES = new Iso8601_Rule(6);
        static final Iso8601_Rule ISO8601_HOURS_MINUTES = new Iso8601_Rule(5);
        final int length;

        static Iso8601_Rule getRule(int tokenLen) {
            switch (tokenLen) {
                case 1:
                    return ISO8601_HOURS;
                case 2:
                    return ISO8601_HOURS_MINUTES;
                case 3:
                    return ISO8601_HOURS_COLON_MINUTES;
                default:
                    throw new IllegalArgumentException("invalid number of X");
            }
        }

        Iso8601_Rule(int length2) {
            this.length = length2;
        }

        public int estimateLength() {
            return this.length;
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            int offset = calendar.get(15) + calendar.get(16);
            if (offset == 0) {
                buffer.append("Z");
                return;
            }
            if (offset < 0) {
                buffer.append('-');
                offset = -offset;
            } else {
                buffer.append('+');
            }
            int hours = offset / DateTimeConstants.MILLIS_PER_HOUR;
            FastDatePrinter.appendDigits(buffer, hours);
            int i = this.length;
            if (i >= 5) {
                if (i == 6) {
                    buffer.append(':');
                }
                FastDatePrinter.appendDigits(buffer, (offset / DateTimeConstants.MILLIS_PER_MINUTE) - (hours * 60));
            }
        }
    }

    private interface NumberRule extends Rule {
        void appendTo(Appendable appendable, int i) throws IOException;
    }

    private static class PaddedNumberField implements NumberRule {
        private final int mField;
        private final int mSize;

        PaddedNumberField(int field, int size) {
            if (size >= 3) {
                this.mField = field;
                this.mSize = size;
                return;
            }
            throw new IllegalArgumentException();
        }

        public int estimateLength() {
            return this.mSize;
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            appendTo(buffer, calendar.get(this.mField));
        }

        public final void appendTo(Appendable buffer, int value) throws IOException {
            FastDatePrinter.appendFullDigits(buffer, value, this.mSize);
        }
    }

    private interface Rule {
        void appendTo(Appendable appendable, Calendar calendar) throws IOException;

        int estimateLength();
    }

    private static class StringLiteral implements Rule {
        private final String mValue;

        StringLiteral(String value) {
            this.mValue = value;
        }

        public int estimateLength() {
            return this.mValue.length();
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            buffer.append(this.mValue);
        }
    }

    private static class TextField implements Rule {
        private final int mField;
        private final String[] mValues;

        TextField(int field, String[] values) {
            this.mField = field;
            this.mValues = values;
        }

        public int estimateLength() {
            int max = 0;
            int i = this.mValues.length;
            while (true) {
                i--;
                if (i < 0) {
                    return max;
                }
                int len = this.mValues[i].length();
                if (len > max) {
                    max = len;
                }
            }
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            buffer.append(this.mValues[calendar.get(this.mField)]);
        }
    }

    private static class TimeZoneDisplayKey {
        private final Locale mLocale;
        private final int mStyle;
        private final TimeZone mTimeZone;

        TimeZoneDisplayKey(TimeZone timeZone, boolean daylight, int style, Locale locale) {
            this.mTimeZone = timeZone;
            if (daylight) {
                this.mStyle = Integer.MIN_VALUE | style;
            } else {
                this.mStyle = style;
            }
            this.mLocale = locale;
        }

        public int hashCode() {
            return (((this.mStyle * 31) + this.mLocale.hashCode()) * 31) + this.mTimeZone.hashCode();
        }

        public boolean equals(Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TimeZoneDisplayKey)) {
                return false;
            }
            TimeZoneDisplayKey other = (TimeZoneDisplayKey) obj;
            if (!this.mTimeZone.equals(other.mTimeZone) || this.mStyle != other.mStyle || !this.mLocale.equals(other.mLocale)) {
                z = false;
            }
            return z;
        }
    }

    private static class TimeZoneNameRule implements Rule {
        private final String mDaylight;
        private final Locale mLocale;
        private final String mStandard;
        private final int mStyle;

        TimeZoneNameRule(TimeZone timeZone, Locale locale, int style) {
            this.mLocale = locale;
            this.mStyle = style;
            this.mStandard = FastDatePrinter.getTimeZoneDisplay(timeZone, false, style, locale);
            this.mDaylight = FastDatePrinter.getTimeZoneDisplay(timeZone, true, style, locale);
        }

        public int estimateLength() {
            return Math.max(this.mStandard.length(), this.mDaylight.length());
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            TimeZone zone = calendar.getTimeZone();
            if (calendar.get(16) != 0) {
                buffer.append(FastDatePrinter.getTimeZoneDisplay(zone, true, this.mStyle, this.mLocale));
            } else {
                buffer.append(FastDatePrinter.getTimeZoneDisplay(zone, false, this.mStyle, this.mLocale));
            }
        }
    }

    private static class TimeZoneNumberRule implements Rule {
        static final TimeZoneNumberRule INSTANCE_COLON = new TimeZoneNumberRule(true);
        static final TimeZoneNumberRule INSTANCE_NO_COLON = new TimeZoneNumberRule(false);
        final boolean mColon;

        TimeZoneNumberRule(boolean colon) {
            this.mColon = colon;
        }

        public int estimateLength() {
            return 5;
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            int offset = calendar.get(15) + calendar.get(16);
            if (offset < 0) {
                buffer.append('-');
                offset = -offset;
            } else {
                buffer.append('+');
            }
            int hours = offset / DateTimeConstants.MILLIS_PER_HOUR;
            FastDatePrinter.appendDigits(buffer, hours);
            if (this.mColon) {
                buffer.append(':');
            }
            FastDatePrinter.appendDigits(buffer, (offset / DateTimeConstants.MILLIS_PER_MINUTE) - (hours * 60));
        }
    }

    private static class TwelveHourField implements NumberRule {
        private final NumberRule mRule;

        TwelveHourField(NumberRule rule) {
            this.mRule = rule;
        }

        public int estimateLength() {
            return this.mRule.estimateLength();
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            int value = calendar.get(10);
            if (value == 0) {
                value = calendar.getLeastMaximum(10) + 1;
            }
            this.mRule.appendTo(buffer, value);
        }

        public void appendTo(Appendable buffer, int value) throws IOException {
            this.mRule.appendTo(buffer, value);
        }
    }

    private static class TwentyFourHourField implements NumberRule {
        private final NumberRule mRule;

        TwentyFourHourField(NumberRule rule) {
            this.mRule = rule;
        }

        public int estimateLength() {
            return this.mRule.estimateLength();
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            int value = calendar.get(11);
            if (value == 0) {
                value = calendar.getMaximum(11) + 1;
            }
            this.mRule.appendTo(buffer, value);
        }

        public void appendTo(Appendable buffer, int value) throws IOException {
            this.mRule.appendTo(buffer, value);
        }
    }

    private static class TwoDigitMonthField implements NumberRule {
        static final TwoDigitMonthField INSTANCE = new TwoDigitMonthField();

        TwoDigitMonthField() {
        }

        public int estimateLength() {
            return 2;
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            appendTo(buffer, calendar.get(2) + 1);
        }

        public final void appendTo(Appendable buffer, int value) throws IOException {
            FastDatePrinter.appendDigits(buffer, value);
        }
    }

    private static class TwoDigitNumberField implements NumberRule {
        private final int mField;

        TwoDigitNumberField(int field) {
            this.mField = field;
        }

        public int estimateLength() {
            return 2;
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            appendTo(buffer, calendar.get(this.mField));
        }

        public final void appendTo(Appendable buffer, int value) throws IOException {
            if (value < 100) {
                FastDatePrinter.appendDigits(buffer, value);
            } else {
                FastDatePrinter.appendFullDigits(buffer, value, 2);
            }
        }
    }

    private static class TwoDigitYearField implements NumberRule {
        static final TwoDigitYearField INSTANCE = new TwoDigitYearField();

        TwoDigitYearField() {
        }

        public int estimateLength() {
            return 2;
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            appendTo(buffer, calendar.get(1) % 100);
        }

        public final void appendTo(Appendable buffer, int value) throws IOException {
            FastDatePrinter.appendDigits(buffer, value);
        }
    }

    private static class UnpaddedMonthField implements NumberRule {
        static final UnpaddedMonthField INSTANCE = new UnpaddedMonthField();

        UnpaddedMonthField() {
        }

        public int estimateLength() {
            return 2;
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            appendTo(buffer, calendar.get(2) + 1);
        }

        public final void appendTo(Appendable buffer, int value) throws IOException {
            if (value < 10) {
                buffer.append((char) (value + 48));
            } else {
                FastDatePrinter.appendDigits(buffer, value);
            }
        }
    }

    private static class UnpaddedNumberField implements NumberRule {
        private final int mField;

        UnpaddedNumberField(int field) {
            this.mField = field;
        }

        public int estimateLength() {
            return 4;
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            appendTo(buffer, calendar.get(this.mField));
        }

        public final void appendTo(Appendable buffer, int value) throws IOException {
            if (value < 10) {
                buffer.append((char) (value + 48));
            } else if (value < 100) {
                FastDatePrinter.appendDigits(buffer, value);
            } else {
                FastDatePrinter.appendFullDigits(buffer, value, 1);
            }
        }
    }

    private static class WeekYear implements NumberRule {
        private final NumberRule mRule;

        WeekYear(NumberRule rule) {
            this.mRule = rule;
        }

        public int estimateLength() {
            return this.mRule.estimateLength();
        }

        public void appendTo(Appendable buffer, Calendar calendar) throws IOException {
            this.mRule.appendTo(buffer, calendar.getWeekYear());
        }

        public void appendTo(Appendable buffer, int value) throws IOException {
            this.mRule.appendTo(buffer, value);
        }
    }

    protected FastDatePrinter(String pattern, TimeZone timeZone, Locale locale) {
        this.mPattern = pattern;
        this.mTimeZone = timeZone;
        this.mLocale = locale;
        init();
    }

    private void init() {
        List<Rule> rulesList = parsePattern();
        this.mRules = (Rule[]) rulesList.toArray(new Rule[rulesList.size()]);
        int len = 0;
        int i = this.mRules.length;
        while (true) {
            i--;
            if (i >= 0) {
                len += this.mRules[i].estimateLength();
            } else {
                this.mMaxLengthEstimate = len;
                return;
            }
        }
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x01b9  */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x01bd  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x01cc  */
    /* JADX WARNING: Removed duplicated region for block: B:97:0x023c A[SYNTHETIC] */
    public List<Rule> parsePattern() {
        String[] weekdays;
        DateFormatSymbols symbols;
        String[] months;
        Object obj;
        DateFormatSymbols symbols2 = new DateFormatSymbols(this.mLocale);
        List<Rule> rules = new ArrayList<>();
        String[] ERAs = symbols2.getEras();
        String[] months2 = symbols2.getMonths();
        String[] shortMonths = symbols2.getShortMonths();
        String[] weekdays2 = symbols2.getWeekdays();
        String[] shortWeekdays = symbols2.getShortWeekdays();
        String[] AmPmStrings = symbols2.getAmPmStrings();
        int length = this.mPattern.length();
        int[] indexRef = new int[1];
        int i = 0;
        while (true) {
            if (i < length) {
                indexRef[0] = i;
                String token = parseToken(this.mPattern, indexRef);
                int i2 = indexRef[0];
                int tokenLen = token.length();
                if (tokenLen == 0) {
                    DateFormatSymbols dateFormatSymbols = symbols2;
                    String[] strArr = months2;
                    String[] strArr2 = weekdays2;
                } else {
                    char c = token.charAt(0);
                    switch (c) {
                        case 'D':
                            symbols = symbols2;
                            months = months2;
                            weekdays = weekdays2;
                            obj = selectNumberRule(6, tokenLen);
                            break;
                        case 'E':
                            symbols = symbols2;
                            months = months2;
                            weekdays = weekdays2;
                            obj = new TextField(7, tokenLen < 4 ? shortWeekdays : weekdays);
                            break;
                        case 'F':
                            symbols = symbols2;
                            months = months2;
                            weekdays = weekdays2;
                            obj = selectNumberRule(8, tokenLen);
                            break;
                        case 'G':
                            symbols = symbols2;
                            months = months2;
                            weekdays = weekdays2;
                            obj = new TextField(0, ERAs);
                            break;
                        case 'H':
                            symbols = symbols2;
                            months = months2;
                            weekdays = weekdays2;
                            obj = selectNumberRule(11, tokenLen);
                            break;
                        default:
                            switch (c) {
                                case 'W':
                                    symbols = symbols2;
                                    months = months2;
                                    weekdays = weekdays2;
                                    obj = selectNumberRule(4, tokenLen);
                                    break;
                                case 'X':
                                    symbols = symbols2;
                                    months = months2;
                                    weekdays = weekdays2;
                                    obj = Iso8601_Rule.getRule(tokenLen);
                                    break;
                                case 'Y':
                                    symbols = symbols2;
                                    months = months2;
                                    weekdays = weekdays2;
                                    if (tokenLen != 2) {
                                    }
                                    if (c == 'Y') {
                                    }
                                    break;
                                case 'Z':
                                    symbols = symbols2;
                                    months = months2;
                                    weekdays = weekdays2;
                                    if (tokenLen != 1) {
                                        if (tokenLen != 2) {
                                            obj = TimeZoneNumberRule.INSTANCE_COLON;
                                            break;
                                        } else {
                                            obj = Iso8601_Rule.ISO8601_HOURS_COLON_MINUTES;
                                            break;
                                        }
                                    } else {
                                        obj = TimeZoneNumberRule.INSTANCE_NO_COLON;
                                        break;
                                    }
                                default:
                                    switch (c) {
                                        case 'y':
                                            break;
                                        case 'z':
                                            symbols = symbols2;
                                            weekdays = weekdays2;
                                            if (tokenLen < 4) {
                                                months = months2;
                                                obj = new TimeZoneNameRule(this.mTimeZone, this.mLocale, 0);
                                                break;
                                            } else {
                                                months = months2;
                                                obj = new TimeZoneNameRule(this.mTimeZone, this.mLocale, 1);
                                                continue;
                                            }
                                        default:
                                            switch (c) {
                                                case '\'':
                                                    symbols = symbols2;
                                                    weekdays = weekdays2;
                                                    String sub = token.substring(1);
                                                    if (sub.length() != 1) {
                                                        obj = new StringLiteral(sub);
                                                        months = months2;
                                                        break;
                                                    } else {
                                                        obj = new CharacterLiteral(sub.charAt(0));
                                                        months = months2;
                                                        break;
                                                    }
                                                case 'K':
                                                    symbols = symbols2;
                                                    weekdays = weekdays2;
                                                    obj = selectNumberRule(10, tokenLen);
                                                    months = months2;
                                                    break;
                                                case 'M':
                                                    symbols = symbols2;
                                                    weekdays = weekdays2;
                                                    if (tokenLen < 4) {
                                                        if (tokenLen != 3) {
                                                            if (tokenLen != 2) {
                                                                obj = UnpaddedMonthField.INSTANCE;
                                                                months = months2;
                                                                break;
                                                            } else {
                                                                obj = TwoDigitMonthField.INSTANCE;
                                                                months = months2;
                                                                break;
                                                            }
                                                        } else {
                                                            obj = new TextField(2, shortMonths);
                                                            months = months2;
                                                            break;
                                                        }
                                                    } else {
                                                        obj = new TextField(2, months2);
                                                        months = months2;
                                                        break;
                                                    }
                                                case 'S':
                                                    symbols = symbols2;
                                                    weekdays = weekdays2;
                                                    obj = selectNumberRule(14, tokenLen);
                                                    months = months2;
                                                    break;
                                                case 'a':
                                                    symbols = symbols2;
                                                    weekdays = weekdays2;
                                                    obj = new TextField(9, AmPmStrings);
                                                    months = months2;
                                                    break;
                                                case 'd':
                                                    symbols = symbols2;
                                                    weekdays = weekdays2;
                                                    obj = selectNumberRule(5, tokenLen);
                                                    months = months2;
                                                    break;
                                                case 'h':
                                                    symbols = symbols2;
                                                    weekdays = weekdays2;
                                                    obj = new TwelveHourField(selectNumberRule(10, tokenLen));
                                                    months = months2;
                                                    break;
                                                case 'k':
                                                    symbols = symbols2;
                                                    weekdays = weekdays2;
                                                    obj = new TwentyFourHourField(selectNumberRule(11, tokenLen));
                                                    months = months2;
                                                    break;
                                                case 'm':
                                                    symbols = symbols2;
                                                    weekdays = weekdays2;
                                                    obj = selectNumberRule(12, tokenLen);
                                                    months = months2;
                                                    break;
                                                case 's':
                                                    symbols = symbols2;
                                                    weekdays = weekdays2;
                                                    obj = selectNumberRule(13, tokenLen);
                                                    months = months2;
                                                    break;
                                                case 'u':
                                                    symbols = symbols2;
                                                    weekdays = weekdays2;
                                                    obj = new DayInWeekField(selectNumberRule(7, tokenLen));
                                                    months = months2;
                                                    break;
                                                case 'w':
                                                    symbols = symbols2;
                                                    weekdays = weekdays2;
                                                    obj = selectNumberRule(3, tokenLen);
                                                    months = months2;
                                                    continue;
                                                default:
                                                    DateFormatSymbols dateFormatSymbols2 = symbols2;
                                                    StringBuilder sb = new StringBuilder();
                                                    String[] strArr3 = weekdays2;
                                                    sb.append("Illegal pattern component: ");
                                                    sb.append(token);
                                                    throw new IllegalArgumentException(sb.toString());
                                            }
                                    }
                                    symbols = symbols2;
                                    months = months2;
                                    weekdays = weekdays2;
                                    if (tokenLen != 2) {
                                        obj = TwoDigitYearField.INSTANCE;
                                    } else {
                                        obj = selectNumberRule(1, tokenLen < 4 ? 4 : tokenLen);
                                    }
                                    if (c == 'Y') {
                                        break;
                                    } else {
                                        obj = new WeekYear((NumberRule) obj);
                                        break;
                                    }
                            }
                    }
                    rules.add(obj);
                    i = i2 + 1;
                    months2 = months;
                    symbols2 = symbols;
                    weekdays2 = weekdays;
                }
            } else {
                String[] strArr4 = months2;
                String[] strArr5 = weekdays2;
            }
        }
        return rules;
    }

    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0068, code lost:
        r2 = r2 - 1;
     */
    public String parseToken(String pattern, int[] indexRef) {
        StringBuilder buf = new StringBuilder();
        int i = indexRef[0];
        int length = pattern.length();
        char c = pattern.charAt(i);
        if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z')) {
            buf.append('\'');
            boolean inLiteral = false;
            while (true) {
                if (i >= length) {
                    break;
                }
                char c2 = pattern.charAt(i);
                if (c2 == '\'') {
                    if (i + 1 >= length || pattern.charAt(i + 1) != '\'') {
                        inLiteral = !inLiteral;
                    } else {
                        i++;
                        buf.append(c2);
                    }
                } else if (inLiteral || ((c2 < 'A' || c2 > 'Z') && (c2 < 'a' || c2 > 'z'))) {
                    buf.append(c2);
                }
                i++;
            }
        } else {
            buf.append(c);
            while (i + 1 < length && pattern.charAt(i + 1) == c) {
                buf.append(c);
                i++;
            }
        }
        indexRef[0] = i;
        return buf.toString();
    }

    /* access modifiers changed from: protected */
    public NumberRule selectNumberRule(int field, int padding) {
        switch (padding) {
            case 1:
                return new UnpaddedNumberField(field);
            case 2:
                return new TwoDigitNumberField(field);
            default:
                return new PaddedNumberField(field, padding);
        }
    }

    @Deprecated
    public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
        if (obj instanceof Date) {
            return format((Date) obj, toAppendTo);
        }
        if (obj instanceof Calendar) {
            return format((Calendar) obj, toAppendTo);
        }
        if (obj instanceof Long) {
            return format(((Long) obj).longValue(), toAppendTo);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Unknown class: ");
        sb.append(obj == null ? "<null>" : obj.getClass().getName());
        throw new IllegalArgumentException(sb.toString());
    }

    /* access modifiers changed from: 0000 */
    public String format(Object obj) {
        if (obj instanceof Date) {
            return format((Date) obj);
        }
        if (obj instanceof Calendar) {
            return format((Calendar) obj);
        }
        if (obj instanceof Long) {
            return format(((Long) obj).longValue());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Unknown class: ");
        sb.append(obj == null ? "<null>" : obj.getClass().getName());
        throw new IllegalArgumentException(sb.toString());
    }

    public String format(long millis) {
        Calendar c = newCalendar();
        c.setTimeInMillis(millis);
        return applyRulesToString(c);
    }

    private String applyRulesToString(Calendar c) {
        return ((StringBuilder) applyRules(c, (B) new StringBuilder(this.mMaxLengthEstimate))).toString();
    }

    private Calendar newCalendar() {
        return Calendar.getInstance(this.mTimeZone, this.mLocale);
    }

    public String format(Date date) {
        Calendar c = newCalendar();
        c.setTime(date);
        return applyRulesToString(c);
    }

    public String format(Calendar calendar) {
        return ((StringBuilder) format(calendar, (B) new StringBuilder(this.mMaxLengthEstimate))).toString();
    }

    public StringBuffer format(long millis, StringBuffer buf) {
        Calendar c = newCalendar();
        c.setTimeInMillis(millis);
        return (StringBuffer) applyRules(c, (B) buf);
    }

    public StringBuffer format(Date date, StringBuffer buf) {
        Calendar c = newCalendar();
        c.setTime(date);
        return (StringBuffer) applyRules(c, (B) buf);
    }

    public StringBuffer format(Calendar calendar, StringBuffer buf) {
        return format(calendar.getTime(), buf);
    }

    public <B extends Appendable> B format(long millis, B buf) {
        Calendar c = newCalendar();
        c.setTimeInMillis(millis);
        return applyRules(c, buf);
    }

    public <B extends Appendable> B format(Date date, B buf) {
        Calendar c = newCalendar();
        c.setTime(date);
        return applyRules(c, buf);
    }

    public <B extends Appendable> B format(Calendar calendar, B buf) {
        if (!calendar.getTimeZone().equals(this.mTimeZone)) {
            calendar = (Calendar) calendar.clone();
            calendar.setTimeZone(this.mTimeZone);
        }
        return applyRules(calendar, buf);
    }

    /* access modifiers changed from: protected */
    @Deprecated
    public StringBuffer applyRules(Calendar calendar, StringBuffer buf) {
        return (StringBuffer) applyRules(calendar, (B) buf);
    }

    private <B extends Appendable> B applyRules(Calendar calendar, B buf) {
        try {
            for (Rule rule : this.mRules) {
                rule.appendTo(buf, calendar);
            }
        } catch (IOException ioe) {
            ExceptionUtils.rethrow(ioe);
        }
        return buf;
    }

    public String getPattern() {
        return this.mPattern;
    }

    public TimeZone getTimeZone() {
        return this.mTimeZone;
    }

    public Locale getLocale() {
        return this.mLocale;
    }

    public int getMaxLengthEstimate() {
        return this.mMaxLengthEstimate;
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof FastDatePrinter)) {
            return false;
        }
        FastDatePrinter other = (FastDatePrinter) obj;
        if (this.mPattern.equals(other.mPattern) && this.mTimeZone.equals(other.mTimeZone) && this.mLocale.equals(other.mLocale)) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return this.mPattern.hashCode() + ((this.mTimeZone.hashCode() + (this.mLocale.hashCode() * 13)) * 13);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FastDatePrinter[");
        sb.append(this.mPattern);
        sb.append(",");
        sb.append(this.mLocale);
        sb.append(",");
        sb.append(this.mTimeZone.getID());
        sb.append("]");
        return sb.toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init();
    }

    /* access modifiers changed from: private */
    public static void appendDigits(Appendable buffer, int value) throws IOException {
        buffer.append((char) ((value / 10) + 48));
        buffer.append((char) ((value % 10) + 48));
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:14:0x0030, code lost:
        if (r6 < 100) goto L_0x003c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0032, code lost:
        r5.append((char) ((r6 / 100) + 48));
        r6 = r6 % 100;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x003c, code lost:
        r5.append('0');
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x003f, code lost:
        if (r6 < 10) goto L_0x004b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0041, code lost:
        r5.append((char) ((r6 / 10) + 48));
        r6 = r6 % 10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x004b, code lost:
        r5.append('0');
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x004e, code lost:
        r5.append((char) (r6 + 48));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:?, code lost:
        return;
     */
    public static void appendFullDigits(Appendable buffer, int value, int minFieldWidth) throws IOException {
        if (value < 10000) {
            int nDigits = 4;
            if (value < 1000) {
                nDigits = 4 - 1;
                if (value < 100) {
                    nDigits--;
                    if (value < 10) {
                        nDigits--;
                    }
                }
            }
            for (int i = minFieldWidth - nDigits; i > 0; i--) {
                buffer.append('0');
            }
            switch (nDigits) {
                case 1:
                    break;
                case 2:
                    break;
                case 3:
                    break;
                case 4:
                    buffer.append((char) ((value / 1000) + 48));
                    value %= 1000;
                    break;
                default:
                    return;
            }
        } else {
            char[] work = new char[10];
            int digit = 0;
            while (value != 0) {
                int digit2 = digit + 1;
                work[digit] = (char) ((value % 10) + 48);
                value /= 10;
                digit = digit2;
            }
            while (digit < minFieldWidth) {
                buffer.append('0');
                minFieldWidth--;
            }
            while (true) {
                digit--;
                if (digit >= 0) {
                    buffer.append(work[digit]);
                } else {
                    return;
                }
            }
        }
    }

    static String getTimeZoneDisplay(TimeZone tz, boolean daylight, int style, Locale locale) {
        TimeZoneDisplayKey key = new TimeZoneDisplayKey(tz, daylight, style, locale);
        String value = (String) cTimeZoneDisplayCache.get(key);
        if (value != null) {
            return value;
        }
        String value2 = tz.getDisplayName(daylight, style, locale);
        String prior = (String) cTimeZoneDisplayCache.putIfAbsent(key, value2);
        if (prior != null) {
            return prior;
        }
        return value2;
    }
}
