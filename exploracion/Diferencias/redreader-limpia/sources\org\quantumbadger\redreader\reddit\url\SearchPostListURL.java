package org.quantumbadger.redreader.reddit.url;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import java.util.ArrayList;
import java.util.List;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.reddit.PostSort;

public class SearchPostListURL extends PostListingURL {
    public final String after;
    public final String before;
    public final Integer limit;
    public final PostSort order;
    public final String query;
    public final String subreddit;

    SearchPostListURL(String subreddit2, String query2, PostSort order2, Integer limit2, String before2, String after2) {
        this.subreddit = subreddit2;
        this.query = query2;
        this.order = order2;
        this.limit = limit2;
        this.before = before2;
        this.after = after2;
    }

    SearchPostListURL(String subreddit2, String query2, Integer limit2, String before2, String after2) {
        this(subreddit2, query2, PostSort.RELEVANCE, limit2, before2, after2);
    }

    public static SearchPostListURL build(String subreddit2, String query2) {
        if (subreddit2 != null) {
            while (subreddit2.startsWith("/")) {
                subreddit2 = subreddit2.substring(1);
            }
            while (subreddit2.startsWith("r/")) {
                subreddit2 = subreddit2.substring(2);
            }
        }
        SearchPostListURL searchPostListURL = new SearchPostListURL(subreddit2, query2, null, null, null);
        return searchPostListURL;
    }

    public PostListingURL after(String after2) {
        SearchPostListURL searchPostListURL = new SearchPostListURL(this.subreddit, this.query, this.order, this.limit, this.before, after2);
        return searchPostListURL;
    }

    public PostListingURL limit(Integer limit2) {
        SearchPostListURL searchPostListURL = new SearchPostListURL(this.subreddit, this.query, this.order, limit2, this.before, this.after);
        return searchPostListURL;
    }

    public SearchPostListURL sort(PostSort newOrder) {
        SearchPostListURL searchPostListURL = new SearchPostListURL(this.subreddit, this.query, newOrder, this.limit, this.before, this.after);
        return searchPostListURL;
    }

    public Uri generateJsonUri() {
        Builder builder = new Builder();
        builder.scheme(Reddit.getScheme()).authority(Reddit.getDomain());
        if (this.subreddit != null) {
            builder.encodedPath("/r/");
            builder.appendPath(this.subreddit);
            builder.appendQueryParameter("restrict_sr", "on");
        } else {
            builder.encodedPath("/");
        }
        builder.appendEncodedPath("search");
        String str = this.query;
        if (str != null) {
            builder.appendQueryParameter("q", str);
        }
        if (this.order != null) {
            switch (this.order) {
                case RELEVANCE:
                case NEW:
                case HOT:
                case TOP:
                case COMMENTS:
                    builder.appendQueryParameter("sort", General.asciiLowercase(this.order.name()));
                    break;
            }
        }
        String str2 = this.before;
        if (str2 != null) {
            builder.appendQueryParameter("before", str2);
        }
        String str3 = this.after;
        if (str3 != null) {
            builder.appendQueryParameter("after", str3);
        }
        Integer num = this.limit;
        if (num != null) {
            builder.appendQueryParameter("limit", String.valueOf(num));
        }
        builder.appendEncodedPath(".json");
        builder.appendQueryParameter("include_over_18", "on");
        return builder.build();
    }

    public int pathType() {
        return 2;
    }

    public static SearchPostListURL parse(Uri uri) {
        Uri uri2 = uri;
        String query2 = "";
        PostSort order2 = null;
        Integer limit2 = null;
        String before2 = null;
        String after2 = null;
        boolean restrict_sr = false;
        for (String parameterKey : General.getUriQueryParameterNames(uri)) {
            if (parameterKey.equalsIgnoreCase("after")) {
                after2 = uri2.getQueryParameter(parameterKey);
            } else if (parameterKey.equalsIgnoreCase("before")) {
                before2 = uri2.getQueryParameter(parameterKey);
            } else if (parameterKey.equalsIgnoreCase("limit")) {
                try {
                    limit2 = Integer.valueOf(Integer.parseInt(uri2.getQueryParameter(parameterKey)));
                } catch (Throwable th) {
                }
            } else if (parameterKey.equalsIgnoreCase("sort")) {
                order2 = PostSort.valueOfOrNull(uri2.getQueryParameter(parameterKey));
            } else if (parameterKey.equalsIgnoreCase("q")) {
                query2 = uri2.getQueryParameter(parameterKey);
            } else if (parameterKey.equalsIgnoreCase("restrict_sr")) {
                restrict_sr = "on".equalsIgnoreCase(uri2.getQueryParameter(parameterKey));
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
        if ((pathSegments.length != 1 && pathSegments.length != 3) || !pathSegments[pathSegments.length - 1].equalsIgnoreCase("search")) {
            return null;
        }
        int length = pathSegments.length;
        if (length == 1) {
            SearchPostListURL searchPostListURL = new SearchPostListURL(null, query2, order2, limit2, before2, after2);
            return searchPostListURL;
        } else if (length != 3 || !pathSegments[0].equals("r")) {
            return null;
        } else {
            SearchPostListURL searchPostListURL2 = new SearchPostListURL(restrict_sr ? pathSegments[1] : null, query2, order2, limit2, before2, after2);
            return searchPostListURL2;
        }
    }

    public String humanReadableName(Context context, boolean shorter) {
        if (shorter) {
            return "Search Results";
        }
        StringBuilder builder = new StringBuilder("Search");
        if (this.query != null) {
            builder.append(" for \"");
            builder.append(this.query);
            builder.append("\"");
        }
        if (this.subreddit != null) {
            builder.append(" on /r/");
            builder.append(this.subreddit);
        }
        return builder.toString();
    }

    public String humanReadablePath() {
        StringBuilder builder = new StringBuilder(super.humanReadablePath());
        if (this.query != null) {
            builder.append("?q=");
            builder.append(this.query);
        }
        return builder.toString();
    }
}
