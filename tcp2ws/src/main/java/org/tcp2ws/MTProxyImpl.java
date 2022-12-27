package org.tcp2ws;

import java.io.IOException;

/* loaded from: org.tcp2ws.jar:org/tcp2ws/MTProxyImpl.class */
public class MTProxyImpl {
    final ProxyHandler m_Parent;

    /* JADX INFO: Access modifiers changed from: package-private */
    public MTProxyImpl(ProxyHandler Parent) {
        this.m_Parent = Parent;
    }

    public void mtpConnect() throws Exception {
        try {
            this.m_Parent.connectToServer((tcp2wsServer.transToIpv6 ? tcp2wsServer.mtpipv6 : tcp2wsServer.mtpcdn).get(this.m_Parent.DC));
        } catch (IOException e) {
            throw new Exception("MTP - Can't connect to " + Utils.getSocketInfo((!tcp2wsServer.transToIpv6 || tcp2wsServer.forceWs) ? this.m_Parent.m_ServerSocket.getSocket() : this.m_Parent.m_ipv6ServerSocket));
        }
    }
}
