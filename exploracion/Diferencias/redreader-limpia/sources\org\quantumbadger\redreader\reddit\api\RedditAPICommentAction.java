package org.quantumbadger.redreader.reddit.api;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.ClipboardManager;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashSet;
import org.apache.commons.lang3.StringEscapeUtils;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.CommentEditActivity;
import org.quantumbadger.redreader.activities.CommentReplyActivity;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.fragments.CommentListingFragment;
import org.quantumbadger.redreader.fragments.CommentPropertiesDialog;
import org.quantumbadger.redreader.reddit.APIResponseHandler.APIFailureType;
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler;
import org.quantumbadger.redreader.reddit.RedditAPI;
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager;
import org.quantumbadger.redreader.reddit.prepared.RedditRenderableComment;
import org.quantumbadger.redreader.reddit.things.RedditComment;
import org.quantumbadger.redreader.reddit.url.UserProfileURL;
import org.quantumbadger.redreader.views.RedditCommentView;

public class RedditAPICommentAction {

    private static class RCVMenuItem {
        public final RedditCommentAction action;
        public final String title;

        private RCVMenuItem(Context context, int titleRes, RedditCommentAction action2) {
            this.title = context.getString(titleRes);
            this.action = action2;
        }
    }

    public enum RedditCommentAction {
        UPVOTE,
        UNVOTE,
        DOWNVOTE,
        SAVE,
        UNSAVE,
        REPORT,
        SHARE,
        COPY_TEXT,
        COPY_URL,
        REPLY,
        USER_PROFILE,
        COMMENT_LINKS,
        COLLAPSE,
        EDIT,
        DELETE,
        PROPERTIES,
        CONTEXT,
        GO_TO_COMMENT,
        ACTION_MENU,
        BACK
    }

    public static void showActionMenu(AppCompatActivity activity, CommentListingFragment commentListingFragment, RedditRenderableComment comment, RedditCommentView commentView, RedditChangeDataManager changeDataManager, boolean isArchived) {
        AppCompatActivity appCompatActivity = activity;
        RedditRenderableComment redditRenderableComment = comment;
        RedditChangeDataManager redditChangeDataManager = changeDataManager;
        RedditAccount user = RedditAccountManager.getInstance(activity).getDefaultAccount();
        ArrayList arrayList = new ArrayList();
        if (!user.isAnonymous()) {
            if (!isArchived) {
                if (!redditChangeDataManager.isUpvoted(redditRenderableComment)) {
                    arrayList.add(new RCVMenuItem(activity, R.string.action_upvote, RedditCommentAction.UPVOTE));
                } else {
                    arrayList.add(new RCVMenuItem(activity, R.string.action_upvote_remove, RedditCommentAction.UNVOTE));
                }
                if (!redditChangeDataManager.isDownvoted(redditRenderableComment)) {
                    arrayList.add(new RCVMenuItem(activity, R.string.action_downvote, RedditCommentAction.DOWNVOTE));
                } else {
                    arrayList.add(new RCVMenuItem(activity, R.string.action_downvote_remove, RedditCommentAction.UNVOTE));
                }
            }
            if (redditChangeDataManager.isSaved(redditRenderableComment)) {
                arrayList.add(new RCVMenuItem(activity, R.string.action_unsave, RedditCommentAction.UNSAVE));
            } else {
                arrayList.add(new RCVMenuItem(activity, R.string.action_save, RedditCommentAction.SAVE));
            }
            arrayList.add(new RCVMenuItem(activity, R.string.action_report, RedditCommentAction.REPORT));
            if (!isArchived) {
                arrayList.add(new RCVMenuItem(activity, R.string.action_reply, RedditCommentAction.REPLY));
            }
            if (user.username.equalsIgnoreCase(comment.getParsedComment().getRawComment().author)) {
                if (!isArchived) {
                    arrayList.add(new RCVMenuItem(activity, R.string.action_edit, RedditCommentAction.EDIT));
                }
                arrayList.add(new RCVMenuItem(activity, R.string.action_delete, RedditCommentAction.DELETE));
            }
        }
        arrayList.add(new RCVMenuItem(activity, R.string.action_comment_context, RedditCommentAction.CONTEXT));
        arrayList.add(new RCVMenuItem(activity, R.string.action_comment_go_to, RedditCommentAction.GO_TO_COMMENT));
        arrayList.add(new RCVMenuItem(activity, R.string.action_comment_links, RedditCommentAction.COMMENT_LINKS));
        if (commentListingFragment != null) {
            arrayList.add(new RCVMenuItem(activity, R.string.action_collapse, RedditCommentAction.COLLAPSE));
        }
        arrayList.add(new RCVMenuItem(activity, R.string.action_share, RedditCommentAction.SHARE));
        arrayList.add(new RCVMenuItem(activity, R.string.action_copy_text, RedditCommentAction.COPY_TEXT));
        arrayList.add(new RCVMenuItem(activity, R.string.action_copy_link, RedditCommentAction.COPY_URL));
        arrayList.add(new RCVMenuItem(activity, R.string.action_user_profile, RedditCommentAction.USER_PROFILE));
        arrayList.add(new RCVMenuItem(activity, R.string.action_properties, RedditCommentAction.PROPERTIES));
        String[] menuText = new String[arrayList.size()];
        for (int i = 0; i < menuText.length; i++) {
            menuText[i] = ((RCVMenuItem) arrayList.get(i)).title;
        }
        Builder builder = new Builder(activity);
        final RedditRenderableComment redditRenderableComment2 = comment;
        final RedditCommentView redditCommentView = commentView;
        final AppCompatActivity appCompatActivity2 = activity;
        final CommentListingFragment commentListingFragment2 = commentListingFragment;
        final ArrayList arrayList2 = arrayList;
        final RedditChangeDataManager redditChangeDataManager2 = changeDataManager;
        AnonymousClass1 r0 = new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                RedditAPICommentAction.onActionMenuItemSelected(redditRenderableComment2, redditCommentView, appCompatActivity2, commentListingFragment2, ((RCVMenuItem) arrayList2.get(which)).action, redditChangeDataManager2);
            }
        };
        builder.setItems(menuText, r0);
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
    }

    public static void onActionMenuItemSelected(RedditRenderableComment renderableComment, RedditCommentView commentView, final AppCompatActivity activity, CommentListingFragment commentListingFragment, RedditCommentAction action, RedditChangeDataManager changeDataManager) {
        AppCompatActivity appCompatActivity = activity;
        final RedditChangeDataManager redditChangeDataManager = changeDataManager;
        final RedditComment comment = renderableComment.getParsedComment().getRawComment();
        switch (action) {
            case UPVOTE:
                RedditCommentView redditCommentView = commentView;
                CommentListingFragment commentListingFragment2 = commentListingFragment;
                action(activity, comment, 0, redditChangeDataManager);
                return;
            case DOWNVOTE:
                RedditCommentView redditCommentView2 = commentView;
                CommentListingFragment commentListingFragment3 = commentListingFragment;
                action(activity, comment, 2, redditChangeDataManager);
                return;
            case UNVOTE:
                RedditCommentView redditCommentView3 = commentView;
                CommentListingFragment commentListingFragment4 = commentListingFragment;
                action(activity, comment, 1, redditChangeDataManager);
                return;
            case SAVE:
                RedditCommentView redditCommentView4 = commentView;
                CommentListingFragment commentListingFragment5 = commentListingFragment;
                action(activity, comment, 3, redditChangeDataManager);
                return;
            case UNSAVE:
                RedditCommentView redditCommentView5 = commentView;
                CommentListingFragment commentListingFragment6 = commentListingFragment;
                action(activity, comment, 5, redditChangeDataManager);
                return;
            case REPORT:
                RedditCommentView redditCommentView6 = commentView;
                CommentListingFragment commentListingFragment7 = commentListingFragment;
                new Builder(activity).setTitle(R.string.action_report).setMessage(R.string.action_report_sure).setPositiveButton(R.string.action_report, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        RedditAPICommentAction.action(activity, comment, 7, redditChangeDataManager);
                    }
                }).setNegativeButton(R.string.dialog_cancel, null).show();
                return;
            case REPLY:
                RedditCommentView redditCommentView7 = commentView;
                CommentListingFragment commentListingFragment8 = commentListingFragment;
                Intent intent = new Intent(activity, CommentReplyActivity.class);
                intent.putExtra(CommentReplyActivity.PARENT_ID_AND_TYPE_KEY, comment.getIdAndType());
                intent.putExtra(CommentReplyActivity.PARENT_MARKDOWN_KEY, StringEscapeUtils.unescapeHtml4(comment.body));
                activity.startActivity(intent);
                return;
            case EDIT:
                RedditCommentView redditCommentView8 = commentView;
                CommentListingFragment commentListingFragment9 = commentListingFragment;
                Intent intent2 = new Intent(activity, CommentEditActivity.class);
                intent2.putExtra("commentIdAndType", comment.getIdAndType());
                intent2.putExtra("commentText", StringEscapeUtils.unescapeHtml4(comment.body));
                activity.startActivity(intent2);
                return;
            case DELETE:
                RedditCommentView redditCommentView9 = commentView;
                CommentListingFragment commentListingFragment10 = commentListingFragment;
                new Builder(activity).setTitle(R.string.accounts_delete).setMessage(R.string.delete_confirm).setPositiveButton(R.string.action_delete, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        RedditAPICommentAction.action(activity, comment, 8, redditChangeDataManager);
                    }
                }).setNegativeButton(R.string.dialog_cancel, null).show();
                return;
            case COMMENT_LINKS:
                RedditCommentView redditCommentView10 = commentView;
                CommentListingFragment commentListingFragment11 = commentListingFragment;
                HashSet<String> linksInComment = comment.computeAllLinks();
                if (linksInComment.isEmpty()) {
                    General.quickToast((Context) activity, (int) R.string.error_toast_no_urls_in_comment);
                    return;
                }
                final String[] linksArr = (String[]) linksInComment.toArray(new String[linksInComment.size()]);
                Builder builder = new Builder(activity);
                builder.setItems(linksArr, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        LinkHandler.onLinkClicked(activity, linksArr[which], false);
                        dialog.dismiss();
                    }
                });
                AlertDialog alert = builder.create();
                alert.setTitle(R.string.action_comment_links);
                alert.setCanceledOnTouchOutside(true);
                alert.show();
                return;
            case SHARE:
                RedditCommentView redditCommentView11 = commentView;
                CommentListingFragment commentListingFragment12 = commentListingFragment;
                Intent mailer = new Intent("android.intent.action.SEND");
                mailer.setType("text/plain");
                String body = "";
                if (PrefsUtility.pref_behaviour_sharing_include_desc(activity, PreferenceManager.getDefaultSharedPreferences(activity))) {
                    mailer.putExtra("android.intent.extra.SUBJECT", String.format(activity.getText(R.string.share_comment_by_on_reddit).toString(), new Object[]{comment.author}));
                    if (PrefsUtility.pref_behaviour_sharing_share_text(activity, PreferenceManager.getDefaultSharedPreferences(activity))) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(StringEscapeUtils.unescapeHtml4(comment.body));
                        sb.append("\r\n\r\n");
                        body = sb.toString();
                    }
                }
                StringBuilder sb2 = new StringBuilder();
                sb2.append(body);
                sb2.append(comment.getContextUrl().generateNonJsonUri().toString());
                mailer.putExtra("android.intent.extra.TEXT", sb2.toString());
                activity.startActivityForResult(Intent.createChooser(mailer, activity.getString(R.string.action_share)), 1);
                return;
            case COPY_TEXT:
                RedditCommentView redditCommentView12 = commentView;
                CommentListingFragment commentListingFragment13 = commentListingFragment;
                ((ClipboardManager) activity.getSystemService("clipboard")).setText(StringEscapeUtils.unescapeHtml4(comment.body));
                return;
            case COPY_URL:
                RedditCommentView redditCommentView13 = commentView;
                CommentListingFragment commentListingFragment14 = commentListingFragment;
                ((ClipboardManager) activity.getSystemService("clipboard")).setText(comment.getContextUrl().context(null).generateNonJsonUri().toString());
                return;
            case COLLAPSE:
                RedditCommentView redditCommentView14 = commentView;
                commentListingFragment.handleCommentVisibilityToggle(commentView);
                return;
            case USER_PROFILE:
                LinkHandler.onLinkClicked(activity, new UserProfileURL(comment.author).toString());
                RedditCommentView redditCommentView15 = commentView;
                CommentListingFragment commentListingFragment15 = commentListingFragment;
                return;
            case PROPERTIES:
                CommentPropertiesDialog.newInstance(comment).show(activity.getSupportFragmentManager(), (String) null);
                RedditCommentView redditCommentView16 = commentView;
                CommentListingFragment commentListingFragment16 = commentListingFragment;
                return;
            case GO_TO_COMMENT:
                LinkHandler.onLinkClicked(activity, comment.getContextUrl().context(null).toString());
                RedditCommentView redditCommentView17 = commentView;
                CommentListingFragment commentListingFragment17 = commentListingFragment;
                return;
            case CONTEXT:
                LinkHandler.onLinkClicked(activity, comment.getContextUrl().toString());
                RedditCommentView redditCommentView18 = commentView;
                CommentListingFragment commentListingFragment18 = commentListingFragment;
                return;
            case ACTION_MENU:
                showActionMenu(activity, commentListingFragment, renderableComment, commentView, changeDataManager, comment.isArchived());
                RedditCommentView redditCommentView19 = commentView;
                CommentListingFragment commentListingFragment19 = commentListingFragment;
                return;
            case BACK:
                activity.onBackPressed();
                RedditCommentView redditCommentView20 = commentView;
                CommentListingFragment commentListingFragment20 = commentListingFragment;
                return;
            default:
                RedditCommentView redditCommentView21 = commentView;
                CommentListingFragment commentListingFragment21 = commentListingFragment;
                return;
        }
    }

    public static void action(AppCompatActivity activity, RedditComment comment, int action, RedditChangeDataManager changeDataManager) {
        AppCompatActivity appCompatActivity = activity;
        RedditComment redditComment = comment;
        int i = action;
        RedditChangeDataManager redditChangeDataManager = changeDataManager;
        RedditAccount user = RedditAccountManager.getInstance(activity).getDefaultAccount();
        if (user.isAnonymous()) {
            General.quickToast((Context) appCompatActivity, appCompatActivity.getString(R.string.error_toast_notloggedin));
            return;
        }
        boolean wasUpvoted = redditChangeDataManager.isUpvoted(redditComment);
        boolean wasDownvoted = redditChangeDataManager.isUpvoted(redditComment);
        boolean z = true;
        if (i != 5) {
            switch (i) {
                case 0:
                    if (!comment.isArchived()) {
                        redditChangeDataManager.markUpvoted(RRTime.utcCurrentTimeMillis(), redditComment);
                        break;
                    }
                    break;
                case 1:
                    if (!comment.isArchived()) {
                        redditChangeDataManager.markUnvoted(RRTime.utcCurrentTimeMillis(), redditComment);
                        break;
                    }
                    break;
                case 2:
                    if (!comment.isArchived()) {
                        redditChangeDataManager.markDownvoted(RRTime.utcCurrentTimeMillis(), redditComment);
                        break;
                    }
                    break;
                case 3:
                    redditChangeDataManager.markSaved(RRTime.utcCurrentTimeMillis(), redditComment, true);
                    break;
            }
        } else {
            redditChangeDataManager.markSaved(RRTime.utcCurrentTimeMillis(), redditComment, false);
        }
        boolean z2 = (i == 2) | (i == 0);
        if (i != 1) {
            z = false;
        }
        boolean vote = z2 | z;
        if (!comment.isArchived() || !vote) {
            CacheManager instance = CacheManager.getInstance(activity);
            final AppCompatActivity appCompatActivity2 = activity;
            final int i2 = action;
            final boolean z3 = wasUpvoted;
            final RedditChangeDataManager redditChangeDataManager2 = changeDataManager;
            final RedditComment redditComment2 = comment;
            final boolean z4 = wasDownvoted;
            AnonymousClass5 r0 = new ActionResponseHandler(activity) {
                /* access modifiers changed from: protected */
                public void onCallbackException(Throwable t) {
                    throw new RuntimeException(t);
                }

                /* access modifiers changed from: protected */
                public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                    revertOnFailure();
                    if (t != null) {
                        t.printStackTrace();
                    }
                    final RRError error = General.getGeneralErrorForFailure(this.context, type, t, status, null);
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            General.showResultDialog(appCompatActivity2, error);
                        }
                    });
                }

                /* access modifiers changed from: protected */
                public void onFailure(APIFailureType type) {
                    revertOnFailure();
                    final RRError error = General.getGeneralErrorForFailure(this.context, type);
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            General.showResultDialog(appCompatActivity2, error);
                        }
                    });
                }

                /* access modifiers changed from: protected */
                public void onSuccess(@Nullable String redirectUrl) {
                    if (i2 == 8) {
                        General.quickToast((Context) this.context, (int) R.string.delete_success);
                    }
                }

                private void revertOnFailure() {
                    int i = i2;
                    if (i != 5) {
                        switch (i) {
                            case 0:
                            case 1:
                            case 2:
                                if (!z3) {
                                    if (!z4) {
                                        redditChangeDataManager2.markUnvoted(RRTime.utcCurrentTimeMillis(), redditComment2);
                                        break;
                                    } else {
                                        redditChangeDataManager2.markDownvoted(RRTime.utcCurrentTimeMillis(), redditComment2);
                                        break;
                                    }
                                } else {
                                    redditChangeDataManager2.markUpvoted(RRTime.utcCurrentTimeMillis(), redditComment2);
                                    break;
                                }
                            case 3:
                                break;
                            default:
                                return;
                        }
                        redditChangeDataManager2.markSaved(RRTime.utcCurrentTimeMillis(), redditComment2, false);
                        return;
                    }
                    redditChangeDataManager2.markSaved(RRTime.utcCurrentTimeMillis(), redditComment2, true);
                }
            };
            RedditAPI.action(instance, r0, user, comment.getIdAndType(), action, activity);
            return;
        }
        Toast.makeText(appCompatActivity, R.string.error_archived_vote, 0).show();
    }
}
