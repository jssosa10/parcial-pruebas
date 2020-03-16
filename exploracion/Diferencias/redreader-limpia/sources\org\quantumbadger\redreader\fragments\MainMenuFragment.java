package org.quantumbadger.redreader.fragments;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.HashSet;
import org.apache.commons.lang3.time.DateUtils;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.adapters.MainMenuListingManager;
import org.quantumbadger.redreader.adapters.MainMenuSelectionListener;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.common.TimestampBound;
import org.quantumbadger.redreader.common.TimestampBound.MoreRecentThanBound;
import org.quantumbadger.redreader.io.RequestResponseHandler;
import org.quantumbadger.redreader.reddit.api.RedditMultiredditSubscriptionManager;
import org.quantumbadger.redreader.reddit.api.RedditMultiredditSubscriptionManager.MultiredditListChangeListener;
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager;
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.SubredditSubscriptionStateChangeListener;
import org.quantumbadger.redreader.reddit.api.SubredditRequestFailure;
import org.quantumbadger.redreader.reddit.url.PostListingURL;
import org.quantumbadger.redreader.views.ScrollbarRecyclerViewManager;
import org.quantumbadger.redreader.views.liststatus.ErrorView;

public class MainMenuFragment extends RRFragment implements MainMenuSelectionListener, SubredditSubscriptionStateChangeListener, MultiredditListChangeListener {
    public static final int MENU_MENU_ACTION_ALL = 10;
    public static final int MENU_MENU_ACTION_CUSTOM = 9;
    public static final int MENU_MENU_ACTION_DOWNVOTED = 5;
    public static final int MENU_MENU_ACTION_FRONTPAGE = 0;
    public static final int MENU_MENU_ACTION_HIDDEN = 8;
    public static final int MENU_MENU_ACTION_INBOX = 2;
    public static final int MENU_MENU_ACTION_MODMAIL = 7;
    public static final int MENU_MENU_ACTION_POPULAR = 11;
    public static final int MENU_MENU_ACTION_PROFILE = 1;
    public static final int MENU_MENU_ACTION_RANDOM = 12;
    public static final int MENU_MENU_ACTION_RANDOM_NSFW = 13;
    public static final int MENU_MENU_ACTION_SAVED = 6;
    public static final int MENU_MENU_ACTION_SUBMITTED = 3;
    public static final int MENU_MENU_ACTION_UPVOTED = 4;
    private final MainMenuListingManager mManager;
    private final View mOuter;

    @Retention(RetentionPolicy.SOURCE)
    public @interface MainMenuAction {
    }

    public enum MainMenuShortcutItems {
        FRONTPAGE,
        POPULAR,
        ALL,
        CUSTOM,
        RANDOM,
        RANDOM_NSFW
    }

    public enum MainMenuUserItems {
        PROFILE,
        INBOX,
        SUBMITTED,
        SAVED,
        HIDDEN,
        UPVOTED,
        DOWNVOTED,
        MODMAIL
    }

    public MainMenuFragment(AppCompatActivity parent, Bundle savedInstanceState, boolean force) {
        super(parent, savedInstanceState);
        final Context context = getActivity();
        RedditAccount user = RedditAccountManager.getInstance(context).getDefaultAccount();
        ScrollbarRecyclerViewManager recyclerViewManager = new ScrollbarRecyclerViewManager(parent, null, false);
        this.mOuter = recyclerViewManager.getOuterView();
        RecyclerView recyclerView = recyclerViewManager.getRecyclerView();
        this.mManager = new MainMenuListingManager(getActivity(), this, user);
        recyclerView.setAdapter(this.mManager.getAdapter());
        int paddingPx = General.dpToPixels(context, 8.0f);
        recyclerView.setPadding(paddingPx, 0, paddingPx, 0);
        recyclerView.setClipToPadding(false);
        TypedArray appearance = context.obtainStyledAttributes(new int[]{R.attr.rrListItemBackgroundCol});
        getActivity().getWindow().setBackgroundDrawable(new ColorDrawable(appearance.getColor(0, General.COLOR_INVALID)));
        appearance.recycle();
        final RedditMultiredditSubscriptionManager multiredditSubscriptionManager = RedditMultiredditSubscriptionManager.getSingleton(context, user);
        final RedditSubredditSubscriptionManager subredditSubscriptionManager = RedditSubredditSubscriptionManager.getSingleton(context, user);
        if (force) {
            multiredditSubscriptionManager.triggerUpdate(new RequestResponseHandler<HashSet<String>, SubredditRequestFailure>() {
                public void onRequestFailed(SubredditRequestFailure failureReason) {
                    MainMenuFragment.this.onMultiredditError(failureReason.asError(context));
                }

                public void onRequestSuccess(HashSet<String> result, long timeCached) {
                    multiredditSubscriptionManager.addListener(MainMenuFragment.this);
                    MainMenuFragment.this.onMultiredditSubscriptionsChanged(result);
                }
            }, TimestampBound.NONE);
            subredditSubscriptionManager.triggerUpdate(new RequestResponseHandler<HashSet<String>, SubredditRequestFailure>() {
                public void onRequestFailed(SubredditRequestFailure failureReason) {
                    MainMenuFragment.this.onSubredditError(failureReason.asError(context));
                }

                public void onRequestSuccess(HashSet<String> result, long timeCached) {
                    subredditSubscriptionManager.addListener(MainMenuFragment.this);
                    MainMenuFragment.this.onSubredditSubscriptionsChanged(result);
                }
            }, TimestampBound.NONE);
            return;
        }
        multiredditSubscriptionManager.addListener(this);
        subredditSubscriptionManager.addListener(this);
        if (multiredditSubscriptionManager.areSubscriptionsReady()) {
            onMultiredditSubscriptionsChanged(multiredditSubscriptionManager.getSubscriptionList());
        }
        if (subredditSubscriptionManager.areSubscriptionsReady()) {
            onSubredditSubscriptionsChanged(subredditSubscriptionManager.getSubscriptionList());
        }
        MoreRecentThanBound oneHour = TimestampBound.notOlderThan(DateUtils.MILLIS_PER_HOUR);
        multiredditSubscriptionManager.triggerUpdate(null, oneHour);
        subredditSubscriptionManager.triggerUpdate(null, oneHour);
    }

    public View getView() {
        return this.mOuter;
    }

    public Bundle onSaveInstanceState() {
        return null;
    }

    public void onSubredditSubscriptionsChanged(Collection<String> subscriptions) {
        this.mManager.setSubreddits(subscriptions);
    }

    public void onMultiredditSubscriptionsChanged(Collection<String> subscriptions) {
        this.mManager.setMultireddits(subscriptions);
    }

    /* access modifiers changed from: private */
    public void onSubredditError(RRError error) {
        this.mManager.setSubredditsError(new ErrorView(getActivity(), error));
    }

    /* access modifiers changed from: private */
    public void onMultiredditError(RRError error) {
        this.mManager.setMultiredditsError(new ErrorView(getActivity(), error));
    }

    public void onSelected(int type) {
        ((MainMenuSelectionListener) getActivity()).onSelected(type);
    }

    public void onSelected(PostListingURL postListingURL) {
        ((MainMenuSelectionListener) getActivity()).onSelected(postListingURL);
    }

    public void onSubredditSubscriptionListUpdated(RedditSubredditSubscriptionManager subredditSubscriptionManager) {
        onSubredditSubscriptionsChanged(subredditSubscriptionManager.getSubscriptionList());
    }

    public void onMultiredditListUpdated(RedditMultiredditSubscriptionManager multiredditSubscriptionManager) {
        onMultiredditSubscriptionsChanged(multiredditSubscriptionManager.getSubscriptionList());
    }

    public void onSubredditSubscriptionAttempted(RedditSubredditSubscriptionManager subredditSubscriptionManager) {
    }

    public void onSubredditUnsubscriptionAttempted(RedditSubredditSubscriptionManager subredditSubscriptionManager) {
    }
}
