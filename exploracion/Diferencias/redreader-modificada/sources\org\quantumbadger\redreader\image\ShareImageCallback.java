package org.quantumbadger.redreader.image;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.BaseActivity.PermissionCallback;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached;
import org.quantumbadger.redreader.common.Constants.FileType;
import org.quantumbadger.redreader.common.Constants.Priority;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;

public class ShareImageCallback implements PermissionCallback {
    /* access modifiers changed from: private */
    public final AppCompatActivity activity;
    /* access modifiers changed from: private */
    public final String uri;

    public ShareImageCallback(AppCompatActivity activity2, String uri2) {
        this.activity = activity2;
        this.uri = uri2;
    }

    public void onPermissionGranted() {
        final RedditAccount anon = RedditAccountManager.getAnon();
        LinkHandler.getImageInfo(this.activity, this.uri, Priority.IMAGE_VIEW, 0, new GetImageInfoListener() {
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                General.showResultDialog(ShareImageCallback.this.activity, General.getGeneralErrorForFailure(ShareImageCallback.this.activity, type, t, status, ShareImageCallback.this.uri));
            }

            public void onSuccess(ImageInfo info2) {
                CacheManager instance = CacheManager.getInstance(ShareImageCallback.this.activity);
                AnonymousClass1 r14 = r0;
                final ImageInfo imageInfo = info2;
                AnonymousClass1 r0 = new CacheRequest(this, General.uriFromString(info2.urlOriginal), anon, null, Priority.IMAGE_VIEW, 0, DownloadStrategyIfNotCached.INSTANCE, FileType.IMAGE, 2, false, false, ShareImageCallback.this.activity) {
                    final /* synthetic */ AnonymousClass1 this$1;

                    {
                        this.this$1 = this$1;
                    }

                    /* access modifiers changed from: protected */
                    public void onCallbackException(Throwable t) {
                        BugReportActivity.handleGlobalError(this.context, t);
                    }

                    /* access modifiers changed from: protected */
                    public void onDownloadNecessary() {
                        General.quickToast(this.context, (int) R.string.download_downloading);
                    }

                    /* access modifiers changed from: protected */
                    public void onDownloadStarted() {
                    }

                    /* access modifiers changed from: protected */
                    public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                        General.showResultDialog(ShareImageCallback.this.activity, General.getGeneralErrorForFailure(this.context, type, t, status, this.url.toString()));
                    }

                    /* access modifiers changed from: protected */
                    public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
                    }

                    /* access modifiers changed from: protected */
                    public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
                        String filename = General.filenameFromString(imageInfo.urlOriginal);
                        File dst = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), filename);
                        if (dst.exists()) {
                            int count = 0;
                            while (dst.exists()) {
                                count++;
                                File externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                                StringBuilder sb = new StringBuilder();
                                sb.append(count);
                                sb.append("_");
                                sb.append(filename.substring(1));
                                dst = new File(externalStoragePublicDirectory, sb.toString());
                            }
                        }
                        Uri sharedImage = FileProvider.getUriForFile(this.context, "org.quantumbadger.redreader.provider", dst);
                        try {
                            InputStream cacheFileInputStream = cacheFile.getInputStream();
                            if (cacheFileInputStream == null) {
                                notifyFailure(3, null, null, "Could not find cached image");
                                return;
                            }
                            General.copyFile(cacheFileInputStream, dst);
                            Intent shareIntent = new Intent();
                            shareIntent.setAction("android.intent.action.SEND");
                            shareIntent.putExtra("android.intent.extra.STREAM", sharedImage);
                            shareIntent.setType(mimetype);
                            ShareImageCallback.this.activity.startActivity(Intent.createChooser(shareIntent, ShareImageCallback.this.activity.getString(R.string.action_share_image)));
                            ShareImageCallback.this.activity.sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", sharedImage));
                            Context context = this.context;
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append(this.context.getString(R.string.action_save_image_success));
                            sb2.append(StringUtils.SPACE);
                            sb2.append(dst.getAbsolutePath());
                            General.quickToast(context, sb2.toString());
                        } catch (IOException e) {
                            notifyFailure(2, e, null, "Could not copy file");
                        }
                    }
                };
                instance.makeRequest(r14);
            }

            public void onNotAnImage() {
                General.quickToast((Context) ShareImageCallback.this.activity, (int) R.string.selected_link_is_not_image);
            }
        });
    }

    public void onPermissionDenied() {
        General.quickToast((Context) this.activity, (int) R.string.save_image_permission_denied);
    }
}
