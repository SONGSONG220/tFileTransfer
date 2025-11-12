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
        retryTimeoutInMillis: Long = 1000L,
    ) : Response

    /**
     * For Tcp no response
     */
    suspend fun <Request : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>
    )

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
        retryTimeoutInMillis: Long = 1000L,
    ) : Response

    /**
     * For Udp no response
     */
    suspend fun <Request : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>,
        targetAddress: AddressWithPort,
    )
}


suspend inline fun <reified Request : Any, reified Response : Any> IClientManager.requestSimplify(
    type: Int,
    request: Request,
    retryTimes: Int = 2,
    retryTimeout: Long = 1000L,
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

suspend inline fun <reified Request : Any> IClientManager.requestSimplify(
    type: Int,
    request: Request,
) {
    request(
        type = type,
        request = request,
        requestClass = Request::class,
    )
}

suspend inline fun <reified Request : Any, reified Response : Any> IClientManager.requestSimplify(
    type: Int,
    request: Request,
    targetAddress: AddressWithPort,
    retryTimes: Int = 2,
    retryTimeout: Long = 1000L,
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

suspend inline fun <reified Request : Any> IClientManager.requestSimplify(
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


