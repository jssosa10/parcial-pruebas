package org.quantumbadger.redreader.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;

class IndentView extends View {
    private final float mHalfALine;
    private int mIndent;
    private final Paint mPaint;
    private final int mPixelsPerIndent;
    private final boolean mPrefDrawLines;

    public IndentView(Context context) {
        this(context, null);
    }

    public IndentView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IndentView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mPaint = new Paint();
        this.mPixelsPerIndent = General.dpToPixels(context, 10.0f);
        int mPixelsPerLine = General.dpToPixels(context, 2.0f);
        this.mHalfALine = (float) (mPixelsPerLine / 2);
        TypedArray attr = context.obtainStyledAttributes(new int[]{R.attr.rrIndentBackgroundCol, R.attr.rrIndentLineCol});
        int rrIndentBackgroundCol = attr.getColor(0, General.COLOR_INVALID);
        int rrIndentLineCol = attr.getColor(1, General.COLOR_INVALID);
        attr.recycle();
        setBackgroundColor(rrIndentBackgroundCol);
        this.mPaint.setColor(rrIndentLineCol);
        this.mPaint.setStrokeWidth((float) mPixelsPerLine);
        this.mPrefDrawLines = PrefsUtility.pref_appearance_indentlines(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int height = getMeasuredHeight();
        if (this.mPrefDrawLines) {
            float[] lines = new float[(this.mIndent * 4)];
            int i = 0;
            int l = 0;
            while (i < this.mIndent) {
                i++;
                float x = ((float) (this.mPixelsPerIndent * i)) - this.mHalfALine;
                lines[l] = x;
                int l2 = l + 1;
                lines[l2] = 0.0f;
                int l3 = l2 + 1;
                lines[l3] = x;
                int l4 = l3 + 1;
                lines[l4] = (float) height;
                l = l4 + 1;
            }
            canvas.drawLines(lines, this.mPaint);
            return;
        }
        float rightLine = ((float) getWidth()) - this.mHalfALine;
        canvas.drawLine(rightLine, 0.0f, rightLine, (float) getHeight(), this.mPaint);
    }

    public void setIndentation(int indent) {
        getLayoutParams().width = this.mPixelsPerIndent * indent;
        this.mIndent = indent;
        invalidate();
        requestLayout();
    }
}
