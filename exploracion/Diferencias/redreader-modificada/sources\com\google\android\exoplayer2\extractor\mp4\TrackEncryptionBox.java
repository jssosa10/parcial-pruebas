package com.google.android.exoplayer2.extractor.mp4;

import android.support.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.extractor.TrackOutput.CryptoData;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;

public final class TrackEncryptionBox {
    private static final String TAG = "TrackEncryptionBox";
    public final CryptoData cryptoData;
    public final byte[] defaultInitializationVector;
    public final boolean isEncrypted;
    public final int perSampleIvSize;
    @Nullable
    public final String schemeType;

    public TrackEncryptionBox(boolean isEncrypted2, @Nullable String schemeType2, int perSampleIvSize2, byte[] keyId, int defaultEncryptedBlocks, int defaultClearBlocks, @Nullable byte[] defaultInitializationVector2) {
        boolean z = true;
        boolean z2 = perSampleIvSize2 == 0;
        if (defaultInitializationVector2 != null) {
            z = false;
        }
        Assertions.checkArgument(z ^ z2);
        this.isEncrypted = isEncrypted2;
        this.schemeType = schemeType2;
        this.perSampleIvSize = perSampleIvSize2;
        this.defaultInitializationVector = defaultInitializationVector2;
        this.cryptoData = new CryptoData(schemeToCryptoMode(schemeType2), keyId, defaultEncryptedBlocks, defaultClearBlocks);
    }

    private static int schemeToCryptoMode(@Nullable String schemeType2) {
        if (schemeType2 == null) {
            return 1;
        }
        char c = 65535;
        int hashCode = schemeType2.hashCode();
        if (hashCode != 3046605) {
            if (hashCode != 3046671) {
                if (hashCode != 3049879) {
                    if (hashCode == 3049895 && schemeType2.equals(C.CENC_TYPE_cens)) {
                        c = 1;
                    }
                } else if (schemeType2.equals(C.CENC_TYPE_cenc)) {
                    c = 0;
                }
            } else if (schemeType2.equals(C.CENC_TYPE_cbcs)) {
                c = 3;
            }
        } else if (schemeType2.equals(C.CENC_TYPE_cbc1)) {
            c = 2;
        }
        switch (c) {
            case 0:
            case 1:
                return 1;
            case 2:
            case 3:
                return 2;
            default:
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Unsupported protection scheme type '");
                sb.append(schemeType2);
                sb.append("'. Assuming AES-CTR crypto mode.");
                Log.w(str, sb.toString());
                return 1;
        }
    }
}
