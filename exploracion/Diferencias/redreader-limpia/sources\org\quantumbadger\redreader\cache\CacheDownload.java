package org.quantumbadger.redreader.cache;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.cache.CacheManager.WritableCacheFile;
import org.quantumbadger.redreader.cache.CachingInputStream.BytesReadListener;
import org.quantumbadger.redreader.common.PrioritisedCachedThreadPool.Task;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.common.TorCommon;
import org.quantumbadger.redreader.http.HTTPBackend;
import org.quantumbadger.redreader.http.HTTPBackend.Listener;
import org.quantumbadger.redreader.http.HTTPBackend.Request;
import org.quantumbadger.redreader.http.HTTPBackend.RequestDetails;
import org.quantumbadger.redreader.jsonwrap.JsonValue;
import org.quantumbadger.redreader.reddit.api.RedditOAuth;
import org.quantumbadger.redreader.reddit.api.RedditOAuth.AccessToken;
import org.quantumbadger.redreader.reddit.api.RedditOAuth.FetchAccessTokenResult;
import org.quantumbadger.redreader.reddit.api.RedditOAuth.FetchAccessTokenResultStatus;

public final class CacheDownload extends Task {
    private static final AtomicBoolean resetUserCredentials = new AtomicBoolean(false);
    private volatile boolean mCancelled = false;
    /* access modifiers changed from: private */
    public final CacheRequest mInitiator;
    /* access modifiers changed from: private */
    public final Request mRequest;
    /* access modifiers changed from: private */
    public final CacheManager manager;
    /* access modifiers changed from: private */
    public final UUID session;

    public CacheDownload(CacheRequest initiator, CacheManager manager2, PrioritisedDownloadQueue queue) {
        this.mInitiator = initiator;
        this.manager = manager2;
        if (!initiator.setDownload(this)) {
            this.mCancelled = true;
        }
        if (initiator.requestSession != null) {
            this.session = initiator.requestSession;
        } else {
            this.session = UUID.randomUUID();
        }
        this.mRequest = HTTPBackend.getBackend().prepareRequest(initiator.context, new RequestDetails(this.mInitiator.url, this.mInitiator.postFields));
    }

    public synchronized void cancel() {
        this.mCancelled = true;
        new Thread() {
            public void run() {
                if (CacheDownload.this.mRequest != null) {
                    CacheDownload.this.mRequest.cancel();
                    CacheDownload.this.mInitiator.notifyFailure(4, null, null, "Cancelled");
                }
            }
        }.start();
    }

    public void doDownload() {
        if (!this.mCancelled) {
            try {
                performDownload(this.mRequest);
            } catch (Throwable t) {
                BugReportActivity.handleGlobalError(this.mInitiator.context, t);
            }
        }
    }

    public static void resetUserCredentialsOnNextRequest() {
        resetUserCredentials.set(true);
    }

    private void performDownload(Request request) {
        FetchAccessTokenResult result;
        if (this.mInitiator.queueType == 0) {
            if (resetUserCredentials.getAndSet(false)) {
                this.mInitiator.user.setAccessToken(null);
            }
            AccessToken accessToken = this.mInitiator.user.getMostRecentAccessToken();
            if (accessToken == null || accessToken.isExpired()) {
                this.mInitiator.notifyProgress(true, 0, 0);
                if (this.mInitiator.user.isAnonymous()) {
                    result = RedditOAuth.fetchAnonymousAccessTokenSynchronous(this.mInitiator.context);
                } else {
                    result = RedditOAuth.fetchAccessTokenSynchronous(this.mInitiator.context, this.mInitiator.user.refreshToken);
                }
                if (result.status != FetchAccessTokenResultStatus.SUCCESS) {
                    CacheRequest cacheRequest = this.mInitiator;
                    Throwable th = result.error.t;
                    Integer num = result.error.httpStatus;
                    StringBuilder sb = new StringBuilder();
                    sb.append(result.error.title);
                    sb.append(": ");
                    sb.append(result.error.message);
                    cacheRequest.notifyFailure(1, th, num, sb.toString());
                    return;
                }
                accessToken = result.accessToken;
                this.mInitiator.user.setAccessToken(accessToken);
            }
            StringBuilder sb2 = new StringBuilder();
            sb2.append("bearer ");
            sb2.append(accessToken.token);
            request.addHeader("Authorization", sb2.toString());
        }
        if (this.mInitiator.queueType == 1) {
            request.addHeader("Authorization", "Client-ID c3713d9e7674477");
        }
        this.mInitiator.notifyDownloadStarted();
        request.executeInThisThread(new Listener() {
            public void onError(int failureType, Throwable exception, Integer httpStatus) {
                if (CacheDownload.this.mInitiator.queueType == 0 && TorCommon.isTorEnabled()) {
                    HTTPBackend.getBackend().recreateHttpBackend();
                    CacheDownload.resetUserCredentialsOnNextRequest();
                }
                CacheDownload.this.mInitiator.notifyFailure(failureType, exception, httpStatus, "");
            }

            /* JADX WARNING: Code restructure failed: missing block: B:64:0x0185, code lost:
                r0 = e;
             */
            /* JADX WARNING: Code restructure failed: missing block: B:75:0x01b1, code lost:
                r0 = e;
             */
            /* JADX WARNING: Code restructure failed: missing block: B:76:0x01b3, code lost:
                r0 = move-exception;
             */
            /* JADX WARNING: Code restructure failed: missing block: B:77:0x01b4, code lost:
                r0.printStackTrace();
                org.quantumbadger.redreader.cache.CacheDownload.access$100(r1.this$0).notifyFailure(0, r0, null, "The connection was interrupted");
             */
            /* JADX WARNING: Code restructure failed: missing block: B:78:0x01c4, code lost:
                r0 = e;
             */
            /* JADX WARNING: Code restructure failed: missing block: B:79:0x01c5, code lost:
                r9 = 2;
             */
            /* JADX WARNING: Failed to process nested try/catch */
            /* JADX WARNING: Removed duplicated region for block: B:13:0x0047  */
            /* JADX WARNING: Removed duplicated region for block: B:14:0x0049  */
            /* JADX WARNING: Removed duplicated region for block: B:76:0x01b3 A[ExcHandler: Throwable (r0v11 't' java.lang.Throwable A[CUSTOM_DECLARE]), Splitter:B:50:0x012f] */
            public void onSuccess(String mimetype, Long bodyBytes, InputStream is) {
                WritableCacheFile cacheFile;
                NotifyOutputStream cacheOs;
                int i;
                byte[] buf;
                int bytesRead;
                InputStream bis;
                int failureType;
                final Long l = bodyBytes;
                InputStream inputStream = is;
                if (CacheDownload.this.mInitiator.cache) {
                    try {
                        try {
                            WritableCacheFile cacheFile2 = CacheDownload.this.manager.openNewCacheFile(CacheDownload.this.mInitiator, CacheDownload.this.session, mimetype);
                            cacheOs = cacheFile2.getOutputStream();
                            cacheFile = cacheFile2;
                        } catch (IOException e) {
                            e = e;
                            e.printStackTrace();
                            if (!CacheDownload.this.manager.getPreferredCacheLocation().exists()) {
                            }
                            CacheDownload.this.mInitiator.notifyFailure(failureType, e, null, "Could not access the local cache");
                            return;
                        }
                    } catch (IOException e2) {
                        e = e2;
                        String str = mimetype;
                        e.printStackTrace();
                        if (!CacheDownload.this.manager.getPreferredCacheLocation().exists()) {
                            failureType = 2;
                        } else {
                            failureType = 11;
                        }
                        CacheDownload.this.mInitiator.notifyFailure(failureType, e, null, "Could not access the local cache");
                        return;
                    }
                } else {
                    String str2 = mimetype;
                    cacheOs = null;
                    cacheFile = null;
                }
                if (CacheDownload.this.mInitiator.isJson) {
                    if (CacheDownload.this.mInitiator.cache) {
                        bis = new BufferedInputStream(new CachingInputStream(inputStream, cacheOs, new BytesReadListener() {
                            public void onBytesRead(long total) {
                                if (l != null) {
                                    CacheDownload.this.mInitiator.notifyProgress(false, total, l.longValue());
                                }
                            }
                        }), 65536);
                    } else {
                        bis = new BufferedInputStream(inputStream, 65536);
                    }
                    try {
                        JsonValue value = new JsonValue(bis);
                        CacheDownload.this.mInitiator.notifyJsonParseStarted(value, RRTime.utcCurrentTimeMillis(), CacheDownload.this.session, false);
                        value.buildInThisThread();
                        if (!CacheDownload.this.mInitiator.cache || cacheFile == null) {
                        } else {
                            try {
                                InputStream inputStream2 = bis;
                                try {
                                    CacheDownload.this.mInitiator.notifySuccess(cacheFile.getReadableCacheFile(), RRTime.utcCurrentTimeMillis(), CacheDownload.this.session, false, mimetype);
                                } catch (IOException e3) {
                                    e = e3;
                                }
                            } catch (IOException e4) {
                                e = e4;
                                InputStream inputStream3 = bis;
                                if (e.getMessage().contains("ENOSPC")) {
                                    CacheDownload.this.mInitiator.notifyFailure(7, e, null, "Out of disk space");
                                } else {
                                    CacheDownload.this.mInitiator.notifyFailure(2, e, null, "Cache file not found");
                                }
                            }
                        }
                    } catch (Throwable t) {
                        InputStream inputStream4 = bis;
                        t.printStackTrace();
                        CacheDownload.this.mInitiator.notifyFailure(6, t, null, "Error parsing the JSON stream");
                        return;
                    }
                } else if (!CacheDownload.this.mInitiator.cache) {
                    BugReportActivity.handleGlobalError(CacheDownload.this.mInitiator.context, "Cache disabled for non-JSON request");
                    return;
                } else {
                    try {
                        buf = new byte[65536];
                        long totalBytesRead = 0;
                        while (true) {
                            int read = inputStream.read(buf);
                            bytesRead = read;
                            if (read <= 0) {
                                break;
                            }
                            long totalBytesRead2 = totalBytesRead + ((long) bytesRead);
                            cacheOs.write(buf, 0, bytesRead);
                            if (l != null) {
                                CacheDownload.this.mInitiator.notifyProgress(false, totalBytesRead2, bodyBytes.longValue());
                            }
                            totalBytesRead = totalBytesRead2;
                        }
                        cacheOs.flush();
                        cacheOs.close();
                        int i2 = bytesRead;
                        byte[] bArr = buf;
                        CacheDownload.this.mInitiator.notifySuccess(cacheFile.getReadableCacheFile(), RRTime.utcCurrentTimeMillis(), CacheDownload.this.session, false, mimetype);
                    } catch (IOException e5) {
                        e = e5;
                        int i3 = bytesRead;
                        byte[] bArr2 = buf;
                    } catch (Throwable t2) {
                    }
                }
                if (e.getMessage() == null || !e.getMessage().contains("ENOSPC")) {
                    e.printStackTrace();
                    CacheDownload.this.mInitiator.notifyFailure(0, e, null, "The connection was interrupted");
                } else {
                    CacheDownload.this.mInitiator.notifyFailure(i, e, null, "Out of disk space");
                }
                if (e.getMessage().contains("ENOSPC")) {
                    CacheDownload.this.mInitiator.notifyFailure(7, e, null, "Out of disk space");
                } else {
                    i = 2;
                    CacheDownload.this.mInitiator.notifyFailure(2, e, null, "Cache file not found");
                }
            }
        });
    }

    public int getPrimaryPriority() {
        return this.mInitiator.priority;
    }

    public int getSecondaryPriority() {
        return this.mInitiator.listId;
    }

    public void run() {
        doDownload();
    }
}
