package org.quantumbadger.redreader.views.liststatus;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;

public class StatusListItemView extends FrameLayout {
    private View contents = null;
    protected final float dpScale;

    public StatusListItemView(Context context) {
        super(context);
        this.dpScale = context.getResources().getDisplayMetrics().density;
    }

    public void setContents(View contents2) {
        View view = this.contents;
        if (view != null) {
            removeView(view);
        }
        this.contents = contents2;
        addView(contents2);
        LayoutParams layoutParams = contents2.getLayoutParams();
        layoutParams.width = -1;
        layoutParams.height = -1;
    }

    public void hideNoAnim() {
        setVisibility(8);
        removeAllViews();
        this.contents = null;
        requestLayout();
    }
}
