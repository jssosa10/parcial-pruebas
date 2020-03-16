package org.quantumbadger.redreader.reddit.url;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.reddit.PostSort;
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;

public class MultiredditPostListURL extends PostListingURL {
    @Nullable
    public final String after;
    @Nullable
    public final String before;
    @Nullable
    public final Integer limit;
    @NonNull
    public final String name;
    @Nullable
    public final PostSort order;
    @Nullable
    public final String username;

    public static RedditURL getMultireddit(@NonNull String name2) {
        Builder builder = new Builder();
        builder.scheme(Reddit.getScheme()).authority(Reddit.getDomain());
        builder.encodedPath("/me/m/");
        builder.appendPath(name2);
        return RedditURLParser.parse(builder.build());
    }

    public static RedditURL getMultireddit(@NonNull String username2, @NonNull String name2) {
        Builder builder = new Builder();
        builder.scheme(Reddit.getScheme()).authority(Reddit.getDomain());
        builder.encodedPath("/user/");
        builder.appendPath(username2);
        builder.appendPath("/m/");
        builder.appendPath(name2);
        return RedditURLParser.parse(builder.build());
    }

    private MultiredditPostListURL(@Nullable String username2, @NonNull String name2, @Nullable PostSort order2, @Nullable Integer limit2, @Nullable String before2, @Nullable String after2) {
        this.username = username2;
        this.name = name2;
        this.order = order2;
        this.limit = limit2;
        this.before = before2;
        this.after = after2;
    }

    public MultiredditPostListURL after(String newAfter) {
        MultiredditPostListURL multiredditPostListURL = new MultiredditPostListURL(this.username, this.name, this.order, this.limit, this.before, newAfter);
        return multiredditPostListURL;
    }

    public MultiredditPostListURL limit(Integer newLimit) {
        MultiredditPostListURL multiredditPostListURL = new MultiredditPostListURL(this.username, this.name, this.order, newLimit, this.before, this.after);
        return multiredditPostListURL;
    }

    public MultiredditPostListURL sort(PostSort newOrder) {
        MultiredditPostListURL multiredditPostListURL = new MultiredditPostListURL(this.username, this.name, newOrder, this.limit, this.before, this.after);
        return multiredditPostListURL;
    }

    public PostSort getOrder() {
        return this.order;
    }

    public Uri generateJsonUri() {
        Builder builder = new Builder();
        builder.scheme(Reddit.getScheme()).authority(Reddit.getDomain());
        if (this.username != null) {
            builder.encodedPath("/user/");
            builder.appendPath(this.username);
        } else {
            builder.encodedPath("/me/");
        }
        builder.appendPath("m");
        builder.appendPath(this.name);
        PostSort postSort = this.order;
        if (postSort != null) {
            postSort.addToSubredditListingUri(builder);
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
        return 8;
    }

    public static MultiredditPostListURL parse(Uri uri) {
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
            order2 = PostSort.parse(pathSegments[pathSegments.length - 1], uri.getQueryParameter("t"));
        } else {
            order2 = null;
        }
        if (pathSegments.length < 3) {
            return null;
        }
        if (pathSegments[0].equalsIgnoreCase("me")) {
            if (!pathSegments[1].equalsIgnoreCase("m")) {
                return null;
            }
            MultiredditPostListURL multiredditPostListURL = new MultiredditPostListURL(null, pathSegments[2], order2, limit2, before2, after2);
            return multiredditPostListURL;
        } else if (!pathSegments[0].equalsIgnoreCase("user") || !pathSegments[2].equalsIgnoreCase("m") || pathSegments.length < 4) {
            return null;
        } else {
            MultiredditPostListURL multiredditPostListURL2 = new MultiredditPostListURL(pathSegments[1], pathSegments[3], order2, limit2, before2, after2);
            return multiredditPostListURL2;
        }
    }

    public String humanReadablePath() {
        String path = super.humanReadablePath();
        if (this.order == null) {
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
                return path;
        }
    }

    public String humanReadableName(Context context, boolean shorter) {
        if (this.username == null) {
            return this.name;
        }
        return String.format(Locale.US, "%s (%s)", new Object[]{this.name, this.username});
    }
}
