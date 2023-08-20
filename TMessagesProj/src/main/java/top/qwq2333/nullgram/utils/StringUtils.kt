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
package top.qwq2333.nullgram.utils

import org.telegram.tgnet.TLRPC
import ws.vinta.pangu.Pangu
import kotlin.math.ceil

object StringUtils {
    private val pangu = Pangu()

    /**
     *
     * 字符串是否为空白，空白的定义如下：
     *
     *  1. `null`
     *  1. 空字符串：`""`
     *  1. 空格、全角空格、制表符、换行符，等不可见字符
     *
     *
     *
     * 例：
     *
     *  * `StringUtils.isBlank(null)     // true`
     *  * `StringUtils.isBlank("")       // true`
     *  * `StringUtils.isBlank(" \t\n")  // true`
     *  * `StringUtils.isBlank("abc")    // false`
     *
     *
     * @param str 被检测的字符串
     * @return 若为空白，则返回 true
     */
    @JvmStatic
    fun isBlank(str: CharSequence?): Boolean {
        if (str.isNullOrEmpty()) {
            return true
        }
        val length: Int = str.length
        for (i in 0 until length) {
            // 只要有一个非空字符即为非空字符串
            if (!isBlankChar(str[i])) {
                return false
            }
        }
        return true
    }

    /**
     * 是否空白符<br></br>
     * 空白符包括空格、制表符、全角空格和不间断空格<br></br>
     *
     * @param c 字符
     * @return 是否空白符
     * @see Character.isWhitespace
     * @see Character.isSpaceChar
     */
    fun isBlankChar(c: Int): Boolean {
        return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\ufeff'.code || c == '\u202a'.code || c == '\u0000'.code
    }

    /**
     * 是否空白符<br></br>
     * 空白符包括空格、制表符、全角空格和不间断空格<br></br>
     *
     * @param c 字符
     * @return 是否空白符
     * @see Character.isWhitespace
     * @see Character.isSpaceChar
     */
    fun isBlankChar(c: Char): Boolean {
        return isBlankChar(c.code)
    }

    /**
     * Return a string with a maximum length of `length` characters.
     * If there are more than `length` characters, then string ends with an ellipsis ("...").
     *
     * @param text   text
     * @param length maximum length you want
     * @return Return a string with a maximum length of `length` characters.
     */
    @JvmStatic
    @Suppress("NAME_SHADOWING")
    fun ellipsis(text: String, length: Int): String {
        // The letters [iIl1] are slim enough to only count as half a character.
        var length = length
        length += ceil(text.replace("[^iIl]".toRegex(), "").length / 2.0).toInt()
        return if (text.length > length) {
            text.substring(0, length - 3) + "..."
        } else text
    }

    @JvmStatic
    fun spacingText(text: String, entities: ArrayList<TLRPC.MessageEntity>?): Pair<String, ArrayList<TLRPC.MessageEntity>?> {
        if (entities.isNullOrEmpty()) return Pair(pangu.spacingText(text), entities)
        if (text.startsWith("/")) return Pair(text, entities) // command

        val panguText = pangu.spacingText(text)
        val panguEntities = arrayListOf<TLRPC.MessageEntity>()

        if (panguText.length == text.length) return Pair(panguText, entities) // processed or unnecessary

        entities.forEach {

            val char = mutableListOf<Char>().also { list ->
                for (i in it.offset until (it.offset + it.length).coerceAtMost(text.length)) {
                    list.add(text[i])
                }
            }.also { list ->
                if (list.isEmpty()) return@forEach
            }

            var length = 0
            var start = it.offset
            var matched = false // matched first character
            for (i in it.offset until panguText.length) {
                if (start > i) continue
                if (panguText[i] == char[0]) { // match
                    char.removeAt(0)
                    if (!matched) {
                        start = i
                        matched = true
                    }
                }
                if (matched) length++
                if (char.isEmpty()) { // empty processing list
                    panguEntities.add(it.apply {
                        it.offset = start
                        it.length = length.coerceAtMost(start + panguText.lastIndex)
                    })
                    break
                }
            }
        }
        return Pair(panguText, panguEntities)
    }
}
