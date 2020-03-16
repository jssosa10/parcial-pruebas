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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Iterator;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.fragments.MarkdownPreviewDialog;
import org.quantumbadger.redreader.reddit.APIResponseHandler.APIFailureType;
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler;
import org.quantumbadger.redreader.reddit.RedditAPI;

public class CommentReplyActivity extends BaseActivity {
    private static final String COMMENT_TEXT_KEY = "comment_text";
    public static final String PARENT_ID_AND_TYPE_KEY = "parentIdAndType";
    public static final String PARENT_MARKDOWN_KEY = "parent_markdown";
    public static final String PARENT_TYPE = "parentType";
    public static final String PARENT_TYPE_MESSAGE = "parentTypeMessage";
    /* access modifiers changed from: private */
    public static String lastParentIdAndType;
    /* access modifiers changed from: private */
    public static String lastText;
    private CheckBox inboxReplies;
    /* access modifiers changed from: private */
    public ParentType mParentType;
    private String parentIdAndType = null;
    private boolean sendRepliesToInbox = true;
    private EditText textEdit;
    private Spinner usernameSpinner;

    private enum ParentType {
        MESSAGE,
        COMMENT_OR_POST
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        String existingCommentText;
        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent == null || !intent.hasExtra(PARENT_TYPE) || !intent.getStringExtra(PARENT_TYPE).equals(PARENT_TYPE_MESSAGE)) {
            this.mParentType = ParentType.COMMENT_OR_POST;
            setTitle((int) R.string.submit_comment_actionbar);
        } else {
            this.mParentType = ParentType.MESSAGE;
            setTitle((int) R.string.submit_pmreply_actionbar);
        }
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.comment_reply, null);
        this.usernameSpinner = (Spinner) layout.findViewById(R.id.comment_reply_username);
        this.inboxReplies = (CheckBox) layout.findViewById(R.id.comment_reply_inbox);
        this.textEdit = (EditText) layout.findViewById(R.id.comment_reply_text);
        if (this.mParentType == ParentType.COMMENT_OR_POST) {
            this.inboxReplies.setVisibility(0);
        }
        if (intent != null && intent.hasExtra(PARENT_ID_AND_TYPE_KEY)) {
            this.parentIdAndType = intent.getStringExtra(PARENT_ID_AND_TYPE_KEY);
        } else if (savedInstanceState == null || !savedInstanceState.containsKey(PARENT_ID_AND_TYPE_KEY)) {
            throw new RuntimeException("No parent ID in CommentReplyActivity");
        } else {
            this.parentIdAndType = savedInstanceState.getString(PARENT_ID_AND_TYPE_KEY);
        }
        if (savedInstanceState != null && savedInstanceState.containsKey(COMMENT_TEXT_KEY)) {
            existingCommentText = savedInstanceState.getString(COMMENT_TEXT_KEY);
        } else if (lastText == null || !this.parentIdAndType.equals(lastParentIdAndType)) {
            existingCommentText = null;
        } else {
            existingCommentText = lastText;
        }
        if (existingCommentText != null) {
            this.textEdit.setText(existingCommentText);
        }
        if (intent != null && intent.hasExtra(PARENT_MARKDOWN_KEY)) {
            ((TextView) layout.findViewById(R.id.comment_parent_text)).setText(intent.getStringExtra(PARENT_MARKDOWN_KEY));
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
        outState.putString(COMMENT_TEXT_KEY, this.textEdit.getText().toString());
        outState.putString(PARENT_ID_AND_TYPE_KEY, this.parentIdAndType);
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
                    CommentReplyActivity commentReplyActivity = CommentReplyActivity.this;
                    General.quickToast((Context) commentReplyActivity, commentReplyActivity.getString(R.string.comment_reply_oncancel));
                    General.safeDismissDialog(progressDialog);
                }
            });
            progressDialog.setOnKeyListener(new OnKeyListener() {
                public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                    if (keyCode == 4) {
                        CommentReplyActivity commentReplyActivity = CommentReplyActivity.this;
                        General.quickToast((Context) commentReplyActivity, commentReplyActivity.getString(R.string.comment_reply_oncancel));
                        General.safeDismissDialog(progressDialog);
                    }
                    return true;
                }
            });
            ActionResponseHandler handler = new ActionResponseHandler(this) {
                /* access modifiers changed from: protected */
                public void onSuccess(@Nullable final String redirectUrl) {
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            General.safeDismissDialog(progressDialog);
                            if (CommentReplyActivity.this.mParentType == ParentType.MESSAGE) {
                                General.quickToast((Context) CommentReplyActivity.this, CommentReplyActivity.this.getString(R.string.pm_reply_done));
                            } else {
                                General.quickToast((Context) CommentReplyActivity.this, CommentReplyActivity.this.getString(R.string.comment_reply_done_norefresh));
                            }
                            CommentReplyActivity.lastText = null;
                            CommentReplyActivity.lastParentIdAndType = null;
                            if (redirectUrl != null) {
                                LinkHandler.onLinkClicked(CommentReplyActivity.this, redirectUrl);
                            }
                            CommentReplyActivity.this.finish();
                        }
                    });
                }

                /* access modifiers changed from: protected */
                public void onCallbackException(Throwable t) {
                    BugReportActivity.handleGlobalError((Context) CommentReplyActivity.this, t);
                }

                /* access modifiers changed from: protected */
                public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                    final RRError error = General.getGeneralErrorForFailure(this.context, type, t, status, null);
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            General.showResultDialog(CommentReplyActivity.this, error);
                            General.safeDismissDialog(progressDialog);
                        }
                    });
                }

                /* access modifiers changed from: protected */
                public void onFailure(APIFailureType type) {
                    final RRError error = General.getGeneralErrorForFailure(this.context, type);
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            General.showResultDialog(CommentReplyActivity.this, error);
                            General.safeDismissDialog(progressDialog);
                        }
                    });
                }
            };
            ActionResponseHandler inboxHandler = new ActionResponseHandler(this) {
                /* access modifiers changed from: protected */
                public void onSuccess(@Nullable String redirectUrl) {
                }

                /* access modifiers changed from: protected */
                public void onCallbackException(Throwable t) {
                    BugReportActivity.handleGlobalError((Context) CommentReplyActivity.this, t);
                }

                /* access modifiers changed from: protected */
                public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                    Toast.makeText(this.context, CommentReplyActivity.this.getString(R.string.disable_replies_to_infobox_failed), 0).show();
                }

                /* access modifiers changed from: protected */
                public void onFailure(APIFailureType type) {
                    Toast.makeText(this.context, CommentReplyActivity.this.getString(R.string.disable_replies_to_infobox_failed), 0).show();
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
            if (this.mParentType == ParentType.COMMENT_OR_POST) {
                this.sendRepliesToInbox = this.inboxReplies.isChecked();
            } else {
                this.sendRepliesToInbox = true;
            }
            RedditAPI.comment(cm, handler, inboxHandler, selectedAccount, this.parentIdAndType, this.textEdit.getText().toString(), this.sendRepliesToInbox, this);
            progressDialog.show();
        } else if (item.getTitle().equals(getString(R.string.comment_reply_preview))) {
            MarkdownPreviewDialog.newInstance(this.textEdit.getText().toString()).show(getSupportFragmentManager(), "MarkdownPreviewDialog");
        }
        return true;
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        EditText editText = this.textEdit;
        if (editText != null) {
            lastText = editText.getText().toString();
            lastParentIdAndType = this.parentIdAndType;
        }
    }

    public void onBackPressed() {
        if (General.onBackPressed()) {
            super.onBackPressed();
        }
    }
}
