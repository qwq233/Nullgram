package org.tcp2ws;

import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketOpcode;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;

/* loaded from: org.tcp2ws.jar:org/tcp2ws/Socks4Impl.class */
public class Socks4Impl {
    final ProxyHandler m_Parent;
    byte socksCommand;
    final byte[] DST_Port = new byte[2];
    byte[] DST_Addr = new byte[4];
    byte SOCKS_Version = 0;
    InetAddress m_ServerIP = null;
    int m_nServerPort = 0;
    InetAddress m_ClientIP = null;
    int m_nClientPort = 0;

    /* JADX INFO: Access modifiers changed from: package-private */
    public Socks4Impl(ProxyHandler Parent) {
        this.m_Parent = Parent;
    }

    public byte getSuccessCode() {
        return (byte) 90;
    }

    public byte getFailCode() {
        return (byte) 91;
    }

    @NotNull
    public String commName(byte code) {
        switch (code) {
            case 1:
                return "CONNECT";
            case SocksConstants.SC_UDP /* 3 */:
                return "UDP Association";
            default:
                return "Unknown Command";
        }
    }

    @NotNull
    public String replyName(byte code) {
        switch (code) {
            case WebSocketOpcode.CONTINUATION /* 0 */:
                return "SUCCESS";
            case 1:
                return "General SOCKS Server failure";
            case 2:
                return "Connection not allowed by ruleset";
            case SocksConstants.SC_UDP /* 3 */:
                return "Network Unreachable";
            case SocksConstants.SOCKS4_Version /* 4 */:
                return "HOST Unreachable";
            case SocksConstants.SOCKS5_Version /* 5 */:
                return "Connection Refused";
            case 6:
                return "TTL Expired";
            case 7:
                return "Command not supported";
            case WebSocketOpcode.CLOSE /* 8 */:
                return "Address Type not Supported";
            case WebSocketOpcode.PING /* 9 */:
                return "to 0xFF UnAssigned";
            case 90:
                return "Request GRANTED";
            case 91:
                return "Request REJECTED or FAILED";
            case 92:
                return "Request REJECTED - SOCKS server can't connect to Identd on the client";
            case 93:
                return "Request REJECTED - Client and Identd report diff user-ID";
            default:
                return "Unknown Command";
        }
    }

    public boolean isInvalidAddress(byte Atype) {
        this.m_ServerIP = Utils.calcInetAddress(Atype, this.DST_Addr);
        this.m_nServerPort = Utils.calcPort(this.DST_Port[0], this.DST_Port[1]);
        this.m_ClientIP = this.m_Parent.m_ClientSocket.getInetAddress();
        this.m_nClientPort = this.m_Parent.m_ClientSocket.getPort();
        return this.m_ServerIP == null || this.m_nServerPort < 0;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public byte getByte() {
        try {
            return this.m_Parent.getByteFromClient();
        } catch (Exception e) {
            return (byte) 0;
        }
    }

    public void authenticate(byte SOCKS_Ver) throws Exception {
        this.SOCKS_Version = SOCKS_Ver;
    }

    public void getClientCommand() throws Exception {
        this.socksCommand = getByte();
        this.DST_Port[0] = getByte();
        this.DST_Port[1] = getByte();
        for (int i = 0; i < 4; i++) {
            this.DST_Addr[i] = getByte();
        }
        do {
        } while (getByte() != 0);
        if (this.socksCommand < 1 || this.socksCommand > 2) {
            refuseCommand((byte) 91);
            throw new Exception("Socks 4 - Unsupported Command : " + commName(this.socksCommand));
        } else if (isInvalidAddress((byte) 1)) {
            refuseCommand((byte) 92);
            throw new Exception("Socks 4 - Unknown Host/IP address '" + this.m_ServerIP.toString());
        }
    }

    public void replyCommand(byte ReplyCode) {
        byte[] REPLY = {0, ReplyCode, this.DST_Port[0], this.DST_Port[1], this.DST_Addr[0], this.DST_Addr[1], this.DST_Addr[2], this.DST_Addr[3]};
        this.m_Parent.sendToClient(REPLY);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void refuseCommand(byte errorCode) {
        replyCommand(errorCode);
    }

    private String getCdn() {
        String _server = this.m_ServerIP.getHostAddress();
        String server = null;
        for (int i = 0; server == null && i <= 3; i++) {
            server = (tcp2wsServer.transToIpv6 ? tcp2wsServer.ipv6 : tcp2wsServer.cdn).get(_server.substring(0, _server.length() - i));
        }
        return server != null ? server : _server;
    }

    public void connect() throws Exception {
        try {
            this.m_Parent.connectToServer(getCdn());
            replyCommand(getSuccessCode());
        } catch (IOException e) {
            refuseCommand(getFailCode());
            throw new Exception("Socks 4 - Can't connect to " + Utils.getSocketInfo((!tcp2wsServer.transToIpv6 || tcp2wsServer.forceWs) ? this.m_Parent.m_ServerSocket.getSocket() : this.m_Parent.m_ipv6ServerSocket));
        }
    }

    public void udp() throws IOException, WebSocketException {
        refuseCommand((byte) 91);
    }
}
