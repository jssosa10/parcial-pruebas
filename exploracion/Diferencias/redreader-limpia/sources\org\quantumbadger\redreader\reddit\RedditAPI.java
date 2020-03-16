package org.quantumbadger.redreader.reddit;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.google.android.exoplayer2.upstream.DataSchemeDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.activities.PMSendActivity;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategy;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.TimestampBound;
import org.quantumbadger.redreader.http.HTTPBackend.PostField;
import org.quantumbadger.redreader.io.RequestResponseHandler;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedArray;
import org.quantumbadger.redreader.jsonwrap.JsonValue;
import org.quantumbadger.redreader.reddit.APIResponseHandler.APIFailureType;
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler;
import org.quantumbadger.redreader.reddit.APIResponseHandler.UserResponseHandler;
import org.quantumbadger.redreader.reddit.api.SubredditRequestFailure;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;
import org.quantumbadger.redreader.reddit.things.RedditThing;

public final class RedditAPI {
    public static final int ACTION_DELETE = 8;
    public static final int ACTION_DOWNVOTE = 2;
    public static final int ACTION_HIDE = 4;
    public static final int ACTION_REPORT = 7;
    public static final int ACTION_SAVE = 3;
    public static final int ACTION_UNHIDE = 6;
    public static final int ACTION_UNSAVE = 5;
    public static final int ACTION_UNVOTE = 1;
    public static final int ACTION_UPVOTE = 0;
    public static final int SUBSCRIPTION_ACTION_SUBSCRIBE = 0;
    public static final int SUBSCRIPTION_ACTION_UNSUBSCRIBE = 1;
    private static final String TAG = "RedditAPI";

    private static abstract class APIGetRequest extends CacheRequest {
        public abstract void onJsonParseStarted(JsonValue jsonValue, long j, UUID uuid, boolean z);

        public APIGetRequest(URI url, RedditAccount user, int priority, int fileType, DownloadStrategy downloadStrategy, boolean cache, boolean cancelExisting, Context context) {
            super(url, user, null, priority, 0, downloadStrategy, fileType, 0, true, null, cache, cancelExisting, context);
        }

        /* access modifiers changed from: protected */
        public final void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
            if (!this.cache) {
                throw new RuntimeException("onSuccess called for uncached request");
            }
        }

        /* access modifiers changed from: protected */
        public final void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
        }
    }

    private static abstract class APIPostRequest extends CacheRequest {
        public abstract void onJsonParseStarted(JsonValue jsonValue, long j, UUID uuid, boolean z);

        /* access modifiers changed from: protected */
        public void onDownloadNecessary() {
        }

        /* access modifiers changed from: protected */
        public void onDownloadStarted() {
        }

        public APIPostRequest(URI url, RedditAccount user, List<PostField> postFields, Context context) {
            super(url, user, null, -500, 0, DownloadStrategyAlways.INSTANCE, -1, 0, true, postFields, false, false, context);
        }

        /* access modifiers changed from: protected */
        public final void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
            throw new RuntimeException("onSuccess called for uncached request");
        }

        /* access modifiers changed from: protected */
        public final void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RedditAction {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface RedditSubredditAction {
    }

    public static void submit(CacheManager cm, ActionResponseHandler responseHandler, RedditAccount user, boolean is_self, String subreddit, String title, String body, boolean sendRepliesToInbox, boolean markAsNsfw, boolean markAsSpoiler, Context context) {
        String str = body;
        LinkedList<PostField> postFields = new LinkedList<>();
        postFields.add(new PostField("kind", is_self ? "self" : "link"));
        postFields.add(new PostField("sendreplies", sendRepliesToInbox ? "true" : "false"));
        postFields.add(new PostField("nsfw", markAsNsfw ? "true" : "false"));
        postFields.add(new PostField("spoiler", markAsSpoiler ? "true" : "false"));
        String str2 = subreddit;
        postFields.add(new PostField("sr", subreddit));
        postFields.add(new PostField("title", title));
        if (is_self) {
            postFields.add(new PostField(MimeTypes.BASE_TYPE_TEXT, str));
        } else {
            postFields.add(new PostField("url", str));
        }
        final ActionResponseHandler actionResponseHandler = responseHandler;
        AnonymousClass1 r2 = new APIPostRequest(Reddit.getUri("/api/submit"), user, postFields, context) {
            public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
                System.out.println(result.toString());
                try {
                    APIFailureType failureType = RedditAPI.findFailureType(result);
                    if (failureType != null) {
                        actionResponseHandler.notifyFailure(failureType);
                        return;
                    }
                } catch (Throwable t) {
                    notifyFailure(6, t, null, "JSON failed to parse");
                }
                actionResponseHandler.notifySuccess(RedditAPI.findRedirectUrl(result));
            }

            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                BugReportActivity.handleGlobalError(this.context, t);
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                actionResponseHandler.notifyFailure(type, t, status, readableMessage);
            }
        };
        CacheManager cacheManager = cm;
        cm.makeRequest(r2);
    }

    public static void compose(@NonNull CacheManager cm, @NonNull ActionResponseHandler responseHandler, @NonNull RedditAccount user, @NonNull String recipient, @NonNull String subject, @NonNull String body, @NonNull Context context) {
        LinkedList<PostField> postFields = new LinkedList<>();
        postFields.add(new PostField("api_type", "json"));
        postFields.add(new PostField(PMSendActivity.EXTRA_SUBJECT, subject));
        postFields.add(new PostField("to", recipient));
        postFields.add(new PostField(MimeTypes.BASE_TYPE_TEXT, body));
        final ActionResponseHandler actionResponseHandler = responseHandler;
        AnonymousClass2 r1 = new APIPostRequest(Reddit.getUri("/api/compose"), user, postFields, context) {
            public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
                System.out.println(result.toString());
                try {
                    APIFailureType failureType = RedditAPI.findFailureType(result);
                    if (failureType != null) {
                        actionResponseHandler.notifyFailure(failureType);
                        return;
                    }
                } catch (Throwable t) {
                    notifyFailure(6, t, null, "JSON failed to parse");
                }
                actionResponseHandler.notifySuccess(null);
            }

            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                BugReportActivity.handleGlobalError(this.context, t);
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                actionResponseHandler.notifyFailure(type, t, status, readableMessage);
            }
        };
        cm.makeRequest(r1);
    }

    public static void comment(CacheManager cm, ActionResponseHandler responseHandler, ActionResponseHandler inboxResponseHandler, RedditAccount user, String parentIdAndType, String markdown, boolean sendRepliesToInbox, Context context) {
        LinkedList<PostField> postFields = new LinkedList<>();
        postFields.add(new PostField("thing_id", parentIdAndType));
        postFields.add(new PostField(MimeTypes.BASE_TYPE_TEXT, markdown));
        final ActionResponseHandler actionResponseHandler = responseHandler;
        final boolean z = sendRepliesToInbox;
        final CacheManager cacheManager = cm;
        final ActionResponseHandler actionResponseHandler2 = inboxResponseHandler;
        AnonymousClass3 r1 = new APIPostRequest(Reddit.getUri("/api/comment"), user, postFields, context) {
            public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
                System.out.println(result.toString());
                try {
                    APIFailureType failureType = RedditAPI.findFailureType(result);
                    if (failureType != null) {
                        actionResponseHandler.notifyFailure(failureType);
                        return;
                    }
                    if (!z) {
                        String commentFullname = RedditAPI.findThingIdFromCommentResponse(result);
                        if (commentFullname != null && commentFullname.length() > 0) {
                            RedditAPI.sendReplies(cacheManager, actionResponseHandler2, this.user, commentFullname, false, this.context);
                        }
                    }
                    String permalink = RedditAPI.findStringValue(result, "permalink");
                    if (permalink != null && !permalink.contains("?")) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(permalink);
                        sb.append("?context=1");
                        permalink = sb.toString();
                    }
                    actionResponseHandler.notifySuccess(permalink);
                } catch (Throwable t) {
                    notifyFailure(6, t, null, "JSON failed to parse");
                }
            }

            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                BugReportActivity.handleGlobalError(this.context, t);
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                actionResponseHandler.notifyFailure(type, t, status, readableMessage);
            }
        };
        CacheManager cacheManager2 = cm;
        cm.makeRequest(r1);
    }

    public static void markAllAsRead(CacheManager cm, ActionResponseHandler responseHandler, RedditAccount user, Context context) {
        final ActionResponseHandler actionResponseHandler = responseHandler;
        AnonymousClass4 r0 = new APIPostRequest(Reddit.getUri("/api/read_all_messages"), user, new LinkedList<>(), context) {
            public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
                try {
                    APIFailureType failureType = RedditAPI.findFailureType(result);
                    if (failureType != null) {
                        actionResponseHandler.notifyFailure(failureType);
                        return;
                    }
                } catch (Throwable t) {
                    notifyFailure(6, t, null, "JSON failed to parse");
                }
                actionResponseHandler.notifySuccess(null);
            }

            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                BugReportActivity.handleGlobalError(this.context, t);
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                actionResponseHandler.notifyFailure(type, t, status, readableMessage);
            }
        };
        cm.makeRequest(r0);
    }

    public static void editComment(CacheManager cm, ActionResponseHandler responseHandler, RedditAccount user, String commentIdAndType, String markdown, Context context) {
        LinkedList<PostField> postFields = new LinkedList<>();
        postFields.add(new PostField("thing_id", commentIdAndType));
        postFields.add(new PostField(MimeTypes.BASE_TYPE_TEXT, markdown));
        final ActionResponseHandler actionResponseHandler = responseHandler;
        AnonymousClass5 r1 = new APIPostRequest(Reddit.getUri("/api/editusertext"), user, postFields, context) {
            public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
                try {
                    APIFailureType failureType = RedditAPI.findFailureType(result);
                    if (failureType != null) {
                        actionResponseHandler.notifyFailure(failureType);
                        return;
                    }
                } catch (Throwable t) {
                    notifyFailure(6, t, null, "JSON failed to parse");
                }
                actionResponseHandler.notifySuccess(null);
            }

            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                BugReportActivity.handleGlobalError(this.context, t);
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                actionResponseHandler.notifyFailure(type, t, status, readableMessage);
            }
        };
        cm.makeRequest(r1);
    }

    public static void action(CacheManager cm, ActionResponseHandler responseHandler, RedditAccount user, String idAndType, int action, Context context) {
        LinkedList<PostField> postFields = new LinkedList<>();
        postFields.add(new PostField(TtmlNode.ATTR_ID, idAndType));
        final ActionResponseHandler actionResponseHandler = responseHandler;
        AnonymousClass6 r1 = new APIPostRequest(prepareActionUri(action, postFields), user, postFields, context) {
            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                BugReportActivity.handleGlobalError(this.context, t);
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                actionResponseHandler.notifyFailure(type, t, status, readableMessage);
            }

            public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
                try {
                    APIFailureType failureType = RedditAPI.findFailureType(result);
                    if (failureType != null) {
                        actionResponseHandler.notifyFailure(failureType);
                        return;
                    }
                } catch (Throwable t) {
                    notifyFailure(6, t, null, "JSON failed to parse");
                }
                actionResponseHandler.notifySuccess(null);
            }
        };
        cm.makeRequest(r1);
    }

    private static URI prepareActionUri(int action, LinkedList<PostField> postFields) {
        switch (action) {
            case 0:
                postFields.add(new PostField("dir", "1"));
                return Reddit.getUri(Reddit.PATH_VOTE);
            case 1:
                postFields.add(new PostField("dir", "0"));
                return Reddit.getUri(Reddit.PATH_VOTE);
            case 2:
                postFields.add(new PostField("dir", "-1"));
                return Reddit.getUri(Reddit.PATH_VOTE);
            case 3:
                return Reddit.getUri(Reddit.PATH_SAVE);
            case 4:
                return Reddit.getUri(Reddit.PATH_HIDE);
            case 5:
                return Reddit.getUri(Reddit.PATH_UNSAVE);
            case 6:
                return Reddit.getUri(Reddit.PATH_UNHIDE);
            case 7:
                return Reddit.getUri(Reddit.PATH_REPORT);
            case 8:
                return Reddit.getUri(Reddit.PATH_DELETE);
            default:
                throw new RuntimeException("Unknown post/comment action");
        }
    }

    public static void subscriptionAction(CacheManager cm, ActionResponseHandler responseHandler, RedditAccount user, String subredditCanonicalName, int action, Context context) {
        RedditSubredditManager instance = RedditSubredditManager.getInstance(context, user);
        TimestampBound timestampBound = TimestampBound.ANY;
        final ActionResponseHandler actionResponseHandler = responseHandler;
        final int i = action;
        final CacheManager cacheManager = cm;
        final RedditAccount redditAccount = user;
        final Context context2 = context;
        AnonymousClass7 r2 = new RequestResponseHandler<RedditSubreddit, SubredditRequestFailure>() {
            public void onRequestFailed(SubredditRequestFailure failureReason) {
                actionResponseHandler.notifyFailure(failureReason.requestFailureType, failureReason.t, failureReason.statusLine, failureReason.readableMessage);
            }

            public void onRequestSuccess(RedditSubreddit subreddit, long timeCached) {
                LinkedList<PostField> postFields = new LinkedList<>();
                postFields.add(new PostField("sr", subreddit.name));
                URI url = RedditAPI.subscriptionPrepareActionUri(i, postFields);
                CacheManager cacheManager = cacheManager;
                AnonymousClass1 r1 = new APIPostRequest(url, redditAccount, postFields, context2) {
                    /* access modifiers changed from: protected */
                    public void onCallbackException(Throwable t) {
                        BugReportActivity.handleGlobalError(this.context, t);
                    }

                    /* access modifiers changed from: protected */
                    public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                        actionResponseHandler.notifyFailure(type, t, status, readableMessage);
                    }

                    public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
                        try {
                            APIFailureType failureType = RedditAPI.findFailureType(result);
                            if (failureType != null) {
                                actionResponseHandler.notifyFailure(failureType);
                                return;
                            }
                        } catch (Throwable t) {
                            notifyFailure(6, t, null, "JSON failed to parse");
                        }
                        actionResponseHandler.notifySuccess(null);
                    }
                };
                cacheManager.makeRequest(r1);
            }
        };
        instance.getSubreddit(subredditCanonicalName, timestampBound, r2, null);
    }

    /* access modifiers changed from: private */
    public static URI subscriptionPrepareActionUri(int action, LinkedList<PostField> postFields) {
        switch (action) {
            case 0:
                postFields.add(new PostField("action", "sub"));
                return Reddit.getUri(Reddit.PATH_SUBSCRIBE);
            case 1:
                postFields.add(new PostField("action", "unsub"));
                return Reddit.getUri(Reddit.PATH_SUBSCRIBE);
            default:
                throw new RuntimeException("Unknown subreddit action");
        }
    }

    public static void getUser(CacheManager cm, String usernameToGet, UserResponseHandler responseHandler, RedditAccount user, DownloadStrategy downloadStrategy, boolean cancelExisting, Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append("/user/");
        String str = usernameToGet;
        sb.append(usernameToGet);
        sb.append("/about.json");
        final UserResponseHandler userResponseHandler = responseHandler;
        AnonymousClass8 r2 = new APIGetRequest(Reddit.getUri(sb.toString()), user, -500, 130, downloadStrategy, true, cancelExisting, context) {
            /* access modifiers changed from: protected */
            public void onDownloadNecessary() {
            }

            /* access modifiers changed from: protected */
            public void onDownloadStarted() {
                userResponseHandler.notifyDownloadStarted();
            }

            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                BugReportActivity.handleGlobalError(this.context, t);
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                userResponseHandler.notifyFailure(type, t, status, readableMessage);
            }

            public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
                try {
                    userResponseHandler.notifySuccess(((RedditThing) result.asObject(RedditThing.class)).asUser(), timestamp);
                } catch (Throwable t) {
                    notifyFailure(6, t, null, "JSON parse failed for unknown reason");
                }
            }
        };
        CacheManager cacheManager = cm;
        cm.makeRequest(r2);
    }

    public static void sendReplies(CacheManager cm, ActionResponseHandler responseHandler, RedditAccount user, String fullname, boolean state, Context context) {
        LinkedList<PostField> postFields = new LinkedList<>();
        postFields.add(new PostField(TtmlNode.ATTR_ID, fullname));
        postFields.add(new PostField("state", String.valueOf(state)));
        final ActionResponseHandler actionResponseHandler = responseHandler;
        AnonymousClass9 r1 = new APIPostRequest(Reddit.getUri("/api/sendreplies"), user, postFields, context) {
            public void onJsonParseStarted(JsonValue result, long timestamp, UUID session, boolean fromCache) {
                try {
                    APIFailureType failureType = RedditAPI.findFailureType(result);
                    if (failureType != null) {
                        actionResponseHandler.notifyFailure(failureType);
                        return;
                    }
                } catch (Throwable t) {
                    notifyFailure(6, t, null, "JSON failed to parse");
                }
                actionResponseHandler.notifySuccess(null);
            }

            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                BugReportActivity.handleGlobalError(this.context, t);
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                actionResponseHandler.notifyFailure(type, t, status, readableMessage);
            }
        };
        cm.makeRequest(r1);
    }

    /* access modifiers changed from: private */
    public static String findThingIdFromCommentResponse(JsonValue response) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("t1_");
            sb.append(response.asObject().getArray("jquery").getArray(30).getArray(3).getArray(0).getObject(0).getObject(DataSchemeDataSource.SCHEME_DATA).getString(TtmlNode.ATTR_ID));
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to find comment thing ID", e);
            return null;
        }
    }

    /* access modifiers changed from: private */
    public static String findRedirectUrl(JsonValue response) {
        String lastAttr = null;
        try {
            Iterator it = response.asObject().getArray("jquery").iterator();
            while (it.hasNext()) {
                JsonValue elem = (JsonValue) it.next();
                if (elem.getType() == 1) {
                    JsonBufferedArray arr = elem.asArray();
                    if ("attr".equals(arr.getString(2))) {
                        lastAttr = arr.getString(3);
                    } else if (NotificationCompat.CATEGORY_CALL.equals(arr.getString(2)) && "redirect".equals(lastAttr)) {
                        return arr.getArray(3).getString(0);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to find redirect URL", e);
        }
        return null;
    }

    /* access modifiers changed from: private */
    @Nullable
    public static String findStringValue(JsonValue response, String key) {
        if (response == null) {
            return null;
        }
        switch (response.getType()) {
            case 0:
                Iterator it = response.asObject().iterator();
                while (it.hasNext()) {
                    Entry<String, JsonValue> v = (Entry) it.next();
                    if (key.equalsIgnoreCase((String) v.getKey()) && ((JsonValue) v.getValue()).getType() == 4) {
                        return ((JsonValue) v.getValue()).asString();
                    }
                    String result = findStringValue((JsonValue) v.getValue(), key);
                    if (result != null) {
                        return result;
                    }
                }
                break;
            case 1:
                Iterator it2 = response.asArray().iterator();
                while (it2.hasNext()) {
                    String result2 = findStringValue((JsonValue) it2.next(), key);
                    if (result2 != null) {
                        return result2;
                    }
                }
                break;
        }
        return null;
    }

    /* access modifiers changed from: private */
    public static APIFailureType findFailureType(JsonValue response) {
        APIFailureType aPIFailureType = null;
        if (response == null) {
            return null;
        }
        boolean unknownError = false;
        int type = response.getType();
        if (type != 4) {
            switch (type) {
                case 0:
                    Iterator it = response.asObject().iterator();
                    while (it.hasNext()) {
                        Entry<String, JsonValue> v = (Entry) it.next();
                        if ("success".equals(v.getKey()) && ((JsonValue) v.getValue()).getType() == 3 && Boolean.FALSE.equals(((JsonValue) v.getValue()).asBoolean())) {
                            unknownError = true;
                        }
                        APIFailureType failureType = findFailureType((JsonValue) v.getValue());
                        if (failureType == APIFailureType.UNKNOWN) {
                            unknownError = true;
                        } else if (failureType != null) {
                            return failureType;
                        }
                    }
                    try {
                        JsonBufferedArray errors = response.asObject().getObject("json").getArray("errors");
                        if (errors != null) {
                            errors.join();
                            if (errors.getCurrentItemCount() > 0) {
                                unknownError = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        break;
                    }
                    break;
                case 1:
                    Iterator it2 = response.asArray().iterator();
                    while (it2.hasNext()) {
                        APIFailureType failureType2 = findFailureType((JsonValue) it2.next());
                        if (failureType2 == APIFailureType.UNKNOWN) {
                            unknownError = true;
                        } else if (failureType2 != null) {
                            return failureType2;
                        }
                    }
                    break;
            }
        } else {
            String responseAsString = response.asString();
            if (Reddit.isApiErrorUser(responseAsString)) {
                return APIFailureType.INVALID_USER;
            }
            if (Reddit.isApiErrorCaptcha(responseAsString)) {
                return APIFailureType.BAD_CAPTCHA;
            }
            if (Reddit.isApiErrorNotAllowed(responseAsString)) {
                return APIFailureType.NOTALLOWED;
            }
            if (Reddit.isApiErrorSubredditRequired(responseAsString)) {
                return APIFailureType.SUBREDDIT_REQUIRED;
            }
            if (Reddit.isApiErrorURLRequired(responseAsString)) {
                return APIFailureType.URL_REQUIRED;
            }
            if (Reddit.isApiTooFast(responseAsString)) {
                return APIFailureType.TOO_FAST;
            }
            if (Reddit.isApiTooLong(responseAsString)) {
                return APIFailureType.TOO_LONG;
            }
            if (Reddit.isApiAlreadySubmitted(responseAsString)) {
                return APIFailureType.ALREADY_SUBMITTED;
            }
            if (Reddit.isApiError(responseAsString)) {
                unknownError = true;
            }
        }
        if (unknownError) {
            aPIFailureType = APIFailureType.UNKNOWN;
        }
        return aPIFailureType;
    }
}
