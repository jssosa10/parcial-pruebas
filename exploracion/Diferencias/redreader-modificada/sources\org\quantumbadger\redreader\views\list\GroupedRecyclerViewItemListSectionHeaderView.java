package org.quantumbadger.redreader.views.list;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter.Item;

public class GroupedRecyclerViewItemListSectionHeaderView extends Item {
    @NonNull
    private final CharSequence mText;

    public GroupedRecyclerViewItemListSectionHeaderView(@NonNull CharSequence text) {
        this.mText = text;
    }

    public Class getViewType() {
        return GroupedRecyclerViewItemListSectionHeaderView.class;
    }

    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_sectionheader, viewGroup, false)) {
        };
    }

    public void onBindViewHolder(ViewHolder viewHolder) {
        ((TextView) viewHolder.itemView).setText(this.mText);
    }

    public boolean isHidden() {
        return false;
    }
}
