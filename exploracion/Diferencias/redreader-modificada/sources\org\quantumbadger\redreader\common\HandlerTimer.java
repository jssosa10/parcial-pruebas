package org.quantumbadger.redreader.common;

import android.os.Handler;
import android.support.annotation.UiThread;
import android.util.SparseBooleanArray;

public class HandlerTimer {
    private final Handler mHandler;
    private int mNextId = 0;
    /* access modifiers changed from: private */
    public final SparseBooleanArray mTimers = new SparseBooleanArray();

    public HandlerTimer(Handler handler) {
        this.mHandler = handler;
    }

    private int getNextId() {
        this.mNextId++;
        while (true) {
            if (!this.mTimers.get(this.mNextId, false)) {
                int i = this.mNextId;
                if (i != 0) {
                    return i;
                }
            }
            this.mNextId++;
        }
    }

    @UiThread
    public int setTimer(long delayMs, final Runnable runnable) {
        final int id = getNextId();
        this.mTimers.put(id, true);
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                if (HandlerTimer.this.mTimers.get(id, false)) {
                    HandlerTimer.this.mTimers.delete(id);
                    runnable.run();
                }
            }
        }, delayMs);
        return id;
    }

    public void cancelTimer(int timerId) {
        this.mTimers.delete(timerId);
    }
}
