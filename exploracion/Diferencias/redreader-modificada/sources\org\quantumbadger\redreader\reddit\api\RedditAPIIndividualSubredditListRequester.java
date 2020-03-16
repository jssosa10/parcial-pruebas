package org.quantumbadger.redreader.reddit.api;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.util.Log;
import com.google.android.exoplayer2.upstream.DataSchemeDataSource;
import java.net.URI;
import java.util.ArrayList;
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
import org.quantumbadger.redreader.common.Constants.Priority;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.TimestampBound;
import org.quantumbadger.redreader.common.UnexpectedInternalStateException;
import org.quantumbadger.redreader.io.CacheDataSource;
import org.quantumbadger.redreader.io.RequestResponseHandler;
import org.quantumbadger.redreader.io.WritableHashSet;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedArray;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedObject;
import org.quantumbadger.redreader.jsonwrap.JsonValue;
import org.quantumbadger.redreader.reddit.RedditSubredditManager;
import org.quantumbadger.redreader.reddit.RedditSubredditManager.SubredditListType;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit.InvalidSubredditNameException;
import org.quantumbadger.redreader.reddit.things.RedditThing;

public class RedditAPIIndividualSubredditListRequester implements CacheDataSource<SubredditListType, WritableHashSet, SubredditRequestFailure> {
    private final Context context;
    private final RedditAccount user;

    public RedditAPIIndividualSubredditListRequester(Context context2, RedditAccount user2) {
        this.context = context2;
        this.user = user2;
    }

    public void performRequest(SubredditListType type, TimestampBound timestampBound, RequestResponseHandler<WritableHashSet, SubredditRequestFailure> handler) {
        if (type == SubredditListType.DEFAULTS) {
            long now = System.currentTimeMillis();
            HashSet<String> data = new HashSet<>(Reddit.DEFAULT_SUBREDDITS.length + 1);
            for (String name : Reddit.DEFAULT_SUBREDDITS) {
                data.add(General.asciiLowercase(name));
            }
            data.add("/r/redreader");
            handler.onRequestSuccess(new WritableHashSet(data, now, "DEFAULTS"), now);
            return;
        }
        if (type == SubredditListType.MOST_POPULAR) {
            doSubredditListRequest(SubredditListType.MOST_POPULAR, handler, null);
        } else if (this.user.isAnonymous()) {
            switch (type) {
                case SUBSCRIBED:
                    performRequest(SubredditListType.DEFAULTS, timestampBound, handler);
                    return;
                case MODERATED:
                    long curTime = System.currentTimeMillis();
                    handler.onRequestSuccess(new WritableHashSet(new HashSet<>(), curTime, SubredditListType.MODERATED.name()), curTime);
                    return;
                case MULTIREDDITS:
                    long curTime2 = System.currentTimeMillis();
                    handler.onRequestSuccess(new WritableHashSet(new HashSet<>(), curTime2, SubredditListType.MULTIREDDITS.name()), curTime2);
                    return;
                default:
                    StringBuilder sb = new StringBuilder();
                    sb.append("Internal error: unknown subreddit list type '");
                    sb.append(type.name());
                    sb.append("'");
                    throw new RuntimeException(sb.toString());
            }
        } else {
            doSubredditListRequest(type, handler, null);
        }
    }

    /* access modifiers changed from: private */
    public void doSubredditListRequest(SubredditListType type, RequestResponseHandler<WritableHashSet, SubredditRequestFailure> handler, String after) {
        URI uri;
        URI uri2;
        String str = after;
        int i = AnonymousClass2.$SwitchMap$org$quantumbadger$redreader$reddit$RedditSubredditManager$SubredditListType[type.ordinal()];
        if (i != 4) {
            switch (i) {
                case 1:
                    uri = Reddit.getUri(Reddit.PATH_SUBREDDITS_MINE_SUBSCRIBER);
                    break;
                case 2:
                    uri = Reddit.getUri(Reddit.PATH_SUBREDDITS_MINE_MODERATOR);
                    break;
                default:
                    throw new UnexpectedInternalStateException(type.name());
            }
        } else {
            uri = Reddit.getUri(Reddit.PATH_SUBREDDITS_POPULAR);
        }
        if (str != null) {
            Builder builder = Uri.parse(uri.toString()).buildUpon();
            builder.appendQueryParameter("after", str);
            uri2 = General.uriFromString(builder.toString());
        } else {
            uri2 = uri;
        }
        RedditAccount redditAccount = this.user;
        DownloadStrategyAlways downloadStrategyAlways = DownloadStrategyAlways.INSTANCE;
        Context context2 = this.context;
        final RequestResponseHandler<WritableHashSet, SubredditRequestFailure> requestResponseHandler = handler;
        final SubredditListType subredditListType = type;
        final String str2 = after;
        AnonymousClass1 r0 = new CacheRequest(this, uri2, redditAccount, null, Priority.API_SUBREDDIT_INVIDIVUAL, 0, downloadStrategyAlways, 100, 0, true, false, context2) {
            final /* synthetic */ RedditAPIIndividualSubredditListRequester this$0;

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
                long j = timestamp;
                try {
                    final HashSet hashSet = new HashSet();
                    ArrayList arrayList = new ArrayList();
                    JsonBufferedObject redditListing = result.asObject().getObject(DataSchemeDataSource.SCHEME_DATA);
                    JsonBufferedArray subreddits = redditListing.getArray("children");
                    if (subreddits.join() == 2) {
                        RequestResponseHandler requestResponseHandler = requestResponseHandler;
                        SubredditRequestFailure subredditRequestFailure = new SubredditRequestFailure(6, (Throwable) null, (Integer) null, "Unknown parse error", this.url.toString());
                        requestResponseHandler.onRequestFailed(subredditRequestFailure);
                    } else if (subredditListType == SubredditListType.SUBSCRIBED && subreddits.getCurrentItemCount() == 0 && str2 == null) {
                        this.this$0.performRequest(SubredditListType.DEFAULTS, TimestampBound.ANY, requestResponseHandler);
                    } else {
                        Iterator it = subreddits.iterator();
                        while (it.hasNext()) {
                            RedditSubreddit subreddit = ((RedditThing) ((JsonValue) it.next()).asObject(RedditThing.class)).asSubreddit();
                            subreddit.downloadTime = j;
                            try {
                                hashSet.add(subreddit.getCanonicalName());
                                arrayList.add(subreddit);
                            } catch (InvalidSubredditNameException e) {
                                Log.e("SubredditListRequester", "Ignoring invalid subreddit", e);
                            }
                        }
                        RedditSubredditManager.getInstance(this.context, this.user).offerRawSubredditData(arrayList, j);
                        String receivedAfter = redditListing.getString("after");
                        if (receivedAfter == null || subredditListType == SubredditListType.MOST_POPULAR) {
                            requestResponseHandler.onRequestSuccess(new WritableHashSet(hashSet, j, subredditListType.name()), j);
                            if (str2 == null) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("Got ");
                                sb.append(hashSet.size());
                                sb.append(" subreddits in 1 request");
                                Log.i("SubredditListRequester", sb.toString());
                            }
                        }
                        this.this$0.doSubredditListRequest(subredditListType, new RequestResponseHandler<WritableHashSet, SubredditRequestFailure>() {
                            public void onRequestFailed(SubredditRequestFailure failureReason) {
                                requestResponseHandler.onRequestFailed(failureReason);
                            }

                            public void onRequestSuccess(WritableHashSet result, long timeCached) {
                                hashSet.addAll(result.toHashset());
                                requestResponseHandler.onRequestSuccess(new WritableHashSet(hashSet, timeCached, subredditListType.name()), timeCached);
                                if (str2 == null) {
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("Got ");
                                    sb.append(hashSet.size());
                                    sb.append(" subreddits in multiple requests");
                                    Log.i("SubredditListRequester", sb.toString());
                                }
                            }
                        }, receivedAfter);
                    }
                } catch (Exception e2) {
                    Exception e3 = e2;
                    RequestResponseHandler requestResponseHandler2 = requestResponseHandler;
                    SubredditRequestFailure subredditRequestFailure2 = new SubredditRequestFailure(6, (Throwable) e3, (Integer) null, "Parse error", this.url.toString());
                    requestResponseHandler2.onRequestFailed(subredditRequestFailure2);
                }
            }
        };
        CacheManager.getInstance(this.context).makeRequest(r0);
    }

    public void performRequest(Collection<SubredditListType> collection, TimestampBound timestampBound, RequestResponseHandler<HashMap<SubredditListType, WritableHashSet>, SubredditRequestFailure> requestResponseHandler) {
        throw new UnsupportedOperationException();
    }

    public void performWrite(WritableHashSet value) {
        throw new UnsupportedOperationException();
    }

    public void performWrite(Collection<WritableHashSet> collection) {
        throw new UnsupportedOperationException();
    }
}
