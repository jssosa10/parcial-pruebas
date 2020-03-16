package org.quantumbadger.redreader.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.views.LiveDHM.Params;

public abstract class SwipableItemView extends FrameLayout {
    /* access modifiers changed from: private */
    public SwipeAnimation mCurrentSwipeAnimation;
    private float mCurrentSwipeDelta = 0.0f;
    /* access modifiers changed from: private */
    public float mOverallSwipeDelta = 0.0f;
    private final SwipeHistory mSwipeHistory = new SwipeHistory(30);
    private boolean mSwipeInProgress = false;
    private MotionEvent mSwipeStart;
    private int mSwipeStartPointerId = -1;
    private boolean mSwipingEnabled = true;
    private float mVelocity;

    private class SwipeAnimation extends RRDHMAnimation {
        private SwipeAnimation(Params params) {
            super(params);
        }

        /* access modifiers changed from: protected */
        public void onUpdatedPosition(float position) {
            SwipableItemView.this.mOverallSwipeDelta = position;
            SwipableItemView.this.updateOffset();
        }

        /* access modifiers changed from: protected */
        public void onEndPosition(float endPosition) {
            SwipableItemView.this.mOverallSwipeDelta = endPosition;
            SwipableItemView.this.updateOffset();
            SwipableItemView.this.mCurrentSwipeAnimation = null;
        }
    }

    /* access modifiers changed from: protected */
    public abstract boolean allowSwipingLeft();

    /* access modifiers changed from: protected */
    public abstract boolean allowSwipingRight();

    /* access modifiers changed from: protected */
    public abstract void onSwipeDeltaChanged(float f);

    /* access modifiers changed from: protected */
    public abstract void onSwipeFingerDown(int i, int i2, float f, boolean z);

    public SwipableItemView(@NonNull Context context) {
        super(context);
    }

    public void setSwipingEnabled(boolean swipingEnabled) {
        this.mSwipingEnabled = swipingEnabled;
    }

    /* access modifiers changed from: protected */
    public void resetSwipeState() {
        this.mSwipeHistory.clear();
        this.mSwipeStart = null;
        this.mSwipeStartPointerId = -1;
        this.mSwipeInProgress = false;
        this.mCurrentSwipeDelta = 0.0f;
        this.mOverallSwipeDelta = 0.0f;
        cancelSwipeAnimation();
        updateOffset();
    }

    /* access modifiers changed from: private */
    public void updateOffset() {
        float overallPos = this.mOverallSwipeDelta + this.mCurrentSwipeDelta;
        if ((overallPos > 0.0f && !allowSwipingRight()) || (overallPos < 0.0f && !allowSwipingLeft())) {
            this.mOverallSwipeDelta = -this.mCurrentSwipeDelta;
        }
        onSwipeDeltaChanged(this.mOverallSwipeDelta + this.mCurrentSwipeDelta);
    }

    private void onFingerDown(int x, int y) {
        boolean wasOldSwipeInterrupted = (this.mCurrentSwipeAnimation == null && this.mOverallSwipeDelta == 0.0f) ? false : true;
        cancelSwipeAnimation();
        this.mSwipeHistory.clear();
        this.mVelocity = 0.0f;
        this.mOverallSwipeDelta += this.mCurrentSwipeDelta;
        this.mCurrentSwipeDelta = 0.0f;
        onSwipeFingerDown(x, y, this.mOverallSwipeDelta, wasOldSwipeInterrupted);
    }

    private void onFingerSwipeMove() {
        this.mSwipeHistory.add(this.mCurrentSwipeDelta, System.currentTimeMillis());
        updateOffset();
    }

    private void onSwipeEnd() {
        if (this.mSwipeHistory.size() >= 2) {
            this.mVelocity = (this.mSwipeHistory.getMostRecent() - this.mSwipeHistory.getAtTimeAgoMs(100)) * 10.0f;
        } else {
            this.mVelocity = 0.0f;
        }
        this.mOverallSwipeDelta += this.mCurrentSwipeDelta;
        this.mCurrentSwipeDelta = 0.0f;
        animateSwipeToRestPosition();
    }

    private void onSwipeCancelled() {
        this.mVelocity = 0.0f;
        this.mOverallSwipeDelta += this.mCurrentSwipeDelta;
        this.mCurrentSwipeDelta = 0.0f;
        animateSwipeToRestPosition();
    }

    private void animateSwipeToRestPosition() {
        Params params = new Params();
        params.startPosition = this.mOverallSwipeDelta;
        params.startVelocity = this.mVelocity;
        startSwipeAnimation(new SwipeAnimation(params));
    }

    private void startSwipeAnimation(SwipeAnimation animation) {
        SwipeAnimation swipeAnimation = this.mCurrentSwipeAnimation;
        if (swipeAnimation != null) {
            swipeAnimation.stop();
        }
        this.mCurrentSwipeAnimation = animation;
        this.mCurrentSwipeAnimation.start();
    }

    private void cancelSwipeAnimation() {
        SwipeAnimation swipeAnimation = this.mCurrentSwipeAnimation;
        if (swipeAnimation != null) {
            swipeAnimation.stop();
            this.mCurrentSwipeAnimation = null;
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!this.mSwipeInProgress && !swipeStartLogic(ev)) {
            return super.onInterceptTouchEvent(ev);
        }
        return true;
    }

    private boolean swipeStartLogic(MotionEvent ev) {
        if (this.mSwipeInProgress) {
            throw new RuntimeException();
        } else if (!this.mSwipingEnabled) {
            return false;
        } else {
            int action = ev.getAction() & 255;
            int pointerId = ev.getPointerId(ev.getActionIndex());
            if (action == 0 || action == 5) {
                if (this.mSwipeStart != null) {
                    return false;
                }
                this.mSwipeStart = MotionEvent.obtain(ev);
                this.mSwipeStartPointerId = pointerId;
                onFingerDown((int) ev.getX(), (int) ev.getY());
            } else if (action == 2) {
                if (this.mSwipeStart == null || pointerId != this.mSwipeStartPointerId) {
                    return false;
                }
                float xDelta = ev.getX() - this.mSwipeStart.getX();
                float yDelta = ev.getY() - this.mSwipeStart.getY();
                int minXDelta = General.dpToPixels(getContext(), 20.0f);
                int maxYDelta = General.dpToPixels(getContext(), 10.0f);
                if (Math.abs(xDelta) >= ((float) minXDelta) && Math.abs(yDelta) <= ((float) maxYDelta)) {
                    this.mSwipeInProgress = true;
                    this.mCurrentSwipeDelta = 0.0f;
                    requestDisallowInterceptTouchEvent(true);
                    cancelLongPress();
                    return true;
                }
            } else if ((action != 3 && action != 1 && action != 6 && action != 4) || pointerId != this.mSwipeStartPointerId) {
                return false;
            } else {
                this.mSwipeStart = null;
                onSwipeCancelled();
            }
            return false;
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!this.mSwipeInProgress) {
            if (swipeStartLogic(ev)) {
                return true;
            }
            return super.onTouchEvent(ev);
        } else if (this.mSwipeStart != null) {
            int action = ev.getAction() & 255;
            if (ev.getPointerId(ev.getActionIndex()) != this.mSwipeStartPointerId) {
                return false;
            }
            if (action == 2) {
                this.mCurrentSwipeDelta = ev.getX() - this.mSwipeStart.getX();
                onFingerSwipeMove();
            } else if (action == 3 || action == 1 || action == 6 || action == 4) {
                this.mSwipeStart = null;
                this.mSwipeInProgress = false;
                requestDisallowInterceptTouchEvent(false);
                onSwipeEnd();
            }
            return true;
        } else {
            throw new RuntimeException();
        }
    }
}
