package org.quantumbadger.redreader.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.ChangelogManager;
import org.quantumbadger.redreader.common.PrefsUtility;

public class ChangelogActivity extends BaseActivity {
    /* access modifiers changed from: protected */
    public boolean baseActivityIsToolbarActionBarEnabled() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        PrefsUtility.applySettingsTheme(this);
        super.onCreate(savedInstanceState);
        getSupportActionBarOrThrow().setTitle((int) R.string.title_changelog);
        getSupportActionBarOrThrow().setHomeButtonEnabled(true);
        getSupportActionBarOrThrow().setDisplayHomeAsUpEnabled(true);
        LinearLayout items = new LinearLayout(this);
        items.setOrientation(1);
        ChangelogManager.generateViews(this, items, true);
        ScrollView sv = new ScrollView(this);
        sv.addView(items);
        setBaseActivityContentView((View) sv);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != 16908332) {
            return false;
        }
        finish();
        return true;
    }
}
