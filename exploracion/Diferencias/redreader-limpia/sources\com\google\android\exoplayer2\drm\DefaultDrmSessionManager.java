package com.google.android.exoplayer2.drm;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.drm.DefaultDrmSession.ProvisioningManager;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager.MissingSchemeDataException;
import com.google.android.exoplayer2.drm.DrmInitData.SchemeData;
import com.google.android.exoplayer2.drm.DrmSession.DrmSessionException;
import com.google.android.exoplayer2.drm.ExoMediaCrypto;
import com.google.android.exoplayer2.drm.ExoMediaDrm.OnEventListener;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.EventDispatcher;
import com.google.android.exoplayer2.util.EventDispatcher.Event;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@TargetApi(18)
public class DefaultDrmSessionManager<T extends ExoMediaCrypto> implements DrmSessionManager<T>, ProvisioningManager<T> {
    public static final int INITIAL_DRM_REQUEST_RETRY_COUNT = 3;
    public static final int MODE_DOWNLOAD = 2;
    public static final int MODE_PLAYBACK = 0;
    public static final int MODE_QUERY = 1;
    public static final int MODE_RELEASE = 3;
    public static final String PLAYREADY_CUSTOM_DATA_KEY = "PRCustomData";
    private static final String TAG = "DefaultDrmSessionMgr";
    private final MediaDrmCallback callback;
    private final EventDispatcher<DefaultDrmSessionEventListener> eventDispatcher;
    private final int initialDrmRequestRetryCount;
    private final ExoMediaDrm<T> mediaDrm;
    volatile MediaDrmHandler mediaDrmHandler;
    /* access modifiers changed from: private */
    public int mode;
    private final boolean multiSession;
    private byte[] offlineLicenseKeySetId;
    private final HashMap<String, String> optionalKeyRequestParameters;
    private Looper playbackLooper;
    private final List<DefaultDrmSession<T>> provisioningSessions;
    /* access modifiers changed from: private */
    public final List<DefaultDrmSession<T>> sessions;
    private final UUID uuid;

    @Deprecated
    public interface EventListener extends DefaultDrmSessionEventListener {
    }

    private class MediaDrmEventListener implements OnEventListener<T> {
        private MediaDrmEventListener() {
        }

        public void onEvent(ExoMediaDrm<? extends T> exoMediaDrm, byte[] sessionId, int event, int extra, byte[] data) {
            if (DefaultDrmSessionManager.this.mode == 0) {
                DefaultDrmSessionManager.this.mediaDrmHandler.obtainMessage(event, sessionId).sendToTarget();
            }
        }
    }

    @SuppressLint({"HandlerLeak"})
    private class MediaDrmHandler extends Handler {
        public MediaDrmHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            byte[] sessionId = (byte[]) msg.obj;
            for (DefaultDrmSession<T> session : DefaultDrmSessionManager.this.sessions) {
                if (session.hasSessionId(sessionId)) {
                    session.onMediaDrmEvent(msg.what);
                    return;
                }
            }
        }
    }

    public static final class MissingSchemeDataException extends Exception {
        private MissingSchemeDataException(UUID uuid) {
            StringBuilder sb = new StringBuilder();
            sb.append("Media does not support uuid: ");
            sb.append(uuid);
            super(sb.toString());
        }
    }

    @Documented
    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {
    }

    @Deprecated
    public static DefaultDrmSessionManager<FrameworkMediaCrypto> newWidevineInstance(MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2, Handler eventHandler, DefaultDrmSessionEventListener eventListener) throws UnsupportedDrmException {
        DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = newWidevineInstance(callback2, optionalKeyRequestParameters2);
        if (!(eventHandler == null || eventListener == null)) {
            drmSessionManager.addListener(eventHandler, eventListener);
        }
        return drmSessionManager;
    }

    public static DefaultDrmSessionManager<FrameworkMediaCrypto> newWidevineInstance(MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2) throws UnsupportedDrmException {
        return newFrameworkInstance(C.WIDEVINE_UUID, callback2, optionalKeyRequestParameters2);
    }

    @Deprecated
    public static DefaultDrmSessionManager<FrameworkMediaCrypto> newPlayReadyInstance(MediaDrmCallback callback2, String customData, Handler eventHandler, DefaultDrmSessionEventListener eventListener) throws UnsupportedDrmException {
        DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = newPlayReadyInstance(callback2, customData);
        if (!(eventHandler == null || eventListener == null)) {
            drmSessionManager.addListener(eventHandler, eventListener);
        }
        return drmSessionManager;
    }

    public static DefaultDrmSessionManager<FrameworkMediaCrypto> newPlayReadyInstance(MediaDrmCallback callback2, String customData) throws UnsupportedDrmException {
        HashMap<String, String> optionalKeyRequestParameters2;
        if (!TextUtils.isEmpty(customData)) {
            optionalKeyRequestParameters2 = new HashMap<>();
            optionalKeyRequestParameters2.put(PLAYREADY_CUSTOM_DATA_KEY, customData);
        } else {
            optionalKeyRequestParameters2 = null;
        }
        return newFrameworkInstance(C.PLAYREADY_UUID, callback2, optionalKeyRequestParameters2);
    }

    @Deprecated
    public static DefaultDrmSessionManager<FrameworkMediaCrypto> newFrameworkInstance(UUID uuid2, MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2, Handler eventHandler, DefaultDrmSessionEventListener eventListener) throws UnsupportedDrmException {
        DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = newFrameworkInstance(uuid2, callback2, optionalKeyRequestParameters2);
        if (!(eventHandler == null || eventListener == null)) {
            drmSessionManager.addListener(eventHandler, eventListener);
        }
        return drmSessionManager;
    }

    public static DefaultDrmSessionManager<FrameworkMediaCrypto> newFrameworkInstance(UUID uuid2, MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2) throws UnsupportedDrmException {
        DefaultDrmSessionManager defaultDrmSessionManager = new DefaultDrmSessionManager(uuid2, (ExoMediaDrm<T>) FrameworkMediaDrm.newInstance(uuid2), callback2, optionalKeyRequestParameters2, false, 3);
        return defaultDrmSessionManager;
    }

    @Deprecated
    public DefaultDrmSessionManager(UUID uuid2, ExoMediaDrm<T> mediaDrm2, MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2, Handler eventHandler, DefaultDrmSessionEventListener eventListener) {
        this(uuid2, mediaDrm2, callback2, optionalKeyRequestParameters2);
        if (eventHandler != null && eventListener != null) {
            addListener(eventHandler, eventListener);
        }
    }

    public DefaultDrmSessionManager(UUID uuid2, ExoMediaDrm<T> mediaDrm2, MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2) {
        this(uuid2, mediaDrm2, callback2, optionalKeyRequestParameters2, false, 3);
    }

    @Deprecated
    public DefaultDrmSessionManager(UUID uuid2, ExoMediaDrm<T> mediaDrm2, MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2, Handler eventHandler, DefaultDrmSessionEventListener eventListener, boolean multiSession2) {
        this(uuid2, mediaDrm2, callback2, optionalKeyRequestParameters2, multiSession2);
        if (eventHandler != null && eventListener != null) {
            addListener(eventHandler, eventListener);
        }
    }

    public DefaultDrmSessionManager(UUID uuid2, ExoMediaDrm<T> mediaDrm2, MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2, boolean multiSession2) {
        this(uuid2, mediaDrm2, callback2, optionalKeyRequestParameters2, multiSession2, 3);
    }

    @Deprecated
    public DefaultDrmSessionManager(UUID uuid2, ExoMediaDrm<T> mediaDrm2, MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2, Handler eventHandler, DefaultDrmSessionEventListener eventListener, boolean multiSession2, int initialDrmRequestRetryCount2) {
        this(uuid2, mediaDrm2, callback2, optionalKeyRequestParameters2, multiSession2, initialDrmRequestRetryCount2);
        if (eventHandler != null && eventListener != null) {
            addListener(eventHandler, eventListener);
        }
    }

    public DefaultDrmSessionManager(UUID uuid2, ExoMediaDrm<T> mediaDrm2, MediaDrmCallback callback2, HashMap<String, String> optionalKeyRequestParameters2, boolean multiSession2, int initialDrmRequestRetryCount2) {
        Assertions.checkNotNull(uuid2);
        Assertions.checkNotNull(mediaDrm2);
        Assertions.checkArgument(!C.COMMON_PSSH_UUID.equals(uuid2), "Use C.CLEARKEY_UUID instead");
        this.uuid = uuid2;
        this.mediaDrm = mediaDrm2;
        this.callback = callback2;
        this.optionalKeyRequestParameters = optionalKeyRequestParameters2;
        this.eventDispatcher = new EventDispatcher<>();
        this.multiSession = multiSession2;
        this.initialDrmRequestRetryCount = initialDrmRequestRetryCount2;
        this.mode = 0;
        this.sessions = new ArrayList();
        this.provisioningSessions = new ArrayList();
        if (multiSession2 && C.WIDEVINE_UUID.equals(uuid2) && Util.SDK_INT >= 19) {
            mediaDrm2.setPropertyString("sessionSharing", "enable");
        }
        mediaDrm2.setOnEventListener(new MediaDrmEventListener());
    }

    public final void addListener(Handler handler, DefaultDrmSessionEventListener eventListener) {
        this.eventDispatcher.addListener(handler, eventListener);
    }

    public final void removeListener(DefaultDrmSessionEventListener eventListener) {
        this.eventDispatcher.removeListener(eventListener);
    }

    public final String getPropertyString(String key) {
        return this.mediaDrm.getPropertyString(key);
    }

    public final void setPropertyString(String key, String value) {
        this.mediaDrm.setPropertyString(key, value);
    }

    public final byte[] getPropertyByteArray(String key) {
        return this.mediaDrm.getPropertyByteArray(key);
    }

    public final void setPropertyByteArray(String key, byte[] value) {
        this.mediaDrm.setPropertyByteArray(key, value);
    }

    public void setMode(int mode2, byte[] offlineLicenseKeySetId2) {
        Assertions.checkState(this.sessions.isEmpty());
        if (mode2 == 1 || mode2 == 3) {
            Assertions.checkNotNull(offlineLicenseKeySetId2);
        }
        this.mode = mode2;
        this.offlineLicenseKeySetId = offlineLicenseKeySetId2;
    }

    public boolean canAcquireSession(@NonNull DrmInitData drmInitData) {
        boolean z = true;
        if (this.offlineLicenseKeySetId != null) {
            return true;
        }
        if (getSchemeDatas(drmInitData, this.uuid, true).isEmpty()) {
            if (drmInitData.schemeDataCount != 1 || !drmInitData.get(0).matches(C.COMMON_PSSH_UUID)) {
                return false;
            }
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("DrmInitData only contains common PSSH SchemeData. Assuming support for: ");
            sb.append(this.uuid);
            Log.w(str, sb.toString());
        }
        String schemeType = drmInitData.schemeType;
        if (schemeType == null || C.CENC_TYPE_cenc.equals(schemeType)) {
            return true;
        }
        if (!C.CENC_TYPE_cbc1.equals(schemeType) && !C.CENC_TYPE_cbcs.equals(schemeType) && !C.CENC_TYPE_cens.equals(schemeType)) {
            return true;
        }
        if (Util.SDK_INT < 25) {
            z = false;
        }
        return z;
    }

    public DrmSession<T> acquireSession(Looper playbackLooper2, DrmInitData drmInitData) {
        List list;
        DefaultDrmSession defaultDrmSession;
        Looper looper = playbackLooper2;
        Looper looper2 = this.playbackLooper;
        Assertions.checkState(looper2 == null || looper2 == looper);
        if (this.sessions.isEmpty()) {
            this.playbackLooper = looper;
            if (this.mediaDrmHandler == null) {
                this.mediaDrmHandler = new MediaDrmHandler<>(looper);
            }
        }
        DefaultDrmSession defaultDrmSession2 = null;
        if (this.offlineLicenseKeySetId == null) {
            List<SchemeData> schemeDatas = getSchemeDatas(drmInitData, this.uuid, false);
            if (schemeDatas.isEmpty()) {
                MissingSchemeDataException error = new MissingSchemeDataException(this.uuid);
                this.eventDispatcher.dispatch(new Event() {
                    public final void sendTo(Object obj) {
                        ((DefaultDrmSessionEventListener) obj).onDrmSessionManagerError(MissingSchemeDataException.this);
                    }
                });
                return new ErrorStateDrmSession(new DrmSessionException(error));
            }
            list = schemeDatas;
        } else {
            DrmInitData drmInitData2 = drmInitData;
            list = null;
        }
        if (this.multiSession) {
            Iterator it = this.sessions.iterator();
            while (true) {
                if (!it.hasNext()) {
                    defaultDrmSession = null;
                    break;
                }
                DefaultDrmSession<T> existingSession = (DefaultDrmSession) it.next();
                if (Util.areEqual(existingSession.schemeDatas, list)) {
                    defaultDrmSession = existingSession;
                    break;
                }
            }
        } else {
            if (!this.sessions.isEmpty()) {
                defaultDrmSession2 = (DefaultDrmSession) this.sessions.get(0);
            }
            defaultDrmSession = defaultDrmSession2;
        }
        if (defaultDrmSession == null) {
            DefaultDrmSession<T> session = new DefaultDrmSession<>(this.uuid, this.mediaDrm, this, list, this.mode, this.offlineLicenseKeySetId, this.optionalKeyRequestParameters, this.callback, playbackLooper2, this.eventDispatcher, this.initialDrmRequestRetryCount);
            this.sessions.add(session);
            defaultDrmSession = session;
        }
        defaultDrmSession.acquire();
        return defaultDrmSession;
    }

    public void releaseSession(DrmSession<T> session) {
        if (!(session instanceof ErrorStateDrmSession)) {
            DefaultDrmSession<T> drmSession = (DefaultDrmSession) session;
            if (drmSession.release()) {
                this.sessions.remove(drmSession);
                if (this.provisioningSessions.size() > 1 && this.provisioningSessions.get(0) == drmSession) {
                    ((DefaultDrmSession) this.provisioningSessions.get(1)).provision();
                }
                this.provisioningSessions.remove(drmSession);
            }
        }
    }

    public void provisionRequired(DefaultDrmSession<T> session) {
        this.provisioningSessions.add(session);
        if (this.provisioningSessions.size() == 1) {
            session.provision();
        }
    }

    public void onProvisionCompleted() {
        for (DefaultDrmSession<T> session : this.provisioningSessions) {
            session.onProvisionCompleted();
        }
        this.provisioningSessions.clear();
    }

    public void onProvisionError(Exception error) {
        for (DefaultDrmSession<T> session : this.provisioningSessions) {
            session.onProvisionError(error);
        }
        this.provisioningSessions.clear();
    }

    private static List<SchemeData> getSchemeDatas(DrmInitData drmInitData, UUID uuid2, boolean allowMissingData) {
        List<SchemeData> matchingSchemeDatas = new ArrayList<>(drmInitData.schemeDataCount);
        for (int i = 0; i < drmInitData.schemeDataCount; i++) {
            SchemeData schemeData = drmInitData.get(i);
            if ((schemeData.matches(uuid2) || (C.CLEARKEY_UUID.equals(uuid2) && schemeData.matches(C.COMMON_PSSH_UUID))) && (schemeData.data != null || allowMissingData)) {
                matchingSchemeDatas.add(schemeData);
            }
        }
        return matchingSchemeDatas;
    }
}
