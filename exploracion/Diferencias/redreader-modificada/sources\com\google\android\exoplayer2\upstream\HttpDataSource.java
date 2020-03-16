package com.google.android.exoplayer2.upstream;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Predicate;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface HttpDataSource extends DataSource {
    public static final Predicate<String> REJECT_PAYWALL_TYPES = $$Lambda$HttpDataSource$fzi4cgBB9tTB1JUdq8hmlAPFIw.INSTANCE;

    /* renamed from: com.google.android.exoplayer2.upstream.HttpDataSource$-CC reason: invalid class name */
    public final /* synthetic */ class CC {
        public static /* synthetic */ boolean lambda$static$0(String contentType) {
            String contentType2 = Util.toLowerInvariant(contentType);
            return !TextUtils.isEmpty(contentType2) && (!contentType2.contains(MimeTypes.BASE_TYPE_TEXT) || contentType2.contains(MimeTypes.TEXT_VTT)) && !contentType2.contains("html") && !contentType2.contains("xml");
        }
    }

    public static abstract class BaseFactory implements Factory {
        private final RequestProperties defaultRequestProperties = new RequestProperties();

        /* access modifiers changed from: protected */
        public abstract HttpDataSource createDataSourceInternal(RequestProperties requestProperties);

        public final HttpDataSource createDataSource() {
            return createDataSourceInternal(this.defaultRequestProperties);
        }

        public final RequestProperties getDefaultRequestProperties() {
            return this.defaultRequestProperties;
        }

        @Deprecated
        public final void setDefaultRequestProperty(String name, String value) {
            this.defaultRequestProperties.set(name, value);
        }

        @Deprecated
        public final void clearDefaultRequestProperty(String name) {
            this.defaultRequestProperties.remove(name);
        }

        @Deprecated
        public final void clearAllDefaultRequestProperties() {
            this.defaultRequestProperties.clear();
        }
    }

    public interface Factory extends com.google.android.exoplayer2.upstream.DataSource.Factory {

        /* renamed from: com.google.android.exoplayer2.upstream.HttpDataSource$Factory$-CC reason: invalid class name */
        public final /* synthetic */ class CC {
        }

        @Deprecated
        void clearAllDefaultRequestProperties();

        @Deprecated
        void clearDefaultRequestProperty(String str);

        HttpDataSource createDataSource();

        RequestProperties getDefaultRequestProperties();

        @Deprecated
        void setDefaultRequestProperty(String str, String str2);
    }

    public static class HttpDataSourceException extends IOException {
        public static final int TYPE_CLOSE = 3;
        public static final int TYPE_OPEN = 1;
        public static final int TYPE_READ = 2;
        public final DataSpec dataSpec;
        public final int type;

        @Documented
        @Retention(RetentionPolicy.SOURCE)
        public @interface Type {
        }

        public HttpDataSourceException(DataSpec dataSpec2, int type2) {
            this.dataSpec = dataSpec2;
            this.type = type2;
        }

        public HttpDataSourceException(String message, DataSpec dataSpec2, int type2) {
            super(message);
            this.dataSpec = dataSpec2;
            this.type = type2;
        }

        public HttpDataSourceException(IOException cause, DataSpec dataSpec2, int type2) {
            super(cause);
            this.dataSpec = dataSpec2;
            this.type = type2;
        }

        public HttpDataSourceException(String message, IOException cause, DataSpec dataSpec2, int type2) {
            super(message, cause);
            this.dataSpec = dataSpec2;
            this.type = type2;
        }
    }

    public static final class InvalidContentTypeException extends HttpDataSourceException {
        public final String contentType;

        public InvalidContentTypeException(String contentType2, DataSpec dataSpec) {
            StringBuilder sb = new StringBuilder();
            sb.append("Invalid content type: ");
            sb.append(contentType2);
            super(sb.toString(), dataSpec, 1);
            this.contentType = contentType2;
        }
    }

    public static final class InvalidResponseCodeException extends HttpDataSourceException {
        public final Map<String, List<String>> headerFields;
        public final int responseCode;
        @Nullable
        public final String responseMessage;

        @Deprecated
        public InvalidResponseCodeException(int responseCode2, Map<String, List<String>> headerFields2, DataSpec dataSpec) {
            this(responseCode2, null, headerFields2, dataSpec);
        }

        public InvalidResponseCodeException(int responseCode2, @Nullable String responseMessage2, Map<String, List<String>> headerFields2, DataSpec dataSpec) {
            StringBuilder sb = new StringBuilder();
            sb.append("Response code: ");
            sb.append(responseCode2);
            super(sb.toString(), dataSpec, 1);
            this.responseCode = responseCode2;
            this.responseMessage = responseMessage2;
            this.headerFields = headerFields2;
        }
    }

    public static final class RequestProperties {
        private final Map<String, String> requestProperties = new HashMap();
        private Map<String, String> requestPropertiesSnapshot;

        public synchronized void set(String name, String value) {
            this.requestPropertiesSnapshot = null;
            this.requestProperties.put(name, value);
        }

        public synchronized void set(Map<String, String> properties) {
            this.requestPropertiesSnapshot = null;
            this.requestProperties.putAll(properties);
        }

        public synchronized void clearAndSet(Map<String, String> properties) {
            this.requestPropertiesSnapshot = null;
            this.requestProperties.clear();
            this.requestProperties.putAll(properties);
        }

        public synchronized void remove(String name) {
            this.requestPropertiesSnapshot = null;
            this.requestProperties.remove(name);
        }

        public synchronized void clear() {
            this.requestPropertiesSnapshot = null;
            this.requestProperties.clear();
        }

        public synchronized Map<String, String> getSnapshot() {
            if (this.requestPropertiesSnapshot == null) {
                this.requestPropertiesSnapshot = Collections.unmodifiableMap(new HashMap(this.requestProperties));
            }
            return this.requestPropertiesSnapshot;
        }
    }

    void clearAllRequestProperties();

    void clearRequestProperty(String str);

    void close() throws HttpDataSourceException;

    Map<String, List<String>> getResponseHeaders();

    long open(DataSpec dataSpec) throws HttpDataSourceException;

    int read(byte[] bArr, int i, int i2) throws HttpDataSourceException;

    void setRequestProperty(String str, String str2);
}
