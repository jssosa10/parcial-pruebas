package org.quantumbadger.redreader.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.fragments.WebViewFragment;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;
import org.quantumbadger.redreader.reddit.things.RedditPost;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL;
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener;

public class WebViewActivity extends BaseActivity implements PostSelectionListener {
    public static final int CLEAR_CACHE = 20;
    public static final int SHARE = 40;
    public static final int USE_HTTPS = 30;
    public static final int VIEW_IN_BROWSER = 10;
    private RedditPost mPost;
    private WebViewFragment webView;

    public void onCreate(Bundle savedInstanceState) {
        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        String url = intent.getStringExtra("url");
        this.mPost = (RedditPost) intent.getParcelableExtra("post");
        if (url == null) {
            BugReportActivity.handleGlobalError((Context) this, "No URL");
        }
        this.webView = WebViewFragment.newInstance(url, this.mPost);
        setBaseActivityContentView(View.inflate(this, R.layout.main_single, null));
        getSupportFragmentManager().beginTransaction().add((int) R.id.main_single_frame, (Fragment) this.webView).commit();
    }

    public void onBackPressed() {
        if (General.onBackPressed() && !this.webView.onBackButtonPressed()) {
            super.onBackPressed();
        }
    }

    public void onPostSelected(RedditPreparedPost post) {
        LinkHandler.onLinkClicked(this, post.src.getUrl(), false, post.src.getSrc());
    }

    public void onPostCommentsSelected(RedditPreparedPost post) {
        LinkHandler.onLinkClicked(this, PostCommentListingURL.forPostId(post.src.getIdAlone()).toString(), false);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        String currentUrl = this.webView.getCurrentUrl();
        int itemId = item.getItemId();
        if (itemId == 10) {
            if (currentUrl != null) {
                try {
                    Intent intent = new Intent("android.intent.action.VIEW");
                    intent.setData(Uri.parse(currentUrl));
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    Toast.makeText(this, "Error: could not launch browser.", 1).show();
                }
            }
            return true;
        } else if (itemId != 20) {
            if (itemId != 30) {
                if (itemId != 40) {
                    return super.onOptionsItemSelected(item);
                }
            } else if (currentUrl != null) {
                if (currentUrl.startsWith("https://")) {
                    General.quickToast((Context) this, (int) R.string.webview_https_already);
                    return true;
                } else if (!currentUrl.startsWith("http://")) {
                    General.quickToast((Context) this, (int) R.string.webview_https_unknownprotocol);
                    return true;
                } else {
                    LinkHandler.onLinkClicked(this, currentUrl.replace("http://", "https://"), true, this.mPost);
                    return true;
                }
            }
            if (currentUrl != null) {
                Intent mailer = new Intent("android.intent.action.SEND");
                mailer.setType("text/plain");
                RedditPost redditPost = this.mPost;
                if (redditPost != null) {
                    mailer.putExtra("android.intent.extra.SUBJECT", redditPost.title);
                }
                mailer.putExtra("android.intent.extra.TEXT", currentUrl);
                startActivity(Intent.createChooser(mailer, getString(R.string.action_share)));
            }
            return true;
        } else {
            this.webView.clearCache();
            Toast.makeText(this, R.string.web_view_clear_cache_success_toast, 1).show();
            return true;
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 10, 0, R.string.web_view_open_browser);
        menu.add(0, 20, 1, R.string.web_view_clear_cache);
        menu.add(0, 30, 2, R.string.webview_use_https);
        menu.add(0, 40, 3, R.string.action_share);
        return super.onCreateOptionsMenu(menu);
    }

    public String getCurrentUrl() {
        return this.webView.getCurrentUrl();
    }
}
