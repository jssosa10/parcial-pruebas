package org.quantumbadger.redreader.receivers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import com.google.android.exoplayer2.upstream.DataSchemeDataSource;
import java.util.UUID;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.activities.InboxListingActivity;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways;
import org.quantumbadger.redreader.common.Constants.FileType;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedArray;
import org.quantumbadger.redreader.jsonwrap.JsonValue;
import org.quantumbadger.redreader.reddit.things.RedditComment;
import org.quantumbadger.redreader.reddit.things.RedditMessage;
import org.quantumbadger.redreader.reddit.things.RedditThing;
import org.quantumbadger.redreader.reddit.things.RedditThing.Kind;

public class NewMessageChecker extends BroadcastReceiver {
    private static final String PREFS_SAVED_MESSAGE_ID = "LastMessageId";
    private static final String PREFS_SAVED_MESSAGE_TIMESTAMP = "LastMessageTimestamp";
    private static final String TAG = "NewMessageChecker";

    /* renamed from: org.quantumbadger.redreader.receivers.NewMessageChecker$2 reason: invalid class name */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$org$quantumbadger$redreader$reddit$things$RedditThing$Kind = new int[Kind.values().length];

        static {
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$things$RedditThing$Kind[Kind.COMMENT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$things$RedditThing$Kind[Kind.MESSAGE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    public void onReceive(Context context, Intent intent) {
        checkForNewMessages(context);
    }

    public static void checkForNewMessages(Context context) {
        Log.i("RedReader", "Checking for new messages.");
        if (PrefsUtility.pref_behaviour_notifications(context, PreferenceManager.getDefaultSharedPreferences(context))) {
            RedditAccount user = RedditAccountManager.getInstance(context).getDefaultAccount();
            if (!user.isAnonymous()) {
                CacheManager cm = CacheManager.getInstance(context);
                AnonymousClass1 r1 = new CacheRequest(Reddit.getUri("/message/unread.json?limit=2"), user, null, -500, 0, DownloadStrategyAlways.INSTANCE, FileType.INBOX_LIST, 0, true, true, context) {
                    /* access modifiers changed from: protected */
                    public void onDownloadNecessary() {
                    }

                    /* access modifiers changed from: protected */
                    public void onDownloadStarted() {
                    }

                    /* access modifiers changed from: protected */
                    public void onCallbackException(Throwable t) {
                        BugReportActivity.handleGlobalError(this.context, t);
                    }

                    /* access modifiers changed from: protected */
                    public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                        Log.e(NewMessageChecker.TAG, "Request failed", t);
                    }

                    /* access modifiers changed from: protected */
                    public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
                    }

                    /* access modifiers changed from: protected */
                    public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
                    }

                    public void onJsonParseStarted(JsonValue value, long timestamp, UUID session, boolean fromCache) {
                        long messageTimestamp;
                        String messageID;
                        String title;
                        String title2;
                        try {
                            JsonBufferedArray children = value.asObject().getObject(DataSchemeDataSource.SCHEME_DATA).getArray("children");
                            children.join();
                            int messageCount = children.getCurrentItemCount();
                            String str = NewMessageChecker.TAG;
                            StringBuilder sb = new StringBuilder();
                            sb.append("Got response. Message count = ");
                            sb.append(messageCount);
                            Log.e(str, sb.toString());
                            if (messageCount >= 1) {
                                RedditThing thing = (RedditThing) children.get(0).asObject(RedditThing.class);
                                String text = this.context.getString(R.string.notification_message_action);
                                switch (AnonymousClass2.$SwitchMap$org$quantumbadger$redreader$reddit$things$RedditThing$Kind[thing.getKind().ordinal()]) {
                                    case 1:
                                        RedditComment comment = thing.asComment();
                                        title = this.context.getString(R.string.notification_comment, new Object[]{comment.author});
                                        messageID = comment.name;
                                        messageTimestamp = comment.created_utc;
                                        break;
                                    case 2:
                                        RedditMessage message = thing.asMessage();
                                        title = this.context.getString(R.string.notification_message, new Object[]{message.author});
                                        messageID = message.name;
                                        messageTimestamp = message.created_utc;
                                        break;
                                    default:
                                        throw new RuntimeException("Unknown item in list.");
                                }
                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.context);
                                String oldMessageId = prefs.getString(NewMessageChecker.PREFS_SAVED_MESSAGE_ID, "");
                                String title3 = title;
                                long oldMessageTimestamp = prefs.getLong(NewMessageChecker.PREFS_SAVED_MESSAGE_TIMESTAMP, 0);
                                if (oldMessageId != null) {
                                    if (messageID.equals(oldMessageId) || oldMessageTimestamp > messageTimestamp) {
                                        Log.e(NewMessageChecker.TAG, "All messages have been previously seen.");
                                    }
                                }
                                Log.e(NewMessageChecker.TAG, "New messages detected. Showing notification.");
                                prefs.edit().putString(NewMessageChecker.PREFS_SAVED_MESSAGE_ID, messageID).putLong(NewMessageChecker.PREFS_SAVED_MESSAGE_TIMESTAMP, messageTimestamp).apply();
                                if (messageCount > 1) {
                                    title2 = this.context.getString(R.string.notification_message_multiple);
                                } else {
                                    title2 = title3;
                                }
                                NewMessageChecker.createNotification(title2, text, this.context);
                            }
                        } catch (Throwable t) {
                            notifyFailure(6, t, null, "Parse failure");
                        }
                    }
                };
                cm.makeRequest(r1);
            }
        }
    }

    /* access modifiers changed from: private */
    public static void createNotification(String title, String text, Context context) {
        Builder notification = new Builder(context).setSmallIcon(R.drawable.icon_notif).setContentTitle(title).setContentText(text).setAutoCancel(true).setChannelId("RRNewMessageChecker");
        notification.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, InboxListingActivity.class), 0));
        ((NotificationManager) context.getSystemService("notification")).notify(0, notification.getNotification());
    }
}
