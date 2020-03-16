package info.guardianproject.netcipher.web;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Parcelable;
import android.util.ArrayMap;
import android.util.Log;
import android.webkit.WebView;
import info.guardianproject.netcipher.client.StrongHttpsClient;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.apache.http.HttpHost;

public class WebkitProxy {
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8118;
    private static final int DEFAULT_SOCKS_PORT = 9050;
    private static final int REQUEST_CODE = 0;
    private static final String TAG = "OrbotHelpher";

    public static boolean setProxy(String appClass, Context ctx, WebView wView, String host, int port) throws Exception {
        setSystemProperties(host, port);
        if (VERSION.SDK_INT < 13) {
            setProxyUpToHC(wView, host, port);
            return false;
        } else if (VERSION.SDK_INT < 19) {
            return setWebkitProxyICS(ctx, host, port);
        } else {
            if (VERSION.SDK_INT < 20) {
                boolean worked = setKitKatProxy(appClass, ctx, host, port);
                if (!worked) {
                    return setWebkitProxyICS(ctx, host, port);
                }
                return worked;
            } else if (VERSION.SDK_INT >= 21) {
                return setWebkitProxyLollipop(ctx, host, port);
            } else {
                return false;
            }
        }
    }

    private static void setSystemProperties(String host, int port) {
        System.setProperty("proxyHost", host);
        StringBuilder sb = new StringBuilder();
        sb.append(port);
        sb.append("");
        System.setProperty("proxyPort", sb.toString());
        System.setProperty("http.proxyHost", host);
        StringBuilder sb2 = new StringBuilder();
        sb2.append(port);
        sb2.append("");
        System.setProperty("http.proxyPort", sb2.toString());
        System.setProperty("https.proxyHost", host);
        StringBuilder sb3 = new StringBuilder();
        sb3.append(port);
        sb3.append("");
        System.setProperty("https.proxyPort", sb3.toString());
        System.setProperty("socks.proxyHost", host);
        System.setProperty("socks.proxyPort", "9050");
        System.setProperty("socksProxyHost", host);
        System.setProperty("socksProxyPort", "9050");
    }

    private static boolean setWebkitProxyGingerbread(Context ctx, String host, int port) throws Exception {
        Object requestQueueObject = getRequestQueue(ctx);
        if (requestQueueObject == null) {
            return false;
        }
        setDeclaredField(requestQueueObject, "mProxyHost", new HttpHost(host, port, StrongHttpsClient.TYPE_HTTP));
        return true;
    }

    private static boolean setProxyUpToHC(WebView webview, String host, int port) {
        Log.d(TAG, "Setting proxy with <= 3.2 API.");
        HttpHost proxyServer = new HttpHost(host, port);
        try {
            Class networkClass = Class.forName("android.webkit.Network");
            if (networkClass == null) {
                Log.e(TAG, "failed to get class for android.webkit.Network");
                return false;
            }
            Method getInstanceMethod = networkClass.getMethod("getInstance", new Class[]{Context.class});
            if (getInstanceMethod == null) {
                Log.e(TAG, "failed to get getInstance method");
            }
            Object network = getInstanceMethod.invoke(networkClass, new Object[]{webview.getContext()});
            if (network == null) {
                Log.e(TAG, "error getting network: network is null");
                return false;
            }
            try {
                Object requestQueue = getFieldValueSafely(networkClass.getDeclaredField("mRequestQueue"), network);
                if (requestQueue == null) {
                    Log.e(TAG, "Request queue is null");
                    return false;
                }
                try {
                    Field proxyHostField = Class.forName("android.net.http.RequestQueue").getDeclaredField("mProxyHost");
                    boolean temp = proxyHostField.isAccessible();
                    try {
                        proxyHostField.setAccessible(true);
                        proxyHostField.set(requestQueue, proxyServer);
                    } catch (Exception e) {
                        Log.e(TAG, "error setting proxy host");
                    } catch (Throwable th) {
                        proxyHostField.setAccessible(temp);
                        throw th;
                    }
                    proxyHostField.setAccessible(temp);
                    Log.d(TAG, "Setting proxy with <= 3.2 API successful!");
                    return true;
                } catch (Exception e2) {
                    Log.e(TAG, "error getting proxy host field");
                    return false;
                }
            } catch (Exception e3) {
                Log.e(TAG, "error getting field value");
                return false;
            }
        } catch (Exception ex) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("error getting network: ");
            sb.append(ex);
            Log.e(str, sb.toString());
            return false;
        }
    }

    private static Object getFieldValueSafely(Field field, Object classInstance) throws IllegalArgumentException, IllegalAccessException {
        boolean oldAccessibleValue = field.isAccessible();
        field.setAccessible(true);
        Object result = field.get(classInstance);
        field.setAccessible(oldAccessibleValue);
        return result;
    }

    private static boolean setWebkitProxyICS(Context ctx, String host, int port) {
        try {
            Class webViewCoreClass = Class.forName("android.webkit.WebViewCore");
            Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
            if (!(webViewCoreClass == null || proxyPropertiesClass == null)) {
                Method m = webViewCoreClass.getDeclaredMethod("sendStaticMessage", new Class[]{Integer.TYPE, Object.class});
                Constructor c = proxyPropertiesClass.getConstructor(new Class[]{String.class, Integer.TYPE, String.class});
                if (!(m == null || c == null)) {
                    m.setAccessible(true);
                    c.setAccessible(true);
                    m.invoke(null, new Object[]{Integer.valueOf(193), c.newInstance(new Object[]{host, Integer.valueOf(port), null})});
                    return true;
                }
            }
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Exception setting WebKit proxy through android.net.ProxyProperties: ");
            sb.append(e.toString());
            Log.e("ProxySettings", sb.toString());
        } catch (Error e2) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Exception setting WebKit proxy through android.webkit.Network: ");
            sb2.append(e2.toString());
            Log.e("ProxySettings", sb2.toString());
        }
        return false;
    }

    @TargetApi(19)
    public static boolean resetKitKatProxy(String appClass, Context appContext) {
        return setKitKatProxy(appClass, appContext, null, 0);
    }

    @TargetApi(19)
    private static boolean setKitKatProxy(String appClass, Context appContext, String host, int port) {
        Field loadedApkField;
        Class applictionCls;
        Context context = appContext;
        String str = host;
        int i = port;
        if (str != null) {
            System.setProperty("http.proxyHost", str);
            StringBuilder sb = new StringBuilder();
            sb.append(i);
            sb.append("");
            System.setProperty("http.proxyPort", sb.toString());
            System.setProperty("https.proxyHost", str);
            StringBuilder sb2 = new StringBuilder();
            sb2.append(i);
            sb2.append("");
            System.setProperty("https.proxyPort", sb2.toString());
        }
        try {
            Class applictionCls2 = Class.forName(appClass);
            Field loadedApkField2 = applictionCls2.getField("mLoadedApk");
            loadedApkField2.setAccessible(true);
            Object loadedApk = loadedApkField2.get(context);
            Field receiversField = Class.forName("android.app.LoadedApk").getDeclaredField("mReceivers");
            receiversField.setAccessible(true);
            for (ArrayMap keySet : ((ArrayMap) receiversField.get(loadedApk)).values()) {
                for (Object rec : keySet.keySet()) {
                    Class clazz = rec.getClass();
                    if (clazz.getName().contains("ProxyChangeListener")) {
                        applictionCls = applictionCls2;
                        Method onReceiveMethod = clazz.getDeclaredMethod("onReceive", new Class[]{Context.class, Intent.class});
                        Intent intent = new Intent("android.intent.action.PROXY_CHANGE");
                        if (str != null) {
                            String str2 = "android.net.ProxyProperties";
                            loadedApkField = loadedApkField2;
                            Class cls = Class.forName("android.net.ProxyProperties");
                            Constructor constructor = cls.getConstructor(new Class[]{String.class, Integer.TYPE, String.class});
                            Class cls2 = cls;
                            constructor.setAccessible(true);
                            Constructor constructor2 = constructor;
                            intent.putExtra("proxy", (Parcelable) constructor.newInstance(new Object[]{str, Integer.valueOf(port), null}));
                        } else {
                            loadedApkField = loadedApkField2;
                        }
                        onReceiveMethod.invoke(rec, new Object[]{context, intent});
                    } else {
                        applictionCls = applictionCls2;
                        loadedApkField = loadedApkField2;
                    }
                    applictionCls2 = applictionCls;
                    loadedApkField2 = loadedApkField;
                    str = host;
                    int i2 = port;
                }
                Field field = loadedApkField2;
                str = host;
                int i3 = port;
            }
            Field field2 = loadedApkField2;
            return true;
        } catch (ClassNotFoundException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionAsString = sw.toString();
            Log.v(TAG, e.getMessage());
            Log.v(TAG, exceptionAsString);
            return false;
        } catch (NoSuchFieldException e2) {
            StringWriter sw2 = new StringWriter();
            e2.printStackTrace(new PrintWriter(sw2));
            String exceptionAsString2 = sw2.toString();
            Log.v(TAG, e2.getMessage());
            Log.v(TAG, exceptionAsString2);
            return false;
        } catch (IllegalAccessException e3) {
            StringWriter sw3 = new StringWriter();
            e3.printStackTrace(new PrintWriter(sw3));
            String exceptionAsString3 = sw3.toString();
            Log.v(TAG, e3.getMessage());
            Log.v(TAG, exceptionAsString3);
            return false;
        } catch (IllegalArgumentException e4) {
            StringWriter sw4 = new StringWriter();
            e4.printStackTrace(new PrintWriter(sw4));
            String exceptionAsString4 = sw4.toString();
            Log.v(TAG, e4.getMessage());
            Log.v(TAG, exceptionAsString4);
            return false;
        } catch (NoSuchMethodException e5) {
            StringWriter sw5 = new StringWriter();
            e5.printStackTrace(new PrintWriter(sw5));
            String exceptionAsString5 = sw5.toString();
            Log.v(TAG, e5.getMessage());
            Log.v(TAG, exceptionAsString5);
            return false;
        } catch (InvocationTargetException e6) {
            StringWriter sw6 = new StringWriter();
            e6.printStackTrace(new PrintWriter(sw6));
            String exceptionAsString6 = sw6.toString();
            Log.v(TAG, e6.getMessage());
            Log.v(TAG, exceptionAsString6);
            return false;
        } catch (InstantiationException e7) {
            StringWriter sw7 = new StringWriter();
            e7.printStackTrace(new PrintWriter(sw7));
            String exceptionAsString7 = sw7.toString();
            Log.v(TAG, e7.getMessage());
            Log.v(TAG, exceptionAsString7);
            return false;
        }
    }

    @TargetApi(21)
    public static boolean resetLollipopProxy(String appClass, Context appContext) {
        return setWebkitProxyLollipop(appContext, null, 0);
    }

    @TargetApi(21)
    private static boolean setWebkitProxyLollipop(Context appContext, String host, int port) {
        Class applictionClass;
        Context context = appContext;
        String str = host;
        int i = port;
        System.setProperty("http.proxyHost", str);
        StringBuilder sb = new StringBuilder();
        sb.append(i);
        sb.append("");
        System.setProperty("http.proxyPort", sb.toString());
        System.setProperty("https.proxyHost", str);
        StringBuilder sb2 = new StringBuilder();
        sb2.append(i);
        sb2.append("");
        System.setProperty("https.proxyPort", sb2.toString());
        try {
            Class applictionClass2 = Class.forName("android.app.Application");
            Field mLoadedApkField = applictionClass2.getDeclaredField("mLoadedApk");
            mLoadedApkField.setAccessible(true);
            Object mloadedApk = mLoadedApkField.get(context);
            Field mReceiversField = Class.forName("android.app.LoadedApk").getDeclaredField("mReceivers");
            mReceiversField.setAccessible(true);
            for (ArrayMap keySet : ((ArrayMap) mReceiversField.get(mloadedApk)).values()) {
                for (Object receiver : keySet.keySet()) {
                    Class clazz = receiver.getClass();
                    if (clazz.getName().contains("ProxyChangeListener")) {
                        applictionClass = applictionClass2;
                        clazz.getDeclaredMethod("onReceive", new Class[]{Context.class, Intent.class}).invoke(receiver, new Object[]{context, new Intent("android.intent.action.PROXY_CHANGE")});
                    } else {
                        applictionClass = applictionClass2;
                    }
                    applictionClass2 = applictionClass;
                }
            }
            return true;
        } catch (ClassNotFoundException e) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("Exception setting WebKit proxy on Lollipop through ProxyChangeListener: ");
            sb3.append(e.toString());
            Log.d("ProxySettings", sb3.toString());
            return false;
        } catch (NoSuchFieldException e2) {
            StringBuilder sb4 = new StringBuilder();
            sb4.append("Exception setting WebKit proxy on Lollipop through ProxyChangeListener: ");
            sb4.append(e2.toString());
            Log.d("ProxySettings", sb4.toString());
            return false;
        } catch (IllegalAccessException e3) {
            StringBuilder sb5 = new StringBuilder();
            sb5.append("Exception setting WebKit proxy on Lollipop through ProxyChangeListener: ");
            sb5.append(e3.toString());
            Log.d("ProxySettings", sb5.toString());
            return false;
        } catch (NoSuchMethodException e4) {
            StringBuilder sb6 = new StringBuilder();
            sb6.append("Exception setting WebKit proxy on Lollipop through ProxyChangeListener: ");
            sb6.append(e4.toString());
            Log.d("ProxySettings", sb6.toString());
            return false;
        } catch (InvocationTargetException e5) {
            StringBuilder sb7 = new StringBuilder();
            sb7.append("Exception setting WebKit proxy on Lollipop through ProxyChangeListener: ");
            sb7.append(e5.toString());
            Log.d("ProxySettings", sb7.toString());
            return false;
        }
    }

    private static boolean sendProxyChangedIntent(Context ctx, String host, int port) {
        try {
            Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
            if (proxyPropertiesClass != null) {
                Constructor c = proxyPropertiesClass.getConstructor(new Class[]{String.class, Integer.TYPE, String.class});
                if (c != null) {
                    c.setAccessible(true);
                    Object properties = c.newInstance(new Object[]{host, Integer.valueOf(port), null});
                    Intent intent = new Intent("android.intent.action.PROXY_CHANGE");
                    intent.putExtra("proxy", (Parcelable) properties);
                    ctx.sendBroadcast(intent);
                }
            }
        } catch (Exception e) {
            Log.e("ProxySettings", "Exception sending Intent ", e);
        } catch (Error e2) {
            Log.e("ProxySettings", "Exception sending Intent ", e2);
        }
        return false;
    }

    public static void resetProxy(String appClass, Context ctx) throws Exception {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
        if (VERSION.SDK_INT < 14) {
            resetProxyForGingerBread(ctx);
        } else if (VERSION.SDK_INT < 19) {
            resetProxyForICS();
        } else {
            resetKitKatProxy(appClass, ctx);
        }
    }

    private static void resetProxyForICS() throws Exception {
        try {
            Class webViewCoreClass = Class.forName("android.webkit.WebViewCore");
            Class proxyPropertiesClass = Class.forName("android.net.ProxyProperties");
            if (webViewCoreClass != null && proxyPropertiesClass != null) {
                Method m = webViewCoreClass.getDeclaredMethod("sendStaticMessage", new Class[]{Integer.TYPE, Object.class});
                if (m != null) {
                    m.setAccessible(true);
                    m.invoke(null, new Object[]{Integer.valueOf(193), null});
                }
            }
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder();
            sb.append("Exception setting WebKit proxy through android.net.ProxyProperties: ");
            sb.append(e.toString());
            Log.e("ProxySettings", sb.toString());
            throw e;
        } catch (Error e2) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Exception setting WebKit proxy through android.webkit.Network: ");
            sb2.append(e2.toString());
            Log.e("ProxySettings", sb2.toString());
            throw e2;
        }
    }

    private static void resetProxyForGingerBread(Context ctx) throws Exception {
        Object requestQueueObject = getRequestQueue(ctx);
        if (requestQueueObject != null) {
            setDeclaredField(requestQueueObject, "mProxyHost", null);
        }
    }

    public static Object getRequestQueue(Context ctx) throws Exception {
        Class networkClass = Class.forName("android.webkit.Network");
        if (networkClass == null) {
            return null;
        }
        Object networkObj = invokeMethod(networkClass, "getInstance", new Object[]{ctx}, Context.class);
        if (networkObj != null) {
            return getDeclaredField(networkObj, "mRequestQueue");
        }
        return null;
    }

    private static Object getDeclaredField(Object obj, String name) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(obj);
    }

    private static void setDeclaredField(Object obj, String name, Object value) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static Object invokeMethod(Object object, String methodName, Object[] params, Class... types) throws Exception {
        Class c = object instanceof Class ? (Class) object : object.getClass();
        if (types != null) {
            return c.getMethod(methodName, types).invoke(object, params);
        }
        return c.getMethod(methodName, new Class[0]).invoke(object, new Object[0]);
    }

    public static Socket getSocket(Context context, String proxyHost, int proxyPort) throws IOException {
        Socket sock = new Socket();
        sock.connect(new InetSocketAddress(proxyHost, proxyPort), 10000);
        return sock;
    }

    public static Socket getSocket(Context context) throws IOException {
        return getSocket(context, DEFAULT_HOST, DEFAULT_SOCKS_PORT);
    }

    public static AlertDialog initOrbot(Activity activity, CharSequence stringTitle, CharSequence stringMessage, CharSequence stringButtonYes, CharSequence stringButtonNo, CharSequence stringDesiredBarcodeFormats) {
        Intent intentScan = new Intent(OrbotHelper.ACTION_START_TOR);
        intentScan.addCategory("android.intent.category.DEFAULT");
        try {
            activity.startActivityForResult(intentScan, 0);
            return null;
        } catch (ActivityNotFoundException e) {
            return showDownloadDialog(activity, stringTitle, stringMessage, stringButtonYes, stringButtonNo);
        }
    }

    private static AlertDialog showDownloadDialog(final Activity activity, CharSequence stringTitle, CharSequence stringMessage, CharSequence stringButtonYes, CharSequence stringButtonNo) {
        Builder downloadDialog = new Builder(activity);
        downloadDialog.setTitle(stringTitle);
        downloadDialog.setMessage(stringMessage);
        downloadDialog.setPositiveButton(stringButtonYes, new OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                activity.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://search?q=pname:org.torproject.android")));
            }
        });
        downloadDialog.setNegativeButton(stringButtonNo, new OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        return downloadDialog.show();
    }
}
