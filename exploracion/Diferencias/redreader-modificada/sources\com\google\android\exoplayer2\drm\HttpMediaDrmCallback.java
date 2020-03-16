package com.google.android.exoplayer2.drm;

import android.annotation.TargetApi;
import android.net.Uri;
import android.text.TextUtils;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.drm.ExoMediaDrm.KeyRequest;
import com.google.android.exoplayer2.drm.ExoMediaDrm.ProvisionRequest;
import com.google.android.exoplayer2.upstream.DataSourceInputStream;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource.Factory;
import com.google.android.exoplayer2.upstream.HttpDataSource.InvalidResponseCodeException;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

@TargetApi(18)
public final class HttpMediaDrmCallback implements MediaDrmCallback {
    private static final int MAX_MANUAL_REDIRECTS = 5;
    private final Factory dataSourceFactory;
    private final String defaultLicenseUrl;
    private final boolean forceDefaultLicenseUrl;
    private final Map<String, String> keyRequestProperties;

    public HttpMediaDrmCallback(String defaultLicenseUrl2, Factory dataSourceFactory2) {
        this(defaultLicenseUrl2, false, dataSourceFactory2);
    }

    public HttpMediaDrmCallback(String defaultLicenseUrl2, boolean forceDefaultLicenseUrl2, Factory dataSourceFactory2) {
        this.dataSourceFactory = dataSourceFactory2;
        this.defaultLicenseUrl = defaultLicenseUrl2;
        this.forceDefaultLicenseUrl = forceDefaultLicenseUrl2;
        this.keyRequestProperties = new HashMap();
    }

    public void setKeyRequestProperty(String name, String value) {
        Assertions.checkNotNull(name);
        Assertions.checkNotNull(value);
        synchronized (this.keyRequestProperties) {
            this.keyRequestProperties.put(name, value);
        }
    }

    public void clearKeyRequestProperty(String name) {
        Assertions.checkNotNull(name);
        synchronized (this.keyRequestProperties) {
            this.keyRequestProperties.remove(name);
        }
    }

    public void clearAllKeyRequestProperties() {
        synchronized (this.keyRequestProperties) {
            this.keyRequestProperties.clear();
        }
    }

    public byte[] executeProvisionRequest(UUID uuid, ProvisionRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(request.getDefaultUrl());
        sb.append("&signedRequest=");
        sb.append(Util.fromUtf8Bytes(request.getData()));
        return executePost(this.dataSourceFactory, sb.toString(), Util.EMPTY_BYTE_ARRAY, null);
    }

    public byte[] executeKeyRequest(UUID uuid, KeyRequest request) throws Exception {
        String url = request.getLicenseServerUrl();
        if (this.forceDefaultLicenseUrl || TextUtils.isEmpty(url)) {
            url = this.defaultLicenseUrl;
        }
        Map<String, String> requestProperties = new HashMap<>();
        String contentType = C.PLAYREADY_UUID.equals(uuid) ? "text/xml" : C.CLEARKEY_UUID.equals(uuid) ? "application/json" : "application/octet-stream";
        requestProperties.put("Content-Type", contentType);
        if (C.PLAYREADY_UUID.equals(uuid)) {
            requestProperties.put("SOAPAction", "http://schemas.microsoft.com/DRM/2007/03/protocols/AcquireLicense");
        }
        synchronized (this.keyRequestProperties) {
            requestProperties.putAll(this.keyRequestProperties);
        }
        return executePost(this.dataSourceFactory, url, request.getData(), requestProperties);
    }

    /* JADX WARNING: Removed duplicated region for block: B:27:0x006e A[SYNTHETIC, Splitter:B:27:0x006e] */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0076  */
    /* JADX WARNING: Removed duplicated region for block: B:34:0x007a A[LOOP:1: B:7:0x002e->B:34:0x007a, LOOP_END] */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0081 A[EDGE_INSN: B:35:0x0081->B:36:? ?: BREAK  , SYNTHETIC, Splitter:B:35:0x0081] */
    private static byte[] executePost(Factory dataSourceFactory2, String url, byte[] data, Map<String, String> requestProperties) throws IOException {
        DataSourceInputStream inputStream;
        int manualRedirectCount;
        int manualRedirectCount2;
        HttpDataSource dataSource = dataSourceFactory2.createDataSource();
        if (requestProperties != null) {
            for (Entry<String, String> requestProperty : requestProperties.entrySet()) {
                dataSource.setRequestProperty((String) requestProperty.getKey(), (String) requestProperty.getValue());
            }
        }
        String url2 = url;
        int manualRedirectCount3 = 0;
        while (true) {
            DataSpec dataSpec = new DataSpec(Uri.parse(url2), data, 0, 0, -1, null, 1);
            inputStream = new DataSourceInputStream(dataSource, dataSpec);
            try {
                byte[] byteArray = Util.toByteArray(inputStream);
                Util.closeQuietly((Closeable) inputStream);
                return byteArray;
            } catch (InvalidResponseCodeException e) {
                InvalidResponseCodeException e2 = e;
                if (e2.responseCode == 307 || e2.responseCode == 308) {
                    manualRedirectCount = manualRedirectCount3 + 1;
                    if (manualRedirectCount3 < 5) {
                        manualRedirectCount2 = 1;
                        url2 = manualRedirectCount2 == 0 ? getRedirectUrl(e2) : null;
                        if (url2 == null) {
                            Util.closeQuietly((Closeable) inputStream);
                            manualRedirectCount3 = manualRedirectCount;
                        } else {
                            throw e2;
                        }
                    } else {
                        manualRedirectCount3 = manualRedirectCount;
                    }
                }
                manualRedirectCount = manualRedirectCount3;
                manualRedirectCount2 = 0;
                url2 = manualRedirectCount2 == 0 ? getRedirectUrl(e2) : null;
                if (url2 == null) {
                }
            } catch (Throwable th) {
                e = th;
                int i = manualRedirectCount;
            }
        }
        Util.closeQuietly((Closeable) inputStream);
        throw e;
    }

    private static String getRedirectUrl(InvalidResponseCodeException exception) {
        Map<String, List<String>> headerFields = exception.headerFields;
        if (headerFields != null) {
            List<String> locationHeaders = (List) headerFields.get("Location");
            if (locationHeaders != null && !locationHeaders.isEmpty()) {
                return (String) locationHeaders.get(0);
            }
        }
        return null;
    }
}
