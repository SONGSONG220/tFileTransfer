package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.PackageDataWithAddress
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import com.tans.tfiletransfer.net.socket.udp.UdpTask
import kotlinx.coroutines.delay

object UdpServerTest {
    suspend fun run() {
        val bufferPool = BufferPool()
        val udpClient = UdpTask(
            connectionType = UdpTask.Companion.UdpConnectionType.Bind(
                AddressWithPort("127.0.0.1", BIND_PORT)
            ),
            bufferPool = bufferPool
        )
        delay(200)
        udpClient.startTask()
        val readChannel = udpClient.pktReadChannel()
        for (pkt in readChannel) {
            println("Receive client(${pkt.address}) msg: ${String(pkt.pkt.data.array, 0, pkt.pkt.data.contentSize,
                Charsets.UTF_8)}")

            val buffer = bufferPool.get(1024)
            val bytes = "Hello Client.".toByteArray(Charsets.UTF_8)
            val okIoBuffer = okio.Buffer()
            okIoBuffer.write(bytes)
            buffer.contentSize = okIoBuffer.read(buffer.array)
            udpClient.writePktData(
                pktDataWithAddress = PackageDataWithAddress(
                    pkt = PackageData(
                        type = 1,
                        messageId = 1000L,
                        data = buffer
                    ),
                    address = pkt.address
                )
            )
        }
    }

    const val BIND_PORT = 1997
}