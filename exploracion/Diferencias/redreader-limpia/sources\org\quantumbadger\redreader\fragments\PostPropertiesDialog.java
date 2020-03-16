package org.quantumbadger.redreader.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;
import org.apache.commons.lang3.StringEscapeUtils;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.reddit.things.RedditPost;

public final class PostPropertiesDialog extends PropertiesDialog {
    public static PostPropertiesDialog newInstance(RedditPost post) {
        PostPropertiesDialog pp = new PostPropertiesDialog();
        Bundle args = new Bundle();
        args.putParcelable("post", post);
        pp.setArguments(args);
        return pp;
    }

    /* access modifiers changed from: protected */
    public String getTitle(Context context) {
        return context.getString(R.string.props_post_title);
    }

    /* access modifiers changed from: protected */
    public void prepare(AppCompatActivity context, LinearLayout items) {
        RedditPost post = (RedditPost) getArguments().getParcelable("post");
        items.addView(propView((Context) context, (int) R.string.props_title, (CharSequence) StringEscapeUtils.unescapeHtml4(post.title.trim()), true));
        items.addView(propView((Context) context, (int) R.string.props_author, (CharSequence) post.author, false));
        items.addView(propView((Context) context, (int) R.string.props_url, (CharSequence) StringEscapeUtils.unescapeHtml4(post.getUrl()), false));
        items.addView(propView((Context) context, (int) R.string.props_created, (CharSequence) RRTime.formatDateTime(post.created_utc * 1000, context), false));
        if (post.edited instanceof Long) {
            items.addView(propView((Context) context, (int) R.string.props_edited, (CharSequence) RRTime.formatDateTime(((Long) post.edited).longValue() * 1000, context), false));
        } else {
            items.addView(propView((Context) context, (int) R.string.props_edited, (int) R.string.props_never, false));
        }
        items.addView(propView((Context) context, (int) R.string.props_subreddit, (CharSequence) post.subreddit, false));
        items.addView(propView((Context) context, (int) R.string.props_score, (CharSequence) String.valueOf(post.score), false));
        items.addView(propView((Context) context, (int) R.string.props_num_comments, (CharSequence) String.valueOf(post.num_comments), false));
        if (post.selftext != null && post.selftext.length() > 0) {
            items.addView(propView((Context) context, (int) R.string.props_self_markdown, (CharSequence) StringEscapeUtils.unescapeHtml4(post.selftext), false));
        }
    }
}
