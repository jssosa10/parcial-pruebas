package org.quantumbadger.redreader.cache;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NotifyOutputStream extends FilterOutputStream {
    private final Listener listener;

    public interface Listener {
        void onClose() throws IOException;
    }

    public NotifyOutputStream(OutputStream out, Listener listener2) {
        super(out);
        this.listener = listener2;
    }

    public void close() throws IOException {
        super.close();
        this.listener.onClose();
    }
}
