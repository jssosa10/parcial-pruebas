package org.quantumbadger.redreader.cache;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategy;
import org.quantumbadger.redreader.common.PrioritisedCachedThreadPool;
import org.quantumbadger.redreader.common.PrioritisedCachedThreadPool.Task;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.http.HTTPBackend.PostField;
import org.quantumbadger.redreader.jsonwrap.JsonValue;

public abstract class CacheRequest implements Comparable<CacheRequest> {
    public static final int DOWNLOAD_QUEUE_IMAGE_PRECACHE = 3;
    public static final int DOWNLOAD_QUEUE_IMGUR_API = 1;
    public static final int DOWNLOAD_QUEUE_IMMEDIATE = 2;
    public static final int DOWNLOAD_QUEUE_REDDIT_API = 0;
    private static final PrioritisedCachedThreadPool JSON_NOTIFY_THREADS = new PrioritisedCachedThreadPool(2, "JSON notify");
    public static final int REQUEST_FAILURE_CACHE_DIR_DOES_NOT_EXIST = 11;
    public static final int REQUEST_FAILURE_CACHE_MISS = 3;
    public static final int REQUEST_FAILURE_CANCELLED = 4;
    public static final int REQUEST_FAILURE_CONNECTION = 0;
    public static final int REQUEST_FAILURE_DISK_SPACE = 7;
    public static final int REQUEST_FAILURE_MALFORMED_URL = 5;
    public static final int REQUEST_FAILURE_PARSE = 6;
    public static final int REQUEST_FAILURE_PARSE_IMGUR = 9;
    public static final int REQUEST_FAILURE_REDDIT_REDIRECT = 8;
    public static final int REQUEST_FAILURE_REQUEST = 1;
    public static final int REQUEST_FAILURE_STORAGE = 2;
    public static final int REQUEST_FAILURE_UPLOAD_FAIL_IMGUR = 10;
    public final boolean cache;
    private boolean cancelled;
    public final Context context;
    private CacheDownload download;
    @NonNull
    public final DownloadStrategy downloadStrategy;
    public final int fileType;
    public final boolean isJson;
    public final int listId;
    public final List<PostField> postFields;
    public final int priority;
    public final int queueType;
    public final UUID requestSession;
    public final URI url;
    public final RedditAccount user;

    @Retention(RetentionPolicy.SOURCE)
    public @interface DownloadQueueType {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RequestFailureType {
    }

    /* access modifiers changed from: protected */
    public abstract void onCallbackException(Throwable th);

    /* access modifiers changed from: protected */
    public abstract void onDownloadNecessary();

    /* access modifiers changed from: protected */
    public abstract void onDownloadStarted();

    /* access modifiers changed from: protected */
    public abstract void onFailure(int i, Throwable th, Integer num, String str);

    /* access modifiers changed from: protected */
    public abstract void onProgress(boolean z, long j, long j2);

    /* access modifiers changed from: protected */
    public abstract void onSuccess(ReadableCacheFile readableCacheFile, long j, UUID uuid, boolean z, String str);

    /* access modifiers changed from: 0000 */
    public synchronized boolean setDownload(CacheDownload download2) {
        if (this.cancelled) {
            return false;
        }
        this.download = download2;
        return true;
    }

    public synchronized void cancel() {
        this.cancelled = true;
        if (this.download != null) {
            this.download.cancel();
            this.download = null;
        }
    }

    protected CacheRequest(URI url2, RedditAccount user2, UUID requestSession2, int priority2, int listId2, @NonNull DownloadStrategy downloadStrategy2, int fileType2, int queueType2, boolean isJson2, boolean cancelExisting, Context context2) {
        this(url2, user2, requestSession2, priority2, listId2, downloadStrategy2, fileType2, queueType2, isJson2, null, true, cancelExisting, context2);
    }

    protected CacheRequest(URI url2, RedditAccount user2, UUID requestSession2, int priority2, int listId2, @NonNull DownloadStrategy downloadStrategy2, int fileType2, int queueType2, boolean isJson2, List<PostField> postFields2, boolean cache2, boolean cancelExisting, Context context2) {
        URI uri = url2;
        RedditAccount redditAccount = user2;
        boolean z = isJson2;
        List<PostField> list = postFields2;
        boolean z2 = cache2;
        this.context = context2;
        if (redditAccount == null) {
            UUID uuid = requestSession2;
            int i = priority2;
            int i2 = listId2;
            DownloadStrategy downloadStrategy3 = downloadStrategy2;
            int i3 = fileType2;
            int i4 = queueType2;
            throw new NullPointerException("User was null - set to empty string for anonymous");
        } else if (!downloadStrategy2.shouldDownloadWithoutCheckingCache() && list != null) {
            throw new IllegalArgumentException("Should not perform cache lookup for POST requests");
        } else if (!z && list != null) {
            throw new IllegalArgumentException("POST requests must be for JSON values");
        } else if (z2 && list != null) {
            throw new IllegalArgumentException("Cannot cache a POST request");
        } else if (z2 || z) {
            this.url = uri;
            this.user = redditAccount;
            this.requestSession = requestSession2;
            this.priority = priority2;
            this.listId = listId2;
            this.downloadStrategy = downloadStrategy2;
            this.fileType = fileType2;
            this.queueType = queueType2;
            this.isJson = z;
            this.postFields = list;
            this.cache = z2;
            if (uri == null) {
                notifyFailure(5, null, null, "Malformed URL");
                cancel();
            }
        } else {
            throw new IllegalArgumentException("Must cache non-JSON requests");
        }
    }

    public final boolean isHigherPriorityThan(CacheRequest another) {
        int i = this.priority;
        int i2 = another.priority;
        boolean z = true;
        if (i != i2) {
            if (i >= i2) {
                z = false;
            }
            return z;
        }
        if (this.listId >= another.listId) {
            z = false;
        }
        return z;
    }

    public int compareTo(CacheRequest another) {
        if (isHigherPriorityThan(another)) {
            return -1;
        }
        return another.isHigherPriorityThan(this) ? 1 : 0;
    }

    public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
        throw new RuntimeException("CacheRequest method has not been overridden");
    }

    public final void notifyFailure(int type, Throwable t, Integer httpStatus, String readableMessage) {
        try {
            onFailure(type, t, httpStatus, readableMessage);
        } catch (Throwable t2) {
            Log.e("CacheRequest", "Exception thrown by onCallbackException", t2);
            BugReportActivity.addGlobalError(new RRError(null, null, t1));
            BugReportActivity.handleGlobalError(this.context, t2);
        }
    }

    public final void notifyProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
        try {
            onProgress(authorizationInProgress, bytesRead, totalBytes);
        } catch (Throwable t2) {
            Log.e("CacheRequest", "Exception thrown by onCallbackException", t2);
            BugReportActivity.addGlobalError(new RRError(null, null, t1));
            BugReportActivity.handleGlobalError(this.context, t2);
        }
    }

    public final void notifySuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
        try {
            onSuccess(cacheFile, timestamp, session, fromCache, mimetype);
        } catch (Throwable t2) {
            Log.e("CacheRequest", "Exception thrown by onCallbackException", t2);
            BugReportActivity.addGlobalError(new RRError(null, null, t1));
            BugReportActivity.handleGlobalError(this.context, t2);
        }
    }

    public final void notifyJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
        PrioritisedCachedThreadPool prioritisedCachedThreadPool = JSON_NOTIFY_THREADS;
        final JsonValue jsonValue = result;
        final long j = timestamp;
        final UUID uuid = session;
        final boolean z = fromCache;
        AnonymousClass1 r1 = new Task() {
            public int getPrimaryPriority() {
                return CacheRequest.this.priority;
            }

            public int getSecondaryPriority() {
                return CacheRequest.this.listId;
            }

            public void run() {
                try {
                    CacheRequest.this.onJsonParseStarted(jsonValue, j, uuid, z);
                } catch (Throwable t2) {
                    Log.e("CacheRequest", "Exception thrown by onCallbackException", t2);
                    BugReportActivity.addGlobalError(new RRError(null, null, t1));
                    BugReportActivity.handleGlobalError(CacheRequest.this.context, t2);
                }
            }
        };
        prioritisedCachedThreadPool.add(r1);
    }

    public final void notifyDownloadNecessary() {
        try {
            onDownloadNecessary();
        } catch (Throwable t2) {
            Log.e("CacheRequest", "Exception thrown by onCallbackException", t2);
            BugReportActivity.addGlobalError(new RRError(null, null, t1));
            BugReportActivity.handleGlobalError(this.context, t2);
        }
    }

    public final void notifyDownloadStarted() {
        try {
            onDownloadStarted();
        } catch (Throwable t2) {
            Log.e("CacheRequest", "Exception thrown by onCallbackException", t2);
            BugReportActivity.addGlobalError(new RRError(null, null, t1));
            BugReportActivity.handleGlobalError(this.context, t2);
        }
    }
}
