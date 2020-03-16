package org.quantumbadger.redreader.views.liststatus;

import android.content.res.TypedArray;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL;

public final class CommentSubThreadView extends StatusListItemView {
    /* access modifiers changed from: private */
    public final PostCommentListingURL mUrl;

    public CommentSubThreadView(AppCompatActivity activity, PostCommentListingURL url, int messageRes) {
        final AppCompatActivity appCompatActivity = activity;
        super(activity);
        this.mUrl = url;
        TypedArray attr = appCompatActivity.obtainStyledAttributes(new int[]{R.attr.rrCommentSpecificThreadHeaderBackCol, R.attr.rrCommentSpecificThreadHeaderTextCol});
        int rrCommentSpecificThreadHeaderBackCol = attr.getColor(0, 0);
        int rrCommentSpecificThreadHeaderTextCol = attr.getColor(1, 0);
        attr.recycle();
        TextView textView = new TextView(appCompatActivity);
        textView.setText(messageRes);
        textView.setTextColor(rrCommentSpecificThreadHeaderTextCol);
        textView.setTextSize(15.0f);
        textView.setPadding((int) (this.dpScale * 15.0f), (int) (this.dpScale * 10.0f), (int) (this.dpScale * 10.0f), (int) (this.dpScale * 4.0f));
        TextView messageView = new TextView(appCompatActivity);
        messageView.setText(R.string.comment_header_specific_thread_message);
        messageView.setTextColor(rrCommentSpecificThreadHeaderTextCol);
        messageView.setTextSize(12.0f);
        messageView.setPadding((int) (this.dpScale * 15.0f), 0, (int) (this.dpScale * 10.0f), (int) (this.dpScale * 10.0f));
        LinearLayout layout = new LinearLayout(appCompatActivity);
        layout.setOrientation(1);
        layout.addView(textView);
        layout.addView(messageView);
        setContents(layout);
        setDescendantFocusability(393216);
        setBackgroundColor(rrCommentSpecificThreadHeaderBackCol);
        setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                LinkHandler.onLinkClicked(appCompatActivity, CommentSubThreadView.this.mUrl.commentId(null).toString());
            }
        });
    }
}
