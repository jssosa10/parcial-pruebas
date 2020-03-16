package org.quantumbadger.redreader.image;

import org.quantumbadger.redreader.image.ImgurAPI.AlbumInfo;

public interface GetAlbumInfoListener {
    void onFailure(int i, Throwable th, Integer num, String str);

    void onSuccess(AlbumInfo albumInfo);
}
