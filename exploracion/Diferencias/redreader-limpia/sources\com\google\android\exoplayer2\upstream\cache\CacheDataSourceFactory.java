package com.google.android.exoplayer2.upstream.cache;

import com.google.android.exoplayer2.upstream.DataSink;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSource.Factory;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource.EventListener;

public final class CacheDataSourceFactory implements Factory {
    private final Cache cache;
    private final Factory cacheReadDataSourceFactory;
    private final DataSink.Factory cacheWriteDataSinkFactory;
    private final EventListener eventListener;
    private final int flags;
    private final Factory upstreamFactory;

    public CacheDataSourceFactory(Cache cache2, Factory upstreamFactory2) {
        this(cache2, upstreamFactory2, 0);
    }

    public CacheDataSourceFactory(Cache cache2, Factory upstreamFactory2, int flags2) {
        this(cache2, upstreamFactory2, flags2, 2097152);
    }

    public CacheDataSourceFactory(Cache cache2, Factory upstreamFactory2, int flags2, long maxCacheFileSize) {
        this(cache2, upstreamFactory2, new FileDataSourceFactory(), new CacheDataSinkFactory(cache2, maxCacheFileSize), flags2, null);
    }

    public CacheDataSourceFactory(Cache cache2, Factory upstreamFactory2, Factory cacheReadDataSourceFactory2, DataSink.Factory cacheWriteDataSinkFactory2, int flags2, EventListener eventListener2) {
        this.cache = cache2;
        this.upstreamFactory = upstreamFactory2;
        this.cacheReadDataSourceFactory = cacheReadDataSourceFactory2;
        this.cacheWriteDataSinkFactory = cacheWriteDataSinkFactory2;
        this.flags = flags2;
        this.eventListener = eventListener2;
    }

    public CacheDataSource createDataSource() {
        Cache cache2 = this.cache;
        DataSource createDataSource = this.upstreamFactory.createDataSource();
        DataSource createDataSource2 = this.cacheReadDataSourceFactory.createDataSource();
        DataSink.Factory factory = this.cacheWriteDataSinkFactory;
        CacheDataSource cacheDataSource = new CacheDataSource(cache2, createDataSource, createDataSource2, factory != null ? factory.createDataSink() : null, this.flags, this.eventListener);
        return cacheDataSource;
    }
}
