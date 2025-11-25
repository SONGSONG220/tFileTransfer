package com.tans.tfiletransfer.net.transferproto.fileexplore

import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.Address
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ext.client.ITcpClientManager
import com.tans.tfiletransfer.net.socket.ext.client.defaultClientManager
import com.tans.tfiletransfer.net.socket.ext.client.requestSimplify
import com.tans.tfiletransfer.net.socket.ext.defaultServerManager
import com.tans.tfiletransfer.net.socket.tcp.ITcpClientTask
import com.tans.tfiletransfer.net.socket.tcp.TcpClientTask
import com.tans.tfiletransfer.net.transferproto.TransferException
import com.tans.tfiletransfer.net.transferproto.TransferProtoConstant
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExploreHandshake
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.FileExploreDataType
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExploreHandshakeReq
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExploreHandshakeRsp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FileExploreClient(
    private val serverAddress: Address,
    private val localFileSeparator: String,
    override val deviceExplorer: IDeviceExplorer,
    override val remoteRequestSendHandler: IRemoteRequestSendHandler,
    override val remoteRequestDownloadHandler: IRemoteRequestDownloadHandler,
    private val heartBeatIntervalInMillis: Long = 3000L
) : BaseFileExplore() {

    override val tag: String = TAG

    private var clientManager: ITcpClientManager? = null
    private var clientTask: ITcpClientTask? = null

    override suspend fun onStartTask() {
        val clientTask = TcpClientTask(
            serverAddress = AddressWithPort(serverAddress, TransferProtoConstant.FILE_EXPLORE_SERVER_PORT),
            // lost 3 heartbeats, consider connection lost.
            readWriteIdleLimitInMillis = heartBeatIntervalInMillis * 3
        )
        clientTask.startTask()
        clientTask.waitTaskConnectedOrError().apply {
            if (this is TaskState.Error) {
                error(TransferException("Failed to start client task. Cause: ${this.throwable?.message}", this.throwable))
                return
            }
        }
        val clientManager = clientTask.defaultClientManager().defaultServerManager()
        val handshakeRsp = try {
            clientManager.requestSimplify<ExploreHandshakeReq, ExploreHandshakeRsp>(
                requestType = FileExploreDataType.HandshakeReq.type,
                responseType = FileExploreDataType.HandshakeRsp.type,
                request = ExploreHandshakeReq(
                    version = TransferProtoConstant.VERSION,
                    fileSeparator = localFileSeparator
                )
            )
        } catch (e: Throwable) {
            clientTask.stopTask()
            error(TransferException("Handshake fail. ${e.message}", e))
            return
        }
        val exploreHandshake = ExploreHandshake(handshakeRsp.fileSeparator)
        exploreHandshakeFlow.value = exploreHandshake
        NetLog.d(TAG, "Handshake: $exploreHandshake")

        this.clientTask = clientTask
        this.clientManager = clientManager
        updateStateExpect(
            expect = TaskState.Connecting,
            update = TaskState.Connected,
            fail = {
                this.clientTask = null
                this.clientManager = null
                clientTask.stopTask()
                error(TransferException("Fail to update connected state."))
            }
        ) {
            NetLog.d(TAG, "Task connected.")
            clientManager.registerServer(exploreDirServer)
            clientManager.registerServer(sendFilesServer)
            clientManager.registerServer(downloadFileServer)
            clientManager.registerServer(messageServer)
            onConnectionCreated(clientManager)
        }
    }

    override fun clientManager(): ITcpClientManager? = clientManager

    override fun release() {
        clientTask?.stopTask()
        clientTask = null
        clientManager = null
    }

    private fun onConnectionCreated(
        clientManager: ITcpClientManager
    ) {
        coroutineScope.launch {
            runCatching {
                clientManager.connection.connectionTask.waitTaskFinished()
                error(TransferException("Client task finished."))
            }
        }

        coroutineScope.launch {
            runCatching {
                while (true) {
                    try {
                        clientManager.requestSimplify<Unit, Unit>(
                            requestType = FileExploreDataType.HeartbeatReq.type,
                            responseType = FileExploreDataType.HeartbeatRsp.type,
                            request = Unit
                        )
                        NetLog.d(TAG, "Heartbeat sent.")
                    } catch (e: Throwable) {
                        NetLog.e(TAG, "Failed to send heartbeat. Cause: ${e.message}", e)
                    }
                    delay(heartBeatIntervalInMillis)
                }
            }
        }
    }

    companion object {
        private const val TAG = "FileExploreClient"
    }
}