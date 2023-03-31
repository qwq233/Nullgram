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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.userAgent
import org.json.JSONObject
import org.telegram.messenger.Utilities
import top.qwq2333.nullgram.translate.BaseTranslator
import top.qwq2333.nullgram.utils.encodeUrl
import java.util.Locale
import java.util.UUID

object BaiduTranslator : BaseTranslator()  {
    private fun randomString(): String {
        val symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()
        val buf = CharArray(9)
        for (idx in buf.indices) {
            buf[idx] = symbols[Utilities.random.nextInt(symbols.size)]
        }
        return String(buf)
    }
    private val targetLanguages: List<String> = mutableListOf(
        "zh", "en", "ja", "ko", "fr", "es", "th", "ar",
        "ru", "pt", "de", "it", "el", "nl", "pl", "bg",
        "et", "da", "fi", "cs", "ro", "sl", "sv", "hu",
        "zh-TW", "vi"
    )
    private val baiduLanguages: List<String> = mutableListOf(
        "zh", "en", "jp", "kor", "fra", "spa", "th", "ara",
        "ru", "pt", "de", "it", "el", "nl", "pl", "bul",
        "est", "dan", "fin", "cs", "rom", "slo", "swe", "hu",
        "cht", "vie"
    )
    private val cuid = UUID.randomUUID().toString().uppercase(Locale.getDefault()).replace("-", "") + "|" + randomString()

    override fun convertLanguageCode(code: String, reverse: Boolean): String {
        val index = if (reverse) baiduLanguages.indexOf(code) else targetLanguages.indexOf(code)
        if (index < 0) {
            return code
        }
        return if (reverse) targetLanguages[index] else baiduLanguages[index]
    }

    override fun convertLanguageCode(language: String, country: String?): String {
        val languageLowerCase = language.lowercase(Locale.getDefault())
        val code = if (!TextUtils.isEmpty(country)) {
            val countryUpperCase = country!!.uppercase(Locale.getDefault())
            if (targetLanguages.contains("$languageLowerCase-$countryUpperCase")) {
                "$languageLowerCase-$countryUpperCase"
            } else if (languageLowerCase == "zh") {
                if (countryUpperCase == "HK") {
                    "zh-TW"
                } else {
                    languageLowerCase
                }
            } else {
                languageLowerCase
            }
        } else {
            languageLowerCase
        }
        return code
    }

    override suspend fun translateText(text: String, from: String, to: String): RequestResult {
        val currentTime = System.currentTimeMillis()
        val sign = Utilities.MD5("query${text}imeiversion153timestamp${currentTime}fromautoto${to}reqv2transtextimage607e34f0fb3bf7895c102dacf9e9b0d7")
        client.post("https://fanyi-app.baidu.com/transapp/agent.php?product=transapp&type=json&version=153&plat=android&req=v2trans&cuid=$cuid") {
            contentType(ContentType.Application.FormUrlEncoded)
            userAgent("BDTApp; Android 12; BaiduTranslate/10.2.1")
            setBody("sign=${sign}&sofireId=&zhType=0&use_cache_response=1&from=auto&timestamp=${currentTime}&query=${text.encodeUrl()}&needfixl=1&lfixver=1&is_show_ad=1&appRecommendSwitch=1&to=${to}&page=translate")
        }.let {
            if (TextUtils.isEmpty(it.bodyAsText())) {
                return RequestResult(from,null,it.status)
            } else {
                val json: JSONObject = JSONObject(it.bodyAsText())
                val array = json.getJSONArray("fanyi_list")
                buildString {
                    for (i in 0 until array.length()) {
                        append(array.getString(i))
                        if (i != array.length() - 1) append("\n")
                    }
                    val sourceLang = json.getString("detect_lang")
                    return RequestResult(sourceLang, toString())
                }
            }
        }
    }

    override fun getTargetLanguages(): List<String> {
        return targetLanguages
    }
}
