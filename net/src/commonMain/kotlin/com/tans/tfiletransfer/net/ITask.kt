package com.tans.tfiletransfer.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ITask {

    val stateFlow: StateFlow<TaskState>
    val coroutineScope: CoroutineScope
    val stateUpdateMutex: Mutex

    fun startTask() {
        coroutineScope.launch {
            updateStateExpect(TaskState.Init, TaskState.Connecting) {
                onStartTask()
            }
        }
    }

    suspend fun onStartTask()

    fun state(): Flow<TaskState> = stateFlow

    fun currentState(): TaskState = stateFlow.value

    suspend fun waitTaskConnectedOrError() : TaskState {
        return state().first { it is TaskState.Connected || it is TaskState.Error }
    }

    suspend fun waitTaskFinished(): TaskState {
        return state().first { it is TaskState.Closed || it is TaskState.Error }
    }

    fun stopTask(cause: String? = null) {
        coroutineScope.launch {
            updateStateIf(
                `if` = {
                    val s = currentState()
                    s != TaskState.Init && s !is TaskState.Closed && s !is TaskState.Error
                },
                update = TaskState.Closed(cause)
            ) {
                onStopTask(cause)
                coroutineScope.cancel()
            }
        }
    }

    suspend fun onStopTask(cause: String?)

    fun error(throwable: Throwable?) {
        coroutineScope.launch {
            updateStateIf(
                `if` = {
                    val s = currentState()
                    s != TaskState.Init && s !is TaskState.Closed && s !is TaskState.Error
                },
                update = TaskState.Error(throwable)
            ) {
                onError(throwable)
                coroutineScope.cancel()
            }
        }
    }

    suspend fun onError(throwable: Throwable?)

    suspend fun updateStateExpect(
        expect: TaskState,
        update: TaskState,
        fail: suspend () -> Unit = {},
        success: suspend () -> Unit = {}
    ) {
        updateStateIf(
            `if` = {
                currentState() == expect
            },
            update = update,
            fail = fail,
            success = success
        )
    }

    suspend fun updateStateIf(
        `if`: suspend() -> Boolean,
        update: TaskState,
        fail: suspend() -> Unit = {},
        success: suspend() -> Unit = {}
    ) {
        var isSuccess = false
        stateUpdateMutex.withLock {
            if (`if`()) {
                (stateFlow as MutableStateFlow).value = update
                isSuccess = true
            }
        }
        if (isSuccess) {
            success()
        } else {
            fail()
        }
    }
}

sealed class TaskState {
    data object Init : TaskState()
    data object Connecting : TaskState()
    data object Connected : TaskState()
    data class Closed(val cause: String?) : TaskState()
    data class Error(val throwable: Throwable?) : TaskState()
}