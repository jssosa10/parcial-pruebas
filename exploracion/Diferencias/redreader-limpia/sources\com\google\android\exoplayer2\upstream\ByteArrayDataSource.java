package com.google.android.exoplayer2.upstream;

import android.net.Uri;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.util.Assertions;
import java.io.IOException;

public final class ByteArrayDataSource extends BaseDataSource {
    private int bytesRemaining;
    private final byte[] data;
    private boolean opened;
    private int readPosition;
    @Nullable
    private Uri uri;

    public ByteArrayDataSource(byte[] data2) {
        boolean z = false;
        super(false);
        Assertions.checkNotNull(data2);
        if (data2.length > 0) {
            z = true;
        }
        Assertions.checkArgument(z);
        this.data = data2;
    }

    public long open(DataSpec dataSpec) throws IOException {
        this.uri = dataSpec.uri;
        transferInitializing(dataSpec);
        this.readPosition = (int) dataSpec.position;
        this.bytesRemaining = (int) (dataSpec.length == -1 ? ((long) this.data.length) - dataSpec.position : dataSpec.length);
        int i = this.bytesRemaining;
        if (i <= 0 || this.readPosition + i > this.data.length) {
            StringBuilder sb = new StringBuilder();
            sb.append("Unsatisfiable range: [");
            sb.append(this.readPosition);
            sb.append(", ");
            sb.append(dataSpec.length);
            sb.append("], length: ");
            sb.append(this.data.length);
            throw new IOException(sb.toString());
        }
        this.opened = true;
        transferStarted(dataSpec);
        return (long) this.bytesRemaining;
    }

    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        if (readLength == 0) {
            return 0;
        }
        int i = this.bytesRemaining;
        if (i == 0) {
            return -1;
        }
        int readLength2 = Math.min(readLength, i);
        System.arraycopy(this.data, this.readPosition, buffer, offset, readLength2);
        this.readPosition += readLength2;
        this.bytesRemaining -= readLength2;
        bytesTransferred(readLength2);
        return readLength2;
    }

    @Nullable
    public Uri getUri() {
        return this.uri;
    }

    public void close() throws IOException {
        if (this.opened) {
            this.opened = false;
            transferEnded();
        }
        this.uri = null;
    }
}
