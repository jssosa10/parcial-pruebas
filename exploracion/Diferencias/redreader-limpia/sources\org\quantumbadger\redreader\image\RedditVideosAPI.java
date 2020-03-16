package org.quantumbadger.redreader.image;

import android.content.Context;
import android.util.Log;
import com.google.android.exoplayer2.util.MimeTypes;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached;
import org.quantumbadger.redreader.common.Constants.FileType;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.image.ImageInfo.HasAudio;
import org.quantumbadger.redreader.image.ImageInfo.MediaType;

public final class RedditVideosAPI {
    /* access modifiers changed from: private */
    public static final String[] PREFERRED_VIDEO_FORMATS = {"DASH_480", "DASH_2_4_M", "DASH_360", "DASH_1_2_M", "DASH_720", "DASH_4_8_M", "DASH_240", "DASH_600_K", "DASH_1080", "DASH_9_6_M"};

    public static void getImageInfo(Context context, String imageId, int priority, int listId, GetImageInfoListener listener) {
        StringBuilder sb = new StringBuilder();
        sb.append("https://v.redd.it/");
        sb.append(imageId);
        sb.append("/DASHPlaylist.mpd");
        String apiUrl = sb.toString();
        AnonymousClass1 r17 = r2;
        final GetImageInfoListener getImageInfoListener = listener;
        CacheManager instance = CacheManager.getInstance(context);
        final String str = imageId;
        final String str2 = apiUrl;
        AnonymousClass1 r2 = new CacheRequest(General.uriFromString(apiUrl), RedditAccountManager.getAnon(), null, priority, listId, DownloadStrategyIfNotCached.INSTANCE, FileType.IMAGE_INFO, 2, false, false, context) {
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
                InputStream is;
                ImageInfo result;
                try {
                    is = cacheFile.getInputStream();
                    String mpd = General.readWholeStreamAsUTF8(is);
                    String videoUrl = null;
                    String audioUrl = null;
                    if (mpd.contains(MimeTypes.BASE_TYPE_AUDIO)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("https://v.redd.it/");
                        sb.append(str);
                        sb.append("/audio");
                        audioUrl = sb.toString();
                    }
                    String[] access$000 = RedditVideosAPI.PREFERRED_VIDEO_FORMATS;
                    int length = access$000.length;
                    int i = 0;
                    while (true) {
                        if (i >= length) {
                            break;
                        }
                        String format = access$000[i];
                        if (mpd.contains(format)) {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("https://v.redd.it/");
                            sb2.append(str);
                            sb2.append("/");
                            sb2.append(format);
                            videoUrl = sb2.toString();
                            break;
                        }
                        i++;
                    }
                    if (videoUrl == null) {
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("https://v.redd.it/");
                        sb3.append(str);
                        sb3.append("/DASH_480");
                        videoUrl = sb3.toString();
                    }
                    if (audioUrl != null) {
                        result = new ImageInfo(videoUrl, audioUrl, MediaType.VIDEO, HasAudio.HAS_AUDIO);
                    } else {
                        result = new ImageInfo(videoUrl, MediaType.VIDEO, HasAudio.NO_AUDIO);
                    }
                    String str = "RedditVideosAPI";
                    Locale locale = Locale.US;
                    String str2 = "For '%s', got video stream '%s' and audio stream '%s'";
                    Object[] objArr = new Object[3];
                    objArr[0] = str2;
                    objArr[1] = videoUrl;
                    objArr[2] = audioUrl == null ? "null" : audioUrl;
                    Log.i(str, String.format(locale, str2, objArr));
                    getImageInfoListener.onSuccess(result);
                    General.closeSafely(is);
                } catch (IOException e) {
                    getImageInfoListener.onFailure(2, e, null, "Failed to read mpd");
                } catch (Throwable th) {
                    General.closeSafely(is);
                    throw th;
                }
            }
        };
        instance.makeRequest(r17);
    }
}
