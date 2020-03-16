package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import java.lang.ref.SoftReference;

public class BufferRecyclers {
    protected static final ThreadLocal<SoftReference<JsonStringEncoder>> _encoderRef = new ThreadLocal<>();
    protected static final ThreadLocal<SoftReference<BufferRecycler>> _recyclerRef = new ThreadLocal<>();

    public static BufferRecycler getBufferRecycler() {
        SoftReference<BufferRecycler> ref = (SoftReference) _recyclerRef.get();
        BufferRecycler br = ref == null ? null : (BufferRecycler) ref.get();
        if (br != null) {
            return br;
        }
        BufferRecycler br2 = new BufferRecycler();
        _recyclerRef.set(new SoftReference(br2));
        return br2;
    }

    public static JsonStringEncoder getJsonStringEncoder() {
        SoftReference<JsonStringEncoder> ref = (SoftReference) _encoderRef.get();
        JsonStringEncoder enc = ref == null ? null : (JsonStringEncoder) ref.get();
        if (enc != null) {
            return enc;
        }
        JsonStringEncoder enc2 = new JsonStringEncoder();
        _encoderRef.set(new SoftReference(enc2));
        return enc2;
    }
}
