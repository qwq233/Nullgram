/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.os.Debug;
import android.os.Handler;
import android.os.Looper;

import org.telegram.messenger.time.FastDateFormat;
import org.telegram.messenger.video.MediaCodecVideoConvertor;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.OutputStreamWriter;
import java.util.Map;

import top.qwq2333.nullgram.utils.Log;

/**
 * @deprecated use {@link Log} instead
 */
@Deprecated
public class FileLog {
    private OutputStreamWriter streamWriter = null;
    private FastDateFormat dateFormat = null;
    private FastDateFormat fileDateFormat = null;
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


    public static void dumpResponseAndRequest(TLObject request, TLObject response, TLRPC.TL_error error, long requestMsgId, long startRequestTimeInMillis, int requestToken) {
        return;
    }

    public static void dumpUnparsedMessage(TLObject message, long messageId) {
        return;
    }

    private static boolean gsonDisabled;
    public static void disableGson(boolean disable) {
        gsonDisabled = disable;
    }

    private static void checkGson() {
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

    private static long dumpedHeap;
    public void dumpMemory(boolean force) {
        if (!force && System.currentTimeMillis() - dumpedHeap < 30_000) return;
        dumpedHeap = System.currentTimeMillis();
        try {
            Debug.dumpHprofData(new File(AndroidUtilities.getLogsDir(), getInstance().dateFormat.format(System.currentTimeMillis()) + "_heap.hprof").getAbsolutePath());
        } catch (Exception e2) {
            FileLog.e(e2);
        }
    }

    private void dumpANR() {
        StringBuilder sb = new StringBuilder();
        Map<Thread, StackTraceElement[]> allThreads = Thread.getAllStackTraces();

        for (Map.Entry<Thread, StackTraceElement[]> entry : allThreads.entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTrace = entry.getValue();

            sb.append("Thread: ").append(thread.getName()).append("\n");
            for (StackTraceElement element : stackTrace) {
                sb.append("\tat ").append(element).append("\n");
            }
            sb.append("\n\n");
        }

        FileLog.e("ANR thread dump\n" + sb.toString());
        dumpMemory(false);
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
     * @deprecated use {@link Log#w(String msg)} instead
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

    public class ANRDetector {
        private final long TIMEOUT_MS = 5000; // ANR threshold (5 seconds)
        private final Handler mainHandler = new Handler(Looper.getMainLooper());
        private boolean isUIThreadResponsive = true;

        public ANRDetector(Runnable anrDetected) {
            new Thread(() -> {
                while (true) {
                    isUIThreadResponsive = false;

                    // Post a task to the main thread
                    mainHandler.post(() -> isUIThreadResponsive = true);

                    try {
                        Thread.sleep(TIMEOUT_MS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (!isUIThreadResponsive) {
                        anrDetected.run();
                    }
                }
            }).start();
        }
    }
}
