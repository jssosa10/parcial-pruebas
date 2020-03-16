package org.quantumbadger.redreader.cache.downloadstrategy;

import org.quantumbadger.redreader.cache.CacheEntry;
import org.quantumbadger.redreader.common.TimestampBound;

public class DownloadStrategyIfTimestampOutsideBounds implements DownloadStrategy {
    private final TimestampBound mTimestampBound;

    public DownloadStrategyIfTimestampOutsideBounds(TimestampBound timestampBound) {
        this.mTimestampBound = timestampBound;
    }

    public boolean shouldDownloadWithoutCheckingCache() {
        return false;
    }

    public boolean shouldDownloadIfCacheEntryFound(CacheEntry entry) {
        return !this.mTimestampBound.verifyTimestamp(entry.timestamp);
    }

    public boolean shouldDownloadIfNotCached() {
        return true;
    }
}
