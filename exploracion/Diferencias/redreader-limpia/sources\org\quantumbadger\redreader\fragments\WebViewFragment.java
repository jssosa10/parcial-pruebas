package org.quantumbadger.redreader.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.reddit.prepared.RedditParsedPost;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;
import org.quantumbadger.redreader.reddit.things.RedditPost;
import org.quantumbadger.redreader.reddit.url.RedditURLParser;
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener;
import org.quantumbadger.redreader.views.bezelmenu.BezelSwipeOverlay;
import org.quantumbadger.redreader.views.bezelmenu.BezelSwipeOverlay.BezelSwipeListener;
import org.quantumbadger.redreader.views.bezelmenu.SideToolbarOverlay;
import org.quantumbadger.redreader.views.bezelmenu.SideToolbarOverlay.SideToolbarPosition;
import org.quantumbadger.redreader.views.webview.VideoEnabledWebChromeClient;
import org.quantumbadger.redreader.views.webview.VideoEnabledWebChromeClient.ToggledFullscreenCallback;
import org.quantumbadger.redreader.views.webview.WebViewFixed;

public class WebViewFragment extends Fragment implements PostSelectionListener {
    /* access modifiers changed from: private */
    public volatile String currentUrl;
    /* access modifiers changed from: private */
    public volatile boolean goingBack;
    private String html;
    /* access modifiers changed from: private */
    public volatile int lastBackDepthAttempt;
    /* access modifiers changed from: private */
    public AppCompatActivity mActivity;
    /* access modifiers changed from: private */
    public String mUrl;
    private FrameLayout outer;
    /* access modifiers changed from: private */
    public ProgressBar progressView;
    /* access modifiers changed from: private */
    public WebViewFixed webView;

    public static WebViewFragment newInstance(String url, RedditPost post) {
        WebViewFragment f = new WebViewFragment();
        Bundle bundle = new Bundle(1);
        bundle.putString("url", url);
        if (post != null) {
            bundle.putParcelable("post", post);
        }
        f.setArguments(bundle);
        return f;
    }

    public static WebViewFragment newInstanceHtml(String html2) {
        WebViewFragment f = new WebViewFragment();
        Bundle bundle = new Bundle(1);
        bundle.putString("html", html2);
        f.setArguments(bundle);
        return f;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mUrl = getArguments().getString("url");
        this.html = getArguments().getString("html");
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final RedditPreparedPost post;
        this.mActivity = (AppCompatActivity) getActivity();
        CookieSyncManager.createInstance(this.mActivity);
        this.outer = (FrameLayout) inflater.inflate(R.layout.web_view_fragment, null);
        RedditPost src_post = (RedditPost) getArguments().getParcelable("post");
        if (src_post != null) {
            RedditParsedPost parsedPost = new RedditParsedPost(src_post, false);
            AppCompatActivity appCompatActivity = this.mActivity;
            post = new RedditPreparedPost(appCompatActivity, CacheManager.getInstance(appCompatActivity), 0, parsedPost, -1, false, false);
        } else {
            post = null;
        }
        this.webView = (WebViewFixed) this.outer.findViewById(R.id.web_view_fragment_webviewfixed);
        FrameLayout loadingViewFrame = (FrameLayout) this.outer.findViewById(R.id.web_view_fragment_loadingview_frame);
        this.progressView = new ProgressBar(this.mActivity, null, 16842872);
        loadingViewFrame.addView(this.progressView);
        loadingViewFrame.setPadding(General.dpToPixels(this.mActivity, 10.0f), 0, General.dpToPixels(this.mActivity, 10.0f), 0);
        VideoEnabledWebChromeClient chromeClient = new VideoEnabledWebChromeClient(loadingViewFrame, (FrameLayout) this.outer.findViewById(R.id.web_view_fragment_fullscreen_frame)) {
            public void onProgressChanged(WebView view, final int newProgress) {
                super.onProgressChanged(view, newProgress);
                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                    public void run() {
                        WebViewFragment.this.progressView.setProgress(newProgress);
                        WebViewFragment.this.progressView.setVisibility(newProgress == 100 ? 8 : 0);
                    }
                });
            }
        };
        chromeClient.setOnToggledFullscreen(new ToggledFullscreenCallback() {
            public void toggledFullscreen(boolean fullscreen) {
                if (fullscreen) {
                    LayoutParams attrs = WebViewFragment.this.getActivity().getWindow().getAttributes();
                    attrs.flags |= 1024;
                    attrs.flags |= 128;
                    WebViewFragment.this.getActivity().getWindow().setAttributes(attrs);
                    ((AppCompatActivity) WebViewFragment.this.getActivity()).getSupportActionBar().hide();
                    if (VERSION.SDK_INT >= 14) {
                        WebViewFragment.this.getActivity().getWindow().getDecorView().setSystemUiVisibility(1);
                        return;
                    }
                    return;
                }
                LayoutParams attrs2 = WebViewFragment.this.getActivity().getWindow().getAttributes();
                if (!PrefsUtility.pref_appearance_hide_android_status(WebViewFragment.this.getContext(), PreferenceManager.getDefaultSharedPreferences(WebViewFragment.this.getContext()))) {
                    attrs2.flags &= -1025;
                }
                attrs2.flags &= -129;
                WebViewFragment.this.getActivity().getWindow().setAttributes(attrs2);
                ((AppCompatActivity) WebViewFragment.this.getActivity()).getSupportActionBar().show();
                if (VERSION.SDK_INT >= 14) {
                    WebViewFragment.this.getActivity().getWindow().getDecorView().setSystemUiVisibility(0);
                }
            }
        });
        this.webView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(final String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                new Builder(WebViewFragment.this.mActivity).setTitle((int) R.string.download_link_title).setMessage((int) R.string.download_link_message).setPositiveButton(17039379, (OnClickListener) new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent("android.intent.action.VIEW");
                        i.setData(Uri.parse(url));
                        WebViewFragment.this.getContext().startActivity(i);
                        WebViewFragment.this.mActivity.onBackPressed();
                    }
                }).setNegativeButton(17039369, (OnClickListener) new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        WebViewFragment.this.mActivity.onBackPressed();
                    }
                }).setIcon(17301543).show();
            }
        });
        WebSettings settings = this.webView.getSettings();
        settings.setBuiltInZoomControls(true);
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setDomStorageEnabled(true);
        settings.setDisplayZoomControls(false);
        this.webView.setWebChromeClient(chromeClient);
        String str = this.mUrl;
        if (str != null) {
            this.webView.loadUrl(str);
        } else {
            this.webView.loadDataWithBaseURL("https://reddit.com/", this.html, "text/html; charset=UTF-8", null, null);
        }
        this.webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) {
                    return false;
                }
                if (url.startsWith("data:")) {
                    return true;
                }
                if (WebViewFragment.this.goingBack && WebViewFragment.this.currentUrl != null && url.equals(WebViewFragment.this.currentUrl)) {
                    General.quickToast(WebViewFragment.this.mActivity, String.format(Locale.US, "Handling redirect loop (level %d)", new Object[]{Integer.valueOf(-WebViewFragment.this.lastBackDepthAttempt)}), 0);
                    WebViewFragment.this.lastBackDepthAttempt = WebViewFragment.this.lastBackDepthAttempt - 1;
                    if (WebViewFragment.this.webView.canGoBackOrForward(WebViewFragment.this.lastBackDepthAttempt)) {
                        WebViewFragment.this.webView.goBackOrForward(WebViewFragment.this.lastBackDepthAttempt);
                    } else {
                        WebViewFragment.this.mActivity.finish();
                    }
                } else if (RedditURLParser.parse(Uri.parse(url)) != null) {
                    LinkHandler.onLinkClicked(WebViewFragment.this.mActivity, url, false);
                } else {
                    WebViewFragment.this.webView.loadUrl(url);
                    WebViewFragment.this.currentUrl = url;
                }
                return true;
            }

            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (WebViewFragment.this.mUrl != null && url != null) {
                    AppCompatActivity activity = WebViewFragment.this.mActivity;
                    if (activity != null) {
                        activity.setTitle(url);
                    }
                }
            }

            public void onPageFinished(final WebView view, final String url) {
                super.onPageFinished(view, url);
                new Timer().schedule(new TimerTask() {
                    public void run() {
                        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                            public void run() {
                                if (WebViewFragment.this.currentUrl != null && url != null && url.equals(view.getUrl())) {
                                    if (!WebViewFragment.this.goingBack || !url.equals(WebViewFragment.this.currentUrl)) {
                                        WebViewFragment.this.goingBack = false;
                                    } else {
                                        General.quickToast((Context) WebViewFragment.this.mActivity, String.format(Locale.US, "Handling redirect loop (level %d)", new Object[]{Integer.valueOf(-WebViewFragment.this.lastBackDepthAttempt)}));
                                        WebViewFragment.this.lastBackDepthAttempt = WebViewFragment.this.lastBackDepthAttempt - 1;
                                        if (WebViewFragment.this.webView.canGoBackOrForward(WebViewFragment.this.lastBackDepthAttempt)) {
                                            WebViewFragment.this.webView.goBackOrForward(WebViewFragment.this.lastBackDepthAttempt);
                                        } else {
                                            WebViewFragment.this.mActivity.finish();
                                        }
                                    }
                                }
                            }
                        });
                    }
                }, 1000);
            }

            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);
            }
        });
        FrameLayout outerFrame = new FrameLayout(this.mActivity);
        outerFrame.addView(this.outer);
        if (post != null) {
            final SideToolbarOverlay toolbarOverlay = new SideToolbarOverlay(this.mActivity);
            BezelSwipeOverlay bezelOverlay = new BezelSwipeOverlay(this.mActivity, new BezelSwipeListener() {
                public boolean onSwipe(int edge) {
                    toolbarOverlay.setContents(post.generateToolbar(WebViewFragment.this.mActivity, false, toolbarOverlay));
                    toolbarOverlay.show(edge == 0 ? SideToolbarPosition.LEFT : SideToolbarPosition.RIGHT);
                    return true;
                }

                public boolean onTap() {
                    if (!toolbarOverlay.isShown()) {
                        return false;
                    }
                    toolbarOverlay.hide();
                    return true;
                }
            });
            outerFrame.addView(bezelOverlay);
            outerFrame.addView(toolbarOverlay);
            bezelOverlay.getLayoutParams().width = -1;
            bezelOverlay.getLayoutParams().height = -1;
            toolbarOverlay.getLayoutParams().width = -1;
            toolbarOverlay.getLayoutParams().height = -1;
        }
        return outerFrame;
    }

    public void onDestroyView() {
        this.webView.stopLoading();
        this.webView.loadData("<html></html>", "text/plain", "UTF-8");
        this.webView.reload();
        this.webView.loadUrl("about:blank");
        this.outer.removeAllViews();
        this.webView.destroy();
        CookieManager.getInstance().removeAllCookie();
        super.onDestroyView();
    }

    public boolean onBackButtonPressed() {
        if (!this.webView.canGoBack()) {
            return false;
        }
        this.goingBack = true;
        this.lastBackDepthAttempt = -1;
        this.webView.goBack();
        return true;
    }

    public void onPostSelected(RedditPreparedPost post) {
        ((PostSelectionListener) this.mActivity).onPostSelected(post);
    }

    public void onPostCommentsSelected(RedditPreparedPost post) {
        ((PostSelectionListener) this.mActivity).onPostCommentsSelected(post);
    }

    public String getCurrentUrl() {
        return this.currentUrl != null ? this.currentUrl : this.mUrl;
    }

    public void onPause() {
        super.onPause();
        this.webView.onPause();
        this.webView.pauseTimers();
    }

    public void onResume() {
        super.onResume();
        this.webView.resumeTimers();
        this.webView.onResume();
    }

    public void clearCache() {
        this.webView.clearBrowser();
    }
}
