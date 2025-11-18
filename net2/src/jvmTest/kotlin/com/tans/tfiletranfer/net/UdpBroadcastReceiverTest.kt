package com.tans.tfiletranfer.net

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.ext.server.defaultServerManager
import com.tans.tfiletransfer.net.socket.ext.server.server
import com.tans.tfiletransfer.net.socket.findLocalAddressV4
import com.tans.tfiletransfer.net.socket.getBroadcastAddressV4
import com.tans.tfiletransfer.net.socket.udp.UdpTask
import com.tans.tfiletransfer.net.transferproto.conn.broadcast.model.BroadcastMsg
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

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
        val serverManager = task.defaultServerManager()
        serverManager.registerServer(
            server<BroadcastMsg, Unit>(
                requestType = 0,
                responseType = -1,
            ) { _, remoteAddress, request, _ ->
                println("Receive BroadcastMsg from ${remoteAddress}: $request}")
                null
            }
        )
        task.startTask()
        task.state().filter { it is TaskState.Closed || it is TaskState.Error }.first()
    }
}