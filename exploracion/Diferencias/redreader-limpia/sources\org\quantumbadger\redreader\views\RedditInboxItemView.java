package org.quantumbadger.redreader.views;

import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.RRThemeAttributes;
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager;
import org.quantumbadger.redreader.reddit.prepared.RedditRenderableInboxItem;

public class RedditInboxItemView extends LinearLayout {
    private RedditRenderableInboxItem currentItem = null;
    /* access modifiers changed from: private */
    public final AppCompatActivity mActivity;
    private final FrameLayout mBodyHolder;
    private final View mDivider;
    private final TextView mHeader;
    private final RRThemeAttributes mTheme;
    private final boolean showLinkButtons;

    public RedditInboxItemView(AppCompatActivity activity, RRThemeAttributes theme) {
        super(activity);
        this.mActivity = activity;
        this.mTheme = theme;
        setOrientation(1);
        this.mDivider = new View(activity);
        this.mDivider.setBackgroundColor(Color.argb(128, 128, 128, 128));
        addView(this.mDivider);
        this.mDivider.getLayoutParams().width = -1;
        this.mDivider.getLayoutParams().height = 1;
        LinearLayout inner = new LinearLayout(activity);
        inner.setOrientation(1);
        this.mHeader = new TextView(activity);
        this.mHeader.setTextSize(theme.rrCommentFontScale * 11.0f);
        this.mHeader.setTextColor(theme.rrCommentHeaderCol);
        inner.addView(this.mHeader);
        this.mHeader.getLayoutParams().width = -1;
        this.mBodyHolder = new FrameLayout(activity);
        this.mBodyHolder.setPadding(0, General.dpToPixels(activity, 2.0f), 0, 0);
        inner.addView(this.mBodyHolder);
        this.mBodyHolder.getLayoutParams().width = -1;
        int paddingPixels = General.dpToPixels(activity, 8.0f);
        inner.setPadding(paddingPixels + paddingPixels, paddingPixels, paddingPixels, paddingPixels);
        addView(inner);
        inner.getLayoutParams().width = -1;
        setDescendantFocusability(393216);
        this.showLinkButtons = PrefsUtility.pref_appearance_linkbuttons(activity, PreferenceManager.getDefaultSharedPreferences(activity));
        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                RedditInboxItemView redditInboxItemView = RedditInboxItemView.this;
                redditInboxItemView.handleInboxClick(redditInboxItemView.mActivity);
            }
        });
        setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                RedditInboxItemView redditInboxItemView = RedditInboxItemView.this;
                redditInboxItemView.handleInboxLongClick(redditInboxItemView.mActivity);
                return true;
            }
        });
    }

    public void reset(AppCompatActivity context, RedditChangeDataManager changeDataManager, RRThemeAttributes theme, RedditRenderableInboxItem item, boolean showDividerAtTop) {
        this.currentItem = item;
        this.mDivider.setVisibility(showDividerAtTop ? 0 : 8);
        this.mHeader.setText(item.getHeader(theme, changeDataManager, context));
        View body = item.getBody(context, Integer.valueOf(this.mTheme.rrCommentBodyCol), Float.valueOf(this.mTheme.rrCommentFontScale * 13.0f), this.showLinkButtons);
        this.mBodyHolder.removeAllViews();
        this.mBodyHolder.addView(body);
        body.getLayoutParams().width = -1;
    }

    public void handleInboxClick(AppCompatActivity activity) {
        RedditRenderableInboxItem redditRenderableInboxItem = this.currentItem;
        if (redditRenderableInboxItem != null) {
            redditRenderableInboxItem.handleInboxClick(activity);
        }
    }

    public void handleInboxLongClick(AppCompatActivity activity) {
        RedditRenderableInboxItem redditRenderableInboxItem = this.currentItem;
        if (redditRenderableInboxItem != null) {
            redditRenderableInboxItem.handleInboxLongClick(activity);
        }
    }
}
