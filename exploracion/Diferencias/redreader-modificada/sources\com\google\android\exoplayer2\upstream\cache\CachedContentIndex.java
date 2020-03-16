package com.google.android.exoplayer2.upstream.cache;

import android.util.SparseArray;
import com.google.android.exoplayer2.upstream.cache.Cache.CacheException;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.AtomicFile;
import com.google.android.exoplayer2.util.ReusableBufferedOutputStream;
import com.google.android.exoplayer2.util.Util;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

class CachedContentIndex {
    public static final String FILE_NAME = "cached_content_index.exi";
    private static final int FLAG_ENCRYPTED_INDEX = 1;
    private static final int VERSION = 2;
    private final AtomicFile atomicFile;
    private ReusableBufferedOutputStream bufferedOutputStream;
    private boolean changed;
    private final Cipher cipher;
    private final boolean encrypt;
    private final SparseArray<String> idToKey;
    private final HashMap<String, CachedContent> keyToContent;
    private final SecretKeySpec secretKeySpec;

    public CachedContentIndex(File cacheDir) {
        this(cacheDir, null);
    }

    public CachedContentIndex(File cacheDir, byte[] secretKey) {
        this(cacheDir, secretKey, secretKey != null);
    }

    public CachedContentIndex(File cacheDir, byte[] secretKey, boolean encrypt2) {
        this.encrypt = encrypt2;
        if (secretKey != null) {
            Assertions.checkArgument(secretKey.length == 16);
            try {
                this.cipher = getCipher();
                this.secretKeySpec = new SecretKeySpec(secretKey, "AES");
            } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
                throw new IllegalStateException(e);
            }
        } else {
            Assertions.checkState(!encrypt2);
            this.cipher = null;
            this.secretKeySpec = null;
        }
        this.keyToContent = new HashMap<>();
        this.idToKey = new SparseArray<>();
        this.atomicFile = new AtomicFile(new File(cacheDir, FILE_NAME));
    }

    public void load() {
        Assertions.checkState(!this.changed);
        if (!readFile()) {
            this.atomicFile.delete();
            this.keyToContent.clear();
            this.idToKey.clear();
        }
    }

    public void store() throws CacheException {
        if (this.changed) {
            writeFile();
            this.changed = false;
        }
    }

    public CachedContent getOrAdd(String key) {
        CachedContent cachedContent = (CachedContent) this.keyToContent.get(key);
        return cachedContent == null ? addNew(key) : cachedContent;
    }

    public CachedContent get(String key) {
        return (CachedContent) this.keyToContent.get(key);
    }

    public Collection<CachedContent> getAll() {
        return this.keyToContent.values();
    }

    public int assignIdForKey(String key) {
        return getOrAdd(key).id;
    }

    public String getKeyForId(int id) {
        return (String) this.idToKey.get(id);
    }

    public void maybeRemove(String key) {
        CachedContent cachedContent = (CachedContent) this.keyToContent.get(key);
        if (cachedContent != null && cachedContent.isEmpty() && !cachedContent.isLocked()) {
            this.keyToContent.remove(key);
            this.idToKey.remove(cachedContent.id);
            this.changed = true;
        }
    }

    public void removeEmpty() {
        String[] keys = new String[this.keyToContent.size()];
        this.keyToContent.keySet().toArray(keys);
        for (String key : keys) {
            maybeRemove(key);
        }
    }

    public Set<String> getKeys() {
        return this.keyToContent.keySet();
    }

    public void applyContentMetadataMutations(String key, ContentMetadataMutations mutations) {
        if (getOrAdd(key).applyMetadataMutations(mutations)) {
            this.changed = true;
        }
    }

    public ContentMetadata getContentMetadata(String key) {
        CachedContent cachedContent = get(key);
        return cachedContent != null ? cachedContent.getMetadata() : DefaultContentMetadata.EMPTY;
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x006a A[Catch:{ IOException -> 0x00a4, all -> 0x009d }, LOOP:0: B:30:0x0068->B:31:0x006a, LOOP_END] */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0084  */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x0086  */
    private boolean readFile() {
        int count;
        int hashCode;
        int i;
        int fileHashCode;
        DataInputStream input = null;
        try {
            InputStream inputStream = new BufferedInputStream(this.atomicFile.openRead());
            input = new DataInputStream(inputStream);
            int version = input.readInt();
            if (version >= 0) {
                if (version <= 2) {
                    if ((input.readInt() & 1) != 0) {
                        if (this.cipher == null) {
                            Util.closeQuietly((Closeable) input);
                            return false;
                        }
                        byte[] initializationVector = new byte[16];
                        input.readFully(initializationVector);
                        try {
                            this.cipher.init(2, this.secretKeySpec, new IvParameterSpec(initializationVector));
                            input = new DataInputStream(new CipherInputStream(inputStream, this.cipher));
                        } catch (InvalidKeyException e) {
                            e = e;
                        } catch (InvalidAlgorithmParameterException e2) {
                            e = e2;
                        }
                    } else if (this.encrypt) {
                        this.changed = true;
                        count = input.readInt();
                        hashCode = 0;
                        for (i = 0; i < count; i++) {
                            CachedContent cachedContent = CachedContent.readFromStream(version, input);
                            add(cachedContent);
                            hashCode += cachedContent.headerHashCode(version);
                        }
                        fileHashCode = input.readInt();
                        boolean isEOF = input.read() != -1;
                        if (fileHashCode == hashCode || !isEOF) {
                            Util.closeQuietly((Closeable) input);
                            return false;
                        }
                        Util.closeQuietly((Closeable) input);
                        return true;
                    }
                    count = input.readInt();
                    hashCode = 0;
                    while (i < count) {
                    }
                    fileHashCode = input.readInt();
                    if (input.read() != -1) {
                    }
                    if (fileHashCode == hashCode) {
                    }
                    Util.closeQuietly((Closeable) input);
                    return false;
                }
            }
            Util.closeQuietly((Closeable) input);
            return false;
            throw new IllegalStateException(e);
        } catch (IOException e3) {
            if (input != null) {
                Util.closeQuietly((Closeable) input);
            }
            return false;
        } catch (Throwable th) {
            if (input != null) {
                Util.closeQuietly((Closeable) input);
            }
            throw th;
        }
    }

    private void writeFile() throws CacheException {
        DataOutputStream output = null;
        try {
            OutputStream outputStream = this.atomicFile.startWrite();
            if (this.bufferedOutputStream == null) {
                this.bufferedOutputStream = new ReusableBufferedOutputStream(outputStream);
            } else {
                this.bufferedOutputStream.reset(outputStream);
            }
            output = new DataOutputStream(this.bufferedOutputStream);
            output.writeInt(2);
            output.writeInt(this.encrypt ? 1 : 0);
            if (this.encrypt) {
                byte[] initializationVector = new byte[16];
                new Random().nextBytes(initializationVector);
                output.write(initializationVector);
                try {
                    this.cipher.init(1, this.secretKeySpec, new IvParameterSpec(initializationVector));
                    output.flush();
                    output = new DataOutputStream(new CipherOutputStream(this.bufferedOutputStream, this.cipher));
                } catch (InvalidKeyException e) {
                    e = e;
                } catch (InvalidAlgorithmParameterException e2) {
                    e = e2;
                }
            }
            output.writeInt(this.keyToContent.size());
            int hashCode = 0;
            for (CachedContent cachedContent : this.keyToContent.values()) {
                cachedContent.writeToStream(output);
                hashCode += cachedContent.headerHashCode(2);
            }
            output.writeInt(hashCode);
            this.atomicFile.endWrite(output);
            Util.closeQuietly((Closeable) null);
            return;
            throw new IllegalStateException(e);
        } catch (IOException e3) {
            throw new CacheException((Throwable) e3);
        } catch (Throwable th) {
            Util.closeQuietly((Closeable) output);
            throw th;
        }
    }

    private CachedContent addNew(String key) {
        CachedContent cachedContent = new CachedContent(getNewId(this.idToKey), key);
        add(cachedContent);
        this.changed = true;
        return cachedContent;
    }

    private void add(CachedContent cachedContent) {
        this.keyToContent.put(cachedContent.key, cachedContent);
        this.idToKey.put(cachedContent.id, cachedContent.key);
    }

    private static Cipher getCipher() throws NoSuchPaddingException, NoSuchAlgorithmException {
        if (Util.SDK_INT == 18) {
            try {
                return Cipher.getInstance("AES/CBC/PKCS5PADDING", "BC");
            } catch (Throwable th) {
            }
        }
        return Cipher.getInstance("AES/CBC/PKCS5PADDING");
    }

    public static int getNewId(SparseArray<String> idToKey2) {
        int size = idToKey2.size();
        int id = size == 0 ? 0 : idToKey2.keyAt(size - 1) + 1;
        if (id < 0) {
            int id2 = 0;
            while (id < size && id == idToKey2.keyAt(id)) {
                id2 = id + 1;
            }
        }
        return id;
    }
}
