package org.quantumbadger.redreader.views;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.internal.view.SupportMenu;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import com.github.lzyzsd.circleprogress.DonutProgress;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.General;

public class HorizontalSwipeProgressOverlay extends RelativeLayout {
    private int mCurrentIconResource = 0;
    private final ImageView mIcon;
    private final DonutProgress mProgress;

    public HorizontalSwipeProgressOverlay(Context context) {
        super(context);
        View background = new View(context);
        int backgroundDimensionsPx = General.dpToPixels(context, 200.0f);
        background.setBackgroundColor(Color.argb(127, 0, 0, 0));
        addView(background);
        background.getLayoutParams().width = backgroundDimensionsPx;
        background.getLayoutParams().height = backgroundDimensionsPx;
        ((LayoutParams) background.getLayoutParams()).addRule(13);
        this.mIcon = new ImageView(context);
        this.mIcon.setImageResource(R.drawable.ic_action_forward_dark);
        this.mCurrentIconResource = R.drawable.ic_action_forward_dark;
        addView(this.mIcon);
        ((LayoutParams) this.mIcon.getLayoutParams()).addRule(13);
        this.mProgress = new DonutProgress(context);
        addView(this.mProgress);
        ((LayoutParams) this.mProgress.getLayoutParams()).addRule(13);
        int progressDimensionsPx = General.dpToPixels(context, 150.0f);
        this.mProgress.getLayoutParams().width = progressDimensionsPx;
        this.mProgress.getLayoutParams().height = progressDimensionsPx;
        this.mProgress.setAspectIndicatorDisplay(false);
        this.mProgress.setFinishedStrokeColor(SupportMenu.CATEGORY_MASK);
        this.mProgress.setUnfinishedStrokeColor(Color.argb(127, 0, 0, 0));
        int progressStrokeWidthPx = General.dpToPixels(context, 15.0f);
        this.mProgress.setUnfinishedStrokeWidth((float) progressStrokeWidthPx);
        this.mProgress.setFinishedStrokeWidth((float) progressStrokeWidthPx);
        this.mProgress.setStartingDegree(-90);
        this.mProgress.initPainters();
        setVisibility(8);
    }

    private void setIconResource(int resource) {
        if (resource != this.mCurrentIconResource) {
            this.mCurrentIconResource = resource;
            this.mIcon.setImageResource(resource);
        }
    }

    public void onSwipeUpdate(float px, float maxPx) {
        this.mProgress.setProgress(-(px / maxPx));
        if (Math.abs(px) > 20.0f) {
            setVisibility(0);
        }
        if (px < 0.0f) {
            setIconResource(R.drawable.ic_action_forward_dark);
        } else {
            setIconResource(R.drawable.ic_action_back_dark);
        }
    }

    public void onSwipeEnd() {
        setVisibility(8);
    }
}
