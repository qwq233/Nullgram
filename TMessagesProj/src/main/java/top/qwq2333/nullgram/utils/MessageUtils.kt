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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import android.util.Base64
import android.util.Pair
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.FileProvider
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import org.telegram.SQLite.SQLiteCursor
import org.telegram.SQLite.SQLiteException
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.BaseController
import org.telegram.messenger.BuildConfig
import org.telegram.messenger.ChatObject
import org.telegram.messenger.Emoji
import org.telegram.messenger.FileLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.MediaController
import org.telegram.messenger.MediaDataController
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.NotificationCenter
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.messenger.Utilities
import org.telegram.tgnet.ConnectionsManager
import org.telegram.tgnet.RequestDelegate
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.ChatMessageCell
import org.telegram.ui.Cells.CheckBoxCell
import org.telegram.ui.ChatActivity
import org.telegram.ui.Components.AlertsCreator
import org.telegram.ui.Components.AvatarDrawable
import org.telegram.ui.Components.BackupImageView
import org.telegram.ui.Components.Bulletin
import org.telegram.ui.Components.Bulletin.ButtonLayout
import org.telegram.ui.Components.Bulletin.LottieLayout
import org.telegram.ui.Components.Bulletin.TwoLineLottieLayout
import org.telegram.ui.Components.Bulletin.UndoButton
import org.telegram.ui.Components.EditTextBoldCursor
import org.telegram.ui.Components.Forum.ForumUtilities
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.TranscribeButton
import top.qwq2333.nullgram.helpers.QrHelper
import top.qwq2333.nullgram.helpers.QrHelper.readQr
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

class MessageUtils(num: Int) : BaseController(num) {

    private fun getTargetMessageObjectFromGroup(selectedObjectGroup: MessageObject.GroupedMessages): MessageObject? {
        var messageObject: MessageObject? = null
        for (obj in selectedObjectGroup.messages) {
            if (!TextUtils.isEmpty(obj.messageOwner.message)) {
                if (messageObject != null) {
                    messageObject = null
                    break
                } else {
                    messageObject = obj
                }
            }
        }
        return messageObject
    }

    fun getMessageForRepeat(selectedObject: MessageObject, selectedObjectGroup: MessageObject.GroupedMessages?): MessageObject? {
        var messageObject: MessageObject? = null
        if (selectedObjectGroup != null && !selectedObjectGroup.isDocuments) {
            messageObject = getTargetMessageObjectFromGroup(selectedObjectGroup)
        } else if (!TextUtils.isEmpty(selectedObject.messageOwner.message) || selectedObject.isAnyKindOfSticker) {
            messageObject = selectedObject
        }
        return messageObject
    }

    @JvmOverloads
    fun createDeleteHistoryAlert(fragment: BaseFragment?, chat: TLRPC.Chat?, forumTopic: TLRPC.TL_forumTopic?, mergeDialogId: Long, resourcesProvider: Theme.ResourcesProvider? = null) {
        if (fragment?.getParentActivity() == null || chat == null) {
            return
        }
        val context: Context = fragment.getParentActivity()
        val builder = AlertDialog.Builder(context, resourcesProvider)
        val cell = if (forumTopic == null && ChatObject.isChannel(chat) && ChatObject.canUserDoAction(chat, ChatObject.ACTION_DELETE_MESSAGES)) CheckBoxCell(
            context, 1, resourcesProvider
        ) else null
        val messageTextView = TextView(context)
        messageTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack))
        messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        messageTextView.setGravity((if (LocaleController.isRTL) Gravity.RIGHT else Gravity.LEFT) or Gravity.TOP)
        val frameLayout: FrameLayout = object : FrameLayout(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                if (cell != null) {
                    setMeasuredDimension(measuredWidth, measuredHeight + cell.measuredHeight + AndroidUtilities.dp(7f))
                }
            }
        }
        builder.setView(frameLayout)
        val avatarDrawable = AvatarDrawable()
        avatarDrawable.setTextSize(AndroidUtilities.dp(12f))
        avatarDrawable.setInfo(chat)
        val imageView = BackupImageView(context)
        imageView.setRoundRadius(AndroidUtilities.dp(20f))
        if (forumTopic != null) {
            ForumUtilities.setTopicIcon(imageView, forumTopic, false, true, null)
        } else {
            imageView.setForUserOrChat(chat, avatarDrawable)
        }
        frameLayout.addView(imageView, LayoutHelper.createFrame(40, 40f, (if (LocaleController.isRTL) Gravity.RIGHT else Gravity.LEFT) or Gravity.TOP, 22f, 5f, 22f, 0f))
        val textView = TextView(context)
        textView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem))
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"))
        textView.setLines(1)
        textView.setMaxLines(1)
        textView.setSingleLine(true)
        textView.setGravity((if (LocaleController.isRTL) Gravity.RIGHT else Gravity.LEFT) or Gravity.CENTER_VERTICAL)
        textView.ellipsize = TextUtils.TruncateAt.END
        textView.text = LocaleController.getString("DeleteAllFromSelf", R.string.DeleteAllFromSelf)
        frameLayout.addView(
            textView, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT.toFloat(),
                (if (LocaleController.isRTL) Gravity.RIGHT else Gravity.LEFT) or Gravity.TOP,
                (if (LocaleController.isRTL) 21 else 76).toFloat(),
                11f,
                (if (LocaleController.isRTL) 76 else 21).toFloat(),
                0f
            )
        )
        frameLayout.addView(
            messageTextView, LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT.toFloat(), (if (LocaleController.isRTL) Gravity.RIGHT else Gravity.LEFT) or Gravity.TOP, 24f, 57f, 24f, 9f
            )
        )
        if (cell != null) {
            val sendAs = ChatObject.getSendAsPeerId(chat, messagesController.getChatFull(chat.id), true) != userConfig.getClientUserId()
            cell.background = Theme.getSelectorDrawable(false)
            cell.setText(LocaleController.getString("DeleteAllFromSelfAdmin", R.string.DeleteAllFromSelfAdmin), "", !ChatObject.shouldSendAnonymously(chat) && !sendAs, false)
            cell.setPadding(
                if (LocaleController.isRTL) AndroidUtilities.dp(16f) else AndroidUtilities.dp(8f),
                0,
                if (LocaleController.isRTL) AndroidUtilities.dp(8f) else AndroidUtilities.dp(16f),
                0
            )
            frameLayout.addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48f, Gravity.BOTTOM or Gravity.LEFT, 0f, 0f, 0f, 0f))
            cell.setOnClickListener(View.OnClickListener { v: View ->
                val cell1 = v as CheckBoxCell
                cell1.setChecked(!cell1.isChecked, true)
            })
        }
        messageTextView.text = AndroidUtilities.replaceTags(LocaleController.getString("DeleteAllFromSelfAlert", R.string.DeleteAllFromSelfAlert))
        builder.setPositiveButton(LocaleController.getString("DeleteAll", R.string.DeleteAll)) { _: DialogInterface?, i: Int ->
            if (cell != null && cell.isChecked) {
                showDeleteHistoryBulletin(fragment, 0, false, { messagesController.deleteUserChannelHistory(chat, userConfig.currentUser, null, 0) }, resourcesProvider)
            } else {
                deleteUserHistoryWithSearch(
                    fragment, -chat.id, forumTopic?.id ?: 0, mergeDialogId
                ) { count: Int, deleteAction: Runnable? -> showDeleteHistoryBulletin(fragment, count, true, deleteAction, resourcesProvider) }
            }
        }
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
        val alertDialog = builder.create()
        fragment.showDialog(alertDialog)
        val button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE) as TextView
        button.setTextColor(Theme.getColor(Theme.key_text_RedRegular))
    }

    fun deleteUserHistoryWithSearch(fragment: BaseFragment?, dialogId: Long, replyMessageId: Int, mergeDialogId: Long, callback: ((Int, Runnable?) -> Unit)?) {
        Utilities.globalQueue.postRunnable {
            val messageIds = ArrayList<Int>()
            val latch = CountDownLatch(1)
            val peer = messagesController.getInputPeer(dialogId)
            val fromId = MessagesController.getInputPeer(userConfig.currentUser)
            doSearchMessages(fragment, latch, messageIds, peer, replyMessageId, fromId, Int.MAX_VALUE, 0)
            try {
                latch.await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (messageIds.isNotEmpty()) {
                val lists = ArrayList<ArrayList<Int>>()
                val N = messageIds.size
                var i = 0
                while (i < N) {
                    lists.add(ArrayList(messageIds.subList(i, min(N.toDouble(), (i + 100).toDouble()).toInt())))
                    i += 100
                }
                val deleteAction = Runnable {
                    for (list in lists) {
                        messagesController.deleteMessages(list, null, null, dialogId, true, false)
                    }
                }
                AndroidUtilities.runOnUIThread {
                    if (callback != null) {
                        callback.invoke(messageIds.size, deleteAction)
                    } else {
                        deleteAction.run()
                    }
                }
            }
            if (mergeDialogId != 0L) {
                deleteUserHistoryWithSearch(fragment, mergeDialogId, 0, 0, null)
            }
        }
    }

    fun doSearchMessages(
        fragment: BaseFragment?, latch: CountDownLatch, messageIds: ArrayList<Int>, peer: TLRPC.InputPeer?, replyMessageId: Int, fromId: TLRPC.InputPeer?, offsetId: Int, hash: Long
    ) {
        val req = TLRPC.TL_messages_search()
        req.peer = peer
        req.limit = 100
        req.q = ""
        req.offset_id = offsetId
        req.from_id = fromId
        req.flags = req.flags or 1
        req.filter = TLRPC.TL_inputMessagesFilterEmpty()
        if (replyMessageId != 0) {
            req.top_msg_id = replyMessageId
            req.flags = req.flags or 2
        }
        req.hash = hash
        connectionsManager.sendRequest(req, { response: TLObject?, error: TLRPC.TL_error? ->
            if (response is TLRPC.messages_Messages) {
                val res = response
                if (response is TLRPC.TL_messages_messagesNotModified || res.messages.isEmpty()) {
                    latch.countDown()
                    return@sendRequest
                }
                var newOffsetId = offsetId
                for (message in res.messages) {
                    newOffsetId = min(newOffsetId.toDouble(), message.id.toDouble()).toInt()
                    if (!message.out || message.post) {
                        continue
                    }
                    messageIds.add(message.id)
                }
                doSearchMessages(fragment, latch, messageIds, peer, replyMessageId, fromId, newOffsetId, calcMessagesHash(res.messages))
            } else {
                if (error != null) {
                    AndroidUtilities.runOnUIThread { AlertsCreator.showSimpleAlert(fragment, """
     ${LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred)}
     ${error.text}
     """.trimIndent()
                        )
                    }
                }
                latch.countDown()
            }
        }, ConnectionsManager.RequestFlagFailOnServerErrors)
    }

    private fun calcMessagesHash(messages: ArrayList<TLRPC.Message>?): Long {
        if (messages.isNullOrEmpty()) {
            return 0
        }
        var acc: Long = 0
        for (message in messages) {
            acc = MediaDataController.calcHash(acc, message.id.toLong())
        }
        return acc
    }

    fun saveStickerToGallery(activity: Activity, messageObject: MessageObject, callback: Utilities.Callback<Uri>) {
        saveStickerToGallery(activity, getPathToMessage(messageObject), messageObject.isVideoSticker, callback)
    }

    fun addMessageToClipboard(selectedObject: MessageObject, callback: Runnable) {
        val path = getPathToMessage(selectedObject)
        if (!TextUtils.isEmpty(path)) {
            addFileToClipboard(File(path), callback)
        }
    }

    fun getPathToMessage(messageObject: MessageObject): String? {
        var path = messageObject.messageOwner.attachPath
        if (!TextUtils.isEmpty(path)) {
            val temp = File(path)
            if (!temp.exists()) {
                path = null
            }
        }
        if (TextUtils.isEmpty(path)) {
            path = fileLoader.getPathToMessage(messageObject.messageOwner).toString()
            val temp = File(path)
            if (!temp.exists()) {
                path = null
            }
        }
        if (TextUtils.isEmpty(path)) {
            path = fileLoader.getPathToAttach(messageObject.getDocument(), true).toString()
            val temp = File(path)
            if (!temp.exists()) {
                return null
            }
        }
        return path
    }

    class DatacenterInfo(@JvmField var id: Int) {
        @JvmField
        var pingId: Long = 0

        @JvmField
        var ping: Long = 0

        @JvmField
        var checking = false

        @JvmField
        var available = false

        @JvmField
        var availableCheckTime: Long = 0
    }

    @SuppressLint("SetTextI18n")
    fun showSendCallbackDialog(fragment: ChatActivity, resourcesProvider: Theme.ResourcesProvider?, originalData: ByteArray?, messageObject: MessageObject?) {
        val context: Context = fragment.getParentActivity()
        val builder = AlertDialog.Builder(context, resourcesProvider)
        builder.setTitle(LocaleController.getString("SendCallback", R.string.SendCallback))
        val editText: EditTextBoldCursor = object : EditTextBoldCursor(context) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64f), MeasureSpec.EXACTLY))
            }
        }
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
        try {
            editText.setText(utf8Decoder.decode(ByteBuffer.wrap(originalData)).toString())
        } catch (ignore: CharacterCodingException) {
        }
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider))
        editText.setHintText(LocaleController.getString("CallbackData", R.string.CallbackData))
        editText.setHeaderHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider))
        editText.setSingleLine(true)
        editText.isFocusable = true
        editText.setTransformHintToHeader(true)
        editText.setLineColors(
            Theme.getColor(Theme.key_windowBackgroundWhiteInputField, resourcesProvider),
            Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated, resourcesProvider),
            Theme.getColor(
                Theme.key_text_RedRegular, resourcesProvider
            )
        )
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE)
        editText.setBackgroundDrawable(null)
        editText.requestFocus()
        editText.setPadding(0, 0, 0, 0)
        builder.setView(editText)
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK)) { dialogInterface: DialogInterface?, i: Int ->
            val button = TLRPC.TL_keyboardButtonCallback()
            button.data = editText.getText().toString().toByteArray(StandardCharsets.UTF_8)
            sendMessagesHelper.sendCallback(true, messageObject, button, fragment)
        }
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
        builder.show().setOnShowListener { dialog: DialogInterface? ->
            editText.requestFocus()
            AndroidUtilities.showKeyboard(editText)
        }
        val layoutParams = editText.layoutParams as MarginLayoutParams
        if (layoutParams is FrameLayout.LayoutParams) {
            layoutParams.gravity = Gravity.CENTER_HORIZONTAL
        }
        layoutParams.leftMargin = AndroidUtilities.dp(24f)
        layoutParams.rightMargin = layoutParams.leftMargin
        layoutParams.height = AndroidUtilities.dp(36f)
        editText.setLayoutParams(layoutParams)
        editText.setSelection(0, editText.getText().length)
    }

    fun getTextOrBase64(data: ByteArray): String {
        return try {
            utf8Decoder.decode(ByteBuffer.wrap(data)).toString()
        } catch (e: CharacterCodingException) {
            Base64.encodeToString(data, Base64.NO_PADDING or Base64.NO_WRAP)
        }
    }

    fun createQR(key: String?): Bitmap? {
        tryOrLog { val hints = HashMap<EncodeHintType, Any?>()
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
            hints[EncodeHintType.MARGIN] = 0
            val writer = QRCodeWriter()
            return writer.encode(key, 768, 768, hints, null)
        }
        return null
    }

    fun isMessageObjectAutoTranslatable(messageObject: MessageObject): Boolean {
        if (messageObject.translated || messageObject.translating || messageObject.isOutOwner()) {
            return false
        }
        return if (messageObject.isPoll) {
            true
        } else !TextUtils.isEmpty(messageObject.messageOwner.message) && !isLinkOrEmojiOnlyMessage(messageObject)
    }

    fun isLinkOrEmojiOnlyMessage(messageObject: MessageObject): Boolean {
        val entities = messageObject.messageOwner.entities
        if (entities != null) {
            for (entity in entities) {
                if (entity is TLRPC.TL_messageEntityBotCommand || entity is TLRPC.TL_messageEntityEmail || entity is TLRPC.TL_messageEntityUrl || entity is TLRPC.TL_messageEntityMention || entity is TLRPC.TL_messageEntityCashtag || entity is TLRPC.TL_messageEntityHashtag || entity is TLRPC.TL_messageEntityBankCard || entity is TLRPC.TL_messageEntityPhone) {
                    if (entity.offset == 0 && entity.length == messageObject.messageOwner.message.length) {
                        return true
                    }
                }
            }
        }
        return Emoji.fullyConsistsOfEmojis(messageObject.messageOwner.message)
    }

    fun getMessagePlainText(messageObject: MessageObject): String {
        val message: String = if (messageObject.isPoll) {
            val poll = (messageObject.messageOwner.media as TLRPC.TL_messageMediaPoll).poll
            val pollText = StringBuilder(poll.question).append("\n")
            for (answer in poll.answers) {
                pollText.append("\n\uD83D\uDD18 ")
                pollText.append(answer.text)
            }
            pollText.toString()
        } else if (messageObject.isVoiceTranscriptionOpen) {
            messageObject.messageOwner.voiceTranscription
        } else {
            messageObject.messageOwner.message
        }
        return message
    }

    fun getMessageForTranslate(selectedObject: MessageObject, selectedObjectGroup: MessageObject.GroupedMessages?): MessageObject? {
        val messageObject: MessageObject? = if (selectedObjectGroup != null && !selectedObjectGroup.isDocuments) {
            getTargetMessageObjectFromGroup(selectedObjectGroup)
        } else if (selectedObject.isPoll) {
            selectedObject
        } else if (selectedObject.isVoiceTranscriptionOpen && !TextUtils.isEmpty(selectedObject.messageOwner.voiceTranscription) && !TranscribeButton.isTranscribing(selectedObject)) {
            selectedObject
        } else if (!selectedObject.isVoiceTranscriptionOpen && !TextUtils.isEmpty(selectedObject.messageOwner.message) && !isLinkOrEmojiOnlyMessage(selectedObject)) {
            selectedObject
        } else null
        return if (messageObject != null && messageObject.translating) {
            null
        } else messageObject
    }

    fun resetMessageContent(dialogId: Long, messageObject: MessageObject, translated: Boolean) {
        resetMessageContent(dialogId, messageObject, translated, null, false, null)
    }

    fun resetMessageContent(dialogId: Long, messageObject: MessageObject, translated: Boolean, translating: Boolean) {
        resetMessageContent(dialogId, messageObject, translated, null, translating, null)
    }

    fun resetMessageContent(dialogId: Long, messageObject: MessageObject, translated: Boolean, original: Any?, translating: Boolean, translatedLanguage: Pair<String?, String?>?) {
        val message = messageObject.messageOwner
        val obj = MessageObject(currentAccount, message, true, true)
        obj.originalMessage = original
        obj.translating = translating
        obj.translatedLanguage = translatedLanguage
        obj.translated = translated
        if (messageObject.isSponsored) {
            obj.sponsoredId = messageObject.sponsoredId
            obj.botStartParam = messageObject.botStartParam
        }
        replaceMessagesObject(dialogId, obj)
    }

    private fun replaceMessagesObject(dialogId: Long, messageObject: MessageObject) {
        val arrayList = ArrayList<MessageObject>()
        arrayList.add(messageObject)
        notificationCenter.postNotificationName(NotificationCenter.replaceMessagesObjects, dialogId, arrayList, false)
    }

    private fun resolveUser(userName: String, userId: Long, callback: (TLRPC.User?) -> Unit) {
        val req = TLRPC.TL_contacts_resolveUsername()
        req.username = userName
        connectionsManager.sendRequest(req) { response: TLObject?, error: TLRPC.TL_error? ->
            AndroidUtilities.runOnUIThread {
                if (response != null) {
                    val res = response as TLRPC.TL_contacts_resolvedPeer
                    messagesController.putUsers(res.users, false)
                    messagesController.putChats(res.chats, false)
                    messagesStorage.putUsersAndChats(res.users, res.chats, true, true)
                    callback.invoke(if (res.peer.user_id == userId) messagesController.getUser(userId) else null)
                } else {
                    callback.invoke(null)
                }
            }
        }
    }

    @JvmOverloads
    fun searchUser(userId: Long, searchUser: Boolean = true, cache: Boolean = true, callback: (TLRPC.User?) -> Unit) {
        val bot = messagesController.getUser(189165596L)
        if (bot == null) {
            if (searchUser) {
                resolveUser("usinfobot", 189165596L) { user: TLRPC.User? -> searchUser(userId, false, false, callback) }
            } else {
                callback.invoke(null)
            }
            return
        }
        val key = "user_search_$userId"
        val requestDelegate = RequestDelegate { response: TLObject?, error: TLRPC.TL_error? ->
            AndroidUtilities.runOnUIThread {
                if (cache && (response !is TLRPC.messages_BotResults || response.results.isEmpty())) {
                    searchUser(userId, searchUser, false, callback)
                    return@runOnUIThread
                }
                if (response is TLRPC.messages_BotResults) {
                    val res = response
                    if (!cache && res.cache_time != 0) {
                        messagesStorage.saveBotCache(key, res)
                    }
                    if (res.results.isEmpty()) {
                        callback.invoke(null)
                        return@runOnUIThread
                    }
                    val result = res.results[0]
                    if (result.send_message == null || TextUtils.isEmpty(result.send_message.message)) {
                        callback.invoke(null)
                        return@runOnUIThread
                    }
                    val lines = result.send_message.message.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    if (lines.size < 3) {
                        callback.invoke(null)
                        return@runOnUIThread
                    }
                    val fakeUser = TLRPC.TL_user()
                    for (line in lines) {
                        val line = line.replace("\\p{C}".toRegex(), "").trim { it <= ' ' }
                        if (line.startsWith("\uD83D\uDC64")) {
                            fakeUser.id = Utilities.parseLong(line.replace("\uD83D\uDC64", ""))
                        } else if (line.startsWith("\uD83D\uDC66\uD83C\uDFFB")) {
                            fakeUser.first_name = line.replace("\uD83D\uDC66\uD83C\uDFFB", "").trim { it <= ' ' }
                        } else if (line.startsWith("\uD83D\uDC6A")) {
                            fakeUser.last_name = line.replace("\uD83D\uDC6A", "").trim { it <= ' ' }
                        } else if (line.startsWith("\uD83C\uDF10")) {
                            fakeUser.username = line.replace("\uD83C\uDF10", "").replace("@", "").trim { it <= ' ' }
                        }
                    }
                    if (fakeUser.id == 0L) {
                        callback.invoke(null)
                        return@runOnUIThread
                    }
                    if (fakeUser.username != null) {
                        resolveUser(fakeUser.username, fakeUser.id) { user: TLRPC.User? ->
                            if (user != null) {
                                callback.invoke(user)
                            } else {
                                fakeUser.username = null
                                callback.invoke(fakeUser)
                            }
                        }
                    } else {
                        callback.invoke(fakeUser)
                    }
                } else {
                    callback.invoke(null)
                }
            }
        }
        if (cache) {
            messagesStorage.getBotCache(key, requestDelegate)
        } else {
            val req = TLRPC.TL_messages_getInlineBotResults()
            req.query = userId.toString()
            req.bot = messagesController.getInputUser(bot)
            req.offset = ""
            req.peer = TLRPC.TL_inputPeerEmpty()
            connectionsManager.sendRequest(req, requestDelegate, ConnectionsManager.RequestFlagFailOnServerErrors)
        }
    }

    fun getLastMessageFromUnblockUser(dialogId: Long): MessageObject? {
        val cursor: SQLiteCursor
        var resp: MessageObject? = null
        try {
            cursor = messagesStorage.database.queryFinalized(
                    String.format(
                        Locale.US,
                        "SELECT data,send_state,mid,date FROM messages WHERE uid = %d ORDER BY date DESC LIMIT %d,%d",
                        dialogId,
                        0,
                        10
                    )
                )
            while (cursor.next()) {
                val data = cursor.byteBufferValue(0) ?: continue
                val message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false)
                data.reuse()
                if (messagesController.blockePeers.indexOfKey(message.from_id.user_id) < 0) {
                    resp = MessageObject(currentAccount, message, true, true)
                    message.send_state = cursor.intValue(1)
                    message.id = cursor.intValue(2)
                    message.date = cursor.intValue(3)
                    message.dialog_id = dialogId
                    if (messagesController.getUser(resp.getSenderId()) == null) {
                        val user = messagesStorage.getUser(resp.getSenderId())
                        if (user != null) {
                            messagesController.putUser(user, true)
                        }
                    }
                    break
                }
            }
            cursor.dispose()
        } catch (sqLiteException: SQLiteException) {
            Log.e("SQLiteException when read last message from unblocked user", sqLiteException)
            return null
        }
        return resp
    }

    companion object {
        @JvmField
        val datacenterInfos = ArrayList<DatacenterInfo>(5)

        init {
            for (a in 1..5) {
                datacenterInfos.add(DatacenterInfo(a))
            }
        }

        private val Instance = arrayOfNulls<MessageUtils>(UserConfig.MAX_ACCOUNT_COUNT)

        @JvmStatic
        fun getInstance(num: Int): MessageUtils? {
            var localInstance = Instance[num]
            if (localInstance == null) {
                synchronized(MessageUtils::class.java) {
                    localInstance = Instance[num]
                    if (localInstance == null) {
                        localInstance = MessageUtils(num)
                        Instance[num] = localInstance
                    }
                }
            }
            return localInstance
        }

        fun showDeleteHistoryBulletin(fragment: BaseFragment, count: Int, search: Boolean, delayedAction: Runnable?, resourcesProvider: Theme.ResourcesProvider?) {
            if (fragment.getParentActivity() == null) {
                delayedAction?.run()
                return
            }
            val buttonLayout: ButtonLayout
            if (search) {
                val layout = TwoLineLottieLayout(fragment.getParentActivity(), resourcesProvider)
                layout.titleTextView.text = LocaleController.getString("DeleteAllFromSelfDone", R.string.DeleteAllFromSelfDone)
                layout.subtitleTextView.text = LocaleController.formatPluralString("MessagesDeletedHint", count)
                layout.setTimer()
                buttonLayout = layout
            } else {
                val layout = LottieLayout(fragment.getParentActivity(), resourcesProvider)
                layout.textView.text = LocaleController.getString("DeleteAllFromSelfDone", R.string.DeleteAllFromSelfDone)
                layout.setTimer()
                buttonLayout = layout
            }
            buttonLayout.setButton(UndoButton(fragment.getParentActivity(), true, resourcesProvider).setDelayedAction(delayedAction))
            Bulletin.make(fragment, buttonLayout, Bulletin.DURATION_PROLONG).show()
        }

        fun saveStickerToGallery(activity: Activity, document: TLRPC.Document?, callback: Utilities.Callback<Uri>) {
            val path = FileLoader.getInstance(UserConfig.selectedAccount).getPathToAttach(document, true).toString()
            val temp = File(path)
            if (!temp.exists()) {
                return
            }
            saveStickerToGallery(activity, path, MessageObject.isVideoSticker(document), callback)
        }

        private fun saveStickerToGallery(activity: Activity, path: String?, video: Boolean, callback: Utilities.Callback<Uri>) {
            Utilities.globalQueue.postRunnable {
                tryOrLog {
                    if (video) {
                        MediaController.saveFile(path, activity, 1, null, null, callback)
                    } else {
                        val image = BitmapFactory.decodeFile(path)
                        if (image != null) {
                            val file = File(path!!.replace(".webp", ".png"))
                            val stream = FileOutputStream(file)
                            image.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            stream.close()
                            MediaController.saveFile(file.toString(), activity, 0, null, null, callback)
                        }
                    }
                }
            }
        }

        fun addFileToClipboard(file: File?, callback: Runnable) = tryOrLog {
            val context = ApplicationLoader.applicationContext
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file!!)
            val clip = ClipData.newUri(context.contentResolver, "label", uri)
            clipboard.setPrimaryClip(clip)
            callback.run()
        }

        @JvmStatic
        fun getDCLocation(dc: Int): String = when (dc) {
            1, 3 -> "Miami"
            2, 4 -> "Amsterdam"
            5 -> "Singapore"
            else -> "Unknown"
        }

        @JvmStatic
        fun getDCName(dc: Int): String = when (dc) {
            1 -> "Pluto"
            2 -> "Venus"
            3 -> "Aurora"
            4 -> "Vesta"
            5 -> "Flora"
            else -> "Unknown"
        }

        fun formatDCString(dc: Int): String {
            return String.format(Locale.US, "DC%d %s, %s", dc, getDCName(dc), getDCLocation(dc))
        }

        private val utf8Decoder = StandardCharsets.UTF_8.newDecoder()

        @JvmStatic
        fun readQrFromMessage(
            parent: View,
            selectedObject: MessageObject,
            selectedObjectGroup: MessageObject.GroupedMessages?,
            viewGroup: ViewGroup,
            callback: Utilities.Callback<ArrayList<QrHelper.QrResult>?>,
            waitForQr: AtomicBoolean,
            onQrDetectionDone: AtomicReference<Runnable?>
        ) {
            waitForQr.set(true)
            Utilities.globalQueue.postRunnable {
                val qrResults = ArrayList<QrHelper.QrResult>()
                val messageObjects = ArrayList<MessageObject>()
                if (selectedObjectGroup != null) {
                    messageObjects.addAll(selectedObjectGroup.messages)
                } else {
                    messageObjects.add(selectedObject)
                }
                for (i in 0 until viewGroup.childCount) {
                    val child = viewGroup.getChildAt(i)
                    if (child is ChatMessageCell) {
                        val cell = child
                        if (messageObjects.contains(cell.messageObject)) {
                            qrResults.addAll(readQr(cell.photoImage.getBitmap()))
                        }
                    }
                }
                AndroidUtilities.runOnUIThread {
                    callback.run(qrResults)
                    waitForQr.set(false)
                    if (onQrDetectionDone.get() != null) {
                        onQrDetectionDone.get()!!.run()
                        onQrDetectionDone.set(null)
                    }
                }
            }
            parent.postDelayed({
                if (onQrDetectionDone.get() != null) {
                    onQrDetectionDone.getAndSet(null)!!.run()
                }
            }, 250)
        }
    }
}
