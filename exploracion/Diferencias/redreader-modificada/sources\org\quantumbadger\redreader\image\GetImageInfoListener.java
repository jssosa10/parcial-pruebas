package org.quantumbadger.redreader.image;

public interface GetImageInfoListener {
    void onFailure(int i, Throwable th, Integer num, String str);

    void onNotAnImage();

    void onSuccess(ImageInfo imageInfo);
}
