package org.quantumbadger.redreader.common;

public class TriggerableThread {
    private boolean allowRetrigger = false;
    /* access modifiers changed from: private */
    public final long initialDelay;
    private boolean shouldRetrigger = false;
    /* access modifiers changed from: private */
    public final Runnable task;
    private InternalTriggerableThread thread;

    private final class InternalTriggerableThread extends Thread {
        private InternalTriggerableThread() {
        }

        public void run() {
            do {
                try {
                    Thread.sleep(TriggerableThread.this.initialDelay);
                    TriggerableThread.this.onSleepEnd();
                    TriggerableThread.this.task.run();
                } catch (InterruptedException e) {
                    throw new UnexpectedInternalStateException();
                }
            } while (TriggerableThread.this.shouldThreadContinue());
        }
    }

    public TriggerableThread(Runnable task2, long initialDelay2) {
        this.task = task2;
        this.initialDelay = initialDelay2;
    }

    public synchronized void trigger() {
        if (this.thread == null) {
            this.thread = new InternalTriggerableThread();
            this.thread.start();
        } else if (this.allowRetrigger) {
            this.shouldRetrigger = true;
        }
    }

    /* access modifiers changed from: private */
    public synchronized void onSleepEnd() {
        this.allowRetrigger = true;
    }

    /* access modifiers changed from: private */
    public synchronized boolean shouldThreadContinue() {
        if (this.shouldRetrigger) {
            this.shouldRetrigger = false;
            return true;
        }
        this.thread = null;
        this.allowRetrigger = false;
        return false;
    }
}
