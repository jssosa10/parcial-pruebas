package org.quantumbadger.redreader.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import java.util.UUID;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccountChangeListener;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuCommentsListener;
import org.quantumbadger.redreader.activities.RefreshableActivity.RefreshableFragment;
import org.quantumbadger.redreader.activities.SessionChangeListener.SessionChangeType;
import org.quantumbadger.redreader.common.DialogUtils;
import org.quantumbadger.redreader.common.DialogUtils.OnSearchListener;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.fragments.CommentListingFragment;
import org.quantumbadger.redreader.fragments.SessionListDialog;
import org.quantumbadger.redreader.listingcontrollers.CommentListingController;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL.Sort;
import org.quantumbadger.redreader.reddit.url.RedditURLParser;
import org.quantumbadger.redreader.reddit.url.UserCommentListingURL;
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener;

public class CommentListingActivity extends RefreshableActivity implements RedditAccountChangeListener, OptionsMenuCommentsListener, PostSelectionListener, SessionChangeListener {
    public static final String EXTRA_SEARCH_STRING = "cla_search_string";
    private static final String SAVEDSTATE_FRAGMENT = "cla_fragment";
    private static final String SAVEDSTATE_SESSION = "cla_session";
    private static final String SAVEDSTATE_SORT = "cla_sort";
    private static final String TAG = "CommentListingActivity";
    private CommentListingController controller;
    private CommentListingFragment mFragment;

    public void onCreate(Bundle savedInstanceState) {
        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);
        setTitle((CharSequence) getString(R.string.app_name));
        RedditAccountManager.getInstance(this).addUpdateListener(this);
        if (getIntent() != null) {
            Intent intent = getIntent();
            String url = intent.getDataString();
            String searchString = intent.getStringExtra(EXTRA_SEARCH_STRING);
            this.controller = new CommentListingController(RedditURLParser.parseProbableCommentListing(Uri.parse(url)), this);
            this.controller.setSearchString(searchString);
            Bundle fragmentSavedInstanceState = null;
            if (savedInstanceState != null) {
                if (savedInstanceState.containsKey(SAVEDSTATE_SESSION)) {
                    this.controller.setSession(UUID.fromString(savedInstanceState.getString(SAVEDSTATE_SESSION)));
                }
                if (savedInstanceState.containsKey(SAVEDSTATE_SORT)) {
                    this.controller.setSort(Sort.valueOf(savedInstanceState.getString(SAVEDSTATE_SORT)));
                }
                if (savedInstanceState.containsKey(SAVEDSTATE_FRAGMENT)) {
                    fragmentSavedInstanceState = savedInstanceState.getBundle(SAVEDSTATE_FRAGMENT);
                }
            }
            doRefresh(RefreshableFragment.COMMENTS, false, fragmentSavedInstanceState);
            return;
        }
        throw new RuntimeException("Nothing to show!");
    }

    /* access modifiers changed from: protected */
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        UUID session = this.controller.getSession();
        if (session != null) {
            outState.putString(SAVEDSTATE_SESSION, session.toString());
        }
        Sort sort = this.controller.getSort();
        if (sort != null) {
            outState.putString(SAVEDSTATE_SORT, sort.name());
        }
        CommentListingFragment commentListingFragment = this.mFragment;
        if (commentListingFragment != null) {
            outState.putBundle(SAVEDSTATE_FRAGMENT, commentListingFragment.onSaveInstanceState());
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        OptionsMenuUtility.prepare(this, menu, false, false, true, false, false, this.controller.isUserCommentListing(), false, this.controller.isSortable(), false, null, false, true, null, null);
        CommentListingFragment commentListingFragment = this.mFragment;
        if (commentListingFragment != null) {
            commentListingFragment.onCreateOptionsMenu(menu);
        } else {
            Menu menu2 = menu;
        }
        return true;
    }

    public void onRedditAccountChanged() {
        requestRefresh(RefreshableFragment.ALL, false);
    }

    /* access modifiers changed from: protected */
    public void doRefresh(RefreshableFragment which, boolean force, Bundle savedInstanceState) {
        this.mFragment = this.controller.get(this, force, savedInstanceState);
        View view = this.mFragment.getView();
        setBaseActivityContentView(view);
        General.setLayoutMatchParent(view);
        setTitle((CharSequence) this.controller.getCommentListingUrl().humanReadableName(this, false));
        invalidateOptionsMenu();
    }

    public void onRefreshComments() {
        this.controller.setSession(null);
        requestRefresh(RefreshableFragment.COMMENTS, true);
    }

    public void onPastComments() {
        SessionListDialog.newInstance(this.controller.getUri(), this.controller.getSession(), SessionChangeType.COMMENTS).show(getSupportFragmentManager(), (String) null);
    }

    public void onSortSelected(Sort order) {
        this.controller.setSort(order);
        requestRefresh(RefreshableFragment.COMMENTS, false);
    }

    public void onSortSelected(UserCommentListingURL.Sort order) {
        this.controller.setSort(order);
        requestRefresh(RefreshableFragment.COMMENTS, false);
    }

    public void onSearchComments() {
        DialogUtils.showSearchDialog(this, new OnSearchListener() {
            public void onSearch(@Nullable String query) {
                Intent searchIntent = CommentListingActivity.this.getIntent();
                searchIntent.putExtra(CommentListingActivity.EXTRA_SEARCH_STRING, query);
                CommentListingActivity.this.startActivity(searchIntent);
            }
        });
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        CommentListingFragment commentListingFragment = this.mFragment;
        if (commentListingFragment == null || !commentListingFragment.onOptionsItemSelected(item)) {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void onSessionRefreshSelected(SessionChangeType type) {
        onRefreshComments();
    }

    public void onSessionSelected(UUID session, SessionChangeType type) {
        this.controller.setSession(session);
        requestRefresh(RefreshableFragment.COMMENTS, false);
    }

    public void onSessionChanged(UUID session, SessionChangeType type, long timestamp) {
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append(type.name());
        sb.append(" session changed to ");
        sb.append(session != null ? session.toString() : "<null>");
        Log.i(str, sb.toString());
        this.controller.setSession(session);
    }

    public void onPostSelected(RedditPreparedPost post) {
        LinkHandler.onLinkClicked(this, post.src.getUrl(), false, post.src.getSrc());
    }

    public void onPostCommentsSelected(RedditPreparedPost post) {
        LinkHandler.onLinkClicked(this, PostCommentListingURL.forPostId(post.src.getIdAlone()).toString(), false);
    }

    public void onBackPressed() {
        if (General.onBackPressed()) {
            super.onBackPressed();
        }
    }
}
