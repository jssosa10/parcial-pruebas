package org.quantumbadger.redreader.reddit.prepared.markdown;

import android.net.Uri;
import android.net.Uri.Builder;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParser.MarkdownParagraphType;
import org.quantumbadger.redreader.views.LinkifiedTextView;

public final class MarkdownParagraph {
    final int level;
    final List<Link> links = new ArrayList();
    final int number;
    final MarkdownParagraph parent;
    final CharArrSubstring raw;
    final Spanned spanned = internalGenerateSpanned();
    final int[] tokens;
    final MarkdownParagraphType type;

    public class Link {
        final String subtitle;
        final String title;
        private final String url;

        public Link(String title2, String subtitle2, String url2) {
            this.title = title2;
            this.subtitle = subtitle2;
            this.url = url2;
        }

        public void onClicked(AppCompatActivity activity) {
            LinkHandler.onLinkClicked(activity, this.url, false);
        }

        public void onLongClicked(AppCompatActivity activity) {
            LinkHandler.onLinkLongClicked(activity, this.url);
        }
    }

    public MarkdownParagraph(CharArrSubstring raw2, MarkdownParagraph parent2, MarkdownParagraphType type2, int[] tokens2, int level2, int number2) {
        this.raw = raw2;
        this.parent = parent2;
        this.type = type2;
        this.tokens = tokens2;
        this.level = level2;
        this.number = number2;
        if (tokens2 == null && raw2 != null) {
            raw2.replaceUnicodeSpaces();
        }
    }

    private Spanned internalGenerateSpanned() {
        int i;
        int strikeStart;
        int italicStart;
        int boldStart;
        int strikeStart2;
        int italicStart2;
        String subtitle;
        if (this.type == MarkdownParagraphType.CODE || this.type == MarkdownParagraphType.HLINE) {
            return null;
        }
        if (this.tokens == null) {
            return new SpannableString(this.raw.toString());
        }
        SpannableStringBuilder builder = new SpannableStringBuilder();
        int boldStart2 = -1;
        int italicStart3 = -1;
        int strikeStart3 = -1;
        int linkStart = -1;
        int caretStart = -1;
        int parentOpenCount = 0;
        int parentCloseCount = 0;
        int i2 = 0;
        while (true) {
            int[] iArr = this.tokens;
            int i3 = 1;
            if (i2 < iArr.length) {
                int token = iArr[i2];
                if (token != 32) {
                    switch (token) {
                        case MarkdownTokenizer.TOKEN_BRACKET_SQUARE_CLOSE /*-9*/:
                            int urlStart = indexOf(iArr, -10, i2 + 1);
                            int urlEnd = indexOf(this.tokens, -11, urlStart + 1);
                            StringBuilder urlBuilder = new StringBuilder(urlEnd - urlStart);
                            for (int j = urlStart + 1; j < urlEnd; j++) {
                                urlBuilder.append((char) this.tokens[j]);
                            }
                            String linkText = String.valueOf(builder.subSequence(linkStart, builder.length()));
                            final String url = urlBuilder.toString();
                            int boldStart3 = boldStart2;
                            if (url.startsWith("/spoiler")) {
                                builder.delete(linkStart, builder.length());
                                builder.append("[Spoiler]");
                                Builder spoilerUriBuilder = Uri.parse("rr://msg/").buildUpon();
                                italicStart2 = italicStart3;
                                strikeStart2 = strikeStart3;
                                spoilerUriBuilder.appendQueryParameter("title", "Spoiler");
                                spoilerUriBuilder.appendQueryParameter("message", linkText);
                                int i4 = i2;
                                int i5 = urlStart;
                                Builder builder2 = spoilerUriBuilder;
                                this.links.add(new Link("Spoiler", null, spoilerUriBuilder.toString()));
                            } else {
                                italicStart2 = italicStart3;
                                strikeStart2 = strikeStart3;
                                int i6 = i2;
                                int i7 = urlStart;
                                if (url.length() > 3 && url.charAt(2) == ' ' && (url.charAt(0) == '#' || url.charAt(0) == '/')) {
                                    char charAt = url.charAt(1);
                                    if (charAt == 'b') {
                                        subtitle = "Spoiler: Book";
                                    } else if (charAt != 'g') {
                                        subtitle = "Spoiler";
                                    } else {
                                        subtitle = "Spoiler: Speculation";
                                    }
                                    Builder spoilerUriBuilder2 = Uri.parse("rr://msg/").buildUpon();
                                    spoilerUriBuilder2.appendQueryParameter("title", subtitle);
                                    spoilerUriBuilder2.appendQueryParameter("message", url.substring(3));
                                    this.links.add(new Link(linkText, subtitle, spoilerUriBuilder2.toString()));
                                } else {
                                    this.links.add(new Link(linkText, url, url));
                                }
                            }
                            builder.setSpan(new ClickableSpan() {
                                public void onClick(View widget) {
                                    LinkHandler.onLinkClicked(((LinkifiedTextView) widget).getActivity(), url);
                                }
                            }, linkStart, builder.length(), 17);
                            i2 = urlEnd;
                            boldStart2 = boldStart3;
                            italicStart3 = italicStart2;
                            strikeStart3 = strikeStart2;
                            continue;
                        case MarkdownTokenizer.TOKEN_BRACKET_SQUARE_OPEN /*-8*/:
                            linkStart = builder.length();
                            continue;
                        case MarkdownTokenizer.TOKEN_GRAVE /*-7*/:
                            int codeStart = builder.length();
                            while (true) {
                                int[] iArr2 = this.tokens;
                                i2 += i3;
                                if (iArr2[i2] != -7) {
                                    builder.append((char) iArr2[i2]);
                                    i3 = 1;
                                } else {
                                    builder.setSpan(new TypefaceSpan("monospace"), codeStart, builder.length(), 17);
                                    continue;
                                }
                            }
                        case MarkdownTokenizer.TOKEN_CARET /*-6*/:
                            if (caretStart < 0) {
                                caretStart = builder.length();
                                continue;
                            } else {
                                builder.append(' ');
                                boldStart = boldStart2;
                                italicStart = italicStart3;
                                strikeStart = strikeStart3;
                                i = i2;
                            }
                        case -5:
                            if (strikeStart3 != -1) {
                                builder.setSpan(new StrikethroughSpan(), strikeStart3, builder.length(), 17);
                                strikeStart3 = -1;
                                break;
                            } else {
                                strikeStart3 = builder.length();
                                continue;
                            }
                        case -4:
                        case -2:
                            if (boldStart2 >= 0) {
                                builder.setSpan(new StyleSpan(1), boldStart2, builder.length(), 17);
                                boldStart2 = -1;
                                break;
                            } else {
                                boldStart2 = builder.length();
                                continue;
                            }
                        case -3:
                        case -1:
                            if (italicStart3 >= 0) {
                                builder.setSpan(new StyleSpan(2), italicStart3, builder.length(), 17);
                                italicStart3 = -1;
                                break;
                            } else {
                                italicStart3 = builder.length();
                                continue;
                            }
                        default:
                            switch (token) {
                                case 40:
                                    if (caretStart < 0) {
                                        parentOpenCount = 0;
                                        builder.append('(');
                                        break;
                                    } else {
                                        parentOpenCount++;
                                        if (caretStart != builder.length()) {
                                            builder.append('(');
                                            break;
                                        } else {
                                            continue;
                                        }
                                    }
                                case 41:
                                    if (caretStart < 0) {
                                        parentCloseCount = 0;
                                        builder.append(')');
                                        break;
                                    } else {
                                        parentCloseCount++;
                                        if (parentOpenCount == parentCloseCount) {
                                            builder.setSpan(new SuperscriptSpan(), caretStart, builder.length(), 17);
                                            builder.setSpan(new RelativeSizeSpan(0.6f), caretStart, builder.length(), 17);
                                            caretStart = -1;
                                            break;
                                        } else {
                                            builder.append(')');
                                            continue;
                                            continue;
                                        }
                                    }
                                default:
                                    builder.append((char) token);
                                    boldStart = boldStart2;
                                    italicStart = italicStart3;
                                    strikeStart = strikeStart3;
                                    i = i2;
                            }
                    }
                } else {
                    boldStart = boldStart2;
                    italicStart = italicStart3;
                    strikeStart = strikeStart3;
                    i = i2;
                    builder.append(' ');
                    if (caretStart >= 0 && parentOpenCount == parentCloseCount) {
                        builder.setSpan(new SuperscriptSpan(), caretStart, builder.length(), 17);
                        builder.setSpan(new RelativeSizeSpan(0.6f), caretStart, builder.length(), 17);
                        caretStart = -1;
                        boldStart2 = boldStart;
                        italicStart3 = italicStart;
                        strikeStart3 = strikeStart;
                        i2 = i;
                        i2++;
                    }
                }
                boldStart2 = boldStart;
                italicStart3 = italicStart;
                strikeStart3 = strikeStart;
                i2 = i;
                i2++;
            } else {
                int i8 = boldStart2;
                int i9 = italicStart3;
                int i10 = strikeStart3;
                int i11 = i2;
                if (caretStart >= 0) {
                    builder.setSpan(new SuperscriptSpan(), caretStart, builder.length(), 17);
                    builder.setSpan(new RelativeSizeSpan(0.6f), caretStart, builder.length(), 17);
                }
                if (this.type == MarkdownParagraphType.HEADER) {
                    while (builder.length() > 0 && builder.charAt(builder.length() - 1) == '#') {
                        builder.delete(builder.length() - 1, builder.length());
                    }
                }
                return builder;
            }
        }
    }

    private static int indexOf(int[] haystack, int needle, int startPos) {
        for (int i = startPos; i < haystack.length; i++) {
            if (haystack[i] == needle) {
                return i;
            }
        }
        return -1;
    }

    public boolean isEmpty() {
        boolean z = false;
        if (this.type == MarkdownParagraphType.HLINE) {
            return false;
        }
        if (this.type == MarkdownParagraphType.EMPTY) {
            return true;
        }
        int[] iArr = this.tokens;
        if (iArr == null) {
            if (this.raw.countSpacesAtStart() == this.raw.length) {
                z = true;
            }
            return z;
        }
        for (int token : iArr) {
            if (!MarkdownTokenizer.isUnicodeWhitespace(token)) {
                return false;
            }
        }
        return true;
    }
}
