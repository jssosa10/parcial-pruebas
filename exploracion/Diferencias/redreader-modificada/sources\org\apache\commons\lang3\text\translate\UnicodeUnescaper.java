package org.apache.commons.lang3.text.translate;

import java.io.IOException;
import java.io.Writer;

@Deprecated
public class UnicodeUnescaper extends CharSequenceTranslator {
    public int translate(CharSequence input, int index, Writer out) throws IOException {
        if (input.charAt(index) != '\\' || index + 1 >= input.length() || input.charAt(index + 1) != 'u') {
            return 0;
        }
        int i = 2;
        while (index + i < input.length() && input.charAt(index + i) == 'u') {
            i++;
        }
        if (index + i < input.length() && input.charAt(index + i) == '+') {
            i++;
        }
        if (index + i + 4 <= input.length()) {
            CharSequence unicode = input.subSequence(index + i, index + i + 4);
            try {
                out.write((char) Integer.parseInt(unicode.toString(), 16));
                return i + 4;
            } catch (NumberFormatException nfe) {
                StringBuilder sb = new StringBuilder();
                sb.append("Unable to parse unicode value: ");
                sb.append(unicode);
                throw new IllegalArgumentException(sb.toString(), nfe);
            }
        } else {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Less than 4 hex digits in unicode value: '");
            sb2.append(input.subSequence(index, input.length()));
            sb2.append("' due to end of CharSequence");
            throw new IllegalArgumentException(sb2.toString());
        }
    }
}
