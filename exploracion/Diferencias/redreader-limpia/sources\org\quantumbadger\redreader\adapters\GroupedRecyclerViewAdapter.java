package org.quantumbadger.redreader.adapters;

import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.LayoutParams;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

public class GroupedRecyclerViewAdapter extends Adapter {
    /* access modifiers changed from: private */
    public static final AtomicLong ITEM_UNIQUE_ID_GENERATOR = new AtomicLong(100000);
    private final HashMap<Class, Integer> mItemViewTypeMap = new HashMap<>();
    private final ArrayList<Item>[] mItems;
    private final HashMap<Integer, Item> mViewTypeItemMap = new HashMap<>();

    public static abstract class Item {
        /* access modifiers changed from: private */
        public boolean mCurrentlyHidden = false;
        /* access modifiers changed from: private */
        public final long mUniqueId = GroupedRecyclerViewAdapter.ITEM_UNIQUE_ID_GENERATOR.incrementAndGet();

        public abstract Class getViewType();

        public abstract boolean isHidden();

        public abstract void onBindViewHolder(ViewHolder viewHolder);

        public abstract ViewHolder onCreateViewHolder(ViewGroup viewGroup);
    }

    public GroupedRecyclerViewAdapter(int groups) {
        this.mItems = (ArrayList[]) new ArrayList[groups];
        for (int i = 0; i < groups; i++) {
            this.mItems[i] = new ArrayList<>();
        }
        setHasStableIds(true);
    }

    private int getItemPositionInternal(int groupId, Item item) {
        ArrayList<Item> group = this.mItems[groupId];
        for (int i = 0; i < group.size(); i++) {
            if (group.get(i) == item) {
                return getItemPositionInternal(groupId, i);
            }
        }
        throw new RuntimeException("Item not found");
    }

    private int getItemPositionInternal(int group, int positionInGroup) {
        int result = 0;
        for (int i = 0; i < group; i++) {
            result += getGroupUnhiddenCount(i);
        }
        for (int i2 = 0; i2 < positionInGroup; i2++) {
            if (!((Item) this.mItems[group].get(i2)).mCurrentlyHidden) {
                result++;
            }
        }
        return result;
    }

    private Item getItemInternal(int desiredPosition) {
        if (desiredPosition >= 0) {
            int currentPosition = 0;
            int groupId = 0;
            while (true) {
                ArrayList<Item>[] arrayListArr = this.mItems;
                if (groupId < arrayListArr.length) {
                    ArrayList<Item> group = arrayListArr[groupId];
                    for (int positionInGroup = 0; positionInGroup < group.size(); positionInGroup++) {
                        Item item = (Item) group.get(positionInGroup);
                        if (!item.mCurrentlyHidden) {
                            if (currentPosition == desiredPosition) {
                                return item;
                            }
                            currentPosition++;
                        }
                    }
                    groupId++;
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Item desiredPosition ");
                    sb.append(desiredPosition);
                    sb.append(" is too high");
                    throw new RuntimeException(sb.toString());
                }
            }
        } else {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("Item desiredPosition ");
            sb2.append(desiredPosition);
            sb2.append(" is too low");
            throw new RuntimeException(sb2.toString());
        }
    }

    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        ViewHolder viewHolder = ((Item) this.mViewTypeItemMap.get(Integer.valueOf(viewType))).onCreateViewHolder(viewGroup);
        viewHolder.itemView.setLayoutParams(new LayoutParams(-1, -2));
        return viewHolder;
    }

    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        getItemInternal(position).onBindViewHolder(viewHolder);
    }

    public int getItemViewType(int position) {
        Item item = getItemInternal(position);
        Class viewTypeClass = item.getViewType();
        Integer typeId = (Integer) this.mItemViewTypeMap.get(viewTypeClass);
        if (typeId == null) {
            typeId = Integer.valueOf(this.mItemViewTypeMap.size());
            this.mItemViewTypeMap.put(viewTypeClass, typeId);
            this.mViewTypeItemMap.put(typeId, item);
        }
        return typeId.intValue();
    }

    private int getGroupUnhiddenCount(int groupId) {
        ArrayList<Item> group = this.mItems[groupId];
        int result = 0;
        for (int i = 0; i < group.size(); i++) {
            if (!((Item) group.get(i)).mCurrentlyHidden) {
                result++;
            }
        }
        return result;
    }

    public long getItemId(int position) {
        return getItemInternal(position).mUniqueId;
    }

    public int getItemCount() {
        int count = 0;
        for (int i = 0; i < this.mItems.length; i++) {
            count += getGroupUnhiddenCount(i);
        }
        return count;
    }

    public Item getItemAtPosition(int position) {
        return getItemInternal(position);
    }

    public void appendToGroup(int group, Item item) {
        int position = getItemPositionInternal(group + 1, 0);
        this.mItems[group].add(item);
        if (!item.mCurrentlyHidden) {
            notifyItemInserted(position);
        }
    }

    public void appendToGroup(int group, Collection<Item> items) {
        int position = getItemPositionInternal(group + 1, 0);
        this.mItems[group].addAll(items);
        for (Item item : items) {
            item.mCurrentlyHidden = false;
        }
        notifyItemRangeInserted(position, items.size());
    }

    public void removeAllFromGroup(int groupId) {
        ArrayList<Item> group = this.mItems[groupId];
        for (int i = group.size() - 1; i >= 0; i--) {
            Item item = (Item) group.get(i);
            int position = getItemPositionInternal(groupId, i);
            group.remove(i);
            if (!item.mCurrentlyHidden) {
                notifyItemRemoved(position);
            }
        }
    }

    public void removeFromGroup(int groupId, Item item) {
        ArrayList<Item> group = this.mItems[groupId];
        for (int i = 0; i < group.size(); i++) {
            if (group.get(i) == item) {
                int position = getItemPositionInternal(groupId, i);
                group.remove(i);
                if (!item.mCurrentlyHidden) {
                    notifyItemRemoved(position);
                    return;
                }
                return;
            }
        }
        throw new RuntimeException("Item not found");
    }

    public void updateHiddenStatus() {
        int position = 0;
        int groupId = 0;
        while (true) {
            ArrayList<Item>[] arrayListArr = this.mItems;
            if (groupId < arrayListArr.length) {
                ArrayList<Item> group = arrayListArr[groupId];
                for (int positionInGroup = 0; positionInGroup < group.size(); positionInGroup++) {
                    Item item = (Item) group.get(positionInGroup);
                    boolean wasHidden = item.mCurrentlyHidden;
                    boolean isHidden = item.isHidden();
                    item.mCurrentlyHidden = isHidden;
                    if (isHidden && !wasHidden) {
                        notifyItemRemoved(position);
                    } else if (!isHidden && wasHidden) {
                        notifyItemInserted(position);
                    }
                    if (!isHidden) {
                        position++;
                    }
                }
                groupId++;
            } else {
                return;
            }
        }
    }

    public void notifyItemChanged(int groupId, Item item) {
        notifyItemChanged(getItemPositionInternal(groupId, item));
    }
}
