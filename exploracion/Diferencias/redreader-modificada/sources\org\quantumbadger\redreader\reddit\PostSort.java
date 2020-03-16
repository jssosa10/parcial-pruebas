package org.quantumbadger.redreader.reddit;

import android.net.Uri.Builder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import org.quantumbadger.redreader.common.General;

public enum PostSort {
    HOT,
    NEW,
    RISING,
    TOP_HOUR,
    TOP_DAY,
    TOP_WEEK,
    TOP_MONTH,
    TOP_YEAR,
    TOP_ALL,
    CONTROVERSIAL,
    BEST,
    RELEVANCE,
    COMMENTS,
    TOP;

    @Nullable
    public static PostSort valueOfOrNull(@NonNull String string) {
        try {
            return valueOf(General.asciiUppercase(string));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public static PostSort parse(@Nullable String sort, @Nullable String t) {
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
        if (sort2.equals("best")) {
            return BEST;
        }
        if (sort2.equals("controversial")) {
            return CONTROVERSIAL;
        }
        if (sort2.equals("rising")) {
            return RISING;
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

    public void addToUserPostListingUri(@NonNull Builder builder) {
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

    public void addToSubredditListingUri(@NonNull Builder builder) {
        switch (this) {
            case HOT:
            case NEW:
            case CONTROVERSIAL:
            case RISING:
            case BEST:
                builder.appendEncodedPath(General.asciiLowercase(name()));
                return;
            case TOP_HOUR:
            case TOP_DAY:
            case TOP_WEEK:
            case TOP_MONTH:
            case TOP_YEAR:
            case TOP_ALL:
                builder.appendEncodedPath("top");
                builder.appendQueryParameter("t", General.asciiLowercase(name().split("_")[1]));
                return;
            default:
                return;
        }
    }
}
