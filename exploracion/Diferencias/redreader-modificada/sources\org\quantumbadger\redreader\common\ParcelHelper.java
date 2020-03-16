package org.quantumbadger.redreader.common;

import android.os.Parcel;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.quantumbadger.redreader.image.ImageInfo.HasAudio;
import org.quantumbadger.redreader.image.ImageInfo.MediaType;

public class ParcelHelper {
    public static boolean readBoolean(Parcel in) {
        return in.readByte() == 1;
    }

    public static String readNullableString(Parcel in) {
        if (readBoolean(in)) {
            return null;
        }
        return in.readString();
    }

    @Nullable
    public static MediaType readNullableImageInfoMediaType(Parcel in) {
        if (readBoolean(in)) {
            return null;
        }
        return MediaType.valueOf(in.readString());
    }

    public static HasAudio readImageInfoHasAudio(Parcel in) {
        return HasAudio.valueOf(in.readString());
    }

    public static void writeNullableEnum(Parcel parcel, @Nullable Enum value) {
        if (value == null) {
            writeBoolean(parcel, false);
            return;
        }
        writeBoolean(parcel, true);
        parcel.writeString(value.name());
    }

    public static void writeNonNullEnum(Parcel parcel, @NonNull Enum value) {
        parcel.writeString(value.name());
    }

    public static Integer readNullableInt(Parcel in) {
        if (readBoolean(in)) {
            return null;
        }
        return Integer.valueOf(in.readInt());
    }

    public static Long readNullableLong(Parcel in) {
        if (readBoolean(in)) {
            return null;
        }
        return Long.valueOf(in.readLong());
    }

    public static Boolean readNullableBoolean(Parcel in) {
        if (readBoolean(in)) {
            return null;
        }
        return Boolean.valueOf(readBoolean(in));
    }

    public static void writeBoolean(Parcel parcel, boolean b) {
        parcel.writeByte(b ? (byte) 1 : 0);
    }

    public static void writeNullableString(Parcel parcel, String value) {
        if (value == null) {
            writeBoolean(parcel, false);
            return;
        }
        writeBoolean(parcel, true);
        parcel.writeString(value);
    }

    public static void writeNullableLong(Parcel parcel, Long value) {
        if (value == null) {
            writeBoolean(parcel, false);
            return;
        }
        writeBoolean(parcel, true);
        parcel.writeLong(value.longValue());
    }

    public static void writeNullableBoolean(Parcel parcel, Boolean value) {
        if (value == null) {
            writeBoolean(parcel, false);
            return;
        }
        writeBoolean(parcel, true);
        writeBoolean(parcel, value.booleanValue());
    }
}
