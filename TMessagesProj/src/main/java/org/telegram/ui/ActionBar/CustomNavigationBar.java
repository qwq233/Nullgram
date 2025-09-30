/*
 * Copyright (C) 2019-2025 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.telegram.ui.ActionBar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.telegram.messenger.AndroidUtilities;

public class CustomNavigationBar extends View {
    private static final boolean USE_INSETS = Build.VERSION.SDK_INT >= 35;

    private final Paint paint = new Paint();
    private int height;

    public CustomNavigationBar(Context context) {
        super(context);

        if (USE_INSETS) {
            ViewCompat.setOnApplyWindowInsetsListener(this, this::onApplyWindowInsets);
        }
    }

    public void setColor(int color) {
        if (paint.getColor() != color) {
            paint.setColor(color);
            invalidate();
        }
    }

    public int getColor() {
        return paint.getColor();
    }

    @NonNull
    private WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
        final int height = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
        if (this.height != height) {
            this.height = height;
            requestLayout();
        }

        return WindowInsetsCompat.CONSUMED;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!USE_INSETS) {
            height = AndroidUtilities.navigationBarHeight;
        }
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), paint);
    }
}
