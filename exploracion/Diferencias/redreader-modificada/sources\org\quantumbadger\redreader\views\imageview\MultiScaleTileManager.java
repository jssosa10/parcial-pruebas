package org.quantumbadger.redreader.views.imageview;

import android.graphics.Bitmap;
import org.quantumbadger.redreader.views.imageview.ImageViewTileLoader.Listener;

public class MultiScaleTileManager {
    public static final int MAX_SAMPLE_SIZE = 32;
    private int mDesiredScaleIndex = -1;
    private final Object mLock = new Object();
    private final ImageViewTileLoader[] mTileLoaders = new ImageViewTileLoader[(sampleSizeToScaleIndex(32) + 1)];

    public static int scaleIndexToSampleSize(int scaleIndex) {
        return 1 << scaleIndex;
    }

    public static int sampleSizeToScaleIndex(int sampleSize) {
        return Integer.numberOfTrailingZeros(sampleSize);
    }

    public MultiScaleTileManager(ImageTileSource imageTileSource, ImageViewTileLoaderThread thread, int x, int y, Listener listener) {
        int s = 0;
        while (true) {
            ImageViewTileLoader[] imageViewTileLoaderArr = this.mTileLoaders;
            if (s < imageViewTileLoaderArr.length) {
                ImageViewTileLoader imageViewTileLoader = new ImageViewTileLoader(imageTileSource, thread, x, y, scaleIndexToSampleSize(s), listener, this.mLock);
                imageViewTileLoaderArr[s] = imageViewTileLoader;
                s++;
            } else {
                return;
            }
        }
    }

    public Bitmap getAtDesiredScale() {
        return this.mTileLoaders[this.mDesiredScaleIndex].get();
    }

    public void markAsWanted(int desiredScaleIndex) {
        if (desiredScaleIndex != this.mDesiredScaleIndex) {
            this.mDesiredScaleIndex = desiredScaleIndex;
            synchronized (this.mLock) {
                this.mTileLoaders[desiredScaleIndex].markAsWanted();
                for (int s = 0; s < this.mTileLoaders.length; s++) {
                    if (s != desiredScaleIndex) {
                        this.mTileLoaders[s].markAsUnwanted();
                    }
                }
            }
        }
    }

    public void markAsUnwanted() {
        if (this.mDesiredScaleIndex != -1) {
            this.mDesiredScaleIndex = -1;
            synchronized (this.mLock) {
                for (ImageViewTileLoader markAsUnwanted : this.mTileLoaders) {
                    markAsUnwanted.markAsUnwanted();
                }
            }
        }
    }
}
