package org.quantumbadger.redreader.io;

import android.support.annotation.Nullable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ExtendedDataOutputStream extends DataOutputStream {
    public ExtendedDataOutputStream(OutputStream out) {
        super(out);
    }

    public void writeNullableBoolean(@Nullable Boolean value) throws IOException {
        if (value == null) {
            writeBoolean(false);
            return;
        }
        writeBoolean(true);
        writeBoolean(value.booleanValue());
    }
}
