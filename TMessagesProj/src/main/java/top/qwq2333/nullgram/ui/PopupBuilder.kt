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
import android.view.View
import android.widget.LinearLayout
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.RadioColorCell
import top.qwq2333.nullgram.ui.simplemenu.SimpleMenuPopupWindow
import top.qwq2333.nullgram.utils.Log

object PopupBuilder {
    private var mPopupWindow: SimpleMenuPopupWindow? = null

    @JvmStatic
    @JvmOverloads
    fun show(
        entries: ArrayList<out CharSequence?>,
        title: String?,
        checkedIndex: Int,
        context: Context?,
        itemView: View?,
        resourcesProvider: Theme.ResourcesProvider? = null,
        listener: SimpleMenuPopupWindow.OnItemClickListener
    ) {
        if (itemView == null) {
            val builder = AlertDialog.Builder(context, resourcesProvider)
            builder.setTitle(title)
            val linearLayout = LinearLayout(context)
            linearLayout.orientation = LinearLayout.VERTICAL
            builder.setView(linearLayout)
            for (a in entries.indices) {
                val cell = RadioColorCell(context)
                cell.setPadding(AndroidUtilities.dp(4f), 0, AndroidUtilities.dp(4f), 0)
                cell.tag = a
                cell.setCheckColor(
                    Theme.getColor(Theme.key_radioBackground, resourcesProvider),
                    Theme.getColor(Theme.key_dialogRadioBackgroundChecked, resourcesProvider)
                )
                cell.setTextAndValue(entries[a] as String?, checkedIndex == a)
                linearLayout.addView(cell)
                cell.setOnClickListener { v: View ->
                    val which = v.tag as Int
                    builder.dismissRunnable.run()
                    listener.onClick(which)
                }
            }
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
            builder.show()
        } else {
            val container = itemView.parent as View
            if (mPopupWindow != null) {
                try {
                    if (mPopupWindow!!.isShowing) mPopupWindow!!.dismiss()
                } catch (e: Exception) {
                    Log.e(e)
                }
            }
            mPopupWindow = SimpleMenuPopupWindow(context)
            mPopupWindow!!.onItemClickListener = listener
            mPopupWindow!!.setEntries(entries.toTypedArray())
            mPopupWindow!!.setSelectedIndex(checkedIndex)
            mPopupWindow!!.show(itemView, container, 0)
        }
    }
}
