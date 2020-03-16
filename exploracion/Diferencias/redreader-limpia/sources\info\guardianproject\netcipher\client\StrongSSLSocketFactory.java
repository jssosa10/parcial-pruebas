package info.guardianproject.netcipher.client;

import android.content.Context;
import ch.boye.httpclientandroidlib.conn.scheme.LayeredSchemeSocketFactory;
import ch.boye.httpclientandroidlib.conn.ssl.SSLSocketFactory;
import ch.boye.httpclientandroidlib.params.HttpParams;
import java.io.IOException;
import java.net.Proxy;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;

public class StrongSSLSocketFactory extends SSLSocketFactory implements LayeredSchemeSocketFactory {
    public static final String SSL = "SSL";
    public static final String SSLV2 = "SSLv2";
    public static final String TLS = "TLS";
    private String[] mCipherSuites;
    private boolean mEnableStongerDefaultProtocalVersion = true;
    private boolean mEnableStongerDefaultSSLCipherSuite = true;
    private javax.net.ssl.SSLSocketFactory mFactory = null;
    private String[] mProtocols;
    private Proxy mProxy = null;

    public StrongSSLSocketFactory(Context context, TrustManager[] trustManagers, KeyStore keyStore, String keyStorePassword) throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        super(keyStore);
        SSLContext sslContext = SSLContext.getInstance(TLS);
        sslContext.init(createKeyManagers(keyStore, keyStorePassword), trustManagers, new SecureRandom());
        this.mFactory = sslContext.getSocketFactory();
    }

    private void readSSLParameters(SSLSocket sslSocket) {
        String[] arr$;
        String[] arr$2;
        List<String> protocolsToEnable = new ArrayList<>();
        List<String> supportedProtocols = Arrays.asList(sslSocket.getSupportedProtocols());
        for (String enabledProtocol : StrongConstants.ENABLED_PROTOCOLS) {
            if (supportedProtocols.contains(enabledProtocol)) {
                protocolsToEnable.add(enabledProtocol);
            }
        }
        this.mProtocols = (String[]) protocolsToEnable.toArray(new String[protocolsToEnable.size()]);
        List<String> cipherSuitesToEnable = new ArrayList<>();
        List<String> supportedCipherSuites = Arrays.asList(sslSocket.getSupportedCipherSuites());
        for (String enabledCipherSuite : StrongConstants.ENABLED_CIPHERS) {
            if (supportedCipherSuites.contains(enabledCipherSuite)) {
                cipherSuitesToEnable.add(enabledCipherSuite);
            }
        }
        this.mCipherSuites = (String[]) cipherSuitesToEnable.toArray(new String[cipherSuitesToEnable.size()]);
    }

    private KeyManager[] createKeyManagers(KeyStore keystore, String password) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        if (keystore != null) {
            KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmfactory.init(keystore, password != null ? password.toCharArray() : null);
            return kmfactory.getKeyManagers();
        }
        throw new IllegalArgumentException("Keystore may not be null");
    }

    public Socket createSocket() throws IOException {
        Socket newSocket = this.mFactory.createSocket();
        enableStrongerDefaults(newSocket);
        return newSocket;
    }

    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
        Socket newSocket = this.mFactory.createSocket(socket, host, port, autoClose);
        enableStrongerDefaults(newSocket);
        return newSocket;
    }

    private void enableStrongerDefaults(Socket socket) {
        if (isSecure(socket)) {
            SSLSocket sslSocket = (SSLSocket) socket;
            readSSLParameters(sslSocket);
            if (this.mEnableStongerDefaultProtocalVersion) {
                String[] strArr = this.mProtocols;
                if (strArr != null) {
                    sslSocket.setEnabledProtocols(strArr);
                }
            }
            if (this.mEnableStongerDefaultSSLCipherSuite) {
                String[] strArr2 = this.mCipherSuites;
                if (strArr2 != null) {
                    sslSocket.setEnabledCipherSuites(strArr2);
                }
            }
        }
    }

    public boolean isSecure(Socket sock) throws IllegalArgumentException {
        return sock instanceof SSLSocket;
    }

    public void setProxy(Proxy proxy) {
        this.mProxy = proxy;
    }

    public Proxy getProxy() {
        return this.mProxy;
    }

    public boolean isEnableStongerDefaultSSLCipherSuite() {
        return this.mEnableStongerDefaultSSLCipherSuite;
    }

    public void setEnableStongerDefaultSSLCipherSuite(boolean enable) {
        this.mEnableStongerDefaultSSLCipherSuite = enable;
    }

    public boolean isEnableStongerDefaultProtocalVersion() {
        return this.mEnableStongerDefaultProtocalVersion;
    }

    public void setEnableStongerDefaultProtocalVersion(boolean enable) {
        this.mEnableStongerDefaultProtocalVersion = enable;
    }

    public Socket createSocket(HttpParams httpParams) throws IOException {
        Socket newSocket = this.mFactory.createSocket();
        enableStrongerDefaults(newSocket);
        return newSocket;
    }

    public Socket createLayeredSocket(Socket arg0, String arg1, int arg2, boolean arg3) throws IOException, UnknownHostException {
        return this.mFactory.createLayeredSocket(arg0, arg1, arg2, arg3);
    }
}
