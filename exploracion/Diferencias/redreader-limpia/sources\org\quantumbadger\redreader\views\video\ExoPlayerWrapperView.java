package org.quantumbadger.redreader.views.video;

import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player.EventListener;
import com.google.android.exoplayer2.Player.EventListener.CC;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.TimeBar;
import com.google.android.exoplayer2.ui.TimeBar.OnScrubListener;
import java.util.concurrent.atomic.AtomicReference;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;

public class ExoPlayerWrapperView extends FrameLayout {
    private static final String TAG = "ExoPlayerWrapperView";
    @Nullable
    private final RelativeLayout mControlView;
    /* access modifiers changed from: private */
    @NonNull
    public final Listener mListener;
    /* access modifiers changed from: private */
    public boolean mReleased;
    @Nullable
    private final DefaultTimeBar mTimeBarView;
    /* access modifiers changed from: private */
    @NonNull
    public final SimpleExoPlayer mVideoPlayer;

    public interface Listener {
        void onError();
    }

    public ExoPlayerWrapperView(@NonNull Context context, @NonNull MediaSource mediaSource, @NonNull Listener listener, int controlsMarginRightDp) {
        super(context);
        this.mListener = listener;
        this.mVideoPlayer = ExoPlayerFactory.newSimpleInstance(context, (TrackSelector) new DefaultTrackSelector());
        PlayerView videoPlayerView = new PlayerView(context);
        addView(videoPlayerView);
        videoPlayerView.setPlayer(this.mVideoPlayer);
        videoPlayerView.requestFocus();
        this.mVideoPlayer.prepare(mediaSource);
        this.mVideoPlayer.setPlayWhenReady(true);
        videoPlayerView.setUseController(false);
        if (PrefsUtility.pref_behaviour_video_playback_controls(context, PreferenceManager.getDefaultSharedPreferences(context))) {
            this.mControlView = new RelativeLayout(context);
            addView(this.mControlView);
            LinearLayout controlBar = new LinearLayout(context);
            this.mControlView.addView(controlBar);
            controlBar.setBackgroundColor(Color.argb(127, 127, 127, 127));
            controlBar.setOrientation(1);
            LayoutParams controlBarLayoutParams = (LayoutParams) controlBar.getLayoutParams();
            controlBarLayoutParams.width = -2;
            controlBarLayoutParams.height = -2;
            controlBarLayoutParams.addRule(12);
            controlBarLayoutParams.rightMargin = General.dpToPixels(context, (float) controlsMarginRightDp);
            LinearLayout buttons = new LinearLayout(context);
            controlBar.addView(buttons);
            buttons.setOrientation(0);
            LinearLayout.LayoutParams buttonsLayoutParams = (LinearLayout.LayoutParams) buttons.getLayoutParams();
            buttonsLayoutParams.width = -1;
            buttonsLayoutParams.height = -2;
            addButton(createButton(context, this.mControlView, R.drawable.exo_controls_previous, new OnClickListener() {
                public void onClick(View view) {
                    ExoPlayerWrapperView.this.mVideoPlayer.seekTo(0);
                    ExoPlayerWrapperView.this.updateProgress();
                }
            }), buttons);
            addButton(createButton(context, this.mControlView, R.drawable.exo_controls_rewind, new OnClickListener() {
                public void onClick(View view) {
                    ExoPlayerWrapperView.this.mVideoPlayer.seekTo(ExoPlayerWrapperView.this.mVideoPlayer.getCurrentPosition() - 3000);
                    ExoPlayerWrapperView.this.updateProgress();
                }
            }), buttons);
            final AtomicReference<ImageButton> playButton = new AtomicReference<>();
            playButton.set(createButton(context, this.mControlView, R.drawable.exo_controls_pause, new OnClickListener() {
                public void onClick(View view) {
                    ExoPlayerWrapperView.this.mVideoPlayer.setPlayWhenReady(!ExoPlayerWrapperView.this.mVideoPlayer.getPlayWhenReady());
                    if (ExoPlayerWrapperView.this.mVideoPlayer.getPlayWhenReady()) {
                        ((ImageButton) playButton.get()).setImageResource(R.drawable.exo_controls_pause);
                    } else {
                        ((ImageButton) playButton.get()).setImageResource(R.drawable.exo_controls_play);
                    }
                    ExoPlayerWrapperView.this.updateProgress();
                }
            }));
            addButton((ImageButton) playButton.get(), buttons);
            addButton(createButton(context, this.mControlView, R.drawable.exo_controls_fastforward, new OnClickListener() {
                public void onClick(View view) {
                    ExoPlayerWrapperView.this.mVideoPlayer.seekTo(ExoPlayerWrapperView.this.mVideoPlayer.getCurrentPosition() + 3000);
                    ExoPlayerWrapperView.this.updateProgress();
                }
            }), buttons);
            this.mTimeBarView = new DefaultTimeBar(context, null);
            controlBar.addView(this.mTimeBarView);
            LinearLayout.LayoutParams seekBarLayoutParams = (LinearLayout.LayoutParams) this.mTimeBarView.getLayoutParams();
            int marginPx = General.dpToPixels(context, 8.0f);
            seekBarLayoutParams.setMargins(marginPx, marginPx, marginPx, marginPx);
            this.mTimeBarView.addListener(new OnScrubListener() {
                public void onScrubStart(TimeBar timeBar, long position) {
                }

                public void onScrubMove(TimeBar timeBar, long position) {
                    ExoPlayerWrapperView.this.mVideoPlayer.seekTo(position);
                }

                public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
                }
            });
            new Runnable() {
                public void run() {
                    ExoPlayerWrapperView.this.updateProgress();
                    if (!ExoPlayerWrapperView.this.mReleased) {
                        AndroidCommon.UI_THREAD_HANDLER.postDelayed(this, 250);
                    }
                }
            }.run();
            this.mControlView.setVisibility(8);
        } else {
            this.mControlView = null;
            this.mTimeBarView = null;
        }
        videoPlayerView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        this.mVideoPlayer.addListener(new EventListener() {
            public /* synthetic */ void onLoadingChanged(boolean z) {
                CC.$default$onLoadingChanged(this, z);
            }

            public /* synthetic */ void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
                CC.$default$onPlaybackParametersChanged(this, playbackParameters);
            }

            public /* synthetic */ void onPositionDiscontinuity(int i) {
                CC.$default$onPositionDiscontinuity(this, i);
            }

            public /* synthetic */ void onRepeatModeChanged(int i) {
                CC.$default$onRepeatModeChanged(this, i);
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

            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == 4) {
                    ExoPlayerWrapperView.this.mVideoPlayer.seekTo(0);
                }
                ExoPlayerWrapperView.this.updateProgress();
            }

            public void onPlayerError(ExoPlaybackException error) {
                Log.e(ExoPlayerWrapperView.TAG, "ExoPlayer error", error);
                ExoPlayerWrapperView.this.mListener.onError();
            }

            public void onSeekProcessed() {
                ExoPlayerWrapperView.this.updateProgress();
            }
        });
    }

    public void handleTap() {
        RelativeLayout relativeLayout = this.mControlView;
        if (relativeLayout != null) {
            if (relativeLayout.getVisibility() != 0) {
                this.mControlView.setVisibility(0);
            } else {
                this.mControlView.setVisibility(8);
            }
        }
    }

    public void release() {
        if (!this.mReleased) {
            removeAllViews();
            this.mVideoPlayer.release();
            this.mReleased = true;
        }
    }

    private static ImageButton createButton(@NonNull Context context, @NonNull ViewGroup root, @DrawableRes int image, @NonNull OnClickListener clickListener) {
        ImageButton ib = (ImageButton) LayoutInflater.from(context).inflate(R.layout.flat_image_button, root, false);
        int buttonPadding = General.dpToPixels(context, 14.0f);
        ib.setPadding(buttonPadding, buttonPadding, buttonPadding, buttonPadding);
        ib.setImageResource(image);
        ib.setOnClickListener(clickListener);
        return ib;
    }

    private static void addButton(ImageButton button, LinearLayout layout) {
        layout.addView(button);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) button.getLayoutParams();
        layoutParams.width = -2;
        layoutParams.height = -2;
    }

    public void updateProgress() {
        if (this.mTimeBarView != null && !this.mReleased) {
            long duration = this.mVideoPlayer.getDuration();
            if (duration > 0) {
                this.mTimeBarView.setDuration(duration);
                this.mTimeBarView.setPosition(this.mVideoPlayer.getCurrentPosition());
                this.mTimeBarView.setBufferedPosition(this.mVideoPlayer.getBufferedPosition());
                return;
            }
            this.mTimeBarView.setDuration(0);
            this.mTimeBarView.setPosition(0);
            this.mTimeBarView.setBufferedPosition(0);
        }
    }

    public boolean isMuted() {
        return this.mVideoPlayer.getVolume() < 0.01f;
    }

    public void setMuted(boolean mute) {
        this.mVideoPlayer.setVolume(mute ? 0.0f : 1.0f);
    }
}
