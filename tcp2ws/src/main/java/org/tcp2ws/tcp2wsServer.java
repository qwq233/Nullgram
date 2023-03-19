package org.tcp2ws;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

/* loaded from: org.tcp2ws.jar:org/tcp2ws/tcp2wsServer.class */
public class tcp2wsServer {
    protected int port;
    protected boolean stopping = false;
    protected static boolean tls = false;
    protected static InetAddress[] fixedIps = null;
    protected static boolean ifDoH = false;
    protected static String DoHLink = "https://1.0.0.1/dns-query";
    protected static boolean ifMTP = true;
    protected static boolean isTgaMode = true;
    protected static boolean transToIpv6 = false;
    protected static boolean forceWs = false;
    private static ReentrantLock lock = new ReentrantLock();
    static Map<String, String> cdn = new HashMap();
    static Map<Integer, String> mtpcdn = new HashMap();
    static Map<String, String> ipv6 = new HashMap();
    static Map<Integer, String> mtpipv6 = new HashMap();
    public static Map<String, InetAddress[]> DoHCache = new HashMap();

    static {
        mtpcdn.put(1, "pluto.nekoe.eu.org");
        mtpcdn.put(2, "venus.nekoe.eu.org");
        mtpcdn.put(3, "aurora.nekoe.eu.org");
        mtpcdn.put(4, "vesta.nekoe.eu.org");
        mtpcdn.put(5, "flora.nekoe.eu.org");
        mtpcdn.put(17, "test_pluto.nekoe.eu.org");
        mtpcdn.put(18, "test_venus.nekoe.eu.org");
        mtpcdn.put(19, "test_aurora.nekoe.eu.org");

//media
        cdn.put("149.154.175.50", "pluto.nekoe.eu.org");
        cdn.put("149.154.167.51", "venus.nekoe.eu.org");
        cdn.put("95.161.76.100", "venus.nekoe.eu.org");
        cdn.put("149.154.175.100", "aurora.nekoe.eu.org");
        cdn.put("149.154.167.91", "vesta.nekoe.eu.org");
        cdn.put("149.154.171.5", "flora.nekoe.eu.org");

        try {
            cdn.put(InetAddress.getByName("2001:b28:f23d:f001:0000:0000:0000:000a").getHostAddress(), "pluto.nekoe.eu.org");
            cdn.put(InetAddress.getByName("2001:67c:4e8:f002:0000:0000:0000:000a").getHostAddress(), "venus.nekoe.eu.org");
            cdn.put(InetAddress.getByName("2001:b28:f23d:f003:0000:0000:0000:000a").getHostAddress(), "aurora.nekoe.eu.org");
            cdn.put(InetAddress.getByName("2001:67c:4e8:f004:0000:0000:0000:000a").getHostAddress(), "vesta.nekoe.eu.org");
            cdn.put(InetAddress.getByName("2001:b28:f23f:f005:0000:0000:0000:000a").getHostAddress(), "flora.nekoe.eu.org");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
//proxy
        cdn.put("149.154.175.5", "pluto.nekoe.eu.org");
        cdn.put("149.154.161.144", "venus.nekoe.eu.org");
        cdn.put("149.154.167.15", "venus.nekoe.eu.org");
        cdn.put("149.154.167.5", "venus.nekoe.eu.org");
        cdn.put("149.154.167.6", "venus.nekoe.eu.org");
        cdn.put("149.154.167.7", "venus.nekoe.eu.org");
        cdn.put("149.154.167.2", "venus.nekoe.eu.org");
        cdn.put("91.108.4.", "vesta.nekoe.eu.org");
        cdn.put("149.154.164.", "vesta.nekoe.eu.org");
        cdn.put("149.154.165.", "vesta.nekoe.eu.org");
        cdn.put("149.154.166.", "vesta.nekoe.eu.org");
        cdn.put("149.154.167.8", "vesta.nekoe.eu.org");
        cdn.put("149.154.167.9", "vesta.nekoe.eu.org");
        cdn.put("91.108.56.", "flora.nekoe.eu.org");
        cdn.put("111.62.91.", "venus.nekoe.eu.org");

        try {
            cdn.put(InetAddress.getByName("2001:b28:f23d:f001:0000:0000:0000:000d").getHostAddress(), "pluto.nekoe.eu.org");
            cdn.put(InetAddress.getByName("2001:67c:4e8:f002:0000:0000:0000:000d").getHostAddress(), "venus.nekoe.eu.org");
            cdn.put(InetAddress.getByName("2001:b28:f23d:f003:0000:0000:0000:000d").getHostAddress(), "aurora.nekoe.eu.org");
            cdn.put(InetAddress.getByName("2001:67c:4e8:f004:0000:0000:0000:000d").getHostAddress(), "vesta.nekoe.eu.org");
            cdn.put(InetAddress.getByName("2001:b28:f23f:f005:0000:0000:0000:000d").getHostAddress(), "flora.nekoe.eu.org");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

//test
        cdn.put("149.154.175.10", "test_pluto.nekoe.eu.org");
        cdn.put("149.154.175.40", "test_pluto.nekoe.eu.org");
        cdn.put("149.154.167.40", "test_venus.nekoe.eu.org");
        cdn.put("149.154.175.117", "test_aurora.nekoe.eu.org");

        try {
            cdn.put(InetAddress.getByName("2001:b28:f23d:f001:0000:0000:0000:000e").getHostAddress(), "test_pluto.nekoe.eu.org");
            cdn.put(InetAddress.getByName("2001:67c:4e8:f002:0000:0000:0000:000e").getHostAddress(), "test_venus.nekoe.eu.org");
            cdn.put(InetAddress.getByName("2001:b28:f23d:f003:0000:0000:0000:000e").getHostAddress(), "test_aurora.nekoe.eu.org");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
    }

    public tcp2wsServer setTls(boolean tls2) {
        tls = tls2;
        return this;
    }

    public tcp2wsServer setCdn(HashMap<String, String> cdn2) {
        cdn = cdn2;
        return this;
    }

    public tcp2wsServer setMtpCdn(HashMap<Integer, String> mtpcdn2) {
        mtpcdn = mtpcdn2;
        return this;
    }

    public tcp2wsServer setIpv6(HashMap<String, String> ipv62) {
        ipv6 = ipv62;
        return this;
    }

    public tcp2wsServer setMtpIpv6(HashMap<Integer, String> mtpipv62) {
        mtpipv6 = mtpipv62;
        return this;
    }

    public tcp2wsServer setForceWs(boolean forceWs2) {
        forceWs = forceWs2;
        return this;
    }

    public tcp2wsServer setFixedIps(String s) throws UnknownHostException {
        lock.lock();
        ifDoH = false;
        fixedIps = fixedIps;
        lock.unlock();
        return this;
    }

    public tcp2wsServer setIfDoH(boolean ifDoH2) {
        lock.lock();
        if (ifDoH2) {
            fixedIps = null;
        }
        ifDoH = ifDoH2;
        lock.unlock();
        return this;
    }

    public tcp2wsServer setDoHLink(String DoHLink2) {
        DoHLink = DoHLink2;
        return this;
    }

    public tcp2wsServer setIfMTP(boolean ifMTP2) {
        ifMTP = ifMTP2;
        return this;
    }

    public tcp2wsServer setTgaMode(boolean isTgaMode2) {
        isTgaMode = isTgaMode2;
        return this;
    }

    public tcp2wsServer setTransToIpv6(boolean transToIpv62) {
        transToIpv6 = transToIpv62;
        return this;
    }

    public static InetAddress[] getFixedIps() {
        return fixedIps;
    }

    public static boolean getIfDoH() {
        return ifDoH;
    }

    public static String getDoHLink() {
        return DoHLink;
    }

    public synchronized void start(int listenPort) {
        this.stopping = false;
        this.port = listenPort;
        new Thread(new ServerProcess()).start();
    }

    public synchronized void stop() {
        this.stopping = true;
    }

    /* loaded from: org.tcp2ws.jar:org/tcp2ws/tcp2wsServer$ServerProcess.class */
    private class ServerProcess implements Runnable {
        private ServerProcess() {
        }

        @Override // java.lang.Runnable
        public void run() {
            try {
                new Timer().schedule(new TimerTask() { // from class: org.tcp2ws.tcp2wsServer.ServerProcess.1
                    @Override // java.util.TimerTask, java.lang.Runnable
                    public void run() {
                        if (tcp2wsServer.ifDoH) {
                            tcp2wsServer.DoHCache = new HashMap();
                        }
                    }
                }, 0L, 300000L);
                handleClients(tcp2wsServer.this.port);
            } catch (IOException e) {
                Thread.currentThread().interrupt();
            }
        }

        protected void handleClients(int port) throws IOException {
            ServerSocket listenSocket = new ServerSocket(port);
            listenSocket.setSoTimeout(200);
            tcp2wsServer.this.port = listenSocket.getLocalPort();
            while (true) {
                synchronized (tcp2wsServer.this) {
                    if (tcp2wsServer.this.stopping) {
                        try {
                            listenSocket.close();
                            return;
                        } catch (IOException e) {
                            return;
                        }
                    }
                }
                handleNextClient(listenSocket);
            }
        }

        private void handleNextClient(ServerSocket listenSocket) {
            try {
                Socket clientSocket = listenSocket.accept();
                clientSocket.setSoTimeout(200);
                new Thread(new ProxyHandler(clientSocket)).start();
            } catch (Exception e) {
            }
        }
    }
}
