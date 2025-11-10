package com.tans.tfiletransfer.net.socket.ext.server

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ConnectionTask
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.PackageDataWithAddress
import com.tans.tfiletransfer.net.socket.ext.converter.IConverterFactory
import com.tans.tfiletransfer.net.socket.tcp.BaseTcpClientTask
import com.tans.tfiletransfer.net.socket.udp.UdpTask
import kotlin.reflect.KClass

private const val TAG = "IServer"

interface IServer<Request : Any, Response : Any> {

    val requestClass: KClass<Request>

    val responseClass: KClass<Response>

    val replyType: Int

    fun couldHandle(requestType: Int): Boolean

    suspend fun dispatchRequest(
        localAddress: AddressWithPort?,
        remoteAddress: AddressWithPort?,
        msg: PackageData,
        converterFactory: IConverterFactory,
        connectionTask: ConnectionTask,
        isNewRequest: Boolean
    ) {
        // 找到 request 的 body 转换器
        val converter = converterFactory.findBodyConverter(msg.type, requestClass)
        if (converter != null) {
            // 转换 request 的数据
            val convertedData = converter.convert(
                type = msg.type,
                dataClass = requestClass,
                packageData = msg,
                bufferPool = connectionTask.bufferPool
            )
            if (convertedData != null) {
                // 处理 request 的数据并获取 response
                val response = onRequest(localAddress, remoteAddress, convertedData, isNewRequest)
                if (response != null) {
                    // 找到 response 的 pkt 转换器
                    val responseConverter = converterFactory.findPackageDataConverter(replyType, responseClass)
                    if (responseConverter != null) {
                        // 转换 response 到 pkt
                        val pktData = responseConverter.convert(
                            type = replyType,
                            messageId = msg.messageId,
                            data = response,
                            dataClass = responseClass,
                            bufferPool = connectionTask.bufferPool
                        )
                        if (pktData != null) {
                            // 发送 response 数据
                            if (connectionTask is UdpTask) {
                                if (remoteAddress != null) {
                                    val ret = connectionTask.writePktData(
                                        pktDataWithAddress = PackageDataWithAddress(
                                            pkt = pktData,
                                            address = remoteAddress
                                        )
                                    )
                                    if (!ret) {
                                        NetLog.e(TAG, "Send udp replay fail.")
                                    }
                                }
                            } else if (connectionTask is BaseTcpClientTask) {
                                val ret = connectionTask.writePktData(pktData)
                                if (!ret) {
                                    NetLog.e(TAG, "Send tcp replay fail.")
                                }
                            }
                        } else {
                            NetLog.e(TAG, "${responseConverter::class.simpleName} convert $replyType fail.")
                        }
                    } else {
                        NetLog.e(TAG, "Didn't find converter $replyType, $responseClass")
                    }
                }
            } else {
                NetLog.e(TAG, "${converter::class.simpleName} convert $requestClass fail.")
            }
        } else {
            NetLog.e(TAG, "Didn't find converter ${msg.type}, $requestClass")
        }
    }

    suspend fun onRequest(
        localAddress: AddressWithPort?,
        remoteAddress: AddressWithPort?,
        r: Request,
        isNewRequest: Boolean
    ): Response?

}

inline fun <reified Request : Any, reified Response : Any> simplifyServer(
    requestType: Int,
    responseType: Int,
    crossinline onRequest: suspend (localAddress: AddressWithPort?, remoteAddress: AddressWithPort?, r: Request, isNewRequest: Boolean) -> Response?
): IServer<Request, Response> {
    return object : IServer<Request, Response> {

        override val requestClass: KClass<Request> = Request::class
        override val responseClass: KClass<Response> = Response::class
        override val replyType: Int = responseType

        override fun couldHandle(r: Int): Boolean = requestType == r

        override suspend fun onRequest(
            localAddress: AddressWithPort?,
            remoteAddress: AddressWithPort?,
            r: Request,
            isNewRequest: Boolean
        ): Response? {
            return onRequest(localAddress, remoteAddress, r, isNewRequest)
        }

    }
}