package org.quantumbadger.redreader.views;

public final class SwipeHistory {
    private int len = 0;
    private final float[] positions;
    private int start = 0;
    private final long[] timestamps;

    public SwipeHistory(int len2) {
        this.positions = new float[len2];
        this.timestamps = new long[len2];
    }

    public void add(float position, long timestamp) {
        int i = this.len;
        float[] fArr = this.positions;
        if (i >= fArr.length) {
            int i2 = this.start;
            fArr[i2] = position;
            this.timestamps[i2] = timestamp;
            this.start = (i2 + 1) % fArr.length;
            return;
        }
        int i3 = this.start;
        fArr[(i3 + i) % fArr.length] = position;
        long[] jArr = this.timestamps;
        jArr[(i3 + i) % jArr.length] = timestamp;
        this.len = i + 1;
    }

    public float getMostRecent() {
        return this.positions[getNthMostRecentIndex(0)];
    }

    public float getAtTimeAgoMs(long timeAgo) {
        long timestamp = this.timestamps[getNthMostRecentIndex(0)] - timeAgo;
        float result = getMostRecent();
        for (int i = 0; i < this.len; i++) {
            int index = getNthMostRecentIndex(i);
            if (timestamp > this.timestamps[index]) {
                return result;
            }
            result = this.positions[index];
        }
        return result;
    }

    private int getNthMostRecentIndex(int n) {
        int i = this.len;
        if (n < i && n >= 0) {
            return (((this.start + i) - n) - 1) % this.positions.length;
        }
        throw new ArrayIndexOutOfBoundsException(n);
    }

    public void clear() {
        this.len = 0;
        this.start = 0;
    }

    public int size() {
        return this.len;
    }
}
