package org.quantumbadger.redreader.adapters;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter.Item;

final class GroupedRecyclerViewItemFrameLayout extends Item {
    private final View mChildView;
    private boolean mHidden;
    private FrameLayout mParent;

    GroupedRecyclerViewItemFrameLayout(View childView) {
        this.mChildView = childView;
    }

    public Class getViewType() {
        return getClass();
    }

    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        viewGroup.getLayoutParams().width = -1;
        return new ViewHolder(new FrameLayout(viewGroup.getContext())) {
        };
    }

    public void onBindViewHolder(ViewHolder viewHolder) {
        FrameLayout view = (FrameLayout) viewHolder.itemView;
        view.removeAllViews();
        if (this.mParent != null) {
            ViewParent parent = this.mChildView.getParent();
            FrameLayout frameLayout = this.mParent;
            if (parent == frameLayout) {
                frameLayout.removeAllViews();
            }
        }
        this.mParent = view;
        view.addView(this.mChildView);
        this.mChildView.getLayoutParams().width = -1;
    }

    public boolean isHidden() {
        return this.mHidden;
    }

    public void setHidden(boolean hidden) {
        this.mHidden = hidden;
    }
}
