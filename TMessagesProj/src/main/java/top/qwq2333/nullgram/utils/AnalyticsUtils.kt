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
package top.qwq2333.nullgram.utils

import android.app.Application
import android.util.Base64
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import org.telegram.messenger.BuildConfig
import org.telegram.messenger.UserConfig
import top.qwq2333.gen.Config
import java.util.Arrays


object AnalyticsUtils {
    private var isInit = false
    private val isEnabled = BuildConfig.APPLICATION_ID != Base64.decode("dG9wLnF3cTIzMzMubnVsbGdyYW0=", Base64.DEFAULT).contentToString()
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var crashlytics: FirebaseCrashlytics

    @JvmStatic
    fun start(app: Application) {
        Log.d("Analytics: ${Base64.encodeToString(BuildConfig.APPLICATION_ID.toByteArray(), Base64.DEFAULT)}")
        Log.d("Analytics: ${BuildConfig.APPLICATION_ID != Arrays.toString(Base64.decode("dG9wLnF3cTIzMzMubnVsbGdyYW0=", Base64.DEFAULT))}")

        if (isInit && UserConfig.getActivatedAccountsCount() < 1) return // stop analytics if no user login
        try {
            firebaseAnalytics = Firebase.analytics
            crashlytics = FirebaseCrashlytics.getInstance()
            val currentUser = UserConfig.getInstance(UserConfig.selectedAccount)
            Log.d("FirebaseCrashlytics start: set user id: " + currentUser.getClientUserId())
            currentUser.getClientUserId().toString().run {
                crashlytics.setUserId(this)
                firebaseAnalytics.setUserId(this)
            }
            crashlytics.setCustomKey("Build Time", BuildConfig.BUILD_TIME)
            for (i in 0..UserConfig.MAX_ACCOUNT_COUNT) {
                UserConfig.getInstance(i)?.let {
                    if (!it.isClientActivated) return@let
                    crashlytics.setCustomKey("User $i", it.getClientUserId().toString())
                }
            }
            Config.showHiddenSettings.run {
                firebaseAnalytics.setUserProperty("isEnabledHiddenFeature", this.toString())
                crashlytics.setCustomKey("isEnabledHiddenFeature", this)
            }
        } catch (ignored: Exception) {
        }

        if (isEnabled) {
            isInit = true
            return
        }
        isInit = true
    }

    @JvmStatic
    fun setUserId(id: Long) {
        if (isEnabled) return
        Log.d("FirebaseCrashlytics reset: set user id: $id")
        id.toString().run {
            crashlytics.setUserId(this)
            firebaseAnalytics.setUserId(this)
        }
    }

    @JvmStatic
    fun trackCrashes(thr: Throwable) {
        if (isEnabled) return
        crashlytics.recordException(thr)
    }
}
