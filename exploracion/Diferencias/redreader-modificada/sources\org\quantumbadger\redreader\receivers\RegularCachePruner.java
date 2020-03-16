package org.quantumbadger.redreader.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager;

public class RegularCachePruner extends BroadcastReceiver {
    public void onReceive(final Context context, Intent intent) {
        Log.i("RegularCachePruner", "Pruning cache...");
        new Thread() {
            public void run() {
                RedditChangeDataManager.pruneAllUsers(context);
                CacheManager.getInstance(context).pruneCache();
            }
        }.start();
    }
}
