package org.quantumbadger.redreader.reddit.prepared;

import org.apache.commons.lang3.StringEscapeUtils;
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParagraphGroup;
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParser;
import org.quantumbadger.redreader.reddit.things.RedditPost;
import org.quantumbadger.redreader.reddit.things.RedditThingWithIdAndType;

public class RedditParsedPost implements RedditThingWithIdAndType {
    private final String mFlairText;
    private final String mPermalink;
    private final MarkdownParagraphGroup mSelfText;
    private final RedditPost mSrc;
    private final String mTitle;
    private final String mUrl;

    public RedditParsedPost(RedditPost src, boolean parseSelfText) {
        this.mSrc = src;
        if (src.title == null) {
            this.mTitle = "[null]";
        } else {
            this.mTitle = StringEscapeUtils.unescapeHtml4(src.title.replace(10, ' ')).trim();
        }
        this.mUrl = StringEscapeUtils.unescapeHtml4(src.getUrl());
        this.mPermalink = StringEscapeUtils.unescapeHtml4(src.permalink);
        if (!parseSelfText || !src.is_self || src.selftext == null || src.selftext.trim().length() <= 0) {
            this.mSelfText = null;
        } else {
            this.mSelfText = MarkdownParser.parse(StringEscapeUtils.unescapeHtml4(src.selftext).toCharArray());
        }
        if (src.link_flair_text == null || src.link_flair_text.length() <= 0) {
            this.mFlairText = null;
        } else {
            this.mFlairText = StringEscapeUtils.unescapeHtml4(src.link_flair_text);
        }
    }

    public String getIdAlone() {
        return this.mSrc.getIdAlone();
    }

    public String getIdAndType() {
        return this.mSrc.getIdAndType();
    }

    public String getTitle() {
        return this.mTitle;
    }

    public String getUrl() {
        return this.mUrl;
    }

    public String getPermalink() {
        return this.mPermalink;
    }

    public boolean isStickied() {
        return this.mSrc.stickied;
    }

    public RedditPost getSrc() {
        return this.mSrc;
    }

    public String getThumbnailUrl() {
        return this.mSrc.thumbnail;
    }

    public boolean isArchived() {
        return this.mSrc.archived;
    }

    public String getAuthor() {
        return this.mSrc.author;
    }

    public String getRawSelfText() {
        return this.mSrc.selftext;
    }

    public boolean isSpoiler() {
        return Boolean.TRUE.equals(this.mSrc.spoiler);
    }

    public String getUnescapedSelfText() {
        return StringEscapeUtils.unescapeHtml4(this.mSrc.selftext);
    }

    public String getSubreddit() {
        return this.mSrc.subreddit;
    }

    public int getScoreExcludingOwnVote() {
        int score = this.mSrc.score;
        if (Boolean.TRUE.equals(this.mSrc.likes)) {
            score--;
        }
        if (Boolean.FALSE.equals(this.mSrc.likes)) {
            return score + 1;
        }
        return score;
    }

    public int getGoldAmount() {
        return this.mSrc.gilded;
    }

    public boolean isNsfw() {
        return this.mSrc.over_18;
    }

    public String getFlairText() {
        return this.mFlairText;
    }

    public long getCreatedTimeSecsUTC() {
        return this.mSrc.created_utc;
    }

    public String getDomain() {
        return this.mSrc.domain;
    }

    public boolean isSelfPost() {
        return this.mSrc.is_self;
    }

    public MarkdownParagraphGroup getSelfText() {
        return this.mSelfText;
    }
}
