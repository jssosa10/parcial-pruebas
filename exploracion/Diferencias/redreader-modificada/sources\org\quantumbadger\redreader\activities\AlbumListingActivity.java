package org.quantumbadger.redreader.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import java.util.regex.Matcher;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.adapters.AlbumAdapter;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.Constants.Priority;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.image.GetAlbumInfoListener;
import org.quantumbadger.redreader.image.GetImageInfoListener;
import org.quantumbadger.redreader.image.ImageInfo;
import org.quantumbadger.redreader.image.ImgurAPI.AlbumInfo;
import org.quantumbadger.redreader.views.ScrollbarRecyclerViewManager;

public class AlbumListingActivity extends BaseActivity {
    /* access modifiers changed from: private */
    public boolean mHaveReverted = false;
    /* access modifiers changed from: private */
    public String mUrl;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);
        setTitle((int) R.string.imgur_album);
        this.mUrl = getIntent().getDataString();
        if (this.mUrl == null) {
            finish();
            return;
        }
        Matcher matchImgur = LinkHandler.imgurAlbumPattern.matcher(this.mUrl);
        if (matchImgur.find()) {
            final String albumId = matchImgur.group(2);
            StringBuilder sb = new StringBuilder();
            sb.append("Loading URL ");
            sb.append(this.mUrl);
            sb.append(", album id ");
            sb.append(albumId);
            Log.i("AlbumListingActivity", sb.toString());
            ProgressBar progressBar = new ProgressBar(this, null, 16842872);
            progressBar.setIndeterminate(true);
            final LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(1);
            layout.addView(progressBar);
            LinkHandler.getImgurAlbumInfo(this, albumId, Priority.IMAGE_VIEW, 0, new GetAlbumInfoListener() {
                public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("getAlbumInfo call failed: ");
                    sb.append(type);
                    Log.e("AlbumListingActivity", sb.toString());
                    if (status != null) {
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("status was: ");
                        sb2.append(status.toString());
                        Log.e("AlbumListingActivity", sb2.toString());
                    }
                    if (t != null) {
                        Log.e("AlbumListingActivity", "exception was: ", t);
                    }
                    if (status == null) {
                        AlbumListingActivity.this.revertToWeb();
                    } else {
                        LinkHandler.getImgurImageInfo(AlbumListingActivity.this, albumId, Priority.IMAGE_VIEW, 0, false, new GetImageInfoListener() {
                            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("Image info request also failed: ");
                                sb.append(type);
                                Log.e("AlbumListingActivity", sb.toString());
                                AlbumListingActivity.this.revertToWeb();
                            }

                            public void onSuccess(ImageInfo info2) {
                                Log.i("AlbumListingActivity", "Link was actually an image.");
                                LinkHandler.onLinkClicked(AlbumListingActivity.this, info2.urlOriginal);
                                AlbumListingActivity.this.finish();
                            }

                            public void onNotAnImage() {
                                Log.i("AlbumListingActivity", "Not an image either");
                                AlbumListingActivity.this.revertToWeb();
                            }
                        });
                    }
                }

                public void onSuccess(final AlbumInfo info2) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Got album, ");
                    sb.append(info2.images.size());
                    sb.append(" image(s)");
                    Log.i("AlbumListingActivity", sb.toString());
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            if (info2.title != null && !info2.title.trim().isEmpty()) {
                                AlbumListingActivity albumListingActivity = AlbumListingActivity.this;
                                StringBuilder sb = new StringBuilder();
                                sb.append(AlbumListingActivity.this.getString(R.string.imgur_album));
                                sb.append(": ");
                                sb.append(info2.title);
                                albumListingActivity.setTitle((CharSequence) sb.toString());
                            }
                            layout.removeAllViews();
                            if (info2.images.size() == 1) {
                                LinkHandler.onLinkClicked(AlbumListingActivity.this, ((ImageInfo) info2.images.get(0)).urlOriginal);
                                AlbumListingActivity.this.finish();
                                return;
                            }
                            ScrollbarRecyclerViewManager recyclerViewManager = new ScrollbarRecyclerViewManager(AlbumListingActivity.this, null, false);
                            layout.addView(recyclerViewManager.getOuterView());
                            recyclerViewManager.getRecyclerView().setAdapter(new AlbumAdapter(AlbumListingActivity.this, info2));
                        }
                    });
                }
            });
            setBaseActivityContentView((View) layout);
            return;
        }
        Log.e("AlbumListingActivity", "URL match failed");
        revertToWeb();
    }

    public void onBackPressed() {
        if (General.onBackPressed()) {
            super.onBackPressed();
        }
    }

    /* access modifiers changed from: private */
    public void revertToWeb() {
        Runnable r = new Runnable() {
            public void run() {
                if (!AlbumListingActivity.this.mHaveReverted) {
                    AlbumListingActivity.this.mHaveReverted = true;
                    AlbumListingActivity albumListingActivity = AlbumListingActivity.this;
                    LinkHandler.onLinkClicked(albumListingActivity, albumListingActivity.mUrl, true);
                    AlbumListingActivity.this.finish();
                }
            }
        };
        if (General.isThisUIThread()) {
            r.run();
        } else {
            AndroidCommon.UI_THREAD_HANDLER.post(r);
        }
    }
}
