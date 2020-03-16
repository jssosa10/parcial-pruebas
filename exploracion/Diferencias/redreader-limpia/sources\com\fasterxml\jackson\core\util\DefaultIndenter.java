package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter.NopIndenter;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;

public class DefaultIndenter extends NopIndenter {
    private static final int INDENT_LEVELS = 16;
    public static final DefaultIndenter SYSTEM_LINEFEED_INSTANCE = new DefaultIndenter("  ", SYS_LF);
    public static final String SYS_LF;
    private static final long serialVersionUID = 1;
    private final int charsPerLevel;
    private final String eol;
    private final char[] indents;

    static {
        String lf;
        try {
            lf = System.getProperty("line.separator");
        } catch (Throwable th) {
            lf = StringUtils.LF;
        }
        SYS_LF = lf;
    }

    public DefaultIndenter() {
        this("  ", SYS_LF);
    }

    public DefaultIndenter(String indent, String eol2) {
        this.charsPerLevel = indent.length();
        this.indents = new char[(indent.length() * 16)];
        int offset = 0;
        for (int i = 0; i < 16; i++) {
            indent.getChars(0, indent.length(), this.indents, offset);
            offset += indent.length();
        }
        this.eol = eol2;
    }

    public DefaultIndenter withLinefeed(String lf) {
        if (lf.equals(this.eol)) {
            return this;
        }
        return new DefaultIndenter(getIndent(), lf);
    }

    public DefaultIndenter withIndent(String indent) {
        if (indent.equals(getIndent())) {
            return this;
        }
        return new DefaultIndenter(indent, this.eol);
    }

    public boolean isInline() {
        return false;
    }

    public void writeIndentation(JsonGenerator jg, int level) throws IOException {
        jg.writeRaw(this.eol);
        if (level > 0) {
            int level2 = level * this.charsPerLevel;
            while (true) {
                char[] cArr = this.indents;
                if (level2 > cArr.length) {
                    jg.writeRaw(cArr, 0, cArr.length);
                    level2 -= this.indents.length;
                } else {
                    jg.writeRaw(cArr, 0, level2);
                    return;
                }
            }
        }
    }

    public String getEol() {
        return this.eol;
    }

    public String getIndent() {
        return new String(this.indents, 0, this.charsPerLevel);
    }
}
