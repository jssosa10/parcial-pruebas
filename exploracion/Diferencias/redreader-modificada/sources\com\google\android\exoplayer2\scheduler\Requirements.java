package com.google.android.exoplayer2.scheduler;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import com.google.android.exoplayer2.util.Util;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class Requirements {
    private static final int DEVICE_CHARGING = 16;
    private static final int DEVICE_IDLE = 8;
    public static final int NETWORK_TYPE_ANY = 1;
    private static final int NETWORK_TYPE_MASK = 7;
    public static final int NETWORK_TYPE_METERED = 4;
    public static final int NETWORK_TYPE_NONE = 0;
    public static final int NETWORK_TYPE_NOT_ROAMING = 3;
    private static final String[] NETWORK_TYPE_STRINGS = null;
    public static final int NETWORK_TYPE_UNMETERED = 2;
    private static final String TAG = "Requirements";
    private final int requirements;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface NetworkType {
    }

    public Requirements(int networkType, boolean charging, boolean idle) {
        int i = 0;
        int i2 = (charging ? 16 : 0) | networkType;
        if (idle) {
            i = 8;
        }
        this(i | i2);
    }

    public Requirements(int requirementsData) {
        this.requirements = requirementsData;
    }

    public int getRequiredNetworkType() {
        return this.requirements & 7;
    }

    public boolean isChargingRequired() {
        return (this.requirements & 16) != 0;
    }

    public boolean isIdleRequired() {
        return (this.requirements & 8) != 0;
    }

    public boolean checkRequirements(Context context) {
        return checkNetworkRequirements(context) && checkChargingRequirement(context) && checkIdleRequirement(context);
    }

    public int getRequirementsData() {
        return this.requirements;
    }

    private boolean checkNetworkRequirements(Context context) {
        int networkRequirement = getRequiredNetworkType();
        if (networkRequirement == 0) {
            return true;
        }
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            logd("No network info or no connection.");
            return false;
        } else if (!checkInternetConnectivity(connectivityManager)) {
            return false;
        } else {
            if (networkRequirement == 1) {
                return true;
            }
            if (networkRequirement == 3) {
                boolean roaming = networkInfo.isRoaming();
                StringBuilder sb = new StringBuilder();
                sb.append("Roaming: ");
                sb.append(roaming);
                logd(sb.toString());
                return !roaming;
            }
            boolean activeNetworkMetered = isActiveNetworkMetered(connectivityManager, networkInfo);
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Metered network: ");
            sb2.append(activeNetworkMetered);
            logd(sb2.toString());
            if (networkRequirement == 2) {
                return !activeNetworkMetered;
            }
            if (networkRequirement == 4) {
                return activeNetworkMetered;
            }
            throw new IllegalStateException();
        }
    }

    private boolean checkChargingRequirement(Context context) {
        boolean z = true;
        if (!isChargingRequired()) {
            return true;
        }
        Intent batteryStatus = context.registerReceiver(null, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        if (batteryStatus == null) {
            return false;
        }
        int status = batteryStatus.getIntExtra(NotificationCompat.CATEGORY_STATUS, -1);
        if (!(status == 2 || status == 5)) {
            z = false;
        }
        return z;
    }

    private boolean checkIdleRequirement(Context context) {
        boolean z = true;
        if (!isIdleRequired()) {
            return true;
        }
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        if (Util.SDK_INT >= 23) {
            z = powerManager.isDeviceIdleMode();
        } else if (Util.SDK_INT < 20 ? powerManager.isScreenOn() : powerManager.isInteractive()) {
            z = false;
        }
        return z;
    }

    private static boolean checkInternetConnectivity(ConnectivityManager connectivityManager) {
        boolean z = true;
        if (Util.SDK_INT < 23) {
            return true;
        }
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            logd("No active network.");
            return false;
        }
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        boolean validated = networkCapabilities == null || !networkCapabilities.hasCapability(16);
        StringBuilder sb = new StringBuilder();
        sb.append("Network capability validated: ");
        sb.append(validated);
        logd(sb.toString());
        if (validated) {
            z = false;
        }
        return z;
    }

    private static boolean isActiveNetworkMetered(ConnectivityManager connectivityManager, NetworkInfo networkInfo) {
        if (Util.SDK_INT >= 16) {
            return connectivityManager.isActiveNetworkMetered();
        }
        int type = networkInfo.getType();
        boolean z = true;
        if (type == 1 || type == 7 || type == 9) {
            z = false;
        }
        return z;
    }

    private static void logd(String message) {
    }

    public String toString() {
        return super.toString();
    }
}
