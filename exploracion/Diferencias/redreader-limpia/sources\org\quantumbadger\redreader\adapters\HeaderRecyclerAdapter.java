package org.quantumbadger.redreader.adapters;

import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.ViewGroup;

public abstract class HeaderRecyclerAdapter<VH extends ViewHolder> extends Adapter<VH> {
    protected static final int HEADER_SIZE = 1;
    private static final int TYPE_CONTENT = 1;
    private static final int TYPE_HEADER = 0;

    /* access modifiers changed from: protected */
    public abstract int getContentItemCount();

    /* access modifiers changed from: protected */
    public abstract void onBindContentItemViewHolder(VH vh, int i);

    /* access modifiers changed from: protected */
    public abstract void onBindHeaderItemViewHolder(VH vh, int i);

    /* access modifiers changed from: protected */
    public abstract VH onCreateContentItemViewHolder(ViewGroup viewGroup);

    /* access modifiers changed from: protected */
    public abstract VH onCreateHeaderItemViewHolder(ViewGroup viewGroup);

    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case 0:
                return onCreateHeaderItemViewHolder(parent);
            case 1:
                return onCreateContentItemViewHolder(parent);
            default:
                throw new IllegalStateException();
        }
    }

    public void onBindViewHolder(VH holder, int position) {
        if (position == 0) {
            onBindHeaderItemViewHolder(holder, position);
        } else {
            onBindContentItemViewHolder(holder, position - 1);
        }
    }

    public int getItemCount() {
        return getContentItemCount() + 1;
    }

    public int getItemViewType(int position) {
        return position == 0 ? 0 : 1;
    }
}
