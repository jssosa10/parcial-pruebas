package com.google.android.exoplayer2.audio;

import com.google.android.exoplayer2.audio.AudioProcessor.UnhandledFormatException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class ResamplingAudioProcessor implements AudioProcessor {
    private ByteBuffer buffer = EMPTY_BUFFER;
    private int channelCount = -1;
    private int encoding = 0;
    private boolean inputEnded;
    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private int sampleRateHz = -1;

    public boolean configure(int sampleRateHz2, int channelCount2, int encoding2) throws UnhandledFormatException {
        if (encoding2 != 3 && encoding2 != 2 && encoding2 != Integer.MIN_VALUE && encoding2 != 1073741824) {
            throw new UnhandledFormatException(sampleRateHz2, channelCount2, encoding2);
        } else if (this.sampleRateHz == sampleRateHz2 && this.channelCount == channelCount2 && this.encoding == encoding2) {
            return false;
        } else {
            this.sampleRateHz = sampleRateHz2;
            this.channelCount = channelCount2;
            this.encoding = encoding2;
            return true;
        }
    }

    public boolean isActive() {
        int i = this.encoding;
        return (i == 0 || i == 2) ? false : true;
    }

    public int getOutputChannelCount() {
        return this.channelCount;
    }

    public int getOutputEncoding() {
        return 2;
    }

    public int getOutputSampleRateHz() {
        return this.sampleRateHz;
    }

    public void queueInput(ByteBuffer inputBuffer) {
        int resampledSize;
        int position = inputBuffer.position();
        int limit = inputBuffer.limit();
        int size = limit - position;
        int i = this.encoding;
        if (i == Integer.MIN_VALUE) {
            resampledSize = (size / 3) * 2;
        } else if (i == 3) {
            resampledSize = size * 2;
        } else if (i == 1073741824) {
            resampledSize = size / 2;
        } else {
            throw new IllegalStateException();
        }
        if (this.buffer.capacity() < resampledSize) {
            this.buffer = ByteBuffer.allocateDirect(resampledSize).order(ByteOrder.nativeOrder());
        } else {
            this.buffer.clear();
        }
        int i2 = this.encoding;
        if (i2 == Integer.MIN_VALUE) {
            for (int i3 = position; i3 < limit; i3 += 3) {
                this.buffer.put(inputBuffer.get(i3 + 1));
                this.buffer.put(inputBuffer.get(i3 + 2));
            }
        } else if (i2 == 3) {
            for (int i4 = position; i4 < limit; i4++) {
                this.buffer.put(0);
                this.buffer.put((byte) ((inputBuffer.get(i4) & 255) - 128));
            }
        } else if (i2 == 1073741824) {
            for (int i5 = position; i5 < limit; i5 += 4) {
                this.buffer.put(inputBuffer.get(i5 + 2));
                this.buffer.put(inputBuffer.get(i5 + 3));
            }
        } else {
            throw new IllegalStateException();
        }
        inputBuffer.position(inputBuffer.limit());
        this.buffer.flip();
        this.outputBuffer = this.buffer;
    }

    public void queueEndOfStream() {
        this.inputEnded = true;
    }

    public ByteBuffer getOutput() {
        ByteBuffer outputBuffer2 = this.outputBuffer;
        this.outputBuffer = EMPTY_BUFFER;
        return outputBuffer2;
    }

    public boolean isEnded() {
        return this.inputEnded && this.outputBuffer == EMPTY_BUFFER;
    }

    public void flush() {
        this.outputBuffer = EMPTY_BUFFER;
        this.inputEnded = false;
    }

    public void reset() {
        flush();
        this.sampleRateHz = -1;
        this.channelCount = -1;
        this.encoding = 0;
        this.buffer = EMPTY_BUFFER;
    }
}
