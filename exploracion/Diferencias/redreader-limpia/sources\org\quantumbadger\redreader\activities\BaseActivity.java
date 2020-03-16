package org.quantumbadger.redreader.activities;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.TypedArray;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceNavbarColour;
import org.quantumbadger.redreader.common.PrefsUtility.ScreenOrientation;
import org.quantumbadger.redreader.common.TorCommon;

public abstract class BaseActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {
    private static boolean closingAll = false;
    private ImageView mActionbarBackIconView;
    private View mActionbarTitleOuterView;
    private TextView mActionbarTitleTextView;
    private FrameLayout mContentView;
    private final HashMap<Integer, PermissionCallback> mPermissionRequestCallbacks = new HashMap<>();
    private final AtomicInteger mPermissionRequestIdGenerator = new AtomicInteger();
    private SharedPreferences mSharedPreferences;

    public interface PermissionCallback {
        void onPermissionDenied();

        void onPermissionGranted();
    }

    /* access modifiers changed from: protected */
    public boolean baseActivityIsToolbarActionBarEnabled() {
        return true;
    }

    /* access modifiers changed from: protected */
    public boolean baseActivityIsActionBarBackEnabled() {
        return true;
    }

    public void setTitle(CharSequence text) {
        super.setTitle(text);
        TextView textView = this.mActionbarTitleTextView;
        if (textView != null) {
            textView.setText(text);
        }
    }

    public void setTitle(int res) {
        setTitle(getText(res));
    }

    public void closeAllExceptMain() {
        closingAll = true;
        closeIfNecessary();
    }

    @NonNull
    public final ActionBar getSupportActionBarOrThrow() {
        ActionBar result = getSupportActionBar();
        if (result != null) {
            return result;
        }
        throw new RuntimeException("Action bar is null");
    }

    /* access modifiers changed from: protected */
    public void configBackButton(boolean isVisible, OnClickListener listener) {
        if (isVisible) {
            this.mActionbarBackIconView.setVisibility(0);
            this.mActionbarTitleOuterView.setOnClickListener(listener);
            this.mActionbarTitleOuterView.setClickable(true);
            return;
        }
        this.mActionbarBackIconView.setVisibility(8);
        this.mActionbarTitleOuterView.setClickable(false);
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        Toolbar toolbar;
        View outerView;
        int colour;
        super.onCreate(savedInstanceState);
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (PrefsUtility.pref_appearance_hide_android_status(this, this.mSharedPreferences)) {
            getWindow().setFlags(1024, 1024);
        }
        this.mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        setOrientationFromPrefs();
        closeIfNecessary();
        if (baseActivityIsToolbarActionBarEnabled()) {
            if (!PrefsUtility.pref_appearance_bottom_toolbar(this, this.mSharedPreferences)) {
                outerView = getLayoutInflater().inflate(R.layout.rr_actionbar, null);
                toolbar = (Toolbar) outerView.findViewById(R.id.rr_actionbar_toolbar);
                this.mContentView = (FrameLayout) outerView.findViewById(R.id.rr_actionbar_content);
            } else {
                outerView = getLayoutInflater().inflate(R.layout.rr_actionbar_reverse, null);
                toolbar = (Toolbar) outerView.findViewById(R.id.rr_actionbar_reverse_toolbar);
                this.mContentView = (FrameLayout) outerView.findViewById(R.id.rr_actionbar_reverse_content);
            }
            super.setContentView(outerView);
            setSupportActionBar(toolbar);
            getSupportActionBarOrThrow().setCustomView((int) R.layout.actionbar_title);
            getSupportActionBarOrThrow().setDisplayShowCustomEnabled(true);
            getSupportActionBarOrThrow().setDisplayShowTitleEnabled(false);
            toolbar.setContentInsetsAbsolute(0, 0);
            this.mActionbarTitleTextView = (TextView) toolbar.findViewById(R.id.actionbar_title_text);
            this.mActionbarBackIconView = (ImageView) toolbar.findViewById(R.id.actionbar_title_back_image);
            this.mActionbarTitleOuterView = toolbar.findViewById(R.id.actionbar_title_outer);
            if (getTitle() != null) {
                setTitle(getTitle());
            }
            configBackButton(baseActivityIsActionBarBackEnabled(), new OnClickListener() {
                public void onClick(View v) {
                    BaseActivity.this.finish();
                }
            });
            if (VERSION.SDK_INT >= 21) {
                AppearanceNavbarColour navbarColour = PrefsUtility.appearance_navbar_colour(this, this.mSharedPreferences);
                if (navbarColour != AppearanceNavbarColour.BLACK) {
                    TypedArray appearance = obtainStyledAttributes(new int[]{R.attr.colorPrimary, R.attr.colorPrimaryDark});
                    if (navbarColour == AppearanceNavbarColour.PRIMARY) {
                        colour = appearance.getColor(0, General.COLOR_INVALID);
                    } else {
                        colour = appearance.getColor(1, General.COLOR_INVALID);
                    }
                    appearance.recycle();
                    getWindow().setNavigationBarColor(colour);
                }
            }
        }
    }

    public void setBaseActivityContentView(@LayoutRes int layoutResID) {
        FrameLayout frameLayout = this.mContentView;
        if (frameLayout != null) {
            frameLayout.removeAllViews();
            getLayoutInflater().inflate(layoutResID, this.mContentView, true);
            return;
        }
        super.setContentView(layoutResID);
    }

    public void setBaseActivityContentView(@NonNull View view) {
        FrameLayout frameLayout = this.mContentView;
        if (frameLayout != null) {
            frameLayout.removeAllViews();
            this.mContentView.addView(view);
            return;
        }
        super.setContentView(view);
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        setOrientationFromPrefs();
        closeIfNecessary();
        TorCommon.updateTorStatus(this);
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        this.mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void closeIfNecessary() {
        if (!closingAll) {
            return;
        }
        if (this instanceof MainActivity) {
            closingAll = false;
        } else {
            finish();
        }
    }

    public void requestPermissionWithCallback(@NonNull String permission, @NonNull PermissionCallback callback) {
        General.checkThisIsUIThread();
        if (VERSION.SDK_INT < 23) {
            callback.onPermissionGranted();
        } else if (checkSelfPermission(permission) == 0) {
            callback.onPermissionGranted();
        } else {
            int requestCode = this.mPermissionRequestIdGenerator.incrementAndGet();
            this.mPermissionRequestCallbacks.put(Integer.valueOf(requestCode), callback);
            requestPermissions(new String[]{permission}, requestCode);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PermissionCallback callback = (PermissionCallback) this.mPermissionRequestCallbacks.remove(Integer.valueOf(requestCode));
        if (callback == null) {
            return;
        }
        if (permissions.length != 1) {
            throw new RuntimeException("Unexpected permission result");
        } else if (grantResults[0] == 0) {
            callback.onPermissionGranted();
        } else {
            callback.onPermissionDenied();
        }
    }

    private void setOrientationFromPrefs() {
        ScreenOrientation orientation = PrefsUtility.pref_behaviour_screen_orientation(this, this.mSharedPreferences);
        if (orientation == ScreenOrientation.AUTO) {
            setRequestedOrientation(-1);
        } else if (orientation == ScreenOrientation.PORTRAIT) {
            setRequestedOrientation(1);
        } else if (orientation == ScreenOrientation.LANDSCAPE) {
            setRequestedOrientation(0);
        }
    }

    /* access modifiers changed from: protected */
    public void onSharedPreferenceChangedInner(SharedPreferences prefs, String key) {
    }

    public final void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        onSharedPreferenceChangedInner(prefs, key);
        if (key.equals(getString(R.string.pref_menus_optionsmenu_items_key))) {
            invalidateOptionsMenu();
        }
    }
}
