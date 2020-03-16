package org.quantumbadger.redreader.adapters;

import android.preference.PreferenceManager;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView.Adapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceThumbnailsShow;
import org.quantumbadger.redreader.image.ImageInfo;
import org.quantumbadger.redreader.image.ImgurAPI.AlbumInfo;
import org.quantumbadger.redreader.viewholders.VH3TextIcon;

public class AlbumAdapter extends Adapter<VH3TextIcon> {
    /* access modifiers changed from: private */
    public final AppCompatActivity activity;
    /* access modifiers changed from: private */
    public final AlbumInfo albumInfo;

    public AlbumAdapter(AppCompatActivity activity2, AlbumInfo albumInfo2) {
        this.activity = activity2;
        this.albumInfo = albumInfo2;
    }

    public VH3TextIcon onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VH3TextIcon(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_3_text_icon, parent, false));
    }

    public void onBindViewHolder(VH3TextIcon vh, int position) {
        String subtitle;
        ImageInfo imageInfo;
        final VH3TextIcon vH3TextIcon;
        VH3TextIcon vH3TextIcon2 = vh;
        int i = position;
        final long bindingId = vH3TextIcon2.bindingId + 1;
        vH3TextIcon2.bindingId = bindingId;
        ImageInfo imageInfo2 = (ImageInfo) this.albumInfo.images.get(i);
        if (imageInfo2.title == null || imageInfo2.title.trim().isEmpty()) {
            TextView textView = vH3TextIcon2.text;
            StringBuilder sb = new StringBuilder();
            sb.append("Image ");
            sb.append(i + 1);
            textView.setText(sb.toString());
        } else {
            TextView textView2 = vH3TextIcon2.text;
            StringBuilder sb2 = new StringBuilder();
            sb2.append(i + 1);
            sb2.append(". ");
            sb2.append(imageInfo2.title.trim());
            textView2.setText(sb2.toString());
        }
        String subtitle2 = "";
        if (imageInfo2.type != null) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append(subtitle2);
            sb3.append(imageInfo2.type);
            subtitle2 = sb3.toString();
        }
        if (!(imageInfo2.width == null || imageInfo2.height == null)) {
            if (!subtitle2.isEmpty()) {
                StringBuilder sb4 = new StringBuilder();
                sb4.append(subtitle2);
                sb4.append(", ");
                subtitle2 = sb4.toString();
            }
            StringBuilder sb5 = new StringBuilder();
            sb5.append(subtitle2);
            sb5.append(imageInfo2.width);
            sb5.append("x");
            sb5.append(imageInfo2.height);
            subtitle2 = sb5.toString();
        }
        boolean z = true;
        if (imageInfo2.size != null) {
            if (!subtitle2.isEmpty()) {
                StringBuilder sb6 = new StringBuilder();
                sb6.append(subtitle2);
                sb6.append(", ");
                subtitle2 = sb6.toString();
            }
            long size = imageInfo2.size.longValue();
            if (size < PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED) {
                StringBuilder sb7 = new StringBuilder();
                sb7.append(subtitle2);
                sb7.append(String.format("%.1f kB", new Object[]{Float.valueOf(((float) size) / 1024.0f)}));
                subtitle = sb7.toString();
            } else {
                StringBuilder sb8 = new StringBuilder();
                sb8.append(subtitle2);
                sb8.append(String.format("%.1f MB", new Object[]{Float.valueOf(((float) size) / 1048576.0f)}));
                subtitle = sb8.toString();
            }
        } else {
            subtitle = subtitle2;
        }
        vH3TextIcon2.text2.setVisibility(subtitle.isEmpty() ? 8 : 0);
        vH3TextIcon2.text2.setText(subtitle);
        if (imageInfo2.caption == null || imageInfo2.caption.length() <= 0) {
            vH3TextIcon2.text3.setVisibility(8);
        } else {
            vH3TextIcon2.text3.setText(imageInfo2.caption);
            vH3TextIcon2.text3.setVisibility(0);
        }
        vH3TextIcon2.icon.setImageBitmap(null);
        boolean isConnectionWifi = General.isConnectionWifi(this.activity);
        AppCompatActivity appCompatActivity = this.activity;
        AppearanceThumbnailsShow thumbnailsPref = PrefsUtility.appearance_thumbnails_show(appCompatActivity, PreferenceManager.getDefaultSharedPreferences(appCompatActivity));
        if (thumbnailsPref != AppearanceThumbnailsShow.ALWAYS && (thumbnailsPref != AppearanceThumbnailsShow.WIFIONLY || !isConnectionWifi)) {
            z = false;
        }
        if (!z) {
            String str = subtitle;
            imageInfo = imageInfo2;
        } else if (imageInfo2.urlBigSquare == null) {
            AppearanceThumbnailsShow appearanceThumbnailsShow = thumbnailsPref;
            String str2 = subtitle;
            imageInfo = imageInfo2;
        } else {
            vH3TextIcon2.text2.setVisibility(0);
            CacheManager instance = CacheManager.getInstance(this.activity);
            URI uriFromString = General.uriFromString(imageInfo2.urlBigSquare);
            RedditAccount anon = RedditAccountManager.getAnon();
            DownloadStrategyIfNotCached downloadStrategyIfNotCached = DownloadStrategyIfNotCached.INSTANCE;
            AppCompatActivity appCompatActivity2 = this.activity;
            AnonymousClass1 r24 = r0;
            CacheManager cacheManager = instance;
            DownloadStrategyIfNotCached downloadStrategyIfNotCached2 = downloadStrategyIfNotCached;
            AppearanceThumbnailsShow appearanceThumbnailsShow2 = thumbnailsPref;
            String str3 = subtitle;
            imageInfo = imageInfo2;
            final VH3TextIcon vH3TextIcon3 = vh;
            AnonymousClass1 r0 = new CacheRequest(this, uriFromString, anon, null, 100, position, downloadStrategyIfNotCached2, 200, 2, false, false, appCompatActivity2) {
                final /* synthetic */ AlbumAdapter this$0;

                {
                    this.this$0 = this$0;
                }

                /* access modifiers changed from: protected */
                public void onCallbackException(Throwable t) {
                    Log.e("AlbumAdapter", "Error in album thumbnail fetch callback", t);
                }

                /* access modifiers changed from: protected */
                public void onDownloadNecessary() {
                }

                /* access modifiers changed from: protected */
                public void onDownloadStarted() {
                }

                /* access modifiers changed from: protected */
                public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Failed to fetch thumbnail ");
                    sb.append(this.url.toString());
                    Log.e("AlbumAdapter", sb.toString());
                }

                /* access modifiers changed from: protected */
                public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
                }

                /* access modifiers changed from: protected */
                public void onSuccess(final ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            try {
                                if (vH3TextIcon3.bindingId == bindingId) {
                                    vH3TextIcon3.icon.setImageURI(cacheFile.getUri());
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
            };
            cacheManager.makeRequest(r24);
            vH3TextIcon = vh;
            final ImageInfo imageInfo3 = imageInfo;
            vH3TextIcon.itemView.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    LinkHandler.onLinkClicked(AlbumAdapter.this.activity, imageInfo3.urlOriginal, false, null, AlbumAdapter.this.albumInfo, vH3TextIcon.getAdapterPosition());
                }
            });
            vH3TextIcon.itemView.setOnLongClickListener(new OnLongClickListener() {
                public boolean onLongClick(View v) {
                    LinkHandler.onLinkLongClicked(AlbumAdapter.this.activity, imageInfo3.urlOriginal, false);
                    return true;
                }
            });
        }
        vH3TextIcon = vh;
        vH3TextIcon.icon.setVisibility(8);
        final ImageInfo imageInfo32 = imageInfo;
        vH3TextIcon.itemView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                LinkHandler.onLinkClicked(AlbumAdapter.this.activity, imageInfo32.urlOriginal, false, null, AlbumAdapter.this.albumInfo, vH3TextIcon.getAdapterPosition());
            }
        });
        vH3TextIcon.itemView.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                LinkHandler.onLinkLongClicked(AlbumAdapter.this.activity, imageInfo32.urlOriginal, false);
                return true;
            }
        });
    }

    public int getItemCount() {
        return this.albumInfo.images.size();
    }
}
