package org.quantumbadger.redreader.views.webview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import info.guardianproject.netcipher.web.WebkitProxy;
import java.util.Map;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.RedReader;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.common.TorCommon;

public class WebViewFixed extends WebView {
    private boolean addedJavascriptInterface = false;
    /* access modifiers changed from: private */
    public VideoEnabledWebChromeClient videoEnabledWebChromeClient;

    public class JavascriptInterface {
        public JavascriptInterface() {
        }

        @android.webkit.JavascriptInterface
        public void notifyVideoEnd() {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                public void run() {
                    if (WebViewFixed.this.videoEnabledWebChromeClient != null) {
                        WebViewFixed.this.videoEnabledWebChromeClient.onHideCustomView();
                    }
                }
            });
        }
    }

    public WebViewFixed(Context context) {
        super(context);
        setTor(context);
    }

    public WebViewFixed(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setTor(context);
    }

    public WebViewFixed(Context context, AttributeSet attrs) {
        super(context, attrs);
        setTor(context);
    }

    public boolean isVideoFullscreen() {
        VideoEnabledWebChromeClient videoEnabledWebChromeClient2 = this.videoEnabledWebChromeClient;
        return videoEnabledWebChromeClient2 != null && videoEnabledWebChromeClient2.isVideoFullscreen();
    }

    @SuppressLint({"SetJavaScriptEnabled"})
    public void setWebChromeClient(WebChromeClient client) {
        getSettings().setJavaScriptEnabled(true);
        if (client instanceof VideoEnabledWebChromeClient) {
            this.videoEnabledWebChromeClient = (VideoEnabledWebChromeClient) client;
        }
        super.setWebChromeClient(client);
    }

    public void loadData(String data, String mimeType, String encoding) {
        addJavascriptInterface();
        super.loadData(data, mimeType, encoding);
    }

    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
        addJavascriptInterface();
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl);
    }

    public void loadUrl(String url) {
        addJavascriptInterface();
        super.loadUrl(url);
    }

    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        addJavascriptInterface();
        super.loadUrl(url, additionalHttpHeaders);
    }

    private void addJavascriptInterface() {
        if (!this.addedJavascriptInterface) {
            addJavascriptInterface(new JavascriptInterface(), "_VideoEnabledWebView");
            this.addedJavascriptInterface = true;
        }
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
        try {
            super.onWindowFocusChanged(hasWindowFocus);
        } catch (NullPointerException ex) {
            Log.e("WebView", "WebView.onWindowFocusChanged", ex);
        }
    }

    private void setTor(Context context) {
        if (TorCommon.isTorEnabled()) {
            try {
                clearBrowser();
                if (!WebkitProxy.setProxy(RedReader.class.getCanonicalName(), context.getApplicationContext(), this, "127.0.0.1", 8118)) {
                    BugReportActivity.handleGlobalError(context, getResources().getString(R.string.error_tor_setting_failed));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void clearBrowser() {
        clearCache(true);
        clearFormData();
        clearHistory();
        CookieManager.getInstance().removeAllCookie();
    }
}
