package org.quantumbadger.redreader.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import org.quantumbadger.redreader.common.LinkHandler;

public class LinkDispatchActivity extends AppCompatActivity {
    private static final String TAG = "LinkDispatchActivity";

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null) {
            Log.e(TAG, "Got null intent");
            finish();
            return;
        }
        Uri data = intent.getData();
        if (data == null) {
            Log.e(TAG, "Got null intent data");
            finish();
            return;
        }
        LinkHandler.onLinkClicked(this, data.toString(), false, null, null, 0, true);
        finish();
    }
}
