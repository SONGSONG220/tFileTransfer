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

object TcpServerTest {

    suspend fun run() {
        try {
            val selectorManager = SelectorManager(Dispatchers.IO)
            selectorManager.use {
                val serverSocket = aSocket(selectorManager)
                    .tcp()
                    .configure {
                        reuseAddress = true
                        reusePort = true
                    }
                    .bind("127.0.0.1", BIND_PORT)
                serverSocket.use { serverSocket ->
                    println("Bind server socket success.")
                    while (true) {
                        val clientSocket = serverSocket.accept()
                        println("New client connection coming: ${clientSocket.remoteAddress}")
                        clientSocket.use {
                            val writeChannel = clientSocket.openWriteChannel(autoFlush = true)
                            val writeArray = "Message from server -> Hello.".toByteArray(Charsets.UTF_8)
                            writeChannel.writeInt(writeArray.size)
                            writeChannel.writeByteArray(writeArray)
                            writeChannel.flushAndClose()
                            println("Send message to client success")

                            val readChannel = clientSocket.openReadChannel()
                            val readSize = readChannel.readInt()
                            val readBytes = ByteArray(readSize)
                            readChannel.readAvailable(readBytes)
                            readChannel.cancel()
                            println("Receive client msg: ${readBytes.toString(Charsets.UTF_8)}")
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            println(e.message)
            e.printStackTrace()
        }
    }

    const val BIND_PORT = 1996
}