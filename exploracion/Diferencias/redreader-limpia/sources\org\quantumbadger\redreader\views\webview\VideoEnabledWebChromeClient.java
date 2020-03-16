package org.quantumbadger.redreader.views.webview;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebChromeClient;
import android.webkit.WebChromeClient.CustomViewCallback;
import android.widget.FrameLayout;
import android.widget.VideoView;

public class VideoEnabledWebChromeClient extends WebChromeClient implements OnPreparedListener, OnCompletionListener, OnErrorListener {
    private View activityNonVideoView;
    private ViewGroup activityVideoView;
    private boolean isVideoFullscreen;
    private View loadingView;
    private ToggledFullscreenCallback toggledFullscreenCallback;
    private CustomViewCallback videoViewCallback;
    private FrameLayout videoViewContainer;
    private WebViewFixed webView;

    public interface ToggledFullscreenCallback {
        void toggledFullscreen(boolean z);
    }

    public VideoEnabledWebChromeClient() {
    }

    public VideoEnabledWebChromeClient(View activityNonVideoView2, ViewGroup activityVideoView2) {
        this.activityNonVideoView = activityNonVideoView2;
        this.activityVideoView = activityVideoView2;
        this.loadingView = null;
        this.webView = null;
        this.isVideoFullscreen = false;
    }

    public VideoEnabledWebChromeClient(View activityNonVideoView2, ViewGroup activityVideoView2, View loadingView2) {
        this.activityNonVideoView = activityNonVideoView2;
        this.activityVideoView = activityVideoView2;
        this.loadingView = loadingView2;
        this.webView = null;
        this.isVideoFullscreen = false;
    }

    public VideoEnabledWebChromeClient(View activityNonVideoView2, ViewGroup activityVideoView2, View loadingView2, WebViewFixed webView2) {
        this.activityNonVideoView = activityNonVideoView2;
        this.activityVideoView = activityVideoView2;
        this.loadingView = loadingView2;
        this.webView = webView2;
        this.isVideoFullscreen = false;
    }

    public boolean isVideoFullscreen() {
        return this.isVideoFullscreen;
    }

    public void setOnToggledFullscreen(ToggledFullscreenCallback callback) {
        this.toggledFullscreenCallback = callback;
    }

    public void onShowCustomView(View view, CustomViewCallback callback) {
        if (view instanceof FrameLayout) {
            FrameLayout frameLayout = (FrameLayout) view;
            View focusedChild = frameLayout.getFocusedChild();
            this.isVideoFullscreen = true;
            this.videoViewContainer = frameLayout;
            this.videoViewCallback = callback;
            this.activityNonVideoView.setVisibility(4);
            this.activityVideoView.addView(this.videoViewContainer, new LayoutParams(-1, -1));
            this.activityVideoView.setVisibility(0);
            if (focusedChild instanceof VideoView) {
                VideoView videoView = (VideoView) focusedChild;
                videoView.setOnPreparedListener(this);
                videoView.setOnCompletionListener(this);
                videoView.setOnErrorListener(this);
            } else {
                WebViewFixed webViewFixed = this.webView;
                if (webViewFixed != null && webViewFixed.getSettings().getJavaScriptEnabled() && (focusedChild instanceof SurfaceView)) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("javascript:");
                    sb.append("var _ytrp_html5_video_last;");
                    String js = sb.toString();
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(js);
                    sb2.append("var _ytrp_html5_video = document.getElementsByTagName('video')[0];");
                    String js2 = sb2.toString();
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append(js2);
                    sb3.append("if (_ytrp_html5_video != undefined && _ytrp_html5_video != _ytrp_html5_video_last) {");
                    String js3 = sb3.toString();
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append(js3);
                    sb4.append("_ytrp_html5_video_last = _ytrp_html5_video;");
                    String js4 = sb4.toString();
                    StringBuilder sb5 = new StringBuilder();
                    sb5.append(js4);
                    sb5.append("function _ytrp_html5_video_ended() {");
                    String js5 = sb5.toString();
                    StringBuilder sb6 = new StringBuilder();
                    sb6.append(js5);
                    sb6.append("_WebViewFixed.notifyVideoEnd();");
                    String js6 = sb6.toString();
                    StringBuilder sb7 = new StringBuilder();
                    sb7.append(js6);
                    sb7.append("}");
                    String js7 = sb7.toString();
                    StringBuilder sb8 = new StringBuilder();
                    sb8.append(js7);
                    sb8.append("_ytrp_html5_video.addEventListener('ended', _ytrp_html5_video_ended);");
                    String js8 = sb8.toString();
                    StringBuilder sb9 = new StringBuilder();
                    sb9.append(js8);
                    sb9.append("}");
                    this.webView.loadUrl(sb9.toString());
                }
            }
            ToggledFullscreenCallback toggledFullscreenCallback2 = this.toggledFullscreenCallback;
            if (toggledFullscreenCallback2 != null) {
                toggledFullscreenCallback2.toggledFullscreen(true);
            }
        }
    }

    public void onShowCustomView(View view, int requestedOrientation, CustomViewCallback callback) {
        onShowCustomView(view, callback);
    }

    public void onHideCustomView() {
        if (this.isVideoFullscreen) {
            this.activityVideoView.setVisibility(4);
            this.activityVideoView.removeView(this.videoViewContainer);
            this.activityNonVideoView.setVisibility(0);
            CustomViewCallback customViewCallback = this.videoViewCallback;
            if (customViewCallback != null && !customViewCallback.getClass().getName().contains(".chromium.")) {
                this.videoViewCallback.onCustomViewHidden();
            }
            this.isVideoFullscreen = false;
            this.videoViewContainer = null;
            this.videoViewCallback = null;
            ToggledFullscreenCallback toggledFullscreenCallback2 = this.toggledFullscreenCallback;
            if (toggledFullscreenCallback2 != null) {
                toggledFullscreenCallback2.toggledFullscreen(false);
            }
        }
    }

    public View getVideoLoadingProgressView() {
        View view = this.loadingView;
        if (view == null) {
            return super.getVideoLoadingProgressView();
        }
        view.setVisibility(0);
        return this.loadingView;
    }

    public void onPrepared(MediaPlayer mp) {
        View view = this.loadingView;
        if (view != null) {
            view.setVisibility(8);
        }
    }

    public void onCompletion(MediaPlayer mp) {
        onHideCustomView();
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    public boolean onBackPressed() {
        if (!this.isVideoFullscreen) {
            return false;
        }
        onHideCustomView();
        return true;
    }
}
