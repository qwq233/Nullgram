package org.tcp2ws;

import com.neovisionaries.ws.client.WebSocketException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;


/* loaded from: org.tcp2ws.jar:org/tcp2ws/Socks5Impl.class */
public class Socks5Impl extends Socks4Impl {
    private static final int[] ADDR_Size = {-1, 4, -1, -1, 16};
    private static final byte[] SRE_REFUSE = {5, -1};
    private static final byte[] SRE_ACCEPT = {5, 0};
    private static final int MAX_ADDR_LEN = 255;
    private byte ADDRESS_TYPE;
    private DatagramSocket DGSocket;
    private DatagramPacket DGPack;
    private InetAddress UDP_IA;
    private int UDP_port;

    private static /* synthetic */ void $$$reportNull$$$0(int i) {
        throw new IllegalStateException(String.format("@NotNull method %s.%s must not return null", "org/tcp2ws/Socks5Impl", "addDgpHead"));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public Socks5Impl(ProxyHandler Parent) {
        super(Parent);
        this.DGSocket = null;
        this.DGPack = null;
        this.UDP_IA = null;
        this.UDP_port = 0;
        this.DST_Addr = new byte[MAX_ADDR_LEN];
    }

    @Override // org.tcp2ws.Socks4Impl
    public byte getSuccessCode() {
        return (byte) 0;
    }

    @Override // org.tcp2ws.Socks4Impl
    public byte getFailCode() {
        return (byte) 4;
    }

    @Nullable
    public InetAddress calcInetAddress(byte AType, byte[] addr) {
        InetAddress IA;
        switch (AType) {
            case 1:
                IA = Utils.calcInetAddress(AType, addr);
                break;
            case 2:
            default:
                return null;
            case SocksConstants.SC_UDP /* 3 */:
                if (addr[0] <= 0) {
                    return null;
                }
                StringBuilder sIA = new StringBuilder();
                for (int i = 1; i <= addr[0]; i++) {
                    sIA.append((char) addr[i]);
                }
                try {
                    IA = InetAddress.getByName(sIA.toString());
                    break;
                } catch (UnknownHostException e) {
                    return null;
                }
            case SocksConstants.SOCKS4_Version /* 4 */:
                IA = Utils.calcInetAddress(AType, addr);
                break;
        }
        return IA;
    }

    public boolean isInvalidAddress() {
        this.m_ServerIP = calcInetAddress(this.ADDRESS_TYPE, this.DST_Addr);
        this.m_nServerPort = Utils.calcPort(this.DST_Port[0], this.DST_Port[1]);
        this.m_ClientIP = this.m_Parent.m_ClientSocket.getInetAddress();
        this.m_nClientPort = this.m_Parent.m_ClientSocket.getPort();
        return this.m_ServerIP == null || this.m_nServerPort < 0;
    }

    @Override // org.tcp2ws.Socks4Impl
    public void authenticate(byte SOCKS_Ver) throws Exception {
        super.authenticate(SOCKS_Ver);
        if (this.SOCKS_Version == 5) {
            if (!checkAuthentication()) {
                refuseAuthentication("SOCKS 5 - Not Supported Authentication!");
                throw new Exception("SOCKS 5 - Not Supported Authentication.");
            } else {
                acceptAuthentication();
                return;
            }
        }
        refuseAuthentication("Incorrect SOCKS version : " + ((int) this.SOCKS_Version));
        throw new Exception("Not Supported SOCKS Version -'" + ((int) this.SOCKS_Version) + "'");
    }

    public void refuseAuthentication(String msg) {
        this.m_Parent.sendToClient(SRE_REFUSE);
    }

    public void acceptAuthentication() {
        byte[] tSRE_Accept = SRE_ACCEPT;
        tSRE_Accept[0] = this.SOCKS_Version;
        this.m_Parent.sendToClient(tSRE_Accept);
    }

    public boolean checkAuthentication() {
        int Methods_Num = getByte();
        StringBuilder Methods = new StringBuilder();
        for (int i = 0; i < Methods_Num; i++) {
            Methods.append(",-").append((int) getByte()).append('-');
        }
        return Methods.indexOf("-0-") != -1 || Methods.indexOf("-00-") != -1;
    }

    @Override // org.tcp2ws.Socks4Impl
    public void getClientCommand() throws Exception {
        this.SOCKS_Version = getByte();
        this.socksCommand = getByte();
        getByte();
        this.ADDRESS_TYPE = getByte();
        int Addr_Len = ADDR_Size[this.ADDRESS_TYPE];
        this.DST_Addr[0] = getByte();
        if (this.ADDRESS_TYPE == 3) {
            Addr_Len = this.DST_Addr[0] + 1;
        }
        for (int i = 1; i < Addr_Len; i++) {
            this.DST_Addr[i] = getByte();
        }
        this.DST_Port[0] = getByte();
        this.DST_Port[1] = getByte();
        if (this.SOCKS_Version != 5) {
            refuseCommand((byte) -1);
            throw new Exception("Incorrect SOCKS Version of Command: " + ((int) this.SOCKS_Version));
        } else if (this.socksCommand < 1 || this.socksCommand > 3) {
            refuseCommand((byte) 7);
            throw new Exception("SOCKS 5 - Unsupported Command: \"" + ((int) this.socksCommand) + "\"");
        } else if (this.ADDRESS_TYPE != 1 && this.ADDRESS_TYPE != 4) {
            refuseCommand((byte) 8);
            throw new Exception("SOCKS 5 - Unsupported Address Type: " + ((int) this.ADDRESS_TYPE));
        } else if (isInvalidAddress()) {
            refuseCommand((byte) 4);
            throw new Exception("SOCKS 5 - Unknown Host/IP address '" + this.m_ServerIP.toString() + "'");
        }
    }

    public void udpReply(byte replyCode, InetAddress IA, int pt) {
        byte[] IP = IA.getAddress();
        byte[] REPLY = new byte[10];
        formGenericReply(replyCode, pt, REPLY, IP);
        this.m_Parent.sendToClient(REPLY);
    }

    private void formGenericReply(byte replyCode, int pt, byte[] REPLY, byte[] IP) {
        REPLY[0] = 5;
        REPLY[1] = replyCode;
        REPLY[2] = 0;
        REPLY[3] = 1;
        REPLY[4] = IP[0];
        REPLY[5] = IP[1];
        REPLY[6] = IP[2];
        REPLY[7] = IP[3];
        REPLY[8] = (byte) ((pt & 65280) >> 8);
        REPLY[9] = (byte) (pt & MAX_ADDR_LEN);
    }

    @Override // org.tcp2ws.Socks4Impl
    public void udp() throws IOException, WebSocketException {
        try {
            this.DGSocket = new DatagramSocket();
            initUdpInOut();
            InetAddress MyIP = this.m_Parent.m_ClientSocket.getLocalAddress();
            int MyPort = this.DGSocket.getLocalPort();
            udpReply((byte) 0, MyIP, MyPort);
            while (this.m_Parent.checkClientData() >= 0) {
                processUdp();
                Thread.yield();
            }
        } catch (IOException e) {
            refuseCommand((byte) 5);
            throw new IOException("Connection Refused - FAILED TO INITIALIZE UDP Association.");
        }
    }

    private void initUdpInOut() throws IOException {
        this.DGSocket.setSoTimeout(10);
        this.m_Parent.m_Buffer = new byte[40];
        this.DGPack = new DatagramPacket(this.m_Parent.m_Buffer, 40);
    }

    @NotNull
    private byte[] addDgpHead(byte[] buffer) {
        byte[] IABuf = this.DGPack.getAddress().getAddress();
        int DGport = this.DGPack.getPort();
        int HeaderLen = 6 + IABuf.length;
        int DataLen = this.DGPack.getLength();
        int NewPackLen = HeaderLen + DataLen;
        byte[] UB = new byte[NewPackLen];
        UB[0] = 0;
        UB[1] = 0;
        UB[2] = 0;
        UB[3] = 1;
        System.arraycopy(IABuf, 0, UB, 4, IABuf.length);
        UB[4 + IABuf.length] = (byte) ((DGport >> 8) & MAX_ADDR_LEN);
        UB[5 + IABuf.length] = (byte) (DGport & MAX_ADDR_LEN);
        System.arraycopy(buffer, 0, UB, 6 + IABuf.length, DataLen);
        System.arraycopy(UB, 0, buffer, 0, NewPackLen);
        if (UB == null) {
            $$$reportNull$$$0(0);
        }
        return UB;
    }

    @Nullable
    private byte[] clearDgpHead(byte[] buffer) {
        int IAlen;
        byte AType = buffer[3];
        switch (AType) {
            case 1:
                IAlen = 4;
                break;
            case SocksConstants.SC_UDP /* 3 */:
                IAlen = buffer[4] + 1;
                break;
            default:
                return null;
        }
        byte[] IABuf = new byte[IAlen];
        System.arraycopy(buffer, 4, IABuf, 0, IAlen);
        int p = 4 + IAlen;
        this.UDP_IA = calcInetAddress(AType, IABuf);
        int p2 = p + 1;
        int p3 = p2 + 1;
        this.UDP_port = Utils.calcPort(buffer[p], buffer[p2]);
        if (this.UDP_IA == null) {
            return null;
        }
        int DataLen = this.DGPack.getLength() - p3;
        byte[] UB = new byte[DataLen];
        System.arraycopy(buffer, p3, UB, 0, DataLen);
        System.arraycopy(UB, 0, buffer, 0, DataLen);
        return UB;
    }

    protected void udpSend(DatagramPacket DGP) {
        if (DGP != null) {
            String str = DGP.getAddress() + ":" + DGP.getPort() + "> : " + DGP.getLength() + " bytes";
            try {
                this.DGSocket.send(DGP);
            } catch (IOException e) {
            }
        }
    }

    public void processUdp() {
        try {
            this.DGSocket.receive(this.DGPack);
            if (this.m_ClientIP.equals(this.DGPack.getAddress())) {
                processUdpClient();
            } else {
                processUdpRemote();
            }
            try {
                initUdpInOut();
            } catch (IOException e) {
                this.m_Parent.close();
            }
        } catch (InterruptedIOException e2) {
        } catch (IOException e3) {
        }
    }

    private void processUdpClient() {
        this.m_nClientPort = this.DGPack.getPort();
        byte[] Buf = clearDgpHead(this.DGPack.getData());
        if (Buf == null || Buf.length <= 0 || this.UDP_IA == null || this.UDP_port == 0) {
            return;
        }
        if (this.m_ServerIP != this.UDP_IA || this.m_nServerPort != this.UDP_port) {
            this.m_ServerIP = this.UDP_IA;
            this.m_nServerPort = this.UDP_port;
        }
        DatagramPacket DGPSend = new DatagramPacket(Buf, Buf.length, this.UDP_IA, this.UDP_port);
        udpSend(DGPSend);
    }

    public void processUdpRemote() {
        InetAddress DGP_IP = this.DGPack.getAddress();
        int DGP_Port = this.DGPack.getPort();
        byte[] Buf = addDgpHead(this.m_Parent.m_Buffer);
        DatagramPacket DGPSend = new DatagramPacket(Buf, Buf.length, this.m_ClientIP, this.m_nClientPort);
        udpSend(DGPSend);
        if (DGP_IP != this.UDP_IA || DGP_Port != this.UDP_port) {
            this.m_ServerIP = DGP_IP;
            this.m_nServerPort = DGP_Port;
        }
    }
}
