package org.quantumbadger.redreader.http;

import android.content.Context;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import org.quantumbadger.redreader.http.okhttp.OKHTTPBackend;

public abstract class HTTPBackend {

    public interface Listener {
        void onError(int i, Throwable th, Integer num);

        void onSuccess(String str, Long l, InputStream inputStream);
    }

    public static class PostField {
        public final String name;
        public final String value;

        public PostField(String name2, String value2) {
            this.name = name2;
            this.value = value2;
        }

        public String encode() {
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(URLEncoder.encode(this.name, "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(this.value, "UTF-8"));
                return sb.toString();
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        public static String encodeList(List<PostField> fields) {
            StringBuilder result = new StringBuilder();
            for (PostField field : fields) {
                if (result.length() > 0) {
                    result.append('&');
                }
                result.append(field.encode());
            }
            return result.toString();
        }
    }

    public interface Request {
        void addHeader(String str, String str2);

        void cancel();

        void executeInThisThread(Listener listener);
    }

    public static class RequestDetails {
        private final List<PostField> mPostFields;
        private final URI mUrl;

        public RequestDetails(URI url, List<PostField> postFields) {
            this.mUrl = url;
            this.mPostFields = postFields;
        }

        public URI getUrl() {
            return this.mUrl;
        }

        public List<PostField> getPostFields() {
            return this.mPostFields;
        }
    }

    public abstract Request prepareRequest(Context context, RequestDetails requestDetails);

    public abstract void recreateHttpBackend();

    public static HTTPBackend getBackend() {
        return OKHTTPBackend.getHttpBackend();
    }
}
