@file:Suppress("UNCHECKED_CAST")

package com.tans.tfiletransfer.net.socket

import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
actual fun findLocalAddressV4(): List<Address> {
    return com.tans.tfiletransfer.ios.Address.findLocalIPv4Addresses() as List<Address>
}

@OptIn(ExperimentalForeignApi::class)
actual fun Address.getBroadcastAddressV4(): BroadcastAddress {
    val broadcastAddress = com.tans.tfiletransfer.ios.Address.broadcastAddressFor(this)
    return BroadcastAddress(
        address = broadcastAddress ?: "255.255.255.255",
        mask = 24,
    )
}
