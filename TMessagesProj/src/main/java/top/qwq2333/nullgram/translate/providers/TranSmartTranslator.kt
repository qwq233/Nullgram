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

@file:Suppress("PropertyName")

package top.qwq2333.nullgram.translate.providers

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import top.qwq2333.nullgram.translate.BaseTranslator
import top.qwq2333.nullgram.utils.Log
import java.util.Date
import java.util.UUID

object TranSmartTranslator : BaseTranslator() {

    private val targetLanguages = listOf(
        "ar", "fr", "fil", "lo", "ja", "it", "hi", "id", "vi", "de", "km", "ms", "th", "tr", "zh", "ru", "ko", "pt", "es"
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

    @Serializable
    data class Request(
        val header: Header, val type: String = "plain", val model_category: String = "normal", val text_domain: String = "", val source: Source, val target: Target
    ) {
        @Serializable
        data class Header(
            val fn: String = "auto_translation", val session: String = "", val client_key: String, val user: String = ""
        )

        @Serializable
        data class Source(
            val lang: String, val text_list: List<String>
        )

        @Serializable
        data class Target(
            val lang: String
        )
    }

    @Serializable
    data class Response(
        val header: Header, val auto_translation: List<String>?,
    ) {
        @Serializable
        data class Header(val type: String, val ret_code: String, val time_cost: Double, val request_id: String)
    }

    override suspend fun translateText(text: String, from: String, to: String): RequestResult {
        client.post("https://transmart.qq.com/api/imt") {
            contentType(ContentType.Application.Json)
            setBody(
                Request(
                    header = Request.Header(client_key = "browser-chrome-${getRandomBrowserVersion()}-${getRandomOperatingSystem()}-${UUID.randomUUID()}-${Date().time}"),
                    source = Request.Source(
                        if (targetLanguages.contains(from)) from else "en", text.split("\n")
                    ),
                    target = Request.Target(to)
                )
            )
        }.let {
            when (it.status) {
                HttpStatusCode.OK -> {
                    Log.d(it.bodyAsText())
                    val resp: Response = it.body()

                    if (resp.header.ret_code != "succ") {
                        return RequestResult(from, null, HttpStatusCode(HttpStatusCode.BadRequest.value, resp.header.ret_code))
                    }

                    return RequestResult(
                        to, StringBuilder().apply {
                            resp.auto_translation?.forEach { append(it).append("\n") }
                        }.toString().trimEnd()
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
