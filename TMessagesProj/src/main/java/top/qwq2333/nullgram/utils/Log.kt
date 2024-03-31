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

import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.telegram.messenger.AndroidUtilities
import java.io.File
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Log {
    const val TAG = "Nullgram"
    private val logFile: File

    enum class Level {
        DEBUG, INFO, WARN, ERROR
    }

    init {
        val parentFile = AndroidUtilities.getLogsDir()
        CoroutineScope(Dispatchers.IO).launch {
            parentFile.listFiles()?.forEach {
                // delete logs older than 1 day
                if (it.readAttributes().creationTime().toMillis() < System.currentTimeMillis() - 1 * 24 * 60 * 60 * 1000) {
                    it.delete()
                }
            }
        }

        logFile = File(parentFile, "log-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.txt").also {
            if (!it.exists()) {
                it.createNewFile()
            }
            it.setWritable(true)
            it.appendText(">>>> Log start at ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}\n", Charset.forName("UTF-8"))
        }
    }

    private fun writeToFile(level: Level, tag: String?, msg: String) {
        CoroutineScope(Dispatchers.IO).launch {
            logFile.appendText("${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())} ${level.name} ${tag ?: ""}: $msg\n", Charset.forName("UTF-8"))
        }
    }
    @JvmStatic
    fun shareLog(context: Context) {
        ShareUtil.shareFile(context, logFile)
    }

    @JvmStatic
    fun refreshLog() {
        synchronized(logFile) {
            logFile.let {
                it.delete()
                it.createNewFile()
                it.appendText(">>>> Log start at ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}\n", Charset.forName("UTF-8"))
            }
        }
    }

    /**
     * 日志等级 Debug
     * @param msg 日志内容
     */
    @JvmStatic
    fun d(tag: String, msg: String) {
        Log.d(TAG, "$tag: $msg")
        writeToFile(Level.DEBUG, tag, msg)
    }

    /**
     * 日志等级 Info
     * @param msg 日志内容
     */
    @JvmStatic
    fun i(tag: String, msg: String) {
        Log.i(TAG, "$tag: $msg")
        writeToFile(Level.INFO, tag, msg)
    }

    /**
     * 日志等级 Warn
     * @param msg 日志内容
     */
    @JvmStatic
    fun w(tag: String, msg: String) {
        Log.w(TAG, "$tag: $msg")
        writeToFile(Level.WARN, tag, msg)
        FirebaseCrashlytics.getInstance().log("$tag: $msg")
    }

    /**
     * 日志等级 Error
     * @param msg 日志内容
     */
    @JvmStatic
    fun e(tag: String, msg: String) {
        Log.e(TAG, "$tag: $msg")
        writeToFile(Level.ERROR, tag, msg)
        FirebaseCrashlytics.getInstance().log("$tag: $msg")
    }

    /**
     * 日志等级 Debug
     * @param throwable 异常
     * @param msg 日志内容
     */
    @JvmStatic
    @JvmOverloads
    fun d(msg: String, throwable: Throwable? = null) {
        Log.d(TAG, msg, throwable)
        writeToFile(Level.DEBUG, null, msg)
        if (throwable != null) writeToFile(Level.DEBUG, null, throwable.stackTraceToString())
    }

    /**
     * 日志等级 Info
     * @param throwable 异常
     * @param msg 日志内容
     */
    @JvmStatic
    @JvmOverloads
    fun i(msg: String, throwable: Throwable? = null) {
        Log.i(TAG, msg, throwable)
        writeToFile(Level.INFO, null, msg)
        if (throwable != null) writeToFile(Level.INFO, null, throwable.stackTraceToString())
    }

    /**
     * 日志等级 Warn
     * @param throwable 异常
     * @param msg 日志内容
     */
    @JvmStatic
    @JvmOverloads
    fun w(msg: String, throwable: Throwable? = null) {
        Log.w(TAG, msg, throwable)
        writeToFile(Level.WARN, null, msg)
        if (throwable != null) writeToFile(Level.WARN, null, throwable.stackTraceToString())
    }

    /**
     * 日志等级 Error
     * @param throwable 异常
     * @param msg 日志内容
     */
    @JvmStatic
    @JvmOverloads
    fun e(msg: String, throwable: Throwable? = null) {
        Log.e(TAG, msg, throwable)
        writeToFile(Level.ERROR, null, msg)
        if (throwable != null) writeToFile(Level.ERROR, null, throwable.stackTraceToString())
        if (throwable != null) {
            AnalyticsUtils.trackCrashes(throwable)
        }
    }

    /**
     * 触发一次崩溃
     * 当throwable为null时，触发一个空指针异常
     * 当throwable不为null时，触发throwable
     *
     * @param throwable 异常
     */
    @JvmStatic
    @JvmOverloads
    fun crash(throwable: Throwable? = null) {
        if (throwable != null) {
            throw throwable
        } else {
            throw NullPointerException("manual crash")
        }
    }

    /**
     * DEBUG ONLY
     */
    @JvmStatic
    fun throwException() = try {
        throw NullPointerException("manual crash")
    } catch (e: Exception) {
        w(e)
    }

    /**
     * 日志等级 Warn
     * @param throwable 异常
     */
    @JvmStatic
    fun w(throwable: Throwable) {
        Log.w(TAG, "", throwable)
        writeToFile(Level.WARN, null, throwable.stackTraceToString())
        AnalyticsUtils.trackCrashes(throwable)

    }

    /**
     * 日志等级 Error
     * @param throwable 异常
     */
    @JvmStatic
    fun e(throwable: Throwable) {
        Log.e(TAG, "", throwable)
        writeToFile(Level.ERROR, null, throwable.stackTraceToString())
        AnalyticsUtils.trackCrashes(throwable)
    }

    private const val ENABLE_NATIVE_LOG = false

    @JvmStatic
    fun nativeLog(level: Int, tag: String, msg: String) {
        if (!ENABLE_NATIVE_LOG) return
        when(level) {
            0 -> d(tag, msg)
            1 -> i(tag, msg)
            2 -> w(tag, msg)
            3 -> e(tag, msg)
        }
    }
}
