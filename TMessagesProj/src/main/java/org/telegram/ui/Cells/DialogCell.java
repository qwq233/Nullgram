/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ReplacementSpan;
import android.text.style.StyleSpan;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ChatThemeController;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Adapters.DialogsAdapter;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.CanvasButton;
import org.telegram.ui.Components.CheckBox2;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.EmptyStubSpan;
import org.telegram.ui.Components.ForegroundColorSpanThemable;
import org.telegram.ui.Components.Forum.ForumUtilities;
import org.telegram.ui.Components.Premium.PremiumGradient;
import org.telegram.ui.Components.PullForegroundDrawable;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble;
import org.telegram.ui.Components.StaticLayoutEx;
import org.telegram.ui.Components.StatusDrawable;
import org.telegram.ui.Components.SwipeGestureSettingsView;
import org.telegram.ui.Components.TextStyleSpan;
import org.telegram.ui.Components.TypefaceSpan;
import org.telegram.ui.Components.URLSpanNoUnderline;
import org.telegram.ui.Components.URLSpanNoUnderlineBold;
import org.telegram.ui.Components.spoilers.SpoilerEffect;
import org.telegram.ui.DialogsActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

import top.qwq2333.nullgram.config.ConfigManager;
import top.qwq2333.nullgram.utils.Defines;
import top.qwq2333.nullgram.utils.Log;


public class DialogCell extends BaseCell {

    public boolean drawingForBlur;
    boolean moving;
    private RLottieDrawable lastDrawTranslationDrawable;
    private int lastDrawSwipeMessageStringId;
    public boolean swipeCanceled;
    public static final int SENT_STATE_NOTHING = -1;
    public static final int SENT_STATE_PROGRESS = 0;
    public static final int SENT_STATE_SENT = 1;
    public static final int SENT_STATE_READ = 2;
    public boolean drawAvatar = true;
    public int messagePaddingStart = 72;
    public int heightDefault = 72;
    public int heightThreeLines = 78;
    public TLRPC.TL_forumTopic forumTopic;
    private boolean isTopic;
    private boolean twoLinesForName;
    private boolean nameIsEllipsized;
    private Paint topicCounterPaint;
    public float chekBoxPaddingTop = 42;
    private boolean needEmoji;
    private boolean hasNameInMessage;
    private TextPaint currentMessagePaint;
    private Paint buttonBackgroundPaint;
    CanvasButton canvasButton;
    DialogCellDelegate delegate;
    private boolean applyName;
    private boolean lastTopicMessageUnread;

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public boolean isMoving() {
        return moving;
    }

    public void setForumTopic(TLRPC.TL_forumTopic topic, long dialog_id, MessageObject messageObject, boolean animated) {
        forumTopic = topic;
        isTopic = forumTopic != null;
        if (currentDialogId != dialog_id) {
            lastStatusDrawableParams = -1;
        }
        if (messageObject.topicIconDrawable[0] != null) {
            messageObject.topicIconDrawable[0].setColor(topic.icon_color);
        }
        currentDialogId = dialog_id;
        lastDialogChangedTime = System.currentTimeMillis();
        message = messageObject;
        isDialogCell = false;
        if (messageObject != null) {
            lastMessageDate = messageObject.messageOwner.date;
            currentEditDate = messageObject != null ? messageObject.messageOwner.edit_date : 0;
            markUnread = false;
            messageId = messageObject != null ? messageObject.getId() : 0;
            lastUnreadState = messageObject != null && messageObject.isUnread();
        }
        if (message != null) {
            lastSendState = message.messageOwner.send_state;
        }
        if (!animated) {
            lastStatusDrawableParams = -1;
        }
        if (topic != null) {
            groupMessages = topic.groupedMessages;
        }
        update(0, animated);
    }

    public static class FixedWidthSpan extends ReplacementSpan {

        private final int width;

        public FixedWidthSpan(int w) {
            width = w;
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            if (fm == null) {
                fm = paint.getFontMetricsInt();
            }
            if (fm != null) {
                int h = fm.descent - fm.ascent;
                fm.bottom = fm.descent = 1 - h;
                fm.top = fm.ascent = -1;
            }
            return width;
        }

        @Override
        public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {

        }
    }

    public static class CustomDialog {
        public String name;
        public String message;
        public int id;
        public int unread_count;
        public boolean pinned;
        public boolean muted;
        public int type;
        public int date;
        public boolean verified;
        public boolean isMedia;
        public int sent = -1;
    }

    private int paintIndex;

    private final int currentAccount;
    private CustomDialog customDialog;
    private long currentDialogId;
    private int currentDialogFolderId;
    private int currentDialogFolderDialogsCount;
    private int currentEditDate;
    private boolean isDialogCell;
    private int lastMessageDate;
    private int unreadCount;
    private boolean markUnread;
    private int mentionCount;
    private int reactionMentionCount;
    private boolean lastUnreadState;
    private int lastSendState;
    private boolean dialogMuted;
    private boolean topicMuted;
    private boolean drawUnmute;
    private float dialogMutedProgress;
    private boolean hasUnmutedTopics = false;
    private MessageObject message;
    private boolean isForum;
    private ArrayList<MessageObject> groupMessages;
    private boolean clearingDialog;
    private CharSequence lastMessageString;
    private int index;
    private int dialogsType;
    private int folderId;
    private int messageId;
    private boolean archiveHidden;
    protected boolean forbidVerified;
    protected boolean forbidDraft;

    private float cornerProgress;
    private long lastUpdateTime;
    private float onlineProgress;
    private float chatCallProgress;
    private float innerProgress;
    private int progressStage;

    private float clipProgress;
    private int topClip;
    private int bottomClip;
    private float translationX;
    private boolean isSliding;
    private RLottieDrawable translationDrawable;
    private boolean translationAnimationStarted;
    private boolean drawRevealBackground;
    private float currentRevealProgress;
    private float currentRevealBounceProgress;
    private float archiveBackgroundProgress;

    private int thumbsCount;
    private boolean hasVideoThumb;
    private ImageReceiver[] thumbImage = new ImageReceiver[3];
    private boolean[] drawPlay = new boolean[3];

    public ImageReceiver avatarImage = new ImageReceiver(this);
    private AvatarDrawable avatarDrawable = new AvatarDrawable();
    private boolean animatingArchiveAvatar;
    private float animatingArchiveAvatarProgress;
    private final BounceInterpolator interpolator = new BounceInterpolator();
    private PullForegroundDrawable archivedChatsDrawable;

    private TLRPC.User user;
    private TLRPC.Chat chat;
    private TLRPC.EncryptedChat encryptedChat;
    private CharSequence lastPrintString;
    private int printingStringType;
    private TLRPC.DraftMessage draftMessage;

    private CheckBox2 checkBox;

    public boolean useForceThreeLines;
    public boolean useSeparator;
    public boolean fullSeparator;
    public boolean fullSeparator2;

    private boolean useMeForMyMessages;

    private boolean hasCall;

    private int nameLeft;
    private int nameWidth;
    private StaticLayout nameLayout;
    private boolean nameLayoutFits;
    private float nameLayoutTranslateX;
    private boolean nameLayoutEllipsizeLeft;
    private boolean nameLayoutEllipsizeByGradient;
    private Paint fadePaint;
    private Paint fadePaintBack;
    private boolean drawNameLock;
    private int nameMuteLeft;
    private int nameLockLeft;
    private int nameLockTop;

    private int timeLeft;
    private int timeTop;
    private StaticLayout timeLayout;

    private int lock2Left;

    private boolean promoDialog;

    private boolean drawCheck1;
    private boolean drawCheck2;
    private boolean drawClock;
    private int checkDrawLeft;
    private int checkDrawLeft1;
    private int clockDrawLeft;
    private int checkDrawTop;
    private int halfCheckDrawLeft;

    private int messageTop;
    private int messageLeft;
    private StaticLayout messageLayout;

    private int buttonTop;
    private StaticLayout buttonLayout;

    private Stack<SpoilerEffect> spoilersPool = new Stack<>();
    private List<SpoilerEffect> spoilers = new ArrayList<>();
    private Stack<SpoilerEffect> spoilersPool2 = new Stack<>();
    private List<SpoilerEffect> spoilers2 = new ArrayList<>();
    private AnimatedEmojiSpan.EmojiGroupedSpans animatedEmojiStack, animatedEmojiStack2, animatedEmojiStack3;

    private int messageNameTop;
    private int messageNameLeft;
    private StaticLayout messageNameLayout;

    private boolean drawError;
    private int errorTop;
    private int errorLeft;

    private boolean attachedToWindow;

    private float reorderIconProgress;
    private boolean drawReorder;
    private boolean drawPinBackground;
    private boolean drawPin;
    private boolean drawPinForced;
    private int pinTop;
    private int pinLeft;
    protected int translateY;

    private boolean drawCount;
    private boolean drawCount2 = true;
    private int countTop;
    private int countLeft;
    private int countWidth;
    private int countWidthOld;
    private int countLeftOld;
    private boolean countAnimationIncrement;
    private ValueAnimator countAnimator;
    private ValueAnimator reactionsMentionsAnimator;
    private float countChangeProgress = 1f;
    private float reactionsMentionsChangeProgress = 1f;
    private StaticLayout countLayout;
    private StaticLayout countOldLayout;
    private StaticLayout countAnimationStableLayout;
    private StaticLayout countAnimationInLayout;

    private boolean drawMention;
    private boolean drawReactionMention;
    private int mentionLeft;
    private int reactionMentionLeft;
    private int mentionWidth;
    private StaticLayout mentionLayout;

    private boolean drawVerified;
    private boolean drawPremium;
    private AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable emojiStatus;

    private int drawScam;

    private boolean isSelected;

    private final RectF rect = new RectF();
    private DialogsAdapter.DialogsPreloader preloader;

    private int animateToStatusDrawableParams;
    private int animateFromStatusDrawableParams;
    private int lastStatusDrawableParams = -1;
    private float statusDrawableProgress;
    private boolean statusDrawableAnimationInProgress;
    private ValueAnimator statusDrawableAnimator;
    long lastDialogChangedTime;
    private int statusDrawableLeft;

    private final DialogsActivity parentFragment;

    private StaticLayout swipeMessageTextLayout;
    private int swipeMessageTextId;
    private int swipeMessageWidth;
    private int readOutboxMaxId = -1;

    public static class BounceInterpolator implements Interpolator {

        public float getInterpolation(float t) {
            if (t < 0.33f) {
                return 0.1f * (t / 0.33f);
            } else {
                t -= 0.33f;
                if (t < 0.33f) {
                    return 0.1f - 0.15f * (t / 0.34f);
                } else {
                    t -= 0.34f;
                    return -0.05f + 0.05f * (t / 0.33f);
                }
            }
        }
    }

    public DialogCell(DialogsActivity fragment, Context context, boolean needCheck, boolean forceThreeLines) {
        this(fragment, context, needCheck, forceThreeLines, UserConfig.selectedAccount, null);
    }

    private final Theme.ResourcesProvider resourcesProvider;

    public DialogCell(DialogsActivity fragment, Context context, boolean needCheck, boolean forceThreeLines, int account, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        parentFragment = fragment;
        Theme.createDialogsResources(context);
        avatarImage.setRoundRadius(AndroidUtilities.dp(28));
        for (int i = 0; i < thumbImage.length; ++i) {
            thumbImage[i] = new ImageReceiver(this);
            thumbImage[i].setRoundRadius(AndroidUtilities.dp(2));
        }
        useForceThreeLines = forceThreeLines;
        currentAccount = account;


        emojiStatus = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(this, AndroidUtilities.dp(22));
        emojiStatus.center = false;
    }

    public void setDialog(TLRPC.Dialog dialog, int type, int folder) {
        if (currentDialogId != dialog.id) {
            if (statusDrawableAnimator != null) {
                statusDrawableAnimator.removeAllListeners();
                statusDrawableAnimator.cancel();
            }
            statusDrawableAnimationInProgress = false;
            lastStatusDrawableParams = -1;
        }
        currentDialogId = dialog.id;
        lastDialogChangedTime = System.currentTimeMillis();
        isDialogCell = true;
        if (dialog instanceof TLRPC.TL_dialogFolder) {
            TLRPC.TL_dialogFolder dialogFolder = (TLRPC.TL_dialogFolder) dialog;
            currentDialogFolderId = dialogFolder.folder.id;
            if (archivedChatsDrawable != null) {
                archivedChatsDrawable.setCell(this);
            }
        } else {
            currentDialogFolderId = 0;
        }
        dialogsType = type;
        folderId = folder;
        messageId = 0;
        update(0, false);
        checkOnline();
        checkGroupCall();
        checkChatTheme();
    }

    protected boolean drawLock2() {
        return false;
    }

    public void setDialogIndex(int i) {
        index = i;
    }

    public void setDialog(CustomDialog dialog) {
        customDialog = dialog;
        messageId = 0;
        update(0);
        checkOnline();
        checkGroupCall();
        checkChatTheme();
    }

    private void checkOnline() {
        if (user != null) {
            TLRPC.User newUser = MessagesController.getInstance(currentAccount).getUser(user.id);
            if (newUser != null) {
                user = newUser;
            }
        }
        boolean isOnline = isOnline();
        onlineProgress = isOnline ? 1.0f : 0.0f;
    }

    private boolean isOnline() {
        if (isForumCell()) {
            return false;
        }
        if (user == null || user.self) {
            return false;
        }
        if (user.status != null && user.status.expires <= 0) {
            if (MessagesController.getInstance(currentAccount).onlinePrivacy.containsKey(user.id)) {
                return true;
            }
        }
        return user.status != null && user.status.expires > ConnectionsManager.getInstance(
            currentAccount).getCurrentTime();
    }

    private void checkGroupCall() {
        hasCall = chat != null && chat.call_active && chat.call_not_empty;
        chatCallProgress = hasCall ? 1.0f : 0.0f;
    }

    private void checkChatTheme() {
        if (message != null && message.messageOwner != null && message.messageOwner.action instanceof TLRPC.TL_messageActionSetChatTheme && lastUnreadState) {
            TLRPC.TL_messageActionSetChatTheme setThemeAction = (TLRPC.TL_messageActionSetChatTheme) message.messageOwner.action;
            ChatThemeController.getInstance(currentAccount).setDialogTheme(currentDialogId, setThemeAction.emoticon, false);
        }
    }

    public void setDialog(long dialog_id, MessageObject messageObject, int date, boolean useMe, boolean animated) {
        if (currentDialogId != dialog_id) {
            lastStatusDrawableParams = -1;
        }
        currentDialogId = dialog_id;
        lastDialogChangedTime = System.currentTimeMillis();
        message = messageObject;
        useMeForMyMessages = useMe;
        isDialogCell = false;
        lastMessageDate = date;
        currentEditDate = messageObject != null ? messageObject.messageOwner.edit_date : 0;
        unreadCount = 0;
        markUnread = false;
        messageId = messageObject != null ? messageObject.getId() : 0;
        mentionCount = 0;
        reactionMentionCount = 0;
        lastUnreadState = messageObject != null && messageObject.isUnread();
        if (message != null) {
            lastSendState = message.messageOwner.send_state;
        }
        update(0, animated);
    }

    public long getDialogId() {
        return currentDialogId;
    }

    public int getDialogIndex() {
        return index;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setPreloader(DialogsAdapter.DialogsPreloader preloader) {
        this.preloader = preloader;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isSliding = false;
        drawRevealBackground = false;
        currentRevealProgress = 0.0f;
        attachedToWindow = false;
        reorderIconProgress = getIsPinned() && drawReorder ? 1.0f : 0.0f;
        avatarImage.onDetachedFromWindow();
        for (int i = 0; i < thumbImage.length; ++i) {
            thumbImage[i].onDetachedFromWindow();
        }
        if (translationDrawable != null) {
            translationDrawable.stop();
            translationDrawable.setProgress(0.0f);
            translationDrawable.setCallback(null);
            translationDrawable = null;
            translationAnimationStarted = false;
        }
        if (preloader != null) {
            preloader.remove(currentDialogId);
        }
        if (emojiStatus != null) {
            emojiStatus.detach();
        }
        AnimatedEmojiSpan.release(this, animatedEmojiStack);
        AnimatedEmojiSpan.release(this, animatedEmojiStack2);
        AnimatedEmojiSpan.release(this, animatedEmojiStack3);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        avatarImage.onAttachedToWindow();
        for (int i = 0; i < thumbImage.length; ++i) {
            thumbImage[i].onAttachedToWindow();
        }
        resetPinnedArchiveState();
        animatedEmojiStack = AnimatedEmojiSpan.update(AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES, this, animatedEmojiStack, messageLayout);
        animatedEmojiStack2 = AnimatedEmojiSpan.update(AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES, this, animatedEmojiStack2, messageNameLayout);
        animatedEmojiStack3 = AnimatedEmojiSpan.update(AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES, this, animatedEmojiStack3, buttonLayout);
    }

    public void resetPinnedArchiveState() {
        archiveHidden = SharedConfig.archiveHidden;
        archiveBackgroundProgress = archiveHidden ? 0.0f : 1.0f;
        avatarDrawable.setArchivedAvatarHiddenProgress(archiveBackgroundProgress);
        clipProgress = 0.0f;
        isSliding = false;
        reorderIconProgress = getIsPinned() && drawReorder ? 1.0f : 0.0f;
        attachedToWindow = true;
        cornerProgress = 0.0f;
        setTranslationX(0);
        setTranslationY(0);
        if (emojiStatus != null) {
            emojiStatus.attach();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (checkBox != null) {
            checkBox.measure(
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(24), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(24), MeasureSpec.EXACTLY)
            );
        }
        if (isTopic) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? heightThreeLines : heightDefault) + (useSeparator ? 1 : 0));
            checkTwoLinesForName();
        }
        if (isForumCell()) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 86 : 91 + (useSeparator ? 1 : 0)));
        } else {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? heightThreeLines : heightDefault) + (useSeparator ? 1 : 0) + (twoLinesForName ? AndroidUtilities.dp(20) : 0));
        }
        topClip = 0;
        bottomClip = getMeasuredHeight();
    }

    private void checkTwoLinesForName() {
        twoLinesForName = false;
        if (isTopic) {
            buildLayout();
            if (nameIsEllipsized) {
                twoLinesForName = true;
                buildLayout();
            }
        }
    }

    int lastSize;
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (currentDialogId == 0 && customDialog == null) {
            return;
        }
        if (checkBox != null) {
            int paddingStart = AndroidUtilities.dp(messagePaddingStart - (useForceThreeLines || SharedConfig.useThreeLinesLayout ? 29 : 27));
            int x = LocaleController.isRTL ? (right - left) - paddingStart : paddingStart;
            int y = AndroidUtilities.dp(chekBoxPaddingTop + (useForceThreeLines || SharedConfig.useThreeLinesLayout ? 6 : 0));
            checkBox.layout(x, y, x + checkBox.getMeasuredWidth(), y + checkBox.getMeasuredHeight());
        }
        int size = getMeasuredHeight() + getMeasuredWidth() << 16;
        if (size != lastSize) {
            lastSize = size;
            try {
                buildLayout();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    public boolean isUnread() {
        return (unreadCount != 0 || markUnread) && !dialogMuted;
    }

    public boolean getHasUnread() {
        return (unreadCount != 0 || markUnread);
    }

    public boolean getIsMuted() {
        return dialogMuted;
    }

    public boolean getIsPinned() {
        return drawPin || drawPinForced;
    }

    public void setPinForced(boolean value) {
        drawPinForced = value;
        if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
            buildLayout();
        }
        invalidate();
    }

    private CharSequence formatArchivedDialogNames() {
        ArrayList<TLRPC.Dialog> dialogs = MessagesController.getInstance(currentAccount).getDialogs(currentDialogFolderId);
        currentDialogFolderDialogsCount = dialogs.size();
        SpannableStringBuilder builder = new SpannableStringBuilder();
        for (int a = 0, N = dialogs.size(); a < N; a++) {
            TLRPC.Dialog dialog = dialogs.get(a);
            TLRPC.User currentUser = null;
            TLRPC.Chat currentChat = null;
            if (DialogObject.isEncryptedDialog(dialog.id)) {
                TLRPC.EncryptedChat encryptedChat = MessagesController.getInstance(currentAccount).getEncryptedChat(DialogObject.getEncryptedChatId(dialog.id));
                if (encryptedChat != null) {
                    currentUser = MessagesController.getInstance(currentAccount).getUser(encryptedChat.user_id);
                }
            } else {
                if (DialogObject.isUserDialog(dialog.id)) {
                    currentUser = MessagesController.getInstance(currentAccount).getUser(dialog.id);
                } else {
                    currentChat = MessagesController.getInstance(currentAccount).getChat(-dialog.id);
                }
            }
            String title;
            if (currentChat != null) {
                title = currentChat.title.replace('\n', ' ');
            } else if (currentUser != null) {
                if (UserObject.isDeleted(currentUser)) {
                    title = LocaleController.getString("HiddenName", R.string.HiddenName);
                } else {
                    title = ContactsController.formatName(currentUser.first_name, currentUser.last_name).replace('\n', ' ');
                }
            } else {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            int boldStart = builder.length();
            int boldEnd = boldStart + title.length();
            builder.append(title);
            if (dialog.unread_count > 0) {
                builder.setSpan(new TypefaceSpan(AndroidUtilities.getTypeface("fonts/rmedium.ttf"), 0, Theme.getColor(Theme.key_chats_nameArchived, resourcesProvider)), boldStart, boldEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (builder.length() > 150) {
                break;
            }
        }
        return Emoji.replaceEmoji(builder, Theme.dialogs_messagePaint[paintIndex].getFontMetricsInt(), AndroidUtilities.dp(17), false);
    }

    int thumbSize;

    public void buildLayout() {
        if (useForceThreeLines || SharedConfig.useThreeLinesLayout) {
            Theme.dialogs_namePaint[1].setTextSize(AndroidUtilities.dp(16));
            Theme.dialogs_nameEncryptedPaint[1].setTextSize(AndroidUtilities.dp(16));
            Theme.dialogs_messagePaint[1].setTextSize(AndroidUtilities.dp(15));
            Theme.dialogs_messagePrintingPaint[1].setTextSize(AndroidUtilities.dp(15));

            Theme.dialogs_messagePaint[1].setColor(Theme.dialogs_messagePaint[1].linkColor = Theme.getColor(Theme.key_chats_message_threeLines, resourcesProvider));
            paintIndex = 1;
            thumbSize = 18;
        } else {
            Theme.dialogs_namePaint[0].setTextSize(AndroidUtilities.dp(17));
            Theme.dialogs_nameEncryptedPaint[0].setTextSize(AndroidUtilities.dp(17));
            Theme.dialogs_messagePaint[0].setTextSize(AndroidUtilities.dp(16));
            Theme.dialogs_messagePrintingPaint[0].setTextSize(AndroidUtilities.dp(16));

            Theme.dialogs_messagePaint[0].setColor(Theme.dialogs_messagePaint[0].linkColor = Theme.getColor(Theme.key_chats_message, resourcesProvider));
            paintIndex = 0;
            thumbSize = 19;
        }

        currentDialogFolderDialogsCount = 0;
        String nameString = "";
        String timeString = "";
        String countString = null;
        String mentionString = null;
        CharSequence messageString = "";
        CharSequence messageNameString = null;
        CharSequence printingString = null;
        CharSequence buttonString = null;
        if (!isForumCell() && (isDialogCell || isTopic)) {
            printingString = MessagesController.getInstance(currentAccount).getPrintingString(currentDialogId, getTopicId(), true);
        }
        currentMessagePaint = Theme.dialogs_messagePaint[paintIndex];
        boolean checkMessage = true;

        drawNameLock = false;
        drawVerified = false;
        drawPremium = false;
        drawScam = 0;
        drawPinBackground = false;
        thumbsCount = 0;
        hasVideoThumb = false;
        nameLayoutEllipsizeByGradient = false;
        int offsetName = 0;
        boolean showChecks = !UserObject.isUserSelf(user) && !useMeForMyMessages;
        boolean drawTime = true;
        printingStringType = -1;
        int printingStringReplaceIndex = -1;
        if (!isForumCell()) {
            buttonLayout = null;
        }

        String messageFormat;
        if (Build.VERSION.SDK_INT >= 18) {
            if ((!useForceThreeLines && !SharedConfig.useThreeLinesLayout || currentDialogFolderId != 0) || isForumCell()) {
                messageFormat = "%2$s: \u2068%1$s\u2069";
                hasNameInMessage = true;
            } else {
                messageFormat = "\u2068%1$s\u2069";
                hasNameInMessage = false;
            }
        } else {
            if ((!useForceThreeLines && !SharedConfig.useThreeLinesLayout || currentDialogFolderId != 0) || isForumCell()) {
                messageFormat = "%2$s: %1$s";
                hasNameInMessage = true;
            } else {
                messageFormat = "%1$s";
                hasNameInMessage = false;
            }
        }

        CharSequence msgText = message != null ? message.messageText : null;
        if (msgText instanceof Spannable) {
            Spannable sp = new SpannableStringBuilder(msgText);
            for (Object span : sp.getSpans(0, sp.length(), URLSpanNoUnderlineBold.class))
                sp.removeSpan(span);
            for (Object span : sp.getSpans(0, sp.length(), URLSpanNoUnderline.class))
                sp.removeSpan(span);
            msgText = sp;
        }
        lastMessageString = msgText;

        if (customDialog != null) {
            if (customDialog.type == 2) {
                drawNameLock = true;
                if (useForceThreeLines || SharedConfig.useThreeLinesLayout) {
                    nameLockTop = AndroidUtilities.dp(12.5f);
                    if (!LocaleController.isRTL) {
                        nameLockLeft = AndroidUtilities.dp(messagePaddingStart + 6);
                        nameLeft = AndroidUtilities.dp(messagePaddingStart + 10) + Theme.dialogs_lockDrawable.getIntrinsicWidth();
                    } else {
                        nameLockLeft = getMeasuredWidth() - AndroidUtilities.dp(messagePaddingStart + 6) - Theme.dialogs_lockDrawable.getIntrinsicWidth();
                        nameLeft = AndroidUtilities.dp(22);
                    }
                } else {
                    nameLockTop = AndroidUtilities.dp(16.5f);
                    if (!LocaleController.isRTL) {
                        nameLockLeft = AndroidUtilities.dp(messagePaddingStart + 4);
                        nameLeft = AndroidUtilities.dp(messagePaddingStart + 8) + Theme.dialogs_lockDrawable.getIntrinsicWidth();
                    } else {
                        nameLockLeft = getMeasuredWidth() - AndroidUtilities.dp(messagePaddingStart + 4) - Theme.dialogs_lockDrawable.getIntrinsicWidth();
                        nameLeft = AndroidUtilities.dp(18);
                    }
                }
            } else {
                drawVerified = !forbidVerified && customDialog.verified;
                if (useForceThreeLines || SharedConfig.useThreeLinesLayout) {
                    if (!LocaleController.isRTL) {
                        nameLeft = AndroidUtilities.dp(messagePaddingStart + 6);
                    } else {
                        nameLeft = AndroidUtilities.dp(22);
                    }
                } else {
                    if (!LocaleController.isRTL) {
                        nameLeft = AndroidUtilities.dp(messagePaddingStart + 4);
                    } else {
                        nameLeft = AndroidUtilities.dp(18);
                    }
                }
            }

            if (customDialog.type == 1) {
                messageNameString = LocaleController.getString("FromYou", R.string.FromYou);
                checkMessage = false;
                SpannableStringBuilder stringBuilder;
                if (customDialog.isMedia) {
                    currentMessagePaint = Theme.dialogs_messagePrintingPaint[paintIndex];
                    stringBuilder = SpannableStringBuilder.valueOf(String.format(messageFormat, message.messageText));
                    stringBuilder.setSpan(new ForegroundColorSpanThemable(Theme.key_chats_attachMessage, resourcesProvider), 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    String mess = customDialog.message;
                    if (mess.length() > 150) {
                        mess = mess.substring(0, 150);
                    }
                    if (useForceThreeLines || SharedConfig.useThreeLinesLayout) {
                        stringBuilder = SpannableStringBuilder.valueOf(String.format(messageFormat, mess, messageNameString));
                    } else {
                        stringBuilder = SpannableStringBuilder.valueOf(String.format(messageFormat, mess.replace('\n', ' '), messageNameString));
                    }
                }
                messageString = Emoji.replaceEmoji(stringBuilder, Theme.dialogs_messagePaint[paintIndex].getFontMetricsInt(), AndroidUtilities.dp(20), false);
            } else {
                messageString = customDialog.message;
                if (customDialog.isMedia) {
                    currentMessagePaint = Theme.dialogs_messagePrintingPaint[paintIndex];
                }
            }

            timeString = LocaleController.stringForMessageListDate(customDialog.date);

            if (customDialog.unread_count != 0) {
                drawCount = true;
                countString = String.format("%d", customDialog.unread_count);
            } else {
                drawCount = false;
            }

            if (customDialog.sent == SENT_STATE_PROGRESS) {
                drawClock = true;
                drawCheck1 = false;
                drawCheck2 = false;
            } else if (customDialog.sent == SENT_STATE_READ) {
                drawCheck1 = true;
                drawCheck2 = true;
                drawClock = false;
            } else if (customDialog.sent == SENT_STATE_SENT) {
                drawCheck1 = false;
                drawCheck2 = true;
                drawClock = false;
            } else {
                drawClock = false;
                drawCheck1 = false;
                drawCheck2 = false;
            }

            drawError = false;
            nameString = customDialog.name;
        } else {
            if (useForceThreeLines || SharedConfig.useThreeLinesLayout) {
                if (!LocaleController.isRTL) {
                    nameLeft = AndroidUtilities.dp(messagePaddingStart + 6);
                } else {
                    nameLeft = AndroidUtilities.dp(22);
                }
            } else {
                if (!LocaleController.isRTL) {
                    nameLeft = AndroidUtilities.dp(messagePaddingStart + 4);
                } else {
                    nameLeft = AndroidUtilities.dp(18);
                }
            }

            if (encryptedChat != null) {
                if (currentDialogFolderId == 0) {
                    drawNameLock = true;
                    if (useForceThreeLines || SharedConfig.useThreeLinesLayout) {
                        nameLockTop = AndroidUtilities.dp(12.5f);
                        if (!LocaleController.isRTL) {
                            nameLockLeft = AndroidUtilities.dp(messagePaddingStart + 6);
                            nameLeft = AndroidUtilities.dp(messagePaddingStart + 10) + Theme.dialogs_lockDrawable.getIntrinsicWidth();
                        } else {
                            nameLockLeft = getMeasuredWidth() - AndroidUtilities.dp(messagePaddingStart + 6) - Theme.dialogs_lockDrawable.getIntrinsicWidth();
                            nameLeft = AndroidUtilities.dp(22);
                        }
                    } else {
                        nameLockTop = AndroidUtilities.dp(16.5f);
                        if (!LocaleController.isRTL) {
                            nameLockLeft = AndroidUtilities.dp(messagePaddingStart + 4);
                            nameLeft = AndroidUtilities.dp(messagePaddingStart + 8) + Theme.dialogs_lockDrawable.getIntrinsicWidth();
                        } else {
                            nameLockLeft = getMeasuredWidth() - AndroidUtilities.dp(messagePaddingStart + 4) - Theme.dialogs_lockDrawable.getIntrinsicWidth();
                            nameLeft = AndroidUtilities.dp(18);
                        }
                    }
                }
            } else {
                if (currentDialogFolderId == 0) {
                    if (chat != null) {
                        if (chat.scam) {
                            drawScam = 1;
                            Theme.dialogs_scamDrawable.checkText();
                        } else if (chat.fake) {
                            drawScam = 2;
                            Theme.dialogs_fakeDrawable.checkText();
                        } else {
                            drawVerified = !forbidVerified && chat.verified;
                        }
                    } else if (user != null) {
                        if (user.scam) {
                            drawScam = 1;
                            Theme.dialogs_scamDrawable.checkText();
                        } else if (user.fake) {
                            drawScam = 2;
                            Theme.dialogs_fakeDrawable.checkText();
                        } else {
                            drawVerified =!forbidVerified && user.verifiedExtended();
                        }
                        drawPremium = MessagesController.getInstance(currentAccount).isPremiumUser(user) && UserConfig.getInstance(currentAccount).clientUserId != user.id && user.id != 0;
                        if (drawPremium) {
                            if (user.emoji_status instanceof TLRPC.TL_emojiStatusUntil && ((TLRPC.TL_emojiStatusUntil) user.emoji_status).until > (int) (System.currentTimeMillis() / 1000)) {
                                nameLayoutEllipsizeByGradient = true;
                                emojiStatus.set(((TLRPC.TL_emojiStatusUntil) user.emoji_status).document_id, false);
                            } else if (user.emoji_status instanceof TLRPC.TL_emojiStatus) {
                                nameLayoutEllipsizeByGradient = true;
                                emojiStatus.set(((TLRPC.TL_emojiStatus) user.emoji_status).document_id, false);
                            } else {
                                nameLayoutEllipsizeByGradient = true;
                                emojiStatus.set(PremiumGradient.getInstance().premiumStarDrawableMini, false);
                            }
                        }
                    }
                }
            }

            int lastDate = lastMessageDate;
            if (lastMessageDate == 0 && message != null) {
                lastDate = message.messageOwner.date;
            }

            if (isTopic) {
                draftMessage = MediaDataController.getInstance(currentAccount).getDraft(currentDialogId, getTopicId());
                if (draftMessage != null && TextUtils.isEmpty(draftMessage.message)) {
                    draftMessage = null;
                }
            } else if (isDialogCell) {
                draftMessage = MediaDataController.getInstance(currentAccount).getDraft(currentDialogId, 0);
            } else {
                draftMessage = null;
            }

            if (draftMessage != null && (TextUtils.isEmpty(draftMessage.message) && draftMessage.reply_to_msg_id == 0 || lastDate > draftMessage.date && unreadCount != 0) ||
                    ChatObject.isChannel(chat) && !chat.megagroup && !chat.creator && (chat.admin_rights == null || !chat.admin_rights.post_messages) ||
                    chat != null && (chat.left || chat.kicked) || forbidDraft || ChatObject.isForum(chat) && !isTopic) {
                draftMessage = null;
            }

            if (isForumCell()) {
                draftMessage = null;
                needEmoji = true;
                updateMessageThumbs();
                messageNameString = getMessageNameString();
                messageString = formatTopicsNames();
                String restrictionReason = message != null ? MessagesController.getRestrictionReason(message.messageOwner.restriction_reason) : null;
                buttonString = message != null ? getMessageStringFormatted(messageFormat, restrictionReason, messageNameString, true) : "";
                if (applyName && buttonString.length() >= 0 && messageNameString != null) {
                    SpannableStringBuilder spannableStringBuilder = SpannableStringBuilder.valueOf(buttonString);
                    spannableStringBuilder.setSpan(new ForegroundColorSpanThemable(Theme.key_chats_name, resourcesProvider), 0, Math.min(spannableStringBuilder.length(), messageNameString.length() + 1), 0);
                    buttonString = spannableStringBuilder;
                }
                currentMessagePaint = Theme.dialogs_messagePaint[paintIndex];
            } else if (printingString != null) {
                lastPrintString = printingString;
                printingStringType = MessagesController.getInstance(currentAccount).getPrintingStringType(currentDialogId, getTopicId());
                StatusDrawable statusDrawable = Theme.getChatStatusDrawable(printingStringType);
                int startPadding = 0;
                if (statusDrawable != null) {
                    startPadding = statusDrawable.getIntrinsicWidth() + AndroidUtilities.dp(3);
                }
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();

                printingString = TextUtils.replace(printingString, new String[]{"..."}, new String[]{""});
                if (printingStringType == 5) {
                    printingStringReplaceIndex = printingString.toString().indexOf("**oo**");
                }
                if (printingStringReplaceIndex >= 0) {
                    spannableStringBuilder.append(printingString).setSpan(new FixedWidthSpan(Theme.getChatStatusDrawable(printingStringType).getIntrinsicWidth()), printingStringReplaceIndex, printingStringReplaceIndex + 6, 0);
                } else {
                    spannableStringBuilder.append(" ").append(printingString).setSpan(new FixedWidthSpan(startPadding), 0, 1, 0);
                }

                messageString = spannableStringBuilder;
                currentMessagePaint = Theme.dialogs_messagePrintingPaint[paintIndex];
                checkMessage = false;
            } else {
                lastPrintString = null;
                if (draftMessage != null) {
                    checkMessage = false;
                    messageNameString = LocaleController.getString("Draft", R.string.Draft);
                    if (TextUtils.isEmpty(draftMessage.message)) {
                        if (useForceThreeLines || SharedConfig.useThreeLinesLayout) {
                            messageString = "";
                        } else {
                            SpannableStringBuilder stringBuilder = SpannableStringBuilder.valueOf(messageNameString);
                            stringBuilder.setSpan(new ForegroundColorSpanThemable(Theme.key_chats_draft, resourcesProvider), 0, messageNameString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            messageString = stringBuilder;
                        }
                    } else {
                        String mess = draftMessage.message;
                        if (mess.length() > 150) {
                            mess = mess.substring(0, 150);
                        }
                        Spannable messSpan = new SpannableStringBuilder(mess);
                        MediaDataController.addTextStyleRuns(draftMessage, messSpan, TextStyleSpan.FLAG_STYLE_SPOILER);
                        if (draftMessage != null && draftMessage.entities != null) {
                            MediaDataController.addAnimatedEmojiSpans(draftMessage.entities, messSpan, currentMessagePaint == null ? null : currentMessagePaint.getFontMetricsInt());
                        }

                        SpannableStringBuilder stringBuilder = AndroidUtilities.formatSpannable(messageFormat, AndroidUtilities.replaceNewLines(messSpan), messageNameString);
                        if (!useForceThreeLines && !SharedConfig.useThreeLinesLayout) {
                            stringBuilder.setSpan(new ForegroundColorSpanThemable(Theme.key_chats_draft, resourcesProvider), 0, messageNameString.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        messageString = Emoji.replaceEmoji(stringBuilder, Theme.dialogs_messagePaint[paintIndex].getFontMetricsInt(), AndroidUtilities.dp(20), false);
                    }
                } else {
                    if (clearingDialog) {
                        currentMessagePaint = Theme.dialogs_messagePrintingPaint[paintIndex];
                        messageString = LocaleController.getString("HistoryCleared", R.string.HistoryCleared);
                    } else if (message == null) {
                        if (encryptedChat != null) {
                            currentMessagePaint = Theme.dialogs_messagePrintingPaint[paintIndex];
                            if (encryptedChat instanceof TLRPC.TL_encryptedChatRequested) {
                                messageString = LocaleController.getString("EncryptionProcessing", R.string.EncryptionProcessing);
                            } else if (encryptedChat instanceof TLRPC.TL_encryptedChatWaiting) {
                                messageString = LocaleController.formatString("AwaitingEncryption", R.string.AwaitingEncryption, UserObject.getFirstName(user));
                            } else if (encryptedChat instanceof TLRPC.TL_encryptedChatDiscarded) {
                                messageString = LocaleController.getString("EncryptionRejected", R.string.EncryptionRejected);
                            } else if (encryptedChat instanceof TLRPC.TL_encryptedChat) {
                                if (encryptedChat.admin_id == UserConfig.getInstance(currentAccount).getClientUserId()) {
                                    messageString = LocaleController.formatString("EncryptedChatStartedOutgoing", R.string.EncryptedChatStartedOutgoing, UserObject.getFirstName(user));
                                } else {
                                    messageString = LocaleController.getString("EncryptedChatStartedIncoming", R.string.EncryptedChatStartedIncoming);
                                }
                            }
                        } else {
                            if (dialogsType == 3 && UserObject.isUserSelf(user)) {
                                messageString = LocaleController.getString("SavedMessagesInfo", R.string.SavedMessagesInfo);
                                showChecks = false;
                                drawTime = false;
                            } else {
                                messageString = "";
                            }
                        }
                    } else {
                        String restrictionReason = MessagesController.getRestrictionReason(message.messageOwner.restriction_reason);
                        TLRPC.User fromUser = null;
                        TLRPC.Chat fromChat = null;
                        long fromId = message.getFromChatId();
                        if (DialogObject.isUserDialog(fromId)) {
                            fromUser = MessagesController.getInstance(currentAccount).getUser(fromId);
                        } else {
                            fromChat = MessagesController.getInstance(currentAccount).getChat(-fromId);
                        }
                        drawCount2 = true;
                        boolean lastMessageIsReaction = false;
                        if (dialogsType == 0 && currentDialogId > 0 && message.isOutOwner() && message.messageOwner.reactions != null && message.messageOwner.reactions.recent_reactions != null && !message.messageOwner.reactions.recent_reactions.isEmpty() && reactionMentionCount > 0) {
                            TLRPC.MessagePeerReaction lastReaction = message.messageOwner.reactions.recent_reactions.get(0);
                            if (lastReaction.unread && lastReaction.peer_id.user_id != 0 &&lastReaction.peer_id.user_id != UserConfig.getInstance(currentAccount).clientUserId) {
                                lastMessageIsReaction = true;
                                ReactionsLayoutInBubble.VisibleReaction visibleReaction = ReactionsLayoutInBubble.VisibleReaction.fromTLReaction(lastReaction.reaction);
                                currentMessagePaint = Theme.dialogs_messagePrintingPaint[paintIndex];
                                if (visibleReaction.emojicon != null) {
                                    messageString = LocaleController.formatString("ReactionInDialog", R.string.ReactionInDialog, visibleReaction.emojicon);
                                } else {
                                    String string = LocaleController.formatString("ReactionInDialog", R.string.ReactionInDialog, "**reaction**");
                                    int i = string.indexOf("**reaction**");
                                    string = string.replace("**reaction**", "d");

                                    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(string);
                                    spannableStringBuilder.setSpan(new AnimatedEmojiSpan(visibleReaction.documentId, currentMessagePaint == null ? null : currentMessagePaint.getFontMetricsInt()), i, i + 1, 0);

                                    messageString = spannableStringBuilder;
                                }
                            }
                        }
                        if (lastMessageIsReaction) {

                        } else if (dialogsType == 2) {
                            if (chat != null) {
                                if (ChatObject.isChannel(chat) && !chat.megagroup) {
                                    if (chat.participants_count != 0) {
                                        messageString = LocaleController.formatPluralStringComma("Subscribers", chat.participants_count);
                                    } else {
                                        if (!ChatObject.isPublic(chat)) {
                                            messageString = LocaleController.getString("ChannelPrivate", R.string.ChannelPrivate).toLowerCase();
                                        } else {
                                            messageString = LocaleController.getString("ChannelPublic", R.string.ChannelPublic).toLowerCase();
                                        }
                                    }
                                } else {
                                    if (chat.participants_count != 0) {
                                        messageString = LocaleController.formatPluralStringComma("Members", chat.participants_count);
                                    } else {
                                        if (chat.has_geo) {
                                            messageString = LocaleController.getString("MegaLocation", R.string.MegaLocation);
                                        } else if (!ChatObject.isPublic(chat)) {
                                            messageString = LocaleController.getString("MegaPrivate", R.string.MegaPrivate).toLowerCase();
                                        } else {
                                            messageString = LocaleController.getString("MegaPublic", R.string.MegaPublic).toLowerCase();
                                        }
                                    }
                                }
                            } else {
                                messageString = "";
                            }
                            drawCount2 = false;
                            showChecks = false;
                            drawTime = false;
                        } else if (dialogsType == 3 && UserObject.isUserSelf(user)) {
                            messageString = LocaleController.getString("SavedMessagesInfo", R.string.SavedMessagesInfo);
                            showChecks = false;
                            drawTime = false;
                        } else if (!useForceThreeLines && !SharedConfig.useThreeLinesLayout && currentDialogFolderId != 0) {
                            checkMessage = false;
                            messageString = formatArchivedDialogNames();
                        } else if (message.messageOwner instanceof TLRPC.TL_messageService && (!MessageObject.isTopicActionMessage(message) || message.messageOwner.action instanceof TLRPC.TL_messageActionTopicCreate)) {
                            if (ChatObject.isChannelAndNotMegaGroup(chat) && (message.messageOwner.action instanceof TLRPC.TL_messageActionChannelMigrateFrom)) {
                                messageString = "";
                                showChecks = false;
                            } else {
                                messageString = msgText;
                            }
                            currentMessagePaint = Theme.dialogs_messagePrintingPaint[paintIndex];
                        } else {
                            needEmoji = true;
                            updateMessageThumbs();
                            if (chat != null && chat.id > 0 && fromChat == null && (!ChatObject.isChannel(chat) || ChatObject.isMegagroup(chat)) && !ForumUtilities.isTopicCreateMessage(message)) {
                                messageNameString = getMessageNameString();
                                if (chat.forum && !isTopic) {
                                    CharSequence topicName = MessagesController.getInstance(currentAccount).getTopicsController().getTopicIconName(chat, message, currentMessagePaint);
                                    if (!TextUtils.isEmpty(topicName)) {
                                        SpannableStringBuilder arrowSpan = new SpannableStringBuilder("-");
                                        ColoredImageSpan coloredImageSpan = new ColoredImageSpan(ContextCompat.getDrawable(ApplicationLoader.applicationContext, R.drawable.msg_mini_forumarrow).mutate());
                                        coloredImageSpan.setColorKey(useForceThreeLines || SharedConfig.useThreeLinesLayout ? null : Theme.key_chats_nameMessage);
                                        arrowSpan.setSpan(coloredImageSpan, 0, 1, 0);
                                        SpannableStringBuilder nameSpannableString = new SpannableStringBuilder();
                                        nameSpannableString.append(messageNameString).append(arrowSpan).append(topicName);
                                        messageNameString = nameSpannableString;
                                    }
                                }
                                checkMessage = false;
                                SpannableStringBuilder stringBuilder = getMessageStringFormatted(messageFormat, restrictionReason, messageNameString, false);

                                int thumbInsertIndex = 0;
                                if (!useForceThreeLines && !SharedConfig.useThreeLinesLayout || currentDialogFolderId != 0 && stringBuilder.length() > 0) {
                                    try {
                                        stringBuilder.setSpan(new ForegroundColorSpanThemable(Theme.key_chats_nameMessage, resourcesProvider), 0, thumbInsertIndex = messageNameString.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                        offsetName = thumbInsertIndex;
                                    } catch (Exception e) {
                                        FileLog.e(e);
                                    }
                                }
                                messageString = Emoji.replaceEmoji(stringBuilder, Theme.dialogs_messagePaint[paintIndex].getFontMetricsInt(), AndroidUtilities.dp(20), false);
                                if (message.hasHighlightedWords()) {
                                    CharSequence messageH = AndroidUtilities.highlightText(messageString, message.highlightedWords, resourcesProvider);
                                    if (messageH != null) {
                                        messageString = messageH;
                                    }
                                }
                                if (thumbsCount > 0) {
                                    if (!(messageString instanceof SpannableStringBuilder)) {
                                        messageString = new SpannableStringBuilder(messageString);
                                    }
                                    checkMessage = false;
                                    SpannableStringBuilder builder = (SpannableStringBuilder) messageString;
                                    if (thumbInsertIndex >= builder.length()) {
                                        builder.append(" ");
                                        builder.setSpan(new FixedWidthSpan(AndroidUtilities.dp(thumbsCount * (thumbSize + 2) - 2 + 5)), builder.length() - 1, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    } else {
                                        builder.insert(thumbInsertIndex, " ");
                                        builder.setSpan(new FixedWidthSpan(AndroidUtilities.dp(thumbsCount * (thumbSize + 2) - 2 + 5)), thumbInsertIndex, thumbInsertIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    }
                                }
                            } else {
                                if (!TextUtils.isEmpty(restrictionReason)) {
                                    messageString = restrictionReason;
                                } else if (MessageObject.isTopicActionMessage(message)) {
                                    if (message.messageTextShort != null && (!(message.messageOwner.action instanceof TLRPC.TL_messageActionTopicCreate) || !isTopic)) {
                                        messageString = message.messageTextShort;
                                    } else {
                                        messageString = message.messageText;
                                    }
                                    if (message.topicIconDrawable[0] != null) {
                                        TLRPC.TL_forumTopic topic = MessagesController.getInstance(currentAccount).getTopicsController().findTopic(-message.getDialogId(), MessageObject.getTopicId(message.messageOwner));
                                        if (topic != null) {
                                            message.topicIconDrawable[0].setColor(topic.icon_color);
                                        }
                                    }
                                } else if (message.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto && message.messageOwner.media.photo instanceof TLRPC.TL_photoEmpty && message.messageOwner.media.ttl_seconds != 0) {
                                    messageString = LocaleController.getString("AttachPhotoExpired", R.string.AttachPhotoExpired);
                                } else if (message.messageOwner.media instanceof TLRPC.TL_messageMediaDocument && message.messageOwner.media.document instanceof TLRPC.TL_documentEmpty && message.messageOwner.media.ttl_seconds != 0) {
                                    messageString = LocaleController.getString("AttachVideoExpired", R.string.AttachVideoExpired);
                                } else if (getCaptionMessage() != null) {
                                    MessageObject message = getCaptionMessage();
                                    String emoji;
                                    if (!needEmoji) {
                                        emoji = "";
                                    } else if (message.isVideo()) {
                                        emoji = "\uD83D\uDCF9 ";
                                    } else if (message.isVoice()) {
                                        emoji = "\uD83C\uDFA4 ";
                                    } else if (message.isMusic()) {
                                        emoji = "\uD83C\uDFA7 ";
                                    } else if (message.isPhoto()) {
                                        emoji = "\uD83D\uDDBC ";
                                    } else {
                                        emoji = "\uD83D\uDCCE ";
                                    }
                                    if (message.hasHighlightedWords() && !TextUtils.isEmpty(message.messageOwner.message)) {
                                        String str = message.messageTrimmedToHighlight;
                                        if (message.messageTrimmedToHighlight != null) {
                                            str = message.messageTrimmedToHighlight;
                                        }
                                        int w = getMeasuredWidth() - AndroidUtilities.dp(messagePaddingStart + 23 + 24);
                                        if (hasNameInMessage) {
                                            if (!TextUtils.isEmpty(messageNameString)) {
                                                w -= currentMessagePaint.measureText(messageNameString.toString());
                                            }
                                            w -= currentMessagePaint.measureText(": ");
                                        }
                                        if (w > 0) {
                                            str = AndroidUtilities.ellipsizeCenterEnd(str, message.highlightedWords.get(0), w, currentMessagePaint, 130).toString();
                                        }
                                        messageString = emoji + str;
                                    } else {
                                        SpannableStringBuilder msgBuilder = new SpannableStringBuilder(message.caption);
                                        if (ConfigManager.getBooleanOrFalse(Defines.displaySpoilerMsgDirectly) && message != null && message.messageOwner != null) {
                                            MediaDataController.addTextStyleRuns(message.messageOwner.entities, message.caption, msgBuilder, TextStyleSpan.FLAG_STYLE_SPOILER);
                                            MediaDataController.addAnimatedEmojiSpans(message.messageOwner.entities, msgBuilder, currentMessagePaint == null ? null : currentMessagePaint.getFontMetricsInt());
                                        }
                                        messageString = new SpannableStringBuilder(emoji).append(msgBuilder);
                                    }
                                } else if (thumbsCount > 1) {
                                    if (hasVideoThumb) {
                                        messageString = LocaleController.formatPluralString("Media", groupMessages == null ? 0 : groupMessages.size());
                                    } else {
                                        messageString = LocaleController.formatPluralString("Photos", groupMessages == null ? 0 : groupMessages.size());
                                    }
                                    currentMessagePaint = Theme.dialogs_messagePrintingPaint[paintIndex];
                                } else {
                                    if (message.messageOwner.media instanceof TLRPC.TL_messageMediaPoll) {
                                        TLRPC.TL_messageMediaPoll mediaPoll = (TLRPC.TL_messageMediaPoll) message.messageOwner.media;
                                        messageString = "\uD83D\uDCCA " + mediaPoll.poll.question;
                                    } else if (message.messageOwner.media instanceof TLRPC.TL_messageMediaGame) {
                                        messageString = "\uD83C\uDFAE " + message.messageOwner.media.game.title;
                                    } else if (message.messageOwner.media instanceof TLRPC.TL_messageMediaInvoice) {
                                        messageString = message.messageOwner.media.title;
                                    } else if (message.type == MessageObject.TYPE_MUSIC) {
                                        messageString = String.format("\uD83C\uDFA7 %s - %s", message.getMusicAuthor(), message.getMusicTitle());
                                    } else {
                                        if (message.hasHighlightedWords() && !TextUtils.isEmpty(message.messageOwner.message)){
                                            messageString = message.messageTrimmedToHighlight;
                                            if (message.messageTrimmedToHighlight != null) {
                                                messageString = message.messageTrimmedToHighlight;
                                            }
                                            int w = getMeasuredWidth() - AndroidUtilities.dp(messagePaddingStart + 23 );
                                            messageString = AndroidUtilities.ellipsizeCenterEnd(messageString, message.highlightedWords.get(0), w, currentMessagePaint, 130).toString();
                                        } else {
                                            SpannableStringBuilder stringBuilder = new SpannableStringBuilder(msgText);
                                            if (ConfigManager.getBooleanOrFalse(Defines.displaySpoilerMsgDirectly))
                                            MediaDataController.addTextStyleRuns(message, stringBuilder, TextStyleSpan.FLAG_STYLE_SPOILER);
                                            if (ConfigManager.getBooleanOrFalse(Defines.displaySpoilerMsgDirectly) && message != null && message.messageOwner != null) {
                                                MediaDataController.addAnimatedEmojiSpans(message.messageOwner.entities, stringBuilder, currentMessagePaint == null ? null : currentMessagePaint.getFontMetricsInt());
                                            }
                                            messageString = stringBuilder;
                                        }
                                        AndroidUtilities.highlightText(messageString, message.highlightedWords, resourcesProvider);
                                    }
                                    if (message.messageOwner.media != null && !message.isMediaEmpty()) {
                                        currentMessagePaint = Theme.dialogs_messagePrintingPaint[paintIndex];
                                    }
                                }
                                if (thumbsCount > 0) {
                                    if (message.hasHighlightedWords() && !TextUtils.isEmpty(message.messageOwner.message)) {
                                        messageString = message.messageTrimmedToHighlight;
                                        if (message.messageTrimmedToHighlight != null) {
                                            messageString = message.messageTrimmedToHighlight;
                                        }
                                        int w = getMeasuredWidth() - AndroidUtilities.dp(messagePaddingStart + 23 + (thumbSize + 2) * thumbsCount - 2 + 5);
                                        messageString = AndroidUtilities.ellipsizeCenterEnd(messageString, message.highlightedWords.get(0), w, currentMessagePaint, 130).toString();
                                    } else {
                                        if (messageString.length() > 150) {
                                            messageString = messageString.subSequence(0, 150);
                                        }
                                        messageString = AndroidUtilities.replaceNewLines(messageString);
                                    }
                                    if (!(messageString instanceof SpannableStringBuilder)) {
                                        messageString = new SpannableStringBuilder(messageString);
                                    }
                                    checkMessage = false;
                                    SpannableStringBuilder builder = (SpannableStringBuilder) messageString;
                                    builder.insert(0, " ");
                                    builder.setSpan(new FixedWidthSpan(AndroidUtilities.dp((thumbSize + 2) * thumbsCount - 2 + 5)), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    Emoji.replaceEmoji(builder, Theme.dialogs_messagePaint[paintIndex].getFontMetricsInt(), AndroidUtilities.dp(17), false);
                                    if (message.hasHighlightedWords()) {
                                        CharSequence s = AndroidUtilities.highlightText(builder, message.highlightedWords, resourcesProvider);
                                        if (s != null) {
                                            messageString = s;
                                        }
                                    }
                                }
                            }
                        }
                        if (currentDialogFolderId != 0) {
                            messageNameString = formatArchivedDialogNames();
                        }
                    }
                }
            }

            if (draftMessage != null) {
                timeString = LocaleController.stringForMessageListDate(draftMessage.date);
            } else if (lastMessageDate != 0) {
                timeString = LocaleController.stringForMessageListDate(lastMessageDate);
            } else if (message != null) {
                timeString = LocaleController.stringForMessageListDate(message.messageOwner.date);
            }

            if (message == null) {
                drawCheck1 = false;
                drawCheck2 = false;
                drawClock = false;
                drawCount = false;
                drawMention = false;
                drawReactionMention = false;
                drawError = false;
            } else {
                if (currentDialogFolderId != 0) {
                    if (unreadCount + mentionCount > 0) {
                        if (unreadCount > mentionCount) {
                            drawCount = true;
                            drawMention = false;
                            countString = String.format("%d", unreadCount + mentionCount);
                        } else {
                            drawCount = false;
                            drawMention = true;
                            mentionString = String.format("%d", unreadCount + mentionCount);
                        }
                    } else {
                        drawCount = false;
                        drawMention = false;
                    }
                    drawReactionMention = false;
                } else {
                    if (clearingDialog) {
                        drawCount = false;
                        showChecks = false;
                    } else if (unreadCount != 0 && (unreadCount != 1 || unreadCount != mentionCount || message == null || !message.messageOwner.mentioned)) {
                        drawCount = true;
                        countString = String.format("%d", unreadCount);
                    } else if (markUnread) {
                        drawCount = true;
                        countString = "";
                    } else {
                        drawCount = false;
                    }
                    if (mentionCount != 0) {
                        drawMention = true;
                        mentionString = "@";
                    } else {
                        drawMention = false;
                    }
                    if (reactionMentionCount > 0) {
                        drawReactionMention = true;
                    } else {
                        drawReactionMention = false;
                    }
                }

                if (message.isOut() && draftMessage == null && showChecks && !(message.messageOwner.action instanceof TLRPC.TL_messageActionHistoryClear)) {
                    if (message.isSending()) {
                        drawCheck1 = false;
                        drawCheck2 = false;
                        drawClock = true;
                        drawError = false;
                    } else if (message.isSendError()) {
                        drawCheck1 = false;
                        drawCheck2 = false;
                        drawClock = false;
                        drawError = true;
                        drawCount = false;
                        drawMention = false;
                    } else if (message.isSent()) {
                        if (forumTopic != null) {
                            drawCheck1 = forumTopic.read_outbox_max_id >= message.getId();
                        } else if (isDialogCell) {
                            drawCheck1 = (readOutboxMaxId > 0 && readOutboxMaxId >= message.getId()) || !message.isUnread() || ChatObject.isChannel(chat) && !chat.megagroup;
                        } else {
                            drawCheck1 = !message.isUnread() || ChatObject.isChannel(chat) && !chat.megagroup;
                        }
                        drawCheck2 = true;
                        drawClock = false;
                        drawError = false;
                    }
                } else {
                    drawCheck1 = false;
                    drawCheck2 = false;
                    drawClock = false;
                    drawError = false;
                }
            }

            promoDialog = false;
            MessagesController messagesController = MessagesController.getInstance(currentAccount);
            if (dialogsType == 0 && messagesController.isPromoDialog(currentDialogId, true)) {
                drawPinBackground = true;
                promoDialog = true;
                if (messagesController.promoDialogType == MessagesController.PROMO_TYPE_PROXY) {
                    timeString = LocaleController.getString("UseProxySponsor", R.string.UseProxySponsor);
                } else if (messagesController.promoDialogType == MessagesController.PROMO_TYPE_PSA) {
                    timeString = LocaleController.getString("PsaType_" + messagesController.promoPsaType);
                    if (TextUtils.isEmpty(timeString)) {
                        timeString = LocaleController.getString("PsaTypeDefault", R.string.PsaTypeDefault);
                    }
                    if (!TextUtils.isEmpty(messagesController.promoPsaMessage)) {
                        messageString = messagesController.promoPsaMessage;
                        thumbsCount = 0;
                    }
                }
            }

            if (currentDialogFolderId != 0) {
                nameString = LocaleController.getString("ArchivedChats", R.string.ArchivedChats);
            } else {
                if (chat != null) {
                    if (isTopic) {
                        nameString = forumTopic.title;
                    } else {
                        nameString = chat.title;
                    }
                } else if (user != null) {
                    if (UserObject.isReplyUser(user)) {
                        nameString = LocaleController.getString("RepliesTitle", R.string.RepliesTitle);
                    } else if (UserObject.isUserSelf(user)) {
                        if (useMeForMyMessages) {
                            nameString = LocaleController.getString("FromYou", R.string.FromYou);
                        } else {
                            if (dialogsType == 3) {
                                drawPinBackground = true;
                            }
                            nameString = LocaleController.getString("SavedMessages", R.string.SavedMessages);
                        }
                    } else {
                        nameString = UserObject.getUserName(user);
                    }
                }
                if (nameString != null && nameString.length() == 0) {
                    nameString = LocaleController.getString("HiddenName", R.string.HiddenName);
                }
            }
        }

        int timeWidth;
        if (drawTime) {
            timeWidth = (int) Math.ceil(Theme.dialogs_timePaint.measureText(timeString));
            timeLayout = new StaticLayout(timeString, Theme.dialogs_timePaint, timeWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            if (!LocaleController.isRTL) {
                timeLeft = getMeasuredWidth() - AndroidUtilities.dp(15) - timeWidth;
            } else {
                timeLeft = AndroidUtilities.dp(15);
            }
        } else {
            timeWidth = 0;
            timeLayout = null;
            timeLeft = 0;
        }

        int timeLeftOffset = 0;
        if (drawLock2()) {
            if (LocaleController.isRTL) {
                lock2Left = timeLeft + timeWidth + AndroidUtilities.dp(4);
            } else {
                lock2Left = timeLeft - Theme.dialogs_lock2Drawable.getIntrinsicWidth() - AndroidUtilities.dp(4);
            }
            timeLeftOffset += Theme.dialogs_lock2Drawable.getIntrinsicWidth() + AndroidUtilities.dp(4);
            timeWidth += timeLeftOffset;
        }

        if (!LocaleController.isRTL) {
            nameWidth = getMeasuredWidth() - nameLeft - AndroidUtilities.dp(14 + 8) - timeWidth;
        } else {
            nameWidth = getMeasuredWidth() - nameLeft - AndroidUtilities.dp(messagePaddingStart + 5 + 8) - timeWidth;
            nameLeft += timeWidth;
        }
        if (drawNameLock) {
            nameWidth -= AndroidUtilities.dp(4) + Theme.dialogs_lockDrawable.getIntrinsicWidth();
        }
        if (drawClock) {
            int w = Theme.dialogs_clockDrawable.getIntrinsicWidth() + AndroidUtilities.dp(5);
            nameWidth -= w;
            if (!LocaleController.isRTL) {
                clockDrawLeft = timeLeft - timeLeftOffset - w;
            } else {
                clockDrawLeft = timeLeft + timeWidth + AndroidUtilities.dp(5);
                nameLeft += w;
            }
        } else if (drawCheck2) {
            int w = Theme.dialogs_checkDrawable.getIntrinsicWidth() + AndroidUtilities.dp(5);
            nameWidth -= w;
            if (drawCheck1) {
                nameWidth -= Theme.dialogs_halfCheckDrawable.getIntrinsicWidth() - AndroidUtilities.dp(8);
                if (!LocaleController.isRTL) {
                    halfCheckDrawLeft = timeLeft - timeLeftOffset - w;
                    checkDrawLeft = halfCheckDrawLeft - AndroidUtilities.dp(5.5f);
                } else {
                    checkDrawLeft = timeLeft + timeWidth + AndroidUtilities.dp(5);
                    halfCheckDrawLeft = checkDrawLeft + AndroidUtilities.dp(5.5f);
                    nameLeft += w + Theme.dialogs_halfCheckDrawable.getIntrinsicWidth() - AndroidUtilities.dp(8);
                }
            } else {
                if (!LocaleController.isRTL) {
                    checkDrawLeft1 = timeLeft - timeLeftOffset - w;
                } else {
                    checkDrawLeft1 = timeLeft + timeWidth + AndroidUtilities.dp(5);
                    nameLeft += w;
                }
            }
        }

        if (drawPremium && emojiStatus.getDrawable() != null) {
            int w = AndroidUtilities.dp(6 + 24 + 6);
            nameWidth -= w;
            if (LocaleController.isRTL) {
                nameLeft += w;
            }
        } else if ((dialogMuted || drawUnmute) && !drawVerified && drawScam == 0) {
            int w = AndroidUtilities.dp(6) + Theme.dialogs_muteDrawable.getIntrinsicWidth();
            nameWidth -= w;
            if (LocaleController.isRTL) {
                nameLeft += w;
            }
        } else if (drawVerified) {
            int w = AndroidUtilities.dp(6) + Theme.dialogs_verifiedDrawable.getIntrinsicWidth();
            nameWidth -= w;
            if (LocaleController.isRTL) {
                nameLeft += w;
            }
        } else if (drawPremium) {
            int w = AndroidUtilities.dp(6 + 24 + 6);
            nameWidth -= w;
            if (LocaleController.isRTL) {
                nameLeft += w;
            }
        } else if (drawScam != 0) {
            int w = AndroidUtilities.dp(6) + (drawScam == 1 ? Theme.dialogs_scamDrawable : Theme.dialogs_fakeDrawable).getIntrinsicWidth();
            nameWidth -= w;
            if (LocaleController.isRTL) {
                nameLeft += w;
            }
        }
        try {
            int ellipsizeWidth = nameWidth - AndroidUtilities.dp(12);
            if (ellipsizeWidth < 0) {
                ellipsizeWidth = 0;
            }
            CharSequence nameStringFinal = nameString.replace('\n', ' ');
            if (nameLayoutEllipsizeByGradient) {
                nameLayoutFits = nameStringFinal.length() == TextUtils.ellipsize(nameStringFinal, Theme.dialogs_namePaint[paintIndex], ellipsizeWidth, TextUtils.TruncateAt.END).length();
                ellipsizeWidth += AndroidUtilities.dp(48);
            }
            nameIsEllipsized = Theme.dialogs_namePaint[paintIndex].measureText(nameStringFinal.toString()) > ellipsizeWidth;
            if (!twoLinesForName) {
                nameStringFinal = TextUtils.ellipsize(nameStringFinal, Theme.dialogs_namePaint[paintIndex], ellipsizeWidth, TextUtils.TruncateAt.END);
            }
            nameStringFinal = Emoji.replaceEmoji(nameStringFinal, Theme.dialogs_namePaint[paintIndex].getFontMetricsInt(), AndroidUtilities.dp(20), false);
            if (message != null && message.hasHighlightedWords()) {
                CharSequence s = AndroidUtilities.highlightText(nameStringFinal, message.highlightedWords, resourcesProvider);
                if (s != null) {
                    nameStringFinal = s;
                }
            }
            if (twoLinesForName) {
                nameLayout = StaticLayoutEx.createStaticLayout(nameStringFinal, Theme.dialogs_namePaint[paintIndex], ellipsizeWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false, TextUtils.TruncateAt.END, ellipsizeWidth, 2);
            } else {
                nameLayout = new StaticLayout(nameStringFinal, Theme.dialogs_namePaint[paintIndex], Math.max(ellipsizeWidth, nameWidth), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            }
            nameLayoutTranslateX = nameLayoutEllipsizeByGradient && nameLayout.isRtlCharAt(0) ? -AndroidUtilities.dp(36) : 0;
            nameLayoutEllipsizeLeft = nameLayout.isRtlCharAt(0);
        } catch (Exception e) {
            FileLog.e(e);
        }

        int messageWidth;
        int avatarLeft;
        int avatarTop;
        int thumbLeft;
        if (useForceThreeLines || SharedConfig.useThreeLinesLayout) {
            avatarTop = AndroidUtilities.dp(11);
            messageNameTop = AndroidUtilities.dp(32);
            timeTop = AndroidUtilities.dp(13);
            errorTop = AndroidUtilities.dp(43);
            pinTop = AndroidUtilities.dp(43);
            countTop = AndroidUtilities.dp(43);
            checkDrawTop = AndroidUtilities.dp(13);
            messageWidth = getMeasuredWidth() - AndroidUtilities.dp(messagePaddingStart + 21);

            if (LocaleController.isRTL) {
                messageLeft = messageNameLeft = AndroidUtilities.dp(16);
                avatarLeft = getMeasuredWidth() - AndroidUtilities.dp(66);
                thumbLeft = avatarLeft - AndroidUtilities.dp(13 + 18);
            } else {
                messageLeft = messageNameLeft = AndroidUtilities.dp(messagePaddingStart + 6);
                avatarLeft = AndroidUtilities.dp(10);
                thumbLeft = avatarLeft + AndroidUtilities.dp(56 + 13);
            }
            avatarImage.setImageCoords(avatarLeft, avatarTop, AndroidUtilities.dp(56), AndroidUtilities.dp(56));
            for (int i = 0; i < thumbImage.length; ++i) {
                thumbImage[i].setImageCoords(thumbLeft + (thumbSize + 2) * i, avatarTop + AndroidUtilities.dp(31) + (twoLinesForName ?  AndroidUtilities.dp(20) : 0), AndroidUtilities.dp(18), AndroidUtilities.dp(18));
            }
        } else {
            avatarTop = AndroidUtilities.dp(9);
            messageNameTop = AndroidUtilities.dp(31);
            timeTop = AndroidUtilities.dp(16);
            errorTop = AndroidUtilities.dp(39);
            pinTop = AndroidUtilities.dp(39);
            countTop = isTopic ? AndroidUtilities.dp(36) : AndroidUtilities.dp(39);
            checkDrawTop = AndroidUtilities.dp(17);
            messageWidth = getMeasuredWidth() - AndroidUtilities.dp(messagePaddingStart + 23 - (LocaleController.isRTL ? 0 : 12));

            if (LocaleController.isRTL) {
                messageLeft = messageNameLeft = AndroidUtilities.dp(22);
                avatarLeft = getMeasuredWidth() - AndroidUtilities.dp(64);
                thumbLeft = avatarLeft - AndroidUtilities.dp(11 + (thumbsCount * (thumbSize + 2) - 2));
            } else {
                messageLeft = messageNameLeft = AndroidUtilities.dp(messagePaddingStart + 4);
                avatarLeft = AndroidUtilities.dp(10);
                thumbLeft = avatarLeft + AndroidUtilities.dp(56 + 11);
            }
            avatarImage.setImageCoords(avatarLeft, avatarTop, AndroidUtilities.dp(54), AndroidUtilities.dp(54));
            for (int i = 0; i < thumbImage.length; ++i) {
                thumbImage[i].setImageCoords(thumbLeft + (thumbSize + 2) * i, avatarTop + AndroidUtilities.dp(30) + (twoLinesForName ? AndroidUtilities.dp(20) : 0), AndroidUtilities.dp(thumbSize), AndroidUtilities.dp(thumbSize));
            }
        }
        if (twoLinesForName) {
            messageNameTop += AndroidUtilities.dp(20);
        }
        if (getIsPinned()) {
            if (!LocaleController.isRTL) {
                pinLeft = getMeasuredWidth() - Theme.dialogs_pinnedDrawable.getIntrinsicWidth() - AndroidUtilities.dp(14);
            } else {
                pinLeft = AndroidUtilities.dp(14);
            }
        }
        if (drawError) {
            int w = AndroidUtilities.dp(23 + 8);
            messageWidth -= w;
            if (!LocaleController.isRTL) {
                errorLeft = getMeasuredWidth() - AndroidUtilities.dp(23 + 11);
            } else {
                errorLeft = AndroidUtilities.dp(11);
                messageLeft += w;
                messageNameLeft += w;
            }
        } else if (countString != null || mentionString != null || drawReactionMention) {
            if (countString != null) {
                countWidth = Math.max(AndroidUtilities.dp(12), (int) Math.ceil(Theme.dialogs_countTextPaint.measureText(countString)));
                countLayout = new StaticLayout(countString, Theme.dialogs_countTextPaint, countWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                int w = countWidth + AndroidUtilities.dp(18);
                messageWidth -= w;
                if (!LocaleController.isRTL) {
                    countLeft = getMeasuredWidth() - countWidth - AndroidUtilities.dp(20);
                } else {
                    countLeft = AndroidUtilities.dp(20);
                    messageLeft += w;
                    messageNameLeft += w;
                }
                drawCount = true;
            } else {
                countWidth = 0;
            }
            if (mentionString != null) {
                if (currentDialogFolderId != 0) {
                    mentionWidth = Math.max(AndroidUtilities.dp(12), (int) Math.ceil(Theme.dialogs_countTextPaint.measureText(mentionString)));
                    mentionLayout = new StaticLayout(mentionString, Theme.dialogs_countTextPaint, mentionWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                } else {
                    mentionWidth = AndroidUtilities.dp(12);
                }
                int w = mentionWidth + AndroidUtilities.dp(18);
                messageWidth -= w;
                if (!LocaleController.isRTL) {
                    mentionLeft = getMeasuredWidth() - mentionWidth - AndroidUtilities.dp(20) - (countWidth != 0 ? countWidth + AndroidUtilities.dp(18) : 0);
                } else {
                    mentionLeft = AndroidUtilities.dp(20) + (countWidth != 0 ? countWidth + AndroidUtilities.dp(18) : 0);
                    messageLeft += w;
                    messageNameLeft += w;
                }
                drawMention = true;
            } else {
                mentionWidth = 0;
            }
            if (drawReactionMention) {
                int w = AndroidUtilities.dp(24);
                messageWidth -= w;
                if (!LocaleController.isRTL) {
                    reactionMentionLeft = getMeasuredWidth() - AndroidUtilities.dp(32);
                    if (drawMention) {
                        reactionMentionLeft -= (mentionWidth != 0 ? (mentionWidth + AndroidUtilities.dp(18)) : 0);
                    }
                    if (drawCount) {
                        reactionMentionLeft -= (countWidth != 0 ? countWidth + AndroidUtilities.dp(18) : 0);
                    }
                } else {
                    reactionMentionLeft = AndroidUtilities.dp(20);
                    if (drawMention) {
                        reactionMentionLeft += (mentionWidth != 0 ? (mentionWidth + AndroidUtilities.dp(18)) : 0);
                    }
                    if (drawCount) {
                        reactionMentionLeft += (countWidth != 0 ? (countWidth + AndroidUtilities.dp(18)) : 0);
                    }
                    messageLeft += w;
                    messageNameLeft += w;
                }
            }
        } else {
            if (getIsPinned()) {
                int w = Theme.dialogs_pinnedDrawable.getIntrinsicWidth() + AndroidUtilities.dp(8);
                messageWidth -= w;
                if (LocaleController.isRTL) {
                    messageLeft += w;
                    messageNameLeft += w;
                }
            }
            drawCount = false;
            drawMention = false;
        }

        if (checkMessage) {
            if (messageString == null) {
                messageString = "";
            }
            CharSequence mess = messageString;
            if (mess.length() > 150) {
                mess = mess.subSequence(0, 150);
            }
            if (!useForceThreeLines && !SharedConfig.useThreeLinesLayout || messageNameString != null) {
                mess = AndroidUtilities.replaceNewLines(mess);
            } else {
                mess = AndroidUtilities.replaceTwoNewLinesToOne(mess);
            }
            messageString = Emoji.replaceEmoji(mess, Theme.dialogs_messagePaint[paintIndex].getFontMetricsInt(), AndroidUtilities.dp(17), false);
            if (message != null) {
                CharSequence s = AndroidUtilities.highlightText(messageString, message.highlightedWords, resourcesProvider);
                if (s != null) {
                    messageString = s;
                }
            }
        }
        messageWidth = Math.max(AndroidUtilities.dp(12), messageWidth);
        buttonTop = useForceThreeLines || SharedConfig.useThreeLinesLayout ? AndroidUtilities.dp(58) : AndroidUtilities.dp(62);
        if (isForumCell()) {
            if (useForceThreeLines || SharedConfig.useThreeLinesLayout) {
                messageTop = AndroidUtilities.dp(34);
            } else {
                messageTop = AndroidUtilities.dp(39);
            }
            for (int i = 0; i < thumbImage.length; ++i) {
                thumbImage[i].setImageY(buttonTop);
            }
        } else if ((useForceThreeLines || SharedConfig.useThreeLinesLayout) && messageNameString != null && (currentDialogFolderId == 0 || currentDialogFolderDialogsCount == 1)) {
            try {
                if (message != null && message.hasHighlightedWords()) {
                    CharSequence s = AndroidUtilities.highlightText(messageNameString, message.highlightedWords, resourcesProvider);
                    if (s != null) {
                        messageNameString = s;
                    }
                }
                messageNameLayout = StaticLayoutEx.createStaticLayout(messageNameString, Theme.dialogs_messageNamePaint, messageWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false, TextUtils.TruncateAt.END, messageWidth, 1);
            } catch (Exception e) {
                FileLog.e(e);
            }
            messageTop = AndroidUtilities.dp(32 + 19);
            int yoff = nameIsEllipsized && isTopic ? AndroidUtilities.dp(20) : 0;
            for (int i = 0; i < thumbImage.length; ++i) {
                thumbImage[i].setImageY(avatarTop + yoff + AndroidUtilities.dp(40));
            }
        } else {
            messageNameLayout = null;
            if (useForceThreeLines || SharedConfig.useThreeLinesLayout) {
                messageTop = AndroidUtilities.dp(32);
                int yoff = nameIsEllipsized && isTopic ? AndroidUtilities.dp(20) : 0;
                for (int i = 0; i < thumbImage.length; ++i) {
                    thumbImage[i].setImageY(avatarTop + yoff + AndroidUtilities.dp(21));
                }
            } else {
                messageTop = AndroidUtilities.dp(39);
            }
        }

        if (twoLinesForName) {
            messageTop += AndroidUtilities.dp(20);
        }
        animatedEmojiStack2 = AnimatedEmojiSpan.update(AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES, this, animatedEmojiStack2, messageNameLayout);


        try {
            if (!TextUtils.isEmpty(buttonString)) {
                buttonString = Emoji.replaceEmoji(buttonString, currentMessagePaint.getFontMetricsInt(), AndroidUtilities.dp(17), false);
                CharSequence buttonStringFinal = TextUtils.ellipsize(buttonString, currentMessagePaint, messageWidth - AndroidUtilities.dp(26), TextUtils.TruncateAt.END);
                buttonLayout = new StaticLayout(buttonStringFinal, currentMessagePaint, messageWidth - AndroidUtilities.dp(20), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

                spoilersPool2.addAll(spoilers2);
                spoilers2.clear();
                SpoilerEffect.addSpoilers(this, buttonLayout, spoilersPool2, spoilers2);
            } else {
                buttonLayout = null;
            }
        } catch (Exception e) {

        }
        animatedEmojiStack3 = AnimatedEmojiSpan.update(AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES, this, animatedEmojiStack3, buttonLayout);

        try {
            CharSequence messageStringFinal;
            if ((useForceThreeLines || SharedConfig.useThreeLinesLayout) && currentDialogFolderId != 0 && currentDialogFolderDialogsCount > 1) {
                messageStringFinal = messageNameString;
                messageNameString = null;
                currentMessagePaint = Theme.dialogs_messagePaint[paintIndex];
            } else if (!useForceThreeLines && !SharedConfig.useThreeLinesLayout || messageNameString != null) {
                if (!isForumCell() && messageString instanceof Spanned && ((Spanned) messageString).getSpans(0, messageString.length(), FixedWidthSpan.class).length <= 0) {
                    messageStringFinal = TextUtils.ellipsize(messageString, currentMessagePaint, messageWidth - AndroidUtilities.dp(12 + (thumbsCount * (thumbSize + 2) - 2) + 5), TextUtils.TruncateAt.END);
                } else {
                    messageStringFinal = TextUtils.ellipsize(messageString, currentMessagePaint, messageWidth - AndroidUtilities.dp(12), TextUtils.TruncateAt.END);
                }
            } else {
                messageStringFinal = messageString;
            }
            // Removing links and bold spans to get rid of underlining and boldness
            if (messageStringFinal instanceof Spannable) {
                Spannable messageStringSpannable = (Spannable) messageStringFinal;
                for (CharacterStyle span : messageStringSpannable.getSpans(0, messageStringSpannable.length(), CharacterStyle.class)) {
                    if (span instanceof ClickableSpan || (span instanceof StyleSpan && ((StyleSpan) span).getStyle() == android.graphics.Typeface.BOLD)) {
                        messageStringSpannable.removeSpan(span);
                    }
                }
            }

            if (useForceThreeLines || SharedConfig.useThreeLinesLayout) {
                if (thumbsCount > 0 && messageNameString != null) {
                    messageWidth += AndroidUtilities.dp(5);
                }
                messageLayout = StaticLayoutEx.createStaticLayout(messageStringFinal, currentMessagePaint, messageWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, AndroidUtilities.dp(1), false, TextUtils.TruncateAt.END, messageWidth, messageNameString != null ? 1 : 2);
            } else {
                if (thumbsCount > 0) {
                    messageWidth += AndroidUtilities.dp((thumbsCount * (thumbSize + 2) - 2) + 5);
                    if (LocaleController.isRTL) {
                        messageLeft -= AndroidUtilities.dp((thumbsCount * (thumbSize + 2) - 2) + 5);
                    }
                }
                messageLayout = new StaticLayout(messageStringFinal, currentMessagePaint, messageWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            }
            spoilersPool.addAll(spoilers);
            spoilers.clear();
            if (ConfigManager.getBooleanOrFalse(Defines.displaySpoilerMsgDirectly)) {
                SpoilerEffect.addSpoilers(this, messageLayout, spoilersPool, spoilers);
            }
        } catch (Exception e) {
            messageLayout = null;
            FileLog.e(e);
        }
        animatedEmojiStack = AnimatedEmojiSpan.update(AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES, this, animatedEmojiStack, messageLayout);

        double widthpx;
        float left;
        if (LocaleController.isRTL) {
            if (nameLayout != null && nameLayout.getLineCount() > 0) {
                left = nameLayout.getLineLeft(0);
                widthpx = Math.ceil(nameLayout.getLineWidth(0));
                nameWidth += AndroidUtilities.dp(12);
                if (nameLayoutEllipsizeByGradient) {
                    widthpx = Math.min(nameWidth, widthpx);
                }
                if ((dialogMuted || drawUnmute) && !drawVerified && drawScam == 0) {
                    nameMuteLeft = (int) (nameLeft + (nameWidth - widthpx) - AndroidUtilities.dp(6) - Theme.dialogs_muteDrawable.getIntrinsicWidth());
                } else if (drawVerified) {
                    nameMuteLeft = (int) (nameLeft + (nameWidth - widthpx) - AndroidUtilities.dp(6) - Theme.dialogs_verifiedDrawable.getIntrinsicWidth());
                } else if (drawPremium) {
                    nameMuteLeft = (int) (nameLeft + (nameWidth - widthpx - left) - AndroidUtilities.dp(24));
                } else if (drawScam != 0) {
                    nameMuteLeft = (int) (nameLeft + (nameWidth - widthpx) - AndroidUtilities.dp(6) - (drawScam == 1 ? Theme.dialogs_scamDrawable : Theme.dialogs_fakeDrawable).getIntrinsicWidth());
                }
                if (left == 0) {
                    if (widthpx < nameWidth) {
                        nameLeft += (nameWidth - widthpx);
                    }
                }
            }
            if (messageLayout != null) {
                int lineCount = messageLayout.getLineCount();
                if (lineCount > 0) {
                    int w = Integer.MAX_VALUE;
                    for (int a = 0; a < lineCount; a++) {
                        left = messageLayout.getLineLeft(a);
                        if (left == 0) {
                            widthpx = Math.ceil(messageLayout.getLineWidth(a));
                            w = Math.min(w, (int) (messageWidth - widthpx));
                        } else {
                            w = 0;
                            break;
                        }
                    }
                    if (w != Integer.MAX_VALUE) {
                        messageLeft += w;
                    }
                }
            }
            if (messageNameLayout != null && messageNameLayout.getLineCount() > 0) {
                left = messageNameLayout.getLineLeft(0);
                if (left == 0) {
                    widthpx = Math.ceil(messageNameLayout.getLineWidth(0));
                    if (widthpx < messageWidth) {
                        messageNameLeft += (messageWidth - widthpx);
                    }
                }
            }
        } else {
            if (nameLayout != null && nameLayout.getLineCount() > 0) {
                left = nameLayout.getLineRight(0);
                if (nameLayoutEllipsizeByGradient) {
                    left = Math.min(nameWidth, left);
                }
                if (left == nameWidth) {
                    widthpx = Math.ceil(nameLayout.getLineWidth(0));
                    if (nameLayoutEllipsizeByGradient) {
                        widthpx = Math.min(nameWidth, widthpx);
//                        widthpx -= AndroidUtilities.dp(36);
//                        left += AndroidUtilities.dp(36);
                    }
                    if (widthpx < nameWidth) {
                        nameLeft -= (nameWidth - widthpx);
                    }
                }
                if (dialogMuted || drawUnmute || drawVerified || drawPremium || drawScam != 0) {
                    nameMuteLeft = (int) (nameLeft + left + AndroidUtilities.dp(6));
                }
            }
            if (messageLayout != null) {
                int lineCount = messageLayout.getLineCount();
                if (lineCount > 0) {
                    left = Integer.MAX_VALUE;
                    for (int a = 0; a < lineCount; a++) {
                        left = Math.min(left, messageLayout.getLineLeft(a));
                    }
                    messageLeft -= left;
                }
            }
            if (messageNameLayout != null && messageNameLayout.getLineCount() > 0) {
                messageNameLeft -= messageNameLayout.getLineLeft(0);
            }
        }
        if (messageLayout != null && printingStringType >= 0 && messageLayout.getText().length() > 0) {
            float x1, x2;
            if (printingStringReplaceIndex >= 0 && printingStringReplaceIndex + 1 < messageLayout.getText().length() ){
                x1 = messageLayout.getPrimaryHorizontal(printingStringReplaceIndex);
                x2 = messageLayout.getPrimaryHorizontal(printingStringReplaceIndex + 1);
            } else {
                x1 = messageLayout.getPrimaryHorizontal(0);
                x2 = messageLayout.getPrimaryHorizontal(1);
            }
            if (x1 < x2) {
                statusDrawableLeft = (int) (messageLeft + x1);
            } else {
                statusDrawableLeft = (int) (messageLeft + x2 + AndroidUtilities.dp(3));
            }
        }
        updateThumbsPosition();
    }

    private void updateThumbsPosition() {
        if (thumbsCount > 0) {
            StaticLayout layout = isForumCell() ? buttonLayout : messageLayout;
            if (layout == null) {
                return;
            }
            try {
                CharSequence text = layout.getText();
                if (text instanceof Spanned) {
                    FixedWidthSpan[] spans = ((Spanned) text).getSpans(0, text.length(), FixedWidthSpan.class);
                    if (spans != null && spans.length > 0) {
                        int spanOffset = ((Spanned) text).getSpanStart(spans[0]);
                        if (spanOffset < 0) {
                            spanOffset = 0;
                        }

                        float x1 = layout.getPrimaryHorizontal(spanOffset);
                        float x2 = layout.getPrimaryHorizontal(spanOffset + 1);
                        int offset = (int) Math.ceil(Math.min(x1, x2));
                        if (offset != 0) {
                            offset += AndroidUtilities.dp(3);
                        }
                        for (int i = 0; i < thumbsCount; ++i) {
                            thumbImage[i].setImageX(messageLeft + offset + AndroidUtilities.dp((thumbSize + 2) * i));
                        }
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    private CharSequence applyThumbs(CharSequence string) {
        if (thumbsCount > 0) {
            SpannableStringBuilder builder = SpannableStringBuilder.valueOf(string);
            builder.insert(0, " ");
            builder.setSpan(new FixedWidthSpan(AndroidUtilities.dp((thumbSize + 2) * thumbsCount - 2 + 5)), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return builder;
        }
        return string;
    }

    int topMessageTopicStartIndex;
    int topMessageTopicEndIndex;

    private CharSequence formatTopicsNames() {
        topMessageTopicStartIndex = 0;
        topMessageTopicEndIndex = 0;
        if (chat != null) {
            List<TLRPC.TL_forumTopic> topics = MessagesController.getInstance(currentAccount).getTopicsController().getTopics(chat.id);

            boolean hasDivider = false;
            if (topics != null && !topics.isEmpty()) {
                topics = new ArrayList<>(topics);
                Collections.sort(topics, Comparator.comparingInt(o -> -o.top_message));
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                int topMessageTopicId = 0;
                int boldLen = 0;
                if (message != null) {
                    topMessageTopicId = MessageObject.getTopicId(message.messageOwner);
                    TLRPC.TL_forumTopic topic = MessagesController.getInstance(currentAccount).getTopicsController().findTopic(chat.id, topMessageTopicId);
                    if (topic != null) {
                        CharSequence topicString = ForumUtilities.getTopicSpannedName(topic, currentMessagePaint);
                        spannableStringBuilder.append(topicString);
                        if (topic.unread_count > 0) {
                            boldLen = topicString.length();
                        }
                        topMessageTopicStartIndex = 0;
                        topMessageTopicEndIndex = topicString.length();

                        if (message.isOutOwner()) {
                            lastTopicMessageUnread = topic.read_inbox_max_id < message.getId();
                        } else {
                            lastTopicMessageUnread = topic.unread_count > 0;
                        }
                    }
                    if (lastTopicMessageUnread) {
                        spannableStringBuilder.append(" ");
                        spannableStringBuilder.setSpan(new FixedWidthSpan(AndroidUtilities.dp(3)), spannableStringBuilder.length() - 1, spannableStringBuilder.length(), 0);
                        hasDivider = true;
                    }
                }

                boolean firstApplay = true;
                for (int i = 0; i < Math.min(5, topics.size()); i++) {
                    if (topics.get(i).id == topMessageTopicId) {
                        continue;
                    }

                    if (spannableStringBuilder.length() != 0) {
                        if (firstApplay && hasDivider) {
                            spannableStringBuilder.append(" ");
                        } else {
                            spannableStringBuilder.append(", ");
                        }
                    }
                    firstApplay = false;
                    CharSequence topicString = ForumUtilities.getTopicSpannedName(topics.get(i), currentMessagePaint);
                    spannableStringBuilder.append(topicString);
                }
                if (boldLen > 0) {
                    spannableStringBuilder.setSpan(
                            new TypefaceSpan(AndroidUtilities.getTypeface("fonts/rmedium.ttf"), 0, Theme.getColor(Theme.key_chats_name, resourcesProvider)),
                            0, Math.min(spannableStringBuilder.length(), boldLen + 2), 0
                    );
                }
                return spannableStringBuilder;
            }

            if (!MessagesController.getInstance(currentAccount).getTopicsController().endIsReached(chat.id)) {
                MessagesController.getInstance(currentAccount).getTopicsController().preloadTopics(chat.id);
                return "Loading...";
            } else {
                return "no created topics";
            }
        }

        return null;
    }

    protected boolean isForumCell() {
        return !isDialogFolder() && chat != null && chat.forum && !isTopic;
    }

    private void drawCheckStatus(Canvas canvas, boolean drawClock, boolean drawCheck1, boolean drawCheck2, boolean moveCheck,  float alpha) {
        if (alpha == 0 && !moveCheck) {
            return;
        }
        float scale = 0.5f + 0.5f * alpha;
        if (drawClock) {
            setDrawableBounds(Theme.dialogs_clockDrawable, clockDrawLeft, checkDrawTop);
            if (alpha != 1f) {
                canvas.save();
                canvas.scale(scale, scale, Theme.dialogs_clockDrawable.getBounds().centerX(), Theme.dialogs_halfCheckDrawable.getBounds().centerY());
                Theme.dialogs_clockDrawable.setAlpha((int) (255 * alpha));
            }
            Theme.dialogs_clockDrawable.draw(canvas);
            if (alpha != 1f) {
                canvas.restore();
                Theme.dialogs_clockDrawable.setAlpha(255);
            }
            invalidate();
        } else if (drawCheck2) {
            if (drawCheck1) {
                setDrawableBounds(Theme.dialogs_halfCheckDrawable, halfCheckDrawLeft, checkDrawTop);
                if (moveCheck) {
                    canvas.save();
                    canvas.scale(scale, scale, Theme.dialogs_halfCheckDrawable.getBounds().centerX(), Theme.dialogs_halfCheckDrawable.getBounds().centerY());
                    Theme.dialogs_halfCheckDrawable.setAlpha((int) (255 * alpha));
                }
                if (!moveCheck && alpha != 0) {
                    canvas.save();
                    canvas.scale(scale, scale, Theme.dialogs_halfCheckDrawable.getBounds().centerX(), Theme.dialogs_halfCheckDrawable.getBounds().centerY());
                    Theme.dialogs_halfCheckDrawable.setAlpha((int) (255 * alpha));
                    Theme.dialogs_checkReadDrawable.setAlpha((int) (255 * alpha));
                }

                Theme.dialogs_halfCheckDrawable.draw(canvas);

                if (moveCheck) {
                    canvas.restore();
                    canvas.save();
                    canvas.translate(AndroidUtilities.dp(4) * (1f - alpha), 0);
                }
                setDrawableBounds(Theme.dialogs_checkReadDrawable, checkDrawLeft, checkDrawTop);
                Theme.dialogs_checkReadDrawable.draw(canvas);
                if (moveCheck) {
                    canvas.restore();
                    Theme.dialogs_halfCheckDrawable.setAlpha(255);
                }

                if (!moveCheck && alpha != 0) {
                    canvas.restore();
                    Theme.dialogs_halfCheckDrawable.setAlpha(255);
                    Theme.dialogs_checkReadDrawable.setAlpha(255);
                }
            } else {
                setDrawableBounds(Theme.dialogs_checkDrawable, checkDrawLeft1, checkDrawTop);
                if (alpha != 1f) {
                    canvas.save();
                    canvas.scale(scale, scale, Theme.dialogs_checkDrawable.getBounds().centerX(), Theme.dialogs_halfCheckDrawable.getBounds().centerY());
                    Theme.dialogs_checkDrawable.setAlpha((int) (255 * alpha));
                }
                Theme.dialogs_checkDrawable.draw(canvas);
                if (alpha != 1f) {
                    canvas.restore();
                    Theme.dialogs_checkDrawable.setAlpha(255);
                }
            }
        }
    }

    public boolean isPointInsideAvatar(float x, float y) {
        if (!LocaleController.isRTL) {
            return x >= 0 && x < AndroidUtilities.dp(60);
        } else {
            return x >= getMeasuredWidth() - AndroidUtilities.dp(60) && x < getMeasuredWidth();
        }
    }

    public void setDialogSelected(boolean value) {
        if (isSelected != value) {
            invalidate();
        }
        isSelected = value;
    }

    public boolean checkCurrentDialogIndex(boolean frozen) {
        if (parentFragment == null) {
            return false;
        }
        ArrayList<TLRPC.Dialog> dialogsArray = parentFragment.getDialogsArray(currentAccount, dialogsType, folderId, frozen);
        boolean requestLayout = false;
        if (index < dialogsArray.size()) {
            TLRPC.Dialog dialog = dialogsArray.get(index);
            TLRPC.Dialog nextDialog = index + 1 < dialogsArray.size() ? dialogsArray.get(index + 1) : null;
            TLRPC.DraftMessage newDraftMessage = MediaDataController.getInstance(currentAccount).getDraft(currentDialogId, 0);
            MessageObject newMessageObject;
            if (currentDialogFolderId != 0) {
                newMessageObject = findFolderTopMessage();
                groupMessages = null;
            } else {
                groupMessages = MessagesController.getInstance(currentAccount).dialogMessage.get(dialog.id);
                newMessageObject = groupMessages != null && groupMessages.size() > 0 ? groupMessages.get(0) : null;
            }
            if (currentDialogId != dialog.id ||
                    message != null && message.getId() != dialog.top_message ||
                    newMessageObject != null && newMessageObject.messageOwner.edit_date != currentEditDate ||
                    unreadCount != dialog.unread_count ||
                    mentionCount != dialog.unread_mentions_count ||
                    markUnread != dialog.unread_mark ||
                    message != newMessageObject ||
                    newDraftMessage != draftMessage || drawPin != dialog.pinned) {
                boolean dialogChanged = currentDialogId != dialog.id;

                if (isForum != MessagesController.getInstance(currentAccount).isForum(dialog.id)) {
                    requestLayout = true;
                }
                isForum = MessagesController.getInstance(currentAccount).isForum(dialog.id);

                currentDialogId = dialog.id;
                if (dialogChanged) {
                    lastDialogChangedTime = System.currentTimeMillis();
                    if (statusDrawableAnimator != null) {
                        statusDrawableAnimator.removeAllListeners();
                        statusDrawableAnimator.cancel();
                    }
                    statusDrawableAnimationInProgress = false;
                    lastStatusDrawableParams = -1;
                }
                if (dialog instanceof TLRPC.TL_dialogFolder) {
                    TLRPC.TL_dialogFolder dialogFolder = (TLRPC.TL_dialogFolder) dialog;
                    currentDialogFolderId = dialogFolder.folder.id;
                } else {
                    currentDialogFolderId = 0;
                }
                if (dialogsType == 7 || dialogsType == 8) {
                    MessagesController.DialogFilter filter = MessagesController.getInstance(currentAccount).selectedDialogFilter[dialogsType == 8 ? 1 : 0];
                    fullSeparator = dialog instanceof TLRPC.TL_dialog && nextDialog != null && filter != null && filter.pinnedDialogs.indexOfKey(dialog.id) >= 0 && filter.pinnedDialogs.indexOfKey(nextDialog.id) < 0;
                    fullSeparator2 = false;
                } else {
                    fullSeparator = dialog instanceof TLRPC.TL_dialog && dialog.pinned && nextDialog != null && !nextDialog.pinned;
                    fullSeparator2 = dialog instanceof TLRPC.TL_dialogFolder && nextDialog != null && !nextDialog.pinned;
                }
                update(0, !dialogChanged);
                if (dialogChanged) {
                    reorderIconProgress = drawPin && drawReorder ? 1.0f : 0.0f;
                }
                checkOnline();
                checkGroupCall();
                checkChatTheme();
            }
        }
        if (requestLayout) {
            requestLayout();
        }
        return requestLayout;
    }

    public void animateArchiveAvatar() {
        if (avatarDrawable.getAvatarType() != AvatarDrawable.AVATAR_TYPE_ARCHIVED) {
            return;
        }
        animatingArchiveAvatar = true;
        animatingArchiveAvatarProgress = 0.0f;
        Theme.dialogs_archiveAvatarDrawable.setProgress(0.0f);
        Theme.dialogs_archiveAvatarDrawable.start();
        invalidate();
    }

    public void setChecked(boolean checked, boolean animated) {
        if (checkBox == null) {
            checkBox = new CheckBox2(getContext(), 21, resourcesProvider);
            checkBox.setColor(null, Theme.key_windowBackgroundWhite, Theme.key_checkboxCheck);
            checkBox.setDrawUnchecked(false);
            checkBox.setDrawBackgroundAsArc(3);
            addView(checkBox);
        }
        checkBox.setChecked(checked, animated);
    }

    private MessageObject findFolderTopMessage() {
        if (parentFragment == null) {
            return null;
        }
        ArrayList<TLRPC.Dialog> dialogs = parentFragment.getDialogsArray(currentAccount, dialogsType, currentDialogFolderId, false);
        MessageObject maxMessage = null;
        if (!dialogs.isEmpty()) {
            for (int a = 0, N = dialogs.size(); a < N; a++) {
                TLRPC.Dialog dialog = dialogs.get(a);
                ArrayList<MessageObject> groupMessages = MessagesController.getInstance(currentAccount).dialogMessage.get(dialog.id);
                MessageObject object = groupMessages != null && groupMessages.size() > 0 ? groupMessages.get(0) : null;
                if (object != null && (maxMessage == null || object.messageOwner.date > maxMessage.messageOwner.date)) {
                    maxMessage = object;
                }
                if (dialog.pinnedNum == 0 && maxMessage != null) {
                    break;
                }
            }
        }
        return maxMessage;
    }

    public boolean isFolderCell() {
        return currentDialogFolderId != 0;
    }

    public boolean update(int mask) {
        return update(mask, true);
    }

    public boolean update(int mask, boolean animated) {
        boolean requestLayout = false;
        if (customDialog != null) {
            lastMessageDate = customDialog.date;
            lastUnreadState = customDialog.unread_count != 0;
            unreadCount = customDialog.unread_count;
            drawPin = customDialog.pinned;
            dialogMuted = customDialog.muted;
            hasUnmutedTopics = false;
            avatarDrawable.setInfo(customDialog.id, customDialog.name, null);
            avatarImage.setImage(null, "50_50", avatarDrawable, null, 0);
            for (int i = 0; i < thumbImage.length; ++i) {
                thumbImage[i].setImageBitmap((BitmapDrawable) null);
            }
            avatarImage.setRoundRadius(AndroidUtilities.dp(28));
            drawUnmute = false;
        } else {
            int oldUnreadCount = unreadCount;
            boolean oldHasReactionsMentions = reactionMentionCount != 0;
            boolean oldMarkUnread = markUnread;
            boolean oldIsForumCell = isForumCell();
            hasUnmutedTopics = false;
            readOutboxMaxId = -1;
            if (isDialogCell) {
                TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogs_dict.get(currentDialogId);
                if (dialog != null) {
                    readOutboxMaxId = dialog.read_outbox_max_id;
                    if (mask == 0) {
                        clearingDialog = MessagesController.getInstance(currentAccount).isClearingDialog(dialog.id);
                        groupMessages = MessagesController.getInstance(currentAccount).dialogMessage.get(dialog.id);
                        message = groupMessages != null && groupMessages.size() > 0 ? groupMessages.get(0) : null;
                        lastUnreadState = message != null && message.isUnread();
                        TLRPC.Chat localChat = MessagesController.getInstance(currentAccount).getChat(-dialog.id);
                        boolean isForumCell = localChat != null && localChat.forum && !isTopic;
                        if (isForumCell != oldIsForumCell) {
                            requestLayout = true;
                        }
                        if (localChat != null && localChat.forum) {
                            int[] counts = MessagesController.getInstance(currentAccount).getTopicsController().getForumUnreadCount(localChat.id);
                            unreadCount = counts[0];
                            mentionCount = counts[1];
                            reactionMentionCount = counts[2];
                            hasUnmutedTopics = counts[3] != 0;
                        } else if (dialog instanceof TLRPC.TL_dialogFolder) {
                            unreadCount = MessagesStorage.getInstance(currentAccount).getArchiveUnreadCount();
                            mentionCount = 0;
                            reactionMentionCount = 0;
                        } else {
                            unreadCount = dialog.unread_count;
                            mentionCount = dialog.unread_mentions_count;
                            reactionMentionCount = dialog.unread_reactions_count;
                        }
                        markUnread = dialog.unread_mark;
                        currentEditDate = message != null ? message.messageOwner.edit_date : 0;
                        lastMessageDate = dialog.last_message_date;
                        if (dialogsType == 7 || dialogsType == 8) {
                            MessagesController.DialogFilter filter = MessagesController.getInstance(currentAccount).selectedDialogFilter[dialogsType == 8 ? 1 : 0];
                            drawPin = filter != null && filter.pinnedDialogs.indexOfKey(dialog.id) >= 0;
                        } else {
                            drawPin = currentDialogFolderId == 0 && dialog.pinned;
                        }
                        if (message != null) {
                            lastSendState = message.messageOwner.send_state;
                        }
                    }
                } else {
                    unreadCount = 0;
                    mentionCount = 0;
                    reactionMentionCount = 0;
                    currentEditDate = 0;
                    lastMessageDate = 0;
                    clearingDialog = false;
                }
            } else {
                drawPin = false;
            }
            if (forumTopic != null) {
                unreadCount = forumTopic.unread_count;
                mentionCount = forumTopic.unread_mentions_count;
                reactionMentionCount = forumTopic.unread_reactions_count;
            }
            if (dialogsType == 2) {
                drawPin = false;
            }

            if (mask != 0) {
                boolean continueUpdate = false;
                if (user != null && (mask & MessagesController.UPDATE_MASK_STATUS) != 0) {
                    user = MessagesController.getInstance(currentAccount).getUser(user.id);
                    invalidate();
                }
                if (user != null && (mask & MessagesController.UPDATE_MASK_EMOJI_STATUS) != 0) {
                    user = MessagesController.getInstance(currentAccount).getUser(user.id);
                    if (user.emoji_status instanceof TLRPC.TL_emojiStatusUntil && ((TLRPC.TL_emojiStatusUntil) user.emoji_status).until > (int) (System.currentTimeMillis() / 1000)) {
                        nameLayoutEllipsizeByGradient = true;
                        emojiStatus.set(((TLRPC.TL_emojiStatusUntil) user.emoji_status).document_id, animated);
                    } else if (user.emoji_status instanceof TLRPC.TL_emojiStatus) {
                        nameLayoutEllipsizeByGradient = true;
                        emojiStatus.set(((TLRPC.TL_emojiStatus) user.emoji_status).document_id, animated);
                    } else {
                        nameLayoutEllipsizeByGradient = true;
                        emojiStatus.set(PremiumGradient.getInstance().premiumStarDrawableMini, animated);
                    }
                    invalidate();
                }
                if (isDialogCell || isTopic) {
                    if ((mask & MessagesController.UPDATE_MASK_USER_PRINT) != 0) {
                        CharSequence printString = MessagesController.getInstance(currentAccount).getPrintingString(currentDialogId, getTopicId(), true);
                        if (lastPrintString != null && printString == null || lastPrintString == null && printString != null || lastPrintString != null && !lastPrintString.equals(printString)) {
                            continueUpdate = true;
                        }
                    }
                }
                if (!continueUpdate && (mask & MessagesController.UPDATE_MASK_MESSAGE_TEXT) != 0) {
                    if (message != null && message.messageText != lastMessageString) {
                        continueUpdate = true;
                    }
                }
                if (!continueUpdate && (mask & MessagesController.UPDATE_MASK_CHAT) != 0 && chat != null) {
                    TLRPC.Chat newChat = MessagesController.getInstance(currentAccount).getChat(chat.id);
                    if ((newChat.call_active && newChat.call_not_empty) != hasCall) {
                        continueUpdate = true;
                    }
                }
                if (!continueUpdate && (mask & MessagesController.UPDATE_MASK_AVATAR) != 0) {
                    if (chat == null) {
                        continueUpdate = true;
                    }
                }
                if (!continueUpdate && (mask & MessagesController.UPDATE_MASK_NAME) != 0) {
                    if (chat == null) {
                        continueUpdate = true;
                    }
                }
                if (!continueUpdate && (mask & MessagesController.UPDATE_MASK_CHAT_AVATAR) != 0) {
                    if (user == null) {
                        continueUpdate = true;
                    }
                }
                if (!continueUpdate && (mask & MessagesController.UPDATE_MASK_CHAT_NAME) != 0) {
                    if (user == null) {
                        continueUpdate = true;
                    }
                }
                if (!continueUpdate) {
                    if (message != null && lastUnreadState != message.isUnread()) {
                        lastUnreadState = message.isUnread();
                        continueUpdate = true;
                    }
                    if (isDialogCell) {
                        TLRPC.Dialog dialog = MessagesController.getInstance(currentAccount).dialogs_dict.get(currentDialogId);
                        int newCount;
                        int newMentionCount;
                        int newReactionCout = 0;

                        TLRPC.Chat localChat = dialog == null ? null : MessagesController.getInstance(currentAccount).getChat(-dialog.id);
                        if (localChat != null && localChat.forum) {
                            int[] counts = MessagesController.getInstance(currentAccount).getTopicsController().getForumUnreadCount(localChat.id);
                            newCount = counts[0];
                            newMentionCount = counts[1];
                            newReactionCout = counts[2];
                            hasUnmutedTopics = counts[3] != 0;
                        } else if (dialog instanceof TLRPC.TL_dialogFolder) {
                            newCount = MessagesStorage.getInstance(currentAccount).getArchiveUnreadCount();
                            newMentionCount = 0;
                        } else if (dialog != null) {
                            newCount = dialog.unread_count;
                            newMentionCount = dialog.unread_mentions_count;
                            newReactionCout = dialog.unread_reactions_count;
                        } else {
                            newCount = 0;
                            newMentionCount = 0;
                        }
                        if (dialog != null && (unreadCount != newCount || markUnread != dialog.unread_mark || mentionCount != newMentionCount || reactionMentionCount != newReactionCout)) {
                            unreadCount = newCount;
                            mentionCount = newMentionCount;
                            markUnread = dialog.unread_mark;
                            reactionMentionCount = newReactionCout;
                            continueUpdate = true;
                        }
                    }
                }
                if (!continueUpdate && (mask & MessagesController.UPDATE_MASK_SEND_STATE) != 0) {
                    if (message != null && lastSendState != message.messageOwner.send_state) {
                        lastSendState = message.messageOwner.send_state;
                        continueUpdate = true;
                    }
                }

                if (!continueUpdate) {
                    invalidate();
                    return requestLayout;
                }
            }

            user = null;
            chat = null;
            encryptedChat = null;

            long dialogId;
            if (currentDialogFolderId != 0) {
                dialogMuted = false;
                drawUnmute = false;
                message = findFolderTopMessage();
                if (message != null) {
                    dialogId = message.getDialogId();
                } else {
                    dialogId = 0;
                }
            } else {
                drawUnmute = false;
                if (forumTopic != null) {
                    boolean allDialogMuted = MessagesController.getInstance(currentAccount).isDialogMuted(currentDialogId, 0);
                    topicMuted = MessagesController.getInstance(currentAccount).isDialogMuted(currentDialogId, forumTopic.id);
                    if (allDialogMuted == topicMuted) {
                        dialogMuted = false;
                        drawUnmute = false;
                    } else {
                        dialogMuted = topicMuted;
                        drawUnmute = !topicMuted;
                    }
                } else {
                    dialogMuted = isDialogCell && MessagesController.getInstance(currentAccount).isDialogMuted(currentDialogId, getTopicId());
                }


                dialogId = currentDialogId;
            }

            if (dialogId != 0) {
                if (DialogObject.isEncryptedDialog(dialogId)) {
                    encryptedChat = MessagesController.getInstance(currentAccount).getEncryptedChat(DialogObject.getEncryptedChatId(dialogId));
                    if (encryptedChat != null) {
                        user = MessagesController.getInstance(currentAccount).getUser(encryptedChat.user_id);
                    }
                } else if (DialogObject.isUserDialog(dialogId)) {
                    user = MessagesController.getInstance(currentAccount).getUser(dialogId);
                } else {
                    chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
                    if (!isDialogCell && chat != null && chat.migrated_to != null) {
                        TLRPC.Chat chat2 = MessagesController.getInstance(currentAccount).getChat(chat.migrated_to.channel_id);
                        if (chat2 != null) {
                            chat = chat2;
                        }
                    }
                }
                if (useMeForMyMessages && user != null && message.isOutOwner()) {
                    user = MessagesController.getInstance(currentAccount).getUser(UserConfig.getInstance(currentAccount).clientUserId);
                }
            }

            if (currentDialogFolderId != 0) {
                Theme.dialogs_archiveAvatarDrawable.setCallback(this);
                avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_ARCHIVED);
                avatarImage.setImage(null, null, avatarDrawable, null, user, 0);
            } else {
                if (user != null) {
                    avatarDrawable.setInfo(user);
                    if (UserObject.isReplyUser(user)) {
                        avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_REPLIES);
                        avatarImage.setImage(null, null, avatarDrawable, null, user, 0);
                    } else if (UserObject.isUserSelf(user) && !useMeForMyMessages) {
                        avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_SAVED);
                        avatarImage.setImage(null, null, avatarDrawable, null, user, 0);
                    } else {
                        avatarImage.setForUserOrChat(user, avatarDrawable, null, true);
                    }
                } else if (chat != null) {
                    avatarDrawable.setInfo(chat);
                    avatarImage.setForUserOrChat(chat, avatarDrawable);
                }
            }

            if (animated && (oldUnreadCount != unreadCount || oldMarkUnread != markUnread) && (!isDialogCell || (System.currentTimeMillis() - lastDialogChangedTime) > 100)) {
                if (countAnimator != null) {
                    countAnimator.cancel();
                }
                countAnimator = ValueAnimator.ofFloat(0, 1f);
                countAnimator.addUpdateListener(valueAnimator -> {
                    countChangeProgress = (float) valueAnimator.getAnimatedValue();
                    invalidate();
                });
                countAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        countChangeProgress = 1f;
                        countOldLayout = null;
                        countAnimationStableLayout = null;
                        countAnimationInLayout = null;
                        invalidate();
                    }
                });
                if ((oldUnreadCount == 0 || markUnread) && !(!markUnread && oldMarkUnread)) {
                    countAnimator.setDuration(220);
                    countAnimator.setInterpolator(new OvershootInterpolator());
                } else if (unreadCount == 0) {
                    countAnimator.setDuration(150);
                    countAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
                } else {
                    countAnimator.setDuration(430);
                    countAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
                }
                if (drawCount && drawCount2 && countLayout != null) {
                    String oldStr = String.format("%d", oldUnreadCount);
                    String newStr = String.format("%d", unreadCount);

                    if (oldStr.length() == newStr.length()) {
                        SpannableStringBuilder oldSpannableStr = new SpannableStringBuilder(oldStr);
                        SpannableStringBuilder newSpannableStr = new SpannableStringBuilder(newStr);
                        SpannableStringBuilder stableStr = new SpannableStringBuilder(newStr);
                        for (int i = 0; i < oldStr.length(); i++) {
                            if (oldStr.charAt(i) == newStr.charAt(i)) {
                                oldSpannableStr.setSpan(new EmptyStubSpan(), i, i + 1, 0);
                                newSpannableStr.setSpan(new EmptyStubSpan(), i, i + 1, 0);
                            } else {
                                stableStr.setSpan(new EmptyStubSpan(), i, i + 1, 0);
                            }
                        }

                        int countOldWidth = Math.max(AndroidUtilities.dp(12), (int) Math.ceil(Theme.dialogs_countTextPaint.measureText(oldStr)));
                        countOldLayout = new StaticLayout(oldSpannableStr, Theme.dialogs_countTextPaint, countOldWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                        countAnimationStableLayout = new StaticLayout(stableStr, Theme.dialogs_countTextPaint, countOldWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                        countAnimationInLayout = new StaticLayout(newSpannableStr, Theme.dialogs_countTextPaint, countOldWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                    } else {
                        countOldLayout = countLayout;
                    }
                }
                countWidthOld = countWidth;
                countLeftOld = countLeft;
                countAnimationIncrement = unreadCount > oldUnreadCount;
                countAnimator.start();
            }

            boolean newHasReactionsMentions = reactionMentionCount != 0;
            if (animated && (newHasReactionsMentions != oldHasReactionsMentions)) {
                if (reactionsMentionsAnimator != null) {
                    reactionsMentionsAnimator.cancel();
                }
                reactionsMentionsChangeProgress = 0;
                reactionsMentionsAnimator = ValueAnimator.ofFloat(0, 1f);
                reactionsMentionsAnimator.addUpdateListener(valueAnimator -> {
                    reactionsMentionsChangeProgress = (float) valueAnimator.getAnimatedValue();
                    invalidate();
                });
                reactionsMentionsAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        reactionsMentionsChangeProgress = 1f;
                        invalidate();
                    }
                });
                if (newHasReactionsMentions) {
                    reactionsMentionsAnimator.setDuration(220);
                    reactionsMentionsAnimator.setInterpolator(new OvershootInterpolator());
                } else {
                    reactionsMentionsAnimator.setDuration(150);
                    reactionsMentionsAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
                }
                reactionsMentionsAnimator.start();
            }

            avatarImage.setRoundRadius(chat != null && chat.forum && currentDialogFolderId == 0 ? AndroidUtilities.dp(16) : AndroidUtilities.dp(28));
        }
        if (!isTopic && (getMeasuredWidth() != 0 || getMeasuredHeight() != 0)) {
            buildLayout();
        } else {
            requestLayout();
        }

        if (!animated) {
            dialogMutedProgress = (dialogMuted || drawUnmute) ? 1f : 0f;
            if (countAnimator != null) {
                countAnimator.cancel();
            }
        }

        invalidate();
        return requestLayout;
    }

    private int getTopicId() {
        return forumTopic == null ? 0 : forumTopic.id;
    }

    @Override
    public float getTranslationX() {
        return translationX;
    }

    @Override
    public void setTranslationX(float value) {
        if (value == translationX) {
            return;
        }
        translationX = value;
        if (translationDrawable != null && translationX == 0) {
            translationDrawable.setProgress(0.0f);
            translationAnimationStarted = false;
            archiveHidden = SharedConfig.archiveHidden;
            currentRevealProgress = 0;
            isSliding = false;
        }
        if (translationX != 0) {
            isSliding = true;
        } else {
            currentRevealBounceProgress = 0f;
            currentRevealProgress = 0f;
            drawRevealBackground = false;
        }
        if (isSliding && !swipeCanceled) {
            boolean prevValue = drawRevealBackground;
            drawRevealBackground = Math.abs(translationX) >= getMeasuredWidth() * 0.45f;
            if (prevValue != drawRevealBackground && archiveHidden == SharedConfig.archiveHidden) {
                try {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                } catch (Exception ignore) {

                }
            }
        }
        invalidate();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        if (currentDialogId == 0 && customDialog == null) {
            return;
        }

        boolean needInvalidate = false;

        if (currentDialogFolderId != 0 && archivedChatsDrawable != null && archivedChatsDrawable.outProgress == 0.0f && translationX == 0.0f) {
            if (!drawingForBlur) {
                canvas.save();
                canvas.clipRect(0, 0, getMeasuredWidth(), getMeasuredHeight());
                archivedChatsDrawable.draw(canvas);
                canvas.restore();
            }
            return;
        }

        long newTime = SystemClock.elapsedRealtime();
        long dt = newTime - lastUpdateTime;
        if (dt > 17) {
            dt = 17;
        }
        lastUpdateTime = newTime;

        if (clipProgress != 0.0f && Build.VERSION.SDK_INT != 24) {
            canvas.save();
            canvas.clipRect(0, topClip * clipProgress, getMeasuredWidth(), getMeasuredHeight() - (int) (bottomClip * clipProgress));
        }

        int backgroundColor = 0;
        if (translationX != 0 || cornerProgress != 0.0f) {
            canvas.save();
            String swipeMessage;
            int revealBackgroundColor;
            int swipeMessageStringId;
            if (currentDialogFolderId != 0) {
                if (archiveHidden) {
                    backgroundColor = Theme.getColor(Theme.key_chats_archivePinBackground, resourcesProvider);
                    revealBackgroundColor = Theme.getColor(Theme.key_chats_archiveBackground, resourcesProvider);
                    swipeMessage = LocaleController.getString("UnhideFromTop", swipeMessageStringId = R.string.UnhideFromTop);
                    translationDrawable = Theme.dialogs_unpinArchiveDrawable;
                } else {
                    backgroundColor = Theme.getColor(Theme.key_chats_archiveBackground, resourcesProvider);
                    revealBackgroundColor = Theme.getColor(Theme.key_chats_archivePinBackground, resourcesProvider);
                    swipeMessage = LocaleController.getString("HideOnTop", swipeMessageStringId = R.string.HideOnTop);
                    translationDrawable = Theme.dialogs_pinArchiveDrawable;
                }
            } else {
                if (promoDialog) {
                    backgroundColor = Theme.getColor(Theme.key_chats_archiveBackground, resourcesProvider);
                    revealBackgroundColor = Theme.getColor(Theme.key_chats_archivePinBackground, resourcesProvider);
                    swipeMessage = LocaleController.getString("PsaHide", swipeMessageStringId = R.string.PsaHide);
                    translationDrawable = Theme.dialogs_hidePsaDrawable;
                } else if (folderId == 0) {
                    backgroundColor = Theme.getColor(Theme.key_chats_archiveBackground, resourcesProvider);
                    revealBackgroundColor = Theme.getColor(Theme.key_chats_archivePinBackground, resourcesProvider);
                    if (SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_MUTE) {
                        if (dialogMuted) {
                            swipeMessage = LocaleController.getString("SwipeUnmute", swipeMessageStringId = R.string.SwipeUnmute);
                            translationDrawable = Theme.dialogs_swipeUnmuteDrawable;
                        } else {
                            swipeMessage = LocaleController.getString("SwipeMute", swipeMessageStringId = R.string.SwipeMute);
                            translationDrawable = Theme.dialogs_swipeMuteDrawable;
                        }
                    } else if (SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_DELETE) {
                        swipeMessage = LocaleController.getString("SwipeDeleteChat", swipeMessageStringId = R.string.SwipeDeleteChat);
                        backgroundColor = Theme.getColor(Theme.key_dialogSwipeRemove, resourcesProvider);
                        translationDrawable = Theme.dialogs_swipeDeleteDrawable;
                    } else if (SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_READ) {
                        if (unreadCount > 0 || markUnread) {
                            swipeMessage = LocaleController.getString("SwipeMarkAsRead", swipeMessageStringId = R.string.SwipeMarkAsRead);
                            translationDrawable = Theme.dialogs_swipeReadDrawable;
                        } else {
                            swipeMessage = LocaleController.getString("SwipeMarkAsUnread", swipeMessageStringId = R.string.SwipeMarkAsUnread);
                            translationDrawable = Theme.dialogs_swipeUnreadDrawable;
                        }
                    } else if (SharedConfig.getChatSwipeAction(currentAccount) == SwipeGestureSettingsView.SWIPE_GESTURE_PIN) {
                        if (getIsPinned()) {
                            swipeMessage = LocaleController.getString("SwipeUnpin", swipeMessageStringId = R.string.SwipeUnpin);
                            translationDrawable = Theme.dialogs_swipeUnpinDrawable;
                        } else {
                            swipeMessage = LocaleController.getString("SwipePin", swipeMessageStringId = R.string.SwipePin);
                            translationDrawable = Theme.dialogs_swipePinDrawable;
                        }
                    } else {
                        swipeMessage = LocaleController.getString("Archive", swipeMessageStringId = R.string.Archive);
                        translationDrawable = Theme.dialogs_archiveDrawable;
                    }
                } else {
                    backgroundColor = Theme.getColor(Theme.key_chats_archivePinBackground, resourcesProvider);
                    revealBackgroundColor = Theme.getColor(Theme.key_chats_archiveBackground, resourcesProvider);
                    swipeMessage = LocaleController.getString("Unarchive", swipeMessageStringId = R.string.Unarchive);
                    translationDrawable = Theme.dialogs_unarchiveDrawable;
                }
            }

            if (swipeCanceled && lastDrawTranslationDrawable != null) {
                translationDrawable = lastDrawTranslationDrawable;
                swipeMessageStringId = lastDrawSwipeMessageStringId;
            } else {
                lastDrawTranslationDrawable = translationDrawable;
                lastDrawSwipeMessageStringId = swipeMessageStringId;
            }

            if (!translationAnimationStarted && Math.abs(translationX) > AndroidUtilities.dp(43)) {
                translationAnimationStarted = true;
                translationDrawable.setProgress(0.0f);
                translationDrawable.setCallback(this);
                translationDrawable.start();
            }

            float tx = getMeasuredWidth() + translationX;
            if (currentRevealProgress < 1.0f) {
                Theme.dialogs_pinnedPaint.setColor(backgroundColor);
                canvas.drawRect(tx - AndroidUtilities.dp(8), 0, getMeasuredWidth(), getMeasuredHeight(), Theme.dialogs_pinnedPaint);
                if (currentRevealProgress == 0) {
                    if (Theme.dialogs_archiveDrawableRecolored) {
                        Theme.dialogs_archiveDrawable.setLayerColor("Arrow.**", Theme.getNonAnimatedColor(Theme.key_chats_archiveBackground));
                        Theme.dialogs_archiveDrawableRecolored = false;
                    }
                    if (Theme.dialogs_hidePsaDrawableRecolored) {
                        Theme.dialogs_hidePsaDrawable.beginApplyLayerColors();
                        Theme.dialogs_hidePsaDrawable.setLayerColor("Line 1.**", Theme.getNonAnimatedColor(Theme.key_chats_archiveBackground));
                        Theme.dialogs_hidePsaDrawable.setLayerColor("Line 2.**", Theme.getNonAnimatedColor(Theme.key_chats_archiveBackground));
                        Theme.dialogs_hidePsaDrawable.setLayerColor("Line 3.**", Theme.getNonAnimatedColor(Theme.key_chats_archiveBackground));
                        Theme.dialogs_hidePsaDrawable.commitApplyLayerColors();
                        Theme.dialogs_hidePsaDrawableRecolored = false;
                    }
                }
            }
            int drawableX = getMeasuredWidth() - AndroidUtilities.dp(43) - translationDrawable.getIntrinsicWidth() / 2;
            int drawableY = AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 12 : 9);
            int drawableCx = drawableX + translationDrawable.getIntrinsicWidth() / 2;
            int drawableCy = drawableY + translationDrawable.getIntrinsicHeight() / 2;

            if (currentRevealProgress > 0.0f) {
                canvas.save();
                canvas.clipRect(tx - AndroidUtilities.dp(8), 0, getMeasuredWidth(), getMeasuredHeight());
                Theme.dialogs_pinnedPaint.setColor(revealBackgroundColor);

                float rad = (float) Math.sqrt(drawableCx * drawableCx + (drawableCy - getMeasuredHeight()) * (drawableCy - getMeasuredHeight()));
                canvas.drawCircle(drawableCx, drawableCy, rad * AndroidUtilities.accelerateInterpolator.getInterpolation(currentRevealProgress), Theme.dialogs_pinnedPaint);
                canvas.restore();

                if (!Theme.dialogs_archiveDrawableRecolored) {
                    Theme.dialogs_archiveDrawable.setLayerColor("Arrow.**", Theme.getNonAnimatedColor(Theme.key_chats_archivePinBackground));
                    Theme.dialogs_archiveDrawableRecolored = true;
                }
                if (!Theme.dialogs_hidePsaDrawableRecolored) {
                    Theme.dialogs_hidePsaDrawable.beginApplyLayerColors();
                    Theme.dialogs_hidePsaDrawable.setLayerColor("Line 1.**", Theme.getNonAnimatedColor(Theme.key_chats_archivePinBackground));
                    Theme.dialogs_hidePsaDrawable.setLayerColor("Line 2.**", Theme.getNonAnimatedColor(Theme.key_chats_archivePinBackground));
                    Theme.dialogs_hidePsaDrawable.setLayerColor("Line 3.**", Theme.getNonAnimatedColor(Theme.key_chats_archivePinBackground));
                    Theme.dialogs_hidePsaDrawable.commitApplyLayerColors();
                    Theme.dialogs_hidePsaDrawableRecolored = true;
                }
            }

            canvas.save();
            canvas.translate(drawableX, drawableY);
            if (currentRevealBounceProgress != 0.0f && currentRevealBounceProgress != 1.0f) {
                float scale = 1.0f + interpolator.getInterpolation(currentRevealBounceProgress);
                canvas.scale(scale, scale, translationDrawable.getIntrinsicWidth() / 2, translationDrawable.getIntrinsicHeight() / 2);
            }
            setDrawableBounds(translationDrawable, 0, 0);
            translationDrawable.draw(canvas);
            canvas.restore();

            canvas.clipRect(tx, 0, getMeasuredWidth(), getMeasuredHeight());

            int width = (int) Math.ceil(Theme.dialogs_countTextPaint.measureText(swipeMessage));

            if (swipeMessageTextId != swipeMessageStringId || swipeMessageWidth != getMeasuredWidth()) {
                swipeMessageTextId = swipeMessageStringId;
                swipeMessageWidth = getMeasuredWidth();
                swipeMessageTextLayout = new StaticLayout(swipeMessage, Theme.dialogs_archiveTextPaint, Math.min(AndroidUtilities.dp(80), width), Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);

                if (swipeMessageTextLayout.getLineCount() > 1) {
                    swipeMessageTextLayout = new StaticLayout(swipeMessage, Theme.dialogs_archiveTextPaintSmall, Math.min(AndroidUtilities.dp(82), width), Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                }
            }

            if (swipeMessageTextLayout != null) {
                canvas.save();
                float yOffset = swipeMessageTextLayout.getLineCount() > 1 ? -AndroidUtilities.dp(4) : 0;
                canvas.translate(getMeasuredWidth() - AndroidUtilities.dp(43) - swipeMessageTextLayout.getWidth() / 2f, AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 50 : 47) + yOffset);
                swipeMessageTextLayout.draw(canvas);
                canvas.restore();
            }
            canvas.restore();
        } else if (translationDrawable != null) {
            translationDrawable.stop();
            translationDrawable.setProgress(0.0f);
            translationDrawable.setCallback(null);
            translationDrawable = null;
            translationAnimationStarted = false;
        }

        if (translationX != 0) {
            canvas.save();
            canvas.translate(translationX, 0);
        }

        float cornersRadius = AndroidUtilities.dp(8) * cornerProgress;
        if (isSelected) {
            rect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
            canvas.drawRoundRect(rect, cornersRadius, cornersRadius, Theme.dialogs_tabletSeletedPaint);
        }
        if (currentDialogFolderId != 0 && (!SharedConfig.archiveHidden || archiveBackgroundProgress != 0)) {
            Theme.dialogs_pinnedPaint.setColor(AndroidUtilities.getOffsetColor(0, Theme.getColor(Theme.key_chats_pinnedOverlay, resourcesProvider), archiveBackgroundProgress, 1.0f));
            canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight() - translateY, Theme.dialogs_pinnedPaint);
        } else if (getIsPinned() || drawPinBackground) {
            Theme.dialogs_pinnedPaint.setColor(Theme.getColor(Theme.key_chats_pinnedOverlay, resourcesProvider));
            canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight() - translateY, Theme.dialogs_pinnedPaint);
        }

        if (translationX != 0 || cornerProgress != 0.0f) {
            canvas.save();

            Theme.dialogs_pinnedPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
            rect.set(getMeasuredWidth() - AndroidUtilities.dp(64), 0, getMeasuredWidth(), getMeasuredHeight());
            canvas.drawRoundRect(rect, cornersRadius, cornersRadius, Theme.dialogs_pinnedPaint);

            if (isSelected) {
                canvas.drawRoundRect(rect, cornersRadius, cornersRadius, Theme.dialogs_tabletSeletedPaint);
            }

            if (currentDialogFolderId != 0 && (!SharedConfig.archiveHidden || archiveBackgroundProgress != 0)) {
                Theme.dialogs_pinnedPaint.setColor(AndroidUtilities.getOffsetColor(0, Theme.getColor(Theme.key_chats_pinnedOverlay, resourcesProvider), archiveBackgroundProgress, 1.0f));
                canvas.drawRoundRect(rect, cornersRadius, cornersRadius, Theme.dialogs_pinnedPaint);
            } else if (getIsPinned() || drawPinBackground) {
                Theme.dialogs_pinnedPaint.setColor(Theme.getColor(Theme.key_chats_pinnedOverlay, resourcesProvider));
                canvas.drawRoundRect(rect, cornersRadius, cornersRadius, Theme.dialogs_pinnedPaint);
            }
            canvas.restore();
        }

        if (translationX != 0) {
            if (cornerProgress < 1.0f) {
                cornerProgress += dt / 150.0f;
                if (cornerProgress > 1.0f) {
                    cornerProgress = 1.0f;
                }
                needInvalidate = true;
            }
        } else if (cornerProgress > 0.0f) {
            cornerProgress -= dt / 150.0f;
            if (cornerProgress < 0.0f) {
                cornerProgress = 0.0f;
            }
            needInvalidate = true;
        }

        if (drawNameLock) {
            setDrawableBounds(Theme.dialogs_lockDrawable, nameLockLeft, nameLockTop);
            Theme.dialogs_lockDrawable.draw(canvas);
        }

        if (nameLayout != null) {
            if (nameLayoutEllipsizeByGradient && !nameLayoutFits) {
                if (nameLayoutEllipsizeLeft && fadePaint == null) {
                    fadePaint = new Paint();
                    fadePaint.setShader(new LinearGradient(0, 0, AndroidUtilities.dp(24), 0, new int[]{0xffffffff, 0}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
                    fadePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
                } else if (fadePaintBack == null) {
                    fadePaintBack = new Paint();
                    fadePaintBack.setShader(new LinearGradient(0, 0, AndroidUtilities.dp(24), 0, new int[]{0, 0xffffffff}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
                    fadePaintBack.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
                }
                canvas.saveLayerAlpha(0, 0, getMeasuredWidth(), getMeasuredHeight(), 255, Canvas.ALL_SAVE_FLAG);
                canvas.clipRect(nameLeft, 0, nameLeft + nameWidth, getMeasuredHeight());
            }
            if (currentDialogFolderId != 0) {
                Theme.dialogs_namePaint[paintIndex].setColor(Theme.dialogs_namePaint[paintIndex].linkColor = Theme.getColor(Theme.key_chats_nameArchived, resourcesProvider));
            } else if (encryptedChat != null || customDialog != null && customDialog.type == 2) {
                Theme.dialogs_namePaint[paintIndex].setColor(Theme.dialogs_namePaint[paintIndex].linkColor = Theme.getColor(Theme.key_chats_secretName, resourcesProvider));
            } else {
                Theme.dialogs_namePaint[paintIndex].setColor(Theme.dialogs_namePaint[paintIndex].linkColor = Theme.getColor(Theme.key_chats_name, resourcesProvider));
            }
            canvas.save();
            canvas.translate(nameLeft + nameLayoutTranslateX, AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 10 : 13));
            nameLayout.draw(canvas);
            canvas.restore();
            if (nameLayoutEllipsizeByGradient && !nameLayoutFits) {
                canvas.save();
                if (nameLayoutEllipsizeLeft) {
                    canvas.translate(nameLeft , 0);
                    canvas.drawRect(0, 0, AndroidUtilities.dp(24), getMeasuredHeight(), fadePaint);
                } else {
                    canvas.translate(nameLeft + nameWidth - AndroidUtilities.dp(24), 0);
                    canvas.drawRect(0, 0, AndroidUtilities.dp(24), getMeasuredHeight(), fadePaintBack);
                }
                canvas.restore();
                canvas.restore();
            }
        }

        if (timeLayout != null && currentDialogFolderId == 0) {
            canvas.save();
            canvas.translate(timeLeft, timeTop);
            timeLayout.draw(canvas);
            canvas.restore();
        }

        if (drawLock2()) {
            Theme.dialogs_lock2Drawable.setBounds(
                lock2Left,
                timeTop + (timeLayout.getHeight() - Theme.dialogs_lock2Drawable.getIntrinsicHeight()) / 2,
                lock2Left + Theme.dialogs_lock2Drawable.getIntrinsicWidth(),
                timeTop + (timeLayout.getHeight() - Theme.dialogs_lock2Drawable.getIntrinsicHeight()) / 2 + Theme.dialogs_lock2Drawable.getIntrinsicHeight()
            );
            Theme.dialogs_lock2Drawable.draw(canvas);
        }

        if (messageNameLayout != null && !isForumCell()) {
            if (currentDialogFolderId != 0) {
                Theme.dialogs_messageNamePaint.setColor(Theme.dialogs_messageNamePaint.linkColor = Theme.getColor(Theme.key_chats_nameMessageArchived_threeLines, resourcesProvider));
            } else if (draftMessage != null) {
                Theme.dialogs_messageNamePaint.setColor(Theme.dialogs_messageNamePaint.linkColor = Theme.getColor(Theme.key_chats_draft, resourcesProvider));
            } else {
                Theme.dialogs_messageNamePaint.setColor(Theme.dialogs_messageNamePaint.linkColor = Theme.getColor(Theme.key_chats_nameMessage_threeLines, resourcesProvider));
            }
            canvas.save();
            canvas.translate(messageNameLeft, messageNameTop);
            try {
                messageNameLayout.draw(canvas);
                AnimatedEmojiSpan.drawAnimatedEmojis(canvas, messageNameLayout, animatedEmojiStack2, -.075f, null, 0, 0, 0, 1f);
            } catch (Exception e) {
                FileLog.e(e);
            }
            canvas.restore();
        }

        if (messageLayout != null) {
            if (currentDialogFolderId != 0) {
                if (chat != null) {
                    Theme.dialogs_messagePaint[paintIndex].setColor(Theme.dialogs_messagePaint[paintIndex].linkColor = Theme.getColor(Theme.key_chats_nameMessageArchived, resourcesProvider));
                } else {
                    Theme.dialogs_messagePaint[paintIndex].setColor(Theme.dialogs_messagePaint[paintIndex].linkColor = Theme.getColor(Theme.key_chats_messageArchived, resourcesProvider));
                }
            } else {
                Theme.dialogs_messagePaint[paintIndex].setColor(Theme.dialogs_messagePaint[paintIndex].linkColor = Theme.getColor(Theme.key_chats_message, resourcesProvider));
            }
            canvas.save();
            canvas.translate(messageLeft, messageTop);
            if (!spoilers.isEmpty()) {
                try {
                    canvas.save();
                    SpoilerEffect.clipOutCanvas(canvas, spoilers);
                    messageLayout.draw(canvas);
                    AnimatedEmojiSpan.drawAnimatedEmojis(canvas, messageLayout, animatedEmojiStack, -.075f, spoilers, 0, 0, 0, 1f);
                    canvas.restore();

                    for (int i = 0; i < spoilers.size(); i++) {
                        SpoilerEffect eff = spoilers.get(i);
                        eff.setColor(messageLayout.getPaint().getColor());
                        eff.draw(canvas);
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            } else {
                messageLayout.draw(canvas);
                AnimatedEmojiSpan.drawAnimatedEmojis(canvas, messageLayout, animatedEmojiStack, -.075f, null, 0, 0, 0, 1f);
            }
            canvas.restore();

            if (printingStringType >= 0) {
                StatusDrawable statusDrawable = Theme.getChatStatusDrawable(printingStringType);
                if (statusDrawable != null) {
                    canvas.save();
                    if (printingStringType == 1 || printingStringType == 4) {
                        canvas.translate(statusDrawableLeft, messageTop + (printingStringType == 1 ? AndroidUtilities.dp(1) : 0));
                    } else {
                        canvas.translate(statusDrawableLeft, messageTop + (AndroidUtilities.dp(18) - statusDrawable.getIntrinsicHeight()) / 2f);
                    }
                    statusDrawable.draw(canvas);
                    invalidate(statusDrawableLeft, messageTop, statusDrawableLeft + statusDrawable.getIntrinsicWidth(), messageTop + statusDrawable.getIntrinsicHeight());
                    canvas.restore();
                }
            }
        }

        if (buttonLayout != null) {
            canvas.save();
            if (buttonBackgroundPaint == null) {
                buttonBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            }
            if (canvasButton == null) {
                canvasButton = new CanvasButton(this);
                canvasButton.setDelegate(() -> {
                    delegate.onButtonClicked(this);
                });
                canvasButton.setLongPress(() -> {
                    delegate.onButtonLongPress(this);
                });
            }

            if (lastTopicMessageUnread) {
                canvasButton.setColor(ColorUtils.setAlphaComponent(currentMessagePaint.getColor(), Theme.isCurrentThemeDark() ? 36 : 26));
                canvasButton.rewind();
                if (topMessageTopicEndIndex != topMessageTopicStartIndex && topMessageTopicEndIndex > 0) {
                    AndroidUtilities.rectTmp.set(messageLeft + AndroidUtilities.dp(2), messageTop, messageLeft + messageLayout.getPrimaryHorizontal(Math.min(messageLayout.getText().length(), topMessageTopicEndIndex)) - AndroidUtilities.dp(3), buttonTop - AndroidUtilities.dp(4));
                    AndroidUtilities.rectTmp.inset(-AndroidUtilities.dp(8), -AndroidUtilities.dp(4));
                    if (AndroidUtilities.rectTmp.right > AndroidUtilities.rectTmp.left) {
                        canvasButton.addRect(AndroidUtilities.rectTmp);
                    }
                }

                AndroidUtilities.rectTmp.set(messageLeft + AndroidUtilities.dp(2), buttonTop + AndroidUtilities.dp(2), messageLeft + buttonLayout.getLineWidth(0) + AndroidUtilities.dp(12), buttonTop + buttonLayout.getHeight());
                AndroidUtilities.rectTmp.inset(-AndroidUtilities.dp(8), -AndroidUtilities.dp(3));
                canvasButton.addRect(AndroidUtilities.rectTmp);
                canvasButton.draw(canvas);

                Theme.dialogs_forum_arrowDrawable.setAlpha(125);
                setDrawableBounds(Theme.dialogs_forum_arrowDrawable, AndroidUtilities.rectTmp.right - AndroidUtilities.dp(18),  AndroidUtilities.rectTmp.top + (AndroidUtilities.rectTmp.height() - Theme.dialogs_forum_arrowDrawable.getIntrinsicHeight()) / 2f);
                Theme.dialogs_forum_arrowDrawable.draw(canvas);
            }


            canvas.translate(messageLeft - buttonLayout.getLineLeft(0), buttonTop);
            if (!spoilers2.isEmpty()) {
                try {
                    canvas.save();
                    SpoilerEffect.clipOutCanvas(canvas, spoilers2);
                    buttonLayout.draw(canvas);
                    AnimatedEmojiSpan.drawAnimatedEmojis(canvas, buttonLayout, animatedEmojiStack3, -.075f, spoilers2, 0, 0, 0, 1f);
                    canvas.restore();

                    for (int i = 0; i < spoilers2.size(); i++) {
                        SpoilerEffect eff = spoilers2.get(i);
                        eff.setColor(buttonLayout.getPaint().getColor());
                        eff.draw(canvas);
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            } else {
                buttonLayout.draw(canvas);
                AnimatedEmojiSpan.drawAnimatedEmojis(canvas, buttonLayout, animatedEmojiStack3, -.075f, null, 0, 0, 0, 1f);
            }
            canvas.restore();
        }


        if (currentDialogFolderId == 0) {
            int currentStatus = (drawClock ? 1 : 0) +  (drawCheck1 ? 2 : 0) + (drawCheck2 ? 4 : 0);
            if (lastStatusDrawableParams >= 0 && lastStatusDrawableParams != currentStatus && !statusDrawableAnimationInProgress) {
                createStatusDrawableAnimator(lastStatusDrawableParams, currentStatus);
            }
            if (statusDrawableAnimationInProgress) {
                currentStatus = animateToStatusDrawableParams;
            }

            boolean drawClock = (currentStatus & 1) != 0;
            boolean drawCheck1 = (currentStatus & 2) != 0;
            boolean drawCheck2 = (currentStatus & 4) != 0;

            if (statusDrawableAnimationInProgress) {
                boolean outDrawClock = (animateFromStatusDrawableParams & 1) != 0;
                boolean outDrawCheck1 = (animateFromStatusDrawableParams & 2) != 0;
                boolean outDrawCheck2 = (animateFromStatusDrawableParams & 4) != 0;
                if (!drawClock && !outDrawClock && outDrawCheck2 && !outDrawCheck1 && drawCheck1 && drawCheck2) {
                    drawCheckStatus(canvas, drawClock, drawCheck1, drawCheck2, true, statusDrawableProgress);
                } else {
                    drawCheckStatus(canvas, outDrawClock, outDrawCheck1, outDrawCheck2, false, 1f - statusDrawableProgress);
                    drawCheckStatus(canvas, drawClock, drawCheck1, drawCheck2, false, statusDrawableProgress);
                }
            } else {
                drawCheckStatus(canvas, drawClock, drawCheck1, drawCheck2, false,1f);
            }
            lastStatusDrawableParams = (this.drawClock ? 1 : 0) +  (this.drawCheck1 ? 2 : 0) + (this.drawCheck2 ? 4 : 0);
        }

        boolean drawMuted = drawUnmute || dialogMuted;
        if (dialogsType != 2 && (drawMuted || dialogMutedProgress > 0) && !drawVerified && drawScam == 0 && !drawPremium) {
            if (drawMuted && dialogMutedProgress != 1f) {
                dialogMutedProgress += 16 / 150f;
                if (dialogMutedProgress > 1f) {
                    dialogMutedProgress = 1f;
                } else {
                    invalidate();
                }
            } else if (!drawMuted && dialogMutedProgress != 0f) {
                dialogMutedProgress -= 16 / 150f;
                if (dialogMutedProgress < 0f) {
                    dialogMutedProgress = 0f;
                } else {
                    invalidate();
                }
            }
            float muteX = nameMuteLeft - AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 0 : 1);
            float muteY = AndroidUtilities.dp(SharedConfig.useThreeLinesLayout ? 13.5f : 17.5f);
            setDrawableBounds(Theme.dialogs_muteDrawable, muteX, muteY);
            setDrawableBounds(Theme.dialogs_unmuteDrawable, muteX, muteY);
            if (dialogMutedProgress != 1f) {
                canvas.save();
                canvas.scale(dialogMutedProgress, dialogMutedProgress, Theme.dialogs_muteDrawable.getBounds().centerX(), Theme.dialogs_muteDrawable.getBounds().centerY());
                if (drawUnmute) {
                    Theme.dialogs_unmuteDrawable.setAlpha((int) (255 * dialogMutedProgress));
                    Theme.dialogs_unmuteDrawable.draw(canvas);
                    Theme.dialogs_unmuteDrawable.setAlpha(255);
                } else {
                    Theme.dialogs_muteDrawable.setAlpha((int) (255 * dialogMutedProgress));
                    Theme.dialogs_muteDrawable.draw(canvas);
                    Theme.dialogs_muteDrawable.setAlpha(255);
                }
                canvas.restore();
            } else {
                if (drawUnmute) {
                    Theme.dialogs_unmuteDrawable.draw(canvas);
                } else {
                    Theme.dialogs_muteDrawable.draw(canvas);
                }
            }

        } else if (drawVerified) {
            setDrawableBounds(Theme.dialogs_verifiedDrawable, nameMuteLeft - AndroidUtilities.dp(1), AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 13.5f : 16.5f));
            setDrawableBounds(Theme.dialogs_verifiedCheckDrawable, nameMuteLeft - AndroidUtilities.dp(1), AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 13.5f : 16.5f));
            Theme.dialogs_verifiedDrawable.draw(canvas);
            Theme.dialogs_verifiedCheckDrawable.draw(canvas);
        } else if (drawPremium) {
            if (emojiStatus != null) {
                emojiStatus.setBounds(
                    nameMuteLeft - AndroidUtilities.dp(2),
                    AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 12.5f : 15.5f) - AndroidUtilities.dp(4),
                    nameMuteLeft + AndroidUtilities.dp(20),
                    AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 12.5f : 15.5f) - AndroidUtilities.dp(4) + AndroidUtilities.dp(22)
                );
                emojiStatus.setColor(Theme.getColor(Theme.key_chats_verifiedBackground, resourcesProvider));
                emojiStatus.draw(canvas);
            } else {
                Drawable premiumDrawable = PremiumGradient.getInstance().premiumStarDrawableMini;
                setDrawableBounds(premiumDrawable, nameMuteLeft - AndroidUtilities.dp(1), AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 12.5f : 15.5f));
                premiumDrawable.draw(canvas);
            }
        } else if (drawScam != 0) {
            setDrawableBounds((drawScam == 1 ? Theme.dialogs_scamDrawable : Theme.dialogs_fakeDrawable), nameMuteLeft, AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 12 : 15));
            (drawScam == 1 ? Theme.dialogs_scamDrawable : Theme.dialogs_fakeDrawable).draw(canvas);
        }

        if (drawReorder || reorderIconProgress != 0) {
            Theme.dialogs_reorderDrawable.setAlpha((int) (reorderIconProgress * 255));
            setDrawableBounds(Theme.dialogs_reorderDrawable, pinLeft, pinTop);
            Theme.dialogs_reorderDrawable.draw(canvas);
        }
        if (drawError) {
            Theme.dialogs_errorDrawable.setAlpha((int) ((1.0f - reorderIconProgress) * 255));
            rect.set(errorLeft, errorTop, errorLeft + AndroidUtilities.dp(23), errorTop + AndroidUtilities.dp(23));
            canvas.drawRoundRect(rect, 11.5f * AndroidUtilities.density, 11.5f * AndroidUtilities.density, Theme.dialogs_errorPaint);
            setDrawableBounds(Theme.dialogs_errorDrawable, errorLeft + AndroidUtilities.dp(5.5f), errorTop + AndroidUtilities.dp(5));
            Theme.dialogs_errorDrawable.draw(canvas);
        } else if ((drawCount || drawMention) && drawCount2 || countChangeProgress != 1f || drawReactionMention || reactionsMentionsChangeProgress != 1f) {
            boolean drawCounterMuted;
            if (isTopic) {
                drawCounterMuted = topicMuted;
            } else {
                drawCounterMuted = chat != null && chat.forum && forumTopic == null ? !hasUnmutedTopics : dialogMuted;
            }
            if (drawCount && drawCount2 || countChangeProgress != 1f) {
                final float progressFinal = (unreadCount == 0 && !markUnread) ? 1f - countChangeProgress : countChangeProgress;
                Paint paint;
                int fillPaintAlpha = 255;
                boolean restoreCountTextPaint = false;
                if (isTopic && forumTopic.read_inbox_max_id == 0) {
                    if (topicCounterPaint == null) {
                        topicCounterPaint = new Paint();
                    }
                    paint = topicCounterPaint;
                    int color = Theme.getColor(drawCounterMuted ? Theme.key_topics_unreadCounterMuted :  Theme.key_topics_unreadCounter, resourcesProvider);
                    paint.setColor(color);
                    Theme.dialogs_countTextPaint.setColor(color);
                    fillPaintAlpha = drawCounterMuted ? 30 : 40;
                    restoreCountTextPaint = true;
                } else {
                    paint = drawCounterMuted || currentDialogFolderId != 0 ? Theme.dialogs_countGrayPaint : Theme.dialogs_countPaint;
                }

                if (countOldLayout == null || unreadCount == 0) {
                    StaticLayout drawLayout = unreadCount == 0 ? countOldLayout : countLayout;
                    paint.setAlpha((int) ((1.0f - reorderIconProgress) * fillPaintAlpha));
                    Theme.dialogs_countTextPaint.setAlpha((int) ((1.0f - reorderIconProgress) * 255));

                    int x = countLeft - AndroidUtilities.dp(5.5f);
                    rect.set(x, countTop, x + countWidth + AndroidUtilities.dp(11), countTop + AndroidUtilities.dp(23));

                    if (progressFinal != 1f) {
                        if (getIsPinned()) {
                            Theme.dialogs_pinnedDrawable.setAlpha((int) ((1.0f - reorderIconProgress) * 255));
                            setDrawableBounds(Theme.dialogs_pinnedDrawable, pinLeft, pinTop);
                            canvas.save();
                            canvas.scale(1f - progressFinal, 1f - progressFinal, Theme.dialogs_pinnedDrawable.getBounds().centerX(), Theme.dialogs_pinnedDrawable.getBounds().centerY());
                            Theme.dialogs_pinnedDrawable.draw(canvas);
                            canvas.restore();
                        }
                        canvas.save();
                        canvas.scale(progressFinal, progressFinal, rect.centerX(), rect.centerY());
                    }

                    canvas.drawRoundRect(rect, 11.5f * AndroidUtilities.density, 11.5f * AndroidUtilities.density, paint);
                    if (drawLayout != null) {
                        canvas.save();
                        canvas.translate(countLeft, countTop + AndroidUtilities.dp(4));
                        drawLayout.draw(canvas);
                        canvas.restore();
                    }

                    if (progressFinal != 1f) {
                        canvas.restore();
                    }
                } else {
                    paint.setAlpha((int) ((1.0f - reorderIconProgress) * fillPaintAlpha));
                    Theme.dialogs_countTextPaint.setAlpha((int) ((1.0f - reorderIconProgress) * 255));

                    float progressHalf = progressFinal * 2;
                    if (progressHalf > 1f) {
                        progressHalf = 1f;
                    }

                    float countLeft = this.countLeft * progressHalf + countLeftOld * (1f - progressHalf);
                    float x = countLeft - AndroidUtilities.dp(5.5f);
                    rect.set(x, countTop, x + (countWidth * progressHalf) + (countWidthOld * (1f - progressHalf)) + AndroidUtilities.dp(11), countTop + AndroidUtilities.dp(23));

                    float scale = 1f;
                    if (progressFinal <= 0.5f) {
                        scale += 0.1f * CubicBezierInterpolator.EASE_OUT.getInterpolation(progressFinal * 2);
                    } else {
                        scale += 0.1f * CubicBezierInterpolator.EASE_IN.getInterpolation((1f - (progressFinal - 0.5f) * 2));
                    }


                    canvas.save();
                    canvas.scale(scale, scale, rect.centerX(), rect.centerY());
                    canvas.drawRoundRect(rect, 11.5f * AndroidUtilities.density, 11.5f * AndroidUtilities.density, paint);

                    if (countAnimationStableLayout != null) {
                        canvas.save();
                        canvas.translate(countLeft,  countTop + AndroidUtilities.dp(4));
                        countAnimationStableLayout.draw(canvas);
                        canvas.restore();
                    }

                    int textAlpha = Theme.dialogs_countTextPaint.getAlpha();
                    Theme.dialogs_countTextPaint.setAlpha((int) (textAlpha * progressHalf));
                    if (countAnimationInLayout != null) {
                        canvas.save();
                        canvas.translate(countLeft,  (countAnimationIncrement ? AndroidUtilities.dp(13) : -AndroidUtilities.dp(13)) * (1f - progressHalf) + countTop + AndroidUtilities.dp(4));
                        countAnimationInLayout.draw(canvas);
                        canvas.restore();
                    } else if (countLayout != null) {
                        canvas.save();
                        canvas.translate(countLeft,  (countAnimationIncrement ? AndroidUtilities.dp(13) : -AndroidUtilities.dp(13)) * (1f - progressHalf) + countTop + AndroidUtilities.dp(4));
                        countLayout.draw(canvas);
                        canvas.restore();
                    }

                    if (countOldLayout != null) {
                        Theme.dialogs_countTextPaint.setAlpha((int) (textAlpha * (1f - progressHalf)));
                        canvas.save();
                        canvas.translate(countLeft, (countAnimationIncrement ? -AndroidUtilities.dp(13) : AndroidUtilities.dp(13)) * progressHalf + countTop + AndroidUtilities.dp(4));
                        countOldLayout.draw(canvas);
                        canvas.restore();
                    }
                    Theme.dialogs_countTextPaint.setAlpha(textAlpha);
                    canvas.restore();
                }
                if (restoreCountTextPaint) {
                    Theme.dialogs_countTextPaint.setColor(Theme.getColor(Theme.key_chats_unreadCounterText));
                }
            }
            if (drawMention) {
                Theme.dialogs_countPaint.setAlpha((int) ((1.0f - reorderIconProgress) * 255));

                int x = mentionLeft - AndroidUtilities.dp(5.5f);
                rect.set(x, countTop, x + mentionWidth + AndroidUtilities.dp(11), countTop + AndroidUtilities.dp(23));
                Paint paint = drawCounterMuted && folderId != 0 ? Theme.dialogs_countGrayPaint : Theme.dialogs_countPaint;
                canvas.drawRoundRect(rect, 11.5f * AndroidUtilities.density, 11.5f * AndroidUtilities.density, paint);
                if (mentionLayout != null) {
                    Theme.dialogs_countTextPaint.setAlpha((int) ((1.0f - reorderIconProgress) * 255));

                    canvas.save();
                    canvas.translate(mentionLeft, countTop + AndroidUtilities.dp(4));
                    mentionLayout.draw(canvas);
                    canvas.restore();
                } else {
                    Theme.dialogs_mentionDrawable.setAlpha((int) ((1.0f - reorderIconProgress) * 255));

                    setDrawableBounds(Theme.dialogs_mentionDrawable, mentionLeft - AndroidUtilities.dp(2), countTop + AndroidUtilities.dp(3.2f), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
                    Theme.dialogs_mentionDrawable.draw(canvas);
                }
            }

            if (drawReactionMention || reactionsMentionsChangeProgress != 1f) {

                Theme.dialogs_reactionsCountPaint.setAlpha((int) ((1.0f - reorderIconProgress) * 255));

                int x = reactionMentionLeft - AndroidUtilities.dp(5.5f);
                rect.set(x, countTop, x + AndroidUtilities.dp(23), countTop + AndroidUtilities.dp(23));
                Paint paint = Theme.dialogs_reactionsCountPaint;

                canvas.save();
                if (reactionsMentionsChangeProgress != 1f) {
                    float s = drawReactionMention ? reactionsMentionsChangeProgress : (1f - reactionsMentionsChangeProgress);
                    canvas.scale(s, s, rect.centerX(),  rect.centerY());
                }
                canvas.drawRoundRect(rect, 11.5f * AndroidUtilities.density, 11.5f * AndroidUtilities.density, paint);
                Theme.dialogs_reactionsMentionDrawable.setAlpha((int) ((1.0f - reorderIconProgress) * 255));
                setDrawableBounds(Theme.dialogs_reactionsMentionDrawable, reactionMentionLeft - AndroidUtilities.dp(2), countTop + AndroidUtilities.dp(3.8f), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
                Theme.dialogs_reactionsMentionDrawable.draw(canvas);
                canvas.restore();
            }
        } else if (getIsPinned()) {
            Theme.dialogs_pinnedDrawable.setAlpha((int) ((1.0f - reorderIconProgress) * 255));
            setDrawableBounds(Theme.dialogs_pinnedDrawable, pinLeft, pinTop);
            Theme.dialogs_pinnedDrawable.draw(canvas);
        }

        if (animatingArchiveAvatar) {
            canvas.save();
            float scale = 1.0f + interpolator.getInterpolation(animatingArchiveAvatarProgress / 170.0f);
            canvas.scale(scale, scale, avatarImage.getCenterX(), avatarImage.getCenterY());
        }

        if (drawAvatar && (currentDialogFolderId == 0 || archivedChatsDrawable == null || !archivedChatsDrawable.isDraw())) {
            avatarImage.draw(canvas);
        }

        if (thumbsCount > 0) {
            for (int i = 0; i < thumbsCount; ++i) {
                thumbImage[i].draw(canvas);
                if (drawPlay[i]) {
                    int x = (int) (thumbImage[i].getCenterX() - Theme.dialogs_playDrawable.getIntrinsicWidth() / 2);
                    int y = (int) (thumbImage[i].getCenterY() - Theme.dialogs_playDrawable.getIntrinsicHeight() / 2);
                    setDrawableBounds(Theme.dialogs_playDrawable, x, y);
                    Theme.dialogs_playDrawable.draw(canvas);
                }
            }
        }

        if (animatingArchiveAvatar) {
            canvas.restore();
        }

        if (isDialogCell && currentDialogFolderId == 0) {
            if (user != null && !MessagesController.isSupportUser(user) && !user.bot) {
                boolean isOnline = isOnline();
                if (isOnline || onlineProgress != 0) {
                    int top = (int) (avatarImage.getImageY2() - AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 6 : 8));
                    int left;
                    if (LocaleController.isRTL) {
                        left = (int) (avatarImage.getImageX() + AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 10 : 6));
                    } else {
                        left = (int) (avatarImage.getImageX2() - AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 10 : 6));
                    }

                    Theme.dialogs_onlineCirclePaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
                    canvas.drawCircle(left, top, AndroidUtilities.dp(7) * onlineProgress, Theme.dialogs_onlineCirclePaint);
                    Theme.dialogs_onlineCirclePaint.setColor(Theme.getColor(Theme.key_chats_onlineCircle, resourcesProvider));
                    canvas.drawCircle(left, top, AndroidUtilities.dp(5) * onlineProgress, Theme.dialogs_onlineCirclePaint);
                    if (isOnline) {
                        if (onlineProgress < 1.0f) {
                            onlineProgress += dt / 150.0f;
                            if (onlineProgress > 1.0f) {
                                onlineProgress = 1.0f;
                            }
                            needInvalidate = true;
                        }
                    } else {
                        if (onlineProgress > 0.0f) {
                            onlineProgress -= dt / 150.0f;
                            if (onlineProgress < 0.0f) {
                                onlineProgress = 0.0f;
                            }
                            needInvalidate = true;
                        }
                    }
                }
            } else if (chat != null) {
                hasCall = chat.call_active && chat.call_not_empty;
                if (hasCall || chatCallProgress != 0) {
                    float checkProgress = checkBox != null && checkBox.isChecked() ? 1.0f - checkBox.getProgress() : 1.0f;
                    int top = (int) (avatarImage.getImageY2() - AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 6 : 8));
                    int left;
                    if (LocaleController.isRTL) {
                        left = (int) (avatarImage.getImageX() + AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 10 : 6));
                    } else {
                        left = (int) (avatarImage.getImageX2() - AndroidUtilities.dp(useForceThreeLines || SharedConfig.useThreeLinesLayout ? 10 : 6));
                    }

                    Theme.dialogs_onlineCirclePaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
                    canvas.drawCircle(left, top, AndroidUtilities.dp(11) * chatCallProgress * checkProgress, Theme.dialogs_onlineCirclePaint);
                    Theme.dialogs_onlineCirclePaint.setColor(Theme.getColor(Theme.key_chats_onlineCircle, resourcesProvider));
                    canvas.drawCircle(left, top, AndroidUtilities.dp(9) * chatCallProgress * checkProgress, Theme.dialogs_onlineCirclePaint);
                    Theme.dialogs_onlineCirclePaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));

                    float size1;
                    float size2;
                    if (progressStage == 0) {
                        size1 = AndroidUtilities.dp(1) + AndroidUtilities.dp(4) * innerProgress;
                        size2 = AndroidUtilities.dp(3) - AndroidUtilities.dp(2) * innerProgress;
                    } else if (progressStage == 1) {
                        size1 = AndroidUtilities.dp(5) - AndroidUtilities.dp(4) * innerProgress;
                        size2 = AndroidUtilities.dp(1) + AndroidUtilities.dp(4) * innerProgress;
                    } else if (progressStage == 2) {
                        size1 = AndroidUtilities.dp(1) + AndroidUtilities.dp(2) * innerProgress;
                        size2 = AndroidUtilities.dp(5) - AndroidUtilities.dp(4) * innerProgress;
                    } else if (progressStage == 3) {
                        size1 = AndroidUtilities.dp(3) - AndroidUtilities.dp(2) * innerProgress;
                        size2 = AndroidUtilities.dp(1) + AndroidUtilities.dp(2) * innerProgress;
                    } else if (progressStage == 4) {
                        size1 = AndroidUtilities.dp(1) + AndroidUtilities.dp(4) * innerProgress;
                        size2 = AndroidUtilities.dp(3) - AndroidUtilities.dp(2) * innerProgress;
                    } else if (progressStage == 5) {
                        size1 = AndroidUtilities.dp(5) - AndroidUtilities.dp(4) * innerProgress;
                        size2 = AndroidUtilities.dp(1) + AndroidUtilities.dp(4) * innerProgress;
                    } else if (progressStage == 6) {
                        size1 = AndroidUtilities.dp(1) + AndroidUtilities.dp(4) * innerProgress;
                        size2 = AndroidUtilities.dp(5) - AndroidUtilities.dp(4) * innerProgress;
                    } else {
                        size1 = AndroidUtilities.dp(5) - AndroidUtilities.dp(4) * innerProgress;
                        size2 = AndroidUtilities.dp(1) + AndroidUtilities.dp(2) * innerProgress;
                    }

                    if (chatCallProgress < 1.0f || checkProgress < 1.0f) {
                        canvas.save();
                        canvas.scale(chatCallProgress * checkProgress, chatCallProgress * checkProgress, left, top);
                    }
                    rect.set(left - AndroidUtilities.dp(1), top - size1, left + AndroidUtilities.dp(1), top + size1);
                    canvas.drawRoundRect(rect, AndroidUtilities.dp(1), AndroidUtilities.dp(1), Theme.dialogs_onlineCirclePaint);

                    rect.set(left - AndroidUtilities.dp(5), top - size2, left - AndroidUtilities.dp(3), top + size2);
                    canvas.drawRoundRect(rect, AndroidUtilities.dp(1), AndroidUtilities.dp(1), Theme.dialogs_onlineCirclePaint);

                    rect.set(left + AndroidUtilities.dp(3), top - size2, left + AndroidUtilities.dp(5), top + size2);
                    canvas.drawRoundRect(rect, AndroidUtilities.dp(1), AndroidUtilities.dp(1), Theme.dialogs_onlineCirclePaint);
                    if (chatCallProgress < 1.0f || checkProgress < 1.0f) {
                        canvas.restore();
                    }

                    innerProgress += dt / 400.0f;
                    if (innerProgress >= 1.0f) {
                        innerProgress = 0.0f;
                        progressStage++;
                        if (progressStage >= 8) {
                            progressStage = 0;
                        }
                    }
                    needInvalidate = true;

                    if (hasCall) {
                        if (chatCallProgress < 1.0f) {
                            chatCallProgress += dt / 150.0f;
                            if (chatCallProgress > 1.0f) {
                                chatCallProgress = 1.0f;
                            }
                        }
                    } else {
                        if (chatCallProgress > 0.0f) {
                            chatCallProgress -= dt / 150.0f;
                            if (chatCallProgress < 0.0f) {
                                chatCallProgress = 0.0f;
                            }
                        }
                    }
                }
            }
        }

        if (translationX != 0) {
            canvas.restore();
        }
        if (currentDialogFolderId != 0 && translationX == 0 && archivedChatsDrawable != null) {
            canvas.save();
            canvas.clipRect(0, 0, getMeasuredWidth(), getMeasuredHeight());
            archivedChatsDrawable.draw(canvas);
            canvas.restore();
        }

        if (useSeparator) {
            int left;
            if (fullSeparator || currentDialogFolderId != 0 && archiveHidden && !fullSeparator2 || fullSeparator2 && !archiveHidden) {
                left = 0;
            } else {
                left = AndroidUtilities.dp(messagePaddingStart);
            }
            if (LocaleController.isRTL) {
                canvas.drawLine(0, getMeasuredHeight() - 1, getMeasuredWidth() - left, getMeasuredHeight() - 1, Theme.dividerPaint);
            } else {
                canvas.drawLine(left, getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }

        if (clipProgress != 0.0f) {
            if (Build.VERSION.SDK_INT != 24) {
                canvas.restore();
            } else {
                Theme.dialogs_pinnedPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
                canvas.drawRect(0, 0, getMeasuredWidth(), topClip * clipProgress, Theme.dialogs_pinnedPaint);
                canvas.drawRect(0, getMeasuredHeight() - (int) (bottomClip * clipProgress), getMeasuredWidth(), getMeasuredHeight(), Theme.dialogs_pinnedPaint);
            }
        }

        if (drawReorder || reorderIconProgress != 0.0f) {
            if (drawReorder) {
                if (reorderIconProgress < 1.0f) {
                    reorderIconProgress += dt / 170.0f;
                    if (reorderIconProgress > 1.0f) {
                        reorderIconProgress = 1.0f;
                    }
                    needInvalidate = true;
                }
            } else {
                if (reorderIconProgress > 0.0f) {
                    reorderIconProgress -= dt / 170.0f;
                    if (reorderIconProgress < 0.0f) {
                        reorderIconProgress = 0.0f;
                    }
                    needInvalidate = true;
                }
            }
        }

        if (archiveHidden) {
            if (archiveBackgroundProgress > 0.0f) {
                archiveBackgroundProgress -= dt / 230.0f;
                if (archiveBackgroundProgress < 0.0f) {
                    archiveBackgroundProgress = 0.0f;
                }
                if (avatarDrawable.getAvatarType() == AvatarDrawable.AVATAR_TYPE_ARCHIVED) {
                    avatarDrawable.setArchivedAvatarHiddenProgress(CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(archiveBackgroundProgress));
                }
                needInvalidate = true;
            }
        } else {
            if (archiveBackgroundProgress < 1.0f) {
                archiveBackgroundProgress += dt / 230.0f;
                if (archiveBackgroundProgress > 1.0f) {
                    archiveBackgroundProgress = 1.0f;
                }
                if (avatarDrawable.getAvatarType() == AvatarDrawable.AVATAR_TYPE_ARCHIVED) {
                    avatarDrawable.setArchivedAvatarHiddenProgress(CubicBezierInterpolator.EASE_OUT_QUINT.getInterpolation(archiveBackgroundProgress));
                }
                needInvalidate = true;
            }
        }

        if (animatingArchiveAvatar) {
            animatingArchiveAvatarProgress += dt;
            if (animatingArchiveAvatarProgress >= 170.0f) {
                animatingArchiveAvatarProgress = 170.0f;
                animatingArchiveAvatar = false;
            }
            needInvalidate = true;
        }
        if (drawRevealBackground) {
            if (currentRevealBounceProgress < 1.0f) {
                currentRevealBounceProgress += dt / 170.0f;
                if (currentRevealBounceProgress > 1.0f) {
                    currentRevealBounceProgress = 1.0f;
                    needInvalidate = true;
                }
            }
            if (currentRevealProgress < 1.0f) {
                currentRevealProgress += dt / 300.0f;
                if (currentRevealProgress > 1.0f) {
                    currentRevealProgress = 1.0f;
                }
                needInvalidate = true;
            }
        } else {
            if (currentRevealBounceProgress == 1.0f) {
                currentRevealBounceProgress = 0.0f;
                needInvalidate = true;
            }
            if (currentRevealProgress > 0.0f) {
                currentRevealProgress -= dt / 300.0f;
                if (currentRevealProgress < 0.0f) {
                    currentRevealProgress = 0.0f;
                }
                needInvalidate = true;
            }
        }
        if (needInvalidate) {
            invalidate();
        }
    }

    private void createStatusDrawableAnimator(int lastStatusDrawableParams, int currentStatus) {
        statusDrawableProgress = 0f;
        statusDrawableAnimator = ValueAnimator.ofFloat(0,1f);
        statusDrawableAnimator.setDuration(220);

        statusDrawableAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
        animateFromStatusDrawableParams = lastStatusDrawableParams;
        animateToStatusDrawableParams = currentStatus;
        statusDrawableAnimator.addUpdateListener(valueAnimator -> {
            statusDrawableProgress = (float) valueAnimator.getAnimatedValue();
            invalidate();
        });
        statusDrawableAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                int currentStatus = (DialogCell.this.drawClock ? 1 : 0) +  (DialogCell.this.drawCheck1 ? 2 : 0) + (DialogCell.this.drawCheck2 ? 4 : 0);
                if (animateToStatusDrawableParams != currentStatus) {
                    createStatusDrawableAnimator(animateToStatusDrawableParams, currentStatus);
                } else {
                    statusDrawableAnimationInProgress = false;
                    DialogCell.this.lastStatusDrawableParams = animateToStatusDrawableParams;
                }
                invalidate();
            }
        });
        statusDrawableAnimationInProgress = true;
        statusDrawableAnimator.start();
    }

    public void startOutAnimation() {
        if (archivedChatsDrawable != null) {
            archivedChatsDrawable.outCy = avatarImage.getCenterY();
            archivedChatsDrawable.outCx = avatarImage.getCenterX();
            archivedChatsDrawable.outRadius = avatarImage.getImageWidth() / 2.0f;
            archivedChatsDrawable.outImageSize = avatarImage.getBitmapWidth();
            archivedChatsDrawable.startOutAnimation();
        }
    }

    public void onReorderStateChanged(boolean reordering, boolean animated) {
        if (!getIsPinned() && reordering || drawReorder == reordering) {
            if (!getIsPinned()) {
                drawReorder = false;
            }
            return;
        }
        drawReorder = reordering;
        if (animated) {
            reorderIconProgress = drawReorder ? 0.0f : 1.0f;
        } else {
            reorderIconProgress = drawReorder ? 1.0f : 0.0f;
        }
        invalidate();
    }

    public void setSliding(boolean value) {
        isSliding = value;
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        if (who == translationDrawable || who == Theme.dialogs_archiveAvatarDrawable) {
            invalidate(who.getBounds());
        } else {
            super.invalidateDrawable(who);
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action == R.id.acc_action_chat_preview && parentFragment != null) {
            parentFragment.showChatPreview(this);
            return true;
        }
        return super.performAccessibilityAction(action, arguments);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (isFolderCell() && archivedChatsDrawable != null && SharedConfig.archiveHidden && archivedChatsDrawable.pullProgress == 0.0f) {
            info.setVisibleToUser(false);
        } else {
            info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
            info.addAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
            if (!isFolderCell() && parentFragment != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.acc_action_chat_preview, LocaleController.getString("AccActionChatPreview", R.string.AccActionChatPreview)));
            }
        }
        if (checkBox != null && checkBox.isChecked()) {
            info.setClassName("android.widget.CheckBox");
            info.setCheckable(true);
            info.setChecked(true);
        }
    }

    @Override
    public void onPopulateAccessibilityEvent(AccessibilityEvent event) {
        super.onPopulateAccessibilityEvent(event);
        StringBuilder sb = new StringBuilder();
        if (currentDialogFolderId == 1) {
            sb.append(LocaleController.getString("ArchivedChats", R.string.ArchivedChats));
            sb.append(". ");
        } else {
            if (encryptedChat != null) {
                sb.append(LocaleController.getString("AccDescrSecretChat", R.string.AccDescrSecretChat));
                sb.append(". ");
            }
            if (user != null) {
                if (UserObject.isReplyUser(user)) {
                    sb.append(LocaleController.getString("RepliesTitle", R.string.RepliesTitle));
                } else {
                    if (user.bot) {
                        sb.append(LocaleController.getString("Bot", R.string.Bot));
                        sb.append(". ");
                    }
                    if (user.self) {
                        sb.append(LocaleController.getString("SavedMessages", R.string.SavedMessages));
                    } else {
                        sb.append(ContactsController.formatName(user.first_name, user.last_name));
                    }
                }
                sb.append(". ");
            } else if (chat != null) {
                if (chat.broadcast) {
                    sb.append(LocaleController.getString("AccDescrChannel", R.string.AccDescrChannel));
                } else {
                    sb.append(LocaleController.getString("AccDescrGroup", R.string.AccDescrGroup));
                }
                sb.append(". ");
                sb.append(chat.title);
                sb.append(". ");
            }
        }
        if (drawVerified) {
            sb.append(LocaleController.getString("AccDescrVerified", R.string.AccDescrVerified));
            sb.append(". ");
        }
        if (unreadCount > 0) {
            sb.append(LocaleController.formatPluralString("NewMessages", unreadCount));
            sb.append(". ");
        }
        if (mentionCount > 0) {
            sb.append(LocaleController.formatPluralString("AccDescrMentionCount", mentionCount));
            sb.append(". ");
        }
        if (reactionMentionCount > 0) {
            sb.append(LocaleController.getString("AccDescrMentionReaction", R.string.AccDescrMentionReaction));
            sb.append(". ");
        }
        if (message == null || currentDialogFolderId != 0) {
            event.setContentDescription(sb.toString());
            return;
        }
        int lastDate = lastMessageDate;
        if (lastMessageDate == 0) {
            lastDate = message.messageOwner.date;
        }
        String date = LocaleController.formatDateAudio(lastDate, true);
        if (message.isOut()) {
            sb.append(LocaleController.formatString("AccDescrSentDate", R.string.AccDescrSentDate, date));
        } else {
            sb.append(LocaleController.formatString("AccDescrReceivedDate", R.string.AccDescrReceivedDate, date));
        }
        sb.append(". ");
        if (chat != null && !message.isOut() && message.isFromUser() && message.messageOwner.action == null) {
            TLRPC.User fromUser = MessagesController.getInstance(currentAccount).getUser(message.messageOwner.from_id.user_id);
            if (fromUser != null) {
                sb.append(ContactsController.formatName(fromUser.first_name, fromUser.last_name));
                sb.append(". ");
            }
        }
        if (encryptedChat == null) {
            StringBuilder messageString = new StringBuilder();
            messageString.append(message.messageText);
            if (!message.isMediaEmpty()) {
                MessageObject captionMessage = getCaptionMessage();
                if (captionMessage != null && !TextUtils.isEmpty(captionMessage.caption)) {
                    if (messageString.length() > 0) {
                        messageString.append(". ");
                    }
                    messageString.append(captionMessage);
                }
            }
            int len = messageLayout == null ? -1 : messageLayout.getText().length();
            if (len > 0) {
                int index = messageString.length(), b;
                if ((b = messageString.indexOf("\n", len)) < index && b >= 0)
                    index = b;
                if ((b = messageString.indexOf("\t", len)) < index && b >= 0)
                    index = b;
                if ((b = messageString.indexOf(" ", len)) < index && b >= 0)
                    index = b;
                sb.append(messageString.substring(0, index));
            } else {
                sb.append(messageString);
            }
        }
        event.setContentDescription(sb.toString());
    }

    private MessageObject getCaptionMessage() {
        if (groupMessages == null) {
            if (message != null && message.caption != null) {
                return message;
            }
            return null;
        }

        MessageObject captionMessage = null;
        int hasCaption = 0;
        for (int i = 0; i < groupMessages.size(); ++i) {
            MessageObject msg = groupMessages.get(i);
            if (msg != null && msg.caption != null) {
                captionMessage = msg;
                if (!TextUtils.isEmpty(msg.caption)) {
                    hasCaption++;
                }
            }
        }
        if (hasCaption > 1) {
            return null;
        }
        return captionMessage;
    }

    public void updateMessageThumbs() {
        if (message == null) {
            return;
        }
        String restrictionReason = MessagesController.getRestrictionReason(message.messageOwner.restriction_reason);
        if (groupMessages != null && groupMessages.size() > 1 && TextUtils.isEmpty(restrictionReason) && currentDialogFolderId == 0 && encryptedChat == null) {
            thumbsCount = 0;
            hasVideoThumb = false;
            Collections.sort(groupMessages, (a, b) -> a.getId() - b.getId());
            for (int i = 0; i < groupMessages.size(); ++i) {
                MessageObject message = groupMessages.get(i);
                if (message != null && !message.needDrawBluredPreview() && (message.isPhoto() || message.isNewGif() || message.isVideo() || message.isRoundVideo())) {
                    String type = message.isWebpage() ? message.messageOwner.media.webpage.type : null;
                    if (!("app".equals(type) || "profile".equals(type) || "article".equals(type) || type != null && type.startsWith("telegram_"))) {
                        TLRPC.PhotoSize smallThumb = FileLoader.getClosestPhotoSizeWithSize(message.photoThumbs, 40);
                        TLRPC.PhotoSize bigThumb = FileLoader.getClosestPhotoSizeWithSize(message.photoThumbs, AndroidUtilities.getPhotoSize());
                        if (smallThumb == bigThumb) {
                            bigThumb = null;
                        }
                        if (smallThumb != null) {
                            hasVideoThumb = hasVideoThumb || (message.isVideo() || message.isRoundVideo());
                            if (i < 2) {
                                thumbsCount++;
                                drawPlay[i] = message.isVideo() || message.isRoundVideo();
                                int size = message.type == MessageObject.TYPE_PHOTO && bigThumb != null ? bigThumb.size : 0;
                                thumbImage[i].setImage(ImageLocation.getForObject(bigThumb, message.photoThumbsObject), "20_20", ImageLocation.getForObject(smallThumb, message.photoThumbsObject), "20_20", size, null, message, 0);
                                thumbImage[i].setRoundRadius(message.isRoundVideo() ? AndroidUtilities.dp(18) : AndroidUtilities.dp(2));
                                needEmoji = false;
                            }
                        }
                    }
                }
            }
        } else if (message != null && currentDialogFolderId == 0) {
            thumbsCount = 0;
            hasVideoThumb = false;
            if (!message.needDrawBluredPreview() && (message.isPhoto() || message.isNewGif() || message.isVideo() || message.isRoundVideo())) {
                String type = message.isWebpage() ? message.messageOwner.media.webpage.type : null;
                if (!("app".equals(type) || "profile".equals(type) || "article".equals(type) || type != null && type.startsWith("telegram_"))) {
                    TLRPC.PhotoSize smallThumb = FileLoader.getClosestPhotoSizeWithSize(message.photoThumbs, 40);
                    TLRPC.PhotoSize bigThumb = FileLoader.getClosestPhotoSizeWithSize(message.photoThumbs, AndroidUtilities.getPhotoSize());
                    if (smallThumb == bigThumb) {
                        bigThumb = null;
                    }
                    if (smallThumb != null) {
                        hasVideoThumb = hasVideoThumb || (message.isVideo() || message.isRoundVideo());
                        if (thumbsCount < 3) {
                            thumbsCount++;
                            drawPlay[0] = message.isVideo() || message.isRoundVideo();
                            int size = message.type == MessageObject.TYPE_PHOTO && bigThumb != null ? bigThumb.size : 0;
                            thumbImage[0].setImage(ImageLocation.getForObject(bigThumb, message.photoThumbsObject), "20_20", ImageLocation.getForObject(smallThumb, message.photoThumbsObject), "20_20", size, null, message, 0);
                            thumbImage[0].setRoundRadius(message.isRoundVideo() ? AndroidUtilities.dp(18) : AndroidUtilities.dp(2));
                            needEmoji = false;
                        }
                    }
                }
            }
        }
    }

    public String getMessageNameString() {
        if (message == null) {
            return null;
        }
        TLRPC.User user;
        TLRPC.User fromUser = null;
        TLRPC.Chat fromChat = null;
        long fromId = message.getFromChatId();
        if (DialogObject.isUserDialog(fromId)) {
            fromUser = MessagesController.getInstance(currentAccount).getUser(fromId);
        } else {
            fromChat = MessagesController.getInstance(currentAccount).getChat(-fromId);
        }

        if (message.isOutOwner()) {
            return LocaleController.getString("FromYou", R.string.FromYou);
        } else if (message != null && message.messageOwner != null && message.messageOwner.from_id instanceof TLRPC.TL_peerUser && (user = MessagesController.getInstance(currentAccount).getUser(message.messageOwner.from_id.user_id)) != null) {
            return UserObject.getFirstName(user).replace("\n", "");
        } else if (message != null && message.messageOwner != null && message.messageOwner.fwd_from != null && message.messageOwner.fwd_from.from_name != null) {
            return message.messageOwner.fwd_from.from_name;
        } else if (fromUser != null) {
            if (useForceThreeLines || SharedConfig.useThreeLinesLayout) {
                if (UserObject.isDeleted(fromUser)) {
                    return LocaleController.getString("HiddenName", R.string.HiddenName);
                } else {
                    return ContactsController.formatName(fromUser.first_name, fromUser.last_name).replace("\n", "");
                }
            } else {
                return UserObject.getFirstName(fromUser).replace("\n", "");
            }
        } else if (fromChat != null && fromChat.title != null) {
            return fromChat.title.replace("\n", "");
        }else {
            return "DELETED";
        }
    }

    public SpannableStringBuilder getMessageStringFormatted(String messageFormat, String restrictionReason, CharSequence messageNameString, boolean applyThumbs) {
        SpannableStringBuilder stringBuilder;
        MessageObject captionMessage = getCaptionMessage();
        CharSequence msgText = message != null ? message.messageText : null;
        applyName = true;
        if (!TextUtils.isEmpty(restrictionReason)) {
            stringBuilder = SpannableStringBuilder.valueOf(AndroidUtilities.formatSpannable(messageFormat, restrictionReason, messageNameString));
        } else if (message.messageOwner instanceof TLRPC.TL_messageService) {
            CharSequence mess;
            if (message.messageTextShort != null && (!(message.messageOwner.action instanceof TLRPC.TL_messageActionTopicCreate) || !isTopic)) {
                mess = message.messageTextShort;
            } else {
                mess = message.messageText;
            }
            if (MessageObject.isTopicActionMessage(message)) {
                stringBuilder = AndroidUtilities.formatSpannable(messageFormat, mess, messageNameString);
                if (message.topicIconDrawable[0] != null) {
                    TLRPC.TL_forumTopic topic = MessagesController.getInstance(currentAccount).getTopicsController().findTopic(-message.getDialogId(), MessageObject.getTopicId(message.messageOwner));
                    if (topic != null) {
                        message.topicIconDrawable[0].setColor(topic.icon_color);
                    }
                }
            } else {
                applyName = false;
                stringBuilder = SpannableStringBuilder.valueOf(mess);
            }
        } else if (captionMessage != null && captionMessage.caption != null) {
            MessageObject message = captionMessage;
            CharSequence mess = message.caption.toString();
            String emoji;
            if (!needEmoji) {
                emoji = "";
            } else if (message.isVideo()) {
                emoji = "\uD83D\uDCF9 ";
            } else if (message.isVoice()) {
                emoji = "\uD83C\uDFA4 ";
            } else if (message.isMusic()) {
                emoji = "\uD83C\uDFA7 ";
            } else if (message.isPhoto()) {
                emoji = "\uD83D\uDDBC ";
            } else {
                emoji = "\uD83D\uDCCE ";
            }
            if (message.hasHighlightedWords() && !TextUtils.isEmpty(message.messageOwner.message)) {
                String str = message.messageTrimmedToHighlight;
                if (message.messageTrimmedToHighlight != null) {
                    str = message.messageTrimmedToHighlight;
                }
                int w = getMeasuredWidth() - AndroidUtilities.dp(messagePaddingStart + 23 + 24);
                if (hasNameInMessage) {
                    if (!TextUtils.isEmpty(messageNameString)) {
                        w -= currentMessagePaint.measureText(messageNameString.toString());
                    }
                    w -= currentMessagePaint.measureText(": ");
                }
                if (w > 0) {
                    str = AndroidUtilities.ellipsizeCenterEnd(str, message.highlightedWords.get(0), w, currentMessagePaint, 130).toString();
                }
                stringBuilder = new SpannableStringBuilder(emoji).append(str);
            } else {
                if (mess.length() > 150) {
                    mess = mess.subSequence(0, 150);
                }
                SpannableStringBuilder msgBuilder = new SpannableStringBuilder(mess);
                MediaDataController.addTextStyleRuns(message.messageOwner.entities, mess, msgBuilder, TextStyleSpan.FLAG_STYLE_SPOILER);
                if (message != null && message.messageOwner != null) {
                    MediaDataController.addAnimatedEmojiSpans(message.messageOwner.entities, msgBuilder, currentMessagePaint == null ? null : currentMessagePaint.getFontMetricsInt());
                }
                CharSequence charSequence = new SpannableStringBuilder(emoji).append(AndroidUtilities.replaceNewLines(msgBuilder));
                if (applyThumbs) {
                    charSequence = applyThumbs(charSequence);
                }
                stringBuilder = AndroidUtilities.formatSpannable(messageFormat, charSequence, messageNameString);
            }
        } else if (message.messageOwner.media != null && !message.isMediaEmpty()) {
            currentMessagePaint = Theme.dialogs_messagePrintingPaint[paintIndex];
            String innerMessage;
            String colorKey = Theme.key_chats_attachMessage;
            if (message.messageOwner.media instanceof TLRPC.TL_messageMediaPoll) {
                TLRPC.TL_messageMediaPoll mediaPoll = (TLRPC.TL_messageMediaPoll) message.messageOwner.media;
                if (Build.VERSION.SDK_INT >= 18) {
                    innerMessage = String.format("\uD83D\uDCCA \u2068%s\u2069", mediaPoll.poll.question);
                } else {
                    innerMessage = String.format("\uD83D\uDCCA %s", mediaPoll.poll.question);
                }
            } else if (message.messageOwner.media instanceof TLRPC.TL_messageMediaGame) {
                if (Build.VERSION.SDK_INT >= 18) {
                    innerMessage = String.format("\uD83C\uDFAE \u2068%s\u2069", message.messageOwner.media.game.title);
                } else {
                    innerMessage = String.format("\uD83C\uDFAE %s", message.messageOwner.media.game.title);
                }
            } else if (message.messageOwner.media instanceof TLRPC.TL_messageMediaInvoice) {
                innerMessage = message.messageOwner.media.title;
            } else if (message.type == MessageObject.TYPE_MUSIC) {
                if (Build.VERSION.SDK_INT >= 18) {
                    innerMessage = String.format("\uD83C\uDFA7 \u2068%s - %s\u2069", message.getMusicAuthor(), message.getMusicTitle());
                } else {
                    innerMessage = String.format("\uD83C\uDFA7 %s - %s", message.getMusicAuthor(), message.getMusicTitle());
                }
            } else if (thumbsCount > 1) {
                if (hasVideoThumb) {
                    innerMessage = LocaleController.formatPluralString("Media", groupMessages == null ? 0 : groupMessages.size());
                } else {
                    innerMessage = LocaleController.formatPluralString("Photos", groupMessages == null ? 0 : groupMessages.size());
                }
                colorKey = Theme.key_chats_actionMessage;
            } else {
                innerMessage = msgText.toString();
                colorKey = Theme.key_chats_actionMessage;
            }
            innerMessage = innerMessage.replace('\n', ' ');
            CharSequence message = innerMessage;
            if (applyThumbs) {
                message = applyThumbs(innerMessage);
            }
            stringBuilder = AndroidUtilities.formatSpannable(messageFormat, message, messageNameString);
            if (!isForumCell()) {
                try {
                    stringBuilder.setSpan(new ForegroundColorSpanThemable(colorKey, resourcesProvider), hasNameInMessage ? messageNameString.length() + 2 : 0, stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        } else if (message.messageOwner.message != null) {
            CharSequence mess = message.messageOwner.message;
            if (message.hasHighlightedWords()) {
                if (message.messageTrimmedToHighlight != null) {
                    mess = message.messageTrimmedToHighlight;
                }
                int w = getMeasuredWidth() - AndroidUtilities.dp(messagePaddingStart + 23 + 10);
                if (hasNameInMessage) {
                    if (!TextUtils.isEmpty(messageNameString)) {
                        w -= currentMessagePaint.measureText(messageNameString.toString());
                    }
                    w -= currentMessagePaint.measureText(": ");
                }
                if (w > 0) {
                    mess = AndroidUtilities.ellipsizeCenterEnd(mess, message.highlightedWords.get(0), w, currentMessagePaint, 130).toString();
                }
            } else {
                if (mess.length() > 150) {
                    mess = mess.subSequence(0, 150);
                }
                mess = AndroidUtilities.replaceNewLines(mess);
            }
            mess = new SpannableStringBuilder(mess);
            MediaDataController.addTextStyleRuns(message, (Spannable) mess, TextStyleSpan.FLAG_STYLE_SPOILER);
            if (message != null && message.messageOwner != null) {
                MediaDataController.addAnimatedEmojiSpans(message.messageOwner.entities, mess, currentMessagePaint == null ? null : currentMessagePaint.getFontMetricsInt());
            }
            if (applyThumbs) {
                mess = applyThumbs(mess);
            }
            stringBuilder = AndroidUtilities.formatSpannable(messageFormat, mess, messageNameString);
        } else {
            stringBuilder = SpannableStringBuilder.valueOf("");
        }
        return stringBuilder;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (delegate == null || delegate.canClickButtonInside()) {
            if (lastTopicMessageUnread && canvasButton != null && buttonLayout != null && canvasButton.checkTouchEvent(event)) {
                return true;
            }
        }
        return super.onTouchEvent(event);
    }


    public void setClipProgress(float value) {
        clipProgress = value;
        invalidate();
    }

    public float getClipProgress() {
        return clipProgress;
    }

    public void setTopClip(int value) {
        topClip = value;
    }

    public void setBottomClip(int value) {
        bottomClip = value;
    }

    public void setArchivedPullAnimation(PullForegroundDrawable drawable) {
        archivedChatsDrawable = drawable;
    }

    public int getCurrentDialogFolderId() {
        return currentDialogFolderId;
    }

    public boolean isDialogFolder() {
        return currentDialogFolderId > 0;
    }

    public MessageObject getMessage() {
        return message;
    }

    public void setDialogCellDelegate(DialogCellDelegate delegate) {
        this.delegate = delegate;
    }

    public interface DialogCellDelegate {
        void onButtonClicked(DialogCell dialogCell);
        void onButtonLongPress(DialogCell dialogCell);
        boolean canClickButtonInside();
    }

}
