package info.guardianproject.netcipher.proxy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.exoplayer2.C;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

public class OrbotHelper {
    public static final String ACTION_REQUEST_HS = "org.torproject.android.REQUEST_HS_PORT";
    public static final String ACTION_START = "org.torproject.android.intent.action.START";
    public static final String ACTION_START_TOR = "org.torproject.android.START_TOR";
    public static final String ACTION_STATUS = "org.torproject.android.intent.action.STATUS";
    public static final String EXTRA_PACKAGE_NAME = "org.torproject.android.intent.extra.PACKAGE_NAME";
    public static final String EXTRA_STATUS = "org.torproject.android.intent.extra.STATUS";
    private static final String FDROID_PACKAGE_NAME = "org.fdroid.fdroid";
    public static final int HS_REQUEST_CODE = 9999;
    public static final String ORBOT_FDROID_URI = "https://f-droid.org/repository/browse/?fdid=org.torproject.android";
    public static final String ORBOT_MARKET_URI = "market://details?id=org.torproject.android";
    public static final String ORBOT_PACKAGE_NAME = "org.torproject.android";
    public static final String ORBOT_PLAY_URI = "https://play.google.com/store/apps/details?id=org.torproject.android";
    private static final String PLAY_PACKAGE_NAME = "com.android.vending";
    private static final int REQUEST_CODE_STATUS = 100;
    public static final int START_TOR_RESULT = 37428;
    public static final String STATUS_OFF = "OFF";
    public static final String STATUS_ON = "ON";
    public static final String STATUS_STARTING = "STARTING";
    public static final String STATUS_STARTS_DISABLED = "STARTS_DISABLED";
    public static final String STATUS_STOPPING = "STOPPING";

    private OrbotHelper() {
    }

    public static boolean isOnionAddress(URL url) {
        return url.getHost().endsWith(".onion");
    }

    public static boolean isOnionAddress(String urlString) {
        try {
            return isOnionAddress(new URL(urlString));
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static boolean isOnionAddress(Uri uri) {
        return uri.getHost().endsWith(".onion");
    }

    public static boolean isOrbotRunning(Context context) {
        return TorServiceUtils.findProcessId(context) != -1;
    }

    public static boolean isOrbotInstalled(Context context) {
        return isAppInstalled(context, ORBOT_PACKAGE_NAME);
    }

    private static boolean isAppInstalled(Context context, String uri) {
        try {
            context.getPackageManager().getPackageInfo(uri, 1);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    public static void requestHiddenServiceOnPort(Activity activity, int port) {
        Intent intent = new Intent(ACTION_REQUEST_HS);
        intent.setPackage(ORBOT_PACKAGE_NAME);
        intent.putExtra("hs_port", port);
        activity.startActivityForResult(intent, HS_REQUEST_CODE);
    }

    public static boolean requestStartTor(Context context) {
        if (!isOrbotInstalled(context)) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("requestStartTor ");
        sb.append(context.getPackageName());
        Log.i("OrbotHelper", sb.toString());
        context.sendBroadcast(getOrbotStartIntent(context));
        return true;
    }

    public static Intent getOrbotStartIntent(Context context) {
        Intent intent = new Intent(ACTION_START);
        intent.setPackage(ORBOT_PACKAGE_NAME);
        intent.putExtra(EXTRA_PACKAGE_NAME, context.getPackageName());
        return intent;
    }

    @Deprecated
    public static Intent getOrbotStartIntent() {
        Intent intent = new Intent(ACTION_START);
        intent.setPackage(ORBOT_PACKAGE_NAME);
        return intent;
    }

    public static boolean requestShowOrbotStart(Activity activity) {
        if (!isOrbotInstalled(activity) || isOrbotRunning(activity)) {
            return false;
        }
        activity.startActivityForResult(getShowOrbotStartIntent(), START_TOR_RESULT);
        return true;
    }

    public static Intent getShowOrbotStartIntent() {
        Intent intent = new Intent(ACTION_START_TOR);
        intent.setPackage(ORBOT_PACKAGE_NAME);
        intent.addFlags(C.ENCODING_PCM_MU_LAW);
        return intent;
    }

    public static Intent getOrbotInstallIntent(Context context) {
        ResolveInfo r;
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setData(Uri.parse(ORBOT_MARKET_URI));
        String foundPackageName = null;
        Iterator i$ = context.getPackageManager().queryIntentActivities(intent, 0).iterator();
        while (true) {
            if (!i$.hasNext()) {
                break;
            }
            r = (ResolveInfo) i$.next();
            StringBuilder sb = new StringBuilder();
            sb.append("market: ");
            sb.append(r.activityInfo.packageName);
            Log.i("OrbotHelper", sb.toString());
            if (!TextUtils.equals(r.activityInfo.packageName, FDROID_PACKAGE_NAME)) {
                if (TextUtils.equals(r.activityInfo.packageName, PLAY_PACKAGE_NAME)) {
                    break;
                }
            } else {
                break;
            }
        }
        foundPackageName = r.activityInfo.packageName;
        if (foundPackageName == null) {
            intent.setData(Uri.parse(ORBOT_FDROID_URI));
        } else {
            intent.setPackage(foundPackageName);
        }
        return intent;
    }
}
