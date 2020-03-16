package info.guardianproject.netcipher.client;

import android.content.Context;
import android.util.Log;
import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.conn.ClientConnectionOperator;
import ch.boye.httpclientandroidlib.conn.scheme.PlainSocketFactory;
import ch.boye.httpclientandroidlib.conn.scheme.Scheme;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.impl.client.DefaultHttpClient;
import ch.boye.httpclientandroidlib.impl.conn.tsccm.ThreadSafeClientConnManager;
import info.guardianproject.onionkit.R;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import javax.net.ssl.TrustManagerFactory;
import org.quantumbadger.redreader.common.Constants.Reddit;

public class StrongHttpsClient extends DefaultHttpClient {
    private static final String TRUSTSTORE_PASSWORD = "changeit";
    private static final String TRUSTSTORE_TYPE = "BKS";
    public static final String TYPE_HTTP = "http";
    public static final String TYPE_SOCKS = "socks";
    final Context context;
    private SchemeRegistry mRegistry = new SchemeRegistry();
    /* access modifiers changed from: private */
    public HttpHost proxyHost;
    private String proxyType;
    private StrongSSLSocketFactory sFactory;

    public StrongHttpsClient(Context context2) {
        this.context = context2;
        this.mRegistry.register(new Scheme(TYPE_HTTP, 80, PlainSocketFactory.getSocketFactory()));
        try {
            KeyStore keyStore = loadKeyStore();
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            this.sFactory = new StrongSSLSocketFactory(context2, trustManagerFactory.getTrustManagers(), keyStore, TRUSTSTORE_PASSWORD);
            this.mRegistry.register(new Scheme(Reddit.SCHEME_HTTPS, 443, this.sFactory));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private KeyStore loadKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        KeyStore trustStore = KeyStore.getInstance(TRUSTSTORE_TYPE);
        trustStore.load(this.context.getResources().openRawResource(R.raw.debiancacerts), TRUSTSTORE_PASSWORD.toCharArray());
        return trustStore;
    }

    public StrongHttpsClient(Context context2, KeyStore keystore) {
        this.context = context2;
        this.mRegistry.register(new Scheme(TYPE_HTTP, 80, PlainSocketFactory.getSocketFactory()));
        try {
            this.sFactory = new StrongSSLSocketFactory(context2, TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).getTrustManagers(), keystore, TRUSTSTORE_PASSWORD);
            this.mRegistry.register(new Scheme(Reddit.SCHEME_HTTPS, 443, this.sFactory));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /* access modifiers changed from: protected */
    public ThreadSafeClientConnManager createClientConnectionManager() {
        if (this.proxyHost == null && this.proxyType == null) {
            Log.d("StrongHTTPS", "not proxying");
            return new MyThreadSafeClientConnManager(getParams(), this.mRegistry);
        } else if (this.proxyHost == null || !this.proxyType.equalsIgnoreCase(TYPE_SOCKS)) {
            StringBuilder sb = new StringBuilder();
            sb.append("proxying with: ");
            sb.append(this.proxyType);
            Log.d("StrongHTTPS", sb.toString());
            return new MyThreadSafeClientConnManager(getParams(), this.mRegistry);
        } else {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("proxying using: ");
            sb2.append(this.proxyType);
            Log.d("StrongHTTPS", sb2.toString());
            return new MyThreadSafeClientConnManager(getParams(), this.mRegistry) {
                /* access modifiers changed from: protected */
                public ClientConnectionOperator createConnectionOperator(SchemeRegistry schreg) {
                    return new SocksProxyClientConnOperator(schreg, StrongHttpsClient.this.proxyHost.getHostName(), StrongHttpsClient.this.proxyHost.getPort());
                }
            };
        }
    }

    public void useProxy(boolean enableTor, String type, String host, int port) {
        if (enableTor) {
            this.proxyType = type;
            if (type.equalsIgnoreCase(TYPE_SOCKS)) {
                this.proxyHost = new HttpHost(host, port);
                return;
            }
            this.proxyHost = new HttpHost(host, port, type);
            getParams().setParameter("http.route.default-proxy", this.proxyHost);
            return;
        }
        getParams().removeParameter("http.route.default-proxy");
        this.proxyHost = null;
    }

    public void disableProxy() {
        getParams().removeParameter("http.route.default-proxy");
        this.proxyHost = null;
    }
}
