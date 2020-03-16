package info.guardianproject.netcipher.proxy;

import android.util.Log;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ProxySelector extends java.net.ProxySelector {
    private ArrayList<Proxy> listProxies = new ArrayList<>();

    public void addProxy(Type type, String host, int port) {
        this.listProxies.add(new Proxy(type, new InetSocketAddress(host, port)));
    }

    public void connectFailed(URI uri, SocketAddress address, IOException failure) {
        StringBuilder sb = new StringBuilder();
        sb.append("could not connect to ");
        sb.append(address.toString());
        sb.append(": ");
        sb.append(failure.getMessage());
        Log.w("ProxySelector", sb.toString());
    }

    public List<Proxy> select(URI uri) {
        return this.listProxies;
    }
}
