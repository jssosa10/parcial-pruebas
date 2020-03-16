package org.quantumbadger.redreader.fragments;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import java.net.URI;
import java.util.UUID;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccountChangeListener;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.SessionChangeListener.SessionChangeType;
import org.quantumbadger.redreader.adapters.SessionListAdapter;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.General;

public class SessionListDialog extends AppCompatDialogFragment implements RedditAccountChangeListener {
    private volatile boolean alreadyCreated = false;
    private UUID current;
    /* access modifiers changed from: private */
    public RecyclerView rv;
    private SessionChangeType type;
    private URI url;

    public static SessionListDialog newInstance(Uri url2, UUID current2, SessionChangeType type2) {
        SessionListDialog dialog = new SessionListDialog();
        Bundle args = new Bundle(3);
        args.putString("url", url2.toString());
        if (current2 != null) {
            args.putString("current", current2.toString());
        }
        args.putString("type", type2.name());
        dialog.setArguments(args);
        return dialog;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.url = General.uriFromString(getArguments().getString("url"));
        if (getArguments().containsKey("current")) {
            this.current = UUID.fromString(getArguments().getString("current"));
        } else {
            this.current = null;
        }
        this.type = SessionChangeType.valueOf(getArguments().getString("type"));
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        if (this.alreadyCreated) {
            return getDialog();
        }
        this.alreadyCreated = true;
        Context context = getContext();
        Builder builder = new Builder(context);
        builder.setTitle(context.getString(R.string.options_past));
        this.rv = new RecyclerView(context);
        builder.setView(this.rv);
        this.rv.setLayoutManager(new LinearLayoutManager(context));
        RecyclerView recyclerView = this.rv;
        SessionListAdapter sessionListAdapter = new SessionListAdapter(context, this.url, this.current, this.type, this);
        recyclerView.setAdapter(sessionListAdapter);
        this.rv.setHasFixedSize(true);
        RedditAccountManager.getInstance(context).addUpdateListener(this);
        builder.setNeutralButton(context.getString(R.string.dialog_close), null);
        return builder.create();
    }

    public void onRedditAccountChanged() {
        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
            public void run() {
                SessionListDialog.this.rv.getAdapter().notifyDataSetChanged();
            }
        });
    }
}
