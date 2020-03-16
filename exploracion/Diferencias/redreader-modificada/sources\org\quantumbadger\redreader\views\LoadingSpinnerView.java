package org.quantumbadger.redreader.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import com.github.lzyzsd.circleprogress.DonutProgress;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.General;

public class LoadingSpinnerView extends RelativeLayout {
    final DonutProgress mProgressView;

    public LoadingSpinnerView(Context context) {
        super(context);
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{R.attr.rrLoadingRingForegroundCol, R.attr.rrLoadingRingBackgroundCol});
        int foreground = typedArray.getColor(0, General.COLOR_INVALID);
        int background = typedArray.getColor(1, -16711936);
        typedArray.recycle();
        this.mProgressView = new DonutProgress(context);
        this.mProgressView.setAspectIndicatorDisplay(false);
        this.mProgressView.setIndeterminate(true);
        this.mProgressView.setFinishedStrokeColor(foreground);
        this.mProgressView.setUnfinishedStrokeColor(background);
        int progressStrokeWidthPx = General.dpToPixels(context, 10.0f);
        this.mProgressView.setUnfinishedStrokeWidth((float) progressStrokeWidthPx);
        this.mProgressView.setFinishedStrokeWidth((float) progressStrokeWidthPx);
        this.mProgressView.setStartingDegree(-90);
        this.mProgressView.initPainters();
        addView(this.mProgressView);
        int progressDimensionsPx = General.dpToPixels(context, 100.0f);
        this.mProgressView.getLayoutParams().width = progressDimensionsPx;
        this.mProgressView.getLayoutParams().height = progressDimensionsPx;
        ((LayoutParams) this.mProgressView.getLayoutParams()).addRule(13);
    }
}
