package com.tans.tfiletransfer.net.transferproto.filetransfer.sender

import com.tans.tfiletransfer.net.ITask
import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.Address
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.ext.ITcpServerClientManager
import com.tans.tfiletransfer.net.socket.ext.defaultClientManager
import com.tans.tfiletransfer.net.socket.ext.server.defaultServerManager
import com.tans.tfiletransfer.net.socket.ext.server.server
import com.tans.tfiletransfer.net.socket.tcp.TcpServerTask
import com.tans.tfiletransfer.net.transferproto.TransferException
import com.tans.tfiletransfer.net.transferproto.TransferProtoConstant
import com.tans.tfiletransfer.net.transferproto.filetransfer.model.DownloadFileSegmentReq
import com.tans.tfiletransfer.net.transferproto.filetransfer.model.FileTransferDataType
import com.tans.tfiletransfer.net.transferproto.filetransfer.model.SenderFile
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeout

class FilesSender(
    val toSendLocalFiles: List<SenderFile>,
    val localAddress: Address,
    val waitClientSegmentTimeoutInMillis: Long = 3000L
) : ITask {
    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()

    private val sendingFileTask: MutableStateFlow<FileSender?> = MutableStateFlow(null)

    override suspend fun onStartTask() {
        if (toSendLocalFiles.isEmpty()) {
            error(TransferException("No file to send."))
            return
        }
        val serverTask = TcpServerTask(
            bindAddress = AddressWithPort(
                address = localAddress,
                port = TransferProtoConstant.FILE_TRANSFER_SERVER_PORT
            )
        )
        serverTask.startTask()
        serverTask.waitTaskConnectedOrError().apply {
            if (this is TaskState.Error) {
                error(TransferException("Failed to start server task. Cause: ${this.throwable?.message}", this.throwable))
                return
            }
        }
        val toSendLocalFilesToReqChannel = toSendLocalFiles.associateWith { Channel<Pair<DownloadFileSegmentReq, ITcpServerClientManager>>(Channel.UNLIMITED) to atomic(0L) }

        coroutineScope.launch {
            try {
                for (clientTask in serverTask.clientChannel()) {
                    launch {
                        val serverClientManager = clientTask
                            .defaultServerManager()
                            .defaultClientManager()
                        val reqChannel = Channel<DownloadFileSegmentReq>(1)
                        val server = server<DownloadFileSegmentReq, Unit>(
                            requestType = FileTransferDataType.DownloadFileSegmentReq.type,
                            responseType = FileTransferDataType.DownloadFileSegmentRsp.type,
                        ) { _, _, r, isNewRequest ->
                            NetLog.d(TAG, "Receive download file segment req: $r")
                            if (isNewRequest) {
                                reqChannel.send(r)
                            }
                            Unit
                        }
                        serverClientManager.registerServer(server)
                        val req = try {
                           withTimeout(waitClientSegmentTimeoutInMillis) {
                                reqChannel.receive()
                            }
                        } catch (e: Throwable) {
                            clientTask.stopTask()
                            serverTask.stopTask()
                            error(TransferException("Waiting client segment request error.", e))
                            return@launch
                        } finally {
                            serverClientManager.unregisterServer(server)
                        }
                        val keyValue = toSendLocalFilesToReqChannel.toList().find { it.first.exploreFile.path == req.file.path }
                        if (keyValue == null) {
                            clientTask.stopTask()
                            serverTask.stopTask()
                            error(TransferException("Wrong download req $req, don't find target file."))
                            return@launch
                        }
                        val key = keyValue.first
                        val value = keyValue.second
                        val segmentSize = req.end - req.start
                        if (segmentSize <= 0L) {
                            clientTask.stopTask()
                            serverTask.stopTask()
                            error(TransferException("Wrong download req $req, segment size must be greater than 0."))
                            return@launch
                        }
                        val (channel, receivedReqSegmentSize) = value

                        try {
                            channel.send(req to serverClientManager)
                            if (receivedReqSegmentSize.addAndGet(segmentSize) >= key.exploreFile.size) {
                                channel.close()
                            }
                        } catch (_: Throwable) {
                        }
                    }
                }
            } catch (_: Throwable) {
            }
        }
        for (toSendFile: SenderFile in toSendLocalFiles) {
            val c = toSendLocalFilesToReqChannel[toSendFile]!!.first
            val fileSender = FileSender(
                toSendChannel = c,
                toSendFile = toSendFile,
            )
            fileSender.startTask()
            sendingFileTask.value = fileSender
            try {
                val senderState = fileSender.waitTaskFinished()
                if (senderState is TaskState.Error) {
                    serverTask.stopTask()
                    error(TransferException("Failed to send file ${toSendFile.exploreFile.path}. Cause: ${senderState.throwable?.message}", senderState.throwable))
                    return
                }
            } catch (e: Throwable) {
                serverTask.stopTask(e.message)
                error(TransferException("File sender canceled", e))
                return
            }
        }
        stopTask("Send all files success.")

    }

    override suspend fun onStopTask(cause: String?) {
        NetLog.d(TAG, "Task stopped. Cause: $cause")
    }

    override suspend fun onError(throwable: Throwable?) {
        NetLog.e(TAG, throwable?.message ?: "Unknown error.", throwable)
    }

    fun sendingFileTask(): Flow<FileSender?> = sendingFileTask

    companion object {
        private const val TAG = "FilesSender"
    }
}