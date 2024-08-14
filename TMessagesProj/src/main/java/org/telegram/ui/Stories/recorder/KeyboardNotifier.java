/*
 * Copyright (C) 2019-2024 qwq233 <qwq233@qwq2333.top>
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

package org.telegram.ui.Stories.recorder;

import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;

public class KeyboardNotifier {

    private final View rootView;
    private View realRootView;
    private final Utilities.Callback<Integer> listener;
    public boolean ignoring;
    private boolean awaitingKeyboard;

    private final Rect rect = new Rect();

    public KeyboardNotifier(@NonNull View rootView, Utilities.Callback<Integer> listener) {
        this(rootView, false, listener);
    }

    public KeyboardNotifier(@NonNull View rootView, boolean getRootView, Utilities.Callback<Integer> listener) {
        this.rootView = rootView;
        this.listener = listener;
        realRootView = rootView;

        if (this.rootView.isAttachedToWindow()) {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
            rootView.addOnLayoutChangeListener(onLayoutChangeListener);
        }
        this.rootView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(@NonNull View v) {
                if (getRootView) {
                    realRootView = v.getRootView();
                }
                rootView.getViewTreeObserver().addOnGlobalLayoutListener(onGlobalLayoutListener);
                rootView.addOnLayoutChangeListener(onLayoutChangeListener);
            }

            @Override
            public void onViewDetachedFromWindow(@NonNull View v) {
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(onGlobalLayoutListener);
                rootView.removeOnLayoutChangeListener(onLayoutChangeListener);
            }
        });
    }

    private final View.OnLayoutChangeListener onLayoutChangeListener = (view, l, t, r, b, ol, ot, or, ob) -> update();
    private final ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener = this::update;

    private int lastKeyboardHeight;
    private int keyboardHeight;

    private void update() {
        if (ignoring) {
            return;
        }

        rootView.getWindowVisibleDisplayFrame(rect);
        final int screenHeight = (realRootView == null ? rootView : realRootView).getHeight();
        keyboardHeight = screenHeight - rect.bottom;
        final boolean unique = lastKeyboardHeight != keyboardHeight;
        lastKeyboardHeight = keyboardHeight;

        if (unique) {
            fire();
        }
    }

    public int getKeyboardHeight() {
        return keyboardHeight;
    }

    public boolean keyboardVisible() {
        return keyboardHeight > AndroidUtilities.navigationBarHeight + AndroidUtilities.dp(20) || awaitingKeyboard;
    }

    public void ignore(boolean ignore) {
        ignoring = ignore;
        update();
    }

    public void fire() {
        if (awaitingKeyboard) {
            if (keyboardHeight < AndroidUtilities.navigationBarHeight + AndroidUtilities.dp(20)) {
                return;
            } else {
                awaitingKeyboard = false;
            }
        }

        if (listener != null) {
            listener.run(keyboardHeight);
        }
    }

    public void awaitKeyboard() {
        awaitingKeyboard = true;
    }

}
