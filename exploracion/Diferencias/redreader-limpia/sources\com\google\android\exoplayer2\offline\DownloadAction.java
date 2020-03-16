package com.google.android.exoplayer2.offline;

import android.net.Uri;
import android.support.annotation.Nullable;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class DownloadAction {
    @Nullable
    private static Deserializer[] defaultDeserializers;
    public final byte[] data;
    public final boolean isRemoveAction;
    public final String type;
    public final Uri uri;
    public final int version;

    public static abstract class Deserializer {
        public final String type;
        public final int version;

        public abstract DownloadAction readFromStream(int i, DataInputStream dataInputStream) throws IOException;

        public Deserializer(String type2, int version2) {
            this.type = type2;
            this.version = version2;
        }
    }

    public abstract Downloader createDownloader(DownloaderConstructorHelper downloaderConstructorHelper);

    /* access modifiers changed from: protected */
    public abstract void writeToStream(DataOutputStream dataOutputStream) throws IOException;

    public static synchronized Deserializer[] getDefaultDeserializers() {
        int count;
        int count2;
        int count3;
        synchronized (DownloadAction.class) {
            if (defaultDeserializers != null) {
                Deserializer[] deserializerArr = defaultDeserializers;
                return deserializerArr;
            }
            Deserializer[] deserializers = new Deserializer[4];
            int count4 = 0 + 1;
            deserializers[0] = ProgressiveDownloadAction.DESERIALIZER;
            try {
                count = count4 + 1;
                try {
                    deserializers[count4] = getDeserializer(Class.forName("com.google.android.exoplayer2.source.dash.offline.DashDownloadAction"));
                } catch (Exception e) {
                }
            } catch (Exception e2) {
                count = count4;
            }
            try {
                count2 = count + 1;
                try {
                    deserializers[count] = getDeserializer(Class.forName("com.google.android.exoplayer2.source.hls.offline.HlsDownloadAction"));
                } catch (Exception e3) {
                }
            } catch (Exception e4) {
                count2 = count;
            }
            try {
                count3 = count2 + 1;
                try {
                    deserializers[count2] = getDeserializer(Class.forName("com.google.android.exoplayer2.source.smoothstreaming.offline.SsDownloadAction"));
                } catch (Exception e5) {
                }
            } catch (Exception e6) {
                count3 = count2;
            }
            defaultDeserializers = (Deserializer[]) Arrays.copyOf((Object[]) Assertions.checkNotNull(deserializers), count3);
            Deserializer[] deserializerArr2 = defaultDeserializers;
            return deserializerArr2;
        }
    }

    public static DownloadAction deserializeFromStream(Deserializer[] deserializers, InputStream input) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(input);
        String type2 = dataInputStream.readUTF();
        int version2 = dataInputStream.readInt();
        for (Deserializer deserializer : deserializers) {
            if (type2.equals(deserializer.type) && deserializer.version >= version2) {
                return deserializer.readFromStream(version2, dataInputStream);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("No deserializer found for:");
        sb.append(type2);
        sb.append(", ");
        sb.append(version2);
        throw new DownloadException(sb.toString());
    }

    public static void serializeToStream(DownloadAction action, OutputStream output) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(output);
        dataOutputStream.writeUTF(action.type);
        dataOutputStream.writeInt(action.version);
        action.writeToStream(dataOutputStream);
        dataOutputStream.flush();
    }

    protected DownloadAction(String type2, int version2, Uri uri2, boolean isRemoveAction2, @Nullable byte[] data2) {
        this.type = type2;
        this.version = version2;
        this.uri = uri2;
        this.isRemoveAction = isRemoveAction2;
        this.data = data2 != null ? data2 : Util.EMPTY_BYTE_ARRAY;
    }

    public final byte[] toByteArray() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            serializeToStream(this, output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException();
        }
    }

    public boolean isSameMedia(DownloadAction other) {
        return this.uri.equals(other.uri);
    }

    public List<StreamKey> getKeys() {
        return Collections.emptyList();
    }

    public boolean equals(@Nullable Object o) {
        boolean z = false;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DownloadAction that = (DownloadAction) o;
        if (this.type.equals(that.type) && this.version == that.version && this.uri.equals(that.uri) && this.isRemoveAction == that.isRemoveAction && Arrays.equals(this.data, that.data)) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return (((this.uri.hashCode() * 31) + (this.isRemoveAction ? 1 : 0)) * 31) + Arrays.hashCode(this.data);
    }

    private static Deserializer getDeserializer(Class<?> clazz) throws NoSuchFieldException, IllegalAccessException {
        return (Deserializer) Assertions.checkNotNull(clazz.getDeclaredField("DESERIALIZER").get(null));
    }
}
