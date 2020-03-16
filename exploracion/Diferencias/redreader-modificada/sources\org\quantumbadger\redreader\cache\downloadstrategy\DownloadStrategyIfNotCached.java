package org.quantumbadger.redreader.cache.downloadstrategy;

import org.quantumbadger.redreader.cache.CacheEntry;

public class DownloadStrategyIfNotCached implements DownloadStrategy {
    public static final DownloadStrategyIfNotCached INSTANCE = new DownloadStrategyIfNotCached();

    private DownloadStrategyIfNotCached() {
    }

    public boolean shouldDownloadWithoutCheckingCache() {
        return false;
    }

    public boolean shouldDownloadIfCacheEntryFound(CacheEntry entry) {
        return false;
    }

    public boolean shouldDownloadIfNotCached() {
        return true;
    }
}
