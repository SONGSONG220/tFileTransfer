package com.tans.tfiletransfer.net.socket.tcp

import com.tans.tfiletransfer.net.socket.ConnectionTask
import com.tans.tfiletransfer.net.socket.ConnectionTaskState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex

class TcpServerTask : ConnectionTask {
    override val stateFlow: StateFlow<ConnectionTaskState> = MutableStateFlow(ConnectionTaskState.Init)
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

}