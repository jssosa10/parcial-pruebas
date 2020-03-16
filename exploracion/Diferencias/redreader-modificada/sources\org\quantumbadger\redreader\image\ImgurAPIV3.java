package org.quantumbadger.redreader.image;

import android.content.Context;
import com.google.android.exoplayer2.upstream.DataSchemeDataSource;
import java.net.URI;
import java.util.UUID;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached;
import org.quantumbadger.redreader.common.Constants.FileType;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.image.ImgurAPI.AlbumInfo;
import org.quantumbadger.redreader.jsonwrap.JsonValue;

public final class ImgurAPIV3 {
    public static void getAlbumInfo(Context context, String albumId, int priority, int listId, boolean withAuth, GetAlbumInfoListener listener) {
        StringBuilder sb = new StringBuilder();
        sb.append("https://api.imgur.com/3/album/");
        sb.append(albumId);
        String apiUrl = sb.toString();
        CacheManager instance = CacheManager.getInstance(context);
        URI uriFromString = General.uriFromString(apiUrl);
        AnonymousClass1 r16 = r2;
        final GetAlbumInfoListener getAlbumInfoListener = listener;
        String str = apiUrl;
        CacheManager cacheManager = instance;
        final String str2 = albumId;
        AnonymousClass1 r2 = new CacheRequest(uriFromString, RedditAccountManager.getAnon(), null, priority, listId, DownloadStrategyIfNotCached.INSTANCE, FileType.IMAGE_INFO, withAuth ? 1 : 2, true, false, context) {
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
                getAlbumInfoListener.onFailure(type, t, status, readableMessage);
            }

            /* access modifiers changed from: protected */
            public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
            }

            /* access modifiers changed from: protected */
            public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
            }

            public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
                try {
                    getAlbumInfoListener.onSuccess(AlbumInfo.parseV3(str2, result.asObject().getObject(DataSchemeDataSource.SCHEME_DATA)));
                } catch (Throwable t) {
                    getAlbumInfoListener.onFailure(6, t, null, "Imgur data parse failed");
                }
            }
        };
        cacheManager.makeRequest(r16);
    }

    public static void getImageInfo(Context context, String imageId, int priority, int listId, boolean withAuth, GetImageInfoListener listener) {
        StringBuilder sb = new StringBuilder();
        sb.append("https://api.imgur.com/3/image/");
        sb.append(imageId);
        String apiUrl = sb.toString();
        CacheManager instance = CacheManager.getInstance(context);
        URI uriFromString = General.uriFromString(apiUrl);
        String str = apiUrl;
        AnonymousClass2 r0 = r3;
        final GetImageInfoListener getImageInfoListener = listener;
        AnonymousClass2 r3 = new CacheRequest(uriFromString, RedditAccountManager.getAnon(), null, priority, listId, DownloadStrategyIfNotCached.INSTANCE, FileType.IMAGE_INFO, withAuth ? 1 : 2, true, false, context) {
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
                    getImageInfoListener.onSuccess(ImageInfo.parseImgurV3(result.asObject().getObject(DataSchemeDataSource.SCHEME_DATA)));
                } catch (Throwable t) {
                    getImageInfoListener.onFailure(6, t, null, "Imgur data parse failed");
                }
            }
        };
        instance.makeRequest(r0);
    }
}
