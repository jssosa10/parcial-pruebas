package com.google.android.exoplayer2.audio;

import android.support.annotation.Nullable;
import android.support.v4.media.session.PlaybackStateCompat;
import com.google.android.exoplayer2.audio.AudioProcessor.UnhandledFormatException;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public final class SonicAudioProcessor implements AudioProcessor {
    private static final float CLOSE_THRESHOLD = 0.01f;
    public static final float MAXIMUM_PITCH = 8.0f;
    public static final float MAXIMUM_SPEED = 8.0f;
    public static final float MINIMUM_PITCH = 0.1f;
    public static final float MINIMUM_SPEED = 0.1f;
    private static final int MIN_BYTES_FOR_SPEEDUP_CALCULATION = 1024;
    public static final int SAMPLE_RATE_NO_CHANGE = -1;
    private ByteBuffer buffer = EMPTY_BUFFER;
    private int channelCount = -1;
    private long inputBytes;
    private boolean inputEnded;
    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private long outputBytes;
    private int outputSampleRateHz = -1;
    private int pendingOutputSampleRateHz = -1;
    private float pitch = 1.0f;
    private int sampleRateHz = -1;
    private ShortBuffer shortBuffer = this.buffer.asShortBuffer();
    @Nullable
    private Sonic sonic;
    private float speed = 1.0f;

    public float setSpeed(float speed2) {
        float speed3 = Util.constrainValue(speed2, 0.1f, 8.0f);
        if (this.speed != speed3) {
            this.speed = speed3;
            this.sonic = null;
        }
        flush();
        return speed3;
    }

    public float setPitch(float pitch2) {
        float pitch3 = Util.constrainValue(pitch2, 0.1f, 8.0f);
        if (this.pitch != pitch3) {
            this.pitch = pitch3;
            this.sonic = null;
        }
        flush();
        return pitch3;
    }

    public void setOutputSampleRateHz(int sampleRateHz2) {
        this.pendingOutputSampleRateHz = sampleRateHz2;
    }

    public long scaleDurationForSpeedup(long duration) {
        long j;
        long j2 = this.outputBytes;
        if (j2 >= PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID) {
            int i = this.outputSampleRateHz;
            int i2 = this.sampleRateHz;
            if (i == i2) {
                j = Util.scaleLargeTimestamp(duration, this.inputBytes, j2);
            } else {
                j = Util.scaleLargeTimestamp(duration, this.inputBytes * ((long) i), j2 * ((long) i2));
            }
            return j;
        }
        double d = (double) this.speed;
        double d2 = (double) duration;
        Double.isNaN(d);
        Double.isNaN(d2);
        return (long) (d * d2);
    }

    public boolean configure(int sampleRateHz2, int channelCount2, int encoding) throws UnhandledFormatException {
        if (encoding == 2) {
            int outputSampleRateHz2 = this.pendingOutputSampleRateHz;
            if (outputSampleRateHz2 == -1) {
                outputSampleRateHz2 = sampleRateHz2;
            }
            if (this.sampleRateHz == sampleRateHz2 && this.channelCount == channelCount2 && this.outputSampleRateHz == outputSampleRateHz2) {
                return false;
            }
            this.sampleRateHz = sampleRateHz2;
            this.channelCount = channelCount2;
            this.outputSampleRateHz = outputSampleRateHz2;
            this.sonic = null;
            return true;
        }
        throw new UnhandledFormatException(sampleRateHz2, channelCount2, encoding);
    }

    public boolean isActive() {
        return this.sampleRateHz != -1 && (Math.abs(this.speed - 1.0f) >= CLOSE_THRESHOLD || Math.abs(this.pitch - 1.0f) >= CLOSE_THRESHOLD || this.outputSampleRateHz != this.sampleRateHz);
    }

    public int getOutputChannelCount() {
        return this.channelCount;
    }

    public int getOutputEncoding() {
        return 2;
    }

    public int getOutputSampleRateHz() {
        return this.outputSampleRateHz;
    }

    public void queueInput(ByteBuffer inputBuffer) {
        Assertions.checkState(this.sonic != null);
        if (inputBuffer.hasRemaining()) {
            ShortBuffer shortBuffer2 = inputBuffer.asShortBuffer();
            int inputSize = inputBuffer.remaining();
            this.inputBytes += (long) inputSize;
            this.sonic.queueInput(shortBuffer2);
            inputBuffer.position(inputBuffer.position() + inputSize);
        }
        int outputSize = this.sonic.getFramesAvailable() * this.channelCount * 2;
        if (outputSize > 0) {
            if (this.buffer.capacity() < outputSize) {
                this.buffer = ByteBuffer.allocateDirect(outputSize).order(ByteOrder.nativeOrder());
                this.shortBuffer = this.buffer.asShortBuffer();
            } else {
                this.buffer.clear();
                this.shortBuffer.clear();
            }
            this.sonic.getOutput(this.shortBuffer);
            this.outputBytes += (long) outputSize;
            this.buffer.limit(outputSize);
            this.outputBuffer = this.buffer;
        }
    }

    public void queueEndOfStream() {
        Assertions.checkState(this.sonic != null);
        this.sonic.queueEndOfStream();
        this.inputEnded = true;
    }

    public ByteBuffer getOutput() {
        ByteBuffer outputBuffer2 = this.outputBuffer;
        this.outputBuffer = EMPTY_BUFFER;
        return outputBuffer2;
    }

    public boolean isEnded() {
        if (this.inputEnded) {
            Sonic sonic2 = this.sonic;
            if (sonic2 == null || sonic2.getFramesAvailable() == 0) {
                return true;
            }
        }
        return false;
    }

    public void flush() {
        if (isActive()) {
            Sonic sonic2 = this.sonic;
            if (sonic2 == null) {
                Sonic sonic3 = new Sonic(this.sampleRateHz, this.channelCount, this.speed, this.pitch, this.outputSampleRateHz);
                this.sonic = sonic3;
            } else {
                sonic2.flush();
            }
        }
        this.outputBuffer = EMPTY_BUFFER;
        this.inputBytes = 0;
        this.outputBytes = 0;
        this.inputEnded = false;
    }

    public void reset() {
        this.speed = 1.0f;
        this.pitch = 1.0f;
        this.channelCount = -1;
        this.sampleRateHz = -1;
        this.outputSampleRateHz = -1;
        this.buffer = EMPTY_BUFFER;
        this.shortBuffer = this.buffer.asShortBuffer();
        this.outputBuffer = EMPTY_BUFFER;
        this.pendingOutputSampleRateHz = -1;
        this.sonic = null;
        this.inputBytes = 0;
        this.outputBytes = 0;
        this.inputEnded = false;
    }
}
