package org.quantumbadger.redreader.image;

import android.content.Context;
import java.util.UUID;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached;
import org.quantumbadger.redreader.common.Constants.FileType;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.jsonwrap.JsonValue;

public final class GfycatAPI {
    public static void getImageInfo(Context context, String imageId, int priority, int listId, GetImageInfoListener listener) {
        StringBuilder sb = new StringBuilder();
        sb.append("https://api.gfycat.com/v1/gfycats/");
        sb.append(imageId);
        String apiUrl = sb.toString();
        CacheManager instance = CacheManager.getInstance(context);
        String str = apiUrl;
        AnonymousClass1 r0 = r3;
        final GetImageInfoListener getImageInfoListener = listener;
        AnonymousClass1 r3 = new CacheRequest(General.uriFromString(apiUrl), RedditAccountManager.getAnon(), null, priority, listId, DownloadStrategyIfNotCached.INSTANCE, FileType.IMAGE_INFO, 2, true, false, context) {
            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                BugReportActivity.handleGlobalError(this.context, t);
            }

            /* access modifiers changed from: protected */
            public void onDownloadNecessary() {
            }

            /* access modifiers changed from: protected */
            public void onDownloadStarted() {
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                getImageInfoListener.onFailure(type, t, status, readableMessage);
            }

            /* access modifiers changed from: protected */
            public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
            }

            /* access modifiers changed from: protected */
            public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
            }

            public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
                try {
                    getImageInfoListener.onSuccess(ImageInfo.parseGfycat(result.asObject().getObject("gfyItem")));
                } catch (Throwable t) {
                    getImageInfoListener.onFailure(6, t, null, "Gfycat data parse failed");
                }
            }
        };
        instance.makeRequest(r0);
    }
}
