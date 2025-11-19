package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.transferproto.conn.qrcode.QRCodeClient

object QRCodeClientTest {
    suspend fun run() {
        val localAddress = findLocalAddressV4()[0]
        println("Local Address: $localAddress")
        val qrCodeClient = QRCodeClient(
            localAddress = localAddress,
            localDeviceName = "TestDevice_Client"
        )
        qrCodeClient.startTask()
        qrCodeClient.waitTaskFinished()
    }
}