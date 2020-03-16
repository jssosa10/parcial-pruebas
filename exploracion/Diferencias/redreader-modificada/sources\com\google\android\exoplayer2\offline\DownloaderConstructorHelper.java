package com.google.android.exoplayer2.offline;

import android.support.annotation.Nullable;
import com.google.android.exoplayer2.upstream.DataSink;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.upstream.DummyDataSource;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.PriorityDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSink;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.PriorityTaskManager;

public final class DownloaderConstructorHelper {
    private final Cache cache;
    private final Factory cacheReadDataSourceFactory;
    private final DataSink.Factory cacheWriteDataSinkFactory;
    private final PriorityTaskManager priorityTaskManager;
    private final Factory upstreamDataSourceFactory;

    public DownloaderConstructorHelper(Cache cache2, Factory upstreamDataSourceFactory2) {
        this(cache2, upstreamDataSourceFactory2, null, null, null);
    }

    public DownloaderConstructorHelper(Cache cache2, Factory upstreamDataSourceFactory2, @Nullable Factory cacheReadDataSourceFactory2, @Nullable DataSink.Factory cacheWriteDataSinkFactory2, @Nullable PriorityTaskManager priorityTaskManager2) {
        Assertions.checkNotNull(upstreamDataSourceFactory2);
        this.cache = cache2;
        this.upstreamDataSourceFactory = upstreamDataSourceFactory2;
        this.cacheReadDataSourceFactory = cacheReadDataSourceFactory2;
        this.cacheWriteDataSinkFactory = cacheWriteDataSinkFactory2;
        this.priorityTaskManager = priorityTaskManager2;
    }

    public Cache getCache() {
        return this.cache;
    }

    public PriorityTaskManager getPriorityTaskManager() {
        PriorityTaskManager priorityTaskManager2 = this.priorityTaskManager;
        return priorityTaskManager2 != null ? priorityTaskManager2 : new PriorityTaskManager();
    }

    public CacheDataSource buildCacheDataSource(boolean offline) {
        Factory factory = this.cacheReadDataSourceFactory;
        DataSource cacheReadDataSource = factory != null ? factory.createDataSource() : new FileDataSource();
        if (offline) {
            CacheDataSource cacheDataSource = new CacheDataSource(this.cache, DummyDataSource.INSTANCE, cacheReadDataSource, null, 1, null);
            return cacheDataSource;
        }
        DataSink.Factory factory2 = this.cacheWriteDataSinkFactory;
        DataSink cacheWriteDataSink = factory2 != null ? factory2.createDataSink() : new CacheDataSink(this.cache, 2097152);
        DataSource upstream = this.upstreamDataSourceFactory.createDataSource();
        PriorityTaskManager priorityTaskManager2 = this.priorityTaskManager;
        CacheDataSource cacheDataSource2 = new CacheDataSource(this.cache, priorityTaskManager2 == null ? upstream : new PriorityDataSource(upstream, priorityTaskManager2, -1000), cacheReadDataSource, cacheWriteDataSink, 1, null);
        return cacheDataSource2;
    }
}
