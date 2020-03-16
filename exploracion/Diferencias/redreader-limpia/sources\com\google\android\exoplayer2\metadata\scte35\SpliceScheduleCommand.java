package com.google.android.exoplayer2.metadata.scte35;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.ParsableByteArray;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SpliceScheduleCommand extends SpliceCommand {
    public static final Creator<SpliceScheduleCommand> CREATOR = new Creator<SpliceScheduleCommand>() {
        public SpliceScheduleCommand createFromParcel(Parcel in) {
            return new SpliceScheduleCommand(in);
        }

        public SpliceScheduleCommand[] newArray(int size) {
            return new SpliceScheduleCommand[size];
        }
    };
    public final List<Event> events;

    public static final class ComponentSplice {
        public final int componentTag;
        public final long utcSpliceTime;

        private ComponentSplice(int componentTag2, long utcSpliceTime2) {
            this.componentTag = componentTag2;
            this.utcSpliceTime = utcSpliceTime2;
        }

        /* access modifiers changed from: private */
        public static ComponentSplice createFromParcel(Parcel in) {
            return new ComponentSplice(in.readInt(), in.readLong());
        }

        /* access modifiers changed from: private */
        public void writeToParcel(Parcel dest) {
            dest.writeInt(this.componentTag);
            dest.writeLong(this.utcSpliceTime);
        }
    }

    public static final class Event {
        public final boolean autoReturn;
        public final int availNum;
        public final int availsExpected;
        public final long breakDurationUs;
        public final List<ComponentSplice> componentSpliceList;
        public final boolean outOfNetworkIndicator;
        public final boolean programSpliceFlag;
        public final boolean spliceEventCancelIndicator;
        public final long spliceEventId;
        public final int uniqueProgramId;
        public final long utcSpliceTime;

        private Event(long spliceEventId2, boolean spliceEventCancelIndicator2, boolean outOfNetworkIndicator2, boolean programSpliceFlag2, List<ComponentSplice> componentSpliceList2, long utcSpliceTime2, boolean autoReturn2, long breakDurationUs2, int uniqueProgramId2, int availNum2, int availsExpected2) {
            this.spliceEventId = spliceEventId2;
            this.spliceEventCancelIndicator = spliceEventCancelIndicator2;
            this.outOfNetworkIndicator = outOfNetworkIndicator2;
            this.programSpliceFlag = programSpliceFlag2;
            this.componentSpliceList = Collections.unmodifiableList(componentSpliceList2);
            this.utcSpliceTime = utcSpliceTime2;
            this.autoReturn = autoReturn2;
            this.breakDurationUs = breakDurationUs2;
            this.uniqueProgramId = uniqueProgramId2;
            this.availNum = availNum2;
            this.availsExpected = availsExpected2;
        }

        private Event(Parcel in) {
            this.spliceEventId = in.readLong();
            boolean z = false;
            this.spliceEventCancelIndicator = in.readByte() == 1;
            this.outOfNetworkIndicator = in.readByte() == 1;
            this.programSpliceFlag = in.readByte() == 1;
            int componentSpliceListLength = in.readInt();
            ArrayList<ComponentSplice> componentSpliceList2 = new ArrayList<>(componentSpliceListLength);
            for (int i = 0; i < componentSpliceListLength; i++) {
                componentSpliceList2.add(ComponentSplice.createFromParcel(in));
            }
            this.componentSpliceList = Collections.unmodifiableList(componentSpliceList2);
            this.utcSpliceTime = in.readLong();
            if (in.readByte() == 1) {
                z = true;
            }
            this.autoReturn = z;
            this.breakDurationUs = in.readLong();
            this.uniqueProgramId = in.readInt();
            this.availNum = in.readInt();
            this.availsExpected = in.readInt();
        }

        /* access modifiers changed from: private */
        public static Event parseFromSection(ParsableByteArray sectionData) {
            int availsExpected2;
            int availNum2;
            int uniqueProgramId2;
            long breakDurationUs2;
            long utcSpliceTime2;
            boolean programSpliceFlag2;
            boolean outOfNetworkIndicator2;
            boolean autoReturn2;
            ArrayList arrayList;
            long spliceEventId2 = sectionData.readUnsignedInt();
            boolean spliceEventCancelIndicator2 = (sectionData.readUnsignedByte() & 128) != 0;
            long utcSpliceTime3 = C.TIME_UNSET;
            ArrayList arrayList2 = new ArrayList();
            boolean autoReturn3 = false;
            long breakDurationUs3 = C.TIME_UNSET;
            if (!spliceEventCancelIndicator2) {
                int headerByte = sectionData.readUnsignedByte();
                boolean outOfNetworkIndicator3 = (headerByte & 128) != 0;
                boolean programSpliceFlag3 = (headerByte & 64) != 0;
                boolean durationFlag = (headerByte & 32) != 0;
                if (programSpliceFlag3) {
                    utcSpliceTime3 = sectionData.readUnsignedInt();
                }
                if (!programSpliceFlag3) {
                    int componentCount = sectionData.readUnsignedByte();
                    arrayList2 = new ArrayList(componentCount);
                    int i = 0;
                    while (i < componentCount) {
                        boolean outOfNetworkIndicator4 = outOfNetworkIndicator3;
                        boolean programSpliceFlag4 = programSpliceFlag3;
                        long utcSpliceTime4 = utcSpliceTime3;
                        int componentCount2 = componentCount;
                        arrayList2.add(new ComponentSplice(sectionData.readUnsignedByte(), sectionData.readUnsignedInt()));
                        i++;
                        outOfNetworkIndicator3 = outOfNetworkIndicator4;
                        programSpliceFlag3 = programSpliceFlag4;
                        utcSpliceTime3 = utcSpliceTime4;
                        componentCount = componentCount2;
                    }
                    outOfNetworkIndicator2 = outOfNetworkIndicator3;
                    int i2 = componentCount;
                    programSpliceFlag2 = programSpliceFlag3;
                    utcSpliceTime2 = utcSpliceTime3;
                } else {
                    outOfNetworkIndicator2 = outOfNetworkIndicator3;
                    programSpliceFlag2 = programSpliceFlag3;
                    utcSpliceTime2 = utcSpliceTime3;
                }
                if (durationFlag) {
                    long firstByte = (long) sectionData.readUnsignedByte();
                    autoReturn3 = (128 & firstByte) != 0;
                    breakDurationUs3 = (1000 * (((1 & firstByte) << 32) | sectionData.readUnsignedInt())) / 90;
                }
                uniqueProgramId2 = sectionData.readUnsignedShort();
                availNum2 = sectionData.readUnsignedByte();
                availsExpected2 = sectionData.readUnsignedByte();
                arrayList = arrayList2;
                autoReturn2 = autoReturn3;
                breakDurationUs2 = breakDurationUs3;
            } else {
                outOfNetworkIndicator2 = false;
                programSpliceFlag2 = false;
                utcSpliceTime2 = -9223372036854775807L;
                arrayList = arrayList2;
                uniqueProgramId2 = 0;
                availNum2 = 0;
                availsExpected2 = 0;
                autoReturn2 = false;
                breakDurationUs2 = -9223372036854775807L;
            }
            Event event = new Event(spliceEventId2, spliceEventCancelIndicator2, outOfNetworkIndicator2, programSpliceFlag2, arrayList, utcSpliceTime2, autoReturn2, breakDurationUs2, uniqueProgramId2, availNum2, availsExpected2);
            return event;
        }

        /* access modifiers changed from: private */
        public void writeToParcel(Parcel dest) {
            dest.writeLong(this.spliceEventId);
            dest.writeByte(this.spliceEventCancelIndicator ? (byte) 1 : 0);
            dest.writeByte(this.outOfNetworkIndicator ? (byte) 1 : 0);
            dest.writeByte(this.programSpliceFlag ? (byte) 1 : 0);
            int componentSpliceListSize = this.componentSpliceList.size();
            dest.writeInt(componentSpliceListSize);
            for (int i = 0; i < componentSpliceListSize; i++) {
                ((ComponentSplice) this.componentSpliceList.get(i)).writeToParcel(dest);
            }
            dest.writeLong(this.utcSpliceTime);
            dest.writeByte(this.autoReturn ? (byte) 1 : 0);
            dest.writeLong(this.breakDurationUs);
            dest.writeInt(this.uniqueProgramId);
            dest.writeInt(this.availNum);
            dest.writeInt(this.availsExpected);
        }

        /* access modifiers changed from: private */
        public static Event createFromParcel(Parcel in) {
            return new Event(in);
        }
    }

    private SpliceScheduleCommand(List<Event> events2) {
        this.events = Collections.unmodifiableList(events2);
    }

    private SpliceScheduleCommand(Parcel in) {
        int eventsSize = in.readInt();
        ArrayList<Event> events2 = new ArrayList<>(eventsSize);
        for (int i = 0; i < eventsSize; i++) {
            events2.add(Event.createFromParcel(in));
        }
        this.events = Collections.unmodifiableList(events2);
    }

    static SpliceScheduleCommand parseFromSection(ParsableByteArray sectionData) {
        int spliceCount = sectionData.readUnsignedByte();
        ArrayList<Event> events2 = new ArrayList<>(spliceCount);
        for (int i = 0; i < spliceCount; i++) {
            events2.add(Event.parseFromSection(sectionData));
        }
        return new SpliceScheduleCommand((List<Event>) events2);
    }

    public void writeToParcel(Parcel dest, int flags) {
        int eventsSize = this.events.size();
        dest.writeInt(eventsSize);
        for (int i = 0; i < eventsSize; i++) {
            ((Event) this.events.get(i)).writeToParcel(dest);
        }
    }
}
