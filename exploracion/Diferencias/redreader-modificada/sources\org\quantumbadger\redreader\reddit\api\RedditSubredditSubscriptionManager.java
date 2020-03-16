package org.quantumbadger.redreader.reddit.api;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.common.TimestampBound;
import org.quantumbadger.redreader.common.UnexpectedInternalStateException;
import org.quantumbadger.redreader.common.collections.WeakReferenceListManager;
import org.quantumbadger.redreader.common.collections.WeakReferenceListManager.ArgOperator;
import org.quantumbadger.redreader.io.RawObjectDB;
import org.quantumbadger.redreader.io.RequestResponseHandler;
import org.quantumbadger.redreader.io.WritableHashSet;
import org.quantumbadger.redreader.reddit.APIResponseHandler.APIFailureType;
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler;
import org.quantumbadger.redreader.reddit.RedditAPI;
import org.quantumbadger.redreader.reddit.RedditSubredditHistory;
import org.quantumbadger.redreader.reddit.RedditSubredditManager.SubredditListType;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit.InvalidSubredditNameException;

public class RedditSubredditSubscriptionManager {
    private static final String TAG = "SubscriptionManager";
    private static RawObjectDB<String, WritableHashSet> db = null;
    private static RedditSubredditSubscriptionManager singleton;
    private static RedditAccount singletonAccount;
    private final Context context;
    private final WeakReferenceListManager<SubredditSubscriptionStateChangeListener> listeners = new WeakReferenceListManager<>();
    private final SubredditSubscriptionStateChangeNotifier notifier = new SubredditSubscriptionStateChangeNotifier();
    private final HashSet<String> pendingSubscriptions = new HashSet<>();
    private final HashSet<String> pendingUnsubscriptions = new HashSet<>();
    private WritableHashSet subscriptions;
    private final RedditAccount user;

    private class SubredditActionResponseHandler extends ActionResponseHandler {
        private final int action;
        /* access modifiers changed from: private */
        public final AppCompatActivity activity;
        private final String canonicalName;

        protected SubredditActionResponseHandler(AppCompatActivity activity2, int action2, String canonicalName2) {
            super(activity2);
            this.activity = activity2;
            this.action = action2;
            this.canonicalName = canonicalName2;
        }

        /* access modifiers changed from: protected */
        public void onSuccess(@Nullable String redirectUrl) {
            switch (this.action) {
                case 0:
                    RedditSubredditSubscriptionManager.this.onSubscriptionAttemptSuccess(this.canonicalName);
                    break;
                case 1:
                    RedditSubredditSubscriptionManager.this.onUnsubscriptionAttemptSuccess(this.canonicalName);
                    break;
            }
            RedditSubredditSubscriptionManager.this.triggerUpdate(null, TimestampBound.NONE);
        }

        /* access modifiers changed from: protected */
        public void onCallbackException(Throwable t) {
            BugReportActivity.handleGlobalError((Context) this.context, t);
        }

        /* access modifiers changed from: protected */
        public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
            RedditSubredditSubscriptionManager.this.onSubscriptionChangeAttemptFailed(this.canonicalName);
            if (t != null) {
                t.printStackTrace();
            }
            final RRError error = General.getGeneralErrorForFailure(this.context, type, t, status, null);
            AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                public void run() {
                    General.showResultDialog(SubredditActionResponseHandler.this.activity, error);
                }
            });
        }

        /* access modifiers changed from: protected */
        public void onFailure(APIFailureType type) {
            RedditSubredditSubscriptionManager.this.onSubscriptionChangeAttemptFailed(this.canonicalName);
            final RRError error = General.getGeneralErrorForFailure(this.context, type);
            AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                public void run() {
                    General.showResultDialog(SubredditActionResponseHandler.this.activity, error);
                }
            });
        }
    }

    private enum SubredditSubscriptionChangeType {
        LIST_UPDATED,
        SUBSCRIPTION_ATTEMPTED,
        UNSUBSCRIPTION_ATTEMPTED
    }

    public enum SubredditSubscriptionState {
        SUBSCRIBED,
        SUBSCRIBING,
        UNSUBSCRIBING,
        NOT_SUBSCRIBED
    }

    public interface SubredditSubscriptionStateChangeListener {
        void onSubredditSubscriptionAttempted(RedditSubredditSubscriptionManager redditSubredditSubscriptionManager);

        void onSubredditSubscriptionListUpdated(RedditSubredditSubscriptionManager redditSubredditSubscriptionManager);

        void onSubredditUnsubscriptionAttempted(RedditSubredditSubscriptionManager redditSubredditSubscriptionManager);
    }

    private class SubredditSubscriptionStateChangeNotifier implements ArgOperator<SubredditSubscriptionStateChangeListener, SubredditSubscriptionChangeType> {
        private SubredditSubscriptionStateChangeNotifier() {
        }

        public void operate(SubredditSubscriptionStateChangeListener listener, SubredditSubscriptionChangeType changeType) {
            switch (changeType) {
                case LIST_UPDATED:
                    listener.onSubredditSubscriptionListUpdated(RedditSubredditSubscriptionManager.this);
                    return;
                case SUBSCRIPTION_ATTEMPTED:
                    listener.onSubredditSubscriptionAttempted(RedditSubredditSubscriptionManager.this);
                    return;
                case UNSUBSCRIPTION_ATTEMPTED:
                    listener.onSubredditUnsubscriptionAttempted(RedditSubredditSubscriptionManager.this);
                    return;
                default:
                    StringBuilder sb = new StringBuilder();
                    sb.append("Invalid SubredditSubscriptionChangeType ");
                    sb.append(changeType.toString());
                    throw new UnexpectedInternalStateException(sb.toString());
            }
        }
    }

    public static synchronized RedditSubredditSubscriptionManager getSingleton(Context context2, RedditAccount account) {
        RedditSubredditSubscriptionManager redditSubredditSubscriptionManager;
        synchronized (RedditSubredditSubscriptionManager.class) {
            if (db == null) {
                db = new RawObjectDB<>(context2, "rr_subscriptions.db", WritableHashSet.class);
            }
            if (singleton == null || !account.equals(singletonAccount)) {
                singleton = new RedditSubredditSubscriptionManager(account, context2);
                singletonAccount = account;
            }
            redditSubredditSubscriptionManager = singleton;
        }
        return redditSubredditSubscriptionManager;
    }

    private RedditSubredditSubscriptionManager(RedditAccount user2, Context context2) {
        this.user = user2;
        this.context = context2;
        this.subscriptions = (WritableHashSet) db.getById(user2.getCanonicalUsername());
        WritableHashSet writableHashSet = this.subscriptions;
        if (writableHashSet != null) {
            addToHistory(user2, writableHashSet.toHashset());
        }
    }

    public void addListener(SubredditSubscriptionStateChangeListener listener) {
        this.listeners.add(listener);
    }

    public synchronized boolean areSubscriptionsReady() {
        return this.subscriptions != null;
    }

    public synchronized SubredditSubscriptionState getSubscriptionState(String subredditCanonicalId) {
        if (this.pendingSubscriptions.contains(subredditCanonicalId)) {
            return SubredditSubscriptionState.SUBSCRIBING;
        } else if (this.pendingUnsubscriptions.contains(subredditCanonicalId)) {
            return SubredditSubscriptionState.UNSUBSCRIBING;
        } else if (this.subscriptions.toHashset().contains(subredditCanonicalId)) {
            return SubredditSubscriptionState.SUBSCRIBED;
        } else {
            return SubredditSubscriptionState.NOT_SUBSCRIBED;
        }
    }

    private synchronized void onSubscriptionAttempt(String subredditCanonicalId) {
        this.pendingSubscriptions.add(subredditCanonicalId);
        this.listeners.map(this.notifier, SubredditSubscriptionChangeType.SUBSCRIPTION_ATTEMPTED);
    }

    private synchronized void onUnsubscriptionAttempt(String subredditCanonicalId) {
        this.pendingUnsubscriptions.add(subredditCanonicalId);
        this.listeners.map(this.notifier, SubredditSubscriptionChangeType.UNSUBSCRIPTION_ATTEMPTED);
    }

    /* access modifiers changed from: private */
    public synchronized void onSubscriptionChangeAttemptFailed(String subredditCanonicalId) {
        this.pendingUnsubscriptions.remove(subredditCanonicalId);
        this.pendingSubscriptions.remove(subredditCanonicalId);
        this.listeners.map(this.notifier, SubredditSubscriptionChangeType.LIST_UPDATED);
    }

    /* access modifiers changed from: private */
    public synchronized void onSubscriptionAttemptSuccess(String subredditCanonicalId) {
        this.pendingSubscriptions.remove(subredditCanonicalId);
        this.subscriptions.toHashset().add(subredditCanonicalId);
        this.listeners.map(this.notifier, SubredditSubscriptionChangeType.LIST_UPDATED);
    }

    /* access modifiers changed from: private */
    public synchronized void onUnsubscriptionAttemptSuccess(String subredditCanonicalId) {
        this.pendingUnsubscriptions.remove(subredditCanonicalId);
        this.subscriptions.toHashset().remove(subredditCanonicalId);
        this.listeners.map(this.notifier, SubredditSubscriptionChangeType.LIST_UPDATED);
    }

    private static void addToHistory(RedditAccount account, HashSet<String> newSubscriptions) {
        Iterator it = newSubscriptions.iterator();
        while (it.hasNext()) {
            String sub = (String) it.next();
            try {
                RedditSubredditHistory.addSubreddit(account, sub);
            } catch (InvalidSubredditNameException e) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Invalid subreddit name ");
                sb.append(sub);
                Log.e(str, sb.toString(), e);
            }
        }
    }

    /* access modifiers changed from: private */
    public synchronized void onNewSubscriptionListReceived(HashSet<String> newSubscriptions, long timestamp) {
        this.pendingSubscriptions.clear();
        this.pendingUnsubscriptions.clear();
        this.subscriptions = new WritableHashSet(newSubscriptions, timestamp, this.user.getCanonicalUsername());
        db.put(this.subscriptions);
        addToHistory(this.user, newSubscriptions);
        this.listeners.map(this.notifier, SubredditSubscriptionChangeType.LIST_UPDATED);
    }

    public synchronized ArrayList<String> getSubscriptionList() {
        return new ArrayList<>(this.subscriptions.toHashset());
    }

    public void triggerUpdate(final RequestResponseHandler<HashSet<String>, SubredditRequestFailure> handler, TimestampBound timestampBound) {
        WritableHashSet writableHashSet = this.subscriptions;
        if (writableHashSet == null || !timestampBound.verifyTimestamp(writableHashSet.getTimestamp())) {
            new RedditAPIIndividualSubredditListRequester(this.context, this.user).performRequest(SubredditListType.SUBSCRIBED, timestampBound, (RequestResponseHandler<WritableHashSet, SubredditRequestFailure>) new RequestResponseHandler<WritableHashSet, SubredditRequestFailure>() {
                public void onRequestFailed(SubredditRequestFailure failureReason) {
                    RequestResponseHandler requestResponseHandler = handler;
                    if (requestResponseHandler != null) {
                        requestResponseHandler.onRequestFailed(failureReason);
                    }
                }

                public void onRequestSuccess(WritableHashSet result, long timeCached) {
                    HashSet<String> newSubscriptions = result.toHashset();
                    RedditSubredditSubscriptionManager.this.onNewSubscriptionListReceived(newSubscriptions, timeCached);
                    RequestResponseHandler requestResponseHandler = handler;
                    if (requestResponseHandler != null) {
                        requestResponseHandler.onRequestSuccess(newSubscriptions, timeCached);
                    }
                }
            });
        }
    }

    public void subscribe(String subredditCanonicalId, AppCompatActivity activity) {
        RedditAPI.subscriptionAction(CacheManager.getInstance(this.context), new SubredditActionResponseHandler(activity, 0, subredditCanonicalId), this.user, subredditCanonicalId, 0, this.context);
        onSubscriptionAttempt(subredditCanonicalId);
    }

    public void unsubscribe(String subredditCanonicalId, AppCompatActivity activity) {
        RedditAPI.subscriptionAction(CacheManager.getInstance(this.context), new SubredditActionResponseHandler(activity, 1, subredditCanonicalId), this.user, subredditCanonicalId, 1, this.context);
        onUnsubscriptionAttempt(subredditCanonicalId);
    }

    public Long getSubscriptionListTimestamp() {
        WritableHashSet writableHashSet = this.subscriptions;
        if (writableHashSet != null) {
            return Long.valueOf(writableHashSet.getTimestamp());
        }
        return null;
    }
}
