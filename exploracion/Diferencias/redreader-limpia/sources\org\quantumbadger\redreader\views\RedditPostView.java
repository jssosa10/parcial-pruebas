package org.quantumbadger.redreader.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.PrefsUtility.PostFlingAction;
import org.quantumbadger.redreader.fragments.PostListingFragment;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost.Action;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost.ThumbnailLoadedCallback;

public final class RedditPostView extends FlingableItemView implements ThumbnailLoadedCallback {
    private final LinearLayout commentsButton;
    private final TextView commentsText;
    private final float dpScale;
    /* access modifiers changed from: private */
    public final AppCompatActivity mActivity;
    private ActionDescriptionPair mLeftFlingAction;
    private final PostFlingAction mLeftFlingPref;
    private final LinearLayout mOuterView;
    private ActionDescriptionPair mRightFlingAction;
    private final PostFlingAction mRightFlingPref;
    private final ImageView overlayIcon;
    /* access modifiers changed from: private */
    public RedditPreparedPost post = null;
    private final int rrListItemBackgroundCol;
    private final int rrPostCommentsButtonBackCol;
    private final int rrPostTitleCol;
    private final int rrPostTitleReadCol;
    private final TextView subtitle;
    private final Handler thumbnailHandler;
    /* access modifiers changed from: private */
    public final ImageView thumbnailView;
    private final TextView title;
    /* access modifiers changed from: private */
    public int usageId = 0;

    private final class ActionDescriptionPair {
        public final Action action;
        public final int descriptionRes;

        private ActionDescriptionPair(Action action2, int descriptionRes2) {
            this.action = action2;
            this.descriptionRes = descriptionRes2;
        }
    }

    public interface PostSelectionListener {
        void onPostCommentsSelected(RedditPreparedPost redditPreparedPost);

        void onPostSelected(RedditPreparedPost redditPreparedPost);
    }

    /* access modifiers changed from: protected */
    public void onSetItemFlingPosition(float position) {
        this.mOuterView.setTranslationX(position);
    }

    /* access modifiers changed from: protected */
    @NonNull
    public String getFlingLeftText() {
        this.mLeftFlingAction = chooseFlingAction(this.mLeftFlingPref);
        ActionDescriptionPair actionDescriptionPair = this.mLeftFlingAction;
        if (actionDescriptionPair != null) {
            return this.mActivity.getString(actionDescriptionPair.descriptionRes);
        }
        return "Disabled";
    }

    /* access modifiers changed from: protected */
    @NonNull
    public String getFlingRightText() {
        this.mRightFlingAction = chooseFlingAction(this.mRightFlingPref);
        ActionDescriptionPair actionDescriptionPair = this.mRightFlingAction;
        if (actionDescriptionPair != null) {
            return this.mActivity.getString(actionDescriptionPair.descriptionRes);
        }
        return "Disabled";
    }

    /* access modifiers changed from: protected */
    public boolean allowFlingingLeft() {
        return this.mLeftFlingAction != null;
    }

    /* access modifiers changed from: protected */
    public boolean allowFlingingRight() {
        return this.mRightFlingAction != null;
    }

    /* access modifiers changed from: protected */
    public void onFlungLeft() {
        RedditPreparedPost.onActionMenuItemSelected(this.post, this.mActivity, this.mLeftFlingAction.action);
    }

    /* access modifiers changed from: protected */
    public void onFlungRight() {
        RedditPreparedPost.onActionMenuItemSelected(this.post, this.mActivity, this.mRightFlingAction.action);
    }

    private ActionDescriptionPair chooseFlingAction(PostFlingAction pref) {
        switch (pref) {
            case UPVOTE:
                if (this.post.isUpvoted()) {
                    return new ActionDescriptionPair(Action.UNVOTE, R.string.action_vote_remove);
                }
                return new ActionDescriptionPair(Action.UPVOTE, R.string.action_upvote);
            case DOWNVOTE:
                if (this.post.isDownvoted()) {
                    return new ActionDescriptionPair(Action.UNVOTE, R.string.action_vote_remove);
                }
                return new ActionDescriptionPair(Action.DOWNVOTE, R.string.action_downvote);
            case SAVE:
                if (this.post.isSaved()) {
                    return new ActionDescriptionPair(Action.UNSAVE, R.string.action_unsave);
                }
                return new ActionDescriptionPair(Action.SAVE, R.string.action_save);
            case HIDE:
                if (this.post.isHidden()) {
                    return new ActionDescriptionPair(Action.UNHIDE, R.string.action_unhide);
                }
                return new ActionDescriptionPair(Action.HIDE, R.string.action_hide);
            case COMMENTS:
                return new ActionDescriptionPair(Action.COMMENTS, R.string.action_comments_short);
            case LINK:
                return new ActionDescriptionPair(Action.LINK, R.string.action_link_short);
            case BROWSER:
                return new ActionDescriptionPair(Action.EXTERNAL, R.string.action_external_short);
            case ACTION_MENU:
                return new ActionDescriptionPair(Action.ACTION_MENU, R.string.action_actionmenu_short);
            case BACK:
                return new ActionDescriptionPair(Action.BACK, R.string.action_back);
            default:
                return null;
        }
    }

    public RedditPostView(Context context, final PostListingFragment fragmentParent, AppCompatActivity activity, boolean leftHandedMode) {
        super(context);
        this.mActivity = activity;
        this.thumbnailHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message msg) {
                if (RedditPostView.this.usageId == msg.what) {
                    RedditPostView.this.thumbnailView.setImageBitmap((Bitmap) msg.obj);
                }
            }
        };
        this.dpScale = context.getResources().getDisplayMetrics().density;
        float fontScale = PrefsUtility.appearance_fontscale_posts(context, PreferenceManager.getDefaultSharedPreferences(context));
        View rootView = LayoutInflater.from(context).inflate(R.layout.reddit_post, this, true);
        this.mOuterView = (LinearLayout) rootView.findViewById(R.id.reddit_post_layout);
        this.mOuterView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                fragmentParent.onPostSelected(RedditPostView.this.post);
            }
        });
        this.mOuterView.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                RedditPreparedPost.showActionMenu(RedditPostView.this.mActivity, RedditPostView.this.post);
                return true;
            }
        });
        if (leftHandedMode) {
            ArrayList<View> outerViewElements = new ArrayList<>(3);
            for (int i = this.mOuterView.getChildCount() - 1; i >= 0; i--) {
                outerViewElements.add(this.mOuterView.getChildAt(i));
                this.mOuterView.removeViewAt(i);
            }
            for (int i2 = 0; i2 < outerViewElements.size(); i2++) {
                this.mOuterView.addView((View) outerViewElements.get(i2));
            }
        }
        this.thumbnailView = (ImageView) rootView.findViewById(R.id.reddit_post_thumbnail_view);
        this.overlayIcon = (ImageView) rootView.findViewById(R.id.reddit_post_overlay_icon);
        this.title = (TextView) rootView.findViewById(R.id.reddit_post_title);
        this.subtitle = (TextView) rootView.findViewById(R.id.reddit_post_subtitle);
        this.commentsButton = (LinearLayout) rootView.findViewById(R.id.reddit_post_comments_button);
        this.commentsText = (TextView) this.commentsButton.findViewById(R.id.reddit_post_comments_text);
        this.commentsButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                fragmentParent.onPostCommentsSelected(RedditPostView.this.post);
            }
        });
        TextView textView = this.title;
        textView.setTextSize(0, textView.getTextSize() * fontScale);
        TextView textView2 = this.subtitle;
        textView2.setTextSize(0, textView2.getTextSize() * fontScale);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.mLeftFlingPref = PrefsUtility.pref_behaviour_fling_post_left(context, sharedPreferences);
        this.mRightFlingPref = PrefsUtility.pref_behaviour_fling_post_right(context, sharedPreferences);
        TypedArray attr = context.obtainStyledAttributes(new int[]{R.attr.rrPostTitleCol, R.attr.rrPostTitleReadCol, R.attr.rrListItemBackgroundCol, R.attr.rrPostCommentsButtonBackCol});
        this.rrPostTitleCol = attr.getColor(0, 0);
        this.rrPostTitleReadCol = attr.getColor(1, 0);
        this.rrListItemBackgroundCol = attr.getColor(2, 0);
        this.rrPostCommentsButtonBackCol = attr.getColor(3, 0);
        attr.recycle();
    }

    @UiThread
    public void reset(RedditPreparedPost data) {
        if (data != this.post) {
            this.usageId++;
            resetSwipeState();
            this.thumbnailView.setImageBitmap(data.getThumbnail(this, this.usageId));
            this.title.setText(data.src.getTitle());
            this.commentsText.setText(String.valueOf(data.src.getSrc().num_comments));
            if (data.hasThumbnail) {
                this.thumbnailView.setVisibility(0);
                this.thumbnailView.setMinimumWidth((int) (this.dpScale * 64.0f));
                this.thumbnailView.getLayoutParams().height = -1;
            } else {
                this.thumbnailView.setMinimumWidth(0);
                this.thumbnailView.setVisibility(8);
            }
        }
        RedditPreparedPost redditPreparedPost = this.post;
        if (redditPreparedPost != null) {
            redditPreparedPost.unbind(this);
        }
        data.bind(this);
        this.post = data;
        updateAppearance();
    }

    public void updateAppearance() {
        if (VERSION.SDK_INT >= 21) {
            this.mOuterView.setBackgroundResource(R.drawable.rr_postlist_item_selector_main);
            this.commentsButton.setBackgroundResource(R.drawable.rr_postlist_commentbutton_selector_main);
        } else {
            this.mOuterView.setBackgroundColor(this.rrListItemBackgroundCol);
            this.commentsButton.setBackgroundColor(this.rrPostCommentsButtonBackCol);
        }
        if (this.post.isRead()) {
            this.title.setTextColor(this.rrPostTitleReadCol);
        } else {
            this.title.setTextColor(this.rrPostTitleCol);
        }
        this.subtitle.setText(this.post.postListDescription);
        boolean overlayVisible = true;
        if (this.post.isSaved()) {
            this.overlayIcon.setImageResource(R.drawable.ic_action_star_filled_dark);
        } else if (this.post.isHidden()) {
            this.overlayIcon.setImageResource(R.drawable.ic_action_cross_dark);
        } else if (this.post.isUpvoted()) {
            this.overlayIcon.setImageResource(R.drawable.action_upvote_dark);
        } else if (this.post.isDownvoted()) {
            this.overlayIcon.setImageResource(R.drawable.action_downvote_dark);
        } else {
            overlayVisible = false;
        }
        if (overlayVisible) {
            this.overlayIcon.setVisibility(0);
        } else {
            this.overlayIcon.setVisibility(8);
        }
    }

    public void betterThumbnailAvailable(Bitmap thumbnail, int callbackUsageId) {
        Message msg = Message.obtain();
        msg.obj = thumbnail;
        msg.what = callbackUsageId;
        this.thumbnailHandler.sendMessage(msg);
    }
}
