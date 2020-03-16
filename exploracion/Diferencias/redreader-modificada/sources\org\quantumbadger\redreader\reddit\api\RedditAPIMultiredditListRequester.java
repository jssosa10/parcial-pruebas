package org.quantumbadger.redreader.reddit.api;

import android.content.Context;
import com.google.android.exoplayer2.upstream.DataSchemeDataSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.common.TimestampBound;
import org.quantumbadger.redreader.io.CacheDataSource;
import org.quantumbadger.redreader.io.RequestResponseHandler;
import org.quantumbadger.redreader.io.WritableHashSet;
import org.quantumbadger.redreader.jsonwrap.JsonValue;

public class RedditAPIMultiredditListRequester implements CacheDataSource<Key, WritableHashSet, SubredditRequestFailure> {
    private final Context context;
    private final RedditAccount user;

    public static class Key {
        public static final Key INSTANCE = new Key();

        private Key() {
        }
    }

    public RedditAPIMultiredditListRequester(Context context2, RedditAccount user2) {
        this.context = context2;
        this.user = user2;
    }

    public void performRequest(Key key, TimestampBound timestampBound, RequestResponseHandler<WritableHashSet, SubredditRequestFailure> handler) {
        if (this.user.isAnonymous()) {
            long now = RRTime.utcCurrentTimeMillis();
            handler.onRequestSuccess(new WritableHashSet(new HashSet<>(), now, this.user.getCanonicalUsername()), now);
            return;
        }
        doRequest(handler);
    }

    private void doRequest(RequestResponseHandler<WritableHashSet, SubredditRequestFailure> handler) {
        final RequestResponseHandler<WritableHashSet, SubredditRequestFailure> requestResponseHandler = handler;
        AnonymousClass1 r0 = new CacheRequest(this, Reddit.getUri(Reddit.PATH_MULTIREDDITS_MINE), this.user, null, -100, 0, DownloadStrategyAlways.INSTANCE, 102, 0, true, false, this.context) {
            final /* synthetic */ RedditAPIMultiredditListRequester this$0;

            {
                this.this$0 = this$0;
            }

            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                RequestResponseHandler requestResponseHandler = requestResponseHandler;
                SubredditRequestFailure subredditRequestFailure = new SubredditRequestFailure(6, t, (Integer) null, "Internal error", this.url);
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
                SubredditRequestFailure subredditRequestFailure = new SubredditRequestFailure(type, t, status, readableMessage, this.url.toString());
                requestResponseHandler.onRequestFailed(subredditRequestFailure);
            }

            /* access modifiers changed from: protected */
            public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
            }

            public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
                try {
                    HashSet<String> output = new HashSet<>();
                    Iterator it = result.asArray().iterator();
                    while (it.hasNext()) {
                        output.add(((JsonValue) it.next()).asObject().getObject(DataSchemeDataSource.SCHEME_DATA).getString("name"));
                    }
                    requestResponseHandler.onRequestSuccess(new WritableHashSet(output, timestamp, this.user.getCanonicalUsername()), timestamp);
                } catch (Exception e) {
                    Exception e2 = e;
                    RequestResponseHandler requestResponseHandler = requestResponseHandler;
                    SubredditRequestFailure subredditRequestFailure = new SubredditRequestFailure(6, (Throwable) e2, (Integer) null, "Parse error", this.url.toString());
                    requestResponseHandler.onRequestFailed(subredditRequestFailure);
                }
            }
        };
        CacheManager.getInstance(this.context).makeRequest(r0);
    }

    public void performRequest(Collection<Key> collection, TimestampBound timestampBound, RequestResponseHandler<HashMap<Key, WritableHashSet>, SubredditRequestFailure> requestResponseHandler) {
        throw new UnsupportedOperationException();
    }

    public void performWrite(WritableHashSet value) {
        throw new UnsupportedOperationException();
    }

    public void performWrite(Collection<WritableHashSet> collection) {
        throw new UnsupportedOperationException();
    }
}
