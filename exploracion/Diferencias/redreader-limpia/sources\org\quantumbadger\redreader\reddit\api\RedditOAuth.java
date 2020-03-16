package org.quantumbadger.redreader.reddit.api;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.SystemClock;
import android.util.Base64;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.http.HTTPBackend;
import org.quantumbadger.redreader.http.HTTPBackend.Listener;
import org.quantumbadger.redreader.http.HTTPBackend.PostField;
import org.quantumbadger.redreader.http.HTTPBackend.Request;
import org.quantumbadger.redreader.http.HTTPBackend.RequestDetails;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedObject;
import org.quantumbadger.redreader.jsonwrap.JsonValue;

public final class RedditOAuth {
    private static final String ACCESS_TOKEN_URL = "https://www.reddit.com/api/v1/access_token";
    private static final String ALL_SCOPES = "identity,edit,flair,history,modconfig,modflair,modlog,modposts,modwiki,mysubreddits,privatemessages,read,report,save,submit,subscribe,vote,wikiedit,wikiread";
    private static final String CLIENT_ID = "m_zCW1Dixs9WLA";
    private static final String REDIRECT_URI = "http://rr_oauth_redir";

    public static final class AccessToken extends Token {
        private final long mMonotonicTimestamp = SystemClock.elapsedRealtime();

        public AccessToken(String token) {
            super(token);
        }

        public boolean isExpired() {
            return this.mMonotonicTimestamp + 1800000 < SystemClock.elapsedRealtime();
        }
    }

    public static final class FetchAccessTokenResult {
        public final AccessToken accessToken;
        public final RRError error;
        public final FetchAccessTokenResultStatus status;

        public FetchAccessTokenResult(FetchAccessTokenResultStatus status2, RRError error2) {
            this.status = status2;
            this.error = error2;
            this.accessToken = null;
        }

        public FetchAccessTokenResult(AccessToken accessToken2) {
            this.status = FetchAccessTokenResultStatus.SUCCESS;
            this.error = null;
            this.accessToken = accessToken2;
        }
    }

    public enum FetchAccessTokenResultStatus {
        SUCCESS,
        INVALID_REQUEST,
        INVALID_RESPONSE,
        CONNECTION_ERROR,
        UNKNOWN_ERROR
    }

    private static final class FetchRefreshTokenResult {
        public final AccessToken accessToken;
        public final RRError error;
        public final RefreshToken refreshToken;
        public final FetchRefreshTokenResultStatus status;

        public FetchRefreshTokenResult(FetchRefreshTokenResultStatus status2, RRError error2) {
            this.status = status2;
            this.error = error2;
            this.refreshToken = null;
            this.accessToken = null;
        }

        public FetchRefreshTokenResult(RefreshToken refreshToken2, AccessToken accessToken2) {
            this.status = FetchRefreshTokenResultStatus.SUCCESS;
            this.error = null;
            this.refreshToken = refreshToken2;
            this.accessToken = accessToken2;
        }
    }

    private enum FetchRefreshTokenResultStatus {
        SUCCESS,
        USER_REFUSED_PERMISSION,
        INVALID_REQUEST,
        INVALID_RESPONSE,
        CONNECTION_ERROR,
        UNKNOWN_ERROR
    }

    private static final class FetchUserInfoResult {
        public final RRError error;
        public final FetchUserInfoResultStatus status;
        public final String username;

        public FetchUserInfoResult(FetchUserInfoResultStatus status2, RRError error2) {
            this.status = status2;
            this.error = error2;
            this.username = null;
        }

        public FetchUserInfoResult(String username2) {
            this.status = FetchUserInfoResultStatus.SUCCESS;
            this.error = null;
            this.username = username2;
        }
    }

    private enum FetchUserInfoResultStatus {
        SUCCESS,
        INVALID_RESPONSE,
        CONNECTION_ERROR,
        UNKNOWN_ERROR
    }

    public enum LoginError {
        SUCCESS,
        USER_REFUSED_PERMISSION,
        CONNECTION_ERROR,
        UNKNOWN_ERROR;

        static LoginError fromFetchRefreshTokenStatus(FetchRefreshTokenResultStatus status) {
            switch (status) {
                case SUCCESS:
                    return SUCCESS;
                case USER_REFUSED_PERMISSION:
                    return USER_REFUSED_PERMISSION;
                case INVALID_REQUEST:
                    return UNKNOWN_ERROR;
                case INVALID_RESPONSE:
                    return UNKNOWN_ERROR;
                case CONNECTION_ERROR:
                    return CONNECTION_ERROR;
                case UNKNOWN_ERROR:
                    return UNKNOWN_ERROR;
                default:
                    return UNKNOWN_ERROR;
            }
        }

        static LoginError fromFetchUserInfoStatus(FetchUserInfoResultStatus status) {
            switch (status) {
                case SUCCESS:
                    return SUCCESS;
                case INVALID_RESPONSE:
                    return UNKNOWN_ERROR;
                case CONNECTION_ERROR:
                    return CONNECTION_ERROR;
                case UNKNOWN_ERROR:
                    return UNKNOWN_ERROR;
                default:
                    return UNKNOWN_ERROR;
            }
        }
    }

    public interface LoginListener {
        void onLoginFailure(LoginError loginError, RRError rRError);

        void onLoginSuccess(RedditAccount redditAccount);
    }

    public static final class RefreshToken extends Token {
        public RefreshToken(String token) {
            super(token);
        }
    }

    public static class Token {
        public final String token;

        public Token(String token2) {
            this.token = token2;
        }

        public String toString() {
            return this.token;
        }
    }

    public static Uri getPromptUri() {
        Builder uri = Uri.parse("https://www.reddit.com/api/v1/authorize.compact").buildUpon();
        uri.appendQueryParameter("response_type", "code");
        uri.appendQueryParameter("duration", "permanent");
        uri.appendQueryParameter("state", "Texas");
        uri.appendQueryParameter("redirect_uri", REDIRECT_URI);
        uri.appendQueryParameter("client_id", CLIENT_ID);
        uri.appendQueryParameter("scope", ALL_SCOPES);
        return uri.build();
    }

    /* access modifiers changed from: private */
    public static FetchRefreshTokenResult handleRefreshTokenError(Throwable exception, Integer httpStatus, Context context, String uri) {
        if (httpStatus != null && httpStatus.intValue() != 200) {
            FetchRefreshTokenResultStatus fetchRefreshTokenResultStatus = FetchRefreshTokenResultStatus.UNKNOWN_ERROR;
            RRError rRError = new RRError(context.getString(R.string.error_unknown_title), context.getString(R.string.message_cannotlogin), null, httpStatus, uri);
            return new FetchRefreshTokenResult(fetchRefreshTokenResultStatus, rRError);
        } else if (exception == null || !(exception instanceof IOException)) {
            FetchRefreshTokenResultStatus fetchRefreshTokenResultStatus2 = FetchRefreshTokenResultStatus.UNKNOWN_ERROR;
            RRError rRError2 = new RRError(context.getString(R.string.error_unknown_title), context.getString(R.string.error_unknown_message), exception, null, uri);
            return new FetchRefreshTokenResult(fetchRefreshTokenResultStatus2, rRError2);
        } else {
            FetchRefreshTokenResultStatus fetchRefreshTokenResultStatus3 = FetchRefreshTokenResultStatus.CONNECTION_ERROR;
            RRError rRError3 = new RRError(context.getString(R.string.error_connection_title), context.getString(R.string.error_connection_message), exception, null, uri);
            return new FetchRefreshTokenResult(fetchRefreshTokenResultStatus3, rRError3);
        }
    }

    /* access modifiers changed from: private */
    public static FetchAccessTokenResult handleAccessTokenError(Throwable exception, Integer httpStatus, Context context, String uri) {
        if (httpStatus != null && httpStatus.intValue() != 200) {
            FetchAccessTokenResultStatus fetchAccessTokenResultStatus = FetchAccessTokenResultStatus.UNKNOWN_ERROR;
            RRError rRError = new RRError(context.getString(R.string.error_unknown_title), context.getString(R.string.message_cannotlogin), null, httpStatus, uri);
            return new FetchAccessTokenResult(fetchAccessTokenResultStatus, rRError);
        } else if (exception == null || !(exception instanceof IOException)) {
            FetchAccessTokenResultStatus fetchAccessTokenResultStatus2 = FetchAccessTokenResultStatus.UNKNOWN_ERROR;
            RRError rRError2 = new RRError(context.getString(R.string.error_unknown_title), context.getString(R.string.error_unknown_message), exception, null, uri);
            return new FetchAccessTokenResult(fetchAccessTokenResultStatus2, rRError2);
        } else {
            FetchAccessTokenResultStatus fetchAccessTokenResultStatus3 = FetchAccessTokenResultStatus.CONNECTION_ERROR;
            RRError rRError3 = new RRError(context.getString(R.string.error_connection_title), context.getString(R.string.error_connection_message), exception, null, uri);
            return new FetchAccessTokenResult(fetchAccessTokenResultStatus3, rRError3);
        }
    }

    /* access modifiers changed from: private */
    public static FetchRefreshTokenResult fetchRefreshTokenSynchronous(Context context, Uri redirectUri) {
        final Context context2 = context;
        Uri uri = redirectUri;
        String error = uri.getQueryParameter("error");
        if (error == null) {
            String code = uri.getQueryParameter("code");
            if (code == null) {
                return new FetchRefreshTokenResult(FetchRefreshTokenResultStatus.INVALID_RESPONSE, new RRError(context2.getString(R.string.error_unknown_title), context2.getString(R.string.error_unknown_message)));
            }
            String str = ACCESS_TOKEN_URL;
            ArrayList arrayList = new ArrayList(3);
            arrayList.add(new PostField("grant_type", "authorization_code"));
            arrayList.add(new PostField("code", code));
            arrayList.add(new PostField("redirect_uri", REDIRECT_URI));
            try {
                Request request = HTTPBackend.getBackend().prepareRequest(context2, new RequestDetails(General.uriFromString(ACCESS_TOKEN_URL), arrayList));
                StringBuilder sb = new StringBuilder();
                sb.append("Basic ");
                sb.append(Base64.encodeToString("m_zCW1Dixs9WLA:".getBytes(), 10));
                request.addHeader("Authorization", sb.toString());
                final AtomicReference<FetchRefreshTokenResult> result = new AtomicReference<>();
                request.executeInThisThread(new Listener() {
                    public void onError(int failureType, Throwable exception, Integer httpStatus) {
                        result.set(RedditOAuth.handleRefreshTokenError(exception, httpStatus, context2, RedditOAuth.ACCESS_TOKEN_URL));
                    }

                    public void onSuccess(String mimetype, Long bodyBytes, InputStream body) {
                        try {
                            JsonValue jsonValue = new JsonValue(body);
                            jsonValue.buildInThisThread();
                            JsonBufferedObject responseObject = jsonValue.asObject();
                            result.set(new FetchRefreshTokenResult(new RefreshToken(responseObject.getString("refresh_token")), new AccessToken(responseObject.getString("access_token"))));
                        } catch (IOException e) {
                            IOException e2 = e;
                            AtomicReference atomicReference = result;
                            FetchRefreshTokenResultStatus fetchRefreshTokenResultStatus = FetchRefreshTokenResultStatus.CONNECTION_ERROR;
                            RRError rRError = new RRError(context2.getString(R.string.error_connection_title), context2.getString(R.string.error_connection_message), e2, null, RedditOAuth.ACCESS_TOKEN_URL);
                            atomicReference.set(new FetchRefreshTokenResult(fetchRefreshTokenResultStatus, rRError));
                        } catch (Throwable t) {
                            throw new RuntimeException(t);
                        }
                    }
                });
                return (FetchRefreshTokenResult) result.get();
            } catch (Throwable th) {
                Throwable t = th;
                FetchRefreshTokenResultStatus fetchRefreshTokenResultStatus = FetchRefreshTokenResultStatus.UNKNOWN_ERROR;
                RRError rRError = new RRError(context2.getString(R.string.error_unknown_title), context2.getString(R.string.error_unknown_message), t, null, ACCESS_TOKEN_URL);
                return new FetchRefreshTokenResult(fetchRefreshTokenResultStatus, rRError);
            }
        } else if (error.equals("access_denied")) {
            return new FetchRefreshTokenResult(FetchRefreshTokenResultStatus.USER_REFUSED_PERMISSION, new RRError(context2.getString(R.string.error_title_login_user_denied_permission), context2.getString(R.string.error_message_login_user_denied_permission)));
        } else {
            return new FetchRefreshTokenResult(FetchRefreshTokenResultStatus.INVALID_REQUEST, new RRError(context2.getString(R.string.error_title_login_unknown_reddit_error, new Object[]{error}), context2.getString(R.string.error_unknown_message)));
        }
    }

    /* access modifiers changed from: private */
    public static FetchUserInfoResult fetchUserInfoSynchronous(final Context context, AccessToken accessToken) {
        final URI uri = Reddit.getUri(Reddit.PATH_ME);
        try {
            Request request = HTTPBackend.getBackend().prepareRequest(context, new RequestDetails(uri, null));
            StringBuilder sb = new StringBuilder();
            sb.append("bearer ");
            sb.append(accessToken.token);
            request.addHeader("Authorization", sb.toString());
            final AtomicReference<FetchUserInfoResult> result = new AtomicReference<>();
            request.executeInThisThread(new Listener() {
                public void onError(int failureType, Throwable exception, Integer httpStatus) {
                    if (httpStatus == null || httpStatus.intValue() == 200) {
                        AtomicReference atomicReference = result;
                        FetchUserInfoResultStatus fetchUserInfoResultStatus = FetchUserInfoResultStatus.UNKNOWN_ERROR;
                        RRError rRError = new RRError(context.getString(R.string.error_unknown_title), context.getString(R.string.error_unknown_message), exception, null, uri.toString());
                        atomicReference.set(new FetchUserInfoResult(fetchUserInfoResultStatus, rRError));
                        return;
                    }
                    AtomicReference atomicReference2 = result;
                    FetchUserInfoResultStatus fetchUserInfoResultStatus2 = FetchUserInfoResultStatus.CONNECTION_ERROR;
                    RRError rRError2 = new RRError(context.getString(R.string.error_unknown_title), context.getString(R.string.error_unknown_message), null, httpStatus, uri.toString());
                    atomicReference2.set(new FetchUserInfoResult(fetchUserInfoResultStatus2, rRError2));
                }

                public void onSuccess(String mimetype, Long bodyBytes, InputStream body) {
                    try {
                        try {
                            JsonValue jsonValue = new JsonValue(body);
                            jsonValue.buildInThisThread();
                            String username = jsonValue.asObject().getString("name");
                            if (username != null) {
                                if (username.length() != 0) {
                                    result.set(new FetchUserInfoResult(username));
                                }
                            }
                            AtomicReference atomicReference = result;
                            FetchUserInfoResultStatus fetchUserInfoResultStatus = FetchUserInfoResultStatus.INVALID_RESPONSE;
                            RRError rRError = new RRError(context.getString(R.string.error_unknown_title), context.getString(R.string.error_unknown_message), null, null, uri.toString());
                            atomicReference.set(new FetchUserInfoResult(fetchUserInfoResultStatus, rRError));
                        } catch (IOException e) {
                            e = e;
                            IOException e2 = e;
                            AtomicReference atomicReference2 = result;
                            FetchUserInfoResultStatus fetchUserInfoResultStatus2 = FetchUserInfoResultStatus.CONNECTION_ERROR;
                            RRError rRError2 = new RRError(context.getString(R.string.error_connection_title), context.getString(R.string.error_connection_message), e2, null, uri.toString());
                            atomicReference2.set(new FetchUserInfoResult(fetchUserInfoResultStatus2, rRError2));
                        } catch (Throwable th) {
                            t = th;
                            throw new RuntimeException(t);
                        }
                    } catch (IOException e3) {
                        e = e3;
                        InputStream inputStream = body;
                        IOException e22 = e;
                        AtomicReference atomicReference22 = result;
                        FetchUserInfoResultStatus fetchUserInfoResultStatus22 = FetchUserInfoResultStatus.CONNECTION_ERROR;
                        RRError rRError22 = new RRError(context.getString(R.string.error_connection_title), context.getString(R.string.error_connection_message), e22, null, uri.toString());
                        atomicReference22.set(new FetchUserInfoResult(fetchUserInfoResultStatus22, rRError22));
                    } catch (Throwable th2) {
                        t = th2;
                        InputStream inputStream2 = body;
                        throw new RuntimeException(t);
                    }
                }
            });
            return (FetchUserInfoResult) result.get();
        } catch (Throwable th) {
            Throwable t = th;
            FetchUserInfoResultStatus fetchUserInfoResultStatus = FetchUserInfoResultStatus.UNKNOWN_ERROR;
            RRError rRError = new RRError(context.getString(R.string.error_unknown_title), context.getString(R.string.error_unknown_message), t, null, uri.toString());
            return new FetchUserInfoResult(fetchUserInfoResultStatus, rRError);
        }
    }

    public static void loginAsynchronous(final Context context, final Uri redirectUri, final LoginListener listener) {
        new Thread() {
            public void run() {
                try {
                    FetchRefreshTokenResult fetchRefreshTokenResult = RedditOAuth.fetchRefreshTokenSynchronous(context, redirectUri);
                    if (fetchRefreshTokenResult.status != FetchRefreshTokenResultStatus.SUCCESS) {
                        listener.onLoginFailure(LoginError.fromFetchRefreshTokenStatus(fetchRefreshTokenResult.status), fetchRefreshTokenResult.error);
                        return;
                    }
                    FetchUserInfoResult fetchUserInfoResult = RedditOAuth.fetchUserInfoSynchronous(context, fetchRefreshTokenResult.accessToken);
                    if (fetchUserInfoResult.status != FetchUserInfoResultStatus.SUCCESS) {
                        listener.onLoginFailure(LoginError.fromFetchUserInfoStatus(fetchUserInfoResult.status), fetchUserInfoResult.error);
                        return;
                    }
                    RedditAccount account = new RedditAccount(fetchUserInfoResult.username, fetchRefreshTokenResult.refreshToken, 0);
                    account.setAccessToken(fetchRefreshTokenResult.accessToken);
                    RedditAccountManager accountManager = RedditAccountManager.getInstance(context);
                    accountManager.addAccount(account);
                    accountManager.setDefaultAccount(account);
                    listener.onLoginSuccess(account);
                } catch (Throwable t) {
                    listener.onLoginFailure(LoginError.UNKNOWN_ERROR, new RRError(context.getString(R.string.error_unknown_title), context.getString(R.string.error_unknown_message), t));
                }
            }
        }.start();
    }

    public static FetchAccessTokenResult fetchAccessTokenSynchronous(final Context context, RefreshToken refreshToken) {
        String str = ACCESS_TOKEN_URL;
        ArrayList<PostField> postFields = new ArrayList<>(2);
        postFields.add(new PostField("grant_type", "refresh_token"));
        postFields.add(new PostField("refresh_token", refreshToken.token));
        try {
            Request request = HTTPBackend.getBackend().prepareRequest(context, new RequestDetails(General.uriFromString(ACCESS_TOKEN_URL), postFields));
            StringBuilder sb = new StringBuilder();
            sb.append("Basic ");
            sb.append(Base64.encodeToString("m_zCW1Dixs9WLA:".getBytes(), 10));
            request.addHeader("Authorization", sb.toString());
            final AtomicReference<FetchAccessTokenResult> result = new AtomicReference<>();
            request.executeInThisThread(new Listener() {
                public void onError(int failureType, Throwable exception, Integer httpStatus) {
                    result.set(RedditOAuth.handleAccessTokenError(exception, httpStatus, context, RedditOAuth.ACCESS_TOKEN_URL));
                }

                public void onSuccess(String mimetype, Long bodyBytes, InputStream body) {
                    try {
                        JsonValue jsonValue = new JsonValue(body);
                        jsonValue.buildInThisThread();
                        JsonBufferedObject responseObject = jsonValue.asObject();
                        String accessTokenString = responseObject.getString("access_token");
                        if (accessTokenString != null) {
                            result.set(new FetchAccessTokenResult(new AccessToken(accessTokenString)));
                            return;
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("Null access token: ");
                        sb.append(responseObject.getString("error"));
                        throw new RuntimeException(sb.toString());
                    } catch (IOException e) {
                        IOException e2 = e;
                        AtomicReference atomicReference = result;
                        FetchAccessTokenResultStatus fetchAccessTokenResultStatus = FetchAccessTokenResultStatus.CONNECTION_ERROR;
                        RRError rRError = new RRError(context.getString(R.string.error_connection_title), context.getString(R.string.error_connection_message), e2, null, RedditOAuth.ACCESS_TOKEN_URL);
                        atomicReference.set(new FetchAccessTokenResult(fetchAccessTokenResultStatus, rRError));
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                }
            });
            return (FetchAccessTokenResult) result.get();
        } catch (Throwable th) {
            Throwable t = th;
            FetchAccessTokenResultStatus fetchAccessTokenResultStatus = FetchAccessTokenResultStatus.UNKNOWN_ERROR;
            RRError rRError = new RRError(context.getString(R.string.error_unknown_title), context.getString(R.string.error_unknown_message), t, null, ACCESS_TOKEN_URL);
            return new FetchAccessTokenResult(fetchAccessTokenResultStatus, rRError);
        }
    }

    public static FetchAccessTokenResult fetchAnonymousAccessTokenSynchronous(final Context context) {
        String str = ACCESS_TOKEN_URL;
        ArrayList<PostField> postFields = new ArrayList<>(2);
        postFields.add(new PostField("grant_type", "https://oauth.reddit.com/grants/installed_client"));
        postFields.add(new PostField("device_id", "DO_NOT_TRACK_THIS_DEVICE"));
        try {
            Request request = HTTPBackend.getBackend().prepareRequest(context, new RequestDetails(General.uriFromString(ACCESS_TOKEN_URL), postFields));
            StringBuilder sb = new StringBuilder();
            sb.append("Basic ");
            sb.append(Base64.encodeToString("m_zCW1Dixs9WLA:".getBytes(), 10));
            request.addHeader("Authorization", sb.toString());
            final AtomicReference<FetchAccessTokenResult> result = new AtomicReference<>();
            request.executeInThisThread(new Listener() {
                public void onError(int failureType, Throwable exception, Integer httpStatus) {
                    result.set(RedditOAuth.handleAccessTokenError(exception, httpStatus, context, RedditOAuth.ACCESS_TOKEN_URL));
                }

                public void onSuccess(String mimetype, Long bodyBytes, InputStream body) {
                    try {
                        JsonValue jsonValue = new JsonValue(body);
                        jsonValue.buildInThisThread();
                        JsonBufferedObject responseObject = jsonValue.asObject();
                        String accessTokenString = responseObject.getString("access_token");
                        if (accessTokenString != null) {
                            result.set(new FetchAccessTokenResult(new AccessToken(accessTokenString)));
                            return;
                        }
                        StringBuilder sb = new StringBuilder();
                        sb.append("Null access token: ");
                        sb.append(responseObject.getString("error"));
                        throw new RuntimeException(sb.toString());
                    } catch (IOException e) {
                        IOException e2 = e;
                        AtomicReference atomicReference = result;
                        FetchAccessTokenResultStatus fetchAccessTokenResultStatus = FetchAccessTokenResultStatus.CONNECTION_ERROR;
                        RRError rRError = new RRError(context.getString(R.string.error_connection_title), context.getString(R.string.error_connection_message), e2, null, RedditOAuth.ACCESS_TOKEN_URL);
                        atomicReference.set(new FetchAccessTokenResult(fetchAccessTokenResultStatus, rRError));
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                }
            });
            return (FetchAccessTokenResult) result.get();
        } catch (Throwable th) {
            Throwable t = th;
            FetchAccessTokenResultStatus fetchAccessTokenResultStatus = FetchAccessTokenResultStatus.UNKNOWN_ERROR;
            RRError rRError = new RRError(context.getString(R.string.error_unknown_title), context.getString(R.string.message_cannotlogin), t, null, ACCESS_TOKEN_URL);
            return new FetchAccessTokenResult(fetchAccessTokenResultStatus, rRError);
        }
    }
}
