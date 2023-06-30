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
package top.qwq2333.nullgram.helpers

import android.R
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PatternMatcher
import androidx.annotation.RequiresApi
import org.telegram.messenger.ApplicationLoader
import org.telegram.ui.ActionBar.Theme
import top.qwq2333.nullgram.utils.Log

@RequiresApi(api = Build.VERSION_CODES.S)
object MonetHelper {
    private val ids = hashMapOf<String, Int>().apply {
        put("a1_0", R.color.system_accent1_0)
        put("a1_10", R.color.system_accent1_10)
        put("a1_50", R.color.system_accent1_50)
        put("a1_100", R.color.system_accent1_100)
        put("a1_200", R.color.system_accent1_200)
        put("a1_300", R.color.system_accent1_300)
        put("a1_400", R.color.system_accent1_400)
        put("a1_500", R.color.system_accent1_500)
        put("a1_600", R.color.system_accent1_600)
        put("a1_700", R.color.system_accent1_700)
        put("a1_800", R.color.system_accent1_800)
        put("a1_900", R.color.system_accent1_900)
        put("a1_1000", R.color.system_accent1_1000)
        put("a2_0", R.color.system_accent2_0)
        put("a2_10", R.color.system_accent2_10)
        put("a2_50", R.color.system_accent2_50)
        put("a2_100", R.color.system_accent2_100)
        put("a2_200", R.color.system_accent2_200)
        put("a2_300", R.color.system_accent2_300)
        put("a2_400", R.color.system_accent2_400)
        put("a2_500", R.color.system_accent2_500)
        put("a2_600", R.color.system_accent2_600)
        put("a2_700", R.color.system_accent2_700)
        put("a2_800", R.color.system_accent2_800)
        put("a2_900", R.color.system_accent2_900)
        put("a2_1000", R.color.system_accent2_1000)
        put("a3_0", R.color.system_accent3_0)
        put("a3_10", R.color.system_accent3_10)
        put("a3_50", R.color.system_accent3_50)
        put("a3_100", R.color.system_accent3_100)
        put("a3_200", R.color.system_accent3_200)
        put("a3_300", R.color.system_accent3_300)
        put("a3_400", R.color.system_accent3_400)
        put("a3_500", R.color.system_accent3_500)
        put("a3_600", R.color.system_accent3_600)
        put("a3_700", R.color.system_accent3_700)
        put("a3_800", R.color.system_accent3_800)
        put("a3_900", R.color.system_accent3_900)
        put("a3_1000", R.color.system_accent3_1000)
        put("n1_0", R.color.system_neutral1_0)
        put("n1_10", R.color.system_neutral1_10)
        put("n1_50", R.color.system_neutral1_50)
        put("n1_100", R.color.system_neutral1_100)
        put("n1_200", R.color.system_neutral1_200)
        put("n1_300", R.color.system_neutral1_300)
        put("n1_400", R.color.system_neutral1_400)
        put("n1_500", R.color.system_neutral1_500)
        put("n1_600", R.color.system_neutral1_600)
        put("n1_700", R.color.system_neutral1_700)
        put("n1_800", R.color.system_neutral1_800)
        put("n1_900", R.color.system_neutral1_900)
        put("n1_1000", R.color.system_neutral1_1000)
        put("n2_0", R.color.system_neutral2_0)
        put("n2_10", R.color.system_neutral2_10)
        put("n2_50", R.color.system_neutral2_50)
        put("n2_100", R.color.system_neutral2_100)
        put("n2_200", R.color.system_neutral2_200)
        put("n2_300", R.color.system_neutral2_300)
        put("n2_400", R.color.system_neutral2_400)
        put("n2_500", R.color.system_neutral2_500)
        put("n2_600", R.color.system_neutral2_600)
        put("n2_700", R.color.system_neutral2_700)
        put("n2_800", R.color.system_neutral2_800)
        put("n2_900", R.color.system_neutral2_900)
        put("n2_1000", R.color.system_neutral2_1000)
        put("monetRedDark", org.telegram.messenger.R.color.monetRedDark)
        put("monetRedLight", org.telegram.messenger.R.color.monetRedLight)
        put("monetRedCall", org.telegram.messenger.R.color.monetRedCall)
        put("monetGreenCall", org.telegram.messenger.R.color.monetGreenCall)
    }
    private const val ACTION_OVERLAY_CHANGED = "android.intent.action.OVERLAY_CHANGED"
    private val overlayChangeReceiver = OverlayChangeReceiver()

    @JvmStatic
    fun getColor(color: String): Int = getColor(color, false)

    @JvmStatic
    fun getColor(color: String, amoled: Boolean): Int = try {
        val id = ids.getOrDefault(if (amoled && "n1_900" == color) "n1_1000" else color, 0)
        ApplicationLoader.applicationContext.getColor(id)
    } catch (e: Exception) {
        Log.e("Theme", "Error loading color $color")
        e.printStackTrace()
        0
    }

    @JvmStatic
    fun registerReceiver(context: Context) = overlayChangeReceiver.register(context)

    @JvmStatic
    fun unregisterReceiver(context: Context) = try {
        overlayChangeReceiver.unregister(context)
    } catch (e: IllegalArgumentException) {
        Log.w(e)
    }

    private class OverlayChangeReceiver : BroadcastReceiver() {
        fun register(context: Context) {
            val packageFilter = IntentFilter(ACTION_OVERLAY_CHANGED)
            packageFilter.addDataScheme("package")
            packageFilter.addDataSchemeSpecificPart("android", PatternMatcher.PATTERN_LITERAL)
            context.registerReceiver(this, packageFilter)
        }

        fun unregister(context: Context) {
            context.unregisterReceiver(this)
        }

        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_OVERLAY_CHANGED == intent.action) {
                if (Theme.getActiveTheme().isMonet) {
                    Theme.applyTheme(Theme.getActiveTheme())
                }
            }
        }
    }
}
