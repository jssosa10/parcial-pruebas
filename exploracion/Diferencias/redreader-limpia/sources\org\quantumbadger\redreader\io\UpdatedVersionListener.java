package org.quantumbadger.redreader.io;

import org.quantumbadger.redreader.io.WritableObject;

public interface UpdatedVersionListener<K, V extends WritableObject<K>> {
    void onUpdatedVersion(V v);
}
