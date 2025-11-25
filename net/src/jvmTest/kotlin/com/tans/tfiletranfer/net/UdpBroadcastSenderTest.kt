package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.socket.getBroadcastAddressV4
import com.tans.tfiletransfer.net.transferproto.conn.broadcast.BroadcastSender
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object UdpBroadcastSenderTest {

    suspend fun run() {
        val address = findLocalAddressV4()[0]
        val broadcast = address.getBroadcastAddressV4()
        println("Address: $address")
        println("Broadcast: $broadcast")
        val broadcastSender = BroadcastSender(
            localDeviceName = "TestDevice_Sender",
            localAddress = address,
            broadcastAddress = broadcast.address
        )
        broadcastSender.startTask()
        coroutineScope {
            val job = launch {
                broadcastSender.connectionRequest()
                    .collect {
                        println("ConnectionRequest: $it")
                    }
            }
            broadcastSender.waitTaskFinished()
            job.cancel()
        }
    }
}