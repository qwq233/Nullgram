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
import android.util.Base64
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONArray
import top.qwq2333.nullgram.translate.BaseTranslator
import top.qwq2333.nullgram.utils.Log
import top.qwq2333.nullgram.utils.encodeUrl
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object MicrosoftTranslator : BaseTranslator() {
    private val targetLanguages = listOf(
        "sq", "ar", "az", "ga", "et", "or", "mww", "bg", "is", "pl", "bs", "fa", "ko",
        "da", "de", "ru", "fr", "zh-TW", "fil", "fj", "fi", "gu", "kk", "ht", "nl",
        "ca", "zh-CN", "cs", "kn", "otq", "hr", "lv", "lt", "ro", "mg", "mt", "mr",
        "ml", "ms", "mi", "bn", "af", "ne", "nb", "pa", "pt", "pt-PT", "ja", "sv", "sm",
        "sr-Latn", "sr-Cyrl", "sk", "sl", "sw", "ty", "te", "ta", "th", "to", "tr", "cy",
        "ur", "uk", "es", "he", "el", "hu", "hy", "it", "hi", "id", "en", "yua", "yue",
        "vi", "am", "as", "prs", "fr-CA", "iu", "km", "tlh-Latn", "ku", "kmr", "lo", "my", "ps", "ti"
    )

    @Serializable
    private data class Request(val Text: String)

    override fun getTargetLanguages(): List<String> = targetLanguages

    override suspend fun translateText(text: String, from: String, to: String): RequestResult {
        Log.d("text: $text")
        Log.d("from: $from")
        Log.d("to: $to")
        val url = "api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=&to=$to"

        val uuid = UUID.randomUUID().toString().replace("-", "")
        val currentTime = getCurrentTime()
        val bytes = String.format(
            "%s%s%s%s",
            "MSTranslatorAndroidApp",
            url.encodeUrl(),
            currentTime,
            uuid
        ).lowercase().toByteArray()
        val secretKeySpec =
            SecretKeySpec(Base64.decode("oik6PdDdMnOXemTbwvMn9de/h9lFnfBaCWbGMMZqqoSaQaqUOqjVGm5NqsmjcBI1x+sS9ugjB55HEJWRiFXYFw", Base64.NO_PADDING or Base64.NO_WRAP), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKeySpec)
        val key = String.format(
            "%s::%s::%s::%s",
            "MSTranslatorAndroidApp",
            Base64.encodeToString(mac.doFinal(bytes), 2),
            currentTime,
            uuid
        )

        Log.d("Microsoft Translator posting")
        Log.d("key: $key")
        Log.d(Json.encodeToString(listOf(Request(text))))

        client.post("https://$url") {
            contentType(ContentType.Application.Json)
            header("X-Mt-Signature", key)
            header("User-Agent", "okhttp/4.5.0")
            setBody(listOf(Request(text)))

        }.let {
            when (it.status) {
                HttpStatusCode.OK -> {
                    val jsonObject = JSONArray(it.bodyAsText()).getJSONObject(0)
                    if (!jsonObject.has("translations") && jsonObject.has("message")) {
                        throw IOException(jsonObject.getString("message"))
                    }
                    val array = jsonObject.getJSONArray("translations")

                    return RequestResult(jsonObject.getJSONObject("detectedLanguage").getString("language"), array.getJSONObject(0).getString("text"))
                }

                else -> {
                    Log.w(it.bodyAsText())
                    return RequestResult(from, null, it.status)
                }
            }

        }
    }


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

    override fun convertLanguageCode(code: String, reverse: Boolean): String {
        if (reverse) {
            if (code == "zh-Hans") {
                return "zh-CN"
            } else if (code == "zh-Hant") {
                return "zh-TW"
            }
        } else {
            if (code == "zh-CN") {
                return "zh-Hans"
            } else if (code == "zh-TW") {
                return "zh-Hant"
            }
        }
        return code
    }

    private fun getCurrentTime(): String {
        val simpleDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US)
        simpleDateFormat.timeZone = TimeZone.getTimeZone("GMT")
        return simpleDateFormat.format(Date(Calendar.getInstance(TimeZone.getTimeZone("GMT")).timeInMillis)).lowercase() + "GMT"
    }

}
