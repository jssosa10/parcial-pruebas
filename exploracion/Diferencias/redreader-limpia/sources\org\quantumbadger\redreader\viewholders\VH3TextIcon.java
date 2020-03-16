package org.quantumbadger.redreader.viewholders;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import org.quantumbadger.redreader.R;

public class VH3TextIcon extends ViewHolder {
    public long bindingId = 0;
    public final ImageView icon;
    public final TextView text;
    public final TextView text2;
    public final TextView text3;

    public VH3TextIcon(View itemView) {
        super(itemView);
        this.text = (TextView) itemView.findViewById(R.id.recycler_item_text);
        this.text2 = (TextView) itemView.findViewById(R.id.recycler_item_2_text);
        this.text3 = (TextView) itemView.findViewById(R.id.recycler_item_3_text);
        this.icon = (ImageView) itemView.findViewById(R.id.recycler_item_icon);
    }
}
