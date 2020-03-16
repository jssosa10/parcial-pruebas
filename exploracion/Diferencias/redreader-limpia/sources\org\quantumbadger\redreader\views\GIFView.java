package org.quantumbadger.redreader.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class GIFView extends View {
    private final Movie mMovie;
    private long movieStart;
    private final Paint paint = new Paint();

    public GIFView(Context context, InputStream is) {
        super(context);
        setLayerType(1, null);
        byte[] data = streamToBytes(is);
        this.mMovie = Movie.decodeByteArray(data, 0, data.length);
        if (this.mMovie.duration() >= 1) {
            this.paint.setAntiAlias(true);
            this.paint.setFilterBitmap(true);
            return;
        }
        throw new RuntimeException("Invalid GIF");
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        canvas.drawColor(0);
        super.onDraw(canvas);
        long now = SystemClock.uptimeMillis();
        float scale = Math.min(((float) getWidth()) / ((float) this.mMovie.width()), ((float) getHeight()) / ((float) this.mMovie.height()));
        canvas.scale(scale, scale);
        canvas.translate(((((float) getWidth()) / scale) - ((float) this.mMovie.width())) / 2.0f, ((((float) getHeight()) / scale) - ((float) this.mMovie.height())) / 2.0f);
        if (this.movieStart == 0) {
            this.movieStart = (long) ((int) now);
        }
        Movie movie = this.mMovie;
        movie.setTime((int) ((now - this.movieStart) % ((long) movie.duration())));
        this.mMovie.draw(canvas, 0.0f, 0.0f, this.paint);
        invalidate();
    }

    private static byte[] streamToBytes(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        byte[] buffer = new byte[1024];
        while (true) {
            try {
                int read = is.read(buffer);
                int len = read;
                if (read < 0) {
                    return baos.toByteArray();
                }
                baos.write(buffer, 0, len);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
