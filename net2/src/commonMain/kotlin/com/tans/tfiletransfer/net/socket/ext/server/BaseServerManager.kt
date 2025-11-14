package com.tans.tfiletransfer.net.socket.ext.server

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.IConnectionTask
import com.tans.tfiletransfer.net.socket.PackageData
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal abstract class BaseServerManager() : IServerManager {

    override val connectionTask: IConnectionTask = connection.connectionTask

    private val serversLock = Mutex()
    private val servers = mutableListOf<IServer<*, *>>()
    private val messageIdLock = Mutex()
    private val handledMessageId = mutableSetOf<Long>()

    protected fun onRequest(
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
                        serverManager = this@BaseServerManager,
                        isNewRequest = isNew
                    )
                } else {
                    // NetLog.w(TAG, "Don't find server to handle ${pkt.type} message.")
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
