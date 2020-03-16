package org.quantumbadger.redreader.reddit;

import android.content.Context;
import java.util.Collection;
import java.util.HashMap;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.TimestampBound;
import org.quantumbadger.redreader.io.RawObjectDB;
import org.quantumbadger.redreader.io.RequestResponseHandler;
import org.quantumbadger.redreader.io.ThreadedRawObjectDB;
import org.quantumbadger.redreader.io.UpdatedVersionListener;
import org.quantumbadger.redreader.io.WeakCache;
import org.quantumbadger.redreader.reddit.api.RedditAPIIndividualSubredditDataRequester;
import org.quantumbadger.redreader.reddit.api.SubredditRequestFailure;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;

public class RedditSubredditManager {
    private static RedditSubredditManager singleton;
    private static RedditAccount singletonUser;
    private final WeakCache<String, RedditSubreddit, SubredditRequestFailure> subredditCache;

    public enum SubredditListType {
        SUBSCRIBED,
        MODERATED,
        MULTIREDDITS,
        MOST_POPULAR,
        DEFAULTS
    }

    public void offerRawSubredditData(Collection<RedditSubreddit> toWrite, long timestamp) {
        this.subredditCache.performWrite(toWrite);
    }

    public static synchronized RedditSubredditManager getInstance(Context context, RedditAccount user) {
        RedditSubredditManager redditSubredditManager;
        synchronized (RedditSubredditManager.class) {
            if (singleton == null || !user.equals(singletonUser)) {
                singletonUser = user;
                singleton = new RedditSubredditManager(context, user);
            }
            redditSubredditManager = singleton;
        }
        return redditSubredditManager;
    }

    private RedditSubredditManager(Context context, RedditAccount user) {
        this.subredditCache = new WeakCache<>(new ThreadedRawObjectDB<>(new RawObjectDB<>(context, getDbFilename("subreddits", user), RedditSubreddit.class), new RedditAPIIndividualSubredditDataRequester(context, user)));
    }

    private static String getDbFilename(String type, RedditAccount user) {
        StringBuilder sb = new StringBuilder();
        sb.append(General.sha1(user.username.getBytes()));
        sb.append("_");
        sb.append(type);
        sb.append("_subreddits.db");
        return sb.toString();
    }

    public void getSubreddit(String subredditCanonicalId, TimestampBound timestampBound, RequestResponseHandler<RedditSubreddit, SubredditRequestFailure> handler, UpdatedVersionListener<String, RedditSubreddit> updatedVersionListener) {
        this.subredditCache.performRequest(RedditSubreddit.getDisplayNameFromCanonicalName(subredditCanonicalId), timestampBound, handler, updatedVersionListener);
    }

    public void getSubreddits(Collection<String> ids, TimestampBound timestampBound, RequestResponseHandler<HashMap<String, RedditSubreddit>, SubredditRequestFailure> handler) {
        this.subredditCache.performRequest(ids, timestampBound, handler);
    }
}
