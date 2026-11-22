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
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.json.JSONArray
import org.json.JSONObject
import top.qwq2333.nullgram.config.ConfigManager
import top.qwq2333.nullgram.translate.BaseTranslator
import top.qwq2333.nullgram.utils.Defines
import top.qwq2333.nullgram.utils.Log
import java.io.IOException
import java.util.Locale

/**
 * @author Naomi
 * @date 2023/4/6 11:21
 *
 */
object DeepLTranslator : BaseTranslator() {

    private const val TRANSLATE_URL = "https://oneshot-free.www.deepl.com/v1/translate"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    private val targetLanguages = listOf(
        "bg", "cs", "da", "de", "el", "en-GB", "en-US", "en", "es", "et",
        "fi", "fr", "hu", "id", "it", "ja", "lt", "lv", "nl", "pl", "pt-BR",
        "pt-PT", "pt", "ro", "ru", "sk", "sl", "sv", "tr", "uk", "zh"
    )

    override fun getTargetLanguages(): List<String> = targetLanguages

    override fun convertLanguageCode(language: String, country: String?): String {
        val languageLowerCase: String = language.lowercase(Locale.getDefault())
        val code: String = if (!TextUtils.isEmpty(country)) {
            val countryUpperCase: String = country!!.uppercase(Locale.getDefault())
            if (targetLanguages.contains("$languageLowerCase-$countryUpperCase")) {
                "$languageLowerCase-$countryUpperCase"
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

        val targetLang = when (to.lowercase(Locale.getDefault())) {
            "zh" -> "zh-Hans"
            "pt" -> "pt-BR"
            else -> to
        }

        val payload = JSONObject().apply {
            put("text", JSONArray().put(text))
            put("target_lang", targetLang)
            if (from.isNotEmpty() && !from.equals("auto", ignoreCase = true)) {
                put("source_lang", from)
            }
            put("usage_type", "translate")
        }

        val response = client.post(TRANSLATE_URL) {
            contentType(ContentType.Application.Json)
            header("Authorization", "None")
            header("Referer", "https://www.deepl.com/")
            header("Origin", "https://www.deepl.com")
            header("User-Agent", USER_AGENT)
            setBody(payload.toString())
        }

        return when (response.status) {
            HttpStatusCode.OK -> {
                val jsonObject = JSONObject(response.bodyAsText())
                if (jsonObject.has("error")) {
                    throw IOException(jsonObject.getString("message"))
                }
                val translations = jsonObject.getJSONArray("translations")
                val first = translations.getJSONObject(0)
                val detected = first.optString("detected_source_language", from).lowercase(Locale.getDefault())
                val translatedText = first.getString("text")
                RequestResult(detected, translatedText)
            }

            else -> {
                Log.w(response.bodyAsText())
                RequestResult(from, null, response.status)
            }
        }
    }

    const val FORMALITY_DEFAULT = 0
    const val FORMALITY_MORE = 1
    const val FORMALITY_LESS = 2

    private fun getFormalityString(): String? {
        return when (ConfigManager.getIntOrDefault(Defines.deepLFormality, -1)) {
            FORMALITY_DEFAULT -> null
            FORMALITY_MORE -> "formal"
            FORMALITY_LESS -> "informal"
            else -> null
        }
    }
}
