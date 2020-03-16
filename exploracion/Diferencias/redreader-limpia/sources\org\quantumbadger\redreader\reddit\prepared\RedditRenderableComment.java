package org.quantumbadger.redreader.reddit.prepared;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeConstants;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.common.BetterSSB;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceCommentHeaderItem;
import org.quantumbadger.redreader.common.RRThemeAttributes;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.reddit.api.RedditAPICommentAction;
import org.quantumbadger.redreader.reddit.things.RedditComment;
import org.quantumbadger.redreader.reddit.things.RedditThingWithIdAndType;

public class RedditRenderableComment implements RedditRenderableInboxItem, RedditThingWithIdAndType {
    private final RedditParsedComment mComment;
    private final Integer mMinimumCommentScore;
    private final String mParentPostAuthor;
    private final boolean mShowScore;

    public RedditRenderableComment(RedditParsedComment comment, String parentPostAuthor, Integer minimumCommentScore, boolean showScore) {
        this.mComment = comment;
        this.mParentPostAuthor = parentPostAuthor;
        this.mMinimumCommentScore = minimumCommentScore;
        this.mShowScore = showScore;
    }

    private int computeScore(RedditChangeDataManager changeDataManager) {
        RedditComment rawComment = this.mComment.getRawComment();
        int score = rawComment.ups - rawComment.downs;
        if (Boolean.TRUE.equals(rawComment.likes)) {
            score--;
        }
        if (Boolean.FALSE.equals(rawComment.likes)) {
            score++;
        }
        if (changeDataManager.isUpvoted(this.mComment)) {
            return score + 1;
        }
        if (changeDataManager.isDownvoted(this.mComment)) {
            return score - 1;
        }
        return score;
    }

    public CharSequence getHeader(RRThemeAttributes theme, RedditChangeDataManager changeDataManager, Context context) {
        int pointsCol;
        int backgroundColour;
        boolean setBackgroundColour;
        RRThemeAttributes rRThemeAttributes = theme;
        RedditChangeDataManager redditChangeDataManager = changeDataManager;
        Context context2 = context;
        BetterSSB sb = new BetterSSB();
        RedditComment rawComment = this.mComment.getRawComment();
        int score = computeScore(redditChangeDataManager);
        if (redditChangeDataManager.isUpvoted(this.mComment)) {
            pointsCol = rRThemeAttributes.rrPostSubtitleUpvoteCol;
        } else if (redditChangeDataManager.isDownvoted(this.mComment)) {
            pointsCol = rRThemeAttributes.rrPostSubtitleDownvoteCol;
        } else {
            pointsCol = rRThemeAttributes.rrCommentHeaderBoldCol;
        }
        if (rRThemeAttributes.shouldShow(AppearanceCommentHeaderItem.AUTHOR)) {
            if (this.mParentPostAuthor != null && rawComment.author.equalsIgnoreCase(this.mParentPostAuthor) && !rawComment.author.equals("[deleted]")) {
                setBackgroundColour = true;
                backgroundColour = Color.rgb(0, 126, DateTimeConstants.HOURS_PER_WEEK);
            } else if ("moderator".equals(rawComment.distinguished)) {
                setBackgroundColour = true;
                backgroundColour = Color.rgb(0, 170, 0);
            } else if ("admin".equals(rawComment.distinguished)) {
                setBackgroundColour = true;
                backgroundColour = Color.rgb(170, 0, 0);
            } else {
                setBackgroundColour = false;
                backgroundColour = 0;
            }
            if (setBackgroundColour) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append(StringUtils.SPACE);
                sb2.append(rawComment.author);
                sb2.append(StringUtils.SPACE);
                sb.append(sb2.toString(), 49, -1, backgroundColour, 1.0f);
            } else {
                sb.append(rawComment.author, 17, rRThemeAttributes.rrCommentHeaderAuthorCol, 0, 1.0f);
            }
        }
        String flair = this.mComment.getFlair();
        if (rRThemeAttributes.shouldShow(AppearanceCommentHeaderItem.FLAIR) && flair != null && flair.length() > 0) {
            if (rRThemeAttributes.shouldShow(AppearanceCommentHeaderItem.AUTHOR)) {
                sb.append("  ", 0);
            }
            StringBuilder sb3 = new StringBuilder();
            sb3.append(StringUtils.SPACE);
            sb3.append(flair);
            sb3.append(General.LTR_OVERRIDE_MARK);
            sb3.append(StringUtils.SPACE);
            sb.append(sb3.toString(), 48, rRThemeAttributes.rrFlairTextCol, rRThemeAttributes.rrFlairBackCol, 1.0f);
        }
        if (rRThemeAttributes.shouldShow(AppearanceCommentHeaderItem.AUTHOR) || rRThemeAttributes.shouldShow(AppearanceCommentHeaderItem.FLAIR)) {
            sb.append("   ", 0);
        }
        if (rRThemeAttributes.shouldShow(AppearanceCommentHeaderItem.SCORE) && this.mShowScore) {
            if (!Boolean.TRUE.equals(rawComment.score_hidden)) {
                sb.append(String.valueOf(score), 17, pointsCol, 0, 1.0f);
            } else {
                sb.append("??", 17, pointsCol, 0, 1.0f);
            }
            StringBuilder sb4 = new StringBuilder();
            sb4.append(StringUtils.SPACE);
            sb4.append(context2.getString(R.string.subtitle_points));
            sb4.append(StringUtils.SPACE);
            sb.append(sb4.toString(), 0);
        }
        if (rRThemeAttributes.shouldShow(AppearanceCommentHeaderItem.GOLD) && rawComment.gilded > 0) {
            sb.append(StringUtils.SPACE, 0);
            StringBuilder sb5 = new StringBuilder();
            sb5.append(StringUtils.SPACE);
            sb5.append(context2.getString(R.string.gold));
            sb5.append(BetterSSB.NBSP);
            sb5.append("x");
            sb5.append(rawComment.gilded);
            sb5.append(StringUtils.SPACE);
            sb.append(sb5.toString(), 48, rRThemeAttributes.rrGoldTextCol, rRThemeAttributes.rrGoldBackCol, 1.0f);
            sb.append("  ", 0);
        }
        if (rRThemeAttributes.shouldShow(AppearanceCommentHeaderItem.AGE)) {
            sb.append(RRTime.formatDurationFrom(context2, rawComment.created_utc * 1000), 17, rRThemeAttributes.rrCommentHeaderBoldCol, 0, 1.0f);
            if (rawComment.edited != null && (rawComment.edited instanceof Long)) {
                sb.append("*", 17, rRThemeAttributes.rrCommentHeaderBoldCol, 0, 1.0f);
            }
        }
        return sb.get();
    }

    public View getBody(AppCompatActivity activity, Integer textColor, Float textSize, boolean showLinkButtons) {
        return this.mComment.getBody().buildView(activity, textColor, textSize, showLinkButtons);
    }

    public void handleInboxClick(AppCompatActivity activity) {
        LinkHandler.onLinkClicked(activity, Reddit.getUri(this.mComment.getRawComment().context).toString());
    }

    public void handleInboxLongClick(AppCompatActivity activity) {
        RedditAPICommentAction.showActionMenu(activity, null, this, null, RedditChangeDataManager.getInstance(RedditAccountManager.getInstance(activity).getDefaultAccount()), false);
    }

    public String getIdAlone() {
        return this.mComment.getIdAlone();
    }

    public String getIdAndType() {
        return this.mComment.getIdAndType();
    }

    public RedditParsedComment getParsedComment() {
        return this.mComment;
    }

    private boolean isScoreBelowThreshold(RedditChangeDataManager changeDataManager) {
        boolean z = false;
        if (this.mMinimumCommentScore == null || Boolean.TRUE.equals(this.mComment.getRawComment().score_hidden)) {
            return false;
        }
        if (computeScore(changeDataManager) < this.mMinimumCommentScore.intValue()) {
            z = true;
        }
        return z;
    }

    public boolean isCollapsed(RedditChangeDataManager changeDataManager) {
        Boolean collapsed = changeDataManager.isHidden(this);
        if (collapsed != null) {
            return collapsed.booleanValue();
        }
        return isScoreBelowThreshold(changeDataManager);
    }
}
