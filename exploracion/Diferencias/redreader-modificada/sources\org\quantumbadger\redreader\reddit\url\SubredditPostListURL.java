package org.quantumbadger.redreader.reddit.url;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.reddit.PostSort;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit.InvalidSubredditNameException;
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;

public class SubredditPostListURL extends PostListingURL {
    @Nullable
    public final String after;
    @Nullable
    public final String before;
    @Nullable
    public final Integer limit;
    @Nullable
    public final PostSort order;
    public final String subreddit;
    public final Type type;

    public enum Type {
        FRONTPAGE,
        ALL,
        SUBREDDIT,
        SUBREDDIT_COMBINATION,
        ALL_SUBTRACTION,
        POPULAR,
        RANDOM
    }

    public static SubredditPostListURL getFrontPage() {
        SubredditPostListURL subredditPostListURL = new SubredditPostListURL(Type.FRONTPAGE, null, null, null, null, null);
        return subredditPostListURL;
    }

    public static SubredditPostListURL getPopular() {
        SubredditPostListURL subredditPostListURL = new SubredditPostListURL(Type.POPULAR, null, null, null, null, null);
        return subredditPostListURL;
    }

    public static SubredditPostListURL getRandom() {
        SubredditPostListURL subredditPostListURL = new SubredditPostListURL(Type.RANDOM, "random", null, null, null, null);
        return subredditPostListURL;
    }

    public static SubredditPostListURL getRandomNsfw() {
        SubredditPostListURL subredditPostListURL = new SubredditPostListURL(Type.RANDOM, "randnsfw", null, null, null, null);
        return subredditPostListURL;
    }

    public static SubredditPostListURL getAll() {
        SubredditPostListURL subredditPostListURL = new SubredditPostListURL(Type.ALL, null, null, null, null, null);
        return subredditPostListURL;
    }

    public static RedditURL getSubreddit(String subreddit2) throws InvalidSubredditNameException {
        Builder builder = new Builder();
        builder.scheme(Reddit.getScheme()).authority(Reddit.getDomain());
        builder.encodedPath("/r/");
        builder.appendPath(RedditSubreddit.stripRPrefix(subreddit2));
        return RedditURLParser.parse(builder.build());
    }

    private SubredditPostListURL(Type type2, String subreddit2, @Nullable PostSort order2, @Nullable Integer limit2, @Nullable String before2, @Nullable String after2) {
        this.type = type2;
        this.subreddit = subreddit2;
        this.order = order2;
        this.limit = limit2;
        this.before = before2;
        this.after = after2;
    }

    public SubredditPostListURL after(String newAfter) {
        SubredditPostListURL subredditPostListURL = new SubredditPostListURL(this.type, this.subreddit, this.order, this.limit, this.before, newAfter);
        return subredditPostListURL;
    }

    public SubredditPostListURL limit(Integer newLimit) {
        SubredditPostListURL subredditPostListURL = new SubredditPostListURL(this.type, this.subreddit, this.order, newLimit, this.before, this.after);
        return subredditPostListURL;
    }

    public SubredditPostListURL sort(PostSort newOrder) {
        SubredditPostListURL subredditPostListURL = new SubredditPostListURL(this.type, this.subreddit, newOrder, this.limit, this.before, this.after);
        return subredditPostListURL;
    }

    public PostSort getOrder() {
        return this.order;
    }

    public Uri generateJsonUri() {
        Builder builder = new Builder();
        builder.scheme(Reddit.getScheme()).authority(Reddit.getDomain());
        switch (this.type) {
            case FRONTPAGE:
                builder.encodedPath("/");
                break;
            case ALL:
                builder.encodedPath("/r/all");
                break;
            case SUBREDDIT:
            case SUBREDDIT_COMBINATION:
            case ALL_SUBTRACTION:
            case RANDOM:
                builder.encodedPath("/r/");
                builder.appendPath(this.subreddit);
                break;
            case POPULAR:
                builder.encodedPath("/r/popular");
                break;
        }
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
        return 0;
    }

    public static SubredditPostListURL parse(Uri uri) {
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
        switch (pathSegments.length) {
            case 0:
                SubredditPostListURL subredditPostListURL = new SubredditPostListURL(Type.FRONTPAGE, null, null, limit2, before2, after2);
                return subredditPostListURL;
            case 1:
                if (order2 == null) {
                    return null;
                }
                SubredditPostListURL subredditPostListURL2 = new SubredditPostListURL(Type.FRONTPAGE, null, order2, limit2, before2, after2);
                return subredditPostListURL2;
            case 2:
            case 3:
                if (!pathSegments[0].equals("r")) {
                    return null;
                }
                String subreddit2 = General.asciiLowercase(pathSegments[1]);
                if (subreddit2.equals("all")) {
                    if (pathSegments.length == 2) {
                        SubredditPostListURL subredditPostListURL3 = new SubredditPostListURL(Type.ALL, null, null, limit2, before2, after2);
                        return subredditPostListURL3;
                    } else if (order2 == null) {
                        return null;
                    } else {
                        SubredditPostListURL subredditPostListURL4 = new SubredditPostListURL(Type.ALL, null, order2, limit2, before2, after2);
                        return subredditPostListURL4;
                    }
                } else if (subreddit2.equals("popular")) {
                    SubredditPostListURL subredditPostListURL5 = new SubredditPostListURL(Type.POPULAR, null, order2, limit2, before2, after2);
                    return subredditPostListURL5;
                } else if (subreddit2.equals("random") || subreddit2.equals("randnsfw")) {
                    SubredditPostListURL subredditPostListURL6 = new SubredditPostListURL(Type.RANDOM, subreddit2, order2, limit2, before2, after2);
                    return subredditPostListURL6;
                } else if (subreddit2.matches("all(\\-[\\w\\.]+)+")) {
                    if (pathSegments.length == 2) {
                        SubredditPostListURL subredditPostListURL7 = new SubredditPostListURL(Type.ALL_SUBTRACTION, subreddit2, null, limit2, before2, after2);
                        return subredditPostListURL7;
                    } else if (order2 == null) {
                        return null;
                    } else {
                        SubredditPostListURL subredditPostListURL8 = new SubredditPostListURL(Type.ALL_SUBTRACTION, subreddit2, order2, limit2, before2, after2);
                        return subredditPostListURL8;
                    }
                } else if (subreddit2.matches("\\w+(\\+[\\w\\.]+)+")) {
                    if (pathSegments.length == 2) {
                        SubredditPostListURL subredditPostListURL9 = new SubredditPostListURL(Type.SUBREDDIT_COMBINATION, subreddit2, null, limit2, before2, after2);
                        return subredditPostListURL9;
                    } else if (order2 == null) {
                        return null;
                    } else {
                        SubredditPostListURL subredditPostListURL10 = new SubredditPostListURL(Type.SUBREDDIT_COMBINATION, subreddit2, order2, limit2, before2, after2);
                        return subredditPostListURL10;
                    }
                } else if (!subreddit2.matches("[\\w\\.]+")) {
                    return null;
                } else {
                    if (pathSegments.length == 2) {
                        SubredditPostListURL subredditPostListURL11 = new SubredditPostListURL(Type.SUBREDDIT, subreddit2, null, limit2, before2, after2);
                        return subredditPostListURL11;
                    } else if (order2 == null) {
                        return null;
                    } else {
                        SubredditPostListURL subredditPostListURL12 = new SubredditPostListURL(Type.SUBREDDIT, subreddit2, order2, limit2, before2, after2);
                        return subredditPostListURL12;
                    }
                }
            default:
                return null;
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
        switch (this.type) {
            case FRONTPAGE:
                return context.getString(R.string.mainmenu_all);
            case ALL:
                return context.getString(R.string.mainmenu_frontpage);
            case SUBREDDIT:
                try {
                    return RedditSubreddit.getCanonicalName(this.subreddit);
                } catch (InvalidSubredditNameException e) {
                    return this.subreddit;
                }
            case SUBREDDIT_COMBINATION:
            case ALL_SUBTRACTION:
                return this.subreddit;
            case RANDOM:
                return context.getString("randnsfw".equals(this.subreddit) ? R.string.mainmenu_random_nsfw : R.string.mainmenu_random);
            case POPULAR:
                return context.getString(R.string.mainmenu_popular);
            default:
                return super.humanReadableName(context, shorter);
        }
    }

    public SubredditPostListURL changeSubreddit(String newSubreddit) {
        SubredditPostListURL subredditPostListURL = new SubredditPostListURL(this.type, newSubreddit, this.order, this.limit, this.before, this.after);
        return subredditPostListURL;
    }
}
