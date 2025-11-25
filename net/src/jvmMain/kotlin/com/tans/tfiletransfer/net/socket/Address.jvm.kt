package com.tans.tfiletransfer.net.socket

import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Enumeration


actual fun findLocalAddressV4(): List<Address> {
    val interfaces: Enumeration<NetworkInterface>? = NetworkInterface.getNetworkInterfaces()
    val result = ArrayList<Address>()
    if (interfaces != null) {
        while (interfaces.hasMoreElements()) {
            val inetAddresses = interfaces.nextElement().inetAddresses
            while (inetAddresses.hasMoreElements()) {
                val address = inetAddresses.nextElement()
                if (address.address.size == 4 && !address.isLinkLocalAddress && !address.isLoopbackAddress) {
                    result.add(address.address.toAddress())
                }
            }
        }
    }
    return result
}

actual fun Address.getBroadcastAddressV4(): BroadcastAddress {
    val jvmAddress = InetAddress.getByAddress(this.toBytes())
    return NetworkInterface.getByInetAddress(jvmAddress)?.interfaceAddresses
        ?.filter {
            val broadcast = it.broadcast
            val address = it.address
            address == jvmAddress && broadcast != null && broadcast.address?.size == 4
        }?.firstNotNullOfOrNull() {
            BroadcastAddress(
                address = it.broadcast.address.toAddress(),
                mask = it.networkPrefixLength.toInt()
            )
        } ?: BroadcastAddress("255.255.255.255", 24)
}