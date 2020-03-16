package org.quantumbadger.redreader.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingQueue;
import org.quantumbadger.redreader.common.TimestampBound;
import org.quantumbadger.redreader.common.TriggerableThread;
import org.quantumbadger.redreader.io.WritableObject;

public class ThreadedRawObjectDB<K, V extends WritableObject<K>, F> implements CacheDataSource<K, V, F> {
    /* access modifiers changed from: private */
    public final CacheDataSource<K, V, F> alternateSource;
    /* access modifiers changed from: private */
    public final RawObjectDB<K, V> db;
    private final Object ioLock = new Object();
    private final TriggerableThread readThread = new TriggerableThread(new Runnable() {
        public void run() {
            ThreadedRawObjectDB.this.doRead();
        }
    }, 0);
    private final LinkedBlockingQueue<ReadOperation> toRead = new LinkedBlockingQueue<>();
    /* access modifiers changed from: private */
    public final HashMap<K, V> toWrite = new HashMap<>();
    private final TriggerableThread writeThread = new TriggerableThread(new Runnable() {
        public void run() {
            ThreadedRawObjectDB.this.doWrite();
        }
    }, 1500);

    private class BulkReadOperation extends ReadOperation {
        public final Collection<K> keys;
        public final RequestResponseHandler<HashMap<K, V>, F> responseHandler;

        private BulkReadOperation(TimestampBound timestampBound, RequestResponseHandler<HashMap<K, V>, F> responseHandler2, Collection<K> keys2) {
            super(timestampBound);
            this.responseHandler = responseHandler2;
            this.keys = keys2;
        }

        public void run() {
            final HashMap<K, V> existingResult = new HashMap<>(this.keys.size());
            long oldestTimestamp = Long.MAX_VALUE;
            synchronized (ThreadedRawObjectDB.this.toWrite) {
                Iterator<K> iter = this.keys.iterator();
                while (iter.hasNext()) {
                    K key = iter.next();
                    V writeCacheResult = (WritableObject) ThreadedRawObjectDB.this.toWrite.get(key);
                    if (writeCacheResult != null && this.timestampBound.verifyTimestamp(writeCacheResult.getTimestamp())) {
                        iter.remove();
                        existingResult.put(key, writeCacheResult);
                        oldestTimestamp = Math.min(oldestTimestamp, writeCacheResult.getTimestamp());
                    }
                }
            }
            if (this.keys.size() == 0) {
                this.responseHandler.onRequestSuccess(existingResult, oldestTimestamp);
                return;
            }
            Iterator<K> iter2 = this.keys.iterator();
            while (iter2.hasNext()) {
                K key2 = iter2.next();
                V dbResult = ThreadedRawObjectDB.this.db.getById(key2);
                if (dbResult != null && this.timestampBound.verifyTimestamp(dbResult.getTimestamp())) {
                    iter2.remove();
                    existingResult.put(key2, dbResult);
                    oldestTimestamp = Math.min(oldestTimestamp, dbResult.getTimestamp());
                }
            }
            if (this.keys.size() == 0) {
                this.responseHandler.onRequestSuccess(existingResult, oldestTimestamp);
                return;
            }
            final long outerOldestTimestamp = oldestTimestamp;
            ThreadedRawObjectDB.this.alternateSource.performRequest(this.keys, this.timestampBound, (RequestResponseHandler<HashMap<K, V>, F>) new RequestResponseHandler<HashMap<K, V>, F>() {
                public void onRequestFailed(F failureReason) {
                    BulkReadOperation.this.responseHandler.onRequestFailed(failureReason);
                }

                public void onRequestSuccess(HashMap<K, V> result, long timeCached) {
                    ThreadedRawObjectDB.this.performWrite(result.values());
                    existingResult.putAll(result);
                    BulkReadOperation.this.responseHandler.onRequestSuccess(existingResult, Math.min(timeCached, outerOldestTimestamp));
                }
            });
        }
    }

    private abstract class ReadOperation {
        public final TimestampBound timestampBound;

        public abstract void run();

        private ReadOperation(TimestampBound timestampBound2) {
            this.timestampBound = timestampBound2;
        }
    }

    private class SingleReadOperation extends ReadOperation {
        public final K key;
        public final RequestResponseHandler<V, F> responseHandler;

        private SingleReadOperation(TimestampBound timestampBound, RequestResponseHandler<V, F> responseHandler2, K key2) {
            super(timestampBound);
            this.responseHandler = responseHandler2;
            this.key = key2;
        }

        /* JADX WARNING: Code restructure failed: missing block: B:11:0x002f, code lost:
            r0 = org.quantumbadger.redreader.io.ThreadedRawObjectDB.access$600(r5.this$0).getById(r5.key);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:12:0x003b, code lost:
            if (r0 == null) goto L_0x0053;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:14:0x0047, code lost:
            if (r5.timestampBound.verifyTimestamp(r0.getTimestamp()) == false) goto L_0x0053;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:15:0x0049, code lost:
            r5.responseHandler.onRequestSuccess(r0, r0.getTimestamp());
         */
        /* JADX WARNING: Code restructure failed: missing block: B:16:0x0052, code lost:
            return;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:17:0x0053, code lost:
            org.quantumbadger.redreader.io.ThreadedRawObjectDB.access$700(r5.this$0).performRequest(r5.key, r5.timestampBound, (org.quantumbadger.redreader.io.RequestResponseHandler<V, F>) new org.quantumbadger.redreader.io.ThreadedRawObjectDB.SingleReadOperation.AnonymousClass1<V,F>(r5));
         */
        /* JADX WARNING: Code restructure failed: missing block: B:18:0x0065, code lost:
            return;
         */
        public void run() {
            synchronized (ThreadedRawObjectDB.this.toWrite) {
                V writeCacheResult = (WritableObject) ThreadedRawObjectDB.this.toWrite.get(this.key);
                if (writeCacheResult != null && this.timestampBound.verifyTimestamp(writeCacheResult.getTimestamp())) {
                    this.responseHandler.onRequestSuccess(writeCacheResult, writeCacheResult.getTimestamp());
                }
            }
        }
    }

    public ThreadedRawObjectDB(RawObjectDB<K, V> db2, CacheDataSource<K, V, F> alternateSource2) {
        this.db = db2;
        this.alternateSource = alternateSource2;
    }

    /* access modifiers changed from: private */
    public void doWrite() {
        ArrayList<V> values;
        synchronized (this.ioLock) {
            synchronized (this.toWrite) {
                values = new ArrayList<>(this.toWrite.values());
                this.toWrite.clear();
            }
            this.db.putAll(values);
        }
    }

    /* access modifiers changed from: private */
    public void doRead() {
        synchronized (this.ioLock) {
            while (!this.toRead.isEmpty()) {
                ((ReadOperation) this.toRead.remove()).run();
            }
        }
    }

    public void performRequest(K key, TimestampBound timestampBound, RequestResponseHandler<V, F> handler) {
        LinkedBlockingQueue<ReadOperation> linkedBlockingQueue = this.toRead;
        SingleReadOperation singleReadOperation = new SingleReadOperation(timestampBound, handler, key);
        linkedBlockingQueue.offer(singleReadOperation);
        this.readThread.trigger();
    }

    public void performRequest(Collection<K> keys, TimestampBound timestampBound, RequestResponseHandler<HashMap<K, V>, F> handler) {
        LinkedBlockingQueue<ReadOperation> linkedBlockingQueue = this.toRead;
        BulkReadOperation bulkReadOperation = new BulkReadOperation(timestampBound, handler, keys);
        linkedBlockingQueue.offer(bulkReadOperation);
        this.readThread.trigger();
    }

    public void performWrite(V value) {
        synchronized (this.toWrite) {
            this.toWrite.put(value.getKey(), value);
        }
        this.writeThread.trigger();
    }

    public void performWrite(Collection<V> values) {
        synchronized (this.toWrite) {
            for (V value : values) {
                this.toWrite.put(value.getKey(), value);
            }
        }
        this.writeThread.trigger();
    }
}
