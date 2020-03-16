package org.quantumbadger.redreader.views.liststatus;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.fragments.ErrorPropertiesDialog;

public final class ErrorView extends StatusListItemView {
    public ErrorView(final AppCompatActivity activity, final RRError error) {
        super(activity);
        TextView textView = new TextView(activity);
        textView.setText(error.title);
        textView.setTextColor(-1);
        textView.setTextSize(15.0f);
        textView.setPadding((int) (this.dpScale * 15.0f), (int) (this.dpScale * 10.0f), (int) (this.dpScale * 10.0f), (int) (this.dpScale * 4.0f));
        TextView messageView = new TextView(activity);
        messageView.setText(error.message);
        messageView.setTextColor(-1);
        messageView.setTextSize(12.0f);
        messageView.setPadding((int) (this.dpScale * 15.0f), 0, (int) (this.dpScale * 10.0f), (int) (this.dpScale * 10.0f));
        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(1);
        layout.addView(textView);
        layout.addView(messageView);
        setContents(layout);
        setBackgroundColor(Color.rgb(204, 0, 0));
        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ErrorPropertiesDialog.newInstance(error).show(activity.getSupportFragmentManager(), (String) null);
            }
        });
    }
}
