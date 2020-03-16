package com.google.android.exoplayer2;

import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import com.google.android.exoplayer2.DefaultMediaClock.PlaybackParameterListener;
import com.google.android.exoplayer2.PlayerMessage.Sender;
import com.google.android.exoplayer2.Timeline.Period;
import com.google.android.exoplayer2.Timeline.Window;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSource.MediaPeriodId;
import com.google.android.exoplayer2.source.MediaSource.SourceInfoRefreshListener;
import com.google.android.exoplayer2.source.SampleStream;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector.InvalidationListener;
import com.google.android.exoplayer2.trackselection.TrackSelectorResult;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.HandlerWrapper;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.TraceUtil;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

final class ExoPlayerImplInternal implements Callback, MediaPeriod.Callback, InvalidationListener, SourceInfoRefreshListener, PlaybackParameterListener, Sender {
    private static final int IDLE_INTERVAL_MS = 1000;
    private static final int MSG_DO_SOME_WORK = 2;
    public static final int MSG_ERROR = 2;
    private static final int MSG_PERIOD_PREPARED = 9;
    public static final int MSG_PLAYBACK_INFO_CHANGED = 0;
    public static final int MSG_PLAYBACK_PARAMETERS_CHANGED = 1;
    private static final int MSG_PLAYBACK_PARAMETERS_CHANGED_INTERNAL = 16;
    private static final int MSG_PREPARE = 0;
    private static final int MSG_REFRESH_SOURCE_INFO = 8;
    private static final int MSG_RELEASE = 7;
    private static final int MSG_SEEK_TO = 3;
    private static final int MSG_SEND_MESSAGE = 14;
    private static final int MSG_SEND_MESSAGE_TO_TARGET_THREAD = 15;
    private static final int MSG_SET_PLAYBACK_PARAMETERS = 4;
    private static final int MSG_SET_PLAY_WHEN_READY = 1;
    private static final int MSG_SET_REPEAT_MODE = 12;
    private static final int MSG_SET_SEEK_PARAMETERS = 5;
    private static final int MSG_SET_SHUFFLE_ENABLED = 13;
    private static final int MSG_SOURCE_CONTINUE_LOADING_REQUESTED = 10;
    private static final int MSG_STOP = 6;
    private static final int MSG_TRACK_SELECTION_INVALIDATED = 11;
    private static final int PREPARING_SOURCE_INTERVAL_MS = 10;
    private static final int RENDERING_INTERVAL_MS = 10;
    private static final String TAG = "ExoPlayerImplInternal";
    private final long backBufferDurationUs;
    private final BandwidthMeter bandwidthMeter;
    private final Clock clock;
    private final TrackSelectorResult emptyTrackSelectorResult;
    private Renderer[] enabledRenderers;
    private final Handler eventHandler;
    private final HandlerWrapper handler;
    private final HandlerThread internalPlaybackThread;
    private final LoadControl loadControl;
    private final DefaultMediaClock mediaClock;
    private MediaSource mediaSource;
    private int nextPendingMessageIndex;
    private SeekPosition pendingInitialSeekPosition;
    private final ArrayList<PendingMessageInfo> pendingMessages;
    private int pendingPrepareCount;
    private final Period period;
    private boolean playWhenReady;
    private PlaybackInfo playbackInfo;
    private final PlaybackInfoUpdate playbackInfoUpdate;
    private final ExoPlayer player;
    private final MediaPeriodQueue queue = new MediaPeriodQueue();
    private boolean rebuffering;
    private boolean released;
    private final RendererCapabilities[] rendererCapabilities;
    private long rendererPositionUs;
    private final Renderer[] renderers;
    private int repeatMode;
    private final boolean retainBackBufferFromKeyframe;
    private SeekParameters seekParameters;
    private boolean shuffleModeEnabled;
    private final TrackSelector trackSelector;
    private final Window window;

    private static final class MediaSourceRefreshInfo {
        public final Object manifest;
        public final MediaSource source;
        public final Timeline timeline;

        public MediaSourceRefreshInfo(MediaSource source2, Timeline timeline2, Object manifest2) {
            this.source = source2;
            this.timeline = timeline2;
            this.manifest = manifest2;
        }
    }

    private static final class PendingMessageInfo implements Comparable<PendingMessageInfo> {
        public final PlayerMessage message;
        public int resolvedPeriodIndex;
        public long resolvedPeriodTimeUs;
        @Nullable
        public Object resolvedPeriodUid;

        public PendingMessageInfo(PlayerMessage message2) {
            this.message = message2;
        }

        public void setResolvedPosition(int periodIndex, long periodTimeUs, Object periodUid) {
            this.resolvedPeriodIndex = periodIndex;
            this.resolvedPeriodTimeUs = periodTimeUs;
            this.resolvedPeriodUid = periodUid;
        }

        public int compareTo(@NonNull PendingMessageInfo other) {
            int i = 1;
            if ((this.resolvedPeriodUid == null) != (other.resolvedPeriodUid == null)) {
                if (this.resolvedPeriodUid != null) {
                    i = -1;
                }
                return i;
            } else if (this.resolvedPeriodUid == null) {
                return 0;
            } else {
                int comparePeriodIndex = this.resolvedPeriodIndex - other.resolvedPeriodIndex;
                if (comparePeriodIndex != 0) {
                    return comparePeriodIndex;
                }
                return Util.compareLong(this.resolvedPeriodTimeUs, other.resolvedPeriodTimeUs);
            }
        }
    }

    private static final class PlaybackInfoUpdate {
        /* access modifiers changed from: private */
        public int discontinuityReason;
        private PlaybackInfo lastPlaybackInfo;
        /* access modifiers changed from: private */
        public int operationAcks;
        /* access modifiers changed from: private */
        public boolean positionDiscontinuity;

        private PlaybackInfoUpdate() {
        }

        public boolean hasPendingUpdate(PlaybackInfo playbackInfo) {
            return playbackInfo != this.lastPlaybackInfo || this.operationAcks > 0 || this.positionDiscontinuity;
        }

        public void reset(PlaybackInfo playbackInfo) {
            this.lastPlaybackInfo = playbackInfo;
            this.operationAcks = 0;
            this.positionDiscontinuity = false;
        }

        public void incrementPendingOperationAcks(int operationAcks2) {
            this.operationAcks += operationAcks2;
        }

        public void setPositionDiscontinuity(int discontinuityReason2) {
            boolean z = true;
            if (!this.positionDiscontinuity || this.discontinuityReason == 4) {
                this.positionDiscontinuity = true;
                this.discontinuityReason = discontinuityReason2;
                return;
            }
            if (discontinuityReason2 != 4) {
                z = false;
            }
            Assertions.checkArgument(z);
        }
    }

    private static final class SeekPosition {
        public final Timeline timeline;
        public final int windowIndex;
        public final long windowPositionUs;

        public SeekPosition(Timeline timeline2, int windowIndex2, long windowPositionUs2) {
            this.timeline = timeline2;
            this.windowIndex = windowIndex2;
            this.windowPositionUs = windowPositionUs2;
        }
    }

    public ExoPlayerImplInternal(Renderer[] renderers2, TrackSelector trackSelector2, TrackSelectorResult emptyTrackSelectorResult2, LoadControl loadControl2, BandwidthMeter bandwidthMeter2, boolean playWhenReady2, int repeatMode2, boolean shuffleModeEnabled2, Handler eventHandler2, ExoPlayer player2, Clock clock2) {
        this.renderers = renderers2;
        this.trackSelector = trackSelector2;
        this.emptyTrackSelectorResult = emptyTrackSelectorResult2;
        this.loadControl = loadControl2;
        this.bandwidthMeter = bandwidthMeter2;
        this.playWhenReady = playWhenReady2;
        this.repeatMode = repeatMode2;
        this.shuffleModeEnabled = shuffleModeEnabled2;
        this.eventHandler = eventHandler2;
        this.player = player2;
        this.clock = clock2;
        this.backBufferDurationUs = loadControl2.getBackBufferDurationUs();
        this.retainBackBufferFromKeyframe = loadControl2.retainBackBufferFromKeyframe();
        this.seekParameters = SeekParameters.DEFAULT;
        this.playbackInfo = PlaybackInfo.createDummy(C.TIME_UNSET, emptyTrackSelectorResult2);
        this.playbackInfoUpdate = new PlaybackInfoUpdate();
        this.rendererCapabilities = new RendererCapabilities[renderers2.length];
        for (int i = 0; i < renderers2.length; i++) {
            renderers2[i].setIndex(i);
            this.rendererCapabilities[i] = renderers2[i].getCapabilities();
        }
        this.mediaClock = new DefaultMediaClock(this, clock2);
        this.pendingMessages = new ArrayList<>();
        this.enabledRenderers = new Renderer[0];
        this.window = new Window();
        this.period = new Period();
        trackSelector2.init(this, bandwidthMeter2);
        this.internalPlaybackThread = new HandlerThread("ExoPlayerImplInternal:Handler", -16);
        this.internalPlaybackThread.start();
        this.handler = clock2.createHandler(this.internalPlaybackThread.getLooper(), this);
    }

    public void prepare(MediaSource mediaSource2, boolean resetPosition, boolean resetState) {
        this.handler.obtainMessage(0, resetPosition, resetState, mediaSource2).sendToTarget();
    }

    public void setPlayWhenReady(boolean playWhenReady2) {
        this.handler.obtainMessage(1, playWhenReady2, 0).sendToTarget();
    }

    public void setRepeatMode(int repeatMode2) {
        this.handler.obtainMessage(12, repeatMode2, 0).sendToTarget();
    }

    public void setShuffleModeEnabled(boolean shuffleModeEnabled2) {
        this.handler.obtainMessage(13, shuffleModeEnabled2, 0).sendToTarget();
    }

    public void seekTo(Timeline timeline, int windowIndex, long positionUs) {
        this.handler.obtainMessage(3, new SeekPosition(timeline, windowIndex, positionUs)).sendToTarget();
    }

    public void setPlaybackParameters(PlaybackParameters playbackParameters) {
        this.handler.obtainMessage(4, playbackParameters).sendToTarget();
    }

    public void setSeekParameters(SeekParameters seekParameters2) {
        this.handler.obtainMessage(5, seekParameters2).sendToTarget();
    }

    public void stop(boolean reset) {
        this.handler.obtainMessage(6, reset, 0).sendToTarget();
    }

    public synchronized void sendMessage(PlayerMessage message) {
        if (this.released) {
            Log.w(TAG, "Ignoring messages sent after release.");
            message.markAsProcessed(false);
            return;
        }
        this.handler.obtainMessage(14, message).sendToTarget();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0023, code lost:
        return;
     */
    public synchronized void release() {
        if (!this.released) {
            this.handler.sendEmptyMessage(7);
            boolean wasInterrupted = false;
            while (!this.released) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    wasInterrupted = true;
                }
            }
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public Looper getPlaybackLooper() {
        return this.internalPlaybackThread.getLooper();
    }

    public void onSourceInfoRefreshed(MediaSource source, Timeline timeline, Object manifest) {
        this.handler.obtainMessage(8, new MediaSourceRefreshInfo(source, timeline, manifest)).sendToTarget();
    }

    public void onPrepared(MediaPeriod source) {
        this.handler.obtainMessage(9, source).sendToTarget();
    }

    public void onContinueLoadingRequested(MediaPeriod source) {
        this.handler.obtainMessage(10, source).sendToTarget();
    }

    public void onTrackSelectionsInvalidated() {
        this.handler.sendEmptyMessage(11);
    }

    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        this.handler.obtainMessage(16, playbackParameters).sendToTarget();
    }

    public boolean handleMessage(Message msg) {
        try {
            switch (msg.what) {
                case 0:
                    prepareInternal((MediaSource) msg.obj, msg.arg1 != 0, msg.arg2 != 0);
                    break;
                case 1:
                    setPlayWhenReadyInternal(msg.arg1 != 0);
                    break;
                case 2:
                    doSomeWork();
                    break;
                case 3:
                    seekToInternal((SeekPosition) msg.obj);
                    break;
                case 4:
                    setPlaybackParametersInternal((PlaybackParameters) msg.obj);
                    break;
                case 5:
                    setSeekParametersInternal((SeekParameters) msg.obj);
                    break;
                case 6:
                    stopInternal(msg.arg1 != 0, true);
                    break;
                case 7:
                    releaseInternal();
                    return true;
                case 8:
                    handleSourceInfoRefreshed((MediaSourceRefreshInfo) msg.obj);
                    break;
                case 9:
                    handlePeriodPrepared((MediaPeriod) msg.obj);
                    break;
                case 10:
                    handleContinueLoadingRequested((MediaPeriod) msg.obj);
                    break;
                case 11:
                    reselectTracksInternal();
                    break;
                case 12:
                    setRepeatModeInternal(msg.arg1);
                    break;
                case 13:
                    setShuffleModeEnabledInternal(msg.arg1 != 0);
                    break;
                case 14:
                    sendMessageInternal((PlayerMessage) msg.obj);
                    break;
                case 15:
                    sendMessageToTargetThread((PlayerMessage) msg.obj);
                    break;
                case 16:
                    handlePlaybackParameters((PlaybackParameters) msg.obj);
                    break;
                default:
                    return false;
            }
            maybeNotifyPlaybackInfoChanged();
        } catch (ExoPlaybackException e) {
            Log.e(TAG, "Playback error.", e);
            stopInternal(false, false);
            this.eventHandler.obtainMessage(2, e).sendToTarget();
            maybeNotifyPlaybackInfoChanged();
        } catch (IOException e2) {
            Log.e(TAG, "Source error.", e2);
            stopInternal(false, false);
            this.eventHandler.obtainMessage(2, ExoPlaybackException.createForSource(e2)).sendToTarget();
            maybeNotifyPlaybackInfoChanged();
        } catch (RuntimeException e3) {
            Log.e(TAG, "Internal runtime error.", e3);
            stopInternal(false, false);
            this.eventHandler.obtainMessage(2, ExoPlaybackException.createForUnexpected(e3)).sendToTarget();
            maybeNotifyPlaybackInfoChanged();
        }
        return true;
    }

    private void setState(int state) {
        if (this.playbackInfo.playbackState != state) {
            this.playbackInfo = this.playbackInfo.copyWithPlaybackState(state);
        }
    }

    private void setIsLoading(boolean isLoading) {
        if (this.playbackInfo.isLoading != isLoading) {
            this.playbackInfo = this.playbackInfo.copyWithIsLoading(isLoading);
        }
    }

    private void maybeNotifyPlaybackInfoChanged() {
        if (this.playbackInfoUpdate.hasPendingUpdate(this.playbackInfo)) {
            this.eventHandler.obtainMessage(0, this.playbackInfoUpdate.operationAcks, this.playbackInfoUpdate.positionDiscontinuity ? this.playbackInfoUpdate.discontinuityReason : -1, this.playbackInfo).sendToTarget();
            this.playbackInfoUpdate.reset(this.playbackInfo);
        }
    }

    private void prepareInternal(MediaSource mediaSource2, boolean resetPosition, boolean resetState) {
        this.pendingPrepareCount++;
        resetInternal(true, resetPosition, resetState);
        this.loadControl.onPrepared();
        this.mediaSource = mediaSource2;
        setState(2);
        mediaSource2.prepareSource(this.player, true, this, this.bandwidthMeter.getTransferListener());
        this.handler.sendEmptyMessage(2);
    }

    private void setPlayWhenReadyInternal(boolean playWhenReady2) throws ExoPlaybackException {
        this.rebuffering = false;
        this.playWhenReady = playWhenReady2;
        if (!playWhenReady2) {
            stopRenderers();
            updatePlaybackPositions();
        } else if (this.playbackInfo.playbackState == 3) {
            startRenderers();
            this.handler.sendEmptyMessage(2);
        } else if (this.playbackInfo.playbackState == 2) {
            this.handler.sendEmptyMessage(2);
        }
    }

    private void setRepeatModeInternal(int repeatMode2) throws ExoPlaybackException {
        this.repeatMode = repeatMode2;
        if (!this.queue.updateRepeatMode(repeatMode2)) {
            seekToCurrentPosition(true);
        }
        handleLoadingMediaPeriodChanged(false);
    }

    private void setShuffleModeEnabledInternal(boolean shuffleModeEnabled2) throws ExoPlaybackException {
        this.shuffleModeEnabled = shuffleModeEnabled2;
        if (!this.queue.updateShuffleModeEnabled(shuffleModeEnabled2)) {
            seekToCurrentPosition(true);
        }
        handleLoadingMediaPeriodChanged(false);
    }

    private void seekToCurrentPosition(boolean sendDiscontinuity) throws ExoPlaybackException {
        MediaPeriodId periodId = this.queue.getPlayingPeriod().f18info.id;
        long newPositionUs = seekToPeriodPosition(periodId, this.playbackInfo.positionUs, true);
        if (newPositionUs != this.playbackInfo.positionUs) {
            PlaybackInfo playbackInfo2 = this.playbackInfo;
            this.playbackInfo = playbackInfo2.copyWithNewPosition(periodId, newPositionUs, playbackInfo2.contentPositionUs, getTotalBufferedDurationUs());
            if (sendDiscontinuity) {
                this.playbackInfoUpdate.setPositionDiscontinuity(4);
            }
        }
    }

    private void startRenderers() throws ExoPlaybackException {
        this.rebuffering = false;
        this.mediaClock.start();
        for (Renderer renderer : this.enabledRenderers) {
            renderer.start();
        }
    }

    private void stopRenderers() throws ExoPlaybackException {
        this.mediaClock.stop();
        for (Renderer renderer : this.enabledRenderers) {
            ensureStopped(renderer);
        }
    }

    private void updatePlaybackPositions() throws ExoPlaybackException {
        if (this.queue.hasPlayingPeriod()) {
            MediaPeriodHolder playingPeriodHolder = this.queue.getPlayingPeriod();
            long periodPositionUs = playingPeriodHolder.mediaPeriod.readDiscontinuity();
            if (periodPositionUs != C.TIME_UNSET) {
                resetRendererPosition(periodPositionUs);
                if (periodPositionUs != this.playbackInfo.positionUs) {
                    PlaybackInfo playbackInfo2 = this.playbackInfo;
                    this.playbackInfo = playbackInfo2.copyWithNewPosition(playbackInfo2.periodId, periodPositionUs, this.playbackInfo.contentPositionUs, getTotalBufferedDurationUs());
                    this.playbackInfoUpdate.setPositionDiscontinuity(4);
                }
            } else {
                this.rendererPositionUs = this.mediaClock.syncAndGetPositionUs();
                long periodPositionUs2 = playingPeriodHolder.toPeriodTime(this.rendererPositionUs);
                maybeTriggerPendingMessages(this.playbackInfo.positionUs, periodPositionUs2);
                this.playbackInfo.positionUs = periodPositionUs2;
            }
            MediaPeriodHolder loadingPeriod = this.queue.getLoadingPeriod();
            this.playbackInfo.bufferedPositionUs = loadingPeriod.getBufferedPositionUs();
            this.playbackInfo.totalBufferedDurationUs = getTotalBufferedDurationUs();
        }
    }

    private void doSomeWork() throws ExoPlaybackException, IOException {
        Renderer[] rendererArr;
        long operationStartTimeMs = this.clock.uptimeMillis();
        updatePeriods();
        if (!this.queue.hasPlayingPeriod()) {
            maybeThrowPeriodPrepareError();
            scheduleNextWork(operationStartTimeMs, 10);
            return;
        }
        MediaPeriodHolder playingPeriodHolder = this.queue.getPlayingPeriod();
        TraceUtil.beginSection("doSomeWork");
        updatePlaybackPositions();
        long rendererPositionElapsedRealtimeUs = SystemClock.elapsedRealtime() * 1000;
        playingPeriodHolder.mediaPeriod.discardBuffer(this.playbackInfo.positionUs - this.backBufferDurationUs, this.retainBackBufferFromKeyframe);
        boolean renderersReadyOrEnded = true;
        boolean renderersEnded = true;
        for (Renderer renderer : this.enabledRenderers) {
            renderer.render(this.rendererPositionUs, rendererPositionElapsedRealtimeUs);
            boolean z = true;
            renderersEnded = renderersEnded && renderer.isEnded();
            boolean rendererReadyOrEnded = renderer.isReady() || renderer.isEnded() || rendererWaitingForNextStream(renderer);
            if (!rendererReadyOrEnded) {
                renderer.maybeThrowStreamError();
            }
            if (!renderersReadyOrEnded || !rendererReadyOrEnded) {
                z = false;
            }
            renderersReadyOrEnded = z;
        }
        if (!renderersReadyOrEnded) {
            maybeThrowPeriodPrepareError();
        }
        long playingPeriodDurationUs = playingPeriodHolder.f18info.durationUs;
        if (renderersEnded && ((playingPeriodDurationUs == C.TIME_UNSET || playingPeriodDurationUs <= this.playbackInfo.positionUs) && playingPeriodHolder.f18info.isFinal)) {
            setState(4);
            stopRenderers();
        } else if (this.playbackInfo.playbackState == 2 && shouldTransitionToReadyState(renderersReadyOrEnded)) {
            setState(3);
            if (this.playWhenReady) {
                startRenderers();
            }
        } else if (this.playbackInfo.playbackState == 3 && (this.enabledRenderers.length != 0 ? !renderersReadyOrEnded : !isTimelineReady())) {
            this.rebuffering = this.playWhenReady;
            setState(2);
            stopRenderers();
        }
        if (this.playbackInfo.playbackState == 2) {
            for (Renderer renderer2 : this.enabledRenderers) {
                renderer2.maybeThrowStreamError();
            }
        }
        if ((this.playWhenReady && this.playbackInfo.playbackState == 3) || this.playbackInfo.playbackState == 2) {
            scheduleNextWork(operationStartTimeMs, 10);
        } else if (this.enabledRenderers.length == 0 || this.playbackInfo.playbackState == 4) {
            this.handler.removeMessages(2);
        } else {
            scheduleNextWork(operationStartTimeMs, 1000);
        }
        TraceUtil.endSection();
    }

    private void scheduleNextWork(long thisOperationStartTimeMs, long intervalMs) {
        this.handler.removeMessages(2);
        this.handler.sendEmptyMessageAtTime(2, thisOperationStartTimeMs + intervalMs);
    }

    /* JADX WARNING: Removed duplicated region for block: B:61:0x00fe  */
    /* JADX WARNING: Removed duplicated region for block: B:68:0x011c  */
    private void seekToInternal(SeekPosition seekPosition) throws ExoPlaybackException {
        long periodPositionUs;
        long contentPositionUs;
        MediaPeriodId periodId;
        boolean seekPositionAdjusted;
        SeekPosition seekPosition2 = seekPosition;
        this.playbackInfoUpdate.incrementPendingOperationAcks(1);
        Pair<Object, Long> resolvedSeekPosition = resolveSeekPosition(seekPosition2, true);
        if (resolvedSeekPosition == null) {
            MediaPeriodId periodId2 = this.playbackInfo.getDummyFirstMediaPeriodId(this.shuffleModeEnabled, this.window);
            contentPositionUs = C.TIME_UNSET;
            periodId = periodId2;
            seekPositionAdjusted = true;
            periodPositionUs = -9223372036854775807L;
        } else {
            Object periodUid = resolvedSeekPosition.first;
            contentPositionUs = ((Long) resolvedSeekPosition.second).longValue();
            periodId = this.queue.resolveMediaPeriodIdForAds(periodUid, contentPositionUs);
            if (periodId.isAd()) {
                periodPositionUs = 0;
                seekPositionAdjusted = true;
            } else {
                periodPositionUs = ((Long) resolvedSeekPosition.second).longValue();
                seekPositionAdjusted = seekPosition2.windowPositionUs == C.TIME_UNSET;
            }
        }
        try {
            if (this.mediaSource == null) {
            } else if (this.pendingPrepareCount > 0) {
                Pair pair = resolvedSeekPosition;
            } else if (periodPositionUs == C.TIME_UNSET) {
                try {
                    setState(4);
                    resetInternal(false, true, false);
                    Pair pair2 = resolvedSeekPosition;
                    this.playbackInfo = this.playbackInfo.copyWithNewPosition(periodId, periodPositionUs, contentPositionUs, getTotalBufferedDurationUs());
                    if (seekPositionAdjusted) {
                        this.playbackInfoUpdate.setPositionDiscontinuity(2);
                    }
                } catch (Throwable th) {
                    th = th;
                    Pair pair3 = resolvedSeekPosition;
                    this.playbackInfo = this.playbackInfo.copyWithNewPosition(periodId, periodPositionUs, contentPositionUs, getTotalBufferedDurationUs());
                    if (seekPositionAdjusted) {
                    }
                    throw th;
                }
            } else {
                long newPeriodPositionUs = periodPositionUs;
                if (periodId.equals(this.playbackInfo.periodId)) {
                    MediaPeriodHolder playingPeriodHolder = this.queue.getPlayingPeriod();
                    if (!(playingPeriodHolder == null || newPeriodPositionUs == 0)) {
                        newPeriodPositionUs = playingPeriodHolder.mediaPeriod.getAdjustedSeekPositionUs(newPeriodPositionUs, this.seekParameters);
                    }
                    Pair pair4 = resolvedSeekPosition;
                    try {
                        if (C.usToMs(newPeriodPositionUs) == C.usToMs(this.playbackInfo.positionUs)) {
                            MediaPeriodHolder mediaPeriodHolder = playingPeriodHolder;
                            this.playbackInfo = this.playbackInfo.copyWithNewPosition(periodId, this.playbackInfo.positionUs, contentPositionUs, getTotalBufferedDurationUs());
                            if (seekPositionAdjusted) {
                                this.playbackInfoUpdate.setPositionDiscontinuity(2);
                            }
                            return;
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        this.playbackInfo = this.playbackInfo.copyWithNewPosition(periodId, periodPositionUs, contentPositionUs, getTotalBufferedDurationUs());
                        if (seekPositionAdjusted) {
                            this.playbackInfoUpdate.setPositionDiscontinuity(2);
                        }
                        throw th;
                    }
                }
                long newPeriodPositionUs2 = seekToPeriodPosition(periodId, newPeriodPositionUs);
                seekPositionAdjusted |= periodPositionUs != newPeriodPositionUs2;
                periodPositionUs = newPeriodPositionUs2;
                this.playbackInfo = this.playbackInfo.copyWithNewPosition(periodId, periodPositionUs, contentPositionUs, getTotalBufferedDurationUs());
                if (seekPositionAdjusted) {
                }
            }
            this.pendingInitialSeekPosition = seekPosition2;
            this.playbackInfo = this.playbackInfo.copyWithNewPosition(periodId, periodPositionUs, contentPositionUs, getTotalBufferedDurationUs());
            if (seekPositionAdjusted) {
            }
        } catch (Throwable th3) {
            th = th3;
            Pair pair5 = resolvedSeekPosition;
            this.playbackInfo = this.playbackInfo.copyWithNewPosition(periodId, periodPositionUs, contentPositionUs, getTotalBufferedDurationUs());
            if (seekPositionAdjusted) {
            }
            throw th;
        }
    }

    private long seekToPeriodPosition(MediaPeriodId periodId, long periodPositionUs) throws ExoPlaybackException {
        return seekToPeriodPosition(periodId, periodPositionUs, this.queue.getPlayingPeriod() != this.queue.getReadingPeriod());
    }

    private long seekToPeriodPosition(MediaPeriodId periodId, long periodPositionUs, boolean forceDisableRenderers) throws ExoPlaybackException {
        stopRenderers();
        this.rebuffering = false;
        setState(2);
        MediaPeriodHolder oldPlayingPeriodHolder = this.queue.getPlayingPeriod();
        MediaPeriodHolder newPlayingPeriodHolder = oldPlayingPeriodHolder;
        while (true) {
            if (newPlayingPeriodHolder != null) {
                if (periodId.equals(newPlayingPeriodHolder.f18info.id) && newPlayingPeriodHolder.prepared) {
                    this.queue.removeAfter(newPlayingPeriodHolder);
                    break;
                }
                newPlayingPeriodHolder = this.queue.advancePlayingPeriod();
            } else {
                break;
            }
        }
        if (oldPlayingPeriodHolder != newPlayingPeriodHolder || forceDisableRenderers) {
            for (Renderer renderer : this.enabledRenderers) {
                disableRenderer(renderer);
            }
            this.enabledRenderers = new Renderer[0];
            oldPlayingPeriodHolder = null;
        }
        if (newPlayingPeriodHolder != null) {
            updatePlayingPeriodRenderers(oldPlayingPeriodHolder);
            if (newPlayingPeriodHolder.hasEnabledTracks) {
                periodPositionUs = newPlayingPeriodHolder.mediaPeriod.seekToUs(periodPositionUs);
                newPlayingPeriodHolder.mediaPeriod.discardBuffer(periodPositionUs - this.backBufferDurationUs, this.retainBackBufferFromKeyframe);
            }
            resetRendererPosition(periodPositionUs);
            maybeContinueLoading();
        } else {
            this.queue.clear(true);
            this.playbackInfo = this.playbackInfo.copyWithTrackInfo(TrackGroupArray.EMPTY, this.emptyTrackSelectorResult);
            resetRendererPosition(periodPositionUs);
        }
        handleLoadingMediaPeriodChanged(false);
        this.handler.sendEmptyMessage(2);
        return periodPositionUs;
    }

    private void resetRendererPosition(long periodPositionUs) throws ExoPlaybackException {
        long j;
        if (!this.queue.hasPlayingPeriod()) {
            j = periodPositionUs;
        } else {
            j = this.queue.getPlayingPeriod().toRendererTime(periodPositionUs);
        }
        this.rendererPositionUs = j;
        this.mediaClock.resetPosition(this.rendererPositionUs);
        for (Renderer renderer : this.enabledRenderers) {
            renderer.resetPosition(this.rendererPositionUs);
        }
    }

    private void setPlaybackParametersInternal(PlaybackParameters playbackParameters) {
        this.mediaClock.setPlaybackParameters(playbackParameters);
    }

    private void setSeekParametersInternal(SeekParameters seekParameters2) {
        this.seekParameters = seekParameters2;
    }

    private void stopInternal(boolean reset, boolean acknowledgeStop) {
        resetInternal(true, reset, reset);
        this.playbackInfoUpdate.incrementPendingOperationAcks(this.pendingPrepareCount + (acknowledgeStop));
        this.pendingPrepareCount = 0;
        this.loadControl.onStopped();
        setState(1);
    }

    private void releaseInternal() {
        resetInternal(true, true, true);
        this.loadControl.onReleased();
        setState(1);
        this.internalPlaybackThread.quit();
        synchronized (this) {
            this.released = true;
            notifyAll();
        }
    }

    private void resetInternal(boolean releaseMediaSource, boolean resetPosition, boolean resetState) {
        this.handler.removeMessages(2);
        this.rebuffering = false;
        this.mediaClock.stop();
        this.rendererPositionUs = 0;
        for (Renderer renderer : this.enabledRenderers) {
            try {
                disableRenderer(renderer);
            } catch (ExoPlaybackException | RuntimeException e) {
                Log.e(TAG, "Stop failed.", e);
            }
        }
        this.enabledRenderers = new Renderer[0];
        this.queue.clear(!resetPosition);
        setIsLoading(false);
        if (resetPosition) {
            this.pendingInitialSeekPosition = null;
        }
        if (resetState) {
            this.queue.setTimeline(Timeline.EMPTY);
            Iterator it = this.pendingMessages.iterator();
            while (it.hasNext()) {
                ((PendingMessageInfo) it.next()).message.markAsProcessed(false);
            }
            this.pendingMessages.clear();
            this.nextPendingMessageIndex = 0;
        }
        MediaPeriodId mediaPeriodId = resetPosition ? this.playbackInfo.getDummyFirstMediaPeriodId(this.shuffleModeEnabled, this.window) : this.playbackInfo.periodId;
        long j = C.TIME_UNSET;
        long startPositionUs = resetPosition ? -9223372036854775807L : this.playbackInfo.positionUs;
        if (!resetPosition) {
            j = this.playbackInfo.contentPositionUs;
        }
        PlaybackInfo playbackInfo2 = new PlaybackInfo(resetState ? Timeline.EMPTY : this.playbackInfo.timeline, resetState ? null : this.playbackInfo.manifest, mediaPeriodId, startPositionUs, j, this.playbackInfo.playbackState, false, resetState ? TrackGroupArray.EMPTY : this.playbackInfo.trackGroups, resetState ? this.emptyTrackSelectorResult : this.playbackInfo.trackSelectorResult, mediaPeriodId, startPositionUs, 0, startPositionUs);
        this.playbackInfo = playbackInfo2;
        if (releaseMediaSource) {
            MediaSource mediaSource2 = this.mediaSource;
            if (mediaSource2 != null) {
                mediaSource2.releaseSource(this);
                this.mediaSource = null;
            }
        }
    }

    private void sendMessageInternal(PlayerMessage message) throws ExoPlaybackException {
        if (message.getPositionMs() == C.TIME_UNSET) {
            sendMessageToTarget(message);
        } else if (this.mediaSource == null || this.pendingPrepareCount > 0) {
            this.pendingMessages.add(new PendingMessageInfo(message));
        } else {
            PendingMessageInfo pendingMessageInfo = new PendingMessageInfo(message);
            if (resolvePendingMessagePosition(pendingMessageInfo)) {
                this.pendingMessages.add(pendingMessageInfo);
                Collections.sort(this.pendingMessages);
                return;
            }
            message.markAsProcessed(false);
        }
    }

    private void sendMessageToTarget(PlayerMessage message) throws ExoPlaybackException {
        if (message.getHandler().getLooper() == this.handler.getLooper()) {
            deliverMessage(message);
            if (this.playbackInfo.playbackState == 3 || this.playbackInfo.playbackState == 2) {
                this.handler.sendEmptyMessage(2);
                return;
            }
            return;
        }
        this.handler.obtainMessage(15, message).sendToTarget();
    }

    private void sendMessageToTargetThread(PlayerMessage message) {
        message.getHandler().post(new Runnable(message) {
            private final /* synthetic */ PlayerMessage f$1;

            {
                this.f$1 = r2;
            }

            public final void run() {
                ExoPlayerImplInternal.lambda$sendMessageToTargetThread$0(ExoPlayerImplInternal.this, this.f$1);
            }
        });
    }

    public static /* synthetic */ void lambda$sendMessageToTargetThread$0(ExoPlayerImplInternal exoPlayerImplInternal, PlayerMessage message) {
        try {
            exoPlayerImplInternal.deliverMessage(message);
        } catch (ExoPlaybackException e) {
            Log.e(TAG, "Unexpected error delivering message on external thread.", e);
            throw new RuntimeException(e);
        }
    }

    private void deliverMessage(PlayerMessage message) throws ExoPlaybackException {
        if (!message.isCanceled()) {
            try {
                message.getTarget().handleMessage(message.getType(), message.getPayload());
            } finally {
                message.markAsProcessed(true);
            }
        }
    }

    private void resolvePendingMessagePositions() {
        for (int i = this.pendingMessages.size() - 1; i >= 0; i--) {
            if (!resolvePendingMessagePosition((PendingMessageInfo) this.pendingMessages.get(i))) {
                ((PendingMessageInfo) this.pendingMessages.get(i)).message.markAsProcessed(false);
                this.pendingMessages.remove(i);
            }
        }
        Collections.sort(this.pendingMessages);
    }

    private boolean resolvePendingMessagePosition(PendingMessageInfo pendingMessageInfo) {
        if (pendingMessageInfo.resolvedPeriodUid == null) {
            Pair<Object, Long> periodPosition = resolveSeekPosition(new SeekPosition(pendingMessageInfo.message.getTimeline(), pendingMessageInfo.message.getWindowIndex(), C.msToUs(pendingMessageInfo.message.getPositionMs())), false);
            if (periodPosition == null) {
                return false;
            }
            pendingMessageInfo.setResolvedPosition(this.playbackInfo.timeline.getIndexOfPeriod(periodPosition.first), ((Long) periodPosition.second).longValue(), periodPosition.first);
        } else {
            int index = this.playbackInfo.timeline.getIndexOfPeriod(pendingMessageInfo.resolvedPeriodUid);
            if (index == -1) {
                return false;
            }
            pendingMessageInfo.resolvedPeriodIndex = index;
        }
        return true;
    }

    private void maybeTriggerPendingMessages(long oldPeriodPositionUs, long newPeriodPositionUs) throws ExoPlaybackException {
        if (!this.pendingMessages.isEmpty() && !this.playbackInfo.periodId.isAd()) {
            if (this.playbackInfo.startPositionUs == oldPeriodPositionUs) {
                oldPeriodPositionUs--;
            }
            int currentPeriodIndex = this.playbackInfo.timeline.getIndexOfPeriod(this.playbackInfo.periodId.periodUid);
            int i = this.nextPendingMessageIndex;
            PendingMessageInfo previousInfo = i > 0 ? (PendingMessageInfo) this.pendingMessages.get(i - 1) : null;
            while (previousInfo != null && (previousInfo.resolvedPeriodIndex > currentPeriodIndex || (previousInfo.resolvedPeriodIndex == currentPeriodIndex && previousInfo.resolvedPeriodTimeUs > oldPeriodPositionUs))) {
                this.nextPendingMessageIndex--;
                int i2 = this.nextPendingMessageIndex;
                previousInfo = i2 > 0 ? (PendingMessageInfo) this.pendingMessages.get(i2 - 1) : null;
            }
            PendingMessageInfo nextInfo = this.nextPendingMessageIndex < this.pendingMessages.size() ? (PendingMessageInfo) this.pendingMessages.get(this.nextPendingMessageIndex) : null;
            while (nextInfo != null && nextInfo.resolvedPeriodUid != null && (nextInfo.resolvedPeriodIndex < currentPeriodIndex || (nextInfo.resolvedPeriodIndex == currentPeriodIndex && nextInfo.resolvedPeriodTimeUs <= oldPeriodPositionUs))) {
                this.nextPendingMessageIndex++;
                nextInfo = this.nextPendingMessageIndex < this.pendingMessages.size() ? (PendingMessageInfo) this.pendingMessages.get(this.nextPendingMessageIndex) : null;
            }
            while (nextInfo != null && nextInfo.resolvedPeriodUid != null && nextInfo.resolvedPeriodIndex == currentPeriodIndex && nextInfo.resolvedPeriodTimeUs > oldPeriodPositionUs && nextInfo.resolvedPeriodTimeUs <= newPeriodPositionUs) {
                sendMessageToTarget(nextInfo.message);
                if (nextInfo.message.getDeleteAfterDelivery() || nextInfo.message.isCanceled()) {
                    this.pendingMessages.remove(this.nextPendingMessageIndex);
                } else {
                    this.nextPendingMessageIndex++;
                }
                nextInfo = this.nextPendingMessageIndex < this.pendingMessages.size() ? (PendingMessageInfo) this.pendingMessages.get(this.nextPendingMessageIndex) : null;
            }
        }
    }

    private void ensureStopped(Renderer renderer) throws ExoPlaybackException {
        if (renderer.getState() == 2) {
            renderer.stop();
        }
    }

    private void disableRenderer(Renderer renderer) throws ExoPlaybackException {
        this.mediaClock.onRendererDisabled(renderer);
        ensureStopped(renderer);
        renderer.disable();
    }

    private void reselectTracksInternal() throws ExoPlaybackException {
        MediaPeriodHolder playingPeriodHolder;
        boolean selectionsChangedForReadPeriod;
        if (this.queue.hasPlayingPeriod()) {
            float playbackSpeed = this.mediaClock.getPlaybackParameters().speed;
            MediaPeriodHolder periodHolder = this.queue.getPlayingPeriod();
            MediaPeriodHolder readingPeriodHolder = this.queue.getReadingPeriod();
            boolean selectionsChangedForReadPeriod2 = true;
            while (true) {
                if (periodHolder == null) {
                    boolean z = selectionsChangedForReadPeriod2;
                    break;
                } else if (!periodHolder.prepared) {
                    boolean z2 = selectionsChangedForReadPeriod2;
                    break;
                } else if (periodHolder.selectTracks(playbackSpeed)) {
                    if (selectionsChangedForReadPeriod2) {
                        MediaPeriodHolder playingPeriodHolder2 = this.queue.getPlayingPeriod();
                        boolean[] streamResetFlags = new boolean[this.renderers.length];
                        long periodPositionUs = playingPeriodHolder2.applyTrackSelection(this.playbackInfo.positionUs, this.queue.removeAfter(playingPeriodHolder2), streamResetFlags);
                        if (this.playbackInfo.playbackState == 4 || periodPositionUs == this.playbackInfo.positionUs) {
                            playingPeriodHolder = playingPeriodHolder2;
                        } else {
                            PlaybackInfo playbackInfo2 = this.playbackInfo;
                            playingPeriodHolder = playingPeriodHolder2;
                            this.playbackInfo = playbackInfo2.copyWithNewPosition(playbackInfo2.periodId, periodPositionUs, this.playbackInfo.contentPositionUs, getTotalBufferedDurationUs());
                            this.playbackInfoUpdate.setPositionDiscontinuity(4);
                            resetRendererPosition(periodPositionUs);
                        }
                        int enabledRendererCount = 0;
                        boolean[] rendererWasEnabledFlags = new boolean[this.renderers.length];
                        int i = 0;
                        while (true) {
                            Renderer[] rendererArr = this.renderers;
                            if (i >= rendererArr.length) {
                                break;
                            }
                            Renderer renderer = rendererArr[i];
                            rendererWasEnabledFlags[i] = renderer.getState() != 0;
                            MediaPeriodHolder playingPeriodHolder3 = playingPeriodHolder;
                            SampleStream sampleStream = playingPeriodHolder3.sampleStreams[i];
                            if (sampleStream != null) {
                                enabledRendererCount++;
                            }
                            if (!rendererWasEnabledFlags[i]) {
                                selectionsChangedForReadPeriod = selectionsChangedForReadPeriod2;
                            } else if (sampleStream != renderer.getStream()) {
                                disableRenderer(renderer);
                                selectionsChangedForReadPeriod = selectionsChangedForReadPeriod2;
                            } else if (streamResetFlags[i]) {
                                selectionsChangedForReadPeriod = selectionsChangedForReadPeriod2;
                                renderer.resetPosition(this.rendererPositionUs);
                            } else {
                                selectionsChangedForReadPeriod = selectionsChangedForReadPeriod2;
                            }
                            i++;
                            playingPeriodHolder = playingPeriodHolder3;
                            selectionsChangedForReadPeriod2 = selectionsChangedForReadPeriod;
                        }
                        MediaPeriodHolder playingPeriodHolder4 = playingPeriodHolder;
                        this.playbackInfo = this.playbackInfo.copyWithTrackInfo(playingPeriodHolder4.trackGroups, playingPeriodHolder4.trackSelectorResult);
                        enableRenderers(rendererWasEnabledFlags, enabledRendererCount);
                    } else {
                        this.queue.removeAfter(periodHolder);
                        if (periodHolder.prepared) {
                            periodHolder.applyTrackSelection(Math.max(periodHolder.f18info.startPositionUs, periodHolder.toPeriodTime(this.rendererPositionUs)), false);
                        }
                    }
                    handleLoadingMediaPeriodChanged(true);
                    if (this.playbackInfo.playbackState != 4) {
                        maybeContinueLoading();
                        updatePlaybackPositions();
                        this.handler.sendEmptyMessage(2);
                    }
                    return;
                } else {
                    boolean selectionsChangedForReadPeriod3 = selectionsChangedForReadPeriod2;
                    if (periodHolder == readingPeriodHolder) {
                        selectionsChangedForReadPeriod2 = false;
                    } else {
                        selectionsChangedForReadPeriod2 = selectionsChangedForReadPeriod3;
                    }
                    periodHolder = periodHolder.next;
                }
            }
        }
    }

    private void updateTrackSelectionPlaybackSpeed(float playbackSpeed) {
        TrackSelection[] trackSelections;
        for (MediaPeriodHolder periodHolder = this.queue.getFrontPeriod(); periodHolder != null; periodHolder = periodHolder.next) {
            if (periodHolder.trackSelectorResult != null) {
                for (TrackSelection trackSelection : periodHolder.trackSelectorResult.selections.getAll()) {
                    if (trackSelection != null) {
                        trackSelection.onPlaybackSpeed(playbackSpeed);
                    }
                }
            }
        }
    }

    private boolean shouldTransitionToReadyState(boolean renderersReadyOrEnded) {
        if (this.enabledRenderers.length == 0) {
            return isTimelineReady();
        }
        boolean z = false;
        if (!renderersReadyOrEnded) {
            return false;
        }
        if (!this.playbackInfo.isLoading) {
            return true;
        }
        MediaPeriodHolder loadingHolder = this.queue.getLoadingPeriod();
        if ((loadingHolder.isFullyBuffered() && loadingHolder.f18info.isFinal) || this.loadControl.shouldStartPlayback(getTotalBufferedDurationUs(), this.mediaClock.getPlaybackParameters().speed, this.rebuffering)) {
            z = true;
        }
        return z;
    }

    private boolean isTimelineReady() {
        MediaPeriodHolder playingPeriodHolder = this.queue.getPlayingPeriod();
        long playingPeriodDurationUs = playingPeriodHolder.f18info.durationUs;
        return playingPeriodDurationUs == C.TIME_UNSET || this.playbackInfo.positionUs < playingPeriodDurationUs || (playingPeriodHolder.next != null && (playingPeriodHolder.next.prepared || playingPeriodHolder.next.f18info.id.isAd()));
    }

    private void maybeThrowSourceInfoRefreshError() throws IOException {
        if (this.queue.getLoadingPeriod() != null) {
            Renderer[] rendererArr = this.enabledRenderers;
            int length = rendererArr.length;
            int i = 0;
            while (i < length) {
                if (rendererArr[i].hasReadStreamToEnd()) {
                    i++;
                } else {
                    return;
                }
            }
        }
        this.mediaSource.maybeThrowSourceInfoRefreshError();
    }

    private void maybeThrowPeriodPrepareError() throws IOException {
        MediaPeriodHolder loadingPeriodHolder = this.queue.getLoadingPeriod();
        MediaPeriodHolder readingPeriodHolder = this.queue.getReadingPeriod();
        if (loadingPeriodHolder != null && !loadingPeriodHolder.prepared && (readingPeriodHolder == null || readingPeriodHolder.next == loadingPeriodHolder)) {
            Renderer[] rendererArr = this.enabledRenderers;
            int length = rendererArr.length;
            int i = 0;
            while (i < length) {
                if (rendererArr[i].hasReadStreamToEnd()) {
                    i++;
                } else {
                    return;
                }
            }
            loadingPeriodHolder.mediaPeriod.maybeThrowPrepareError();
        }
    }

    private void handleSourceInfoRefreshed(MediaSourceRefreshInfo sourceRefreshInfo) throws ExoPlaybackException {
        MediaSourceRefreshInfo mediaSourceRefreshInfo = sourceRefreshInfo;
        if (mediaSourceRefreshInfo.source == this.mediaSource) {
            Timeline oldTimeline = this.playbackInfo.timeline;
            Timeline timeline = mediaSourceRefreshInfo.timeline;
            Object manifest = mediaSourceRefreshInfo.manifest;
            this.queue.setTimeline(timeline);
            this.playbackInfo = this.playbackInfo.copyWithTimeline(timeline, manifest);
            resolvePendingMessagePositions();
            int i = this.pendingPrepareCount;
            if (i > 0) {
                this.playbackInfoUpdate.incrementPendingOperationAcks(i);
                this.pendingPrepareCount = 0;
                SeekPosition seekPosition = this.pendingInitialSeekPosition;
                if (seekPosition != null) {
                    try {
                        Pair<Object, Long> periodPosition = resolveSeekPosition(seekPosition, true);
                        this.pendingInitialSeekPosition = null;
                        if (periodPosition == null) {
                            handleSourceInfoRefreshEndedPlayback();
                        } else {
                            Object periodUid = periodPosition.first;
                            long positionUs = ((Long) periodPosition.second).longValue();
                            MediaPeriodId periodId = this.queue.resolveMediaPeriodIdForAds(periodUid, positionUs);
                            this.playbackInfo = this.playbackInfo.resetToNewPosition(periodId, periodId.isAd() ? 0 : positionUs, positionUs);
                        }
                    } catch (IllegalSeekPositionException e) {
                        IllegalSeekPositionException e2 = e;
                        this.playbackInfo = this.playbackInfo.resetToNewPosition(this.playbackInfo.getDummyFirstMediaPeriodId(this.shuffleModeEnabled, this.window), C.TIME_UNSET, C.TIME_UNSET);
                        throw e2;
                    }
                } else if (this.playbackInfo.startPositionUs == C.TIME_UNSET) {
                    if (timeline.isEmpty()) {
                        handleSourceInfoRefreshEndedPlayback();
                    } else {
                        Pair<Object, Long> defaultPosition = getPeriodPosition(timeline, timeline.getFirstWindowIndex(this.shuffleModeEnabled), C.TIME_UNSET);
                        Object periodUid2 = defaultPosition.first;
                        long startPositionUs = ((Long) defaultPosition.second).longValue();
                        MediaPeriodId periodId2 = this.queue.resolveMediaPeriodIdForAds(periodUid2, startPositionUs);
                        this.playbackInfo = this.playbackInfo.resetToNewPosition(periodId2, periodId2.isAd() ? 0 : startPositionUs, startPositionUs);
                    }
                }
            } else if (oldTimeline.isEmpty()) {
                if (!timeline.isEmpty()) {
                    Pair<Object, Long> defaultPosition2 = getPeriodPosition(timeline, timeline.getFirstWindowIndex(this.shuffleModeEnabled), C.TIME_UNSET);
                    Object periodUid3 = defaultPosition2.first;
                    long startPositionUs2 = ((Long) defaultPosition2.second).longValue();
                    MediaPeriodId periodId3 = this.queue.resolveMediaPeriodIdForAds(periodUid3, startPositionUs2);
                    this.playbackInfo = this.playbackInfo.resetToNewPosition(periodId3, periodId3.isAd() ? 0 : startPositionUs2, startPositionUs2);
                }
            } else {
                MediaPeriodHolder periodHolder = this.queue.getFrontPeriod();
                long contentPositionUs = this.playbackInfo.contentPositionUs;
                Object playingPeriodUid = periodHolder == null ? this.playbackInfo.periodId.periodUid : periodHolder.uid;
                int periodIndex = timeline.getIndexOfPeriod(playingPeriodUid);
                if (periodIndex == -1) {
                    Object newPeriodUid = resolveSubsequentPeriod(playingPeriodUid, oldTimeline, timeline);
                    if (newPeriodUid == null) {
                        handleSourceInfoRefreshEndedPlayback();
                        return;
                    }
                    Pair<Object, Long> defaultPosition3 = getPeriodPosition(timeline, timeline.getPeriodByUid(newPeriodUid, this.period).windowIndex, C.TIME_UNSET);
                    Object newPeriodUid2 = defaultPosition3.first;
                    long contentPositionUs2 = ((Long) defaultPosition3.second).longValue();
                    MediaPeriodId periodId4 = this.queue.resolveMediaPeriodIdForAds(newPeriodUid2, contentPositionUs2);
                    if (periodHolder != null) {
                        while (periodHolder.next != null) {
                            periodHolder = periodHolder.next;
                            if (periodHolder.f18info.id.equals(periodId4)) {
                                periodHolder.f18info = this.queue.getUpdatedMediaPeriodInfo(periodHolder.f18info);
                            }
                        }
                    }
                    this.playbackInfo = this.playbackInfo.copyWithNewPosition(periodId4, seekToPeriodPosition(periodId4, periodId4.isAd() ? 0 : contentPositionUs2), contentPositionUs2, getTotalBufferedDurationUs());
                    return;
                }
                MediaPeriodId playingPeriodId = this.playbackInfo.periodId;
                if (playingPeriodId.isAd()) {
                    MediaPeriodId periodId5 = this.queue.resolveMediaPeriodIdForAds(playingPeriodUid, contentPositionUs);
                    if (!periodId5.equals(playingPeriodId)) {
                        int i2 = periodIndex;
                        this.playbackInfo = this.playbackInfo.copyWithNewPosition(periodId5, seekToPeriodPosition(periodId5, periodId5.isAd() ? 0 : contentPositionUs), contentPositionUs, getTotalBufferedDurationUs());
                        return;
                    }
                    long j = contentPositionUs;
                } else {
                    long j2 = contentPositionUs;
                }
                if (!this.queue.updateQueuedPeriods(playingPeriodId, this.rendererPositionUs)) {
                    seekToCurrentPosition(false);
                }
                handleLoadingMediaPeriodChanged(false);
            }
        }
    }

    private void handleSourceInfoRefreshEndedPlayback() {
        setState(4);
        resetInternal(false, true, false);
    }

    @Nullable
    private Object resolveSubsequentPeriod(Object oldPeriodUid, Timeline oldTimeline, Timeline newTimeline) {
        int oldPeriodIndex = oldTimeline.getIndexOfPeriod(oldPeriodUid);
        int newPeriodIndex = -1;
        int maxIterations = oldTimeline.getPeriodCount();
        for (int i = 0; i < maxIterations && newPeriodIndex == -1; i++) {
            oldPeriodIndex = oldTimeline.getNextPeriodIndex(oldPeriodIndex, this.period, this.window, this.repeatMode, this.shuffleModeEnabled);
            if (oldPeriodIndex == -1) {
                break;
            }
            newPeriodIndex = newTimeline.getIndexOfPeriod(oldTimeline.getUidOfPeriod(oldPeriodIndex));
        }
        if (newPeriodIndex == -1) {
            return null;
        }
        return newTimeline.getUidOfPeriod(newPeriodIndex);
    }

    private Pair<Object, Long> resolveSeekPosition(SeekPosition seekPosition, boolean trySubsequentPeriods) {
        Timeline timeline = this.playbackInfo.timeline;
        Timeline seekTimeline = seekPosition.timeline;
        if (timeline.isEmpty()) {
            return null;
        }
        if (seekTimeline.isEmpty()) {
            seekTimeline = timeline;
        }
        try {
            Pair<Object, Long> periodPosition = seekTimeline.getPeriodPosition(this.window, this.period, seekPosition.windowIndex, seekPosition.windowPositionUs);
            if (timeline == seekTimeline) {
                return periodPosition;
            }
            int periodIndex = timeline.getIndexOfPeriod(periodPosition.first);
            if (periodIndex != -1) {
                return periodPosition;
            }
            if (!trySubsequentPeriods || resolveSubsequentPeriod(periodPosition.first, seekTimeline, timeline) == null) {
                return null;
            }
            return getPeriodPosition(timeline, timeline.getPeriod(periodIndex, this.period).windowIndex, C.TIME_UNSET);
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalSeekPositionException(timeline, seekPosition.windowIndex, seekPosition.windowPositionUs);
        }
    }

    private Pair<Object, Long> getPeriodPosition(Timeline timeline, int windowIndex, long windowPositionUs) {
        return timeline.getPeriodPosition(this.window, this.period, windowIndex, windowPositionUs);
    }

    private void updatePeriods() throws ExoPlaybackException, IOException {
        MediaPeriodHolder playingPeriodHolder;
        MediaPeriodHolder loadingPeriodHolder;
        ExoPlayerImplInternal exoPlayerImplInternal = this;
        MediaSource mediaSource2 = exoPlayerImplInternal.mediaSource;
        if (mediaSource2 != null) {
            if (exoPlayerImplInternal.pendingPrepareCount > 0) {
                mediaSource2.maybeThrowSourceInfoRefreshError();
                return;
            }
            maybeUpdateLoadingPeriod();
            MediaPeriodHolder loadingPeriodHolder2 = exoPlayerImplInternal.queue.getLoadingPeriod();
            if (loadingPeriodHolder2 == null || loadingPeriodHolder2.isFullyBuffered()) {
                exoPlayerImplInternal.setIsLoading(false);
            } else if (!exoPlayerImplInternal.playbackInfo.isLoading) {
                maybeContinueLoading();
            }
            if (exoPlayerImplInternal.queue.hasPlayingPeriod()) {
                MediaPeriodHolder playingPeriodHolder2 = exoPlayerImplInternal.queue.getPlayingPeriod();
                MediaPeriodHolder readingPeriodHolder = exoPlayerImplInternal.queue.getReadingPeriod();
                boolean advancedPlayingPeriod = false;
                while (exoPlayerImplInternal.playWhenReady && playingPeriodHolder2 != readingPeriodHolder && exoPlayerImplInternal.rendererPositionUs >= playingPeriodHolder2.next.getStartPositionRendererTime()) {
                    if (advancedPlayingPeriod) {
                        maybeNotifyPlaybackInfoChanged();
                    }
                    int discontinuityReason = playingPeriodHolder2.f18info.isLastInTimelinePeriod ? 0 : 3;
                    MediaPeriodHolder oldPlayingPeriodHolder = playingPeriodHolder2;
                    playingPeriodHolder2 = exoPlayerImplInternal.queue.advancePlayingPeriod();
                    exoPlayerImplInternal.updatePlayingPeriodRenderers(oldPlayingPeriodHolder);
                    exoPlayerImplInternal.playbackInfo = exoPlayerImplInternal.playbackInfo.copyWithNewPosition(playingPeriodHolder2.f18info.id, playingPeriodHolder2.f18info.startPositionUs, playingPeriodHolder2.f18info.contentPositionUs, getTotalBufferedDurationUs());
                    exoPlayerImplInternal.playbackInfoUpdate.setPositionDiscontinuity(discontinuityReason);
                    updatePlaybackPositions();
                    advancedPlayingPeriod = true;
                }
                if (readingPeriodHolder.f18info.isFinal) {
                    int i = 0;
                    while (true) {
                        Renderer[] rendererArr = exoPlayerImplInternal.renderers;
                        if (i < rendererArr.length) {
                            Renderer renderer = rendererArr[i];
                            SampleStream sampleStream = readingPeriodHolder.sampleStreams[i];
                            if (sampleStream != null && renderer.getStream() == sampleStream && renderer.hasReadStreamToEnd()) {
                                renderer.setCurrentStreamFinal();
                            }
                            i++;
                        } else {
                            return;
                        }
                    }
                } else if (readingPeriodHolder.next != null) {
                    int i2 = 0;
                    while (true) {
                        Renderer[] rendererArr2 = exoPlayerImplInternal.renderers;
                        if (i2 < rendererArr2.length) {
                            Renderer renderer2 = rendererArr2[i2];
                            SampleStream sampleStream2 = readingPeriodHolder.sampleStreams[i2];
                            if (renderer2.getStream() == sampleStream2 && (sampleStream2 == null || renderer2.hasReadStreamToEnd())) {
                                i2++;
                            }
                        } else if (!readingPeriodHolder.next.prepared) {
                            maybeThrowPeriodPrepareError();
                            return;
                        } else {
                            TrackSelectorResult oldTrackSelectorResult = readingPeriodHolder.trackSelectorResult;
                            MediaPeriodHolder readingPeriodHolder2 = exoPlayerImplInternal.queue.advanceReadingPeriod();
                            TrackSelectorResult newTrackSelectorResult = readingPeriodHolder2.trackSelectorResult;
                            boolean initialDiscontinuity = readingPeriodHolder2.mediaPeriod.readDiscontinuity() != C.TIME_UNSET;
                            int i3 = 0;
                            while (true) {
                                Renderer[] rendererArr3 = exoPlayerImplInternal.renderers;
                                if (i3 < rendererArr3.length) {
                                    Renderer renderer3 = rendererArr3[i3];
                                    if (!oldTrackSelectorResult.isRendererEnabled(i3)) {
                                        loadingPeriodHolder = loadingPeriodHolder2;
                                        playingPeriodHolder = playingPeriodHolder2;
                                    } else if (initialDiscontinuity) {
                                        renderer3.setCurrentStreamFinal();
                                        loadingPeriodHolder = loadingPeriodHolder2;
                                        playingPeriodHolder = playingPeriodHolder2;
                                    } else if (!renderer3.isCurrentStreamFinal()) {
                                        TrackSelection newSelection = newTrackSelectorResult.selections.get(i3);
                                        boolean newRendererEnabled = newTrackSelectorResult.isRendererEnabled(i3);
                                        boolean isNoSampleRenderer = exoPlayerImplInternal.rendererCapabilities[i3].getTrackType() == 6;
                                        RendererConfiguration oldConfig = oldTrackSelectorResult.rendererConfigurations[i3];
                                        RendererConfiguration newConfig = newTrackSelectorResult.rendererConfigurations[i3];
                                        if (!newRendererEnabled || !newConfig.equals(oldConfig) || isNoSampleRenderer) {
                                            loadingPeriodHolder = loadingPeriodHolder2;
                                            boolean z = isNoSampleRenderer;
                                            playingPeriodHolder = playingPeriodHolder2;
                                            renderer3.setCurrentStreamFinal();
                                        } else {
                                            loadingPeriodHolder = loadingPeriodHolder2;
                                            boolean z2 = isNoSampleRenderer;
                                            playingPeriodHolder = playingPeriodHolder2;
                                            renderer3.replaceStream(getFormats(newSelection), readingPeriodHolder2.sampleStreams[i3], readingPeriodHolder2.getRendererOffset());
                                        }
                                    } else {
                                        loadingPeriodHolder = loadingPeriodHolder2;
                                        playingPeriodHolder = playingPeriodHolder2;
                                    }
                                    i3++;
                                    loadingPeriodHolder2 = loadingPeriodHolder;
                                    playingPeriodHolder2 = playingPeriodHolder;
                                    exoPlayerImplInternal = this;
                                } else {
                                    MediaPeriodHolder mediaPeriodHolder = playingPeriodHolder2;
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void maybeUpdateLoadingPeriod() throws IOException {
        this.queue.reevaluateBuffer(this.rendererPositionUs);
        if (this.queue.shouldLoadNextMediaPeriod()) {
            MediaPeriodInfo info2 = this.queue.getNextMediaPeriodInfo(this.rendererPositionUs, this.playbackInfo);
            if (info2 == null) {
                maybeThrowSourceInfoRefreshError();
                return;
            }
            this.queue.enqueueNextMediaPeriod(this.rendererCapabilities, this.trackSelector, this.loadControl.getAllocator(), this.mediaSource, info2).prepare(this, info2.startPositionUs);
            setIsLoading(true);
            handleLoadingMediaPeriodChanged(false);
        }
    }

    private void handlePeriodPrepared(MediaPeriod mediaPeriod) throws ExoPlaybackException {
        if (this.queue.isLoading(mediaPeriod)) {
            MediaPeriodHolder loadingPeriodHolder = this.queue.getLoadingPeriod();
            loadingPeriodHolder.handlePrepared(this.mediaClock.getPlaybackParameters().speed);
            updateLoadControlTrackSelection(loadingPeriodHolder.trackGroups, loadingPeriodHolder.trackSelectorResult);
            if (!this.queue.hasPlayingPeriod()) {
                resetRendererPosition(this.queue.advancePlayingPeriod().f18info.startPositionUs);
                updatePlayingPeriodRenderers(null);
            }
            maybeContinueLoading();
        }
    }

    private void handleContinueLoadingRequested(MediaPeriod mediaPeriod) {
        if (this.queue.isLoading(mediaPeriod)) {
            this.queue.reevaluateBuffer(this.rendererPositionUs);
            maybeContinueLoading();
        }
    }

    private void handlePlaybackParameters(PlaybackParameters playbackParameters) throws ExoPlaybackException {
        Renderer[] rendererArr;
        this.eventHandler.obtainMessage(1, playbackParameters).sendToTarget();
        updateTrackSelectionPlaybackSpeed(playbackParameters.speed);
        for (Renderer renderer : this.renderers) {
            if (renderer != null) {
                renderer.setOperatingRate(playbackParameters.speed);
            }
        }
    }

    private void maybeContinueLoading() {
        MediaPeriodHolder loadingPeriodHolder = this.queue.getLoadingPeriod();
        long nextLoadPositionUs = loadingPeriodHolder.getNextLoadPositionUs();
        if (nextLoadPositionUs == Long.MIN_VALUE) {
            setIsLoading(false);
            return;
        }
        boolean continueLoading = this.loadControl.shouldContinueLoading(getTotalBufferedDurationUs(nextLoadPositionUs), this.mediaClock.getPlaybackParameters().speed);
        setIsLoading(continueLoading);
        if (continueLoading) {
            loadingPeriodHolder.continueLoading(this.rendererPositionUs);
        }
    }

    private void updatePlayingPeriodRenderers(@Nullable MediaPeriodHolder oldPlayingPeriodHolder) throws ExoPlaybackException {
        MediaPeriodHolder newPlayingPeriodHolder = this.queue.getPlayingPeriod();
        if (newPlayingPeriodHolder != null && oldPlayingPeriodHolder != newPlayingPeriodHolder) {
            int enabledRendererCount = 0;
            boolean[] rendererWasEnabledFlags = new boolean[this.renderers.length];
            int i = 0;
            while (true) {
                Renderer[] rendererArr = this.renderers;
                if (i < rendererArr.length) {
                    Renderer renderer = rendererArr[i];
                    rendererWasEnabledFlags[i] = renderer.getState() != 0;
                    if (newPlayingPeriodHolder.trackSelectorResult.isRendererEnabled(i)) {
                        enabledRendererCount++;
                    }
                    if (rendererWasEnabledFlags[i] && (!newPlayingPeriodHolder.trackSelectorResult.isRendererEnabled(i) || (renderer.isCurrentStreamFinal() && renderer.getStream() == oldPlayingPeriodHolder.sampleStreams[i]))) {
                        disableRenderer(renderer);
                    }
                    i++;
                } else {
                    this.playbackInfo = this.playbackInfo.copyWithTrackInfo(newPlayingPeriodHolder.trackGroups, newPlayingPeriodHolder.trackSelectorResult);
                    enableRenderers(rendererWasEnabledFlags, enabledRendererCount);
                    return;
                }
            }
        }
    }

    private void enableRenderers(boolean[] rendererWasEnabledFlags, int totalEnabledRendererCount) throws ExoPlaybackException {
        this.enabledRenderers = new Renderer[totalEnabledRendererCount];
        int enabledRendererCount = 0;
        MediaPeriodHolder playingPeriodHolder = this.queue.getPlayingPeriod();
        for (int i = 0; i < this.renderers.length; i++) {
            if (playingPeriodHolder.trackSelectorResult.isRendererEnabled(i)) {
                int enabledRendererCount2 = enabledRendererCount + 1;
                enableRenderer(i, rendererWasEnabledFlags[i], enabledRendererCount);
                enabledRendererCount = enabledRendererCount2;
            }
        }
    }

    private void enableRenderer(int rendererIndex, boolean wasRendererEnabled, int enabledRendererIndex) throws ExoPlaybackException {
        int i = rendererIndex;
        MediaPeriodHolder playingPeriodHolder = this.queue.getPlayingPeriod();
        Renderer renderer = this.renderers[i];
        this.enabledRenderers[enabledRendererIndex] = renderer;
        if (renderer.getState() == 0) {
            RendererConfiguration rendererConfiguration = playingPeriodHolder.trackSelectorResult.rendererConfigurations[i];
            Format[] formats = getFormats(playingPeriodHolder.trackSelectorResult.selections.get(i));
            boolean playing = this.playWhenReady && this.playbackInfo.playbackState == 3;
            renderer.enable(rendererConfiguration, formats, playingPeriodHolder.sampleStreams[i], this.rendererPositionUs, !wasRendererEnabled && playing, playingPeriodHolder.getRendererOffset());
            this.mediaClock.onRendererEnabled(renderer);
            if (playing) {
                renderer.start();
            }
        }
    }

    private boolean rendererWaitingForNextStream(Renderer renderer) {
        MediaPeriodHolder readingPeriodHolder = this.queue.getReadingPeriod();
        return readingPeriodHolder.next != null && readingPeriodHolder.next.prepared && renderer.hasReadStreamToEnd();
    }

    private void handleLoadingMediaPeriodChanged(boolean loadingTrackSelectionChanged) {
        long j;
        MediaPeriodHolder loadingMediaPeriodHolder = this.queue.getLoadingPeriod();
        MediaPeriodId loadingMediaPeriodId = loadingMediaPeriodHolder == null ? this.playbackInfo.periodId : loadingMediaPeriodHolder.f18info.id;
        boolean loadingMediaPeriodChanged = !this.playbackInfo.loadingMediaPeriodId.equals(loadingMediaPeriodId);
        if (loadingMediaPeriodChanged) {
            this.playbackInfo = this.playbackInfo.copyWithLoadingMediaPeriodId(loadingMediaPeriodId);
        }
        PlaybackInfo playbackInfo2 = this.playbackInfo;
        if (loadingMediaPeriodHolder == null) {
            j = playbackInfo2.positionUs;
        } else {
            j = loadingMediaPeriodHolder.getBufferedPositionUs();
        }
        playbackInfo2.bufferedPositionUs = j;
        this.playbackInfo.totalBufferedDurationUs = getTotalBufferedDurationUs();
        if ((loadingMediaPeriodChanged || loadingTrackSelectionChanged) && loadingMediaPeriodHolder != null && loadingMediaPeriodHolder.prepared) {
            updateLoadControlTrackSelection(loadingMediaPeriodHolder.trackGroups, loadingMediaPeriodHolder.trackSelectorResult);
        }
    }

    private long getTotalBufferedDurationUs() {
        return getTotalBufferedDurationUs(this.playbackInfo.bufferedPositionUs);
    }

    private long getTotalBufferedDurationUs(long bufferedPositionInLoadingPeriodUs) {
        MediaPeriodHolder loadingPeriodHolder = this.queue.getLoadingPeriod();
        if (loadingPeriodHolder == null) {
            return 0;
        }
        return bufferedPositionInLoadingPeriodUs - loadingPeriodHolder.toPeriodTime(this.rendererPositionUs);
    }

    private void updateLoadControlTrackSelection(TrackGroupArray trackGroups, TrackSelectorResult trackSelectorResult) {
        this.loadControl.onTracksSelected(this.renderers, trackGroups, trackSelectorResult.selections);
    }

    private static Format[] getFormats(TrackSelection newSelection) {
        int length = newSelection != null ? newSelection.length() : 0;
        Format[] formats = new Format[length];
        for (int i = 0; i < length; i++) {
            formats[i] = newSelection.getFormat(i);
        }
        return formats;
    }
}
