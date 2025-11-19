package com.tans.tfiletransfer.net.transferproto.conn.wifip2p

import com.tans.tfiletransfer.net.ITask
import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.ext.client.ITcpClientManager
import com.tans.tfiletransfer.net.socket.ext.client.requestSimplify
import com.tans.tfiletransfer.net.socket.ext.server.IServer
import com.tans.tfiletransfer.net.socket.ext.server.server
import com.tans.tfiletransfer.net.transferproto.TransferException
import com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model.WifiP2pDataType
import com.tans.tfiletransfer.net.transferproto.conn.wifip2p.model.WifiP2pHandshake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex

abstract class BaseWifiP2pConnection : ITask {

    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()

    abstract val tag: String

    private val connectionRequestFlow: MutableSharedFlow<Unit> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    private val closeP2pRequestFlow: MutableSharedFlow<Unit> by lazy {
        MutableSharedFlow(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    protected val wifiHandshakeFlow: MutableStateFlow<WifiP2pHandshake?> by lazy {
        MutableStateFlow(null)
    }

    protected val createConnServer: IServer<Unit, Unit> by lazy {
        server(
            requestType = WifiP2pDataType.CreateConnReq.type,
            responseType = WifiP2pDataType.CreateConnRsp.type,
        ) { _, remoteAddress, _, isNew ->
            NetLog.d(tag, "Received create connection request. RemoteAddress: $remoteAddress")
            if (isNew) {
                val isSuccess = connectionRequestFlow.tryEmit(Unit)
                if (!isSuccess) {
                    NetLog.e(tag, "Failed to emit connection request.")
                }
            }
            Unit
        }
    }

    protected val closeP2pServer: IServer<Unit, Unit> by lazy {
        server(
            requestType = WifiP2pDataType.CloseP2pReq.type,
            responseType = WifiP2pDataType.CloseP2pRsp.type,
        ) { _, remoteAddress, _, isNew ->
            NetLog.d(tag, "Remote device $remoteAddress request to close WiFi-P2P connection.")
            if (isNew) {
                val isSuccess = closeP2pRequestFlow.tryEmit(Unit)
                if (!isSuccess) {
                    NetLog.e(tag, "Failed to emit close p2p request.")
                }
            }
            Unit
        }
    }

    fun connectionRequest(): Flow<Unit> = connectionRequestFlow

    fun closeWifiP2pRequest(): Flow<Unit> = closeP2pRequestFlow

    override suspend fun onError(throwable: Throwable?) {
        NetLog.e(tag, throwable?.message ?: "Unknown error.", throwable)
        release()
    }

    override suspend fun onStopTask(cause: String?) {
        NetLog.d(tag, "Task stopped. Cause: $cause")
        release()
    }

    suspend fun waitHandshakeOrNull(): WifiP2pHandshake? {
        val state = waitTaskConnectedOrError()
        return if (state is TaskState.Connected) {
            wifiHandshakeFlow.value.apply {
                if (this == null) {
                    error(TransferException("Connected, but handshake is null."))
                }
            }
        } else {
            null
        }
    }

    suspend fun requestCreateConnection(): Boolean {
        return try {
            clientManager()?.requestSimplify<Unit, Unit>(
                requestType = WifiP2pDataType.CreateConnReq.type,
                responseType = WifiP2pDataType.CreateConnRsp.type,
                request = Unit
            ) ?: throw TransferException("Connection has already closed.")
            true
        } catch (e: Throwable) {
            NetLog.e(tag, "Failed to request create connection to remote device. Cause: ${e.message}", e)
            false
        }
    }

    suspend fun requestCloseWifiP2p(): Boolean {
        return try {
            clientManager()?.requestSimplify<Unit, Unit>(
                requestType = WifiP2pDataType.CloseP2pReq.type,
                responseType = WifiP2pDataType.CloseP2pRsp.type,
                request = Unit
            ) ?: throw TransferException("Connection has already closed.")
            true
        } catch (e: Throwable) {
            NetLog.e(tag, "Failed to request close WiFi-P2P. Cause: ${e.message}", e)
            false
        }
    }

    protected abstract fun clientManager(): ITcpClientManager?

    protected abstract fun release()

}