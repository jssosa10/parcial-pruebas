package org.quantumbadger.redreader.views;

import android.support.annotation.NonNull;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.views.RRChoreographer.Callback;

public class RRChoreographerLegacy extends RRChoreographer implements Runnable {
    static final RRChoreographerLegacy INSTANCE = new RRChoreographerLegacy();
    private int mCallbackCount = 0;
    private final Callback[] mCallbacks = new Callback[128];
    private boolean mPosted = false;

    private RRChoreographerLegacy() {
    }

    public void postFrameCallback(@NonNull Callback callback) {
        Callback[] callbackArr = this.mCallbacks;
        int i = this.mCallbackCount;
        callbackArr[i] = callback;
        this.mCallbackCount = i + 1;
        if (!this.mPosted) {
            AndroidCommon.UI_THREAD_HANDLER.postDelayed(this, 16);
            this.mPosted = true;
        }
    }

    public void run() {
        long frameTimeNanos = System.nanoTime();
        int callbackCount = this.mCallbackCount;
        this.mPosted = false;
        this.mCallbackCount = 0;
        for (int i = 0; i < callbackCount; i++) {
            this.mCallbacks[i].doFrame(frameTimeNanos);
        }
    }
}
