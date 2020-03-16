package org.quantumbadger.redreader.cache;

import android.os.Process;

final class CacheDownloadThread extends Thread {
    private final CacheDownload singleDownload;

    public CacheDownloadThread(CacheDownload singleDownload2, boolean start, String name) {
        super(name);
        this.singleDownload = singleDownload2;
        if (start) {
            start();
        }
    }

    public void run() {
        Process.setThreadPriority(10);
        this.singleDownload.doDownload();
    }
}
