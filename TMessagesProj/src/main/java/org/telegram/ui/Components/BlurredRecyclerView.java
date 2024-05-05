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

package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SharedConfig;

public class BlurredRecyclerView extends RecyclerListView {

    public int blurTopPadding;
    public int topPadding;
    public int bottomPadding;
    boolean globalIgnoreLayout;
    public int additionalClipBottom;

    public BlurredRecyclerView(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        globalIgnoreLayout = true;
        updateTopPadding();
        super.setPadding(getPaddingLeft(), topPadding + blurTopPadding, getPaddingRight(), getPaddingBottom());
        globalIgnoreLayout = false;
        super.onMeasure(widthSpec, heightSpec);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateTopPadding();
    }

    private void updateTopPadding() {
        if (getLayoutParams() == null) {
            return;
        }
        if (SharedConfig.chatBlurEnabled()) {
            blurTopPadding = AndroidUtilities.dp(203);
            ((MarginLayoutParams) getLayoutParams()).topMargin = -blurTopPadding;
        } else {
            blurTopPadding = 0;
            ((MarginLayoutParams) getLayoutParams()).topMargin = 0;
        }
    }

    @Override
    public void requestLayout() {
        if (globalIgnoreLayout) {
            return;
        }
        super.requestLayout();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (blurTopPadding != 0) {
            canvas.clipRect(0, blurTopPadding, getMeasuredWidth(), getMeasuredHeight() + additionalClipBottom);
            super.dispatchDraw(canvas);
        } else {
            super.dispatchDraw(canvas);
        }
    }

    @Override
    public boolean drawChild(Canvas canvas, View child, long drawingTime) {
        if (child.getY() + child.getMeasuredHeight() < blurTopPadding) {
            return true;
        }
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        topPadding = top;
        bottomPadding = bottom;
        super.setPadding(left, topPadding + blurTopPadding, right, bottomPadding);
    }
}
