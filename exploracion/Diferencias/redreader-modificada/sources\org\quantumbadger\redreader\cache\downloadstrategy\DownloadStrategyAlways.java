package org.quantumbadger.redreader.cache.downloadstrategy;

import org.quantumbadger.redreader.cache.CacheEntry;

public class DownloadStrategyAlways implements DownloadStrategy {
    public static final DownloadStrategyAlways INSTANCE = new DownloadStrategyAlways();

    private DownloadStrategyAlways() {
    }

    public boolean shouldDownloadWithoutCheckingCache() {
        return true;
    }

    public boolean shouldDownloadIfCacheEntryFound(CacheEntry entry) {
        return true;
    }

    public boolean shouldDownloadIfNotCached() {
        return true;
    }
}
