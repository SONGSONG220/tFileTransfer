package com.tans.tfiletransfer.net.transferproto.fileexplore

import com.tans.tfiletransfer.net.ITask
import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.ext.client.ITcpClientManager
import com.tans.tfiletransfer.net.socket.ext.client.requestSimplify
import com.tans.tfiletransfer.net.socket.ext.server.IServer
import com.tans.tfiletransfer.net.socket.ext.server.server
import com.tans.tfiletransfer.net.transferproto.TransferException
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.DownloadFilesReq
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.DownloadFilesRsp
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.FileExploreDataType
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExploreDirReq
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExploreDirRsp
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerDir
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExploreHandshake
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.SendFilesReq
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.SendFilesRsp
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.SendMsgReq
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex

abstract class BaseFileExplore : ITask {

    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()

    abstract val deviceExplorer: IDeviceExplorer
    abstract val remoteRequestSendHandler: IRemoteRequestSendHandler
    abstract val remoteRequestDownloadHandler: IRemoteRequestDownloadHandler

    abstract val tag: String

    private val remoteMessageFlow: MutableSharedFlow<Pair<String, Long>> by lazy {
        MutableSharedFlow(extraBufferCapacity = 10, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    protected val exploreHandshakeFlow: MutableStateFlow<ExploreHandshake?> by lazy {
        MutableStateFlow(null)
    }

    protected val exploreDirServer: IServer<ExploreDirReq, ExploreDirRsp> by lazy {
        server<ExploreDirReq, ExploreDirRsp>(
            requestType = FileExploreDataType.ExploreDirReq.type,
            responseType = FileExploreDataType.ExploreDirRsp.type
        ) { _, _, request, _ ->
            NetLog.d(tag, "Remote request explore dir: ${request.requestPath}")
            val (dirs, files) = deviceExplorer.explore(request.requestPath)
            ExploreDirRsp(
                path = request.requestPath,
                childrenDirs = dirs,
                childrenFiles = files
            )
        }
    }

    protected val sendFilesServer: IServer<SendFilesReq, SendFilesRsp> by lazy {
        server<SendFilesReq, SendFilesRsp>(
            requestType = FileExploreDataType.SendFilesReq.type,
            responseType = FileExploreDataType.SendFilesRsp.type
        ) { _, _, request, _ ->
            val bufferSize = remoteRequestSendHandler.onRequestSend(request.sendFiles, request.maxConnection)
            NetLog.d(tag, "Remote request send files: ${request.sendFiles}")
            if (bufferSize == null) {
                NetLog.w(tag, "Reject remote send files.")
                null
            } else {
                SendFilesRsp(bufferSize)
            }
        }
    }

    protected val downloadFileServer: IServer<DownloadFilesReq, DownloadFilesRsp> by lazy {
        server<DownloadFilesReq, DownloadFilesRsp>(
            requestType = FileExploreDataType.DownloadFilesReq.type,
            responseType = FileExploreDataType.DownloadFilesRsp.type
        ) { _, _, request, _ ->
            NetLog.d(tag, "Remote request download files: ${request.downloadFiles}")
            val maxConnection = remoteRequestDownloadHandler.onRequestDownload(request.downloadFiles, request.bufferSize)
            if (maxConnection == null) {
                NetLog.w(tag, "Reject remote download files.")
                null
            } else {
                DownloadFilesRsp(maxConnection)
            }
        }
    }

    protected val messageServer: IServer<SendMsgReq, Unit> by lazy {
        server<SendMsgReq, Unit>(
            requestType = FileExploreDataType.SendMsgReq.type,
            responseType = FileExploreDataType.SendMsgRsp.type
        ) { _, _, request, isNewRequest ->
            if (isNewRequest) {
                NetLog.d(tag, "Remote send message: ${request.msg}")
                val isSuccess = remoteMessageFlow.tryEmit(request.msg to request.sendTime)
                if (!isSuccess) {
                    NetLog.e(tag, "Failed to emit remote message: ${request.msg}")
                }
            }
            Unit
        }
    }

    suspend fun waitHandshakeOrNull(): ExploreHandshake? {
        val state = waitTaskConnectedOrError()
        return if (state is TaskState.Connected) {
            exploreHandshakeFlow.value.apply {
                if (this == null) {
                    error(TransferException("Connected, but handshake is null."))
                }
            }
        } else {
            null
        }
    }

    suspend fun requestExploreRemoteDir(requestDir: String): Pair<List<ExplorerDir>, List<ExplorerFile>>? {
        return try {
            clientManager()?.requestSimplify<ExploreDirReq, ExploreDirRsp>(
                requestType = FileExploreDataType.ExploreDirReq.type,
                responseType = FileExploreDataType.ExploreDirRsp.type,
                request = ExploreDirReq(requestDir)
            )?.let {
                it.childrenDirs to it.childrenFiles
            } ?: throw TransferException("Connection has already closed.")
        } catch (e: Throwable) {
            NetLog.e(tag, "Failed to explore remote dir: $requestDir. Cause: ${e.message}", e)
            null
        }
    }

    suspend fun requestSendMessage(msg: String, sendTime: Long): Boolean {
        return try {
            clientManager()?.requestSimplify<SendMsgReq, Unit>(
                requestType = FileExploreDataType.SendMsgReq.type,
                responseType = FileExploreDataType.SendMsgRsp.type,
                request = SendMsgReq(sendTime, msg)
            ) ?: throw TransferException("Connection has already closed.")
            true
        } catch (e: Throwable) {
            NetLog.e(tag, "Failed to send message: $msg. Cause: ${e.message}", e)
            false
        }
    }

    /**
     * return the buffer size if success, null if failed.
     */
    suspend fun requestSendFiles(toSendFiles: List<ExplorerFile>, maxConnection: Int): Int? {
        return try {
            clientManager()?.requestSimplify<SendFilesReq, SendFilesRsp>(
                requestType = FileExploreDataType.SendFilesReq.type,
                responseType = FileExploreDataType.SendFilesRsp.type,
                request = SendFilesReq(toSendFiles, maxConnection)
            )?.bufferSize ?: throw TransferException("Connection has already closed.")
        } catch (e: Throwable) {
            NetLog.e(tag, "Failed to send files: ${toSendFiles}. Cause: ${e.message}", e)
            null
        }
    }

    /**
     * return the max connection if success, null if failed.
     */
    suspend fun requestDownloadFiles(toDownloadFiles: List<ExplorerFile>, bufferSize: Int): Int? {
        return try {
            clientManager()?.requestSimplify<DownloadFilesReq, DownloadFilesRsp>(
                requestType = FileExploreDataType.DownloadFilesReq.type,
                responseType = FileExploreDataType.DownloadFilesRsp.type,
                request = DownloadFilesReq(toDownloadFiles, bufferSize)
            )?.maxConnection ?: throw TransferException("Connection has already closed.")
        } catch (e: Throwable) {
            NetLog.e(tag, "Failed to download files: ${toDownloadFiles}. Cause: ${e.message}", e)
            null
        }
    }

    fun remoteMessage(): Flow<Pair<String, Long>> = remoteMessageFlow

    override suspend fun onError(throwable: Throwable?) {
        NetLog.e(tag, throwable?.message ?: "Unknown error.", throwable)
        release()
    }

    override suspend fun onStopTask(cause: String?) {
        NetLog.d(tag, "Task stopped. Cause: $cause")
        release()
    }

    protected abstract fun clientManager(): ITcpClientManager?

    abstract fun release()

}
