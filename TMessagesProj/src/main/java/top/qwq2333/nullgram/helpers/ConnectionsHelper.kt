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

package top.qwq2333.nullgram.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.AccountInstance
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import top.qwq2333.nullgram.config.CloudStorage
import top.qwq2333.nullgram.utils.Log
import java.util.concurrent.CountDownLatch

class ConnectionsHelper(instance: Int) : AccountInstance(instance) {
    companion object {
        private val Instance = arrayOfNulls<ConnectionsHelper>(UserConfig.MAX_ACCOUNT_COUNT)

        @JvmStatic
        fun getInstance(num: Int): ConnectionsHelper {
            var localInstance: ConnectionsHelper?
            synchronized(CloudStorage::class.java) {
                localInstance = Instance[num]
                if (localInstance == null) {
                    localInstance = ConnectionsHelper(num)
                    Instance[num] = localInstance
                }
            }

            return localInstance!!
        }
    }

    suspend fun sendReqAndGet(req: TLObject): TLObject? {
        var result: TLObject? = null
        val latch = CountDownLatch(1)

        withContext(Dispatchers.IO) {
            connectionsManager.sendRequest(req) { response, _ ->
                if (response != null) {
                    result = response
                }
                latch.countDown()
            }
            latch.await()
        }
        return result
    }

    suspend fun <T> sendReqAndDo(req: TLObject, flags: Int = 0, action: (TLObject?, TLRPC.TL_error?) -> T?): T? {
        var result: T? = null
        val latch = CountDownLatch(1)
        withContext(Dispatchers.IO) {
            connectionsManager.sendRequest(req, { response: TLObject?, error: TLRPC.TL_error? ->
                result = action(response, error)
                Log.d("countdown")
                latch.countDown()
            }, flags)
            Log.d("await")
            latch.await()
        }

        return result
    }
}
