package jp.tomorrowkey.android.gifplayer;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.support.v4.app.FragmentTransaction;
import java.io.InputStream;
import java.util.Vector;

public class GifDecoder {
    protected static final int MAX_STACK_SIZE = 4096;
    public static final int STATUS_FORMAT_ERROR = 1;
    public static final int STATUS_OK = 0;
    public static final int STATUS_OPEN_ERROR = 2;
    protected int[] act;
    protected int bgColor;
    protected int bgIndex;
    protected byte[] block = new byte[256];
    protected int blockSize = 0;
    protected int delay = 0;
    protected int dispose = 0;
    protected int frameCount;
    protected Vector<GifFrame> frames;
    protected int[] gct;
    protected boolean gctFlag;
    protected int gctSize;
    protected int height;
    protected int ih;
    protected Bitmap image;
    protected InputStream in;
    protected boolean interlace;
    protected int iw;
    protected int ix;
    protected int iy;
    protected int lastBgColor;
    protected Bitmap lastBitmap;
    protected int lastDispose = 0;
    protected int[] lct;
    protected boolean lctFlag;
    protected int lctSize;
    protected int loopCount = 1;
    protected int lrh;
    protected int lrw;
    protected int lrx;
    protected int lry;
    protected int pixelAspect;
    protected byte[] pixelStack;
    protected byte[] pixels;
    protected short[] prefix;
    protected int status;
    protected byte[] suffix;
    protected int transIndex;
    protected boolean transparency = false;
    protected int width;

    private static class GifFrame {
        public int delay;
        public Bitmap image;

        public GifFrame(Bitmap im, int del) {
            this.image = im;
            this.delay = del;
        }
    }

    public int getDelay(int n) {
        this.delay = -1;
        if (n >= 0 && n < this.frameCount) {
            this.delay = ((GifFrame) this.frames.elementAt(n)).delay;
        }
        return this.delay;
    }

    public int getFrameCount() {
        return this.frameCount;
    }

    public Bitmap getBitmap() {
        return getFrame(0);
    }

    public int getLoopCount() {
        return this.loopCount;
    }

    /* access modifiers changed from: protected */
    public void setPixels() {
        int[] dest = new int[(this.width * this.height)];
        int i = this.lastDispose;
        if (i > 0) {
            if (i == 3) {
                int n = this.frameCount - 2;
                if (n > 0) {
                    this.lastBitmap = getFrame(n - 1);
                } else {
                    this.lastBitmap = null;
                }
            }
            Bitmap bitmap = this.lastBitmap;
            if (bitmap != null) {
                int i2 = this.width;
                bitmap.getPixels(dest, 0, i2, 0, 0, i2, this.height);
                if (this.lastDispose == 2) {
                    int c = 0;
                    if (!this.transparency) {
                        c = this.lastBgColor;
                    }
                    for (int i3 = 0; i3 < this.lrh; i3++) {
                        int n1 = ((this.lry + i3) * this.width) + this.lrx;
                        int n2 = this.lrw + n1;
                        for (int k = n1; k < n2; k++) {
                            dest[k] = c;
                        }
                    }
                }
            }
        }
        int pass = 1;
        int inc = 8;
        int iline = 0;
        int i4 = 0;
        while (true) {
            int i5 = this.ih;
            if (i4 < i5) {
                int line = i4;
                if (this.interlace) {
                    if (iline >= i5) {
                        pass++;
                        switch (pass) {
                            case 2:
                                iline = 4;
                                break;
                            case 3:
                                iline = 2;
                                inc = 4;
                                break;
                            case 4:
                                iline = 1;
                                inc = 2;
                                break;
                        }
                    }
                    line = iline;
                    iline += inc;
                }
                int line2 = line + this.iy;
                if (line2 < this.height) {
                    int i6 = this.width;
                    int k2 = line2 * i6;
                    int dx = this.ix + k2;
                    int dlim = this.iw + dx;
                    if (k2 + i6 < dlim) {
                        dlim = k2 + i6;
                    }
                    int sx = this.iw * i4;
                    while (dx < dlim) {
                        int sx2 = sx + 1;
                        int c2 = this.act[this.pixels[sx] & 255];
                        if (c2 != 0) {
                            dest[dx] = c2;
                        }
                        dx++;
                        sx = sx2;
                    }
                }
                i4++;
            } else {
                this.image = Bitmap.createBitmap(dest, this.width, this.height, Config.ARGB_4444);
                return;
            }
        }
    }

    public Bitmap getFrame(int n) {
        int i = this.frameCount;
        if (i <= 0) {
            return null;
        }
        return ((GifFrame) this.frames.elementAt(n % i)).image;
    }

    public int read(InputStream is) {
        init();
        if (is != null) {
            this.in = is;
            readHeader();
            if (!err()) {
                readContents();
                if (this.frameCount < 0) {
                    this.status = 1;
                }
            }
        } else {
            this.status = 2;
        }
        try {
            is.close();
        } catch (Exception e) {
        }
        return this.status;
    }

    /* JADX WARNING: type inference failed for: r1v0 */
    /* JADX WARNING: type inference failed for: r13v1 */
    /* JADX WARNING: type inference failed for: r13v2, types: [int] */
    /* JADX WARNING: type inference failed for: r1v1 */
    /* JADX WARNING: type inference failed for: r24v0 */
    /* JADX WARNING: type inference failed for: r13v3 */
    /* JADX WARNING: type inference failed for: r1v5 */
    /* JADX WARNING: type inference failed for: r24v1 */
    /* JADX WARNING: type inference failed for: r13v4 */
    /* JADX WARNING: type inference failed for: r1v7 */
    /* JADX WARNING: type inference failed for: r24v2 */
    /* JADX WARNING: type inference failed for: r24v3 */
    /* JADX WARNING: type inference failed for: r24v4 */
    /* JADX WARNING: type inference failed for: r24v5 */
    /* JADX WARNING: type inference failed for: r1v8 */
    /* JADX WARNING: type inference failed for: r12v3 */
    /* JADX WARNING: type inference failed for: r12v4 */
    /* JADX WARNING: type inference failed for: r5v9 */
    /* JADX WARNING: type inference failed for: r13v5 */
    /* JADX WARNING: type inference failed for: r3v16, types: [short[]] */
    /* JADX WARNING: type inference failed for: r12v5, types: [short] */
    /* JADX WARNING: type inference failed for: r3v18 */
    /* JADX WARNING: type inference failed for: r12v6 */
    /* JADX WARNING: type inference failed for: r24v6 */
    /* JADX WARNING: type inference failed for: r13v6 */
    /* JADX WARNING: type inference failed for: r1v11 */
    /* JADX WARNING: type inference failed for: r13v7 */
    /* JADX WARNING: type inference failed for: r24v7 */
    /* JADX WARNING: type inference failed for: r1v12 */
    /* JADX WARNING: type inference failed for: r13v9 */
    /* JADX WARNING: type inference failed for: r13v10 */
    /* JADX WARNING: type inference failed for: r1v13 */
    /* JADX WARNING: type inference failed for: r1v14 */
    /* JADX WARNING: type inference failed for: r13v11 */
    /* JADX WARNING: type inference failed for: r1v15 */
    /* JADX WARNING: type inference failed for: r13v12 */
    /* JADX WARNING: type inference failed for: r12v8 */
    /* JADX WARNING: type inference failed for: r12v9 */
    /* access modifiers changed from: protected */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x0135, code lost:
        r4 = r23;
     */
    /* JADX WARNING: Incorrect type for immutable var: ssa=short, code=null, for r12v5, types: [short] */
    /* JADX WARNING: Incorrect type for immutable var: ssa=short[], code=null, for r3v16, types: [short[]] */
    /* JADX WARNING: Multi-variable type inference failed. Error: jadx.core.utils.exceptions.JadxRuntimeException: No candidate types for var: r13v3
  assigns: []
  uses: []
  mth insns count: 180
    	at jadx.core.dex.visitors.typeinference.TypeSearch.fillTypeCandidates(TypeSearch.java:237)
    	at java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.typeinference.TypeSearch.run(TypeSearch.java:53)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.runMultiVariableSearch(TypeInferenceVisitor.java:99)
    	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:92)
    	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
    	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
    	at java.util.ArrayList.forEach(Unknown Source)
    	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
    	at jadx.core.ProcessClass.process(ProcessClass.java:30)
    	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:311)
    	at jadx.api.JavaClass.decompile(JavaClass.java:62)
    	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:217)
     */
    /* JADX WARNING: Unknown variable types count: 19 */
    public void decodeBitmapData() {
        ? r13;
        ? r1;
        int clear;
        int data_size;
        ? r24;
        ? r132;
        int first;
        ? r133;
        ? r12 = -1;
        int npix = this.iw * this.ih;
        byte[] bArr = this.pixels;
        if (bArr == null || bArr.length < npix) {
            this.pixels = new byte[npix];
        }
        if (this.prefix == null) {
            this.prefix = new short[4096];
        }
        if (this.suffix == null) {
            this.suffix = new byte[4096];
        }
        if (this.pixelStack == null) {
            this.pixelStack = new byte[FragmentTransaction.TRANSIT_FRAGMENT_OPEN];
        }
        int data_size2 = read();
        int clear2 = 1 << data_size2;
        int end_of_information = clear2 + 1;
        int available = clear2 + 2;
        int code_size = data_size2 + 1;
        int code_mask = (1 << code_size) - 1;
        for (int code = 0; code < clear2; code++) {
            this.prefix[code] = 0;
            this.suffix[code] = (byte) code;
        }
        int top = 0;
        int count = 0;
        int datum = 0;
        ? r134 = -1;
        byte b = 0;
        int bits = 0;
        int available2 = available;
        int i = 0;
        int pi = 0;
        int bi = 0;
        ? r14 = r12;
        while (true) {
            if (i >= npix) {
                ? r242 = r14;
                int i2 = data_size2;
                byte b2 = b;
                int i3 = clear2;
                break;
            }
            if (top == 0) {
                if (bits >= code_size) {
                    byte b3 = datum & code_mask;
                    datum >>= code_size;
                    bits -= code_size;
                    if (b3 > available2) {
                        ? r243 = r14;
                        int i4 = data_size2;
                        first = b;
                        int i5 = clear2;
                        break;
                    } else if (b3 == end_of_information) {
                        ? r244 = r14;
                        int i6 = data_size2;
                        first = b;
                        int i7 = clear2;
                        break;
                    } else {
                        if (b3 == clear2) {
                            code_size = data_size2 + 1;
                            code_mask = (1 << code_size) - 1;
                            available2 = clear2 + 2;
                            r13 = r14;
                            r1 = r14;
                        } else if (r134 == r14) {
                            int top2 = top + 1;
                            ? r245 = r14;
                            this.pixelStack[top] = this.suffix[b3];
                            r13 = b3;
                            b = b3;
                            top = top2;
                            r1 = r245;
                        } else {
                            r24 = r14;
                            ? r15 = b3;
                            if (b3 == available2) {
                                int top3 = top + 1;
                                data_size = data_size2;
                                this.pixelStack[top] = (byte) b;
                                b3 = r134;
                                top = top3;
                            } else {
                                data_size = data_size2;
                            }
                            ? r122 = b3;
                            while (r122 > clear2) {
                                int top4 = top + 1;
                                byte b4 = b;
                                this.pixelStack[top] = this.suffix[r122];
                                top = top4;
                                b = b4;
                                r122 = this.prefix[r122];
                            }
                            int first2 = b;
                            byte[] bArr2 = this.suffix;
                            b = bArr2[r122] & 255;
                            if (available2 >= 4096) {
                                int i8 = clear2;
                                break;
                            }
                            int top5 = top + 1;
                            clear = clear2;
                            this.pixelStack[top] = (byte) b;
                            this.prefix[available2] = (short) r134;
                            bArr2[available2] = (byte) b;
                            available2++;
                            if ((available2 & code_mask) == 0) {
                                if (available2 < 4096) {
                                    code_size++;
                                    code_mask += available2;
                                }
                            }
                            r133 = r15;
                            top = top5;
                        }
                        r134 = r13;
                        r14 = r1;
                    }
                } else {
                    if (count == 0) {
                        count = readBlock();
                        if (count <= 0) {
                            ? r246 = r14;
                            int i9 = data_size2;
                            int i10 = clear2;
                            break;
                        }
                        bi = 0;
                    }
                    datum += (this.block[bi] & 255) << bits;
                    bits += 8;
                    bi++;
                    count--;
                    r13 = r134;
                    r1 = r14;
                    r134 = r13;
                    r14 = r1;
                }
            } else {
                r24 = r14;
                data_size = data_size2;
                byte b5 = b;
                clear = clear2;
                r133 = r134;
            }
            top--;
            int pi2 = pi + 1;
            this.pixels[pi] = this.pixelStack[top];
            i++;
            pi = pi2;
            r1 = r24;
            data_size2 = data_size;
            clear2 = clear;
            r13 = r132;
            r134 = r13;
            r14 = r1;
        }
        for (int i11 = pi; i11 < npix; i11++) {
            this.pixels[i11] = 0;
        }
    }

    /* access modifiers changed from: protected */
    public boolean err() {
        return this.status != 0;
    }

    /* access modifiers changed from: protected */
    public void init() {
        this.status = 0;
        this.frameCount = 0;
        this.frames = new Vector<>();
        this.gct = null;
        this.lct = null;
    }

    /* access modifiers changed from: protected */
    public int read() {
        try {
            return this.in.read();
        } catch (Exception e) {
            this.status = 1;
            return 0;
        }
    }

    /* access modifiers changed from: protected */
    public int readBlock() {
        this.blockSize = read();
        int n = 0;
        if (this.blockSize > 0) {
            while (n < this.blockSize) {
                try {
                    int count = this.in.read(this.block, n, this.blockSize - n);
                    if (count == -1) {
                        break;
                    }
                    n += count;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (n < this.blockSize) {
                this.status = 1;
            }
        }
        return n;
    }

    /* access modifiers changed from: protected */
    public int[] readColorTable(int ncolors) {
        int nbytes = ncolors * 3;
        int[] tab = null;
        byte[] c = new byte[nbytes];
        int n = 0;
        try {
            n = this.in.read(c);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (n < nbytes) {
            this.status = 1;
        } else {
            tab = new int[256];
            int i = 0;
            int r = 0;
            while (i < ncolors) {
                int j = r + 1;
                int j2 = j + 1;
                int j3 = j2 + 1;
                int i2 = i + 1;
                tab[i] = -16777216 | ((c[r] & 255) << 16) | ((c[j] & 255) << 8) | (c[j2] & 255);
                r = j3;
                i = i2;
            }
        }
        return tab;
    }

    /* access modifiers changed from: protected */
    public void readContents() {
        boolean done = false;
        while (!done && !err()) {
            int code = read();
            if (code == 33) {
                int code2 = read();
                if (code2 == 1) {
                    skip();
                } else if (code2 != 249) {
                    switch (code2) {
                        case 254:
                            skip();
                            break;
                        case 255:
                            readBlock();
                            String app = "";
                            for (int i = 0; i < 11; i++) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(app);
                                sb.append((char) this.block[i]);
                                app = sb.toString();
                            }
                            if (!app.equals("NETSCAPE2.0")) {
                                skip();
                                break;
                            } else {
                                readNetscapeExt();
                                break;
                            }
                        default:
                            skip();
                            break;
                    }
                } else {
                    readGraphicControlExt();
                }
            } else if (code == 44) {
                readBitmap();
            } else if (code != 59) {
                this.status = 1;
            } else {
                done = true;
            }
        }
    }

    /* access modifiers changed from: protected */
    public void readGraphicControlExt() {
        read();
        int packed = read();
        this.dispose = (packed & 28) >> 2;
        boolean z = true;
        if (this.dispose == 0) {
            this.dispose = 1;
        }
        if ((packed & 1) == 0) {
            z = false;
        }
        this.transparency = z;
        this.delay = readShort() * 10;
        this.transIndex = read();
        read();
    }

    /* access modifiers changed from: protected */
    public void readHeader() {
        String id = "";
        for (int i = 0; i < 6; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(id);
            sb.append((char) read());
            id = sb.toString();
        }
        if (!id.startsWith("GIF")) {
            this.status = 1;
            return;
        }
        readLSD();
        if (this.gctFlag && !err()) {
            this.gct = readColorTable(this.gctSize);
            this.bgColor = this.gct[this.bgIndex];
        }
    }

    /* access modifiers changed from: protected */
    public void readBitmap() {
        this.ix = readShort();
        this.iy = readShort();
        this.iw = readShort();
        this.ih = readShort();
        int packed = read();
        this.lctFlag = (packed & 128) != 0;
        this.lctSize = (int) Math.pow(2.0d, (double) ((packed & 7) + 1));
        this.interlace = (packed & 64) != 0;
        if (this.lctFlag) {
            this.lct = readColorTable(this.lctSize);
            this.act = this.lct;
        } else {
            this.act = this.gct;
            if (this.bgIndex == this.transIndex) {
                this.bgColor = 0;
            }
        }
        int save = 0;
        if (this.transparency) {
            int[] iArr = this.act;
            int i = this.transIndex;
            save = iArr[i];
            iArr[i] = 0;
        }
        if (this.act == null) {
            this.status = 1;
        }
        if (!err()) {
            decodeBitmapData();
            skip();
            if (!err()) {
                this.frameCount++;
                this.image = Bitmap.createBitmap(this.width, this.height, Config.ARGB_4444);
                setPixels();
                this.frames.addElement(new GifFrame(this.image, this.delay));
                if (this.transparency) {
                    this.act[this.transIndex] = save;
                }
                resetFrame();
            }
        }
    }

    /* access modifiers changed from: protected */
    public void readLSD() {
        this.width = readShort();
        this.height = readShort();
        int packed = read();
        this.gctFlag = (packed & 128) != 0;
        this.gctSize = 2 << (packed & 7);
        this.bgIndex = read();
        this.pixelAspect = read();
    }

    /* access modifiers changed from: protected */
    public void readNetscapeExt() {
        do {
            readBlock();
            byte[] bArr = this.block;
            if (bArr[0] == 1) {
                this.loopCount = ((bArr[2] & 255) << 8) | (bArr[1] & 255);
            }
            if (this.blockSize <= 0) {
                return;
            }
        } while (!err());
    }

    /* access modifiers changed from: protected */
    public int readShort() {
        return read() | (read() << 8);
    }

    /* access modifiers changed from: protected */
    public void resetFrame() {
        this.lastDispose = this.dispose;
        this.lrx = this.ix;
        this.lry = this.iy;
        this.lrw = this.iw;
        this.lrh = this.ih;
        this.lastBitmap = this.image;
        this.lastBgColor = this.bgColor;
        this.dispose = 0;
        this.transparency = false;
        this.delay = 0;
        this.lct = null;
    }

    /* access modifiers changed from: protected */
    public void skip() {
        do {
            readBlock();
            if (this.blockSize <= 0) {
                return;
            }
        } while (!err());
    }
}
