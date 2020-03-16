package org.quantumbadger.redreader.io;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface WritableObject<K> {

    public static class CreationData {
        public final String key;
        public final long timestamp;

        public CreationData(String key2, long timestamp2) {
            this.key = key2;
            this.timestamp = timestamp2;
        }
    }

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WritableField {
    }

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WritableObjectKey {
    }

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WritableObjectTimestamp {
    }

    @Target({ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WritableObjectVersion {
    }

    K getKey();

    long getTimestamp();
}
