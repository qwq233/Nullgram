/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.telegram.messenger.time.FastDateFormat;
import org.telegram.messenger.video.MediaCodecVideoConvertor;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.OutputStreamWriter;
import java.util.HashSet;

import top.qwq2333.nullgram.utils.Log;

/**
 * @deprecated use {@link Log} instead
 */
@Deprecated
public class FileLog {
    private OutputStreamWriter streamWriter = null;
    private FastDateFormat dateFormat = null;
    private DispatchQueue logQueue = null;

    private File currentFile = null;
    private File networkFile = null;
    private File tonlibFile = null;
    private boolean initied;
    public static boolean databaseIsMalformed = false;

    private OutputStreamWriter tlStreamWriter = null;
    private File tlRequestsFile = null;

    private final static String tag = "tmessages";
    private final static String mtproto_tag = "MTProto";

    private static volatile FileLog Instance = null;

    public static FileLog getInstance() {
        FileLog localInstance = Instance;
        if (localInstance == null) {
            synchronized (FileLog.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new FileLog();
                }
            }
        }
        return localInstance;
    }

    public FileLog() {
        return;
    }


    private static Gson gson;
    private static HashSet<String> excludeRequests;

    public static void dumpResponseAndRequest(TLObject request, TLObject response, TLRPC.TL_error error, long requestMsgId, long startRequestTimeInMillis, int requestToken) {
        return;
    }

    public static void dumpUnparsedMessage(TLObject message, long messageId) {
        return;
    }

    private static void checkGson() {
        if (gson == null) {
            HashSet<String> privateFields = new HashSet<>();
            privateFields.add("message");
            privateFields.add("phone");
            privateFields.add("about");
            privateFields.add("status_text");
            privateFields.add("bytes");
            privateFields.add("secret");
            privateFields.add("stripped_thumb");

            privateFields.add("networkType");
            privateFields.add("disableFree");

            //exclude file loading
            excludeRequests = new HashSet<>();
            excludeRequests.add("TL_upload_getFile");
            excludeRequests.add("TL_upload_getWebFile");

            gson = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {

                @Override
                public boolean shouldSkipField(FieldAttributes f) {
                    if (privateFields.contains(f.getName())) {
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean shouldSkipClass(Class<?> clazz) {
                    return false;
                }
            }).create();
        }
    }



    public void init() {
        return;
    }

    public static void ensureInitied() {
        return;
    }

    public static String getNetworkLogPath() {
        return "";
    }

    public static String getTonlibLogPath() {
        return "";
    }

    /**
     * @deprecated use {@link Log#e(String, Throwable)} instead
     */
    @Deprecated
    public static void e(final String message, final Throwable exception) {
        Log.e(message, exception);
    }

    /**
     * @deprecated use {@link Log#e(String msg)} instead
     */
    @Deprecated
    public static void e(final String message) {
        Log.e(message);
    }

    /**
     * @deprecated use {@link Log#e(String, Throwable)} instead
     */
    @Deprecated
    public static void e(final Throwable e) {
        e(e, true);
    }

    /**
     * @deprecated use {@link Log#e(String, Throwable)} instead
     */
    @Deprecated
    public static void e(final Throwable e, boolean logToAppCenter) {
        Log.e(e);
        if (needSent(e) && logToAppCenter) {
            AndroidUtilities.appCenterLog(e);
        }
    }

    public static void fatal(final Throwable e) {
        fatal(e, true);
    }

    public static void fatal(final Throwable e, boolean logToAppCenter) {
        if (needSent(e) && logToAppCenter) {
            AndroidUtilities.appCenterLog(e);
        }
    }

    private static boolean needSent(Throwable e) {
        if (e instanceof InterruptedException || e instanceof MediaCodecVideoConvertor.ConversionCanceledException || e instanceof IgnoreSentException) {
            return false;
        }
        return true;
    }

    /**
     * @deprecated use {@link #e(String)} instead
     * @param message
     */
    @Deprecated
    public static void d(final String message) {
        Log.d(message);
    }

    /**
     * @deprecated use {@link Log#w(String msg)} )} instead
     */
    @Deprecated
    public static void w(final String message) {
        Log.w(message);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public static void cleanupLogs() {
    }

    public static class IgnoreSentException extends Exception{

        public IgnoreSentException(String e) {
            super(e);
        }

    }
}
