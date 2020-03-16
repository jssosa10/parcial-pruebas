package org.quantumbadger.redreader.views.imageview;

import android.graphics.Bitmap;

public interface ImageTileSource {
    void dispose();

    int getHTileCount();

    int getHeight();

    Bitmap getTile(int i, int i2, int i3);

    int getTileSize();

    int getVTileCount();

    int getWidth();
}
