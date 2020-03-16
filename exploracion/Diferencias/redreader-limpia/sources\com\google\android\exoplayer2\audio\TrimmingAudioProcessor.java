package com.google.android.exoplayer2.audio;

import com.google.android.exoplayer2.audio.AudioProcessor.UnhandledFormatException;
import com.google.android.exoplayer2.util.Util;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class TrimmingAudioProcessor implements AudioProcessor {
    private static final int OUTPUT_ENCODING = 2;
    private ByteBuffer buffer = EMPTY_BUFFER;
    private int bytesPerFrame;
    private int channelCount = -1;
    private byte[] endBuffer = Util.EMPTY_BYTE_ARRAY;
    private int endBufferSize;
    private boolean inputEnded;
    private boolean isActive;
    private ByteBuffer outputBuffer = EMPTY_BUFFER;
    private int pendingTrimStartBytes;
    private boolean receivedInputSinceConfigure;
    private int sampleRateHz = -1;
    private int trimEndFrames;
    private int trimStartFrames;
    private long trimmedFrameCount;

    public void setTrimFrameCount(int trimStartFrames2, int trimEndFrames2) {
        this.trimStartFrames = trimStartFrames2;
        this.trimEndFrames = trimEndFrames2;
    }

    public void resetTrimmedFrameCount() {
        this.trimmedFrameCount = 0;
    }

    public long getTrimmedFrameCount() {
        return this.trimmedFrameCount;
    }

    public boolean configure(int sampleRateHz2, int channelCount2, int encoding) throws UnhandledFormatException {
        if (encoding == 2) {
            int i = this.endBufferSize;
            if (i > 0) {
                this.trimmedFrameCount += (long) (i / this.bytesPerFrame);
            }
            this.channelCount = channelCount2;
            this.sampleRateHz = sampleRateHz2;
            this.bytesPerFrame = Util.getPcmFrameSize(2, channelCount2);
            int i2 = this.trimEndFrames;
            int i3 = this.bytesPerFrame;
            this.endBuffer = new byte[(i2 * i3)];
            this.endBufferSize = 0;
            int i4 = this.trimStartFrames;
            this.pendingTrimStartBytes = i3 * i4;
            boolean wasActive = this.isActive;
            this.isActive = (i4 == 0 && i2 == 0) ? false : true;
            this.receivedInputSinceConfigure = false;
            if (wasActive != this.isActive) {
                return true;
            }
            return false;
        }
        throw new UnhandledFormatException(sampleRateHz2, channelCount2, encoding);
    }

    public boolean isActive() {
        return this.isActive;
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
        int position = inputBuffer.position();
        int limit = inputBuffer.limit();
        int remaining = limit - position;
        if (remaining != 0) {
            this.receivedInputSinceConfigure = true;
            int trimBytes = Math.min(remaining, this.pendingTrimStartBytes);
            this.trimmedFrameCount += (long) (trimBytes / this.bytesPerFrame);
            this.pendingTrimStartBytes -= trimBytes;
            inputBuffer.position(position + trimBytes);
            if (this.pendingTrimStartBytes <= 0) {
                int remaining2 = remaining - trimBytes;
                int remainingBytesToOutput = (this.endBufferSize + remaining2) - this.endBuffer.length;
                if (this.buffer.capacity() < remainingBytesToOutput) {
                    this.buffer = ByteBuffer.allocateDirect(remainingBytesToOutput).order(ByteOrder.nativeOrder());
                } else {
                    this.buffer.clear();
                }
                int endBufferBytesToOutput = Util.constrainValue(remainingBytesToOutput, 0, this.endBufferSize);
                this.buffer.put(this.endBuffer, 0, endBufferBytesToOutput);
                int inputBufferBytesToOutput = Util.constrainValue(remainingBytesToOutput - endBufferBytesToOutput, 0, remaining2);
                inputBuffer.limit(inputBuffer.position() + inputBufferBytesToOutput);
                this.buffer.put(inputBuffer);
                inputBuffer.limit(limit);
                int remaining3 = remaining2 - inputBufferBytesToOutput;
                this.endBufferSize -= endBufferBytesToOutput;
                byte[] bArr = this.endBuffer;
                System.arraycopy(bArr, endBufferBytesToOutput, bArr, 0, this.endBufferSize);
                inputBuffer.get(this.endBuffer, this.endBufferSize, remaining3);
                this.endBufferSize += remaining3;
                this.buffer.flip();
                this.outputBuffer = this.buffer;
            }
        }
    }

    public void queueEndOfStream() {
        this.inputEnded = true;
    }

    public ByteBuffer getOutput() {
        ByteBuffer outputBuffer2 = this.outputBuffer;
        if (this.inputEnded && this.endBufferSize > 0 && outputBuffer2 == EMPTY_BUFFER) {
            int capacity = this.buffer.capacity();
            int i = this.endBufferSize;
            if (capacity < i) {
                this.buffer = ByteBuffer.allocateDirect(i).order(ByteOrder.nativeOrder());
            } else {
                this.buffer.clear();
            }
            this.buffer.put(this.endBuffer, 0, this.endBufferSize);
            this.endBufferSize = 0;
            this.buffer.flip();
            outputBuffer2 = this.buffer;
        }
        this.outputBuffer = EMPTY_BUFFER;
        return outputBuffer2;
    }

    public boolean isEnded() {
        return this.inputEnded && this.endBufferSize == 0 && this.outputBuffer == EMPTY_BUFFER;
    }

    public void flush() {
        this.outputBuffer = EMPTY_BUFFER;
        this.inputEnded = false;
        if (this.receivedInputSinceConfigure) {
            this.pendingTrimStartBytes = 0;
        }
        this.endBufferSize = 0;
    }

    public void reset() {
        flush();
        this.buffer = EMPTY_BUFFER;
        this.channelCount = -1;
        this.sampleRateHz = -1;
        this.endBuffer = Util.EMPTY_BYTE_ARRAY;
    }
}
