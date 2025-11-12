package com.tans.tfiletransfer.net.socket.ext.server

interface IServerManager {

    fun <Request : Any, Response : Any> registerServer(s: IServer<Request, Response>)

    fun <Request : Any, Response : Any> unregisterServer(s: IServer<Request, Response>)

    fun clearAllServers()
}