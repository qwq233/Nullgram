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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.GridLayout
import android.widget.ImageView
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.ActionBarPopupWindow
import org.telegram.ui.ActionBar.ActionBarPopupWindow.ActionBarPopupWindowLayout
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.LayoutHelper
import top.qwq2333.nullgram.helpers.FolderIconHelper
import top.qwq2333.nullgram.helpers.FolderIconHelper.getTabIcon
import java.util.concurrent.atomic.AtomicReference


object IconSelector {
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    @JvmStatic
    fun show(fragment: BaseFragment, view: View, selectedIcon: String, onIconSelectedListener: OnIconSelectedListener) {
        selectedPaint.color = Theme.getColor(Theme.key_windowBackgroundWhiteValueText)
        selectedPaint.alpha = 40

        val context: Context = fragment.parentActivity

        val layout = ActionBarPopupWindowLayout(context)
        val backgroundPaddings = Rect()
        val shadowDrawable: Drawable = fragment.parentActivity.resources.getDrawable(R.drawable.popup_fixed_alert).mutate()
        shadowDrawable.getPadding(backgroundPaddings)
        layout.backgroundColor = Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground)

        val location = IntArray(2)
        view.getLocationInWindow(location)

        val popupX: Int = location[0] - AndroidUtilities.dp(8f) - backgroundPaddings.left + view.measuredWidth
        val popupY: Int = location[1] - AndroidUtilities.dp(8f) - backgroundPaddings.top + view.measuredHeight

        val scrimPopupWindowRef = AtomicReference<ActionBarPopupWindow?>()

        val gridLayout = GridLayout(context)
        var columnCount = 6
        while (AndroidUtilities.displaySize.x - popupX < 48 * columnCount + AndroidUtilities.dp(8f)) {
            columnCount--
        }
        gridLayout.columnCount = columnCount

        for (icon in FolderIconHelper.folderIcons.keys.toTypedArray<String>()) {
            val imageView: ImageView = object : ImageView(context) {
                override fun onDraw(canvas: Canvas) {
                    if (isSelected) {
                        AndroidUtilities.rectTmp[0f, 0f, measuredWidth.toFloat()] = measuredHeight.toFloat()
                        canvas.drawRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(4f).toFloat(), AndroidUtilities.dp(4f).toFloat(), selectedPaint)
                    }
                    super.onDraw(canvas)
                }
            }
            val selected = icon == selectedIcon
            imageView.scaleType = ImageView.ScaleType.CENTER
            imageView.background = Theme.createRadSelectorDrawable(
                if (selected) Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteValueText), .1f) else Theme.getColor(
                    Theme.key_listSelector
                ), AndroidUtilities.dp(2f), AndroidUtilities.dp(2f)
            )
            imageView.colorFilter =
                PorterDuffColorFilter(Theme.getColor(if (selected) Theme.key_windowBackgroundWhiteValueText else Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.MULTIPLY)
            imageView.setImageResource(getTabIcon(icon))
            imageView.isSelected = selected
            imageView.setOnClickListener { v: View? ->
                if (selectedIcon == icon) {
                    return@setOnClickListener
                }
                if (scrimPopupWindowRef.get() != null) {
                    scrimPopupWindowRef.getAndSet(null)!!.dismiss()
                }
                onIconSelectedListener.onIconSelected(icon)
            }
            gridLayout.addView(imageView, LayoutHelper.createFrame(48, 48, Gravity.CENTER))
        }
        layout.addView(gridLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4f, 4f, 4f, 4f))

        val scrimPopupWindow = ActionBarPopupWindow(layout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT)
        scrimPopupWindowRef.set(scrimPopupWindow)
        scrimPopupWindow.setPauseNotifications(true)
        scrimPopupWindow.setDismissAnimationDuration(220)
        scrimPopupWindow.isOutsideTouchable = true
        scrimPopupWindow.isClippingEnabled = true
        scrimPopupWindow.animationStyle = R.style.PopupContextAnimation
        scrimPopupWindow.isFocusable = true
        layout.measure(
            View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000f), View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000f), View.MeasureSpec.AT_MOST)
        )
        scrimPopupWindow.inputMethodMode = ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED
        scrimPopupWindow.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
        scrimPopupWindow.contentView.isFocusableInTouchMode = true
        scrimPopupWindow.showAtLocation(view, Gravity.LEFT or Gravity.TOP, popupX, popupY)
        scrimPopupWindow.dimBehind()
    }

    interface OnIconSelectedListener {
        fun onIconSelected(emoticon: String?)
    }
}
