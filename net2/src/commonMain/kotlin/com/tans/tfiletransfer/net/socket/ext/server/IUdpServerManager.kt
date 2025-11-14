package com.tans.tfiletransfer.net.socket.ext.server

import com.tans.tfiletransfer.net.socket.ext.Connection
import com.tans.tfiletransfer.net.socket.udp.IUdpTask

interface IUdpServerManager : IServerManager {
    override val connection: Connection.UdpConnection
    override val connectionTask: IUdpTask
}