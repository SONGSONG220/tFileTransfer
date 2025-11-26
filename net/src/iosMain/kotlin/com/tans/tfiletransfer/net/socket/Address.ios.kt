package com.tans.tfiletransfer.net.socket

actual fun findLocalAddressV4(): List<Address> {
    return listOf("127.0.0.1")
}

actual fun Address.getBroadcastAddressV4(): BroadcastAddress {
    return BroadcastAddress(address = this, mask = 24)
}
