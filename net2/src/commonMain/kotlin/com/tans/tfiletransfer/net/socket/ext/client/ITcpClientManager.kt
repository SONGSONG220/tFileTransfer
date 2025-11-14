package com.tans.tfiletransfer.net.socket.ext.client

import com.tans.tfiletransfer.net.socket.ext.Connection
import com.tans.tfiletransfer.net.socket.ext.IConnectionManager
import com.tans.tfiletransfer.net.socket.tcp.ITcpClientTask
import kotlin.reflect.KClass

interface ITcpClientManager : IConnectionManager {
    override val connection: Connection.TcpConnection
    override val connectionTask: ITcpClientTask

    suspend fun <Request : Any, Response : Any> request(
        requestType: Int,
        request: Request,
        requestClass: KClass<Request>,
        responseType: Int,
        responseClass: KClass<Response>,
        retryTimes: Int = 2,
        retryTimeoutInMillis: Long = 1000L,
    ) : Response

    suspend fun <Request : Any> request(
        requestType: Int,
        request: Request,
        requestClass: KClass<Request>
    )
}


suspend inline fun <reified Request : Any, reified Response : Any> ITcpClientManager.requestSimplify(
    requestType: Int,
    request: Request,
    responseType: Int,
    retryTimes: Int = DEFAULT_RETRY_TIMES,
    retryTimeout: Long = DEFAULT_RETRY_TIMEOUT,
): Response {
    return request(
        requestType = requestType,
        request = request,
        requestClass = Request::class,
        responseType = responseType,
        responseClass = Response::class,
        retryTimes = retryTimes,
        retryTimeoutInMillis = retryTimeout,
    )
}

suspend inline fun <reified Request : Any> ITcpClientManager.requestSimplify(
    requestType: Int,
    request: Request,
) {
    request(
        requestType = requestType,
        request = request,
        requestClass = Request::class,
    )
}
