package com.google.android.exoplayer2.ui;

import android.annotation.SuppressLint;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.os.EnvironmentCompat;
import android.widget.TextView;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.Player.EventListener.CC;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.util.Assertions;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

public class DebugTextViewHelper implements EventListener, Runnable {
    private static final int REFRESH_INTERVAL_MS = 1000;
    private final SimpleExoPlayer player;
    private boolean started;
    private final TextView textView;

    public /* synthetic */ void onLoadingChanged(boolean z) {
        CC.$default$onLoadingChanged(this, z);
    }

    public /* synthetic */ void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        CC.$default$onPlaybackParametersChanged(this, playbackParameters);
    }

    public /* synthetic */ void onPlayerError(ExoPlaybackException exoPlaybackException) {
        CC.$default$onPlayerError(this, exoPlaybackException);
    }

    public /* synthetic */ void onRepeatModeChanged(int i) {
        CC.$default$onRepeatModeChanged(this, i);
    }

    public /* synthetic */ void onSeekProcessed() {
        CC.$default$onSeekProcessed(this);
    }

    public /* synthetic */ void onShuffleModeEnabledChanged(boolean z) {
        CC.$default$onShuffleModeEnabledChanged(this, z);
    }

    public /* synthetic */ void onTimelineChanged(Timeline timeline, @Nullable Object obj, int i) {
        CC.$default$onTimelineChanged(this, timeline, obj, i);
    }

    public /* synthetic */ void onTracksChanged(TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {
        CC.$default$onTracksChanged(this, trackGroupArray, trackSelectionArray);
    }

    public DebugTextViewHelper(SimpleExoPlayer player2, TextView textView2) {
        Assertions.checkArgument(player2.getApplicationLooper() == Looper.getMainLooper());
        this.player = player2;
        this.textView = textView2;
    }

    public final void start() {
        if (!this.started) {
            this.started = true;
            this.player.addListener(this);
            updateAndPost();
        }
    }

    public final void stop() {
        if (this.started) {
            this.started = false;
            this.player.removeListener(this);
            this.textView.removeCallbacks(this);
        }
    }

    public final void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        updateAndPost();
    }

    public final void onPositionDiscontinuity(int reason) {
        updateAndPost();
    }

    public final void run() {
        updateAndPost();
    }

    /* access modifiers changed from: protected */
    @SuppressLint({"SetTextI18n"})
    public final void updateAndPost() {
        this.textView.setText(getDebugString());
        this.textView.removeCallbacks(this);
        this.textView.postDelayed(this, 1000);
    }

    /* access modifiers changed from: protected */
    public String getDebugString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPlayerStateString());
        sb.append(getVideoString());
        sb.append(getAudioString());
        return sb.toString();
    }

    /* access modifiers changed from: protected */
    public String getPlayerStateString() {
        String playbackStateString;
        switch (this.player.getPlaybackState()) {
            case 1:
                playbackStateString = "idle";
                break;
            case 2:
                playbackStateString = "buffering";
                break;
            case 3:
                playbackStateString = "ready";
                break;
            case 4:
                playbackStateString = "ended";
                break;
            default:
                playbackStateString = EnvironmentCompat.MEDIA_UNKNOWN;
                break;
        }
        return String.format("playWhenReady:%s playbackState:%s window:%s", new Object[]{Boolean.valueOf(this.player.getPlayWhenReady()), playbackStateString, Integer.valueOf(this.player.getCurrentWindowIndex())});
    }

    /* access modifiers changed from: protected */
    public String getVideoString() {
        Format format = this.player.getVideoFormat();
        if (format == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.LF);
        sb.append(format.sampleMimeType);
        sb.append("(id:");
        sb.append(format.id);
        sb.append(" r:");
        sb.append(format.width);
        sb.append("x");
        sb.append(format.height);
        sb.append(getPixelAspectRatioString(format.pixelWidthHeightRatio));
        sb.append(getDecoderCountersBufferCountString(this.player.getVideoDecoderCounters()));
        sb.append(")");
        return sb.toString();
    }

    /* access modifiers changed from: protected */
    public String getAudioString() {
        Format format = this.player.getAudioFormat();
        if (format == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.LF);
        sb.append(format.sampleMimeType);
        sb.append("(id:");
        sb.append(format.id);
        sb.append(" hz:");
        sb.append(format.sampleRate);
        sb.append(" ch:");
        sb.append(format.channelCount);
        sb.append(getDecoderCountersBufferCountString(this.player.getAudioDecoderCounters()));
        sb.append(")");
        return sb.toString();
    }

    private static String getDecoderCountersBufferCountString(DecoderCounters counters) {
        if (counters == null) {
            return "";
        }
        counters.ensureUpdated();
        StringBuilder sb = new StringBuilder();
        sb.append(" sib:");
        sb.append(counters.skippedInputBufferCount);
        sb.append(" sb:");
        sb.append(counters.skippedOutputBufferCount);
        sb.append(" rb:");
        sb.append(counters.renderedOutputBufferCount);
        sb.append(" db:");
        sb.append(counters.droppedBufferCount);
        sb.append(" mcdb:");
        sb.append(counters.maxConsecutiveDroppedBufferCount);
        sb.append(" dk:");
        sb.append(counters.droppedToKeyframeCount);
        return sb.toString();
    }

    private static String getPixelAspectRatioString(float pixelAspectRatio) {
        if (pixelAspectRatio == -1.0f || pixelAspectRatio == 1.0f) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(" par:");
        sb.append(String.format(Locale.US, "%.02f", new Object[]{Float.valueOf(pixelAspectRatio)}));
        return sb.toString();
    }
}
