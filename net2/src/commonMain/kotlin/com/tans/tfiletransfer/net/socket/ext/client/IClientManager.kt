package com.tans.tfiletransfer.net.socket.ext.client

import com.tans.tfiletransfer.net.socket.AddressWithPort
import kotlin.reflect.KClass


interface IClientManager {

    /**
     * For Tcp
     */
    suspend fun <Request : Any, Response : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>,
        responseClass: KClass<Response>,
        retryTimes: Int = 2,
        retryTimeout: Long = 1000L,
    ) : Response?

    /**
     * For Udp
     */
    suspend fun <Request : Any, Response : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>,
        responseClass: KClass<Response>,
        targetAddress: AddressWithPort,
        retryTimes: Int = 2,
        retryTimeout: Long = 1000L,
    ) : Response?
}


suspend inline fun <reified Request : Any, reified Response : Any> IClientManager.requestSimplify(
    type: Int,
    request: Request,
    retryTimes: Int = 2,
    retryTimeout: Long = 1000L,
) {
    request(
        type = type,
        request = request,
        requestClass = Request::class,
        responseClass = Response::class,
        retryTimes = retryTimes,
        retryTimeout = retryTimeout,
    )
}

suspend inline fun <reified Request : Any, reified Response : Any> IClientManager.requestSimplify(
    type: Int,
    request: Request,
    targetAddress: AddressWithPort,
    retryTimes: Int = 2,
    retryTimeout: Long = 1000L,
) {
    request(
        type = type,
        request = request,
        requestClass = Request::class,
        responseClass = Response::class,
        targetAddress = targetAddress,
        retryTimes = retryTimes,
        retryTimeout = retryTimeout,
    )
}


