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
import java.util.UUID
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

    private val uuid = UUID.randomUUID().toString()

    override suspend fun translateText(text: String, from: String, to: String): RequestResult {
        Log.d("text: $text")
        Log.d("from: $from")
        Log.d("to: $to")

        client.post("https://www2.deepl.com/jsonrpc") {
            contentType(ContentType.Application.Json)
            header("Referer", "https://www.deepl.com/")
            header("Accept-Encoding", "gzip")
            header("User-Agent", "DeepL/1.8(52) Android 13 (Pixel 5;aarch64)")
            header("Client-Id", uuid)
            header("x-instance", uuid)
            header("x-app-os-name", "Android")
            header("x-app-os-version", "13")
            header("x-app-version", "1.8")
            header("x-app-build", "52")
            header("x-app-device", "Pixel 5")
            header("x-app-instance-id", uuid)
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

    const val FORMALITY_DEFAULT = 0
    const val FORMALITY_MORE = 1
    const val FORMALITY_LESS = 2

    private val id: AtomicLong = AtomicLong(ThreadLocalRandom.current().nextLong("10000000000".toLong()))

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
            put("regionalVariant", JSONObject.NULL)
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

        val body: String = if ((id.get() + 3) % 13 == 0L || (id.get() + 5) % 29 == 0L) _body.toString().replace("hod\":\"", "hod\" : \"")
        else _body.toString().replace("hod\":\"", "hod\": \"")

        return body
    }

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
        return now + iNumber - now % iNumber
    }
}
