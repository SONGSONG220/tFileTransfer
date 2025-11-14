package com.tans.tfiletransfer.net.socket.ext.client

import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.PackageDataWithAddress
import com.tans.tfiletransfer.net.socket.SocketRuntimeException
import com.tans.tfiletransfer.net.socket.ext.Connection
import com.tans.tfiletransfer.net.socket.ext.converter.DefaultConverterFactory
import com.tans.tfiletransfer.net.socket.ext.converter.IConverterFactory
import com.tans.tfiletransfer.net.socket.udp.IUdpTask
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.reflect.KClass

internal class DefaultUdpClientManager(
    override val connection: Connection.UdpConnection,
    override val converterFactory: IConverterFactory = DefaultConverterFactory(),
) : BaseClientManager(connection), IUdpClientManager {

    override val connectionTask: IUdpTask = connection.connectionTask

    override suspend fun <Request : Any, Response : Any> request(
        type: Int,
        request: Request,
        requestClass: KClass<Request>,
        responseClass: KClass<Response>,
        targetAddress: AddressWithPort,
        retryTimes: Int,
        retryTimeoutInMillis: Long
    ): Response = suspendCancellableCoroutine { cont ->
        Task(
            type = type,
            messageId = generateMessageId(),
            udpTargetAddress = targetAddress,
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
        requestClass: KClass<Request>,
        targetAddress: AddressWithPort
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
        val ret = connectionTask.writePktData(
            pktDataWithAddress = PackageDataWithAddress(
                pkt = pkt,
                address = targetAddress
            )
        )
        if (!ret) {
            throw SocketRuntimeException("Request $type fail, connection task not active.")
        }
    }

    override val tag: String = TAG

    companion object {
        private const val TAG = "DefaultUdpClientManager"
    }
}

fun IUdpTask.defaultClientManager(converterFactory: IConverterFactory = DefaultConverterFactory()): IUdpClientManager {
    return DefaultUdpClientManager(connection = Connection.UdpConnection(this), converterFactory = converterFactory)
}