package com.google.android.exoplayer2.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ui.TimeBar.OnScrubListener;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArraySet;

public class DefaultTimeBar extends View implements TimeBar {
    private static final String ACCESSIBILITY_CLASS_NAME = "android.widget.SeekBar";
    public static final int DEFAULT_AD_MARKER_COLOR = -1291845888;
    public static final int DEFAULT_AD_MARKER_WIDTH_DP = 4;
    public static final int DEFAULT_BAR_HEIGHT_DP = 4;
    private static final int DEFAULT_INCREMENT_COUNT = 20;
    public static final int DEFAULT_PLAYED_COLOR = -1;
    public static final int DEFAULT_SCRUBBER_DISABLED_SIZE_DP = 0;
    public static final int DEFAULT_SCRUBBER_DRAGGED_SIZE_DP = 16;
    public static final int DEFAULT_SCRUBBER_ENABLED_SIZE_DP = 12;
    public static final int DEFAULT_TOUCH_TARGET_HEIGHT_DP = 26;
    private static final int FINE_SCRUB_RATIO = 3;
    private static final int FINE_SCRUB_Y_THRESHOLD_DP = -50;
    private static final long STOP_SCRUBBING_TIMEOUT_MS = 1000;
    private int adGroupCount;
    @Nullable
    private long[] adGroupTimesMs;
    private final Paint adMarkerPaint = new Paint();
    private final int adMarkerWidth;
    private final int barHeight;
    private final Rect bufferedBar = new Rect();
    private final Paint bufferedPaint = new Paint();
    private long bufferedPosition;
    private long duration;
    private final int fineScrubYThreshold;
    private final StringBuilder formatBuilder;
    private final Formatter formatter;
    private int keyCountIncrement;
    private long keyTimeIncrement;
    private int lastCoarseScrubXPosition;
    private final CopyOnWriteArraySet<OnScrubListener> listeners;
    private final int[] locationOnScreen;
    @Nullable
    private boolean[] playedAdGroups;
    private final Paint playedAdMarkerPaint = new Paint();
    private final Paint playedPaint = new Paint();
    private long position;
    private final Rect progressBar = new Rect();
    private long scrubPosition;
    private final Rect scrubberBar = new Rect();
    private final int scrubberDisabledSize;
    private final int scrubberDraggedSize;
    @Nullable
    private final Drawable scrubberDrawable;
    private final int scrubberEnabledSize;
    private final int scrubberPadding;
    private final Paint scrubberPaint = new Paint();
    private boolean scrubbing;
    private final Rect seekBounds = new Rect();
    private final Runnable stopScrubbingRunnable;
    private final Point touchPosition;
    private final int touchTargetHeight;
    private final Paint unplayedPaint = new Paint();

    public DefaultTimeBar(Context context, AttributeSet attrs) {
        AttributeSet attributeSet = attrs;
        super(context, attrs);
        this.scrubberPaint.setAntiAlias(true);
        this.listeners = new CopyOnWriteArraySet<>();
        this.locationOnScreen = new int[2];
        this.touchPosition = new Point();
        Resources res = context.getResources();
        DisplayMetrics displayMetrics = res.getDisplayMetrics();
        this.fineScrubYThreshold = dpToPx(displayMetrics, FINE_SCRUB_Y_THRESHOLD_DP);
        int defaultBarHeight = dpToPx(displayMetrics, 4);
        int defaultTouchTargetHeight = dpToPx(displayMetrics, 26);
        int defaultAdMarkerWidth = dpToPx(displayMetrics, 4);
        int defaultScrubberEnabledSize = dpToPx(displayMetrics, 12);
        int defaultScrubberDisabledSize = dpToPx(displayMetrics, 0);
        int defaultScrubberDraggedSize = dpToPx(displayMetrics, 16);
        if (attributeSet != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.DefaultTimeBar, 0, 0);
            try {
                this.scrubberDrawable = a.getDrawable(R.styleable.DefaultTimeBar_scrubber_drawable);
                if (this.scrubberDrawable != null) {
                    try {
                        setDrawableLayoutDirection(this.scrubberDrawable);
                        defaultTouchTargetHeight = Math.max(this.scrubberDrawable.getMinimumHeight(), defaultTouchTargetHeight);
                    } catch (Throwable th) {
                        th = th;
                        Resources resources = res;
                        a.recycle();
                        throw th;
                    }
                }
                this.barHeight = a.getDimensionPixelSize(R.styleable.DefaultTimeBar_bar_height, defaultBarHeight);
                this.touchTargetHeight = a.getDimensionPixelSize(R.styleable.DefaultTimeBar_touch_target_height, defaultTouchTargetHeight);
                this.adMarkerWidth = a.getDimensionPixelSize(R.styleable.DefaultTimeBar_ad_marker_width, defaultAdMarkerWidth);
                this.scrubberEnabledSize = a.getDimensionPixelSize(R.styleable.DefaultTimeBar_scrubber_enabled_size, defaultScrubberEnabledSize);
                this.scrubberDisabledSize = a.getDimensionPixelSize(R.styleable.DefaultTimeBar_scrubber_disabled_size, defaultScrubberDisabledSize);
                this.scrubberDraggedSize = a.getDimensionPixelSize(R.styleable.DefaultTimeBar_scrubber_dragged_size, defaultScrubberDraggedSize);
                int playedColor = a.getInt(R.styleable.DefaultTimeBar_played_color, -1);
                int scrubberColor = a.getInt(R.styleable.DefaultTimeBar_scrubber_color, getDefaultScrubberColor(playedColor));
                int bufferedColor = a.getInt(R.styleable.DefaultTimeBar_buffered_color, getDefaultBufferedColor(playedColor));
                int unplayedColor = a.getInt(R.styleable.DefaultTimeBar_unplayed_color, getDefaultUnplayedColor(playedColor));
                int adMarkerColor = a.getInt(R.styleable.DefaultTimeBar_ad_marker_color, DEFAULT_AD_MARKER_COLOR);
                Resources resources2 = res;
                try {
                    int playedAdMarkerColor = a.getInt(R.styleable.DefaultTimeBar_played_ad_marker_color, getDefaultPlayedAdMarkerColor(adMarkerColor));
                    this.playedPaint.setColor(playedColor);
                    this.scrubberPaint.setColor(scrubberColor);
                    this.bufferedPaint.setColor(bufferedColor);
                    this.unplayedPaint.setColor(unplayedColor);
                    this.adMarkerPaint.setColor(adMarkerColor);
                    this.playedAdMarkerPaint.setColor(playedAdMarkerColor);
                    a.recycle();
                } catch (Throwable th2) {
                    th = th2;
                    a.recycle();
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                Resources resources3 = res;
                a.recycle();
                throw th;
            }
        } else {
            this.barHeight = defaultBarHeight;
            this.touchTargetHeight = defaultTouchTargetHeight;
            this.adMarkerWidth = defaultAdMarkerWidth;
            this.scrubberEnabledSize = defaultScrubberEnabledSize;
            this.scrubberDisabledSize = defaultScrubberDisabledSize;
            this.scrubberDraggedSize = defaultScrubberDraggedSize;
            this.playedPaint.setColor(-1);
            this.scrubberPaint.setColor(getDefaultScrubberColor(-1));
            this.bufferedPaint.setColor(getDefaultBufferedColor(-1));
            this.unplayedPaint.setColor(getDefaultUnplayedColor(-1));
            this.adMarkerPaint.setColor(DEFAULT_AD_MARKER_COLOR);
            this.scrubberDrawable = null;
        }
        this.formatBuilder = new StringBuilder();
        this.formatter = new Formatter(this.formatBuilder, Locale.getDefault());
        this.stopScrubbingRunnable = new Runnable() {
            public final void run() {
                DefaultTimeBar.this.stopScrubbing(false);
            }
        };
        Drawable drawable = this.scrubberDrawable;
        if (drawable != null) {
            this.scrubberPadding = (drawable.getMinimumWidth() + 1) / 2;
        } else {
            this.scrubberPadding = (Math.max(this.scrubberDisabledSize, Math.max(this.scrubberEnabledSize, this.scrubberDraggedSize)) + 1) / 2;
        }
        this.duration = C.TIME_UNSET;
        this.keyTimeIncrement = C.TIME_UNSET;
        this.keyCountIncrement = 20;
        setFocusable(true);
        if (Util.SDK_INT >= 16) {
            maybeSetImportantForAccessibilityV16();
        }
    }

    public void setPlayedColor(@ColorInt int playedColor) {
        this.playedPaint.setColor(playedColor);
        invalidate(this.seekBounds);
    }

    public void setScrubberColor(@ColorInt int scrubberColor) {
        this.scrubberPaint.setColor(scrubberColor);
        invalidate(this.seekBounds);
    }

    public void setBufferedColor(@ColorInt int bufferedColor) {
        this.bufferedPaint.setColor(bufferedColor);
        invalidate(this.seekBounds);
    }

    public void setUnplayedColor(@ColorInt int unplayedColor) {
        this.unplayedPaint.setColor(unplayedColor);
        invalidate(this.seekBounds);
    }

    public void setAdMarkerColor(@ColorInt int adMarkerColor) {
        this.adMarkerPaint.setColor(adMarkerColor);
        invalidate(this.seekBounds);
    }

    public void setPlayedAdMarkerColor(@ColorInt int playedAdMarkerColor) {
        this.playedAdMarkerPaint.setColor(playedAdMarkerColor);
        invalidate(this.seekBounds);
    }

    public void addListener(OnScrubListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(OnScrubListener listener) {
        this.listeners.remove(listener);
    }

    public void setKeyTimeIncrement(long time) {
        Assertions.checkArgument(time > 0);
        this.keyCountIncrement = -1;
        this.keyTimeIncrement = time;
    }

    public void setKeyCountIncrement(int count) {
        Assertions.checkArgument(count > 0);
        this.keyCountIncrement = count;
        this.keyTimeIncrement = C.TIME_UNSET;
    }

    public void setPosition(long position2) {
        this.position = position2;
        setContentDescription(getProgressText());
        update();
    }

    public void setBufferedPosition(long bufferedPosition2) {
        this.bufferedPosition = bufferedPosition2;
        update();
    }

    public void setDuration(long duration2) {
        this.duration = duration2;
        if (this.scrubbing && duration2 == C.TIME_UNSET) {
            stopScrubbing(true);
        }
        update();
    }

    public void setAdGroupTimesMs(@Nullable long[] adGroupTimesMs2, @Nullable boolean[] playedAdGroups2, int adGroupCount2) {
        Assertions.checkArgument(adGroupCount2 == 0 || !(adGroupTimesMs2 == null || playedAdGroups2 == null));
        this.adGroupCount = adGroupCount2;
        this.adGroupTimesMs = adGroupTimesMs2;
        this.playedAdGroups = playedAdGroups2;
        update();
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (this.scrubbing && !enabled) {
            stopScrubbing(true);
        }
    }

    public void onDraw(Canvas canvas) {
        canvas.save();
        drawTimeBar(canvas);
        drawPlayhead(canvas);
        canvas.restore();
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean z = false;
        if (!isEnabled() || this.duration <= 0) {
            return false;
        }
        Point touchPosition2 = resolveRelativeTouchPosition(event);
        int x = touchPosition2.x;
        int y = touchPosition2.y;
        switch (event.getAction()) {
            case 0:
                if (isInSeekBar((float) x, (float) y)) {
                    positionScrubber((float) x);
                    startScrubbing();
                    this.scrubPosition = getScrubberPosition();
                    update();
                    invalidate();
                    return true;
                }
                break;
            case 1:
            case 3:
                if (this.scrubbing) {
                    if (event.getAction() == 3) {
                        z = true;
                    }
                    stopScrubbing(z);
                    return true;
                }
                break;
            case 2:
                if (this.scrubbing) {
                    if (y < this.fineScrubYThreshold) {
                        int i = this.lastCoarseScrubXPosition;
                        positionScrubber((float) (i + ((x - i) / 3)));
                    } else {
                        this.lastCoarseScrubXPosition = x;
                        positionScrubber((float) x);
                    }
                    this.scrubPosition = getScrubberPosition();
                    Iterator it = this.listeners.iterator();
                    while (it.hasNext()) {
                        ((OnScrubListener) it.next()).onScrubMove(this, this.scrubPosition);
                    }
                    update();
                    invalidate();
                    return true;
                }
                break;
        }
        return false;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:7:0x0018, code lost:
        if (scrubIncrementally(r0) == false) goto L_0x0036;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:8:0x001a, code lost:
        removeCallbacks(r6.stopScrubbingRunnable);
        postDelayed(r6.stopScrubbingRunnable, 1000);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:9:0x0026, code lost:
        return true;
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (isEnabled()) {
            long positionIncrement = getPositionIncrement();
            if (keyCode != 66) {
                switch (keyCode) {
                    case 21:
                        positionIncrement = -positionIncrement;
                        break;
                    case 22:
                        break;
                    case 23:
                        break;
                }
            }
            if (this.scrubbing) {
                removeCallbacks(this.stopScrubbingRunnable);
                this.stopScrubbingRunnable.run();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /* access modifiers changed from: protected */
    public void drawableStateChanged() {
        super.drawableStateChanged();
        updateDrawableState();
    }

    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        Drawable drawable = this.scrubberDrawable;
        if (drawable != null) {
            drawable.jumpToCurrentState();
        }
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int height = heightMode == 0 ? this.touchTargetHeight : heightMode == 1073741824 ? heightSize : Math.min(this.touchTargetHeight, heightSize);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
        updateDrawableState();
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int barY = ((bottom - top) - this.touchTargetHeight) / 2;
        int seekLeft = getPaddingLeft();
        int seekRight = width - getPaddingRight();
        int i = this.touchTargetHeight;
        int progressY = ((i - this.barHeight) / 2) + barY;
        this.seekBounds.set(seekLeft, barY, seekRight, i + barY);
        this.progressBar.set(this.seekBounds.left + this.scrubberPadding, progressY, this.seekBounds.right - this.scrubberPadding, this.barHeight + progressY);
        update();
    }

    public void onRtlPropertiesChanged(int layoutDirection) {
        Drawable drawable = this.scrubberDrawable;
        if (drawable != null && setDrawableLayoutDirection(drawable, layoutDirection)) {
            invalidate();
        }
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (event.getEventType() == 4) {
            event.getText().add(getProgressText());
        }
        event.setClassName(ACCESSIBILITY_CLASS_NAME);
    }

    @TargetApi(21)
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info2) {
        super.onInitializeAccessibilityNodeInfo(info2);
        info2.setClassName(ACCESSIBILITY_CLASS_NAME);
        info2.setContentDescription(getProgressText());
        if (this.duration > 0) {
            if (Util.SDK_INT >= 21) {
                info2.addAction(AccessibilityAction.ACTION_SCROLL_FORWARD);
                info2.addAction(AccessibilityAction.ACTION_SCROLL_BACKWARD);
            } else if (Util.SDK_INT >= 16) {
                info2.addAction(4096);
                info2.addAction(8192);
            }
        }
    }

    @TargetApi(16)
    public boolean performAccessibilityAction(int action, @Nullable Bundle args) {
        if (super.performAccessibilityAction(action, args)) {
            return true;
        }
        if (this.duration <= 0) {
            return false;
        }
        if (action == 8192) {
            if (scrubIncrementally(-getPositionIncrement())) {
                stopScrubbing(false);
            }
        } else if (action != 4096) {
            return false;
        } else {
            if (scrubIncrementally(getPositionIncrement())) {
                stopScrubbing(false);
            }
        }
        sendAccessibilityEvent(4);
        return true;
    }

    @TargetApi(16)
    private void maybeSetImportantForAccessibilityV16() {
        if (getImportantForAccessibility() == 0) {
            setImportantForAccessibility(1);
        }
    }

    private void startScrubbing() {
        this.scrubbing = true;
        setPressed(true);
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
        Iterator it = this.listeners.iterator();
        while (it.hasNext()) {
            ((OnScrubListener) it.next()).onScrubStart(this, getScrubberPosition());
        }
    }

    /* access modifiers changed from: private */
    public void stopScrubbing(boolean canceled) {
        this.scrubbing = false;
        setPressed(false);
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(false);
        }
        invalidate();
        Iterator it = this.listeners.iterator();
        while (it.hasNext()) {
            ((OnScrubListener) it.next()).onScrubStop(this, getScrubberPosition(), canceled);
        }
    }

    private void update() {
        this.bufferedBar.set(this.progressBar);
        this.scrubberBar.set(this.progressBar);
        long newScrubberTime = this.scrubbing ? this.scrubPosition : this.position;
        if (this.duration > 0) {
            this.bufferedBar.right = Math.min(this.progressBar.left + ((int) ((((long) this.progressBar.width()) * this.bufferedPosition) / this.duration)), this.progressBar.right);
            this.scrubberBar.right = Math.min(this.progressBar.left + ((int) ((((long) this.progressBar.width()) * newScrubberTime) / this.duration)), this.progressBar.right);
        } else {
            this.bufferedBar.right = this.progressBar.left;
            this.scrubberBar.right = this.progressBar.left;
        }
        invalidate(this.seekBounds);
    }

    private void positionScrubber(float xPosition) {
        this.scrubberBar.right = Util.constrainValue((int) xPosition, this.progressBar.left, this.progressBar.right);
    }

    private Point resolveRelativeTouchPosition(MotionEvent motionEvent) {
        getLocationOnScreen(this.locationOnScreen);
        this.touchPosition.set(((int) motionEvent.getRawX()) - this.locationOnScreen[0], ((int) motionEvent.getRawY()) - this.locationOnScreen[1]);
        return this.touchPosition;
    }

    private long getScrubberPosition() {
        if (this.progressBar.width() <= 0 || this.duration == C.TIME_UNSET) {
            return 0;
        }
        return (((long) this.scrubberBar.width()) * this.duration) / ((long) this.progressBar.width());
    }

    private boolean isInSeekBar(float x, float y) {
        return this.seekBounds.contains((int) x, (int) y);
    }

    private void drawTimeBar(Canvas canvas) {
        DefaultTimeBar defaultTimeBar = this;
        int progressBarHeight = defaultTimeBar.progressBar.height();
        int barTop = defaultTimeBar.progressBar.centerY() - (progressBarHeight / 2);
        int barBottom = barTop + progressBarHeight;
        if (defaultTimeBar.duration <= 0) {
            canvas.drawRect((float) defaultTimeBar.progressBar.left, (float) barTop, (float) defaultTimeBar.progressBar.right, (float) barBottom, defaultTimeBar.unplayedPaint);
            return;
        }
        int bufferedLeft = defaultTimeBar.bufferedBar.left;
        int bufferedRight = defaultTimeBar.bufferedBar.right;
        int progressLeft = Math.max(Math.max(defaultTimeBar.progressBar.left, bufferedRight), defaultTimeBar.scrubberBar.right);
        if (progressLeft < defaultTimeBar.progressBar.right) {
            canvas.drawRect((float) progressLeft, (float) barTop, (float) defaultTimeBar.progressBar.right, (float) barBottom, defaultTimeBar.unplayedPaint);
        }
        int bufferedLeft2 = Math.max(bufferedLeft, defaultTimeBar.scrubberBar.right);
        if (bufferedRight > bufferedLeft2) {
            canvas.drawRect((float) bufferedLeft2, (float) barTop, (float) bufferedRight, (float) barBottom, defaultTimeBar.bufferedPaint);
        }
        if (defaultTimeBar.scrubberBar.width() > 0) {
            canvas.drawRect((float) defaultTimeBar.scrubberBar.left, (float) barTop, (float) defaultTimeBar.scrubberBar.right, (float) barBottom, defaultTimeBar.playedPaint);
        }
        if (defaultTimeBar.adGroupCount != 0) {
            long[] adGroupTimesMs2 = (long[]) Assertions.checkNotNull(defaultTimeBar.adGroupTimesMs);
            boolean[] playedAdGroups2 = (boolean[]) Assertions.checkNotNull(defaultTimeBar.playedAdGroups);
            int adMarkerOffset = defaultTimeBar.adMarkerWidth / 2;
            int i = 0;
            while (i < defaultTimeBar.adGroupCount) {
                int bufferedLeft3 = bufferedLeft2;
                int bufferedRight2 = bufferedRight;
                int markerLeft = defaultTimeBar.progressBar.left + Math.min(defaultTimeBar.progressBar.width() - defaultTimeBar.adMarkerWidth, Math.max(0, ((int) ((((long) defaultTimeBar.progressBar.width()) * Util.constrainValue(adGroupTimesMs2[i], 0, defaultTimeBar.duration)) / defaultTimeBar.duration)) - adMarkerOffset));
                int progressBarHeight2 = progressBarHeight;
                canvas.drawRect((float) markerLeft, (float) barTop, (float) (defaultTimeBar.adMarkerWidth + markerLeft), (float) barBottom, playedAdGroups2[i] ? defaultTimeBar.playedAdMarkerPaint : defaultTimeBar.adMarkerPaint);
                i++;
                bufferedRight = bufferedRight2;
                progressBarHeight = progressBarHeight2;
                bufferedLeft2 = bufferedLeft3;
                defaultTimeBar = this;
            }
            int i2 = bufferedLeft2;
            int i3 = bufferedRight;
        }
    }

    private void drawPlayhead(Canvas canvas) {
        if (this.duration > 0) {
            int playheadX = Util.constrainValue(this.scrubberBar.right, this.scrubberBar.left, this.progressBar.right);
            int playheadY = this.scrubberBar.centerY();
            Drawable drawable = this.scrubberDrawable;
            if (drawable == null) {
                int scrubberSize = (this.scrubbing || isFocused()) ? this.scrubberDraggedSize : isEnabled() ? this.scrubberEnabledSize : this.scrubberDisabledSize;
                canvas.drawCircle((float) playheadX, (float) playheadY, (float) (scrubberSize / 2), this.scrubberPaint);
            } else {
                int scrubberDrawableWidth = drawable.getIntrinsicWidth();
                int scrubberDrawableHeight = this.scrubberDrawable.getIntrinsicHeight();
                this.scrubberDrawable.setBounds(playheadX - (scrubberDrawableWidth / 2), playheadY - (scrubberDrawableHeight / 2), (scrubberDrawableWidth / 2) + playheadX, (scrubberDrawableHeight / 2) + playheadY);
                this.scrubberDrawable.draw(canvas);
            }
        }
    }

    private void updateDrawableState() {
        Drawable drawable = this.scrubberDrawable;
        if (drawable != null && drawable.isStateful() && this.scrubberDrawable.setState(getDrawableState())) {
            invalidate();
        }
    }

    private String getProgressText() {
        return Util.getStringForTime(this.formatBuilder, this.formatter, this.position);
    }

    private long getPositionIncrement() {
        long j = this.keyTimeIncrement;
        if (j != C.TIME_UNSET) {
            return j;
        }
        long j2 = this.duration;
        if (j2 == C.TIME_UNSET) {
            return 0;
        }
        return j2 / ((long) this.keyCountIncrement);
    }

    private boolean scrubIncrementally(long positionChange) {
        if (this.duration <= 0) {
            return false;
        }
        long scrubberPosition = getScrubberPosition();
        this.scrubPosition = Util.constrainValue(scrubberPosition + positionChange, 0, this.duration);
        if (this.scrubPosition == scrubberPosition) {
            return false;
        }
        if (!this.scrubbing) {
            startScrubbing();
        }
        Iterator it = this.listeners.iterator();
        while (it.hasNext()) {
            ((OnScrubListener) it.next()).onScrubMove(this, this.scrubPosition);
        }
        update();
        return true;
    }

    private boolean setDrawableLayoutDirection(Drawable drawable) {
        return Util.SDK_INT >= 23 && setDrawableLayoutDirection(drawable, getLayoutDirection());
    }

    private static boolean setDrawableLayoutDirection(Drawable drawable, int layoutDirection) {
        return Util.SDK_INT >= 23 && drawable.setLayoutDirection(layoutDirection);
    }

    public static int getDefaultScrubberColor(int playedColor) {
        return -16777216 | playedColor;
    }

    public static int getDefaultUnplayedColor(int playedColor) {
        return (16777215 & playedColor) | 855638016;
    }

    public static int getDefaultBufferedColor(int playedColor) {
        return (16777215 & playedColor) | -872415232;
    }

    public static int getDefaultPlayedAdMarkerColor(int adMarkerColor) {
        return (16777215 & adMarkerColor) | 855638016;
    }

    private static int dpToPx(DisplayMetrics displayMetrics, int dps) {
        return (int) ((((float) dps) * displayMetrics.density) + 0.5f);
    }
}
