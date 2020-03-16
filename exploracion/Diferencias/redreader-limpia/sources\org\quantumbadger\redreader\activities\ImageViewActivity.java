package org.quantumbadger.redreader.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import com.github.lzyzsd.circleprogress.DonutProgress;
import com.google.android.exoplayer2.source.ExtractorMediaSource.Factory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.TransferListener;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.Constants;
import org.quantumbadger.redreader.common.Constants.FileType;
import org.quantumbadger.redreader.common.Constants.Mime;
import org.quantumbadger.redreader.common.Constants.Priority;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.PrefsUtility.GifViewMode;
import org.quantumbadger.redreader.common.PrefsUtility.ImageViewMode;
import org.quantumbadger.redreader.common.PrefsUtility.VideoViewMode;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.fragments.ImageInfoDialog;
import org.quantumbadger.redreader.image.GetAlbumInfoListener;
import org.quantumbadger.redreader.image.GetImageInfoListener;
import org.quantumbadger.redreader.image.GifDecoderThread;
import org.quantumbadger.redreader.image.GifDecoderThread.OnGifLoadedListener;
import org.quantumbadger.redreader.image.ImageInfo;
import org.quantumbadger.redreader.image.ImageInfo.HasAudio;
import org.quantumbadger.redreader.image.ImageInfo.MediaType;
import org.quantumbadger.redreader.image.ImgurAPI.AlbumInfo;
import org.quantumbadger.redreader.reddit.prepared.RedditParsedPost;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost;
import org.quantumbadger.redreader.reddit.things.RedditPost;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL;
import org.quantumbadger.redreader.views.GIFView;
import org.quantumbadger.redreader.views.HorizontalSwipeProgressOverlay;
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener;
import org.quantumbadger.redreader.views.bezelmenu.BezelSwipeOverlay;
import org.quantumbadger.redreader.views.bezelmenu.BezelSwipeOverlay.BezelSwipeListener;
import org.quantumbadger.redreader.views.bezelmenu.SideToolbarOverlay;
import org.quantumbadger.redreader.views.bezelmenu.SideToolbarOverlay.SideToolbarPosition;
import org.quantumbadger.redreader.views.glview.RRGLSurfaceView;
import org.quantumbadger.redreader.views.imageview.BasicGestureHandler;
import org.quantumbadger.redreader.views.imageview.ImageTileSource;
import org.quantumbadger.redreader.views.imageview.ImageTileSourceWholeBitmap;
import org.quantumbadger.redreader.views.imageview.ImageViewDisplayListManager;
import org.quantumbadger.redreader.views.imageview.ImageViewDisplayListManager.Listener;
import org.quantumbadger.redreader.views.liststatus.ErrorView;
import org.quantumbadger.redreader.views.video.ExoPlayerWrapperView;

public class ImageViewActivity extends BaseActivity implements PostSelectionListener, Listener {
    private static final String TAG = "ImageViewActivity";
    /* access modifiers changed from: private */
    public GifDecoderThread gifThread;
    /* access modifiers changed from: private */
    public ImageView imageView;
    /* access modifiers changed from: private */
    public int mAlbumImageIndex;
    /* access modifiers changed from: private */
    public AlbumInfo mAlbumInfo;
    @Nullable
    private LinearLayout mFloatingToolbar;
    private int mGallerySwipeLengthPx;
    /* access modifiers changed from: private */
    public boolean mHaveReverted = false;
    /* access modifiers changed from: private */
    public ImageInfo mImageInfo;
    /* access modifiers changed from: private */
    public ImageViewDisplayListManager mImageViewDisplayerManager;
    /* access modifiers changed from: private */
    public boolean mIsDestroyed = false;
    /* access modifiers changed from: private */
    public boolean mIsPaused = true;
    private FrameLayout mLayout;
    private RedditPost mPost;
    /* access modifiers changed from: private */
    public TextView mProgressText;
    /* access modifiers changed from: private */
    public CacheRequest mRequest;
    private boolean mSwipeCancelled;
    private HorizontalSwipeProgressOverlay mSwipeOverlay;
    /* access modifiers changed from: private */
    public String mUrl;
    /* access modifiers changed from: private */
    public ExoPlayerWrapperView mVideoPlayerWrapper;
    /* access modifiers changed from: private */
    public GLSurfaceView surfaceView;

    /* access modifiers changed from: protected */
    public boolean baseActivityIsToolbarActionBarEnabled() {
        return false;
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        int i;
        final RedditPreparedPost post;
        super.onCreate(savedInstanceState);
        this.mGallerySwipeLengthPx = General.dpToPixels(this, (float) PrefsUtility.pref_behaviour_gallery_swipe_length_dp(this, PreferenceManager.getDefaultSharedPreferences(this)));
        final Intent intent = getIntent();
        this.mUrl = intent.getDataString();
        if (this.mUrl == null) {
            finish();
            return;
        }
        this.mPost = (RedditPost) intent.getParcelableExtra("post");
        if (intent.hasExtra("album")) {
            LinkHandler.getImgurAlbumInfo(this, intent.getStringExtra("album"), Priority.IMAGE_VIEW, 0, new GetAlbumInfoListener() {
                public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                }

                public void onSuccess(final AlbumInfo info2) {
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            ImageViewActivity.this.mAlbumInfo = info2;
                            ImageViewActivity.this.mAlbumImageIndex = intent.getIntExtra("albumImageIndex", 0);
                        }
                    });
                }
            });
        }
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append("Loading URL ");
        sb.append(this.mUrl);
        Log.i(str, sb.toString());
        final DonutProgress progressBar = new DonutProgress(this);
        progressBar.setIndeterminate(true);
        progressBar.setFinishedStrokeColor(Color.rgb(200, 200, 200));
        progressBar.setUnfinishedStrokeColor(Color.rgb(50, 50, 50));
        progressBar.setAspectIndicatorStrokeColor(Color.rgb(200, 200, 200));
        int progressStrokeWidthPx = General.dpToPixels(this, 15.0f);
        progressBar.setUnfinishedStrokeWidth((float) progressStrokeWidthPx);
        progressBar.setFinishedStrokeWidth((float) progressStrokeWidthPx);
        progressBar.setAspectIndicatorStrokeWidth((float) General.dpToPixels(this, 1.0f));
        progressBar.setStartingDegree(-90);
        progressBar.initPainters();
        LinearLayout progressTextLayout = new LinearLayout(this);
        progressTextLayout.setOrientation(1);
        progressTextLayout.setGravity(1);
        progressTextLayout.addView(progressBar);
        int progressDimensionsPx = General.dpToPixels(this, 150.0f);
        progressBar.getLayoutParams().width = progressDimensionsPx;
        progressBar.getLayoutParams().height = progressDimensionsPx;
        this.mProgressText = new TextView(this);
        this.mProgressText.setText(R.string.download_loading);
        this.mProgressText.setAllCaps(true);
        this.mProgressText.setTextSize(2, 18.0f);
        this.mProgressText.setGravity(1);
        progressTextLayout.addView(this.mProgressText);
        this.mProgressText.getLayoutParams().width = -2;
        this.mProgressText.getLayoutParams().height = -2;
        ((MarginLayoutParams) this.mProgressText.getLayoutParams()).topMargin = General.dpToPixels(this, 10.0f);
        RelativeLayout progressLayout = new RelativeLayout(this);
        progressLayout.addView(progressTextLayout);
        ((LayoutParams) progressTextLayout.getLayoutParams()).addRule(13);
        progressTextLayout.getLayoutParams().width = -1;
        progressTextLayout.getLayoutParams().height = -2;
        this.mLayout = new FrameLayout(this);
        this.mLayout.addView(progressLayout);
        LinkHandler.getImageInfo(this, this.mUrl, Priority.IMAGE_VIEW, 0, new GetImageInfoListener() {
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                ImageViewActivity.this.revertToWeb();
            }

            public void onSuccess(ImageInfo info2) {
                URI audioUri;
                String str = ImageViewActivity.TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Got image URL: ");
                sb.append(info2.urlOriginal);
                Log.i(str, sb.toString());
                String str2 = ImageViewActivity.TAG;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Got image Type: ");
                sb2.append(info2.type);
                Log.i(str2, sb2.toString());
                String str3 = ImageViewActivity.TAG;
                StringBuilder sb3 = new StringBuilder();
                sb3.append("Got media Type: ");
                sb3.append(info2.mediaType);
                Log.i(str3, sb3.toString());
                ImageViewActivity.this.mImageInfo = info2;
                URI uri = General.uriFromString(info2.urlOriginal);
                if (uri == null) {
                    ImageViewActivity.this.revertToWeb();
                    return;
                }
                if (info2.urlAudioStream == null) {
                    audioUri = null;
                } else {
                    audioUri = General.uriFromString(info2.urlAudioStream);
                }
                ImageViewActivity.this.openImage(progressBar, uri, audioUri);
            }

            public void onNotAnImage() {
                ImageViewActivity.this.revertToWeb();
            }
        });
        RedditPost redditPost = this.mPost;
        if (redditPost != null) {
            RelativeLayout relativeLayout = progressLayout;
            i = -1;
            int i2 = progressDimensionsPx;
            LinearLayout linearLayout = progressTextLayout;
            post = new RedditPreparedPost(this, CacheManager.getInstance(this), 0, new RedditParsedPost(redditPost, false), -1, false, false);
        } else {
            int i3 = progressDimensionsPx;
            LinearLayout linearLayout2 = progressTextLayout;
            i = -1;
            post = null;
        }
        FrameLayout outerFrame = new FrameLayout(this);
        outerFrame.addView(this.mLayout);
        this.mLayout.getLayoutParams().width = i;
        this.mLayout.getLayoutParams().height = i;
        if (PrefsUtility.pref_appearance_image_viewer_show_floating_toolbar(this, PreferenceManager.getDefaultSharedPreferences(this))) {
            this.mFloatingToolbar = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.floating_toolbar, outerFrame, false);
            outerFrame.addView(this.mFloatingToolbar);
            this.mFloatingToolbar.setVisibility(8);
        }
        if (post != null) {
            final SideToolbarOverlay toolbarOverlay = new SideToolbarOverlay(this);
            BezelSwipeOverlay bezelOverlay = new BezelSwipeOverlay(this, new BezelSwipeListener() {
                public boolean onSwipe(int edge) {
                    SideToolbarOverlay sideToolbarOverlay = toolbarOverlay;
                    sideToolbarOverlay.setContents(post.generateToolbar(ImageViewActivity.this, false, sideToolbarOverlay));
                    toolbarOverlay.show(edge == 0 ? SideToolbarPosition.LEFT : SideToolbarPosition.RIGHT);
                    return true;
                }

                public boolean onTap() {
                    if (!toolbarOverlay.isShown()) {
                        return false;
                    }
                    toolbarOverlay.hide();
                    return true;
                }
            });
            outerFrame.addView(bezelOverlay);
            outerFrame.addView(toolbarOverlay);
            bezelOverlay.getLayoutParams().width = i;
            bezelOverlay.getLayoutParams().height = i;
            toolbarOverlay.getLayoutParams().width = i;
            toolbarOverlay.getLayoutParams().height = i;
        }
        setBaseActivityContentView((View) outerFrame);
    }

    /* access modifiers changed from: private */
    public void setMainView(View v) {
        this.mLayout.removeAllViews();
        this.mLayout.addView(v);
        this.mSwipeOverlay = new HorizontalSwipeProgressOverlay(this);
        this.mLayout.addView(this.mSwipeOverlay);
        v.getLayoutParams().width = -1;
        v.getLayoutParams().height = -1;
    }

    /* access modifiers changed from: private */
    public void onImageLoaded(final ReadableCacheFile cacheFile, @Nullable final ReadableCacheFile audioCacheFile, final String mimetype) {
        if (mimetype == null || (!Mime.isImage(mimetype) && !Mime.isVideo(mimetype))) {
            revertToWeb();
            return;
        }
        try {
            final InputStream cacheFileInputStream = cacheFile.getInputStream();
            if (cacheFileInputStream == null) {
                revertToWeb();
                return;
            }
            ImageInfo imageInfo = this.mImageInfo;
            if (imageInfo != null && ((imageInfo.title != null && this.mImageInfo.title.length() > 0) || (this.mImageInfo.caption != null && this.mImageInfo.caption.length() > 0))) {
                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                    public void run() {
                        ImageViewActivity.this.addFloatingToolbarButton(R.drawable.ic_action_info_dark, new OnClickListener() {
                            public void onClick(View view) {
                                ImageInfoDialog.newInstance(ImageViewActivity.this.mImageInfo).show(ImageViewActivity.this.getSupportFragmentManager(), (String) null);
                            }
                        });
                    }
                });
            }
            if (Mime.isVideo(mimetype)) {
                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                    public void run() {
                        MediaSource mediaSource;
                        if (!ImageViewActivity.this.mIsDestroyed) {
                            ImageViewActivity.this.mRequest = null;
                            ImageViewActivity imageViewActivity = ImageViewActivity.this;
                            VideoViewMode videoViewMode = PrefsUtility.pref_behaviour_videoview_mode(imageViewActivity, PreferenceManager.getDefaultSharedPreferences(imageViewActivity));
                            if (videoViewMode == VideoViewMode.INTERNAL_BROWSER) {
                                ImageViewActivity.this.revertToWeb();
                            } else if (videoViewMode == VideoViewMode.EXTERNAL_BROWSER) {
                                ImageViewActivity.this.openInExternalBrowser();
                            } else if (videoViewMode == VideoViewMode.EXTERNAL_APP_VLC) {
                                Intent intent = new Intent("android.intent.action.VIEW");
                                intent.setClassName("org.videolan.vlc", "org.videolan.vlc.gui.video.VideoPlayerActivity");
                                try {
                                    intent.setDataAndType(cacheFile.getUri(), mimetype);
                                    try {
                                        ImageViewActivity.this.startActivity(intent);
                                    } catch (Throwable t) {
                                        General.quickToast((Context) ImageViewActivity.this, (int) R.string.videoview_mode_app_vlc_launch_failed);
                                        Log.e(ImageViewActivity.TAG, "VLC failed to launch", t);
                                    }
                                    ImageViewActivity.this.finish();
                                } catch (IOException e) {
                                    ImageViewActivity.this.revertToWeb();
                                }
                            } else {
                                try {
                                    Log.i(ImageViewActivity.TAG, "Playing video using ExoPlayer");
                                    RelativeLayout layout = new RelativeLayout(ImageViewActivity.this);
                                    layout.setGravity(17);
                                    DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory((Context) ImageViewActivity.this, Constants.ua(ImageViewActivity.this), (TransferListener) null);
                                    MediaSource videoMediaSource = new Factory(dataSourceFactory).createMediaSource(cacheFile.getUri());
                                    if (audioCacheFile == null) {
                                        mediaSource = videoMediaSource;
                                    } else {
                                        mediaSource = new MergingMediaSource(videoMediaSource, new Factory(dataSourceFactory).createMediaSource(audioCacheFile.getUri()));
                                    }
                                    ImageViewActivity.this.mVideoPlayerWrapper = new ExoPlayerWrapperView(ImageViewActivity.this, mediaSource, new ExoPlayerWrapperView.Listener() {
                                        public void onError() {
                                            ImageViewActivity.this.revertToWeb();
                                        }
                                    }, 128);
                                    layout.addView(ImageViewActivity.this.mVideoPlayerWrapper);
                                    ImageViewActivity.this.setMainView(layout);
                                    layout.getLayoutParams().width = -1;
                                    layout.getLayoutParams().height = -1;
                                    ImageViewActivity.this.mVideoPlayerWrapper.setLayoutParams(new LayoutParams(-1, -1));
                                    BasicGestureHandler gestureHandler = new BasicGestureHandler(ImageViewActivity.this);
                                    ImageViewActivity.this.mVideoPlayerWrapper.setOnTouchListener(gestureHandler);
                                    layout.setOnTouchListener(gestureHandler);
                                    boolean muteByDefault = PrefsUtility.pref_behaviour_video_mute_default(ImageViewActivity.this, PreferenceManager.getDefaultSharedPreferences(ImageViewActivity.this));
                                    ImageViewActivity.this.mVideoPlayerWrapper.setMuted(muteByDefault);
                                    if (!(ImageViewActivity.this.mImageInfo == null || ImageViewActivity.this.mImageInfo.hasAudio == HasAudio.NO_AUDIO)) {
                                        final AtomicReference<ImageButton> muteButton = new AtomicReference<>();
                                        muteButton.set(ImageViewActivity.this.addFloatingToolbarButton(muteByDefault ? R.drawable.ic_volume_off_white_24dp : R.drawable.ic_volume_up_white_24dp, new OnClickListener() {
                                            public void onClick(View view) {
                                                ImageButton button = (ImageButton) muteButton.get();
                                                if (ImageViewActivity.this.mVideoPlayerWrapper.isMuted()) {
                                                    ImageViewActivity.this.mVideoPlayerWrapper.setMuted(false);
                                                    button.setImageResource(R.drawable.ic_volume_up_white_24dp);
                                                    return;
                                                }
                                                ImageViewActivity.this.mVideoPlayerWrapper.setMuted(true);
                                                button.setImageResource(R.drawable.ic_volume_off_white_24dp);
                                            }
                                        }));
                                    }
                                } catch (OutOfMemoryError e2) {
                                    General.quickToast((Context) ImageViewActivity.this, (int) R.string.imageview_oom);
                                    ImageViewActivity.this.revertToWeb();
                                } catch (Throwable th) {
                                    General.quickToast((Context) ImageViewActivity.this, (int) R.string.imageview_invalid_video);
                                    ImageViewActivity.this.revertToWeb();
                                }
                            }
                        }
                    }
                });
            } else if (Mime.isImageGif(mimetype)) {
                GifViewMode gifViewMode = PrefsUtility.pref_behaviour_gifview_mode(this, PreferenceManager.getDefaultSharedPreferences(this));
                if (gifViewMode == GifViewMode.INTERNAL_BROWSER) {
                    revertToWeb();
                } else if (gifViewMode == GifViewMode.EXTERNAL_BROWSER) {
                    openInExternalBrowser();
                } else if (gifViewMode == GifViewMode.INTERNAL_MOVIE) {
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            if (!ImageViewActivity.this.mIsDestroyed) {
                                ImageViewActivity.this.mRequest = null;
                                try {
                                    GIFView gifView = new GIFView(ImageViewActivity.this, cacheFileInputStream);
                                    ImageViewActivity.this.setMainView(gifView);
                                    gifView.setOnTouchListener(new BasicGestureHandler(ImageViewActivity.this));
                                } catch (OutOfMemoryError e) {
                                    General.quickToast((Context) ImageViewActivity.this, (int) R.string.imageview_oom);
                                    ImageViewActivity.this.revertToWeb();
                                } catch (Throwable th) {
                                    General.quickToast((Context) ImageViewActivity.this, (int) R.string.imageview_invalid_gif);
                                    ImageViewActivity.this.revertToWeb();
                                }
                            }
                        }
                    });
                } else {
                    this.gifThread = new GifDecoderThread(cacheFileInputStream, new OnGifLoadedListener() {
                        public void onGifLoaded() {
                            AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                                public void run() {
                                    if (!ImageViewActivity.this.mIsDestroyed) {
                                        ImageViewActivity.this.mRequest = null;
                                        ImageViewActivity.this.imageView = new ImageView(ImageViewActivity.this);
                                        ImageViewActivity.this.imageView.setScaleType(ScaleType.FIT_CENTER);
                                        ImageViewActivity.this.setMainView(ImageViewActivity.this.imageView);
                                        ImageViewActivity.this.gifThread.setView(ImageViewActivity.this.imageView);
                                        ImageViewActivity.this.imageView.setOnTouchListener(new BasicGestureHandler(ImageViewActivity.this));
                                    }
                                }
                            });
                        }

                        public void onOutOfMemory() {
                            General.quickToast((Context) ImageViewActivity.this, (int) R.string.imageview_oom);
                            ImageViewActivity.this.revertToWeb();
                        }

                        public void onGifInvalid() {
                            General.quickToast((Context) ImageViewActivity.this, (int) R.string.imageview_invalid_gif);
                            ImageViewActivity.this.revertToWeb();
                        }
                    });
                    this.gifThread.start();
                }
            } else {
                ImageViewMode imageViewMode = PrefsUtility.pref_behaviour_imageview_mode(this, PreferenceManager.getDefaultSharedPreferences(this));
                if (imageViewMode == ImageViewMode.INTERNAL_BROWSER) {
                    revertToWeb();
                } else if (imageViewMode == ImageViewMode.EXTERNAL_BROWSER) {
                    openInExternalBrowser();
                } else {
                    try {
                        byte[] buf = new byte[((int) cacheFile.getSize())];
                        new DataInputStream(cacheFileInputStream).readFully(buf);
                        try {
                            final ImageTileSource imageTileSource = new ImageTileSourceWholeBitmap(buf);
                            AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                                public void run() {
                                    if (!ImageViewActivity.this.mIsDestroyed) {
                                        ImageViewActivity.this.mRequest = null;
                                        ImageViewActivity imageViewActivity = ImageViewActivity.this;
                                        imageViewActivity.mImageViewDisplayerManager = new ImageViewDisplayListManager(imageViewActivity, imageTileSource, imageViewActivity);
                                        ImageViewActivity imageViewActivity2 = ImageViewActivity.this;
                                        imageViewActivity2.surfaceView = new RRGLSurfaceView(imageViewActivity2, imageViewActivity2.mImageViewDisplayerManager);
                                        ImageViewActivity imageViewActivity3 = ImageViewActivity.this;
                                        imageViewActivity3.setMainView(imageViewActivity3.surfaceView);
                                        if (ImageViewActivity.this.mIsPaused) {
                                            ImageViewActivity.this.surfaceView.onPause();
                                        } else {
                                            ImageViewActivity.this.surfaceView.onResume();
                                        }
                                    }
                                }
                            });
                        } catch (Throwable t) {
                            Log.e(TAG, "Exception when creating ImageTileSource", t);
                            General.quickToast((Context) this, (int) R.string.imageview_decode_failed);
                            revertToWeb();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (OutOfMemoryError e2) {
                        General.quickToast((Context) this, (int) R.string.imageview_oom);
                        revertToWeb();
                    }
                }
            }
        } catch (IOException e3) {
            revertToWeb();
        }
    }

    public void onPostSelected(RedditPreparedPost post) {
        LinkHandler.onLinkClicked(this, post.src.getUrl(), false, post.src.getSrc());
    }

    public void onPostCommentsSelected(RedditPreparedPost post) {
        LinkHandler.onLinkClicked(this, PostCommentListingURL.forPostId(post.src.getIdAlone()).generateJsonUri().toString(), false);
    }

    public void onBackPressed() {
        if (General.onBackPressed()) {
            super.onBackPressed();
        }
    }

    /* access modifiers changed from: private */
    public void revertToWeb() {
        Log.i(TAG, "Using internal browser");
        Runnable r = new Runnable() {
            public void run() {
                if (!ImageViewActivity.this.mHaveReverted) {
                    ImageViewActivity.this.mHaveReverted = true;
                    ImageViewActivity imageViewActivity = ImageViewActivity.this;
                    LinkHandler.onLinkClicked(imageViewActivity, imageViewActivity.mUrl, true);
                    ImageViewActivity.this.finish();
                }
            }
        };
        if (General.isThisUIThread()) {
            r.run();
        } else {
            AndroidCommon.UI_THREAD_HANDLER.post(r);
        }
    }

    /* access modifiers changed from: private */
    public void openInExternalBrowser() {
        Log.i(TAG, "Using external browser");
        Runnable r = new Runnable() {
            public void run() {
                ImageViewActivity imageViewActivity = ImageViewActivity.this;
                LinkHandler.openWebBrowser(imageViewActivity, Uri.parse(imageViewActivity.mUrl), false);
                ImageViewActivity.this.finish();
            }
        };
        if (General.isThisUIThread()) {
            r.run();
        } else {
            AndroidCommon.UI_THREAD_HANDLER.post(r);
        }
    }

    public void onPause() {
        if (!this.mIsPaused) {
            this.mIsPaused = true;
            super.onPause();
            GLSurfaceView gLSurfaceView = this.surfaceView;
            if (gLSurfaceView != null) {
                gLSurfaceView.onPause();
                return;
            }
            return;
        }
        throw new RuntimeException();
    }

    public void onResume() {
        if (this.mIsPaused) {
            this.mIsPaused = false;
            super.onResume();
            GLSurfaceView gLSurfaceView = this.surfaceView;
            if (gLSurfaceView != null) {
                gLSurfaceView.onResume();
                return;
            }
            return;
        }
        throw new RuntimeException();
    }

    public void onDestroy() {
        super.onDestroy();
        this.mIsDestroyed = true;
        CacheRequest cacheRequest = this.mRequest;
        if (cacheRequest != null) {
            cacheRequest.cancel();
        }
        GifDecoderThread gifDecoderThread = this.gifThread;
        if (gifDecoderThread != null) {
            gifDecoderThread.stopPlaying();
        }
        ExoPlayerWrapperView exoPlayerWrapperView = this.mVideoPlayerWrapper;
        if (exoPlayerWrapperView != null) {
            exoPlayerWrapperView.release();
            this.mVideoPlayerWrapper = null;
        }
    }

    public void onSingleTap() {
        if (PrefsUtility.pref_behaviour_video_playback_controls(this, PreferenceManager.getDefaultSharedPreferences(this))) {
            ExoPlayerWrapperView exoPlayerWrapperView = this.mVideoPlayerWrapper;
            if (exoPlayerWrapperView != null) {
                exoPlayerWrapperView.handleTap();
                return;
            }
        }
        finish();
    }

    public void onHorizontalSwipe(float pixels) {
        if (!this.mSwipeCancelled) {
            HorizontalSwipeProgressOverlay horizontalSwipeProgressOverlay = this.mSwipeOverlay;
            if (!(horizontalSwipeProgressOverlay == null || this.mAlbumInfo == null)) {
                horizontalSwipeProgressOverlay.onSwipeUpdate(pixels, (float) this.mGallerySwipeLengthPx);
                int i = this.mGallerySwipeLengthPx;
                if (pixels >= ((float) i)) {
                    this.mSwipeCancelled = true;
                    HorizontalSwipeProgressOverlay horizontalSwipeProgressOverlay2 = this.mSwipeOverlay;
                    if (horizontalSwipeProgressOverlay2 != null) {
                        horizontalSwipeProgressOverlay2.onSwipeEnd();
                    }
                    if (this.mAlbumImageIndex > 0) {
                        LinkHandler.onLinkClicked(this, ((ImageInfo) this.mAlbumInfo.images.get(this.mAlbumImageIndex - 1)).urlOriginal, false, this.mPost, this.mAlbumInfo, this.mAlbumImageIndex - 1);
                        finish();
                    } else {
                        General.quickToast((Context) this, (int) R.string.album_already_first_image);
                    }
                } else if (pixels <= ((float) (-i))) {
                    this.mSwipeCancelled = true;
                    HorizontalSwipeProgressOverlay horizontalSwipeProgressOverlay3 = this.mSwipeOverlay;
                    if (horizontalSwipeProgressOverlay3 != null) {
                        horizontalSwipeProgressOverlay3.onSwipeEnd();
                    }
                    if (this.mAlbumImageIndex < this.mAlbumInfo.images.size() - 1) {
                        LinkHandler.onLinkClicked(this, ((ImageInfo) this.mAlbumInfo.images.get(this.mAlbumImageIndex + 1)).urlOriginal, false, this.mPost, this.mAlbumInfo, this.mAlbumImageIndex + 1);
                        finish();
                    } else {
                        General.quickToast((Context) this, (int) R.string.album_already_last_image);
                    }
                }
            }
        }
    }

    public void onHorizontalSwipeEnd() {
        this.mSwipeCancelled = false;
        HorizontalSwipeProgressOverlay horizontalSwipeProgressOverlay = this.mSwipeOverlay;
        if (horizontalSwipeProgressOverlay != null) {
            horizontalSwipeProgressOverlay.onSwipeEnd();
        }
    }

    public void onImageViewDLMOutOfMemory() {
        if (!this.mHaveReverted) {
            General.quickToast((Context) this, (int) R.string.imageview_oom);
            revertToWeb();
        }
    }

    public void onImageViewDLMException(Throwable t) {
        if (!this.mHaveReverted) {
            General.quickToast((Context) this, (int) R.string.imageview_decode_failed);
            revertToWeb();
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ImageViewDisplayListManager imageViewDisplayListManager = this.mImageViewDisplayerManager;
        if (imageViewDisplayListManager != null) {
            imageViewDisplayListManager.resetTouchState();
        }
    }

    /* access modifiers changed from: private */
    public void openImage(DonutProgress progressBar, URI uri, @Nullable URI audioUri) {
        if (this.mImageInfo.mediaType != null) {
            String str = TAG;
            StringBuilder sb = new StringBuilder();
            sb.append("Media type ");
            sb.append(this.mImageInfo.mediaType);
            sb.append(" detected");
            Log.i(str, sb.toString());
            if (this.mImageInfo.mediaType == MediaType.IMAGE) {
                ImageViewMode imageViewMode = PrefsUtility.pref_behaviour_imageview_mode(this, PreferenceManager.getDefaultSharedPreferences(this));
                if (imageViewMode == ImageViewMode.EXTERNAL_BROWSER) {
                    openInExternalBrowser();
                    return;
                } else if (imageViewMode == ImageViewMode.INTERNAL_BROWSER) {
                    revertToWeb();
                    return;
                }
            } else if (this.mImageInfo.mediaType == MediaType.GIF) {
                GifViewMode gifViewMode = PrefsUtility.pref_behaviour_gifview_mode(this, PreferenceManager.getDefaultSharedPreferences(this));
                if (gifViewMode == GifViewMode.EXTERNAL_BROWSER) {
                    openInExternalBrowser();
                    return;
                } else if (gifViewMode == GifViewMode.INTERNAL_BROWSER) {
                    revertToWeb();
                    return;
                }
            } else if (this.mImageInfo.mediaType == MediaType.VIDEO) {
                VideoViewMode videoViewMode = PrefsUtility.pref_behaviour_videoview_mode(this, PreferenceManager.getDefaultSharedPreferences(this));
                if (videoViewMode == VideoViewMode.EXTERNAL_BROWSER) {
                    openInExternalBrowser();
                    return;
                } else if (videoViewMode == VideoViewMode.INTERNAL_BROWSER) {
                    revertToWeb();
                    return;
                }
            }
        }
        Log.i(TAG, "Proceeding with download");
        makeCacheRequest(progressBar, uri, audioUri);
    }

    /* access modifiers changed from: private */
    public void manageAspectRatioIndicator(DonutProgress progressBar) {
        if (!PrefsUtility.pref_appearance_show_aspect_ratio_indicator(this, PreferenceManager.getDefaultSharedPreferences(this)) || this.mImageInfo.width == null || this.mImageInfo.height == null || this.mImageInfo.width.longValue() <= 0 || this.mImageInfo.height.longValue() <= 0) {
            progressBar.setAspectIndicatorDisplay(false);
            return;
        }
        progressBar.setLoadingImageAspectRatio(((float) this.mImageInfo.width.longValue()) / ((float) this.mImageInfo.height.longValue()));
        progressBar.setAspectIndicatorDisplay(true);
    }

    private void makeCacheRequest(DonutProgress progressBar, URI uri, @Nullable URI audioUri) {
        final DonutProgress donutProgress = progressBar;
        URI uri2 = uri;
        final URI uri3 = audioUri;
        final Object resultLock = new Object();
        final AtomicBoolean failed = new AtomicBoolean(false);
        final AtomicReference<ReadableCacheFile> audio = new AtomicReference<>();
        final AtomicReference<ReadableCacheFile> video = new AtomicReference<>();
        final AtomicReference<String> videoMimetype = new AtomicReference<>();
        AnonymousClass11 r38 = r0;
        CacheManager instance = CacheManager.getInstance(this);
        AnonymousClass11 r0 = new CacheRequest(this, uri2, RedditAccountManager.getAnon(), null, Priority.IMAGE_VIEW, 0, DownloadStrategyIfNotCached.INSTANCE, FileType.IMAGE, 2, false, false, this) {
            /* access modifiers changed from: private */
            public boolean mProgressTextSet = false;
            final /* synthetic */ ImageViewActivity this$0;

            {
                this.this$0 = this$0;
            }

            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                BugReportActivity.handleGlobalError(this.context.getApplicationContext(), new RRError(null, null, t));
            }

            /* access modifiers changed from: protected */
            public void onDownloadNecessary() {
                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                    public void run() {
                        donutProgress.setVisibility(0);
                        donutProgress.setIndeterminate(true);
                        AnonymousClass11.this.this$0.manageAspectRatioIndicator(donutProgress);
                    }
                });
            }

            /* access modifiers changed from: protected */
            public void onDownloadStarted() {
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                synchronized (resultLock) {
                    if (!failed.getAndSet(true)) {
                        final RRError error = General.getGeneralErrorForFailure(this.context, type, t, status, this.url.toString());
                        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                            public void run() {
                                AnonymousClass11.this.this$0.mRequest = null;
                                LinearLayout layout = new LinearLayout(AnonymousClass11.this.context);
                                ErrorView errorView = new ErrorView(AnonymousClass11.this.this$0, error);
                                layout.addView(errorView);
                                errorView.getLayoutParams().width = -1;
                                AnonymousClass11.this.this$0.setMainView(layout);
                            }
                        });
                    }
                }
            }

            /* access modifiers changed from: protected */
            public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
                Handler handler = AndroidCommon.UI_THREAD_HANDLER;
                final boolean z = authorizationInProgress;
                final long j = bytesRead;
                final long j2 = totalBytes;
                AnonymousClass3 r1 = new Runnable() {
                    public void run() {
                        donutProgress.setVisibility(0);
                        donutProgress.setIndeterminate(z);
                        donutProgress.setProgress(((float) ((j * 1000) / j2)) / 1000.0f);
                        AnonymousClass11.this.this$0.manageAspectRatioIndicator(donutProgress);
                        if (!AnonymousClass11.this.mProgressTextSet) {
                            AnonymousClass11.this.this$0.mProgressText.setText(General.bytesToMegabytes(j2));
                            AnonymousClass11.this.mProgressTextSet = true;
                        }
                    }
                };
                handler.post(r1);
            }

            /* access modifiers changed from: protected */
            public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
                synchronized (resultLock) {
                    if (audio.get() == null) {
                        if (uri3 != null) {
                            video.set(cacheFile);
                            videoMimetype.set(mimetype);
                        }
                    }
                    this.this$0.onImageLoaded(cacheFile, (ReadableCacheFile) audio.get(), mimetype);
                }
            }
        };
        AnonymousClass11 r1 = r38;
        this.mRequest = r1;
        instance.makeRequest(r1);
        if (audioUri != null) {
            CacheManager instance2 = CacheManager.getInstance(this);
            final Object obj = resultLock;
            final AtomicBoolean atomicBoolean = failed;
            final AtomicReference atomicReference = video;
            final AtomicReference atomicReference2 = videoMimetype;
            final AtomicReference atomicReference3 = audio;
            AnonymousClass12 r20 = new CacheRequest(this, audioUri, RedditAccountManager.getAnon(), null, Priority.IMAGE_VIEW, 0, DownloadStrategyIfNotCached.INSTANCE, FileType.IMAGE, 2, false, false, this) {
                final /* synthetic */ ImageViewActivity this$0;

                {
                    this.this$0 = this$0;
                }

                /* access modifiers changed from: protected */
                public void onCallbackException(Throwable t) {
                    BugReportActivity.handleGlobalError(this.context.getApplicationContext(), new RRError(null, null, t));
                }

                /* access modifiers changed from: protected */
                public void onDownloadNecessary() {
                }

                /* access modifiers changed from: protected */
                public void onDownloadStarted() {
                }

                /* access modifiers changed from: protected */
                public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                    synchronized (obj) {
                        if (!atomicBoolean.getAndSet(true)) {
                            final RRError error = General.getGeneralErrorForFailure(this.context, type, t, status, this.url.toString());
                            AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                                public void run() {
                                    AnonymousClass12.this.this$0.mRequest = null;
                                    LinearLayout layout = new LinearLayout(AnonymousClass12.this.context);
                                    ErrorView errorView = new ErrorView(AnonymousClass12.this.this$0, error);
                                    layout.addView(errorView);
                                    errorView.getLayoutParams().width = -1;
                                    AnonymousClass12.this.this$0.setMainView(layout);
                                }
                            });
                        }
                    }
                }

                /* access modifiers changed from: protected */
                public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
                }

                /* access modifiers changed from: protected */
                public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
                    synchronized (obj) {
                        if (atomicReference.get() != null) {
                            this.this$0.onImageLoaded((ReadableCacheFile) atomicReference.get(), cacheFile, (String) atomicReference2.get());
                        } else {
                            atomicReference3.set(cacheFile);
                        }
                    }
                }
            };
            this.mRequest = r20;
            instance2.makeRequest(r20);
        }
    }

    /* access modifiers changed from: private */
    @Nullable
    public ImageButton addFloatingToolbarButton(int drawable, @NonNull OnClickListener listener) {
        LinearLayout linearLayout = this.mFloatingToolbar;
        if (linearLayout == null) {
            return null;
        }
        linearLayout.setVisibility(0);
        ImageButton ib = (ImageButton) LayoutInflater.from(this).inflate(R.layout.flat_image_button, this.mFloatingToolbar, false);
        int buttonPadding = General.dpToPixels(this, 10.0f);
        ib.setPadding(buttonPadding, buttonPadding, buttonPadding, buttonPadding);
        ib.setImageResource(drawable);
        ib.setOnClickListener(listener);
        this.mFloatingToolbar.addView(ib);
        return ib;
    }
}
