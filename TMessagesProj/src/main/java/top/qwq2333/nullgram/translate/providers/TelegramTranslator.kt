package top.qwq2333.nullgram.translate.providers

import io.ktor.http.HttpStatusCode
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import top.qwq2333.nullgram.translate.BaseTranslator
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

object TelegramTranslator : BaseTranslator() {
    override suspend fun translateText(text: String, from: String, to: String): RequestResult {
        val result = AtomicReference<RequestResult>()
        val latch = CountDownLatch(1)
        ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(TLRPC.TL_messages_translateText().apply {
            flags = flags or 2
            to_lang = to
            this.text = arrayListOf(TLRPC.TL_textWithEntities().apply {
                this.text = text
            })
        }) { res: TLObject?, error: TLRPC.TL_error? ->
            if (error == null) {
                if (res is TLRPC.TL_messages_translateResult && res.result.isNotEmpty()) {
                    val sb = StringBuilder().apply {
                        res.result.forEach() {
                            append(it.text)
                        }
                    }
                    result.set(RequestResult(from, sb.toString()))
                } else {
                    result.set(RequestResult(from, null, HttpStatusCode.TooManyRequests))
                }
            } else {
                result.set(RequestResult(from, null, HttpStatusCode(500, error.text)))
            }
            latch.countDown()
        }
        latch.await()
        return result.get()
    }

    override fun getTargetLanguages(): List<String> = GoogleTranslator.getTargetLanguages()
    override fun convertLanguageCode(language: String, country: String?): String = GoogleTranslator.convertLanguageCode(language, country)


}
