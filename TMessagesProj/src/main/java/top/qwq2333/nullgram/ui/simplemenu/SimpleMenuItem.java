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

import android.content.Context;
import android.graphics.Canvas;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class SimpleMenuItem extends TextView {
    private boolean isSelected;

    public SimpleMenuItem(Context context) {
        super(context);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
        setMinimumHeight(AndroidUtilities.dp(48));
        setGravity(Gravity.CENTER_VERTICAL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isSelected) {
            canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), Theme.dialogs_tabletSeletedPaint);
        }
        super.onDraw(canvas);
    }

    public void setTextAndCheck(CharSequence text, boolean selected, boolean multiline, int padding) {
        setText(text);
        isSelected = selected;
        setMaxLines(multiline ? Integer.MAX_VALUE : 1);
        setPadding(padding, AndroidUtilities.dp(8), padding, AndroidUtilities.dp(8));
    }
}
