package com.tans.tfiletransfer.net.socket.ext.server

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ConnectionTask
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.ext.converter.DefaultConverterFactory
import com.tans.tfiletransfer.net.socket.ext.converter.IConverterFactory
import com.tans.tfiletransfer.net.socket.tcp.BaseTcpClientTask
import com.tans.tfiletransfer.net.socket.udp.UdpTask
import io.ktor.network.sockets.ABoundSocket
import io.ktor.network.sockets.InetSocketAddress
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultServerManager(
    val connectionTask: ConnectionTask,
    private val converterFactory: IConverterFactory = DefaultConverterFactory(),
) : IServerManager {
    private val serversLock = Mutex()
    private val servers = mutableListOf<IServer<*, *>>()
    private val messageIdLock = Mutex()
    private val handledMessageId = mutableSetOf<Long>()

    init {
        if (connectionTask is BaseTcpClientTask) {
            connectionTask.coroutineScope.launch {
                try {
                    for (pkt in connectionTask.pktReadChannel()) {
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
                            onNewMessage(
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
        if (connectionTask is UdpTask) {
            connectionTask.coroutineScope.launch {
                try {
                    for (pkt in connectionTask.pktReadChannel()) {
                        val socket = connectionTask.socket() as? ABoundSocket
                        if (socket != null) {
                            val localAddress = (socket.localAddress as InetSocketAddress).let {
                                AddressWithPort(
                                    address = it.hostname,
                                    port = it.port
                                )
                            }
                            onNewMessage(
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
    }

    private fun onNewMessage(
        localAddress: AddressWithPort,
        remoteAddress: AddressWithPort,
        pkt: PackageData
    ) {
        connectionTask.coroutineScope.launch {
            try {
                val server = serversLock.withLock {
                    servers.find { it.requestType == pkt.type }
                }
                if (server != null) {
                    val isNew = messageIdLock.withLock {
                        handledMessageId.add(pkt.messageId)
                    }
                    server.dispatchRequest(
                        localAddress = localAddress,
                        remoteAddress = remoteAddress,
                        requestPkt = pkt,
                        converterFactory = converterFactory,
                        connectionTask = connectionTask,
                        isNewRequest = isNew
                    )
                } else {
                    NetLog.e(TAG, "Don't find server to handle ${pkt.type} message.")
                }
            } catch (e: Throwable) {
                NetLog.e(TAG, "Handle msg from $remoteAddress fail: localAddress=$localAddress, type=${pkt.type}, error=${e.message}", e)
            }

        }
    }

    override fun <Request : Any, Response : Any> registerServer(s: IServer<Request, Response>) {
        connectionTask.coroutineScope.launch {
            serversLock.withLock {
                servers.add(s)
            }
        }
    }

    override fun <Request : Any, Response : Any> unregisterServer(s: IServer<Request, Response>) {
        connectionTask.coroutineScope.launch {
            serversLock.withLock {
                servers.remove(s)
            }
        }
    }

    override fun clearAllServers() {
        connectionTask.coroutineScope.launch {
            serversLock.withLock {
                servers.clear()
            }
        }
    }

    companion object {
        private const val TAG = "DefaultServerManager"
    }
}