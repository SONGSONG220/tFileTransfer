package com.tans.tfiletransfer.net.socket.ext.client

import com.tans.tfiletransfer.net.socket.SocketRuntimeException
import com.tans.tfiletransfer.net.socket.ext.Connection
import com.tans.tfiletransfer.net.socket.ext.converter.DefaultConverterFactory
import com.tans.tfiletransfer.net.socket.ext.converter.IConverterFactory
import com.tans.tfiletransfer.net.socket.tcp.ITcpClientTask
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.reflect.KClass

internal class DefaultTcpClientManager(
    override val connection: Connection.TcpConnection,
    override val converterFactory: IConverterFactory = DefaultConverterFactory(),
) : BaseClientManager(connection), ITcpClientManager {

    override val connectionTask: ITcpClientTask = connection.connectionTask

    override suspend fun <Request : Any, Response : Any> request(
        requestType: Int,
        request: Request,
        requestClass: KClass<Request>,
        responseType: Int,
        responseClass: KClass<Response>,
        retryTimes: Int,
        retryTimeoutInMillis: Long
    ): Response = suspendCancellableCoroutine { cont ->
        Task(
            requestType = requestType,
            messageId = generateMessageId(),
            udpTargetAddress = null,
            request = request,
            requestClass = requestClass,
            responseType = responseType,
            responseClass = responseClass,
            retryTimes = retryTimes,
            retryTimeoutInMillis = retryTimeoutInMillis,
            callback = cont
        ).run()
    }

    override suspend fun <Request : Any> request(
        requestType: Int,
        request: Request,
        requestClass: KClass<Request>
    ) {
        val converter = converterFactory.findPackageConverter(
            type = requestType,
            dataTypeClass = requestClass
        )
        if (converter == null) {
            throw SocketRuntimeException("Don't find converter for $requestType")
        }
        val pkt = converter.convert(
            type = requestType,
            messageId = generateMessageId(),
            data = request,
            dataTypeClass = requestClass,
            bufferPool = connectionTask.bufferPool
        )
        val ret = connectionTask.writePktData(pkt)
        if (!ret) {
            throw SocketRuntimeException("Request $requestType fail, connection task not active.")
        }
    }

    override val tag: String = TAG


    companion object {
        const val TAG = "DefaultTcpClientManager"
    }
}

fun ITcpClientTask.defaultClientManager(converterFactory: IConverterFactory = DefaultConverterFactory()): ITcpClientManager {
    return DefaultTcpClientManager(connection = Connection.TcpConnection(this), converterFactory = converterFactory)
}