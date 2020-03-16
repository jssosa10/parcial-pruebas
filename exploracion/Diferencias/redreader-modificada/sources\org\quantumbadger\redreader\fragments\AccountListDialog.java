package org.quantumbadger.redreader.fragments;

import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountChangeListener;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.adapters.AccountListAdapter;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.reddit.api.RedditOAuth;
import org.quantumbadger.redreader.reddit.api.RedditOAuth.LoginError;
import org.quantumbadger.redreader.reddit.api.RedditOAuth.LoginListener;

public class AccountListDialog extends AppCompatDialogFragment implements RedditAccountChangeListener {
    private volatile boolean alreadyCreated = false;
    /* access modifiers changed from: private */
    public AppCompatActivity mActivity;
    /* access modifiers changed from: private */
    public RecyclerView rv;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 123 && requestCode == resultCode && data.hasExtra("url")) {
            final ProgressDialog progressDialog = new ProgressDialog(getActivity());
            progressDialog.setTitle(R.string.accounts_loggingin);
            progressDialog.setMessage(getString(R.string.accounts_loggingin_msg));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(true);
            progressDialog.setCanceledOnTouchOutside(false);
            final AtomicBoolean cancelled = new AtomicBoolean(false);
            progressDialog.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialogInterface) {
                    cancelled.set(true);
                    General.safeDismissDialog(progressDialog);
                }
            });
            progressDialog.setOnKeyListener(new OnKeyListener() {
                public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                    if (keyCode == 4) {
                        cancelled.set(true);
                        General.safeDismissDialog(progressDialog);
                    }
                    return true;
                }
            });
            progressDialog.show();
            RedditOAuth.loginAsynchronous(this.mActivity.getApplicationContext(), Uri.parse(data.getStringExtra("url")), new LoginListener() {
                public void onLoginSuccess(RedditAccount account) {
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            General.safeDismissDialog(progressDialog);
                            if (!cancelled.get()) {
                                Builder alertBuilder = new Builder(AccountListDialog.this.mActivity);
                                alertBuilder.setNeutralButton(R.string.dialog_close, new OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                });
                                Context context = AccountListDialog.this.mActivity.getApplicationContext();
                                alertBuilder.setTitle(context.getString(R.string.general_success));
                                alertBuilder.setMessage(context.getString(R.string.message_nowloggedin));
                                alertBuilder.create().show();
                            }
                        }
                    });
                }

                public void onLoginFailure(LoginError error, final RRError details) {
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            General.safeDismissDialog(progressDialog);
                            if (!cancelled.get()) {
                                General.showResultDialog(AccountListDialog.this.mActivity, details);
                            }
                        }
                    });
                }
            });
        }
    }

    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        if (this.alreadyCreated) {
            return getDialog();
        }
        this.alreadyCreated = true;
        this.mActivity = (AppCompatActivity) getActivity();
        Builder builder = new Builder(this.mActivity);
        builder.setTitle(this.mActivity.getString(R.string.options_accounts_long));
        this.rv = new RecyclerView(this.mActivity);
        builder.setView(this.rv);
        this.rv.setLayoutManager(new LinearLayoutManager(this.mActivity));
        this.rv.setAdapter(new AccountListAdapter(this.mActivity, this));
        this.rv.setHasFixedSize(true);
        RedditAccountManager.getInstance(this.mActivity).addUpdateListener(this);
        builder.setNeutralButton(this.mActivity.getString(R.string.dialog_close), null);
        return builder.create();
    }

    public void onRedditAccountChanged() {
        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
            public void run() {
                AccountListDialog.this.rv.setAdapter(new AccountListAdapter(AccountListDialog.this.mActivity, AccountListDialog.this));
            }
        });
    }
}
