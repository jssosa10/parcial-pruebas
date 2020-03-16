package org.quantumbadger.redreader.common;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;

public abstract class UpdateNotifier<E> {
    private final LinkedList<WeakReference<E>> listeners = new LinkedList<>();

    /* access modifiers changed from: protected */
    public abstract void notifyListener(E e);

    public synchronized void addListener(E updateListener) {
        this.listeners.add(new WeakReference(updateListener));
    }

    public synchronized void updateAllListeners() {
        Iterator<WeakReference<E>> iter = this.listeners.iterator();
        while (iter.hasNext()) {
            E listener = ((WeakReference) iter.next()).get();
            if (listener == null) {
                iter.remove();
            } else {
                notifyListener(listener);
            }
        }
    }
}
