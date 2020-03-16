package org.quantumbadger.redreader.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import java.util.ArrayList;
import java.util.Iterator;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.fragments.MarkdownPreviewDialog;
import org.quantumbadger.redreader.reddit.APIResponseHandler.APIFailureType;
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler;
import org.quantumbadger.redreader.reddit.RedditAPI;

public class PMSendActivity extends BaseActivity {
    public static final String EXTRA_RECIPIENT = "recipient";
    public static final String EXTRA_SUBJECT = "subject";
    private static final String SAVED_STATE_RECIPIENT = "recipient";
    private static final String SAVED_STATE_SUBJECT = "pm_subject";
    private static final String SAVED_STATE_TEXT = "pm_text";
    /* access modifiers changed from: private */
    public static String lastRecipient;
    /* access modifiers changed from: private */
    public static String lastSubject;
    /* access modifiers changed from: private */
    public static String lastText;
    /* access modifiers changed from: private */
    public boolean mSendSuccess;
    private EditText recipientEdit;
    private EditText subjectEdit;
    private EditText textEdit;
    private Spinner usernameSpinner;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        String initialSubject;
        CharSequence charSequence;
        String initialRecipient;
        String initialRecipient2;
        String initialSubject2;
        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);
        setTitle((int) R.string.pm_send_actionbar);
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.pm_send, null);
        this.usernameSpinner = (Spinner) layout.findViewById(R.id.pm_send_username);
        this.recipientEdit = (EditText) layout.findViewById(R.id.pm_send_recipient);
        this.subjectEdit = (EditText) layout.findViewById(R.id.pm_send_subject);
        this.textEdit = (EditText) layout.findViewById(R.id.pm_send_text);
        if (savedInstanceState == null || !savedInstanceState.containsKey(SAVED_STATE_TEXT)) {
            Intent intent = getIntent();
            if (intent == null || !intent.hasExtra("recipient")) {
                initialRecipient2 = lastRecipient;
            } else {
                initialRecipient2 = intent.getStringExtra("recipient");
            }
            if (intent == null || !intent.hasExtra(EXTRA_SUBJECT)) {
                initialSubject2 = lastSubject;
            } else {
                initialSubject2 = intent.getStringExtra(EXTRA_SUBJECT);
            }
            CharSequence charSequence2 = initialSubject2;
            initialSubject = lastText;
            initialRecipient = initialRecipient2;
            charSequence = charSequence2;
        } else {
            initialRecipient = savedInstanceState.getString("recipient");
            charSequence = savedInstanceState.getString(SAVED_STATE_SUBJECT);
            initialSubject = savedInstanceState.getString(SAVED_STATE_TEXT);
        }
        if (initialRecipient != null) {
            this.recipientEdit.setText(initialRecipient);
        }
        if (charSequence != null) {
            this.subjectEdit.setText(charSequence);
        }
        if (initialSubject != null) {
            this.textEdit.setText(initialSubject);
        }
        ArrayList<RedditAccount> accounts = RedditAccountManager.getInstance(this).getAccounts();
        ArrayList<String> usernames = new ArrayList<>();
        Iterator it = accounts.iterator();
        while (it.hasNext()) {
            RedditAccount account = (RedditAccount) it.next();
            if (!account.isAnonymous()) {
                usernames.add(account.username);
            }
        }
        if (usernames.size() == 0) {
            General.quickToast((Context) this, getString(R.string.error_toast_notloggedin));
            finish();
        }
        this.usernameSpinner.setAdapter(new ArrayAdapter(this, 17367043, usernames));
        ScrollView sv = new ScrollView(this);
        sv.addView(layout);
        setBaseActivityContentView((View) sv);
    }

    /* access modifiers changed from: protected */
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("recipient", this.recipientEdit.getText().toString());
        outState.putString(SAVED_STATE_SUBJECT, this.subjectEdit.getText().toString());
        outState.putString(SAVED_STATE_TEXT, this.textEdit.getText().toString());
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem send = menu.add(R.string.comment_reply_send);
        send.setIcon(R.drawable.ic_action_send_dark);
        send.setShowAsAction(2);
        menu.add(R.string.comment_reply_preview);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        RedditAccount selectedAccount;
        if (item.getTitle().equals(getString(R.string.comment_reply_send))) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(getString(R.string.comment_reply_submitting_title));
            progressDialog.setMessage(getString(R.string.comment_reply_submitting_message));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(true);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialogInterface) {
                    PMSendActivity pMSendActivity = PMSendActivity.this;
                    General.quickToast((Context) pMSendActivity, pMSendActivity.getString(R.string.comment_reply_oncancel));
                    General.safeDismissDialog(progressDialog);
                }
            });
            progressDialog.setOnKeyListener(new OnKeyListener() {
                public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                    if (keyCode == 4) {
                        PMSendActivity pMSendActivity = PMSendActivity.this;
                        General.quickToast((Context) pMSendActivity, pMSendActivity.getString(R.string.comment_reply_oncancel));
                        General.safeDismissDialog(progressDialog);
                    }
                    return true;
                }
            });
            ActionResponseHandler handler = new ActionResponseHandler(this) {
                /* access modifiers changed from: protected */
                public void onSuccess(@Nullable String redirectUrl) {
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            General.safeDismissDialog(progressDialog);
                            PMSendActivity.this.mSendSuccess = true;
                            PMSendActivity.lastText = null;
                            PMSendActivity.lastRecipient = null;
                            PMSendActivity.lastSubject = null;
                            General.quickToast((Context) PMSendActivity.this, PMSendActivity.this.getString(R.string.pm_send_done));
                            PMSendActivity.this.finish();
                        }
                    });
                }

                /* access modifiers changed from: protected */
                public void onCallbackException(Throwable t) {
                    BugReportActivity.handleGlobalError((Context) PMSendActivity.this, t);
                }

                /* access modifiers changed from: protected */
                public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                    final RRError error = General.getGeneralErrorForFailure(this.context, type, t, status, null);
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            General.showResultDialog(PMSendActivity.this, error);
                            General.safeDismissDialog(progressDialog);
                        }
                    });
                }

                /* access modifiers changed from: protected */
                public void onFailure(APIFailureType type) {
                    final RRError error = General.getGeneralErrorForFailure(this.context, type);
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            General.showResultDialog(PMSendActivity.this, error);
                            General.safeDismissDialog(progressDialog);
                        }
                    });
                }
            };
            CacheManager cm = CacheManager.getInstance(this);
            Iterator it = RedditAccountManager.getInstance(this).getAccounts().iterator();
            while (true) {
                if (!it.hasNext()) {
                    selectedAccount = null;
                    break;
                }
                RedditAccount account = (RedditAccount) it.next();
                if (!account.isAnonymous() && account.username.equalsIgnoreCase((String) this.usernameSpinner.getSelectedItem())) {
                    selectedAccount = account;
                    break;
                }
            }
            if (selectedAccount != null) {
                RedditAPI.compose(cm, handler, selectedAccount, this.recipientEdit.getText().toString(), this.subjectEdit.getText().toString(), this.textEdit.getText().toString(), this);
                progressDialog.show();
            } else {
                throw new RuntimeException("Selected account no longer present");
            }
        } else if (item.getTitle().equals(getString(R.string.comment_reply_preview))) {
            MarkdownPreviewDialog.newInstance(this.textEdit.getText().toString()).show(getSupportFragmentManager(), "MarkdownPreviewDialog");
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        if (!this.mSendSuccess && this.textEdit != null) {
            lastRecipient = this.recipientEdit.getText().toString();
            lastSubject = this.subjectEdit.getText().toString();
            lastText = this.textEdit.getText().toString();
        }
    }

    public void onBackPressed() {
        if (General.onBackPressed()) {
            super.onBackPressed();
        }
    }
}
