package com.google.android.exoplayer2.upstream.cache;

import android.support.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class DefaultContentMetadata implements ContentMetadata {
    public static final DefaultContentMetadata EMPTY = new DefaultContentMetadata(Collections.emptyMap());
    private static final int MAX_VALUE_LENGTH = 10485760;
    private int hashCode;
    private final Map<String, byte[]> metadata;

    public static DefaultContentMetadata readFromStream(DataInputStream input) throws IOException {
        int size = input.readInt();
        HashMap<String, byte[]> metadata2 = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String name = input.readUTF();
            int valueSize = input.readInt();
            if (valueSize < 0 || valueSize > MAX_VALUE_LENGTH) {
                StringBuilder sb = new StringBuilder();
                sb.append("Invalid value size: ");
                sb.append(valueSize);
                throw new IOException(sb.toString());
            }
            byte[] value = new byte[valueSize];
            input.readFully(value);
            metadata2.put(name, value);
        }
        return new DefaultContentMetadata(metadata2);
    }

    private DefaultContentMetadata(Map<String, byte[]> metadata2) {
        this.metadata = Collections.unmodifiableMap(metadata2);
    }

    public DefaultContentMetadata copyWithMutationsApplied(ContentMetadataMutations mutations) {
        Map<String, byte[]> mutatedMetadata = applyMutations(this.metadata, mutations);
        if (isMetadataEqual(mutatedMetadata)) {
            return this;
        }
        return new DefaultContentMetadata(mutatedMetadata);
    }

    public void writeToStream(DataOutputStream output) throws IOException {
        output.writeInt(this.metadata.size());
        for (Entry<String, byte[]> entry : this.metadata.entrySet()) {
            output.writeUTF((String) entry.getKey());
            byte[] value = (byte[]) entry.getValue();
            output.writeInt(value.length);
            output.write(value);
        }
    }

    public final byte[] get(String name, byte[] defaultValue) {
        if (!this.metadata.containsKey(name)) {
            return defaultValue;
        }
        byte[] bytes = (byte[]) this.metadata.get(name);
        return Arrays.copyOf(bytes, bytes.length);
    }

    public final String get(String name, String defaultValue) {
        if (this.metadata.containsKey(name)) {
            return new String((byte[]) this.metadata.get(name), Charset.forName("UTF-8"));
        }
        return defaultValue;
    }

    public final long get(String name, long defaultValue) {
        if (this.metadata.containsKey(name)) {
            return ByteBuffer.wrap((byte[]) this.metadata.get(name)).getLong();
        }
        return defaultValue;
    }

    public final boolean contains(String name) {
        return this.metadata.containsKey(name);
    }

    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return isMetadataEqual(((DefaultContentMetadata) o).metadata);
    }

    private boolean isMetadataEqual(Map<String, byte[]> otherMetadata) {
        if (this.metadata.size() != otherMetadata.size()) {
            return false;
        }
        for (Entry<String, byte[]> entry : this.metadata.entrySet()) {
            if (!Arrays.equals((byte[]) entry.getValue(), (byte[]) otherMetadata.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        if (this.hashCode == 0) {
            int result = 0;
            for (Entry<String, byte[]> entry : this.metadata.entrySet()) {
                result += ((String) entry.getKey()).hashCode() ^ Arrays.hashCode((byte[]) entry.getValue());
            }
            this.hashCode = result;
        }
        return this.hashCode;
    }

    private static Map<String, byte[]> applyMutations(Map<String, byte[]> otherMetadata, ContentMetadataMutations mutations) {
        HashMap<String, byte[]> metadata2 = new HashMap<>(otherMetadata);
        removeValues(metadata2, mutations.getRemovedValues());
        addValues(metadata2, mutations.getEditedValues());
        return metadata2;
    }

    private static void removeValues(HashMap<String, byte[]> metadata2, List<String> names) {
        for (int i = 0; i < names.size(); i++) {
            metadata2.remove(names.get(i));
        }
    }

    private static void addValues(HashMap<String, byte[]> metadata2, Map<String, Object> values) {
        for (String name : values.keySet()) {
            byte[] bytes = getBytes(values.get(name));
            if (bytes.length <= MAX_VALUE_LENGTH) {
                metadata2.put(name, bytes);
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("The size of ");
                sb.append(name);
                sb.append(" (");
                sb.append(bytes.length);
                sb.append(") is greater than maximum allowed: ");
                sb.append(MAX_VALUE_LENGTH);
                throw new IllegalArgumentException(sb.toString());
            }
        }
    }

    private static byte[] getBytes(Object value) {
        if (value instanceof Long) {
            return ByteBuffer.allocate(8).putLong(((Long) value).longValue()).array();
        }
        if (value instanceof String) {
            return ((String) value).getBytes(Charset.forName("UTF-8"));
        }
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        throw new IllegalArgumentException();
    }
}
