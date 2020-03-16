package com.google.android.exoplayer2.metadata.emsg;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.metadata.Metadata.Entry;
import com.google.android.exoplayer2.util.Util;
import java.util.Arrays;

public final class EventMessage implements Entry {
    public static final Creator<EventMessage> CREATOR = new Creator<EventMessage>() {
        public EventMessage createFromParcel(Parcel in) {
            return new EventMessage(in);
        }

        public EventMessage[] newArray(int size) {
            return new EventMessage[size];
        }
    };
    public final long durationMs;
    private int hashCode;
    public final long id;
    public final byte[] messageData;
    public final long presentationTimeUs;
    public final String schemeIdUri;
    public final String value;

    public EventMessage(String schemeIdUri2, String value2, long durationMs2, long id2, byte[] messageData2, long presentationTimeUs2) {
        this.schemeIdUri = schemeIdUri2;
        this.value = value2;
        this.durationMs = durationMs2;
        this.id = id2;
        this.messageData = messageData2;
        this.presentationTimeUs = presentationTimeUs2;
    }

    EventMessage(Parcel in) {
        this.schemeIdUri = (String) Util.castNonNull(in.readString());
        this.value = (String) Util.castNonNull(in.readString());
        this.presentationTimeUs = in.readLong();
        this.durationMs = in.readLong();
        this.id = in.readLong();
        this.messageData = (byte[]) Util.castNonNull(in.createByteArray());
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            int i = 17 * 31;
            String str = this.schemeIdUri;
            int i2 = 0;
            int result = (i + (str != null ? str.hashCode() : 0)) * 31;
            String str2 = this.value;
            if (str2 != null) {
                i2 = str2.hashCode();
            }
            int result2 = (result + i2) * 31;
            long j = this.presentationTimeUs;
            int result3 = (result2 + ((int) (j ^ (j >>> 32)))) * 31;
            long j2 = this.durationMs;
            int result4 = (result3 + ((int) (j2 ^ (j2 >>> 32)))) * 31;
            long j3 = this.id;
            this.hashCode = ((result4 + ((int) (j3 ^ (j3 >>> 32)))) * 31) + Arrays.hashCode(this.messageData);
        }
        return this.hashCode;
    }

    public boolean equals(@Nullable Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        EventMessage other = (EventMessage) obj;
        if (this.presentationTimeUs != other.presentationTimeUs || this.durationMs != other.durationMs || this.id != other.id || !Util.areEqual(this.schemeIdUri, other.schemeIdUri) || !Util.areEqual(this.value, other.value) || !Arrays.equals(this.messageData, other.messageData)) {
            z = false;
        }
        return z;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EMSG: scheme=");
        sb.append(this.schemeIdUri);
        sb.append(", id=");
        sb.append(this.id);
        sb.append(", value=");
        sb.append(this.value);
        return sb.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.schemeIdUri);
        dest.writeString(this.value);
        dest.writeLong(this.presentationTimeUs);
        dest.writeLong(this.durationMs);
        dest.writeLong(this.id);
        dest.writeByteArray(this.messageData);
    }
}
