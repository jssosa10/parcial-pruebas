package com.google.android.exoplayer2.text;

import android.annotation.TargetApi;
import android.graphics.Typeface;
import android.support.v4.view.ViewCompat;
import android.view.accessibility.CaptioningManager.CaptionStyle;
import com.google.android.exoplayer2.util.Util;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class CaptionStyleCompat {
    public static final CaptionStyleCompat DEFAULT;
    public static final int EDGE_TYPE_DEPRESSED = 4;
    public static final int EDGE_TYPE_DROP_SHADOW = 2;
    public static final int EDGE_TYPE_NONE = 0;
    public static final int EDGE_TYPE_OUTLINE = 1;
    public static final int EDGE_TYPE_RAISED = 3;
    public static final int USE_TRACK_COLOR_SETTINGS = 1;
    public final int backgroundColor;
    public final int edgeColor;
    public final int edgeType;
    public final int foregroundColor;
    public final Typeface typeface;
    public final int windowColor;

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface EdgeType {
    }

    static {
        CaptionStyleCompat captionStyleCompat = new CaptionStyleCompat(-1, ViewCompat.MEASURED_STATE_MASK, 0, 0, -1, null);
        DEFAULT = captionStyleCompat;
    }

    @TargetApi(19)
    public static CaptionStyleCompat createFromCaptionStyle(CaptionStyle captionStyle) {
        if (Util.SDK_INT >= 21) {
            return createFromCaptionStyleV21(captionStyle);
        }
        return createFromCaptionStyleV19(captionStyle);
    }

    public CaptionStyleCompat(int foregroundColor2, int backgroundColor2, int windowColor2, int edgeType2, int edgeColor2, Typeface typeface2) {
        this.foregroundColor = foregroundColor2;
        this.backgroundColor = backgroundColor2;
        this.windowColor = windowColor2;
        this.edgeType = edgeType2;
        this.edgeColor = edgeColor2;
        this.typeface = typeface2;
    }

    @TargetApi(19)
    private static CaptionStyleCompat createFromCaptionStyleV19(CaptionStyle captionStyle) {
        CaptionStyleCompat captionStyleCompat = new CaptionStyleCompat(captionStyle.foregroundColor, captionStyle.backgroundColor, 0, captionStyle.edgeType, captionStyle.edgeColor, captionStyle.getTypeface());
        return captionStyleCompat;
    }

    @TargetApi(21)
    private static CaptionStyleCompat createFromCaptionStyleV21(CaptionStyle captionStyle) {
        CaptionStyleCompat captionStyleCompat = new CaptionStyleCompat(captionStyle.hasForegroundColor() ? captionStyle.foregroundColor : DEFAULT.foregroundColor, captionStyle.hasBackgroundColor() ? captionStyle.backgroundColor : DEFAULT.backgroundColor, captionStyle.hasWindowColor() ? captionStyle.windowColor : DEFAULT.windowColor, captionStyle.hasEdgeType() ? captionStyle.edgeType : DEFAULT.edgeType, captionStyle.hasEdgeColor() ? captionStyle.edgeColor : DEFAULT.edgeColor, captionStyle.getTypeface());
        return captionStyleCompat;
    }
}
