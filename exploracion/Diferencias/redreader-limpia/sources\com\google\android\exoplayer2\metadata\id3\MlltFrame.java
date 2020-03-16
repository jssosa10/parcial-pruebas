package com.google.android.exoplayer2.metadata.id3;

import android.os.Parcel;
import android.os.Parcelable.Creator;
import android.support.annotation.Nullable;
import java.util.Arrays;

public final class MlltFrame extends Id3Frame {
    public static final Creator<MlltFrame> CREATOR = new Creator<MlltFrame>() {
        public MlltFrame createFromParcel(Parcel in) {
            return new MlltFrame(in);
        }

        public MlltFrame[] newArray(int size) {
            return new MlltFrame[size];
        }
    };
    public static final String ID = "MLLT";
    public final int bytesBetweenReference;
    public final int[] bytesDeviations;
    public final int millisecondsBetweenReference;
    public final int[] millisecondsDeviations;
    public final int mpegFramesBetweenReference;

    public MlltFrame(int mpegFramesBetweenReference2, int bytesBetweenReference2, int millisecondsBetweenReference2, int[] bytesDeviations2, int[] millisecondsDeviations2) {
        super(ID);
        this.mpegFramesBetweenReference = mpegFramesBetweenReference2;
        this.bytesBetweenReference = bytesBetweenReference2;
        this.millisecondsBetweenReference = millisecondsBetweenReference2;
        this.bytesDeviations = bytesDeviations2;
        this.millisecondsDeviations = millisecondsDeviations2;
    }

    MlltFrame(Parcel in) {
        super(ID);
        this.mpegFramesBetweenReference = in.readInt();
        this.bytesBetweenReference = in.readInt();
        this.millisecondsBetweenReference = in.readInt();
        this.bytesDeviations = in.createIntArray();
        this.millisecondsDeviations = in.createIntArray();
    }

    public boolean equals(@Nullable Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MlltFrame other = (MlltFrame) obj;
        if (!(this.mpegFramesBetweenReference == other.mpegFramesBetweenReference && this.bytesBetweenReference == other.bytesBetweenReference && this.millisecondsBetweenReference == other.millisecondsBetweenReference && Arrays.equals(this.bytesDeviations, other.bytesDeviations) && Arrays.equals(this.millisecondsDeviations, other.millisecondsDeviations))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (((((((((17 * 31) + this.mpegFramesBetweenReference) * 31) + this.bytesBetweenReference) * 31) + this.millisecondsBetweenReference) * 31) + Arrays.hashCode(this.bytesDeviations)) * 31) + Arrays.hashCode(this.millisecondsDeviations);
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mpegFramesBetweenReference);
        dest.writeInt(this.bytesBetweenReference);
        dest.writeInt(this.millisecondsBetweenReference);
        dest.writeIntArray(this.bytesDeviations);
        dest.writeIntArray(this.millisecondsDeviations);
    }

    public int describeContents() {
        return 0;
    }
}
