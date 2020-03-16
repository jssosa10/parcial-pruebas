package org.quantumbadger.redreader.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.exoplayer2.upstream.DataSchemeDataSource;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang3.StringEscapeUtils;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuPostsListener;
import org.quantumbadger.redreader.activities.SessionChangeListener;
import org.quantumbadger.redreader.activities.SessionChangeListener.SessionChangeType;
import org.quantumbadger.redreader.adapters.PostListingManager;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategy;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfTimestampOutsideBounds;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyNever;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.Constants.FileType;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceThumbnailsShow;
import org.quantumbadger.redreader.common.PrefsUtility.CachePrecacheComments;
import org.quantumbadger.redreader.common.PrefsUtility.CachePrecacheImages;
import org.quantumbadger.redreader.common.PrefsUtility.GifViewMode;
import org.quantumbadger.redreader.common.PrefsUtility.ImageViewMode;
import org.quantumbadger.redreader.common.PrefsUtility.VideoViewMode;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.common.TimestampBound;
import org.quantumbadger.redreader.image.GetImageInfoListener;
import org.quantumbadger.redreader.image.ImageInfo;
import org.quantumbadger.redreader.image.ImageInfo.MediaType;
import org.quantumbadger.redreader.io.RequestResponseHandler;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedArray;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedObject;
import org.quantumbadger.redreader.jsonwrap.JsonValue;
import org.quantumbadger.redreader.listingcontrollers.CommentListingController;
import org.quantumbadger.redreader.reddit.PostSort;
import org.quantumbadger.redreader.reddit.RedditPostListItem;
import org.quantumbadger.redreader.reddit.RedditSubredditManager;
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager;
import org.quantumbadger.redreader.reddit.api.SubredditRequestFailure;
import org.quantumbadger.redreader.reddit.prepared.RedditParsedPost;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;
import org.quantumbadger.redreader.reddit.things.RedditPost;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit.InvalidSubredditNameException;
import org.quantumbadger.redreader.reddit.things.RedditThing;
import org.quantumbadger.redreader.reddit.things.RedditThing.Kind;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL;
import org.quantumbadger.redreader.reddit.url.PostListingURL;
import org.quantumbadger.redreader.reddit.url.RedditURLParser;
import org.quantumbadger.redreader.reddit.url.SearchPostListURL;
import org.quantumbadger.redreader.reddit.url.SubredditPostListURL;
import org.quantumbadger.redreader.reddit.url.SubredditPostListURL.Type;
import org.quantumbadger.redreader.views.PostListingHeader;
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener;
import org.quantumbadger.redreader.views.ScrollbarRecyclerViewManager;
import org.quantumbadger.redreader.views.SearchListingHeader;
import org.quantumbadger.redreader.views.liststatus.ErrorView;

public class PostListingFragment extends RRFragment implements PostSelectionListener {
    private static final String SAVEDSTATE_FIRST_VISIBLE_POS = "firstVisiblePosition";
    private static final String TAG = "PostListingFragment";
    /* access modifiers changed from: private */
    public String mAfter = null;
    private String mLastAfter = null;
    /* access modifiers changed from: private */
    public TextView mLoadMoreView;
    private final View mOuter;
    /* access modifiers changed from: private */
    public int mPostCount = 0;
    private final int mPostCountLimit;
    /* access modifiers changed from: private */
    public final HashSet<String> mPostIds = new HashSet<>(200);
    /* access modifiers changed from: private */
    public final PostListingManager mPostListingManager;
    /* access modifiers changed from: private */
    public PostListingURL mPostListingURL;
    /* access modifiers changed from: private */
    public final AtomicInteger mPostRefreshCount = new AtomicInteger(0);
    private Integer mPreviousFirstVisibleItemPosition;
    /* access modifiers changed from: private */
    public boolean mReadyToDownloadMore = false;
    private final RecyclerView mRecyclerView;
    /* access modifiers changed from: private */
    public CacheRequest mRequest;
    /* access modifiers changed from: private */
    public UUID mSession;
    /* access modifiers changed from: private */
    public final SharedPreferences mSharedPreferences;
    /* access modifiers changed from: private */
    public RedditSubreddit mSubreddit;
    /* access modifiers changed from: private */
    public long mTimestamp;

    private class PostListingRequest extends CacheRequest {
        private final boolean firstDownload;
        final /* synthetic */ PostListingFragment this$0;

        protected PostListingRequest(PostListingFragment postListingFragment, Uri url, RedditAccount user, UUID requestSession, DownloadStrategy downloadStrategy, boolean firstDownload2) {
            this.this$0 = postListingFragment;
            super(General.uriFromString(url.toString()), user, requestSession, -200, 0, downloadStrategy, 110, 0, true, false, postListingFragment.getActivity());
            this.firstDownload = firstDownload2;
        }

        /* access modifiers changed from: protected */
        public void onDownloadNecessary() {
        }

        /* access modifiers changed from: protected */
        public void onDownloadStarted() {
        }

        /* access modifiers changed from: protected */
        public void onCallbackException(Throwable t) {
            BugReportActivity.handleGlobalError(this.context, t);
        }

        /* access modifiers changed from: protected */
        public void onFailure(final int type, final Throwable t, final Integer status, String readableMessage) {
            AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                public void run() {
                    RRError error;
                    PostListingRequest.this.this$0.mPostListingManager.setLoadingVisible(false);
                    if (type == 3) {
                        error = new RRError(PostListingRequest.this.context.getString(R.string.error_postlist_cache_title), PostListingRequest.this.context.getString(R.string.error_postlist_cache_message), t, status, PostListingRequest.this.url.toString());
                    } else {
                        error = General.getGeneralErrorForFailure(PostListingRequest.this.context, type, t, status, PostListingRequest.this.url.toString());
                    }
                    PostListingRequest.this.this$0.mPostListingManager.addFooterError(new ErrorView(PostListingRequest.this.this$0.getActivity(), error));
                }
            });
        }

        /* access modifiers changed from: protected */
        public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
        }

        /* access modifiers changed from: protected */
        public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
        }

        /* JADX WARNING: type inference failed for: r4v0, types: [org.quantumbadger.redreader.fragments.PostListingFragment$PostListingRequest] */
        /* JADX WARNING: type inference failed for: r15v1 */
        /* JADX WARNING: type inference failed for: r1v45, types: [java.util.List] */
        /* JADX WARNING: type inference failed for: r4v2 */
        /* JADX WARNING: type inference failed for: r15v2, types: [org.quantumbadger.redreader.fragments.PostListingFragment$PostListingRequest] */
        /* JADX WARNING: type inference failed for: r4v3, types: [java.util.List] */
        /* JADX WARNING: type inference failed for: r23v0 */
        /* JADX WARNING: type inference failed for: r4v4, types: [org.quantumbadger.redreader.fragments.PostListingFragment$PostListingRequest] */
        /* JADX WARNING: type inference failed for: r15v3 */
        /* JADX WARNING: type inference failed for: r4v5 */
        /* JADX WARNING: type inference failed for: r23v1 */
        /* JADX WARNING: type inference failed for: r4v6 */
        /* JADX WARNING: type inference failed for: r15v4 */
        /* JADX WARNING: type inference failed for: r4v7 */
        /* JADX WARNING: type inference failed for: r23v2 */
        /* JADX WARNING: type inference failed for: r4v8 */
        /* JADX WARNING: type inference failed for: r23v3 */
        /* JADX WARNING: type inference failed for: r4v9 */
        /* JADX WARNING: type inference failed for: r23v5 */
        /* JADX WARNING: type inference failed for: r4v10 */
        /* JADX WARNING: type inference failed for: r4v11 */
        /* JADX WARNING: type inference failed for: r4v12 */
        /* JADX WARNING: type inference failed for: r4v13 */
        /* JADX WARNING: type inference failed for: r4v14, types: [org.quantumbadger.redreader.fragments.PostListingFragment$PostListingRequest] */
        /* JADX WARNING: type inference failed for: r23v6 */
        /* JADX WARNING: type inference failed for: r4v15 */
        /* JADX WARNING: type inference failed for: r24v8, types: [org.quantumbadger.redreader.account.RedditAccount] */
        /* JADX WARNING: type inference failed for: r23v10 */
        /* JADX WARNING: type inference failed for: r4v16, types: [org.quantumbadger.redreader.account.RedditAccount] */
        /* JADX WARNING: type inference failed for: r4v17 */
        /* JADX WARNING: type inference failed for: r23v11 */
        /* JADX WARNING: type inference failed for: r4v18 */
        /* JADX WARNING: type inference failed for: r15v6 */
        /* JADX WARNING: type inference failed for: r4v19 */
        /* JADX WARNING: type inference failed for: r15v7 */
        /* JADX WARNING: type inference failed for: r15v8 */
        /* JADX WARNING: type inference failed for: r15v9 */
        /* JADX WARNING: type inference failed for: r15v10 */
        /* JADX WARNING: type inference failed for: r15v11 */
        /* JADX WARNING: type inference failed for: r15v12 */
        /* JADX WARNING: type inference failed for: r15v13 */
        /* JADX WARNING: type inference failed for: r15v14 */
        /* JADX WARNING: type inference failed for: r15v15 */
        /* JADX WARNING: type inference failed for: r4v21 */
        /* JADX WARNING: type inference failed for: r4v22 */
        /* JADX WARNING: type inference failed for: r4v23 */
        /* JADX WARNING: type inference failed for: r15v16 */
        /* JADX WARNING: type inference failed for: r4v24 */
        /* JADX WARNING: type inference failed for: r23v12 */
        /* JADX WARNING: type inference failed for: r4v25 */
        /* JADX WARNING: type inference failed for: r4v26 */
        /* JADX WARNING: type inference failed for: r4v27 */
        /* JADX WARNING: type inference failed for: r4v28 */
        /* JADX WARNING: type inference failed for: r4v29 */
        /* JADX WARNING: type inference failed for: r4v30 */
        /* JADX WARNING: type inference failed for: r4v31 */
        /* JADX WARNING: type inference failed for: r4v32 */
        /* JADX WARNING: Code restructure failed: missing block: B:28:0x00ba, code lost:
            if (r35 != false) goto L_0x00bc;
         */
        /* JADX WARNING: Multi-variable type inference failed. Error: jadx.core.utils.exceptions.JadxRuntimeException: No candidate types for var: r15v3
  assigns: []
  uses: []
  mth insns count: 408
        	at jadx.core.dex.visitors.typeinference.TypeSearch.fillTypeCandidates(TypeSearch.java:237)
        	at java.util.ArrayList.forEach(Unknown Source)
        	at jadx.core.dex.visitors.typeinference.TypeSearch.run(TypeSearch.java:53)
        	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runMultiVariableSearch(TypeInferenceVisitor.java:99)
        	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:92)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(Unknown Source)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$0(DepthTraversal.java:13)
        	at java.util.ArrayList.forEach(Unknown Source)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:13)
        	at jadx.core.ProcessClass.process(ProcessClass.java:30)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:49)
        	at java.util.ArrayList.forEach(Unknown Source)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:49)
        	at jadx.core.ProcessClass.process(ProcessClass.java:35)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:311)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:217)
         */
        /* JADX WARNING: Removed duplicated region for block: B:109:0x0309 A[SYNTHETIC, Splitter:B:109:0x0309] */
        /* JADX WARNING: Removed duplicated region for block: B:114:0x031c A[SYNTHETIC, Splitter:B:114:0x031c] */
        /* JADX WARNING: Removed duplicated region for block: B:123:0x03b3  */
        /* JADX WARNING: Removed duplicated region for block: B:138:0x0433 A[Catch:{ Throwable -> 0x0415, Throwable -> 0x0481 }] */
        /* JADX WARNING: Removed duplicated region for block: B:25:0x00b6 A[SYNTHETIC, Splitter:B:25:0x00b6] */
        /* JADX WARNING: Removed duplicated region for block: B:32:0x00c2 A[Catch:{ Throwable -> 0x0483 }] */
        /* JADX WARNING: Removed duplicated region for block: B:36:0x00d0 A[SYNTHETIC, Splitter:B:36:0x00d0] */
        /* JADX WARNING: Removed duplicated region for block: B:46:0x012d A[SYNTHETIC, Splitter:B:46:0x012d] */
        /* JADX WARNING: Removed duplicated region for block: B:59:0x01a2  */
        /* JADX WARNING: Removed duplicated region for block: B:60:0x01a6  */
        /* JADX WARNING: Removed duplicated region for block: B:64:0x01cb  */
        /* JADX WARNING: Removed duplicated region for block: B:65:0x01cf  */
        /* JADX WARNING: Removed duplicated region for block: B:69:0x01f2 A[SYNTHETIC, Splitter:B:69:0x01f2] */
        /* JADX WARNING: Removed duplicated region for block: B:80:0x023e A[Catch:{ Throwable -> 0x0483 }] */
        /* JADX WARNING: Removed duplicated region for block: B:91:0x0294 A[SYNTHETIC, Splitter:B:91:0x0294] */
        /* JADX WARNING: Unknown variable types count: 17 */
        public void onJsonParseStarted(JsonValue value, long timestamp, UUID session, boolean fromCache) {
            ? r4;
            boolean z;
            CachePrecacheImages imagePrecachePref;
            CachePrecacheComments commentPrecachePref;
            boolean z2;
            boolean z3;
            boolean z4;
            boolean showSubredditName;
            Iterator it;
            ? r15;
            ? r42;
            boolean isPostBlocked;
            JsonBufferedObject listing;
            AppearanceThumbnailsShow thumbnailsPref;
            CachePrecacheImages imagePrecachePref2;
            CachePrecacheComments commentPrecachePref2;
            ? r23;
            AppCompatActivity activity;
            ? r43;
            boolean leftHandedMode;
            ArrayList arrayList;
            boolean downloadThisThumbnail;
            boolean leftHandedMode2;
            ArrayList arrayList2;
            RedditPreparedPost preparedPost;
            AppCompatActivity activity2;
            RedditPost post;
            ? r232;
            ? r44;
            ? r45;
            ? r46;
            ? r47;
            ? r152 = this;
            final long j = timestamp;
            UUID uuid = session;
            AppCompatActivity activity3 = r152.this$0.getActivity();
            if (r152.firstDownload && fromCache && RRTime.since(timestamp) > 600000) {
                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                    public void run() {
                        TextView cacheNotif = (TextView) LayoutInflater.from(PostListingRequest.this.this$0.getActivity()).inflate(R.layout.cached_header, null, false);
                        cacheNotif.setText(PostListingRequest.this.this$0.getActivity().getString(R.string.listing_cached, new Object[]{RRTime.formatDateTime(j, PostListingRequest.this.this$0.getActivity())}));
                        PostListingRequest.this.this$0.mPostListingManager.addNotification(cacheNotif);
                    }
                });
            }
            if (r152.firstDownload) {
                ((SessionChangeListener) activity3).onSessionChanged(uuid, SessionChangeType.POSTS, j);
                r152.this$0.mSession = uuid;
                r152.this$0.mTimestamp = j;
            }
            try {
                JsonBufferedObject thing = value.asObject();
                JsonBufferedObject listing2 = thing.getObject(DataSchemeDataSource.SCHEME_DATA);
                JsonBufferedArray posts = listing2.getArray("children");
                boolean isNsfwAllowed = PrefsUtility.pref_behaviour_nsfw(activity3, r152.this$0.mSharedPreferences);
                boolean hideReadPosts = PrefsUtility.pref_behaviour_hide_read_posts(activity3, r152.this$0.mSharedPreferences);
                boolean isConnectionWifi = General.isConnectionWifi(activity3);
                AppearanceThumbnailsShow thumbnailsPref2 = PrefsUtility.appearance_thumbnails_show(activity3, r152.this$0.mSharedPreferences);
                if (thumbnailsPref2 != AppearanceThumbnailsShow.ALWAYS) {
                    try {
                        if (thumbnailsPref2 != AppearanceThumbnailsShow.WIFIONLY || !isConnectionWifi) {
                            z = false;
                            boolean downloadThumbnails = z;
                            boolean showNsfwThumbnails = PrefsUtility.appearance_thumbnails_nsfw_show(activity3, r152.this$0.mSharedPreferences);
                            imagePrecachePref = PrefsUtility.cache_precache_images(activity3, r152.this$0.mSharedPreferences);
                            commentPrecachePref = PrefsUtility.cache_precache_comments(activity3, r152.this$0.mSharedPreferences);
                            if (imagePrecachePref != CachePrecacheImages.ALWAYS) {
                                if (imagePrecachePref == CachePrecacheImages.WIFIONLY) {
                                }
                                z2 = false;
                                boolean precacheImages = z2;
                                if (commentPrecachePref != CachePrecacheComments.ALWAYS) {
                                    if (commentPrecachePref != CachePrecacheComments.WIFIONLY || !isConnectionWifi) {
                                        z3 = false;
                                        boolean precacheComments = z3;
                                        final ImageViewMode imageViewMode = PrefsUtility.pref_behaviour_imageview_mode(activity3, r152.this$0.mSharedPreferences);
                                        final GifViewMode gifViewMode = PrefsUtility.pref_behaviour_gifview_mode(activity3, r152.this$0.mSharedPreferences);
                                        final VideoViewMode videoViewMode = PrefsUtility.pref_behaviour_videoview_mode(activity3, r152.this$0.mSharedPreferences);
                                        boolean leftHandedMode3 = PrefsUtility.pref_appearance_left_handed(activity3, r152.this$0.mSharedPreferences);
                                        if (r152.this$0.mPostListingURL.pathType() == 0) {
                                            if (r152.this$0.mPostListingURL.asSubredditPostListURL().type == Type.ALL || r152.this$0.mPostListingURL.asSubredditPostListURL().type == Type.ALL_SUBTRACTION || r152.this$0.mPostListingURL.asSubredditPostListURL().type == Type.POPULAR) {
                                                z4 = true;
                                                boolean subredditFilteringEnabled = z4;
                                                ? pref_blocked_subreddits = PrefsUtility.pref_blocked_subreddits(activity3, r152.this$0.mSharedPreferences);
                                                String str = PostListingFragment.TAG;
                                                StringBuilder sb = new StringBuilder();
                                                sb.append("Precaching images: ");
                                                sb.append(!precacheImages ? OrbotHelper.STATUS_ON : OrbotHelper.STATUS_OFF);
                                                Log.i(str, sb.toString());
                                                String str2 = PostListingFragment.TAG;
                                                StringBuilder sb2 = new StringBuilder();
                                                sb2.append("Precaching comments: ");
                                                sb2.append(!precacheComments ? OrbotHelper.STATUS_ON : OrbotHelper.STATUS_OFF);
                                                Log.i(str2, sb2.toString());
                                                CacheManager cm = CacheManager.getInstance(activity3);
                                                if (r152.this$0.mPostListingURL != null) {
                                                    if (r152.this$0.mPostListingURL.pathType() == 0 && r152.this$0.mPostListingURL.asSubredditPostListURL().type == Type.SUBREDDIT) {
                                                        showSubredditName = false;
                                                        ArrayList arrayList3 = new ArrayList(25);
                                                        it = posts.iterator();
                                                        while (it.hasNext()) {
                                                            JsonValue postThingValue = (JsonValue) it.next();
                                                            RedditThing postThing = (RedditThing) postThingValue.asObject(RedditThing.class);
                                                            if (!postThing.getKind().equals(Kind.POST)) {
                                                                r15 = r152;
                                                                r42 = pref_blocked_subreddits;
                                                            } else {
                                                                RedditPost post2 = postThing.asPost();
                                                                JsonBufferedObject thing2 = thing;
                                                                r152.this$0.mAfter = post2.name;
                                                                if (subredditFilteringEnabled) {
                                                                    r152 = r152;
                                                                    if (r152.this$0.getIsPostBlocked(pref_blocked_subreddits, post2)) {
                                                                        isPostBlocked = true;
                                                                        if (isPostBlocked) {
                                                                            r152 = r152;
                                                                            if (post2.over_18) {
                                                                                if (!isNsfwAllowed) {
                                                                                    arrayList = arrayList3;
                                                                                    r23 = pref_blocked_subreddits;
                                                                                    leftHandedMode = leftHandedMode3;
                                                                                    commentPrecachePref2 = commentPrecachePref;
                                                                                    imagePrecachePref2 = imagePrecachePref;
                                                                                    thumbnailsPref = thumbnailsPref2;
                                                                                    listing = listing2;
                                                                                    activity = activity3;
                                                                                    r43 = r152;
                                                                                }
                                                                            }
                                                                            boolean z5 = isPostBlocked;
                                                                            if (r152.this$0.mPostIds.add(post2.getIdAlone())) {
                                                                                if (downloadThumbnails) {
                                                                                    r152 = r152;
                                                                                    r152 = r152;
                                                                                    if (!post2.over_18 || showNsfwThumbnails) {
                                                                                        downloadThisThumbnail = true;
                                                                                        r152 = r152;
                                                                                        int positionInList = r152.this$0.mPostCount;
                                                                                        CachePrecacheComments commentPrecachePref3 = commentPrecachePref;
                                                                                        RedditParsedPost parsedPost = new RedditParsedPost(post2, false);
                                                                                        RedditPreparedPost redditPreparedPost = new RedditPreparedPost(activity3, cm, positionInList, parsedPost, timestamp, showSubredditName, downloadThisThumbnail);
                                                                                        r152 = r152;
                                                                                        if (hideReadPosts) {
                                                                                            r152 = r152;
                                                                                            if (redditPreparedPost.isRead()) {
                                                                                                commentPrecachePref = commentPrecachePref3;
                                                                                                thing = thing2;
                                                                                                r15 = r152;
                                                                                                r42 = pref_blocked_subreddits;
                                                                                            }
                                                                                        }
                                                                                        if (!precacheComments) {
                                                                                            try {
                                                                                                JsonValue postThingValue2 = postThingValue;
                                                                                                CommentListingController controller = new CommentListingController(PostCommentListingURL.forPostId(redditPreparedPost.src.getIdAlone()), activity3);
                                                                                                CacheManager instance = CacheManager.getInstance(activity3);
                                                                                                preparedPost = redditPreparedPost;
                                                                                                r1 = r1;
                                                                                                AnonymousClass3 r52 = r1;
                                                                                                JsonValue jsonValue = postThingValue2;
                                                                                                arrayList2 = arrayList3;
                                                                                                URI uriFromString = General.uriFromString(controller.getUri().toString());
                                                                                                r232 = pref_blocked_subreddits;
                                                                                                ? defaultAccount = RedditAccountManager.getInstance(activity3).getDefaultAccount();
                                                                                                leftHandedMode2 = leftHandedMode3;
                                                                                                CacheManager cacheManager = instance;
                                                                                                commentPrecachePref2 = commentPrecachePref3;
                                                                                                imagePrecachePref2 = imagePrecachePref;
                                                                                                post = post2;
                                                                                                thumbnailsPref = thumbnailsPref2;
                                                                                                listing = listing2;
                                                                                                activity2 = activity3;
                                                                                                try {
                                                                                                    AnonymousClass3 r1 = new CacheRequest(uriFromString, defaultAccount, null, 500, positionInList, DownloadStrategyIfNotCached.INSTANCE, 120, 0, false, false, activity2) {
                                                                                                        /* access modifiers changed from: protected */
                                                                                                        public void onCallbackException(Throwable t) {
                                                                                                        }

                                                                                                        /* access modifiers changed from: protected */
                                                                                                        public void onDownloadNecessary() {
                                                                                                        }

                                                                                                        /* access modifiers changed from: protected */
                                                                                                        public void onDownloadStarted() {
                                                                                                        }

                                                                                                        /* access modifiers changed from: protected */
                                                                                                        public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                                                                                                            String str = PostListingFragment.TAG;
                                                                                                            StringBuilder sb = new StringBuilder();
                                                                                                            sb.append("Failed to precache ");
                                                                                                            sb.append(this.url.toString());
                                                                                                            sb.append("(RequestFailureType code: ");
                                                                                                            sb.append(type);
                                                                                                            sb.append(")");
                                                                                                            Log.e(str, sb.toString());
                                                                                                        }

                                                                                                        /* access modifiers changed from: protected */
                                                                                                        public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
                                                                                                        }

                                                                                                        /* access modifiers changed from: protected */
                                                                                                        public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
                                                                                                            String str = PostListingFragment.TAG;
                                                                                                            StringBuilder sb = new StringBuilder();
                                                                                                            sb.append("Successfully precached ");
                                                                                                            sb.append(this.url.toString());
                                                                                                            Log.i(str, sb.toString());
                                                                                                        }
                                                                                                    };
                                                                                                    cacheManager.makeRequest(r52);
                                                                                                    r44 = defaultAccount;
                                                                                                } catch (Throwable th) {
                                                                                                    t = th;
                                                                                                    r4 = r152;
                                                                                                    AppCompatActivity appCompatActivity = activity2;
                                                                                                }
                                                                                            } catch (Throwable th2) {
                                                                                                t = th2;
                                                                                                AppCompatActivity appCompatActivity2 = activity3;
                                                                                                r4 = r152;
                                                                                                r4.notifyFailure(6, t, null, "Parse failure");
                                                                                                return;
                                                                                            }
                                                                                        } else {
                                                                                            preparedPost = redditPreparedPost;
                                                                                            JsonValue jsonValue2 = postThingValue;
                                                                                            arrayList2 = arrayList3;
                                                                                            r232 = pref_blocked_subreddits;
                                                                                            leftHandedMode2 = leftHandedMode3;
                                                                                            imagePrecachePref2 = imagePrecachePref;
                                                                                            post = post2;
                                                                                            thumbnailsPref = thumbnailsPref2;
                                                                                            listing = listing2;
                                                                                            activity2 = activity3;
                                                                                            commentPrecachePref2 = commentPrecachePref3;
                                                                                            r44 = pref_blocked_subreddits;
                                                                                        }
                                                                                        r45 = r44;
                                                                                        String url = parsedPost.getUrl();
                                                                                        r14 = r14;
                                                                                        r47 = r152;
                                                                                        final boolean z6 = precacheImages;
                                                                                        final RedditPost redditPost = post;
                                                                                        final AppCompatActivity appCompatActivity3 = activity2;
                                                                                        final int i = positionInList;
                                                                                        r45 = r47;
                                                                                        AnonymousClass4 r14 = new GetImageInfoListener() {
                                                                                            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                                                                                            }

                                                                                            public void onNotAnImage() {
                                                                                            }

                                                                                            public void onSuccess(ImageInfo info2) {
                                                                                                if (z6) {
                                                                                                    if (info2.size != null && info2.size.longValue() > 15728640) {
                                                                                                        Log.i(PostListingFragment.TAG, String.format("Not precaching '%s': too big (%d kB)", new Object[]{redditPost.getUrl(), Long.valueOf(info2.size.longValue() / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID)}));
                                                                                                    } else if (MediaType.GIF.equals(info2.mediaType) && !gifViewMode.downloadInApp) {
                                                                                                        Log.i(PostListingFragment.TAG, String.format("Not precaching '%s': GIFs are opened externally", new Object[]{redditPost.getUrl()}));
                                                                                                    } else if (MediaType.IMAGE.equals(info2.mediaType) && !imageViewMode.downloadInApp) {
                                                                                                        Log.i(PostListingFragment.TAG, String.format("Not precaching '%s': images are opened externally", new Object[]{redditPost.getUrl()}));
                                                                                                    } else if (!MediaType.VIDEO.equals(info2.mediaType) || videoViewMode.downloadInApp) {
                                                                                                        PostListingRequest.this.this$0.precacheImage(appCompatActivity3, info2.urlOriginal, i);
                                                                                                        if (info2.urlAudioStream != null) {
                                                                                                            PostListingRequest.this.this$0.precacheImage(appCompatActivity3, info2.urlAudioStream, i);
                                                                                                        }
                                                                                                    } else {
                                                                                                        Log.i(PostListingFragment.TAG, String.format("Not precaching '%s': videos are opened externally", new Object[]{redditPost.getUrl()}));
                                                                                                    }
                                                                                                }
                                                                                            }
                                                                                        };
                                                                                        activity = activity2;
                                                                                        r45 = r47;
                                                                                        LinkHandler.getImageInfo(activity, url, 500, positionInList, r14);
                                                                                        leftHandedMode = leftHandedMode2;
                                                                                        arrayList = arrayList2;
                                                                                        arrayList.add(new RedditPostListItem(preparedPost, r47.this$0, activity, leftHandedMode));
                                                                                        r47.this$0.mPostCount = r47.this$0.mPostCount + 1;
                                                                                        r47.this$0.mPostRefreshCount.decrementAndGet();
                                                                                        r23 = r232;
                                                                                        r43 = r47;
                                                                                    }
                                                                                }
                                                                                downloadThisThumbnail = false;
                                                                                r152 = r152;
                                                                                int positionInList2 = r152.this$0.mPostCount;
                                                                                CachePrecacheComments commentPrecachePref32 = commentPrecachePref;
                                                                                RedditParsedPost parsedPost2 = new RedditParsedPost(post2, false);
                                                                                RedditPreparedPost redditPreparedPost2 = new RedditPreparedPost(activity3, cm, positionInList2, parsedPost2, timestamp, showSubredditName, downloadThisThumbnail);
                                                                                r152 = r152;
                                                                                if (hideReadPosts) {
                                                                                }
                                                                                if (!precacheComments) {
                                                                                }
                                                                                try {
                                                                                    r45 = r44;
                                                                                    String url2 = parsedPost2.getUrl();
                                                                                    r14 = r14;
                                                                                    r47 = r152;
                                                                                    final boolean z62 = precacheImages;
                                                                                    final RedditPost redditPost2 = post;
                                                                                    final AppCompatActivity appCompatActivity32 = activity2;
                                                                                    final int i2 = positionInList2;
                                                                                } catch (Throwable th3) {
                                                                                    t = th3;
                                                                                    r46 = r152;
                                                                                    AppCompatActivity appCompatActivity4 = activity2;
                                                                                    r4 = r46;
                                                                                    r4.notifyFailure(6, t, null, "Parse failure");
                                                                                    return;
                                                                                }
                                                                                try {
                                                                                    r45 = r47;
                                                                                    AnonymousClass4 r142 = new GetImageInfoListener() {
                                                                                        public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                                                                                        }

                                                                                        public void onNotAnImage() {
                                                                                        }

                                                                                        public void onSuccess(ImageInfo info2) {
                                                                                            if (z62) {
                                                                                                if (info2.size != null && info2.size.longValue() > 15728640) {
                                                                                                    Log.i(PostListingFragment.TAG, String.format("Not precaching '%s': too big (%d kB)", new Object[]{redditPost2.getUrl(), Long.valueOf(info2.size.longValue() / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID)}));
                                                                                                } else if (MediaType.GIF.equals(info2.mediaType) && !gifViewMode.downloadInApp) {
                                                                                                    Log.i(PostListingFragment.TAG, String.format("Not precaching '%s': GIFs are opened externally", new Object[]{redditPost2.getUrl()}));
                                                                                                } else if (MediaType.IMAGE.equals(info2.mediaType) && !imageViewMode.downloadInApp) {
                                                                                                    Log.i(PostListingFragment.TAG, String.format("Not precaching '%s': images are opened externally", new Object[]{redditPost2.getUrl()}));
                                                                                                } else if (!MediaType.VIDEO.equals(info2.mediaType) || videoViewMode.downloadInApp) {
                                                                                                    PostListingRequest.this.this$0.precacheImage(appCompatActivity32, info2.urlOriginal, i2);
                                                                                                    if (info2.urlAudioStream != null) {
                                                                                                        PostListingRequest.this.this$0.precacheImage(appCompatActivity32, info2.urlAudioStream, i2);
                                                                                                    }
                                                                                                } else {
                                                                                                    Log.i(PostListingFragment.TAG, String.format("Not precaching '%s': videos are opened externally", new Object[]{redditPost2.getUrl()}));
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    };
                                                                                    activity = activity2;
                                                                                    r45 = r47;
                                                                                    LinkHandler.getImageInfo(activity, url2, 500, positionInList2, r142);
                                                                                    leftHandedMode = leftHandedMode2;
                                                                                    arrayList = arrayList2;
                                                                                    arrayList.add(new RedditPostListItem(preparedPost, r47.this$0, activity, leftHandedMode));
                                                                                    r47.this$0.mPostCount = r47.this$0.mPostCount + 1;
                                                                                    r47.this$0.mPostRefreshCount.decrementAndGet();
                                                                                    r23 = r232;
                                                                                    r43 = r47;
                                                                                } catch (Throwable th4) {
                                                                                    t = th4;
                                                                                    r4 = r45;
                                                                                    r4.notifyFailure(6, t, null, "Parse failure");
                                                                                    return;
                                                                                }
                                                                            } else {
                                                                                JsonValue jsonValue3 = postThingValue;
                                                                                arrayList = arrayList3;
                                                                                r23 = pref_blocked_subreddits;
                                                                                leftHandedMode = leftHandedMode3;
                                                                                commentPrecachePref2 = commentPrecachePref;
                                                                                imagePrecachePref2 = imagePrecachePref;
                                                                                RedditPost redditPost3 = post2;
                                                                                thumbnailsPref = thumbnailsPref2;
                                                                                listing = listing2;
                                                                                activity = activity3;
                                                                                r43 = r152;
                                                                            }
                                                                        } else {
                                                                            JsonValue jsonValue4 = postThingValue;
                                                                            arrayList = arrayList3;
                                                                            r23 = pref_blocked_subreddits;
                                                                            leftHandedMode = leftHandedMode3;
                                                                            commentPrecachePref2 = commentPrecachePref;
                                                                            imagePrecachePref2 = imagePrecachePref;
                                                                            RedditPost redditPost4 = post2;
                                                                            thumbnailsPref = thumbnailsPref2;
                                                                            listing = listing2;
                                                                            activity = activity3;
                                                                            r43 = r152;
                                                                        }
                                                                        long j2 = timestamp;
                                                                        UUID uuid2 = session;
                                                                        r15 = r43;
                                                                        activity3 = activity;
                                                                        r42 = r23;
                                                                        commentPrecachePref = commentPrecachePref2;
                                                                        imagePrecachePref = imagePrecachePref2;
                                                                        thumbnailsPref2 = thumbnailsPref;
                                                                        thing = thing2;
                                                                        listing2 = listing;
                                                                        leftHandedMode3 = leftHandedMode;
                                                                        arrayList3 = arrayList;
                                                                    }
                                                                }
                                                                isPostBlocked = false;
                                                                if (isPostBlocked) {
                                                                }
                                                                long j22 = timestamp;
                                                                UUID uuid22 = session;
                                                                r15 = r43;
                                                                activity3 = activity;
                                                                r42 = r23;
                                                                commentPrecachePref = commentPrecachePref2;
                                                                imagePrecachePref = imagePrecachePref2;
                                                                thumbnailsPref2 = thumbnailsPref;
                                                                thing = thing2;
                                                                listing2 = listing;
                                                                leftHandedMode3 = leftHandedMode;
                                                                arrayList3 = arrayList;
                                                            }
                                                            r152 = r15;
                                                            pref_blocked_subreddits = r42;
                                                        }
                                                        final ArrayList arrayList4 = arrayList3;
                                                        ? r233 = pref_blocked_subreddits;
                                                        boolean z7 = leftHandedMode3;
                                                        CachePrecacheComments cachePrecacheComments = commentPrecachePref;
                                                        CachePrecacheImages cachePrecacheImages = imagePrecachePref;
                                                        AppearanceThumbnailsShow appearanceThumbnailsShow = thumbnailsPref2;
                                                        JsonBufferedObject jsonBufferedObject = listing2;
                                                        AppCompatActivity appCompatActivity5 = activity3;
                                                        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                                                            public void run() {
                                                                PostListingRequest.this.this$0.mPostListingManager.addPosts(arrayList4);
                                                                PostListingRequest.this.this$0.mPostListingManager.setLoadingVisible(false);
                                                                PostListingRequest.this.this$0.onPostsAdded();
                                                                PostListingRequest.this.this$0.mRequest = null;
                                                                PostListingRequest.this.this$0.mReadyToDownloadMore = true;
                                                                PostListingRequest.this.this$0.onLoadMoreItemsCheck();
                                                            }
                                                        });
                                                    }
                                                }
                                                showSubredditName = true;
                                                ArrayList arrayList32 = new ArrayList(25);
                                                it = posts.iterator();
                                                while (it.hasNext()) {
                                                }
                                                final ArrayList arrayList42 = arrayList32;
                                                ? r2332 = pref_blocked_subreddits;
                                                boolean z72 = leftHandedMode3;
                                                CachePrecacheComments cachePrecacheComments2 = commentPrecachePref;
                                                CachePrecacheImages cachePrecacheImages2 = imagePrecachePref;
                                                AppearanceThumbnailsShow appearanceThumbnailsShow2 = thumbnailsPref2;
                                                JsonBufferedObject jsonBufferedObject2 = listing2;
                                                AppCompatActivity appCompatActivity52 = activity3;
                                                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                                                    public void run() {
                                                        PostListingRequest.this.this$0.mPostListingManager.addPosts(arrayList42);
                                                        PostListingRequest.this.this$0.mPostListingManager.setLoadingVisible(false);
                                                        PostListingRequest.this.this$0.onPostsAdded();
                                                        PostListingRequest.this.this$0.mRequest = null;
                                                        PostListingRequest.this.this$0.mReadyToDownloadMore = true;
                                                        PostListingRequest.this.this$0.onLoadMoreItemsCheck();
                                                    }
                                                });
                                            }
                                        }
                                        z4 = false;
                                        boolean subredditFilteringEnabled2 = z4;
                                        ? pref_blocked_subreddits2 = PrefsUtility.pref_blocked_subreddits(activity3, r152.this$0.mSharedPreferences);
                                        String str3 = PostListingFragment.TAG;
                                        StringBuilder sb3 = new StringBuilder();
                                        sb3.append("Precaching images: ");
                                        sb3.append(!precacheImages ? OrbotHelper.STATUS_ON : OrbotHelper.STATUS_OFF);
                                        Log.i(str3, sb3.toString());
                                        String str22 = PostListingFragment.TAG;
                                        StringBuilder sb22 = new StringBuilder();
                                        sb22.append("Precaching comments: ");
                                        sb22.append(!precacheComments ? OrbotHelper.STATUS_ON : OrbotHelper.STATUS_OFF);
                                        Log.i(str22, sb22.toString());
                                        CacheManager cm2 = CacheManager.getInstance(activity3);
                                        if (r152.this$0.mPostListingURL != null) {
                                        }
                                        showSubredditName = true;
                                        ArrayList arrayList322 = new ArrayList(25);
                                        it = posts.iterator();
                                        while (it.hasNext()) {
                                        }
                                        final ArrayList arrayList422 = arrayList322;
                                        ? r23322 = pref_blocked_subreddits2;
                                        boolean z722 = leftHandedMode3;
                                        CachePrecacheComments cachePrecacheComments22 = commentPrecachePref;
                                        CachePrecacheImages cachePrecacheImages22 = imagePrecachePref;
                                        AppearanceThumbnailsShow appearanceThumbnailsShow22 = thumbnailsPref2;
                                        JsonBufferedObject jsonBufferedObject22 = listing2;
                                        AppCompatActivity appCompatActivity522 = activity3;
                                        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                                            public void run() {
                                                PostListingRequest.this.this$0.mPostListingManager.addPosts(arrayList422);
                                                PostListingRequest.this.this$0.mPostListingManager.setLoadingVisible(false);
                                                PostListingRequest.this.this$0.onPostsAdded();
                                                PostListingRequest.this.this$0.mRequest = null;
                                                PostListingRequest.this.this$0.mReadyToDownloadMore = true;
                                                PostListingRequest.this.this$0.onLoadMoreItemsCheck();
                                            }
                                        });
                                    }
                                }
                                z3 = true;
                                boolean precacheComments2 = z3;
                                final ImageViewMode imageViewMode2 = PrefsUtility.pref_behaviour_imageview_mode(activity3, r152.this$0.mSharedPreferences);
                                final GifViewMode gifViewMode2 = PrefsUtility.pref_behaviour_gifview_mode(activity3, r152.this$0.mSharedPreferences);
                                final VideoViewMode videoViewMode2 = PrefsUtility.pref_behaviour_videoview_mode(activity3, r152.this$0.mSharedPreferences);
                                boolean leftHandedMode32 = PrefsUtility.pref_appearance_left_handed(activity3, r152.this$0.mSharedPreferences);
                                if (r152.this$0.mPostListingURL.pathType() == 0) {
                                }
                                z4 = false;
                                boolean subredditFilteringEnabled22 = z4;
                                ? pref_blocked_subreddits22 = PrefsUtility.pref_blocked_subreddits(activity3, r152.this$0.mSharedPreferences);
                                String str32 = PostListingFragment.TAG;
                                StringBuilder sb32 = new StringBuilder();
                                sb32.append("Precaching images: ");
                                sb32.append(!precacheImages ? OrbotHelper.STATUS_ON : OrbotHelper.STATUS_OFF);
                                Log.i(str32, sb32.toString());
                                String str222 = PostListingFragment.TAG;
                                StringBuilder sb222 = new StringBuilder();
                                sb222.append("Precaching comments: ");
                                sb222.append(!precacheComments2 ? OrbotHelper.STATUS_ON : OrbotHelper.STATUS_OFF);
                                Log.i(str222, sb222.toString());
                                CacheManager cm22 = CacheManager.getInstance(activity3);
                                if (r152.this$0.mPostListingURL != null) {
                                }
                                showSubredditName = true;
                                ArrayList arrayList3222 = new ArrayList(25);
                                it = posts.iterator();
                                while (it.hasNext()) {
                                }
                                final ArrayList arrayList4222 = arrayList3222;
                                ? r233222 = pref_blocked_subreddits22;
                                boolean z7222 = leftHandedMode32;
                                CachePrecacheComments cachePrecacheComments222 = commentPrecachePref;
                                CachePrecacheImages cachePrecacheImages222 = imagePrecachePref;
                                AppearanceThumbnailsShow appearanceThumbnailsShow222 = thumbnailsPref2;
                                JsonBufferedObject jsonBufferedObject222 = listing2;
                                AppCompatActivity appCompatActivity5222 = activity3;
                                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                                    public void run() {
                                        PostListingRequest.this.this$0.mPostListingManager.addPosts(arrayList4222);
                                        PostListingRequest.this.this$0.mPostListingManager.setLoadingVisible(false);
                                        PostListingRequest.this.this$0.onPostsAdded();
                                        PostListingRequest.this.this$0.mRequest = null;
                                        PostListingRequest.this.this$0.mReadyToDownloadMore = true;
                                        PostListingRequest.this.this$0.onLoadMoreItemsCheck();
                                    }
                                });
                            }
                            if (!General.isCacheDiskFull(activity3)) {
                                z2 = true;
                                boolean precacheImages2 = z2;
                                if (commentPrecachePref != CachePrecacheComments.ALWAYS) {
                                }
                                z3 = true;
                                boolean precacheComments22 = z3;
                                final ImageViewMode imageViewMode22 = PrefsUtility.pref_behaviour_imageview_mode(activity3, r152.this$0.mSharedPreferences);
                                final GifViewMode gifViewMode22 = PrefsUtility.pref_behaviour_gifview_mode(activity3, r152.this$0.mSharedPreferences);
                                final VideoViewMode videoViewMode22 = PrefsUtility.pref_behaviour_videoview_mode(activity3, r152.this$0.mSharedPreferences);
                                boolean leftHandedMode322 = PrefsUtility.pref_appearance_left_handed(activity3, r152.this$0.mSharedPreferences);
                                if (r152.this$0.mPostListingURL.pathType() == 0) {
                                }
                                z4 = false;
                                boolean subredditFilteringEnabled222 = z4;
                                ? pref_blocked_subreddits222 = PrefsUtility.pref_blocked_subreddits(activity3, r152.this$0.mSharedPreferences);
                                String str322 = PostListingFragment.TAG;
                                StringBuilder sb322 = new StringBuilder();
                                sb322.append("Precaching images: ");
                                sb322.append(!precacheImages2 ? OrbotHelper.STATUS_ON : OrbotHelper.STATUS_OFF);
                                Log.i(str322, sb322.toString());
                                String str2222 = PostListingFragment.TAG;
                                StringBuilder sb2222 = new StringBuilder();
                                sb2222.append("Precaching comments: ");
                                sb2222.append(!precacheComments22 ? OrbotHelper.STATUS_ON : OrbotHelper.STATUS_OFF);
                                Log.i(str2222, sb2222.toString());
                                CacheManager cm222 = CacheManager.getInstance(activity3);
                                if (r152.this$0.mPostListingURL != null) {
                                }
                                showSubredditName = true;
                                ArrayList arrayList32222 = new ArrayList(25);
                                it = posts.iterator();
                                while (it.hasNext()) {
                                }
                                final ArrayList arrayList42222 = arrayList32222;
                                ? r2332222 = pref_blocked_subreddits222;
                                boolean z72222 = leftHandedMode322;
                                CachePrecacheComments cachePrecacheComments2222 = commentPrecachePref;
                                CachePrecacheImages cachePrecacheImages2222 = imagePrecachePref;
                                AppearanceThumbnailsShow appearanceThumbnailsShow2222 = thumbnailsPref2;
                                JsonBufferedObject jsonBufferedObject2222 = listing2;
                                AppCompatActivity appCompatActivity52222 = activity3;
                                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                                    public void run() {
                                        PostListingRequest.this.this$0.mPostListingManager.addPosts(arrayList42222);
                                        PostListingRequest.this.this$0.mPostListingManager.setLoadingVisible(false);
                                        PostListingRequest.this.this$0.onPostsAdded();
                                        PostListingRequest.this.this$0.mRequest = null;
                                        PostListingRequest.this.this$0.mReadyToDownloadMore = true;
                                        PostListingRequest.this.this$0.onLoadMoreItemsCheck();
                                    }
                                });
                            }
                            z2 = false;
                            boolean precacheImages22 = z2;
                            if (commentPrecachePref != CachePrecacheComments.ALWAYS) {
                            }
                            z3 = true;
                            boolean precacheComments222 = z3;
                            final ImageViewMode imageViewMode222 = PrefsUtility.pref_behaviour_imageview_mode(activity3, r152.this$0.mSharedPreferences);
                            final GifViewMode gifViewMode222 = PrefsUtility.pref_behaviour_gifview_mode(activity3, r152.this$0.mSharedPreferences);
                            final VideoViewMode videoViewMode222 = PrefsUtility.pref_behaviour_videoview_mode(activity3, r152.this$0.mSharedPreferences);
                            boolean leftHandedMode3222 = PrefsUtility.pref_appearance_left_handed(activity3, r152.this$0.mSharedPreferences);
                            if (r152.this$0.mPostListingURL.pathType() == 0) {
                            }
                            z4 = false;
                            boolean subredditFilteringEnabled2222 = z4;
                            ? pref_blocked_subreddits2222 = PrefsUtility.pref_blocked_subreddits(activity3, r152.this$0.mSharedPreferences);
                            String str3222 = PostListingFragment.TAG;
                            StringBuilder sb3222 = new StringBuilder();
                            sb3222.append("Precaching images: ");
                            sb3222.append(!precacheImages22 ? OrbotHelper.STATUS_ON : OrbotHelper.STATUS_OFF);
                            Log.i(str3222, sb3222.toString());
                            String str22222 = PostListingFragment.TAG;
                            StringBuilder sb22222 = new StringBuilder();
                            sb22222.append("Precaching comments: ");
                            sb22222.append(!precacheComments222 ? OrbotHelper.STATUS_ON : OrbotHelper.STATUS_OFF);
                            Log.i(str22222, sb22222.toString());
                            CacheManager cm2222 = CacheManager.getInstance(activity3);
                            if (r152.this$0.mPostListingURL != null) {
                            }
                            showSubredditName = true;
                            ArrayList arrayList322222 = new ArrayList(25);
                            it = posts.iterator();
                            while (it.hasNext()) {
                            }
                            final ArrayList arrayList422222 = arrayList322222;
                            ? r23322222 = pref_blocked_subreddits2222;
                            boolean z722222 = leftHandedMode3222;
                            CachePrecacheComments cachePrecacheComments22222 = commentPrecachePref;
                            CachePrecacheImages cachePrecacheImages22222 = imagePrecachePref;
                            AppearanceThumbnailsShow appearanceThumbnailsShow22222 = thumbnailsPref2;
                            JsonBufferedObject jsonBufferedObject22222 = listing2;
                            AppCompatActivity appCompatActivity522222 = activity3;
                            AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                                public void run() {
                                    PostListingRequest.this.this$0.mPostListingManager.addPosts(arrayList422222);
                                    PostListingRequest.this.this$0.mPostListingManager.setLoadingVisible(false);
                                    PostListingRequest.this.this$0.onPostsAdded();
                                    PostListingRequest.this.this$0.mRequest = null;
                                    PostListingRequest.this.this$0.mReadyToDownloadMore = true;
                                    PostListingRequest.this.this$0.onLoadMoreItemsCheck();
                                }
                            });
                        }
                    } catch (Throwable th5) {
                        t = th5;
                        AppCompatActivity appCompatActivity6 = activity3;
                        r4 = r152;
                    }
                }
                z = true;
                boolean downloadThumbnails2 = z;
                boolean showNsfwThumbnails2 = PrefsUtility.appearance_thumbnails_nsfw_show(activity3, r152.this$0.mSharedPreferences);
                imagePrecachePref = PrefsUtility.cache_precache_images(activity3, r152.this$0.mSharedPreferences);
                commentPrecachePref = PrefsUtility.cache_precache_comments(activity3, r152.this$0.mSharedPreferences);
                if (imagePrecachePref != CachePrecacheImages.ALWAYS) {
                }
                if (!General.isCacheDiskFull(activity3)) {
                }
                z2 = false;
                boolean precacheImages222 = z2;
                if (commentPrecachePref != CachePrecacheComments.ALWAYS) {
                }
                z3 = true;
                boolean precacheComments2222 = z3;
                final ImageViewMode imageViewMode2222 = PrefsUtility.pref_behaviour_imageview_mode(activity3, r152.this$0.mSharedPreferences);
                final GifViewMode gifViewMode2222 = PrefsUtility.pref_behaviour_gifview_mode(activity3, r152.this$0.mSharedPreferences);
                final VideoViewMode videoViewMode2222 = PrefsUtility.pref_behaviour_videoview_mode(activity3, r152.this$0.mSharedPreferences);
                boolean leftHandedMode32222 = PrefsUtility.pref_appearance_left_handed(activity3, r152.this$0.mSharedPreferences);
                if (r152.this$0.mPostListingURL.pathType() == 0) {
                }
                z4 = false;
                boolean subredditFilteringEnabled22222 = z4;
                ? pref_blocked_subreddits22222 = PrefsUtility.pref_blocked_subreddits(activity3, r152.this$0.mSharedPreferences);
                String str32222 = PostListingFragment.TAG;
                StringBuilder sb32222 = new StringBuilder();
                sb32222.append("Precaching images: ");
                sb32222.append(!precacheImages222 ? OrbotHelper.STATUS_ON : OrbotHelper.STATUS_OFF);
                Log.i(str32222, sb32222.toString());
                String str222222 = PostListingFragment.TAG;
                StringBuilder sb222222 = new StringBuilder();
                sb222222.append("Precaching comments: ");
                sb222222.append(!precacheComments2222 ? OrbotHelper.STATUS_ON : OrbotHelper.STATUS_OFF);
                Log.i(str222222, sb222222.toString());
                CacheManager cm22222 = CacheManager.getInstance(activity3);
                if (r152.this$0.mPostListingURL != null) {
                }
                showSubredditName = true;
                ArrayList arrayList3222222 = new ArrayList(25);
                it = posts.iterator();
                while (it.hasNext()) {
                }
                final ArrayList arrayList4222222 = arrayList3222222;
                ? r233222222 = pref_blocked_subreddits22222;
                boolean z7222222 = leftHandedMode32222;
                CachePrecacheComments cachePrecacheComments222222 = commentPrecachePref;
                CachePrecacheImages cachePrecacheImages222222 = imagePrecachePref;
                AppearanceThumbnailsShow appearanceThumbnailsShow222222 = thumbnailsPref2;
                JsonBufferedObject jsonBufferedObject222222 = listing2;
                AppCompatActivity appCompatActivity5222222 = activity3;
                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                    public void run() {
                        PostListingRequest.this.this$0.mPostListingManager.addPosts(arrayList4222222);
                        PostListingRequest.this.this$0.mPostListingManager.setLoadingVisible(false);
                        PostListingRequest.this.this$0.onPostsAdded();
                        PostListingRequest.this.this$0.mRequest = null;
                        PostListingRequest.this.this$0.mReadyToDownloadMore = true;
                        PostListingRequest.this.this$0.onLoadMoreItemsCheck();
                    }
                });
            } catch (Throwable th6) {
                t = th6;
                AppCompatActivity appCompatActivity7 = activity3;
                r4 = r152;
            }
        }
    }

    public PostListingFragment(AppCompatActivity parent, Bundle savedInstanceState, Uri url, UUID session, boolean forceDownload) {
        DownloadStrategy downloadStrategy;
        SubredditPostListURL subredditPostListURL;
        final AppCompatActivity appCompatActivity = parent;
        Bundle bundle = savedInstanceState;
        UUID uuid = session;
        super(parent, savedInstanceState);
        this.mPostListingManager = new PostListingManager(appCompatActivity);
        if (bundle != null) {
            this.mPreviousFirstVisibleItemPosition = Integer.valueOf(bundle.getInt(SAVEDSTATE_FIRST_VISIBLE_POS));
        }
        try {
            this.mPostListingURL = (PostListingURL) RedditURLParser.parseProbablePostListing(url);
            this.mSession = uuid;
            final Context context = getContext();
            this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (this.mPostListingURL != null) {
                switch (PrefsUtility.pref_behaviour_post_count(context, this.mSharedPreferences)) {
                    case ALL:
                        this.mPostCountLimit = -1;
                        break;
                    case R25:
                        this.mPostCountLimit = 25;
                        break;
                    case R50:
                        this.mPostCountLimit = 50;
                        break;
                    case R100:
                        this.mPostCountLimit = 100;
                        break;
                    default:
                        this.mPostCountLimit = 0;
                        break;
                }
                if (this.mPostCountLimit > 0) {
                    restackRefreshCount();
                }
                ScrollbarRecyclerViewManager recyclerViewManager = new ScrollbarRecyclerViewManager(context, null, false);
                if ((appCompatActivity instanceof OptionsMenuPostsListener) && PrefsUtility.pref_behaviour_enable_swipe_refresh(context, this.mSharedPreferences)) {
                    recyclerViewManager.enablePullToRefresh(new OnRefreshListener() {
                        public void onRefresh() {
                            ((OptionsMenuPostsListener) appCompatActivity).onRefreshPosts();
                        }
                    });
                }
                this.mRecyclerView = recyclerViewManager.getRecyclerView();
                this.mPostListingManager.setLayoutManager((LinearLayoutManager) this.mRecyclerView.getLayoutManager());
                this.mRecyclerView.setAdapter(this.mPostListingManager.getAdapter());
                this.mOuter = recyclerViewManager.getOuterView();
                this.mRecyclerView.addOnScrollListener(new OnScrollListener() {
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        PostListingFragment.this.onLoadMoreItemsCheck();
                    }
                });
                this.mRecyclerView.getLayoutParams().height = -1;
                int i = this.mPostCountLimit;
                if (i > 0 && 50 > i) {
                    int i2 = this.mPostCountLimit;
                }
                if (forceDownload) {
                    downloadStrategy = DownloadStrategyAlways.INSTANCE;
                } else if (uuid == null && bundle == null && General.isNetworkConnected(context)) {
                    downloadStrategy = new DownloadStrategyIfTimestampOutsideBounds(TimestampBound.notOlderThan(PrefsUtility.pref_cache_rerequest_postlist_age_ms(context, this.mSharedPreferences)));
                } else {
                    downloadStrategy = DownloadStrategyIfNotCached.INSTANCE;
                }
                PostListingRequest postListingRequest = r1;
                PostListingRequest postListingRequest2 = new PostListingRequest(this, this.mPostListingURL.generateJsonUri(), RedditAccountManager.getInstance(context).getDefaultAccount(), session, downloadStrategy, true);
                this.mRequest = postListingRequest;
                int pathType = this.mPostListingURL.pathType();
                if (pathType != 8) {
                    switch (pathType) {
                        case 0:
                            switch (((SubredditPostListURL) this.mPostListingURL).type) {
                                case FRONTPAGE:
                                case ALL:
                                case SUBREDDIT_COMBINATION:
                                case ALL_SUBTRACTION:
                                case POPULAR:
                                    setHeader(this.mPostListingURL.humanReadableName(getActivity(), true), this.mPostListingURL.humanReadableUrl());
                                    CacheManager.getInstance(context).makeRequest(this.mRequest);
                                    return;
                                case RANDOM:
                                case SUBREDDIT:
                                    try {
                                        RedditSubredditManager.getInstance(getActivity(), RedditAccountManager.getInstance(getActivity()).getDefaultAccount()).getSubreddit(RedditSubreddit.getCanonicalName(subredditPostListURL.subreddit), TimestampBound.NONE, new RequestResponseHandler<RedditSubreddit, SubredditRequestFailure>() {
                                            public void onRequestFailed(SubredditRequestFailure failureReason) {
                                                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                                                    public void run() {
                                                        CacheManager.getInstance(context).makeRequest(PostListingFragment.this.mRequest);
                                                    }
                                                });
                                            }

                                            public void onRequestSuccess(final RedditSubreddit result, long timeCached) {
                                                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                                                    public void run() {
                                                        PostListingFragment.this.mSubreddit = result;
                                                        PostListingFragment.this.onSubredditReceived();
                                                        CacheManager.getInstance(context).makeRequest(PostListingFragment.this.mRequest);
                                                    }
                                                });
                                            }
                                        }, null);
                                        return;
                                    } catch (InvalidSubredditNameException e) {
                                        throw new RuntimeException(e);
                                    }
                                default:
                                    return;
                            }
                        case 1:
                            break;
                        case 2:
                            setHeader(new SearchListingHeader(getActivity(), (SearchPostListURL) this.mPostListingURL));
                            CacheManager.getInstance(context).makeRequest(this.mRequest);
                            return;
                        default:
                            return;
                    }
                }
                setHeader(this.mPostListingURL.humanReadableName(getActivity(), true), this.mPostListingURL.humanReadableUrl());
                CacheManager.getInstance(context).makeRequest(this.mRequest);
                return;
            }
            this.mPostListingManager.addFooterError(new ErrorView(getActivity(), new RRError("Invalid post listing URL", "Could not navigate to that URL.")));
            throw new RuntimeException("Invalid post listing URL");
        } catch (ClassCastException e2) {
            Toast.makeText(getActivity(), "Invalid post listing URL.", 1).show();
            throw new RuntimeException("Invalid post listing URL");
        }
    }

    private LinearLayout createVerticalLinearLayout(Context context) {
        LinearLayout result = new LinearLayout(context);
        result.setOrientation(1);
        return result;
    }

    public View getView() {
        return this.mOuter;
    }

    public Bundle onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putInt(SAVEDSTATE_FIRST_VISIBLE_POS, ((LinearLayoutManager) this.mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition());
        return bundle;
    }

    public void cancel() {
        CacheRequest cacheRequest = this.mRequest;
        if (cacheRequest != null) {
            cacheRequest.cancel();
        }
    }

    public synchronized void restackRefreshCount() {
        while (this.mPostRefreshCount.get() <= 0) {
            this.mPostRefreshCount.addAndGet(this.mPostCountLimit);
        }
    }

    /* access modifiers changed from: private */
    public void onSubredditReceived() {
        final String subtitle;
        if (this.mPostListingURL.pathType() == 0 && this.mPostListingURL.asSubredditPostListURL().type == Type.RANDOM) {
            try {
                this.mPostListingURL = this.mPostListingURL.asSubredditPostListURL().changeSubreddit(RedditSubreddit.stripRPrefix(this.mSubreddit.url));
                PostListingRequest postListingRequest = new PostListingRequest(this, this.mPostListingURL.generateJsonUri(), RedditAccountManager.getInstance(getContext()).getDefaultAccount(), this.mSession, this.mRequest.downloadStrategy, true);
                this.mRequest = postListingRequest;
            } catch (InvalidSubredditNameException e) {
                throw new RuntimeException(e);
            }
        }
        if (this.mPostListingURL.getOrder() != null && this.mPostListingURL.getOrder() != PostSort.HOT) {
            subtitle = this.mPostListingURL.humanReadableUrl();
        } else if (this.mSubreddit.subscribers == null) {
            subtitle = getString(R.string.header_subscriber_count_unknown);
        } else {
            subtitle = getContext().getString(R.string.header_subscriber_count, new Object[]{NumberFormat.getNumberInstance(Locale.getDefault()).format(this.mSubreddit.subscribers)});
        }
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PostListingFragment postListingFragment = PostListingFragment.this;
                postListingFragment.setHeader(StringEscapeUtils.unescapeHtml4(postListingFragment.mSubreddit.title), subtitle);
                PostListingFragment.this.getActivity().invalidateOptionsMenu();
            }
        });
    }

    /* access modifiers changed from: private */
    public void setHeader(String title, String subtitle) {
        setHeader(new PostListingHeader(getActivity(), title, subtitle));
    }

    private void setHeader(final View view) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                PostListingFragment.this.mPostListingManager.addPostListingHeader(view);
            }
        });
    }

    public void onPostSelected(final RedditPreparedPost post) {
        ((PostSelectionListener) getActivity()).onPostSelected(post);
        new Thread() {
            public void run() {
                post.markAsRead(PostListingFragment.this.getActivity());
            }
        }.start();
    }

    public void onPostCommentsSelected(final RedditPreparedPost post) {
        ((PostSelectionListener) getActivity()).onPostCommentsSelected(post);
        new Thread() {
            public void run() {
                post.markAsRead(PostListingFragment.this.getActivity());
            }
        }.start();
    }

    /* access modifiers changed from: private */
    public void onLoadMoreItemsCheck() {
        General.checkThisIsUIThread();
        if (this.mReadyToDownloadMore) {
            String str = this.mAfter;
            if (str != null && !str.equals(this.mLastAfter)) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) this.mRecyclerView.getLayoutManager();
                if ((layoutManager.getItemCount() - layoutManager.findLastVisibleItemPosition() < 20 && (this.mPostCountLimit <= 0 || this.mPostRefreshCount.get() > 0)) || (this.mPreviousFirstVisibleItemPosition != null && layoutManager.getItemCount() <= this.mPreviousFirstVisibleItemPosition.intValue())) {
                    String str2 = this.mAfter;
                    this.mLastAfter = str2;
                    this.mReadyToDownloadMore = false;
                    Uri newUri = this.mPostListingURL.after(str2).generateJsonUri();
                    DownloadStrategy strategy = RRTime.since(this.mTimestamp) < 10800000 ? DownloadStrategyIfNotCached.INSTANCE : DownloadStrategyNever.INSTANCE;
                    if (this.mPostCountLimit > 0 && 50 > this.mPostRefreshCount.get()) {
                        int i = this.mPostRefreshCount.get();
                    }
                    PostListingRequest postListingRequest = new PostListingRequest(this, newUri, RedditAccountManager.getInstance(getActivity()).getDefaultAccount(), this.mSession, strategy, false);
                    this.mRequest = postListingRequest;
                    this.mPostListingManager.setLoadingVisible(true);
                    CacheManager.getInstance(getActivity()).makeRequest(this.mRequest);
                } else if (this.mPostCountLimit > 0 && this.mPostRefreshCount.get() <= 0 && this.mLoadMoreView == null) {
                    this.mLoadMoreView = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.load_more_posts, null);
                    this.mLoadMoreView.setOnClickListener(new OnClickListener() {
                        public void onClick(View view) {
                            PostListingFragment.this.mPostListingManager.removeLoadMoreButton();
                            PostListingFragment.this.mLoadMoreView = null;
                            PostListingFragment.this.restackRefreshCount();
                            PostListingFragment.this.onLoadMoreItemsCheck();
                        }
                    });
                    this.mPostListingManager.addLoadMoreButton(this.mLoadMoreView);
                }
            }
        }
    }

    public void onSubscribe() {
        if (this.mPostListingURL.pathType() == 0) {
            try {
                RedditSubredditSubscriptionManager.getSingleton(getActivity(), RedditAccountManager.getInstance(getActivity()).getDefaultAccount()).subscribe(RedditSubreddit.getCanonicalName(this.mPostListingURL.asSubredditPostListURL().subreddit), getActivity());
            } catch (InvalidSubredditNameException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void onUnsubscribe() {
        if (this.mSubreddit != null) {
            try {
                RedditSubredditSubscriptionManager.getSingleton(getActivity(), RedditAccountManager.getInstance(getActivity()).getDefaultAccount()).unsubscribe(this.mSubreddit.getCanonicalName(), getActivity());
            } catch (InvalidSubredditNameException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public RedditSubreddit getSubreddit() {
        return this.mSubreddit;
    }

    private static Uri setUriDownloadCount(Uri input, int count) {
        return input.buildUpon().appendQueryParameter("limit", String.valueOf(count)).build();
    }

    public void onPostsAdded() {
        if (this.mPreviousFirstVisibleItemPosition != null) {
            LinearLayoutManager layoutManager = (LinearLayoutManager) this.mRecyclerView.getLayoutManager();
            if (layoutManager.getItemCount() > this.mPreviousFirstVisibleItemPosition.intValue()) {
                layoutManager.scrollToPositionWithOffset(this.mPreviousFirstVisibleItemPosition.intValue(), 0);
                this.mPreviousFirstVisibleItemPosition = null;
            } else {
                layoutManager.scrollToPosition(layoutManager.getItemCount() - 1);
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean getIsPostBlocked(@NonNull List<String> blockedSubreddits, @NonNull RedditPost post) throws InvalidSubredditNameException {
        String canonicalName = RedditSubreddit.getCanonicalName(post.subreddit);
        for (String blockedSubredditName : blockedSubreddits) {
            if (blockedSubredditName.equalsIgnoreCase(canonicalName)) {
                return true;
            }
        }
        return false;
    }

    /* access modifiers changed from: private */
    public void precacheImage(Activity activity, String url, int positionInList) {
        URI uri = General.uriFromString(url);
        if (uri == null) {
            Log.i(TAG, String.format("Not precaching '%s': failed to parse URL", new Object[]{url}));
            return;
        }
        CacheManager instance = CacheManager.getInstance(activity);
        AnonymousClass9 r0 = new CacheRequest(uri, RedditAccountManager.getAnon(), null, 500, positionInList, DownloadStrategyIfNotCached.INSTANCE, FileType.IMAGE, 3, false, false, activity) {
            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
            }

            /* access modifiers changed from: protected */
            public void onDownloadNecessary() {
            }

            /* access modifiers changed from: protected */
            public void onDownloadStarted() {
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                String str;
                String str2 = PostListingFragment.TAG;
                Locale locale = Locale.US;
                String str3 = "Failed to precache %s (RequestFailureType %d, status %s, readable '%s')";
                Object[] objArr = new Object[4];
                objArr[0] = this.url;
                objArr[1] = Integer.valueOf(type);
                if (status == null) {
                    str = "NULL";
                } else {
                    str = status.toString();
                }
                objArr[2] = str;
                objArr[3] = readableMessage == null ? "NULL" : readableMessage;
                Log.e(str2, String.format(locale, str3, objArr));
            }

            /* access modifiers changed from: protected */
            public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
            }

            /* access modifiers changed from: protected */
            public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
                String str = PostListingFragment.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Successfully precached ");
                sb.append(this.url);
                Log.i(str, sb.toString());
            }
        };
        instance.makeRequest(r0);
    }
}
