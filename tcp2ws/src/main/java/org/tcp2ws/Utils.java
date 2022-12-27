package org.tcp2ws;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/* loaded from: org.tcp2ws.jar:org/tcp2ws/Utils.class */
public final class Utils {

    @Nullable
    public static InetAddress calcInetAddress(byte Atype, byte[] addr) {
        try {
            if (Atype == 1) {
                return Inet4Address.getByAddress(Arrays.copyOf(addr, 4));
            }
            if (Atype == 4) {
                return Inet4Address.getByAddress(Arrays.copyOf(addr, 16));
            }
            return null;
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public static int byte2int(byte b) {
        return b < 0 ? 256 + b : b;
    }

    public static int calcPort(byte Hi, byte Lo) {
        return (byte2int(Hi) << 8) | byte2int(Lo);
    }

    @NotNull
    public static String iP2Str(InetAddress IP) {
        return IP == null ? "NA/NA" : String.format("%s/%s", IP.getHostName(), IP.getHostAddress());
    }

    @NotNull
    public static String getSocketInfo(Socket sock) {
        return sock == null ? "<NA/NA:0>" : String.format("<%s:%d>", iP2Str(sock.getInetAddress()), Integer.valueOf(sock.getPort()));
    }

    @NotNull
    public static String getSocketInfo(DatagramPacket DGP) {
        return DGP == null ? "<NA/NA:0>" : String.format("<%s:%d>", iP2Str(DGP.getAddress()), Integer.valueOf(DGP.getPort()));
    }

    public static byte[] reverse(byte[] arr) {
        for (int i = 0; i < arr.length / 2; i++) {
            byte b = arr[i];
            arr[i] = arr[(arr.length - i) - 1];
            arr[(arr.length - i) - 1] = b;
        }
        return arr;
    }

    public static byte[] SHA256(byte[] message) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        digest.reset();
        digest.update(message);
        return digest.digest();
    }

    public static byte[] concat(byte[] first, byte[] second) {
        int length = first.length + second.length;
        byte[] result = new byte[length];
        int pos = 0;
        for (byte element : first) {
            result[pos] = element;
            pos++;
        }
        for (byte element2 : second) {
            result[pos] = element2;
            pos++;
        }
        return result;
    }

    public static void bytesToHex(byte[] bytes) {
        char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 255;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[(j * 2) + 1] = HEX_ARRAY[v & 15];
        }
        System.out.println(hexChars);
    }
}
