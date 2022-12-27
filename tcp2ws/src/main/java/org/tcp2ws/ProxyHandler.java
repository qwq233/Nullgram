package org.tcp2ws;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketCloseCode;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/* loaded from: org.tcp2ws.jar:org/tcp2ws/ProxyHandler.class */
public class ProxyHandler implements Runnable {
    Socket m_ClientSocket;
    static final byte[] secret = new byte[16];
    static final byte[] emptyBytes = new byte[8];
    volatile int DC;
    Cipher incomingEncryptCipher;
    Cipher incomingDecryptCipher;
    Cipher outgoingEncryptCipher;
    Cipher outgoingDecryptCipher;
    private InputStream m_ClientInput = null;
    private OutputStream m_ClientOutput = null;
    private InputStream m_ipv6ServerInput = null;
    private OutputStream m_ipv6ServerOutput = null;
    private Socks4Impl comm = null;
    WebSocket m_ServerSocket = null;
    Socket m_ipv6ServerSocket = null;
    byte[] m_Buffer = new byte[40];
    boolean isHandshake = false;
    private Object m_lock = this;

    public ProxyHandler(Socket clientSocket) {
        this.m_ClientSocket = clientSocket;
        try {
            this.m_ClientSocket.setSoTimeout(10);
        } catch (SocketException ignored) {
        }
    }

    public void setLock(Object lock) {
        this.m_lock = lock;
    }

    @Override // java.lang.Runnable
    public void run() {
        setLock(this);
        if (prepareClient()) {
            processRelay();
            close();
        }
    }

    public void close() {
        try {
            if (this.m_ClientOutput != null) {
                this.m_ClientOutput.flush();
                this.m_ClientOutput.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (this.m_ClientSocket != null) {
                this.m_ClientSocket.close();
            }
        } catch (IOException ignored) {
        }
        if (this.m_ServerSocket != null && this.m_ServerSocket.isOpen()) {
            this.m_ServerSocket.disconnect(WebSocketCloseCode.NORMAL);
        }
        this.m_ServerSocket = null;
        this.m_ClientSocket = null;
    }

    public void sendToClient(byte[] buffer) {
        sendToClient(buffer, buffer.length);
    }

    public void sendToClient(byte[] buffer, int len) {
        if (this.m_ClientOutput != null && len > 0 && len <= buffer.length) {
            try {
                this.m_ClientOutput.write(buffer, 0, len);
                this.m_ClientOutput.flush();
            } catch (IOException e) {
            }
        }
    }

    public void sendToServer(byte[] buffer, int len) {
        if (this.m_ipv6ServerOutput != null && len > 0 && len <= buffer.length) {
            try {
                this.m_ipv6ServerOutput.write(buffer, 0, len);
                this.m_ipv6ServerOutput.flush();
            } catch (IOException ignored) {
            }
        }
    }

    public void connectToServer(String server) throws IOException {
        if (server.equals("")) {
            close();
        } else {
            prepareServer(server);
        }
    }

    protected void prepareServer(final String server) throws IOException {
        synchronized (this.m_lock) {
            if (tcp2wsServer.transToIpv6 && !tcp2wsServer.forceWs) {
                this.m_ipv6ServerSocket = new Socket(server, 443);
                this.m_ipv6ServerSocket.setSoTimeout(10);
                this.m_ipv6ServerInput = this.m_ipv6ServerSocket.getInputStream();
                this.m_ipv6ServerOutput = this.m_ipv6ServerSocket.getOutputStream();
            } else {
                int count_520 = 0;
                while (true) {
                    if (count_520 >= 10) {
                        break;
                    }
                    try {
                        this.m_ServerSocket = new WebSocketFactory().setConnectionTimeout(5000).createSocket(((!tcp2wsServer.tls || tcp2wsServer.forceWs) ? "ws://" : "wss://") + server + "/api").addListener(new WebSocketAdapter() { // from class: org.tcp2ws.ProxyHandler.1
                            @Override // com.neovisionaries.ws.client.WebSocketAdapter, com.neovisionaries.ws.client.WebSocketListener
                            public void onBinaryMessage(WebSocket websocket, byte[] binary) {
                                ProxyHandler.this.sendToClient(tcp2wsServer.ifMTP ? ProxyHandler.this.incomingEncryptCipher.update(ProxyHandler.this.incomingDecryptCipher.update(binary)) : binary);
                            }

                            @Override // com.neovisionaries.ws.client.WebSocketAdapter, com.neovisionaries.ws.client.WebSocketListener
                            public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws IOException {
                                if (closedByServer) {
                                    System.out.println(server + "," + clientCloseFrame.getCloseCode() + clientCloseFrame.getCloseReason());
                                    ProxyHandler.this.close();
                                }
                            }
                        }).addExtension(WebSocketExtension.PERMESSAGE_DEFLATE).addProtocol("binary").connect();
                        break;
                    } catch (WebSocketException e) {
                        if (e.getMessage().contains("520")) {
                            count_520++;
                        } else {
                            System.out.println(server);
                            e.printStackTrace();
                            break;
                        }
                    }
                }
            }
        }
    }

    public boolean prepareClient() {
        if (this.m_ClientSocket == null) {
            return false;
        }
        try {
            this.m_ClientInput = this.m_ClientSocket.getInputStream();
            this.m_ClientOutput = this.m_ClientSocket.getOutputStream();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void processRelay() {
        try {
            if (tcp2wsServer.ifMTP) {
                processHandshake();
                relay();
            } else {
                byte SOCKS_Version = getByteFromClient();
                switch (SOCKS_Version) {
                    case SocksConstants.SOCKS4_Version /* 4 */:
                        this.comm = new Socks4Impl(this);
                        break;
                    case SocksConstants.SOCKS5_Version /* 5 */:
                        this.comm = new Socks5Impl(this);
                        break;
                    default:
                        return;
                }
                this.comm.authenticate(SOCKS_Version);
                this.comm.getClientCommand();
                switch (this.comm.socksCommand) {
                    case 1:
                        this.comm.connect();
                        processHandshake();
                        relay();
                        break;
                    case SocksConstants.SC_UDP /* 3 */:
                        this.comm.udp();
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte getByteFromClient() throws Exception {
        while (this.m_ClientSocket != null) {
            try {
                int b = this.m_ClientInput.read();
                return (byte) b;
            } catch (InterruptedIOException e) {
                Thread.yield();
            }
        }
        throw new Exception("Interrupted Reading GetByteFromClient()");
    }

    public void relay() {
        boolean isActive = true;
        while (isActive) {
            int dlen = checkClientData();
            if (dlen < 0) {
                isActive = false;
            }
            if (dlen > 0) {
                isActive = true;
                if (tcp2wsServer.transToIpv6 && !tcp2wsServer.forceWs) {
                    sendToServer(this.m_Buffer, dlen);
                } else {
                    this.m_ServerSocket.sendBinary((this.isHandshake && tcp2wsServer.isTgaMode) ? this.outgoingEncryptCipher.update(this.outgoingDecryptCipher.update(Arrays.copyOf(this.m_Buffer, dlen))) : Arrays.copyOf(this.m_Buffer, dlen));
                }
            }
            if (tcp2wsServer.transToIpv6 && !tcp2wsServer.forceWs) {
                int dlen2 = checkServerData();
                if (dlen2 < 0) {
                    isActive = false;
                }
                if (dlen2 > 0) {
                    isActive = true;
                    sendToClient(tcp2wsServer.ifMTP ? this.incomingEncryptCipher.update(this.incomingDecryptCipher.update(Arrays.copyOf(this.m_Buffer, dlen2))) : this.m_Buffer, dlen2);
                }
            }
            Thread.yield();
        }
    }

    private void processHandshake() {
        byte[] decrypted;
        int _dlen;
        byte[] buffer = new byte[0];
        int dlen = 0;
        while (dlen < 105 && (_dlen = checkClientData()) >= 0) {
            buffer = Utils.concat(buffer, Arrays.copyOf(this.m_Buffer, _dlen));
            dlen += _dlen;
        }
        byte[] decrypted2 = new byte[0];
        byte[] bArr = new byte[0];
        try {
            if (tcp2wsServer.isTgaMode) {
                this.outgoingEncryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
            }
            this.outgoingDecryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
            if (tcp2wsServer.ifMTP) {
                byte[] reversedBuffer = Utils.reverse(Arrays.copyOf(buffer, 64));
                this.incomingEncryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
                this.incomingEncryptCipher.init(1, new SecretKeySpec(Utils.SHA256(Utils.concat(Arrays.copyOfRange(reversedBuffer, 8, 40), secret)), "AES"), new IvParameterSpec(Arrays.copyOfRange(reversedBuffer, 40, 56)));
                byte[] outgoingKey = Utils.SHA256(Utils.concat(Arrays.copyOfRange(buffer, 8, 40), secret));
                if (tcp2wsServer.isTgaMode) {
                    this.outgoingEncryptCipher.init(1, new SecretKeySpec(outgoingKey, "AES"), new IvParameterSpec(Arrays.copyOfRange(buffer, 40, 56)));
                }
                this.outgoingDecryptCipher.init(2, new SecretKeySpec(outgoingKey, "AES"), new IvParameterSpec(Arrays.copyOfRange(buffer, 40, 56)));
                decrypted2 = this.outgoingDecryptCipher.update(buffer);
                this.DC = Math.abs((int) decrypted2[60]);
                MTProxyImpl mtpComm = new MTProxyImpl(this);
                mtpComm.mtpConnect();
                System.arraycopy(outgoingKey, 0, buffer, 8, 32);
                byte[] reversedBuffer2 = Utils.reverse(Arrays.copyOf(buffer, 64));
                this.incomingDecryptCipher = Cipher.getInstance("AES/CTR/NoPadding");
                this.incomingDecryptCipher.init(2, new SecretKeySpec(Arrays.copyOfRange(reversedBuffer2, 8, 40), "AES"), new IvParameterSpec(Arrays.copyOfRange(reversedBuffer2, 40, 56)));
            } else {
                this.outgoingDecryptCipher.init(2, new SecretKeySpec(Arrays.copyOfRange(buffer, 8, 40), "AES"), new IvParameterSpec(Arrays.copyOfRange(buffer, 40, 56)));
                if (tcp2wsServer.isTgaMode) {
                    this.outgoingEncryptCipher.init(1, new SecretKeySpec(Arrays.copyOfRange(buffer, 8, 40), "AES"), new IvParameterSpec(Arrays.copyOfRange(buffer, 40, 56)));
                }
                decrypted2 = this.outgoingDecryptCipher.update(buffer);
            }
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e2) {
            e2.printStackTrace();
        } catch (NoSuchAlgorithmException e3) {
            e3.printStackTrace();
        } catch (NoSuchPaddingException e4) {
            e4.printStackTrace();
        } catch (Exception e5) {
            e5.printStackTrace();
        }
        this.isHandshake = Arrays.equals(Arrays.copyOfRange(decrypted2, 65, 73), emptyBytes);
        if (tcp2wsServer.isTgaMode && this.isHandshake && (!tcp2wsServer.transToIpv6 || tcp2wsServer.forceWs)) {
            this.outgoingEncryptCipher.update(decrypted2);
            if (!tcp2wsServer.transToIpv6 || tcp2wsServer.forceWs) {
                this.m_ServerSocket.sendBinary(buffer);
            } else {
                sendToServer(buffer, buffer.length);
            }
            do {
                if (tcp2wsServer.forceWs) {
                    Thread.yield();
                    byte[] buffer2 = new byte[0];
                    int dlen2 = 0;
                    while (dlen2 < 85) {
                        int _dlen2 = checkServerData();
                        buffer2 = Utils.concat(buffer2, Arrays.copyOf(this.m_Buffer, _dlen2));
                        dlen2 += _dlen2;
                    }
                }
                Thread.yield();
                byte[] buffer3 = new byte[0];
                int dlen3 = 0;
                while (dlen3 < 382) {
                    int _dlen3 = checkClientData();
                    buffer3 = Utils.concat(buffer3, Arrays.copyOf(this.m_Buffer, _dlen3));
                    dlen3 += _dlen3;
                }
                if (!tcp2wsServer.transToIpv6 || tcp2wsServer.forceWs) {
                    this.m_ServerSocket.sendBinary(deleteMsgsAck(buffer3));
                } else {
                    sendToServer(deleteMsgsAck(buffer3), buffer3.length - 41);
                }
                if (tcp2wsServer.forceWs) {
                    Thread.yield();
                    byte[] buffer4 = new byte[0];
                    int dlen4 = 0;
                    while (dlen4 < 656) {
                        int _dlen4 = checkServerData();
                        buffer4 = Utils.concat(buffer4, Arrays.copyOf(this.m_Buffer, _dlen4));
                        dlen4 += _dlen4;
                    }
                }
                Thread.yield();
                byte[] buffer5 = new byte[0];
                int dlen5 = 0;
                while (dlen5 < 438) {
                    int _dlen5 = checkClientData();
                    buffer5 = Utils.concat(buffer5, Arrays.copyOf(this.m_Buffer, _dlen5));
                    dlen5 += _dlen5;
                }
                if (!tcp2wsServer.transToIpv6 || tcp2wsServer.forceWs) {
                    this.m_ServerSocket.sendBinary(deleteMsgsAck(buffer5));
                } else {
                    sendToServer(deleteMsgsAck(buffer5), buffer5.length - 41);
                }
                if (tcp2wsServer.forceWs) {
                    Thread.yield();
                    byte[] buffer6 = new byte[0];
                    int dlen6 = 0;
                    while (dlen6 < 73) {
                        int _dlen6 = checkServerData();
                        buffer6 = Utils.concat(buffer6, Arrays.copyOf(this.m_Buffer, _dlen6));
                        dlen6 += _dlen6;
                    }
                }
                Thread.yield();
                byte[] buffer7 = new byte[0];
                int dlen7 = 0;
                while (dlen7 < 82) {
                    int _dlen7 = checkClientData();
                    buffer7 = Utils.concat(buffer7, Arrays.copyOf(this.m_Buffer, _dlen7));
                    dlen7 += _dlen7;
                }
                decrypted = this.outgoingDecryptCipher.update(buffer7);
                if (!tcp2wsServer.transToIpv6 || tcp2wsServer.forceWs) {
                    this.m_ServerSocket.sendBinary(this.outgoingEncryptCipher.update(Arrays.copyOfRange(decrypted, 41, buffer7.length)));
                } else {
                    sendToServer(this.outgoingEncryptCipher.update(Arrays.copyOfRange(decrypted, 41, buffer7.length)), buffer7.length - 41);
                }
            } while (Arrays.equals(Arrays.copyOfRange(decrypted, 42, 50), emptyBytes));
        } else if (!tcp2wsServer.transToIpv6 || tcp2wsServer.forceWs) {
            this.m_ServerSocket.sendBinary(buffer);
        } else {
            sendToServer(buffer, buffer.length);
        }
        Thread.yield();
    }

    private byte[] deleteMsgsAck(byte[] buffer) {
        byte[] decrypted = this.outgoingDecryptCipher.update(buffer);
        byte[] encrypted = this.outgoingEncryptCipher.update(Arrays.copyOfRange(decrypted, 41, decrypted.length));
        return encrypted;
    }

    public int checkClientData() {
        synchronized (this.m_lock) {
            if (this.m_ClientInput == null) {
                return -1;
            }
            try {
                int dlen = this.m_ClientInput.read(this.m_Buffer, 0, 40);
                if (dlen < 0) {
                    close();
                }
                return dlen;
            } catch (InterruptedIOException e) {
                return 0;
            } catch (IOException e2) {
                if (!(e2.getMessage().contains("Socket Closed") | e2.getMessage().contains("socket closed") | e2.getMessage().contains("Connection reset"))) {
                    e2.printStackTrace();
                }
                close();
                return -1;
            }
        }
    }

    public int checkServerData() {
        synchronized (this.m_lock) {
            if (this.m_ipv6ServerInput == null) {
                return -1;
            }
            try {
                int dlen = this.m_ipv6ServerInput.read(this.m_Buffer, 0, 40);
                if (dlen < 0) {
                    close();
                }
                return dlen;
            } catch (InterruptedIOException e) {
                return 0;
            } catch (IOException e2) {
                close();
                return -1;
            }
        }
    }
}
