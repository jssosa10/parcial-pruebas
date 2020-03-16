package com.google.android.exoplayer2.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.support.annotation.AttrRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.Parameters;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.ParametersBuilder;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.util.Assertions;
import java.util.Arrays;

public class TrackSelectionView extends LinearLayout {
    private boolean allowAdaptiveSelections;
    private final ComponentListener componentListener;
    private final CheckedTextView defaultView;
    private final CheckedTextView disableView;
    private final LayoutInflater inflater;
    private boolean isDisabled;
    @Nullable
    private SelectionOverride override;
    private int rendererIndex;
    private final int selectableItemBackgroundResourceId;
    private TrackGroupArray trackGroups;
    private TrackNameProvider trackNameProvider;
    private DefaultTrackSelector trackSelector;
    private CheckedTextView[][] trackViews;

    private class ComponentListener implements OnClickListener {
        private ComponentListener() {
        }

        public void onClick(View view) {
            TrackSelectionView.this.onClick(view);
        }
    }

    public static Pair<AlertDialog, TrackSelectionView> getDialog(Activity activity, CharSequence title, DefaultTrackSelector trackSelector2, int rendererIndex2) {
        Builder builder = new Builder(activity);
        View dialogView = LayoutInflater.from(builder.getContext()).inflate(R.layout.exo_track_selection_dialog, null);
        TrackSelectionView selectionView = (TrackSelectionView) dialogView.findViewById(R.id.exo_track_selection_view);
        selectionView.init(trackSelector2, rendererIndex2);
        return Pair.create(builder.setTitle(title).setView(dialogView).setPositiveButton(17039370, new DialogInterface.OnClickListener() {
            public final void onClick(DialogInterface dialogInterface, int i) {
                TrackSelectionView.this.applySelection();
            }
        }).setNegativeButton(17039360, null).create(), selectionView);
    }

    public TrackSelectionView(Context context) {
        this(context, null);
    }

    public TrackSelectionView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TrackSelectionView(Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray attributeArray = context.getTheme().obtainStyledAttributes(new int[]{16843534});
        this.selectableItemBackgroundResourceId = attributeArray.getResourceId(0, 0);
        attributeArray.recycle();
        this.inflater = LayoutInflater.from(context);
        this.componentListener = new ComponentListener();
        this.trackNameProvider = new DefaultTrackNameProvider(getResources());
        this.disableView = (CheckedTextView) this.inflater.inflate(17367055, this, false);
        this.disableView.setBackgroundResource(this.selectableItemBackgroundResourceId);
        this.disableView.setText(R.string.exo_track_selection_none);
        this.disableView.setEnabled(false);
        this.disableView.setFocusable(true);
        this.disableView.setOnClickListener(this.componentListener);
        this.disableView.setVisibility(8);
        addView(this.disableView);
        addView(this.inflater.inflate(R.layout.exo_list_divider, this, false));
        this.defaultView = (CheckedTextView) this.inflater.inflate(17367055, this, false);
        this.defaultView.setBackgroundResource(this.selectableItemBackgroundResourceId);
        this.defaultView.setText(R.string.exo_track_selection_auto);
        this.defaultView.setEnabled(false);
        this.defaultView.setFocusable(true);
        this.defaultView.setOnClickListener(this.componentListener);
        addView(this.defaultView);
    }

    public void setAllowAdaptiveSelections(boolean allowAdaptiveSelections2) {
        if (this.allowAdaptiveSelections != allowAdaptiveSelections2) {
            this.allowAdaptiveSelections = allowAdaptiveSelections2;
            updateViews();
        }
    }

    public void setShowDisableOption(boolean showDisableOption) {
        this.disableView.setVisibility(showDisableOption ? 0 : 8);
    }

    public void setTrackNameProvider(TrackNameProvider trackNameProvider2) {
        this.trackNameProvider = (TrackNameProvider) Assertions.checkNotNull(trackNameProvider2);
        updateViews();
    }

    public void init(DefaultTrackSelector trackSelector2, int rendererIndex2) {
        this.trackSelector = trackSelector2;
        this.rendererIndex = rendererIndex2;
        updateViews();
    }

    private void updateViews() {
        MappedTrackInfo trackInfo;
        for (int i = getChildCount() - 1; i >= 3; i--) {
            removeViewAt(i);
        }
        DefaultTrackSelector defaultTrackSelector = this.trackSelector;
        if (defaultTrackSelector == null) {
            trackInfo = null;
        } else {
            trackInfo = defaultTrackSelector.getCurrentMappedTrackInfo();
        }
        if (this.trackSelector == null || trackInfo == null) {
            this.disableView.setEnabled(false);
            this.defaultView.setEnabled(false);
            return;
        }
        this.disableView.setEnabled(true);
        this.defaultView.setEnabled(true);
        this.trackGroups = trackInfo.getTrackGroups(this.rendererIndex);
        Parameters parameters = this.trackSelector.getParameters();
        this.isDisabled = parameters.getRendererDisabled(this.rendererIndex);
        this.override = parameters.getSelectionOverride(this.rendererIndex, this.trackGroups);
        this.trackViews = new CheckedTextView[this.trackGroups.length][];
        for (int groupIndex = 0; groupIndex < this.trackGroups.length; groupIndex++) {
            TrackGroup group = this.trackGroups.get(groupIndex);
            boolean enableAdaptiveSelections = this.allowAdaptiveSelections && this.trackGroups.get(groupIndex).length > 1 && trackInfo.getAdaptiveSupport(this.rendererIndex, groupIndex, false) != 0;
            this.trackViews[groupIndex] = new CheckedTextView[group.length];
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                if (trackIndex == 0) {
                    addView(this.inflater.inflate(R.layout.exo_list_divider, this, false));
                }
                CheckedTextView trackView = (CheckedTextView) this.inflater.inflate(enableAdaptiveSelections ? 17367056 : 17367055, this, false);
                trackView.setBackgroundResource(this.selectableItemBackgroundResourceId);
                trackView.setText(this.trackNameProvider.getTrackName(group.getFormat(trackIndex)));
                if (trackInfo.getTrackSupport(this.rendererIndex, groupIndex, trackIndex) == 4) {
                    trackView.setFocusable(true);
                    trackView.setTag(Pair.create(Integer.valueOf(groupIndex), Integer.valueOf(trackIndex)));
                    trackView.setOnClickListener(this.componentListener);
                } else {
                    trackView.setFocusable(false);
                    trackView.setEnabled(false);
                }
                this.trackViews[groupIndex][trackIndex] = trackView;
                addView(trackView);
            }
        }
        updateViewStates();
    }

    private void updateViewStates() {
        this.disableView.setChecked(this.isDisabled);
        this.defaultView.setChecked(!this.isDisabled && this.override == null);
        int i = 0;
        while (i < this.trackViews.length) {
            int j = 0;
            while (true) {
                CheckedTextView[][] checkedTextViewArr = this.trackViews;
                if (j >= checkedTextViewArr[i].length) {
                    break;
                }
                CheckedTextView checkedTextView = checkedTextViewArr[i][j];
                SelectionOverride selectionOverride = this.override;
                checkedTextView.setChecked(selectionOverride != null && selectionOverride.groupIndex == i && this.override.containsTrack(j));
                j++;
            }
            i++;
        }
    }

    /* access modifiers changed from: private */
    public void applySelection() {
        ParametersBuilder parametersBuilder = this.trackSelector.buildUponParameters();
        parametersBuilder.setRendererDisabled(this.rendererIndex, this.isDisabled);
        SelectionOverride selectionOverride = this.override;
        if (selectionOverride != null) {
            parametersBuilder.setSelectionOverride(this.rendererIndex, this.trackGroups, selectionOverride);
        } else {
            parametersBuilder.clearSelectionOverrides(this.rendererIndex);
        }
        this.trackSelector.setParameters(parametersBuilder);
    }

    /* access modifiers changed from: private */
    public void onClick(View view) {
        if (view == this.disableView) {
            onDisableViewClicked();
        } else if (view == this.defaultView) {
            onDefaultViewClicked();
        } else {
            onTrackViewClicked(view);
        }
        updateViewStates();
    }

    private void onDisableViewClicked() {
        this.isDisabled = true;
        this.override = null;
    }

    private void onDefaultViewClicked() {
        this.isDisabled = false;
        this.override = null;
    }

    private void onTrackViewClicked(View view) {
        this.isDisabled = false;
        Pair<Integer, Integer> tag = (Pair) view.getTag();
        int groupIndex = ((Integer) tag.first).intValue();
        int trackIndex = ((Integer) tag.second).intValue();
        SelectionOverride selectionOverride = this.override;
        if (selectionOverride == null || selectionOverride.groupIndex != groupIndex || !this.allowAdaptiveSelections) {
            this.override = new SelectionOverride(groupIndex, trackIndex);
            return;
        }
        int overrideLength = this.override.length;
        int[] overrideTracks = this.override.tracks;
        if (!((CheckedTextView) view).isChecked()) {
            this.override = new SelectionOverride(groupIndex, getTracksAdding(overrideTracks, trackIndex));
        } else if (overrideLength == 1) {
            this.override = null;
            this.isDisabled = true;
        } else {
            this.override = new SelectionOverride(groupIndex, getTracksRemoving(overrideTracks, trackIndex));
        }
    }

    private static int[] getTracksAdding(int[] tracks, int addedTrack) {
        int[] tracks2 = Arrays.copyOf(tracks, tracks.length + 1);
        tracks2[tracks2.length - 1] = addedTrack;
        return tracks2;
    }

    private static int[] getTracksRemoving(int[] tracks, int removedTrack) {
        int[] newTracks = new int[(tracks.length - 1)];
        int trackCount = 0;
        for (int track : tracks) {
            if (track != removedTrack) {
                int trackCount2 = trackCount + 1;
                newTracks[trackCount] = track;
                trackCount = trackCount2;
            }
        }
        return newTracks;
    }
}
