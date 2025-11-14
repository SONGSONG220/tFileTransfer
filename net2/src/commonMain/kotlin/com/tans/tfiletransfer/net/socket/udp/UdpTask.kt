package com.tans.tfiletransfer.net.socket.udp

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.BaseConnectionTask
import com.tans.tfiletransfer.net.socket.ConnectionTaskState
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.PackageDataWithAddress
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ASocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.DatagramReadWriteChannel
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.readAvailable
import io.ktor.utils.io.core.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.io.InternalIoApi

class UdpTask(
    val connectionType: UdpConnectionType,
    override val bufferPool: BufferPool = BufferPool(),
    readWriteIdleLimitInMillis: Long = Long.MAX_VALUE
) : BaseConnectionTask(readWriteIdleLimitInMillis), IUdpTask {

    private val selector = SelectorManager(Dispatchers.IO)

    private val pktReadChannel: MutableSharedFlow<PackageDataWithAddress> = MutableSharedFlow(extraBufferCapacity = 10, onBufferOverflow = BufferOverflow.SUSPEND)

    private val pktWriteChannel: Channel<PackageDataWithAddress> = Channel(10)

    private var socket: ASocket? = null

    override val tag: String = TAG

    override suspend fun onStartTask() {
        super.onStartTask()
        try {
            val socket = aSocket(selector)
                .udp()
                .configure {
                    broadcast = true
                    reuseAddress = true
                }
                .let {
                    when (connectionType) {
                        is UdpConnectionType.Bind -> it.bind(InetSocketAddress(connectionType.localAddress.address, connectionType.localAddress.port))
                        is UdpConnectionType.Connect -> it.connect(InetSocketAddress(connectionType.remoteAddress.address, connectionType.remoteAddress.port))
                    }
                }
            updateStateExpect(
                expect = ConnectionTaskState.Connecting,
                update = ConnectionTaskState.Connected,
                fail = {
                    socket.close()
                },
                success = {
                    NetLog.d(TAG, "Udp connect success: $connectionType")
                    this.socket = socket
                    startRead(socket)
                    startWrite(socket)
                }
            )

        } catch (e: Throwable) {
            NetLog.e(TAG, "Connection fail: ${e.message}", e)
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

    override fun socket(): ASocket? = socket

    override fun pktReadChannel(): Flow<PackageDataWithAddress> = pktReadChannel

    override suspend fun writePktData(pktDataWithAddress: PackageDataWithAddress): Boolean {
        return if (currentState() == ConnectionTaskState.Connected) {
            pktWriteChannel.send(pktDataWithAddress)
            true
        } else {
            false
        }
    }

    @OptIn(InternalIoApi::class)
    private fun startRead(socket: DatagramReadWriteChannel) {
        coroutineScope.launch {
            try {
                val readChannel = socket.incoming
                for (datagram in readChannel) {
                    val remoteAddressInet = datagram.address as InetSocketAddress
                    val remoteAddress = AddressWithPort(
                        address = remoteAddressInet.hostname,
                        port = remoteAddressInet.port
                    )
                    val pkt = datagram.packet
                    // val pktLen = pkt.readInt()
                    val pktLen = pkt.buffer.size.toInt()
                    val type = pkt.readInt()
                    val msgId = pkt.readLong()
                    val dataLen = pktLen - 4 - 8
                    val data = bufferPool.get(dataLen)
                    data.contentSize = dataLen
                    pkt.readAvailable(buffer = data.array, offset = 0, length = dataLen)
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
                NetLog.e(TAG, "Read chanel error: ${e.message}", e)
                error(e)
            }
        }
    }

    private fun startWrite(socket: DatagramReadWriteChannel) {
        coroutineScope.launch {
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
            } catch (e: Throwable) {
                NetLog.e(TAG, "Read channel error: ${e.message}", e)
                error(e)
            }
        }
    }

    private fun release() {
        socket?.close()
        socket = null
        selector.close()
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