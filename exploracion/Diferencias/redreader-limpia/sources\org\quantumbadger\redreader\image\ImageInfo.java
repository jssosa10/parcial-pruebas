package org.quantumbadger.redreader.image;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.util.MimeTypes;
import java.io.IOException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.quantumbadger.redreader.common.ParcelHelper;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedObject;

public class ImageInfo implements Parcelable {
    public static final Creator<ImageInfo> CREATOR = new Creator<ImageInfo>() {
        public ImageInfo createFromParcel(Parcel in) {
            return new ImageInfo(in);
        }

        public ImageInfo[] newArray(int size) {
            return new ImageInfo[size];
        }
    };
    public final String caption;
    @NonNull
    public final HasAudio hasAudio;
    public final Long height;
    public final Boolean isAnimated;
    public final MediaType mediaType;
    public final Long size;
    public final String title;
    public final String type;
    @Nullable
    public final String urlAudioStream;
    public final String urlBigSquare;
    public final String urlOriginal;
    public final Long width;

    public enum HasAudio {
        HAS_AUDIO,
        MAYBE_AUDIO,
        NO_AUDIO;

        @NonNull
        public static HasAudio fromBoolean(@Nullable Boolean value) {
            if (value == null) {
                return MAYBE_AUDIO;
            }
            if (value.booleanValue()) {
                return HAS_AUDIO;
            }
            return NO_AUDIO;
        }
    }

    public enum MediaType {
        IMAGE,
        VIDEO,
        GIF
    }

    public ImageInfo(String urlOriginal2, MediaType mediaType2, @NonNull HasAudio hasAudio2) {
        this(urlOriginal2, null, mediaType2, hasAudio2);
    }

    public ImageInfo(String urlOriginal2, @Nullable String urlAudioStream2, MediaType mediaType2, @NonNull HasAudio hasAudio2) {
        this.urlOriginal = urlOriginal2;
        this.urlAudioStream = urlAudioStream2;
        this.urlBigSquare = null;
        this.title = null;
        this.caption = null;
        this.type = null;
        this.isAnimated = null;
        this.width = null;
        this.height = null;
        this.size = null;
        this.mediaType = mediaType2;
        this.hasAudio = hasAudio2;
    }

    private ImageInfo(Parcel in) {
        this.urlOriginal = ParcelHelper.readNullableString(in);
        this.urlBigSquare = ParcelHelper.readNullableString(in);
        this.urlAudioStream = ParcelHelper.readNullableString(in);
        this.title = ParcelHelper.readNullableString(in);
        this.caption = ParcelHelper.readNullableString(in);
        this.type = ParcelHelper.readNullableString(in);
        this.isAnimated = ParcelHelper.readNullableBoolean(in);
        this.width = ParcelHelper.readNullableLong(in);
        this.height = ParcelHelper.readNullableLong(in);
        this.size = ParcelHelper.readNullableLong(in);
        this.mediaType = ParcelHelper.readNullableImageInfoMediaType(in);
        this.hasAudio = ParcelHelper.readImageInfoHasAudio(in);
    }

    public ImageInfo(String urlOriginal2, String urlBigSquare2, String title2, String caption2, String type2, Boolean isAnimated2, Long width2, Long height2, Long size2, MediaType mediaType2, @NonNull HasAudio hasAudio2) {
        this.urlOriginal = urlOriginal2;
        this.urlBigSquare = urlBigSquare2;
        this.urlAudioStream = null;
        this.title = title2;
        this.caption = caption2;
        this.type = type2;
        this.isAnimated = isAnimated2;
        this.width = width2;
        this.height = height2;
        this.size = size2;
        this.mediaType = mediaType2;
        this.hasAudio = hasAudio2;
    }

    public static ImageInfo parseGfycat(JsonBufferedObject object) throws IOException, InterruptedException {
        JsonBufferedObject jsonBufferedObject = object;
        Long width2 = jsonBufferedObject.getLong("width");
        Long height2 = jsonBufferedObject.getLong("height");
        String urlOriginal2 = jsonBufferedObject.getString("mp4Url");
        Long size2 = jsonBufferedObject.getLong("mp4Size");
        String title2 = jsonBufferedObject.getString("title");
        String str = urlOriginal2;
        String str2 = title2;
        Long l = width2;
        Long l2 = height2;
        Long l3 = size2;
        ImageInfo imageInfo = new ImageInfo(str, null, str2, null, MimeTypes.VIDEO_MP4, Boolean.valueOf(true), l, l2, l3, MediaType.VIDEO, HasAudio.fromBoolean(jsonBufferedObject.getBoolean("hasAudio")));
        return imageInfo;
    }

    public static ImageInfo parseStreamable(JsonBufferedObject object) throws IOException, InterruptedException {
        String[] preferredTypes;
        String urlOriginal2;
        JsonBufferedObject files = object.getObject("files");
        String selectedType = null;
        JsonBufferedObject fileObj = null;
        for (String type2 : new String[]{"mp4", "webm", "mp4-high", "webm-high", "mp4-mobile", "webm-mobile"}) {
            fileObj = files.getObject(type2);
            selectedType = type2;
            if (fileObj != null) {
                break;
            }
        }
        if (fileObj != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("video/");
            sb.append(selectedType.split("\\-")[0]);
            String mimeType = sb.toString();
            Long width2 = fileObj.getLong("width");
            Long height2 = fileObj.getLong("height");
            String urlOriginal3 = fileObj.getString("url");
            if (urlOriginal3.startsWith("//")) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("https:");
                sb2.append(urlOriginal3);
                urlOriginal2 = sb2.toString();
            } else {
                urlOriginal2 = urlOriginal3;
            }
            ImageInfo imageInfo = new ImageInfo(urlOriginal2, null, null, null, mimeType, Boolean.valueOf(true), width2, height2, null, MediaType.VIDEO, HasAudio.MAYBE_AUDIO);
            return imageInfo;
        }
        throw new IOException("No suitable Streamable files found");
    }

    public static ImageInfo parseImgur(JsonBufferedObject object) throws IOException, InterruptedException {
        Long size2;
        Long height2;
        Long width2;
        JsonBufferedObject jsonBufferedObject = object;
        JsonBufferedObject image = jsonBufferedObject.getObject("image");
        JsonBufferedObject links = jsonBufferedObject.getObject("links");
        String urlOriginal2 = null;
        String urlBigSquare2 = null;
        String title2 = null;
        String caption2 = null;
        String type2 = null;
        boolean isAnimated2 = false;
        if (image != null) {
            title2 = image.getString("title");
            caption2 = image.getString("caption");
            type2 = image.getString("type");
            isAnimated2 = "true".equals(image.getString("animated"));
            width2 = image.getLong("width");
            height2 = image.getLong("height");
            size2 = image.getLong("size");
        } else {
            width2 = null;
            height2 = null;
            size2 = null;
        }
        if (links != null) {
            urlOriginal2 = links.getString("original");
            if (urlOriginal2 != null && isAnimated2) {
                urlOriginal2 = urlOriginal2.replace(".gif", ".mp4");
            }
            urlBigSquare2 = links.getString("big_square");
        }
        if (title2 != null) {
            title2 = StringEscapeUtils.unescapeHtml4(title2);
        }
        if (caption2 != null) {
            caption2 = StringEscapeUtils.unescapeHtml4(caption2);
        }
        ImageInfo imageInfo = new ImageInfo(urlOriginal2, urlBigSquare2, title2, caption2, type2, Boolean.valueOf(isAnimated2), width2, height2, size2, isAnimated2 ? MediaType.VIDEO : MediaType.IMAGE, isAnimated2 ? HasAudio.MAYBE_AUDIO : HasAudio.NO_AUDIO);
        return imageInfo;
    }

    public static ImageInfo parseImgurV3(JsonBufferedObject object) throws IOException, InterruptedException {
        Boolean hasSound;
        Long size2;
        Long height2;
        boolean mp4;
        Long width2;
        String type2;
        boolean isAnimated2;
        JsonBufferedObject jsonBufferedObject = object;
        String id = null;
        String urlOriginal2 = null;
        String thumbnailUrl = null;
        String title2 = null;
        String caption2 = null;
        if (jsonBufferedObject != null) {
            id = jsonBufferedObject.getString(TtmlNode.ATTR_ID);
            title2 = jsonBufferedObject.getString("title");
            caption2 = jsonBufferedObject.getString("description");
            String type3 = jsonBufferedObject.getString("type");
            boolean isAnimated3 = jsonBufferedObject.getBoolean("animated").booleanValue();
            Long width3 = jsonBufferedObject.getLong("width");
            Long height3 = jsonBufferedObject.getLong("height");
            Long size3 = jsonBufferedObject.getLong("size");
            if (jsonBufferedObject.getString("mp4") != null) {
                urlOriginal2 = jsonBufferedObject.getString("mp4");
                type2 = type3;
                isAnimated2 = isAnimated3;
                width2 = width3;
                height2 = height3;
                size2 = jsonBufferedObject.getLong("mp4_size");
                mp4 = true;
                hasSound = jsonBufferedObject.getBoolean("has_sound");
            } else {
                urlOriginal2 = jsonBufferedObject.getString("link");
                type2 = type3;
                isAnimated2 = isAnimated3;
                width2 = width3;
                height2 = height3;
                size2 = size3;
                mp4 = false;
                hasSound = Boolean.valueOf(false);
            }
        } else {
            type2 = null;
            isAnimated2 = false;
            width2 = null;
            height2 = null;
            size2 = null;
            mp4 = false;
            hasSound = null;
        }
        if (title2 != null) {
            title2 = StringEscapeUtils.unescapeHtml4(title2);
        }
        if (caption2 != null) {
            caption2 = StringEscapeUtils.unescapeHtml4(caption2);
        }
        if (id != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("https://i.imgur.com/");
            sb.append(id);
            sb.append("b.jpg");
            thumbnailUrl = sb.toString();
        }
        ImageInfo imageInfo = new ImageInfo(urlOriginal2, thumbnailUrl, title2, caption2, type2, Boolean.valueOf(isAnimated2), width2, height2, size2, mp4 ? MediaType.VIDEO : MediaType.IMAGE, HasAudio.fromBoolean(hasSound));
        return imageInfo;
    }

    public static ImageInfo parseDeviantArt(JsonBufferedObject object) throws IOException, InterruptedException {
        JsonBufferedObject jsonBufferedObject = object;
        String urlOriginal2 = null;
        String thumbnailUrl = null;
        String title2 = null;
        String tags = null;
        String type2 = null;
        Long width2 = null;
        Long height2 = null;
        Long size2 = Long.valueOf(0);
        if (jsonBufferedObject != null) {
            urlOriginal2 = jsonBufferedObject.getString("url");
            thumbnailUrl = jsonBufferedObject.getString("thumbnail_url");
            title2 = jsonBufferedObject.getString("title");
            tags = jsonBufferedObject.getString("tags");
            type2 = jsonBufferedObject.getString("imagetype");
            width2 = jsonBufferedObject.getLong("width");
            height2 = jsonBufferedObject.getLong("height");
        }
        if (title2 != null) {
            title2 = StringEscapeUtils.unescapeHtml4(title2);
        }
        if (tags != null) {
            tags = StringEscapeUtils.unescapeHtml4(tags);
        }
        ImageInfo imageInfo = new ImageInfo(urlOriginal2, thumbnailUrl, title2, tags, type2, Boolean.valueOf(false), width2, height2, size2, MediaType.IMAGE, HasAudio.NO_AUDIO);
        return imageInfo;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        ParcelHelper.writeNullableString(parcel, this.urlOriginal);
        ParcelHelper.writeNullableString(parcel, this.urlBigSquare);
        ParcelHelper.writeNullableString(parcel, this.urlAudioStream);
        ParcelHelper.writeNullableString(parcel, this.title);
        ParcelHelper.writeNullableString(parcel, this.caption);
        ParcelHelper.writeNullableString(parcel, this.type);
        ParcelHelper.writeNullableBoolean(parcel, this.isAnimated);
        ParcelHelper.writeNullableLong(parcel, this.width);
        ParcelHelper.writeNullableLong(parcel, this.height);
        ParcelHelper.writeNullableLong(parcel, this.size);
        ParcelHelper.writeNullableEnum(parcel, this.mediaType);
        ParcelHelper.writeNonNullEnum(parcel, this.hasAudio);
    }
}
