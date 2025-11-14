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
        type: Int,
        request: Request,
        requestClass: KClass<Request>,
        responseClass: KClass<Response>,
        retryTimes: Int,
        retryTimeoutInMillis: Long
    ): Response = suspendCancellableCoroutine { cont ->
        Task(
            type = type,
            messageId = generateMessageId(),
            udpTargetAddress = null,
            request = request,
            requestClass = requestClass,
            responseClass = responseClass,
            retryTimes = retryTimes,
            retryTimeoutInMillis = retryTimeoutInMillis,
            callback = cont
        ).run()
    }

    override suspend fun <Request : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>
    ) {
        val converter = converterFactory.findPackageConverter(
            type = type,
            dataTypeClass = requestClass
        )
        if (converter == null) {
            throw SocketRuntimeException("Don't find converter for $type")
        }
        val pkt = converter.convert(
            type = type,
            messageId = generateMessageId(),
            data = request,
            dataTypeClass = requestClass,
            bufferPool = connectionTask.bufferPool
        )
        val ret = connectionTask.writePktData(pkt)
        if (!ret) {
            throw SocketRuntimeException("Request $type fail, connection task not active.")
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