package org.quantumbadger.redreader.views.imageview;

import android.view.MotionEvent;
import org.quantumbadger.redreader.common.MutableFloatPoint2D;

public class FingerTracker {
    private final Finger[] mFingers = new Finger[10];
    private final FingerListener mListener;

    public class Finger {
        boolean mActive = false;
        int mAndroidId;
        final MutableFloatPoint2D mCurrentPos = new MutableFloatPoint2D();
        long mDownDuration;
        long mDownStartTime;
        final MutableFloatPoint2D mLastPos = new MutableFloatPoint2D();
        final MutableFloatPoint2D mPosDifference = new MutableFloatPoint2D();
        final MutableFloatPoint2D mStartPos = new MutableFloatPoint2D();
        final MutableFloatPoint2D mTotalPosDifference = new MutableFloatPoint2D();

        public Finger() {
        }

        public void onDown(MotionEvent event) {
            int index = event.getActionIndex();
            this.mActive = true;
            this.mAndroidId = event.getPointerId(index);
            this.mCurrentPos.set(event, index);
            this.mLastPos.set(this.mCurrentPos);
            this.mStartPos.set(this.mCurrentPos);
            this.mPosDifference.reset();
            this.mTotalPosDifference.reset();
            this.mDownStartTime = event.getDownTime();
            this.mDownDuration = 0;
        }

        public void onMove(MotionEvent event) {
            int index = event.findPointerIndex(this.mAndroidId);
            if (index >= 0) {
                this.mLastPos.set(this.mCurrentPos);
                this.mCurrentPos.set(event, index);
                this.mCurrentPos.sub(this.mLastPos, this.mPosDifference);
                this.mCurrentPos.sub(this.mStartPos, this.mTotalPosDifference);
                this.mDownDuration = event.getEventTime() - this.mDownStartTime;
            }
        }

        public void onUp(MotionEvent event) {
            this.mLastPos.set(this.mCurrentPos);
            this.mCurrentPos.set(event, event.getActionIndex());
            this.mCurrentPos.sub(this.mLastPos, this.mPosDifference);
            this.mCurrentPos.sub(this.mStartPos, this.mTotalPosDifference);
            this.mDownDuration = event.getEventTime() - this.mDownStartTime;
            this.mActive = false;
        }
    }

    public interface FingerListener {
        void onFingerDown(Finger finger);

        void onFingerUp(Finger finger);

        void onFingersMoved();
    }

    public FingerTracker(FingerListener mListener2) {
        this.mListener = mListener2;
        int i = 0;
        while (true) {
            Finger[] fingerArr = this.mFingers;
            if (i < fingerArr.length) {
                fingerArr[i] = new Finger();
                i++;
            } else {
                return;
            }
        }
    }

    public void onTouchEvent(MotionEvent event) {
        int i = 0;
        switch (event.getActionMasked()) {
            case 0:
            case 5:
                Finger[] fingerArr = this.mFingers;
                int length = fingerArr.length;
                while (i < length) {
                    Finger f = fingerArr[i];
                    if (!f.mActive) {
                        f.onDown(event);
                        this.mListener.onFingerDown(f);
                        return;
                    }
                    i++;
                }
                return;
            case 1:
            case 6:
                int id = event.getPointerId(event.getActionIndex());
                Finger[] fingerArr2 = this.mFingers;
                int length2 = fingerArr2.length;
                while (i < length2) {
                    Finger f2 = fingerArr2[i];
                    if (!f2.mActive || f2.mAndroidId != id) {
                        i++;
                    } else {
                        f2.onUp(event);
                        this.mListener.onFingerUp(f2);
                        return;
                    }
                }
                return;
            case 2:
                Finger[] fingerArr3 = this.mFingers;
                int length3 = fingerArr3.length;
                while (i < length3) {
                    Finger finger = fingerArr3[i];
                    if (finger.mActive) {
                        finger.onMove(event);
                    }
                    i++;
                }
                this.mListener.onFingersMoved();
                return;
            default:
                return;
        }
    }
}
