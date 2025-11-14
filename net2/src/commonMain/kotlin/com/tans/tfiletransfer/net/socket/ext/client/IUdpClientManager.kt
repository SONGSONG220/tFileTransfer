package com.tans.tfiletransfer.net.socket.ext.client

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ext.Connection
import com.tans.tfiletransfer.net.socket.ext.IConnectionManager
import com.tans.tfiletransfer.net.socket.udp.IUdpTask
import kotlin.reflect.KClass

interface IUdpClientManager : IConnectionManager {
    override val connection: Connection.UdpConnection
    override val connectionTask: IUdpTask

    suspend fun <Request : Any, Response : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>,
        responseClass: KClass<Response>,
        targetAddress: AddressWithPort,
        retryTimes: Int = 2,
        retryTimeoutInMillis: Long = 1000L,
    ) : Response

    suspend fun <Request : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>,
        targetAddress: AddressWithPort,
    )
}

suspend inline fun <reified Request : Any, reified Response : Any> IUdpClientManager.requestSimplify(
    type: Int,
    request: Request,
    targetAddress: AddressWithPort,
    retryTimes: Int = DEFAULT_RETRY_TIMES,
    retryTimeout: Long = DEFAULT_RETRY_TIMEOUT,
): Response {
    return request(
        type = type,
        request = request,
        requestClass = Request::class,
        responseClass = Response::class,
        targetAddress = targetAddress,
        retryTimes = retryTimes,
        retryTimeoutInMillis = retryTimeout,
    )
}

suspend inline fun <reified Request : Any> IUdpClientManager.requestSimplify(
    type: Int,
    request: Request,
    targetAddress: AddressWithPort,
) {
    request(
        type = type,
        request = request,
        requestClass = Request::class,
        targetAddress = targetAddress
    )
}