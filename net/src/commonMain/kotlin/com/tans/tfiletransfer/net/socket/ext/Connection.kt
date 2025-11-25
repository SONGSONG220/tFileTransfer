package com.tans.tfiletransfer.net.socket.ext

import com.tans.tfiletransfer.net.socket.IConnectionTask
import com.tans.tfiletransfer.net.socket.tcp.ITcpClientTask
import com.tans.tfiletransfer.net.socket.udp.IUdpTask

sealed class Connection {

    abstract val connectionTask: IConnectionTask

    class TcpConnection(override val connectionTask: ITcpClientTask) : Connection()
    class UdpConnection(override val connectionTask: IUdpTask) : Connection()
}