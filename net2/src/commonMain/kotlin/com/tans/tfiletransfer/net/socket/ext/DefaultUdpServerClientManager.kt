package com.tans.tfiletransfer.net.socket.ext

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ext.client.DefaultUdpClientManager
import com.tans.tfiletransfer.net.socket.ext.client.IUdpClientManager
import com.tans.tfiletransfer.net.socket.ext.converter.IConverterFactory
import com.tans.tfiletransfer.net.socket.ext.server.DefaultServerManager
import com.tans.tfiletransfer.net.socket.ext.server.IServer
import com.tans.tfiletransfer.net.socket.ext.server.IServerManager
import com.tans.tfiletransfer.net.socket.udp.IUdpTask
import kotlin.reflect.KClass

internal class DefaultUdpServerClientManager(
    private val serverManager: IServerManager,
    private val clientManager: IUdpClientManager,
) : IUdpServerClientManager {

    override val connection: Connection.UdpConnection = clientManager.connection
    override val connectionTask: IUdpTask = clientManager.connectionTask
    override val converterFactory: IConverterFactory = clientManager.converterFactory

    override fun <Request : Any, Response : Any> registerServer(s: IServer<Request, Response>) {
        serverManager.registerServer(s)
    }

    override fun <Request : Any, Response : Any> unregisterServer(s: IServer<Request, Response>) {
        serverManager.unregisterServer(s)
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
        targetAddress: AddressWithPort,
        retryTimes: Int,
        retryTimeoutInMillis: Long,
    ): Response {
        return clientManager.request(
            requestType = requestType,
            request = request,
            requestClass = requestClass,
            responseType = responseType,
            responseClass = responseClass,
            targetAddress = targetAddress,
            retryTimes = retryTimes,
            retryTimeoutInMillis = retryTimeoutInMillis,
        )
    }

    override suspend fun <Request : Any> request(
        requestType: Int,
        request: Request,
        requestClass: KClass<Request>,
        targetAddress: AddressWithPort
    ) {
        return clientManager.request(
            requestType = requestType,
            request = request,
            requestClass = requestClass,
            targetAddress = targetAddress,
        )
    }
}

fun IUdpClientManager.defaultServerManager(): IUdpServerClientManager {
    return DefaultUdpServerClientManager(
        serverManager = DefaultServerManager(connection, converterFactory),
        clientManager = this,
    )
}

fun IServerManager.defaultUdpClientManager(): IUdpServerClientManager {
    return DefaultUdpServerClientManager(
        serverManager = this,
        clientManager = DefaultUdpClientManager(
            connection as Connection.UdpConnection,
            converterFactory
        ),
    )
}