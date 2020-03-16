package org.quantumbadger.redreader.common;

import java.util.HashSet;
import java.util.LinkedList;

public class UniqueSynchronizedQueue<E> {
    private final LinkedList<E> queue = new LinkedList<>();
    private final HashSet<E> set = new HashSet<>();

    public synchronized void enqueue(E object) {
        if (this.set.add(object)) {
            this.queue.addLast(object);
        }
    }

    public synchronized E dequeue() {
        if (this.queue.isEmpty()) {
            return null;
        }
        E result = this.queue.removeFirst();
        this.set.remove(result);
        return result;
    }
}
