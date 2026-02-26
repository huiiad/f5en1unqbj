package com.ssp.sdk.util;

import android.content.Context;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

public class IpUtils {

    public static String getLocalIp(Context context) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) {
                return "";
            }
            for (NetworkInterface intf : Collections.list(interfaces)) {
                if (!intf.isUp() || intf.isLoopback()) {
                    continue;
                }
                for (InetAddress addr : Collections.list(intf.getInetAddresses())) {
                    if (addr.isLoopbackAddress()) {
                        continue;
                    }
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return "";
    }
}

