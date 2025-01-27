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
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.NonNull;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.PhotoViewer;

public class PhotoViewerBlurDrawable extends CompatDrawable {

    private final PhotoViewer photoViewer;
    private final View view;
    private final BlurringShader.StoryBlurDrawer backgroundBlur;

    public PhotoViewerBlurDrawable(PhotoViewer photoViewer, BlurringShader.BlurManager blurManager, View view) {
        super(view);
        this.photoViewer = photoViewer;
        this.view = view;
        backgroundBlur = new BlurringShader.StoryBlurDrawer(blurManager, view, BlurringShader.StoryBlurDrawer.BLUR_TYPE_BACKGROUND, false);
    }

    private int rounding = -1;
    public void setRounding(int rounding) {
        this.rounding = rounding;
    }

    private boolean applyBounds = true;
    public PhotoViewerBlurDrawable setApplyBounds(boolean v) {
        applyBounds = v;
        return this;
    }

    private final Path path = new Path();
    private final RectF rect = new RectF();

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Rect bounds = getBounds();
        canvas.save();
        path.rewind();
        final float r;
        final float alpha = (float) paint.getAlpha() / 0xFF;
        if (rounding == -1) {
            r = Math.min(bounds.width(), bounds.height()) / 2.0f;
        } else {
            r = rounding;
        }
        rect.set(bounds);
        path.addRoundRect(rect, r, r, Path.Direction.CW);
        canvas.clipPath(path);
        View v = view;
        while (v != null && v != photoViewer.windowView && v.getParent() instanceof View) {
            canvas.translate(-v.getX(), -v.getY());
            v = (View) v.getParent();
        }
        if (applyBounds) {
            canvas.translate(rect.left, rect.top);
        }
        photoViewer.drawCaptionBlur(canvas, backgroundBlur, Theme.multAlpha(0xFF262626, alpha), Theme.multAlpha(0x33000000, alpha), false, true, false);
        canvas.restore();
    }
}
