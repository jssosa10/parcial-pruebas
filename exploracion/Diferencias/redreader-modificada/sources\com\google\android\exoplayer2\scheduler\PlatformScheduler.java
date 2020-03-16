package com.google.android.exoplayer2.scheduler;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobInfo.Builder;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.support.annotation.RequiresPermission;
import com.google.android.exoplayer2.util.Util;

@TargetApi(21)
public final class PlatformScheduler implements Scheduler {
    private static final String KEY_REQUIREMENTS = "requirements";
    private static final String KEY_SERVICE_ACTION = "service_action";
    private static final String KEY_SERVICE_PACKAGE = "service_package";
    private static final String TAG = "PlatformScheduler";
    private final int jobId;
    private final JobScheduler jobScheduler;
    private final ComponentName jobServiceComponentName;

    public static final class PlatformSchedulerService extends JobService {
        public boolean onStartJob(JobParameters params) {
            PlatformScheduler.logd("PlatformSchedulerService started");
            PersistableBundle extras = params.getExtras();
            if (new Requirements(extras.getInt(PlatformScheduler.KEY_REQUIREMENTS)).checkRequirements(this)) {
                PlatformScheduler.logd("Requirements are met");
                String serviceAction = extras.getString(PlatformScheduler.KEY_SERVICE_ACTION);
                String servicePackage = extras.getString(PlatformScheduler.KEY_SERVICE_PACKAGE);
                Intent intent = new Intent(serviceAction).setPackage(servicePackage);
                StringBuilder sb = new StringBuilder();
                sb.append("Starting service action: ");
                sb.append(serviceAction);
                sb.append(" package: ");
                sb.append(servicePackage);
                PlatformScheduler.logd(sb.toString());
                Util.startForegroundService(this, intent);
            } else {
                PlatformScheduler.logd("Requirements are not met");
                jobFinished(params, true);
            }
            return false;
        }

        public boolean onStopJob(JobParameters params) {
            return false;
        }
    }

    @RequiresPermission("android.permission.RECEIVE_BOOT_COMPLETED")
    public PlatformScheduler(Context context, int jobId2) {
        this.jobId = jobId2;
        this.jobServiceComponentName = new ComponentName(context, PlatformSchedulerService.class);
        this.jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
    }

    public boolean schedule(Requirements requirements, String servicePackage, String serviceAction) {
        int result = this.jobScheduler.schedule(buildJobInfo(this.jobId, this.jobServiceComponentName, requirements, serviceAction, servicePackage));
        StringBuilder sb = new StringBuilder();
        sb.append("Scheduling job: ");
        sb.append(this.jobId);
        sb.append(" result: ");
        sb.append(result);
        logd(sb.toString());
        return result == 1;
    }

    public boolean cancel() {
        StringBuilder sb = new StringBuilder();
        sb.append("Canceling job: ");
        sb.append(this.jobId);
        logd(sb.toString());
        this.jobScheduler.cancel(this.jobId);
        return true;
    }

    private static JobInfo buildJobInfo(int jobId2, ComponentName jobServiceComponentName2, Requirements requirements, String serviceAction, String servicePackage) {
        int networkType;
        Builder builder = new Builder(jobId2, jobServiceComponentName2);
        switch (requirements.getRequiredNetworkType()) {
            case 0:
                networkType = 0;
                break;
            case 1:
                networkType = 1;
                break;
            case 2:
                networkType = 2;
                break;
            case 3:
                if (Util.SDK_INT >= 24) {
                    networkType = 3;
                    break;
                } else {
                    throw new UnsupportedOperationException();
                }
            case 4:
                if (Util.SDK_INT >= 26) {
                    networkType = 4;
                    break;
                } else {
                    throw new UnsupportedOperationException();
                }
            default:
                throw new UnsupportedOperationException();
        }
        builder.setRequiredNetworkType(networkType);
        builder.setRequiresDeviceIdle(requirements.isIdleRequired());
        builder.setRequiresCharging(requirements.isChargingRequired());
        builder.setPersisted(true);
        PersistableBundle extras = new PersistableBundle();
        extras.putString(KEY_SERVICE_ACTION, serviceAction);
        extras.putString(KEY_SERVICE_PACKAGE, servicePackage);
        extras.putInt(KEY_REQUIREMENTS, requirements.getRequirementsData());
        builder.setExtras(extras);
        return builder.build();
    }

    /* access modifiers changed from: private */
    public static void logd(String message) {
    }
}
