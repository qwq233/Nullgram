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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.telegram.messenger.ApplicationLoader
import top.qwq2333.nullgram.utils.Log
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToLong

object ProfileDateHelper {
    private const val JSON_FILE = "id_date.json"

    @Serializable
    private data class ProfileDateResponse(val data: List<ProfileDateData>)

    @Serializable
    data class ProfileDateData(val id: Long, val date: Long)

    private val json = Json { ignoreUnknownKeys = true }

    private val profileDateDataList: List<ProfileDateData> by lazy {
        runCatching {
            ApplicationLoader.applicationContext.assets.open(JSON_FILE).bufferedReader().use {
                json.decodeFromString<ProfileDateResponse>(it.readText()).data
            }
        }.getOrElse {
            Log.e(it)
            emptyList()
        }
    }

    fun getUserTime(key: String, stringRes: Int, date: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = date }
        val year = calendar.get(Calendar.YEAR)
        val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        val timeStr = LocaleController.getInstance().formatterDay.format(Date(date))
        val st = LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, "$year-$month-$day", timeStr)
        return LocaleController.formatString(key, stringRes, st)
    }

    @JvmStatic
    fun getUserTime(userId: Long): String {
        val dataList = profileDateDataList
        if (dataList.isEmpty()) {
            return "unknown"
        }
        for (i in 1 until dataList.size) {
            val data1 = dataList[i - 1]
            val data2 = dataList[i]
            if (userId >= data1.id && userId <= data2.id) {
                val idx = userId - data1.id
                val idxRange = data2.id - data1.id
                val t = idx.toDouble() / idxRange
                val date1 = data1.date
                val date2 = data2.date
                val date = (date1 + t * (date2 - date1)) * 1000.0
                val dateLong = date.roundToLong()
                return getUserTime("RegistrationDateApproximately", R.string.RegistrationDateApproximately, dateLong)
            }
        }
        if (userId <= 1000000L) {
            return getUserTime("RegistrationDateOlder", R.string.RegistrationDateOlder, 1380326400000L)
        }
        return getUserTime("RegistrationDateNewer", R.string.RegistrationDateNewer, 1711889200000L)
    }
}
