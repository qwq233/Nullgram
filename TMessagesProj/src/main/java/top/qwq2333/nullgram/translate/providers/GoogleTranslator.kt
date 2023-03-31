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

package top.qwq2333.nullgram.translate.providers

import android.text.TextUtils
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.userAgent
import org.json.JSONObject
import top.qwq2333.nullgram.translate.BaseTranslator
import top.qwq2333.nullgram.utils.Log
import top.qwq2333.nullgram.utils.encodeUrl
import java.util.Locale

object GoogleTranslator : BaseTranslator() {

    private val targetLanguages = listOf(
        "sq", "ar", "am", "az", "ga", "et", "or", "eu", "be", "bg", "is", "pl", "bs",
        "fa", "af", "tt", "da", "de", "ru", "fr", "tl", "fi", "fy", "km", "ka", "gu",
        "kk", "ht", "ko", "ha", "nl", "ky", "gl", "ca", "cs", "kn", "co", "hr", "ku",
        "la", "lv", "lo", "lt", "lb", "rw", "ro", "mg", "mt", "mr", "ml", "ms", "mk",
        "mi", "mn", "bn", "my", "hmn", "xh", "zu", "ne", "no", "pa", "pt", "ps", "ny",
        "ja", "sv", "sm", "sr", "st", "si", "eo", "sk", "sl", "sw", "gd", "ceb", "so",
        "tg", "te", "ta", "th", "tr", "tk", "cy", "ug", "ur", "uk", "uz", "es", "iw",
        "el", "haw", "sd", "hu", "sn", "hy", "ig", "it", "yi", "hi", "su", "id", "jw",
        "en", "yo", "vi", "zh-TW", "zh-CN", "zh"
    )

    private val devices = listOf(
        "Linux; U; Android 10; Pixel 4",
        "Linux; U; Android 10; Pixel 4 XL",
        "Linux; U; Android 10; Pixel 4a",
        "Linux; U; Android 10; Pixel 4a XL",
        "Linux; U; Android 11; Pixel 4",
        "Linux; U; Android 11; Pixel 4 XL",
        "Linux; U; Android 11; Pixel 4a",
        "Linux; U; Android 11; Pixel 4a XL",
        "Linux; U; Android 11; Pixel 5",
        "Linux; U; Android 11; Pixel 5a",
        "Linux; U; Android 12; Pixel 4",
        "Linux; U; Android 12; Pixel 4 XL",
        "Linux; U; Android 12; Pixel 4a",
        "Linux; U; Android 12; Pixel 4a XL",
        "Linux; U; Android 12; Pixel 5",
        "Linux; U; Android 12; Pixel 5a",
        "Linux; U; Android 12; Pixel 6",
        "Linux; U; Android 12; Pixel 6 Pro"
    )

    override fun convertLanguageCode(language: String, country: String?): String {
        val languageLowerCase = language.lowercase(Locale.getDefault())
        val code: String = if (!TextUtils.isEmpty(country)) {
            val countryUpperCase = country!!.uppercase(Locale.getDefault())
            if (targetLanguages.contains("$languageLowerCase-$countryUpperCase")) {
                "$languageLowerCase-$countryUpperCase"
            } else if (languageLowerCase == "zh") {
                when (countryUpperCase) {
                    "DG" -> "zh-CN"
                    "HK" -> "zh-TW"
                    else -> language
                }
            } else {
                languageLowerCase
            }
        } else {
            languageLowerCase
        }
        return code
    }

    override suspend fun translateText(text: String, from: String, to: String): RequestResult {
        Log.d("text: $text")
        Log.d("from: $from")
        Log.d("to: $to")
        val url = "https://translate.google.com/translate_a/single?dj=1" +
            "&q=" + text.encodeUrl() +
            "&sl=auto" +
            "&tl=" + to +
            "&ie=UTF-8&oe=UTF-8&client=at&dt=t&otf=2"

        client.get(url) {
            userAgent("GoogleTranslate/6.28.0.05.421483610 (${devices.random()})")
        }.let {
            Log.d("response: ${it.bodyAsText()}")
            when (it.status) {
                HttpStatusCode.OK -> {
                    val jsonObject = JSONObject(it.bodyAsText())
                    buildString {
                        val array = jsonObject.getJSONArray("sentences")
                        for (i in 0 until array.length()) {
                            append(array.getJSONObject(i).getString("trans"))
                        }
                        val sourceLang = jsonObject.getJSONObject("ld_result").getJSONArray("srclangs").getString(0)
                        return RequestResult(sourceLang, toString())
                    }
                }

                else -> {
                    return RequestResult(from, null, it.status)
                }
            }
        }
    }

    override fun getTargetLanguages(): List<String> = targetLanguages

}
