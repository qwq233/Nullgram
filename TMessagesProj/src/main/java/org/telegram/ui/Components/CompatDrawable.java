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

package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CompatDrawable extends Drawable {
    public final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CompatDrawable(View view) {
        if (view != null) {
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View v) {
                    onAttachedToWindow();
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View v) {
                    onDetachedToWindow();
                }
            });
            if (view.isAttachedToWindow()) {
                view.post(this::onAttachedToWindow);
            }
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {

    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }
    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
    }
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    public void onAttachedToWindow() {}
    public void onDetachedToWindow() {}
}
