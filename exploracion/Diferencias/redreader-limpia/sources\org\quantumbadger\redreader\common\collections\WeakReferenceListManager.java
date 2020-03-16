package org.quantumbadger.redreader.common.collections;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

public final class WeakReferenceListManager<E> {
    private final ArrayList<WeakReference<E>> data = new ArrayList<>();

    public interface ArgOperator<E, A> {
        void operate(E e, A a);
    }

    public interface Operator<E> {
        void operate(E e);
    }

    public synchronized int size() {
        return this.data.size();
    }

    public synchronized void add(E object) {
        this.data.add(new WeakReference(object));
    }

    public synchronized void map(Operator<E> operator) {
        Iterator<WeakReference<E>> iterator = this.data.iterator();
        while (iterator.hasNext()) {
            E object = ((WeakReference) iterator.next()).get();
            if (object == null) {
                iterator.remove();
            } else {
                operator.operate(object);
            }
        }
    }

    public synchronized <A> void map(ArgOperator<E, A> operator, A arg) {
        Iterator<WeakReference<E>> iterator = this.data.iterator();
        while (iterator.hasNext()) {
            E object = ((WeakReference) iterator.next()).get();
            if (object == null) {
                iterator.remove();
            } else {
                operator.operate(object, arg);
            }
        }
    }

    public synchronized void remove(E object) {
        Iterator<WeakReference<E>> iterator = this.data.iterator();
        while (iterator.hasNext()) {
            if (((WeakReference) iterator.next()).get() == object) {
                iterator.remove();
            }
        }
    }

    public synchronized void clean() {
        Iterator<WeakReference<E>> iterator = this.data.iterator();
        while (iterator.hasNext()) {
            if (((WeakReference) iterator.next()).get() == null) {
                iterator.remove();
            }
        }
    }

    public synchronized boolean isEmpty() {
        return this.data.isEmpty();
    }
}
