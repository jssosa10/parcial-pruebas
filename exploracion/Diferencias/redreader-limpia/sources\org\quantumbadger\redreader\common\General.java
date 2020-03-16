package org.quantumbadger.redreader.common;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowManager.BadTokenException;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceTwopane;
import org.quantumbadger.redreader.fragments.ErrorPropertiesDialog;
import org.quantumbadger.redreader.reddit.APIResponseHandler.APIFailureType;

public final class General {
    public static final Charset CHARSET_UTF8 = Charset.forName("UTF-8");
    public static final int COLOR_INVALID = -65281;
    public static final String LTR_OVERRIDE_MARK = "‭";
    private static long lastBackPress = -1;
    private static Typeface monoTypeface;
    private static final Pattern urlPattern = Pattern.compile("^(https?)://([^/]+)/+([^\\?#]+)((?:\\?[^#]+)?)((?:#.+)?)$");

    public static boolean onBackPressed() {
        if (lastBackPress >= SystemClock.uptimeMillis() - 300) {
            return false;
        }
        lastBackPress = SystemClock.uptimeMillis();
        return true;
    }

    public static Typeface getMonoTypeface(Context context) {
        if (monoTypeface == null) {
            monoTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/VeraMono.ttf");
        }
        return monoTypeface;
    }

    public static Message handlerMessage(int what, Object obj) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        return msg;
    }

    public static void moveFile(File src, File dst) throws IOException {
        if (!src.renameTo(dst)) {
            copyFile(src, dst);
            if (!src.delete()) {
                src.deleteOnExit();
            }
        }
    }

    public static void copyFile(File src, File dst) throws IOException {
        copyFile((InputStream) new FileInputStream(src), (OutputStream) new FileOutputStream(dst));
    }

    public static void copyFile(InputStream fis, File dst) throws IOException {
        copyFile(fis, (OutputStream) new FileOutputStream(dst));
    }

    public static void copyFile(InputStream fis, OutputStream fos) throws IOException {
        byte[] buf = new byte[32768];
        while (true) {
            int read = fis.read(buf);
            int bytesRead = read;
            if (read > 0) {
                fos.write(buf, 0, bytesRead);
            } else {
                fis.close();
                fos.close();
                return;
            }
        }
    }

    public static boolean isCacheDiskFull(Context context) {
        return getFreeSpaceAvailable(PrefsUtility.pref_cache_location(context, PreferenceManager.getDefaultSharedPreferences(context))) < 134217728;
    }

    public static long getFreeSpaceAvailable(String path) {
        long blockSize;
        long availableBlocks;
        StatFs stat = new StatFs(path);
        if (VERSION.SDK_INT >= 18) {
            availableBlocks = stat.getAvailableBlocksLong();
            blockSize = stat.getBlockSizeLong();
        } else {
            availableBlocks = (long) stat.getAvailableBlocks();
            blockSize = (long) stat.getBlockSize();
        }
        return availableBlocks * blockSize;
    }

    public static String addUnits(long input) {
        int i = 0;
        long result = input;
        while (i <= 3 && result >= PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID) {
            i++;
            result = input / ((long) Math.pow(1024.0d, (double) i));
        }
        switch (i) {
            case 1:
                StringBuilder sb = new StringBuilder();
                sb.append(result);
                sb.append(" KiB");
                return sb.toString();
            case 2:
                StringBuilder sb2 = new StringBuilder();
                sb2.append(result);
                sb2.append(" MiB");
                return sb2.toString();
            case 3:
                StringBuilder sb3 = new StringBuilder();
                sb3.append(result);
                sb3.append(" GiB");
                return sb3.toString();
            default:
                StringBuilder sb4 = new StringBuilder();
                sb4.append(result);
                sb4.append(" B");
                return sb4.toString();
        }
    }

    public static String bytesToMegabytes(long input) {
        long totalKilobytes = input / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
        return String.format(Locale.US, "%d.%02d MB", new Object[]{Long.valueOf(totalKilobytes / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID), Long.valueOf((totalKilobytes % PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID) / 10)});
    }

    public static int dpToPixels(Context context, float dp) {
        return Math.round(TypedValue.applyDimension(1, dp, context.getResources().getDisplayMetrics()));
    }

    public static int spToPixels(Context context, float sp) {
        return Math.round(TypedValue.applyDimension(2, sp, context.getResources().getDisplayMetrics()));
    }

    public static void quickToast(Context context, int textRes) {
        quickToast(context, context.getString(textRes));
    }

    public static void quickToast(final Context context, final String text) {
        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
            public void run() {
                Toast.makeText(context, text, 1).show();
            }
        });
    }

    public static void quickToast(final Context context, final String text, final int duration) {
        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
            public void run() {
                Toast.makeText(context, text, duration).show();
            }
        });
    }

    public static boolean isTablet(Context context, SharedPreferences sharedPreferences) {
        AppearanceTwopane pref = PrefsUtility.appearance_twopane(context, sharedPreferences);
        boolean z = true;
        switch (pref) {
            case AUTO:
                if ((context.getResources().getConfiguration().screenLayout & 15) != 4) {
                    z = false;
                }
                return z;
            case NEVER:
                return false;
            case FORCE:
                return true;
            default:
                StringBuilder sb = new StringBuilder();
                sb.append("Unknown AppearanceTwopane value ");
                sb.append(pref.name());
                BugReportActivity.handleGlobalError(context, sb.toString());
                return false;
        }
    }

    public static boolean isConnectionWifi(Context context) {
        NetworkInfo info2 = ((ConnectivityManager) context.getSystemService("connectivity")).getNetworkInfo(1);
        if (info2 == null || info2.getDetailedState() != DetailedState.CONNECTED) {
            return false;
        }
        return true;
    }

    public static boolean isNetworkConnected(Context context) {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) context.getSystemService("connectivity")).getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /* JADX WARNING: type inference failed for: r0v5 */
    /* JADX WARNING: type inference failed for: r1v4 */
    /* JADX WARNING: type inference failed for: r0v6 */
    /* JADX WARNING: type inference failed for: r1v5 */
    /* JADX WARNING: Multi-variable type inference failed */
    public static RRError getGeneralErrorForFailure(Context context, int type, Throwable t, Integer status, String url) {
        int message;
        int title;
        switch (type) {
            case 0:
                title = R.string.error_connection_title;
                message = R.string.error_connection_message;
                break;
            case 1:
                if (status == null) {
                    title = R.string.error_unknown_api_title;
                    message = R.string.error_unknown_api_message;
                    break;
                } else {
                    switch (status.intValue()) {
                        case 400:
                        case 401:
                        case 403:
                            URI uri = uriFromString(url);
                            if (!((uri == null || uri.getHost() == null || (!Reddit.DOMAIN_HTTPS_HUMAN.equalsIgnoreCase(uri.getHost()) && !uri.getHost().endsWith(".reddit.com"))) ? false : true)) {
                                title = R.string.error_403_title_nonreddit;
                                message = R.string.error_403_message_nonreddit;
                                break;
                            } else {
                                title = R.string.error_403_title;
                                message = R.string.error_403_message;
                                break;
                            }
                        case 404:
                            title = R.string.error_404_title;
                            message = R.string.error_404_message;
                            break;
                        case 502:
                        case 503:
                        case 504:
                            title = R.string.error_redditdown_title;
                            message = R.string.error_redditdown_message;
                            break;
                        default:
                            title = R.string.error_unknown_api_title;
                            message = R.string.error_unknown_api_message;
                            break;
                    }
                }
            case 2:
                title = R.string.error_unexpected_storage_title;
                message = R.string.error_unexpected_storage_message;
                break;
            case 3:
                title = R.string.error_unexpected_cache_title;
                message = R.string.error_unexpected_cache_message;
                break;
            case 4:
                title = R.string.error_cancelled_title;
                message = R.string.error_cancelled_message;
                break;
            case 5:
                title = R.string.error_malformed_url_title;
                message = R.string.error_malformed_url_message;
                break;
            case 6:
                title = R.string.error_parse_title;
                message = R.string.error_parse_message;
                break;
            case 7:
                title = R.string.error_disk_space_title;
                message = R.string.error_disk_space_message;
                break;
            case 8:
                title = R.string.error_403_title;
                message = R.string.error_403_message;
                break;
            case 9:
                title = R.string.error_parse_imgur_title;
                message = R.string.error_parse_imgur_message;
                break;
            case 10:
                title = R.string.error_upload_fail_imgur_title;
                message = R.string.error_upload_fail_imgur_message;
                break;
            case 11:
                title = R.string.error_cache_dir_does_not_exist_title;
                message = R.string.error_cache_dir_does_not_exist_message;
                break;
            default:
                title = R.string.error_unknown_title;
                message = R.string.error_unknown_message;
                break;
        }
        RRError rRError = new RRError(context.getString(title), context.getString(message), t, status, url);
        return rRError;
    }

    public static RRError getGeneralErrorForFailure(Context context, APIFailureType type) {
        int message;
        int title;
        switch (type) {
            case INVALID_USER:
                title = R.string.error_403_title;
                message = R.string.error_403_message;
                break;
            case BAD_CAPTCHA:
                title = R.string.error_bad_captcha_title;
                message = R.string.error_bad_captcha_message;
                break;
            case NOTALLOWED:
                title = R.string.error_403_title;
                message = R.string.error_403_message;
                break;
            case SUBREDDIT_REQUIRED:
                title = R.string.error_subreddit_required_title;
                message = R.string.error_subreddit_required_message;
                break;
            case URL_REQUIRED:
                title = R.string.error_url_required_title;
                message = R.string.error_url_required_message;
                break;
            case TOO_FAST:
                title = R.string.error_too_fast_title;
                message = R.string.error_too_fast_message;
                break;
            case TOO_LONG:
                title = R.string.error_too_long_title;
                message = R.string.error_too_long_message;
                break;
            case ALREADY_SUBMITTED:
                title = R.string.error_already_submitted_title;
                message = R.string.error_already_submitted_message;
                break;
            default:
                title = R.string.error_unknown_api_title;
                message = R.string.error_unknown_api_message;
                break;
        }
        return new RRError(context.getString(title), context.getString(message));
    }

    public static void showResultDialog(final AppCompatActivity context, final RRError error) {
        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
            public void run() {
                try {
                    Builder alertBuilder = new Builder(context);
                    alertBuilder.setNeutralButton(R.string.dialog_close, null);
                    alertBuilder.setNegativeButton(R.string.button_moredetail, new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ErrorPropertiesDialog.newInstance(error).show(context.getSupportFragmentManager(), "ErrorPropertiesDialog");
                        }
                    });
                    alertBuilder.setTitle(error.title);
                    alertBuilder.setMessage(error.message);
                    alertBuilder.create().show();
                } catch (BadTokenException e) {
                    Log.e("General", "Tried to show result dialog after activity closed", e);
                }
            }
        });
    }

    public static String filenameFromString(String url) {
        String filename = uriFromString(url).getPath().replace(File.separator, "");
        if (filename.substring(1).split("\\.", 2).length >= 2) {
            return filename;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(filename);
        sb.append(".jpg");
        return sb.toString();
    }

    public static URI uriFromString(String url) {
        String scheme;
        String authority;
        String str;
        String path;
        String query;
        String fragment;
        String str2 = url;
        try {
            return new URI(str2);
        } catch (Throwable th) {
            if (path != null) {
                if (path.contains(StringUtils.SPACE)) {
                    URI uri = new URI(scheme, authority, path.replace(StringUtils.SPACE, "%20"), query, fragment);
                    return uri;
                }
            }
            return null;
        }
    }

    public static String sha1(byte[] plaintext) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(plaintext, 0, plaintext.length);
            byte[] hash = digest.digest();
            StringBuilder result = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                result.append(String.format(Locale.US, "%02X", new Object[]{Byte.valueOf(b)}));
            }
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Set<String> getUriQueryParameterNames(Uri uri) {
        if (!uri.isOpaque()) {
            String query = uri.getEncodedQuery();
            if (query == null) {
                return Collections.emptySet();
            }
            Set<String> names = new LinkedHashSet<>();
            int pos = 0;
            while (pos < query.length()) {
                int next = query.indexOf(38, pos);
                int end = next == -1 ? query.length() : next;
                int separator = query.indexOf(61, pos);
                if (separator > end || separator == -1) {
                    separator = end;
                }
                names.add(Uri.decode(query.substring(pos, separator)));
                pos = end + 1;
            }
            return Collections.unmodifiableSet(names);
        }
        throw new UnsupportedOperationException("This isn't a hierarchical URI.");
    }

    public static int divideCeil(int num, int divisor) {
        return ((num + divisor) - 1) / divisor;
    }

    public static void checkThisIsUIThread() {
        if (!isThisUIThread()) {
            throw new RuntimeException("Called from invalid thread");
        }
    }

    public static boolean isThisUIThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    public static <E> ArrayList<E> listOfOne(E obj) {
        ArrayList<E> result = new ArrayList<>(1);
        result.add(obj);
        return result;
    }

    public static String asciiUppercase(String input) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] >= 'a' && chars[i] <= 'z') {
                chars[i] = (char) (chars[i] - 'a');
                chars[i] = (char) (chars[i] + 'A');
            }
        }
        return new String(chars);
    }

    @NonNull
    public static String asciiLowercase(@NonNull String input) {
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] >= 'A' && chars[i] <= 'Z') {
                chars[i] = (char) (chars[i] - 'A');
                chars[i] = (char) (chars[i] + 'a');
            }
        }
        return new String(chars);
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[65536];
        while (true) {
            int read = in.read(buffer);
            int bytesRead = read;
            if (read > 0) {
                out.write(buffer, 0, bytesRead);
            } else {
                return;
            }
        }
    }

    public static byte[] readWholeStream(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copyStream(in, baos);
        return baos.toByteArray();
    }

    public static String readWholeStreamAsUTF8(InputStream in) throws IOException {
        return new String(readWholeStream(in), CHARSET_UTF8);
    }

    public static void setAllMarginsDp(Context context, View view, int marginDp) {
        MarginLayoutParams layoutParams = (MarginLayoutParams) view.getLayoutParams();
        int marginPx = dpToPixels(context, (float) marginDp);
        layoutParams.leftMargin = marginPx;
        layoutParams.rightMargin = marginPx;
        layoutParams.topMargin = marginPx;
        layoutParams.bottomMargin = marginPx;
    }

    public static void setLayoutMatchParent(View view) {
        LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = -1;
        layoutParams.height = -1;
    }

    public static void recreateActivityNoAnimation(AppCompatActivity activity) {
        Intent intent = activity.getIntent();
        activity.overridePendingTransition(0, 0);
        intent.addFlags(65536);
        activity.finish();
        activity.overridePendingTransition(0, 0);
        activity.startActivity(intent);
    }

    public static long hoursToMs(long hours) {
        return hours * 60 * 60 * 1000;
    }

    public static void safeDismissDialog(Dialog dialog) {
        try {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            Log.e("safeDismissDialog", "Caught exception while dismissing dialog", e);
        }
    }

    public static void closeSafely(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            Log.e("closeSafely", "Failed to close resource", e);
        }
    }
}
