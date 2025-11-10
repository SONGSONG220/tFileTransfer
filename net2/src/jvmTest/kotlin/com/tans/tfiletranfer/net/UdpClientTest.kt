package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ConnectionTaskState
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import com.tans.tfiletransfer.net.socket.udp.UdpTask
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

object UdpClientTest {

    suspend fun run() {
        val bufferPool = BufferPool()
        val udpClient = UdpTask(
            connectionType = UdpTask.Companion.UdpConnectionType.Connect(
                AddressWithPort("127.0.0.1", UdpServerTest.BIND_PORT)
            ),
            bufferPool = bufferPool
        )
        delay(200)
        udpClient.startTask()

        coroutineScope {
            launch {
                udpClient.state().filter { it == ConnectionTaskState.Connected }.firstOrNull()
                val buffer = bufferPool.get(1024)
                val bytes = "Hello Server".toByteArray(Charsets.UTF_8)
                val okIoBuffer = okio.Buffer()
                okIoBuffer.write(bytes)
                buffer.contentSize = okIoBuffer.read(buffer.array)
                udpClient.writePktData(
                    pkt = PackageData(
                        type = 1,
                        messageId = 1000L,
                        data = buffer
                    )
                )
            }
            launch {
                val readChannel = udpClient.pktReadChannel()
                for (pkt in readChannel) {
                    println("Receive server(${pkt.address}) msg: ${String(pkt.pkt.data.array, 0, pkt.pkt.data.contentSize,
                        Charsets.UTF_8)}")
                }
            }
        }
    }
}