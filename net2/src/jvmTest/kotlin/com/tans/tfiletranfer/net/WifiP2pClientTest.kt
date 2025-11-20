package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.transferproto.conn.wifip2p.WifiP2pClient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

object WifiP2pClientTest {

    suspend fun run() {
        // Assumes server is running on the same machine.
        val serverAddress = findLocalAddressV4().first()
        println("Client will connect to server: $serverAddress")
        val client = WifiP2pClient(
            localDeviceName = "TestDevice_Client",
            serverAddress = serverAddress
        )
        coroutineScope {
            delay(200)
            client.startTask()
            launch {
                val handshake = client.waitHandshakeOrNull()
                println("Client handshake success: $handshake")
                if (handshake != null) {
                    val createConnJob = launch {
                        val result = client.requestCreateConnection()
                        println("Client request create connection result: $result")
                    }
                    val waitCloseJob = launch {
                        val closeReq = client.closeWifiP2pRequest().first()
                        println("Client received close request: $closeReq")
                    }
                    createConnJob.join()
                    waitCloseJob.join()
                    client.stopTask()
                } else {
                    println("Client handshake failed.")
                    client.stopTask()
                }
            }
            client.waitTaskFinished()
            println("Client test finished.")
        }
    }
}