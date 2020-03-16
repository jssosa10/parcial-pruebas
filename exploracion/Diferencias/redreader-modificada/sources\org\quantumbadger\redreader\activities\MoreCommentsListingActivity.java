package org.quantumbadger.redreader.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.Iterator;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccountChangeListener;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuCommentsListener;
import org.quantumbadger.redreader.activities.RefreshableActivity.RefreshableFragment;
import org.quantumbadger.redreader.common.DialogUtils;
import org.quantumbadger.redreader.common.DialogUtils.OnSearchListener;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.fragments.CommentListingFragment;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL.Sort;
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;
import org.quantumbadger.redreader.reddit.url.UserCommentListingURL;
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener;

public class MoreCommentsListingActivity extends RefreshableActivity implements RedditAccountChangeListener, OptionsMenuCommentsListener, PostSelectionListener {
    private static final String EXTRA_SEARCH_STRING = "mcla_search_string";
    private CommentListingFragment mFragment;
    private FrameLayout mPane;
    private String mSearchString = null;
    private final ArrayList<RedditURL> mUrls = new ArrayList<>(32);

    public void onCreate(Bundle savedInstanceState) {
        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);
        setTitle((int) R.string.app_name);
        View layout = getLayoutInflater().inflate(R.layout.main_single, null);
        setBaseActivityContentView(layout);
        this.mPane = (FrameLayout) layout.findViewById(R.id.main_single_frame);
        RedditAccountManager.getInstance(this).addUpdateListener(this);
        if (getIntent() != null) {
            Intent intent = getIntent();
            this.mSearchString = intent.getStringExtra(EXTRA_SEARCH_STRING);
            ArrayList<String> commentIds = intent.getStringArrayListExtra("commentIds");
            String postId = intent.getStringExtra("postId");
            Iterator it = commentIds.iterator();
            while (it.hasNext()) {
                this.mUrls.add(PostCommentListingURL.forPostId(postId).commentId((String) it.next()));
            }
            doRefresh(RefreshableFragment.COMMENTS, false, null);
            return;
        }
        throw new RuntimeException("Nothing to show! (should load from bundle)");
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        OptionsMenuUtility.prepare(this, menu, false, false, true, false, false, false, false, false, false, null, false, false, null, null);
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
        CommentListingFragment commentListingFragment = new CommentListingFragment(this, savedInstanceState, this.mUrls, null, this.mSearchString, force);
        this.mFragment = commentListingFragment;
        this.mPane.removeAllViews();
        View view = this.mFragment.getView();
        this.mPane.addView(view);
        General.setLayoutMatchParent(view);
        setTitle((CharSequence) "More Comments");
    }

    public void onRefreshComments() {
        requestRefresh(RefreshableFragment.COMMENTS, true);
    }

    public void onPastComments() {
    }

    public void onSortSelected(Sort order) {
    }

    public void onSortSelected(UserCommentListingURL.Sort order) {
    }

    public void onSearchComments() {
        DialogUtils.showSearchDialog(this, new OnSearchListener() {
            public void onSearch(@Nullable String query) {
                Intent searchIntent = MoreCommentsListingActivity.this.getIntent();
                searchIntent.putExtra(MoreCommentsListingActivity.EXTRA_SEARCH_STRING, query);
                MoreCommentsListingActivity.this.startActivity(searchIntent);
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
