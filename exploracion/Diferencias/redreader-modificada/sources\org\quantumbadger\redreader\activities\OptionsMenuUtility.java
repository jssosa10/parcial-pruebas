package org.quantumbadger.redreader.activities;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.SubMenu;
import java.util.EnumSet;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.UnexpectedInternalStateException;
import org.quantumbadger.redreader.fragments.AccountListDialog;
import org.quantumbadger.redreader.reddit.PostSort;
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.SubredditSubscriptionState;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL.Sort;
import org.quantumbadger.redreader.reddit.url.UserCommentListingURL;
import org.quantumbadger.redreader.settings.SettingsActivity;

public final class OptionsMenuUtility {

    /* renamed from: org.quantumbadger.redreader.activities.OptionsMenuUtility$23 reason: invalid class name */
    static /* synthetic */ class AnonymousClass23 {
        static final /* synthetic */ int[] $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option = new int[Option.values().length];

        static {
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.ACCOUNTS.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.SETTINGS.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.CLOSE_ALL.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.THEMES.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.REFRESH_SUBREDDITS.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.REFRESH_POSTS.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.SUBMIT_POST.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.SEARCH.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.SEARCH_COMMENTS.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.REFRESH_COMMENTS.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.PAST_POSTS.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.PAST_COMMENTS.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.SUBSCRIBE.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.UNSUBSCRIBE.ordinal()] = 14;
            } catch (NoSuchFieldError e14) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.UNSUBSCRIBING.ordinal()] = 15;
            } catch (NoSuchFieldError e15) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.SUBSCRIBING.ordinal()] = 16;
            } catch (NoSuchFieldError e16) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.SIDEBAR.ordinal()] = 17;
            } catch (NoSuchFieldError e17) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.PIN.ordinal()] = 18;
            } catch (NoSuchFieldError e18) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.UNPIN.ordinal()] = 19;
            } catch (NoSuchFieldError e19) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.BLOCK.ordinal()] = 20;
            } catch (NoSuchFieldError e20) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[Option.UNBLOCK.ordinal()] = 21;
            } catch (NoSuchFieldError e21) {
            }
            $SwitchMap$org$quantumbadger$redreader$reddit$api$RedditSubredditSubscriptionManager$SubredditSubscriptionState = new int[SubredditSubscriptionState.values().length];
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$api$RedditSubredditSubscriptionManager$SubredditSubscriptionState[SubredditSubscriptionState.NOT_SUBSCRIBED.ordinal()] = 1;
            } catch (NoSuchFieldError e22) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$api$RedditSubredditSubscriptionManager$SubredditSubscriptionState[SubredditSubscriptionState.SUBSCRIBED.ordinal()] = 2;
            } catch (NoSuchFieldError e23) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$api$RedditSubredditSubscriptionManager$SubredditSubscriptionState[SubredditSubscriptionState.SUBSCRIBING.ordinal()] = 3;
            } catch (NoSuchFieldError e24) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$api$RedditSubredditSubscriptionManager$SubredditSubscriptionState[SubredditSubscriptionState.UNSUBSCRIBING.ordinal()] = 4;
            } catch (NoSuchFieldError e25) {
            }
        }
    }

    private enum Option {
        ACCOUNTS,
        SETTINGS,
        CLOSE_ALL,
        SUBMIT_POST,
        SEARCH,
        SEARCH_COMMENTS,
        REFRESH_SUBREDDITS,
        REFRESH_POSTS,
        REFRESH_COMMENTS,
        PAST_POSTS,
        THEMES,
        PAST_COMMENTS,
        SUBSCRIBE,
        SUBSCRIBING,
        UNSUBSCRIBING,
        UNSUBSCRIBE,
        SIDEBAR,
        PIN,
        UNPIN,
        BLOCK,
        UNBLOCK
    }

    public interface OptionsMenuCommentsListener extends OptionsMenuListener {
        void onPastComments();

        void onRefreshComments();

        void onSearchComments();

        void onSortSelected(Sort sort);

        void onSortSelected(UserCommentListingURL.Sort sort);
    }

    public enum OptionsMenuItemsPref {
        ACCOUNTS,
        THEME,
        CLOSE_ALL,
        PAST,
        SUBMIT_POST,
        SEARCH,
        REPLY,
        PIN,
        BLOCK
    }

    private interface OptionsMenuListener {
    }

    public interface OptionsMenuPostsListener extends OptionsMenuListener {
        void onBlock();

        void onPastPosts();

        void onPin();

        void onRefreshPosts();

        void onSearchPosts();

        void onSidebar();

        void onSortSelected(PostSort postSort);

        void onSubmitPost();

        void onSubscribe();

        void onUnblock();

        void onUnpin();

        void onUnsubscribe();
    }

    public interface OptionsMenuSubredditsListener extends OptionsMenuListener {
        void onRefreshSubreddits();
    }

    public static <E extends BaseActivity & OptionsMenuListener> void prepare(E activity, Menu menu, boolean subredditsVisible, boolean postsVisible, boolean commentsVisible, boolean areSearchResults, boolean isUserPostListing, boolean isUserCommentListing, boolean postsSortable, boolean commentsSortable, boolean isFrontPage, SubredditSubscriptionState subredditSubscriptionState, boolean subredditHasSidebar, boolean pastCommentsSupported, Boolean subredditPinned, Boolean subredditBlocked) {
        E e = activity;
        Menu menu2 = menu;
        boolean z = isFrontPage;
        SubredditSubscriptionState subredditSubscriptionState2 = subredditSubscriptionState;
        EnumSet<OptionsMenuItemsPref> optionsMenuItemsPrefs = PrefsUtility.pref_menus_optionsmenu_items(activity, PreferenceManager.getDefaultSharedPreferences(activity));
        if (subredditsVisible && !postsVisible && !commentsVisible) {
            add(activity, menu, Option.REFRESH_SUBREDDITS, false);
        } else if (!subredditsVisible && postsVisible && !commentsVisible) {
            if (postsSortable) {
                if (areSearchResults) {
                    addAllSearchSorts(activity, menu, true);
                } else {
                    addAllPostSorts(activity, menu, true, !isUserPostListing, z);
                }
            }
            add(activity, menu, Option.REFRESH_POSTS, false);
            if (optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.PAST)) {
                add(activity, menu, Option.PAST_POSTS, false);
            }
            if (optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.SUBMIT_POST)) {
                add(activity, menu, Option.SUBMIT_POST, false);
            }
            if (optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.SEARCH)) {
                add(activity, menu, Option.SEARCH, false);
            }
            if (subredditPinned != null && optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.PIN)) {
                if (subredditPinned.booleanValue()) {
                    add(activity, menu, Option.UNPIN, false);
                } else {
                    add(activity, menu, Option.PIN, false);
                }
            }
            if (subredditBlocked != null && optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.BLOCK)) {
                if (subredditBlocked.booleanValue()) {
                    add(activity, menu, Option.UNBLOCK, false);
                } else {
                    add(activity, menu, Option.BLOCK, false);
                }
            }
            if (subredditSubscriptionState2 != null) {
                addSubscriptionItem(activity, menu, subredditSubscriptionState2);
            }
            if (subredditHasSidebar) {
                add(activity, menu, Option.SIDEBAR, false);
            }
        } else if (subredditsVisible || postsVisible || !commentsVisible) {
            if (postsVisible && commentsVisible) {
                SubMenu sortMenu = menu.addSubMenu(R.string.options_sort);
                sortMenu.getItem().setIcon(R.drawable.ic_sort_dark);
                sortMenu.getItem().setShowAsAction(2);
                if (postsSortable) {
                    if (areSearchResults) {
                        addAllSearchSorts(activity, sortMenu, false);
                    } else {
                        addAllPostSorts(activity, sortMenu, false, !isUserPostListing, z);
                    }
                }
                if (commentsSortable) {
                    addAllCommentSorts(activity, sortMenu, false);
                }
                if (optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.PAST)) {
                    SubMenu pastMenu = menu.addSubMenu(R.string.options_past);
                    add(activity, pastMenu, Option.PAST_POSTS, true);
                    if (pastCommentsSupported) {
                        add(activity, pastMenu, Option.PAST_COMMENTS, true);
                    }
                }
                if (optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.SEARCH)) {
                    add(activity, menu, Option.SEARCH_COMMENTS, false);
                }
            } else if (postsVisible) {
                if (postsSortable) {
                    if (areSearchResults) {
                        addAllSearchSorts(activity, menu, true);
                    } else {
                        addAllPostSorts(activity, menu, true, !isUserPostListing, z);
                    }
                }
                add(activity, menu, Option.PAST_POSTS, false);
            }
            SubMenu refreshMenu = menu.addSubMenu(R.string.options_refresh);
            refreshMenu.getItem().setIcon(R.drawable.ic_refresh_dark);
            refreshMenu.getItem().setShowAsAction(2);
            if (subredditsVisible) {
                add(activity, refreshMenu, Option.REFRESH_SUBREDDITS, true);
            }
            if (postsVisible) {
                add(activity, refreshMenu, Option.REFRESH_POSTS, true);
                if (optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.SUBMIT_POST)) {
                    add(activity, menu, Option.SUBMIT_POST, false);
                }
                if (optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.SEARCH)) {
                    add(activity, menu, Option.SEARCH, false);
                }
                if (subredditPinned != null && optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.PIN)) {
                    if (subredditPinned.booleanValue()) {
                        add(activity, menu, Option.UNPIN, false);
                    } else {
                        add(activity, menu, Option.PIN, false);
                    }
                }
                if (subredditSubscriptionState2 != null) {
                    addSubscriptionItem(activity, menu, subredditSubscriptionState2);
                }
                if (subredditHasSidebar) {
                    add(activity, menu, Option.SIDEBAR, false);
                }
            }
            if (commentsVisible) {
                add(activity, refreshMenu, Option.REFRESH_COMMENTS, true);
            }
        } else {
            if (commentsSortable && !isUserCommentListing) {
                addAllCommentSorts(activity, menu, true);
            } else if (commentsSortable && isUserCommentListing) {
                addAllUserCommentSorts(activity, menu, true);
            }
            add(activity, menu, Option.REFRESH_COMMENTS, false);
            if (optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.SEARCH)) {
                add(activity, menu, Option.SEARCH, false);
            }
            if (pastCommentsSupported && optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.PAST)) {
                add(activity, menu, Option.PAST_COMMENTS, false);
            }
        }
        if (optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.ACCOUNTS)) {
            add(activity, menu, Option.ACCOUNTS, false);
        }
        if (optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.THEME)) {
            add(activity, menu, Option.THEMES, false);
        }
        add(activity, menu, Option.SETTINGS, false);
        if (optionsMenuItemsPrefs.contains(OptionsMenuItemsPref.CLOSE_ALL)) {
            add(activity, menu, Option.CLOSE_ALL, false);
        }
    }

    private static void addSubscriptionItem(BaseActivity activity, Menu menu, SubredditSubscriptionState subredditSubscriptionState) {
        if (subredditSubscriptionState != null) {
            switch (subredditSubscriptionState) {
                case NOT_SUBSCRIBED:
                    add(activity, menu, Option.SUBSCRIBE, false);
                    return;
                case SUBSCRIBED:
                    add(activity, menu, Option.UNSUBSCRIBE, false);
                    return;
                case SUBSCRIBING:
                    add(activity, menu, Option.SUBSCRIBING, false);
                    return;
                case UNSUBSCRIBING:
                    add(activity, menu, Option.UNSUBSCRIBING, false);
                    return;
                default:
                    throw new UnexpectedInternalStateException("Unknown subscription state");
            }
        }
    }

    private static void add(final BaseActivity activity, Menu menu, Option option, boolean longText) {
        int i = AnonymousClass23.$SwitchMap$org$quantumbadger$redreader$activities$OptionsMenuUtility$Option[option.ordinal()];
        int i2 = R.string.options_past;
        switch (i) {
            case 1:
                menu.add(activity.getString(R.string.options_accounts)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        return true;
                    }
                });
                return;
            case 2:
                menu.add(activity.getString(R.string.options_settings)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        activity.startActivityForResult(new Intent(activity, SettingsActivity.class), 1);
                        return true;
                    }
                });
                return;
            case 3:
                if (!(activity instanceof MainActivity)) {
                    menu.add(activity.getString(R.string.options_close_all)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            activity.closeAllExceptMain();
                            return true;
                        }
                    });
                    return;
                }
                return;
            case 4:
                menu.add(activity.getString(R.string.options_theme)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        new AccountListDialog().show(activity.getSupportFragmentManager(), (String) null);
                        return true;
                    }
                });
                return;
            case 5:
                MenuItem refreshSubreddits = menu.add(activity.getString(R.string.options_refresh_subreddits)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ((OptionsMenuSubredditsListener) activity).onRefreshSubreddits();
                        return true;
                    }
                });
                refreshSubreddits.setShowAsAction(2);
                if (!longText) {
                    refreshSubreddits.setIcon(R.drawable.ic_refresh_dark);
                    return;
                }
                return;
            case 6:
                MenuItem refreshPosts = menu.add(activity.getString(R.string.options_refresh_posts)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ((OptionsMenuPostsListener) activity).onRefreshPosts();
                        return true;
                    }
                });
                refreshPosts.setShowAsAction(2);
                if (!longText) {
                    refreshPosts.setIcon(R.drawable.ic_refresh_dark);
                    return;
                }
                return;
            case 7:
                menu.add(activity.getString(R.string.options_submit_post)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ((OptionsMenuPostsListener) activity).onSubmitPost();
                        return true;
                    }
                });
                return;
            case 8:
                menu.add(0, 0, 1, activity.getString(R.string.action_search)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        BaseActivity baseActivity = activity;
                        if (baseActivity instanceof OptionsMenuPostsListener) {
                            ((OptionsMenuPostsListener) baseActivity).onSearchPosts();
                            return true;
                        } else if (!(baseActivity instanceof OptionsMenuCommentsListener)) {
                            return false;
                        } else {
                            ((OptionsMenuCommentsListener) baseActivity).onSearchComments();
                            return true;
                        }
                    }
                });
                return;
            case 9:
                menu.add(0, 0, 1, activity.getString(R.string.action_search_comments)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        BaseActivity baseActivity = activity;
                        if (!(baseActivity instanceof OptionsMenuCommentsListener)) {
                            return false;
                        }
                        ((OptionsMenuCommentsListener) baseActivity).onSearchComments();
                        return true;
                    }
                });
                return;
            case 10:
                MenuItem refreshComments = menu.add(activity.getString(R.string.options_refresh_comments)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ((OptionsMenuCommentsListener) activity).onRefreshComments();
                        return true;
                    }
                });
                refreshComments.setShowAsAction(2);
                if (!longText) {
                    refreshComments.setIcon(R.drawable.ic_refresh_dark);
                    return;
                }
                return;
            case 11:
                if (longText) {
                    i2 = R.string.options_past_posts;
                }
                menu.add(activity.getString(i2)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ((OptionsMenuPostsListener) activity).onPastPosts();
                        return true;
                    }
                });
                return;
            case 12:
                if (longText) {
                    i2 = R.string.options_past_comments;
                }
                menu.add(activity.getString(i2)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ((OptionsMenuCommentsListener) activity).onPastComments();
                        return true;
                    }
                });
                return;
            case 13:
                menu.add(activity.getString(R.string.options_subscribe)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ((OptionsMenuPostsListener) activity).onSubscribe();
                        return true;
                    }
                });
                return;
            case 14:
                menu.add(activity.getString(R.string.options_unsubscribe)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ((OptionsMenuPostsListener) activity).onUnsubscribe();
                        return true;
                    }
                });
                return;
            case 15:
                menu.add(activity.getString(R.string.options_unsubscribing)).setEnabled(false);
                return;
            case 16:
                menu.add(activity.getString(R.string.options_subscribing)).setEnabled(false);
                return;
            case 17:
                menu.add(activity.getString(R.string.options_sidebar)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ((OptionsMenuPostsListener) activity).onSidebar();
                        return true;
                    }
                });
                return;
            case 18:
                menu.add(activity.getString(R.string.pin_subreddit)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ((OptionsMenuPostsListener) activity).onPin();
                        return true;
                    }
                });
                return;
            case 19:
                menu.add(activity.getString(R.string.unpin_subreddit)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ((OptionsMenuPostsListener) activity).onUnpin();
                        return true;
                    }
                });
                return;
            case 20:
                menu.add(activity.getString(R.string.block_subreddit)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ((OptionsMenuPostsListener) activity).onBlock();
                        return true;
                    }
                });
                return;
            case 21:
                menu.add(activity.getString(R.string.unblock_subreddit)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        ((OptionsMenuPostsListener) activity).onUnblock();
                        return true;
                    }
                });
                return;
            default:
                BugReportActivity.handleGlobalError((Context) activity, "Unknown menu option added");
                return;
        }
    }

    private static void addAllPostSorts(AppCompatActivity activity, Menu menu, boolean icon, boolean includeRising, boolean includeBest) {
        SubMenu sortPosts = menu.addSubMenu(R.string.options_sort_posts);
        if (icon) {
            sortPosts.getItem().setIcon(R.drawable.ic_sort_dark);
            sortPosts.getItem().setShowAsAction(2);
        }
        addSort(activity, (Menu) sortPosts, (int) R.string.sort_posts_hot, PostSort.HOT);
        addSort(activity, (Menu) sortPosts, (int) R.string.sort_posts_new, PostSort.NEW);
        if (includeRising) {
            addSort(activity, (Menu) sortPosts, (int) R.string.sort_posts_rising, PostSort.RISING);
        }
        addSort(activity, (Menu) sortPosts, (int) R.string.sort_posts_controversial, PostSort.CONTROVERSIAL);
        if (includeBest) {
            addSort(activity, (Menu) sortPosts, (int) R.string.sort_posts_best, PostSort.BEST);
        }
        SubMenu sortPostsTop = sortPosts.addSubMenu(R.string.sort_posts_top);
        addSort(activity, (Menu) sortPostsTop, (int) R.string.sort_posts_top_hour, PostSort.TOP_HOUR);
        addSort(activity, (Menu) sortPostsTop, (int) R.string.sort_posts_top_today, PostSort.TOP_DAY);
        addSort(activity, (Menu) sortPostsTop, (int) R.string.sort_posts_top_week, PostSort.TOP_WEEK);
        addSort(activity, (Menu) sortPostsTop, (int) R.string.sort_posts_top_month, PostSort.TOP_MONTH);
        addSort(activity, (Menu) sortPostsTop, (int) R.string.sort_posts_top_year, PostSort.TOP_YEAR);
        addSort(activity, (Menu) sortPostsTop, (int) R.string.sort_posts_top_all, PostSort.TOP_ALL);
    }

    private static void addAllSearchSorts(AppCompatActivity activity, Menu menu, boolean icon) {
        SubMenu sortPosts = menu.addSubMenu(R.string.options_sort_posts);
        if (icon) {
            sortPosts.getItem().setIcon(R.drawable.ic_sort_dark);
            sortPosts.getItem().setShowAsAction(2);
        }
        addSort(activity, (Menu) sortPosts, (int) R.string.sort_posts_relevance, PostSort.RELEVANCE);
        addSort(activity, (Menu) sortPosts, (int) R.string.sort_posts_new, PostSort.NEW);
        addSort(activity, (Menu) sortPosts, (int) R.string.sort_posts_hot, PostSort.HOT);
        addSort(activity, (Menu) sortPosts, (int) R.string.sort_posts_top, PostSort.TOP);
        addSort(activity, (Menu) sortPosts, (int) R.string.sort_posts_comments, PostSort.COMMENTS);
    }

    private static void addSort(final AppCompatActivity activity, Menu menu, int name, final PostSort order) {
        menu.add(activity.getString(name)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                ((OptionsMenuPostsListener) activity).onSortSelected(order);
                return true;
            }
        });
    }

    private static void addAllCommentSorts(AppCompatActivity activity, Menu menu, boolean icon) {
        SubMenu sortComments = menu.addSubMenu(R.string.options_sort_comments);
        if (icon) {
            sortComments.getItem().setIcon(R.drawable.ic_sort_dark);
            sortComments.getItem().setShowAsAction(2);
        }
        addSort(activity, (Menu) sortComments, (int) R.string.sort_comments_best, Sort.BEST);
        addSort(activity, (Menu) sortComments, (int) R.string.sort_comments_hot, Sort.HOT);
        addSort(activity, (Menu) sortComments, (int) R.string.sort_comments_new, Sort.NEW);
        addSort(activity, (Menu) sortComments, (int) R.string.sort_comments_old, Sort.OLD);
        addSort(activity, (Menu) sortComments, (int) R.string.sort_comments_controversial, Sort.CONTROVERSIAL);
        addSort(activity, (Menu) sortComments, (int) R.string.sort_comments_top, Sort.TOP);
        addSort(activity, (Menu) sortComments, (int) R.string.sort_comments_qa, Sort.QA);
    }

    private static void addSort(final AppCompatActivity activity, Menu menu, int name, final Sort order) {
        menu.add(activity.getString(name)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                ((OptionsMenuCommentsListener) activity).onSortSelected(order);
                return true;
            }
        });
    }

    private static void addAllUserCommentSorts(AppCompatActivity activity, Menu menu, boolean icon) {
        SubMenu sortComments = menu.addSubMenu(R.string.options_sort_comments);
        if (icon) {
            sortComments.getItem().setIcon(R.drawable.ic_sort_dark);
            sortComments.getItem().setShowAsAction(2);
        }
        addSort(activity, (Menu) sortComments, (int) R.string.sort_comments_hot, UserCommentListingURL.Sort.HOT);
        addSort(activity, (Menu) sortComments, (int) R.string.sort_comments_new, UserCommentListingURL.Sort.NEW);
        addSort(activity, (Menu) sortComments, (int) R.string.sort_comments_controversial, UserCommentListingURL.Sort.CONTROVERSIAL);
        SubMenu sortCommentsTop = sortComments.addSubMenu(R.string.sort_comments_top);
        addSort(activity, (Menu) sortCommentsTop, (int) R.string.sort_posts_top_hour, UserCommentListingURL.Sort.TOP_HOUR);
        addSort(activity, (Menu) sortCommentsTop, (int) R.string.sort_posts_top_today, UserCommentListingURL.Sort.TOP_DAY);
        addSort(activity, (Menu) sortCommentsTop, (int) R.string.sort_posts_top_week, UserCommentListingURL.Sort.TOP_WEEK);
        addSort(activity, (Menu) sortCommentsTop, (int) R.string.sort_posts_top_month, UserCommentListingURL.Sort.TOP_MONTH);
        addSort(activity, (Menu) sortCommentsTop, (int) R.string.sort_posts_top_year, UserCommentListingURL.Sort.TOP_YEAR);
        addSort(activity, (Menu) sortCommentsTop, (int) R.string.sort_posts_top_all, UserCommentListingURL.Sort.TOP_ALL);
    }

    private static void addSort(final AppCompatActivity activity, Menu menu, int name, final UserCommentListingURL.Sort order) {
        menu.add(activity.getString(name)).setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem menuItem) {
                ((OptionsMenuCommentsListener) activity).onSortSelected(order);
                return true;
            }
        });
    }
}
