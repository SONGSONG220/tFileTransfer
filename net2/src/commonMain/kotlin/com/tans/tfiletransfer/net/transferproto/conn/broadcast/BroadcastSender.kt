package com.tans.tfiletransfer.net.transferproto.conn.broadcast

import com.tans.tfiletransfer.net.ITask
import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.Address
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ext.client.IUdpClientManager
import com.tans.tfiletransfer.net.socket.ext.client.defaultClientManager
import com.tans.tfiletransfer.net.socket.ext.client.requestSimplify
import com.tans.tfiletransfer.net.socket.ext.server.defaultServerManager
import com.tans.tfiletransfer.net.socket.ext.server.server
import com.tans.tfiletransfer.net.socket.udp.UdpTask
import com.tans.tfiletransfer.net.transferproto.TransferException
import com.tans.tfiletransfer.net.transferproto.TransferProtoConstant
import com.tans.tfiletransfer.net.transferproto.conn.broadcast.model.BroadcastCreateConnReq
import com.tans.tfiletransfer.net.transferproto.conn.broadcast.model.BroadcastCreateConnRsp
import com.tans.tfiletransfer.net.transferproto.conn.broadcast.model.BroadcastConnType
import com.tans.tfiletransfer.net.transferproto.conn.broadcast.model.BroadcastMsg
import com.tans.tfiletransfer.net.transferproto.conn.broadcast.model.RemoteDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.Json

class BroadcastSender(
    private val localDeviceName: String,
    private val localAddress: Address,
    private val broadcastAddress: Address,
    private val broadcastSendIntervalMillis: Long = 1000L
) : ITask {

    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()

    private val connectionRequestFlow: MutableSharedFlow<RemoteDevice> = MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var senderTask: UdpTask? = null
    private var createConnectionTask: UdpTask? = null

    override suspend fun onStartTask() {
        val senderTask = UdpTask(
            connectionType = UdpTask.Companion.UdpConnectionType.Connect(
                remoteAddress = AddressWithPort(broadcastAddress, TransferProtoConstant.BROADCAST_CONN_SCANNER_PORT),
            ),
            enableBroadcast = true
        )
        val senderClient = senderTask.defaultClientManager()
        senderTask.startTask()
        senderTask.waitTaskConnectedOrError().apply {
            if (this is TaskState.Error) {
                error(TransferException("Failed to start broadcast sender task. Cause: ${this.throwable?.message}", this.throwable))
                return
            }
        }
        val createConnectionTask = UdpTask(
            connectionType = UdpTask.Companion.UdpConnectionType.Bind(
                localAddress = AddressWithPort(localAddress, TransferProtoConstant.BROADCAST_CONN_SERVER_PORT)
            )
        )
        createConnectionTask.startTask()
        createConnectionTask.waitTaskConnectedOrError().apply {
            if (this is TaskState.Error) {
                senderTask.stopTask()
                error(TransferException("Failed to start create connection task. Cause: ${this.throwable?.message}", this.throwable))
                return
            }
        }
        this.senderTask = senderTask
        this.createConnectionTask = createConnectionTask
        updateStateExpect(
            expect = TaskState.Connecting,
            update = TaskState.Connected,
            fail = {
                this.senderTask = null
                this.createConnectionTask = null
                senderTask.stopTask()
                createConnectionTask.stopTask()
                error(TransferException("Fail to update connected state."))
            }
        ) {
            NetLog.d(TAG, "Broadcast sender task and create connection task connected.")
            onConnectionCreated(senderClient, createConnectionTask)
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

    private fun onConnectionCreated(
        senderClient: IUdpClientManager,
        createConnectionTask: UdpTask,
    ) {
        val broadcastMsg = BroadcastMsg(
            version = TransferProtoConstant.VERSION,
            deviceName = localDeviceName,
        )
        val broadcastStr = try {
            Json.encodeToString(broadcastMsg)
        } catch (e: Throwable) {
            error(TransferException("Failed to encode broadcast msg to string. Cause: ${e.message}", e))
            return
        }

        coroutineScope.launch {
            runCatching {
                senderClient.connectionTask.waitTaskFinished()
                error(TransferException("Sender task finished."))
            }
        }
        coroutineScope.launch {
            runCatching {
                createConnectionTask.waitTaskFinished()
                error(TransferException("Create connection task finished."))
            }
        }

        coroutineScope.launch {
            runCatching {
                while (true) {
                    try {
                        senderClient.requestSimplify<String>(
                            requestType = BroadcastConnType.BroadcastMsg.type,
                            request = broadcastStr,
                            targetAddress = AddressWithPort(broadcastAddress, TransferProtoConstant.BROADCAST_CONN_SCANNER_PORT)
                        )
                    } catch (e: Throwable) {
                        NetLog.e(TAG, "Failed to send broadcast message. Cause: ${e.message}", e)
                    }
                    delay(broadcastSendIntervalMillis)
                }
            }
        }

        createConnectionTask.defaultServerManager()
            .registerServer(server<BroadcastCreateConnReq, BroadcastCreateConnRsp>(
                requestType = BroadcastConnType.CreateConnReq.type,
                responseType = BroadcastConnType.CreateConnRsp.type
            ) { _, remoteAddress, r, isNewRequest ->
                NetLog.d(TAG, "Received create connection request. Request: $r")
                if (isNewRequest) {
                    if (r.version == TransferProtoConstant.VERSION) {
                        val isSuccess = connectionRequestFlow.tryEmit(
                            RemoteDevice(
                                deviceName = r.deviceName,
                                remoteAddress = remoteAddress.address
                            )
                        )
                        if (!isSuccess) {
                            NetLog.e(TAG, "Failed to emit connection request. Request: $r")
                        }
                    } else {
                        NetLog.w(TAG, "Received create connection request with invalid version. Request: $r")
                    }
                }
                BroadcastCreateConnRsp(localDeviceName)
            })

    }

    private fun release() {
        senderTask?.stopTask()
        senderTask = null
        createConnectionTask?.stopTask()
        createConnectionTask = null
    }

    companion object {
        private const val TAG = "BroadcastSender"
    }
}
