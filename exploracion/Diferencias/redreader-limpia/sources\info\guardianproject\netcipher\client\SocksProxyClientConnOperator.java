package info.guardianproject.netcipher.client;

import ch.boye.httpclientandroidlib.HttpHost;
import ch.boye.httpclientandroidlib.conn.OperatedClientConnection;
import ch.boye.httpclientandroidlib.conn.scheme.Scheme;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeRegistry;
import ch.boye.httpclientandroidlib.conn.scheme.SchemeSocketFactory;
import ch.boye.httpclientandroidlib.conn.ssl.SSLSocketFactory;
import ch.boye.httpclientandroidlib.impl.conn.DefaultClientConnectionOperator;
import ch.boye.httpclientandroidlib.params.HttpParams;
import ch.boye.httpclientandroidlib.protocol.HttpContext;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import org.joda.time.DateTimeConstants;

public class SocksProxyClientConnOperator extends DefaultClientConnectionOperator {
    private static final int CONNECT_TIMEOUT_MILLISECONDS = 60000;
    private static final int READ_TIMEOUT_MILLISECONDS = 60000;
    private String mProxyHost;
    private int mProxyPort;

    public SocksProxyClientConnOperator(SchemeRegistry registry, String proxyHost, int proxyPort) {
        super(registry);
        this.mProxyHost = proxyHost;
        this.mProxyPort = proxyPort;
    }

    public void openConnection(OperatedClientConnection conn, HttpHost target, InetAddress local, HttpContext context, HttpParams params) throws IOException {
        OperatedClientConnection operatedClientConnection = conn;
        HttpHost httpHost = target;
        HttpContext httpContext = context;
        HttpParams httpParams = params;
        Socket socket = null;
        Socket sslSocket = null;
        if (operatedClientConnection == null || httpHost == null || httpParams == null) {
            throw new IllegalArgumentException("Required argument may not be null");
        }
        try {
            if (!conn.isOpen()) {
                Scheme scheme = this.schemeRegistry.getScheme(target.getSchemeName());
                SchemeSocketFactory schemeSocketFactory = scheme.getSchemeSocketFactory();
                int port = scheme.resolvePort(target.getPort());
                String host = target.getHostName();
                Socket socket2 = new Socket();
                operatedClientConnection.opening(socket2, httpHost);
                socket2.setSoTimeout(DateTimeConstants.MILLIS_PER_MINUTE);
                socket2.connect(new InetSocketAddress(this.mProxyHost, this.mProxyPort), DateTimeConstants.MILLIS_PER_MINUTE);
                DataOutputStream outputStream = new DataOutputStream(socket2.getOutputStream());
                outputStream.write(4);
                outputStream.write(1);
                outputStream.writeShort((short) port);
                outputStream.writeInt(1);
                outputStream.write(0);
                outputStream.write(host.getBytes());
                outputStream.write(0);
                DataInputStream inputStream = new DataInputStream(socket2.getInputStream());
                if (inputStream.readByte() == 0 && inputStream.readByte() == 90) {
                    inputStream.readShort();
                    inputStream.readInt();
                    if (schemeSocketFactory instanceof SSLSocketFactory) {
                        Socket sslSocket2 = ((SSLSocketFactory) schemeSocketFactory).createLayeredSocket(socket2, host, port, httpParams);
                        operatedClientConnection.opening(sslSocket2, httpHost);
                        sslSocket2.setSoTimeout(DateTimeConstants.MILLIS_PER_MINUTE);
                        prepareSocket(sslSocket2, httpContext, httpParams);
                        operatedClientConnection.openCompleted(schemeSocketFactory.isSecure(sslSocket2), httpParams);
                        return;
                    }
                    operatedClientConnection.opening(socket2, httpHost);
                    socket2.setSoTimeout(DateTimeConstants.MILLIS_PER_MINUTE);
                    prepareSocket(socket2, httpContext, httpParams);
                    operatedClientConnection.openCompleted(schemeSocketFactory.isSecure(socket2), httpParams);
                    return;
                }
                throw new IOException("SOCKS4a connect failed");
            }
            throw new IllegalStateException("Connection must not be open");
        } catch (IOException e) {
            IOException e2 = e;
            if (sslSocket != null) {
                try {
                    sslSocket.close();
                } catch (IOException e3) {
                    throw e2;
                }
            }
            if (socket != null) {
                socket.close();
            }
            throw e2;
        }
    }

    public void updateSecureConnection(OperatedClientConnection conn, HttpHost target, HttpContext context, HttpParams params) throws IOException {
        throw new RuntimeException("operation not supported");
    }

    /* access modifiers changed from: protected */
    public InetAddress[] resolveHostname(String host) throws UnknownHostException {
        throw new RuntimeException("operation not supported");
    }
}
