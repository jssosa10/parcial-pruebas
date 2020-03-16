package com.github.lzyzsd.circleprogress;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.view.View;
import android.view.View.MeasureSpec;

public class DonutProgress extends View {
    private boolean aspectIndicatorDisplay;
    private Paint aspectIndicatorPaint;
    private RectF aspectIndicatorRect = new RectF();
    private int aspectIndicatorStrokeColor;
    private float aspectIndicatorStrokeWidth;
    private RectF finishedOuterRect = new RectF();
    private Paint finishedPaint;
    private int finishedStrokeColor;
    private float finishedStrokeWidth;
    private float imageAspectRatio;
    private boolean indeterminate;
    private final int min_size = ((int) dp2px(getResources(), 100.0f));
    private float progress = 0.0f;
    private int startingDegree;
    private RectF unfinishedOuterRect = new RectF();
    private Paint unfinishedPaint;
    private int unfinishedStrokeColor;
    private float unfinishedStrokeWidth;

    public static float dp2px(Resources resources, float dp) {
        return (dp * resources.getDisplayMetrics().density) + 0.5f;
    }

    public DonutProgress(Context context) {
        super(context);
        initPainters();
    }

    public void initPainters() {
        this.finishedPaint = new Paint();
        this.finishedPaint.setColor(this.finishedStrokeColor);
        this.finishedPaint.setStyle(Style.STROKE);
        this.finishedPaint.setAntiAlias(true);
        this.finishedPaint.setStrokeWidth(this.finishedStrokeWidth);
        this.unfinishedPaint = new Paint();
        this.unfinishedPaint.setColor(this.unfinishedStrokeColor);
        this.unfinishedPaint.setStyle(Style.STROKE);
        this.unfinishedPaint.setAntiAlias(true);
        this.unfinishedPaint.setStrokeWidth(this.unfinishedStrokeWidth);
        this.aspectIndicatorPaint = new Paint();
        this.aspectIndicatorPaint.setColor(this.aspectIndicatorStrokeColor);
        this.aspectIndicatorPaint.setStyle(Style.STROKE);
        this.aspectIndicatorPaint.setAntiAlias(true);
        this.aspectIndicatorPaint.setStrokeWidth(this.aspectIndicatorStrokeWidth);
    }

    public void setFinishedStrokeWidth(float finishedStrokeWidth2) {
        this.finishedStrokeWidth = finishedStrokeWidth2;
    }

    public void setUnfinishedStrokeWidth(float unfinishedStrokeWidth2) {
        this.unfinishedStrokeWidth = unfinishedStrokeWidth2;
    }

    public void setAspectIndicatorStrokeWidth(float aspectIndicatorStrokeWidth2) {
        this.aspectIndicatorStrokeWidth = aspectIndicatorStrokeWidth2;
    }

    public void setIndeterminate(boolean value) {
        this.indeterminate = value;
        invalidate();
    }

    public void setAspectIndicatorDisplay(boolean value) {
        this.aspectIndicatorDisplay = value;
    }

    private float getProgressAngle() {
        return getProgress() * 360.0f;
    }

    public float getProgress() {
        return this.progress;
    }

    public void setProgress(float progress2) {
        if (((double) Math.abs(progress2 - this.progress)) > 1.0E-4d) {
            this.progress = progress2;
            invalidate();
        }
    }

    public void setFinishedStrokeColor(int finishedStrokeColor2) {
        this.finishedStrokeColor = finishedStrokeColor2;
    }

    public void setUnfinishedStrokeColor(int unfinishedStrokeColor2) {
        this.unfinishedStrokeColor = unfinishedStrokeColor2;
    }

    public void setAspectIndicatorStrokeColor(int aspectIndicatorStrokeColor2) {
        this.aspectIndicatorStrokeColor = aspectIndicatorStrokeColor2;
    }

    public void setLoadingImageAspectRatio(float imageAspectRatio2) {
        this.imageAspectRatio = imageAspectRatio2;
    }

    public int getStartingDegree() {
        return this.startingDegree;
    }

    /* access modifiers changed from: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measure(widthMeasureSpec), measure(heightMeasureSpec));
    }

    private int measure(int measureSpec) {
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        if (mode == 1073741824) {
            return size;
        }
        int result = this.min_size;
        if (mode == Integer.MIN_VALUE) {
            return Math.min(result, size);
        }
        return result;
    }

    /* access modifiers changed from: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float delta = Math.max(this.finishedStrokeWidth, this.unfinishedStrokeWidth);
        this.finishedOuterRect.set(delta, delta, ((float) getWidth()) - delta, ((float) getHeight()) - delta);
        this.unfinishedOuterRect.set(delta, delta, ((float) getWidth()) - delta, ((float) getHeight()) - delta);
        canvas.drawArc(this.unfinishedOuterRect, 0.0f, 360.0f, false, this.unfinishedPaint);
        if (this.indeterminate) {
            canvas.drawArc(this.finishedOuterRect, (((float) (System.currentTimeMillis() % 1000)) * 360.0f) / 1000.0f, 50.0f, false, this.finishedPaint);
            invalidate();
        } else {
            canvas.drawArc(this.finishedOuterRect, (float) getStartingDegree(), getProgressAngle(), false, this.finishedPaint);
        }
        if (this.aspectIndicatorDisplay) {
            float f = this.imageAspectRatio;
            if (f > 2.75f) {
                this.imageAspectRatio = 2.75f;
            } else if (f < 0.36363637f) {
                this.imageAspectRatio = 0.36363637f;
            }
            float f2 = delta * 3.5f;
            float f3 = delta * 0.875f;
            float f4 = this.imageAspectRatio;
            float indicatorLeft = f2 + (f3 * (1.0f - f4));
            float indicatorTop = (3.5f * delta) + (0.875f * delta * (1.0f - (1.0f / f4)));
            this.aspectIndicatorRect.set(indicatorLeft, indicatorTop, ((float) getWidth()) - indicatorLeft, ((float) getHeight()) - indicatorTop);
            canvas.drawRect(this.aspectIndicatorRect, this.aspectIndicatorPaint);
            return;
        }
        Canvas canvas2 = canvas;
    }

    public void setStartingDegree(int startingDegree2) {
        this.startingDegree = startingDegree2;
    }
}
