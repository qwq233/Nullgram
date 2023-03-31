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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.telegram.messenger.ApplicationLoader

object UIUtil {

    @JvmStatic
    fun runOnUIThread(runnable: Runnable) = ApplicationLoader.applicationHandler.post(runnable)

    fun runOnUIThread(runnable: () -> Unit) = ApplicationLoader.applicationHandler.post(runnable)

    @JvmStatic
    @JvmOverloads
    fun runOnIoDispatcher(runnable: Runnable, delay: Long = 0) {

        GlobalScope.launch(Dispatchers.IO) {

            delay(delay)

            runnable.run()

        }

    }

    fun runOnIoDispatcher(runnable: suspend () -> Unit) {

        GlobalScope.launch(Dispatchers.IO) {

            runnable()

        }

    }

}
