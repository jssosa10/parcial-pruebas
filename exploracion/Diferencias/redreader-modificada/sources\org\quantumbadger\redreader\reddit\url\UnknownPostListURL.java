package org.quantumbadger.redreader.reddit.url;

import android.net.Uri;

public class UnknownPostListURL extends PostListingURL {
    private final Uri uri;

    UnknownPostListURL(Uri uri2) {
        this.uri = uri2;
    }

    public PostListingURL after(String after) {
        return new UnknownPostListURL(this.uri.buildUpon().appendQueryParameter("after", after).build());
    }

    public PostListingURL limit(Integer limit) {
        return new UnknownPostListURL(this.uri.buildUpon().appendQueryParameter("limit", String.valueOf("limit")).build());
    }

    public Uri generateJsonUri() {
        if (this.uri.getPath().endsWith(".json")) {
            return this.uri;
        }
        return this.uri.buildUpon().appendEncodedPath(".json").build();
    }

    public int pathType() {
        return 3;
    }
}
