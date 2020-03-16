package org.quantumbadger.redreader.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import org.quantumbadger.redreader.R;

public class ScrollbarRecyclerViewManager {
    private final View mOuter;
    /* access modifiers changed from: private */
    public final RecyclerView mRecyclerView;
    /* access modifiers changed from: private */
    public boolean mScrollUnnecessary = false;
    /* access modifiers changed from: private */
    public final View mScrollbar;
    /* access modifiers changed from: private */
    public final FrameLayout mScrollbarFrame;
    private final SwipeRefreshLayout mSwipeRefreshLayout;

    public ScrollbarRecyclerViewManager(Context context, ViewGroup root, boolean attachToRoot) {
        this.mOuter = LayoutInflater.from(context).inflate(R.layout.scrollbar_recyclerview, root, attachToRoot);
        this.mSwipeRefreshLayout = (SwipeRefreshLayout) this.mOuter.findViewById(R.id.scrollbar_recyclerview_refreshlayout);
        this.mRecyclerView = (RecyclerView) this.mOuter.findViewById(R.id.scrollbar_recyclerview_recyclerview);
        this.mScrollbar = this.mOuter.findViewById(R.id.scrollbar_recyclerview_scrollbar);
        this.mScrollbarFrame = (FrameLayout) this.mOuter.findViewById(R.id.scrollbar_recyclerview_scrollbarframe);
        this.mSwipeRefreshLayout.setEnabled(false);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        this.mRecyclerView.setLayoutManager(linearLayoutManager);
        this.mRecyclerView.setHasFixedSize(true);
        linearLayoutManager.setSmoothScrollbarEnabled(false);
        this.mRecyclerView.addOnScrollListener(new OnScrollListener() {
            private void updateScroll() {
                int firstVisible = linearLayoutManager.findFirstVisibleItemPosition();
                boolean scrollUnnecessary = true;
                int itemsVisible = (linearLayoutManager.findLastVisibleItemPosition() - firstVisible) + 1;
                int totalCount = linearLayoutManager.getItemCount();
                if (itemsVisible != totalCount) {
                    scrollUnnecessary = false;
                }
                if (scrollUnnecessary != ScrollbarRecyclerViewManager.this.mScrollUnnecessary) {
                    ScrollbarRecyclerViewManager.this.mScrollbar.setVisibility(scrollUnnecessary ? 4 : 0);
                }
                ScrollbarRecyclerViewManager.this.mScrollUnnecessary = scrollUnnecessary;
                if (!scrollUnnecessary) {
                    int recyclerViewHeight = ScrollbarRecyclerViewManager.this.mRecyclerView.getMeasuredHeight();
                    int scrollBarHeight = ScrollbarRecyclerViewManager.this.mScrollbar.getMeasuredHeight();
                    double d = (double) firstVisible;
                    double d2 = (double) (totalCount - itemsVisible);
                    Double.isNaN(d);
                    Double.isNaN(d2);
                    double d3 = d / d2;
                    double d4 = (double) (recyclerViewHeight - scrollBarHeight);
                    Double.isNaN(d4);
                    ScrollbarRecyclerViewManager.this.mScrollbarFrame.setPadding(0, (int) Math.round(d3 * d4), 0, 0);
                }
            }

            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateScroll();
            }

            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                switch (newState) {
                    case 0:
                        ScrollbarRecyclerViewManager.this.hideScrollbar();
                        break;
                    case 1:
                        ScrollbarRecyclerViewManager.this.showScrollbar();
                        break;
                }
                updateScroll();
            }
        });
    }

    public void enablePullToRefresh(@NonNull OnRefreshListener listener) {
        this.mSwipeRefreshLayout.setOnRefreshListener(listener);
        this.mSwipeRefreshLayout.setEnabled(true);
    }

    /* access modifiers changed from: private */
    public void showScrollbar() {
        this.mScrollbar.animate().cancel();
        this.mScrollbar.setAlpha(1.0f);
    }

    /* access modifiers changed from: private */
    public void hideScrollbar() {
        this.mScrollbar.animate().alpha(0.0f).setStartDelay(500).setDuration(500).start();
    }

    public View getOuterView() {
        return this.mOuter;
    }

    public RecyclerView getRecyclerView() {
        return this.mRecyclerView;
    }
}
