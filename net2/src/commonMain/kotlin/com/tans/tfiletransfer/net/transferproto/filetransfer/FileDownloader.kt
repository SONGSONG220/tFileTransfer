package com.tans.tfiletransfer.net.transferproto.filetransfer

import com.tans.tfiletransfer.net.ITask
import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.Address
import com.tans.tfiletransfer.net.transferproto.TransferException
import com.tans.tfiletransfer.net.transferproto.fileexplore.model.ExplorerFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import okio.Path.Companion.toPath

class FileDownloader internal constructor(
    val downloadDir: String,
    val toDownloadRemoteFile: ExplorerFile,
    val senderAddress: Address,
    val maxConnection: Int,
    val minDownloadFileSegmentSize: Long
) : ITask {

    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()

    override suspend fun onStartTask() {
        val downloadDirPath = try {
            val downloadDirPath = downloadDir.toPath()
            if (!fileSystem.exists(downloadDirPath)) {
                fileSystem.createDirectories(downloadDirPath)
            }
            downloadDirPath
        } catch (e: Throwable) {
            error(TransferException("Failed to create download dir. Cause: ${e.message}", e))
            return
        }
        val downloadingFilePath = try {
            resolveUniqueLocalFilePath(downloadDirPath, "${toDownloadRemoteFile.name}.downloading", true)
        } catch (e: Throwable) {
            error(TransferException("Failed to create downloading file. Cause: ${e.message}", e))
            return
        }
        val downloadingFileHandle = try {
            val downloadingFileHandle = fileSystem.openReadWrite(downloadingFilePath)
            try {
                downloadingFileHandle.resize(toDownloadRemoteFile.size)
            } catch (e: Throwable) {
                downloadingFileHandle.close()
                throw e
            }
            downloadingFileHandle
        } catch (e: Throwable) {
            try {
                fileSystem.delete(downloadingFilePath)
            } catch (e: Throwable) {
                NetLog.e(TAG, "Failed to delete downloading file. Cause: ${e.message}", e)
            }
            error(TransferException("Failed to resize downloading file. Cause: ${e.message}", e))
            return
        }
        val segmentRanges = calculateFileSegmentRanges(toDownloadRemoteFile, minDownloadFileSegmentSize, maxConnection)
        val segmentDownloaderTasks = segmentRanges.map { (start, endExclusive) ->
            FileSegmentDownloader(downloadingFileHandle, start, endExclusive, toDownloadRemoteFile, senderAddress)
        }
        fun segmentDownloaderTaskError(errorTask: FileSegmentDownloader, e: Throwable?) {
            for (task in segmentDownloaderTasks) {
                if (task !== errorTask) {
                    task.error(TransferException("Other segment downloader task error. Cause: ${e?.message}", e))
                }
            }
        }
        NetLog.d(TAG, "Start download file ${toDownloadRemoteFile.name} from $senderAddress. Segment count: ${segmentRanges.size}")
        val segmentDownloaderJobs = segmentDownloaderTasks.map { task ->
            coroutineScope.async {
                task.startTask()
                try {
                    val s = task.waitTaskFinished()
                    if (s is TaskState.Error) {
                        error(TransferException("Segment downloader task error. Cause: ${s.throwable?.message}", s.throwable))
                        segmentDownloaderTaskError(task, s.throwable)
                        false
                    } else {
                        true
                    }
                } catch (e: Throwable) { // File downloader canceled.
                    task.error(TransferException("File downloader canceled.", e))
                    false
                }
            }
        }
        val allSuccess = try {
            !segmentDownloaderJobs.any { !it.await() }
        } catch (_: Throwable) { // canceled.
            false
        }
        downloadingFileHandle.close()
        if (allSuccess) {
            try {
                val toRenamePath = resolveUniqueLocalFilePath(downloadDirPath, toDownloadRemoteFile.name, false)
                fileSystem.atomicMove(downloadingFilePath, toRenamePath)
            } catch (e: Throwable) {
                NetLog.e(TAG, "Failed to rename downloading file. Cause: ${e.message}", e)
            }
            val msg = "Download file ${toDownloadRemoteFile.name} from $senderAddress success."
            NetLog.d(TAG, msg)
            stopTask(msg)
        } else {
            try {
                fileSystem.delete(downloadingFilePath)
            } catch (e: Throwable) {
                NetLog.e(TAG, "Failed to delete downloading file. Cause: ${e.message}", e)
            }
            NetLog.e(TAG, "Download file ${toDownloadRemoteFile.name} from $senderAddress failed.")
            error(TransferException("Download file ${toDownloadRemoteFile.name} from $senderAddress failed."))
        }
    }

    override suspend fun onStopTask(cause: String?) {
        NetLog.d(TAG, "Task stopped. Cause: $cause")
    }

    override suspend fun onError(throwable: Throwable?) {
        NetLog.e(TAG, throwable?.message ?: "Unknown error.", throwable)
    }

    companion object {
        private const val TAG = "FileDownloader"
    }
}