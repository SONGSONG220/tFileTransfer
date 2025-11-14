package com.tans.tfiletransfer.net.socket.tcp

import com.tans.tfiletransfer.net.socket.IConnectionTask
import com.tans.tfiletransfer.net.socket.PackageData
import io.ktor.network.sockets.Socket
import kotlinx.coroutines.channels.ReceiveChannel

interface ITcpClientTask : IConnectionTask {

    fun socket(): Socket?

    suspend fun writePktData(pkt: PackageData): Boolean

    fun pktReadChannel(): ReceiveChannel<PackageData>
}