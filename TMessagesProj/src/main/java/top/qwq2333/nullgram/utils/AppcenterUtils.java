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

package top.qwq2333.nullgram.utils;

import android.app.ActivityManager;
import android.app.Application;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.crashes.Crashes;

import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.UserConfig;

import java.util.HashMap;

public class AppcenterUtils {

    private final static String appCenterToken = BuildVars.APPCENTER_HASH;
    private static boolean isInit = false;
    private static final Channel.Listener patchDeviceListener = new AbstractChannelListener() {
        @Override
        public void onPreparedLog(@NonNull com.microsoft.appcenter.ingestion.models.Log log, @NonNull String groupName, int flags) {
            var device = log.getDevice();
            device.setAppVersion(BuildConfig.VERSION_NAME);
            device.setAppBuild(String.valueOf(BuildConfig.VERSION_CODE));
        }
    };

    private static void addPatchDeviceListener() {
        try {
            var channelField = AppCenter.class.getDeclaredField("mChannel");
            channelField.setAccessible(true);
            var channel = (Channel) channelField.get(AppCenter.getInstance());
            assert channel != null;
            channel.addListener(patchDeviceListener);
        } catch (ReflectiveOperationException e) {
            Log.e("add listener", e);
        }
    }

    private static void patchDevice() {
        try {
            var handlerField = AppCenter.class.getDeclaredField("mHandler");
            handlerField.setAccessible(true);
            var handler = ((Handler) handlerField.get(AppCenter.getInstance()));
            assert handler != null;
            handler.post(AppcenterUtils::addPatchDeviceListener);
        } catch (ReflectiveOperationException e) {
            Log.e("patch device", e);
        }
    }

    public static void start(Application app) {
        if (isInit || !BuildConfig.APPLICATION_ID.equals("top.qwq2333.nullgram")) {
            return;
        }
        try {
            var currentUser = UserConfig.getInstance(UserConfig.selectedAccount);
            Log.d("FirebaseCrashlytics start: set user id: " + currentUser.getClientUserId());
            FirebaseCrashlytics.getInstance().setUserId(String.valueOf(currentUser.getClientUserId()));
        } catch (Exception ignored) { }

        AppCenter.start(app, appCenterToken, Crashes.class, Analytics.class);
        patchDevice();

        AppcenterUtils.trackEvent("App start");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            var am = app.getSystemService(ActivityManager.class);
            var map = new HashMap<String, String>(1);
            var reasons = am.getHistoricalProcessExitReasons(null, 0, 1);
            if (reasons.size() == 1) {
                map.put("description", reasons.get(0).getDescription());
                map.put("importance", String.valueOf(reasons.get(0).getImportance()));
                map.put("process", reasons.get(0).getProcessName());
                map.put("reason", String.valueOf(reasons.get(0).getReason()));
                map.put("status", String.valueOf(reasons.get(0).getStatus()));
                AppcenterUtils.trackEvent("Last exit reasons", map);
            }
        }

        isInit = true;
    }

    public static void trackEvent(String event) {
        if (!BuildConfig.APPLICATION_ID.equals("top.qwq2333.nullgram"))
            return;
        Analytics.trackEvent(event);
    }

    public static void trackEvent(String event, HashMap<String, String> map) {
        if (!BuildConfig.APPLICATION_ID.equals("top.qwq2333.nullgram"))
            return;
        Analytics.trackEvent(event, map);
    }

    public static void trackCrashes(Throwable thr) {
        if (!BuildConfig.APPLICATION_ID.equals("top.qwq2333.nullgram"))
            return;
        FirebaseCrashlytics.getInstance().recordException(thr);
        Crashes.trackError(thr);
    }

}
