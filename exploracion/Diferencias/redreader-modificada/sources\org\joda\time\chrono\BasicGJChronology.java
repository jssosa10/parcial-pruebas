package org.joda.time.chrono;

import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.Chronology;

abstract class BasicGJChronology extends BasicChronology {
    private static final long FEB_29 = 5097600000L;
    private static final int[] MAX_DAYS_PER_MONTH_ARRAY = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final long[] MAX_TOTAL_MILLIS_BY_MONTH_ARRAY = new long[12];
    private static final int[] MIN_DAYS_PER_MONTH_ARRAY = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static final long[] MIN_TOTAL_MILLIS_BY_MONTH_ARRAY = new long[12];
    private static final long serialVersionUID = 538276888268L;

    static {
        long j = 0;
        int i = 0;
        long j2 = 0;
        while (i < 11) {
            j += ((long) MIN_DAYS_PER_MONTH_ARRAY[i]) * DateUtils.MILLIS_PER_DAY;
            int i2 = i + 1;
            MIN_TOTAL_MILLIS_BY_MONTH_ARRAY[i2] = j;
            j2 += ((long) MAX_DAYS_PER_MONTH_ARRAY[i]) * DateUtils.MILLIS_PER_DAY;
            MAX_TOTAL_MILLIS_BY_MONTH_ARRAY[i2] = j2;
            i = i2;
        }
    }

    BasicGJChronology(Chronology chronology, Object obj, int i) {
        super(chronology, obj, i);
    }

    /* access modifiers changed from: 0000 */
    public boolean isLeapDay(long j) {
        return dayOfMonth().get(j) == 29 && monthOfYear().isLeap(j);
    }

    /* access modifiers changed from: 0000 */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x003d, code lost:
        if (r14 < 12825000) goto L_0x007f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x004e, code lost:
        if (r14 < 20587500) goto L_0x0094;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x005a, code lost:
        if (r14 < 28265625) goto L_0x00a7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x007d, code lost:
        if (r14 < 12740625) goto L_0x007f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x0092, code lost:
        if (r14 < 20503125) goto L_0x0094;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:0x00a5, code lost:
        if (r14 < 28181250) goto L_0x00a7;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:55:?, code lost:
        return 12;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:59:?, code lost:
        return 5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:60:?, code lost:
        return 6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:62:?, code lost:
        return 8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:63:?, code lost:
        return 9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:?, code lost:
        return 11;
     */
    public int getMonthOfYear(long j, int i) {
        int yearMillis = (int) ((j - getYearMillis(i)) >> 10);
        if (isLeapYear(i)) {
            if (yearMillis < 15356250) {
                if (yearMillis >= 7678125) {
                    if (yearMillis >= 10209375) {
                    }
                    return 4;
                } else if (yearMillis >= 2615625) {
                    if (yearMillis < 5062500) {
                        return 2;
                    }
                    return 3;
                }
            } else if (yearMillis < 23118750) {
                if (yearMillis >= 17971875) {
                }
                return 7;
            } else {
                if (yearMillis >= 25734375) {
                }
                return 10;
            }
        } else if (yearMillis < 15271875) {
            if (yearMillis >= 7593750) {
                if (yearMillis >= 10125000) {
                }
                return 4;
            } else if (yearMillis >= 2615625) {
                if (yearMillis < 4978125) {
                    return 2;
                }
                return 3;
            }
        } else if (yearMillis < 23034375) {
            if (yearMillis >= 17887500) {
            }
            return 7;
        } else {
            if (yearMillis >= 25650000) {
            }
            return 10;
        }
        return 1;
    }

    /* access modifiers changed from: 0000 */
    public int getDaysInYearMonth(int i, int i2) {
        if (isLeapYear(i)) {
            return MAX_DAYS_PER_MONTH_ARRAY[i2 - 1];
        }
        return MIN_DAYS_PER_MONTH_ARRAY[i2 - 1];
    }

    /* access modifiers changed from: 0000 */
    public int getDaysInMonthMax(int i) {
        return MAX_DAYS_PER_MONTH_ARRAY[i - 1];
    }

    /* access modifiers changed from: 0000 */
    public int getDaysInMonthMaxForSet(long j, int i) {
        if (i > 28 || i < 1) {
            return getDaysInMonthMax(j);
        }
        return 28;
    }

    /* access modifiers changed from: 0000 */
    public long getTotalMillisByYearMonth(int i, int i2) {
        if (isLeapYear(i)) {
            return MAX_TOTAL_MILLIS_BY_MONTH_ARRAY[i2 - 1];
        }
        return MIN_TOTAL_MILLIS_BY_MONTH_ARRAY[i2 - 1];
    }

    /* access modifiers changed from: 0000 */
    public long getYearDifference(long j, long j2) {
        int year = getYear(j);
        int year2 = getYear(j2);
        long yearMillis = j - getYearMillis(year);
        long yearMillis2 = j2 - getYearMillis(year2);
        if (yearMillis2 >= FEB_29) {
            if (isLeapYear(year2)) {
                if (!isLeapYear(year)) {
                    yearMillis2 -= DateUtils.MILLIS_PER_DAY;
                }
            } else if (yearMillis >= FEB_29 && isLeapYear(year)) {
                yearMillis -= DateUtils.MILLIS_PER_DAY;
            }
        }
        int i = year - year2;
        if (yearMillis < yearMillis2) {
            i--;
        }
        return (long) i;
    }

    /* access modifiers changed from: 0000 */
    public long setYear(long j, int i) {
        int year = getYear(j);
        int dayOfYear = getDayOfYear(j, year);
        int millisOfDay = getMillisOfDay(j);
        if (dayOfYear > 59) {
            if (isLeapYear(year)) {
                if (!isLeapYear(i)) {
                    dayOfYear--;
                }
            } else if (isLeapYear(i)) {
                dayOfYear++;
            }
        }
        return getYearMonthDayMillis(i, 1, dayOfYear) + ((long) millisOfDay);
    }
}
