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
import android.os.Build;
import android.util.Property;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class SimpleMenuBoundsProperty extends Property<PropertyHolder, Rect> {

    public static final Property<PropertyHolder, Rect> BOUNDS;

    static {
        BOUNDS = new SimpleMenuBoundsProperty("bounds");
    }

    public SimpleMenuBoundsProperty(String name) {
        super(Rect.class, name);
    }

    @Override
    public Rect get(PropertyHolder holder) {
        return holder.getBounds();
    }

    @Override
    public void set(PropertyHolder holder, Rect value) {
        holder.setBounds(value);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            holder.getContentView().invalidate();
        }
    }
}
