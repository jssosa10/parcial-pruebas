package org.quantumbadger.redreader.fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.UUID;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.activities.CommentReplyActivity;
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuCommentsListener;
import org.quantumbadger.redreader.adapters.FilteredCommentListingManager;
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter.Item;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategy;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.PrefsUtility.SelfpostAction;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.common.RRThemeAttributes;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.reddit.CommentListingRequest;
import org.quantumbadger.redreader.reddit.CommentListingRequest.Listener;
import org.quantumbadger.redreader.reddit.RedditCommentListItem;
import org.quantumbadger.redreader.reddit.api.RedditAPICommentAction;
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;
import org.quantumbadger.redreader.reddit.prepared.RedditRenderableComment;
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;
import org.quantumbadger.redreader.views.RedditCommentView;
import org.quantumbadger.redreader.views.RedditCommentView.CommentListener;
import org.quantumbadger.redreader.views.RedditPostHeaderView;
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener;
import org.quantumbadger.redreader.views.ScrollbarRecyclerViewManager;
import org.quantumbadger.redreader.views.bezelmenu.BezelSwipeOverlay;
import org.quantumbadger.redreader.views.bezelmenu.BezelSwipeOverlay.BezelSwipeListener;
import org.quantumbadger.redreader.views.bezelmenu.SideToolbarOverlay;
import org.quantumbadger.redreader.views.bezelmenu.SideToolbarOverlay.SideToolbarPosition;
import org.quantumbadger.redreader.views.liststatus.CommentSubThreadView;
import org.quantumbadger.redreader.views.liststatus.ErrorView;

public class CommentListingFragment extends RRFragment implements PostSelectionListener, CommentListener, Listener {
    private static final String SAVEDSTATE_FIRST_VISIBLE_POS = "firstVisiblePosition";
    private boolean isArchived;
    private final ArrayList<RedditURL> mAllUrls;
    private Long mCachedTimestamp = null;
    private final float mCommentFontScale;
    /* access modifiers changed from: private */
    public final FilteredCommentListingManager mCommentListingManager;
    private final DownloadStrategy mDownloadStrategy;
    @Nullable
    private final LinearLayout mFloatingToolbar;
    private final FrameLayout mOuterFrame;
    /* access modifiers changed from: private */
    public RedditPreparedPost mPost = null;
    private Integer mPreviousFirstVisibleItemPosition;
    /* access modifiers changed from: private */
    public final RecyclerView mRecyclerView;
    private final UUID mSession;
    private final boolean mShowLinkButtons;
    private final LinkedList<RedditURL> mUrlsToDownload;
    private final RedditAccount mUser;

    public CommentListingFragment(AppCompatActivity parent, Bundle savedInstanceState, ArrayList<RedditURL> urls, UUID session, String searchString, boolean forceDownload) {
        final AppCompatActivity appCompatActivity = parent;
        Bundle bundle = savedInstanceState;
        super(parent, savedInstanceState);
        if (bundle != null) {
            this.mPreviousFirstVisibleItemPosition = Integer.valueOf(bundle.getInt(SAVEDSTATE_FIRST_VISIBLE_POS));
        }
        this.mCommentListingManager = new FilteredCommentListingManager(appCompatActivity, searchString);
        this.mAllUrls = urls;
        this.mUrlsToDownload = new LinkedList<>(this.mAllUrls);
        this.mSession = session;
        if (forceDownload) {
            this.mDownloadStrategy = DownloadStrategyAlways.INSTANCE;
        } else {
            this.mDownloadStrategy = DownloadStrategyIfNotCached.INSTANCE;
        }
        this.mUser = RedditAccountManager.getInstance(getActivity()).getDefaultAccount();
        parent.invalidateOptionsMenu();
        final Context context = getActivity();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.mCommentFontScale = PrefsUtility.appearance_fontscale_comments(context, prefs);
        this.mShowLinkButtons = PrefsUtility.pref_appearance_linkbuttons(context, prefs);
        this.mOuterFrame = new FrameLayout(context);
        ScrollbarRecyclerViewManager recyclerViewManager = new ScrollbarRecyclerViewManager(context, null, false);
        if ((appCompatActivity instanceof OptionsMenuCommentsListener) && PrefsUtility.pref_behaviour_enable_swipe_refresh(context, prefs)) {
            recyclerViewManager.enablePullToRefresh(new OnRefreshListener() {
                public void onRefresh() {
                    ((OptionsMenuCommentsListener) appCompatActivity).onRefreshComments();
                }
            });
        }
        this.mRecyclerView = recyclerViewManager.getRecyclerView();
        this.mCommentListingManager.setLayoutManager((LinearLayoutManager) this.mRecyclerView.getLayoutManager());
        this.mRecyclerView.setAdapter(this.mCommentListingManager.getAdapter());
        this.mOuterFrame.addView(recyclerViewManager.getOuterView());
        this.mRecyclerView.setItemAnimator(null);
        if (!PrefsUtility.pref_appearance_comments_show_floating_toolbar(context, prefs)) {
            this.mFloatingToolbar = null;
        } else {
            this.mFloatingToolbar = (LinearLayout) LayoutInflater.from(context).inflate(R.layout.floating_toolbar, this.mOuterFrame, false);
            FrameLayout floatingToolbarContainer = new FrameLayout(context);
            floatingToolbarContainer.addView(this.mFloatingToolbar);
            this.mOuterFrame.addView(floatingToolbarContainer);
            if (PrefsUtility.isNightMode(context)) {
                this.mFloatingToolbar.setBackgroundColor(Color.argb(204, 51, 51, 51));
            }
            int buttonVPadding = General.dpToPixels(context, 12.0f);
            int buttonHPadding = General.dpToPixels(context, 16.0f);
            ImageButton previousButton = (ImageButton) LayoutInflater.from(context).inflate(R.layout.flat_image_button, this.mFloatingToolbar, false);
            previousButton.setPadding(buttonHPadding, buttonVPadding, buttonHPadding, buttonVPadding);
            previousButton.setImageResource(R.drawable.ic_ff_up_dark);
            previousButton.setContentDescription(getString(R.string.button_prev_comment_parent));
            this.mFloatingToolbar.addView(previousButton);
            previousButton.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) CommentListingFragment.this.mRecyclerView.getLayoutManager();
                    int pos = layoutManager.findFirstVisibleItemPosition();
                    while (true) {
                        pos--;
                        if (pos > 0) {
                            Item item = CommentListingFragment.this.mCommentListingManager.getItemAtPosition(pos);
                            if ((item instanceof RedditCommentListItem) && ((RedditCommentListItem) item).isComment() && ((RedditCommentListItem) item).getIndent() == 0) {
                                layoutManager.scrollToPositionWithOffset(pos, 0);
                                return;
                            }
                        } else {
                            layoutManager.scrollToPositionWithOffset(0, 0);
                            return;
                        }
                    }
                }
            });
            previousButton.setOnLongClickListener(new OnLongClickListener() {
                public boolean onLongClick(View view) {
                    General.quickToast(context, (int) R.string.button_prev_comment_parent);
                    return true;
                }
            });
            ImageButton nextButton = (ImageButton) LayoutInflater.from(context).inflate(R.layout.flat_image_button, this.mFloatingToolbar, false);
            nextButton.setPadding(buttonHPadding, buttonVPadding, buttonHPadding, buttonVPadding);
            nextButton.setImageResource(R.drawable.ic_ff_down_dark);
            nextButton.setContentDescription(getString(R.string.button_next_comment_parent));
            this.mFloatingToolbar.addView(nextButton);
            nextButton.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) CommentListingFragment.this.mRecyclerView.getLayoutManager();
                    int pos = layoutManager.findFirstVisibleItemPosition();
                    while (true) {
                        pos++;
                        if (pos < layoutManager.getItemCount()) {
                            Item item = CommentListingFragment.this.mCommentListingManager.getItemAtPosition(pos);
                            if ((item instanceof RedditCommentListItem) && ((RedditCommentListItem) item).isComment() && ((RedditCommentListItem) item).getIndent() == 0) {
                                layoutManager.scrollToPositionWithOffset(pos, 0);
                                return;
                            }
                        } else {
                            return;
                        }
                    }
                }
            });
            nextButton.setOnLongClickListener(new OnLongClickListener() {
                public boolean onLongClick(View view) {
                    General.quickToast(context, (int) R.string.button_next_comment_parent);
                    return true;
                }
            });
        }
        final SideToolbarOverlay toolbarOverlay = new SideToolbarOverlay(context);
        BezelSwipeOverlay bezelOverlay = new BezelSwipeOverlay(context, new BezelSwipeListener() {
            public boolean onSwipe(int edge) {
                if (CommentListingFragment.this.mPost == null) {
                    return false;
                }
                toolbarOverlay.setContents(CommentListingFragment.this.mPost.generateToolbar(CommentListingFragment.this.getActivity(), true, toolbarOverlay));
                toolbarOverlay.show(edge == 0 ? SideToolbarPosition.LEFT : SideToolbarPosition.RIGHT);
                return true;
            }

            public boolean onTap() {
                if (!toolbarOverlay.isShown()) {
                    return false;
                }
                toolbarOverlay.hide();
                return true;
            }
        });
        this.mOuterFrame.addView(bezelOverlay);
        this.mOuterFrame.addView(toolbarOverlay);
        bezelOverlay.getLayoutParams().width = -1;
        bezelOverlay.getLayoutParams().height = -1;
        toolbarOverlay.getLayoutParams().width = -1;
        toolbarOverlay.getLayoutParams().height = -1;
        makeNextRequest(context);
    }

    public void handleCommentVisibilityToggle(RedditCommentView view) {
        RedditChangeDataManager changeDataManager = RedditChangeDataManager.getInstance(this.mUser);
        RedditCommentListItem item = view.getComment();
        if (item.isComment()) {
            RedditRenderableComment comment = item.asComment();
            changeDataManager.markHidden(RRTime.utcCurrentTimeMillis(), comment, Boolean.valueOf(!comment.isCollapsed(changeDataManager)));
            this.mCommentListingManager.updateHiddenStatus();
            LinearLayoutManager layoutManager = (LinearLayoutManager) this.mRecyclerView.getLayoutManager();
            int position = layoutManager.getPosition(view);
            if (position == layoutManager.findFirstVisibleItemPosition()) {
                layoutManager.scrollToPositionWithOffset(position, 0);
            }
        }
    }

    public View getView() {
        return this.mOuterFrame;
    }

    public Bundle onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putInt(SAVEDSTATE_FIRST_VISIBLE_POS, ((LinearLayoutManager) this.mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition());
        return bundle;
    }

    @SuppressLint({"WrongConstant"})
    private void makeNextRequest(Context context) {
        if (!this.mUrlsToDownload.isEmpty()) {
            new CommentListingRequest(context, this, getActivity(), (RedditURL) this.mUrlsToDownload.getFirst(), this.mAllUrls.size() == 1, (RedditURL) this.mUrlsToDownload.getFirst(), this.mUser, this.mSession, this.mDownloadStrategy, this);
        }
    }

    public void onCommentClicked(RedditCommentView view) {
        switch (PrefsUtility.pref_behaviour_actions_comment_tap(getActivity(), PreferenceManager.getDefaultSharedPreferences(getActivity()))) {
            case COLLAPSE:
                handleCommentVisibilityToggle(view);
                return;
            case ACTION_MENU:
                RedditCommentListItem item = view.getComment();
                if (item != null && item.isComment()) {
                    RedditAPICommentAction.showActionMenu(getActivity(), this, item.asComment(), view, RedditChangeDataManager.getInstance(this.mUser), this.isArchived);
                    return;
                }
                return;
            default:
                return;
        }
    }

    public void onCommentLongClicked(RedditCommentView view) {
        switch (PrefsUtility.pref_behaviour_actions_comment_longclick(getActivity(), PreferenceManager.getDefaultSharedPreferences(getActivity()))) {
            case COLLAPSE:
                handleCommentVisibilityToggle(view);
                return;
            case ACTION_MENU:
                RedditCommentListItem item = view.getComment();
                if (item != null && item.isComment()) {
                    RedditAPICommentAction.showActionMenu(getActivity(), this, item.asComment(), view, RedditChangeDataManager.getInstance(this.mUser), this.isArchived);
                    return;
                }
                return;
            default:
                return;
        }
    }

    public void onCommentListingRequestDownloadNecessary() {
        this.mCommentListingManager.setLoadingVisible(true);
    }

    public void onCommentListingRequestDownloadStarted() {
    }

    public void onCommentListingRequestException(Throwable t) {
        BugReportActivity.handleGlobalError((Context) getActivity(), t);
    }

    public void onCommentListingRequestFailure(RRError error) {
        this.mCommentListingManager.setLoadingVisible(false);
        this.mCommentListingManager.addFooterError(new ErrorView(getActivity(), error));
    }

    public void onCommentListingRequestCachedCopy(long timestamp) {
        this.mCachedTimestamp = Long.valueOf(timestamp);
    }

    public void onCommentListingRequestParseStart() {
        this.mCommentListingManager.setLoadingVisible(true);
    }

    public void onCommentListingRequestAuthorizing() {
        this.mCommentListingManager.setLoadingVisible(true);
    }

    public void onCommentListingRequestPostDownloaded(RedditPreparedPost post) {
        Context context = getActivity();
        if (this.mPost == null) {
            RRThemeAttributes attr = new RRThemeAttributes(context);
            this.mPost = post;
            this.isArchived = post.isArchived;
            this.mCommentListingManager.addPostHeader(new RedditPostHeaderView(getActivity(), this.mPost));
            final LinearLayoutManager layoutManager = (LinearLayoutManager) this.mRecyclerView.getLayoutManager();
            layoutManager.scrollToPositionWithOffset(0, 0);
            if (post.src.getSelfText() != null) {
                final ViewGroup selfText = post.src.getSelfText().buildView(getActivity(), Integer.valueOf(attr.rrMainTextCol), Float.valueOf(this.mCommentFontScale * 14.0f), this.mShowLinkButtons);
                selfText.setFocusable(false);
                selfText.setDescendantFocusability(393216);
                int paddingPx = General.dpToPixels(context, 10.0f);
                FrameLayout paddingLayout = new FrameLayout(context);
                final TextView collapsedView = new TextView(context);
                StringBuilder sb = new StringBuilder();
                sb.append("[ + ]  ");
                sb.append(getActivity().getString(R.string.collapsed_self_post));
                collapsedView.setText(sb.toString());
                collapsedView.setVisibility(8);
                collapsedView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
                paddingLayout.addView(selfText);
                paddingLayout.addView(collapsedView);
                paddingLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
                if (PrefsUtility.pref_behaviour_self_post_tap_actions(context, PreferenceManager.getDefaultSharedPreferences(context)) == SelfpostAction.COLLAPSE) {
                    paddingLayout.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            if (selfText.getVisibility() == 8) {
                                selfText.setVisibility(0);
                                collapsedView.setVisibility(8);
                                return;
                            }
                            selfText.setVisibility(8);
                            collapsedView.setVisibility(0);
                            layoutManager.scrollToPositionWithOffset(0, 0);
                        }
                    });
                }
                this.mCommentListingManager.addPostSelfText(paddingLayout);
            }
            if (!General.isTablet(context, PreferenceManager.getDefaultSharedPreferences(context))) {
                getActivity().setTitle(post.src.getTitle());
            }
            if (this.mCommentListingManager.isSearchListing()) {
                this.mCommentListingManager.addNotification(new CommentSubThreadView(getActivity(), ((RedditURL) this.mAllUrls.get(0)).asPostCommentListURL(), R.string.comment_header_search_thread_title));
            } else if (!this.mAllUrls.isEmpty() && ((RedditURL) this.mAllUrls.get(0)).pathType() == 7 && ((RedditURL) this.mAllUrls.get(0)).asPostCommentListURL().commentId != null) {
                this.mCommentListingManager.addNotification(new CommentSubThreadView(getActivity(), ((RedditURL) this.mAllUrls.get(0)).asPostCommentListURL(), R.string.comment_header_specific_thread_title));
            }
            Long l = this.mCachedTimestamp;
            if (l != null && RRTime.since(l.longValue()) > 600000) {
                TextView cacheNotif = (TextView) LayoutInflater.from(getActivity()).inflate(R.layout.cached_header, null, false);
                cacheNotif.setText(getActivity().getString(R.string.listing_cached, new Object[]{RRTime.formatDateTime(this.mCachedTimestamp.longValue(), getActivity())}));
                this.mCommentListingManager.addNotification(cacheNotif);
            }
        }
    }

    public void onCommentListingRequestAllItemsDownloaded(ArrayList<RedditCommentListItem> items) {
        this.mCommentListingManager.addComments(items);
        LinearLayout linearLayout = this.mFloatingToolbar;
        if (!(linearLayout == null || linearLayout.getVisibility() == 0)) {
            this.mFloatingToolbar.setVisibility(0);
            Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_from_bottom);
            animation.setInterpolator(new OvershootInterpolator());
            this.mFloatingToolbar.startAnimation(animation);
        }
        this.mUrlsToDownload.removeFirst();
        LinearLayoutManager layoutManager = (LinearLayoutManager) this.mRecyclerView.getLayoutManager();
        if (this.mPreviousFirstVisibleItemPosition != null && layoutManager.getItemCount() > this.mPreviousFirstVisibleItemPosition.intValue()) {
            layoutManager.scrollToPositionWithOffset(this.mPreviousFirstVisibleItemPosition.intValue(), 0);
            this.mPreviousFirstVisibleItemPosition = null;
        }
        if (this.mUrlsToDownload.isEmpty()) {
            if (this.mCommentListingManager.getCommentCount() == 0) {
                View emptyView = LayoutInflater.from(getContext()).inflate(R.layout.no_comments_yet, this.mRecyclerView, false);
                if (this.mCommentListingManager.isSearchListing()) {
                    ((TextView) emptyView.findViewById(R.id.empty_view_text)).setText(R.string.no_search_results);
                }
                this.mCommentListingManager.addViewToItems(emptyView);
            } else {
                View blankView = new View(getContext());
                blankView.setMinimumWidth(1);
                blankView.setMinimumHeight(General.dpToPixels(getContext(), 96.0f));
                this.mCommentListingManager.addViewToItems(blankView);
            }
            this.mCommentListingManager.setLoadingVisible(false);
            return;
        }
        makeNextRequest(getActivity());
    }

    public void onCreateOptionsMenu(Menu menu) {
        ArrayList<RedditURL> arrayList = this.mAllUrls;
        if (arrayList != null && arrayList.size() > 0 && ((RedditURL) this.mAllUrls.get(0)).pathType() == 7) {
            menu.add(R.string.action_reply);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle() == null || !item.getTitle().equals(getActivity().getString(R.string.action_reply))) {
            return false;
        }
        onParentReply();
        return true;
    }

    private void onParentReply() {
        if (this.mPost != null) {
            Intent intent = new Intent(getActivity(), CommentReplyActivity.class);
            intent.putExtra(CommentReplyActivity.PARENT_ID_AND_TYPE_KEY, this.mPost.src.getIdAndType());
            intent.putExtra(CommentReplyActivity.PARENT_MARKDOWN_KEY, this.mPost.src.getUnescapedSelfText());
            startActivity(intent);
            return;
        }
        General.quickToast((Context) getActivity(), (int) R.string.error_toast_parent_post_not_downloaded);
    }

    public void onPostSelected(RedditPreparedPost post) {
        ((PostSelectionListener) getActivity()).onPostSelected(post);
    }

    public void onPostCommentsSelected(RedditPreparedPost post) {
        ((PostSelectionListener) getActivity()).onPostCommentsSelected(post);
    }
}
