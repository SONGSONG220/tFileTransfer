package com.tans.tfiletranfer.net

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.readAvailable
import io.ktor.utils.io.core.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.io.Buffer

object UdpClientTest {

    suspend fun run() {
        try {
            val selector = SelectorManager(Dispatchers.IO)
            selector.use {
                val remoteAddress = InetSocketAddress("127.0.0.1", UdpServerTest.BIND_PORT)
                val socket = aSocket(selector)
                    .udp()
                    .configure {
                        broadcast = true
                    }
                    .connect(remoteAddress = remoteAddress)
                socket.use {
                    val sendArray = "Message from client -> Hello.".toByteArray(Charsets.UTF_8)
                    val sendBuffer = Buffer()
                    sendBuffer.writeInt(sendArray.size)
                    sendBuffer.writeFully(sendArray)
                    val sendPkt = Datagram(sendBuffer, remoteAddress)
                    socket.outgoing.send(sendPkt)

                    val receiveData = socket.incoming.receive()
                    val receiveDataSize = receiveData.packet.readInt()
                    val receiveArray = ByteArray(receiveDataSize)
                    receiveData.packet.readAvailable(receiveArray)
                    println("Receive server msg: ${receiveArray.toString(Charsets.UTF_8)}")
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}