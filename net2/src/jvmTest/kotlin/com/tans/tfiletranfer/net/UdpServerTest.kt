package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ConnectionTaskState
import com.tans.tfiletransfer.net.socket.ext.defaultClientManager
import com.tans.tfiletransfer.net.socket.ext.server.defaultServerManager
import com.tans.tfiletransfer.net.socket.ext.server.server
import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.socket.udp.UdpTask
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

object UdpServerTest {

    suspend fun run() {
        val localAddress = findLocalAddressV4()[0]
        val udpClient = UdpTask(
            connectionType = UdpTask.Companion.UdpConnectionType.Bind(AddressWithPort(localAddress, BIND_PORT)),
            readWriteIdleLimitInMillis = 5000L
        )
        val serverManager = udpClient.defaultServerManager().defaultClientManager()
        serverManager.registerServer(
            server<String, String>(
                requestType = 0,
                responseType = 1
            ) { _, remoteAddress, request, _ ->
                println("Receive client msg from $remoteAddress: $request")
                "Hello, Client"
            }
        )
        udpClient.startTask()
        udpClient.state().filter { it is ConnectionTaskState.Closed || it is ConnectionTaskState.Error }.first()
    }

    const val BIND_PORT = 1997
}