package okhttp3.internal.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.HttpRetryException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketTimeoutException;
import java.security.cert.CertificateException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;
import okhttp3.Address;
import okhttp3.Call;
import okhttp3.CertificatePinner;
import okhttp3.EventListener;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Interceptor.Chain;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.internal.Util;
import okhttp3.internal.connection.RouteException;
import okhttp3.internal.connection.StreamAllocation;
import okhttp3.internal.http2.ConnectionShutdownException;
import org.quantumbadger.redreader.common.Constants.FileType;

public final class RetryAndFollowUpInterceptor implements Interceptor {
    private static final int MAX_FOLLOW_UPS = 20;
    private Object callStackTrace;
    private volatile boolean canceled;
    private final OkHttpClient client;
    private final boolean forWebSocket;
    private volatile StreamAllocation streamAllocation;

    public RetryAndFollowUpInterceptor(OkHttpClient client2, boolean forWebSocket2) {
        this.client = client2;
        this.forWebSocket = forWebSocket2;
    }

    public void cancel() {
        this.canceled = true;
        StreamAllocation streamAllocation2 = this.streamAllocation;
        if (streamAllocation2 != null) {
            streamAllocation2.cancel();
        }
    }

    public boolean isCanceled() {
        return this.canceled;
    }

    public void setCallStackTrace(Object callStackTrace2) {
        this.callStackTrace = callStackTrace2;
    }

    public StreamAllocation streamAllocation() {
        return this.streamAllocation;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:51:0x013c, code lost:
        if (0 != 0) goto L_0x013e;
     */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x0150  */
    public Response intercept(Chain chain) throws IOException {
        RealInterceptorChain realChain;
        RealInterceptorChain realChain2;
        int followUpCount;
        Request request = chain.request();
        RealInterceptorChain realChain3 = (RealInterceptorChain) chain;
        Call call = realChain3.call();
        EventListener eventListener = realChain3.eventListener();
        StreamAllocation streamAllocation2 = new StreamAllocation(this.client.connectionPool(), createAddress(request.url()), call, eventListener, this.callStackTrace);
        this.streamAllocation = streamAllocation2;
        int followUpCount2 = 0;
        Request request2 = request;
        Response priorResponse = null;
        while (!this.canceled) {
            boolean z = false;
            try {
                Response response = realChain3.proceed(request2, streamAllocation2, null, null);
                if (0 != 0) {
                    streamAllocation2.streamFailed(null);
                    streamAllocation2.release();
                }
                if (priorResponse != null) {
                    response = response.newBuilder().priorResponse(priorResponse.newBuilder().body(null).build()).build();
                }
                Request followUp = followUpRequest(response, streamAllocation2.route());
                if (followUp == null) {
                    if (!this.forWebSocket) {
                        streamAllocation2.release();
                    }
                    return response;
                }
                Util.closeQuietly((Closeable) response.body());
                int followUpCount3 = followUpCount2 + 1;
                if (followUpCount3 > 20) {
                    int followUpCount4 = followUpCount3;
                    streamAllocation2.release();
                    StringBuilder sb = new StringBuilder();
                    sb.append("Too many follow-up requests: ");
                    sb.append(followUpCount4);
                    throw new ProtocolException(sb.toString());
                } else if (!(followUp.body() instanceof UnrepeatableRequestBody)) {
                    if (!sameConnection(response, followUp.url())) {
                        streamAllocation2.release();
                        realChain2 = realChain3;
                        followUpCount = followUpCount3;
                        StreamAllocation streamAllocation3 = new StreamAllocation(this.client.connectionPool(), createAddress(followUp.url()), call, eventListener, this.callStackTrace);
                        this.streamAllocation = streamAllocation3;
                        streamAllocation2 = streamAllocation3;
                    } else {
                        realChain2 = realChain3;
                        followUpCount = followUpCount3;
                        if (streamAllocation2.codec() != null) {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("Closing the body of ");
                            sb2.append(response);
                            sb2.append(" didn't close its backing stream. Bad interceptor?");
                            throw new IllegalStateException(sb2.toString());
                        }
                    }
                    request2 = followUp;
                    priorResponse = response;
                    followUpCount2 = followUpCount;
                    realChain3 = realChain2;
                } else {
                    int i = followUpCount3;
                    streamAllocation2.release();
                    throw new HttpRetryException("Cannot retry streamed HTTP body", response.code());
                }
            } catch (RouteException e) {
                realChain = realChain3;
                RouteException e2 = e;
                if (!recover(e2.getLastConnectException(), streamAllocation2, false, request2)) {
                    throw e2.getLastConnectException();
                }
            } catch (IOException e3) {
                realChain = realChain3;
                IOException e4 = e3;
                if (!(e4 instanceof ConnectionShutdownException)) {
                    z = true;
                }
                if (recover(e4, streamAllocation2, z, request2)) {
                    if (0 != 0) {
                        streamAllocation2.streamFailed(null);
                        streamAllocation2.release();
                    }
                    realChain3 = realChain;
                } else {
                    throw e4;
                }
            } catch (Throwable th) {
                e = th;
                if (1 != 0) {
                }
                throw e;
            }
        }
        streamAllocation2.release();
        throw new IOException("Canceled");
    }

    private Address createAddress(HttpUrl url) {
        SSLSocketFactory sslSocketFactory = null;
        HostnameVerifier hostnameVerifier = null;
        CertificatePinner certificatePinner = null;
        if (url.isHttps()) {
            sslSocketFactory = this.client.sslSocketFactory();
            hostnameVerifier = this.client.hostnameVerifier();
            certificatePinner = this.client.certificatePinner();
        }
        Address address = new Address(url.host(), url.port(), this.client.dns(), this.client.socketFactory(), sslSocketFactory, hostnameVerifier, certificatePinner, this.client.proxyAuthenticator(), this.client.proxy(), this.client.protocols(), this.client.connectionSpecs(), this.client.proxySelector());
        return address;
    }

    private boolean recover(IOException e, StreamAllocation streamAllocation2, boolean requestSendStarted, Request userRequest) {
        streamAllocation2.streamFailed(e);
        if (!this.client.retryOnConnectionFailure()) {
            return false;
        }
        if ((!requestSendStarted || !(userRequest.body() instanceof UnrepeatableRequestBody)) && isRecoverable(e, requestSendStarted) && streamAllocation2.hasMoreRoutes()) {
            return true;
        }
        return false;
    }

    private boolean isRecoverable(IOException e, boolean requestSendStarted) {
        boolean z = false;
        if (e instanceof ProtocolException) {
            return false;
        }
        if (e instanceof InterruptedIOException) {
            if ((e instanceof SocketTimeoutException) && !requestSendStarted) {
                z = true;
            }
            return z;
        } else if ((!(e instanceof SSLHandshakeException) || !(e.getCause() instanceof CertificateException)) && !(e instanceof SSLPeerUnverifiedException)) {
            return true;
        } else {
            return false;
        }
    }

    private Request followUpRequest(Response userResponse, Route route) throws IOException {
        Proxy selectedProxy;
        if (userResponse != null) {
            int responseCode = userResponse.code();
            String method = userResponse.request().method();
            RequestBody requestBody = null;
            switch (responseCode) {
                case FileType.IMAGE_INFO /*300*/:
                case 301:
                case 302:
                case 303:
                    break;
                case StatusLine.HTTP_TEMP_REDIRECT /*307*/:
                case StatusLine.HTTP_PERM_REDIRECT /*308*/:
                    if (!method.equals("GET") && !method.equals("HEAD")) {
                        return null;
                    }
                case 401:
                    return this.client.authenticator().authenticate(route, userResponse);
                case 407:
                    if (route != null) {
                        selectedProxy = route.proxy();
                    } else {
                        selectedProxy = this.client.proxy();
                    }
                    if (selectedProxy.type() == Type.HTTP) {
                        return this.client.proxyAuthenticator().authenticate(route, userResponse);
                    }
                    throw new ProtocolException("Received HTTP_PROXY_AUTH (407) code while not using proxy");
                case 408:
                    if (!this.client.retryOnConnectionFailure() || (userResponse.request().body() instanceof UnrepeatableRequestBody)) {
                        return null;
                    }
                    if ((userResponse.priorResponse() == null || userResponse.priorResponse().code() != 408) && retryAfter(userResponse, 0) <= 0) {
                        return userResponse.request();
                    }
                    return null;
                case 503:
                    if ((userResponse.priorResponse() == null || userResponse.priorResponse().code() != 503) && retryAfter(userResponse, Integer.MAX_VALUE) == 0) {
                        return userResponse.request();
                    }
                    return null;
                default:
                    return null;
            }
            if (!this.client.followRedirects()) {
                return null;
            }
            String location = userResponse.header("Location");
            if (location == null) {
                return null;
            }
            HttpUrl url = userResponse.request().url().resolve(location);
            if (url == null) {
                return null;
            }
            if (!url.scheme().equals(userResponse.request().url().scheme()) && !this.client.followSslRedirects()) {
                return null;
            }
            Builder requestBuilder = userResponse.request().newBuilder();
            if (HttpMethod.permitsRequestBody(method)) {
                boolean maintainBody = HttpMethod.redirectsWithBody(method);
                if (HttpMethod.redirectsToGet(method)) {
                    requestBuilder.method("GET", null);
                } else {
                    if (maintainBody) {
                        requestBody = userResponse.request().body();
                    }
                    requestBuilder.method(method, requestBody);
                }
                if (!maintainBody) {
                    requestBuilder.removeHeader("Transfer-Encoding");
                    requestBuilder.removeHeader("Content-Length");
                    requestBuilder.removeHeader("Content-Type");
                }
            }
            if (!sameConnection(userResponse, url)) {
                requestBuilder.removeHeader("Authorization");
            }
            return requestBuilder.url(url).build();
        }
        throw new IllegalStateException();
    }

    private int retryAfter(Response userResponse, int defaultDelay) {
        String header = userResponse.header("Retry-After");
        if (header == null) {
            return defaultDelay;
        }
        if (header.matches("\\d+")) {
            return Integer.valueOf(header).intValue();
        }
        return Integer.MAX_VALUE;
    }

    private boolean sameConnection(Response response, HttpUrl followUp) {
        HttpUrl url = response.request().url();
        return url.host().equals(followUp.host()) && url.port() == followUp.port() && url.scheme().equals(followUp.scheme());
    }
}
