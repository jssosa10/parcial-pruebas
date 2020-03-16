package com.google.android.exoplayer2.text.webvtt;

import android.support.annotation.NonNull;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan.Standard;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import com.google.android.exoplayer2.text.webvtt.WebvttCue.Builder;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public final class WebvttCueParser {
    private static final char CHAR_AMPERSAND = '&';
    private static final char CHAR_GREATER_THAN = '>';
    private static final char CHAR_LESS_THAN = '<';
    private static final char CHAR_SEMI_COLON = ';';
    private static final char CHAR_SLASH = '/';
    private static final char CHAR_SPACE = ' ';
    public static final Pattern CUE_HEADER_PATTERN = Pattern.compile("^(\\S+)\\s+-->\\s+(\\S+)(.*)?$");
    private static final Pattern CUE_SETTING_PATTERN = Pattern.compile("(\\S+?):(\\S+)");
    private static final String ENTITY_AMPERSAND = "amp";
    private static final String ENTITY_GREATER_THAN = "gt";
    private static final String ENTITY_LESS_THAN = "lt";
    private static final String ENTITY_NON_BREAK_SPACE = "nbsp";
    private static final int STYLE_BOLD = 1;
    private static final int STYLE_ITALIC = 2;
    private static final String TAG = "WebvttCueParser";
    private static final String TAG_BOLD = "b";
    private static final String TAG_CLASS = "c";
    private static final String TAG_ITALIC = "i";
    private static final String TAG_LANG = "lang";
    private static final String TAG_UNDERLINE = "u";
    private static final String TAG_VOICE = "v";
    private final StringBuilder textBuilder = new StringBuilder();

    private static final class StartTag {
        private static final String[] NO_CLASSES = new String[0];
        public final String[] classes;
        public final String name;
        public final int position;
        public final String voice;

        private StartTag(String name2, int position2, String voice2, String[] classes2) {
            this.position = position2;
            this.name = name2;
            this.voice = voice2;
            this.classes = classes2;
        }

        public static StartTag buildStartTag(String fullTagExpression, int position2) {
            String voice2;
            String[] classes2;
            String fullTagExpression2 = fullTagExpression.trim();
            if (fullTagExpression2.isEmpty()) {
                return null;
            }
            int voiceStartIndex = fullTagExpression2.indexOf(StringUtils.SPACE);
            if (voiceStartIndex == -1) {
                voice2 = "";
            } else {
                voice2 = fullTagExpression2.substring(voiceStartIndex).trim();
                fullTagExpression2 = fullTagExpression2.substring(0, voiceStartIndex);
            }
            String[] nameAndClasses = Util.split(fullTagExpression2, "\\.");
            String name2 = nameAndClasses[0];
            if (nameAndClasses.length > 1) {
                classes2 = (String[]) Arrays.copyOfRange(nameAndClasses, 1, nameAndClasses.length);
            } else {
                classes2 = NO_CLASSES;
            }
            return new StartTag(name2, position2, voice2, classes2);
        }

        public static StartTag buildWholeCueVirtualTag() {
            return new StartTag("", 0, "", new String[0]);
        }
    }

    private static final class StyleMatch implements Comparable<StyleMatch> {
        public final int score;
        public final WebvttCssStyle style;

        public StyleMatch(int score2, WebvttCssStyle style2) {
            this.score = score2;
            this.style = style2;
        }

        public int compareTo(@NonNull StyleMatch another) {
            return this.score - another.score;
        }
    }

    public boolean parseCue(ParsableByteArray webvttData, Builder builder, List<WebvttCssStyle> styles) {
        String firstLine = webvttData.readLine();
        if (firstLine == null) {
            return false;
        }
        Matcher cueHeaderMatcher = CUE_HEADER_PATTERN.matcher(firstLine);
        if (cueHeaderMatcher.matches()) {
            return parseCue(null, cueHeaderMatcher, webvttData, builder, this.textBuilder, styles);
        }
        String secondLine = webvttData.readLine();
        if (secondLine == null) {
            return false;
        }
        Matcher cueHeaderMatcher2 = CUE_HEADER_PATTERN.matcher(secondLine);
        if (!cueHeaderMatcher2.matches()) {
            return false;
        }
        return parseCue(firstLine.trim(), cueHeaderMatcher2, webvttData, builder, this.textBuilder, styles);
    }

    static void parseCueSettingsList(String cueSettingsList, Builder builder) {
        Matcher cueSettingMatcher = CUE_SETTING_PATTERN.matcher(cueSettingsList);
        while (cueSettingMatcher.find()) {
            String name = cueSettingMatcher.group(1);
            String value = cueSettingMatcher.group(2);
            try {
                if ("line".equals(name)) {
                    parseLineAttribute(value, builder);
                } else if ("align".equals(name)) {
                    builder.setTextAlignment(parseTextAlignment(value));
                } else if ("position".equals(name)) {
                    parsePositionAttribute(value, builder);
                } else if ("size".equals(name)) {
                    builder.setWidth(WebvttParserUtil.parsePercentage(value));
                } else {
                    String str = TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Unknown cue setting ");
                    sb.append(name);
                    sb.append(":");
                    sb.append(value);
                    Log.w(str, sb.toString());
                }
            } catch (NumberFormatException e) {
                String str2 = TAG;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Skipping bad cue setting: ");
                sb2.append(cueSettingMatcher.group());
                Log.w(str2, sb2.toString());
            }
        }
    }

    static void parseCueText(String id, String markup, Builder builder, List<WebvttCssStyle> styles) {
        SpannableStringBuilder spannedText = new SpannableStringBuilder();
        ArrayDeque<StartTag> startTagStack = new ArrayDeque<>();
        List<StyleMatch> scratchStyleMatches = new ArrayList<>();
        int pos = 0;
        while (pos < markup.length()) {
            char curr = markup.charAt(pos);
            if (curr == '&') {
                int semiColonEndIndex = markup.indexOf(59, pos + 1);
                int spaceEndIndex = markup.indexOf(32, pos + 1);
                int entityEndIndex = semiColonEndIndex == -1 ? spaceEndIndex : spaceEndIndex == -1 ? semiColonEndIndex : Math.min(semiColonEndIndex, spaceEndIndex);
                if (entityEndIndex != -1) {
                    applyEntity(markup.substring(pos + 1, entityEndIndex), spannedText);
                    if (entityEndIndex == spaceEndIndex) {
                        spannedText.append(StringUtils.SPACE);
                    }
                    pos = entityEndIndex + 1;
                } else {
                    spannedText.append(curr);
                    pos++;
                }
            } else if (curr != '<') {
                spannedText.append(curr);
                pos++;
            } else if (pos + 1 >= markup.length()) {
                pos++;
            } else {
                int ltPos = pos;
                boolean isVoidTag = false;
                int i = 1;
                boolean isClosingTag = markup.charAt(ltPos + 1) == '/';
                pos = findEndOfTag(markup, ltPos + 1);
                if (markup.charAt(pos - 2) == '/') {
                    isVoidTag = true;
                }
                if (isClosingTag) {
                    i = 2;
                }
                String fullTagExpression = markup.substring(i + ltPos, isVoidTag ? pos - 2 : pos - 1);
                String tagName = getTagName(fullTagExpression);
                if (tagName != null && isSupportedTag(tagName)) {
                    if (isClosingTag) {
                        while (!startTagStack.isEmpty()) {
                            StartTag startTag = (StartTag) startTagStack.pop();
                            applySpansForTag(id, startTag, spannedText, styles, scratchStyleMatches);
                            if (startTag.name.equals(tagName)) {
                                break;
                            }
                        }
                    } else if (!isVoidTag) {
                        startTagStack.push(StartTag.buildStartTag(fullTagExpression, spannedText.length()));
                    }
                }
            }
        }
        while (!startTagStack.isEmpty()) {
            applySpansForTag(id, (StartTag) startTagStack.pop(), spannedText, styles, scratchStyleMatches);
        }
        applySpansForTag(id, StartTag.buildWholeCueVirtualTag(), spannedText, styles, scratchStyleMatches);
        builder.setText(spannedText);
    }

    private static boolean parseCue(String id, Matcher cueHeaderMatcher, ParsableByteArray webvttData, Builder builder, StringBuilder textBuilder2, List<WebvttCssStyle> styles) {
        try {
            builder.setStartTime(WebvttParserUtil.parseTimestampUs(cueHeaderMatcher.group(1))).setEndTime(WebvttParserUtil.parseTimestampUs(cueHeaderMatcher.group(2)));
            parseCueSettingsList(cueHeaderMatcher.group(3), builder);
            textBuilder2.setLength(0);
            while (true) {
                String readLine = webvttData.readLine();
                String line = readLine;
                if (!TextUtils.isEmpty(readLine)) {
                    if (textBuilder2.length() > 0) {
                        textBuilder2.append(StringUtils.LF);
                    }
                    textBuilder2.append(line.trim());
                } else {
                    parseCueText(id, textBuilder2.toString(), builder, styles);
                    return true;
                }
            }
        } catch (NumberFormatException e) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Skipping cue with bad header: ");
            sb.append(cueHeaderMatcher.group());
            Log.w(str, sb.toString());
            return false;
        }
    }

    private static void parseLineAttribute(String s, Builder builder) throws NumberFormatException {
        int commaIndex = s.indexOf(44);
        if (commaIndex != -1) {
            builder.setLineAnchor(parsePositionAnchor(s.substring(commaIndex + 1)));
            s = s.substring(0, commaIndex);
        } else {
            builder.setLineAnchor(Integer.MIN_VALUE);
        }
        if (s.endsWith("%")) {
            builder.setLine(WebvttParserUtil.parsePercentage(s)).setLineType(0);
            return;
        }
        int lineNumber = Integer.parseInt(s);
        if (lineNumber < 0) {
            lineNumber--;
        }
        builder.setLine((float) lineNumber).setLineType(1);
    }

    private static void parsePositionAttribute(String s, Builder builder) throws NumberFormatException {
        int commaIndex = s.indexOf(44);
        if (commaIndex != -1) {
            builder.setPositionAnchor(parsePositionAnchor(s.substring(commaIndex + 1)));
            s = s.substring(0, commaIndex);
        } else {
            builder.setPositionAnchor(Integer.MIN_VALUE);
        }
        builder.setPosition(WebvttParserUtil.parsePercentage(s));
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0048  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0061 A[RETURN] */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0062 A[RETURN] */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0063 A[RETURN] */
    private static int parsePositionAnchor(String s) {
        char c;
        int hashCode = s.hashCode();
        if (hashCode == -1364013995) {
            if (s.equals(TtmlNode.CENTER)) {
                c = 1;
                switch (c) {
                    case 0:
                        break;
                    case 1:
                    case 2:
                        break;
                    case 3:
                        break;
                }
            }
        } else if (hashCode == -1074341483) {
            if (s.equals("middle")) {
                c = 2;
                switch (c) {
                    case 0:
                        break;
                    case 1:
                    case 2:
                        break;
                    case 3:
                        break;
                }
            }
        } else if (hashCode == 100571) {
            if (s.equals(TtmlNode.END)) {
                c = 3;
                switch (c) {
                    case 0:
                        break;
                    case 1:
                    case 2:
                        break;
                    case 3:
                        break;
                }
            }
        } else if (hashCode == 109757538 && s.equals(TtmlNode.START)) {
            c = 0;
            switch (c) {
                case 0:
                    return 0;
                case 1:
                case 2:
                    return 1;
                case 3:
                    return 2;
                default:
                    String str = TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Invalid anchor value: ");
                    sb.append(s);
                    Log.w(str, sb.toString());
                    return Integer.MIN_VALUE;
            }
        }
        c = 65535;
        switch (c) {
            case 0:
                break;
            case 1:
            case 2:
                break;
            case 3:
                break;
        }
    }

    private static Alignment parseTextAlignment(String s) {
        char c;
        switch (s.hashCode()) {
            case -1364013995:
                if (s.equals(TtmlNode.CENTER)) {
                    c = 2;
                    break;
                }
            case -1074341483:
                if (s.equals("middle")) {
                    c = 3;
                    break;
                }
            case 100571:
                if (s.equals(TtmlNode.END)) {
                    c = 4;
                    break;
                }
            case 3317767:
                if (s.equals(TtmlNode.LEFT)) {
                    c = 1;
                    break;
                }
            case 108511772:
                if (s.equals(TtmlNode.RIGHT)) {
                    c = 5;
                    break;
                }
            case 109757538:
                if (s.equals(TtmlNode.START)) {
                    c = 0;
                    break;
                }
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
            case 1:
                return Alignment.ALIGN_NORMAL;
            case 2:
            case 3:
                return Alignment.ALIGN_CENTER;
            case 4:
            case 5:
                return Alignment.ALIGN_OPPOSITE;
            default:
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Invalid alignment value: ");
                sb.append(s);
                Log.w(str, sb.toString());
                return null;
        }
    }

    private static int findEndOfTag(String markup, int startPos) {
        int index = markup.indexOf(62, startPos);
        return index == -1 ? markup.length() : index + 1;
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0043  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x005f  */
    /* JADX WARNING: Removed duplicated region for block: B:24:0x0065  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x006b  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0071  */
    private static void applyEntity(String entity, SpannableStringBuilder spannedText) {
        char c;
        int hashCode = entity.hashCode();
        if (hashCode == 3309) {
            if (entity.equals(ENTITY_GREATER_THAN)) {
                c = 1;
                switch (c) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                }
            }
        } else if (hashCode == 3464) {
            if (entity.equals(ENTITY_LESS_THAN)) {
                c = 0;
                switch (c) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                }
            }
        } else if (hashCode == 96708) {
            if (entity.equals(ENTITY_AMPERSAND)) {
                c = 3;
                switch (c) {
                    case 0:
                        break;
                    case 1:
                        break;
                    case 2:
                        break;
                    case 3:
                        break;
                }
            }
        } else if (hashCode == 3374865 && entity.equals(ENTITY_NON_BREAK_SPACE)) {
            c = 2;
            switch (c) {
                case 0:
                    spannedText.append(CHAR_LESS_THAN);
                    return;
                case 1:
                    spannedText.append(CHAR_GREATER_THAN);
                    return;
                case 2:
                    spannedText.append(CHAR_SPACE);
                    return;
                case 3:
                    spannedText.append(CHAR_AMPERSAND);
                    return;
                default:
                    String str = TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("ignoring unsupported entity: '&");
                    sb.append(entity);
                    sb.append(";'");
                    Log.w(str, sb.toString());
                    return;
            }
        }
        c = 65535;
        switch (c) {
            case 0:
                break;
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
        }
    }

    private static boolean isSupportedTag(String tagName) {
        char c;
        switch (tagName.hashCode()) {
            case 98:
                if (tagName.equals(TAG_BOLD)) {
                    c = 0;
                    break;
                }
            case 99:
                if (tagName.equals(TAG_CLASS)) {
                    c = 1;
                    break;
                }
            case 105:
                if (tagName.equals(TAG_ITALIC)) {
                    c = 2;
                    break;
                }
            case 117:
                if (tagName.equals(TAG_UNDERLINE)) {
                    c = 4;
                    break;
                }
            case 118:
                if (tagName.equals(TAG_VOICE)) {
                    c = 5;
                    break;
                }
            case 3314158:
                if (tagName.equals(TAG_LANG)) {
                    c = 3;
                    break;
                }
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return true;
            default:
                return false;
        }
    }

    private static void applySpansForTag(String cueId, StartTag startTag, SpannableStringBuilder text, List<WebvttCssStyle> styles, List<StyleMatch> scratchStyleMatches) {
        char c;
        int start = startTag.position;
        int end = text.length();
        String str = startTag.name;
        switch (str.hashCode()) {
            case 0:
                if (str.equals("")) {
                    c = 6;
                    break;
                }
            case 98:
                if (str.equals(TAG_BOLD)) {
                    c = 0;
                    break;
                }
            case 99:
                if (str.equals(TAG_CLASS)) {
                    c = 3;
                    break;
                }
            case 105:
                if (str.equals(TAG_ITALIC)) {
                    c = 1;
                    break;
                }
            case 117:
                if (str.equals(TAG_UNDERLINE)) {
                    c = 2;
                    break;
                }
            case 118:
                if (str.equals(TAG_VOICE)) {
                    c = 5;
                    break;
                }
            case 3314158:
                if (str.equals(TAG_LANG)) {
                    c = 4;
                    break;
                }
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                text.setSpan(new StyleSpan(1), start, end, 33);
                break;
            case 1:
                text.setSpan(new StyleSpan(2), start, end, 33);
                break;
            case 2:
                text.setSpan(new UnderlineSpan(), start, end, 33);
                break;
            case 3:
            case 4:
            case 5:
            case 6:
                break;
            default:
                return;
        }
        scratchStyleMatches.clear();
        getApplicableStyles(styles, cueId, startTag, scratchStyleMatches);
        int styleMatchesCount = scratchStyleMatches.size();
        for (int i = 0; i < styleMatchesCount; i++) {
            applyStyleToText(text, ((StyleMatch) scratchStyleMatches.get(i)).style, start, end);
        }
    }

    private static void applyStyleToText(SpannableStringBuilder spannedText, WebvttCssStyle style, int start, int end) {
        if (style != null) {
            if (style.getStyle() != -1) {
                spannedText.setSpan(new StyleSpan(style.getStyle()), start, end, 33);
            }
            if (style.isLinethrough()) {
                spannedText.setSpan(new StrikethroughSpan(), start, end, 33);
            }
            if (style.isUnderline()) {
                spannedText.setSpan(new UnderlineSpan(), start, end, 33);
            }
            if (style.hasFontColor()) {
                spannedText.setSpan(new ForegroundColorSpan(style.getFontColor()), start, end, 33);
            }
            if (style.hasBackgroundColor()) {
                spannedText.setSpan(new BackgroundColorSpan(style.getBackgroundColor()), start, end, 33);
            }
            if (style.getFontFamily() != null) {
                spannedText.setSpan(new TypefaceSpan(style.getFontFamily()), start, end, 33);
            }
            if (style.getTextAlign() != null) {
                spannedText.setSpan(new Standard(style.getTextAlign()), start, end, 33);
            }
            switch (style.getFontSizeUnit()) {
                case 1:
                    spannedText.setSpan(new AbsoluteSizeSpan((int) style.getFontSize(), true), start, end, 33);
                    break;
                case 2:
                    spannedText.setSpan(new RelativeSizeSpan(style.getFontSize()), start, end, 33);
                    break;
                case 3:
                    spannedText.setSpan(new RelativeSizeSpan(style.getFontSize() / 100.0f), start, end, 33);
                    break;
            }
        }
    }

    private static String getTagName(String tagExpression) {
        String tagExpression2 = tagExpression.trim();
        if (tagExpression2.isEmpty()) {
            return null;
        }
        return Util.splitAtFirst(tagExpression2, "[ \\.]")[0];
    }

    private static void getApplicableStyles(List<WebvttCssStyle> declaredStyles, String id, StartTag tag, List<StyleMatch> output) {
        int styleCount = declaredStyles.size();
        for (int i = 0; i < styleCount; i++) {
            WebvttCssStyle style = (WebvttCssStyle) declaredStyles.get(i);
            int score = style.getSpecificityScore(id, tag.name, tag.classes, tag.voice);
            if (score > 0) {
                output.add(new StyleMatch(score, style));
            }
        }
        Collections.sort(output);
    }
}
