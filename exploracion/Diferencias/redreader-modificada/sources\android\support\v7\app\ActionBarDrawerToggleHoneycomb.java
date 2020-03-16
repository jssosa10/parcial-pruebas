package android.support.v7.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.lang.reflect.Method;

class ActionBarDrawerToggleHoneycomb {
    private static final String TAG = "ActionBarDrawerToggleHC";
    private static final int[] THEME_ATTRS = {16843531};

    static class SetIndicatorInfo {
        public Method setHomeActionContentDescription;
        public Method setHomeAsUpIndicator;
        public ImageView upIndicatorView;

        SetIndicatorInfo(Activity activity) {
            try {
                this.setHomeAsUpIndicator = ActionBar.class.getDeclaredMethod("setHomeAsUpIndicator", new Class[]{Drawable.class});
                this.setHomeActionContentDescription = ActionBar.class.getDeclaredMethod("setHomeActionContentDescription", new Class[]{Integer.TYPE});
            } catch (NoSuchMethodException e) {
                View home = activity.findViewById(16908332);
                if (home != null) {
                    ViewGroup parent = (ViewGroup) home.getParent();
                    if (parent.getChildCount() == 2) {
                        View first = parent.getChildAt(0);
                        View up = first.getId() == 16908332 ? parent.getChildAt(1) : first;
                        if (up instanceof ImageView) {
                            this.upIndicatorView = (ImageView) up;
                        }
                    }
                }
            }
        }
    }

    public static SetIndicatorInfo setActionBarUpIndicator(SetIndicatorInfo info2, Activity activity, Drawable drawable, int contentDescRes) {
        SetIndicatorInfo info3 = new SetIndicatorInfo(activity);
        if (info3.setHomeAsUpIndicator != null) {
            try {
                ActionBar actionBar = activity.getActionBar();
                info3.setHomeAsUpIndicator.invoke(actionBar, new Object[]{drawable});
                info3.setHomeActionContentDescription.invoke(actionBar, new Object[]{Integer.valueOf(contentDescRes)});
            } catch (Exception e) {
                Log.w(TAG, "Couldn't set home-as-up indicator via JB-MR2 API", e);
            }
        } else if (info3.upIndicatorView != null) {
            info3.upIndicatorView.setImageDrawable(drawable);
        } else {
            Log.w(TAG, "Couldn't set home-as-up indicator");
        }
        return info3;
    }

    public static SetIndicatorInfo setActionBarDescription(SetIndicatorInfo info2, Activity activity, int contentDescRes) {
        if (info2 == null) {
            info2 = new SetIndicatorInfo(activity);
        }
        if (info2.setHomeAsUpIndicator != null) {
            try {
                ActionBar actionBar = activity.getActionBar();
                info2.setHomeActionContentDescription.invoke(actionBar, new Object[]{Integer.valueOf(contentDescRes)});
                if (VERSION.SDK_INT <= 19) {
                    actionBar.setSubtitle(actionBar.getSubtitle());
                }
            } catch (Exception e) {
                Log.w(TAG, "Couldn't set content description via JB-MR2 API", e);
            }
        }
        return info2;
    }

    public static Drawable getThemeUpIndicator(Activity activity) {
        TypedArray a = activity.obtainStyledAttributes(THEME_ATTRS);
        Drawable result = a.getDrawable(0);
        a.recycle();
        return result;
    }

    private ActionBarDrawerToggleHoneycomb() {
    }
}
