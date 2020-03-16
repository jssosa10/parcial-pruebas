package org.quantumbadger.redreader.activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.exoplayer2.C;
import java.util.Iterator;
import java.util.LinkedList;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.Constants;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.RRError;

public class BugReportActivity extends BaseActivity {
    private static final LinkedList<RRError> errors = new LinkedList<>();

    public static synchronized void addGlobalError(RRError error) {
        synchronized (BugReportActivity.class) {
            errors.add(error);
        }
    }

    public static synchronized void handleGlobalError(Context context, String text) {
        synchronized (BugReportActivity.class) {
            handleGlobalError(context, new RRError(text, null, new RuntimeException()));
        }
    }

    public static synchronized void handleGlobalError(Context context, Throwable t) {
        synchronized (BugReportActivity.class) {
            if (t != null) {
                Log.e("BugReportActivity", "Handling exception", t);
            }
            handleGlobalError(context, new RRError(null, null, t));
        }
    }

    public static synchronized void handleGlobalError(final Context context, RRError error) {
        synchronized (BugReportActivity.class) {
            addGlobalError(error);
            AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                public void run() {
                    Intent intent = new Intent(context, BugReportActivity.class);
                    intent.addFlags(C.ENCODING_PCM_MU_LAW);
                    context.startActivity(intent);
                }
            });
        }
    }

    /* access modifiers changed from: private */
    public static synchronized LinkedList<RRError> getErrors() {
        LinkedList<RRError> result;
        synchronized (BugReportActivity.class) {
            result = new LinkedList<>(errors);
            errors.clear();
        }
        return result;
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(1);
        TextView title = new TextView(this);
        title.setText(R.string.bug_title);
        layout.addView(title);
        title.setTextSize(20.0f);
        TextView text = new TextView(this);
        text.setText(R.string.bug_message);
        layout.addView(text);
        text.setTextSize(15.0f);
        int paddingPx = General.dpToPixels(this, 20.0f);
        title.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        text.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        Button send = new Button(this);
        send.setText(R.string.bug_button_send);
        send.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                LinkedList<RRError> errors = BugReportActivity.getErrors();
                StringBuilder sb = new StringBuilder(1024);
                sb.append("Error report -- RedReader v");
                sb.append(Constants.version(BugReportActivity.this));
                sb.append("\r\n\r\n");
                Iterator it = errors.iterator();
                while (it.hasNext()) {
                    RRError error = (RRError) it.next();
                    sb.append("-------------------------------");
                    if (error.title != null) {
                        sb.append("Title: ");
                        sb.append(error.title);
                        sb.append("\r\n");
                    }
                    if (error.message != null) {
                        sb.append("Message: ");
                        sb.append(error.message);
                        sb.append("\r\n");
                    }
                    if (error.httpStatus != null) {
                        sb.append("HTTP Status: ");
                        sb.append(error.httpStatus);
                        sb.append("\r\n");
                    }
                    BugReportActivity.appendException(sb, error.t, 25);
                }
                Intent intent = new Intent("android.intent.action.SEND");
                intent.setType("message/rfc822");
                intent.putExtra("android.intent.extra.EMAIL", new String[]{"bugreports@redreader.org"});
                intent.putExtra("android.intent.extra.SUBJECT", "Bug Report");
                intent.putExtra("android.intent.extra.TEXT", sb.toString());
                try {
                    BugReportActivity.this.startActivity(Intent.createChooser(intent, "Email bug report"));
                } catch (ActivityNotFoundException e) {
                    General.quickToast((Context) BugReportActivity.this, "No email apps installed!");
                }
                BugReportActivity.this.finish();
            }
        });
        Button ignore = new Button(this);
        ignore.setText(R.string.bug_button_ignore);
        ignore.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                BugReportActivity.this.finish();
            }
        });
        layout.addView(send);
        layout.addView(ignore);
        ScrollView sv = new ScrollView(this);
        sv.addView(layout);
        setBaseActivityContentView((View) sv);
    }

    public static void appendException(StringBuilder sb, Throwable t, int recurseLimit) {
        StackTraceElement[] stackTrace;
        if (t != null) {
            sb.append("Exception: ");
            sb.append(t.getClass().getCanonicalName());
            sb.append("\r\n");
            sb.append(t.getMessage());
            sb.append("\r\n");
            for (StackTraceElement elem : t.getStackTrace()) {
                sb.append("  ");
                sb.append(elem.toString());
                sb.append("\r\n");
            }
            if (recurseLimit > 0 && t.getCause() != null) {
                sb.append("Caused by: ");
                appendException(sb, t.getCause(), recurseLimit - 1);
            }
        }
    }
}
