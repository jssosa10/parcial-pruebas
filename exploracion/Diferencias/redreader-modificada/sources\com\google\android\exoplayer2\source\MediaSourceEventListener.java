package com.google.android.exoplayer2.source;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CheckResult;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.source.MediaSource.MediaPeriodId;
import com.google.android.exoplayer2.source.MediaSourceEventListener.EventDispatcher;
import com.google.android.exoplayer2.source.MediaSourceEventListener.LoadEventInfo;
import com.google.android.exoplayer2.source.MediaSourceEventListener.MediaLoadData;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public interface MediaSourceEventListener {

    public static final class EventDispatcher {
        private final CopyOnWriteArrayList<ListenerAndHandler> listenerAndHandlers;
        @Nullable
        public final MediaPeriodId mediaPeriodId;
        private final long mediaTimeOffsetMs;
        public final int windowIndex;

        private static final class ListenerAndHandler {
            public final Handler handler;
            public final MediaSourceEventListener listener;

            public ListenerAndHandler(Handler handler2, MediaSourceEventListener listener2) {
                this.handler = handler2;
                this.listener = listener2;
            }
        }

        public EventDispatcher() {
            this(new CopyOnWriteArrayList(), 0, null, 0);
        }

        private EventDispatcher(CopyOnWriteArrayList<ListenerAndHandler> listenerAndHandlers2, int windowIndex2, @Nullable MediaPeriodId mediaPeriodId2, long mediaTimeOffsetMs2) {
            this.listenerAndHandlers = listenerAndHandlers2;
            this.windowIndex = windowIndex2;
            this.mediaPeriodId = mediaPeriodId2;
            this.mediaTimeOffsetMs = mediaTimeOffsetMs2;
        }

        @CheckResult
        public EventDispatcher withParameters(int windowIndex2, @Nullable MediaPeriodId mediaPeriodId2, long mediaTimeOffsetMs2) {
            EventDispatcher eventDispatcher = new EventDispatcher(this.listenerAndHandlers, windowIndex2, mediaPeriodId2, mediaTimeOffsetMs2);
            return eventDispatcher;
        }

        public void addEventListener(Handler handler, MediaSourceEventListener eventListener) {
            Assertions.checkArgument((handler == null || eventListener == null) ? false : true);
            this.listenerAndHandlers.add(new ListenerAndHandler(handler, eventListener));
        }

        public void removeEventListener(MediaSourceEventListener eventListener) {
            Iterator it = this.listenerAndHandlers.iterator();
            while (it.hasNext()) {
                ListenerAndHandler listenerAndHandler = (ListenerAndHandler) it.next();
                if (listenerAndHandler.listener == eventListener) {
                    this.listenerAndHandlers.remove(listenerAndHandler);
                }
            }
        }

        public void mediaPeriodCreated() {
            MediaPeriodId mediaPeriodId2 = (MediaPeriodId) Assertions.checkNotNull(this.mediaPeriodId);
            Iterator it = this.listenerAndHandlers.iterator();
            while (it.hasNext()) {
                ListenerAndHandler listenerAndHandler = (ListenerAndHandler) it.next();
                postOrRun(listenerAndHandler.handler, new Runnable(listenerAndHandler.listener, mediaPeriodId2) {
                    private final /* synthetic */ MediaSourceEventListener f$1;
                    private final /* synthetic */ MediaPeriodId f$2;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                    }

                    public final void run() {
                        this.f$1.onMediaPeriodCreated(EventDispatcher.this.windowIndex, this.f$2);
                    }
                });
            }
        }

        public void mediaPeriodReleased() {
            MediaPeriodId mediaPeriodId2 = (MediaPeriodId) Assertions.checkNotNull(this.mediaPeriodId);
            Iterator it = this.listenerAndHandlers.iterator();
            while (it.hasNext()) {
                ListenerAndHandler listenerAndHandler = (ListenerAndHandler) it.next();
                postOrRun(listenerAndHandler.handler, new Runnable(listenerAndHandler.listener, mediaPeriodId2) {
                    private final /* synthetic */ MediaSourceEventListener f$1;
                    private final /* synthetic */ MediaPeriodId f$2;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                    }

                    public final void run() {
                        this.f$1.onMediaPeriodReleased(EventDispatcher.this.windowIndex, this.f$2);
                    }
                });
            }
        }

        public void loadStarted(DataSpec dataSpec, int dataType, long elapsedRealtimeMs) {
            loadStarted(dataSpec, dataType, -1, null, 0, null, C.TIME_UNSET, C.TIME_UNSET, elapsedRealtimeMs);
        }

        public void loadStarted(DataSpec dataSpec, int dataType, int trackType, @Nullable Format trackFormat, int trackSelectionReason, @Nullable Object trackSelectionData, long mediaStartTimeUs, long mediaEndTimeUs, long elapsedRealtimeMs) {
            LoadEventInfo loadEventInfo = new LoadEventInfo(dataSpec, dataSpec.uri, Collections.emptyMap(), elapsedRealtimeMs, 0, 0);
            MediaLoadData mediaLoadData = new MediaLoadData(dataType, trackType, trackFormat, trackSelectionReason, trackSelectionData, adjustMediaTime(mediaStartTimeUs), adjustMediaTime(mediaEndTimeUs));
            loadStarted(loadEventInfo, mediaLoadData);
        }

        public void loadStarted(LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
            Iterator it = this.listenerAndHandlers.iterator();
            while (it.hasNext()) {
                ListenerAndHandler listenerAndHandler = (ListenerAndHandler) it.next();
                postOrRun(listenerAndHandler.handler, new Runnable(listenerAndHandler.listener, loadEventInfo, mediaLoadData) {
                    private final /* synthetic */ MediaSourceEventListener f$1;
                    private final /* synthetic */ LoadEventInfo f$2;
                    private final /* synthetic */ MediaLoadData f$3;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                        this.f$3 = r4;
                    }

                    public final void run() {
                        this.f$1.onLoadStarted(EventDispatcher.this.windowIndex, EventDispatcher.this.mediaPeriodId, this.f$2, this.f$3);
                    }
                });
            }
        }

        public void loadCompleted(DataSpec dataSpec, Uri uri, Map<String, List<String>> responseHeaders, int dataType, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
            loadCompleted(dataSpec, uri, responseHeaders, dataType, -1, null, 0, null, C.TIME_UNSET, C.TIME_UNSET, elapsedRealtimeMs, loadDurationMs, bytesLoaded);
        }

        public void loadCompleted(DataSpec dataSpec, Uri uri, Map<String, List<String>> responseHeaders, int dataType, int trackType, @Nullable Format trackFormat, int trackSelectionReason, @Nullable Object trackSelectionData, long mediaStartTimeUs, long mediaEndTimeUs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
            LoadEventInfo loadEventInfo = new LoadEventInfo(dataSpec, uri, responseHeaders, elapsedRealtimeMs, loadDurationMs, bytesLoaded);
            MediaLoadData mediaLoadData = new MediaLoadData(dataType, trackType, trackFormat, trackSelectionReason, trackSelectionData, adjustMediaTime(mediaStartTimeUs), adjustMediaTime(mediaEndTimeUs));
            loadCompleted(loadEventInfo, mediaLoadData);
        }

        public void loadCompleted(LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
            Iterator it = this.listenerAndHandlers.iterator();
            while (it.hasNext()) {
                ListenerAndHandler listenerAndHandler = (ListenerAndHandler) it.next();
                postOrRun(listenerAndHandler.handler, new Runnable(listenerAndHandler.listener, loadEventInfo, mediaLoadData) {
                    private final /* synthetic */ MediaSourceEventListener f$1;
                    private final /* synthetic */ LoadEventInfo f$2;
                    private final /* synthetic */ MediaLoadData f$3;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                        this.f$3 = r4;
                    }

                    public final void run() {
                        this.f$1.onLoadCompleted(EventDispatcher.this.windowIndex, EventDispatcher.this.mediaPeriodId, this.f$2, this.f$3);
                    }
                });
            }
        }

        public void loadCanceled(DataSpec dataSpec, Uri uri, Map<String, List<String>> responseHeaders, int dataType, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
            loadCanceled(dataSpec, uri, responseHeaders, dataType, -1, null, 0, null, C.TIME_UNSET, C.TIME_UNSET, elapsedRealtimeMs, loadDurationMs, bytesLoaded);
        }

        public void loadCanceled(DataSpec dataSpec, Uri uri, Map<String, List<String>> responseHeaders, int dataType, int trackType, @Nullable Format trackFormat, int trackSelectionReason, @Nullable Object trackSelectionData, long mediaStartTimeUs, long mediaEndTimeUs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
            LoadEventInfo loadEventInfo = new LoadEventInfo(dataSpec, uri, responseHeaders, elapsedRealtimeMs, loadDurationMs, bytesLoaded);
            MediaLoadData mediaLoadData = new MediaLoadData(dataType, trackType, trackFormat, trackSelectionReason, trackSelectionData, adjustMediaTime(mediaStartTimeUs), adjustMediaTime(mediaEndTimeUs));
            loadCanceled(loadEventInfo, mediaLoadData);
        }

        public void loadCanceled(LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
            Iterator it = this.listenerAndHandlers.iterator();
            while (it.hasNext()) {
                ListenerAndHandler listenerAndHandler = (ListenerAndHandler) it.next();
                postOrRun(listenerAndHandler.handler, new Runnable(listenerAndHandler.listener, loadEventInfo, mediaLoadData) {
                    private final /* synthetic */ MediaSourceEventListener f$1;
                    private final /* synthetic */ LoadEventInfo f$2;
                    private final /* synthetic */ MediaLoadData f$3;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                        this.f$3 = r4;
                    }

                    public final void run() {
                        this.f$1.onLoadCanceled(EventDispatcher.this.windowIndex, EventDispatcher.this.mediaPeriodId, this.f$2, this.f$3);
                    }
                });
            }
        }

        public void loadError(DataSpec dataSpec, Uri uri, Map<String, List<String>> responseHeaders, int dataType, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {
            loadError(dataSpec, uri, responseHeaders, dataType, -1, null, 0, null, C.TIME_UNSET, C.TIME_UNSET, elapsedRealtimeMs, loadDurationMs, bytesLoaded, error, wasCanceled);
        }

        public void loadError(DataSpec dataSpec, Uri uri, Map<String, List<String>> responseHeaders, int dataType, int trackType, @Nullable Format trackFormat, int trackSelectionReason, @Nullable Object trackSelectionData, long mediaStartTimeUs, long mediaEndTimeUs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {
            LoadEventInfo loadEventInfo = new LoadEventInfo(dataSpec, uri, responseHeaders, elapsedRealtimeMs, loadDurationMs, bytesLoaded);
            MediaLoadData mediaLoadData = new MediaLoadData(dataType, trackType, trackFormat, trackSelectionReason, trackSelectionData, adjustMediaTime(mediaStartTimeUs), adjustMediaTime(mediaEndTimeUs));
            loadError(loadEventInfo, mediaLoadData, error, wasCanceled);
        }

        public void loadError(LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
            Iterator it = this.listenerAndHandlers.iterator();
            while (it.hasNext()) {
                ListenerAndHandler listenerAndHandler = (ListenerAndHandler) it.next();
                MediaSourceEventListener listener = listenerAndHandler.listener;
                Handler handler = listenerAndHandler.handler;
                $$Lambda$MediaSourceEventListener$EventDispatcher$0XTAsNqR4TUW1yA_ZD1_p3oT84 r0 = new Runnable(listener, loadEventInfo, mediaLoadData, error, wasCanceled) {
                    private final /* synthetic */ MediaSourceEventListener f$1;
                    private final /* synthetic */ LoadEventInfo f$2;
                    private final /* synthetic */ MediaLoadData f$3;
                    private final /* synthetic */ IOException f$4;
                    private final /* synthetic */ boolean f$5;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                        this.f$3 = r4;
                        this.f$4 = r5;
                        this.f$5 = r6;
                    }

                    public final void run() {
                        this.f$1.onLoadError(EventDispatcher.this.windowIndex, EventDispatcher.this.mediaPeriodId, this.f$2, this.f$3, this.f$4, this.f$5);
                    }
                };
                postOrRun(handler, r0);
            }
        }

        public void readingStarted() {
            MediaPeriodId mediaPeriodId2 = (MediaPeriodId) Assertions.checkNotNull(this.mediaPeriodId);
            Iterator it = this.listenerAndHandlers.iterator();
            while (it.hasNext()) {
                ListenerAndHandler listenerAndHandler = (ListenerAndHandler) it.next();
                postOrRun(listenerAndHandler.handler, new Runnable(listenerAndHandler.listener, mediaPeriodId2) {
                    private final /* synthetic */ MediaSourceEventListener f$1;
                    private final /* synthetic */ MediaPeriodId f$2;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                    }

                    public final void run() {
                        this.f$1.onReadingStarted(EventDispatcher.this.windowIndex, this.f$2);
                    }
                });
            }
        }

        public void upstreamDiscarded(int trackType, long mediaStartTimeUs, long mediaEndTimeUs) {
            MediaLoadData mediaLoadData = new MediaLoadData(1, trackType, null, 3, null, adjustMediaTime(mediaStartTimeUs), adjustMediaTime(mediaEndTimeUs));
            upstreamDiscarded(mediaLoadData);
        }

        public void upstreamDiscarded(MediaLoadData mediaLoadData) {
            MediaPeriodId mediaPeriodId2 = (MediaPeriodId) Assertions.checkNotNull(this.mediaPeriodId);
            Iterator it = this.listenerAndHandlers.iterator();
            while (it.hasNext()) {
                ListenerAndHandler listenerAndHandler = (ListenerAndHandler) it.next();
                postOrRun(listenerAndHandler.handler, new Runnable(listenerAndHandler.listener, mediaPeriodId2, mediaLoadData) {
                    private final /* synthetic */ MediaSourceEventListener f$1;
                    private final /* synthetic */ MediaPeriodId f$2;
                    private final /* synthetic */ MediaLoadData f$3;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                        this.f$3 = r4;
                    }

                    public final void run() {
                        this.f$1.onUpstreamDiscarded(EventDispatcher.this.windowIndex, this.f$2, this.f$3);
                    }
                });
            }
        }

        public void downstreamFormatChanged(int trackType, @Nullable Format trackFormat, int trackSelectionReason, @Nullable Object trackSelectionData, long mediaTimeUs) {
            MediaLoadData mediaLoadData = new MediaLoadData(1, trackType, trackFormat, trackSelectionReason, trackSelectionData, adjustMediaTime(mediaTimeUs), C.TIME_UNSET);
            downstreamFormatChanged(mediaLoadData);
        }

        public void downstreamFormatChanged(MediaLoadData mediaLoadData) {
            Iterator it = this.listenerAndHandlers.iterator();
            while (it.hasNext()) {
                ListenerAndHandler listenerAndHandler = (ListenerAndHandler) it.next();
                postOrRun(listenerAndHandler.handler, new Runnable(listenerAndHandler.listener, mediaLoadData) {
                    private final /* synthetic */ MediaSourceEventListener f$1;
                    private final /* synthetic */ MediaLoadData f$2;

                    {
                        this.f$1 = r2;
                        this.f$2 = r3;
                    }

                    public final void run() {
                        this.f$1.onDownstreamFormatChanged(EventDispatcher.this.windowIndex, EventDispatcher.this.mediaPeriodId, this.f$2);
                    }
                });
            }
        }

        private long adjustMediaTime(long mediaTimeUs) {
            long mediaTimeMs = C.usToMs(mediaTimeUs);
            return mediaTimeMs == C.TIME_UNSET ? C.TIME_UNSET : this.mediaTimeOffsetMs + mediaTimeMs;
        }

        private void postOrRun(Handler handler, Runnable runnable) {
            if (handler.getLooper() == Looper.myLooper()) {
                runnable.run();
            } else {
                handler.post(runnable);
            }
        }
    }

    public static final class LoadEventInfo {
        public final long bytesLoaded;
        public final DataSpec dataSpec;
        public final long elapsedRealtimeMs;
        public final long loadDurationMs;
        public final Map<String, List<String>> responseHeaders;
        public final Uri uri;

        public LoadEventInfo(DataSpec dataSpec2, Uri uri2, Map<String, List<String>> responseHeaders2, long elapsedRealtimeMs2, long loadDurationMs2, long bytesLoaded2) {
            this.dataSpec = dataSpec2;
            this.uri = uri2;
            this.responseHeaders = responseHeaders2;
            this.elapsedRealtimeMs = elapsedRealtimeMs2;
            this.loadDurationMs = loadDurationMs2;
            this.bytesLoaded = bytesLoaded2;
        }
    }

    public static final class MediaLoadData {
        public final int dataType;
        public final long mediaEndTimeMs;
        public final long mediaStartTimeMs;
        @Nullable
        public final Format trackFormat;
        @Nullable
        public final Object trackSelectionData;
        public final int trackSelectionReason;
        public final int trackType;

        public MediaLoadData(int dataType2, int trackType2, @Nullable Format trackFormat2, int trackSelectionReason2, @Nullable Object trackSelectionData2, long mediaStartTimeMs2, long mediaEndTimeMs2) {
            this.dataType = dataType2;
            this.trackType = trackType2;
            this.trackFormat = trackFormat2;
            this.trackSelectionReason = trackSelectionReason2;
            this.trackSelectionData = trackSelectionData2;
            this.mediaStartTimeMs = mediaStartTimeMs2;
            this.mediaEndTimeMs = mediaEndTimeMs2;
        }
    }

    void onDownstreamFormatChanged(int i, @Nullable MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData);

    void onLoadCanceled(int i, @Nullable MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData);

    void onLoadCompleted(int i, @Nullable MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData);

    void onLoadError(int i, @Nullable MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException iOException, boolean z);

    void onLoadStarted(int i, @Nullable MediaPeriodId mediaPeriodId, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData);

    void onMediaPeriodCreated(int i, MediaPeriodId mediaPeriodId);

    void onMediaPeriodReleased(int i, MediaPeriodId mediaPeriodId);

    void onReadingStarted(int i, MediaPeriodId mediaPeriodId);

    void onUpstreamDiscarded(int i, MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData);
}
