package com.tans.tfiletransfer.net.socket

import com.tans.tfiletransfer.net.TaskState
import com.tans.tfiletransfer.net.NetLog
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlin.time.TimeSource

abstract class BaseConnectionTask(
    private val readWriteIdleLimitInMillis: Long
) : IConnectionTask {

    private val readWriteTimeMark = atomic<TimeSource.Monotonic.ValueTimeMark?>(null)
    private val checkIdle: Boolean = readWriteIdleLimitInMillis in 1 until Long.MAX_VALUE

    override val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override val stateFlow: StateFlow<TaskState> = MutableStateFlow(TaskState.Init)
    override val stateUpdateMutex: Mutex = Mutex()

    open val tag = "BaseConnectionTask"

    override suspend fun onStartTask() {
        if (checkIdle) {
            readWriteTimeMark.getAndSet(TimeSource.Monotonic.markNow())
            coroutineScope.launch {
                try {
                    while (true) {
                        delay(readWriteIdleLimitInMillis)
                        val mark = readWriteTimeMark.value
                        if (mark == null || mark.elapsedNow().inWholeMilliseconds > readWriteIdleLimitInMillis) {
                            val errorMsg = "Read/Write idle timeout: ${readWriteIdleLimitInMillis}ms"
                            NetLog.e(tag, errorMsg)
                            error(SocketException(errorMsg))
                            break
                        }
                    }
                } catch (_: Throwable) {
                    // Job cancelled
                }
            }
        }
    }

    protected fun resetLastReadWriteTime() {
        if (checkIdle) {
            readWriteTimeMark.getAndSet(TimeSource.Monotonic.markNow())
        }
    }
}