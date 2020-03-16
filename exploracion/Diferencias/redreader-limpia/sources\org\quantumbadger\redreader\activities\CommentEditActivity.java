package org.quantumbadger.redreader.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import org.quantumbadger.redreader.R;
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

public class CommentEditActivity extends BaseActivity {
    private String commentIdAndType = null;
    /* access modifiers changed from: private */
    public boolean isSelfPost = false;
    private EditText textEdit;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);
        if (getIntent() == null || !getIntent().hasExtra("isSelfPost") || !getIntent().getBooleanExtra("isSelfPost", false)) {
            setTitle((int) R.string.edit_comment_actionbar);
        } else {
            setTitle((int) R.string.edit_post_actionbar);
            this.isSelfPost = true;
        }
        this.textEdit = (EditText) getLayoutInflater().inflate(R.layout.comment_edit, null);
        if (getIntent() != null && getIntent().hasExtra("commentIdAndType")) {
            this.commentIdAndType = getIntent().getStringExtra("commentIdAndType");
            this.textEdit.setText(getIntent().getStringExtra("commentText"));
        } else if (savedInstanceState != null && savedInstanceState.containsKey("commentIdAndType")) {
            this.textEdit.setText(savedInstanceState.getString("commentText"));
            this.commentIdAndType = savedInstanceState.getString("commentIdAndType");
        }
        ScrollView sv = new ScrollView(this);
        sv.addView(this.textEdit);
        setBaseActivityContentView((View) sv);
    }

    /* access modifiers changed from: protected */
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("commentText", this.textEdit.getText().toString());
        outState.putString("commentIdAndType", this.commentIdAndType);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem send = menu.add(R.string.comment_edit_save);
        send.setIcon(R.drawable.ic_action_save_dark);
        send.setShowAsAction(2);
        menu.add(R.string.comment_reply_preview);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().equals(getString(R.string.comment_edit_save))) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(getString(R.string.comment_reply_submitting_title));
            progressDialog.setMessage(getString(R.string.comment_reply_submitting_message));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(true);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialogInterface) {
                    General.quickToast((Context) CommentEditActivity.this, (int) R.string.comment_reply_oncancel);
                    General.safeDismissDialog(progressDialog);
                }
            });
            progressDialog.setOnKeyListener(new OnKeyListener() {
                public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                    if (keyCode == 4) {
                        General.quickToast((Context) CommentEditActivity.this, (int) R.string.comment_reply_oncancel);
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
                            if (CommentEditActivity.this.isSelfPost) {
                                General.quickToast((Context) CommentEditActivity.this, (int) R.string.post_edit_done);
                            } else {
                                General.quickToast((Context) CommentEditActivity.this, (int) R.string.comment_edit_done);
                            }
                            CommentEditActivity.this.finish();
                        }
                    });
                }

                /* access modifiers changed from: protected */
                public void onCallbackException(Throwable t) {
                    BugReportActivity.handleGlobalError((Context) CommentEditActivity.this, t);
                }

                /* access modifiers changed from: protected */
                public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                    final RRError error = General.getGeneralErrorForFailure(this.context, type, t, status, null);
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            General.showResultDialog(CommentEditActivity.this, error);
                            General.safeDismissDialog(progressDialog);
                        }
                    });
                }

                /* access modifiers changed from: protected */
                public void onFailure(APIFailureType type) {
                    final RRError error = General.getGeneralErrorForFailure(this.context, type);
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            General.showResultDialog(CommentEditActivity.this, error);
                            General.safeDismissDialog(progressDialog);
                        }
                    });
                }
            };
            RedditAPI.editComment(CacheManager.getInstance(this), handler, RedditAccountManager.getInstance(this).getDefaultAccount(), this.commentIdAndType, this.textEdit.getText().toString(), this);
            progressDialog.show();
        } else if (item.getTitle().equals(getString(R.string.comment_reply_preview))) {
            MarkdownPreviewDialog.newInstance(this.textEdit.getText().toString()).show(getSupportFragmentManager(), "MarkdownPreviewDialog");
        }
        return true;
    }

    public void onBackPressed() {
        if (General.onBackPressed()) {
            super.onBackPressed();
        }
    }
}
