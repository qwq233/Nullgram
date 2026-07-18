package org.telegram.ui.Components.chat;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.RoundedCorner;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.inset.InAppKeyboardInsetView;
import org.telegram.ui.Components.inset.WindowInsetsProvider;

public class ChatInputViewsContainer extends FrameLayout {
    private WindowInsetsProvider windowInsetsProvider;

    private final FrameLayout inputIslandBubbleContainer;
    private final FrameLayout inAppKeyboardBubbleContainer;

    private BlurredBackgroundDrawable inputBackgroundDrawable;
    private BlurredBackgroundDrawable underKeyboardBackgroundDrawable;

    public ChatInputViewsContainer(@NonNull Context context) {
        super(context);

        inputIslandBubbleContainer = new FrameLayout(context);
        addView(inputIslandBubbleContainer,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));

        inAppKeyboardBubbleContainer = new FrameLayout(context) {
            @Override
            public void addView(View child, int width, int height) {
                super.addView(child, width, height);
                checkViewsPositions();
            }
        };
        addView(inAppKeyboardBubbleContainer,
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));
    }

    public void setWindowInsetsProvider(WindowInsetsProvider windowInsetsProvider) {
        this.windowInsetsProvider = windowInsetsProvider;
    }

    public void setInputIslandBubbleDrawable(BlurredBackgroundDrawable drawable) {
        inputBackgroundDrawable = drawable;
        inputBackgroundDrawable.setPadding(0);
        inputBackgroundDrawable.setRadius(0);
        inputBackgroundDrawable.setStrokeWidth(0, 0);
        invalidate();
    }

    public void setUnderKeyboardBackgroundDrawable(BlurredBackgroundDrawable drawable) {
        underKeyboardBackgroundDrawable = drawable;
        underKeyboardBackgroundDrawable.enableInAppKeyboardOptimization();
        underKeyboardBackgroundDrawable.setRadius(0);
        underKeyboardBackgroundDrawable.setStrokeWidth(0, 0);
        invalidate();
    }

    public void updateColors() {
        if (inputBackgroundDrawable != null) {
            inputBackgroundDrawable.updateColors();
        }
        if (underKeyboardBackgroundDrawable != null) {
            underKeyboardBackgroundDrawable.updateColors();
        }
        invalidate();
    }

    @NonNull
    public FrameLayout getInputIslandBubbleContainer() {
        return inputIslandBubbleContainer;
    }

    @NonNull
    public FrameLayout getInAppKeyboardBubbleContainer() {
        return inAppKeyboardBubbleContainer;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        checkViewsPositions();
        checkInAppKeyboardChild();
    }

    private void checkInAppKeyboardViewHeight() {
        LayoutParams lp = (LayoutParams) inAppKeyboardBubbleContainer.getLayoutParams();
        final int newHeight = windowInsetsProvider.getInAppKeyboardRecommendedViewHeight();

        if (lp.height != newHeight) {
            lp.height = newHeight;
            requestLayout();
        }
    }

    private float maxBottomInset;
    private float imeBottomInset;
    private boolean needDrawInAppKeyboard;

    public void checkInsets() {
        maxBottomInset = windowInsetsProvider.getAnimatedMaxBottomInset();
        imeBottomInset = windowInsetsProvider.getAnimatedImeBottomInset();
        needDrawInAppKeyboard = windowInsetsProvider.inAppViewIsVisible();

        if ((inAppKeyboardBubbleContainer.getVisibility() == VISIBLE) != needDrawInAppKeyboard) {
            inAppKeyboardBubbleContainer.setVisibility(needDrawInAppKeyboard ? VISIBLE : GONE);
        }

        checkInAppKeyboardViewHeight();
        checkInAppKeyboardChild();

        if (underKeyboardBackgroundDrawable != null) {
            int leftBottomRadius = 0;
            int rightBottomRadius = 0;
            if (Build.VERSION.SDK_INT >= 31) {
                final WindowInsets insets = getRootWindowInsets();
                if (insets != null) {
                    final RoundedCorner bottomLeft = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT);
                    final RoundedCorner bottomRight = insets.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT);
                    leftBottomRadius = bottomLeft == null ? 0 : bottomLeft.getRadius();
                    rightBottomRadius = bottomRight == null ? 0 : bottomRight.getRadius();
                }
            }
            underKeyboardBackgroundDrawable.setRadius(0, 0, rightBottomRadius, leftBottomRadius, true);
        }

        checkViewsPositions();
        invalidate();
    }

    private void checkViewsPositions() {
        inputIslandBubbleContainer.setTranslationY(-maxBottomInset);
        inAppKeyboardBubbleContainer.setTranslationY(inAppKeyboardBubbleContainer.getMeasuredHeight() - imeBottomInset);
    }

    private void checkInAppKeyboardChild() {
        final int navbarHeight = windowInsetsProvider.getCurrentNavigationBarInset();
        final float keyboardHeight = windowInsetsProvider.getAnimatedImeBottomInset();

        for (int a = 0, n = inAppKeyboardBubbleContainer.getChildCount(); a < n; a++) {
            final View child = inAppKeyboardBubbleContainer.getChildAt(a);
            if (child instanceof InAppKeyboardInsetView) {
                InAppKeyboardInsetView insetView = (InAppKeyboardInsetView) child;
                insetView.applyNavigationBarHeight(navbarHeight);
                insetView.applyInAppKeyboardAnimatedHeight(keyboardHeight);
            }
        }
    }

    private float inputBubbleHeight;
    private int inputBubbleHeightRound;

    public void setInputBubbleHeight(float height) {
        inputBubbleHeight = height;
        inputBubbleHeightRound = Math.round(height);
        invalidate();
    }

    public float getInputBubbleHeight() {
        return inputBubbleHeight;
    }

    public float getInputBubbleTop() {
        return getInputBubbleBottom() - inputBubbleHeight;
    }

    public float getInputBubbleBottom() {
        return getMeasuredHeight() - maxBottomInset;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        checkViewsPositions();
        checkInAppKeyboardChild();
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        final int contentBottom = Math.round(getInputBubbleBottom() + bubbleInputTranslationY);
        final int contentTop = contentBottom - inputBubbleHeightRound;

        if (inputBubbleAlpha > 0 && inputBackgroundDrawable != null) {
            final int shadowHeight = Theme.chat_composeShadowDrawable.getIntrinsicHeight();
            final int oldShadowAlpha = Theme.chat_composeShadowDrawable.getAlpha();
            Theme.chat_composeShadowDrawable.setAlpha(inputBubbleAlpha);
            Theme.chat_composeShadowDrawable.setBounds(0, contentTop - shadowHeight, getMeasuredWidth(), contentTop);
            Theme.chat_composeShadowDrawable.draw(canvas);
            Theme.chat_composeShadowDrawable.setAlpha(oldShadowAlpha);

            final int backgroundBottom = needDrawInAppKeyboard
                ? getMeasuredHeight() - (int) imeBottomInset
                : getMeasuredHeight();
            inputBackgroundDrawable.setAlpha(inputBubbleAlpha);
            inputBackgroundDrawable.setBounds(0, contentTop, getMeasuredWidth(), Math.max(contentTop, backgroundBottom));
            inputBackgroundDrawable.draw(canvas);
        }

        if (needDrawInAppKeyboard && underKeyboardBackgroundDrawable != null) {
            underKeyboardBackgroundDrawable.setBounds(
                0,
                getMeasuredHeight() - (int) imeBottomInset,
                getMeasuredWidth(),
                getMeasuredHeight()
            );
            underKeyboardBackgroundDrawable.draw(canvas);
        }

        super.dispatchDraw(canvas);
    }

    @Override
    protected boolean drawChild(@NonNull Canvas canvas, View child, long drawingTime) {
        final boolean clipKeyboard = child == inAppKeyboardBubbleContainer && underKeyboardBackgroundDrawable != null;
        if (clipKeyboard) {
            canvas.save();
            canvas.clipPath(underKeyboardBackgroundDrawable.getPath());
        }

        final boolean result = super.drawChild(canvas, child, drawingTime);
        if (clipKeyboard) {
            canvas.restore();
        }
        return result;
    }

    private float bubbleInputTranslationY;
    private int inputBubbleAlpha = 255;

    public void setInputBubbleTranslationY(float translationY) {
        bubbleInputTranslationY = translationY;
        invalidate();
    }

    public void setInputBubbleAlpha(int alpha) {
        if (inputBubbleAlpha != alpha) {
            inputBubbleAlpha = alpha;
            invalidate();
        }
    }

    private boolean captured;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            final int y = (int) event.getY();
            final int contentBottom = Math.round(getInputBubbleBottom() + bubbleInputTranslationY);
            final int contentTop = contentBottom - inputBubbleHeightRound;
            final boolean inputHit = inputBubbleAlpha == 255 && y >= contentTop && y < contentBottom;
            final boolean keyboardHit = needDrawInAppKeyboard && underKeyboardBackgroundDrawable != null
                && y >= getMeasuredHeight() - imeBottomInset
                && y < getMeasuredHeight() - windowInsetsProvider.getCurrentNavigationBarInset();
            captured = inputHit || keyboardHit;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            captured = false;
        }

        return captured;
    }
}
