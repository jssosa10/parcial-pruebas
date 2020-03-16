package org.quantumbadger.redreader.listingcontrollers;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import java.util.UUID;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.fragments.PostListingFragment;
import org.quantumbadger.redreader.reddit.PostSort;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit.InvalidSubredditNameException;
import org.quantumbadger.redreader.reddit.url.PostListingURL;
import org.quantumbadger.redreader.reddit.url.SubredditPostListURL.Type;
import org.quantumbadger.redreader.reddit.url.UserPostListingURL;

public class PostListingController {
    private UUID session = null;
    private PostListingURL url;

    public void setSession(UUID session2) {
        this.session = session2;
    }

    public UUID getSession() {
        return this.session;
    }

    public PostListingController(PostListingURL url2, Context context) {
        if (url2.pathType() == 0) {
            if (url2.asSubredditPostListURL().order == null) {
                PostSort order = defaultOrder(context);
                if (order == PostSort.BEST && url2.asSubredditPostListURL().type != Type.FRONTPAGE) {
                    order = PostSort.HOT;
                }
                url2 = url2.asSubredditPostListURL().sort(order);
            }
        } else if (url2.pathType() == 1 && url2.asUserPostListURL().order == null) {
            url2 = url2.asUserPostListURL().sort(PostSort.NEW);
        }
        this.url = url2;
    }

    public boolean isSortable() {
        boolean z = false;
        if (this.url.pathType() == 1) {
            if (this.url.asUserPostListURL().type == UserPostListingURL.Type.SUBMITTED) {
                z = true;
            }
            return z;
        }
        if (this.url.pathType() == 0 || this.url.pathType() == 8 || this.url.pathType() == 2) {
            z = true;
        }
        return z;
    }

    public boolean isFrontPage() {
        return this.url.pathType() == 0 && this.url.asSubredditPostListURL().type == Type.FRONTPAGE;
    }

    public final void setSort(PostSort order) {
        if (this.url.pathType() == 0) {
            this.url = this.url.asSubredditPostListURL().sort(order);
        } else if (this.url.pathType() == 8) {
            this.url = this.url.asMultiredditPostListURL().sort(order);
        } else if (this.url.pathType() == 2) {
            this.url = this.url.asSearchPostListURL().sort(order);
        } else if (this.url.pathType() == 1) {
            this.url = this.url.asUserPostListURL().sort(order);
        } else {
            throw new RuntimeException("Cannot set sort for this URL");
        }
    }

    private PostSort defaultOrder(Context context) {
        return PrefsUtility.pref_behaviour_postsort(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    public final PostSort getSort() {
        if (this.url.pathType() == 0) {
            return this.url.asSubredditPostListURL().order;
        }
        if (this.url.pathType() == 8) {
            return this.url.asMultiredditPostListURL().order;
        }
        if (this.url.pathType() == 2) {
            return this.url.asSearchPostListURL().order;
        }
        if (this.url.pathType() == 1) {
            return this.url.asUserPostListURL().order;
        }
        return null;
    }

    public Uri getUri() {
        return this.url.generateJsonUri();
    }

    public final PostListingFragment get(AppCompatActivity parent, boolean force, Bundle savedInstanceState) {
        if (force) {
            this.session = null;
        }
        PostListingFragment postListingFragment = new PostListingFragment(parent, savedInstanceState, getUri(), this.session, force);
        return postListingFragment;
    }

    public final boolean isSubreddit() {
        return this.url.pathType() == 0 && this.url.asSubredditPostListURL().type == Type.SUBREDDIT;
    }

    public final boolean isRandomSubreddit() {
        return this.url.pathType() == 0 && this.url.asSubredditPostListURL().type == Type.RANDOM;
    }

    public final boolean isSearchResults() {
        return this.url.pathType() == 2;
    }

    public final boolean isSubredditSearchResults() {
        return isSearchResults() && this.url.asSearchPostListURL().subreddit != null;
    }

    public final boolean isUserPostListing() {
        return this.url.pathType() == 1;
    }

    public final String subredditCanonicalName() {
        if (this.url.pathType() == 0 && this.url.asSubredditPostListURL().type == Type.SUBREDDIT) {
            try {
                return RedditSubreddit.getCanonicalName(this.url.asSubredditPostListURL().subreddit);
            } catch (InvalidSubredditNameException e) {
                throw new RuntimeException(e);
            }
        } else if (this.url.pathType() != 2 || this.url.asSearchPostListURL().subreddit == null) {
            return null;
        } else {
            try {
                return RedditSubreddit.getCanonicalName(this.url.asSearchPostListURL().subreddit);
            } catch (InvalidSubredditNameException e2) {
                throw new RuntimeException(e2);
            }
        }
    }
}
