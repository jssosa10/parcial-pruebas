package org.quantumbadger.redreader.account;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.common.UpdateNotifier;
import org.quantumbadger.redreader.reddit.api.RedditOAuth.RefreshToken;

public final class RedditAccountManager extends SQLiteOpenHelper {
    private static final String ACCOUNTS_DB_FILENAME = "accounts_oauth2.db";
    private static final int ACCOUNTS_DB_VERSION = 2;
    private static final RedditAccount ANON = new RedditAccount("", null, 10);
    private static final String FIELD_PRIORITY = "priority";
    private static final String FIELD_REFRESH_TOKEN = "refresh_token";
    private static final String FIELD_USERNAME = "username";
    private static final String TABLE = "accounts_oauth2";
    private static RedditAccountManager singleton;
    private List<RedditAccount> accountsCache = null;
    private final Context context;
    private RedditAccount defaultAccountCache = null;
    private final UpdateNotifier<RedditAccountChangeListener> updateNotifier = new UpdateNotifier<RedditAccountChangeListener>() {
        /* access modifiers changed from: protected */
        public void notifyListener(RedditAccountChangeListener listener) {
            listener.onRedditAccountChanged();
        }
    };

    public static synchronized RedditAccountManager getInstance(Context context2) {
        RedditAccountManager redditAccountManager;
        synchronized (RedditAccountManager.class) {
            if (singleton == null) {
                singleton = new RedditAccountManager(context2.getApplicationContext());
            }
            redditAccountManager = singleton;
        }
        return redditAccountManager;
    }

    public static synchronized RedditAccountManager getInstanceOrNull() {
        RedditAccountManager redditAccountManager;
        synchronized (RedditAccountManager.class) {
            redditAccountManager = singleton;
        }
        return redditAccountManager;
    }

    public static RedditAccount getAnon() {
        return ANON;
    }

    private RedditAccountManager(Context context2) {
        super(context2.getApplicationContext(), ACCOUNTS_DB_FILENAME, null, 2);
        this.context = context2;
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format("CREATE TABLE %s (%s TEXT NOT NULL PRIMARY KEY ON CONFLICT REPLACE,%s TEXT,%s INTEGER)", new Object[]{TABLE, FIELD_USERNAME, FIELD_REFRESH_TOKEN, FIELD_PRIORITY}));
        addAccount(getAnon(), db);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL(String.format(Locale.US, "UPDATE %s SET %2$s=TRIM(%2$s) WHERE %2$s <> TRIM(%2$s)", new Object[]{TABLE, FIELD_USERNAME}));
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Invalid accounts DB update: ");
        sb.append(oldVersion);
        sb.append(" to ");
        sb.append(newVersion);
        throw new RuntimeException(sb.toString());
    }

    public synchronized void addAccount(RedditAccount account) {
        addAccount(account, null);
    }

    private synchronized void addAccount(RedditAccount account, SQLiteDatabase inDb) {
        SQLiteDatabase db;
        if (inDb == null) {
            try {
                db = getWritableDatabase();
            } catch (Throwable th) {
                throw th;
            }
        } else {
            db = inDb;
        }
        ContentValues row = new ContentValues();
        row.put(FIELD_USERNAME, account.username);
        if (account.refreshToken == null) {
            row.putNull(FIELD_REFRESH_TOKEN);
        } else {
            row.put(FIELD_REFRESH_TOKEN, account.refreshToken.token);
        }
        row.put(FIELD_PRIORITY, Long.valueOf(account.priority));
        db.insert(TABLE, null, row);
        reloadAccounts(db);
        this.updateNotifier.updateAllListeners();
        if (inDb == null) {
            db.close();
        }
    }

    public synchronized ArrayList<RedditAccount> getAccounts() {
        if (this.accountsCache == null) {
            SQLiteDatabase db = getReadableDatabase();
            reloadAccounts(db);
            db.close();
        }
        return new ArrayList<>(this.accountsCache);
    }

    public RedditAccount getAccount(@NonNull String username) {
        if ("".equals(username)) {
            return getAnon();
        }
        RedditAccount selectedAccount = null;
        Iterator it = getAccounts().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            RedditAccount account = (RedditAccount) it.next();
            if (!account.isAnonymous() && account.username.equalsIgnoreCase(username)) {
                selectedAccount = account;
                break;
            }
        }
        return selectedAccount;
    }

    public synchronized RedditAccount getDefaultAccount() {
        if (this.defaultAccountCache == null) {
            SQLiteDatabase db = getReadableDatabase();
            reloadAccounts(db);
            db.close();
        }
        return this.defaultAccountCache;
    }

    public synchronized void setDefaultAccount(RedditAccount newDefault) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(String.format(Locale.US, "UPDATE %s SET %s=(SELECT MIN(%s)-1 FROM %s) WHERE %s=?", new Object[]{TABLE, FIELD_PRIORITY, FIELD_PRIORITY, TABLE, FIELD_USERNAME}), new String[]{newDefault.username});
        reloadAccounts(db);
        db.close();
        this.updateNotifier.updateAllListeners();
    }

    private synchronized void reloadAccounts(SQLiteDatabase db) {
        RefreshToken refreshToken;
        synchronized (this) {
            SQLiteDatabase sQLiteDatabase = db;
            Cursor cursor = sQLiteDatabase.query(TABLE, new String[]{FIELD_USERNAME, FIELD_REFRESH_TOKEN, FIELD_PRIORITY}, null, null, null, null, "priority ASC");
            this.accountsCache = new LinkedList();
            this.defaultAccountCache = null;
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String username = cursor.getString(0);
                    if (cursor.isNull(1)) {
                        refreshToken = null;
                    } else {
                        refreshToken = new RefreshToken(cursor.getString(1));
                    }
                    RedditAccount account = new RedditAccount(username, refreshToken, cursor.getLong(2));
                    this.accountsCache.add(account);
                    if (this.defaultAccountCache == null || account.priority < this.defaultAccountCache.priority) {
                        this.defaultAccountCache = account;
                    }
                }
                cursor.close();
            } else {
                BugReportActivity.handleGlobalError(this.context, "Cursor was null after query");
            }
        }
    }

    public void addUpdateListener(RedditAccountChangeListener listener) {
        this.updateNotifier.addListener(listener);
    }

    public void deleteAccount(RedditAccount account) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE, "username=?", new String[]{account.username});
        reloadAccounts(db);
        this.updateNotifier.updateAllListeners();
        db.close();
    }
}
