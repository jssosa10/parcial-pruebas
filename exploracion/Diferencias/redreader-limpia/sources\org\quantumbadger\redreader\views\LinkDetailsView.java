package org.quantumbadger.redreader.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint.Style;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.General;

public class LinkDetailsView extends FrameLayout {
    public LinkDetailsView(Context context, String title, String subtitle) {
        super(context);
        setClickable(true);
        final LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(0);
        addView(layout);
        int marginPx = General.dpToPixels(context, 10.0f);
        layout.setGravity(16);
        TypedArray appearance = context.obtainStyledAttributes(new int[]{R.attr.rrIconLink});
        ImageView icon = new ImageView(context);
        icon.setImageDrawable(appearance.getDrawable(0));
        appearance.recycle();
        layout.addView(icon);
        ((LayoutParams) icon.getLayoutParams()).setMargins(marginPx, marginPx, marginPx, marginPx);
        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(1);
        layout.addView(textLayout);
        ((LayoutParams) textLayout.getLayoutParams()).setMargins(0, marginPx, marginPx, marginPx);
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(15.0f);
        textLayout.addView(titleView);
        if (subtitle != null && !title.equals(subtitle)) {
            TextView subtitleView = new TextView(context);
            subtitleView.setText(subtitle);
            subtitleView.setTextSize(11.0f);
            textLayout.addView(subtitleView);
        }
        float borderPx = (float) General.dpToPixels(context, 2.0f);
        ShapeDrawable border = new ShapeDrawable(new RectShape());
        border.getPaint().setColor(Color.argb(128, 128, 128, 128));
        border.getPaint().setStrokeWidth(borderPx);
        border.getPaint().setStyle(Style.STROKE);
        setBackgroundDrawable(border);
        setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                int actionMasked = event.getActionMasked();
                if (actionMasked != 3) {
                    switch (actionMasked) {
                        case 0:
                            layout.setBackgroundColor(Color.argb(50, 128, 128, 128));
                            LinkDetailsView.this.invalidate();
                            break;
                        case 1:
                            break;
                    }
                }
                layout.setBackgroundColor(0);
                LinkDetailsView.this.invalidate();
                return false;
            }
        });
    }
}
