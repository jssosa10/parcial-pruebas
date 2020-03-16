package com.google.android.exoplayer2.source;

import android.os.Handler;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource.MediaPeriodId;
import com.google.android.exoplayer2.source.MediaSource.SourceInfoRefreshListener;
import com.google.android.exoplayer2.source.MediaSourceEventListener.EventDispatcher;
import com.google.android.exoplayer2.source.MediaSourceEventListener.LoadEventInfo;
import com.google.android.exoplayer2.source.MediaSourceEventListener.MediaLoadData;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.util.HashMap;

public abstract class CompositeMediaSource<T> extends BaseMediaSource {
    private final HashMap<T, MediaSourceAndListener> childSources = new HashMap<>();
    @Nullable
    private Handler eventHandler;
    @Nullable
    private TransferListener mediaTransferListener;
    @Nullable
    private ExoPlayer player;

    private final class ForwardingEventListener implements MediaSourceEventListener {
        private EventDispatcher eventDispatcher;
        private final T id;

        public ForwardingEventListener(T id2) {
            this.eventDispatcher = CompositeMediaSource.this.createEventDispatcher(null);
            this.id = id2;
        }

        public void onMediaPeriodCreated(int windowIndex, MediaPeriodId mediaPeriodId) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.eventDispatcher.mediaPeriodCreated();
            }
        }

        public void onMediaPeriodReleased(int windowIndex, MediaPeriodId mediaPeriodId) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.eventDispatcher.mediaPeriodReleased();
            }
        }

        public void onLoadStarted(int windowIndex, @Nullable MediaPeriodId mediaPeriodId, LoadEventInfo loadEventData, MediaLoadData mediaLoadData) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.eventDispatcher.loadStarted(loadEventData, maybeUpdateMediaLoadData(mediaLoadData));
            }
        }

        public void onLoadCompleted(int windowIndex, @Nullable MediaPeriodId mediaPeriodId, LoadEventInfo loadEventData, MediaLoadData mediaLoadData) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.eventDispatcher.loadCompleted(loadEventData, maybeUpdateMediaLoadData(mediaLoadData));
            }
        }

        public void onLoadCanceled(int windowIndex, @Nullable MediaPeriodId mediaPeriodId, LoadEventInfo loadEventData, MediaLoadData mediaLoadData) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.eventDispatcher.loadCanceled(loadEventData, maybeUpdateMediaLoadData(mediaLoadData));
            }
        }

        public void onLoadError(int windowIndex, @Nullable MediaPeriodId mediaPeriodId, LoadEventInfo loadEventData, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.eventDispatcher.loadError(loadEventData, maybeUpdateMediaLoadData(mediaLoadData), error, wasCanceled);
            }
        }

        public void onReadingStarted(int windowIndex, MediaPeriodId mediaPeriodId) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.eventDispatcher.readingStarted();
            }
        }

        public void onUpstreamDiscarded(int windowIndex, @Nullable MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.eventDispatcher.upstreamDiscarded(maybeUpdateMediaLoadData(mediaLoadData));
            }
        }

        public void onDownstreamFormatChanged(int windowIndex, @Nullable MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
            if (maybeUpdateEventDispatcher(windowIndex, mediaPeriodId)) {
                this.eventDispatcher.downstreamFormatChanged(maybeUpdateMediaLoadData(mediaLoadData));
            }
        }

        private boolean maybeUpdateEventDispatcher(int childWindowIndex, @Nullable MediaPeriodId childMediaPeriodId) {
            MediaPeriodId mediaPeriodId = null;
            if (childMediaPeriodId != null) {
                mediaPeriodId = CompositeMediaSource.this.getMediaPeriodIdForChildMediaPeriodId(this.id, childMediaPeriodId);
                if (mediaPeriodId == null) {
                    return false;
                }
            }
            int windowIndex = CompositeMediaSource.this.getWindowIndexForChildWindowIndex(this.id, childWindowIndex);
            if (this.eventDispatcher.windowIndex != windowIndex || !Util.areEqual(this.eventDispatcher.mediaPeriodId, mediaPeriodId)) {
                this.eventDispatcher = CompositeMediaSource.this.createEventDispatcher(windowIndex, mediaPeriodId, 0);
            }
            return true;
        }

        private MediaLoadData maybeUpdateMediaLoadData(MediaLoadData mediaLoadData) {
            MediaLoadData mediaLoadData2 = mediaLoadData;
            long mediaStartTimeMs = CompositeMediaSource.this.getMediaTimeForChildMediaTime(this.id, mediaLoadData2.mediaStartTimeMs);
            long mediaEndTimeMs = CompositeMediaSource.this.getMediaTimeForChildMediaTime(this.id, mediaLoadData2.mediaEndTimeMs);
            if (mediaStartTimeMs == mediaLoadData2.mediaStartTimeMs && mediaEndTimeMs == mediaLoadData2.mediaEndTimeMs) {
                return mediaLoadData2;
            }
            MediaLoadData mediaLoadData3 = new MediaLoadData(mediaLoadData2.dataType, mediaLoadData2.trackType, mediaLoadData2.trackFormat, mediaLoadData2.trackSelectionReason, mediaLoadData2.trackSelectionData, mediaStartTimeMs, mediaEndTimeMs);
            return mediaLoadData3;
        }
    }

    private static final class MediaSourceAndListener {
        public final MediaSourceEventListener eventListener;
        public final SourceInfoRefreshListener listener;
        public final MediaSource mediaSource;

        public MediaSourceAndListener(MediaSource mediaSource2, SourceInfoRefreshListener listener2, MediaSourceEventListener eventListener2) {
            this.mediaSource = mediaSource2;
            this.listener = listener2;
            this.eventListener = eventListener2;
        }
    }

    /* access modifiers changed from: protected */
    public abstract void onChildSourceInfoRefreshed(T t, MediaSource mediaSource, Timeline timeline, @Nullable Object obj);

    protected CompositeMediaSource() {
    }

    @CallSuper
    public void prepareSourceInternal(ExoPlayer player2, boolean isTopLevelSource, @Nullable TransferListener mediaTransferListener2) {
        this.player = player2;
        this.mediaTransferListener = mediaTransferListener2;
        this.eventHandler = new Handler();
    }

    @CallSuper
    public void maybeThrowSourceInfoRefreshError() throws IOException {
        for (MediaSourceAndListener childSource : this.childSources.values()) {
            childSource.mediaSource.maybeThrowSourceInfoRefreshError();
        }
    }

    @CallSuper
    public void releaseSourceInternal() {
        for (MediaSourceAndListener childSource : this.childSources.values()) {
            childSource.mediaSource.releaseSource(childSource.listener);
            childSource.mediaSource.removeEventListener(childSource.eventListener);
        }
        this.childSources.clear();
        this.player = null;
    }

    /* access modifiers changed from: protected */
    public final void prepareChildSource(T id, MediaSource mediaSource) {
        Assertions.checkArgument(!this.childSources.containsKey(id));
        SourceInfoRefreshListener sourceListener = new SourceInfoRefreshListener(id) {
            private final /* synthetic */ Object f$1;

            {
                this.f$1 = r2;
            }

            public final void onSourceInfoRefreshed(MediaSource mediaSource, Timeline timeline, Object obj) {
                CompositeMediaSource.this.onChildSourceInfoRefreshed(this.f$1, mediaSource, timeline, obj);
            }
        };
        MediaSourceEventListener eventListener = new ForwardingEventListener(id);
        this.childSources.put(id, new MediaSourceAndListener(mediaSource, sourceListener, eventListener));
        mediaSource.addEventListener((Handler) Assertions.checkNotNull(this.eventHandler), eventListener);
        mediaSource.prepareSource((ExoPlayer) Assertions.checkNotNull(this.player), false, sourceListener, this.mediaTransferListener);
    }

    /* access modifiers changed from: protected */
    public final void releaseChildSource(T id) {
        MediaSourceAndListener removedChild = (MediaSourceAndListener) Assertions.checkNotNull(this.childSources.remove(id));
        removedChild.mediaSource.releaseSource(removedChild.listener);
        removedChild.mediaSource.removeEventListener(removedChild.eventListener);
    }

    /* access modifiers changed from: protected */
    public int getWindowIndexForChildWindowIndex(T t, int windowIndex) {
        return windowIndex;
    }

    /* access modifiers changed from: protected */
    @Nullable
    public MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(T t, MediaPeriodId mediaPeriodId) {
        return mediaPeriodId;
    }

    /* access modifiers changed from: protected */
    public long getMediaTimeForChildMediaTime(@Nullable T t, long mediaTimeMs) {
        return mediaTimeMs;
    }
}
