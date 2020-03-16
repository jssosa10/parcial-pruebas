package org.quantumbadger.redreader.io;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import org.quantumbadger.redreader.common.TimestampBound;
import org.quantumbadger.redreader.common.collections.WeakReferenceListManager;
import org.quantumbadger.redreader.io.WritableObject;

public final class PermanentCache<K, V extends WritableObject<K>, F> implements CacheDataSource<K, V, F> {
    private final CacheDataSource<K, V, F> cacheDataSource;
    /* access modifiers changed from: private */
    public final HashMap<K, CacheEntry> cached = new HashMap<>();
    private final UpdatedVersionListenerNotifier<K, V> updatedVersionListenerNotifier = new UpdatedVersionListenerNotifier<>();

    private final class CacheEntry {
        public final V data;
        public final WeakReferenceListManager<UpdatedVersionListener<K, V>> listeners;

        private CacheEntry(V data2, WeakReferenceListManager<UpdatedVersionListener<K, V>> listeners2) {
            this.data = data2;
            this.listeners = listeners2;
        }

        private CacheEntry(PermanentCache permanentCache, V data2) {
            this(data2, new WeakReferenceListManager<>());
        }
    }

    public PermanentCache(CacheDataSource<K, V, F> cacheDataSource2) {
        this.cacheDataSource = cacheDataSource2;
    }

    public void performRequest(K key, TimestampBound timestampBound, RequestResponseHandler<V, F> handler) {
        performRequest(key, timestampBound, handler, null);
    }

    public synchronized void performRequest(Collection<K> keys, TimestampBound timestampBound, RequestResponseHandler<HashMap<K, V>, F> handler) {
        HashSet<K> keysRemaining = new HashSet<>(keys);
        HashMap<K, V> cacheResult = new HashMap<>(keys.size());
        long oldestTimestamp = Long.MAX_VALUE;
        for (K key : keys) {
            CacheEntry entry = (CacheEntry) this.cached.get(key);
            if (entry != null) {
                V value = entry.data;
                if (timestampBound.verifyTimestamp(value.getTimestamp())) {
                    keysRemaining.remove(key);
                    cacheResult.put(key, value);
                    oldestTimestamp = Math.min(oldestTimestamp, value.getTimestamp());
                }
            }
        }
        if (keysRemaining.size() > 0) {
            final long outerOldestTimestamp = oldestTimestamp;
            CacheDataSource<K, V, F> cacheDataSource2 = this.cacheDataSource;
            final RequestResponseHandler<HashMap<K, V>, F> requestResponseHandler = handler;
            final HashMap hashMap = cacheResult;
            AnonymousClass1 r3 = new RequestResponseHandler<HashMap<K, V>, F>() {
                public void onRequestFailed(F failureReason) {
                    requestResponseHandler.onRequestFailed(failureReason);
                }

                public void onRequestSuccess(HashMap<K, V> result, long timeCached) {
                    hashMap.putAll(result);
                    requestResponseHandler.onRequestSuccess(hashMap, Math.min(timeCached, outerOldestTimestamp));
                }
            };
            cacheDataSource2.performRequest((Collection<K>) keysRemaining, timestampBound, (RequestResponseHandler<HashMap<K, V>, F>) r3);
        } else {
            handler.onRequestSuccess(cacheResult, oldestTimestamp);
        }
    }

    public synchronized void performWrite(V value) {
        put(value, true);
    }

    public void performWrite(Collection<V> values) {
        put(values, true);
    }

    public synchronized void performRequest(final K key, TimestampBound timestampBound, final RequestResponseHandler<V, F> handler, final UpdatedVersionListener<K, V> updatedVersionListener) {
        if (timestampBound != null) {
            CacheEntry existingEntry = (CacheEntry) this.cached.get(key);
            if (existingEntry != null) {
                V existing = existingEntry.data;
                if (timestampBound.verifyTimestamp(existing.getTimestamp())) {
                    handler.onRequestSuccess(existing, existing.getTimestamp());
                    return;
                }
            }
        }
        this.cacheDataSource.performRequest(key, timestampBound, (RequestResponseHandler<V, F>) new RequestResponseHandler<V, F>() {
            public void onRequestFailed(F failureReason) {
                handler.onRequestFailed(failureReason);
            }

            public void onRequestSuccess(V result, long timeCached) {
                synchronized (PermanentCache.this) {
                    PermanentCache.this.put(result, false);
                    if (updatedVersionListener != null) {
                        ((CacheEntry) PermanentCache.this.cached.get(key)).listeners.add(updatedVersionListener);
                    }
                    handler.onRequestSuccess(result, timeCached);
                }
            }
        });
    }

    public synchronized void forceUpdate(K key) {
        this.cacheDataSource.performRequest(key, (TimestampBound) null, (RequestResponseHandler<V, F>) new RequestResponseHandler<V, F>() {
            public void onRequestFailed(F f) {
            }

            public void onRequestSuccess(V result, long timeCached) {
                PermanentCache.this.put(result, false);
            }
        });
    }

    /* access modifiers changed from: private */
    public synchronized void put(V value, boolean writeDown) {
        CacheEntry oldEntry = (CacheEntry) this.cached.get(value.getKey());
        if (oldEntry != null) {
            this.cached.put(value.getKey(), new CacheEntry(value, oldEntry.listeners));
            oldEntry.listeners.map(this.updatedVersionListenerNotifier, value);
        } else {
            this.cached.put(value.getKey(), new CacheEntry((WritableObject) value));
        }
        if (writeDown) {
            this.cacheDataSource.performWrite(value);
        }
    }

    private synchronized void put(Collection<V> values, boolean writeDown) {
        for (V value : values) {
            CacheEntry oldEntry = (CacheEntry) this.cached.get(value.getKey());
            if (oldEntry != null) {
                this.cached.put(value.getKey(), new CacheEntry(value, oldEntry.listeners));
                oldEntry.listeners.map(this.updatedVersionListenerNotifier, value);
            } else {
                this.cached.put(value.getKey(), new CacheEntry((WritableObject) value));
            }
        }
        if (writeDown) {
            this.cacheDataSource.performWrite(values);
        }
    }
}
