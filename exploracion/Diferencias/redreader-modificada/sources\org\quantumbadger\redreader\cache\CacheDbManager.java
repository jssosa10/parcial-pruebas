package org.quantumbadger.redreader.cache;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.UUID;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.common.RRTime;

final class CacheDbManager extends SQLiteOpenHelper {
    private static final String CACHE_DB_FILENAME = "cache.db";
    private static final int CACHE_DB_VERSION = 1;
    private static final String FIELD_ID = "id";
    private static final String FIELD_MIMETYPE = "mimetype";
    private static final String FIELD_SESSION = "session";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_URL = "url";
    private static final String FIELD_USER = "user";
    private static final int STATUS_DONE = 2;
    private static final int STATUS_MOVING = 1;
    private static final String TABLE = "web";
    private final Context context;

    CacheDbManager(Context context2) {
        super(context2, CACHE_DB_FILENAME, null, 1);
        this.context = context2;
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT,%s TEXT NOT NULL,%s TEXT NOT NULL,%s TEXT NOT NULL,%s INTEGER,%s INTEGER,%s INTEGER,%s TEXT,UNIQUE (%s, %s, %s) ON CONFLICT REPLACE)", new Object[]{TABLE, "id", FIELD_URL, FIELD_USER, FIELD_SESSION, FIELD_TIMESTAMP, "status", FIELD_TYPE, FIELD_MIMETYPE, FIELD_USER, FIELD_URL, FIELD_SESSION}));
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new RuntimeException("Attempt to upgrade database in first version of the app!");
    }

    /* access modifiers changed from: 0000 */
    public synchronized LinkedList<CacheEntry> select(URI url, String user, UUID session) {
        String[] queryParams;
        String queryString;
        String[] fields = {"id", FIELD_URL, FIELD_USER, FIELD_SESSION, FIELD_TIMESTAMP, "status", FIELD_TYPE, FIELD_MIMETYPE};
        SQLiteDatabase db = getReadableDatabase();
        if (session == null) {
            queryString = String.format(Locale.US, "%s=%d AND %s=? AND %s=?", new Object[]{"status", Integer.valueOf(2), FIELD_URL, FIELD_USER});
            queryParams = new String[]{url.toString(), user};
        } else {
            queryString = String.format(Locale.US, "%s=%d AND %s=? AND %s=? AND %s=?", new Object[]{"status", Integer.valueOf(2), FIELD_URL, FIELD_USER, FIELD_SESSION});
            queryParams = new String[]{url.toString(), user, session.toString()};
        }
        Cursor cursor = db.query(TABLE, fields, queryString, queryParams, null, null, "timestamp DESC");
        LinkedList<CacheEntry> result = new LinkedList<>();
        if (cursor == null) {
            BugReportActivity.handleGlobalError(this.context, "Cursor was null after query");
            return null;
        }
        while (cursor.moveToNext()) {
            result.add(new CacheEntry(cursor));
        }
        cursor.close();
        return result;
    }

    /* access modifiers changed from: 0000 */
    public synchronized long newEntry(CacheRequest request, UUID session, String mimetype) throws IOException {
        long result;
        if (session != null) {
            try {
                SQLiteDatabase db = getWritableDatabase();
                ContentValues row = new ContentValues();
                row.put(FIELD_URL, request.url.toString());
                row.put(FIELD_USER, request.user.username);
                row.put(FIELD_SESSION, session.toString());
                row.put(FIELD_TYPE, Integer.valueOf(request.fileType));
                row.put("status", Integer.valueOf(1));
                row.put(FIELD_TIMESTAMP, Long.valueOf(RRTime.utcCurrentTimeMillis()));
                row.put(FIELD_MIMETYPE, mimetype);
                result = db.insert(TABLE, null, row);
                if (result < 0) {
                    throw new IOException("DB insert failed");
                }
            } catch (Throwable th) {
                throw th;
            }
        } else {
            throw new RuntimeException("No session to write");
        }
        return result;
    }

    /* access modifiers changed from: 0000 */
    public synchronized void setEntryDone(long id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues row = new ContentValues();
        row.put("status", Integer.valueOf(2));
        db.update(TABLE, row, "id=?", new String[]{String.valueOf(id)});
    }

    /* access modifiers changed from: 0000 */
    public synchronized int delete(long id) {
        return getWritableDatabase().delete(TABLE, "id=?", new String[]{String.valueOf(id)});
    }

    /* access modifiers changed from: protected */
    public synchronized int deleteAllBeforeTimestamp(long timestamp) {
        return getWritableDatabase().delete(TABLE, "timestamp<?", new String[]{String.valueOf(timestamp)});
    }

    public synchronized ArrayList<Long> getFilesToPrune(HashSet<Long> currentFiles, HashMap<Integer, Long> maxAge, long defaultMaxAge) {
        ArrayList<Long> filesToDelete;
        long pruneIfBeforeMs;
        HashMap<Integer, Long> hashMap = maxAge;
        synchronized (this) {
            SQLiteDatabase db = getWritableDatabase();
            long currentTime = RRTime.utcCurrentTimeMillis();
            int i = 0;
            int i2 = 1;
            int i3 = 2;
            Cursor cursor = db.query(TABLE, new String[]{"id", FIELD_TIMESTAMP, FIELD_TYPE}, null, null, null, null, null, null);
            HashSet<Long> currentEntries = new HashSet<>();
            ArrayList<Long> entriesToDelete = new ArrayList<>();
            filesToDelete = new ArrayList<>(32);
            while (cursor.moveToNext()) {
                long id = cursor.getLong(i);
                long timestamp = cursor.getLong(i2);
                int type = cursor.getInt(i3);
                if (hashMap.containsKey(Integer.valueOf(type))) {
                    pruneIfBeforeMs = currentTime - ((Long) hashMap.get(Integer.valueOf(type))).longValue();
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Using default age! Filetype ");
                    sb.append(type);
                    Log.e("RR DEBUG cache", sb.toString());
                    pruneIfBeforeMs = currentTime - defaultMaxAge;
                }
                if (!currentFiles.contains(Long.valueOf(id))) {
                    entriesToDelete.add(Long.valueOf(id));
                } else if (timestamp < pruneIfBeforeMs) {
                    entriesToDelete.add(Long.valueOf(id));
                    filesToDelete.add(Long.valueOf(id));
                } else {
                    currentEntries.add(Long.valueOf(id));
                }
                hashMap = maxAge;
                i = 0;
                i2 = 1;
                i3 = 2;
            }
            HashSet<Long> hashSet = currentFiles;
            Iterator it = currentFiles.iterator();
            while (it.hasNext()) {
                long id2 = ((Long) it.next()).longValue();
                if (!currentEntries.contains(Long.valueOf(id2))) {
                    filesToDelete.add(Long.valueOf(id2));
                }
            }
            if (!entriesToDelete.isEmpty()) {
                StringBuilder query = new StringBuilder(String.format(Locale.US, "DELETE FROM %s WHERE %s IN (", new Object[]{TABLE, "id"}));
                query.append(entriesToDelete.remove(entriesToDelete.size() - 1));
                Iterator it2 = entriesToDelete.iterator();
                while (true) {
                    if (!it2.hasNext()) {
                        break;
                    }
                    long id3 = ((Long) it2.next()).longValue();
                    query.append(",");
                    query.append(id3);
                    if (query.length() > 524288) {
                        break;
                    }
                }
                query.append(')');
                db.execSQL(query.toString());
            }
            cursor.close();
        }
        return filesToDelete;
    }

    public synchronized void emptyTheWholeCache() {
        getWritableDatabase().execSQL(String.format(Locale.US, "DELETE FROM %s", new Object[]{TABLE}));
    }
}
