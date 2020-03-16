package com.google.android.exoplayer2.ui;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;
import com.google.android.exoplayer2.offline.DownloadManager.TaskState;

public final class DownloadNotificationUtil {
    @StringRes
    private static final int NULL_STRING_ID = 0;

    private DownloadNotificationUtil() {
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x0064  */
    public static Notification buildProgressNotification(Context context, @DrawableRes int smallIcon, String channelId, @Nullable PendingIntent contentIntent, @Nullable String message, TaskState[] taskStates) {
        int titleStringId;
        int i;
        TaskState[] taskStateArr = taskStates;
        boolean haveDownloadTasks = false;
        boolean haveRemoveTasks = false;
        int length = taskStateArr.length;
        boolean haveDownloadedBytes = false;
        boolean allDownloadPercentagesUnknown = true;
        int downloadTaskCount = 0;
        float totalPercentage = 0.0f;
        int i2 = 0;
        while (true) {
            boolean z = true;
            if (i2 >= length) {
                break;
            }
            TaskState taskState = taskStateArr[i2];
            if (taskState.state == 1 || taskState.state == 2) {
                if (taskState.action.isRemoveAction) {
                    haveRemoveTasks = true;
                } else {
                    haveDownloadTasks = true;
                    if (taskState.downloadPercentage != -1.0f) {
                        allDownloadPercentagesUnknown = false;
                        totalPercentage += taskState.downloadPercentage;
                    }
                    if (taskState.downloadedBytes <= 0) {
                        z = false;
                    }
                    haveDownloadedBytes |= z;
                    downloadTaskCount++;
                }
            }
            i2++;
        }
        if (haveDownloadTasks) {
            i = R.string.exo_download_downloading;
        } else if (haveRemoveTasks) {
            i = R.string.exo_download_removing;
        } else {
            titleStringId = 0;
            Builder notificationBuilder = newNotificationBuilder(context, smallIcon, channelId, contentIntent, message, titleStringId);
            int progress = 0;
            boolean indeterminate = true;
            if (haveDownloadTasks) {
                progress = (int) (totalPercentage / ((float) downloadTaskCount));
                indeterminate = allDownloadPercentagesUnknown && haveDownloadedBytes;
            }
            notificationBuilder.setProgress(100, progress, indeterminate);
            notificationBuilder.setOngoing(true);
            notificationBuilder.setShowWhen(false);
            return notificationBuilder.build();
        }
        titleStringId = i;
        Builder notificationBuilder2 = newNotificationBuilder(context, smallIcon, channelId, contentIntent, message, titleStringId);
        int progress2 = 0;
        boolean indeterminate2 = true;
        if (haveDownloadTasks) {
        }
        notificationBuilder2.setProgress(100, progress2, indeterminate2);
        notificationBuilder2.setOngoing(true);
        notificationBuilder2.setShowWhen(false);
        return notificationBuilder2.build();
    }

    public static Notification buildDownloadCompletedNotification(Context context, @DrawableRes int smallIcon, String channelId, @Nullable PendingIntent contentIntent, @Nullable String message) {
        return newNotificationBuilder(context, smallIcon, channelId, contentIntent, message, R.string.exo_download_completed).build();
    }

    public static Notification buildDownloadFailedNotification(Context context, @DrawableRes int smallIcon, String channelId, @Nullable PendingIntent contentIntent, @Nullable String message) {
        return newNotificationBuilder(context, smallIcon, channelId, contentIntent, message, R.string.exo_download_failed).build();
    }

    private static Builder newNotificationBuilder(Context context, @DrawableRes int smallIcon, String channelId, @Nullable PendingIntent contentIntent, @Nullable String message, @StringRes int titleStringId) {
        Builder notificationBuilder = new Builder(context, channelId).setSmallIcon(smallIcon);
        if (titleStringId != 0) {
            notificationBuilder.setContentTitle(context.getResources().getString(titleStringId));
        }
        if (contentIntent != null) {
            notificationBuilder.setContentIntent(contentIntent);
        }
        if (message != null) {
            notificationBuilder.setStyle(new BigTextStyle().bigText(message));
        }
        return notificationBuilder;
    }
}
