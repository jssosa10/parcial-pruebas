package org.quantumbadger.redreader.views;

import android.app.Activity;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import org.apache.commons.lang3.StringUtils;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.activities.PostListingActivity;
import org.quantumbadger.redreader.reddit.url.SearchPostListURL;

public final class SearchListingHeader extends FrameLayout {
    EditText mQuery = ((EditText) findViewById(R.id.search_listing_header_query_editText));
    Button mSearchButton;
    EditText mSubreddit;
    SearchPostListURL mUrl;

    public SearchListingHeader(final Activity parentActivity, SearchPostListURL url) {
        super(parentActivity);
        this.mUrl = url;
        ((LayoutInflater) parentActivity.getSystemService("layout_inflater")).inflate(R.layout.search_listing_header, this, true);
        this.mQuery.setText(url.query);
        this.mQuery.setImeOptions(5);
        this.mSubreddit = (EditText) findViewById(R.id.search_listing_header_sub_editText);
        if (url.subreddit == null) {
            this.mSubreddit.setText("all");
        } else {
            this.mSubreddit.setText(url.subreddit);
        }
        OnEditorActionListener onEnter = new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                SearchListingHeader.performSearch(parentActivity, SearchListingHeader.this.mSubreddit, SearchListingHeader.this.mQuery);
                return true;
            }
        };
        this.mSubreddit.setImeOptions(3);
        this.mSubreddit.setOnEditorActionListener(onEnter);
        this.mSearchButton = (Button) findViewById(R.id.search_listing_header_search);
        this.mSearchButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                SearchListingHeader.performSearch(parentActivity, SearchListingHeader.this.mSubreddit, SearchListingHeader.this.mQuery);
            }
        });
    }

    /* access modifiers changed from: private */
    public static void performSearch(Activity parentActivity, EditText mSubreddit2, EditText mQuery2) {
        String subreddit = mSubreddit2.getText().toString().trim();
        if (StringUtils.isEmpty(subreddit)) {
            subreddit = null;
        }
        SearchPostListURL url = SearchPostListURL.build(subreddit, mQuery2.getText().toString().trim());
        Intent intent = new Intent(parentActivity, PostListingActivity.class);
        intent.setData(url.generateJsonUri());
        parentActivity.startActivity(intent);
        parentActivity.finish();
    }
}
