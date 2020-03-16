package org.quantumbadger.redreader.reddit.things;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.UnexpectedInternalStateException;
import org.quantumbadger.redreader.io.WritableObject;
import org.quantumbadger.redreader.io.WritableObject.CreationData;
import org.quantumbadger.redreader.io.WritableObject.WritableField;
import org.quantumbadger.redreader.io.WritableObject.WritableObjectTimestamp;
import org.quantumbadger.redreader.io.WritableObject.WritableObjectVersion;

public class RedditSubreddit implements Parcelable, Comparable<RedditSubreddit>, WritableObject<String> {
    public static final Creator<RedditSubreddit> CREATOR = new Creator<RedditSubreddit>() {
        public RedditSubreddit createFromParcel(Parcel in) {
            return new RedditSubreddit(in);
        }

        public RedditSubreddit[] newArray(int size) {
            return new RedditSubreddit[size];
        }
    };
    @WritableObjectVersion
    public static int DB_VERSION = 1;
    private static final Pattern NAME_PATTERN = Pattern.compile("(/)?(r/)?([\\w\\+\\-\\.:]+)/?");
    private static final Pattern USER_PATTERN = Pattern.compile("(/)?(u/|user/)([\\w\\+\\-\\.:]+)/?");
    @WritableField
    public Integer accounts_active;
    @WritableField
    public long created;
    @WritableField
    public long created_utc;
    @WritableField
    public String description;
    @WritableField
    public String description_html;
    @WritableField
    public String display_name;
    @WritableObjectTimestamp
    public long downloadTime;
    @WritableField
    public String header_img;
    @WritableField
    public String header_title;
    @WritableField
    public String id;
    @WritableField
    public String name;
    @WritableField
    public boolean over18;
    @WritableField
    public String public_description;
    @WritableField
    public Integer subscribers;
    @WritableField
    public String title;
    @WritableField
    public String url;

    public static final class InvalidSubredditNameException extends Exception {
        public InvalidSubredditNameException(String subredditName) {
            Locale locale = Locale.US;
            String str = "Invalid subreddit name '%s'.";
            Object[] objArr = new Object[1];
            objArr[0] = subredditName == null ? "NULL" : subredditName;
            super(String.format(locale, str, objArr));
        }
    }

    public String getKey() {
        try {
            return getCanonicalName();
        } catch (InvalidSubredditNameException e) {
            throw new UnexpectedInternalStateException(String.format(Locale.US, "Cannot save subreddit '%s'", new Object[]{this.url}));
        }
    }

    public long getTimestamp() {
        return this.downloadTime;
    }

    public RedditSubreddit(CreationData creationData) {
        this();
        this.downloadTime = creationData.timestamp;
    }

    public static String stripRPrefix(String name2) throws InvalidSubredditNameException {
        Matcher matcher = NAME_PATTERN.matcher(name2);
        if (matcher.matches()) {
            return matcher.group(3);
        }
        throw new InvalidSubredditNameException(name2);
    }

    public static String stripUserPrefix(String name2) {
        Matcher matcher = USER_PATTERN.matcher(name2);
        if (matcher.matches()) {
            return matcher.group(3);
        }
        return null;
    }

    public static String getCanonicalName(String name2) throws InvalidSubredditNameException {
        String userSr = stripUserPrefix(name2);
        if (userSr != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("/user/");
            sb.append(General.asciiLowercase(userSr));
            return sb.toString();
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("/r/");
        sb2.append(General.asciiLowercase(stripRPrefix(name2)));
        return sb2.toString();
    }

    public String getCanonicalName() throws InvalidSubredditNameException {
        return getCanonicalName(this.url);
    }

    public static String getDisplayNameFromCanonicalName(String canonicalName) {
        if (canonicalName.startsWith("/user/")) {
            return canonicalName;
        }
        return canonicalName.substring(3);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.header_img);
        out.writeString(this.header_title);
        out.writeString(this.description);
        out.writeString(this.description_html);
        out.writeString(this.public_description);
        out.writeString(this.id);
        out.writeString(this.name);
        out.writeString(this.title);
        out.writeString(this.display_name);
        out.writeString(this.url);
        out.writeLong(this.created);
        out.writeLong(this.created_utc);
        Integer num = this.accounts_active;
        int i = -1;
        out.writeInt(num == null ? -1 : num.intValue());
        Integer num2 = this.subscribers;
        if (num2 != null) {
            i = num2.intValue();
        }
        out.writeInt(i);
        out.writeInt(this.over18 ? 1 : 0);
    }

    public RedditSubreddit() {
    }

    public RedditSubreddit(String url2, String title2, boolean isSortable) {
        this.url = url2;
        this.title = title2;
    }

    public RedditSubreddit(Parcel parcel) {
        this.header_img = parcel.readString();
        this.header_title = parcel.readString();
        this.description = parcel.readString();
        this.description_html = parcel.readString();
        this.public_description = parcel.readString();
        this.id = parcel.readString();
        this.name = parcel.readString();
        this.title = parcel.readString();
        this.display_name = parcel.readString();
        this.url = parcel.readString();
        this.created = parcel.readLong();
        this.created_utc = parcel.readLong();
        this.accounts_active = Integer.valueOf(parcel.readInt());
        this.subscribers = Integer.valueOf(parcel.readInt());
        if (this.accounts_active.intValue() < 0) {
            this.accounts_active = null;
        }
        if (this.subscribers.intValue() < 0) {
            this.subscribers = null;
        }
        boolean z = true;
        if (parcel.readInt() != 1) {
            z = false;
        }
        this.over18 = z;
    }

    public int compareTo(RedditSubreddit another) {
        return General.asciiLowercase(this.display_name).compareTo(General.asciiLowercase(another.display_name));
    }

    public String getSidebarHtml(boolean nightMode) {
        String unescaped = StringEscapeUtils.unescapeHtml4(this.description_html);
        StringBuilder result = new StringBuilder(unescaped.length() + 512);
        result.append("<html>");
        result.append("<head>");
        result.append("<meta name=\"viewport\" content=\"width=device-width, user-scalable=yes\">");
        if (nightMode) {
            result.append("<style>");
            result.append("body {color: white; background-color: black;}");
            result.append("a {color: #3399FF; background-color: 000033;}");
            result.append("</style>");
        }
        result.append("</head>");
        result.append("<body>");
        result.append(unescaped);
        result.append("</body>");
        result.append("</html>");
        return result.toString();
    }
}
