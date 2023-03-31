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
package top.qwq2333.nullgram.ui

import android.content.Context
import android.graphics.Paint
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ViewSwitcher
import org.telegram.ui.ActionBar.SimpleTextView

open class SimpleTextViewSwitcher(context: Context?) : ViewSwitcher(context) {
    fun setText(text: CharSequence?, animated: Boolean) {
        if (!TextUtils.equals(text, currentView.text)) {
            if (animated) {
                nextView.text = text
                showNext()
            } else {
                currentView.text = text
            }
        }
    }

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams) {
        require(child is SimpleTextView)
        super.addView(child, index, params)
    }

    override fun getCurrentView(): SimpleTextView {
        return super.getCurrentView() as SimpleTextView
    }

    override fun getNextView(): SimpleTextView {
        return super.getNextView() as SimpleTextView
    }

    fun invalidateViews() {
        currentView.invalidate()
        nextView.invalidate()
    }

    fun setTextColor(color: Int) {
        currentView.textColor = color
        nextView.textColor = color
    }

    val paint: Paint
        get() = currentView.paint
    val text: CharSequence
        get() = currentView.text
}
