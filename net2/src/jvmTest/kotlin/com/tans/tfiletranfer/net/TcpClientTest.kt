package com.tans.tfiletranfer.net

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.cancel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readInt
import io.ktor.utils.io.writeByteArray
import io.ktor.utils.io.writeInt
import kotlinx.coroutines.Dispatchers

object TcpClientTest {

    suspend fun run() {
        try {
            val selectorManager = SelectorManager(Dispatchers.IO)
            selectorManager.use {
                val clientSocket = aSocket(selectorManager)
                    .tcp()
                    .configure { selectorManager
                        reuseAddress = true
                        reusePort = true
                    }
                    .connect("127.0.0.1", TcpServerTest.BIND_PORT)
                clientSocket.use {
                    println("Connect to server success.")
                    val readChannel = clientSocket.openReadChannel()
                    val readLen = readChannel.readInt()
                    val readArray = ByteArray(readLen)
                    readChannel.readAvailable(readArray)
                    readChannel.cancel()
                    println("Receive server msg: ${readArray.toString(Charsets.UTF_8)}")

                    val writeChannel = clientSocket.openWriteChannel(autoFlush = true)
                    val writeArray = "Message from client -> Hello, ^_^.".toByteArray(Charsets.UTF_8)
                    writeChannel.writeInt(writeArray.size)
                    writeChannel.writeByteArray(writeArray)
                    writeChannel.flushAndClose()
                    println("Send message to server success.")
                }
            }
        } catch (e: Throwable) {
            println(e.message)
            e.printStackTrace()
        }
    }
}