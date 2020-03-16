package org.quantumbadger.redreader.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.TextView;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.views.LiveDHM.Params;

public abstract class FlingableItemView extends SwipableItemView {
    /* access modifiers changed from: private */
    public FlingHintAnimation mFlingHintAnimation;
    private final TextView mFlingHintLeft;
    private final FrameLayout mFlingHintOuter;
    private final TextView mFlingHintRight;
    /* access modifiers changed from: private */
    public float mFlingHintYPos = 0.0f;
    private boolean mLeftFlingHintShown = false;
    private final int mOffsetActionPerformed;
    private final int mOffsetBeginAllowed;
    private boolean mRightFlingHintShown = false;
    private boolean mSwipeReady = false;
    private final Drawable rrIconFfLeft;
    private final Drawable rrIconFfRight;
    private final Drawable rrIconTick;

    private class FlingHintAnimation extends RRDHMAnimation {
        private FlingHintAnimation(Params params) {
            super(params);
        }

        /* access modifiers changed from: protected */
        public void onUpdatedPosition(float position) {
            FlingableItemView.this.mFlingHintYPos = position;
            FlingableItemView.this.updateFlingHintPosition();
        }

        /* access modifiers changed from: protected */
        public void onEndPosition(float endPosition) {
            FlingableItemView.this.mFlingHintYPos = endPosition;
            FlingableItemView.this.updateFlingHintPosition();
            FlingableItemView.this.mFlingHintAnimation = null;
        }
    }

    /* access modifiers changed from: protected */
    public abstract boolean allowFlingingLeft();

    /* access modifiers changed from: protected */
    public abstract boolean allowFlingingRight();

    /* access modifiers changed from: protected */
    @NonNull
    public abstract String getFlingLeftText();

    /* access modifiers changed from: protected */
    @NonNull
    public abstract String getFlingRightText();

    /* access modifiers changed from: protected */
    public abstract void onFlungLeft();

    /* access modifiers changed from: protected */
    public abstract void onFlungRight();

    /* access modifiers changed from: protected */
    public abstract void onSetItemFlingPosition(float f);

    public FlingableItemView(@NonNull Context context) {
        super(context);
        this.mOffsetBeginAllowed = General.dpToPixels(context, 50.0f);
        this.mOffsetActionPerformed = General.dpToPixels(context, 150.0f);
        TypedArray attr = context.obtainStyledAttributes(new int[]{R.attr.rrIconFfLeft, R.attr.rrIconFfRight, R.attr.rrIconTick, R.attr.rrListBackgroundCol});
        this.rrIconFfLeft = attr.getDrawable(0);
        this.rrIconFfRight = attr.getDrawable(1);
        this.rrIconTick = attr.getDrawable(2);
        int rrListBackgroundCol = attr.getColor(3, General.COLOR_INVALID);
        attr.recycle();
        this.mFlingHintOuter = (FrameLayout) LayoutInflater.from(context).inflate(R.layout.fling_hint, null, false);
        addView(this.mFlingHintOuter);
        LayoutParams flingHintLayoutParams = this.mFlingHintOuter.getLayoutParams();
        flingHintLayoutParams.width = -1;
        flingHintLayoutParams.height = -2;
        this.mFlingHintLeft = (TextView) this.mFlingHintOuter.findViewById(R.id.reddit_post_fling_text_left);
        this.mFlingHintRight = (TextView) this.mFlingHintOuter.findViewById(R.id.reddit_post_fling_text_right);
        this.mFlingHintLeft.setCompoundDrawablesWithIntrinsicBounds(null, this.rrIconFfLeft, null, null);
        this.mFlingHintRight.setCompoundDrawablesWithIntrinsicBounds(null, this.rrIconFfRight, null, null);
        setBackgroundColor(rrListBackgroundCol);
    }

    public void setFlingingEnabled(boolean flingingEnabled) {
        this.mFlingHintOuter.setVisibility(flingingEnabled ? 0 : 8);
        setSwipingEnabled(flingingEnabled);
    }

    /* access modifiers changed from: protected */
    public final boolean allowSwipingLeft() {
        return allowFlingingLeft();
    }

    /* access modifiers changed from: protected */
    public final boolean allowSwipingRight() {
        return allowFlingingRight();
    }

    /* access modifiers changed from: private */
    public void updateFlingHintPosition() {
        this.mFlingHintOuter.setTranslationY(this.mFlingHintYPos);
    }

    /* access modifiers changed from: protected */
    public void onSwipeFingerDown(int x, int y, float xOffsetPixels, boolean wasOldSwipeInterrupted) {
        if (((float) this.mOffsetBeginAllowed) > Math.abs(xOffsetPixels)) {
            this.mFlingHintLeft.setText(getFlingLeftText());
            this.mFlingHintRight.setText(getFlingRightText());
            this.mFlingHintLeft.setCompoundDrawablesWithIntrinsicBounds(null, this.rrIconFfLeft, null, null);
            this.mFlingHintRight.setCompoundDrawablesWithIntrinsicBounds(null, this.rrIconFfRight, null, null);
            this.mSwipeReady = true;
        }
        int height = this.mFlingHintOuter.getMeasuredHeight();
        int parentHeight = getMeasuredHeight();
        FlingHintAnimation oldAnimation = this.mFlingHintAnimation;
        FlingHintAnimation flingHintAnimation = this.mFlingHintAnimation;
        if (flingHintAnimation != null) {
            flingHintAnimation.stop();
            this.mFlingHintAnimation = null;
        }
        if (parentHeight > height * 3) {
            int yPos = Math.min(Math.max(y - (height / 2), 0), parentHeight - height);
            if (wasOldSwipeInterrupted) {
                if (Math.abs(((float) yPos) - this.mFlingHintYPos) < ((float) height)) {
                    yPos = (int) this.mFlingHintYPos;
                }
                Params params = new Params();
                params.startPosition = this.mFlingHintYPos;
                params.endPosition = (float) yPos;
                if (oldAnimation != null) {
                    params.startVelocity = oldAnimation.getCurrentVelocity();
                }
                this.mFlingHintAnimation = new FlingHintAnimation(params);
                this.mFlingHintAnimation.start();
                return;
            }
            this.mFlingHintYPos = (float) yPos;
            updateFlingHintPosition();
            return;
        }
        this.mFlingHintYPos = (float) ((parentHeight - height) / 2);
        updateFlingHintPosition();
    }

    public void onSwipeDeltaChanged(float xOffsetPixels) {
        onSetItemFlingPosition(xOffsetPixels);
        float absOffset = Math.abs(xOffsetPixels);
        if (this.mSwipeReady && absOffset > ((float) this.mOffsetActionPerformed)) {
            if (xOffsetPixels > 0.0f) {
                onFlungRight();
                this.mFlingHintRight.setCompoundDrawablesWithIntrinsicBounds(null, this.rrIconTick, null, null);
            } else {
                onFlungLeft();
                this.mFlingHintLeft.setCompoundDrawablesWithIntrinsicBounds(null, this.rrIconTick, null, null);
            }
            this.mSwipeReady = false;
        } else if (absOffset <= 5.0f) {
            if (this.mRightFlingHintShown) {
                this.mRightFlingHintShown = false;
                this.mFlingHintRight.setVisibility(4);
            }
            if (this.mLeftFlingHintShown) {
                this.mLeftFlingHintShown = false;
                this.mFlingHintLeft.setVisibility(4);
            }
        } else if (xOffsetPixels > 0.0f) {
            if (!this.mRightFlingHintShown) {
                this.mRightFlingHintShown = true;
                this.mFlingHintRight.setVisibility(0);
            }
            if (this.mLeftFlingHintShown) {
                this.mLeftFlingHintShown = false;
                this.mFlingHintLeft.setVisibility(4);
            }
        } else {
            if (!this.mLeftFlingHintShown) {
                this.mLeftFlingHintShown = true;
                this.mFlingHintLeft.setVisibility(0);
            }
            if (this.mRightFlingHintShown) {
                this.mRightFlingHintShown = false;
                this.mFlingHintRight.setVisibility(4);
            }
        }
    }
}
