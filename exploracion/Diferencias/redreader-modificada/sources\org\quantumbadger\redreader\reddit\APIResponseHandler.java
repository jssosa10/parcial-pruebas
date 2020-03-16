package org.quantumbadger.redreader.reddit;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import java.util.List;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;
import org.quantumbadger.redreader.reddit.things.RedditUser;

public abstract class APIResponseHandler {
    /* access modifiers changed from: protected */
    public final AppCompatActivity context;

    public enum APIFailureType {
        INVALID_USER,
        BAD_CAPTCHA,
        NOTALLOWED,
        SUBREDDIT_REQUIRED,
        URL_REQUIRED,
        UNKNOWN,
        TOO_FAST,
        TOO_LONG,
        ALREADY_SUBMITTED
    }

    public static abstract class ActionResponseHandler extends APIResponseHandler {
        /* access modifiers changed from: protected */
        public abstract void onSuccess(@Nullable String str);

        protected ActionResponseHandler(AppCompatActivity context) {
            super(context);
        }

        public final void notifySuccess(@Nullable String redirectUrl) {
            try {
                onSuccess(redirectUrl);
            } catch (Throwable t2) {
                BugReportActivity.addGlobalError(new RRError(null, null, t1));
                BugReportActivity.handleGlobalError((Context) this.context, t2);
            }
        }
    }

    public static abstract class NewCaptchaResponseHandler extends APIResponseHandler {
        /* access modifiers changed from: protected */
        public abstract void onSuccess(String str);

        protected NewCaptchaResponseHandler(AppCompatActivity context) {
            super(context);
        }

        public final void notifySuccess(String captchaId) {
            try {
                onSuccess(captchaId);
            } catch (Throwable t2) {
                BugReportActivity.addGlobalError(new RRError(null, null, t1));
                BugReportActivity.handleGlobalError((Context) this.context, t2);
            }
        }
    }

    public static abstract class SubredditResponseHandler extends APIResponseHandler {
        /* access modifiers changed from: protected */
        public abstract void onDownloadNecessary();

        /* access modifiers changed from: protected */
        public abstract void onDownloadStarted();

        /* access modifiers changed from: protected */
        public abstract void onSuccess(List<RedditSubreddit> list, long j);

        protected SubredditResponseHandler(AppCompatActivity context) {
            super(context);
        }

        public final void notifySuccess(List<RedditSubreddit> result, long timestamp) {
            try {
                onSuccess(result, timestamp);
            } catch (Throwable t2) {
                BugReportActivity.addGlobalError(new RRError(null, null, t1));
                BugReportActivity.handleGlobalError((Context) this.context, t2);
            }
        }

        public final void notifyDownloadNecessary() {
            try {
                onDownloadNecessary();
            } catch (Throwable t2) {
                BugReportActivity.addGlobalError(new RRError(null, null, t1));
                BugReportActivity.handleGlobalError((Context) this.context, t2);
            }
        }

        public final void notifyDownloadStarted() {
            try {
                onDownloadStarted();
            } catch (Throwable t2) {
                BugReportActivity.addGlobalError(new RRError(null, null, t1));
                BugReportActivity.handleGlobalError((Context) this.context, t2);
            }
        }
    }

    public static abstract class UserResponseHandler extends APIResponseHandler {
        /* access modifiers changed from: protected */
        public abstract void onDownloadStarted();

        /* access modifiers changed from: protected */
        public abstract void onSuccess(RedditUser redditUser, long j);

        protected UserResponseHandler(AppCompatActivity context) {
            super(context);
        }

        public final void notifySuccess(RedditUser result, long timestamp) {
            try {
                onSuccess(result, timestamp);
            } catch (Throwable t2) {
                BugReportActivity.addGlobalError(new RRError(null, null, t1));
                BugReportActivity.handleGlobalError((Context) this.context, t2);
            }
        }

        public final void notifyDownloadStarted() {
            try {
                onDownloadStarted();
            } catch (Throwable t2) {
                BugReportActivity.addGlobalError(new RRError(null, null, t1));
                BugReportActivity.handleGlobalError((Context) this.context, t2);
            }
        }
    }

    /* access modifiers changed from: protected */
    public abstract void onCallbackException(Throwable th);

    /* access modifiers changed from: protected */
    public abstract void onFailure(int i, Throwable th, Integer num, String str);

    /* access modifiers changed from: protected */
    public abstract void onFailure(APIFailureType aPIFailureType);

    private APIResponseHandler(AppCompatActivity context2) {
        this.context = context2;
    }

    public final void notifyFailure(int type, Throwable t, Integer status, String readableMessage) {
        try {
            onFailure(type, t, status, readableMessage);
        } catch (Throwable t2) {
            BugReportActivity.addGlobalError(new RRError(null, null, t1));
            BugReportActivity.handleGlobalError((Context) this.context, t2);
        }
    }

    public final void notifyFailure(APIFailureType type) {
        try {
            onFailure(type);
        } catch (Throwable t2) {
            BugReportActivity.addGlobalError(new RRError(null, null, t1));
            BugReportActivity.handleGlobalError((Context) this.context, t2);
        }
    }
}
