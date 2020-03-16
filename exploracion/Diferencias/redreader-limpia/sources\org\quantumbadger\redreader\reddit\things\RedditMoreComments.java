package org.quantumbadger.redreader.reddit.things;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedArray;
import org.quantumbadger.redreader.jsonwrap.JsonValue;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL;
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;

public class RedditMoreComments {
    public JsonBufferedArray children;
    public int count;
    public String parent_id;

    public List<PostCommentListingURL> getMoreUrls(RedditURL commentListingURL) {
        ArrayList<PostCommentListingURL> urls = new ArrayList<>(16);
        if (commentListingURL.pathType() == 7) {
            if (this.count > 0) {
                Iterator it = this.children.iterator();
                while (it.hasNext()) {
                    JsonValue child = (JsonValue) it.next();
                    if (child.getType() == 4) {
                        urls.add(commentListingURL.asPostCommentListURL().commentId(child.asString()));
                    }
                }
            } else {
                urls.add(commentListingURL.asPostCommentListURL().commentId(this.parent_id));
            }
        }
        return urls;
    }

    public int getCount() {
        return this.count;
    }
}
