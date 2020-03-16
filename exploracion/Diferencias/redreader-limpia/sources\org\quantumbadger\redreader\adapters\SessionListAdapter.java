package org.quantumbadger.redreader.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import java.net.URI;
import java.util.ArrayList;
import java.util.UUID;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.SessionChangeListener;
import org.quantumbadger.redreader.activities.SessionChangeListener.SessionChangeType;
import org.quantumbadger.redreader.cache.CacheEntry;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.common.BetterSSB;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.viewholders.VH1Text;

public class SessionListAdapter extends HeaderRecyclerAdapter<ViewHolder> {
    /* access modifiers changed from: private */
    public final Context context;
    private final UUID current;
    /* access modifiers changed from: private */
    public final AppCompatDialogFragment fragment;
    private final Drawable rrIconRefresh;
    /* access modifiers changed from: private */
    public final ArrayList<CacheEntry> sessions;
    /* access modifiers changed from: private */
    public final SessionChangeType type;

    public SessionListAdapter(Context context2, URI url, UUID current2, SessionChangeType type2, AppCompatDialogFragment fragment2) {
        this.context = context2;
        this.current = current2;
        this.type = type2;
        this.fragment = fragment2;
        this.sessions = new ArrayList<>(CacheManager.getInstance(context2).getSessions(url, RedditAccountManager.getInstance(context2).getDefaultAccount()));
        TypedArray attr = context2.obtainStyledAttributes(new int[]{R.attr.rrIconRefresh});
        this.rrIconRefresh = ContextCompat.getDrawable(context2, attr.getResourceId(0, 0));
        attr.recycle();
    }

    /* access modifiers changed from: protected */
    public ViewHolder onCreateHeaderItemViewHolder(ViewGroup parent) {
        return new VH1Text(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_1_text, parent, false));
    }

    /* access modifiers changed from: protected */
    public ViewHolder onCreateContentItemViewHolder(ViewGroup parent) {
        return new VH1Text(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_1_text, parent, false));
    }

    /* access modifiers changed from: protected */
    public void onBindHeaderItemViewHolder(ViewHolder holder, int position) {
        VH1Text vh = (VH1Text) holder;
        vh.text.setText(this.context.getString(R.string.options_refresh));
        vh.text.setCompoundDrawablesWithIntrinsicBounds(this.rrIconRefresh, null, null, null);
        vh.itemView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ((SessionChangeListener) SessionListAdapter.this.context).onSessionRefreshSelected(SessionListAdapter.this.type);
                SessionListAdapter.this.fragment.dismiss();
            }
        });
    }

    /* access modifiers changed from: protected */
    public void onBindContentItemViewHolder(ViewHolder holder, final int position) {
        VH1Text vh = (VH1Text) holder;
        CacheEntry session = (CacheEntry) this.sessions.get(position);
        BetterSSB name = new BetterSSB();
        if (RRTime.utcCurrentTimeMillis() - session.timestamp < 120000) {
            name.append(RRTime.formatDurationFrom(this.context, session.timestamp), 0);
        } else {
            name.append(RRTime.formatDateTime(session.timestamp, this.context), 0);
        }
        if (session.session.equals(this.current)) {
            TypedArray attr = this.context.obtainStyledAttributes(new int[]{R.attr.rrListSubtitleCol});
            int col = attr.getColor(0, 0);
            attr.recycle();
            StringBuilder sb = new StringBuilder();
            sb.append("  (");
            sb.append(this.context.getString(R.string.session_active));
            sb.append(")");
            name.append(sb.toString(), 80, col, 0, 0.8f);
        }
        vh.text.setText(name.get());
        vh.itemView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ((SessionChangeListener) SessionListAdapter.this.context).onSessionSelected(((CacheEntry) SessionListAdapter.this.sessions.get(position)).session, SessionListAdapter.this.type);
                SessionListAdapter.this.fragment.dismiss();
            }
        });
    }

    /* access modifiers changed from: protected */
    public int getContentItemCount() {
        return this.sessions.size();
    }
}
