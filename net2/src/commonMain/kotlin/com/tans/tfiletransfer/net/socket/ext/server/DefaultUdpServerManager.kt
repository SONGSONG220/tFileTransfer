package com.tans.tfiletransfer.net.socket.ext.server

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.PackageDataWithAddress
import com.tans.tfiletransfer.net.socket.ext.Connection
import com.tans.tfiletransfer.net.socket.ext.converter.DefaultConverterFactory
import com.tans.tfiletransfer.net.socket.ext.converter.IConverterFactory
import com.tans.tfiletransfer.net.socket.udp.IUdpTask
import io.ktor.network.sockets.ABoundSocket
import io.ktor.network.sockets.InetSocketAddress
import kotlinx.coroutines.launch

internal class DefaultUdpServerManager(
    override val connection: Connection.UdpConnection,
    override val converterFactory: IConverterFactory = DefaultConverterFactory(),
) : BaseServerManager(), IUdpServerManager {
    override val connectionTask: IUdpTask = connection.connectionTask

    init {
        connectionTask.coroutineScope.launch {
            try {
                connectionTask.pktReadChannel()
                    .collect { pkt ->
                        val socket: ABoundSocket? = connectionTask.socket() as? ABoundSocket
                        if (socket != null) {
                            val localAddress = (socket.localAddress as InetSocketAddress).let {
                                AddressWithPort(
                                    address = it.hostname,
                                    port = it.port
                                )
                            }
                            onRequest(
                                localAddress = localAddress,
                                remoteAddress = pkt.address,
                                pkt = pkt.pkt
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
        return connectionTask.writePktData(
            PackageDataWithAddress(
                pkt = pkt,
                address = targetAddress
            )
        )
    }

    companion object {
        private const val TAG = "DefaultUdpServerManager"
    }
}

fun IUdpTask.defaultServerManager(converterFactory: IConverterFactory = DefaultConverterFactory()): IUdpServerManager {
    return DefaultUdpServerManager(Connection.UdpConnection(this), converterFactory)
}