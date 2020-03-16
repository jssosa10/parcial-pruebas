package org.quantumbadger.redreader.cache;

import android.content.Context;
import java.util.HashSet;
import java.util.Iterator;
import org.quantumbadger.redreader.common.PrioritisedCachedThreadPool;

class PrioritisedDownloadQueue {
    private final PrioritisedCachedThreadPool mDownloadThreadPool = new PrioritisedCachedThreadPool(5, "Download");
    private final HashSet<CacheDownload> redditDownloadsQueued = new HashSet<>();

    private class RedditQueueProcessor extends Thread {
        public RedditQueueProcessor() {
            super("Reddit Queue Processor");
        }

        public void run() {
            while (true) {
                synchronized (this) {
                    new CacheDownloadThread(PrioritisedDownloadQueue.this.getNextRedditInQueue(), true, "Cache Download Thread: Reddit");
                }
                try {
                    sleep(1200);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public PrioritisedDownloadQueue(Context context) {
        new RedditQueueProcessor().start();
    }

    public synchronized void add(CacheRequest request, CacheManager manager) {
        CacheDownload download = new CacheDownload(request, manager, this);
        if (request.queueType == 0) {
            this.redditDownloadsQueued.add(download);
            notifyAll();
        } else {
            if (request.queueType != 2) {
                if (request.queueType != 1) {
                    this.mDownloadThreadPool.add(download);
                }
            }
            new CacheDownloadThread(download, true, "Cache Download Thread: Immediate");
        }
    }

    /* access modifiers changed from: private */
    public synchronized CacheDownload getNextRedditInQueue() {
        CacheDownload next;
        while (this.redditDownloadsQueued.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        next = null;
        Iterator it = this.redditDownloadsQueued.iterator();
        while (it.hasNext()) {
            CacheDownload entry = (CacheDownload) it.next();
            if (next == null || entry.isHigherPriorityThan(next)) {
                next = entry;
            }
        }
        this.redditDownloadsQueued.remove(next);
        return next;
    }
}
