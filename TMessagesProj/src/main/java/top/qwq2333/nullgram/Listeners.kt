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

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.telephony.TelephonyCallback
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.KeepAliveJob
import org.telegram.messenger.LocaleController
import org.telegram.messenger.NotificationsController
import org.telegram.messenger.R
import top.qwq2333.nullgram.utils.Log

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

class SaveToDownloadReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "MediaController"
        private const val ACTION_CANCEL = "top.qwq2333.nullgram.SAVE_TO_DOWNLOAD_CANCEL"
        private const val EXTRA_ID = "top.qwq2333.nullgram.NOTIFICATION_ID"

        val notifications = hashMapOf<Int, Pair<NotificationCompat.Builder, () -> Unit>>()
        private val notificationManager by lazy {
            NotificationManagerCompat.from(ApplicationLoader.applicationContext)
        }
        @JvmStatic
        var id = 0
            get() = field++
            private set

        @SuppressLint("MissingPermission")
        @JvmStatic
        fun makeNotification(context: Context, id: Int, toFinish: Int, cancel: () -> Unit) {
            NotificationsController.checkOtherNotificationsChannel()
            val intent = Intent(context, SaveToDownloadReceiver::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_ID, id)
            }
            val pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val builder = NotificationCompat.Builder(context, NotificationsController.OTHER_NOTIFICATIONS_CHANNEL).apply {
                setContentTitle(context.getString(R.string.AppName))
                setTicker(LocaleController.formatPluralString("SaveToDownloadCount", toFinish))
                setContentText(LocaleController.formatPluralString("SaveToDownloadCount", toFinish))
                setCategory(NotificationCompat.CATEGORY_STATUS)
                setProgress(100, 0, true)
                setWhen(System.currentTimeMillis())
                setOngoing(true)
                setOnlyAlertOnce(true)
                setSmallIcon(android.R.drawable.stat_sys_download)
                setCategory(NotificationCompat.CATEGORY_PROGRESS)
                addAction(R.drawable.ic_close_white, LocaleController.getString(R.string.Cancel), pendingIntent)
            }

            notifications[id] = builder to cancel
            notificationManager.notify(TAG, id, builder.build())
        }

        @SuppressLint("MissingPermission")
        @JvmStatic
        fun updateProgress(id: Int, progress: Int) {
            if (notifications[id] != null) {
                notifications[id]!!.first.setProgress(100, progress, false)
                notificationManager.notify(TAG, id, notifications[id]!!.first.build())
            }
        }

        @JvmStatic
        fun cancelNotification(id: Int) {
            Log.d("Cancel notification $id")
            notifications[id]?.second?.invoke()
            notifications.remove(id)
            notificationManager.cancel(TAG, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CANCEL) {
            val id = intent.getIntExtra(EXTRA_ID, -1)
            if (id != -1) {
                cancelNotification(id)
            }
        }
    }
}
