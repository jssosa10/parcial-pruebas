package org.quantumbadger.redreader.common;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import org.apache.commons.lang3.StringUtils;
import org.quantumbadger.redreader.R;

public class DialogUtils {

    public interface OnSearchListener {
        void onSearch(@Nullable String str);
    }

    public static void showSearchDialog(Context context, OnSearchListener listener) {
        showSearchDialog(context, R.string.action_search, listener);
    }

    public static void showSearchDialog(Context context, int titleRes, final OnSearchListener listener) {
        Builder alertBuilder = new Builder(context);
        final EditText editText = (EditText) LayoutInflater.from(context).inflate(R.layout.dialog_editbox, null);
        OnEditorActionListener onEnter = new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                DialogUtils.performSearch(editText, listener);
                return true;
            }
        };
        editText.setImeOptions(3);
        editText.setOnEditorActionListener(onEnter);
        alertBuilder.setView(editText);
        alertBuilder.setTitle(titleRes);
        alertBuilder.setPositiveButton(R.string.action_search, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                DialogUtils.performSearch(editText, listener);
            }
        });
        alertBuilder.setNegativeButton(R.string.dialog_cancel, null);
        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.getWindow().setSoftInputMode(4);
        alertDialog.show();
    }

    /* access modifiers changed from: private */
    public static void performSearch(EditText editText, OnSearchListener listener) {
        String query = General.asciiLowercase(editText.getText().toString()).trim();
        if (StringUtils.isEmpty(query)) {
            listener.onSearch(null);
        } else {
            listener.onSearch(query);
        }
    }
}
