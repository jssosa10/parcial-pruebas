package org.quantumbadger.redreader.common;

import android.content.Context;
import android.text.format.DateFormat;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.PeriodType;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.quantumbadger.redreader.R;

public class RRTime {
    private static final DateTimeFormatter dtFormatter12hr = DateTimeFormat.forPattern("yyyy-MM-dd h:mm a");
    private static final DateTimeFormatter dtFormatter24hr = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");

    public static long utcCurrentTimeMillis() {
        return DateTime.now(DateTimeZone.UTC).getMillis();
    }

    public static String formatDateTime(long utc_ms, Context context) {
        DateTime localDateTime = new DateTime(utc_ms).withZone(DateTimeZone.getDefault());
        if (DateFormat.is24HourFormat(context)) {
            return dtFormatter24hr.print((ReadableInstant) localDateTime);
        }
        return dtFormatter12hr.print((ReadableInstant) localDateTime);
    }

    public static String formatDurationFrom(Context context, long startTime) {
        Context context2 = context;
        String str = StringUtils.SPACE;
        String str2 = ",";
        String str3 = ", ";
        long endTime = utcCurrentTimeMillis();
        String duration = new PeriodFormatterBuilder().appendYears().appendSuffix(StringUtils.SPACE).appendSuffix(context2.getString(R.string.time_year), context2.getString(R.string.time_years)).appendSeparator(", ").appendMonths().appendSuffix(StringUtils.SPACE).appendSuffix(context2.getString(R.string.time_month), context2.getString(R.string.time_months)).appendSeparator(", ").appendDays().appendSuffix(StringUtils.SPACE).appendSuffix(context2.getString(R.string.time_day), context2.getString(R.string.time_days)).appendSeparator(", ").appendHours().appendSuffix(StringUtils.SPACE).appendSuffix(context2.getString(R.string.time_hour), context2.getString(R.string.time_hours)).appendSeparator(", ").appendMinutes().appendSuffix(StringUtils.SPACE).appendSuffix(context2.getString(R.string.time_min), context2.getString(R.string.time_mins)).appendSeparator(", ").appendSeconds().appendSuffix(StringUtils.SPACE).appendSuffix(context2.getString(R.string.time_sec), context2.getString(R.string.time_secs)).appendSeparator(", ").appendMillis().appendSuffix(StringUtils.SPACE).appendSuffix(context2.getString(R.string.time_ms)).toFormatter().print(new Duration(startTime, endTime).toPeriodTo(new DateTime(endTime).withZone(DateTimeZone.getDefault())).normalizedStandard(PeriodType.yearMonthDayTime()));
        List<String> parts = Arrays.asList(duration.split(","));
        if (parts.size() >= 2) {
            StringBuilder sb = new StringBuilder();
            sb.append((String) parts.get(0));
            sb.append(",");
            sb.append((String) parts.get(1));
            duration = sb.toString();
        }
        return String.format(context2.getString(R.string.time_ago), new Object[]{duration});
    }

    public static long since(long timestamp) {
        return utcCurrentTimeMillis() - timestamp;
    }
}
