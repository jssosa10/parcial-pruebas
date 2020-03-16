package com.google.android.exoplayer2.source;

import android.os.Handler;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource.MediaPeriodId;
import com.google.android.exoplayer2.source.MediaSource.SourceInfoRefreshListener;
import com.google.android.exoplayer2.source.MediaSourceEventListener.EventDispatcher;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Assertions;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class BaseMediaSource implements MediaSource {
    private final EventDispatcher eventDispatcher = new EventDispatcher();
    @Nullable
    private Object manifest;
    @Nullable
    private ExoPlayer player;
    private final ArrayList<SourceInfoRefreshListener> sourceInfoListeners = new ArrayList<>(1);
    @Nullable
    private Timeline timeline;

    /* access modifiers changed from: protected */
    public abstract void prepareSourceInternal(ExoPlayer exoPlayer, boolean z, @Nullable TransferListener transferListener);

    /* access modifiers changed from: protected */
    public abstract void releaseSourceInternal();

    /* access modifiers changed from: protected */
    public final void refreshSourceInfo(Timeline timeline2, @Nullable Object manifest2) {
        this.timeline = timeline2;
        this.manifest = manifest2;
        Iterator it = this.sourceInfoListeners.iterator();
        while (it.hasNext()) {
            ((SourceInfoRefreshListener) it.next()).onSourceInfoRefreshed(this, timeline2, manifest2);
        }
    }

    /* access modifiers changed from: protected */
    public final EventDispatcher createEventDispatcher(@Nullable MediaPeriodId mediaPeriodId) {
        return this.eventDispatcher.withParameters(0, mediaPeriodId, 0);
    }

    /* access modifiers changed from: protected */
    public final EventDispatcher createEventDispatcher(MediaPeriodId mediaPeriodId, long mediaTimeOffsetMs) {
        Assertions.checkArgument(mediaPeriodId != null);
        return this.eventDispatcher.withParameters(0, mediaPeriodId, mediaTimeOffsetMs);
    }

    /* access modifiers changed from: protected */
    public final EventDispatcher createEventDispatcher(int windowIndex, @Nullable MediaPeriodId mediaPeriodId, long mediaTimeOffsetMs) {
        return this.eventDispatcher.withParameters(windowIndex, mediaPeriodId, mediaTimeOffsetMs);
    }

    public final void addEventListener(Handler handler, MediaSourceEventListener eventListener) {
        this.eventDispatcher.addEventListener(handler, eventListener);
    }

    public final void removeEventListener(MediaSourceEventListener eventListener) {
        this.eventDispatcher.removeEventListener(eventListener);
    }

    public final void prepareSource(ExoPlayer player2, boolean isTopLevelSource, SourceInfoRefreshListener listener) {
        prepareSource(player2, isTopLevelSource, listener, null);
    }

    public final void prepareSource(ExoPlayer player2, boolean isTopLevelSource, SourceInfoRefreshListener listener, @Nullable TransferListener mediaTransferListener) {
        ExoPlayer exoPlayer = this.player;
        Assertions.checkArgument(exoPlayer == null || exoPlayer == player2);
        this.sourceInfoListeners.add(listener);
        if (this.player == null) {
            this.player = player2;
            prepareSourceInternal(player2, isTopLevelSource, mediaTransferListener);
            return;
        }
        Timeline timeline2 = this.timeline;
        if (timeline2 != null) {
            listener.onSourceInfoRefreshed(this, timeline2, this.manifest);
        }
    }

    public final void releaseSource(SourceInfoRefreshListener listener) {
        this.sourceInfoListeners.remove(listener);
        if (this.sourceInfoListeners.isEmpty()) {
            this.player = null;
            this.timeline = null;
            this.manifest = null;
            releaseSourceInternal();
        }
    }
}
