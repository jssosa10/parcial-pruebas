package org.quantumbadger.redreader.reddit;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import com.google.android.exoplayer2.upstream.DataSchemeDataSource;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.activities.SessionChangeListener;
import org.quantumbadger.redreader.activities.SessionChangeListener.SessionChangeType;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategy;
import org.quantumbadger.redreader.common.Constants.Priority;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.fragments.CommentListingFragment;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedArray;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedObject;
import org.quantumbadger.redreader.jsonwrap.JsonValue;
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager;
import org.quantumbadger.redreader.reddit.prepared.RedditParsedComment;
import org.quantumbadger.redreader.reddit.prepared.RedditParsedPost;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;
import org.quantumbadger.redreader.reddit.prepared.RedditRenderableComment;
import org.quantumbadger.redreader.reddit.things.RedditComment;
import org.quantumbadger.redreader.reddit.things.RedditPost;
import org.quantumbadger.redreader.reddit.things.RedditThing;
import org.quantumbadger.redreader.reddit.things.RedditThing.Kind;
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;

public class CommentListingRequest {
    /* access modifiers changed from: private */
    public static final Event[] EVENT_TYPES = Event.values();
    /* access modifiers changed from: private */
    public final AppCompatActivity mActivity;
    /* access modifiers changed from: private */
    public final CacheManager mCacheManager;
    private final RedditURL mCommentListingURL;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public final DownloadStrategy mDownloadStrategy;
    private final Handler mEventHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            switch (AnonymousClass2.$SwitchMap$org$quantumbadger$redreader$reddit$CommentListingRequest$Event[CommentListingRequest.EVENT_TYPES[msg.what].ordinal()]) {
                case 1:
                    CommentListingRequest.this.mListener.onCommentListingRequestDownloadNecessary();
                    return;
                case 2:
                    CommentListingRequest.this.mListener.onCommentListingRequestDownloadStarted();
                    return;
                case 3:
                    CommentListingRequest.this.mListener.onCommentListingRequestException((Throwable) msg.obj);
                    return;
                case 4:
                    CommentListingRequest.this.mListener.onCommentListingRequestFailure((RRError) msg.obj);
                    return;
                case 5:
                    CommentListingRequest.this.mListener.onCommentListingRequestCachedCopy(((Long) msg.obj).longValue());
                    return;
                case 6:
                    CommentListingRequest.this.mListener.onCommentListingRequestAuthorizing();
                    return;
                case 7:
                    CommentListingRequest.this.mListener.onCommentListingRequestParseStart();
                    return;
                case 8:
                    CommentListingRequest.this.mListener.onCommentListingRequestPostDownloaded((RedditPreparedPost) msg.obj);
                    return;
                case 9:
                    CommentListingRequest.this.mListener.onCommentListingRequestAllItemsDownloaded((ArrayList) msg.obj);
                    return;
                default:
                    throw new RuntimeException("Unknown event type");
            }
        }
    };
    private final CommentListingFragment mFragment;
    /* access modifiers changed from: private */
    public final Listener mListener;
    /* access modifiers changed from: private */
    public final boolean mParsePostSelfText;
    /* access modifiers changed from: private */
    public final UUID mSession;
    /* access modifiers changed from: private */
    public final RedditURL mUrl;
    /* access modifiers changed from: private */
    public final RedditAccount mUser;

    /* renamed from: org.quantumbadger.redreader.reddit.CommentListingRequest$2 reason: invalid class name */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$org$quantumbadger$redreader$reddit$CommentListingRequest$Event = new int[Event.values().length];

        static {
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$CommentListingRequest$Event[Event.EVENT_DOWNLOAD_NECESSARY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$CommentListingRequest$Event[Event.EVENT_DOWNLOAD_STARTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$CommentListingRequest$Event[Event.EVENT_EXCEPTION.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$CommentListingRequest$Event[Event.EVENT_FAILURE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$CommentListingRequest$Event[Event.EVENT_CACHED_COPY.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$CommentListingRequest$Event[Event.EVENT_AUTHORIZING.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$CommentListingRequest$Event[Event.EVENT_PARSE_START.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$CommentListingRequest$Event[Event.EVENT_POST_DOWNLOADED.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$CommentListingRequest$Event[Event.EVENT_ALL_ITEMS_DOWNLOADED.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
        }
    }

    private class CommentListingCacheRequest extends CacheRequest {
        protected CommentListingCacheRequest() {
            super(General.uriFromString(CommentListingRequest.this.mUrl.generateJsonUri().toString()), CommentListingRequest.this.mUser, CommentListingRequest.this.mSession, Priority.API_COMMENT_LIST, 0, CommentListingRequest.this.mDownloadStrategy, 120, 0, true, false, CommentListingRequest.this.mContext);
        }

        /* access modifiers changed from: protected */
        public void onCallbackException(Throwable t) {
            CommentListingRequest.this.notifyListener(Event.EVENT_EXCEPTION, t);
        }

        /* access modifiers changed from: protected */
        public void onDownloadNecessary() {
            CommentListingRequest.this.notifyListener(Event.EVENT_DOWNLOAD_NECESSARY);
        }

        /* access modifiers changed from: protected */
        public void onDownloadStarted() {
            CommentListingRequest.this.notifyListener(Event.EVENT_DOWNLOAD_STARTED);
        }

        /* access modifiers changed from: protected */
        public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
            CommentListingRequest.this.notifyListener(Event.EVENT_FAILURE, General.getGeneralErrorForFailure(this.context, type, t, status, this.url.toString()));
        }

        /* access modifiers changed from: protected */
        public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
            if (authorizationInProgress) {
                CommentListingRequest.this.notifyListener(Event.EVENT_AUTHORIZING);
            }
        }

        /* access modifiers changed from: protected */
        public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
        }

        public void onJsonParseStarted(JsonValue value, long timestamp, UUID session, boolean fromCache) {
            int i;
            JsonBufferedObject thing;
            long j = timestamp;
            String parentPostAuthor = null;
            if (CommentListingRequest.this.mActivity instanceof SessionChangeListener) {
                ((SessionChangeListener) CommentListingRequest.this.mActivity).onSessionChanged(session, SessionChangeType.COMMENTS, j);
            } else {
                UUID uuid = session;
            }
            Integer minimumCommentScore = PrefsUtility.pref_behaviour_comment_min(CommentListingRequest.this.mContext, PreferenceManager.getDefaultSharedPreferences(this.context));
            if (fromCache) {
                CommentListingRequest.this.notifyListener(Event.EVENT_CACHED_COPY, Long.valueOf(timestamp));
            }
            CommentListingRequest.this.notifyListener(Event.EVENT_PARSE_START);
            try {
                if (value.getType() == 1) {
                    JsonBufferedArray root = value.asArray();
                    JsonBufferedObject thing2 = root.get(0).asObject();
                    JsonBufferedObject listing = thing2.getObject(DataSchemeDataSource.SCHEME_DATA);
                    JsonBufferedArray postContainer = listing.getArray("children");
                    RedditPost post = ((RedditThing) postContainer.getObject(0, RedditThing.class)).asPost();
                    RedditParsedPost parsedPost = new RedditParsedPost(post, CommentListingRequest.this.mParsePostSelfText);
                    RedditPost redditPost = post;
                    JsonBufferedObject jsonBufferedObject = listing;
                    JsonBufferedArray jsonBufferedArray = postContainer;
                    JsonBufferedObject jsonBufferedObject2 = thing2;
                    JsonBufferedArray jsonBufferedArray2 = root;
                    i = 1;
                    RedditPreparedPost redditPreparedPost = new RedditPreparedPost(this.context, CommentListingRequest.this.mCacheManager, 0, parsedPost, timestamp, true, false);
                    CommentListingRequest.this.notifyListener(Event.EVENT_POST_DOWNLOADED, redditPreparedPost);
                    parentPostAuthor = parsedPost.getAuthor();
                } else {
                    i = 1;
                }
                if (value.getType() == i) {
                    thing = value.asArray().get(i).asObject();
                } else {
                    thing = value.asObject();
                }
                JsonBufferedArray topLevelComments = thing.getObject(DataSchemeDataSource.SCHEME_DATA).getArray("children");
                ArrayList arrayList = new ArrayList(200);
                Iterator it = topLevelComments.iterator();
                while (it.hasNext()) {
                    CommentListingRequest.this.buildCommentTree((JsonValue) it.next(), null, arrayList, minimumCommentScore, parentPostAuthor);
                }
                RedditChangeDataManager changeDataManager = RedditChangeDataManager.getInstance(CommentListingRequest.this.mUser);
                Iterator it2 = arrayList.iterator();
                while (it2.hasNext()) {
                    RedditCommentListItem item = (RedditCommentListItem) it2.next();
                    if (item.isComment()) {
                        changeDataManager.update(j, item.asComment().getParsedComment().getRawComment());
                    }
                }
                CommentListingRequest.this.notifyListener(Event.EVENT_ALL_ITEMS_DOWNLOADED, arrayList);
            } catch (Throwable t) {
                notifyFailure(6, t, null, "Parse failure");
            }
        }
    }

    private enum Event {
        EVENT_DOWNLOAD_NECESSARY,
        EVENT_DOWNLOAD_STARTED,
        EVENT_EXCEPTION,
        EVENT_FAILURE,
        EVENT_CACHED_COPY,
        EVENT_AUTHORIZING,
        EVENT_PARSE_START,
        EVENT_POST_DOWNLOADED,
        EVENT_ALL_ITEMS_DOWNLOADED
    }

    @UiThread
    public interface Listener {
        void onCommentListingRequestAllItemsDownloaded(ArrayList<RedditCommentListItem> arrayList);

        void onCommentListingRequestAuthorizing();

        void onCommentListingRequestCachedCopy(long j);

        void onCommentListingRequestDownloadNecessary();

        void onCommentListingRequestDownloadStarted();

        void onCommentListingRequestException(Throwable th);

        void onCommentListingRequestFailure(RRError rRError);

        void onCommentListingRequestParseStart();

        void onCommentListingRequestPostDownloaded(RedditPreparedPost redditPreparedPost);
    }

    public CommentListingRequest(Context context, CommentListingFragment fragment, AppCompatActivity activity, RedditURL commentListingURL, boolean parsePostSelfText, RedditURL url, RedditAccount user, UUID session, DownloadStrategy downloadStrategy, Listener listener) {
        this.mContext = context;
        this.mFragment = fragment;
        this.mActivity = activity;
        this.mCommentListingURL = commentListingURL;
        this.mParsePostSelfText = parsePostSelfText;
        this.mUrl = url;
        this.mUser = user;
        this.mSession = session;
        this.mDownloadStrategy = downloadStrategy;
        this.mListener = listener;
        this.mCacheManager = CacheManager.getInstance(context);
        this.mCacheManager.makeRequest(new CommentListingCacheRequest());
    }

    /* access modifiers changed from: private */
    public void buildCommentTree(JsonValue value, RedditCommentListItem parent, ArrayList<RedditCommentListItem> output, Integer minimumCommentScore, String parentPostAuthor) throws IOException, InterruptedException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        ArrayList<RedditCommentListItem> arrayList = output;
        RedditThing thing = (RedditThing) value.asObject(RedditThing.class);
        if (thing.getKind() == Kind.MORE_COMMENTS && this.mUrl.pathType() == 7) {
            RedditCommentListItem redditCommentListItem = new RedditCommentListItem(thing.asMoreComments(), parent, this.mFragment, this.mActivity, this.mCommentListingURL);
            arrayList.add(redditCommentListItem);
            Integer num = minimumCommentScore;
            String str = parentPostAuthor;
        } else if (thing.getKind() == Kind.COMMENT) {
            RedditComment comment = thing.asComment();
            RedditCommentListItem redditCommentListItem2 = parent;
            RedditCommentListItem redditCommentListItem3 = new RedditCommentListItem(new RedditRenderableComment(new RedditParsedComment(comment), parentPostAuthor, minimumCommentScore, true), redditCommentListItem2, this.mFragment, this.mActivity, this.mCommentListingURL);
            arrayList.add(redditCommentListItem3);
            if (comment.replies.getType() == 0) {
                Iterator it = comment.replies.asObject().getObject(DataSchemeDataSource.SCHEME_DATA).getArray("children").iterator();
                while (it.hasNext()) {
                    buildCommentTree((JsonValue) it.next(), redditCommentListItem3, output, minimumCommentScore, parentPostAuthor);
                }
            }
        } else {
            Integer num2 = minimumCommentScore;
            String str2 = parentPostAuthor;
        }
    }

    /* access modifiers changed from: private */
    public void notifyListener(Event eventType) {
        notifyListener(eventType, null);
    }

    /* access modifiers changed from: private */
    public void notifyListener(Event eventType, Object object) {
        Message message = Message.obtain();
        message.what = eventType.ordinal();
        message.obj = object;
        this.mEventHandler.sendMessage(message);
    }
}
