package org.quantumbadger.redreader.settings;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity.Header;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import java.util.List;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.PrefsUtility.ScreenOrientation;

public final class SettingsActivity extends AppCompatPreferenceActivity implements OnSharedPreferenceChangeListener {
    private SharedPreferences sharedPreferences;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        getWindow().requestFeature(8);
        PrefsUtility.applySettingsTheme(this);
        super.onCreate(savedInstanceState);
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        setOrientationFromPrefs();
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.prefheaders, target);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return false;
        }
        finish();
        return true;
    }

    private void setOrientationFromPrefs() {
        ScreenOrientation orientation = PrefsUtility.pref_behaviour_screen_orientation(this, this.sharedPreferences);
        if (orientation == ScreenOrientation.AUTO) {
            setRequestedOrientation(-1);
        } else if (orientation == ScreenOrientation.PORTRAIT) {
            setRequestedOrientation(1);
        } else if (orientation == ScreenOrientation.LANDSCAPE) {
            setRequestedOrientation(0);
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(getString(R.string.pref_behaviour_screenorientation_key))) {
            setOrientationFromPrefs();
        }
    }

    /* access modifiers changed from: protected */
    public boolean isValidFragment(String fragmentName) {
        return fragmentName.equals(SettingsFragment.class.getCanonicalName());
    }
}
