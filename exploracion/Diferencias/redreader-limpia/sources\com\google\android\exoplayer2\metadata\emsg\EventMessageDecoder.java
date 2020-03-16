package com.google.android.exoplayer2.metadata.emsg;

import com.google.android.exoplayer2.metadata.Metadata;
import com.google.android.exoplayer2.metadata.MetadataDecoder;
import com.google.android.exoplayer2.metadata.MetadataInputBuffer;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;
import java.nio.ByteBuffer;
import java.util.Arrays;

public final class EventMessageDecoder implements MetadataDecoder {
    public Metadata decode(MetadataInputBuffer inputBuffer) {
        ByteBuffer buffer = inputBuffer.data;
        byte[] data = buffer.array();
        int size = buffer.limit();
        ParsableByteArray emsgData = new ParsableByteArray(data, size);
        String schemeIdUri = (String) Assertions.checkNotNull(emsgData.readNullTerminatedString());
        String value = (String) Assertions.checkNotNull(emsgData.readNullTerminatedString());
        long readUnsignedInt = emsgData.readUnsignedInt();
        long presentationTimeUs = Util.scaleLargeTimestamp(emsgData.readUnsignedInt(), 1000000, readUnsignedInt);
        ByteBuffer byteBuffer = buffer;
        EventMessage eventMessage = new EventMessage(schemeIdUri, value, Util.scaleLargeTimestamp(emsgData.readUnsignedInt(), 1000, readUnsignedInt), emsgData.readUnsignedInt(), Arrays.copyOfRange(data, emsgData.getPosition(), size), presentationTimeUs);
        Metadata metadata = new Metadata(eventMessage);
        return metadata;
    }
}
