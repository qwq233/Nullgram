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
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author Naomi
 * @date 2023/4/6 11:21
 *
 */
object DeepLTranslator : BaseTranslator() {

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

        client.post("https://www2.deepl.com/jsonrpc") {
            contentType(ContentType.Application.Json)
            header("x-app-os-name", "iOS")
            header("x-app-os-version", "16.3.0")
            header("Accept-Language", "en-US,en;q=0.9")
            header("x-app-device", "iPhone13,2")
            header("User-Agent", "DeepL-iOS/2.9.1 iOS 16.3.0 (iPhone13,2)")
            header("x-app-build", "510265")
            header("x-app-version", "2.9.1")
            header("Connection", "keep-alive")
            setBody(getRequestBody(text, from, to))
        }.let {
            when (it.status) {
                HttpStatusCode.OK -> {
                    val jsonObject = JSONObject(it.bodyAsText())
                    if (jsonObject.has("error")) {
                        throw IOException(jsonObject.getString("message"))
                    }
                    val array = jsonObject.getJSONObject("result")

                    return RequestResult(
                        array.getString("lang").lowercase(),
                        array.getJSONArray("texts").getJSONObject(0).getString("text")
                    )
                }

                else -> {
                    Log.w(it.bodyAsText())
                    return RequestResult(from, null, it.status)
                }
            }
        }
    }

    private val id: AtomicLong = AtomicLong(ThreadLocalRandom.current().nextLong(8300000L, 8399999L))

    private fun getRequestBody(text: String, from: String, to: String): String {
        var iCounter = 1
        val iMatcher: Matcher = Pattern.compile("[i]").matcher(text)
        while (iMatcher.find()) {
            iCounter++
        }
        val texts = JSONArray().put(JSONObject().apply {
            put("text", text)
            put("requestAlternatives", 0)
        })
        val lang = JSONObject().apply {
            put("source_lang_user_selected", from)
            put("target_lang", to)
        }
        val commonJobParams = JSONObject().apply {
            put("transcribe_as", "")
            put("wasSpoken", false)
            put("formality", getFormalityString() ?: JSONObject.NULL)
        }
        val params = JSONObject().apply {
            put("texts", texts)
            put("splitting", "newlines")
            put("commonJobParams", commonJobParams)
            put("lang", lang)
            put("timestamp", getTimestamp(iCounter))
        }
        val _body = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("method", "LMT_handle_texts")
            put("params", params)
            put("id", id.incrementAndGet())
        }

        val body: String = if ((id.get() + 3) % 13 == 0L || (id.get() + 5) % 29 == 0L) _body.toString().replace("\"method\":\"", "\"method\" : \"")
        else _body.toString().replace("\"method\":\"", "\"method\": \"")

        android.util.Log.e("Nullgram", "getRequestBody: "+body)
        return body
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

    private fun getTimestamp(iNumber: Int): Long {
        val now = System.currentTimeMillis()
        if (iNumber == 0)
            return now
        return now + iNumber - now % iNumber
    }
}
