package top.qwq2333.nullgram.translate

import android.text.TextUtils
import android.util.Pair
import androidx.collection.LruCache
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.Charsets
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.tgnet.TLRPC
import top.qwq2333.nullgram.helpers.TranslateHelper

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
    val client = HttpClient(OkHttp) {
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
                val result = translateText(source, from, to)
                return if (result.error == null) {
                    val translateResult = TranslateResult(from, result.result)
                    cache.put(Pair(source, to), translateResult)
                    translateResult
                } else {
                    TranslateResult(from, null, result.error)
                }
            }

            is TLRPC.Poll -> {
                val translatedPoll: TLRPC.TL_poll = TLRPC.TL_poll()
                // Keep original information
                translatedPoll.close_date = source.close_date
                translatedPoll.close_period = source.close_period
                translatedPoll.closed = source.closed
                translatedPoll.flags = source.flags
                translatedPoll.id = source.id
                translatedPoll.multiple_choice = source.multiple_choice
                translatedPoll.public_voters = source.public_voters
                translatedPoll.quiz = source.quiz

                // Translate question
                val translatedQuestion = translateText(source.question, from, to)
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
                    val translatedAnswer = translateText(it.text, from, to)
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
