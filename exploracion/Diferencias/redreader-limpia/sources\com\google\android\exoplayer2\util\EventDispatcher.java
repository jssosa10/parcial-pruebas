package com.google.android.exoplayer2.util;

import android.os.Handler;
import com.google.android.exoplayer2.util.EventDispatcher.Event;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public final class EventDispatcher<T> {
    private final CopyOnWriteArrayList<HandlerAndListener<T>> listeners = new CopyOnWriteArrayList<>();

    public interface Event<T> {
        void sendTo(T t);
    }

    private static final class HandlerAndListener<T> {
        private final Handler handler;
        /* access modifiers changed from: private */
        public final T listener;
        private boolean released;

        public HandlerAndListener(Handler handler2, T eventListener) {
            this.handler = handler2;
            this.listener = eventListener;
        }

        public void release() {
            this.released = true;
        }

        public void dispatch(Event<T> event) {
            this.handler.post(new Runnable(event) {
                private final /* synthetic */ Event f$1;

                {
                    this.f$1 = r2;
                }

                public final void run() {
                    HandlerAndListener.lambda$dispatch$0(HandlerAndListener.this, this.f$1);
                }
            });
        }

        public static /* synthetic */ void lambda$dispatch$0(HandlerAndListener handlerAndListener, Event event) {
            if (!handlerAndListener.released) {
                event.sendTo(handlerAndListener.listener);
            }
        }
    }

    public void addListener(Handler handler, T eventListener) {
        Assertions.checkArgument((handler == null || eventListener == null) ? false : true);
        removeListener(eventListener);
        this.listeners.add(new HandlerAndListener(handler, eventListener));
    }

    public void removeListener(T eventListener) {
        Iterator it = this.listeners.iterator();
        while (it.hasNext()) {
            HandlerAndListener<T> handlerAndListener = (HandlerAndListener) it.next();
            if (handlerAndListener.listener == eventListener) {
                handlerAndListener.release();
                this.listeners.remove(handlerAndListener);
            }
        }
    }

    public void dispatch(Event<T> event) {
        Iterator it = this.listeners.iterator();
        while (it.hasNext()) {
            ((HandlerAndListener) it.next()).dispatch(event);
        }
    }
}
