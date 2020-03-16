package org.quantumbadger.redreader.adapters;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import java.util.ArrayList;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.OAuthLoginActivity;
import org.quantumbadger.redreader.common.BetterSSB;
import org.quantumbadger.redreader.viewholders.VH1Text;

public class AccountListAdapter extends HeaderRecyclerAdapter<ViewHolder> {
    /* access modifiers changed from: private */
    public final ArrayList<RedditAccount> accounts;
    /* access modifiers changed from: private */
    public final Context context;
    /* access modifiers changed from: private */
    public final Fragment fragment;
    private final Drawable rrIconAdd;

    public AccountListAdapter(Context context2, Fragment fragment2) {
        this.context = context2;
        this.fragment = fragment2;
        this.accounts = RedditAccountManager.getInstance(context2).getAccounts();
        TypedArray attr = context2.obtainStyledAttributes(new int[]{R.attr.rrIconAdd});
        this.rrIconAdd = ContextCompat.getDrawable(context2, attr.getResourceId(0, 0));
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
        vh.text.setText(this.context.getString(R.string.accounts_add));
        vh.text.setCompoundDrawablesWithIntrinsicBounds(this.rrIconAdd, null, null, null);
        holder.itemView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                AccountListAdapter.this.fragment.startActivityForResult(new Intent(AccountListAdapter.this.context, OAuthLoginActivity.class), 123);
            }
        });
    }

    /* access modifiers changed from: protected */
    public void onBindContentItemViewHolder(ViewHolder holder, final int position) {
        VH1Text vh = (VH1Text) holder;
        RedditAccount account = (RedditAccount) this.accounts.get(position);
        BetterSSB username = new BetterSSB();
        if (account.isAnonymous()) {
            username.append(this.context.getString(R.string.accounts_anon), 0);
        } else {
            username.append(account.username, 0);
        }
        if (account.equals(RedditAccountManager.getInstance(this.context).getDefaultAccount())) {
            TypedArray attr = this.context.obtainStyledAttributes(new int[]{R.attr.rrListSubtitleCol});
            int col = attr.getColor(0, 0);
            attr.recycle();
            StringBuilder sb = new StringBuilder();
            sb.append("  (");
            sb.append(this.context.getString(R.string.accounts_active));
            sb.append(")");
            username.append(sb.toString(), 80, col, 0, 0.8f);
        }
        vh.text.setText(username.get());
        vh.itemView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                final RedditAccount account = (RedditAccount) AccountListAdapter.this.accounts.get(position);
                final String[] items = account.isAnonymous() ? new String[]{AccountListAdapter.this.context.getString(R.string.accounts_setactive)} : new String[]{AccountListAdapter.this.context.getString(R.string.accounts_setactive), AccountListAdapter.this.context.getString(R.string.accounts_delete)};
                Builder builder = new Builder(AccountListAdapter.this.context);
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String selected = items[which];
                        if (selected.equals(AccountListAdapter.this.context.getString(R.string.accounts_setactive))) {
                            RedditAccountManager.getInstance(AccountListAdapter.this.context).setDefaultAccount(account);
                        } else if (selected.equals(AccountListAdapter.this.context.getString(R.string.accounts_delete))) {
                            new Builder(AccountListAdapter.this.context).setTitle(R.string.accounts_delete).setMessage(R.string.accounts_delete_sure).setPositiveButton(R.string.accounts_delete, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    RedditAccountManager.getInstance(AccountListAdapter.this.context).deleteAccount(account);
                                }
                            }).setNegativeButton(R.string.dialog_cancel, null).show();
                        }
                    }
                });
                builder.setNeutralButton(R.string.dialog_cancel, null);
                AlertDialog alert = builder.create();
                alert.setTitle(account.isAnonymous() ? AccountListAdapter.this.context.getString(R.string.accounts_anon) : account.username);
                alert.setCanceledOnTouchOutside(true);
                alert.show();
            }
        });
    }

    /* access modifiers changed from: protected */
    public int getContentItemCount() {
        return this.accounts.size();
    }
}
