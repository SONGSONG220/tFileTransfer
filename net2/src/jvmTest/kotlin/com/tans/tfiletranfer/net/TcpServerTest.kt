package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import com.tans.tfiletransfer.net.socket.tcp.TcpServerTask
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


object TcpServerTest {

    suspend fun run() {
        val bufferPool = BufferPool()
        val serverTask = TcpServerTask(bindAddress = AddressWithPort("127.0.0.1", BIND_PORT), bufferPool = bufferPool)
        serverTask.startTask()
        coroutineScope {
            launch {
                serverTask.state().collect {
                    println("Server state: $it")
                }
            }
            launch {
                for (client in serverTask.clientChannel()) {
                    launch {
                        val greetingToClient = "Hello, client"
                        val buffer = bufferPool.get(1024)
                        val okioBuffer = okio.Buffer()
                        okioBuffer.writeUtf8(greetingToClient)
                        val contentSize = okioBuffer.read(buffer.array)
                        buffer.contentSize = contentSize
                        client.writePktData(
                            PackageData(
                                type = 1,
                                messageId = 1000L,
                                data = buffer
                            )
                        )
                    }
                    launch {
                        for (pkt in client.pktReadChannel()) {
                            println("Server receive: ${String(pkt.data.array, 0, pkt.data.contentSize, Charsets.UTF_8)}")
                            bufferPool.put(pkt.data)
                        }
                    }
                }
            }
        }
    }

    const val BIND_PORT = 1996
}