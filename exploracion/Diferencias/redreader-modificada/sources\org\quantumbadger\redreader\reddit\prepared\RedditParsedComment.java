package org.quantumbadger.redreader.reddit.prepared;

import org.apache.commons.lang3.StringEscapeUtils;
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParagraphGroup;
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParser;
import org.quantumbadger.redreader.reddit.things.RedditComment;
import org.quantumbadger.redreader.reddit.things.RedditThingWithIdAndType;

public class RedditParsedComment implements RedditThingWithIdAndType {
    private final MarkdownParagraphGroup mBody;
    private final String mFlair;
    private final RedditComment mSrc;

    public RedditParsedComment(RedditComment comment) {
        this.mSrc = comment;
        this.mBody = MarkdownParser.parse(StringEscapeUtils.unescapeHtml4(comment.body).toCharArray());
        if (comment.author_flair_text != null) {
            this.mFlair = StringEscapeUtils.unescapeHtml4(comment.author_flair_text);
        } else {
            this.mFlair = null;
        }
    }

    public MarkdownParagraphGroup getBody() {
        return this.mBody;
    }

    public String getFlair() {
        return this.mFlair;
    }

    public String getIdAlone() {
        return this.mSrc.getIdAlone();
    }

    public String getIdAndType() {
        return this.mSrc.getIdAndType();
    }

    public RedditComment getRawComment() {
        return this.mSrc;
    }
}
