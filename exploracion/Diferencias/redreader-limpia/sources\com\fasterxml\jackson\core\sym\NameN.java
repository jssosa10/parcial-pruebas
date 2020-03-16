package com.fasterxml.jackson.core.sym;

import java.util.Arrays;

public final class NameN extends Name {
    private final int[] q;
    private final int q1;
    private final int q2;
    private final int q3;
    private final int q4;
    private final int qlen;

    NameN(String name, int hash, int q12, int q22, int q32, int q42, int[] quads, int quadLen) {
        super(name, hash);
        this.q1 = q12;
        this.q2 = q22;
        this.q3 = q32;
        this.q4 = q42;
        this.q = quads;
        this.qlen = quadLen;
    }

    public static NameN construct(String name, int hash, int[] q5, int qlen2) {
        int[] buf;
        int[] iArr = q5;
        int i = qlen2;
        if (i >= 4) {
            int q12 = iArr[0];
            int q22 = iArr[1];
            int q32 = iArr[2];
            int q42 = iArr[3];
            if (i - 4 > 0) {
                buf = Arrays.copyOfRange(iArr, 4, i);
            } else {
                buf = null;
            }
            NameN nameN = new NameN(name, hash, q12, q22, q32, q42, buf, qlen2);
            return nameN;
        }
        throw new IllegalArgumentException();
    }

    public boolean equals(int quad) {
        return false;
    }

    public boolean equals(int quad1, int quad2) {
        return false;
    }

    public boolean equals(int quad1, int quad2, int quad3) {
        return false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:22:0x003e, code lost:
        if (r7[6] == r6.q[2]) goto L_0x0041;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0040, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x0048, code lost:
        if (r7[5] == r6.q[1]) goto L_0x004b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x004a, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0052, code lost:
        if (r7[4] == r6.q[0]) goto L_0x0055;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0054, code lost:
        return false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0055, code lost:
        return true;
     */
    public boolean equals(int[] quads, int len) {
        if (len != this.qlen || quads[0] != this.q1 || quads[1] != this.q2 || quads[2] != this.q3 || quads[3] != this.q4) {
            return false;
        }
        switch (len) {
            case 4:
                break;
            case 5:
                break;
            case 6:
                break;
            case 7:
                break;
            case 8:
                if (quads[7] != this.q[3]) {
                    return false;
                }
                break;
            default:
                return _equals2(quads);
        }
    }

    private final boolean _equals2(int[] quads) {
        int end = this.qlen - 4;
        for (int i = 0; i < end; i++) {
            if (quads[i + 4] != this.q[i]) {
                return false;
            }
        }
        return true;
    }
}
