package com.google.android.exoplayer2.upstream;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.upstream.HttpDataSource.InvalidResponseCodeException;
import java.io.IOException;

public class DefaultLoadErrorHandlingPolicy implements LoadErrorHandlingPolicy {
    private static final int DEFAULT_BEHAVIOR_MIN_LOADABLE_RETRY_COUNT = -1;
    public static final int DEFAULT_MIN_LOADABLE_RETRY_COUNT = 3;
    public static final int DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE = 6;
    public static final long DEFAULT_TRACK_BLACKLIST_MS = 60000;
    private final int minimumLoadableRetryCount;

    public DefaultLoadErrorHandlingPolicy() {
        this(-1);
    }

    public DefaultLoadErrorHandlingPolicy(int minimumLoadableRetryCount2) {
        this.minimumLoadableRetryCount = minimumLoadableRetryCount2;
    }

    public long getBlacklistDurationMsFor(int dataType, long loadDurationMs, IOException exception, int errorCount) {
        boolean z = exception instanceof InvalidResponseCodeException;
        long j = C.TIME_UNSET;
        if (!z) {
            return C.TIME_UNSET;
        }
        int responseCode = ((InvalidResponseCodeException) exception).responseCode;
        if (responseCode == 404 || responseCode == 410) {
            j = 60000;
        }
        return j;
    }

    public long getRetryDelayMsFor(int dataType, long loadDurationMs, IOException exception, int errorCount) {
        if (exception instanceof ParserException) {
            return C.TIME_UNSET;
        }
        return (long) Math.min((errorCount - 1) * 1000, 5000);
    }

    public int getMinimumLoadableRetryCount(int dataType) {
        int i = this.minimumLoadableRetryCount;
        if (i != -1) {
            return i;
        }
        return dataType == 7 ? 6 : 3;
    }
}
