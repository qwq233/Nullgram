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

package org.telegram.ui.ActionBar;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.widget.TextView;

import org.telegram.ui.Components.AnimatedFloat;
import org.telegram.ui.Components.CircularProgressDrawable;
import org.telegram.ui.Components.CubicBezierInterpolator;

public class TextViewWithLoading extends TextView {

    private boolean loading = false;
    private final AnimatedFloat animatedLoading = new AnimatedFloat(this, 320, CubicBezierInterpolator.EASE_OUT_QUINT);
    private CircularProgressDrawable spinner;

    public TextViewWithLoading(Context context) {
        super(context);

        spinner = new CircularProgressDrawable();
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(color);
        spinner.setColor(color);
    }

    public void setLoading(boolean loading, boolean animated) {
        if (this.loading == loading) {
            return;
        }
        this.loading = loading;
        invalidate();
        if (!animated) {
            animatedLoading.force(loading);
        }
    }

    public boolean isLoading() {
        return loading;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float loading = animatedLoading.set(this.loading);

        if (loading < 1) {
            if (loading <= 0) {
                canvas.save();
            } else {
                canvas.saveLayerAlpha(0, 0, getWidth(), getHeight(), (int) (0xFF * (1.0f - loading)), Canvas.ALL_SAVE_FLAG);
            }
            canvas.translate(0, dp(6) * loading);
            super.onDraw(canvas);
            canvas.restore();
        }

        if (loading > 0) {
            int cx = getWidth() / 2, cy = getHeight() / 2;
            cx -= (int) (dp(6) * (1.0f - loading));
            spinner.setAlpha((int) (0xFF * loading));
            spinner.setBounds(
                cx - spinner.getIntrinsicWidth() / 2, cy - spinner.getIntrinsicWidth() / 2,
                cx + spinner.getIntrinsicWidth() / 2, cy + spinner.getIntrinsicHeight() / 2
            );
            spinner.draw(canvas);
            invalidate();
        }

    }

}
