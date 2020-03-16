package org.quantumbadger.redreader.image;

import android.content.Context;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import org.apache.commons.lang3.StringEscapeUtils;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached;
import org.quantumbadger.redreader.common.Constants.FileType;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedArray;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedObject;
import org.quantumbadger.redreader.jsonwrap.JsonValue;

public final class ImgurAPI {

    public static class AlbumInfo {
        public final String description;
        public final String id;
        public final ArrayList<ImageInfo> images;
        public final String title;

        public AlbumInfo(String id2, String title2, String description2, ArrayList<ImageInfo> images2) {
            this.id = id2;
            this.title = title2;
            this.description = description2;
            this.images = new ArrayList<>(images2);
        }

        public static AlbumInfo parse(String id2, JsonBufferedObject object) throws IOException, InterruptedException {
            String title2 = object.getString("title");
            String description2 = object.getString("description");
            if (title2 != null) {
                title2 = StringEscapeUtils.unescapeHtml4(title2);
            }
            if (description2 != null) {
                description2 = StringEscapeUtils.unescapeHtml4(description2);
            }
            JsonBufferedArray imagesJson = object.getArray("images");
            ArrayList<ImageInfo> images2 = new ArrayList<>();
            Iterator it = imagesJson.iterator();
            while (it.hasNext()) {
                images2.add(ImageInfo.parseImgur(((JsonValue) it.next()).asObject()));
            }
            return new AlbumInfo(id2, title2, description2, images2);
        }

        public static AlbumInfo parseV3(String id2, JsonBufferedObject object) throws IOException, InterruptedException {
            String title2 = object.getString("title");
            String description2 = object.getString("description");
            if (title2 != null) {
                title2 = StringEscapeUtils.unescapeHtml4(title2);
            }
            if (description2 != null) {
                description2 = StringEscapeUtils.unescapeHtml4(description2);
            }
            JsonBufferedArray imagesJson = object.getArray("images");
            ArrayList<ImageInfo> images2 = new ArrayList<>();
            Iterator it = imagesJson.iterator();
            while (it.hasNext()) {
                images2.add(ImageInfo.parseImgurV3(((JsonValue) it.next()).asObject()));
            }
            return new AlbumInfo(id2, title2, description2, images2);
        }
    }

    public static void getAlbumInfo(Context context, String albumId, int priority, int listId, GetAlbumInfoListener listener) {
        StringBuilder sb = new StringBuilder();
        sb.append("https://api.imgur.com/2/album/");
        sb.append(albumId);
        sb.append(".json");
        String apiUrl = sb.toString();
        AnonymousClass1 r16 = r2;
        final GetAlbumInfoListener getAlbumInfoListener = listener;
        String str = apiUrl;
        CacheManager instance = CacheManager.getInstance(context);
        final String str2 = albumId;
        AnonymousClass1 r2 = new CacheRequest(General.uriFromString(apiUrl), RedditAccountManager.getAnon(), null, priority, listId, DownloadStrategyIfNotCached.INSTANCE, FileType.IMAGE_INFO, 2, true, false, context) {
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
                    getAlbumInfoListener.onSuccess(AlbumInfo.parse(str2, result.asObject().getObject("album")));
                } catch (Throwable t) {
                    getAlbumInfoListener.onFailure(6, t, null, "Imgur data parse failed");
                }
            }
        };
        instance.makeRequest(r16);
    }

    public static void getImageInfo(Context context, String imageId, int priority, int listId, GetImageInfoListener listener) {
        StringBuilder sb = new StringBuilder();
        sb.append("https://api.imgur.com/2/image/");
        sb.append(imageId);
        sb.append(".json");
        String apiUrl = sb.toString();
        CacheManager instance = CacheManager.getInstance(context);
        String str = apiUrl;
        AnonymousClass2 r0 = r3;
        final GetImageInfoListener getImageInfoListener = listener;
        AnonymousClass2 r3 = new CacheRequest(General.uriFromString(apiUrl), RedditAccountManager.getAnon(), null, priority, listId, DownloadStrategyIfNotCached.INSTANCE, FileType.IMAGE_INFO, 2, true, false, context) {
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
                    getImageInfoListener.onSuccess(ImageInfo.parseImgur(result.asObject().getObject("image")));
                } catch (Throwable t) {
                    getImageInfoListener.onFailure(6, t, null, "Imgur data parse failed");
                }
            }
        };
        instance.makeRequest(r0);
    }
}
