package org.telegram.ui.Components.chat.layouts;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.lerp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;

import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;

@SuppressLint("ViewConstructor")
public class ChatActivityChannelButtonsLayout extends FrameLayout implements FactorAnimator.Target {
    public static final int BUTTON_SEARCH = 0;
    public static final int BUTTON_GIFT = 1;
    public static final int BUTTON_DIRECT = 2;
    public static final int BUTTON_GIGA_GROUP_INFO = 3;
    private static final int BUTTONS_COUNT = 4;

    private final ButtonHolder[] buttonHolders = new ButtonHolder[BUTTONS_COUNT];
    private final OnClickListener[] onClickListeners = new OnClickListener[BUTTONS_COUNT];
    private final OnButtonFullyVisibleListener[] onButtonFullyVisible = new OnButtonFullyVisibleListener[BUTTONS_COUNT];
    private final FrameLayout container;

    private static final @DrawableRes int[] buttonIcons = new int[] {
        R.drawable.msg_search,
        R.drawable.input_gift_s,
        R.drawable.input_message,
        R.drawable.msg_help
    };
    private static final int[] buttonsOrderLeft = new int[] {
        BUTTON_SEARCH
    };
    private static final int[] buttonsOrderRight = new int[] {
        BUTTON_GIFT,
        BUTTON_DIRECT,
        BUTTON_GIGA_GROUP_INFO
    };

    private final Theme.ResourcesProvider resourcesProvider;

    public ChatActivityChannelButtonsLayout(@NonNull Context context,
                                            Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;

        container = new FrameLayout(context);
        addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER_VERTICAL));
    }

    public void updateColors() {
        for (ButtonHolder holder : buttonHolders) {
            if (holder != null) {
                final int color = Theme.getColor(Theme.key_chat_fieldOverlayText, resourcesProvider);
                holder.button.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
                holder.button.setBackground(Theme.createSelectorDrawable(
                    Theme.multAlpha(color, .10f), Theme.RIPPLE_MASK_CIRCLE_20DP));
            }
        }
    }

    public FrameLayout getContainer() {
        return container;
    }

    public void showButton(final int buttonId, boolean show, boolean animated) {
        if (buttonId < 0 || buttonId >= buttonHolders.length) {
            return;
        }

        if (buttonHolders[buttonId] == null && !show) {
            return;
        }

        if (buttonHolders[buttonId] == null) {
            final int animatorId = (buttonId << 16) | VISIBILITY_ANIMATOR_ID;
            final BoolAnimator visibilityAnimator = new BoolAnimator(animatorId, this,
                CubicBezierInterpolator.EASE_OUT_QUINT, 300);

            final ImageView button = new ImageView(getContext());
            button.setImageResource(buttonIcons[buttonId]);
            button.setScaleType(ImageView.ScaleType.CENTER);

            ScaleStateListAnimator.apply(button, .13f, 2f);
            button.setVisibility(GONE);
            button.setOnClickListener(v -> {
                if (onClickListeners[buttonId] != null) {
                    onClickListeners[buttonId].onClick(v);
                }
            });
            addView(button, LayoutHelper.createFrame(48, 48));

            buttonHolders[buttonId] = new ButtonHolder(button, visibilityAnimator);
            updateColors();
            checkButtonsPositionsAndVisibility();
        }

        buttonHolders[buttonId].visibilityAnimator.setValue(show, animated);
    }

    public boolean isButtonVisible(final int buttonId) {
        if (buttonId < 0 || buttonId >= buttonHolders.length || buttonHolders[buttonId] == null) {
            return false;
        }

        return buttonHolders[buttonId].visibilityAnimator.getValue();
    }

    public void setButtonOnClickListener(int buttonId, View.OnClickListener listener) {
        this.onClickListeners[buttonId] = listener;
    }

    public void setButtonOnFullyVisibleListener(int buttonId, OnButtonFullyVisibleListener listener) {
        this.onButtonFullyVisible[buttonId] = listener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        checkContainerPaddings(false);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        checkButtonsPositionsAndVisibility();
    }

    private static final int VISIBILITY_ANIMATOR_ID = 1;

    private float totalVisibilityFactor;
    public void setTotalVisibilityFactor(float factor) {
        if (totalVisibilityFactor != factor) {
            totalVisibilityFactor = factor;
            checkButtonsPositionsAndVisibility();
            invalidate();
        }
    }

    @Override
    public void onFactorChanged(int id, float factor, float fraction, FactorAnimator callee) {
        final int buttonId = id >> 16;
        final int animatorId = id & 0xFFFF;
        if (buttonId < 0 || buttonId >= buttonHolders.length || buttonHolders[buttonId] == null) {
            return;
        }

        if (animatorId == VISIBILITY_ANIMATOR_ID) {
            checkContainerPaddings(true);
            checkButtonsPositionsAndVisibility();
            invalidate();
        }
    }

    @Override
    public void onFactorChangeFinished(int id, float finalFactor, FactorAnimator callee) {
        final int buttonId = id >> 16;
        final int animatorId = id & 0xFFFF;
        if (buttonId < 0 || buttonId >= buttonHolders.length || buttonHolders[buttonId] == null) {
            return;
        }

        final ButtonHolder holder = buttonHolders[buttonId];
        if (animatorId == VISIBILITY_ANIMATOR_ID) {
            if (holder.visibilityAnimator.getValue()) {
                if (onButtonFullyVisible[buttonId] != null) {
                    onButtonFullyVisible[buttonId].onButtonFullyVisible(holder.button, buttonId, !holder.wasShown);
                }
                holder.wasShown = true;
            }
        }
    }

    private float totalWidthLeft, totalWidthRight;

    private void checkContainerPaddings(boolean canRequestLayout) {
        int paddingLeft = 0, paddingRight = 0;
        for (final int buttonId : buttonsOrderLeft) {
            final ButtonHolder holder = buttonHolders[buttonId];
            if (holder == null) {
                continue;
            }
            paddingLeft += holder.visibilityAnimator.getValue() ? dp(48) : 0;
        }

        for (final int buttonId : buttonsOrderRight) {
            final ButtonHolder holder = buttonHolders[buttonId];
            if (holder == null) {
                continue;
            }
            paddingRight += holder.visibilityAnimator.getValue() ? dp(48) : 0;
        }

        final MarginLayoutParams lp = (MarginLayoutParams) container.getLayoutParams();

        if (lp.leftMargin != paddingLeft || lp.rightMargin != paddingRight) {
            lp.leftMargin = paddingLeft;
            lp.rightMargin = paddingRight;
            if (canRequestLayout) {
                container.requestLayout();
            }
        }
    }

    private void checkButtonsPositionsAndVisibility() {
        totalWidthLeft = 0;
        totalWidthRight = 0;

        for (final ButtonHolder holder: buttonHolders) {
            if (holder == null) {
                continue;
            }

            final float visibility = holder.visibilityAnimator.getFloatValue() * totalVisibilityFactor;
            holder.button.setVisibility(visibility > 0 ? VISIBLE : GONE);
            holder.button.setAlpha(visibility);
            holder.button.setScaleX(lerp(0.4f, 1f, visibility));
            holder.button.setScaleY(lerp(0.4f, 1f, visibility));
        }

        for (final int buttonId : buttonsOrderLeft) {
            final ButtonHolder holder = buttonHolders[buttonId];
            if (holder == null) {
                continue;
            }

            final float width = holder.visibilityAnimator.getFloatValue() * dp(48);
            holder.button.setTranslationX(totalWidthLeft);
            totalWidthLeft += width;
        }

        for (final int buttonId : buttonsOrderRight) {
            final ButtonHolder holder = buttonHolders[buttonId];
            if (holder == null) {
                continue;
            }

            final float width = holder.visibilityAnimator.getFloatValue() * dp(48);
            holder.button.setTranslationX(getMeasuredWidth() - holder.button.getMeasuredWidth() - totalWidthRight);
            totalWidthRight += width;
        }

        if (totalVisibilityFactor < 1) {
            for (final int buttonId : buttonsOrderLeft) {
                final ButtonHolder holder = buttonHolders[buttonId];
                if (holder == null) {
                    continue;
                }

                holder.button.setTranslationX(holder.button.getTranslationX() - totalWidthLeft * (1 - totalVisibilityFactor));
            }

            for (final int buttonId : buttonsOrderRight) {
                final ButtonHolder holder = buttonHolders[buttonId];
                if (holder == null) {
                    continue;
                }

                holder.button.setTranslationX(holder.button.getTranslationX() + totalWidthRight * (1 - totalVisibilityFactor));
            }

            totalWidthLeft *= totalVisibilityFactor;
            totalWidthRight *= totalVisibilityFactor;
        }

    }
    public interface OnButtonFullyVisibleListener {
        void onButtonFullyVisible(View v, int buttonId, boolean firstTime);
    }

    private static class ButtonHolder {
        public final ImageView button;
        public final BoolAnimator visibilityAnimator;
        public boolean wasShown;

        private ButtonHolder(ImageView button, BoolAnimator visibilityAnimator) {
            this.button = button;
            this.visibilityAnimator = visibilityAnimator;
        }
    }
}
