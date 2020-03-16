package org.quantumbadger.redreader.fragments;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.General;

public abstract class PropertiesDialog extends AppCompatDialogFragment {
    private volatile boolean alreadyCreated = false;
    protected int rrCommentBodyCol;
    protected int rrListDividerCol;
    protected int rrListHeaderTextCol;

    /* access modifiers changed from: protected */
    public abstract String getTitle(Context context);

    /* access modifiers changed from: protected */
    public abstract void prepare(AppCompatActivity appCompatActivity, LinearLayout linearLayout);

    @NonNull
    public final Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        if (this.alreadyCreated) {
            return getDialog();
        }
        this.alreadyCreated = true;
        AppCompatActivity context = (AppCompatActivity) getActivity();
        TypedArray attr = context.obtainStyledAttributes(new int[]{R.attr.rrListHeaderTextCol, R.attr.rrListDividerCol, R.attr.rrMainTextCol});
        this.rrListHeaderTextCol = attr.getColor(0, 0);
        this.rrListDividerCol = attr.getColor(1, 0);
        this.rrCommentBodyCol = attr.getColor(2, 0);
        attr.recycle();
        Builder builder = new Builder(context);
        LinearLayout items = new LinearLayout(context);
        items.setOrientation(1);
        prepare(context, items);
        builder.setTitle(getTitle(context));
        ScrollView sv = new ScrollView(context);
        sv.addView(items);
        builder.setView(sv);
        builder.setNeutralButton(R.string.dialog_close, null);
        return builder.create();
    }

    /* access modifiers changed from: protected */
    public final LinearLayout propView(Context context, int titleRes, int textRes, boolean firstInList) {
        return propView(context, context.getString(titleRes), (CharSequence) getString(textRes), firstInList);
    }

    /* access modifiers changed from: protected */
    public final LinearLayout propView(Context context, int titleRes, CharSequence text, boolean firstInList) {
        return propView(context, context.getString(titleRes), text, firstInList);
    }

    /* access modifiers changed from: protected */
    public final LinearLayout propView(Context context, String title, CharSequence text, boolean firstInList) {
        int paddingPixels = General.dpToPixels(context, 12.0f);
        LinearLayout prop = new LinearLayout(context);
        prop.setOrientation(1);
        if (!firstInList) {
            View divider = new View(context);
            divider.setMinimumHeight(General.dpToPixels(context, 1.0f));
            divider.setBackgroundColor(this.rrListDividerCol);
            prop.addView(divider);
        }
        TextView titleView = new TextView(context);
        titleView.setText(title.toUpperCase());
        titleView.setTextColor(this.rrListHeaderTextCol);
        titleView.setTextSize(12.0f);
        titleView.setPadding(paddingPixels, paddingPixels, paddingPixels, 0);
        prop.addView(titleView);
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(this.rrCommentBodyCol);
        textView.setTextSize(15.0f);
        textView.setPadding(paddingPixels, 0, paddingPixels, paddingPixels);
        textView.setTextIsSelectable(true);
        prop.addView(textView);
        return prop;
    }
}
