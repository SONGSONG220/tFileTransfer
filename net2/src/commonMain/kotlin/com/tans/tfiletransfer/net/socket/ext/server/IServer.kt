package com.tans.tfiletransfer.net.socket.ext.server

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ConnectionTask
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.PackageDataWithAddress
import com.tans.tfiletransfer.net.socket.SocketRuntimeException
import com.tans.tfiletransfer.net.socket.ext.converter.IConverterFactory
import com.tans.tfiletransfer.net.socket.tcp.BaseTcpClientTask
import com.tans.tfiletransfer.net.socket.udp.UdpTask
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
        converterFactory: IConverterFactory,
        connectionTask: ConnectionTask,
        isNewRequest: Boolean
    ) {
        // 找到 request 的 body 转换器
        val requestTypeConverter = converterFactory.findTypeConverter(requestPkt.type, requestClass)
        if (requestTypeConverter != null) {
            // 转换 request 的数据
            val convertedRequest = requestTypeConverter.convert(
                type = requestPkt.type,
                typeClass = requestClass,
                pkt = requestPkt,
                bufferPool = connectionTask.bufferPool
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
                    converterFactory.findPackageConverter(responseType, responseClass)
                if (responseConverter != null) {
                    // 转换 response 到 pkt
                    val responsePkt = responseConverter.convert(
                        type = responseType,
                        messageId = requestPkt.messageId,
                        data = response,
                        dataTypeClass = responseClass,
                        bufferPool = connectionTask.bufferPool
                    )
                    // 发送 response 数据
                    if (connectionTask is UdpTask) {
                        val ret = connectionTask.writePktData(
                            pktDataWithAddress = PackageDataWithAddress(
                                pkt = responsePkt,
                                address = remoteAddress
                            )
                        )
                        if (!ret) {
                            NetLog.e(
                                TAG,
                                "Send udp replay fail, requestType=$requestType, responseType=$responseType"
                            )
                        }
                    } else if (connectionTask is BaseTcpClientTask) {
                        val ret = connectionTask.writePktData(responsePkt)
                        if (!ret) {
                            NetLog.e(
                                TAG,
                                "Send tcp replay fail, requestType=$requestType, responseType=$responseType"
                            )
                        }
                    }
                }

            } else {
                throw SocketRuntimeException("Didn't find pkt converter responseType=$responseType, responseTypeClazz=$responseClass")
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