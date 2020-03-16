package org.quantumbadger.redreader.common;

import android.view.MotionEvent;

public class MutableFloatPoint2D {
    public float x;
    public float y;

    public void reset() {
        this.x = 0.0f;
        this.y = 0.0f;
    }

    public void set(MotionEvent event, int pointerIndex) {
        this.x = event.getX(pointerIndex);
        this.y = event.getY(pointerIndex);
    }

    public void set(MutableFloatPoint2D other) {
        this.x = other.x;
        this.y = other.y;
    }

    public void set(float x2, float y2) {
        this.x = x2;
        this.y = y2;
    }

    public void add(MutableFloatPoint2D rhs, MutableFloatPoint2D result) {
        result.x = this.x + rhs.x;
        result.y = this.y + rhs.y;
    }

    public void sub(MutableFloatPoint2D rhs, MutableFloatPoint2D result) {
        result.x = this.x - rhs.x;
        result.y = this.y - rhs.y;
    }

    public void add(MutableFloatPoint2D rhs) {
        add(rhs, this);
    }

    public void sub(MutableFloatPoint2D rhs) {
        sub(rhs, this);
    }

    public void scale(double factor) {
        double d = (double) this.x;
        Double.isNaN(d);
        this.x = (float) (d * factor);
        double d2 = (double) this.y;
        Double.isNaN(d2);
        this.y = (float) (d2 * factor);
    }

    public double euclideanDistanceTo(MutableFloatPoint2D other) {
        float xDistance = this.x - other.x;
        float yDistance = this.y - other.y;
        return Math.sqrt((double) ((xDistance * xDistance) + (yDistance * yDistance)));
    }

    public float distanceSquared() {
        float f = this.x;
        float f2 = f * f;
        float f3 = this.y;
        return f2 + (f3 * f3);
    }
}
