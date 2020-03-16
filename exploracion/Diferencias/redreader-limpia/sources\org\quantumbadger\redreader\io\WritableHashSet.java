package org.quantumbadger.redreader.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import org.quantumbadger.redreader.common.UnexpectedInternalStateException;
import org.quantumbadger.redreader.io.WritableObject.CreationData;
import org.quantumbadger.redreader.io.WritableObject.WritableField;
import org.quantumbadger.redreader.io.WritableObject.WritableObjectKey;
import org.quantumbadger.redreader.io.WritableObject.WritableObjectTimestamp;
import org.quantumbadger.redreader.io.WritableObject.WritableObjectVersion;

public class WritableHashSet implements WritableObject<String> {
    @WritableObjectVersion
    public static int DB_VERSION = 1;
    private transient HashSet<String> hashSet = null;
    @WritableObjectKey
    private final String key;
    @WritableField
    private String serialised;
    @WritableObjectTimestamp
    private final long timestamp;

    public WritableHashSet(HashSet<String> data, long timestamp2, String key2) {
        this.hashSet = data;
        this.timestamp = timestamp2;
        this.key = key2;
        this.serialised = listToEscapedString(this.hashSet);
    }

    private WritableHashSet(String serializedData, long timestamp2, String key2) {
        this.timestamp = timestamp2;
        this.key = key2;
        this.serialised = serializedData;
    }

    public WritableHashSet(CreationData creationData) {
        this.timestamp = creationData.timestamp;
        this.key = creationData.key;
    }

    public String toString() {
        throw new UnexpectedInternalStateException("Using toString() is the wrong way to serialise a WritableHashSet");
    }

    public String serializeWithMetadata() {
        ArrayList<String> result = new ArrayList<>(3);
        result.add(this.serialised);
        result.add(String.valueOf(this.timestamp));
        result.add(this.key);
        return listToEscapedString(result);
    }

    public static WritableHashSet unserializeWithMetadata(String raw) {
        ArrayList<String> data = escapedStringToList(raw);
        return new WritableHashSet((String) data.get(0), Long.valueOf((String) data.get(1)).longValue(), (String) data.get(2));
    }

    public synchronized HashSet<String> toHashset() {
        if (this.hashSet != null) {
            return this.hashSet;
        }
        HashSet<String> hashSet2 = new HashSet<>(escapedStringToList(this.serialised));
        this.hashSet = hashSet2;
        return hashSet2;
    }

    public String getKey() {
        return this.key;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public static String listToEscapedString(Collection<String> list) {
        if (list.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String str : list) {
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == ';') {
                    sb.append("\\;");
                } else if (c != '\\') {
                    sb.append(c);
                } else {
                    sb.append("\\\\");
                }
            }
            sb.append(';');
        }
        return sb.toString();
    }

    public static ArrayList<String> escapedStringToList(String str) {
        if (str.length() > 0 && !str.endsWith(";")) {
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append(";");
            str = sb.toString();
        }
        ArrayList<String> result = new ArrayList<>();
        if (str != null) {
            boolean isEscaped = false;
            StringBuilder sb2 = new StringBuilder();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                boolean z = false;
                if (c == ';' && !isEscaped) {
                    result.add(sb2.toString());
                    sb2.setLength(0);
                } else if (c != '\\') {
                    sb2.append(c);
                } else if (isEscaped) {
                    sb2.append('\\');
                }
                if (c == '\\' && !isEscaped) {
                    z = true;
                }
                isEscaped = z;
            }
        }
        return result;
    }
}
