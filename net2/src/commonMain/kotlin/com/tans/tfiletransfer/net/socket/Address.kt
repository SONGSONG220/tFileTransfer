package com.tans.tfiletransfer.net.socket

import okio.Buffer
import kotlin.experimental.and

// example: 127.0.0.1
typealias Address = String

data class AddressWithPort(
    val address: Address,
    val port: Int
) {
    override fun toString(): String {
        return "$address:$port"
    }
}

data class BroadcastAddress(
    val address: Address,
    val mask: Int
)

expect fun findLocalAddressV4(): List<Address>


expect fun Address.getBroadcastAddressV4(): BroadcastAddress

fun Int.toBytes(): ByteArray {
    val buffer = Buffer()
    buffer.writeInt(this)
    return buffer.readByteArray()
}

fun ByteArray.toInt(): Int {
    if (size != 4)
        throw Exception("The length of the byte array must be at least 4 bytes long.")
    val buffer = Buffer()
    buffer.write(this)
    return buffer.readInt()
}

fun Address.toBytes(): ByteArray {
    return this.split('.').let {
        if (it.size != 4) {
            error("Wrong address: $this")
        }
        it.map { s ->
            (s.toShort() and 0x00FF).toByte()
        }
    }.toByteArray()
}

fun ByteArray.toAddress(): Address {
    if (size != 4)
        throw Exception("The length of the byte array must be at least 4 bytes long.")
    return "${get(0).toUByte()}.${get(1).toUByte()}.${get(2).toUByte()}.${get(3).toUByte()}"
}