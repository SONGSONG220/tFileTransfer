package com.tans.tfiletransfer.net.socket.ext.server

import com.tans.tfiletransfer.net.socket.ext.Connection
import com.tans.tfiletransfer.net.socket.tcp.ITcpClientTask

interface ITcpServerManager : IServerManager {
    override val connection: Connection.TcpConnection
    override val connectionTask: ITcpClientTask
}