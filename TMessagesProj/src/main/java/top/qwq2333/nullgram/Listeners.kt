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

package top.qwq2333.nullgram

import android.os.Build
import android.service.notification.NotificationListenerService
import android.telephony.TelephonyCallback
import androidx.annotation.RequiresApi
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.KeepAliveJob

@RequiresApi(Build.VERSION_CODES.S)
abstract class CallStateListener : TelephonyCallback(), TelephonyCallback.CallStateListener {
    abstract override fun onCallStateChanged(state: Int)
}

class NullgramPushService : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        ApplicationLoader.postInitApplication()
        KeepAliveJob.startJob()
    }

}
