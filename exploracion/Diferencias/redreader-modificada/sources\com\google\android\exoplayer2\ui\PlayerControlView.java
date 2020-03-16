package com.google.android.exoplayer2.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ControlDispatcher;
import com.google.android.exoplayer2.DefaultControlDispatcher;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerLibraryInfo;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.Player.EventListener.CC;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Timeline.Period;
import com.google.android.exoplayer2.Timeline.Window;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.TimeBar.OnScrubListener;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.RepeatModeUtil;
import com.google.android.exoplayer2.util.Util;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

public class PlayerControlView extends FrameLayout {
    public static final int DEFAULT_FAST_FORWARD_MS = 15000;
    public static final int DEFAULT_REPEAT_TOGGLE_MODES = 0;
    public static final int DEFAULT_REWIND_MS = 5000;
    public static final int DEFAULT_SHOW_TIMEOUT_MS = 5000;
    private static final long MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000;
    public static final int MAX_WINDOWS_FOR_MULTI_WINDOW_TIME_BAR = 100;
    private long[] adGroupTimesMs;
    private final ComponentListener componentListener;
    /* access modifiers changed from: private */
    public ControlDispatcher controlDispatcher;
    private final TextView durationView;
    private long[] extraAdGroupTimesMs;
    private boolean[] extraPlayedAdGroups;
    /* access modifiers changed from: private */
    public final View fastForwardButton;
    private int fastForwardMs;
    /* access modifiers changed from: private */
    public final StringBuilder formatBuilder;
    /* access modifiers changed from: private */
    public final Formatter formatter;
    private final Runnable hideAction;
    private long hideAtMs;
    private boolean isAttachedToWindow;
    private boolean multiWindowTimeBar;
    /* access modifiers changed from: private */
    public final View nextButton;
    /* access modifiers changed from: private */
    public final View pauseButton;
    private final Period period;
    /* access modifiers changed from: private */
    public final View playButton;
    /* access modifiers changed from: private */
    @Nullable
    public PlaybackPreparer playbackPreparer;
    private boolean[] playedAdGroups;
    /* access modifiers changed from: private */
    public Player player;
    /* access modifiers changed from: private */
    public final TextView positionView;
    /* access modifiers changed from: private */
    public final View previousButton;
    private final String repeatAllButtonContentDescription;
    private final Drawable repeatAllButtonDrawable;
    private final String repeatOffButtonContentDescription;
    private final Drawable repeatOffButtonDrawable;
    private final String repeatOneButtonContentDescription;
    private final Drawable repeatOneButtonDrawable;
    /* access modifiers changed from: private */
    public final ImageView repeatToggleButton;
    /* access modifiers changed from: private */
    public int repeatToggleModes;
    /* access modifiers changed from: private */
    public final View rewindButton;
    private int rewindMs;
    /* access modifiers changed from: private */
    public boolean scrubbing;
    private boolean showMultiWindowTimeBar;
    private boolean showShuffleButton;
    private int showTimeoutMs;
    /* access modifiers changed from: private */
    public final View shuffleButton;
    private final TimeBar timeBar;
    private final Runnable updateProgressAction;
    private VisibilityListener visibilityListener;
    private final Window window;

    private final class ComponentListener implements EventListener, OnScrubListener, OnClickListener {
        public /* synthetic */ void onLoadingChanged(boolean z) {
            CC.$default$onLoadingChanged(this, z);
        }

        public /* synthetic */ void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            CC.$default$onPlaybackParametersChanged(this, playbackParameters);
        }

        public /* synthetic */ void onPlayerError(ExoPlaybackException exoPlaybackException) {
            CC.$default$onPlayerError(this, exoPlaybackException);
        }

        public /* synthetic */ void onSeekProcessed() {
            CC.$default$onSeekProcessed(this);
        }

        public /* synthetic */ void onTracksChanged(TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {
            CC.$default$onTracksChanged(this, trackGroupArray, trackSelectionArray);
        }

        private ComponentListener() {
        }

        public void onScrubStart(TimeBar timeBar, long position) {
            PlayerControlView.this.scrubbing = true;
        }

        public void onScrubMove(TimeBar timeBar, long position) {
            if (PlayerControlView.this.positionView != null) {
                PlayerControlView.this.positionView.setText(Util.getStringForTime(PlayerControlView.this.formatBuilder, PlayerControlView.this.formatter, position));
            }
        }

        public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
            PlayerControlView.this.scrubbing = false;
            if (!canceled && PlayerControlView.this.player != null) {
                PlayerControlView.this.seekToTimeBarPosition(position);
            }
        }

        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            PlayerControlView.this.updatePlayPauseButton();
            PlayerControlView.this.updateProgress();
        }

        public void onRepeatModeChanged(int repeatMode) {
            PlayerControlView.this.updateRepeatModeButton();
            PlayerControlView.this.updateNavigation();
        }

        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
            PlayerControlView.this.updateShuffleButton();
            PlayerControlView.this.updateNavigation();
        }

        public void onPositionDiscontinuity(int reason) {
            PlayerControlView.this.updateNavigation();
            PlayerControlView.this.updateProgress();
        }

        public void onTimelineChanged(Timeline timeline, @Nullable Object manifest, int reason) {
            PlayerControlView.this.updateNavigation();
            PlayerControlView.this.updateTimeBarMode();
            PlayerControlView.this.updateProgress();
        }

        public void onClick(View view) {
            if (PlayerControlView.this.player == null) {
                return;
            }
            if (PlayerControlView.this.nextButton == view) {
                PlayerControlView.this.next();
            } else if (PlayerControlView.this.previousButton == view) {
                PlayerControlView.this.previous();
            } else if (PlayerControlView.this.fastForwardButton == view) {
                PlayerControlView.this.fastForward();
            } else if (PlayerControlView.this.rewindButton == view) {
                PlayerControlView.this.rewind();
            } else if (PlayerControlView.this.playButton == view) {
                if (PlayerControlView.this.player.getPlaybackState() == 1) {
                    if (PlayerControlView.this.playbackPreparer != null) {
                        PlayerControlView.this.playbackPreparer.preparePlayback();
                    }
                } else if (PlayerControlView.this.player.getPlaybackState() == 4) {
                    PlayerControlView.this.controlDispatcher.dispatchSeekTo(PlayerControlView.this.player, PlayerControlView.this.player.getCurrentWindowIndex(), C.TIME_UNSET);
                }
                PlayerControlView.this.controlDispatcher.dispatchSetPlayWhenReady(PlayerControlView.this.player, true);
            } else if (PlayerControlView.this.pauseButton == view) {
                PlayerControlView.this.controlDispatcher.dispatchSetPlayWhenReady(PlayerControlView.this.player, false);
            } else if (PlayerControlView.this.repeatToggleButton == view) {
                PlayerControlView.this.controlDispatcher.dispatchSetRepeatMode(PlayerControlView.this.player, RepeatModeUtil.getNextRepeatMode(PlayerControlView.this.player.getRepeatMode(), PlayerControlView.this.repeatToggleModes));
            } else if (PlayerControlView.this.shuffleButton == view) {
                PlayerControlView.this.controlDispatcher.dispatchSetShuffleModeEnabled(PlayerControlView.this.player, true ^ PlayerControlView.this.player.getShuffleModeEnabled());
            }
        }
    }

    public interface VisibilityListener {
        void onVisibilityChange(int i);
    }

    static {
        ExoPlayerLibraryInfo.registerModule("goog.exo.ui");
    }

    public PlayerControlView(Context context) {
        this(context, null);
    }

    public PlayerControlView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerControlView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, attrs);
    }

    public PlayerControlView(Context context, AttributeSet attrs, int defStyleAttr, AttributeSet playbackAttrs) {
        super(context, attrs, defStyleAttr);
        int controllerLayoutId = R.layout.exo_player_control_view;
        this.rewindMs = 5000;
        this.fastForwardMs = 15000;
        this.showTimeoutMs = 5000;
        this.repeatToggleModes = 0;
        this.hideAtMs = C.TIME_UNSET;
        this.showShuffleButton = false;
        if (playbackAttrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(playbackAttrs, R.styleable.PlayerControlView, 0, 0);
            try {
                this.rewindMs = a.getInt(R.styleable.PlayerControlView_rewind_increment, this.rewindMs);
                this.fastForwardMs = a.getInt(R.styleable.PlayerControlView_fastforward_increment, this.fastForwardMs);
                this.showTimeoutMs = a.getInt(R.styleable.PlayerControlView_show_timeout, this.showTimeoutMs);
                controllerLayoutId = a.getResourceId(R.styleable.PlayerControlView_controller_layout_id, controllerLayoutId);
                this.repeatToggleModes = getRepeatToggleModes(a, this.repeatToggleModes);
                this.showShuffleButton = a.getBoolean(R.styleable.PlayerControlView_show_shuffle_button, this.showShuffleButton);
            } finally {
                a.recycle();
            }
        }
        this.period = new Period();
        this.window = new Window();
        this.formatBuilder = new StringBuilder();
        this.formatter = new Formatter(this.formatBuilder, Locale.getDefault());
        this.adGroupTimesMs = new long[0];
        this.playedAdGroups = new boolean[0];
        this.extraAdGroupTimesMs = new long[0];
        this.extraPlayedAdGroups = new boolean[0];
        this.componentListener = new ComponentListener();
        this.controlDispatcher = new DefaultControlDispatcher();
        this.updateProgressAction = new Runnable() {
            public final void run() {
                PlayerControlView.this.updateProgress();
            }
        };
        this.hideAction = new Runnable() {
            public final void run() {
                PlayerControlView.this.hide();
            }
        };
        LayoutInflater.from(context).inflate(controllerLayoutId, this);
        setDescendantFocusability(262144);
        this.durationView = (TextView) findViewById(R.id.exo_duration);
        this.positionView = (TextView) findViewById(R.id.exo_position);
        this.timeBar = (TimeBar) findViewById(R.id.exo_progress);
        TimeBar timeBar2 = this.timeBar;
        if (timeBar2 != null) {
            timeBar2.addListener(this.componentListener);
        }
        this.playButton = findViewById(R.id.exo_play);
        View view = this.playButton;
        if (view != null) {
            view.setOnClickListener(this.componentListener);
        }
        this.pauseButton = findViewById(R.id.exo_pause);
        View view2 = this.pauseButton;
        if (view2 != null) {
            view2.setOnClickListener(this.componentListener);
        }
        this.previousButton = findViewById(R.id.exo_prev);
        View view3 = this.previousButton;
        if (view3 != null) {
            view3.setOnClickListener(this.componentListener);
        }
        this.nextButton = findViewById(R.id.exo_next);
        View view4 = this.nextButton;
        if (view4 != null) {
            view4.setOnClickListener(this.componentListener);
        }
        this.rewindButton = findViewById(R.id.exo_rew);
        View view5 = this.rewindButton;
        if (view5 != null) {
            view5.setOnClickListener(this.componentListener);
        }
        this.fastForwardButton = findViewById(R.id.exo_ffwd);
        View view6 = this.fastForwardButton;
        if (view6 != null) {
            view6.setOnClickListener(this.componentListener);
        }
        this.repeatToggleButton = (ImageView) findViewById(R.id.exo_repeat_toggle);
        ImageView imageView = this.repeatToggleButton;
        if (imageView != null) {
            imageView.setOnClickListener(this.componentListener);
        }
        this.shuffleButton = findViewById(R.id.exo_shuffle);
        View view7 = this.shuffleButton;
        if (view7 != null) {
            view7.setOnClickListener(this.componentListener);
        }
        Resources resources = context.getResources();
        this.repeatOffButtonDrawable = resources.getDrawable(R.drawable.exo_controls_repeat_off);
        this.repeatOneButtonDrawable = resources.getDrawable(R.drawable.exo_controls_repeat_one);
        this.repeatAllButtonDrawable = resources.getDrawable(R.drawable.exo_controls_repeat_all);
        this.repeatOffButtonContentDescription = resources.getString(R.string.exo_controls_repeat_off_description);
        this.repeatOneButtonContentDescription = resources.getString(R.string.exo_controls_repeat_one_description);
        this.repeatAllButtonContentDescription = resources.getString(R.string.exo_controls_repeat_all_description);
    }

    private static int getRepeatToggleModes(TypedArray a, int repeatToggleModes2) {
        return a.getInt(R.styleable.PlayerControlView_repeat_toggle_modes, repeatToggleModes2);
    }

    public Player getPlayer() {
        return this.player;
    }

    public void setPlayer(@Nullable Player player2) {
        boolean z = true;
        Assertions.checkState(Looper.myLooper() == Looper.getMainLooper());
        if (!(player2 == null || player2.getApplicationLooper() == Looper.getMainLooper())) {
            z = false;
        }
        Assertions.checkArgument(z);
        Player player3 = this.player;
        if (player3 != player2) {
            if (player3 != null) {
                player3.removeListener(this.componentListener);
            }
            this.player = player2;
            if (player2 != null) {
                player2.addListener(this.componentListener);
            }
            updateAll();
        }
    }

    public void setShowMultiWindowTimeBar(boolean showMultiWindowTimeBar2) {
        this.showMultiWindowTimeBar = showMultiWindowTimeBar2;
        updateTimeBarMode();
    }

    public void setExtraAdGroupMarkers(@Nullable long[] extraAdGroupTimesMs2, @Nullable boolean[] extraPlayedAdGroups2) {
        boolean z = false;
        if (extraAdGroupTimesMs2 == null) {
            this.extraAdGroupTimesMs = new long[0];
            this.extraPlayedAdGroups = new boolean[0];
        } else {
            if (extraAdGroupTimesMs2.length == extraPlayedAdGroups2.length) {
                z = true;
            }
            Assertions.checkArgument(z);
            this.extraAdGroupTimesMs = extraAdGroupTimesMs2;
            this.extraPlayedAdGroups = extraPlayedAdGroups2;
        }
        updateProgress();
    }

    public void setVisibilityListener(VisibilityListener listener) {
        this.visibilityListener = listener;
    }

    public void setPlaybackPreparer(@Nullable PlaybackPreparer playbackPreparer2) {
        this.playbackPreparer = playbackPreparer2;
    }

    public void setControlDispatcher(@Nullable ControlDispatcher controlDispatcher2) {
        this.controlDispatcher = controlDispatcher2 == null ? new DefaultControlDispatcher() : controlDispatcher2;
    }

    public void setRewindIncrementMs(int rewindMs2) {
        this.rewindMs = rewindMs2;
        updateNavigation();
    }

    public void setFastForwardIncrementMs(int fastForwardMs2) {
        this.fastForwardMs = fastForwardMs2;
        updateNavigation();
    }

    public int getShowTimeoutMs() {
        return this.showTimeoutMs;
    }

    public void setShowTimeoutMs(int showTimeoutMs2) {
        this.showTimeoutMs = showTimeoutMs2;
        if (isVisible()) {
            hideAfterTimeout();
        }
    }

    public int getRepeatToggleModes() {
        return this.repeatToggleModes;
    }

    public void setRepeatToggleModes(int repeatToggleModes2) {
        this.repeatToggleModes = repeatToggleModes2;
        Player player2 = this.player;
        if (player2 != null) {
            int currentMode = player2.getRepeatMode();
            if (repeatToggleModes2 == 0 && currentMode != 0) {
                this.controlDispatcher.dispatchSetRepeatMode(this.player, 0);
            } else if (repeatToggleModes2 == 1 && currentMode == 2) {
                this.controlDispatcher.dispatchSetRepeatMode(this.player, 1);
            } else if (repeatToggleModes2 == 2 && currentMode == 1) {
                this.controlDispatcher.dispatchSetRepeatMode(this.player, 2);
            }
        }
        updateRepeatModeButton();
    }

    public boolean getShowShuffleButton() {
        return this.showShuffleButton;
    }

    public void setShowShuffleButton(boolean showShuffleButton2) {
        this.showShuffleButton = showShuffleButton2;
        updateShuffleButton();
    }

    public void show() {
        if (!isVisible()) {
            setVisibility(0);
            VisibilityListener visibilityListener2 = this.visibilityListener;
            if (visibilityListener2 != null) {
                visibilityListener2.onVisibilityChange(getVisibility());
            }
            updateAll();
            requestPlayPauseFocus();
        }
        hideAfterTimeout();
    }

    public void hide() {
        if (isVisible()) {
            setVisibility(8);
            VisibilityListener visibilityListener2 = this.visibilityListener;
            if (visibilityListener2 != null) {
                visibilityListener2.onVisibilityChange(getVisibility());
            }
            removeCallbacks(this.updateProgressAction);
            removeCallbacks(this.hideAction);
            this.hideAtMs = C.TIME_UNSET;
        }
    }

    public boolean isVisible() {
        return getVisibility() == 0;
    }

    private void hideAfterTimeout() {
        removeCallbacks(this.hideAction);
        if (this.showTimeoutMs > 0) {
            long uptimeMillis = SystemClock.uptimeMillis();
            int i = this.showTimeoutMs;
            this.hideAtMs = uptimeMillis + ((long) i);
            if (this.isAttachedToWindow) {
                postDelayed(this.hideAction, (long) i);
                return;
            }
            return;
        }
        this.hideAtMs = C.TIME_UNSET;
    }

    private void updateAll() {
        updatePlayPauseButton();
        updateNavigation();
        updateRepeatModeButton();
        updateShuffleButton();
        updateProgress();
    }

    /* access modifiers changed from: private */
    public void updatePlayPauseButton() {
        if (isVisible() && this.isAttachedToWindow) {
            boolean requestPlayPauseFocus = false;
            boolean playing = isPlaying();
            View view = this.playButton;
            int i = 8;
            boolean z = true;
            if (view != null) {
                requestPlayPauseFocus = false | (playing && view.isFocused());
                this.playButton.setVisibility(playing ? 8 : 0);
            }
            View view2 = this.pauseButton;
            if (view2 != null) {
                if (playing || !view2.isFocused()) {
                    z = false;
                }
                requestPlayPauseFocus |= z;
                View view3 = this.pauseButton;
                if (playing) {
                    i = 0;
                }
                view3.setVisibility(i);
            }
            if (requestPlayPauseFocus) {
                requestPlayPauseFocus();
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateNavigation() {
        if (isVisible() && this.isAttachedToWindow) {
            Player player2 = this.player;
            Timeline timeline = player2 != null ? player2.getCurrentTimeline() : null;
            boolean z = true;
            boolean isSeekable = false;
            boolean enablePrevious = false;
            boolean enableNext = false;
            if ((timeline != null && !timeline.isEmpty()) && !this.player.isPlayingAd()) {
                timeline.getWindow(this.player.getCurrentWindowIndex(), this.window);
                isSeekable = this.window.isSeekable;
                enablePrevious = isSeekable || !this.window.isDynamic || this.player.hasPrevious();
                enableNext = this.window.isDynamic || this.player.hasNext();
            }
            setButtonEnabled(enablePrevious, this.previousButton);
            setButtonEnabled(enableNext, this.nextButton);
            setButtonEnabled(this.fastForwardMs > 0 && isSeekable, this.fastForwardButton);
            if (this.rewindMs <= 0 || !isSeekable) {
                z = false;
            }
            setButtonEnabled(z, this.rewindButton);
            TimeBar timeBar2 = this.timeBar;
            if (timeBar2 != null) {
                timeBar2.setEnabled(isSeekable);
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateRepeatModeButton() {
        if (isVisible() && this.isAttachedToWindow) {
            ImageView imageView = this.repeatToggleButton;
            if (imageView != null) {
                if (this.repeatToggleModes == 0) {
                    imageView.setVisibility(8);
                } else if (this.player == null) {
                    setButtonEnabled(false, imageView);
                } else {
                    setButtonEnabled(true, imageView);
                    switch (this.player.getRepeatMode()) {
                        case 0:
                            this.repeatToggleButton.setImageDrawable(this.repeatOffButtonDrawable);
                            this.repeatToggleButton.setContentDescription(this.repeatOffButtonContentDescription);
                            break;
                        case 1:
                            this.repeatToggleButton.setImageDrawable(this.repeatOneButtonDrawable);
                            this.repeatToggleButton.setContentDescription(this.repeatOneButtonContentDescription);
                            break;
                        case 2:
                            this.repeatToggleButton.setImageDrawable(this.repeatAllButtonDrawable);
                            this.repeatToggleButton.setContentDescription(this.repeatAllButtonContentDescription);
                            break;
                    }
                    this.repeatToggleButton.setVisibility(0);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateShuffleButton() {
        if (isVisible() && this.isAttachedToWindow) {
            View view = this.shuffleButton;
            if (view != null) {
                if (!this.showShuffleButton) {
                    view.setVisibility(8);
                } else {
                    Player player2 = this.player;
                    if (player2 == null) {
                        setButtonEnabled(false, view);
                    } else {
                        view.setAlpha(player2.getShuffleModeEnabled() ? 1.0f : 0.3f);
                        this.shuffleButton.setEnabled(true);
                        this.shuffleButton.setVisibility(0);
                    }
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void updateTimeBarMode() {
        Player player2 = this.player;
        if (player2 != null) {
            this.multiWindowTimeBar = this.showMultiWindowTimeBar && canShowMultiWindowTimeBar(player2.getCurrentTimeline(), this.window);
        }
    }

    /* access modifiers changed from: private */
    public void updateProgress() {
        long delayMs;
        long currentWindowTimeBarOffsetMs;
        long bufferedPosition;
        long adGroupTimeInPeriodUs;
        if (isVisible() && this.isAttachedToWindow) {
            long position = 0;
            long bufferedPosition2 = 0;
            long duration = 0;
            Player player2 = this.player;
            if (player2 != null) {
                long durationUs = 0;
                int adGroupCount = 0;
                Timeline timeline = player2.getCurrentTimeline();
                if (!timeline.isEmpty()) {
                    int currentWindowIndex = this.player.getCurrentWindowIndex();
                    int firstWindowIndex = this.multiWindowTimeBar ? 0 : currentWindowIndex;
                    int lastWindowIndex = this.multiWindowTimeBar ? timeline.getWindowCount() - 1 : currentWindowIndex;
                    int i = firstWindowIndex;
                    currentWindowTimeBarOffsetMs = 0;
                    while (true) {
                        if (i > lastWindowIndex) {
                            long j = bufferedPosition2;
                            break;
                        }
                        if (i == currentWindowIndex) {
                            currentWindowTimeBarOffsetMs = C.usToMs(durationUs);
                        }
                        timeline.getWindow(i, this.window);
                        long position2 = position;
                        if (this.window.durationUs == C.TIME_UNSET) {
                            Assertions.checkState(!this.multiWindowTimeBar);
                            long j2 = bufferedPosition2;
                            break;
                        }
                        for (int j3 = this.window.firstPeriodIndex; j3 <= this.window.lastPeriodIndex; j3++) {
                            timeline.getPeriod(j3, this.period);
                            int periodAdGroupCount = this.period.getAdGroupCount();
                            int adGroupIndex = 0;
                            while (adGroupIndex < periodAdGroupCount) {
                                int periodAdGroupCount2 = periodAdGroupCount;
                                long adGroupTimeInPeriodUs2 = this.period.getAdGroupTimeUs(adGroupIndex);
                                if (adGroupTimeInPeriodUs2 == Long.MIN_VALUE) {
                                    bufferedPosition = bufferedPosition2;
                                    if (this.period.durationUs == C.TIME_UNSET) {
                                        adGroupIndex++;
                                        periodAdGroupCount = periodAdGroupCount2;
                                        bufferedPosition2 = bufferedPosition;
                                    } else {
                                        adGroupTimeInPeriodUs = this.period.durationUs;
                                    }
                                } else {
                                    bufferedPosition = bufferedPosition2;
                                    adGroupTimeInPeriodUs = adGroupTimeInPeriodUs2;
                                }
                                long adGroupTimeInWindowUs = adGroupTimeInPeriodUs + this.period.getPositionInWindowUs();
                                if (adGroupTimeInWindowUs >= 0) {
                                    long j4 = adGroupTimeInPeriodUs;
                                    if (adGroupTimeInWindowUs <= this.window.durationUs) {
                                        long[] jArr = this.adGroupTimesMs;
                                        if (adGroupCount == jArr.length) {
                                            int newLength = jArr.length == 0 ? 1 : jArr.length * 2;
                                            this.adGroupTimesMs = Arrays.copyOf(this.adGroupTimesMs, newLength);
                                            this.playedAdGroups = Arrays.copyOf(this.playedAdGroups, newLength);
                                        }
                                        this.adGroupTimesMs[adGroupCount] = C.usToMs(durationUs + adGroupTimeInWindowUs);
                                        this.playedAdGroups[adGroupCount] = this.period.hasPlayedAdGroup(adGroupIndex);
                                        adGroupCount++;
                                    }
                                }
                                adGroupIndex++;
                                periodAdGroupCount = periodAdGroupCount2;
                                bufferedPosition2 = bufferedPosition;
                            }
                            long j5 = bufferedPosition2;
                        }
                        durationUs += this.window.durationUs;
                        i++;
                        position = position2;
                    }
                } else {
                    currentWindowTimeBarOffsetMs = 0;
                }
                duration = C.usToMs(durationUs);
                position = currentWindowTimeBarOffsetMs + this.player.getContentPosition();
                bufferedPosition2 = currentWindowTimeBarOffsetMs + this.player.getContentBufferedPosition();
                if (this.timeBar != null) {
                    int extraAdGroupCount = this.extraAdGroupTimesMs.length;
                    int totalAdGroupCount = adGroupCount + extraAdGroupCount;
                    long[] jArr2 = this.adGroupTimesMs;
                    if (totalAdGroupCount > jArr2.length) {
                        this.adGroupTimesMs = Arrays.copyOf(jArr2, totalAdGroupCount);
                        this.playedAdGroups = Arrays.copyOf(this.playedAdGroups, totalAdGroupCount);
                    }
                    System.arraycopy(this.extraAdGroupTimesMs, 0, this.adGroupTimesMs, adGroupCount, extraAdGroupCount);
                    System.arraycopy(this.extraPlayedAdGroups, 0, this.playedAdGroups, adGroupCount, extraAdGroupCount);
                    this.timeBar.setAdGroupTimesMs(this.adGroupTimesMs, this.playedAdGroups, totalAdGroupCount);
                }
            }
            TextView textView = this.durationView;
            if (textView != null) {
                textView.setText(Util.getStringForTime(this.formatBuilder, this.formatter, duration));
            }
            TextView textView2 = this.positionView;
            if (textView2 != null && !this.scrubbing) {
                textView2.setText(Util.getStringForTime(this.formatBuilder, this.formatter, position));
            }
            TimeBar timeBar2 = this.timeBar;
            if (timeBar2 != null) {
                timeBar2.setPosition(position);
                this.timeBar.setBufferedPosition(bufferedPosition2);
                this.timeBar.setDuration(duration);
            }
            removeCallbacks(this.updateProgressAction);
            Player player3 = this.player;
            int playbackState = player3 == null ? 1 : player3.getPlaybackState();
            if (!(playbackState == 1 || playbackState == 4)) {
                if (!this.player.getPlayWhenReady() || playbackState != 3) {
                    delayMs = 1000;
                } else {
                    float playbackSpeed = this.player.getPlaybackParameters().speed;
                    if (playbackSpeed <= 0.1f) {
                        delayMs = 1000;
                    } else if (playbackSpeed <= 5.0f) {
                        long mediaTimeUpdatePeriodMs = (long) (1000 / Math.max(1, Math.round(1.0f / playbackSpeed)));
                        long mediaTimeDelayMs = mediaTimeUpdatePeriodMs - (position % mediaTimeUpdatePeriodMs);
                        if (mediaTimeDelayMs < mediaTimeUpdatePeriodMs / 5) {
                            mediaTimeDelayMs += mediaTimeUpdatePeriodMs;
                        }
                        delayMs = playbackSpeed == 1.0f ? mediaTimeDelayMs : (long) (((float) mediaTimeDelayMs) / playbackSpeed);
                    } else {
                        delayMs = 200;
                    }
                }
                postDelayed(this.updateProgressAction, delayMs);
            }
        }
    }

    private void requestPlayPauseFocus() {
        boolean playing = isPlaying();
        if (!playing) {
            View view = this.playButton;
            if (view != null) {
                view.requestFocus();
                return;
            }
        }
        if (playing) {
            View view2 = this.pauseButton;
            if (view2 != null) {
                view2.requestFocus();
            }
        }
    }

    private void setButtonEnabled(boolean enabled, View view) {
        if (view != null) {
            view.setEnabled(enabled);
            view.setAlpha(enabled ? 1.0f : 0.3f);
            view.setVisibility(0);
        }
    }

    /* access modifiers changed from: private */
    public void previous() {
        Timeline timeline = this.player.getCurrentTimeline();
        if (!timeline.isEmpty() && !this.player.isPlayingAd()) {
            timeline.getWindow(this.player.getCurrentWindowIndex(), this.window);
            int previousWindowIndex = this.player.getPreviousWindowIndex();
            if (previousWindowIndex == -1 || (this.player.getCurrentPosition() > MAX_POSITION_FOR_SEEK_TO_PREVIOUS && (!this.window.isDynamic || this.window.isSeekable))) {
                seekTo(0);
            } else {
                seekTo(previousWindowIndex, C.TIME_UNSET);
            }
        }
    }

    /* access modifiers changed from: private */
    public void next() {
        Timeline timeline = this.player.getCurrentTimeline();
        if (!timeline.isEmpty() && !this.player.isPlayingAd()) {
            int windowIndex = this.player.getCurrentWindowIndex();
            int nextWindowIndex = this.player.getNextWindowIndex();
            if (nextWindowIndex != -1) {
                seekTo(nextWindowIndex, C.TIME_UNSET);
            } else if (timeline.getWindow(windowIndex, this.window).isDynamic) {
                seekTo(windowIndex, C.TIME_UNSET);
            }
        }
    }

    /* access modifiers changed from: private */
    public void rewind() {
        if (this.rewindMs > 0) {
            seekTo(Math.max(this.player.getCurrentPosition() - ((long) this.rewindMs), 0));
        }
    }

    /* access modifiers changed from: private */
    public void fastForward() {
        if (this.fastForwardMs > 0) {
            long durationMs = this.player.getDuration();
            long seekPositionMs = this.player.getCurrentPosition() + ((long) this.fastForwardMs);
            if (durationMs != C.TIME_UNSET) {
                seekPositionMs = Math.min(seekPositionMs, durationMs);
            }
            seekTo(seekPositionMs);
        }
    }

    private void seekTo(long positionMs) {
        seekTo(this.player.getCurrentWindowIndex(), positionMs);
    }

    private void seekTo(int windowIndex, long positionMs) {
        if (!this.controlDispatcher.dispatchSeekTo(this.player, windowIndex, positionMs)) {
            updateProgress();
        }
    }

    /* access modifiers changed from: private */
    public void seekToTimeBarPosition(long positionMs) {
        int windowIndex;
        Timeline timeline = this.player.getCurrentTimeline();
        if (this.multiWindowTimeBar && !timeline.isEmpty()) {
            int windowCount = timeline.getWindowCount();
            windowIndex = 0;
            while (true) {
                long windowDurationMs = timeline.getWindow(windowIndex, this.window).getDurationMs();
                if (positionMs < windowDurationMs) {
                    break;
                } else if (windowIndex == windowCount - 1) {
                    positionMs = windowDurationMs;
                    break;
                } else {
                    positionMs -= windowDurationMs;
                    windowIndex++;
                }
            }
        } else {
            windowIndex = this.player.getCurrentWindowIndex();
        }
        seekTo(windowIndex, positionMs);
    }

    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.isAttachedToWindow = true;
        long j = this.hideAtMs;
        if (j != C.TIME_UNSET) {
            long delayMs = j - SystemClock.uptimeMillis();
            if (delayMs <= 0) {
                hide();
            } else {
                postDelayed(this.hideAction, delayMs);
            }
        } else if (isVisible()) {
            hideAfterTimeout();
        }
        updateAll();
    }

    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.isAttachedToWindow = false;
        removeCallbacks(this.updateProgressAction);
        removeCallbacks(this.hideAction);
    }

    public final boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == 0) {
            removeCallbacks(this.hideAction);
        } else if (ev.getAction() == 1) {
            hideAfterTimeout();
        }
        return super.dispatchTouchEvent(ev);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return dispatchMediaKeyEvent(event) || super.dispatchKeyEvent(event);
    }

    public boolean dispatchMediaKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (this.player == null || !isHandledMediaKey(keyCode)) {
            return false;
        }
        if (event.getAction() == 0) {
            if (keyCode != 90) {
                if (keyCode != 89) {
                    if (event.getRepeatCount() == 0) {
                        switch (keyCode) {
                            case 85:
                                ControlDispatcher controlDispatcher2 = this.controlDispatcher;
                                Player player2 = this.player;
                                controlDispatcher2.dispatchSetPlayWhenReady(player2, !player2.getPlayWhenReady());
                                break;
                            case 87:
                                next();
                                break;
                            case 88:
                                previous();
                                break;
                            case 126:
                                this.controlDispatcher.dispatchSetPlayWhenReady(this.player, true);
                                break;
                            case 127:
                                this.controlDispatcher.dispatchSetPlayWhenReady(this.player, false);
                                break;
                        }
                    }
                } else {
                    rewind();
                }
            } else {
                fastForward();
            }
        }
        return true;
    }

    private boolean isPlaying() {
        Player player2 = this.player;
        if (player2 == null || player2.getPlaybackState() == 4 || this.player.getPlaybackState() == 1 || !this.player.getPlayWhenReady()) {
            return false;
        }
        return true;
    }

    @SuppressLint({"InlinedApi"})
    private static boolean isHandledMediaKey(int keyCode) {
        return keyCode == 90 || keyCode == 89 || keyCode == 85 || keyCode == 126 || keyCode == 127 || keyCode == 87 || keyCode == 88;
    }

    private static boolean canShowMultiWindowTimeBar(Timeline timeline, Window window2) {
        if (timeline.getWindowCount() > 100) {
            return false;
        }
        int windowCount = timeline.getWindowCount();
        for (int i = 0; i < windowCount; i++) {
            if (timeline.getWindow(i, window2).durationUs == C.TIME_UNSET) {
                return false;
            }
        }
        return true;
    }
}
