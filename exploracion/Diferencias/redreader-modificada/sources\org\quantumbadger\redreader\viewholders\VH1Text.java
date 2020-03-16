package org.quantumbadger.redreader.viewholders;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.widget.TextView;
import org.quantumbadger.redreader.R;

public class VH1Text extends ViewHolder {
    public final TextView text;

    public VH1Text(View itemView) {
        super(itemView);
        this.text = (TextView) itemView.findViewById(R.id.recycler_item_text);
    }
}
