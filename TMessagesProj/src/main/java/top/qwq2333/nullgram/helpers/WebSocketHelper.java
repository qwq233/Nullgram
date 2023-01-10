package top.qwq2333.nullgram.helpers;

import org.tcp2ws.tcp2wsServer;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import top.qwq2333.nullgram.config.ConfigManager;
import top.qwq2333.nullgram.utils.AppcenterUtils;
import top.qwq2333.nullgram.utils.Defines;
import top.qwq2333.nullgram.utils.Log;

public class WebSocketHelper {
    public static final String NekogramPublicProxyServer = "ws.neko";
    public static final String NekogramXPublicProxyServer = "tehcneko.xyz";
    public static int backend = ConfigManager.getIntOrDefault(Defines.wsBuiltInProxyBackend, 0); // 0 -> Nekogram; 1 -> Nekogram X
    public static AtomicReference<String> serverHost = new AtomicReference<>(ConfigManager.getStringOrDefault(Defines.wsServerHost, NekogramPublicProxyServer));

    private static int socksPort = -1;
    private static boolean tcp2wsStarted = false;
    private static tcp2wsServer tcp2wsServer;
    public static boolean wsEnableTLS = ConfigManager.getBooleanOrDefault(Defines.wsEnableTLS, true);
    public static boolean wsUseMTP = ConfigManager.getBooleanOrDefault(Defines.wsUseMTP, false);
    public static boolean wsUseDoH = ConfigManager.getBooleanOrDefault(Defines.wsUseDoH, true);

    public static void toggleWsEnableTLS() {
        ConfigManager.putBoolean(Defines.wsEnableTLS, !ConfigManager.getBooleanOrDefault(Defines.wsEnableTLS, true));
        wsEnableTLS = ConfigManager.getBooleanOrDefault(Defines.wsEnableTLS, true);
    }

    public static void toggleWsUseDoH() {
        ConfigManager.putBoolean(Defines.wsUseDoH, !ConfigManager.getBooleanOrDefault(Defines.wsUseDoH, true));
        wsUseDoH = ConfigManager.getBooleanOrDefault(Defines.wsUseDoH, true);
    }

    public static void setWsUseMTP(boolean value) {
        ConfigManager.putBoolean(Defines.wsUseMTP, value);
        wsUseMTP = ConfigManager.getBooleanOrDefault(Defines.wsUseMTP, true);
    }

    public static void setBackend(int targetBackend) {
        Log.d("setBackend:" + targetBackend);
        ConfigManager.putInt(Defines.wsBuiltInProxyBackend, targetBackend);
        backend = targetBackend;
        setServerHost(targetBackend == 0 ? NekogramPublicProxyServer : NekogramXPublicProxyServer);
        SharedConfig.reloadProxyList(true);
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.proxySettingsChanged);

    }

    public static void setServerHost(String targetServerHost) {
        Log.d("set server host:" + targetServerHost);
        ConfigManager.putString(Defines.wsServerHost, targetServerHost);
        serverHost.set(targetServerHost);
        wsReloadConfig();
    }

    public static int getSocksPort() {
        return getSocksPort(6356);
    }

    public static void wsReloadConfig() {
        Log.d("ws reload config");
        if (tcp2wsServer != null) {
            try {
                tcp2wsServer.setTls(false).setIfMTP(wsUseMTP).setIfDoH(wsUseDoH);
            } catch (Exception e) {
                Log.e(e);
            }
        }
    }

    public static int getSocksPort(int port) {
        if (tcp2wsStarted && socksPort != -1) {
            return socksPort;
        }
        try {
            if (port != -1) {
                socksPort = port;
            } else {
                ServerSocket socket = new ServerSocket(0);
                socksPort = socket.getLocalPort();
                socket.close();
            }
            if (!tcp2wsStarted) {
                tcp2wsServer = new tcp2wsServer()
                    .setTgaMode(false)
                    .setTls(false)
                    .setIfMTP(wsUseMTP)
                    .setIfDoH(wsUseDoH);
                tcp2wsServer.start(socksPort);
                tcp2wsStarted = true;
                var map = new HashMap<String, String>();
                map.put("buildType", BuildConfig.BUILD_TYPE);
                map.put("buildFlavor", BuildConfig.FLAVOR);
                map.put("isPlay", String.valueOf(BuildConfig.isPlay));
                AppcenterUtils.trackEvent("tcp2ws started", map);
            }
            Log.d("tcp2ws started on port " + socksPort);
            Log.d("serverHost: " + serverHost);
            return socksPort;
        } catch (Exception e) {
            FileLog.e(e);
            if (port != -1) {
                return getSocksPort(-1);
            } else {
                return -1;
            }
        }
    }
}
