package top.qwq2333.nullgram.translate.providers

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
import top.qwq2333.nullgram.translate.BaseTranslator
import top.qwq2333.nullgram.utils.Log

object LingoTranslator : BaseTranslator() {

    private val targetLanguages = listOf("zh", "en", "es", "fr", "ja", "ru")
    override fun getTargetLanguages(): List<String> = targetLanguages

    @Serializable
    data class Request(
        val source:String,
        val trans_type:String,
        val request_id:String = System.currentTimeMillis().toString(),
        val detect: Boolean = true)

    @Serializable
    data class Response(
        val target: String?,
        val error: String?
    )

    override suspend fun translateText(text: String, from: String, to: String): RequestResult {
        client.post("https://api.interpreter.caiyunai.com/v1/translator") {
            contentType(ContentType.Application.Json)
            header("User-Agent", "okhttp/3.12.3")
            header("X-Authorization", "token 9sdftiq37bnv410eon2l")
            setBody(Json.encodeToString(Request(text, "auto2$to")))
        }.let {
            when(it.status) {
                HttpStatusCode.OK -> {
                    Log.w(it.bodyAsText())

                    Json.decodeFromString(Response.serializer(), it.bodyAsText()).let { response ->
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
