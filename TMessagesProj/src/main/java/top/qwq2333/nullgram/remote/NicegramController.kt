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

package top.qwq2333.nullgram.remote

import androidx.collection.LruCache
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import top.qwq2333.nullgram.utils.Log

object NicegramController : BaseController() {
    override val baseUrl = "https://restore-access.indream.app"
    private val lruCache = LruCache<Long, RegDateResponse>(100)

    @Serializable
    private data class RegDateRequest(val telegramId: Long)

    @Serializable
    data class RegDateResponse(val data: Data) {
        @Serializable
        data class Data(
            val type: RegDateType,
            val date: String
        )


        @Serializable(with = RegDateResponseSerializer::class)
        enum class RegDateType(val key: String) {
            Approximately("TYPE_APPROX"),
            OlderThan("TYPE_OLDER"),
            NewerThan("TYPE_NEWER"),
            Exactly("TYPE_EXACTLY"),
        }

        @OptIn(ExperimentalSerializationApi::class)
        @Serializer(forClass = RegDateType::class)
        private object RegDateResponseSerializer : KSerializer<RegDateType> {
            override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("RegDateType", PrimitiveKind.STRING)
            override fun deserialize(decoder: Decoder): RegDateType = try {
                RegDateType.values().first { it.key == decoder.decodeString() }
            } catch (e: NoSuchElementException) {
                Log.e("Unknown RegDateType: ${decoder.decodeString()}")
                throw IllegalStateException("Unknown RegDateType: ${decoder.decodeString()}")
            } catch (e: Exception) {
                Log.e(e)
                throw e
            }

            override fun serialize(encoder: Encoder, value: RegDateType) {
                throw IllegalStateException("This object should not be serialized")
            }
        }
    }

    fun getRegDate(userId: Long, onError: (Throwable) -> (Unit), callback: (RegDateResponse.RegDateType, String) -> (Unit)) {
        lruCache[userId]?.let {
            callback(it.data.type, it.data.date)
        }
        val apiKey = "e758fb28-79be-4d1c-af6b-066633ded128" // it might be changed in the future
        CoroutineScope(Dispatchers.Main).launch {
            runCatching {
                val result = withContext(Dispatchers.IO) {
                    client.post("$baseUrl/regdate") {
                        contentType(ContentType.Application.Json)
                        header("User-Agent", "okhttp/4.5.0")
                        header("X-Api-Key", apiKey)
                        setBody(RegDateRequest(userId))
                    }.let {
                        Log.d(it.status.toString())
                        Log.d(it.bodyAsText())

                        val resp: RegDateResponse = it.body()
                        lruCache.put(userId, resp)
                        resp
                    }
                }
                callback(result.data.type, result.data.date)
            }.getOrElse {
                Log.e(it)
                onError(it)
            }
        }
    }
}
