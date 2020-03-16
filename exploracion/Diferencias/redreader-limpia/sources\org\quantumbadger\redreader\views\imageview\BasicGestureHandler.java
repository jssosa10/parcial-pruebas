package org.quantumbadger.redreader.views.imageview;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import org.quantumbadger.redreader.views.imageview.FingerTracker.Finger;
import org.quantumbadger.redreader.views.imageview.FingerTracker.FingerListener;

public class BasicGestureHandler implements OnTouchListener, FingerListener {
    private int mCurrentFingerCount;
    private final FingerTracker mFingerTracker = new FingerTracker(this);
    private Finger mFirstFinger;
    private final Listener mListener;

    public interface Listener {
        void onHorizontalSwipe(float f);

        void onHorizontalSwipeEnd();

        void onSingleTap();
    }

    public BasicGestureHandler(Listener listener) {
        this.mListener = listener;
    }

    public boolean onTouch(View v, MotionEvent event) {
        this.mFingerTracker.onTouchEvent(event);
        return true;
    }

    public void onFingerDown(Finger finger) {
        this.mCurrentFingerCount++;
        if (this.mCurrentFingerCount > 1) {
            this.mFirstFinger = null;
        } else {
            this.mFirstFinger = finger;
        }
    }

    public void onFingersMoved() {
        Finger finger = this.mFirstFinger;
        if (finger != null) {
            this.mListener.onHorizontalSwipe(finger.mTotalPosDifference.x);
        }
    }

    public void onFingerUp(Finger finger) {
        this.mCurrentFingerCount--;
        if (this.mFirstFinger != null) {
            this.mListener.onHorizontalSwipeEnd();
            if (this.mFirstFinger.mDownDuration < 300 && this.mFirstFinger.mPosDifference.x < 20.0f && this.mFirstFinger.mPosDifference.y < 20.0f) {
                this.mListener.onSingleTap();
            }
            this.mFirstFinger = null;
        }
    }
}
