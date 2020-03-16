package org.quantumbadger.redreader.common.collections;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import org.quantumbadger.redreader.common.collections.WeakReferenceListManager.ArgOperator;
import org.quantumbadger.redreader.common.collections.WeakReferenceListManager.Operator;

public class WeakReferenceListHashMapManager<K, V> {
    private byte mCleanupCounter = 0;
    private final HashMap<K, WeakReferenceListManager<V>> mData = new HashMap<>();

    public synchronized void add(K key, V value) {
        WeakReferenceListManager weakReferenceListManager = (WeakReferenceListManager) this.mData.get(key);
        if (weakReferenceListManager == null) {
            weakReferenceListManager = new WeakReferenceListManager();
            this.mData.put(key, weakReferenceListManager);
        }
        weakReferenceListManager.add(value);
        byte b = (byte) (this.mCleanupCounter + 1);
        this.mCleanupCounter = b;
        if (b == 0) {
            clean();
        }
    }

    public synchronized void remove(K key, V value) {
        WeakReferenceListManager<V> list = (WeakReferenceListManager) this.mData.get(key);
        if (list != null) {
            list.remove(value);
        }
    }

    public synchronized void map(K key, Operator<V> operator) {
        WeakReferenceListManager<V> list = (WeakReferenceListManager) this.mData.get(key);
        if (list != null) {
            list.map(operator);
        }
    }

    public synchronized <A> void map(K key, ArgOperator<V, A> operator, A arg) {
        WeakReferenceListManager<V> list = (WeakReferenceListManager) this.mData.get(key);
        if (list != null) {
            list.map(operator, arg);
        }
    }

    public synchronized void clean() {
        Iterator<Entry<K, WeakReferenceListManager<V>>> iterator = this.mData.entrySet().iterator();
        while (iterator.hasNext()) {
            WeakReferenceListManager<V> list = (WeakReferenceListManager) ((Entry) iterator.next()).getValue();
            list.clean();
            if (list.isEmpty()) {
                iterator.remove();
            }
        }
    }
}
