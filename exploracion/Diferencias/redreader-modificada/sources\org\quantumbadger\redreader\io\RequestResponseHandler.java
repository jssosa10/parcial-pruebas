package org.quantumbadger.redreader.io;

public interface RequestResponseHandler<E, F> {
    void onRequestFailed(F f);

    void onRequestSuccess(E e, long j);
}
