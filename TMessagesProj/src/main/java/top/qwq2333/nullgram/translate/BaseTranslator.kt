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

package top.qwq2333.nullgram.translate

import android.text.TextUtils
import android.util.Pair
import androidx.collection.LruCache
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.Charsets
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import kotlinx.serialization.json.Json
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.tgnet.TLRPC
import top.qwq2333.nullgram.helpers.TranslateHelper
import top.qwq2333.nullgram.utils.Log

abstract class BaseTranslator {
    /**
     * cache pool
     */
    private val cache: LruCache<Pair<Any, String>, TranslateResult> = LruCache<Pair<Any, String>, TranslateResult>(200)

    data class RequestResult(
        val from: String, val result: String?, val error: HttpStatusCode? = null
    )

    data class TranslateResult(
        val from: String, val result: Any?, val error: HttpStatusCode? = null
    )

    /**
     * default http client
     *
     * Charset: `UTF-8`
     */
    protected val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        install(HttpCookies)
        // Ensure using UTF-8
        Charsets {
            register(Charsets.UTF_8)
            sendCharset = Charsets.UTF_8
            responseCharsetFallback = Charset.forName("GBK")
        }
    }

    /**
     * translate text
     *
     * if failed, return null
     *
     * @param text origin text
     * @param from source language
     * @param to target language
     * @return translated result
     */
    abstract suspend fun translateText(text: String, from: String, to: String): RequestResult

    /**
     * get available languages
     */
    abstract fun getTargetLanguages(): List<String>

    open fun convertLanguageCode(language: String, country: String? = null): String {
        return language
    }

    open fun convertLanguageCode(code: String, reverse: Boolean): String {
        return code
    }

    /**
     * @return is current provider supported this language
     */
    open fun supportLanguage(language: String): Boolean {
        return getTargetLanguages().contains(language)
    }

    open fun getCurrentTargetLanguage(): String {
        return getTargetLanguage(TranslateHelper.currentTargetLanguage)
    }

    fun getCurrentAppLanguage(): String {
        var toLang: String?
        val locale = LocaleController.getInstance().currentLocale
        toLang = convertLanguageCode(locale.language, locale.country)
        if (!supportLanguage(toLang)) {
            toLang = convertLanguageCode(LocaleController.getString("LanguageCode", R.string.LanguageCode), null)
        }
        return toLang
    }

    open fun getTargetLanguage(language: String): String = if (language == "app") {
        getCurrentAppLanguage()
    } else {
        language
    }

    private suspend fun doTranslateText(text: String, from: String, to: String): RequestResult {
        return runCatching {
            translateText(text, from, to)
        }.getOrElse {
            Log.w("Translate Error Occur: ", it)
            RequestResult(from, null, HttpStatusCode(500, it.message ?: ""))
        }
    }

    /**
     * translate
     *
     * @param source one of them: [String] or [TLRPC.TL_poll]
     * @param from source language
     * @param to target language
     *
     * @return [TranslateResult]
     */
    open suspend fun translate(source: Any, from: String, to: String): TranslateResult {
        val cachedResult: TranslateResult? = cache.get(Pair(source, to))
        if (cachedResult != null) {
            return cachedResult
        }

        val from = convertLanguageCode(if (TextUtils.isEmpty(from) || "und" == from) "auto" else from, false)
        val to = convertLanguageCode(to, false)
        when (source) {
            is String -> {
                val result = doTranslateText(source, from, to)
                return if (result.error == null) {
                    val translateResult = TranslateResult(from, result.result)
                    cache.put(Pair(source, to), translateResult)
                    translateResult
                } else {
                    TranslateResult(from, null, result.error)
                }
            }

            is TLRPC.Poll -> {
                val translatedPoll: TLRPC.TL_poll = TLRPC.TL_poll().apply {
                    // Keep original information
                    close_date = source.close_date
                    close_period = source.close_period
                    closed = source.closed
                    flags = source.flags
                    id = source.id
                    multiple_choice = source.multiple_choice
                    public_voters = source.public_voters
                    quiz = source.quiz
                }

                // Translate question
                val translatedQuestion = doTranslateText(source.question, from, to)
                translatedPoll.question = if (translatedQuestion.error == null) {
                    if (TranslateHelper.showOriginal) {
                        """
                        |${source.question}
                        |
                        |--------
                        |
                        |${translatedQuestion.result!!}
                        """.trimMargin()
                    } else {
                        translatedQuestion.result!!
                    }
                } else {
                    return TranslateResult(from, null, translatedQuestion.error)
                }
                // Translate options
                source.answers.forEach {
                    val translatedAnswer = doTranslateText(it.text, from, to)
                    val translatedPollAnswer = TLRPC.TL_pollAnswer()
                    translatedPollAnswer.text = if (translatedAnswer.error == null) {
                        if (TranslateHelper.showOriginal) {
                            "${it.text} | ${translatedAnswer.result!!}"
                        } else {
                            translatedAnswer.result
                        }
                    } else {
                        return TranslateResult(from, null, translatedAnswer.error)
                    }
                    translatedPollAnswer.option = it.option
                    translatedPoll.answers.add(translatedPollAnswer)
                }
                val result = TranslateResult(from, translatedPoll)
                cache.put(Pair(source, to), result)
                return result
            }

            else -> {
                throw UnsupportedOperationException("Unsupported translation query")
            }
        }
    }
}
