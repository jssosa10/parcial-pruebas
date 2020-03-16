package org.quantumbadger.redreader.views;

import android.os.Build.VERSION;
import android.support.annotation.NonNull;
import android.util.Log;

public abstract class RRChoreographer {

    public interface Callback {
        void doFrame(long j);
    }

    public abstract void postFrameCallback(@NonNull Callback callback);

    @NonNull
    public static RRChoreographer getInstance() {
        if (VERSION.SDK_INT >= 16) {
            Log.i("RRChoreographer", "Using modern Choreographer");
            return RRChoreographerModern.INSTANCE;
        }
        Log.i("RRChoreographer", "Using legacy Choreographer");
        return RRChoreographerLegacy.INSTANCE;
    }
}
