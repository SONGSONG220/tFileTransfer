package com.tans.tfiletransfer.net.socket.ext

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.IConnectionTask
import com.tans.tfiletransfer.net.socket.ext.client.DefaultClientManager
import com.tans.tfiletransfer.net.socket.ext.client.IClientManager
import com.tans.tfiletransfer.net.socket.ext.converter.IConverterFactory
import com.tans.tfiletransfer.net.socket.ext.server.DefaultServerManager
import com.tans.tfiletransfer.net.socket.ext.server.IServer
import com.tans.tfiletransfer.net.socket.ext.server.IServerManager
import kotlin.reflect.KClass

class DefaultServerClientManager(
    val serverManager: IServerManager,
    val clientManager: IClientManager
) : IServerClientManager {
    override val connectionTask: IConnectionTask = serverManager.connectionTask
    override val converterFactory: IConverterFactory = serverManager.converterFactory

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
        type: Int,
        request: Request,
        requestClass: KClass<Request>,
        responseClass: KClass<Response>,
        retryTimes: Int,
        retryTimeoutInMillis: Long
    ): Response {
        return clientManager.request(
            type = type,
            request = request,
            requestClass = requestClass,
            responseClass = responseClass,
            retryTimes = retryTimes,
            retryTimeoutInMillis = retryTimeoutInMillis
        )
    }

    override suspend fun <Request : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>
    ) {
        return clientManager.request(
            type = type,
            request = request,
            requestClass = requestClass
        )
    }

    override suspend fun <Request : Any, Response : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>,
        responseClass: KClass<Response>,
        targetAddress: AddressWithPort,
        retryTimes: Int,
        retryTimeoutInMillis: Long
    ): Response {
        return clientManager.request(
            type = type,
            request = request,
            requestClass = requestClass,
            responseClass = responseClass,
            targetAddress = targetAddress,
            retryTimes = retryTimes,
            retryTimeoutInMillis = retryTimeoutInMillis
        )
    }

    override suspend fun <Request : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>,
        targetAddress: AddressWithPort
    ) {
        return clientManager.request(
            type = type,
            request = request,
            requestClass = requestClass,
            targetAddress = targetAddress
        )
    }
}

fun IServerManager.defaultClientManager(): IServerClientManager {
    return this as? IServerClientManager
        ?: DefaultServerClientManager(
            serverManager = this,
            clientManager = DefaultClientManager(
                connectionTask = connectionTask,
                converterFactory = converterFactory
            )
        )
}

fun IClientManager.defaultServerManager(): IServerClientManager {
    return this as? IServerClientManager
        ?: DefaultServerClientManager(
            serverManager = DefaultServerManager(
                connectionTask = connectionTask,
                converterFactory = converterFactory
            ),
            clientManager = this
        )
}