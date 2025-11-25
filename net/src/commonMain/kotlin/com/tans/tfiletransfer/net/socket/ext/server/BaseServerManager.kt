package com.tans.tfiletransfer.net.socket.ext.server

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.collections.AtomicList
import com.tans.tfiletransfer.net.collections.AtomicSet
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.PackageData
import kotlinx.coroutines.launch

internal abstract class BaseServerManager() : IServerManager {

    abstract val tag: String

    private val servers = AtomicList<IServer<*, *>>()
    private val handledMessageId = AtomicSet<Long>()
    private val messageIdRing = AtomicList<Long>()

    private fun trackMessageId(id: Long): Boolean {
        val added = handledMessageId.add(id)
        if (added) {
            while (true) {
                val snapshot = messageIdRing.snapshot
                if (snapshot.size < MAX_TRACKED_MESSAGE_IDS) {
                    messageIdRing.add(id)
                    break
                } else {
                    val oldest = snapshot.first()
                    if (messageIdRing.remove(oldest)) {
                        handledMessageId.remove(oldest)
                        messageIdRing.add(id)
                        break
                    }
                }
            }
        }
        return added
    }

    protected fun onRequest(
        localAddress: AddressWithPort,
        remoteAddress: AddressWithPort,
        pkt: PackageData
    ) {
        connectionTask.coroutineScope.launch {
            try {
                val server = servers.snapshot.find { it.requestType == pkt.type }
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
            servers.add(s)
        }
    }

    override fun <Request : Any, Response : Any> unregisterServer(s: IServer<Request, Response>) {
        connectionTask.coroutineScope.launch {
            servers.remove(s)
        }
    }

    override fun clearAllServers() {
        connectionTask.coroutineScope.launch {
            servers.clear()
        }
    }

    companion object {
        private const val MAX_TRACKED_MESSAGE_IDS = 4096
    }
}
