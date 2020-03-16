package org.quantumbadger.redreader.listingcontrollers;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import java.util.UUID;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.fragments.CommentListingFragment;
import org.quantumbadger.redreader.reddit.url.CommentListingURL;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL;
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;
import org.quantumbadger.redreader.reddit.url.UserCommentListingURL.Sort;

public class CommentListingController {
    private String mSearchString = null;
    private UUID mSession = null;
    private CommentListingURL mUrl;

    public UUID getSession() {
        return this.mSession;
    }

    public void setSession(UUID session) {
        this.mSession = session;
    }

    public CommentListingController(RedditURL url, Context context) {
        if (url.pathType() == 7) {
            if (url.asPostCommentListURL().order == null) {
                url = url.asPostCommentListURL().order(defaultOrder(context));
            }
        } else if (url.pathType() == 5 && url.asUserCommentListURL().order == null) {
            url = url.asUserCommentListURL().order(Sort.NEW);
        }
        if (url instanceof CommentListingURL) {
            this.mUrl = (CommentListingURL) url;
            return;
        }
        throw new RuntimeException("Not comment listing URL");
    }

    private PostCommentListingURL.Sort defaultOrder(Context context) {
        return PrefsUtility.pref_behaviour_commentsort(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    public void setSort(PostCommentListingURL.Sort s) {
        if (this.mUrl.pathType() == 7) {
            this.mUrl = this.mUrl.asPostCommentListURL().order(s);
        }
    }

    public void setSort(Sort s) {
        if (this.mUrl.pathType() == 5) {
            this.mUrl = this.mUrl.asUserCommentListURL().order(s);
        }
    }

    public PostCommentListingURL.Sort getSort() {
        if (this.mUrl.pathType() == 7) {
            return this.mUrl.asPostCommentListURL().order;
        }
        return null;
    }

    public void setSearchString(String searchString) {
        this.mSearchString = searchString;
    }

    public String getSearchString() {
        return this.mSearchString;
    }

    public Uri getUri() {
        return this.mUrl.generateJsonUri();
    }

    public CommentListingURL getCommentListingUrl() {
        return this.mUrl;
    }

    public CommentListingFragment get(AppCompatActivity parent, boolean force, Bundle savedInstanceState) {
        if (force) {
            this.mSession = null;
        }
        CommentListingFragment commentListingFragment = new CommentListingFragment(parent, savedInstanceState, General.listOfOne(this.mUrl), this.mSession, this.mSearchString, force);
        return commentListingFragment;
    }

    public boolean isSortable() {
        return this.mUrl.pathType() == 7 || this.mUrl.pathType() == 5;
    }

    public boolean isUserCommentListing() {
        return this.mUrl.pathType() == 5;
    }
}
