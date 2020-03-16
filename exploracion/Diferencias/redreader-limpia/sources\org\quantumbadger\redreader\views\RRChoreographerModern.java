package org.quantumbadger.redreader.views;

import android.annotation.TargetApi;
import android.support.annotation.NonNull;
import android.view.Choreographer;
import android.view.Choreographer.FrameCallback;
import org.quantumbadger.redreader.views.RRChoreographer.Callback;

@TargetApi(16)
public class RRChoreographerModern extends RRChoreographer implements FrameCallback {
    private static final Choreographer CHOREOGRAPHER = Choreographer.getInstance();
    static final RRChoreographerModern INSTANCE = new RRChoreographerModern();
    private int mCallbackCount = 0;
    private final Callback[] mCallbacks = new Callback[128];
    private boolean mPosted = false;

    private RRChoreographerModern() {
    }

    public void postFrameCallback(@NonNull Callback callback) {
        Callback[] callbackArr = this.mCallbacks;
        int i = this.mCallbackCount;
        callbackArr[i] = callback;
        this.mCallbackCount = i + 1;
        if (!this.mPosted) {
            CHOREOGRAPHER.postFrameCallback(this);
            this.mPosted = true;
        }
    }

    public void doFrame(long frameTimeNanos) {
        int callbackCount = this.mCallbackCount;
        this.mPosted = false;
        this.mCallbackCount = 0;
        for (int i = 0; i < callbackCount; i++) {
            this.mCallbacks[i].doFrame(frameTimeNanos);
        }
    }
}
