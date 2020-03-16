package org.quantumbadger.redreader.reddit.prepared.markdown;

import java.util.LinkedList;

public final class CharArrSubstring {
    protected final char[] arr;
    public final int length;
    protected final int start;

    CharArrSubstring(char[] arr2, int start2, int length2) {
        this.arr = arr2;
        this.start = start2;
        this.length = length2;
    }

    public static CharArrSubstring generate(char[] src) {
        return new CharArrSubstring(src, 0, src.length);
    }

    public static CharArrSubstring[] generateFromLines(char[] src) {
        int curPos = 0;
        LinkedList<CharArrSubstring> result = new LinkedList<>();
        while (true) {
            int indexOfLinebreak = indexOfLinebreak(src, curPos);
            int nextLinebreak = indexOfLinebreak;
            if (indexOfLinebreak != -1) {
                result.add(new CharArrSubstring(src, curPos, nextLinebreak - curPos));
                curPos = nextLinebreak + 1;
            } else {
                result.add(new CharArrSubstring(src, curPos, src.length - curPos));
                return (CharArrSubstring[]) result.toArray(new CharArrSubstring[result.size()]);
            }
        }
    }

    public CharArrSubstring rejoin(CharArrSubstring toAppend) {
        int i = toAppend.start - 1;
        int i2 = this.start;
        int i3 = this.length;
        if (i == i2 + i3) {
            return new CharArrSubstring(this.arr, i2, i3 + 1 + toAppend.length);
        }
        throw new RuntimeException("Internal error: attempt to join non-consecutive substrings");
    }

    private static int indexOfLinebreak(char[] raw, int startPos) {
        for (int i = startPos; i < raw.length; i++) {
            if (raw[i] == 10) {
                return i;
            }
        }
        return -1;
    }

    public int countSpacesAtStart() {
        int i = 0;
        while (true) {
            int i2 = this.length;
            if (i >= i2) {
                return i2;
            }
            if (this.arr[this.start + i] != ' ') {
                return i;
            }
            i++;
        }
    }

    public int countSpacesAtEnd() {
        int i = 0;
        while (true) {
            int i2 = this.length;
            if (i >= i2) {
                return i2;
            }
            if (this.arr[((this.start + i2) - 1) - i] != ' ') {
                return i;
            }
            i++;
        }
    }

    public char charAt(int index) {
        return this.arr[this.start + index];
    }

    public int countPrefixLengthIgnoringSpaces(char c) {
        int i = 0;
        while (true) {
            int i2 = this.length;
            if (i >= i2) {
                return i2;
            }
            char[] cArr = this.arr;
            int i3 = this.start;
            if (cArr[i3 + i] != ' ' && cArr[i3 + i] != c) {
                return i;
            }
            i++;
        }
    }

    public int countPrefixLevelIgnoringSpaces(char c) {
        int level = 0;
        int i = 0;
        while (true) {
            int i2 = this.length;
            if (i >= i2) {
                return i2;
            }
            char[] cArr = this.arr;
            int i3 = this.start;
            if (cArr[i3 + i] != ' ' && cArr[i3 + i] != c) {
                return level;
            }
            if (this.arr[this.start + i] == c) {
                level++;
            }
            i++;
        }
    }

    public CharArrSubstring left(int chars) {
        return new CharArrSubstring(this.arr, this.start, chars);
    }

    public CharArrSubstring substring(int start2) {
        return new CharArrSubstring(this.arr, this.start + start2, this.length - start2);
    }

    public CharArrSubstring substring(int start2, int len) {
        return new CharArrSubstring(this.arr, this.start + start2, len);
    }

    public CharArrSubstring readInteger(int start2) {
        int i = start2;
        while (true) {
            int i2 = this.length;
            if (i >= i2) {
                return new CharArrSubstring(this.arr, this.start + start2, i2 - start2);
            }
            char c = this.arr[this.start + i];
            if (c >= '0' && c <= '9') {
                i++;
            }
        }
        return new CharArrSubstring(this.arr, this.start + start2, i - start2);
    }

    public String toString() {
        return new String(this.arr, this.start, this.length);
    }

    public boolean isRepeatingChar(char c, int start2, int len) {
        for (int i = 0; i < len; i++) {
            if (this.arr[i + start2 + this.start] != c) {
                return false;
            }
        }
        return true;
    }

    public boolean equalAt(int position, String needle) {
        if (this.length < needle.length() + position) {
            return false;
        }
        for (int i = 0; i < needle.length(); i++) {
            if (needle.charAt(i) != this.arr[this.start + position + i]) {
                return false;
            }
        }
        return true;
    }

    public void replaceUnicodeSpaces() {
        for (int i = 0; i < this.length; i++) {
            if (MarkdownTokenizer.isUnicodeWhitespace(this.arr[this.start + i])) {
                this.arr[this.start + i] = ' ';
            }
        }
    }
}
