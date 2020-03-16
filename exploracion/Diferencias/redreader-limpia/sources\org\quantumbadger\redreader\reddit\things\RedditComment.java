package org.quantumbadger.redreader.reddit.things;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.annotation.Nullable;
import java.util.HashSet;
import org.apache.commons.lang3.StringEscapeUtils;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.jsonwrap.JsonValue;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL;

public final class RedditComment implements Parcelable, RedditThingWithIdAndType {
    public static final Creator<RedditComment> CREATOR = new Creator<RedditComment>() {
        public RedditComment createFromParcel(Parcel in) {
            return new RedditComment(in);
        }

        public RedditComment[] newArray(int size) {
            return new RedditComment[size];
        }
    };
    public Boolean archived;
    public String author;
    public String author_flair_text;
    public String body;
    public String body_html;
    public String context;
    public long created;
    public long created_utc;
    public String distinguished;
    public int downs;
    public Object edited;
    public int gilded;
    public String id;
    public Boolean likes;
    public String link_id;
    public String name;
    public String parent_id;
    public JsonValue replies;
    public Boolean saved;
    public Boolean score_hidden;
    public String subreddit;
    public String subreddit_id;
    public int ups;

    public RedditComment() {
    }

    private RedditComment(Parcel in) {
        this.body = in.readString();
        this.body_html = in.readString();
        this.author = in.readString();
        this.subreddit = in.readString();
        this.author_flair_text = in.readString();
        boolean z = false;
        this.archived = Boolean.valueOf(in.readInt() == 1);
        switch (in.readInt()) {
            case -1:
                this.likes = Boolean.valueOf(false);
                break;
            case 0:
                this.likes = null;
                break;
            case 1:
                this.likes = Boolean.valueOf(true);
                break;
        }
        this.replies = null;
        this.id = in.readString();
        this.subreddit_id = in.readString();
        this.link_id = in.readString();
        this.parent_id = in.readString();
        this.name = in.readString();
        this.context = in.readString();
        this.ups = in.readInt();
        this.downs = in.readInt();
        long in_edited = in.readLong();
        if (in_edited == -1) {
            this.edited = Boolean.valueOf(false);
        } else {
            this.edited = Long.valueOf(in_edited);
        }
        this.created = in.readLong();
        this.created_utc = in.readLong();
        if (in.readInt() != 0) {
            z = true;
        }
        this.saved = Boolean.valueOf(z);
        this.gilded = in.readInt();
        this.distinguished = in.readString();
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.body);
        parcel.writeString(this.body_html);
        parcel.writeString(this.author);
        parcel.writeString(this.subreddit);
        parcel.writeString(this.author_flair_text);
        parcel.writeInt(this.archived.booleanValue() ? 1 : 0);
        Boolean bool = this.likes;
        if (bool == null) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(bool.booleanValue() ? 1 : -1);
        }
        parcel.writeString(this.id);
        parcel.writeString(this.subreddit_id);
        parcel.writeString(this.link_id);
        parcel.writeString(this.parent_id);
        parcel.writeString(this.name);
        parcel.writeString(this.context);
        parcel.writeInt(this.ups);
        parcel.writeInt(this.downs);
        Object obj = this.edited;
        if (obj instanceof Long) {
            parcel.writeLong(((Long) obj).longValue());
        } else {
            parcel.writeLong(-1);
        }
        parcel.writeLong(this.created);
        parcel.writeLong(this.created_utc);
        parcel.writeInt(this.saved.booleanValue() ? 1 : 0);
        parcel.writeInt(this.gilded);
        parcel.writeString(this.distinguished);
    }

    public String getIdAlone() {
        return this.id;
    }

    public String getIdAndType() {
        return this.name;
    }

    public boolean isArchived() {
        return Boolean.TRUE.equals(this.archived);
    }

    @Nullable
    public PostCommentListingURL getContextUrl() {
        if (this.context != null) {
            String rawContextUrl = this.context;
            if (rawContextUrl.startsWith("r/")) {
                StringBuilder sb = new StringBuilder();
                sb.append("/");
                sb.append(rawContextUrl);
                rawContextUrl = sb.toString();
            }
            if (rawContextUrl.startsWith("/")) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("https://reddit.com");
                sb2.append(rawContextUrl);
                rawContextUrl = sb2.toString();
            }
            return PostCommentListingURL.parse(Uri.parse(rawContextUrl));
        }
        PostCommentListingURL postCommentListingURL = new PostCommentListingURL(null, this.link_id, getIdAlone(), Integer.valueOf(3), null, null);
        return postCommentListingURL;
    }

    public int describeContents() {
        return 0;
    }

    public HashSet<String> computeAllLinks() {
        return LinkHandler.computeAllLinks(StringEscapeUtils.unescapeHtml4(this.body_html));
    }
}
