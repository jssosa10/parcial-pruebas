package org.quantumbadger.redreader.common;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.lang3.time.DateUtils;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuItemsPref;
import org.quantumbadger.redreader.adapters.MainMenuListingManager.SubredditAction;
import org.quantumbadger.redreader.common.Constants.FileType;
import org.quantumbadger.redreader.common.LinkHandler.LinkAction;
import org.quantumbadger.redreader.fragments.MainMenuFragment.MainMenuShortcutItems;
import org.quantumbadger.redreader.fragments.MainMenuFragment.MainMenuUserItems;
import org.quantumbadger.redreader.io.WritableHashSet;
import org.quantumbadger.redreader.reddit.PostSort;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost.Action;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit.InvalidSubredditNameException;
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL.Sort;

public final class PrefsUtility {

    public enum AlbumViewMode {
        INTERNAL_LIST,
        INTERNAL_BROWSER,
        EXTERNAL_BROWSER
    }

    public enum AppearanceCommentHeaderItem {
        AUTHOR,
        FLAIR,
        SCORE,
        AGE,
        GOLD
    }

    public enum AppearanceNavbarColour {
        BLACK,
        PRIMARY,
        PRIMARYDARK
    }

    public enum AppearanceTheme {
        RED,
        GREEN,
        BLUE,
        LTBLUE,
        ORANGE,
        GRAY,
        NIGHT,
        NIGHT_LOWCONTRAST,
        ULTRABLACK
    }

    public enum AppearanceThumbnailsShow {
        NEVER,
        WIFIONLY,
        ALWAYS
    }

    public enum AppearanceTwopane {
        NEVER,
        AUTO,
        FORCE
    }

    public enum BlockedSubredditSort {
        NAME,
        DATE
    }

    public enum CachePrecacheComments {
        NEVER,
        WIFIONLY,
        ALWAYS
    }

    public enum CachePrecacheImages {
        NEVER,
        WIFIONLY,
        ALWAYS
    }

    public enum CommentAction {
        COLLAPSE,
        ACTION_MENU,
        NOTHING
    }

    public enum CommentFlingAction {
        UPVOTE,
        DOWNVOTE,
        SAVE,
        REPLY,
        USER_PROFILE,
        COLLAPSE,
        ACTION_MENU,
        PROPERTIES,
        BACK,
        DISABLED
    }

    public enum GifViewMode {
        INTERNAL_MOVIE(true),
        INTERNAL_LEGACY(true),
        INTERNAL_BROWSER(false),
        EXTERNAL_BROWSER(false);
        
        public final boolean downloadInApp;

        private GifViewMode(boolean downloadInApp2) {
            this.downloadInApp = downloadInApp2;
        }
    }

    public enum ImageViewMode {
        INTERNAL_OPENGL(true),
        INTERNAL_BROWSER(false),
        EXTERNAL_BROWSER(false);
        
        public final boolean downloadInApp;

        private ImageViewMode(boolean downloadInApp2) {
            this.downloadInApp = downloadInApp2;
        }
    }

    public enum PinnedSubredditSort {
        NAME,
        DATE
    }

    public enum PostCount {
        R25,
        R50,
        R100,
        ALL
    }

    public enum PostFlingAction {
        UPVOTE,
        DOWNVOTE,
        SAVE,
        HIDE,
        COMMENTS,
        LINK,
        ACTION_MENU,
        BROWSER,
        BACK,
        DISABLED
    }

    public enum ScreenOrientation {
        AUTO,
        PORTRAIT,
        LANDSCAPE
    }

    public enum SelfpostAction {
        COLLAPSE,
        NOTHING
    }

    public enum VideoViewMode {
        INTERNAL_VIDEOVIEW(true),
        INTERNAL_BROWSER(false),
        EXTERNAL_BROWSER(false),
        EXTERNAL_APP_VLC(true);
        
        public final boolean downloadInApp;

        private VideoViewMode(boolean downloadInApp2) {
            this.downloadInApp = downloadInApp2;
        }
    }

    private static <E> Set<E> setFromArray(E[] data) {
        HashSet<E> result = new HashSet<>(data.length);
        Collections.addAll(result, data);
        return result;
    }

    private static String getString(int id, String defaultValue, Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getString(context.getString(id), defaultValue);
    }

    public static Set<String> getStringSet(int id, int defaultArrayRes, Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getStringSet(context.getString(id), setFromArray(context.getResources().getStringArray(defaultArrayRes)));
    }

    private static boolean getBoolean(int id, boolean defaultValue, Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getBoolean(context.getString(id), defaultValue);
    }

    private static long getLong(int id, long defaultValue, Context context, SharedPreferences sharedPreferences) {
        return sharedPreferences.getLong(context.getString(id), defaultValue);
    }

    public static boolean isReLayoutRequired(Context context, String key) {
        return context.getString(R.string.pref_appearance_twopane_key).equals(key) || context.getString(R.string.pref_appearance_theme_key).equals(key) || context.getString(R.string.pref_menus_mainmenu_useritems_key).equals(key) || context.getString(R.string.pref_menus_mainmenu_shortcutitems_key).equals(key);
    }

    public static boolean isRefreshRequired(Context context, String key) {
        return key.startsWith("pref_appearance") || key.equals(context.getString(R.string.pref_behaviour_fling_post_left_key)) || key.equals(context.getString(R.string.pref_behaviour_fling_post_right_key)) || key.equals(context.getString(R.string.pref_behaviour_nsfw_key)) || key.equals(context.getString(R.string.pref_behaviour_postcount_key)) || key.equals(context.getString(R.string.pref_behaviour_comment_min_key)) || key.equals(context.getString(R.string.pref_behaviour_pinned_subredditsort_key)) || key.equals(context.getString(R.string.pref_behaviour_blocked_subredditsort_key));
    }

    public static boolean isRestartRequired(Context context, String key) {
        return context.getString(R.string.pref_appearance_theme_key).equals(key) || context.getString(R.string.pref_appearance_navbar_color_key).equals(key) || context.getString(R.string.pref_appearance_langforce_key).equals(key) || context.getString(R.string.pref_behaviour_bezel_toolbar_swipezone_key).equals(key) || context.getString(R.string.pref_appearance_hide_username_main_menu_key).equals(key) || context.getString(R.string.pref_appearance_hide_android_status_key).equals(key) || context.getString(R.string.pref_appearance_comments_show_floating_toolbar_key).equals(key) || context.getString(R.string.pref_behaviour_enable_swipe_refresh_key).equals(key) || context.getString(R.string.pref_menus_show_multireddit_main_menu_key).equals(key) || context.getString(R.string.pref_menus_show_subscribed_subreddits_main_menu_key).equals(key) || context.getString(R.string.pref_appearance_bottom_toolbar_key).equals(key);
    }

    public static AppearanceTwopane appearance_twopane(Context context, SharedPreferences sharedPreferences) {
        return AppearanceTwopane.valueOf(General.asciiUppercase(getString(R.string.pref_appearance_twopane_key, "auto", context, sharedPreferences)));
    }

    public static boolean isNightMode(Context context) {
        AppearanceTheme theme = appearance_theme(context, PreferenceManager.getDefaultSharedPreferences(context));
        return theme == AppearanceTheme.NIGHT || theme == AppearanceTheme.NIGHT_LOWCONTRAST || theme == AppearanceTheme.ULTRABLACK;
    }

    public static AppearanceTheme appearance_theme(Context context, SharedPreferences sharedPreferences) {
        return AppearanceTheme.valueOf(General.asciiUppercase(getString(R.string.pref_appearance_theme_key, "red", context, sharedPreferences)));
    }

    public static AppearanceNavbarColour appearance_navbar_colour(Context context, SharedPreferences sharedPreferences) {
        return AppearanceNavbarColour.valueOf(General.asciiUppercase(getString(R.string.pref_appearance_navbar_color_key, "black", context, sharedPreferences)));
    }

    public static void applyTheme(@NonNull Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        switch (appearance_theme(activity, prefs)) {
            case RED:
                activity.setTheme(R.style.RR_Light_Red);
                break;
            case GREEN:
                activity.setTheme(R.style.RR_Light_Green);
                break;
            case BLUE:
                activity.setTheme(R.style.RR_Light_Blue);
                break;
            case LTBLUE:
                activity.setTheme(R.style.RR_Light_LtBlue);
                break;
            case ORANGE:
                activity.setTheme(R.style.RR_Light_Orange);
                break;
            case GRAY:
                activity.setTheme(R.style.RR_Light_Gray);
                break;
            case NIGHT:
                activity.setTheme(R.style.RR_Dark);
                break;
            case NIGHT_LOWCONTRAST:
                activity.setTheme(R.style.RR_Dark_LowContrast);
                break;
            case ULTRABLACK:
                activity.setTheme(R.style.RR_Dark_UltraBlack);
                break;
        }
        applyLanguage(activity, prefs);
    }

    public static void applySettingsTheme(@NonNull Activity activity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        activity.setTheme(R.style.RR_Settings);
        applyLanguage(activity, prefs);
    }

    private static void applyLanguage(Activity activity, SharedPreferences prefs) {
        Resources[] resourcesArr;
        String lang = getString(R.string.pref_appearance_langforce_key, "auto", activity, prefs);
        for (Resources res : new Resources[]{activity.getResources(), activity.getApplication().getResources()}) {
            DisplayMetrics dm = res.getDisplayMetrics();
            Configuration conf = res.getConfiguration();
            if (lang.equals("auto")) {
                conf.locale = Locale.getDefault();
            } else if (lang.contains("-r")) {
                String[] split = lang.split("-r");
                conf.locale = new Locale(split[0], split[1]);
            } else {
                conf.locale = new Locale(lang);
            }
            res.updateConfiguration(conf, dm);
        }
    }

    public static AppearanceThumbnailsShow appearance_thumbnails_show(Context context, SharedPreferences sharedPreferences) {
        if (!getBoolean(R.string.pref_appearance_thumbnails_show_key, true, context, sharedPreferences)) {
            return AppearanceThumbnailsShow.NEVER;
        }
        if (getBoolean(R.string.pref_appearance_thumbnails_wifionly_key, false, context, sharedPreferences)) {
            return AppearanceThumbnailsShow.WIFIONLY;
        }
        return AppearanceThumbnailsShow.ALWAYS;
    }

    public static boolean appearance_thumbnails_nsfw_show(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_appearance_thumbnails_nsfw_show_key, false, context, sharedPreferences);
    }

    public static float appearance_fontscale_comments(Context context, SharedPreferences sharedPreferences) {
        return Float.valueOf(getString(R.string.pref_appearance_fontscale_comments_key, "1", context, sharedPreferences)).floatValue();
    }

    public static float appearance_fontscale_inbox(Context context, SharedPreferences sharedPreferences) {
        return Float.valueOf(getString(R.string.pref_appearance_fontscale_inbox_key, "1", context, sharedPreferences)).floatValue();
    }

    public static float appearance_fontscale_posts(Context context, SharedPreferences sharedPreferences) {
        return Float.valueOf(getString(R.string.pref_appearance_fontscale_posts_key, "1", context, sharedPreferences)).floatValue();
    }

    public static boolean pref_appearance_hide_username_main_menu(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_appearance_hide_username_main_menu_key, false, context, sharedPreferences);
    }

    public static boolean pref_show_popular_main_menu(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_menus_show_popular_main_menu_key, false, context, sharedPreferences);
    }

    public static boolean pref_show_random_main_menu(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_menus_show_random_main_menu_key, false, context, sharedPreferences);
    }

    public static boolean pref_show_multireddit_main_menu(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_menus_show_multireddit_main_menu_key, true, context, sharedPreferences);
    }

    public static boolean pref_show_subscribed_subreddits_main_menu(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_menus_show_subscribed_subreddits_main_menu_key, true, context, sharedPreferences);
    }

    public static boolean pref_appearance_show_blocked_subreddits_main_menu(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_appearance_show_blocked_subreddits_main_menu_key, false, context, sharedPreferences);
    }

    public static boolean pref_appearance_linkbuttons(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_appearance_linkbuttons_key, true, context, sharedPreferences);
    }

    public static boolean pref_appearance_hide_android_status(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_appearance_hide_android_status_key, false, context, sharedPreferences);
    }

    public static boolean pref_appearance_link_text_clickable(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_appearance_link_text_clickable_key, true, context, sharedPreferences);
    }

    public static boolean pref_appearance_image_viewer_show_floating_toolbar(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_appearance_image_viewer_show_floating_toolbar_key, true, context, sharedPreferences);
    }

    public static boolean pref_appearance_show_aspect_ratio_indicator(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_appearance_show_aspect_ratio_indicator_key, false, context, sharedPreferences);
    }

    public static boolean pref_appearance_comments_show_floating_toolbar(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_appearance_comments_show_floating_toolbar_key, true, context, sharedPreferences);
    }

    public static boolean pref_appearance_indentlines(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_appearance_indentlines_key, false, context, sharedPreferences);
    }

    public static boolean pref_appearance_left_handed(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_appearance_left_handed_key, false, context, sharedPreferences);
    }

    public static boolean pref_appearance_bottom_toolbar(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_appearance_bottom_toolbar_key, false, context, sharedPreferences);
    }

    public static EnumSet<AppearanceCommentHeaderItem> appearance_comment_header_items(Context context, SharedPreferences sharedPreferences) {
        Set<String> strings = getStringSet(R.string.pref_appearance_comment_header_items_key, R.array.pref_appearance_comment_header_items_default, context, sharedPreferences);
        EnumSet<AppearanceCommentHeaderItem> result = EnumSet.noneOf(AppearanceCommentHeaderItem.class);
        for (String s : strings) {
            if (!s.equalsIgnoreCase("ups_downs")) {
                try {
                    result.add(AppearanceCommentHeaderItem.valueOf(General.asciiUppercase(s)));
                } catch (IllegalArgumentException e) {
                }
            }
        }
        return result;
    }

    public static boolean pref_behaviour_skiptofrontpage(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_behaviour_skiptofrontpage_key, false, context, sharedPreferences);
    }

    public static boolean pref_behaviour_useinternalbrowser(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_behaviour_useinternalbrowser_key, true, context, sharedPreferences);
    }

    public static boolean pref_behaviour_usecustomtabs(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_behaviour_usecustomtabs_key, false, context, sharedPreferences);
    }

    public static boolean pref_behaviour_notifications(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_behaviour_notifications_key, true, context, sharedPreferences);
    }

    public static boolean pref_behaviour_enable_swipe_refresh(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_behaviour_enable_swipe_refresh_key, true, context, sharedPreferences);
    }

    public static boolean pref_behaviour_video_playback_controls(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_behaviour_video_playback_controls_key, false, context, sharedPreferences);
    }

    public static boolean pref_behaviour_video_mute_default(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_behaviour_video_mute_default_key, true, context, sharedPreferences);
    }

    public static int pref_behaviour_bezel_toolbar_swipezone_dp(Context context, SharedPreferences sharedPreferences) {
        try {
            return Integer.parseInt(getString(R.string.pref_behaviour_bezel_toolbar_swipezone_key, "10", context, sharedPreferences));
        } catch (Throwable th) {
            return 10;
        }
    }

    public static int pref_behaviour_gallery_swipe_length_dp(Context context, SharedPreferences sharedPreferences) {
        try {
            return Integer.parseInt(getString(R.string.pref_behaviour_gallery_swipe_length_key, "150", context, sharedPreferences));
        } catch (Throwable th) {
            return 150;
        }
    }

    public static Integer pref_behaviour_comment_min(Context context, SharedPreferences sharedPreferences) {
        Integer defaultValue = Integer.valueOf(-4);
        String value = getString(R.string.pref_behaviour_comment_min_key, defaultValue.toString(), context, sharedPreferences);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(value));
        } catch (Throwable th) {
            return defaultValue;
        }
    }

    public static ImageViewMode pref_behaviour_imageview_mode(Context context, SharedPreferences sharedPreferences) {
        return ImageViewMode.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_imageview_mode_key, "internal_opengl", context, sharedPreferences)));
    }

    public static AlbumViewMode pref_behaviour_albumview_mode(Context context, SharedPreferences sharedPreferences) {
        return AlbumViewMode.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_albumview_mode_key, "internal_list", context, sharedPreferences)));
    }

    public static GifViewMode pref_behaviour_gifview_mode(Context context, SharedPreferences sharedPreferences) {
        return GifViewMode.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_gifview_mode_key, "internal_movie", context, sharedPreferences)));
    }

    public static VideoViewMode pref_behaviour_videoview_mode(Context context, SharedPreferences sharedPreferences) {
        return VideoViewMode.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_videoview_mode_key, "internal_videoview", context, sharedPreferences)));
    }

    public static PostFlingAction pref_behaviour_fling_post_left(Context context, SharedPreferences sharedPreferences) {
        return PostFlingAction.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_fling_post_left_key, "downvote", context, sharedPreferences)));
    }

    public static PostFlingAction pref_behaviour_fling_post_right(Context context, SharedPreferences sharedPreferences) {
        return PostFlingAction.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_fling_post_right_key, "upvote", context, sharedPreferences)));
    }

    public static SelfpostAction pref_behaviour_self_post_tap_actions(Context context, SharedPreferences sharedPreferences) {
        return SelfpostAction.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_self_post_tap_actions_key, "collapse", context, sharedPreferences)));
    }

    public static CommentFlingAction pref_behaviour_fling_comment_left(Context context, SharedPreferences sharedPreferences) {
        return CommentFlingAction.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_fling_comment_left_key, "downvote", context, sharedPreferences)));
    }

    public static CommentFlingAction pref_behaviour_fling_comment_right(Context context, SharedPreferences sharedPreferences) {
        return CommentFlingAction.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_fling_comment_right_key, "upvote", context, sharedPreferences)));
    }

    public static CommentAction pref_behaviour_actions_comment_tap(Context context, SharedPreferences sharedPreferences) {
        return CommentAction.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_actions_comment_tap_key, "collapse", context, sharedPreferences)));
    }

    public static CommentAction pref_behaviour_actions_comment_longclick(Context context, SharedPreferences sharedPreferences) {
        return CommentAction.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_actions_comment_longclick_key, "action_menu", context, sharedPreferences)));
    }

    public static boolean pref_behaviour_sharing_share_text(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_behaviour_sharing_share_text_key, true, context, sharedPreferences);
    }

    public static boolean pref_behaviour_sharing_include_desc(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_behaviour_sharing_include_desc_key, true, context, sharedPreferences);
    }

    public static PostSort pref_behaviour_postsort(Context context, SharedPreferences sharedPreferences) {
        return PostSort.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_postsort_key, "hot", context, sharedPreferences)));
    }

    public static Sort pref_behaviour_commentsort(Context context, SharedPreferences sharedPreferences) {
        return Sort.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_commentsort_key, "best", context, sharedPreferences)));
    }

    public static PinnedSubredditSort pref_behaviour_pinned_subredditsort(Context context, SharedPreferences sharedPreferences) {
        return PinnedSubredditSort.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_pinned_subredditsort_key, "name", context, sharedPreferences)));
    }

    public static BlockedSubredditSort pref_behaviour_blocked_subredditsort(Context context, SharedPreferences sharedPreferences) {
        return BlockedSubredditSort.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_blocked_subredditsort_key, "name", context, sharedPreferences)));
    }

    public static boolean pref_behaviour_nsfw(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_behaviour_nsfw_key, false, context, sharedPreferences);
    }

    public static boolean pref_behaviour_hide_read_posts(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_behaviour_hide_read_posts_key, false, context, sharedPreferences);
    }

    public static boolean pref_behaviour_share_permalink(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_behaviour_share_permalink_key, false, context, sharedPreferences);
    }

    public static PostCount pref_behaviour_post_count(Context context, SharedPreferences sharedPreferences) {
        return PostCount.valueOf(getString(R.string.pref_behaviour_postcount_key, "ALL", context, sharedPreferences));
    }

    public static ScreenOrientation pref_behaviour_screen_orientation(Context context, SharedPreferences sharedPreferences) {
        return ScreenOrientation.valueOf(General.asciiUppercase(getString(R.string.pref_behaviour_screenorientation_key, ScreenOrientation.AUTO.name(), context, sharedPreferences)));
    }

    public static String pref_cache_location(Context context, SharedPreferences sharedPreferences) {
        File defaultCacheDir = context.getExternalCacheDir();
        if (defaultCacheDir == null) {
            defaultCacheDir = context.getCacheDir();
        }
        return getString(R.string.pref_cache_location_key, defaultCacheDir.getAbsolutePath(), context, sharedPreferences);
    }

    public static void pref_cache_location(Context context, SharedPreferences sharedPreferences, String path) {
        sharedPreferences.edit().putString(context.getString(R.string.pref_cache_location_key), path).apply();
    }

    public static long pref_cache_rerequest_postlist_age_ms(Context context, SharedPreferences sharedPreferences) {
        try {
            return General.hoursToMs((long) Integer.parseInt(getString(R.string.pref_cache_rerequest_postlist_age_key, "1", context, sharedPreferences)));
        } catch (Throwable th) {
            return 1;
        }
    }

    public static HashMap<Integer, Long> pref_cache_maxage(Context context, SharedPreferences sharedPreferences) {
        HashMap<Integer, Long> result = new HashMap<>();
        long maxAgeListing = Long.valueOf(getString(R.string.pref_cache_maxage_listing_key, "168", context, sharedPreferences)).longValue() * DateUtils.MILLIS_PER_HOUR;
        long maxAgeThumb = Long.valueOf(getString(R.string.pref_cache_maxage_thumb_key, "168", context, sharedPreferences)).longValue() * DateUtils.MILLIS_PER_HOUR;
        long maxAgeImage = Long.valueOf(getString(R.string.pref_cache_maxage_image_key, "72", context, sharedPreferences)).longValue() * DateUtils.MILLIS_PER_HOUR;
        result.put(Integer.valueOf(110), Long.valueOf(maxAgeListing));
        result.put(Integer.valueOf(120), Long.valueOf(maxAgeListing));
        result.put(Integer.valueOf(100), Long.valueOf(maxAgeListing));
        result.put(Integer.valueOf(101), Long.valueOf(maxAgeListing));
        result.put(Integer.valueOf(130), Long.valueOf(maxAgeListing));
        result.put(Integer.valueOf(FileType.INBOX_LIST), Long.valueOf(maxAgeListing));
        result.put(Integer.valueOf(200), Long.valueOf(maxAgeThumb));
        result.put(Integer.valueOf(FileType.IMAGE), Long.valueOf(maxAgeImage));
        result.put(Integer.valueOf(FileType.IMAGE_INFO), Long.valueOf(maxAgeImage));
        result.put(Integer.valueOf(FileType.CAPTCHA), Long.valueOf(maxAgeImage));
        return result;
    }

    public static Long pref_cache_maxage_entry(Context context, SharedPreferences sharedPreferences) {
        return Long.valueOf(Long.valueOf(getString(R.string.pref_cache_maxage_entry_key, "168", context, sharedPreferences)).longValue() * DateUtils.MILLIS_PER_HOUR);
    }

    public static CachePrecacheImages cache_precache_images(Context context, SharedPreferences sharedPreferences) {
        if (network_tor(context, sharedPreferences)) {
            return CachePrecacheImages.NEVER;
        }
        if (!getBoolean(R.string.pref_cache_precache_images_key, true, context, sharedPreferences)) {
            return CachePrecacheImages.NEVER;
        }
        if (getBoolean(R.string.pref_cache_precache_images_wifionly_key, true, context, sharedPreferences)) {
            return CachePrecacheImages.WIFIONLY;
        }
        return CachePrecacheImages.ALWAYS;
    }

    public static CachePrecacheComments cache_precache_comments(Context context, SharedPreferences sharedPreferences) {
        if (!getBoolean(R.string.pref_cache_precache_comments_key, true, context, sharedPreferences)) {
            return CachePrecacheComments.NEVER;
        }
        if (getBoolean(R.string.pref_cache_precache_comments_wifionly_key, false, context, sharedPreferences)) {
            return CachePrecacheComments.WIFIONLY;
        }
        return CachePrecacheComments.ALWAYS;
    }

    public static boolean network_tor(Context context, SharedPreferences sharedPreferences) {
        return getBoolean(R.string.pref_network_tor_key, false, context, sharedPreferences);
    }

    public static EnumSet<Action> pref_menus_post_context_items(Context context, SharedPreferences sharedPreferences) {
        Set<String> strings = getStringSet(R.string.pref_menus_post_context_items_key, R.array.pref_menus_post_context_items_return, context, sharedPreferences);
        EnumSet<Action> result = EnumSet.noneOf(Action.class);
        for (String s : strings) {
            result.add(Action.valueOf(General.asciiUppercase(s)));
        }
        return result;
    }

    public static EnumSet<Action> pref_menus_post_toolbar_items(Context context, SharedPreferences sharedPreferences) {
        Set<String> strings = getStringSet(R.string.pref_menus_post_toolbar_items_key, R.array.pref_menus_post_toolbar_items_return, context, sharedPreferences);
        EnumSet<Action> result = EnumSet.noneOf(Action.class);
        for (String s : strings) {
            result.add(Action.valueOf(General.asciiUppercase(s)));
        }
        return result;
    }

    public static EnumSet<LinkAction> pref_menus_link_context_items(Context context, SharedPreferences sharedPreferences) {
        Set<String> strings = getStringSet(R.string.pref_menus_link_context_items_key, R.array.pref_menus_link_context_items_return, context, sharedPreferences);
        EnumSet<LinkAction> result = EnumSet.noneOf(LinkAction.class);
        for (String s : strings) {
            result.add(LinkAction.valueOf(General.asciiUppercase(s)));
        }
        return result;
    }

    public static EnumSet<SubredditAction> pref_menus_subreddit_context_items(Context context, SharedPreferences sharedPreferences) {
        Set<String> strings = getStringSet(R.string.pref_menus_subreddit_context_items_key, R.array.pref_menus_subreddit_context_items_return, context, sharedPreferences);
        EnumSet<SubredditAction> result = EnumSet.noneOf(SubredditAction.class);
        for (String s : strings) {
            result.add(SubredditAction.valueOf(General.asciiUppercase(s)));
        }
        return result;
    }

    public static EnumSet<MainMenuUserItems> pref_menus_mainmenu_useritems(Context context, SharedPreferences sharedPreferences) {
        Set<String> strings = getStringSet(R.string.pref_menus_mainmenu_useritems_key, R.array.pref_menus_mainmenu_useritems_items_default, context, sharedPreferences);
        EnumSet<MainMenuUserItems> result = EnumSet.noneOf(MainMenuUserItems.class);
        for (String s : strings) {
            result.add(MainMenuUserItems.valueOf(General.asciiUppercase(s)));
        }
        return result;
    }

    public static EnumSet<MainMenuShortcutItems> pref_menus_mainmenu_shortcutitems(Context context, SharedPreferences sharedPreferences) {
        Set<String> strings = getStringSet(R.string.pref_menus_mainmenu_shortcutitems_key, R.array.pref_menus_mainmenu_shortcutitems_items_default, context, sharedPreferences);
        EnumSet<MainMenuShortcutItems> result = EnumSet.noneOf(MainMenuShortcutItems.class);
        for (String s : strings) {
            result.add(MainMenuShortcutItems.valueOf(General.asciiUppercase(s)));
        }
        return result;
    }

    public static EnumSet<OptionsMenuItemsPref> pref_menus_optionsmenu_items(Context context, SharedPreferences sharedPreferences) {
        Set<String> strings = getStringSet(R.string.pref_menus_optionsmenu_items_key, R.array.pref_menus_optionsmenu_items_items_default, context, sharedPreferences);
        EnumSet<OptionsMenuItemsPref> result = EnumSet.noneOf(OptionsMenuItemsPref.class);
        for (String s : strings) {
            result.add(OptionsMenuItemsPref.valueOf(General.asciiUppercase(s)));
        }
        return result;
    }

    public static List<String> pref_pinned_subreddits(Context context, SharedPreferences sharedPreferences) {
        return WritableHashSet.escapedStringToList(getString(R.string.pref_pinned_subreddits_key, "", context, sharedPreferences));
    }

    public static void pref_pinned_subreddits_add(Context context, SharedPreferences sharedPreferences, String subreddit) throws InvalidSubredditNameException {
        pref_subreddits_add(context, sharedPreferences, subreddit, R.string.pref_pinned_subreddits_key);
    }

    public static void pref_pinned_subreddits_remove(Context context, SharedPreferences sharedPreferences, String subreddit) throws InvalidSubredditNameException {
        pref_subreddits_remove(context, sharedPreferences, subreddit, R.string.pref_pinned_subreddits_key);
    }

    public static boolean pref_pinned_subreddits_check(Context context, SharedPreferences sharedPreferences, String subreddit) throws InvalidSubredditNameException {
        for (String existingSr : pref_pinned_subreddits(context, sharedPreferences)) {
            if (General.asciiLowercase(subreddit).equals(General.asciiLowercase(existingSr))) {
                return true;
            }
        }
        return false;
    }

    public static List<String> pref_blocked_subreddits(Context context, SharedPreferences sharedPreferences) {
        return WritableHashSet.escapedStringToList(getString(R.string.pref_blocked_subreddits_key, "", context, sharedPreferences));
    }

    public static void pref_blocked_subreddits_add(Context context, SharedPreferences sharedPreferences, String subreddit) throws InvalidSubredditNameException {
        pref_subreddits_add(context, sharedPreferences, subreddit, R.string.pref_blocked_subreddits_key);
        General.quickToast(context, (int) R.string.block_done);
    }

    public static void pref_blocked_subreddits_remove(Context context, SharedPreferences sharedPreferences, String subreddit) throws InvalidSubredditNameException {
        pref_subreddits_remove(context, sharedPreferences, subreddit, R.string.pref_blocked_subreddits_key);
        General.quickToast(context, (int) R.string.unblock_done);
    }

    public static boolean pref_blocked_subreddits_check(Context context, SharedPreferences sharedPreferences, String subreddit) throws InvalidSubredditNameException {
        for (String existingSr : pref_blocked_subreddits(context, sharedPreferences)) {
            if (General.asciiLowercase(subreddit).equals(General.asciiLowercase(existingSr))) {
                return true;
            }
        }
        return false;
    }

    private static void pref_subreddits_add(Context context, SharedPreferences sharedPreferences, String subreddit, int prefId) throws InvalidSubredditNameException {
        String name = RedditSubreddit.getCanonicalName(subreddit);
        ArrayList<String> list = WritableHashSet.escapedStringToList(getString(prefId, "", context, sharedPreferences));
        list.add(name);
        sharedPreferences.edit().putString(context.getString(prefId), WritableHashSet.listToEscapedString(list)).apply();
    }

    private static void pref_subreddits_remove(Context context, SharedPreferences sharedPreferences, String subreddit, int prefId) throws InvalidSubredditNameException {
        String name = RedditSubreddit.getCanonicalName(subreddit);
        ArrayList<String> list = WritableHashSet.escapedStringToList(getString(prefId, "", context, sharedPreferences));
        list.add(name);
        ArrayList<String> result = new ArrayList<>(list.size());
        Iterator it = list.iterator();
        while (it.hasNext()) {
            String existingSr = (String) it.next();
            if (!General.asciiLowercase(name).equals(General.asciiLowercase(existingSr))) {
                result.add(existingSr);
            }
        }
        sharedPreferences.edit().putString(context.getString(prefId), WritableHashSet.listToEscapedString(result)).apply();
    }
}
