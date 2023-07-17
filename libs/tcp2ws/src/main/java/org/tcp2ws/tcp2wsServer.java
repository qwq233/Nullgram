package org.tcp2ws;

import com.neovisionaries.ws.client.WebSocket;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class tcp2wsServer {

    protected int port;
    protected boolean stopping = false;
    protected static boolean tls = false;
    protected static String userAgent = "tcp2ws/1.0.0";
    protected static String connHash = "";

    static Map<String, String> cdn = new HashMap<>();
    static Map<Integer, String> mtpcdn = new HashMap<>();
    static final Map<String, HashSet<WebSocket>> inactiveWs = new HashMap<>();

    public tcp2wsServer setCdnDomain(String domain) {
        mtpcdn.put(1, "pluto." + domain);
        mtpcdn.put(2, "venus." + domain);
        mtpcdn.put(3, "aurora." + domain);
        mtpcdn.put(4, "vesta." + domain);
        mtpcdn.put(5, "flora." + domain);
        mtpcdn.put(17, "test_pluto." + domain);
        mtpcdn.put(18, "test_venus." + domain);
        mtpcdn.put(19, "test_aurora." + domain);

//media
        cdn.put("149.154.175.50", "pluto." + domain);
        cdn.put("149.154.167.51", "venus." + domain);
        cdn.put("95.161.76.100", "venus." + domain);
        cdn.put("149.154.175.100", "aurora." + domain);
        cdn.put("149.154.167.91", "vesta." + domain);
        cdn.put("149.154.171.5", "flora." + domain);

        try {
            cdn.put(InetAddress.getByName("2001:b28:f23d:f001:0000:0000:0000:000a").getHostAddress(), "pluto." + domain);
            cdn.put(InetAddress.getByName("2001:67c:4e8:f002:0000:0000:0000:000a").getHostAddress(), "venus." + domain);
            cdn.put(InetAddress.getByName("2001:b28:f23d:f003:0000:0000:0000:000a").getHostAddress(), "aurora." + domain);
            cdn.put(InetAddress.getByName("2001:67c:4e8:f004:0000:0000:0000:000a").getHostAddress(), "vesta." + domain);
            cdn.put(InetAddress.getByName("2001:b28:f23f:f005:0000:0000:0000:000a").getHostAddress(), "flora." + domain);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
//proxy
        cdn.put("149.154.175.5", "pluto." + domain);
        cdn.put("149.154.161.144", "venus." + domain);
        cdn.put("149.154.167.15", "venus." + domain);
        cdn.put("149.154.167.5", "venus." + domain);
        cdn.put("149.154.167.6", "venus." + domain);
        cdn.put("149.154.167.7", "venus." + domain);
        cdn.put("149.154.167.2", "venus." + domain);
        cdn.put("91.108.4.", "vesta." + domain);
        cdn.put("149.154.164.", "vesta." + domain);
        cdn.put("149.154.165.", "vesta." + domain);
        cdn.put("149.154.166.", "vesta." + domain);
        cdn.put("149.154.167.8", "vesta." + domain);
        cdn.put("149.154.167.9", "vesta." + domain);
        cdn.put("91.108.56.", "flora." + domain);
        cdn.put("111.62.91.", "venus." + domain);

        try {
            cdn.put(InetAddress.getByName("2001:b28:f23d:f001:0000:0000:0000:000d").getHostAddress(), "pluto." + domain);
            cdn.put(InetAddress.getByName("2001:67c:4e8:f002:0000:0000:0000:000d").getHostAddress(), "venus." + domain);
            cdn.put(InetAddress.getByName("2001:b28:f23d:f003:0000:0000:0000:000d").getHostAddress(), "aurora." + domain);
            cdn.put(InetAddress.getByName("2001:67c:4e8:f004:0000:0000:0000:000d").getHostAddress(), "vesta." + domain);
            cdn.put(InetAddress.getByName("2001:b28:f23f:f005:0000:0000:0000:000d").getHostAddress(), "flora." + domain);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

//test
        cdn.put("149.154.175.10", "test_pluto." + domain);
        cdn.put("149.154.175.40", "test_pluto." + domain);
        cdn.put("149.154.167.40", "test_venus." + domain);
        cdn.put("149.154.175.117", "test_aurora." + domain);

        try {
            cdn.put(InetAddress.getByName("2001:b28:f23d:f001:0000:0000:0000:000e").getHostAddress(), "test_pluto." + domain);
            cdn.put(InetAddress.getByName("2001:67c:4e8:f002:0000:0000:0000:000e").getHostAddress(), "test_venus." + domain);
            cdn.put(InetAddress.getByName("2001:b28:f23d:f003:0000:0000:0000:000e").getHostAddress(), "test_aurora." + domain);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        inactiveWs.put(tcp2wsServer.mtpcdn.get(1), new HashSet<>());
        inactiveWs.put(tcp2wsServer.mtpcdn.get(2), new HashSet<>());
        inactiveWs.put(tcp2wsServer.mtpcdn.get(3), new HashSet<>());
        inactiveWs.put(tcp2wsServer.mtpcdn.get(4), new HashSet<>());
        inactiveWs.put(tcp2wsServer.mtpcdn.get(5), new HashSet<>());
        inactiveWs.put(tcp2wsServer.mtpcdn.get(17), new HashSet<>());
        inactiveWs.put(tcp2wsServer.mtpcdn.get(18), new HashSet<>());
        inactiveWs.put(tcp2wsServer.mtpcdn.get(19), new HashSet<>());

        return this;
    }

    public tcp2wsServer setUserAgent(String userAgent) {
        tcp2wsServer.userAgent = userAgent;
        return this;
    }

    public tcp2wsServer setConnHash(String connHash) {
        tcp2wsServer.connHash = connHash;
        return this;
    }

    public static void main(String[] args) {

    }

    public tcp2wsServer setTls(boolean tls) {
        tcp2wsServer.tls = tls;
        return this;
    }

    public synchronized void start(int listenPort) {
        if (cdn.isEmpty()) {
            throw new RuntimeException("cdn domain not set");
        }
        this.stopping = false;
        this.port = listenPort;
        new Thread(new ServerProcess()).start();
    }

    public synchronized void stop() {
        stopping = true;
    }

    private class ServerProcess implements Runnable {

        @Override
        public void run() {
            try {
                handleClients(port);
            } catch (IOException e) {
                Thread.currentThread().interrupt();
            }
        }

        protected void handleClients(int port) throws IOException {
            final ServerSocket listenSocket = new ServerSocket(port);
            listenSocket.setSoTimeout(SocksConstants.LISTEN_TIMEOUT);
            tcp2wsServer.this.port = listenSocket.getLocalPort();

            while (true) {
                synchronized (tcp2wsServer.this) {
                    if (stopping) {
                        break;
                    }
                }
                handleNextClient(listenSocket);
            }

            try {
                listenSocket.close();
            } catch (IOException e) {
                // ignore
            }
        }

        private void handleNextClient(ServerSocket listenSocket) {
            try {
                final Socket clientSocket = listenSocket.accept();
                clientSocket.setSoTimeout(SocksConstants.DEFAULT_SERVER_TIMEOUT);
                new Thread(new ProxyHandler(clientSocket)).start();
            } catch (InterruptedIOException e) {
                //	This exception is thrown when accept timeout is expired
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
