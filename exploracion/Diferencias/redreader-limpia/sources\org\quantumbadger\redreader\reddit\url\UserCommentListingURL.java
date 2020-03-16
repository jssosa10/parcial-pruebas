package org.quantumbadger.redreader.reddit.url;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;

public class UserCommentListingURL extends CommentListingURL {
    public final String after;
    public final Integer limit;
    public final Sort order;
    public final String user;

    public enum Sort {
        NEW,
        HOT,
        CONTROVERSIAL,
        TOP,
        TOP_HOUR,
        TOP_DAY,
        TOP_WEEK,
        TOP_MONTH,
        TOP_YEAR,
        TOP_ALL;

        @Nullable
        public static Sort parse(@Nullable String sort, @Nullable String t) {
            if (sort == null) {
                return null;
            }
            String sort2 = General.asciiLowercase(sort);
            String t2 = t != null ? General.asciiLowercase(t) : null;
            if (sort2.equals("hot")) {
                return HOT;
            }
            if (sort2.equals("new")) {
                return NEW;
            }
            if (sort2.equals("controversial")) {
                return CONTROVERSIAL;
            }
            if (!sort2.equals("top")) {
                return null;
            }
            if (t2 == null) {
                return TOP_ALL;
            }
            if (t2.equals("all")) {
                return TOP_ALL;
            }
            if (t2.equals("hour")) {
                return TOP_HOUR;
            }
            if (t2.equals("day")) {
                return TOP_DAY;
            }
            if (t2.equals("week")) {
                return TOP_WEEK;
            }
            if (t2.equals("month")) {
                return TOP_MONTH;
            }
            if (t2.equals("year")) {
                return TOP_YEAR;
            }
            return TOP_ALL;
        }

        public void addToUserCommentListingUri(@NonNull Builder builder) {
            switch (this) {
                case HOT:
                case NEW:
                case CONTROVERSIAL:
                    builder.appendQueryParameter("sort", General.asciiLowercase(name()));
                    return;
                case TOP_HOUR:
                case TOP_DAY:
                case TOP_WEEK:
                case TOP_MONTH:
                case TOP_YEAR:
                case TOP_ALL:
                    String[] parts = name().split("_");
                    builder.appendQueryParameter("sort", General.asciiLowercase(parts[0]));
                    builder.appendQueryParameter("t", General.asciiLowercase(parts[1]));
                    return;
                default:
                    return;
            }
        }
    }

    UserCommentListingURL(String user2, Sort order2, Integer limit2, String after2) {
        this.user = user2;
        this.order = order2;
        this.limit = limit2;
        this.after = after2;
    }

    public UserCommentListingURL after(String newAfter) {
        return new UserCommentListingURL(this.user, this.order, this.limit, newAfter);
    }

    public UserCommentListingURL limit(Integer newLimit) {
        return new UserCommentListingURL(this.user, this.order, newLimit, this.after);
    }

    public UserCommentListingURL order(Sort newOrder) {
        return new UserCommentListingURL(this.user, newOrder, this.limit, this.after);
    }

    public static UserCommentListingURL parse(Uri uri) {
        Sort order2;
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
            order2 = Sort.parse(uri.getQueryParameter("sort"), uri.getQueryParameter("t"));
        } else {
            order2 = null;
        }
        if (pathSegments.length < 3) {
            return null;
        }
        if (!pathSegments[0].equalsIgnoreCase("user") && !pathSegments[0].equalsIgnoreCase("u")) {
            return null;
        }
        String username = pathSegments[1];
        if (!pathSegments[2].equalsIgnoreCase("comments")) {
            return null;
        }
        Integer limit2 = null;
        String after2 = null;
        for (String parameterKey : General.getUriQueryParameterNames(uri)) {
            if (parameterKey.equalsIgnoreCase("after")) {
                after2 = uri.getQueryParameter(parameterKey);
            } else if (parameterKey.equalsIgnoreCase("limit")) {
                try {
                    limit2 = Integer.valueOf(Integer.parseInt(uri.getQueryParameter(parameterKey)));
                } catch (Throwable th) {
                }
            }
        }
        return new UserCommentListingURL(username, order2, limit2, after2);
    }

    public Uri generateJsonUri() {
        Builder builder = new Builder();
        builder.scheme(Reddit.getScheme()).authority(Reddit.getDomain());
        builder.appendEncodedPath("user");
        builder.appendPath(this.user);
        builder.appendEncodedPath("comments");
        Sort sort = this.order;
        if (sort != null) {
            sort.addToUserCommentListingUri(builder);
        }
        String str = this.after;
        if (str != null) {
            builder.appendQueryParameter("after", str);
        }
        Integer num = this.limit;
        if (num != null) {
            builder.appendQueryParameter("limit", String.valueOf(num));
        }
        builder.appendEncodedPath(".json");
        return builder.build();
    }

    public int pathType() {
        return 5;
    }

    public String humanReadableName(Context context, boolean shorter) {
        String name = context.getString(R.string.user_comments);
        if (shorter) {
            return name;
        }
        return String.format("%s (%s)", new Object[]{name, this.user});
    }
}
