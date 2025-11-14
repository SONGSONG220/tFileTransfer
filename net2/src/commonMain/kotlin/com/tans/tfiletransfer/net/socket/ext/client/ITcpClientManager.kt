package com.tans.tfiletransfer.net.socket.ext.client

import com.tans.tfiletransfer.net.socket.ext.Connection
import com.tans.tfiletransfer.net.socket.ext.IConnectionManager
import com.tans.tfiletransfer.net.socket.tcp.ITcpClientTask
import kotlin.reflect.KClass

interface ITcpClientManager : IConnectionManager {
    override val connection: Connection.TcpConnection
    override val connectionTask: ITcpClientTask

    suspend fun <Request : Any, Response : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>,
        responseClass: KClass<Response>,
        retryTimes: Int = 2,
        retryTimeoutInMillis: Long = 1000L,
    ) : Response


    suspend fun <Request : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>
    )
}


suspend inline fun <reified Request : Any, reified Response : Any> ITcpClientManager.requestSimplify(
    type: Int,
    request: Request,
    retryTimes: Int = DEFAULT_RETRY_TIMES,
    retryTimeout: Long = DEFAULT_RETRY_TIMEOUT,
): Response {
    return request(
        type = type,
        request = request,
        requestClass = Request::class,
        responseClass = Response::class,
        retryTimes = retryTimes,
        retryTimeoutInMillis = retryTimeout,
    )
}

suspend inline fun <reified Request : Any> ITcpClientManager.requestSimplify(
    type: Int,
    request: Request,
) {
    request(
        type = type,
        request = request,
        requestClass = Request::class,
    )
}
