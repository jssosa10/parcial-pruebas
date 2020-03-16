package org.apache.commons.lang3.time;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FastDateParser implements DateParser, Serializable {
    private static final Strategy ABBREVIATED_YEAR_STRATEGY = new NumberStrategy(1) {
        /* access modifiers changed from: 0000 */
        public int modify(FastDateParser parser, int iValue) {
            return iValue < 100 ? parser.adjustYear(iValue) : iValue;
        }
    };
    private static final Strategy DAY_OF_MONTH_STRATEGY = new NumberStrategy(5);
    private static final Strategy DAY_OF_WEEK_IN_MONTH_STRATEGY = new NumberStrategy(8);
    private static final Strategy DAY_OF_WEEK_STRATEGY = new NumberStrategy(7) {
        /* access modifiers changed from: 0000 */
        public int modify(FastDateParser parser, int iValue) {
            if (iValue != 7) {
                return iValue + 1;
            }
            return 1;
        }
    };
    private static final Strategy DAY_OF_YEAR_STRATEGY = new NumberStrategy(6);
    private static final Strategy HOUR12_STRATEGY = new NumberStrategy(10) {
        /* access modifiers changed from: 0000 */
        public int modify(FastDateParser parser, int iValue) {
            if (iValue == 12) {
                return 0;
            }
            return iValue;
        }
    };
    private static final Strategy HOUR24_OF_DAY_STRATEGY = new NumberStrategy(11) {
        /* access modifiers changed from: 0000 */
        public int modify(FastDateParser parser, int iValue) {
            if (iValue == 24) {
                return 0;
            }
            return iValue;
        }
    };
    private static final Strategy HOUR_OF_DAY_STRATEGY = new NumberStrategy(11);
    private static final Strategy HOUR_STRATEGY = new NumberStrategy(10);
    static final Locale JAPANESE_IMPERIAL = new Locale("ja", "JP", "JP");
    private static final Strategy LITERAL_YEAR_STRATEGY = new NumberStrategy(1);
    /* access modifiers changed from: private */
    public static final Comparator<String> LONGER_FIRST_LOWERCASE = new Comparator<String>() {
        public int compare(String left, String right) {
            return right.compareTo(left);
        }
    };
    private static final Strategy MILLISECOND_STRATEGY = new NumberStrategy(14);
    private static final Strategy MINUTE_STRATEGY = new NumberStrategy(12);
    private static final Strategy NUMBER_MONTH_STRATEGY = new NumberStrategy(2) {
        /* access modifiers changed from: 0000 */
        public int modify(FastDateParser parser, int iValue) {
            return iValue - 1;
        }
    };
    private static final Strategy SECOND_STRATEGY = new NumberStrategy(13);
    private static final Strategy WEEK_OF_MONTH_STRATEGY = new NumberStrategy(4);
    private static final Strategy WEEK_OF_YEAR_STRATEGY = new NumberStrategy(3);
    private static final ConcurrentMap<Locale, Strategy>[] caches = new ConcurrentMap[17];
    private static final long serialVersionUID = 3;
    private final int century;
    private final Locale locale;
    /* access modifiers changed from: private */
    public final String pattern;
    private transient List<StrategyAndWidth> patterns;
    private final int startYear;
    private final TimeZone timeZone;

    private static class CaseInsensitiveTextStrategy extends PatternStrategy {
        private final int field;
        private final Map<String, Integer> lKeyValues;
        final Locale locale;

        CaseInsensitiveTextStrategy(int field2, Calendar definingCalendar, Locale locale2) {
            super();
            this.field = field2;
            this.locale = locale2;
            StringBuilder regex = new StringBuilder();
            regex.append("((?iu)");
            this.lKeyValues = FastDateParser.appendDisplayNames(definingCalendar, locale2, field2, regex);
            regex.setLength(regex.length() - 1);
            regex.append(")");
            createPattern(regex);
        }

        /* access modifiers changed from: 0000 */
        public void setCalendar(FastDateParser parser, Calendar cal, String value) {
            cal.set(this.field, ((Integer) this.lKeyValues.get(value.toLowerCase(this.locale))).intValue());
        }
    }

    private static class CopyQuotedStrategy extends Strategy {
        private final String formatField;

        CopyQuotedStrategy(String formatField2) {
            super();
            this.formatField = formatField2;
        }

        /* access modifiers changed from: 0000 */
        public boolean isNumber() {
            return false;
        }

        /* access modifiers changed from: 0000 */
        public boolean parse(FastDateParser parser, Calendar calendar, String source, ParsePosition pos, int maxWidth) {
            int idx = 0;
            while (idx < this.formatField.length()) {
                int sIdx = pos.getIndex() + idx;
                if (sIdx == source.length()) {
                    pos.setErrorIndex(sIdx);
                    return false;
                } else if (this.formatField.charAt(idx) != source.charAt(sIdx)) {
                    pos.setErrorIndex(sIdx);
                    return false;
                } else {
                    idx++;
                }
            }
            pos.setIndex(this.formatField.length() + pos.getIndex());
            return true;
        }
    }

    private static class ISO8601TimeZoneStrategy extends PatternStrategy {
        private static final Strategy ISO_8601_1_STRATEGY = new ISO8601TimeZoneStrategy("(Z|(?:[+-]\\d{2}))");
        private static final Strategy ISO_8601_2_STRATEGY = new ISO8601TimeZoneStrategy("(Z|(?:[+-]\\d{2}\\d{2}))");
        /* access modifiers changed from: private */
        public static final Strategy ISO_8601_3_STRATEGY = new ISO8601TimeZoneStrategy("(Z|(?:[+-]\\d{2}(?::)\\d{2}))");

        ISO8601TimeZoneStrategy(String pattern) {
            super();
            createPattern(pattern);
        }

        /* access modifiers changed from: 0000 */
        public void setCalendar(FastDateParser parser, Calendar cal, String value) {
            cal.setTimeZone(FastTimeZone.getGmtTimeZone(value));
        }

        static Strategy getStrategy(int tokenLen) {
            switch (tokenLen) {
                case 1:
                    return ISO_8601_1_STRATEGY;
                case 2:
                    return ISO_8601_2_STRATEGY;
                case 3:
                    return ISO_8601_3_STRATEGY;
                default:
                    throw new IllegalArgumentException("invalid number of X");
            }
        }
    }

    private static class NumberStrategy extends Strategy {
        private final int field;

        NumberStrategy(int field2) {
            super();
            this.field = field2;
        }

        /* access modifiers changed from: 0000 */
        public boolean isNumber() {
            return true;
        }

        /* access modifiers changed from: 0000 */
        public boolean parse(FastDateParser parser, Calendar calendar, String source, ParsePosition pos, int maxWidth) {
            int idx = pos.getIndex();
            int last = source.length();
            if (maxWidth == 0) {
                while (idx < last && Character.isWhitespace(source.charAt(idx))) {
                    idx++;
                }
                pos.setIndex(idx);
            } else {
                int end = idx + maxWidth;
                if (last > end) {
                    last = end;
                }
            }
            while (idx < last && Character.isDigit(source.charAt(idx))) {
                idx++;
            }
            if (pos.getIndex() == idx) {
                pos.setErrorIndex(idx);
                return false;
            }
            int value = Integer.parseInt(source.substring(pos.getIndex(), idx));
            pos.setIndex(idx);
            calendar.set(this.field, modify(parser, value));
            return true;
        }

        /* access modifiers changed from: 0000 */
        public int modify(FastDateParser parser, int iValue) {
            return iValue;
        }
    }

    private static abstract class PatternStrategy extends Strategy {
        private Pattern pattern;

        /* access modifiers changed from: 0000 */
        public abstract void setCalendar(FastDateParser fastDateParser, Calendar calendar, String str);

        private PatternStrategy() {
            super();
        }

        /* access modifiers changed from: 0000 */
        public void createPattern(StringBuilder regex) {
            createPattern(regex.toString());
        }

        /* access modifiers changed from: 0000 */
        public void createPattern(String regex) {
            this.pattern = Pattern.compile(regex);
        }

        /* access modifiers changed from: 0000 */
        public boolean isNumber() {
            return false;
        }

        /* access modifiers changed from: 0000 */
        public boolean parse(FastDateParser parser, Calendar calendar, String source, ParsePosition pos, int maxWidth) {
            Matcher matcher = this.pattern.matcher(source.substring(pos.getIndex()));
            if (!matcher.lookingAt()) {
                pos.setErrorIndex(pos.getIndex());
                return false;
            }
            pos.setIndex(pos.getIndex() + matcher.end(1));
            setCalendar(parser, calendar, matcher.group(1));
            return true;
        }
    }

    private static abstract class Strategy {
        /* access modifiers changed from: 0000 */
        public abstract boolean parse(FastDateParser fastDateParser, Calendar calendar, String str, ParsePosition parsePosition, int i);

        private Strategy() {
        }

        /* access modifiers changed from: 0000 */
        public boolean isNumber() {
            return false;
        }
    }

    private static class StrategyAndWidth {
        final Strategy strategy;
        final int width;

        StrategyAndWidth(Strategy strategy2, int width2) {
            this.strategy = strategy2;
            this.width = width2;
        }

        /* access modifiers changed from: 0000 */
        public int getMaxWidth(ListIterator<StrategyAndWidth> lt) {
            int i = 0;
            if (!this.strategy.isNumber() || !lt.hasNext()) {
                return 0;
            }
            Strategy nextStrategy = ((StrategyAndWidth) lt.next()).strategy;
            lt.previous();
            if (nextStrategy.isNumber()) {
                i = this.width;
            }
            return i;
        }
    }

    private class StrategyParser {
        private int currentIdx;
        private final Calendar definingCalendar;

        StrategyParser(Calendar definingCalendar2) {
            this.definingCalendar = definingCalendar2;
        }

        /* access modifiers changed from: 0000 */
        public StrategyAndWidth getNextStrategy() {
            if (this.currentIdx >= FastDateParser.this.pattern.length()) {
                return null;
            }
            char c = FastDateParser.this.pattern.charAt(this.currentIdx);
            if (FastDateParser.isFormatLetter(c)) {
                return letterPattern(c);
            }
            return literal();
        }

        private StrategyAndWidth letterPattern(char c) {
            int begin = this.currentIdx;
            do {
                int i = this.currentIdx + 1;
                this.currentIdx = i;
                if (i >= FastDateParser.this.pattern.length()) {
                    break;
                }
            } while (FastDateParser.this.pattern.charAt(this.currentIdx) == c);
            int width = this.currentIdx - begin;
            return new StrategyAndWidth(FastDateParser.this.getStrategy(c, width, this.definingCalendar), width);
        }

        private StrategyAndWidth literal() {
            boolean activeQuote = false;
            StringBuilder sb = new StringBuilder();
            while (this.currentIdx < FastDateParser.this.pattern.length()) {
                char c = FastDateParser.this.pattern.charAt(this.currentIdx);
                if (!activeQuote && FastDateParser.isFormatLetter(c)) {
                    break;
                }
                boolean z = true;
                if (c == '\'') {
                    int i = this.currentIdx + 1;
                    this.currentIdx = i;
                    if (i == FastDateParser.this.pattern.length() || FastDateParser.this.pattern.charAt(this.currentIdx) != '\'') {
                        if (activeQuote) {
                            z = false;
                        }
                        activeQuote = z;
                    }
                }
                this.currentIdx++;
                sb.append(c);
            }
            if (!activeQuote) {
                String formatField = sb.toString();
                return new StrategyAndWidth(new CopyQuotedStrategy(formatField), formatField.length());
            }
            throw new IllegalArgumentException("Unterminated quote");
        }
    }

    static class TimeZoneStrategy extends PatternStrategy {
        private static final String GMT_OPTION = "GMT[+-]\\d{1,2}:\\d{2}";
        private static final int ID = 0;
        private static final String RFC_822_TIME_ZONE = "[+-]\\d{4}";
        private final Locale locale;
        private final Map<String, TzInfo> tzNames = new HashMap();

        private static class TzInfo {
            int dstOffset;
            TimeZone zone;

            TzInfo(TimeZone tz, boolean useDst) {
                this.zone = tz;
                this.dstOffset = useDst ? tz.getDSTSavings() : 0;
            }
        }

        TimeZoneStrategy(Locale locale2) {
            String[][] arr$;
            super();
            this.locale = locale2;
            StringBuilder sb = new StringBuilder();
            sb.append("((?iu)[+-]\\d{4}|GMT[+-]\\d{1,2}:\\d{2}");
            Set<String> sorted = new TreeSet<>(FastDateParser.LONGER_FIRST_LOWERCASE);
            for (String[] zoneNames : DateFormatSymbols.getInstance(locale2).getZoneStrings()) {
                String tzId = zoneNames[0];
                if (!tzId.equalsIgnoreCase(TimeZones.GMT_ID)) {
                    TimeZone tz = TimeZone.getTimeZone(tzId);
                    TzInfo tzInfo = new TzInfo(tz, false);
                    TzInfo standard = tzInfo;
                    for (int i = 1; i < zoneNames.length; i++) {
                        if (i == 3) {
                            tzInfo = new TzInfo(tz, true);
                        } else if (i == 5) {
                            tzInfo = standard;
                        }
                        if (zoneNames[i] != null) {
                            String key = zoneNames[i].toLowerCase(locale2);
                            if (sorted.add(key)) {
                                this.tzNames.put(key, tzInfo);
                            }
                        }
                    }
                }
            }
            for (String zoneName : sorted) {
                sb.append('|');
                FastDateParser.simpleQuote(sb, zoneName);
            }
            sb.append(")");
            createPattern(sb);
        }

        /* access modifiers changed from: 0000 */
        public void setCalendar(FastDateParser parser, Calendar cal, String timeZone) {
            TimeZone tz = FastTimeZone.getGmtTimeZone(timeZone);
            if (tz != null) {
                cal.setTimeZone(tz);
                return;
            }
            TzInfo tzInfo = (TzInfo) this.tzNames.get(timeZone.toLowerCase(this.locale));
            cal.set(16, tzInfo.dstOffset);
            cal.set(15, tzInfo.zone.getRawOffset());
        }
    }

    protected FastDateParser(String pattern2, TimeZone timeZone2, Locale locale2) {
        this(pattern2, timeZone2, locale2, null);
    }

    protected FastDateParser(String pattern2, TimeZone timeZone2, Locale locale2, Date centuryStart) {
        int centuryStartYear;
        this.pattern = pattern2;
        this.timeZone = timeZone2;
        this.locale = locale2;
        Calendar definingCalendar = Calendar.getInstance(timeZone2, locale2);
        if (centuryStart != null) {
            definingCalendar.setTime(centuryStart);
            centuryStartYear = definingCalendar.get(1);
        } else if (locale2.equals(JAPANESE_IMPERIAL)) {
            centuryStartYear = 0;
        } else {
            definingCalendar.setTime(new Date());
            centuryStartYear = definingCalendar.get(1) - 80;
        }
        this.century = (centuryStartYear / 100) * 100;
        this.startYear = centuryStartYear - this.century;
        init(definingCalendar);
    }

    private void init(Calendar definingCalendar) {
        this.patterns = new ArrayList();
        StrategyParser fm = new StrategyParser(definingCalendar);
        while (true) {
            StrategyAndWidth field = fm.getNextStrategy();
            if (field != null) {
                this.patterns.add(field);
            } else {
                return;
            }
        }
    }

    /* access modifiers changed from: private */
    public static boolean isFormatLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    public String getPattern() {
        return this.pattern;
    }

    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (!(obj instanceof FastDateParser)) {
            return false;
        }
        FastDateParser other = (FastDateParser) obj;
        if (this.pattern.equals(other.pattern) && this.timeZone.equals(other.timeZone) && this.locale.equals(other.locale)) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return this.pattern.hashCode() + ((this.timeZone.hashCode() + (this.locale.hashCode() * 13)) * 13);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FastDateParser[");
        sb.append(this.pattern);
        sb.append(",");
        sb.append(this.locale);
        sb.append(",");
        sb.append(this.timeZone.getID());
        sb.append("]");
        return sb.toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init(Calendar.getInstance(this.timeZone, this.locale));
    }

    public Object parseObject(String source) throws ParseException {
        return parse(source);
    }

    public Date parse(String source) throws ParseException {
        ParsePosition pp = new ParsePosition(0);
        Date date = parse(source, pp);
        if (date != null) {
            return date;
        }
        if (this.locale.equals(JAPANESE_IMPERIAL)) {
            StringBuilder sb = new StringBuilder();
            sb.append("(The ");
            sb.append(this.locale);
            sb.append(" locale does not support dates before 1868 AD)\n");
            sb.append("Unparseable date: \"");
            sb.append(source);
            throw new ParseException(sb.toString(), pp.getErrorIndex());
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Unparseable date: ");
        sb2.append(source);
        throw new ParseException(sb2.toString(), pp.getErrorIndex());
    }

    public Object parseObject(String source, ParsePosition pos) {
        return parse(source, pos);
    }

    public Date parse(String source, ParsePosition pos) {
        Calendar cal = Calendar.getInstance(this.timeZone, this.locale);
        cal.clear();
        if (parse(source, pos, cal)) {
            return cal.getTime();
        }
        return null;
    }

    public boolean parse(String source, ParsePosition pos, Calendar calendar) {
        ListIterator<StrategyAndWidth> lt = this.patterns.listIterator();
        while (lt.hasNext()) {
            StrategyAndWidth strategyAndWidth = (StrategyAndWidth) lt.next();
            if (!strategyAndWidth.strategy.parse(this, calendar, source, pos, strategyAndWidth.getMaxWidth(lt))) {
                return false;
            }
        }
        return true;
    }

    /* access modifiers changed from: private */
    public static StringBuilder simpleQuote(StringBuilder sb, String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '$':
                case '(':
                case ')':
                case '*':
                case '+':
                case '.':
                case '?':
                case '[':
                case '\\':
                case '^':
                case '{':
                case '|':
                    sb.append('\\');
                    break;
            }
            sb.append(c);
        }
        return sb;
    }

    /* access modifiers changed from: private */
    public static Map<String, Integer> appendDisplayNames(Calendar cal, Locale locale2, int field, StringBuilder regex) {
        Map<String, Integer> values = new HashMap<>();
        Map<String, Integer> displayNames = cal.getDisplayNames(field, 0, locale2);
        TreeSet<String> sorted = new TreeSet<>(LONGER_FIRST_LOWERCASE);
        for (Entry<String, Integer> displayName : displayNames.entrySet()) {
            String key = ((String) displayName.getKey()).toLowerCase(locale2);
            if (sorted.add(key)) {
                values.put(key, displayName.getValue());
            }
        }
        Iterator i$ = sorted.iterator();
        while (i$.hasNext()) {
            simpleQuote(regex, (String) i$.next()).append('|');
        }
        return values;
    }

    /* access modifiers changed from: private */
    public int adjustYear(int twoDigitYear) {
        int trial = this.century + twoDigitYear;
        return twoDigitYear >= this.startYear ? trial : trial + 100;
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x0063, code lost:
        return getLocaleSpecificStrategy(15, r6);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x0064, code lost:
        if (r5 <= 2) goto L_0x0069;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x0066, code lost:
        r0 = LITERAL_YEAR_STRATEGY;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x0069, code lost:
        r0 = ABBREVIATED_YEAR_STRATEGY;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x006b, code lost:
        return r0;
     */
    public Strategy getStrategy(char f, int width, Calendar definingCalendar) {
        switch (f) {
            case 'D':
                return DAY_OF_YEAR_STRATEGY;
            case 'E':
                return getLocaleSpecificStrategy(7, definingCalendar);
            case 'F':
                return DAY_OF_WEEK_IN_MONTH_STRATEGY;
            case 'G':
                return getLocaleSpecificStrategy(0, definingCalendar);
            case 'H':
                return HOUR_OF_DAY_STRATEGY;
            default:
                switch (f) {
                    case 'W':
                        return WEEK_OF_MONTH_STRATEGY;
                    case 'X':
                        return ISO8601TimeZoneStrategy.getStrategy(width);
                    case 'Y':
                        break;
                    case 'Z':
                        if (width == 2) {
                            return ISO8601TimeZoneStrategy.ISO_8601_3_STRATEGY;
                        }
                        break;
                    default:
                        switch (f) {
                            case 'y':
                                break;
                            case 'z':
                                break;
                            default:
                                switch (f) {
                                    case 'K':
                                        return HOUR_STRATEGY;
                                    case 'M':
                                        return width >= 3 ? getLocaleSpecificStrategy(2, definingCalendar) : NUMBER_MONTH_STRATEGY;
                                    case 'S':
                                        return MILLISECOND_STRATEGY;
                                    case 'a':
                                        return getLocaleSpecificStrategy(9, definingCalendar);
                                    case 'd':
                                        return DAY_OF_MONTH_STRATEGY;
                                    case 'h':
                                        return HOUR12_STRATEGY;
                                    case 'k':
                                        return HOUR24_OF_DAY_STRATEGY;
                                    case 'm':
                                        return MINUTE_STRATEGY;
                                    case 's':
                                        return SECOND_STRATEGY;
                                    case 'u':
                                        return DAY_OF_WEEK_STRATEGY;
                                    case 'w':
                                        return WEEK_OF_YEAR_STRATEGY;
                                    default:
                                        StringBuilder sb = new StringBuilder();
                                        sb.append("Format '");
                                        sb.append(f);
                                        sb.append("' not supported");
                                        throw new IllegalArgumentException(sb.toString());
                                }
                        }
                }
        }
    }

    private static ConcurrentMap<Locale, Strategy> getCache(int field) {
        ConcurrentMap<Locale, Strategy> concurrentMap;
        synchronized (caches) {
            if (caches[field] == null) {
                caches[field] = new ConcurrentHashMap(3);
            }
            concurrentMap = caches[field];
        }
        return concurrentMap;
    }

    private Strategy getLocaleSpecificStrategy(int field, Calendar definingCalendar) {
        ConcurrentMap<Locale, Strategy> cache = getCache(field);
        Strategy strategy = (Strategy) cache.get(this.locale);
        if (strategy == null) {
            strategy = field == 15 ? new TimeZoneStrategy(this.locale) : new CaseInsensitiveTextStrategy(field, definingCalendar, this.locale);
            Strategy inCache = (Strategy) cache.putIfAbsent(this.locale, strategy);
            if (inCache != null) {
                return inCache;
            }
        }
        return strategy;
    }
}
