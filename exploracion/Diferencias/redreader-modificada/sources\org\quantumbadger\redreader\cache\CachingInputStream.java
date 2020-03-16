package org.quantumbadger.redreader.cache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class CachingInputStream extends InputStream {
    private long bytesRead = 0;
    private final InputStream in;
    private final BytesReadListener listener;
    private final OutputStream out;
    private boolean stillRunning = true;

    public interface BytesReadListener {
        void onBytesRead(long j);
    }

    public CachingInputStream(InputStream in2, OutputStream out2, BytesReadListener listener2) {
        this.in = in2;
        this.out = out2;
        this.listener = listener2;
    }

    private void notifyOnBytesRead() {
        BytesReadListener bytesReadListener = this.listener;
        if (bytesReadListener != null) {
            bytesReadListener.onBytesRead(this.bytesRead);
        }
    }

    public void close() throws IOException {
        if (this.stillRunning) {
            this.in.close();
            throw new RuntimeException("Closing CachingInputStream before the input stream has ended");
        }
    }

    public int read() throws IOException {
        int byteRead = this.in.read();
        if (byteRead >= 0) {
            this.out.write(byteRead);
            this.bytesRead++;
            notifyOnBytesRead();
        } else {
            terminate();
        }
        return byteRead;
    }

    public int read(byte[] buffer, int offset, int length) throws IOException {
        int result = this.in.read(buffer, offset, length);
        if (result > 0) {
            this.out.write(buffer, offset, result);
            this.bytesRead += (long) result;
            notifyOnBytesRead();
        } else {
            terminate();
        }
        return result;
    }

    private void terminate() throws IOException {
        if (this.stillRunning) {
            this.stillRunning = false;
            this.out.flush();
            this.out.close();
            this.in.close();
        }
    }
}
