package org.quantumbadger.redreader.common;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import java.net.URI;
import org.quantumbadger.redreader.RedReader;

public final class Constants {

    public static final class FileType {
        public static final int CAPTCHA = 202;
        public static final int COMMENT_LIST = 120;
        public static final int IMAGE = 201;
        public static final int IMAGE_INFO = 300;
        public static final int INBOX_LIST = 140;
        public static final int MULTIREDDIT_LIST = 102;
        public static final int NOCACHE = -1;
        public static final int POST_LIST = 110;
        public static final int SUBREDDIT_ABOUT = 101;
        public static final int SUBREDDIT_LIST = 100;
        public static final int THUMBNAIL = 200;
        public static final int USER_ABOUT = 130;
    }

    public static final class Mime {
        public static boolean isImage(String mimetype) {
            return General.asciiLowercase(mimetype).startsWith("image/");
        }

        public static boolean isImageGif(String mimetype) {
            return mimetype.equalsIgnoreCase("image/gif");
        }

        public static boolean isVideo(String mimetype) {
            return mimetype.startsWith("video/");
        }
    }

    public static final class Priority {
        public static final int API_ACTION = -500;
        public static final int API_COMMENT_LIST = -300;
        public static final int API_INBOX_LIST = -500;
        public static final int API_MULTIREDDIT_LIST = -200;
        public static final int API_POST_LIST = -200;
        public static final int API_SUBREDDIT_INVIDIVUAL = -250;
        public static final int API_SUBREDDIT_LIST = -100;
        public static final int API_USER_ABOUT = -500;
        public static final int CAPTCHA = -600;
        public static final int COMMENT_PRECACHE = 500;
        public static final int IMAGE_PRECACHE = 500;
        public static final int IMAGE_VIEW = -400;
        public static final int THUMBNAIL = 100;
    }

    public static final class Reddit {
        public static final String[] DEFAULT_SUBREDDITS = {"/r/announcements", "/r/Art", "/r/AskReddit", "/r/askscience", "/r/aww", "/r/blog", "/r/books", "/r/creepy", "/r/dataisbeautiful", "/r/DIY", "/r/Documentaries", "/r/EarthPorn", "/r/explainlikeimfive", "/r/Fitness", "/r/food", "/r/funny", "/r/Futurology", "/r/gadgets", "/r/gaming", "/r/GetMotivated", "/r/gifs", "/r/history", "/r/IAmA", "/r/InternetIsBeautiful", "/r/Jokes", "/r/LifeProTips", "/r/listentothis", "/r/mildlyinteresting", "/r/movies", "/r/Music", "/r/news", "/r/nosleep", "/r/nottheonion", "/r/oldschoolcool", "/r/personalfinance", "/r/philosophy", "/r/photoshopbattles", "/r/pics", "/r/science", "/r/Showerthoughts", "/r/space", "/r/sports", "/r/television", "/r/tifu", "/r/todayilearned", "/r/TwoXChromosomes", "/r/UpliftingNews", "/r/videos", "/r/worldnews", "/r/writingprompts"};
        public static final String DOMAIN_HTTPS = "oauth.reddit.com";
        public static final String DOMAIN_HTTPS_HUMAN = "reddit.com";
        public static final String PATH_COMMENTS = "/comments/";
        public static final String PATH_DELETE = "/api/del";
        public static final String PATH_HIDE = "/api/hide";
        public static final String PATH_ME = "/api/v1/me";
        public static final String PATH_MULTIREDDITS_MINE = "/api/multi/mine.json";
        public static final String PATH_REPORT = "/api/report";
        public static final String PATH_SAVE = "/api/save";
        public static final String PATH_SUBREDDITS_MINE_MODERATOR = "/subreddits/mine/moderator.json?limit=100";
        public static final String PATH_SUBREDDITS_MINE_SUBSCRIBER = "/subreddits/mine/subscriber.json?limit=100";
        public static final String PATH_SUBREDDITS_POPULAR = "/subreddits/popular.json";
        public static final String PATH_SUBSCRIBE = "/api/subscribe";
        public static final String PATH_UNHIDE = "/api/unhide";
        public static final String PATH_UNSAVE = "/api/unsave";
        public static final String PATH_VOTE = "/api/vote";
        public static final String SCHEME_HTTPS = "https";

        public static String getScheme() {
            return SCHEME_HTTPS;
        }

        public static String getDomain() {
            return DOMAIN_HTTPS;
        }

        public static String getHumanReadableDomain() {
            return DOMAIN_HTTPS_HUMAN;
        }

        public static URI getUri(String path) {
            StringBuilder sb = new StringBuilder();
            sb.append(getScheme());
            sb.append("://");
            sb.append(getDomain());
            sb.append(path);
            return General.uriFromString(sb.toString());
        }

        public static URI getNonAPIUri(String path) {
            StringBuilder sb = new StringBuilder();
            sb.append(getScheme());
            sb.append("://reddit.com");
            sb.append(path);
            return General.uriFromString(sb.toString());
        }

        public static boolean isApiErrorUser(String str) {
            return ".error.USER_REQUIRED".equals(str) || "please login to do that".equals(str);
        }

        public static boolean isApiErrorCaptcha(String str) {
            return ".error.BAD_CAPTCHA.field-captcha".equals(str) || "care to try these again?".equals(str);
        }

        public static boolean isApiErrorNotAllowed(String str) {
            return ".error.SUBREDDIT_NOTALLOWED.field-sr".equals(str) || "you aren't allowed to post there.".equals(str);
        }

        public static boolean isApiErrorSubredditRequired(String str) {
            return ".error.SUBREDDIT_REQUIRED.field-sr".equals(str) || "you must specify a subreddit".equals(str);
        }

        public static boolean isApiErrorURLRequired(String str) {
            return ".error.NO_URL.field-url".equals(str) || "a url is required".equals(str);
        }

        public static boolean isApiTooFast(String str) {
            return ".error.RATELIMIT.field-ratelimit".equals(str) || (str != null && str.contains("you are doing that too much"));
        }

        public static boolean isApiTooLong(String str) {
            return "TOO_LONG".equals(str) || (str != null && str.contains("this is too long"));
        }

        public static boolean isApiAlreadySubmitted(String str) {
            return ".error.ALREADY_SUB.field-url".equals(str) || (str != null && str.contains("that link has already been submitted"));
        }

        public static boolean isApiError(String str) {
            return str != null && str.startsWith(".error.");
        }
    }

    public static String version(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String ua(Context context) {
        String canonicalName = RedReader.class.getCanonicalName();
        StringBuilder sb = new StringBuilder();
        sb.append(canonicalName.substring(0, canonicalName.lastIndexOf(46)));
        sb.append("/");
        sb.append(version(context));
        return sb.toString();
    }
}
