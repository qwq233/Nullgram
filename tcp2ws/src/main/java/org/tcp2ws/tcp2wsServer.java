package org.tcp2ws;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class tcp2wsServer {

    protected int port;
    protected boolean stopping = false;
    protected static boolean tls = false;
    protected static String userAgent = "tcp2ws/1.0.0";
    protected static String connHash = "";

    static Map<String, String> cdn = new HashMap<>();
    static Map<Integer, String> mtpcdn = new HashMap<>();

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
