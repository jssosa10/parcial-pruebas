package org.quantumbadger.redreader.common;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.time.DateUtils;
import org.quantumbadger.redreader.receivers.NewMessageChecker;
import org.quantumbadger.redreader.receivers.RegularCachePruner;

public class Alarms {
    private static Map<Alarm, AlarmManager> alarmMap = new HashMap();
    private static Map<Alarm, PendingIntent> intentMap = new HashMap();

    public enum Alarm {
        MESSAGE_CHECKER(1800000, NewMessageChecker.class, true),
        CACHE_PRUNER(DateUtils.MILLIS_PER_HOUR, RegularCachePruner.class, true);
        
        private final Class alarmClass;
        private final long interval;
        private final boolean startOnBoot;

        private Alarm(long interval2, Class alarmClass2, boolean startOnBoot2) {
            this.interval = interval2;
            this.alarmClass = alarmClass2;
            this.startOnBoot = startOnBoot2;
        }

        /* access modifiers changed from: private */
        public long interval() {
            return this.interval;
        }

        /* access modifiers changed from: private */
        public Class alarmClass() {
            return this.alarmClass;
        }

        /* access modifiers changed from: private */
        public boolean startOnBoot() {
            return this.startOnBoot;
        }
    }

    public static void startAlarm(Alarm alarm, Context context) {
        if (!alarmMap.containsKey(alarm)) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, alarm.alarmClass()), 0);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
            alarmManager.setInexactRepeating(1, System.currentTimeMillis(), alarm.interval(), pendingIntent);
            alarmMap.put(alarm, alarmManager);
            intentMap.put(alarm, pendingIntent);
        }
    }

    public static void stopAlarm(Alarm alarm) {
        if (alarmMap.containsKey(alarm)) {
            ((AlarmManager) alarmMap.get(alarm)).cancel((PendingIntent) intentMap.get(alarm));
            alarmMap.remove(alarm);
            intentMap.remove(alarm);
        }
    }

    public static void onBoot(Context context) {
        Alarm[] values;
        for (Alarm alarm : Alarm.values()) {
            if (alarm.startOnBoot()) {
                startAlarm(alarm, context);
            }
        }
    }
}
