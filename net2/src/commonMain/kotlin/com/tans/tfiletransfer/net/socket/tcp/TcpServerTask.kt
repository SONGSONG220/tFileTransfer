package com.tans.tfiletransfer.net.socket.tcp

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.IConnectionTask
import com.tans.tfiletransfer.net.socket.ConnectionTaskState
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ServerSocket
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class TcpServerTask(
    private val bindAddress: AddressWithPort,
    override val bufferPool: BufferPool = BufferPool(),
    val readWriteIdleLimitInMillis: Long = Long.MAX_VALUE
) : IConnectionTask {
    override val stateFlow: StateFlow<ConnectionTaskState> = MutableStateFlow(ConnectionTaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()

    private val selectorManager = SelectorManager()
    private var serverSocket: ServerSocket? = null

    private val clientTaskChannel: Channel<ClientTask> = Channel(10)
    private val clientTasks = mutableListOf<ClientTask>()

    override suspend fun onStartTask() {
        try {
            val serverSocket = aSocket(selectorManager)
                .tcp()
                .configure {
                    reuseAddress = true
                }
                .bind(bindAddress.address, bindAddress.port)
            updateStateExpect(
                expect = ConnectionTaskState.Connecting,
                update = ConnectionTaskState.Connected,
                fail = {
                    serverSocket.close()
                },
                success = {
                    this.serverSocket = serverSocket
                    NetLog.d(TAG, "Bind address $bindAddress success.")
                    waitingClients(serverSocket)
                })
        } catch (e: Throwable) {
            NetLog.e(TAG, "Bind address $bindAddress fail.", e)
            error(e)
        }
    }

    fun clientChannel(): Channel<ClientTask> = clientTaskChannel

    override suspend fun onStopTask(cause: String?) {
        NetLog.d(TAG, "Stop task: $cause")
        release()
    }

    override suspend fun onError(throwable: Throwable?) {
        release()
    }

    private fun release() {
        selectorManager.close()
        serverSocket?.close()
        serverSocket = null
        clientTaskChannel.close()
        for (t in this.clientTasks) {
            t.stopTask()
        }
        clientTasks.clear()
    }

    private fun waitingClients(serverSocket: ServerSocket) {
        coroutineScope.launch {
            try {
                while (true) {
                    val client = serverSocket.accept()
                    NetLog.d(TAG, "Coming new client: ${client.remoteAddress}")
                    val t = ClientTask(client)
                    t.startTask()
                    clientTasks.add(t)
                    clientTaskChannel.send(t)
                }
            } catch (e: Throwable) {
                NetLog.e(TAG, "Waiting socket error: ${e.message}", e)
                error(e)
            }
        }
    }

    inner class ClientTask(
        private val clientSocket: Socket
    ) : BaseTcpClientTask(this@TcpServerTask.readWriteIdleLimitInMillis) {

        override val bufferPool: BufferPool = this@TcpServerTask.bufferPool

        override val tag: String = CLIENT_TAG

        override suspend fun onStartTask() {
            super.onStartTask()
            updateStateExpect(
                expect = ConnectionTaskState.Connecting,
                update = ConnectionTaskState.Connected,
                fail = {
                    clientSocket.close()
                }
            ) {
                this.socket = clientSocket
                startRead(clientSocket)
                startWrite(clientSocket)
            }
        }
    }

    companion object {
        private const val TAG = "TcpServerTask"
        private const val CLIENT_TAG = "TcpServerTask_Client"
    }
}