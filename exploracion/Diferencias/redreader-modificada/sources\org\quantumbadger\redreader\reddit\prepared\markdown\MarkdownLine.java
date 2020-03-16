package org.quantumbadger.redreader.reddit.prepared.markdown;

import org.quantumbadger.redreader.reddit.prepared.markdown.MarkdownParser.MarkdownParagraphType;

public final class MarkdownLine {
    public final int level;
    public final int number;
    public final int prefixLength;
    public final int spacesAtEnd;
    public final int spacesAtStart;
    public final CharArrSubstring src;
    public final MarkdownParagraphType type;

    MarkdownLine(CharArrSubstring src2, MarkdownParagraphType type2, int spacesAtStart2, int spacesAtEnd2, int prefixLength2, int level2, int number2) {
        this.src = src2;
        this.type = type2;
        this.spacesAtStart = spacesAtStart2;
        this.spacesAtEnd = spacesAtEnd2;
        this.prefixLength = prefixLength2;
        this.level = level2;
        this.number = number2;
    }

    public static MarkdownLine generate(CharArrSubstring src2) {
        CharArrSubstring charArrSubstring = src2;
        int spacesAtStart2 = src2.countSpacesAtStart();
        int spacesAtEnd2 = src2.countSpacesAtEnd();
        if (spacesAtStart2 == charArrSubstring.length) {
            MarkdownLine markdownLine = new MarkdownLine(null, MarkdownParagraphType.EMPTY, 0, 0, 0, 0, 0);
            return markdownLine;
        } else if (spacesAtStart2 >= 4) {
            MarkdownLine markdownLine2 = new MarkdownLine(src2, MarkdownParagraphType.CODE, spacesAtStart2, spacesAtEnd2, 4, 0, 0);
            return markdownLine2;
        } else {
            char firstNonSpaceChar = charArrSubstring.charAt(spacesAtStart2);
            if (firstNonSpaceChar == '#') {
                MarkdownLine markdownLine3 = new MarkdownLine(src2, MarkdownParagraphType.HEADER, spacesAtStart2, spacesAtEnd2, charArrSubstring.countPrefixLengthIgnoringSpaces('#'), 0, 0);
                return markdownLine3;
            } else if (firstNonSpaceChar == '*' || firstNonSpaceChar == '-') {
                if (charArrSubstring.length > spacesAtStart2 + 1 && charArrSubstring.charAt(spacesAtStart2 + 1) == ' ') {
                    MarkdownLine markdownLine4 = new MarkdownLine(src2, MarkdownParagraphType.BULLET, spacesAtStart2, spacesAtEnd2, spacesAtStart2 + 2, spacesAtStart2 == 0 ? 0 : 1, 0);
                    return markdownLine4;
                } else if (charArrSubstring.length < 3 || !charArrSubstring.isRepeatingChar('*', spacesAtStart2, charArrSubstring.length - spacesAtEnd2)) {
                    MarkdownLine markdownLine5 = new MarkdownLine(src2, MarkdownParagraphType.TEXT, spacesAtStart2, spacesAtEnd2, spacesAtStart2, 0, 0);
                    return markdownLine5;
                } else {
                    MarkdownLine markdownLine6 = new MarkdownLine(src2, MarkdownParagraphType.HLINE, 0, 0, 0, 0, 0);
                    return markdownLine6;
                }
            } else if (firstNonSpaceChar != '>') {
                switch (firstNonSpaceChar) {
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        CharArrSubstring num = charArrSubstring.readInteger(spacesAtStart2);
                        if (charArrSubstring.length > num.length + spacesAtStart2 + 2 && charArrSubstring.charAt(num.length + spacesAtStart2) == '.' && charArrSubstring.charAt(num.length + spacesAtStart2 + 1) == ' ') {
                            MarkdownLine markdownLine7 = new MarkdownLine(src2, MarkdownParagraphType.NUMBERED, spacesAtStart2, spacesAtEnd2, num.length + spacesAtStart2 + 2, spacesAtStart2 == 0 ? 0 : 1, Integer.parseInt(num.toString()));
                            return markdownLine7;
                        }
                        MarkdownLine markdownLine8 = new MarkdownLine(src2, MarkdownParagraphType.TEXT, spacesAtStart2, spacesAtEnd2, spacesAtStart2, 0, 0);
                        return markdownLine8;
                    default:
                        MarkdownLine markdownLine9 = new MarkdownLine(src2, MarkdownParagraphType.TEXT, spacesAtStart2, spacesAtEnd2, spacesAtStart2, 0, 0);
                        return markdownLine9;
                }
            } else {
                MarkdownLine markdownLine10 = new MarkdownLine(src2, MarkdownParagraphType.QUOTE, spacesAtStart2, spacesAtEnd2, charArrSubstring.countPrefixLengthIgnoringSpaces('>'), charArrSubstring.countPrefixLevelIgnoringSpaces('>'), 0);
                return markdownLine10;
            }
        }
    }

    public MarkdownLine rejoin(MarkdownLine toAppend) {
        this.src.arr[this.src.start + this.src.length] = ' ';
        MarkdownLine markdownLine = new MarkdownLine(this.src.rejoin(toAppend.src), this.type, this.spacesAtStart, toAppend.spacesAtEnd, this.prefixLength, this.level, this.number);
        return markdownLine;
    }

    public MarkdownParagraph tokenize(MarkdownParagraph parent) {
        int i = this.prefixLength;
        CharArrSubstring cleanedSrc = i == 0 ? this.src : this.src.substring(i);
        if (this.type == MarkdownParagraphType.CODE || this.type == MarkdownParagraphType.HLINE) {
            MarkdownParagraph markdownParagraph = new MarkdownParagraph(cleanedSrc, parent, this.type, null, this.level, this.number);
            return markdownParagraph;
        } else if (isPlainText()) {
            MarkdownParagraph markdownParagraph2 = new MarkdownParagraph(cleanedSrc, parent, this.type, null, this.level, this.number);
            return markdownParagraph2;
        } else {
            MarkdownParagraph markdownParagraph3 = new MarkdownParagraph(cleanedSrc, parent, this.type, MarkdownTokenizer.tokenize(cleanedSrc).substringAsArray(0), this.level, this.number);
            return markdownParagraph3;
        }
    }

    private boolean isPlainText() {
        for (int i = this.prefixLength; i < this.src.length; i++) {
            switch (this.src.arr[this.src.start + i]) {
                case '#':
                case '&':
                case '*':
                case '[':
                case '\\':
                case '^':
                case '_':
                case '`':
                case '~':
                    return false;
                case '/':
                    if (!this.src.equalAt(i + 1, "u/") && !this.src.equalAt(i + 1, "r/")) {
                        break;
                    } else {
                        return false;
                    }
                    break;
                case 'h':
                    if (!this.src.equalAt(i + 1, "ttp://") && !this.src.equalAt(i + 1, "ttps://")) {
                        break;
                    } else {
                        return false;
                    }
                case 'r':
                case 'u':
                    if (this.src.length > i + 1 && this.src.arr[this.src.start + i + 1] == '/') {
                        return false;
                    }
                case 'w':
                    if (!this.src.equalAt(i + 1, "ww.")) {
                        break;
                    } else {
                        return false;
                    }
            }
        }
        return true;
    }
}
