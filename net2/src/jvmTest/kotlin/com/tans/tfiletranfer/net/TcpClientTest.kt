package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ConnectionTaskState
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import com.tans.tfiletransfer.net.socket.ext.client.defaultClientManager
import com.tans.tfiletransfer.net.socket.ext.client.requestSimplify
import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.socket.tcp.TcpClientTask
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object TcpClientTest {

    suspend fun run() {
        val localAddress = findLocalAddressV4()[0]
        val clientTask = TcpClientTask(serverAddress = AddressWithPort(localAddress, TcpServerTest.BIND_PORT))
        delay(200)
        val clientManager = clientTask.defaultClientManager()
        clientTask.startTask()
        coroutineScope {
            launch {
                clientTask.state().collect {
                    println("Client state: $it")
                }
            }
            launch {
                clientTask.state().filter { it is ConnectionTaskState.Connected }.first()
                val serverReply = clientManager.requestSimplify<String, String>(
                    type = 0,
                    request = "Hello, Server"
                )
                println("Receive server msg: $serverReply")
            }
        }
    }
}