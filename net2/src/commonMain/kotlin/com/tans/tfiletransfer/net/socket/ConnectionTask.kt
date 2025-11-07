package com.tans.tfiletransfer.net.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ConnectionTask {

    val stateFlow: StateFlow<ConnectionTaskState>
    val coroutineScope: CoroutineScope
    val stateUpdateMutex: Mutex

    fun startTask() {
        coroutineScope.launch {
            updateStateExpect(ConnectionTaskState.Init, ConnectionTaskState.Connecting) {
                onStartTask()
            }
        }
    }

    suspend fun onStartTask()

    fun state(): Flow<ConnectionTaskState> = stateFlow

    fun currentState(): ConnectionTaskState = stateFlow.value

    fun stopTask(cause: String? = null) {
        coroutineScope.launch {
            updateStateIf(
                `if` = {
                    val s = currentState()
                    s != ConnectionTaskState.Init && s !is ConnectionTaskState.Closed && s !is ConnectionTaskState.Error
                },
                update = ConnectionTaskState.Closed(cause)
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
                    s != ConnectionTaskState.Init && s !is ConnectionTaskState.Closed && s !is ConnectionTaskState.Error
                },
                update = ConnectionTaskState.Error(throwable)
            ) {
                onError(throwable)
                coroutineScope.cancel()
            }
        }
    }

    suspend fun onError(throwable: Throwable?)

    suspend fun updateStateExpect(
        expect: ConnectionTaskState,
        update: ConnectionTaskState,
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
        update: ConnectionTaskState,
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