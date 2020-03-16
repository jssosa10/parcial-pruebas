package android.support.v7.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.InputDeviceCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class LinearLayoutCompat extends ViewGroup {
    public static final int HORIZONTAL = 0;
    private static final int INDEX_BOTTOM = 2;
    private static final int INDEX_CENTER_VERTICAL = 0;
    private static final int INDEX_FILL = 3;
    private static final int INDEX_TOP = 1;
    public static final int SHOW_DIVIDER_BEGINNING = 1;
    public static final int SHOW_DIVIDER_END = 4;
    public static final int SHOW_DIVIDER_MIDDLE = 2;
    public static final int SHOW_DIVIDER_NONE = 0;
    public static final int VERTICAL = 1;
    private static final int VERTICAL_GRAVITY_COUNT = 4;
    private boolean mBaselineAligned;
    private int mBaselineAlignedChildIndex;
    private int mBaselineChildTop;
    private Drawable mDivider;
    private int mDividerHeight;
    private int mDividerPadding;
    private int mDividerWidth;
    private int mGravity;
    private int[] mMaxAscent;
    private int[] mMaxDescent;
    private int mOrientation;
    private int mShowDividers;
    private int mTotalLength;
    private boolean mUseLargestChild;
    private float mWeightSum;

    @RestrictTo({Scope.LIBRARY_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface DividerMode {
    }

    public static class LayoutParams extends MarginLayoutParams {
        public int gravity;
        public float weight;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.gravity = -1;
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.LinearLayoutCompat_Layout);
            this.weight = a.getFloat(R.styleable.LinearLayoutCompat_Layout_android_layout_weight, 0.0f);
            this.gravity = a.getInt(R.styleable.LinearLayoutCompat_Layout_android_layout_gravity, -1);
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
            this.gravity = -1;
            this.weight = 0.0f;
        }

        public LayoutParams(int width, int height, float weight2) {
            super(width, height);
            this.gravity = -1;
            this.weight = weight2;
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams p) {
            super(p);
            this.gravity = -1;
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
            this.gravity = -1;
        }

        public LayoutParams(LayoutParams source) {
            super(source);
            this.gravity = -1;
            this.weight = source.weight;
            this.gravity = source.gravity;
        }
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OrientationMode {
    }

    public LinearLayoutCompat(Context context) {
        this(context, null);
    }

    public LinearLayoutCompat(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinearLayoutCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mBaselineAligned = true;
        this.mBaselineAlignedChildIndex = -1;
        this.mBaselineChildTop = 0;
        this.mGravity = 8388659;
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs, R.styleable.LinearLayoutCompat, defStyleAttr, 0);
        int index = a.getInt(R.styleable.LinearLayoutCompat_android_orientation, -1);
        if (index >= 0) {
            setOrientation(index);
        }
        int index2 = a.getInt(R.styleable.LinearLayoutCompat_android_gravity, -1);
        if (index2 >= 0) {
            setGravity(index2);
        }
        boolean baselineAligned = a.getBoolean(R.styleable.LinearLayoutCompat_android_baselineAligned, true);
        if (!baselineAligned) {
            setBaselineAligned(baselineAligned);
        }
        this.mWeightSum = a.getFloat(R.styleable.LinearLayoutCompat_android_weightSum, -1.0f);
        this.mBaselineAlignedChildIndex = a.getInt(R.styleable.LinearLayoutCompat_android_baselineAlignedChildIndex, -1);
        this.mUseLargestChild = a.getBoolean(R.styleable.LinearLayoutCompat_measureWithLargestChild, false);
        setDividerDrawable(a.getDrawable(R.styleable.LinearLayoutCompat_divider));
        this.mShowDividers = a.getInt(R.styleable.LinearLayoutCompat_showDividers, 0);
        this.mDividerPadding = a.getDimensionPixelSize(R.styleable.LinearLayoutCompat_dividerPadding, 0);
        a.recycle();
    }

    public void setShowDividers(int showDividers) {
        if (showDividers != this.mShowDividers) {
            requestLayout();
        }
        this.mShowDividers = showDividers;
    }

    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public int getShowDividers() {
        return this.mShowDividers;
    }

    public Drawable getDividerDrawable() {
        return this.mDivider;
    }

    public void setDividerDrawable(Drawable divider) {
        if (divider != this.mDivider) {
            this.mDivider = divider;
            boolean z = false;
            if (divider != null) {
                this.mDividerWidth = divider.getIntrinsicWidth();
                this.mDividerHeight = divider.getIntrinsicHeight();
            } else {
                this.mDividerWidth = 0;
                this.mDividerHeight = 0;
            }
            if (divider == null) {
                z = true;
            }
            setWillNotDraw(z);
            requestLayout();
        }
    }

    public void setDividerPadding(int padding) {
        this.mDividerPadding = padding;
    }

    public int getDividerPadding() {
        return this.mDividerPadding;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    public int getDividerWidth() {
        return this.mDividerWidth;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        if (this.mDivider != null) {
            if (this.mOrientation == 1) {
                drawDividersVertical(canvas);
            } else {
                drawDividersHorizontal(canvas);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void drawDividersVertical(Canvas canvas) {
        int bottom;
        int count = getVirtualChildCount();
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (!(child == null || child.getVisibility() == 8 || !hasDividerBeforeChildAt(i))) {
                drawHorizontalDivider(canvas, (child.getTop() - ((LayoutParams) child.getLayoutParams()).topMargin) - this.mDividerHeight);
            }
        }
        if (hasDividerBeforeChildAt(count) != 0) {
            View child2 = getVirtualChildAt(count - 1);
            if (child2 == null) {
                bottom = (getHeight() - getPaddingBottom()) - this.mDividerHeight;
            } else {
                bottom = child2.getBottom() + ((LayoutParams) child2.getLayoutParams()).bottomMargin;
            }
            drawHorizontalDivider(canvas, bottom);
        }
    }

    /* access modifiers changed from: 0000 */
    public void drawDividersHorizontal(Canvas canvas) {
        int position;
        int position2;
        int count = getVirtualChildCount();
        boolean isLayoutRtl = ViewUtils.isLayoutRtl(this);
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (!(child == null || child.getVisibility() == 8 || !hasDividerBeforeChildAt(i))) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (isLayoutRtl) {
                    position2 = child.getRight() + lp.rightMargin;
                } else {
                    position2 = (child.getLeft() - lp.leftMargin) - this.mDividerWidth;
                }
                drawVerticalDivider(canvas, position2);
            }
        }
        if (hasDividerBeforeChildAt(count) != 0) {
            View child2 = getVirtualChildAt(count - 1);
            if (child2 != null) {
                LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
                if (isLayoutRtl) {
                    position = (child2.getLeft() - lp2.leftMargin) - this.mDividerWidth;
                } else {
                    position = child2.getRight() + lp2.rightMargin;
                }
            } else if (isLayoutRtl) {
                position = getPaddingLeft();
            } else {
                position = (getWidth() - getPaddingRight()) - this.mDividerWidth;
            }
            drawVerticalDivider(canvas, position);
        }
    }

    /* access modifiers changed from: 0000 */
    public void drawHorizontalDivider(Canvas canvas, int top) {
        this.mDivider.setBounds(getPaddingLeft() + this.mDividerPadding, top, (getWidth() - getPaddingRight()) - this.mDividerPadding, this.mDividerHeight + top);
        this.mDivider.draw(canvas);
    }

    /* access modifiers changed from: 0000 */
    public void drawVerticalDivider(Canvas canvas, int left) {
        this.mDivider.setBounds(left, getPaddingTop() + this.mDividerPadding, this.mDividerWidth + left, (getHeight() - getPaddingBottom()) - this.mDividerPadding);
        this.mDivider.draw(canvas);
    }

    public boolean isBaselineAligned() {
        return this.mBaselineAligned;
    }

    public void setBaselineAligned(boolean baselineAligned) {
        this.mBaselineAligned = baselineAligned;
    }

    public boolean isMeasureWithLargestChildEnabled() {
        return this.mUseLargestChild;
    }

    public void setMeasureWithLargestChildEnabled(boolean enabled) {
        this.mUseLargestChild = enabled;
    }

    public int getBaseline() {
        if (this.mBaselineAlignedChildIndex < 0) {
            return super.getBaseline();
        }
        int childCount = getChildCount();
        int i = this.mBaselineAlignedChildIndex;
        if (childCount > i) {
            View child = getChildAt(i);
            int childBaseline = child.getBaseline();
            if (childBaseline != -1) {
                int childTop = this.mBaselineChildTop;
                if (this.mOrientation == 1) {
                    int majorGravity = this.mGravity & 112;
                    if (majorGravity != 48) {
                        if (majorGravity == 16) {
                            childTop += ((((getBottom() - getTop()) - getPaddingTop()) - getPaddingBottom()) - this.mTotalLength) / 2;
                        } else if (majorGravity == 80) {
                            childTop = ((getBottom() - getTop()) - getPaddingBottom()) - this.mTotalLength;
                        }
                    }
                }
                return ((LayoutParams) child.getLayoutParams()).topMargin + childTop + childBaseline;
            } else if (this.mBaselineAlignedChildIndex == 0) {
                return -1;
            } else {
                throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout points to a View that doesn't know how to get its baseline.");
            }
        } else {
            throw new RuntimeException("mBaselineAlignedChildIndex of LinearLayout set to an index that is out of bounds.");
        }
    }

    public int getBaselineAlignedChildIndex() {
        return this.mBaselineAlignedChildIndex;
    }

    public void setBaselineAlignedChildIndex(int i) {
        if (i < 0 || i >= getChildCount()) {
            StringBuilder sb = new StringBuilder();
            sb.append("base aligned child index out of range (0, ");
            sb.append(getChildCount());
            sb.append(")");
            throw new IllegalArgumentException(sb.toString());
        }
        this.mBaselineAlignedChildIndex = i;
    }

    /* access modifiers changed from: 0000 */
    public View getVirtualChildAt(int index) {
        return getChildAt(index);
    }

    /* access modifiers changed from: 0000 */
    public int getVirtualChildCount() {
        return getChildCount();
    }

    public float getWeightSum() {
        return this.mWeightSum;
    }

    public void setWeightSum(float weightSum) {
        this.mWeightSum = Math.max(0.0f, weightSum);
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mOrientation == 1) {
            measureVertical(widthMeasureSpec, heightMeasureSpec);
        } else {
            measureHorizontal(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /* access modifiers changed from: protected */
    @RestrictTo({Scope.LIBRARY})
    public boolean hasDividerBeforeChildAt(int childIndex) {
        boolean z = false;
        if (childIndex == 0) {
            if ((this.mShowDividers & 1) != 0) {
                z = true;
            }
            return z;
        } else if (childIndex == getChildCount()) {
            if ((this.mShowDividers & 4) != 0) {
                z = true;
            }
            return z;
        } else if ((this.mShowDividers & 2) == 0) {
            return false;
        } else {
            boolean hasVisibleViewBefore = false;
            int i = childIndex - 1;
            while (true) {
                if (i < 0) {
                    break;
                } else if (getChildAt(i).getVisibility() != 8) {
                    hasVisibleViewBefore = true;
                    break;
                } else {
                    i--;
                }
            }
            return hasVisibleViewBefore;
        }
    }

    /* access modifiers changed from: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:171:0x045d  */
    /* JADX WARNING: Removed duplicated region for block: B:189:? A[RETURN, SYNTHETIC] */
    public void measureVertical(int widthMeasureSpec, int heightMeasureSpec) {
        int count;
        int heightMode;
        int maxWidth;
        int alternativeMaxWidth;
        int largestChildHeight;
        int maxWidth2;
        float totalWeight;
        int delta;
        int heightMode2;
        int baselineChildIndex;
        float weightSum;
        int delta2;
        boolean allFillParent;
        int delta3;
        int maxWidth3;
        float totalWeight2;
        int delta4;
        int i;
        int i2;
        int heightMode3;
        int weightedMaxWidth;
        int alternativeMaxWidth2;
        int maxWidth4;
        int weightedMaxWidth2;
        int heightMode4;
        int childState;
        LayoutParams lp;
        View child;
        int i3;
        int weightedMaxWidth3;
        int alternativeMaxWidth3;
        int oldHeight;
        int i4 = widthMeasureSpec;
        int i5 = heightMeasureSpec;
        this.mTotalLength = 0;
        int margin = 0;
        float totalWeight3 = 0.0f;
        int count2 = getVirtualChildCount();
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode5 = MeasureSpec.getMode(heightMeasureSpec);
        int baselineChildIndex2 = this.mBaselineAlignedChildIndex;
        int useLargestChild = this.mUseLargestChild;
        boolean matchWidth = false;
        int childState2 = 0;
        int alternativeMaxWidth4 = 0;
        int measuredWidth = 0;
        int weightedMaxWidth4 = 0;
        boolean skippedMeasure = false;
        int i6 = 0;
        boolean allFillParent2 = true;
        while (true) {
            int weightedMaxWidth5 = measuredWidth;
            int i7 = 8;
            if (i6 < count2) {
                View child2 = getVirtualChildAt(i6);
                if (child2 == null) {
                    this.mTotalLength += measureNullChild(i6);
                    heightMode3 = heightMode5;
                    measuredWidth = weightedMaxWidth5;
                    weightedMaxWidth = count2;
                } else {
                    int childState3 = margin;
                    if (child2.getVisibility() == 8) {
                        i6 += getChildrenSkipCount(child2, i6);
                        heightMode3 = heightMode5;
                        measuredWidth = weightedMaxWidth5;
                        margin = childState3;
                        weightedMaxWidth = count2;
                    } else {
                        if (hasDividerBeforeChildAt(i6)) {
                            this.mTotalLength += this.mDividerHeight;
                        }
                        LayoutParams lp2 = (LayoutParams) child2.getLayoutParams();
                        float totalWeight4 = totalWeight3 + lp2.weight;
                        if (heightMode5 == 1073741824 && lp2.height == 0 && lp2.weight > 0.0f) {
                            int totalLength = this.mTotalLength;
                            int maxWidth5 = alternativeMaxWidth4;
                            this.mTotalLength = Math.max(totalLength, lp2.topMargin + totalLength + lp2.bottomMargin);
                            skippedMeasure = true;
                            lp = lp2;
                            alternativeMaxWidth2 = childState2;
                            heightMode3 = heightMode5;
                            weightedMaxWidth2 = weightedMaxWidth5;
                            childState = childState3;
                            maxWidth4 = maxWidth5;
                            heightMode4 = weightedMaxWidth4;
                            child = child2;
                            weightedMaxWidth = count2;
                        } else {
                            int maxWidth6 = alternativeMaxWidth4;
                            if (lp2.height != 0 || lp2.weight <= 0.0f) {
                                oldHeight = Integer.MIN_VALUE;
                            } else {
                                lp2.height = -2;
                                oldHeight = 0;
                            }
                            lp = lp2;
                            childState = childState3;
                            maxWidth4 = maxWidth6;
                            heightMode3 = heightMode5;
                            heightMode4 = weightedMaxWidth4;
                            View child3 = child2;
                            weightedMaxWidth2 = weightedMaxWidth5;
                            weightedMaxWidth = count2;
                            int oldHeight2 = oldHeight;
                            alternativeMaxWidth2 = childState2;
                            measureChildBeforeLayout(child2, i6, widthMeasureSpec, 0, heightMeasureSpec, totalWeight4 == 0.0f ? this.mTotalLength : 0);
                            if (oldHeight2 != Integer.MIN_VALUE) {
                                lp.height = oldHeight2;
                            }
                            int childHeight = child3.getMeasuredHeight();
                            int totalLength2 = this.mTotalLength;
                            child = child3;
                            this.mTotalLength = Math.max(totalLength2, totalLength2 + childHeight + lp.topMargin + lp.bottomMargin + getNextLocationOffset(child));
                            if (useLargestChild == true) {
                                heightMode4 = Math.max(childHeight, heightMode4);
                            }
                        }
                        if (baselineChildIndex2 >= 0 && baselineChildIndex2 == i6 + 1) {
                            this.mBaselineChildTop = this.mTotalLength;
                        }
                        if (i6 >= baselineChildIndex2 || lp.weight <= 0.0f) {
                            boolean matchWidthLocally = false;
                            if (widthMode != 1073741824) {
                                i3 = -1;
                                if (lp.width == -1) {
                                    matchWidth = true;
                                    matchWidthLocally = true;
                                }
                            } else {
                                i3 = -1;
                            }
                            int margin2 = lp.leftMargin + lp.rightMargin;
                            int measuredWidth2 = child.getMeasuredWidth() + margin2;
                            int maxWidth7 = Math.max(maxWidth4, measuredWidth2);
                            int childState4 = View.combineMeasuredStates(childState, child.getMeasuredState());
                            boolean allFillParent3 = allFillParent2 && lp.width == i3;
                            if (lp.weight > 0.0f) {
                                weightedMaxWidth3 = Math.max(weightedMaxWidth2, matchWidthLocally ? margin2 : measuredWidth2);
                                allFillParent2 = allFillParent3;
                                alternativeMaxWidth3 = alternativeMaxWidth2;
                            } else {
                                int weightedMaxWidth6 = weightedMaxWidth2;
                                allFillParent2 = allFillParent3;
                                alternativeMaxWidth3 = Math.max(alternativeMaxWidth2, matchWidthLocally ? margin2 : measuredWidth2);
                                weightedMaxWidth3 = weightedMaxWidth6;
                            }
                            i6 += getChildrenSkipCount(child, i6);
                            margin = childState4;
                            measuredWidth = weightedMaxWidth3;
                            weightedMaxWidth4 = heightMode4;
                            childState2 = alternativeMaxWidth3;
                            alternativeMaxWidth4 = maxWidth7;
                            totalWeight3 = totalWeight4;
                        } else {
                            throw new RuntimeException("A child of LinearLayout with index less than mBaselineAlignedChildIndex has weight > 0, which won't work.  Either remove the weight, or don't set mBaselineAlignedChildIndex.");
                        }
                    }
                }
                i6++;
                count2 = weightedMaxWidth;
                heightMode5 = heightMode3;
                int i8 = widthMeasureSpec;
                int i9 = heightMeasureSpec;
            } else {
                int childState5 = margin;
                int heightMode6 = heightMode5;
                int largestChildHeight2 = weightedMaxWidth4;
                int i10 = childState2;
                int maxWidth8 = alternativeMaxWidth4;
                int maxWidth9 = i10;
                int i11 = weightedMaxWidth5;
                int weightedMaxWidth7 = count2;
                int weightedMaxWidth8 = i11;
                if (this.mTotalLength > 0) {
                    count = weightedMaxWidth7;
                    if (hasDividerBeforeChildAt(count)) {
                        this.mTotalLength += this.mDividerHeight;
                    }
                } else {
                    count = weightedMaxWidth7;
                }
                if (useLargestChild != 0) {
                    heightMode = heightMode6;
                    if (heightMode == Integer.MIN_VALUE || heightMode == 0) {
                        this.mTotalLength = 0;
                        int i12 = 0;
                        while (i12 < count) {
                            View child4 = getVirtualChildAt(i12);
                            if (child4 == null) {
                                this.mTotalLength += measureNullChild(i12);
                                i2 = i12;
                            } else if (child4.getVisibility() == i7) {
                                i = i12 + getChildrenSkipCount(child4, i12);
                                i12 = i + 1;
                                i7 = 8;
                            } else {
                                LayoutParams lp3 = (LayoutParams) child4.getLayoutParams();
                                int totalLength3 = this.mTotalLength;
                                i2 = i12;
                                this.mTotalLength = Math.max(totalLength3, totalLength3 + largestChildHeight2 + lp3.topMargin + lp3.bottomMargin + getNextLocationOffset(child4));
                            }
                            i = i2;
                            i12 = i + 1;
                            i7 = 8;
                        }
                        int i13 = i12;
                    }
                } else {
                    heightMode = heightMode6;
                }
                this.mTotalLength += getPaddingTop() + getPaddingBottom();
                int i14 = heightMeasureSpec;
                int heightSizeAndState = View.resolveSizeAndState(Math.max(this.mTotalLength, getSuggestedMinimumHeight()), i14, 0);
                int heightSize = heightSizeAndState & ViewCompat.MEASURED_SIZE_MASK;
                int delta5 = heightSize - this.mTotalLength;
                if (skippedMeasure) {
                    delta = delta5;
                    totalWeight = totalWeight3;
                    maxWidth2 = maxWidth8;
                } else if (delta5 == 0 || totalWeight3 <= 0.0f) {
                    int alternativeMaxWidth5 = Math.max(maxWidth9, weightedMaxWidth8);
                    if (useLargestChild != 0) {
                        int i15 = heightSize;
                        if (heightMode != 1073741824) {
                            int i16 = 0;
                            while (i16 < count) {
                                int alternativeMaxWidth6 = alternativeMaxWidth5;
                                View child5 = getVirtualChildAt(i16);
                                if (child5 != null) {
                                    delta4 = delta5;
                                    totalWeight2 = totalWeight3;
                                    if (child5.getVisibility() == 8) {
                                        maxWidth3 = maxWidth8;
                                    } else {
                                        LayoutParams lp4 = (LayoutParams) child5.getLayoutParams();
                                        float childExtra = lp4.weight;
                                        if (childExtra > 0.0f) {
                                            LayoutParams layoutParams = lp4;
                                            float f = childExtra;
                                            maxWidth3 = maxWidth8;
                                            child5.measure(MeasureSpec.makeMeasureSpec(child5.getMeasuredWidth(), 1073741824), MeasureSpec.makeMeasureSpec(largestChildHeight2, 1073741824));
                                        } else {
                                            float f2 = childExtra;
                                            maxWidth3 = maxWidth8;
                                        }
                                    }
                                } else {
                                    delta4 = delta5;
                                    totalWeight2 = totalWeight3;
                                    maxWidth3 = maxWidth8;
                                }
                                i16++;
                                alternativeMaxWidth5 = alternativeMaxWidth6;
                                delta5 = delta4;
                                totalWeight3 = totalWeight2;
                                maxWidth8 = maxWidth3;
                            }
                            alternativeMaxWidth = alternativeMaxWidth5;
                            delta3 = delta5;
                            float f3 = totalWeight3;
                            maxWidth = maxWidth8;
                        } else {
                            alternativeMaxWidth = alternativeMaxWidth5;
                            delta3 = delta5;
                            float f4 = totalWeight3;
                            maxWidth = maxWidth8;
                        }
                    } else {
                        alternativeMaxWidth = alternativeMaxWidth5;
                        delta3 = delta5;
                        float f5 = totalWeight3;
                        maxWidth = maxWidth8;
                    }
                    int i17 = heightMode;
                    int i18 = weightedMaxWidth8;
                    int i19 = largestChildHeight2;
                    int i20 = baselineChildIndex2;
                    int i21 = delta3;
                    largestChildHeight = widthMeasureSpec;
                    int delta6 = useLargestChild;
                    if (!allFillParent2 && widthMode != 1073741824) {
                        maxWidth = alternativeMaxWidth;
                    }
                    setMeasuredDimension(View.resolveSizeAndState(Math.max(maxWidth + getPaddingLeft() + getPaddingRight(), getSuggestedMinimumWidth()), largestChildHeight, childState5), heightSizeAndState);
                    if (!matchWidth) {
                        forceUniformWidth(count, i14);
                        return;
                    }
                    return;
                } else {
                    int i22 = heightSize;
                    delta = delta5;
                    totalWeight = totalWeight3;
                    maxWidth2 = maxWidth8;
                }
                float totalWeight5 = this.mWeightSum;
                if (totalWeight5 <= 0.0f) {
                    totalWeight5 = totalWeight;
                }
                float weightSum2 = totalWeight5;
                this.mTotalLength = 0;
                int i23 = 0;
                int alternativeMaxWidth7 = maxWidth9;
                int measuredWidth3 = delta;
                int maxWidth10 = maxWidth2;
                while (i23 < count) {
                    boolean useLargestChild2 = useLargestChild;
                    View child6 = getVirtualChildAt(i23);
                    int weightedMaxWidth9 = weightedMaxWidth8;
                    int largestChildHeight3 = largestChildHeight2;
                    if (child6.getVisibility() == 8) {
                        heightMode2 = heightMode;
                        baselineChildIndex = baselineChildIndex2;
                        int i24 = widthMeasureSpec;
                    } else {
                        LayoutParams lp5 = (LayoutParams) child6.getLayoutParams();
                        float childExtra2 = lp5.weight;
                        if (childExtra2 > 0.0f) {
                            baselineChildIndex = baselineChildIndex2;
                            int share = (int) ((((float) measuredWidth3) * childExtra2) / weightSum2);
                            weightSum = weightSum2 - childExtra2;
                            delta2 = measuredWidth3 - share;
                            float f6 = childExtra2;
                            int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight() + lp5.leftMargin + lp5.rightMargin, lp5.width);
                            if (lp5.height != 0) {
                                heightMode2 = heightMode;
                            } else if (heightMode != 1073741824) {
                                heightMode2 = heightMode;
                            } else {
                                heightMode2 = heightMode;
                                child6.measure(childWidthMeasureSpec, MeasureSpec.makeMeasureSpec(share > 0 ? share : 0, 1073741824));
                                int i25 = share;
                                childState5 = View.combineMeasuredStates(childState5, child6.getMeasuredState() & InputDeviceCompat.SOURCE_ANY);
                            }
                            int childHeight2 = child6.getMeasuredHeight() + share;
                            if (childHeight2 < 0) {
                                childHeight2 = 0;
                            }
                            int i26 = share;
                            child6.measure(childWidthMeasureSpec, MeasureSpec.makeMeasureSpec(childHeight2, 1073741824));
                            childState5 = View.combineMeasuredStates(childState5, child6.getMeasuredState() & InputDeviceCompat.SOURCE_ANY);
                        } else {
                            heightMode2 = heightMode;
                            float f7 = childExtra2;
                            baselineChildIndex = baselineChildIndex2;
                            int i27 = widthMeasureSpec;
                            weightSum = weightSum2;
                            delta2 = measuredWidth3;
                        }
                        int margin3 = lp5.leftMargin + lp5.rightMargin;
                        int measuredWidth4 = child6.getMeasuredWidth() + margin3;
                        int maxWidth11 = Math.max(maxWidth10, measuredWidth4);
                        alternativeMaxWidth7 = Math.max(alternativeMaxWidth7, widthMode != 1073741824 && lp5.width == -1 ? margin3 : measuredWidth4);
                        if (allFillParent2) {
                            int i28 = margin3;
                            if (lp5.width == -1) {
                                allFillParent = true;
                                int totalLength4 = this.mTotalLength;
                                int maxWidth12 = maxWidth11;
                                this.mTotalLength = Math.max(totalLength4, totalLength4 + child6.getMeasuredHeight() + lp5.topMargin + lp5.bottomMargin + getNextLocationOffset(child6));
                                allFillParent2 = allFillParent;
                                measuredWidth3 = delta2;
                                weightSum2 = weightSum;
                                maxWidth10 = maxWidth12;
                            }
                        }
                        allFillParent = false;
                        int totalLength42 = this.mTotalLength;
                        int maxWidth122 = maxWidth11;
                        this.mTotalLength = Math.max(totalLength42, totalLength42 + child6.getMeasuredHeight() + lp5.topMargin + lp5.bottomMargin + getNextLocationOffset(child6));
                        allFillParent2 = allFillParent;
                        measuredWidth3 = delta2;
                        weightSum2 = weightSum;
                        maxWidth10 = maxWidth122;
                    }
                    i23++;
                    useLargestChild = useLargestChild2;
                    weightedMaxWidth8 = weightedMaxWidth9;
                    largestChildHeight2 = largestChildHeight3;
                    baselineChildIndex2 = baselineChildIndex;
                    heightMode = heightMode2;
                }
                boolean z = useLargestChild;
                int i29 = weightedMaxWidth8;
                int i30 = largestChildHeight2;
                int i31 = baselineChildIndex2;
                largestChildHeight = widthMeasureSpec;
                this.mTotalLength += getPaddingTop() + getPaddingBottom();
                maxWidth = maxWidth10;
                alternativeMaxWidth = alternativeMaxWidth7;
                maxWidth = alternativeMaxWidth;
                setMeasuredDimension(View.resolveSizeAndState(Math.max(maxWidth + getPaddingLeft() + getPaddingRight(), getSuggestedMinimumWidth()), largestChildHeight, childState5), heightSizeAndState);
                if (!matchWidth) {
                }
            }
        }
    }

    private void forceUniformWidth(int count, int heightMeasureSpec) {
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), 1073741824);
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.width == -1) {
                    int oldHeight = lp.height;
                    lp.height = child.getMeasuredHeight();
                    measureChildWithMargins(child, uniformMeasureSpec, 0, heightMeasureSpec, 0);
                    lp.height = oldHeight;
                }
            }
        }
    }

    /* access modifiers changed from: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:197:0x0540  */
    /* JADX WARNING: Removed duplicated region for block: B:205:0x0578  */
    /* JADX WARNING: Removed duplicated region for block: B:225:0x062b  */
    /* JADX WARNING: Removed duplicated region for block: B:226:0x0633  */
    public void measureHorizontal(int widthMeasureSpec, int heightMeasureSpec) {
        int childState;
        int maxHeight;
        int widthMode;
        int childState2;
        int widthSizeAndState;
        int alternativeMaxHeight;
        int delta;
        int maxHeight2;
        int childState3;
        float totalWeight;
        int widthSize;
        int widthMode2;
        int count;
        boolean useLargestChild;
        int widthSizeAndState2;
        int widthMode3;
        float weightSum;
        int i;
        int alternativeMaxHeight2;
        boolean allFillParent;
        int childState4;
        int largestChildWidth;
        int widthSize2;
        int alternativeMaxHeight3;
        int i2;
        int maxHeight3;
        int largestChildWidth2;
        boolean baselineAligned;
        int weightedMaxHeight;
        int maxHeight4;
        int alternativeMaxHeight4;
        int weightedMaxHeight2;
        int widthMode4;
        int i3;
        int largestChildWidth3;
        LayoutParams lp;
        int largestChildWidth4;
        int margin;
        int weightedMaxHeight3;
        int alternativeMaxHeight5;
        int oldWidth;
        int alternativeMaxHeight6;
        int i4 = widthMeasureSpec;
        int i5 = heightMeasureSpec;
        this.mTotalLength = 0;
        int count2 = getVirtualChildCount();
        int widthMode5 = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (this.mMaxAscent == null || this.mMaxDescent == null) {
            this.mMaxAscent = new int[4];
            this.mMaxDescent = new int[4];
        }
        int[] maxAscent = this.mMaxAscent;
        int[] maxDescent = this.mMaxDescent;
        maxAscent[3] = -1;
        maxAscent[2] = -1;
        maxAscent[1] = -1;
        maxAscent[0] = -1;
        maxDescent[3] = -1;
        maxDescent[2] = -1;
        maxDescent[1] = -1;
        maxDescent[0] = -1;
        boolean baselineAligned2 = this.mBaselineAligned;
        boolean skippedMeasure = false;
        int useLargestChild2 = this.mUseLargestChild;
        boolean isExactly = widthMode5 == 1073741824;
        int childState5 = 0;
        int alternativeMaxHeight7 = 0;
        boolean matchHeight = false;
        boolean allFillParent2 = true;
        int childHeight = 0;
        float totalWeight2 = 0.0f;
        int i6 = 0;
        int weightedMaxHeight4 = 0;
        int maxHeight5 = 0;
        while (i6 < count2) {
            View child = getVirtualChildAt(i6);
            if (child == null) {
                int largestChildWidth5 = alternativeMaxHeight7;
                this.mTotalLength += measureNullChild(i6);
                baselineAligned = baselineAligned2;
                weightedMaxHeight = childState5;
                alternativeMaxHeight7 = largestChildWidth5;
                largestChildWidth2 = widthMode5;
            } else {
                int largestChildWidth6 = alternativeMaxHeight7;
                int weightedMaxHeight5 = maxHeight5;
                if (child.getVisibility() == 8) {
                    i6 += getChildrenSkipCount(child, i6);
                    baselineAligned = baselineAligned2;
                    weightedMaxHeight = childState5;
                    alternativeMaxHeight7 = largestChildWidth6;
                    maxHeight5 = weightedMaxHeight5;
                    largestChildWidth2 = widthMode5;
                } else {
                    if (hasDividerBeforeChildAt(i6)) {
                        this.mTotalLength += this.mDividerWidth;
                    }
                    LayoutParams lp2 = (LayoutParams) child.getLayoutParams();
                    float totalWeight3 = totalWeight2 + lp2.weight;
                    if (widthMode5 == 1073741824 && lp2.width == 0 && lp2.weight > 0.0f) {
                        if (isExactly) {
                            alternativeMaxHeight6 = weightedMaxHeight4;
                            this.mTotalLength += lp2.leftMargin + lp2.rightMargin;
                        } else {
                            alternativeMaxHeight6 = weightedMaxHeight4;
                            int totalLength = this.mTotalLength;
                            this.mTotalLength = Math.max(totalLength, lp2.leftMargin + totalLength + lp2.rightMargin);
                        }
                        if (baselineAligned2) {
                            int freeSpec = MeasureSpec.makeMeasureSpec(0, 0);
                            child.measure(freeSpec, freeSpec);
                            lp = lp2;
                            maxHeight4 = childHeight;
                            i3 = i6;
                            baselineAligned = baselineAligned2;
                            largestChildWidth3 = largestChildWidth6;
                            weightedMaxHeight2 = weightedMaxHeight5;
                            alternativeMaxHeight4 = alternativeMaxHeight6;
                            largestChildWidth2 = widthMode5;
                            widthMode4 = -1;
                        } else {
                            skippedMeasure = true;
                            lp = lp2;
                            maxHeight4 = childHeight;
                            i3 = i6;
                            baselineAligned = baselineAligned2;
                            largestChildWidth3 = largestChildWidth6;
                            weightedMaxHeight2 = weightedMaxHeight5;
                            alternativeMaxHeight4 = alternativeMaxHeight6;
                            largestChildWidth2 = widthMode5;
                            widthMode4 = -1;
                        }
                    } else {
                        int alternativeMaxHeight8 = weightedMaxHeight4;
                        if (lp2.width != 0 || lp2.weight <= 0.0f) {
                            oldWidth = Integer.MIN_VALUE;
                        } else {
                            lp2.width = -2;
                            oldWidth = 0;
                        }
                        int largestChildWidth7 = largestChildWidth6;
                        LayoutParams lp3 = lp2;
                        weightedMaxHeight2 = weightedMaxHeight5;
                        int oldWidth2 = oldWidth;
                        alternativeMaxHeight4 = alternativeMaxHeight8;
                        maxHeight4 = childHeight;
                        i3 = i6;
                        baselineAligned = baselineAligned2;
                        largestChildWidth2 = widthMode5;
                        widthMode4 = -1;
                        measureChildBeforeLayout(child, i6, widthMeasureSpec, totalWeight3 == 0.0f ? this.mTotalLength : 0, heightMeasureSpec, 0);
                        int oldWidth3 = oldWidth2;
                        if (oldWidth3 != Integer.MIN_VALUE) {
                            lp = lp3;
                            lp.width = oldWidth3;
                        } else {
                            lp = lp3;
                        }
                        int childWidth = child.getMeasuredWidth();
                        if (isExactly) {
                            this.mTotalLength += lp.leftMargin + childWidth + lp.rightMargin + getNextLocationOffset(child);
                        } else {
                            int totalLength2 = this.mTotalLength;
                            this.mTotalLength = Math.max(totalLength2, totalLength2 + childWidth + lp.leftMargin + lp.rightMargin + getNextLocationOffset(child));
                        }
                        if (useLargestChild2 == true) {
                            largestChildWidth3 = Math.max(childWidth, largestChildWidth7);
                        } else {
                            largestChildWidth3 = largestChildWidth7;
                        }
                    }
                    boolean matchHeightLocally = false;
                    if (heightMode != 1073741824 && lp.height == widthMode4) {
                        matchHeight = true;
                        matchHeightLocally = true;
                    }
                    int margin2 = lp.topMargin + lp.bottomMargin;
                    int childHeight2 = child.getMeasuredHeight() + margin2;
                    int childState6 = View.combineMeasuredStates(childState5, child.getMeasuredState());
                    if (baselineAligned) {
                        int childBaseline = child.getBaseline();
                        if (childBaseline != widthMode4) {
                            int index = ((((lp.gravity < 0 ? this.mGravity : lp.gravity) & 112) >> 4) & -2) >> 1;
                            margin = margin2;
                            maxAscent[index] = Math.max(maxAscent[index], childBaseline);
                            largestChildWidth4 = largestChildWidth3;
                            maxDescent[index] = Math.max(maxDescent[index], childHeight2 - childBaseline);
                        } else {
                            margin = margin2;
                            largestChildWidth4 = largestChildWidth3;
                        }
                    } else {
                        margin = margin2;
                        largestChildWidth4 = largestChildWidth3;
                    }
                    int maxHeight6 = Math.max(maxHeight4, childHeight2);
                    boolean allFillParent3 = allFillParent2 && lp.height == -1;
                    if (lp.weight > 0.0f) {
                        weightedMaxHeight3 = Math.max(weightedMaxHeight2, matchHeightLocally ? margin : childHeight2);
                        LayoutParams layoutParams = lp;
                        alternativeMaxHeight5 = alternativeMaxHeight4;
                    } else {
                        int weightedMaxHeight6 = weightedMaxHeight2;
                        LayoutParams layoutParams2 = lp;
                        alternativeMaxHeight5 = Math.max(alternativeMaxHeight4, matchHeightLocally ? margin : childHeight2);
                        weightedMaxHeight3 = weightedMaxHeight6;
                    }
                    childHeight = maxHeight6;
                    allFillParent2 = allFillParent3;
                    maxHeight5 = weightedMaxHeight3;
                    totalWeight2 = totalWeight3;
                    weightedMaxHeight4 = alternativeMaxHeight5;
                    weightedMaxHeight = childState6;
                    i6 = i3 + getChildrenSkipCount(child, i3);
                    alternativeMaxHeight7 = largestChildWidth4;
                }
            }
            i6++;
            childState5 = weightedMaxHeight;
            baselineAligned2 = baselineAligned;
            widthMode5 = largestChildWidth2;
            int i7 = widthMeasureSpec;
        }
        int i8 = i6;
        boolean baselineAligned3 = baselineAligned2;
        int widthMode6 = widthMode5;
        int childState7 = childState5;
        int weightedMaxHeight7 = maxHeight5;
        int maxHeight7 = childHeight;
        int i9 = weightedMaxHeight4;
        int largestChildWidth8 = alternativeMaxHeight7;
        int alternativeMaxHeight9 = i9;
        if (this.mTotalLength > 0 && hasDividerBeforeChildAt(count2)) {
            this.mTotalLength += this.mDividerWidth;
        }
        if (maxAscent[1] == -1 && maxAscent[0] == -1 && maxAscent[2] == -1 && maxAscent[3] == -1) {
            childState = childState7;
        } else {
            childState = childState7;
            maxHeight7 = Math.max(maxHeight7, Math.max(maxAscent[3], Math.max(maxAscent[0], Math.max(maxAscent[1], maxAscent[2]))) + Math.max(maxDescent[3], Math.max(maxDescent[0], Math.max(maxDescent[1], maxDescent[2]))));
        }
        if (useLargestChild2 != 0) {
            widthMode = widthMode6;
            if (widthMode == Integer.MIN_VALUE || widthMode == 0) {
                this.mTotalLength = 0;
                int i10 = 0;
                while (i10 < count2) {
                    View child2 = getVirtualChildAt(i10);
                    if (child2 == null) {
                        this.mTotalLength += measureNullChild(i10);
                        maxHeight3 = maxHeight7;
                        i2 = i10;
                    } else if (child2.getVisibility() == 8) {
                        maxHeight3 = maxHeight7;
                        i2 = i10 + getChildrenSkipCount(child2, i10);
                    } else {
                        LayoutParams lp4 = (LayoutParams) child2.getLayoutParams();
                        if (isExactly) {
                            maxHeight3 = maxHeight7;
                            i2 = i10;
                            this.mTotalLength += lp4.leftMargin + largestChildWidth8 + lp4.rightMargin + getNextLocationOffset(child2);
                        } else {
                            maxHeight3 = maxHeight7;
                            i2 = i10;
                            int totalLength3 = this.mTotalLength;
                            this.mTotalLength = Math.max(totalLength3, totalLength3 + largestChildWidth8 + lp4.leftMargin + lp4.rightMargin + getNextLocationOffset(child2));
                        }
                    }
                    i10 = i2 + 1;
                    maxHeight7 = maxHeight3;
                }
                maxHeight = maxHeight7;
                int i11 = i10;
            } else {
                maxHeight = maxHeight7;
            }
        } else {
            maxHeight = maxHeight7;
            widthMode = widthMode6;
        }
        this.mTotalLength += getPaddingLeft() + getPaddingRight();
        int widthSizeAndState3 = View.resolveSizeAndState(Math.max(this.mTotalLength, getSuggestedMinimumWidth()), widthMeasureSpec, 0);
        int widthSize3 = widthSizeAndState3 & ViewCompat.MEASURED_SIZE_MASK;
        int delta2 = widthSize3 - this.mTotalLength;
        if (skippedMeasure) {
            totalWeight = totalWeight2;
            int i12 = widthSize3;
            int i13 = largestChildWidth8;
            widthSize = alternativeMaxHeight9;
        } else if (delta2 == 0 || totalWeight2 <= 0.0f) {
            int alternativeMaxHeight10 = Math.max(alternativeMaxHeight9, weightedMaxHeight7);
            if (useLargestChild2 == 0 || widthMode == 1073741824) {
                alternativeMaxHeight = alternativeMaxHeight10;
                int i14 = widthSize3;
                int i15 = largestChildWidth8;
            } else {
                int i16 = 0;
                while (i16 < count2) {
                    float totalWeight4 = totalWeight2;
                    View child3 = getVirtualChildAt(i16);
                    if (child3 != null) {
                        alternativeMaxHeight3 = alternativeMaxHeight10;
                        widthSize2 = widthSize3;
                        if (child3.getVisibility() == 8) {
                            largestChildWidth = largestChildWidth8;
                        } else {
                            LayoutParams lp5 = (LayoutParams) child3.getLayoutParams();
                            float childExtra = lp5.weight;
                            if (childExtra > 0.0f) {
                                LayoutParams layoutParams3 = lp5;
                                float f = childExtra;
                                largestChildWidth = largestChildWidth8;
                                child3.measure(MeasureSpec.makeMeasureSpec(largestChildWidth8, 1073741824), MeasureSpec.makeMeasureSpec(child3.getMeasuredHeight(), 1073741824));
                            } else {
                                float f2 = childExtra;
                                largestChildWidth = largestChildWidth8;
                            }
                        }
                    } else {
                        alternativeMaxHeight3 = alternativeMaxHeight10;
                        widthSize2 = widthSize3;
                        largestChildWidth = largestChildWidth8;
                    }
                    i16++;
                    alternativeMaxHeight10 = alternativeMaxHeight3;
                    totalWeight2 = totalWeight4;
                    widthSize3 = widthSize2;
                    largestChildWidth8 = largestChildWidth;
                }
                alternativeMaxHeight = alternativeMaxHeight10;
                int i17 = widthSize3;
                int i18 = largestChildWidth8;
            }
            int i19 = widthMode;
            int widthMode7 = delta2;
            widthSizeAndState = widthSizeAndState3;
            int i20 = weightedMaxHeight7;
            maxHeight2 = maxHeight;
            childState3 = childState;
            delta = heightMeasureSpec;
            childState2 = count2;
            int maxHeight8 = useLargestChild2;
            if (!allFillParent2 && heightMode != 1073741824) {
                maxHeight2 = alternativeMaxHeight;
            }
            setMeasuredDimension(widthSizeAndState | (-16777216 & childState3), View.resolveSizeAndState(Math.max(maxHeight2 + getPaddingTop() + getPaddingBottom(), getSuggestedMinimumHeight()), delta, childState3 << 16));
            if (!matchHeight) {
                forceUniformHeight(childState2, widthMeasureSpec);
                return;
            }
            int count3 = childState2;
            int i21 = widthMeasureSpec;
            return;
        } else {
            totalWeight = totalWeight2;
            int i22 = widthSize3;
            int i23 = largestChildWidth8;
            widthSize = alternativeMaxHeight9;
        }
        float weightSum2 = this.mWeightSum;
        if (weightSum2 <= 0.0f) {
            weightSum2 = totalWeight;
        }
        maxAscent[3] = -1;
        maxAscent[2] = -1;
        maxAscent[1] = -1;
        maxAscent[0] = -1;
        maxDescent[3] = -1;
        maxDescent[2] = -1;
        maxDescent[1] = -1;
        maxDescent[0] = -1;
        maxHeight2 = -1;
        this.mTotalLength = 0;
        int i24 = 0;
        int alternativeMaxHeight11 = widthSize;
        childState3 = childState;
        while (i24 < count2) {
            int weightedMaxHeight8 = weightedMaxHeight7;
            View child4 = getVirtualChildAt(i24);
            if (child4 != null) {
                useLargestChild = useLargestChild2;
                if (child4.getVisibility() == 8) {
                    widthMode2 = widthMode;
                    widthMode3 = delta2;
                    widthSizeAndState2 = widthSizeAndState3;
                    count = count2;
                    int delta3 = heightMeasureSpec;
                } else {
                    LayoutParams lp6 = (LayoutParams) child4.getLayoutParams();
                    float childExtra2 = lp6.weight;
                    if (childExtra2 > 0.0f) {
                        count = count2;
                        int share = (int) ((((float) delta2) * childExtra2) / weightSum2);
                        float weightSum3 = weightSum2 - childExtra2;
                        float f3 = childExtra2;
                        int delta4 = delta2 - share;
                        widthSizeAndState2 = widthSizeAndState3;
                        int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom() + lp6.topMargin + lp6.bottomMargin, lp6.height);
                        if (lp6.width == 0 && widthMode == 1073741824) {
                            child4.measure(MeasureSpec.makeMeasureSpec(share > 0 ? share : 0, 1073741824), childHeightMeasureSpec);
                            widthMode2 = widthMode;
                        } else {
                            int childWidth2 = child4.getMeasuredWidth() + share;
                            if (childWidth2 < 0) {
                                childWidth2 = 0;
                            }
                            widthMode2 = widthMode;
                            child4.measure(MeasureSpec.makeMeasureSpec(childWidth2, 1073741824), childHeightMeasureSpec);
                        }
                        childState3 = View.combineMeasuredStates(childState3, child4.getMeasuredState() & ViewCompat.MEASURED_STATE_MASK);
                        weightSum2 = weightSum3;
                        widthMode3 = delta4;
                    } else {
                        widthMode2 = widthMode;
                        float f4 = childExtra2;
                        widthMode3 = delta2;
                        widthSizeAndState2 = widthSizeAndState3;
                        count = count2;
                        int delta5 = heightMeasureSpec;
                    }
                    if (isExactly) {
                        this.mTotalLength += child4.getMeasuredWidth() + lp6.leftMargin + lp6.rightMargin + getNextLocationOffset(child4);
                    } else {
                        int totalLength4 = this.mTotalLength;
                        this.mTotalLength = Math.max(totalLength4, child4.getMeasuredWidth() + totalLength4 + lp6.leftMargin + lp6.rightMargin + getNextLocationOffset(child4));
                    }
                    boolean matchHeightLocally2 = heightMode != 1073741824 && lp6.height == -1;
                    int margin3 = lp6.topMargin + lp6.bottomMargin;
                    int childHeight3 = child4.getMeasuredHeight() + margin3;
                    maxHeight2 = Math.max(maxHeight2, childHeight3);
                    if (matchHeightLocally2) {
                        weightSum = weightSum2;
                        i = margin3;
                    } else {
                        weightSum = weightSum2;
                        i = childHeight3;
                    }
                    int alternativeMaxHeight12 = Math.max(alternativeMaxHeight11, i);
                    if (allFillParent2) {
                        alternativeMaxHeight2 = alternativeMaxHeight12;
                        if (lp6.height == -1) {
                            allFillParent = true;
                            if (!baselineAligned3) {
                                int childBaseline2 = child4.getBaseline();
                                allFillParent2 = allFillParent;
                                if (childBaseline2 != -1) {
                                    int gravity = (lp6.gravity < 0 ? this.mGravity : lp6.gravity) & 112;
                                    int index2 = ((gravity >> 4) & -2) >> 1;
                                    int i25 = gravity;
                                    maxAscent[index2] = Math.max(maxAscent[index2], childBaseline2);
                                    childState4 = childState3;
                                    maxDescent[index2] = Math.max(maxDescent[index2], childHeight3 - childBaseline2);
                                } else {
                                    childState4 = childState3;
                                }
                            } else {
                                allFillParent2 = allFillParent;
                                childState4 = childState3;
                            }
                            weightSum2 = weightSum;
                            alternativeMaxHeight11 = alternativeMaxHeight2;
                            childState3 = childState4;
                        }
                    } else {
                        alternativeMaxHeight2 = alternativeMaxHeight12;
                    }
                    allFillParent = false;
                    if (!baselineAligned3) {
                    }
                    weightSum2 = weightSum;
                    alternativeMaxHeight11 = alternativeMaxHeight2;
                    childState3 = childState4;
                }
            } else {
                widthMode2 = widthMode;
                widthMode3 = delta2;
                widthSizeAndState2 = widthSizeAndState3;
                count = count2;
                useLargestChild = useLargestChild2;
                int delta6 = heightMeasureSpec;
            }
            i24++;
            delta2 = widthMode3;
            widthSizeAndState3 = widthSizeAndState2;
            useLargestChild2 = useLargestChild;
            count2 = count;
            weightedMaxHeight7 = weightedMaxHeight8;
            widthMode = widthMode2;
            int i26 = widthMeasureSpec;
        }
        int widthMode8 = delta2;
        widthSizeAndState = widthSizeAndState3;
        childState2 = count2;
        int i27 = weightedMaxHeight7;
        boolean z = useLargestChild2;
        delta = heightMeasureSpec;
        this.mTotalLength += getPaddingLeft() + getPaddingRight();
        if (!(maxAscent[1] == -1 && maxAscent[0] == -1 && maxAscent[2] == -1 && maxAscent[3] == -1)) {
            maxHeight2 = Math.max(maxHeight2, Math.max(maxAscent[3], Math.max(maxAscent[0], Math.max(maxAscent[1], maxAscent[2]))) + Math.max(maxDescent[3], Math.max(maxDescent[0], Math.max(maxDescent[1], maxDescent[2]))));
        }
        alternativeMaxHeight = alternativeMaxHeight11;
        maxHeight2 = alternativeMaxHeight;
        setMeasuredDimension(widthSizeAndState | (-16777216 & childState3), View.resolveSizeAndState(Math.max(maxHeight2 + getPaddingTop() + getPaddingBottom(), getSuggestedMinimumHeight()), delta, childState3 << 16));
        if (!matchHeight) {
        }
    }

    private void forceUniformHeight(int count, int widthMeasureSpec) {
        int uniformMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), 1073741824);
        for (int i = 0; i < count; i++) {
            View child = getVirtualChildAt(i);
            if (child.getVisibility() != 8) {
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.height == -1) {
                    int oldWidth = lp.width;
                    lp.width = child.getMeasuredWidth();
                    measureChildWithMargins(child, widthMeasureSpec, 0, uniformMeasureSpec, 0);
                    lp.width = oldWidth;
                }
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public int getChildrenSkipCount(View child, int index) {
        return 0;
    }

    /* access modifiers changed from: 0000 */
    public int measureNullChild(int childIndex) {
        return 0;
    }

    /* access modifiers changed from: 0000 */
    public void measureChildBeforeLayout(View child, int childIndex, int widthMeasureSpec, int totalWidth, int heightMeasureSpec, int totalHeight) {
        measureChildWithMargins(child, widthMeasureSpec, totalWidth, heightMeasureSpec, totalHeight);
    }

    /* access modifiers changed from: 0000 */
    public int getLocationOffset(View child) {
        return 0;
    }

    /* access modifiers changed from: 0000 */
    public int getNextLocationOffset(View child) {
        return 0;
    }

    /* access modifiers changed from: protected */
    public void onLayout(boolean changed, int l, int t, int r, int b) {
        if (this.mOrientation == 1) {
            layoutVertical(l, t, r, b);
        } else {
            layoutHorizontal(l, t, r, b);
        }
    }

    /* access modifiers changed from: 0000 */
    public void layoutVertical(int left, int top, int right, int bottom) {
        int childTop;
        int paddingLeft;
        int gravity;
        int paddingLeft2 = getPaddingLeft();
        int width = right - left;
        int childRight = width - getPaddingRight();
        int childSpace = (width - paddingLeft2) - getPaddingRight();
        int count = getVirtualChildCount();
        int i = this.mGravity;
        int majorGravity = i & 112;
        int minorGravity = i & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        if (majorGravity == 16) {
            childTop = getPaddingTop() + (((bottom - top) - this.mTotalLength) / 2);
        } else if (majorGravity != 80) {
            childTop = getPaddingTop();
        } else {
            childTop = ((getPaddingTop() + bottom) - top) - this.mTotalLength;
        }
        int i2 = 0;
        while (i2 < count) {
            View child = getVirtualChildAt(i2);
            if (child == null) {
                childTop += measureNullChild(i2);
                paddingLeft = paddingLeft2;
            } else if (child.getVisibility() != 8) {
                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                int gravity2 = lp.gravity;
                if (gravity2 < 0) {
                    gravity = minorGravity;
                } else {
                    gravity = gravity2;
                }
                int layoutDirection = ViewCompat.getLayoutDirection(this);
                int absoluteGravity = GravityCompat.getAbsoluteGravity(gravity, layoutDirection) & 7;
                int childLeft = absoluteGravity != 1 ? absoluteGravity != 5 ? lp.leftMargin + paddingLeft2 : (childRight - childWidth) - lp.rightMargin : ((((childSpace - childWidth) / 2) + paddingLeft2) + lp.leftMargin) - lp.rightMargin;
                if (hasDividerBeforeChildAt(i2) != 0) {
                    childTop += this.mDividerHeight;
                }
                int childTop2 = childTop + lp.topMargin;
                int i3 = layoutDirection;
                int i4 = gravity;
                paddingLeft = paddingLeft2;
                LayoutParams lp2 = lp;
                setChildFrame(child, childLeft, childTop2 + getLocationOffset(child), childWidth, childHeight);
                i2 += getChildrenSkipCount(child, i2);
                childTop = childTop2 + childHeight + lp2.bottomMargin + getNextLocationOffset(child);
            } else {
                paddingLeft = paddingLeft2;
            }
            i2++;
            paddingLeft2 = paddingLeft;
        }
    }

    /* access modifiers changed from: 0000 */
    /* JADX WARNING: Removed duplicated region for block: B:27:0x00c5  */
    /* JADX WARNING: Removed duplicated region for block: B:28:0x00c9  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x00d3  */
    /* JADX WARNING: Removed duplicated region for block: B:44:0x0107  */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x011a  */
    public void layoutHorizontal(int left, int top, int right, int bottom) {
        int childLeft;
        int dir;
        int start;
        int[] maxAscent;
        int[] maxDescent;
        int paddingTop;
        int count;
        int height;
        int layoutDirection;
        int childBaseline;
        int gravity;
        int gravity2;
        int gravity3;
        int childTop;
        boolean isLayoutRtl = ViewUtils.isLayoutRtl(this);
        int paddingTop2 = getPaddingTop();
        int height2 = bottom - top;
        int childBottom = height2 - getPaddingBottom();
        int childSpace = (height2 - paddingTop2) - getPaddingBottom();
        int count2 = getVirtualChildCount();
        int i = this.mGravity;
        int majorGravity = i & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        int minorGravity = i & 112;
        boolean baselineAligned = this.mBaselineAligned;
        int[] maxAscent2 = this.mMaxAscent;
        int[] maxDescent2 = this.mMaxDescent;
        int layoutDirection2 = ViewCompat.getLayoutDirection(this);
        int absoluteGravity = GravityCompat.getAbsoluteGravity(majorGravity, layoutDirection2);
        if (absoluteGravity == 1) {
            childLeft = getPaddingLeft() + (((right - left) - this.mTotalLength) / 2);
        } else if (absoluteGravity != 5) {
            childLeft = getPaddingLeft();
        } else {
            childLeft = ((getPaddingLeft() + right) - left) - this.mTotalLength;
        }
        if (isLayoutRtl) {
            start = count2 - 1;
            dir = -1;
        } else {
            start = 0;
            dir = 1;
        }
        int i2 = 0;
        while (i2 < count2) {
            int childIndex = start + (dir * i2);
            boolean isLayoutRtl2 = isLayoutRtl;
            View child = getVirtualChildAt(childIndex);
            if (child == null) {
                childLeft += measureNullChild(childIndex);
                layoutDirection = layoutDirection2;
                maxDescent = maxDescent2;
                maxAscent = maxAscent2;
                paddingTop = paddingTop2;
                height = height2;
                count = count2;
            } else {
                int i3 = i2;
                layoutDirection = layoutDirection2;
                if (child.getVisibility() != 8) {
                    int childWidth = child.getMeasuredWidth();
                    int childHeight = child.getMeasuredHeight();
                    LayoutParams lp = (LayoutParams) child.getLayoutParams();
                    if (baselineAligned) {
                        height = height2;
                        if (lp.height != -1) {
                            childBaseline = child.getBaseline();
                            gravity = lp.gravity;
                            if (gravity >= 0) {
                                gravity2 = minorGravity;
                            } else {
                                gravity2 = gravity;
                            }
                            gravity3 = gravity2 & 112;
                            count = count2;
                            if (gravity3 != 16) {
                                childTop = ((((childSpace - childHeight) / 2) + paddingTop2) + lp.topMargin) - lp.bottomMargin;
                            } else if (gravity3 == 48) {
                                int childTop2 = lp.topMargin + paddingTop2;
                                childTop = childBaseline != -1 ? childTop2 + (maxAscent2[1] - childBaseline) : childTop2;
                            } else if (gravity3 != 80) {
                                childTop = paddingTop2;
                            } else {
                                int childTop3 = (childBottom - childHeight) - lp.bottomMargin;
                                childTop = childBaseline != -1 ? childTop3 - (maxDescent2[2] - (child.getMeasuredHeight() - childBaseline)) : childTop3;
                            }
                            if (hasDividerBeforeChildAt(childIndex) != 0) {
                                childLeft += this.mDividerWidth;
                            }
                            int childLeft2 = childLeft + lp.leftMargin;
                            paddingTop = paddingTop2;
                            int childIndex2 = childIndex;
                            int i4 = i3;
                            int i5 = childBaseline;
                            LayoutParams lp2 = lp;
                            maxDescent = maxDescent2;
                            maxAscent = maxAscent2;
                            setChildFrame(child, childLeft2 + getLocationOffset(child), childTop, childWidth, childHeight);
                            i2 = i4 + getChildrenSkipCount(child, childIndex2);
                            childLeft = childLeft2 + childWidth + lp2.rightMargin + getNextLocationOffset(child);
                        }
                    } else {
                        height = height2;
                    }
                    childBaseline = -1;
                    gravity = lp.gravity;
                    if (gravity >= 0) {
                    }
                    gravity3 = gravity2 & 112;
                    count = count2;
                    if (gravity3 != 16) {
                    }
                    if (hasDividerBeforeChildAt(childIndex) != 0) {
                    }
                    int childLeft22 = childLeft + lp.leftMargin;
                    paddingTop = paddingTop2;
                    int childIndex22 = childIndex;
                    int i42 = i3;
                    int i52 = childBaseline;
                    LayoutParams lp22 = lp;
                    maxDescent = maxDescent2;
                    maxAscent = maxAscent2;
                    setChildFrame(child, childLeft22 + getLocationOffset(child), childTop, childWidth, childHeight);
                    i2 = i42 + getChildrenSkipCount(child, childIndex22);
                    childLeft = childLeft22 + childWidth + lp22.rightMargin + getNextLocationOffset(child);
                } else {
                    maxDescent = maxDescent2;
                    maxAscent = maxAscent2;
                    paddingTop = paddingTop2;
                    height = height2;
                    count = count2;
                    int paddingTop3 = childIndex;
                    i2 = i3;
                }
            }
            i2++;
            isLayoutRtl = isLayoutRtl2;
            layoutDirection2 = layoutDirection;
            height2 = height;
            count2 = count;
            paddingTop2 = paddingTop;
            maxDescent2 = maxDescent;
            maxAscent2 = maxAscent;
        }
        int i6 = i2;
        int i7 = layoutDirection2;
        int[] iArr = maxDescent2;
        int[] iArr2 = maxAscent2;
        boolean z = isLayoutRtl;
        int i8 = paddingTop2;
        int i9 = height2;
        int i10 = count2;
    }

    private void setChildFrame(View child, int left, int top, int width, int height) {
        child.layout(left, top, left + width, top + height);
    }

    public void setOrientation(int orientation) {
        if (this.mOrientation != orientation) {
            this.mOrientation = orientation;
            requestLayout();
        }
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setGravity(int gravity) {
        if (this.mGravity != gravity) {
            if ((8388615 & gravity) == 0) {
                gravity |= GravityCompat.START;
            }
            if ((gravity & 112) == 0) {
                gravity |= 48;
            }
            this.mGravity = gravity;
            requestLayout();
        }
    }

    public int getGravity() {
        return this.mGravity;
    }

    public void setHorizontalGravity(int horizontalGravity) {
        int gravity = horizontalGravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK;
        int i = this.mGravity;
        if ((8388615 & i) != gravity) {
            this.mGravity = (-8388616 & i) | gravity;
            requestLayout();
        }
    }

    public void setVerticalGravity(int verticalGravity) {
        int gravity = verticalGravity & 112;
        int i = this.mGravity;
        if ((i & 112) != gravity) {
            this.mGravity = (i & -113) | gravity;
            requestLayout();
        }
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    /* access modifiers changed from: protected */
    public LayoutParams generateDefaultLayoutParams() {
        int i = this.mOrientation;
        if (i == 0) {
            return new LayoutParams(-2, -2);
        }
        if (i == 1) {
            return new LayoutParams(-1, -2);
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    /* access modifiers changed from: protected */
    public boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(LinearLayoutCompat.class.getName());
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info2) {
        super.onInitializeAccessibilityNodeInfo(info2);
        info2.setClassName(LinearLayoutCompat.class.getName());
    }
}
