package com.tans.tfiletransfer.net.socket.tcp

import com.tans.tfiletransfer.net.socket.IConnectionTask
import com.tans.tfiletransfer.net.socket.tcp.TcpServerTask.ClientTask
import kotlinx.coroutines.channels.ReceiveChannel

interface ITcpServerTask : IConnectionTask {
    fun clientChannel(): ReceiveChannel<ClientTask>
}