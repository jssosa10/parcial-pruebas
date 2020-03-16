package com.google.android.exoplayer2.trackselection;

import com.google.android.exoplayer2.RendererConfiguration;
import com.google.android.exoplayer2.util.Util;

public final class TrackSelectorResult {

    /* renamed from: info reason: collision with root package name */
    public final Object f19info;
    public final int length;
    public final RendererConfiguration[] rendererConfigurations;
    public final TrackSelectionArray selections;

    public TrackSelectorResult(RendererConfiguration[] rendererConfigurations2, TrackSelection[] selections2, Object info2) {
        this.rendererConfigurations = rendererConfigurations2;
        this.selections = new TrackSelectionArray(selections2);
        this.f19info = info2;
        this.length = rendererConfigurations2.length;
    }

    public boolean isRendererEnabled(int index) {
        return this.rendererConfigurations[index] != null;
    }

    public boolean isEquivalent(TrackSelectorResult other) {
        if (other == null || other.selections.length != this.selections.length) {
            return false;
        }
        for (int i = 0; i < this.selections.length; i++) {
            if (!isEquivalent(other, i)) {
                return false;
            }
        }
        return true;
    }

    public boolean isEquivalent(TrackSelectorResult other, int index) {
        boolean z = false;
        if (other == null) {
            return false;
        }
        if (Util.areEqual(this.rendererConfigurations[index], other.rendererConfigurations[index]) && Util.areEqual(this.selections.get(index), other.selections.get(index))) {
            z = true;
        }
        return z;
    }
}
