package org.quantumbadger.redreader.fragments;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.ChangelogManager;

public final class ChangelogDialog extends PropertiesDialog {
    public static ChangelogDialog newInstance() {
        return new ChangelogDialog();
    }

    /* access modifiers changed from: protected */
    public String getTitle(Context context) {
        return context.getString(R.string.title_changelog);
    }

    /* access modifiers changed from: protected */
    public void prepare(AppCompatActivity context, LinearLayout items) {
        ChangelogManager.generateViews(context, items, false);
    }
}
