package org.quantumbadger.redreader.common;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.preference.PreferenceManager;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import java.util.concurrent.atomic.AtomicBoolean;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.cache.CacheDownload;
import org.quantumbadger.redreader.http.HTTPBackend;

public class TorCommon {
    private static final AtomicBoolean sIsTorEnabled = new AtomicBoolean(false);

    public static void promptToInstallOrbot(final Context context) {
        General.checkThisIsUIThread();
        Builder notInstalled = new Builder(context);
        notInstalled.setMessage(R.string.error_tor_not_installed);
        notInstalled.setPositiveButton(R.string.dialog_yes, new OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Context context = context;
                context.startActivity(OrbotHelper.getOrbotInstallIntent(context));
                dialog.dismiss();
            }
        });
        notInstalled.setNegativeButton(R.string.dialog_no, new OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        notInstalled.create().show();
    }

    public static void updateTorStatus(Context context) {
        General.checkThisIsUIThread();
        boolean torEnabled = PrefsUtility.network_tor(context, PreferenceManager.getDefaultSharedPreferences(context));
        boolean torChanged = torEnabled != isTorEnabled();
        sIsTorEnabled.set(torEnabled);
        if (torEnabled) {
            verifyTorSetup(context);
        }
        if (torChanged) {
            HTTPBackend.getBackend().recreateHttpBackend();
            CacheDownload.resetUserCredentialsOnNextRequest();
        }
    }

    private static void verifyTorSetup(Context context) {
        General.checkThisIsUIThread();
        if (sIsTorEnabled.get()) {
            if (!OrbotHelper.isOrbotInstalled(context)) {
                promptToInstallOrbot(context);
            } else {
                ensureTorIsRunning(context);
            }
        }
    }

    private static void ensureTorIsRunning(Context context) {
        if (!OrbotHelper.isOrbotRunning(context) && !OrbotHelper.requestStartTor(context)) {
            Builder builder = new Builder(context);
            builder.setMessage(R.string.error_tor_start_failed);
            builder.create().show();
        }
    }

    public static boolean isTorEnabled() {
        return sIsTorEnabled.get();
    }
}
