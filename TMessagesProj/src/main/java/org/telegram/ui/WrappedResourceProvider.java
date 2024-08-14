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

package org.telegram.ui;

import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.SparseIntArray;

import org.telegram.ui.ActionBar.Theme;

public class WrappedResourceProvider implements Theme.ResourcesProvider {

    public SparseIntArray sparseIntArray = new SparseIntArray();

    Theme.ResourcesProvider resourcesProvider;
    public WrappedResourceProvider(Theme.ResourcesProvider resourcesProvider) {
        this.resourcesProvider = resourcesProvider;
        appendColors();
    }

    public void appendColors() {

    }

    @Override
    public int getColor(int key) {
        int index = sparseIntArray.indexOfKey(key);
        if (index >= 0) {
            return sparseIntArray.valueAt(index);
        }
        if (resourcesProvider == null) {
            return Theme.getColor(key);
        }
        return resourcesProvider.getColor(key);
    }

    @Override
    public int getColorOrDefault(int key) {
        if (resourcesProvider == null) {
            return Theme.getColor(key);
        }
        return resourcesProvider.getColorOrDefault(key);
    }

    @Override
    public int getCurrentColor(int key) {
        if (resourcesProvider == null) return Theme.getColor(key);
        return resourcesProvider.getCurrentColor(key);
    }

    @Override
    public void setAnimatedColor(int key, int color) {
        if (resourcesProvider != null) {
            resourcesProvider.setAnimatedColor(key, color);
        }
    }

    @Override
    public Drawable getDrawable(String drawableKey) {
        if (resourcesProvider == null) {
            return Theme.getThemeDrawable(drawableKey);
        }
        return resourcesProvider.getDrawable(drawableKey);
    }

    @Override
    public Paint getPaint(String paintKey) {
        if (resourcesProvider == null) {
            return Theme.getThemePaint(paintKey);
        }
        return resourcesProvider.getPaint(paintKey);
    }

    @Override
    public boolean hasGradientService() {
        if (resourcesProvider == null) {
            return Theme.hasGradientService();
        }
        return resourcesProvider.hasGradientService();
    }

    @Override
    public void applyServiceShaderMatrix(int w, int h, float translationX, float translationY) {
        if (resourcesProvider == null) {
            Theme.applyServiceShaderMatrix(w, h, translationX, translationY);
        } else {
            resourcesProvider.applyServiceShaderMatrix(w, h, translationX, translationY);
        }
    }

    @Override
    public ColorFilter getAnimatedEmojiColorFilter() {
        if (resourcesProvider == null) {
            return Theme.getAnimatedEmojiColorFilter(null);
        }
        return resourcesProvider.getAnimatedEmojiColorFilter();
    }
}
