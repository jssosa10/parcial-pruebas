package org.quantumbadger.redreader.views.list;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.quantumbadger.redreader.R;

public class ListItemView extends FrameLayout {
    private final View divider;
    private final ImageView imageView;
    private final TextView textView;

    public ListItemView(Context context) {
        super(context);
        LinearLayout ll = (LinearLayout) inflate(context, R.layout.list_item, null);
        this.divider = ll.findViewById(R.id.list_item_divider);
        this.textView = (TextView) ll.findViewById(R.id.list_item_text);
        this.imageView = (ImageView) ll.findViewById(R.id.list_item_icon);
        setDescendantFocusability(393216);
        addView(ll);
    }

    public void reset(@Nullable Drawable icon, @NonNull CharSequence text, boolean hideDivider) {
        if (hideDivider) {
            this.divider.setVisibility(8);
        } else {
            this.divider.setVisibility(0);
        }
        this.textView.setText(text);
        if (icon != null) {
            this.imageView.setImageDrawable(icon);
            this.imageView.setVisibility(0);
            return;
        }
        this.imageView.setImageBitmap(null);
        this.imageView.setVisibility(8);
    }
}
