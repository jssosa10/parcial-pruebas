package org.quantumbadger.redreader.reddit.url;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import java.util.ArrayList;
import java.util.List;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.reddit.PostSort;

public class UserPostListingURL extends PostListingURL {
    public final String after;
    public final String before;
    public final Integer limit;
    public final PostSort order;
    public final Type type;
    public final String user;

    public enum Type {
        SAVED,
        HIDDEN,
        UPVOTED,
        DOWNVOTED,
        SUBMITTED
    }

    public static UserPostListingURL getSaved(String username) {
        UserPostListingURL userPostListingURL = new UserPostListingURL(Type.SAVED, username, null, null, null, null);
        return userPostListingURL;
    }

    public static UserPostListingURL getHidden(String username) {
        UserPostListingURL userPostListingURL = new UserPostListingURL(Type.HIDDEN, username, null, null, null, null);
        return userPostListingURL;
    }

    public static UserPostListingURL getLiked(String username) {
        UserPostListingURL userPostListingURL = new UserPostListingURL(Type.UPVOTED, username, null, null, null, null);
        return userPostListingURL;
    }

    public static UserPostListingURL getDisliked(String username) {
        UserPostListingURL userPostListingURL = new UserPostListingURL(Type.DOWNVOTED, username, null, null, null, null);
        return userPostListingURL;
    }

    public static UserPostListingURL getSubmitted(String username) {
        UserPostListingURL userPostListingURL = new UserPostListingURL(Type.SUBMITTED, username, null, null, null, null);
        return userPostListingURL;
    }

    UserPostListingURL(Type type2, String user2, PostSort order2, Integer limit2, String before2, String after2) {
        this.type = type2;
        this.user = user2;
        this.order = order2 == PostSort.RISING ? PostSort.NEW : order2;
        this.limit = limit2;
        this.before = before2;
        this.after = after2;
    }

    public UserPostListingURL after(String newAfter) {
        UserPostListingURL userPostListingURL = new UserPostListingURL(this.type, this.user, this.order, this.limit, this.before, newAfter);
        return userPostListingURL;
    }

    public UserPostListingURL limit(Integer newLimit) {
        UserPostListingURL userPostListingURL = new UserPostListingURL(this.type, this.user, this.order, newLimit, this.before, this.after);
        return userPostListingURL;
    }

    public UserPostListingURL sort(PostSort newOrder) {
        UserPostListingURL userPostListingURL = new UserPostListingURL(this.type, this.user, newOrder, this.limit, this.before, this.after);
        return userPostListingURL;
    }

    public PostSort getOrder() {
        return this.order;
    }

    public static UserPostListingURL parse(Uri uri) {
        PostSort order2;
        Integer limit2 = null;
        String before2 = null;
        String after2 = null;
        for (String parameterKey : General.getUriQueryParameterNames(uri)) {
            if (parameterKey.equalsIgnoreCase("after")) {
                after2 = uri.getQueryParameter(parameterKey);
            } else if (parameterKey.equalsIgnoreCase("before")) {
                before2 = uri.getQueryParameter(parameterKey);
            } else if (parameterKey.equalsIgnoreCase("limit")) {
                try {
                    limit2 = Integer.valueOf(Integer.parseInt(uri.getQueryParameter(parameterKey)));
                } catch (Throwable th) {
                }
            }
        }
        List<String> pathSegmentsList = uri.getPathSegments();
        ArrayList<String> pathSegmentsFiltered = new ArrayList<>(pathSegmentsList.size());
        for (String segment : pathSegmentsList) {
            while (true) {
                if (!General.asciiLowercase(segment).endsWith(".json") && !General.asciiLowercase(segment).endsWith(".xml")) {
                    break;
                }
                segment = segment.substring(0, segment.lastIndexOf(46));
            }
            if (segment.length() > 0) {
                pathSegmentsFiltered.add(segment);
            }
        }
        String[] pathSegments = (String[]) pathSegmentsFiltered.toArray(new String[pathSegmentsFiltered.size()]);
        if (pathSegments.length > 0) {
            order2 = PostSort.parse(uri.getQueryParameter("sort"), uri.getQueryParameter("t"));
        } else {
            order2 = null;
        }
        if (pathSegments.length < 3) {
            return null;
        }
        if (!pathSegments[0].equalsIgnoreCase("user") && !pathSegments[0].equalsIgnoreCase("u")) {
            return null;
        }
        try {
            UserPostListingURL userPostListingURL = new UserPostListingURL(Type.valueOf(General.asciiUppercase(pathSegments[2])), pathSegments[1], order2, limit2, before2, after2);
            return userPostListingURL;
        } catch (Throwable th2) {
            return null;
        }
    }

    public Uri generateJsonUri() {
        Builder builder = new Builder();
        builder.scheme(Reddit.getScheme()).authority(Reddit.getDomain());
        builder.appendEncodedPath("user");
        builder.appendPath(this.user);
        builder.appendEncodedPath(General.asciiLowercase(this.type.name()));
        PostSort postSort = this.order;
        if (postSort != null) {
            postSort.addToUserPostListingUri(builder);
        }
        String str = this.before;
        if (str != null) {
            builder.appendQueryParameter("before", str);
        }
        String str2 = this.after;
        if (str2 != null) {
            builder.appendQueryParameter("after", str2);
        }
        Integer num = this.limit;
        if (num != null) {
            builder.appendQueryParameter("limit", String.valueOf(num));
        }
        builder.appendEncodedPath(".json");
        return builder.build();
    }

    public int pathType() {
        return 1;
    }

    public String humanReadablePath() {
        String path = super.humanReadablePath();
        if (this.order == null || this.type != Type.SUBMITTED) {
            return path;
        }
        switch (this.order) {
            case TOP_HOUR:
            case TOP_DAY:
            case TOP_WEEK:
            case TOP_MONTH:
            case TOP_YEAR:
            case TOP_ALL:
                StringBuilder sb = new StringBuilder();
                sb.append(path);
                sb.append("?t=");
                sb.append(General.asciiLowercase(this.order.name().split("_")[1]));
                return sb.toString();
            default:
                StringBuilder sb2 = new StringBuilder();
                sb2.append(path);
                sb2.append("?sort=");
                sb2.append(General.asciiLowercase(this.order.name()));
                return sb2.toString();
        }
    }

    public String humanReadableName(Context context, boolean shorter) {
        String name;
        switch (this.type) {
            case SAVED:
                name = context.getString(R.string.mainmenu_saved);
                break;
            case HIDDEN:
                name = context.getString(R.string.mainmenu_hidden);
                break;
            case UPVOTED:
                name = context.getString(R.string.mainmenu_upvoted);
                break;
            case DOWNVOTED:
                name = context.getString(R.string.mainmenu_downvoted);
                break;
            case SUBMITTED:
                name = context.getString(R.string.mainmenu_submitted);
                break;
            default:
                return super.humanReadableName(context, shorter);
        }
        if (shorter) {
            return name;
        }
        return String.format("%s (%s)", new Object[]{name, this.user});
    }
}
