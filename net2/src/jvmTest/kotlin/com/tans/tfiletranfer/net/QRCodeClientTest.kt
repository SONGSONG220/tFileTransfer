package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.transferproto.conn.qrcode.QRCodeClient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object QRCodeClientTest {
    suspend fun run() {
        val localAddress = findLocalAddressV4()[0]
        println("Local Address: $localAddress")
        val qrCodeClient = QRCodeClient(
            localAddress = localAddress,
            localDeviceName = "TestDevice_Client"
        )
        coroutineScope {
            qrCodeClient.startTask()
            launch {
                qrCodeClient.startTask()
                val state = qrCodeClient.waitTaskConnectedOrError()
                if (state is TaskState.Connected) {
                    val isSuccess = qrCodeClient.requestCreateConnection(localAddress)
                    println("Request create connection: isSuccess=$isSuccess.")
                    qrCodeClient.stopTask()
                }
            }
            qrCodeClient.waitTaskFinished()
        }
    }
}