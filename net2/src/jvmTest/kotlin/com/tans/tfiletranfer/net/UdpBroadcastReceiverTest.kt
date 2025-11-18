package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.socket.getBroadcastAddressV4
import com.tans.tfiletransfer.net.transferproto.conn.broadcast.BroadcastReceiver
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object UdpBroadcastReceiverTest {

    suspend fun run() {
        val address = findLocalAddressV4()[0]
        val broadcast = address.getBroadcastAddressV4()
        println("Address: $address")
        println("Broadcast: $broadcast")
        val broadcastReceiver = BroadcastReceiver(
            localDeviceName = "TestDevice_Receiver",
            localAddress = address,
            broadcastAddress = address
        )
        broadcastReceiver.startTask()
        coroutineScope {
            val job = launch {
                broadcastReceiver.remoteDevices()
                    .collect {
                        println("RemoteDevice: $it")
                    }
            }
            broadcastReceiver.waitTaskFinished()
            job.cancel()
        }
    }
}