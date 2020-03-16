package org.quantumbadger.redreader.views.bezelmenu;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

public class SideToolbarOverlay extends FrameLayout {
    private View contents;
    private SideToolbarPosition shownPosition = null;

    public enum SideToolbarPosition {
        LEFT,
        RIGHT
    }

    public SideToolbarOverlay(Context context) {
        super(context);
    }

    public void setContents(View contents2) {
        this.contents = contents2;
        SideToolbarPosition sideToolbarPosition = this.shownPosition;
        if (sideToolbarPosition != null) {
            show(sideToolbarPosition);
        }
    }

    public void show(SideToolbarPosition pos) {
        removeAllViews();
        addView(this.contents);
        ((LayoutParams) this.contents.getLayoutParams()).gravity = pos == SideToolbarPosition.LEFT ? 3 : 5;
        this.contents.getLayoutParams().width = -2;
        this.contents.getLayoutParams().height = -1;
        this.shownPosition = pos;
    }

    public void hide() {
        this.shownPosition = null;
        removeAllViews();
    }

    public boolean isShown() {
        return this.shownPosition != null;
    }
}
