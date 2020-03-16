package org.quantumbadger.redreader.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.General;

public final class PostListingHeader extends LinearLayout {
    public PostListingHeader(Context context, String titleText, String subtitleText) {
        super(context);
        TypedArray appearance = context.obtainStyledAttributes(new int[]{R.attr.rrPostListHeaderBackgroundCol});
        setBackgroundColor(appearance.getColor(0, General.COLOR_INVALID));
        appearance.recycle();
        float dpScale = context.getResources().getDisplayMetrics().density;
        setOrientation(1);
        int sidesPadding = (int) (15.0f * dpScale);
        int topPadding = (int) (10.0f * dpScale);
        setPadding(sidesPadding, topPadding, sidesPadding, topPadding);
        Typeface tf = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf");
        TextView title = new TextView(context);
        title.setText(titleText);
        title.setTextSize(22.0f);
        title.setTypeface(tf);
        title.setTextColor(-1);
        addView(title);
        TextView subtitle = new TextView(context);
        subtitle.setTextSize(14.0f);
        subtitle.setText(subtitleText);
        subtitle.setTextColor(Color.rgb(200, 200, 200));
        addView(subtitle);
    }
}
