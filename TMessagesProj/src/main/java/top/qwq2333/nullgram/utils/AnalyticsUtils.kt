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
import android.util.Base64
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import org.telegram.messenger.BuildConfig
import org.telegram.messenger.UserConfig
import java.util.Arrays

object AnalyticsUtils {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private var isInit = false
    private val isEnabled = BuildConfig.APPLICATION_ID != Arrays.toString(Base64.decode("dG9wLnF3cTIzMzMubnVsbGdyYW0=", Base64.DEFAULT))

    @JvmStatic
    fun start(app: Application) {
        Log.d("Analytics: ${Base64.encodeToString(BuildConfig.APPLICATION_ID.toByteArray(), Base64.DEFAULT)}")
        Log.d("Analytics: ${BuildConfig.APPLICATION_ID != Arrays.toString(Base64.decode("dG9wLnF3cTIzMzMubnVsbGdyYW0=", Base64.DEFAULT))}")

        if (isInit && UserConfig.getActivatedAccountsCount() < 1) return // stop analytics if no user login

        firebaseAnalytics = Firebase.analytics
        try {
            val currentUser = UserConfig.getInstance(UserConfig.selectedAccount)
            Log.d("FirebaseCrashlytics start: set user id: " + currentUser.getClientUserId())
            val crashlytics = FirebaseCrashlytics.getInstance()

            firebaseAnalytics.setUserId(currentUser.getClientUserId().toString())
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN) {
                for (i in 0..UserConfig.MAX_ACCOUNT_COUNT) {
                    UserConfig.getInstance(i)?.let {
                        if (!it.isClientActivated) return@let
                        param("User $i", it.getClientUserId().toString())
                    }
                }
                param("Build Time", BuildConfig.BUILD_TIME)
                param("Flavor", BuildConfig.FLAVOR)
                param("Build Type", BuildConfig.BUILD_TYPE)
                param("Device", Build.DEVICE)
                param("Model", Build.MODEL)
                param("Product", Build.PRODUCT)
                param("Android Version", Build.VERSION.SDK_INT.toString())
            }

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val am = app.getSystemService(ActivityManager::class.java)
            val reasons = am.getHistoricalProcessExitReasons(null, 0, 1)
            if (reasons.size == 1) {
                firebaseAnalytics.logEvent("last_exit_reason") {
                    param("description", reasons[0].description ?: "null")
                    param("importance", reasons[0].importance.toString())
                    param("process", reasons[0].processName)
                    param("reason", reasons[0].reason.toString())
                    param("status", reasons[0].status.toString())
                }
            }
        }
        isInit = true
    }

    @JvmStatic
    fun setUserId(id: Long) {
        if (isEnabled) return
        Log.d("FirebaseCrashlytics reset: set user id: $id")
        firebaseAnalytics.setUserId(id.toString())
        FirebaseCrashlytics.getInstance().setUserId(id.toString())
    }

    @JvmStatic
    fun trackEvent(event: String, map: HashMap<String, String?>?) {
        if (isEnabled) return
        firebaseAnalytics.logEvent(event) {
            map?.forEach { (key, value) ->
                param(key, value ?: "null")
            }
        }
    }

    @JvmStatic
    fun trackCrashes(thr: Throwable) {
        if (isEnabled) return
        FirebaseCrashlytics.getInstance().recordException(thr)
    }

    fun trackFunctionSwitch(key: String, value: Boolean) {
        if (isEnabled) return
        firebaseAnalytics.logEvent("func_switch") {
            param("key", key)
            param("value", value.toString())
        }
    }
}
