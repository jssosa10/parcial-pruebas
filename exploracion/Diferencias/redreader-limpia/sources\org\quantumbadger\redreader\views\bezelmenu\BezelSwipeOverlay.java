package org.quantumbadger.redreader.views.bezelmenu;

import android.content.Context;
import android.preference.PreferenceManager;
import android.view.MotionEvent;
import android.view.View;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;

public class BezelSwipeOverlay extends View {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    private final BezelSwipeListener listener;
    private final int mSwipeZonePixels;

    public interface BezelSwipeListener {
        boolean onSwipe(int i);

        boolean onTap();
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface SwipeEdge {
    }

    public BezelSwipeOverlay(Context context, BezelSwipeListener listener2) {
        super(context);
        this.listener = listener2;
        this.mSwipeZonePixels = General.dpToPixels(getContext(), (float) PrefsUtility.pref_behaviour_bezel_toolbar_swipezone_dp(context, PreferenceManager.getDefaultSharedPreferences(context)));
    }

    public boolean onTouchEvent(MotionEvent event) {
        if ((event.getAction() & 255) != 0) {
            return false;
        }
        if (event.getX() < ((float) this.mSwipeZonePixels)) {
            return this.listener.onSwipe(0);
        }
        if (event.getX() > ((float) (getWidth() - this.mSwipeZonePixels))) {
            return this.listener.onSwipe(1);
        }
        return this.listener.onTap();
    }
}
