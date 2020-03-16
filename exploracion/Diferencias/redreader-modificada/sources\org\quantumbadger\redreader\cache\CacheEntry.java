package org.quantumbadger.redreader.cache;

import android.database.Cursor;
import java.util.UUID;

public final class CacheEntry {
    public final long id;
    public final String mimetype;
    public final UUID session;
    public final long timestamp;

    CacheEntry(Cursor cursor) {
        this.id = cursor.getLong(0);
        this.session = UUID.fromString(cursor.getString(3));
        this.timestamp = cursor.getLong(4);
        this.mimetype = cursor.getString(7);
    }
}
