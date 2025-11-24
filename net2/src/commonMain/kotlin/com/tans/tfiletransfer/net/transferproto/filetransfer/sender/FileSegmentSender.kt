package com.tans.tfiletransfer.net.transferproto.filetransfer.sender

import com.tans.tfiletransfer.net.ITask
import com.tans.tfiletransfer.net.NetLog
import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.socket.ext.ITcpServerClientManager
import com.tans.tfiletransfer.net.transferproto.filetransfer.model.DownloadFileSegmentReq
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import okio.FileHandle

class FileSegmentSender(
    val sendingFileHandle: FileHandle,
    val serverClientManager: ITcpServerClientManager,
    val downloadReq: DownloadFileSegmentReq,
    val sendCallback: (thisTimeSendBufferSize: Int, downloadedSize: Long) -> Unit,
) : ITask {

    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()

    override suspend fun onStartTask() {

        TODO("Not yet implemented")
    }

    override suspend fun onStopTask(cause: String?) {
    }

    override suspend fun onError(throwable: Throwable?) {
        NetLog.e(TAG, throwable?.message ?: "Unknown error", throwable)
    }

    companion object {
        private const val TAG = "FileSegmentSender"
    }

}