package com.tans.tfiletransfer.net.transferproto.filetransfer.sender

import com.tans.tfiletransfer.net.ITask
import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.ext.ITcpServerClientManager
import com.tans.tfiletransfer.net.socket.ext.client.requestSimplify
import com.tans.tfiletransfer.net.transferproto.filetransfer.model.DownloadFileSegmentReq
import com.tans.tfiletransfer.net.transferproto.filetransfer.model.FileTransferDataType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import okio.FileHandle
import com.tans.tfiletransfer.net.socket.buffer.Buffer
import com.tans.tfiletransfer.net.socket.ext.server.server
import com.tans.tfiletransfer.net.transferproto.TransferException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlin.time.measureTime

class FileSegmentSender internal constructor(
    val sendingFileHandle: FileHandle,
    val serverClientManager: ITcpServerClientManager,
    val downloadReq: DownloadFileSegmentReq,
    val sendCallback: (thisTimeSendBufferSize: Int, downloadedSize: Long) -> Unit,
) : ITask {

    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()

    override suspend fun onStartTask() {
        val start = downloadReq.start
        val end = downloadReq.end
        val segmentSize = end - start
        if (segmentSize <= 0L) {
            error(TransferException("Wrong download req $downloadReq, segment size must be greater than 0."))
            return
        }
        val finishedChannel = Channel<Unit>(1)
        val finishedServer = server<Unit, Unit>(
            requestType = FileTransferDataType.DownloadFileSegmentEndReq.type,
            responseType = FileTransferDataType.DownloadFileSegmentEndReq.type,
        ) { _, _, _, isNew ->
            if (isNew) {
                finishedChannel.trySend(Unit)
            }
            null
        }
        serverClientManager.registerServer(finishedServer)

        NetLog.d(TAG, "Start send file segment. Start: $start, End: $end, FilePath: ${downloadReq.file.path}")
        var offset = start
        var currentBufferSize = DEFAULT_SEND_BUFFER_SIZE
        try {
            while (offset < end) {
                try {
                    val remain = end - offset
                    val bufSize = minOf(remain, currentBufferSize.toLong()).toInt()
                    val buffer: Buffer = serverClientManager.connectionTask.bufferPool.get(bufSize)
                    val readLen = sendingFileHandle.read(offset, buffer.array, 0, bufSize)
                    buffer.contentSize = readLen
                    if (readLen <= 0) {
                        break
                    }
                    val timeCost = measureTime {
                        serverClientManager.requestSimplify<Buffer, Unit>(
                            requestType = FileTransferDataType.SendFileBufferReq.type,
                            request = buffer,
                            responseType = FileTransferDataType.SendFileBufferRsp.type,
                            retryTimeout = 4000L,
                            retryTimes = 1
                        )
                    }.inWholeMilliseconds
                    currentBufferSize = ((readLen.toLong() * TARGET_SEND_DURATION_MS) / timeCost).toInt()
                        .coerceAtLeast(MIN_SEND_BUFFER_SIZE)
                        .coerceAtMost(MAX_SEND_BUFFER_SIZE)
                    offset += readLen
                    sendCallback(readLen, offset - start)
                } catch (e: Throwable) {
                    error(TransferException("Send file buffer failed: ${e.message}", e))
                    return
                }
            }
        } catch (e: Throwable) {
            error(TransferException("File segment sender canceled.", e))
            return
        }
        try {
            withTimeout(200L) {
                finishedChannel.receive()
            }
            NetLog.d(TAG, "Receive downloader finished.")
        } catch (e: Throwable) {
            NetLog.w(TAG, "Receive downloader finished timeout: ${e.message}")
        }

        val msg = "Send file segment success. Start: $start, End: $end, FilePath: ${downloadReq.file.path}"
        NetLog.d(TAG, msg)
        stopTask(msg)
    }

    override suspend fun onStopTask(cause: String?) {
        serverClientManager.connectionTask.stopTask(cause)
    }

    override suspend fun onError(throwable: Throwable?) {
        serverClientManager.connectionTask.onError(throwable)
        NetLog.e(TAG, throwable?.message ?: "Unknown error", throwable)
    }

    companion object {
        private const val TAG = "FileSegmentSender"
        // 128 K
        private const val DEFAULT_SEND_BUFFER_SIZE = 128 * 1024
        // 512 B
        private const val MIN_SEND_BUFFER_SIZE = 512
        // 3 M
        private const val MAX_SEND_BUFFER_SIZE = 3 * 1024 * 1024

        private const val TARGET_SEND_DURATION_MS = 1024L
    }

}
