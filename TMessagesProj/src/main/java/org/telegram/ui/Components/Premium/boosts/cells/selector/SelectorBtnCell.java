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

package org.telegram.ui.Components.Premium.boosts.cells.selector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedFloat;
import org.telegram.ui.Components.RecyclerListView;

@SuppressLint("ViewConstructor")
public class SelectorBtnCell extends LinearLayout {

    private final Theme.ResourcesProvider resourcesProvider;
    private final RecyclerListView listView;
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final AnimatedFloat alpha = new AnimatedFloat(this);

    public SelectorBtnCell(Context context, Theme.ResourcesProvider resourcesProvider, RecyclerListView listView) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.listView = listView;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        dividerPaint.setColor(Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider));
        if (listView != null) {
            dividerPaint.setAlpha((int) (0xFF * alpha.set(listView.canScrollVertically(1) ? 1 : 0)));
        } else {
            dividerPaint.setAlpha((int) (0xFF * alpha.set(1)));
        }
        canvas.drawRect(0, 0, getWidth(), AndroidUtilities.getShadowHeight(), dividerPaint);
    }
}
