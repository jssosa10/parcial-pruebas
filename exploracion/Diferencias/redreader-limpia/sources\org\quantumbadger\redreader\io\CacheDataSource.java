package org.quantumbadger.redreader.io;

import java.util.Collection;
import java.util.HashMap;
import org.quantumbadger.redreader.common.TimestampBound;

public interface CacheDataSource<K, V, F> {
    void performRequest(K k, TimestampBound timestampBound, RequestResponseHandler<V, F> requestResponseHandler);

    void performRequest(Collection<K> collection, TimestampBound timestampBound, RequestResponseHandler<HashMap<K, V>, F> requestResponseHandler);

    void performWrite(V v);

    void performWrite(Collection<V> collection);
}
