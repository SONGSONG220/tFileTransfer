package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.transferproto.conn.qrcode.QRCodeServer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object QRCodeServerTest {

    suspend fun run() {
        val localAddress = findLocalAddressV4()[0]
        println("Local Address: $localAddress")
        val qrCodeServer = QRCodeServer(
            localAddress = localAddress
        )
        coroutineScope {
            qrCodeServer.startTask()
            launch {
                val clientRequest = qrCodeServer.connectionRequest().first()
                println("Receive client request: $clientRequest")
                qrCodeServer.stopTask()
            }
            qrCodeServer.waitTaskFinished()
        }
    }
}