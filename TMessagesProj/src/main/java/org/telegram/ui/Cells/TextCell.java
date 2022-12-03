/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.Switch;

public class TextCell extends FrameLayout {

    public final SimpleTextView textView;
    public final AnimatedTextView valueTextView;
    public final RLottieImageView imageView;
    private Switch checkBox;
    private ImageView valueImageView;
    private int leftPadding;
    private boolean needDivider;
    private int offsetFromImage = 71;
    public int imageLeft = 21;
    private boolean inDialogs;
    private boolean prioritizeTitleOverValue;
    private Theme.ResourcesProvider resourcesProvider;
    private boolean attached;


    public TextCell(Context context) {
        this(context, 23, false, false, null);
    }

    public TextCell(Context context, Theme.ResourcesProvider resourcesProvider) {
        this(context, 23, false, false, resourcesProvider);
    }

    public TextCell(Context context, int left, boolean dialog) {
        this(context, left, dialog, false, null);
    }

    public TextCell(Context context, int left, boolean dialog, boolean needCheck, Theme.ResourcesProvider resourcesProvider) {
        super(context);

        this.resourcesProvider = resourcesProvider;
        leftPadding = left;

        textView = new SimpleTextView(context);
        textView.setTextColor(Theme.getColor(dialog ? Theme.key_dialogTextBlack : Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        textView.setTextSize(16);
        textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        textView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT));

        valueTextView = new AnimatedTextView(context);
        valueTextView.setTextColor(Theme.getColor(dialog ? Theme.key_dialogTextBlue2 : Theme.key_windowBackgroundWhiteValueText, resourcesProvider));
        valueTextView.setPadding(0, AndroidUtilities.dp(18), 0, AndroidUtilities.dp(18));
        valueTextView.setTextSize(AndroidUtilities.dp(16));
        valueTextView.setGravity(LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT);
        valueTextView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(valueTextView);

        imageView = new RLottieImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER);
        imageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(dialog ? Theme.key_dialogIcon : Theme.key_windowBackgroundWhiteGrayIcon, resourcesProvider), PorterDuff.Mode.MULTIPLY));
        addView(imageView);

        valueImageView = new ImageView(context);
        valueImageView.setScaleType(ImageView.ScaleType.CENTER);
        addView(valueImageView);

        if (needCheck) {
            checkBox = new Switch(context, resourcesProvider);
            checkBox.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
            addView(checkBox, LayoutHelper.createFrame(37, 20, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, 22, 0, 22, 0));
        }

        setFocusable(true);
    }

    public Switch getCheckBox() {
        return checkBox;
    }

    public void setIsInDialogs() {
        inDialogs = true;
    }

    public SimpleTextView getTextView() {
        return textView;
    }

    public RLottieImageView getImageView() {
        return imageView;
    }

    public AnimatedTextView getValueTextView() {
        return valueTextView;
    }

    public ImageView getValueImageView() {
        return valueImageView;
    }

    public void setPrioritizeTitleOverValue(boolean prioritizeTitleOverValue) {
        this.prioritizeTitleOverValue = prioritizeTitleOverValue;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = AndroidUtilities.dp(48);

        if (prioritizeTitleOverValue) {
            textView.measure(MeasureSpec.makeMeasureSpec(width - AndroidUtilities.dp(71 + leftPadding), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.EXACTLY));
            valueTextView.measure(MeasureSpec.makeMeasureSpec(width - AndroidUtilities.dp(103 + leftPadding) - textView.getTextWidth(), LocaleController.isRTL ? MeasureSpec.AT_MOST : MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.EXACTLY));
        } else {
            valueTextView.measure(MeasureSpec.makeMeasureSpec(width - AndroidUtilities.dp(leftPadding), LocaleController.isRTL ? MeasureSpec.AT_MOST : MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.EXACTLY));
            textView.measure(MeasureSpec.makeMeasureSpec(width - AndroidUtilities.dp(71 + leftPadding) - valueTextView.width(), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.EXACTLY));
        }
        if (imageView.getVisibility() == VISIBLE) {
            imageView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
        }
        if (valueImageView.getVisibility() == VISIBLE) {
            valueImageView.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
        }
        if (checkBox != null) {
            checkBox.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(37), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(20), MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(width, AndroidUtilities.dp(50) + (needDivider ? 1 : 0));
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (checkBox != null) {
            checkBox.setEnabled(enabled);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int height = bottom - top;
        int width = right - left;

        int viewTop = (height - valueTextView.getTextHeight()) / 2;
        int viewLeft = LocaleController.isRTL ? AndroidUtilities.dp(leftPadding) : 0;
        if (prioritizeTitleOverValue && !LocaleController.isRTL) {
             viewLeft = width - valueTextView.getMeasuredWidth() - AndroidUtilities.dp(leftPadding);
        }
        valueTextView.layout(viewLeft, viewTop, viewLeft + valueTextView.getMeasuredWidth(), viewTop + valueTextView.getMeasuredHeight());

        viewTop = (height - textView.getTextHeight()) / 2;
        if (LocaleController.isRTL) {
            viewLeft = getMeasuredWidth() - textView.getMeasuredWidth() - AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? offsetFromImage : leftPadding);
        } else {
            viewLeft = AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? offsetFromImage : leftPadding);
        }
        textView.layout(viewLeft, viewTop, viewLeft + textView.getMeasuredWidth(), viewTop + textView.getMeasuredHeight());

        if (imageView.getVisibility() == VISIBLE) {
            viewTop = AndroidUtilities.dp(5);
            viewLeft = !LocaleController.isRTL ? AndroidUtilities.dp(imageLeft) : width - imageView.getMeasuredWidth() - AndroidUtilities.dp(imageLeft);
            imageView.layout(viewLeft, viewTop, viewLeft + imageView.getMeasuredWidth(), viewTop + imageView.getMeasuredHeight());
        }

        if (valueImageView.getVisibility() == VISIBLE) {
            viewTop = (height - valueImageView.getMeasuredHeight()) / 2;
            viewLeft = LocaleController.isRTL ? AndroidUtilities.dp(23) : width - valueImageView.getMeasuredWidth() - AndroidUtilities.dp(23);
            valueImageView.layout(viewLeft, viewTop, viewLeft + valueImageView.getMeasuredWidth(), viewTop + valueImageView.getMeasuredHeight());
        }
        if (checkBox != null && checkBox.getVisibility() == VISIBLE) {
            viewTop = (height - checkBox.getMeasuredHeight()) / 2;
            viewLeft = LocaleController.isRTL ? AndroidUtilities.dp(22) : width - checkBox.getMeasuredWidth() - AndroidUtilities.dp(22);
            checkBox.layout(viewLeft, viewTop, viewLeft + checkBox.getMeasuredWidth(), viewTop + checkBox.getMeasuredHeight());
        }
    }

    public void setTextColor(int color) {
        textView.setTextColor(color);
    }

    public void setColors(String icon, String text) {
        textView.setTextColor(Theme.getColor(text, resourcesProvider));
        textView.setTag(text);
        if (icon != null) {
            imageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(icon, resourcesProvider), PorterDuff.Mode.MULTIPLY));
            imageView.setTag(icon);
        }
    }

    public void setText(String text, boolean divider) {
        imageLeft = 21;
        textView.setText(text);
        valueTextView.setText(null, false);
        imageView.setVisibility(GONE);
        valueTextView.setVisibility(GONE);
        valueImageView.setVisibility(GONE);
        needDivider = divider;
        setWillNotDraw(!needDivider);
    }

    public void setTextAndIcon(String text, int resId, boolean divider) {
        imageLeft = 21;
        offsetFromImage = 71;
        textView.setText(text);
        valueTextView.setText(null, false);
        imageView.setImageResource(resId);
        imageView.setVisibility(VISIBLE);
        valueTextView.setVisibility(GONE);
        valueImageView.setVisibility(GONE);
        imageView.setPadding(0, AndroidUtilities.dp(7), 0, 0);
        needDivider = divider;
        setWillNotDraw(!needDivider);
    }

    public void setTextAndIcon(String text, Drawable drawable, boolean divider) {
        offsetFromImage = 68;
        imageLeft = 18;
        textView.setText(text);
        valueTextView.setText(null, false);
        imageView.setColorFilter(null);
        if (drawable instanceof RLottieDrawable) {
            imageView.setAnimation((RLottieDrawable) drawable);
        } else {
            imageView.setImageDrawable(drawable);
        }
        imageView.setVisibility(VISIBLE);
        valueTextView.setVisibility(GONE);
        valueImageView.setVisibility(GONE);
        imageView.setPadding(0, AndroidUtilities.dp(6), 0, 0);
        needDivider = divider;
        setWillNotDraw(!needDivider);
    }

    public void setOffsetFromImage(int value) {
        offsetFromImage = value;
    }

    public void setImageLeft(int imageLeft) {
        this.imageLeft = imageLeft;
    }

    public void setTextAndValue(String text, String value, boolean divider) {
        setTextAndValue(text, value, false, divider);
    }

    public void setTextAndValue(String text, String value, boolean animated, boolean divider) {
        imageLeft = 21;
        offsetFromImage = 71;
        textView.setText(text);
        valueTextView.setText(value, animated);
        valueTextView.setVisibility(VISIBLE);
        imageView.setVisibility(GONE);
        valueImageView.setVisibility(GONE);
        needDivider = divider;
        setWillNotDraw(!needDivider);
        if (checkBox != null) {
            checkBox.setVisibility(GONE);
        }
    }

    public void setTextAndValueAndIcon(String text, String value, int resId, boolean divider) {
        setTextAndValueAndIcon(text, value, false, resId, divider);
    }

    public void setTextAndValueAndIcon(String text, String value, boolean animated, int resId, boolean divider) {
        imageLeft = 21;
        offsetFromImage = 71;
        textView.setText(text);
        valueTextView.setText(value, animated);
        valueTextView.setVisibility(VISIBLE);
        valueImageView.setVisibility(GONE);
        imageView.setVisibility(VISIBLE);
        imageView.setPadding(0, AndroidUtilities.dp(7), 0, 0);
        imageView.setImageResource(resId);
        needDivider = divider;
        setWillNotDraw(!needDivider);
        if (checkBox != null) {
            checkBox.setVisibility(GONE);
        }
    }

    public void setTextAndCheckAndIcon(String text, boolean checked, int resId, boolean divider) {
        imageLeft = 21;
        offsetFromImage = 71;
        textView.setText(text);
        valueTextView.setVisibility(GONE);
        valueImageView.setVisibility(GONE);
        if (checkBox != null) {
            checkBox.setVisibility(VISIBLE);
            checkBox.setChecked(checked, false);
        }
        imageView.setVisibility(VISIBLE);
        imageView.setPadding(0, AndroidUtilities.dp(7), 0, 0);
        imageView.setImageResource(resId);
        needDivider = divider;
        setWillNotDraw(!needDivider);
    }

    public void setTextAndValueDrawable(String text, Drawable drawable, boolean divider) {
        imageLeft = 21;
        offsetFromImage = 71;
        textView.setText(text);
        valueTextView.setText(null, false);
        valueImageView.setVisibility(VISIBLE);
        valueImageView.setImageDrawable(drawable);
        valueTextView.setVisibility(GONE);
        imageView.setVisibility(GONE);
        imageView.setPadding(0, AndroidUtilities.dp(7), 0, 0);
        needDivider = divider;
        setWillNotDraw(!needDivider);
        if (checkBox != null) {
            checkBox.setVisibility(GONE);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? (inDialogs ? 72 : 68) : 20), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(imageView.getVisibility() == VISIBLE ? (inDialogs ? 72 : 68) : 20) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        final CharSequence text = textView.getText();
        if (!TextUtils.isEmpty(text)) {
            final CharSequence valueText = valueTextView.getText();
            if (!TextUtils.isEmpty(valueText)) {
                info.setText(text + ": " + valueText);
            } else {
                info.setText(text);
            }
        }
        info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    public void setNeedDivider(boolean needDivider) {
        if (this.needDivider != needDivider) {
            this.needDivider = needDivider;
            setWillNotDraw(!needDivider);
            invalidate();
        }
    }

    public void setChecked(boolean checked) {
        checkBox.setChecked(checked, true);
    }

    public void showEnabledAlpha(boolean show) {
        float alpha = show ? 0.5f : 1f;
        if (attached) {
            if (imageView != null) {
                imageView.animate().alpha(alpha).start();
            }
            if (textView != null) {
                textView.animate().alpha(alpha).start();
            }
            if (valueTextView != null) {
                valueTextView.animate().alpha(alpha).start();
            }
            if (valueImageView != null) {
                valueImageView.animate().alpha(alpha).start();
            }
        } else {
            if (imageView != null) {
                imageView.setAlpha(alpha);
            }
            if (textView != null) {
                textView.setAlpha(alpha);
            }
            if (valueTextView != null) {
                valueTextView.setAlpha(alpha);
            }
            if (valueImageView != null) {
                valueImageView.setAlpha(alpha);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        attached = false;
    }
}
