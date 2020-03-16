package org.quantumbadger.redreader.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.quantumbadger.redreader.common.Alarms;

public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Alarms.onBoot(context);
    }
}
