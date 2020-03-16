package org.quantumbadger.redreader.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView.LayoutParams;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.exoplayer2.upstream.DataSchemeDataSource;
import java.net.URI;
import java.util.Iterator;
import java.util.UUID;
import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter;
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter.Item;
import org.quantumbadger.redreader.cache.CacheManager;
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile;
import org.quantumbadger.redreader.cache.CacheRequest;
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.Constants.FileType;
import org.quantumbadger.redreader.common.Constants.Reddit;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.RRError;
import org.quantumbadger.redreader.common.RRThemeAttributes;
import org.quantumbadger.redreader.common.RRTime;
import org.quantumbadger.redreader.jsonwrap.JsonBufferedObject;
import org.quantumbadger.redreader.jsonwrap.JsonValue;
import org.quantumbadger.redreader.reddit.APIResponseHandler.APIFailureType;
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler;
import org.quantumbadger.redreader.reddit.RedditAPI;
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager;
import org.quantumbadger.redreader.reddit.prepared.RedditParsedComment;
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedMessage;
import org.quantumbadger.redreader.reddit.prepared.RedditRenderableComment;
import org.quantumbadger.redreader.reddit.prepared.RedditRenderableInboxItem;
import org.quantumbadger.redreader.reddit.things.RedditThing;
import org.quantumbadger.redreader.reddit.things.RedditThing.Kind;
import org.quantumbadger.redreader.views.RedditInboxItemView;
import org.quantumbadger.redreader.views.ScrollbarRecyclerViewManager;
import org.quantumbadger.redreader.views.liststatus.ErrorView;
import org.quantumbadger.redreader.views.liststatus.LoadingView;

public final class InboxListingActivity extends BaseActivity {
    private static final int OPTIONS_MENU_MARK_ALL_AS_READ = 0;
    private static final int OPTIONS_MENU_SHOW_UNREAD_ONLY = 1;
    private static final String PREF_ONLY_UNREAD = "inbox_only_show_unread";
    /* access modifiers changed from: private */
    public GroupedRecyclerViewAdapter adapter;
    private boolean isModmail = false;
    /* access modifiers changed from: private */
    public final Handler itemHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            InboxListingActivity.this.adapter.appendToGroup(0, (Item) msg.obj);
        }
    };
    /* access modifiers changed from: private */
    public LoadingView loadingView;
    /* access modifiers changed from: private */
    public RedditChangeDataManager mChangeDataManager;
    private boolean mOnlyShowUnread;
    /* access modifiers changed from: private */
    public RRThemeAttributes mTheme;
    /* access modifiers changed from: private */
    public LinearLayout notifications;
    /* access modifiers changed from: private */
    public CacheRequest request;

    /* renamed from: org.quantumbadger.redreader.activities.InboxListingActivity$4 reason: invalid class name */
    static /* synthetic */ class AnonymousClass4 {
        static final /* synthetic */ int[] $SwitchMap$org$quantumbadger$redreader$reddit$things$RedditThing$Kind = new int[Kind.values().length];

        static {
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$things$RedditThing$Kind[Kind.COMMENT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$org$quantumbadger$redreader$reddit$things$RedditThing$Kind[Kind.MESSAGE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    private final class InboxItem extends Item {
        private final RedditRenderableInboxItem mItem;
        private final int mListPosition;

        private InboxItem(int listPosition, RedditRenderableInboxItem item) {
            this.mListPosition = listPosition;
            this.mItem = item;
        }

        public Class getViewType() {
            return RedditInboxItemView.class;
        }

        public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
            InboxListingActivity inboxListingActivity = InboxListingActivity.this;
            RedditInboxItemView view = new RedditInboxItemView(inboxListingActivity, inboxListingActivity.mTheme);
            view.setLayoutParams(new LayoutParams(-1, -2));
            return new ViewHolder(view) {
            };
        }

        public void onBindViewHolder(ViewHolder viewHolder) {
            RedditInboxItemView redditInboxItemView = (RedditInboxItemView) viewHolder.itemView;
            InboxListingActivity inboxListingActivity = InboxListingActivity.this;
            redditInboxItemView.reset(inboxListingActivity, inboxListingActivity.mChangeDataManager, InboxListingActivity.this.mTheme, this.mItem, this.mListPosition != 0);
        }

        public boolean isHidden() {
            return false;
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        String title;
        PrefsUtility.applyTheme(this);
        super.onCreate(savedInstanceState);
        this.mTheme = new RRThemeAttributes(this);
        this.mChangeDataManager = RedditChangeDataManager.getInstance(RedditAccountManager.getInstance(this).getDefaultAccount());
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.isModmail = getIntent() != null && getIntent().getBooleanExtra("modmail", false);
        this.mOnlyShowUnread = sharedPreferences.getBoolean(PREF_ONLY_UNREAD, false);
        if (!this.isModmail) {
            title = getString(R.string.mainmenu_inbox);
        } else {
            title = getString(R.string.mainmenu_modmail);
        }
        setTitle((CharSequence) title);
        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(1);
        this.loadingView = new LoadingView((Context) this, getString(R.string.download_waiting), true, true);
        this.notifications = new LinearLayout(this);
        this.notifications.setOrientation(1);
        this.notifications.addView(this.loadingView);
        ScrollbarRecyclerViewManager recyclerViewManager = new ScrollbarRecyclerViewManager(this, null, false);
        this.adapter = new GroupedRecyclerViewAdapter(1);
        recyclerViewManager.getRecyclerView().setAdapter(this.adapter);
        outer.addView(this.notifications);
        outer.addView(recyclerViewManager.getOuterView());
        makeFirstRequest(this);
        setBaseActivityContentView((View) outer);
    }

    public void cancel() {
        CacheRequest cacheRequest = this.request;
        if (cacheRequest != null) {
            cacheRequest.cancel();
        }
    }

    private void makeFirstRequest(Context context) {
        URI url;
        RedditAccount user = RedditAccountManager.getInstance(context).getDefaultAccount();
        CacheManager cm = CacheManager.getInstance(context);
        if (this.isModmail) {
            url = Reddit.getUri("/message/moderator.json?limit=100");
        } else if (this.mOnlyShowUnread) {
            url = Reddit.getUri("/message/unread.json?mark=true&limit=100");
        } else {
            url = Reddit.getUri("/message/inbox.json?mark=true&limit=100");
        }
        RedditAccount redditAccount = user;
        RedditAccount redditAccount2 = user;
        AnonymousClass2 r14 = r0;
        AnonymousClass2 r0 = new CacheRequest(url, redditAccount, null, -500, 0, DownloadStrategyAlways.INSTANCE, FileType.INBOX_LIST, 0, true, true, context) {
            /* access modifiers changed from: protected */
            public void onDownloadNecessary() {
            }

            /* access modifiers changed from: protected */
            public void onDownloadStarted() {
            }

            /* access modifiers changed from: protected */
            public void onCallbackException(Throwable t) {
                InboxListingActivity.this.request = null;
                BugReportActivity.handleGlobalError(this.context, t);
            }

            /* access modifiers changed from: protected */
            public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                InboxListingActivity.this.request = null;
                if (InboxListingActivity.this.loadingView != null) {
                    InboxListingActivity.this.loadingView.setDone(R.string.download_failed);
                }
                final RRError error = General.getGeneralErrorForFailure(this.context, type, t, status, this.url.toString());
                AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                    public void run() {
                        InboxListingActivity.this.notifications.addView(new ErrorView(InboxListingActivity.this, error));
                    }
                });
                if (t != null) {
                    t.printStackTrace();
                }
            }

            /* access modifiers changed from: protected */
            public void onProgress(boolean authorizationInProgress, long bytesRead, long totalBytes) {
            }

            /* access modifiers changed from: protected */
            public void onSuccess(ReadableCacheFile cacheFile, long timestamp, UUID session, boolean fromCache, String mimetype) {
                InboxListingActivity.this.request = null;
            }

            public void onJsonParseStarted(JsonValue value, long timestamp, UUID session, boolean fromCache) {
                JsonBufferedObject data;
                JsonBufferedObject root;
                final long j = timestamp;
                if (InboxListingActivity.this.loadingView != null) {
                    InboxListingActivity.this.loadingView.setIndeterminate(R.string.download_downloading);
                }
                if (fromCache && RRTime.since(timestamp) > 600000) {
                    AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                        public void run() {
                            TextView cacheNotif = new TextView(AnonymousClass2.this.context);
                            cacheNotif.setText(AnonymousClass2.this.context.getString(R.string.listing_cached, new Object[]{RRTime.formatDateTime(j, AnonymousClass2.this.context)}));
                            int paddingPx = General.dpToPixels(AnonymousClass2.this.context, 6.0f);
                            int sidePaddingPx = General.dpToPixels(AnonymousClass2.this.context, 10.0f);
                            cacheNotif.setPadding(sidePaddingPx, paddingPx, sidePaddingPx, paddingPx);
                            cacheNotif.setTextSize(13.0f);
                            InboxListingActivity.this.notifications.addView(cacheNotif);
                            InboxListingActivity.this.adapter.notifyDataSetChanged();
                        }
                    });
                }
                AnonymousClass1 r4 = null;
                try {
                    JsonBufferedObject root2 = value.asObject();
                    JsonBufferedObject data2 = root2.getObject(DataSchemeDataSource.SCHEME_DATA);
                    int listPosition = 0;
                    Iterator it = data2.getArray("children").iterator();
                    while (it.hasNext()) {
                        RedditThing thing = (RedditThing) ((JsonValue) it.next()).asObject(RedditThing.class);
                        switch (AnonymousClass4.$SwitchMap$org$quantumbadger$redreader$reddit$things$RedditThing$Kind[thing.getKind().ordinal()]) {
                            case 1:
                                root = root2;
                                data = data2;
                                InboxListingActivity.this.itemHandler.sendMessage(General.handlerMessage(0, new InboxItem(listPosition, new RedditRenderableComment(new RedditParsedComment(thing.asComment()), null, Integer.valueOf(-100000), false))));
                                listPosition++;
                                break;
                            case 2:
                                RedditPreparedMessage message = new RedditPreparedMessage(InboxListingActivity.this, thing.asMessage(), j);
                                InboxListingActivity.this.itemHandler.sendMessage(General.handlerMessage(0, new InboxItem(listPosition, message)));
                                listPosition++;
                                if (message.src.replies != null && message.src.replies.getType() == 0) {
                                    Iterator it2 = message.src.replies.asObject().getObject(DataSchemeDataSource.SCHEME_DATA).getArray("children").iterator();
                                    while (it2.hasNext()) {
                                        JsonBufferedObject root3 = root2;
                                        JsonBufferedObject data3 = data2;
                                        InboxListingActivity.this.itemHandler.sendMessage(General.handlerMessage(0, new InboxItem(listPosition, new RedditPreparedMessage(InboxListingActivity.this, ((RedditThing) ((JsonValue) it2.next()).asObject(RedditThing.class)).asMessage(), j))));
                                        listPosition++;
                                        root2 = root3;
                                        data2 = data3;
                                        j = timestamp;
                                    }
                                    root = root2;
                                    data = data2;
                                    break;
                                } else {
                                    root = root2;
                                    data = data2;
                                    break;
                                }
                                break;
                            default:
                                JsonBufferedObject jsonBufferedObject = root2;
                                JsonBufferedObject jsonBufferedObject2 = data2;
                                throw new RuntimeException("Unknown item in list.");
                        }
                        root2 = root;
                        data2 = data;
                        j = timestamp;
                        r4 = null;
                    }
                    JsonBufferedObject jsonBufferedObject3 = data2;
                    if (InboxListingActivity.this.loadingView != null) {
                        InboxListingActivity.this.loadingView.setDone(R.string.download_done);
                    }
                } catch (Throwable t) {
                    notifyFailure(6, t, null, "Parse failure");
                }
            }
        };
        this.request = r14;
        cm.makeRequest(this.request);
    }

    public void onBackPressed() {
        if (General.onBackPressed()) {
            super.onBackPressed();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, R.string.mark_all_as_read);
        menu.add(0, 1, 1, R.string.inbox_unread_only);
        menu.getItem(1).setCheckable(true);
        if (this.mOnlyShowUnread) {
            menu.getItem(1).setChecked(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                RedditAPI.markAllAsRead(CacheManager.getInstance(this), new ActionResponseHandler(this) {
                    /* access modifiers changed from: protected */
                    public void onSuccess(@Nullable String redirectUrl) {
                        General.quickToast((Context) this.context, (int) R.string.mark_all_as_read_success);
                    }

                    /* access modifiers changed from: protected */
                    public void onCallbackException(Throwable t) {
                        BugReportActivity.addGlobalError(new RRError("Mark all as Read failed", "Callback exception", t));
                    }

                    /* access modifiers changed from: protected */
                    public void onFailure(int type, Throwable t, Integer status, String readableMessage) {
                        final RRError error = General.getGeneralErrorForFailure(this.context, type, t, status, "Reddit API action: Mark all as Read");
                        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                            public void run() {
                                General.showResultDialog(InboxListingActivity.this, error);
                            }
                        });
                    }

                    /* access modifiers changed from: protected */
                    public void onFailure(APIFailureType type) {
                        final RRError error = General.getGeneralErrorForFailure(this.context, type);
                        AndroidCommon.UI_THREAD_HANDLER.post(new Runnable() {
                            public void run() {
                                General.showResultDialog(InboxListingActivity.this, error);
                            }
                        });
                    }
                }, RedditAccountManager.getInstance(this).getDefaultAccount(), this);
                return true;
            case 1:
                boolean enabled = !item.isChecked();
                item.setChecked(enabled);
                this.mOnlyShowUnread = enabled;
                PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(PREF_ONLY_UNREAD, enabled).apply();
                General.recreateActivityNoAnimation(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
