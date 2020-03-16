package info.guardianproject.netcipher;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import info.guardianproject.netcipher.client.TlsOnlySocketFactory;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.quantumbadger.redreader.common.Constants.Reddit;

public class NetCipher {
    public static final Proxy ORBOT_HTTP_PROXY = new Proxy(Type.HTTP, new InetSocketAddress("127.0.0.1", 8118));
    private static final String TAG = "NetCipher";
    private static Proxy proxy;

    private NetCipher() {
    }

    public static void setProxy(String host, int port) {
        if (!TextUtils.isEmpty(host) && port > 0) {
            setProxy(new Proxy(Type.HTTP, new InetSocketAddress(host, port)));
        } else if (proxy != ORBOT_HTTP_PROXY) {
            setProxy(null);
        }
    }

    public static void setProxy(Proxy proxy2) {
        if (proxy2 == null || proxy != ORBOT_HTTP_PROXY) {
            proxy = proxy2;
        } else {
            Log.w(TAG, "useTor is enabled, ignoring new proxy settings!");
        }
    }

    public static Proxy getProxy() {
        return proxy;
    }

    public static void clearProxy() {
        setProxy(null);
    }

    public static void useTor() {
        setProxy(ORBOT_HTTP_PROXY);
    }

    public static HttpURLConnection getHttpURLConnection(URL url, boolean compatible) throws IOException {
        HttpURLConnection connection;
        Proxy proxy2 = proxy;
        if (OrbotHelper.isOnionAddress(url)) {
            proxy2 = ORBOT_HTTP_PROXY;
        }
        if (proxy2 != null) {
            connection = (HttpURLConnection) url.openConnection(proxy2);
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
        if (connection instanceof HttpsURLConnection) {
            try {
                SSLContext sslcontext = SSLContext.getInstance("TLSv1");
                sslcontext.init(null, null, null);
                ((HttpsURLConnection) connection).setSSLSocketFactory(new TlsOnlySocketFactory(sslcontext.getSocketFactory(), compatible));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalArgumentException(e);
            } catch (KeyManagementException e2) {
                throw new IllegalArgumentException(e2);
            }
        }
        return connection;
    }

    public static HttpsURLConnection getHttpsURLConnection(String urlString) throws IOException {
        return getHttpsURLConnection(new URL(urlString.replaceFirst("^[Hh][Tt][Tt][Pp]:", "https:")), false);
    }

    public static HttpsURLConnection getHttpsURLConnection(Uri uri) throws IOException {
        return getHttpsURLConnection(uri.toString());
    }

    public static HttpsURLConnection getHttpsURLConnection(URI uri) throws IOException {
        if (TextUtils.equals(uri.getScheme(), Reddit.SCHEME_HTTPS)) {
            return getHttpsURLConnection(uri.toURL(), false);
        }
        return getHttpsURLConnection(uri.toString());
    }

    public static HttpsURLConnection getHttpsURLConnection(URL url) throws IOException {
        return getHttpsURLConnection(url, false);
    }

    public static HttpsURLConnection getCompatibleHttpsURLConnection(URL url) throws IOException {
        return getHttpsURLConnection(url, true);
    }

    public static HttpsURLConnection getHttpsURLConnection(URL url, boolean compatible) throws IOException {
        HttpURLConnection connection = getHttpURLConnection(url, compatible);
        if (connection instanceof HttpsURLConnection) {
            return (HttpsURLConnection) connection;
        }
        throw new IllegalArgumentException("not an HTTPS connection!");
    }

    public static HttpURLConnection getCompatibleHttpURLConnection(URL url) throws IOException {
        return getHttpURLConnection(url, true);
    }

    public static HttpURLConnection getHttpURLConnection(String urlString) throws IOException {
        return getHttpURLConnection(new URL(urlString));
    }

    public static HttpURLConnection getHttpURLConnection(Uri uri) throws IOException {
        return getHttpURLConnection(uri.toString());
    }

    public static HttpURLConnection getHttpURLConnection(URI uri) throws IOException {
        return getHttpURLConnection(uri.toURL());
    }

    public static HttpURLConnection getHttpURLConnection(URL url) throws IOException {
        return getHttpURLConnection(url, false);
    }
}
