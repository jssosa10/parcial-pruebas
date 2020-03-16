package org.quantumbadger.redreader.adapters;

import org.quantumbadger.redreader.reddit.url.PostListingURL;

public interface MainMenuSelectionListener {
    void onSelected(int i);

    void onSelected(PostListingURL postListingURL);
}
