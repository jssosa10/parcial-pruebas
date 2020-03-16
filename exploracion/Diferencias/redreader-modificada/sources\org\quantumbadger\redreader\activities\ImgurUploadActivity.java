package org.quantumbadger.redreader.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Base64OutputStream;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.exoplayer2.upstream.DataSchemeDataSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.http.HTTPBackend.PostField;
import org.quantumbadger.redreader.image.ThumbnailScaler;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedObject;
import org.quantumbadger.redreader.jsonwrap.JsonValue;
import org.quantumbadger.redreader.views.LoadingSpinnerView;

public class ImgurUploadActivity extends BaseActivity {
    private static final int REQUEST_CODE = 1;
    /* access modifiers changed from: private */
    public String mBase64Data;
    private View mLoadingOverlay;
    /* access modifiers changed from: private */
    public TextView mTextView;
    /* access modifiers changed from: private */
    public ImageView mThumbnailView;
    /* access modifiers changed from: private */
    public Button mUploadButton;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);
        setTitle((int) R.string.upload_to_imgur);
        FrameLayout outerLayout = new FrameLayout(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(1);
        this.mTextView = new TextView(this);
        this.mTextView.setText(R.string.no_file_selected);
        layout.addView(this.mTextView);
        General.setAllMarginsDp(this, this.mTextView, 10);
        this.mUploadButton = new Button(this);
        this.mUploadButton.setText(R.string.button_upload);
        this.mUploadButton.setEnabled(false);
        layout.addView(this.mUploadButton);
        updateUploadButtonVisibility();
        Button browseButton = new Button(this);
        browseButton.setText(R.string.button_browse);
        layout.addView(browseButton);
        this.mThumbnailView = new ImageView(this);
        layout.addView(this.mThumbnailView);
        General.setAllMarginsDp(this, this.mThumbnailView, 20);
        browseButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent("android.intent.action.GET_CONTENT");
                intent.setType("image/*");
                ImgurUploadActivity.this.startActivityForResult(intent, 1);
            }
        });
        this.mUploadButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (ImgurUploadActivity.this.mBase64Data != null) {
                    ImgurUploadActivity.this.uploadImage();
                } else {
                    General.quickToast((Context) ImgurUploadActivity.this, (int) R.string.no_file_selected);
                }
            }
        });
        ScrollView sv = new ScrollView(this);
        sv.addView(layout);
        outerLayout.addView(sv);
        this.mLoadingOverlay = new LoadingSpinnerView(this);
        outerLayout.addView(this.mLoadingOverlay);
        this.mLoadingOverlay.setBackgroundColor(Color.argb(220, 50, 50, 50));
        LayoutParams layoutParams = this.mLoadingOverlay.getLayoutParams();
        layoutParams.width = -1;
        layoutParams.height = -1;
        this.mLoadingOverlay.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            }
        });
        this.mLoadingOverlay.setVisibility(8);
        setBaseActivityContentView((View) outerLayout);
        General.setAllMarginsDp(this, layout, 20);
    }

    private void showLoadingOverlay() {
        this.mLoadingOverlay.setVisibility(0);
    }

    /* access modifiers changed from: private */
    public void hideLoadingOverlay() {
        this.mLoadingOverlay.setVisibility(8);
    }

    /* access modifiers changed from: private */
    public void updateUploadButtonVisibility() {
        this.mUploadButton.setVisibility(this.mBase64Data != null ? 0 : 8);
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int result, Intent data) {
        if (data != null && data.getData() != null && requestCode == 1 && result == -1) {
            onImageSelected(data.getData());
        }
    }

    private void onImageSelected(final Uri uri) {
        showLoadingOverlay();
        new Thread("Image selected thread") {
            public void run() {
                ParcelFileDescriptor file;
                Base64OutputStream output;
                try {
                    file = ImgurUploadActivity.this.getContentResolver().openFileDescriptor(uri, "r");
                    long statSize = file.getStatSize();
                    if (statSize >= 10000000) {
                        ImgurUploadActivity imgurUploadActivity = ImgurUploadActivity.this;
                        String string = ImgurUploadActivity.this.getString(R.string.error_file_too_big_title);
                        ImgurUploadActivity imgurUploadActivity2 = ImgurUploadActivity.this;
                        StringBuilder sb = new StringBuilder();
                        sb.append(statSize / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID);
                        sb.append("kB");
                        General.showResultDialog(imgurUploadActivity, new RRError(string, imgurUploadActivity2.getString(R.string.error_file_too_big_message, new Object[]{sb.toString(), "10MB"})));
                        return;
                    }
                    int thumbnailSizePx = General.dpToPixels(ImgurUploadActivity.this, 200.0f);
                    Bitmap rawBitmap = BitmapFactory.decodeFileDescriptor(file.getFileDescriptor());
                    final Bitmap thumbnailBitmap = ThumbnailScaler.scaleNoCrop(rawBitmap, thumbnailSizePx);
                    rawBitmap.recycle();
                    InputStream inputStream = ImgurUploadActivity.this.getContentResolver().openInputStream(uri);
                    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
                    output = new Base64OutputStream(byteOutput, 0);
                    General.copyStream(inputStream, output);
                    output.flush();
                    final String base64String = new String(byteOutput.toByteArray());
                    Handler handler = AndroidCommon.UI_THREAD_HANDLER;
                    final Bitmap bitmap = rawBitmap;
                    Base64OutputStream base64OutputStream = output;
                    ParcelFileDescriptor parcelFileDescriptor = file;
                    AnonymousClass1 r9 = r1;
                    final long j = statSize;
                    AnonymousClass1 r1 = new Runnable() {
                        public void run() {
                            ImgurUploadActivity.this.mBase64Data = base64String;
                            ImgurUploadActivity.this.mUploadButton.setEnabled(true);
                            ImgurUploadActivity.this.mThumbnailView.setImageBitmap(thumbnailBitmap);
                            TextView access$400 = ImgurUploadActivity.this.mTextView;
                            ImgurUploadActivity imgurUploadActivity = ImgurUploadActivity.this;
                            StringBuilder sb = new StringBuilder();
                            sb.append(j / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID);
                            sb.append("kB");
                            access$400.setText(imgurUploadActivity.getString(R.string.image_selected_summary, new Object[]{Integer.valueOf(bitmap.getWidth()), Integer.valueOf(bitmap.getHeight()), sb.toString()}));
                            ImgurUploadActivity.this.hideLoadingOverlay();
                            ImgurUploadActivity.this.updateUploadButtonVisibility();
                        }
                    };
                    handler.post(r9);
                } catch (IOException e) {
                    Base64OutputStream base64OutputStream2 = output;
                    ParcelFileDescriptor parcelFileDescriptor2 = file;
                    throw new RuntimeException(e);
                } catch (Exception e2) {
                    ImgurUploadActivity imgurUploadActivity3 = ImgurUploadActivity.this;
                    General.showResultDialog(imgurUploadActivity3, new RRError(imgurUploadActivity3.getString(R.string.error_file_open_failed_title), ImgurUploadActivity.this.getString(R.string.error_file_open_failed_message), e2));
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            ImgurUploadActivity.this.mBase64Data = null;
                            ImgurUploadActivity.this.mUploadButton.setEnabled(false);
                            ImgurUploadActivity.this.mThumbnailView.setImageBitmap(null);
                            ImgurUploadActivity.this.mTextView.setText(R.string.no_file_selected);
                            ImgurUploadActivity.this.hideLoadingOverlay();
                            ImgurUploadActivity.this.updateUploadButtonVisibility();
                        }
                    });
                }
            }
        }.start();
    }

    /* access modifiers changed from: private */
    public void uploadImage() {
        showLoadingOverlay();
        ArrayList<PostField> postFields = new ArrayList<>(1);
        postFields.add(new PostField("image", this.mBase64Data));
        CacheManager instance = CacheManager.getInstance(this);
        AnonymousClass5 r18 = r2;
        AnonymousClass5 r2 = new CacheRequest(General.uriFromString("https://api.imgur.com/3/image"), RedditAccountManager.getInstance(this).getDefaultAccount(), null, -500, 0, DownloadStrategyAlways.INSTANCE, -1, 1, true, postFields, false, false, this) {
            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                BugReportActivity.handleGlobalError((Context) ImgurUploadActivity.this, t);
            }

            /* access modifiers changed from: protected */
            public void onDownloadNecessary() {
            }

            /* access modifiers changed from: protected */
            public void onDownloadStarted() {
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer httpStatus, String readableMessage) {
                ImgurUploadActivity imgurUploadActivity = ImgurUploadActivity.this;
                General.showResultDialog(imgurUploadActivity, General.getGeneralErrorForFailure(imgurUploadActivity, type, t, httpStatus, this.url.toString()));
                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                    public void run() {
                        ImgurUploadActivity.this.hideLoadingOverlay();
                    }
                });
            }

            public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
                try {
                    result.join();
                    try {
                        JsonBufferedObject root = result.asObject();
                        if (root != null) {
                            if (!Boolean.TRUE.equals(root.getBoolean("success"))) {
                                onFailure(10, null, null, null);
                                return;
                            }
                            String id = root.getObject(DataSchemeDataSource.SCHEME_DATA).getString(TtmlNode.ATTR_ID);
                            StringBuilder sb = new StringBuilder();
                            sb.append("https://imgur.com/");
                            sb.append(id);
                            final Uri imageUri = Uri.parse(sb.toString());
                            AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                                public void run() {
                                    Intent resultIntent = new Intent();
                                    resultIntent.setData(imageUri);
                                    ImgurUploadActivity.this.setResult(0, resultIntent);
                                    ImgurUploadActivity.this.finish();
                                }
                            });
                            return;
                        }
                        throw new RuntimeException("Response root object is null");
                    } catch (Throwable t) {
                        onFailure(9, t, null, t.toString());
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            /* access modifiers changed from: protected */
            public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
            }

            /* access modifiers changed from: protected */
            public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
            }
        };
        instance.makeRequest(r18);
    }

    public void onBackPressed() {
        if (General.onBackPressed()) {
            super.onBackPressed();
        }
    }
}
