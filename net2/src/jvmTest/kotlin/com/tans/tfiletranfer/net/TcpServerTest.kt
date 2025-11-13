package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import com.tans.tfiletransfer.net.socket.ext.server.defaultServerManager
import com.tans.tfiletransfer.net.socket.ext.server.server
import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.socket.tcp.TcpServerTask
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch


object TcpServerTest {

    suspend fun run() {
        val localAddress = findLocalAddressV4()[0]
        val serverTask = TcpServerTask(bindAddress = AddressWithPort(localAddress, BIND_PORT))
        serverTask.startTask()
        coroutineScope {
            launch {
                serverTask.state().collect {
                    println("Server state: $it")
                }
            }
            launch {
                for (clientTask in serverTask.clientChannel()) {
                    val serverManager = clientTask.defaultServerManager()
                    serverManager.registerServer(
                        server<String, String>(
                            requestType = 0,
                            responseType = 1,
                        ) { _, _, request, _ ->
                            println("Receive msg from client: $request")
                            "Hello, Client."
                        }
                    )
                }
            }
        }
    }

    const val BIND_PORT = 1996
}