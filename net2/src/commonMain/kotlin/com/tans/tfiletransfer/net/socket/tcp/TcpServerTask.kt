package com.tans.tfiletransfer.net.socket.tcp

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.SocketException
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TcpServerTask(
    private val bindAddress: AddressWithPort,
    override val bufferPool: BufferPool = BufferPool(),
    val readWriteIdleLimitInMillis: Long = Long.MAX_VALUE
) : ITcpServerTask {
    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()

    private val selectorManager = SelectorManager(Dispatchers.IO)
    private var serverSocket: ServerSocket? = null

    private val clientTaskChannel: Channel<ClientTask> = Channel(10)
    private val clientTasks = mutableListOf<ClientTask>()
    private val clientTasksMutex: Mutex = Mutex()

    override suspend fun onStartTask() {
        try {
            val serverSocket = aSocket(selectorManager)
                .tcp()
                .bind(bindAddress.address, bindAddress.port) {
                    reuseAddress = true
                }
            this.serverSocket = serverSocket
            updateStateExpect(
                expect = TaskState.Connecting,
                update = TaskState.Connected,
                fail = {
                    runCatching {
                        serverSocket.close()
                    }
                    this.serverSocket = null
                },
                success = {
                    NetLog.d(TAG, "Bind address $bindAddress success.")
                    waitingClients(serverSocket)
                })
        } catch (e: Throwable) {
            error(SocketException("Bind address $bindAddress fail.", e))
        }
    }

    override fun clientChannel(): Channel<ClientTask> = clientTaskChannel

    override suspend fun onStopTask(cause: String?) {
        NetLog.d(TAG, "Stop task: $cause")
        release()
    }

    override suspend fun onError(throwable: Throwable?) {
        NetLog.e(TAG, throwable?.message ?: "Unknown error", throwable)
        release()
    }

    private suspend fun release() {
        runCatching {
            serverSocket?.close()
        }
        serverSocket = null
        selectorManager.close()
        clientTaskChannel.close()
        clientTasksMutex.withLock {
            for (t in clientTasks) {
                t.stopTask()
            }
            clientTasks.clear()
        }
    }

    private fun waitingClients(serverSocket: ServerSocket) {
        coroutineScope.launch {
            try {
                while (true) {
                    val client = serverSocket.accept()
                    NetLog.d(TAG, "Coming new client: ${client.remoteAddress}")
                    val t = ClientTask(client)
                    t.startTask()
                    clientTasksMutex.withLock {
                        clientTasks.add(t)
                    }
                    clientTaskChannel.send(t)
                    coroutineScope.launch {
                        try {
                            t.state().first { it is TaskState.Closed || it is TaskState.Error }
                            clientTasksMutex.withLock {
                                clientTasks.remove(t)
                            }
                        } catch (_: Throwable) {
                        }
                    }
                }
            } catch (e: Throwable) {
                error(SocketException("Waiting socket error: ${e.message}", e))
            }
        }
    }

    inner class ClientTask(
        private val clientSocket: Socket
    ) : BaseTcpClientTask(this@TcpServerTask.readWriteIdleLimitInMillis) {

        override val bufferPool: BufferPool = this@TcpServerTask.bufferPool
        override val selectorManager: SelectorManager? = null
        override val tag: String = CLIENT_TAG

        override suspend fun onStartTask() {
            super.onStartTask()
            this.socket = clientSocket
            updateStateExpect(
                expect = TaskState.Connecting,
                update = TaskState.Connected,
                fail = {
                    this.socket = null
                    runCatching {
                        clientSocket.close()
                    }
                }
            ) {
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