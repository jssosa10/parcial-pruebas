package com.google.android.exoplayer2.offline;

import android.net.Uri;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.source.TrackGroupArray;
import java.util.List;

public final class ProgressiveDownloadHelper extends DownloadHelper {
    @Nullable
    private final String customCacheKey;
    private final Uri uri;

    public ProgressiveDownloadHelper(Uri uri2) {
        this(uri2, null);
    }

    public ProgressiveDownloadHelper(Uri uri2, @Nullable String customCacheKey2) {
        this.uri = uri2;
        this.customCacheKey = customCacheKey2;
    }

    /* access modifiers changed from: protected */
    public void prepareInternal() {
    }

    public int getPeriodCount() {
        return 1;
    }

    public TrackGroupArray getTrackGroups(int periodIndex) {
        return TrackGroupArray.EMPTY;
    }

    public ProgressiveDownloadAction getDownloadAction(@Nullable byte[] data, List<TrackKey> list) {
        return ProgressiveDownloadAction.createDownloadAction(this.uri, data, this.customCacheKey);
    }

    public ProgressiveDownloadAction getRemoveAction(@Nullable byte[] data) {
        return ProgressiveDownloadAction.createRemoveAction(this.uri, data, this.customCacheKey);
    }
}
