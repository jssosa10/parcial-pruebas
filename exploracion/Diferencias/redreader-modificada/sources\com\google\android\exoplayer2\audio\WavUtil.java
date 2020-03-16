package com.google.android.exoplayer2.audio;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.upstream.DataSchemeDataSource;
import com.google.android.exoplayer2.util.Util;

public final class WavUtil {
    public static final int DATA_FOURCC = Util.getIntegerCodeForString(DataSchemeDataSource.SCHEME_DATA);
    public static final int FMT_FOURCC = Util.getIntegerCodeForString("fmt ");
    public static final int RIFF_FOURCC = Util.getIntegerCodeForString("RIFF");
    private static final int TYPE_A_LAW = 6;
    private static final int TYPE_FLOAT = 3;
    private static final int TYPE_MU_LAW = 7;
    private static final int TYPE_PCM = 1;
    private static final int TYPE_WAVE_FORMAT_EXTENSIBLE = 65534;
    public static final int WAVE_FOURCC = Util.getIntegerCodeForString("WAVE");

    public static int getTypeForEncoding(int encoding) {
        if (encoding != Integer.MIN_VALUE) {
            if (encoding == 268435456) {
                return 7;
            }
            if (encoding == 536870912) {
                return 6;
            }
            if (encoding != 1073741824) {
                switch (encoding) {
                    case 2:
                    case 3:
                        break;
                    case 4:
                        return 3;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        }
        return 1;
    }

    public static int getEncodingForType(int type, int bitsPerSample) {
        if (type != 1) {
            int i = 0;
            if (type == 3) {
                if (bitsPerSample == 32) {
                    i = 4;
                }
                return i;
            } else if (type != TYPE_WAVE_FORMAT_EXTENSIBLE) {
                switch (type) {
                    case 6:
                        return C.ENCODING_PCM_A_LAW;
                    case 7:
                        return C.ENCODING_PCM_MU_LAW;
                    default:
                        return 0;
                }
            }
        }
        return Util.getPcmEncoding(bitsPerSample);
    }

    private WavUtil() {
    }
}
