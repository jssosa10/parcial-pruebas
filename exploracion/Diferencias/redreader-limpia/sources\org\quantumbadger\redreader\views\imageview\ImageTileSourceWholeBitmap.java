package org.quantumbadger.redreader.views.imageview;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.Log;
import java.io.IOException;
import org.quantumbadger.redreader.common.General;

public class ImageTileSourceWholeBitmap implements ImageTileSource {
    private static final int TILE_SIZE = 512;
    private Bitmap mBitmap = null;
    private final byte[] mData;
    private final int mHeight;
    private final int mWidth;

    public ImageTileSourceWholeBitmap(byte[] data) throws IOException {
        this.mData = data;
        new Options().inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        this.mWidth = bitmap.getWidth();
        this.mHeight = bitmap.getHeight();
        bitmap.recycle();
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public int getTileSize() {
        return 512;
    }

    public int getHTileCount() {
        return General.divideCeil(getWidth(), 512);
    }

    public int getVTileCount() {
        return General.divideCeil(getHeight(), 512);
    }

    public synchronized Bitmap getTile(int sampleSize, int tileX, int tileY) {
        int i = tileX;
        int i2 = tileY;
        synchronized (this) {
            if (this.mBitmap == null) {
                Log.i("ImageTileSourceWholeBitmap", "Loading bitmap.");
                this.mBitmap = BitmapFactory.decodeByteArray(this.mData, 0, this.mData.length);
            }
            int tileStartX = i * 512;
            int tileStartY = i2 * 512;
            int tileEndX = (i + 1) * 512;
            int tileEndY = (i2 + 1) * 512;
            int outputTileSize = 512 / sampleSize;
            if (tileEndX > getWidth() || tileEndY > getHeight()) {
                Bitmap tile = Bitmap.createBitmap(outputTileSize, outputTileSize, Config.ARGB_8888);
                Canvas canvas = new Canvas(tile);
                int tileLimitedEndX = Math.min(tileEndX, getWidth());
                int tileLimitedEndY = Math.min(tileEndY, getHeight());
                int i3 = tileStartX;
                canvas.drawBitmap(this.mBitmap, new Rect(tileStartX, tileStartY, tileLimitedEndX, tileLimitedEndY), new Rect(0, 0, (tileLimitedEndX - tileStartX) / sampleSize, (tileLimitedEndY - tileStartY) / sampleSize), null);
                return tile;
            }
            Bitmap createScaledBitmap = Bitmap.createScaledBitmap(Bitmap.createBitmap(this.mBitmap, tileStartX, tileStartY, tileEndX - tileStartX, tileEndY - tileStartY), outputTileSize, outputTileSize, true);
            return createScaledBitmap;
        }
    }

    public synchronized void dispose() {
        if (this.mBitmap != null && !this.mBitmap.isRecycled()) {
            this.mBitmap.recycle();
        }
        this.mBitmap = null;
    }
}
