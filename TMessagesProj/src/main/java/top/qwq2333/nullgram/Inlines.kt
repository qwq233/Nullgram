/*
 * Copyright (C) 2019-2024 qwq233 <qwq233@qwq2333.top>
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

package top.qwq2333.nullgram

import android.view.View
import android.widget.LinearLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.telegram.messenger.AccountInstance
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Components.LayoutHelper
import top.qwq2333.nullgram.utils.Log
import java.net.URLEncoder

fun String.encodeUrl(): String = URLEncoder.encode(this, "UTF-8")
fun String.isNumber(): Boolean = try {
    this.toLong()
    true
} catch (e: NumberFormatException) {
    false
}

internal inline fun tryOrLog(block: () -> Unit) = runCatching {
    block()
}.onFailure {
    Log.e(it)
}

internal inline fun runOnIoDispatcher(crossinline block: () -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        block()
    }
}

internal inline fun LinearLayout.addView(view: View, init: LinearLayout.LayoutParams.() -> Unit) {
    addView(
        view, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT) // default params
            .apply(init)
    )
}

internal inline fun createLinear(init: LinearLayout.LayoutParams.() -> Unit): LinearLayout.LayoutParams {
    return LayoutHelper.createLinear(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        .apply(init)
}

internal fun AccountInstance.cacheUsersAndChats(users: ArrayList<TLRPC.User>? = null, chats: ArrayList<TLRPC.Chat>? = null) {
    this.messagesStorage.putUsersAndChats(users, chats, false, true)
    this.messagesController.apply {
        putUsers(users, false)
        putChats(chats, false)
    }
}

internal fun cacheUsersAndChats(users: ArrayList<TLRPC.User>? = null, chats: ArrayList<TLRPC.Chat>? = null) {
    AccountInstance.getInstance(UserConfig.selectedAccount).apply {
        cacheUsersAndChats(users, chats)
    }
}
