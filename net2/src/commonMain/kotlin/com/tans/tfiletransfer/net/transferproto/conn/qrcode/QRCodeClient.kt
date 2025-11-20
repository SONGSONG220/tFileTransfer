package com.tans.tfiletransfer.net.transferproto.conn.qrcode

import com.tans.tfiletransfer.net.ITask
import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.Address
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ext.client.IUdpClientManager
import com.tans.tfiletransfer.net.socket.ext.client.defaultClientManager
import com.tans.tfiletransfer.net.socket.ext.client.requestSimplify
import com.tans.tfiletransfer.net.socket.udp.IUdpTask
import com.tans.tfiletransfer.net.socket.udp.UdpTask
import com.tans.tfiletransfer.net.transferproto.TransferException
import com.tans.tfiletransfer.net.transferproto.TransferProtoConstant
import com.tans.tfiletransfer.net.transferproto.conn.qrcode.model.QRCodeConnType
import com.tans.tfiletransfer.net.transferproto.conn.qrcode.model.QRCodeCreateConnReq
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class QRCodeClient(
    private val localDeviceName: String,
    private val localAddress: Address,
) : ITask {

    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()
    private var createConnTask: UdpTask? = null
    private var createConnClient: IUdpClientManager? = null

    override suspend fun onStartTask() {
        val createConnTask = UdpTask(
            connectionType = UdpTask.Companion.UdpConnectionType.Bind(
                localAddress = AddressWithPort(localAddress, TransferProtoConstant.QR_CODE_CONN_CLIENT_PORT)
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
        this.createConnClient = createConnTask.defaultClientManager()
        updateStateExpect(
            expect = TaskState.Connecting,
            update = TaskState.Connected,
            fail = {
                this.createConnTask = null
                this.createConnClient = null
                createConnTask.stopTask()
                error(TransferException("Fail to update connected state."))
            }
        ) {
            NetLog.d(TAG, "Task connected.")
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

    suspend fun requestCreateConnection(remoteAddress: Address): Boolean {
        return try {
            createConnClient?.requestSimplify<QRCodeCreateConnReq, Unit>(
                requestType = QRCodeConnType.CreateConnReq.type,
                request = QRCodeCreateConnReq(
                    version = TransferProtoConstant.VERSION,
                    deviceName = localDeviceName
                ),
                responseType = QRCodeConnType.CreateConnRsp.type,
                targetAddress = AddressWithPort(remoteAddress, TransferProtoConstant.QR_CODE_CONN_SERVER_PORT)
            ) ?: throw TransferException("Connection has already closed.")
            true
        } catch (e: Throwable) {
            NetLog.e(TAG, "Failed to request create connection to remote device. Cause: ${e.message}", e)
            false
        }
    }

    private fun onConnectionCreated(createConnTask: IUdpTask) {
        coroutineScope.launch {
            runCatching {
                createConnTask.waitTaskFinished()
                error(TransferException("Create connection task finished."))
            }
        }
    }

    private fun release() {
        createConnTask?.stopTask()
        createConnClient = null
        createConnTask = null
    }

    companion object {
        private const val TAG = "QRCodeClient"
    }
}
