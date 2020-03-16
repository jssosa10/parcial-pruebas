package org.quantumbadger.redreader.reddit.things;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class RedditUser implements Parcelable {
    public static final Creator<RedditUser> CREATOR = new Creator<RedditUser>() {
        public RedditUser createFromParcel(Parcel in) {
            return new RedditUser(in);
        }

        public RedditUser[] newArray(int size) {
            return new RedditUser[size];
        }
    };
    public int comment_karma;
    public long created;
    public long created_utc;
    public Boolean has_mail;
    public Boolean has_mod_mail;
    public String id;
    public boolean is_friend;
    public boolean is_gold;
    public boolean is_mod;
    public int link_karma;
    public String modhash;
    public String name;
    public boolean over_18;

    public int describeContents() {
        return 0;
    }

    public RedditUser() {
    }

    private RedditUser(Parcel in) {
        this.comment_karma = in.readInt();
        this.link_karma = in.readInt();
        this.created = in.readLong();
        this.created_utc = in.readLong();
        boolean z = false;
        this.has_mail = Boolean.valueOf(in.readInt() == 1);
        int has_mail_in = in.readInt();
        if (has_mail_in == 0) {
            this.has_mail = null;
        } else {
            this.has_mail = Boolean.valueOf(has_mail_in == 1);
        }
        int has_mod_mail_in = in.readInt();
        if (has_mod_mail_in == 0) {
            this.has_mod_mail = null;
        } else {
            this.has_mod_mail = Boolean.valueOf(has_mod_mail_in == 1);
        }
        this.is_friend = in.readInt() == 1;
        this.is_gold = in.readInt() == 1;
        this.is_mod = in.readInt() == 1;
        if (in.readInt() == 1) {
            z = true;
        }
        this.over_18 = z;
        this.id = in.readString();
        this.modhash = in.readString();
        this.name = in.readString();
    }

    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(this.comment_karma);
        parcel.writeInt(this.link_karma);
        parcel.writeLong(this.created);
        parcel.writeLong(this.created_utc);
        Boolean bool = this.has_mail;
        int i = 1;
        if (bool == null) {
            parcel.writeInt(0);
        } else {
            parcel.writeInt(bool.booleanValue() ? 1 : -1);
        }
        Boolean bool2 = this.has_mod_mail;
        if (bool2 == null) {
            parcel.writeInt(0);
        } else {
            if (!bool2.booleanValue()) {
                i = -1;
            }
            parcel.writeInt(i);
        }
        parcel.writeInt(this.is_friend ? 1 : 0);
        parcel.writeInt(this.is_gold ? 1 : 0);
        parcel.writeInt(this.is_mod ? 1 : 0);
        parcel.writeInt(this.over_18 ? 1 : 0);
        parcel.writeString(this.id);
        parcel.writeString(this.modhash);
        parcel.writeString(this.name);
    }
}
