package com.tans.tfiletransfer.net.transferproto.fileexplore

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.Address
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ext.client.ITcpClientManager
import com.tans.tfiletransfer.net.socket.ext.defaultClientManager
import com.tans.tfiletransfer.net.socket.ext.server.defaultServerManager
import com.tans.tfiletransfer.net.socket.ext.server.IServer
import com.tans.tfiletransfer.net.socket.ext.server.server
import com.tans.tfiletransfer.net.socket.tcp.ITcpClientTask
import com.tans.tfiletransfer.net.socket.tcp.ITcpServerTask
import com.tans.tfiletransfer.net.socket.tcp.TcpServerTask
import com.tans.tfiletransfer.net.transferproto.TransferException
import com.tans.tfiletransfer.net.transferproto.TransferProtoConstant
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExploreHandshake
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExploreHandshakeReq
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExploreHandshakeRsp
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.FileExploreDataType
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class FileExploreServer(
    private val localAddress: Address,
    private val localFileSeparator: String,
    override val deviceExplorer: IDeviceExplorer,
    override val remoteRequestSendHandler: IRemoteRequestSendHandler,
    override val remoteRequestDownloadHandler: IRemoteRequestDownloadHandler,
    private val waitClientTimeoutInMillis: Long = 3000L,
    private val waitHandshakeTimeoutInMillis: Long = 3000L,
    private val heartBeatIntervalInMillis: Long = 3000L
) : BaseFileExplore() {

    override val tag: String = TAG

    private var serverTask: ITcpServerTask? = null
    private var clientTask: ITcpClientTask? = null
    private var clientManager: ITcpClientManager? = null

    private val heartbeatServer: IServer<Unit, Unit> by lazy {
        server(
            requestType = FileExploreDataType.HeartbeatReq.type,
            responseType = FileExploreDataType.HeartbeatRsp.type
        ) { _, _, _, _ ->
            NetLog.d(tag, "Heartbeat received.")
        }
    }

    override suspend fun onStartTask() {
        val serverTask = TcpServerTask(
            bindAddress = AddressWithPort(localAddress, TransferProtoConstant.FILE_EXPLORE_SERVER_PORT),
            // lost 3 heartbeats, consider connection lost.
            readWriteIdleLimitInMillis = heartBeatIntervalInMillis * 3
        )
        serverTask.startTask()
        serverTask.waitTaskConnectedOrError().apply {
            if (this is TaskState.Error) {
                error(TransferException("Failed to start server task. Cause: ${this.throwable?.message}", this.throwable))
                return
            }
        }
        val clientTask = try {
            withTimeout(waitClientTimeoutInMillis) {
                serverTask.clientChannel().receive()
            }
        } catch (e: Throwable) {
            serverTask.stopTask()
            error(TransferException("Failed to receive client channel. Cause: ${e.message}", e))
            return
        }
        val manager = clientTask.defaultServerManager().defaultClientManager()

        val handshakeServer: IServer<ExploreHandshakeReq, ExploreHandshakeRsp> = server(
            requestType = FileExploreDataType.HandshakeReq.type,
            responseType = FileExploreDataType.HandshakeRsp.type
        ) { _, _, r, isNew ->
            if (r.version != TransferProtoConstant.VERSION) {
                error(TransferException("Version not match. Expect: ${TransferProtoConstant.VERSION}, but got: ${r.version}"))
                serverTask.stopTask()
                clientTask.stopTask()
                null
            } else {
                if (isNew) {
                    exploreHandshakeFlow.value = ExploreHandshake(r.fileSeparator)
                }
                ExploreHandshakeRsp(localFileSeparator)
            }
        }
        manager.registerServer(handshakeServer)
        val hs = try {
            withTimeout(waitHandshakeTimeoutInMillis) {
                exploreHandshakeFlow.filterNotNull().first()
            }
        } catch (e: Throwable) {
            serverTask.stopTask()
            clientTask.stopTask()
            error(TransferException("Failed to receive handshake. Cause: ${e.message}", e))
            return
        }
        manager.unregisterServer(handshakeServer)
        NetLog.d(TAG, "Handshake: $hs")

        this.serverTask = serverTask
        this.clientTask = clientTask
        this.clientManager = manager
        updateStateExpect(
            expect = TaskState.Connecting,
            update = TaskState.Connected,
            fail = {
                this.serverTask = null
                this.clientTask = null
                this.clientManager = null
                serverTask.stopTask()
                clientTask.stopTask()
                error(TransferException("Fail to update connected state."))
            }
        ) {
            NetLog.d(TAG, "Task connected.")
            manager.registerServer(heartbeatServer)
            manager.registerServer(exploreDirServer)
            manager.registerServer(sendFilesServer)
            manager.registerServer(downloadFileServer)
            manager.registerServer(messageServer)
            onConnectionCreated(serverTask, clientTask)
        }
    }

    override fun clientManager(): ITcpClientManager? = clientManager

    override fun release() {
        clientTask?.stopTask()
        serverTask?.stopTask()
        clientManager = null
        clientTask = null
        serverTask = null
    }

    private fun onConnectionCreated(
        serverTask: ITcpServerTask,
        clientTask: ITcpClientTask,
    ) {
        coroutineScope.launch {
            runCatching {
                serverTask.waitTaskFinished()
                error(TransferException("Server task finished."))
            }
        }
        coroutineScope.launch {
            runCatching {
                clientTask.waitTaskFinished()
                error(TransferException("Client task finished."))
            }
        }
    }

    companion object {
        private const val TAG = "FileExploreServer"
    }
}
