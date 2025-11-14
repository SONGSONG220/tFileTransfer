package com.tans.tfiletransfer.net.socket.ext.server

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.ext.Connection
import com.tans.tfiletransfer.net.socket.ext.converter.DefaultConverterFactory
import com.tans.tfiletransfer.net.socket.ext.converter.IConverterFactory
import com.tans.tfiletransfer.net.socket.tcp.ITcpClientTask
import io.ktor.network.sockets.InetSocketAddress
import kotlinx.coroutines.launch

internal class DefaultTcpServerManager(
    override val connection: Connection.TcpConnection,
    override val converterFactory: IConverterFactory = DefaultConverterFactory(),
) : BaseServerManager(), ITcpServerManager {
    override val connectionTask: ITcpClientTask = connection.connectionTask

    init {
        connectionTask.coroutineScope.launch {
            try {
                connectionTask.pktReadChannel()
                    .collect { pkt ->
                        val socket = connectionTask.socket()
                        if (socket != null) {
                            val localAddress = (socket.localAddress as InetSocketAddress).let {
                                AddressWithPort(
                                    address = it.hostname,
                                    port = it.port
                                )
                            }
                            val remoteAddress = (socket.remoteAddress as InetSocketAddress).let {
                                AddressWithPort(
                                    address = it.hostname,
                                    port = it.port
                                )
                            }
                            onRequest(
                                localAddress = localAddress,
                                remoteAddress = remoteAddress,
                                pkt = pkt
                            )
                        }
                    }
            } catch (e: Throwable) {
                NetLog.e(TAG, "Read pkt error: ${e.message}", e)
            }
        }
    }

    override suspend fun replyClient(
        pkt: PackageData,
        targetAddress: AddressWithPort
    ): Boolean {
        return connectionTask.writePktData(pkt)
    }

    companion object {
        private const val TAG = "DefaultTcpServerManager"
    }
}

fun ITcpClientTask.defaultServerManager(converterFactory: IConverterFactory = DefaultConverterFactory()): ITcpServerManager {
    return DefaultTcpServerManager(Connection.TcpConnection(this), converterFactory)
}
