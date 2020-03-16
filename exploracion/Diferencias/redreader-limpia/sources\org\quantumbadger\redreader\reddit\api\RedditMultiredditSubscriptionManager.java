package org.quantumbadger.redreader.reddit.api;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.common.TimestampBound;
import org.quantumbadger.redreader.common.collections.WeakReferenceListManager;
import org.quantumbadger.redreader.common.collections.WeakReferenceListManager.Operator;
import org.quantumbadger.redreader.io.RawObjectDB;
import org.quantumbadger.redreader.io.RequestResponseHandler;
import org.quantumbadger.redreader.io.WritableHashSet;
import org.quantumbadger.redreader.reddit.api.RedditAPIMultiredditListRequester.Key;

public class RedditMultiredditSubscriptionManager {
    private static RawObjectDB<String, WritableHashSet> db = null;
    private static RedditMultiredditSubscriptionManager singleton;
    private static RedditAccount singletonAccount;
    private final WeakReferenceListManager<MultiredditListChangeListener> listeners = new WeakReferenceListManager<>();
    @NonNull
    private final Context mContext;
    private WritableHashSet mMultireddits;
    @NonNull
    private final RedditAccount mUser;
    private final MultiredditListChangeNotifier notifier = new MultiredditListChangeNotifier();

    public interface MultiredditListChangeListener {
        void onMultiredditListUpdated(RedditMultiredditSubscriptionManager redditMultiredditSubscriptionManager);
    }

    private class MultiredditListChangeNotifier implements Operator<MultiredditListChangeListener> {
        private MultiredditListChangeNotifier() {
        }

        public void operate(MultiredditListChangeListener listener) {
            listener.onMultiredditListUpdated(RedditMultiredditSubscriptionManager.this);
        }
    }

    public static synchronized RedditMultiredditSubscriptionManager getSingleton(@NonNull Context context, @NonNull RedditAccount account) {
        RedditMultiredditSubscriptionManager redditMultiredditSubscriptionManager;
        synchronized (RedditMultiredditSubscriptionManager.class) {
            if (db == null) {
                db = new RawObjectDB<>(context, "rr_multireddit_subscriptions.db", WritableHashSet.class);
            }
            if (singleton == null || !account.equals(singletonAccount)) {
                singleton = new RedditMultiredditSubscriptionManager(account, context);
                singletonAccount = account;
            }
            redditMultiredditSubscriptionManager = singleton;
        }
        return redditMultiredditSubscriptionManager;
    }

    private RedditMultiredditSubscriptionManager(@NonNull RedditAccount user, @NonNull Context context) {
        this.mUser = user;
        this.mContext = context;
        this.mMultireddits = (WritableHashSet) db.getById(user.getCanonicalUsername());
    }

    public void addListener(@NonNull MultiredditListChangeListener listener) {
        this.listeners.add(listener);
    }

    public synchronized boolean areSubscriptionsReady() {
        return this.mMultireddits != null;
    }

    /* access modifiers changed from: private */
    public synchronized void onNewSubscriptionListReceived(HashSet<String> newSubscriptions, long timestamp) {
        this.mMultireddits = new WritableHashSet(newSubscriptions, timestamp, this.mUser.getCanonicalUsername());
        this.listeners.map(this.notifier);
        db.put(this.mMultireddits);
    }

    public synchronized ArrayList<String> getSubscriptionList() {
        return new ArrayList<>(this.mMultireddits.toHashset());
    }

    public void triggerUpdate(@Nullable final RequestResponseHandler<HashSet<String>, SubredditRequestFailure> handler, @NonNull TimestampBound timestampBound) {
        WritableHashSet writableHashSet = this.mMultireddits;
        if (writableHashSet == null || !timestampBound.verifyTimestamp(writableHashSet.getTimestamp())) {
            new RedditAPIMultiredditListRequester(this.mContext, this.mUser).performRequest(Key.INSTANCE, timestampBound, (RequestResponseHandler<WritableHashSet, SubredditRequestFailure>) new RequestResponseHandler<WritableHashSet, SubredditRequestFailure>() {
                public void onRequestFailed(SubredditRequestFailure failureReason) {
                    RequestResponseHandler requestResponseHandler = handler;
                    if (requestResponseHandler != null) {
                        requestResponseHandler.onRequestFailed(failureReason);
                    }
                }

                public void onRequestSuccess(WritableHashSet result, long timeCached) {
                    HashSet<String> newSubscriptions = result.toHashset();
                    RedditMultiredditSubscriptionManager.this.onNewSubscriptionListReceived(newSubscriptions, timeCached);
                    RequestResponseHandler requestResponseHandler = handler;
                    if (requestResponseHandler != null) {
                        requestResponseHandler.onRequestSuccess(newSubscriptions, timeCached);
                    }
                }
            });
        }
    }

    public Long getSubscriptionListTimestamp() {
        WritableHashSet writableHashSet = this.mMultireddits;
        if (writableHashSet != null) {
            return Long.valueOf(writableHashSet.getTimestamp());
        }
        return null;
    }
}
