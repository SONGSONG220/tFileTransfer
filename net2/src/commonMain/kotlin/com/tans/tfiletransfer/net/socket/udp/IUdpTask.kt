package com.tans.tfiletransfer.net.socket.udp

import com.tans.tfiletransfer.net.socket.IConnectionTask
import com.tans.tfiletransfer.net.socket.PackageDataWithAddress
import io.ktor.network.sockets.ASocket
import kotlinx.coroutines.channels.ReceiveChannel

interface IUdpTask : IConnectionTask {

    fun socket(): ASocket?

    suspend fun writePktData(pktDataWithAddress: PackageDataWithAddress): Boolean

    fun pktReadChannel(): ReceiveChannel<PackageDataWithAddress>
}