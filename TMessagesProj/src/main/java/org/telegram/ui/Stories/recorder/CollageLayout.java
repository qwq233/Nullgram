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

package org.telegram.ui.Stories.recorder;

import android.graphics.RectF;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.BuildVars;

import java.util.ArrayList;

public class CollageLayout {

    private static ArrayList<CollageLayout> layouts;
    public static ArrayList<CollageLayout> getLayouts() {
        if (layouts == null) {
            layouts = new ArrayList<>();
            layouts.add(new CollageLayout("./."));
            layouts.add(new CollageLayout(".."));
            layouts.add(new CollageLayout("../."));
            layouts.add(new CollageLayout("./.."));
            layouts.add(new CollageLayout("././."));
            layouts.add(new CollageLayout("..."));
            layouts.add(new CollageLayout("../.."));
            layouts.add(new CollageLayout("./../.."));
            layouts.add(new CollageLayout("../../."));
            layouts.add(new CollageLayout("../../.."));
            if (BuildVars.DEBUG_PRIVATE_VERSION) {
                layouts.add(new CollageLayout("../../../.."));
                layouts.add(new CollageLayout(".../.../..."));
                layouts.add(new CollageLayout("..../..../...."));
                layouts.add(new CollageLayout(".../.../.../..."));
            }
        }
        return layouts;
    }

    public static CollageLayout of(int count) {
        for (CollageLayout layout : getLayouts()) {
            if (layout.parts.size() >= count)
                return layout;
        }
        return null;
    }

    public static int getMaxCount() {
        int max = 0;
        for (CollageLayout layout : getLayouts())
            max = Math.max(max, layout.parts.size());
        return max;
    }

    private final String src;
    public final int w, h;
    public final int[] columns;
    public final ArrayList<Part> parts = new ArrayList<>();

    public CollageLayout(@Nullable String schema) {
        if (schema == null) schema = ".";
        src = schema;
        final String[] rows = src.split("/");
        h = rows.length;
        columns = new int[h];
        int maxW = 0;
        for (int y = 0; y < rows.length; ++y) {
            columns[y] = rows[y].length();
            maxW = Math.max(maxW, rows[y].length());
        }
        w = maxW;
        for (int y = 0; y < rows.length; ++y) {
            for (int x = 0; x < rows[y].length(); ++x) {
                parts.add(new Part(this, x, y));
            }
        }
    }

    public CollageLayout delete(int index) {
        if (index < 0 || index >= parts.size()) return null;
        ArrayList<Part> newParts = new ArrayList<>(parts);
        newParts.remove(index);
        final StringBuilder newSource = new StringBuilder();
        for (int i = 0, y = 0; i < newParts.size(); ++i) {
            final Part p = newParts.get(i);
            if (p.y != y) {
                newSource.append("/");
                y = p.y;
            }
            newSource.append(".");
        }
        return new CollageLayout(newSource.toString());
    }

    public static class Part {
        public final CollageLayout layout;
        public final int x, y;
        private Part(CollageLayout layout, int x, int y) {
            this.layout = layout;
            this.x = x;
            this.y = y;
        }

        public final float l(float w) {
            return ((float) w / layout.columns[y] * x);
        }
        public final float t(float h) {
            return ((float) h / layout.h * y);
        }
        public final float r(float w) {
            return ((float) w / layout.columns[y] * (x + 1));
        }
        public final float b(float h) {
            return ((float) h / layout.h * (y + 1));
        }

        public final float w(float w) {
            return (float) w / layout.columns[y];
        }
        public final float h(float h) {
            return (float) h / layout.h;
        }

        public final void bounds(RectF rect, float w, float h) {
            rect.set(l(w), t(h), r(w), b(h));
        }
    }

    @NonNull
    @Override
    public String toString() {
        return src;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof CollageLayout) {
            return TextUtils.equals(src, ((CollageLayout) obj).src);
        }
        return false;
    }
}
