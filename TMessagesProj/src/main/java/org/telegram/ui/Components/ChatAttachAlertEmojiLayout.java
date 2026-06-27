package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;

@SuppressLint("ViewConstructor")
public class ChatAttachAlertEmojiLayout extends ChatAttachAlert.AttachAlertLayout {

    private final EmojiView emojiView;
    private final boolean sticker;
    private int currentItemTop;

    public ChatAttachAlertEmojiLayout(ChatAttachAlert alert, Context context, Theme.ResourcesProvider resourcesProvider, boolean stickers) {
        super(alert, context, resourcesProvider);
        this.sticker = stickers;

        occupyNavigationBar = true;
        emojiView = new EmojiView(alert.baseFragment, !stickers, stickers, false, getContext(), true, null, null, false, resourcesProvider, false, true);
        emojiView.shouldLightenBackground = false;
        emojiView.setAllow(stickers, false, false);
        addView(emojiView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    public void setDelegate(EmojiView.EmojiViewDelegate delegate) {
        emojiView.setDelegate(delegate);
    }

    @Override
    public void scrollToTop() {
    }

    @Override
    public int needsActionBar() {
        return 1;
    }

    @Override
    public int getListTopPadding() {
        return currentItemTop;
    }

    @Override
    public int getCurrentItemTop() {
        return currentItemTop;
    }

    @Override
    public int getFirstOffset() {
        return getListTopPadding() + dp(56);
    }

    @Override
    public void setTranslationY(float translationY) {
        super.setTranslationY(translationY);
        parentAlert.getSheetContainer().invalidate();
        invalidate();
    }

    @Override
    public void onPreMeasure(int availableWidth, int availableHeight) {
        LayoutParams layoutParams = (LayoutParams) getLayoutParams();
        layoutParams.topMargin = ActionBar.getCurrentActionBarHeight();

        int paddingTop;
        if (!AndroidUtilities.isTablet() && AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y) {
            paddingTop = (int) (availableHeight / 3.5f);
        } else {
            paddingTop = (availableHeight / 5 * 2);
        }
        paddingTop -= dp(52);
        if (paddingTop < 0) {
            paddingTop = 0;
        }

        currentItemTop = Math.max(0, paddingTop + dp(36));
        emojiView.setPadding(0, currentItemTop, 0, dp(48));
    }

    @Override
    public void onShow(ChatAttachAlert.AttachAlertLayout previousLayout) {
        try {
            parentAlert.actionBar.getTitleTextView().setBuildFullLayout(true);
        } catch (Exception ignore) {}
        parentAlert.actionBar.setTitle(LocaleController.getString(sticker ?
            R.string.ChatSticker : R.string.ChatEmoji));
    }
}

