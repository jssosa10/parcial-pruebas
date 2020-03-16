package org.quantumbadger.redreader.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import java.util.EnumSet;
import java.util.Iterator;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;

public abstract class RefreshableActivity extends BaseActivity {
    /* access modifiers changed from: private */
    public boolean paused = false;
    /* access modifiers changed from: private */
    public final EnumSet<RefreshableFragment> refreshOnResume = EnumSet.noneOf(RefreshableFragment.class);

    public enum RefreshableFragment {
        MAIN,
        MAIN_RELAYOUT,
        POSTS,
        COMMENTS,
        RESTART,
        ALL
    }

    /* access modifiers changed from: protected */
    public abstract void doRefresh(RefreshableFragment refreshableFragment, boolean z, Bundle bundle);

    /* access modifiers changed from: protected */
    public void onPause() {
        super.onPause();
        this.paused = true;
    }

    /* access modifiers changed from: protected */
    public void onSharedPreferenceChangedInner(SharedPreferences prefs, String key) {
        if (PrefsUtility.isRestartRequired(this, key)) {
            requestRefresh(RefreshableFragment.RESTART, false);
        } else if ((this instanceof MainActivity) && PrefsUtility.isReLayoutRequired(this, key)) {
            requestRefresh(RefreshableFragment.MAIN_RELAYOUT, false);
        } else if (PrefsUtility.isRefreshRequired(this, key)) {
            requestRefresh(RefreshableFragment.ALL, false);
        } else {
            if ((this instanceof MainActivity) && (key.equals(getString(R.string.pref_pinned_subreddits_key)) || key.equals(getString(R.string.pref_blocked_subreddits_key)))) {
                requestRefresh(RefreshableFragment.MAIN, false);
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        this.paused = false;
        if (!this.refreshOnResume.isEmpty()) {
            Iterator it = this.refreshOnResume.iterator();
            while (it.hasNext()) {
                doRefreshNow((RefreshableFragment) it.next(), false);
            }
            this.refreshOnResume.clear();
        }
    }

    /* access modifiers changed from: protected */
    public void doRefreshNow(RefreshableFragment which, boolean force) {
        if (which == RefreshableFragment.RESTART) {
            General.recreateActivityNoAnimation(null);
        } else {
            doRefresh(which, force, null);
        }
    }

    public final void requestRefresh(final RefreshableFragment which, final boolean force) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (!RefreshableActivity.this.paused) {
                    RefreshableActivity.this.doRefreshNow(which, force);
                } else {
                    RefreshableActivity.this.refreshOnResume.add(which);
                }
            }
        });
    }
}
