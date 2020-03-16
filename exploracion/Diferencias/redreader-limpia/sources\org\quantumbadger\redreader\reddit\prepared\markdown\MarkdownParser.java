package org.quantumbadger.redreader.reddit.prepared.markdown;

import java.util.ArrayList;
import java.util.Iterator;

public final class MarkdownParser {

    public enum MarkdownParagraphType {
        TEXT,
        CODE,
        BULLET,
        NUMBERED,
        QUOTE,
        HEADER,
        HLINE,
        EMPTY
    }

    /* JADX WARNING: Code restructure failed: missing block: B:50:0x0082, code lost:
        continue;
     */
    public static MarkdownParagraphGroup parse(char[] raw) {
        CharArrSubstring[] rawLines = CharArrSubstring.generateFromLines(raw);
        MarkdownLine[] lines = new MarkdownLine[rawLines.length];
        for (int i = 0; i < rawLines.length; i++) {
            lines[i] = MarkdownLine.generate(rawLines[i]);
        }
        ArrayList<MarkdownLine> mergedLines = new ArrayList<>(rawLines.length);
        MarkdownLine currentLine = null;
        for (int i2 = 0; i2 < lines.length; i2++) {
            if (currentLine != null) {
                switch (lines[i2].type) {
                    case QUOTE:
                    case BULLET:
                    case NUMBERED:
                    case CODE:
                    case HEADER:
                    case HLINE:
                        mergedLines.add(currentLine);
                        currentLine = lines[i2];
                        break;
                    case TEXT:
                        if (i2 >= 1) {
                            switch (lines[i2 - 1].type) {
                                case QUOTE:
                                case BULLET:
                                case NUMBERED:
                                case TEXT:
                                    if (lines[i2 - 1].spacesAtEnd < 2) {
                                        currentLine = currentLine.rejoin(lines[i2]);
                                        break;
                                    } else {
                                        mergedLines.add(currentLine);
                                        currentLine = lines[i2];
                                        break;
                                    }
                                case CODE:
                                case HEADER:
                                case HLINE:
                                    mergedLines.add(currentLine);
                                    currentLine = lines[i2];
                                    break;
                            }
                        } else {
                            throw new RuntimeException("Internal error: invalid paragrapher state");
                        }
                    case EMPTY:
                        mergedLines.add(currentLine);
                        currentLine = null;
                        break;
                }
            } else if (lines[i2].type != MarkdownParagraphType.EMPTY) {
                currentLine = lines[i2];
            }
        }
        if (currentLine != null) {
            mergedLines.add(currentLine);
        }
        ArrayList<MarkdownParagraph> outputParagraphs = new ArrayList<>(mergedLines.size());
        Iterator it = mergedLines.iterator();
        while (it.hasNext()) {
            MarkdownParagraph paragraph = ((MarkdownLine) it.next()).tokenize(outputParagraphs.isEmpty() ? null : (MarkdownParagraph) outputParagraphs.get(outputParagraphs.size() - 1));
            if (!paragraph.isEmpty()) {
                outputParagraphs.add(paragraph);
            }
        }
        return new MarkdownParagraphGroup((MarkdownParagraph[]) outputParagraphs.toArray(new MarkdownParagraph[outputParagraphs.size()]));
    }
}
