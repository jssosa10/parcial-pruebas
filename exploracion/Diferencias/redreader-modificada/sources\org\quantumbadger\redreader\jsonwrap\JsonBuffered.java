package org.quantumbadger.redreader.jsonwrap;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public abstract class JsonBuffered {
    public static final int STATUS_FAILED = 2;
    public static final int STATUS_LOADED = 1;
    public static final int STATUS_LOADING = 0;
    private Throwable failReason = null;
    private volatile int status = 0;

    @Retention(RetentionPolicy.SOURCE)
    public @interface Status {
    }

    /* access modifiers changed from: protected */
    public abstract void buildBuffered(JsonParser jsonParser) throws IOException;

    /* access modifiers changed from: protected */
    public abstract void prettyPrint(int i, StringBuilder sb) throws InterruptedException, IOException;

    public final int getStatus() {
        return this.status;
    }

    public final synchronized int join() throws InterruptedException {
        while (this.status == 0) {
            wait();
        }
        return this.status;
    }

    private synchronized void setLoaded() {
        this.status = 1;
        notifyAll();
    }

    private synchronized void setFailed(Throwable t) {
        this.status = 2;
        this.failReason = t;
        notifyAll();
    }

    public final Throwable getFailReason() {
        return this.failReason;
    }

    /* access modifiers changed from: protected */
    public final void throwFailReasonException() throws IOException {
        Throwable t = getFailReason();
        if (t instanceof JsonParseException) {
            throw ((JsonParseException) t);
        } else if (t instanceof IOException) {
            throw ((IOException) t);
        } else {
            throw new RuntimeException(t);
        }
    }

    /* access modifiers changed from: protected */
    public final void build(JsonParser jp2) throws IOException {
        try {
            buildBuffered(jp2);
            setLoaded();
        } catch (IOException e) {
            setFailed(e);
            throw e;
        } catch (Throwable t) {
            setFailed(t);
            throw new RuntimeException(t);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        try {
            prettyPrint(0, sb);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        return sb.toString();
    }
}
