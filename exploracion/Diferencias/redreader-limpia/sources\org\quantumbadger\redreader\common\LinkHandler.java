package org.quantumbadger.redreader.common;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.ClipboardManager;
import android.util.Log;
import android.util.TypedValue;
import com.google.android.exoplayer2.C;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.quantumbadger.redreader.BuildConfig;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.activities.AlbumListingActivity;
import org.quantumbadger.redreader.activities.BaseActivity;
import org.quantumbadger.redreader.activities.CommentListingActivity;
import org.quantumbadger.redreader.activities.ImageViewActivity;
import org.quantumbadger.redreader.activities.PostListingActivity;
import org.quantumbadger.redreader.activities.WebViewActivity;
import org.quantumbadger.redreader.fragments.UserProfileDialog;
import org.quantumbadger.redreader.image.DeviantArtAPI;
import org.quantumbadger.redreader.image.GetAlbumInfoListener;
import org.quantumbadger.redreader.image.GetImageInfoListener;
import org.quantumbadger.redreader.image.GfycatAPI;
import org.quantumbadger.redreader.image.ImageInfo;
import org.quantumbadger.redreader.image.ImageInfo.HasAudio;
import org.quantumbadger.redreader.image.ImageInfo.MediaType;
import org.quantumbadger.redreader.image.ImgurAPI;
import org.quantumbadger.redreader.image.ImgurAPI.AlbumInfo;
import org.quantumbadger.redreader.image.ImgurAPIV3;
import org.quantumbadger.redreader.image.RedditVideosAPI;
import org.quantumbadger.redreader.image.SaveImageCallback;
import org.quantumbadger.redreader.image.ShareImageCallback;
import org.quantumbadger.redreader.image.StreamableAPI;
import org.quantumbadger.redreader.reddit.things.RedditPost;
import org.quantumbadger.redreader.reddit.url.RedditURLParser;
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL;

public class LinkHandler {
    public static final Pattern deviantartPattern = Pattern.compile("https://www\\.deviantart\\.com/([\\w\\-]+)/art/([\\w\\-]+)");
    public static final Pattern gfycatPattern = Pattern.compile(".*[^A-Za-z]gfycat\\.com/(?:gifs/detail/)?(\\w+).*");
    public static final Pattern googlePlayPattern = Pattern.compile("^https?://[\\.\\w]*play\\.google\\.\\w+/.*");
    public static final Pattern imgflipPattern = Pattern.compile(".*[^A-Za-z]imgflip\\.com/i/(\\w+).*");
    public static final Pattern imgurAlbumPattern = Pattern.compile(".*[^A-Za-z]imgur\\.com/(a|gallery)/(\\w+).*");
    public static final Pattern imgurPattern = Pattern.compile(".*[^A-Za-z]imgur\\.com/(\\w+).*");
    public static final Pattern lvmePattern = Pattern.compile(".*[^A-Za-z]livememe\\.com/(\\w+).*");
    public static final Pattern makeamemePattern = Pattern.compile(".*[^A-Za-z]makeameme\\.org/meme/([\\w\\-]+).*");
    public static final Pattern qkmePattern1 = Pattern.compile(".*[^A-Za-z]qkme\\.me/(\\w+).*");
    public static final Pattern qkmePattern2 = Pattern.compile(".*[^A-Za-z]quickmeme\\.com/meme/(\\w+).*");
    public static final Pattern redditVideosPattern = Pattern.compile(".*[^A-Za-z]v.redd.it/(\\w+).*");
    public static final Pattern reddituploadsPattern = Pattern.compile(".*[^A-Za-z]i\\.reddituploads\\.com/(\\w+).*");
    public static final Pattern streamablePattern = Pattern.compile(".*[^A-Za-z]streamable\\.com/(\\w+).*");
    public static final Pattern vimeoPattern = Pattern.compile("^https?://[\\.\\w]*vimeo\\.\\w+/.*");
    public static final Pattern youtuDotBePattern = Pattern.compile("^https?://[\\.\\w]*youtu\\.be/([A-Za-z0-9\\-_]+)(\\?.*|).*");
    public static final Pattern youtubeDotComPattern = Pattern.compile("^https?://[\\.\\w]*youtube\\.\\w+/.*");

    private static abstract class AlbumInfoRetryListener implements GetAlbumInfoListener {
        private final GetAlbumInfoListener mListener;

        public abstract void onFailure(int i, Throwable th, Integer num, String str);

        private AlbumInfoRetryListener(GetAlbumInfoListener listener) {
            this.mListener = listener;
        }

        public void onSuccess(AlbumInfo info2) {
            this.mListener.onSuccess(info2);
        }
    }

    private static abstract class ImageInfoRetryListener implements GetImageInfoListener {
        private final GetImageInfoListener mListener;

        public abstract void onFailure(int i, Throwable th, Integer num, String str);

        private ImageInfoRetryListener(GetImageInfoListener listener) {
            this.mListener = listener;
        }

        public void onSuccess(ImageInfo info2) {
            this.mListener.onSuccess(info2);
        }

        public void onNotAnImage() {
            this.mListener.onNotAnImage();
        }
    }

    public enum LinkAction {
        SHARE(R.string.action_share),
        COPY_URL(R.string.action_copy_link),
        SHARE_IMAGE(R.string.action_share_image),
        SAVE_IMAGE(R.string.action_save),
        EXTERNAL(R.string.action_external);
        
        public final int descriptionResId;

        private LinkAction(int descriptionResId2) {
            this.descriptionResId = descriptionResId2;
        }
    }

    private static class LinkMenuItem {
        public final LinkAction action;
        public final String title;

        private LinkMenuItem(Context context, int titleRes, LinkAction action2) {
            this.title = context.getString(titleRes);
            this.action = action2;
        }
    }

    public static void onLinkClicked(AppCompatActivity activity, String url) {
        onLinkClicked(activity, url, false);
    }

    public static void onLinkClicked(AppCompatActivity activity, String url, boolean forceNoImage) {
        onLinkClicked(activity, url, forceNoImage, null);
    }

    public static void onLinkClicked(AppCompatActivity activity, String url, boolean forceNoImage, RedditPost post) {
        onLinkClicked(activity, url, forceNoImage, post, null, 0);
    }

    public static void onLinkClicked(AppCompatActivity activity, String url, boolean forceNoImage, RedditPost post, AlbumInfo albumInfo, int albumImageIndex) {
        onLinkClicked(activity, url, forceNoImage, post, albumInfo, albumImageIndex, false);
    }

    public static void onLinkClicked(AppCompatActivity activity, String url, boolean forceNoImage, RedditPost post, AlbumInfo albumInfo, int albumImageIndex, boolean fromExternalIntent) {
        String str;
        final AppCompatActivity appCompatActivity = activity;
        String url2 = url;
        RedditPost redditPost = post;
        AlbumInfo albumInfo2 = albumInfo;
        boolean z = fromExternalIntent;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        if (url2.startsWith("rr://")) {
            final Uri rrUri = Uri.parse(url);
            if (rrUri.getAuthority().equals(NotificationCompat.CATEGORY_MESSAGE)) {
                new Handler().post(new Runnable() {
                    public void run() {
                        Builder builder = new Builder(appCompatActivity);
                        builder.setTitle(rrUri.getQueryParameter("title"));
                        builder.setMessage(rrUri.getQueryParameter("message"));
                        builder.create().show();
                    }
                });
                return;
            }
        }
        if (url2.startsWith("r/") || url2.startsWith("u/")) {
            StringBuilder sb = new StringBuilder();
            sb.append("/");
            sb.append(url2);
            url2 = sb.toString();
        }
        if (url2.startsWith("/")) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("https://reddit.com");
            sb2.append(url2);
            url2 = sb2.toString();
        }
        if (!url2.contains("://")) {
            StringBuilder sb3 = new StringBuilder();
            sb3.append("http://");
            sb3.append(url2);
            url2 = sb3.toString();
        }
        if (forceNoImage || !isProbablyAnImage(url2)) {
            int i = albumImageIndex;
            if (!forceNoImage && imgurAlbumPattern.matcher(url2).matches()) {
                switch (PrefsUtility.pref_behaviour_albumview_mode(appCompatActivity, sharedPreferences)) {
                    case INTERNAL_LIST:
                        Intent intent = new Intent(appCompatActivity, AlbumListingActivity.class);
                        intent.setData(Uri.parse(url2));
                        intent.putExtra("post", redditPost);
                        appCompatActivity.startActivity(intent);
                        return;
                    case INTERNAL_BROWSER:
                        Intent intent2 = new Intent();
                        if (!PrefsUtility.pref_behaviour_usecustomtabs(appCompatActivity, sharedPreferences) || VERSION.SDK_INT < 18) {
                            intent2.setClass(appCompatActivity, WebViewActivity.class);
                            intent2.putExtra("url", url2);
                            intent2.putExtra("post", redditPost);
                        } else {
                            intent2.setAction("android.intent.action.VIEW");
                            intent2.setData(Uri.parse(url2));
                            intent2.addFlags(C.ENCODING_PCM_MU_LAW);
                            Bundle bundle = new Bundle();
                            bundle.putBinder("android.support.customtabs.extra.SESSION", null);
                            intent2.putExtras(bundle);
                            intent2.putExtra("android.support.customtabs.extra.SHARE_MENU_ITEM", true);
                            TypedValue typedValue = new TypedValue();
                            activity.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
                            intent2.putExtra("android.support.customtabs.extra.TOOLBAR_COLOR", typedValue.data);
                            intent2.putExtra("android.support.customtabs.extra.ENABLE_URLBAR_HIDING", true);
                        }
                        appCompatActivity.startActivity(intent2);
                        return;
                    case EXTERNAL_BROWSER:
                        openWebBrowser(appCompatActivity, Uri.parse(url2), z);
                        return;
                }
            }
            RedditURL redditURL = RedditURLParser.parse(Uri.parse(url2));
            if (redditURL != null) {
                switch (redditURL.pathType()) {
                    case 0:
                    case 1:
                    case 3:
                    case 8:
                        Intent intent3 = new Intent(appCompatActivity, PostListingActivity.class);
                        intent3.setData(redditURL.generateJsonUri());
                        appCompatActivity.startActivityForResult(intent3, 1);
                        return;
                    case 4:
                        UserProfileDialog.newInstance(redditURL.asUserProfileURL().username).show(activity.getSupportFragmentManager(), (String) null);
                        return;
                    case 5:
                    case 7:
                        Intent intent4 = new Intent(appCompatActivity, CommentListingActivity.class);
                        intent4.setData(redditURL.generateJsonUri());
                        appCompatActivity.startActivityForResult(intent4, 1);
                        return;
                }
            }
            if (!PrefsUtility.pref_behaviour_useinternalbrowser(appCompatActivity, sharedPreferences) && openWebBrowser(appCompatActivity, Uri.parse(url2), z)) {
                return;
            }
            if ((!youtubeDotComPattern.matcher(url2).matches() && !vimeoPattern.matcher(url2).matches() && !googlePlayPattern.matcher(url2).matches()) || !openWebBrowser(appCompatActivity, Uri.parse(url2), z)) {
                Matcher youtuDotBeMatcher = youtuDotBePattern.matcher(url2);
                if (youtuDotBeMatcher.find() && youtuDotBeMatcher.group(1) != null) {
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append("http://youtube.com/watch?v=");
                    sb4.append(youtuDotBeMatcher.group(1));
                    if (youtuDotBeMatcher.group(2).length() > 0) {
                        StringBuilder sb5 = new StringBuilder();
                        sb5.append("&");
                        sb5.append(youtuDotBeMatcher.group(2).substring(1));
                        str = sb5.toString();
                    } else {
                        str = "";
                    }
                    sb4.append(str);
                    if (openWebBrowser(appCompatActivity, Uri.parse(sb4.toString()), z)) {
                        return;
                    }
                }
                Intent intent5 = new Intent();
                if (!PrefsUtility.pref_behaviour_usecustomtabs(appCompatActivity, sharedPreferences) || VERSION.SDK_INT < 18) {
                    intent5.setClass(appCompatActivity, WebViewActivity.class);
                    intent5.putExtra("url", url2);
                    intent5.putExtra("post", redditPost);
                } else {
                    intent5.setAction("android.intent.action.VIEW");
                    intent5.setData(Uri.parse(url2));
                    intent5.addFlags(C.ENCODING_PCM_MU_LAW);
                    Bundle bundle2 = new Bundle();
                    bundle2.putBinder("android.support.customtabs.extra.SESSION", null);
                    intent5.putExtras(bundle2);
                    intent5.putExtra("android.support.customtabs.extra.SHARE_MENU_ITEM", true);
                    TypedValue typedValue2 = new TypedValue();
                    activity.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue2, true);
                    intent5.putExtra("android.support.customtabs.extra.TOOLBAR_COLOR", typedValue2.data);
                    intent5.putExtra("android.support.customtabs.extra.ENABLE_URLBAR_HIDING", true);
                }
                appCompatActivity.startActivity(intent5);
                return;
            }
            return;
        }
        Intent intent6 = new Intent(appCompatActivity, ImageViewActivity.class);
        intent6.setData(Uri.parse(url2));
        intent6.putExtra("post", redditPost);
        if (albumInfo2 != null) {
            intent6.putExtra("album", albumInfo2.id);
            intent6.putExtra("albumImageIndex", albumImageIndex);
        } else {
            int i2 = albumImageIndex;
        }
        appCompatActivity.startActivity(intent6);
    }

    public static void onLinkLongClicked(AppCompatActivity activity, String uri) {
        onLinkLongClicked(activity, uri, false);
    }

    public static void onLinkLongClicked(final AppCompatActivity activity, final String uri, boolean forceNoImage) {
        if (uri != null) {
            EnumSet<LinkAction> itemPref = PrefsUtility.pref_menus_link_context_items(activity, PreferenceManager.getDefaultSharedPreferences(activity));
            if (!itemPref.isEmpty()) {
                final ArrayList<LinkMenuItem> menu = new ArrayList<>();
                if (itemPref.contains(LinkAction.COPY_URL)) {
                    menu.add(new LinkMenuItem(activity, R.string.action_copy_link, LinkAction.COPY_URL));
                }
                if (itemPref.contains(LinkAction.EXTERNAL)) {
                    menu.add(new LinkMenuItem(activity, R.string.action_external, LinkAction.EXTERNAL));
                }
                if (itemPref.contains(LinkAction.SAVE_IMAGE) && isProbablyAnImage(uri) && !forceNoImage) {
                    menu.add(new LinkMenuItem(activity, R.string.action_save_image, LinkAction.SAVE_IMAGE));
                }
                if (itemPref.contains(LinkAction.SHARE)) {
                    menu.add(new LinkMenuItem(activity, R.string.action_share, LinkAction.SHARE));
                }
                if (itemPref.contains(LinkAction.SHARE_IMAGE) && isProbablyAnImage(uri) && !forceNoImage) {
                    menu.add(new LinkMenuItem(activity, R.string.action_share_image, LinkAction.SHARE_IMAGE));
                }
                String[] menuText = new String[menu.size()];
                for (int i = 0; i < menuText.length; i++) {
                    menuText[i] = ((LinkMenuItem) menu.get(i)).title;
                }
                Builder builder = new Builder(activity);
                builder.setItems(menuText, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        LinkHandler.onActionMenuItemSelected(uri, activity, ((LinkMenuItem) menu.get(which)).action);
                    }
                });
                AlertDialog alert = builder.create();
                alert.setCanceledOnTouchOutside(true);
                alert.show();
            }
        }
    }

    public static void onActionMenuItemSelected(String uri, AppCompatActivity activity, LinkAction action) {
        switch (action) {
            case SHARE:
                Intent mailer = new Intent("android.intent.action.SEND");
                mailer.setType("text/plain");
                mailer.putExtra("android.intent.extra.TEXT", uri);
                activity.startActivity(Intent.createChooser(mailer, activity.getString(R.string.action_share)));
                return;
            case COPY_URL:
                ((ClipboardManager) activity.getSystemService("clipboard")).setText(uri);
                return;
            case EXTERNAL:
                try {
                    Intent intent = new Intent("android.intent.action.VIEW");
                    intent.setData(Uri.parse(uri));
                    activity.startActivity(intent);
                    return;
                } catch (ActivityNotFoundException e) {
                    General.quickToast((Context) activity, (int) R.string.error_no_suitable_apps_available);
                    return;
                }
            case SHARE_IMAGE:
                ((BaseActivity) activity).requestPermissionWithCallback("android.permission.WRITE_EXTERNAL_STORAGE", new ShareImageCallback(activity, uri));
                return;
            case SAVE_IMAGE:
                ((BaseActivity) activity).requestPermissionWithCallback("android.permission.WRITE_EXTERNAL_STORAGE", new SaveImageCallback(activity, uri));
                return;
            default:
                return;
        }
    }

    public static boolean openWebBrowser(AppCompatActivity activity, Uri uri, boolean fromExternalIntent) {
        if (!fromExternalIntent) {
            try {
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.setData(uri);
                activity.startActivity(intent);
                return true;
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder();
                sb.append("Failed to open url \"");
                sb.append(uri.toString());
                sb.append("\" in external browser");
                General.quickToast((Context) activity, sb.toString());
            }
        } else {
            Intent baseIntent = new Intent("android.intent.action.VIEW");
            baseIntent.setData(uri);
            ArrayList<Intent> targetIntents = new ArrayList<>();
            for (ResolveInfo info2 : activity.getPackageManager().queryIntentActivities(baseIntent, 0)) {
                String packageName = info2.activityInfo.packageName;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Considering ");
                sb2.append(packageName);
                Log.i("RRDEBUG", sb2.toString());
                if (packageName != null && !packageName.startsWith(BuildConfig.APPLICATION_ID)) {
                    Intent intent2 = new Intent("android.intent.action.VIEW");
                    intent2.setData(uri);
                    intent2.setPackage(packageName);
                    targetIntents.add(intent2);
                }
            }
            if (!targetIntents.isEmpty()) {
                Intent chooserIntent = Intent.createChooser((Intent) targetIntents.remove(0), activity.getString(R.string.open_with));
                if (!targetIntents.isEmpty()) {
                    chooserIntent.putExtra("android.intent.extra.INITIAL_INTENTS", (Parcelable[]) targetIntents.toArray(new Parcelable[0]));
                }
                activity.startActivity(chooserIntent);
                return true;
            }
            return false;
        }
    }

    public static boolean isProbablyAnImage(String url) {
        Matcher matchImgur = imgurPattern.matcher(url);
        boolean z = true;
        if (matchImgur.find()) {
            String imgId = matchImgur.group(1);
            if (imgId.length() > 2 && !imgId.startsWith("gallery")) {
                return true;
            }
        }
        Matcher matchGfycat = gfycatPattern.matcher(url);
        if (matchGfycat.find() && matchGfycat.group(1).length() > 5) {
            return true;
        }
        Matcher matchStreamable = streamablePattern.matcher(url);
        if (matchStreamable.find() && matchStreamable.group(1).length() > 2) {
            return true;
        }
        Matcher matchRedditUploads = reddituploadsPattern.matcher(url);
        if (matchRedditUploads.find() && matchRedditUploads.group(1).length() > 10) {
            return true;
        }
        Matcher matchImgflip = imgflipPattern.matcher(url);
        if (matchImgflip.find() && matchImgflip.group(1).length() > 3) {
            return true;
        }
        Matcher matchMakeameme = makeamemePattern.matcher(url);
        if (matchMakeameme.find() && matchMakeameme.group(1).length() > 3) {
            return true;
        }
        if (deviantartPattern.matcher(url).find() && url.length() > 40) {
            return true;
        }
        Matcher matchRedditVideos = redditVideosPattern.matcher(url);
        if (matchRedditVideos.find() && matchRedditVideos.group(1).length() > 3) {
            return true;
        }
        if (getImageUrlPatternMatch(url) == null) {
            z = false;
        }
        return z;
    }

    public static void getImgurImageInfo(Context context, String imgId, int priority, int listId, boolean returnUrlOnFailure, GetImageInfoListener listener) {
        StringBuilder sb = new StringBuilder();
        sb.append("Image ");
        sb.append(imgId);
        sb.append(": trying API v3 with auth");
        Log.i("getImgurImageInfo", sb.toString());
        final String str = imgId;
        final Context context2 = context;
        final int i = priority;
        final int i2 = listId;
        final GetImageInfoListener getImageInfoListener = listener;
        final boolean z = returnUrlOnFailure;
        AnonymousClass3 r2 = new ImageInfoRetryListener(listener) {
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                StringBuilder sb = new StringBuilder();
                sb.append("Image ");
                sb.append(str);
                sb.append(": trying API v3 without auth");
                Log.i("getImgurImageInfo", sb.toString());
                ImgurAPIV3.getImageInfo(context2, str, i, i2, false, new ImageInfoRetryListener(getImageInfoListener) {
                    public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Image ");
                        sb.append(str);
                        sb.append(": trying API v2");
                        Log.i("getImgurImageInfo", sb.toString());
                        ImgurAPI.getImageInfo(context2, str, i, i2, new ImageInfoRetryListener(getImageInfoListener) {
                            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                                Log.i("getImgurImageInfo", "All API requests failed!");
                                if (z) {
                                    GetImageInfoListener getImageInfoListener = getImageInfoListener;
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("https://i.imgur.com/");
                                    sb.append(str);
                                    sb.append(".jpg");
                                    getImageInfoListener.onSuccess(new ImageInfo(sb.toString(), null, HasAudio.MAYBE_AUDIO));
                                    return;
                                }
                                getImageInfoListener.onFailure(type, t, status, readableMessage);
                            }
                        });
                    }
                });
            }
        };
        ImgurAPIV3.getImageInfo(context, imgId, priority, listId, true, r2);
    }

    public static void getImgurAlbumInfo(Context context, String albumId, int priority, int listId, GetAlbumInfoListener listener) {
        StringBuilder sb = new StringBuilder();
        sb.append("Album ");
        sb.append(albumId);
        sb.append(": trying API v3 with auth");
        Log.i("getImgurAlbumInfo", sb.toString());
        final String str = albumId;
        final Context context2 = context;
        final int i = priority;
        final int i2 = listId;
        final GetAlbumInfoListener getAlbumInfoListener = listener;
        AnonymousClass4 r2 = new AlbumInfoRetryListener(listener) {
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                StringBuilder sb = new StringBuilder();
                sb.append("Album ");
                sb.append(str);
                sb.append(": trying API v3 without auth");
                Log.i("getImgurAlbumInfo", sb.toString());
                ImgurAPIV3.getAlbumInfo(context2, str, i, i2, false, new AlbumInfoRetryListener(getAlbumInfoListener) {
                    public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Album ");
                        sb.append(str);
                        sb.append(": trying API v2");
                        Log.i("getImgurAlbumInfo", sb.toString());
                        ImgurAPI.getAlbumInfo(context2, str, i, i2, new AlbumInfoRetryListener(getAlbumInfoListener) {
                            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                                Log.i("getImgurImageInfo", "All API requests failed!");
                                getAlbumInfoListener.onFailure(type, t, status, readableMessage);
                            }
                        });
                    }
                });
            }
        };
        ImgurAPIV3.getAlbumInfo(context, albumId, priority, listId, true, r2);
    }

    public static void getImageInfo(Context context, String url, int priority, int listId, GetImageInfoListener listener) {
        Matcher matchImgur = imgurPattern.matcher(url);
        if (matchImgur.find()) {
            String imgId = matchImgur.group(1);
            if (imgId.length() > 2 && !imgId.startsWith("gallery")) {
                getImgurImageInfo(context, imgId, priority, listId, true, listener);
                return;
            }
        }
        Matcher matchGfycat = gfycatPattern.matcher(url);
        if (matchGfycat.find()) {
            String imgId2 = matchGfycat.group(1);
            if (imgId2.length() > 5) {
                GfycatAPI.getImageInfo(context, imgId2, priority, listId, listener);
                return;
            }
        }
        Matcher matchStreamable = streamablePattern.matcher(url);
        if (matchStreamable.find()) {
            String imgId3 = matchStreamable.group(1);
            if (imgId3.length() > 2) {
                StreamableAPI.getImageInfo(context, imgId3, priority, listId, listener);
                return;
            }
        }
        if (deviantartPattern.matcher(url).find()) {
            String imgId4 = url;
            if (imgId4.length() > 40) {
                DeviantArtAPI.getImageInfo(context, imgId4, priority, listId, listener);
                return;
            }
        }
        Matcher matchRedditVideos = redditVideosPattern.matcher(url);
        if (matchRedditVideos.find()) {
            String imgId5 = matchRedditVideos.group(1);
            if (imgId5.length() > 3) {
                RedditVideosAPI.getImageInfo(context, imgId5, priority, listId, listener);
                return;
            }
        }
        ImageInfo imageUrlPatternMatch = getImageUrlPatternMatch(url);
        if (imageUrlPatternMatch != null) {
            listener.onSuccess(imageUrlPatternMatch);
        } else {
            listener.onNotAnImage();
        }
    }

    private static ImageInfo getImageUrlPatternMatch(String url) {
        String urlLower = General.asciiLowercase(url);
        Matcher matchRedditUploads = reddituploadsPattern.matcher(url);
        if (matchRedditUploads.find() && matchRedditUploads.group(1).length() > 10) {
            return new ImageInfo(url, MediaType.IMAGE, HasAudio.NO_AUDIO);
        }
        Matcher matchImgflip = imgflipPattern.matcher(url);
        if (matchImgflip.find()) {
            String imgId = matchImgflip.group(1);
            if (imgId.length() > 3) {
                StringBuilder sb = new StringBuilder();
                sb.append("https://i.imgflip.com/");
                sb.append(imgId);
                sb.append(".jpg");
                return new ImageInfo(sb.toString(), MediaType.IMAGE, HasAudio.NO_AUDIO);
            }
        }
        Matcher matchMakeameme = makeamemePattern.matcher(url);
        if (matchMakeameme.find()) {
            String imgId2 = matchMakeameme.group(1);
            if (imgId2.length() > 3) {
                StringBuilder sb2 = new StringBuilder();
                sb2.append("https://media.makeameme.org/created/");
                sb2.append(imgId2);
                sb2.append(".jpg");
                return new ImageInfo(sb2.toString(), MediaType.IMAGE, HasAudio.NO_AUDIO);
            }
        }
        String[] imageExtensions = {".jpg", ".jpeg", ".png"};
        String[] videoExtensions = {".webm", ".mp4", ".h264", ".gifv", ".mkv", ".3gp"};
        for (String ext : imageExtensions) {
            if (urlLower.endsWith(ext)) {
                return new ImageInfo(url, MediaType.IMAGE, HasAudio.MAYBE_AUDIO);
            }
        }
        for (String ext2 : videoExtensions) {
            if (urlLower.endsWith(ext2)) {
                return new ImageInfo(url, MediaType.VIDEO, HasAudio.MAYBE_AUDIO);
            }
        }
        if (urlLower.endsWith(".gif")) {
            return new ImageInfo(url, MediaType.GIF, HasAudio.MAYBE_AUDIO);
        }
        if (url.contains("?")) {
            String urlBeforeQ = urlLower.split("\\?")[0];
            for (String ext3 : imageExtensions) {
                if (urlBeforeQ.endsWith(ext3)) {
                    return new ImageInfo(url, MediaType.IMAGE, HasAudio.MAYBE_AUDIO);
                }
            }
            for (String ext4 : videoExtensions) {
                if (urlBeforeQ.endsWith(ext4)) {
                    return new ImageInfo(url, MediaType.VIDEO, HasAudio.MAYBE_AUDIO);
                }
            }
            if (urlBeforeQ.endsWith(".gif")) {
                return new ImageInfo(url, MediaType.GIF, HasAudio.MAYBE_AUDIO);
            }
        }
        Matcher matchQkme1 = qkmePattern1.matcher(url);
        if (matchQkme1.find()) {
            String imgId3 = matchQkme1.group(1);
            if (imgId3.length() > 2) {
                return new ImageInfo(String.format(Locale.US, "http://i.qkme.me/%s.jpg", new Object[]{imgId3}), MediaType.IMAGE, HasAudio.NO_AUDIO);
            }
        }
        Matcher matchQkme2 = qkmePattern2.matcher(url);
        if (matchQkme2.find()) {
            String imgId4 = matchQkme2.group(1);
            if (imgId4.length() > 2) {
                return new ImageInfo(String.format(Locale.US, "http://i.qkme.me/%s.jpg", new Object[]{imgId4}), MediaType.IMAGE, HasAudio.NO_AUDIO);
            }
        }
        Matcher matchLvme = lvmePattern.matcher(url);
        if (matchLvme.find()) {
            String imgId5 = matchLvme.group(1);
            if (imgId5.length() > 2) {
                return new ImageInfo(String.format(Locale.US, "http://www.livememe.com/%s.jpg", new Object[]{imgId5}), MediaType.IMAGE, HasAudio.NO_AUDIO);
            }
        }
        return null;
    }

    public static LinkedHashSet<String> computeAllLinks(String text) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Matcher urlMatcher = Pattern.compile("\\b((((ht|f)tp(s?)\\:\\/\\/|~\\/|\\/)|www.)(\\w+:\\w+@)?(([-\\w]+\\.)+(com|org|net|gov|mil|biz|info|mobi|name|aero|jobs|museum|travel|[a-z]{2}))(:[\\d]{1,5})?(((\\/([-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?((\\?([-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)(&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*(#([-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?)\\b").matcher(text);
        while (urlMatcher.find()) {
            result.add(urlMatcher.group(1));
        }
        Matcher subredditMatcher = Pattern.compile("(?<!\\w)(/?[ru]/\\w+)\\b").matcher(text);
        while (subredditMatcher.find()) {
            result.add(subredditMatcher.group(1));
        }
        return result;
    }
}
