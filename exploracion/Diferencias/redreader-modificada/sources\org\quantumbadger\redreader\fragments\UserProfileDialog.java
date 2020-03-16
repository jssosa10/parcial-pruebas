package org.quantumbadger.redreader.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.activities.PMSendActivity;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.reddit.APIResponseHandler.APIFailureType;
import org.quantumbadger.redreader.reddit.APIResponseHandler.UserResponseHandler;
import org.quantumbadger.redreader.reddit.RedditAPI;
import org.quantumbadger.redreader.reddit.things.RedditUser;
import org.quantumbadger.redreader.reddit.url.UserPostListingURL;
import org.quantumbadger.redreader.views.liststatus.ErrorView;
import org.quantumbadger.redreader.views.liststatus.LoadingView;

public class UserProfileDialog extends PropertiesDialog {
    /* access modifiers changed from: private */
    public boolean active = true;
    /* access modifiers changed from: private */
    public String username;

    public static UserProfileDialog newInstance(String user) {
        UserProfileDialog dialog = new UserProfileDialog();
        Bundle args = new Bundle();
        args.putString("user", user);
        dialog.setArguments(args);
        return dialog;
    }

    public void onDestroy() {
        super.onDestroy();
        this.active = false;
    }

    /* access modifiers changed from: protected */
    public String getTitle(Context context) {
        return this.username;
    }

    public final void prepare(AppCompatActivity context, final LinearLayout items) {
        final LoadingView loadingView = new LoadingView((Context) context, (int) R.string.download_waiting, true, true);
        items.addView(loadingView);
        this.username = getArguments().getString("user");
        RedditAPI.getUser(CacheManager.getInstance(context), this.username, new UserResponseHandler(context) {
            /* access modifiers changed from: protected */
            public void onDownloadStarted() {
                if (UserProfileDialog.this.active) {
                    loadingView.setIndeterminate(R.string.download_connecting);
                }
            }

            /* access modifiers changed from: protected */
            public void onSuccess(final RedditUser user, long timestamp) {
                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                    public void run() {
                        if (UserProfileDialog.this.active) {
                            loadingView.setDone(R.string.download_done);
                            LinearLayout karmaLayout = (LinearLayout) AnonymousClass1.this.context.getLayoutInflater().inflate(R.layout.karma, null);
                            items.addView(karmaLayout);
                            TextView commentKarma = (TextView) karmaLayout.findViewById(R.id.layout_karma_text_comment);
                            ((TextView) karmaLayout.findViewById(R.id.layout_karma_text_link)).setText(String.valueOf(user.link_karma));
                            commentKarma.setText(String.valueOf(user.comment_karma));
                            items.addView(UserProfileDialog.this.propView((Context) AnonymousClass1.this.context, (int) R.string.userprofile_created, (CharSequence) RRTime.formatDateTime(user.created_utc * 1000, AnonymousClass1.this.context), false));
                            Boolean bool = user.has_mail;
                            int i = R.string.general_false;
                            if (bool != null) {
                                items.addView(UserProfileDialog.this.propView((Context) AnonymousClass1.this.context, (int) R.string.userprofile_hasmail, user.has_mail.booleanValue() ? R.string.general_true : R.string.general_false, false));
                            }
                            if (user.has_mod_mail != null) {
                                LinearLayout linearLayout = items;
                                UserProfileDialog userProfileDialog = UserProfileDialog.this;
                                AppCompatActivity access$500 = AnonymousClass1.this.context;
                                if (user.has_mod_mail.booleanValue()) {
                                    i = R.string.general_true;
                                }
                                linearLayout.addView(userProfileDialog.propView((Context) access$500, (int) R.string.userprofile_hasmodmail, i, false));
                            }
                            if (user.is_friend) {
                                items.addView(UserProfileDialog.this.propView((Context) AnonymousClass1.this.context, (int) R.string.userprofile_isfriend, (int) R.string.general_true, false));
                            }
                            if (user.is_gold) {
                                items.addView(UserProfileDialog.this.propView((Context) AnonymousClass1.this.context, (int) R.string.userprofile_isgold, (int) R.string.general_true, false));
                            }
                            if (user.is_mod) {
                                items.addView(UserProfileDialog.this.propView((Context) AnonymousClass1.this.context, (int) R.string.userprofile_moderator, (int) R.string.general_true, false));
                            }
                            Button commentsButton = new Button(AnonymousClass1.this.context);
                            commentsButton.setText(R.string.userprofile_viewcomments);
                            commentsButton.setOnClickListener(new OnClickListener() {
                                public void onClick(View v) {
                                    AppCompatActivity access$1000 = AnonymousClass1.this.context;
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("/user/");
                                    sb.append(UserProfileDialog.this.username);
                                    sb.append("/comments.json");
                                    LinkHandler.onLinkClicked(access$1000, Reddit.getUri(sb.toString()).toString(), false);
                                }
                            });
                            items.addView(commentsButton);
                            commentsButton.setPadding(20, 20, 20, 20);
                            Button postsButton = new Button(AnonymousClass1.this.context);
                            postsButton.setText(R.string.userprofile_viewposts);
                            postsButton.setOnClickListener(new OnClickListener() {
                                public void onClick(View v) {
                                    LinkHandler.onLinkClicked(AnonymousClass1.this.context, UserPostListingURL.getSubmitted(UserProfileDialog.this.username).generateJsonUri().toString(), false);
                                }
                            });
                            items.addView(postsButton);
                            postsButton.setPadding(20, 20, 20, 20);
                            if (!RedditAccountManager.getInstance(AnonymousClass1.this.context).getDefaultAccount().isAnonymous()) {
                                Button pmButton = new Button(AnonymousClass1.this.context);
                                pmButton.setText(R.string.userprofile_pm);
                                pmButton.setOnClickListener(new OnClickListener() {
                                    public void onClick(View v) {
                                        Intent intent = new Intent(AnonymousClass1.this.context, PMSendActivity.class);
                                        intent.putExtra(PMSendActivity.EXTRA_RECIPIENT, UserProfileDialog.this.username);
                                        UserProfileDialog.this.startActivity(intent);
                                    }
                                });
                                items.addView(pmButton);
                                pmButton.setPadding(20, 20, 20, 20);
                            }
                        }
                    }
                });
            }

            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                BugReportActivity.handleGlobalError((Context) this.context, t);
            }

            /* access modifiers changed from: protected */
            public void onFailure(final int type, final Throwable t, final Integer status, String readableMessage) {
                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                    public void run() {
                        if (UserProfileDialog.this.active) {
                            loadingView.setDone(R.string.download_failed);
                            items.addView(new ErrorView(AnonymousClass1.this.context, General.getGeneralErrorForFailure(AnonymousClass1.this.context, type, t, status, null)));
                        }
                    }
                });
            }

            /* access modifiers changed from: protected */
            public void onFailure(final APIFailureType type) {
                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                    public void run() {
                        if (UserProfileDialog.this.active) {
                            loadingView.setDone(R.string.download_failed);
                            items.addView(new ErrorView(AnonymousClass1.this.context, General.getGeneralErrorForFailure(AnonymousClass1.this.context, type)));
                        }
                    }
                });
            }
        }, RedditAccountManager.getInstance(context).getDefaultAccount(), DownloadStrategyAlways.INSTANCE, true, context);
    }
}
