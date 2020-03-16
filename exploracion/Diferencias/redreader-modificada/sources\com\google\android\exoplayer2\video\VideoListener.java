package com.google.android.exoplayer2.video;

public interface VideoListener {

    /* renamed from: com.google.android.exoplayer2.video.VideoListener$-CC reason: invalid class name */
    public final /* synthetic */ class CC {
        public static void $default$onVideoSizeChanged(VideoListener videoListener, int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        }

        public static void $default$onSurfaceSizeChanged(VideoListener videoListener, int width, int height) {
        }

        public static void $default$onRenderedFirstFrame(VideoListener videoListener) {
        }
    }

    void onRenderedFirstFrame();

    void onSurfaceSizeChanged(int i, int i2);

    void onVideoSizeChanged(int i, int i2, int i3, float f);
}
