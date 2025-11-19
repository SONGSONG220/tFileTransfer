package com.tans.tfiletransfer.net.transferproto.conn.qrcode

import com.tans.tfiletransfer.net.ITask
import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.Address
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ext.server.defaultServerManager
import com.tans.tfiletransfer.net.socket.ext.server.server
import com.tans.tfiletransfer.net.socket.udp.IUdpTask
import com.tans.tfiletransfer.net.socket.udp.UdpTask
import com.tans.tfiletransfer.net.transferproto.TransferException
import com.tans.tfiletransfer.net.transferproto.TransferProtoConstant
import com.tans.tfiletransfer.net.transferproto.conn.broadcast.model.RemoteDevice
import com.tans.tfiletransfer.net.transferproto.conn.qrcode.model.QRCodeConnType
import com.tans.tfiletransfer.net.transferproto.conn.qrcode.model.QRCodeCreateConnReq
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class QRCodeServer(private val localAddress: Address) : ITask {
    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()
    private var createConnTask: UdpTask? = null

    private val connectionRequestFlow: MutableSharedFlow<RemoteDevice> = MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override suspend fun onStartTask() {
        val createConnTask = UdpTask(
            connectionType = UdpTask.Companion.UdpConnectionType.Bind(
                localAddress = AddressWithPort(localAddress, TransferProtoConstant.QR_CODE_CONN_SERVER_PORT)
            )
        )
        createConnTask.startTask()
        createConnTask.waitTaskConnectedOrError().apply {
            if (this is TaskState.Error) {
                error(TransferException("Failed to start QRCode client task. Cause: ${this.throwable?.message}", this.throwable))
                return
            }
        }
        this.createConnTask = createConnTask
        updateStateExpect(
            expect = TaskState.Connecting,
            update = TaskState.Connected,
            fail = {
                this.createConnTask = null
                createConnTask.stopTask()
                error(TransferException("Fail to update connected state."))
            }
        ) {
            NetLog.d(TAG, "Task connected")
            onConnectionCreated(createConnTask)
        }
    }

    override suspend fun onStopTask(cause: String?) {
        NetLog.d(TAG, "Task stopped. Cause: $cause")
        release()
    }

    override suspend fun onError(throwable: Throwable?) {
        NetLog.e(TAG, throwable?.message ?: "Unknown error.", throwable)
        release()
    }

    fun connectionRequest(): Flow<RemoteDevice> = connectionRequestFlow

    private fun onConnectionCreated(createConnTask: IUdpTask) {
        coroutineScope.launch {
            runCatching {
                createConnTask.waitTaskFinished()
                error(TransferException("Create connection task finished."))
            }
        }
        createConnTask.defaultServerManager()
            .registerServer(server<QRCodeCreateConnReq, Unit>(
                requestType = QRCodeConnType.CreateConnReq.type,
                responseType = QRCodeConnType.CreateConnRsp.type,
            ) { _, remoteAddress, r, isNew ->
                NetLog.d(TAG, "Received create connection request. Request: $r")
                if (r.version == TransferProtoConstant.VERSION) {
                    if (isNew) {
                        val isSuccess = connectionRequestFlow.tryEmit(
                            RemoteDevice(
                                remoteAddress = remoteAddress.address,
                                deviceName = r.deviceName
                            )
                        )
                        if (!isSuccess) {
                            NetLog.e(TAG, "Failed to emit connection request. Request: $r")
                        }
                    }
                    Unit
                } else {
                    NetLog.w(TAG, "Received create connection request with invalid version. Request: $r")
                    null
                }
            })
    }

    private fun release() {
        createConnTask?.stopTask()
        createConnTask = null
        coroutineScope.cancel()
    }

    companion object {
        private const val TAG = "QRCodeServer"
    }
}