package org.quantumbadger.redreader.account;

import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.reddit.api.RedditOAuth.AccessToken;
import org.quantumbadger.redreader.reddit.api.RedditOAuth.RefreshToken;

public class RedditAccount {
    private AccessToken accessToken;
    public final long priority;
    public final RefreshToken refreshToken;
    public final String username;

    public RedditAccount(String username2, RefreshToken refreshToken2, long priority2) {
        if (username2 != null) {
            this.username = username2.trim();
            this.refreshToken = refreshToken2;
            this.priority = priority2;
            return;
        }
        throw new RuntimeException("Null user in RedditAccount");
    }

    public boolean isAnonymous() {
        return this.username.length() == 0;
    }

    public String getCanonicalUsername() {
        return General.asciiLowercase(this.username.trim());
    }

    public synchronized AccessToken getMostRecentAccessToken() {
        return this.accessToken;
    }

    public synchronized void setAccessToken(AccessToken token) {
        this.accessToken = token;
    }

    public boolean equals(Object o) {
        return (o instanceof RedditAccount) && this.username.equalsIgnoreCase(((RedditAccount) o).username);
    }

    public int hashCode() {
        return getCanonicalUsername().hashCode();
    }
}
