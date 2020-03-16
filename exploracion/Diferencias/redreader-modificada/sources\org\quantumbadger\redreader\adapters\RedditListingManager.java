package org.quantumbadger.redreader.adapters;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;
import java.util.Collection;
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter.Item;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.views.LoadingSpinnerView;
import org.quantumbadger.redreader.views.RedditPostHeaderView;
import org.quantumbadger.redreader.views.liststatus.ErrorView;

public abstract class RedditListingManager {
    private static final int GROUP_FOOTER_ERRORS = 6;
    private static final int GROUP_HEADER = 0;
    private static final int GROUP_ITEMS = 3;
    private static final int GROUP_LOADING = 5;
    private static final int GROUP_LOAD_MORE_BUTTON = 4;
    private static final int GROUP_NOTIFICATIONS = 1;
    private static final int GROUP_POST_SELFTEXT = 2;
    private final GroupedRecyclerViewAdapter mAdapter = new GroupedRecyclerViewAdapter(7);
    private LinearLayoutManager mLayoutManager;
    private final GroupedRecyclerViewItemFrameLayout mLoadingItem;
    private boolean mWorkaroundDone = false;

    public RedditListingManager(Context context) {
        General.checkThisIsUIThread();
        LoadingSpinnerView loadingSpinnerView = new LoadingSpinnerView(context);
        int paddingPx = General.dpToPixels(context, 30.0f);
        loadingSpinnerView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        this.mLoadingItem = new GroupedRecyclerViewItemFrameLayout(loadingSpinnerView);
        this.mAdapter.appendToGroup(5, (Item) this.mLoadingItem);
    }

    public void setLayoutManager(LinearLayoutManager layoutManager) {
        General.checkThisIsUIThread();
        this.mLayoutManager = layoutManager;
    }

    private void doWorkaround() {
        if (!this.mWorkaroundDone) {
            LinearLayoutManager linearLayoutManager = this.mLayoutManager;
            if (linearLayoutManager != null) {
                linearLayoutManager.scrollToPositionWithOffset(0, 0);
                this.mWorkaroundDone = true;
            }
        }
    }

    public void addFooterError(ErrorView view) {
        General.checkThisIsUIThread();
        this.mAdapter.appendToGroup(6, (Item) new GroupedRecyclerViewItemFrameLayout(view));
    }

    public void addPostHeader(RedditPostHeaderView view) {
        General.checkThisIsUIThread();
        this.mAdapter.appendToGroup(0, (Item) new GroupedRecyclerViewItemFrameLayout(view));
        doWorkaround();
    }

    public void addPostListingHeader(View view) {
        General.checkThisIsUIThread();
        this.mAdapter.appendToGroup(0, (Item) new GroupedRecyclerViewItemFrameLayout(view));
        doWorkaround();
    }

    public void addPostSelfText(View view) {
        General.checkThisIsUIThread();
        this.mAdapter.appendToGroup(2, (Item) new GroupedRecyclerViewItemFrameLayout(view));
        doWorkaround();
    }

    public void addNotification(View view) {
        General.checkThisIsUIThread();
        this.mAdapter.appendToGroup(1, (Item) new GroupedRecyclerViewItemFrameLayout(view));
        doWorkaround();
    }

    public void addItems(Collection<Item> items) {
        General.checkThisIsUIThread();
        this.mAdapter.appendToGroup(3, items);
        doWorkaround();
    }

    public void addViewToItems(View view) {
        General.checkThisIsUIThread();
        this.mAdapter.appendToGroup(3, (Item) new GroupedRecyclerViewItemFrameLayout(view));
        doWorkaround();
    }

    public void addLoadMoreButton(View view) {
        General.checkThisIsUIThread();
        this.mAdapter.appendToGroup(4, (Item) new GroupedRecyclerViewItemFrameLayout(view));
        doWorkaround();
    }

    public void removeLoadMoreButton() {
        General.checkThisIsUIThread();
        this.mAdapter.removeAllFromGroup(4);
    }

    public void setLoadingVisible(boolean visible) {
        General.checkThisIsUIThread();
        this.mLoadingItem.setHidden(!visible);
        this.mAdapter.updateHiddenStatus();
    }

    public GroupedRecyclerViewAdapter getAdapter() {
        General.checkThisIsUIThread();
        return this.mAdapter;
    }

    public void updateHiddenStatus() {
        General.checkThisIsUIThread();
        this.mAdapter.updateHiddenStatus();
    }

    public Item getItemAtPosition(int position) {
        return this.mAdapter.getItemAtPosition(position);
    }
}
