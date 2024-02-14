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

package top.qwq2333.nullgram

import android.content.Context
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.ShadowSectionCell
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.LayoutHelper
import top.qwq2333.gen.Config
import top.qwq2333.nullgram.activity.ChatSettingActivity

fun ChatSettingActivity.createMessageFilterSetter(context: Context, resourcesProvider: Theme.ResourcesProvider? = null) {
    AlertDialog.Builder(context, resourcesProvider).apply {
        setTitle(LocaleController.getString("MessageFilter", R.string.MessageFilter))
        setCancelable(true)
        val editText = EditTextBoldCursor(context).apply {
            layoutParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT).apply {
                rightMargin = AndroidUtilities.dp(24f)
                leftMargin = AndroidUtilities.dp(24f)
                height = AndroidUtilities.dp(48f)
            }
            background = null

            setHintText(LocaleController.getString("Pattern", R.string.Pattern), true)
            setHintColor(Theme.getColor(Theme.key_dialogTextGray, resourcesProvider))
            setHeaderHintColor(Theme.getColor(Theme.key_dialogTextBlue))
            setTransformHintToHeader(true)

            setText(Config.messageFilter)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
            setTextColor(Theme.getColor(Theme.key_dialogTextGray, resourcesProvider))

            setCursorColor(Theme.getColor(Theme.key_dialogTextGray))
            setCursorSize(AndroidUtilities.dp(20f))
            setCursorWidth(1.5f)

            setLineColors(
                Theme.getColor(Theme.key_windowBackgroundWhiteInputField),
                Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated),
                Theme.getColor(Theme.key_text_RedRegular)
            )
        }

        val descView = TextView(context).apply {
            layoutParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT).apply {
                rightMargin = AndroidUtilities.dp(24f)
                leftMargin = AndroidUtilities.dp(24f)
                topMargin = AndroidUtilities.dp(8f)
            }
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10f)
            setTextColor(Theme.getColor(Theme.key_dialogTextGray, resourcesProvider))
            text = LocaleController.getString("MessageFilterDesc", R.string.MessageFilterDesc)
        }

        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(descView)
            addView(ShadowSectionCell(context))
            addView(editText)
        }.also { setView(it) }

        setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
        setPositiveButton(LocaleController.getString("Save", R.string.Save)) { _, _ ->
            Config.messageFilter = editText.text.toString()
        }
    }.show()
}
