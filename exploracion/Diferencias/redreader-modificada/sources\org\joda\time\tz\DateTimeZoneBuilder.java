package org.joda.time.tz;

import com.google.android.exoplayer2.extractor.ts.PsExtractor;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.chrono.ISOChronology;

public class DateTimeZoneBuilder {
    private final ArrayList<RuleSet> iRuleSets = new ArrayList<>(10);

    private static final class DSTZone extends DateTimeZone {
        private static final long serialVersionUID = 6941492635554961361L;
        final Recurrence iEndRecurrence;
        final int iStandardOffset;
        final Recurrence iStartRecurrence;

        static DSTZone readFrom(DataInput dataInput, String str) throws IOException {
            return new DSTZone(str, (int) DateTimeZoneBuilder.readMillis(dataInput), Recurrence.readFrom(dataInput), Recurrence.readFrom(dataInput));
        }

        DSTZone(String str, int i, Recurrence recurrence, Recurrence recurrence2) {
            super(str);
            this.iStandardOffset = i;
            this.iStartRecurrence = recurrence;
            this.iEndRecurrence = recurrence2;
        }

        public String getNameKey(long j) {
            return findMatchingRecurrence(j).getNameKey();
        }

        public int getOffset(long j) {
            return this.iStandardOffset + findMatchingRecurrence(j).getSaveMillis();
        }

        public int getStandardOffset(long j) {
            return this.iStandardOffset;
        }

        public boolean isFixed() {
            return false;
        }

        public long nextTransition(long j) {
            long j2;
            int i = this.iStandardOffset;
            Recurrence recurrence = this.iStartRecurrence;
            Recurrence recurrence2 = this.iEndRecurrence;
            try {
                j2 = recurrence.next(j, i, recurrence2.getSaveMillis());
                if (j > 0 && j2 < 0) {
                    j2 = j;
                }
            } catch (ArithmeticException | IllegalArgumentException e) {
                j2 = j;
            }
            try {
                long next = recurrence2.next(j, i, recurrence.getSaveMillis());
                if (j <= 0 || next >= 0) {
                    j = next;
                }
            } catch (ArithmeticException | IllegalArgumentException e2) {
            }
            return j2 > j ? j : j2;
        }

        public long previousTransition(long j) {
            long j2;
            long j3 = j + 1;
            int i = this.iStandardOffset;
            Recurrence recurrence = this.iStartRecurrence;
            Recurrence recurrence2 = this.iEndRecurrence;
            try {
                j2 = recurrence.previous(j3, i, recurrence2.getSaveMillis());
                if (j3 < 0 && j2 > 0) {
                    j2 = j3;
                }
            } catch (ArithmeticException | IllegalArgumentException e) {
                j2 = j3;
            }
            try {
                long previous = recurrence2.previous(j3, i, recurrence.getSaveMillis());
                if (j3 >= 0 || previous <= 0) {
                    j3 = previous;
                }
            } catch (ArithmeticException | IllegalArgumentException e2) {
            }
            if (j2 > j3) {
                j3 = j2;
            }
            return j3 - 1;
        }

        public boolean equals(Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DSTZone)) {
                return false;
            }
            DSTZone dSTZone = (DSTZone) obj;
            if (!getID().equals(dSTZone.getID()) || this.iStandardOffset != dSTZone.iStandardOffset || !this.iStartRecurrence.equals(dSTZone.iStartRecurrence) || !this.iEndRecurrence.equals(dSTZone.iEndRecurrence)) {
                z = false;
            }
            return z;
        }

        public void writeTo(DataOutput dataOutput) throws IOException {
            DateTimeZoneBuilder.writeMillis(dataOutput, (long) this.iStandardOffset);
            this.iStartRecurrence.writeTo(dataOutput);
            this.iEndRecurrence.writeTo(dataOutput);
        }

        private Recurrence findMatchingRecurrence(long j) {
            long j2;
            int i = this.iStandardOffset;
            Recurrence recurrence = this.iStartRecurrence;
            Recurrence recurrence2 = this.iEndRecurrence;
            try {
                j2 = recurrence.next(j, i, recurrence2.getSaveMillis());
            } catch (ArithmeticException | IllegalArgumentException e) {
                j2 = j;
            }
            try {
                j = recurrence2.next(j, i, recurrence.getSaveMillis());
            } catch (ArithmeticException | IllegalArgumentException e2) {
            }
            return j2 > j ? recurrence : recurrence2;
        }
    }

    private static final class OfYear {
        final boolean iAdvance;
        final int iDayOfMonth;
        final int iDayOfWeek;
        final int iMillisOfDay;
        final char iMode;
        final int iMonthOfYear;

        static OfYear readFrom(DataInput dataInput) throws IOException {
            OfYear ofYear = new OfYear((char) dataInput.readUnsignedByte(), dataInput.readUnsignedByte(), dataInput.readByte(), dataInput.readUnsignedByte(), dataInput.readBoolean(), (int) DateTimeZoneBuilder.readMillis(dataInput));
            return ofYear;
        }

        OfYear(char c, int i, int i2, int i3, boolean z, int i4) {
            if (c == 'u' || c == 'w' || c == 's') {
                this.iMode = c;
                this.iMonthOfYear = i;
                this.iDayOfMonth = i2;
                this.iDayOfWeek = i3;
                this.iAdvance = z;
                this.iMillisOfDay = i4;
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Unknown mode: ");
            sb.append(c);
            throw new IllegalArgumentException(sb.toString());
        }

        public long setInstant(int i, int i2, int i3) {
            char c = this.iMode;
            if (c == 'w') {
                i2 += i3;
            } else if (c != 's') {
                i2 = 0;
            }
            ISOChronology instanceUTC = ISOChronology.getInstanceUTC();
            long dayOfMonth = setDayOfMonth(instanceUTC, instanceUTC.millisOfDay().set(instanceUTC.monthOfYear().set(instanceUTC.year().set(0, i), this.iMonthOfYear), this.iMillisOfDay));
            if (this.iDayOfWeek != 0) {
                dayOfMonth = setDayOfWeek(instanceUTC, dayOfMonth);
            }
            return dayOfMonth - ((long) i2);
        }

        public long next(long j, int i, int i2) {
            char c = this.iMode;
            if (c == 'w') {
                i += i2;
            } else if (c != 's') {
                i = 0;
            }
            long j2 = (long) i;
            long j3 = j + j2;
            ISOChronology instanceUTC = ISOChronology.getInstanceUTC();
            long dayOfMonthNext = setDayOfMonthNext(instanceUTC, instanceUTC.millisOfDay().add(instanceUTC.millisOfDay().set(instanceUTC.monthOfYear().set(j3, this.iMonthOfYear), 0), this.iMillisOfDay));
            if (this.iDayOfWeek != 0) {
                dayOfMonthNext = setDayOfWeek(instanceUTC, dayOfMonthNext);
                if (dayOfMonthNext <= j3) {
                    dayOfMonthNext = setDayOfWeek(instanceUTC, setDayOfMonthNext(instanceUTC, instanceUTC.monthOfYear().set(instanceUTC.year().add(dayOfMonthNext, 1), this.iMonthOfYear)));
                }
            } else if (dayOfMonthNext <= j3) {
                dayOfMonthNext = setDayOfMonthNext(instanceUTC, instanceUTC.year().add(dayOfMonthNext, 1));
            }
            return dayOfMonthNext - j2;
        }

        public long previous(long j, int i, int i2) {
            char c = this.iMode;
            if (c == 'w') {
                i += i2;
            } else if (c != 's') {
                i = 0;
            }
            long j2 = (long) i;
            long j3 = j + j2;
            ISOChronology instanceUTC = ISOChronology.getInstanceUTC();
            long dayOfMonthPrevious = setDayOfMonthPrevious(instanceUTC, instanceUTC.millisOfDay().add(instanceUTC.millisOfDay().set(instanceUTC.monthOfYear().set(j3, this.iMonthOfYear), 0), this.iMillisOfDay));
            if (this.iDayOfWeek != 0) {
                dayOfMonthPrevious = setDayOfWeek(instanceUTC, dayOfMonthPrevious);
                if (dayOfMonthPrevious >= j3) {
                    dayOfMonthPrevious = setDayOfWeek(instanceUTC, setDayOfMonthPrevious(instanceUTC, instanceUTC.monthOfYear().set(instanceUTC.year().add(dayOfMonthPrevious, -1), this.iMonthOfYear)));
                }
            } else if (dayOfMonthPrevious >= j3) {
                dayOfMonthPrevious = setDayOfMonthPrevious(instanceUTC, instanceUTC.year().add(dayOfMonthPrevious, -1));
            }
            return dayOfMonthPrevious - j2;
        }

        public boolean equals(Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof OfYear)) {
                return false;
            }
            OfYear ofYear = (OfYear) obj;
            if (!(this.iMode == ofYear.iMode && this.iMonthOfYear == ofYear.iMonthOfYear && this.iDayOfMonth == ofYear.iDayOfMonth && this.iDayOfWeek == ofYear.iDayOfWeek && this.iAdvance == ofYear.iAdvance && this.iMillisOfDay == ofYear.iMillisOfDay)) {
                z = false;
            }
            return z;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[OfYear]\nMode: ");
            sb.append(this.iMode);
            sb.append(10);
            sb.append("MonthOfYear: ");
            sb.append(this.iMonthOfYear);
            sb.append(10);
            sb.append("DayOfMonth: ");
            sb.append(this.iDayOfMonth);
            sb.append(10);
            sb.append("DayOfWeek: ");
            sb.append(this.iDayOfWeek);
            sb.append(10);
            sb.append("AdvanceDayOfWeek: ");
            sb.append(this.iAdvance);
            sb.append(10);
            sb.append("MillisOfDay: ");
            sb.append(this.iMillisOfDay);
            sb.append(10);
            return sb.toString();
        }

        public void writeTo(DataOutput dataOutput) throws IOException {
            dataOutput.writeByte(this.iMode);
            dataOutput.writeByte(this.iMonthOfYear);
            dataOutput.writeByte(this.iDayOfMonth);
            dataOutput.writeByte(this.iDayOfWeek);
            dataOutput.writeBoolean(this.iAdvance);
            DateTimeZoneBuilder.writeMillis(dataOutput, (long) this.iMillisOfDay);
        }

        private long setDayOfMonthNext(Chronology chronology, long j) {
            try {
                return setDayOfMonth(chronology, j);
            } catch (IllegalArgumentException e) {
                if (this.iMonthOfYear == 2 && this.iDayOfMonth == 29) {
                    while (!chronology.year().isLeap(j)) {
                        j = chronology.year().add(j, 1);
                    }
                    return setDayOfMonth(chronology, j);
                }
                throw e;
            }
        }

        private long setDayOfMonthPrevious(Chronology chronology, long j) {
            try {
                return setDayOfMonth(chronology, j);
            } catch (IllegalArgumentException e) {
                if (this.iMonthOfYear == 2 && this.iDayOfMonth == 29) {
                    while (!chronology.year().isLeap(j)) {
                        j = chronology.year().add(j, -1);
                    }
                    return setDayOfMonth(chronology, j);
                }
                throw e;
            }
        }

        private long setDayOfMonth(Chronology chronology, long j) {
            if (this.iDayOfMonth >= 0) {
                return chronology.dayOfMonth().set(j, this.iDayOfMonth);
            }
            return chronology.dayOfMonth().add(chronology.monthOfYear().add(chronology.dayOfMonth().set(j, 1), 1), this.iDayOfMonth);
        }

        private long setDayOfWeek(Chronology chronology, long j) {
            int i = this.iDayOfWeek - chronology.dayOfWeek().get(j);
            if (i == 0) {
                return j;
            }
            if (this.iAdvance) {
                if (i < 0) {
                    i += 7;
                }
            } else if (i > 0) {
                i -= 7;
            }
            return chronology.dayOfWeek().add(j, i);
        }
    }

    private static final class PrecalculatedZone extends DateTimeZone {
        private static final long serialVersionUID = 7811976468055766265L;
        private final String[] iNameKeys;
        private final int[] iStandardOffsets;
        private final DSTZone iTailZone;
        private final long[] iTransitions;
        private final int[] iWallOffsets;

        static PrecalculatedZone readFrom(DataInput dataInput, String str) throws IOException {
            DSTZone dSTZone;
            int i;
            int readUnsignedShort = dataInput.readUnsignedShort();
            String[] strArr = new String[readUnsignedShort];
            for (int i2 = 0; i2 < readUnsignedShort; i2++) {
                strArr[i2] = dataInput.readUTF();
            }
            int readInt = dataInput.readInt();
            long[] jArr = new long[readInt];
            int[] iArr = new int[readInt];
            int[] iArr2 = new int[readInt];
            String[] strArr2 = new String[readInt];
            for (int i3 = 0; i3 < readInt; i3++) {
                jArr[i3] = DateTimeZoneBuilder.readMillis(dataInput);
                iArr[i3] = (int) DateTimeZoneBuilder.readMillis(dataInput);
                iArr2[i3] = (int) DateTimeZoneBuilder.readMillis(dataInput);
                if (readUnsignedShort < 256) {
                    try {
                        i = dataInput.readUnsignedByte();
                    } catch (ArrayIndexOutOfBoundsException e) {
                        throw new IOException("Invalid encoding");
                    }
                } else {
                    i = dataInput.readUnsignedShort();
                }
                strArr2[i3] = strArr[i];
            }
            if (dataInput.readBoolean()) {
                dSTZone = DSTZone.readFrom(dataInput, str);
            } else {
                dSTZone = null;
            }
            PrecalculatedZone precalculatedZone = new PrecalculatedZone(str, jArr, iArr, iArr2, strArr2, dSTZone);
            return precalculatedZone;
        }

        static PrecalculatedZone create(String str, boolean z, ArrayList<Transition> arrayList, DSTZone dSTZone) {
            DSTZone dSTZone2;
            String str2 = str;
            DSTZone dSTZone3 = dSTZone;
            int size = arrayList.size();
            if (size != 0) {
                long[] jArr = new long[size];
                int[] iArr = new int[size];
                int[] iArr2 = new int[size];
                String[] strArr = new String[size];
                int i = 0;
                Transition transition = null;
                int i2 = 0;
                while (i2 < size) {
                    Transition transition2 = (Transition) arrayList.get(i2);
                    if (transition2.isTransitionFrom(transition)) {
                        jArr[i2] = transition2.getMillis();
                        iArr[i2] = transition2.getWallOffset();
                        iArr2[i2] = transition2.getStandardOffset();
                        strArr[i2] = transition2.getNameKey();
                        i2++;
                        transition = transition2;
                    } else {
                        throw new IllegalArgumentException(str2);
                    }
                }
                String[] strArr2 = new String[5];
                String[][] zoneStrings = new DateFormatSymbols(Locale.ENGLISH).getZoneStrings();
                String[] strArr3 = strArr2;
                for (String[] strArr4 : zoneStrings) {
                    if (strArr4 != null && strArr4.length == 5 && str2.equals(strArr4[0])) {
                        strArr3 = strArr4;
                    }
                }
                ISOChronology instanceUTC = ISOChronology.getInstanceUTC();
                while (i < strArr.length - 1) {
                    String str3 = strArr[i];
                    int i3 = i + 1;
                    long j = (long) iArr[i];
                    long j2 = (long) iArr[i3];
                    long j3 = (long) iArr2[i];
                    int[] iArr3 = iArr;
                    int[] iArr4 = iArr2;
                    long j4 = (long) iArr2[i3];
                    long j5 = j;
                    String[] strArr5 = strArr;
                    String str4 = strArr[i3];
                    Period period = new Period(jArr[i], jArr[i3], PeriodType.yearMonthDay(), (Chronology) instanceUTC);
                    if (j5 != j2 && j3 == j4 && str3.equals(str4) && period.getYears() == 0 && period.getMonths() > 4 && period.getMonths() < 8 && str3.equals(strArr3[2]) && str3.equals(strArr3[4])) {
                        if (ZoneInfoLogger.verbose()) {
                            PrintStream printStream = System.out;
                            StringBuilder sb = new StringBuilder();
                            sb.append("Fixing duplicate name key - ");
                            sb.append(str4);
                            printStream.println(sb.toString());
                            PrintStream printStream2 = System.out;
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("     - ");
                            sb2.append(new DateTime(jArr[i], (Chronology) instanceUTC));
                            sb2.append(" - ");
                            sb2.append(new DateTime(jArr[i3], (Chronology) instanceUTC));
                            printStream2.println(sb2.toString());
                        }
                        if (j5 > j2) {
                            StringBuilder sb3 = new StringBuilder();
                            sb3.append(str3);
                            sb3.append("-Summer");
                            strArr5[i] = sb3.toString().intern();
                        } else if (j5 < j2) {
                            StringBuilder sb4 = new StringBuilder();
                            sb4.append(str4);
                            sb4.append("-Summer");
                            strArr5[i3] = sb4.toString().intern();
                            i = i3;
                        }
                    }
                    i++;
                    iArr = iArr3;
                    iArr2 = iArr4;
                    strArr = strArr5;
                    String str5 = str;
                    DSTZone dSTZone4 = dSTZone;
                }
                int[] iArr5 = iArr;
                int[] iArr6 = iArr2;
                String[] strArr6 = strArr;
                DSTZone dSTZone5 = dSTZone;
                if (dSTZone5 == null || !dSTZone5.iStartRecurrence.getNameKey().equals(dSTZone5.iEndRecurrence.getNameKey())) {
                    dSTZone2 = dSTZone5;
                } else {
                    if (ZoneInfoLogger.verbose()) {
                        PrintStream printStream3 = System.out;
                        StringBuilder sb5 = new StringBuilder();
                        sb5.append("Fixing duplicate recurrent name key - ");
                        sb5.append(dSTZone5.iStartRecurrence.getNameKey());
                        printStream3.println(sb5.toString());
                    }
                    if (dSTZone5.iStartRecurrence.getSaveMillis() > 0) {
                        dSTZone2 = new DSTZone(dSTZone.getID(), dSTZone5.iStandardOffset, dSTZone5.iStartRecurrence.renameAppend("-Summer"), dSTZone5.iEndRecurrence);
                    } else {
                        dSTZone2 = new DSTZone(dSTZone.getID(), dSTZone5.iStandardOffset, dSTZone5.iStartRecurrence, dSTZone5.iEndRecurrence.renameAppend("-Summer"));
                    }
                }
                PrecalculatedZone precalculatedZone = new PrecalculatedZone(z ? str : "", jArr, iArr5, iArr6, strArr6, dSTZone2);
                return precalculatedZone;
            }
            throw new IllegalArgumentException();
        }

        private PrecalculatedZone(String str, long[] jArr, int[] iArr, int[] iArr2, String[] strArr, DSTZone dSTZone) {
            super(str);
            this.iTransitions = jArr;
            this.iWallOffsets = iArr;
            this.iStandardOffsets = iArr2;
            this.iNameKeys = strArr;
            this.iTailZone = dSTZone;
        }

        public String getNameKey(long j) {
            long[] jArr = this.iTransitions;
            int binarySearch = Arrays.binarySearch(jArr, j);
            if (binarySearch >= 0) {
                return this.iNameKeys[binarySearch];
            }
            int i = binarySearch ^ -1;
            if (i >= jArr.length) {
                DSTZone dSTZone = this.iTailZone;
                if (dSTZone == null) {
                    return this.iNameKeys[i - 1];
                }
                return dSTZone.getNameKey(j);
            } else if (i > 0) {
                return this.iNameKeys[i - 1];
            } else {
                return "UTC";
            }
        }

        public int getOffset(long j) {
            long[] jArr = this.iTransitions;
            int binarySearch = Arrays.binarySearch(jArr, j);
            if (binarySearch >= 0) {
                return this.iWallOffsets[binarySearch];
            }
            int i = binarySearch ^ -1;
            if (i >= jArr.length) {
                DSTZone dSTZone = this.iTailZone;
                if (dSTZone == null) {
                    return this.iWallOffsets[i - 1];
                }
                return dSTZone.getOffset(j);
            } else if (i > 0) {
                return this.iWallOffsets[i - 1];
            } else {
                return 0;
            }
        }

        public int getStandardOffset(long j) {
            long[] jArr = this.iTransitions;
            int binarySearch = Arrays.binarySearch(jArr, j);
            if (binarySearch >= 0) {
                return this.iStandardOffsets[binarySearch];
            }
            int i = binarySearch ^ -1;
            if (i >= jArr.length) {
                DSTZone dSTZone = this.iTailZone;
                if (dSTZone == null) {
                    return this.iStandardOffsets[i - 1];
                }
                return dSTZone.getStandardOffset(j);
            } else if (i > 0) {
                return this.iStandardOffsets[i - 1];
            } else {
                return 0;
            }
        }

        public boolean isFixed() {
            return false;
        }

        public long nextTransition(long j) {
            long[] jArr = this.iTransitions;
            int binarySearch = Arrays.binarySearch(jArr, j);
            int i = binarySearch >= 0 ? binarySearch + 1 : binarySearch ^ -1;
            if (i < jArr.length) {
                return jArr[i];
            }
            if (this.iTailZone == null) {
                return j;
            }
            long j2 = jArr[jArr.length - 1];
            if (j < j2) {
                j = j2;
            }
            return this.iTailZone.nextTransition(j);
        }

        public long previousTransition(long j) {
            long[] jArr = this.iTransitions;
            int binarySearch = Arrays.binarySearch(jArr, j);
            if (binarySearch < 0) {
                int i = binarySearch ^ -1;
                if (i < jArr.length) {
                    if (i > 0) {
                        long j2 = jArr[i - 1];
                        if (j2 > Long.MIN_VALUE) {
                            return j2 - 1;
                        }
                    }
                    return j;
                }
                DSTZone dSTZone = this.iTailZone;
                if (dSTZone != null) {
                    long previousTransition = dSTZone.previousTransition(j);
                    if (previousTransition < j) {
                        return previousTransition;
                    }
                }
                long j3 = jArr[i - 1];
                if (j3 > Long.MIN_VALUE) {
                    return j3 - 1;
                }
                return j;
            } else if (j > Long.MIN_VALUE) {
                return j - 1;
            } else {
                return j;
            }
        }

        public boolean equals(Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PrecalculatedZone)) {
                return false;
            }
            PrecalculatedZone precalculatedZone = (PrecalculatedZone) obj;
            if (getID().equals(precalculatedZone.getID()) && Arrays.equals(this.iTransitions, precalculatedZone.iTransitions) && Arrays.equals(this.iNameKeys, precalculatedZone.iNameKeys) && Arrays.equals(this.iWallOffsets, precalculatedZone.iWallOffsets) && Arrays.equals(this.iStandardOffsets, precalculatedZone.iStandardOffsets)) {
                DSTZone dSTZone = this.iTailZone;
                if (dSTZone != null) {
                }
            }
            z = false;
            return z;
        }

        public void writeTo(DataOutput dataOutput) throws IOException {
            int length = this.iTransitions.length;
            HashSet<String> hashSet = new HashSet<>();
            boolean z = false;
            for (int i = 0; i < length; i++) {
                hashSet.add(this.iNameKeys[i]);
            }
            int size = hashSet.size();
            if (size <= 65535) {
                String[] strArr = new String[size];
                int i2 = 0;
                for (String str : hashSet) {
                    strArr[i2] = str;
                    i2++;
                }
                dataOutput.writeShort(size);
                for (int i3 = 0; i3 < size; i3++) {
                    dataOutput.writeUTF(strArr[i3]);
                }
                dataOutput.writeInt(length);
                for (int i4 = 0; i4 < length; i4++) {
                    DateTimeZoneBuilder.writeMillis(dataOutput, this.iTransitions[i4]);
                    DateTimeZoneBuilder.writeMillis(dataOutput, (long) this.iWallOffsets[i4]);
                    DateTimeZoneBuilder.writeMillis(dataOutput, (long) this.iStandardOffsets[i4]);
                    String str2 = this.iNameKeys[i4];
                    int i5 = 0;
                    while (true) {
                        if (i5 >= size) {
                            break;
                        } else if (!strArr[i5].equals(str2)) {
                            i5++;
                        } else if (size < 256) {
                            dataOutput.writeByte(i5);
                        } else {
                            dataOutput.writeShort(i5);
                        }
                    }
                }
                if (this.iTailZone != null) {
                    z = true;
                }
                dataOutput.writeBoolean(z);
                DSTZone dSTZone = this.iTailZone;
                if (dSTZone != null) {
                    dSTZone.writeTo(dataOutput);
                    return;
                }
                return;
            }
            throw new UnsupportedOperationException("String pool is too large");
        }

        public boolean isCachable() {
            if (this.iTailZone != null) {
                return true;
            }
            long[] jArr = this.iTransitions;
            if (jArr.length <= 1) {
                return false;
            }
            double d = 0.0d;
            int i = 0;
            for (int i2 = 1; i2 < jArr.length; i2++) {
                long j = jArr[i2] - jArr[i2 - 1];
                if (j < 63158400000L) {
                    double d2 = (double) j;
                    Double.isNaN(d2);
                    d += d2;
                    i++;
                }
            }
            if (i > 0) {
                double d3 = (double) i;
                Double.isNaN(d3);
                if ((d / d3) / 8.64E7d >= 25.0d) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class Recurrence {
        final String iNameKey;
        final OfYear iOfYear;
        final int iSaveMillis;

        static Recurrence readFrom(DataInput dataInput) throws IOException {
            return new Recurrence(OfYear.readFrom(dataInput), dataInput.readUTF(), (int) DateTimeZoneBuilder.readMillis(dataInput));
        }

        Recurrence(OfYear ofYear, String str, int i) {
            this.iOfYear = ofYear;
            this.iNameKey = str;
            this.iSaveMillis = i;
        }

        public OfYear getOfYear() {
            return this.iOfYear;
        }

        public long next(long j, int i, int i2) {
            return this.iOfYear.next(j, i, i2);
        }

        public long previous(long j, int i, int i2) {
            return this.iOfYear.previous(j, i, i2);
        }

        public String getNameKey() {
            return this.iNameKey;
        }

        public int getSaveMillis() {
            return this.iSaveMillis;
        }

        public boolean equals(Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Recurrence)) {
                return false;
            }
            Recurrence recurrence = (Recurrence) obj;
            if (this.iSaveMillis != recurrence.iSaveMillis || !this.iNameKey.equals(recurrence.iNameKey) || !this.iOfYear.equals(recurrence.iOfYear)) {
                z = false;
            }
            return z;
        }

        public void writeTo(DataOutput dataOutput) throws IOException {
            this.iOfYear.writeTo(dataOutput);
            dataOutput.writeUTF(this.iNameKey);
            DateTimeZoneBuilder.writeMillis(dataOutput, (long) this.iSaveMillis);
        }

        /* access modifiers changed from: 0000 */
        public Recurrence rename(String str) {
            return new Recurrence(this.iOfYear, str, this.iSaveMillis);
        }

        /* access modifiers changed from: 0000 */
        public Recurrence renameAppend(String str) {
            StringBuilder sb = new StringBuilder();
            sb.append(this.iNameKey);
            sb.append(str);
            return rename(sb.toString().intern());
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.iOfYear);
            sb.append(" named ");
            sb.append(this.iNameKey);
            sb.append(" at ");
            sb.append(this.iSaveMillis);
            return sb.toString();
        }
    }

    private static final class Rule {
        final int iFromYear;
        final Recurrence iRecurrence;
        final int iToYear;

        Rule(Recurrence recurrence, int i, int i2) {
            this.iRecurrence = recurrence;
            this.iFromYear = i;
            this.iToYear = i2;
        }

        public int getFromYear() {
            return this.iFromYear;
        }

        public int getToYear() {
            return this.iToYear;
        }

        public OfYear getOfYear() {
            return this.iRecurrence.getOfYear();
        }

        public String getNameKey() {
            return this.iRecurrence.getNameKey();
        }

        public int getSaveMillis() {
            return this.iRecurrence.getSaveMillis();
        }

        public long next(long j, int i, int i2) {
            int i3;
            ISOChronology instanceUTC = ISOChronology.getInstanceUTC();
            int i4 = i + i2;
            if (j == Long.MIN_VALUE) {
                i3 = Integer.MIN_VALUE;
            } else {
                i3 = instanceUTC.year().get(((long) i4) + j);
            }
            long next = this.iRecurrence.next(i3 < this.iFromYear ? (instanceUTC.year().set(0, this.iFromYear) - ((long) i4)) - 1 : j, i, i2);
            return (next <= j || instanceUTC.year().get(((long) i4) + next) <= this.iToYear) ? next : j;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.iFromYear);
            sb.append(" to ");
            sb.append(this.iToYear);
            sb.append(" using ");
            sb.append(this.iRecurrence);
            return sb.toString();
        }
    }

    private static final class RuleSet {
        private static final int YEAR_LIMIT = (ISOChronology.getInstanceUTC().year().get(DateTimeUtils.currentTimeMillis()) + 100);
        private String iInitialNameKey;
        private int iInitialSaveMillis;
        private ArrayList<Rule> iRules;
        private int iStandardOffset;
        private OfYear iUpperOfYear;
        private int iUpperYear;

        RuleSet() {
            this.iRules = new ArrayList<>(10);
            this.iUpperYear = Integer.MAX_VALUE;
        }

        RuleSet(RuleSet ruleSet) {
            this.iStandardOffset = ruleSet.iStandardOffset;
            this.iRules = new ArrayList<>(ruleSet.iRules);
            this.iInitialNameKey = ruleSet.iInitialNameKey;
            this.iInitialSaveMillis = ruleSet.iInitialSaveMillis;
            this.iUpperYear = ruleSet.iUpperYear;
            this.iUpperOfYear = ruleSet.iUpperOfYear;
        }

        public int getStandardOffset() {
            return this.iStandardOffset;
        }

        public void setStandardOffset(int i) {
            this.iStandardOffset = i;
        }

        public void setFixedSavings(String str, int i) {
            this.iInitialNameKey = str;
            this.iInitialSaveMillis = i;
        }

        public void addRule(Rule rule) {
            if (!this.iRules.contains(rule)) {
                this.iRules.add(rule);
            }
        }

        public void setUpperLimit(int i, OfYear ofYear) {
            this.iUpperYear = i;
            this.iUpperOfYear = ofYear;
        }

        public Transition firstTransition(long j) {
            String str = this.iInitialNameKey;
            if (str != null) {
                int i = this.iStandardOffset;
                Transition transition = new Transition(j, str, i + this.iInitialSaveMillis, i);
                return transition;
            }
            ArrayList<Rule> arrayList = new ArrayList<>(this.iRules);
            long j2 = Long.MIN_VALUE;
            int i2 = 0;
            Transition transition2 = null;
            while (true) {
                Transition nextTransition = nextTransition(j2, i2);
                if (nextTransition == null) {
                    break;
                }
                long millis = nextTransition.getMillis();
                if (millis == j) {
                    transition2 = new Transition(j, nextTransition);
                    break;
                } else if (millis > j) {
                    if (transition2 == null) {
                        Iterator it = arrayList.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            Rule rule = (Rule) it.next();
                            if (rule.getSaveMillis() == 0) {
                                transition2 = new Transition(j, rule, this.iStandardOffset);
                                break;
                            }
                        }
                    }
                    if (transition2 == null) {
                        String nameKey = nextTransition.getNameKey();
                        int i3 = this.iStandardOffset;
                        Transition transition3 = new Transition(j, nameKey, i3, i3);
                        transition2 = transition3;
                    }
                } else {
                    transition2 = new Transition(j, nextTransition);
                    i2 = nextTransition.getSaveMillis();
                    j2 = millis;
                }
            }
            this.iRules = arrayList;
            return transition2;
        }

        public Transition nextTransition(long j, int i) {
            ISOChronology instanceUTC = ISOChronology.getInstanceUTC();
            Iterator it = this.iRules.iterator();
            long j2 = Long.MAX_VALUE;
            Rule rule = null;
            while (it.hasNext()) {
                Rule rule2 = (Rule) it.next();
                long next = rule2.next(j, this.iStandardOffset, i);
                if (next <= j) {
                    it.remove();
                } else if (next <= j2) {
                    rule = rule2;
                    j2 = next;
                }
            }
            if (rule == null || instanceUTC.year().get(j2) >= YEAR_LIMIT) {
                return null;
            }
            int i2 = this.iUpperYear;
            if (i2 >= Integer.MAX_VALUE || j2 < this.iUpperOfYear.setInstant(i2, this.iStandardOffset, i)) {
                return new Transition(j2, rule, this.iStandardOffset);
            }
            return null;
        }

        public long getUpperLimit(int i) {
            int i2 = this.iUpperYear;
            if (i2 == Integer.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            return this.iUpperOfYear.setInstant(i2, this.iStandardOffset, i);
        }

        public DSTZone buildTailZone(String str) {
            if (this.iRules.size() == 2) {
                Rule rule = (Rule) this.iRules.get(0);
                Rule rule2 = (Rule) this.iRules.get(1);
                if (rule.getToYear() == Integer.MAX_VALUE && rule2.getToYear() == Integer.MAX_VALUE) {
                    return new DSTZone(str, this.iStandardOffset, rule.iRecurrence, rule2.iRecurrence);
                }
            }
            return null;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(this.iInitialNameKey);
            sb.append(" initial: ");
            sb.append(this.iInitialSaveMillis);
            sb.append(" std: ");
            sb.append(this.iStandardOffset);
            sb.append(" upper: ");
            sb.append(this.iUpperYear);
            sb.append(StringUtils.SPACE);
            sb.append(this.iUpperOfYear);
            sb.append(StringUtils.SPACE);
            sb.append(this.iRules);
            return sb.toString();
        }
    }

    private static final class Transition {
        private final long iMillis;
        private final String iNameKey;
        private final int iStandardOffset;
        private final int iWallOffset;

        Transition(long j, Transition transition) {
            this.iMillis = j;
            this.iNameKey = transition.iNameKey;
            this.iWallOffset = transition.iWallOffset;
            this.iStandardOffset = transition.iStandardOffset;
        }

        Transition(long j, Rule rule, int i) {
            this.iMillis = j;
            this.iNameKey = rule.getNameKey();
            this.iWallOffset = rule.getSaveMillis() + i;
            this.iStandardOffset = i;
        }

        Transition(long j, String str, int i, int i2) {
            this.iMillis = j;
            this.iNameKey = str;
            this.iWallOffset = i;
            this.iStandardOffset = i2;
        }

        public long getMillis() {
            return this.iMillis;
        }

        public String getNameKey() {
            return this.iNameKey;
        }

        public int getWallOffset() {
            return this.iWallOffset;
        }

        public int getStandardOffset() {
            return this.iStandardOffset;
        }

        public int getSaveMillis() {
            return this.iWallOffset - this.iStandardOffset;
        }

        public Transition withMillis(long j) {
            Transition transition = new Transition(j, this.iNameKey, this.iWallOffset, this.iStandardOffset);
            return transition;
        }

        public boolean isTransitionFrom(Transition transition) {
            boolean z = true;
            if (transition == null) {
                return true;
            }
            if (this.iMillis <= transition.iMillis || (this.iWallOffset == transition.iWallOffset && this.iStandardOffset == transition.iStandardOffset && this.iNameKey.equals(transition.iNameKey))) {
                z = false;
            }
            return z;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(new DateTime(this.iMillis, DateTimeZone.UTC));
            sb.append(StringUtils.SPACE);
            sb.append(this.iStandardOffset);
            sb.append(StringUtils.SPACE);
            sb.append(this.iWallOffset);
            return sb.toString();
        }
    }

    public static DateTimeZone readFrom(InputStream inputStream, String str) throws IOException {
        if (inputStream instanceof DataInput) {
            return readFrom((DataInput) inputStream, str);
        }
        return readFrom((DataInput) new DataInputStream(inputStream), str);
    }

    public static DateTimeZone readFrom(DataInput dataInput, String str) throws IOException {
        int readUnsignedByte = dataInput.readUnsignedByte();
        if (readUnsignedByte == 67) {
            return CachedDateTimeZone.forZone(PrecalculatedZone.readFrom(dataInput, str));
        }
        if (readUnsignedByte == 70) {
            DateTimeZone fixedDateTimeZone = new FixedDateTimeZone(str, dataInput.readUTF(), (int) readMillis(dataInput), (int) readMillis(dataInput));
            if (fixedDateTimeZone.equals(DateTimeZone.UTC)) {
                fixedDateTimeZone = DateTimeZone.UTC;
            }
            return fixedDateTimeZone;
        } else if (readUnsignedByte == 80) {
            return PrecalculatedZone.readFrom(dataInput, str);
        } else {
            throw new IOException("Invalid encoding");
        }
    }

    static void writeMillis(DataOutput dataOutput, long j) throws IOException {
        if (j % 1800000 == 0) {
            long j2 = j / 1800000;
            if (((j2 << 58) >> 58) == j2) {
                dataOutput.writeByte((int) (j2 & 63));
                return;
            }
        }
        if (j % 60000 == 0) {
            long j3 = j / 60000;
            if (((j3 << 34) >> 34) == j3) {
                dataOutput.writeInt(1073741824 | ((int) (j3 & 1073741823)));
                return;
            }
        }
        if (j % 1000 == 0) {
            long j4 = j / 1000;
            if (((j4 << 26) >> 26) == j4) {
                dataOutput.writeByte(((int) ((j4 >> 32) & 63)) | 128);
                dataOutput.writeInt((int) (-1 & j4));
                return;
            }
        }
        dataOutput.writeByte(j < 0 ? 255 : PsExtractor.AUDIO_STREAM);
        dataOutput.writeLong(j);
    }

    static long readMillis(DataInput dataInput) throws IOException {
        int readUnsignedByte = dataInput.readUnsignedByte();
        switch (readUnsignedByte >> 6) {
            case 1:
                return ((long) (dataInput.readUnsignedByte() | ((readUnsignedByte << 26) >> 2) | (dataInput.readUnsignedByte() << 16) | (dataInput.readUnsignedByte() << 8))) * 60000;
            case 2:
                return (((((long) readUnsignedByte) << 58) >> 26) | ((long) (dataInput.readUnsignedByte() << 24)) | ((long) (dataInput.readUnsignedByte() << 16)) | ((long) (dataInput.readUnsignedByte() << 8)) | ((long) dataInput.readUnsignedByte())) * 1000;
            case 3:
                return dataInput.readLong();
            default:
                return ((long) ((readUnsignedByte << 26) >> 26)) * 1800000;
        }
    }

    private static DateTimeZone buildFixedZone(String str, String str2, int i, int i2) {
        if (!"UTC".equals(str) || !str.equals(str2) || i != 0 || i2 != 0) {
            return new FixedDateTimeZone(str, str2, i, i2);
        }
        return DateTimeZone.UTC;
    }

    public DateTimeZoneBuilder addCutover(int i, char c, int i2, int i3, int i4, boolean z, int i5) {
        if (this.iRuleSets.size() > 0) {
            OfYear ofYear = new OfYear(c, i2, i3, i4, z, i5);
            ArrayList<RuleSet> arrayList = this.iRuleSets;
            ((RuleSet) arrayList.get(arrayList.size() - 1)).setUpperLimit(i, ofYear);
        }
        this.iRuleSets.add(new RuleSet());
        return this;
    }

    public DateTimeZoneBuilder setStandardOffset(int i) {
        getLastRuleSet().setStandardOffset(i);
        return this;
    }

    public DateTimeZoneBuilder setFixedSavings(String str, int i) {
        getLastRuleSet().setFixedSavings(str, i);
        return this;
    }

    public DateTimeZoneBuilder addRecurringSavings(String str, int i, int i2, int i3, char c, int i4, int i5, int i6, boolean z, int i7) {
        if (i2 <= i3) {
            OfYear ofYear = new OfYear(c, i4, i5, i6, z, i7);
            String str2 = str;
            int i8 = i;
            getLastRuleSet().addRule(new Rule(new Recurrence(ofYear, str, i), i2, i3));
        }
        return this;
    }

    private RuleSet getLastRuleSet() {
        if (this.iRuleSets.size() == 0) {
            addCutover(Integer.MIN_VALUE, 'w', 1, 1, 0, false, 0);
        }
        ArrayList<RuleSet> arrayList = this.iRuleSets;
        return (RuleSet) arrayList.get(arrayList.size() - 1);
    }

    public DateTimeZone toDateTimeZone(String str, boolean z) {
        if (str != null) {
            ArrayList arrayList = new ArrayList();
            long j = Long.MIN_VALUE;
            int size = this.iRuleSets.size();
            DSTZone dSTZone = null;
            for (int i = 0; i < size; i++) {
                RuleSet ruleSet = (RuleSet) this.iRuleSets.get(i);
                Transition firstTransition = ruleSet.firstTransition(j);
                if (firstTransition != null) {
                    addTransition(arrayList, firstTransition);
                    long millis = firstTransition.getMillis();
                    int saveMillis = firstTransition.getSaveMillis();
                    RuleSet ruleSet2 = new RuleSet(ruleSet);
                    while (true) {
                        Transition nextTransition = ruleSet2.nextTransition(millis, saveMillis);
                        if (nextTransition == null || (addTransition(arrayList, nextTransition) && dSTZone != null)) {
                            j = ruleSet2.getUpperLimit(saveMillis);
                        } else {
                            long millis2 = nextTransition.getMillis();
                            int saveMillis2 = nextTransition.getSaveMillis();
                            if (dSTZone == null && i == size - 1) {
                                dSTZone = ruleSet2.buildTailZone(str);
                                saveMillis = saveMillis2;
                                millis = millis2;
                            } else {
                                saveMillis = saveMillis2;
                                millis = millis2;
                            }
                        }
                    }
                    j = ruleSet2.getUpperLimit(saveMillis);
                }
            }
            if (arrayList.size() == 0) {
                if (dSTZone != null) {
                    return dSTZone;
                }
                return buildFixedZone(str, "UTC", 0, 0);
            } else if (arrayList.size() == 1 && dSTZone == null) {
                Transition transition = (Transition) arrayList.get(0);
                return buildFixedZone(str, transition.getNameKey(), transition.getWallOffset(), transition.getStandardOffset());
            } else {
                PrecalculatedZone create = PrecalculatedZone.create(str, z, arrayList, dSTZone);
                if (create.isCachable()) {
                    return CachedDateTimeZone.forZone(create);
                }
                return create;
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    private boolean addTransition(ArrayList<Transition> arrayList, Transition transition) {
        int size = arrayList.size();
        if (size == 0) {
            arrayList.add(transition);
            return true;
        }
        int i = size - 1;
        Transition transition2 = (Transition) arrayList.get(i);
        int i2 = 0;
        if (!transition.isTransitionFrom(transition2)) {
            return false;
        }
        if (size >= 2) {
            i2 = ((Transition) arrayList.get(size - 2)).getWallOffset();
        }
        int wallOffset = transition2.getWallOffset();
        if (transition.getMillis() + ((long) wallOffset) == transition2.getMillis() + ((long) i2)) {
            return addTransition(arrayList, transition.withMillis(((Transition) arrayList.remove(i)).getMillis()));
        }
        arrayList.add(transition);
        return true;
    }

    public void writeTo(String str, OutputStream outputStream) throws IOException {
        if (outputStream instanceof DataOutput) {
            writeTo(str, (DataOutput) outputStream);
            return;
        }
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        writeTo(str, (DataOutput) dataOutputStream);
        dataOutputStream.flush();
    }

    public void writeTo(String str, DataOutput dataOutput) throws IOException {
        DateTimeZone dateTimeZone = toDateTimeZone(str, false);
        if (dateTimeZone instanceof FixedDateTimeZone) {
            dataOutput.writeByte(70);
            dataOutput.writeUTF(dateTimeZone.getNameKey(0));
            writeMillis(dataOutput, (long) dateTimeZone.getOffset(0));
            writeMillis(dataOutput, (long) dateTimeZone.getStandardOffset(0));
            return;
        }
        if (dateTimeZone instanceof CachedDateTimeZone) {
            dataOutput.writeByte(67);
            dateTimeZone = ((CachedDateTimeZone) dateTimeZone).getUncachedZone();
        } else {
            dataOutput.writeByte(80);
        }
        ((PrecalculatedZone) dateTimeZone).writeTo(dataOutput);
    }
}
