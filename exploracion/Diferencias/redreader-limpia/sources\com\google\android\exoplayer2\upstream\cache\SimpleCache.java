package com.google.android.exoplayer2.upstream.cache;

import android.os.ConditionVariable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.upstream.cache.Cache.CacheException;
import com.google.android.exoplayer2.upstream.cache.Cache.Listener;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

public final class SimpleCache implements Cache {
    private static final String TAG = "SimpleCache";
    private static boolean cacheFolderLockingDisabled;
    private static final HashSet<File> lockedCacheDirs = new HashSet<>();
    private final File cacheDir;
    /* access modifiers changed from: private */
    public final CacheEvictor evictor;
    private final CachedContentIndex index;
    private final HashMap<String, ArrayList<Listener>> listeners;
    private boolean released;
    private long totalSpace;

    public static synchronized boolean isCacheFolderLocked(File cacheFolder) {
        boolean contains;
        synchronized (SimpleCache.class) {
            contains = lockedCacheDirs.contains(cacheFolder.getAbsoluteFile());
        }
        return contains;
    }

    @Deprecated
    public static synchronized void disableCacheFolderLocking() {
        synchronized (SimpleCache.class) {
            cacheFolderLockingDisabled = true;
            lockedCacheDirs.clear();
        }
    }

    public SimpleCache(File cacheDir2, CacheEvictor evictor2) {
        this(cacheDir2, evictor2, null, false);
    }

    public SimpleCache(File cacheDir2, CacheEvictor evictor2, byte[] secretKey) {
        this(cacheDir2, evictor2, secretKey, secretKey != null);
    }

    public SimpleCache(File cacheDir2, CacheEvictor evictor2, byte[] secretKey, boolean encrypt) {
        this(cacheDir2, evictor2, new CachedContentIndex(cacheDir2, secretKey, encrypt));
    }

    SimpleCache(File cacheDir2, CacheEvictor evictor2, CachedContentIndex index2) {
        if (lockFolder(cacheDir2)) {
            this.cacheDir = cacheDir2;
            this.evictor = evictor2;
            this.index = index2;
            this.listeners = new HashMap<>();
            final ConditionVariable conditionVariable = new ConditionVariable();
            new Thread("SimpleCache.initialize()") {
                public void run() {
                    synchronized (SimpleCache.this) {
                        conditionVariable.open();
                        SimpleCache.this.initialize();
                        SimpleCache.this.evictor.onCacheInitialized();
                    }
                }
            }.start();
            conditionVariable.block();
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Another SimpleCache instance uses the folder: ");
        sb.append(cacheDir2);
        throw new IllegalStateException(sb.toString());
    }

    public synchronized void release() throws CacheException {
        if (!this.released) {
            this.listeners.clear();
            try {
                removeStaleSpansAndCachedContents();
            } finally {
                unlockFolder(this.cacheDir);
                this.released = true;
            }
        }
    }

    public synchronized NavigableSet<CacheSpan> addListener(String key, Listener listener) {
        Assertions.checkState(!this.released);
        ArrayList arrayList = (ArrayList) this.listeners.get(key);
        if (arrayList == null) {
            arrayList = new ArrayList();
            this.listeners.put(key, arrayList);
        }
        arrayList.add(listener);
        return getCachedSpans(key);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0020, code lost:
        return;
     */
    public synchronized void removeListener(String key, Listener listener) {
        if (!this.released) {
            ArrayList<Listener> listenersForKey = (ArrayList) this.listeners.get(key);
            if (listenersForKey != null) {
                listenersForKey.remove(listener);
                if (listenersForKey.isEmpty()) {
                    this.listeners.remove(key);
                }
            }
        }
    }

    @NonNull
    public synchronized NavigableSet<CacheSpan> getCachedSpans(String key) {
        TreeSet treeSet;
        Assertions.checkState(!this.released);
        CachedContent cachedContent = this.index.get(key);
        if (cachedContent != null) {
            if (!cachedContent.isEmpty()) {
                treeSet = new TreeSet(cachedContent.getSpans());
            }
        }
        treeSet = new TreeSet();
        return treeSet;
    }

    public synchronized Set<String> getKeys() {
        Assertions.checkState(!this.released);
        return new HashSet(this.index.getKeys());
    }

    public synchronized long getCacheSpace() {
        Assertions.checkState(!this.released);
        return this.totalSpace;
    }

    public synchronized SimpleCacheSpan startReadWrite(String key, long position) throws InterruptedException, CacheException {
        SimpleCacheSpan span;
        while (true) {
            span = startReadWriteNonBlocking(key, position);
            if (span == null) {
                wait();
            }
        }
        return span;
    }

    @Nullable
    public synchronized SimpleCacheSpan startReadWriteNonBlocking(String key, long position) throws CacheException {
        Assertions.checkState(!this.released);
        SimpleCacheSpan cacheSpan = getSpan(key, position);
        if (cacheSpan.isCached) {
            try {
                SimpleCacheSpan newCacheSpan = this.index.get(key).touch(cacheSpan);
                notifySpanTouched(cacheSpan, newCacheSpan);
                return newCacheSpan;
            } catch (CacheException e) {
                return cacheSpan;
            }
        } else {
            CachedContent cachedContent = this.index.getOrAdd(key);
            if (cachedContent.isLocked()) {
                return null;
            }
            cachedContent.setLocked(true);
            return cacheSpan;
        }
    }

    public synchronized File startFile(String key, long position, long maxLength) throws CacheException {
        CachedContent cachedContent;
        Assertions.checkState(!this.released);
        cachedContent = this.index.get(key);
        Assertions.checkNotNull(cachedContent);
        Assertions.checkState(cachedContent.isLocked());
        if (!this.cacheDir.exists()) {
            this.cacheDir.mkdirs();
            removeStaleSpansAndCachedContents();
        }
        this.evictor.onStartFile(this, key, position, maxLength);
        return SimpleCacheSpan.getCacheFile(this.cacheDir, cachedContent.id, position, System.currentTimeMillis());
    }

    public synchronized void commitFile(File file) throws CacheException {
        boolean z = true;
        Assertions.checkState(!this.released);
        SimpleCacheSpan span = SimpleCacheSpan.createCacheEntry(file, this.index);
        Assertions.checkState(span != null);
        CachedContent cachedContent = this.index.get(span.key);
        Assertions.checkNotNull(cachedContent);
        Assertions.checkState(cachedContent.isLocked());
        if (file.exists()) {
            if (file.length() == 0) {
                file.delete();
                return;
            }
            long length = ContentMetadataInternal.getContentLength(cachedContent.getMetadata());
            if (length != -1) {
                if (span.position + span.length > length) {
                    z = false;
                }
                Assertions.checkState(z);
            }
            addSpan(span);
            this.index.store();
            notifyAll();
        }
    }

    public synchronized void releaseHoleSpan(CacheSpan holeSpan) {
        Assertions.checkState(!this.released);
        CachedContent cachedContent = this.index.get(holeSpan.key);
        Assertions.checkNotNull(cachedContent);
        Assertions.checkState(cachedContent.isLocked());
        cachedContent.setLocked(false);
        this.index.maybeRemove(cachedContent.key);
        notifyAll();
    }

    public synchronized void removeSpan(CacheSpan span) throws CacheException {
        Assertions.checkState(!this.released);
        removeSpan(span, true);
    }

    public synchronized boolean isCached(String key, long position, long length) {
        boolean z;
        z = true;
        Assertions.checkState(!this.released);
        CachedContent cachedContent = this.index.get(key);
        if (cachedContent == null || cachedContent.getCachedBytesLength(position, length) < length) {
            z = false;
        }
        return z;
    }

    public synchronized long getCachedLength(String key, long position, long length) {
        CachedContent cachedContent;
        Assertions.checkState(!this.released);
        cachedContent = this.index.get(key);
        return cachedContent != null ? cachedContent.getCachedBytesLength(position, length) : -length;
    }

    public synchronized void setContentLength(String key, long length) throws CacheException {
        ContentMetadataMutations mutations = new ContentMetadataMutations();
        ContentMetadataInternal.setContentLength(mutations, length);
        applyContentMetadataMutations(key, mutations);
    }

    public synchronized long getContentLength(String key) {
        return ContentMetadataInternal.getContentLength(getContentMetadata(key));
    }

    public synchronized void applyContentMetadataMutations(String key, ContentMetadataMutations mutations) throws CacheException {
        Assertions.checkState(!this.released);
        this.index.applyContentMetadataMutations(key, mutations);
        this.index.store();
    }

    public synchronized ContentMetadata getContentMetadata(String key) {
        Assertions.checkState(!this.released);
        return this.index.getContentMetadata(key);
    }

    private SimpleCacheSpan getSpan(String key, long position) throws CacheException {
        SimpleCacheSpan span;
        CachedContent cachedContent = this.index.get(key);
        if (cachedContent == null) {
            return SimpleCacheSpan.createOpenHole(key, position);
        }
        while (true) {
            span = cachedContent.getSpan(position);
            if (!span.isCached || span.file.exists()) {
                return span;
            }
            removeStaleSpansAndCachedContents();
        }
        return span;
    }

    /* access modifiers changed from: private */
    public void initialize() {
        if (!this.cacheDir.exists()) {
            this.cacheDir.mkdirs();
            return;
        }
        this.index.load();
        File[] files = this.cacheDir.listFiles();
        if (files != null) {
            int length = files.length;
            for (int i = 0; i < length; i++) {
                File file = files[i];
                if (!file.getName().equals(CachedContentIndex.FILE_NAME)) {
                    SimpleCacheSpan span = file.length() > 0 ? SimpleCacheSpan.createCacheEntry(file, this.index) : null;
                    if (span != null) {
                        addSpan(span);
                    } else {
                        file.delete();
                    }
                }
            }
            this.index.removeEmpty();
            try {
                this.index.store();
            } catch (CacheException e) {
                Log.e(TAG, "Storing index file failed", e);
            }
        }
    }

    private void addSpan(SimpleCacheSpan span) {
        this.index.getOrAdd(span.key).addSpan(span);
        this.totalSpace += span.length;
        notifySpanAdded(span);
    }

    private void removeSpan(CacheSpan span, boolean removeEmptyCachedContent) throws CacheException {
        CachedContent cachedContent = this.index.get(span.key);
        if (cachedContent != null && cachedContent.removeSpan(span)) {
            this.totalSpace -= span.length;
            if (removeEmptyCachedContent) {
                try {
                    this.index.maybeRemove(cachedContent.key);
                    this.index.store();
                } catch (Throwable th) {
                    notifySpanRemoved(span);
                    throw th;
                }
            }
            notifySpanRemoved(span);
        }
    }

    private void removeStaleSpansAndCachedContents() throws CacheException {
        ArrayList<CacheSpan> spansToBeRemoved = new ArrayList<>();
        for (CachedContent cachedContent : this.index.getAll()) {
            Iterator it = cachedContent.getSpans().iterator();
            while (it.hasNext()) {
                CacheSpan span = (CacheSpan) it.next();
                if (!span.file.exists()) {
                    spansToBeRemoved.add(span);
                }
            }
        }
        for (int i = 0; i < spansToBeRemoved.size(); i++) {
            removeSpan((CacheSpan) spansToBeRemoved.get(i), false);
        }
        this.index.removeEmpty();
        this.index.store();
    }

    private void notifySpanRemoved(CacheSpan span) {
        ArrayList<Listener> keyListeners = (ArrayList) this.listeners.get(span.key);
        if (keyListeners != null) {
            for (int i = keyListeners.size() - 1; i >= 0; i--) {
                ((Listener) keyListeners.get(i)).onSpanRemoved(this, span);
            }
        }
        this.evictor.onSpanRemoved(this, span);
    }

    private void notifySpanAdded(SimpleCacheSpan span) {
        ArrayList<Listener> keyListeners = (ArrayList) this.listeners.get(span.key);
        if (keyListeners != null) {
            for (int i = keyListeners.size() - 1; i >= 0; i--) {
                ((Listener) keyListeners.get(i)).onSpanAdded(this, span);
            }
        }
        this.evictor.onSpanAdded(this, span);
    }

    private void notifySpanTouched(SimpleCacheSpan oldSpan, CacheSpan newSpan) {
        ArrayList<Listener> keyListeners = (ArrayList) this.listeners.get(oldSpan.key);
        if (keyListeners != null) {
            for (int i = keyListeners.size() - 1; i >= 0; i--) {
                ((Listener) keyListeners.get(i)).onSpanTouched(this, oldSpan, newSpan);
            }
        }
        this.evictor.onSpanTouched(this, oldSpan, newSpan);
    }

    private static synchronized boolean lockFolder(File cacheDir2) {
        synchronized (SimpleCache.class) {
            if (cacheFolderLockingDisabled) {
                return true;
            }
            boolean add = lockedCacheDirs.add(cacheDir2.getAbsoluteFile());
            return add;
        }
    }

    private static synchronized void unlockFolder(File cacheDir2) {
        synchronized (SimpleCache.class) {
            if (!cacheFolderLockingDisabled) {
                lockedCacheDirs.remove(cacheDir2.getAbsoluteFile());
            }
        }
    }
}
