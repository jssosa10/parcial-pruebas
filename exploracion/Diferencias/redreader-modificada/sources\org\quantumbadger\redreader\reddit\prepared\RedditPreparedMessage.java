package org.quantumbadger.redreader.reddit.prepared;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashSet;
import org.apache.commons.lang3.StringEscapeUtils;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.activities.CommentReplyActivity;
import org.quantumbadger.redreader.common.BetterSSB;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.RRThemeAttributes;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParagraphGroup;
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParser;
import org.quantumbadger.redreader.reddit.things.RedditMessage;

public final class RedditPreparedMessage implements RedditRenderableInboxItem {
    public final MarkdownParagraphGroup body;
    public SpannableStringBuilder header;
    public final String idAndType;
    public final RedditMessage src;

    public RedditPreparedMessage(Context context, RedditMessage message, long timestamp) {
        Context context2 = context;
        RedditMessage redditMessage = message;
        this.src = redditMessage;
        TypedArray appearance = context2.obtainStyledAttributes(new int[]{R.attr.rrCommentHeaderBoldCol, R.attr.rrCommentHeaderAuthorCol});
        int rrCommentHeaderBoldCol = appearance.getColor(0, 255);
        int rrCommentHeaderAuthorCol = appearance.getColor(1, 255);
        appearance.recycle();
        this.body = MarkdownParser.parse(message.getUnescapedBodyMarkdown().toCharArray());
        this.idAndType = redditMessage.name;
        BetterSSB sb = new BetterSSB();
        if (this.src.author == null) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("[");
            sb2.append(context2.getString(R.string.general_unknown));
            sb2.append("]");
            sb.append(sb2.toString(), 17, rrCommentHeaderAuthorCol, 0, 1.0f);
        } else {
            sb.append(this.src.author, 17, rrCommentHeaderAuthorCol, 0, 1.0f);
        }
        sb.append("   ", 0);
        sb.append(RRTime.formatDurationFrom(context2, this.src.created_utc * 1000), 17, rrCommentHeaderBoldCol, 0, 1.0f);
        this.header = sb.get();
    }

    public HashSet<String> computeAllLinks() {
        return LinkHandler.computeAllLinks(StringEscapeUtils.unescapeHtml4(this.src.body_html));
    }

    public SpannableStringBuilder getHeader() {
        return this.header;
    }

    private void openReplyActivity(AppCompatActivity activity) {
        Intent intent = new Intent(activity, CommentReplyActivity.class);
        intent.putExtra(CommentReplyActivity.PARENT_ID_AND_TYPE_KEY, this.idAndType);
        intent.putExtra(CommentReplyActivity.PARENT_MARKDOWN_KEY, this.src.getUnescapedBodyMarkdown());
        intent.putExtra(CommentReplyActivity.PARENT_TYPE, CommentReplyActivity.PARENT_TYPE_MESSAGE);
        activity.startActivity(intent);
    }

    public void handleInboxClick(AppCompatActivity activity) {
        openReplyActivity(activity);
    }

    public void handleInboxLongClick(AppCompatActivity activity) {
        openReplyActivity(activity);
    }

    public CharSequence getHeader(RRThemeAttributes theme, RedditChangeDataManager changeDataManager, Context context) {
        return this.header;
    }

    public View getBody(AppCompatActivity activity, Integer textColor, Float textSize, boolean showLinkButtons) {
        LinearLayout subjectLayout = new LinearLayout(activity);
        subjectLayout.setOrientation(1);
        TextView subjectText = new TextView(activity);
        subjectText.setText(StringEscapeUtils.unescapeHtml4(this.src.subject != null ? this.src.subject : "(no subject)"));
        subjectText.setTextColor(textColor.intValue());
        subjectText.setTextSize(textSize.floatValue());
        subjectText.setTypeface(null, 1);
        subjectLayout.addView(subjectText);
        subjectLayout.addView(this.body.buildView(activity, textColor, textSize, showLinkButtons));
        return subjectLayout;
    }
}
