package org.quantumbadger.redreader.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParser;

public class MarkdownPreviewDialog extends PropertiesDialog {
    public static MarkdownPreviewDialog newInstance(String markdown) {
        MarkdownPreviewDialog dialog = new MarkdownPreviewDialog();
        Bundle args = new Bundle(1);
        args.putString("markdown", markdown);
        dialog.setArguments(args);
        return dialog;
    }

    /* access modifiers changed from: protected */
    public String getTitle(Context context) {
        return context.getString(R.string.comment_reply_preview);
    }

    /* access modifiers changed from: protected */
    public void prepare(AppCompatActivity context, LinearLayout items) {
        ViewGroup parsed = MarkdownParser.parse(getArguments().getString("markdown").toCharArray()).buildView(context, null, Float.valueOf(14.0f), false);
        int paddingPx = General.dpToPixels(context, 10.0f);
        parsed.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        items.addView(parsed);
    }
}
