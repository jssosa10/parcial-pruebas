package org.quantumbadger.redreader.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import info.guardianproject.netcipher.web.WebkitProxy;
import java.io.ByteArrayInputStream;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.RedReader;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.TorCommon;
import org.quantumbadger.redreader.reddit.api.RedditOAuth;

public class OAuthLoginActivity extends BaseActivity {
    private static final String CSS_FIXES = "li {\n  list-style-type: none;\n  margin:10px\n}\n\nlabel {\n  margin-right: 10px;\n}\n\ndiv.icon, div.infobar, div.mobile-web-redirect-bar, div#topbar {\n  display: none;\n  visibility: collapse;\n  height: 0px;\n  padding: 0px;\n  margin:0px;\n}\n\ndiv.content {\n  padding: 0px;\n  margin: 20px;\n}\n\nbody {\n  background-color: #FFF;\n}\n\ninput.newbutton {\n  background-color: #888;\n  font-size: 20pt;\n  margin: 10px;\n  border-image-source: none;\n  color: #FFF;\n  border: none;\n  padding-left:10px;\n  padding-right:10px;\n  padding-top:6px;\n  padding-bottom:6px;\n}\n\nbutton {\n  background-color: #888;\n  font-size: 15pt;\n  border-image-source: none;\n  color: #FFF;\n  border: none;\n  padding-left:10px;\n  padding-right:10px;\n  padding-top:6px;\n  padding-bottom:6px;\n}\n\ninput.allow {\n  background-color: #0A0;\n}\n\ninput.allow:active, input.allow:hover {\n  background-color: #0F0;\n}\n\ninput.decline {\n  background-color: #A00;\n}\n\ninput.decline:active, input.decline:hover {\n  background-color: #F00;\n}\n\nform.pretty-form {\n  float: left;\n}\n\n";
    private WebView mWebView;

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        CookieManager.getInstance().removeAllCookie();
    }

    public void onCreate(Bundle savedInstanceState) {
        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);
        this.mWebView = new WebView(this);
        if (TorCommon.isTorEnabled()) {
            try {
                if (!WebkitProxy.setProxy(RedReader.class.getCanonicalName(), getApplicationContext(), this.mWebView, "127.0.0.1", 8118)) {
                    BugReportActivity.handleGlobalError((Context) this, getResources().getString(R.string.error_tor_setting_failed));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        WebSettings settings = this.mWebView.getSettings();
        settings.setBuiltInZoomControls(false);
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setDomStorageEnabled(true);
        settings.setSaveFormData(false);
        settings.setSavePassword(false);
        settings.setDatabaseEnabled(false);
        settings.setAppCacheEnabled(false);
        settings.setDisplayZoomControls(false);
        setTitle((CharSequence) RedditOAuth.getPromptUri().toString());
        this.mWebView.loadUrl(RedditOAuth.getPromptUri().toString());
        this.mWebView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("http://rr_oauth_redir")) {
                    Intent intent = new Intent();
                    intent.putExtra("url", url);
                    OAuthLoginActivity.this.setResult(123, intent);
                    OAuthLoginActivity.this.finish();
                    return true;
                }
                OAuthLoginActivity.this.setTitle((CharSequence) url);
                return false;
            }

            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (url.matches(".*compact.*\\.css")) {
                    return new WebResourceResponse("text/css", "UTF-8", new ByteArrayInputStream(OAuthLoginActivity.CSS_FIXES.getBytes()));
                }
                return null;
            }
        });
        setBaseActivityContentView((View) this.mWebView);
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        super.onPause();
        WebView webView = this.mWebView;
        if (webView != null) {
            webView.onPause();
            this.mWebView.pauseTimers();
        }
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        WebView webView = this.mWebView;
        if (webView != null) {
            webView.resumeTimers();
            this.mWebView.onResume();
        }
    }
}
