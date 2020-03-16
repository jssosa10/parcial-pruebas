package org.apache.commons.lang3.text;

import java.text.Format;
import java.text.MessageFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;

@Deprecated
public class ExtendedMessageFormat extends MessageFormat {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static final String DUMMY_PATTERN = "";
    private static final char END_FE = '}';
    private static final int HASH_SEED = 31;
    private static final char QUOTE = '\'';
    private static final char START_FE = '{';
    private static final char START_FMT = ',';
    private static final long serialVersionUID = -2362048321261811743L;
    private final Map<String, ? extends FormatFactory> registry;
    private String toPattern;

    public ExtendedMessageFormat(String pattern) {
        this(pattern, Locale.getDefault());
    }

    public ExtendedMessageFormat(String pattern, Locale locale) {
        this(pattern, locale, null);
    }

    public ExtendedMessageFormat(String pattern, Map<String, ? extends FormatFactory> registry2) {
        this(pattern, Locale.getDefault(), registry2);
    }

    public ExtendedMessageFormat(String pattern, Locale locale, Map<String, ? extends FormatFactory> registry2) {
        super("");
        setLocale(locale);
        this.registry = registry2;
        applyPattern(pattern);
    }

    public String toPattern() {
        return this.toPattern;
    }

    public final void applyPattern(String pattern) {
        if (this.registry == null) {
            super.applyPattern(pattern);
            this.toPattern = super.toPattern();
            return;
        }
        ArrayList<Format> foundFormats = new ArrayList<>();
        ArrayList<String> foundDescriptions = new ArrayList<>();
        StringBuilder stripCustom = new StringBuilder(pattern.length());
        ParsePosition pos = new ParsePosition(0);
        char[] c = pattern.toCharArray();
        int fmtCount = 0;
        while (pos.getIndex() < pattern.length()) {
            char c2 = c[pos.getIndex()];
            if (c2 != '\'') {
                if (c2 == '{') {
                    fmtCount++;
                    seekNonWs(pattern, pos);
                    int start = pos.getIndex();
                    int index = readArgumentIndex(pattern, next(pos));
                    stripCustom.append(START_FE);
                    stripCustom.append(index);
                    seekNonWs(pattern, pos);
                    Format format = null;
                    String formatDescription = null;
                    if (c[pos.getIndex()] == ',') {
                        formatDescription = parseFormatDescription(pattern, next(pos));
                        format = getFormat(formatDescription);
                        if (format == null) {
                            stripCustom.append(START_FMT);
                            stripCustom.append(formatDescription);
                        }
                    }
                    foundFormats.add(format);
                    foundDescriptions.add(format == null ? null : formatDescription);
                    boolean z = true;
                    Validate.isTrue(foundFormats.size() == fmtCount);
                    if (foundDescriptions.size() != fmtCount) {
                        z = false;
                    }
                    Validate.isTrue(z);
                    if (c[pos.getIndex()] != '}') {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Unreadable format element at position ");
                        sb.append(start);
                        throw new IllegalArgumentException(sb.toString());
                    }
                }
                stripCustom.append(c[pos.getIndex()]);
                next(pos);
            } else {
                appendQuotedString(pattern, pos, stripCustom);
            }
        }
        super.applyPattern(stripCustom.toString());
        this.toPattern = insertFormats(super.toPattern(), foundDescriptions);
        if (containsElements(foundFormats)) {
            Format[] origFormats = getFormats();
            int i = 0;
            Iterator<Format> it = foundFormats.iterator();
            while (it.hasNext()) {
                Format f = (Format) it.next();
                if (f != null) {
                    origFormats[i] = f;
                }
                i++;
            }
            super.setFormats(origFormats);
        }
    }

    public void setFormat(int formatElementIndex, Format newFormat) {
        throw new UnsupportedOperationException();
    }

    public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {
        throw new UnsupportedOperationException();
    }

    public void setFormats(Format[] newFormats) {
        throw new UnsupportedOperationException();
    }

    public void setFormatsByArgumentIndex(Format[] newFormats) {
        throw new UnsupportedOperationException();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || !super.equals(obj) || ObjectUtils.notEqual(getClass(), obj.getClass())) {
            return false;
        }
        ExtendedMessageFormat rhs = (ExtendedMessageFormat) obj;
        if (ObjectUtils.notEqual(this.toPattern, rhs.toPattern)) {
            return false;
        }
        return true ^ ObjectUtils.notEqual(this.registry, rhs.registry);
    }

    public int hashCode() {
        return (((super.hashCode() * 31) + Objects.hashCode(this.registry)) * 31) + Objects.hashCode(this.toPattern);
    }

    private Format getFormat(String desc) {
        if (this.registry != null) {
            String name = desc;
            String args = null;
            int i = desc.indexOf(44);
            if (i > 0) {
                name = desc.substring(0, i).trim();
                args = desc.substring(i + 1).trim();
            }
            FormatFactory factory = (FormatFactory) this.registry.get(name);
            if (factory != null) {
                return factory.getFormat(name, args, getLocale());
            }
        }
        return null;
    }

    private int readArgumentIndex(String pattern, ParsePosition pos) {
        int start = pos.getIndex();
        seekNonWs(pattern, pos);
        StringBuilder result = new StringBuilder();
        boolean error = false;
        while (!error && pos.getIndex() < pattern.length()) {
            char c = pattern.charAt(pos.getIndex());
            if (Character.isWhitespace(c)) {
                seekNonWs(pattern, pos);
                c = pattern.charAt(pos.getIndex());
                if (!(c == ',' || c == '}')) {
                    error = true;
                    next(pos);
                }
            }
            if ((c == ',' || c == '}') && result.length() > 0) {
                try {
                    return Integer.parseInt(result.toString());
                } catch (NumberFormatException e) {
                }
            }
            error = !Character.isDigit(c);
            result.append(c);
            next(pos);
        }
        if (error) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid format argument index at position ");
            sb.append(start);
            sb.append(": ");
            sb.append(pattern.substring(start, pos.getIndex()));
            throw new IllegalArgumentException(sb.toString());
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Unterminated format element at position ");
        sb2.append(start);
        throw new IllegalArgumentException(sb2.toString());
    }

    private String parseFormatDescription(String pattern, ParsePosition pos) {
        int start = pos.getIndex();
        seekNonWs(pattern, pos);
        int text = pos.getIndex();
        int depth = 1;
        while (pos.getIndex() < pattern.length()) {
            char charAt = pattern.charAt(pos.getIndex());
            if (charAt == '\'') {
                getQuotedString(pattern, pos);
            } else if (charAt == '{') {
                depth++;
            } else if (charAt != '}') {
                continue;
            } else {
                depth--;
                if (depth == 0) {
                    return pattern.substring(text, pos.getIndex());
                }
            }
            next(pos);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Unterminated format element at position ");
        sb.append(start);
        throw new IllegalArgumentException(sb.toString());
    }

    private String insertFormats(String pattern, ArrayList<String> customPatterns) {
        if (!containsElements(customPatterns)) {
            return pattern;
        }
        StringBuilder sb = new StringBuilder(pattern.length() * 2);
        ParsePosition pos = new ParsePosition(0);
        int fe = -1;
        int depth = 0;
        while (pos.getIndex() < pattern.length()) {
            char c = pattern.charAt(pos.getIndex());
            if (c == '\'') {
                appendQuotedString(pattern, pos, sb);
            } else if (c != '{') {
                if (c == '}') {
                    depth--;
                }
                sb.append(c);
                next(pos);
            } else {
                depth++;
                sb.append(START_FE);
                sb.append(readArgumentIndex(pattern, next(pos)));
                if (depth == 1) {
                    fe++;
                    String customPattern = (String) customPatterns.get(fe);
                    if (customPattern != null) {
                        sb.append(START_FMT);
                        sb.append(customPattern);
                    }
                }
            }
        }
        return sb.toString();
    }

    private void seekNonWs(String pattern, ParsePosition pos) {
        char[] buffer = pattern.toCharArray();
        do {
            int len = StrMatcher.splitMatcher().isMatch(buffer, pos.getIndex());
            pos.setIndex(pos.getIndex() + len);
            if (len <= 0) {
                return;
            }
        } while (pos.getIndex() < pattern.length());
    }

    private ParsePosition next(ParsePosition pos) {
        pos.setIndex(pos.getIndex() + 1);
        return pos;
    }

    private StringBuilder appendQuotedString(String pattern, ParsePosition pos, StringBuilder appendTo) {
        if (appendTo != null) {
            appendTo.append(QUOTE);
        }
        next(pos);
        int start = pos.getIndex();
        char[] c = pattern.toCharArray();
        int lastHold = start;
        int i = pos.getIndex();
        while (i < pattern.length()) {
            if (c[pos.getIndex()] != '\'') {
                next(pos);
                i++;
            } else {
                next(pos);
                if (appendTo == null) {
                    return null;
                }
                appendTo.append(c, lastHold, pos.getIndex() - lastHold);
                return appendTo;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Unterminated quoted string at position ");
        sb.append(start);
        throw new IllegalArgumentException(sb.toString());
    }

    private void getQuotedString(String pattern, ParsePosition pos) {
        appendQuotedString(pattern, pos, null);
    }

    private boolean containsElements(Collection<?> coll) {
        if (coll == null || coll.isEmpty()) {
            return false;
        }
        for (Object name : coll) {
            if (name != null) {
                return true;
            }
        }
        return false;
    }
}
