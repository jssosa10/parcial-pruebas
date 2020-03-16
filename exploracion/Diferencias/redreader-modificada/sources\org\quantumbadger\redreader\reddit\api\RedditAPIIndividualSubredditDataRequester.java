package org.quantumbadger.redreader.reddit.api;

import android.content.Context;
import android.util.Log;
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways;
import org.quantumbadger.redreader.common.Constants.Priority;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.TimestampBound;
import org.quantumbadger.redreader.io.CacheDataSource;
import org.quantumbadger.redreader.io.RequestResponseHandler;
import org.quantumbadger.redreader.jsonwrap.JsonValue;
import org.quantumbadger.redreader.reddit.RedditSubredditHistory;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit.InvalidSubredditNameException;
import org.quantumbadger.redreader.reddit.things.RedditThing;

public class RedditAPIIndividualSubredditDataRequester implements CacheDataSource<String, RedditSubreddit, SubredditRequestFailure> {
    private static final String TAG = "IndividualSRDataReq";
    private final Context context;
    /* access modifiers changed from: private */
    public final RedditAccount user;

    public RedditAPIIndividualSubredditDataRequester(Context context2, RedditAccount user2) {
        this.context = context2;
        this.user = user2;
    }

    public void performRequest(String subredditCanonicalName, TimestampBound timestampBound, RequestResponseHandler<RedditSubreddit, SubredditRequestFailure> handler) {
        StringBuilder sb = new StringBuilder();
        sb.append("/r/");
        final String str = subredditCanonicalName;
        sb.append(str);
        sb.append("/about.json");
        final RequestResponseHandler<RedditSubreddit, SubredditRequestFailure> requestResponseHandler = handler;
        AnonymousClass1 r0 = new CacheRequest(this, Reddit.getUri(sb.toString()), this.user, null, Priority.API_SUBREDDIT_INVIDIVUAL, 0, DownloadStrategyAlways.INSTANCE, 101, 0, true, false, this.context) {
            final /* synthetic */ RedditAPIIndividualSubredditDataRequester this$0;

            {
                this.this$0 = this$0;
            }

            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                RequestResponseHandler requestResponseHandler = requestResponseHandler;
                SubredditRequestFailure subredditRequestFailure = new SubredditRequestFailure(6, t, (Integer) null, "Parse error", this.url);
                requestResponseHandler.onRequestFailed(subredditRequestFailure);
            }

            /* access modifiers changed from: protected */
            public void onDownloadNecessary() {
            }

            /* access modifiers changed from: protected */
            public void onDownloadStarted() {
            }

            /* access modifiers changed from: protected */
            public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                RequestResponseHandler requestResponseHandler = requestResponseHandler;
                SubredditRequestFailure subredditRequestFailure = new SubredditRequestFailure(type, t, status, readableMessage, this.url);
                requestResponseHandler.onRequestFailed(subredditRequestFailure);
            }

            /* access modifiers changed from: protected */
            public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
            }

            public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
                try {
                    RedditSubreddit subreddit = ((RedditThing) result.asObject(RedditThing.class)).asSubreddit();
                    subreddit.downloadTime = timestamp;
                    requestResponseHandler.onRequestSuccess(subreddit, timestamp);
                    RedditSubredditHistory.addSubreddit(this.user, str);
                } catch (Exception e) {
                    Exception e2 = e;
                    RequestResponseHandler requestResponseHandler = requestResponseHandler;
                    SubredditRequestFailure subredditRequestFailure = new SubredditRequestFailure(6, (Throwable) e2, (Integer) null, "Parse error", this.url);
                    requestResponseHandler.onRequestFailed(subredditRequestFailure);
                }
            }
        };
        CacheManager.getInstance(this.context).makeRequest(r0);
    }

    public void performRequest(Collection<String> subredditCanonicalIds, TimestampBound timestampBound, RequestResponseHandler<HashMap<String, RedditSubreddit>, SubredditRequestFailure> handler) {
        final HashMap<String, RedditSubreddit> result = new HashMap<>();
        final AtomicBoolean stillOkay = new AtomicBoolean(true);
        final AtomicInteger requestsToGo = new AtomicInteger(subredditCanonicalIds.size());
        final AtomicLong oldestResult = new AtomicLong(Long.MAX_VALUE);
        final RequestResponseHandler<HashMap<String, RedditSubreddit>, SubredditRequestFailure> requestResponseHandler = handler;
        AnonymousClass2 r0 = new RequestResponseHandler<RedditSubreddit, SubredditRequestFailure>() {
            public void onRequestFailed(SubredditRequestFailure failureReason) {
                synchronized (result) {
                    if (stillOkay.get()) {
                        stillOkay.set(false);
                        requestResponseHandler.onRequestFailed(failureReason);
                    }
                }
            }

            public void onRequestSuccess(RedditSubreddit innerResult, long timeCached) {
                synchronized (result) {
                    if (stillOkay.get()) {
                        result.put(innerResult.getKey(), innerResult);
                        oldestResult.set(Math.min(oldestResult.get(), timeCached));
                        try {
                            RedditSubredditHistory.addSubreddit(RedditAPIIndividualSubredditDataRequester.this.user, innerResult.getCanonicalName());
                        } catch (InvalidSubredditNameException e) {
                            String str = RedditAPIIndividualSubredditDataRequester.TAG;
                            StringBuilder sb = new StringBuilder();
                            sb.append("Invalid subreddit name ");
                            sb.append(innerResult.name);
                            Log.e(str, sb.toString(), e);
                        }
                        if (requestsToGo.decrementAndGet() == 0) {
                            requestResponseHandler.onRequestSuccess(result, oldestResult.get());
                        }
                    }
                }
            }
        };
        for (String subredditCanonicalId : subredditCanonicalIds) {
            performRequest(subredditCanonicalId, timestampBound, (RequestResponseHandler<RedditSubreddit, SubredditRequestFailure>) r0);
        }
    }

    public void performWrite(RedditSubreddit value) {
        throw new UnsupportedOperationException();
    }

    public void performWrite(Collection<RedditSubreddit> collection) {
        throw new UnsupportedOperationException();
    }
}
