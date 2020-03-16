package com.google.android.exoplayer2.drm;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.DeniedByServerException;
import android.media.MediaCrypto;
import android.media.MediaCryptoException;
import android.media.MediaDrm;
import android.media.MediaDrm.KeyStatus;
import android.media.MediaDrmException;
import android.media.NotProvisionedException;
import android.media.UnsupportedSchemeException;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.drm.DrmInitData.SchemeData;
import com.google.android.exoplayer2.drm.ExoMediaDrm.KeyRequest;
import com.google.android.exoplayer2.drm.ExoMediaDrm.OnEventListener;
import com.google.android.exoplayer2.drm.ExoMediaDrm.OnKeyStatusChangeListener;
import com.google.android.exoplayer2.drm.ExoMediaDrm.ProvisionRequest;
import com.google.android.exoplayer2.extractor.mp4.PsshAtomUtil;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@TargetApi(23)
public final class FrameworkMediaDrm implements ExoMediaDrm<FrameworkMediaCrypto> {
    private static final String CENC_SCHEME_MIME_TYPE = "cenc";
    private final MediaDrm mediaDrm;
    private final UUID uuid;

    public static FrameworkMediaDrm newInstance(UUID uuid2) throws UnsupportedDrmException {
        try {
            return new FrameworkMediaDrm(uuid2);
        } catch (UnsupportedSchemeException e) {
            throw new UnsupportedDrmException(1, e);
        } catch (Exception e2) {
            throw new UnsupportedDrmException(2, e2);
        }
    }

    private FrameworkMediaDrm(UUID uuid2) throws UnsupportedSchemeException {
        Assertions.checkNotNull(uuid2);
        Assertions.checkArgument(!C.COMMON_PSSH_UUID.equals(uuid2), "Use C.CLEARKEY_UUID instead");
        this.uuid = uuid2;
        this.mediaDrm = new MediaDrm((Util.SDK_INT >= 27 || !C.CLEARKEY_UUID.equals(uuid2)) ? uuid2 : C.COMMON_PSSH_UUID);
        if (C.WIDEVINE_UUID.equals(uuid2) && needsForceWidevineL3Workaround()) {
            forceWidevineL3(this.mediaDrm);
        }
    }

    public void setOnEventListener(OnEventListener<? super FrameworkMediaCrypto> listener) {
        this.mediaDrm.setOnEventListener(listener == null ? null : new MediaDrm.OnEventListener(listener) {
            private final /* synthetic */ OnEventListener f$1;

            {
                this.f$1 = r2;
            }

            public final void onEvent(MediaDrm mediaDrm, byte[] bArr, int i, int i2, byte[] bArr2) {
                this.f$1.onEvent(FrameworkMediaDrm.this, bArr, i, i2, bArr2);
            }
        });
    }

    public void setOnKeyStatusChangeListener(OnKeyStatusChangeListener<? super FrameworkMediaCrypto> listener) {
        if (Util.SDK_INT >= 23) {
            this.mediaDrm.setOnKeyStatusChangeListener(listener == null ? null : new MediaDrm.OnKeyStatusChangeListener(listener) {
                private final /* synthetic */ OnKeyStatusChangeListener f$1;

                {
                    this.f$1 = r2;
                }

                public final void onKeyStatusChange(MediaDrm mediaDrm, byte[] bArr, List list, boolean z) {
                    FrameworkMediaDrm.lambda$setOnKeyStatusChangeListener$1(FrameworkMediaDrm.this, this.f$1, mediaDrm, bArr, list, z);
                }
            }, null);
            return;
        }
        throw new UnsupportedOperationException();
    }

    /* JADX WARNING: Incorrect type for immutable var: ssa=java.util.List, code=java.util.List<android.media.MediaDrm$KeyStatus>, for r10v0, types: [java.util.List, java.util.List<android.media.MediaDrm$KeyStatus>] */
    public static /* synthetic */ void lambda$setOnKeyStatusChangeListener$1(FrameworkMediaDrm frameworkMediaDrm, OnKeyStatusChangeListener listener, MediaDrm mediaDrm2, byte[] sessionId, List<KeyStatus> keyInfo, boolean hasNewUsableKey) {
        List<ExoMediaDrm.KeyStatus> exoKeyInfo = new ArrayList<>();
        for (KeyStatus keyStatus : keyInfo) {
            exoKeyInfo.add(new ExoMediaDrm.KeyStatus(keyStatus.getStatusCode(), keyStatus.getKeyId()));
        }
        listener.onKeyStatusChange(frameworkMediaDrm, sessionId, exoKeyInfo, hasNewUsableKey);
    }

    public byte[] openSession() throws MediaDrmException {
        return this.mediaDrm.openSession();
    }

    public void closeSession(byte[] sessionId) {
        this.mediaDrm.closeSession(sessionId);
    }

    public KeyRequest getKeyRequest(byte[] scope, @Nullable List<SchemeData> schemeDatas, int keyType, @Nullable HashMap<String, String> optionalParameters) throws NotProvisionedException {
        SchemeData schemeData = null;
        byte[] initData = null;
        String mimeType = null;
        if (schemeDatas != null) {
            schemeData = getSchemeData(this.uuid, schemeDatas);
            initData = adjustRequestInitData(this.uuid, schemeData.data);
            mimeType = adjustRequestMimeType(this.uuid, schemeData.mimeType);
        }
        MediaDrm.KeyRequest request = this.mediaDrm.getKeyRequest(scope, initData, mimeType, keyType, optionalParameters);
        byte[] requestData = adjustRequestData(this.uuid, request.getData());
        String licenseServerUrl = request.getDefaultUrl();
        if (TextUtils.isEmpty(licenseServerUrl) && schemeData != null && !TextUtils.isEmpty(schemeData.licenseServerUrl)) {
            licenseServerUrl = schemeData.licenseServerUrl;
        }
        return new KeyRequest(requestData, licenseServerUrl);
    }

    public byte[] provideKeyResponse(byte[] scope, byte[] response) throws NotProvisionedException, DeniedByServerException {
        if (C.CLEARKEY_UUID.equals(this.uuid)) {
            response = ClearKeyUtil.adjustResponseData(response);
        }
        return this.mediaDrm.provideKeyResponse(scope, response);
    }

    public ProvisionRequest getProvisionRequest() {
        MediaDrm.ProvisionRequest request = this.mediaDrm.getProvisionRequest();
        return new ProvisionRequest(request.getData(), request.getDefaultUrl());
    }

    public void provideProvisionResponse(byte[] response) throws DeniedByServerException {
        this.mediaDrm.provideProvisionResponse(response);
    }

    public Map<String, String> queryKeyStatus(byte[] sessionId) {
        return this.mediaDrm.queryKeyStatus(sessionId);
    }

    public void release() {
        this.mediaDrm.release();
    }

    public void restoreKeys(byte[] sessionId, byte[] keySetId) {
        this.mediaDrm.restoreKeys(sessionId, keySetId);
    }

    public String getPropertyString(String propertyName) {
        return this.mediaDrm.getPropertyString(propertyName);
    }

    public byte[] getPropertyByteArray(String propertyName) {
        return this.mediaDrm.getPropertyByteArray(propertyName);
    }

    public void setPropertyString(String propertyName, String value) {
        this.mediaDrm.setPropertyString(propertyName, value);
    }

    public void setPropertyByteArray(String propertyName, byte[] value) {
        this.mediaDrm.setPropertyByteArray(propertyName, value);
    }

    public FrameworkMediaCrypto createMediaCrypto(byte[] initData) throws MediaCryptoException {
        return new FrameworkMediaCrypto(new MediaCrypto(this.uuid, initData), Util.SDK_INT < 21 && C.WIDEVINE_UUID.equals(this.uuid) && "L3".equals(getPropertyString("securityLevel")));
    }

    private static SchemeData getSchemeData(UUID uuid2, List<SchemeData> schemeDatas) {
        if (!C.WIDEVINE_UUID.equals(uuid2)) {
            return (SchemeData) schemeDatas.get(0);
        }
        if (Util.SDK_INT >= 28 && schemeDatas.size() > 1) {
            SchemeData firstSchemeData = (SchemeData) schemeDatas.get(0);
            int concatenatedDataLength = 0;
            boolean canConcatenateData = true;
            int i = 0;
            while (true) {
                if (i >= schemeDatas.size()) {
                    break;
                }
                SchemeData schemeData = (SchemeData) schemeDatas.get(i);
                if (schemeData.requiresSecureDecryption != firstSchemeData.requiresSecureDecryption || !Util.areEqual(schemeData.mimeType, firstSchemeData.mimeType) || !Util.areEqual(schemeData.licenseServerUrl, firstSchemeData.licenseServerUrl) || !PsshAtomUtil.isPsshAtom(schemeData.data)) {
                    canConcatenateData = false;
                } else {
                    concatenatedDataLength += schemeData.data.length;
                    i++;
                }
            }
            if (canConcatenateData) {
                byte[] concatenatedData = new byte[concatenatedDataLength];
                int concatenatedDataPosition = 0;
                for (int i2 = 0; i2 < schemeDatas.size(); i2++) {
                    SchemeData schemeData2 = (SchemeData) schemeDatas.get(i2);
                    int schemeDataLength = schemeData2.data.length;
                    System.arraycopy(schemeData2.data, 0, concatenatedData, concatenatedDataPosition, schemeDataLength);
                    concatenatedDataPosition += schemeDataLength;
                }
                return firstSchemeData.copyWithData(concatenatedData);
            }
        }
        for (int i3 = 0; i3 < schemeDatas.size(); i3++) {
            SchemeData schemeData3 = (SchemeData) schemeDatas.get(i3);
            int version = PsshAtomUtil.parseVersion(schemeData3.data);
            if (Util.SDK_INT < 23 && version == 0) {
                return schemeData3;
            }
            if (Util.SDK_INT >= 23 && version == 1) {
                return schemeData3;
            }
        }
        return (SchemeData) schemeDatas.get(0);
    }

    private static byte[] adjustRequestInitData(UUID uuid2, byte[] initData) {
        if ((Util.SDK_INT < 21 && C.WIDEVINE_UUID.equals(uuid2)) || (C.PLAYREADY_UUID.equals(uuid2) && "Amazon".equals(Util.MANUFACTURER) && ("AFTB".equals(Util.MODEL) || "AFTS".equals(Util.MODEL) || "AFTM".equals(Util.MODEL)))) {
            byte[] psshData = PsshAtomUtil.parseSchemeSpecificData(initData, uuid2);
            if (psshData != null) {
                return psshData;
            }
        }
        return initData;
    }

    private static String adjustRequestMimeType(UUID uuid2, String mimeType) {
        if (Util.SDK_INT >= 26 || !C.CLEARKEY_UUID.equals(uuid2) || (!MimeTypes.VIDEO_MP4.equals(mimeType) && !MimeTypes.AUDIO_MP4.equals(mimeType))) {
            return mimeType;
        }
        return "cenc";
    }

    private static byte[] adjustRequestData(UUID uuid2, byte[] requestData) {
        if (C.CLEARKEY_UUID.equals(uuid2)) {
            return ClearKeyUtil.adjustRequestData(requestData);
        }
        return requestData;
    }

    @SuppressLint({"WrongConstant"})
    private static void forceWidevineL3(MediaDrm mediaDrm2) {
        mediaDrm2.setPropertyString("securityLevel", "L3");
    }

    private static boolean needsForceWidevineL3Workaround() {
        return "ASUS_Z00AD".equals(Util.MODEL);
    }
}
