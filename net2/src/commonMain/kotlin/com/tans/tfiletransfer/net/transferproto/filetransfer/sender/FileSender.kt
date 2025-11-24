package com.tans.tfiletransfer.net.transferproto.filetransfer.sender

import com.tans.tfiletransfer.net.ITask
import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.ext.ITcpServerClientManager
import com.tans.tfiletransfer.net.transferproto.TransferException
import com.tans.tfiletransfer.net.transferproto.filetransfer.fileSystem
import com.tans.tfiletransfer.net.transferproto.filetransfer.model.DownloadFileSegmentReq
import com.tans.tfiletransfer.net.transferproto.filetransfer.model.SenderFile
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import okio.Path.Companion.toPath

class FileSender internal constructor(
    val toSendChannel: Channel<Pair<DownloadFileSegmentReq, ITcpServerClientManager>>,
    val toSendFile: SenderFile,
) : ITask {

    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()
    private val sendSize = atomic(0L)
    private val sendSizeFlow: MutableStateFlow<Long> = MutableStateFlow(0L)

    override suspend fun onStartTask() {
        val sendingFileHandle = try {
            fileSystem.openReadOnly(toSendFile.realFilePath.toPath())
        } catch (e: Throwable) {
            error(TransferException("Open file failed. Path: ${toSendFile.realFilePath}, Error: ${e.message}", e))
            return
        }
        val segmentSenderTasks = mutableListOf<FileSegmentSender>()
        fun segmentSenderTaskError(errorTask: FileSegmentSender?, e: Throwable?) {
            for (task in segmentSenderTasks) {
                if (task !== errorTask) {
                    task.error(TransferException("Other segment downloader task error. Cause: ${e?.message}", e))
                }
            }
        }
        NetLog.d(TAG, "Start send file. FilePath: ${toSendFile.realFilePath}")
        try {
            for ((downloadReq, serverClientManager) in toSendChannel) {
                val senderTask = FileSegmentSender(
                    sendingFileHandle = sendingFileHandle,
                    serverClientManager = serverClientManager,
                    downloadReq = downloadReq
                ) { thisTimeSendBufferSize, _ ->
                    sendSize.addAndGet(thisTimeSendBufferSize.toLong())
                    sendSizeFlow.value = sendSize.value
                }
                senderTask.startTask()
                segmentSenderTasks.add(senderTask)
            }
        } catch (e: Throwable) { // Canceled.
            val canceledException = TransferException("File sender canceled.", e)
            segmentSenderTaskError(null, canceledException)
            error(canceledException)
            runCatching {
                sendingFileHandle.close()
            }
            return
        }
        NetLog.d(TAG, "Receive all segment sender tasks. Count: ${segmentSenderTasks.size}")
        val senderJobs = segmentSenderTasks.map { task ->
            coroutineScope.async {
                try {
                    val s = task.waitTaskFinished()
                    if (s is TaskState.Error) {
                        error(TransferException("Segment sender task error. Cause: ${s.throwable?.message}", s.throwable))
                        segmentSenderTaskError(task, s.throwable)
                        false
                    } else {
                        true
                    }
                } catch (e: Throwable) { // File downloader canceled.
                    task.error(TransferException("File sender canceled.", e))
                    false
                }
            }
        }
        val allSuccess = try {
            !senderJobs.any { !it.await() }
        } catch (_: Throwable) { // canceled.
            false
        }
        runCatching {
            sendingFileHandle.close()
        }
        if (allSuccess) {
            val msg = "Send file success. FilePath: ${toSendFile.realFilePath}"
            NetLog.d(TAG, msg)
            stopTask(msg)
        } else {
            val msg = "Send file failed. FilePath: ${toSendFile.realFilePath}"
            NetLog.e(TAG, msg)
            error(TransferException(msg))
        }
    }

    override suspend fun onStopTask(cause: String?) {
        NetLog.d(TAG, "Task stopped. Cause: $cause")
    }

    override suspend fun onError(throwable: Throwable?) {
        NetLog.e(TAG, throwable?.message ?: "Unknown error.", throwable)
    }

    fun sendSize(): Flow<Long> = sendSizeFlow

    companion object {
        private const val TAG = "FileSender"
    }
}