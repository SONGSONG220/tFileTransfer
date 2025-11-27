package com.tans.tfiletransfer.net.socket.udp

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.BaseConnectionTask
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.PackageDataWithAddress
import com.tans.tfiletransfer.net.socket.SocketException
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import com.tans.tfiletransfer.net.socket.toAddress
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ASocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.DatagramReadWriteChannel
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.readFully
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.core.writeFully
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class UdpTask(
    val connectionType: UdpConnectionType,
    override val bufferPool: BufferPool = BufferPool(),
    private val enableBroadcast: Boolean = false,
    readWriteIdleLimitInMillis: Long = Long.MAX_VALUE
) : BaseConnectionTask(readWriteIdleLimitInMillis), IUdpTask {

    private val pktReadChannel: MutableSharedFlow<PackageDataWithAddress> = MutableSharedFlow(extraBufferCapacity = 10, onBufferOverflow = BufferOverflow.SUSPEND)

    private val pktWriteChannel: Channel<PackageDataWithAddress> = Channel(10)

    private var socket: ASocket? = null
    private var selectorManager: SelectorManager? = null

    override val tag: String = TAG

    override suspend fun onStartTask() {
        super.onStartTask()
        val selectorManager = SelectorManager(Dispatchers.IO)
        try {
            val socket = aSocket(selectorManager)
                .udp()
                .let {
                    when (connectionType) {
                        is UdpConnectionType.Bind -> it.bind(InetSocketAddress(connectionType.localAddress.address, connectionType.localAddress.port)) {
                            broadcast = enableBroadcast
                            reuseAddress = true
                        }
                        is UdpConnectionType.Connect -> it.connect(InetSocketAddress(connectionType.remoteAddress.address, connectionType.remoteAddress.port)) {
                            broadcast = enableBroadcast
                            reuseAddress = true
                        }
                    }
                }
            this.socket = socket
            this.selectorManager = selectorManager
            updateStateExpect(
                expect = TaskState.Connecting,
                update = TaskState.Connected,
                fail = {
                    this.socket = null
                    this.selectorManager = null
                    runCatching {
                        socket.close()
                    }
                    selectorManager.close()
                },
                success = {
                    NetLog.d(TAG, "Udp connect success: $connectionType")
                    startRead(socket)
                    startWrite(socket)
                }
            )

        } catch (e: Throwable) {
            selectorManager.close()
            error(SocketException("Connection fail: ${e.message}", e))
        }
    }

    override suspend fun onStopTask(cause: String?) {
        NetLog.d(TAG, "Task closed.")
        release()
    }

    override suspend fun onError(throwable: Throwable?) {
        NetLog.e(TAG, throwable?.message ?: "Unknown error", throwable)
        release()
    }

    override fun socket(): ASocket? = socket

    override fun pktReadChannel(): Flow<PackageDataWithAddress> = pktReadChannel

    override suspend fun writePktData(pktDataWithAddress: PackageDataWithAddress): Boolean {
        return if (currentState() == TaskState.Connected) {
            try {
                pktWriteChannel.send(pktDataWithAddress)
                true
            } catch (e: Throwable) {
                NetLog.e(TAG, "Write channel error: ${e.message}", e)
                false
            }
        } else {
            false
        }
    }


    private fun startRead(socket: DatagramReadWriteChannel) {
        coroutineScope.launch {
            try {
                val readChannel = socket.incoming
                for (datagram in readChannel) {
                    val remoteAddressInet = datagram.address as InetSocketAddress
                    val remoteAddress = AddressWithPort(
                        address = remoteAddressInet.toAddress(),
                        port = remoteAddressInet.port
                    )
                    val pkt = datagram.packet
                    // val pktLen = pkt.readInt()
                    val pktLen = pkt.remaining.toInt()
                    val type = pkt.readInt()
                    val msgId = pkt.readLong()
                    val dataLen = pktLen - 4 - 8
                    val data = bufferPool.get(dataLen)
                    data.contentSize = dataLen
                    pkt.readFully(out = data.array, offset = 0, length = dataLen)
                    pktReadChannel.emit(
                        PackageDataWithAddress(
                            pkt = PackageData(
                                type = type,
                                messageId = msgId,
                                data = data
                            ),
                            address = remoteAddress
                        )
                    )
                    resetLastReadWriteTime()
                }
            } catch (e: Throwable) {
                error(SocketException("Read chanel error: ${e.message}", e))
            }
        }
    }

    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    private fun startWrite(socket: DatagramReadWriteChannel) {
        coroutineScope.launch(start = CoroutineStart.ATOMIC) {
            try {
                val writeChannel = socket.outgoing
                for (toWrite in pktWriteChannel) {
                    val ktBuffer = kotlinx.io.Buffer()
                    // ktBuffer.writeInt(toWrite.pkt.data.contentSize + 4 + 8)
                    ktBuffer.writeInt(toWrite.pkt.type)
                    ktBuffer.writeLong(toWrite.pkt.messageId)
                    ktBuffer.writeFully(toWrite.pkt.data.array, 0, toWrite.pkt.data.contentSize)
                    writeChannel.send(Datagram(ktBuffer, InetSocketAddress(toWrite.address.address, toWrite.address.port)))
                    bufferPool.put(toWrite.pkt.data)
                    resetLastReadWriteTime()
                }
                NetLog.d(TAG, "Write channel closed.")
            } catch (e: Throwable) {
                error(SocketException("Write channel error: ${e.message}", e))
            }
            this@UdpTask.socket?.let {
                runCatching {
                    it.close()
                }
                NetLog.d(TAG, "Socket closed.")
            } ?: NetLog.e(TAG, "Socket is null.")
            this@UdpTask.socket = null
            this@UdpTask.selectorManager?.close()
            this@UdpTask.selectorManager = null
        }
    }

    private fun release() {
        pktWriteChannel.close()
    }

    companion object {
        private const val TAG = "UdpTask"

        sealed class UdpConnectionType {
            class Bind(val localAddress: AddressWithPort) : UdpConnectionType() {
                override fun toString(): String {
                    return "Bind(${localAddress.address}:${localAddress.port})"
                }
            }
            class Connect(val remoteAddress: AddressWithPort) : UdpConnectionType() {
                override fun toString(): String {
                    return "Connect(${remoteAddress.address}:${remoteAddress.port})"
                }
            }
        }
    }
}