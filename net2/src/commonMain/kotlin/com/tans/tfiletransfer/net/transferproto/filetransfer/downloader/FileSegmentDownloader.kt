package com.tans.tfiletransfer.net.transferproto.filetransfer.downloader

import com.tans.tfiletransfer.net.ITask
import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.Address
import com.tans.tfiletransfer.net.socket.AddressWithPort
import com.tans.tfiletransfer.net.socket.PackageData
import com.tans.tfiletransfer.net.socket.ext.client.defaultClientManager
import com.tans.tfiletransfer.net.socket.ext.client.requestSimplify
import com.tans.tfiletransfer.net.socket.ext.defaultServerManager
import com.tans.tfiletransfer.net.socket.ext.server.server
import com.tans.tfiletransfer.net.socket.tcp.TcpClientTask
import com.tans.tfiletransfer.net.transferproto.TransferException
import com.tans.tfiletransfer.net.transferproto.TransferProtoConstant
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile
import com.tans.tfiletransfer.net.transferproto.filetransfer.model.DownloadFileSegmentReq
import com.tans.tfiletransfer.net.transferproto.filetransfer.model.FileTransferDataType
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import okio.FileHandle

class FileSegmentDownloader internal constructor(
    val downloadingFileHandle: FileHandle,
    val segmentStart: Long,
    val segmentEnd: Long,
    val toDownloadRemoteFile: ExplorerFile,
    val senderAddress: Address,
    val downloadedCallback: (thisTimeDownloadBufferSize: Int, downloadedSize: Long) -> Unit
) : ITask {

    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()

    override suspend fun onStartTask() {

        var retryTimes = 0
        var downloaderTask: TcpClientTask
        while (true) {
            downloaderTask = TcpClientTask(serverAddress = AddressWithPort(senderAddress, TransferProtoConstant.FILE_TRANSFER_SERVER_PORT))
            downloaderTask.startTask()
            val state = downloaderTask.waitTaskConnectedOrError()
            if (state is TaskState.Connected) {
                break
            }
            if (retryTimes > MAX_CONNECTION_RETRY_TIMES) {
                error(TransferException("Failed to connect to server. Retry times: $retryTimes", (state as? TaskState.Error)?.throwable))
                return
            } else {
                NetLog.e(TAG, "Failed to connect to server. Retry times: $retryTimes", (state as? TaskState.Error)?.throwable)
            }
            retryTimes ++
        }
        val clientServerManager = downloaderTask.defaultClientManager().defaultServerManager()
        val downloadedSize = atomic(0L)
        val finished = atomic(false)
        val finishChannel = Channel<Unit>(1)
        val downloadServer = server<PackageData, Unit>(
            requestType = FileTransferDataType.SendFileBufferReq.type,
            responseType = FileTransferDataType.SendFileBufferRsp.type,
        ) { _, _, request, isNew ->
            if (isNew) {
                if (finished.value) return@server Unit
                val buffer = request.data
                val remain = segmentEnd - segmentStart - downloadedSize.value
                val writeLen = if (remain <= 0L) 0 else minOf(remain, buffer.contentSize.toLong()).toInt()
                try {
                    if (writeLen > 0) {
                        val writeStart = downloadedSize.value + segmentStart
                        downloadingFileHandle.write(writeStart, buffer.array, 0, writeLen)
                        val size = downloadedSize.addAndGet(writeLen.toLong())
                        downloadedCallback(writeLen, size)
                        if (size >= segmentEnd - segmentStart) {
                            finished.getAndSet(true)
                            finishChannel.send(Unit)
                        }
                    } else {
                        finished.getAndSet(true)
                    }
                    downloaderTask.bufferPool.put(buffer)
                    Unit
                } catch (e: Throwable) {
                    error(TransferException("Write file fail: ${e.message}", e))
                    downloaderTask.stopTask()
                    null
                }
            } else {
                Unit
            }
        }
        clientServerManager.registerServer(downloadServer)
        try {
            clientServerManager.requestSimplify<DownloadFileSegmentReq, Unit>(
                requestType = FileTransferDataType.DownloadFileSegmentReq.type,
                responseType = FileTransferDataType.DownloadFileSegmentRsp.type,
                request = DownloadFileSegmentReq(
                    file = toDownloadRemoteFile,
                    start = segmentStart,
                    end = segmentEnd,
                ),
            )
        } catch (e: Throwable) {
            error(TransferException("Request download file segment failed. Start: $segmentStart, End: $segmentEnd, File: $toDownloadRemoteFile", e))
            downloaderTask.stopTask()
            return
        }
        try {
            finishChannel.receive()
        } catch (e: Throwable) { // canceled
            NetLog.e(TAG, "Download file segment canceled. Start: $segmentStart, End: $segmentEnd, File: $toDownloadRemoteFile", e)
            downloaderTask.stopTask()
            return
        }
        NetLog.d(TAG, "Download file segment success. Start: $segmentStart, End: $segmentEnd, File: $toDownloadRemoteFile")
        runCatching {
            // No reply
            clientServerManager.requestSimplify<Unit>(
                requestType = FileTransferDataType.DownloadFileSegmentEndReq.type,
                request = Unit
            )
        }
        downloaderTask.stopTask()
        stopTask("Download file segment success.")
    }

    override suspend fun onStopTask(cause: String?) {

    }

    override suspend fun onError(throwable: Throwable?) {
        NetLog.e(TAG, throwable?.message ?: "Unknown error", throwable)
    }

    companion object {
        private const val TAG = "FileSegmentDownloader"
        private const val MAX_CONNECTION_RETRY_TIMES = 2
    }
}