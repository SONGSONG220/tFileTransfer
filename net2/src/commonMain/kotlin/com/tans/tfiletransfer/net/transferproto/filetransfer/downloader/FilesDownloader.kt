package com.tans.tfiletransfer.net.transferproto.filetransfer.downloader

import com.tans.tfiletransfer.net.ITask
import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.Address
import com.tans.tfiletransfer.net.transferproto.TransferException
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex

class FilesDownloader(
    val downloadDir: String,
    val toDownloadRemoteFiles: List<ExplorerFile>,
    val senderAddress: Address,
    val maxConnection: Int,
    val minDownloadFileSegmentSize: Long = DEFAULT_MIN_DOWNLOAD_FILE_SEGMENT_SIZE
) : ITask {

    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()

    private val downloadingFileTask: MutableStateFlow<FileDownloader?> = MutableStateFlow(null)

    override suspend fun onStartTask() {
        if (toDownloadRemoteFiles.isEmpty()) {
            error(TransferException("No file to download."))
            return
        }

        for (toDownloadRemoteFile in toDownloadRemoteFiles) {
            val downloader = FileDownloader(
                downloadDir = downloadDir,
                toDownloadRemoteFile = toDownloadRemoteFile,
                senderAddress = senderAddress,
                maxConnection = maxConnection,
                minDownloadFileSegmentSize = minDownloadFileSegmentSize
            )
            downloader.startTask()
            downloadingFileTask.value = downloader
            try {
                val downloaderState = downloader.waitTaskFinished()
                if (downloaderState is TaskState.Error) {
                    error(TransferException("Download file $toDownloadRemoteFile failed. Cause: ${downloaderState.throwable?.message}", downloaderState.throwable))
                    return
                }
            } catch (e: Throwable) { // Files downloader canceled
                downloader.error(TransferException("Files downloader canceled.", e))
                return
            }
        }
        stopTask("Download all files success.")
    }

    override suspend fun onStopTask(cause: String?) {
        NetLog.d(TAG, "Task stopped. Cause: $cause")
    }

    override suspend fun onError(throwable: Throwable?) {
        NetLog.e(TAG, throwable?.message ?: "Unknown error.", throwable)
    }

    fun downloadingFileTask(): Flow<FileDownloader?> = downloadingFileTask

    companion object Companion {
        private const val TAG = "FilesDownloader"
        // 10MB
        const val DEFAULT_MIN_DOWNLOAD_FILE_SEGMENT_SIZE = 10L * 1024L * 1024L
    }
}
