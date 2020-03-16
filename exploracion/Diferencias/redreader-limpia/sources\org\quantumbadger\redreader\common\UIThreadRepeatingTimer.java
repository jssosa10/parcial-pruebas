package org.quantumbadger.redreader.common;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.UiThread;

public class UIThreadRepeatingTimer implements Runnable {
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final long mIntervalMs;
    private final Listener mListener;
    private boolean mShouldTimerRun = false;

    public interface Listener {
        void onUIThreadRepeatingTimer(UIThreadRepeatingTimer uIThreadRepeatingTimer);
    }

    public UIThreadRepeatingTimer(long mIntervalMs2, Listener mListener2) {
        this.mIntervalMs = mIntervalMs2;
        this.mListener = mListener2;
    }

    @UiThread
    public void startTimer() {
        General.checkThisIsUIThread();
        this.mShouldTimerRun = true;
        this.mHandler.postDelayed(this, this.mIntervalMs);
    }

    @UiThread
    public void stopTimer() {
        General.checkThisIsUIThread();
        this.mShouldTimerRun = false;
    }

    public void run() {
        if (this.mShouldTimerRun) {
            this.mListener.onUIThreadRepeatingTimer(this);
            if (this.mShouldTimerRun) {
                this.mHandler.postDelayed(this, this.mIntervalMs);
            }
        }
    }
}
