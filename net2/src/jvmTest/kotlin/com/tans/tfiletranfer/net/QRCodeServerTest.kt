package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.transferproto.conn.qrcode.QRCodeServer

object QRCodeServerTest {

    suspend fun run() {
        val localAddress = findLocalAddressV4()[0]
        println("Local Address: $localAddress")
        val qrCodeServer = QRCodeServer(
            localAddress = localAddress
        )
        qrCodeServer.startTask()
        qrCodeServer.waitTaskFinished()
    }
}