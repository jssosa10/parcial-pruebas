package org.quantumbadger.redreader.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;
import org.apache.commons.lang3.StringEscapeUtils;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.reddit.things.RedditComment;

public final class CommentPropertiesDialog extends PropertiesDialog {
    public static CommentPropertiesDialog newInstance(RedditComment comment) {
        CommentPropertiesDialog pp = new CommentPropertiesDialog();
        Bundle args = new Bundle();
        args.putParcelable("comment", comment);
        pp.setArguments(args);
        return pp;
    }

    /* access modifiers changed from: protected */
    public String getTitle(Context context) {
        return context.getString(R.string.props_comment_title);
    }

    /* access modifiers changed from: protected */
    public void prepare(AppCompatActivity context, LinearLayout items) {
        RedditComment comment = (RedditComment) getArguments().getParcelable("comment");
        items.addView(propView((Context) context, "ID", (CharSequence) comment.name, true));
        items.addView(propView((Context) context, (int) R.string.props_author, (CharSequence) comment.author, false));
        if (comment.author_flair_text != null && comment.author_flair_text.length() > 0) {
            items.addView(propView((Context) context, (int) R.string.props_author_flair, (CharSequence) comment.author_flair_text, false));
        }
        items.addView(propView((Context) context, (int) R.string.props_created, (CharSequence) RRTime.formatDateTime(comment.created_utc * 1000, context), false));
        if (comment.edited instanceof Long) {
            items.addView(propView((Context) context, (int) R.string.props_edited, (CharSequence) RRTime.formatDateTime(((Long) comment.edited).longValue() * 1000, context), false));
        } else {
            items.addView(propView((Context) context, (int) R.string.props_edited, (int) R.string.props_never, false));
        }
        items.addView(propView((Context) context, (int) R.string.props_score, (CharSequence) String.valueOf(comment.ups - comment.downs), false));
        items.addView(propView((Context) context, (int) R.string.props_subreddit, (CharSequence) comment.subreddit, false));
        if (comment.body != null && comment.body.length() > 0) {
            items.addView(propView((Context) context, (int) R.string.props_body_markdown, (CharSequence) StringEscapeUtils.unescapeHtml4(comment.body), false));
        }
    }
}
