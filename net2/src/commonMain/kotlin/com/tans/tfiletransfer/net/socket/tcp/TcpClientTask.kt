package com.tans.tfiletransfer.net.socket.tcp

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ConnectionTask
import com.tans.tfiletransfer.net.socket.ConnectionTaskState
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readInt
import io.ktor.utils.io.readLong
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeInt
import io.ktor.utils.io.writeLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class TcpClientTask(
    private val serverAddress: AddressWithPort,
    private val bufferPool: BufferPool = BufferPool()
) : ConnectionTask {
    override val stateFlow: StateFlow<ConnectionTaskState> = MutableStateFlow(ConnectionTaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()

    private val selector = SelectorManager(Dispatchers.IO)

    private val pktReadChannel: Channel<PackageData> = Channel(10)

    private val pktWriteChannel: Channel<PackageData> = Channel(10)

    private var socket: Socket? = null

    override suspend fun onStartTask() {
        try {
            val socket = aSocket(selector)
                .tcp()
                .configure {
                    reuseAddress = true
                }
                .connect(serverAddress.address, serverAddress.port)
            updateStateExpect(
                expect = ConnectionTaskState.Connecting,
                update = ConnectionTaskState.Connected,
                fail = {
                    socket.close()
                },
                success = {
                    NetLog.d(TAG, "Connect to server $serverAddress success.")
                    this.socket = socket
                    readSocketData(socket)
                    waitingWriteSocketData(socket)
                }
            )
        } catch (e: Throwable) {
            NetLog.e(TAG, "Connect to server $serverAddress fail: ${e.message}", e)
            error(e)
        }

    }

    override suspend fun onStopTask(cause: String?) {
        NetLog.d(TAG, "Task closed.")
        release()
    }

    override suspend fun onError(throwable: Throwable?) {
        release()
    }

    fun pktReadChannel(): Channel<PackageData> = pktReadChannel

    suspend fun writePktData(pkt: PackageData): Boolean {
        return if (currentState() == ConnectionTaskState.Connected) {
            pktWriteChannel.send(pkt)
            true
        } else {
            false
        }
    }

    private fun readSocketData(socket: Socket) {
        coroutineScope.launch {
            try {
                val readChannel = socket.openReadChannel()
                while (true) {
                    val pktLen = readChannel.readInt()
                    val type = readChannel.readInt()
                    val msgId = readChannel.readLong()
                    val dataLen = pktLen - 4 - 8
                    val data = bufferPool.get(dataLen)
                    readChannel.readAvailable(buffer = data.array, offset = 0, length = dataLen)
                    data.contentSize = dataLen
                    pktReadChannel.send(
                        PackageData(
                            type = type,
                            messageId = msgId,
                            data = data
                        )
                    )
                }
            } catch (e: Throwable) {
                NetLog.e(TAG, "Read chanel error: ${e.message}", e)
                error(e)
            }
        }
    }

    private fun waitingWriteSocketData(socket: Socket) {
        coroutineScope.launch {
            try {
                val writeChannel = socket.openWriteChannel(true)
                for (pkt in pktWriteChannel) {
                    val pktLen = pkt.data.contentSize + 4 + 8
                    writeChannel.writeInt(pktLen)
                    writeChannel.writeInt(pkt.type)
                    writeChannel.writeLong(pkt.messageId)
                    writeChannel.writeFully(pkt.data.array, 0, pkt.data.contentSize)
                    writeChannel.flush()
                    bufferPool.put(pkt.data)
                }
            } catch (e: Throwable) {
                NetLog.e(TAG, "Write channel error: ${e.message}", e)
                error(e)
            }
        }
    }

    private fun release() {
        socket?.close()
        socket = null
        selector.close()
        pktReadChannel.close()
        pktWriteChannel.close()
    }

    companion object {
        private const val TAG = "TcpClientTask"
    }
}