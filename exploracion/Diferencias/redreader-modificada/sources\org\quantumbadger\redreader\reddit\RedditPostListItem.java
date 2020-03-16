package org.quantumbadger.redreader.reddit;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.ViewGroup;
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter.Item;
import org.quantumbadger.redreader.fragments.PostListingFragment;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;
import org.quantumbadger.redreader.views.RedditPostView;

public class RedditPostListItem extends Item {
    private final AppCompatActivity mActivity;
    private final PostListingFragment mFragment;
    private final boolean mLeftHandedMode;
    private final RedditPreparedPost mPost;

    public RedditPostListItem(RedditPreparedPost post, PostListingFragment fragment, AppCompatActivity activity, boolean leftHandedMode) {
        this.mFragment = fragment;
        this.mActivity = activity;
        this.mPost = post;
        this.mLeftHandedMode = leftHandedMode;
    }

    public Class getViewType() {
        return RedditPostView.class;
    }

    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        AppCompatActivity appCompatActivity = this.mActivity;
        return new ViewHolder(new RedditPostView(appCompatActivity, this.mFragment, appCompatActivity, this.mLeftHandedMode)) {
        };
    }

    public void onBindViewHolder(ViewHolder viewHolder) {
        ((RedditPostView) viewHolder.itemView).reset(this.mPost);
    }

    public boolean isHidden() {
        return false;
    }
}
