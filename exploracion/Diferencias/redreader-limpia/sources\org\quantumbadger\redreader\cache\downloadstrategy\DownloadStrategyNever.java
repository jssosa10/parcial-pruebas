package org.quantumbadger.redreader.cache.downloadstrategy;

import org.quantumbadger.redreader.cache.CacheEntry;

public class DownloadStrategyNever implements DownloadStrategy {
    public static final DownloadStrategyNever INSTANCE = new DownloadStrategyNever();

    private DownloadStrategyNever() {
    }

    public boolean shouldDownloadWithoutCheckingCache() {
        return false;
    }

    public boolean shouldDownloadIfCacheEntryFound(CacheEntry entry) {
        return false;
    }

    public boolean shouldDownloadIfNotCached() {
        return false;
    }
}
