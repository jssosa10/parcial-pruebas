package org.quantumbadger.redreader.reddit.prepared;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import org.quantumbadger.redreader.common.RRThemeAttributes;

public interface RedditRenderableCommentListItem {
    View getBody(AppCompatActivity appCompatActivity, Integer num, Float f, boolean z);

    CharSequence getHeader(RRThemeAttributes rRThemeAttributes, RedditChangeDataManager redditChangeDataManager, Context context);
}
