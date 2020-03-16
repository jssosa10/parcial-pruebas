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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
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

public class PostSubmitActivity extends BaseActivity {
    private static final int REQUEST_UPLOAD = 1;
    private static final String[] postTypes = {"Link", "Self", "Upload to Imgur"};
    private CheckBox markAsNsfwCheckbox;
    private CheckBox markAsSpoilerCheckbox;
    private CheckBox sendRepliesToInboxCheckbox;
    private EditText subredditEdit;
    private EditText textEdit;
    private EditText titleEdit;
    private Spinner typeSpinner;
    private Spinner usernameSpinner;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);
        setTitle((int) R.string.submit_post_actionbar);
        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.post_submit, null);
        this.typeSpinner = (Spinner) layout.findViewById(R.id.post_submit_type);
        this.usernameSpinner = (Spinner) layout.findViewById(R.id.post_submit_username);
        this.subredditEdit = (EditText) layout.findViewById(R.id.post_submit_subreddit);
        this.titleEdit = (EditText) layout.findViewById(R.id.post_submit_title);
        this.textEdit = (EditText) layout.findViewById(R.id.post_submit_body);
        this.sendRepliesToInboxCheckbox = (CheckBox) layout.findViewById(R.id.post_submit_send_replies_to_inbox);
        this.markAsNsfwCheckbox = (CheckBox) layout.findViewById(R.id.post_submit_mark_nsfw);
        this.markAsSpoilerCheckbox = (CheckBox) layout.findViewById(R.id.post_submit_mark_spoiler);
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("subreddit")) {
                String subreddit = intent.getStringExtra("subreddit");
                if (subreddit != null && subreddit.length() > 0 && !subreddit.matches("/?(r/)?all/?") && subreddit.matches("/?(r/)?\\w+/?")) {
                    this.subredditEdit.setText(subreddit);
                }
            } else if ("android.intent.action.SEND".equalsIgnoreCase(intent.getAction()) && intent.hasExtra("android.intent.extra.TEXT")) {
                this.textEdit.setText(intent.getStringExtra("android.intent.extra.TEXT"));
            }
        } else if (savedInstanceState != null && savedInstanceState.containsKey("post_title")) {
            this.titleEdit.setText(savedInstanceState.getString("post_title"));
            this.textEdit.setText(savedInstanceState.getString("post_body"));
            this.subredditEdit.setText(savedInstanceState.getString("subreddit"));
            this.typeSpinner.setSelection(savedInstanceState.getInt("post_type"));
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
            General.quickToast((Context) this, (int) R.string.error_toast_notloggedin);
            finish();
        }
        this.usernameSpinner.setAdapter(new ArrayAdapter(this, 17367043, usernames));
        this.typeSpinner.setAdapter(new ArrayAdapter(this, 17367043, postTypes));
        setHint();
        this.typeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                PostSubmitActivity.this.setHint();
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        ScrollView sv = new ScrollView(this);
        sv.addView(layout);
        setBaseActivityContentView((View) sv);
    }

    /* access modifiers changed from: private */
    public void setHint() {
        Object selected = this.typeSpinner.getSelectedItem();
        if (selected.equals("Link") || selected.equals("Upload to Imgur")) {
            this.textEdit.setHint(getString(R.string.submit_post_url_hint));
            this.textEdit.setInputType(17);
            this.textEdit.setSingleLine(true);
        } else if (selected.equals("Self")) {
            this.textEdit.setHint(getString(R.string.submit_post_self_text_hint));
            this.textEdit.setInputType(131153);
            this.textEdit.setSingleLine(false);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Unknown selection ");
            sb.append(selected.toString());
            throw new RuntimeException(sb.toString());
        }
        if (selected.equals("Upload to Imgur")) {
            this.typeSpinner.setSelection(0);
            startActivityForResult(new Intent(this, ImgurUploadActivity.class), 1);
        }
    }

    /* access modifiers changed from: protected */
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("post_title", this.titleEdit.getText().toString());
        outState.putString("post_body", this.textEdit.getText().toString());
        outState.putString("subreddit", this.subredditEdit.getText().toString());
        outState.putInt("post_type", this.typeSpinner.getSelectedItemPosition());
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem send = menu.add(R.string.comment_reply_send);
        send.setIcon(R.drawable.ic_action_send_dark);
        send.setShowAsAction(2);
        menu.add(R.string.comment_reply_preview);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().equals(getString(R.string.comment_reply_send))) {
            String subreddit = this.subredditEdit.getText().toString();
            String postTitle = this.titleEdit.getText().toString();
            String text = this.textEdit.getText().toString();
            if (subreddit.isEmpty()) {
                Toast.makeText(this, R.string.submit_post_specify_subreddit, 0).show();
                this.subredditEdit.requestFocus();
            } else if (postTitle.isEmpty()) {
                Toast.makeText(this, R.string.submit_post_title_empty, 0).show();
                this.titleEdit.requestFocus();
            } else if (!getString(R.string.submit_post_url_hint).equals(this.textEdit.getHint().toString()) || !text.isEmpty()) {
                final ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setTitle(getString(R.string.comment_reply_submitting_title));
                progressDialog.setMessage(getString(R.string.comment_reply_submitting_message));
                progressDialog.setIndeterminate(true);
                progressDialog.setCancelable(true);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setOnCancelListener(new OnCancelListener() {
                    public void onCancel(DialogInterface dialogInterface) {
                        PostSubmitActivity postSubmitActivity = PostSubmitActivity.this;
                        General.quickToast((Context) postSubmitActivity, postSubmitActivity.getString(R.string.comment_reply_oncancel));
                        General.safeDismissDialog(progressDialog);
                    }
                });
                progressDialog.setOnKeyListener(new OnKeyListener() {
                    public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                        if (keyCode == 4) {
                            PostSubmitActivity postSubmitActivity = PostSubmitActivity.this;
                            General.quickToast((Context) postSubmitActivity, postSubmitActivity.getString(R.string.comment_reply_oncancel));
                            General.safeDismissDialog(progressDialog);
                        }
                        return true;
                    }
                });
                CacheManager cm = CacheManager.getInstance(this);
                ActionResponseHandler handler = new ActionResponseHandler(this) {
                    /* access modifiers changed from: protected */
                    public void onSuccess(@Nullable final String redirectUrl) {
                        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                            public void run() {
                                General.safeDismissDialog(progressDialog);
                                General.quickToast((Context) PostSubmitActivity.this, PostSubmitActivity.this.getString(R.string.post_submit_done));
                                if (redirectUrl != null) {
                                    LinkHandler.onLinkClicked(PostSubmitActivity.this, redirectUrl);
                                }
                                PostSubmitActivity.this.finish();
                            }
                        });
                    }

                    /* access modifiers changed from: protected */
                    public void onCallbackException(Throwable t) {
                        BugReportActivity.handleGlobalError((Context) PostSubmitActivity.this, t);
                    }

                    /* access modifiers changed from: protected */
                    public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                        final RRError error = General.getGeneralErrorForFailure(this.context, type, t, status, null);
                        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                            public void run() {
                                General.showResultDialog(PostSubmitActivity.this, error);
                                General.safeDismissDialog(progressDialog);
                            }
                        });
                    }

                    /* access modifiers changed from: protected */
                    public void onFailure(APIFailureType type) {
                        final RRError error = General.getGeneralErrorForFailure(this.context, type);
                        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                            public void run() {
                                General.showResultDialog(PostSubmitActivity.this, error);
                                General.safeDismissDialog(progressDialog);
                            }
                        });
                    }
                };
                boolean is_self = this.typeSpinner.getSelectedItem().equals("Self");
                RedditAccount selectedAccount = RedditAccountManager.getInstance(this).getAccount((String) this.usernameSpinner.getSelectedItem());
                while (subreddit.startsWith("/")) {
                    subreddit = subreddit.substring(1);
                }
                while (subreddit.startsWith("r/")) {
                    subreddit = subreddit.substring(2);
                }
                String subreddit2 = subreddit;
                while (subreddit2.endsWith("/")) {
                    subreddit2 = subreddit2.substring(0, subreddit2.length() - 1);
                }
                String subreddit3 = subreddit2;
                RedditAPI.submit(cm, handler, selectedAccount, is_self, subreddit2, postTitle, text, this.sendRepliesToInboxCheckbox.isChecked(), this.markAsNsfwCheckbox.isChecked(), this.markAsSpoilerCheckbox.isChecked(), this);
                progressDialog.show();
                String str = subreddit3;
            } else {
                Toast.makeText(this, R.string.submit_post_url_empty, 0).show();
                this.textEdit.requestFocus();
            }
            return true;
        } else if (!item.getTitle().equals(getString(R.string.comment_reply_preview))) {
            return super.onOptionsItemSelected(item);
        } else {
            MarkdownPreviewDialog.newInstance(this.textEdit.getText().toString()).show(getSupportFragmentManager(), (String) null);
            return true;
        }
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && data != null && data.getData() != null) {
            this.textEdit.setText(data.getData().toString());
        }
    }

    public void onBackPressed() {
        if (General.onBackPressed()) {
            super.onBackPressed();
        }
    }
}
