package org.quantumbadger.redreader.common.collections;

import java.util.ArrayList;

public class Stack<E> {
    private final ArrayList<E> mData;

    public Stack(int initialCapacity) {
        this.mData = new ArrayList<>(initialCapacity);
    }

    public void push(E obj) {
        this.mData.add(obj);
    }

    public E pop() {
        ArrayList<E> arrayList = this.mData;
        return arrayList.remove(arrayList.size() - 1);
    }

    public boolean isEmpty() {
        return this.mData.isEmpty();
    }

    public boolean remove(E obj) {
        return this.mData.remove(obj);
    }
}
