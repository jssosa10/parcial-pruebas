package org.quantumbadger.redreader.views;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import java.util.ArrayList;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.activities.MoreCommentsListingActivity;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.reddit.RedditCommentListItem;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL;
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;

public class LoadMoreCommentsView extends LinearLayout {
    /* access modifiers changed from: private */
    public final RedditURL mCommentListingURL;
    private final IndentView mIndentView;
    /* access modifiers changed from: private */
    public RedditCommentListItem mItem;
    private final TextView mTitleView;

    public LoadMoreCommentsView(final Context context, RedditURL commentListingURL) {
        super(context);
        this.mCommentListingURL = commentListingURL;
        setOrientation(1);
        View divider = new View(context);
        addView(divider);
        divider.getLayoutParams().width = -1;
        divider.getLayoutParams().height = 1;
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(0);
        addView(layout);
        int marginPx = General.dpToPixels(context, 8.0f);
        layout.setGravity(16);
        this.mIndentView = new IndentView(context);
        layout.addView(this.mIndentView);
        this.mIndentView.getLayoutParams().height = -1;
        TypedArray appearance = context.obtainStyledAttributes(new int[]{R.attr.rrIconForward, R.attr.rrListItemBackgroundCol, R.attr.rrListDividerCol});
        ImageView icon = new ImageView(context);
        icon.setImageDrawable(appearance.getDrawable(0));
        layout.setBackgroundColor(appearance.getColor(1, General.COLOR_INVALID));
        divider.setBackgroundColor(appearance.getColor(2, General.COLOR_INVALID));
        appearance.recycle();
        icon.setScaleX(0.75f);
        icon.setScaleY(0.75f);
        layout.addView(icon);
        ((LayoutParams) icon.getLayoutParams()).setMargins(marginPx, marginPx, marginPx, marginPx);
        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(1);
        layout.addView(textLayout);
        ((LayoutParams) textLayout.getLayoutParams()).setMargins(0, marginPx, marginPx, marginPx);
        this.mTitleView = new TextView(context);
        this.mTitleView.setText("Error: text not set");
        this.mTitleView.setTextSize(13.0f);
        textLayout.addView(this.mTitleView);
        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (LoadMoreCommentsView.this.mCommentListingURL.pathType() == 7) {
                    PostCommentListingURL listingUrl = LoadMoreCommentsView.this.mCommentListingURL.asPostCommentListURL();
                    ArrayList<String> commentIds = new ArrayList<>(16);
                    for (PostCommentListingURL url : LoadMoreCommentsView.this.mItem.asLoadMore().getMoreUrls(LoadMoreCommentsView.this.mCommentListingURL)) {
                        commentIds.add(url.commentId);
                    }
                    Intent intent = new Intent(context, MoreCommentsListingActivity.class);
                    intent.putExtra("postId", listingUrl.postId);
                    intent.putStringArrayListExtra("commentIds", commentIds);
                    context.startActivity(intent);
                    return;
                }
                General.quickToast(context, (int) R.string.load_more_comments_failed_unknown_url_type);
            }
        });
    }

    public void reset(RedditCommentListItem item) {
        this.mItem = item;
        StringBuilder title = new StringBuilder(getContext().getString(R.string.more_comments_button_text));
        int count = item.asLoadMore().getCount();
        title.append(getResources().getQuantityString(R.plurals.subtitle_replies, count, new Object[]{Integer.valueOf(count)}));
        this.mTitleView.setText(title);
        this.mIndentView.setIndentation(item.getIndent());
    }
}
