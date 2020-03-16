package org.quantumbadger.redreader.adapters;

import android.content.Context;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter.Item;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.reddit.RedditCommentListItem;

public class FilteredCommentListingManager extends RedditListingManager {
    private int mCommentCount;
    @Nullable
    private final String mSearchString;

    public FilteredCommentListingManager(Context context, @Nullable String searchString) {
        super(context);
        this.mSearchString = searchString;
    }

    public void addComments(Collection<RedditCommentListItem> comments) {
        Collection<Item> filteredComments = filter(comments);
        addItems(filteredComments);
        this.mCommentCount += filteredComments.size();
    }

    private Collection<Item> filter(Collection<RedditCommentListItem> comments) {
        Collection<RedditCommentListItem> searchComments;
        if (this.mSearchString == null) {
            searchComments = comments;
        } else {
            searchComments = new ArrayList<>();
            for (RedditCommentListItem comment : comments) {
                if (comment.isComment()) {
                    String commentStr = comment.asComment().getParsedComment().getRawComment().body;
                    if (commentStr != null && General.asciiLowercase(commentStr).contains(this.mSearchString)) {
                        searchComments.add(comment);
                    }
                }
            }
        }
        return Collections.unmodifiableCollection(searchComments);
    }

    public boolean isSearchListing() {
        return this.mSearchString != null;
    }

    public int getCommentCount() {
        return this.mCommentCount;
    }
}
