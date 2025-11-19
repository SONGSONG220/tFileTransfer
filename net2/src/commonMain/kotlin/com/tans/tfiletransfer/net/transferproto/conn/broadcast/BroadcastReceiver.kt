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
import com.tans.tfiletransfer.net.socket.udp.IUdpTask
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.time.TimeSource

class BroadcastReceiver(
    private val localDeviceName: String,
    private val localAddress: Address,
    private val broadcastAddress: Address,
    private val maxRemoteDevicesIdleTimeInMillis: Long = 5000L
) : ITask {

    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()

    private val remoteDevicesFlow: MutableStateFlow<List<Pair<TimeSource.Monotonic.ValueTimeMark, RemoteDevice>>> = MutableStateFlow(emptyList())

    private var receiverTask: UdpTask? = null
    private var createConnectionTask: UdpTask? = null
    private var createConnectionTaskClient: IUdpClientManager? = null

    override suspend fun onStartTask() {
        val receiverTask = UdpTask(
            connectionType = UdpTask.Companion.UdpConnectionType.Bind(
                localAddress = AddressWithPort(broadcastAddress, TransferProtoConstant.BROADCAST_CONN_SCANNER_PORT)
            ),
            enableBroadcast = true
        )
        receiverTask.startTask()
        receiverTask.waitTaskConnectedOrError().apply {
            if (this is TaskState.Error) {
                error(TransferException("Failed to start broadcast receiver task. Cause: ${this.throwable?.message}", this.throwable))
                return
            }
        }
        val createConnectionTask = UdpTask(
            connectionType = UdpTask.Companion.UdpConnectionType.Bind(
                localAddress = AddressWithPort(localAddress, TransferProtoConstant.BROADCAST_CONN_CLIENT_PORT)
            )
        )
        createConnectionTask.startTask()
        createConnectionTask.waitTaskConnectedOrError().apply {
            if (this is TaskState.Error) {
                receiverTask.stopTask()
                error(TransferException("Failed to start create connection task. Cause: ${this.throwable?.message}", this.throwable))
                return
            }
        }
        this.receiverTask = receiverTask
        this.createConnectionTask = createConnectionTask
        this.createConnectionTaskClient = createConnectionTask.defaultClientManager()
        updateStateExpect(
            expect = TaskState.Connecting,
            update = TaskState.Connected,
            fail = {
                this.receiverTask = null
                this.createConnectionTask = null
                this.createConnectionTaskClient = null
                receiverTask.stopTask()
                createConnectionTask.stopTask()
                error(TransferException("Fail to update connected state."))
            }
        ) {
            NetLog.d(TAG, "Broadcast receiver task and create connection task connected.")
            onConnectionCreated(receiverTask = receiverTask, createConnectionTask = createConnectionTask)
        }
    }

    override suspend fun onStopTask(cause: String?) {
        NetLog.d(TAG, "Task stopped. Cause: $cause")
        release()
    }

    override suspend fun onError(throwable: Throwable?) {
        NetLog.e(TAG, throwable?.message ?: "Unknown error", throwable)
        release()
    }

    suspend fun requestCreateConnection(remoteDevice: RemoteDevice): BroadcastCreateConnRsp? {
        return try {
            val req = BroadcastCreateConnReq(
                version = TransferProtoConstant.VERSION,
                deviceName = localDeviceName
            )
            createConnectionTaskClient?.requestSimplify<BroadcastCreateConnReq, BroadcastCreateConnRsp>(
                requestType = BroadcastConnType.CreateConnReq.type,
                request = req,
                responseType = BroadcastConnType.CreateConnRsp.type,
                targetAddress = AddressWithPort(remoteDevice.remoteAddress, TransferProtoConstant.BROADCAST_CONN_SERVER_PORT)
            ) ?: throw TransferException("Connection has already closed.")
        } catch (e: Exception) {
            NetLog.e(TAG, "Failed to request create connection to remote device. Cause: ${e.message}", e)
            null
        }
    }

    fun remoteDevices(): Flow<List<RemoteDevice>> = remoteDevicesFlow.map { it.map { (_, d) -> d } }.distinctUntilChanged()

    private fun receiveBroadcastMsg(
        remoteAddress: AddressWithPort,
        msg: BroadcastMsg) {
        if (msg.version != TransferProtoConstant.VERSION) {
            NetLog.w(TAG, "Received broadcast message with invalid version. Version: ${msg.version}")
            return
        }
        if (remoteAddress.address == localAddress) {
            NetLog.d(TAG, "Received broadcast message from self.")
            return
        }
        val nowMark = TimeSource.Monotonic.markNow()
        val currentDevices = remoteDevicesFlow.value
        var isNew = true
        val newDevices = currentDevices.map {
            if (it.second.remoteAddress == remoteAddress.address) {
                isNew = false
                nowMark to it.second
            } else {
                it
            }
        }.let {
            if (isNew) {
                NetLog.d(TAG, "Find new device: name=${msg.deviceName}, address=${remoteAddress.address}")
                it + (nowMark to RemoteDevice(
                    remoteAddress = remoteAddress.address,
                    deviceName = msg.deviceName,
                ))
            } else {
                it
            }
        }
        remoteDevicesFlow.value = newDevices
    }

    private fun onConnectionCreated(
        receiverTask: IUdpTask,
        createConnectionTask: IUdpTask) {
        coroutineScope.launch {
            runCatching {
                receiverTask.waitTaskFinished()
                error(TransferException("Receiver task finished."))
            }
        }
        coroutineScope.launch {
            runCatching {
                createConnectionTask.waitTaskFinished()
                error(TransferException("Create connection task finished."))
            }
        }
        coroutineScope.launch { // Remove out of data devices
            runCatching {
                while (true) {
                    delay(maxRemoteDevicesIdleTimeInMillis)
                    var removeDeviceCount = 0
                    val toCheckDevices = remoteDevicesFlow.value
                    val newDevices = toCheckDevices.filter {
                        if (it.first.elapsedNow().inWholeMilliseconds > maxRemoteDevicesIdleTimeInMillis) {
                            NetLog.d(TAG, "Remove device ${it.second.deviceName}, because of out of data.")
                            removeDeviceCount ++
                            false
                        } else {
                            true
                        }
                    }
                    if (removeDeviceCount > 0) {
                        remoteDevicesFlow.value = newDevices
                    }
                }
            }
        }
        receiverTask.defaultServerManager()
            .registerServer(server<BroadcastMsg, Unit>(
                requestType = BroadcastConnType.BroadcastMsg.type,
                responseType = -1
            ) { _, remoteAddress, r, isNewRequest ->
                receiveBroadcastMsg(remoteAddress, r)
                null
            })
    }

    private fun release() {
        receiverTask?.stopTask()
        receiverTask = null
        createConnectionTask?.stopTask()
        createConnectionTask = null
        createConnectionTaskClient = null
        remoteDevicesFlow.value = emptyList()
    }

    companion object {
        private const val TAG = "BroadcastReceiver"
    }
}