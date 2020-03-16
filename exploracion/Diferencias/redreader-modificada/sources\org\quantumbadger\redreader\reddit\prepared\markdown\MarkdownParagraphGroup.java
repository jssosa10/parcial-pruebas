package org.quantumbadger.redreader.reddit.prepared.markdown;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParagraph.Link;
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParser.MarkdownParagraphType;
import org.quantumbadger.redreader.views.LinkDetailsView;
import org.quantumbadger.redreader.views.LinkifiedTextView;

public final class MarkdownParagraphGroup {
    private final MarkdownParagraph[] paragraphs;

    public MarkdownParagraphGroup(MarkdownParagraph[] paragraphs2) {
        this.paragraphs = paragraphs2;
    }

    public ViewGroup buildView(AppCompatActivity activity, Integer textColor, Float textSize, boolean showLinkButtons) {
        char c;
        int codeLineSpacing;
        final AppCompatActivity appCompatActivity = activity;
        float dpScale = activity.getResources().getDisplayMetrics().density;
        int paragraphSpacing = (int) (dpScale * 6.0f);
        int codeLineSpacing2 = (int) (dpScale * 3.0f);
        int quoteBarWidth = (int) (3.0f * dpScale);
        char c2 = 5;
        LinearLayout layout = new LinearLayout(appCompatActivity);
        layout.setOrientation(1);
        MarkdownParagraph[] markdownParagraphArr = this.paragraphs;
        int length = markdownParagraphArr.length;
        int i = 0;
        while (i < length) {
            MarkdownParagraph paragraph = markdownParagraphArr[i];
            TextView tv = new LinkifiedTextView(appCompatActivity);
            tv.setText(paragraph.spanned, BufferType.SPANNABLE);
            if (textColor != null) {
                tv.setTextColor(textColor.intValue());
            }
            if (textSize != null) {
                tv.setTextSize(textSize.floatValue());
            }
            switch (paragraph.type) {
                case BULLET:
                    codeLineSpacing = codeLineSpacing2;
                    c = c2;
                    LinearLayout bulletItem = new LinearLayout(appCompatActivity);
                    int paddingPx = General.dpToPixels(appCompatActivity, 6.0f);
                    bulletItem.setPadding(paddingPx, paddingPx, paddingPx, 0);
                    TextView bullet = new TextView(appCompatActivity);
                    bullet.setText("•   ");
                    if (textSize != null) {
                        bullet.setTextSize(textSize.floatValue());
                    }
                    bulletItem.addView(bullet);
                    bulletItem.addView(tv);
                    layout.addView(bulletItem);
                    ((MarginLayoutParams) bulletItem.getLayoutParams()).leftMargin = (int) (((float) (paragraph.level == 0 ? 12 : 24)) * dpScale);
                    break;
                case NUMBERED:
                    codeLineSpacing = codeLineSpacing2;
                    c = c2;
                    LinearLayout numberedItem = new LinearLayout(appCompatActivity);
                    int paddingPx2 = General.dpToPixels(appCompatActivity, 6.0f);
                    numberedItem.setPadding(paddingPx2, paddingPx2, paddingPx2, 0);
                    TextView number = new TextView(appCompatActivity);
                    StringBuilder sb = new StringBuilder();
                    sb.append(paragraph.number);
                    sb.append(".   ");
                    number.setText(sb.toString());
                    if (textSize != null) {
                        number.setTextSize(textSize.floatValue());
                    }
                    numberedItem.addView(number);
                    numberedItem.addView(tv);
                    layout.addView(numberedItem);
                    ((MarginLayoutParams) numberedItem.getLayoutParams()).leftMargin = (int) (((float) (paragraph.level == 0 ? 12 : 24)) * dpScale);
                    break;
                case CODE:
                    codeLineSpacing = codeLineSpacing2;
                    c = c2;
                    tv.setTypeface(General.getMonoTypeface(activity));
                    tv.setText(paragraph.raw.arr, paragraph.raw.start, paragraph.raw.length);
                    layout.addView(tv);
                    if (paragraph.parent != null) {
                        ((MarginLayoutParams) tv.getLayoutParams()).topMargin = paragraph.parent.type == MarkdownParagraphType.CODE ? codeLineSpacing : paragraphSpacing;
                    }
                    ((MarginLayoutParams) tv.getLayoutParams()).leftMargin = (int) (dpScale * 6.0f);
                    break;
                case HEADER:
                    codeLineSpacing = codeLineSpacing2;
                    c = c2;
                    SpannableString underlinedText = new SpannableString(paragraph.spanned);
                    underlinedText.setSpan(new UnderlineSpan(), 0, underlinedText.length(), 17);
                    tv.setText(underlinedText);
                    layout.addView(tv);
                    if (paragraph.parent != null) {
                        ((MarginLayoutParams) tv.getLayoutParams()).topMargin = paragraphSpacing;
                        break;
                    }
                    break;
                case HLINE:
                    codeLineSpacing = codeLineSpacing2;
                    c = c2;
                    View hLine = new View(appCompatActivity);
                    layout.addView(hLine);
                    MarginLayoutParams hLineParams = (MarginLayoutParams) hLine.getLayoutParams();
                    hLineParams.width = -1;
                    hLineParams.height = (int) dpScale;
                    hLineParams.setMargins((int) (dpScale * 15.0f), paragraphSpacing, (int) (15.0f * dpScale), 0);
                    hLine.setBackgroundColor(Color.rgb(128, 128, 128));
                    break;
                case QUOTE:
                    LinearLayout quoteLayout = new LinearLayout(appCompatActivity);
                    int lvl = 0;
                    while (true) {
                        codeLineSpacing = codeLineSpacing2;
                        if (lvl >= Math.min(5, paragraph.level)) {
                            c = c2;
                            quoteLayout.addView(tv);
                            layout.addView(quoteLayout);
                            if (paragraph.parent != null) {
                                if (paragraph.parent.type != MarkdownParagraphType.QUOTE) {
                                    ((MarginLayoutParams) quoteLayout.getLayoutParams()).topMargin = paragraphSpacing;
                                    break;
                                } else {
                                    ((MarginLayoutParams) tv.getLayoutParams()).topMargin = paragraphSpacing;
                                    break;
                                }
                            }
                        } else {
                            View quoteIndent = new View(appCompatActivity);
                            quoteLayout.addView(quoteIndent);
                            char c3 = c2;
                            quoteIndent.setBackgroundColor(Color.rgb(128, 128, 128));
                            quoteIndent.getLayoutParams().width = quoteBarWidth;
                            quoteIndent.getLayoutParams().height = -1;
                            ((MarginLayoutParams) quoteIndent.getLayoutParams()).rightMargin = quoteBarWidth;
                            lvl++;
                            codeLineSpacing2 = codeLineSpacing;
                            c2 = c3;
                        }
                    }
                    break;
                case TEXT:
                    layout.addView(tv);
                    if (paragraph.parent == null) {
                        codeLineSpacing = codeLineSpacing2;
                        c = c2;
                        break;
                    } else {
                        ((MarginLayoutParams) tv.getLayoutParams()).topMargin = paragraphSpacing;
                        codeLineSpacing = codeLineSpacing2;
                        c = c2;
                        break;
                    }
                case EMPTY:
                    throw new RuntimeException("Internal error: empty paragraph when building view");
                default:
                    codeLineSpacing = codeLineSpacing2;
                    c = c2;
                    break;
            }
            if (showLinkButtons) {
                for (final Link link : paragraph.links) {
                    LinkDetailsView ldv = new LinkDetailsView(appCompatActivity, link.title, link.subtitle);
                    layout.addView(ldv);
                    int linkMarginPx = Math.round(8.0f * dpScale);
                    float dpScale2 = dpScale;
                    ((LayoutParams) ldv.getLayoutParams()).setMargins(0, linkMarginPx, 0, linkMarginPx);
                    ldv.getLayoutParams().width = -1;
                    ldv.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            link.onClicked(appCompatActivity);
                        }
                    });
                    ldv.setOnLongClickListener(new OnLongClickListener() {
                        public boolean onLongClick(View v) {
                            link.onLongClicked(appCompatActivity);
                            return true;
                        }
                    });
                    dpScale = dpScale2;
                }
            }
            i++;
            dpScale = dpScale;
            codeLineSpacing2 = codeLineSpacing;
            c2 = c;
        }
        int i2 = codeLineSpacing2;
        char c4 = c2;
        return layout;
    }
}
