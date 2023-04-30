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

import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import top.qwq2333.nullgram.translate.BaseTranslator
import top.qwq2333.nullgram.utils.Log

object LingoTranslator : BaseTranslator() {

    private val targetLanguages = listOf("zh", "en", "ja", "ko", "es", "fr", "ru")
    override fun getTargetLanguages(): List<String> = targetLanguages

    @Serializable
    @OptIn(ExperimentalSerializationApi::class)
    data class Request constructor(
        val source: String,
        val trans_type: String,
        val request_id: String = System.currentTimeMillis().toString(),
        val detect: Boolean = true
    )

    @Serializable
    data class Response(
        val target: String?,
        val error: String? = null
    )

    override suspend fun translateText(text: String, from: String, to: String): RequestResult {
        client.post("https://api.interpreter.caiyunai.com/v1/translator") {
            contentType(ContentType.Application.Json)
            header("User-Agent", "okhttp/3.12.3")
            header("X-Authorization", "token 9sdftiq37bnv410eon2l")
            setBody(Request(text, "auto2$to"))
        }.let {
            when (it.status) {
                HttpStatusCode.OK -> {
                    (it.body() as Response).let { response ->
                        if (response.error != null) {
                            return RequestResult(from, null, HttpStatusCode(HttpStatusCode.BadRequest.value, response.error))
                        }
                        return RequestResult(from, response.target)
                    }
                }

                else -> {
                    Log.w(it.bodyAsText())
                    return RequestResult(from, null, it.status)
                }
            }
        }
    }

}
