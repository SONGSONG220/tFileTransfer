package com.tans.tfiletranfer.net

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.readAvailable
import io.ktor.utils.io.core.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.io.Buffer

object UdpServerTest {
    suspend fun run() {
        try {
            val selector = SelectorManager(Dispatchers.IO)
            selector.use {
                val socket = aSocket(selector)
                    .udp()
                    .configure { selector
                        broadcast = true
                    }
                    .bind("127.0.0.1", BIND_PORT)
                socket.use {
                    val receiveData = socket.incoming.receive()
                    val receiveDataSize = receiveData.packet.readInt()
                    val receiveArray = ByteArray(receiveDataSize)
                    receiveData.packet.readAvailable(receiveArray)
                    println("Receive client msg: ${receiveArray.toString(Charsets.UTF_8)}")

                    val sendArray = "Message from server -> Hello.".toByteArray(Charsets.UTF_8)
                    val sendBuffer = Buffer()
                    sendBuffer.writeInt(sendArray.size)
                    sendBuffer.writeFully(sendArray)
                    val sendPkt = Datagram(sendBuffer, receiveData.address)
                    socket.outgoing.send(sendPkt)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    const val BIND_PORT = 1997
}