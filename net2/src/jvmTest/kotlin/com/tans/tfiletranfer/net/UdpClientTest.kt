package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ext.client.defaultClientManager
import com.tans.tfiletransfer.net.socket.ext.client.requestSimplify
import com.tans.tfiletransfer.net.socket.ext.defaultServerManager
import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.socket.udp.UdpTask
import kotlinx.coroutines.delay

object UdpClientTest {

    suspend fun run() {
        val localAddress = findLocalAddressV4()[0]
        val targetAddress = AddressWithPort(localAddress, UdpServerTest.BIND_PORT)
        val udpClient = UdpTask(
            connectionType = UdpTask.Companion.UdpConnectionType.Connect(targetAddress),
            readWriteIdleLimitInMillis = 5000L
        )
        val clientManager = udpClient.defaultClientManager().defaultServerManager()
        delay(200)
        udpClient.startTask()
        val serverReply = try {
            clientManager.requestSimplify<String, String>(requestType = 0, responseType = 1, request = "Hello, Server", targetAddress = targetAddress)
        } catch (_: Throwable) {
            null
        }
        println("Receive server msg: $serverReply")
        udpClient.stopTask()
    }
}