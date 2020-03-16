package org.quantumbadger.redreader.views.bezelmenu;

import android.content.Context;
import android.graphics.Color;
import android.os.Build.VERSION;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import com.google.android.exoplayer2.extractor.ts.PsExtractor;
import org.quantumbadger.redreader.common.General;

public class VerticalToolbar extends FrameLayout {
    private final LinearLayout buttons;

    public VerticalToolbar(Context context) {
        super(context);
        setBackgroundColor(Color.argb(PsExtractor.AUDIO_STREAM, 0, 0, 0));
        if (VERSION.SDK_INT >= 21) {
            setElevation((float) General.dpToPixels(context, 10.0f));
        }
        this.buttons = new LinearLayout(context);
        this.buttons.setOrientation(1);
        ScrollView sv = new ScrollView(context);
        sv.addView(this.buttons);
        addView(sv);
    }

    public void addItem(View v) {
        this.buttons.addView(v);
    }
}
