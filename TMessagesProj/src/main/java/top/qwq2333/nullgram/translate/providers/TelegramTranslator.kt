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
