package org.quantumbadger.redreader.image;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.ImageView;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import jp.tomorrowkey.android.gifplayer.GifDecoder;

public class GifDecoderThread extends Thread {
    private final Handler handler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            if (GifDecoderThread.this.playing && GifDecoderThread.this.view != null) {
                GifDecoderThread.this.view.setImageBitmap((Bitmap) msg.obj);
            }
        }
    };
    /* access modifiers changed from: private */
    public final InputStream is;
    private final OnGifLoadedListener listener;
    /* access modifiers changed from: private */
    public volatile boolean playing = true;
    /* access modifiers changed from: private */
    public ImageView view;

    public interface OnGifLoadedListener {
        void onGifInvalid();

        void onGifLoaded();

        void onOutOfMemory();
    }

    public void setView(ImageView view2) {
        this.view = view2;
    }

    public GifDecoderThread(InputStream is2, OnGifLoadedListener listener2) {
        super("GIF playing thread");
        this.is = is2;
        this.listener = listener2;
    }

    public void stopPlaying() {
        this.playing = false;
        interrupt();
        try {
            this.is.close();
        } catch (Throwable th) {
        }
    }

    public void run() {
        final AtomicBoolean loaded = new AtomicBoolean(false);
        final AtomicBoolean failed = new AtomicBoolean(false);
        GifDecoder decoder = new GifDecoder();
        final GifDecoder gifDecoder = decoder;
        AnonymousClass2 r0 = new Thread("GIF decoding thread") {
            public void run() {
                try {
                    gifDecoder.read(GifDecoderThread.this.is);
                    loaded.set(true);
                } catch (Throwable t) {
                    t.printStackTrace();
                    failed.set(true);
                }
            }
        };
        r0.start();
        try {
            if (this.playing) {
                this.listener.onGifLoaded();
                int frame = 0;
                while (this.playing) {
                    while (decoder.getFrameCount() <= frame + 1 && !loaded.get() && !failed.get()) {
                        try {
                            sleep(100);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    int frame2 = frame % decoder.getFrameCount();
                    Bitmap img = decoder.getFrame(frame2);
                    Message msg = Message.obtain();
                    msg.obj = img;
                    this.handler.sendMessage(msg);
                    try {
                        sleep((long) Math.max(32, decoder.getDelay(frame2)));
                        if (failed.get()) {
                            this.listener.onGifInvalid();
                            return;
                        }
                        frame = frame2 + 1;
                    } catch (InterruptedException e2) {
                        return;
                    }
                }
            }
        } catch (OutOfMemoryError e3) {
            this.listener.onOutOfMemory();
        } catch (Throwable th) {
            this.listener.onGifInvalid();
        }
    }
}
