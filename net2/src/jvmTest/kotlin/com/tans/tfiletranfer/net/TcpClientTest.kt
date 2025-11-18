package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.ext.client.defaultClientManager
import com.tans.tfiletransfer.net.socket.ext.client.requestSimplify
import com.tans.tfiletransfer.net.socket.ext.defaultServerManager
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
        val clientManager = clientTask.defaultClientManager().defaultServerManager()
        clientTask.startTask()
        coroutineScope {
            val stateJob = launch {
                clientTask.state().collect {
                    println("Client state: $it")
                }
            }
            clientTask.state().filter { it is TaskState.Connected }.first()
            val serverReply = try {
                clientManager.requestSimplify<String, String>(
                    requestType = 0,
                    responseType = 1,
                    request = "Hello, Server"
                )
            } catch (_: Throwable) {
                null
            }
            println("Receive server msg: $serverReply")
            clientTask.stopTask()
            stateJob.cancel()
            println("Connection closed.")
        }
    }
}