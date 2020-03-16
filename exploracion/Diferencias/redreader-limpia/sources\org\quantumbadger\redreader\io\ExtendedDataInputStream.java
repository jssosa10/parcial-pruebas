package org.quantumbadger.redreader.io;

import android.support.annotation.Nullable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ExtendedDataInputStream extends DataInputStream {
    public ExtendedDataInputStream(InputStream in) {
        super(in);
    }

    @Nullable
    public Boolean readNullableBoolean() throws IOException {
        if (!readBoolean()) {
            return null;
        }
        return Boolean.valueOf(readBoolean());
    }
}
