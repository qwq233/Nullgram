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

package top.qwq2333.nullgram.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.telegram.messenger.AccountInstance
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import top.qwq2333.nullgram.utils.Log

class CloudStorage(instance: Int) : AccountInstance(instance) {
    private var botUser: TLRPC.User? = null

    @Serializable
    data class KeyPair(val key: String, val value: String? = null)

    private enum class Method(val method: String) {
        get("getStorageValues"),
        set("saveStorageValue"),
        delete("deleteStorageValues"),
        getKeys("getStorageKeys")
    }

    private fun invokeStorageMethod(method: Method, params: KeyPair): KeyPair? {
        if (botUser == null) {
            Log.e(IllegalStateException("For some reason, unable to get bot user"))
            return null
        }
        val req = TLRPC.TL_bots_invokeWebViewCustomMethod().apply {
            bot = messagesController.getInputUser(botUser)
            custom_method = method.method
            this.params = TLRPC.TL_dataJSON().apply {
                data = if (method == Method.getKeys) {
                    "{}"
                } else {
                    Json.encodeToString(KeyPair.serializer(), params)
                }
            }
        }

        return CoroutineScope(Dispatchers.IO).future {
            connectionsHelper.sendReqAndDo(req) { response, _ ->
                if (response is TLRPC.TL_dataJSON) {
                    return@sendReqAndDo Json.decodeFromString(KeyPair.serializer(), response.data)
                } else null
            }
        }.get()
    }

    fun get(key: String): String? {
        return invokeStorageMethod(Method.get, KeyPair(key))?.value
    }

    fun set(key: String, value: String) {
        invokeStorageMethod(Method.set, KeyPair(key, value))
    }

    fun delete(key: String) {
        invokeStorageMethod(Method.delete, KeyPair(key))
    }

    companion object {
        private val Instance = arrayOfNulls<CloudStorage>(UserConfig.MAX_ACCOUNT_COUNT)

        @JvmStatic
        fun getInstance(num: Int): CloudStorage {
            var localInstance: CloudStorage?
            synchronized(CloudStorage::class.java) {
                localInstance = Instance[num]
                if (localInstance == null) {
                    localInstance = CloudStorage(num)
                    Instance[num] = localInstance

                    val req = TLRPC.TL_contacts_resolveUsername().apply {
                        username = ""
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        localInstance!!.connectionsHelper.sendReqAndDo(req) { response, _ ->
                            if (response != null) {
                                response as TLRPC.TL_contacts_resolvedPeer
                                localInstance!!.apply {
                                    messagesController.putUsers(response.users, false)
                                    messagesController.putChats(response.chats, false)
                                    messagesStorage.putUsersAndChats(response.users, response.chats, true, true)
                                    botUser = messagesController.getUser(response.peer.user_id)
                                }
                            }
                        }
                    }
                }
            }

            return localInstance!!
        }
    }

}
