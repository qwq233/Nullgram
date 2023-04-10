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
import java.util.HashSet;
import java.util.Iterator;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@SuppressWarnings("SynchronizeOnNonFinalField")
public class ProxyHandler implements Runnable {

    private InputStream m_ClientInput = null;
    private OutputStream m_ClientOutput = null;

    private Object m_lock;

    Socket m_ClientSocket;
    WebSocket m_ServerSocket = null;

    byte[] m_Buffer = new byte[SocksConstants.DEFAULT_BUF_SIZE];
    final static byte[] emptyBytes = new byte[8];
    String server;

    Cipher outgoingDecryptCipher;

    boolean isHandshake = false;

    public ProxyHandler(Socket clientSocket) {
        m_lock = this;
        m_ClientSocket = clientSocket;
        try {
            m_ClientSocket.setSoTimeout(SocksConstants.DEFAULT_PROXY_TIMEOUT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void setLock(Object lock) {
        this.m_lock = lock;
    }

    public void run() {
        setLock(this);

        if (prepareClient()) {
            processRelay();
            close();
        }
    }

    public void close() {
        try {
            if (m_ClientOutput != null) {
                m_ClientOutput.flush();
                m_ClientOutput.close();
            }
        } catch (IOException e) {
            // ignore
        }

        try {
            if (m_ClientSocket != null) {
                m_ClientSocket.close();
            }
        } catch (IOException e) {
            // ignore
        }

        if (m_ServerSocket != null && m_ServerSocket.isOpen()) {
            HashSet<WebSocket> set = tcp2wsServer.inactiveWs.get(server);
            if (set != null) {
                set.add(m_ServerSocket);
            }
        }

        m_ServerSocket = null;
        m_ClientSocket = null;
    }

    public void sendToClient(byte[] buffer) {
        sendToClient(buffer, buffer.length);
    }

    public void sendToClient(byte[] buffer, int len) {
        if (m_ClientOutput != null && len > 0 && len <= buffer.length) {
            try {
                m_ClientOutput.write(buffer, 0, len);
                m_ClientOutput.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void connectToServer(String server) throws IOException {

        if (server.equals("")) {
            close();
            return;
        }
        this.server = server;
        prepareServer();
    }

    protected void prepareServer() throws IOException {
        synchronized (m_lock) {
            HashSet<WebSocket> set = tcp2wsServer.inactiveWs.get(server);
            if (set != null) {
                Iterator<WebSocket> iterator = set.iterator();
                while (iterator.hasNext()) {
                    WebSocket _m_ServerSocket = iterator.next();
                    if (_m_ServerSocket.isOpen()) {
                        m_ServerSocket = _m_ServerSocket;
                        return;
                    } else {
                        iterator.remove();
                        _m_ServerSocket.sendClose();
                    }
                }
            }
            int count_520 = 0;
            while (count_520 < 10) {
                try {
                    m_ServerSocket = new WebSocketFactory()
                        .setConnectionTimeout(5000)
                        .createSocket((tcp2wsServer.tls ? "wss://" : "ws://") + server + "/api")
                        .addListener(new WebSocketAdapter() {
                            public void onBinaryMessage(WebSocket websocket, byte[] binary) {
                                sendToClient(binary);
                            }

                            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
                                if (closedByServer) {
                                    System.out.println(server + "," + clientCloseFrame.getCloseCode() + clientCloseFrame.getCloseReason());
                                    m_ServerSocket.sendClose();
                                    close();
                                }
                            }
                        })
                        .addExtension("permessage-deflate")
                        .addProtocol("binary")
                        .addHeader("User-Agent", tcp2wsServer.userAgent)
                        .addHeader("Conn-Hash", tcp2wsServer.connHash)
                        .connect();
                    break;
                } catch (WebSocketException e) {
                    if (e.getMessage().contains("520"))
                        count_520++;
                    else {
                        System.out.println(server);
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
    }

    public boolean prepareClient() {
        if (m_ClientSocket == null) return false;

        try {
            m_ClientInput = m_ClientSocket.getInputStream();
            m_ClientOutput = m_ClientSocket.getOutputStream();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void processRelay() {
        try {
            byte SOCKS_Version = getByteFromClient();

            Socks4Impl comm;
            switch (SOCKS_Version) {
                case SocksConstants.SOCKS4_Version:
                    comm = new Socks4Impl(this);
                    break;
                case SocksConstants.SOCKS5_Version:
                    comm = new Socks5Impl(this);
                    break;
                default:
                    return;
            }

            comm.authenticate(SOCKS_Version);
            comm.getClientCommand();
            switch (comm.socksCommand) {
                case SocksConstants.SC_CONNECT:
                    comm.connect();
                    processHandshake();
                    relay();
                    break;

                case SocksConstants.SC_BIND:
                    //comm.bind();
                    relay();
                    break;

                case SocksConstants.SC_UDP:
                    comm.udp();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte getByteFromClient() throws Exception {
        while (m_ClientSocket != null) {
            int b;
            try {
                b = m_ClientInput.read();
            } catch (InterruptedIOException e) {
                Thread.yield();
                continue;
            }
            return (byte) b; // return loaded byte
        }
        throw new Exception("Interrupted Reading GetByteFromClient()");
    }

    public void relay() {
        if (m_ServerSocket == null) return;

        boolean isActive = true;

        while (isActive) {

            //---> Check for client data <---

            int dlen = checkClientData();

            if (dlen < 0) {
                isActive = false;
            }
            if (dlen > 0) {
                m_ServerSocket.sendBinary(Arrays.copyOf(m_Buffer, dlen));
            }

            Thread.yield();
        }
    }

    private void processHandshake() {
        byte[] buffer = new byte[]{};
        for (int dlen = 0, _dlen; dlen < 105; dlen += _dlen) {
            _dlen = checkClientData();
            if (_dlen < 0) return;
            buffer = Utils.concat(buffer, Arrays.copyOf(m_Buffer, _dlen));
        }

        byte[] decrypted = new byte[]{};

        try {
            outgoingDecryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
            outgoingDecryptCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(Arrays.copyOfRange(buffer, 8, 40), "AES"), new IvParameterSpec(Arrays.copyOfRange(buffer, 40, 56)));
            decrypted = outgoingDecryptCipher.update(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        isHandshake = Arrays.equals(Arrays.copyOfRange(decrypted, 65, 73), emptyBytes);
        m_ServerSocket.sendBinary(buffer);
        Thread.yield();
    }

    public int checkClientData() {
        synchronized (m_lock) {
            //	The client side is not opened.
            if (m_ClientInput == null) return -1;

            int dlen;

            try {
                dlen = m_ClientInput.read(m_Buffer, 0, SocksConstants.DEFAULT_BUF_SIZE);
            } catch (InterruptedIOException e) {
                return 0;
            } catch (IOException e) {
                if (!(e.getMessage().contains("Socket Closed") | e.getMessage().contains("socket closed") | e.getMessage().contains("Connection reset")))
                    e.printStackTrace();
                close();    //	Close the server on this exception
                return -1;
            }

            if (dlen < 0) close();

            return dlen;
        }
    }
}
