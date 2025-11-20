package com.tans.tfiletransfer.net.socket.ext.client

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.PackageDataWithAddress
import com.tans.tfiletransfer.net.socket.SocketException
import com.tans.tfiletransfer.net.socket.ext.Connection
import com.tans.tfiletransfer.net.socket.ext.converter.DefaultConverterFactory
import com.tans.tfiletransfer.net.socket.ext.converter.IConverterFactory
import com.tans.tfiletransfer.net.socket.udp.IUdpTask
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

internal class DefaultUdpClientManager(
    override val connection: Connection.UdpConnection,
    override val converterFactory: IConverterFactory = DefaultConverterFactory(),
) : BaseClientManager(), IUdpClientManager {

    override val connectionTask: IUdpTask = connection.connectionTask

    init {
        connectionTask.coroutineScope.launch {
            try {
                connectionTask.pktReadChannel()
                    .collect {
                        onResponseData(responsePkt = it.pkt, remoteAddress = it.address.address, remotePort = it.address.port)
                    }
            } catch (e: Throwable) {
                NetLog.e(TAG, "Read pkt fail: ${e.message}", e)
            }
        }
    }

    override suspend fun <Request : Any, Response : Any> request(
        requestType: Int,
        request: Request,
        requestClass: KClass<Request>,
        responseType: Int,
        responseClass: KClass<Response>,
        targetAddress: AddressWithPort,
        retryTimes: Int,
        retryTimeoutInMillis: Long
    ): Response  {
        return safeTask { cont ->
            val task = UdpTask(
                udpTargetAddress = targetAddress,
                requestType = requestType,
                messageId = generateMessageId(),
                request = request,
                requestClass = requestClass,
                responseType = responseType,
                responseClass = responseClass,
                retryTimes = retryTimes,
                retryTimeoutInMillis = retryTimeoutInMillis,
                callback = cont
            )
            task.run()
            cont.invokeOnCancellation {
                task.removeTaskForce()
            }
        }
    }

    override suspend fun <Request : Any> request(
        requestType: Int,
        request: Request,
        requestClass: KClass<Request>,
        targetAddress: AddressWithPort
    ) {
        val converter = converterFactory.findPackageConverter(
            type = requestType,
            dataTypeClass = requestClass
        )
        if (converter == null) {
            throw SocketException("Don't find converter for $requestType")
        }
        val pkt = converter.convert(
            type = requestType,
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
            throw SocketException("Request $requestType fail, connection task not active.")
        }
    }

    override val tag: String = TAG


    inner class UdpTask<Request: Any, Response: Any>(
        val udpTargetAddress: AddressWithPort,
        override val requestType: Int,
        override val messageId: Long,
        override val request: Request,
        override val requestClass: KClass<Request>,
        override val responseType: Int,
        override val responseClass: KClass<Response>,
        override val retryTimes: Int,
        override val retryTimeoutInMillis: Long,
        override val callback: CancellableContinuation<Response>,
        override val delay: Long = 0
    ) : Task<Request, Response>() {

        override suspend fun writeRequestPktData(pktData: PackageData): Boolean {
            return connectionTask.writePktData(PackageDataWithAddress(
                pkt = pktData,
                address = udpTargetAddress
            ))
        }

        override fun handleResponseData(
            responsePkt: PackageData,
            remoteAddress: String,
            remotePort: Int
        ): Boolean {
            return responsePkt.type == responseType && responsePkt.messageId == this.messageId && udpTargetAddress.address == remoteAddress
        }

        override fun retry() {
            UdpTask(
                udpTargetAddress = udpTargetAddress,
                requestType = requestType,
                messageId = messageId,
                request = request,
                requestClass = requestClass,
                responseType = responseType,
                responseClass = responseClass,
                retryTimes = retryTimes - 1,
                retryTimeoutInMillis = retryTimeoutInMillis,
                callback = callback,
                delay = DEFAULT_RETRY_DELAY
            ).run()
        }
    }

    companion object {
        private const val TAG = "DefaultUdpClientManager"
    }
}

fun IUdpTask.defaultClientManager(converterFactory: IConverterFactory = DefaultConverterFactory()): IUdpClientManager {
    return DefaultUdpClientManager(connection = Connection.UdpConnection(this), converterFactory = converterFactory)
}