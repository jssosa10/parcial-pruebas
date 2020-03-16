package org.apache.commons.lang3.time;

import java.util.Date;
import java.util.TimeZone;

class GmtTimeZone extends TimeZone {
    private static final int HOURS_PER_DAY = 24;
    private static final int MILLISECONDS_PER_MINUTE = 60000;
    private static final int MINUTES_PER_HOUR = 60;
    static final long serialVersionUID = 1;
    private final int offset;
    private final String zoneId;

    GmtTimeZone(boolean negate, int hours, int minutes) {
        if (hours >= 24) {
            StringBuilder sb = new StringBuilder();
            sb.append(hours);
            sb.append(" hours out of range");
            throw new IllegalArgumentException(sb.toString());
        } else if (minutes < 60) {
            int milliseconds = ((hours * 60) + minutes) * 60000;
            this.offset = negate ? -milliseconds : milliseconds;
            StringBuilder sb2 = new StringBuilder(9);
            sb2.append(TimeZones.GMT_ID);
            sb2.append(negate ? '-' : '+');
            StringBuilder twoDigits = twoDigits(sb2, hours);
            twoDigits.append(':');
            this.zoneId = twoDigits(twoDigits, minutes).toString();
        } else {
            StringBuilder sb3 = new StringBuilder();
            sb3.append(minutes);
            sb3.append(" minutes out of range");
            throw new IllegalArgumentException(sb3.toString());
        }
    }

    private static StringBuilder twoDigits(StringBuilder sb, int n) {
        sb.append((char) ((n / 10) + 48));
        sb.append((char) ((n % 10) + 48));
        return sb;
    }

    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        return this.offset;
    }

    public void setRawOffset(int offsetMillis) {
        throw new UnsupportedOperationException();
    }

    public int getRawOffset() {
        return this.offset;
    }

    public String getID() {
        return this.zoneId;
    }

    public boolean useDaylightTime() {
        return false;
    }

    public boolean inDaylightTime(Date date) {
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[GmtTimeZone id=\"");
        sb.append(this.zoneId);
        sb.append("\",offset=");
        sb.append(this.offset);
        sb.append(']');
        return sb.toString();
    }

    public int hashCode() {
        return this.offset;
    }

    public boolean equals(Object other) {
        boolean z = false;
        if (!(other instanceof GmtTimeZone)) {
            return false;
        }
        if (this.zoneId == ((GmtTimeZone) other).zoneId) {
            z = true;
        }
        return z;
    }
}
