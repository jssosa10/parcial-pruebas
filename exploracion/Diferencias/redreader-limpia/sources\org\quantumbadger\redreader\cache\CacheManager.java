package org.quantumbadger.redreader.cache;

import android.content.Context;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.cache.NotifyOutputStream.Listener;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.PrioritisedCachedThreadPool;
import org.quantumbadger.redreader.common.PrioritisedCachedThreadPool.Task;
import org.quantumbadger.redreader.jsonwrap.JsonValue;

public final class CacheManager {
    private static final String ext = ".rr_cache_data";
    private static final AtomicBoolean isAlreadyInitialized = new AtomicBoolean(false);
    private static CacheManager singleton = null;
    private static final String tempExt = ".rr_cache_data_tmp";
    /* access modifiers changed from: private */
    public final Context context;
    /* access modifiers changed from: private */
    public final CacheDbManager dbManager;
    /* access modifiers changed from: private */
    public final PrioritisedDownloadQueue downloadQueue;
    /* access modifiers changed from: private */
    public final PrioritisedCachedThreadPool mDiskCacheThreadPool = new PrioritisedCachedThreadPool(2, "Disk Cache");
    /* access modifiers changed from: private */
    public final PriorityBlockingQueue<CacheRequest> requests = new PriorityBlockingQueue<>();

    public class ReadableCacheFile {
        private final long id;

        private ReadableCacheFile(long id2) {
            this.id = id2;
        }

        public InputStream getInputStream() throws IOException {
            return CacheManager.this.getCacheFileInputStream(this.id);
        }

        public Uri getUri() throws IOException {
            return CacheManager.this.getCacheFileUri(this.id);
        }

        public String toString() {
            return String.format(Locale.US, "[ReadableCacheFile : id %d]", new Object[]{Long.valueOf(this.id)});
        }

        public long getSize() {
            return CacheManager.this.getExistingCacheFile(this.id).length();
        }
    }

    private class RequestHandlerThread extends Thread {
        public RequestHandlerThread() {
            super("Request Handler Thread");
        }

        public void run() {
            Process.setThreadPriority(10);
            while (true) {
                try {
                    CacheRequest cacheRequest = (CacheRequest) CacheManager.this.requests.take();
                    CacheRequest request = cacheRequest;
                    if (cacheRequest != null) {
                        handleRequest(request);
                    } else {
                        return;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void handleRequest(CacheRequest request) {
            if (request.url == null) {
                request.notifyFailure(5, new NullPointerException("URL was null"), null, "URL was null");
                return;
            }
            if (request.downloadStrategy.shouldDownloadWithoutCheckingCache()) {
                queueDownload(request);
            } else {
                LinkedList<CacheEntry> result = CacheManager.this.dbManager.select(request.url, request.user.username, request.requestSession);
                if (!result.isEmpty()) {
                    CacheEntry entry = mostRecentFromList(result);
                    if (request.downloadStrategy.shouldDownloadIfCacheEntryFound(entry)) {
                        queueDownload(request);
                    } else {
                        handleCacheEntryFound(entry, request);
                    }
                } else if (request.downloadStrategy.shouldDownloadIfNotCached()) {
                    queueDownload(request);
                } else {
                    request.notifyFailure(3, null, null, "Could not find this data in the cache");
                }
            }
        }

        private CacheEntry mostRecentFromList(LinkedList<CacheEntry> list) {
            CacheEntry entry = null;
            Iterator it = list.iterator();
            while (it.hasNext()) {
                CacheEntry e = (CacheEntry) it.next();
                if (entry == null || entry.timestamp < e.timestamp) {
                    entry = e;
                }
            }
            return entry;
        }

        private void queueDownload(CacheRequest request) {
            request.notifyDownloadNecessary();
            try {
                CacheManager.this.downloadQueue.add(request, CacheManager.this);
            } catch (Exception e) {
                request.notifyFailure(5, e, null, e.toString());
            }
        }

        private void handleCacheEntryFound(final CacheEntry entry, final CacheRequest request) {
            if (CacheManager.this.getExistingCacheFile(entry.id) == null) {
                request.notifyFailure(2, null, null, "A cache entry was found in the database, but the actual data couldn't be found. Press refresh to download the content again.");
                CacheManager.this.dbManager.delete(entry.id);
                return;
            }
            CacheManager.this.mDiskCacheThreadPool.add(new Task() {
                public int getPrimaryPriority() {
                    return request.priority;
                }

                public int getSecondaryPriority() {
                    return request.listId;
                }

                public void run() {
                    if (request.isJson) {
                        InputStream cacheFileInputStream = null;
                        try {
                            InputStream cacheFileInputStream2 = CacheManager.this.getCacheFileInputStream(entry.id);
                            if (cacheFileInputStream2 == null) {
                                request.notifyFailure(3, null, null, "Couldn't retrieve cache file");
                                return;
                            }
                            JsonValue value = new JsonValue(cacheFileInputStream2);
                            request.notifyJsonParseStarted(value, entry.timestamp, entry.session, true);
                            value.buildInThisThread();
                        } catch (Throwable t) {
                            if (cacheFileInputStream != null) {
                                try {
                                    cacheFileInputStream.close();
                                } catch (IOException e) {
                                }
                            }
                            CacheManager.this.dbManager.delete(entry.id);
                            File existingCacheFile = CacheManager.this.getExistingCacheFile(entry.id);
                            if (existingCacheFile != null) {
                                existingCacheFile.delete();
                            }
                            request.notifyFailure(6, t, null, "Error parsing the JSON stream");
                            return;
                        }
                    }
                    request.notifySuccess(new ReadableCacheFile(entry.id), entry.timestamp, entry.session, true, entry.mimetype);
                }
            });
        }
    }

    public class WritableCacheFile {
        /* access modifiers changed from: private */
        public long cacheFileId;
        /* access modifiers changed from: private */
        public final File location;
        private final NotifyOutputStream os;
        /* access modifiers changed from: private */
        public ReadableCacheFile readableCacheFile;
        private final CacheRequest request;

        private WritableCacheFile(CacheRequest request2, UUID session, String mimetype) throws IOException {
            this.cacheFileId = -1;
            this.readableCacheFile = null;
            this.request = request2;
            this.location = CacheManager.this.getPreferredCacheLocation();
            File file = this.location;
            StringBuilder sb = new StringBuilder();
            sb.append(UUID.randomUUID().toString());
            sb.append(CacheManager.tempExt);
            File tmpFile = new File(file, sb.toString());
            OutputStream bufferedOs = new BufferedOutputStream(new FileOutputStream(tmpFile), 65536);
            final CacheManager cacheManager = CacheManager.this;
            final CacheRequest cacheRequest = request2;
            final UUID uuid = session;
            final String str = mimetype;
            final File file2 = tmpFile;
            AnonymousClass1 r3 = new Listener() {
                public void onClose() throws IOException {
                    WritableCacheFile writableCacheFile = WritableCacheFile.this;
                    writableCacheFile.cacheFileId = CacheManager.this.dbManager.newEntry(cacheRequest, uuid, str);
                    File access$200 = WritableCacheFile.this.location;
                    StringBuilder sb = new StringBuilder();
                    sb.append(WritableCacheFile.this.cacheFileId);
                    sb.append(CacheManager.ext);
                    General.moveFile(file2, new File(access$200, sb.toString()));
                    CacheManager.this.dbManager.setEntryDone(WritableCacheFile.this.cacheFileId);
                    WritableCacheFile writableCacheFile2 = WritableCacheFile.this;
                    writableCacheFile2.readableCacheFile = new ReadableCacheFile(WritableCacheFile.this.cacheFileId);
                }
            };
            this.os = new NotifyOutputStream(bufferedOs, r3);
        }

        public NotifyOutputStream getOutputStream() {
            return this.os;
        }

        public ReadableCacheFile getReadableCacheFile() throws IOException {
            if (this.readableCacheFile == null) {
                if (!this.request.isJson) {
                    BugReportActivity.handleGlobalError(CacheManager.this.context, "Attempt to read cache file before closing");
                }
                try {
                    this.os.flush();
                    this.os.close();
                } catch (IOException e) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Error closing ");
                    sb.append(this.cacheFileId);
                    Log.e("getReadableCacheFile", sb.toString());
                    throw e;
                }
            }
            return this.readableCacheFile;
        }
    }

    public static synchronized CacheManager getInstance(Context context2) {
        CacheManager cacheManager;
        synchronized (CacheManager.class) {
            if (singleton == null) {
                singleton = new CacheManager(context2.getApplicationContext());
            }
            cacheManager = singleton;
        }
        return cacheManager;
    }

    private CacheManager(Context context2) {
        if (isAlreadyInitialized.compareAndSet(false, true)) {
            this.context = context2;
            this.dbManager = new CacheDbManager(context2);
            this.downloadQueue = new PrioritisedDownloadQueue(context2);
            new RequestHandlerThread().start();
            return;
        }
        throw new RuntimeException("Attempt to initialize the cache twice.");
    }

    private Long isCacheFile(String file) {
        if (!file.endsWith(ext)) {
            return null;
        }
        String[] fileSplit = file.split("\\.");
        if (fileSplit.length != 2) {
            return null;
        }
        try {
            return Long.valueOf(Long.parseLong(fileSplit[0]));
        } catch (Exception e) {
            return null;
        }
    }

    private void getCacheFileList(File dir, HashSet<Long> currentFiles) {
        String[] list = dir.list();
        if (list != null) {
            for (String file : list) {
                Long cacheFileId = isCacheFile(file);
                if (cacheFileId != null) {
                    currentFiles.add(cacheFileId);
                }
            }
        }
    }

    private static void pruneTemp(File dir) {
        String[] list = dir.list();
        if (list != null) {
            for (String file : list) {
                if (file.endsWith(tempExt)) {
                    new File(dir, file).delete();
                }
            }
        }
    }

    public static List<File> getCacheDirs(Context context2) {
        File[] externalCacheDirs;
        ArrayList<File> dirs = new ArrayList<>();
        dirs.add(context2.getCacheDir());
        if (VERSION.SDK_INT >= 19) {
            for (File dir : context2.getExternalCacheDirs()) {
                if (dir != null) {
                    dirs.add(dir);
                }
            }
        } else {
            File extDir = context2.getExternalCacheDir();
            if (extDir != null) {
                dirs.add(extDir);
            }
        }
        return dirs;
    }

    public void pruneTemp() {
        for (File dir : getCacheDirs(this.context)) {
            pruneTemp(dir);
        }
    }

    public synchronized void pruneCache() {
        try {
            HashSet<Long> currentFiles = new HashSet<>(128);
            for (File dir : getCacheDirs(this.context)) {
                getCacheFileList(dir, currentFiles);
            }
            ArrayList<Long> filesToDelete = this.dbManager.getFilesToPrune(currentFiles, PrefsUtility.pref_cache_maxage(this.context, PreferenceManager.getDefaultSharedPreferences(this.context)), 72);
            StringBuilder sb = new StringBuilder();
            sb.append("Pruning ");
            sb.append(filesToDelete.size());
            sb.append(" files");
            Log.i("CacheManager", sb.toString());
            Iterator it = filesToDelete.iterator();
            while (it.hasNext()) {
                File file = getExistingCacheFile(((Long) it.next()).longValue());
                if (file != null) {
                    file.delete();
                }
            }
        } catch (Throwable t) {
            BugReportActivity.handleGlobalError(this.context, t);
        }
        return;
    }

    public synchronized void emptyTheWholeCache() {
        this.dbManager.emptyTheWholeCache();
    }

    public void makeRequest(CacheRequest request) {
        this.requests.put(request);
    }

    public LinkedList<CacheEntry> getSessions(URI url, RedditAccount user) {
        return this.dbManager.select(url, user.username, null);
    }

    public File getPreferredCacheLocation() {
        Context context2 = this.context;
        return new File(PrefsUtility.pref_cache_location(context2, PreferenceManager.getDefaultSharedPreferences(context2)));
    }

    public WritableCacheFile openNewCacheFile(CacheRequest request, UUID session, String mimetype) throws IOException {
        WritableCacheFile writableCacheFile = new WritableCacheFile(request, session, mimetype);
        return writableCacheFile;
    }

    /* access modifiers changed from: private */
    public File getExistingCacheFile(long id) {
        for (File dir : getCacheDirs(this.context)) {
            StringBuilder sb = new StringBuilder();
            sb.append(id);
            sb.append(ext);
            File f = new File(dir, sb.toString());
            if (f.exists()) {
                return f;
            }
        }
        return null;
    }

    /* access modifiers changed from: private */
    public InputStream getCacheFileInputStream(long id) throws IOException {
        File cacheFile = getExistingCacheFile(id);
        if (cacheFile == null) {
            return null;
        }
        return new BufferedInputStream(new FileInputStream(cacheFile), 8192);
    }

    /* access modifiers changed from: private */
    public Uri getCacheFileUri(long id) throws IOException {
        File cacheFile = getExistingCacheFile(id);
        if (cacheFile == null) {
            return null;
        }
        return Uri.fromFile(cacheFile);
    }
}
