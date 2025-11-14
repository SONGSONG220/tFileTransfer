package com.tans.tfiletransfer.net.socket.ext

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.ext.client.DefaultTcpClientManager
import com.tans.tfiletransfer.net.socket.ext.client.ITcpClientManager
import com.tans.tfiletransfer.net.socket.ext.converter.IConverterFactory
import com.tans.tfiletransfer.net.socket.ext.server.DefaultTcpServerManager
import com.tans.tfiletransfer.net.socket.ext.server.IServer
import com.tans.tfiletransfer.net.socket.ext.server.ITcpServerManager
import com.tans.tfiletransfer.net.socket.tcp.ITcpClientTask
import kotlin.reflect.KClass

internal class DefaultTcpServerClientManager(
    private val serverManager: ITcpServerManager,
    private val clientManager: ITcpClientManager,
) : ITcpServerClientManager {

    override val connection: Connection.TcpConnection = clientManager.connection
    override val connectionTask: ITcpClientTask = clientManager.connectionTask
    override val converterFactory: IConverterFactory = clientManager.converterFactory

    override fun <Request : Any, Response : Any> registerServer(s: IServer<Request, Response>) {
        serverManager.registerServer(s)
    }

    override fun <Request : Any, Response : Any> unregisterServer(s: IServer<Request, Response>) {
        serverManager.unregisterServer(s)
    }

    override suspend fun replyClient(
        pkt: PackageData,
        targetAddress: AddressWithPort
    ): Boolean {
        return serverManager.replyClient(pkt, targetAddress)
    }

    override fun clearAllServers() {
        serverManager.clearAllServers()
    }

    override suspend fun <Request : Any, Response : Any> request(
        requestType: Int,
        request: Request,
        requestClass: KClass<Request>,
        responseType: Int,
        responseClass: KClass<Response>,
        retryTimes: Int,
        retryTimeoutInMillis: Long
    ): Response {
        return clientManager.request(
            requestType = requestType,
            request = request,
            requestClass = requestClass,
            responseType = responseType,
            responseClass = responseClass,
            retryTimes = retryTimes,
            retryTimeoutInMillis = retryTimeoutInMillis,
        )
    }

    override suspend fun <Request : Any> request(
        requestType: Int,
        request: Request,
        requestClass: KClass<Request>
    ) {
        return clientManager.request(
            requestType = requestType,
            request = request,
            requestClass = requestClass
        )
    }
}

fun ITcpClientManager.defaultServerManager(): ITcpServerClientManager {
    return DefaultTcpServerClientManager(
        serverManager = DefaultTcpServerManager(connection, converterFactory),
        clientManager = this,
    )
}

fun ITcpServerManager.defaultClientManager(): ITcpServerClientManager {
    return DefaultTcpServerClientManager(
        serverManager = this,
        clientManager = DefaultTcpClientManager(
            connection,
            converterFactory
        ),
    )
}
