package org.quantumbadger.redreader.views.list;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter.Item;

public class GroupedRecyclerViewItemListItemView extends Item {
    @Nullable
    private final OnClickListener mClickListener;
    private final boolean mHideDivider;
    @Nullable
    private final Drawable mIcon;
    @Nullable
    private final OnLongClickListener mLongClickListener;
    @NonNull
    private final CharSequence mText;

    public GroupedRecyclerViewItemListItemView(@Nullable Drawable icon, @NonNull CharSequence text, boolean hideDivider, @Nullable OnClickListener clickListener, @Nullable OnLongClickListener longClickListener) {
        this.mIcon = icon;
        this.mText = text;
        this.mHideDivider = hideDivider;
        this.mClickListener = clickListener;
        this.mLongClickListener = longClickListener;
    }

    public Class getViewType() {
        return ListItemView.class;
    }

    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        return new ViewHolder(new ListItemView(viewGroup.getContext())) {
        };
    }

    public void onBindViewHolder(ViewHolder viewHolder) {
        ListItemView view = (ListItemView) viewHolder.itemView;
        view.reset(this.mIcon, this.mText, this.mHideDivider);
        view.setOnClickListener(this.mClickListener);
        view.setOnLongClickListener(this.mLongClickListener);
        if (this.mClickListener == null) {
            view.setClickable(false);
        }
        if (this.mLongClickListener == null) {
            view.setLongClickable(false);
        }
    }

    public boolean isHidden() {
        return false;
    }
}
