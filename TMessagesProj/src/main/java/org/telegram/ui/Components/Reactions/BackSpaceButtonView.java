/*
 * Copyright (C) 2019-2025 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this software.
 *  If not, see
 * <https://www.gnu.org/licenses/>
 */

package org.telegram.ui.Components.Reactions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.CombinedDrawable;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Stories.RoundRectOutlineProvider;

@SuppressLint("ViewConstructor")
public class BackSpaceButtonView extends FrameLayout {

    private final Theme.ResourcesProvider resourcesProvider;
    private final ImageView backspaceButton;
    private boolean backspacePressed;
    private boolean backspaceOnce;
    private Utilities.Callback<Boolean> onBackspace;

    public BackSpaceButtonView(@NonNull Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;

        backspaceButton = new ImageView(context) {
            private long lastClick = 0;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (System.currentTimeMillis() < lastClick + 350) {
                        return false;
                    }
                    lastClick = System.currentTimeMillis();
                    backspacePressed = true;
                    backspaceOnce = false;
                    postBackspaceRunnable(350);
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP) {
                    backspacePressed = false;
                    if (!backspaceOnce) {
                        if (onBackspace != null) {
                            onBackspace.run(false);
                            try {
                                backspaceButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                            } catch (Exception ignored) {}
                        }
                    }
                }
                super.onTouchEvent(event);
                return true;
            }
        };
        backspaceButton.setHapticFeedbackEnabled(true);
        backspaceButton.setImageResource(R.drawable.smiles_tab_clear);
        backspaceButton.setColorFilter(new PorterDuffColorFilter(getThemedColor(Theme.key_chat_emojiPanelBackspace), PorterDuff.Mode.MULTIPLY));
        backspaceButton.setScaleType(ImageView.ScaleType.CENTER);
        backspaceButton.setContentDescription(LocaleController.getString(R.string.AccDescrBackspace));
        backspaceButton.setFocusable(true);
        backspaceButton.setOnClickListener(v -> {

        });
        addView(backspaceButton, LayoutHelper.createFrame(36, 36, Gravity.CENTER));

        int rippleColor = Theme.getColor(Theme.key_listSelector);
        Drawable drawable = Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(36), getThemedColor(Theme.key_windowBackgroundWhite), rippleColor);
        if (Build.VERSION.SDK_INT >= 21) {
            backspaceButton.setBackground(drawable);
            backspaceButton.setOutlineProvider(new RoundRectOutlineProvider(18));
            backspaceButton.setElevation(AndroidUtilities.dp(1));
            backspaceButton.setClipToOutline(true);
        } else {
            Drawable shadowDrawable = context.getResources().getDrawable(R.drawable.floating_shadow).mutate();
            shadowDrawable.setColorFilter(new PorterDuffColorFilter(0xff000000, PorterDuff.Mode.MULTIPLY));
            CombinedDrawable combinedDrawable = new CombinedDrawable(shadowDrawable, drawable, 0, 0);
            combinedDrawable.setIconSize(AndroidUtilities.dp(36), AndroidUtilities.dp(36));
            drawable = combinedDrawable;
            backspaceButton.setBackground(drawable);
        }

        setClickable(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(42), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(42), MeasureSpec.EXACTLY)
        );
    }

    public void setOnBackspace(Utilities.Callback<Boolean> onBackspace) {
        this.onBackspace = onBackspace;
    }

    private void postBackspaceRunnable(final int time) {
        AndroidUtilities.runOnUIThread(() -> {
            if (!backspacePressed) {
                return;
            }
            if (onBackspace != null) {
                onBackspace.run(time < 300);
                try {
                    backspaceButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                } catch (Exception ignored) {}
            }
            backspaceOnce = true;
            postBackspaceRunnable(Math.max(50, time - 100));
        }, time);
    }

    private int getThemedColor(int key) {
        if (resourcesProvider != null) {
            return resourcesProvider.getColor(key);
        }
        return Theme.getColor(key);
    }
}
