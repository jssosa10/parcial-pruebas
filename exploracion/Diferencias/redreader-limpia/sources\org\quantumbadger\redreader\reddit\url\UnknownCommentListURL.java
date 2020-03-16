package org.quantumbadger.redreader.reddit.url;

import android.net.Uri;

public class UnknownCommentListURL extends CommentListingURL {
    private final Uri uri;

    UnknownCommentListURL(Uri uri2) {
        this.uri = uri2;
    }

    public CommentListingURL after(String after) {
        return new UnknownCommentListURL(this.uri.buildUpon().appendQueryParameter("after", after).build());
    }

    public CommentListingURL limit(Integer limit) {
        return new UnknownCommentListURL(this.uri.buildUpon().appendQueryParameter("limit", String.valueOf("limit")).build());
    }

    public Uri generateJsonUri() {
        if (this.uri.getPath().endsWith(".json")) {
            return this.uri;
        }
        return this.uri.buildUpon().appendEncodedPath(".json").build();
    }

    public int pathType() {
        return 6;
    }
}
