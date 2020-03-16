package org.quantumbadger.redreader.cache.downloadstrategy;

import org.quantumbadger.redreader.cache.CacheEntry;

public interface DownloadStrategy {
    boolean shouldDownloadIfCacheEntryFound(CacheEntry cacheEntry);

    boolean shouldDownloadIfNotCached();

    boolean shouldDownloadWithoutCheckingCache();
}
