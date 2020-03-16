package org.apache.commons.lang3.time;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.Validate;

public class DateUtils {
    public static final long MILLIS_PER_DAY = 86400000;
    public static final long MILLIS_PER_HOUR = 3600000;
    public static final long MILLIS_PER_MINUTE = 60000;
    public static final long MILLIS_PER_SECOND = 1000;
    public static final int RANGE_MONTH_MONDAY = 6;
    public static final int RANGE_MONTH_SUNDAY = 5;
    public static final int RANGE_WEEK_CENTER = 4;
    public static final int RANGE_WEEK_MONDAY = 2;
    public static final int RANGE_WEEK_RELATIVE = 3;
    public static final int RANGE_WEEK_SUNDAY = 1;
    public static final int SEMI_MONTH = 1001;
    private static final int[][] fields = {new int[]{14}, new int[]{13}, new int[]{12}, new int[]{11, 10}, new int[]{5, 5, 9}, new int[]{2, 1001}, new int[]{1}, new int[]{0}};

    static class DateIterator implements Iterator<Calendar> {
        private final Calendar endFinal;
        private final Calendar spot;

        DateIterator(Calendar startFinal, Calendar endFinal2) {
            this.endFinal = endFinal2;
            this.spot = startFinal;
            this.spot.add(5, -1);
        }

        public boolean hasNext() {
            return this.spot.before(this.endFinal);
        }

        public Calendar next() {
            if (!this.spot.equals(this.endFinal)) {
                this.spot.add(5, 1);
                return (Calendar) this.spot.clone();
            }
            throw new NoSuchElementException();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private enum ModifyType {
        TRUNCATE,
        ROUND,
        CEILING
    }

    public static boolean isSameDay(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return isSameDay(cal1, cal2);
    }

    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 != null && cal2 != null) {
            return cal1.get(0) == cal2.get(0) && cal1.get(1) == cal2.get(1) && cal1.get(6) == cal2.get(6);
        }
        throw new IllegalArgumentException("The date must not be null");
    }

    public static boolean isSameInstant(Date date1, Date date2) {
        if (date1 != null && date2 != null) {
            return date1.getTime() == date2.getTime();
        }
        throw new IllegalArgumentException("The date must not be null");
    }

    public static boolean isSameInstant(Calendar cal1, Calendar cal2) {
        if (cal1 != null && cal2 != null) {
            return cal1.getTime().getTime() == cal2.getTime().getTime();
        }
        throw new IllegalArgumentException("The date must not be null");
    }

    public static boolean isSameLocalTime(Calendar cal1, Calendar cal2) {
        if (cal1 != null && cal2 != null) {
            return cal1.get(14) == cal2.get(14) && cal1.get(13) == cal2.get(13) && cal1.get(12) == cal2.get(12) && cal1.get(11) == cal2.get(11) && cal1.get(6) == cal2.get(6) && cal1.get(1) == cal2.get(1) && cal1.get(0) == cal2.get(0) && cal1.getClass() == cal2.getClass();
        }
        throw new IllegalArgumentException("The date must not be null");
    }

    public static Date parseDate(String str, String... parsePatterns) throws ParseException {
        return parseDate(str, null, parsePatterns);
    }

    public static Date parseDate(String str, Locale locale, String... parsePatterns) throws ParseException {
        return parseDateWithLeniency(str, locale, parsePatterns, true);
    }

    public static Date parseDateStrictly(String str, String... parsePatterns) throws ParseException {
        return parseDateStrictly(str, null, parsePatterns);
    }

    public static Date parseDateStrictly(String str, Locale locale, String... parsePatterns) throws ParseException {
        return parseDateWithLeniency(str, locale, parsePatterns, false);
    }

    private static Date parseDateWithLeniency(String str, Locale locale, String[] parsePatterns, boolean lenient) throws ParseException {
        if (str == null || parsePatterns == null) {
            throw new IllegalArgumentException("Date and Patterns must not be null");
        }
        TimeZone tz = TimeZone.getDefault();
        Locale lcl = locale == null ? Locale.getDefault() : locale;
        ParsePosition pos = new ParsePosition(0);
        Calendar calendar = Calendar.getInstance(tz, lcl);
        calendar.setLenient(lenient);
        for (String parsePattern : parsePatterns) {
            FastDateParser fdp = new FastDateParser(parsePattern, tz, lcl);
            calendar.clear();
            try {
                if (fdp.parse(str, pos, calendar) && pos.getIndex() == str.length()) {
                    return calendar.getTime();
                }
            } catch (IllegalArgumentException e) {
            }
            pos.setIndex(0);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Unable to parse the date: ");
        sb.append(str);
        throw new ParseException(sb.toString(), -1);
    }

    public static Date addYears(Date date, int amount) {
        return add(date, 1, amount);
    }

    public static Date addMonths(Date date, int amount) {
        return add(date, 2, amount);
    }

    public static Date addWeeks(Date date, int amount) {
        return add(date, 3, amount);
    }

    public static Date addDays(Date date, int amount) {
        return add(date, 5, amount);
    }

    public static Date addHours(Date date, int amount) {
        return add(date, 11, amount);
    }

    public static Date addMinutes(Date date, int amount) {
        return add(date, 12, amount);
    }

    public static Date addSeconds(Date date, int amount) {
        return add(date, 13, amount);
    }

    public static Date addMilliseconds(Date date, int amount) {
        return add(date, 14, amount);
    }

    private static Date add(Date date, int calendarField, int amount) {
        validateDateNotNull(date);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(calendarField, amount);
        return c.getTime();
    }

    public static Date setYears(Date date, int amount) {
        return set(date, 1, amount);
    }

    public static Date setMonths(Date date, int amount) {
        return set(date, 2, amount);
    }

    public static Date setDays(Date date, int amount) {
        return set(date, 5, amount);
    }

    public static Date setHours(Date date, int amount) {
        return set(date, 11, amount);
    }

    public static Date setMinutes(Date date, int amount) {
        return set(date, 12, amount);
    }

    public static Date setSeconds(Date date, int amount) {
        return set(date, 13, amount);
    }

    public static Date setMilliseconds(Date date, int amount) {
        return set(date, 14, amount);
    }

    private static Date set(Date date, int calendarField, int amount) {
        validateDateNotNull(date);
        Calendar c = Calendar.getInstance();
        c.setLenient(false);
        c.setTime(date);
        c.set(calendarField, amount);
        return c.getTime();
    }

    public static Calendar toCalendar(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c;
    }

    public static Calendar toCalendar(Date date, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz);
        c.setTime(date);
        return c;
    }

    public static Date round(Date date, int field) {
        validateDateNotNull(date);
        Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, ModifyType.ROUND);
        return gval.getTime();
    }

    public static Calendar round(Calendar date, int field) {
        if (date != null) {
            Calendar rounded = (Calendar) date.clone();
            modify(rounded, field, ModifyType.ROUND);
            return rounded;
        }
        throw new IllegalArgumentException("The date must not be null");
    }

    public static Date round(Object date, int field) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        } else if (date instanceof Date) {
            return round((Date) date, field);
        } else {
            if (date instanceof Calendar) {
                return round((Calendar) date, field).getTime();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Could not round ");
            sb.append(date);
            throw new ClassCastException(sb.toString());
        }
    }

    public static Date truncate(Date date, int field) {
        validateDateNotNull(date);
        Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, ModifyType.TRUNCATE);
        return gval.getTime();
    }

    public static Calendar truncate(Calendar date, int field) {
        if (date != null) {
            Calendar truncated = (Calendar) date.clone();
            modify(truncated, field, ModifyType.TRUNCATE);
            return truncated;
        }
        throw new IllegalArgumentException("The date must not be null");
    }

    public static Date truncate(Object date, int field) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        } else if (date instanceof Date) {
            return truncate((Date) date, field);
        } else {
            if (date instanceof Calendar) {
                return truncate((Calendar) date, field).getTime();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Could not truncate ");
            sb.append(date);
            throw new ClassCastException(sb.toString());
        }
    }

    public static Date ceiling(Date date, int field) {
        validateDateNotNull(date);
        Calendar gval = Calendar.getInstance();
        gval.setTime(date);
        modify(gval, field, ModifyType.CEILING);
        return gval.getTime();
    }

    public static Calendar ceiling(Calendar date, int field) {
        if (date != null) {
            Calendar ceiled = (Calendar) date.clone();
            modify(ceiled, field, ModifyType.CEILING);
            return ceiled;
        }
        throw new IllegalArgumentException("The date must not be null");
    }

    public static Date ceiling(Object date, int field) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        } else if (date instanceof Date) {
            return ceiling((Date) date, field);
        } else {
            if (date instanceof Calendar) {
                return ceiling((Calendar) date, field).getTime();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Could not find ceiling of for type: ");
            sb.append(date.getClass());
            throw new ClassCastException(sb.toString());
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:58:0x00e1, code lost:
        r22 = r3;
        r2 = 0;
        r3 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:59:0x00e7, code lost:
        if (r1 == 9) goto L_0x0114;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:61:0x00eb, code lost:
        if (r1 == 1001) goto L_0x00f1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:64:0x00f4, code lost:
        if (r16[0] != 5) goto L_0x0110;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:0x00f6, code lost:
        r4 = r0.get(5) - 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:66:0x00fe, code lost:
        if (r4 < 15) goto L_0x0104;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:67:0x0100, code lost:
        r2 = r4 - 15;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:68:0x0104, code lost:
        r2 = r4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:0x0106, code lost:
        if (r2 <= 7) goto L_0x010a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:71:0x0108, code lost:
        r4 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x010a, code lost:
        r4 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:73:0x010b, code lost:
        r10 = r4;
        r3 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:76:0x0119, code lost:
        if (r16[0] != 11) goto L_0x012e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:0x011b, code lost:
        r2 = r0.get(11);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:78:0x0121, code lost:
        if (r2 < 12) goto L_0x0125;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x0123, code lost:
        r2 = r2 - 12;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:81:0x0126, code lost:
        if (r2 < 6) goto L_0x012a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:0x0128, code lost:
        r11 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:83:0x012a, code lost:
        r11 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:84:0x012b, code lost:
        r10 = r11;
        r3 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:86:0x0130, code lost:
        if (r3 != false) goto L_0x0152;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:87:0x0132, code lost:
        r11 = r0.getActualMinimum(r16[0]);
        r2 = r0.get(r16[0]) - r11;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:88:0x014c, code lost:
        if (r2 <= ((r0.getActualMaximum(r16[0]) - r11) / 2)) goto L_0x0150;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:89:0x014e, code lost:
        r5 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:90:0x0150, code lost:
        r5 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:91:0x0151, code lost:
        r10 = r5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:92:0x0152, code lost:
        if (r2 == 0) goto L_0x0160;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:93:0x0154, code lost:
        r0.set(r16[0], r0.get(r16[0]) - r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:94:0x0160, code lost:
        r15 = r15 + 1;
        r5 = r18;
        r4 = r19;
        r2 = r25;
     */
    private static void modify(Calendar val, int field, ModifyType modType) {
        Calendar calendar = val;
        int i = field;
        ModifyType modifyType = modType;
        if (calendar.get(1) > 280000000) {
            throw new ArithmeticException("Calendar value too large for accurate calculations");
        } else if (i != 14) {
            Date date = val.getTime();
            long time = date.getTime();
            boolean done = false;
            int millisecs = calendar.get(14);
            if (ModifyType.TRUNCATE == modifyType || millisecs < 500) {
                time -= (long) millisecs;
            }
            if (i == 13) {
                done = true;
            }
            int seconds = calendar.get(13);
            if (!done && (ModifyType.TRUNCATE == modifyType || seconds < 30)) {
                time -= ((long) seconds) * 1000;
            }
            if (i == 12) {
                done = true;
            }
            int minutes = calendar.get(12);
            if (!done && (ModifyType.TRUNCATE == modifyType || minutes < 30)) {
                time -= ((long) minutes) * 60000;
            }
            if (date.getTime() != time) {
                date.setTime(time);
                calendar.setTime(date);
            }
            boolean roundUp = false;
            int[][] arr$ = fields;
            int len$ = arr$.length;
            int i$ = 0;
            while (i$ < len$) {
                int[] aField = arr$[i$];
                int[] arr$2 = aField;
                int len$2 = arr$2.length;
                int millisecs2 = millisecs;
                int i$2 = 0;
                while (true) {
                    Date date2 = date;
                    if (i$2 >= len$2) {
                        break;
                    } else if (arr$2[i$2] == i) {
                        int i2 = len$2;
                        if (modifyType == ModifyType.CEILING || (modifyType == ModifyType.ROUND && roundUp)) {
                            if (i == 1001) {
                                if (calendar.get(5) == 1) {
                                    calendar.add(5, 15);
                                } else {
                                    calendar.add(5, -15);
                                    calendar.add(2, 1);
                                }
                            } else if (i != 9) {
                                calendar.add(aField[0], 1);
                            } else if (calendar.get(11) == 0) {
                                calendar.add(11, 12);
                            } else {
                                calendar.add(11, -12);
                                calendar.add(5, 1);
                            }
                        }
                        return;
                    } else {
                        i$2++;
                        date = date2;
                        modifyType = modType;
                    }
                }
            }
            Date date3 = date;
            StringBuilder sb = new StringBuilder();
            sb.append("The field ");
            sb.append(i);
            sb.append(" is not supported");
            throw new IllegalArgumentException(sb.toString());
        }
    }

    public static Iterator<Calendar> iterator(Date focus, int rangeStyle) {
        validateDateNotNull(focus);
        Calendar gval = Calendar.getInstance();
        gval.setTime(focus);
        return iterator(gval, rangeStyle);
    }

    public static Iterator<Calendar> iterator(Calendar focus, int rangeStyle) {
        Calendar end;
        Calendar start;
        if (focus != null) {
            int startCutoff = 1;
            int endCutoff = 7;
            switch (rangeStyle) {
                case 1:
                case 2:
                case 3:
                case 4:
                    start = truncate(focus, 5);
                    end = truncate(focus, 5);
                    switch (rangeStyle) {
                        case 2:
                            startCutoff = 2;
                            endCutoff = 1;
                            break;
                        case 3:
                            startCutoff = focus.get(7);
                            endCutoff = startCutoff - 1;
                            break;
                        case 4:
                            startCutoff = focus.get(7) - 3;
                            endCutoff = focus.get(7) + 3;
                            break;
                    }
                case 5:
                case 6:
                    start = truncate(focus, 2);
                    end = (Calendar) start.clone();
                    end.add(2, 1);
                    end.add(5, -1);
                    if (rangeStyle == 6) {
                        startCutoff = 2;
                        endCutoff = 1;
                        break;
                    }
                    break;
                default:
                    StringBuilder sb = new StringBuilder();
                    sb.append("The range style ");
                    sb.append(rangeStyle);
                    sb.append(" is not valid.");
                    throw new IllegalArgumentException(sb.toString());
            }
            if (startCutoff < 1) {
                startCutoff += 7;
            }
            if (startCutoff > 7) {
                startCutoff -= 7;
            }
            if (endCutoff < 1) {
                endCutoff += 7;
            }
            if (endCutoff > 7) {
                endCutoff -= 7;
            }
            while (start.get(7) != startCutoff) {
                start.add(5, -1);
            }
            while (end.get(7) != endCutoff) {
                end.add(5, 1);
            }
            return new DateIterator(start, end);
        }
        throw new IllegalArgumentException("The date must not be null");
    }

    public static Iterator<?> iterator(Object focus, int rangeStyle) {
        if (focus == null) {
            throw new IllegalArgumentException("The date must not be null");
        } else if (focus instanceof Date) {
            return iterator((Date) focus, rangeStyle);
        } else {
            if (focus instanceof Calendar) {
                return iterator((Calendar) focus, rangeStyle);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Could not iterate based on ");
            sb.append(focus);
            throw new ClassCastException(sb.toString());
        }
    }

    public static long getFragmentInMilliseconds(Date date, int fragment) {
        return getFragment(date, fragment, TimeUnit.MILLISECONDS);
    }

    public static long getFragmentInSeconds(Date date, int fragment) {
        return getFragment(date, fragment, TimeUnit.SECONDS);
    }

    public static long getFragmentInMinutes(Date date, int fragment) {
        return getFragment(date, fragment, TimeUnit.MINUTES);
    }

    public static long getFragmentInHours(Date date, int fragment) {
        return getFragment(date, fragment, TimeUnit.HOURS);
    }

    public static long getFragmentInDays(Date date, int fragment) {
        return getFragment(date, fragment, TimeUnit.DAYS);
    }

    public static long getFragmentInMilliseconds(Calendar calendar, int fragment) {
        return getFragment(calendar, fragment, TimeUnit.MILLISECONDS);
    }

    public static long getFragmentInSeconds(Calendar calendar, int fragment) {
        return getFragment(calendar, fragment, TimeUnit.SECONDS);
    }

    public static long getFragmentInMinutes(Calendar calendar, int fragment) {
        return getFragment(calendar, fragment, TimeUnit.MINUTES);
    }

    public static long getFragmentInHours(Calendar calendar, int fragment) {
        return getFragment(calendar, fragment, TimeUnit.HOURS);
    }

    public static long getFragmentInDays(Calendar calendar, int fragment) {
        return getFragment(calendar, fragment, TimeUnit.DAYS);
    }

    private static long getFragment(Date date, int fragment, TimeUnit unit) {
        validateDateNotNull(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return getFragment(calendar, fragment, unit);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x005b, code lost:
        r0 = r0 + r8.convert((long) r6.get(12), java.util.concurrent.TimeUnit.MINUTES);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0069, code lost:
        r0 = r0 + r8.convert((long) r6.get(13), java.util.concurrent.TimeUnit.SECONDS);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:?, code lost:
        return r0 + r8.convert((long) r6.get(14), java.util.concurrent.TimeUnit.MILLISECONDS);
     */
    private static long getFragment(Calendar calendar, int fragment, TimeUnit unit) {
        if (calendar != null) {
            long result = 0;
            int offset = unit == TimeUnit.DAYS ? 0 : 1;
            switch (fragment) {
                case 1:
                    result = 0 + unit.convert((long) (calendar.get(6) - offset), TimeUnit.DAYS);
                    break;
                case 2:
                    result = 0 + unit.convert((long) (calendar.get(5) - offset), TimeUnit.DAYS);
                    break;
            }
            switch (fragment) {
                case 1:
                case 2:
                case 5:
                case 6:
                    result += unit.convert((long) calendar.get(11), TimeUnit.HOURS);
                    break;
                case 11:
                    break;
                case 12:
                    break;
                case 13:
                    break;
                case 14:
                    return result;
                default:
                    StringBuilder sb = new StringBuilder();
                    sb.append("The fragment ");
                    sb.append(fragment);
                    sb.append(" is not supported");
                    throw new IllegalArgumentException(sb.toString());
            }
        } else {
            throw new IllegalArgumentException("The date must not be null");
        }
    }

    public static boolean truncatedEquals(Calendar cal1, Calendar cal2, int field) {
        return truncatedCompareTo(cal1, cal2, field) == 0;
    }

    public static boolean truncatedEquals(Date date1, Date date2, int field) {
        return truncatedCompareTo(date1, date2, field) == 0;
    }

    public static int truncatedCompareTo(Calendar cal1, Calendar cal2, int field) {
        return truncate(cal1, field).compareTo(truncate(cal2, field));
    }

    public static int truncatedCompareTo(Date date1, Date date2, int field) {
        return truncate(date1, field).compareTo(truncate(date2, field));
    }

    private static void validateDateNotNull(Date date) {
        Validate.isTrue(date != null, "The date must not be null", new Object[0]);
    }
}
