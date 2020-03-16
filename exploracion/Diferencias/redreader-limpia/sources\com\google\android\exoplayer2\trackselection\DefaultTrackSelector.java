package com.google.android.exoplayer2.trackselection;

import android.content.Context;
import android.graphics.Point;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererConfiguration;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection.Factory;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultTrackSelector extends MappingTrackSelector {
    private static final float FRACTION_TO_CONSIDER_FULLSCREEN = 0.98f;
    private static final int[] NO_TRACKS = new int[0];
    private static final int WITHIN_RENDERER_CAPABILITIES_BONUS = 1000;
    private final Factory adaptiveTrackSelectionFactory;
    private final AtomicReference<Parameters> parametersReference;

    private static final class AudioConfigurationTuple {
        public final int channelCount;
        @Nullable
        public final String mimeType;
        public final int sampleRate;

        public AudioConfigurationTuple(int channelCount2, int sampleRate2, @Nullable String mimeType2) {
            this.channelCount = channelCount2;
            this.sampleRate = sampleRate2;
            this.mimeType = mimeType2;
        }

        public boolean equals(@Nullable Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            AudioConfigurationTuple other = (AudioConfigurationTuple) obj;
            if (!(this.channelCount == other.channelCount && this.sampleRate == other.sampleRate && TextUtils.equals(this.mimeType, other.mimeType))) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            int result = ((this.channelCount * 31) + this.sampleRate) * 31;
            String str = this.mimeType;
            return result + (str != null ? str.hashCode() : 0);
        }
    }

    protected static final class AudioTrackScore implements Comparable<AudioTrackScore> {
        private final int bitrate;
        private final int channelCount;
        private final int defaultSelectionFlagScore;
        private final int matchLanguageScore;
        private final Parameters parameters;
        private final int sampleRate;
        private final int withinRendererCapabilitiesScore;

        public AudioTrackScore(Format format, Parameters parameters2, int formatSupport) {
            this.parameters = parameters2;
            int i = 0;
            this.withinRendererCapabilitiesScore = DefaultTrackSelector.isSupported(formatSupport, false) ? 1 : 0;
            this.matchLanguageScore = DefaultTrackSelector.formatHasLanguage(format, parameters2.preferredAudioLanguage) ? 1 : 0;
            if ((format.selectionFlags & 1) != 0) {
                i = 1;
            }
            this.defaultSelectionFlagScore = i;
            this.channelCount = format.channelCount;
            this.sampleRate = format.sampleRate;
            this.bitrate = format.bitrate;
        }

        public int compareTo(@NonNull AudioTrackScore other) {
            int i = this.withinRendererCapabilitiesScore;
            int i2 = other.withinRendererCapabilitiesScore;
            if (i != i2) {
                return DefaultTrackSelector.compareInts(i, i2);
            }
            int i3 = this.matchLanguageScore;
            int i4 = other.matchLanguageScore;
            if (i3 != i4) {
                return DefaultTrackSelector.compareInts(i3, i4);
            }
            int i5 = this.defaultSelectionFlagScore;
            int i6 = other.defaultSelectionFlagScore;
            if (i5 != i6) {
                return DefaultTrackSelector.compareInts(i5, i6);
            }
            if (this.parameters.forceLowestBitrate) {
                return DefaultTrackSelector.compareInts(other.bitrate, this.bitrate);
            }
            int i7 = 1;
            if (this.withinRendererCapabilitiesScore != 1) {
                i7 = -1;
            }
            int resultSign = i7;
            int i8 = this.channelCount;
            int i9 = other.channelCount;
            if (i8 != i9) {
                return DefaultTrackSelector.compareInts(i8, i9) * resultSign;
            }
            int i10 = this.sampleRate;
            int i11 = other.sampleRate;
            if (i10 != i11) {
                return DefaultTrackSelector.compareInts(i10, i11) * resultSign;
            }
            return DefaultTrackSelector.compareInts(this.bitrate, other.bitrate) * resultSign;
        }
    }

    public static final class Parameters implements Parcelable {
        public static final Creator<Parameters> CREATOR = new Creator<Parameters>() {
            public Parameters createFromParcel(Parcel in) {
                return new Parameters(in);
            }

            public Parameters[] newArray(int size) {
                return new Parameters[size];
            }
        };
        public static final Parameters DEFAULT = new Parameters();
        public final boolean allowMixedMimeAdaptiveness;
        public final boolean allowNonSeamlessAdaptiveness;
        public final int disabledTextTrackSelectionFlags;
        public final boolean exceedRendererCapabilitiesIfNecessary;
        public final boolean exceedVideoConstraintsIfNecessary;
        public final boolean forceHighestSupportedBitrate;
        public final boolean forceLowestBitrate;
        public final int maxVideoBitrate;
        public final int maxVideoFrameRate;
        public final int maxVideoHeight;
        public final int maxVideoWidth;
        @Nullable
        public final String preferredAudioLanguage;
        @Nullable
        public final String preferredTextLanguage;
        /* access modifiers changed from: private */
        public final SparseBooleanArray rendererDisabledFlags;
        public final boolean selectUndeterminedTextLanguage;
        /* access modifiers changed from: private */
        public final SparseArray<Map<TrackGroupArray, SelectionOverride>> selectionOverrides;
        public final int tunnelingAudioSessionId;
        public final int viewportHeight;
        public final boolean viewportOrientationMayChange;
        public final int viewportWidth;

        private Parameters() {
            SparseArray sparseArray = r2;
            SparseArray sparseArray2 = new SparseArray();
            SparseBooleanArray sparseBooleanArray = r3;
            SparseBooleanArray sparseBooleanArray2 = new SparseBooleanArray();
            this(sparseArray, sparseBooleanArray, null, null, false, 0, false, false, false, true, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, true, true, Integer.MAX_VALUE, Integer.MAX_VALUE, true, 0);
        }

        Parameters(SparseArray<Map<TrackGroupArray, SelectionOverride>> selectionOverrides2, SparseBooleanArray rendererDisabledFlags2, @Nullable String preferredAudioLanguage2, @Nullable String preferredTextLanguage2, boolean selectUndeterminedTextLanguage2, int disabledTextTrackSelectionFlags2, boolean forceLowestBitrate2, boolean forceHighestSupportedBitrate2, boolean allowMixedMimeAdaptiveness2, boolean allowNonSeamlessAdaptiveness2, int maxVideoWidth2, int maxVideoHeight2, int maxVideoFrameRate2, int maxVideoBitrate2, boolean exceedVideoConstraintsIfNecessary2, boolean exceedRendererCapabilitiesIfNecessary2, int viewportWidth2, int viewportHeight2, boolean viewportOrientationMayChange2, int tunnelingAudioSessionId2) {
            this.selectionOverrides = selectionOverrides2;
            this.rendererDisabledFlags = rendererDisabledFlags2;
            this.preferredAudioLanguage = Util.normalizeLanguageCode(preferredAudioLanguage2);
            this.preferredTextLanguage = Util.normalizeLanguageCode(preferredTextLanguage2);
            this.selectUndeterminedTextLanguage = selectUndeterminedTextLanguage2;
            this.disabledTextTrackSelectionFlags = disabledTextTrackSelectionFlags2;
            this.forceLowestBitrate = forceLowestBitrate2;
            this.forceHighestSupportedBitrate = forceHighestSupportedBitrate2;
            this.allowMixedMimeAdaptiveness = allowMixedMimeAdaptiveness2;
            this.allowNonSeamlessAdaptiveness = allowNonSeamlessAdaptiveness2;
            this.maxVideoWidth = maxVideoWidth2;
            this.maxVideoHeight = maxVideoHeight2;
            this.maxVideoFrameRate = maxVideoFrameRate2;
            this.maxVideoBitrate = maxVideoBitrate2;
            this.exceedVideoConstraintsIfNecessary = exceedVideoConstraintsIfNecessary2;
            this.exceedRendererCapabilitiesIfNecessary = exceedRendererCapabilitiesIfNecessary2;
            this.viewportWidth = viewportWidth2;
            this.viewportHeight = viewportHeight2;
            this.viewportOrientationMayChange = viewportOrientationMayChange2;
            this.tunnelingAudioSessionId = tunnelingAudioSessionId2;
        }

        Parameters(Parcel in) {
            this.selectionOverrides = readSelectionOverrides(in);
            this.rendererDisabledFlags = in.readSparseBooleanArray();
            this.preferredAudioLanguage = in.readString();
            this.preferredTextLanguage = in.readString();
            this.selectUndeterminedTextLanguage = Util.readBoolean(in);
            this.disabledTextTrackSelectionFlags = in.readInt();
            this.forceLowestBitrate = Util.readBoolean(in);
            this.forceHighestSupportedBitrate = Util.readBoolean(in);
            this.allowMixedMimeAdaptiveness = Util.readBoolean(in);
            this.allowNonSeamlessAdaptiveness = Util.readBoolean(in);
            this.maxVideoWidth = in.readInt();
            this.maxVideoHeight = in.readInt();
            this.maxVideoFrameRate = in.readInt();
            this.maxVideoBitrate = in.readInt();
            this.exceedVideoConstraintsIfNecessary = Util.readBoolean(in);
            this.exceedRendererCapabilitiesIfNecessary = Util.readBoolean(in);
            this.viewportWidth = in.readInt();
            this.viewportHeight = in.readInt();
            this.viewportOrientationMayChange = Util.readBoolean(in);
            this.tunnelingAudioSessionId = in.readInt();
        }

        public final boolean getRendererDisabled(int rendererIndex) {
            return this.rendererDisabledFlags.get(rendererIndex);
        }

        public final boolean hasSelectionOverride(int rendererIndex, TrackGroupArray groups) {
            Map<TrackGroupArray, SelectionOverride> overrides = (Map) this.selectionOverrides.get(rendererIndex);
            return overrides != null && overrides.containsKey(groups);
        }

        @Nullable
        public final SelectionOverride getSelectionOverride(int rendererIndex, TrackGroupArray groups) {
            Map<TrackGroupArray, SelectionOverride> overrides = (Map) this.selectionOverrides.get(rendererIndex);
            if (overrides != null) {
                return (SelectionOverride) overrides.get(groups);
            }
            return null;
        }

        public ParametersBuilder buildUpon() {
            return new ParametersBuilder(this);
        }

        public boolean equals(@Nullable Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Parameters other = (Parameters) obj;
            if (!(this.selectUndeterminedTextLanguage == other.selectUndeterminedTextLanguage && this.disabledTextTrackSelectionFlags == other.disabledTextTrackSelectionFlags && this.forceLowestBitrate == other.forceLowestBitrate && this.forceHighestSupportedBitrate == other.forceHighestSupportedBitrate && this.allowMixedMimeAdaptiveness == other.allowMixedMimeAdaptiveness && this.allowNonSeamlessAdaptiveness == other.allowNonSeamlessAdaptiveness && this.maxVideoWidth == other.maxVideoWidth && this.maxVideoHeight == other.maxVideoHeight && this.maxVideoFrameRate == other.maxVideoFrameRate && this.exceedVideoConstraintsIfNecessary == other.exceedVideoConstraintsIfNecessary && this.exceedRendererCapabilitiesIfNecessary == other.exceedRendererCapabilitiesIfNecessary && this.viewportOrientationMayChange == other.viewportOrientationMayChange && this.viewportWidth == other.viewportWidth && this.viewportHeight == other.viewportHeight && this.maxVideoBitrate == other.maxVideoBitrate && this.tunnelingAudioSessionId == other.tunnelingAudioSessionId && TextUtils.equals(this.preferredAudioLanguage, other.preferredAudioLanguage) && TextUtils.equals(this.preferredTextLanguage, other.preferredTextLanguage) && areRendererDisabledFlagsEqual(this.rendererDisabledFlags, other.rendererDisabledFlags) && areSelectionOverridesEqual(this.selectionOverrides, other.selectionOverrides))) {
                z = false;
            }
            return z;
        }

        public int hashCode() {
            int i;
            int result = ((((((((((((((((((((((((((((((((int) this.selectUndeterminedTextLanguage) * true) + this.disabledTextTrackSelectionFlags) * 31) + (this.forceLowestBitrate ? 1 : 0)) * 31) + (this.forceHighestSupportedBitrate ? 1 : 0)) * 31) + (this.allowMixedMimeAdaptiveness ? 1 : 0)) * 31) + (this.allowNonSeamlessAdaptiveness ? 1 : 0)) * 31) + this.maxVideoWidth) * 31) + this.maxVideoHeight) * 31) + this.maxVideoFrameRate) * 31) + (this.exceedVideoConstraintsIfNecessary ? 1 : 0)) * 31) + (this.exceedRendererCapabilitiesIfNecessary ? 1 : 0)) * 31) + (this.viewportOrientationMayChange ? 1 : 0)) * 31) + this.viewportWidth) * 31) + this.viewportHeight) * 31) + this.maxVideoBitrate) * 31) + this.tunnelingAudioSessionId) * 31;
            String str = this.preferredAudioLanguage;
            int i2 = 0;
            if (str == null) {
                i = 0;
            } else {
                i = str.hashCode();
            }
            int result2 = (result + i) * 31;
            String str2 = this.preferredTextLanguage;
            if (str2 != null) {
                i2 = str2.hashCode();
            }
            return result2 + i2;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            writeSelectionOverridesToParcel(dest, this.selectionOverrides);
            dest.writeSparseBooleanArray(this.rendererDisabledFlags);
            dest.writeString(this.preferredAudioLanguage);
            dest.writeString(this.preferredTextLanguage);
            Util.writeBoolean(dest, this.selectUndeterminedTextLanguage);
            dest.writeInt(this.disabledTextTrackSelectionFlags);
            Util.writeBoolean(dest, this.forceLowestBitrate);
            Util.writeBoolean(dest, this.forceHighestSupportedBitrate);
            Util.writeBoolean(dest, this.allowMixedMimeAdaptiveness);
            Util.writeBoolean(dest, this.allowNonSeamlessAdaptiveness);
            dest.writeInt(this.maxVideoWidth);
            dest.writeInt(this.maxVideoHeight);
            dest.writeInt(this.maxVideoFrameRate);
            dest.writeInt(this.maxVideoBitrate);
            Util.writeBoolean(dest, this.exceedVideoConstraintsIfNecessary);
            Util.writeBoolean(dest, this.exceedRendererCapabilitiesIfNecessary);
            dest.writeInt(this.viewportWidth);
            dest.writeInt(this.viewportHeight);
            Util.writeBoolean(dest, this.viewportOrientationMayChange);
            dest.writeInt(this.tunnelingAudioSessionId);
        }

        private static SparseArray<Map<TrackGroupArray, SelectionOverride>> readSelectionOverrides(Parcel in) {
            int renderersWithOverridesCount = in.readInt();
            SparseArray<Map<TrackGroupArray, SelectionOverride>> selectionOverrides2 = new SparseArray<>(renderersWithOverridesCount);
            for (int i = 0; i < renderersWithOverridesCount; i++) {
                int rendererIndex = in.readInt();
                int overrideCount = in.readInt();
                Map<TrackGroupArray, SelectionOverride> overrides = new HashMap<>(overrideCount);
                for (int j = 0; j < overrideCount; j++) {
                    overrides.put((TrackGroupArray) in.readParcelable(TrackGroupArray.class.getClassLoader()), (SelectionOverride) in.readParcelable(SelectionOverride.class.getClassLoader()));
                }
                selectionOverrides2.put(rendererIndex, overrides);
            }
            return selectionOverrides2;
        }

        private static void writeSelectionOverridesToParcel(Parcel dest, SparseArray<Map<TrackGroupArray, SelectionOverride>> selectionOverrides2) {
            int renderersWithOverridesCount = selectionOverrides2.size();
            dest.writeInt(renderersWithOverridesCount);
            for (int i = 0; i < renderersWithOverridesCount; i++) {
                int rendererIndex = selectionOverrides2.keyAt(i);
                Map<TrackGroupArray, SelectionOverride> overrides = (Map) selectionOverrides2.valueAt(i);
                int overrideCount = overrides.size();
                dest.writeInt(rendererIndex);
                dest.writeInt(overrideCount);
                for (Entry<TrackGroupArray, SelectionOverride> override : overrides.entrySet()) {
                    dest.writeParcelable((Parcelable) override.getKey(), 0);
                    dest.writeParcelable((Parcelable) override.getValue(), 0);
                }
            }
        }

        private static boolean areRendererDisabledFlagsEqual(SparseBooleanArray first, SparseBooleanArray second) {
            int firstSize = first.size();
            if (second.size() != firstSize) {
                return false;
            }
            for (int indexInFirst = 0; indexInFirst < firstSize; indexInFirst++) {
                if (second.indexOfKey(first.keyAt(indexInFirst)) < 0) {
                    return false;
                }
            }
            return true;
        }

        private static boolean areSelectionOverridesEqual(SparseArray<Map<TrackGroupArray, SelectionOverride>> first, SparseArray<Map<TrackGroupArray, SelectionOverride>> second) {
            int firstSize = first.size();
            if (second.size() != firstSize) {
                return false;
            }
            for (int indexInFirst = 0; indexInFirst < firstSize; indexInFirst++) {
                int indexInSecond = second.indexOfKey(first.keyAt(indexInFirst));
                if (indexInSecond < 0 || !areSelectionOverridesEqual((Map) first.valueAt(indexInFirst), (Map) second.valueAt(indexInSecond))) {
                    return false;
                }
            }
            return true;
        }

        private static boolean areSelectionOverridesEqual(Map<TrackGroupArray, SelectionOverride> first, Map<TrackGroupArray, SelectionOverride> second) {
            if (second.size() != first.size()) {
                return false;
            }
            for (Entry<TrackGroupArray, SelectionOverride> firstEntry : first.entrySet()) {
                TrackGroupArray key = (TrackGroupArray) firstEntry.getKey();
                if (second.containsKey(key)) {
                    if (!Util.areEqual(firstEntry.getValue(), second.get(key))) {
                    }
                }
                return false;
            }
            return true;
        }
    }

    public static final class ParametersBuilder {
        private boolean allowMixedMimeAdaptiveness;
        private boolean allowNonSeamlessAdaptiveness;
        private int disabledTextTrackSelectionFlags;
        private boolean exceedRendererCapabilitiesIfNecessary;
        private boolean exceedVideoConstraintsIfNecessary;
        private boolean forceHighestSupportedBitrate;
        private boolean forceLowestBitrate;
        private int maxVideoBitrate;
        private int maxVideoFrameRate;
        private int maxVideoHeight;
        private int maxVideoWidth;
        @Nullable
        private String preferredAudioLanguage;
        @Nullable
        private String preferredTextLanguage;
        private final SparseBooleanArray rendererDisabledFlags;
        private boolean selectUndeterminedTextLanguage;
        private final SparseArray<Map<TrackGroupArray, SelectionOverride>> selectionOverrides;
        private int tunnelingAudioSessionId;
        private int viewportHeight;
        private boolean viewportOrientationMayChange;
        private int viewportWidth;

        public ParametersBuilder() {
            this(Parameters.DEFAULT);
        }

        private ParametersBuilder(Parameters initialValues) {
            this.selectionOverrides = cloneSelectionOverrides(initialValues.selectionOverrides);
            this.rendererDisabledFlags = initialValues.rendererDisabledFlags.clone();
            this.preferredAudioLanguage = initialValues.preferredAudioLanguage;
            this.preferredTextLanguage = initialValues.preferredTextLanguage;
            this.selectUndeterminedTextLanguage = initialValues.selectUndeterminedTextLanguage;
            this.disabledTextTrackSelectionFlags = initialValues.disabledTextTrackSelectionFlags;
            this.forceLowestBitrate = initialValues.forceLowestBitrate;
            this.forceHighestSupportedBitrate = initialValues.forceHighestSupportedBitrate;
            this.allowMixedMimeAdaptiveness = initialValues.allowMixedMimeAdaptiveness;
            this.allowNonSeamlessAdaptiveness = initialValues.allowNonSeamlessAdaptiveness;
            this.maxVideoWidth = initialValues.maxVideoWidth;
            this.maxVideoHeight = initialValues.maxVideoHeight;
            this.maxVideoFrameRate = initialValues.maxVideoFrameRate;
            this.maxVideoBitrate = initialValues.maxVideoBitrate;
            this.exceedVideoConstraintsIfNecessary = initialValues.exceedVideoConstraintsIfNecessary;
            this.exceedRendererCapabilitiesIfNecessary = initialValues.exceedRendererCapabilitiesIfNecessary;
            this.viewportWidth = initialValues.viewportWidth;
            this.viewportHeight = initialValues.viewportHeight;
            this.viewportOrientationMayChange = initialValues.viewportOrientationMayChange;
            this.tunnelingAudioSessionId = initialValues.tunnelingAudioSessionId;
        }

        public ParametersBuilder setPreferredAudioLanguage(String preferredAudioLanguage2) {
            this.preferredAudioLanguage = preferredAudioLanguage2;
            return this;
        }

        public ParametersBuilder setPreferredTextLanguage(String preferredTextLanguage2) {
            this.preferredTextLanguage = preferredTextLanguage2;
            return this;
        }

        public ParametersBuilder setSelectUndeterminedTextLanguage(boolean selectUndeterminedTextLanguage2) {
            this.selectUndeterminedTextLanguage = selectUndeterminedTextLanguage2;
            return this;
        }

        public ParametersBuilder setDisabledTextTrackSelectionFlags(int disabledTextTrackSelectionFlags2) {
            this.disabledTextTrackSelectionFlags = disabledTextTrackSelectionFlags2;
            return this;
        }

        public ParametersBuilder setForceLowestBitrate(boolean forceLowestBitrate2) {
            this.forceLowestBitrate = forceLowestBitrate2;
            return this;
        }

        public ParametersBuilder setForceHighestSupportedBitrate(boolean forceHighestSupportedBitrate2) {
            this.forceHighestSupportedBitrate = forceHighestSupportedBitrate2;
            return this;
        }

        public ParametersBuilder setAllowMixedMimeAdaptiveness(boolean allowMixedMimeAdaptiveness2) {
            this.allowMixedMimeAdaptiveness = allowMixedMimeAdaptiveness2;
            return this;
        }

        public ParametersBuilder setAllowNonSeamlessAdaptiveness(boolean allowNonSeamlessAdaptiveness2) {
            this.allowNonSeamlessAdaptiveness = allowNonSeamlessAdaptiveness2;
            return this;
        }

        public ParametersBuilder setMaxVideoSizeSd() {
            return setMaxVideoSize(1279, 719);
        }

        public ParametersBuilder clearVideoSizeConstraints() {
            return setMaxVideoSize(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        public ParametersBuilder setMaxVideoSize(int maxVideoWidth2, int maxVideoHeight2) {
            this.maxVideoWidth = maxVideoWidth2;
            this.maxVideoHeight = maxVideoHeight2;
            return this;
        }

        public ParametersBuilder setMaxVideoFrameRate(int maxVideoFrameRate2) {
            this.maxVideoFrameRate = maxVideoFrameRate2;
            return this;
        }

        public ParametersBuilder setMaxVideoBitrate(int maxVideoBitrate2) {
            this.maxVideoBitrate = maxVideoBitrate2;
            return this;
        }

        public ParametersBuilder setExceedVideoConstraintsIfNecessary(boolean exceedVideoConstraintsIfNecessary2) {
            this.exceedVideoConstraintsIfNecessary = exceedVideoConstraintsIfNecessary2;
            return this;
        }

        public ParametersBuilder setExceedRendererCapabilitiesIfNecessary(boolean exceedRendererCapabilitiesIfNecessary2) {
            this.exceedRendererCapabilitiesIfNecessary = exceedRendererCapabilitiesIfNecessary2;
            return this;
        }

        public ParametersBuilder setViewportSizeToPhysicalDisplaySize(Context context, boolean viewportOrientationMayChange2) {
            Point viewportSize = Util.getPhysicalDisplaySize(context);
            return setViewportSize(viewportSize.x, viewportSize.y, viewportOrientationMayChange2);
        }

        public ParametersBuilder clearViewportSizeConstraints() {
            return setViewportSize(Integer.MAX_VALUE, Integer.MAX_VALUE, true);
        }

        public ParametersBuilder setViewportSize(int viewportWidth2, int viewportHeight2, boolean viewportOrientationMayChange2) {
            this.viewportWidth = viewportWidth2;
            this.viewportHeight = viewportHeight2;
            this.viewportOrientationMayChange = viewportOrientationMayChange2;
            return this;
        }

        public final ParametersBuilder setRendererDisabled(int rendererIndex, boolean disabled) {
            if (this.rendererDisabledFlags.get(rendererIndex) == disabled) {
                return this;
            }
            if (disabled) {
                this.rendererDisabledFlags.put(rendererIndex, true);
            } else {
                this.rendererDisabledFlags.delete(rendererIndex);
            }
            return this;
        }

        public final ParametersBuilder setSelectionOverride(int rendererIndex, TrackGroupArray groups, SelectionOverride override) {
            Map map = (Map) this.selectionOverrides.get(rendererIndex);
            if (map == null) {
                map = new HashMap();
                this.selectionOverrides.put(rendererIndex, map);
            }
            if (map.containsKey(groups) && Util.areEqual(map.get(groups), override)) {
                return this;
            }
            map.put(groups, override);
            return this;
        }

        public final ParametersBuilder clearSelectionOverride(int rendererIndex, TrackGroupArray groups) {
            Map<TrackGroupArray, SelectionOverride> overrides = (Map) this.selectionOverrides.get(rendererIndex);
            if (overrides == null || !overrides.containsKey(groups)) {
                return this;
            }
            overrides.remove(groups);
            if (overrides.isEmpty()) {
                this.selectionOverrides.remove(rendererIndex);
            }
            return this;
        }

        public final ParametersBuilder clearSelectionOverrides(int rendererIndex) {
            Map<TrackGroupArray, SelectionOverride> overrides = (Map) this.selectionOverrides.get(rendererIndex);
            if (overrides == null || overrides.isEmpty()) {
                return this;
            }
            this.selectionOverrides.remove(rendererIndex);
            return this;
        }

        public final ParametersBuilder clearSelectionOverrides() {
            if (this.selectionOverrides.size() == 0) {
                return this;
            }
            this.selectionOverrides.clear();
            return this;
        }

        public ParametersBuilder setTunnelingAudioSessionId(int tunnelingAudioSessionId2) {
            if (this.tunnelingAudioSessionId == tunnelingAudioSessionId2) {
                return this;
            }
            this.tunnelingAudioSessionId = tunnelingAudioSessionId2;
            return this;
        }

        public Parameters build() {
            Parameters parameters = new Parameters(this.selectionOverrides, this.rendererDisabledFlags, this.preferredAudioLanguage, this.preferredTextLanguage, this.selectUndeterminedTextLanguage, this.disabledTextTrackSelectionFlags, this.forceLowestBitrate, this.forceHighestSupportedBitrate, this.allowMixedMimeAdaptiveness, this.allowNonSeamlessAdaptiveness, this.maxVideoWidth, this.maxVideoHeight, this.maxVideoFrameRate, this.maxVideoBitrate, this.exceedVideoConstraintsIfNecessary, this.exceedRendererCapabilitiesIfNecessary, this.viewportWidth, this.viewportHeight, this.viewportOrientationMayChange, this.tunnelingAudioSessionId);
            return parameters;
        }

        private static SparseArray<Map<TrackGroupArray, SelectionOverride>> cloneSelectionOverrides(SparseArray<Map<TrackGroupArray, SelectionOverride>> selectionOverrides2) {
            SparseArray<Map<TrackGroupArray, SelectionOverride>> clone = new SparseArray<>();
            for (int i = 0; i < selectionOverrides2.size(); i++) {
                clone.put(selectionOverrides2.keyAt(i), new HashMap((Map) selectionOverrides2.valueAt(i)));
            }
            return clone;
        }
    }

    public static final class SelectionOverride implements Parcelable {
        public static final Creator<SelectionOverride> CREATOR = new Creator<SelectionOverride>() {
            public SelectionOverride createFromParcel(Parcel in) {
                return new SelectionOverride(in);
            }

            public SelectionOverride[] newArray(int size) {
                return new SelectionOverride[size];
            }
        };
        public final int groupIndex;
        public final int length;
        public final int[] tracks;

        public SelectionOverride(int groupIndex2, int... tracks2) {
            this.groupIndex = groupIndex2;
            this.tracks = Arrays.copyOf(tracks2, tracks2.length);
            this.length = tracks2.length;
            Arrays.sort(this.tracks);
        }

        SelectionOverride(Parcel in) {
            this.groupIndex = in.readInt();
            this.length = in.readByte();
            this.tracks = new int[this.length];
            in.readIntArray(this.tracks);
        }

        public boolean containsTrack(int track) {
            for (int overrideTrack : this.tracks) {
                if (overrideTrack == track) {
                    return true;
                }
            }
            return false;
        }

        public int hashCode() {
            return (this.groupIndex * 31) + Arrays.hashCode(this.tracks);
        }

        public boolean equals(@Nullable Object obj) {
            boolean z = true;
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            SelectionOverride other = (SelectionOverride) obj;
            if (this.groupIndex != other.groupIndex || !Arrays.equals(this.tracks, other.tracks)) {
                z = false;
            }
            return z;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.groupIndex);
            dest.writeInt(this.tracks.length);
            dest.writeIntArray(this.tracks);
        }
    }

    public DefaultTrackSelector() {
        this((Factory) new AdaptiveTrackSelection.Factory());
    }

    @Deprecated
    public DefaultTrackSelector(BandwidthMeter bandwidthMeter) {
        this((Factory) new AdaptiveTrackSelection.Factory(bandwidthMeter));
    }

    public DefaultTrackSelector(Factory adaptiveTrackSelectionFactory2) {
        this.adaptiveTrackSelectionFactory = adaptiveTrackSelectionFactory2;
        this.parametersReference = new AtomicReference<>(Parameters.DEFAULT);
    }

    public void setParameters(Parameters parameters) {
        Assertions.checkNotNull(parameters);
        if (!((Parameters) this.parametersReference.getAndSet(parameters)).equals(parameters)) {
            invalidate();
        }
    }

    public void setParameters(ParametersBuilder parametersBuilder) {
        setParameters(parametersBuilder.build());
    }

    public Parameters getParameters() {
        return (Parameters) this.parametersReference.get();
    }

    public ParametersBuilder buildUponParameters() {
        return getParameters().buildUpon();
    }

    @Deprecated
    public final void setRendererDisabled(int rendererIndex, boolean disabled) {
        setParameters(buildUponParameters().setRendererDisabled(rendererIndex, disabled));
    }

    @Deprecated
    public final boolean getRendererDisabled(int rendererIndex) {
        return getParameters().getRendererDisabled(rendererIndex);
    }

    @Deprecated
    public final void setSelectionOverride(int rendererIndex, TrackGroupArray groups, SelectionOverride override) {
        setParameters(buildUponParameters().setSelectionOverride(rendererIndex, groups, override));
    }

    @Deprecated
    public final boolean hasSelectionOverride(int rendererIndex, TrackGroupArray groups) {
        return getParameters().hasSelectionOverride(rendererIndex, groups);
    }

    @Nullable
    @Deprecated
    public final SelectionOverride getSelectionOverride(int rendererIndex, TrackGroupArray groups) {
        return getParameters().getSelectionOverride(rendererIndex, groups);
    }

    @Deprecated
    public final void clearSelectionOverride(int rendererIndex, TrackGroupArray groups) {
        setParameters(buildUponParameters().clearSelectionOverride(rendererIndex, groups));
    }

    @Deprecated
    public final void clearSelectionOverrides(int rendererIndex) {
        setParameters(buildUponParameters().clearSelectionOverrides(rendererIndex));
    }

    @Deprecated
    public final void clearSelectionOverrides() {
        setParameters(buildUponParameters().clearSelectionOverrides());
    }

    @Deprecated
    public void setTunnelingAudioSessionId(int tunnelingAudioSessionId) {
        setParameters(buildUponParameters().setTunnelingAudioSessionId(tunnelingAudioSessionId));
    }

    /* access modifiers changed from: protected */
    public final Pair<RendererConfiguration[], TrackSelection[]> selectTracks(MappedTrackInfo mappedTrackInfo, int[][][] rendererFormatSupports, int[] rendererMixedMimeTypeAdaptationSupports) throws ExoPlaybackException {
        Parameters params = (Parameters) this.parametersReference.get();
        int rendererCount = mappedTrackInfo.getRendererCount();
        TrackSelection[] rendererTrackSelections = selectAllTracks(mappedTrackInfo, rendererFormatSupports, rendererMixedMimeTypeAdaptationSupports, params);
        for (int i = 0; i < rendererCount; i++) {
            if (params.getRendererDisabled(i)) {
                rendererTrackSelections[i] = null;
            } else {
                TrackGroupArray rendererTrackGroups = mappedTrackInfo.getTrackGroups(i);
                if (params.hasSelectionOverride(i, rendererTrackGroups)) {
                    SelectionOverride override = params.getSelectionOverride(i, rendererTrackGroups);
                    if (override == null) {
                        rendererTrackSelections[i] = null;
                    } else if (override.length == 1) {
                        rendererTrackSelections[i] = new FixedTrackSelection(rendererTrackGroups.get(override.groupIndex), override.tracks[0]);
                    } else {
                        rendererTrackSelections[i] = ((Factory) Assertions.checkNotNull(this.adaptiveTrackSelectionFactory)).createTrackSelection(rendererTrackGroups.get(override.groupIndex), getBandwidthMeter(), override.tracks);
                    }
                }
            }
        }
        RendererConfiguration[] rendererConfigurations = new RendererConfiguration[rendererCount];
        for (int i2 = 0; i2 < rendererCount; i2++) {
            rendererConfigurations[i2] = !params.getRendererDisabled(i2) && (mappedTrackInfo.getRendererType(i2) == 6 || rendererTrackSelections[i2] != null) ? RendererConfiguration.DEFAULT : null;
        }
        maybeConfigureRenderersForTunneling(mappedTrackInfo, rendererFormatSupports, rendererConfigurations, rendererTrackSelections, params.tunnelingAudioSessionId);
        return Pair.create(rendererConfigurations, rendererTrackSelections);
    }

    /* access modifiers changed from: protected */
    public TrackSelection[] selectAllTracks(MappedTrackInfo mappedTrackInfo, int[][][] rendererFormatSupports, int[] rendererMixedMimeTypeAdaptationSupports, Parameters params) throws ExoPlaybackException {
        int rendererCount;
        int selectedTextTrackScore;
        int selectedTextRendererIndex;
        MappedTrackInfo mappedTrackInfo2 = mappedTrackInfo;
        Parameters parameters = params;
        int rendererCount2 = mappedTrackInfo.getRendererCount();
        TrackSelection[] rendererTrackSelections = new TrackSelection[rendererCount2];
        boolean seenVideoRendererWithMappedTracks = false;
        boolean selectedVideoTracks = false;
        for (int i = 0; i < rendererCount2; i++) {
            if (2 == mappedTrackInfo2.getRendererType(i)) {
                boolean z = false;
                if (!selectedVideoTracks) {
                    rendererTrackSelections[i] = selectVideoTrack(mappedTrackInfo2.getTrackGroups(i), rendererFormatSupports[i], rendererMixedMimeTypeAdaptationSupports[i], params, this.adaptiveTrackSelectionFactory);
                    selectedVideoTracks = rendererTrackSelections[i] != null;
                }
                if (mappedTrackInfo2.getTrackGroups(i).length > 0) {
                    z = true;
                }
                seenVideoRendererWithMappedTracks |= z;
            }
        }
        AudioTrackScore selectedAudioTrackScore = null;
        int selectedAudioRendererIndex = -1;
        int selectedTextTrackScore2 = Integer.MIN_VALUE;
        int i2 = 0;
        int selectedTextRendererIndex2 = -1;
        while (i2 < rendererCount2) {
            int trackType = mappedTrackInfo2.getRendererType(i2);
            switch (trackType) {
                case 1:
                    rendererCount = rendererCount2;
                    int i3 = trackType;
                    selectedTextRendererIndex = selectedTextRendererIndex2;
                    selectedTextTrackScore = selectedTextTrackScore2;
                    Pair<TrackSelection, AudioTrackScore> audioSelection = selectAudioTrack(mappedTrackInfo2.getTrackGroups(i2), rendererFormatSupports[i2], rendererMixedMimeTypeAdaptationSupports[i2], params, seenVideoRendererWithMappedTracks ? null : this.adaptiveTrackSelectionFactory);
                    if (audioSelection != null && (selectedAudioTrackScore == null || ((AudioTrackScore) audioSelection.second).compareTo(selectedAudioTrackScore) > 0)) {
                        if (selectedAudioRendererIndex != -1) {
                            rendererTrackSelections[selectedAudioRendererIndex] = null;
                        }
                        rendererTrackSelections[i2] = (TrackSelection) audioSelection.first;
                        selectedAudioTrackScore = (AudioTrackScore) audioSelection.second;
                        selectedAudioRendererIndex = i2;
                        selectedTextRendererIndex2 = selectedTextRendererIndex;
                        selectedTextTrackScore2 = selectedTextTrackScore;
                        continue;
                    }
                case 2:
                    selectedTextRendererIndex = selectedTextRendererIndex2;
                    selectedTextTrackScore = selectedTextTrackScore2;
                    rendererCount = rendererCount2;
                    selectedTextRendererIndex2 = selectedTextRendererIndex;
                    selectedTextTrackScore2 = selectedTextTrackScore;
                    break;
                case 3:
                    Pair<TrackSelection, Integer> textSelection = selectTextTrack(mappedTrackInfo2.getTrackGroups(i2), rendererFormatSupports[i2], parameters);
                    if (textSelection == null || ((Integer) textSelection.second).intValue() <= selectedTextTrackScore2) {
                        selectedTextRendererIndex = selectedTextRendererIndex2;
                        selectedTextTrackScore = selectedTextTrackScore2;
                        rendererCount = rendererCount2;
                    } else {
                        if (selectedTextRendererIndex2 != -1) {
                            rendererTrackSelections[selectedTextRendererIndex2] = null;
                        }
                        rendererTrackSelections[i2] = (TrackSelection) textSelection.first;
                        selectedTextTrackScore2 = ((Integer) textSelection.second).intValue();
                        selectedTextRendererIndex2 = i2;
                        rendererCount = rendererCount2;
                        continue;
                    }
                    break;
                default:
                    selectedTextRendererIndex = selectedTextRendererIndex2;
                    selectedTextTrackScore = selectedTextTrackScore2;
                    rendererCount = rendererCount2;
                    rendererTrackSelections[i2] = selectOtherTrack(trackType, mappedTrackInfo2.getTrackGroups(i2), rendererFormatSupports[i2], parameters);
            }
            selectedTextRendererIndex2 = selectedTextRendererIndex;
            selectedTextTrackScore2 = selectedTextTrackScore;
            i2++;
            rendererCount2 = rendererCount;
        }
        int i4 = selectedTextRendererIndex2;
        int i5 = selectedTextTrackScore2;
        int i6 = rendererCount2;
        return rendererTrackSelections;
    }

    /* access modifiers changed from: protected */
    @Nullable
    public TrackSelection selectVideoTrack(TrackGroupArray groups, int[][] formatSupports, int mixedMimeTypeAdaptationSupports, Parameters params, @Nullable Factory adaptiveTrackSelectionFactory2) throws ExoPlaybackException {
        TrackSelection selection = null;
        if (!params.forceHighestSupportedBitrate && !params.forceLowestBitrate && adaptiveTrackSelectionFactory2 != null) {
            selection = selectAdaptiveVideoTrack(groups, formatSupports, mixedMimeTypeAdaptationSupports, params, adaptiveTrackSelectionFactory2, getBandwidthMeter());
        }
        if (selection == null) {
            return selectFixedVideoTrack(groups, formatSupports, params);
        }
        return selection;
    }

    @Nullable
    private static TrackSelection selectAdaptiveVideoTrack(TrackGroupArray groups, int[][] formatSupport, int mixedMimeTypeAdaptationSupports, Parameters params, Factory adaptiveTrackSelectionFactory2, BandwidthMeter bandwidthMeter) throws ExoPlaybackException {
        TrackGroupArray trackGroupArray = groups;
        Parameters parameters = params;
        int requiredAdaptiveSupport = parameters.allowNonSeamlessAdaptiveness ? 24 : 16;
        boolean allowMixedMimeTypes = parameters.allowMixedMimeAdaptiveness && (mixedMimeTypeAdaptationSupports & requiredAdaptiveSupport) != 0;
        for (int i = 0; i < trackGroupArray.length; i++) {
            TrackGroup group = trackGroupArray.get(i);
            int[] adaptiveTracks = getAdaptiveVideoTracksForGroup(group, formatSupport[i], allowMixedMimeTypes, requiredAdaptiveSupport, parameters.maxVideoWidth, parameters.maxVideoHeight, parameters.maxVideoFrameRate, parameters.maxVideoBitrate, parameters.viewportWidth, parameters.viewportHeight, parameters.viewportOrientationMayChange);
            if (adaptiveTracks.length > 0) {
                return ((Factory) Assertions.checkNotNull(adaptiveTrackSelectionFactory2)).createTrackSelection(group, bandwidthMeter, adaptiveTracks);
            }
            BandwidthMeter bandwidthMeter2 = bandwidthMeter;
        }
        BandwidthMeter bandwidthMeter3 = bandwidthMeter;
        return null;
    }

    private static int[] getAdaptiveVideoTracksForGroup(TrackGroup group, int[] formatSupport, boolean allowMixedMimeTypes, int requiredAdaptiveSupport, int maxVideoWidth, int maxVideoHeight, int maxVideoFrameRate, int maxVideoBitrate, int viewportWidth, int viewportHeight, boolean viewportOrientationMayChange) {
        String selectedMimeType;
        int i;
        int selectedMimeTypeTrackCount;
        TrackGroup trackGroup = group;
        if (trackGroup.length < 2) {
            return NO_TRACKS;
        }
        List<Integer> selectedTrackIndices = getViewportFilteredTrackIndices(trackGroup, viewportWidth, viewportHeight, viewportOrientationMayChange);
        if (selectedTrackIndices.size() < 2) {
            return NO_TRACKS;
        }
        if (!allowMixedMimeTypes) {
            HashSet hashSet = new HashSet();
            selectedMimeType = null;
            int selectedMimeTypeTrackCount2 = 0;
            int i2 = 0;
            while (i2 < selectedTrackIndices.size()) {
                int trackIndex = ((Integer) selectedTrackIndices.get(i2)).intValue();
                String sampleMimeType = trackGroup.getFormat(trackIndex).sampleMimeType;
                if (hashSet.add(sampleMimeType)) {
                    String sampleMimeType2 = sampleMimeType;
                    int i3 = trackIndex;
                    selectedMimeTypeTrackCount = selectedMimeTypeTrackCount2;
                    i = i2;
                    int countForMimeType = getAdaptiveVideoTrackCountForMimeType(group, formatSupport, requiredAdaptiveSupport, sampleMimeType, maxVideoWidth, maxVideoHeight, maxVideoFrameRate, maxVideoBitrate, selectedTrackIndices);
                    if (countForMimeType > selectedMimeTypeTrackCount) {
                        selectedMimeType = sampleMimeType2;
                        selectedMimeTypeTrackCount2 = countForMimeType;
                        i2 = i + 1;
                    }
                } else {
                    int i4 = trackIndex;
                    selectedMimeTypeTrackCount = selectedMimeTypeTrackCount2;
                    i = i2;
                }
                selectedMimeTypeTrackCount2 = selectedMimeTypeTrackCount;
                i2 = i + 1;
            }
            int i5 = selectedMimeTypeTrackCount2;
            int i6 = i2;
        } else {
            selectedMimeType = null;
        }
        filterAdaptiveVideoTrackCountForMimeType(group, formatSupport, requiredAdaptiveSupport, selectedMimeType, maxVideoWidth, maxVideoHeight, maxVideoFrameRate, maxVideoBitrate, selectedTrackIndices);
        return selectedTrackIndices.size() < 2 ? NO_TRACKS : Util.toArray(selectedTrackIndices);
    }

    private static int getAdaptiveVideoTrackCountForMimeType(TrackGroup group, int[] formatSupport, int requiredAdaptiveSupport, @Nullable String mimeType, int maxVideoWidth, int maxVideoHeight, int maxVideoFrameRate, int maxVideoBitrate, List<Integer> selectedTrackIndices) {
        int adaptiveTrackCount = 0;
        for (int i = 0; i < selectedTrackIndices.size(); i++) {
            int trackIndex = ((Integer) selectedTrackIndices.get(i)).intValue();
            TrackGroup trackGroup = group;
            if (isSupportedAdaptiveVideoTrack(group.getFormat(trackIndex), mimeType, formatSupport[trackIndex], requiredAdaptiveSupport, maxVideoWidth, maxVideoHeight, maxVideoFrameRate, maxVideoBitrate)) {
                adaptiveTrackCount++;
            }
        }
        TrackGroup trackGroup2 = group;
        List<Integer> list = selectedTrackIndices;
        return adaptiveTrackCount;
    }

    private static void filterAdaptiveVideoTrackCountForMimeType(TrackGroup group, int[] formatSupport, int requiredAdaptiveSupport, @Nullable String mimeType, int maxVideoWidth, int maxVideoHeight, int maxVideoFrameRate, int maxVideoBitrate, List<Integer> selectedTrackIndices) {
        List<Integer> list = selectedTrackIndices;
        for (int i = selectedTrackIndices.size() - 1; i >= 0; i--) {
            int trackIndex = ((Integer) list.get(i)).intValue();
            TrackGroup trackGroup = group;
            if (!isSupportedAdaptiveVideoTrack(group.getFormat(trackIndex), mimeType, formatSupport[trackIndex], requiredAdaptiveSupport, maxVideoWidth, maxVideoHeight, maxVideoFrameRate, maxVideoBitrate)) {
                list.remove(i);
            }
        }
        TrackGroup trackGroup2 = group;
    }

    private static boolean isSupportedAdaptiveVideoTrack(Format format, @Nullable String mimeType, int formatSupport, int requiredAdaptiveSupport, int maxVideoWidth, int maxVideoHeight, int maxVideoFrameRate, int maxVideoBitrate) {
        if (!isSupported(formatSupport, false) || (formatSupport & requiredAdaptiveSupport) == 0) {
            return false;
        }
        if (mimeType != null && !Util.areEqual(format.sampleMimeType, mimeType)) {
            return false;
        }
        if (format.width != -1 && format.width > maxVideoWidth) {
            return false;
        }
        if (format.height != -1 && format.height > maxVideoHeight) {
            return false;
        }
        if (format.frameRate != -1.0f && format.frameRate > ((float) maxVideoFrameRate)) {
            return false;
        }
        if (format.bitrate == -1 || format.bitrate <= maxVideoBitrate) {
            return true;
        }
        return false;
    }

    @Nullable
    private static TrackSelection selectFixedVideoTrack(TrackGroupArray groups, int[][] formatSupports, Parameters params) {
        int formatPixelCount;
        TrackGroupArray trackGroupArray = groups;
        Parameters parameters = params;
        TrackGroup selectedGroup = null;
        int selectedTrackIndex = 0;
        int selectedTrackScore = 0;
        int selectedBitrate = -1;
        int selectedPixelCount = -1;
        int groupIndex = 0;
        while (groupIndex < trackGroupArray.length) {
            TrackGroup trackGroup = trackGroupArray.get(groupIndex);
            List<Integer> selectedTrackIndices = getViewportFilteredTrackIndices(trackGroup, parameters.viewportWidth, parameters.viewportHeight, parameters.viewportOrientationMayChange);
            int[] trackFormatSupport = formatSupports[groupIndex];
            int trackIndex = 0;
            while (trackIndex < trackGroup.length) {
                if (isSupported(trackFormatSupport[trackIndex], parameters.exceedRendererCapabilitiesIfNecessary)) {
                    Format format = trackGroup.getFormat(trackIndex);
                    boolean isWithinConstraints = selectedTrackIndices.contains(Integer.valueOf(trackIndex)) && (format.width == -1 || format.width <= parameters.maxVideoWidth) && ((format.height == -1 || format.height <= parameters.maxVideoHeight) && ((format.frameRate == -1.0f || format.frameRate <= ((float) parameters.maxVideoFrameRate)) && (format.bitrate == -1 || format.bitrate <= parameters.maxVideoBitrate)));
                    if (isWithinConstraints || parameters.exceedVideoConstraintsIfNecessary) {
                        int trackScore = isWithinConstraints ? 2 : 1;
                        boolean isWithinCapabilities = isSupported(trackFormatSupport[trackIndex], false);
                        if (isWithinCapabilities) {
                            trackScore += 1000;
                        }
                        boolean selectTrack = trackScore > selectedTrackScore;
                        if (trackScore == selectedTrackScore) {
                            if (parameters.forceLowestBitrate) {
                                selectTrack = compareFormatValues(format.bitrate, selectedBitrate) < 0;
                            } else {
                                int formatPixelCount2 = format.getPixelCount();
                                if (formatPixelCount2 != selectedPixelCount) {
                                    int i = formatPixelCount2;
                                    formatPixelCount = compareFormatValues(formatPixelCount2, selectedPixelCount);
                                } else {
                                    int i2 = formatPixelCount2;
                                    formatPixelCount = compareFormatValues(format.bitrate, selectedBitrate);
                                }
                                selectTrack = !isWithinCapabilities || !isWithinConstraints ? formatPixelCount < 0 : formatPixelCount > 0;
                            }
                        }
                        if (selectTrack) {
                            selectedGroup = trackGroup;
                            selectedTrackIndex = trackIndex;
                            selectedTrackScore = trackScore;
                            selectedBitrate = format.bitrate;
                            selectedPixelCount = format.getPixelCount();
                        }
                    }
                }
                trackIndex++;
                TrackGroupArray trackGroupArray2 = groups;
            }
            groupIndex++;
            trackGroupArray = groups;
        }
        if (selectedGroup == null) {
            return null;
        }
        return new FixedTrackSelection(selectedGroup, selectedTrackIndex);
    }

    /* access modifiers changed from: protected */
    @Nullable
    public Pair<TrackSelection, AudioTrackScore> selectAudioTrack(TrackGroupArray groups, int[][] formatSupports, int mixedMimeTypeAdaptationSupports, Parameters params, @Nullable Factory adaptiveTrackSelectionFactory2) throws ExoPlaybackException {
        int selectedTrackIndex = -1;
        int selectedGroupIndex = -1;
        AudioTrackScore selectedTrackScore = null;
        for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
            TrackGroup trackGroup = groups.get(groupIndex);
            int[] trackFormatSupport = formatSupports[groupIndex];
            for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                if (isSupported(trackFormatSupport[trackIndex], params.exceedRendererCapabilitiesIfNecessary)) {
                    AudioTrackScore trackScore = new AudioTrackScore(trackGroup.getFormat(trackIndex), params, trackFormatSupport[trackIndex]);
                    if (selectedTrackScore == null || trackScore.compareTo(selectedTrackScore) > 0) {
                        selectedGroupIndex = groupIndex;
                        selectedTrackIndex = trackIndex;
                        selectedTrackScore = trackScore;
                    }
                }
            }
        }
        if (selectedGroupIndex == -1) {
            return null;
        }
        TrackGroup selectedGroup = groups.get(selectedGroupIndex);
        TrackSelection selection = null;
        if (!params.forceHighestSupportedBitrate && !params.forceLowestBitrate && adaptiveTrackSelectionFactory2 != null) {
            int[] adaptiveTracks = getAdaptiveAudioTracks(selectedGroup, formatSupports[selectedGroupIndex], params.allowMixedMimeAdaptiveness);
            if (adaptiveTracks.length > 0) {
                selection = adaptiveTrackSelectionFactory2.createTrackSelection(selectedGroup, getBandwidthMeter(), adaptiveTracks);
            }
        }
        if (selection == null) {
            selection = new FixedTrackSelection(selectedGroup, selectedTrackIndex);
        }
        return Pair.create(selection, Assertions.checkNotNull(selectedTrackScore));
    }

    private static int[] getAdaptiveAudioTracks(TrackGroup group, int[] formatSupport, boolean allowMixedMimeTypes) {
        int selectedConfigurationTrackCount = 0;
        AudioConfigurationTuple selectedConfiguration = null;
        HashSet<AudioConfigurationTuple> seenConfigurationTuples = new HashSet<>();
        for (int i = 0; i < group.length; i++) {
            Format format = group.getFormat(i);
            AudioConfigurationTuple configuration = new AudioConfigurationTuple(format.channelCount, format.sampleRate, allowMixedMimeTypes ? null : format.sampleMimeType);
            if (seenConfigurationTuples.add(configuration)) {
                int configurationCount = getAdaptiveAudioTrackCount(group, formatSupport, configuration);
                if (configurationCount > selectedConfigurationTrackCount) {
                    selectedConfiguration = configuration;
                    selectedConfigurationTrackCount = configurationCount;
                }
            }
        }
        if (selectedConfigurationTrackCount <= 1) {
            return NO_TRACKS;
        }
        int[] adaptiveIndices = new int[selectedConfigurationTrackCount];
        int index = 0;
        for (int i2 = 0; i2 < group.length; i2++) {
            if (isSupportedAdaptiveAudioTrack(group.getFormat(i2), formatSupport[i2], (AudioConfigurationTuple) Assertions.checkNotNull(selectedConfiguration))) {
                int index2 = index + 1;
                adaptiveIndices[index] = i2;
                index = index2;
            }
        }
        return adaptiveIndices;
    }

    private static int getAdaptiveAudioTrackCount(TrackGroup group, int[] formatSupport, AudioConfigurationTuple configuration) {
        int count = 0;
        for (int i = 0; i < group.length; i++) {
            if (isSupportedAdaptiveAudioTrack(group.getFormat(i), formatSupport[i], configuration)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isSupportedAdaptiveAudioTrack(Format format, int formatSupport, AudioConfigurationTuple configuration) {
        if (!isSupported(formatSupport, false) || format.channelCount != configuration.channelCount || format.sampleRate != configuration.sampleRate) {
            return false;
        }
        if (configuration.mimeType == null || TextUtils.equals(configuration.mimeType, format.sampleMimeType)) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: protected */
    @Nullable
    public Pair<TrackSelection, Integer> selectTextTrack(TrackGroupArray groups, int[][] formatSupport, Parameters params) throws ExoPlaybackException {
        int trackScore;
        int trackScore2;
        TrackGroupArray trackGroupArray = groups;
        Parameters parameters = params;
        TrackGroup selectedGroup = null;
        int selectedTrackIndex = 0;
        int selectedTrackScore = 0;
        int groupIndex = 0;
        while (groupIndex < trackGroupArray.length) {
            TrackGroup trackGroup = trackGroupArray.get(groupIndex);
            int[] trackFormatSupport = formatSupport[groupIndex];
            int trackIndex = 0;
            while (trackIndex < trackGroup.length) {
                if (isSupported(trackFormatSupport[trackIndex], parameters.exceedRendererCapabilitiesIfNecessary)) {
                    Format format = trackGroup.getFormat(trackIndex);
                    int maskedSelectionFlags = format.selectionFlags & (parameters.disabledTextTrackSelectionFlags ^ -1);
                    boolean isForced = true;
                    boolean isDefault = (maskedSelectionFlags & 1) != 0;
                    if ((maskedSelectionFlags & 2) == 0) {
                        isForced = false;
                    }
                    boolean preferredLanguageFound = formatHasLanguage(format, parameters.preferredTextLanguage);
                    if (preferredLanguageFound || (parameters.selectUndeterminedTextLanguage && formatHasNoLanguage(format))) {
                        if (isDefault) {
                            trackScore2 = 8;
                        } else if (!isForced) {
                            trackScore2 = 6;
                        } else {
                            trackScore2 = 4;
                        }
                        trackScore = trackScore2 + (preferredLanguageFound);
                    } else if (isDefault) {
                        trackScore = 3;
                    } else if (isForced) {
                        if (formatHasLanguage(format, parameters.preferredAudioLanguage)) {
                            trackScore = 2;
                        } else {
                            trackScore = 1;
                        }
                    }
                    if (isSupported(trackFormatSupport[trackIndex], false)) {
                        trackScore += 1000;
                    }
                    if (trackScore > selectedTrackScore) {
                        selectedGroup = trackGroup;
                        selectedTrackIndex = trackIndex;
                        selectedTrackScore = trackScore;
                    }
                }
                trackIndex++;
                TrackGroupArray trackGroupArray2 = groups;
            }
            groupIndex++;
            trackGroupArray = groups;
        }
        if (selectedGroup == null) {
            return null;
        }
        return Pair.create(new FixedTrackSelection(selectedGroup, selectedTrackIndex), Integer.valueOf(selectedTrackScore));
    }

    /* access modifiers changed from: protected */
    @Nullable
    public TrackSelection selectOtherTrack(int trackType, TrackGroupArray groups, int[][] formatSupport, Parameters params) throws ExoPlaybackException {
        TrackGroupArray trackGroupArray = groups;
        TrackGroup selectedGroup = null;
        int selectedTrackIndex = 0;
        int selectedTrackScore = 0;
        for (int groupIndex = 0; groupIndex < trackGroupArray.length; groupIndex++) {
            TrackGroup trackGroup = trackGroupArray.get(groupIndex);
            int[] trackFormatSupport = formatSupport[groupIndex];
            for (int trackIndex = 0; trackIndex < trackGroup.length; trackIndex++) {
                if (isSupported(trackFormatSupport[trackIndex], params.exceedRendererCapabilitiesIfNecessary)) {
                    int trackScore = 1;
                    if ((trackGroup.getFormat(trackIndex).selectionFlags & 1) != 0) {
                        trackScore = 2;
                    }
                    if (isSupported(trackFormatSupport[trackIndex], false)) {
                        trackScore += 1000;
                    }
                    if (trackScore > selectedTrackScore) {
                        selectedGroup = trackGroup;
                        selectedTrackIndex = trackIndex;
                        selectedTrackScore = trackScore;
                    }
                }
            }
            Parameters parameters = params;
        }
        Parameters parameters2 = params;
        if (selectedGroup == null) {
            return null;
        }
        return new FixedTrackSelection(selectedGroup, selectedTrackIndex);
    }

    private static void maybeConfigureRenderersForTunneling(MappedTrackInfo mappedTrackInfo, int[][][] renderererFormatSupports, RendererConfiguration[] rendererConfigurations, TrackSelection[] trackSelections, int tunnelingAudioSessionId) {
        boolean z;
        if (tunnelingAudioSessionId != 0) {
            int tunnelingAudioRendererIndex = -1;
            int tunnelingVideoRendererIndex = -1;
            boolean enableTunneling = true;
            int i = 0;
            while (true) {
                z = true;
                if (i >= mappedTrackInfo.getRendererCount()) {
                    break;
                }
                int rendererType = mappedTrackInfo.getRendererType(i);
                TrackSelection trackSelection = trackSelections[i];
                if ((rendererType == 1 || rendererType == 2) && trackSelection != null && rendererSupportsTunneling(renderererFormatSupports[i], mappedTrackInfo.getTrackGroups(i), trackSelection)) {
                    if (rendererType == 1) {
                        if (tunnelingAudioRendererIndex != -1) {
                            enableTunneling = false;
                            break;
                        }
                        tunnelingAudioRendererIndex = i;
                    } else if (tunnelingVideoRendererIndex != -1) {
                        enableTunneling = false;
                        break;
                    } else {
                        tunnelingVideoRendererIndex = i;
                    }
                }
                i++;
            }
            if (tunnelingAudioRendererIndex == -1 || tunnelingVideoRendererIndex == -1) {
                z = false;
            }
            if (enableTunneling && z) {
                RendererConfiguration tunnelingRendererConfiguration = new RendererConfiguration(tunnelingAudioSessionId);
                rendererConfigurations[tunnelingAudioRendererIndex] = tunnelingRendererConfiguration;
                rendererConfigurations[tunnelingVideoRendererIndex] = tunnelingRendererConfiguration;
            }
        }
    }

    private static boolean rendererSupportsTunneling(int[][] formatSupports, TrackGroupArray trackGroups, TrackSelection selection) {
        if (selection == null) {
            return false;
        }
        int trackGroupIndex = trackGroups.indexOf(selection.getTrackGroup());
        for (int i = 0; i < selection.length(); i++) {
            if ((formatSupports[trackGroupIndex][selection.getIndexInTrackGroup(i)] & 32) != 32) {
                return false;
            }
        }
        return true;
    }

    private static int compareFormatValues(int first, int second) {
        if (first == -1) {
            return second == -1 ? 0 : -1;
        }
        if (second == -1) {
            return 1;
        }
        return first - second;
    }

    protected static boolean isSupported(int formatSupport, boolean allowExceedsCapabilities) {
        int maskedSupport = formatSupport & 7;
        return maskedSupport == 4 || (allowExceedsCapabilities && maskedSupport == 3);
    }

    protected static boolean formatHasNoLanguage(Format format) {
        return TextUtils.isEmpty(format.language) || formatHasLanguage(format, C.LANGUAGE_UNDETERMINED);
    }

    protected static boolean formatHasLanguage(Format format, @Nullable String language) {
        return language != null && TextUtils.equals(language, Util.normalizeLanguageCode(format.language));
    }

    private static List<Integer> getViewportFilteredTrackIndices(TrackGroup group, int viewportWidth, int viewportHeight, boolean orientationMayChange) {
        ArrayList<Integer> selectedTrackIndices = new ArrayList<>(group.length);
        for (int i = 0; i < group.length; i++) {
            selectedTrackIndices.add(Integer.valueOf(i));
        }
        if (viewportWidth == Integer.MAX_VALUE || viewportHeight == Integer.MAX_VALUE) {
            return selectedTrackIndices;
        }
        int maxVideoPixelsToRetain = Integer.MAX_VALUE;
        for (int i2 = 0; i2 < group.length; i2++) {
            Format format = group.getFormat(i2);
            if (format.width > 0 && format.height > 0) {
                Point maxVideoSizeInViewport = getMaxVideoSizeInViewport(orientationMayChange, viewportWidth, viewportHeight, format.width, format.height);
                int videoPixels = format.width * format.height;
                if (format.width >= ((int) (((float) maxVideoSizeInViewport.x) * FRACTION_TO_CONSIDER_FULLSCREEN)) && format.height >= ((int) (((float) maxVideoSizeInViewport.y) * FRACTION_TO_CONSIDER_FULLSCREEN)) && videoPixels < maxVideoPixelsToRetain) {
                    maxVideoPixelsToRetain = videoPixels;
                }
            }
        }
        if (maxVideoPixelsToRetain != Integer.MAX_VALUE) {
            for (int i3 = selectedTrackIndices.size() - 1; i3 >= 0; i3--) {
                int pixelCount = group.getFormat(((Integer) selectedTrackIndices.get(i3)).intValue()).getPixelCount();
                if (pixelCount == -1 || pixelCount > maxVideoPixelsToRetain) {
                    selectedTrackIndices.remove(i3);
                }
            }
        }
        return selectedTrackIndices;
    }

    private static Point getMaxVideoSizeInViewport(boolean orientationMayChange, int viewportWidth, int viewportHeight, int videoWidth, int videoHeight) {
        if (orientationMayChange) {
            boolean z = true;
            boolean z2 = videoWidth > videoHeight;
            if (viewportWidth <= viewportHeight) {
                z = false;
            }
            if (z2 != z) {
                int tempViewportWidth = viewportWidth;
                viewportWidth = viewportHeight;
                viewportHeight = tempViewportWidth;
            }
        }
        if (videoWidth * viewportHeight >= videoHeight * viewportWidth) {
            return new Point(viewportWidth, Util.ceilDivide(viewportWidth * videoHeight, videoWidth));
        }
        return new Point(Util.ceilDivide(viewportHeight * videoWidth, videoHeight), viewportHeight);
    }

    /* access modifiers changed from: private */
    public static int compareInts(int first, int second) {
        if (first > second) {
            return 1;
        }
        return second > first ? -1 : 0;
    }
}
