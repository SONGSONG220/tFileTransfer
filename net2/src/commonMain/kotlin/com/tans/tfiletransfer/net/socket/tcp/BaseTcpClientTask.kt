package com.tans.tfiletransfer.net.socket.tcp

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.BaseConnectionTask
import com.tans.tfiletransfer.net.socket.ConnectionTaskState
import com.tans.tfiletransfer.net.socket.PackageData
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readInt
import io.ktor.utils.io.readLong
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeInt
import io.ktor.utils.io.writeLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

abstract class BaseTcpClientTask(
    readWriteIdleLimitInMillis: Long = Long.MAX_VALUE
) : BaseConnectionTask(readWriteIdleLimitInMillis) {

    protected val selector = SelectorManager(Dispatchers.IO)
    protected val pktReadChannel: Channel<PackageData> = Channel(10)
    protected val pktWriteChannel: Channel<PackageData> = Channel(10)
    protected var socket: Socket? = null

    override suspend fun onStopTask(cause: String?) {
        release()
    }

    override suspend fun onError(throwable: Throwable?) {
        release()
    }

    fun socket(): Socket? = socket

    fun pktReadChannel(): Channel<PackageData> = pktReadChannel

    suspend fun writePktData(pkt: PackageData): Boolean {
        return if (currentState() == ConnectionTaskState.Connected) {
            pktWriteChannel.send(pkt)
            true
        } else {
            false
        }
    }

    protected fun startRead(socket: Socket) {
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
                    resetLastReadWriteTime()
                }
            } catch (e: Throwable) {
                NetLog.e(tag, "Read chanel error: ${e.message}", e)
                error(e)
            }
        }
    }

    protected fun startWrite(socket: Socket) {
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
                    resetLastReadWriteTime()
                }
            } catch (e: Throwable) {
                NetLog.e(tag, "Write channel error: ${e.message}", e)
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
}