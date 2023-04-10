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

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.json.JSONObject
import top.qwq2333.nullgram.translate.BaseTranslator
import top.qwq2333.nullgram.utils.Log
import top.qwq2333.nullgram.utils.encodeUrl
import java.io.IOException
import java.util.UUID

/**
 * @author Naomi
 * @date 2023/4/6 10:21
 *
 */
object YandexTranslator : BaseTranslator() {
    private val targetLanguages = listOf(
        "af", "sq", "am", "ar", "hy", "az", "ba", "eu", "be", "bn", "bs", "bg", "my",
        "ca", "ceb", "zh", "cv", "hr", "cs", "da", "nl", "sjn", "emj", "en", "eo",
        "et", "fi", "fr", "gl", "ka", "de", "el", "gu", "ht", "he", "mrj", "hi",
        "hu", "is", "id", "ga", "it", "ja", "jv", "kn", "kk", "kazlat", "km", "ko",
        "ky", "lo", "la", "lv", "lt", "lb", "mk", "mg", "ms", "ml", "mt", "mi", "mr",
        "mhr", "mn", "ne", "no", "pap", "fa", "pl", "pt", "pa", "ro", "ru", "gd", "sr",
        "si", "sk", "sl", "es", "su", "sw", "sv", "tl", "tg", "ta", "tt", "te", "th", "tr",
        "udm", "uk", "ur", "uz", "uzbcyr", "vi", "cy", "xh", "sah", "yi", "zu"
    )

    override fun getTargetLanguages(): List<String> = targetLanguages

    override suspend fun translateText(text: String, from: String, to: String): RequestResult {
        Log.d("text: $text")
        Log.d("from: $from")
        Log.d("to: $to")
        val uuid = UUID.randomUUID().toString().replace("-", "")

        client.post("https://translate.yandex.net/api/v1/tr.json/translate?id=$uuid-0-0&srv=android") {
            contentType(ContentType.Application.FormUrlEncoded)
            header("User-Agent", "ru.yandex.translate/21.15.4.21402814 (Xiaomi Redmi K20 Pro; Android 11)")
            setBody("lang=$to&text=${text.encodeUrl()}")
        }.let {
            when (it.status) {
                HttpStatusCode.OK -> {
                    val jsonObject = JSONObject(it.bodyAsText())
                    if (!jsonObject.has("text") && jsonObject.has("message")) {
                        throw IOException(jsonObject.getString("message"))
                    }
                    val array = jsonObject.getJSONArray("text")

                    return RequestResult(jsonObject.getString("lang").split("-")[0], array.getString(0))
                }

                else -> {
                    Log.w(it.bodyAsText())
                    return RequestResult(from, null, it.status)
                }
            }
        }
    }

}
