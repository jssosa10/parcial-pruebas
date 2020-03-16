package com.google.android.exoplayer2.upstream.cache;

import android.support.annotation.Nullable;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SimpleCacheSpan extends CacheSpan {
    private static final Pattern CACHE_FILE_PATTERN_V1 = Pattern.compile("^(.+)\\.(\\d+)\\.(\\d+)\\.v1\\.exo$", 32);
    private static final Pattern CACHE_FILE_PATTERN_V2 = Pattern.compile("^(.+)\\.(\\d+)\\.(\\d+)\\.v2\\.exo$", 32);
    private static final Pattern CACHE_FILE_PATTERN_V3 = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.v3\\.exo$", 32);
    private static final String SUFFIX = ".v3.exo";

    public static File getCacheFile(File cacheDir, int id, long position, long lastAccessTimestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append(id);
        sb.append(".");
        sb.append(position);
        sb.append(".");
        sb.append(lastAccessTimestamp);
        sb.append(SUFFIX);
        return new File(cacheDir, sb.toString());
    }

    public static SimpleCacheSpan createLookup(String key, long position) {
        SimpleCacheSpan simpleCacheSpan = new SimpleCacheSpan(key, position, -1, C.TIME_UNSET, null);
        return simpleCacheSpan;
    }

    public static SimpleCacheSpan createOpenHole(String key, long position) {
        SimpleCacheSpan simpleCacheSpan = new SimpleCacheSpan(key, position, -1, C.TIME_UNSET, null);
        return simpleCacheSpan;
    }

    public static SimpleCacheSpan createClosedHole(String key, long position, long length) {
        SimpleCacheSpan simpleCacheSpan = new SimpleCacheSpan(key, position, length, C.TIME_UNSET, null);
        return simpleCacheSpan;
    }

    @Nullable
    public static SimpleCacheSpan createCacheEntry(File file, CachedContentIndex index) {
        File file2;
        String name = file.getName();
        SimpleCacheSpan simpleCacheSpan = null;
        if (!name.endsWith(SUFFIX)) {
            file2 = upgradeFile(file, index);
            if (file2 == null) {
                return null;
            }
            name = file2.getName();
        } else {
            file2 = file;
        }
        Matcher matcher = CACHE_FILE_PATTERN_V3.matcher(name);
        if (!matcher.matches()) {
            return null;
        }
        long length = file2.length();
        String key = index.getKeyForId(Integer.parseInt(matcher.group(1)));
        if (key != null) {
            simpleCacheSpan = new SimpleCacheSpan(key, Long.parseLong(matcher.group(2)), length, Long.parseLong(matcher.group(3)), file2);
        }
        return simpleCacheSpan;
    }

    @Nullable
    private static File upgradeFile(File file, CachedContentIndex index) {
        String key;
        String filename = file.getName();
        Matcher matcher = CACHE_FILE_PATTERN_V2.matcher(filename);
        if (matcher.matches()) {
            key = Util.unescapeFileName(matcher.group(1));
            if (key == null) {
                return null;
            }
        } else {
            matcher = CACHE_FILE_PATTERN_V1.matcher(filename);
            if (!matcher.matches()) {
                return null;
            }
            key = matcher.group(1);
        }
        File newCacheFile = getCacheFile(file.getParentFile(), index.assignIdForKey(key), Long.parseLong(matcher.group(2)), Long.parseLong(matcher.group(3)));
        if (!file.renameTo(newCacheFile)) {
            return null;
        }
        return newCacheFile;
    }

    private SimpleCacheSpan(String key, long position, long length, long lastAccessTimestamp, @Nullable File file) {
        super(key, position, length, lastAccessTimestamp, file);
    }

    public SimpleCacheSpan copyWithUpdatedLastAccessTime(int id) {
        Assertions.checkState(this.isCached);
        long now = System.currentTimeMillis();
        SimpleCacheSpan simpleCacheSpan = new SimpleCacheSpan(this.key, this.position, this.length, now, getCacheFile(this.file.getParentFile(), id, this.position, now));
        return simpleCacheSpan;
    }
}
