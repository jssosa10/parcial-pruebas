package org.quantumbadger.redreader.views;

public class LiveDHM {
    private final Params mParams;
    private float mPosition;
    private int mStep = 0;
    private float mVelocity;

    public static class Params {
        public float accelerationCoefficient = 30.0f;
        public float endPosition = 0.0f;
        public float startPosition = 0.0f;
        public float startVelocity = 0.0f;
        public float stepLengthSeconds = 0.016666668f;
        public int thresholdMaxSteps = 1000;
        public float thresholdPositionDifference = 0.49f;
        public float thresholdVelocity = 15.0f;
        public float velocityDamping = 0.87f;
    }

    public LiveDHM(Params params) {
        this.mParams = params;
        this.mPosition = params.startPosition;
        this.mVelocity = params.startVelocity;
    }

    public void calculateStep() {
        this.mVelocity -= this.mParams.stepLengthSeconds * ((this.mPosition - this.mParams.endPosition) * this.mParams.accelerationCoefficient);
        this.mVelocity *= this.mParams.velocityDamping;
        this.mPosition += this.mVelocity * this.mParams.stepLengthSeconds;
        this.mStep++;
    }

    public int getCurrentStep() {
        return this.mStep;
    }

    public float getCurrentPosition() {
        return this.mPosition;
    }

    public float getCurrentVelocity() {
        return this.mVelocity;
    }

    public Params getParams() {
        return this.mParams;
    }

    public boolean isEndThresholdReached() {
        if (this.mStep >= this.mParams.thresholdMaxSteps) {
            return true;
        }
        if (Math.abs(this.mPosition) <= this.mParams.thresholdPositionDifference && Math.abs(this.mVelocity) <= this.mParams.thresholdVelocity) {
            return true;
        }
        return false;
    }
}
