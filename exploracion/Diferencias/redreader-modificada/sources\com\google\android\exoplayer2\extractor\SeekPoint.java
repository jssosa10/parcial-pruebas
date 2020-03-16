package com.google.android.exoplayer2.extractor;

import android.support.annotation.Nullable;

public final class SeekPoint {
    public static final SeekPoint START = new SeekPoint(0, 0);
    public final long position;
    public final long timeUs;

    public SeekPoint(long timeUs2, long position2) {
        this.timeUs = timeUs2;
        this.position = position2;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[timeUs=");
        sb.append(this.timeUs);
        sb.append(", position=");
        sb.append(this.position);
        sb.append("]");
        return sb.toString();
    }

    public boolean equals(@Nullable Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SeekPoint other = (SeekPoint) obj;
        if (!(this.timeUs == other.timeUs && this.position == other.position)) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (((int) this.timeUs) * 31) + ((int) this.position);
    }
}
