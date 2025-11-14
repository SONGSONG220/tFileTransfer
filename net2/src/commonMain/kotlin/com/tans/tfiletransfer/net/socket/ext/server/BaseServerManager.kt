package com.tans.tfiletransfer.net.socket.ext.server

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.IConnectionTask
import com.tans.tfiletransfer.net.socket.PackageData
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal abstract class BaseServerManager() : IServerManager {

    abstract val tag: String

    private val serversLock = Mutex()
    private val servers = mutableListOf<IServer<*, *>>()
    private val messageIdLock = Mutex()
    private val handledMessageId = mutableSetOf<Long>()
    private val messageIdRing = LongArray(MAX_TRACKED_MESSAGE_IDS)
    private var messageIdRingCount = 0
    private var messageIdRingStart = 0

    private suspend fun trackMessageId(id: Long): Boolean {
        return messageIdLock.withLock {
            val added = handledMessageId.add(id)
            if (added) {
                if (messageIdRingCount < MAX_TRACKED_MESSAGE_IDS) {
                    val idx = (messageIdRingStart + messageIdRingCount) % MAX_TRACKED_MESSAGE_IDS
                    messageIdRing[idx] = id
                    messageIdRingCount++
                } else {
                    val oldest = messageIdRing[messageIdRingStart]
                    handledMessageId.remove(oldest)
                    messageIdRing[messageIdRingStart] = id
                    messageIdRingStart = (messageIdRingStart + 1) % MAX_TRACKED_MESSAGE_IDS
                }
            }
            added
        }
    }

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
                    val isNew = trackMessageId(pkt.messageId)
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
                NetLog.e(tag, "Handle msg from $remoteAddress fail: localAddress=$localAddress, type=${pkt.type}, error=${e.message}", e)
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
        private const val MAX_TRACKED_MESSAGE_IDS = 4096
    }
}
