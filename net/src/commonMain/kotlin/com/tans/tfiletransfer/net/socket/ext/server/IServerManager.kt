package com.tans.tfiletransfer.net.socket.ext.server

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.ext.IConnectionManager

interface IServerManager : IConnectionManager {

    fun <Request : Any, Response : Any> registerServer(s: IServer<Request, Response>)

    fun <Request : Any, Response : Any> unregisterServer(s: IServer<Request, Response>)

    suspend fun replyClient(pkt: PackageData, targetAddress: AddressWithPort): Boolean

    fun clearAllServers()
}