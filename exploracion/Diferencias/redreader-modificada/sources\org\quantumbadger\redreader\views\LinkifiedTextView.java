package org.quantumbadger.redreader.views;

import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;
import org.quantumbadger.redreader.common.PrefsUtility;

public class LinkifiedTextView extends TextView {
    private final AppCompatActivity mActivity;

    public LinkifiedTextView(AppCompatActivity activity) {
        super(activity);
        this.mActivity = activity;
    }

    public AppCompatActivity getActivity() {
        return this.mActivity;
    }

    public boolean onTouchEvent(MotionEvent event) {
        CharSequence text = getText();
        if (!(text instanceof Spannable)) {
            return false;
        }
        AppCompatActivity appCompatActivity = this.mActivity;
        if (!PrefsUtility.pref_appearance_link_text_clickable(appCompatActivity, PreferenceManager.getDefaultSharedPreferences(appCompatActivity))) {
            return false;
        }
        Spannable buffer = (Spannable) text;
        int action = event.getAction();
        if (action == 1 || action == 0) {
            int x = (((int) event.getX()) - getTotalPaddingLeft()) + getScrollX();
            int y = (((int) event.getY()) - getTotalPaddingTop()) + getScrollY();
            Layout layout = getLayout();
            int off = layout.getOffsetForHorizontal(layout.getLineForVertical(y), (float) x);
            ClickableSpan[] links = (ClickableSpan[]) buffer.getSpans(off, off, ClickableSpan.class);
            if (links.length != 0) {
                if (action == 1) {
                    links[0].onClick(this);
                } else if (action == 0) {
                    Selection.setSelection(buffer, buffer.getSpanStart(links[0]), buffer.getSpanEnd(links[0]));
                }
                return true;
            }
            Selection.removeSelection(buffer);
        }
        return false;
    }
}
