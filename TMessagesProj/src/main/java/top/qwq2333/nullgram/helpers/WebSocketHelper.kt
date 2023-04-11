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
package top.qwq2333.nullgram.helpers

import androidx.core.util.Pair
import org.tcp2ws.tcp2wsServer
import org.telegram.messenger.BuildConfig
import org.telegram.messenger.FileLog
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import top.qwq2333.nullgram.config.ConfigManager
import top.qwq2333.nullgram.utils.AnalyticsUtils.trackEvent
import top.qwq2333.nullgram.utils.Defines
import top.qwq2333.nullgram.utils.Log.d
import top.qwq2333.nullgram.utils.Log.e
import top.qwq2333.nullgram.utils.Log.i
import java.net.ServerSocket

object WebSocketHelper {
    const val proxyServer = "ws.neko"

    private var socksPort = -1
    private var tcp2wsStarted = false
    private var tcp2wsServer: tcp2wsServer? = null

    private val userAgent = "Nekogram/9.5.8 (3252; 381d52f35f552e10ad1701445dba9cd14acb7e43)"
    private val connHash = "381d52f35f552e10ad1701445dba9cd14acb7e43"

    enum class WsProvider(val num: Int, var host: String) {
        Nekogram(0, "nekoe.eu.org"),
        Custom(2, ConfigManager.getStringOrDefault(Defines.wsServerHost, "")!!),
    }

    @JvmStatic
    var currentProvider = when (ConfigManager.getIntOrDefault(Defines.wsBuiltInProxyBackend, WsProvider.Nekogram.num)) {
        WsProvider.Nekogram.num -> WsProvider.Nekogram
        WsProvider.Custom.num -> WsProvider.Custom
        else -> WsProvider.Nekogram
    }
        set(value) {
            if (value.equals(WsProvider.Custom)) {
                value.host = ConfigManager.getStringOrDefault(Defines.wsServerHost, "")!!
            }
            ConfigManager.putInt(Defines.wsBuiltInProxyBackend, value.num)
            field = value
        }

    @JvmStatic
    fun getProviders(): Pair<ArrayList<String>, ArrayList<WsProvider>> {
        val names = ArrayList<String>()
        val types = ArrayList<WsProvider>()
        names.add("Nekogram")
        types.add(WsProvider.Nekogram)
        names.add(LocaleController.getString("AutoDownloadCustom", R.string.AutoDownloadCustom))
        types.add(WsProvider.Custom)
        return Pair(names, types)
    }

    @JvmField
    var wsEnableTLS = ConfigManager.getBooleanOrDefault(Defines.wsEnableTLS, true)

    @JvmStatic
    fun toggleWsEnableTLS() {
        ConfigManager.putBoolean(Defines.wsEnableTLS, !ConfigManager.getBooleanOrDefault(Defines.wsEnableTLS, true))
        wsEnableTLS = ConfigManager.getBooleanOrDefault(Defines.wsEnableTLS, true)
    }

    @JvmStatic
    fun getSocksPort(): Int {
        return getSocksPort(6356)
    }

    @JvmStatic
    fun wsReloadConfig() {
        d("ws reload config: ${currentProvider.host} tls: $wsEnableTLS")
        if (tcp2wsServer != null) {
            try {
                tcp2wsServer!!.setCdnDomain(currentProvider.host)
                    .setTls(wsEnableTLS)
                    .setUserAgent(System.getProperty("http.agent") + " " + userAgent)
                    .setConnHash(connHash)
            } catch (e: Exception) {
                e(e)
            }
        }
    }

    fun getSocksPort(port: Int): Int {
        return if (tcp2wsStarted && socksPort != -1) {
            socksPort
        } else try {
            if (port != -1) {
                socksPort = port
            } else {
                val socket = ServerSocket(0)
                socksPort = socket.localPort
                socket.close()
            }
            if (!tcp2wsStarted) {
                i("useragent: ${System.getProperty("http.agent")} $userAgent")
                tcp2wsServer = tcp2wsServer().setCdnDomain(currentProvider.host)
                    .setTls(wsEnableTLS)
                    .setUserAgent(System.getProperty("http.agent") + " " + userAgent)
                    .setConnHash(connHash)
                tcp2wsServer!!.start(socksPort)
                tcp2wsStarted = true
                val map = HashMap<String, String?>()
                map["buildType"] = BuildConfig.BUILD_TYPE
                map["buildFlavor"] = BuildConfig.FLAVOR
                map["isPlay"] = BuildConfig.isPlay.toString()
                trackEvent("tcp2ws started", map)
            }
            d("tcp2ws started on port " + socksPort)
            d("serverHost: " + currentProvider.host + " tls: " + wsEnableTLS)
            socksPort
        } catch (e: Exception) {
            FileLog.e(e)
            if (port != -1) {
                getSocksPort(-1)
            } else {
                -1
            }
        }
    }
}
