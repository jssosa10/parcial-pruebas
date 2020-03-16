package org.joda.time.tz;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.TimeZones;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.MutableDateTime;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.chrono.LenientChronology;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class ZoneInfoCompiler {
    static Chronology cLenientISO;
    static DateTimeOfYear cStartOfYear;
    private List<String> iBackLinks = new ArrayList();
    private List<String> iGoodLinks = new ArrayList();
    private Map<String, RuleSet> iRuleSets = new HashMap();
    private List<Zone> iZones = new ArrayList();

    static class DateTimeOfYear {
        public final boolean iAdvanceDayOfWeek;
        public final int iDayOfMonth;
        public final int iDayOfWeek;
        public final int iMillisOfDay;
        public final int iMonthOfYear;
        public final char iZoneChar;

        DateTimeOfYear() {
            this.iMonthOfYear = 1;
            this.iDayOfMonth = 1;
            this.iDayOfWeek = 0;
            this.iAdvanceDayOfWeek = false;
            this.iMillisOfDay = 0;
            this.iZoneChar = 'w';
        }

        DateTimeOfYear(StringTokenizer stringTokenizer) {
            int i;
            boolean z;
            int i2;
            int i3;
            int i4 = 0;
            char c = 'w';
            if (stringTokenizer.hasMoreTokens()) {
                i3 = ZoneInfoCompiler.parseMonth(stringTokenizer.nextToken());
                if (stringTokenizer.hasMoreTokens()) {
                    String nextToken = stringTokenizer.nextToken();
                    if (nextToken.startsWith("last")) {
                        i2 = ZoneInfoCompiler.parseDayOfWeek(nextToken.substring(4));
                        z = false;
                        i = -1;
                    } else {
                        try {
                            i = Integer.parseInt(nextToken);
                            i2 = 0;
                            z = false;
                        } catch (NumberFormatException e) {
                            int indexOf = nextToken.indexOf(">=");
                            if (indexOf > 0) {
                                i = Integer.parseInt(nextToken.substring(indexOf + 2));
                                i2 = ZoneInfoCompiler.parseDayOfWeek(nextToken.substring(0, indexOf));
                                z = true;
                            } else {
                                int indexOf2 = nextToken.indexOf("<=");
                                if (indexOf2 > 0) {
                                    i = Integer.parseInt(nextToken.substring(indexOf2 + 2));
                                    i2 = ZoneInfoCompiler.parseDayOfWeek(nextToken.substring(0, indexOf2));
                                    z = false;
                                } else {
                                    throw new IllegalArgumentException(nextToken);
                                }
                            }
                        }
                    }
                    if (stringTokenizer.hasMoreTokens()) {
                        String nextToken2 = stringTokenizer.nextToken();
                        c = ZoneInfoCompiler.parseZoneChar(nextToken2.charAt(nextToken2.length() - 1));
                        if (!nextToken2.equals("24:00")) {
                            i4 = ZoneInfoCompiler.parseTime(nextToken2);
                        } else if (i3 == 12 && i == 31) {
                            i4 = ZoneInfoCompiler.parseTime("23:59:59.999");
                        } else {
                            LocalDate plusMonths = i == -1 ? new LocalDate(2001, i3, 1).plusMonths(1) : new LocalDate(2001, i3, i).plusDays(1);
                            boolean z2 = (i == -1 || i2 == 0) ? false : true;
                            int monthOfYear = plusMonths.getMonthOfYear();
                            int dayOfMonth = plusMonths.getDayOfMonth();
                            if (i2 != 0) {
                                i2 = (((i2 - 1) + 1) % 7) + 1;
                            }
                            i = dayOfMonth;
                            int i5 = monthOfYear;
                            z = z2;
                            i3 = i5;
                        }
                    }
                } else {
                    i2 = 0;
                    z = false;
                    i = 1;
                }
            } else {
                i3 = 1;
                i2 = 0;
                z = false;
                i = 1;
            }
            this.iMonthOfYear = i3;
            this.iDayOfMonth = i;
            this.iDayOfWeek = i2;
            this.iAdvanceDayOfWeek = z;
            this.iMillisOfDay = i4;
            this.iZoneChar = c;
        }

        public void addRecurring(DateTimeZoneBuilder dateTimeZoneBuilder, String str, int i, int i2, int i3) {
            dateTimeZoneBuilder.addRecurringSavings(str, i, i2, i3, this.iZoneChar, this.iMonthOfYear, this.iDayOfMonth, this.iDayOfWeek, this.iAdvanceDayOfWeek, this.iMillisOfDay);
        }

        public void addCutover(DateTimeZoneBuilder dateTimeZoneBuilder, int i) {
            dateTimeZoneBuilder.addCutover(i, this.iZoneChar, this.iMonthOfYear, this.iDayOfMonth, this.iDayOfWeek, this.iAdvanceDayOfWeek, this.iMillisOfDay);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("MonthOfYear: ");
            sb.append(this.iMonthOfYear);
            sb.append(StringUtils.LF);
            sb.append("DayOfMonth: ");
            sb.append(this.iDayOfMonth);
            sb.append(StringUtils.LF);
            sb.append("DayOfWeek: ");
            sb.append(this.iDayOfWeek);
            sb.append(StringUtils.LF);
            sb.append("AdvanceDayOfWeek: ");
            sb.append(this.iAdvanceDayOfWeek);
            sb.append(StringUtils.LF);
            sb.append("MillisOfDay: ");
            sb.append(this.iMillisOfDay);
            sb.append(StringUtils.LF);
            sb.append("ZoneChar: ");
            sb.append(this.iZoneChar);
            sb.append(StringUtils.LF);
            return sb.toString();
        }
    }

    private static class Rule {
        public final DateTimeOfYear iDateTimeOfYear;
        public final int iFromYear;
        public final String iLetterS;
        public final String iName;
        public final int iSaveMillis;
        public final int iToYear;
        public final String iType;

        Rule(StringTokenizer stringTokenizer) {
            if (stringTokenizer.countTokens() >= 6) {
                this.iName = stringTokenizer.nextToken().intern();
                this.iFromYear = ZoneInfoCompiler.parseYear(stringTokenizer.nextToken(), 0);
                this.iToYear = ZoneInfoCompiler.parseYear(stringTokenizer.nextToken(), this.iFromYear);
                if (this.iToYear >= this.iFromYear) {
                    this.iType = ZoneInfoCompiler.parseOptional(stringTokenizer.nextToken());
                    this.iDateTimeOfYear = new DateTimeOfYear(stringTokenizer);
                    this.iSaveMillis = ZoneInfoCompiler.parseTime(stringTokenizer.nextToken());
                    this.iLetterS = ZoneInfoCompiler.parseOptional(stringTokenizer.nextToken());
                    return;
                }
                throw new IllegalArgumentException();
            }
            throw new IllegalArgumentException("Attempting to create a Rule from an incomplete tokenizer");
        }

        public void addRecurring(DateTimeZoneBuilder dateTimeZoneBuilder, String str) {
            DateTimeZoneBuilder dateTimeZoneBuilder2 = dateTimeZoneBuilder;
            this.iDateTimeOfYear.addRecurring(dateTimeZoneBuilder2, formatName(str), this.iSaveMillis, this.iFromYear, this.iToYear);
        }

        private String formatName(String str) {
            String str2;
            int indexOf = str.indexOf(47);
            if (indexOf <= 0) {
                int indexOf2 = str.indexOf("%s");
                if (indexOf2 < 0) {
                    return str;
                }
                String substring = str.substring(0, indexOf2);
                String substring2 = str.substring(indexOf2 + 2);
                if (this.iLetterS == null) {
                    str2 = substring.concat(substring2);
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(substring);
                    sb.append(this.iLetterS);
                    sb.append(substring2);
                    str2 = sb.toString();
                }
                return str2.intern();
            } else if (this.iSaveMillis == 0) {
                return str.substring(0, indexOf).intern();
            } else {
                return str.substring(indexOf + 1).intern();
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[Rule]\nName: ");
            sb.append(this.iName);
            sb.append(StringUtils.LF);
            sb.append("FromYear: ");
            sb.append(this.iFromYear);
            sb.append(StringUtils.LF);
            sb.append("ToYear: ");
            sb.append(this.iToYear);
            sb.append(StringUtils.LF);
            sb.append("Type: ");
            sb.append(this.iType);
            sb.append(StringUtils.LF);
            sb.append(this.iDateTimeOfYear);
            sb.append("SaveMillis: ");
            sb.append(this.iSaveMillis);
            sb.append(StringUtils.LF);
            sb.append("LetterS: ");
            sb.append(this.iLetterS);
            sb.append(StringUtils.LF);
            return sb.toString();
        }
    }

    private static class RuleSet {
        private List<Rule> iRules = new ArrayList();

        RuleSet(Rule rule) {
            this.iRules.add(rule);
        }

        /* access modifiers changed from: 0000 */
        public void addRule(Rule rule) {
            if (rule.iName.equals(((Rule) this.iRules.get(0)).iName)) {
                this.iRules.add(rule);
                return;
            }
            throw new IllegalArgumentException("Rule name mismatch");
        }

        public void addRecurring(DateTimeZoneBuilder dateTimeZoneBuilder, String str) {
            for (int i = 0; i < this.iRules.size(); i++) {
                ((Rule) this.iRules.get(i)).addRecurring(dateTimeZoneBuilder, str);
            }
        }
    }

    private static class Zone {
        public final String iFormat;
        public final String iName;
        private Zone iNext;
        public final int iOffsetMillis;
        public final String iRules;
        public final DateTimeOfYear iUntilDateTimeOfYear;
        public final int iUntilYear;

        Zone(StringTokenizer stringTokenizer) {
            this(stringTokenizer.nextToken(), stringTokenizer);
        }

        private Zone(String str, StringTokenizer stringTokenizer) {
            int i;
            this.iName = str.intern();
            this.iOffsetMillis = ZoneInfoCompiler.parseTime(stringTokenizer.nextToken());
            this.iRules = ZoneInfoCompiler.parseOptional(stringTokenizer.nextToken());
            this.iFormat = stringTokenizer.nextToken().intern();
            DateTimeOfYear startOfYear = ZoneInfoCompiler.getStartOfYear();
            if (stringTokenizer.hasMoreTokens()) {
                i = Integer.parseInt(stringTokenizer.nextToken());
                if (stringTokenizer.hasMoreTokens()) {
                    startOfYear = new DateTimeOfYear(stringTokenizer);
                }
            } else {
                i = Integer.MAX_VALUE;
            }
            this.iUntilYear = i;
            this.iUntilDateTimeOfYear = startOfYear;
        }

        /* access modifiers changed from: 0000 */
        public void chain(StringTokenizer stringTokenizer) {
            Zone zone = this.iNext;
            if (zone != null) {
                zone.chain(stringTokenizer);
            } else {
                this.iNext = new Zone(this.iName, stringTokenizer);
            }
        }

        public void addToBuilder(DateTimeZoneBuilder dateTimeZoneBuilder, Map<String, RuleSet> map) {
            addToBuilder(this, dateTimeZoneBuilder, map);
        }

        private static void addToBuilder(Zone zone, DateTimeZoneBuilder dateTimeZoneBuilder, Map<String, RuleSet> map) {
            while (zone != null) {
                dateTimeZoneBuilder.setStandardOffset(zone.iOffsetMillis);
                String str = zone.iRules;
                if (str == null) {
                    dateTimeZoneBuilder.setFixedSavings(zone.iFormat, 0);
                } else {
                    try {
                        dateTimeZoneBuilder.setFixedSavings(zone.iFormat, ZoneInfoCompiler.parseTime(str));
                    } catch (Exception e) {
                        RuleSet ruleSet = (RuleSet) map.get(zone.iRules);
                        if (ruleSet != null) {
                            ruleSet.addRecurring(dateTimeZoneBuilder, zone.iFormat);
                        } else {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Rules not found: ");
                            sb.append(zone.iRules);
                            throw new IllegalArgumentException(sb.toString());
                        }
                    }
                }
                int i = zone.iUntilYear;
                if (i != Integer.MAX_VALUE) {
                    zone.iUntilDateTimeOfYear.addCutover(dateTimeZoneBuilder, i);
                    zone = zone.iNext;
                } else {
                    return;
                }
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[Zone]\nName: ");
            sb.append(this.iName);
            sb.append(StringUtils.LF);
            sb.append("OffsetMillis: ");
            sb.append(this.iOffsetMillis);
            sb.append(StringUtils.LF);
            sb.append("Rules: ");
            sb.append(this.iRules);
            sb.append(StringUtils.LF);
            sb.append("Format: ");
            sb.append(this.iFormat);
            sb.append(StringUtils.LF);
            sb.append("UntilYear: ");
            sb.append(this.iUntilYear);
            sb.append(StringUtils.LF);
            sb.append(this.iUntilDateTimeOfYear);
            String sb2 = sb.toString();
            if (this.iNext == null) {
                return sb2;
            }
            StringBuilder sb3 = new StringBuilder();
            sb3.append(sb2);
            sb3.append("...\n");
            sb3.append(this.iNext.toString());
            return sb3.toString();
        }
    }

    public static void main(String[] strArr) throws Exception {
        if (strArr.length == 0) {
            printUsage();
            return;
        }
        int i = 0;
        File file = null;
        File file2 = null;
        int i2 = 0;
        boolean z = false;
        while (true) {
            if (i2 >= strArr.length) {
                break;
            }
            try {
                if ("-src".equals(strArr[i2])) {
                    i2++;
                    file = new File(strArr[i2]);
                } else if ("-dst".equals(strArr[i2])) {
                    i2++;
                    file2 = new File(strArr[i2]);
                } else if ("-verbose".equals(strArr[i2])) {
                    z = true;
                } else if ("-?".equals(strArr[i2])) {
                    printUsage();
                    return;
                }
                i2++;
            } catch (IndexOutOfBoundsException e) {
                printUsage();
                return;
            }
        }
        if (i2 >= strArr.length) {
            printUsage();
            return;
        }
        File[] fileArr = new File[(strArr.length - i2)];
        while (i2 < strArr.length) {
            fileArr[i] = file == null ? new File(strArr[i2]) : new File(file, strArr[i2]);
            i2++;
            i++;
        }
        ZoneInfoLogger.set(z);
        new ZoneInfoCompiler().compile(file2, fileArr);
    }

    private static void printUsage() {
        System.out.println("Usage: java org.joda.time.tz.ZoneInfoCompiler <options> <source files>");
        System.out.println("where possible options include:");
        System.out.println("  -src <directory>    Specify where to read source files");
        System.out.println("  -dst <directory>    Specify where to write generated files");
        System.out.println("  -verbose            Output verbosely (default false)");
    }

    static DateTimeOfYear getStartOfYear() {
        if (cStartOfYear == null) {
            cStartOfYear = new DateTimeOfYear();
        }
        return cStartOfYear;
    }

    static Chronology getLenientISOChronology() {
        if (cLenientISO == null) {
            cLenientISO = LenientChronology.getInstance(ISOChronology.getInstanceUTC());
        }
        return cLenientISO;
    }

    static void writeZoneInfoMap(DataOutputStream dataOutputStream, Map<String, DateTimeZone> map) throws IOException {
        HashMap hashMap = new HashMap(map.size());
        TreeMap treeMap = new TreeMap();
        short s = 0;
        for (Entry entry : map.entrySet()) {
            String str = (String) entry.getKey();
            if (!hashMap.containsKey(str)) {
                Short valueOf = Short.valueOf(s);
                hashMap.put(str, valueOf);
                treeMap.put(valueOf, str);
                s = (short) (s + 1);
                if (s == 0) {
                    throw new InternalError("Too many time zone ids");
                }
            }
            String id = ((DateTimeZone) entry.getValue()).getID();
            if (!hashMap.containsKey(id)) {
                Short valueOf2 = Short.valueOf(s);
                hashMap.put(id, valueOf2);
                treeMap.put(valueOf2, id);
                s = (short) (s + 1);
                if (s == 0) {
                    throw new InternalError("Too many time zone ids");
                }
            }
        }
        dataOutputStream.writeShort(treeMap.size());
        for (String writeUTF : treeMap.values()) {
            dataOutputStream.writeUTF(writeUTF);
        }
        dataOutputStream.writeShort(map.size());
        for (Entry entry2 : map.entrySet()) {
            dataOutputStream.writeShort(((Short) hashMap.get((String) entry2.getKey())).shortValue());
            dataOutputStream.writeShort(((Short) hashMap.get(((DateTimeZone) entry2.getValue()).getID())).shortValue());
        }
    }

    static int parseYear(String str, int i) {
        String lowerCase = str.toLowerCase(Locale.ENGLISH);
        if (lowerCase.equals("minimum") || lowerCase.equals("min")) {
            return Integer.MIN_VALUE;
        }
        if (lowerCase.equals("maximum") || lowerCase.equals("max")) {
            return Integer.MAX_VALUE;
        }
        if (lowerCase.equals("only")) {
            return i;
        }
        return Integer.parseInt(lowerCase);
    }

    static int parseMonth(String str) {
        DateTimeField monthOfYear = ISOChronology.getInstanceUTC().monthOfYear();
        return monthOfYear.get(monthOfYear.set(0, str, Locale.ENGLISH));
    }

    static int parseDayOfWeek(String str) {
        DateTimeField dayOfWeek = ISOChronology.getInstanceUTC().dayOfWeek();
        return dayOfWeek.get(dayOfWeek.set(0, str, Locale.ENGLISH));
    }

    static String parseOptional(String str) {
        if (str.equals("-")) {
            return null;
        }
        return str;
    }

    static int parseTime(String str) {
        int i;
        DateTimeFormatter hourMinuteSecondFraction = ISODateTimeFormat.hourMinuteSecondFraction();
        MutableDateTime mutableDateTime = new MutableDateTime(0, getLenientISOChronology());
        if (str.startsWith("-")) {
            i = 1;
        } else {
            i = 0;
        }
        if (hourMinuteSecondFraction.parseInto(mutableDateTime, str, i) != (i ^ -1)) {
            int millis = (int) mutableDateTime.getMillis();
            if (i == 1) {
                return -millis;
            }
            return millis;
        }
        throw new IllegalArgumentException(str);
    }

    static char parseZoneChar(char c) {
        if (c != 'G') {
            if (c != 'S') {
                if (!(c == 'U' || c == 'Z' || c == 'g')) {
                    if (c != 's') {
                        if (!(c == 'u' || c == 'z')) {
                            return 'w';
                        }
                    }
                }
            }
            return 's';
        }
        return 'u';
    }

    /* JADX WARNING: Code restructure failed: missing block: B:24:0x0102, code lost:
        r5 = r5 - 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0104, code lost:
        if (r5 < 0) goto L_0x0164;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0106, code lost:
        r6 = r0.previousTransition(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x010c, code lost:
        if (r6 == r1) goto L_0x0164;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0110, code lost:
        if (r6 >= r3) goto L_0x0113;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0113, code lost:
        r1 = ((java.lang.Long) r13.get(r5)).longValue() - 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0123, code lost:
        if (r1 == r6) goto L_0x0162;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x0125, code lost:
        r3 = java.lang.System.out;
        r4 = new java.lang.StringBuilder();
        r4.append("*r* Error in ");
        r4.append(r19.getID());
        r4.append(org.apache.commons.lang3.StringUtils.SPACE);
        r4.append(new org.joda.time.DateTime(r6, (org.joda.time.Chronology) org.joda.time.chrono.ISOChronology.getInstanceUTC()));
        r4.append(" != ");
        r4.append(new org.joda.time.DateTime(r1, (org.joda.time.Chronology) org.joda.time.chrono.ISOChronology.getInstanceUTC()));
        r3.println(r4.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x0161, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x0162, code lost:
        r1 = r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x0165, code lost:
        return true;
     */
    static boolean test(String str, DateTimeZone dateTimeZone) {
        long nextTransition;
        String nameKey;
        DateTimeZone dateTimeZone2 = dateTimeZone;
        if (!str.equals(dateTimeZone.getID())) {
            return true;
        }
        long j = ISOChronology.getInstanceUTC().year().set(0, 1850);
        long j2 = ISOChronology.getInstanceUTC().year().set(0, 2050);
        int offset = dateTimeZone2.getOffset(j);
        int standardOffset = dateTimeZone2.getStandardOffset(j);
        String nameKey2 = dateTimeZone2.getNameKey(j);
        ArrayList arrayList = new ArrayList();
        while (true) {
            nextTransition = dateTimeZone2.nextTransition(j);
            if (nextTransition == j || nextTransition > j2) {
                long j3 = ISOChronology.getInstanceUTC().year().set(0, 2050);
                long j4 = ISOChronology.getInstanceUTC().year().set(0, 1850);
                int size = arrayList.size();
            } else {
                int offset2 = dateTimeZone2.getOffset(nextTransition);
                int standardOffset2 = dateTimeZone2.getStandardOffset(nextTransition);
                nameKey = dateTimeZone2.getNameKey(nextTransition);
                if (offset == offset2 && standardOffset == standardOffset2 && nameKey2.equals(nameKey)) {
                    PrintStream printStream = System.out;
                    StringBuilder sb = new StringBuilder();
                    sb.append("*d* Error in ");
                    sb.append(dateTimeZone.getID());
                    sb.append(StringUtils.SPACE);
                    sb.append(new DateTime(nextTransition, (Chronology) ISOChronology.getInstanceUTC()));
                    printStream.println(sb.toString());
                    return false;
                } else if (nameKey == null || (nameKey.length() < 3 && !"??".equals(nameKey))) {
                    PrintStream printStream2 = System.out;
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("*s* Error in ");
                    sb2.append(dateTimeZone.getID());
                    sb2.append(StringUtils.SPACE);
                    sb2.append(new DateTime(nextTransition, (Chronology) ISOChronology.getInstanceUTC()));
                    sb2.append(", nameKey=");
                    sb2.append(nameKey);
                    printStream2.println(sb2.toString());
                } else {
                    arrayList.add(Long.valueOf(nextTransition));
                    nameKey2 = nameKey;
                    offset = offset2;
                    j = nextTransition;
                }
            }
        }
        PrintStream printStream22 = System.out;
        StringBuilder sb22 = new StringBuilder();
        sb22.append("*s* Error in ");
        sb22.append(dateTimeZone.getID());
        sb22.append(StringUtils.SPACE);
        sb22.append(new DateTime(nextTransition, (Chronology) ISOChronology.getInstanceUTC()));
        sb22.append(", nameKey=");
        sb22.append(nameKey);
        printStream22.println(sb22.toString());
        return false;
    }

    /* JADX WARNING: Removed duplicated region for block: B:16:0x0030  */
    public Map<String, DateTimeZone> compile(File file, File[] fileArr) throws IOException {
        if (fileArr != null) {
            int i = 0;
            while (i < fileArr.length) {
                BufferedReader bufferedReader = null;
                try {
                    BufferedReader bufferedReader2 = new BufferedReader(new FileReader(fileArr[i]));
                    try {
                        parseDataFile(bufferedReader2, "backward".equals(fileArr[i].getName()));
                        bufferedReader2.close();
                        i++;
                    } catch (Throwable th) {
                        th = th;
                        bufferedReader = bufferedReader2;
                        if (bufferedReader != null) {
                            bufferedReader.close();
                        }
                        throw th;
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferedReader != null) {
                    }
                    throw th;
                }
            }
        }
        if (file != null) {
            if (!file.exists() && !file.mkdirs()) {
                StringBuilder sb = new StringBuilder();
                sb.append("Destination directory doesn't exist and cannot be created: ");
                sb.append(file);
                throw new IOException(sb.toString());
            } else if (!file.isDirectory()) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Destination is not a directory: ");
                sb2.append(file);
                throw new IOException(sb2.toString());
            }
        }
        TreeMap treeMap = new TreeMap();
        TreeMap treeMap2 = new TreeMap();
        System.out.println("Writing zoneinfo files");
        for (int i2 = 0; i2 < this.iZones.size(); i2++) {
            Zone zone = (Zone) this.iZones.get(i2);
            DateTimeZoneBuilder dateTimeZoneBuilder = new DateTimeZoneBuilder();
            zone.addToBuilder(dateTimeZoneBuilder, this.iRuleSets);
            DateTimeZone dateTimeZone = dateTimeZoneBuilder.toDateTimeZone(zone.iName, true);
            if (test(dateTimeZone.getID(), dateTimeZone)) {
                treeMap.put(dateTimeZone.getID(), dateTimeZone);
                treeMap2.put(dateTimeZone.getID(), zone);
                if (file != null) {
                    writeZone(file, dateTimeZoneBuilder, dateTimeZone);
                }
            }
        }
        for (int i3 = 0; i3 < this.iGoodLinks.size(); i3 += 2) {
            String str = (String) this.iGoodLinks.get(i3);
            String str2 = (String) this.iGoodLinks.get(i3 + 1);
            Zone zone2 = (Zone) treeMap2.get(str);
            if (zone2 == null) {
                PrintStream printStream = System.out;
                StringBuilder sb3 = new StringBuilder();
                sb3.append("Cannot find source zone '");
                sb3.append(str);
                sb3.append("' to link alias '");
                sb3.append(str2);
                sb3.append("' to");
                printStream.println(sb3.toString());
            } else {
                DateTimeZoneBuilder dateTimeZoneBuilder2 = new DateTimeZoneBuilder();
                zone2.addToBuilder(dateTimeZoneBuilder2, this.iRuleSets);
                DateTimeZone dateTimeZone2 = dateTimeZoneBuilder2.toDateTimeZone(str2, true);
                if (test(dateTimeZone2.getID(), dateTimeZone2)) {
                    treeMap.put(dateTimeZone2.getID(), dateTimeZone2);
                    if (file != null) {
                        writeZone(file, dateTimeZoneBuilder2, dateTimeZone2);
                    }
                }
                treeMap.put(dateTimeZone2.getID(), dateTimeZone2);
                if (ZoneInfoLogger.verbose()) {
                    PrintStream printStream2 = System.out;
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append("Good link: ");
                    sb4.append(str2);
                    sb4.append(" -> ");
                    sb4.append(str);
                    sb4.append(" revived");
                    printStream2.println(sb4.toString());
                }
            }
        }
        for (int i4 = 0; i4 < 2; i4++) {
            for (int i5 = 0; i5 < this.iBackLinks.size(); i5 += 2) {
                String str3 = (String) this.iBackLinks.get(i5);
                String str4 = (String) this.iBackLinks.get(i5 + 1);
                DateTimeZone dateTimeZone3 = (DateTimeZone) treeMap.get(str3);
                if (dateTimeZone3 != null) {
                    treeMap.put(str4, dateTimeZone3);
                    if (ZoneInfoLogger.verbose()) {
                        PrintStream printStream3 = System.out;
                        StringBuilder sb5 = new StringBuilder();
                        sb5.append("Back link: ");
                        sb5.append(str4);
                        sb5.append(" -> ");
                        sb5.append(dateTimeZone3.getID());
                        printStream3.println(sb5.toString());
                    }
                } else if (i4 > 0) {
                    PrintStream printStream4 = System.out;
                    StringBuilder sb6 = new StringBuilder();
                    sb6.append("Cannot find time zone '");
                    sb6.append(str3);
                    sb6.append("' to link alias '");
                    sb6.append(str4);
                    sb6.append("' to");
                    printStream4.println(sb6.toString());
                }
            }
        }
        if (file != null) {
            System.out.println("Writing ZoneInfoMap");
            File file2 = new File(file, "ZoneInfoMap");
            if (!file2.getParentFile().exists()) {
                file2.getParentFile().mkdirs();
            }
            DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file2));
            try {
                TreeMap treeMap3 = new TreeMap(String.CASE_INSENSITIVE_ORDER);
                treeMap3.putAll(treeMap);
                writeZoneInfoMap(dataOutputStream, treeMap3);
            } finally {
                dataOutputStream.close();
            }
        }
        return treeMap;
    }

    /* JADX INFO: finally extract failed */
    private void writeZone(File file, DateTimeZoneBuilder dateTimeZoneBuilder, DateTimeZone dateTimeZone) throws IOException {
        if (ZoneInfoLogger.verbose()) {
            PrintStream printStream = System.out;
            StringBuilder sb = new StringBuilder();
            sb.append("Writing ");
            sb.append(dateTimeZone.getID());
            printStream.println(sb.toString());
        }
        File file2 = new File(file, dateTimeZone.getID());
        if (!file2.getParentFile().exists()) {
            file2.getParentFile().mkdirs();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file2);
        try {
            dateTimeZoneBuilder.writeTo(dateTimeZone.getID(), (OutputStream) fileOutputStream);
            fileOutputStream.close();
            FileInputStream fileInputStream = new FileInputStream(file2);
            DateTimeZone readFrom = DateTimeZoneBuilder.readFrom((InputStream) fileInputStream, dateTimeZone.getID());
            fileInputStream.close();
            if (!dateTimeZone.equals(readFrom)) {
                PrintStream printStream2 = System.out;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("*e* Error in ");
                sb2.append(dateTimeZone.getID());
                sb2.append(": Didn't read properly from file");
                printStream2.println(sb2.toString());
            }
        } catch (Throwable th) {
            fileOutputStream.close();
            throw th;
        }
    }

    public void parseDataFile(BufferedReader bufferedReader, boolean z) throws IOException {
        Zone zone = null;
        while (true) {
            String readLine = bufferedReader.readLine();
            if (readLine != null) {
                String trim = readLine.trim();
                if (!(trim.length() == 0 || trim.charAt(0) == '#')) {
                    int indexOf = readLine.indexOf(35);
                    if (indexOf >= 0) {
                        readLine = readLine.substring(0, indexOf);
                    }
                    StringTokenizer stringTokenizer = new StringTokenizer(readLine, " \t");
                    if (!Character.isWhitespace(readLine.charAt(0)) || !stringTokenizer.hasMoreTokens()) {
                        if (zone != null) {
                            this.iZones.add(zone);
                        }
                        if (stringTokenizer.hasMoreTokens()) {
                            String nextToken = stringTokenizer.nextToken();
                            if (nextToken.equalsIgnoreCase("Rule")) {
                                Rule rule = new Rule(stringTokenizer);
                                RuleSet ruleSet = (RuleSet) this.iRuleSets.get(rule.iName);
                                if (ruleSet == null) {
                                    this.iRuleSets.put(rule.iName, new RuleSet(rule));
                                } else {
                                    ruleSet.addRule(rule);
                                }
                            } else if (nextToken.equalsIgnoreCase("Zone")) {
                                if (stringTokenizer.countTokens() >= 4) {
                                    zone = new Zone(stringTokenizer);
                                } else {
                                    throw new IllegalArgumentException("Attempting to create a Zone from an incomplete tokenizer");
                                }
                            } else if (nextToken.equalsIgnoreCase("Link")) {
                                String nextToken2 = stringTokenizer.nextToken();
                                String nextToken3 = stringTokenizer.nextToken();
                                if (z || nextToken3.equals("US/Pacific-New") || nextToken3.startsWith("Etc/") || nextToken3.equals(TimeZones.GMT_ID)) {
                                    this.iBackLinks.add(nextToken2);
                                    this.iBackLinks.add(nextToken3);
                                } else {
                                    this.iGoodLinks.add(nextToken2);
                                    this.iGoodLinks.add(nextToken3);
                                }
                            } else {
                                PrintStream printStream = System.out;
                                StringBuilder sb = new StringBuilder();
                                sb.append("Unknown line: ");
                                sb.append(readLine);
                                printStream.println(sb.toString());
                            }
                        }
                        zone = null;
                    } else if (zone != null) {
                        zone.chain(stringTokenizer);
                    }
                }
            } else if (zone != null) {
                this.iZones.add(zone);
                return;
            } else {
                return;
            }
        }
    }
}
