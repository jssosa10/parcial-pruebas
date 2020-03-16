package com.google.android.exoplayer2.source;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlayerMessage.Target;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.Timeline.Period;
import com.google.android.exoplayer2.Timeline.Window;
import com.google.android.exoplayer2.source.MediaSource.MediaPeriodId;
import com.google.android.exoplayer2.source.ShuffleOrder.DefaultShuffleOrder;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class ConcatenatingMediaSource extends CompositeMediaSource<MediaSourceHolder> implements Target {
    private static final int MSG_ADD = 0;
    private static final int MSG_MOVE = 2;
    private static final int MSG_NOTIFY_LISTENER = 4;
    private static final int MSG_ON_COMPLETION = 5;
    private static final int MSG_REMOVE = 1;
    private static final int MSG_SET_SHUFFLE_ORDER = 3;
    private final boolean isAtomic;
    private boolean listenerNotificationScheduled;
    private final Map<MediaPeriod, MediaSourceHolder> mediaSourceByMediaPeriod;
    private final Map<Object, MediaSourceHolder> mediaSourceByUid;
    private final List<MediaSourceHolder> mediaSourceHolders;
    private final List<MediaSourceHolder> mediaSourcesPublic;
    private final List<Runnable> pendingOnCompletionActions;
    private final Period period;
    private int periodCount;
    @Nullable
    private ExoPlayer player;
    @Nullable
    private Handler playerApplicationHandler;
    private ShuffleOrder shuffleOrder;
    private final boolean useLazyPreparation;
    private final Window window;
    private int windowCount;

    private static final class ConcatenatedTimeline extends AbstractConcatenatedTimeline {
        private final HashMap<Object, Integer> childIndexByUid = new HashMap<>();
        private final int[] firstPeriodInChildIndices;
        private final int[] firstWindowInChildIndices;
        private final int periodCount;
        private final Timeline[] timelines;
        private final Object[] uids;
        private final int windowCount;

        public ConcatenatedTimeline(Collection<MediaSourceHolder> mediaSourceHolders, int windowCount2, int periodCount2, ShuffleOrder shuffleOrder, boolean isAtomic) {
            super(isAtomic, shuffleOrder);
            this.windowCount = windowCount2;
            this.periodCount = periodCount2;
            int childCount = mediaSourceHolders.size();
            this.firstPeriodInChildIndices = new int[childCount];
            this.firstWindowInChildIndices = new int[childCount];
            this.timelines = new Timeline[childCount];
            this.uids = new Object[childCount];
            int index = 0;
            for (MediaSourceHolder mediaSourceHolder : mediaSourceHolders) {
                this.timelines[index] = mediaSourceHolder.timeline;
                this.firstPeriodInChildIndices[index] = mediaSourceHolder.firstPeriodIndexInChild;
                this.firstWindowInChildIndices[index] = mediaSourceHolder.firstWindowIndexInChild;
                this.uids[index] = mediaSourceHolder.uid;
                int index2 = index + 1;
                this.childIndexByUid.put(this.uids[index], Integer.valueOf(index));
                index = index2;
            }
        }

        /* access modifiers changed from: protected */
        public int getChildIndexByPeriodIndex(int periodIndex) {
            return Util.binarySearchFloor(this.firstPeriodInChildIndices, periodIndex + 1, false, false);
        }

        /* access modifiers changed from: protected */
        public int getChildIndexByWindowIndex(int windowIndex) {
            return Util.binarySearchFloor(this.firstWindowInChildIndices, windowIndex + 1, false, false);
        }

        /* access modifiers changed from: protected */
        public int getChildIndexByChildUid(Object childUid) {
            Integer index = (Integer) this.childIndexByUid.get(childUid);
            if (index == null) {
                return -1;
            }
            return index.intValue();
        }

        /* access modifiers changed from: protected */
        public Timeline getTimelineByChildIndex(int childIndex) {
            return this.timelines[childIndex];
        }

        /* access modifiers changed from: protected */
        public int getFirstPeriodIndexByChildIndex(int childIndex) {
            return this.firstPeriodInChildIndices[childIndex];
        }

        /* access modifiers changed from: protected */
        public int getFirstWindowIndexByChildIndex(int childIndex) {
            return this.firstWindowInChildIndices[childIndex];
        }

        /* access modifiers changed from: protected */
        public Object getChildUidByChildIndex(int childIndex) {
            return this.uids[childIndex];
        }

        public int getWindowCount() {
            return this.windowCount;
        }

        public int getPeriodCount() {
            return this.periodCount;
        }
    }

    private static final class DeferredTimeline extends ForwardingTimeline {
        /* access modifiers changed from: private */
        public static final Object DUMMY_ID = new Object();
        private static final DummyTimeline DUMMY_TIMELINE = new DummyTimeline();
        /* access modifiers changed from: private */
        public final Object replacedId;

        public static DeferredTimeline createWithRealTimeline(Timeline timeline, Object firstPeriodUid) {
            return new DeferredTimeline(timeline, firstPeriodUid);
        }

        public DeferredTimeline() {
            this(DUMMY_TIMELINE, DUMMY_ID);
        }

        private DeferredTimeline(Timeline timeline, Object replacedId2) {
            super(timeline);
            this.replacedId = replacedId2;
        }

        public DeferredTimeline cloneWithUpdatedTimeline(Timeline timeline) {
            return new DeferredTimeline(timeline, this.replacedId);
        }

        public Timeline getTimeline() {
            return this.timeline;
        }

        public Period getPeriod(int periodIndex, Period period, boolean setIds) {
            this.timeline.getPeriod(periodIndex, period, setIds);
            if (Util.areEqual(period.uid, this.replacedId)) {
                period.uid = DUMMY_ID;
            }
            return period;
        }

        public int getIndexOfPeriod(Object uid) {
            return this.timeline.getIndexOfPeriod(DUMMY_ID.equals(uid) ? this.replacedId : uid);
        }

        public Object getUidOfPeriod(int periodIndex) {
            Object uid = this.timeline.getUidOfPeriod(periodIndex);
            return Util.areEqual(uid, this.replacedId) ? DUMMY_ID : uid;
        }
    }

    private static final class DummyMediaSource extends BaseMediaSource {
        private DummyMediaSource() {
        }

        /* access modifiers changed from: protected */
        public void prepareSourceInternal(ExoPlayer player, boolean isTopLevelSource, @Nullable TransferListener mediaTransferListener) {
        }

        /* access modifiers changed from: protected */
        public void releaseSourceInternal() {
        }

        public void maybeThrowSourceInfoRefreshError() throws IOException {
        }

        public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator) {
            throw new UnsupportedOperationException();
        }

        public void releasePeriod(MediaPeriod mediaPeriod) {
        }
    }

    private static final class DummyTimeline extends Timeline {
        private DummyTimeline() {
        }

        public int getWindowCount() {
            return 1;
        }

        public Window getWindow(int windowIndex, Window window, boolean setTag, long defaultPositionProjectionUs) {
            return window.set(null, C.TIME_UNSET, C.TIME_UNSET, false, true, 0, C.TIME_UNSET, 0, 0, 0);
        }

        public int getPeriodCount() {
            return 1;
        }

        public Period getPeriod(int periodIndex, Period period, boolean setIds) {
            return period.set(Integer.valueOf(0), DeferredTimeline.DUMMY_ID, 0, C.TIME_UNSET, 0);
        }

        public int getIndexOfPeriod(Object uid) {
            return uid == DeferredTimeline.DUMMY_ID ? 0 : -1;
        }

        public Object getUidOfPeriod(int periodIndex) {
            return DeferredTimeline.DUMMY_ID;
        }
    }

    static final class MediaSourceHolder implements Comparable<MediaSourceHolder> {
        public List<DeferredMediaPeriod> activeMediaPeriods = new ArrayList();
        public int childIndex;
        public int firstPeriodIndexInChild;
        public int firstWindowIndexInChild;
        public boolean hasStartedPreparing;
        public boolean isPrepared;
        public boolean isRemoved;
        public final MediaSource mediaSource;
        public DeferredTimeline timeline = new DeferredTimeline();
        public final Object uid = new Object();

        public MediaSourceHolder(MediaSource mediaSource2) {
            this.mediaSource = mediaSource2;
        }

        public void reset(int childIndex2, int firstWindowIndexInChild2, int firstPeriodIndexInChild2) {
            this.childIndex = childIndex2;
            this.firstWindowIndexInChild = firstWindowIndexInChild2;
            this.firstPeriodIndexInChild = firstPeriodIndexInChild2;
            this.hasStartedPreparing = false;
            this.isPrepared = false;
            this.isRemoved = false;
            this.activeMediaPeriods.clear();
        }

        public int compareTo(@NonNull MediaSourceHolder other) {
            return this.firstPeriodIndexInChild - other.firstPeriodIndexInChild;
        }
    }

    private static final class MessageData<T> {
        @Nullable
        public final Runnable actionOnCompletion;
        public final T customData;
        public final int index;

        public MessageData(int index2, T customData2, @Nullable Runnable actionOnCompletion2) {
            this.index = index2;
            this.actionOnCompletion = actionOnCompletion2;
            this.customData = customData2;
        }
    }

    public ConcatenatingMediaSource(MediaSource... mediaSources) {
        this(false, mediaSources);
    }

    public ConcatenatingMediaSource(boolean isAtomic2, MediaSource... mediaSources) {
        this(isAtomic2, new DefaultShuffleOrder(0), mediaSources);
    }

    public ConcatenatingMediaSource(boolean isAtomic2, ShuffleOrder shuffleOrder2, MediaSource... mediaSources) {
        this(isAtomic2, false, shuffleOrder2, mediaSources);
    }

    public ConcatenatingMediaSource(boolean isAtomic2, boolean useLazyPreparation2, ShuffleOrder shuffleOrder2, MediaSource... mediaSources) {
        for (MediaSource mediaSource : mediaSources) {
            Assertions.checkNotNull(mediaSource);
        }
        this.shuffleOrder = shuffleOrder2.getLength() > 0 ? shuffleOrder2.cloneAndClear() : shuffleOrder2;
        this.mediaSourceByMediaPeriod = new IdentityHashMap();
        this.mediaSourceByUid = new HashMap();
        this.mediaSourcesPublic = new ArrayList();
        this.mediaSourceHolders = new ArrayList();
        this.pendingOnCompletionActions = new ArrayList();
        this.isAtomic = isAtomic2;
        this.useLazyPreparation = useLazyPreparation2;
        this.window = new Window();
        this.period = new Period();
        addMediaSources(Arrays.asList(mediaSources));
    }

    public final synchronized void addMediaSource(MediaSource mediaSource) {
        addMediaSource(this.mediaSourcesPublic.size(), mediaSource, null);
    }

    public final synchronized void addMediaSource(MediaSource mediaSource, @Nullable Runnable actionOnCompletion) {
        addMediaSource(this.mediaSourcesPublic.size(), mediaSource, actionOnCompletion);
    }

    public final synchronized void addMediaSource(int index, MediaSource mediaSource) {
        addMediaSource(index, mediaSource, null);
    }

    public final synchronized void addMediaSource(int index, MediaSource mediaSource, @Nullable Runnable actionOnCompletion) {
        addMediaSources(index, Collections.singletonList(mediaSource), actionOnCompletion);
    }

    public final synchronized void addMediaSources(Collection<MediaSource> mediaSources) {
        addMediaSources(this.mediaSourcesPublic.size(), mediaSources, null);
    }

    public final synchronized void addMediaSources(Collection<MediaSource> mediaSources, @Nullable Runnable actionOnCompletion) {
        addMediaSources(this.mediaSourcesPublic.size(), mediaSources, actionOnCompletion);
    }

    public final synchronized void addMediaSources(int index, Collection<MediaSource> mediaSources) {
        addMediaSources(index, mediaSources, null);
    }

    public final synchronized void addMediaSources(int index, Collection<MediaSource> mediaSources, @Nullable Runnable actionOnCompletion) {
        for (MediaSource mediaSource : mediaSources) {
            Assertions.checkNotNull(mediaSource);
        }
        List<MediaSourceHolder> mediaSourceHolders2 = new ArrayList<>(mediaSources.size());
        for (MediaSource mediaSource2 : mediaSources) {
            mediaSourceHolders2.add(new MediaSourceHolder(mediaSource2));
        }
        this.mediaSourcesPublic.addAll(index, mediaSourceHolders2);
        if (this.player != null && !mediaSources.isEmpty()) {
            this.player.createMessage(this).setType(0).setPayload(new MessageData(index, mediaSourceHolders2, actionOnCompletion)).send();
        } else if (actionOnCompletion != null) {
            actionOnCompletion.run();
        }
    }

    public final synchronized void removeMediaSource(int index) {
        removeMediaSource(index, null);
    }

    public final synchronized void removeMediaSource(int index, @Nullable Runnable actionOnCompletion) {
        removeMediaSourceRange(index, index + 1, actionOnCompletion);
    }

    public final synchronized void removeMediaSourceRange(int fromIndex, int toIndex) {
        removeMediaSourceRange(fromIndex, toIndex, null);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:15:0x0035, code lost:
        return;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:7:0x000e, code lost:
        return;
     */
    public final synchronized void removeMediaSourceRange(int fromIndex, int toIndex, @Nullable Runnable actionOnCompletion) {
        Util.removeRange(this.mediaSourcesPublic, fromIndex, toIndex);
        if (fromIndex == toIndex) {
            if (actionOnCompletion != null) {
                actionOnCompletion.run();
            }
        } else if (this.player != null) {
            this.player.createMessage(this).setType(1).setPayload(new MessageData(fromIndex, Integer.valueOf(toIndex), actionOnCompletion)).send();
        } else if (actionOnCompletion != null) {
            actionOnCompletion.run();
        }
    }

    public final synchronized void moveMediaSource(int currentIndex, int newIndex) {
        moveMediaSource(currentIndex, newIndex, null);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0036, code lost:
        return;
     */
    public final synchronized void moveMediaSource(int currentIndex, int newIndex, @Nullable Runnable actionOnCompletion) {
        if (currentIndex != newIndex) {
            this.mediaSourcesPublic.add(newIndex, this.mediaSourcesPublic.remove(currentIndex));
            if (this.player != null) {
                this.player.createMessage(this).setType(2).setPayload(new MessageData(currentIndex, Integer.valueOf(newIndex), actionOnCompletion)).send();
            } else if (actionOnCompletion != null) {
                actionOnCompletion.run();
            }
        }
    }

    public final synchronized void clear() {
        clear(null);
    }

    public final synchronized void clear(@Nullable Runnable actionOnCompletion) {
        removeMediaSourceRange(0, getSize(), actionOnCompletion);
    }

    public final synchronized int getSize() {
        return this.mediaSourcesPublic.size();
    }

    public final synchronized MediaSource getMediaSource(int index) {
        return ((MediaSourceHolder) this.mediaSourcesPublic.get(index)).mediaSource;
    }

    public final synchronized void setShuffleOrder(ShuffleOrder shuffleOrder2) {
        setShuffleOrder(shuffleOrder2, null);
    }

    public final synchronized void setShuffleOrder(ShuffleOrder shuffleOrder2, @Nullable Runnable actionOnCompletion) {
        ExoPlayer player2 = this.player;
        if (player2 != null) {
            int size = getSize();
            if (shuffleOrder2.getLength() != size) {
                shuffleOrder2 = shuffleOrder2.cloneAndClear().cloneAndInsert(0, size);
            }
            player2.createMessage(this).setType(3).setPayload(new MessageData(0, shuffleOrder2, actionOnCompletion)).send();
        } else {
            this.shuffleOrder = shuffleOrder2.getLength() > 0 ? shuffleOrder2.cloneAndClear() : shuffleOrder2;
            if (actionOnCompletion != null) {
                actionOnCompletion.run();
            }
        }
    }

    public final synchronized void prepareSourceInternal(ExoPlayer player2, boolean isTopLevelSource, @Nullable TransferListener mediaTransferListener) {
        super.prepareSourceInternal(player2, isTopLevelSource, mediaTransferListener);
        this.player = player2;
        this.playerApplicationHandler = new Handler(player2.getApplicationLooper());
        if (this.mediaSourcesPublic.isEmpty()) {
            notifyListener();
        } else {
            this.shuffleOrder = this.shuffleOrder.cloneAndInsert(0, this.mediaSourcesPublic.size());
            addMediaSourcesInternal(0, this.mediaSourcesPublic);
            scheduleListenerNotification(null);
        }
    }

    public void maybeThrowSourceInfoRefreshError() throws IOException {
    }

    public final MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator) {
        MediaSourceHolder holder = (MediaSourceHolder) this.mediaSourceByUid.get(getMediaSourceHolderUid(id.periodUid));
        if (holder == null) {
            holder = new MediaSourceHolder(new DummyMediaSource());
            holder.hasStartedPreparing = true;
        }
        DeferredMediaPeriod mediaPeriod = new DeferredMediaPeriod(holder.mediaSource, id, allocator);
        this.mediaSourceByMediaPeriod.put(mediaPeriod, holder);
        holder.activeMediaPeriods.add(mediaPeriod);
        if (!holder.hasStartedPreparing) {
            holder.hasStartedPreparing = true;
            prepareChildSource(holder, holder.mediaSource);
        } else if (holder.isPrepared) {
            mediaPeriod.createPeriod(id.copyWithPeriodUid(getChildPeriodUid(holder, id.periodUid)));
        }
        return mediaPeriod;
    }

    public final void releasePeriod(MediaPeriod mediaPeriod) {
        MediaSourceHolder holder = (MediaSourceHolder) Assertions.checkNotNull(this.mediaSourceByMediaPeriod.remove(mediaPeriod));
        ((DeferredMediaPeriod) mediaPeriod).releasePeriod();
        holder.activeMediaPeriods.remove(mediaPeriod);
        maybeReleaseChildSource(holder);
    }

    public final void releaseSourceInternal() {
        super.releaseSourceInternal();
        this.mediaSourceHolders.clear();
        this.mediaSourceByUid.clear();
        this.player = null;
        this.playerApplicationHandler = null;
        this.shuffleOrder = this.shuffleOrder.cloneAndClear();
        this.windowCount = 0;
        this.periodCount = 0;
    }

    /* access modifiers changed from: protected */
    public final void onChildSourceInfoRefreshed(MediaSourceHolder mediaSourceHolder, MediaSource mediaSource, Timeline timeline, @Nullable Object manifest) {
        updateMediaSourceInternal(mediaSourceHolder, timeline);
    }

    /* access modifiers changed from: protected */
    @Nullable
    public MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(MediaSourceHolder mediaSourceHolder, MediaPeriodId mediaPeriodId) {
        for (int i = 0; i < mediaSourceHolder.activeMediaPeriods.size(); i++) {
            if (((DeferredMediaPeriod) mediaSourceHolder.activeMediaPeriods.get(i)).id.windowSequenceNumber == mediaPeriodId.windowSequenceNumber) {
                return mediaPeriodId.copyWithPeriodUid(getPeriodUid(mediaSourceHolder, mediaPeriodId.periodUid));
            }
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public int getWindowIndexForChildWindowIndex(MediaSourceHolder mediaSourceHolder, int windowIndex) {
        return mediaSourceHolder.firstWindowIndexInChild + windowIndex;
    }

    public final void handleMessage(int messageType, @Nullable Object message) throws ExoPlaybackException {
        if (this.player != null) {
            switch (messageType) {
                case 0:
                    MessageData<Collection<MediaSourceHolder>> addMessage = (MessageData) Util.castNonNull(message);
                    this.shuffleOrder = this.shuffleOrder.cloneAndInsert(addMessage.index, ((Collection) addMessage.customData).size());
                    addMediaSourcesInternal(addMessage.index, (Collection) addMessage.customData);
                    scheduleListenerNotification(addMessage.actionOnCompletion);
                    break;
                case 1:
                    MessageData<Integer> removeMessage = (MessageData) Util.castNonNull(message);
                    int fromIndex = removeMessage.index;
                    int toIndex = ((Integer) removeMessage.customData).intValue();
                    if (fromIndex == 0 && toIndex == this.shuffleOrder.getLength()) {
                        this.shuffleOrder = this.shuffleOrder.cloneAndClear();
                    } else {
                        for (int index = toIndex - 1; index >= fromIndex; index--) {
                            this.shuffleOrder = this.shuffleOrder.cloneAndRemove(index);
                        }
                    }
                    for (int index2 = toIndex - 1; index2 >= fromIndex; index2--) {
                        removeMediaSourceInternal(index2);
                    }
                    scheduleListenerNotification(removeMessage.actionOnCompletion);
                    break;
                case 2:
                    MessageData<Integer> moveMessage = (MessageData) Util.castNonNull(message);
                    this.shuffleOrder = this.shuffleOrder.cloneAndRemove(moveMessage.index);
                    this.shuffleOrder = this.shuffleOrder.cloneAndInsert(((Integer) moveMessage.customData).intValue(), 1);
                    moveMediaSourceInternal(moveMessage.index, ((Integer) moveMessage.customData).intValue());
                    scheduleListenerNotification(moveMessage.actionOnCompletion);
                    break;
                case 3:
                    MessageData<ShuffleOrder> shuffleOrderMessage = (MessageData) Util.castNonNull(message);
                    this.shuffleOrder = (ShuffleOrder) shuffleOrderMessage.customData;
                    scheduleListenerNotification(shuffleOrderMessage.actionOnCompletion);
                    break;
                case 4:
                    notifyListener();
                    break;
                case 5:
                    List<Runnable> actionsOnCompletion = (List) Util.castNonNull(message);
                    Handler handler = (Handler) Assertions.checkNotNull(this.playerApplicationHandler);
                    for (int i = 0; i < actionsOnCompletion.size(); i++) {
                        handler.post((Runnable) actionsOnCompletion.get(i));
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    private void scheduleListenerNotification(@Nullable Runnable actionOnCompletion) {
        if (!this.listenerNotificationScheduled) {
            ((ExoPlayer) Assertions.checkNotNull(this.player)).createMessage(this).setType(4).send();
            this.listenerNotificationScheduled = true;
        }
        if (actionOnCompletion != null) {
            this.pendingOnCompletionActions.add(actionOnCompletion);
        }
    }

    private void notifyListener() {
        this.listenerNotificationScheduled = false;
        List<Runnable> actionsOnCompletion = this.pendingOnCompletionActions.isEmpty() ? Collections.emptyList() : new ArrayList<>(this.pendingOnCompletionActions);
        this.pendingOnCompletionActions.clear();
        ConcatenatedTimeline concatenatedTimeline = new ConcatenatedTimeline(this.mediaSourceHolders, this.windowCount, this.periodCount, this.shuffleOrder, this.isAtomic);
        refreshSourceInfo(concatenatedTimeline, null);
        if (!actionsOnCompletion.isEmpty()) {
            ((ExoPlayer) Assertions.checkNotNull(this.player)).createMessage(this).setType(5).setPayload(actionsOnCompletion).send();
        }
    }

    private void addMediaSourcesInternal(int index, Collection<MediaSourceHolder> mediaSourceHolders2) {
        for (MediaSourceHolder mediaSourceHolder : mediaSourceHolders2) {
            int index2 = index + 1;
            addMediaSourceInternal(index, mediaSourceHolder);
            index = index2;
        }
    }

    private void addMediaSourceInternal(int newIndex, MediaSourceHolder newMediaSourceHolder) {
        if (newIndex > 0) {
            MediaSourceHolder previousHolder = (MediaSourceHolder) this.mediaSourceHolders.get(newIndex - 1);
            newMediaSourceHolder.reset(newIndex, previousHolder.firstWindowIndexInChild + previousHolder.timeline.getWindowCount(), previousHolder.firstPeriodIndexInChild + previousHolder.timeline.getPeriodCount());
        } else {
            newMediaSourceHolder.reset(newIndex, 0, 0);
        }
        correctOffsets(newIndex, 1, newMediaSourceHolder.timeline.getWindowCount(), newMediaSourceHolder.timeline.getPeriodCount());
        this.mediaSourceHolders.add(newIndex, newMediaSourceHolder);
        this.mediaSourceByUid.put(newMediaSourceHolder.uid, newMediaSourceHolder);
        if (!this.useLazyPreparation) {
            newMediaSourceHolder.hasStartedPreparing = true;
            prepareChildSource(newMediaSourceHolder, newMediaSourceHolder.mediaSource);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x00a6  */
    private void updateMediaSourceInternal(MediaSourceHolder mediaSourceHolder, Timeline timeline) {
        DeferredMediaPeriod deferredMediaPeriod;
        long windowStartPositionUs;
        MediaSourceHolder mediaSourceHolder2 = mediaSourceHolder;
        Timeline timeline2 = timeline;
        if (mediaSourceHolder2 != null) {
            DeferredTimeline deferredTimeline = mediaSourceHolder2.timeline;
            if (deferredTimeline.getTimeline() != timeline2) {
                int windowOffsetUpdate = timeline.getWindowCount() - deferredTimeline.getWindowCount();
                int periodOffsetUpdate = timeline.getPeriodCount() - deferredTimeline.getPeriodCount();
                if (!(windowOffsetUpdate == 0 && periodOffsetUpdate == 0)) {
                    correctOffsets(mediaSourceHolder2.childIndex + 1, 0, windowOffsetUpdate, periodOffsetUpdate);
                }
                if (mediaSourceHolder2.isPrepared) {
                    mediaSourceHolder2.timeline = deferredTimeline.cloneWithUpdatedTimeline(timeline2);
                } else if (timeline.isEmpty()) {
                    mediaSourceHolder2.timeline = DeferredTimeline.createWithRealTimeline(timeline2, DeferredTimeline.DUMMY_ID);
                } else {
                    Assertions.checkState(mediaSourceHolder2.activeMediaPeriods.size() <= 1);
                    if (mediaSourceHolder2.activeMediaPeriods.isEmpty()) {
                        deferredMediaPeriod = null;
                    } else {
                        deferredMediaPeriod = (DeferredMediaPeriod) mediaSourceHolder2.activeMediaPeriods.get(0);
                    }
                    DeferredMediaPeriod deferredMediaPeriod2 = deferredMediaPeriod;
                    long windowStartPositionUs2 = this.window.getDefaultPositionUs();
                    if (deferredMediaPeriod2 != null) {
                        long periodPreparePositionUs = deferredMediaPeriod2.getPreparePositionUs();
                        if (periodPreparePositionUs != 0) {
                            windowStartPositionUs = periodPreparePositionUs;
                            Pair<Object, Long> periodPosition = timeline.getPeriodPosition(this.window, this.period, 0, windowStartPositionUs);
                            Object periodUid = periodPosition.first;
                            long periodPositionUs = ((Long) periodPosition.second).longValue();
                            mediaSourceHolder2.timeline = DeferredTimeline.createWithRealTimeline(timeline2, periodUid);
                            if (deferredMediaPeriod2 != null) {
                                deferredMediaPeriod2.overridePreparePositionUs(periodPositionUs);
                                deferredMediaPeriod2.createPeriod(deferredMediaPeriod2.id.copyWithPeriodUid(getChildPeriodUid(mediaSourceHolder2, deferredMediaPeriod2.id.periodUid)));
                            }
                        }
                    }
                    windowStartPositionUs = windowStartPositionUs2;
                    Pair<Object, Long> periodPosition2 = timeline.getPeriodPosition(this.window, this.period, 0, windowStartPositionUs);
                    Object periodUid2 = periodPosition2.first;
                    long periodPositionUs2 = ((Long) periodPosition2.second).longValue();
                    mediaSourceHolder2.timeline = DeferredTimeline.createWithRealTimeline(timeline2, periodUid2);
                    if (deferredMediaPeriod2 != null) {
                    }
                }
                mediaSourceHolder2.isPrepared = true;
                scheduleListenerNotification(null);
                return;
            }
            return;
        }
        throw new IllegalArgumentException();
    }

    private void removeMediaSourceInternal(int index) {
        MediaSourceHolder holder = (MediaSourceHolder) this.mediaSourceHolders.remove(index);
        this.mediaSourceByUid.remove(holder.uid);
        Timeline oldTimeline = holder.timeline;
        correctOffsets(index, -1, -oldTimeline.getWindowCount(), -oldTimeline.getPeriodCount());
        holder.isRemoved = true;
        maybeReleaseChildSource(holder);
    }

    private void moveMediaSourceInternal(int currentIndex, int newIndex) {
        int startIndex = Math.min(currentIndex, newIndex);
        int endIndex = Math.max(currentIndex, newIndex);
        int windowOffset = ((MediaSourceHolder) this.mediaSourceHolders.get(startIndex)).firstWindowIndexInChild;
        int periodOffset = ((MediaSourceHolder) this.mediaSourceHolders.get(startIndex)).firstPeriodIndexInChild;
        List<MediaSourceHolder> list = this.mediaSourceHolders;
        list.add(newIndex, list.remove(currentIndex));
        for (int i = startIndex; i <= endIndex; i++) {
            MediaSourceHolder holder = (MediaSourceHolder) this.mediaSourceHolders.get(i);
            holder.firstWindowIndexInChild = windowOffset;
            holder.firstPeriodIndexInChild = periodOffset;
            windowOffset += holder.timeline.getWindowCount();
            periodOffset += holder.timeline.getPeriodCount();
        }
    }

    private void correctOffsets(int startIndex, int childIndexUpdate, int windowOffsetUpdate, int periodOffsetUpdate) {
        this.windowCount += windowOffsetUpdate;
        this.periodCount += periodOffsetUpdate;
        for (int i = startIndex; i < this.mediaSourceHolders.size(); i++) {
            MediaSourceHolder mediaSourceHolder = (MediaSourceHolder) this.mediaSourceHolders.get(i);
            mediaSourceHolder.childIndex += childIndexUpdate;
            MediaSourceHolder mediaSourceHolder2 = (MediaSourceHolder) this.mediaSourceHolders.get(i);
            mediaSourceHolder2.firstWindowIndexInChild += windowOffsetUpdate;
            MediaSourceHolder mediaSourceHolder3 = (MediaSourceHolder) this.mediaSourceHolders.get(i);
            mediaSourceHolder3.firstPeriodIndexInChild += periodOffsetUpdate;
        }
    }

    private void maybeReleaseChildSource(MediaSourceHolder mediaSourceHolder) {
        if (mediaSourceHolder.isRemoved && mediaSourceHolder.hasStartedPreparing && mediaSourceHolder.activeMediaPeriods.isEmpty()) {
            releaseChildSource(mediaSourceHolder);
        }
    }

    private static Object getMediaSourceHolderUid(Object periodUid) {
        return ConcatenatedTimeline.getChildTimelineUidFromConcatenatedUid(periodUid);
    }

    private static Object getChildPeriodUid(MediaSourceHolder holder, Object periodUid) {
        Object childUid = ConcatenatedTimeline.getChildPeriodUidFromConcatenatedUid(periodUid);
        return childUid.equals(DeferredTimeline.DUMMY_ID) ? holder.timeline.replacedId : childUid;
    }

    private static Object getPeriodUid(MediaSourceHolder holder, Object childPeriodUid) {
        if (holder.timeline.replacedId.equals(childPeriodUid)) {
            childPeriodUid = DeferredTimeline.DUMMY_ID;
        }
        return ConcatenatedTimeline.getConcatenatedUid(holder.uid, childPeriodUid);
    }
}
