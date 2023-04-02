package org.tcp2ws;

import static java.lang.String.format;

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

public final class Utils {

    @Nullable
    public static InetAddress calcInetAddress(byte Atype, byte[] addr) {
        try {
            if (Atype == 0x01)
                return Inet4Address.getByAddress(Arrays.copyOf(addr, 4));
            else if (Atype == 0x04)
                return Inet4Address.getByAddress(Arrays.copyOf(addr, 16));
        } catch (UnknownHostException e) {
            return null;
        }
        return null;
    }

    public static int byte2int(byte b) {
        return (int) b < 0 ? 0x100 + (int) b : b;
    }

    public static int calcPort(byte Hi, byte Lo) {
        return ((byte2int(Hi) << 8) | byte2int(Lo));
    }

    @NotNull
    public static String iP2Str(InetAddress IP) {
        return IP == null
            ? "NA/NA"
            : format("%s/%s", IP.getHostName(), IP.getHostAddress());
    }

    @NotNull
    public static String getSocketInfo(Socket sock) {
        return sock == null
            ? "<NA/NA:0>"
            : format("<%s:%d>", Utils.iP2Str(sock.getInetAddress()), sock.getPort());
    }

    @NotNull
    public static String getSocketInfo(DatagramPacket DGP) {
        return DGP == null
            ? "<NA/NA:0>"
            : format("<%s:%d>", Utils.iP2Str(DGP.getAddress()), DGP.getPort());
    }

    public static byte[] reverse(byte[] arr) {
        for (int i = 0; i < arr.length / 2; i++) {
            int temp = arr[i];
            arr[i] = arr[arr.length - i - 1];
            arr[arr.length - i - 1] = (byte) temp;
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
        for (byte element : second) {
            result[pos] = element;
            pos++;
        }
        return result;
    }

    public static void bytesToHex(byte[] bytes) {
        char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        System.out.println(hexChars);
    }

}
