package com.google.android.exoplayer2.text.ttml;

final class TtmlRegion {
    public final String id;
    public final float line;
    public final int lineAnchor;
    public final int lineType;
    public final float position;
    public final float textSize;
    public final int textSizeType;
    public final float width;

    public TtmlRegion(String id2) {
        this(id2, Float.MIN_VALUE, Float.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Float.MIN_VALUE, Integer.MIN_VALUE, Float.MIN_VALUE);
    }

    public TtmlRegion(String id2, float position2, float line2, int lineType2, int lineAnchor2, float width2, int textSizeType2, float textSize2) {
        this.id = id2;
        this.position = position2;
        this.line = line2;
        this.lineType = lineType2;
        this.lineAnchor = lineAnchor2;
        this.width = width2;
        this.textSizeType = textSizeType2;
        this.textSize = textSize2;
    }
}
