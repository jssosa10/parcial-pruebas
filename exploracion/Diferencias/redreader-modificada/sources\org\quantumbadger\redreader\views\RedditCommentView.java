package org.quantumbadger.redreader.views;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.PrefsUtility.CommentFlingAction;
import org.quantumbadger.redreader.common.RRThemeAttributes;
import org.quantumbadger.redreader.fragments.CommentListingFragment;
import org.quantumbadger.redreader.reddit.RedditCommentListItem;
import org.quantumbadger.redreader.reddit.api.RedditAPICommentAction;
import org.quantumbadger.redreader.reddit.api.RedditAPICommentAction.RedditCommentAction;
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager;
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager.Listener;
import org.quantumbadger.redreader.reddit.prepared.RedditParsedComment;

public class RedditCommentView extends FlingableItemView implements Listener {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                ((RedditCommentView) msg.obj).update();
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Unknown message type ");
            sb.append(msg.what);
            throw new RuntimeException(sb.toString());
        }
    };
    private static final int HANDLER_REQUEST_COMMENT_CHANGED = 1;
    private final AppCompatActivity mActivity;
    private final FrameLayout mBodyHolder;
    private final RedditChangeDataManager mChangeDataManager;
    private RedditCommentListItem mComment;
    private final float mFontScale;
    @Nullable
    private final CommentListingFragment mFragment;
    private final TextView mHeader;
    private final IndentView mIndentView;
    private final LinearLayout mIndentedContent;
    @Nullable
    private ActionDescriptionPair mLeftFlingAction;
    /* access modifiers changed from: private */
    public final CommentListener mListener;
    @Nullable
    private ActionDescriptionPair mRightFlingAction;
    private final boolean mShowLinkButtons;
    private final RRThemeAttributes mTheme;

    private final class ActionDescriptionPair {
        public final RedditCommentAction action;
        public final int descriptionRes;

        private ActionDescriptionPair(RedditCommentAction action2, int descriptionRes2) {
            this.action = action2;
            this.descriptionRes = descriptionRes2;
        }
    }

    public interface CommentListener {
        void onCommentClicked(RedditCommentView redditCommentView);

        void onCommentLongClicked(RedditCommentView redditCommentView);
    }

    /* access modifiers changed from: protected */
    public void onSetItemFlingPosition(float position) {
        this.mIndentedContent.setTranslationX(position);
    }

    @Nullable
    private ActionDescriptionPair chooseFlingAction(CommentFlingAction pref) {
        if (!this.mComment.isComment()) {
            return null;
        }
        RedditParsedComment comment = this.mComment.asComment().getParsedComment();
        switch (pref) {
            case UPVOTE:
                if (this.mChangeDataManager.isUpvoted(comment)) {
                    return new ActionDescriptionPair(RedditCommentAction.UNVOTE, R.string.action_vote_remove);
                }
                return new ActionDescriptionPair(RedditCommentAction.UPVOTE, R.string.action_upvote);
            case DOWNVOTE:
                if (this.mChangeDataManager.isDownvoted(comment)) {
                    return new ActionDescriptionPair(RedditCommentAction.UNVOTE, R.string.action_vote_remove);
                }
                return new ActionDescriptionPair(RedditCommentAction.DOWNVOTE, R.string.action_downvote);
            case SAVE:
                if (this.mChangeDataManager.isSaved(comment)) {
                    return new ActionDescriptionPair(RedditCommentAction.UNSAVE, R.string.action_unsave);
                }
                return new ActionDescriptionPair(RedditCommentAction.SAVE, R.string.action_save);
            case REPLY:
                return new ActionDescriptionPair(RedditCommentAction.REPLY, R.string.action_reply);
            case USER_PROFILE:
                return new ActionDescriptionPair(RedditCommentAction.USER_PROFILE, R.string.action_user_profile);
            case COLLAPSE:
                if (this.mFragment == null) {
                    return null;
                }
                return new ActionDescriptionPair(RedditCommentAction.COLLAPSE, R.string.action_collapse);
            case ACTION_MENU:
                if (this.mFragment == null) {
                    return null;
                }
                return new ActionDescriptionPair(RedditCommentAction.ACTION_MENU, R.string.action_actionmenu_short);
            case PROPERTIES:
                return new ActionDescriptionPair(RedditCommentAction.PROPERTIES, R.string.action_properties);
            case BACK:
                return new ActionDescriptionPair(RedditCommentAction.BACK, R.string.action_back);
            case DISABLED:
                return null;
            default:
                return null;
        }
    }

    /* access modifiers changed from: protected */
    @NonNull
    public String getFlingLeftText() {
        Context context = getContext();
        this.mLeftFlingAction = chooseFlingAction(PrefsUtility.pref_behaviour_fling_comment_left(context, PreferenceManager.getDefaultSharedPreferences(context)));
        ActionDescriptionPair actionDescriptionPair = this.mLeftFlingAction;
        if (actionDescriptionPair == null) {
            return "Disabled";
        }
        return context.getString(actionDescriptionPair.descriptionRes);
    }

    /* access modifiers changed from: protected */
    @NonNull
    public String getFlingRightText() {
        Context context = getContext();
        this.mRightFlingAction = chooseFlingAction(PrefsUtility.pref_behaviour_fling_comment_right(context, PreferenceManager.getDefaultSharedPreferences(context)));
        ActionDescriptionPair actionDescriptionPair = this.mRightFlingAction;
        if (actionDescriptionPair == null) {
            return "Disabled";
        }
        return context.getString(actionDescriptionPair.descriptionRes);
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
        if (this.mLeftFlingAction != null && this.mComment.isComment()) {
            RedditAPICommentAction.onActionMenuItemSelected(this.mComment.asComment(), this, this.mActivity, this.mFragment, this.mLeftFlingAction.action, this.mChangeDataManager);
        }
    }

    /* access modifiers changed from: protected */
    public void onFlungRight() {
        if (this.mRightFlingAction != null && this.mComment.isComment()) {
            RedditAPICommentAction.onActionMenuItemSelected(this.mComment.asComment(), this, this.mActivity, this.mFragment, this.mRightFlingAction.action, this.mChangeDataManager);
        }
    }

    public RedditCommentView(AppCompatActivity context, RRThemeAttributes themeAttributes, CommentListener listener, CommentListingFragment fragment) {
        super(context);
        this.mActivity = context;
        this.mTheme = themeAttributes;
        this.mListener = listener;
        this.mFragment = fragment;
        this.mChangeDataManager = RedditChangeDataManager.getInstance(RedditAccountManager.getInstance(context).getDefaultAccount());
        View rootView = LayoutInflater.from(context).inflate(R.layout.reddit_comment, this, true);
        this.mIndentView = (IndentView) rootView.findViewById(R.id.view_reddit_comment_indentview);
        this.mHeader = (TextView) rootView.findViewById(R.id.view_reddit_comment_header);
        this.mBodyHolder = (FrameLayout) rootView.findViewById(R.id.view_reddit_comment_bodyholder);
        this.mIndentedContent = (LinearLayout) rootView.findViewById(R.id.view_reddit_comment_indented_content);
        this.mFontScale = PrefsUtility.appearance_fontscale_comments(context, PreferenceManager.getDefaultSharedPreferences(context));
        TextView textView = this.mHeader;
        textView.setTextSize(0, textView.getTextSize() * this.mFontScale);
        this.mShowLinkButtons = PrefsUtility.pref_appearance_linkbuttons(context, PreferenceManager.getDefaultSharedPreferences(context));
        setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                RedditCommentView.this.mListener.onCommentClicked(RedditCommentView.this);
            }
        });
        setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                RedditCommentView.this.mListener.onCommentLongClicked(RedditCommentView.this);
                return true;
            }
        });
    }

    public void onRedditDataChange(String thingIdAndType) {
        Handler handler = HANDLER;
        handler.sendMessage(Message.obtain(handler, 1, this));
    }

    /* access modifiers changed from: private */
    public void update() {
        reset(this.mActivity, this.mComment, true);
    }

    public void reset(AppCompatActivity activity, RedditCommentListItem comment) {
        reset(activity, comment, false);
    }

    public void reset(AppCompatActivity activity, RedditCommentListItem comment, boolean updateOnly) {
        if (!updateOnly) {
            if (comment.isComment()) {
                RedditCommentListItem redditCommentListItem = this.mComment;
                if (redditCommentListItem != comment) {
                    if (redditCommentListItem != null) {
                        this.mChangeDataManager.removeListener(redditCommentListItem.asComment(), this);
                    }
                    this.mChangeDataManager.addListener(comment.asComment(), this);
                }
                this.mComment = comment;
                resetSwipeState();
            } else {
                throw new RuntimeException("Not a comment");
            }
        }
        this.mIndentView.setIndentation(comment.getIndent());
        boolean hideLinkButtons = comment.asComment().getParsedComment().getRawComment().author.equalsIgnoreCase("autowikibot");
        this.mBodyHolder.removeAllViews();
        View commentBody = comment.asComment().getBody(activity, Integer.valueOf(this.mTheme.rrCommentBodyCol), Float.valueOf(this.mFontScale * 13.0f), this.mShowLinkButtons && !hideLinkButtons);
        this.mBodyHolder.addView(commentBody);
        commentBody.getLayoutParams().width = -1;
        ((MarginLayoutParams) commentBody.getLayoutParams()).topMargin = General.dpToPixels(activity, 1.0f);
        CharSequence headerText = this.mComment.asComment().getHeader(this.mTheme, this.mChangeDataManager, activity);
        if (this.mComment.isCollapsed(this.mChangeDataManager)) {
            setFlingingEnabled(false);
            TextView textView = this.mHeader;
            StringBuilder sb = new StringBuilder();
            sb.append("[ + ]  ");
            sb.append(headerText);
            textView.setText(sb.toString());
            this.mBodyHolder.setVisibility(8);
            return;
        }
        setFlingingEnabled(true);
        this.mHeader.setText(headerText);
        this.mBodyHolder.setVisibility(0);
    }

    public RedditCommentListItem getComment() {
        return this.mComment;
    }
}
