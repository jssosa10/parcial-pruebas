package org.quantumbadger.redreader.adapters;

import android.content.Context;
import java.util.Collection;
import java.util.Collections;
import org.quantumbadger.redreader.reddit.RedditPostListItem;

public class PostListingManager extends RedditListingManager {
    private int mPostCount;

    public PostListingManager(Context context) {
        super(context);
    }

    public void addPosts(Collection<RedditPostListItem> posts) {
        addItems(Collections.unmodifiableCollection(posts));
        this.mPostCount += posts.size();
    }

    public int getPostCount() {
        return this.mPostCount;
    }
}
