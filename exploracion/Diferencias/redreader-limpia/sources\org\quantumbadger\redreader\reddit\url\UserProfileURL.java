package org.quantumbadger.redreader.reddit.url;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import java.util.ArrayList;
import java.util.List;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;

public class UserProfileURL extends RedditURL {
    public final String username;

    public UserProfileURL(String username2) {
        this.username = username2;
    }

    public static UserProfileURL parse(Uri uri) {
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
        if (pathSegments.length != 2) {
            return null;
        }
        if (pathSegments[0].equalsIgnoreCase("user") || pathSegments[0].equalsIgnoreCase("u")) {
            return new UserProfileURL(pathSegments[1]);
        }
        return null;
    }

    public Uri generateJsonUri() {
        Builder builder = new Builder();
        builder.scheme(Reddit.getScheme()).authority(Reddit.getDomain());
        builder.appendEncodedPath("user");
        builder.appendPath(this.username);
        builder.appendEncodedPath(".json");
        return builder.build();
    }

    public int pathType() {
        return 4;
    }

    public String humanReadableName(Context context, boolean shorter) {
        return this.username;
    }
}
