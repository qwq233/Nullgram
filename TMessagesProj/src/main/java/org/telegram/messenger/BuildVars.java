/*
 * This is the source code of Telegram for Android v. 7.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2020.
 */

package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public class BuildVars {

    public static final boolean DEBUG_VERSION = true;
    public static boolean DEBUG_PRIVATE_VERSION = false;
    public static boolean CHECK_UPDATES = true;
    public static boolean LOGS_ENABLED = true;
    public static boolean USE_CLOUD_STRINGS = true;
    public static boolean NO_SCOPED_STORAGE = Build.VERSION.SDK_INT <= 29;
    public static final int BUILD_VERSION = 4275;
    public static final String BUILD_VERSION_STRING = "10.6.1";
    public static final int APP_ID = 19797609;
    public static final String APP_HASH = "e8f1567dbbf38944a1391c4d23c34b60";
    public static final String APPCENTER_HASH = "e07b49da-11a5-46db-a780-f5cd7b9a1a5a";
    public static String SAFETYNET_KEY = "";

    public static String SMS_HASH = "O2P2z+/jBpJ";
    public static final  String PLAYSTORE_APP_URL = "https://play.google.com/store/apps/details?id=top.qwq2333.nullgram";
    public static String HUAWEI_STORE_URL = "";

    public static String GOOGLE_AUTH_CLIENT_ID = "760348033671-81kmi3pi84p11ub8hp9a1funsv0rn2p9.apps.googleusercontent.com";


    // You can use this flag to disable Google Play Billing (If you're making fork and want it to be in Google Play)
    public static boolean IS_BILLING_UNAVAILABLE = BuildConfig.isPlay;

    static {
        if (ApplicationLoader.applicationContext != null) {
            SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("systemConfig", Context.MODE_PRIVATE);
            LOGS_ENABLED = DEBUG_VERSION || sharedPreferences.getBoolean("logsEnabled", DEBUG_VERSION);
        }
    }

    public static boolean useInvoiceBilling() {
        return true;
    }


    public static boolean isStandaloneApp() {
        return false;
    }

    public static boolean isBetaApp() {
        return false;
    }


    public static boolean isHuaweiStoreApp() {
        return ApplicationLoader.isHuaweiStoreBuild();
    }

    public static String getSmsHash() {
        return ApplicationLoader.isStandaloneBuild() ? "w0lkcmTZkKh" : (DEBUG_VERSION ? "O2P2z+/jBpJ" : "oLeq9AcOZkT");
    }
}
