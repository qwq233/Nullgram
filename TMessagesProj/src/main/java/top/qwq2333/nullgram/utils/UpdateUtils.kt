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

import android.content.Context
import org.telegram.messenger.AccountInstance
import org.telegram.messenger.BuildConfig
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MessagesController
import org.telegram.messenger.MessagesStorage
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.messenger.browser.Browser
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.AlertDialog
import top.qwq2333.nullgram.config.ConfigManager

object UpdateUtils {

    private const val channelUsername = "NullgramClient"

    @JvmStatic
    fun postCheckFollowChannel(ctx: Context, currentAccount: Int) = UIUtil.runOnIoDispatcher {

        if (ConfigManager.getBooleanOrFalse(Defines.updateChannelSkip)) return@runOnIoDispatcher

        val messagesCollector = MessagesController.getInstance(currentAccount)
        val connectionsManager = ConnectionsManager.getInstance(currentAccount)
        val messagesStorage = MessagesStorage.getInstance(currentAccount)
        val updateChannel = messagesCollector.getUserOrChat(channelUsername)

        if (updateChannel is TLRPC.Chat) checkFollowChannel(ctx, currentAccount, updateChannel) else {
            connectionsManager.sendRequest(TLRPC.TL_contacts_resolveUsername().apply {
                username = channelUsername
            }) { response: TLObject?, error: TLRPC.TL_error? ->
                if (error == null) {
                    val res = response as TLRPC.TL_contacts_resolvedPeer
                    val chat = res.chats.find { it.username == channelUsername } ?: return@sendRequest
                    messagesCollector.putChats(res.chats, false)
                    messagesStorage.putUsersAndChats(res.users, res.chats, false, true)
                    checkFollowChannel(ctx, currentAccount, chat)
                }
            }
        }
    }

    private fun checkFollowChannel(ctx: Context, currentAccount: Int, channel: TLRPC.Chat) {

        if (!channel.left || channel.kicked) {
            //   MessagesController.getMainSettings(currentAccount).edit().putBoolean("update_channel_skip", true).apply()
            return
        }
        UIUtil.runOnUIThread {
            val messagesCollector = MessagesController.getInstance(currentAccount)
            val userConfig = UserConfig.getInstance(currentAccount)

            val builder = AlertDialog.Builder(ctx)

            builder.setTitle(LocaleController.getString("FCTitle", R.string.FCTitle))
            builder.setMessage(LocaleController.getString("FCInfo", R.string.FCInfo))

            builder.setPositiveButton(LocaleController.getString("ChannelJoin", R.string.ChannelJoin)) { _, _ ->
                messagesCollector.addUserToChat(channel.id, userConfig.currentUser, 0, null, null, null)
                Browser.openUrl(ctx, "https://t.me/$channelUsername")
            }

            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)

            builder.setNeutralButton(LocaleController.getString("DoNotRemindAgain", R.string.DoNotRemindAgain)) { _, _ ->
                ConfigManager.putBoolean(Defines.updateChannelSkip, true)
            }
            try {
                builder.show()
            } catch (ignored: Exception) {
            }
        }
    }

    private const val maxReadCount = 50
    private const val stableMetadataChannelID: Long = 1514826137
    private const val stableMetadataChannelName = "NullgramMetaData"
    private const val previewMetadataChannelID: Long = 1524514483
    private const val previewMetadataChannelName = "PreviewMetaData"
    private const val stableChannelAPKsID: Long = 1645976613
    private const val stableChannelAPKsName = "NullgramAPKs"
    private const val previewChannelAPKsID: Long = 1714986438
    private const val previewChannelAPKsName = "NullgramCI"

    @JvmStatic
    fun retrieveUpdateMetadata(callback: (UpdateMetadata?, Boolean) -> Unit) {
        val (metadataChannelID, metadataChannelName) = when (ConfigManager.getIntOrDefault(Defines.updateChannel, -1)) {
            Defines.stableChannel -> stableMetadataChannelID to stableMetadataChannelName
            Defines.ciChannel -> previewMetadataChannelID to previewMetadataChannelName
            else -> if (BuildConfig.VERSION_NAME.contains("preview")) previewMetadataChannelID to previewMetadataChannelName else stableMetadataChannelID to stableMetadataChannelName
        }
        val localVersionCode = BuildConfig.VERSION_CODE
        val accountInstance = AccountInstance.getInstance(UserConfig.selectedAccount)
        TLRPC.TL_messages_getHistory().apply {
            peer = accountInstance.messagesController.getInputPeer(-metadataChannelID)
            offset_id = 0
            limit = maxReadCount
        }.let { req ->
            val sendReq = Runnable {
                accountInstance.connectionsManager.sendRequest(req) { resp: TLObject, error: TLRPC.TL_error? ->
                    if (error != null) {
                        Log.e("checkUpdate", "Error when retrieving update metadata from channel ${error.text}")
                        callback.invoke(null, true)
                        return@sendRequest
                    }
                    val res = resp as TLRPC.messages_Messages
                    var targetMetadata: UpdateMetadata? = null
                    val metas = ArrayList<UpdateMetadata>().apply {
                        res.messages.forEach { msg ->
                            if (msg !is TLRPC.TL_message) {
                                Log.i("checkUpdate", "CheckUpdate: Not TL_message")
                                return@forEach
                            }
                            if (!msg.message.startsWith("v")) {
                                Log.i("checkUpdate", "CheckUpdate: Not startsWith v")
                                return@forEach
                            }
                            msg.message.split(",").dropLastWhile { split -> split.isEmpty() }.toTypedArray().let { split ->
                                if (split.size < 5) {
                                    Log.i("checkUpdate", "CheckUpdate: Not enough split")
                                    return@forEach
                                }
                                add(
                                    UpdateMetadata(
                                        messageID = msg.id,
                                        versionName = split[0],
                                        versionCode = Integer.parseInt(split[1]),
                                        apkChannelMessageID = Integer.parseInt(split[2]),
                                        updateLogMessageID = Integer.parseInt(split[3]),
                                        canNotSkip = split[4] == "true"
                                    )
                                )
                            }
                        }

                    }.sortedByDescending { it.versionCode }
                    for (i in metas.indices) {
                        Log.i("checkUpdate", "$i ${metas[i].versionName} ${metas[i].versionCode}")
                    }
                    for (meta in metas) {
                        if (meta.versionCode <= localVersionCode) {
                            Log.i("checkUpdate", "versionCode <= localVersionCode , ignore.")
                            break
                        }
                        targetMetadata = meta
                        break
                    }

                    if (targetMetadata != null) {
                        for (msg in res.messages) {
                            if (msg !is TLRPC.TL_message) {
                                Log.i("checkUpdate", "CheckUpdate: Not TL_message")
                                continue
                            }
                            if (msg.id == targetMetadata.updateLogMessageID) {
                                targetMetadata.updateLogEntities = msg.entities
                                targetMetadata.updateLog = msg.message
                                break
                            }
                        }
                        Log.i("checkUpdate", "Found Update Metadata: ${targetMetadata.versionName} ${targetMetadata.versionCode}")
                        callback.invoke(targetMetadata, true)
                    } else {
                        Log.i("checkUpdate", "No update metadata found.")
                        callback.invoke(null, false)
                    }
                }
            }

            if (req.peer.access_hash != 0L) {
                sendReq.run()
            } else {
                TLRPC.TL_contacts_resolveUsername().apply {
                    username = metadataChannelName
                }.let { req2 ->
                    accountInstance.connectionsManager.sendRequest(req2) { resp: TLObject, error: TLRPC.TL_error? ->
                        if (error != null) {
                            Log.e("checkUpdate", "Error when retrieving update metadata from channel ${error.text}")
                            callback.invoke(null, true)
                            return@sendRequest
                        }
                        val res = if (resp is TLRPC.TL_contacts_resolvedPeer) {
                            resp
                        } else {
                            Log.e("checkUpdate", "Error when checking update, unable to resolve metadata channel, unexpected responseType ${resp::class.java.name}")
                            callback.invoke(null, true)
                            return@sendRequest
                        }
                        accountInstance.messagesController.putUsers(res.users, false)
                        accountInstance.messagesController.putChats(res.chats, false)
                        accountInstance.messagesStorage.putUsersAndChats(res.users, res.chats, false, true)
                        if (res.chats == null || res.chats.isEmpty()) {
                            Log.e("checkUpdate", "Error when checking update, unable to resolve metadata channel, chats is null or empty")
                            callback.invoke(null, true)
                            return@sendRequest
                        }
                        req.peer = TLRPC.TL_inputPeerChannel().apply {
                            channel_id = res.chats[0].id
                            access_hash = res.chats[0].access_hash
                        }
                        sendReq.run()
                    }
                }
            }
        }
    }

    @JvmStatic
    fun checkUpdate(callback: (TLRPC.TL_help_appUpdate?, Boolean) -> Unit) {
        if (BuildConfig.isPlay) return
        val (apksChannelID, apksChannelName) = when (ConfigManager.getIntOrDefault(Defines.updateChannel, -1)) {
            Defines.stableChannel -> stableChannelAPKsID to stableChannelAPKsName
            Defines.ciChannel -> previewChannelAPKsID to previewChannelAPKsName
            else -> if (BuildConfig.VERSION_NAME.contains("preview")) previewChannelAPKsID to previewChannelAPKsName else stableChannelAPKsID to stableChannelAPKsName
        }
        val accountInstance = AccountInstance.getInstance(UserConfig.selectedAccount)
        retrieveUpdateMetadata { metadata, error ->
            if (metadata == null) {
                Log.d("checkUpdate", "No update metadata found, skip.")
                callback.invoke(null, error)
                return@retrieveUpdateMetadata
            }
            val req = TLRPC.TL_messages_getHistory().apply {
                peer = accountInstance.messagesController.getInputPeer(-apksChannelID)
                min_id = metadata.apkChannelMessageID
                limit = maxReadCount
            }

            val sendReq = Runnable {
                accountInstance.connectionsManager.sendRequest(req) { resp: TLObject, error: TLRPC.TL_error? ->
                    if (error != null) {
                        Log.e("checkUpdate", "Error when retrieving update from channel ${error.text}")
                        callback.invoke(null, true)
                        return@sendRequest
                    }
                    val res = resp as TLRPC.messages_Messages
                    for (msg in res.messages) {
                        if (msg.media == null) {
                            Log.i("checkUpdate", "res.messages.get(i).media == null")
                            continue
                        }
                        val apkDocument = msg.media.document
                        val fileName = if (apkDocument.attributes.size == 0) "" else apkDocument.attributes[0].file_name
                        Log.d("checkUpdate", "file_name： ${apkDocument.attributes[0].file_name}")
                        if (!(fileName.contains(BuildConfig.FLAVOR) && fileName.contains(metadata.versionName))) continue
                        val update = TLRPC.TL_help_appUpdate().apply {
                            version = metadata.versionName
                            document = apkDocument
                            can_not_skip = metadata.canNotSkip
                            flags = flags or 2
                        }

                        if (metadata.updateLog != null) {
                            update.text = metadata.updateLog
                            update.entities = metadata.updateLogEntities
                        }
                        callback.invoke(update, false)
                        return@sendRequest
                    }
                    callback.invoke(null, false)
                }
            }

            if (req.peer.access_hash != 0L) {
                sendReq.run()
            } else {
                TLRPC.TL_contacts_resolveUsername().apply {
                    username = apksChannelName
                }.let { req2 ->
                    accountInstance.connectionsManager.sendRequest(req2) { resp: TLObject, error: TLRPC.TL_error? ->
                        if (error != null) {
                            Log.e("checkUpdate", "Error when retrieving update from channel ${error.text}")
                            callback.invoke(null, true)
                            return@sendRequest
                        }
                        val res = if (resp is TLRPC.TL_contacts_resolvedPeer) {
                            resp
                        } else {
                            Log.e("checkUpdate", "Error when checking update, unable to resolve apk channel, unexpected responseType ${resp::class.java.name}")
                            callback.invoke(null, true)
                            return@sendRequest
                        }
                        accountInstance.messagesController.putUsers(res.users, false)
                        accountInstance.messagesController.putChats(res.chats, false)
                        accountInstance.messagesStorage.putUsersAndChats(res.users, res.chats, false, true)
                        if (res.chats == null || res.chats.isEmpty()) {
                            Log.e("checkUpdate", "Error when checking update, unable to resolve apk channel, chats is null or empty")
                            callback.invoke(null, true)
                            return@sendRequest
                        }
                        req.peer = TLRPC.TL_inputPeerChannel().apply {
                            channel_id = res.chats[0].id
                            access_hash = res.chats[0].access_hash
                        }
                        sendReq.run()
                    }
                }
            }
        }
    }

    data class UpdateMetadata(
        val messageID: Int,
        val versionName: String,
        val versionCode: Int,
        val apkChannelMessageID: Int,
        val updateLogMessageID: Int,
        val canNotSkip: Boolean,
        var updateLog: String? = null,
        var updateLogEntities: ArrayList<TLRPC.MessageEntity>? = null,
    )
}
