package org.quantumbadger.redreader.views.liststatus;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils.TruncateAt;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.quantumbadger.redreader.R;

public final class LoadingView extends StatusListItemView {
    private static final int LOADING_DONE = -2;
    private static final int LOADING_INDETERMINATE = -1;
    private final Handler loadingHandler;
    /* access modifiers changed from: private */
    public final TextView textView;

    public void setIndeterminate(int textRes) {
        sendMessage(getContext().getString(textRes), -1);
    }

    public void setProgress(int textRes, float fraction) {
        sendMessage(getContext().getString(textRes), Math.round(100.0f * fraction));
    }

    public void setDone(int textRes) {
        sendMessage(getContext().getString(textRes), -2);
    }

    private void sendMessage(String text, int what) {
        Message msg = Message.obtain();
        msg.obj = text;
        msg.what = what;
        this.loadingHandler.sendMessage(msg);
    }

    public LoadingView(Context context) {
        this(context, (int) R.string.download_waiting, true, true);
    }

    public LoadingView(Context context, int initialTextRes, boolean progressBarEnabled, boolean indeterminate) {
        this(context, context.getString(initialTextRes), progressBarEnabled, indeterminate);
    }

    public LoadingView(Context context, String initialText, boolean progressBarEnabled, boolean indeterminate) {
        super(context);
        this.loadingHandler = new Handler(Looper.getMainLooper()) {
            public void handleMessage(Message msg) {
                if (LoadingView.this.textView != null) {
                    LoadingView.this.textView.setText(((String) msg.obj).toUpperCase());
                }
                if (msg.what != -1 && msg.what == -2) {
                    LoadingView.this.hideNoAnim();
                }
            }
        };
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(1);
        this.textView = new TextView(context);
        this.textView.setText(initialText.toUpperCase());
        this.textView.setTextSize(13.0f);
        this.textView.setPadding((int) (this.dpScale * 15.0f), (int) (this.dpScale * 10.0f), (int) (this.dpScale * 10.0f), (int) (this.dpScale * 10.0f));
        this.textView.setSingleLine(true);
        this.textView.setEllipsize(TruncateAt.END);
        layout.addView(this.textView);
        setContents(layout);
    }
}
