/*
 * Copyright (C) 2019-2023 qwq233 <qwq233@qwq2333.top>
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

package top.qwq2333.nullgram.ui.simplemenu;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * A wrapped {@link Drawable} that force use its own bounds to draw.
 * <p>
 * It maybe a little dirty. But if we don't do that, during the expanding animation, there will be
 * one or two frame using wrong bounds because of parent view sets bounds.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class CustomBoundsDrawable extends DrawableWrapper {

    public CustomBoundsDrawable(Drawable wrappedDrawable) {
        super(wrappedDrawable);
    }

    public void setCustomBounds(@NonNull Rect bounds) {
        setCustomBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    public void setCustomBounds(int left, int top, int right, int bottom) {
        setBounds(left, top, right, bottom);
        getWrappedDrawable().setBounds(left, top, right, bottom);
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
    }

    @Override
    public void setBounds(@NonNull Rect bounds) {
    }
}
