package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ConnectionTaskState
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import com.tans.tfiletransfer.net.socket.tcp.TcpClientTask
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object TcpClientTest {

    suspend fun run() {
        val bufferPool = BufferPool()
        val clientTask = TcpClientTask(serverAddress = AddressWithPort("127.0.0.1", 1996), bufferPool = bufferPool)
        delay(200)
        clientTask.startTask()
        coroutineScope {
            launch {
                clientTask.state().collect {
                    println("Client state: $it")
                }
            }
            launch {
                for (pkt in clientTask.pktReadChannel()) {
                    println("Client receive: ${String(pkt.data.array, 0, pkt.data.contentSize, Charsets.UTF_8)}")
                    bufferPool.put(pkt.data)
                }
            }
            launch {
                clientTask.state().filter { it == ConnectionTaskState.Connected }.first()
                val greetingToServer = "Hello, server"
                val buffer = bufferPool.get(1024)
                val okioBuffer = okio.Buffer()
                okioBuffer.writeUtf8(greetingToServer)
                val contentSize = okioBuffer.read(buffer.array)
                buffer.contentSize = contentSize
                clientTask.writePktData(
                    PackageData(
                        type = 1,
                        messageId = 1000L,
                        data = buffer
                    )
                )
                // delay(1000)
                // clientTask.stopTask()
            }
        }
    }
}