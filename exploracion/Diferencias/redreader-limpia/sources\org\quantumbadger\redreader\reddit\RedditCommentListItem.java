package org.quantumbadger.redreader.reddit;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter.Item;
import org.quantumbadger.redreader.common.RRThemeAttributes;
import org.quantumbadger.redreader.fragments.CommentListingFragment;
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager;
import org.quantumbadger.redreader.reddit.prepared.RedditRenderableComment;
import org.quantumbadger.redreader.reddit.things.RedditMoreComments;
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;
import org.quantumbadger.redreader.views.LoadMoreCommentsView;
import org.quantumbadger.redreader.views.RedditCommentView;

public class RedditCommentListItem extends Item {
    private final AppCompatActivity mActivity;
    private final RedditChangeDataManager mChangeDataManager;
    private final RedditRenderableComment mComment;
    private final RedditURL mCommentListingUrl;
    private final CommentListingFragment mFragment;
    private final int mIndent;
    private final RedditMoreComments mMoreComments;
    private final RedditCommentListItem mParent;
    private final Type mType;

    public enum Type {
        COMMENT,
        LOAD_MORE
    }

    public RedditCommentListItem(RedditRenderableComment comment, RedditCommentListItem parent, CommentListingFragment fragment, AppCompatActivity activity, RedditURL commentListingUrl) {
        this.mParent = parent;
        this.mFragment = fragment;
        this.mActivity = activity;
        this.mCommentListingUrl = commentListingUrl;
        this.mType = Type.COMMENT;
        this.mComment = comment;
        this.mMoreComments = null;
        if (parent == null) {
            this.mIndent = 0;
        } else {
            this.mIndent = parent.getIndent() + 1;
        }
        this.mChangeDataManager = RedditChangeDataManager.getInstance(RedditAccountManager.getInstance(activity).getDefaultAccount());
    }

    public RedditCommentListItem(RedditMoreComments moreComments, RedditCommentListItem parent, CommentListingFragment fragment, AppCompatActivity activity, RedditURL commentListingUrl) {
        this.mParent = parent;
        this.mFragment = fragment;
        this.mActivity = activity;
        this.mCommentListingUrl = commentListingUrl;
        this.mType = Type.LOAD_MORE;
        this.mComment = null;
        this.mMoreComments = moreComments;
        if (parent == null) {
            this.mIndent = 0;
        } else {
            this.mIndent = parent.getIndent() + 1;
        }
        this.mChangeDataManager = RedditChangeDataManager.getInstance(RedditAccountManager.getInstance(activity).getDefaultAccount());
    }

    public boolean isComment() {
        return this.mType == Type.COMMENT;
    }

    public boolean isLoadMore() {
        return this.mType == Type.LOAD_MORE;
    }

    public RedditRenderableComment asComment() {
        if (isComment()) {
            return this.mComment;
        }
        throw new RuntimeException("Called asComment() on non-comment item");
    }

    public RedditMoreComments asLoadMore() {
        if (isLoadMore()) {
            return this.mMoreComments;
        }
        throw new RuntimeException("Called asLoadMore() on non-load-more item");
    }

    public int getIndent() {
        return this.mIndent;
    }

    public RedditCommentListItem getParent() {
        return this.mParent;
    }

    public boolean isCollapsed(RedditChangeDataManager changeDataManager) {
        if (!isComment()) {
            return false;
        }
        return this.mComment.isCollapsed(changeDataManager);
    }

    public boolean isHidden(RedditChangeDataManager changeDataManager) {
        RedditCommentListItem redditCommentListItem = this.mParent;
        boolean z = false;
        if (redditCommentListItem == null) {
            return false;
        }
        if (redditCommentListItem.isCollapsed(changeDataManager) || this.mParent.isHidden(changeDataManager)) {
            z = true;
        }
        return z;
    }

    public Class getViewType() {
        if (isComment()) {
            return RedditCommentView.class;
        }
        if (isLoadMore()) {
            return LoadMoreCommentsView.class;
        }
        throw new RuntimeException("Unknown item type");
    }

    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        View view;
        Context context = viewGroup.getContext();
        if (isComment()) {
            AppCompatActivity appCompatActivity = this.mActivity;
            RRThemeAttributes rRThemeAttributes = new RRThemeAttributes(context);
            CommentListingFragment commentListingFragment = this.mFragment;
            view = new RedditCommentView(appCompatActivity, rRThemeAttributes, commentListingFragment, commentListingFragment);
        } else if (isLoadMore()) {
            view = new LoadMoreCommentsView(context, this.mCommentListingUrl);
        } else {
            throw new RuntimeException("Unknown item type");
        }
        return new ViewHolder(view) {
        };
    }

    public void onBindViewHolder(ViewHolder viewHolder) {
        if (isComment()) {
            ((RedditCommentView) viewHolder.itemView).reset(this.mActivity, this);
        } else if (isLoadMore()) {
            ((LoadMoreCommentsView) viewHolder.itemView).reset(this);
        } else {
            throw new RuntimeException("Unknown item type");
        }
    }

    public boolean isHidden() {
        return isHidden(this.mChangeDataManager);
    }
}
