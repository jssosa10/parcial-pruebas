package org.quantumbadger.redreader.reddit.prepared;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.lang3.time.DateUtils;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.collections.WeakReferenceListHashMapManager;
import org.quantumbadger.redreader.common.collections.WeakReferenceListManager.ArgOperator;
import org.quantumbadger.redreader.io.ExtendedDataInputStream;
import org.quantumbadger.redreader.io.ExtendedDataOutputStream;
import org.quantumbadger.redreader.io.RedditChangeDataIO;
import org.quantumbadger.redreader.reddit.things.RedditComment;
import org.quantumbadger.redreader.reddit.things.RedditPost;
import org.quantumbadger.redreader.reddit.things.RedditThingWithIdAndType;

public final class RedditChangeDataManager {
    private static final HashMap<RedditAccount, RedditChangeDataManager> INSTANCE_MAP = new HashMap<>();
    private static final int MAX_ENTRY_COUNT = 10000;
    private static final String TAG = "RedditChangeDataManager";
    private final HashMap<String, Entry> mEntries = new HashMap<>();
    private final WeakReferenceListHashMapManager<String, Listener> mListeners = new WeakReferenceListHashMapManager<>();
    private final Object mLock = new Object();

    private static final class Entry {
        static final Entry CLEAR_ENTRY = new Entry();
        private final boolean mIsDownvoted;
        private final Boolean mIsHidden;
        private final boolean mIsRead;
        private final boolean mIsSaved;
        private final boolean mIsUpvoted;
        /* access modifiers changed from: private */
        public final long mTimestamp;

        private Entry() {
            this.mTimestamp = Long.MIN_VALUE;
            this.mIsUpvoted = false;
            this.mIsDownvoted = false;
            this.mIsRead = false;
            this.mIsSaved = false;
            this.mIsHidden = null;
        }

        private Entry(long timestamp, boolean isUpvoted, boolean isDownvoted, boolean isRead, boolean isSaved, Boolean isHidden) {
            this.mTimestamp = timestamp;
            this.mIsUpvoted = isUpvoted;
            this.mIsDownvoted = isDownvoted;
            this.mIsRead = isRead;
            this.mIsSaved = isSaved;
            this.mIsHidden = isHidden;
        }

        private Entry(ExtendedDataInputStream dis) throws IOException {
            this.mTimestamp = dis.readLong();
            this.mIsUpvoted = dis.readBoolean();
            this.mIsDownvoted = dis.readBoolean();
            this.mIsRead = dis.readBoolean();
            this.mIsSaved = dis.readBoolean();
            this.mIsHidden = dis.readNullableBoolean();
        }

        /* access modifiers changed from: private */
        public void writeTo(ExtendedDataOutputStream dos) throws IOException {
            dos.writeLong(this.mTimestamp);
            dos.writeBoolean(this.mIsUpvoted);
            dos.writeBoolean(this.mIsDownvoted);
            dos.writeBoolean(this.mIsRead);
            dos.writeBoolean(this.mIsSaved);
            dos.writeNullableBoolean(this.mIsHidden);
        }

        /* access modifiers changed from: 0000 */
        public boolean isClear() {
            return !this.mIsUpvoted && !this.mIsDownvoted && !this.mIsRead && !this.mIsSaved && this.mIsHidden == null;
        }

        public boolean isUpvoted() {
            return this.mIsUpvoted;
        }

        public boolean isSaved() {
            return this.mIsSaved;
        }

        public boolean isRead() {
            return this.mIsRead;
        }

        public Boolean isHidden() {
            return this.mIsHidden;
        }

        public boolean isDownvoted() {
            return this.mIsDownvoted;
        }

        /* access modifiers changed from: 0000 */
        public Entry update(long timestamp, RedditComment comment) {
            if (timestamp < this.mTimestamp) {
                return this;
            }
            Entry entry = new Entry(timestamp, Boolean.TRUE.equals(comment.likes), Boolean.FALSE.equals(comment.likes), false, Boolean.TRUE.equals(comment.saved), this.mIsHidden);
            return entry;
        }

        /* access modifiers changed from: 0000 */
        public Entry update(long timestamp, RedditPost post) {
            Boolean bool;
            if (timestamp < this.mTimestamp) {
                return this;
            }
            boolean equals = Boolean.TRUE.equals(post.likes);
            boolean equals2 = Boolean.FALSE.equals(post.likes);
            boolean z = post.clicked || this.mIsRead;
            boolean z2 = post.saved;
            if (post.hidden) {
                bool = Boolean.valueOf(true);
            } else {
                bool = null;
            }
            Entry entry = new Entry(timestamp, equals, equals2, z, z2, bool);
            return entry;
        }

        /* access modifiers changed from: 0000 */
        public Entry markUpvoted(long timestamp) {
            Entry entry = new Entry(timestamp, true, false, this.mIsRead, this.mIsSaved, this.mIsHidden);
            return entry;
        }

        /* access modifiers changed from: 0000 */
        public Entry markDownvoted(long timestamp) {
            Entry entry = new Entry(timestamp, false, true, this.mIsRead, this.mIsSaved, this.mIsHidden);
            return entry;
        }

        /* access modifiers changed from: 0000 */
        public Entry markUnvoted(long timestamp) {
            Entry entry = new Entry(timestamp, false, false, this.mIsRead, this.mIsSaved, this.mIsHidden);
            return entry;
        }

        /* access modifiers changed from: 0000 */
        public Entry markRead(long timestamp) {
            Entry entry = new Entry(timestamp, this.mIsUpvoted, this.mIsDownvoted, true, this.mIsSaved, this.mIsHidden);
            return entry;
        }

        /* access modifiers changed from: 0000 */
        public Entry markSaved(long timestamp, boolean isSaved) {
            Entry entry = new Entry(timestamp, this.mIsUpvoted, this.mIsDownvoted, this.mIsRead, isSaved, this.mIsHidden);
            return entry;
        }

        /* access modifiers changed from: 0000 */
        public Entry markHidden(long timestamp, Boolean isHidden) {
            Entry entry = new Entry(timestamp, this.mIsUpvoted, this.mIsDownvoted, this.mIsRead, this.mIsSaved, isHidden);
            return entry;
        }
    }

    public interface Listener {
        void onRedditDataChange(String str);
    }

    private static final class ListenerNotifyOperator implements ArgOperator<Listener, String> {
        public static final ListenerNotifyOperator INSTANCE = new ListenerNotifyOperator();

        private ListenerNotifyOperator() {
        }

        public void operate(Listener listener, String arg) {
            listener.onRedditDataChange(arg);
        }
    }

    public static RedditChangeDataManager getInstance(RedditAccount user) {
        RedditChangeDataManager result;
        synchronized (INSTANCE_MAP) {
            result = (RedditChangeDataManager) INSTANCE_MAP.get(user);
            if (result == null) {
                result = new RedditChangeDataManager();
                INSTANCE_MAP.put(user, result);
            }
        }
        return result;
    }

    private static HashMap<RedditAccount, HashMap<String, Entry>> snapshotAllUsers() {
        HashMap<RedditAccount, HashMap<String, Entry>> result = new HashMap<>();
        synchronized (INSTANCE_MAP) {
            for (RedditAccount account : INSTANCE_MAP.keySet()) {
                result.put(account, getInstance(account).snapshot());
            }
        }
        return result;
    }

    public static void writeAllUsers(ExtendedDataOutputStream dos) throws IOException {
        Log.i(TAG, "Taking snapshot...");
        HashMap<RedditAccount, HashMap<String, Entry>> data = snapshotAllUsers();
        Log.i(TAG, "Writing to stream...");
        Set<java.util.Map.Entry<RedditAccount, HashMap<String, Entry>>> userDataSet = data.entrySet();
        dos.writeInt(userDataSet.size());
        for (java.util.Map.Entry<RedditAccount, HashMap<String, Entry>> userData : userDataSet) {
            String username = ((RedditAccount) userData.getKey()).getCanonicalUsername();
            dos.writeUTF(username);
            Set<java.util.Map.Entry<String, Entry>> entrySet = ((HashMap) userData.getValue()).entrySet();
            dos.writeInt(entrySet.size());
            for (java.util.Map.Entry<String, Entry> entry : entrySet) {
                dos.writeUTF((String) entry.getKey());
                ((Entry) entry.getValue()).writeTo(dos);
            }
            Log.i(TAG, String.format(Locale.US, "Wrote %d entries for user '%s'", new Object[]{Integer.valueOf(entrySet.size()), username}));
        }
        Log.i(TAG, "All entries written to stream.");
    }

    public static void readAllUsers(ExtendedDataInputStream dis, Context context) throws IOException {
        Log.i(TAG, "Reading from stream...");
        int userCount = dis.readInt();
        String str = TAG;
        StringBuilder sb = new StringBuilder();
        sb.append(userCount);
        sb.append(" users to read.");
        Log.i(str, sb.toString());
        for (int i = 0; i < userCount; i++) {
            String username = dis.readUTF();
            int entryCount = dis.readInt();
            Log.i(TAG, String.format(Locale.US, "Reading %d entries for user '%s'", new Object[]{Integer.valueOf(entryCount), username}));
            HashMap<String, Entry> entries = new HashMap<>(entryCount);
            for (int j = 0; j < entryCount; j++) {
                entries.put(dis.readUTF(), new Entry(dis));
            }
            Log.i(TAG, "Getting account...");
            RedditAccount account = RedditAccountManager.getInstance(context).getAccount(username);
            if (account == null) {
                Log.i(TAG, String.format(Locale.US, "Skipping user '%s' as the account no longer exists", new Object[]{username}));
            } else {
                getInstance(account).insertAll(entries);
                Log.i(TAG, String.format(Locale.US, "Finished inserting entries for user '%s'", new Object[]{username}));
            }
        }
        Log.i(TAG, "All entries read from stream.");
    }

    public static void pruneAllUsers(Context context) {
        Set<RedditAccount> users;
        Log.i(TAG, "Pruning for all users...");
        synchronized (INSTANCE_MAP) {
            users = new HashSet<>(INSTANCE_MAP.keySet());
        }
        for (RedditAccount user : users) {
            getInstance(user).prune(context);
        }
        Log.i(TAG, "Pruning complete.");
    }

    public void addListener(RedditThingWithIdAndType thing, Listener listener) {
        this.mListeners.add(thing.getIdAndType(), listener);
    }

    public void removeListener(RedditThingWithIdAndType thing, Listener listener) {
        this.mListeners.remove(thing.getIdAndType(), listener);
    }

    private Entry get(RedditThingWithIdAndType thing) {
        Entry entry = (Entry) this.mEntries.get(thing.getIdAndType());
        if (entry == null) {
            return Entry.CLEAR_ENTRY;
        }
        return entry;
    }

    private void set(RedditThingWithIdAndType thing, Entry existingValue, Entry newValue) {
        if (!newValue.isClear()) {
            this.mEntries.put(thing.getIdAndType(), newValue);
            RedditChangeDataIO.notifyUpdateStatic();
        } else if (!existingValue.isClear()) {
            this.mEntries.remove(thing.getIdAndType());
            RedditChangeDataIO.notifyUpdateStatic();
        }
        this.mListeners.map(thing.getIdAndType(), ListenerNotifyOperator.INSTANCE, thing.getIdAndType());
    }

    private void insertAll(HashMap<String, Entry> entries) {
        synchronized (this.mLock) {
            for (java.util.Map.Entry<String, Entry> entry : entries.entrySet()) {
                Entry newEntry = (Entry) entry.getValue();
                Entry existingEntry = (Entry) this.mEntries.get(entry.getKey());
                if (existingEntry == null || existingEntry.mTimestamp < newEntry.mTimestamp) {
                    this.mEntries.put(entry.getKey(), newEntry);
                }
            }
        }
        for (String idAndType : entries.keySet()) {
            this.mListeners.map(idAndType, ListenerNotifyOperator.INSTANCE, idAndType);
        }
    }

    public void update(long timestamp, RedditComment comment) {
        synchronized (this.mLock) {
            Entry existingEntry = get(comment);
            set(comment, existingEntry, existingEntry.update(timestamp, comment));
        }
    }

    public void update(long timestamp, RedditPost post) {
        synchronized (this.mLock) {
            Entry existingEntry = get(post);
            set(post, existingEntry, existingEntry.update(timestamp, post));
        }
    }

    public void markUpvoted(long timestamp, RedditThingWithIdAndType thing) {
        synchronized (this.mLock) {
            Entry existingEntry = get(thing);
            set(thing, existingEntry, existingEntry.markUpvoted(timestamp));
        }
    }

    public void markDownvoted(long timestamp, RedditThingWithIdAndType thing) {
        synchronized (this.mLock) {
            Entry existingEntry = get(thing);
            set(thing, existingEntry, existingEntry.markDownvoted(timestamp));
        }
    }

    public void markUnvoted(long timestamp, RedditThingWithIdAndType thing) {
        synchronized (this.mLock) {
            Entry existingEntry = get(thing);
            set(thing, existingEntry, existingEntry.markUnvoted(timestamp));
        }
    }

    public void markSaved(long timestamp, RedditThingWithIdAndType thing, boolean saved) {
        synchronized (this.mLock) {
            Entry existingEntry = get(thing);
            set(thing, existingEntry, existingEntry.markSaved(timestamp, saved));
        }
    }

    public void markHidden(long timestamp, RedditThingWithIdAndType thing, Boolean hidden) {
        synchronized (this.mLock) {
            Entry existingEntry = get(thing);
            set(thing, existingEntry, existingEntry.markHidden(timestamp, hidden));
        }
    }

    public void markRead(long timestamp, RedditThingWithIdAndType thing) {
        synchronized (this.mLock) {
            Entry existingEntry = get(thing);
            set(thing, existingEntry, existingEntry.markRead(timestamp));
        }
    }

    public boolean isUpvoted(RedditThingWithIdAndType thing) {
        boolean isUpvoted;
        synchronized (this.mLock) {
            isUpvoted = get(thing).isUpvoted();
        }
        return isUpvoted;
    }

    public boolean isDownvoted(RedditThingWithIdAndType thing) {
        boolean isDownvoted;
        synchronized (this.mLock) {
            isDownvoted = get(thing).isDownvoted();
        }
        return isDownvoted;
    }

    public boolean isRead(RedditThingWithIdAndType thing) {
        boolean isRead;
        synchronized (this.mLock) {
            isRead = get(thing).isRead();
        }
        return isRead;
    }

    public boolean isSaved(RedditThingWithIdAndType thing) {
        boolean isSaved;
        synchronized (this.mLock) {
            isSaved = get(thing).isSaved();
        }
        return isSaved;
    }

    public Boolean isHidden(RedditThingWithIdAndType thing) {
        Boolean isHidden;
        synchronized (this.mLock) {
            isHidden = get(thing).isHidden();
        }
        return isHidden;
    }

    private HashMap<String, Entry> snapshot() {
        HashMap<String, Entry> hashMap;
        synchronized (this.mLock) {
            hashMap = new HashMap<>(this.mEntries);
        }
        return hashMap;
    }

    private void prune(Context context) {
        long now = System.currentTimeMillis();
        long timestampBoundary = now - PrefsUtility.pref_cache_maxage_entry(context, PreferenceManager.getDefaultSharedPreferences(context)).longValue();
        synchronized (this.mLock) {
            Iterator<java.util.Map.Entry<String, Entry>> iterator = this.mEntries.entrySet().iterator();
            SortedMap<Long, String> byTimestamp = new TreeMap<>();
            while (iterator.hasNext()) {
                java.util.Map.Entry<String, Entry> entry = (java.util.Map.Entry) iterator.next();
                long timestamp = ((Entry) entry.getValue()).mTimestamp;
                byTimestamp.put(Long.valueOf(timestamp), entry.getKey());
                if (timestamp < timestampBoundary) {
                    Log.i(TAG, String.format("Pruning '%s' (%d hours old)", new Object[]{entry.getKey(), Long.valueOf((now - timestamp) / DateUtils.MILLIS_PER_HOUR)}));
                    iterator.remove();
                }
            }
            Iterator<java.util.Map.Entry<Long, String>> iter2 = byTimestamp.entrySet().iterator();
            while (true) {
                if (!iter2.hasNext()) {
                    break;
                } else if (this.mEntries.size() <= 10000) {
                    break;
                } else {
                    java.util.Map.Entry<Long, String> entry2 = (java.util.Map.Entry) iter2.next();
                    Log.i(TAG, String.format("Evicting '%s' (%d hours old)", new Object[]{entry2.getValue(), Long.valueOf((now - ((Long) entry2.getKey()).longValue()) / DateUtils.MILLIS_PER_HOUR)}));
                    this.mEntries.remove(entry2.getValue());
                }
            }
        }
    }
}
