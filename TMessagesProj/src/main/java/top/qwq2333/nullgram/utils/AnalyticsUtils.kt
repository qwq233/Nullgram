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

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.Handler
import android.util.Base64
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.channel.AbstractChannelListener
import com.microsoft.appcenter.channel.Channel
import com.microsoft.appcenter.crashes.Crashes
import org.telegram.messenger.BuildConfig
import org.telegram.messenger.BuildVars
import org.telegram.messenger.UserConfig
import java.util.Arrays


object AnalyticsUtils {
    private val appCenterToken = BuildVars.APPCENTER_HASH
    private var isInit = false
    private val isEnabled = BuildConfig.APPLICATION_ID != Arrays.toString(Base64.decode("dG9wLnF3cTIzMzMubnVsbGdyYW0=", Base64.DEFAULT))
    private val patchDeviceListener: Channel.Listener = object : AbstractChannelListener() {
        override fun onPreparedLog(log: com.microsoft.appcenter.ingestion.models.Log, groupName: String, flags: Int) {
            val device = log.device
            device.appVersion = BuildConfig.VERSION_NAME
            device.appBuild = BuildConfig.VERSION_CODE.toString()
        }
    }

    private fun addPatchDeviceListener() {
        try {
            val channelField = AppCenter::class.java.getDeclaredField("mChannel")
            channelField.isAccessible = true
            val channel = (channelField[AppCenter.getInstance()] as Channel)
            channel.addListener(patchDeviceListener)
        } catch (e: ReflectiveOperationException) {
            Log.e("add listener", e)
        }
    }

    private fun patchDevice() {
        try {
            val handlerField = AppCenter::class.java.getDeclaredField("mHandler")
            handlerField.isAccessible = true
            val handler = handlerField[AppCenter.getInstance()] as Handler
            handler.post {
                addPatchDeviceListener()
            }
        } catch (e: ReflectiveOperationException) {
            Log.e("patch device", e)
        }
    }

    @JvmStatic
    fun start(app: Application) {
        Log.d("Analytics: ${Base64.encodeToString(BuildConfig.APPLICATION_ID.toByteArray(), Base64.DEFAULT)}")
        Log.d("Analytics: ${BuildConfig.APPLICATION_ID != Arrays.toString(Base64.decode("dG9wLnF3cTIzMzMubnVsbGdyYW0=", Base64.DEFAULT))}")

        if (isInit && UserConfig.getActivatedAccountsCount() < 1) return // stop analytics if no user login
        try {
            val currentUser = UserConfig.getInstance(UserConfig.selectedAccount)
            Log.d("FirebaseCrashlytics start: set user id: " + currentUser.getClientUserId())
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setUserId(currentUser.getClientUserId().toString())
            crashlytics.setCustomKey("Build Time", BuildConfig.BUILD_TIME)
            for (i in 0 ..  UserConfig.MAX_ACCOUNT_COUNT) {
                UserConfig.getInstance(i)?.let {
                    if (!it.isClientActivated) return@let
                    crashlytics.setCustomKey("User $i", it.getClientUserId().toString())
                }
            }
        } catch (ignored: Exception) { }

        if (isEnabled) {
            isInit = true
            return
        }

        AppCenter.start(app, appCenterToken, Analytics::class.java)
        patchDevice()
        trackEvent("App start")
        AppCenter.setUserId(UserConfig.getInstance(UserConfig.selectedAccount)?.getClientUserId().toString())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val am = app.getSystemService(ActivityManager::class.java)
            val map = HashMap<String, String?>(1)
            val reasons = am.getHistoricalProcessExitReasons(null, 0, 1)
            if (reasons.size == 1) {
                map["description"] = reasons[0].description
                map["importance"] = reasons[0].importance.toString()
                map["process"] = reasons[0].processName
                map["reason"] = reasons[0].reason.toString()
                map["status"] = reasons[0].status.toString()
                trackEvent("Last exit reasons", map)
            }
        }
        isInit = true
    }

    @JvmStatic
    fun setUserId(id: Long) {
        if (isEnabled) return
        Log.d("FirebaseCrashlytics reset: set user id: $id")
        FirebaseCrashlytics.getInstance().setUserId(id.toString())
    }

    @JvmStatic
    fun trackEvent(event: String) {
        if (isEnabled) return
        Analytics.trackEvent(event)
    }

    @JvmStatic
    fun trackEvent(event: String, map: HashMap<String, String?>?) {
        if (isEnabled) return
        Analytics.trackEvent(event, map)
    }

    @JvmStatic
    fun trackCrashes(thr: Throwable) {
        if (isEnabled) return
        FirebaseCrashlytics.getInstance().recordException(thr)
        Crashes.trackError(thr)
    }
}
