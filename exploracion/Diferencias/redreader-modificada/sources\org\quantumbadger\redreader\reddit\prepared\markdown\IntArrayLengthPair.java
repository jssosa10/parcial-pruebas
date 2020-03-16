package org.quantumbadger.redreader.reddit.prepared.markdown;

public final class IntArrayLengthPair {
    public final int[] data;
    public int pos = 0;

    public IntArrayLengthPair(int capacity) {
        this.data = new int[capacity];
    }

    public void clear() {
        this.pos = 0;
    }

    public void append(int[] arr) {
        System.arraycopy(arr, 0, this.data, this.pos, arr.length);
        this.pos += arr.length;
    }

    public void append(char[] arr) {
        for (int i = 0; i < arr.length; i++) {
            this.data[this.pos + i] = arr[i];
        }
        this.pos += arr.length;
    }

    public int[] substringAsArray(int start) {
        int[] result = new int[(this.pos - start)];
        System.arraycopy(this.data, start, result, 0, result.length);
        return result;
    }
}
