package org.quantumbadger.redreader.views.imageview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import java.lang.reflect.Array;
import java.util.Arrays;
import org.quantumbadger.redreader.common.MutableFloatPoint2D;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.UIThreadRepeatingTimer;
import org.quantumbadger.redreader.common.collections.Stack;
import org.quantumbadger.redreader.views.glview.Refreshable;
import org.quantumbadger.redreader.views.glview.displaylist.RRGLDisplayList;
import org.quantumbadger.redreader.views.glview.displaylist.RRGLDisplayListRenderer.DisplayListManager;
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableGroup;
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableScale;
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableTexturedQuad;
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableTranslation;
import org.quantumbadger.redreader.views.glview.program.RRGLContext;
import org.quantumbadger.redreader.views.glview.program.RRGLTexture;
import org.quantumbadger.redreader.views.imageview.FingerTracker.Finger;

public class ImageViewDisplayListManager implements DisplayListManager, org.quantumbadger.redreader.common.UIThreadRepeatingTimer.Listener, org.quantumbadger.redreader.views.imageview.ImageViewTileLoader.Listener {
    private static final long DOUBLE_TAP_MAX_GAP_DURATION_MS = 275;
    private static final long TAP_MAX_DURATION_MS = 225;
    private BoundsHelper mBoundsHelper = null;
    private final CoordinateHelper mCoordinateHelper = new CoordinateHelper();
    private TouchState mCurrentTouchState = null;
    private final UIThreadRepeatingTimer mDoubleTapGapTimer = new UIThreadRepeatingTimer(50, this);
    private Finger mDragFinger;
    private long mFirstTapReleaseTime = -1;
    private final int mHTileCount;
    private final ImageTileSource mImageTileSource;
    private int mLastSampleSize = 1;
    private final Listener mListener;
    private final int mLoadingCheckerboardDarkCol;
    private final int mLoadingCheckerboardLightCol;
    private RRGLTexture mNotLoadedTexture;
    private RRGLRenderableScale mOverallScale;
    private RRGLRenderableTranslation mOverallTranslation;
    private Finger mPinchFinger1;
    private Finger mPinchFinger2;
    private Refreshable mRefreshable;
    private int mResolutionX;
    private int mResolutionY;
    private ImageViewScaleAnimation mScaleAnimation = null;
    private float mScreenDensity = 1.0f;
    private ImageViewScrollbars mScrollbars;
    private final Stack<Finger> mSpareFingers = new Stack<>(8);
    private boolean[][] mTileLoaded;
    private final MultiScaleTileManager[][] mTileLoaders;
    private final int mTileSize;
    private boolean[][] mTileVisibility;
    private final RRGLRenderableTexturedQuad[][] mTiles;
    private final MutableFloatPoint2D mTmpPoint1_onFingersMoved = new MutableFloatPoint2D();
    private final MutableFloatPoint2D mTmpPoint2_onFingersMoved = new MutableFloatPoint2D();
    private final int mVTileCount;

    public interface Listener extends org.quantumbadger.redreader.views.imageview.BasicGestureHandler.Listener {
        void onImageViewDLMException(Throwable th);

        void onImageViewDLMOutOfMemory();
    }

    private enum TouchState {
        ONE_FINGER_DOWN,
        ONE_FINGER_DRAG,
        TWO_FINGER_PINCH,
        DOUBLE_TAP_WAIT_NO_FINGERS_DOWN,
        DOUBLE_TAP_ONE_FINGER_DOWN,
        DOUBLE_TAP_ONE_FINGER_DRAG
    }

    public ImageViewDisplayListManager(Context context, ImageTileSource imageTileSource, Listener listener) {
        this.mImageTileSource = imageTileSource;
        this.mListener = listener;
        this.mHTileCount = this.mImageTileSource.getHTileCount();
        this.mVTileCount = this.mImageTileSource.getVTileCount();
        this.mTileSize = this.mImageTileSource.getTileSize();
        this.mTiles = (RRGLRenderableTexturedQuad[][]) Array.newInstance(RRGLRenderableTexturedQuad.class, new int[]{this.mHTileCount, this.mVTileCount});
        this.mTileLoaders = (MultiScaleTileManager[][]) Array.newInstance(MultiScaleTileManager.class, new int[]{this.mHTileCount, this.mVTileCount});
        ImageViewTileLoaderThread thread = new ImageViewTileLoaderThread();
        for (int x = 0; x < this.mHTileCount; x++) {
            for (int y = 0; y < this.mVTileCount; y++) {
                MultiScaleTileManager[] multiScaleTileManagerArr = this.mTileLoaders[x];
                MultiScaleTileManager multiScaleTileManager = new MultiScaleTileManager(imageTileSource, thread, x, y, this);
                multiScaleTileManagerArr[y] = multiScaleTileManager;
            }
        }
        if (PrefsUtility.isNightMode(context) != 0) {
            this.mLoadingCheckerboardDarkCol = Color.rgb(70, 70, 70);
            this.mLoadingCheckerboardLightCol = Color.rgb(110, 110, 110);
            return;
        }
        this.mLoadingCheckerboardDarkCol = Color.rgb(150, 150, 150);
        this.mLoadingCheckerboardLightCol = -1;
    }

    public synchronized void onGLSceneCreate(RRGLDisplayList scene, RRGLContext glContext, Refreshable refreshable) {
        RRGLDisplayList rRGLDisplayList = scene;
        RRGLContext rRGLContext = glContext;
        synchronized (this) {
            this.mTileVisibility = (boolean[][]) Array.newInstance(boolean.class, new int[]{this.mHTileCount, this.mVTileCount});
            this.mTileLoaded = (boolean[][]) Array.newInstance(boolean.class, new int[]{this.mHTileCount, this.mVTileCount});
            this.mRefreshable = refreshable;
            this.mScreenDensity = glContext.getScreenDensity();
            Bitmap notLoadedBitmap = Bitmap.createBitmap(256, 256, Config.ARGB_8888);
            Canvas notLoadedCanvas = new Canvas(notLoadedBitmap);
            Paint lightPaint = new Paint();
            Paint darkPaint = new Paint();
            lightPaint.setColor(this.mLoadingCheckerboardLightCol);
            darkPaint.setColor(this.mLoadingCheckerboardDarkCol);
            for (int x = 0; x < 4; x++) {
                for (int y = 0; y < 4; y++) {
                    notLoadedCanvas.drawRect((float) (x * 64), (float) (y * 64), (float) ((x + 1) * 64), (float) ((y + 1) * 64), ((x ^ y) & 1) == 0 ? lightPaint : darkPaint);
                }
            }
            this.mNotLoadedTexture = new RRGLTexture(rRGLContext, notLoadedBitmap);
            RRGLRenderableGroup group = new RRGLRenderableGroup();
            this.mOverallScale = new RRGLRenderableScale(group);
            this.mOverallTranslation = new RRGLRenderableTranslation(this.mOverallScale);
            rRGLDisplayList.add(this.mOverallTranslation);
            int i = 0;
            int x2 = 0;
            while (x2 < this.mHTileCount) {
                for (int y2 = i; y2 < this.mVTileCount; y2++) {
                    RRGLRenderableTexturedQuad quad = new RRGLRenderableTexturedQuad(rRGLContext, this.mNotLoadedTexture);
                    this.mTiles[x2][y2] = quad;
                    RRGLRenderableTranslation translation = new RRGLRenderableTranslation(quad);
                    translation.setPosition((float) x2, (float) y2);
                    RRGLRenderableScale scale = new RRGLRenderableScale(translation);
                    scale.setScale((float) this.mTileSize, (float) this.mTileSize);
                    group.add(scale);
                }
                x2++;
                i = 0;
            }
            this.mScrollbars = new ImageViewScrollbars(rRGLContext, this.mCoordinateHelper, this.mImageTileSource.getWidth(), this.mImageTileSource.getHeight());
            rRGLDisplayList.add(this.mScrollbars);
        }
    }

    public synchronized void onGLSceneResolutionChange(RRGLDisplayList scene, RRGLContext context, int width, int height) {
        this.mResolutionX = width;
        this.mResolutionY = height;
        boolean setInitialScale = this.mBoundsHelper == null;
        BoundsHelper boundsHelper = new BoundsHelper(width, height, this.mImageTileSource.getWidth(), this.mImageTileSource.getHeight(), this.mCoordinateHelper);
        this.mBoundsHelper = boundsHelper;
        if (setInitialScale) {
            this.mBoundsHelper.applyMinScale();
        }
        this.mScrollbars.setResolution(width, height);
        this.mScrollbars.showBars();
    }

    public synchronized boolean onGLSceneUpdate(RRGLDisplayList scene, RRGLContext context) {
        boolean z;
        float scale;
        int sampleSize;
        synchronized (this) {
            if (this.mScaleAnimation != null && !this.mScaleAnimation.onStep()) {
                this.mScaleAnimation = null;
            }
            if (this.mBoundsHelper != null) {
                this.mBoundsHelper.applyBounds();
            }
            MutableFloatPoint2D positionOffset = this.mCoordinateHelper.getPositionOffset();
            float scale2 = this.mCoordinateHelper.getScale();
            this.mOverallTranslation.setPosition(positionOffset);
            this.mOverallScale.setScale(scale2, scale2);
            this.mScrollbars.update();
            int sampleSize2 = pickSampleSize();
            int y = 0;
            if (this.mLastSampleSize != sampleSize2) {
                for (boolean[] arr : this.mTileLoaded) {
                    Arrays.fill(arr, false);
                }
                this.mLastSampleSize = sampleSize2;
            }
            float firstVisiblePixelX = (-positionOffset.x) / scale2;
            float firstVisiblePixelY = (-positionOffset.y) / scale2;
            int firstVisibleTileX = (int) Math.floor((double) (firstVisiblePixelX / ((float) this.mTileSize)));
            int firstVisibleTileY = (int) Math.floor((double) (firstVisiblePixelY / ((float) this.mTileSize)));
            int lastVisibleTileX = (int) Math.ceil((double) ((firstVisiblePixelX + (((float) this.mResolutionX) / scale2)) / ((float) this.mTileSize)));
            int lastVisibleTileY = (int) Math.ceil((double) ((firstVisiblePixelY + (((float) this.mResolutionY) / scale2)) / ((float) this.mTileSize)));
            int desiredScaleIndex = MultiScaleTileManager.sampleSizeToScaleIndex(sampleSize2);
            int x = 0;
            while (true) {
                z = true;
                if (x < this.mHTileCount) {
                    int i = y;
                    while (y < this.mVTileCount) {
                        boolean isTileVisible = x >= firstVisibleTileX && y >= firstVisibleTileY && x <= lastVisibleTileX && y <= lastVisibleTileY;
                        boolean isTileWanted = x >= firstVisibleTileX + -1 && y >= firstVisibleTileY + -1 && x <= lastVisibleTileX + 1 && y <= lastVisibleTileY + 1;
                        if (!isTileWanted || this.mTileLoaded[x][y]) {
                            this.mTileLoaders[x][y].markAsUnwanted();
                        } else {
                            this.mTileLoaders[x][y].markAsWanted(desiredScaleIndex);
                        }
                        MutableFloatPoint2D positionOffset2 = positionOffset;
                        boolean isTileVisible2 = isTileVisible;
                        if (isTileVisible2 == this.mTileVisibility[x][y]) {
                            if (this.mTileLoaded[x][y]) {
                                scale = scale2;
                                sampleSize = sampleSize2;
                                y++;
                                sampleSize2 = sampleSize;
                                positionOffset = positionOffset2;
                                scale2 = scale;
                            }
                        }
                        if (!isTileVisible2 || this.mTileLoaded[x][y]) {
                            scale = scale2;
                            sampleSize = sampleSize2;
                            if (!isTileWanted) {
                                this.mTiles[x][y].setTexture(this.mNotLoadedTexture);
                            }
                        } else {
                            scale = scale2;
                            Bitmap tile = this.mTileLoaders[x][y].getAtDesiredScale();
                            if (tile != null) {
                                try {
                                    sampleSize = sampleSize2;
                                    try {
                                        RRGLTexture texture = new RRGLTexture(context, tile);
                                        this.mTiles[x][y].setTexture(texture);
                                        texture.releaseReference();
                                        this.mTileLoaded[x][y] = true;
                                        tile.recycle();
                                    } catch (Exception e) {
                                        e = e;
                                    }
                                } catch (Exception e2) {
                                    e = e2;
                                    sampleSize = sampleSize2;
                                    Bitmap bitmap = tile;
                                    Log.e("ImageViewDisplayListMan", "Exception when creating texture", e);
                                    this.mTileVisibility[x][y] = isTileVisible2;
                                    y++;
                                    sampleSize2 = sampleSize;
                                    positionOffset = positionOffset2;
                                    scale2 = scale;
                                }
                            } else {
                                sampleSize = sampleSize2;
                            }
                        }
                        this.mTileVisibility[x][y] = isTileVisible2;
                        y++;
                        sampleSize2 = sampleSize;
                        positionOffset = positionOffset2;
                        scale2 = scale;
                    }
                    float f = scale2;
                    int i2 = sampleSize2;
                    x++;
                    y = 0;
                } else {
                    float f2 = scale2;
                    int i3 = sampleSize2;
                    if (this.mScaleAnimation != null) {
                        this.mScrollbars.showBars();
                    }
                    if (this.mScaleAnimation == null) {
                        z = false;
                    }
                }
            }
        }
        return z;
    }

    public void onUIAttach() {
    }

    public void onUIDetach() {
        this.mImageTileSource.dispose();
    }

    /* JADX WARNING: Code restructure failed: missing block: B:19:0x004c, code lost:
        return;
     */
    public synchronized void onFingerDown(Finger finger) {
        if (this.mScrollbars != null) {
            this.mScaleAnimation = null;
            this.mScrollbars.showBars();
            if (this.mCurrentTouchState == null) {
                this.mCurrentTouchState = TouchState.ONE_FINGER_DOWN;
                this.mDragFinger = finger;
            } else {
                switch (this.mCurrentTouchState) {
                    case DOUBLE_TAP_WAIT_NO_FINGERS_DOWN:
                        this.mCurrentTouchState = TouchState.DOUBLE_TAP_ONE_FINGER_DOWN;
                        this.mDragFinger = finger;
                        this.mDoubleTapGapTimer.stopTimer();
                        break;
                    case ONE_FINGER_DRAG:
                        this.mListener.onHorizontalSwipeEnd();
                        break;
                    case ONE_FINGER_DOWN:
                    case DOUBLE_TAP_ONE_FINGER_DOWN:
                    case DOUBLE_TAP_ONE_FINGER_DRAG:
                        break;
                    default:
                        this.mSpareFingers.push(finger);
                        break;
                }
                this.mCurrentTouchState = TouchState.TWO_FINGER_PINCH;
                this.mPinchFinger1 = this.mDragFinger;
                this.mPinchFinger2 = finger;
                this.mDragFinger = null;
            }
        }
    }

    /* Debug info: failed to restart local var, previous not found, register: 10 */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x00ee, code lost:
        return;
     */
    public synchronized void onFingersMoved() {
        if (this.mCurrentTouchState != null) {
            if (this.mScrollbars != null) {
                this.mScaleAnimation = null;
                this.mScrollbars.showBars();
                switch (this.mCurrentTouchState) {
                    case ONE_FINGER_DRAG:
                        break;
                    case ONE_FINGER_DOWN:
                        if (this.mDragFinger.mTotalPosDifference.distanceSquared() >= this.mScreenDensity * 100.0f * this.mScreenDensity) {
                            this.mCurrentTouchState = TouchState.ONE_FINGER_DRAG;
                            break;
                        }
                        break;
                    case DOUBLE_TAP_ONE_FINGER_DOWN:
                        if (this.mDragFinger.mTotalPosDifference.distanceSquared() >= this.mScreenDensity * 400.0f * this.mScreenDensity) {
                            this.mCurrentTouchState = TouchState.DOUBLE_TAP_ONE_FINGER_DRAG;
                            break;
                        }
                        break;
                    case DOUBLE_TAP_ONE_FINGER_DRAG:
                        MutableFloatPoint2D screenCentre = this.mTmpPoint1_onFingersMoved;
                        screenCentre.set((float) (this.mResolutionX / 2), (float) (this.mResolutionY / 2));
                        this.mCoordinateHelper.scaleAboutScreenPoint(screenCentre, (float) Math.pow(1.01d, (double) (this.mDragFinger.mPosDifference.y / this.mScreenDensity)));
                        break;
                    case TWO_FINGER_PINCH:
                        double oldDistance = this.mPinchFinger1.mLastPos.euclideanDistanceTo(this.mPinchFinger2.mLastPos);
                        double newDistance = this.mPinchFinger1.mCurrentPos.euclideanDistanceTo(this.mPinchFinger2.mCurrentPos);
                        MutableFloatPoint2D oldCentre = this.mTmpPoint1_onFingersMoved;
                        this.mPinchFinger1.mLastPos.add(this.mPinchFinger2.mLastPos, oldCentre);
                        oldCentre.scale(0.5d);
                        MutableFloatPoint2D newCentre = this.mTmpPoint2_onFingersMoved;
                        this.mPinchFinger1.mCurrentPos.add(this.mPinchFinger2.mCurrentPos, newCentre);
                        newCentre.scale(0.5d);
                        this.mCoordinateHelper.scaleAboutScreenPoint(newCentre, (float) (newDistance / oldDistance));
                        this.mCoordinateHelper.translateScreen(oldCentre, newCentre);
                        break;
                }
                if (this.mBoundsHelper.isMinScale()) {
                    this.mListener.onHorizontalSwipe(this.mDragFinger.mTotalPosDifference.x);
                } else {
                    this.mCoordinateHelper.translateScreen(this.mDragFinger.mLastPos, this.mDragFinger.mCurrentPos);
                }
            }
        }
    }

    /* Debug info: failed to restart local var, previous not found, register: 6 */
    /* JADX WARNING: Code restructure failed: missing block: B:47:0x00af, code lost:
        return;
     */
    public synchronized void onFingerUp(Finger finger) {
        if (this.mScrollbars != null) {
            this.mScaleAnimation = null;
            this.mScrollbars.showBars();
            if (!this.mSpareFingers.remove(finger)) {
                if (this.mCurrentTouchState != null) {
                    switch (this.mCurrentTouchState) {
                        case ONE_FINGER_DRAG:
                            this.mListener.onHorizontalSwipeEnd();
                            break;
                        case ONE_FINGER_DOWN:
                            if (finger.mDownDuration < TAP_MAX_DURATION_MS) {
                                this.mDoubleTapGapTimer.startTimer();
                                this.mCurrentTouchState = TouchState.DOUBLE_TAP_WAIT_NO_FINGERS_DOWN;
                                this.mFirstTapReleaseTime = System.currentTimeMillis();
                            } else {
                                this.mCurrentTouchState = null;
                            }
                            this.mDragFinger = null;
                            break;
                        case DOUBLE_TAP_ONE_FINGER_DOWN:
                            if (finger.mDownDuration < TAP_MAX_DURATION_MS) {
                                onDoubleTap(finger.mCurrentPos);
                            }
                            this.mCurrentTouchState = null;
                            this.mDragFinger = null;
                            break;
                        case DOUBLE_TAP_ONE_FINGER_DRAG:
                            break;
                        case TWO_FINGER_PINCH:
                            if (!this.mSpareFingers.isEmpty()) {
                                if (this.mPinchFinger1 != finger) {
                                    this.mPinchFinger2 = (Finger) this.mSpareFingers.pop();
                                    break;
                                } else {
                                    this.mPinchFinger1 = (Finger) this.mSpareFingers.pop();
                                    break;
                                }
                            } else {
                                this.mCurrentTouchState = TouchState.ONE_FINGER_DRAG;
                                this.mDragFinger = this.mPinchFinger1 == finger ? this.mPinchFinger2 : this.mPinchFinger1;
                                this.mPinchFinger1 = null;
                                this.mPinchFinger2 = null;
                                break;
                            }
                    }
                    if (this.mSpareFingers.isEmpty()) {
                        this.mCurrentTouchState = null;
                        this.mDragFinger = null;
                    } else {
                        this.mDragFinger = (Finger) this.mSpareFingers.pop();
                    }
                }
            }
        }
    }

    private void onDoubleTap(MutableFloatPoint2D position) {
        float targetScale;
        float minScale = this.mBoundsHelper.getMinScale();
        float currentScale = this.mCoordinateHelper.getScale();
        double d = (double) currentScale;
        double d2 = (double) minScale;
        Double.isNaN(d2);
        if (d > d2 * 1.01d) {
            targetScale = minScale;
        } else {
            targetScale = Math.max(((float) this.mResolutionX) / ((float) this.mImageTileSource.getWidth()), ((float) this.mResolutionY) / ((float) this.mImageTileSource.getHeight()));
            double d3 = (double) (targetScale / currentScale);
            Double.isNaN(d3);
            if (Math.abs(d3 - 1.0d) < 0.05d) {
                targetScale = currentScale * 3.0f;
            }
        }
        this.mScaleAnimation = new ImageViewScaleAnimation(targetScale, this.mCoordinateHelper, 15, position);
    }

    public void onUIThreadRepeatingTimer(UIThreadRepeatingTimer timer) {
        if (this.mCurrentTouchState != TouchState.DOUBLE_TAP_WAIT_NO_FINGERS_DOWN) {
            this.mDoubleTapGapTimer.stopTimer();
        } else if (System.currentTimeMillis() - this.mFirstTapReleaseTime > DOUBLE_TAP_MAX_GAP_DURATION_MS) {
            this.mListener.onSingleTap();
            this.mCurrentTouchState = null;
            this.mDoubleTapGapTimer.stopTimer();
        }
    }

    private int pickSampleSize() {
        int result = 1;
        while (result <= 32) {
            double d = (double) (result * 2);
            Double.isNaN(d);
            if (1.0d / d <= ((double) this.mCoordinateHelper.getScale())) {
                break;
            }
            result *= 2;
        }
        return result;
    }

    public void onTileLoaded(int x, int y, int sampleSize) {
        this.mRefreshable.refresh();
    }

    public void onTileLoaderOutOfMemory() {
        this.mListener.onImageViewDLMOutOfMemory();
    }

    public void onTileLoaderException(Throwable t) {
        this.mListener.onImageViewDLMException(t);
    }

    public void resetTouchState() {
        this.mCurrentTouchState = null;
    }
}
