package com.google.android.exoplayer2.util;

import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.view.Surface;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Timeline.Period;
import com.google.android.exoplayer2.Timeline.Window;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.analytics.AnalyticsListener.CC;
import com.google.android.exoplayer2.analytics.AnalyticsListener.EventTime;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.source.MediaSourceEventListener.LoadEventInfo;
import com.google.android.exoplayer2.source.MediaSourceEventListener.MediaLoadData;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

public class EventLogger implements AnalyticsListener {
    private static final String DEFAULT_TAG = "EventLogger";
    private static final int MAX_TIMELINE_ITEM_LINES = 3;
    private static final NumberFormat TIME_FORMAT = NumberFormat.getInstance(Locale.US);
    private final Period period;
    private final long startTimeMs;
    private final String tag;
    @Nullable
    private final MappingTrackSelector trackSelector;
    private final Window window;

    public /* synthetic */ void onAudioAttributesChanged(EventTime eventTime, AudioAttributes audioAttributes) {
        CC.$default$onAudioAttributesChanged(this, eventTime, audioAttributes);
    }

    public /* synthetic */ void onVolumeChanged(EventTime eventTime, float f) {
        CC.$default$onVolumeChanged(this, eventTime, f);
    }

    static {
        TIME_FORMAT.setMinimumFractionDigits(2);
        TIME_FORMAT.setMaximumFractionDigits(2);
        TIME_FORMAT.setGroupingUsed(false);
    }

    public EventLogger(@Nullable MappingTrackSelector trackSelector2) {
        this(trackSelector2, DEFAULT_TAG);
    }

    public EventLogger(@Nullable MappingTrackSelector trackSelector2, String tag2) {
        this.trackSelector = trackSelector2;
        this.tag = tag2;
        this.window = new Window();
        this.period = new Period();
        this.startTimeMs = SystemClock.elapsedRealtime();
    }

    public void onLoadingChanged(EventTime eventTime, boolean isLoading) {
        logd(eventTime, "loading", Boolean.toString(isLoading));
    }

    public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int state) {
        StringBuilder sb = new StringBuilder();
        sb.append(playWhenReady);
        sb.append(", ");
        sb.append(getStateString(state));
        logd(eventTime, "state", sb.toString());
    }

    public void onRepeatModeChanged(EventTime eventTime, int repeatMode) {
        logd(eventTime, "repeatMode", getRepeatModeString(repeatMode));
    }

    public void onShuffleModeChanged(EventTime eventTime, boolean shuffleModeEnabled) {
        logd(eventTime, "shuffleModeEnabled", Boolean.toString(shuffleModeEnabled));
    }

    public void onPositionDiscontinuity(EventTime eventTime, int reason) {
        logd(eventTime, "positionDiscontinuity", getDiscontinuityReasonString(reason));
    }

    public void onSeekStarted(EventTime eventTime) {
        logd(eventTime, "seekStarted");
    }

    public void onPlaybackParametersChanged(EventTime eventTime, PlaybackParameters playbackParameters) {
        logd(eventTime, "playbackParameters", Util.formatInvariant("speed=%.2f, pitch=%.2f, skipSilence=%s", Float.valueOf(playbackParameters.speed), Float.valueOf(playbackParameters.pitch), Boolean.valueOf(playbackParameters.skipSilence)));
    }

    public void onTimelineChanged(EventTime eventTime, int reason) {
        int periodCount = eventTime.timeline.getPeriodCount();
        int windowCount = eventTime.timeline.getWindowCount();
        StringBuilder sb = new StringBuilder();
        sb.append("timelineChanged [");
        sb.append(getEventTimeString(eventTime));
        sb.append(", periodCount=");
        sb.append(periodCount);
        sb.append(", windowCount=");
        sb.append(windowCount);
        sb.append(", reason=");
        sb.append(getTimelineChangeReasonString(reason));
        logd(sb.toString());
        for (int i = 0; i < Math.min(periodCount, 3); i++) {
            eventTime.timeline.getPeriod(i, this.period);
            StringBuilder sb2 = new StringBuilder();
            sb2.append("  period [");
            sb2.append(getTimeString(this.period.getDurationMs()));
            sb2.append("]");
            logd(sb2.toString());
        }
        if (periodCount > 3) {
            logd("  ...");
        }
        for (int i2 = 0; i2 < Math.min(windowCount, 3); i2++) {
            eventTime.timeline.getWindow(i2, this.window);
            StringBuilder sb3 = new StringBuilder();
            sb3.append("  window [");
            sb3.append(getTimeString(this.window.getDurationMs()));
            sb3.append(", ");
            sb3.append(this.window.isSeekable);
            sb3.append(", ");
            sb3.append(this.window.isDynamic);
            sb3.append("]");
            logd(sb3.toString());
        }
        if (windowCount > 3) {
            logd("  ...");
        }
        logd("]");
    }

    public void onPlayerError(EventTime eventTime, ExoPlaybackException e) {
        loge(eventTime, "playerFailed", e);
    }

    public void onTracksChanged(EventTime eventTime, TrackGroupArray ignored, TrackSelectionArray trackSelections) {
        MappingTrackSelector mappingTrackSelector = this.trackSelector;
        MappedTrackInfo mappedTrackInfo = mappingTrackSelector != null ? mappingTrackSelector.getCurrentMappedTrackInfo() : null;
        if (mappedTrackInfo == null) {
            logd(eventTime, "tracksChanged", "[]");
            return;
        }
        EventTime eventTime2 = eventTime;
        StringBuilder sb = new StringBuilder();
        sb.append("tracksChanged [");
        sb.append(getEventTimeString(eventTime));
        sb.append(", ");
        logd(sb.toString());
        int rendererCount = mappedTrackInfo.getRendererCount();
        int rendererIndex = 0;
        while (true) {
            boolean z = false;
            if (rendererIndex >= rendererCount) {
                break;
            }
            TrackGroupArray rendererTrackGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
            TrackSelection trackSelection = trackSelections.get(rendererIndex);
            if (rendererTrackGroups.length > 0) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("  Renderer:");
                sb2.append(rendererIndex);
                sb2.append(" [");
                logd(sb2.toString());
                int groupIndex = 0;
                while (groupIndex < rendererTrackGroups.length) {
                    TrackGroup trackGroup = rendererTrackGroups.get(groupIndex);
                    String adaptiveSupport = getAdaptiveSupportString(trackGroup.length, mappedTrackInfo.getAdaptiveSupport(rendererIndex, groupIndex, z));
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("    Group:");
                    sb3.append(groupIndex);
                    sb3.append(", adaptive_supported=");
                    sb3.append(adaptiveSupport);
                    sb3.append(" [");
                    logd(sb3.toString());
                    for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                        String status = getTrackStatusString(trackSelection, trackGroup, trackIndex);
                        String formatSupport = getFormatSupportString(mappedTrackInfo.getTrackSupport(rendererIndex, groupIndex, trackIndex));
                        StringBuilder sb4 = new StringBuilder();
                        sb4.append("      ");
                        sb4.append(status);
                        sb4.append(" Track:");
                        sb4.append(trackIndex);
                        sb4.append(", ");
                        sb4.append(Format.toLogString(trackGroup.getFormat(trackIndex)));
                        sb4.append(", supported=");
                        sb4.append(formatSupport);
                        logd(sb4.toString());
                    }
                    logd("    ]");
                    groupIndex++;
                    z = false;
                }
                if (trackSelection != null) {
                    int selectionIndex = 0;
                    while (true) {
                        if (selectionIndex >= trackSelection.length()) {
                            break;
                        }
                        Metadata metadata = trackSelection.getFormat(selectionIndex).metadata;
                        if (metadata != null) {
                            logd("    Metadata [");
                            printMetadata(metadata, "      ");
                            logd("    ]");
                            break;
                        }
                        selectionIndex++;
                    }
                }
                logd("  ]");
            }
            rendererIndex++;
        }
        TrackSelectionArray trackSelectionArray = trackSelections;
        TrackGroupArray unassociatedTrackGroups = mappedTrackInfo.getUnmappedTrackGroups();
        if (unassociatedTrackGroups.length > 0) {
            logd("  Renderer:None [");
            for (int groupIndex2 = 0; groupIndex2 < unassociatedTrackGroups.length; groupIndex2++) {
                StringBuilder sb5 = new StringBuilder();
                sb5.append("    Group:");
                sb5.append(groupIndex2);
                sb5.append(" [");
                logd(sb5.toString());
                TrackGroup trackGroup2 = unassociatedTrackGroups.get(groupIndex2);
                for (int trackIndex2 = 0; trackIndex2 < trackGroup2.length; trackIndex2++) {
                    String status2 = getTrackStatusString(false);
                    String formatSupport2 = getFormatSupportString(0);
                    StringBuilder sb6 = new StringBuilder();
                    sb6.append("      ");
                    sb6.append(status2);
                    sb6.append(" Track:");
                    sb6.append(trackIndex2);
                    sb6.append(", ");
                    sb6.append(Format.toLogString(trackGroup2.getFormat(trackIndex2)));
                    sb6.append(", supported=");
                    sb6.append(formatSupport2);
                    logd(sb6.toString());
                }
                logd("    ]");
            }
            logd("  ]");
        }
        logd("]");
    }

    public void onSeekProcessed(EventTime eventTime) {
        logd(eventTime, "seekProcessed");
    }

    public void onMetadata(EventTime eventTime, Metadata metadata) {
        StringBuilder sb = new StringBuilder();
        sb.append("metadata [");
        sb.append(getEventTimeString(eventTime));
        sb.append(", ");
        logd(sb.toString());
        printMetadata(metadata, "  ");
        logd("]");
    }

    public void onDecoderEnabled(EventTime eventTime, int trackType, DecoderCounters counters) {
        logd(eventTime, "decoderEnabled", getTrackTypeString(trackType));
    }

    public void onAudioSessionId(EventTime eventTime, int audioSessionId) {
        logd(eventTime, "audioSessionId", Integer.toString(audioSessionId));
    }

    public void onDecoderInitialized(EventTime eventTime, int trackType, String decoderName, long initializationDurationMs) {
        StringBuilder sb = new StringBuilder();
        sb.append(getTrackTypeString(trackType));
        sb.append(", ");
        sb.append(decoderName);
        logd(eventTime, "decoderInitialized", sb.toString());
    }

    public void onDecoderInputFormatChanged(EventTime eventTime, int trackType, Format format) {
        StringBuilder sb = new StringBuilder();
        sb.append(getTrackTypeString(trackType));
        sb.append(", ");
        sb.append(Format.toLogString(format));
        logd(eventTime, "decoderInputFormatChanged", sb.toString());
    }

    public void onDecoderDisabled(EventTime eventTime, int trackType, DecoderCounters counters) {
        logd(eventTime, "decoderDisabled", getTrackTypeString(trackType));
    }

    public void onAudioUnderrun(EventTime eventTime, int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
        StringBuilder sb = new StringBuilder();
        sb.append(bufferSize);
        sb.append(", ");
        sb.append(bufferSizeMs);
        sb.append(", ");
        sb.append(elapsedSinceLastFeedMs);
        sb.append("]");
        loge(eventTime, "audioTrackUnderrun", sb.toString(), null);
    }

    public void onDroppedVideoFrames(EventTime eventTime, int count, long elapsedMs) {
        logd(eventTime, "droppedFrames", Integer.toString(count));
    }

    public void onVideoSizeChanged(EventTime eventTime, int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        StringBuilder sb = new StringBuilder();
        sb.append(width);
        sb.append(", ");
        sb.append(height);
        logd(eventTime, "videoSizeChanged", sb.toString());
    }

    public void onRenderedFirstFrame(EventTime eventTime, @Nullable Surface surface) {
        logd(eventTime, "renderedFirstFrame", String.valueOf(surface));
    }

    public void onMediaPeriodCreated(EventTime eventTime) {
        logd(eventTime, "mediaPeriodCreated");
    }

    public void onMediaPeriodReleased(EventTime eventTime) {
        logd(eventTime, "mediaPeriodReleased");
    }

    public void onLoadStarted(EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
    }

    public void onLoadError(EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData, IOException error, boolean wasCanceled) {
        printInternalError(eventTime, "loadError", error);
    }

    public void onLoadCanceled(EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
    }

    public void onLoadCompleted(EventTime eventTime, LoadEventInfo loadEventInfo, MediaLoadData mediaLoadData) {
    }

    public void onReadingStarted(EventTime eventTime) {
        logd(eventTime, "mediaPeriodReadingStarted");
    }

    public void onBandwidthEstimate(EventTime eventTime, int totalLoadTimeMs, long totalBytesLoaded, long bitrateEstimate) {
    }

    public void onSurfaceSizeChanged(EventTime eventTime, int width, int height) {
        StringBuilder sb = new StringBuilder();
        sb.append(width);
        sb.append(", ");
        sb.append(height);
        logd(eventTime, "surfaceSizeChanged", sb.toString());
    }

    public void onUpstreamDiscarded(EventTime eventTime, MediaLoadData mediaLoadData) {
        logd(eventTime, "upstreamDiscarded", Format.toLogString(mediaLoadData.trackFormat));
    }

    public void onDownstreamFormatChanged(EventTime eventTime, MediaLoadData mediaLoadData) {
        logd(eventTime, "downstreamFormatChanged", Format.toLogString(mediaLoadData.trackFormat));
    }

    public void onDrmSessionAcquired(EventTime eventTime) {
        logd(eventTime, "drmSessionAcquired");
    }

    public void onDrmSessionManagerError(EventTime eventTime, Exception e) {
        printInternalError(eventTime, "drmSessionManagerError", e);
    }

    public void onDrmKeysRestored(EventTime eventTime) {
        logd(eventTime, "drmKeysRestored");
    }

    public void onDrmKeysRemoved(EventTime eventTime) {
        logd(eventTime, "drmKeysRemoved");
    }

    public void onDrmKeysLoaded(EventTime eventTime) {
        logd(eventTime, "drmKeysLoaded");
    }

    public void onDrmSessionReleased(EventTime eventTime) {
        logd(eventTime, "drmSessionReleased");
    }

    /* access modifiers changed from: protected */
    public void logd(String msg) {
        Log.d(this.tag, msg);
    }

    /* access modifiers changed from: protected */
    public void loge(String msg, @Nullable Throwable tr) {
        Log.e(this.tag, msg, tr);
    }

    private void logd(EventTime eventTime, String eventName) {
        logd(getEventString(eventTime, eventName));
    }

    private void logd(EventTime eventTime, String eventName, String eventDescription) {
        logd(getEventString(eventTime, eventName, eventDescription));
    }

    private void loge(EventTime eventTime, String eventName, @Nullable Throwable throwable) {
        loge(getEventString(eventTime, eventName), throwable);
    }

    private void loge(EventTime eventTime, String eventName, String eventDescription, @Nullable Throwable throwable) {
        loge(getEventString(eventTime, eventName, eventDescription), throwable);
    }

    private void printInternalError(EventTime eventTime, String type, Exception e) {
        loge(eventTime, "internalError", type, e);
    }

    private void printMetadata(Metadata metadata, String prefix) {
        for (int i = 0; i < metadata.length(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(prefix);
            sb.append(metadata.get(i));
            logd(sb.toString());
        }
    }

    private String getEventString(EventTime eventTime, String eventName) {
        StringBuilder sb = new StringBuilder();
        sb.append(eventName);
        sb.append(" [");
        sb.append(getEventTimeString(eventTime));
        sb.append("]");
        return sb.toString();
    }

    private String getEventString(EventTime eventTime, String eventName, String eventDescription) {
        StringBuilder sb = new StringBuilder();
        sb.append(eventName);
        sb.append(" [");
        sb.append(getEventTimeString(eventTime));
        sb.append(", ");
        sb.append(eventDescription);
        sb.append("]");
        return sb.toString();
    }

    private String getEventTimeString(EventTime eventTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("window=");
        sb.append(eventTime.windowIndex);
        String windowPeriodString = sb.toString();
        if (eventTime.mediaPeriodId != null) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append(windowPeriodString);
            sb2.append(", period=");
            sb2.append(eventTime.timeline.getIndexOfPeriod(eventTime.mediaPeriodId.periodUid));
            windowPeriodString = sb2.toString();
            if (eventTime.mediaPeriodId.isAd()) {
                StringBuilder sb3 = new StringBuilder();
                sb3.append(windowPeriodString);
                sb3.append(", adGroup=");
                sb3.append(eventTime.mediaPeriodId.adGroupIndex);
                String windowPeriodString2 = sb3.toString();
                StringBuilder sb4 = new StringBuilder();
                sb4.append(windowPeriodString2);
                sb4.append(", ad=");
                sb4.append(eventTime.mediaPeriodId.adIndexInAdGroup);
                windowPeriodString = sb4.toString();
            }
        }
        StringBuilder sb5 = new StringBuilder();
        sb5.append(getTimeString(eventTime.realtimeMs - this.startTimeMs));
        sb5.append(", ");
        sb5.append(getTimeString(eventTime.currentPlaybackPositionMs));
        sb5.append(", ");
        sb5.append(windowPeriodString);
        return sb5.toString();
    }

    private static String getTimeString(long timeMs) {
        return timeMs == C.TIME_UNSET ? "?" : TIME_FORMAT.format((double) (((float) timeMs) / 1000.0f));
    }

    private static String getStateString(int state) {
        switch (state) {
            case 1:
                return "IDLE";
            case 2:
                return "BUFFERING";
            case 3:
                return "READY";
            case 4:
                return "ENDED";
            default:
                return "?";
        }
    }

    private static String getFormatSupportString(int formatSupport) {
        switch (formatSupport) {
            case 0:
                return "NO";
            case 1:
                return "NO_UNSUPPORTED_TYPE";
            case 2:
                return "NO_UNSUPPORTED_DRM";
            case 3:
                return "NO_EXCEEDS_CAPABILITIES";
            case 4:
                return "YES";
            default:
                return "?";
        }
    }

    private static String getAdaptiveSupportString(int trackCount, int adaptiveSupport) {
        if (trackCount < 2) {
            return "N/A";
        }
        if (adaptiveSupport == 0) {
            return "NO";
        }
        if (adaptiveSupport == 8) {
            return "YES_NOT_SEAMLESS";
        }
        if (adaptiveSupport != 16) {
            return "?";
        }
        return "YES";
    }

    private static String getTrackStatusString(@Nullable TrackSelection selection, TrackGroup group, int trackIndex) {
        return getTrackStatusString((selection == null || selection.getTrackGroup() != group || selection.indexOf(trackIndex) == -1) ? false : true);
    }

    private static String getTrackStatusString(boolean enabled) {
        return enabled ? "[X]" : "[ ]";
    }

    private static String getRepeatModeString(int repeatMode) {
        switch (repeatMode) {
            case 0:
                return OrbotHelper.STATUS_OFF;
            case 1:
                return "ONE";
            case 2:
                return "ALL";
            default:
                return "?";
        }
    }

    private static String getDiscontinuityReasonString(int reason) {
        switch (reason) {
            case 0:
                return "PERIOD_TRANSITION";
            case 1:
                return "SEEK";
            case 2:
                return "SEEK_ADJUSTMENT";
            case 3:
                return "AD_INSERTION";
            case 4:
                return "INTERNAL";
            default:
                return "?";
        }
    }

    private static String getTimelineChangeReasonString(int reason) {
        switch (reason) {
            case 0:
                return "PREPARED";
            case 1:
                return "RESET";
            case 2:
                return "DYNAMIC";
            default:
                return "?";
        }
    }

    private static String getTrackTypeString(int trackType) {
        String str;
        switch (trackType) {
            case 0:
                return "default";
            case 1:
                return MimeTypes.BASE_TYPE_AUDIO;
            case 2:
                return MimeTypes.BASE_TYPE_VIDEO;
            case 3:
                return MimeTypes.BASE_TYPE_TEXT;
            case 4:
                return TtmlNode.TAG_METADATA;
            case 5:
                return "camera motion";
            case 6:
                return "none";
            default:
                if (trackType >= 10000) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("custom (");
                    sb.append(trackType);
                    sb.append(")");
                    str = sb.toString();
                } else {
                    str = "?";
                }
                return str;
        }
    }
}
