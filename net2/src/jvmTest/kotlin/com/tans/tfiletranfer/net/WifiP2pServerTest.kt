package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.transferproto.conn.wifip2p.WifiP2pServer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object WifiP2pServerTest {

    suspend fun run() {
        val localAddress = findLocalAddressV4().first()
        println("Server address: $localAddress")
        val server = WifiP2pServer(
            localDeviceName = "TestDevice_Server",
            localAddress = localAddress
        )
        coroutineScope {
            server.startTask()
            launch {
                val handshake = server.waitHandshakeOrNull()
                println("Server handshake success: $handshake")
                if (handshake != null) {
                    val requestCloseJob = launch {
                        // val ret = server.requestCloseWifiP2p()
                        // println("Server request close wifi p2p: $ret")
                    }
                    val waitRequestJob = launch {
                        val req = server.connectionRequest().first()
                        println("Server received connection request: $req")
                    }
                    requestCloseJob.join()
                    waitRequestJob.join()
                    server.stopTask()
                } else {
                    println("Server handshake failed.")
                    server.stopTask()
                }
            }
            server.waitTaskFinished()
            println("Server test finished.")
        }
    }
}