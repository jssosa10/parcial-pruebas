package org.quantumbadger.redreader.adapters;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.ClipboardManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter.Item;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.PrefsUtility.BlockedSubredditSort;
import org.quantumbadger.redreader.common.PrefsUtility.PinnedSubredditSort;
import org.quantumbadger.redreader.fragments.MainMenuFragment.MainMenuShortcutItems;
import org.quantumbadger.redreader.fragments.MainMenuFragment.MainMenuUserItems;
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager;
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.SubredditSubscriptionState;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit.InvalidSubredditNameException;
import org.quantumbadger.redreader.reddit.url.MultiredditPostListURL;
import org.quantumbadger.redreader.reddit.url.PostListingURL;
import org.quantumbadger.redreader.reddit.url.SubredditPostListURL;
import org.quantumbadger.redreader.views.LoadingSpinnerView;
import org.quantumbadger.redreader.views.list.GroupedRecyclerViewItemListItemView;
import org.quantumbadger.redreader.views.list.GroupedRecyclerViewItemListSectionHeaderView;
import org.quantumbadger.redreader.views.liststatus.ErrorView;

public class MainMenuListingManager {
    private static final int GROUP_BLOCKED_SUBREDDITS_HEADER = 6;
    private static final int GROUP_BLOCKED_SUBREDDITS_ITEMS = 7;
    private static final int GROUP_MAIN_HEADER = 0;
    private static final int GROUP_MAIN_ITEMS = 1;
    private static final int GROUP_MULTIREDDITS_HEADER = 8;
    private static final int GROUP_MULTIREDDITS_ITEMS = 9;
    private static final int GROUP_PINNED_SUBREDDITS_HEADER = 4;
    private static final int GROUP_PINNED_SUBREDDITS_ITEMS = 5;
    private static final int GROUP_SUBREDDITS_HEADER = 10;
    private static final int GROUP_SUBREDDITS_ITEMS = 11;
    private static final int GROUP_USER_HEADER = 2;
    private static final int GROUP_USER_ITEMS = 3;
    /* access modifiers changed from: private */
    @NonNull
    public final AppCompatActivity mActivity;
    /* access modifiers changed from: private */
    @NonNull
    public final GroupedRecyclerViewAdapter mAdapter = new GroupedRecyclerViewAdapter(12);
    /* access modifiers changed from: private */
    @NonNull
    public final Context mContext;
    /* access modifiers changed from: private */
    @NonNull
    public final MainMenuSelectionListener mListener;
    @Nullable
    private Item mMultiredditHeaderItem;
    /* access modifiers changed from: private */
    @Nullable
    public ArrayList<String> mMultiredditSubscriptions;
    /* access modifiers changed from: private */
    @Nullable
    public ArrayList<String> mSubredditSubscriptions;

    /* renamed from: org.quantumbadger.redreader.adapters.MainMenuListingManager$9 reason: invalid class name */
    static /* synthetic */ class AnonymousClass9 {
        static final /* synthetic */ int[] $SwitchMap$org$quantumbadger$redreader$common$PrefsUtility$BlockedSubredditSort = new int[BlockedSubredditSort.values().length];
        static final /* synthetic */ int[] $SwitchMap$org$quantumbadger$redreader$common$PrefsUtility$PinnedSubredditSort = new int[PinnedSubredditSort.values().length];

        static {
            $SwitchMap$org$quantumbadger$redreader$adapters$MainMenuListingManager$SubredditAction = new int[SubredditAction.values().length];
            try {
                $SwitchMap$org$quantumbadger$redreader$adapters$MainMenuListingManager$SubredditAction[SubredditAction.SHARE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$adapters$MainMenuListingManager$SubredditAction[SubredditAction.COPY_URL.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$adapters$MainMenuListingManager$SubredditAction[SubredditAction.EXTERNAL.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$adapters$MainMenuListingManager$SubredditAction[SubredditAction.PIN.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$adapters$MainMenuListingManager$SubredditAction[SubredditAction.UNPIN.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$adapters$MainMenuListingManager$SubredditAction[SubredditAction.BLOCK.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$adapters$MainMenuListingManager$SubredditAction[SubredditAction.UNBLOCK.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$adapters$MainMenuListingManager$SubredditAction[SubredditAction.SUBSCRIBE.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$adapters$MainMenuListingManager$SubredditAction[SubredditAction.UNSUBSCRIBE.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$common$PrefsUtility$BlockedSubredditSort[BlockedSubredditSort.NAME.ordinal()] = 1;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$common$PrefsUtility$BlockedSubredditSort[BlockedSubredditSort.DATE.ordinal()] = 2;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$common$PrefsUtility$PinnedSubredditSort[PinnedSubredditSort.NAME.ordinal()] = 1;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$common$PrefsUtility$PinnedSubredditSort[PinnedSubredditSort.DATE.ordinal()] = 2;
            } catch (NoSuchFieldError e13) {
            }
        }
    }

    public enum SubredditAction {
        SHARE(R.string.action_share),
        COPY_URL(R.string.action_copy_link),
        BLOCK(R.string.block_subreddit),
        UNBLOCK(R.string.unblock_subreddit),
        PIN(R.string.pin_subreddit),
        UNPIN(R.string.unpin_subreddit),
        SUBSCRIBE(R.string.options_subscribe),
        UNSUBSCRIBE(R.string.options_unsubscribe),
        EXTERNAL(R.string.action_external);
        
        public final int descriptionResId;

        private SubredditAction(int descriptionResId2) {
            this.descriptionResId = descriptionResId2;
        }
    }

    private static class SubredditMenuItem {
        public final SubredditAction action;
        public final String title;

        private SubredditMenuItem(Context context, int titleRes, SubredditAction action2) {
            this.title = context.getString(titleRes);
            this.action = action2;
        }
    }

    @NonNull
    public GroupedRecyclerViewAdapter getAdapter() {
        return this.mAdapter;
    }

    public MainMenuListingManager(@NonNull AppCompatActivity activity, @NonNull MainMenuSelectionListener listener, @NonNull RedditAccount user) {
        AppCompatActivity appCompatActivity = activity;
        General.checkThisIsUIThread();
        this.mActivity = appCompatActivity;
        this.mContext = activity.getApplicationContext();
        this.mListener = listener;
        TypedArray attr = appCompatActivity.obtainStyledAttributes(new int[]{R.attr.rrIconPerson, R.attr.rrIconEnvOpen, R.attr.rrIconSend, R.attr.rrIconStarFilled, R.attr.rrIconCross, R.attr.rrIconUpvote, R.attr.rrIconDownvote});
        Drawable rrIconPerson = ContextCompat.getDrawable(appCompatActivity, attr.getResourceId(0, 0));
        Drawable rrIconEnvOpen = ContextCompat.getDrawable(appCompatActivity, attr.getResourceId(1, 0));
        Drawable rrIconSend = ContextCompat.getDrawable(appCompatActivity, attr.getResourceId(2, 0));
        Drawable rrIconStarFilled = ContextCompat.getDrawable(appCompatActivity, attr.getResourceId(3, 0));
        Drawable rrIconCross = ContextCompat.getDrawable(appCompatActivity, attr.getResourceId(4, 0));
        Drawable rrIconUpvote = ContextCompat.getDrawable(appCompatActivity, attr.getResourceId(5, 0));
        Drawable rrIconDownvote = ContextCompat.getDrawable(appCompatActivity, attr.getResourceId(6, 0));
        attr.recycle();
        EnumSet<MainMenuShortcutItems> mainMenuShortcutItems = PrefsUtility.pref_menus_mainmenu_shortcutitems(appCompatActivity, PreferenceManager.getDefaultSharedPreferences(activity));
        if (mainMenuShortcutItems.contains(MainMenuShortcutItems.FRONTPAGE)) {
            this.mAdapter.appendToGroup(1, (Item) makeItem((int) R.string.mainmenu_frontpage, 0, (Drawable) null, true));
        }
        if (mainMenuShortcutItems.contains(MainMenuShortcutItems.POPULAR)) {
            this.mAdapter.appendToGroup(1, (Item) makeItem((int) R.string.mainmenu_popular, 11, (Drawable) null, false));
        }
        if (mainMenuShortcutItems.contains(MainMenuShortcutItems.ALL)) {
            this.mAdapter.appendToGroup(1, (Item) makeItem((int) R.string.mainmenu_all, 10, (Drawable) null, false));
        }
        if (mainMenuShortcutItems.contains(MainMenuShortcutItems.CUSTOM)) {
            this.mAdapter.appendToGroup(1, (Item) makeItem((int) R.string.mainmenu_custom_destination, 9, (Drawable) null, false));
        }
        if (mainMenuShortcutItems.contains(MainMenuShortcutItems.RANDOM)) {
            this.mAdapter.appendToGroup(1, (Item) makeItem((int) R.string.mainmenu_random, 12, (Drawable) null, false));
        }
        if (mainMenuShortcutItems.contains(MainMenuShortcutItems.RANDOM_NSFW)) {
            this.mAdapter.appendToGroup(1, (Item) makeItem((int) R.string.mainmenu_random_nsfw, 13, (Drawable) null, false));
        }
        if (!user.isAnonymous()) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
            EnumSet<MainMenuUserItems> mainMenuUserItems = PrefsUtility.pref_menus_mainmenu_useritems(appCompatActivity, sharedPreferences);
            if (!mainMenuUserItems.isEmpty()) {
                if (PrefsUtility.pref_appearance_hide_username_main_menu(appCompatActivity, PreferenceManager.getDefaultSharedPreferences(activity))) {
                    this.mAdapter.appendToGroup(2, (Item) new GroupedRecyclerViewItemListSectionHeaderView(appCompatActivity.getString(R.string.mainmenu_useritems)));
                    RedditAccount redditAccount = user;
                } else {
                    this.mAdapter.appendToGroup(2, (Item) new GroupedRecyclerViewItemListSectionHeaderView(user.username));
                }
                AtomicBoolean isFirst = new AtomicBoolean(true);
                if (mainMenuUserItems.contains(MainMenuUserItems.PROFILE)) {
                    SharedPreferences sharedPreferences2 = sharedPreferences;
                    this.mAdapter.appendToGroup(3, (Item) makeItem((int) R.string.mainmenu_profile, 1, rrIconPerson, isFirst.getAndSet(false)));
                }
                if (mainMenuUserItems.contains(MainMenuUserItems.INBOX)) {
                    this.mAdapter.appendToGroup(3, (Item) makeItem((int) R.string.mainmenu_inbox, 2, rrIconEnvOpen, isFirst.getAndSet(false)));
                }
                if (mainMenuUserItems.contains(MainMenuUserItems.SUBMITTED)) {
                    this.mAdapter.appendToGroup(3, (Item) makeItem((int) R.string.mainmenu_submitted, 3, rrIconSend, isFirst.getAndSet(false)));
                }
                if (mainMenuUserItems.contains(MainMenuUserItems.SAVED)) {
                    this.mAdapter.appendToGroup(3, (Item) makeItem((int) R.string.mainmenu_saved, 6, rrIconStarFilled, isFirst.getAndSet(false)));
                }
                if (mainMenuUserItems.contains(MainMenuUserItems.HIDDEN)) {
                    Drawable drawable = rrIconPerson;
                    this.mAdapter.appendToGroup(3, (Item) makeItem((int) R.string.mainmenu_hidden, 8, rrIconCross, isFirst.getAndSet(false)));
                }
                if (mainMenuUserItems.contains(MainMenuUserItems.UPVOTED)) {
                    this.mAdapter.appendToGroup(3, (Item) makeItem((int) R.string.mainmenu_upvoted, 4, rrIconUpvote, isFirst.getAndSet(false)));
                }
                if (mainMenuUserItems.contains(MainMenuUserItems.DOWNVOTED)) {
                    this.mAdapter.appendToGroup(3, (Item) makeItem((int) R.string.mainmenu_downvoted, 5, rrIconDownvote, isFirst.getAndSet(false)));
                }
                if (mainMenuUserItems.contains(MainMenuUserItems.MODMAIL)) {
                    this.mAdapter.appendToGroup(3, (Item) makeItem((int) R.string.mainmenu_modmail, 7, rrIconEnvOpen, isFirst.getAndSet(false)));
                }
            } else {
                RedditAccount redditAccount2 = user;
                SharedPreferences sharedPreferences3 = sharedPreferences;
                Drawable drawable2 = rrIconPerson;
            }
        } else {
            RedditAccount redditAccount3 = user;
            Drawable drawable3 = rrIconPerson;
        }
        setPinnedSubreddits();
        if (PrefsUtility.pref_appearance_show_blocked_subreddits_main_menu(appCompatActivity, PreferenceManager.getDefaultSharedPreferences(activity))) {
            setBlockedSubreddits();
        }
        if (!user.isAnonymous() && PrefsUtility.pref_show_multireddit_main_menu(appCompatActivity, PreferenceManager.getDefaultSharedPreferences(activity))) {
            showMultiredditsHeader(activity);
            LoadingSpinnerView multiredditsLoadingSpinnerView = new LoadingSpinnerView(appCompatActivity);
            int paddingPx = General.dpToPixels(appCompatActivity, 30.0f);
            multiredditsLoadingSpinnerView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
            this.mAdapter.appendToGroup(9, (Item) new GroupedRecyclerViewItemFrameLayout(multiredditsLoadingSpinnerView));
        }
        PrefsUtility.pref_show_subscribed_subreddits_main_menu(appCompatActivity, PreferenceManager.getDefaultSharedPreferences(activity));
    }

    private void setPinnedSubreddits() {
        AppCompatActivity appCompatActivity = this.mActivity;
        List<String> pinnedSubreddits = PrefsUtility.pref_pinned_subreddits(appCompatActivity, PreferenceManager.getDefaultSharedPreferences(appCompatActivity));
        AppCompatActivity appCompatActivity2 = this.mActivity;
        PinnedSubredditSort pinnedSubredditsSort = PrefsUtility.pref_behaviour_pinned_subredditsort(appCompatActivity2, PreferenceManager.getDefaultSharedPreferences(appCompatActivity2));
        if (pinnedSubreddits != null) {
            this.mAdapter.removeAllFromGroup(5);
            this.mAdapter.removeAllFromGroup(4);
        }
        if (!pinnedSubreddits.isEmpty()) {
            this.mAdapter.appendToGroup(4, (Item) new GroupedRecyclerViewItemListSectionHeaderView(this.mActivity.getString(R.string.mainmenu_header_subreddits_pinned)));
            boolean isFirst = true;
            if (AnonymousClass9.$SwitchMap$org$quantumbadger$redreader$common$PrefsUtility$PinnedSubredditSort[pinnedSubredditsSort.ordinal()] == 1) {
                Collections.sort(pinnedSubreddits);
            }
            for (String sr : pinnedSubreddits) {
                this.mAdapter.appendToGroup(5, (Item) makeSubredditItem(sr, isFirst));
                isFirst = false;
            }
        }
    }

    private void setBlockedSubreddits() {
        AppCompatActivity appCompatActivity = this.mActivity;
        List<String> blockedSubreddits = PrefsUtility.pref_blocked_subreddits(appCompatActivity, PreferenceManager.getDefaultSharedPreferences(appCompatActivity));
        AppCompatActivity appCompatActivity2 = this.mActivity;
        BlockedSubredditSort blockedSubredditsSort = PrefsUtility.pref_behaviour_blocked_subredditsort(appCompatActivity2, PreferenceManager.getDefaultSharedPreferences(appCompatActivity2));
        if (blockedSubreddits != null) {
            this.mAdapter.removeAllFromGroup(7);
            this.mAdapter.removeAllFromGroup(6);
        }
        if (!blockedSubreddits.isEmpty()) {
            this.mAdapter.appendToGroup(6, (Item) new GroupedRecyclerViewItemListSectionHeaderView(this.mActivity.getString(R.string.mainmenu_header_subreddits_blocked)));
            if (AnonymousClass9.$SwitchMap$org$quantumbadger$redreader$common$PrefsUtility$BlockedSubredditSort[blockedSubredditsSort.ordinal()] == 1) {
                Collections.sort(blockedSubreddits);
            }
            boolean isFirst = true;
            for (String sr : blockedSubreddits) {
                this.mAdapter.appendToGroup(7, (Item) makeSubredditItem(sr, isFirst));
                isFirst = false;
            }
        }
    }

    /* access modifiers changed from: private */
    public void showMultiredditsHeader(@NonNull Context context) {
        General.checkThisIsUIThread();
        if (this.mMultiredditHeaderItem == null) {
            this.mMultiredditHeaderItem = new GroupedRecyclerViewItemListSectionHeaderView(context.getString(R.string.mainmenu_header_multireddits));
            this.mAdapter.appendToGroup(8, this.mMultiredditHeaderItem);
        }
    }

    /* access modifiers changed from: private */
    public void hideMultiredditsHeader() {
        General.checkThisIsUIThread();
        this.mMultiredditHeaderItem = null;
        this.mAdapter.removeAllFromGroup(8);
    }

    public void setMultiredditsError(final ErrorView errorView) {
        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
            public void run() {
                MainMenuListingManager.this.mAdapter.removeAllFromGroup(9);
                MainMenuListingManager.this.mAdapter.appendToGroup(9, (Item) new GroupedRecyclerViewItemFrameLayout(errorView));
            }
        });
    }

    public void setSubredditsError(final ErrorView errorView) {
        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
            public void run() {
                MainMenuListingManager.this.mAdapter.removeAllFromGroup(11);
                MainMenuListingManager.this.mAdapter.appendToGroup(11, (Item) new GroupedRecyclerViewItemFrameLayout(errorView));
            }
        });
    }

    public void setSubreddits(Collection<String> subscriptions) {
        final ArrayList<String> subscriptionsSorted = new ArrayList<>(subscriptions);
        Collections.sort(subscriptionsSorted);
        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
            public void run() {
                GroupedRecyclerViewItemListItemView item;
                if (MainMenuListingManager.this.mSubredditSubscriptions != null && MainMenuListingManager.this.mSubredditSubscriptions.equals(subscriptionsSorted)) {
                    return;
                }
                if (!PrefsUtility.pref_show_subscribed_subreddits_main_menu(MainMenuListingManager.this.mActivity, PreferenceManager.getDefaultSharedPreferences(MainMenuListingManager.this.mActivity))) {
                    MainMenuListingManager.this.mAdapter.removeAllFromGroup(10);
                    MainMenuListingManager.this.mAdapter.removeAllFromGroup(11);
                    return;
                }
                MainMenuListingManager.this.mSubredditSubscriptions = subscriptionsSorted;
                MainMenuListingManager.this.mAdapter.removeAllFromGroup(11);
                boolean isFirst = true;
                Iterator it = subscriptionsSorted.iterator();
                while (it.hasNext()) {
                    String subreddit = (String) it.next();
                    try {
                        item = MainMenuListingManager.this.makeSubredditItem(RedditSubreddit.getDisplayNameFromCanonicalName(RedditSubreddit.getCanonicalName(subreddit)), isFirst);
                    } catch (InvalidSubredditNameException e) {
                        MainMenuListingManager mainMenuListingManager = MainMenuListingManager.this;
                        StringBuilder sb = new StringBuilder();
                        sb.append("Invalid: ");
                        sb.append(subreddit);
                        item = mainMenuListingManager.makeSubredditItem(sb.toString(), isFirst);
                    }
                    MainMenuListingManager.this.mAdapter.appendToGroup(11, (Item) item);
                    isFirst = false;
                }
            }
        });
    }

    public void setMultireddits(Collection<String> subscriptions) {
        final ArrayList<String> subscriptionsSorted = new ArrayList<>(subscriptions);
        Collections.sort(subscriptionsSorted);
        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
            public void run() {
                if (MainMenuListingManager.this.mMultiredditSubscriptions != null && MainMenuListingManager.this.mMultiredditSubscriptions.equals(subscriptionsSorted)) {
                    return;
                }
                if (!PrefsUtility.pref_show_multireddit_main_menu(MainMenuListingManager.this.mActivity, PreferenceManager.getDefaultSharedPreferences(MainMenuListingManager.this.mActivity))) {
                    MainMenuListingManager.this.mAdapter.removeAllFromGroup(8);
                    MainMenuListingManager.this.mAdapter.removeAllFromGroup(9);
                    return;
                }
                MainMenuListingManager.this.mMultiredditSubscriptions = subscriptionsSorted;
                MainMenuListingManager.this.mAdapter.removeAllFromGroup(9);
                if (subscriptionsSorted.isEmpty()) {
                    MainMenuListingManager.this.hideMultiredditsHeader();
                } else {
                    MainMenuListingManager mainMenuListingManager = MainMenuListingManager.this;
                    mainMenuListingManager.showMultiredditsHeader(mainMenuListingManager.mContext);
                    boolean isFirst = true;
                    Iterator it = subscriptionsSorted.iterator();
                    while (it.hasNext()) {
                        MainMenuListingManager.this.mAdapter.appendToGroup(9, (Item) MainMenuListingManager.this.makeMultiredditItem((String) it.next(), isFirst));
                        isFirst = false;
                    }
                }
            }
        });
    }

    private GroupedRecyclerViewItemListItemView makeItem(int nameRes, int action, @Nullable Drawable icon, boolean hideDivider) {
        return makeItem(this.mContext.getString(nameRes), action, icon, hideDivider);
    }

    private GroupedRecyclerViewItemListItemView makeItem(@NonNull String name, final int action, @Nullable Drawable icon, boolean hideDivider) {
        GroupedRecyclerViewItemListItemView groupedRecyclerViewItemListItemView = new GroupedRecyclerViewItemListItemView(icon, name, hideDivider, new OnClickListener() {
            public void onClick(View view) {
                MainMenuListingManager.this.mListener.onSelected(action);
            }
        }, null);
        return groupedRecyclerViewItemListItemView;
    }

    /* access modifiers changed from: private */
    public GroupedRecyclerViewItemListItemView makeSubredditItem(final String name, boolean hideDivider) {
        GroupedRecyclerViewItemListItemView groupedRecyclerViewItemListItemView = new GroupedRecyclerViewItemListItemView(null, name, hideDivider, new OnClickListener() {
            public void onClick(View view) {
                try {
                    String canonicalName = RedditSubreddit.getCanonicalName(name);
                    if (canonicalName.startsWith("/r/")) {
                        MainMenuListingManager.this.mListener.onSelected((PostListingURL) SubredditPostListURL.getSubreddit(canonicalName));
                    } else {
                        LinkHandler.onLinkClicked(MainMenuListingManager.this.mActivity, canonicalName);
                    }
                } catch (InvalidSubredditNameException e) {
                    throw new RuntimeException(e);
                }
            }
        }, new OnLongClickListener() {
            public boolean onLongClick(View view) {
                try {
                    EnumSet<SubredditAction> itemPref = PrefsUtility.pref_menus_subreddit_context_items(MainMenuListingManager.this.mActivity, PreferenceManager.getDefaultSharedPreferences(MainMenuListingManager.this.mActivity));
                    List pref_pinned_subreddits = PrefsUtility.pref_pinned_subreddits(MainMenuListingManager.this.mActivity, PreferenceManager.getDefaultSharedPreferences(MainMenuListingManager.this.mActivity));
                    if (itemPref.isEmpty()) {
                        return true;
                    }
                    final String subredditCanonicalName = RedditSubreddit.getCanonicalName(name);
                    final ArrayList<SubredditMenuItem> menu = new ArrayList<>();
                    if (itemPref.contains(SubredditAction.COPY_URL)) {
                        menu.add(new SubredditMenuItem(MainMenuListingManager.this.mActivity, R.string.action_copy_link, SubredditAction.COPY_URL));
                    }
                    if (itemPref.contains(SubredditAction.EXTERNAL)) {
                        menu.add(new SubredditMenuItem(MainMenuListingManager.this.mActivity, R.string.action_external, SubredditAction.EXTERNAL));
                    }
                    if (itemPref.contains(SubredditAction.SHARE)) {
                        menu.add(new SubredditMenuItem(MainMenuListingManager.this.mActivity, R.string.action_share, SubredditAction.SHARE));
                    }
                    if (itemPref.contains(SubredditAction.BLOCK)) {
                        if (PrefsUtility.pref_blocked_subreddits(MainMenuListingManager.this.mActivity, PreferenceManager.getDefaultSharedPreferences(MainMenuListingManager.this.mActivity)).contains(subredditCanonicalName)) {
                            menu.add(new SubredditMenuItem(MainMenuListingManager.this.mActivity, R.string.unblock_subreddit, SubredditAction.UNBLOCK));
                        } else {
                            menu.add(new SubredditMenuItem(MainMenuListingManager.this.mActivity, R.string.block_subreddit, SubredditAction.BLOCK));
                        }
                    }
                    if (!RedditAccountManager.getInstance(MainMenuListingManager.this.mActivity).getDefaultAccount().isAnonymous() && itemPref.contains(SubredditAction.SUBSCRIBE)) {
                        RedditSubredditSubscriptionManager subscriptionManager = RedditSubredditSubscriptionManager.getSingleton(MainMenuListingManager.this.mActivity, RedditAccountManager.getInstance(MainMenuListingManager.this.mActivity).getDefaultAccount());
                        if (subscriptionManager.areSubscriptionsReady()) {
                            if (subscriptionManager.getSubscriptionState(subredditCanonicalName) == SubredditSubscriptionState.SUBSCRIBED) {
                                menu.add(new SubredditMenuItem(MainMenuListingManager.this.mActivity, R.string.options_unsubscribe, SubredditAction.UNSUBSCRIBE));
                            } else {
                                menu.add(new SubredditMenuItem(MainMenuListingManager.this.mActivity, R.string.options_subscribe, SubredditAction.SUBSCRIBE));
                            }
                        }
                    }
                    String[] menuText = new String[menu.size()];
                    for (int i = 0; i < menuText.length; i++) {
                        menuText[i] = ((SubredditMenuItem) menu.get(i)).title;
                    }
                    Builder builder = new Builder(MainMenuListingManager.this.mActivity);
                    builder.setItems(menuText, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            MainMenuListingManager.this.onSubredditActionMenuItemSelected(subredditCanonicalName, MainMenuListingManager.this.mActivity, ((SubredditMenuItem) menu.get(which)).action);
                        }
                    });
                    AlertDialog alert = builder.create();
                    alert.setCanceledOnTouchOutside(true);
                    alert.show();
                    return true;
                } catch (InvalidSubredditNameException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        return groupedRecyclerViewItemListItemView;
    }

    /* access modifiers changed from: private */
    public void onSubredditActionMenuItemSelected(String subredditCanonicalName, AppCompatActivity activity, SubredditAction action) {
        try {
            String url = Reddit.getNonAPIUri(subredditCanonicalName).toString();
            RedditSubredditSubscriptionManager subMan = RedditSubredditSubscriptionManager.getSingleton(activity, RedditAccountManager.getInstance(activity).getDefaultAccount());
            List<String> pinnedSubreddits = PrefsUtility.pref_pinned_subreddits(this.mActivity, PreferenceManager.getDefaultSharedPreferences(this.mActivity));
            List<String> blockedSubreddits = PrefsUtility.pref_blocked_subreddits(this.mActivity, PreferenceManager.getDefaultSharedPreferences(this.mActivity));
            switch (action) {
                case SHARE:
                    Intent mailer = new Intent("android.intent.action.SEND");
                    mailer.setType("text/plain");
                    mailer.putExtra("android.intent.extra.TEXT", url);
                    activity.startActivity(Intent.createChooser(mailer, activity.getString(R.string.action_share)));
                    return;
                case COPY_URL:
                    ((ClipboardManager) activity.getSystemService("clipboard")).setText(url);
                    return;
                case EXTERNAL:
                    Intent intent = new Intent("android.intent.action.VIEW");
                    intent.setData(Uri.parse(url));
                    activity.startActivity(intent);
                    return;
                case PIN:
                    if (!pinnedSubreddits.contains(subredditCanonicalName)) {
                        PrefsUtility.pref_pinned_subreddits_add(this.mActivity, PreferenceManager.getDefaultSharedPreferences(this.mActivity), subredditCanonicalName);
                        return;
                    } else {
                        Toast.makeText(this.mActivity, R.string.mainmenu_toast_subscribed, 0).show();
                        return;
                    }
                case UNPIN:
                    if (pinnedSubreddits.contains(subredditCanonicalName)) {
                        PrefsUtility.pref_pinned_subreddits_remove(this.mActivity, PreferenceManager.getDefaultSharedPreferences(this.mActivity), subredditCanonicalName);
                        return;
                    } else {
                        Toast.makeText(this.mActivity, R.string.mainmenu_toast_not_pinned, 0).show();
                        return;
                    }
                case BLOCK:
                    if (!blockedSubreddits.contains(subredditCanonicalName)) {
                        PrefsUtility.pref_blocked_subreddits_add(this.mActivity, PreferenceManager.getDefaultSharedPreferences(this.mActivity), subredditCanonicalName);
                        return;
                    } else {
                        Toast.makeText(this.mActivity, R.string.mainmenu_toast_blocked, 0).show();
                        return;
                    }
                case UNBLOCK:
                    if (blockedSubreddits.contains(subredditCanonicalName)) {
                        PrefsUtility.pref_blocked_subreddits_remove(this.mActivity, PreferenceManager.getDefaultSharedPreferences(this.mActivity), subredditCanonicalName);
                        return;
                    } else {
                        Toast.makeText(this.mActivity, R.string.mainmenu_toast_not_blocked, 0).show();
                        return;
                    }
                case SUBSCRIBE:
                    if (subMan.getSubscriptionState(subredditCanonicalName) == SubredditSubscriptionState.NOT_SUBSCRIBED) {
                        subMan.subscribe(subredditCanonicalName, activity);
                        setPinnedSubreddits();
                        setBlockedSubreddits();
                        Toast.makeText(this.mActivity, R.string.options_subscribing, 0).show();
                        return;
                    }
                    Toast.makeText(this.mActivity, R.string.mainmenu_toast_subscribed, 0).show();
                    return;
                case UNSUBSCRIBE:
                    if (subMan.getSubscriptionState(subredditCanonicalName) == SubredditSubscriptionState.SUBSCRIBED) {
                        subMan.unsubscribe(subredditCanonicalName, activity);
                        setPinnedSubreddits();
                        setBlockedSubreddits();
                        Toast.makeText(this.mActivity, R.string.options_unsubscribing, 0).show();
                        return;
                    }
                    Toast.makeText(this.mActivity, R.string.mainmenu_toast_not_subscribed, 0).show();
                    return;
                default:
                    return;
            }
        } catch (InvalidSubredditNameException ex) {
            throw new RuntimeException(ex);
        }
    }

    /* access modifiers changed from: private */
    public GroupedRecyclerViewItemListItemView makeMultiredditItem(final String name, boolean hideDivider) {
        GroupedRecyclerViewItemListItemView groupedRecyclerViewItemListItemView = new GroupedRecyclerViewItemListItemView(null, name, hideDivider, new OnClickListener() {
            public void onClick(View view) {
                MainMenuListingManager.this.mListener.onSelected((PostListingURL) MultiredditPostListURL.getMultireddit(name));
            }
        }, null);
        return groupedRecyclerViewItemListItemView;
    }
}
