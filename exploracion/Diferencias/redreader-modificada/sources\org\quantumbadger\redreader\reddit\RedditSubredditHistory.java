package org.quantumbadger.redreader.reddit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit.InvalidSubredditNameException;

public class RedditSubredditHistory {
    private static final HashMap<RedditAccount, HashSet<String>> SUBREDDITS = new HashMap<>();

    public static synchronized void addSubreddit(RedditAccount account, String name) throws InvalidSubredditNameException {
        synchronized (RedditSubredditHistory.class) {
            putDefaultSubreddits(account);
            ((HashSet) SUBREDDITS.get(account)).add(General.asciiLowercase(RedditSubreddit.stripRPrefix(name)));
        }
    }

    public static synchronized ArrayList<String> getSubredditsSorted(RedditAccount account) {
        ArrayList<String> result;
        synchronized (RedditSubredditHistory.class) {
            putDefaultSubreddits(account);
            result = new ArrayList<>((Collection) SUBREDDITS.get(account));
            Collections.sort(result);
        }
        return result;
    }

    private static void putDefaultSubreddits(RedditAccount account) {
        if (!SUBREDDITS.containsKey(account) || SUBREDDITS.get(account) == null) {
            SUBREDDITS.put(account, new HashSet());
        }
        HashSet<String> personalizedSubreddits = (HashSet) SUBREDDITS.get(account);
        if (personalizedSubreddits.isEmpty()) {
            String[] strArr = Reddit.DEFAULT_SUBREDDITS;
            int length = strArr.length;
            int i = 0;
            while (i < length) {
                try {
                    personalizedSubreddits.add(General.asciiLowercase(RedditSubreddit.stripRPrefix(strArr[i])));
                    i++;
                } catch (InvalidSubredditNameException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
