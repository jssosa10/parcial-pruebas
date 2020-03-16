package org.quantumbadger.redreader.views.imageview;

import org.quantumbadger.redreader.common.MutableFloatPoint2D;
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderable;
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableBlend;
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableColouredQuad;
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableGroup;
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableScale;
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableTranslation;
import org.quantumbadger.redreader.views.glview.program.RRGLContext;
import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack;

public class ImageViewScrollbars extends RRGLRenderable {
    private static final float ALPHA_STEP = 0.05f;
    private static final float EPSILON = 1.0E-4f;
    private final CoordinateHelper mCoordinateHelper;
    private float mCurrentAlpha = 1.0f;
    private final int mDimBarWidth;
    private final int mDimBorderWidth;
    private final int mDimMarginEnds;
    private final int mDimMarginSides;
    private final RRGLRenderableGroup mHScroll;
    private final RRGLRenderableScale mHScrollBarScale;
    private final RRGLRenderableTranslation mHScrollBarTranslation;
    private final RRGLRenderableScale mHScrollBorderScale;
    private final RRGLRenderableTranslation mHScrollBorderTranslation;
    private final RRGLRenderableScale mHScrollMarkerScale;
    private final RRGLRenderableTranslation mHScrollMarkerTranslation;
    private final int mImageResX;
    private final int mImageResY;
    private boolean mIsVisible = true;
    private final RRGLRenderableBlend mRenderable;
    private int mResX;
    private int mResY;
    private long mShowUntil = -1;
    private final RRGLRenderableGroup mVScroll;
    private final RRGLRenderableScale mVScrollBarScale;
    private final RRGLRenderableTranslation mVScrollBarTranslation;
    private final RRGLRenderableScale mVScrollBorderScale;
    private final RRGLRenderableTranslation mVScrollBorderTranslation;
    private final RRGLRenderableScale mVScrollMarkerScale;
    private final RRGLRenderableTranslation mVScrollMarkerTranslation;

    public ImageViewScrollbars(RRGLContext glContext, CoordinateHelper coordinateHelper, int imageResX, int imageResY) {
        this.mCoordinateHelper = coordinateHelper;
        this.mImageResX = imageResX;
        this.mImageResY = imageResY;
        RRGLRenderableGroup group = new RRGLRenderableGroup();
        this.mRenderable = new RRGLRenderableBlend(group);
        this.mDimMarginSides = glContext.dpToPixels(10.0f);
        this.mDimMarginEnds = glContext.dpToPixels(20.0f);
        this.mDimBarWidth = glContext.dpToPixels(6.0f);
        this.mDimBorderWidth = glContext.dpToPixels(1.0f);
        this.mVScroll = new RRGLRenderableGroup();
        group.add(this.mVScroll);
        RRGLRenderableColouredQuad vScrollMarker = new RRGLRenderableColouredQuad(glContext);
        RRGLRenderableColouredQuad vScrollBar = new RRGLRenderableColouredQuad(glContext);
        RRGLRenderableColouredQuad vScrollBorder = new RRGLRenderableColouredQuad(glContext);
        vScrollMarker.setColour(1.0f, 1.0f, 1.0f, 0.8f);
        vScrollBar.setColour(0.0f, 0.0f, 0.0f, 0.5f);
        vScrollBorder.setColour(1.0f, 1.0f, 1.0f, 0.5f);
        this.mVScrollMarkerScale = new RRGLRenderableScale(vScrollMarker);
        this.mVScrollBarScale = new RRGLRenderableScale(vScrollBar);
        this.mVScrollBorderScale = new RRGLRenderableScale(vScrollBorder);
        this.mVScrollMarkerTranslation = new RRGLRenderableTranslation(this.mVScrollMarkerScale);
        this.mVScrollBarTranslation = new RRGLRenderableTranslation(this.mVScrollBarScale);
        this.mVScrollBorderTranslation = new RRGLRenderableTranslation(this.mVScrollBorderScale);
        this.mVScroll.add(this.mVScrollBorderTranslation);
        this.mVScroll.add(this.mVScrollBarTranslation);
        this.mVScroll.add(this.mVScrollMarkerTranslation);
        this.mHScroll = new RRGLRenderableGroup();
        group.add(this.mHScroll);
        RRGLRenderableColouredQuad hScrollMarker = new RRGLRenderableColouredQuad(glContext);
        RRGLRenderableColouredQuad hScrollBar = new RRGLRenderableColouredQuad(glContext);
        RRGLRenderableColouredQuad hScrollBorder = new RRGLRenderableColouredQuad(glContext);
        hScrollMarker.setColour(1.0f, 1.0f, 1.0f, 0.8f);
        hScrollBar.setColour(0.0f, 0.0f, 0.0f, 0.5f);
        hScrollBorder.setColour(1.0f, 1.0f, 1.0f, 0.5f);
        this.mHScrollMarkerScale = new RRGLRenderableScale(hScrollMarker);
        this.mHScrollBarScale = new RRGLRenderableScale(hScrollBar);
        this.mHScrollBorderScale = new RRGLRenderableScale(hScrollBorder);
        this.mHScrollMarkerTranslation = new RRGLRenderableTranslation(this.mHScrollMarkerScale);
        this.mHScrollBarTranslation = new RRGLRenderableTranslation(this.mHScrollBarScale);
        this.mHScrollBorderTranslation = new RRGLRenderableTranslation(this.mHScrollBorderScale);
        this.mHScroll.add(this.mHScrollBorderTranslation);
        this.mHScroll.add(this.mHScrollBarTranslation);
        this.mHScroll.add(this.mHScrollMarkerTranslation);
    }

    public void update() {
        MutableFloatPoint2D tmp1 = new MutableFloatPoint2D();
        MutableFloatPoint2D tmp2 = new MutableFloatPoint2D();
        this.mCoordinateHelper.convertScreenToScene(tmp1, tmp2);
        float xStart = tmp2.x / ((float) this.mImageResX);
        float yStart = tmp2.y / ((float) this.mImageResY);
        tmp1.set((float) this.mResX, (float) this.mResY);
        this.mCoordinateHelper.convertScreenToScene(tmp1, tmp2);
        float xEnd = tmp2.x / ((float) this.mImageResX);
        float yEnd = tmp2.y / ((float) this.mImageResY);
        if (yStart >= 1.0E-4f || yEnd <= 0.9999f) {
            this.mVScroll.show();
            int i = this.mResY;
            int i2 = this.mDimMarginEnds;
            float vScrollTotalHeight = (float) (i - (i2 * 2));
            float vScrollHeight = (yEnd - yStart) * vScrollTotalHeight;
            float vScrollTop = (yStart * vScrollTotalHeight) + ((float) i2);
            float vScrollLeft = (float) ((this.mResX - this.mDimBarWidth) - this.mDimMarginSides);
            RRGLRenderableTranslation rRGLRenderableTranslation = this.mVScrollBorderTranslation;
            int i3 = this.mDimBorderWidth;
            rRGLRenderableTranslation.setPosition(vScrollLeft - ((float) i3), (float) (i2 - i3));
            RRGLRenderableScale rRGLRenderableScale = this.mVScrollBorderScale;
            int i4 = this.mDimBarWidth;
            int i5 = this.mDimBorderWidth;
            rRGLRenderableScale.setScale((float) (i4 + (i5 * 2)), ((float) (i5 * 2)) + vScrollTotalHeight);
            this.mVScrollBarTranslation.setPosition(vScrollLeft, (float) this.mDimMarginEnds);
            this.mVScrollBarScale.setScale((float) this.mDimBarWidth, vScrollTotalHeight);
            this.mVScrollMarkerTranslation.setPosition(vScrollLeft, vScrollTop);
            this.mVScrollMarkerScale.setScale((float) this.mDimBarWidth, vScrollHeight);
        } else {
            this.mVScroll.hide();
        }
        if (xStart >= 1.0E-4f || xEnd <= 0.9999f) {
            this.mHScroll.show();
            int i6 = this.mResX;
            int i7 = this.mDimMarginEnds;
            float hScrollTotalWidth = (float) (i6 - (i7 * 2));
            float hScrollWidth = (xEnd - xStart) * hScrollTotalWidth;
            float hScrollLeft = (xStart * hScrollTotalWidth) + ((float) i7);
            float hScrollTop = (float) ((this.mResY - this.mDimBarWidth) - this.mDimMarginSides);
            RRGLRenderableTranslation rRGLRenderableTranslation2 = this.mHScrollBorderTranslation;
            int i8 = this.mDimBorderWidth;
            rRGLRenderableTranslation2.setPosition((float) (i7 - i8), hScrollTop - ((float) i8));
            RRGLRenderableScale rRGLRenderableScale2 = this.mHScrollBorderScale;
            int i9 = this.mDimBorderWidth;
            rRGLRenderableScale2.setScale(((float) (i9 * 2)) + hScrollTotalWidth, (float) (this.mDimBarWidth + (i9 * 2)));
            this.mHScrollBarTranslation.setPosition((float) this.mDimMarginEnds, hScrollTop);
            this.mHScrollBarScale.setScale(hScrollTotalWidth, (float) this.mDimBarWidth);
            this.mHScrollMarkerTranslation.setPosition(hScrollLeft, hScrollTop);
            this.mHScrollMarkerScale.setScale(hScrollWidth, (float) this.mDimBarWidth);
            return;
        }
        this.mHScroll.hide();
    }

    public synchronized void setResolution(int x, int y) {
        this.mResX = x;
        this.mResY = y;
    }

    public void onAdded() {
        super.onAdded();
        this.mRenderable.onAdded();
    }

    public void onRemoved() {
        this.mRenderable.onRemoved();
        super.onRemoved();
    }

    public synchronized boolean isAnimating() {
        return this.mIsVisible;
    }

    public synchronized void showBars() {
        this.mShowUntil = System.currentTimeMillis() + 600;
        this.mIsVisible = true;
        this.mCurrentAlpha = 1.0f;
    }

    /* access modifiers changed from: protected */
    public synchronized void renderInternal(RRGLMatrixStack stack, long time) {
        if (this.mIsVisible && time > this.mShowUntil) {
            this.mCurrentAlpha -= ALPHA_STEP;
            if (this.mCurrentAlpha < 0.0f) {
                this.mIsVisible = false;
                this.mCurrentAlpha = 0.0f;
            }
        }
        this.mRenderable.setOverallAlpha(this.mCurrentAlpha);
        this.mRenderable.startRender(stack, time);
    }
}
