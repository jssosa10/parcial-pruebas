package org.quantumbadger.redreader.activities;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;
import java.util.Locale;
import java.util.UUID;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountChangeListener;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuPostsListener;
import org.quantumbadger.redreader.activities.RefreshableActivity.RefreshableFragment;
import org.quantumbadger.redreader.activities.SessionChangeListener.SessionChangeType;
import org.quantumbadger.redreader.common.DialogUtils;
import org.quantumbadger.redreader.common.DialogUtils.OnSearchListener;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.fragments.PostListingFragment;
import org.quantumbadger.redreader.fragments.SessionListDialog;
import org.quantumbadger.redreader.listingcontrollers.PostListingController;
import org.quantumbadger.redreader.reddit.PostSort;
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager;
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.SubredditSubscriptionState;
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.SubredditSubscriptionStateChangeListener;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit.InvalidSubredditNameException;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL;
import org.quantumbadger.redreader.reddit.url.PostListingURL;
import org.quantumbadger.redreader.reddit.url.RedditURLParser;
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;
import org.quantumbadger.redreader.reddit.url.SearchPostListURL;
import org.quantumbadger.redreader.reddit.url.SubredditPostListURL;
import org.quantumbadger.redreader.reddit.url.SubredditPostListURL.Type;
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener;

public class PostListingActivity extends RefreshableActivity implements RedditAccountChangeListener, PostSelectionListener, OptionsMenuPostsListener, SessionChangeListener, SubredditSubscriptionStateChangeListener {
    private static final String SAVEDSTATE_FRAGMENT = "pla_fragment";
    private static final String SAVEDSTATE_SESSION = "pla_session";
    private static final String SAVEDSTATE_SORT = "pla_sort";
    private PostListingController controller;
    private PostListingFragment fragment;

    public void onCreate(Bundle savedInstanceState) {
        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(obtainStyledAttributes(new int[]{R.attr.rrListBackgroundCol}).getColor(0, 0)));
        RedditAccountManager.getInstance(this).addUpdateListener(this);
        if (getIntent() != null) {
            RedditURL url = RedditURLParser.parseProbablePostListing(getIntent().getData());
            if (url instanceof PostListingURL) {
                this.controller = new PostListingController((PostListingURL) url, this);
                Bundle fragmentSavedInstanceState = null;
                if (savedInstanceState != null) {
                    if (savedInstanceState.containsKey(SAVEDSTATE_SESSION)) {
                        this.controller.setSession(UUID.fromString(savedInstanceState.getString(SAVEDSTATE_SESSION)));
                    }
                    if (savedInstanceState.containsKey(SAVEDSTATE_SORT)) {
                        this.controller.setSort(PostSort.valueOf(savedInstanceState.getString(SAVEDSTATE_SORT)));
                    }
                    if (savedInstanceState.containsKey(SAVEDSTATE_FRAGMENT)) {
                        fragmentSavedInstanceState = savedInstanceState.getBundle(SAVEDSTATE_FRAGMENT);
                    }
                }
                setTitle((CharSequence) url.humanReadableName(this, false));
                setBaseActivityContentView((int) R.layout.main_single);
                doRefresh(RefreshableFragment.POSTS, false, fragmentSavedInstanceState);
                addSubscriptionListener();
                return;
            }
            throw new RuntimeException(String.format(Locale.US, "'%s' is not a post listing URL!", new Object[]{url.generateJsonUri()}));
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
        PostSort sort = this.controller.getSort();
        if (sort != null) {
            outState.putString(SAVEDSTATE_SORT, sort.name());
        }
        PostListingFragment postListingFragment = this.fragment;
        if (postListingFragment != null) {
            outState.putBundle(SAVEDSTATE_FRAGMENT, postListingFragment.onSaveInstanceState());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x005d  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x00c7  */
    public boolean onCreateOptionsMenu(Menu menu) {
        SubredditSubscriptionState subredditSubscriptionState;
        Boolean subredditBlockedState;
        Boolean subredditPinState;
        SubredditPostListURL url;
        RedditAccount user = RedditAccountManager.getInstance(this).getDefaultAccount();
        RedditSubredditSubscriptionManager subredditSubscriptionManager = RedditSubredditSubscriptionManager.getSingleton(this, user);
        if (!user.isAnonymous() && this.controller.isSubreddit() && subredditSubscriptionManager.areSubscriptionsReady()) {
            PostListingFragment postListingFragment = this.fragment;
            if (!(postListingFragment == null || postListingFragment.getSubreddit() == null)) {
                subredditSubscriptionState = subredditSubscriptionManager.getSubscriptionState(this.controller.subredditCanonicalName());
                PostListingFragment postListingFragment2 = this.fragment;
                String subredditDescription = (postListingFragment2 != null || postListingFragment2.getSubreddit() == null) ? null : this.fragment.getSubreddit().description_html;
                if (this.controller.isSubreddit()) {
                    PostListingFragment postListingFragment3 = this.fragment;
                    if (!(postListingFragment3 == null || postListingFragment3.getSubreddit() == null)) {
                        try {
                            subredditBlockedState = Boolean.valueOf(PrefsUtility.pref_blocked_subreddits_check(this, PreferenceManager.getDefaultSharedPreferences(this), this.fragment.getSubreddit().getCanonicalName()));
                            subredditPinState = Boolean.valueOf(PrefsUtility.pref_pinned_subreddits_check(this, PreferenceManager.getDefaultSharedPreferences(this), this.fragment.getSubreddit().getCanonicalName()));
                        } catch (InvalidSubredditNameException e) {
                            subredditPinState = null;
                            subredditBlockedState = null;
                        }
                        RedditSubredditSubscriptionManager redditSubredditSubscriptionManager = subredditSubscriptionManager;
                        RedditAccount redditAccount = user;
                        OptionsMenuUtility.prepare(this, menu, false, true, false, this.controller.isSearchResults(), this.controller.isUserPostListing(), false, this.controller.isSortable(), true, this.controller.isFrontPage(), subredditSubscriptionState, subredditDescription == null && subredditDescription.length() > 0, false, subredditPinState, subredditBlockedState);
                        if (!(this.fragment == null || !this.controller.isRandomSubreddit() || this.fragment.getSubreddit() == null)) {
                            url = SubredditPostListURL.parse(this.controller.getUri());
                            if (url != null && url.type == Type.RANDOM) {
                                this.controller = new PostListingController(url.changeSubreddit(RedditSubreddit.stripRPrefix(this.fragment.getSubreddit().url)), this);
                            }
                        }
                        return true;
                    }
                }
                subredditPinState = null;
                subredditBlockedState = null;
                RedditSubredditSubscriptionManager redditSubredditSubscriptionManager2 = subredditSubscriptionManager;
                RedditAccount redditAccount2 = user;
                OptionsMenuUtility.prepare(this, menu, false, true, false, this.controller.isSearchResults(), this.controller.isUserPostListing(), false, this.controller.isSortable(), true, this.controller.isFrontPage(), subredditSubscriptionState, subredditDescription == null && subredditDescription.length() > 0, false, subredditPinState, subredditBlockedState);
                url = SubredditPostListURL.parse(this.controller.getUri());
                this.controller = new PostListingController(url.changeSubreddit(RedditSubreddit.stripRPrefix(this.fragment.getSubreddit().url)), this);
                return true;
            }
        }
        subredditSubscriptionState = null;
        PostListingFragment postListingFragment22 = this.fragment;
        String subredditDescription2 = (postListingFragment22 != null || postListingFragment22.getSubreddit() == null) ? null : this.fragment.getSubreddit().description_html;
        if (this.controller.isSubreddit()) {
        }
        subredditPinState = null;
        subredditBlockedState = null;
        RedditSubredditSubscriptionManager redditSubredditSubscriptionManager22 = subredditSubscriptionManager;
        RedditAccount redditAccount22 = user;
        OptionsMenuUtility.prepare(this, menu, false, true, false, this.controller.isSearchResults(), this.controller.isUserPostListing(), false, this.controller.isSortable(), true, this.controller.isFrontPage(), subredditSubscriptionState, subredditDescription2 == null && subredditDescription2.length() > 0, false, subredditPinState, subredditBlockedState);
        url = SubredditPostListURL.parse(this.controller.getUri());
        try {
            this.controller = new PostListingController(url.changeSubreddit(RedditSubreddit.stripRPrefix(this.fragment.getSubreddit().url)), this);
            return true;
        } catch (InvalidSubredditNameException e2) {
            throw new RuntimeException(e2);
        }
    }

    private void addSubscriptionListener() {
        RedditSubredditSubscriptionManager.getSingleton(this, RedditAccountManager.getInstance(this).getDefaultAccount()).addListener(this);
    }

    public void onRedditAccountChanged() {
        addSubscriptionListener();
        postInvalidateOptionsMenu();
        requestRefresh(RefreshableFragment.ALL, false);
    }

    /* access modifiers changed from: protected */
    public void doRefresh(RefreshableFragment which, boolean force, Bundle savedInstanceState) {
        PostListingFragment postListingFragment = this.fragment;
        if (postListingFragment != null) {
            postListingFragment.cancel();
        }
        this.fragment = this.controller.get(this, force, savedInstanceState);
        View view = this.fragment.getView();
        setBaseActivityContentView(view);
        General.setLayoutMatchParent(view);
    }

    public void onPostSelected(RedditPreparedPost post) {
        LinkHandler.onLinkClicked(this, post.src.getUrl(), false, post.src.getSrc());
    }

    public void onPostCommentsSelected(RedditPreparedPost post) {
        LinkHandler.onLinkClicked(this, PostCommentListingURL.forPostId(post.src.getIdAlone()).toString(), false);
    }

    public void onRefreshPosts() {
        this.controller.setSession(null);
        requestRefresh(RefreshableFragment.POSTS, true);
    }

    public void onPastPosts() {
        SessionListDialog.newInstance(this.controller.getUri(), this.controller.getSession(), SessionChangeType.POSTS).show(getSupportFragmentManager(), "SessionListDialog");
    }

    public void onSubmitPost() {
        Intent intent = new Intent(this, PostSubmitActivity.class);
        if (this.controller.isSubreddit()) {
            intent.putExtra("subreddit", this.controller.subredditCanonicalName());
        }
        startActivity(intent);
    }

    public void onSortSelected(PostSort order) {
        this.controller.setSort(order);
        requestRefresh(RefreshableFragment.POSTS, false);
    }

    public void onSearchPosts() {
        onSearchPosts(this.controller, this);
    }

    public static void onSearchPosts(final PostListingController controller2, final AppCompatActivity activity) {
        DialogUtils.showSearchDialog(activity, new OnSearchListener() {
            public void onSearch(@Nullable String query) {
                SearchPostListURL url;
                if (query != null) {
                    PostListingController postListingController = controller2;
                    if (postListingController == null || (!postListingController.isSubreddit() && !controller2.isSubredditSearchResults())) {
                        url = SearchPostListURL.build(null, query);
                    } else {
                        url = SearchPostListURL.build(controller2.subredditCanonicalName(), query);
                    }
                    Intent intent = new Intent(activity, PostListingActivity.class);
                    intent.setData(url.generateJsonUri());
                    activity.startActivity(intent);
                }
            }
        });
    }

    public void onSubscribe() {
        this.fragment.onSubscribe();
    }

    public void onUnsubscribe() {
        this.fragment.onUnsubscribe();
    }

    public void onSidebar() {
        if (this.fragment.getSubreddit() != null) {
            Intent intent = new Intent(this, HtmlViewActivity.class);
            intent.putExtra("html", this.fragment.getSubreddit().getSidebarHtml(PrefsUtility.isNightMode(this)));
            intent.putExtra("title", String.format(Locale.US, "%s: %s", new Object[]{getString(R.string.sidebar_activity_title), this.fragment.getSubreddit().url}));
            startActivityForResult(intent, 1);
        }
    }

    public void onPin() {
        if (this.fragment != null) {
            try {
                PrefsUtility.pref_pinned_subreddits_add(this, PreferenceManager.getDefaultSharedPreferences(this), this.fragment.getSubreddit().getCanonicalName());
                invalidateOptionsMenu();
            } catch (InvalidSubredditNameException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void onUnpin() {
        if (this.fragment != null) {
            try {
                PrefsUtility.pref_pinned_subreddits_remove(this, PreferenceManager.getDefaultSharedPreferences(this), this.fragment.getSubreddit().getCanonicalName());
                invalidateOptionsMenu();
            } catch (InvalidSubredditNameException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void onBlock() {
        if (this.fragment != null) {
            try {
                PrefsUtility.pref_blocked_subreddits_add(this, PreferenceManager.getDefaultSharedPreferences(this), this.fragment.getSubreddit().getCanonicalName());
                invalidateOptionsMenu();
            } catch (InvalidSubredditNameException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void onUnblock() {
        if (this.fragment != null) {
            try {
                PrefsUtility.pref_blocked_subreddits_remove(this, PreferenceManager.getDefaultSharedPreferences(this), this.fragment.getSubreddit().getCanonicalName());
                invalidateOptionsMenu();
            } catch (InvalidSubredditNameException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void onSessionSelected(UUID session, SessionChangeType type) {
        this.controller.setSession(session);
        requestRefresh(RefreshableFragment.POSTS, false);
    }

    public void onSessionRefreshSelected(SessionChangeType type) {
        onRefreshPosts();
    }

    public void onSessionChanged(UUID session, SessionChangeType type, long timestamp) {
        this.controller.setSession(session);
    }

    public void onBackPressed() {
        if (General.onBackPressed()) {
            super.onBackPressed();
        }
    }

    public void onSubredditSubscriptionListUpdated(RedditSubredditSubscriptionManager subredditSubscriptionManager) {
        postInvalidateOptionsMenu();
    }

    public void onSubredditSubscriptionAttempted(RedditSubredditSubscriptionManager subredditSubscriptionManager) {
        postInvalidateOptionsMenu();
    }

    public void onSubredditUnsubscriptionAttempted(RedditSubredditSubscriptionManager subredditSubscriptionManager) {
        postInvalidateOptionsMenu();
    }

    private void postInvalidateOptionsMenu() {
        runOnUiThread(new Runnable() {
            public void run() {
                PostListingActivity.this.invalidateOptionsMenu();
            }
        });
    }
}
