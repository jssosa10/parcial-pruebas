package com.google.android.exoplayer2.text.subrip;

import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.text.Subtitle;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.util.Collections;
import java.util.List;

final class SubripSubtitle implements Subtitle {
    private final long[] cueTimesUs;
    private final Cue[] cues;

    public SubripSubtitle(Cue[] cues2, long[] cueTimesUs2) {
        this.cues = cues2;
        this.cueTimesUs = cueTimesUs2;
    }

    public int getNextEventTimeIndex(long timeUs) {
        int index = Util.binarySearchCeil(this.cueTimesUs, timeUs, false, false);
        if (index < this.cueTimesUs.length) {
            return index;
        }
        return -1;
    }

    public int getEventTimeCount() {
        return this.cueTimesUs.length;
    }

    public long getEventTime(int index) {
        boolean z = true;
        Assertions.checkArgument(index >= 0);
        if (index >= this.cueTimesUs.length) {
            z = false;
        }
        Assertions.checkArgument(z);
        return this.cueTimesUs[index];
    }

    public List<Cue> getCues(long timeUs) {
        int index = Util.binarySearchFloor(this.cueTimesUs, timeUs, true, false);
        if (index != -1) {
            Cue[] cueArr = this.cues;
            if (cueArr[index] != null) {
                return Collections.singletonList(cueArr[index]);
            }
        }
        return Collections.emptyList();
    }
}
