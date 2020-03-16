package org.apache.commons.lang3.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class Memoizer<I, O> implements Computable<I, O> {
    private final ConcurrentMap<I, Future<O>> cache;
    /* access modifiers changed from: private */
    public final Computable<I, O> computable;
    private final boolean recalculate;

    public Memoizer(Computable<I, O> computable2) {
        this(computable2, false);
    }

    public Memoizer(Computable<I, O> computable2, boolean recalculate2) {
        this.cache = new ConcurrentHashMap();
        this.computable = computable2;
        this.recalculate = recalculate2;
    }

    public O compute(final I arg) throws InterruptedException {
        while (true) {
            Future future = (Future) this.cache.get(arg);
            if (future == 0) {
                FutureTask futureTask = new FutureTask(new Callable<O>() {
                    public O call() throws InterruptedException {
                        return Memoizer.this.computable.compute(arg);
                    }
                });
                future = (Future) this.cache.putIfAbsent(arg, futureTask);
                if (future == 0) {
                    future = futureTask;
                    futureTask.run();
                }
            }
            try {
                return future.get();
            } catch (CancellationException e) {
                this.cache.remove(arg, future);
            } catch (ExecutionException e2) {
                if (this.recalculate) {
                    this.cache.remove(arg, future);
                }
                throw launderException(e2.getCause());
            }
        }
    }

    private RuntimeException launderException(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            return (RuntimeException) throwable;
        }
        if (throwable instanceof Error) {
            throw ((Error) throwable);
        }
        throw new IllegalStateException("Unchecked exception", throwable);
    }
}
