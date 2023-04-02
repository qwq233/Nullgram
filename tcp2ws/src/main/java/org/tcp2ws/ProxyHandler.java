//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.tcp2ws;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ProxyHandler implements Runnable {
    private InputStream m_ClientInput = null;
    private OutputStream m_ClientOutput = null;
    private Object m_lock = this;
    Socket m_ClientSocket;
    WebSocket m_ServerSocket = null;
    byte[] m_Buffer = new byte[40];
    static final byte[] emptyBytes = new byte[8];
    String server;
    Cipher outgoingDecryptCipher;
    boolean isHandshake = false;
    private static final Map<String, HashSet<WebSocket>> inactiveWs = new HashMap();

    public ProxyHandler(Socket clientSocket) {
        this.m_ClientSocket = clientSocket;

        try {
            this.m_ClientSocket.setSoTimeout(10);
        } catch (SocketException var3) {
            var3.printStackTrace();
        }

    }

    public void setLock(Object lock) {
        this.m_lock = lock;
    }

    public void run() {
        this.setLock(this);
        if (this.prepareClient()) {
            this.processRelay();
            this.close();
        }

    }

    public void close() {
        try {
            if (this.m_ClientOutput != null) {
                this.m_ClientOutput.flush();
                this.m_ClientOutput.close();
            }
        } catch (IOException var3) {
        }

        try {
            if (this.m_ClientSocket != null) {
                this.m_ClientSocket.close();
            }
        } catch (IOException var2) {
        }

        if (this.m_ServerSocket != null && this.m_ServerSocket.isOpen()) {
            ((HashSet)inactiveWs.get(this.server)).add(this.m_ServerSocket);
        }

        this.m_ServerSocket = null;
        this.m_ClientSocket = null;
    }

    public void sendToClient(byte[] buffer) {
        this.sendToClient(buffer, buffer.length);
    }

    public void sendToClient(byte[] buffer, int len) {
        if (this.m_ClientOutput != null && len > 0 && len <= buffer.length) {
            try {
                this.m_ClientOutput.write(buffer, 0, len);
                this.m_ClientOutput.flush();
            } catch (IOException var4) {
                var4.printStackTrace();
            }
        }

    }

    public void connectToServer(String server) throws IOException {
        if (server.equals("")) {
            this.close();
        } else {
            this.server = server;
            this.prepareServer();
        }
    }

    protected void prepareServer() throws IOException {
        synchronized(this.m_lock) {
            Iterator<WebSocket> iterator = ((HashSet)inactiveWs.get(this.server)).iterator();

            while(iterator.hasNext()) {
                WebSocket _m_ServerSocket = (WebSocket)iterator.next();
                if (_m_ServerSocket.isOpen()) {
                    this.m_ServerSocket = _m_ServerSocket;
                    return;
                }

                iterator.remove();
                _m_ServerSocket.sendClose();
            }

            int count_520 = 0;

            while(count_520 < 10) {
                try {
                    WebSocketFactory var10001 = (new WebSocketFactory()).setConnectionTimeout(5000);
                    String var10002 = tcp2wsServer.tls ? "wss://" : "ws://";
                    this.m_ServerSocket = var10001.createSocket(var10002 + this.server + "/api").addListener(new WebSocketAdapter() {
                        public void onBinaryMessage(WebSocket websocket, byte[] binary) {
                            ProxyHandler.this.sendToClient(binary);
                        }

                        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
                            if (closedByServer) {
                                String var10001 = ProxyHandler.this.server;
                                System.out.println(var10001 + "," + clientCloseFrame.getCloseCode() + clientCloseFrame.getCloseReason());
                                ProxyHandler.this.m_ServerSocket.sendClose();
                                ProxyHandler.this.close();
                            }

                        }
                    }).addExtension("permessage-deflate").addProtocol("binary").addHeader("User-Agent", tcp2wsServer.userAgent).addHeader("Conn-Hash", tcp2wsServer.connHash).connect();
                    break;
                } catch (WebSocketException var6) {
                    if (!var6.getMessage().contains("520")) {
                        System.out.println(this.server);
                        var6.printStackTrace();
                        break;
                    }

                    ++count_520;
                }
            }

        }
    }

    public boolean prepareClient() {
        if (this.m_ClientSocket == null) {
            return false;
        } else {
            try {
                this.m_ClientInput = this.m_ClientSocket.getInputStream();
                this.m_ClientOutput = this.m_ClientSocket.getOutputStream();
                return true;
            } catch (IOException var2) {
                return false;
            }
        }
    }

    public void processRelay() {
        try {
            byte SOCKS_Version = this.getByteFromClient();
            Object comm;
            switch (SOCKS_Version) {
                case 4:
                    comm = new Socks4Impl(this);
                    break;
                case 5:
                    comm = new Socks5Impl(this);
                    break;
                default:
                    return;
            }

            ((Socks4Impl)comm).authenticate(SOCKS_Version);
            ((Socks4Impl)comm).getClientCommand();
            switch (((Socks4Impl)comm).socksCommand) {
                case 1:
                    ((Socks4Impl)comm).connect();
                    this.processHandshake();
                    this.relay();
                    break;
                case 2:
                    this.relay();
                    break;
                case 3:
                    ((Socks4Impl)comm).udp();
            }
        } catch (Exception var3) {
            var3.printStackTrace();
        }

    }

    public byte getByteFromClient() throws Exception {
        while(true) {
            if (this.m_ClientSocket != null) {
                int b;
                try {
                    b = this.m_ClientInput.read();
                } catch (InterruptedIOException var3) {
                    Thread.yield();
                    continue;
                }

                return (byte)b;
            }

            throw new Exception("Interrupted Reading GetByteFromClient()");
        }
    }

    public void relay() {
        if (this.m_ServerSocket != null) {
            for(boolean isActive = true; isActive; Thread.yield()) {
                int dlen = this.checkClientData();
                if (dlen < 0) {
                    isActive = false;
                }

                if (dlen > 0) {
                    this.m_ServerSocket.sendBinary(Arrays.copyOf(this.m_Buffer, dlen));
                }
            }

        }
    }

    private void processHandshake() {
        byte[] buffer = new byte[0];

        int _dlen;
        for(int dlen = 0; dlen < 105; dlen += _dlen) {
            _dlen = this.checkClientData();
            if (_dlen < 0) {
                return;
            }

            buffer = Utils.concat(buffer, Arrays.copyOf(this.m_Buffer, _dlen));
        }

        byte[] decrypted = new byte[0];

        try {
            this.outgoingDecryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
            this.outgoingDecryptCipher.init(2, new SecretKeySpec(Arrays.copyOfRange(buffer, 8, 40), "AES"), new IvParameterSpec(Arrays.copyOfRange(buffer, 40, 56)));
            decrypted = this.outgoingDecryptCipher.update(buffer);
        } catch (Exception var4) {
            var4.printStackTrace();
        }

        this.isHandshake = Arrays.equals(Arrays.copyOfRange(decrypted, 65, 73), emptyBytes);
        this.m_ServerSocket.sendBinary(buffer);
        Thread.yield();
    }

    public int checkClientData() {
        synchronized(this.m_lock) {
            if (this.m_ClientInput == null) {
                return -1;
            } else {
                int dlen;
                try {
                    dlen = this.m_ClientInput.read(this.m_Buffer, 0, 40);
                } catch (InterruptedIOException var5) {
                    return 0;
                } catch (IOException var6) {
                    if (!(var6.getMessage().contains("Socket Closed") | var6.getMessage().contains("socket closed") | var6.getMessage().contains("Connection reset"))) {
                        var6.printStackTrace();
                    }

                    this.close();
                    return -1;
                }

                if (dlen < 0) {
                    this.close();
                }

                return dlen;
            }
        }
    }

    static {
        inactiveWs.put((String)tcp2wsServer.mtpcdn.get(1), new HashSet());
        inactiveWs.put((String)tcp2wsServer.mtpcdn.get(2), new HashSet());
        inactiveWs.put((String)tcp2wsServer.mtpcdn.get(3), new HashSet());
        inactiveWs.put((String)tcp2wsServer.mtpcdn.get(4), new HashSet());
        inactiveWs.put((String)tcp2wsServer.mtpcdn.get(5), new HashSet());
        inactiveWs.put((String)tcp2wsServer.mtpcdn.get(17), new HashSet());
        inactiveWs.put((String)tcp2wsServer.mtpcdn.get(18), new HashSet());
        inactiveWs.put((String)tcp2wsServer.mtpcdn.get(19), new HashSet());
    }
}
