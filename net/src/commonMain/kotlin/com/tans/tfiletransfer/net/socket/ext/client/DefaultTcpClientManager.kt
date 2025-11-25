package com.tans.tfiletransfer.net.socket.ext.client

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.SocketException
import com.tans.tfiletransfer.net.socket.ext.Connection
import com.tans.tfiletransfer.net.socket.ext.converter.DefaultConverterFactory
import com.tans.tfiletransfer.net.socket.ext.converter.IConverterFactory
import com.tans.tfiletransfer.net.socket.tcp.ITcpClientTask
import com.tans.tfiletransfer.net.socket.toAddress
import io.ktor.network.sockets.InetSocketAddress
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

internal class DefaultTcpClientManager(
    override val connection: Connection.TcpConnection,
    override val converterFactory: IConverterFactory = DefaultConverterFactory(),
) : BaseClientManager(), ITcpClientManager {

    override val connectionTask: ITcpClientTask = connection.connectionTask


    init {
        connectionTask.coroutineScope.launch {
            try {
                var remoteAddress: AddressWithPort? = null
                connectionTask.pktReadChannel()
                    .collect {
                        if (remoteAddress == null) {
                            val inetAddress = (connection.connectionTask.socket()!!.remoteAddress as InetSocketAddress)
                            remoteAddress = AddressWithPort(inetAddress.toAddress(), inetAddress.port)
                        }
                        onResponseData(it, remoteAddress.address, remoteAddress.port)
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
        retryTimes: Int,
        retryTimeoutInMillis: Long
    ): Response {
        return safeTask { cont ->
            val task = TcpTask(
                requestType = requestType,
                messageId = generateMessageId(),
                request = request,
                requestClass = requestClass,
                responseType = responseType,
                responseClass = responseClass,
                retryTimes = retryTimes,
                maxRetryTimes = retryTimes,
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
        requestClass: KClass<Request>
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
        val ret = connectionTask.writePktData(pkt)
        if (!ret) {
            throw SocketException("Request $requestType fail, connection task not active.")
        }
    }

    inner class TcpTask<Request: Any, Response: Any>(
        override val requestType: Int,
        override val messageId: Long,
        override val request: Request,
        override val requestClass: KClass<Request>,
        override val responseType: Int,
        override val responseClass: KClass<Response>,
        override val retryTimes: Int,
        override val maxRetryTimes: Int,
        override val retryTimeoutInMillis: Long,
        override val callback: CancellableContinuation<Response>,
        override val delay: Long = 0
    ) : Task<Request, Response>() {

        override suspend fun writeRequestPktData(pktData: PackageData): Boolean {
            return connectionTask.writePktData(pktData)
        }

        override fun handleResponseData(
            responsePkt: PackageData,
            remoteAddress: String,
            remotePort: Int
        ): Boolean {
            return responsePkt.type == responseType && responsePkt.messageId == this.messageId
        }

        override fun retry(delay: Long) {
            TcpTask(
                requestType = requestType,
                messageId = messageId,
                request = request,
                requestClass = requestClass,
                responseType = responseType,
                responseClass = responseClass,
                retryTimes = retryTimes - 1,
                maxRetryTimes = maxRetryTimes,
                retryTimeoutInMillis = retryTimeoutInMillis,
                callback = callback,
                delay = delay
            ).run()
        }
    }

    override val tag: String = TAG

    companion object {
        private const val TAG = "DefaultTcpClientManager"
    }
}

fun ITcpClientTask.defaultClientManager(converterFactory: IConverterFactory = DefaultConverterFactory()): ITcpClientManager {
    return DefaultTcpClientManager(connection = Connection.TcpConnection(this), converterFactory = converterFactory)
}