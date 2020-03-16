package org.quantumbadger.redreader.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.internal.view.SupportMenu;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.apache.commons.lang3.StringUtils;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.BetterSSB;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;

public class RedditPostHeaderView extends LinearLayout {
    private final RedditPreparedPost post;
    private final TextView subtitle;

    public RedditPostHeaderView(final AppCompatActivity activity, final RedditPreparedPost post2) {
        super(activity);
        this.post = post2;
        float dpScale = activity.getResources().getDisplayMetrics().density;
        setOrientation(1);
        int sidesPadding = (int) (15.0f * dpScale);
        int topPadding = (int) (10.0f * dpScale);
        setPadding(sidesPadding, topPadding, sidesPadding, topPadding);
        Typeface tf = Typeface.createFromAsset(activity.getAssets(), "fonts/Roboto-Light.ttf");
        TextView title = new TextView(activity);
        title.setTextSize(19.0f);
        title.setTypeface(tf);
        title.setText(post2.src.getTitle());
        title.setTextColor(-1);
        addView(title);
        this.subtitle = new TextView(activity);
        this.subtitle.setTextSize(13.0f);
        rebuildSubtitle(activity);
        this.subtitle.setTextColor(Color.rgb(200, 200, 200));
        addView(this.subtitle);
        TypedArray appearance = activity.obtainStyledAttributes(new int[]{R.attr.rrPostListHeaderBackgroundCol});
        setBackgroundColor(appearance.getColor(0, General.COLOR_INVALID));
        appearance.recycle();
        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (!post2.isSelf()) {
                    LinkHandler.onLinkClicked(activity, post2.src.getUrl(), false, post2.src.getSrc());
                }
            }
        });
        setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                RedditPreparedPost.showActionMenu(activity, post2);
                return true;
            }
        });
    }

    private void rebuildSubtitle(Context context) {
        int pointsCol;
        TypedArray appearance = context.obtainStyledAttributes(new int[]{R.attr.rrPostSubtitleBoldCol, R.attr.rrPostSubtitleUpvoteCol, R.attr.rrPostSubtitleDownvoteCol, R.attr.rrGoldTextCol, R.attr.rrGoldBackCol});
        int rrPostSubtitleUpvoteCol = appearance.getColor(1, 255);
        int rrPostSubtitleDownvoteCol = appearance.getColor(2, 255);
        int rrGoldTextCol = appearance.getColor(3, 255);
        int rrGoldBackCol = appearance.getColor(4, 255);
        appearance.recycle();
        BetterSSB postListDescSb = new BetterSSB();
        if (this.post.isUpvoted()) {
            pointsCol = rrPostSubtitleUpvoteCol;
        } else if (this.post.isDownvoted()) {
            pointsCol = rrPostSubtitleDownvoteCol;
        } else {
            pointsCol = -1;
        }
        if (this.post.src.isNsfw()) {
            postListDescSb.append(" NSFW ", 49, -1, SupportMenu.CATEGORY_MASK, 1.0f);
            postListDescSb.append("  ", 0);
        }
        postListDescSb.append(String.valueOf(this.post.computeScore()), 17, pointsCol, 0, 1.0f);
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.SPACE);
        sb.append(context.getString(R.string.subtitle_points));
        sb.append(StringUtils.SPACE);
        postListDescSb.append(sb.toString(), 0);
        if (this.post.src.getGoldAmount() > 0) {
            postListDescSb.append(StringUtils.SPACE, 0);
            StringBuilder sb2 = new StringBuilder();
            sb2.append(StringUtils.SPACE);
            sb2.append(context.getString(R.string.gold));
            sb2.append(BetterSSB.NBSP);
            sb2.append("x");
            sb2.append(this.post.src.getGoldAmount());
            sb2.append(StringUtils.SPACE);
            postListDescSb.append(sb2.toString(), 48, rrGoldTextCol, rrGoldBackCol, 1.0f);
            postListDescSb.append("  ", 0);
        }
        postListDescSb.append(RRTime.formatDurationFrom(context, this.post.src.getCreatedTimeSecsUTC() * 1000), 17, -1, 0, 1.0f);
        StringBuilder sb3 = new StringBuilder();
        sb3.append(StringUtils.SPACE);
        sb3.append(context.getString(R.string.subtitle_by));
        sb3.append(StringUtils.SPACE);
        postListDescSb.append(sb3.toString(), 0);
        postListDescSb.append(this.post.src.getAuthor(), 17, -1, 0, 1.0f);
        StringBuilder sb4 = new StringBuilder();
        sb4.append(StringUtils.SPACE);
        sb4.append(context.getString(R.string.subtitle_to));
        sb4.append(StringUtils.SPACE);
        postListDescSb.append(sb4.toString(), 0);
        postListDescSb.append(this.post.src.getSubreddit(), 17, -1, 0, 1.0f);
        StringBuilder sb5 = new StringBuilder();
        sb5.append(" (");
        sb5.append(this.post.src.getDomain());
        sb5.append(")");
        postListDescSb.append(sb5.toString(), 0);
        this.subtitle.setText(postListDescSb.get());
    }
}
