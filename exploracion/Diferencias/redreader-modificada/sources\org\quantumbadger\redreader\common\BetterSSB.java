package org.quantumbadger.redreader.common;

import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import java.util.Iterator;

public class BetterSSB {
    public static final int BACKGROUND_COLOR = 32;
    public static final int BOLD = 1;
    public static final int FOREGROUND_COLOR = 16;
    public static final int ITALIC = 2;
    public static final char NBSP = ' ';
    public static final int SIZE = 64;
    public static final int STRIKETHROUGH = 8;
    public static final int UNDERLINE = 4;
    private final SpannableStringBuilder sb = new SpannableStringBuilder();

    public void append(String str, int flags) {
        append(str, flags, 0, 0, 1.0f);
    }

    public void append(String str, int flags, String url) {
        append(str, flags, 0, 0, 1.0f, url);
    }

    public void append(String str, int flags, int foregroundCol, int backgroundCol, float scale) {
        append(str, flags, foregroundCol, backgroundCol, scale, null);
    }

    public void append(String str, int flags, int foregroundCol, int backgroundCol, float scale, String url) {
        int strStart = this.sb.length();
        this.sb.append(str);
        int strEnd = this.sb.length();
        if ((flags & 1) != 0) {
            this.sb.setSpan(new StyleSpan(1), strStart, strEnd, 17);
        }
        if ((flags & 2) != 0) {
            this.sb.setSpan(new StyleSpan(2), strStart, strEnd, 17);
        }
        if ((flags & 4) != 0) {
            this.sb.setSpan(new UnderlineSpan(), strStart, strEnd, 17);
        }
        if ((flags & 8) != 0) {
            this.sb.setSpan(new StrikethroughSpan(), strStart, strEnd, 17);
        }
        if ((flags & 16) != 0) {
            this.sb.setSpan(new ForegroundColorSpan(foregroundCol), strStart, strEnd, 17);
        }
        if ((flags & 32) != 0) {
            this.sb.setSpan(new BackgroundColorSpan(backgroundCol), strStart, strEnd, 17);
        }
        if ((flags & 64) != 0) {
            this.sb.setSpan(new RelativeSizeSpan(scale), strStart, strEnd, 17);
        }
        if (url != null) {
            this.sb.setSpan(new URLSpan(url), strStart, strEnd, 17);
        }
    }

    public void linkify() {
        String asText = this.sb.toString();
        Iterator it = LinkHandler.computeAllLinks(asText).iterator();
        while (it.hasNext()) {
            String link = (String) it.next();
            int index = -1;
            while (index < asText.length()) {
                int indexOf = asText.indexOf(link, index + 1);
                index = indexOf;
                if (indexOf < 0) {
                    break;
                } else if (((URLSpan[]) this.sb.getSpans(index, link.length() + index, URLSpan.class)).length < 1) {
                    this.sb.setSpan(new URLSpan(link), index, link.length() + index, 17);
                }
            }
        }
    }

    public SpannableStringBuilder get() {
        return this.sb;
    }
}
