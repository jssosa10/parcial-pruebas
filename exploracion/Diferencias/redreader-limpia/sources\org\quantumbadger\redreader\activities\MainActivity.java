package org.quantumbadger.redreader.activities;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountChangeListener;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuCommentsListener;
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuPostsListener;
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuSubredditsListener;
import org.quantumbadger.redreader.activities.RefreshableActivity.RefreshableFragment;
import org.quantumbadger.redreader.activities.SessionChangeListener.SessionChangeType;
import org.quantumbadger.redreader.adapters.MainMenuSelectionListener;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.common.DialogUtils;
import org.quantumbadger.redreader.common.DialogUtils.OnSearchListener;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.fragments.AccountListDialog;
import org.quantumbadger.redreader.fragments.ChangelogDialog;
import org.quantumbadger.redreader.fragments.CommentListingFragment;
import org.quantumbadger.redreader.fragments.MainMenuFragment;
import org.quantumbadger.redreader.fragments.PostListingFragment;
import org.quantumbadger.redreader.fragments.SessionListDialog;
import org.quantumbadger.redreader.listingcontrollers.CommentListingController;
import org.quantumbadger.redreader.listingcontrollers.PostListingController;
import org.quantumbadger.redreader.reddit.PostSort;
import org.quantumbadger.redreader.reddit.RedditSubredditHistory;
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager;
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.SubredditSubscriptionState;
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.SubredditSubscriptionStateChangeListener;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit.InvalidSubredditNameException;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL.Sort;
import org.quantumbadger.redreader.reddit.url.PostListingURL;
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;
import org.quantumbadger.redreader.reddit.url.SearchPostListURL;
import org.quantumbadger.redreader.reddit.url.SubredditPostListURL;
import org.quantumbadger.redreader.reddit.url.UserCommentListingURL;
import org.quantumbadger.redreader.reddit.url.UserPostListingURL;
import org.quantumbadger.redreader.reddit.url.UserProfileURL;
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener;

public class MainActivity extends RefreshableActivity implements MainMenuSelectionListener, RedditAccountChangeListener, PostSelectionListener, OptionsMenuSubredditsListener, OptionsMenuPostsListener, OptionsMenuCommentsListener, SessionChangeListener, SubredditSubscriptionStateChangeListener {
    /* access modifiers changed from: private */
    public CommentListingController commentListingController;
    private CommentListingFragment commentListingFragment;
    private View commentListingView;
    private boolean isMenuShown = true;
    private FrameLayout mLeftPane;
    private FrameLayout mRightPane;
    private FrameLayout mSinglePane;
    private MainMenuFragment mainMenuFragment;
    private View mainMenuView;
    private PostListingController postListingController;
    private PostListingFragment postListingFragment;
    private View postListingView;
    private SharedPreferences sharedPreferences;
    private boolean twoPane;

    /* access modifiers changed from: protected */
    public boolean baseActivityIsActionBarBackEnabled() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);
        if (isTaskRoot() || !getIntent().hasCategory("android.intent.category.LAUNCHER") || getIntent().getAction() == null || !getIntent().getAction().equals("android.intent.action.MAIN")) {
            this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            if (savedInstanceState == null && PrefsUtility.pref_behaviour_skiptofrontpage(this, this.sharedPreferences)) {
                onSelected((PostListingURL) SubredditPostListURL.getFrontPage());
            }
            setTitle((int) R.string.app_name);
            this.twoPane = General.isTablet(this, this.sharedPreferences);
            doRefresh(RefreshableFragment.MAIN_RELAYOUT, false, null);
            RedditAccountManager.getInstance(this).addUpdateListener(this);
            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                int appVersion = pInfo.versionCode;
                if (!this.sharedPreferences.contains("firstRunMessageShown")) {
                    new Builder(this).setTitle(R.string.firstrun_login_title).setMessage(R.string.firstrun_login_message).setPositiveButton(R.string.firstrun_login_button_now, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            new AccountListDialog().show(MainActivity.this.getSupportFragmentManager(), (String) null);
                        }
                    }).setNegativeButton(R.string.firstrun_login_button_later, null).show();
                    Editor edit = this.sharedPreferences.edit();
                    edit.putString("firstRunMessageShown", "true");
                    edit.apply();
                } else if (this.sharedPreferences.contains("lastVersion")) {
                    int lastVersion = this.sharedPreferences.getInt("lastVersion", 0);
                    if (lastVersion < 63) {
                        new Builder(this).setTitle(R.string.firstrun_login_title).setMessage(R.string.upgrade_v190_login_message).setPositiveButton(R.string.firstrun_login_button_now, new OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                new AccountListDialog().show(MainActivity.this.getSupportFragmentManager(), (String) null);
                            }
                        }).setNegativeButton(R.string.firstrun_login_button_later, null).show();
                    }
                    if (lastVersion != appVersion) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Updated to version ");
                        sb.append(pInfo.versionName);
                        General.quickToast((Context) this, sb.toString());
                        this.sharedPreferences.edit().putInt("lastVersion", appVersion).apply();
                        ChangelogDialog.newInstance().show(getSupportFragmentManager(), (String) null);
                        if (lastVersion <= 51) {
                            Set<String> existingCommentHeaderItems = PrefsUtility.getStringSet(R.string.pref_appearance_comment_header_items_key, R.array.pref_appearance_comment_header_items_default, this, this.sharedPreferences);
                            existingCommentHeaderItems.add("gold");
                            this.sharedPreferences.edit().putStringSet(getString(R.string.pref_appearance_comment_header_items_key), existingCommentHeaderItems).apply();
                            new Thread() {
                                public void run() {
                                    CacheManager.getInstance(MainActivity.this).emptyTheWholeCache();
                                }
                            }.start();
                        }
                        if (lastVersion <= 76) {
                            Set<String> existingPostContextItems = PrefsUtility.getStringSet(R.string.pref_menus_post_context_items_key, R.array.pref_menus_post_context_items_return, this, this.sharedPreferences);
                            existingPostContextItems.add("share_image");
                            this.sharedPreferences.edit().putStringSet(getString(R.string.pref_menus_post_context_items_key), existingPostContextItems).apply();
                        }
                        if (lastVersion <= 77) {
                            Set<String> existingPostContextItems2 = PrefsUtility.getStringSet(R.string.pref_menus_post_context_items_key, R.array.pref_menus_post_context_items_return, this, this.sharedPreferences);
                            existingPostContextItems2.add("edit");
                            existingPostContextItems2.add("pin");
                            existingPostContextItems2.add("subscribe");
                            existingPostContextItems2.add("block");
                            this.sharedPreferences.edit().putStringSet(getString(R.string.pref_menus_post_context_items_key), existingPostContextItems2).apply();
                        }
                        if (lastVersion <= 84) {
                            Set<String> existingShortcutPreferences = PrefsUtility.getStringSet(R.string.pref_menus_mainmenu_shortcutitems_key, R.array.pref_menus_mainmenu_shortcutitems_items_default, this, this.sharedPreferences);
                            if (PrefsUtility.pref_show_popular_main_menu(this, this.sharedPreferences)) {
                                existingShortcutPreferences.add("popular");
                            }
                            if (PrefsUtility.pref_show_random_main_menu(this, this.sharedPreferences)) {
                                existingShortcutPreferences.add("random");
                            }
                            this.sharedPreferences.edit().putStringSet(getString(R.string.pref_menus_mainmenu_shortcutitems_key), existingShortcutPreferences).apply();
                        }
                    }
                } else {
                    this.sharedPreferences.edit().putInt("lastVersion", appVersion).apply();
                    ChangelogDialog.newInstance().show(getSupportFragmentManager(), (String) null);
                }
                addSubscriptionListener();
                if (Boolean.valueOf(getIntent().getBooleanExtra("isNewMessage", false)).booleanValue()) {
                    startActivity(new Intent(this, InboxListingActivity.class));
                }
            } catch (NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            finish();
        }
    }

    private void addSubscriptionListener() {
        RedditSubredditSubscriptionManager.getSingleton(this, RedditAccountManager.getInstance(this).getDefaultAccount()).addListener(this);
    }

    public void onSelected(int type) {
        String username = RedditAccountManager.getInstance(this).getDefaultAccount().username;
        switch (type) {
            case 0:
                onSelected((PostListingURL) SubredditPostListURL.getFrontPage());
                return;
            case 1:
                LinkHandler.onLinkClicked(this, new UserProfileURL(username).toString());
                return;
            case 2:
                startActivity(new Intent(this, InboxListingActivity.class));
                return;
            case 3:
                onSelected((PostListingURL) UserPostListingURL.getSubmitted(username));
                return;
            case 4:
                onSelected((PostListingURL) UserPostListingURL.getLiked(username));
                return;
            case 5:
                onSelected((PostListingURL) UserPostListingURL.getDisliked(username));
                return;
            case 6:
                onSelected((PostListingURL) UserPostListingURL.getSaved(username));
                return;
            case 7:
                Intent intent = new Intent(this, InboxListingActivity.class);
                intent.putExtra("modmail", true);
                startActivity(intent);
                return;
            case 8:
                onSelected((PostListingURL) UserPostListingURL.getHidden(username));
                return;
            case 9:
                Builder alertBuilder = new Builder(this);
                View root = getLayoutInflater().inflate(R.layout.dialog_mainmenu_custom, null);
                final Spinner destinationType = (Spinner) root.findViewById(R.id.dialog_mainmenu_custom_type);
                final AutoCompleteTextView editText = (AutoCompleteTextView) root.findViewById(R.id.dialog_mainmenu_custom_value);
                final String[] typeReturnValues = getResources().getStringArray(R.array.mainmenu_custom_destination_type_return);
                ArrayAdapter arrayAdapter = new ArrayAdapter(this, 17367050, RedditSubredditHistory.getSubredditsSorted(RedditAccountManager.getInstance(this).getDefaultAccount()).toArray(new String[0]));
                editText.setAdapter(arrayAdapter);
                editText.setOnEditorActionListener(new OnEditorActionListener() {
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId != 2) {
                            return false;
                        }
                        MainActivity.this.openCustomLocation(typeReturnValues, destinationType, editText);
                        return true;
                    }
                });
                alertBuilder.setView(root);
                final String[] strArr = typeReturnValues;
                final Spinner spinner = destinationType;
                final AutoCompleteTextView autoCompleteTextView = editText;
                final ArrayAdapter arrayAdapter2 = arrayAdapter;
                AnonymousClass5 r0 = new OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        String typeName = strArr[spinner.getSelectedItemPosition()];
                        if (((typeName.hashCode() == 487638174 && typeName.equals("subreddit")) ? (char) 0 : 65535) != 0) {
                            autoCompleteTextView.setAdapter(null);
                        } else {
                            autoCompleteTextView.setAdapter(arrayAdapter2);
                        }
                    }

                    public void onNothingSelected(AdapterView<?> adapterView) {
                        autoCompleteTextView.setAdapter(null);
                    }
                };
                destinationType.setOnItemSelectedListener(r0);
                alertBuilder.setPositiveButton(R.string.dialog_go, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.openCustomLocation(typeReturnValues, destinationType, editText);
                    }
                });
                alertBuilder.setNegativeButton(R.string.dialog_cancel, null);
                AlertDialog alertDialog = alertBuilder.create();
                alertDialog.getWindow().setSoftInputMode(4);
                alertDialog.show();
                return;
            case 10:
                onSelected((PostListingURL) SubredditPostListURL.getAll());
                return;
            case 11:
                onSelected((PostListingURL) SubredditPostListURL.getPopular());
                return;
            case 12:
                onSelected((PostListingURL) SubredditPostListURL.getRandom());
                return;
            case 13:
                onSelected((PostListingURL) SubredditPostListURL.getRandomNsfw());
                return;
            default:
                return;
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x004d  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x007f  */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x0090  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x00ec  */
    /* JADX WARNING: Removed duplicated region for block: B:54:? A[RETURN, SYNTHETIC] */
    public void openCustomLocation(String[] typeReturnValues, Spinner destinationType, AutoCompleteTextView editText) {
        char c;
        String typeName = typeReturnValues[destinationType.getSelectedItemPosition()];
        int hashCode = typeName.hashCode();
        if (hashCode == -906336856) {
            if (typeName.equals("search")) {
                c = 3;
                switch (c) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                }
            }
        } else if (hashCode == 116079) {
            if (typeName.equals("url")) {
                c = 2;
                switch (c) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                }
            }
        } else if (hashCode == 3599307) {
            if (typeName.equals("user")) {
                c = 1;
                switch (c) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                }
            }
        } else if (hashCode == 487638174 && typeName.equals("subreddit")) {
            c = 0;
            switch (c) {
                case 0:
                    try {
                        RedditURL redditURL = SubredditPostListURL.getSubreddit(RedditSubreddit.stripRPrefix(editText.getText().toString().trim().replace(StringUtils.SPACE, "")));
                        if (redditURL != null) {
                            if (redditURL.pathType() == 0) {
                                onSelected((PostListingURL) redditURL.asSubredditPostListURL());
                                return;
                            }
                        }
                        General.quickToast((Context) this, (int) R.string.mainmenu_custom_invalid_name);
                        return;
                    } catch (InvalidSubredditNameException e) {
                        General.quickToast((Context) this, (int) R.string.mainmenu_custom_invalid_name);
                        return;
                    }
                case 1:
                    String userInput = editText.getText().toString().trim().replace(StringUtils.SPACE, "");
                    if (!userInput.startsWith("/u/") && !userInput.startsWith("/user/")) {
                        if (userInput.startsWith("u/") || userInput.startsWith("user/")) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("/");
                            sb.append(userInput);
                            userInput = sb.toString();
                        } else {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("/u/");
                            sb2.append(userInput);
                            userInput = sb2.toString();
                        }
                    }
                    LinkHandler.onLinkClicked(this, userInput);
                    return;
                case 2:
                    LinkHandler.onLinkClicked(this, editText.getText().toString().trim());
                    return;
                case 3:
                    String query = editText.getText().toString().trim();
                    if (StringUtils.isEmpty(query)) {
                        General.quickToast((Context) this, (int) R.string.mainmenu_custom_empty_search_query);
                        return;
                    }
                    SearchPostListURL url = SearchPostListURL.build(null, query);
                    Intent intent = new Intent(this, PostListingActivity.class);
                    intent.setData(url.generateJsonUri());
                    startActivity(intent);
                    return;
                default:
                    return;
            }
        }
        c = 65535;
        switch (c) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
        }
    }

    public void onSelected(PostListingURL url) {
        if (url != null) {
            if (this.twoPane) {
                this.postListingController = new PostListingController(url, this);
                requestRefresh(RefreshableFragment.POSTS, false);
            } else {
                Intent intent = new Intent(this, PostListingActivity.class);
                intent.setData(url.generateJsonUri());
                startActivityForResult(intent, 1);
            }
        }
    }

    public void onRedditAccountChanged() {
        addSubscriptionListener();
        postInvalidateOptionsMenu();
        requestRefresh(RefreshableFragment.ALL, false);
    }

    /* access modifiers changed from: protected */
    public void doRefresh(RefreshableFragment which, boolean force, Bundle savedInstanceState) {
        View layout;
        if (which == RefreshableFragment.MAIN_RELAYOUT) {
            this.mainMenuFragment = null;
            this.postListingFragment = null;
            this.commentListingFragment = null;
            this.mainMenuView = null;
            this.postListingView = null;
            this.commentListingView = null;
            FrameLayout frameLayout = this.mLeftPane;
            if (frameLayout != null) {
                frameLayout.removeAllViews();
            }
            FrameLayout frameLayout2 = this.mRightPane;
            if (frameLayout2 != null) {
                frameLayout2.removeAllViews();
            }
            this.twoPane = General.isTablet(this, this.sharedPreferences);
            if (this.twoPane) {
                layout = getLayoutInflater().inflate(R.layout.main_double, null);
                this.mLeftPane = (FrameLayout) layout.findViewById(R.id.main_left_frame);
                this.mRightPane = (FrameLayout) layout.findViewById(R.id.main_right_frame);
                this.mSinglePane = null;
            } else {
                layout = getLayoutInflater().inflate(R.layout.main_single, null);
                this.mLeftPane = null;
                this.mRightPane = null;
                this.mSinglePane = (FrameLayout) layout.findViewById(R.id.main_single_frame);
            }
            setBaseActivityContentView(layout);
            invalidateOptionsMenu();
            requestRefresh(RefreshableFragment.ALL, false);
            return;
        }
        if (this.twoPane) {
            FrameLayout postContainer = this.isMenuShown ? this.mRightPane : this.mLeftPane;
            if (this.isMenuShown && (which == RefreshableFragment.ALL || which == RefreshableFragment.MAIN)) {
                this.mainMenuFragment = new MainMenuFragment(this, null, force);
                this.mainMenuView = this.mainMenuFragment.getView();
                this.mLeftPane.removeAllViews();
                this.mLeftPane.addView(this.mainMenuView);
            }
            if (this.postListingController != null && (which == RefreshableFragment.ALL || which == RefreshableFragment.POSTS)) {
                if (force) {
                    PostListingFragment postListingFragment2 = this.postListingFragment;
                    if (postListingFragment2 != null) {
                        postListingFragment2.cancel();
                    }
                }
                this.postListingFragment = this.postListingController.get(this, force, null);
                this.postListingView = this.postListingFragment.getView();
                postContainer.removeAllViews();
                postContainer.addView(this.postListingView);
            }
            if (this.commentListingController != null && (which == RefreshableFragment.ALL || which == RefreshableFragment.COMMENTS)) {
                this.commentListingFragment = this.commentListingController.get(this, force, null);
                this.commentListingView = this.commentListingFragment.getView();
                this.mRightPane.removeAllViews();
                this.mRightPane.addView(this.commentListingView);
            }
        } else if (which == RefreshableFragment.ALL || which == RefreshableFragment.MAIN) {
            this.mainMenuFragment = new MainMenuFragment(this, null, force);
            this.mainMenuView = this.mainMenuFragment.getView();
            this.mSinglePane.removeAllViews();
            this.mSinglePane.addView(this.mainMenuView);
        }
        invalidateOptionsMenu();
    }

    public void onBackPressed() {
        if (General.onBackPressed()) {
            if (!this.twoPane || this.isMenuShown) {
                super.onBackPressed();
                return;
            }
            this.isMenuShown = true;
            this.mainMenuFragment = new MainMenuFragment(this, null, false);
            this.mainMenuView = this.mainMenuFragment.getView();
            this.commentListingFragment = null;
            this.commentListingView = null;
            this.mLeftPane.removeAllViews();
            this.mRightPane.removeAllViews();
            this.mLeftPane.addView(this.mainMenuView);
            this.mRightPane.addView(this.postListingView);
            showBackButton(false);
            invalidateOptionsMenu();
        }
    }

    public void onPostCommentsSelected(RedditPreparedPost post) {
        if (this.twoPane) {
            this.commentListingController = new CommentListingController(PostCommentListingURL.forPostId(post.src.getIdAlone()), this);
            showBackButton(true);
            if (this.isMenuShown) {
                this.commentListingFragment = this.commentListingController.get(this, false, null);
                this.commentListingView = this.commentListingFragment.getView();
                this.mLeftPane.removeAllViews();
                this.mRightPane.removeAllViews();
                this.mLeftPane.addView(this.postListingView);
                this.mRightPane.addView(this.commentListingView);
                this.mainMenuFragment = null;
                this.mainMenuView = null;
                this.isMenuShown = false;
                invalidateOptionsMenu();
                return;
            }
            requestRefresh(RefreshableFragment.COMMENTS, false);
            return;
        }
        LinkHandler.onLinkClicked(this, PostCommentListingURL.forPostId(post.src.getIdAlone()).toString(), false);
    }

    public void onPostSelected(RedditPreparedPost post) {
        if (post.isSelf()) {
            onPostCommentsSelected(post);
        } else {
            LinkHandler.onLinkClicked(this, post.src.getUrl(), false, post.src.getSrc());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:55:0x00ca  */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x00e3 A[ADDED_TO_REGION] */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x010c  */
    /* JADX WARNING: Removed duplicated region for block: B:69:0x0112  */
    public boolean onCreateOptionsMenu(Menu menu) {
        SubredditSubscriptionState subredditSubscriptionState;
        Boolean subredditBlockedState;
        Boolean subredditBlockedState2;
        CommentListingFragment commentListingFragment2;
        boolean postsVisible = this.postListingFragment != null;
        boolean commentsVisible = this.commentListingFragment != null;
        PostListingController postListingController2 = this.postListingController;
        boolean postsSortable = postListingController2 != null && postListingController2.isSortable();
        CommentListingController commentListingController2 = this.commentListingController;
        boolean commentsSortable = commentListingController2 != null && commentListingController2.isSortable();
        PostListingController postListingController3 = this.postListingController;
        boolean isFrontPage = postListingController3 != null && postListingController3.isFrontPage();
        RedditAccount user = RedditAccountManager.getInstance(this).getDefaultAccount();
        RedditSubredditSubscriptionManager subredditSubscriptionManager = RedditSubredditSubscriptionManager.getSingleton(this, user);
        if (postsVisible && !user.isAnonymous() && this.postListingController.isSubreddit() && subredditSubscriptionManager.areSubscriptionsReady()) {
            PostListingFragment postListingFragment2 = this.postListingFragment;
            if (!(postListingFragment2 == null || postListingFragment2.getSubreddit() == null)) {
                subredditSubscriptionState = subredditSubscriptionManager.getSubscriptionState(this.postListingController.subredditCanonicalName());
                if (postsVisible && this.postListingController.isSubreddit()) {
                    PostListingFragment postListingFragment3 = this.postListingFragment;
                    if (!(postListingFragment3 == null || postListingFragment3.getSubreddit() == null)) {
                        subredditBlockedState = Boolean.valueOf(PrefsUtility.pref_blocked_subreddits_check(this, this.sharedPreferences, this.postListingFragment.getSubreddit().getCanonicalName()));
                        subredditBlockedState2 = Boolean.valueOf(PrefsUtility.pref_pinned_subreddits_check(this, this.sharedPreferences, this.postListingFragment.getSubreddit().getCanonicalName()));
                        PostListingFragment postListingFragment4 = this.postListingFragment;
                        String subredditDescription = (postListingFragment4 != null || postListingFragment4.getSubreddit() == null) ? null : this.postListingFragment.getSubreddit().description_html;
                        RedditSubredditSubscriptionManager redditSubredditSubscriptionManager = subredditSubscriptionManager;
                        RedditAccount redditAccount = user;
                        OptionsMenuUtility.prepare(this, menu, this.isMenuShown, postsVisible, commentsVisible, false, false, false, postsSortable, commentsSortable, isFrontPage, subredditSubscriptionState, !postsVisible && subredditDescription != null && subredditDescription.length() > 0, true, subredditBlockedState2, subredditBlockedState);
                        commentListingFragment2 = this.commentListingFragment;
                        if (commentListingFragment2 != null) {
                            commentListingFragment2.onCreateOptionsMenu(menu);
                        } else {
                            Menu menu2 = menu;
                        }
                        return true;
                    }
                }
                subredditBlockedState2 = null;
                subredditBlockedState = null;
                PostListingFragment postListingFragment42 = this.postListingFragment;
                String subredditDescription2 = (postListingFragment42 != null || postListingFragment42.getSubreddit() == null) ? null : this.postListingFragment.getSubreddit().description_html;
                RedditSubredditSubscriptionManager redditSubredditSubscriptionManager2 = subredditSubscriptionManager;
                RedditAccount redditAccount2 = user;
                OptionsMenuUtility.prepare(this, menu, this.isMenuShown, postsVisible, commentsVisible, false, false, false, postsSortable, commentsSortable, isFrontPage, subredditSubscriptionState, !postsVisible && subredditDescription2 != null && subredditDescription2.length() > 0, true, subredditBlockedState2, subredditBlockedState);
                commentListingFragment2 = this.commentListingFragment;
                if (commentListingFragment2 != null) {
                }
                return true;
            }
        }
        subredditSubscriptionState = null;
        PostListingFragment postListingFragment32 = this.postListingFragment;
        try {
            subredditBlockedState = Boolean.valueOf(PrefsUtility.pref_blocked_subreddits_check(this, this.sharedPreferences, this.postListingFragment.getSubreddit().getCanonicalName()));
            subredditBlockedState2 = Boolean.valueOf(PrefsUtility.pref_pinned_subreddits_check(this, this.sharedPreferences, this.postListingFragment.getSubreddit().getCanonicalName()));
        } catch (InvalidSubredditNameException e) {
            subredditBlockedState2 = null;
            subredditBlockedState = null;
        }
        PostListingFragment postListingFragment422 = this.postListingFragment;
        String subredditDescription22 = (postListingFragment422 != null || postListingFragment422.getSubreddit() == null) ? null : this.postListingFragment.getSubreddit().description_html;
        RedditSubredditSubscriptionManager redditSubredditSubscriptionManager22 = subredditSubscriptionManager;
        RedditAccount redditAccount22 = user;
        OptionsMenuUtility.prepare(this, menu, this.isMenuShown, postsVisible, commentsVisible, false, false, false, postsSortable, commentsSortable, isFrontPage, subredditSubscriptionState, !postsVisible && subredditDescription22 != null && subredditDescription22.length() > 0, true, subredditBlockedState2, subredditBlockedState);
        commentListingFragment2 = this.commentListingFragment;
        if (commentListingFragment2 != null) {
        }
        return true;
    }

    public void onRefreshComments() {
        this.commentListingController.setSession(null);
        requestRefresh(RefreshableFragment.COMMENTS, true);
    }

    public void onPastComments() {
        SessionListDialog.newInstance(this.commentListingController.getUri(), this.commentListingController.getSession(), SessionChangeType.COMMENTS).show(getSupportFragmentManager(), (String) null);
    }

    public void onSortSelected(Sort order) {
        this.commentListingController.setSort(order);
        requestRefresh(RefreshableFragment.COMMENTS, false);
    }

    public void onSortSelected(UserCommentListingURL.Sort order) {
        this.commentListingController.setSort(order);
        requestRefresh(RefreshableFragment.COMMENTS, false);
    }

    public void onSearchComments() {
        DialogUtils.showSearchDialog(this, R.string.action_search_comments, new OnSearchListener() {
            public void onSearch(@Nullable String query) {
                Intent searchIntent = new Intent(MainActivity.this, CommentListingActivity.class);
                searchIntent.setData(MainActivity.this.commentListingController.getUri());
                searchIntent.putExtra(CommentListingActivity.EXTRA_SEARCH_STRING, query);
                MainActivity.this.startActivity(searchIntent);
            }
        });
    }

    public void onRefreshPosts() {
        this.postListingController.setSession(null);
        requestRefresh(RefreshableFragment.POSTS, true);
    }

    public void onPastPosts() {
        SessionListDialog.newInstance(this.postListingController.getUri(), this.postListingController.getSession(), SessionChangeType.POSTS).show(getSupportFragmentManager(), (String) null);
    }

    public void onSubmitPost() {
        Intent intent = new Intent(this, PostSubmitActivity.class);
        if (this.postListingController.isSubreddit()) {
            intent.putExtra("subreddit", this.postListingController.subredditCanonicalName());
        }
        startActivity(intent);
    }

    public void onSortSelected(PostSort order) {
        this.postListingController.setSort(order);
        requestRefresh(RefreshableFragment.POSTS, false);
    }

    public void onSearchPosts() {
        PostListingActivity.onSearchPosts(this.postListingController, this);
    }

    public void onSubscribe() {
        PostListingFragment postListingFragment2 = this.postListingFragment;
        if (postListingFragment2 != null) {
            postListingFragment2.onSubscribe();
        }
    }

    public void onUnsubscribe() {
        PostListingFragment postListingFragment2 = this.postListingFragment;
        if (postListingFragment2 != null) {
            postListingFragment2.onUnsubscribe();
        }
    }

    public void onSidebar() {
        Intent intent = new Intent(this, HtmlViewActivity.class);
        intent.putExtra("html", this.postListingFragment.getSubreddit().getSidebarHtml(PrefsUtility.isNightMode(this)));
        intent.putExtra("title", String.format(Locale.US, "%s: %s", new Object[]{getString(R.string.sidebar_activity_title), this.postListingFragment.getSubreddit().url}));
        startActivityForResult(intent, 1);
    }

    public void onPin() {
        PostListingFragment postListingFragment2 = this.postListingFragment;
        if (postListingFragment2 != null) {
            try {
                PrefsUtility.pref_pinned_subreddits_add(this, this.sharedPreferences, postListingFragment2.getSubreddit().getCanonicalName());
                invalidateOptionsMenu();
            } catch (InvalidSubredditNameException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void onUnpin() {
        PostListingFragment postListingFragment2 = this.postListingFragment;
        if (postListingFragment2 != null) {
            try {
                PrefsUtility.pref_pinned_subreddits_remove(this, this.sharedPreferences, postListingFragment2.getSubreddit().getCanonicalName());
                invalidateOptionsMenu();
            } catch (InvalidSubredditNameException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void onBlock() {
        PostListingFragment postListingFragment2 = this.postListingFragment;
        if (postListingFragment2 != null) {
            try {
                PrefsUtility.pref_blocked_subreddits_add(this, this.sharedPreferences, postListingFragment2.getSubreddit().getCanonicalName());
                invalidateOptionsMenu();
            } catch (InvalidSubredditNameException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void onUnblock() {
        PostListingFragment postListingFragment2 = this.postListingFragment;
        if (postListingFragment2 != null) {
            try {
                PrefsUtility.pref_blocked_subreddits_remove(this, this.sharedPreferences, postListingFragment2.getSubreddit().getCanonicalName());
                invalidateOptionsMenu();
            } catch (InvalidSubredditNameException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void onRefreshSubreddits() {
        requestRefresh(RefreshableFragment.MAIN, true);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        CommentListingFragment commentListingFragment2 = this.commentListingFragment;
        if (commentListingFragment2 != null && commentListingFragment2.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() != 16908332) {
            return super.onOptionsItemSelected(item);
        }
        onBackPressed();
        return true;
    }

    public void onSessionSelected(UUID session, SessionChangeType type) {
        switch (type) {
            case POSTS:
                this.postListingController.setSession(session);
                requestRefresh(RefreshableFragment.POSTS, false);
                return;
            case COMMENTS:
                this.commentListingController.setSession(session);
                requestRefresh(RefreshableFragment.COMMENTS, false);
                return;
            default:
                return;
        }
    }

    public void onSessionRefreshSelected(SessionChangeType type) {
        switch (type) {
            case POSTS:
                onRefreshPosts();
                return;
            case COMMENTS:
                onRefreshComments();
                return;
            default:
                return;
        }
    }

    public void onSessionChanged(UUID session, SessionChangeType type, long timestamp) {
        switch (type) {
            case POSTS:
                PostListingController postListingController2 = this.postListingController;
                if (postListingController2 != null) {
                    postListingController2.setSession(session);
                    return;
                }
                return;
            case COMMENTS:
                CommentListingController commentListingController2 = this.commentListingController;
                if (commentListingController2 != null) {
                    commentListingController2.setSession(session);
                    return;
                }
                return;
            default:
                return;
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
                MainActivity.this.invalidateOptionsMenu();
            }
        });
    }

    private void showBackButton(boolean isVisible) {
        configBackButton(isVisible, new View.OnClickListener() {
            public void onClick(View v) {
                MainActivity.this.onBackPressed();
            }
        });
    }
}
