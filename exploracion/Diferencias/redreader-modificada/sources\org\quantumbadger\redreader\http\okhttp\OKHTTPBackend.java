package org.quantumbadger.redreader.http.okhttp;

import android.content.Context;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.ConnectionPool;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.quantumbadger.redreader.common.Constants;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.TorCommon;
import org.quantumbadger.redreader.http.HTTPBackend;
import org.quantumbadger.redreader.http.HTTPBackend.Listener;
import org.quantumbadger.redreader.http.HTTPBackend.PostField;
import org.quantumbadger.redreader.http.HTTPBackend.Request;
import org.quantumbadger.redreader.http.HTTPBackend.RequestDetails;

public class OKHTTPBackend extends HTTPBackend {
    private static HTTPBackend httpBackend;
    /* access modifiers changed from: private */
    public final OkHttpClient mClient;

    private OKHTTPBackend() {
        Builder builder = new Builder();
        final List<Cookie> list = new ArrayList<>();
        Cookie.Builder cookieBuilder = new Cookie.Builder();
        cookieBuilder.domain(Reddit.DOMAIN_HTTPS_HUMAN);
        cookieBuilder.name("over18");
        cookieBuilder.value("1");
        cookieBuilder.path("/");
        list.add(cookieBuilder.build());
        builder.cookieJar(new CookieJar() {
            public void saveFromResponse(HttpUrl url, List<Cookie> list) {
            }

            public List<Cookie> loadForRequest(HttpUrl url) {
                if (url.toString().contains("search")) {
                    return list;
                }
                return Collections.emptyList();
            }
        });
        if (TorCommon.isTorEnabled()) {
            builder.proxy(new Proxy(Type.HTTP, new InetSocketAddress("127.0.0.1", 8118)));
        }
        builder.followRedirects(true);
        builder.followSslRedirects(true);
        builder.connectTimeout(15000, TimeUnit.SECONDS);
        builder.readTimeout(10000, TimeUnit.SECONDS);
        builder.connectionPool(new ConnectionPool(1, 5, TimeUnit.SECONDS));
        builder.retryOnConnectionFailure(true);
        this.mClient = builder.build();
    }

    public static synchronized HTTPBackend getHttpBackend() {
        HTTPBackend hTTPBackend;
        synchronized (OKHTTPBackend.class) {
            if (httpBackend == null) {
                httpBackend = new OKHTTPBackend();
            }
            hTTPBackend = httpBackend;
        }
        return hTTPBackend;
    }

    public synchronized void recreateHttpBackend() {
        httpBackend = new OKHTTPBackend();
    }

    public Request prepareRequest(Context context, RequestDetails details) {
        final okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        builder.header("User-Agent", Constants.ua(context));
        List<PostField> postFields = details.getPostFields();
        if (postFields != null) {
            builder.post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), PostField.encodeList(postFields)));
        } else {
            builder.get();
        }
        builder.url(details.getUrl().toString());
        builder.cacheControl(CacheControl.FORCE_NETWORK);
        final AtomicReference<Call> callRef = new AtomicReference<>();
        return new Request() {
            public void executeInThisThread(Listener listener) {
                Long bodyBytes;
                InputStream bodyStream;
                Call call = OKHTTPBackend.this.mClient.newCall(builder.build());
                StringBuilder sb = new StringBuilder();
                sb.append("calling: ");
                sb.append(call.request().url());
                Log.d("OK", sb.toString());
                callRef.set(call);
                try {
                    Response response = call.execute();
                    int status = response.code();
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("request got status: ");
                    sb2.append(status);
                    Log.d("OK", sb2.toString());
                    if (status == 200 || status == 202) {
                        ResponseBody body = response.body();
                        if (body != null) {
                            bodyStream = body.byteStream();
                            bodyBytes = Long.valueOf(body.contentLength());
                        } else {
                            bodyStream = null;
                            bodyBytes = null;
                        }
                        listener.onSuccess(response.header("Content-Type"), bodyBytes, bodyStream);
                    } else {
                        listener.onError(1, null, Integer.valueOf(status));
                    }
                } catch (IOException e) {
                    listener.onError(0, e, null);
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("request didn't even connect: ");
                    sb3.append(e.getMessage());
                    Log.d("OK", sb3.toString());
                } catch (Throwable t) {
                    listener.onError(0, t, null);
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append("request didn't even connect: ");
                    sb4.append(t.getMessage());
                    Log.d("OK", sb4.toString());
                }
            }

            public void cancel() {
                Call call = (Call) callRef.getAndSet(null);
                if (call != null) {
                    call.cancel();
                }
            }

            public void addHeader(String name, String value) {
                builder.addHeader(name, value);
            }
        };
    }
}
