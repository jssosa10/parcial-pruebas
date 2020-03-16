package android.support.v4.content.pm;

import android.content.pm.PackageInfo;
import android.os.Build.VERSION;
import android.support.annotation.NonNull;

public final class PackageInfoCompat {
    public static long getLongVersionCode(@NonNull PackageInfo info2) {
        if (VERSION.SDK_INT >= 28) {
            return info2.getLongVersionCode();
        }
        return (long) info2.versionCode;
    }

    private PackageInfoCompat() {
    }
}
