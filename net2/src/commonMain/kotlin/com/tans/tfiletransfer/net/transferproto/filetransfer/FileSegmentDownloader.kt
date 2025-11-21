package com.tans.tfiletransfer.net.transferproto.filetransfer

import com.tans.tfiletransfer.net.ITask
import com.tans.tfiletransfer.net.TaskState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import okio.FileHandle

class FileSegmentDownloader internal constructor(
    val downloadingFileHandle: FileHandle,
    val segmentStart: Long,
    val segmentEnd: Long,
) : ITask {

    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateUpdateMutex: Mutex = Mutex()

    override suspend fun onStartTask() {
        TODO("Not yet implemented")
    }

    override suspend fun onStopTask(cause: String?) {
        TODO("Not yet implemented")
    }

    override suspend fun onError(throwable: Throwable?) {
        TODO("Not yet implemented")
    }

    companion object {
        private const val TAG = "FileSegmentDownloader"
    }
}