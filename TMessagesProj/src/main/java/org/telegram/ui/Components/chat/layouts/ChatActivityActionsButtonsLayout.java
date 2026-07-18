package org.telegram.ui.Components.chat.layouts;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.lerp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;

import me.vkryl.android.AnimatorUtils;
import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;

@SuppressLint("ViewConstructor")
public class ChatActivityActionsButtonsLayout extends FrameLayout {
    private final Theme.ResourcesProvider resourcesProvider;

    private final ButtonHolder replyButton = new ButtonHolder();
    private final ButtonHolder forwardButton = new ButtonHolder();

    public ChatActivityActionsButtonsLayout(@NonNull Context context,
                                            Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;

        addTextView(replyButton, LocaleController.getString(R.string.Reply), R.drawable.input_reply, true);
        addTextView(forwardButton, LocaleController.getString(R.string.Forward), R.drawable.input_forward, false);
        setClipChildren(false);

        addView(replyButton.button, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP));
        addView(forwardButton.button, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.RIGHT | Gravity.TOP));
        updateColors();
    }

    public void setReplyButtonOnClickListener(View.OnClickListener listener) {
        replyButton.button.setOnClickListener(listener);
    }

    public void setForwardButtonOnClickListener(View.OnClickListener listener) {
        forwardButton.button.setOnClickListener(listener);
    }

    public View getForwardButton() {
        return forwardButton.button;
    }

    private void addTextView(ButtonHolder holder, String text, @DrawableRes int iconRes, boolean reply) {
        TextView button = new TextView(getContext());
        button.setText(text);
        button.setGravity(Gravity.CENTER_VERTICAL);
        button.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        button.setPadding(AndroidUtilities.dp(reply ? 14 : 21), 0, AndroidUtilities.dp(21), 0);
        button.setCompoundDrawablePadding(AndroidUtilities.dp(reply ? 7 : 6));
        button.setTypeface(AndroidUtilities.bold());
        Drawable image = getContext().getResources().getDrawable(iconRes).mutate();
        button.setCompoundDrawablesWithIntrinsicBounds(image, null, null, null);

        holder.textView = holder.button = button;
        /*if (getDialogId() == UserObject.VERIFY) {
            button.setVisibility(View.GONE);
        }*/
    }


    public void showReplyButton(boolean visible, boolean animated) {
        replyButton.visibilityAnimator.setValue(visible, animated);
    }

    public void setReplyButtonEnabled(boolean enabled, boolean animated) {
        replyButton.enabledAnimator.setValue(enabled, animated);
        replyButton.button.setEnabled(enabled);
    }

    public void showForwardButton(boolean visible, boolean animated) {
        forwardButton.visibilityAnimator.setValue(visible, animated);
    }

    public void setForwardButtonEnabled(boolean enabled, boolean animated) {
        forwardButton.enabledAnimator.setValue(enabled, animated);
        forwardButton.button.setEnabled(enabled);
    }

    public void updateColors() {
        updateButtonColors(replyButton);
        updateButtonColors(forwardButton);
    }

    private void updateButtonColors(ButtonHolder holder) {
        final int color = Theme.getColor(Theme.key_actionBarActionModeDefaultIcon, resourcesProvider);
        holder.textView.setTextColor(color);
        Drawable icon = holder.textView.getCompoundDrawables()[0];
        if (icon != null) {
            icon.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
        }
        holder.textView.setBackground(Theme.createSelectorDrawable(
            Theme.getColor(Theme.key_actionBarActionModeDefaultSelector, resourcesProvider), 3));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        checkButtonsPositionsAndVisibility();
    }

    private float totalVisibilityFactor;
    public void setTotalVisibilityFactor(float factor) {
        if (totalVisibilityFactor != factor) {
            totalVisibilityFactor = factor;
            checkButtonsPositionsAndVisibility();
        }
    }

    private void checkButtonsPositionsAndVisibility() {
        checkHolderPositionsAndVisibility(forwardButton);
        checkHolderPositionsAndVisibility(replyButton);
    }

    private void checkHolderPositionsAndVisibility(ButtonHolder holder) {
        final float visibility = totalVisibilityFactor * holder.visibilityAnimator.getFloatValue();
        final float offsetY = dp(48) * (1f - visibility);
        float offsetX = getMeasuredWidth() / 2f * (1f - AnimatorUtils.DECELERATE_INTERPOLATOR.getInterpolation(visibility));
        if (holder == replyButton) {
            offsetX *= -1;
        }

        holder.button.setTranslationX(offsetX);
        holder.button.setTranslationY(offsetY);
        holder.button.setAlpha(visibility * lerp(0.5f, 1f, holder.enabledAnimator.getFloatValue()));
        holder.button.setVisibility(visibility > 0 ? VISIBLE : INVISIBLE);
    }

    private class ButtonHolder implements FactorAnimator.Target {
        public TextView button;
        public TextView textView;

        public BoolAnimator visibilityAnimator = new BoolAnimator(0, this, CubicBezierInterpolator.EASE_OUT_QUINT, 350, true);
        public BoolAnimator enabledAnimator = new BoolAnimator(1, this, CubicBezierInterpolator.EASE_OUT_QUINT, 350, true);

        @Override
        public void onFactorChanged(int id, float factor, float fraction, FactorAnimator callee) {
            checkHolderPositionsAndVisibility(this);
        }
    }
}
