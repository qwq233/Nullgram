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

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.messenger.UserConfig
import org.telegram.messenger.XiaomiUtilities
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.AlertsCreator
import org.telegram.ui.Components.BulletinFactory
import org.telegram.ui.Components.LayoutHelper
import org.telegram.ui.Components.RLottieImageView
import org.telegram.ui.LaunchActivity
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


object APKUtils {

    @SuppressLint("StaticFieldLeak")
    private var dialog: AlertDialog? = null
    private fun installApk(context: Context, apk: File) {
        val flag = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        val action = APKUtils::class.java.name
        val intent = Intent(action).setPackage(context.packageName)
        val pending = PendingIntent.getBroadcast(context, 0, intent, flag)
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        try {
            installer.openSession(installer.createSession(params)).use { session ->
                val out = session.openWrite(apk.name, 0, apk.length())
                FileInputStream(apk).use { `in` -> out.use { transfer(`in`, out) } }
                session.commit(pending.intentSender)
            }
        } catch (e: IOException) {
            Log.e(e)
            if (dialog != null) {
                dialog!!.dismiss()
                dialog = null
            }
            AlertsCreator.createSimpleAlert(
                context, """
     ${LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred)}
     ${e.localizedMessage}
     """.trimIndent()
            ).show()
        }
    }

    @JvmStatic
    fun installUpdate(context: Activity?, document: TLRPC.Document?) {
        if (context == null || document == null) {
            return
        }
        if (XiaomiUtilities.isMIUI()) {
            AndroidUtilities.openForView(document, false, context)
            return
        }
        val apk = FileLoader.getInstance(UserConfig.selectedAccount).getPathToAttach(document, true) ?: return
        if (dialog != null && dialog!!.isShowing) {
            return
        }
        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.layoutParams =
            LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT.toFloat(), Gravity.TOP or Gravity.LEFT, 4f, 4f, 4f, 4f)
        val imageView = RLottieImageView(context)
        imageView.setAutoRepeat(true)
        imageView.setAnimation(R.raw.db_migration_placeholder, 160, 160)
        imageView.playAnimation()
        linearLayout.addView(imageView, LayoutHelper.createLinear(160, 160, Gravity.CENTER_HORIZONTAL or Gravity.TOP, 17, 24, 17, 0))
        val textView = TextView(context)
        textView.typeface = AndroidUtilities.getTypeface("fonts/rmedium.ttf")
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack))
        textView.isSingleLine = true
        textView.ellipsize = TextUtils.TruncateAt.END
        textView.text = LocaleController.getString("UpdateInstalling", R.string.UpdateInstalling)
        linearLayout.addView(
            textView,
            LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL or Gravity.TOP, 17, 20, 17, 0)
        )
        val textView2 = TextView(context)
        textView2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        textView2.setTextColor(Theme.getColor(Theme.key_dialogTextGray))
        textView2.text = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(context)) LocaleController.getString(
            "UpdateInstallingRelaunch", R.string.UpdateInstallingRelaunch
        ) else LocaleController.getString("UpdateInstallingNotification", R.string.UpdateInstallingNotification)
        linearLayout.addView(
            textView2,
            LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL or Gravity.TOP, 17, 4, 17, 24)
        )
        val builder = AlertDialog.Builder(context)
        builder.setView(linearLayout)
        dialog = builder.create()
        dialog!!.setCanceledOnTouchOutside(false)
        dialog!!.setCancelable(false)
        dialog!!.show()
        CoroutineScope(Dispatchers.IO).launch {
            val receiver = register(context) {
                if (dialog != null) {
                    dialog!!.dismiss()
                    dialog = null
                }
            }
            installApk(context, apk)
            val intent = receiver.waitIntent()
            if (intent != null) {
                context.startActivity(intent)
            }
        }
    }

    @Throws(IOException::class)
    private fun transfer(`in`: InputStream, out: OutputStream) {
        val size = 8192
        val buffer = ByteArray(size)
        var read: Int
        while (`in`.read(buffer, 0, size).also { read = it } >= 0) {
            out.write(buffer, 0, read)
        }
    }

    private fun register(context: Context, onSuccess: Runnable): InstallReceiver {
        val receiver = InstallReceiver(context, ApplicationLoader.getApplicationId(), onSuccess)
        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED)
        filter.addDataScheme("package")
        context.registerReceiver(receiver, filter)
        context.registerReceiver(receiver, IntentFilter(APKUtils::class.java.name))
        return receiver
    }

    private class InstallReceiver(private val context: Context, private val packageName: String, private val onSuccess: Runnable) : BroadcastReceiver() {
        private val latch = CountDownLatch(1)
        private var intent: Intent? = null

        override fun onReceive(c: Context, i: Intent) {
            if (Intent.ACTION_PACKAGE_ADDED == i.action) {
                val data = i.data ?: return
                val pkg = data.schemeSpecificPart
                if (pkg == packageName) {
                    onSuccess.run()
                    context.unregisterReceiver(this)
                }
                return
            }
            val status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE_INVALID)
            when (status) {
                PackageInstaller.STATUS_PENDING_USER_ACTION -> intent = i.getParcelableExtra(Intent.EXTRA_INTENT)!!

                PackageInstaller.STATUS_FAILURE, PackageInstaller.STATUS_FAILURE_BLOCKED, PackageInstaller.STATUS_FAILURE_CONFLICT,
                PackageInstaller.STATUS_FAILURE_INCOMPATIBLE, PackageInstaller.STATUS_FAILURE_INVALID, PackageInstaller.STATUS_FAILURE_STORAGE -> {
                    val id = i.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, 0)
                    if (id > 0) {
                        val installer = context.packageManager.packageInstaller
                        val info = installer.getSessionInfo(id)
                        if (info != null) {
                            installer.abandonSession(info.sessionId)
                        }
                    }
                    if (context is LaunchActivity) {
                        context.showBulletin { factory: BulletinFactory ->
                            factory.createErrorBulletin(LocaleController.formatString("UpdateFailedToInstall", R.string.UpdateFailedToInstall, status))
                        }
                    }
                    onSuccess.run()
                    context.unregisterReceiver(this)
                }

                PackageInstaller.STATUS_FAILURE_ABORTED, PackageInstaller.STATUS_SUCCESS -> {
                    onSuccess.run()
                    context.unregisterReceiver(this)
                }

                else -> {
                    onSuccess.run()
                    context.unregisterReceiver(this)
                }
            }
            latch.countDown()
        }

        fun waitIntent(): Intent? {
            latch.await(10, TimeUnit.SECONDS)
            return intent
        }
    }

    class UpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_MY_PACKAGE_REPLACED != intent.action) return
            val packageName = context.packageName
            val installer = context.packageManager.getInstallerPackageName(packageName)
            if (packageName != installer) return
            val startIntent = Intent(context, LaunchActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(context)) {
                context.startActivity(startIntent)
            } else {
                val channel = NotificationChannelCompat.Builder("updated", NotificationManagerCompat.IMPORTANCE_HIGH)
                    .setName(LocaleController.getString("UpdateApp", R.string.UpdateApp)).setLightsEnabled(false).setVibrationEnabled(false)
                    .setSound(null, null).build()
                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.createNotificationChannel(channel)
                val pendingIntent =
                    PendingIntent.getActivity(context, 0, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    notificationManager.notify(
                        8732833,
                        NotificationCompat.Builder(context, "updated").setSmallIcon(R.drawable.notification).setColor(-0xee5306)
                            .setShowWhen(false)
                            .setContentText(LocaleController.getString("UpdateInstalledNotification", R.string.UpdateInstalledNotification))
                            .setCategory(NotificationCompat.CATEGORY_STATUS).setContentIntent(pendingIntent).build()
                    )
                }
            }
        }
    }
}
