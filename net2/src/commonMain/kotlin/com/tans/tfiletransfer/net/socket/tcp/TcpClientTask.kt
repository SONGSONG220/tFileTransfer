package com.tans.tfiletransfer.net.socket.tcp

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ConnectionTaskState
import com.tans.tfiletransfer.net.socket.buffer.BufferPool
import io.ktor.network.sockets.aSocket

class TcpClientTask(
    private val serverAddress: AddressWithPort,
    override val bufferPool: BufferPool = BufferPool()
) : BaseTcpClientTask() {

    override val tag: String = TAG

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


    companion object {
        private const val TAG = "TcpClientTask"
    }
}