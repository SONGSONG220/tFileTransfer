package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ConnectionTaskState
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.buffer.Buffer
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import com.tans.tfiletransfer.net.socket.tcp.TcpClientTask
import com.tans.tfiletransfer.net.socket.tcp.TcpServerTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object MainTest {

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val bufferPool = BufferPool()
            launch {
                val serverTask = TcpServerTask(bindAddress = AddressWithPort("127.0.0.1", 1996), bufferPool = bufferPool)
                serverTask.startTask()

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
                                println("Server receive: ${pkt.data.array.toString(Charsets.UTF_8)}")
                                bufferPool.put(pkt.data)
                            }
                        }
                    }
                }
            }

            launch {
                val clientTask = TcpClientTask(serverAddress = AddressWithPort("127.0.0.1", 1996), bufferPool = bufferPool)
                delay(200)
                clientTask.startTask()
                launch {
                    clientTask.state().collect {
                        println("Client state: $it")
                    }
                }
                launch {
                    for (pkt in clientTask.pktReadChannel()) {
                        println("Client receive: ${pkt.data.array.toString(Charsets.UTF_8)}")
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
                    delay(1000)
                    clientTask.stopTask()
                }
            }
        }
    }
}