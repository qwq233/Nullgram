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

package top.qwq2333.nullgram.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController

object TypefaceUtils {
    private val TEST_TEXT: String = if (listOf("zh", "ja", "ko").contains(LocaleController.getInstance().currentLocale.language)) {
        "日"
    } else {
        "R"
    }

    private val CANVAS_SIZE = AndroidUtilities.dp(12f)
    private val PAINT: Paint = Paint().apply {
        textSize = CANVAS_SIZE.toFloat()
        isAntiAlias = false
        isSubpixelText = false
        isFakeBoldText = false
    }

    @JvmStatic
    fun isMediumWeightSupported(): Boolean = testTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))

    @JvmStatic
    fun isItalicSupported(): Boolean = testTypeface(Typeface.create("sans-serif", Typeface.ITALIC))

    private fun testTypeface(typeface: Typeface): Boolean {
        val canvas = Canvas()
        val bitmap1 = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ALPHA_8)
        canvas.setBitmap(bitmap1)
        PAINT.setTypeface(null)
        canvas.drawText(TEST_TEXT, 0f, CANVAS_SIZE.toFloat(), PAINT)
        val bitmap2 = Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ALPHA_8)
        canvas.setBitmap(bitmap2)
        PAINT.setTypeface(typeface)
        canvas.drawText(TEST_TEXT, 0f, CANVAS_SIZE.toFloat(), PAINT)
        val supported = !bitmap1.sameAs(bitmap2)
        AndroidUtilities.recycleBitmaps(listOf(bitmap1, bitmap2))
        return supported
    }
}
