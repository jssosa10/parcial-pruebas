package org.quantumbadger.redreader.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.common.RRError;

public final class ErrorPropertiesDialog extends PropertiesDialog {
    public static ErrorPropertiesDialog newInstance(RRError error) {
        ErrorPropertiesDialog dialog = new ErrorPropertiesDialog();
        Bundle args = new Bundle();
        args.putString("title", error.title);
        args.putString("message", error.message);
        if (error.t != null) {
            StringBuilder sb = new StringBuilder(1024);
            BugReportActivity.appendException(sb, error.t, 10);
            args.putString("t", sb.toString());
        }
        if (error.httpStatus != null) {
            args.putString("httpStatus", error.httpStatus.toString());
        }
        if (error.url != null) {
            args.putString("url", error.url);
        }
        dialog.setArguments(args);
        return dialog;
    }

    /* access modifiers changed from: protected */
    public String getTitle(Context context) {
        return context.getString(R.string.props_error_title);
    }

    /* access modifiers changed from: protected */
    public void prepare(AppCompatActivity context, LinearLayout items) {
        items.addView(propView((Context) context, (int) R.string.props_title, (CharSequence) getArguments().getString("title"), true));
        items.addView(propView((Context) context, "Message", (CharSequence) getArguments().getString("message"), false));
        if (getArguments().containsKey("httpStatus")) {
            items.addView(propView((Context) context, "HTTP status", (CharSequence) getArguments().getString("httpStatus"), false));
        }
        if (getArguments().containsKey("url")) {
            items.addView(propView((Context) context, "URL", (CharSequence) getArguments().getString("url"), false));
        }
        if (getArguments().containsKey("t")) {
            items.addView(propView((Context) context, "Exception", (CharSequence) getArguments().getString("t"), false));
        }
    }
}
