package org.quantumbadger.redreader.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import java.util.EnumSet;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceCommentHeaderItem;

public class RRThemeAttributes {
    public final int colorAccent;
    private final EnumSet<AppearanceCommentHeaderItem> mCommentHeaderItems;
    public final int rrCommentBodyCol;
    public final float rrCommentFontScale;
    public final int rrCommentHeaderAuthorCol;
    public final int rrCommentHeaderBoldCol;
    public final int rrCommentHeaderCol;
    public final int rrFlairBackCol;
    public final int rrFlairTextCol;
    public final int rrGoldBackCol;
    public final int rrGoldTextCol;
    public final int rrMainTextCol;
    public final int rrPostSubtitleDownvoteCol;
    public final int rrPostSubtitleUpvoteCol;

    public RRThemeAttributes(Context context) {
        TypedArray appearance = context.obtainStyledAttributes(new int[]{R.attr.rrCommentHeaderBoldCol, R.attr.rrCommentHeaderAuthorCol, R.attr.rrPostSubtitleUpvoteCol, R.attr.rrPostSubtitleDownvoteCol, R.attr.rrFlairBackCol, R.attr.rrFlairTextCol, R.attr.rrGoldBackCol, R.attr.rrGoldTextCol, R.attr.rrCommentHeaderCol, R.attr.rrCommentBodyCol, R.attr.rrMainTextCol, R.attr.colorAccent});
        this.rrCommentHeaderBoldCol = appearance.getColor(0, 255);
        this.rrCommentHeaderAuthorCol = appearance.getColor(1, 255);
        this.rrPostSubtitleUpvoteCol = appearance.getColor(2, 255);
        this.rrPostSubtitleDownvoteCol = appearance.getColor(3, 255);
        this.rrFlairBackCol = appearance.getColor(4, 0);
        this.rrFlairTextCol = appearance.getColor(5, 255);
        this.rrGoldBackCol = appearance.getColor(6, 0);
        this.rrGoldTextCol = appearance.getColor(7, 255);
        this.rrCommentHeaderCol = appearance.getColor(8, 255);
        this.rrCommentBodyCol = appearance.getColor(9, 255);
        this.rrMainTextCol = appearance.getColor(10, 255);
        this.colorAccent = appearance.getColor(11, 255);
        appearance.recycle();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.mCommentHeaderItems = PrefsUtility.appearance_comment_header_items(context, prefs);
        this.rrCommentFontScale = PrefsUtility.appearance_fontscale_inbox(context, prefs);
    }

    public boolean shouldShow(AppearanceCommentHeaderItem type) {
        return this.mCommentHeaderItems.contains(type);
    }
}
