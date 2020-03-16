package org.quantumbadger.redreader.reddit.url;

import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;

public abstract class CommentListingURL extends RedditURL {
    public abstract CommentListingURL after(String str);

    public abstract CommentListingURL limit(Integer num);
}
