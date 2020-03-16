package org.quantumbadger.redreader.common;

import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;

public class PrioritisedCachedThreadPool {
    private final Executor mExecutor = new Executor();
    /* access modifiers changed from: private */
    public int mIdleThreads;
    private final int mMaxThreads;
    /* access modifiers changed from: private */
    public int mRunningThreads;
    /* access modifiers changed from: private */
    public final ArrayList<Task> mTasks = new ArrayList<>(16);
    private final String mThreadName;
    private int mThreadNameCount = 0;

    private final class Executor implements Runnable {
        static final /* synthetic */ boolean $assertionsDisabled = false;

        static {
            Class<PrioritisedCachedThreadPool> cls = PrioritisedCachedThreadPool.class;
        }

        private Executor() {
        }

        public void run() {
            while (true) {
                Task taskToRun = null;
                synchronized (PrioritisedCachedThreadPool.this.mTasks) {
                    if (PrioritisedCachedThreadPool.this.mTasks.isEmpty()) {
                        PrioritisedCachedThreadPool.this.mIdleThreads = PrioritisedCachedThreadPool.this.mIdleThreads + 1;
                        try {
                            PrioritisedCachedThreadPool.this.mTasks.wait(30000);
                            PrioritisedCachedThreadPool.this.mIdleThreads = PrioritisedCachedThreadPool.this.mIdleThreads - 1;
                            if (PrioritisedCachedThreadPool.this.mTasks.isEmpty()) {
                                PrioritisedCachedThreadPool.this.mRunningThreads = PrioritisedCachedThreadPool.this.mRunningThreads - 1;
                                return;
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (Throwable th) {
                            PrioritisedCachedThreadPool.this.mIdleThreads = PrioritisedCachedThreadPool.this.mIdleThreads - 1;
                            throw th;
                        }
                    }
                    int taskIndex = -1;
                    for (int i = 0; i < PrioritisedCachedThreadPool.this.mTasks.size(); i++) {
                        if (taskToRun == null || ((Task) PrioritisedCachedThreadPool.this.mTasks.get(i)).isHigherPriorityThan(taskToRun)) {
                            taskToRun = (Task) PrioritisedCachedThreadPool.this.mTasks.get(i);
                            taskIndex = i;
                        }
                    }
                    PrioritisedCachedThreadPool.this.mTasks.remove(taskIndex);
                }
                taskToRun.run();
            }
        }
    }

    public static abstract class Task {
        public abstract int getPrimaryPriority();

        public abstract int getSecondaryPriority();

        public abstract void run();

        public boolean isHigherPriorityThan(Task o) {
            return getPrimaryPriority() < o.getPrimaryPriority() || getSecondaryPriority() < o.getSecondaryPriority();
        }
    }

    public PrioritisedCachedThreadPool(int threads, String threadName) {
        this.mMaxThreads = threads;
        this.mThreadName = threadName;
    }

    public void add(Task task) {
        synchronized (this.mTasks) {
            this.mTasks.add(task);
            this.mTasks.notifyAll();
            if (this.mIdleThreads < 1 && this.mRunningThreads < this.mMaxThreads) {
                this.mRunningThreads++;
                Executor executor = this.mExecutor;
                StringBuilder sb = new StringBuilder();
                sb.append(this.mThreadName);
                sb.append(StringUtils.SPACE);
                int i = this.mThreadNameCount;
                this.mThreadNameCount = i + 1;
                sb.append(i);
                new Thread(executor, sb.toString()).start();
            }
        }
    }
}
