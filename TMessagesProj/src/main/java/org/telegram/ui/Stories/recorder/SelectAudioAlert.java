package org.telegram.ui.Stories.recorder;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.escape;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import org.telegram.ui.Components.SearchField;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import org.telegram.ui.recyclerview.LinearSmoothScrollerCustom;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.SharedAudioCell;
import org.telegram.ui.Components.BottomSheetWithRecyclerListView;
import org.telegram.ui.Components.ChatAttachAlertAudioLayout;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ProfileMusicView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Stories.DarkThemeResourceProvider;

import java.io.File;
import java.util.ArrayList;


public class SelectAudioAlert extends BottomSheetWithRecyclerListView implements NotificationCenter.NotificationCenterDelegate, DownloadController.FileDownloadProgressListener {

    private AnimatorSet shadowAnimation;

    private final int tag;
    private boolean local;
    private final SelectAudioAlert parentAlert;
    private final ArrayList<MessageObject> localAudio = new ArrayList<>();
    private final ArrayList<MessageObject> sharedAudio = new ArrayList<>();
    private final ArrayList<MessageObject> globalAudio = new ArrayList<>();
    private final MessagesController.SavedMusicList savedMusicList;
    private final Utilities.Callback<MessageObject> onAudioSelected;
    private MessageObject downloadingMessageObject;
    private boolean withoutSavedMusic;

    private boolean ignoreScroll;

    private final FrameLayout frameLayout;
    private final View shadow;
    private final SearchField searchField;

    public SelectAudioAlert(Context context, Utilities.Callback<MessageObject> onAudioSelected, Theme.ResourcesProvider resourcesProvider) {
        this(context, false, null, onAudioSelected, resourcesProvider);
    }

    public SelectAudioAlert(Context context, boolean local, SelectAudioAlert parentAlert, Utilities.Callback<MessageObject> onAudioSelected, Theme.ResourcesProvider resourcesProvider) {
        super(context, null, true, false, false, ActionBarType.SLIDING, resourcesProvider);

        topPadding = 0.35f;
        fixNavigationBar();
        setSlidingActionBar();
        headerPaddingTop = dp(4);
        headerPaddingBottom = dp(-20);

        this.local = local;
        tag = DownloadController.getInstance(currentAccount).generateObserverTag();
        this.parentAlert = parentAlert;
        this.onAudioSelected = onAudioSelected;

        frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(getThemedColor(Theme.key_dialogBackground));
        searchField = new SearchField(context, false, resourcesProvider) {
            @Override
            public void onTextChange(String text) {
                final String lastQuery = query;
                query = text;
                if (!SelectAudioAlert.this.local) {
                    if (!TextUtils.equals(lastLoadingSharedAudioQuery, query == null ? "" : query)) {
                        cancelLoadingSharedAudio();
                        willLoadSharedAudio = query != null && query.length() > 0;
                    }
                    if (!TextUtils.equals(lastLoadingGlobalAudioQuery, query == null ? "" : query)) {
                        cancelLoadingGlobalAudio();
                        willLoadGlobalAudio = query != null && query.length() > 3 && !TextUtils.isEmpty(MessagesController.getInstance(currentAccount).config.musicSearchUsername.get());
                    }

//                    if (!TextUtils.isEmpty(lastQuery) && TextUtils.isEmpty(query)) {
//                        loadSharedAudio();
//                        loadGlobalAudio();
//                    } else {
                        loadSharedAudioDelayed();
                        loadGlobalAudioDelayed();
//                    }
                }
                adapter.update(true);
            }
        };
        searchField.getSearchEditText().setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    ignoreScroll = true;
                    scrollToSearchTop();
                }
            }
        });
        searchField.setHint(LocaleController.getString(R.string.Search));
        frameLayout.addView(searchField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 58, Gravity.TOP | Gravity.LEFT));
        frameLayout.setPadding(backgroundPaddingLeft, 0, backgroundPaddingLeft, 0);
        shadow = new View(context);
        shadow.setBackgroundColor(getThemedColor(Theme.key_dialogShadowLine));
        shadow.setAlpha(0.0f);
        shadow.setTag(1);
        FrameLayout.LayoutParams shadowParams = new FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, AndroidUtilities.getShadowHeight(), Gravity.TOP | Gravity.LEFT);
        shadowParams.topMargin = dp(58);
        containerView.addView(shadow, shadowParams);
        containerView.addView(frameLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL));

        setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
        recyclerListView.setPadding(backgroundPaddingLeft, 0, backgroundPaddingLeft, 0);
        final DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setSupportsChangeAnimations(false);
        itemAnimator.setDelayAnimations(false);
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDurations(350);
        recyclerListView.setItemAnimator(itemAnimator);

        if (!local) {
            savedMusicList = new MessagesController.SavedMusicList(currentAccount, UserConfig.getInstance(currentAccount).getClientUserId());
            savedMusicList.load();
            loadSharedAudio();

            loadGlobalAudio();
        } else {
            savedMusicList = null;
            loadLocalAudio();
        }

        recyclerListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE && ignoreScroll) {
                    ignoreScroll = false;
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateSearchY();
                if (recyclerListView.scrollingByUser && !ignoreScroll) {
                    AndroidUtilities.hideKeyboard(containerView);
                }
            }
        });
        recyclerListView.setOnItemClickListener((view, position) -> {
            if (view instanceof SharedAudioCell) {
                final SharedAudioCell cell = ((SharedAudioCell) view);
                final MessageObject messageObject = cell.getMessage();
                if (messageObject == null) return;

                DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
                if (downloadingMessageObject != null) {
                    FileLoader.getInstance(currentAccount).cancelLoadFile(downloadingMessageObject.getDocument());
                    downloadingMessageObject = null;
                }

                if (!messageObject.attachPathExists && !messageObject.mediaExists) {
                    final String fileName = messageObject.getFileName();
                    if (TextUtils.isEmpty(fileName)) {
                        return;
                    }
                    downloadingMessageObject = messageObject;
                    DownloadController.getInstance(currentAccount).addLoadingFileObserver(fileName, messageObject, this);
                    FileLoader.getInstance(currentAccount).loadFile(messageObject.getDocument(), messageObject, FileLoader.PRIORITY_NORMAL, 0);
                } else {
                    done(messageObject);
                }
            } else {
                final UItem item = adapter.getItem(position - 1);
                if (item != null && item.id == 1) {
                    new SelectAudioAlert(getContext(), true, this, onAudioSelected, resourcesProvider).show();
                } else if (item != null && item.id == 2) {
                    savedMusicList.load();
                } else if (item != null && item.id == 3) {
                    loadSharedAudio();
                } else if (item != null && item.id == 4) {
                    loadGlobalAudio();
                }
            }
        });
    }

    @Override
    protected void preDrawInternal(Canvas canvas, View parent) {
        updateSearchY();
        super.preDrawInternal(canvas, parent);
    }

    private void scrollToSearchTop() {
        final LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerListView.getLayoutManager();
        final LinearSmoothScrollerCustom linearSmoothScroller = new LinearSmoothScrollerCustom(getContext(), LinearSmoothScrollerCustom.POSITION_TOP);
        linearSmoothScroller.setTargetPosition(1);
        linearSmoothScroller.setOffset(AndroidUtilities.statusBarHeight + ActionBar.getCurrentActionBarHeight() - dp(1));
        layoutManager.startSmoothScroll(linearSmoothScroller);
    }

    private void updateSearchY() {
        float top = AndroidUtilities.displaySize.y;
        for (int i = 0; i < recyclerListView.getChildCount(); ++i) {
            final View child = recyclerListView.getChildAt(i);
            final int position = recyclerListView.getChildAdapterPosition(child);
            if (position >= 1 && child.getY() < top) {
                top = child.getY();
            }
        }
        final float offset = Math.max(AndroidUtilities.statusBarHeight + ActionBar.getCurrentActionBarHeight(), top);
        frameLayout.setTranslationY(offset);
        shadow.setTranslationY(offset);
        runShadowAnimation(top <= AndroidUtilities.statusBarHeight + ActionBar.getCurrentActionBarHeight());
    }

    private void runShadowAnimation(final boolean show) {
        if (show && shadow.getTag() != null || !show && shadow.getTag() == null) {
            shadow.setTag(show ? null : 1);
            if (show) {
                shadow.setVisibility(View.VISIBLE);
            }
            if (shadowAnimation != null) {
                shadowAnimation.cancel();
            }
            shadowAnimation = new AnimatorSet();
            shadowAnimation.playTogether(ObjectAnimator.ofFloat(shadow, View.ALPHA, show ? 1.0f : 0.0f));
            shadowAnimation.setDuration(150);
            shadowAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (shadowAnimation != null && shadowAnimation.equals(animation)) {
                        if (!show) {
                            shadow.setVisibility(View.INVISIBLE);
                        }
                        shadowAnimation = null;
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    if (shadowAnimation != null && shadowAnimation.equals(animation)) {
                        shadowAnimation = null;
                    }
                }
            });
            shadowAnimation.start();
        }
    }

    private void done(MessageObject messageObject) {
        onAudioSelected.run(messageObject);
        if (parentAlert != null) {
            parentAlert.dismiss();
        }
        dismiss();
    }

    public SelectAudioAlert withoutSavedMusic() {
        this.withoutSavedMusic = true;
        this.local = false;
        adapter.update(true);
        return this;
    }

    @Override
    public void dismiss() {
        super.dismiss();

        if (playingAudio != null && MediaController.getInstance().isPlayingMessage(playingAudio)) {
            MediaController.getInstance().cleanupPlayer(true, true);
        }
        playingAudio = null;
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.StoryMusicTitle2);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.musicListLoaded) {
            adapter.update(true);
        }
    }

    private UniversalAdapter adapter;
    @Override
    protected RecyclerListView.SelectionAdapter createAdapter(RecyclerListView listView) {
        adapter = new UniversalAdapter(listView, getContext(), currentAccount, 0, this::fillItems, resourcesProvider);
        return adapter;
    }

    private MessageObject playingAudio;
    private boolean needPlayMessage(MessageObject messageObject) {
        playingAudio = messageObject;
        ArrayList<MessageObject> arrayList = new ArrayList<>();
        arrayList.add(messageObject);
        return MediaController.getInstance().setPlaylist(arrayList, messageObject, 0);
    }

    private String query;
    public void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        adapter.itemsOffset = 1;
        int height = dp(58);
        items.add(UItem.asSpace(0, dp(58), getThemedColor(Theme.key_windowBackgroundGray)));
        if (local || withoutSavedMusic) {
            height += addSection(true, items, getString(R.string.AudioSearchLocal), localAudio, false, false, -1);
        }
        if (!local) {
            if (TextUtils.isEmpty(query) && !withoutSavedMusic) {
                adapter.whiteSectionStart();
                items.add(UItem.asButton(1, R.drawable.msg2_folder, getString(R.string.StoryMusicSelectFromFiles)).accent());
                adapter.whiteSectionEnd();
                height += dp(50);
            }
            if (!withoutSavedMusic && savedMusicList != null) {
                height += addSection(true, items, getString(R.string.AudioSearchProfile), savedMusicList.list, savedMusicList.loading, !savedMusicList.endReached, 2);
            }
            height += addSection(false, items, getString(R.string.AudioSearchChats), sharedAudio, willLoadSharedAudio || loadingSharedAudio, sharedAudioHasMore, 3);
            height += addSection(false, items, getString(R.string.AudioSearchGlobal), globalAudio, willLoadGlobalAudio || loadingGlobalAudio, globalAudioHasMore, 4);
        }
        if (items.size() <= (!local && TextUtils.isEmpty(query) && !withoutSavedMusic ? 2 : 1)) {
            if (TextUtils.isEmpty(query)) {
                items.add(ChatAttachAlertAudioLayout.EmptyView.Factory.as(getString(R.string.NoAudioFound), getString(R.string.NoAudioFilesInfo)));
            } else {
                items.add(ChatAttachAlertAudioLayout.EmptyView.Factory.as(getString(R.string.NoAudioFound), AndroidUtilities.replaceTags(formatString(query.length() >= 3 ? R.string.NoAudioFoundInfo2 : R.string.NoAudioFoundInfo, query))));
            }
        }
//        if (!TextUtils.isEmpty(query) || isKeyboardVisible()) {
            items.add(UItem.asShadow(null));
            height += dp(12);
            items.add(UItem.asSpace(0, Math.max(0, AndroidUtilities.displaySize.y - height - AndroidUtilities.statusBarHeight - ActionBar.getCurrentActionBarHeight() + dp(24)), getThemedColor(Theme.key_windowBackgroundGray)));
//        }
    }

    private int addSection(boolean filter, ArrayList<UItem> items, String name, ArrayList<MessageObject> messageObjects, boolean loading, boolean showMore, int showMoreId) {
        if (messageObjects == null || messageObjects.isEmpty() && !loading) {
            return 0;
        }
        final ArrayList<MessageObject> filtered = new ArrayList<>();
        final String lquery = query == null ? null : query.toLowerCase();
        final String tquery = AndroidUtilities.translitSafe(lquery);
        for (final MessageObject messageObject : messageObjects) {
            if (!filter) {
                messageObject.setQuery(query);
                filtered.add(messageObject);
            } else if (TextUtils.isEmpty(lquery) || messageObjects == sharedAudio) {
                messageObject.setQuery(null);
                filtered.add(messageObject);
            } else {
                final String title = messageObject.getMusicTitle();
                final String author = messageObject.getMusicAuthor();
                if (matches(lquery, tquery, title) || matches(lquery, tquery, author)) {
                    messageObject.setQuery(query);
                    filtered.add(messageObject);
                }
            }
        }
        if (filtered.isEmpty() && !loading) {
            return 0;
        }
        int height = 0;
        if (!items.isEmpty() && items.size() > 1) {
            items.add(UItem.asShadow(null));
            height += dp(12);
        }
        adapter.whiteSectionStart();
        items.add(UItem.asHeader(name));
        for (final MessageObject messageObject : filtered) {
            items.add(SharedAudioCell.Factory.as(messageObject, this::needPlayMessage));
            height += dp(56);
        }
        if (loading) {
            items.add(UItem.asFlicker(FlickerLoadingView.AUDIO_TYPE));
            items.add(UItem.asFlicker(FlickerLoadingView.AUDIO_TYPE));
            items.add(UItem.asFlicker(FlickerLoadingView.AUDIO_TYPE));
            height += 3 * dp(56);
        }
        if (showMore && !loading) {
            items.add(UItem.asButton(showMoreId, R.drawable.arrow_more, getString(R.string.ShowMore)).accent());
            height += dp(50);
        }
        adapter.whiteSectionEnd();
        return height;
    }

    private boolean matches(String lquery, String tquery, String name) {
        if (name == null) return false;
        final String lname = name.toLowerCase();
        if (lname.startsWith(lquery) || lname.contains(" " + lquery))
            return true;
        final String tname = AndroidUtilities.translitSafe(lname);
        if (tname.startsWith(tquery) || tname.contains(" " + tquery))
            return true;
        return false;
    }

    private int nextSearchRate;
    private boolean sharedAudioHasMore;
    private boolean loadingSharedAudio;
    private boolean willLoadSharedAudio;
    private String lastLoadingSharedAudioQuery;
    private int loadingSharedAudioRequestId = -1;
    private void cancelLoadingSharedAudio() {
        if (loadingSharedAudioRequestId >= 0) {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(loadingSharedAudioRequestId, true);
        }
        loadingSharedAudioRequestId = -1;
        nextSearchRate = 0;
        sharedAudio.clear();
        loadingSharedAudio = false;
        willLoadSharedAudio = false;
    }
    private void loadSharedAudioDelayed() {
        AndroidUtilities.cancelRunOnUIThread(loadSharedAudioRunnable);
        AndroidUtilities.runOnUIThread(loadSharedAudioRunnable, 400);
    }
    private Runnable loadSharedAudioRunnable = this::loadSharedAudio;
    private void loadSharedAudio() {
        if (local) return;

        if (!TextUtils.equals(lastLoadingSharedAudioQuery, query == null ? "" : query)) {
            cancelLoadingSharedAudio();
        }

        if (loadingSharedAudio) return;
        if (!sharedAudio.isEmpty() && !sharedAudioHasMore) return;

        loadingSharedAudio = true;

        final TLRPC.TL_messages_searchGlobal req = new TLRPC.TL_messages_searchGlobal();
        req.filter = new TLRPC.TL_inputMessagesFilterMusic();
        req.q = lastLoadingSharedAudioQuery = query == null ? "" : query;
        req.limit = 20;
        if (sharedAudio != null && sharedAudio.size() > 0) {
            final MessageObject lastMessage = sharedAudio.get(sharedAudio.size() - 1);
            req.offset_id = lastMessage.getId();
            req.offset_rate = nextSearchRate;
            long id = MessageObject.getPeerId(lastMessage.messageOwner.peer_id);
            req.offset_peer = MessagesController.getInstance(currentAccount).getInputPeer(id);
        } else {
            req.offset_rate = 0;
            req.offset_id = 0;
            req.offset_peer = new TLRPC.TL_inputPeerEmpty();
        }
        loadingSharedAudioRequestId = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            willLoadSharedAudio = false;
            loadingSharedAudio = false;
            if (res instanceof TLRPC.messages_Messages) {
                final TLRPC.messages_Messages r = (TLRPC.messages_Messages) res;
                MessagesController.getInstance(currentAccount).putUsers(r.users, false);
                MessagesController.getInstance(currentAccount).putChats(r.chats, false);

                for (final TLRPC.Message message : r.messages) {
                    final MessageObject messageObject = new MessageObject(currentAccount, message, false, true);
                    sharedAudio.add(messageObject);
                }

                sharedAudioHasMore = r instanceof TLRPC.TL_messages_messagesSlice && sharedAudio.size() < r.count;
                nextSearchRate = r.next_rate;
            } else {
                sharedAudioHasMore = false;
                nextSearchRate = 0;
            }
            adapter.update(true);
        }));

        adapter.update(true);
    }

    private String globalAudioOffset;
    private boolean globalAudioHasMore;
    private boolean loadingGlobalAudio;
    private boolean willLoadGlobalAudio;
    private TLRPC.User globalAudioBot;
    private boolean resolvingGlobalAudioBot;
    private boolean failedToResolveGlobalAudioBot;
    private int loadingGlobalAudioRequestId = -1;
    private String lastLoadingGlobalAudioQuery;
    private int globalAudioId = -2000000000;
    private void cancelLoadingGlobalAudio() {
        if (loadingGlobalAudioRequestId >= 0) {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(loadingGlobalAudioRequestId, true);
        }
        loadingGlobalAudioRequestId = -1;
        globalAudioOffset = "";
        globalAudioHasMore = false;
        globalAudio.clear();
        loadingGlobalAudio = false;
        willLoadGlobalAudio = false;
    }
    private void loadGlobalAudioDelayed() {
        AndroidUtilities.cancelRunOnUIThread(loadGlobalAudioRunnable);
        AndroidUtilities.runOnUIThread(loadGlobalAudioRunnable, 400);
    }
    private Runnable loadGlobalAudioRunnable = this::loadGlobalAudio;
    private void loadGlobalAudio() {
        final String botUsername = MessagesController.getInstance(currentAccount).config.musicSearchUsername.get();
        if (TextUtils.isEmpty(botUsername)) return;

        if (!TextUtils.equals(lastLoadingGlobalAudioQuery, query == null ? "" : query)) {
            cancelLoadingGlobalAudio();
        }

        if (loadingGlobalAudio) return;
        if (TextUtils.isEmpty(query) || query.length() < 3) return;
        if (!globalAudio.isEmpty() && !globalAudioHasMore) return;

        if (globalAudioBot == null) {
            globalAudioBot = MessagesController.getInstance(currentAccount).getUser(botUsername);
        }
        if (globalAudioBot == null) {
            if (resolvingGlobalAudioBot || failedToResolveGlobalAudioBot) return;
            resolvingGlobalAudioBot = true;
            MessagesController.getInstance(currentAccount).getUserNameResolver().resolve(botUsername, botId -> {
                resolvingGlobalAudioBot = false;
                globalAudioBot = botId == null ? null : MessagesController.getInstance(currentAccount).getUser(botId);
                failedToResolveGlobalAudioBot = globalAudioBot == null;
                if (globalAudioBot != null) {
                    loadGlobalAudio();
                }
            });
            return;
        }

        loadingGlobalAudio = true;
        final TLRPC.User self = UserConfig.getInstance(currentAccount).getCurrentUser();
        final TLRPC.TL_messages_getInlineBotResults req = new TLRPC.TL_messages_getInlineBotResults();
        req.bot = MessagesController.getInstance(currentAccount).getInputUser(globalAudioBot);
        req.peer = MessagesController.getInputPeer(self);
        req.offset = globalAudio.isEmpty() || globalAudioOffset == null ? "" : globalAudioOffset;
        req.query = lastLoadingGlobalAudioQuery = query == null ? "" : query;
        loadingGlobalAudioRequestId = ConnectionsManager.getInstance(currentAccount).sendRequestTyped(req, AndroidUtilities::runOnUIThread, (res, err) -> {
            loadingGlobalAudio = false;
            willLoadGlobalAudio = false;
            if (res != null) {
                MessagesController.getInstance(currentAccount).putUsers(res.users, false);
                for (TLRPC.BotInlineResult r : res.results) {
                    if (r instanceof TLRPC.TL_botInlineMediaResult) {
                        final TLRPC.TL_botInlineMediaResult rMedia = (TLRPC.TL_botInlineMediaResult) r;
                        if (rMedia.document != null) {
                            final TLRPC.TL_message message = new TLRPC.TL_message();
                            message.out = true;
                            message.id = globalAudioId--;
                            message.peer_id = new TLRPC.TL_peerUser();
                            message.from_id = new TLRPC.TL_peerUser();
                            message.peer_id.user_id = message.from_id.user_id = UserConfig.getInstance(currentAccount).getClientUserId();
                            message.date = (int) (System.currentTimeMillis() / 1000);
                            message.message = "";
                            message.media = new TLRPC.TL_messageMediaDocument();
                            message.media.flags |= 3;
                            message.media.document = rMedia.document;
                            message.flags |= TLRPC.MESSAGE_FLAG_HAS_MEDIA | TLRPC.MESSAGE_FLAG_HAS_FROM_ID;
                            globalAudio.add(new MessageObject(currentAccount, message, false, true));
                        }
                    }
                }
                globalAudioOffset = res.next_offset;
                globalAudioHasMore = !globalAudio.isEmpty() && !TextUtils.isEmpty(globalAudioOffset);

                adapter.update(true);
            } else {
                adapter.update(true);
            }
        });

        adapter.update(true);
    }

    private boolean loadingLocalAudio;
    private void loadLocalAudio() {
        if (!local) return;
        if (loadingLocalAudio) return;
        loadingLocalAudio = true;
        Utilities.globalQueue.postRunnable(() -> {
            String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM
            };

            final ArrayList<MessageObject> newAudioEntries = new ArrayList<>();
            try (Cursor cursor = ApplicationLoader.applicationContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, MediaStore.Audio.Media.IS_MUSIC + " != 0", null, MediaStore.Audio.Media.TITLE)) {
                int id = -2000000000;
                while (cursor.moveToNext()) {
                    MediaController.AudioEntry audioEntry = new MediaController.AudioEntry();
                    audioEntry.id = cursor.getInt(0);
                    audioEntry.author = cursor.getString(1);
                    audioEntry.title = cursor.getString(2);
                    audioEntry.path = cursor.getString(3);
                    audioEntry.duration = (int) (cursor.getLong(4) / 1000);
                    audioEntry.genre = cursor.getString(5);

                    File file = new File(audioEntry.path);

                    TLRPC.TL_message message = new TLRPC.TL_message();
                    message.out = true;
                    message.id = id;
                    message.peer_id = new TLRPC.TL_peerUser();
                    message.from_id = new TLRPC.TL_peerUser();
                    message.peer_id.user_id = message.from_id.user_id = UserConfig.getInstance(currentAccount).getClientUserId();
                    message.date = (int) (System.currentTimeMillis() / 1000);
                    message.message = "";
                    message.attachPath = audioEntry.path;
                    message.media = new TLRPC.TL_messageMediaDocument();
                    message.media.flags |= 3;
                    message.media.document = new TLRPC.TL_document();
                    message.flags |= TLRPC.MESSAGE_FLAG_HAS_MEDIA | TLRPC.MESSAGE_FLAG_HAS_FROM_ID;

                    String ext = FileLoader.getFileExtension(file);

                    message.media.document.id = 0;
                    message.media.document.access_hash = 0;
                    message.media.document.file_reference = new byte[0];
                    message.media.document.date = message.date;
                    message.media.document.mime_type = "audio/" + (ext.length() > 0 ? ext : "mp3");
                    message.media.document.size = (int) file.length();
                    message.media.document.dc_id = 0;

                    TLRPC.TL_documentAttributeAudio attributeAudio = new TLRPC.TL_documentAttributeAudio();
                    attributeAudio.duration = audioEntry.duration;
                    attributeAudio.title = audioEntry.title;
                    attributeAudio.performer = audioEntry.author;
                    attributeAudio.flags |= 3;
                    message.media.document.attributes.add(attributeAudio);

                    TLRPC.TL_documentAttributeFilename fileName = new TLRPC.TL_documentAttributeFilename();
                    fileName.file_name = file.getName();
                    message.media.document.attributes.add(fileName);

                    audioEntry.messageObject = new MessageObject(currentAccount, message, false, true);

                    newAudioEntries.add(audioEntry.messageObject);
                    id--;
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            AndroidUtilities.runOnUIThread(() -> {
                loadingLocalAudio = false;
                localAudio.addAll(newAudioEntries);
                adapter.update(true);
            });
        });
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.musicListLoaded);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.musicListLoaded);
    }

    @Override
    public void onFailedDownload(String fileName, boolean canceled) {

    }

    @Override
    public void onSuccessDownload(String fileName) {
        if (downloadingMessageObject != null && TextUtils.equals(downloadingMessageObject.getFileName(), fileName)) {
            done(downloadingMessageObject);
        }
    }

    @Override
    public void onProgressDownload(String fileName, long downloadSize, long totalSize) {

    }

    @Override
    public void onProgressUpload(String fileName, long downloadSize, long totalSize, boolean isEncrypted) {

    }

    @Override
    public int getObserverTag() {
        return tag;
    }

}
