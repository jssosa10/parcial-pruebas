package org.quantumbadger.redreader.reddit.things;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.annotation.Nullable;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedObject;

public final class RedditPost implements Parcelable, RedditThingWithIdAndType {
    public static final Creator<RedditPost> CREATOR = new Creator<RedditPost>() {
        public RedditPost createFromParcel(Parcel in) {
            return new RedditPost(in);
        }

        public RedditPost[] newArray(int size) {
            return new RedditPost[size];
        }
    };
    public boolean archived;
    public String author;
    public String author_flair_text;
    public boolean clicked;
    public long created;
    public long created_utc;
    public String domain;
    public int downs;
    public Object edited;
    public int gilded;
    public boolean hidden;
    public String id;
    public boolean is_self;
    public Boolean likes;
    public String link_flair_text;
    public JsonBufferedObject media;
    public String name;
    public int num_comments;
    public boolean over_18;
    public String permalink;
    @Nullable
    public String rr_internal_dash_url;
    public boolean saved;
    public int score;
    public String selftext;
    public Boolean spoiler;
    public boolean stickied;
    public String subreddit;
    public String subreddit_id;
    public String thumbnail;
    public String title;
    public int ups;
    public String url;

    public RedditPost() {
    }

    @Nullable
    public String getDashUrl() {
        String str = this.rr_internal_dash_url;
        if (str != null) {
            return str;
        }
        JsonBufferedObject jsonBufferedObject = this.media;
        if (jsonBufferedObject != null) {
            try {
                this.rr_internal_dash_url = jsonBufferedObject.getObject("reddit_video").getString("fallback_url");
            } catch (Exception e) {
                this.rr_internal_dash_url = null;
            }
        }
        return this.rr_internal_dash_url;
    }

    public String getUrl() {
        if (getDashUrl() != null) {
            return this.rr_internal_dash_url;
        }
        return this.url;
    }

    private RedditPost(Parcel in) {
        this.id = in.readString();
        this.name = in.readString();
        this.title = in.readString();
        this.url = in.readString();
        this.author = in.readString();
        this.domain = in.readString();
        this.subreddit = in.readString();
        this.subreddit_id = in.readString();
        this.num_comments = in.readInt();
        this.score = in.readInt();
        this.ups = in.readInt();
        this.downs = in.readInt();
        this.gilded = in.readInt();
        this.archived = in.readInt() == 1;
        this.over_18 = in.readInt() == 1;
        this.hidden = in.readInt() == 1;
        this.saved = in.readInt() == 1;
        this.is_self = in.readInt() == 1;
        this.clicked = in.readInt() == 1;
        this.stickied = in.readInt() == 1;
        long in_edited = in.readLong();
        if (in_edited == -1) {
            this.edited = Boolean.valueOf(false);
        } else {
            this.edited = Long.valueOf(in_edited);
        }
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
        this.created = in.readLong();
        this.created_utc = in.readLong();
        this.selftext = in.readString();
        this.permalink = in.readString();
        this.link_flair_text = in.readString();
        this.author_flair_text = in.readString();
        this.thumbnail = in.readString();
        switch (in.readInt()) {
            case -1:
                this.spoiler = Boolean.valueOf(false);
                break;
            case 0:
                this.spoiler = null;
                break;
            case 1:
                this.spoiler = Boolean.valueOf(true);
                break;
        }
        this.rr_internal_dash_url = in.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(this.id);
        parcel.writeString(this.name);
        parcel.writeString(this.title);
        parcel.writeString(this.url);
        parcel.writeString(this.author);
        parcel.writeString(this.domain);
        parcel.writeString(this.subreddit);
        parcel.writeString(this.subreddit_id);
        parcel.writeInt(this.num_comments);
        parcel.writeInt(this.score);
        parcel.writeInt(this.ups);
        parcel.writeInt(this.downs);
        parcel.writeInt(this.gilded);
        parcel.writeInt(this.archived ? 1 : 0);
        parcel.writeInt(this.over_18 ? 1 : 0);
        parcel.writeInt(this.hidden ? 1 : 0);
        parcel.writeInt(this.saved ? 1 : 0);
        parcel.writeInt(this.is_self ? 1 : 0);
        parcel.writeInt(this.clicked ? 1 : 0);
        parcel.writeInt(this.stickied ? 1 : 0);
        Object obj = this.edited;
        if (obj instanceof Long) {
            parcel.writeLong(((Long) obj).longValue());
        } else {
            parcel.writeLong(-1);
        }
        Boolean bool = this.likes;
        int i = 1;
        if (bool == null) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(bool.booleanValue() ? 1 : -1);
        }
        parcel.writeLong(this.created);
        parcel.writeLong(this.created_utc);
        parcel.writeString(this.selftext);
        parcel.writeString(this.permalink);
        parcel.writeString(this.link_flair_text);
        parcel.writeString(this.author_flair_text);
        parcel.writeString(this.thumbnail);
        Boolean bool2 = this.spoiler;
        if (bool2 == null) {
            parcel.writeInt(0);
        } else {
            if (!bool2.booleanValue()) {
                i = -1;
            }
            parcel.writeInt(i);
        }
        getDashUrl();
        parcel.writeString(this.rr_internal_dash_url);
    }

    public String getIdAlone() {
        return this.id;
    }

    public String getIdAndType() {
        return this.name;
    }
}
