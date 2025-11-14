package com.tans.tfiletransfer.net.socket.ext.server

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.SocketRuntimeException
import kotlin.reflect.KClass

private const val TAG = "IServer"

interface IServer<Request : Any, Response : Any> {

    val requestClass: KClass<Request>
    val responseClass: KClass<Response>
    val requestType: Int
    val responseType: Int

    suspend fun dispatchRequest(
        localAddress: AddressWithPort,
        remoteAddress: AddressWithPort,
        requestPkt: PackageData,
        serverManager: IServerManager,
        isNewRequest: Boolean
    ) {
        // 找到 request 的 body 转换器
        val requestTypeConverter = serverManager.converterFactory.findTypeConverter(requestPkt.type, requestClass)
        if (requestTypeConverter != null) {
            // 转换 request 的数据
            val convertedRequest = requestTypeConverter.convert(
                type = requestPkt.type,
                typeClass = requestClass,
                pkt = requestPkt,
                bufferPool = serverManager.connectionTask.bufferPool
            )
            // 处理 request 的数据并获取 response
            val response = onRequest(
                localAddress = localAddress,
                remoteAddress = remoteAddress,
                r = convertedRequest,
                isNewRequest = isNewRequest
            )
            if (response != null) {
                // 找到 response 的 pkt 转换器
                val responseConverter =
                    serverManager.converterFactory.findPackageConverter(responseType, responseClass)
                if (responseConverter != null) {
                    // 转换 response 到 pkt
                    val responsePkt = responseConverter.convert(
                        type = responseType,
                        messageId = requestPkt.messageId,
                        data = response,
                        dataTypeClass = responseClass,
                        bufferPool = serverManager.connectionTask.bufferPool
                    )
                    val ret = serverManager.replyClient(responsePkt, remoteAddress)

                    if (!ret) {
                        NetLog.e(
                            TAG,
                            "Reply client fail, requestType=$requestType, responseType=$responseType"
                        )
                    }
                }

            }
        } else {
            throw SocketRuntimeException("Didn't find request type converter requestType=$requestType, requestTypeClazz=$requestClass")
        }
    }

    suspend fun onRequest(
        localAddress: AddressWithPort,
        remoteAddress: AddressWithPort,
        r: Request,
        isNewRequest: Boolean
    ): Response?

}

inline fun <reified Request : Any, reified Response : Any> server(
    requestType: Int,
    responseType: Int,
    crossinline onRequestP: suspend (localAddress: AddressWithPort, remoteAddress: AddressWithPort, r: Request, isNewRequest: Boolean) -> Response?
): IServer<Request, Response> {
    return object : IServer<Request, Response> {

        override val requestClass: KClass<Request> = Request::class
        override val responseClass: KClass<Response> = Response::class
        override val requestType: Int = requestType
        override val responseType: Int = responseType

        override suspend fun onRequest(
            localAddress: AddressWithPort,
            remoteAddress: AddressWithPort,
            r: Request,
            isNewRequest: Boolean
        ): Response? {
            return onRequestP(localAddress, remoteAddress, r, isNewRequest)
        }

    }
}