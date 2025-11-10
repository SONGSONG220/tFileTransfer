package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.socket.getBroadcastAddressV4
import com.tans.tfiletransfer.net.socket.udp.UdpTask

object UdpBroadcastReceiverTest {

    suspend fun run() {
        val address = findLocalAddressV4()[0]
        val broadcast = address.getBroadcastAddressV4()
        println("LocalAddress=$address, Broadcast=$broadcast")
        val task = UdpTask(
            connectionType = UdpTask.Companion.UdpConnectionType.Bind(
                localAddress = AddressWithPort(broadcast.address, 1997)
            )
        )
        task.startTask()
        val readChannel = task.pktReadChannel()
        for (pkt in readChannel) {
            println("Receive BroadcastMsg from ${pkt.address}: ${String(pkt.pkt.data.array, 0, pkt.pkt.data.contentSize,
                Charsets.UTF_8)}")
        }
    }
}