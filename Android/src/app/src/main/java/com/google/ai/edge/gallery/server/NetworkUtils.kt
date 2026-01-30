package com.google.ai.edge.gallery.server

import java.net.NetworkInterface
import java.util.Collections

fun getLocalIpAddress(): String {
    try {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            val addrs = Collections.list(intf.inetAddresses)
            for (addr in addrs) {
                if (!addr.isLoopbackAddress) {
                    val sAddr = addr.hostAddress
                    val isIPv4 = sAddr?.indexOf(':') ?: -1 < 0
                    if (isIPv4) {
                        return sAddr ?: "Unknown"
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "Unable to get IP"
}
