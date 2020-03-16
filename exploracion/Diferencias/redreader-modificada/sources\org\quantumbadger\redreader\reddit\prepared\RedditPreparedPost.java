package org.quantumbadger.redreader.reddit.prepared;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.internal.view.SupportMenu;
import android.support.v7.app.AppCompatActivity;
import android.text.ClipboardManager;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;
import android.widget.Toast;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.UUID;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.BaseActivity;
import org.quantumbadger.redreader.activities.BugReportActivity;
import org.quantumbadger.redreader.activities.CommentEditActivity;
import org.quantumbadger.redreader.activities.CommentReplyActivity;
import org.quantumbadger.redreader.activities.MainActivity;
import org.quantumbadger.redreader.activities.PostListingActivity;
import org.quantumbadger.redreader.activities.WebViewActivity;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.BetterSSB;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.LinkHandler;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.fragments.PostPropertiesDialog;
import org.quantumbadger.redreader.image.SaveImageCallback;
import org.quantumbadger.redreader.image.ShareImageCallback;
import org.quantumbadger.redreader.image.ThumbnailScaler;
import org.quantumbadger.redreader.reddit.APIResponseHandler.APIFailureType;
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler;
import org.quantumbadger.redreader.reddit.RedditAPI;
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager;
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.SubredditSubscriptionState;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit;
import org.quantumbadger.redreader.reddit.things.RedditSubreddit.InvalidSubredditNameException;
import org.quantumbadger.redreader.reddit.url.SubredditPostListURL;
import org.quantumbadger.redreader.reddit.url.UserProfileURL;
import org.quantumbadger.redreader.views.RedditPostView;
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener;
import org.quantumbadger.redreader.views.bezelmenu.SideToolbarOverlay;
import org.quantumbadger.redreader.views.bezelmenu.VerticalToolbar;

public final class RedditPreparedPost {
    /* access modifiers changed from: private */
    public static final Object singleImageDecodeLock = new Object();
    /* access modifiers changed from: private */
    public RedditPostView boundView = null;
    public final boolean hasThumbnail;
    public final boolean isArchived;
    public long lastChange = Long.MIN_VALUE;
    /* access modifiers changed from: private */
    public final RedditChangeDataManager mChangeDataManager;
    public final boolean mIsProbablyAnImage;
    public SpannableStringBuilder postListDescription;
    private final boolean showSubreddit;
    public final RedditParsedPost src;
    /* access modifiers changed from: private */
    public volatile Bitmap thumbnailCache = null;
    /* access modifiers changed from: private */
    public ThumbnailLoadedCallback thumbnailCallback;
    /* access modifiers changed from: private */
    public int usageId = -1;

    public enum Action {
        UPVOTE(R.string.action_upvote),
        UNVOTE(R.string.action_vote_remove),
        DOWNVOTE(R.string.action_downvote),
        SAVE(R.string.action_save),
        HIDE(R.string.action_hide),
        UNSAVE(R.string.action_unsave),
        UNHIDE(R.string.action_unhide),
        EDIT(R.string.action_edit),
        DELETE(R.string.action_delete),
        REPORT(R.string.action_report),
        SHARE(R.string.action_share),
        REPLY(R.string.action_reply),
        USER_PROFILE(R.string.action_user_profile),
        EXTERNAL(R.string.action_external),
        PROPERTIES(R.string.action_properties),
        COMMENTS(R.string.action_comments),
        LINK(R.string.action_link),
        COMMENTS_SWITCH(R.string.action_comments_switch),
        LINK_SWITCH(R.string.action_link_switch),
        SHARE_COMMENTS(R.string.action_share_comments),
        SHARE_IMAGE(R.string.action_share_image),
        GOTO_SUBREDDIT(R.string.action_gotosubreddit),
        ACTION_MENU(R.string.action_actionmenu),
        SAVE_IMAGE(R.string.action_save_image),
        COPY(R.string.action_copy),
        SELFTEXT_LINKS(R.string.action_selftext_links),
        BACK(R.string.action_back),
        BLOCK(R.string.action_block_subreddit),
        UNBLOCK(R.string.action_unblock_subreddit),
        PIN(R.string.action_pin_subreddit),
        UNPIN(R.string.action_unpin_subreddit),
        SUBSCRIBE(R.string.action_subscribe_subreddit),
        UNSUBSCRIBE(R.string.action_unsubscribe_subreddit);
        
        public final int descriptionResId;

        private Action(int descriptionResId2) {
            this.descriptionResId = descriptionResId2;
        }
    }

    private static class RPVMenuItem {
        public final Action action;
        public final String title;

        private RPVMenuItem(Context context, int titleRes, Action action2) {
            this.title = context.getString(titleRes);
            this.action = action2;
        }
    }

    public interface ThumbnailLoadedCallback {
        void betterThumbnailAvailable(Bitmap bitmap, int i);
    }

    public RedditPreparedPost(Context context, CacheManager cm, int listId, RedditParsedPost post, long timestamp, boolean showSubreddit2, boolean showThumbnails) {
        this.src = post;
        this.showSubreddit = showSubreddit2;
        this.mChangeDataManager = RedditChangeDataManager.getInstance(RedditAccountManager.getInstance(context).getDefaultAccount());
        this.isArchived = post.isArchived();
        this.mIsProbablyAnImage = LinkHandler.isProbablyAnImage(post.getUrl());
        this.hasThumbnail = showThumbnails && hasThumbnail(post);
        int thumbnailWidth = General.dpToPixels(context, 64.0f);
        if (this.hasThumbnail && hasThumbnail(post)) {
            downloadThumbnail(context, thumbnailWidth, cm, listId);
        }
        this.lastChange = timestamp;
        this.mChangeDataManager.update(timestamp, post.getSrc());
        rebuildSubtitle(context);
    }

    public static void showActionMenu(final AppCompatActivity activity, RedditPreparedPost post) {
        EnumSet<Action> itemPref = PrefsUtility.pref_menus_post_context_items(activity, PreferenceManager.getDefaultSharedPreferences(activity));
        if (!itemPref.isEmpty()) {
            RedditAccount user = RedditAccountManager.getInstance(activity).getDefaultAccount();
            final ArrayList<RPVMenuItem> menu = new ArrayList<>();
            if (!RedditAccountManager.getInstance(activity).getDefaultAccount().isAnonymous()) {
                if (itemPref.contains(Action.UPVOTE)) {
                    if (!post.isUpvoted()) {
                        menu.add(new RPVMenuItem(activity, R.string.action_upvote, Action.UPVOTE));
                    } else {
                        menu.add(new RPVMenuItem(activity, R.string.action_upvote_remove, Action.UNVOTE));
                    }
                }
                if (itemPref.contains(Action.DOWNVOTE)) {
                    if (!post.isDownvoted()) {
                        menu.add(new RPVMenuItem(activity, R.string.action_downvote, Action.DOWNVOTE));
                    } else {
                        menu.add(new RPVMenuItem(activity, R.string.action_downvote_remove, Action.UNVOTE));
                    }
                }
                if (itemPref.contains(Action.SAVE)) {
                    if (!post.isSaved()) {
                        menu.add(new RPVMenuItem(activity, R.string.action_save, Action.SAVE));
                    } else {
                        menu.add(new RPVMenuItem(activity, R.string.action_unsave, Action.UNSAVE));
                    }
                }
                if (itemPref.contains(Action.HIDE)) {
                    if (!post.isHidden()) {
                        menu.add(new RPVMenuItem(activity, R.string.action_hide, Action.HIDE));
                    } else {
                        menu.add(new RPVMenuItem(activity, R.string.action_unhide, Action.UNHIDE));
                    }
                }
                if (itemPref.contains(Action.EDIT) && post.isSelf() && user.username.equalsIgnoreCase(post.src.getAuthor())) {
                    menu.add(new RPVMenuItem(activity, R.string.action_edit, Action.EDIT));
                }
                if (itemPref.contains(Action.DELETE) && user.username.equalsIgnoreCase(post.src.getAuthor())) {
                    menu.add(new RPVMenuItem(activity, R.string.action_delete, Action.DELETE));
                }
                if (itemPref.contains(Action.REPORT)) {
                    menu.add(new RPVMenuItem(activity, R.string.action_report, Action.REPORT));
                }
            }
            if (itemPref.contains(Action.EXTERNAL)) {
                menu.add(new RPVMenuItem(activity, R.string.action_external, Action.EXTERNAL));
            }
            if (itemPref.contains(Action.SELFTEXT_LINKS) && post.src.getRawSelfText() != null && post.src.getRawSelfText().length() > 1) {
                menu.add(new RPVMenuItem(activity, R.string.action_selftext_links, Action.SELFTEXT_LINKS));
            }
            if (itemPref.contains(Action.SAVE_IMAGE) && post.mIsProbablyAnImage) {
                menu.add(new RPVMenuItem(activity, R.string.action_save_image, Action.SAVE_IMAGE));
            }
            if (itemPref.contains(Action.GOTO_SUBREDDIT)) {
                menu.add(new RPVMenuItem(activity, R.string.action_gotosubreddit, Action.GOTO_SUBREDDIT));
            }
            if (post.showSubreddit) {
                try {
                    String subredditCanonicalName = RedditSubreddit.getCanonicalName(post.src.getSubreddit());
                    if (itemPref.contains(Action.BLOCK) && post.showSubreddit) {
                        if (PrefsUtility.pref_blocked_subreddits(activity, PreferenceManager.getDefaultSharedPreferences(activity)).contains(subredditCanonicalName)) {
                            menu.add(new RPVMenuItem(activity, R.string.action_unblock_subreddit, Action.UNBLOCK));
                        } else {
                            menu.add(new RPVMenuItem(activity, R.string.action_block_subreddit, Action.BLOCK));
                        }
                    }
                    if (!RedditAccountManager.getInstance(activity).getDefaultAccount().isAnonymous() && itemPref.contains(Action.SUBSCRIBE)) {
                        RedditSubredditSubscriptionManager subscriptionManager = RedditSubredditSubscriptionManager.getSingleton(activity, RedditAccountManager.getInstance(activity).getDefaultAccount());
                        if (subscriptionManager.areSubscriptionsReady()) {
                            if (subscriptionManager.getSubscriptionState(subredditCanonicalName) == SubredditSubscriptionState.SUBSCRIBED) {
                                menu.add(new RPVMenuItem(activity, R.string.action_unsubscribe_subreddit, Action.UNSUBSCRIBE));
                            } else {
                                menu.add(new RPVMenuItem(activity, R.string.action_subscribe_subreddit, Action.SUBSCRIBE));
                            }
                        }
                    }
                } catch (InvalidSubredditNameException ex) {
                    throw new RuntimeException(ex);
                }
            }
            if (itemPref.contains(Action.SHARE)) {
                menu.add(new RPVMenuItem(activity, R.string.action_share, Action.SHARE));
            }
            if (itemPref.contains(Action.SHARE_COMMENTS)) {
                menu.add(new RPVMenuItem(activity, R.string.action_share_comments, Action.SHARE_COMMENTS));
            }
            if (itemPref.contains(Action.SHARE_IMAGE) && post.mIsProbablyAnImage) {
                menu.add(new RPVMenuItem(activity, R.string.action_share_image, Action.SHARE_IMAGE));
            }
            if (itemPref.contains(Action.COPY)) {
                menu.add(new RPVMenuItem(activity, R.string.action_copy, Action.COPY));
            }
            if (itemPref.contains(Action.USER_PROFILE)) {
                menu.add(new RPVMenuItem(activity, R.string.action_user_profile, Action.USER_PROFILE));
            }
            if (itemPref.contains(Action.PROPERTIES)) {
                menu.add(new RPVMenuItem(activity, R.string.action_properties, Action.PROPERTIES));
            }
            String[] menuText = new String[menu.size()];
            for (int i = 0; i < menuText.length; i++) {
                menuText[i] = ((RPVMenuItem) menu.get(i)).title;
            }
            Builder builder = new Builder(activity);
            builder.setItems(menuText, new OnClickListener(post) {
                final /* synthetic */ RedditPreparedPost val$post;

                {
                    this.val$post = r1;
                }

                public void onClick(DialogInterface dialog, int which) {
                    RedditPreparedPost.onActionMenuItemSelected(this.val$post, activity, ((RPVMenuItem) menu.get(which)).action);
                }
            });
            AlertDialog alert = builder.create();
            alert.setCanceledOnTouchOutside(true);
            alert.show();
        }
    }

    public static void onActionMenuItemSelected(final RedditPreparedPost post, final AppCompatActivity activity, Action action) {
        switch (action) {
            case UPVOTE:
                post.action(activity, 0);
                return;
            case DOWNVOTE:
                post.action(activity, 2);
                return;
            case UNVOTE:
                post.action(activity, 1);
                return;
            case SAVE:
                post.action(activity, 3);
                return;
            case UNSAVE:
                post.action(activity, 5);
                return;
            case HIDE:
                post.action(activity, 4);
                return;
            case UNHIDE:
                post.action(activity, 6);
                return;
            case EDIT:
                Intent editIntent = new Intent(activity, CommentEditActivity.class);
                editIntent.putExtra("commentIdAndType", post.src.getIdAndType());
                editIntent.putExtra("commentText", StringEscapeUtils.unescapeHtml4(post.src.getRawSelfText()));
                editIntent.putExtra("isSelfPost", true);
                activity.startActivity(editIntent);
                return;
            case DELETE:
                new Builder(activity).setTitle(R.string.accounts_delete).setMessage(R.string.delete_confirm).setPositiveButton(R.string.action_delete, new OnClickListener(post) {
                    final /* synthetic */ RedditPreparedPost val$post;

                    {
                        this.val$post = r1;
                    }

                    public void onClick(DialogInterface dialog, int which) {
                        this.val$post.action(activity, 8);
                    }
                }).setNegativeButton(R.string.dialog_cancel, null).show();
                return;
            case REPORT:
                new Builder(activity).setTitle(R.string.action_report).setMessage(R.string.action_report_sure).setPositiveButton(R.string.action_report, new OnClickListener(post) {
                    final /* synthetic */ RedditPreparedPost val$post;

                    {
                        this.val$post = r1;
                    }

                    public void onClick(DialogInterface dialog, int which) {
                        this.val$post.action(activity, 7);
                    }
                }).setNegativeButton(R.string.dialog_cancel, null).show();
                return;
            case EXTERNAL:
                Intent intent = new Intent("android.intent.action.VIEW");
                intent.setData(Uri.parse(activity instanceof WebViewActivity ? ((WebViewActivity) activity).getCurrentUrl() : post.src.getUrl()));
                activity.startActivity(intent);
                return;
            case SELFTEXT_LINKS:
                HashSet<String> linksInComment = LinkHandler.computeAllLinks(StringEscapeUtils.unescapeHtml4(post.src.getRawSelfText()));
                if (linksInComment.isEmpty()) {
                    General.quickToast((Context) activity, (int) R.string.error_toast_no_urls_in_self);
                    return;
                }
                final String[] linksArr = (String[]) linksInComment.toArray(new String[linksInComment.size()]);
                Builder builder = new Builder(activity);
                builder.setItems(linksArr, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        LinkHandler.onLinkClicked(activity, linksArr[which], false, post.src.getSrc());
                        dialog.dismiss();
                    }
                });
                AlertDialog alert = builder.create();
                alert.setTitle(R.string.action_selftext_links);
                alert.setCanceledOnTouchOutside(true);
                alert.show();
                return;
            case SAVE_IMAGE:
                ((BaseActivity) activity).requestPermissionWithCallback("android.permission.WRITE_EXTERNAL_STORAGE", new SaveImageCallback(activity, post.src.getUrl()));
                return;
            case SHARE:
                Intent mailer = new Intent("android.intent.action.SEND");
                mailer.setType("text/plain");
                if (PrefsUtility.pref_behaviour_sharing_include_desc(activity, PreferenceManager.getDefaultSharedPreferences(activity))) {
                    mailer.putExtra("android.intent.extra.SUBJECT", post.src.getTitle());
                }
                mailer.putExtra("android.intent.extra.TEXT", post.src.getUrl());
                activity.startActivity(Intent.createChooser(mailer, activity.getString(R.string.action_share)));
                return;
            case SHARE_COMMENTS:
                boolean shareAsPermalink = PrefsUtility.pref_behaviour_share_permalink(activity, PreferenceManager.getDefaultSharedPreferences(activity));
                Intent mailer2 = new Intent("android.intent.action.SEND");
                mailer2.setType("text/plain");
                if (PrefsUtility.pref_behaviour_sharing_include_desc(activity, PreferenceManager.getDefaultSharedPreferences(activity))) {
                    mailer2.putExtra("android.intent.extra.SUBJECT", String.format(activity.getText(R.string.share_comments_for).toString(), new Object[]{post.src.getTitle()}));
                }
                if (shareAsPermalink) {
                    mailer2.putExtra("android.intent.extra.TEXT", Reddit.getNonAPIUri(post.src.getPermalink()).toString());
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(Reddit.PATH_COMMENTS);
                    sb.append(post.src.getIdAlone());
                    mailer2.putExtra("android.intent.extra.TEXT", Reddit.getNonAPIUri(sb.toString()).toString());
                }
                activity.startActivity(Intent.createChooser(mailer2, activity.getString(R.string.action_share_comments)));
                return;
            case SHARE_IMAGE:
                ((BaseActivity) activity).requestPermissionWithCallback("android.permission.WRITE_EXTERNAL_STORAGE", new ShareImageCallback(activity, post.src.getUrl()));
                return;
            case COPY:
                ((ClipboardManager) activity.getSystemService("clipboard")).setText(post.src.getUrl());
                return;
            case GOTO_SUBREDDIT:
                try {
                    Intent intent2 = new Intent(activity, PostListingActivity.class);
                    intent2.setData(SubredditPostListURL.getSubreddit(post.src.getSubreddit()).generateJsonUri());
                    activity.startActivityForResult(intent2, 1);
                    return;
                } catch (InvalidSubredditNameException e) {
                    Toast.makeText(activity, R.string.invalid_subreddit_name, 1).show();
                    return;
                }
            case USER_PROFILE:
                LinkHandler.onLinkClicked(activity, new UserProfileURL(post.src.getAuthor()).toString());
                return;
            case PROPERTIES:
                PostPropertiesDialog.newInstance(post.src.getSrc()).show(activity.getSupportFragmentManager(), (String) null);
                return;
            case COMMENTS:
                ((PostSelectionListener) activity).onPostCommentsSelected(post);
                new Thread(post) {
                    final /* synthetic */ RedditPreparedPost val$post;

                    {
                        this.val$post = r1;
                    }

                    public void run() {
                        this.val$post.markAsRead(activity);
                    }
                }.start();
                return;
            case LINK:
                ((PostSelectionListener) activity).onPostSelected(post);
                return;
            case COMMENTS_SWITCH:
                if (!(activity instanceof MainActivity)) {
                    activity.finish();
                }
                ((PostSelectionListener) activity).onPostCommentsSelected(post);
                return;
            case LINK_SWITCH:
                if (!(activity instanceof MainActivity)) {
                    activity.finish();
                }
                ((PostSelectionListener) activity).onPostSelected(post);
                return;
            case ACTION_MENU:
                showActionMenu(activity, post);
                return;
            case REPLY:
                Intent intent3 = new Intent(activity, CommentReplyActivity.class);
                intent3.putExtra(CommentReplyActivity.PARENT_ID_AND_TYPE_KEY, post.src.getIdAndType());
                intent3.putExtra(CommentReplyActivity.PARENT_MARKDOWN_KEY, post.src.getUnescapedSelfText());
                activity.startActivity(intent3);
                return;
            case BACK:
                activity.onBackPressed();
                return;
            case PIN:
                try {
                    String subredditCanonicalName = RedditSubreddit.getCanonicalName(post.src.getSubreddit());
                    if (!PrefsUtility.pref_pinned_subreddits(activity, PreferenceManager.getDefaultSharedPreferences(activity)).contains(subredditCanonicalName)) {
                        PrefsUtility.pref_pinned_subreddits_add(activity, PreferenceManager.getDefaultSharedPreferences(activity), subredditCanonicalName);
                        return;
                    } else {
                        Toast.makeText(activity, R.string.mainmenu_toast_pinned, 0).show();
                        return;
                    }
                } catch (InvalidSubredditNameException e2) {
                    throw new RuntimeException(e2);
                }
            case UNPIN:
                try {
                    String subredditCanonicalName2 = RedditSubreddit.getCanonicalName(post.src.getSubreddit());
                    if (PrefsUtility.pref_pinned_subreddits(activity, PreferenceManager.getDefaultSharedPreferences(activity)).contains(subredditCanonicalName2)) {
                        PrefsUtility.pref_pinned_subreddits_remove(activity, PreferenceManager.getDefaultSharedPreferences(activity), subredditCanonicalName2);
                        return;
                    } else {
                        Toast.makeText(activity, R.string.mainmenu_toast_not_pinned, 0).show();
                        return;
                    }
                } catch (InvalidSubredditNameException e3) {
                    throw new RuntimeException(e3);
                }
            case BLOCK:
                try {
                    String subredditCanonicalName3 = RedditSubreddit.getCanonicalName(post.src.getSubreddit());
                    if (!PrefsUtility.pref_blocked_subreddits(activity, PreferenceManager.getDefaultSharedPreferences(activity)).contains(subredditCanonicalName3)) {
                        PrefsUtility.pref_blocked_subreddits_add(activity, PreferenceManager.getDefaultSharedPreferences(activity), subredditCanonicalName3);
                        return;
                    } else {
                        Toast.makeText(activity, R.string.mainmenu_toast_blocked, 0).show();
                        return;
                    }
                } catch (InvalidSubredditNameException e4) {
                    throw new RuntimeException(e4);
                }
            case UNBLOCK:
                try {
                    String subredditCanonicalName4 = RedditSubreddit.getCanonicalName(post.src.getSubreddit());
                    if (PrefsUtility.pref_blocked_subreddits(activity, PreferenceManager.getDefaultSharedPreferences(activity)).contains(subredditCanonicalName4)) {
                        PrefsUtility.pref_blocked_subreddits_remove(activity, PreferenceManager.getDefaultSharedPreferences(activity), subredditCanonicalName4);
                        return;
                    } else {
                        Toast.makeText(activity, R.string.mainmenu_toast_not_blocked, 0).show();
                        return;
                    }
                } catch (InvalidSubredditNameException e5) {
                    throw new RuntimeException(e5);
                }
            case SUBSCRIBE:
                try {
                    String subredditCanonicalName5 = RedditSubreddit.getCanonicalName(post.src.getSubreddit());
                    RedditSubredditSubscriptionManager subMan = RedditSubredditSubscriptionManager.getSingleton(activity, RedditAccountManager.getInstance(activity).getDefaultAccount());
                    if (subMan.getSubscriptionState(subredditCanonicalName5) == SubredditSubscriptionState.NOT_SUBSCRIBED) {
                        subMan.subscribe(subredditCanonicalName5, activity);
                        Toast.makeText(activity, R.string.options_subscribing, 0).show();
                        return;
                    }
                    Toast.makeText(activity, R.string.mainmenu_toast_subscribed, 0).show();
                    return;
                } catch (InvalidSubredditNameException e6) {
                    throw new RuntimeException(e6);
                }
            case UNSUBSCRIBE:
                try {
                    String subredditCanonicalName6 = RedditSubreddit.getCanonicalName(post.src.getSubreddit());
                    RedditSubredditSubscriptionManager subMan2 = RedditSubredditSubscriptionManager.getSingleton(activity, RedditAccountManager.getInstance(activity).getDefaultAccount());
                    if (subMan2.getSubscriptionState(subredditCanonicalName6) == SubredditSubscriptionState.SUBSCRIBED) {
                        subMan2.unsubscribe(subredditCanonicalName6, activity);
                        Toast.makeText(activity, R.string.options_unsubscribing, 0).show();
                        return;
                    }
                    Toast.makeText(activity, R.string.mainmenu_toast_not_subscribed, 0).show();
                    return;
                } catch (InvalidSubredditNameException e7) {
                    throw new RuntimeException(e7);
                }
            default:
                return;
        }
    }

    public int computeScore() {
        int score = this.src.getScoreExcludingOwnVote();
        if (isUpvoted()) {
            return score + 1;
        }
        if (isDownvoted()) {
            return score - 1;
        }
        return score;
    }

    /* access modifiers changed from: private */
    public void rebuildSubtitle(Context context) {
        int pointsCol;
        Context context2 = context;
        TypedArray appearance = context2.obtainStyledAttributes(new int[]{R.attr.rrPostSubtitleBoldCol, R.attr.rrPostSubtitleUpvoteCol, R.attr.rrPostSubtitleDownvoteCol, R.attr.rrFlairBackCol, R.attr.rrFlairTextCol, R.attr.rrGoldTextCol, R.attr.rrGoldBackCol});
        int boldCol = appearance.getColor(0, 255);
        int rrPostSubtitleUpvoteCol = appearance.getColor(1, 255);
        int rrPostSubtitleDownvoteCol = appearance.getColor(2, 255);
        int rrFlairBackCol = appearance.getColor(3, 255);
        int rrFlairTextCol = appearance.getColor(4, 255);
        int rrGoldTextCol = appearance.getColor(5, 255);
        int rrGoldBackCol = appearance.getColor(6, 255);
        appearance.recycle();
        BetterSSB postListDescSb = new BetterSSB();
        int score = computeScore();
        if (isUpvoted()) {
            pointsCol = rrPostSubtitleUpvoteCol;
        } else if (isDownvoted()) {
            pointsCol = rrPostSubtitleDownvoteCol;
        } else {
            pointsCol = boldCol;
        }
        if (this.src.isSpoiler()) {
            postListDescSb.append(" SPOILER ", 49, -1, Color.rgb(50, 50, 50), 1.0f);
            postListDescSb.append("  ", 0);
        }
        if (this.src.isStickied()) {
            postListDescSb.append(" STICKY ", 49, -1, Color.rgb(0, 170, 0), 1.0f);
            postListDescSb.append("  ", 0);
        }
        if (this.src.isNsfw()) {
            postListDescSb.append(" NSFW ", 49, -1, SupportMenu.CATEGORY_MASK, 1.0f);
            postListDescSb.append("  ", 0);
        }
        if (this.src.getFlairText() != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.SPACE);
            sb.append(this.src.getFlairText());
            sb.append(General.LTR_OVERRIDE_MARK);
            sb.append(StringUtils.SPACE);
            postListDescSb.append(sb.toString(), 49, rrFlairTextCol, rrFlairBackCol, 1.0f);
            postListDescSb.append("  ", 0);
        }
        postListDescSb.append(String.valueOf(score), 17, pointsCol, 0, 1.0f);
        StringBuilder sb2 = new StringBuilder();
        sb2.append(StringUtils.SPACE);
        sb2.append(context2.getString(R.string.subtitle_points));
        sb2.append(StringUtils.SPACE);
        postListDescSb.append(sb2.toString(), 0);
        if (this.src.getGoldAmount() > 0) {
            postListDescSb.append(StringUtils.SPACE, 0);
            StringBuilder sb3 = new StringBuilder();
            sb3.append(StringUtils.SPACE);
            sb3.append(context2.getString(R.string.gold));
            sb3.append(BetterSSB.NBSP);
            sb3.append("x");
            sb3.append(this.src.getGoldAmount());
            sb3.append(StringUtils.SPACE);
            postListDescSb.append(sb3.toString(), 48, rrGoldTextCol, rrGoldBackCol, 1.0f);
            postListDescSb.append("  ", 0);
        }
        int i = boldCol;
        BetterSSB postListDescSb2 = postListDescSb;
        postListDescSb.append(RRTime.formatDurationFrom(context2, this.src.getCreatedTimeSecsUTC() * 1000), 17, i, 0, 1.0f);
        StringBuilder sb4 = new StringBuilder();
        sb4.append(StringUtils.SPACE);
        sb4.append(context2.getString(R.string.subtitle_by));
        sb4.append(StringUtils.SPACE);
        postListDescSb2.append(sb4.toString(), 0);
        postListDescSb2.append(this.src.getAuthor(), 17, i, 0, 1.0f);
        if (this.showSubreddit) {
            StringBuilder sb5 = new StringBuilder();
            sb5.append(StringUtils.SPACE);
            sb5.append(context2.getString(R.string.subtitle_to));
            sb5.append(StringUtils.SPACE);
            postListDescSb2.append(sb5.toString(), 0);
            postListDescSb2.append(this.src.getSubreddit(), 17, boldCol, 0, 1.0f);
        }
        StringBuilder sb6 = new StringBuilder();
        sb6.append(" (");
        sb6.append(this.src.getDomain());
        sb6.append(")");
        postListDescSb2.append(sb6.toString(), 0);
        this.postListDescription = postListDescSb2.get();
    }

    private static boolean hasThumbnail(RedditParsedPost post) {
        String url = post.getThumbnailUrl();
        return url != null && url.length() != 0 && !url.equalsIgnoreCase("nsfw") && !url.equalsIgnoreCase("self") && !url.equalsIgnoreCase("default");
    }

    private void downloadThumbnail(Context context, int widthPixels, CacheManager cm, int listId) {
        URI uri = General.uriFromString(this.src.getThumbnailUrl());
        AnonymousClass6 r14 = r0;
        final int i = widthPixels;
        AnonymousClass6 r0 = new CacheRequest(this, uri, RedditAccountManager.getAnon(), null, 100, listId, DownloadStrategyIfNotCached.INSTANCE, 200, 2, false, false, context) {
            final /* synthetic */ RedditPreparedPost this$0;

            {
                this.this$0 = this$0;
            }

            /* access modifiers changed from: protected */
            public void onDownloadNecessary() {
            }

            /* access modifiers changed from: protected */
            public void onDownloadStarted() {
            }

            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                throw new RuntimeException(t);
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
            }

            /* access modifiers changed from: protected */
            public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
            }

            /* access modifiers changed from: protected */
            /* JADX WARNING: Code restructure failed: missing block: B:20:0x005d, code lost:
                if (org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost.access$300(r9.this$0) == null) goto L_0x0086;
             */
            /* JADX WARNING: Code restructure failed: missing block: B:21:0x005f, code lost:
                org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost.access$300(r9.this$0).betterThumbnailAvailable(org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost.access$200(r9.this$0), org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost.access$400(r9.this$0));
             */
            public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
                try {
                    synchronized (RedditPreparedPost.singleImageDecodeLock) {
                        Options justDecodeBounds = new Options();
                        int factor = 1;
                        justDecodeBounds.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(cacheFile.getInputStream(), null, justDecodeBounds);
                        int width = justDecodeBounds.outWidth;
                        int height = justDecodeBounds.outHeight;
                        while (width / (factor + 1) > i && height / (factor + 1) > i) {
                            factor *= 2;
                        }
                        Options scaledOptions = new Options();
                        scaledOptions.inSampleSize = factor;
                        Bitmap data = BitmapFactory.decodeStream(cacheFile.getInputStream(), null, scaledOptions);
                        if (data != null) {
                            this.this$0.thumbnailCache = ThumbnailScaler.scale(data, i);
                            if (this.this$0.thumbnailCache != data) {
                                data.recycle();
                            }
                        }
                    }
                } catch (OutOfMemoryError e) {
                    Log.e("RedditPreparedPost", "Out of memory trying to download image");
                    e.printStackTrace();
                } catch (Throwable th) {
                }
            }
        };
        cm.makeRequest(r14);
    }

    public Bitmap getThumbnail(ThumbnailLoadedCallback callback, int usageId2) {
        this.thumbnailCallback = callback;
        this.usageId = usageId2;
        return this.thumbnailCache;
    }

    public boolean isSelf() {
        return this.src.isSelfPost();
    }

    public boolean isRead() {
        return this.mChangeDataManager.isRead(this.src);
    }

    public void bind(RedditPostView boundView2) {
        this.boundView = boundView2;
    }

    public void unbind(RedditPostView boundView2) {
        if (this.boundView == boundView2) {
            this.boundView = null;
        }
    }

    public void markAsRead(Context context) {
        RedditChangeDataManager.getInstance(RedditAccountManager.getInstance(context).getDefaultAccount()).markRead(RRTime.utcCurrentTimeMillis(), this.src);
        refreshView(context);
    }

    public void refreshView(final Context context) {
        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
            public void run() {
                RedditPreparedPost.this.rebuildSubtitle(context);
                if (RedditPreparedPost.this.boundView != null) {
                    RedditPreparedPost.this.boundView.updateAppearance();
                }
            }
        });
    }

    public void action(AppCompatActivity activity, int action) {
        final AppCompatActivity appCompatActivity = activity;
        int i = action;
        RedditAccount user = RedditAccountManager.getInstance(activity).getDefaultAccount();
        if (user.isAnonymous()) {
            AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                public void run() {
                    AppCompatActivity appCompatActivity = appCompatActivity;
                    Toast.makeText(appCompatActivity, appCompatActivity.getString(R.string.error_toast_notloggedin), 0).show();
                }
            });
            return;
        }
        int lastVoteDirection = getVoteDirection();
        boolean archived = this.src.isArchived();
        long now = RRTime.utcCurrentTimeMillis();
        boolean z = true;
        switch (i) {
            case 0:
                if (!archived) {
                    this.mChangeDataManager.markUpvoted(now, this.src);
                    break;
                }
                break;
            case 1:
                if (!archived) {
                    this.mChangeDataManager.markUnvoted(now, this.src);
                    break;
                }
                break;
            case 2:
                if (!archived) {
                    this.mChangeDataManager.markDownvoted(now, this.src);
                    break;
                }
                break;
            case 3:
                this.mChangeDataManager.markSaved(now, this.src, true);
                break;
            case 4:
                this.mChangeDataManager.markHidden(now, this.src, Boolean.valueOf(true));
                break;
            case 5:
                this.mChangeDataManager.markSaved(now, this.src, false);
                break;
            case 6:
                this.mChangeDataManager.markHidden(now, this.src, Boolean.valueOf(false));
                break;
            case 7:
            case 8:
                break;
            default:
                throw new RuntimeException("Unknown post action");
        }
        refreshView(activity);
        boolean z2 = (i == 2) | (i == 0);
        if (i != 1) {
            z = false;
        }
        boolean vote = z2 | z;
        if (!archived || !vote) {
            CacheManager instance = CacheManager.getInstance(activity);
            final int i2 = action;
            final AppCompatActivity appCompatActivity2 = activity;
            final int i3 = lastVoteDirection;
            AnonymousClass9 r0 = new ActionResponseHandler(activity) {
                /* access modifiers changed from: protected */
                public void onCallbackException(Throwable t) {
                    BugReportActivity.handleGlobalError((Context) this.context, t);
                }

                /* access modifiers changed from: protected */
                public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                    revertOnFailure();
                    if (t != null) {
                        t.printStackTrace();
                    }
                    AppCompatActivity appCompatActivity = this.context;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Reddit API action code: ");
                    sb.append(i2);
                    sb.append(StringUtils.SPACE);
                    sb.append(RedditPreparedPost.this.src.getIdAndType());
                    final RRError error = General.getGeneralErrorForFailure(appCompatActivity, type, t, status, sb.toString());
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
                    long now = RRTime.utcCurrentTimeMillis();
                    switch (i2) {
                        case 0:
                            RedditPreparedPost.this.mChangeDataManager.markUpvoted(now, RedditPreparedPost.this.src);
                            break;
                        case 1:
                            RedditPreparedPost.this.mChangeDataManager.markUnvoted(now, RedditPreparedPost.this.src);
                            break;
                        case 2:
                            RedditPreparedPost.this.mChangeDataManager.markDownvoted(now, RedditPreparedPost.this.src);
                            break;
                        case 3:
                            RedditPreparedPost.this.mChangeDataManager.markSaved(now, RedditPreparedPost.this.src, true);
                            break;
                        case 4:
                            RedditPreparedPost.this.mChangeDataManager.markHidden(now, RedditPreparedPost.this.src, Boolean.valueOf(true));
                            break;
                        case 5:
                            RedditPreparedPost.this.mChangeDataManager.markSaved(now, RedditPreparedPost.this.src, false);
                            break;
                        case 6:
                            RedditPreparedPost.this.mChangeDataManager.markHidden(now, RedditPreparedPost.this.src, Boolean.valueOf(false));
                            break;
                        case 7:
                            break;
                        case 8:
                            General.quickToast((Context) appCompatActivity2, (int) R.string.delete_success);
                            break;
                        default:
                            throw new RuntimeException("Unknown post action");
                    }
                    RedditPreparedPost.this.refreshView(this.context);
                }

                private void revertOnFailure() {
                    long now = RRTime.utcCurrentTimeMillis();
                    switch (i2) {
                        case 0:
                        case 1:
                        case 2:
                            switch (i3) {
                                case -1:
                                    RedditPreparedPost.this.mChangeDataManager.markDownvoted(now, RedditPreparedPost.this.src);
                                    break;
                                case 0:
                                    RedditPreparedPost.this.mChangeDataManager.markUnvoted(now, RedditPreparedPost.this.src);
                                    break;
                                case 1:
                                    RedditPreparedPost.this.mChangeDataManager.markUpvoted(now, RedditPreparedPost.this.src);
                                    break;
                            }
                        case 3:
                            break;
                        case 4:
                            RedditPreparedPost.this.mChangeDataManager.markHidden(now, RedditPreparedPost.this.src, Boolean.valueOf(false));
                            break;
                        case 5:
                            RedditPreparedPost.this.mChangeDataManager.markSaved(now, RedditPreparedPost.this.src, true);
                            break;
                        case 6:
                            RedditPreparedPost.this.mChangeDataManager.markHidden(now, RedditPreparedPost.this.src, Boolean.valueOf(true));
                            break;
                        case 7:
                        case 8:
                            break;
                        default:
                            throw new RuntimeException("Unknown post action");
                    }
                    RedditPreparedPost.this.mChangeDataManager.markSaved(now, RedditPreparedPost.this.src, false);
                    RedditPreparedPost.this.refreshView(this.context);
                }
            };
            RedditAPI.action(instance, r0, user, this.src.getIdAndType(), action, activity);
            return;
        }
        Toast.makeText(appCompatActivity, R.string.error_archived_vote, 0).show();
    }

    public boolean isUpvoted() {
        return this.mChangeDataManager.isUpvoted(this.src);
    }

    public boolean isDownvoted() {
        return this.mChangeDataManager.isDownvoted(this.src);
    }

    public int getVoteDirection() {
        if (isUpvoted()) {
            return 1;
        }
        return isDownvoted() ? -1 : 0;
    }

    public boolean isSaved() {
        return this.mChangeDataManager.isSaved(this.src);
    }

    public boolean isHidden() {
        return Boolean.TRUE.equals(this.mChangeDataManager.isHidden(this.src));
    }

    public VerticalToolbar generateToolbar(AppCompatActivity activity, boolean isComments, SideToolbarOverlay overlay) {
        final AppCompatActivity appCompatActivity = activity;
        VerticalToolbar toolbar = new VerticalToolbar(appCompatActivity);
        EnumSet<Action> itemsPref = PrefsUtility.pref_menus_post_toolbar_items(appCompatActivity, PreferenceManager.getDefaultSharedPreferences(activity));
        Action[] possibleItems = new Action[14];
        boolean z = false;
        possibleItems[0] = Action.ACTION_MENU;
        possibleItems[1] = isComments ? Action.LINK_SWITCH : Action.COMMENTS_SWITCH;
        possibleItems[2] = Action.UPVOTE;
        possibleItems[3] = Action.DOWNVOTE;
        possibleItems[4] = Action.SAVE;
        possibleItems[5] = Action.HIDE;
        possibleItems[6] = Action.DELETE;
        possibleItems[7] = Action.REPLY;
        possibleItems[8] = Action.EXTERNAL;
        possibleItems[9] = Action.SAVE_IMAGE;
        possibleItems[10] = Action.SHARE;
        possibleItems[11] = Action.COPY;
        possibleItems[12] = Action.USER_PROFILE;
        possibleItems[13] = Action.PROPERTIES;
        EnumMap<Action, Integer> iconsDark = new EnumMap<>(Action.class);
        iconsDark.put(Action.ACTION_MENU, Integer.valueOf(R.drawable.ic_action_overflow));
        iconsDark.put(Action.COMMENTS_SWITCH, Integer.valueOf(R.drawable.ic_action_comments_dark));
        iconsDark.put(Action.LINK_SWITCH, Integer.valueOf(this.mIsProbablyAnImage ? R.drawable.ic_action_image_dark : R.drawable.ic_action_link_dark));
        iconsDark.put(Action.UPVOTE, Integer.valueOf(R.drawable.action_upvote_dark));
        iconsDark.put(Action.DOWNVOTE, Integer.valueOf(R.drawable.action_downvote_dark));
        iconsDark.put(Action.SAVE, Integer.valueOf(R.drawable.ic_action_star_filled_dark));
        iconsDark.put(Action.HIDE, Integer.valueOf(R.drawable.ic_action_cross_dark));
        iconsDark.put(Action.REPLY, Integer.valueOf(R.drawable.ic_action_reply_dark));
        iconsDark.put(Action.EXTERNAL, Integer.valueOf(R.drawable.ic_action_external_dark));
        iconsDark.put(Action.SAVE_IMAGE, Integer.valueOf(R.drawable.ic_action_save_dark));
        iconsDark.put(Action.SHARE, Integer.valueOf(R.drawable.ic_action_share_dark));
        iconsDark.put(Action.COPY, Integer.valueOf(R.drawable.ic_action_copy_dark));
        iconsDark.put(Action.USER_PROFILE, Integer.valueOf(R.drawable.ic_action_person_dark));
        iconsDark.put(Action.PROPERTIES, Integer.valueOf(R.drawable.ic_action_info_dark));
        EnumMap<Action, Integer> iconsLight = new EnumMap<>(Action.class);
        iconsLight.put(Action.ACTION_MENU, Integer.valueOf(R.drawable.ic_action_overflow));
        iconsLight.put(Action.COMMENTS_SWITCH, Integer.valueOf(R.drawable.ic_action_comments_light));
        iconsLight.put(Action.LINK_SWITCH, Integer.valueOf(this.mIsProbablyAnImage ? R.drawable.ic_action_image_light : R.drawable.ic_action_link_light));
        iconsLight.put(Action.UPVOTE, Integer.valueOf(R.drawable.action_upvote_light));
        iconsLight.put(Action.DOWNVOTE, Integer.valueOf(R.drawable.action_downvote_light));
        iconsLight.put(Action.SAVE, Integer.valueOf(R.drawable.ic_action_star_filled_light));
        iconsLight.put(Action.HIDE, Integer.valueOf(R.drawable.ic_action_cross_light));
        iconsLight.put(Action.REPLY, Integer.valueOf(R.drawable.ic_action_reply_light));
        iconsLight.put(Action.EXTERNAL, Integer.valueOf(R.drawable.ic_action_external_light));
        iconsLight.put(Action.SAVE_IMAGE, Integer.valueOf(R.drawable.ic_action_save_light));
        iconsLight.put(Action.SHARE, Integer.valueOf(R.drawable.ic_action_share_light));
        iconsLight.put(Action.COPY, Integer.valueOf(R.drawable.ic_action_copy_light));
        iconsLight.put(Action.USER_PROFILE, Integer.valueOf(R.drawable.ic_action_person_light));
        iconsLight.put(Action.PROPERTIES, Integer.valueOf(R.drawable.ic_action_info_light));
        int length = possibleItems.length;
        int i = 0;
        while (i < length) {
            final Action action = possibleItems[i];
            if (action == Action.SAVE_IMAGE && !this.mIsProbablyAnImage) {
                SideToolbarOverlay sideToolbarOverlay = overlay;
            } else if (itemsPref.contains(action)) {
                ImageButton ib = (ImageButton) LayoutInflater.from(activity).inflate(R.layout.flat_image_button, toolbar, z);
                int buttonPadding = General.dpToPixels(appCompatActivity, 14.0f);
                ib.setPadding(buttonPadding, buttonPadding, buttonPadding, buttonPadding);
                if ((action != Action.UPVOTE || !isUpvoted()) && ((action != Action.DOWNVOTE || !isDownvoted()) && ((action != Action.SAVE || !isSaved()) && (action != Action.HIDE || !isHidden())))) {
                    ib.setImageResource(((Integer) iconsDark.get(action)).intValue());
                } else {
                    ib.setBackgroundColor(-1);
                    ib.setImageResource(((Integer) iconsLight.get(action)).intValue());
                }
                final SideToolbarOverlay sideToolbarOverlay2 = overlay;
                ib.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Action actionToTake;
                        int i = AnonymousClass12.$SwitchMap$org$quantumbadger$redreader$reddit$prepared$RedditPreparedPost$Action[action.ordinal()];
                        if (i == 4) {
                            actionToTake = RedditPreparedPost.this.isSaved() ? Action.UNSAVE : Action.SAVE;
                        } else if (i != 6) {
                            switch (i) {
                                case 1:
                                    if (!RedditPreparedPost.this.isUpvoted()) {
                                        actionToTake = Action.UPVOTE;
                                        break;
                                    } else {
                                        actionToTake = Action.UNVOTE;
                                        break;
                                    }
                                case 2:
                                    if (!RedditPreparedPost.this.isDownvoted()) {
                                        actionToTake = Action.DOWNVOTE;
                                        break;
                                    } else {
                                        actionToTake = Action.UNVOTE;
                                        break;
                                    }
                                default:
                                    actionToTake = action;
                                    break;
                            }
                        } else {
                            actionToTake = RedditPreparedPost.this.isHidden() ? Action.UNHIDE : Action.HIDE;
                        }
                        RedditPreparedPost.onActionMenuItemSelected(RedditPreparedPost.this, appCompatActivity, actionToTake);
                        sideToolbarOverlay2.hide();
                    }
                });
                Action accessibilityAction = action;
                if ((accessibilityAction == Action.UPVOTE && isUpvoted()) || (accessibilityAction == Action.DOWNVOTE && isDownvoted())) {
                    accessibilityAction = Action.UNVOTE;
                }
                if (accessibilityAction == Action.SAVE && isSaved()) {
                    accessibilityAction = Action.UNSAVE;
                }
                if (accessibilityAction == Action.HIDE && isHidden()) {
                    accessibilityAction = Action.UNHIDE;
                }
                final int textRes = accessibilityAction.descriptionResId;
                ib.setContentDescription(appCompatActivity.getString(textRes));
                ib.setOnLongClickListener(new OnLongClickListener() {
                    public boolean onLongClick(View view) {
                        General.quickToast((Context) appCompatActivity, textRes);
                        return true;
                    }
                });
                toolbar.addItem(ib);
            } else {
                SideToolbarOverlay sideToolbarOverlay3 = overlay;
            }
            i++;
            z = false;
        }
        SideToolbarOverlay sideToolbarOverlay4 = overlay;
        return toolbar;
    }
}
