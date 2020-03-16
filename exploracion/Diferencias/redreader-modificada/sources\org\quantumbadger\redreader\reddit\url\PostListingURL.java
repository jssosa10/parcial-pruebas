package org.quantumbadger.redreader.reddit.url;

import org.quantumbadger.redreader.reddit.PostSort;
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;

public abstract class PostListingURL extends RedditURL {
    public abstract PostListingURL after(String str);

    public abstract PostListingURL limit(Integer num);

    public PostSort getOrder() {
        return null;
    }
}
