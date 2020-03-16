package org.quantumbadger.redreader.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.fragments.WebViewFragment;

public class HtmlViewActivity extends BaseActivity {
    private WebViewFragment webView;

    public static void showAsset(Context context, String filename) {
        try {
            InputStream asset = context.getAssets().open(filename);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(16384);
            byte[] buf = new byte[8192];
            while (true) {
                int read = asset.read(buf);
                int bytesRead = read;
                if (read > 0) {
                    baos.write(buf, 0, bytesRead);
                } else {
                    String html = baos.toString("UTF-8");
                    Intent intent = new Intent(context, HtmlViewActivity.class);
                    intent.putExtra("html", html);
                    context.startActivity(intent);
                    return;
                }
            }
        } catch (IOException e) {
            BugReportActivity.handleGlobalError(context, (Throwable) e);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String html = intent.getStringExtra("html");
        setTitle((CharSequence) intent.getStringExtra("title"));
        if (html == null) {
            BugReportActivity.handleGlobalError((Context) this, "No HTML");
        }
        this.webView = WebViewFragment.newInstanceHtml(html);
        setBaseActivityContentView(View.inflate(this, R.layout.main_single, null));
        getSupportFragmentManager().beginTransaction().add((int) R.id.main_single_frame, (Fragment) this.webView).commit();
    }

    public void onBackPressed() {
        if (General.onBackPressed() && !this.webView.onBackButtonPressed()) {
            super.onBackPressed();
        }
    }
}
