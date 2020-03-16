package org.quantumbadger.redreader.reddit.url;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;

public class PostCommentListingURL extends CommentListingURL {
    public final String after;
    public final String commentId;
    public final Integer context;
    public final Integer limit;
    public final Sort order;
    public final String postId;

    public enum Sort {
        BEST("confidence"),
        HOT("hot"),
        NEW("new"),
        OLD("old"),
        TOP("top"),
        CONTROVERSIAL("controversial"),
        QA("qa");
        
        public final String key;

        private Sort(String key2) {
            this.key = key2;
        }

        public static Sort lookup(String name) {
            String name2 = General.asciiUppercase(name);
            if (name2.equals("CONFIDENCE")) {
                return BEST;
            }
            try {
                return valueOf(name2);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public static PostCommentListingURL forPostId(String postId2) {
        PostCommentListingURL postCommentListingURL = new PostCommentListingURL(null, postId2, null, null, null, null);
        return postCommentListingURL;
    }

    public PostCommentListingURL(String after2, String postId2, String commentId2, Integer context2, Integer limit2, Sort order2) {
        if (postId2 != null && postId2.startsWith("t3_")) {
            postId2 = postId2.substring(3);
        }
        if (commentId2 != null && commentId2.startsWith("t1_")) {
            commentId2 = commentId2.substring(3);
        }
        this.after = after2;
        this.postId = postId2;
        this.commentId = commentId2;
        this.context = context2;
        this.limit = limit2;
        this.order = order2;
    }

    public PostCommentListingURL after(String after2) {
        PostCommentListingURL postCommentListingURL = new PostCommentListingURL(after2, this.postId, this.commentId, this.context, this.limit, this.order);
        return postCommentListingURL;
    }

    public PostCommentListingURL limit(Integer limit2) {
        PostCommentListingURL postCommentListingURL = new PostCommentListingURL(this.after, this.postId, this.commentId, this.context, limit2, this.order);
        return postCommentListingURL;
    }

    public PostCommentListingURL context(Integer context2) {
        PostCommentListingURL postCommentListingURL = new PostCommentListingURL(this.after, this.postId, this.commentId, context2, this.limit, this.order);
        return postCommentListingURL;
    }

    public PostCommentListingURL order(Sort order2) {
        PostCommentListingURL postCommentListingURL = new PostCommentListingURL(this.after, this.postId, this.commentId, this.context, this.limit, order2);
        return postCommentListingURL;
    }

    public PostCommentListingURL commentId(String commentId2) {
        if (commentId2 != null && commentId2.startsWith("t1_")) {
            commentId2 = commentId2.substring(3);
        }
        PostCommentListingURL postCommentListingURL = new PostCommentListingURL(this.after, this.postId, commentId2, this.context, this.limit, this.order);
        return postCommentListingURL;
    }

    public Uri generateJsonUri() {
        Builder builder = new Builder();
        builder.scheme(Reddit.getScheme()).authority(Reddit.getDomain());
        internalGenerateCommon(builder);
        builder.appendEncodedPath(".json");
        return builder.build();
    }

    public Uri generateNonJsonUri() {
        Builder builder = new Builder();
        builder.scheme(Reddit.getScheme()).authority(Reddit.getHumanReadableDomain());
        internalGenerateCommon(builder);
        return builder.build();
    }

    private void internalGenerateCommon(@NonNull Builder builder) {
        builder.encodedPath("/comments");
        builder.appendPath(this.postId);
        if (this.commentId != null) {
            builder.appendEncodedPath("comment");
            builder.appendPath(this.commentId);
            Integer num = this.context;
            if (num != null) {
                builder.appendQueryParameter("context", num.toString());
            }
        }
        String str = this.after;
        if (str != null) {
            builder.appendQueryParameter("after", str);
        }
        Integer num2 = this.limit;
        if (num2 != null) {
            builder.appendQueryParameter("limit", num2.toString());
        }
        Sort sort = this.order;
        if (sort != null) {
            builder.appendQueryParameter("sort", sort.key);
        }
    }

    public static PostCommentListingURL parse(Uri uri) {
        Uri uri2 = uri;
        List<String> pathSegmentsList = uri.getPathSegments();
        ArrayList<String> pathSegmentsFiltered = new ArrayList<>(pathSegmentsList.size());
        for (String segment : pathSegmentsList) {
            while (true) {
                if (!General.asciiLowercase(segment).endsWith(".json") && !General.asciiLowercase(segment).endsWith(".xml")) {
                    break;
                }
                segment = segment.substring(0, segment.lastIndexOf(46));
            }
            pathSegmentsFiltered.add(segment);
        }
        String[] pathSegments = (String[]) pathSegmentsFiltered.toArray(new String[pathSegmentsFiltered.size()]);
        if (pathSegments.length == 1 && uri.getHost().equals("redd.it")) {
            PostCommentListingURL postCommentListingURL = new PostCommentListingURL(null, pathSegments[0], null, null, null, null);
            return postCommentListingURL;
        } else if (pathSegments.length < 2) {
            return null;
        } else {
            int offset = 0;
            if (pathSegments[0].equalsIgnoreCase("r")) {
                offset = 2;
                if (pathSegments.length - 2 < 2) {
                    return null;
                }
            }
            if (!pathSegments[offset].equalsIgnoreCase("comments")) {
                return null;
            }
            String commentId2 = null;
            String postId2 = pathSegments[offset + 1];
            int offset2 = offset + 2;
            if (pathSegments.length - offset2 >= 2) {
                commentId2 = pathSegments[offset2 + 1];
            }
            Integer limit2 = null;
            Integer context2 = null;
            Sort order2 = null;
            String after2 = null;
            for (String parameterKey : General.getUriQueryParameterNames(uri)) {
                if (parameterKey.equalsIgnoreCase("after")) {
                    after2 = uri2.getQueryParameter(parameterKey);
                } else if (parameterKey.equalsIgnoreCase("limit")) {
                    try {
                        limit2 = Integer.valueOf(Integer.parseInt(uri2.getQueryParameter(parameterKey)));
                    } catch (Throwable th) {
                    }
                } else if (parameterKey.equalsIgnoreCase("context")) {
                    try {
                        context2 = Integer.valueOf(Integer.parseInt(uri2.getQueryParameter(parameterKey)));
                    } catch (Throwable th2) {
                    }
                } else if (parameterKey.equalsIgnoreCase("sort")) {
                    order2 = Sort.lookup(uri2.getQueryParameter(parameterKey));
                }
            }
            PostCommentListingURL postCommentListingURL2 = new PostCommentListingURL(after2, postId2, commentId2, context2, limit2, order2);
            return postCommentListingURL2;
        }
    }

    public int pathType() {
        return 7;
    }

    public String humanReadableName(Context context2, boolean shorter) {
        return super.humanReadableName(context2, shorter);
    }
}
