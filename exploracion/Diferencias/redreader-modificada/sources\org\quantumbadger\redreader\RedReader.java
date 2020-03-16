package org.quantumbadger.redreader;

import android.app.Application;
import android.os.Environment;
import android.os.Process;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.UUID;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.common.Alarms;
import org.quantumbadger.redreader.io.RedditChangeDataIO;
import org.quantumbadger.redreader.receivers.NewMessageChecker;
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager;

public class RedReader extends Application {
    public void onCreate() {
        super.onCreate();
        Log.i("RedReader", "Application created.");
        final UncaughtExceptionHandler androidHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable t) {
                try {
                    t.printStackTrace();
                    File dir = Environment.getExternalStorageDirectory();
                    if (dir == null) {
                        dir = Environment.getDataDirectory();
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("redreader_crash_log_");
                    sb.append(UUID.randomUUID().toString());
                    sb.append(".txt");
                    PrintWriter pw = new PrintWriter(new FileOutputStream(new File(dir, sb.toString())));
                    t.printStackTrace(pw);
                    pw.flush();
                    pw.close();
                } catch (Throwable th) {
                }
                androidHandler.uncaughtException(thread, t);
            }
        });
        final CacheManager cm = CacheManager.getInstance(this);
        new Thread() {
            public void run() {
                Process.setThreadPriority(10);
                cm.pruneTemp();
                cm.pruneCache();
            }
        }.start();
        new Thread() {
            public void run() {
                RedditChangeDataIO.getInstance(RedReader.this).runInitialReadInThisThread();
                RedditChangeDataManager.pruneAllUsers(RedReader.this);
            }
        }.start();
        Alarms.onBoot(this);
        NewMessageChecker.checkForNewMessages(this);
    }
}
