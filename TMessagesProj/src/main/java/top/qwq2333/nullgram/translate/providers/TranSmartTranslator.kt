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

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.json.JSONArray
import org.json.JSONObject
import top.qwq2333.nullgram.translate.BaseTranslator
import top.qwq2333.nullgram.utils.Log
import java.util.Date
import java.util.UUID

object TranSmartTranslator : BaseTranslator() {

    private val targetLanguages = listOf(
        "ar", "fr", "fil", "lo", "ja", "it", "hi", "id", "vi",
        "de", "km", "ms", "th", "tr", "zh", "ru", "ko", "pt", "es"
    )

    override fun getTargetLanguages(): List<String> = targetLanguages

    private fun getRandomBrowserVersion(): String {
        val majorVersion = (Math.random() * 17).toInt() + 100
        val minorVersion = (Math.random() * 20).toInt()
        val patchVersion = (Math.random() * 20).toInt()
        return "$majorVersion.$minorVersion.$patchVersion"
    }

    private fun getRandomOperatingSystem(): String {
        val operatingSystems = arrayOf("Mac OS", "Windows")
        val randomIndex = (Math.random() * operatingSystems.size).toInt()
        return operatingSystems[randomIndex]
    }

    private fun getRequestBody(text: String, from: String, to: String): String {
        val source = if (targetLanguages.contains(from)) from else "en"

        val clientKey = "browser-chrome-${getRandomBrowserVersion()}-${getRandomOperatingSystem()}-${UUID.randomUUID()}-${Date().time}"

        val translationRequest = JSONObject()
        translationRequest.put("header", JSONObject().apply {
            put("fn", "auto_translation")
            put("session", "")
            put("client_key", clientKey)
            put("user", "")
        })
        translationRequest.put("type", "plain")
        translationRequest.put("model_category", "normal")
        translationRequest.put("text_domain", "")
        translationRequest.put("source", JSONObject().apply {
            put("lang", source)
            put("text_list", JSONArray(text.split("\n")))
        })
        translationRequest.put("target", JSONObject().apply {
            put("lang", to)
        })

        return translationRequest.toString()
    }

    override suspend fun translateText(text: String, from: String, to: String): RequestResult {
        client.post("https://transmart.qq.com/api/imt") {
            contentType(ContentType.Application.Json)
            setBody(getRequestBody(text, from, to))
        }.let {
            when (it.status) {
                HttpStatusCode.OK -> {
                    val jsonObject = JSONObject(it.bodyAsText())

                    if (!jsonObject.has("auto_translation")) {
                        Log.w(getRequestBody(text, from, to))
                        Log.w(it.bodyAsText())
                        return RequestResult(from, null, HttpStatusCode(HttpStatusCode.BadRequest.value, jsonObject.getString("message")))
                    }

                    return RequestResult(
                        to,
                        jsonObject.getJSONArray("auto_translation").join("\n")
                    )
                }

                else -> {
                    Log.w(it.bodyAsText())
                    return RequestResult(from, null, it.status)
                }
            }
        }
    }

}
