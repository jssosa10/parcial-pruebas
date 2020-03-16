package org.quantumbadger.redreader.image;

import android.graphics.Bitmap;

public final class ThumbnailScaler {
    private static final float maxHeightWidthRatio = 3.0f;

    private static Bitmap scaleAndCrop(Bitmap src, int w, int h, int newWidth) {
        float scaleFactor = ((float) newWidth) / ((float) w);
        Bitmap scaled = Bitmap.createScaledBitmap(src, Math.round(((float) src.getWidth()) * scaleFactor), Math.round(((float) src.getHeight()) * scaleFactor), true);
        Bitmap result = Bitmap.createBitmap(scaled, 0, 0, newWidth, Math.round(((float) h) * scaleFactor));
        if (result != scaled) {
            scaled.recycle();
        }
        return result;
    }

    public static Bitmap scale(Bitmap image, int width) {
        float heightWidthRatio = ((float) image.getHeight()) / ((float) image.getWidth());
        if (heightWidthRatio >= 1.0f && heightWidthRatio <= maxHeightWidthRatio) {
            return Bitmap.createScaledBitmap(image, width, Math.round(((float) width) * heightWidthRatio), true);
        }
        if (heightWidthRatio < 1.0f) {
            return scaleAndCrop(image, image.getHeight(), image.getHeight(), width);
        }
        return scaleAndCrop(image, image.getWidth(), Math.round(((float) image.getWidth()) * maxHeightWidthRatio), width);
    }

    public static Bitmap scaleNoCrop(Bitmap image, int desiredSquareSizePx) {
        float scale = ((float) desiredSquareSizePx) / ((float) Math.max(image.getWidth(), image.getHeight()));
        return Bitmap.createScaledBitmap(image, Math.round(((float) image.getWidth()) * scale), Math.round(((float) image.getHeight()) * scale), true);
    }
}
