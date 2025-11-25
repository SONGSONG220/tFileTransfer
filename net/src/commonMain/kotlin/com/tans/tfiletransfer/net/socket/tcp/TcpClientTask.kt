package com.tans.tfiletransfer.net.socket.tcp

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.SocketException
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

class TcpClientTask(
    private val serverAddress: AddressWithPort,
    override val bufferPool: BufferPool = BufferPool(),
    readWriteIdleLimitInMillis: Long = Long.MAX_VALUE
) : BaseTcpClientTask(readWriteIdleLimitInMillis) {

    override val tag: String = TAG

    override suspend fun onStartTask() {
        super.onStartTask()
        val selectorManager = SelectorManager(Dispatchers.IO)
        try {
            val socket = aSocket(selectorManager)
                .tcp()
                .connect(serverAddress.address, serverAddress.port) {
                    reuseAddress = true
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
                    NetLog.d(TAG, "Connect to server $serverAddress success.")
                    startRead(socket)
                    startWrite(socket)
                }
            )
        } catch (e: Throwable) {
            selectorManager.close()
            error(SocketException("Connect to server $serverAddress fail: ${e.message}", e))
        }

    }


    companion object {
        private const val TAG = "TcpClientTask"
    }
}